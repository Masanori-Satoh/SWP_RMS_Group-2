package com.group2.rms.interview.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.candidate.service.ApplicationStatusService;
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
import com.group2.rms.notification.service.NotificationService;
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
 * Chốt kết quả phỏng vấn theo quyền HM phụ trách phòng ban hoặc Director khi tuyển HM.
 * Mỗi lịch có một kết quả: Considering được chốt lại, Passed/Failed là cuối cùng.
 * Trong cửa sổ 24h sau EndTime cần đủ phiếu; hết cửa sổ vẫn được chốt khi thiếu phiếu.
 *
 * Lịch Completed, quyết định Passed/Failed/Considering và trạng thái hồ sơ là ba khái niệm riêng.
 * Passed/Considering giữ hồ sơ ở Interviewing; Failed chuyển hồ sơ còn trong vòng phỏng vấn sang Rejected.
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
    // hoangnh bổ sung: thông báo in-app cho HR
    private final NotificationService notificationService;
    private final Clock clock;

    /**
     * Lưu quyết định, hoàn thành lịch và đồng bộ trạng thái hồ sơ trong cùng transaction.
     * Chốt lại Considering cập nhật kết quả hiện có, không tạo thêm bản ghi.
     */
    @Override
    @Transactional
    public void decide(Long interviewId, SubmitFinalResultRequest request, User hiringManager) {
        /*
         * Xác định người chốt qua requisition/phòng ban của hồ sơ,
         * không dựa vào người tạo lịch hay người có mặt trong hội đồng.
         */
        InterviewSchedule schedule = interviewScheduleRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch phỏng vấn với mã: " + interviewId));

        /*
         * Vị trí tuyển HM cần Director chốt; vị trí thông thường cần HM quản lý đúng phòng ban.
         */
        if (!isDecisionMaker(hiringManager, schedule)) {
            if (requiresDirectorDecision(schedule)) {
                throw new AccessDeniedException("Chỉ Giám đốc mới được chốt kết quả phỏng vấn cho vị trí Hiring Manager.");
            }
            throw new AccessDeniedException("Chỉ Hiring Manager phụ trách phòng ban của vị trí này mới được chốt kết quả.");
        }

        /*
         * Passed/Failed không được ghi đè; chỉ Considering được sửa.
         * Dùng chung một mốc now cho lần quyết định này.
         */
        LocalDateTime now = LocalDateTime.now(clock);
        String decision = request.finalDecision();
        InterviewFinalResult existing = interviewFinalResultRepository.findByScheduleId(interviewId).orElse(null);
        if (existing != null && !existing.isConsidering()) {
            throw new InterviewFinalResultException("Buổi phỏng vấn này đã được chốt kết quả, không thể chốt lại.");
        }

        if (existing == null) {
            /*
             * Lần chốt đầu kiểm tra lịch đã diễn ra, chưa hủy và đủ phiếu trong cửa sổ 24h.
             * Chỉ phiếu của hội đồng hiện tại được tính vào điều kiện đủ phiếu.
             */
            String blockedReason = InterviewFinalResultService.blockedReason(schedule,
                    submittedPanelCount(schedule), schedule.getInterviewPanels().size(), now);
            if (blockedReason != null) {
                throw new InterviewFinalResultException(blockedReason);
            }
            /*
             * Chỉ ghi ConsideredAt khi bắt đầu cân nhắc; chốt thẳng Passed/Failed để mốc này null.
             */
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
            /*
             * Chốt lại Considering không xét đủ phiếu lần nữa. Giữ ConsideredAt ban đầu
             * để việc lưu lại không kéo dài thêm hạn cân nhắc 7 ngày.
             */
            existing.setHiringManager(hiringManager);
            existing.setFinalDecision(decision);
            existing.setRecommendedSalary(request.recommendedSalary());
            existing.setFinalSummaryComments(blankToNull(request.finalSummaryComments()));
            existing.setApprovedAt(now);
            interviewFinalResultRepository.save(existing);
        }

        /*
         * Đã có quyết định thì hoàn thành lịch đang hoạt động, kể cả Considering.
         * Quyết định còn tạm không có nghĩa buổi phỏng vấn vẫn đang diễn ra.
         */
        InterviewStatus current = schedule.getInterviewStatus();
        if (current == InterviewStatus.Scheduled || current == InterviewStatus.Rescheduled) {
            if (!current.canTransitionTo(InterviewStatus.Completed)) {
                throw new InterviewStatusException(current, InterviewStatus.Completed,
                        "Business Rule GBR-01: Không thể chuyển lịch phỏng vấn sang trạng thái Đã hoàn thành.");
            }
            schedule.transitionTo(InterviewStatus.Completed);
            interviewScheduleRepository.save(schedule);
        }

        /*
         * Đồng bộ qua ApplicationStatusService: Considering/Passed giữ Interviewing để tiếp tục xử lý;
         * Failed chuyển sang Rejected. Module candidate kiểm soát trạng thái nguồn để không kéo lùi hồ sơ.
         */
        Integer applicationId = schedule.getApplication().getApplicationId();
        if (InterviewFinalResult.CONSIDERING.equals(decision)) {
            applicationStatusService.onInterviewScheduled(applicationId);
        } else {
            applicationStatusService.onInterviewDecided(applicationId, InterviewFinalResult.PASSED.equals(decision));
        }
        /*
         * Báo HR kết quả sau khi đồng bộ hồ sơ để tiếp tục lập offer hoặc đóng hồ sơ.
         */
        notificationService.notifyInterviewResultDecided(schedule, hiringManager, decision);

        log.info("Người dùng #{} chốt kết quả {} cho lịch phỏng vấn #{}.",
                hiringManager.getUserId(), request.finalDecision(), interviewId);
    }

    @Override
    public boolean requiresDirectorDecision(InterviewSchedule schedule) {
        /*
         * Phương án tuyển HM nhận diện bằng người tạo requisition có vai trò Director,
         * không dựa trên tên vị trí tuyển dụng.
         */
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
        /*
         * Dùng chung cho quyền hiện nút chốt và thao tác chốt.
         * Thành viên hội đồng không mặc nhiên có quyền chốt; HM còn phải phụ trách đúng phòng ban.
         */
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
        /*
         * Loại phiếu của người đã bị gỡ khỏi hội đồng; mỗi người chỉ đóng góp một lượt đếm.
         * Tránh dùng phiếu của hội đồng cũ để mở quyền chốt sớm.
         */
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
