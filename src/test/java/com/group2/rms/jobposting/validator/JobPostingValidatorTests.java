package com.group2.rms.jobposting.validator;

import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * UNIT TEST: JobPostingValidator (làm sạch + kiểm tra form tạo / sửa tin, kiểm tra trước khi đăng).
 * Clock cố định 18/10/2026 10:00 (giờ VN) để test biên ngày không lỗi khi chạy qua nửa đêm.
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện; dùng hằng số production (TITLE_LIMIT + 1...).
 */
class JobPostingValidatorTests {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final LocalDate TODAY = NOW.toLocalDate();

    private final JobPostingValidator validator = new JobPostingValidator(CLOCK);

    /** Form hợp lệ tối thiểu: đủ 3 ô bắt buộc, lương thỏa thuận, không hiện số lượng, hạn nộp 30 ngày sau. */
    private static JobPostingCreateRequest validForm(String action) {
        return JobPostingCreateRequest.builder()
                .requisitionId(101)
                .postingTitle("  Key Account Manager  ")
                .jobDescription("Chăm sóc khách hàng\r\nĐàm phán hợp đồng")
                .jobRequirements("2 năm kinh nghiệm")
                .salaryMode(JobPostingCreateRequest.SALARY_NEGOTIABLE)
                .showPositions(false)
                .applicationDeadline(TODAY.plusDays(30))
                .action(action)
                .build();
    }

    private Map<String, String> errorsOf(JobPostingCreateRequest form, boolean publishing) {
        JobPostingValidationException exception = assertThrows(JobPostingValidationException.class,
                () -> validator.normalizeAndValidate(form, publishing));
        return exception.getFieldErrors();
    }

    // =========================================================================
    // normalizeAndValidate: ô chữ
    // =========================================================================
    @Nested
    @DisplayName("normalizeAndValidate - ô chữ")
    class TextFields {

        @Test
        @DisplayName("Form hợp lệ -> Không lỗi; tiêu đề được trim, \\r\\n đổi thành \\n")
        void normalizeAndValidate_shouldCleanText_whenFormIsValid() {
            // Arrange
            JobPostingCreateRequest form = validForm(JobPostingValidator.ACTION_PUBLISH);

            // Act
            validator.normalizeAndValidate(form, true);

            // Assert
            assertEquals("Key Account Manager", form.getPostingTitle());
            assertEquals("Chăm sóc khách hàng\nĐàm phán hợp đồng", form.getJobDescription());
        }

        @ParameterizedTest(name = "Tiêu đề = [{0}] -> báo thiếu tiêu đề")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Tiêu đề null / rỗng / chỉ khoảng trắng -> Lỗi ở ô tiêu đề, kể cả khi lưu nháp")
        void normalizeAndValidate_shouldRequireTitle_whenTitleIsBlank(String title) {
            JobPostingCreateRequest form = validForm(JobPostingValidator.ACTION_DRAFT);
            form.setPostingTitle(title);

            assertEquals("Vui lòng nhập tiêu đề tin.", errorsOf(form, false).get("postingTitle"));
        }

        @Test
        @DisplayName("Thiếu mô tả và yêu cầu khi lưu nháp -> Lỗi ở cả hai ô (cột NOT NULL), báo một lượt")
        void normalizeAndValidate_shouldCollectAllErrors_whenDraftMissesRequiredTexts() {
            JobPostingCreateRequest form = validForm(JobPostingValidator.ACTION_DRAFT);
            form.setJobDescription(null);
            form.setJobRequirements("");

            Map<String, String> errors = errorsOf(form, false);

            assertTrue(errors.containsKey("jobDescription"));
            assertTrue(errors.containsKey("jobRequirements"));
        }

        @Test
        @DisplayName("Tiêu đề đúng TITLE_LIMIT ký tự -> Hợp lệ; TITLE_LIMIT + 1 -> Lỗi")
        void normalizeAndValidate_shouldEnforceTitleLimit_atBoundary() {
            JobPostingCreateRequest atLimit = validForm(JobPostingValidator.ACTION_DRAFT);
            atLimit.setPostingTitle("a".repeat(JobPostingValidator.TITLE_LIMIT));
            assertDoesNotThrow(() -> validator.normalizeAndValidate(atLimit, false));

            JobPostingCreateRequest overLimit = validForm(JobPostingValidator.ACTION_DRAFT);
            overLimit.setPostingTitle("a".repeat(JobPostingValidator.TITLE_LIMIT + 1));
            assertEquals(JobPostingValidator.TITLE_TOO_LONG, errorsOf(overLimit, false).get("postingTitle"));
        }

        @Test
        @DisplayName("Mô tả có \\r\\n: đếm độ dài sau khi đổi về \\n -> không bị chặn oan")
        void normalizeAndValidate_shouldCountLengthAfterNormalizing_whenTextHasCrLf() {
            JobPostingCreateRequest form = validForm(JobPostingValidator.ACTION_DRAFT);
            // 2 ký tự mỗi dòng trước khi chuẩn hóa → vượt TEXT_LIMIT; sau chuẩn hóa còn đúng TEXT_LIMIT - 1
            form.setJobDescription("a\r\n".repeat(JobPostingValidator.TEXT_LIMIT / 2 - 1) + "a");

            assertDoesNotThrow(() -> validator.normalizeAndValidate(form, false));
        }

        @Test
        @DisplayName("Action lạ (sửa HTML) -> Lỗi chung 'action'")
        void normalizeAndValidate_shouldRejectUnknownAction() {
            assertEquals("Thao tác lưu tin không hợp lệ.", errorsOf(validForm("hack"), true).get("action"));
        }
    }

    // =========================================================================
    // normalizeAndValidate: lương, số lượng
    // =========================================================================
    @Nested
    @DisplayName("normalizeAndValidate - lương và số lượng")
    class SalaryAndPositions {

        @Test
        @DisplayName("Lương 'Tự nhập' chỉ khoảng trắng, bật số lượng mà để trống -> Lỗi đúng từng ô")
        void normalizeAndValidate_shouldRequireCustomTexts_whenOptionsAreOn() {
            JobPostingCreateRequest form = validForm(JobPostingValidator.ACTION_PUBLISH);
            form.setSalaryMode(JobPostingCreateRequest.SALARY_CUSTOM);
            form.setSalaryCustom("  ");
            form.setShowPositions(true);
            form.setNumberOfPositionsDisplay("");

            Map<String, String> errors = errorsOf(form, true);

            assertEquals("Nhập mức lương hiển thị hoặc chọn một lựa chọn khác.", errors.get("salaryCustom"));
            assertEquals("Nhập số lượng hiển thị hoặc tắt hiển thị số lượng.", errors.get("numberOfPositionsDisplay"));
        }

        @ParameterizedTest(name = "salaryMode = [{0}] -> lỗi")
        @NullAndEmptySource
        @ValueSource(strings = {"RANGE", "hack"})
        @DisplayName("Lựa chọn lương thiếu / sai hoa thường / ngoài danh mục -> Lỗi, không NullPointerException")
        void normalizeAndValidate_shouldRejectSalaryMode_whenOutsideCatalog(String salaryMode) {
            JobPostingCreateRequest form = validForm(JobPostingValidator.ACTION_PUBLISH);
            form.setSalaryMode(salaryMode);

            assertTrue(errorsOf(form, true).containsKey("salaryMode"));
        }
    }

    // =========================================================================
    // normalizeAndValidate: hạn nộp
    // =========================================================================
    @Nested
    @DisplayName("normalizeAndValidate - ngày")
    class Dates {

        @Test
        @DisplayName("D6: đăng tin thiếu hạn nộp -> Lỗi; lưu nháp thiếu hạn nộp -> Hợp lệ")
        void normalizeAndValidate_shouldRequireDeadline_onlyWhenPublishing() {
            JobPostingCreateRequest publish = validForm(JobPostingValidator.ACTION_PUBLISH);
            publish.setApplicationDeadline(null);
            assertEquals("Vui lòng chọn hạn nộp hồ sơ.", errorsOf(publish, true).get("applicationDeadline"));

            JobPostingCreateRequest draft = validForm(JobPostingValidator.ACTION_DRAFT);
            draft.setApplicationDeadline(null);
            assertDoesNotThrow(() -> validator.normalizeAndValidate(draft, false));
        }

        @Test
        @DisplayName("Hạn nộp hôm qua -> Lỗi; hôm nay (cuối ngày, biên) -> Hợp lệ")
        void normalizeAndValidate_shouldAcceptToday_andRejectYesterday() {
            JobPostingCreateRequest yesterday = validForm(JobPostingValidator.ACTION_PUBLISH);
            yesterday.setApplicationDeadline(TODAY.minusDays(1));
            assertEquals("Hạn nộp hồ sơ phải từ hôm nay trở đi.",
                    errorsOf(yesterday, true).get("applicationDeadline"));

            JobPostingCreateRequest today = validForm(JobPostingValidator.ACTION_PUBLISH);
            today.setApplicationDeadline(TODAY);
            assertDoesNotThrow(() -> validator.normalizeAndValidate(today, true));
        }

    }

    // =========================================================================
    // Các hàm khác
    // =========================================================================
    @Nested
    @DisplayName("Kiểm tra trước khi đăng / sửa")
    class Others {

        @Test
        @DisplayName("Hạn nộp lưu là cuối ngày đã chọn (23:59:59); chưa chọn -> null")
        void deadlineOf_shouldUseEndOfDay() {
            JobPostingCreateRequest form = validForm(JobPostingValidator.ACTION_DRAFT);
            assertEquals(TODAY.plusDays(30).atTime(23, 59, 59), validator.deadlineOf(form));

            form.setApplicationDeadline(null);
            assertEquals(null, validator.deadlineOf(form));
        }

        @Test
        @DisplayName("Hạn nộp mặc định của form tạo = hôm nay + DEFAULT_DEADLINE_DAYS (theo Clock)")
        void defaultDeadline_shouldUseClock() {
            assertEquals(TODAY.plusDays(JobPostingValidator.DEFAULT_DEADLINE_DAYS), validator.defaultDeadline());
        }

        @ParameterizedTest(name = "Yêu cầu {0} -> không đăng được")
        @ValueSource(strings = {RequisitionAccess.STATUS_DRAFT, RequisitionAccess.STATUS_PENDING_DIRECTOR,
                RequisitionAccess.STATUS_REJECTED})
        @DisplayName("Yêu cầu nguồn chưa Approved -> JobPostingValidationException")
        void requireApproved_shouldReject_whenRequisitionIsNotApproved(String status) {
            JobRequisition requisition = JobRequisition.builder().approvalStatus(status).build();

            assertThrows(JobPostingValidationException.class, () -> validator.requireApproved(requisition));
        }

        @Test
        @DisplayName("Tin đã đóng -> không sửa được")
        void requireEditable_shouldReject_whenClosed() {
            assertThrows(JobPostingValidationException.class,
                    () -> validator.requireEditable(JobPostingAccess.STATUS_CLOSED));
            assertDoesNotThrow(() -> validator.requireEditable(JobPostingAccess.STATUS_PAUSED));
        }

        @Test
        @DisplayName("Đăng tin đã quá hạn nộp / chưa có hạn nộp -> Lỗi; đủ nội dung còn hạn -> Hợp lệ")
        void requirePublishableContent_shouldCheckDeadline() {
            JobPosting posting = JobPosting.builder().postingTitle("Java").jobDescription("Mô tả")
                    .jobRequirements("Yêu cầu").applicationDeadline(NOW.plusDays(1)).build();
            assertDoesNotThrow(() -> validator.requirePublishableContent(posting, NOW));

            posting.setApplicationDeadline(NOW);
            assertThrows(JobPostingValidationException.class, () -> validator.requirePublishableContent(posting, NOW));

            posting.setApplicationDeadline(null);
            assertThrows(JobPostingValidationException.class, () -> validator.requirePublishableContent(posting, NOW));
        }

        @Test
        @DisplayName("Chỉ 'publish' là nút đăng")
        void isPublish_shouldMatchOnlyPublishAction() {
            assertTrue(JobPostingValidator.isPublish(JobPostingValidator.ACTION_PUBLISH));
            assertFalse(JobPostingValidator.isPublish(JobPostingValidator.ACTION_DRAFT));
            assertFalse(JobPostingValidator.isPublish(null));
        }
    }
}
