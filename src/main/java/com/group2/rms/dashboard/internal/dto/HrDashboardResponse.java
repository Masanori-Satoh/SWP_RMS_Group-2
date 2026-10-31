package com.group2.rms.dashboard.internal.dto;

import java.util.List;

/*
 * DTO: toàn bộ dữ liệu dashboard HR (dashboard/sections/hr.html).
 * moreWork = số việc còn lại ngoài các việc đang hiện.
 */
public record HrDashboardResponse(
        String roleName,
        String fullName,
        List<DashboardMetricResponse> kpis,
        List<DashboardWorkItem> work,
        int moreWork,
        List<DashboardInterviewItem> upcomingInterviews,
        List<WeeklyApplicationItem> weeklyApplications) implements DashboardResponse {

    @Override
    public String section() {
        return "hr";
    }
}
