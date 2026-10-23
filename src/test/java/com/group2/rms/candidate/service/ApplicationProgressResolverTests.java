package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.ApplicationProgressResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.group2.rms.candidate.service.ApplicationProgressStep.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Bảng trạng thái chi tiết (spec luồng trạng thái hồ sơ, mục 2.3): mỗi dòng bảng một test trong {@link Table},
 * thứ tự giống bảng. {@link DriftedData} là dữ liệu cũ chưa đổi trạng thái; {@link Edges} là ca biên.
 */
class ApplicationProgressResolverTests {

    private static final int ID = 298;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 13, 10, 0);
    private static final LocalDateTime TOMORROW = NOW.plusDays(1);
    private static final LocalDateTime YESTERDAY = NOW.minusDays(1);

    @Nested
    class Table {
        @Test
        void suggestionWaitsForHrContactAndCannotBeScheduled() {
            var progress = resolve(status("HM_Suggested"));
            assertEquals(HM_SUGGESTED, progress.step());
            assertEquals(ApplicationStage.SCREENING, progress.stage());
            assertEquals("HR", progress.ownerLabel());
        }

        @Test
        void applied_awaitsAiScreening() {
            ApplicationProgressResponse progress = resolve(status("Applied"));

            assertProgress(progress, ApplicationStage.APPLIED, AWAITING_AI, "Chờ AI chấm CV", "Hệ thống", "neutral");
        }

        @Test
        void aiScreened_awaitsHrReview() {
            ApplicationProgressResponse progress = resolve(status("AI_Screened"));

            assertProgress(progress, ApplicationStage.SCREENING, AWAITING_HR_REVIEW, "Chờ HR duyệt CV", "HR", "warning");
        }

        @Test
        void hrPassed_awaitsHiringManagerReview() {
            ApplicationProgressResponse progress = resolve(status("HR_Passed"));

            assertProgress(progress, ApplicationStage.SCREENING, AWAITING_HM_REVIEW,
                    "Chờ trưởng bộ phận duyệt CV", "Trưởng bộ phận", "warning");
        }

        @Test
        void hiringManagerPassedWithoutSchedule_awaitsScheduling() {
            ApplicationProgressResponse progress = resolve(status("HM_Passed"));

            assertProgress(progress, ApplicationStage.INTERVIEW, AWAITING_SCHEDULE,
                    "Chờ xếp lịch phỏng vấn", "HR", "warning");
        }

        @Test
        void interviewingWithoutActiveSchedule_awaitsScheduling() {
            ApplicationProgressResponse progress = resolve(status("Interviewing"));

            assertEquals(AWAITING_SCHEDULE, progress.step());
        }

        @Test
        void upcomingSchedule_showsInterviewDate() {
            ApplicationProgressResponse progress = resolve(
                    interview("Interviewing", "Scheduled", LocalDateTime.of(2026, 10, 20, 9, 0), 3, 0, null));

            assertProgress(progress, ApplicationStage.INTERVIEW, INTERVIEW_SCHEDULED,
                    "Đã xếp lịch, phỏng vấn ngày 20/10", "Hội đồng", "brand");
        }

        @Test
        void interviewHeldWithMissingEvaluations_awaitsScores() {
            ApplicationProgressResponse progress = resolve(interview("Interviewing", "Scheduled", YESTERDAY, 3, 1, null));

            assertProgress(progress, ApplicationStage.INTERVIEW, AWAITING_EVALUATIONS,
                    "Chờ chấm điểm (1/3)", "Người phỏng vấn", "warning");
        }

        @Test
        void interviewHeldWithAllEvaluations_awaitsDecision() {
            ApplicationProgressResponse progress = resolve(interview("Interviewing", "Completed", YESTERDAY, 3, 3, null));

            assertProgress(progress, ApplicationStage.INTERVIEW, AWAITING_DECISION,
                    "Chờ chốt kết quả phỏng vấn", "Trưởng bộ phận", "warning");
        }

        @Test
        void consideringIsShownAsAwaitingDecision() {
            // HM đang cân nhắc: ứng viên chỉ thấy "Chờ chốt kết quả", không lộ quyết định nội bộ (GBR-02)
            ApplicationProgressResponse progress = resolve(
                    interview("Interviewing", "Completed", YESTERDAY, 3, 3, "Considering"));

            assertProgress(progress, ApplicationStage.INTERVIEW, AWAITING_DECISION,
                    "Chờ chốt kết quả phỏng vấn", "Trưởng bộ phận", "warning");
        }

        @Test
        void passedWithoutOffer_awaitsOffer() {
            ApplicationProgressResponse progress = resolve(
                    interview("Interviewing", "Completed", YESTERDAY, 3, 3, "Passed"));

            assertProgress(progress, ApplicationStage.OFFER, PASSED_AWAITING_OFFER,
                    "Đạt phỏng vấn, chờ lập offer", "HR", "warning");
        }

        @Test
        void draftOffer() {
            ApplicationProgressResponse progress = resolve(offer("Interviewing", "Draft"));

            assertProgress(progress, ApplicationStage.OFFER, OFFER_DRAFT, "Offer đang soạn", "HR", "warning");
        }

        @Test
        void offerPendingDirector() {
            ApplicationProgressResponse progress = resolve(offer("Interviewing", "Pending_Director"));

            assertProgress(progress, ApplicationStage.OFFER, OFFER_AWAITING_DIRECTOR,
                    "Chờ giám đốc duyệt offer", "Giám đốc", "warning");
        }

        @Test
        void offerReturnedByDirector() {
            ApplicationProgressResponse progress = resolve(offer("Interviewing", "Director_Rejected"));

            assertProgress(progress, ApplicationStage.OFFER, OFFER_RETURNED,
                    "Giám đốc trả offer, HR sửa lại", "HR", "danger");
        }

        @Test
        void offerApprovedByDirector_awaitsSending() {
            ApplicationProgressResponse progress = resolve(offer("Interviewing", "Director_Approved"));

            assertProgress(progress, ApplicationStage.OFFER, OFFER_AWAITING_SEND, "Chờ HR gửi offer", "HR", "warning");
        }

        @Test
        void offerSent_awaitsCandidate() {
            ApplicationProgressResponse progress = resolve(offer("Offered", "Sent_Candidate"));

            assertProgress(progress, ApplicationStage.OFFER, OFFER_SENT,
                    "Đã gửi offer, chờ ứng viên trả lời", "Ứng viên", "brand");
        }

        @Test
        void hired() {
            ApplicationProgressResponse progress = resolve(offer("Hired", "Accepted"));

            assertProgress(progress, ApplicationStage.HIRED, HIRED_DONE, "Đã nhận việc", null, "success");
        }

        @Test
        void withdrawnByCandidate() {
            ApplicationProgressResponse progress = resolve(rejected(true, null, null));

            assertProgress(progress, ApplicationStage.CLOSED, WITHDRAWN, "Ứng viên đã rút hồ sơ", null, "neutral");
        }

        @Test
        void offerDeclinedByCandidate() {
            ApplicationProgressResponse progress = resolve(rejected(false, "Passed", "Declined"));

            assertProgress(progress, ApplicationStage.CLOSED, OFFER_DECLINED, "Ứng viên từ chối offer", null, "neutral");
        }

        @Test
        void failedInterview() {
            ApplicationProgressResponse progress = resolve(rejected(false, "Failed", null));

            assertProgress(progress, ApplicationStage.CLOSED, INTERVIEW_FAILED, "Không đạt phỏng vấn", null, "danger");
        }

        @Test
        void otherRejection_failedCvRound() {
            ApplicationProgressResponse progress = resolve(rejected(false, null, null));

            assertProgress(progress, ApplicationStage.CLOSED, CV_REJECTED, "Không qua vòng CV", null, "danger");
        }
    }

    /** Dữ liệu cũ: con dấu trạng thái chưa theo kịp việc đã xảy ra. Hiển thị theo việc thật. */
    @Nested
    class DriftedData {

        @Test
        void hiringManagerPassedWithUpcomingSchedule_showsScheduled() {
            assertEquals(INTERVIEW_SCHEDULED, resolve(interview("HM_Passed", "Scheduled", TOMORROW, 2, 0, null)).step());
        }

        @Test
        void hiringManagerPassedAlreadyFailed_isClosed() {
            assertEquals(INTERVIEW_FAILED, resolve(interview("HM_Passed", "Completed", YESTERDAY, 2, 2, "Failed")).step());
        }

        @Test
        void interviewingAlreadyFailed_isClosed() {
            ApplicationProgressResponse progress = resolve(
                    interview("Interviewing", "Completed", YESTERDAY, 2, 2, "Failed"));

            assertEquals(INTERVIEW_FAILED, progress.step());
            assertEquals(ApplicationStage.CLOSED, progress.stage());
        }

        @Test
        void offeredButOfferAccepted_isHired() {
            assertEquals(HIRED_DONE, resolve(offer("Offered", "Accepted")).step());
        }

        @Test
        void offeredButOfferDeclined_isClosed() {
            assertEquals(OFFER_DECLINED, resolve(offer("Offered", "Declined")).step());
        }

        @Test
        void offeredWithoutOfferRow_trustsStatus() {
            assertEquals(OFFER_SENT, resolve(status("Offered")).step());
        }
    }

    @Nested
    class Edges {

        @Test
        void rescheduledUpcomingInterviewCountsAsScheduled() {
            assertEquals(INTERVIEW_SCHEDULED, resolve(interview("Interviewing", "Rescheduled", TOMORROW, 2, 0, null)).step());
        }

        @Test
        void scheduledButAlreadyStarted_awaitsScores() {
            assertEquals(AWAITING_EVALUATIONS, resolve(interview("Interviewing", "Scheduled", NOW, 2, 0, null)).step());
        }

        @Test
        void completedInterviewIsNeverUpcoming() {
            assertEquals(AWAITING_EVALUATIONS, resolve(interview("Interviewing", "Completed", TOMORROW, 2, 1, null)).step());
        }

        @Test
        void emptyPanel_awaitsDecision() {
            assertEquals(AWAITING_DECISION, resolve(interview("Interviewing", "Completed", YESTERDAY, 0, 0, null)).step());
        }

        @Test
        void legacyApprovedOfferStatus_awaitsSending() {
            assertEquals(OFFER_AWAITING_SEND, resolve(offer("Interviewing", "Approved")).step());
        }

        @Test
        void unknownOfferStatusFallsBackToInterviewFacts() {
            assertEquals(PASSED_AWAITING_OFFER, resolve(offer("Interviewing", "Voided")).step());
        }

        @Test
        void withdrawalWinsOverOtherRejectionReasons() {
            assertEquals(WITHDRAWN, resolve(rejected(true, "Failed", "Declined")).step());
        }

        @Test
        void declinedOfferWinsOverInterviewResult() {
            assertEquals(OFFER_DECLINED, resolve(rejected(false, "Failed", "Declined")).step());
        }

        @Test
        void brandToneUsesSuccessBadge() {
            assertEquals("badge--success", resolve(offer("Offered", "Sent_Candidate")).badge());
            assertEquals("badge--warning", resolve(status("HM_Passed")).badge());
            assertEquals("badge--danger", resolve(rejected(false, null, null)).badge());
        }
    }

    private static ApplicationProgressResponse resolve(ApplicationProgressFacts facts) {
        return ApplicationProgressResolver.resolve(facts, NOW);
    }

    private static void assertProgress(ApplicationProgressResponse progress, ApplicationStage stage,
                                       ApplicationProgressStep step, String label, String owner, String tone) {
        assertAll(
                () -> assertEquals(stage, progress.stage()),
                () -> assertEquals(step, progress.step()),
                () -> assertEquals(label, progress.detailLabel()),
                () -> assertEquals(owner, progress.ownerLabel()),
                () -> assertEquals(tone, progress.tone()));
    }

    private static ApplicationProgressFacts status(String status) {
        return new ApplicationProgressFacts(ID, status, false, null, null, null, 0, 0, null, null);
    }

    private static ApplicationProgressFacts interview(String status, String interviewStatus, LocalDateTime start,
                                                      int panel, int evaluations, String decision) {
        return new ApplicationProgressFacts(ID, status, false, null, interviewStatus, start, panel, evaluations, decision, null);
    }

    /** Đã phỏng vấn đạt rồi mới có offer. */
    private static ApplicationProgressFacts offer(String status, String offerStatus) {
        return new ApplicationProgressFacts(ID, status, false, null, "Completed", YESTERDAY, 2, 2, "Passed", offerStatus);
    }

    private static ApplicationProgressFacts rejected(boolean withdrawn, String decision, String offerStatus) {
        return new ApplicationProgressFacts(ID, "Rejected", withdrawn, null, decision == null ? null : "Completed",
                decision == null ? null : YESTERDAY, 2, 2, decision, offerStatus);
    }
}
