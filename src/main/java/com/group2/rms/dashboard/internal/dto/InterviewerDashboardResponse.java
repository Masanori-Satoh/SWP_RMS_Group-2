package com.group2.rms.dashboard.internal.dto;

import java.time.LocalDate;
import java.util.List;

/*
 * DTO: toàn bộ dữ liệu dashboard Interviewer (dashboard/sections/interviewer.html).
 * weekStart là thứ Hai của tuần đang xem.
 */
public record InterviewerDashboardResponse(
        String roleName,
        String fullName,
        List<DashboardMetricResponse> kpis,
        List<EvaluationItem> pendingEvaluations,
        List<WeekDayItem> week,
        LocalDate weekStart) implements DashboardResponse {

    @Override
    public String section() {
        return "interviewer";
    }

    @Override
    public boolean hasCharts() {
        return false;
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
