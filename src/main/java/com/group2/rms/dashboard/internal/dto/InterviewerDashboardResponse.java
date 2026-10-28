package com.group2.rms.dashboard.internal.dto;

import java.time.LocalDate;
import java.util.List;

/*
 * DTO: toàn bộ dữ liệu dashboard Interviewer (dashboard/sections/interviewer.html).
 * nextInterview = null khi 7 ngày tới không có buổi nào; weekStart là thứ Hai của tuần đang xem.
 */
public record InterviewerDashboardResponse(
        String roleName,
        String fullName,
        List<DashboardMetricResponse> kpis,
        DashboardInterviewItem nextInterview,
        long minutesUntilNext,
        List<EvaluationItem> pendingEvaluations,
        List<WeekDayItem> week,
        LocalDate weekStart,
        int weekInterviewCount) implements DashboardResponse {

    @Override
    public String section() {
        return "interviewer";
    }

    public LocalDate weekEnd() {
        return weekStart.plusDays(6);
    }

    public LocalDate previousWeek() {
        return weekStart.minusWeeks(1);
    }

    public LocalDate nextWeek() {
        return weekStart.plusWeeks(1);
    }
}
