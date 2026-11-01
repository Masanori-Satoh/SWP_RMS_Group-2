package com.group2.rms.dashboard.internal.dto;

/*
 * DTO: một đề nghị tuyển dụng (offer) đang chờ Giám đốc duyệt; initials = chữ viết tắt cho ô tròn đầu dòng.
 */
public record PendingOfferItem(
        String candidateName,
        String initials,
        String positionTitle,
        String salaryText,
        String waitText,
        String waitTone,
        String href) {
}
