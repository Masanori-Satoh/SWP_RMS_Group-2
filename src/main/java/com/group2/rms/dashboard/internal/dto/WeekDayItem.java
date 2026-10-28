package com.group2.rms.dashboard.internal.dto;

import java.time.LocalDate;
import java.util.List;

/*
 * DTO: một cột ngày trong "Lịch tuần này" của Interviewer.
 */
public record WeekDayItem(
        String weekday,
        LocalDate date,
        boolean today,
        List<DashboardInterviewItem> interviews) {
}
