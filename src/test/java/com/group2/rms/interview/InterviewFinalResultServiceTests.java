package com.group2.rms.interview;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.service.ApplicationStatusService;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.interview.dto.SubmitFinalResultRequest;
import com.group2.rms.interview.entity.InterviewEvaluation;
import com.group2.rms.interview.entity.InterviewFinalResult;
import com.group2.rms.interview.entity.InterviewFormat;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.interview.entity.RoleInPanel;
import com.group2.rms.interview.exception.InterviewFinalResultException;
import com.group2.rms.interview.repository.InterviewEvaluationRepository;
import com.group2.rms.interview.repository.InterviewFinalResultRepository;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import com.group2.rms.interview.service.InterviewFinalResultServiceImpl;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class InterviewFinalResultServiceTests {

    private static final Long INTERVIEW_ID = 9L;

    private InterviewScheduleRepository interviewScheduleRepository;
    private InterviewFinalResultRepository interviewFinalResultRepository;
    private RequisitionAccess requisitionAccess;
    private ApplicationStatusService applicationStatusService; // dunglt bổ sung
    private InterviewEvaluationRepository interviewEvaluationRepository;
    private InterviewFinalResultServiceImpl service;

    private User hiringManager;
    private JobRequisition requisition;

    @BeforeEach
    void setUp() {
        interviewScheduleRepository = mock(InterviewScheduleRepository.class);
        interviewFinalResultRepository = mock(InterviewFinalResultRepository.class);
        requisitionAccess = mock(RequisitionAccess.class);
        applicationStatusService = mock(ApplicationStatusService.class); // dunglt bổ sung
        interviewEvaluationRepository = mock(InterviewEvaluationRepository.class);
        service = new InterviewFinalResultServiceImpl(
                interviewScheduleRepository, interviewFinalResultRepository, requisitionAccess,
                applicationStatusService, // dunglt bổ sung
                interviewEvaluationRepository, java.time.Clock.systemDefaultZone());

        hiringManager = user(40, "Hoa HM", "Hiring Manager");
        requisition = JobRequisition.builder().build();
        when(requisitionAccess.inManagedDepartment(hiringManager, requisition)).thenReturn(true);
        when(interviewFinalResultRepository.findByScheduleId(INTERVIEW_ID)).thenReturn(Optional.empty());
    }

    private static User user(int id, String name, String roleName) {
        return User.builder().userId(id).fullName(name).accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
    }

    private InterviewSchedule schedule(InterviewStatus status, LocalDateTime start) {
        Application application = Application.builder()
                .applicationId(100)
                .jobPosting(JobPosting.builder().jobPostingId(5).postingTitle("Java Developer").requisition(requisition).build())
                .build();
        InterviewSchedule schedule = InterviewSchedule.builder()
                .interviewId(INTERVIEW_ID)
                .application(application)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .interviewStatus(status)
                .startTime(start)
                .endTime(start.plusHours(1))
                .build();
        when(interviewScheduleRepository.findByIdWithDetails(INTERVIEW_ID)).thenReturn(Optional.of(schedule));
        return schedule;
    }

    private static SubmitFinalResultRequest passed() {
        return new SubmitFinalResultRequest("Passed", new BigDecimal("25000000"), "  Ứng viên phù hợp  ");
    }

    @Test
    @DisplayName("HM phụ trách phòng ban chốt Đạt khi lịch còn Scheduled: lưu kết quả và chuyển Completed")
    void hiringManagerFinalizesScheduledInterview() {
        InterviewSchedule schedule = schedule(InterviewStatus.Scheduled, LocalDateTime.now().minusHours(2));

        service.decide(INTERVIEW_ID, passed(), hiringManager);

        ArgumentCaptor<InterviewFinalResult> saved = ArgumentCaptor.forClass(InterviewFinalResult.class);
        verify(interviewFinalResultRepository).save(saved.capture());
        assertEquals("Passed", saved.getValue().getFinalDecision());
        assertEquals(hiringManager, saved.getValue().getHiringManager());
        assertEquals(new BigDecimal("25000000"), saved.getValue().getRecommendedSalary());
        assertEquals("Ứng viên phù hợp", saved.getValue().getFinalSummaryComments());
        assertEquals(InterviewStatus.Completed, schedule.getInterviewStatus());
        verify(interviewScheduleRepository).save(schedule);
    }

    @Test
    @DisplayName("Lịch đã Completed: chốt được, không chuyển trạng thái lần nữa")
    void completedInterviewKeepsStatus() {
        schedule(InterviewStatus.Completed, LocalDateTime.now().minusDays(1));

        service.decide(INTERVIEW_ID, new SubmitFinalResultRequest("Failed", null, null), hiringManager);

        verify(interviewFinalResultRepository).save(any(InterviewFinalResult.class));
        verify(interviewScheduleRepository, never()).save(any());
    }

    // dunglt bổ sung: chốt xong thì báo module candidate đổi trạng thái hồ sơ (Passed → true, Failed → false)
    @Test
    @DisplayName("Chốt kết quả thì báo đổi trạng thái hồ sơ; chốt bị chặn thì không báo")
    void decisionNotifiesApplicationStatus() {
        schedule(InterviewStatus.Scheduled, LocalDateTime.now().minusHours(2));
        service.decide(INTERVIEW_ID, passed(), hiringManager);
        verify(applicationStatusService).onInterviewDecided(100, true);

        schedule(InterviewStatus.Completed, LocalDateTime.now().minusDays(1));
        service.decide(INTERVIEW_ID, new SubmitFinalResultRequest("Failed", null, null), hiringManager);
        verify(applicationStatusService).onInterviewDecided(100, false);

        schedule(InterviewStatus.Cancelled, LocalDateTime.now().minusDays(1));
        assertThrows(InterviewFinalResultException.class, () -> service.decide(INTERVIEW_ID, passed(), hiringManager));
        verifyNoMoreInteractions(applicationStatusService);
    }

    @Test
    @DisplayName("HM không phụ trách phòng ban: AccessDeniedException")
    void otherHiringManagerIsDenied() {
        schedule(InterviewStatus.Completed, LocalDateTime.now().minusDays(1));

        assertThrows(AccessDeniedException.class,
                () -> service.decide(INTERVIEW_ID, passed(), user(41, "Khac HM", "Hiring Manager")));
        verify(interviewFinalResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Director dù quản lý mọi phòng ban cũng không phải người chốt")
    void directorIsNotDecisionMaker() {
        InterviewSchedule schedule = schedule(InterviewStatus.Completed, LocalDateTime.now().minusDays(1));
        User director = user(50, "Giam Doc", "Director");
        when(requisitionAccess.inManagedDepartment(director, requisition)).thenReturn(true);

        assertFalse(service.isDecisionMaker(director, schedule));
        assertThrows(AccessDeniedException.class, () -> service.decide(INTERVIEW_ID, passed(), director));
    }

    @Test
    @DisplayName("Đã chốt rồi: InterviewFinalResultException")
    void secondDecisionIsRejected() {
        schedule(InterviewStatus.Completed, LocalDateTime.now().minusDays(1));
        when(interviewFinalResultRepository.findByScheduleId(INTERVIEW_ID))
                .thenReturn(Optional.of(InterviewFinalResult.builder().finalResultId(1).build()));

        assertThrows(InterviewFinalResultException.class, () -> service.decide(INTERVIEW_ID, passed(), hiringManager));
        verify(interviewFinalResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lịch đã hủy hoặc chưa diễn ra: InterviewFinalResultException")
    void cancelledOrFutureInterviewIsRejected() {
        schedule(InterviewStatus.Cancelled, LocalDateTime.now().minusDays(1));
        assertThrows(InterviewFinalResultException.class, () -> service.decide(INTERVIEW_ID, passed(), hiringManager));

        schedule(InterviewStatus.Scheduled, LocalDateTime.now().plusDays(1));
        assertThrows(InterviewFinalResultException.class, () -> service.decide(INTERVIEW_ID, passed(), hiringManager));
        verify(interviewFinalResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Không có lịch: ResourceNotFoundException")
    void missingScheduleIsNotFound() {
        when(interviewScheduleRepository.findByIdWithDetails(INTERVIEW_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.decide(INTERVIEW_ID, passed(), hiringManager));
    }

    // ===== Hạn chấm điểm 24h và kết quả "Cân nhắc" =====

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 23, 10, 0);

    /** Service với đồng hồ cố định tại NOW. */
    private InterviewFinalResultServiceImpl serviceAtNow() {
        Clock fixed = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        return new InterviewFinalResultServiceImpl(interviewScheduleRepository, interviewFinalResultRepository,
                requisitionAccess, applicationStatusService, interviewEvaluationRepository, fixed);
    }

    /** Lịch có hội đồng 2 người, kết thúc lúc endTime; submitted = số người đã nộp phiếu. */
    private InterviewSchedule panelSchedule(LocalDateTime endTime, int submitted) {
        InterviewSchedule schedule = schedule(InterviewStatus.Completed, endTime.minusHours(1));
        User a = user(21, "An Interviewer", "Interviewer");
        User b = user(22, "Binh Interviewer", "Interviewer");
        schedule.addPanelMember(a, RoleInPanel.Interviewer);
        schedule.addPanelMember(b, RoleInPanel.Interviewer);
        List<InterviewEvaluation> evaluations = List.of(a, b).subList(0, submitted).stream()
                .map(u -> InterviewEvaluation.builder().interviewer(u).interviewSchedule(schedule)
                        .recommendation("Hire").evaluatedAt(endTime.plusHours(1)).build())
                .toList();
        when(interviewEvaluationRepository.findByScheduleIdWithInterviewer(INTERVIEW_ID)).thenReturn(evaluations);
        return schedule;
    }

    private static SubmitFinalResultRequest considering() {
        return new SubmitFinalResultRequest("Considering", new BigDecimal("20000000"), "Cần so sánh thêm");
    }

    @Test
    @DisplayName("Trong 24h sau giờ kết thúc mà chưa đủ phiếu: chưa được chốt")
    void within24hMissingEvaluationsIsBlocked() {
        panelSchedule(NOW.minusHours(2), 1);

        InterviewFinalResultException ex = assertThrows(InterviewFinalResultException.class,
                () -> serviceAtNow().decide(INTERVIEW_ID, passed(), hiringManager));
        assertTrue(ex.getMessage().contains("1/2"));
        verify(interviewFinalResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Trong 24h mà đã đủ phiếu: chốt được")
    void within24hAllEvaluationsCanDecide() {
        panelSchedule(NOW.minusHours(2), 2);

        serviceAtNow().decide(INTERVIEW_ID, passed(), hiringManager);

        verify(interviewFinalResultRepository).save(any(InterviewFinalResult.class));
    }

    @Test
    @DisplayName("Sau 24h thì chốt tự do dù thiếu phiếu")
    void after24hMissingEvaluationsCanDecide() {
        panelSchedule(NOW.minusHours(25), 0);

        serviceAtNow().decide(INTERVIEW_ID, passed(), hiringManager);

        verify(interviewFinalResultRepository).save(any(InterviewFinalResult.class));
    }

    @Test
    @DisplayName("Chọn Cân nhắc: lưu ConsideredAt, hồ sơ chưa bị đổi sang Rejected/Offer")
    void consideringStoresConsideredAt() {
        panelSchedule(NOW.minusHours(25), 2);

        serviceAtNow().decide(INTERVIEW_ID, considering(), hiringManager);

        ArgumentCaptor<InterviewFinalResult> saved = ArgumentCaptor.forClass(InterviewFinalResult.class);
        verify(interviewFinalResultRepository).save(saved.capture());
        assertEquals("Considering", saved.getValue().getFinalDecision());
        assertEquals(NOW, saved.getValue().getConsideredAt());
        assertEquals(NOW, saved.getValue().getApprovedAt());
        verify(applicationStatusService).onInterviewScheduled(100);
        verify(applicationStatusService, never()).onInterviewDecided(anyInt(), anyBoolean());
    }

    @Test
    @DisplayName("Cân nhắc → Đạt: cập nhật quyết định và ApprovedAt, giữ ConsideredAt, báo hồ sơ Đạt")
    void consideringCanBeDecidedAsPassed() {
        panelSchedule(NOW.minusDays(4), 2);
        LocalDateTime consideredAt = NOW.minusDays(3);
        InterviewFinalResult existing = InterviewFinalResult.builder().finalResultId(1)
                .finalDecision("Considering").consideredAt(consideredAt).approvedAt(consideredAt).build();
        when(interviewFinalResultRepository.findByScheduleId(INTERVIEW_ID)).thenReturn(Optional.of(existing));

        serviceAtNow().decide(INTERVIEW_ID, passed(), hiringManager);

        assertEquals("Passed", existing.getFinalDecision());
        assertEquals(consideredAt, existing.getConsideredAt());
        assertEquals(NOW, existing.getApprovedAt());
        assertEquals("Ứng viên phù hợp", existing.getFinalSummaryComments());
        verify(interviewFinalResultRepository).save(existing);
        verify(applicationStatusService).onInterviewDecided(100, true);
    }

    @Test
    @DisplayName("Cân nhắc quá 7 ngày vẫn chốt được (chỉ cảnh báo); chốt Không đạt thì hồ sơ bị loại")
    void overdueConsideringCanStillBeFailed() {
        panelSchedule(NOW.minusDays(12), 2);
        InterviewFinalResult existing = InterviewFinalResult.builder().finalResultId(1)
                .finalDecision("Considering").consideredAt(NOW.minusDays(10)).approvedAt(NOW.minusDays(10)).build();
        when(interviewFinalResultRepository.findByScheduleId(INTERVIEW_ID)).thenReturn(Optional.of(existing));

        serviceAtNow().decide(INTERVIEW_ID, new SubmitFinalResultRequest("Failed", null, null), hiringManager);

        assertEquals("Failed", existing.getFinalDecision());
        verify(applicationStatusService).onInterviewDecided(100, false);
    }

    @Test
    @DisplayName("Đã Đạt / Không đạt thì không chốt lại được, kể cả sang Cân nhắc")
    void finalDecisionCannotBeReopened() {
        panelSchedule(NOW.minusDays(4), 2);
        when(interviewFinalResultRepository.findByScheduleId(INTERVIEW_ID)).thenReturn(Optional.of(
                InterviewFinalResult.builder().finalResultId(1).finalDecision("Passed").approvedAt(NOW.minusDays(3)).build()));

        assertThrows(InterviewFinalResultException.class,
                () -> serviceAtNow().decide(INTERVIEW_ID, considering(), hiringManager));
        verify(interviewFinalResultRepository, never()).save(any());
    }
}
