package com.group2.rms.dashboard.internal.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;

/*
 * PERIOD: tuần đang xem trên lịch phỏng vấn của Interviewer.
 * Không gọi now(): người gọi truyền "hôm nay" lấy từ Clock.
 */
public final class DashboardPeriod {

    private DashboardPeriod() {
        // Chỉ chứa hàm static, không tạo đối tượng
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
}
