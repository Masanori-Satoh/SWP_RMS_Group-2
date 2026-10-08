package com.group2.rms.requisition.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalJobPostingDetailResponse {

    private Integer jobPostingId;
    private String postingTitle;
    private Integer requisitionId;
    private String requisitionCode;
    private String requisitionTitle;
    private Integer departmentId;
    private String departmentName;
    private Integer recruitmentRound;
    private Integer numberOfPositions;
    private String employmentType;
    private String workModel;
    private String workLocation;
    private String salaryDisplay;
    private String jobDescription;
    private String jobRequirements;
    private String benefits;
    private String postingStatus;
    private String postingStatusLabel;
    private LocalDateTime postingDate;
    private LocalDateTime applicationDeadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdByName;
    private String createdByEmail;
    private String hiringManagerName;
    private String hiringManagerEmail;
    private String requisitionApprovalStatus;
    private List<String> screeningCriteria;
    private List<ActivityHistoryItem> activityHistory;

    public boolean isExpired() {
        return applicationDeadline != null && applicationDeadline.isBefore(LocalDateTime.now());
    }

    public boolean isPublished() {
        return "Published".equalsIgnoreCase(postingStatus);
    }

    public String getPostingCode() {
        if (jobPostingId == null) {
            return "POST-000";
        }
        int year = (createdAt != null) ? createdAt.getYear() : 2026;
        return String.format("POST-%d-%03d", year, jobPostingId);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityHistoryItem {
        private Integer no;
        private String action;
        private String performedBy;
        private LocalDateTime timestamp;
    }
}
