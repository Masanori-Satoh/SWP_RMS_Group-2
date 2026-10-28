package com.group2.rms.dashboard.dto;

import com.group2.rms.dashboard.DashboardMetricsRepository.ApprovalActivity;
import java.util.List;

public record RecruitmentDashboardResponse(
        String roleName,
        String fullName,
        String scope,
        List<DashboardMetricResponse> metrics,
        List<DashboardBreakdownResponse> breakdowns,
        List<ApprovalActivity> activities

) implements DashboardResponse {
}
