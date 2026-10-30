package com.group2.rms.dashboard.internal.dto;

import java.util.List;

/*
 * DTO: toàn bộ dữ liệu dashboard Hiring Manager (dashboard/sections/hiring-manager.html).
 * moreNeedsAttention = số việc cần xử lý còn lại ngoài các việc đang hiện.
 */
public record HiringManagerDashboardResponse(
        String roleName,
        String fullName,
        String scopeLabel,
        List<DashboardMetricResponse> kpis,
        List<DashboardWorkItem> needsAttention,
        int moreNeedsAttention,
        List<DashboardInterviewItem> upcomingInterviews,
        List<HiringProgressItem> progress) implements DashboardResponse {

    @Override
    public String section() {
        return "hiring-manager";
    }
}
