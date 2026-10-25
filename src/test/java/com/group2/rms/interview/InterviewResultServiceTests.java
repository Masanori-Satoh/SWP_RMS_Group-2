package com.group2.rms.interview;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.interview.dto.InterviewCardState;
import com.group2.rms.interview.dto.InterviewResultDetailResponse;
import com.group2.rms.interview.dto.InterviewScheduleResponse;
import com.group2.rms.interview.dto.PanelMemberResponse;
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

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
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
                interviewFinalResultRepository, interviewFinalResultService, java.time.Clock.systemDefaultZone());

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

    // ===== Hạn chấm điểm, quá hạn chốt và "Cân nhắc" (đồng hồ cố định) =====

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 23, 10, 0);

    private InterviewResultServiceImpl serviceAtNow() {
        Clock fixed = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        return new InterviewResultServiceImpl(interviewScheduleRepository, interviewEvaluationRepository,
                interviewFinalResultRepository, interviewFinalResultService, fixed);
    }

    private void endedAt(LocalDateTime end) {
        schedule.setInterviewStatus(InterviewStatus.Completed);
        schedule.setStartTime(end.minusHours(1));
        schedule.setEndTime(end);
    }

    private InterviewEvaluation submittedAt(User by, LocalDateTime at) {
        InterviewEvaluation evaluation = evaluation(by.getUserId(), by, 4, 4, 4, "Hire", null);
        evaluation.setEvaluatedAt(at);
        return evaluation;
    }

    @Test
    @DisplayName("Trạng thái chấm điểm từng người: nộp đúng hạn / nộp trễ")
    void panelProgressShowsLate() {
        endedAt(NOW.minusHours(30));
        when(interviewEvaluationRepository.findByScheduleIdWithInterviewer(INTERVIEW_ID)).thenReturn(List.of(
                submittedAt(interviewerA, NOW.minusHours(29)),
                submittedAt(interviewerB, NOW.minusHours(2))));

        InterviewResultDetailResponse result = serviceAtNow().getResult(INTERVIEW_ID, user(1, "Lan HR", "HR"));

        assertEquals("Đã nộp", result.panel().get(0).progressDisplay());
        assertEquals("Nộp trễ", result.panel().get(1).progressDisplay());
        assertFalse(result.evaluations().get(0).late());
        assertTrue(result.evaluations().get(1).late());
        assertEquals(NOW.minusHours(6), result.evaluationDeadline());
        assertEquals(NOW.plusHours(42), result.decisionDeadline());
    }

    @Test
    @DisplayName("Quá 24h chưa nộp: Quá hạn đánh giá nhưng vẫn được nộp")
    void overdueMemberCanStillEvaluate() {
        endedAt(NOW.minusHours(30));
        when(interviewEvaluationRepository.findByScheduleIdWithInterviewer(INTERVIEW_ID)).thenReturn(List.of(
                submittedAt(interviewerA, NOW.minusHours(29))));

        InterviewResultDetailResponse result = serviceAtNow().getResult(INTERVIEW_ID, interviewerB);

        assertEquals("Quá hạn đánh giá", result.panel().stream()
                .filter(InterviewResultDetailResponse.PanelMemberStatus::me).findFirst().orElseThrow().progressDisplay());
        assertTrue(result.canEvaluate());
    }

    @Test
    @DisplayName("HM trong 24h mà thiếu phiếu: không chốt được, có lý do hiển thị")
    void decisionMakerBlockedUntilAllEvaluations() {
        endedAt(NOW.minusHours(2));
        User hm = user(40, "Hoa HM", "Hiring Manager");
        when(interviewFinalResultService.isDecisionMaker(hm, schedule)).thenReturn(true);
        when(interviewEvaluationRepository.findByScheduleIdWithInterviewer(INTERVIEW_ID)).thenReturn(List.of(
                submittedAt(interviewerA, NOW.minusHours(1))));

        InterviewResultDetailResponse result = serviceAtNow().getResult(INTERVIEW_ID, hm);

        assertFalse(result.canFinalize());
        assertTrue(result.finalizeBlockedReason().contains("1/2"));
    }

    @Test
    @DisplayName("Chưa chốt và quá hạn 2 ngày sau hạn chấm điểm: chỉ cảnh báo, vẫn chốt được")
    void overdueDecisionIsOnlyAWarning() {
        endedAt(NOW.minusHours(80));
        User hm = user(40, "Hoa HM", "Hiring Manager");
        when(interviewFinalResultService.isDecisionMaker(hm, schedule)).thenReturn(true);

        InterviewResultDetailResponse result = serviceAtNow().getResult(INTERVIEW_ID, hm);

        assertTrue(result.decisionOverdue());
        assertTrue(result.canFinalize());
    }

    @Test
    @DisplayName("Đang cân nhắc: HM chốt lại được; quá 7 ngày thì cảnh báo")
    void consideringCanBeRedecidedAndWarnsAfterSevenDays() {
        endedAt(NOW.minusDays(10));
        User hm = user(40, "Hoa HM", "Hiring Manager");
        when(interviewFinalResultService.isDecisionMaker(hm, schedule)).thenReturn(true);
        when(interviewFinalResultRepository.findByScheduleId(INTERVIEW_ID)).thenReturn(Optional.of(
                InterviewFinalResult.builder().finalResultId(3).interviewSchedule(schedule).hiringManager(hm)
                        .finalDecision("Considering").consideredAt(NOW.minusDays(8))
                        .approvedAt(NOW.minusDays(8)).build()));

        InterviewResultDetailResponse result = serviceAtNow().getResult(INTERVIEW_ID, hm);

        assertTrue(result.canFinalize());
        assertNull(result.finalizeBlockedReason());
        assertTrue(result.finalResult().considering());
        assertEquals("Đang cân nhắc", result.finalResult().decisionDisplay());
        assertTrue(result.finalResult().considerOverdue());
        assertFalse(result.decisionOverdue(), "Đã có quyết định (dù tạm) thì không báo quá hạn chốt");
    }

    @Test
    @DisplayName("Nhãn danh sách lịch: chờ chốt, quá hạn chốt, đang cân nhắc; lịch chưa kết thúc không có nhãn")
    void listDecisionStates() {
        InterviewScheduleResponse awaiting = listItem(1L, NOW.minusHours(5));
        InterviewScheduleResponse overdue = listItem(2L, NOW.minusHours(80));
        InterviewScheduleResponse considering = listItem(3L, NOW.minusDays(2));
        InterviewScheduleResponse upcoming = listItem(4L, NOW.plusDays(1));
        InterviewSchedule consideredSchedule = InterviewSchedule.builder().interviewId(3L).build();
        when(interviewFinalResultRepository.findByInterviewSchedule_InterviewIdIn(anyCollection())).thenReturn(List.of(
                InterviewFinalResult.builder().interviewSchedule(consideredSchedule).finalDecision("Considering")
                        .consideredAt(NOW.minusDays(1)).build()));

        Map<Long, InterviewCardState> states = serviceAtNow()
                .decisionStates(List.of(awaiting, overdue, considering, upcoming));

        assertEquals("AWAITING_DECISION", states.get(1L).code());
        assertEquals("DECISION_OVERDUE", states.get(2L).code());
        assertEquals("CONSIDERING", states.get(3L).code());
        assertFalse(states.containsKey(4L));
    }

    @Test
    @DisplayName("Nhãn chấm điểm của người xem: chờ đánh giá kèm giờ còn lại, quá hạn")
    void listEvaluationStatesForViewer() {
        InterviewScheduleResponse pending = listItem(1L, NOW.minusHours(4));
        InterviewScheduleResponse overdue = listItem(2L, NOW.minusHours(30));
        when(interviewEvaluationRepository.findByInterviewer_UserIdAndInterviewSchedule_InterviewIdIn(eq(21), anyCollection()))
                .thenReturn(List.of());

        Map<Long, InterviewCardState> states = serviceAtNow().evaluationStates(List.of(pending, overdue), 21);

        assertEquals("EVALUATION_PENDING", states.get(1L).code());
        assertTrue(states.get(1L).display().contains("còn 20h"));
        assertEquals("EVALUATION_OVERDUE", states.get(2L).code());
        assertTrue(serviceAtNow().evaluationStates(List.of(pending), 99).isEmpty(), "Không thuộc hội đồng thì không có nhãn");
    }

    private static InterviewScheduleResponse listItem(Long id, LocalDateTime end) {
        return new InterviewScheduleResponse(id, 100, "Chi", "chi@example.com", null, "Java Developer",
                InterviewFormat.Offline_Office, "Tại văn phòng",
                end.isAfter(NOW) ? InterviewStatus.Scheduled : InterviewStatus.Completed, "",
                end.minusHours(1), end, null, 1, "Lan HR", end.minusDays(3),
                List.of(new PanelMemberResponse(21, "iv_an", "An Interviewer", null, null,
                        RoleInPanel.Interviewer, "Interviewer")));
    }
}
