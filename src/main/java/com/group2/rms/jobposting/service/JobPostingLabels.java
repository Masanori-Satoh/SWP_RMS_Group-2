package com.group2.rms.jobposting.service;

import com.group2.rms.admin.entity.AuditLog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/*
 * LABELS: đổi giá trị của tin (trạng thái, hạn nộp, audit) sang chữ tiếng Việt và tông màu badge.
 */
public final class JobPostingLabels {

    // Nhóm hạn nộp trên bộ lọc và màu cột hạn nộp
    public static final String DEADLINE_OPEN = "open";
    public static final String DEADLINE_SOON = "soon";
    public static final String DEADLINE_PAST = "past";
    /** Ô lọc "Hạn nộp" chỉ có Còn hạn / Đã quá hạn; "soon" chỉ dùng để tô màu từng dòng. */
    public static final List<String> DEADLINE_FILTERS = List.of(DEADLINE_OPEN, DEADLINE_PAST);
    /** Còn từ 0 đến 7 ngày là "sắp hết hạn". */
    public static final int DEADLINE_SOON_DAYS = 7;

    /** Chuỗi lương khi chọn "Thỏa thuận" (giống RequisitionLabels.salary khi yêu cầu không có lương). */
    public static final String SALARY_NEGOTIABLE = "Thỏa thuận";
    /** Cách viết cũ còn trong dữ liệu seed, vẫn coi là "Thỏa thuận" khi mở form sửa. */
    public static final String SALARY_NEGOTIABLE_OLD_SPELLING = "Thoả thuận";
    /** Chữ hiển thị khi thiếu dữ liệu liên quan (yêu cầu nguồn, người tạo...). */
    public static final String MISSING = "—";
    public static final String DEPARTMENT_MISSING = "Chưa xác định";
    /** Người thực hiện của dòng lịch sử không gắn tài khoản. */
    public static final String SYSTEM_ACTOR = "Hệ thống";

    // Câu mô tả ghi vào AuditLog.NewValue (JobPostingAuditLogger), và cũng là chữ để nhận ra loại hoạt động
    public static final String LOG_CREATE = "Tạo tin tuyển dụng";
    public static final String LOG_UPDATE = "Cập nhật tin tuyển dụng";
    public static final String LOG_PUBLISH = "Đăng tin tuyển dụng";
    public static final String LOG_PAUSE = "Tạm dừng tin tuyển dụng";
    public static final String LOG_DELETE = "Xóa tin tuyển dụng";
    /** TalentPoolService#closeJob ghi NewValue = "Closed" khi đóng tin. */
    private static final String LOG_CLOSED_BY_TALENT_POOL = JobPostingAccess.STATUS_CLOSED;

    private JobPostingLabels() {
        // Chỉ chứa hàm static
    }

    public static String statusLabel(String viewStatus) {
        if (viewStatus == null) {
            return MISSING;
        }
        return switch (viewStatus) {
            case JobPostingAccess.STATUS_DRAFT -> "Bản nháp";
            case JobPostingAccess.STATUS_PUBLISHED -> "Đang đăng";
            case JobPostingAccess.STATUS_PAUSED -> "Tạm dừng";
            case JobPostingAccess.STATUS_CLOSED -> "Đã đóng";
            default -> viewStatus;
        };
    }

    /** Màu badge theo class catalog: badge--success | warning | danger | neutral. */
    public static String statusTone(String viewStatus) {
        if (viewStatus == null) {
            return "neutral";
        }
        return switch (viewStatus) {
            case JobPostingAccess.STATUS_PUBLISHED -> "success";
            case JobPostingAccess.STATUS_PAUSED -> "warning";
            default -> "neutral";
        };
    }

    public static String deadlineFilterLabel(String value) {
        if (value == null) {
            return "";
        }
        return switch (value) {
            case DEADLINE_OPEN -> "Còn hạn";
            case DEADLINE_PAST -> "Đã quá hạn";
            default -> value;
        };
    }

    /** Nhóm hạn nộp past / soon / open; tin đã đóng hoặc không có hạn nộp thì null. */
    public static String deadlineState(String viewStatus, LocalDateTime deadline, LocalDateTime now) {
        if (deadline == null || JobPostingAccess.STATUS_CLOSED.equals(viewStatus)) {
            return null;
        }
        if (deadline.isBefore(now)) {
            return DEADLINE_PAST;
        }
        if (daysFromToday(deadline, now.toLocalDate()) <= DEADLINE_SOON_DAYS) {
            return DEADLINE_SOON;
        }
        return DEADLINE_OPEN;
    }

    /** Số ngày từ hôm nay tới một mốc thời gian (âm = đã qua). Tính theo ngày lịch, không theo giờ. */
    public static long daysFromToday(LocalDateTime moment, LocalDate today) {
        return ChronoUnit.DAYS.between(today, moment.toLocalDate());
    }

    /** Tên nút sang trang "Ứng viên tiềm năng" của talent pool: HM xem gợi ý cho mình, HR xem đề xuất của HM. */
    public static String suggestionsLinkLabel(boolean viewerIsHiringManager) {
        if (viewerIsHiringManager) {
            return "Ứng viên tiềm năng";
        }
        return "Ứng viên được HM đề xuất";
    }

    public static String recommendationsLinkLabel(boolean viewerIsHiringManager) {
        if (viewerIsHiringManager) {
            return "Gợi ý từ HR";
        }
        return "Gợi ý đã gửi cho HM";
    }

    /** Một dòng audit log → tên hoạt động ở mục "Lịch sử" của trang chi tiết. */
    public static String activityLabel(AuditLog log) {
        String detail = Objects.toString(log.getNewValue(), "");
        String action = Objects.toString(log.getAction(), "");
        if (JobPostingAuditLogger.ACTION_UPDATE.equalsIgnoreCase(action)) {
            return updateActivityLabel(detail);
        }
        if (JobPostingAuditLogger.ACTION_CREATE.equalsIgnoreCase(action)) {
            if (detail.startsWith(LOG_CREATE + ": " + JobPostingAccess.STATUS_PUBLISHED)) {
                return "Tạo và đăng tin";
            }
            return "Tạo mới";
        }
        if (JobPostingAuditLogger.ACTION_DELETE.equalsIgnoreCase(action)) {
            return "Xóa";
        }
        return "Tạo mới";
    }

    private static String updateActivityLabel(String detail) {
        if (detail.startsWith(LOG_PUBLISH) || detail.contains(JobPostingAccess.STATUS_PUBLISHED)) {
            return "Đăng tin";
        }
        if (detail.startsWith(LOG_PAUSE)) {
            return "Tạm dừng";
        }
        if (detail.equals(LOG_CLOSED_BY_TALENT_POOL)) {
            return "Đóng tin";
        }
        return "Cập nhật";
    }
}
