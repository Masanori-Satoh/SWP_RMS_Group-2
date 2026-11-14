package com.group2.rms.offer.service;

import com.group2.rms.notification.service.NotificationService;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.offer.repository.OfferProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real Spring transaction synchronization with JDBC mocked; database predicates are tested separately. */
@ExtendWith(MockitoExtension.class)
class OfferExpirationJobTests {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 13, 12, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock private OfferProposalRepository offers;
    @Mock private NotificationService notifications;
    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    private DataSourceTransactionManager transactions;
    private OfferExpirationJob job;

    @BeforeEach
    void setUp() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        transactions = new DataSourceTransactionManager(dataSource);
        job = new OfferExpirationJob(offers, transactions, CLOCK, notifications);
    }

    private OfferProposal dueOffer(int id) {
        return OfferProposal.builder().offerId(id).offerStatus("Sent_Candidate")
                .responseDeadline(NOW.minusSeconds(1)).expectedStartDate(NOW.toLocalDate().plusDays(1)).build();
    }

    private void due(OfferProposal offer) {
        when(offers.findDueForUpdate(eq(NOW), eq(NOW.toLocalDate()), any(Pageable.class)))
                .thenReturn(List.of(offer));
    }

    @Test
    void expireNow_shouldNotifyInNewTransactionOnlyAfterExpirationCommit() throws Exception {
        OfferProposal offer = dueOffer(41);
        due(offer);
        when(offers.findActiveForUpdate(41)).thenReturn(Optional.of(offer));
        when(offers.save(offer)).thenAnswer(invocation -> {
            verifyNoInteractions(notifications);
            assertEquals("Expired", offer.getOfferStatus());
            return offer;
        });

        assertEquals(1, job.expireNow());

        var ordered = inOrder(connection, offers, notifications);
        ordered.verify(offers).save(offer);
        ordered.verify(connection).commit();
        ordered.verify(offers).findActiveForUpdate(41);
        ordered.verify(notifications).notifyOfferExpired(offer);
        ordered.verify(connection).commit();
        verify(dataSource, times(2)).getConnection();
    }

    @Test
    void expireNow_shouldNotNotifyWhenExpirationRollsBack() throws Exception {
        OfferProposal offer = dueOffer(41);
        due(offer);
        when(offers.save(offer)).thenThrow(new DataAccessResourceFailureException("write failed"));

        assertThrows(DataAccessResourceFailureException.class, job::expireNow);

        verify(connection).rollback();
        verify(connection, never()).commit();
        verifyNoInteractions(notifications);
        verify(offers, never()).findActiveForUpdate(anyInt());
    }

    @Test
    void expireNow_shouldWaitForEnclosingTransactionAndNotNotifyAfterItsRollback() throws Exception {
        OfferProposal offer = dueOffer(41);
        due(offer);

        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            assertEquals(1, job.expireNow());
            verifyNoInteractions(notifications);
            status.setRollbackOnly();
        });

        verify(connection).rollback();
        verifyNoInteractions(notifications);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "Sent_Candidate,2026-11-13T11:59:59,2026-11-14,false,true",
            "Sent_Candidate,2026-11-13T12:00:00,2026-11-13,false,false",
            "Sent_Candidate,2026-11-13T12:00:01,2026-11-14,false,false",
            "Sent_Candidate,NULL,2026-11-12,false,true",
            "Sent_Candidate,NULL,2026-11-13,false,false",
            "Sent_Candidate,NULL,NULL,false,false",
            "Sent_Candidate,2026-11-13T11:59:59,2026-11-14,true,false",
            "Accepted,2026-11-13T11:59:59,2026-11-14,false,false",
            "Declined,2026-11-13T11:59:59,2026-11-14,false,false",
            "Draft,2026-11-13T11:59:59,2026-11-14,false,false",
            "Director_Rejected,2026-11-13T11:59:59,2026-11-14,false,false",
            "Expired,2026-11-13T11:59:59,2026-11-14,false,false"
    }, nullValues = "NULL")
    void expireNow_shouldRecheckStateAndTimeBeforePersisting(String state, String deadline, String start,
                                                           boolean deleted, boolean expires) {
        OfferProposal offer = OfferProposal.builder().offerId(41).offerStatus(state).isDeleted(deleted)
                .responseDeadline(deadline == null ? null : LocalDateTime.parse(deadline))
                .expectedStartDate(start == null ? null : LocalDate.parse(start)).build();
        due(offer);
        if (expires) {
            when(offers.findActiveForUpdate(41)).thenReturn(Optional.of(offer));
        }

        assertEquals(expires ? 1 : 0, job.expireNow());

        assertEquals(expires ? "Expired" : state, offer.getOfferStatus());
        if (expires) {
            verify(notifications).notifyOfferExpired(offer);
        } else {
            verify(offers, never()).save(any());
            verifyNoInteractions(notifications);
        }
    }

    @Test
    void expireNow_shouldNotNotifyTwiceOnRepeatedRun() {
        OfferProposal offer = dueOffer(41);
        when(offers.findDueForUpdate(eq(NOW), eq(NOW.toLocalDate()), any(Pageable.class)))
                .thenReturn(List.of(offer), List.of());
        when(offers.findActiveForUpdate(41)).thenReturn(Optional.of(offer));

        assertEquals(1, job.expireNow());
        assertEquals(0, job.expireNow());

        verify(notifications, times(1)).notifyOfferExpired(offer);
    }

    @Test
    void expireNow_shouldContinueAfterNotificationFailureWithoutRollingBackExpiration() throws Exception {
        OfferProposal first = dueOffer(41);
        OfferProposal second = dueOffer(42);
        when(offers.findDueForUpdate(eq(NOW), eq(NOW.toLocalDate()), any(Pageable.class)))
                .thenReturn(List.of(first, second));
        when(offers.findActiveForUpdate(41)).thenReturn(Optional.of(first));
        when(offers.findActiveForUpdate(42)).thenReturn(Optional.of(second));
        doThrow(new DataAccessResourceFailureException("notification failed"))
                .when(notifications).notifyOfferExpired(first);

        assertEquals(2, job.expireNow());

        verify(notifications).notifyOfferExpired(second);
        assertEquals("Expired", first.getOfferStatus());
        assertEquals("Expired", second.getOfferStatus());
        verify(connection, times(2)).commit();
        verify(connection).rollback();
    }

    @Test
    void expireNow_shouldSkipNotificationWhenRecordNoLongerExistsAfterCommit() {
        OfferProposal offer = dueOffer(41);
        due(offer);
        when(offers.findActiveForUpdate(41)).thenReturn(Optional.empty());

        assertEquals(1, job.expireNow());

        verifyNoInteractions(notifications);
    }

    @Test
    void expireNow_shouldProcessMoreThanOneBatch() {
        List<OfferProposal> firstBatch = IntStream.rangeClosed(1, 100).mapToObj(this::dueOffer).toList();
        OfferProposal last = dueOffer(101);
        when(offers.findDueForUpdate(eq(NOW), eq(NOW.toLocalDate()), any(Pageable.class)))
                .thenReturn(firstBatch, List.of(last));
        when(offers.findActiveForUpdate(anyInt())).thenAnswer(invocation -> {
            int id = invocation.getArgument(0);
            return Optional.of(id == 101 ? last : firstBatch.get(id - 1));
        });

        assertEquals(101, job.expireNow());

        verify(notifications, times(101)).notifyOfferExpired(any());
    }
}
