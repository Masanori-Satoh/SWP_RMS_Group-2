package com.group2.rms.dashboard.internal.service;

import com.group2.rms.dashboard.internal.repository.DashboardListRepository;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.AttentionRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.ProgressRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.RequisitionRow;
import com.group2.rms.dashboard.internal.repository.InternalMetricsRepository;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.repository.RequisitionApprovalRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * INTEGRATION TEST (SQL Server thật, rollback sau mỗi test): JPQL của dashboard HM và trang /dashboard/internal
 * render thật với tài khoản HM riêng của test.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional
class HiringManagerDashboardIntegrationTests {

    @Autowired
    private DashboardListRepository lists;
    @Autowired
    private InternalMetricsRepository metrics;
    @Autowired
    private JobRequisitionRepository requisitions;
    @Autowired
    private RequisitionApprovalRepository approvals;
    @Autowired
    private DepartmentRepository departments;
    @Autowired
    private UserRepository users;
    @Autowired
    private RoleRepository roles;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Clock clock;

    private Department department;
    private User hiringManager;
    private User director;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now(clock);
        Role hmRole = roles.findByRoleName(RequisitionAccess.ROLE_HIRING_MANAGER).orElseThrow();
        Role directorRole = roles.findByRoleName(RequisitionAccess.ROLE_DIRECTOR).orElseThrow();
        department = departments.findAll().stream().findFirst().orElseThrow();
        // Tài khoản HM riêng của test để danh sách "của tôi" không lẫn dữ liệu seed.
        hiringManager = users.saveAndFlush(User.builder().username("dash_hm_it").passwordHash("$2a$10$dummy")
                .fullName("HM Dashboard Tester").email("dash_hm_it@rms.local").accountStatus("Active")
                .role(hmRole).department(department).build());
        director = users.saveAndFlush(User.builder().username("dash_dir_it").passwordHash("$2a$10$dummy")
                .fullName("Director Dashboard Tester").email("dash_dir_it@rms.local").accountStatus("Active")
                .role(directorRole).department(department).build());
    }

    private JobRequisition saveRequisition(String title, String status, LocalDateTime submittedAt) {
        return requisitions.saveAndFlush(JobRequisition.builder()
                .title(title).hiringManager(hiringManager).department(department).approvalStatus(status)
                .submittedAt(submittedAt).recruitmentRound(1).numberOfPositions(2).employmentType("Full-time")
                .gender("Any").workModel("On-site").jobDescription("Mô tả kiểm thử")
                .requirementDetails("Yêu cầu kiểm thử").screeningCriteria(new ArrayList<>()).build());
    }

    private void saveRejection(JobRequisition requisition, String comment, LocalDateTime at) {
        approvals.saveAndFlush(RequisitionApproval.builder().requisition(requisition).director(director)
                .status(RequisitionAccess.STATUS_REJECTED).comments(comment).approvalDate(at).build());
    }

    @Test
    @DisplayName("Cần xử lý: chỉ bị từ chối (kèm lý do mới nhất) và bản nháp, bị từ chối xếp trước")
    void requisitionsNeedingAttention_shouldReturnActionableRowsRejectedFirst() {
        // Arrange
        JobRequisition rejected = saveRequisition("Bị từ chối kiểm thử", RequisitionAccess.STATUS_REJECTED, now.minusDays(5));
        saveRejection(rejected, "Lý do cũ", now.minusDays(4));
        saveRejection(rejected, "Ngân sách chưa đủ — cần giảm số lượng", now.minusDays(1));
        // Bản nháp lưu sau nên mới cập nhật hơn, nhưng vẫn phải đứng sau yêu cầu bị từ chối
        JobRequisition draft = saveRequisition("Nháp kiểm thử", RequisitionAccess.STATUS_DRAFT, null);
        saveRequisition("Chờ kiểm thử", RequisitionAccess.STATUS_PENDING_DIRECTOR, now.minusDays(10));
        saveRequisition("Đã duyệt kiểm thử", RequisitionAccess.STATUS_APPROVED, now.minusDays(20));
        entityManager.clear();

        // Act
        List<AttentionRow> rows = lists.requisitionsNeedingAttention(hiringManager.getUserId());

        // Assert
        assertEquals(2, rows.size());
        assertEquals(rejected.getRequisitionId(), rows.get(0).requisitionId());
        assertEquals("Ngân sách chưa đủ — cần giảm số lượng", rows.get(0).rejectComment());
        assertEquals(draft.getRequisitionId(), rows.get(1).requisitionId());
    }

    @Test
    @DisplayName("Chờ duyệt: đếm đúng yêu cầu của HM, ngày gửi sớm nhất; không có yêu cầu chờ -> null")
    void pendingQueries_shouldReturnCountAndOldestSubmittedAt() {
        // Arrange
        assertNull(metrics.oldestOwnPendingSubmittedAt(hiringManager.getUserId()));
        saveRequisition("Chờ lâu kiểm thử", RequisitionAccess.STATUS_PENDING_DIRECTOR, now.minusDays(10));
        saveRequisition("Chờ mới kiểm thử", RequisitionAccess.STATUS_PENDING_DIRECTOR, now.minusDays(1));
        entityManager.clear();

        // Act
        long pending = metrics.ownPendingRequisitions(hiringManager.getUserId());
        LocalDateTime oldest = metrics.oldestOwnPendingSubmittedAt(hiringManager.getUserId());

        // Assert
        assertEquals(2, pending);
        assertNotNull(oldest);
        assertEquals(now.minusDays(10).toLocalDate(), oldest.toLocalDate());
    }

    @Test
    @DisplayName("Tiến độ theo vị trí: chỉ yêu cầu đang tuyển, chưa có hồ sơ -> 0 đã tuyển, đủ số cần tuyển")
    void hiringProgress_shouldReturnApprovedRequisitionsWithZeroCounts_whenNoApplications() {
        // Arrange
        JobRequisition approved = saveRequisition("Đang tuyển kiểm thử", RequisitionAccess.STATUS_APPROVED, now.minusDays(3));
        saveRequisition("Nháp không tính", RequisitionAccess.STATUS_DRAFT, null);
        entityManager.clear();

        // Act
        List<ProgressRow> rows = lists.hiringProgress(hiringManager.getUserId(), department.getDepartmentId());

        // Assert
        ProgressRow row = rows.stream().filter(item -> item.requisitionId() == approved.getRequisitionId())
                .findFirst().orElseThrow();
        assertEquals(2, row.numberOfPositions());
        assertEquals(0, row.hired());
        assertEquals(0, row.interviewing());
        assertEquals(0, row.offered());
        assertTrue(rows.stream().noneMatch(item -> item.title().equals("Nháp không tính")));
    }

    @Test
    @DisplayName("Yêu cầu cần theo dõi: chỉ của HM này, tối đa limit dòng, có số lượng và trạng thái")
    void recentRequisitions_shouldReturnOwnRowsWithinLimit() {
        // Arrange
        for (int i = 1; i <= 6; i++) {
            saveRequisition("Gần đây kiểm thử " + i, RequisitionAccess.STATUS_DRAFT, null);
        }
        entityManager.clear();

        // Act
        List<RequisitionRow> rows = lists.recentRequisitions(hiringManager.getUserId(),
                HiringManagerDashboardService.LIST_LIMIT);

        // Assert
        assertEquals(HiringManagerDashboardService.LIST_LIMIT, rows.size());
        assertTrue(rows.stream().allMatch(row -> row.title().startsWith("Gần đây kiểm thử")));
        assertEquals(2, rows.getFirst().numberOfPositions());
    }

    @Test
    @DisplayName("Số liệu phòng ban: câu đếm hồ sơ / phỏng vấn chạy được trên SQL Server; không có phòng ban -> 0 / rỗng")
    void departmentQueries_shouldRunOnSqlServer() {
        // Act + Assert
        assertTrue(metrics.departmentApplicationsBetween(hiringManager.getUserId(), department.getDepartmentId(),
                now.minusDays(30), now) >= 0);
        assertTrue(metrics.completedDepartmentInterviewsBetween(department.getDepartmentId(), now.minusDays(30), now) >= 0);
        assertEquals(0, metrics.completedDepartmentInterviewsBetween(null, now.minusDays(30), now));
        assertTrue(lists.departmentInterviews(null, now, now.plusDays(7), 5).isEmpty());
        lists.departmentInterviews(department.getDepartmentId(), now, now.plusDays(7), 5);
    }

    @Test
    @DisplayName("HM mở /dashboard/internal -> KPI có icon, Cần bạn xử lý có lý do từ chối, Tiến độ theo vị trí; không JavaScript biểu đồ")
    void dashboard_shouldRenderHiringManagerSections_whenHiringManagerLogsIn() throws Exception {
        // Arrange
        JobRequisition rejected = saveRequisition("Từ chối hiển thị", RequisitionAccess.STATUS_REJECTED, now.minusDays(2));
        saveRejection(rejected, "Cần bổ sung mô tả công việc", now.minusDays(1));
        saveRequisition("Vị trí đang tuyển hiển thị", RequisitionAccess.STATUS_APPROVED, now.minusDays(3));

        // Act
        String html = mvc.perform(get("/dashboard/internal").with(user(hiringManager.getUsername())
                        .authorities(new SimpleGrantedAuthority("ROLE_HIRING_MANAGER"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertTrue(html.contains("dash-kpi__icon"));
        assertTrue(java.util.regex.Pattern.compile("(?s)<svg\\b[^>]*class=\"dash-icon\"[^>]*>\\s*<path\\b[^>]*\\bd=\"[^\"]+\"")
                .matcher(html).find(), "Dashboard icons must render a non-empty SVG path");
        assertTrue(html.contains("Đã tuyển / cần tuyển"));
        assertTrue(html.contains("Cần bạn xử lý"));
        assertTrue(html.contains("Cần bổ sung mô tả công việc"));
        assertTrue(html.contains("/requisitions/edit/" + rejected.getRequisitionId()));
        assertTrue(html.contains("Tiến độ tuyển theo vị trí"));
        assertTrue(html.contains("Vị trí đang tuyển hiển thị"));
        assertTrue(html.contains("Yêu cầu cần theo dõi"));
        assertFalse(html.contains("Pipeline ứng viên"));
        assertFalse(html.contains("chart.umd.min.js"));
        assertFalse(html.contains("recruitment-dashboard.js"));
    }
}
