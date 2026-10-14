package com.group2.rms.interview;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.interview.dto.SubmitEvaluationRequest;
import com.group2.rms.interview.entity.InterviewEvaluation;
import com.group2.rms.interview.entity.InterviewFormat;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.interview.entity.RoleInPanel;
import com.group2.rms.interview.exception.InterviewEvaluationException;
import com.group2.rms.interview.repository.InterviewEvaluationRepository;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import com.group2.rms.interview.service.InterviewEvaluationServiceImpl;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InterviewEvaluationServiceTests {

    private static final Long INTERVIEW_ID = 7L;

    private InterviewScheduleRepository interviewScheduleRepository;
    private InterviewEvaluationRepository interviewEvaluationRepository;
    private InterviewEvaluationServiceImpl service;

    private User interviewer;
    private User hiringManager;

    @BeforeEach
    void setUp() {
        interviewScheduleRepository = mock(InterviewScheduleRepository.class);
        interviewEvaluationRepository = mock(InterviewEvaluationRepository.class);
        service = new InterviewEvaluationServiceImpl(interviewScheduleRepository, interviewEvaluationRepository,
                org.mockito.Mockito.mock(com.group2.rms.notification.NotificationService.class));

        interviewer = User.builder().userId(11).fullName("Tran Interviewer").accountStatus("Active")
                .role(Role.builder().roleId(5).roleName("Interviewer").build()).build();
        hiringManager = User.builder().userId(12).fullName("Le Hiring Manager").accountStatus("Active")
                .role(Role.builder().roleId(3).roleName("Hiring Manager").build()).build();
    }

    private InterviewSchedule schedule(InterviewStatus status, LocalDateTime start) {
        InterviewSchedule schedule = InterviewSchedule.builder()
                .interviewId(INTERVIEW_ID)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .interviewStatus(status)
                .startTime(start)
                .endTime(start.plusHours(1))
                .build();
        schedule.addPanelMember(interviewer, RoleInPanel.Interviewer);
        schedule.addPanelMember(hiringManager, RoleInPanel.HM);
        when(interviewScheduleRepository.findByIdWithDetails(INTERVIEW_ID)).thenReturn(Optional.of(schedule));
        return schedule;
    }

    private static SubmitEvaluationRequest request() {
        return new SubmitEvaluationRequest(4, 3, 5, "Hire", "Nắm vững Spring", "  ", "Phù hợp team");
    }

    @Test
    @DisplayName("Phiếu đầu tiên: lưu phiếu và chuyển lịch Scheduled sang Completed (D3')")
    void firstEvaluationCompletesSchedule() {
        InterviewSchedule schedule = schedule(InterviewStatus.Scheduled, LocalDateTime.now().minusHours(2));

        service.submit(INTERVIEW_ID, request(), interviewer.getUserId());

        ArgumentCaptor<InterviewEvaluation> saved = ArgumentCaptor.forClass(InterviewEvaluation.class);
        verify(interviewEvaluationRepository).save(saved.capture());
        assertEquals(interviewer, saved.getValue().getInterviewer());
        assertEquals("Hire", saved.getValue().getRecommendation());
        assertEquals(4, saved.getValue().getTechnicalScore());
        assertNull(saved.getValue().getWeaknesses(), "Ô chỉ có khoảng trắng lưu thành null");
        assertEquals(InterviewStatus.Completed, schedule.getInterviewStatus());
        verify(interviewScheduleRepository).save(schedule);
    }

    @Test
    @DisplayName("Lịch đã Completed: vẫn nộp được, không chuyển trạng thái lần nữa")
    void evaluationOnCompletedScheduleKeepsStatus() {
        InterviewSchedule schedule = schedule(InterviewStatus.Completed, LocalDateTime.now().minusDays(1));

        service.submit(INTERVIEW_ID, request(), hiringManager.getUserId());

        verify(interviewEvaluationRepository).save(any(InterviewEvaluation.class));
        verify(interviewScheduleRepository, never()).save(any());
        assertEquals(InterviewStatus.Completed, schedule.getInterviewStatus());
    }

    @Test
    @DisplayName("Người không thuộc hội đồng: AccessDeniedException (GBR-05)")
    void outsiderIsDenied() {
        schedule(InterviewStatus.Scheduled, LocalDateTime.now().minusHours(1));

        assertThrows(AccessDeniedException.class, () -> service.submit(INTERVIEW_ID, request(), 99));
        verify(interviewEvaluationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đã nộp phiếu rồi: InterviewEvaluationException")
    void duplicateEvaluationIsRejected() {
        schedule(InterviewStatus.Completed, LocalDateTime.now().minusHours(3));
        when(interviewEvaluationRepository.existsByScheduleIdAndInterviewerId(INTERVIEW_ID, interviewer.getUserId()))
                .thenReturn(true);

        InterviewEvaluationException ex = assertThrows(InterviewEvaluationException.class,
                () -> service.submit(INTERVIEW_ID, request(), interviewer.getUserId()));
        assertTrue(ex.getMessage().contains("đã nộp"));
        verify(interviewEvaluationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lịch đã hủy: InterviewEvaluationException")
    void cancelledScheduleIsRejected() {
        schedule(InterviewStatus.Cancelled, LocalDateTime.now().minusHours(3));

        assertThrows(InterviewEvaluationException.class,
                () -> service.submit(INTERVIEW_ID, request(), interviewer.getUserId()));
        verify(interviewEvaluationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chưa tới giờ phỏng vấn: InterviewEvaluationException, lịch giữ nguyên")
    void futureScheduleIsRejected() {
        InterviewSchedule schedule = schedule(InterviewStatus.Scheduled, LocalDateTime.now().plusDays(1));

        assertThrows(InterviewEvaluationException.class,
                () -> service.submit(INTERVIEW_ID, request(), interviewer.getUserId()));
        assertEquals(InterviewStatus.Scheduled, schedule.getInterviewStatus());
        verify(interviewEvaluationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Không có lịch: ResourceNotFoundException")
    void missingScheduleIsNotFound() {
        when(interviewScheduleRepository.findByIdWithDetails(INTERVIEW_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.submit(INTERVIEW_ID, request(), interviewer.getUserId()));
    }
}
