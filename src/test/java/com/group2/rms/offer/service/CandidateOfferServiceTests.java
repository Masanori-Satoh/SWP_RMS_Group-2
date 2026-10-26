package com.group2.rms.offer.service;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.rule.ApplicationStatusService;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.offer.dto.CandidateOfferResponse;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.offer.exception.OfferResponseException;
import com.group2.rms.offer.repository.CandidateOfferRepository;
import com.group2.rms.jobposting.entity.JobPosting;
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
    @Mock com.group2.rms.notification.NotificationService systemNotificationService;

    private CandidateOfferServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CandidateOfferServiceImpl(offers, currentUserService, applicationStatusService, systemNotificationService);
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

    @Test
    void getDetailReturnsFullOfferLetterData() {
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(offer(OFFER_ID, "Sent_Candidate")));

        var detail = service.getDetail(OFFER_ID);

        assertNotNull(detail);
        assertEquals(OFFER_ID, detail.offerId());
        assertEquals("Junior Java Developer", detail.postingTitle());
        assertEquals("Java Developer", detail.offeredPositionTitle());
        assertEquals(new BigDecimal("20000000"), detail.proposedSalary());
        assertEquals(new BigDecimal("18000000"), detail.probationSalary());
        assertEquals(90, detail.probationRatePercent());
        assertTrue(detail.canRespond());
    }

    @Test
    void getDetailThrowsNotFoundWhenOfferDoesNotExistOrBelongsToOther() {
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getDetail(OFFER_ID));
    }

    @Test
    void declineWithReasonSetsOfferDeclinedAndRejectsApplication() {
        OfferProposal testOffer = offer(OFFER_ID, "Sent_Candidate");
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(testOffer));
        when(offers.answerIfSent(OFFER_ID, "Declined", "Mức đãi ngộ chưa đạt kỳ vọng")).thenReturn(1);

        String title = service.decline(OFFER_ID, "Mức đãi ngộ chưa đạt kỳ vọng");

        assertEquals("Junior Java Developer", title);
        verify(applicationStatusService).onOfferAnswered(APPLICATION_ID, false);
        verify(systemNotificationService).notifyOfferResponseFromCandidate(testOffer, false, "Mức đãi ngộ chưa đạt kỳ vọng");
    }

    @Test
    void acceptNotifiesHrProposedBy() {
        OfferProposal testOffer = offer(OFFER_ID, "Sent_Candidate");
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(testOffer));
        when(offers.answerIfSent(OFFER_ID, "Accepted")).thenReturn(1);

        String title = service.accept(OFFER_ID);

        assertEquals("Junior Java Developer", title);
        verify(applicationStatusService).onOfferAnswered(APPLICATION_ID, true);
        verify(systemNotificationService).notifyOfferResponseFromCandidate(testOffer, true, null);
    }

    @Test
    void listMapsExpiredOfferToAppropriateLabelAndDisablesResponse() {
        OfferProposal expiredOffer = offer(3, "Sent_Candidate");
        expiredOffer.setExpectedStartDate(java.time.LocalDate.now().minusDays(1));

        when(offers.findVisibleByUserId(USER_ID, CandidateOfferServiceImpl.CANDIDATE_VISIBLE_STATUSES))
                .thenReturn(List.of(expiredOffer));

        List<CandidateOfferResponse> rows = service.list();

        assertEquals(1, rows.size());
        assertEquals("Đã quá hạn phản hồi", rows.get(0).statusLabel());
        assertEquals("badge--neutral", rows.get(0).statusBadge());
        assertFalse(rows.get(0).canRespond());
    }

    @Test
    void getDetailMapsExpiredOfferToAppropriateLabelAndDisablesResponse() {
        OfferProposal expiredOffer = offer(OFFER_ID, "Sent_Candidate");
        expiredOffer.setExpectedStartDate(java.time.LocalDate.now().minusDays(2));

        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(expiredOffer));

        var detail = service.getDetail(OFFER_ID);

        assertEquals("Đã quá hạn phản hồi", detail.statusLabel());
        assertEquals("badge--neutral", detail.statusBadge());
        assertFalse(detail.canRespond());
    }

    @Test
    void acceptExpiredOfferThrowsOfferResponseException() {
        OfferProposal expiredOffer = offer(OFFER_ID, "Sent_Candidate");
        expiredOffer.setExpectedStartDate(java.time.LocalDate.now().minusDays(1));

        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(expiredOffer));

        var ex = assertThrows(OfferResponseException.class, () -> service.accept(OFFER_ID));
        assertTrue(ex.getMessage().contains("đã quá ngày bắt đầu"));
        verify(offers, never()).answerIfSent(anyInt(), anyString());
    }

    @Test
    void declineExpiredOfferThrowsOfferResponseException() {
        OfferProposal expiredOffer = offer(OFFER_ID, "Sent_Candidate");
        expiredOffer.setExpectedStartDate(java.time.LocalDate.now().minusDays(1));

        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(expiredOffer));

        var ex = assertThrows(OfferResponseException.class, () -> service.decline(OFFER_ID, "Lý do"));
        assertTrue(ex.getMessage().contains("đã quá ngày bắt đầu"));
        verify(offers, never()).answerIfSent(anyInt(), anyString(), anyString());
    }

    @Test
    void acceptExpiredOfferWithResponseDeadlineThrowsOfferResponseException() {
        OfferProposal expiredOffer = offer(OFFER_ID, "Sent_Candidate");
        expiredOffer.setResponseDeadline(java.time.LocalDateTime.now().minusHours(2));

        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(expiredOffer));

        var ex = assertThrows(OfferResponseException.class, () -> service.accept(OFFER_ID));
        assertTrue(ex.getMessage().contains("đã quá thời hạn phản hồi"));
        verify(offers, never()).answerIfSent(anyInt(), anyString());
    }

    @Test
    void declineExpiredOfferWithResponseDeadlineThrowsOfferResponseException() {
        OfferProposal expiredOffer = offer(OFFER_ID, "Sent_Candidate");
        expiredOffer.setResponseDeadline(java.time.LocalDateTime.now().minusHours(2));

        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(expiredOffer));

        var ex = assertThrows(OfferResponseException.class, () -> service.decline(OFFER_ID, "Lý do"));
        assertTrue(ex.getMessage().contains("đã quá thời hạn phản hồi"));
        verify(offers, never()).answerIfSent(anyInt(), anyString(), anyString());
    }

    @Test
    void acceptOfferWithFutureResponseDeadlineSucceeds() {
        OfferProposal validOffer = offer(OFFER_ID, "Sent_Candidate");
        validOffer.setResponsePeriodDays(5);
        validOffer.setResponseDeadline(java.time.LocalDateTime.now().plusDays(3));

        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(validOffer));
        when(offers.answerIfSent(OFFER_ID, "Accepted")).thenReturn(1);

        String title = service.accept(OFFER_ID);

        assertEquals("Junior Java Developer", title);
        verify(applicationStatusService).onOfferAnswered(APPLICATION_ID, true);
    }

    @Test
    void listAndDetailMapResponseDeadlineProperly() {
        java.time.LocalDateTime deadline = java.time.LocalDateTime.now().plusDays(4);
        OfferProposal validOffer = offer(OFFER_ID, "Sent_Candidate");
        validOffer.setResponsePeriodDays(7);
        validOffer.setResponseDeadline(deadline);

        when(offers.findVisibleByUserId(USER_ID, CandidateOfferServiceImpl.CANDIDATE_VISIBLE_STATUSES))
                .thenReturn(List.of(validOffer));
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(validOffer));

        List<CandidateOfferResponse> list = service.list();
        assertEquals(1, list.size());
        assertEquals(7, list.get(0).responsePeriodDays());
        assertEquals(deadline, list.get(0).responseDeadline());

        var detail = service.getDetail(OFFER_ID);
        assertEquals(7, detail.responsePeriodDays());
        assertEquals(deadline, detail.responseDeadline());
    }

    // =========================================================================
    // EXTENDED UNIT TESTS (English)
    // =========================================================================

    @Test
    @org.junit.jupiter.api.DisplayName("isExpired: When current time is immediately past response deadline, response is blocked")
    void acceptOffer_immediatelyPastResponseDeadline_throwsException() {
        OfferProposal offer = offer(OFFER_ID, "Sent_Candidate");
        offer.setResponseDeadline(java.time.LocalDateTime.now().minusSeconds(1));

        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(offer));

        OfferResponseException ex = assertThrows(OfferResponseException.class, () -> service.accept(OFFER_ID));
        assertTrue(ex.getMessage().contains("quá thời hạn phản hồi"));
        verify(offers, never()).answerIfSent(anyInt(), anyString());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("isExpired: When responseDeadline is null and expectedStartDate is today, response is allowed")
    void acceptOffer_whenResponseDeadlineIsNullAndExpectedStartDateIsToday_succeeds() {
        OfferProposal offer = offer(OFFER_ID, "Sent_Candidate");
        offer.setResponseDeadline(null);
        offer.setExpectedStartDate(java.time.LocalDate.now());

        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(offer));
        when(offers.answerIfSent(OFFER_ID, "Accepted")).thenReturn(1);

        String title = service.accept(OFFER_ID);

        assertEquals("Junior Java Developer", title);
        verify(applicationStatusService).onOfferAnswered(APPLICATION_ID, true);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("isExpired: When responseDeadline is null and expectedStartDate is in future, response is allowed")
    void acceptOffer_whenResponseDeadlineIsNullAndExpectedStartDateInFuture_succeeds() {
        OfferProposal offer = offer(OFFER_ID, "Sent_Candidate");
        offer.setResponseDeadline(null);
        offer.setExpectedStartDate(java.time.LocalDate.now().plusDays(2));

        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(offer));
        when(offers.answerIfSent(OFFER_ID, "Accepted")).thenReturn(1);

        String title = service.accept(OFFER_ID);

        assertEquals("Junior Java Developer", title);
        verify(applicationStatusService).onOfferAnswered(APPLICATION_ID, true);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("isExpired: When responseDeadline is in future but expectedStartDate is in past, response is blocked")
    void acceptOffer_whenExpectedStartDatePassedEvenIfDeadlineInFuture_throwsException() {
        OfferProposal offer = offer(OFFER_ID, "Sent_Candidate");
        offer.setResponseDeadline(java.time.LocalDateTime.now().plusDays(2));
        offer.setExpectedStartDate(java.time.LocalDate.now().minusDays(1));

        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(offer));

        OfferResponseException ex = assertThrows(OfferResponseException.class, () -> service.accept(OFFER_ID));
        assertTrue(ex.getMessage().contains("quá ngày bắt đầu nhận việc dự kiến"));
        verify(offers, never()).answerIfSent(anyInt(), anyString());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("isExpired: When responseDeadline is null and expectedStartDate is in past, decline is blocked")
    void declineOffer_whenExpectedStartDatePassedAndDeadlineNull_throwsException() {
        OfferProposal offer = offer(OFFER_ID, "Sent_Candidate");
        offer.setResponseDeadline(null);
        offer.setExpectedStartDate(java.time.LocalDate.now().minusDays(2));

        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(offer));

        OfferResponseException ex = assertThrows(OfferResponseException.class, () -> service.decline(OFFER_ID, "Lý do"));
        assertTrue(ex.getMessage().contains("quá ngày bắt đầu nhận việc dự kiến"));
        verify(offers, never()).answerIfSent(anyInt(), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @org.junit.jupiter.api.DisplayName("decline: Blank or whitespace declineReason is normalized to null and accepted")
    void declineOffer_blankReason_treatedAsNullAndSucceeds(String blankReason) {
        OfferProposal testOffer = offer(OFFER_ID, "Sent_Candidate");
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(testOffer));
        when(offers.answerIfSent(OFFER_ID, "Declined")).thenReturn(1);

        String title = service.decline(OFFER_ID, blankReason);

        assertEquals("Junior Java Developer", title);
        verify(offers).answerIfSent(OFFER_ID, "Declined");
        verify(applicationStatusService).onOfferAnswered(APPLICATION_ID, false);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("decline: Null declineReason succeeds and calls 2-arg answerIfSent")
    void declineOffer_nullReason_succeeds() {
        OfferProposal testOffer = offer(OFFER_ID, "Sent_Candidate");
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(testOffer));
        when(offers.answerIfSent(OFFER_ID, "Declined")).thenReturn(1);

        String title = service.decline(OFFER_ID, null);

        assertEquals("Junior Java Developer", title);
        verify(offers).answerIfSent(OFFER_ID, "Declined");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("decline: Exactly 1000 characters declineReason is accepted and passed to repository")
    void declineOffer_exactly1000CharactersReason_succeeds() {
        String longReason = "a".repeat(1000);
        OfferProposal testOffer = offer(OFFER_ID, "Sent_Candidate");
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(testOffer));
        when(offers.answerIfSent(OFFER_ID, "Declined", longReason)).thenReturn(1);

        String title = service.decline(OFFER_ID, longReason);

        assertEquals("Junior Java Developer", title);
        verify(offers).answerIfSent(OFFER_ID, "Declined", longReason);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("accept: Attempting to accept an already declined offer is refused")
    void acceptOffer_afterDeclining_throwsOfferResponseException() {
        OfferProposal testOffer = offer(OFFER_ID, "Declined");
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(testOffer));

        OfferResponseException ex = assertThrows(OfferResponseException.class, () -> service.accept(OFFER_ID));
        assertTrue(ex.getMessage().contains("đã được trả lời"));
        verify(offers, never()).answerIfSent(anyInt(), anyString());
        verifyNoInteractions(applicationStatusService);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("decline: Attempting to decline an already accepted offer is refused")
    void declineOffer_afterAccepting_throwsOfferResponseException() {
        OfferProposal testOffer = offer(OFFER_ID, "Accepted");
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(testOffer));

        OfferResponseException ex = assertThrows(OfferResponseException.class, () -> service.decline(OFFER_ID, "Change mind"));
        assertTrue(ex.getMessage().contains("đã được trả lời"));
        verify(offers, never()).answerIfSent(anyInt(), anyString(), anyString());
        verifyNoInteractions(applicationStatusService);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("getDetail: Throws ResourceNotFoundException when candidate accesses offer of another user")
    void candidateCannotAccessOfferOwnedByAnotherUser_getDetail() {
        when(offers.findVisibleById(eq(999), eq(USER_ID), anyCollection())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getDetail(999));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("accept: When notification service throws exception, accept still succeeds")
    void acceptOffer_whenNotificationThrows_stillCompletesAcceptance() {
        OfferProposal testOffer = offer(OFFER_ID, "Sent_Candidate");
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(testOffer));
        when(offers.answerIfSent(OFFER_ID, "Accepted")).thenReturn(1);
        doThrow(new RuntimeException("Notification service failure"))
                .when(systemNotificationService).notifyOfferResponseFromCandidate(any(), eq(true), any());

        String title = service.accept(OFFER_ID);

        assertEquals("Junior Java Developer", title);
        verify(applicationStatusService).onOfferAnswered(APPLICATION_ID, true);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("decline: When notification service throws exception, decline still succeeds")
    void declineOffer_whenNotificationThrows_stillCompletesDecline() {
        OfferProposal testOffer = offer(OFFER_ID, "Sent_Candidate");
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(testOffer));
        when(offers.answerIfSent(OFFER_ID, "Declined")).thenReturn(1);
        doThrow(new RuntimeException("Notification service failure"))
                .when(systemNotificationService).notifyOfferResponseFromCandidate(any(), eq(false), any());

        String title = service.decline(OFFER_ID);

        assertEquals("Junior Java Developer", title);
        verify(applicationStatusService).onOfferAnswered(APPLICATION_ID, false);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("decline: When offer was concurrently answered between read and update, throws exception and leaves application untouched")
    void declineOffer_failsWhenOfferWasAnsweredBetweenReadAndUpdate() {
        OfferProposal testOffer = offer(OFFER_ID, "Sent_Candidate");
        when(offers.findVisibleById(eq(OFFER_ID), eq(USER_ID), anyCollection()))
                .thenReturn(Optional.of(testOffer));
        when(offers.answerIfSent(OFFER_ID, "Declined", "reason")).thenReturn(0);

        OfferResponseException ex = assertThrows(OfferResponseException.class, () ->
                service.decline(OFFER_ID, "reason"));

        assertEquals("Thư mời vừa được cập nhật. Vui lòng tải lại trang.", ex.getMessage());
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

