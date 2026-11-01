package com.group2.rms.dashboard.internal.dto;

/*
 * DTO: một điểm của biểu đồ đường "Xu hướng hồ sơ ứng tuyển" (HR); weekLabel là ngày thứ Hai, rangeLabel hiện khi rê chuột.
 */
public record WeeklyApplicationItem(
        String weekLabel,
        String rangeLabel,
        long count) {
}
