package com.group2.rms.dashboard.dto;

public record DashboardMetricResponse(
        String title,
        long value,
        String detail) {
}