package com.group2.rms.interview;

import com.group2.rms.candidate.Application;
import com.group2.rms.candidate.ApplicationRepository;
import com.group2.rms.interview.dto.InterviewScheduleRequest;
import com.group2.rms.interview.dto.InterviewScheduleResponse;
import com.group2.rms.interview.dto.PanelMemberRequest;
import com.group2.rms.interview.entity.InterviewFormat;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.interview.entity.RoleInPanel;
import com.group2.rms.interview.exception.InterviewStatusException;
import com.group2.rms.interview.repository.InterviewPanelRepository;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import com.group2.rms.interview.service.InterviewSchedulingServiceImpl;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InterviewSchedulingServiceTests {

    private InterviewScheduleRepository interviewScheduleRepository;
    private InterviewPanelRepository interviewPanelRepository;
    private ApplicationRepository applicationRepository;
    private UserRepository userRepository;
    private InterviewSchedulingServiceImpl service;

    private User hrUser;
    private User candidateUser;
    private User hmUser;
    private Application application;

    @BeforeEach
    void setUp() {
        interviewScheduleRepository = mock(InterviewScheduleRepository.class);
        interviewPanelRepository = mock(InterviewPanelRepository.class);
        applicationRepository = mock(ApplicationRepository.class);
        userRepository = mock(UserRepository.class);

        service = new InterviewSchedulingServiceImpl(
                interviewScheduleRepository,
                interviewPanelRepository,
                applicationRepository,
                userRepository
        );

        hrUser = User.builder()
                .userId(1)
                .username("hr_user")
                .fullName("HR Manager")
                .accountStatus("Active")
                .role(Role.builder().roleId(2).roleName("HR").build())
                .build();

        candidateUser = User.builder()
                .userId(2)
                .username("candidate_user")
                .fullName("Nguyen Candidate")
                .accountStatus("Active")
                .role(Role.builder().roleId(6).roleName("Candidate").build())
                .build();

        hmUser = User.builder()
                .userId(3)
                .username("hm_user")
                .fullName("Le Hiring Manager")
                .accountStatus("Active")
                .role(Role.builder().roleId(3).roleName("Hiring Manager").build())
                .build();

        application = Application.builder()
                .applicationId(10)
                .applicationStatus("Applied")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(hrUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(candidateUser));
        when(userRepository.findById(3)).thenReturn(Optional.of(hmUser));
        when(applicationRepository.findById(10)).thenReturn(Optional.of(application));
    }

    @Test
    @DisplayName("Gán người dùng có vai trò Candidate vào hội đồng phỏng vấn phải bị từ chối")
    void cannotAssignCandidateUserToInterviewPanel() {
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .panelMembers(List.of(new PanelMemberRequest(2, RoleInPanel.HM)))
                .build();

        InterviewStatusException ex = assertThrows(InterviewStatusException.class, () ->
                service.createSchedule(request, 1)
        );

        assertTrue(ex.getMessage().contains("không được phép tham gia Hội đồng phỏng vấn"));
    }

    @Test
    @DisplayName("Gán người dùng có vai trò HR vào hội đồng phỏng vấn phải bị từ chối")
    void cannotAssignHrUserToInterviewPanel() {
        User anotherHr = User.builder()
                .userId(4)
                .username("hr_2")
                .fullName("Another HR")
                .accountStatus("Active")
                .role(Role.builder().roleId(2).roleName("HR").build())
                .build();
        when(userRepository.findById(4)).thenReturn(Optional.of(anotherHr));

        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .panelMembers(List.of(new PanelMemberRequest(4, RoleInPanel.HM)))
                .build();

        InterviewStatusException ex = assertThrows(InterviewStatusException.class, () ->
                service.createSchedule(request, 1)
        );

        assertTrue(ex.getMessage().contains("không được phép tham gia Hội đồng phỏng vấn"));
    }

    @Test
    @DisplayName("Tài khoản không Active không được tham gia hội đồng phỏng vấn")
    void cannotAssignInactiveUserToInterviewPanel() {
        User inactiveHm = User.builder()
                .userId(5)
                .username("inactive_hm")
                .fullName("Inactive HM")
                .accountStatus("Inactive")
                .role(Role.builder().roleId(3).roleName("Hiring Manager").build())
                .build();
        when(userRepository.findById(5)).thenReturn(Optional.of(inactiveHm));

        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .panelMembers(List.of(new PanelMemberRequest(5, RoleInPanel.HM)))
                .build();

        InterviewStatusException ex = assertThrows(InterviewStatusException.class, () ->
                service.createSchedule(request, 1)
        );

        assertTrue(ex.getMessage().contains("không ở trạng thái Active"));
    }

    @Test
    @DisplayName("Lên lịch thành công với Interviewer hợp lệ (Hiring Manager, Active)")
    void createScheduleSuccessWithValidInternalUser() {
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .panelMembers(List.of(new PanelMemberRequest(3, RoleInPanel.HM)))
                .build();

        when(interviewScheduleRepository.save(any(InterviewSchedule.class))).thenAnswer(invocation -> {
            InterviewSchedule s = invocation.getArgument(0);
            s.setInterviewId(100L);
            return s;
        });

        InterviewScheduleResponse response = service.createSchedule(request, 1);
        assertNotNull(response);
        assertEquals(InterviewStatus.Scheduled, response.interviewStatus());
        verify(interviewScheduleRepository, atLeastOnce()).save(any(InterviewSchedule.class));
    }

    @Test
    @DisplayName("Rule GBR-01: Cập nhật đi lùi trạng thái từ Completed về Scheduled bị ném ngoại lệ")
    void forwardOnlyStateTransitionEnforced() {
        InterviewSchedule existingSchedule = InterviewSchedule.builder()
                .interviewId(200L)
                .interviewStatus(InterviewStatus.Completed)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .build();

        when(interviewScheduleRepository.findByIdWithDetails(200L)).thenReturn(Optional.of(existingSchedule));

        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .interviewStatus(InterviewStatus.Scheduled)
                .build();

        assertThrows(InterviewStatusException.class, () ->
                service.updateSchedule(200L, request, 1)
        );
    }
}
