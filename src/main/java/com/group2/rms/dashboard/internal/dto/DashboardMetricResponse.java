package com.group2.rms.dashboard.dto;

/*
 * DTO: một thẻ KPI trên dashboard (tiêu đề, con số, dòng giải thích, link khi bấm, tông màu).
 */
public record DashboardMetricResponse(
        String title,
        long value,
        String detail,
        String href,
        String tone) {

    /** Thẻ không có link và tông màu (Admin, Candidate và các dashboard cũ). */
    public DashboardMetricResponse(String title, long value, String detail) {
        this(title, value, detail, null, null);
    }
}
