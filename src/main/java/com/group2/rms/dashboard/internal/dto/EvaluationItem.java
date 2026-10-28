package com.group2.rms.dashboard.internal.dto;

import java.time.LocalDateTime;

/*
 * DTO: một buổi phỏng vấn đã xong nhưng Interviewer chưa gửi phiếu đánh giá.
 */
public record EvaluationItem(
        String candidateName,
        String positionTitle,
        LocalDateTime interviewEnd,
        String waitText,
        String waitTone,
        String href) {
}
