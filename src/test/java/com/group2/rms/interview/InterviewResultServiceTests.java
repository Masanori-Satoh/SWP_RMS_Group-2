package com.group2.rms.interview;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.interview.dto.InterviewResultDetailResponse;
import com.group2.rms.interview.entity.InterviewEvaluation;
import com.group2.rms.interview.entity.InterviewFinalResult;
import com.group2.rms.interview.entity.InterviewFormat;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.interview.entity.RoleInPanel;
import com.group2.rms.interview.repository.InterviewEvaluationRepository;
import com.group2.rms.interview.repository.InterviewFinalResultRepository;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import com.group2.rms.interview.service.InterviewFinalResultService;
import com.group2.rms.interview.service.InterviewResultServiceImpl;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InterviewResultServiceTests {

    private static final Long INTERVIEW_ID = 8L;

    private InterviewScheduleRepository interviewScheduleRepository;
    private InterviewEvaluationRepository interviewEvaluationRepository;
    private InterviewFinalResultRepository interviewFinalResultRepository;
    private InterviewFinalResultService interviewFinalResultService;
    private InterviewResultServiceImpl service;

    private User interviewerA;
    private User interviewerB;
    private InterviewSchedule schedule;

    @BeforeEach
    void setUp() {
        interviewScheduleRepository = mock(InterviewScheduleRepository.class);
        interviewEvaluationRepository = mock(InterviewEvaluationRepository.class);
        interviewFinalResultRepository = mock(InterviewFinalResultRepository.class);
        interviewFinalResultService = mock(InterviewFinalResultService.class);
        service = new InterviewResultServiceImpl(interviewScheduleRepository, interviewEvaluationRepository,
                interviewFinalResultRepository, interviewFinalResultService);

        interviewerA = user(21, "An Interviewer", "Interviewer");
        interviewerB = user(22, "Binh Interviewer", "Interviewer");
        User candidateUser = user(30, "Chi Candidate", "Candidate");

        Application application = Application.builder()
                .applicationId(100)
                .candidate(Candidate.builder().candidateId(1).account(candidateUser).build())
                .jobPosting(JobPosting.builder().jobPostingId(5).postingTitle("Java Developer").build())
                .build();
        schedule = InterviewSchedule.builder()
                .interviewId(INTERVIEW_ID)
                .application(application)
                .interviewFormat(InterviewFormat.Offline_Office)
                .interviewStatus(InterviewStatus.Completed)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().minusDays(1).plusHours(1))
                .build();
        schedule.addPanelMember(interviewerA, RoleInPanel.Interviewer);
        schedule.addPanelMember(interviewerB, RoleInPanel.Interviewer);

        when(interviewScheduleRepository.findByIdWithDetails(INTERVIEW_ID)).thenReturn(Optional.of(schedule));
        when(interviewEvaluationRepository.findByScheduleIdWithInterviewer(INTERVIEW_ID)).thenReturn(List.of(
                evaluation(1, interviewerA, 4, 4, 5, "Hire", "Giỏi <b>Java</b>\nChủ động"),
                evaluation(2, interviewerB, 2, 3, null, "Consider", null)));
        when(interviewFinalResultRepository.findByScheduleId(INTERVIEW_ID)).thenReturn(Optional.empty());
    }

    private static User user(int id, String name, String roleName) {
        return User.builder().userId(id).fullName(name).accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
    }

    private InterviewEvaluation evaluation(int id, User by, Integer tech, Integer soft, Integer culture,
                                           String recommendation, String strengths) {
        return InterviewEvaluation.builder()
                .evaluationId(id).interviewSchedule(schedule).interviewer(by)
                .technicalScore(tech).softSkillScore(soft).culturalFitScore(culture)
                .recommendation(recommendation).strengths(strengths)
                .evaluatedAt(LocalDateTime.now().minusHours(20))
                .build();
    }

    @Test
    @DisplayName("HR thấy mọi phiếu, điểm trung bình và phân bố đề xuất (D5)")
    void hrSeesEverything() {
        InterviewResultDetailResponse result = service.getResult(INTERVIEW_ID, user(1, "Lan HR", "HR"));

        assertTrue(result.canSeeAll());
        assertFalse(result.canEvaluate(), "HR không thuộc hội đồng nên không đánh giá");
        assertEquals(2, result.evaluations().size());
        assertEquals(2, result.submittedCount());
        assertEquals("Chi Candidate", result.candidateName());
        assertEquals("Java Developer", result.jobTitle());
        assertEquals(3.0, result.averages().technical());
        assertEquals(3.5, result.averages().softSkill());
        assertEquals(5.0, result.averages().culturalFit());
        assertEquals(3.6, result.averages().overall());
        assertEquals(1L, result.recommendations().stream()
                .filter(count -> count.value().equals("Hire")).findFirst().orElseThrow().count());
    }

    @Test
    @DisplayName("Text từ DB được escape HTML và đổi xuống dòng thành <br/>")
    void evaluationTextIsEscaped() {
        InterviewResultDetailResponse result = service.getResult(INTERVIEW_ID, user(2, "Director", "Director"));

        assertEquals("Giỏi &lt;b&gt;Java&lt;/b&gt;<br/>Chủ động", result.evaluations().get(0).strengthsHtml());
    }

    @Test
    @DisplayName("Interviewer thuộc hội đồng: chỉ thấy phiếu của mình khi chưa có Final Result (GBR-05)")
    void interviewerSeesOnlyOwnEvaluation() {
        InterviewResultDetailResponse result = service.getResult(INTERVIEW_ID, interviewerA);

        assertFalse(result.canSeeAll());
        assertEquals(1, result.evaluations().size());
        assertTrue(result.evaluations().get(0).mine());
        assertNull(result.averages());
        assertTrue(result.recommendations().isEmpty());
        assertEquals(2, result.panel().size(), "Vẫn thấy ai đã/chưa nộp");
    }

    @Test
    @DisplayName("Interviewer thấy mọi phiếu khi đã có Final Result")
    void interviewerSeesAllAfterFinalResult() {
        when(interviewFinalResultRepository.findByScheduleId(INTERVIEW_ID)).thenReturn(Optional.of(
                InterviewFinalResult.builder().finalResultId(3).interviewSchedule(schedule)
                        .hiringManager(user(40, "Hoa HM", "Hiring Manager"))
                        .finalDecision("Passed").approvedAt(LocalDateTime.now()).build()));

        InterviewResultDetailResponse result = service.getResult(INTERVIEW_ID, interviewerB);

        assertTrue(result.canSeeAll());
        assertEquals(2, result.evaluations().size());
        assertEquals("Đạt", result.finalResult().decisionDisplay());
    }

    @Test
    @DisplayName("Interviewer không thuộc hội đồng: AccessDeniedException")
    void outsiderInterviewerIsDenied() {
        assertThrows(AccessDeniedException.class, () -> service.getResult(INTERVIEW_ID, user(99, "Ngoai Hoi Dong", "Interviewer")));
    }

    @Test
    @DisplayName("Thành viên chưa nộp và đã tới giờ: canEvaluate = true")
    void pendingMemberCanEvaluate() {
        when(interviewEvaluationRepository.findByScheduleIdWithInterviewer(INTERVIEW_ID)).thenReturn(List.of(
                evaluation(1, interviewerA, 4, 4, 5, "Hire", null)));

        InterviewResultDetailResponse result = service.getResult(INTERVIEW_ID, interviewerB);

        assertTrue(result.canEvaluate());
        assertNull(result.evaluateBlockedReason());
        assertTrue(result.evaluations().isEmpty(), "Chưa nộp thì chưa thấy phiếu của người khác");
    }

    @Test
    @DisplayName("Lịch chưa tới giờ: không đánh giá được, có lý do hiển thị")
    void futureScheduleShowsBlockedReason() {
        schedule.setInterviewStatus(InterviewStatus.Scheduled);
        schedule.setStartTime(LocalDateTime.now().plusDays(2));
        schedule.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        when(interviewEvaluationRepository.findByScheduleIdWithInterviewer(INTERVIEW_ID)).thenReturn(List.of());

        InterviewResultDetailResponse result = service.getResult(INTERVIEW_ID, interviewerA);

        assertFalse(result.canEvaluate());
        assertNotNull(result.evaluateBlockedReason());
    }

    @Test
    @DisplayName("Hiring Manager phụ trách phòng ban, chưa chốt: canFinalize = true")
    void decisionMakerCanFinalize() {
        User hm = user(40, "Hoa HM", "Hiring Manager");
        when(interviewFinalResultService.isDecisionMaker(hm, schedule)).thenReturn(true);

        InterviewResultDetailResponse result = service.getResult(INTERVIEW_ID, hm);

        assertTrue(result.canFinalize());
        assertNull(result.finalizeBlockedReason());
        assertTrue(result.canSeeAll());
    }

    @Test
    @DisplayName("Đã có Final Result: không chốt lại được")
    void finalizedScheduleCannotBeFinalizedAgain() {
        User hm = user(40, "Hoa HM", "Hiring Manager");
        when(interviewFinalResultService.isDecisionMaker(hm, schedule)).thenReturn(true);
        when(interviewFinalResultRepository.findByScheduleId(INTERVIEW_ID)).thenReturn(Optional.of(
                InterviewFinalResult.builder().finalResultId(3).interviewSchedule(schedule).hiringManager(hm)
                        .finalDecision("Failed").approvedAt(LocalDateTime.now()).build()));

        InterviewResultDetailResponse result = service.getResult(INTERVIEW_ID, hm);

        assertFalse(result.canFinalize());
        assertEquals("Không đạt", result.finalResult().decisionDisplay());
    }

    @Test
    @DisplayName("HM không phụ trách phòng ban: không thấy nút chốt")
    void otherHiringManagerCannotFinalize() {
        InterviewResultDetailResponse result = service.getResult(INTERVIEW_ID, user(41, "Khac HM", "Hiring Manager"));

        assertFalse(result.canFinalize());
        assertNull(result.finalizeBlockedReason());
    }
}
