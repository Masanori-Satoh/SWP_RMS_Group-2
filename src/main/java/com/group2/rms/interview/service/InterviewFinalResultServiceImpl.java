package com.group2.rms.interview.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.service.ApplicationStatusService;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.interview.dto.SubmitFinalResultRequest;
import com.group2.rms.interview.entity.InterviewFinalResult;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.interview.exception.InterviewFinalResultException;
import com.group2.rms.interview.exception.InterviewStatusException;
import com.group2.rms.interview.repository.InterviewFinalResultRepository;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.User;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.notification.constants.NotificationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Chốt kết quả phỏng vấn:
 * - Chỉ Hiring Manager quản lý phòng ban của yêu cầu tuyển dụng (cùng quy tắc bước duyệt hồ sơ).
 * - Mỗi lịch một kết quả (UQ_InterviewFinalResult_Interview), chốt xong không sửa.
 * - Không cần đủ phiếu đánh giá; phiếu nộp muộn vẫn được nhận.
 * - Trạng thái Application do module candidate đổi qua ApplicationStatusService (dunglt bổ sung):
 *   Failed → Rejected; Passed → giữ Interviewing để HR lập offer.
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
    private final NotificationService notifications;

    @Override
    @Transactional
    public void decide(Long interviewId, SubmitFinalResultRequest request, User hiringManager) {
        InterviewSchedule schedule = interviewScheduleRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch phỏng vấn với mã: " + interviewId));

        if (!isDecisionMaker(hiringManager, schedule)) {
            throw new AccessDeniedException("Chỉ Hiring Manager phụ trách phòng ban của vị trí này mới được chốt kết quả.");
        }

        String blockedReason = InterviewFinalResultService.blockedReason(schedule, LocalDateTime.now());
        if (blockedReason != null) {
            throw new InterviewFinalResultException(blockedReason);
        }

        if (interviewFinalResultRepository.findByScheduleId(interviewId).isPresent()) {
            throw new InterviewFinalResultException("Buổi phỏng vấn này đã được chốt kết quả, không thể chốt lại.");
        }

        interviewFinalResultRepository.save(InterviewFinalResult.builder()
                .interviewSchedule(schedule)
                .hiringManager(hiringManager)
                .finalDecision(request.finalDecision())
                .recommendedSalary(request.recommendedSalary())
                .finalSummaryComments(blankToNull(request.finalSummaryComments()))
                .build());

        InterviewStatus current = schedule.getInterviewStatus();
        if (current == InterviewStatus.Scheduled || current == InterviewStatus.Rescheduled) {
            if (!current.canTransitionTo(InterviewStatus.Completed)) {
                throw new InterviewStatusException(current, InterviewStatus.Completed,
                        "Business Rule GBR-01: Không thể chuyển lịch phỏng vấn sang trạng thái Đã hoàn thành.");
            }
            schedule.transitionTo(InterviewStatus.Completed);
            interviewScheduleRepository.save(schedule);
        }

        // dunglt bổ sung: chốt Failed thì hồ sơ → Rejected; Passed giữ Interviewing để HR lập offer
        applicationStatusService.onInterviewDecided(schedule.getApplication().getApplicationId(),
                "Passed".equals(request.finalDecision()));
        var posting = schedule.getApplication().getJobPosting();
        if (posting != null) {
            notifications.publishToResponsibleHr(posting, NotificationEventType.INTERVIEW_RESULT_UPDATED,
                    "InterviewSchedule", interviewId, "/interviews",
                    "INTERVIEW_RESULT_" + interviewId);
        }

        log.info("Hiring Manager #{} chốt kết quả {} cho lịch phỏng vấn #{}.",
                hiringManager.getUserId(), request.finalDecision(), interviewId);
    }

    @Override
    public boolean isDecisionMaker(User user, InterviewSchedule schedule) {
        if (user == null || !CurrentUserService.hasRole(user, RequisitionAccess.ROLE_HIRING_MANAGER)) {
            return false;
        }
        Application application = schedule.getApplication();
        if (application == null || application.getJobPosting() == null) {
            return false;
        }
        return requisitionAccess.inManagedDepartment(user, application.getJobPosting().getRequisition());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
