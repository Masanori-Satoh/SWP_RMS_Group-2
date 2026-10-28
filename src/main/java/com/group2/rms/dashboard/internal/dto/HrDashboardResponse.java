package com.group2.rms.dashboard.internal.dto;

import java.util.List;

/*
 * DTO: toàn bộ dữ liệu dashboard HR (dashboard/sections/hr.html).
 * moreWork = số việc còn lại ngoài các việc đang hiện; openPostingCount = tổng số tin đang mở.
 */
public record HrDashboardResponse(
        String roleName,
        String fullName,
        List<DashboardMetricResponse> kpis,
        List<DashboardWorkItem> work,
        int moreWork,
        List<DashboardInterviewItem> upcomingInterviews,
        List<DashboardBarResponse> waitingStages,
        List<DashboardColumnResponse> weeklyApplications,
        String trendSummary,
        List<OpenPostingItem> openPostings,
        int openPostingCount) implements DashboardResponse {

    @Override
    public String section() {
        return "hr";
    }
}
