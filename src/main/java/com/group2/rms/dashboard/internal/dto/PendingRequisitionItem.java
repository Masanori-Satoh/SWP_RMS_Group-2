package com.group2.rms.dashboard.internal.dto;

/*
 * DTO: một yêu cầu tuyển dụng đang chờ Giám đốc duyệt.
 * staffingDateText = null với dữ liệu cũ chưa có ngày dự kiến.
 */
public record PendingRequisitionItem(
        String code,
        String title,
        String departmentName,
        String requesterName,
        int positions,
        String staffingDateText,
        String waitText,
        String waitTone,
        String href) {
}
