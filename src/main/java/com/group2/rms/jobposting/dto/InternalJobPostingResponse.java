package com.group2.rms.jobposting.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Một dòng của màn danh sách tin tuyển dụng.
 */
@Builder
public record InternalJobPostingResponse(
        Integer jobPostingId,
        String postingTitle,
        String departmentName,
        String employmentType,
        LocalDateTime applicationDeadline,
        LocalDateTime postingDate,
        String createdByName,
        Integer postingNo,
        String viewStatus,
        String statusLabel,
        String statusTone,
        long applicationCount,
        long newApplicationCount,
        Long daysUntilPosting,
        Long daysUntilDeadline,
        String deadlineState,
        boolean publishable,
        boolean reopenable,
        boolean editable,
        boolean pausable,
        boolean closable,
        boolean deletable,
        boolean applicationsViewable) {
}
