package com.group2.rms.interview.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator cho annotation @ValidInterviewTime.
 *
 * PHÒNG TRÁNH LỖI NULLPOINTEREXCEPTION:
 * Nếu client gửi thiếu startTime hoặc endTime (giá trị bị null), validator này sẽ trả về true
 * để nhường quyền báo lỗi chi tiết cho các annotation @NotNull trên từng trường cụ thể.
 * Chỉ khi cả 2 giá trị đều KHÁC NULL, validator mới tiến hành so sánh logic endTime.isAfter(startTime).
 */
public class ValidInterviewTimeValidator implements ConstraintValidator<ValidInterviewTime, InterviewScheduleRequest> {

    @Override
    public boolean isValid(InterviewScheduleRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        // Null-safety check: Tránh NullPointerException khi client gửi thiếu dữ liệu
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return true;
        }

        // Ràng buộc Rule MSG26: endTime phải strictly after startTime
        return request.getEndTime().isAfter(request.getStartTime());
    }
}
