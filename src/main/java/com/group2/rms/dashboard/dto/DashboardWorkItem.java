package com.group2.rms.dashboard.dto;

/*
 * DTO: một việc cần xử lý trên dashboard (nhãn loại việc, nội dung, đã chờ bao lâu, nút thao tác).
 */
public record DashboardWorkItem(
        String tag,
        String tagTone,
        String title,
        String detail,
        String waitText,
        String waitTone,
        String actionLabel,
        String href) {
}
