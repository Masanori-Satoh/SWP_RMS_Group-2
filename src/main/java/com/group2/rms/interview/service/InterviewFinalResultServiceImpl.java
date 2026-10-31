package com.group2.rms.interview.service;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.candidate.domain.rule.ApplicationStatusService;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.interview.dto.SubmitFinalResultRequest;
import com.group2.rms.interview.entity.InterviewFinalResult;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.interview.exception.InterviewFinalResultException;
import com.group2.rms.interview.exception.InterviewStatusException;
import com.group2.rms.interview.repository.InterviewEvaluationRepository;
import com.group2.rms.interview.repository.InterviewFinalResultRepository;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Chốt kết quả phỏng vấn:
 * - Người chốt: Hiring Manager quản lý phòng ban của yêu cầu tuyển dụng đối với vị trí thông thường;
 *   hoặc Giám đốc (Director) khi tuyển vị trí Hiring Manager (Phương án 2: yêu cầu do Giám đốc tạo).
 * - Mỗi lịch một kết quả (UQ_InterviewFinalResult_Interview). Passed/Failed là cuối cùng;
 *   Considering được chốt lại sau, giữ nguyên ConsideredAt.
 * - Trong 24h sau giờ kết thúc phải đủ phiếu của hội đồng; sau 24h chốt tự do. Các hạn khác chỉ cảnh báo.
 * - Trạng thái Application do module candidate đổi qua ApplicationStatusService (dunglt bổ sung):
 *   Failed → Rejected; Passed / Considering → giữ Interviewing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewFinalResultServiceImpl implements InterviewFinalResultService {

    private final InterviewScheduleRepository interviewScheduleRepository;
    private final InterviewFinalResultRepository interviewFinalResultRepository;
    private final RequisitionAccess requisitionAccess;
    // dunglt bổ sung: đổi trạng thái hồ sơ theo kết quả (module candidate, chiều interview → candidate)
    private final ApplicationStatusService applicationStatusService;
    private final InterviewEvaluationRepository interviewEvaluationRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void decide(Long interviewId, SubmitFinalResultRequest request, User hiringManager) {
        InterviewSchedule schedule = interviewScheduleRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch phỏng vấn với mã: " + interviewId));

        if (!isDecisionMaker(hiringManager, schedule)) {
            if (requiresDirectorDecision(schedule)) {
                throw new AccessDeniedException("Chỉ Giám đốc mới được chốt kết quả phỏng vấn cho vị trí Hiring Manager.");
            }
            throw new AccessDeniedException("Chỉ Hiring Manager phụ trách phòng ban của vị trí này mới được chốt kết quả.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        String decision = request.finalDecision();
        InterviewFinalResult existing = interviewFinalResultRepository.findByScheduleId(interviewId).orElse(null);
        if (existing != null && !existing.isConsidering()) {
            throw new InterviewFinalResultException("Buổi phỏng vấn này đã được chốt kết quả, không thể chốt lại.");
        }

        if (existing == null) {
            String blockedReason = InterviewFinalResultService.blockedReason(schedule,
                    submittedPanelCount(schedule), schedule.getInterviewPanels().size(), now);
            if (blockedReason != null) {
                throw new InterviewFinalResultException(blockedReason);
            }
            interviewFinalResultRepository.save(InterviewFinalResult.builder()
                    .interviewSchedule(schedule)
                    .hiringManager(hiringManager)
                    .finalDecision(decision)
                    .recommendedSalary(request.recommendedSalary())
                    .finalSummaryComments(blankToNull(request.finalSummaryComments()))
                    .approvedAt(now)
                    .consideredAt(InterviewFinalResult.CONSIDERING.equals(decision) ? now : null)
                    .build());
        } else {
            // Chốt lại từ Considering: giữ ConsideredAt để biết đã cân nhắc bao lâu
            existing.setHiringManager(hiringManager);
            existing.setFinalDecision(decision);
            existing.setRecommendedSalary(request.recommendedSalary());
            existing.setFinalSummaryComments(blankToNull(request.finalSummaryComments()));
            existing.setApprovedAt(now);
            interviewFinalResultRepository.save(existing);
        }

        InterviewStatus current = schedule.getInterviewStatus();
        if (current == InterviewStatus.Scheduled || current == InterviewStatus.Rescheduled) {
            if (!current.canTransitionTo(InterviewStatus.Completed)) {
                throw new InterviewStatusException(current, InterviewStatus.Completed,
                        "Business Rule GBR-01: Không thể chuyển lịch phỏng vấn sang trạng thái Đã hoàn thành.");
            }
            schedule.transitionTo(InterviewStatus.Completed);
            interviewScheduleRepository.save(schedule);
        }

        // dunglt bổ sung: chốt Failed thì hồ sơ → Rejected; Passed giữ Interviewing để HR lập offer.
        // Considering chưa phải quyết định cuối: chỉ đưa hồ sơ cũ (HM_Passed) sang Interviewing.
        Integer applicationId = schedule.getApplication().getApplicationId();
        if (InterviewFinalResult.CONSIDERING.equals(decision)) {
            applicationStatusService.onInterviewScheduled(applicationId);
        } else {
            applicationStatusService.onInterviewDecided(applicationId, InterviewFinalResult.PASSED.equals(decision));
        }

        log.info("Người dùng #{} chốt kết quả {} cho lịch phỏng vấn #{}.",
                hiringManager.getUserId(), request.finalDecision(), interviewId);
    }

    @Override
    public boolean requiresDirectorDecision(InterviewSchedule schedule) {
        if (schedule == null) {
            return false;
        }
        Application application = schedule.getApplication();
        if (application == null || application.getJobPosting() == null) {
            return false;
        }
        JobRequisition requisition = application.getJobPosting().getRequisition();
        if (requisition == null || requisition.getCreatedBy() == null) {
            return false;
        }
        return CurrentUserService.hasRole(requisition.getCreatedBy(), RequisitionAccess.ROLE_DIRECTOR);
    }

    @Override
    public boolean isDecisionMaker(User user, InterviewSchedule schedule) {
        if (user == null || schedule == null) {
            return false;
        }
        if (requiresDirectorDecision(schedule)) {
            return CurrentUserService.hasRole(user, RequisitionAccess.ROLE_DIRECTOR);
        }
        if (!CurrentUserService.hasRole(user, RequisitionAccess.ROLE_HIRING_MANAGER)) {
            return false;
        }
        Application application = schedule.getApplication();
        if (application == null || application.getJobPosting() == null) {
            return false;
        }
        return requisitionAccess.inManagedDepartment(user, application.getJobPosting().getRequisition());
    }

    /** Số thành viên hội đồng đã nộp phiếu (phiếu của người ngoài hội đồng không tính). */
    private int submittedPanelCount(InterviewSchedule schedule) {
        Set<Integer> panelIds = schedule.getInterviewPanels().stream()
                .filter(panel -> panel.getInterviewer() != null)
                .map(panel -> panel.getInterviewer().getUserId())
                .collect(Collectors.toSet());
        return (int) interviewEvaluationRepository.findByScheduleIdWithInterviewer(schedule.getInterviewId()).stream()
                .map(evaluation -> evaluation.getInterviewer().getUserId())
                .filter(panelIds::contains)
                .distinct()
                .count();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
