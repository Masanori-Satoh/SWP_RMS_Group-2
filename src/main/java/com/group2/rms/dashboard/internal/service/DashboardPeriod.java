package com.group2.rms.dashboard.internal.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

    /** Rule (D13): thứ Hai của tuần cần xem; giá trị lạ hoặc trống thì về tuần hiện tại. */
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
