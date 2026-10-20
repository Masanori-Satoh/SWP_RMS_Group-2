package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.MyApplicationStep;
import com.group2.rms.candidate.service.MyApplicationJourney.Facts;
import com.group2.rms.candidate.service.MyApplicationJourney.Interview;
import com.group2.rms.candidate.service.MyApplicationJourney.Mark;
import com.group2.rms.candidate.service.MyApplicationJourney.Offer;
import com.group2.rms.candidate.service.MyApplicationJourney.View;
import com.group2.rms.core.web.TimelineItem;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bảng Stage + Status của My Applications (spec My Applications mục 2.2): mỗi dòng bảng một test trong {@link Table},
 * thứ tự giống bảng. {@link ClosedPosting} là vòng dừng khi đóng tin, {@link Journey} là hành trình (mục 2.3).
 */
class MyApplicationJourneyTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final LocalDateTime SUBMITTED = LocalDateTime.of(2026, 10, 1, 8, 30);
    private static final LocalDateTime PAST = LocalDateTime.of(2026, 10, 10, 9, 0);
    private static final LocalDateTime FUTURE = LocalDateTime.of(2026, 11, 24, 10, 0);

    @Nested
    class Table {
        @Test
        void row01_withdrawn_stopsAtScreeningWithWithdrawalDate() {
            View view = resolve(facts("Rejected").marks(mark("Withdrawn", "AI_Screened", LocalDateTime.of(2026, 10, 5, 14, 0))));

            assertStatus(view, "Đã tự rút hồ sơ", "Bạn đã rút hồ sơ ngày 05/10.", "neutral", null, null);
            assertSteps(view, "done", "closed", "upcoming", "upcoming");
        }

        @Test
        void row02_postingClosed_pointsToOpenJobs() {
            View view = resolve(facts("Rejected").marks(mark("PostingClosed", "AI_Screened", PAST)));

            assertStatus(view, "Vị trí đã ngừng tuyển",
                    "Công ty đã ngừng tuyển vị trí này nên hồ sơ của bạn dừng lại. Bạn có thể ứng tuyển vị trí khác.",
                    "neutral", "Xem vị trí đang tuyển", "/jobs");
            assertSteps(view, "done", "closed", "upcoming", "upcoming");
        }

        @Test
        void row03_offerDeclined_stopsAtOffer() {
            View view = resolve(facts("Rejected").interviews(completed()).offer(offer(7, "Declined")));

            assertStatus(view, "Đã từ chối thư mời", null, "neutral", "Xem thư mời", "/portal/offers/7");
            assertSteps(view, "done", "done", "done", "closed");
        }

        @Test
        void row04_rejectedAfterInterview_stopsAtInterview() {
            View view = resolve(facts("Rejected").interviews(completed()));

            assertStatus(view, "Không đạt phỏng vấn", "Cảm ơn bạn đã tham gia phỏng vấn.", "danger", null, null);
            assertSteps(view, "done", "done", "closed", "upcoming");
        }

        @Test
        void row05_rejectedWithoutInterview_stopsAtScreening() {
            View view = resolve(facts("Rejected").interviews(new Interview(FUTURE, "Offline_Office", "Cancelled")));

            assertStatus(view, "Không đạt vòng CV", "Hồ sơ chưa phù hợp với vị trí này.", "danger", null, null);
            assertSteps(view, "done", "closed", "upcoming", "upcoming");
        }

        @Test
        void row06_hired_finishesAllRoundsAndLinksTheOffer() {
            View view = resolve(facts("Hired").interviews(completed()).offer(offer(8, "Accepted")));

            assertStatus(view, "Đã nhận việc", "Chúc mừng bạn!", "success", "Xem thư mời", "/portal/offers/8");
            assertSteps(view, "done", "done", "done", "done");
        }

        @Test
        void row07_offerSent_asksForAnswerBeforeDeadline() {
            View view = resolve(facts("Offered").interviews(completed()).offer(offer(3, "Sent_Candidate")));

            assertStatus(view, "Có thư mời", "Vui lòng trả lời trước 17/10.", "warning",
                    "Trả lời thư mời", "/portal/offers/3");
            assertSteps(view, "done", "done", "done", "current");
        }

        @Test
        void row08_upcomingInterview_showsTimeFormatAndSchedule() {
            View view = resolve(facts("Interviewing").interviews(new Interview(FUTURE, "Online_GoogleMeet", "Scheduled")));

            assertStatus(view, "Phỏng vấn 10:00 24/11", "Trực tuyến. Xem chi tiết ở Lịch phỏng vấn.", "warning",
                    "Xem lịch phỏng vấn", "/portal/interviews");
            assertSteps(view, "done", "done", "current", "upcoming");
        }

        @Test
        void row09_interviewDone_waitsForResult() {
            View view = resolve(facts("HM_Passed").interviews(new Interview(PAST, "Offline_Office", "Scheduled")));

            assertStatus(view, "Chờ kết quả", "Bạn đã phỏng vấn xong, kết quả sẽ được thông báo.", "neutral",
                    "Xem lịch phỏng vấn", "/portal/interviews");
            assertSteps(view, "done", "done", "current", "upcoming");
        }

        @Test
        void row10_cvPassedWithoutSchedule_waitsForAppointment() {
            View view = resolve(facts("HM_Passed"));

            assertStatus(view, "CV đạt yêu cầu", "Bộ phận tuyển dụng sẽ hẹn lịch phỏng vấn với bạn.", "success",
                    null, null);
            assertSteps(view, "done", "done", "current", "upcoming");
        }

        @Test
        void row11_hrPassed_departmentReviewsCv() {
            View view = resolve(facts("HR_Passed"));

            assertStatus(view, "Chờ xét CV", "Bộ phận chuyên môn đang xem CV của bạn.", "neutral", null, null);
            assertSteps(view, "done", "current", "upcoming", "upcoming");
        }

        @Test
        void row12_aiScreened_recruitmentReviewsCv() {
            View view = resolve(facts("AI_Screened"));

            assertStatus(view, "Chờ xét CV", "Bộ phận tuyển dụng đang xem CV của bạn.", "neutral", null, null);
            assertSteps(view, "done", "current", "upcoming", "upcoming");
        }

        @Test
        void row13_applied_systemScreensCv() {
            View view = resolve(facts("Applied"));

            assertStatus(view, "Đã nộp CV", "Hệ thống đang sàng lọc CV của bạn.", "neutral", null, null);
            assertSteps(view, "done", "current", "upcoming", "upcoming");
        }

        @Test
        void row14_offeredWithoutVisibleOffer_saysRecruitmentWillContact() {
            View view = resolve(facts("Offered").interviews(completed()));

            assertStatus(view, "Có thư mời", "Bộ phận tuyển dụng sẽ liên hệ với bạn.", "neutral", null, null);
            assertSteps(view, "done", "done", "done", "current");
        }
    }

    @Nested
    class Edges {
        @Test
        void withdrawalWinsOverOtherReasons() {
            View view = resolve(facts("Rejected").interviews(completed())
                    .marks(mark("Withdrawn", "Applied", PAST), mark("PostingClosed", "Applied", PAST)));

            assertEquals("Đã tự rút hồ sơ", view.statusLabel());
        }

        @Test
        void withdrawalMarkOnAnApplicationStillInFlowIsIgnored() {
            View view = resolve(facts("AI_Screened").marks(mark("Withdrawn", "AI_Screened", PAST)));

            assertEquals("Chờ xét CV", view.statusLabel());
        }

        @Test
        void offerWithoutDeadlineStillAsksForAnswer() {
            Offer noDeadline = new Offer(3, "Sent_Candidate", null, PAST);
            View view = resolve(facts("Offered").offer(noDeadline));

            assertEquals("Vui lòng xem và trả lời thư mời.", view.statusNote());
        }

        @Test
        void nextUpcomingInterviewIsShownWhenSeveralAreScheduled() {
            View view = resolve(facts("Interviewing").interviews(
                    completed(),
                    new Interview(FUTURE.plusDays(3), "Online_GoogleMeet", "Scheduled"),
                    new Interview(FUTURE, "Offline_Office", "Rescheduled")));

            assertEquals("Phỏng vấn 10:00 24/11", view.statusLabel());
            assertEquals("Tại văn phòng. Xem chi tiết ở Lịch phỏng vấn.", view.statusNote());
        }

        @Test
        void cancelledInterviewOnlyMeansNoScheduleYet() {
            View view = resolve(facts("HM_Passed").interviews(new Interview(FUTURE, "Offline_Office", "Cancelled")));

            assertEquals("CV đạt yêu cầu", view.statusLabel());
        }

        @Test
        void hiredWithoutVisibleOfferHasNoOfferButton() {
            View view = resolve(facts("Hired"));

            assertNull(view.actionLabel());
            assertNull(view.actionHref());
        }

        @Test
        void unknownStatusIsShownAsInProgress() {
            View view = resolve(facts("Something_New"));

            assertEquals("Đang xử lý", view.statusLabel());
            assertSteps(view, "done", "current", "upcoming", "upcoming");
        }
    }

    @Nested
    class StepTag {
        @Test
        void inFlowApplicationPointsToItsCurrentStep() {
            View view = resolve(facts("Interviewing").interviews(completed()));

            assertEquals(3, view.stepNumber());
            assertEquals(new MyApplicationStep("Phỏng vấn", "current"), view.step());
        }

        @Test
        void stoppedApplicationPointsToTheStepWhereItStopped() {
            View view = resolve(facts("Rejected").marks(mark("Withdrawn", "Applied", PAST)));

            assertEquals(2, view.stepNumber());
            assertEquals(new MyApplicationStep("Xét CV", "closed"), view.step());
        }

        @Test
        void hiredApplicationPointsToTheLastStepAsDone() {
            View view = resolve(facts("Hired").offer(offer(8, "Accepted")));

            assertEquals(4, view.stepNumber());
            assertEquals(new MyApplicationStep("Thư mời", "done"), view.step());
        }
    }

    @Nested
    class ClosedPosting {
        @Test
        void closedWhileWaitingForInterview_stopsAtInterviewThoughTheScheduleWasCancelled() {
            View view = resolve(facts("Rejected")
                    .interviews(new Interview(FUTURE, "Offline_Office", "Cancelled"))
                    .marks(mark("PostingClosed", "Interviewing", PAST)));

            assertSteps(view, "done", "done", "closed", "upcoming");
        }

        @Test
        void closedAfterOfferWasSent_stopsAtOffer() {
            View view = resolve(facts("Rejected").interviews(completed()).offer(offer(3, "Sent_Candidate"))
                    .marks(mark("PostingClosed", "Offered", PAST)));

            assertSteps(view, "done", "done", "done", "closed");
        }

        @Test
        void completedInterviewCountsEvenWhenStatusBeforeCloseIsEarlier() {
            View view = resolve(facts("Rejected").interviews(completed())
                    .marks(mark("PostingClosed", "HR_Passed", PAST)));

            assertSteps(view, "done", "done", "closed", "upcoming");
        }

        @Test
        void missingStatusBeforeCloseFallsBackToFacts() {
            View view = resolve(facts("Rejected").marks(mark("PostingClosed", null, PAST)));

            assertSteps(view, "done", "closed", "upcoming", "upcoming");
        }
    }

    @Nested
    class Journey {
        @Test
        void milestonesAreOldestFirstWithCandidateWording() {
            View view = resolve(facts("Rejected")
                    .interviews(new Interview(LocalDateTime.of(2026, 10, 8, 9, 0), "Online_GoogleMeet", "Cancelled"),
                            new Interview(LocalDateTime.of(2026, 10, 9, 9, 0), "Offline_Office", "Rescheduled"),
                            new Interview(LocalDateTime.of(2026, 10, 11, 9, 0), "Offline_Office", "Completed"))
                    .offer(new Offer(7, "Declined", NOW, LocalDateTime.of(2026, 10, 15, 9, 0))));

            assertEquals(List.of("Đã nộp hồ sơ", "Lịch phỏng vấn đã hủy", "Hẹn phỏng vấn (Tại văn phòng)",
                    "Đã phỏng vấn", "Bạn đã từ chối thư mời"), titles(view));
        }

        @Test
        void offerAndStopMarksUseCandidateWording() {
            assertEquals(List.of("Đã nộp hồ sơ", "Nhận thư mời nhận việc"),
                    titles(resolve(facts("Offered").offer(offer(3, "Sent_Candidate")))));
            assertEquals(List.of("Đã nộp hồ sơ", "Bạn đã nhận lời"),
                    titles(resolve(facts("Hired").offer(offer(8, "Accepted")))));
            assertEquals(List.of("Đã nộp hồ sơ", "Đã rút hồ sơ"),
                    titles(resolve(facts("Rejected").marks(mark("Withdrawn", "Applied", PAST)))));
            assertEquals(List.of("Đã nộp hồ sơ", "Vị trí đã ngừng tuyển"),
                    titles(resolve(facts("Rejected").marks(mark("PostingClosed", "Applied", PAST)))));
        }

        @Test
        void milestonesNameNoInternalActorOrComment() {
            View view = resolve(facts("Hired").interviews(completed()).offer(offer(8, "Accepted")));

            assertTrue(view.journey().stream().allMatch(item -> item.actor() == null && item.body() == null));
        }
    }

    // ---- Dựng dữ kiện ----

    private static View resolve(FactsBuilder builder) {
        return MyApplicationJourney.resolve(builder.build(), NOW);
    }

    private static FactsBuilder facts(String status) {
        return new FactsBuilder(status);
    }

    private static Interview completed() {
        return new Interview(PAST, "Offline_Office", "Completed");
    }

    private static Offer offer(int id, String status) {
        return new Offer(id, status, LocalDateTime.of(2026, 10, 17, 23, 59), LocalDateTime.of(2026, 10, 12, 9, 0));
    }

    private static Mark mark(String kind, String oldStatus, LocalDateTime at) {
        return new Mark(kind, oldStatus, at);
    }

    private static void assertStatus(View view, String label, String note, String tone, String actionLabel,
                                     String actionHref) {
        assertEquals(label, view.statusLabel());
        assertEquals(note, view.statusNote());
        assertEquals(tone, view.tone());
        assertEquals(actionLabel, view.actionLabel());
        assertEquals(actionHref, view.actionHref());
    }

    private static void assertSteps(View view, String... states) {
        assertEquals(List.of("Nộp CV", "Xét CV", "Phỏng vấn", "Thư mời"),
                view.steps().stream().map(MyApplicationStep::label).toList());
        assertEquals(List.of(states), view.steps().stream().map(MyApplicationStep::state).toList());
    }

    private static List<String> titles(View view) {
        return view.journey().stream().map(TimelineItem::title).toList();
    }

    private static final class FactsBuilder {
        private final String status;
        private List<Interview> interviews = List.of();
        private Offer offer;
        private List<Mark> marks = List.of();

        FactsBuilder(String status) {
            this.status = status;
        }

        FactsBuilder interviews(Interview... value) {
            interviews = List.of(value);
            return this;
        }

        FactsBuilder offer(Offer value) {
            offer = value;
            return this;
        }

        FactsBuilder marks(Mark... value) {
            marks = List.of(value);
            return this;
        }

        Facts build() {
            return new Facts(status, SUBMITTED, interviews, offer, marks);
        }
    }
}
