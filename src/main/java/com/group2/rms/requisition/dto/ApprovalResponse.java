package com.group2.rms.requisition.dto;

import java.time.LocalDateTime;

import lombok.Builder;

/** Một lần Giám đốc duyệt / trả lại yêu cầu, hiện ở mục "Lịch sử phê duyệt" của màn chi tiết. */
@Builder
public record ApprovalResponse(
        String approverName,
        String status,
        LocalDateTime approvalDate,
        String comments) {
}
