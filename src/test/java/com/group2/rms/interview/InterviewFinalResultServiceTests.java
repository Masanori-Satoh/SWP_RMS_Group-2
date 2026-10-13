package com.group2.rms.interview;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.service.ApplicationStatusService;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.interview.dto.SubmitFinalResultRequest;
import com.group2.rms.interview.entity.InterviewFinalResult;
import com.group2.rms.interview.entity.InterviewFormat;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.interview.exception.InterviewFinalResultException;
import com.group2.rms.interview.repository.InterviewFinalResultRepository;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import com.group2.rms.interview.service.InterviewFinalResultServiceImpl;
import com.group2.rms.requisition.entity.JobPosting;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InterviewFinalResultServiceTests {

    private static final Long INTERVIEW_ID = 9L;

    private InterviewScheduleRepository interviewScheduleRepository;
    private InterviewFinalResultRepository interviewFinalResultRepository;
    private RequisitionAccess requisitionAccess;
    private ApplicationStatusService applicationStatusService; // dunglt bổ sung
    private InterviewFinalResultServiceImpl service;

    private User hiringManager;
    private JobRequisition requisition;

    @BeforeEach
    void setUp() {
        interviewScheduleRepository = mock(InterviewScheduleRepository.class);
        interviewFinalResultRepository = mock(InterviewFinalResultRepository.class);
        requisitionAccess = mock(RequisitionAccess.class);
        applicationStatusService = mock(ApplicationStatusService.class); // dunglt bổ sung
        service = new InterviewFinalResultServiceImpl(
                interviewScheduleRepository, interviewFinalResultRepository, requisitionAccess,
                applicationStatusService); // dunglt bổ sung

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
}
