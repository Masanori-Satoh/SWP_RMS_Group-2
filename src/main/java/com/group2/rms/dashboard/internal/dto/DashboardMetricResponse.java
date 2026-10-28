package com.group2.rms.dashboard.internal.dto;

/*
 * DTO: một thẻ KPI (icon, nhãn ngắn, con số, % so với kỳ trước, một dòng phụ, link khi bấm, tông màu).
 * icon là tên trong fragment dashboard/fragments :: icon(name); delta = null khi thẻ không so sánh kỳ.
 */
public record DashboardMetricResponse(
        String icon,
        String title,
        String value,
        String delta,
        String deltaTone,
        String detail,
        String href,
        String tone) {

    /** Thẻ "tồn đọng": số hiện tại, không so sánh kỳ. */
    public static DashboardMetricResponse backlog(String icon, String title, long value, String detail, String href,
            String tone) {
        return new DashboardMetricResponse(icon, title, String.valueOf(value), null, null, detail, href, tone);
    }
}
