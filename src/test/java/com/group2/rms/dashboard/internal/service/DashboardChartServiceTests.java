package com.group2.rms.dashboard;

import com.group2.rms.dashboard.DashboardMetricsRepository.DecisionRow;
import com.group2.rms.dashboard.DashboardMetricsRepository.StatusCount;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Unit checks for role scope, chart values and links. JPQL is verified separately on SQL Server. */
class DashboardChartServiceTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T03:00:00Z"),
            ZoneId.of("Asia/Ho_Chi_Minh"));
    private final UserRepository users = mock(UserRepository.class);
    private final DashboardMetricsRepository metrics = mock(DashboardMetricsRepository.class);
    private final DashboardChartService service = new DashboardChartService(users, metrics, CLOCK);

    @Test
    void forUsername_shouldReturnNoLegacyCharts_whenHiringManager() {
        // Hiring Manager đã có biểu đồ riêng trong HiringManagerDashboardService.
        User actor = actor("Hiring Manager");
        actor.setDepartment(Department.builder().departmentId(3).build());
        when(users.findByUsernameIgnoreCase("viewer")).thenReturn(Optional.of(actor));

        var charts = service.forUsername("viewer", "Hiring Manager");

        assertEquals(List.of(), charts);
        verifyNoInteractions(metrics);
    }

    @Test
    void forUsername_shouldIncludeTodayAndFormalStatuses_whenHr() {
        when(users.findByUsernameIgnoreCase("viewer")).thenReturn(Optional.of(actor("HR")));
        when(metrics.formalApplicationStatuses()).thenReturn(List.of(new StatusCount("Applied", 3)));
        when(metrics.recentApplicationDates(any(), any())).thenReturn(List.of(
                LocalDateTime.of(2026, 10, 20, 9, 0)));
        when(metrics.jobPostingViewStatuses(any())).thenReturn(List.of(
                new StatusCount("Scheduled", 1), new StatusCount("Paused", 2)));

        var charts = service.forUsername("viewer", "HR");

        assertEquals(3, charts.get(0).points().getFirst().count());
        assertEquals(1, charts.get(1).points().getLast().count());
        assertEquals(1, charts.get(2).points().get(2).count());
        assertEquals("/internal/job-postings?status=Scheduled", charts.get(2).points().get(2).href());
        assertEquals(2, charts.get(2).points().get(3).count());
    }

    @Test
    void forUsername_shouldSeparateSystemQueueFromOwnDecisions_whenDirector() {
        when(users.findByUsernameIgnoreCase("viewer")).thenReturn(Optional.of(actor("Director")));
        when(metrics.requisitionsAwaitingDirector()).thenReturn(5L);
        when(metrics.offersAwaitingDirector()).thenReturn(2L);
        when(metrics.directorDecisions(eq(7), any(), any())).thenReturn(List.of(
                new DecisionRow("Approved", LocalDateTime.of(2026, 10, 20, 8, 0)),
                new DecisionRow("Rejected", LocalDateTime.of(2026, 10, 19, 8, 0))));

        var charts = service.forUsername("viewer", "Director");

        assertEquals(5, charts.get(0).points().getFirst().count());
        assertEquals("/offers?status=PENDING", charts.get(0).points().get(1).href());
        assertEquals(1, charts.get(1).points().getFirst().count());
        assertEquals(1, charts.get(1).points().get(1).count());
        assertEquals(1, charts.get(2).points().getLast().count());
        verify(metrics).directorDecisions(eq(7), any(), any());
    }

    @Test
    void forUsername_shouldOnlyChartAssignedInterviewsAndOwnEvaluations_whenInterviewer() {
        when(users.findByUsernameIgnoreCase("viewer")).thenReturn(Optional.of(actor("Interviewer")));
        when(metrics.assignedInterviewStatuses(7)).thenReturn(List.of(new StatusCount("Scheduled", 3)));
        when(metrics.upcomingAssignedInterviewDates(eq(7), any(), any())).thenReturn(List.of(
                LocalDateTime.of(2026, 10, 20, 11, 0)));
        when(metrics.pendingInterviewEvaluations(7)).thenReturn(2L);
        when(metrics.completedInterviewsEvaluatedBy(7)).thenReturn(4L);

        var charts = service.forUsername("viewer", "Interviewer");

        assertEquals(3, charts.size());
        assertEquals(3, charts.get(0).points().getFirst().count());
        assertEquals("/interviews", charts.get(0).points().getFirst().href());
        assertEquals(1, charts.get(1).points().getFirst().count());
        assertEquals(2, charts.get(2).points().getFirst().count());
        assertEquals(4, charts.get(2).points().get(1).count());
        verify(metrics).assignedInterviewStatuses(7);
    }

    @Test
    void forUsername_shouldRejectRoleMismatch_beforeReadingMetrics() {
        when(users.findByUsernameIgnoreCase("viewer")).thenReturn(Optional.of(actor("Candidate")));

        assertThrows(AccessDeniedException.class, () -> service.forUsername("viewer", "HR"));
    }

    private User actor(String roleName) {
        return User.builder().userId(7).username("viewer").accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
    }
}
