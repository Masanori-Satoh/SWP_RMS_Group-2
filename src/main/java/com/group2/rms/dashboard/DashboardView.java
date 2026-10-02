package com.group2.rms.dashboard;

import com.group2.rms.dashboard.DashboardMetricsRepository.ApprovalActivity;
import com.group2.rms.dashboard.DashboardMetricsRepository.StatusCount;

import java.util.List;

/** Values rendered by the dashboard; all numeric values come from database queries. */
public record DashboardView(
        String roleName,
        String fullName,
        String scope,
        List<Metric> metrics,
        List<Breakdown> breakdowns,
        List<ApprovalActivity> activities,
        List<Unavailable> unavailable,
        List<Shortcut> shortcuts,
        List<AccountSummary> accountSummaries) {

    public DashboardView(String roleName, String fullName, String scope, List<Metric> metrics,
                         List<Breakdown> breakdowns, List<ApprovalActivity> activities,
                         List<Unavailable> unavailable, List<Shortcut> shortcuts) {
        this(roleName, fullName, scope, metrics, breakdowns, activities, unavailable, shortcuts, List.of());
    }

    public record AccountSummary(String title, String url, long total, long active, long inactive, long blocked) { }

    public record Metric(String title, long value, String detail) { }

    public record Breakdown(String title, List<StatusCount> statuses) { }

    public record Unavailable(String title, String reason) { }

    public record Shortcut(String title, String url) { }
}
