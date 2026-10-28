package com.group2.rms.dashboard.internal.dto;

import java.time.LocalDateTime;

/*
 * DTO: một dòng "Yêu cầu gần đây" trên dashboard Hiring Manager.
 */
public record RequisitionItem(
        String code,
        String title,
        int positions,
        String statusLabel,
        String statusTone,
        LocalDateTime updatedAt,
        String href) {
}
