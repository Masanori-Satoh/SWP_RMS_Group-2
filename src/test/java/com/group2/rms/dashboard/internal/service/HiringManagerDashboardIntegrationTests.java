package com.group2.rms.dashboard.internal.service;

import com.group2.rms.dashboard.internal.repository.DashboardListRepository;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.AttentionRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.ProgressRow;
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
                .requirementDetails("Yêu cầu kiểm thử").build());
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
    @DisplayName("Chờ duyệt: đếm mọi yêu cầu chờ duyệt trong phạm vi HM, khớp danh sách /requisitions")
    void countPendingRequisitionsInScope_shouldCountWholeDepartment_likeRequisitionList() {
        // Arrange
        long before = metrics.countPendingRequisitionsInScope(hiringManager.getUserId(), department.getDepartmentId());
        saveRequisition("Chờ lâu kiểm thử", RequisitionAccess.STATUS_PENDING_DIRECTOR, now.minusDays(10));
        saveRequisition("Chờ mới kiểm thử", RequisitionAccess.STATUS_PENDING_DIRECTOR, now.minusDays(1));
        JobRequisition colleague = saveRequisition("Của người khác cùng phòng", RequisitionAccess.STATUS_PENDING_DIRECTOR,
                now.minusDays(2));
        colleague.setHiringManager(director);
        saveRequisition("Nháp không tính", RequisitionAccess.STATUS_DRAFT, null);
        entityManager.flush();
        entityManager.clear();

        // Act
        long pending = metrics.countPendingRequisitionsInScope(hiringManager.getUserId(), department.getDepartmentId());

        // Assert: cả yêu cầu của người khác trong phòng, giống màn /requisitions của HM
        assertEquals(before + 3, pending);
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
    @DisplayName("Số liệu phòng ban: câu đếm tin đã đăng / phỏng vấn chạy được trên SQL Server; không có phòng ban -> 0 / rỗng")
    void departmentQueries_shouldRunOnSqlServer() {
        // Act + Assert
        int outsider = -1;
        assertTrue(metrics.countPostedDepartmentRequisitions(hiringManager.getUserId(), department.getDepartmentId()) >= 0);
        assertTrue(metrics.countUpcomingDepartmentInterviews(outsider, department.getDepartmentId(), now, now.plusDays(7)) >= 0);
        assertEquals(0, metrics.countUpcomingDepartmentInterviews(outsider, null, now, now.plusDays(7)));
        assertTrue(lists.departmentInterviews(outsider, null, now, now.plusDays(7), 5).isEmpty());
        lists.departmentInterviews(outsider, department.getDepartmentId(), now, now.plusDays(7), 5);
    }

    @Test
    @DisplayName("HM là trưởng phòng nhưng tài khoản chưa gán phòng -> Vẫn thấy lịch phỏng vấn của phòng mình quản lý")
    void departmentInterviews_shouldIncludeManagedDepartment_whenHiringManagerHasNoDepartment() {
        // Arrange
        hiringManager.setDepartment(null);
        department.setManager(hiringManager);
        entityManager.flush();
        int outsider = -1;
        LocalDateTime to = now.plusDays(30);

        // Act
        long managedCount = metrics.countUpcomingDepartmentInterviews(hiringManager.getUserId(), null, now, to);
        int managedRows = lists.departmentInterviews(hiringManager.getUserId(), null, now, to, 100).size();

        // Assert: bằng đúng số của phòng đó khi tra theo id phòng
        assertEquals(metrics.countUpcomingDepartmentInterviews(outsider, department.getDepartmentId(), now, to), managedCount);
        assertEquals(lists.departmentInterviews(outsider, department.getDepartmentId(), now, to, 100).size(), managedRows);
    }

    @Test
    @DisplayName("HM mở /dashboard/internal -> KPI có icon, Việc cần xử lý có lý do từ chối, Tiến độ theo vị trí vẽ bằng Chart.js")
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
        assertTrue(html.contains("Yêu cầu chờ Giám đốc duyệt"));
        assertTrue(html.contains("Yêu cầu đã đăng tuyển"));
        assertTrue(html.contains("Buổi phỏng vấn sắp tới"));
        assertFalse(html.contains("Vị trí thiếu ứng viên"));
        assertTrue(html.contains("Việc cần xử lý"));
        assertTrue(html.contains("Cần bổ sung mô tả công việc"));
        assertTrue(html.contains("/requisitions/edit/" + rejected.getRequisitionId()));
        assertTrue(html.contains("Tiến độ tuyển theo vị trí"));
        assertTrue(html.contains("Vị trí đang tuyển hiển thị"));
        assertFalse(html.contains("Yêu cầu cần theo dõi"));
        assertFalse(html.contains("Pipeline ứng viên"));
        assertTrue(html.contains("data-chart=\"progress\""));
        assertTrue(html.contains("chart.umd.min.js"));
        assertTrue(html.contains("internal-dashboard.js"));
        assertFalse(html.contains("recruitment-dashboard.js"));
    }
}
