package com.group2.rms.dashboard.internal.service;

import com.group2.rms.dashboard.internal.dto.DashboardResponse;
import com.group2.rms.dashboard.internal.dto.DirectorDashboardResponse;
import com.group2.rms.dashboard.internal.dto.HiringManagerDashboardResponse;
import com.group2.rms.dashboard.internal.dto.HrDashboardResponse;
import com.group2.rms.dashboard.internal.dto.InterviewerDashboardResponse;
import com.group2.rms.dashboard.shared.DashboardAccess;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST (Mockito): InternalDashboardService kiểm tra người xem rồi giao cho đúng service của role.
 * Service của từng role là mock; test dữ liệu của từng role nằm ở {Role}DashboardServiceTests.
 */
class InternalDashboardServiceTests {

    private final UserRepository users = mock(UserRepository.class);
    private final HrDashboardService hrDashboard = mock(HrDashboardService.class);
    private final HiringManagerDashboardService hiringManagerDashboard = mock(HiringManagerDashboardService.class);
    private final DirectorDashboardService directorDashboard = mock(DirectorDashboardService.class);
    private final InterviewerDashboardService interviewerDashboard = mock(InterviewerDashboardService.class);
    private final InternalDashboardService service = new InternalDashboardService(new DashboardAccess(users),
            hrDashboard, hiringManagerDashboard, directorDashboard, interviewerDashboard);

    private User account(String roleName, String status) {
        User user = User.builder().userId(7).username("dashboard").fullName("Người dùng kiểm thử")
                .accountStatus(status).role(Role.builder().roleName(roleName).build()).build();
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    @DisplayName("Username không tồn tại -> UsernameNotFoundException, không dựng dashboard nào")
    void forUsername_shouldThrowNotFound_whenAccountMissing() {
        // Act + Assert
        assertThrows(UsernameNotFoundException.class, () -> service.forUsername("missing", null));
        verifyNoInteractions(hrDashboard, hiringManagerDashboard, directorDashboard, interviewerDashboard);
    }

    @Test
    @DisplayName("Tài khoản bị khóa -> AccessDeniedException, không dựng dashboard nào")
    void forUsername_shouldReject_whenAccountBlocked() {
        // Arrange
        account(DashboardAccess.ROLE_HR, "Blocked");

        // Act + Assert
        assertThrows(AccessDeniedException.class, () -> service.forUsername("dashboard", null));
        verifyNoInteractions(hrDashboard, hiringManagerDashboard, directorDashboard, interviewerDashboard);
    }

    @ParameterizedTest(name = "{0} -> 403")
    @ValueSource(strings = {DashboardAccess.ROLE_SYSTEM_ADMIN, DashboardAccess.ROLE_CANDIDATE})
    @DisplayName("System Admin và Candidate không được mở dashboard nội bộ")
    void forUsername_shouldReject_whenRoleIsNotInternal(String roleName) {
        // Arrange
        account(roleName, "Active");

        // Act + Assert
        assertThrows(AccessDeniedException.class, () -> service.forUsername("dashboard", null));
        verifyNoInteractions(hrDashboard, hiringManagerDashboard, directorDashboard, interviewerDashboard);
    }

    @Test
    @DisplayName("HR -> HrDashboardService")
    void forUsername_shouldDelegateToHrService_whenHr() {
        // Arrange
        User user = account(DashboardAccess.ROLE_HR, "Active");
        HrDashboardResponse expected = new HrDashboardResponse("HR", "Người dùng kiểm thử", List.of(), List.of(), 0,
                List.of(), List.of(), List.of(), "", List.of(), 0);
        when(hrDashboard.build(user)).thenReturn(expected);

        // Act
        DashboardResponse result = service.forUsername("dashboard", null);

        // Assert
        assertSame(expected, result);
        verifyNoInteractions(hiringManagerDashboard, directorDashboard, interviewerDashboard);
    }

    @Test
    @DisplayName("Hiring Manager -> HiringManagerDashboardService")
    void forUsername_shouldDelegateToHiringManagerService_whenHiringManager() {
        // Arrange
        User user = account(DashboardAccess.ROLE_HIRING_MANAGER, "Active");
        HiringManagerDashboardResponse expected = new HiringManagerDashboardResponse("Hiring Manager", "Người dùng kiểm thử",
                "", List.of(), List.of(), 0, List.of(), List.of());
        when(hiringManagerDashboard.build(user)).thenReturn(expected);

        // Act
        DashboardResponse result = service.forUsername("dashboard", null);

        // Assert
        assertSame(expected, result);
        verifyNoInteractions(hrDashboard, directorDashboard, interviewerDashboard);
    }

    @Test
    @DisplayName("Director -> DirectorDashboardService")
    void forUsername_shouldDelegateToDirectorService_whenDirector() {
        // Arrange
        User user = account(DashboardAccess.ROLE_DIRECTOR, "Active");
        DirectorDashboardResponse expected = new DirectorDashboardResponse("Director", "Người dùng kiểm thử", List.of(),
                List.of(), 0, List.of(), 0, List.of(), List.of());
        when(directorDashboard.build(user)).thenReturn(expected);

        // Act
        DashboardResponse result = service.forUsername("dashboard", null);

        // Assert
        assertSame(expected, result);
        verifyNoInteractions(hrDashboard, hiringManagerDashboard, interviewerDashboard);
    }

    @Test
    @DisplayName("Interviewer -> InterviewerDashboardService, chuyển nguyên tham số tuần")
    void forUsername_shouldPassWeekToInterviewerService_whenInterviewer() {
        // Arrange
        User user = account(DashboardAccess.ROLE_INTERVIEWER, "Active");
        InterviewerDashboardResponse expected = new InterviewerDashboardResponse("Interviewer", "Người dùng kiểm thử",
                List.of(), null, 0, List.of(), List.of(), LocalDate.of(2026, 11, 2), 0);
        when(interviewerDashboard.build(any(), any())).thenReturn(expected);

        // Act
        DashboardResponse result = service.forUsername("dashboard", "2026-11-02");

        // Assert
        assertSame(expected, result);
        verify(interviewerDashboard).build(user, "2026-11-02");
        verifyNoInteractions(hrDashboard, hiringManagerDashboard, directorDashboard);
    }
}
