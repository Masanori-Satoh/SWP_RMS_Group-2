package com.group2.rms.dashboard.dto;

import java.util.List;
import com.group2.rms.dashboard.DashboardMetricsRepository.StatusCount;
public record DashboardBreakdownResponse(
    String title,
    List<StatusCount> statuses
) {
}
