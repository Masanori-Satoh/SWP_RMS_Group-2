package com.group2.rms.service;

import com.group2.rms.admin.service.ApiMonitoringService;
import com.group2.rms.dashboard.*;
import com.group2.rms.dashboard.DashboardMetricsRepository.StatusCount;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminDepartmentDashboardTests {
    @Test void adminDepartmentCountsComeFromRepositoryAndKeepAccountLifecyclesSeparate() {
        var users=mock(UserRepository.class); var metrics=mock(DashboardMetricsRepository.class); var api=mock(ApiMonitoringService.class);
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(User.builder().username("admin").fullName("Admin")
                .role(Role.builder().roleName("System Admin").build()).accountStatus("Active").build()));
        when(metrics.departmentStatuses()).thenReturn(List.of(new StatusCount("Active",5),new StatusCount("Inactive",2)));
        when(metrics.internalAccountStatuses()).thenReturn(List.of(new StatusCount("Active",10),new StatusCount("Blocked",1)));
        when(metrics.candidateAccountStatuses()).thenReturn(List.of(new StatusCount("Inactive",3)));
        var result=new DashboardService(users,metrics,api).forUsername("admin");
        assertEquals(new DashboardResponse.DepartmentSummary(7,5,2),result.departments());
        assertEquals(11,result.accountSummaries().getFirst().total()); assertEquals(3,result.accountSummaries().getLast().total());
        assertTrue(result.shortcuts().stream().anyMatch(link->"/admin/departments".equals(link.url())));
        assertTrue(result.shortcuts().stream().noneMatch(link->"/requisitions".equals(link.url())));
        assertNull(result.candidate()); verify(metrics).departmentStatuses();
    }
}
