package com.group2.rms.jobposting.integration;

import com.group2.rms.jobposting.dto.InternalJobPostingResponse;
import com.group2.rms.jobposting.dto.JobPostingSearchRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.jobposting.service.JobPostingLabels;
import com.group2.rms.jobposting.service.JobPostingListService;
import com.group2.rms.jobposting.service.JobPostingSort;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * INTEGRATION TEST: JobPostingListService + JobPostingRepository + SQL Server THẬT (cần SQL Server đang chạy).
 * Mỗi test chạy trong một transaction và rollback ở cuối (@Transactional), nên không để lại dữ liệu.
 * Kiểm tra những gì unit test không thấy: Specification (Hẹn đăng, nhóm hạn nộp, phạm vi HM, tìm tiếng Việt,
 * sắp theo số hồ sơ), các câu JPQL mới của bảng điều khiển / ô chọn yêu cầu, và render trang qua bảo mật thật.
 * Dữ liệu tạo trong một phòng ban riêng, tiêu đề riêng để không đụng dữ liệu seed.
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện, Arrange / Act / Assert;
 * "bây giờ" lấy từ bean Clock của ứng dụng, không gọi LocalDateTime.now().
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional
class JobPostingIntegrationTests {

    private static final String TITLE_PREFIX = "IT-JP ";

    @Autowired
    private JobPostingListService listService;
    @Autowired
    private JobPostingRepository postings;
    @Autowired
    private JobRequisitionRepository requisitions;
    @Autowired
    private DepartmentRepository departments;
    @Autowired
    private UserRepository users;
    @Autowired
    private RoleRepository roles;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private Clock clock;
    @Autowired
    private MockMvc mvc;

    private LocalDateTime now;
    private Department department;
    private Department otherDepartment;
    private User hr;
    private User hiringManager;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now(clock);
        department = departments.save(Department.builder().departmentName("IT-JP Phòng Kỹ thuật")
                .departmentStatus("Active").build());
        otherDepartment = departments.save(Department.builder().departmentName("IT-JP Phòng Khác")
                .departmentStatus("Active").build());
        hr = savedUser("it_jp_hr", RequisitionAccess.ROLE_HR, department);
        hiringManager = savedUser("it_jp_hm", RequisitionAccess.ROLE_HIRING_MANAGER, department);
    }

    // ----- Hàm hỗ trợ -----

    private User savedUser(String username, String roleName, Department inDepartment) {
        Role role = roles.findByRoleName(roleName).orElseGet(() -> roles.save(Role.builder().roleName(roleName).build()));
        return users.findByUsernameIgnoreCase(username).orElseGet(() -> users.save(User.builder()
                .username(username).passwordHash("$2a$10$dummy").fullName("Tester " + username)
                .email(username + "@rms.local").accountStatus("Active").role(role).department(inDepartment).build()));
    }

    /** Yêu cầu đã duyệt, đủ các cột NOT NULL. */
    private JobRequisition savedRequisition(String title, Department inDepartment) {
        return requisitions.saveAndFlush(JobRequisition.builder().title(title).recruitmentRound(1).numberOfPositions(1)
                .employmentType("Full-time").gender("Any").workModel("On-site")
                .jobDescription("Integration-test responsibilities").requirementDetails("Integration-test requirements")
                .approvalStatus(RequisitionAccess.STATUS_APPROVED).hiringManager(hiringManager)
                .department(inDepartment).screeningCriteria(new ArrayList<>()).build());
    }

    private JobPosting savedPosting(JobRequisition requisition, int postingNo, String title, String status,
            LocalDateTime postingDate, LocalDateTime deadline) {
        return postings.saveAndFlush(JobPosting.builder().requisition(requisition).postingNo(postingNo)
                .postingTitle(title).jobDescription("Mô tả").jobRequirements("Yêu cầu").postingStatus(status)
                .postingDate(postingDate).applicationDeadline(deadline).createdBy(hr).build());
    }

    private List<InternalJobPostingResponse> search(User viewer, List<String> statuses, String deadline, String sort) {
        JobPostingSearchRequest filter = new JobPostingSearchRequest(TITLE_PREFIX.trim(), statuses, null, null,
                deadline, null, null, sort, 0, JobPostingListService.MAX_PAGE_SIZE);
        return listService.getListPage(filter, viewer).page().getContent();
    }

    private static List<String> titles(List<InternalJobPostingResponse> rows) {
        return rows.stream().map(InternalJobPostingResponse::postingTitle).sorted().toList();
    }

    // =========================================================================
    // Specification
    // =========================================================================

    @Test
    @DisplayName("Lọc 'Hẹn đăng' / 'Đang đăng' -> Tách đúng theo PostingDate so với hiện tại (không có cột riêng trong DB)")
    void getListPage_shouldSplitScheduledFromPublished_onSqlServer() {
        // Arrange
        JobRequisition requisitionA = savedRequisition("IT-JP Req A", department);
        JobRequisition requisitionB = savedRequisition("IT-JP Req B", department);
        savedPosting(requisitionA, 1, TITLE_PREFIX + "Đang đăng", JobPostingAccess.STATUS_PUBLISHED,
                now.minusDays(1), now.plusDays(20));
        savedPosting(requisitionB, 1, TITLE_PREFIX + "Hẹn đăng", JobPostingAccess.STATUS_PUBLISHED,
                now.plusDays(2), now.plusDays(20));
        entityManager.clear();

        // Act
        List<InternalJobPostingResponse> scheduled = search(hr, List.of(JobPostingAccess.STATUS_SCHEDULED), null, null);
        List<InternalJobPostingResponse> published = search(hr, List.of(JobPostingAccess.STATUS_PUBLISHED), null, null);

        // Assert
        assertEquals(List.of(TITLE_PREFIX + "Hẹn đăng"), titles(scheduled));
        assertEquals(List.of(TITLE_PREFIX + "Đang đăng"), titles(published));
    }

    @Test
    @DisplayName("Lọc hạn nộp 'sắp hết hạn' / 'đã quá hạn' -> Đúng nhóm trên SQL Server")
    void getListPage_shouldFilterByDeadlineBucket_onSqlServer() {
        // Arrange
        JobRequisition soon = savedRequisition("IT-JP Soon", department);
        JobRequisition past = savedRequisition("IT-JP Past", department);
        JobRequisition open = savedRequisition("IT-JP Open", department);
        savedPosting(soon, 1, TITLE_PREFIX + "Còn 3 ngày", JobPostingAccess.STATUS_PUBLISHED, now.minusDays(5), now.plusDays(3));
        savedPosting(past, 1, TITLE_PREFIX + "Quá hạn", JobPostingAccess.STATUS_PUBLISHED, now.minusDays(9), now.minusDays(1));
        savedPosting(open, 1, TITLE_PREFIX + "Còn 30 ngày", JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(30));
        entityManager.clear();

        // Act + Assert
        assertEquals(List.of(TITLE_PREFIX + "Còn 3 ngày"), titles(search(hr, null, JobPostingLabels.DEADLINE_SOON, null)));
        assertEquals(List.of(TITLE_PREFIX + "Quá hạn"), titles(search(hr, null, JobPostingLabels.DEADLINE_PAST, null)));
        assertEquals(List.of(TITLE_PREFIX + "Còn 30 ngày"), titles(search(hr, null, JobPostingLabels.DEADLINE_OPEN, null)));
    }

    @Test
    @DisplayName("HM -> Chỉ thấy tin phòng mình, không thấy tin nháp (phạm vi khớp JobPostingAccess#canView)")
    void getListPage_shouldScopeToOwnDepartmentWithoutDrafts_whenViewerIsHiringManager() {
        // Arrange
        savedPosting(savedRequisition("IT-JP Own", department), 1, TITLE_PREFIX + "Phòng mình",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(10));
        savedPosting(savedRequisition("IT-JP Own draft", department), 1, TITLE_PREFIX + "Nháp phòng mình",
                JobPostingAccess.STATUS_DRAFT, null, now.plusDays(10));
        savedPosting(savedRequisition("IT-JP Other", otherDepartment), 1, TITLE_PREFIX + "Phòng khác",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(10));
        entityManager.clear();

        // Act
        List<InternalJobPostingResponse> rows = search(hiringManager, null, null, null);

        // Assert
        assertEquals(List.of(TITLE_PREFIX + "Phòng mình"), titles(rows));
    }

    @Test
    @DisplayName("Tìm theo tên phòng ban tiếng Việt có dấu; sắp 'Nhiều hồ sơ nhất' chạy được cả câu đếm trang")
    void getListPage_shouldSearchVietnameseDepartment_andSortByApplications() {
        // Arrange
        savedPosting(savedRequisition("IT-JP Search", department), 1, TITLE_PREFIX + "Tìm phòng ban",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(10));
        entityManager.clear();
        JobPostingSearchRequest byDepartment = new JobPostingSearchRequest("phòng kỹ thuật", null, null, null, null,
                null, null, JobPostingSort.MOST_APPLICATIONS.param(), 0, JobPostingListService.MAX_PAGE_SIZE);

        // Act
        List<InternalJobPostingResponse> rows = listService.getListPage(byDepartment, hr).page().getContent();

        // Assert
        assertTrue(rows.stream().anyMatch(row -> (TITLE_PREFIX + "Tìm phòng ban").equals(row.postingTitle())));
        assertTrue(rows.stream().allMatch(row -> "IT-JP Phòng Kỹ thuật".equals(row.departmentName())));
    }

    // =========================================================================
    // JPQL mới (bảng điều khiển, ô chọn yêu cầu)
    // =========================================================================

    @Test
    @DisplayName("Số lần đăng lớn nhất của nhiều yêu cầu -> 1 câu GROUP BY, yêu cầu chưa đăng không có dòng")
    void findMaxPostingNoByRequisitionIds_shouldGroupByRequisition() {
        // Arrange
        JobRequisition postedTwice = savedRequisition("IT-JP Twice", department);
        JobRequisition neverPosted = savedRequisition("IT-JP Never", department);
        savedPosting(postedTwice, 1, TITLE_PREFIX + "Lần 1", JobPostingAccess.STATUS_CLOSED, now.minusDays(40), now.minusDays(10));
        savedPosting(postedTwice, 2, TITLE_PREFIX + "Lần 2", JobPostingAccess.STATUS_DRAFT, null, now.plusDays(10));

        // Act
        Map<Integer, Integer> result = new HashMap<>();
        for (Object[] row : postings.findMaxPostingNoByRequisitionIds(
                List.of(postedTwice.getRequisitionId(), neverPosted.getRequisitionId()))) {
            result.put((Integer) row[0], ((Number) row[1]).intValue());
        }

        // Assert
        assertEquals(Map.of(postedTwice.getRequisitionId(), 2), result);
    }

    @Test
    @DisplayName("Đếm bảng điều khiển: thêm 1 tin còn hạn + 1 tin quá hạn -> Mỗi câu đếm tăng đúng 1; tin mới nằm trong 'gần đây'")
    void dashboardQueries_shouldCountByStatusAndDeadline() {
        // Arrange
        long openBefore = postings.countWithOpenDeadline(JobPostingAccess.STATUS_PUBLISHED, now);
        long passedBefore = postings.countWithPassedDeadline(JobPostingAccess.STATUS_PUBLISHED, now);
        savedPosting(savedRequisition("IT-JP Open count", department), 1, TITLE_PREFIX + "Còn hạn",
                JobPostingAccess.STATUS_PUBLISHED, now.minusMinutes(1), now.plusDays(5));
        savedPosting(savedRequisition("IT-JP Passed count", department), 1, TITLE_PREFIX + "Hết hạn",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(10), now.minusDays(1));
        entityManager.clear();

        // Act + Assert
        assertEquals(openBefore + 1, postings.countWithOpenDeadline(JobPostingAccess.STATUS_PUBLISHED, now));
        assertEquals(passedBefore + 1, postings.countWithPassedDeadline(JobPostingAccess.STATUS_PUBLISHED, now));
        assertTrue(postings.countByDepartment().stream()
                .anyMatch(row -> "IT-JP Phòng Kỹ thuật".equals(row[0]) && ((Number) row[1]).longValue() == 2));
        List<JobPosting> recent = postings.findRecent(PageRequest.of(0, 5));
        assertTrue(recent.stream().anyMatch(posting -> (TITLE_PREFIX + "Còn hạn").equals(posting.getPostingTitle())));
    }

    // =========================================================================
    // Render trang qua bảo mật thật
    // =========================================================================

    @Test
    @WithMockUser(username = "it_jp_hr", roles = "HR")
    @DisplayName("HR mở danh sách và chi tiết tin tiếng Việt -> Render được cả hai trang")
    void pages_shouldRender_whenHrOpensListAndDetail() throws Exception {
        // Arrange
        JobPosting posting = savedPosting(savedRequisition("IT-JP Render", department), 1, TITLE_PREFIX + "Kế toán trưởng",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(10));
        entityManager.clear();

        // Act + Assert
        mvc.perform(get("/internal/job-postings").param("q", "Kế toán"))
                .andExpect(status().isOk()).andExpect(view().name("job-postings/list"));
        mvc.perform(get("/internal/job-postings/{id}", posting.getJobPostingId()))
                .andExpect(status().isOk()).andExpect(view().name("job-postings/detail"));
        mvc.perform(get("/internal/job-postings/{id}/edit", posting.getJobPostingId()))
                .andExpect(status().isOk()).andExpect(view().name("job-postings/create"));
    }

    @Test
    @WithMockUser(username = "it_jp_hm", roles = "HIRING_MANAGER")
    @DisplayName("HM mở tin nháp phòng mình bằng URL -> 403 (D5)")
    void detail_shouldReturnForbidden_whenHiringManagerOpensDraft() throws Exception {
        JobPosting draft = savedPosting(savedRequisition("IT-JP Draft 403", department), 1, TITLE_PREFIX + "Nháp",
                JobPostingAccess.STATUS_DRAFT, null, now.plusDays(10));
        entityManager.clear();

        mvc.perform(get("/internal/job-postings/{id}", draft.getJobPostingId())).andExpect(status().isForbidden());
    }
}
