package com.group2.rms.dashboard.internal.dto;

/*
 * DTO: một đề nghị tuyển dụng (offer) đang chờ Giám đốc duyệt.
 */
public record PendingOfferItem(
        String candidateName,
        String positionTitle,
        String salaryText,
        String waitText,
        String waitTone,
        String href) {
}
