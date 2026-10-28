package com.group2.rms.dashboard.shared;

import com.group2.rms.dashboard.shared.DashboardMetricsRepository.StatusCount;

import java.util.List;

/*
 * DTO: một bảng đếm theo trạng thái (dùng ở dashboard Admin và HR).
 */
public record DashboardBreakdownResponse(
    String title,
    List<StatusCount> statuses
) {
}
