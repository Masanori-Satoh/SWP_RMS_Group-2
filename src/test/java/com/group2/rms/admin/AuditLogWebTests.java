package com.group2.rms.admin;

import com.group2.rms.admin.controller.AuditLogController;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.admin.service.AuditLogService;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditLogController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class AuditLogWebTests {
    @Autowired MockMvc mvc;
    @MockitoBean AuditLogService audit;
    @MockitoBean UserRepository users;

    @BeforeEach void admin() {
        when(users.findByUsernameIgnoreCase("audit-admin")).thenReturn(Optional.of(User.builder()
                .userId(1).username("audit-admin").fullName("Trần Minh Đức").email("admin@example.test")
                .role(Role.builder().roleId(1).roleName("System Admin").build()).accountStatus("Active").build()));
    }

    @Test void rendersFiltersEscapesValuesAndKeepsFiltersOnPageLinks() throws Exception {
        LocalDate from = LocalDate.of(2026, 10, 1), to = LocalDate.of(2026, 10, 31);
        var at = LocalDateTime.of(2026, 10, 30, 10, 45, 12);
        var rows = List.of(new AuditLogService.Row(at, "Trần Minh Đức", "UPDATE", "User", "18",
                "Vai trò: Interviewer\nPhòng ban: Engineering", "Vai trò: HR\nPhòng ban: Human Resources"),
                new AuditLogService.Row(at.minusHours(1), "Nguyễn Thị Lan", "CREATE", "Department", "5", null, "Tên: Engineering"),
                new AuditLogService.Row(at.minusDays(1), "Trần Minh Đức", "DELETE", "TalentPoolNote", "23", "<script>alert(1)</script>", null));
        when(audit.search(from, to, 1, "UPDATE", "User", "18", 0))
                .thenReturn(new PageImpl<>(rows, PageRequest.of(0, 10), 120));
        AuditLogRepository.ActorChoice actor = mock(AuditLogRepository.ActorChoice.class);
        when(actor.getId()).thenReturn(1); when(actor.getName()).thenReturn("Trần Minh Đức");
        when(audit.actors()).thenReturn(List.of(actor));
        when(audit.entities()).thenReturn(List.of("User", "Department", "TalentPoolNote"));
        String html = mvc.perform(get("/admin/audit-logs?from=2026-10-01&to=2026-10-31&actorId=1&action=UPDATE&entity=User&search=18")
                        .with(user("audit-admin").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html.contains("Lịch sử hoạt động hệ thống"));
        assertTrue(html.contains("Tổng 120 bản ghi"));
        assertTrue(html.contains("30/10/2026 10:45:12"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(html.contains("<script>alert(1)</script>"));
        assertFalse(html.contains("Địa chỉ IP"));
        assertFalse(html.contains("name=\"size\""));
        assertTrue(html.contains("Nội dung cũ"));
        assertTrue(html.contains("Nội dung mới"));
        assertTrue(html.contains("Cập nhật"));
        assertTrue(html.contains("Ghi chú ứng viên"));
        assertTrue(html.contains("aul-avatar"));
        assertTrue(html.contains("value=\"UPDATE\" selected=\"selected\""));
        assertTrue(html.contains("value=\"User\" selected=\"selected\""));
        assertTrue(html.contains("entity=User&amp;search=18&amp;page=1"));
        if (Boolean.getBoolean("audit.ui.snapshots")) {
            Files.createDirectories(Path.of("target/audit-ui"));
            Files.writeString(Path.of("target/audit-ui/populated.html"), html, StandardCharsets.UTF_8);
        }
    }

    @Test void emptyPageOffersUsefulFeedback() throws Exception {
        when(audit.search(null, null, null, "", "", "", 0)).thenReturn(org.springframework.data.domain.Page.empty());
        String html = mvc.perform(get("/admin/audit-logs").with(user("audit-admin").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html.contains("Chưa có hoạt động phù hợp"));
        assertFalse(html.contains("class=\"pagination-wrapper\""));
        if (Boolean.getBoolean("audit.ui.snapshots")) {
            Files.createDirectories(Path.of("target/audit-ui"));
            Files.writeString(Path.of("target/audit-ui/empty.html"), html);
        }
    }

    @Test void localizesStoredTalentPoolCodesWhileKeepingFilterValues() throws Exception {
        var at = LocalDateTime.of(2026, 10, 30, 21, 53, 36);
        var rows = List.of(new AuditLogService.Row(at, "Lê Thị Mai Lan", "CREATE", "Application", "1226", "135", "TalentPoolHrRecommended"),
                new AuditLogService.Row(at, "Lê Thị Mai Lan", "CREATE", "Application", "1225", "139", "TalentPoolHrRecommended"),
                new AuditLogService.Row(at.minusHours(5), "Lê Thị Mai Lan", "UPDATE", "Application", "1223", "HM_Suggested", "TalentPoolCancelled"),
                new AuditLogService.Row(at.minusHours(5).minusSeconds(5), "Lê Thị Mai Lan", "UPDATE", "Application", "1222", "HM_Suggested", "TalentPoolHrWithdrawn"),
                new AuditLogService.Row(at.minusDays(2), "Vũ Anh Tuấn", "UPDATE", "Application", "221", null, "TalentPoolSuggested"),
                new AuditLogService.Row(at.minusDays(5), "Vũ Anh Tuấn", "CREATE", "Application", "218", "129", "TalentPoolHrRecommended"),
                new AuditLogService.Row(at.minusDays(23), "Đỗ Minh Khánh", "CREATE", "JobRequisition", "19", null, "Tạo yêu cầu: Chuyên viên Kiểm toán Nội bộ"),
                new AuditLogService.Row(at.minusDays(24), "Nguyễn Hoài Anh", "UPDATE", "Department", "12", "Tên phòng ban: Marketing", "Tên phòng ban: Marketing & Communication"),
                new AuditLogService.Row(at.minusDays(26), "Trần Văn Minh", "DELETE", "TalentPoolNote", "11", "Ứng viên #8\nCó kinh nghiệm quản lý nhóm", null),
                new AuditLogService.Row(at.minusDays(27), "Lê Thị Mai Lan", "LOGIN", "User", "5", null, "User logged in successfully"));
        when(audit.search(null, null, null, "", "", "", 0)).thenReturn(new PageImpl<>(rows, PageRequest.of(0, 10), 70));
        when(audit.entities()).thenReturn(List.of("Application", "Department", "JobRequisition", "TalentPoolNote", "User"));
        String html = mvc.perform(get("/admin/audit-logs").with(user("audit-admin").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html.contains("Trạng thái: HR đã gửi gợi ý"));
        assertTrue(html.contains("Trạng thái: HR đã rút lại gợi ý"));
        assertTrue(html.contains("Hồ sơ nguồn: #135"));
        assertTrue(html.contains("Hồ sơ ứng tuyển"));
        assertTrue(html.contains("value=\"Application\""));
        assertFalse(html.contains("TalentPoolHrRecommended"));
        assertFalse(html.contains("HM_Suggested"));
        if (Boolean.getBoolean("audit.ui.snapshots")) {
            Files.createDirectories(Path.of("target/audit-ui"));
            Files.writeString(Path.of("target/audit-ui/readable.html"), html, StandardCharsets.UTF_8);
        }
    }

    @Test void reversedDateRangeShowsErrorWithoutSearching() throws Exception {
        mvc.perform(get("/admin/audit-logs?from=2026-10-31&to=2026-10-01").with(user("audit-admin").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk()).andExpect(model().attribute("filterError", "Từ ngày phải trước hoặc bằng đến ngày."));
        verify(audit, never()).search(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test void otherRolesCannotReadAuditLogs() throws Exception {
        for (String role : List.of("HR", "HIRING_MANAGER", "DIRECTOR", "INTERVIEWER", "CANDIDATE")) {
            String name = switch (role) { case "HIRING_MANAGER" -> "Hiring Manager"; case "DIRECTOR" -> "Director";
                case "INTERVIEWER" -> "Interviewer"; case "CANDIDATE" -> "Candidate"; default -> "HR"; };
            when(users.findByUsernameIgnoreCase("other")).thenReturn(Optional.of(User.builder()
                    .userId(2).username("other").accountStatus("Active").role(Role.builder().roleName(name).build()).build()));
            mvc.perform(get("/admin/audit-logs").with(user("other").roles(role))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(audit);
    }

    @Test void anonymousUserMustLogIn() throws Exception {
        mvc.perform(get("/admin/audit-logs")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(audit);
    }
}
