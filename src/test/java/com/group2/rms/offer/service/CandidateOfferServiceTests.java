package com.group2.rms.offer.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.service.ApplicationStatusService;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.offer.dto.CandidateOfferResponse;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.offer.exception.OfferResponseException;
import com.group2.rms.offer.repository.CandidateOfferRepository;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Ứng viên xem và trả lời offer. Repository là mock; câu query thật kiểm ở {@code CandidateOfferQueryTests}.
 */
@ExtendWith(MockitoExtension.class)
class CandidateOfferServiceTests {

    private static final int USER_ID = 41;
    private static final int OFFER_ID = 12;
    private static final int APPLICATION_ID = 300;

    @Mock CandidateOfferRepository offers;
    @Mock CurrentUserService currentUserService;
    @Mock ApplicationStatusService applicationStatusService;

    private CandidateOfferServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CandidateOfferServiceImpl(offers, currentUserService, applicationStatusService);
        when(currentUserService.requireUser()).thenReturn(User.builder().userId(USER_ID).build());
    }

    @Test
    void listMapsStatusToLabelBadgeAndAnswerableFlag() {
        when(offers.findVisibleByUserId(USER_ID, CandidateOfferServiceImpl.CANDIDATE_VISIBLE_STATUSES))
                .thenReturn(List.of(offer(3, "Sent_Candidate"), offer(2, "Accepted"), offer(1, "Declined")));

        List<CandidateOfferResponse> rows = service.list();

        assertEquals(List.of("Chờ bạn trả lời", "Đã nhận offer", "Đã từ chối"),
                rows.stream().map(CandidateOfferResponse::statusLabel).toList());
        assertEquals(List.of("badge--warning", "badge--success", "badge--neutral"),
                rows.stream().map(CandidateOfferResponse::statusBadge).toList());
        assertEquals(List.of(true, false, false),
                rows.stream().map(CandidateOfferResponse::canRespond).toList());
        assertEquals("Junior Java Developer", rows.getFirst().postingTitle());
    }

    @Test
    void listAsksOnlyForOffersAlreadySentToTheCandidate() {
        service.list();

        verify(offers).findVisibleByUserId(USER_ID, java.util.Set.of("Sent_Candidate", "Accepted", "Declined"));
    }

    @Test
    void acceptSetsOfferAcceptedAndMovesApplicationToHired() {
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(offer(OFFER_ID, "Sent_Candidate")));
        when(offers.answerIfSent(OFFER_ID, "Accepted")).thenReturn(1);

        String title = service.accept(OFFER_ID);

        assertEquals("Junior Java Developer", title);
        verify(applicationStatusService).onOfferAnswered(APPLICATION_ID, true);
    }

    @Test
    void declineSetsOfferDeclinedAndRejectsApplication() {
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(offer(OFFER_ID, "Sent_Candidate")));
        when(offers.answerIfSent(OFFER_ID, "Declined")).thenReturn(1);

        service.decline(OFFER_ID);

        verify(applicationStatusService).onOfferAnswered(APPLICATION_ID, false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Accepted", "Declined"})
    void answeringTwiceIsRefusedWithoutTouchingApplication(String status) {
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(offer(OFFER_ID, status)));

        assertThrows(OfferResponseException.class, () -> service.accept(OFFER_ID));
        assertThrows(OfferResponseException.class, () -> service.decline(OFFER_ID));

        verify(offers, never()).answerIfSent(anyInt(), anyString());
        verifyNoInteractions(applicationStatusService);
    }

    /** Hai lần bấm gần nhau: lần đọc thấy còn Sent_Candidate nhưng câu cập nhật có điều kiện trả 0. */
    @Test
    void failsWhenOfferWasAnsweredBetweenReadAndUpdate() {
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(offer(OFFER_ID, "Sent_Candidate")));
        when(offers.answerIfSent(OFFER_ID, "Accepted")).thenReturn(0);

        OfferResponseException error = assertThrows(OfferResponseException.class, () -> service.accept(OFFER_ID));

        assertEquals("Thư mời vừa được cập nhật. Vui lòng tải lại trang.", error.getMessage());
        verifyNoInteractions(applicationStatusService);
    }

    @Test
    void offerOfSomeoneElseOrNotYetSentIsNotFound() {
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.accept(OFFER_ID));
        assertThrows(ResourceNotFoundException.class, () -> service.decline(OFFER_ID));

        verify(offers, never()).answerIfSent(anyInt(), anyString());
        verifyNoInteractions(applicationStatusService);
    }

    private static OfferProposal offer(int id, String status) {
        return OfferProposal.builder()
                .offerId(id)
                .offerStatus(status)
                .offeredPositionTitle("Java Developer")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("18000000"))
                .application(Application.builder()
                        .applicationId(APPLICATION_ID)
                        .jobPosting(JobPosting.builder().jobPostingId(7).postingTitle("Junior Java Developer").build())
                        .build())
                .build();
    }
}
