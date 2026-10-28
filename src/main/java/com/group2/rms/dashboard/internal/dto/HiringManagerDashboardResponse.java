package com.group2.rms.dashboard.dto;

import java.util.List;

/*
 * DTO: toàn bộ dữ liệu dashboard Hiring Manager (dashboard/sections/hiring-manager.html).
 * moreNeedsAttention = số việc cần xử lý còn lại ngoài các việc đang hiện.
 */
public record HiringManagerDashboardResponse(
        String roleName,
        String fullName,
        List<DashboardMetricResponse> kpis,
        List<RequisitionItem> recentRequisitions,
        List<DashboardInterviewItem> upcomingInterviews,
        List<DashboardWorkItem> needsAttention,
        int moreNeedsAttention,
        List<DashboardChartResponse> charts) implements DashboardResponse {

    /** Tên file trong templates/dashboard/sections/ mà index.html chèn vào. */
    @Override
    public String section() {
        return "hiring-manager";
    }
}
