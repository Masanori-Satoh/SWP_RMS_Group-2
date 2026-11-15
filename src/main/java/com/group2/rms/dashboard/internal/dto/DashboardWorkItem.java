package com.group2.rms.dashboard.internal.dto;

/*
 * DTO: một việc cần xử lý trên dashboard (nhãn loại việc, nội dung, đã chờ bao lâu, nút thao tác).
 * titleHref = link khi bấm vào tiêu đề (màn xem chi tiết); mặc định trùng link của nút.
 */
public record DashboardWorkItem(
        String tag,
        String tagTone,
        String title,
        String detail,
        String waitText,
        String waitTone,
        String actionLabel,
        String href,
        boolean primary,
        String titleHref) {

    public DashboardWorkItem(String tag, String tagTone, String title, String detail, String waitText,
            String waitTone, String actionLabel, String href, boolean primary) {
        this(tag, tagTone, title, detail, waitText, waitTone, actionLabel, href, primary, href);
    }
}
