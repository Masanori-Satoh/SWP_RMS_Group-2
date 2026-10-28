package com.group2.rms.dashboard.internal.dto;

/*
 * DTO: một yêu cầu tuyển dụng đang chờ Giám đốc duyệt.
 * replacement = tuyển thay thế (có người đã nghỉ) để template nhấn màu; roundText = null khi là đợt 1.
 */
public record PendingRequisitionItem(
        String code,
        String title,
        String departmentName,
        String requesterName,
        int positions,
        String hiringTypeLabel,
        boolean replacement,
        String roundText,
        String waitText,
        String waitTone,
        String href) {
}
