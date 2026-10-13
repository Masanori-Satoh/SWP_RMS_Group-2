package com.group2.rms.candidate.service;

import com.group2.rms.candidate.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Đổi trạng thái đơn theo việc đã xảy ra ở module khác (interview, offer gọi sang; chiều phụ thuộc hợp lệ).
 * Chạy chung transaction với bên gọi. Đơn không còn ở trạng thái mong đợi thì giữ nguyên, không ném lỗi:
 * trạng thái chỉ đi tiến (SRS GBR-01) và không được chặn thao tác của module gọi.
 * <pre>
 *   HM_Passed --xếp lịch / chốt Đạt--> Interviewing
 *   HM_Passed, Interviewing --chốt Không đạt--> Rejected
 * </pre>
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ApplicationStatusService {

    static final Set<String> BEFORE_INTERVIEW = Set.of("HM_Passed");
    static final Set<String> IN_INTERVIEW = Set.of("HM_Passed", "Interviewing");

    private final ApplicationRepository applications;

    /** HR xếp lịch phỏng vấn. */
    public void onInterviewScheduled(Integer applicationId) {
        move(applicationId, BEFORE_INTERVIEW, "Interviewing");
    }

    /**
     * Hiring Manager chốt kết quả phỏng vấn. Đạt: giữ (hoặc đưa về) {@code Interviewing} để HR lập offer;
     * đơn cũ chưa qua {@link #onInterviewScheduled} cũng được đưa sang.
     */
    public void onInterviewDecided(Integer applicationId, boolean passed) {
        if (passed) {
            move(applicationId, BEFORE_INTERVIEW, "Interviewing");
        } else {
            move(applicationId, IN_INTERVIEW, ApplicationReviewService.REJECTED);
        }
    }

    private void move(Integer applicationId, Set<String> from, String to) {
        if (applications.transition(applicationId, from, to) == 0) {
            log.debug("Đơn #{} không ở {} nên giữ nguyên trạng thái (không chuyển sang {}).", applicationId, from, to);
        }
    }
}
