package com.group2.rms.dashboard.internal.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;

/*
 * PERIOD: kỳ so sánh của thẻ KPI "dòng chảy" (30 ngày gần nhất so với 30 ngày trước đó) và tuần của lịch.
 * Không gọi now(): người gọi truyền "bây giờ" lấy từ Clock.
 */
public final class DashboardPeriod {

    /** Rule (D5): KPI dòng chảy đếm trong 30 ngày gần nhất. */
    public static final int WINDOW_DAYS = 30;

    private DashboardPeriod() {
        // Chỉ chứa hàm static, không tạo đối tượng
    }

    /** Đầu kỳ hiện tại: [currentFrom, now). */
    public static LocalDateTime currentFrom(LocalDateTime now) {
        return now.minusDays(WINDOW_DAYS);
    }

    /** Đầu kỳ trước: [previousFrom, currentFrom). */
    public static LocalDateTime previousFrom(LocalDateTime now) {
        return now.minusDays(WINDOW_DAYS * 2L);
    }

    /** Mũi tên % so với kỳ trước: "↑ 19%", "↓ 5%", "0%". Kỳ trước bằng 0 thì không so được. */
    public static String changeText(long current, long previous) {
        if (previous == 0) {
            if (current > 0) {
                return "Mới";
            }
            return null;
        }
        long percent = Math.round((current - previous) * 100.0 / previous);
        return arrow(percent) + Math.abs(percent) + "%";
    }

    /** Chênh lệch giữa hai tỷ lệ phần trăm, tính bằng điểm: "↑ 7 điểm". */
    public static String pointChangeText(Integer currentPercent, Integer previousPercent) {
        if (currentPercent == null || previousPercent == null) {
            return null;
        }
        int points = currentPercent - previousPercent;
        return arrow(points) + Math.abs(points) + " điểm";
    }

    /** Tăng = tốt (xanh), giảm = xấu (đỏ). Mọi KPI dòng chảy hiện có đều theo chiều này. */
    public static String changeTone(long current, long previous) {
        if (current > previous) {
            return "success";
        }
        if (current < previous) {
            return "danger";
        }
        return "neutral";
    }

    /** Tỷ lệ phần trăm làm tròn; mẫu số 0 thì không có tỷ lệ (null). */
    public static Integer percent(long part, long whole) {
        if (whole == 0) {
            return null;
        }
        return (int) Math.round(part * 100.0 / whole);
    }

    /**
     * Thứ Hai của tuần cần xem. Rule (D13): giá trị lạ hoặc trống thì quay về tuần hiện tại,
     * ngày bất kỳ trong tuần cũng được đưa về thứ Hai.
     */
    public static LocalDate weekStart(String requestedDay, LocalDate today) {
        LocalDate day = today;
        if (requestedDay != null && !requestedDay.isBlank()) {
            try {
                day = LocalDate.parse(requestedDay.strip());
            } catch (DateTimeParseException e) {
                day = today;
            }
        }
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static String arrow(long change) {
        if (change > 0) {
            return "↑ ";
        }
        if (change < 0) {
            return "↓ ";
        }
        return "";
    }
}
