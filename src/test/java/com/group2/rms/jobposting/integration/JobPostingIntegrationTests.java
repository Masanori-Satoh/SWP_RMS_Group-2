package com.group2.rms.jobposting.integration;

import com.group2.rms.admin.entity.SystemConfig;
import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.core.dto.FilterOption;
import com.group2.rms.jobposting.dto.InternalJobPostingResponse;
import com.group2.rms.jobposting.dto.JobPostingSearchRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.jobposting.service.JobPostingLabels;
import com.group2.rms.jobposting.service.JobPostingListService;
import com.group2.rms.jobposting.service.JobPostingService;
import com.group2.rms.jobposting.service.JobPostingSort;
import com.group2.rms.jobposting.service.JobPostingSpecifications;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * INTEGRATION TEST: JobPostingListService + JobPostingRepository + SQL Server THẬT (cần SQL Server đang chạy).
 * Mỗi test chạy trong một transaction và rollback ở cuối (@Transactional), nên không để lại dữ liệu.
 * Kiểm tra những gì unit test không thấy: Specification (trạng thái, nhóm hạn nộp, phạm vi HM, tìm tiếng Việt,
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
    private static final String APPLICATION_APPLIED = "Applied";

    @Autowired
    private JobPostingListService listService;
    @Autowired
    private JobPostingService postingService;
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
                .department(inDepartment).build());
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

    // Tìm theo từ khóa + phòng ban như HR gõ trên màn danh sách; chỉ giữ tin do test tạo
    private List<String> searchTitles(String keyword, Integer departmentId) {
        JobPostingSearchRequest filter = new JobPostingSearchRequest(keyword, null, departmentId, null, null,
                null, null, null, 0, JobPostingListService.MAX_PAGE_SIZE);
        return titles(listService.getListPage(filter, hr).page().getContent()).stream()
                .filter(title -> title.startsWith(TITLE_PREFIX)).toList();
    }

    // Chạy thẳng một Specification trên bảng JobPosting, chỉ giữ tin do test tạo
    private List<String> findTitles(Specification<JobPosting> condition) {
        return postings.findAll(JobPostingSpecifications.titleOrDepartmentContains(TITLE_PREFIX.trim()).and(condition))
                .stream().map(JobPosting::getPostingTitle).sorted().toList();
    }

    private JobPosting savedPostingBy(User creator, String title) {
        JobPosting posting = savedPosting(savedRequisition(title, department), 1, title,
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(10));
        posting.setCreatedBy(creator);
        return postings.saveAndFlush(posting);
    }

    // Mỗi hồ sơ một ứng viên riêng để không vướng ràng buộc một ứng viên nộp một tin
    private Application savedApplication(JobPosting posting, LocalDateTime submittedAt) {
        Role candidateRole = roles.findByRoleName("Candidate")
                .orElseGet(() -> roles.save(Role.builder().roleName("Candidate").build()));
        String username = "it_jp_cand_" + System.nanoTime();
        User account = users.save(User.builder().username(username).passwordHash("$2a$10$dummy")
                .fullName("Ứng viên " + username).email(username + "@rms.local").accountStatus("Active")
                .role(candidateRole).build());
        Candidate candidate = Candidate.builder().account(account).build();
        entityManager.persist(candidate);
        Application application = Application.builder().candidate(candidate).jobPosting(posting)
                .appliedCvUrl("/cv/it-jp.pdf").submissionDate(submittedAt).applicationStatus(APPLICATION_APPLIED)
                .build();
        entityManager.persist(application);
        return application;
    }

    private Set<String> choiceValues() {
        return postingService.requisitionChoices(hr).stream().map(FilterOption::value).collect(Collectors.toSet());
    }

    // =========================================================================
    // Specification
    // =========================================================================

    @Test
    @DisplayName("Lọc 'Đang đăng' -> Lấy các tin Published trên SQL Server")
    void getListPage_shouldFindPublished_onSqlServer() {
        // Arrange
        JobRequisition requisitionA = savedRequisition("IT-JP Req A", department);
        JobRequisition requisitionB = savedRequisition("IT-JP Req B", department);
        savedPosting(requisitionA, 1, TITLE_PREFIX + "Đang đăng", JobPostingAccess.STATUS_PUBLISHED,
                now.minusDays(1), now.plusDays(20));
        savedPosting(requisitionB, 1, TITLE_PREFIX + "Đã đăng lần hai", JobPostingAccess.STATUS_PUBLISHED,
                now.minusDays(2), now.plusDays(20));
        entityManager.clear();

        // Act
        List<InternalJobPostingResponse> published = search(hr, List.of(JobPostingAccess.STATUS_PUBLISHED), null, null);

        // Assert
        assertEquals(List.of(TITLE_PREFIX + "Đang đăng", TITLE_PREFIX + "Đã đăng lần hai"), titles(published));
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

    @Test
    @DisplayName("HR chọn phòng ban trong ô lọc -> Chỉ ra tin của phòng đó")
    void getListPage_shouldFilterByDepartmentId_whenHrPicksDepartment() {
        // Arrange
        savedPosting(savedRequisition("IT-JP Dept own", department), 1, TITLE_PREFIX + "Phòng kỹ thuật",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(10));
        savedPosting(savedRequisition("IT-JP Dept other", otherDepartment), 1, TITLE_PREFIX + "Phòng khác",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(10));
        entityManager.clear();

        // Act
        List<String> rows = searchTitles(TITLE_PREFIX.trim(), otherDepartment.getDepartmentId());

        // Assert
        assertEquals(List.of(TITLE_PREFIX + "Phòng khác"), rows);
    }

    @Test
    @DisplayName("Lọc người tạo -> Chỉ ra tin do HR đó tạo")
    void creatorIs_shouldReturnOnlyCreatorsPostings_whenTwoHrsCreatePostings() {
        // Arrange
        User otherHr = savedUser("it_jp_hr2", RequisitionAccess.ROLE_HR, department);
        savedPostingBy(hr, TITLE_PREFIX + "Của HR một");
        savedPostingBy(otherHr, TITLE_PREFIX + "Của HR hai");
        entityManager.clear();

        // Act
        List<String> rows = findTitles(JobPostingSpecifications.creatorIs(otherHr.getUserId()));

        // Assert
        assertEquals(List.of(TITLE_PREFIX + "Của HR hai"), rows);
    }

    @Test
    @DisplayName("Lọc ngày đăng từ / đến -> Tính trọn cả ngày đầu và ngày cuối, loại ngày ngay trước và ngay sau")
    void postedOnOrAfterAndBefore_shouldIncludeWholeBoundaryDays_whenFilteringByDateRange() {
        // Arrange
        LocalDate from = now.toLocalDate().minusDays(10);
        LocalDate to = now.toLocalDate().minusDays(5);
        JobRequisition requisition = savedRequisition("IT-JP Posted range", department);
        savedPosting(requisition, 1, TITLE_PREFIX + "Trước ngày đầu", JobPostingAccess.STATUS_CLOSED,
                from.minusDays(1).atTime(23, 59), now.minusDays(1));
        savedPosting(requisition, 2, TITLE_PREFIX + "Đầu ngày đầu", JobPostingAccess.STATUS_CLOSED,
                from.atStartOfDay(), now.minusDays(1));
        savedPosting(requisition, 3, TITLE_PREFIX + "Cuối ngày cuối", JobPostingAccess.STATUS_CLOSED,
                to.atTime(23, 59), now.minusDays(1));
        savedPosting(requisition, 4, TITLE_PREFIX + "Sau ngày cuối", JobPostingAccess.STATUS_CLOSED,
                to.plusDays(1).atStartOfDay(), now.minusDays(1));
        entityManager.clear();

        // Act
        List<String> rows = findTitles(JobPostingSpecifications.postedOnOrAfter(from)
                .and(JobPostingSpecifications.postedOnOrBefore(to)));

        // Assert
        assertEquals(List.of(TITLE_PREFIX + "Cuối ngày cuối", TITLE_PREFIX + "Đầu ngày đầu"), rows);
    }

    @Test
    @DisplayName("Lọc hạn nộp 'còn hạn' -> Có tin không hạn và tin hạn từ ngày thứ 8, loại tin hết hạn trong ngày thứ 7")
    void getListPage_shouldIncludeNoDeadline_whenDeadlineBucketIsOpen() {
        // Arrange
        LocalDate today = now.toLocalDate();
        savedPosting(savedRequisition("IT-JP No deadline", department), 1, TITLE_PREFIX + "Không hạn",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), null);
        savedPosting(savedRequisition("IT-JP Day 7", department), 1, TITLE_PREFIX + "Hết ngày thứ 7",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), today.plusDays(7).atTime(23, 59));
        savedPosting(savedRequisition("IT-JP Day 8", department), 1, TITLE_PREFIX + "Đầu ngày thứ 8",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), today.plusDays(8).atStartOfDay());
        entityManager.clear();

        // Act
        List<InternalJobPostingResponse> rows = search(hr, null, JobPostingLabels.DEADLINE_OPEN, null);

        // Assert
        assertEquals(List.of(TITLE_PREFIX + "Không hạn", TITLE_PREFIX + "Đầu ngày thứ 8"), titles(rows));
    }

    @Test
    @DisplayName("HM là trưởng phòng của phòng khác -> Thấy cả tin phòng mình và tin phòng mình làm trưởng")
    void getListPage_shouldIncludeManagedDepartment_whenHiringManagerIsDepartmentManager() {
        // Arrange
        otherDepartment.setManager(hiringManager);
        departments.saveAndFlush(otherDepartment);
        savedPosting(savedRequisition("IT-JP Managed own", department), 1, TITLE_PREFIX + "Phòng mình",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(10));
        savedPosting(savedRequisition("IT-JP Managed other", otherDepartment), 1, TITLE_PREFIX + "Phòng làm trưởng",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(10));
        entityManager.clear();

        // Act
        List<InternalJobPostingResponse> rows = search(hiringManager, null, null, null);

        // Assert
        assertEquals(List.of(TITLE_PREFIX + "Phòng làm trưởng", TITLE_PREFIX + "Phòng mình"), titles(rows));
    }

    @Test
    @DisplayName("HM chưa được gán phòng ban nhưng là trưởng phòng -> Chỉ thấy tin phòng mình làm trưởng")
    void getListPage_shouldShowOnlyManagedDepartment_whenHiringManagerHasNoDepartment() {
        // Arrange
        User managerWithoutDepartment = savedUser("it_jp_hm_nodept", RequisitionAccess.ROLE_HIRING_MANAGER, null);
        otherDepartment.setManager(managerWithoutDepartment);
        departments.saveAndFlush(otherDepartment);
        savedPosting(savedRequisition("IT-JP Nodept own", department), 1, TITLE_PREFIX + "Phòng kỹ thuật",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(10));
        savedPosting(savedRequisition("IT-JP Nodept managed", otherDepartment), 1, TITLE_PREFIX + "Phòng làm trưởng",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(10));
        entityManager.clear();

        // Act
        List<InternalJobPostingResponse> rows = search(managerWithoutDepartment, null, null, null);

        // Assert
        assertEquals(List.of(TITLE_PREFIX + "Phòng làm trưởng"), titles(rows));
    }

    @Test
    @DisplayName("Từ khóa có % hoặc _ -> Hiểu là chữ người dùng gõ, không phải ký tự đại diện của LIKE")
    void getListPage_shouldMatchLiterally_whenKeywordHasLikeWildcards() {
        // Arrange
        JobRequisition requisition = savedRequisition("IT-JP Wildcard", department);
        savedPosting(requisition, 1, TITLE_PREFIX + "Giảm 50% phí", JobPostingAccess.STATUS_CLOSED, now.minusDays(9), now.minusDays(1));
        savedPosting(requisition, 2, TITLE_PREFIX + "Giảm 500 phí", JobPostingAccess.STATUS_CLOSED, now.minusDays(9), now.minusDays(1));
        savedPosting(requisition, 3, TITLE_PREFIX + "Mã a_b", JobPostingAccess.STATUS_CLOSED, now.minusDays(9), now.minusDays(1));
        savedPosting(requisition, 4, TITLE_PREFIX + "Mã axb", JobPostingAccess.STATUS_CLOSED, now.minusDays(9), now.minusDays(1));
        entityManager.clear();

        // Act + Assert
        assertEquals(List.of(TITLE_PREFIX + "Giảm 50% phí"), searchTitles("50%", null));
        assertEquals(List.of(TITLE_PREFIX + "Mã a_b"), searchTitles("a_b", null));
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
    @DisplayName("Số lần đăng lớn nhất của một yêu cầu -> Chưa đăng trả 0, đã có lần 1 và lần 3 trả 3")
    void findMaxPostingNo_shouldReturnHighestNumberOrZero_whenRequisitionHasPostings() {
        // Arrange
        JobRequisition posted = savedRequisition("IT-JP Max", department);
        JobRequisition neverPosted = savedRequisition("IT-JP Max never", department);
        savedPosting(posted, 1, TITLE_PREFIX + "Max lần 1", JobPostingAccess.STATUS_CLOSED, now.minusDays(40), now.minusDays(20));
        savedPosting(posted, 3, TITLE_PREFIX + "Max lần 3", JobPostingAccess.STATUS_DRAFT, null, now.plusDays(10));

        // Act + Assert
        assertEquals(3, postings.findMaxPostingNo(posted.getRequisitionId()));
        assertEquals(0, postings.findMaxPostingNo(neverPosted.getRequisitionId()));
    }

    @Test
    @DisplayName("Ô chọn yêu cầu khi tạo tin -> Chỉ có yêu cầu đã duyệt chưa có tin đang mở (tin đã đóng vẫn được chọn)")
    void requisitionChoices_shouldListApprovedWithoutOpenPosting_whenHrCreatesPosting() {
        // Arrange
        JobRequisition noPosting = savedRequisition("IT-JP Choice none", department);
        JobRequisition withDraft = savedRequisition("IT-JP Choice draft", department);
        JobRequisition withPublished = savedRequisition("IT-JP Choice published", department);
        JobRequisition withPaused = savedRequisition("IT-JP Choice paused", department);
        JobRequisition onlyClosed = savedRequisition("IT-JP Choice closed", department);
        JobRequisition notApproved = savedRequisition("IT-JP Choice pending", department);
        notApproved.setApprovalStatus(RequisitionAccess.STATUS_PENDING_DIRECTOR);
        requisitions.saveAndFlush(notApproved);
        savedPosting(withDraft, 1, TITLE_PREFIX + "Choice nháp", JobPostingAccess.STATUS_DRAFT, null, now.plusDays(10));
        savedPosting(withPublished, 1, TITLE_PREFIX + "Choice đăng", JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(10));
        savedPosting(withPaused, 1, TITLE_PREFIX + "Choice dừng", JobPostingAccess.STATUS_PAUSED, now.minusDays(1), now.plusDays(10));
        savedPosting(onlyClosed, 1, TITLE_PREFIX + "Choice đóng", JobPostingAccess.STATUS_CLOSED, now.minusDays(30), now.minusDays(1));
        entityManager.clear();

        // Act
        Set<String> choices = choiceValues();

        // Assert
        assertTrue(choices.contains(String.valueOf(noPosting.getRequisitionId())));
        assertTrue(choices.contains(String.valueOf(onlyClosed.getRequisitionId())));
        assertFalse(choices.contains(String.valueOf(withDraft.getRequisitionId())));
        assertFalse(choices.contains(String.valueOf(withPublished.getRequisitionId())));
        assertFalse(choices.contains(String.valueOf(withPaused.getRequisitionId())));
        assertFalse(choices.contains(String.valueOf(notApproved.getRequisitionId())));
    }

    @Test
    @DisplayName("Đếm hồ sơ 7 ngày gần nhất -> Tính cả hồ sơ nộp đúng mốc 7 ngày, bỏ hồ sơ 8 ngày trước")
    void countApplicationsSince_shouldIncludeExactBoundary_whenCountingLastSevenDays() {
        // Arrange
        JobPosting posting = savedPosting(savedRequisition("IT-JP Since", department), 1, TITLE_PREFIX + "Đếm 7 ngày",
                JobPostingAccess.STATUS_PUBLISHED, now.minusDays(20), now.plusDays(10));
        LocalDateTime since = now.minusDays(7);
        savedApplication(posting, now.minusDays(2));
        savedApplication(posting, since);
        savedApplication(posting, now.minusDays(8));
        entityManager.flush();

        // Act
        long count = postings.countApplicationsSince(posting.getJobPostingId(), since);

        // Assert
        assertEquals(2, count);
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
