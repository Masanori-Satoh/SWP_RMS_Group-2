package com.group2.rms.dashboard;

import com.group2.rms.admin.dto.MonitorRowResponse;
import com.group2.rms.admin.service.ApiMonitoringService;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.core.web.PageLinks;
import com.group2.rms.dashboard.DashboardMetricsRepository.RoleCount;
import com.group2.rms.dashboard.DashboardMetricsRepository.StatusCount;
import com.group2.rms.dashboard.dto.AdminDashboardResponse;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminDashboardAnalyticsTests {
    private final UserRepository users = mock(UserRepository.class);
    private final DashboardMetricsRepository metrics = mock(DashboardMetricsRepository.class);
    private final ApiMonitoringService monitoring = mock(ApiMonitoringService.class);
    private final DashboardService service = new DashboardService(users, metrics, monitoring);

    private void admin(String status) {
        when(users.findByUsernameIgnoreCase("analytics-test")).thenReturn(Optional.of(User.builder()
                .username("analytics-test").fullName("Analytics test").accountStatus(status)
                .role(Role.builder().roleId(73).roleName("System Admin").build()).build()));
    }

    @Test
    void summariesKeepLifecyclesSeparateAndUseOneMonitoringSnapshotWithoutProbing() {
        admin("Active");
        when(metrics.internalAccountStatuses()).thenReturn(List.of(new StatusCount("Active", 12), new StatusCount("Blocked", 2)));
        when(metrics.candidateAccountStatuses()).thenReturn(List.of(new StatusCount("Inactive", 7), new StatusCount("Blocked", 3)));
        when(metrics.candidates()).thenReturn(8L);
        when(metrics.candidatesCreatedBetween(any(), any())).thenReturn(2L);
        var role = new RoleCount(97, "HR", 0);
        when(metrics.internalAccountsByRole()).thenReturn(List.of(role));
        var rows = List.of(new MonitorRowResponse("internal", "Application and SQL Server", "Internal", "Not check", null, null));
        when(monitoring.rows()).thenReturn(rows);
        var response = (AdminDashboardResponse) service.forUsername("analytics-test");
        assertEquals(14, response.internalTotal());
        assertEquals(5, response.blockedTotal());
        assertEquals(8, response.candidates().total());
        assertEquals(2, response.candidates().newLast30Days());
        assertEquals(Duration.ofDays(30), Duration.between(response.candidates().from(), response.candidates().to()));
        assertEquals(ZoneId.systemDefault().getId(), response.candidates().timeZone());
        verify(metrics).candidatesCreatedBetween(response.candidates().from(), response.candidates().to());
        assertEquals("/admin/accounts", response.accountSummaries().getFirst().url());
        assertEquals("/admin/candidate-accounts", response.accountSummaries().getLast().url());
        assertEquals(List.of(role), response.roleCounts());
        assertSame(rows, response.monitorRows());
        verify(monitoring, times(1)).rows();
        verifyNoMoreInteractions(monitoring);
    }

    @Test
    void emptyCountsAndZeroAccountRoleRemainTruthful() {
        admin("Active");
        when(metrics.internalAccountStatuses()).thenReturn(List.of());
        when(metrics.candidateAccountStatuses()).thenReturn(List.of());
        when(metrics.departmentStatuses()).thenReturn(List.of());
        when(metrics.internalAccountsByRole()).thenReturn(List.of(new RoleCount(97, "HR", 0)));
        when(monitoring.rows()).thenReturn(List.of());
        var response = (AdminDashboardResponse) service.forUsername("analytics-test");
        assertEquals(0, response.internalTotal());
        assertEquals(0, response.blockedTotal());
        assertEquals(0, response.candidates().total());
        assertEquals(0, response.candidates().newLast30Days());
        assertEquals(97, response.roleCounts().getFirst().roleId());
        assertEquals(0, response.roleCounts().getFirst().count());
        assertTrue(response.monitorRows().isEmpty());
    }

    @Test
    void inactiveAccountCannotReadDashboard() {
        admin("Inactive");
        assertThrows(AccessDeniedException.class, () -> service.forUsername("analytics-test"));
        verifyNoInteractions(metrics, monitoring);
    }

    @Test
    void hrDashboardDoesNotLoadAdminMetricsOrMonitoring() {
        when(users.findByUsernameIgnoreCase("hr-test")).thenReturn(Optional.of(User.builder()
                .username("hr-test").fullName("HR test").accountStatus("Active")
                .role(Role.builder().roleId(97).roleName("HR").build()).build()));
        service.forUsername("hr-test");
        verify(metrics, never()).internalAccountsByRole();
        verify(metrics, never()).internalAccountStatuses();
        verify(metrics, never()).candidates();
        verify(metrics, never()).candidatesCreatedBetween(any(), any());
        verifyNoInteractions(monitoring);
    }

    @Test
    void hiringManagerDoesNotLoadCandidateSummaryMetrics() {
        when(users.findByUsernameIgnoreCase("manager-test")).thenReturn(Optional.of(User.builder()
                .userId(77).username("manager-test").fullName("Manager").accountStatus("Active")
                .role(Role.builder().roleId(97).roleName("Hiring Manager").build()).build()));
        service.forUsername("manager-test");
        verify(metrics, never()).candidates();
        verify(metrics, never()).candidatesCreatedBetween(any(), any());
        verifyNoInteractions(monitoring);
    }

    @Test
    @SuppressWarnings("unchecked")
    void aggregateReturnsDatabaseIdsAndIncludesZeroRolesWithARealLeftJoin() {
        EntityManager em = mock(EntityManager.class);
        TypedQuery<Object[]> query = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Object[].class))).thenReturn(query);
        when(query.setParameter(eq("roles"), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{97, "HR", 0L}, new Object[]{73, "System Admin", 2L}));
        var counts = new DashboardMetricsRepository(em).internalAccountsByRole();
        assertEquals(new RoleCount(97, "HR", 0), counts.getFirst());
        var jpql = ArgumentCaptor.forClass(String.class);
        verify(em).createQuery(jpql.capture(), eq(Object[].class));
        assertTrue(jpql.getValue().contains("left join User u on u.role = r"));
        assertTrue(jpql.getValue().contains("count(u.userId)"));
        verify(query).setParameter("roles", RoleAuthorities.INTERNAL_ROLE_NAMES);
    }

    @Test
    void paginationPreservesAllFiltersAndAlreadyEncodedSpecialCharacters() {
        String query = "status=Active&roleId=73&departmentId=97&search=An%20%26%20B%C3%ACnh%2BR%25&sort=newest&page=8";
        String link = new PageLinks("/rms/admin/accounts", query).to(1);
        assertEquals("/rms/admin/accounts?status=Active&roleId=73&departmentId=97&search=An%20%26%20B%C3%ACnh%2BR%25&sort=newest&page=1", link);
    }
}
