package com.group2.rms.service;

import com.group2.rms.repository.DashboardMetricsRepository.ApprovalActivity;
import com.group2.rms.repository.DashboardMetricsRepository.StatusCount;

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
        List<Shortcut> shortcuts) {

    public record Metric(String title, long value, String detail) { }

    public record Breakdown(String title, List<StatusCount> statuses) { }

    public record Unavailable(String title, String reason) { }

    public record Shortcut(String title, String url) { }
}
