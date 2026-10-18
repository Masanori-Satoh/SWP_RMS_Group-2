package com.group2.rms.jobposting.service;

import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.service.TalentPoolService;
import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.jobposting.validator.JobPostingValidator;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/*
 * Form tạo / sửa tin tuyển dụng theo mockup (Bước 3 trong plan_posting.md):
 * - JobPostingValidator#validateForm: lỗi theo ô, hạn nộp bắt buộc khi đăng, hẹn đăng
 * - JobPostingServiceImpl: lưu lương theo 3 lựa chọn, ẩn số lượng, hẹn đăng, sửa tin ở từng trạng thái
 */
class JobPostingFormTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 17, 10, 0);

    /** Form hợp lệ tối thiểu: đủ 3 ô bắt buộc, lương thỏa thuận, không hiện số lượng, hạn nộp 30 ngày sau. */
    private static JobPostingCreateRequest validForm(String action) {
        return JobPostingCreateRequest.builder()
                .requisitionId(101)
                .postingTitle("  Key Account Manager  ")
                .jobDescription("Chăm sóc khách hàng\r\nĐàm phán hợp đồng")
                .jobRequirements("2 năm kinh nghiệm")
                .salaryMode(JobPostingCreateRequest.SALARY_NEGOTIABLE)
                .showPositions(false)
                .applicationDeadline(NOW.toLocalDate().plusDays(30))
                .publishMode(JobPostingCreateRequest.PUBLISH_NOW)
                .action(action)
                .build();
    }

    // =========================================================================
    // JobPostingValidator#validateForm
    // =========================================================================
    @Nested
    @DisplayName("JobPostingValidator#validateForm")
    class ValidatorTests {

        private final JobPostingValidator validator = new JobPostingValidator();

        private Map<String, String> errorsOf(JobPostingCreateRequest form, boolean publishing, boolean canSchedule) {
            var ex = assertThrows(JobPostingValidationException.class,
                    () -> validator.validateForm(form, publishing, canSchedule, NOW));
            return ex.getFieldErrors();
        }

        @Test
        @DisplayName("Form hợp lệ: trim tiêu đề, đổi \\r\\n thành \\n")
        void validForm_isCleaned() {
            var form = validForm("publish");
            validator.validateForm(form, true, true, NOW);
            assertEquals("Key Account Manager", form.getPostingTitle());
            assertEquals("Chăm sóc khách hàng\nĐàm phán hợp đồng", form.getJobDescription());
        }

        @Test
        @DisplayName("Thiếu tiêu đề / mô tả / yêu cầu: lỗi theo từng ô, kể cả khi lưu nháp")
        void requiredTexts_evenForDraft() {
            var form = validForm("draft");
            form.setPostingTitle(" ");
            form.setJobDescription(null);
            form.setJobRequirements("");
            Map<String, String> errors = errorsOf(form, false, true);
            assertEquals("Vui lòng nhập tiêu đề tin.", errors.get("postingTitle"));
            assertTrue(errors.containsKey("jobDescription"));
            assertTrue(errors.containsKey("jobRequirements"));
        }

        @Test
        @DisplayName("D6: đăng tin bắt buộc hạn nộp; lưu nháp thì không")
        void deadline_requiredOnlyWhenPublishing() {
            var publish = validForm("publish");
            publish.setApplicationDeadline(null);
            assertEquals("Vui lòng chọn hạn nộp hồ sơ.", errorsOf(publish, true, true).get("applicationDeadline"));

            var draft = validForm("draft");
            draft.setApplicationDeadline(null);
            assertDoesNotThrow(() -> validator.validateForm(draft, false, true, NOW));
        }

        @Test
        @DisplayName("Hạn nộp hôm qua -> lỗi; hạn nộp hôm nay (cuối ngày) -> hợp lệ")
        void deadline_mustBeTodayOrLater() {
            var past = validForm("publish");
            past.setApplicationDeadline(NOW.toLocalDate().minusDays(1));
            assertEquals("Hạn nộp hồ sơ phải từ hôm nay trở đi.", errorsOf(past, true, true).get("applicationDeadline"));

            var today = validForm("publish");
            today.setApplicationDeadline(NOW.toLocalDate());
            assertDoesNotThrow(() -> validator.validateForm(today, true, true, NOW));
        }

        @Test
        @DisplayName("Lương 'Tự nhập' để trống, hoặc bật hiện số lượng mà để trống -> lỗi ở đúng ô")
        void customSalary_andPositions() {
            var form = validForm("publish");
            form.setSalaryMode(JobPostingCreateRequest.SALARY_CUSTOM);
            form.setSalaryCustom("  ");
            form.setShowPositions(true);
            form.setNumberOfPositionsDisplay("");
            Map<String, String> errors = errorsOf(form, true, true);
            assertEquals("Nhập mức lương hiển thị hoặc chọn một lựa chọn khác.", errors.get("salaryCustom"));
            assertEquals("Nhập số lượng hiển thị hoặc tắt hiển thị số lượng.", errors.get("numberOfPositionsDisplay"));
        }

        @Test
        @DisplayName("Thiếu lựa chọn lương (form bị sửa) -> lỗi, không ném NullPointerException")
        void missingSalaryMode() {
            var form = validForm("publish");
            form.setSalaryMode(null);
            assertTrue(errorsOf(form, true, true).containsKey("salaryMode"));
        }

        @Test
        @DisplayName("Hẹn đăng: thời điểm đã qua, quá 90 ngày, hoặc sau hạn nộp -> lỗi")
        void schedule_rules() {
            var past = validForm("publish");
            past.setPublishMode(JobPostingCreateRequest.PUBLISH_LATER);
            past.setScheduledAt(NOW.minusHours(1));
            assertEquals("Thời điểm hẹn đăng phải sau thời điểm hiện tại.", errorsOf(past, true, true).get("scheduledAt"));

            var tooFar = validForm("publish");
            tooFar.setPublishMode(JobPostingCreateRequest.PUBLISH_LATER);
            tooFar.setScheduledAt(NOW.plusDays(JobPostingValidator.MAX_SCHEDULE_DAYS + 1L));
            assertTrue(errorsOf(tooFar, true, true).containsKey("scheduledAt"));

            var afterDeadline = validForm("publish");
            afterDeadline.setPublishMode(JobPostingCreateRequest.PUBLISH_LATER);
            afterDeadline.setScheduledAt(NOW.plusDays(40));
            assertEquals("Hạn nộp hồ sơ phải sau ngày đăng.", errorsOf(afterDeadline, true, true).get("applicationDeadline"));
        }

        @Test
        @DisplayName("Tin đã lên trang tuyển dụng không hẹn lại được; lưu nháp thì bỏ qua lịch đăng")
        void schedule_onlyWhenAllowedAndPublishing() {
            var live = validForm("draft");
            live.setPublishMode(JobPostingCreateRequest.PUBLISH_LATER);
            live.setScheduledAt(NOW.plusDays(2));
            assertEquals("Tin đã đăng nên không hẹn lại ngày đăng được.", errorsOf(live, true, false).get("publishMode"));

            var draft = validForm("draft");
            draft.setPublishMode(JobPostingCreateRequest.PUBLISH_LATER);
            draft.setScheduledAt(NOW.minusDays(1));
            assertDoesNotThrow(() -> validator.validateForm(draft, false, true, NOW));
        }

        @Test
        @DisplayName("Tiêu đề 201 ký tự, action lạ -> lỗi")
        void lengths_andAction() {
            var form = validForm("hack");
            form.setPostingTitle("a".repeat(JobPostingValidator.TITLE_LIMIT + 1));
            Map<String, String> errors = errorsOf(form, true, true);
            assertEquals("Nội dung quá 200 ký tự", errors.get("postingTitle"));
            assertEquals("Thao tác lưu tin không hợp lệ.", errors.get("action"));
        }
    }

    // =========================================================================
    // JobPostingServiceImpl: tạo / sửa
    // =========================================================================
    @Nested
    @DisplayName("JobPostingServiceImpl: tạo / sửa tin")
    @ExtendWith(MockitoExtension.class)
    class ServiceTests {

        @Mock private JobPostingRepository postings;
        @Mock private JobRequisitionRepository requisitions;
        @Mock private AuditLogRepository audit;
        @Mock private RequisitionAccess access;
        @Mock private NotificationService notifications;
        @Mock private TalentPoolService talentPool;
        @Mock private ApplicationRepository applications;
        @Mock private DepartmentRepository departments;
        @Spy private JobPostingValidator validator = new JobPostingValidator();
        @InjectMocks private JobPostingServiceImpl service;

        private User hr;
        private JobRequisition requisition;

        @BeforeEach
        void setUp() {
            hr = User.builder().userId(1).fullName("HR").build();
            requisition = JobRequisition.builder().requisitionId(101).requisitionCode("REQ-0101").title("Key Account Manager")
                    .approvalStatus(RequisitionAccess.STATUS_APPROVED)
                    .department(Department.builder().departmentId(2).departmentName("Sales").build())
                    .minSalary(new BigDecimal("20000000")).maxSalary(new BigDecimal("30000000"))
                    .numberOfPositions(2).build();
            lenient().when(access.role(hr)).thenReturn(RequisitionAccess.ROLE_HR);
            lenient().when(postings.save(any(JobPosting.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        private JobPosting createAndCapture(JobPostingCreateRequest form, boolean publish) {
            when(requisitions.findForUpdate(101)).thenReturn(Optional.of(requisition));
            when(postings.existsByRequisition_RequisitionIdAndPostingStatusIn(eq(101), any())).thenReturn(false);
            if (publish) service.createAndPublishJobPosting(form, hr);
            else service.createJobPosting(form, hr);
            var captor = ArgumentCaptor.forClass(JobPosting.class);
            verify(postings).save(captor.capture());
            return captor.getValue();
        }

        @Test
        @DisplayName("Lương 'Theo yêu cầu' lấy từ DB (không tin số gửi lên); tắt số lượng -> NULL")
        void create_salaryFromRequisition_andHiddenPositions() {
            var form = validForm("draft");
            form.setSalaryMode(JobPostingCreateRequest.SALARY_RANGE);
            form.setMinSalary(new BigDecimal("1"));
            form.setNumberOfPositionsDisplay("99 người");

            JobPosting saved = createAndCapture(form, false);

            assertEquals("20 – 30 triệu VND/tháng", saved.getSalaryDisplay());
            assertNull(saved.getNumberOfPositionsDisplay());
            assertEquals("Draft", saved.getPostingStatus());
            assertNull(saved.getPostingDate(), "Tin nháp không có ngày đăng (rule §24.4)");
            assertEquals(LocalDate.now().plusDays(30).atTime(23, 59, 59), saved.getApplicationDeadline());
        }

        @Test
        @DisplayName("Hẹn đăng: Published + PostingDate = thời điểm hẹn, có thông báo cho HM")
        void create_scheduled() {
            var form = validForm("publish");
            form.setApplicationDeadline(LocalDate.now().plusDays(30));
            form.setPublishMode(JobPostingCreateRequest.PUBLISH_LATER);
            LocalDateTime at = LocalDate.now().plusDays(3).atTime(8, 0);
            form.setScheduledAt(at);

            JobPosting saved = createAndCapture(form, true);

            assertEquals("Published", saved.getPostingStatus());
            assertEquals(at, saved.getPostingDate());
            assertFalse(saved.isVisibleToCandidates(LocalDateTime.now()), "Chưa tới giờ đăng thì ứng viên chưa thấy");
            verify(notifications).notifyJobPostingPublished(saved, hr);
        }

        @Test
        @DisplayName("Sửa tin đang đăng: giữ ngày đăng, không gửi lại thông báo")
        void update_livePosting_keepsPostingDate() {
            LocalDateTime postedAt = LocalDateTime.now().minusDays(5);
            JobPosting live = JobPosting.builder().jobPostingId(9).requisition(requisition).postingStatus("Published")
                    .postingDate(postedAt).build();
            when(postings.findForUpdate(9)).thenReturn(Optional.of(live));
            var form = validForm("draft");
            form.setJobPostingId(9);
            form.setApplicationDeadline(LocalDate.now().plusDays(20));

            service.updateJobPosting(9, form, hr);

            assertEquals("Published", live.getPostingStatus());
            assertEquals(postedAt, live.getPostingDate());
            verifyNoInteractions(notifications);
        }

        @Test
        @DisplayName("Tin hẹn đăng, đổi sang 'Đăng ngay' rồi lưu -> đăng luôn, có thông báo")
        void update_scheduled_toNow() {
            JobPosting scheduled = JobPosting.builder().jobPostingId(10).requisition(requisition).postingStatus("Published")
                    .postingDate(LocalDateTime.now().plusDays(5)).build();
            when(postings.findForUpdate(10)).thenReturn(Optional.of(scheduled));
            var form = validForm("draft");
            form.setJobPostingId(10);
            form.setApplicationDeadline(LocalDate.now().plusDays(20));

            service.updateJobPosting(10, form, hr);

            assertFalse(scheduled.getPostingDate().isAfter(LocalDateTime.now()));
            verify(notifications).notifyJobPostingPublished(scheduled, hr);
        }

        @Test
        @DisplayName("Tin tạm dừng: 'Lưu thay đổi' vẫn tạm dừng; 'Lưu và mở lại' -> Published")
        void update_paused() {
            JobPosting paused = JobPosting.builder().jobPostingId(11).requisition(requisition).postingStatus("Paused")
                    .postingDate(LocalDateTime.now().minusDays(10)).build();
            when(postings.findForUpdate(11)).thenReturn(Optional.of(paused));
            var form = validForm("draft");
            form.setJobPostingId(11);

            service.updateJobPosting(11, form, hr);
            assertEquals("Paused", paused.getPostingStatus());

            form.setAction("publish");
            service.updateAndPublishJobPosting(11, form, hr);
            assertEquals("Published", paused.getPostingStatus());
            verify(notifications).notifyJobPostingPublished(paused, hr);
        }

        @Test
        @DisplayName("Mở form sửa: nhận ra lương tự nhập, số lượng bị ẩn, tin hẹn đăng")
        void prepareEditForm_derivesChoices() {
            LocalDateTime at = LocalDateTime.now().plusDays(4).withNano(0);
            JobPosting posting = JobPosting.builder().jobPostingId(12).requisition(requisition).postingStatus("Published")
                    .postingNo(2).postingDate(at).salaryDisplay("Lên đến 30 triệu + hoa hồng").postingTitle("KAM")
                    .applicationDeadline(LocalDateTime.now().plusDays(30)).build();
            when(postings.findById(12)).thenReturn(Optional.of(posting));
            when(requisitions.findById(101)).thenReturn(Optional.of(requisition));

            JobPostingCreateRequest form = service.prepareEditForm(12, hr);

            assertEquals(JobPostingCreateRequest.SALARY_CUSTOM, form.getSalaryMode());
            assertEquals("Lên đến 30 triệu + hoa hồng", form.getSalaryCustom());
            assertEquals(Boolean.FALSE, form.getShowPositions());
            assertEquals(JobPostingCreateRequest.PUBLISH_LATER, form.getPublishMode());
            assertEquals(at, form.getScheduledAt());
            assertFalse(form.isLive(), "Tin hẹn đăng chưa lên trang tuyển dụng");
            assertEquals(2, form.getPostingNo());
        }
    }
}
