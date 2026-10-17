package com.group2.rms.requisition.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.repository.RequisitionApprovalRepository;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
 * STAGE 2 — SERVICE UNIT TEST.
 * Đối tượng production: RequisitionServiceImpl, triển khai RequisitionService.
 * Setup: MockitoExtension, @Mock, @InjectMocks; không Spring context/SQL Server.
 * Mock dependencies thật đang có, không đổi kiến trúc chỉ để viết test.
 * Mỗi test theo Arrange / Act / Assert, kiểm tra state và side effects quan trọng.
 *
 * [SVC-01] Create Draft đầy đủ/thiếu field: dữ liệu, Hiring Manager, status, audit đúng. (COMPLETED)
 * TODO [SVC-02] Submit hợp lệ -> Pending_Director, submittedAt và workflow event đúng.
 * TODO [SVC-03] Submit không hợp lệ -> không saveAndFlush/event/audit sai;
 *               department không tồn tại trả lỗi field departmentId.
 * TODO [SVC-04] Update Draft/Rejected; chặn actor hoặc trạng thái không được sửa.
 * TODO [SVC-05] ID không tồn tại: exception thực tế, hiện là ResponseStatusException 404.
 * TODO [SVC-07] Approve/reject: đúng actor/status/record/timestamp/event;
 *               chặn sai role, tự duyệt, non-Pending, reject thiếu feedback, comment quá dài.
 * TODO [SVC-08] Withdraw: chủ request Pending được rút về Draft; sai chủ/trạng thái bị chặn.
 * TODO [SVC-09] Copy chỉ chuẩn bị form, không ghi DB; bỏ criteriaId, giữ nguyên nguồn.
 *               Khi lưu hoặc submit bản copy mới gọi create với action tương ứng.
 * TODO [SVC-10] Delete: quyền/trạng thái; Job Posting liên kết chặn xóa;
 *               xóa phụ thuộc cần thiết và giữ audit history.
 * TODO [SVC-11] Criteria thêm/sửa/xóa; chặn ID của request khác hoặc dùng ID cũ khi create;
 *               map đúng mandatory/weight và các field còn lại.
 * TODO [SVC-12] Map request/entity/response: workModel, probationDays, salary, date,
 *               gender, location và field tùy chọn.
 * TODO [SVC-13] Update không đổi không ghi audit/save thừa; có đổi chỉ log đúng field thay đổi.
 * TODO [SVC-14] Search chuẩn hóa page/size/sort và Pageable; SQL lọc/sắp xếp thật ở Stage 4.
 * TODO [ACCESS] Viết RequisitionAccessTests riêng để kiểm tra role + ownership.
 */
@ExtendWith(MockitoExtension.class)
class JobRequisitionServiceTest {

    @Mock private JobRequisitionRepository requisitions;
    @Mock private DepartmentRepository departments;
    @Mock private AuditLogRepository audit;
    @Mock private RequisitionApprovalRepository approvals;
    @Mock private RequisitionAccess access;
    @Mock private RequisitionValidator validator;
    @Mock private NotificationService notificationService;
    @Mock private JobPostingRepository postings;

    @InjectMocks private RequisitionServiceImpl service;

    private User manager;
    private Department itDepartment;

    @BeforeEach
    void setUp() {
        // HM thuộc phòng IT đang hoạt động: yêu cầu tạo ra luôn thuộc phòng ban này
        itDepartment = Department.builder().departmentId(1).departmentName("IT").departmentStatus("Active").build();
        manager = User.builder().userId(10).username("hm").fullName("Nguyen Van A").department(itDepartment).build();
        lenient().when(access.actor()).thenReturn(manager);
        lenient().when(departments.findById(1)).thenReturn(Optional.of(itDepartment));
        lenient().when(requisitions.saveAndFlush(any())).thenAnswer(inv -> {
            JobRequisition r = inv.getArgument(0);
            r.setRequisitionId(101);
            return r;
        });
    }

    // =========================================================================
    // [SVC-01] Create Draft đầy đủ / thiếu field
    // =========================================================================

    @Test
    @DisplayName("SVC-01: Draft đầy đủ field -> Lưu đúng dữ liệu, manager, status Draft, audit CREATE")
    void svc01_createDraft_fullFields() {
        Department dept = Department.builder().departmentId(1).departmentName("IT").build();
        when(departments.findById(1)).thenReturn(Optional.of(dept));

        var req = RequisitionRequest.builder()
                .action("draft")
                .title("Java Engineer")
                .departmentId(1)
                .numberOfPositions(2)
                .employmentType("Full-time")
                .minSalary(new BigDecimal("20000000"))
                .maxSalary(new BigDecimal("35000000"))
                .gender("Any")
                .workLocation("Hanoi")
                .workModel("Hybrid")
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusMonths(1))
                .reasonForHiring("Expansion")
                .jobDescription("Develop RMS")
                .requirementDetails("Spring Boot, SQL")
                .screeningCriteria(new ArrayList<>(List.of(
                        ScreeningCriteriaRequest.builder()
                                .criteriaName("Java").criteriaType("Skill")
                                .requiredValue("2+ years").weight(new BigDecimal("50.00")).isMandatory(true).build()
                ))).build();

        assertEquals(101, service.createRequisition(req));

        verify(access).requireCreate(manager);
        verify(validator).validate(req);

        // Kiểm tra state đã lưu
        var reqCap = ArgumentCaptor.forClass(JobRequisition.class);
        verify(requisitions).saveAndFlush(reqCap.capture());
        JobRequisition saved = reqCap.getValue();

        assertEquals("Draft", saved.getApprovalStatus());
        assertSame(manager, saved.getHiringManager());
        assertNull(saved.getSubmittedAt(), "Draft không được có submittedAt");
        assertEquals("Java Engineer", saved.getTitle());
        assertSame(dept, saved.getDepartment());
        assertEquals(2, saved.getNumberOfPositions());
        assertEquals("Full-time", saved.getEmploymentType());
        assertEquals(new BigDecimal("20000000"), saved.getMinSalary());
        assertEquals(new BigDecimal("35000000"), saved.getMaxSalary());

        // Tiêu chí liên kết 2 chiều
        assertEquals(1, saved.getScreeningCriteria().size());
        ScreeningCriteria sc = saved.getScreeningCriteria().get(0);
        assertEquals("Java", sc.getCriteriaName());
        assertSame(saved, sc.getRequisition());

        // Audit log CREATE
        var auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(auditCap.capture());
        AuditLog log = auditCap.getValue();
        assertEquals("CREATE", log.getAction());
        assertEquals("JobRequisition", log.getEntityName());
        assertEquals("101", log.getEntityId());
        assertEquals("Tạo yêu cầu: Java Engineer · Draft", log.getNewValue());
    }

    @Test
    @DisplayName("SVC-01: Draft thiếu field -> Gán default title 'Untitled requisition', null các field còn lại")
    void svc01_createDraft_missingOptionalFields_savesEmptyTextAndGeneratesCode() {
        // Validator đã bắt buộc Tên vị trí khi lưu nháp; ở đây chỉ kiểm tra service xử lý các trường tùy chọn bị trống
        var req = RequisitionRequest.builder().action("draft").title("Kế toán tổng hợp").build();

        assertEquals(101, service.createRequisition(req));

        var reqCap = ArgumentCaptor.forClass(JobRequisition.class);
        verify(requisitions).saveAndFlush(reqCap.capture());
        JobRequisition saved = reqCap.getValue();

        assertEquals("Kế toán tổng hợp", saved.getTitle());
        assertEquals("Draft", saved.getApprovalStatus());
        assertSame(manager, saved.getHiringManager());
        // Form không gửi phòng ban: service lấy phòng ban của người tạo
        assertSame(itDepartment, saved.getDepartment());
        assertNull(saved.getSubmittedAt());
        assertTrue(saved.getScreeningCriteria().isEmpty());
        // Rule: JobDescription / RequirementDetails NOT NULL trong DB → nháp để trống thì lưu chuỗi rỗng
        assertEquals("", saved.getJobDescription());
        assertEquals("", saved.getRequirementDetails());
        // Rule: mã yêu cầu sinh từ id sau lần lưu đầu: REQ- + id đệm 4 chữ số
        assertEquals("REQ-0101", saved.getRequisitionCode());

        var auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(auditCap.capture());
        assertEquals("Tạo yêu cầu: Kế toán tổng hợp · Draft", auditCap.getValue().getNewValue());
    }

    @Test
    @DisplayName("SVC-01: Draft có dòng criteria trắng -> Lọc bỏ tiêu chí trắng")
    void svc01_createDraft_filtersBlankCriteria() {
        var req = RequisitionRequest.builder()
                .action("draft")
                .title("Tester")
                .screeningCriteria(new ArrayList<>(List.of(
                        ScreeningCriteriaRequest.builder().criteriaName("QA").build(),
                        ScreeningCriteriaRequest.builder().criteriaName("   ").build() // blank
                ))).build();

        service.createRequisition(req);

        var reqCap = ArgumentCaptor.forClass(JobRequisition.class);
        verify(requisitions).saveAndFlush(reqCap.capture());
        assertEquals(1, reqCap.getValue().getScreeningCriteria().size());
        assertEquals("QA", reqCap.getValue().getScreeningCriteria().get(0).getCriteriaName());
    }

    @Test
    @DisplayName("SVC-01: Draft có criteria chứa criteriaId cũ -> Ném lỗi và không lưu DB")
    void svc01_createDraft_rejectsExistingCriteriaId() {
        var req = RequisitionRequest.builder()
                .action("draft")
                .screeningCriteria(new ArrayList<>(List.of(
                        ScreeningCriteriaRequest.builder().criteriaId(99).criteriaName("Java").build()
                ))).build();

        var ex = assertThrows(RequisitionValidationException.class, () -> service.createRequisition(req));
        assertTrue(ex.getErrors().containsKey("screeningCriteria"));

        verify(requisitions, never()).saveAndFlush(any());
        verifyNoInteractions(audit);
    }

    @Test
    @DisplayName("SVC-01: Tài khoản chưa có phòng ban -> Báo lỗi dễ hiểu, không lưu DB")
    void svc01_create_rejectedWhenCreatorHasNoDepartment() {
        manager.setDepartment(null);
        var req = RequisitionRequest.builder().action("draft").title("Kế toán").build();

        var ex = assertThrows(RequisitionValidationException.class, () -> service.createRequisition(req));

        assertEquals("Tài khoản của bạn chưa được gán phòng ban đang hoạt động. Vui lòng liên hệ quản trị viên.",
                ex.getErrors().get("departmentId"));
        verify(requisitions, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("SVC-01: Phòng ban của người tạo đã ngừng hoạt động -> Không tạo được")
    void svc01_create_rejectedWhenDepartmentInactive() {
        itDepartment.setDepartmentStatus("Inactive");
        var req = RequisitionRequest.builder().action("draft").title("Kế toán").build();

        var ex = assertThrows(RequisitionValidationException.class, () -> service.createRequisition(req));

        assertTrue(ex.getErrors().containsKey("departmentId"));
        verify(requisitions, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("SVC-01: Form gửi departmentId khác -> Bị ghi đè bằng phòng ban của người tạo")
    void svc01_create_ignoresDepartmentFromForm() {
        var req = RequisitionRequest.builder().action("draft").title("Kế toán").departmentId(99).build();

        service.createRequisition(req);

        assertEquals(1, req.getDepartmentId());
        verify(departments, never()).findById(99);
    }

    @Test
    @DisplayName("SVC-04: Admin sửa phiếu của HM -> Phiếu giữ nguyên phòng ban cũ")
    void svc04_update_keepsOriginalDepartment() {
        Department salesDepartment = Department.builder().departmentId(2).departmentName("Sales").departmentStatus("Active").build();
        User admin = User.builder().userId(1).username("admin").fullName("Admin").department(itDepartment).build();
        when(access.actor()).thenReturn(admin);
        when(departments.findById(2)).thenReturn(Optional.of(salesDepartment));

        JobRequisition req = JobRequisition.builder()
                .requisitionId(70)
                .title("Sales Executive")
                .approvalStatus("Draft")
                .hiringManager(manager)
                .department(salesDepartment)
                .screeningCriteria(new ArrayList<>())
                .build();
        when(requisitions.findForUpdate(70)).thenReturn(Optional.of(req));

        var dto = RequisitionRequest.builder().action("draft").title("Sales Executive").departmentId(1).build();
        service.updateRequisition(70, dto);

        assertEquals(2, dto.getDepartmentId());
        assertSame(salesDepartment, req.getDepartment());
    }

    // =========================================================================
    // [SVC-07] Decide: Reject -> Tạo thông báo riêng cho Hiring Manager
    // =========================================================================

    @Test
    @DisplayName("SVC-07: Director reject -> Chuyển Rejected, lưu approval, log event và gửi notification cho HM")
    void svc07_reject_notifiesHiringManager() {
        User director = User.builder().userId(20).username("director").fullName("Director B").build();
        when(access.actor()).thenReturn(director);

        JobRequisition req = JobRequisition.builder()
                .requisitionId(50)
                .title("Senior Java")
                .approvalStatus("Pending_Director")
                .hiringManager(manager)
                .screeningCriteria(new ArrayList<>())
                .build();

        when(requisitions.findForUpdate(50)).thenReturn(Optional.of(req));
        when(access.canDecide(director, req)).thenReturn(true);

        service.decide(50, false, "Budget exceeded");

        assertEquals("Rejected", req.getApprovalStatus());
        ArgumentCaptor<RequisitionApproval> decision = ArgumentCaptor.forClass(RequisitionApproval.class);
        verify(approvals).save(decision.capture());
        assertEquals("Budget exceeded", decision.getValue().getComments());
        assertEquals(director, decision.getValue().getDirector());
        verify(requisitions).saveAndFlush(req);
        verify(notificationService).notifyRequisitionRejected(any());
    }

    // =========================================================================
    // [SVC-10] Delete: chặn khi còn dữ liệu phụ thuộc
    // =========================================================================

    /** Phiếu bị trả lại của chính HM đang đăng nhập: được phép sửa / xóa. */
    private JobRequisition rejectedRequisition() {
        JobRequisition req = JobRequisition.builder()
                .requisitionId(60)
                .title("Senior Java")
                .approvalStatus("Rejected")
                .hiringManager(manager)
                .screeningCriteria(new ArrayList<>())
                .build();
        when(requisitions.findForUpdate(60)).thenReturn(Optional.of(req));
        return req;
    }

    @Test
    @DisplayName("SVC-10: Phiếu đã có đợt tuyển sau sao chép từ nó -> Không xóa, báo lỗi rõ ràng")
    void svc10_delete_blockedWhenLaterRoundExists() {
        rejectedRequisition();
        when(postings.existsByRequisition_RequisitionId(60)).thenReturn(false);
        when(requisitions.existsByOriginalRequisition_RequisitionId(60)).thenReturn(true);

        var ex = assertThrows(RequisitionValidationException.class, () -> service.deleteRequisition(60));

        assertEquals("Yêu cầu này đã có đợt tuyển sau được sao chép từ nó và không thể xóa.", ex.getErrors().get("action"));
        verify(requisitions, never()).delete(any(JobRequisition.class));
        verifyNoInteractions(audit);
    }

    @Test
    @DisplayName("SVC-10: Phiếu không có tin tuyển dụng, không có đợt sau -> Xóa lịch sử duyệt rồi xóa phiếu, ghi audit")
    void svc10_delete_succeedsWhenNothingDepends() {
        JobRequisition req = rejectedRequisition();
        when(postings.existsByRequisition_RequisitionId(60)).thenReturn(false);
        when(requisitions.existsByOriginalRequisition_RequisitionId(60)).thenReturn(false);
        when(approvals.findByRequisition_RequisitionIdOrderByApprovalDateDesc(60)).thenReturn(List.of());

        service.deleteRequisition(60);

        verify(access).requireEdit(manager, req);
        verify(audit).save(any(AuditLog.class));
        verify(approvals).deleteAll(List.of());
        verify(requisitions).delete(req);
    }

    // =========================================================================
    // [SVC-HR] Scope: HR chỉ truy vấn Requisition Approved
    // =========================================================================

    @Test
    @DisplayName("SVC-HR: Role HR gọi search -> Áp dụng scope chỉ Approved")
    void svc_hr_searchScope_onlyApproved() {
        User hrUser = User.builder().userId(30).username("hr_user").fullName("HR Specialist").build();
        when(access.actor()).thenReturn(hrUser);
        when(access.role(hrUser)).thenReturn("HR");

        when(requisitions.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        service.getListPage(new com.group2.rms.requisition.dto.RequisitionSearchRequest(
                null, null, null, null, null, null, null, null, null, null));

        var filter = ArgumentCaptor.forClass(org.springframework.data.jpa.domain.Specification.class);
        verify(requisitions).findAll(filter.capture(), any(org.springframework.data.domain.Pageable.class));
        var root = mock(jakarta.persistence.criteria.Root.class);
        var builder = mock(jakarta.persistence.criteria.CriteriaBuilder.class);
        var status = mock(jakarta.persistence.criteria.Path.class);
        when(root.get("approvalStatus")).thenReturn(status);
        filter.getValue().toPredicate(root, null, builder);
        verify(builder).equal(status, "Approved");
    }
}
