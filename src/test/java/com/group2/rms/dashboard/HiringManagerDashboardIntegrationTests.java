package com.group2.rms.dashboard;

import com.group2.rms.dashboard.DashboardListRepository.AttentionRow;
import com.group2.rms.dashboard.DashboardListRepository.RequisitionRow;
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
 * INTEGRATION TEST (SQL Server thật, rollback sau mỗi test): JPQL của dashboard HM và trang /dashboard render thật.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional
class HiringManagerDashboardIntegrationTests {

    @Autowired
    private DashboardListRepository lists;
    @Autowired
    private DashboardMetricsRepository metrics;
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
    @DisplayName("Cần xử lý: chỉ bị từ chối (kèm lý do mới nhất) và bản nháp, bị từ chối xếp trước; KPI đếm riêng yêu cầu chờ ≥ 7 ngày")
    void requisitionsNeedingAttention_shouldReturnActionableRowsRejectedFirst() {
        // Arrange
        JobRequisition rejected = saveRequisition("Bị từ chối kiểm thử", RequisitionAccess.STATUS_REJECTED, now.minusDays(5));
        saveRejection(rejected, "Lý do cũ", now.minusDays(4));
        saveRejection(rejected, "Ngân sách chưa đủ — cần giảm số lượng", now.minusDays(1));
        // Bản nháp lưu sau nên mới cập nhật hơn, nhưng vẫn phải đứng sau yêu cầu bị từ chối
        JobRequisition draft = saveRequisition("Nháp kiểm thử", RequisitionAccess.STATUS_DRAFT, null);
        saveRequisition("Chờ lâu kiểm thử", RequisitionAccess.STATUS_PENDING_DIRECTOR, now.minusDays(10));
        saveRequisition("Chờ mới kiểm thử", RequisitionAccess.STATUS_PENDING_DIRECTOR, now.minusDays(1));
        saveRequisition("Đã duyệt kiểm thử", RequisitionAccess.STATUS_APPROVED, now.minusDays(20));
        entityManager.clear();
        LocalDateTime staleBefore = now.toLocalDate().minusDays(DashboardLabels.WAIT_DANGER_DAYS - 1L).atStartOfDay();

        // Act
        List<AttentionRow> rows = lists.requisitionsNeedingAttention(hiringManager.getUserId());
        long stalePending = metrics.ownStalePendingRequisitions(hiringManager.getUserId(), staleBefore);

        // Assert
        assertEquals(2, rows.size());
        assertEquals(rejected.getRequisitionId(), rows.get(0).requisitionId());
        assertEquals("Ngân sách chưa đủ — cần giảm số lượng", rows.get(0).rejectComment());
        assertEquals(draft.getRequisitionId(), rows.get(1).requisitionId());
        assertEquals(1, stalePending);
    }

    @Test
    @DisplayName("Yêu cầu gần đây: chỉ của HM này, tối đa limit dòng, có số lượng và trạng thái")
    void recentRequisitions_shouldReturnOwnRowsWithinLimit() {
        // Arrange
        for (int i = 1; i <= 6; i++) {
            saveRequisition("Gần đây kiểm thử " + i, RequisitionAccess.STATUS_DRAFT, null);
        }
        entityManager.clear();

        // Act
        List<RequisitionRow> rows = lists.recentRequisitions(hiringManager.getUserId(),
                HiringManagerDashboardService.RECENT_LIMIT);

        // Assert
        assertEquals(HiringManagerDashboardService.RECENT_LIMIT, rows.size());
        assertTrue(rows.stream().allMatch(row -> row.title().startsWith("Gần đây kiểm thử")));
        assertEquals(2, rows.getFirst().numberOfPositions());
        assertEquals(RequisitionAccess.STATUS_DRAFT, rows.getFirst().approvalStatus());
    }

    @Test
    @DisplayName("Lịch phỏng vấn phòng ban: câu đếm và câu danh sách chạy được trên SQL Server, không có phòng ban -> rỗng")
    void departmentInterviewQueries_shouldRunOnSqlServer() {
        // Act + Assert
        long count = metrics.interviewsForDepartmentBetween(department.getDepartmentId(), now, now.plusDays(7));
        int listed = lists.departmentInterviews(department.getDepartmentId(), now, now.plusDays(7), 50).size();
        assertTrue(count >= listed);
        assertEquals(0, metrics.interviewsForDepartmentBetween(null, now, now.plusDays(7)));
        assertTrue(lists.departmentInterviews(null, now, now.plusDays(7), 5).isEmpty());
    }

    @Test
    @DisplayName("HM mở /dashboard -> trang mới (KPI, Cần xử lý có lý do từ chối, biểu đồ thanh), không nạp Chart.js")
    void dashboard_shouldRenderHiringManagerSections_whenHiringManagerLogsIn() throws Exception {
        // Arrange
        JobRequisition rejected = saveRequisition("Từ chối hiển thị", RequisitionAccess.STATUS_REJECTED, now.minusDays(2));
        saveRejection(rejected, "Cần bổ sung mô tả công việc", now.minusDays(1));

        // Act
        String html = mvc.perform(get("/dashboard").with(user(hiringManager.getUsername())
                        .authorities(new SimpleGrantedAuthority("ROLE_HIRING_MANAGER"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertTrue(html.contains("Tổng quan tuyển dụng của tôi"));
        assertTrue(html.contains("Hồ sơ ứng tuyển"));
        assertTrue(html.contains("Ưu tiên cao"));
        assertFalse(html.contains("<th scope=\"row\""));
        assertFalse(html.contains("/requisitions/create"));
        assertTrue(html.contains("Cần bổ sung mô tả công việc"));
        assertTrue(html.contains("/requisitions/edit/" + rejected.getRequisitionId()));
        assertTrue(html.contains("dash-bars__meter"));
        assertTrue(html.contains("Pipeline ứng viên"));
        assertTrue(html.contains("/applications?stage=INTERVIEW"));
        assertFalse(html.contains("chart.umd.min.js"));
        assertFalse(html.contains("recruitment-dashboard.js"));
    }
}
