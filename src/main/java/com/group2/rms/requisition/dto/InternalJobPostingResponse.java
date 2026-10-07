package com.group2.rms.requisition.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalJobPostingResponse {

    private Integer jobPostingId;
    private String postingTitle;
    private Integer requisitionId;
    private String requisitionCode;
    private Integer departmentId;
    private String departmentName;
    private Integer recruitmentRound;
    private String employmentType;
    private String postingStatus;
    private LocalDateTime applicationDeadline;
    private LocalDateTime postingDate;
    private LocalDateTime createdAt;
    private String createdByName;

    public boolean isExpired() {
        return applicationDeadline != null && applicationDeadline.isBefore(LocalDateTime.now());
    }
}
