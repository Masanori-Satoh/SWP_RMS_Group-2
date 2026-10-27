package com.group2.rms.candidate.application.portal.service;

import com.group2.rms.candidate.application.portal.dto.MyApplicationStep;
import com.group2.rms.candidate.application.portal.service.MyApplicationJourney.Facts;
import com.group2.rms.candidate.application.portal.service.MyApplicationJourney.Interview;
import com.group2.rms.candidate.application.portal.service.MyApplicationJourney.Mark;
import com.group2.rms.candidate.application.portal.service.MyApplicationJourney.Offer;
import com.group2.rms.candidate.application.portal.service.MyApplicationJourney.View;
import com.group2.rms.core.web.TimelineItem;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Điều ứng viên thấy trên một đơn: bảng 5.3 của {@code nghiep-vu.md} (luồng 5, L6) mỗi dòng một test trong {@link Table},
 * thứ tự giống bảng. {@link ClosedPosting} là bước dừng khi đóng tin, {@link Journey} là hành trình.
 */
class MyApplicationJourneyTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final LocalDateTime SUBMITTED = LocalDateTime.of(2026, 10, 1, 8, 30);
    private static final LocalDateTime PAST = LocalDateTime.of(2026, 10, 10, 9, 0);
    private static final LocalDateTime FUTURE = LocalDateTime.of(2026, 11, 24, 10, 0);

    /** Câu giải thích chung cho mọi bước duyệt nội bộ trước phỏng vấn (L6: không lộ bước nào đang xét). */
    private static final String SUBMITTED_NOTE =
            "Hồ sơ của bạn đang được xem xét. Bạn sẽ nhận được thông báo khi có lịch phỏng vấn.";

    /** Bảng 5.3 {@code nghiep-vu.md}, từ trên xuống. */
    @Nested
    class Table {
        @ParameterizedTest
        @ValueSource(strings = {"Applied", "AI_Screened", "HR_Passed", "HM_Passed"})
        void row01_internalReviewWithoutSchedule_showsSubmittedOnly(String status) {
            View view = resolve(facts(status));

            assertStatus(view, "Đã nộp CV", SUBMITTED_NOTE, "neutral", null, null);
            assertSteps(view, "current", "upcoming", "upcoming", "upcoming");
        }

        @Test
        void row02_upcomingInterview_showsTimeFormatAndSchedule() {
            View view = resolve(facts("Interviewing").interviews(new Interview(FUTURE, "Online_GoogleMeet", "Scheduled")));

            assertStatus(view, "Phỏng vấn 10:00 24/11", "Trực tuyến. Xem chi tiết ở Lịch phỏng vấn.", "warning",
                    "Xem lịch phỏng vấn", "/portal/interviews");
            assertSteps(view, "done", "current", "upcoming", "upcoming");
        }

        @Test
        void row03_interviewDone_waitsForResult() {
            View view = resolve(facts("Interviewing").interviews(new Interview(PAST, "Offline_Office", "Scheduled")));

            assertStatus(view, "Đã phỏng vấn, chờ kết quả", "Kết quả sẽ được thông báo cho bạn.", "neutral",
                    "Xem lịch phỏng vấn", "/portal/interviews");
            assertSteps(view, "done", "current", "upcoming", "upcoming");
        }

        @Test
        void row04_offerSent_asksForAnswerBeforeDeadline() {
            View view = resolve(facts("Offered").interviews(completed()).offer(offer(3, "Sent_Candidate")));

            assertStatus(view, "Có thư mời", "Vui lòng trả lời trước 17/10.", "warning",
                    "Trả lời thư mời", "/portal/offers/3");
            assertSteps(view, "done", "done", "current", "upcoming");
        }

        @Test
        void row05_hired_finishesAllFourStepsAndLinksTheOffer() {
            View view = resolve(facts("Hired").interviews(completed()).offer(offer(8, "Accepted")));

            assertStatus(view, "Đã nhận việc", "Chúc mừng bạn!", "success", "Xem thư mời", "/portal/offers/8");
            assertSteps(view, "done", "done", "done", "done");
        }

        @Test
        void row06_withdrawn_stopsAtTheStepItWasInWithWithdrawalDate() {
            View view = resolve(facts("Rejected").marks(mark("Withdrawn", "AI_Screened", LocalDateTime.of(2026, 10, 5, 14, 0))));

            assertStatus(view, "Đã tự rút hồ sơ", "Bạn đã rút hồ sơ ngày 05/10.", "neutral", null, null);
            assertSteps(view, "done", "upcoming", "upcoming", "upcoming");
        }

        @Test
        void row07_rejectedAtCv_thanksPolitelyAndPointsToOtherJobs() {
            View view = resolve(facts("Rejected").interviews(new Interview(FUTURE, "Offline_Office", "Cancelled")));

            assertStatus(view, "Không đạt vòng CV",
                    "Cảm ơn bạn đã quan tâm. Hồ sơ của bạn chưa phù hợp với vị trí này.", "danger",
                    "Xem vị trí khác", "/jobs");
            assertSteps(view, "done", "upcoming", "upcoming", "upcoming");
        }

        @Test
        void row08_rejectedAfterInterview_stopsAtInterview() {
            View view = resolve(facts("Rejected").interviews(completed()));

            assertStatus(view, "Không đạt phỏng vấn", "Cảm ơn bạn đã tham gia phỏng vấn.", "danger", null, null);
            assertSteps(view, "done", "done", "upcoming", "upcoming");
        }

        @Test
        void row08_offerDeclined_stopsAtOffer() {
            View view = resolve(facts("Rejected").interviews(completed()).offer(offer(7, "Declined")));

            assertStatus(view, "Đã từ chối thư mời", null, "neutral", "Xem thư mời", "/portal/offers/7");
            assertSteps(view, "done", "done", "done", "upcoming");
        }

        @Test
        void row08_postingClosed_pointsToOpenJobs() {
            View view = resolve(facts("Rejected").marks(mark("PostingClosed", "AI_Screened", PAST)));

            assertStatus(view, "Vị trí đã ngừng tuyển",
                    "Công ty đã ngừng tuyển vị trí này nên hồ sơ của bạn dừng lại. Bạn có thể ứng tuyển vị trí khác.",
                    "neutral", "Xem vị trí đang tuyển", "/jobs");
            assertSteps(view, "done", "upcoming", "upcoming", "upcoming");
        }

        @Test
        void offeredWithoutVisibleOffer_saysRecruitmentWillContact() {
            View view = resolve(facts("Offered").interviews(completed()));

            assertStatus(view, "Có thư mời", "Bộ phận tuyển dụng sẽ liên hệ với bạn.", "neutral", null, null);
            assertSteps(view, "done", "done", "current", "upcoming");
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

            assertEquals("Đã nộp CV", view.statusLabel());
        }

        /** Chuẩn bị cho PR 4 (rút được tới trước thư mời): bước dừng lấy từ trạng thái ngay trước khi rút. */
        @Test
        void withdrawalDuringInterviewRoundStopsAtInterview() {
            View view = resolve(facts("Rejected").interviews(new Interview(FUTURE, "Offline_Office", "Cancelled"))
                    .marks(mark("Withdrawn", "Interviewing", PAST)));

            assertSteps(view, "done", "done", "upcoming", "upcoming");
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

        @ParameterizedTest
        @ValueSource(strings = {"HM_Passed", "Interviewing"})
        void cancelledInterviewOnlyMeansNoScheduleYet(String status) {
            View view = resolve(facts(status).interviews(new Interview(FUTURE, "Offline_Office", "Cancelled")));

            assertStatus(view, "Đã nộp CV", SUBMITTED_NOTE, "neutral", null, null);
            assertSteps(view, "current", "upcoming", "upcoming", "upcoming");
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
            assertSteps(view, "current", "upcoming", "upcoming", "upcoming");
        }
    }

    @Nested
    class ClosedPosting {
        @Test
        void closedWhileWaitingForInterview_stopsAtInterviewThoughTheScheduleWasCancelled() {
            View view = resolve(facts("Rejected")
                    .interviews(new Interview(FUTURE, "Offline_Office", "Cancelled"))
                    .marks(mark("PostingClosed", "Interviewing", PAST)));

            assertSteps(view, "done", "done", "upcoming", "upcoming");
        }

        /** L6: HM đạt mà chưa có lịch thì ứng viên chưa được báo gì, nên đơn vẫn dừng ở Nộp CV. */
        @Test
        void closedAfterHmPassedWithoutSchedule_stopsAtSubmit() {
            View view = resolve(facts("Rejected").marks(mark("PostingClosed", "HM_Passed", PAST)));

            assertSteps(view, "done", "upcoming", "upcoming", "upcoming");
        }

        @Test
        void closedAfterOfferWasSent_stopsAtOffer() {
            View view = resolve(facts("Rejected").interviews(completed()).offer(offer(3, "Sent_Candidate"))
                    .marks(mark("PostingClosed", "Offered", PAST)));

            assertSteps(view, "done", "done", "done", "upcoming");
        }

        @Test
        void completedInterviewCountsEvenWhenStatusBeforeCloseIsEarlier() {
            View view = resolve(facts("Rejected").interviews(completed())
                    .marks(mark("PostingClosed", "HR_Passed", PAST)));

            assertSteps(view, "done", "done", "upcoming", "upcoming");
        }

        @Test
        void missingStatusBeforeCloseFallsBackToFacts() {
            View view = resolve(facts("Rejected").marks(mark("PostingClosed", null, PAST)));

            assertSteps(view, "done", "upcoming", "upcoming", "upcoming");
        }
    }

    /** Đơn gấp (ux mục 2): thư mời còn hạn hoặc lịch phỏng vấn trong 48 giờ tới. */
    @Nested
    class Urgent {
        @Test
        void offerDueInSomeDaysCountsWholeDays() {
            LocalDateTime deadline = NOW.plusDays(3).plusHours(5);
            View view = resolve(facts("Offered").offer(sent(deadline)));

            assertEquals(deadline, view.urgentUntil());
            assertEquals("Còn 3 ngày để trả lời thư mời", view.urgentLabel());
        }

        @Test
        void offerDueWithinADayCountsHours() {
            View view = resolve(facts("Offered").offer(sent(NOW.plusHours(5).plusMinutes(30))));

            assertEquals("Còn 5 giờ để trả lời thư mời", view.urgentLabel());
        }

        @Test
        void offerDueWithinAnHourSaysLessThanAnHour() {
            View view = resolve(facts("Offered").offer(sent(NOW.plusMinutes(20))));

            assertEquals("Còn chưa tới 1 giờ để trả lời thư mời", view.urgentLabel());
        }

        @Test
        void offerPastDeadlineOrWithoutDeadlineIsNotUrgent() {
            assertNotUrgent(resolve(facts("Offered").offer(sent(NOW.minusMinutes(1)))));
            assertNotUrgent(resolve(facts("Offered").offer(sent(null))));
        }

        @Test
        void interviewWithin48HoursIsUrgent() {
            LocalDateTime start = NOW.plusHours(26);
            View view = resolve(facts("Interviewing").interviews(new Interview(start, "Offline_Office", "Scheduled")));

            assertEquals(start, view.urgentUntil());
            assertEquals("Phỏng vấn sau 26 giờ", view.urgentLabel());
        }

        @Test
        void interviewExactly48HoursAwayIsUrgentButNotAMinuteLater() {
            assertEquals("Phỏng vấn sau 48 giờ", resolve(facts("Interviewing")
                    .interviews(new Interview(NOW.plusHours(48), "Offline_Office", "Rescheduled"))).urgentLabel());
            assertNotUrgent(resolve(facts("Interviewing")
                    .interviews(new Interview(NOW.plusHours(48).plusMinutes(1), "Offline_Office", "Scheduled"))));
        }

        @Test
        void interviewWithinAnHourSaysWithinTheHour() {
            View view = resolve(facts("Interviewing")
                    .interviews(new Interview(NOW.plusMinutes(40), "Online_GoogleMeet", "Scheduled")));

            assertEquals("Phỏng vấn trong vòng 1 giờ tới", view.urgentLabel());
        }

        @Test
        void cancelledOrPastInterviewIsNotUrgent() {
            assertNotUrgent(resolve(facts("Interviewing")
                    .interviews(new Interview(NOW.plusHours(3), "Offline_Office", "Cancelled"))));
            assertNotUrgent(resolve(facts("Interviewing")
                    .interviews(new Interview(NOW.minusHours(1), "Offline_Office", "Scheduled"))));
        }

        @Test
        void stoppedApplicationIsNeverUrgent() {
            assertNotUrgent(resolve(facts("Rejected").offer(sent(NOW.plusDays(2)))
                    .marks(mark("PostingClosed", "Offered", PAST))));
        }

        private static Offer sent(LocalDateTime deadline) {
            return new Offer(3, "Sent_Candidate", deadline, PAST);
        }

        private static void assertNotUrgent(View view) {
            assertNull(view.urgentUntil());
            assertNull(view.urgentLabel());
        }
    }

    /** Tab "Đang xử lý" / "Đã kết thúc": đơn dừng hoặc đã nhận việc là kết thúc. */
    @Nested
    class Ended {
        @Test
        void applicationInFlowIsNotEnded() {
            assertFalse(resolve(facts("Applied")).ended());
            assertFalse(resolve(facts("Offered").offer(offer(3, "Sent_Candidate"))).ended());
        }

        @Test
        void hiredOrStoppedApplicationIsEnded() {
            assertTrue(resolve(facts("Hired")).ended());
            assertTrue(resolve(facts("Rejected")).ended());
            assertTrue(resolve(facts("Rejected").marks(mark("Withdrawn", "Applied", PAST))).ended());
        }
    }

    /** Xếp danh sách (dunglt 27/10): nhóm 1 sự kiện sắp tới, nhóm 2 đang xử lý, nhóm 3 đã kết thúc; chuyển biến mới nhất. */
    @Nested
    class Ordering {
        /** Lỗi dunglt thấy ở an.vo: lịch hẹn ngày mai vừa xếp hôm nay phải là chuyển biến hôm nay. */
        @Test
        void interviewJustScheduledCountsAsActivityThoughItIsInTheFuture() {
            LocalDateTime scheduledAt = NOW.minusHours(2);
            View view = resolve(facts("Interviewing").interviews(new Interview(FUTURE, "Offline_Office", "Scheduled", scheduledAt)));

            assertEquals(scheduledAt, view.lastActivityAt());
        }

        @Test
        void interviewScheduledLongAgoKeepsItsOwnScheduleTime() {
            View view = resolve(facts("Interviewing").interviews(new Interview(FUTURE, "Offline_Office", "Scheduled", PAST)));

            assertEquals(PAST, view.lastActivityAt());
        }

        @Test
        void anyFutureInterviewPutsTheApplicationFirstWithItsStartTime() {
            View farAway = resolve(facts("Interviewing").interviews(new Interview(FUTURE, "Offline_Office", "Scheduled")));

            assertEquals(MyApplicationJourney.PRIORITY_UPCOMING, farAway.priority());
            assertEquals(FUTURE, farAway.upcomingAt());
            assertNull(farAway.urgentUntil(), "xa hơn 48 giờ: không gấp nhưng vẫn ở nhóm 1");
        }

        @Test
        void offerAwaitingAnswerIsUpcomingEvenWithoutDeadline() {
            View noDeadline = resolve(facts("Offered").offer(new Offer(3, "Sent_Candidate", null, PAST)));
            LocalDateTime deadline = NOW.plusDays(2);
            View withDeadline = resolve(facts("Offered").offer(new Offer(3, "Sent_Candidate", deadline, PAST)));

            assertEquals(MyApplicationJourney.PRIORITY_UPCOMING, noDeadline.priority());
            assertNull(noDeadline.upcomingAt());
            assertEquals(MyApplicationJourney.PRIORITY_UPCOMING, withDeadline.priority());
            assertEquals(deadline, withDeadline.upcomingAt());
        }

        @Test
        void earlierOfInterviewAndOfferDeadlineIsTheUpcomingEvent() {
            LocalDateTime deadline = NOW.plusDays(2);
            View view = resolve(facts("Offered").interviews(new Interview(FUTURE, "Offline_Office", "Scheduled"))
                    .offer(new Offer(3, "Sent_Candidate", deadline, PAST)));

            assertEquals(deadline, view.upcomingAt());
        }

        @Test
        void inFlowWithoutUpcomingEventIsSecondGroup() {
            assertEquals(MyApplicationJourney.PRIORITY_IN_FLOW, resolve(facts("Applied")).priority());
            assertEquals(MyApplicationJourney.PRIORITY_IN_FLOW, resolve(facts("Interviewing").interviews(completed())).priority());
            // Thư mời quá hạn mà chưa trả lời: không còn là sự kiện sắp tới
            assertEquals(MyApplicationJourney.PRIORITY_IN_FLOW,
                    resolve(facts("Offered").offer(offer(3, "Sent_Candidate"))).priority());
        }

        @Test
        void endedApplicationsAreLastGroupWhateverHappenedRecently() {
            View withdrawn = resolve(facts("Rejected").marks(mark("Withdrawn", "Applied", NOW.minusMinutes(5))));
            View hired = resolve(facts("Hired").offer(offer(8, "Accepted")));

            assertEquals(MyApplicationJourney.PRIORITY_ENDED, withdrawn.priority());
            assertEquals(MyApplicationJourney.PRIORITY_ENDED, hired.priority());
            assertNull(withdrawn.upcomingAt());
        }

        @Test
        void onlyApplicationsStoppedHalfwayAreMarkedStopped() {
            assertTrue(resolve(facts("Rejected")).stopped());
            assertTrue(resolve(facts("Rejected").marks(mark("Withdrawn", "Applied", PAST))).stopped());
            assertFalse(resolve(facts("Hired")).stopped());
            assertFalse(resolve(facts("Applied")).stopped());
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
        void lastActivityIsTheNewestMilestoneThatAlreadyHappened() {
            View view = resolve(facts("Offered").interviews(completed()).offer(offer(3, "Sent_Candidate")));

            assertEquals(LocalDateTime.of(2026, 10, 12, 9, 0), view.lastActivityAt());
        }

        @Test
        void futureInterviewDoesNotCountAsActivityYet() {
            View view = resolve(facts("Interviewing").interviews(new Interview(FUTURE, "Offline_Office", "Scheduled")));

            assertEquals(SUBMITTED, view.lastActivityAt());
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
        assertEquals(List.of("Nộp CV", "Phỏng vấn", "Thư mời", "Nhận việc"),
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
