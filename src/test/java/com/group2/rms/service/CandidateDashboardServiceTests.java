package com.group2.rms.service;

import com.group2.rms.admin.service.ApiMonitoringService;
import com.group2.rms.dashboard.DashboardMetricsRepository;
import com.group2.rms.dashboard.DashboardMetricsRepository.*;
import com.group2.rms.dashboard.DashboardService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CandidateDashboardServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final DashboardMetricsRepository metrics = mock(DashboardMetricsRepository.class);
    private final ApiMonitoringService monitoring = mock(ApiMonitoringService.class);
    private final DashboardService service = new DashboardService(users, metrics, monitoring);

    @Test
    void candidatePanelUsesCurrentAccountAndContainsOnlyCandidateVisibleData() {
        User user = account("Active");
        when(users.findByUsernameIgnoreCase("candidate-test")).thenReturn(Optional.of(user));
        when(metrics.candidateApplications(41)).thenReturn(2L);
        when(metrics.candidateRecentApplications(41, 20)).thenReturn(List.of(
                new CandidateApplicationRow(5, "Kỹ sư phần mềm", LocalDateTime.of(2026, 10, 1, 9, 0), "Applied")));
        when(metrics.candidateOffers(41, 20)).thenReturn(List.of(new CandidateOfferRow(8, "Vị trí chính thức",
                new BigDecimal("20000000"), LocalDate.of(2026, 11, 1), "Declined",
                new BigDecimal("17000000"), "Hà Nội", "Bảo hiểm sức khỏe")));
        var response = service.forUsername("candidate-test");
        assertEquals(2, response.metrics().getFirst().value());
        assertEquals("Đã nộp hồ sơ", response.candidate().recentApplications().getFirst().status());
        assertNull(response.candidate().recentApplications().getFirst().stage());
        assertEquals("Đã từ chối", response.candidate().offers().getFirst().statusLabel());
        assertEquals("candidate@example.test", response.candidate().profile().email());
        verify(metrics).candidateUpcomingInterviews(eq(41), any());
        verify(metrics).candidateNextInterviews(eq(41), any(), eq(10));
        verify(metrics).candidateApplicationStages(41);
        verify(metrics).candidateOfferStatuses(41);
        verifyNoInteractions(monitoring);
    }

    @Test
    void meetingLinksRejectExecutableAndMalformedSchemes() {
        when(users.findByUsernameIgnoreCase("candidate-test")).thenReturn(Optional.of(account("Active")));
        var start = LocalDateTime.of(2026, 11, 1, 10, 0);
        when(metrics.candidateNextInterviews(eq(41), any(), eq(10))).thenReturn(List.of(
                new CandidateInterviewRow("Safe", start, start.plusHours(1), "Online_GoogleMeet", "https://meet.google.com/test-room"),
                new CandidateInterviewRow("Unsafe", start, start.plusHours(1), "Online_GoogleMeet", "javascript:alert(1)"),
                new CandidateInterviewRow("Malformed", start, start.plusHours(1), "Online_GoogleMeet", "https://a bad host/"),
                new CandidateInterviewRow("Office", start, start.plusHours(1), "Offline_Office", "Phòng 3, Hà Nội")));
        var interviews = service.forUsername("candidate-test").candidate().upcomingInterviews();
        assertEquals("https://meet.google.com/test-room", interviews.getFirst().meetingUrl());
        assertNull(interviews.get(1).meetingUrl());
        assertNull(interviews.get(2).meetingUrl());
        assertNull(interviews.get(3).meetingUrl());
        assertEquals("Phòng 3, Hà Nội", interviews.get(3).location());
    }

    @Test
    void disabledAccountCannotReadCandidateData() {
        when(users.findByUsernameIgnoreCase("candidate-test")).thenReturn(Optional.of(account("Inactive")));
        assertThrows(AccessDeniedException.class, () -> service.forUsername("candidate-test"));
        verifyNoInteractions(metrics, monitoring);
    }

    @Test
    @SuppressWarnings("unchecked")
    void officialOfferQueryScopesOwnerAndExcludesInternalStatuses() {
        EntityManager em = mock(EntityManager.class);
        TypedQuery<Object[]> query = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Object[].class))).thenReturn(query);
        when(query.setParameter(eq("userId"), any())).thenReturn(query);
        when(query.setMaxResults(20)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        new DashboardMetricsRepository(em).candidateOffers(41, 20);
        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);
        verify(em).createQuery(jpql.capture(), eq(Object[].class));
        assertTrue(jpql.getValue().contains("o.application.candidate.account.userId = :userId"));
        assertTrue(jpql.getValue().contains("'Declined'"));
        for (String internal : List.of("'Draft'", "'Pending_Director'", "'Director_Approved'", "'Director_Rejected'", "HRResponseNotes", "DirectorComments")) {
            assertFalse(jpql.getValue().contains(internal), internal);
        }
        verify(query).setParameter("userId", 41);
    }

    private User account(String status) {
        return User.builder().userId(41).username("candidate-test").fullName("Ứng viên kiểm thử")
                .email("candidate@example.test").role(Role.builder().roleName("Candidate").build())
                .accountStatus(status).build();
    }
}
