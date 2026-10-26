package com.group2.rms.dashboard.dto;

/*
 * DTO: một thanh của biểu đồ; max = số lớn nhất trong cùng biểu đồ để vẽ độ dài thanh bằng thẻ <progress>.
 */
public record DashboardBarResponse(
        String label,
        long count,
        long max,
        String href) {
}
