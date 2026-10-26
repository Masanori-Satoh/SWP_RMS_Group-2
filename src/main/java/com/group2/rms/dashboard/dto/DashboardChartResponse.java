package com.group2.rms.dashboard.dto;

import java.util.List;

/*
 * DTO: một biểu đồ thanh ngang của dashboard (tiêu đề, phạm vi đang thống kê, các thanh).
 */
public record DashboardChartResponse(
        String title,
        String scope,
        List<DashboardBarResponse> bars) {
}
