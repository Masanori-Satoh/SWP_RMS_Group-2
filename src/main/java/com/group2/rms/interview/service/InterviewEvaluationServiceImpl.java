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
 * Nộp phiếu đánh giá phỏng vấn.
 * - D1': chỉ thành viên InterviewPanel của lịch được nộp, mỗi người một phiếu.
 * - D2': nộp được khi đã tới giờ bắt đầu và lịch chưa bị hủy.
 * - D3': phiếu đầu tiên chuyển lịch Scheduled/Rescheduled sang Completed (GBR-01 chỉ đi tiến).
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
        InterviewSchedule schedule = interviewScheduleRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch phỏng vấn với mã: " + interviewId));

        // GBR-05: chỉ thành viên hội đồng của lịch này được đánh giá
        InterviewPanel membership = schedule.getInterviewPanels().stream()
                .filter(panel -> panel.getInterviewer() != null
                        && evaluatorId != null
                        && evaluatorId.equals(panel.getInterviewer().getUserId()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException(
                        "Bạn không thuộc hội đồng của buổi phỏng vấn này nên không thể đánh giá."));

        String blockedReason = InterviewEvaluationService.blockedReason(schedule, LocalDateTime.now());
        if (blockedReason != null) {
            throw new InterviewEvaluationException(blockedReason);
        }

        if (interviewEvaluationRepository.existsByScheduleIdAndInterviewerId(interviewId, evaluatorId)) {
            throw new InterviewEvaluationException("Bạn đã nộp phiếu đánh giá cho buổi phỏng vấn này.");
        }

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

        // D3': phiếu đầu tiên coi như buổi phỏng vấn đã diễn ra
        InterviewStatus current = schedule.getInterviewStatus();
        if (current == InterviewStatus.Scheduled || current == InterviewStatus.Rescheduled) {
            if (!current.canTransitionTo(InterviewStatus.Completed)) {
                throw new InterviewStatusException(current, InterviewStatus.Completed,
                        "Business Rule GBR-01: Không thể chuyển lịch phỏng vấn sang trạng thái Đã hoàn thành.");
            }
            schedule.transitionTo(InterviewStatus.Completed);
            interviewScheduleRepository.save(schedule);
            log.info("Lịch phỏng vấn #{} chuyển sang Completed sau phiếu đánh giá đầu tiên.", interviewId);
        }

        log.info("Người dùng #{} đã nộp phiếu đánh giá cho lịch phỏng vấn #{}.", evaluatorId, interviewId);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
