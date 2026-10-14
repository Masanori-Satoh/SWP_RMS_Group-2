package com.group2.rms.candidate.service;

import com.group2.rms.candidate.exception.TalentPoolException;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.requisition.repository.JobPostingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Đổi trạng thái đơn theo việc đã xảy ra ở module khác (interview, offer gọi
 * sang; chiều phụ thuộc hợp lệ).
 * Chạy chung transaction với bên gọi. Đơn không còn ở trạng thái mong đợi thì
 * giữ nguyên, không ném lỗi:
 * trạng thái chỉ đi tiến (SRS GBR-01) và không được chặn thao tác của module
 * gọi.
 * 
 * <pre>
 *   HM_Passed --xếp lịch / chốt Đạt--> Interviewing
 *   HM_Passed, Interviewing --chốt Không đạt--> Rejected
 *   Offered --ứng viên nhận offer--> Hired
 *   Offered --ứng viên từ chối offer--> Rejected
 * </pre>
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ApplicationStatusService {

    static final Set<String> BEFORE_INTERVIEW = Set.of("HM_Passed");
    static final Set<String> IN_INTERVIEW = Set.of("HM_Passed", "Interviewing");
    static final Set<String> OFFERED = Set.of("Offered");

    private final ApplicationRepository applications;
    private final JobPostingRepository jobs;

    public void requireScheduleable(Integer id) {
        // check if application is exist
        var app = applications.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ."));
        // check if job posting is exist
        var job = jobs.findForUpdate(app.getJobPosting().getJobPostingId()).orElseThrow();
        // check if job posting is not published and application status is not valid
        if (!"Published".equals(job.getPostingStatus()) || !IN_INTERVIEW.contains(app.getApplicationStatus()))
            throw new TalentPoolException("Hồ sơ chưa đủ điều kiện hoặc đợt tuyển đã đóng.");
        var rows = applications.findProgressFacts(List.of(id));
        //check if cv is not empty  and stage of CV is Interview
        if (!rows.isEmpty() && ApplicationProgressResolver.resolve(ApplicationProgressFacts.from(rows.getFirst()),LocalDateTime.now()).stage() != ApplicationStage.INTERVIEW)
            throw new TalentPoolException("Hồ sơ đã chuyển sang vòng Offer hoặc đã kết thúc.");
    }

    /** HR xếp lịch phỏng vấn. */
    public void onInterviewScheduled(Integer applicationId) {
        move(applicationId, BEFORE_INTERVIEW, "Interviewing");
    }

    /**
     * Hiring Manager chốt kết quả phỏng vấn. Đạt: giữ (hoặc đưa về)
     * {@code Interviewing} để HR lập offer;
     * đơn cũ chưa qua {@link #onInterviewScheduled} cũng được đưa sang.
     */
    public void onInterviewDecided(Integer applicationId, boolean passed) {
        if (passed) {
            move(applicationId, BEFORE_INTERVIEW, "Interviewing");
        } else {
            move(applicationId, IN_INTERVIEW, ApplicationReviewService.REJECTED);
        }
    }

    /** Ứng viên trả lời offer đã gửi: nhận thì {@code Hired}, từ chối thì {@code Rejected}. */
    public void onOfferAnswered(Integer applicationId, boolean accepted) {
        move(applicationId, OFFERED, accepted ? "Hired" : ApplicationReviewService.REJECTED);
    }

    private void move(Integer applicationId, Set<String> from, String to) {
        if (applications.transition(applicationId, from, to) == 0) {
            log.debug("Đơn #{} không ở {} nên giữ nguyên trạng thái (không chuyển sang {}).", applicationId, from, to);
        }
    }
}
