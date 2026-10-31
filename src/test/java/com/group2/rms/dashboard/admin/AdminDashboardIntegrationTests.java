package com.group2.rms.dashboard.admin;

import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.dashboard.admin.service.AdminDashboardService;
import com.group2.rms.dashboard.admin.dto.AdminDashboardResponse;
import com.group2.rms.dashboard.shared.DashboardMetricsRepository;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.AccountListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.ZoneId;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.doReturn;

/** SELECT-only verification against the configured SQL Server; never updates schema or account data. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@Transactional(readOnly = true)
class AdminDashboardIntegrationTests {
    @Autowired com.group2.rms.admin.service.AuditLogService audit;
    @Autowired com.group2.rms.admin.repository.AuditLogTargetRepository auditTargets;
    @MockitoSpyBean AdminDashboardService dashboard;
    @Autowired DashboardMetricsRepository metrics;
    @Autowired UserRepository users;
    @MockitoSpyBean AccountListService accounts;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    private User admin;

    @Test void auditLogQueriesMatchExistingDatabaseRowsWithoutWrites() {
        var page = audit.search(null, null, null, "", "", "", 0);
        assertEquals(jdbc.queryForObject("SELECT COUNT_BIG(*) FROM AuditLog", Long.class), page.getTotalElements());
        var actors = audit.actors();
        assertEquals(jdbc.queryForObject("SELECT COUNT(DISTINCT UserId) FROM AuditLog", Integer.class), actors.size());
        assertEquals(jdbc.queryForObject("SELECT COUNT(DISTINCT EntityName) FROM AuditLog", Integer.class), audit.entities().size());
        if (!actors.isEmpty()) {
            int actorId = actors.getFirst().getId();
            assertEquals(jdbc.queryForObject("SELECT COUNT_BIG(*) FROM AuditLog WHERE UserId=?", Long.class, actorId),
                    audit.search(null, null, actorId, "", "", "", 0).getTotalElements());
        }
        if (!page.isEmpty()) {
            var day = page.getContent().getFirst().timestamp().toLocalDate();
            assertEquals(jdbc.queryForObject("SELECT COUNT_BIG(*) FROM AuditLog WHERE [Timestamp]>=? AND [Timestamp]<?",
                    Long.class, Timestamp.valueOf(day.atStartOfDay()), Timestamp.valueOf(day.plusDays(1).atStartOfDay())),
                    audit.search(day, day, null, "", "", "", 0).getTotalElements());
        }
    }

    @Test void auditTargetNamesResolveAcrossExistingEntityTypesWithoutWrites() {
        var tables = java.util.Map.of("User", "UserId", "Department", "DepartmentId", "Candidate", "CandidateId",
                "Application", "ApplicationId", "JobRequisition", "RequisitionId", "JobPosting", "JobPostingId",
                "TalentPoolNote", "NoteId", "InterviewSchedule", "InterviewId", "SystemConfig", "ConfigKey");
        var fixtures = new java.util.ArrayList<com.group2.rms.admin.entity.AuditLog>();
        var existing = new java.util.HashSet<com.group2.rms.admin.repository.AuditLogTargetRepository.Target>();
        tables.forEach((table, column) -> {
            var ids = jdbc.queryForList("SELECT TOP 1 " + column + " FROM [" + table + "] ORDER BY " + column, String.class);
            String id = ids.isEmpty() ? (table.equals("SystemConfig") ? "__missing__" : "2147483647") : ids.getFirst();
            fixtures.add(com.group2.rms.admin.entity.AuditLog.builder().entityName(table).entityId(id).build());
            if (!ids.isEmpty()) existing.add(new com.group2.rms.admin.repository.AuditLogTargetRepository.Target(table, id));
        });
        var names = auditTargets.findNames(fixtures);
        for (var target : existing) assertNotNull(names.get(target), target.toString());
        var app = fixtures.stream().filter(log -> log.getEntityName().equals("Application")).findFirst().orElseThrow();
        if (existing.contains(new com.group2.rms.admin.repository.AuditLogTargetRepository.Target("Application", app.getEntityId()))) {
            String expected = jdbc.queryForObject("SELECT CONCAT(u.FullName,N' · ',j.PostingTitle) FROM Application a "
                    + "JOIN Candidate c ON c.CandidateId=a.CandidateId JOIN [User] u ON u.UserId=c.UserId "
                    + "JOIN JobPosting j ON j.JobPostingId=a.JobPostingId WHERE a.ApplicationId=?", String.class, Integer.valueOf(app.getEntityId()));
            assertEquals(expected, names.get(new com.group2.rms.admin.repository.AuditLogTargetRepository.Target("Application", app.getEntityId())));
        }
    }

    @BeforeEach
    void currentAdmin() {
        admin = users.findAllWithRoleAndDepartment().stream()
                .filter(account -> "System Admin".equals(account.getRole().getRoleName())
                        && "Active".equals(account.getAccountStatus())).findFirst().orElse(null);
        assumeTrue(admin != null, "Configured database needs an active System Admin for integration verification");
    }

    @Test
    void dashboardAggregatesMatchIndependentSqlAndActualFilteredLists() {
        var response = (AdminDashboardResponse) dashboard.forUsername(admin.getUsername());
        for (var summary : response.accountSummaries()) {
            boolean candidate = "/admin/candidate-accounts".equals(summary.url());
            for (String status : List.of("Active", "Inactive")) {
                long filtered = candidate
                        ? accounts.findCandidateAccounts("", status, "name", 0).getTotalElements()
                        : accounts.findAccounts("", null, null, status, "name", 0).getTotalElements();
                long metric = switch (status) {
                    case "Active" -> summary.active();
                    default -> summary.inactive();
                };
                assertEquals(filtered, metric, summary.title() + " " + status);
            }
            assertEquals(summary.total(), summary.active() + summary.inactive());
        }
        for (var role : response.roleCounts()) {
            long sqlCount = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM [User] WHERE RoleId=?", Long.class, role.roleId());
            assertEquals(sqlCount, role.count());
            assertEquals(sqlCount, accounts.findAccounts("", role.roleId(), null, "", "name", 0).getTotalElements());
        }
        assertEquals(response.internalTotal(), response.roleCounts().stream().mapToLong(row -> row.count()).sum());
        assertEquals(metrics.internalAccountsByRole(), response.roleCounts());
    }

    @Test
    void candidateProfilesAndRollingWindowMatchSqlWithoutApplicationDuplication() {
        var summary = ((AdminDashboardResponse) dashboard.forUsername(admin.getUsername())).candidates();
        assertEquals(jdbc.queryForObject("SELECT COUNT_BIG(*) FROM Candidate", Long.class), summary.total());
        assertEquals(jdbc.queryForObject("SELECT COUNT_BIG(DISTINCT c.CandidateId) FROM Candidate c "
                + "LEFT JOIN Application a ON a.CandidateId=c.CandidateId", Long.class), summary.total());
        assertEquals(Duration.ofDays(30), Duration.between(summary.from(), summary.to()));
        assertEquals(jdbc.queryForObject("SELECT COUNT_BIG(*) FROM Candidate WHERE CreatedAt >= ? AND CreatedAt <= ?",
                Long.class, Timestamp.valueOf(summary.from()), Timestamp.valueOf(summary.to())), summary.newLast30Days());
        int jvmOffsetMinutes = summary.to().atZone(ZoneId.of(summary.timeZone())).getOffset().getTotalSeconds() / 60;
        assertEquals(jdbc.queryForObject("SELECT DATEPART(TZOFFSET, SYSDATETIMEOFFSET())", Integer.class), jvmOffsetMinutes,
                "Candidate DATETIME2 assumes the existing JVM and SQL Server local time conventions agree");

        Timestamp first = jdbc.queryForObject("SELECT MIN(CreatedAt) FROM Candidate", Timestamp.class);
        if (first != null) {
            var at = first.toLocalDateTime();
            assertEquals(jdbc.queryForObject("SELECT COUNT_BIG(*) FROM Candidate WHERE CreatedAt=?", Long.class, first),
                    metrics.candidatesCreatedBetween(at, at), "Both time boundaries are inclusive");
            assertEquals(0, metrics.candidatesCreatedBetween(at.minusDays(1), at.minusSeconds(1)),
                    "A window before the first profile returns zero");
        }
    }

    @Test
    void renderedDashboardLinksUseStoredIdsAndEveryStatusHasCorrectDestination() throws Exception {
        String html = mvc.perform(get("/dashboard/admin").with(user(admin.getUsername())
                        .authorities(new SimpleGrantedAuthority(RoleAuthorities.SYSTEM_ADMIN))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (String status : List.of("Active", "Inactive")) {
            assertTrue(html.contains("/admin/accounts?status=" + status + "&amp;page=0"));
            assertTrue(html.contains("/admin/candidate-accounts?status=" + status + "&amp;page=0"));
        }
        for (var role : metrics.internalAccountsByRole()) {
            assertTrue(html.contains("/admin/accounts?roleId=" + role.roleId() + "&amp;page=0"));
        }
        assertTrue(html.contains("/js/vendor/chart.umd.min.js"));
        assertTrue(html.contains("/js/pages/admin-dashboard.js"));
        assertTrue(html.contains("data-chart-point"));
    }

    @Test
    void hrAndHiringManagerRenderTheirOwnDashboardsWithoutAdminSummary() throws Exception {
        for (String roleName : List.of("HR", "Hiring Manager")) {
            var actor = users.findAllWithRoleAndDepartment().stream()
                    .filter(user -> roleName.equals(user.getRole().getRoleName()) && "Active".equals(user.getAccountStatus()))
                    .findFirst().orElseThrow();
            String authority = roleName.equals("HR") ? "ROLE_HR" : "ROLE_HIRING_MANAGER";
            String html = mvc.perform(get("/dashboard/internal").with(user(actor.getUsername())
                            .authorities(new SimpleGrantedAuthority(authority))))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertFalse(html.contains("data-summary="));
            assertFalse(html.contains("/js/pages/admin-dashboard.js"));
        }
    }

    @Test
    void iconActionsPreserveEditAndStatusPostContractsForEveryAccountState() throws Exception {
        for (String state : List.of("Active", "Inactive")) {
            boolean inactive = "Inactive".equals(state);
            String operation = inactive ? "activate" : "deactivate";
            String label = inactive ? "Kích hoạt lại" : "Vô hiệu hóa";
            var row = new AccountListService.AccountRow(admin.getUserId(), admin.getFullName(), admin.getEmail(),
                    admin.getUsername(), admin.getRole().getRoleName(), null, state);
            doReturn(new org.springframework.data.domain.PageImpl<>(List.of(row),
                    org.springframework.data.domain.PageRequest.of(0, 10), 1))
                    .when(accounts).findAccounts("", null, null, "", "name", 0);
            var result = mvc.perform(get("/admin/accounts").with(user(admin.getUsername())
                            .authorities(new SimpleGrantedAuthority(RoleAuthorities.SYSTEM_ADMIN))))
                    .andExpect(status().isOk()).andReturn();
            String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            String body = html.substring(html.indexOf("<tbody>"), html.indexOf("</tbody>"));
            assertTrue(body.contains("href=\"/admin/accounts/" + admin.getUserId() + "/edit\""));
            var form = java.util.regex.Pattern.compile("<form\\b[^>]*data-status-confirm[^>]*>.*?</form>",
                    java.util.regex.Pattern.DOTALL).matcher(body);
            assertTrue(form.find());
            String action = form.group();
            assertFalse(form.find(), "Exactly one status action per account");
            assertTrue(action.contains("action=\"/admin/accounts/" + admin.getUserId() + "/" + operation + "\""));
            assertTrue(action.contains("method=\"post\""));
            assertTrue(action.contains("name=\"_csrf\""));
            assertTrue(action.contains("data-tooltip=\"" + label + "\""));
            assertTrue(action.contains("data-confirm-label=\"" + label + "\""));
            assertTrue(action.contains("data-confirm-danger=\"" + !inactive + "\""));
            assertEquals(2L, java.util.regex.Pattern.compile("<svg\\b").matcher(body).results().count());
            assertTrue(html.contains("id=\"status-dialog\""));
            // HTML response fixture only: never writes an account status to SQL Server.
            Files.writeString(Path.of("target/internal-icons-" + state.toLowerCase(java.util.Locale.ROOT) + ".html"),
                    html, StandardCharsets.UTF_8);
        }
    }

    @Test
    void destinationsSelectFiltersAndPaginationKeepsThem() throws Exception {
        for (String status : List.of("Active", "Inactive")) {
            mvc.perform(get("/admin/accounts").param("status", status)
                    .with(user(admin.getUsername()).authorities(new SimpleGrantedAuthority(RoleAuthorities.SYSTEM_ADMIN))))
                    .andExpect(status().isOk()).andExpect(model().attribute("selectedStatus", status));
            mvc.perform(get("/admin/candidate-accounts").param("status", status)
                    .with(user(admin.getUsername()).authorities(new SimpleGrantedAuthority(RoleAuthorities.SYSTEM_ADMIN))))
                    .andExpect(status().isOk()).andExpect(model().attribute("selectedStatus", status));
        }
        for (var role : metrics.internalAccountsByRole()) {
            mvc.perform(get("/admin/accounts").param("roleId", role.roleId().toString())
                    .with(user(admin.getUsername()).authorities(new SimpleGrantedAuthority(RoleAuthorities.SYSTEM_ADMIN))))
                    .andExpect(status().isOk()).andExpect(model().attribute("selectedRoleId", role.roleId()));
        }
        for (String route : List.of("/admin/accounts", "/admin/candidate-accounts")) {
            String html = mvc.perform(get(route).queryParam("status", "Active").queryParam("sort", "newest")
                    .with(user(admin.getUsername()).authorities(new SimpleGrantedAuthority(RoleAuthorities.SYSTEM_ADMIN))))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            long total = route.endsWith("candidate-accounts")
                    ? accounts.findCandidateAccounts("", "Active", "newest", 0).getTotalElements()
                    : accounts.findAccounts("", null, null, "Active", "newest", 0).getTotalElements();
            if (total > 10) assertTrue(html.contains("status=Active&amp;sort=newest&amp;page=1"));
        }
    }

    @Test
    void emptyDashboardAndZeroRolesKeepRealIdsAndAccessibleLinks() throws Exception {
        var actual = (AdminDashboardResponse) dashboard.forUsername(admin.getUsername());
        var empty = new AdminDashboardResponse(actual.roleName(), actual.fullName(), actual.scope(),
                actual.accountSummaries().stream().map(row -> new AdminDashboardResponse.AccountSummary(
                        row.title(), row.url(), 0, 0, 0)).toList(),
                actual.departments(), actual.breakdowns(), actual.shortcuts(),
                actual.roleCounts().stream().map(row -> new DashboardMetricsRepository.RoleCount(
                        row.roleId(), row.roleName(), 0)).toList(), List.of(),
                new AdminDashboardResponse.CandidateSummary(0, 0, actual.candidates().from(),
                        actual.candidates().to(), actual.candidates().timeZone()));
        doReturn(empty).when(dashboard).forUsername(admin.getUsername());
        String html = mvc.perform(get("/dashboard/admin").with(user(admin.getUsername())
                        .authorities(new SimpleGrantedAuthority(RoleAuthorities.SYSTEM_ADMIN))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        for (var role : actual.roleCounts()) {
            assertTrue(html.contains("/admin/accounts?roleId=" + role.roleId() + "&amp;page=0"));
        }
        assertTrue(html.contains("data-count=\"0\""));
        assertTrue(html.contains("Nhóm này chưa có tài khoản"));
        // Rendered test fixture only: never served by the application or written to SQL Server.
        Files.writeString(Path.of("target/admin-analytics-empty.html"), html, StandardCharsets.UTF_8);
    }
}
