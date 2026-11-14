package com.group2.rms.requisition.integration;

import com.group2.rms.candidate.talentpool.exception.TalentPoolException;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.jobposting.dto.JobPostingRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.jobposting.service.JobPostingService;
import com.group2.rms.jobposting.validator.JobPostingValidator;
import com.group2.rms.notification.repository.NotificationRepository;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.exception.RequisitionStateException;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.service.RequisitionService;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * INTEGRATION TEST theo LUỒNG NGHIỆP VỤ: Yêu cầu tuyển dụng -> Phê duyệt -> Tin tuyển dụng (SQL Server THẬT).
 * Thứ tự: (1) Happy case đi hết luồng, (2) Ngoại lệ / lỗi của bước Yêu cầu, (3) Ngoại lệ / lỗi của bước Tin.
 * Mỗi test tự rollback (@Transactional). Người thao tác đổi giữa các bước bằng loginAs(...),
 * giống HM / Giám đốc / HR lần lượt đăng nhập.
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện, Arrange / Act / Assert.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class RecruitmentFlowIntegrationTests {

    private static final String TITLE = "FLOW Backend Developer";

    @Autowired
    private RequisitionService requisitionService;
    @Autowired
    private JobPostingService postingService;
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
    private NotificationRepository notifications;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private Clock clock;

    private Department department;
    private User hiringManager;
    private User otherManager;
    private User director;
    private User hr;

    @BeforeEach
    void setUp() {
        department = departments.save(Department.builder().departmentName("FLOW Phòng Kỹ thuật")
                .departmentStatus("Active").build());
        hiringManager = savedUser("flow_hm", RequisitionAccess.ROLE_HIRING_MANAGER, department, "Active");
        otherManager = savedUser("flow_hm2", RequisitionAccess.ROLE_HIRING_MANAGER, department, "Active");
        director = savedUser("flow_dir", RequisitionAccess.ROLE_DIRECTOR, department, "Active");
        hr = savedUser("flow_hr", RequisitionAccess.ROLE_HR, department, "Active");
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // Hàm hỗ trợ
    // =========================================================================

    private User savedUser(String username, String roleName, Department inDepartment, String status) {
        Role role = roles.findByRoleName(roleName).orElseGet(() -> roles.save(Role.builder().roleName(roleName).build()));
        User user = users.findByUsernameIgnoreCase(username).orElseGet(() -> User.builder()
                .username(username).passwordHash("$2a$10$dummy").fullName("Flow " + username)
                .email(username + "@rms.local").role(role).build());
        user.setAccountStatus(status);
        user.setDepartment(inDepartment);
        return users.saveAndFlush(user);
    }

    /** Giả lập người dùng đăng nhập: service đọc người thao tác từ SecurityContext. */
    private void loginAs(User user) {
        String authority = "ROLE_" + user.getRole().getRoleName().toUpperCase().replace(' ', '_');
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getUsername(), "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    /** Form yêu cầu đủ mọi trường bắt buộc khi gửi duyệt. */
    private RequisitionRequest fullRequest(String action) {
        return RequisitionRequest.builder()
                .action(action)
                .title(TITLE)
                .numberOfPositions(2)
                .employmentType("Full-time")
                .workModel("Hybrid")
                .workLocation("Hà Nội")
                .minSalary(new BigDecimal("20000000"))
                .maxSalary(new BigDecimal("30000000"))
                .reasonForHiring("Mở rộng đội backend")
                .jobDescription("Phát triển API")
                .requirementDetails("2 năm Java")
                .build();
    }

    /** Yêu cầu nằm sẵn ở trạng thái cho trước (bỏ qua các bước trước đó của luồng). */
    private JobRequisition savedRequisition(String title, User owner, String status) {
        return requisitions.saveAndFlush(JobRequisition.builder().title(title)
                .numberOfPositions(1).employmentType("Full-time").workModel("On-site")
                .jobDescription("Mô tả").requirementDetails("Yêu cầu").approvalStatus(status)
                .hiringManager(owner).createdBy(owner).department(department).build());
    }

    private JobPosting savedPosting(JobRequisition requisition, int postingNo, String status) {
        return postings.saveAndFlush(JobPosting.builder().requisition(requisition).postingNo(postingNo)
                .postingTitle(requisition.getTitle()).jobDescription("Mô tả").jobRequirements("Yêu cầu")
                .employmentType("Full-time").workModel("On-site")
                .applicationDeadline(LocalDate.now(clock).plusDays(10).atTime(23, 59, 59))
                .postingStatus(status).createdBy(hr).build());
    }

    private String approvalStatusOf(Integer requisitionId) {
        entityManager.flush();
        entityManager.clear();
        return requisitions.findById(requisitionId).orElseThrow().getApprovalStatus();
    }

    private JobPosting reload(Integer postingId) {
        entityManager.flush();
        entityManager.clear();
        return postings.findById(postingId).orElseThrow();
    }

    private String latestNotificationType(User recipient) {
        return notifications.findByRecipient_UserIdOrderByCreatedAtDesc(recipient.getUserId(), PageRequest.of(0, 1))
                .getContent().get(0).getEventType();
    }

    // =========================================================================
    // 1. HAPPY CASE
    // =========================================================================

    @Nested
    @Order(1)
    @DisplayName("1. Happy case")
    class HappyCase {

        @Test
        @DisplayName("Luồng đầy đủ: HM nháp -> gửi duyệt -> GĐ duyệt -> HR tạo nháp -> đăng -> tạm dừng -> mở lại -> đóng -> đăng lại lần 2")
        void fullFlow_shouldMoveThroughEveryStatus_whenEachActorDoesTheirStep() {
            // Bước 1: HM lưu nháp
            loginAs(hiringManager);
            Integer reqId = requisitionService.createRequisition(fullRequest(RequisitionValidator.ACTION_DRAFT));
            assertEquals(RequisitionAccess.STATUS_DRAFT, approvalStatusOf(reqId));

            // Bước 2: HM mở form sửa và gửi Giám đốc duyệt
            RequisitionRequest edit = requisitionService.prepareEditForm(reqId);
            assertEquals(TITLE, edit.getTitle());
            edit.setAction(RequisitionValidator.ACTION_SUBMIT);
            requisitionService.updateRequisition(reqId, edit);
            assertEquals(RequisitionAccess.STATUS_PENDING_DIRECTOR, approvalStatusOf(reqId));
            assertEquals("REQUISITION_PENDING_DIRECTOR", latestNotificationType(director));

            // Bước 3: Giám đốc duyệt
            loginAs(director);
            requisitionService.decide(reqId, true, "Đồng ý tuyển");
            assertEquals(RequisitionAccess.STATUS_APPROVED, approvalStatusOf(reqId));
            assertEquals(1, requisitions.findApprovalHistory(reqId).size());
            assertEquals("REQUISITION_APPROVED", latestNotificationType(hiringManager));

            // Bước 4: HR thấy yêu cầu trong ô chọn, mở form tạo tin (điền sẵn từ yêu cầu) và lưu nháp
            loginAs(hr);
            assertTrue(postingService.requisitionChoices(hr).stream()
                    .anyMatch(o -> o.value().equals(String.valueOf(reqId))));
            JobPostingRequest form = postingService.prepareCreateForm(reqId, hr);
            assertEquals(TITLE, form.getPostingTitle());
            assertEquals(1, form.getPostingNo());
            Integer postingId = postingService.createJobPosting(form, hr);
            JobPosting draft = reload(postingId);
            assertEquals(JobPostingAccess.STATUS_DRAFT, draft.getPostingStatus());
            assertNull(draft.getPostingDate());
            assertEquals("20 – 30 triệu VND/tháng", draft.getSalaryDisplay());

            // Bước 5: Đăng tin -> Published, có ngày đăng
            postingService.publishJobPosting(postingId, hr);
            JobPosting published = reload(postingId);
            assertEquals(JobPostingAccess.STATUS_PUBLISHED, published.getPostingStatus());
            assertNotNull(published.getPostingDate());

            // Bước 6: Tạm dừng rồi mở lại
            postingService.pauseJobPosting(postingId, hr);
            assertEquals(JobPostingAccess.STATUS_PAUSED, reload(postingId).getPostingStatus());
            postingService.publishJobPosting(postingId, hr);
            assertEquals(JobPostingAccess.STATUS_PUBLISHED, reload(postingId).getPostingStatus());

            // Bước 7: Đóng tin -> Closed, có thời điểm đóng
            postingService.closeJobPosting(postingId, hr);
            JobPosting closed = reload(postingId);
            assertEquals(JobPostingAccess.STATUS_CLOSED, closed.getPostingStatus());
            assertNotNull(closed.getClosedAt());

            // Bước 8: Đăng lại -> tin MỚI lần 2, nội dung lấy từ tin đã đóng, đăng ngay
            JobPostingRequest repost = postingService.prepareRepostForm(postingId, hr);
            assertEquals(2, repost.getPostingNo());
            assertNull(repost.getJobPostingId());
            Integer secondId = postingService.createAndPublishJobPosting(repost, hr);
            JobPosting second = reload(secondId);
            assertNotEquals(postingId, secondId);
            assertEquals(2, second.getPostingNo());
            assertEquals(JobPostingAccess.STATUS_PUBLISHED, second.getPostingStatus());
            assertEquals(JobPostingAccess.STATUS_CLOSED, reload(postingId).getPostingStatus());
        }

        @Test
        @DisplayName("GĐ trả lại có lý do -> HM sửa và gửi lại -> quay về chờ duyệt, lịch sử giữ lý do")
        void rejectThenResubmit_shouldReturnToPending_whenManagerFixesRequisition() {
            // Arrange
            JobRequisition pending = savedRequisition(TITLE, hiringManager, RequisitionAccess.STATUS_PENDING_DIRECTOR);
            Integer id = pending.getRequisitionId();
            loginAs(director);
            requisitionService.decide(id, false, "Bổ sung ngân sách");
            assertEquals(RequisitionAccess.STATUS_REJECTED, approvalStatusOf(id));
            assertEquals("REQUISITION_REJECTED", latestNotificationType(hiringManager));

            // Act
            loginAs(hiringManager);
            RequisitionRequest form = requisitionService.prepareEditForm(id);
            form.setAction(RequisitionValidator.ACTION_SUBMIT);
            form.setReasonForHiring("Đã bổ sung ngân sách");
            form.setWorkLocation("Hà Nội");
            requisitionService.updateRequisition(id, form);

            // Assert
            assertEquals(RequisitionAccess.STATUS_PENDING_DIRECTOR, approvalStatusOf(id));
            assertEquals("Bổ sung ngân sách", requisitions.findApprovalHistory(id).get(0).getComments());
        }

        @Test
        @DisplayName("Giám đốc tự tạo và gửi -> Duyệt thẳng Approved, có bản ghi phê duyệt")
        void directorSubmit_shouldBeApprovedDirectly_whenDirectorCreatesRequisition() {
            // Arrange
            loginAs(director);
            RequisitionRequest request = fullRequest(RequisitionValidator.ACTION_SUBMIT);
            request.setDepartmentId(department.getDepartmentId());

            // Act
            Integer id = requisitionService.createRequisition(request);

            // Assert
            assertEquals(RequisitionAccess.STATUS_APPROVED, approvalStatusOf(id));
            assertEquals(1, requisitions.findApprovalHistory(id).size());
        }

        @Test
        @DisplayName("HM sao chép yêu cầu đã duyệt -> Lưu thành đợt 2, nối với yêu cầu gốc")
        void copyThenCreate_shouldLinkToOriginal_whenManagerOpensNextRound() {
            // Arrange
            JobRequisition original = savedRequisition(TITLE, hiringManager, RequisitionAccess.STATUS_APPROVED);
            loginAs(hiringManager);

            // Act
            RequisitionRequest copy = requisitionService.copy(original.getRequisitionId());
            copy.setAction(RequisitionValidator.ACTION_DRAFT);
            Integer id = requisitionService.createRequisition(copy);

            // Assert
            entityManager.flush();
            entityManager.clear();
            JobRequisition saved = requisitions.findById(id).orElseThrow();
            assertNull(saved.getExpectedStaffingDate());
            assertEquals(original.getRequisitionId(), saved.getOriginalRequisition().getRequisitionId());
        }

        @Test
        @DisplayName("HR sửa tin nháp và bấm 'Đăng tin' trên form -> Published, nội dung mới được lưu")
        void updateAndPublish_shouldPublishEditedDraft_whenHrSubmitsForm() {
            // Arrange
            JobRequisition approved = savedRequisition(TITLE, hiringManager, RequisitionAccess.STATUS_APPROVED);
            loginAs(hr);
            Integer id = postingService.createJobPosting(postingService.prepareCreateForm(approved.getRequisitionId(), hr), hr);
            JobPostingRequest edit = postingService.prepareEditForm(id, hr);
            edit.setPostingTitle("FLOW Senior Backend");
            edit.setAction(JobPostingValidator.ACTION_PUBLISH);

            // Act
            postingService.updateAndPublishJobPosting(id, edit, hr);

            // Assert
            JobPosting saved = reload(id);
            assertEquals(JobPostingAccess.STATUS_PUBLISHED, saved.getPostingStatus());
            assertEquals("FLOW Senior Backend", saved.getPostingTitle());
            assertNotNull(saved.getPostingDate());
        }

        @Test
        @DisplayName("Form tạo tin lỗi -> Điền lại thông tin nguồn từ DB, giữ nội dung HR đã gõ")
        void restoreSourceInfo_shouldRefillSourceAndKeepInput_whenCreateFormHasErrors() {
            // Arrange
            JobRequisition approved = savedRequisition(TITLE, hiringManager, RequisitionAccess.STATUS_APPROVED);
            JobPostingRequest edited = JobPostingRequest.builder()
                    .requisitionId(approved.getRequisitionId()).postingTitle("HR đã gõ").build();

            // Act
            JobPostingRequest restored = postingService.restoreSourceInfo(edited, hr);

            // Assert
            assertEquals("HR đã gõ", restored.getPostingTitle());
            assertEquals(TITLE, restored.getRequisitionTitle());
            assertEquals(1, restored.getPostingNo());
        }

        @Test
        @DisplayName("HR xóa tin nháp chưa có hồ sơ -> Tin biến mất, yêu cầu lại được chọn để tạo tin")
        void deleteDraftPosting_shouldRemoveRow_whenNoApplications() {
            // Arrange
            JobRequisition approved = savedRequisition(TITLE, hiringManager, RequisitionAccess.STATUS_APPROVED);
            Integer id = savedPosting(approved, 1, JobPostingAccess.STATUS_DRAFT).getJobPostingId();

            // Act
            postingService.deleteJobPosting(id, hr);

            // Assert
            entityManager.flush();
            assertTrue(postings.findById(id).isEmpty());
            assertNotNull(postingService.prepareCreateForm(approved.getRequisitionId(), hr));
        }
    }

    // =========================================================================
    // 2. NGOẠI LỆ / LỖI — YÊU CẦU TUYỂN DỤNG
    // =========================================================================

    @Nested
    @Order(2)
    @DisplayName("2. Ngoại lệ / lỗi: Yêu cầu tuyển dụng")
    class RequisitionErrors {

        @Test
        @DisplayName("HM gửi duyệt thiếu lý do, mô tả, yêu cầu, địa điểm -> Báo lỗi đủ các ô, không ghi DB")
        void submit_shouldRejectAllMissingFields_whenRequiredDataIsBlank() {
            // Arrange
            loginAs(hiringManager);
            long before = requisitions.count();
            RequisitionRequest request = RequisitionRequest.builder().action(RequisitionValidator.ACTION_SUBMIT)
                    .title(TITLE).numberOfPositions(1).employmentType("Full-time").workModel("On-site").build();

            // Act
            RequisitionValidationException ex = assertThrows(RequisitionValidationException.class,
                    () -> requisitionService.createRequisition(request));

            // Assert
            assertTrue(ex.getErrors().keySet().containsAll(
                    List.of("reasonForHiring", "jobDescription", "requirementDetails", "workLocation")));
            assertEquals(before, requisitions.count());
        }

        @Test
        @DisplayName("Lương tối đa < tối thiểu, số lượng = 0 -> Báo lỗi maxSalary và numberOfPositions")
        void submit_shouldRejectInvalidNumbers_whenSalaryRangeAndPositionsAreWrong() {
            // Arrange
            loginAs(hiringManager);
            RequisitionRequest request = fullRequest(RequisitionValidator.ACTION_SUBMIT);
            request.setMinSalary(new BigDecimal("30000000"));
            request.setMaxSalary(new BigDecimal("10000000"));
            request.setNumberOfPositions(0);

            // Act + Assert
            RequisitionValidationException ex = assertThrows(RequisitionValidationException.class,
                    () -> requisitionService.createRequisition(request));
            assertTrue(ex.getErrors().keySet().containsAll(List.of("maxSalary", "numberOfPositions")));
        }

        @Test
        @DisplayName("HM lưu nháp trùng tên với bản nháp đang có trong phòng -> Báo lỗi ô tiêu đề")
        void saveDraft_shouldReject_whenSameTitleDraftExistsInDepartment() {
            // Arrange
            savedRequisition(TITLE, otherManager, RequisitionAccess.STATUS_DRAFT);
            loginAs(hiringManager);

            // Act + Assert
            RequisitionValidationException ex = assertThrows(RequisitionValidationException.class,
                    () -> requisitionService.createRequisition(fullRequest(RequisitionValidator.ACTION_DRAFT)));
            assertTrue(ex.getErrors().containsKey("title"));
        }

        @Test
        @DisplayName("HR tạo yêu cầu tuyển dụng -> Bị từ chối quyền")
        void create_shouldDenyAccess_whenActorIsHr() {
            loginAs(hr);
            assertThrows(AccessDeniedException.class,
                    () -> requisitionService.createRequisition(fullRequest(RequisitionValidator.ACTION_DRAFT)));
        }

        @Test
        @DisplayName("Tài khoản đã bị khóa -> Bị từ chối ngay từ bước xác định người thao tác")
        void create_shouldDenyAccess_whenAccountIsInactive() {
            User locked = savedUser("flow_hm_locked", RequisitionAccess.ROLE_HIRING_MANAGER, department, "Inactive");
            loginAs(locked);
            assertThrows(AccessDeniedException.class,
                    () -> requisitionService.createRequisition(fullRequest(RequisitionValidator.ACTION_DRAFT)));
        }

        @Test
        @DisplayName("HM chưa được gán phòng ban -> Báo lỗi departmentId")
        void create_shouldReject_whenManagerHasNoDepartment() {
            User noDept = savedUser("flow_hm_nodept", RequisitionAccess.ROLE_HIRING_MANAGER, null, "Active");
            loginAs(noDept);
            RequisitionValidationException ex = assertThrows(RequisitionValidationException.class,
                    () -> requisitionService.createRequisition(fullRequest(RequisitionValidator.ACTION_DRAFT)));
            assertTrue(ex.getErrors().containsKey("departmentId"));
        }

        @Test
        @DisplayName("Giám đốc tạo yêu cầu cho phòng ban ngừng hoạt động -> Báo lỗi departmentId")
        void create_shouldReject_whenDirectorPicksInactiveDepartment() {
            Department inactive = departments.save(Department.builder().departmentName("FLOW Phòng đã đóng")
                    .departmentStatus("Inactive").build());
            loginAs(director);
            RequisitionRequest request = fullRequest(RequisitionValidator.ACTION_SUBMIT);
            request.setDepartmentId(inactive.getDepartmentId());

            RequisitionValidationException ex = assertThrows(RequisitionValidationException.class,
                    () -> requisitionService.createRequisition(request));
            assertTrue(ex.getErrors().containsKey("departmentId"));
        }

        @Test
        @DisplayName("HM sửa yêu cầu đang chờ duyệt / đã duyệt -> Lỗi trạng thái, dữ liệu giữ nguyên")
        void edit_shouldFailWithStateError_whenRequisitionIsPendingOrApproved() {
            // Arrange
            Integer pendingId = savedRequisition(TITLE, hiringManager, RequisitionAccess.STATUS_PENDING_DIRECTOR)
                    .getRequisitionId();
            Integer approvedId = savedRequisition(TITLE + " 2", hiringManager, RequisitionAccess.STATUS_APPROVED)
                    .getRequisitionId();
            loginAs(hiringManager);

            // Act + Assert
            assertThrows(RequisitionStateException.class, () -> requisitionService.prepareEditForm(pendingId));
            assertThrows(RequisitionStateException.class,
                    () -> requisitionService.updateRequisition(approvedId, fullRequest(RequisitionValidator.ACTION_DRAFT)));
            assertEquals(RequisitionAccess.STATUS_APPROVED, approvalStatusOf(approvedId));
        }

        @Test
        @DisplayName("HM sửa / xóa yêu cầu của HM khác cùng phòng -> Bị từ chối quyền")
        void editOrDelete_shouldDenyAccess_whenActorIsNotOwner() {
            Integer id = savedRequisition(TITLE, otherManager, RequisitionAccess.STATUS_DRAFT).getRequisitionId();
            loginAs(hiringManager);

            assertThrows(AccessDeniedException.class,
                    () -> requisitionService.updateRequisition(id, fullRequest(RequisitionValidator.ACTION_DRAFT)));
            assertThrows(AccessDeniedException.class, () -> requisitionService.deleteRequisition(id));
        }

        @Test
        @DisplayName("Giám đốc trả lại mà không nhập lý do -> Báo lỗi ô comment, vẫn chờ duyệt")
        void reject_shouldRequireReason_whenCommentIsBlank() {
            Integer id = savedRequisition(TITLE, hiringManager, RequisitionAccess.STATUS_PENDING_DIRECTOR)
                    .getRequisitionId();
            loginAs(director);

            RequisitionValidationException ex = assertThrows(RequisitionValidationException.class,
                    () -> requisitionService.decide(id, false, "   "));
            assertTrue(ex.getErrors().containsKey("comment"));
            assertEquals(RequisitionAccess.STATUS_PENDING_DIRECTOR, approvalStatusOf(id));
        }

        @Test
        @DisplayName("Giám đốc duyệt yêu cầu đã duyệt rồi -> Lỗi trạng thái, không thêm bản ghi duyệt")
        void approve_shouldFailWithStateError_whenRequisitionIsNotPending() {
            Integer id = savedRequisition(TITLE, hiringManager, RequisitionAccess.STATUS_APPROVED).getRequisitionId();
            loginAs(director);

            assertThrows(RequisitionStateException.class, () -> requisitionService.decide(id, true, null));
            assertTrue(requisitions.findApprovalHistory(id).isEmpty());
        }

        @Test
        @DisplayName("HM tự duyệt yêu cầu / Giám đốc duyệt yêu cầu do chính mình tạo -> Bị từ chối quyền")
        void decide_shouldDenyAccess_whenActorIsManagerOrOwner() {
            Integer hmOwned = savedRequisition(TITLE, hiringManager, RequisitionAccess.STATUS_PENDING_DIRECTOR)
                    .getRequisitionId();
            Integer dirOwned = savedRequisition(TITLE + " 2", director, RequisitionAccess.STATUS_PENDING_DIRECTOR)
                    .getRequisitionId();

            loginAs(hiringManager);
            assertThrows(AccessDeniedException.class, () -> requisitionService.decide(hmOwned, true, null));
            loginAs(director);
            assertThrows(AccessDeniedException.class, () -> requisitionService.decide(dirOwned, true, null));
        }

        @Test
        @DisplayName("Xóa yêu cầu đã có tin tuyển dụng -> Báo lỗi, yêu cầu vẫn còn")
        void delete_shouldReject_whenRequisitionHasPosting() {
            JobRequisition rejected = savedRequisition(TITLE, hiringManager, RequisitionAccess.STATUS_REJECTED);
            savedPosting(rejected, 1, JobPostingAccess.STATUS_CLOSED);
            loginAs(hiringManager);

            assertThrows(RequisitionValidationException.class,
                    () -> requisitionService.deleteRequisition(rejected.getRequisitionId()));
            assertTrue(requisitions.findById(rejected.getRequisitionId()).isPresent());
        }

        @Test
        @DisplayName("Sao chép yêu cầu chưa được duyệt -> Lỗi trạng thái")
        void copy_shouldFailWithStateError_whenSourceIsNotApproved() {
            Integer id = savedRequisition(TITLE, hiringManager, RequisitionAccess.STATUS_DRAFT).getRequisitionId();
            loginAs(hiringManager);
            assertThrows(RequisitionStateException.class, () -> requisitionService.copy(id));
        }

        @Test
        @DisplayName("HR xem yêu cầu chưa duyệt -> Bị từ chối; mở mã không tồn tại -> Không tìm thấy")
        void view_shouldFail_whenHrOpensUnapprovedOrIdDoesNotExist() {
            Integer id = savedRequisition(TITLE, hiringManager, RequisitionAccess.STATUS_PENDING_DIRECTOR)
                    .getRequisitionId();
            loginAs(hr);

            assertThrows(AccessDeniedException.class, () -> requisitionService.getById(id));
            assertThrows(ResourceNotFoundException.class, () -> requisitionService.getById(Integer.MAX_VALUE));
        }
    }

    // =========================================================================
    // 3. NGOẠI LỆ / LỖI — TIN TUYỂN DỤNG
    // =========================================================================

    @Nested
    @Order(3)
    @DisplayName("3. Ngoại lệ / lỗi: Tin tuyển dụng")
    class PostingErrors {

        private JobRequisition approved;

        @BeforeEach
        void approvedRequisition() {
            approved = savedRequisition(TITLE, hiringManager, RequisitionAccess.STATUS_APPROVED);
        }

        @Test
        @DisplayName("HR tạo tin từ yêu cầu chưa duyệt / bị trả lại -> Báo lỗi")
        void prepareCreate_shouldReject_whenRequisitionIsNotApproved() {
            Integer pending = savedRequisition(TITLE + " P", hiringManager, RequisitionAccess.STATUS_PENDING_DIRECTOR)
                    .getRequisitionId();
            Integer rejected = savedRequisition(TITLE + " R", hiringManager, RequisitionAccess.STATUS_REJECTED)
                    .getRequisitionId();

            assertThrows(JobPostingValidationException.class, () -> postingService.prepareCreateForm(pending, hr));
            assertThrows(JobPostingValidationException.class, () -> postingService.prepareCreateForm(rejected, hr));
        }

        @Test
        @DisplayName("Yêu cầu không tồn tại / mã rỗng -> Không tìm thấy / báo lỗi mã")
        void prepareCreate_shouldFail_whenRequisitionIdIsMissingOrUnknown() {
            assertThrows(ResourceNotFoundException.class,
                    () -> postingService.prepareCreateForm(Integer.MAX_VALUE, hr));
            assertThrows(JobPostingValidationException.class, () -> postingService.prepareCreateForm(null, hr));

            JobPostingRequest unknown = JobPostingRequest.builder().requisitionId(Integer.MAX_VALUE).build();
            assertThrows(ResourceNotFoundException.class, () -> postingService.createJobPosting(unknown, hr));
        }

        @Test
        @DisplayName("Yêu cầu đã có tin đang mở -> Không tạo được tin thứ hai")
        void create_shouldReject_whenRequisitionAlreadyHasOpenPosting() {
            JobPostingRequest form = postingService.prepareCreateForm(approved.getRequisitionId(), hr);
            savedPosting(approved, 1, JobPostingAccess.STATUS_PAUSED);

            assertThrows(JobPostingValidationException.class, () -> postingService.createJobPosting(form, hr));
            assertEquals(1, postings.findByRequisition_RequisitionIdOrderByPostingNoAsc(approved.getRequisitionId()).size());
        }

        @Test
        @DisplayName("HM / Giám đốc quản lý tin (tạo, đăng, tạm dừng, xóa) -> Bị từ chối quyền")
        void manage_shouldDenyAccess_whenActorIsNotHr() {
            Integer id = savedPosting(approved, 1, JobPostingAccess.STATUS_DRAFT).getJobPostingId();

            assertThrows(AccessDeniedException.class,
                    () -> postingService.prepareCreateForm(approved.getRequisitionId(), hiringManager));
            assertThrows(AccessDeniedException.class, () -> postingService.publishJobPosting(id, hiringManager));
            assertThrows(AccessDeniedException.class, () -> postingService.pauseJobPosting(id, director));
            assertThrows(AccessDeniedException.class, () -> postingService.deleteJobPosting(id, director));
        }

        @Test
        @DisplayName("Đăng tin không có hạn nộp / hạn nộp đã qua -> Báo lỗi ô hạn nộp, không ghi DB")
        void createAndPublish_shouldRejectDeadline_whenMissingOrInPast() {
            JobPostingRequest noDeadline = postingService.prepareCreateForm(approved.getRequisitionId(), hr);
            noDeadline.setApplicationDeadline(null);
            noDeadline.setAction(JobPostingValidator.ACTION_PUBLISH);
            JobPostingValidationException ex1 = assertThrows(JobPostingValidationException.class,
                    () -> postingService.createAndPublishJobPosting(noDeadline, hr));
            assertTrue(ex1.getFieldErrors().containsKey("applicationDeadline"));

            JobPostingRequest past = postingService.prepareCreateForm(approved.getRequisitionId(), hr);
            past.setApplicationDeadline(LocalDate.now(clock).minusDays(1));
            JobPostingValidationException ex2 = assertThrows(JobPostingValidationException.class,
                    () -> postingService.createJobPosting(past, hr));
            assertTrue(ex2.getFieldErrors().containsKey("applicationDeadline"));

            assertTrue(postings.findByRequisition_RequisitionIdOrderByPostingNoAsc(approved.getRequisitionId()).isEmpty());
        }

        @Test
        @DisplayName("Lưu tin thiếu tiêu đề, mô tả, yêu cầu; lương 'tự nhập' để trống -> Báo lỗi đủ các ô")
        void create_shouldRejectAllMissingContent_whenFormIsBlank() {
            JobPostingRequest form = postingService.prepareCreateForm(approved.getRequisitionId(), hr);
            form.setPostingTitle("  ");
            form.setJobDescription(null);
            form.setJobRequirements("");
            form.setSalaryMode(JobPostingRequest.SALARY_CUSTOM);
            form.setSalaryCustom(" ");

            JobPostingValidationException ex = assertThrows(JobPostingValidationException.class,
                    () -> postingService.createJobPosting(form, hr));
            assertTrue(ex.getFieldErrors().keySet().containsAll(
                    List.of("postingTitle", "jobDescription", "jobRequirements", "salaryCustom")));
        }

        @Test
        @DisplayName("Đăng tin nháp đã quá hạn nộp (nút trên trang chi tiết) -> Báo lỗi, vẫn là nháp")
        void publish_shouldReject_whenDraftDeadlineHasPassed() {
            JobPosting draft = savedPosting(approved, 1, JobPostingAccess.STATUS_DRAFT);
            draft.setApplicationDeadline(LocalDate.now(clock).minusDays(1).atTime(23, 59, 59));
            postings.saveAndFlush(draft);

            assertThrows(JobPostingValidationException.class,
                    () -> postingService.publishJobPosting(draft.getJobPostingId(), hr));
            assertEquals(JobPostingAccess.STATUS_DRAFT, reload(draft.getJobPostingId()).getPostingStatus());
        }

        @Test
        @DisplayName("Chuyển trạng thái sai thứ tự: đăng tin đang đăng, tạm dừng tin nháp, đóng tin nháp -> Báo lỗi")
        void transitions_shouldReject_whenStatusDoesNotAllowAction() {
            Integer draftId = savedPosting(approved, 1, JobPostingAccess.STATUS_DRAFT).getJobPostingId();
            JobRequisition other = savedRequisition(TITLE + " 2", hiringManager, RequisitionAccess.STATUS_APPROVED);
            Integer publishedId = savedPosting(other, 1, JobPostingAccess.STATUS_PUBLISHED).getJobPostingId();
            loginAs(hr);

            assertThrows(JobPostingValidationException.class, () -> postingService.publishJobPosting(publishedId, hr));
            assertThrows(JobPostingValidationException.class, () -> postingService.pauseJobPosting(draftId, hr));
            assertThrows(TalentPoolException.class, () -> postingService.closeJobPosting(draftId, hr));
            assertEquals(JobPostingAccess.STATUS_DRAFT, reload(draftId).getPostingStatus());
        }

        @Test
        @DisplayName("HM đóng đợt tuyển -> Bị từ chối quyền, tin vẫn đang đăng")
        void close_shouldDenyAccess_whenActorIsHiringManager() {
            Integer id = savedPosting(approved, 1, JobPostingAccess.STATUS_PUBLISHED).getJobPostingId();
            loginAs(hiringManager);

            assertThrows(AccessDeniedException.class, () -> postingService.closeJobPosting(id, hiringManager));
            assertEquals(JobPostingAccess.STATUS_PUBLISHED, reload(id).getPostingStatus());
        }

        @Test
        @DisplayName("Sửa tin đã đóng -> Báo lỗi; đăng lại tin chưa đóng -> Báo lỗi")
        void editOrRepost_shouldReject_whenStatusIsWrong() {
            Integer closedId = savedPosting(approved, 1, JobPostingAccess.STATUS_CLOSED).getJobPostingId();
            Integer openId = savedPosting(approved, 2, JobPostingAccess.STATUS_PUBLISHED).getJobPostingId();

            assertThrows(JobPostingValidationException.class, () -> postingService.prepareEditForm(closedId, hr));
            assertThrows(JobPostingValidationException.class, () -> postingService.prepareRepostForm(openId, hr));
            // Đã có tin đang mở thì tin đã đóng cũng không đăng lại được
            assertThrows(JobPostingValidationException.class, () -> postingService.prepareRepostForm(closedId, hr));
        }

        @Test
        @DisplayName("Form sửa bị đổi mã yêu cầu nguồn -> Báo lỗi, tin không đổi")
        void update_shouldReject_whenFormSourceIsTampered() {
            Integer id = savedPosting(approved, 1, JobPostingAccess.STATUS_DRAFT).getJobPostingId();
            JobRequisition other = savedRequisition(TITLE + " 2", hiringManager, RequisitionAccess.STATUS_APPROVED);
            JobPostingRequest form = postingService.prepareEditForm(id, hr);
            form.setRequisitionId(other.getRequisitionId());
            form.setPostingTitle("Bị sửa lén");

            assertThrows(JobPostingValidationException.class, () -> postingService.updateJobPosting(id, form, hr));
            assertEquals(TITLE, reload(id).getPostingTitle());
        }

        @Test
        @DisplayName("Xóa tin đang đăng -> Báo lỗi, tin vẫn còn")
        void delete_shouldReject_whenPostingIsNotDraft() {
            Integer id = savedPosting(approved, 1, JobPostingAccess.STATUS_PUBLISHED).getJobPostingId();

            assertThrows(JobPostingValidationException.class, () -> postingService.deleteJobPosting(id, hr));
            assertTrue(postings.findById(id).isPresent());
        }

        @Test
        @DisplayName("Mở / sửa tin không tồn tại -> Không tìm thấy; form lỗi thiếu mã yêu cầu -> Không tìm thấy")
        void lookup_shouldFail_whenPostingOrSourceDoesNotExist() {
            assertThrows(ResourceNotFoundException.class,
                    () -> postingService.getInternalJobPostingDetail(Integer.MAX_VALUE, hr));
            assertThrows(ResourceNotFoundException.class, () -> postingService.pauseJobPosting(Integer.MAX_VALUE, hr));
            assertThrows(ResourceNotFoundException.class,
                    () -> postingService.restoreSourceInfo(new JobPostingRequest(), hr));
        }
    }
}
