package com.group2.rms.jobposting.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/*
 * DTO: trang chi tiết tin tuyển dụng; mọi nhãn và cờ do JobPostingMapper#toDetail tính sẵn.
 */
@Builder
public record InternalJobPostingDetailResponse(
        Integer jobPostingId,
        String postingTitle,
        Integer requisitionId,
        String requisitionCode,
        String requisitionTitle,
        String departmentName,
        Integer numberOfPositions,
        String employmentType,
        String workModelLabel,
        LocalDate expectedStartDate,
        String workLocation,
        String salaryDisplay,
        String numberOfPositionsDisplay,
        Integer postingNo,
        LocalDateTime closedAt,
        String jobDescription,
        String jobRequirements,
        String benefits,
        String postingStatus,
        String viewStatus,
        String postingStatusLabel,
        String statusTone,
        LocalDateTime postingDate,
        LocalDateTime applicationDeadline,
        Long daysUntilDeadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdByName,
        String hiringManagerName,
        List<ActivityHistoryItem> activityHistory,

        // Dải thông báo và nhắc hạn nộp
        boolean draftBanner,
        boolean pausedBanner,
        boolean closedBanner,
        boolean deadlineSoonBanner,
        boolean deadlinePassedBanner,
        boolean deadlineCountdown,
        boolean deadlineOverdue,
        boolean live,
        boolean canManage,

        // Cờ thao tác của HR
        boolean publishable,
        boolean editable,
        boolean pausable,
        boolean reopenable,
        boolean closable,
        boolean deletable,
        boolean repostable,
        boolean publicViewable,

        // Khối "Ứng viên tiềm năng" (module talent pool)
        boolean talentPoolVisible,
        String suggestionsLinkLabel,
        String recommendationsLinkLabel,

        // Số liệu hồ sơ
        long totalApplications,
        long applicationsLast7Days,
        long pendingApplications,
        long passedCvRoundApplications,
        long interviewingApplications,
        long hiredApplications,
        List<RecentApplication> recentApplications,
        List<PostingRound> rounds) {

    /** Hồ sơ gần đây. */
    public record RecentApplication(Integer applicationId, String candidateName,
            String statusLabel, String statusBadge, LocalDateTime submittedAt) {
    }

    /** Một lần đăng của yêu cầu. */
    public record PostingRound(Integer jobPostingId, Integer postingNo, LocalDateTime postingDate,
            LocalDateTime deadline, String statusLabel, String statusTone, long applications, long hired,
            boolean current) {
    }

    /** Một dòng ở mục "Lịch sử". */
    public record ActivityHistoryItem(Integer no, String action, String performedBy, LocalDateTime timestamp) {
    }

    /** Số liệu hồ sơ của tin, service tra rồi truyền cho JobPostingMapper#toDetail. */
    @Builder
    public record ApplicationStats(long total, long last7Days, long pending, long passedCvRound,
            long interviewing, long hired, List<RecentApplication> recent) {

        /** Tin chưa lên trang tuyển dụng: chưa có số liệu. */
        public static ApplicationStats empty() {
            return new ApplicationStats(0, 0, 0, 0, 0, 0, List.of());
        }
    }
}
