package com.group2.rms.interview.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.interview.dto.SubmitEvaluationRequest;
import com.group2.rms.interview.entity.InterviewEvaluation;
import com.group2.rms.interview.entity.InterviewPanel;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.interview.exception.InterviewEvaluationException;
import com.group2.rms.interview.exception.InterviewStatusException;
import com.group2.rms.interview.repository.InterviewEvaluationRepository;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Nộp phiếu đánh giá của thành viên hội đồng, mỗi người một phiếu cho một lịch.
 * Chỉ nộp từ giờ bắt đầu, lịch hủy không nhận phiếu; quá hạn vẫn nhận và đánh dấu nộp trễ.
 * Lịch hoàn thành khi đủ phiếu hoặc đã đến EndTime. Nộp phiếu không trực tiếp chốt kết quả hay loại hồ sơ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewEvaluationServiceImpl implements InterviewEvaluationService {

    private final InterviewScheduleRepository interviewScheduleRepository;
    private final InterviewEvaluationRepository interviewEvaluationRepository;

    @Override
    @Transactional
    public void submit(Long interviewId, SubmitEvaluationRequest request, Integer evaluatorId) {
        /*
         * Lưu phiếu và hoàn thành lịch cùng transaction để tránh cập nhật dở dang.
         */
        InterviewSchedule schedule = interviewScheduleRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch phỏng vấn với mã: " + interviewId));

        /*
         * Chỉ thành viên của chính lịch này được nộp, dù người dùng có vai trò Interviewer/HR/HM.
         */
        InterviewPanel membership = schedule.getInterviewPanels().stream()
                .filter(panel -> panel.getInterviewer() != null
                        && evaluatorId != null
                        && evaluatorId.equals(panel.getInterviewer().getUserId()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException(
                        "Bạn không thuộc hội đồng của buổi phỏng vấn này nên không thể đánh giá."));

        /*
         * Completed vẫn nhận phiếu bổ sung; hạn EndTime + 24h chỉ dùng đánh dấu trễ, không khóa nộp.
         */
        String blockedReason = InterviewEvaluationService.blockedReason(schedule, LocalDateTime.now());
        if (blockedReason != null) {
            throw new InterviewEvaluationException(blockedReason);
        }

        /*
         * Không nhận lại phiếu đã nộp cho cùng lịch và cùng người.
         */
        if (interviewEvaluationRepository.existsByScheduleIdAndInterviewerId(interviewId, evaluatorId)) {
            throw new InterviewEvaluationException("Bạn đã nộp phiếu đánh giá cho buổi phỏng vấn này.");
        }

        /*
         * Mỗi thành viên chỉ chấm mục thuộc chuyên môn, nhưng phải có ít nhất một điểm.
         * Nhận xét trống được chuẩn hóa thành null.
         */
        InterviewEvaluation evaluation = InterviewEvaluation.builder()
                .interviewSchedule(schedule)
                .interviewer(membership.getInterviewer())
                .technicalScore(request.technicalScore())
                .softSkillScore(request.softSkillScore())
                .culturalFitScore(request.culturalFitScore())
                .recommendation(request.recommendation())
                .strengths(blankToNull(request.strengths()))
                .weaknesses(blankToNull(request.weaknesses()))
                .comments(blankToNull(request.comments()))
                .build();
        interviewEvaluationRepository.save(evaluation);

        /*
         * Chỉ hoàn thành lịch hoạt động khi đủ phiếu hội đồng hiện tại hoặc đã tới EndTime.
         * Phiếu đầu tiên không tự làm buổi phỏng vấn hoàn thành.
         */
        InterviewStatus current = schedule.getInterviewStatus();
        if (current == InterviewStatus.Scheduled || current == InterviewStatus.Rescheduled) {
            java.util.Set<Integer> panelIds = schedule.getInterviewPanels().stream()
                    .filter(panel -> panel.getInterviewer() != null)
                    .map(panel -> panel.getInterviewer().getUserId())
                    .collect(java.util.stream.Collectors.toSet());
            /*
             * Gom người đã nộp vào Set rồi thêm người vừa nộp để không đếm trùng khi query đã thấy phiếu mới.
             * Phiếu của người không còn trong hội đồng không tham gia điều kiện đủ phiếu.
             */
            java.util.Set<Integer> submittedIds = interviewEvaluationRepository.findByScheduleIdWithInterviewer(interviewId).stream()
                    .map(e -> e.getInterviewer().getUserId())
                    .filter(panelIds::contains)
                    .collect(java.util.stream.Collectors.toSet());
            if (panelIds.contains(evaluatorId)) {
                submittedIds.add(evaluatorId);
            }
            boolean allSubmitted = !panelIds.isEmpty() && submittedIds.containsAll(panelIds);
            boolean pastEnd = schedule.getEndTime() != null && !LocalDateTime.now().isBefore(schedule.getEndTime());
            if (allSubmitted || pastEnd) {
                if (!current.canTransitionTo(InterviewStatus.Completed)) {
                    throw new InterviewStatusException(current, InterviewStatus.Completed,
                            "Business Rule GBR-01: Không thể chuyển lịch phỏng vấn sang trạng thái Đã hoàn thành.");
                }
                schedule.transitionTo(InterviewStatus.Completed);
                interviewScheduleRepository.save(schedule);
                log.info("Lịch phỏng vấn #{} chuyển sang Completed ({}).", interviewId,
                        allSubmitted ? "đủ phiếu hội đồng" : "đã qua giờ kết thúc");
            }
        }

        log.info("Người dùng #{} đã nộp phiếu đánh giá cho lịch phỏng vấn #{}.", evaluatorId, interviewId);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
