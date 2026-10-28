package com.group2.rms.jobposting.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Trang chi tiết tin tuyển dụng nội bộ (job-postings/detail.html).
 * Mọi nhãn, số liệu và cờ do JobPostingMapper#toDetail tính sẵn; template chỉ đọc, không tự so sánh vai trò
 * hay trạng thái.
 *
 * @param viewStatus             Draft, Published, Paused, Closed
 * @param daysUntilDeadline      số ngày còn lại tới hạn nộp (âm = đã quá); null nếu không có hạn nộp
 * @param draftBanner            hiện dải "Bản nháp, ứng viên chưa nhìn thấy"
 * @param pausedBanner           hiện dải "Đang tạm dừng nhận hồ sơ"
 * @param closedBanner           hiện dải "Tin đã đóng ngày ..."
 * @param deadlineSoonBanner     tin đang đăng, còn 0..7 ngày tới hạn nộp
 * @param deadlinePassedBanner   tin đang đăng nhưng đã quá hạn nộp
 * @param deadlineCountdown      hiện "· còn N ngày" cạnh hạn nộp (tin chưa đóng, chưa quá hạn)
 * @param deadlineOverdue        hiện "· quá hạn" cạnh hạn nộp (tin chưa đóng)
 * @param live                   tin đã lên trang tuyển dụng (đang đăng / tạm dừng / đã đóng): có số liệu hồ sơ
 * @param canManage              HR: hiện gợi ý "sửa hạn nộp"
 * @param repostable             tin đã đóng, yêu cầu vẫn đã duyệt và chưa có tin đang mở: tạo tin lần mới
 * @param publicViewable         ứng viên đang thấy tin trên trang tuyển dụng
 * @param talentPoolVisible      hiện khối "Ứng viên tiềm năng" (HR, HM)
 * @param pendingApplications    chờ xử lý: mới nộp hoặc AI đã chấm
 * @param rounds                 mọi lần đăng của cùng yêu cầu (gồm tin đang xem)
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
        long aiPassedApplications,
        BigDecimal aiThreshold,
        long interviewingApplications,
        long hiredApplications,
        List<RecentApplication> recentApplications,
        List<PostingRound> rounds) {

    /** Hồ sơ gần đây: điểm AI null = chưa chấm. */
    public record RecentApplication(Integer applicationId, String candidateName, BigDecimal aiScore,
            boolean aiPassed, String statusLabel, String statusBadge, LocalDateTime submittedAt) {
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
    public record ApplicationStats(long total, long last7Days, long pending, long aiPassed, BigDecimal aiThreshold,
            long interviewing, long hired, List<RecentApplication> recent) {

        /** Tin chưa lên trang tuyển dụng: chưa có số liệu. */
        public static ApplicationStats empty(BigDecimal aiThreshold) {
            return new ApplicationStats(0, 0, 0, 0, aiThreshold, 0, 0, List.of());
        }
    }
}
