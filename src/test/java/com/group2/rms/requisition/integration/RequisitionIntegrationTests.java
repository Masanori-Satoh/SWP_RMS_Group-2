package com.group2.rms.requisition.integration;

import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.dto.RequisitionSearchRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.repository.RequisitionApprovalRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.service.RequisitionListService;
import com.group2.rms.requisition.service.RequisitionService;
import com.group2.rms.requisition.validator.RequisitionValidator;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * INTEGRATION TEST: Service + Repository + SQL Server THẬT (cần SQL Server đang chạy).
 * Mỗi test chạy trong một transaction và rollback ở cuối (@Transactional), nên không để lại dữ liệu.
 * Kiểm tra những gì unit test không thấy được: câu JPQL / Specification chạy đúng trên SQL Server,
 * ràng buộc khóa ngoại, cột NVARCHAR tiếng Việt, và cả trang render qua bộ lọc bảo mật thật.
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện, Arrange / Act / Assert.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional
class RequisitionIntegrationTests {

        @Autowired
        private RequisitionService service;
        @Autowired
        private RequisitionListService listService;
        @Autowired
        private JobRequisitionRepository requisitions;
        @Autowired
        private RequisitionApprovalRepository approvals;
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

        private Department department;
        private User hiringManager;
        private User director;

        @BeforeEach
        void setUp() {
                Role hmRole = roles.findByRoleName(RequisitionAccess.ROLE_HIRING_MANAGER)
                                .orElseGet(() -> roles.save(Role.builder().roleName(RequisitionAccess.ROLE_HIRING_MANAGER).build()));
                Role directorRole = roles.findByRoleName(RequisitionAccess.ROLE_DIRECTOR)
                                .orElseGet(() -> roles.save(Role.builder().roleName(RequisitionAccess.ROLE_DIRECTOR).build()));

                department = departments.findAll().stream().findFirst()
                                .orElseGet(() -> departments.save(Department.builder().departmentName("Engineering").build()));

                hiringManager = users.findByUsernameIgnoreCase("it_hm").orElseGet(() -> users.save(User.builder()
                                .username("it_hm").passwordHash("$2a$10$dummy").fullName("HM Tester")
                                .email("hm_tester@rms.local").accountStatus("Active").role(hmRole).department(department)
                                .build()));

                director = users.findByUsernameIgnoreCase("it_dir").orElseGet(() -> users.save(User.builder()
                                .username("it_dir").passwordHash("$2a$10$dummy").fullName("Director Tester")
                                .email("dir_tester@rms.local").accountStatus("Active").role(directorRole).department(department)
                                .build()));
        }

        // ----- Hàm hỗ trợ -----

        /** Các trường NOT NULL của bảng JobRequisition; từng test tự đặt trạng thái / người tạo / phòng ban. */
        private JobRequisition.JobRequisitionBuilder validRequisition() {
                return JobRequisition.builder()
                                .recruitmentRound(1)
                                .numberOfPositions(1)
                                .employmentType("Full-time")
                                .gender("Any")
                                .workModel("On-site")
                                .jobDescription("Integration-test responsibilities")
                                .requirementDetails("Integration-test requirements")
                                .screeningCriteria(new ArrayList<>());
        }

        private JobRequisition saveRequisition(String title, User creator, Department inDepartment, String status) {
                return requisitions.saveAndFlush(validRequisition().title(title).hiringManager(creator)
                                .department(inDepartment).approvalStatus(status).build());
        }

        private JobPosting savePosting(JobRequisition requisition, int postingNo, String status) {
                return postings.saveAndFlush(JobPosting.builder().requisition(requisition).postingNo(postingNo)
                                .postingTitle(requisition.getTitle()).jobDescription("Mô tả").jobRequirements("Yêu cầu")
                                .postingStatus(status).createdBy(hiringManager).build());
        }

        private List<RequisitionResponse> searchByKeyword(String keyword) {
                return listService.getListPage(new RequisitionSearchRequest(
                                keyword, null, null, null, null, null, null, "newest", 0, 10)).page().getContent();
        }

        // ----- Tạo -----

        @Test
        @WithMockUser(username = "it_hm", roles = "HIRING_MANAGER")
        @DisplayName("HM lưu nháp -> Lưu vào DB đúng người tạo, phòng ban, tiêu chí và mã REQ-xxxx")
        void createRequisition_shouldPersistRelationships_whenManagerSavesDraft() {
                // Arrange
                RequisitionRequest request = RequisitionRequest.builder()
                                .action(RequisitionValidator.ACTION_DRAFT)
                                .title("Integration Java Dev")
                                .numberOfPositions(2)
                                .employmentType("Full-time")
                                .gender("Any")
                                .workModel("On-site")
                                .jobDescription("Integration-test responsibilities")
                                .requirementDetails("Integration-test requirements")
                                .screeningCriteria(new ArrayList<>(List.of(ScreeningCriteriaRequest.builder()
                                                .criteriaName("Spring Boot").criteriaType("Skill").requiredValue("2 years")
                                                .weight(new BigDecimal("100.00")).isMandatory(true).build())))
                                .build();

                // Act
                Integer id = service.createRequisition(request);

                // Assert
                JobRequisition saved = requisitions.findById(id).orElseThrow();
                assertEquals(RequisitionAccess.STATUS_DRAFT, saved.getApprovalStatus());
                assertEquals("Integration Java Dev", saved.getTitle());
                assertEquals(hiringManager.getUserId(), saved.getHiringManager().getUserId());
                assertEquals(department.getDepartmentId(), saved.getDepartment().getDepartmentId());
                assertEquals(1, saved.getScreeningCriteria().size());
                assertEquals(String.format("REQ-%04d", id), saved.getRequisitionCode());
        }

        // ----- Đọc: danh sách và chi tiết -----

        @Test
        @WithMockUser(username = "it_hm", roles = "HIRING_MANAGER")
        @DisplayName("Dữ liệu tiếng Việt có dấu -> Đọc đúng ở danh sách, chi tiết và render được cả 2 trang")
        void getListPageAndGetById_shouldReadUnicodeColumns_whenDataHasVietnameseText() throws Exception {
                // Arrange
                JobRequisition requisition = requisitions.saveAndFlush(validRequisition()
                                .title("Unicode read regression").hiringManager(hiringManager).department(department)
                                .approvalStatus(RequisitionAccess.STATUS_DRAFT).gender("Female")
                                .workLocation("Văn phòng Hà Nội").workModel("Hybrid").probationDays(60).build());
                Integer id = requisition.getRequisitionId();
                entityManager.clear(); // Đọc lại từ JDBC, không lấy từ cache của persistence context

                // Act
                RequisitionResponse row = searchByKeyword("Unicode read regression").stream()
                                .filter(r -> id.equals(r.requisitionId())).findFirst().orElseThrow();
                RequisitionResponse detail = service.getById(id);

                // Assert
                assertEquals("Female", entityManager.createNativeQuery(
                                "SELECT RequiredGender FROM JobRequisition WHERE RequisitionId = :id")
                                .setParameter("id", id).getSingleResult());
                assertEquals("Văn phòng Hà Nội", row.workLocation());
                assertEquals("Nữ", detail.genderLabel());
                assertEquals("60 ngày", detail.probationLabel());
                assertEquals("Kết hợp", detail.workModelLabel());
                mvc.perform(get("/requisitions")).andExpect(status().isOk()).andExpect(view().name("requisitions/list"));
                mvc.perform(get("/requisitions/{id}", id)).andExpect(status().isOk())
                                .andExpect(view().name("requisitions/detail"));
        }

        @Test
        @WithMockUser(username = "it_hm", roles = "HIRING_MANAGER")
        @DisplayName("HM -> Thấy phiếu người khác tạo trong phòng mình, không thấy phiếu phòng khác")
        void getListPage_shouldOnlyShowOwnDepartment_whenViewerIsHiringManager() {
                // Arrange
                Department otherDepartment = departments.save(Department.builder()
                                .departmentName("IT Scope Other Dept").departmentStatus("Active").build());
                Integer sameDepartmentId = saveRequisition("Scope test same dept", director, department,
                                RequisitionAccess.STATUS_APPROVED).getRequisitionId();
                saveRequisition("Scope test other dept", director, otherDepartment, RequisitionAccess.STATUS_APPROVED);

                // Act
                List<RequisitionResponse> rows = searchByKeyword("Scope test");

                // Assert
                assertEquals(List.of(sameDepartmentId), rows.stream().map(RequisitionResponse::requisitionId).toList());
        }

        @Test
        @WithMockUser(username = "it_hm", roles = "HIRING_MANAGER")
        @DisplayName("Yêu cầu đăng 2 lần (Closed rồi Published) -> Danh sách hiện tin MỚI NHẤT, không xóa được; yêu cầu chưa có tin -> 'Chưa có tin'")
        void getListPage_shouldShowLatestPosting_whenRequisitionWasPostedTwice() {
                // Arrange
                JobRequisition postedTwice = saveRequisition("Latest posting check A", hiringManager, department,
                                RequisitionAccess.STATUS_APPROVED);
                saveRequisition("Latest posting check B", hiringManager, department, RequisitionAccess.STATUS_APPROVED);
                savePosting(postedTwice, 1, "Closed");
                JobPosting latest = savePosting(postedTwice, 2, "Published");
                entityManager.clear();

                // Act
                List<RequisitionResponse> rows = searchByKeyword("Latest posting check");

                // Assert
                RequisitionResponse rowA = rows.stream().filter(r -> r.title().endsWith("A")).findFirst().orElseThrow();
                RequisitionResponse rowB = rows.stream().filter(r -> r.title().endsWith("B")).findFirst().orElseThrow();
                assertEquals(latest.getJobPostingId(), rowA.jobPostingId());
                assertEquals("Đang đăng", rowA.postingLabel());
                assertFalse(rowA.deletable());
                assertNull(rowB.jobPostingId());
                assertEquals("Chưa có tin", rowB.postingLabel());
                assertEquals("HM Tester", rowB.hiringManagerName());
        }

        // ----- Chuyển trạng thái -----

        @Test
        @WithMockUser(username = "it_dir", roles = "DIRECTOR")
        @DisplayName("Giám đốc duyệt -> Approved, có 1 bản ghi duyệt kèm ý kiến")
        void decide_shouldApproveAndSaveApproval_whenDirectorApproves() {
                // Arrange
                JobRequisition requisition = saveRequisition("Pending Dev", hiringManager, department,
                                RequisitionAccess.STATUS_PENDING_DIRECTOR);

                // Act
                service.decide(requisition.getRequisitionId(), true, "Approved for hiring");

                // Assert
                assertEquals(RequisitionAccess.STATUS_APPROVED,
                                requisitions.findById(requisition.getRequisitionId()).orElseThrow().getApprovalStatus());
                var history = approvals.findByRequisition_RequisitionIdOrderByApprovalDateDesc(requisition.getRequisitionId());
                assertEquals(1, history.size());
                assertEquals(RequisitionAccess.STATUS_APPROVED, history.get(0).getStatus());
                assertEquals("Approved for hiring", history.get(0).getComments());
        }

        @Test
        @WithMockUser(username = "it_dir", roles = "DIRECTOR")
        @DisplayName("Giám đốc trả lại có lý do -> Rejected, bản ghi duyệt lưu lý do")
        void decide_shouldRejectAndSaveReason_whenDirectorRejects() {
                // Arrange
                JobRequisition requisition = saveRequisition("Pending QA", hiringManager, department,
                                RequisitionAccess.STATUS_PENDING_DIRECTOR);

                // Act
                service.decide(requisition.getRequisitionId(), false, "Need more budget details");

                // Assert
                assertEquals(RequisitionAccess.STATUS_REJECTED,
                                requisitions.findById(requisition.getRequisitionId()).orElseThrow().getApprovalStatus());
                var history = approvals.findByRequisition_RequisitionIdOrderByApprovalDateDesc(requisition.getRequisitionId());
                assertEquals(1, history.size());
                assertEquals("Need more budget details", history.get(0).getComments());
        }

        @Test
        @WithMockUser(username = "it_hm", roles = "HIRING_MANAGER")
        @DisplayName("Người tạo rút lại yêu cầu đang chờ -> Về Draft")
        void withdraw_shouldReturnToDraft_whenOwnerWithdrawsPending() {
                // Arrange
                JobRequisition requisition = saveRequisition("Pending UX", hiringManager, department,
                                RequisitionAccess.STATUS_PENDING_DIRECTOR);

                // Act
                service.withdraw(requisition.getRequisitionId());

                // Assert
                assertEquals(RequisitionAccess.STATUS_DRAFT,
                                requisitions.findById(requisition.getRequisitionId()).orElseThrow().getApprovalStatus());
        }

        // ----- Sao chép, xóa -----

        @Test
        @WithMockUser(username = "it_hm", roles = "HIRING_MANAGER")
        @DisplayName("Sao chép yêu cầu đã duyệt -> Trả về form mới cùng tên, gợi ý đợt 2, chưa ghi DB")
        void copy_shouldReturnNewForm_whenSourceIsApproved() {
                // Arrange
                JobRequisition original = saveRequisition("Original Requisition", hiringManager, department,
                                RequisitionAccess.STATUS_APPROVED);
                long countBefore = requisitions.count();

                // Act
                RequisitionRequest copy = service.copy(original.getRequisitionId());

                // Assert
                assertEquals("Original Requisition", copy.getTitle());
                assertEquals(2, copy.getRecruitmentRound());
                assertEquals(countBefore, requisitions.count());
        }

        @Test
        @WithMockUser(username = "it_hm", roles = "HIRING_MANAGER")
        @DisplayName("Xóa bản nháp không có dữ liệu phụ thuộc -> Bản ghi biến mất khỏi DB")
        void deleteRequisition_shouldRemoveRow_whenDraftHasNoDependencies() {
                // Arrange
                Integer id = saveRequisition("To be deleted", hiringManager, department, RequisitionAccess.STATUS_DRAFT)
                                .getRequisitionId();

                // Act
                service.deleteRequisition(id);

                // Assert
                assertTrue(requisitions.findById(id).isEmpty());
        }
}
