package com.group2.rms.dashboard.internal;

import com.group2.rms.dashboard.internal.repository.DashboardListRepository;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.AwaitingPostingRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.DepartmentRequisitionRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.PendingRequisitionRow;
import com.group2.rms.dashboard.internal.repository.InternalMetricsRepository;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * INTEGRATION TEST (SQL Server thật, rollback sau mỗi test): JPQL của dashboard HR / Director / Interviewer
 * chạy được trên schema thật, và trang /dashboard/internal của 3 role render bằng tài khoản seed.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional
class InternalDashboardIntegrationTests {

    @Autowired
    private DashboardListRepository lists;
    @Autowired
    private InternalMetricsRepository metrics;
    @Autowired
    private JobRequisitionRepository requisitions;
    @Autowired
    private JobPostingRepository postings;
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
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now(clock);
        department = departments.findAll().stream().findFirst().orElseThrow();
        hiringManager = users.saveAndFlush(User.builder().username("dash_int_hm").passwordHash("$2a$10$dummy")
                .fullName("Người gửi kiểm thử").email("dash_int_hm@rms.local").accountStatus("Active")
                .role(roles.findByRoleName(RequisitionAccess.ROLE_HIRING_MANAGER).orElseThrow())
                .department(department).build());
    }

    private JobRequisition saveRequisition(String title, String status, int round, JobRequisition original) {
        return saveRequisition(hiringManager, title, status, round, original);
    }

    private JobRequisition saveRequisition(User owner, String title, String status, int round,
            JobRequisition original) {
        return requisitions.saveAndFlush(JobRequisition.builder()
                .title(title).hiringManager(owner).department(department).approvalStatus(status)
                .submittedAt(now.minusDays(9)).recruitmentRound(round).originalRequisition(original)
                .hiringType("Replacement").numberOfPositions(3).employmentType("Full-time").gender("Any")
                .workModel("On-site").jobDescription("Mô tả kiểm thử").requirementDetails("Yêu cầu kiểm thử")
                .screeningCriteria(new ArrayList<>()).build());
    }

    private User saveDirector(String username) {
        return users.saveAndFlush(User.builder().username(username).passwordHash("$2a$10$dummy")
                .fullName("Director kiểm thử").email(username + "@rms.local").accountStatus("Active")
                .role(roles.findByRoleName(RequisitionAccess.ROLE_DIRECTOR).orElseThrow())
                .department(department).build());
    }

    @Test
    @DisplayName("Director: yêu cầu chờ duyệt có phòng ban, người gửi, loại, đợt; đếm theo phòng ban có yêu cầu mới")
    void directorQueries_shouldReturnPendingRequisitionDetails() {
        // Arrange
        User viewer = saveDirector("dash_int_director_details");
        JobRequisition first = saveRequisition("Vị trí tuyển lại kiểm thử", RequisitionAccess.STATUS_APPROVED, 1, null);
        JobRequisition secondRound = saveRequisition("Vị trí tuyển lại kiểm thử", RequisitionAccess.STATUS_PENDING_DIRECTOR,
                2, first);
        entityManager.clear();

        // Act
        List<PendingRequisitionRow> pending = lists.pendingRequisitions(viewer.getUserId());
        List<DepartmentRequisitionRow> byDepartment = lists.openRequisitionsByDepartment();

        // Assert
        PendingRequisitionRow row = pending.stream()
                .filter(item -> item.requisitionId() == secondRound.getRequisitionId()).findFirst().orElseThrow();
        assertEquals(department.getDepartmentName(), row.departmentName());
        assertEquals("Người gửi kiểm thử", row.requesterName());
        assertEquals("Replacement", row.hiringType());
        assertEquals(2, row.recruitmentRound());
        assertEquals(3, row.numberOfPositions());
        DepartmentRequisitionRow departmentRow = byDepartment.stream()
                .filter(item -> item.departmentName().equals(department.getDepartmentName())).findFirst().orElseThrow();
        assertTrue(departmentRow.pending() >= 1);
        assertTrue(departmentRow.approved() >= 1);
    }

    @Test
    @DisplayName("Director chỉ thấy yêu cầu người khác gửi trong hàng chờ duyệt, biểu đồ vẫn đếm toàn công ty")
    void pendingRequisitions_shouldExcludeViewerOwnRequest() {
        User director = saveDirector("dash_int_director");
        JobRequisition other = saveRequisition("Yêu cầu của HM", RequisitionAccess.STATUS_PENDING_DIRECTOR, 1, null);
        JobRequisition own = saveRequisition(director, "Yêu cầu của Director",
                RequisitionAccess.STATUS_PENDING_DIRECTOR, 1, null);
        entityManager.clear();

        List<PendingRequisitionRow> pending = lists.pendingRequisitions(director.getUserId());
        List<DepartmentRequisitionRow> byDepartment = lists.openRequisitionsByDepartment();

        assertTrue(pending.stream().anyMatch(row -> row.requisitionId() == other.getRequisitionId()));
        assertFalse(pending.stream().anyMatch(row -> row.requisitionId() == own.getRequisitionId()));
        assertTrue(byDepartment.stream().anyMatch(row -> row.departmentName().equals(department.getDepartmentName())
                && row.pending() >= 2));
    }

    @Test
    @DisplayName("HR đếm tin Published có ngày đăng hôm nay, bỏ tin hôm qua và hẹn ngày mai")
    void publishedPostingsBetween_shouldCountOnlyToday() {
        LocalDateTime start = now.toLocalDate().atStartOfDay();
        long before = metrics.countPublishedPostingsBetween(start, now);
        savePosting(saveRequisition("Đăng hôm nay", RequisitionAccess.STATUS_APPROVED, 1, null), start);
        savePosting(saveRequisition("Đăng hôm qua", RequisitionAccess.STATUS_APPROVED, 1, null), start.minusDays(1));
        savePosting(saveRequisition("Hẹn ngày mai", RequisitionAccess.STATUS_APPROVED, 1, null), now.plusDays(1));
        entityManager.clear();

        assertEquals(before + 1, metrics.countPublishedPostingsBetween(start, now));
    }

    private JobPosting savePosting(JobRequisition requisition, LocalDateTime postingDate) {
        return postings.saveAndFlush(JobPosting.builder().requisition(requisition).postingNo(1)
                .postingTitle(requisition.getTitle()).jobDescription("Mô tả kiểm thử")
                .jobRequirements("Yêu cầu kiểm thử").postingDate(postingDate)
                .applicationDeadline(now.plusDays(10)).postingStatus(JobPostingAccess.STATUS_PUBLISHED)
                .createdBy(hiringManager).build());
    }

    @Test
    @DisplayName("HR: yêu cầu đã duyệt chưa có tin nằm trong danh sách cần tạo tin; bản nháp thì không")
    void requisitionsAwaitingPosting_shouldIncludeApprovedWithoutPosting() {
        // Arrange
        JobRequisition approved = saveRequisition("Chờ tạo tin kiểm thử", RequisitionAccess.STATUS_APPROVED, 1, null);
        JobRequisition draft = saveRequisition("Nháp kiểm thử", RequisitionAccess.STATUS_DRAFT, 1, null);
        entityManager.clear();

        // Act
        List<AwaitingPostingRow> rows = lists.requisitionsAwaitingPosting();

        // Assert
        assertTrue(rows.stream().anyMatch(row -> row.requisitionId() == approved.getRequisitionId()));
        assertFalse(rows.stream().anyMatch(row -> row.requisitionId() == draft.getRequisitionId()));
    }

    @Test
    @DisplayName("Mọi câu đếm / danh sách còn lại chạy được trên SQL Server (không lỗi JPQL, không số âm)")
    void remainingQueries_shouldRunOnSqlServer() {
        // Arrange
        LocalDateTime from = now.minusDays(30);
        int userId = hiringManager.getUserId();

        // Act + Assert
        assertTrue(metrics.countPublishedPostingsBetween(now.toLocalDate().atStartOfDay(), now) >= 0);
        assertTrue(metrics.applicationDates(from, now).size() >= 0);
        assertTrue(metrics.applicationWaitingSince().size() >= 0);
        assertTrue(metrics.countHiredBetween(from, now) >= 0);
        assertTrue(metrics.countPassedInterviewCandidatesBetween(userId, now.toLocalDate().withDayOfMonth(1).atStartOfDay(), now) >= 0);
        assertTrue(lists.allInterviews(now, now.plusDays(2), 5).size() <= 5);
        assertTrue(lists.panelInterviews(userId, now.minusDays(7), now.plusDays(7)).size() >= 0);
        assertTrue(lists.pendingEvaluations(userId).size() >= 0);
        assertTrue(lists.pendingOffers().size() >= 0);
    }

    @ParameterizedTest(name = "{0} mở /dashboard/internal -> thấy \"{2}\"")
    @CsvSource({
            "HR,ROLE_HR,Việc cần xử lý",
            "Director,ROLE_DIRECTOR,Đề nghị tuyển dụng chờ duyệt",
            "Interviewer,ROLE_INTERVIEWER,Lịch phỏng vấn"})
    @DisplayName("HR / Director / Interviewer -> có KPI icon; chỉ trang có biểu đồ mới nạp Chart.js")
    void dashboard_shouldRenderRoleSection_whenSeedAccountLogsIn(String roleName, String authority, String heading)
            throws Exception {
        // Arrange
        User actor = activeAccount(roleName);
        assumeTrue(actor != null, "Cần một tài khoản Active của role này trong dữ liệu seed");

        // Act
        String html = mvc.perform(get("/dashboard/internal").with(user(actor.getUsername())
                        .authorities(new SimpleGrantedAuthority(authority))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertTrue(html.contains(heading));
        assertTrue(html.contains("dash-kpi__icon"));
        if ("Director".equals(roleName)) {
            assertEquals(3, html.split("class=\"card dash-kpi", -1).length - 1);
            assertTrue(html.contains("Yêu cầu tuyển dụng chờ duyệt"));
            assertTrue(html.contains("Thư mời nhận việc chờ duyệt"));
            assertTrue(html.contains("Ứng viên trúng tuyển trong tháng"));
            assertFalse(html.contains("Tỷ lệ nhận offer"));
        }
        if ("Interviewer".equals(roleName)) {
            assertEquals(3, html.split("class=\"card dash-kpi", -1).length - 1);
            assertTrue(html.contains("Buổi phỏng vấn sắp tới"));
            assertTrue(html.contains("Đánh giá chưa hoàn thành"));
            assertTrue(html.contains("Ứng viên đạt phỏng vấn"));
            assertFalse(html.contains("Đã gửi tháng này"));
        }
        boolean hasCharts = !"Interviewer".equals(roleName);
        assertEquals(hasCharts, html.contains("chart.umd.min.js"));
        assertEquals(hasCharts, html.contains("internal-dashboard.js"));
        assertFalse(html.contains("recruitment-dashboard.js"));
    }

    @Test
    @DisplayName("Interviewer gửi ?week= sai định dạng -> vẫn mở được trang, hiện tuần hiện tại")
    void dashboard_shouldFallBackToCurrentWeek_whenWeekParameterInvalid() throws Exception {
        // Arrange
        User actor = activeAccount("Interviewer");
        assumeTrue(actor != null, "Cần một tài khoản Interviewer Active trong dữ liệu seed");

        // Act
        String html = mvc.perform(get("/dashboard/internal").param("week", "khong-phai-ngay")
                        .with(user(actor.getUsername()).authorities(new SimpleGrantedAuthority("ROLE_INTERVIEWER"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertTrue(html.contains("Lịch phỏng vấn"));
        assertTrue(html.contains("Tuần này"));
    }

    private User activeAccount(String roleName) {
        return users.findAllWithRoleAndDepartment().stream()
                .filter(account -> roleName.equals(account.getRole().getRoleName())
                        && "Active".equals(account.getAccountStatus()))
                .findFirst().orElse(null);
    }
}
