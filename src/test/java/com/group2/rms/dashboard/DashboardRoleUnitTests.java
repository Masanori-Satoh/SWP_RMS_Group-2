package com.group2.rms.dashboard;

import com.group2.rms.admin.service.ApiMonitoringService;
import com.group2.rms.dashboard.DashboardMetricsRepository.*;
import com.group2.rms.dashboard.dto.*;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DashboardRoleUnitTests {
    private final UserRepository users = mock(UserRepository.class);
    private final DashboardMetricsRepository metrics = mock(DashboardMetricsRepository.class);
    private final ApiMonitoringService monitoring = mock(ApiMonitoringService.class);
    private final HiringManagerDashboardService hiringManagerDashboard = mock(HiringManagerDashboardService.class);
    private final DashboardService service = new DashboardService(users, metrics, monitoring, hiringManagerDashboard,
            Clock.fixed(Instant.parse("2026-10-19T02:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));
    private final User user = User.builder().userId(7).username("dashboard").fullName("Test User")
            .email("candidate@example.test").accountStatus("Active").role(Role.builder().roleName("Candidate").build()).build();
    private final LocalDateTime at = LocalDateTime.of(2026, 10, 19, 9, 0);

    @Test
    @DisplayName("DASHBOARD-UNIT-01 | Username không tồn tại: từ chối trước khi đọc metrics")
    void forUsername_missingAccount_throwsNotFound() {
        // Given: Username không tồn tại: từ chối trước khi đọc metrics.
        // Repository mặc định trả Optional.empty.

        // When: Lấy dashboard của tài khoản hiện tại.
        UsernameNotFoundException error = assertThrows(UsernameNotFoundException.class, () -> service.forUsername("missing"));

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Account not found", error.getMessage());
        verify(metrics, times(0)).candidates();
        verify(monitoring, times(0)).rows();
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-02 | Role không hỗ trợ: không tải dữ liệu Admin")
    void forUsername_unsupportedRole_rejectsWithoutAdminData() {
        // Given: Role không hỗ trợ: không tải dữ liệu Admin.
        user.getRole().setRoleName("Unknown");
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));

        // When: Lấy dashboard của tài khoản hiện tại.
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.forUsername("dashboard"));

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Unsupported dashboard role", error.getMessage());
        verify(metrics, times(0)).internalAccountStatuses();
        verify(monitoring, times(0)).rows();
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-03 | Account Blocked: không được đọc dashboard")
    void forUsername_blockedAccount_rejectsBeforeMetrics() {
        // Given: Account Blocked: không được đọc dashboard.
        user.setAccountStatus("Blocked");
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));

        // When: Lấy dashboard của tài khoản hiện tại.
        AccessDeniedException error = assertThrows(AccessDeniedException.class, () -> service.forUsername("dashboard"));

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Inactive account cannot access dashboard", error.getMessage());
        verify(metrics, times(0)).candidateApplications(anyInt());
        verify(monitoring, times(0)).rows();
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-04 | Director: tổng hàng chờ hệ thống, interview và activity của đúng user")
    void director_scopesPersonalMetricsAndReturnsApprovalQueue() {
        // Given: Director: tổng hàng chờ hệ thống, interview và activity của đúng user.
        user.getRole().setRoleName("Director");
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.requisitionsAwaitingDirector()).thenReturn(4L);
        when(metrics.offersAwaitingDirector()).thenReturn(3L);
        when(metrics.assignedCandidatesForInterviewer(7)).thenReturn(2L);

        // When: Lấy dashboard của tài khoản hiện tại.
        RecruitmentDashboardResponse result = (RecruitmentDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Director", result.roleName());
        assertEquals(4L, result.metrics().get(0).value());
        assertEquals(3L, result.metrics().get(1).value());
        assertEquals(2L, result.metrics().get(3).value());
        verify(metrics, times(1)).upcomingAssignedInterviews(eq(7), any(LocalDateTime.class));
        verify(metrics, times(1)).recentDirectorActivity(7);
        verify(monitoring, times(0)).rows();
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-05 | Interviewer: chỉ lấy lịch, ứng viên và đánh giá được giao")
    void interviewer_usesOnlyAssignedMetrics() {
        // Given: Interviewer: chỉ lấy lịch, ứng viên và đánh giá được giao.
        user.getRole().setRoleName("Interviewer");
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.pendingInterviewEvaluations(7)).thenReturn(5L);

        // When: Lấy dashboard của tài khoản hiện tại.
        RecruitmentDashboardResponse result = (RecruitmentDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Interviewer", result.roleName());
        assertEquals(5L, result.metrics().get(2).value());
        verify(metrics, times(1)).upcomingAssignedInterviews(eq(7), any(LocalDateTime.class));
        verify(metrics, times(1)).assignedCandidatesForInterviewer(7);
        verify(metrics, times(0)).internalAccountStatuses();
        verify(monitoring, times(0)).rows();
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-06 | Hiring Manager -> giao cho HiringManagerDashboardService, không đọc số liệu khác")
    void forUsername_shouldDelegateToHiringManagerService_whenRoleIsHiringManager() {
        // Arrange
        user.getRole().setRoleName("Hiring Manager");
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        HiringManagerDashboardResponse built = new HiringManagerDashboardResponse("Hiring Manager", "Test User",
                List.of(), List.of(), List.of(), List.of(), 0, List.of());
        when(hiringManagerDashboard.build(user)).thenReturn(built);

        // Act
        DashboardResponse result = service.forUsername("dashboard");

        // Assert
        assertEquals(built, result);
        verify(hiringManagerDashboard, times(1)).build(user);
        verifyNoInteractions(metrics, monitoring);
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-07 | Candidate chưa có dữ liệu: metrics bằng 0, profile đúng account")
    void candidate_emptyData_returnsZerosAndOwnProfile() {
        // Given: Candidate chưa có dữ liệu: metrics bằng 0, profile đúng account.
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));

        // When: Lấy dashboard của tài khoản hiện tại.
        CandidateDashboardResponse result = (CandidateDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Candidate", result.roleName());
        assertEquals("candidate@example.test", result.profile().email());
        assertEquals(null, result.profile().departmentName());
        assertEquals(0L, result.metrics().getFirst().value());
        assertEquals(List.of(), result.recentApplications());
        assertEquals(List.of(), result.upcomingInterviews());
        assertEquals(List.of(), result.offers());
        verify(metrics, times(1)).candidateApplications(7);
        verify(metrics, times(0)).applications();
        verify(monitoring, times(0)).rows();
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-08 | Candidate: ánh xạ nhãn trạng thái, giữ nguyên giá trị không biết")
    void candidate_mapsApplicationStatusesWithoutChangingStoredValues() {
        // Given: Candidate: ánh xạ nhãn trạng thái, giữ nguyên giá trị không biết.
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.candidateApplicationStages(7)).thenReturn(List.of(
                new StatusCount("Applied", 1),
                new StatusCount("AI_Screened", 1),
                new StatusCount("HR_Passed", 1),
                new StatusCount("HM_Passed", 1),
                new StatusCount("Interviewing", 1),
                new StatusCount("Offered", 1),
                new StatusCount("Hired", 1),
                new StatusCount("Rejected", 1),
                new StatusCount("Other", 1)));
        when(metrics.candidateRecentApplications(7, 20)).thenReturn(List.of(new CandidateApplicationRow(10, "Developer", at, "Applied")));

        // When: Lấy dashboard của tài khoản hiện tại.
        CandidateDashboardResponse result = (CandidateDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals(List.of(
                new StatusCount("Đã nộp hồ sơ", 1),
                new StatusCount("Đã sàng lọc", 1),
                new StatusCount("Qua vòng nhân sự", 1),
                new StatusCount("Qua vòng chuyên môn", 1),
                new StatusCount("Đang phỏng vấn", 1),
                new StatusCount("Đã có thư mời", 1),
                new StatusCount("Đã nhận việc", 1),
                new StatusCount("Không tiếp tục", 1),
                new StatusCount("Other", 1)), result.applicationStatuses());
        assertEquals(at.toLocalDate(), result.recentApplications().getFirst().appliedDate());
        assertEquals("Đã nộp hồ sơ", result.recentApplications().getFirst().status());
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-09 | Candidate: nhãn offer, lương và phòng ban hiển thị đúng")
    void candidate_mapsOffersAndDepartment() {
        // Given: Candidate: nhãn offer, lương và phòng ban hiển thị đúng.
        user.setDepartment(Department.builder().departmentName("Engineering").build());
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.candidateOfferStatuses(7)).thenReturn(List.of(new StatusCount("Sent_Candidate", 1),
                new StatusCount("Accepted", 2), new StatusCount("Declined", 3), new StatusCount("Other", 4)));
        when(metrics.candidateOffers(7, 20)).thenReturn(List.of(new CandidateOfferRow(9, "Developer", new BigDecimal("1000"),
                LocalDate.of(2026, 11, 1), "Sent_Candidate", new BigDecimal("850"), "Office", "Benefits")));

        // When: Lấy dashboard của tài khoản hiện tại.
        CandidateDashboardResponse result = (CandidateDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals(List.of(new StatusCount("Chờ phản hồi", 1), new StatusCount("Đã chấp nhận", 2),
                new StatusCount("Đã từ chối", 3), new StatusCount("Other", 4)), result.offerStatuses());
        assertEquals(new BigDecimal("1000"), result.offers().getFirst().salary());
        assertEquals(new BigDecimal("850"), result.offers().getFirst().probationSalary());
        assertEquals("Chờ phản hồi", result.offers().getFirst().statusLabel());
        assertEquals("Engineering", result.profile().departmentName());
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-10 | Link phỏng vấn online Null: chỉ trả URL http/https hợp lệ")
    void candidate_onlineMeetingNull_returnsSafeLink() {
        // Given: Link phỏng vấn online Null: chỉ trả URL http/https hợp lệ.
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.candidateNextInterviews(eq(7), any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of(new CandidateInterviewRow("Developer", at, at.plusHours(1), "Online_GoogleMeet", null)));

        // When: Lấy dashboard của tài khoản hiện tại.
        CandidateDashboardResponse result = (CandidateDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Trực tuyến", result.upcomingInterviews().getFirst().formatLabel());
        assertEquals(null, result.upcomingInterviews().getFirst().meetingUrl());
        assertEquals(null, result.upcomingInterviews().getFirst().location());
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-11 | Link phỏng vấn online Blank: chỉ trả URL http/https hợp lệ")
    void candidate_onlineMeetingBlank_returnsSafeLink() {
        // Given: Link phỏng vấn online Blank: chỉ trả URL http/https hợp lệ.
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.candidateNextInterviews(eq(7), any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of(new CandidateInterviewRow("Developer", at, at.plusHours(1), "Online_GoogleMeet", "  ")));

        // When: Lấy dashboard của tài khoản hiện tại.
        CandidateDashboardResponse result = (CandidateDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Trực tuyến", result.upcomingInterviews().getFirst().formatLabel());
        assertEquals(null, result.upcomingInterviews().getFirst().meetingUrl());
        assertEquals(null, result.upcomingInterviews().getFirst().location());
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-12 | Link phỏng vấn online Https: chỉ trả URL http/https hợp lệ")
    void candidate_onlineMeetingHttps_returnsSafeLink() {
        // Given: Link phỏng vấn online Https: chỉ trả URL http/https hợp lệ.
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.candidateNextInterviews(eq(7), any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of(new CandidateInterviewRow("Developer", at, at.plusHours(1), "Online_GoogleMeet", " https://meet.example.test/room ")));

        // When: Lấy dashboard của tài khoản hiện tại.
        CandidateDashboardResponse result = (CandidateDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Trực tuyến", result.upcomingInterviews().getFirst().formatLabel());
        assertEquals("https://meet.example.test/room", result.upcomingInterviews().getFirst().meetingUrl());
        assertEquals(null, result.upcomingInterviews().getFirst().location());
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-13 | Link phỏng vấn online Http: chỉ trả URL http/https hợp lệ")
    void candidate_onlineMeetingHttp_returnsSafeLink() {
        // Given: Link phỏng vấn online Http: chỉ trả URL http/https hợp lệ.
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.candidateNextInterviews(eq(7), any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of(new CandidateInterviewRow("Developer", at, at.plusHours(1), "Online_GoogleMeet", "http://meet.example.test/room")));

        // When: Lấy dashboard của tài khoản hiện tại.
        CandidateDashboardResponse result = (CandidateDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Trực tuyến", result.upcomingInterviews().getFirst().formatLabel());
        assertEquals("http://meet.example.test/room", result.upcomingInterviews().getFirst().meetingUrl());
        assertEquals(null, result.upcomingInterviews().getFirst().location());
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-14 | Link phỏng vấn online WrongScheme: chỉ trả URL http/https hợp lệ")
    void candidate_onlineMeetingWrongScheme_returnsSafeLink() {
        // Given: Link phỏng vấn online WrongScheme: chỉ trả URL http/https hợp lệ.
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.candidateNextInterviews(eq(7), any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of(new CandidateInterviewRow("Developer", at, at.plusHours(1), "Online_GoogleMeet", "ftp://meet.example.test/room")));

        // When: Lấy dashboard của tài khoản hiện tại.
        CandidateDashboardResponse result = (CandidateDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Trực tuyến", result.upcomingInterviews().getFirst().formatLabel());
        assertEquals(null, result.upcomingInterviews().getFirst().meetingUrl());
        assertEquals(null, result.upcomingInterviews().getFirst().location());
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-15 | Link phỏng vấn online Script: chỉ trả URL http/https hợp lệ")
    void candidate_onlineMeetingScript_returnsSafeLink() {
        // Given: Link phỏng vấn online Script: chỉ trả URL http/https hợp lệ.
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.candidateNextInterviews(eq(7), any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of(new CandidateInterviewRow("Developer", at, at.plusHours(1), "Online_GoogleMeet", "javascript:alert(1)")));

        // When: Lấy dashboard của tài khoản hiện tại.
        CandidateDashboardResponse result = (CandidateDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Trực tuyến", result.upcomingInterviews().getFirst().formatLabel());
        assertEquals(null, result.upcomingInterviews().getFirst().meetingUrl());
        assertEquals(null, result.upcomingInterviews().getFirst().location());
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-16 | Link phỏng vấn online NoHost: chỉ trả URL http/https hợp lệ")
    void candidate_onlineMeetingNoHost_returnsSafeLink() {
        // Given: Link phỏng vấn online NoHost: chỉ trả URL http/https hợp lệ.
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.candidateNextInterviews(eq(7), any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of(new CandidateInterviewRow("Developer", at, at.plusHours(1), "Online_GoogleMeet", "https:/room")));

        // When: Lấy dashboard của tài khoản hiện tại.
        CandidateDashboardResponse result = (CandidateDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Trực tuyến", result.upcomingInterviews().getFirst().formatLabel());
        assertEquals(null, result.upcomingInterviews().getFirst().meetingUrl());
        assertEquals(null, result.upcomingInterviews().getFirst().location());
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-17 | Link phỏng vấn online UserInfo: chỉ trả URL http/https hợp lệ")
    void candidate_onlineMeetingUserInfo_returnsSafeLink() {
        // Given: Link phỏng vấn online UserInfo: chỉ trả URL http/https hợp lệ.
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.candidateNextInterviews(eq(7), any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of(new CandidateInterviewRow("Developer", at, at.plusHours(1), "Online_GoogleMeet", "https://user@meet.example.test/room")));

        // When: Lấy dashboard của tài khoản hiện tại.
        CandidateDashboardResponse result = (CandidateDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Trực tuyến", result.upcomingInterviews().getFirst().formatLabel());
        assertEquals(null, result.upcomingInterviews().getFirst().meetingUrl());
        assertEquals(null, result.upcomingInterviews().getFirst().location());
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-18 | Link phỏng vấn online Malformed: chỉ trả URL http/https hợp lệ")
    void candidate_onlineMeetingMalformed_returnsSafeLink() {
        // Given: Link phỏng vấn online Malformed: chỉ trả URL http/https hợp lệ.
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.candidateNextInterviews(eq(7), any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of(new CandidateInterviewRow("Developer", at, at.plusHours(1), "Online_GoogleMeet", "http://[invalid")));

        // When: Lấy dashboard của tài khoản hiện tại.
        CandidateDashboardResponse result = (CandidateDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Trực tuyến", result.upcomingInterviews().getFirst().formatLabel());
        assertEquals(null, result.upcomingInterviews().getFirst().meetingUrl());
        assertEquals(null, result.upcomingInterviews().getFirst().location());
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-19 | Phỏng vấn tại văn phòng: hiển thị địa điểm, không tạo meeting URL")
    void candidate_onsiteInterview_returnsLocationWithoutMeetingLink() {
        // Given: Phỏng vấn tại văn phòng: hiển thị địa điểm, không tạo meeting URL.
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.candidateNextInterviews(eq(7), any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of(new CandidateInterviewRow("Developer", at, at.plusHours(1), "Onsite", "Room A")));

        // When: Lấy dashboard của tài khoản hiện tại.
        CandidateDashboardResponse result = (CandidateDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals("Tại văn phòng", result.upcomingInterviews().getFirst().formatLabel());
        assertEquals("Room A", result.upcomingInterviews().getFirst().location());
        assertEquals(null, result.upcomingInterviews().getFirst().meetingUrl());
    }

    @Test
    @DisplayName("DASHBOARD-UNIT-20 | Admin: tổng số phòng ban không làm mất Active/Inactive hoặc trạng thái khác")
    void admin_departmentCounts_preserveActiveInactiveAndTotal() {
        // Given: Admin: tổng số phòng ban không làm mất Active/Inactive hoặc trạng thái khác.
        user.getRole().setRoleName("System Admin");
        when(users.findByUsernameIgnoreCase("dashboard")).thenReturn(Optional.of(user));
        when(metrics.departmentStatuses()).thenReturn(List.of(new StatusCount("Active", 2), new StatusCount("Inactive", 1), new StatusCount("Other", 4)));

        // When: Lấy dashboard của tài khoản hiện tại.
        AdminDashboardResponse result = (AdminDashboardResponse) service.forUsername("dashboard");

        // Then: Kiểm tra dữ liệu hiển thị và phạm vi truy vấn.
        assertEquals(7L, result.departments().totalCount());
        assertEquals(2L, result.departments().activeCount());
        assertEquals(1L, result.departments().inactiveCount());
        verify(monitoring, times(1)).rows();
    }

}

