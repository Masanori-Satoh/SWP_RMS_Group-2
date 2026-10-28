package com.group2.rms.dashboard.dto;

import java.time.LocalDateTime;

/*
 * DTO: một buổi phỏng vấn hiển thị trên dashboard (giờ, ứng viên, vị trí, hình thức, số người trong hội đồng).
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
        String href) {
}
