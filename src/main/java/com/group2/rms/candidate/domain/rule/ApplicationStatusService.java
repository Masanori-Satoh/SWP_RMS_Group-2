package com.group2.rms.candidate.domain.rule;

import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.jobposting.repository.JobPostingRepository;

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
 *   Offered --ứng viên từ chối offer--> Withdrawn
 * </pre>
 * Trạng thái kết thúc trả lời "ai kết thúc đơn": {@code Rejected} công ty loại, {@code Withdrawn} ứng viên tự dừng
 * (rút đơn, từ chối offer), {@code Cancelled} đơn dừng vì tin bị đóng, {@code Hired} nhận việc.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ApplicationStatusService {

    static final Set<String> BEFORE_INTERVIEW = Set.of("HM_Passed");
    static final Set<String> IN_INTERVIEW = Set.of("HM_Passed", "Interviewing");
    static final Set<String> OFFERED = Set.of("Offered");

    public static final String REJECTED = "Rejected";
    public static final String WITHDRAWN = "Withdrawn";
    public static final String CANCELLED = "Cancelled";
    public static final String HIRED = "Hired";

    /** Một nguồn cho "đơn đã kết thúc": mọi danh sách {@code NOT IN} trạng thái kết thúc dùng hằng này. */
    public static final Set<String> TERMINAL = Set.of(HIRED, REJECTED, WITHDRAWN, CANCELLED);

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
            throw new ApplicationStatusException("Hồ sơ chưa đủ điều kiện hoặc đợt tuyển đã đóng.");
        var rows = applications.findProgressFacts(List.of(id));
        //check if cv is not empty  and stage of CV is Interview
        if (!rows.isEmpty() && ApplicationProgressResolver.resolve(ApplicationProgressFacts.from(rows.getFirst()),LocalDateTime.now()).stage() != ApplicationStage.INTERVIEW)
            throw new ApplicationStatusException("Hồ sơ đã chuyển sang vòng Offer hoặc đã kết thúc.");
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
            move(applicationId, IN_INTERVIEW, REJECTED);
        }
    }

    /** Ứng viên trả lời offer đã gửi: nhận thì {@code Hired}, từ chối thì {@code Withdrawn} (ứng viên tự dừng). */
    public void onOfferAnswered(Integer applicationId, boolean accepted) {
        move(applicationId, OFFERED, accepted ? HIRED : WITHDRAWN);
    }

    private void move(Integer applicationId, Set<String> from, String to) {
        if (applications.transition(applicationId, from, to) == 0) {
            log.debug("Đơn #{} không ở {} nên giữ nguyên trạng thái (không chuyển sang {}).", applicationId, from, to);
        }
    }
}
