package com.group2.rms.dashboard.internal.service;

import com.group2.rms.interview.entity.InterviewFormat;
import com.group2.rms.interview.entity.InterviewStatus;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/*
 * LABELS: chữ tiếng Việt và tông màu dùng chung cho các dashboard (số ngày chờ, hình thức, trạng thái phỏng vấn).
 * Nhãn trạng thái yêu cầu tuyển dụng lấy ở RequisitionLabels, không viết lại ở đây.
 */
public final class DashboardLabels {

    /** Chờ từ 3 ngày: tô cam để người dùng chú ý. */
    public static final int WAIT_WARNING_DAYS = 3;
    /** Chờ từ 7 ngày: tô đỏ, coi là quá hạn xử lý. */
    public static final int WAIT_DANGER_DAYS = 7;

    private DashboardLabels() {
        // Chỉ chứa hàm static, không tạo đối tượng
    }

    /** Số ngày lịch đã trôi qua từ "since" đến "now" (gửi hôm qua lúc 23:00 = 1 ngày). */
    public static long waitDays(LocalDateTime since, LocalDateTime now) {
        if (since == null || since.isAfter(now)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(since.toLocalDate(), now.toLocalDate());
    }

    public static String waitText(long days) {
        if (days == 0) {
            return "Hôm nay";
        }
        return days + " ngày";
    }

    public static String waitTone(long days) {
        if (days >= WAIT_DANGER_DAYS) {
            return "danger";
        }
        if (days >= WAIT_WARNING_DAYS) {
            return "warning";
        }
        return "neutral";
    }

    /** Hình thức phỏng vấn (InterviewSchedule.InterviewFormat). */
    public static String interviewFormat(String format) {
        if (InterviewFormat.Online_GoogleMeet.name().equals(format)) {
            return "Trực tuyến";
        }
        if (InterviewFormat.Offline_Office.name().equals(format)) {
            return "Tại văn phòng";
        }
        return format;
    }

    /** Trạng thái lịch phỏng vấn (InterviewSchedule.InterviewStatus); giá trị lạ giữ nguyên. */
    public static String interviewStatus(String status) {
        for (InterviewStatus interviewStatus : InterviewStatus.values()) {
            if (interviewStatus.name().equals(status)) {
                return interviewStatus.getDisplayName();
            }
        }
        return status;
    }

    /** Đổi lịch = cam để người xem biết giờ đã thay đổi; các trạng thái khác = xám. */
    public static String interviewStatusTone(String status) {
        if (InterviewStatus.Rescheduled.name().equals(status)) {
            return "warning";
        }
        return "neutral";
    }

    /** Số tiền đầy đủ có dấu chấm ngăn cách hàng nghìn: 15000000 → "15.000.000 VND". */
    public static String money(BigDecimal amount) {
        if (amount == null) {
            return "Chưa nhập";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        return format.format(amount) + " VND";
    }

    /** Tên thứ ngắn kiểu Việt Nam: thứ Hai → "T2", Chủ nhật → "CN". */
    public static String weekday(LocalDate day) {
        DayOfWeek dayOfWeek = day.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SUNDAY) {
            return "CN";
        }
        return "T" + (dayOfWeek.getValue() + 1);
    }
}
