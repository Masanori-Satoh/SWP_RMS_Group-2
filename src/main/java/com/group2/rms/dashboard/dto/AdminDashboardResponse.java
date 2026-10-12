package com.group2.rms.dashboard.dto;

import java.util.List;

public record AdminDashboardResponse(
        String roleName,
        String fullName,
        String scope,
        List<AccountSummary> accountSummaries,
        DepartmentSummary departments,
        List<DashboardBreakdownResponse> breakdowns,
        List<Shortcut> shortcuts) implements DashboardResponse {

    public record AccountSummary(
            String title,
            String url,
            long total,
            long active,
            long inactive,
            long blocked) {
    }

    public record DepartmentSummary(
            long totalCount,
            long activeCount,
            long inactiveCount) {
    }

    public record Shortcut(
            String title,
            String url) {
    }
}
