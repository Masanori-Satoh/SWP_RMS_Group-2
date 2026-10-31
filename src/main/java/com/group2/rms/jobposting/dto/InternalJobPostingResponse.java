package com.group2.rms.jobposting.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/*
 * DTO: một dòng của màn danh sách tin; nhãn, số ngày, cờ thao tác do JobPostingMapper#toListRow tính sẵn.
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
