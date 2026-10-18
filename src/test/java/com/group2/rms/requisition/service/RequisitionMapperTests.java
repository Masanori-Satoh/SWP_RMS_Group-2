package com.group2.rms.requisition.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * UNIT TEST: RequisitionMapper (entity → DTO cho form, dòng danh sách, trang chi tiết).
 * Dùng RequisitionAccess và RequisitionValidator THẬT để kiểm tra cờ quyền và cờ "bản nháp còn thiếu"
 * đúng như người dùng thấy. Không cần mock: mapper không truy vấn DB (dữ liệu liên quan được truyền vào).
 * Clock cố định: hôm nay là 18/10/2026.
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện, Arrange / Act / Assert.
 */
class RequisitionMapperTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);

    private RequisitionMapper mapper;
    private User manager;
    private User hr;

    @BeforeEach
    void setUp() {
        // RequisitionAccess chỉ cần UserRepository cho actor(); các hàm can...() dùng ở đây không gọi tới
        mapper = new RequisitionMapper(new RequisitionAccess(null), new RequisitionValidator(CLOCK), CLOCK);
        manager = User.builder().userId(2).fullName("Nguyễn Văn A")
                .role(Role.builder().roleName(RequisitionAccess.ROLE_HIRING_MANAGER).build()).build();
        hr = User.builder().userId(1).fullName("HR")
                .role(Role.builder().roleName(RequisitionAccess.ROLE_HR).build()).build();
    }

    private JobRequisition requisition(String status) {
        return JobRequisition.builder()
                .requisitionId(25)
                .title("Java Engineer")
                .hiringManager(manager)
                .department(Department.builder().departmentId(1).departmentName("IT").build())
                .approvalStatus(status)
                .recruitmentRound(1)
                .numberOfPositions(2)
                .employmentType("Full-time")
                .workModel("Hybrid")
                .gender("Any")
                .screeningCriteria(new ArrayList<>())
                .build();
    }

    // ----- Dòng danh sách -----

    @Test
    @DisplayName("Bản nháp của chính mình, chưa có tin / đợt sau -> Sửa và xóa được, không sao chép / tạo tin")
    void toListRow_shouldAllowEditAndDelete_whenOwnerViewsOwnDraft() {
        // Act
        RequisitionResponse row = mapper.toListRow(requisition(RequisitionAccess.STATUS_DRAFT), manager, null, false);

        // Assert
        assertTrue(row.isEditable());
        assertTrue(row.isDeletable());
        assertFalse(row.isCopyable());
        assertFalse(row.isPostable());
        assertFalse(row.isPostingViewable());
        assertEquals("Bản nháp", row.getStatusLabel());
        assertEquals("Chưa có tin", row.getPostingLabel());
        assertEquals("Toàn thời gian", row.getEmploymentTypeLabel());
    }

    @Test
    @DisplayName("Bản nháp đã có đợt sau sao chép từ nó -> Sửa được nhưng không xóa được")
    void toListRow_shouldNotBeDeletable_whenLaterRoundExists() {
        RequisitionResponse row = mapper.toListRow(requisition(RequisitionAccess.STATUS_DRAFT), manager, null, true);

        assertTrue(row.isEditable());
        assertFalse(row.isDeletable());
    }

    @Test
    @DisplayName("HR xem yêu cầu Đã duyệt chưa có tin -> Có nút Tạo tin tuyển dụng")
    void toListRow_shouldBePostable_whenHrViewsApprovedWithoutPosting() {
        RequisitionResponse row = mapper.toListRow(requisition(RequisitionAccess.STATUS_APPROVED), hr, null, false);

        assertTrue(row.isPostable());
        assertFalse(row.isEditable());
    }

    @Test
    @DisplayName("Đã có tin nháp -> HR thấy link tin, HM không thấy (trang tin nháp trả 403 với HM)")
    void toListRow_shouldShowDraftPostingLinkOnlyToHr_whenLatestPostingIsDraft() {
        // Arrange
        JobRequisition approved = requisition(RequisitionAccess.STATUS_APPROVED);
        JobPosting draftPosting = JobPosting.builder().jobPostingId(9).postingNo(1)
                .postingStatus(RequisitionLabels.POSTING_DRAFT).requisition(approved).build();

        // Act
        RequisitionResponse forHr = mapper.toListRow(approved, hr, draftPosting, false);
        RequisitionResponse forManager = mapper.toListRow(approved, manager, draftPosting, false);

        // Assert
        assertEquals(9, forHr.getJobPostingId());
        assertEquals("Tin nháp", forHr.getPostingLabel());
        assertTrue(forHr.isPostingViewable());
        assertFalse(forHr.isPostable(), "Đã có tin thì không tạo thêm tin từ màn yêu cầu");
        assertFalse(forManager.isPostingViewable());
    }

    @Test
    @DisplayName("Yêu cầu cũ chưa lưu mã -> Mã hiển thị sinh từ id: REQ-0025")
    void toListRow_shouldGenerateCode_whenRequisitionCodeIsMissing() {
        RequisitionResponse row = mapper.toListRow(requisition(RequisitionAccess.STATUS_DRAFT), manager, null, false);

        assertEquals("REQ-0025", row.getRequisitionCode());
    }

    // ----- Trang chi tiết -----

    @Test
    @DisplayName("Bản nháp thiếu thông tin bắt buộc khi gửi duyệt -> incomplete = true")
    void toDetail_shouldMarkIncomplete_whenDraftMissesRequiredFields() {
        RequisitionResponse detail = mapper.toDetail(requisition(RequisitionAccess.STATUS_DRAFT), manager, null, false,
                List.of(), List.of());

        assertTrue(detail.isIncomplete());
    }

    @Test
    @DisplayName("Bản nháp đã điền đủ (tổng trọng số 100%) -> incomplete = false")
    void toDetail_shouldNotMarkIncomplete_whenDraftIsReadyToSubmit() {
        // Arrange
        JobRequisition draft = requisition(RequisitionAccess.STATUS_DRAFT);
        draft.setWorkLocation("Hà Nội");
        draft.setReasonForHiring("Mở rộng");
        draft.setJobDescription("Phát triển RMS");
        draft.setRequirementDetails("Spring Boot");
        draft.setExpectedStartDate(TODAY.plusDays(30));
        draft.getScreeningCriteria().add(ScreeningCriteria.builder().criteriaName("Java").criteriaType("Skill")
                .requiredValue("2 năm").weight(new BigDecimal("100")).isMandatory(true).build());

        // Act
        RequisitionResponse detail = mapper.toDetail(draft, manager, null, false, List.of(), List.of());

        // Assert
        assertFalse(detail.isIncomplete());
        assertEquals(new BigDecimal("1E+2"), detail.getCriteriaTotalWeight());
        assertEquals("Kỹ năng", detail.getScreeningCriteria().get(0).getCriteriaTypeLabel());
    }

    @Test
    @DisplayName("Đang chờ duyệt từ 15/10, bắt đầu 28/10 -> Chờ 3 ngày, còn 10 ngày tới ngày bắt đầu")
    void toDetail_shouldCountDaysFromClock_whenRequisitionIsPending() {
        // Arrange
        JobRequisition pending = requisition(RequisitionAccess.STATUS_PENDING_DIRECTOR);
        pending.setSubmittedAt(LocalDateTime.of(2026, 10, 15, 9, 0));
        pending.setExpectedStartDate(TODAY.plusDays(10));

        // Act
        RequisitionResponse detail = mapper.toDetail(pending, manager, null, false, List.of(), List.of());

        // Assert
        assertEquals(3L, detail.getWaitingDays());
        assertEquals(10L, detail.getDaysUntilStart());
        assertTrue(detail.isWithdrawable());
    }

    @Test
    @DisplayName("Bị trả lại -> Lý do là ý kiến của lần duyệt mới nhất")
    void toDetail_shouldUseLatestApprovalComment_whenRequisitionIsRejected() {
        // Arrange
        User director = User.builder().userId(3).fullName("Giám đốc").build();
        List<RequisitionApproval> history = List.of(
                RequisitionApproval.builder().director(director).status("Rejected").comments("Thiếu ngân sách").build(),
                RequisitionApproval.builder().director(director).status("Rejected").comments("Lần cũ").build());

        // Act
        RequisitionResponse detail = mapper.toDetail(requisition(RequisitionAccess.STATUS_REJECTED), manager, null, false,
                history, List.of());

        // Assert
        assertEquals("Thiếu ngân sách", detail.getRejectionReason());
        assertEquals(2, detail.getApprovals().size());
    }

    @Test
    @DisplayName("Nội dung có thẻ HTML -> Được escape trước khi đổi xuống dòng thành <br/> (chống XSS)")
    void toDetail_shouldEscapeHtml_whenDescriptionContainsScriptTag() {
        // Arrange
        JobRequisition requisition = requisition(RequisitionAccess.STATUS_APPROVED);
        requisition.setJobDescription("<script>alert(1)</script>\nLine 2");

        // Act
        RequisitionResponse detail = mapper.toDetail(requisition, manager, null, false, List.of(), List.of());

        // Assert
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;<br/>Line 2", detail.getJobDescriptionHtml());
    }

    @Test
    @DisplayName("Audit log -> Chuyển sang dòng hoạt động có tên người thực hiện")
    void toDetail_shouldMapActivityLog_whenLogsExist() {
        // Arrange
        AuditLog log = AuditLog.builder().auditLogId(1L).action("CREATE").user(manager).newValue("Tạo yêu cầu").build();

        // Act
        RequisitionResponse detail = mapper.toDetail(requisition(RequisitionAccess.STATUS_DRAFT), manager, null, false,
                List.of(), List.of(log));

        // Assert
        assertEquals("Nguyễn Văn A", detail.getActivityLog().get(0).getPerformedBy());
        assertEquals("Tạo yêu cầu", detail.getActivityLog().get(0).getDescription());
    }

    // ----- Form -----

    @Test
    @DisplayName("Đợt 2 trỏ về phiếu gốc -> DTO form mang tên phiếu gốc và phòng ban")
    void toRequestDto_shouldCarryOriginalTitleAndDepartment_whenRequisitionIsLaterRound() {
        // Arrange
        JobRequisition round2 = requisition(RequisitionAccess.STATUS_DRAFT);
        round2.setRecruitmentRound(2);
        round2.setOriginalRequisition(JobRequisition.builder().requisitionId(1).title("Java Engineer").build());

        // Act
        RequisitionRequest form = mapper.toRequestDto(round2);

        // Assert
        assertEquals("Java Engineer", form.getOriginalTitle());
        assertEquals(1, form.getDepartmentId());
        assertEquals(2, form.getRecruitmentRound());
    }
}
