package com.group2.rms.jobposting.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse.ActivityHistoryItem;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse.ApplicationStats;
import com.group2.rms.jobposting.dto.InternalJobPostingResponse;
import com.group2.rms.jobposting.dto.JobPostingRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.validator.JobPostingValidator;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * UNIT TEST: JobPostingMapper (entity → DTO: dòng danh sách, trang chi tiết, form tạo / sửa).
 * Dùng JobPostingAccess và JobPostingValidator THẬT (hàm thuần) → kiểm tra luôn cờ quyền, nhãn, mốc thời gian.
 * Mapper không truy vấn DB nên không cần mock repository.
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện, Clock cố định.
 */
class JobPostingMapperTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private final JobPostingAccess access = new JobPostingAccess(new RequisitionAccess(null));
    private final JobPostingMapper mapper = new JobPostingMapper(access, new JobPostingValidator(CLOCK));

    private User hr;
    private User admin;
    private User director;
    private User manager;
    private JobRequisition requisition;

    @BeforeEach
    void setUp() {
        hr = userWithRole(1, RequisitionAccess.ROLE_HR);
        admin = userWithRole(2, RequisitionAccess.ROLE_SYSTEM_ADMIN);
        director = userWithRole(3, RequisitionAccess.ROLE_DIRECTOR);
        manager = userWithRole(4, RequisitionAccess.ROLE_HIRING_MANAGER);
        requisition = JobRequisition.builder().requisitionId(5).requisitionCode("REQ-0005").title("Java Dev")
                .approvalStatus(RequisitionAccess.STATUS_APPROVED).employmentType("Full-time").workModel("Hybrid")
                .department(Department.builder().departmentId(1).departmentName("Engineering").build())
                .numberOfPositions(2).recruitmentRound(1).workLocation("Hà Nội")
                .jobDescription("Lập trình backend").requirementDetails("2 năm Spring Boot")
                .minSalary(new BigDecimal("20000000")).maxSalary(new BigDecimal("30000000"))
                .screeningCriteria(new ArrayList<>()).build();
    }

    // ----- Hàm hỗ trợ -----

    private static User userWithRole(int id, String roleName) {
        return User.builder().userId(id).fullName("User " + id).role(Role.builder().roleName(roleName).build()).build();
    }

    private JobPosting posting(String status, LocalDateTime postingDate, LocalDateTime deadline) {
        return JobPosting.builder().jobPostingId(11).postingNo(1).postingTitle("Java Dev (Spring)")
                .postingStatus(status).postingDate(postingDate).applicationDeadline(deadline)
                .requisition(requisition).createdBy(hr).build();
    }

    private InternalJobPostingDetailResponse detail(JobPosting posting, User viewer) {
        return mapper.toDetail(posting, viewer, NOW, false, false, ApplicationStats.empty(BigDecimal.TEN),
                List.of(), List.of());
    }

    // =========================================================================
    // toListRow
    // =========================================================================
    @Nested
    @DisplayName("toListRow")
    class ListRow {

        @Test
        @DisplayName("Tin đang đăng có 3 hồ sơ (2 mới), HR -> Nhãn 'Đang đăng', tạm dừng / đóng / sửa, không xóa")
        void toListRow_shouldMapLabelsAndFlags_whenHrViewsPublishedPosting() {
            // Act
            InternalJobPostingResponse row = mapper.toListRow(
                    posting(JobPostingAccess.STATUS_PUBLISHED, NOW.minusDays(3), NOW.plusDays(20)), hr, 3, 2, NOW);

            // Assert
            assertEquals(JobPostingAccess.STATUS_PUBLISHED, row.viewStatus());
            assertEquals("Đang đăng", row.statusLabel());
            assertEquals("success", row.statusTone());
            assertEquals("Engineering", row.departmentName());
            assertEquals("Toàn thời gian", row.employmentType());
            assertEquals(3, row.applicationCount());
            assertEquals(2, row.newApplicationCount());
            assertEquals(20L, row.daysUntilDeadline());
            assertEquals(JobPostingLabels.DEADLINE_OPEN, row.deadlineState());
            assertTrue(row.pausable() && row.closable() && row.editable() && row.applicationsViewable());
            assertFalse(row.deletable() || row.publishable() || row.reopenable());
        }

        @Test
        @DisplayName("Tin đã đăng -> nhãn đang đăng và có thể tạm dừng")
        void toListRow_shouldShowPublished() {
            InternalJobPostingResponse row = mapper.toListRow(
                    posting(JobPostingAccess.STATUS_PUBLISHED, NOW.minusDays(3), NOW.plusDays(30)), hr, 0, 0, NOW);

            assertEquals(JobPostingAccess.STATUS_PUBLISHED, row.viewStatus());
            assertEquals("Đang đăng", row.statusLabel());
            assertTrue(row.pausable() && row.closable());
            assertTrue(row.editable());
        }

        @Test
        @DisplayName("Tin nháp chưa có hồ sơ, Admin -> không được đăng / xóa; hạn nộp còn 7 ngày là 'soon'")
        void toListRow_shouldDenyPublishAndDelete_whenAdminViewsEmptyDraft() {
            InternalJobPostingResponse row = mapper.toListRow(
                    posting(JobPostingAccess.STATUS_DRAFT, null, NOW.plusDays(JobPostingLabels.DEADLINE_SOON_DAYS)),
                    admin, 0, 0, NOW);

            assertFalse(row.publishable() || row.deletable());
            assertEquals(JobPostingLabels.DEADLINE_SOON, row.deadlineState());
            assertFalse(row.applicationsViewable());
        }

        @Test
        @DisplayName("Admin xem tin tạm dừng đã quá hạn -> không mở lại hoặc đóng được, hạn nộp 'past'")
        void toListRow_shouldNotAllowClose_whenAdminViewsPausedPosting() {
            InternalJobPostingResponse row = mapper.toListRow(
                    posting(JobPostingAccess.STATUS_PAUSED, NOW.minusDays(10), NOW.minusDays(1)), admin, 1, 0, NOW);

            assertFalse(row.reopenable());
            assertFalse(row.closable());
            assertEquals(JobPostingLabels.DEADLINE_PAST, row.deadlineState());
        }

        @Test
        @DisplayName("Tin đã đóng -> Không tô màu hạn nộp (deadlineState = null), không sửa được")
        void toListRow_shouldClearDeadlineState_whenPostingIsClosed() {
            InternalJobPostingResponse row = mapper.toListRow(
                    posting(JobPostingAccess.STATUS_CLOSED, NOW.minusDays(40), NOW.minusDays(5)), hr, 4, 0, NOW);

            assertNull(row.deadlineState());
            assertFalse(row.editable());
        }

        @Test
        @DisplayName("Director -> Mọi cờ thao tác tắt, vẫn xem được ứng viên nếu có hồ sơ")
        void toListRow_shouldHaveNoActions_whenViewerIsDirector() {
            InternalJobPostingResponse row = mapper.toListRow(
                    posting(JobPostingAccess.STATUS_PUBLISHED, NOW.minusDays(1), NOW.plusDays(10)), director, 2, 0, NOW);

            assertFalse(row.editable() || row.pausable() || row.closable() || row.deletable()
                    || row.publishable() || row.reopenable());
            assertTrue(row.applicationsViewable());
        }
    }

    // =========================================================================
    // toDetail
    // =========================================================================
    @Nested
    @DisplayName("toDetail")
    class Detail {

        @Test
        @DisplayName("Tin đang đăng còn 5 ngày -> Dải 'sắp hết hạn', đếm ngược, có số liệu (live), HR thấy talent pool")
        void toDetail_shouldShowDeadlineSoonBanner_whenPublishedAndDeadlineIsNear() {
            InternalJobPostingDetailResponse detail =
                    detail(posting(JobPostingAccess.STATUS_PUBLISHED, NOW.minusDays(10), NOW.plusDays(5)), hr);

            assertTrue(detail.deadlineSoonBanner() && detail.deadlineCountdown() && detail.live());
            assertFalse(detail.deadlinePassedBanner() || detail.draftBanner() || detail.closedBanner());
            assertTrue(detail.talentPoolVisible());
            assertEquals("Ứng viên được HM đề xuất", detail.suggestionsLinkLabel());
            assertEquals("Kết hợp", detail.workModelLabel());
            assertTrue(detail.publicViewable());
        }

        @Test
        @DisplayName("Tin đang đăng đã quá hạn -> Dải 'đã quá hạn', hiện '· quá hạn'")
        void toDetail_shouldShowOverdue_whenPublishedPastDeadline() {
            InternalJobPostingDetailResponse detail =
                    detail(posting(JobPostingAccess.STATUS_PUBLISHED, NOW.minusDays(30), NOW.minusDays(2)), hr);

            assertTrue(detail.deadlinePassedBanner() && detail.deadlineOverdue());
            assertFalse(detail.deadlineCountdown());
        }

        @Test
        @DisplayName("Tin đã đóng quá hạn -> Dải 'đã đóng', không nhắc quá hạn")
        void toDetail_shouldNotShowOverdue_whenClosed() {
            InternalJobPostingDetailResponse detail =
                    detail(posting(JobPostingAccess.STATUS_CLOSED, NOW.minusDays(30), NOW.minusDays(2)), hr);

            assertTrue(detail.closedBanner());
            assertFalse(detail.deadlineOverdue() || detail.deadlinePassedBanner());
        }

        @Test
        @DisplayName("Tin đã đăng, Admin không có nút quản lý")
        void toDetail_shouldDenyManageActions_whenAdminViewsPublishedPosting() {
            InternalJobPostingDetailResponse detail =
                    detail(posting(JobPostingAccess.STATUS_PUBLISHED, NOW.minusDays(3), NOW.plusDays(30)), admin);

            assertFalse(detail.editable() || detail.pausable() || detail.talentPoolVisible());
            assertTrue(detail.publicViewable() && detail.live());
        }

        @Test
        @DisplayName("HM xem -> Nút talent pool ghi 'Ứng viên tiềm năng' / 'Gợi ý từ HR'")
        void toDetail_shouldUseManagerLinkLabels_whenViewerIsHiringManager() {
            InternalJobPostingDetailResponse detail =
                    detail(posting(JobPostingAccess.STATUS_PUBLISHED, NOW.minusDays(1), NOW.plusDays(20)), manager);

            assertEquals("Ứng viên tiềm năng", detail.suggestionsLinkLabel());
            assertEquals("Gợi ý từ HR", detail.recommendationsLinkLabel());
        }

        @Test
        @DisplayName("Không có SalaryDisplay -> Hiện 'Thỏa thuận'")
        void toDetail_shouldShowNegotiable_whenSalaryDisplayIsMissing() {
            assertEquals(JobPostingLabels.SALARY_NEGOTIABLE,
                    detail(posting(JobPostingAccess.STATUS_DRAFT, null, null), hr).salaryDisplay());
        }

        @Test
        @DisplayName("Audit log lưu mới nhất trước -> Lịch sử hiện theo thứ tự thời gian, đánh số 1, 2")
        void toActivityHistory_shouldSortChronologically_whenLogsExist() {
            AuditLog created = AuditLog.builder().action(JobPostingAuditLogger.ACTION_CREATE).user(hr)
                    .newValue(JobPostingLabels.LOG_CREATE + ": Draft: Java").timestamp(NOW.minusDays(2)).build();
            AuditLog paused = AuditLog.builder().action(JobPostingAuditLogger.ACTION_UPDATE).user(null)
                    .newValue(JobPostingLabels.LOG_PAUSE + ": Java").timestamp(NOW.minusDays(1)).build();

            List<ActivityHistoryItem> history =
                    mapper.toActivityHistory(posting(JobPostingAccess.STATUS_PAUSED, null, null), List.of(paused, created));

            assertEquals(List.of("Tạo mới", "Tạm dừng"), history.stream().map(ActivityHistoryItem::action).toList());
            assertEquals(2, history.get(1).no());
            assertEquals(JobPostingLabels.SYSTEM_ACTOR, history.get(1).performedBy());
        }
    }

    // =========================================================================
    // Form tạo / sửa
    // =========================================================================
    @Nested
    @DisplayName("Form")
    class Form {

        @Test
        @DisplayName("Form tạo -> Điền sẵn từ yêu cầu: lương 'Theo yêu cầu', hiện '2 người', hạn nộp hôm nay + 30, nháp")
        void toCreateForm_shouldPrefillFromRequisition() {
            // Arrange
            requisition.getScreeningCriteria().add(ScreeningCriteria.builder().criteriaName("Java").requiredValue("2 năm")
                    .isMandatory(true).build());

            // Act
            JobPostingRequest form = mapper.toCreateForm(requisition, 1);

            // Assert
            assertEquals(5, form.getRequisitionId());
            assertEquals("Java Dev", form.getPostingTitle());
            assertEquals(JobPostingRequest.SALARY_RANGE, form.getSalaryMode());
            assertEquals("20 – 30 triệu VND/tháng", form.getSalaryRangeLabel());
            assertEquals("2 người", form.getNumberOfPositionsDisplay());
            assertEquals(NOW.toLocalDate().plusDays(JobPostingValidator.DEFAULT_DEADLINE_DAYS), form.getApplicationDeadline());
            assertEquals(JobPostingValidator.ACTION_DRAFT, form.getAction());
            assertEquals("2 năm Spring Boot\n\nTiêu chí bắt buộc:\n- Java: 2 năm", form.getJobRequirements());
            assertTrue(form.isDraftOrNew());
            assertFalse(form.isLive());
        }

        @Test
        @DisplayName("Yêu cầu không có lương -> Lựa chọn mặc định 'Thỏa thuận'")
        void toCreateForm_shouldChooseNegotiable_whenRequisitionHasNoSalary() {
            requisition.setMinSalary(null);
            requisition.setMaxSalary(null);

            assertEquals(JobPostingRequest.SALARY_NEGOTIABLE, mapper.toCreateForm(requisition, 1).getSalaryMode());
        }

        @Test
        @DisplayName("Form sửa tin đã đăng -> Nhận ra lương tự nhập, số lượng bị ẩn, trạng thái live")
        void toEditForm_shouldDeriveChoices_whenPostingIsPublished() {
            // Arrange
            LocalDateTime at = NOW.minusDays(4);
            JobPosting posting = posting(JobPostingAccess.STATUS_PUBLISHED, at, NOW.plusDays(30));
            posting.setPostingNo(2);
            posting.setSalaryDisplay("Lên đến 30 triệu + hoa hồng");

            // Act
            JobPostingRequest form = mapper.toEditForm(posting);

            // Assert
            assertEquals(11, form.getJobPostingId());
            assertEquals(JobPostingRequest.SALARY_CUSTOM, form.getSalaryMode());
            assertEquals("Lên đến 30 triệu + hoa hồng", form.getSalaryCustom());
            assertEquals(Boolean.FALSE, form.getShowPositions());
            assertTrue(form.isLive());
            assertFalse(form.isDraftOrNew());
            assertEquals(2, form.getPostingNo());
        }

        @Test
        @DisplayName("Form sửa tin tạm dừng có lương cách viết cũ 'Thoả thuận' -> Lựa chọn 'Thỏa thuận', 'paused', 'live'")
        void toEditForm_shouldRecognizeOldSpelling_whenPostingIsPaused() {
            JobPosting posting = posting(JobPostingAccess.STATUS_PAUSED, NOW.minusDays(3), NOW.plusDays(10));
            posting.setSalaryDisplay(JobPostingLabels.SALARY_NEGOTIABLE_OLD_SPELLING);

            JobPostingRequest form = mapper.toEditForm(posting);

            assertEquals(JobPostingRequest.SALARY_NEGOTIABLE, form.getSalaryMode());
            assertTrue(form.isPaused() && form.isLive());
        }
    }
}
