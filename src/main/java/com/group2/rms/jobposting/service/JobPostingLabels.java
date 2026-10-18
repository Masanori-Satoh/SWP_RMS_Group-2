package com.group2.rms.jobposting.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Nơi duy nhất đổi giá trị trạng thái tin tuyển dụng sang chữ tiếng Việt và màu badge.
 * Template chỉ đọc kết quả đã tính sẵn, không tự so sánh chuỗi trạng thái.
 *
 * Trạng thái HIỂN THỊ (viewStatus) khác trạng thái lưu trong DB (PostingStatus) ở một điểm:
 * "Hẹn đăng" (Scheduled) không có trong DB. Đó là tin Published nhưng PostingDate còn ở tương lai,
 * nên ứng viên chưa thấy tin cho tới đúng giờ đăng.
 */
public final class JobPostingLabels {

    // Giá trị PostingStatus trong DB (CK_JobPosting_Status)
    public static final String STATUS_DRAFT = "Draft";
    public static final String STATUS_PUBLISHED = "Published";
    public static final String STATUS_PAUSED = "Paused";
    public static final String STATUS_CLOSED = "Closed";
    /** Chỉ dùng để hiển thị và lọc: Published + PostingDate ở tương lai. */
    public static final String STATUS_SCHEDULED = "Scheduled";

    /** Thứ tự trạng thái trên bộ lọc và badge. */
    public static final List<String> VIEW_STATUSES =
            List.of(STATUS_DRAFT, STATUS_SCHEDULED, STATUS_PUBLISHED, STATUS_PAUSED, STATUS_CLOSED);
    /** HM và Director không xem tin nháp. */
    public static final List<String> VIEW_STATUSES_WITHOUT_DRAFT =
            List.of(STATUS_SCHEDULED, STATUS_PUBLISHED, STATUS_PAUSED, STATUS_CLOSED);

    // Nhóm hạn nộp trên bộ lọc
    public static final String DEADLINE_OPEN = "open";
    public static final String DEADLINE_SOON = "soon";
    public static final String DEADLINE_PAST = "past";
    public static final List<String> DEADLINE_FILTERS = List.of(DEADLINE_OPEN, DEADLINE_SOON, DEADLINE_PAST);
    /** Còn từ 0 đến 7 ngày là "sắp hết hạn". */
    public static final int DEADLINE_SOON_DAYS = 7;

    private JobPostingLabels() {
        // Chỉ chứa hàm static
    }

    /** Trạng thái hiển thị của một tin: tách "Hẹn đăng" ra khỏi "Đang đăng". */
    public static String viewStatus(String postingStatus, LocalDateTime postingDate, LocalDateTime now) {
        if (STATUS_PUBLISHED.equals(postingStatus) && postingDate != null && postingDate.isAfter(now)) {
            return STATUS_SCHEDULED;
        }
        return postingStatus;
    }

    public static String statusLabel(String viewStatus) {
        if (viewStatus == null) {
            return "—";
        }
        return switch (viewStatus) {
            case STATUS_DRAFT -> "Bản nháp";
            case STATUS_SCHEDULED -> "Hẹn đăng";
            case STATUS_PUBLISHED -> "Đang đăng";
            case STATUS_PAUSED -> "Tạm dừng";
            case STATUS_CLOSED -> "Đã đóng";
            default -> viewStatus;
        };
    }

    /** Màu badge theo class catalog: badge--success | warning | danger | neutral. */
    public static String statusTone(String viewStatus) {
        if (viewStatus == null) {
            return "neutral";
        }
        return switch (viewStatus) {
            case STATUS_PUBLISHED -> "success";
            case STATUS_SCHEDULED, STATUS_PAUSED -> "warning";
            default -> "neutral";
        };
    }

    public static String deadlineFilterLabel(String value) {
        if (value == null) {
            return "";
        }
        return switch (value) {
            case DEADLINE_OPEN -> "Còn hạn (trên " + DEADLINE_SOON_DAYS + " ngày)";
            case DEADLINE_SOON -> "Sắp hết hạn (≤ " + DEADLINE_SOON_DAYS + " ngày)";
            case DEADLINE_PAST -> "Đã quá hạn";
            default -> value;
        };
    }

    /** Số ngày từ hôm nay tới một mốc thời gian (âm = đã qua). Tính theo ngày lịch, không theo giờ. */
    public static long daysFromToday(LocalDateTime moment, LocalDate today) {
        return ChronoUnit.DAYS.between(today, moment.toLocalDate());
    }
}
