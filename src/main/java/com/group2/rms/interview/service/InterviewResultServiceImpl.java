package com.group2.rms.interview.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.interview.dto.InterviewEvaluationResponse;
import com.group2.rms.interview.dto.InterviewResultDetailResponse;
import com.group2.rms.interview.dto.InterviewResultDetailResponse.Averages;
import com.group2.rms.interview.dto.InterviewResultDetailResponse.FinalResult;
import com.group2.rms.interview.dto.InterviewResultDetailResponse.PanelMemberStatus;
import com.group2.rms.interview.dto.InterviewResultDetailResponse.RecommendationCount;
import com.group2.rms.interview.entity.EvaluationRecommendation;
import com.group2.rms.interview.entity.InterviewEvaluation;
import com.group2.rms.interview.entity.InterviewFinalResult;
import com.group2.rms.interview.entity.InterviewPanel;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.interview.repository.InterviewEvaluationRepository;
import com.group2.rms.interview.repository.InterviewFinalResultRepository;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class InterviewResultServiceImpl implements InterviewResultService {

    /** Vai trò được xem mọi phiếu đánh giá (D5). */
    private static final Set<String> FULL_ACCESS_ROLES = Set.of("HR", "Hiring Manager", "Director", "System Admin");

    private final InterviewScheduleRepository interviewScheduleRepository;
    private final InterviewEvaluationRepository interviewEvaluationRepository;
    private final InterviewFinalResultRepository interviewFinalResultRepository;
    private final InterviewFinalResultService interviewFinalResultService;

    @Override
    @Transactional(readOnly = true)
    public InterviewResultDetailResponse getResult(Long interviewId, User viewer) {
        Integer viewerId = viewer != null ? viewer.getUserId() : null;
        String viewerRoleName = viewer != null && viewer.getRole() != null ? viewer.getRole().getRoleName() : null;
        InterviewSchedule schedule = interviewScheduleRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch phỏng vấn với mã: " + interviewId));

        boolean panelMember = schedule.getInterviewPanels().stream()
                .anyMatch(panel -> isUser(panel.getInterviewer(), viewerId));
        boolean fullAccess = viewerRoleName != null && FULL_ACCESS_ROLES.contains(viewerRoleName);
        if (!fullAccess && !panelMember) {
            // GBR-05: Interviewer chỉ được xem lịch mình được phân công
            throw new AccessDeniedException("Bạn không được phân công vào buổi phỏng vấn này.");
        }

        List<InterviewEvaluation> allEvaluations = interviewEvaluationRepository.findByScheduleIdWithInterviewer(interviewId);
        FinalResult finalResult = interviewFinalResultRepository.findByScheduleId(interviewId)
                .map(InterviewResultServiceImpl::toFinalResult)
                .orElse(null);
        boolean canSeeAll = fullAccess || finalResult != null;

        Set<Integer> submittedIds = allEvaluations.stream()
                .map(evaluation -> evaluation.getInterviewer().getUserId())
                .collect(Collectors.toSet());
        Map<Integer, String> roleByInterviewer = schedule.getInterviewPanels().stream()
                .filter(panel -> panel.getInterviewer() != null)
                .collect(Collectors.toMap(panel -> panel.getInterviewer().getUserId(),
                        panel -> panel.getRoleInPanel() != null ? panel.getRoleInPanel().getDisplayName() : "",
                        (first, second) -> first));

        List<PanelMemberStatus> panel = schedule.getInterviewPanels().stream()
                .filter(member -> member.getInterviewer() != null)
                .sorted(Comparator.comparing((InterviewPanel member) -> member.getInterviewer().getFullName(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(member -> new PanelMemberStatus(
                        member.getInterviewer().getUserId(),
                        member.getInterviewer().getFullName(),
                        member.getRoleInPanel() != null ? member.getRoleInPanel().getDisplayName() : "",
                        submittedIds.contains(member.getInterviewer().getUserId()),
                        isUser(member.getInterviewer(), viewerId)))
                .toList();

        List<InterviewEvaluation> visibleEvaluations = canSeeAll
                ? allEvaluations
                : allEvaluations.stream().filter(evaluation -> isUser(evaluation.getInterviewer(), viewerId)).toList();
        List<InterviewEvaluationResponse> evaluations = visibleEvaluations.stream()
                .map(evaluation -> toResponse(evaluation, roleByInterviewer, viewerId))
                .toList();

        String blockedReason = null;
        boolean canEvaluate = false;
        if (panelMember && !submittedIds.contains(viewerId)) {
            blockedReason = InterviewEvaluationService.blockedReason(schedule, LocalDateTime.now());
            canEvaluate = blockedReason == null;
        }

        String finalizeBlockedReason = null;
        boolean canFinalize = false;
        if (finalResult == null && interviewFinalResultService.isDecisionMaker(viewer, schedule)) {
            finalizeBlockedReason = InterviewFinalResultService.blockedReason(schedule, LocalDateTime.now());
            canFinalize = finalizeBlockedReason == null;
        }

        Application application = schedule.getApplication();
        InterviewStatus status = schedule.getInterviewStatus();
        return new InterviewResultDetailResponse(
                schedule.getInterviewId(),
                application != null ? application.getApplicationId() : null,
                candidateUser(application) != null ? candidateUser(application).getFullName() : null,
                candidateUser(application) != null ? candidateUser(application).getEmail() : null,
                application != null && application.getJobPosting() != null ? application.getJobPosting().getPostingTitle() : null,
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getInterviewFormat() != null ? schedule.getInterviewFormat().getDisplayName() : null,
                schedule.getLocationOrLink(),
                status != null ? status.getDisplayName() : null,
                statusBadge(status),
                panel,
                submittedIds.size(),
                evaluations,
                canSeeAll ? averages(allEvaluations) : null,
                canSeeAll ? recommendationCounts(allEvaluations) : List.of(),
                finalResult,
                canSeeAll,
                canEvaluate,
                blockedReason,
                canFinalize,
                finalizeBlockedReason
        );
    }

    private static InterviewEvaluationResponse toResponse(InterviewEvaluation evaluation,
                                                          Map<Integer, String> roleByInterviewer,
                                                          Integer viewerId) {
        User interviewer = evaluation.getInterviewer();
        EvaluationRecommendation recommendation = EvaluationRecommendation.fromValue(evaluation.getRecommendation());
        return new InterviewEvaluationResponse(
                evaluation.getEvaluationId(),
                interviewer.getUserId(),
                interviewer.getFullName(),
                roleByInterviewer.getOrDefault(interviewer.getUserId(), ""),
                evaluation.getTechnicalScore(),
                evaluation.getSoftSkillScore(),
                evaluation.getCulturalFitScore(),
                average(Stream.of(evaluation.getTechnicalScore(), evaluation.getSoftSkillScore(),
                        evaluation.getCulturalFitScore())),
                evaluation.getRecommendation(),
                recommendation != null ? recommendation.getDisplayName() : evaluation.getRecommendation(),
                recommendation != null ? recommendation.getBadgeClass() : "badge--neutral",
                toHtml(evaluation.getStrengths()),
                toHtml(evaluation.getWeaknesses()),
                toHtml(evaluation.getComments()),
                evaluation.getEvaluatedAt(),
                isUser(interviewer, viewerId));
    }

    private static Averages averages(List<InterviewEvaluation> evaluations) {
        if (evaluations.isEmpty()) {
            return null;
        }
        return new Averages(
                average(evaluations.stream().map(InterviewEvaluation::getTechnicalScore)),
                average(evaluations.stream().map(InterviewEvaluation::getSoftSkillScore)),
                average(evaluations.stream().map(InterviewEvaluation::getCulturalFitScore)),
                average(evaluations.stream().flatMap(evaluation -> Stream.of(evaluation.getTechnicalScore(),
                        evaluation.getSoftSkillScore(), evaluation.getCulturalFitScore()))));
    }

    private static List<RecommendationCount> recommendationCounts(List<InterviewEvaluation> evaluations) {
        Map<String, Long> counts = evaluations.stream()
                .map(InterviewEvaluation::getRecommendation)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return Arrays.stream(EvaluationRecommendation.values())
                .map(recommendation -> new RecommendationCount(
                        recommendation.name(),
                        recommendation.getDisplayName(),
                        recommendation.getBadgeClass(),
                        counts.getOrDefault(recommendation.name(), 0L)))
                .toList();
    }

    private static FinalResult toFinalResult(InterviewFinalResult result) {
        boolean passed = "Passed".equalsIgnoreCase(result.getFinalDecision());
        return new FinalResult(
                result.getFinalDecision(),
                passed ? "Đạt" : "Không đạt",
                passed ? "badge--success" : "badge--danger",
                result.getHiringManager() != null ? result.getHiringManager().getFullName() : null,
                result.getRecommendedSalary(),
                toHtml(result.getFinalSummaryComments()),
                result.getApprovedAt());
    }

    private static Double average(Stream<Integer> scores) {
        return scores.filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .stream()
                .mapToObj(value -> Math.round(value * 10) / 10.0)
                .findFirst()
                .orElse(null);
    }

    private static String statusBadge(InterviewStatus status) {
        if (status == null) {
            return "badge--neutral";
        }
        return switch (status) {
            case Completed -> "badge--success";
            case Cancelled -> "badge--danger";
            case Scheduled, Rescheduled -> "badge--warning";
        };
    }

    /** Escape HTML rồi đổi xuống dòng thành &lt;br/&gt; để render bằng th:utext an toàn. */
    private static String toHtml(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return HtmlUtils.htmlEscape(text).replace("\r\n", "<br/>").replace("\n", "<br/>");
    }

    private static User candidateUser(Application application) {
        return application != null && application.getCandidate() != null ? application.getCandidate().getAccount() : null;
    }

    private static boolean isUser(User user, Integer userId) {
        return user != null && userId != null && userId.equals(user.getUserId());
    }
}
