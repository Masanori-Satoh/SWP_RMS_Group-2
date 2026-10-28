package com.group2.rms.dashboard.internal.dto;

import java.time.LocalDateTime;

/*
 * DTO: một quyết định gần đây của Giám đốc (duyệt / từ chối yêu cầu hoặc đề nghị tuyển dụng).
 */
public record DecisionItem(
        String typeLabel,
        String title,
        String statusLabel,
        String statusTone,
        LocalDateTime decidedAt) {
}
