package com.group2.rms.candidate.application.pipeline.service;

import com.group2.rms.candidate.domain.rule.ApplicationProgressResponse;
import com.group2.rms.candidate.domain.rule.ApplicationProgressStep;
import com.group2.rms.candidate.domain.rule.ApplicationStage;
import com.group2.rms.core.web.StageItem;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.group2.rms.candidate.domain.rule.ApplicationProgressStep.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationStepperTests {

    // ------------------------------------------------------------------ hồ sơ còn trong luồng

    @Test
    void openApplicationMarksEarlierStagesDoneCurrentStageAndLaterStagesUpcoming() {
        List<StageItem> items = ApplicationStepper.items(progress(AWAITING_SCHEDULE, "Chờ xếp lịch phỏng vấn"));

        assertEquals(List.of("APPLIED", "SCREENING", "INTERVIEW", "OFFER", "HIRED"), keys(items));
        assertEquals(List.of("done", "done", "current", "upcoming", "upcoming"), states(items));
        assertEquals(List.of("Ứng tuyển", "Sàng lọc CV", "Phỏng vấn", "Offer", "Nhận việc"),
                items.stream().map(StageItem::label).toList());
    }

    @Test
    void currentStageShowsDetailAndOwnerOthersShowNone() {
        List<StageItem> items = ApplicationStepper.items(progress(AWAITING_HR_REVIEW, "Chờ HR duyệt CV"));

        assertEquals("Chờ HR duyệt CV · Đang chờ: HR", items.get(0).detail());
        assertEquals(List.of(false, true, true, true, true),
                items.stream().map(item -> item.detail() == null).toList());
    }

    @Test
    void firstAndLastStagesAreReachable() {
        assertEquals(List.of("current", "upcoming", "upcoming", "upcoming", "upcoming"),
                states(ApplicationStepper.items(progress(AWAITING_HR_REVIEW, "Chờ HR duyệt CV"))));

        List<StageItem> hired = ApplicationStepper.items(progress(HIRED_DONE, "Đã nhận việc"));
        assertEquals(List.of("done", "done", "done", "done", "current"), states(hired));
        assertEquals("Đã nhận việc", hired.get(4).detail(), "không có ai cầm bóng thì chỉ hiện nhãn");
    }

    // ------------------------------------------------------------------ hồ sơ đã dừng

    @Test
    void withdrawnStopsAfterApplied() {
        assertClosed(WITHDRAWN, "Ứng viên đã rút hồ sơ", List.of("APPLIED", "CLOSED"));
    }

    @Test
    void cvRejectedStopsAfterScreening() {
        assertClosed(CV_REJECTED, "Không qua vòng CV", List.of("APPLIED", "SCREENING", "CLOSED"));
    }

    @Test
    void interviewFailedStopsAfterInterview() {
        assertClosed(INTERVIEW_FAILED, "Không đạt phỏng vấn", List.of("APPLIED", "SCREENING", "INTERVIEW", "CLOSED"));
    }

    @Test
    void offerDeclinedStopsAfterOffer() {
        assertClosed(OFFER_DECLINED, "Ứng viên từ chối offer",
                List.of("APPLIED", "SCREENING", "INTERVIEW", "OFFER", "CLOSED"));
    }

    @Test
    void everyClosedStepHasAStoppingPointAndNoOpenStepDoes() {
        Arrays.stream(ApplicationProgressStep.values()).forEach(step -> {
            if (step.stage() == ApplicationStage.CLOSED) {
                ApplicationStepper.lastStagePassed(step);
            } else {
                assertThrows(IllegalStateException.class, () -> ApplicationStepper.lastStagePassed(step));
            }
        });
    }

    @Test
    void postingClosedKeepsTheReasonWithoutInferringPassedRounds() {
        assertClosed(POSTING_CLOSED, "Đóng đợt tuyển dụng", List.of("APPLIED", "CLOSED"));
    }

    private static void assertClosed(ApplicationProgressStep step, String reason, List<String> expectedKeys) {
        List<StageItem> items = ApplicationStepper.items(progress(step, reason));

        assertEquals(expectedKeys, keys(items));
        List<String> expectedStates = expectedKeys.stream().map(key -> "CLOSED".equals(key) ? "current" : "done").toList();
        assertEquals(expectedStates, states(items));
        StageItem closed = items.getLast();
        assertEquals("Đã dừng", closed.label());
        assertEquals(reason, closed.detail(), "ô Đã dừng mang lý do");
        assertNull(items.getFirst().detail());
    }

    private static ApplicationProgressResponse progress(ApplicationProgressStep step, String label) {
        return new ApplicationProgressResponse(step.stage(), step, label, step.owner(), step.tone());
    }

    private static List<String> keys(List<StageItem> items) {
        return items.stream().map(StageItem::key).toList();
    }

    private static List<String> states(List<StageItem> items) {
        return items.stream().map(StageItem::state).toList();
    }
}
