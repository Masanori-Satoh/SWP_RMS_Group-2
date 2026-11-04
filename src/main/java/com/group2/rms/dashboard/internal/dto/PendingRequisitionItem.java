package com.group2.rms.dashboard.internal.dto;

/*
 * DTO: một yêu cầu tuyển dụng đang chờ Giám đốc duyệt.
 * roundText = null khi là đợt 1 (đợt 2 trở đi template nhấn màu).
 */
public record PendingRequisitionItem(
        String code,
        String title,
        String departmentName,
        String requesterName,
        int positions,
        String roundText,
        String waitText,
        String waitTone,
        String href) {
}
