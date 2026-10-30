package com.group2.rms.dashboard.internal.service;

import com.group2.rms.dashboard.internal.dto.DashboardMetricResponse;
import com.group2.rms.dashboard.internal.dto.InterviewerDashboardResponse;
import com.group2.rms.dashboard.internal.dto.WeekDayItem;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.EvaluationRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.InterviewRow;
import com.group2.rms.dashboard.internal.repository.InternalMetricsRepository;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST (Mockito): InterviewerDashboardService; repository là mock.
 * Giờ cố định thứ Tư 28/10/2026 09:00 (Asia/Ho_Chi_Minh); tuần này bắt đầu thứ Hai 26/10.
 */
class InterviewerDashboardServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-28T02:00:00Z"),
            ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);
    private static final int USER_ID = 11;

    private InternalMetricsRepository metrics;
    private DashboardListRepository lists;
    private InterviewerDashboardService service;

    @BeforeEach
    void setUp() {
        metrics = mock(InternalMetricsRepository.class);
        lists = mock(DashboardListRepository.class);
        service = new InterviewerDashboardService(metrics, lists, CLOCK);
    }

    private User interviewer() {
        return User.builder().userId(USER_ID).fullName("Khánh Duy").accountStatus("Active")
                .role(Role.builder().roleName("Interviewer").build()).build();
    }

    private InterviewRow interview(long id, LocalDateTime start, String status) {
        return new InterviewRow(id, start, start.plusHours(1), "Ứng viên " + id, "Java Dev", "Offline_Office",
                "Phòng 3A", status, 2);
    }

    private void givenSoon(List<InterviewRow> rows) {
        when(lists.panelInterviews(USER_ID, NOW.toLocalDate().atStartOfDay(), NOW.plusDays(7))).thenReturn(rows);
    }

    @Test
    @DisplayName("Chỉ tính buổi phỏng vấn sắp tới, không tính buổi đã xong hôm nay")
    void build_shouldFindNextInterview_whenOneDoneOneUpcoming() {
        // Arrange
        InterviewRow done = interview(1, NOW.minusHours(2), "Completed");
        InterviewRow next = interview(2, NOW.withHour(14).withMinute(0), "Scheduled");
        givenSoon(List.of(done, next));

        // Act
        InterviewerDashboardResponse result = service.build(interviewer(), null);

        // Assert
        DashboardMetricResponse upcoming = result.kpis().get(0);
        assertEquals(3, result.kpis().size());
        assertEquals("Buổi phỏng vấn sắp tới", upcoming.title());
        assertEquals("1", upcoming.value());
        assertEquals("Trong 7 ngày tới", upcoming.detail());
        assertEquals("Ứng viên 2", result.nextInterview().candidateName());
        assertEquals("Phòng 3A", result.nextInterview().location());
        assertEquals(300, result.minutesUntilNext());
    }

    @Test
    @DisplayName("7 ngày tới không có buổi nào -> không có khối 'Buổi tiếp theo'")
    void build_shouldHaveNoNextInterview_whenNothingScheduled() {
        // Arrange
        givenSoon(List.of());

        // Act
        InterviewerDashboardResponse result = service.build(interviewer(), null);

        // Assert
        assertNull(result.nextInterview());
        assertEquals("0", result.kpis().get(0).value());
    }

    @Test
    @DisplayName("Phiếu chưa hoàn thành và ứng viên đạt tháng này đều thuộc phạm vi người phỏng vấn")
    void build_shouldFlagOverdueEvaluation_whenEndedThreeDaysAgoOrMore() {
        // Arrange
        when(lists.pendingEvaluations(USER_ID)).thenReturn(List.of(
                new EvaluationRow(5, "Lý Minh Khoa", "Frontend", NOW.minusDays(4)),
                new EvaluationRow(6, null, "Backend", NOW.minusHours(3))));
        when(metrics.passedInterviewCandidatesBetween(eq(USER_ID),
                eq(LocalDate.of(2026, 10, 1).atStartOfDay()), eq(NOW))).thenReturn(8L);

        // Act
        InterviewerDashboardResponse result = service.build(interviewer(), null);

        // Assert
        assertEquals("warning", result.pendingEvaluations().get(0).waitTone());
        assertEquals("Ứng viên chưa có tên", result.pendingEvaluations().get(1).candidateName());
        assertEquals(DashboardViews.INTERVIEWS_HREF, result.pendingEvaluations().get(0).href());
        assertEquals("Đánh giá chưa hoàn thành", result.kpis().get(1).title());
        assertEquals("2", result.kpis().get(1).value());
        assertEquals("Đang chờ đánh giá", result.kpis().get(1).detail());
        assertEquals("Ứng viên đạt phỏng vấn", result.kpis().get(2).title());
        assertEquals("8", result.kpis().get(2).value());
        assertEquals("Trong tháng này", result.kpis().get(2).detail());
    }

    @Test
    @DisplayName("Lịch tuần: 7 cột T2..CN, buổi xếp đúng ngày, hôm nay được đánh dấu, buổi đã qua giờ = finished")
    void build_shouldGroupWeekByDay_whenCurrentWeek() {
        // Arrange
        InterviewRow tuesday = interview(1, LocalDateTime.of(2026, 10, 27, 15, 0), "Scheduled");
        InterviewRow wednesday = interview(2, LocalDateTime.of(2026, 10, 28, 14, 0), "Rescheduled");
        when(lists.panelInterviews(USER_ID, MONDAY.atStartOfDay(), MONDAY.plusDays(7).atStartOfDay()))
                .thenReturn(List.of(tuesday, wednesday));

        // Act
        InterviewerDashboardResponse result = service.build(interviewer(), null);
        List<WeekDayItem> week = result.week();

        // Assert
        assertEquals(MONDAY, result.weekStart());
        assertEquals(7, week.size());
        assertEquals("T2", week.get(0).weekday());
        assertEquals("CN", week.get(6).weekday());
        assertTrue(week.get(0).interviews().isEmpty());
        assertTrue(week.get(1).interviews().get(0).finished());
        assertTrue(week.get(2).today());
        assertFalse(week.get(2).interviews().get(0).finished());
        assertEquals(2, result.weekInterviewCount());
        assertEquals(MONDAY.minusWeeks(1), result.previousWeek());
    }

    @Test
    @DisplayName("?week= là một ngày giữa tuần -> đưa về thứ Hai của tuần đó")
    void build_shouldStartWeekOnMonday_whenRequestedDayIsMidWeek() {
        // Act
        InterviewerDashboardResponse result = service.build(interviewer(), "2026-11-05");

        // Assert
        assertEquals(LocalDate.of(2026, 11, 2), result.weekStart());
    }
}
