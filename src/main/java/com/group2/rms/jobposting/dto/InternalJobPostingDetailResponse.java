package com.group2.rms.jobposting.dto;

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
    private String numberOfPositionsDisplay;
    private Integer postingNo;
    private LocalDateTime closedAt;
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

    // ----- Bổ sung theo mockup (service tính sẵn, template chỉ đọc) -----
    /** Draft, Scheduled (hẹn đăng), Published, Paused, Closed. */
    private String viewStatus;
    private String statusTone;
    private String workModelLabel;
    private java.time.LocalDate expectedStartDate;
    /** Số ngày còn lại tới hạn nộp (âm = đã quá); null nếu không có hạn nộp. */
    private Long daysUntilDeadline;
    /** Tin đã lên trang tuyển dụng (đang đăng / tạm dừng / đã đóng): có số liệu hồ sơ. */
    private boolean live;
    private boolean canManage;

    // Cờ thao tác (HR, System Admin; "Đóng tin" chỉ HR)
    private boolean publishable;
    private boolean publishNowable;
    private boolean unschedulable;
    private boolean editable;
    private boolean pausable;
    private boolean reopenable;
    private boolean closable;
    private boolean deletable;
    /** Tin đã đóng, yêu cầu vẫn đã duyệt và chưa có tin đang mở: tạo tin lần mới. */
    private boolean repostable;
    /** Ứng viên đang thấy tin trên trang tuyển dụng. */
    private boolean publicViewable;

    // Số liệu hồ sơ
    private long totalApplications;
    private long applicationsLast7Days;
    /** Chờ xử lý: mới nộp hoặc AI đã chấm. */
    private long pendingApplications;
    private long aiPassedApplications;
    private java.math.BigDecimal aiThreshold;
    private long interviewingApplications;
    private long hiredApplications;
    private List<RecentApplication> recentApplications;
    /** Mọi lần đăng của cùng yêu cầu (gồm tin đang xem). */
    private List<PostingRound> rounds;

    /** Hồ sơ gần đây: điểm AI null = chưa chấm. */
    public record RecentApplication(Integer applicationId, String candidateName, java.math.BigDecimal aiScore,
            boolean aiPassed, String statusLabel, String statusBadge, LocalDateTime submittedAt) {}

    /** Một lần đăng của yêu cầu. */
    public record PostingRound(Integer jobPostingId, Integer postingNo, LocalDateTime postingDate,
            LocalDateTime deadline, String statusLabel, String statusTone, long applications, long hired, boolean current) {}

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
