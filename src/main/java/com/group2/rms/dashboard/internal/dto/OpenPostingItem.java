package com.group2.rms.dashboard.internal.dto;

import java.time.LocalDateTime;

/*
 * DTO: một dòng "Tin đang mở" của HR. applicationsMax = số hồ sơ lớn nhất trong bảng để vẽ thanh <progress>.
 * deadlineText = "Còn N ngày" / "Hết hạn hôm nay" khi sắp hết hạn, null = còn xa (chỉ hiện ngày).
 */
public record OpenPostingItem(
        String title,
        String departmentName,
        long needed,
        long applications,
        long applicationsMax,
        LocalDateTime deadline,
        String deadlineText,
        String deadlineTone,
        String statusLabel,
        String statusTone,
        String href) {
}
