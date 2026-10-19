package com.group2.rms.requisition.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.repository.RequisitionApprovalRepository;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
 * UNIT TEST SERVICE: RequisitionServiceImpl (tạo, sửa, xóa, duyệt, rút lại, sao chép, xem chi tiết).
 * Không Spring context, không SQL Server: repository, quyền, thông báo là mock (Mockito).
 *
 * Service được tạo bằng tay trong setUp() để thấy rõ nó cần những gì:
 * - mock: repository, RequisitionAccess, RequisitionValidator, NotificationService
 * - thật: RequisitionMapper, RequisitionAuditLogger (ghi qua AuditLogRepository mock → verify được audit.save)
 * - Clock cố định: "bây giờ" luôn là 18/10/2026 10:00, test không phụ thuộc ngày chạy.
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): tên test = hàm_shouldKếtQuả_whenĐiềuKiện, mỗi test theo Arrange / Act / Assert,
 * kiểm tra cả những gì KHÔNG được xảy ra (không lưu DB, không ghi audit...).
 * Màn danh sách: RequisitionListServiceTests. Cờ quyền trên DTO: RequisitionMapperTests.
 */
@ExtendWith(MockitoExtension.class)
class RequisitionServiceTests {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private JobRequisitionRepository requisitions;
    @Mock
    private DepartmentRepository departments;
    @Mock
    private RequisitionApprovalRepository approvals;
    @Mock
    private JobPostingRepository postings;
    @Mock
    private AuditLogRepository audit;
    @Mock
    private RequisitionAccess access;
    @Mock
    private RequisitionValidator validator;
    @Mock
    private NotificationService notificationService;

    private RequisitionServiceImpl service;

    private User manager;
    private User director;
    private Department itDepartment;

    @BeforeEach
    void setUp() {
        RequisitionMapper mapper = new RequisitionMapper(access, validator, CLOCK);
        RequisitionAuditLogger auditLogger = new RequisitionAuditLogger(audit);
        service = new RequisitionServiceImpl(requisitions, departments, approvals, postings, access, validator,
                mapper, auditLogger, notificationService, CLOCK);

        // HM thuộc phòng IT đang hoạt động: yêu cầu tạo ra luôn thuộc phòng ban này
        itDepartment = Department.builder().departmentId(1).departmentName("IT").departmentStatus("Active").build();
        manager = User.builder().userId(10).username("hm").fullName("Nguyen Van A").department(itDepartment).build();
        director = User.builder().userId(20).username("director").fullName("Director B").build();

        // lenient(): stub dùng chung, test nào không dùng cũng không bị Mockito báo "unnecessary stubbing"
        lenient().when(access.actor()).thenReturn(manager);
        lenient().when(departments.findById(1)).thenReturn(Optional.of(itDepartment));
        lenient().when(requisitions.saveAndFlush(any())).thenAnswer(invocation -> {
            JobRequisition saved = invocation.getArgument(0);
            if (saved.getRequisitionId() == null) {
                saved.setRequisitionId(101);
            }
            return saved;
        });
    }

    // ----- Hàm hỗ trợ -----

    private JobRequisition savedRequisition() {
        ArgumentCaptor<JobRequisition> captor = ArgumentCaptor.forClass(JobRequisition.class);
        verify(requisitions).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private AuditLog savedAudit() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(captor.capture());
        return captor.getValue();
    }

    private RequisitionRequest draft(String title) {
        return RequisitionRequest.builder().action(RequisitionValidator.ACTION_DRAFT).title(title).build();
    }

    private RequisitionRequest submit(String title) {
        return RequisitionRequest.builder().action(RequisitionValidator.ACTION_SUBMIT).title(title).build();
    }

    /** Yêu cầu của HM đang đăng nhập, ở trạng thái cho trước; findForUpdate / findById đều trả về nó. */
    private JobRequisition requisitionWithStatus(int id, String status) {
        JobRequisition requisition = JobRequisition.builder()
                .requisitionId(id)
                .title("Senior Java")
                .recruitmentRound(1)
                .approvalStatus(status)
                .hiringManager(manager)
                .department(itDepartment)
                .screeningCriteria(new ArrayList<>())
                .build();
        lenient().when(requisitions.findForUpdate(id)).thenReturn(Optional.of(requisition));
        lenient().when(requisitions.findById(id)).thenReturn(Optional.of(requisition));
        return requisition;
    }

    // =========================================================================
    // createRequisition
    // =========================================================================

    @Nested
    @DisplayName("createRequisition")
    class CreateRequisition {

        @Test
        @DisplayName("Lưu nháp đủ trường -> Lưu đúng dữ liệu, người tạo, Draft, tiêu chí 2 chiều, audit CREATE")
        void createRequisition_shouldSaveDraftWithAllFields_whenDataIsValid() {
            // Arrange
            RequisitionRequest request = RequisitionRequest.builder()
                    .action(RequisitionValidator.ACTION_DRAFT)
                    .title("Java Engineer")
                    .numberOfPositions(2)
                    .employmentType("Full-time")
                    .minSalary(new BigDecimal("20000000"))
                    .maxSalary(new BigDecimal("35000000"))
                    .gender("Any")
                    .workLocation("Hanoi")
                    .workModel("Hybrid")
                    .probationDays(60)
                    .reasonForHiring("Expansion")
                    .jobDescription("Develop RMS")
                    .requirementDetails("Spring Boot, SQL")
                    .screeningCriteria(new ArrayList<>(List.of(ScreeningCriteriaRequest.builder()
                            .criteriaName("Java").criteriaType("Skill").requiredValue("2+ years")
                            .weight(new BigDecimal("50.00")).isMandatory(true).build())))
                    .build();

            // Act
            Integer id = service.createRequisition(request);

            // Assert
            assertEquals(101, id);
            verify(access).requireCreate(manager);
            verify(validator).validate(request);

            JobRequisition saved = savedRequisition();
            assertEquals(RequisitionAccess.STATUS_DRAFT, saved.getApprovalStatus());
            assertSame(manager, saved.getHiringManager());
            assertNull(saved.getSubmittedAt(), "Draft không được có submittedAt");
            assertEquals("Java Engineer", saved.getTitle());
            assertSame(itDepartment, saved.getDepartment());
            assertEquals(2, saved.getNumberOfPositions());
            assertEquals(new BigDecimal("20000000"), saved.getMinSalary());
            assertEquals(new BigDecimal("35000000"), saved.getMaxSalary());

            assertEquals(1, saved.getScreeningCriteria().size());
            ScreeningCriteria criterion = saved.getScreeningCriteria().get(0);
            assertEquals("Java", criterion.getCriteriaName());
            assertSame(saved, criterion.getRequisition());

            AuditLog log = savedAudit();
            assertEquals(RequisitionAuditLogger.ACTION_CREATE, log.getAction());
            assertEquals("JobRequisition", log.getEntityName());
            assertEquals("101", log.getEntityId());
            assertEquals("Tạo yêu cầu: Java Engineer · Draft", log.getNewValue());
        }

        @Test
        @DisplayName("Nháp chỉ có tên vị trí -> Mô tả / yêu cầu lưu chuỗi rỗng (cột NOT NULL), sinh mã REQ-0101")
        void createRequisition_shouldSaveEmptyTextAndGenerateCode_whenOptionalFieldsAreMissing() {
            // Act
            service.createRequisition(draft("Kế toán tổng hợp"));

            // Assert
            JobRequisition saved = savedRequisition();
            assertEquals("Kế toán tổng hợp", saved.getTitle());
            assertSame(itDepartment, saved.getDepartment());
            assertTrue(saved.getScreeningCriteria().isEmpty());
            assertEquals("", saved.getJobDescription());
            assertEquals("", saved.getRequirementDetails());
            assertEquals("REQ-0101", saved.getRequisitionCode());
        }

        @Test
        @DisplayName("Dòng tiêu chí chỉ có khoảng trắng -> Bị bỏ qua, không lưu")
        void createRequisition_shouldSkipCriteriaRow_whenRowIsSpaceOnly() {
            // Arrange
            RequisitionRequest request = draft("Tester");
            request.setScreeningCriteria(new ArrayList<>(List.of(
                    ScreeningCriteriaRequest.builder().criteriaName("QA").build(),
                    ScreeningCriteriaRequest.builder().criteriaName("   ").build())));

            // Act
            service.createRequisition(request);

            // Assert
            JobRequisition saved = savedRequisition();
            assertEquals(1, saved.getScreeningCriteria().size());
            assertEquals("QA", saved.getScreeningCriteria().get(0).getCriteriaName());
        }

        @Test
        @DisplayName("Tiêu chí mới mang criteriaId có sẵn (ID giả) -> Báo lỗi, không lưu, không audit")
        void createRequisition_shouldReject_whenNewCriteriaHasForgedId() {
            // Arrange
            RequisitionRequest request = draft("Java Engineer");
            request.setScreeningCriteria(new ArrayList<>(List.of(
                    ScreeningCriteriaRequest.builder().criteriaId(99).criteriaName("Java").build())));

            // Act
            RequisitionValidationException error = assertThrows(RequisitionValidationException.class,
                    () -> service.createRequisition(request));

            // Assert
            assertTrue(error.getErrors().containsKey("screeningCriteria"));
            verify(requisitions, never()).saveAndFlush(any());
            verifyNoInteractions(audit);
        }

        @Test
        @DisplayName("Phòng ban đã có bản nháp cùng tên vị trí -> Báo lỗi ở ô tên vị trí, không lưu")
        void createRequisition_shouldReject_whenDraftWithSameTitleExistsInDepartment() {
            // Arrange
            when(requisitions.existsDraftByTitleAndDepartment("Java Engineer", 1, null)).thenReturn(true);

            // Act
            RequisitionValidationException error = assertThrows(RequisitionValidationException.class,
                    () -> service.createRequisition(draft("Java Engineer")));

            // Assert
            assertTrue(error.getErrors().get("title").startsWith("Bạn đang có một bản nháp"));
            verify(requisitions, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Tài khoản chưa có phòng ban -> Báo lỗi dễ hiểu, không lưu")
        void createRequisition_shouldReject_whenCreatorHasNoDepartment() {
            // Arrange
            manager.setDepartment(null);

            // Act
            RequisitionValidationException error = assertThrows(RequisitionValidationException.class,
                    () -> service.createRequisition(draft("Kế toán")));

            // Assert
            assertEquals("Tài khoản của bạn chưa được gán phòng ban đang hoạt động. Vui lòng liên hệ quản trị viên.",
                    error.getErrors().get("departmentId"));
            verify(requisitions, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Phòng ban của người tạo đã ngừng hoạt động -> Không tạo được")
        void createRequisition_shouldReject_whenCreatorDepartmentIsInactive() {
            // Arrange
            itDepartment.setDepartmentStatus("Inactive");

            // Act + Assert
            RequisitionValidationException error = assertThrows(RequisitionValidationException.class,
                    () -> service.createRequisition(draft("Kế toán")));
            assertTrue(error.getErrors().containsKey("departmentId"));
            verify(requisitions, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Form gửi departmentId khác (sửa HTML) -> Bị ghi đè bằng phòng ban của người tạo")
        void createRequisition_shouldIgnoreFormDepartment_whenCreatorIsHiringManager() {
            // Arrange
            RequisitionRequest request = draft("Kế toán");
            request.setDepartmentId(99);

            // Act
            service.createRequisition(request);

            // Assert
            assertEquals(1, request.getDepartmentId());
            verify(departments, never()).findById(99);
        }

        @Test
        @DisplayName("HM gửi duyệt -> Pending_Director, submittedAt = giờ hiện tại, không có bản ghi duyệt")
        void createRequisition_shouldBePendingDirector_whenManagerSubmits() {
            // Act
            service.createRequisition(submit("Java Engineer"));

            // Assert
            JobRequisition saved = savedRequisition();
            assertEquals(RequisitionAccess.STATUS_PENDING_DIRECTOR, saved.getApprovalStatus());
            assertEquals(NOW, saved.getSubmittedAt());
            verifyNoInteractions(approvals);
        }

        @Test
        @DisplayName("Giám đốc gửi -> Approved, phòng ban theo lựa chọn, lưu bản ghi duyệt của chính Giám đốc")
        void createRequisition_shouldApproveDirectly_whenDirectorSubmits() {
            // Arrange
            Department sales = Department.builder().departmentId(2).departmentName("Sales").departmentStatus("Active").build();
            when(departments.findById(2)).thenReturn(Optional.of(sales));
            when(access.isDirector(manager)).thenReturn(true);
            RequisitionRequest request = submit("Sales Lead");
            request.setDepartmentId(2);

            // Act
            service.createRequisition(request);

            // Assert
            JobRequisition saved = savedRequisition();
            assertEquals(RequisitionAccess.STATUS_APPROVED, saved.getApprovalStatus());
            assertEquals(NOW, saved.getSubmittedAt());
            assertSame(sales, saved.getDepartment());

            ArgumentCaptor<RequisitionApproval> approval = ArgumentCaptor.forClass(RequisitionApproval.class);
            verify(approvals).save(approval.capture());
            assertEquals(RequisitionAccess.STATUS_APPROVED, approval.getValue().getStatus());
            assertSame(manager, approval.getValue().getDirector());
            assertEquals("Tạo yêu cầu: Sales Lead · Approved (Giám đốc phê duyệt trực tiếp)", savedAudit().getNewValue());
        }

        @Test
        @DisplayName("Giám đốc lưu nháp -> Vẫn là Draft, không có bản ghi duyệt")
        void createRequisition_shouldStayDraft_whenDirectorSavesDraft() {
            // Arrange
            when(access.isDirector(manager)).thenReturn(true);
            RequisitionRequest request = draft("Sales Lead");
            request.setDepartmentId(1);

            // Act
            service.createRequisition(request);

            // Assert
            assertEquals(RequisitionAccess.STATUS_DRAFT, savedRequisition().getApprovalStatus());
            verifyNoInteractions(approvals);
        }

        @ParameterizedTest(name = "departmentId = {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"5"})
        @DisplayName("Giám đốc chưa chọn phòng ban, hoặc chọn phòng ban ngừng hoạt động -> Báo lỗi, không lưu")
        void createRequisition_shouldReject_whenDirectorPicksNoActiveDepartment(String departmentIdText) {
            // Arrange
            when(access.isDirector(manager)).thenReturn(true);
            lenient().when(departments.findById(5)).thenReturn(Optional.of(
                    Department.builder().departmentId(5).departmentName("Cũ").departmentStatus("Inactive").build()));
            Integer departmentId = null;
            if (departmentIdText != null && !departmentIdText.isEmpty()) {
                departmentId = Integer.valueOf(departmentIdText);
            }
            RequisitionRequest request = submit("Sales Lead");
            request.setDepartmentId(departmentId);

            // Act
            RequisitionValidationException error = assertThrows(RequisitionValidationException.class,
                    () -> service.createRequisition(request));

            // Assert
            assertEquals("Vui lòng chọn phòng ban đang hoạt động.", error.getErrors().get("departmentId"));
            verify(requisitions, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Không có quyền tạo (vào thẳng URL) -> AccessDenied, không validate, không lưu")
        void createRequisition_shouldThrowAccessDenied_whenViewerCannotCreate() {
            // Arrange
            doThrow(new AccessDeniedException("no")).when(access).requireCreate(manager);

            // Act + Assert
            assertThrows(AccessDeniedException.class, () -> service.createRequisition(draft("Java Engineer")));
            verifyNoInteractions(validator);
            verify(requisitions, never()).saveAndFlush(any());
        }
    }

    // =========================================================================
    // createRequisition từ bản sao chép: đợt tuyển
    // =========================================================================

    @Nested
    @DisplayName("createRequisition (sao chép sang đợt mới)")
    class CreateFromCopy {

        private JobRequisition root;

        @BeforeEach
        void setUpRound1() {
            root = requisitionWithStatus(80, RequisitionAccess.STATUS_APPROVED);
            root.setTitle("Java Engineer");
        }

        @Test
        @DisplayName("Giữ tên vị trí (khác hoa/thường, thừa khoảng trắng) -> Trỏ về phiếu gốc, đợt = max + 1")
        void createRequisition_shouldUseNextRound_whenCopyKeepsTitle() {
            // Arrange
            when(requisitions.findMaxRoundInChain(80)).thenReturn(2);
            RequisitionRequest request = draft("  java engineer ");
            request.setOriginalRequisitionId(80);

            // Act
            service.createRequisition(request);

            // Assert
            JobRequisition saved = savedRequisition();
            assertSame(root, saved.getOriginalRequisition());
            assertEquals(3, saved.getRecruitmentRound());
        }

        @Test
        @DisplayName("Đổi tên vị trí -> Yêu cầu mới ở đợt 1, không có phiếu gốc")
        void createRequisition_shouldResetToRound1_whenCopyChangesTitle() {
            // Arrange
            RequisitionRequest request = draft("Python Engineer");
            request.setOriginalRequisitionId(80);

            // Act
            service.createRequisition(request);

            // Assert
            JobRequisition saved = savedRequisition();
            assertNull(saved.getOriginalRequisition());
            assertEquals(1, saved.getRecruitmentRound());
            verify(requisitions, never()).findMaxRoundInChain(any());
        }

        @ParameterizedTest(name = "phiếu nguồn {0}")
        @ValueSource(strings = {RequisitionAccess.STATUS_DRAFT, RequisitionAccess.STATUS_PENDING_DIRECTOR})
        @DisplayName("Phiếu nguồn chưa xong vòng duyệt -> Không sao chép được (sai trạng thái)")
        void createRequisition_shouldReject_whenCopySourceIsNotFinished(String status) {
            // Arrange
            root.setApprovalStatus(status);
            RequisitionRequest request = draft("Java Engineer");
            request.setOriginalRequisitionId(80);

            // Act
            RequisitionValidationException error = assertThrows(RequisitionValidationException.class,
                    () -> service.createRequisition(request));

            // Assert
            assertTrue(error.getErrors().containsKey("action"));
            verify(requisitions, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Phiếu nguồn không tồn tại (id giả) -> Báo lỗi ở originalRequisitionId")
        void createRequisition_shouldReject_whenCopySourceDoesNotExist() {
            // Arrange
            when(requisitions.findById(404)).thenReturn(Optional.empty());
            RequisitionRequest request = draft("Java Engineer");
            request.setOriginalRequisitionId(404);

            // Act
            RequisitionValidationException error = assertThrows(RequisitionValidationException.class,
                    () -> service.createRequisition(request));

            // Assert
            assertEquals("Yêu cầu nguồn để sao chép không tồn tại.", error.getErrors().get("originalRequisitionId"));
        }
    }

    // =========================================================================
    // updateRequisition
    // =========================================================================

    @Nested
    @DisplayName("updateRequisition")
    class UpdateRequisition {

        @Test
        @DisplayName("Admin sửa phiếu của HM -> Phiếu giữ nguyên phòng ban cũ, bỏ departmentId trên form")
        void updateRequisition_shouldKeepSavedDepartment_whenEditorIsNotDirector() {
            // Arrange
            Department sales = Department.builder().departmentId(2).departmentName("Sales").departmentStatus("Active").build();
            when(departments.findById(2)).thenReturn(Optional.of(sales));
            JobRequisition requisition = requisitionWithStatus(70, RequisitionAccess.STATUS_DRAFT);
            requisition.setDepartment(sales);
            RequisitionRequest request = draft("Sales Executive");
            request.setDepartmentId(1);

            // Act
            service.updateRequisition(70, request);

            // Assert
            assertEquals(2, request.getDepartmentId());
            assertSame(sales, requisition.getDepartment());
        }

        @Test
        @DisplayName("Giám đốc sửa bản nháp và phê duyệt -> Approved, đổi được phòng ban, lưu bản ghi duyệt")
        void updateRequisition_shouldApproveDirectly_whenDirectorSubmits() {
            // Arrange
            Department sales = Department.builder().departmentId(2).departmentName("Sales").departmentStatus("Active").build();
            when(departments.findById(2)).thenReturn(Optional.of(sales));
            when(access.isDirector(manager)).thenReturn(true);
            JobRequisition requisition = requisitionWithStatus(90, RequisitionAccess.STATUS_DRAFT);
            RequisitionRequest request = submit("Senior Java");
            request.setDepartmentId(2);

            // Act
            service.updateRequisition(90, request);

            // Assert
            assertEquals(RequisitionAccess.STATUS_APPROVED, requisition.getApprovalStatus());
            assertSame(sales, requisition.getDepartment());
            verify(approvals).save(any(RequisitionApproval.class));
        }

        @Test
        @DisplayName("Bản nháp đợt 2 bị đổi tên vị trí -> Về đợt 1, bỏ phiếu gốc, audit ghi 'Đợt tuyển dụng: 2 → 1'")
        void updateRequisition_shouldResetToRound1_whenCopiedDraftChangesTitle() {
            // Arrange
            JobRequisition root = JobRequisition.builder().requisitionId(80).title("Java Engineer").recruitmentRound(1).build();
            JobRequisition requisition = requisitionWithStatus(81, RequisitionAccess.STATUS_DRAFT);
            requisition.setTitle("Java Engineer");
            requisition.setRecruitmentRound(2);
            requisition.setOriginalRequisition(root);

            // Act
            service.updateRequisition(81, draft("Python Engineer"));

            // Assert
            assertNull(requisition.getOriginalRequisition());
            assertEquals(1, requisition.getRecruitmentRound());
            assertTrue(savedAudit().getNewValue().contains("Đợt tuyển dụng: 2 → 1"));
        }

        @Test
        @DisplayName("Không đổi gì so với bản đã lưu -> Không lưu lại, không ghi audit")
        void updateRequisition_shouldNotSaveOrAudit_whenNothingChanged() {
            // Arrange
            JobRequisition requisition = requisitionWithStatus(72, RequisitionAccess.STATUS_DRAFT);
            requisition.setJobDescription("");
            requisition.setRequirementDetails("");

            // Act
            service.updateRequisition(72, draft("Senior Java"));

            // Assert
            verify(requisitions, never()).saveAndFlush(any());
            verifyNoInteractions(audit);
        }

        @Test
        @DisplayName("Sửa tiêu chí bằng criteriaId của yêu cầu khác (ID giả) -> Báo lỗi, không ghi audit")
        void updateRequisition_shouldReject_whenCriteriaIdBelongsToAnotherRequisition() {
            // Arrange
            requisitionWithStatus(73, RequisitionAccess.STATUS_DRAFT);
            RequisitionRequest request = draft("Senior Java");
            request.setScreeningCriteria(new ArrayList<>(List.of(
                    ScreeningCriteriaRequest.builder().criteriaId(999).criteriaName("Java").build())));

            // Act
            RequisitionValidationException error = assertThrows(RequisitionValidationException.class,
                    () -> service.updateRequisition(73, request));

            // Assert
            assertEquals("Một tiêu chí không thuộc về yêu cầu này.", error.getErrors().get("screeningCriteria"));
            verifyNoInteractions(audit);
        }

        @Test
        @DisplayName("Không có quyền sửa (sai người / sai trạng thái) -> AccessDenied, không validate")
        void updateRequisition_shouldThrowAccessDenied_whenViewerCannotEdit() {
            // Arrange
            JobRequisition requisition = requisitionWithStatus(74, RequisitionAccess.STATUS_APPROVED);
            doThrow(new AccessDeniedException("no")).when(access).requireEdit(manager, requisition);

            // Act + Assert
            assertThrows(AccessDeniedException.class, () -> service.updateRequisition(74, draft("Senior Java")));
            verifyNoInteractions(validator);
        }

        @Test
        @DisplayName("Id không tồn tại -> ResourceNotFoundException (trang 404)")
        void updateRequisition_shouldThrowNotFound_whenIdDoesNotExist() {
            // Arrange
            when(requisitions.findForUpdate(404)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(ResourceNotFoundException.class, () -> service.updateRequisition(404, draft("Senior Java")));
        }
    }

    // =========================================================================
    // copy, getById
    // =========================================================================

    @Nested
    @DisplayName("copy và getById")
    class ReadOperations {

        @Test
        @DisplayName("Sao chép yêu cầu đã duyệt -> Form mới bỏ criteriaId, gợi ý đợt tiếp theo, không ghi DB")
        void copy_shouldClearCriteriaIdsAndSuggestNextRound_whenSourceIsApproved() {
            // Arrange
            JobRequisition source = requisitionWithStatus(80, RequisitionAccess.STATUS_APPROVED);
            source.getScreeningCriteria().add(ScreeningCriteria.builder().criteriaId(5).criteriaName("Java").build());
            when(requisitions.findMaxRoundInChain(80)).thenReturn(1);

            // Act
            RequisitionRequest copied = service.copy(80);

            // Assert
            assertNull(copied.getScreeningCriteria().get(0).getCriteriaId());
            assertEquals(80, copied.getOriginalRequisitionId());
            assertEquals(2, copied.getRecruitmentRound());
            verify(requisitions, never()).saveAndFlush(any());
        }

        @ParameterizedTest(name = "phiếu {0}")
        @ValueSource(strings = {RequisitionAccess.STATUS_DRAFT, RequisitionAccess.STATUS_PENDING_DIRECTOR})
        @DisplayName("Sao chép yêu cầu chưa xong vòng duyệt -> Báo lỗi (sai trạng thái)")
        void copy_shouldReject_whenSourceIsNotFinished(String status) {
            // Arrange
            requisitionWithStatus(80, status);

            // Act + Assert
            RequisitionValidationException error = assertThrows(RequisitionValidationException.class, () -> service.copy(80));
            assertTrue(error.getErrors().containsKey("action"));
        }

        @Test
        @DisplayName("Xem chi tiết id không tồn tại (gõ URL) -> ResourceNotFoundException")
        void getById_shouldThrowNotFound_whenIdDoesNotExist() {
            // Arrange
            when(requisitions.findById(404)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(ResourceNotFoundException.class, () -> service.getById(404));
        }

        @Test
        @DisplayName("Xem chi tiết không đủ quyền -> AccessDenied, không tải lịch sử duyệt / audit")
        void getById_shouldThrowAccessDenied_whenViewerCannotView() {
            // Arrange
            JobRequisition requisition = requisitionWithStatus(61, RequisitionAccess.STATUS_DRAFT);
            doThrow(new AccessDeniedException("no")).when(access).requireView(manager, requisition);

            // Act + Assert
            assertThrows(AccessDeniedException.class, () -> service.getById(61));
            verifyNoInteractions(approvals, audit);
        }
    }

    // =========================================================================
    // decide (Giám đốc duyệt / trả lại)
    // =========================================================================

    @Nested
    @DisplayName("decide")
    class Decide {

        private JobRequisition pending;

        @BeforeEach
        void setUpPendingRequisition() {
            when(access.actor()).thenReturn(director);
            pending = requisitionWithStatus(50, RequisitionAccess.STATUS_PENDING_DIRECTOR);
            lenient().when(access.canDecide(director, pending)).thenReturn(true);
            lenient().when(approvals.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("Giám đốc duyệt -> Approved, lưu bản ghi duyệt, audit, KHÔNG gửi thông báo trả lại")
        void decide_shouldApproveAndNotNotify_whenDirectorApproves() {
            // Act
            service.decide(50, true, "Looks good");

            // Assert
            assertEquals(RequisitionAccess.STATUS_APPROVED, pending.getApprovalStatus());
            verify(approvals).save(any(RequisitionApproval.class));
            assertEquals("Status: Pending_Director → Approved\nÝ kiến Giám đốc: Looks good", savedAudit().getNewValue());
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("Giám đốc duyệt không ghi ý kiến -> Vẫn duyệt được (ý kiến chỉ bắt buộc khi trả lại)")
        void decide_shouldApprove_whenCommentIsBlank() {
            // Act
            service.decide(50, true, "   ");

            // Assert
            assertEquals(RequisitionAccess.STATUS_APPROVED, pending.getApprovalStatus());
        }

        @Test
        @DisplayName("Giám đốc trả lại có lý do -> Rejected, lưu ý kiến, gửi thông báo cho HM")
        void decide_shouldRejectAndNotifyManager_whenReasonIsGiven() {
            // Act
            service.decide(50, false, "  Budget exceeded  ");

            // Assert
            assertEquals(RequisitionAccess.STATUS_REJECTED, pending.getApprovalStatus());
            ArgumentCaptor<RequisitionApproval> decision = ArgumentCaptor.forClass(RequisitionApproval.class);
            verify(approvals).save(decision.capture());
            assertEquals("Budget exceeded", decision.getValue().getComments());
            assertSame(director, decision.getValue().getDirector());
            verify(requisitions).saveAndFlush(pending);
            verify(notificationService).notifyRequisitionRejected(decision.getValue());
        }

        @ParameterizedTest(name = "lý do = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t\n"})
        @DisplayName("Trả lại mà lý do null / rỗng / chỉ khoảng trắng -> Báo lỗi, giữ Pending, không thông báo")
        void decide_shouldReject_whenRejectReasonIsBlank(String comment) {
            // Act
            RequisitionValidationException error = assertThrows(RequisitionValidationException.class,
                    () -> service.decide(50, false, comment));

            // Assert
            assertEquals("Vui lòng nhập lý do để Hiring Manager chỉnh sửa.", error.getErrors().get("comment"));
            assertEquals(RequisitionAccess.STATUS_PENDING_DIRECTOR, pending.getApprovalStatus());
            verifyNoInteractions(approvals, notificationService);
        }

        @Test
        @DisplayName("Ý kiến đúng bằng giới hạn ký tự -> Hợp lệ")
        void decide_shouldAccept_whenCommentLengthEqualsLimit() {
            // Act
            service.decide(50, false, "x".repeat(RequisitionValidator.COMMENT_CHAR_LIMIT));

            // Assert
            assertEquals(RequisitionAccess.STATUS_REJECTED, pending.getApprovalStatus());
        }

        @Test
        @DisplayName("Ý kiến vượt giới hạn 1 ký tự -> Báo lỗi 'Nội dung quá N ký tự'")
        void decide_shouldReject_whenCommentExceedsLimitByOne() {
            // Act
            RequisitionValidationException error = assertThrows(RequisitionValidationException.class,
                    () -> service.decide(50, false, "x".repeat(RequisitionValidator.COMMENT_CHAR_LIMIT + 1)));

            // Assert
            assertEquals("Nội dung quá " + RequisitionValidator.COMMENT_CHAR_LIMIT + " ký tự", error.getErrors().get("comment"));
            verifyNoInteractions(approvals);
        }

        @Test
        @DisplayName("Người không được duyệt (không phải Giám đốc / tự duyệt / sai trạng thái) -> AccessDenied")
        void decide_shouldThrowAccessDenied_whenViewerCannotDecide() {
            // Arrange
            when(access.canDecide(director, pending)).thenReturn(false);

            // Act + Assert
            assertThrows(AccessDeniedException.class, () -> service.decide(50, true, "ok"));
            assertEquals(RequisitionAccess.STATUS_PENDING_DIRECTOR, pending.getApprovalStatus());
            verifyNoInteractions(approvals, audit, notificationService);
        }
    }

    // =========================================================================
    // withdraw, deleteRequisition
    // =========================================================================

    @Nested
    @DisplayName("withdraw và deleteRequisition")
    class WithdrawAndDelete {

        @Test
        @DisplayName("Người tạo rút lại yêu cầu đang chờ duyệt -> Về Draft, ghi audit")
        void withdraw_shouldReturnToDraft_whenOwnerWithdrawsPending() {
            // Arrange
            JobRequisition requisition = requisitionWithStatus(55, RequisitionAccess.STATUS_PENDING_DIRECTOR);
            when(access.canWithdraw(manager, requisition)).thenReturn(true);

            // Act
            service.withdraw(55);

            // Assert
            assertEquals(RequisitionAccess.STATUS_DRAFT, requisition.getApprovalStatus());
            assertEquals("Status: Pending_Director → Draft (rút lại)", savedAudit().getNewValue());
        }

        @Test
        @DisplayName("Không phải người tạo, hoặc không còn chờ duyệt -> AccessDenied, không đổi trạng thái")
        void withdraw_shouldThrowAccessDenied_whenViewerCannotWithdraw() {
            // Arrange
            JobRequisition requisition = requisitionWithStatus(56, RequisitionAccess.STATUS_PENDING_DIRECTOR);
            when(access.canWithdraw(manager, requisition)).thenReturn(false);

            // Act + Assert
            assertThrows(AccessDeniedException.class, () -> service.withdraw(56));
            assertEquals(RequisitionAccess.STATUS_PENDING_DIRECTOR, requisition.getApprovalStatus());
            verifyNoInteractions(audit);
        }

        @Test
        @DisplayName("Không có tin tuyển dụng, không có đợt sau -> Xóa lịch sử duyệt rồi xóa phiếu, ghi audit DELETE")
        void deleteRequisition_shouldDelete_whenNothingDependsOnIt() {
            // Arrange
            JobRequisition requisition = requisitionWithStatus(60, RequisitionAccess.STATUS_REJECTED);
            when(approvals.findByRequisition_RequisitionIdOrderByApprovalDateDesc(60)).thenReturn(List.of());

            // Act
            service.deleteRequisition(60);

            // Assert
            verify(access).requireEdit(manager, requisition);
            assertEquals(RequisitionAuditLogger.ACTION_DELETE, savedAudit().getAction());
            verify(approvals).deleteAll(List.of());
            verify(requisitions).delete(requisition);
        }

        @Test
        @DisplayName("Đã có tin tuyển dụng -> Không xóa, báo lỗi rõ ràng")
        void deleteRequisition_shouldReject_whenPostingExists() {
            // Arrange
            requisitionWithStatus(60, RequisitionAccess.STATUS_REJECTED);
            when(postings.existsByRequisition_RequisitionId(60)).thenReturn(true);

            // Act
            RequisitionValidationException error = assertThrows(RequisitionValidationException.class,
                    () -> service.deleteRequisition(60));

            // Assert
            assertEquals("Yêu cầu này đã được liên kết với tin đăng tuyển và không thể xóa.", error.getErrors().get("action"));
            verify(requisitions, never()).delete(any(JobRequisition.class));
        }

        @Test
        @DisplayName("Đã có đợt tuyển sau sao chép từ nó -> Không xóa, báo lỗi rõ ràng")
        void deleteRequisition_shouldReject_whenLaterRoundExists() {
            // Arrange
            requisitionWithStatus(60, RequisitionAccess.STATUS_REJECTED);
            when(requisitions.existsByOriginalRequisition_RequisitionId(60)).thenReturn(true);

            // Act
            RequisitionValidationException error = assertThrows(RequisitionValidationException.class,
                    () -> service.deleteRequisition(60));

            // Assert
            assertEquals("Yêu cầu này đã có đợt tuyển sau được sao chép từ nó và không thể xóa.",
                    error.getErrors().get("action"));
            verify(requisitions, never()).delete(any(JobRequisition.class));
            verifyNoInteractions(audit);
        }
    }
}
