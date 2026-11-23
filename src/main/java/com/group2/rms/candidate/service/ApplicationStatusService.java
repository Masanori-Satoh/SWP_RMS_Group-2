package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.ApplicationProgressFacts;
import com.group2.rms.candidate.entity.ApplicationStage;
import com.group2.rms.candidate.exception.ApplicationStatusException;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Service cập nhật trạng thái hồ sơ ứng tuyển theo sự kiện phỏng vấn và thư mời làm việc.
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

    public static final Set<String> TERMINAL = Set.of(HIRED, REJECTED, WITHDRAWN, CANCELLED);

    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final NotificationService notificationService;

    public void requireScheduleable(Integer id) {
        var app = applicationRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ."));
        var job = jobPostingRepository.findForUpdate(app.getJobPosting().getJobPostingId()).orElseThrow();
        if (!"Published".equals(job.getPostingStatus()) || !IN_INTERVIEW.contains(app.getApplicationStatus())) {
            throw new ApplicationStatusException("Hồ sơ chưa đủ điều kiện hoặc đợt tuyển đã đóng.");
        }
        var rows = applicationRepository.findProgressFacts(List.of(id));
        if (!rows.isEmpty()) {
            ApplicationProgressFacts facts = ApplicationProgressFacts.from(rows.getFirst());
            if (ApplicationProgressResolver.resolve(facts, LocalDateTime.now()).stage() != ApplicationStage.INTERVIEW) {
                throw new ApplicationStatusException("Hồ sơ đã chuyển sang vòng Thư mời hoặc đã kết thúc.");
            }
        }
    }

    /**
     * Cập nhật trạng thái hồ sơ khi xếp lịch phỏng vấn.
     */
    public void onInterviewScheduled(Integer applicationId) {
        move(applicationId, BEFORE_INTERVIEW, "Interviewing");
    }

    /**
     * Cập nhật trạng thái hồ sơ sau khi có kết quả phỏng vấn.
     */
    public void onInterviewDecided(Integer applicationId, boolean passed) {
        if (passed) {
            move(applicationId, BEFORE_INTERVIEW, "Interviewing");
        } else if (move(applicationId, IN_INTERVIEW, REJECTED)) {
            applicationRepository.findDetailById(applicationId)
                    .ifPresent(notificationService::notifyApplicationRejected);
        }
    }

    /**
     * Cập nhật trạng thái hồ sơ sau khi ứng viên phản hồi thư mời làm việc.
     */
    public void onOfferAnswered(Integer applicationId, boolean accepted) {
        move(applicationId, OFFERED, accepted ? HIRED : WITHDRAWN);
    }

    private boolean move(Integer applicationId, Set<String> from, String to) {
        if (applicationRepository.transition(applicationId, from, to) == 0) {
            log.debug("Đơn #{} không ở {} nên giữ nguyên trạng thái (không chuyển sang {}).", applicationId, from, to);
            return false;
        }
        return true;
    }
}
