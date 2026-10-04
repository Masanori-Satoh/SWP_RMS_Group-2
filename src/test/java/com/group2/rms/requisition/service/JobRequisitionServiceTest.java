package com.group2.rms.requisition.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.repository.RequisitionApprovalRepository;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.repository.RequisitionWorkflowEventRepository;
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
 * TODO [SVC-12] Map request/entity/response: workModel, probationDuration, salary, date,
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
    @Mock private RequisitionWorkflowEventRepository events;
    @Mock private RequisitionAccess access;
    @Mock private RequisitionValidator validator;
    @Mock private NotificationService notificationService;

    @InjectMocks private RequisitionServiceImpl service;

    private User manager;

    @BeforeEach
    void setUp() {
        manager = User.builder().userId(10).username("hm").fullName("Nguyen Van A").build();
        lenient().when(access.actor()).thenReturn(manager);
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
                .probationDuration("2 months")
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

        // Side effect: Draft không sinh workflow event
        verifyNoInteractions(events);

        // Audit log CREATE
        var auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(auditCap.capture());
        AuditLog log = auditCap.getValue();
        assertEquals("CREATE", log.getAction());
        assertEquals("JobRequisition", log.getEntityName());
        assertEquals("101", log.getEntityId());
        assertEquals("Created Java Engineer · Draft", log.getNewValue());
    }

    @Test
    @DisplayName("SVC-01: Draft thiếu field -> Gán default title 'Untitled requisition', null các field còn lại")
    void svc01_createDraft_missingFields_appliesDefaults() {
        var req = RequisitionRequest.builder().action("draft").build();

        assertEquals(101, service.createRequisition(req));

        verify(departments, never()).findById(any());

        var reqCap = ArgumentCaptor.forClass(JobRequisition.class);
        verify(requisitions).saveAndFlush(reqCap.capture());
        JobRequisition saved = reqCap.getValue();

        assertEquals("Untitled requisition", saved.getTitle());
        assertEquals("Draft", saved.getApprovalStatus());
        assertSame(manager, saved.getHiringManager());
        assertNull(saved.getDepartment());
        assertNull(saved.getNumberOfPositions());
        assertNull(saved.getSubmittedAt());
        assertTrue(saved.getScreeningCriteria().isEmpty());

        verifyNoInteractions(events);

        var auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(auditCap.capture());
        assertEquals("Created Untitled requisition · Draft", auditCap.getValue().getNewValue());
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
        assertNotNull(req.getDecidedAt());
        verify(approvals).save(any(RequisitionApproval.class));
        verify(requisitions).saveAndFlush(req);
        verify(notificationService).notifyRequisitionRejected(req, director, "Budget exceeded");
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

        service.search(1, 10, "", null, "", "", "newest");

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
