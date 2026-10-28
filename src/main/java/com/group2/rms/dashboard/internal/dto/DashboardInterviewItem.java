package com.group2.rms.dashboard.internal.dto;

import java.time.LocalDateTime;

/*
 * DTO: một buổi phỏng vấn hiển thị trên dashboard (giờ, ứng viên, vị trí, hình thức, số người trong hội đồng).
 * finished = buổi đã hoàn tất hoặc đã qua giờ kết thúc (lịch tuần tô xám).
 */
public record DashboardInterviewItem(
        LocalDateTime startTime,
        LocalDateTime endTime,
        String candidateName,
        String positionTitle,
        String formatLabel,
        String location,
        int panelSize,
        String stateLabel,
        String stateTone,
        String href,
        boolean finished) {
}
