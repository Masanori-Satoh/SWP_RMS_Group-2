package com.group2.rms.interview.service;

import com.group2.rms.interview.dto.InterviewResultDetailResponse;
import com.group2.rms.user.entity.User;

/**
 * Xem kết quả phỏng vấn (Interview Result Details - SRS 5.1.28).
 */
public interface InterviewResultService {

    /**
     * Kết quả của một lịch phỏng vấn theo góc nhìn của người xem (D5):
     * HR, Hiring Manager, Director, System Admin thấy mọi phiếu; Interviewer chỉ vào được lịch mình
     * được gán và chỉ thấy phiếu của mình cho tới khi có Final Result (GBR-05).
     * canFinalize chỉ bật cho Hiring Manager phụ trách phòng ban của vị trí, khi lịch chưa được chốt.
     *
     * @throws com.group2.rms.core.exception.ResourceNotFoundException không có lịch
     * @throws org.springframework.security.access.AccessDeniedException người xem không được phép xem lịch này
     */
    InterviewResultDetailResponse getResult(Long interviewId, User viewer);
}
