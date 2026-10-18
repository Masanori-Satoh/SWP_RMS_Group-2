package com.group2.rms.requisition.validator;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.exception.RequisitionValidationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/*
 * UNIT TEST VALIDATOR: RequisitionValidator.validate(RequisitionRequest).
 * Không Spring context, không database. "Hôm nay" cố định bằng Clock (18/10/2026).
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §12, §23):
 * - Tên test: hàm_shouldKếtQuả_whenĐiềuKiện; @DisplayName tiếng Việt "điều kiện -> kết quả".
 * - Mỗi quy tắc có test hợp lệ + sai, nhất là ở biên (đúng giới hạn / giới hạn + 1),
 *   null, rỗng, chỉ khoảng trắng, giá trị ngoài danh mục, trùng, ID giả.
 * - Dùng hằng số của validator (POSITIONS_MAX + 1) thay vì gõ số, đổi giới hạn thì test vẫn đúng ý.
 * - Kiểm tra đúng key lỗi ("numberOfPositions", "screeningCriteria[0].weight"): key quyết định lỗi hiện ở ô nào.
 *
 * Hàm hỗ trợ: valid() = gửi duyệt hợp lệ đủ trường; minimalDraft() = nháp chỉ có 4 trường NOT NULL;
 * criterion(name, weight) = một dòng tiêu chí đủ trường; expectPass / expectFail.
 * Phòng ban, quyền người dùng, criteriaId của yêu cầu khác: kiểm ở RequisitionServiceTests.
 */
class RequisitionValidatorTests {

    /** "Hôm nay" cố định: 18/10/2026 (giờ Việt Nam). Test ngày biên không phụ thuộc lúc chạy. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDate TODAY = LocalDate.now(CLOCK);

    private final RequisitionValidator validator = new RequisitionValidator(CLOCK);

    // helpers ---------------------------------------------------------------

    private RequisitionRequest valid() {
        return RequisitionRequest.builder()
                .action("submit")
                .title("Java Engineer")
                .departmentId(1)
                .numberOfPositions(1)
                .employmentType("Full-time")
                .workModel("On-site")
                .expectedStartDate(TODAY.plusDays(1))
                .workLocation("Office")
                .reasonForHiring("Expansion")
                .jobDescription("Develop software")
                .requirementDetails("Experience")
                .screeningCriteria(new ArrayList<>(List.of(
                        ScreeningCriteriaRequest.builder()
                                .criteriaName("Java")
                                .criteriaType("Skill")
                                .requiredValue("Two years")
                                .weight(new BigDecimal("100"))
                                .build())))
                .build();
    }

    private RequisitionRequest emptyDraft() {
        RequisitionRequest d = new RequisitionRequest();
        d.setAction("draft");
        d.setScreeningCriteria(new ArrayList<>());
        return d;
    }

    /**
     * Bản nháp tối thiểu hợp lệ theo quy tắc mới: Tên vị trí, Số lượng, Hình thức,
     * Mô hình làm việc
     * bắt buộc cả khi lưu nháp (các cột NOT NULL trong DB). Những thứ khác được để
     * trống.
     */
    private RequisitionRequest minimalDraft() {
        RequisitionRequest d = emptyDraft();
        d.setTitle("Draft Title");
        d.setNumberOfPositions(1);
        d.setEmploymentType("Full-time");
        d.setWorkModel("On-site");
        return d;
    }

    /**
     * Một dòng tiêu chí điền đủ trường (CriteriaName, CriteriaType, RequiredValue,
     * Weight đều NOT NULL).
     */
    private ScreeningCriteriaRequest criterion(String name, String weight) {
        return ScreeningCriteriaRequest.builder()
                .criteriaName(name).criteriaType("Skill").requiredValue("Có kinh nghiệm")
                .weight(new BigDecimal(weight)).build();
    }

    private ScreeningCriteriaRequest withId(ScreeningCriteriaRequest c, int id) {
        c.setCriteriaId(id);
        return c;
    }

    private RequisitionValidationException expectFail(RequisitionRequest req) {
        return assertThrows(RequisitionValidationException.class, () -> validator.validate(req));
    }

    private void expectPass(RequisitionRequest req) {
        assertDoesNotThrow(() -> validator.validate(req));
    }

    // VAL-001 — Submit hợp lệ

    @Test
    @DisplayName("Gửi duyệt đủ trường hợp lệ -> Không lỗi")
    void validate_shouldPass_whenSubmitHasAllValidFields() {
        expectPass(valid());
    }

    @Test
    @DisplayName("Mô hình Hybrid -> Hợp lệ")
    void validate_shouldPass_whenWorkModelIsHybrid() {
        var d = valid();
        d.setWorkModel("Hybrid");
        expectPass(d);
    }

    @Test
    @DisplayName("Mô hình Remote -> Hợp lệ")
    void validate_shouldPass_whenWorkModelIsRemote() {
        var d = valid();
        d.setWorkModel("Remote");
        expectPass(d);
    }

    // VAL-002 — Draft: chỉ bắt buộc các trường NOT NULL trong DB, còn lại được để
    // trống

    @Test
    @DisplayName("Nháp chỉ có 4 trường NOT NULL -> Hợp lệ")
    void validate_shouldPass_whenDraftHasOnlyDbRequiredFields() {
        expectPass(minimalDraft());
    }

    @Test
    @DisplayName("Nháp trống hoàn toàn -> Báo lỗi các trường NOT NULL")
    void validate_shouldReject_whenDraftMissesDbRequiredFields() {
        var errors = expectFail(emptyDraft()).getErrors();
        assertTrue(errors.containsKey("title"));
        assertTrue(errors.containsKey("numberOfPositions"));
        assertTrue(errors.containsKey("employmentType"));
        assertTrue(errors.containsKey("workModel"));
    }

    @Test
    @DisplayName("Nháp có dòng tiêu chí điền dở -> Báo lỗi dòng đó")
    void validate_shouldReject_whenDraftHasHalfFilledCriteriaRow() {
        var d = minimalDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Partial").weight(new BigDecimal("20")).build())));
        var errors = expectFail(d).getErrors();
        assertTrue(errors.containsKey("screeningCriteria[0].criteriaType"));
        assertTrue(errors.containsKey("screeningCriteria[0].requiredValue"));
    }

    @Test
    @DisplayName("Nháp có tiêu chí đủ trường, tổng trọng số < 100 -> Hợp lệ")
    void validate_shouldPass_whenDraftCriteriaTotalIsBelow100() {
        var d = minimalDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(criterion("Partial", "20"))));
        expectPass(d);
    }

    @Test
    @DisplayName("Nháp không chọn giới tính -> Tự gán Any")
    void validate_shouldDefaultGenderToAny_whenDraftGenderIsNull() {
        var d = minimalDraft();
        expectPass(d);
        assertEquals("Any", d.getGender());
    }

    @Test
    @DisplayName("Nháp thiếu phòng ban / ngày bắt đầu -> Hợp lệ")
    void validate_shouldPass_whenDraftHasNoDepartmentOrStartDate() {
        var d = minimalDraft();
        d.setTitle("Draft Title");
        expectPass(d);
    }

    @Test
    @DisplayName("Gửi duyệt với dữ liệu nháp tối thiểu -> Báo lỗi")
    void validate_shouldReject_whenMinimalDraftIsSubmitted() {
        var d = minimalDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Partial").weight(new BigDecimal("20")).build())));
        d.setAction("submit");
        expectFail(d);
    }

    // VAL-003 — Draft vẫn kiểm tra giá trị đã nhập

    @Test
    @DisplayName("Nháp có mô hình làm việc sai -> Báo lỗi")
    void validate_shouldReject_whenDraftWorkModelIsInvalid() {
        var d = minimalDraft();
        d.setWorkModel("Flexible");
        assertTrue(expectFail(d).getErrors().containsKey("workModel"));
    }

    @Test
    @DisplayName("Nháp có hình thức làm việc sai -> Báo lỗi")
    void validate_shouldReject_whenDraftEmploymentTypeIsInvalid() {
        var d = minimalDraft();
        d.setEmploymentType("Freelance");
        assertTrue(expectFail(d).getErrors().containsKey("employmentType"));
    }

    @Test
    @DisplayName("Nháp có giới tính sai -> Báo lỗi")
    void validate_shouldReject_whenDraftGenderIsInvalid() {
        var d = minimalDraft();
        d.setGender("Unknown");
        assertTrue(expectFail(d).getErrors().containsKey("gender"));
    }

    @Test
    @DisplayName("Nháp có số lượng âm -> Báo lỗi")
    void validate_shouldReject_whenDraftPositionsIsNegative() {
        var d = minimalDraft();
        d.setNumberOfPositions(-1);
        assertTrue(expectFail(d).getErrors().containsKey("numberOfPositions"));
    }

    @Test
    @DisplayName("Nháp có số lượng 0 -> Báo lỗi")
    void validate_shouldReject_whenDraftPositionsIsZero() {
        var d = minimalDraft();
        d.setNumberOfPositions(0);
        assertTrue(expectFail(d).getErrors().containsKey("numberOfPositions"));
    }

    @Test
    @DisplayName("Số lượng đúng bằng tối đa -> Hợp lệ")
    void validate_shouldPass_whenPositionsEqualsMaximum() {
        var d = minimalDraft();
        d.setNumberOfPositions(RequisitionValidator.POSITIONS_MAX);
        assertDoesNotThrow(() -> validator.validate(d));
    }

    @Test
    @DisplayName("Số lượng tối đa + 1 -> Báo lỗi")
    void validate_shouldReject_whenPositionsExceedsMaximumByOne() {
        var d = minimalDraft();
        d.setNumberOfPositions(RequisitionValidator.POSITIONS_MAX + 1);
        assertEquals("Số lượng cần tuyển phải từ 1 đến 30 người.",
                expectFail(d).getErrors().get("numberOfPositions"));
    }

    // VAL-004 — Action draft/submit

    @Test
    @DisplayName("Action null -> Báo lỗi")
    void validate_shouldReject_whenActionIsNull() {
        var d = valid();
        d.setAction(null);
        assertTrue(expectFail(d).getErrors().containsKey("action"));
    }

    @Test
    @DisplayName("Action rỗng -> Báo lỗi")
    void validate_shouldReject_whenActionIsBlank() {
        var d = valid();
        d.setAction("   ");
        assertTrue(expectFail(d).getErrors().containsKey("action"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "DRAFT", "Submit", "save", "approve", "publish", "" })
    @DisplayName("Action lạ (sửa HTML) -> Báo lỗi")
    void validate_shouldReject_whenActionIsUnknown(String action) {
        var d = valid();
        d.setAction(action);
        assertTrue(expectFail(d).getErrors().containsKey("action"), "action '" + action + "' must be rejected");
    }

    @ParameterizedTest
    @ValueSource(strings = { "draft", "submit" })
    @DisplayName("Action draft / submit -> Hợp lệ")
    void validate_shouldAccept_whenActionIsDraftOrSubmit(String action) {
        var d = valid();
        d.setAction(action);
        expectPass(d);
    }

    // VAL-005 — Chuẩn hóa text và khoảng trắng

    @Test
    @DisplayName("Tên vị trí chỉ có khoảng trắng -> Coi là trống, báo lỗi")
    void validate_shouldRejectTitle_whenTitleIsSpaceOnly() {
        var d = valid();
        d.setTitle("   ");
        assertTrue(expectFail(d).getErrors().containsKey("title"));
    }

    @Test
    @DisplayName("Tên vị trí có khoảng trắng đầu / cuối -> Được cắt bỏ")
    void validate_shouldTrimTitle_whenTitleHasSurroundingSpaces() {
        var d = valid();
        d.setTitle("  Java Engineer  ");
        expectPass(d);
        assertEquals("Java Engineer", d.getTitle());
    }

    @Test
    @DisplayName("Địa điểm chỉ có khoảng trắng -> Coi là trống, báo lỗi")
    void validate_shouldRejectWorkLocation_whenWorkLocationIsSpaceOnly() {
        var d = valid();
        d.setWorkLocation("   ");
        assertTrue(expectFail(d).getErrors().containsKey("workLocation"));
    }

    @Test
    @DisplayName("Gửi duyệt không chọn giới tính -> Hợp lệ (gán Any)")
    void validate_shouldPass_whenSubmitGenderIsNull() {
        var d = valid();
        d.setGender(null);
        expectPass(d);
    }

    // VAL-006 — Tiêu đề placeholder của Draft

    @Test
    @DisplayName("Gửi duyệt với tên mặc định -> Báo lỗi")
    void validate_shouldReject_whenSubmitTitleIsPlaceholder() {
        var d = valid();
        d.setTitle("Untitled requisition");
        assertTrue(expectFail(d).getErrors().containsKey("title"));
    }

    @Test
    @DisplayName("Tên mặc định khác hoa / thường -> Vẫn báo lỗi")
    void validate_shouldReject_whenPlaceholderTitleHasDifferentCase() {
        var d = valid();
        d.setTitle("UNTITLED REQUISITION");
        assertTrue(expectFail(d).getErrors().containsKey("title"));
    }

    @Test
    @DisplayName("Nháp với tên mặc định -> Hợp lệ")
    void validate_shouldPass_whenDraftTitleIsPlaceholder() {
        var d = minimalDraft();
        d.setTitle("Untitled requisition");
        expectPass(d);
    }

    // VAL-007 — Department và headcount

    @Test
    @DisplayName("Gửi duyệt thiếu phòng ban -> Validator bỏ qua (service gán phòng ban)")
    void validate_shouldNotCheckDepartment_whenSubmitHasNoDepartment() {
        // Phòng ban luôn do service gán (phòng ban người tạo / Giám đốc chọn), validator không kiểm
        var d = valid();
        d.setDepartmentId(null);
        expectPass(d);
    }

    @Test
    @DisplayName("Nháp thiếu phòng ban -> Hợp lệ")
    void validate_shouldPass_whenDraftHasNoDepartment() {
        var d = minimalDraft();
        d.setDepartmentId(null);
        expectPass(d);
    }

    @Test
    @DisplayName("Gửi duyệt thiếu số lượng -> Báo lỗi")
    void validate_shouldReject_whenSubmitPositionsIsNull() {
        var d = valid();
        d.setNumberOfPositions(null);
        assertTrue(expectFail(d).getErrors().containsKey("numberOfPositions"));
    }

    @Test
    @DisplayName("Số lượng 0 -> Báo lỗi")
    void validate_shouldReject_whenPositionsIsZero() {
        var d = valid();
        d.setNumberOfPositions(0);
        assertTrue(expectFail(d).getErrors().containsKey("numberOfPositions"));
    }

    @Test
    @DisplayName("Số lượng âm -> Báo lỗi")
    void validate_shouldReject_whenPositionsIsNegative() {
        var d = valid();
        d.setNumberOfPositions(-5);
        assertTrue(expectFail(d).getErrors().containsKey("numberOfPositions"));
    }

    @Test
    @DisplayName("Số lượng dương trong khoảng -> Hợp lệ")
    void validate_shouldPass_whenPositionsIsPositive() {
        var d = valid();
        d.setNumberOfPositions(10);
        expectPass(d);
    }

    // VAL-008 — Employment type, work model, gender

    @ParameterizedTest
    @ValueSource(strings = { "Full-time", "Part-time", "Internship", "Contract" })
    @DisplayName("Hình thức làm việc trong danh mục -> Hợp lệ")
    void validate_shouldPass_whenEmploymentTypeIsInList(String type) {
        var d = valid();
        d.setEmploymentType(type);
        expectPass(d);
    }

    @ParameterizedTest
    @ValueSource(strings = { "full-time", "FULL-TIME", "Freelance", "Temp", "" })
    @DisplayName("Hình thức làm việc ngoài danh mục -> Báo lỗi")
    void validate_shouldReject_whenEmploymentTypeIsNotInList(String type) {
        var d = valid();
        d.setEmploymentType(type);
        assertTrue(expectFail(d).getErrors().containsKey("employmentType"));
    }

    @Test
    @DisplayName("Thiếu hình thức làm việc -> Báo lỗi")
    void validate_shouldReject_whenEmploymentTypeIsNull() {
        var d = valid();
        d.setEmploymentType(null);
        assertTrue(expectFail(d).getErrors().containsKey("employmentType"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "On-site", "Remote", "Hybrid" })
    @DisplayName("Mô hình làm việc trong danh mục -> Hợp lệ")
    void validate_shouldPass_whenWorkModelIsInList(String model) {
        var d = valid();
        d.setWorkModel(model);
        expectPass(d);
    }

    @ParameterizedTest
    @ValueSource(strings = { "on-site", "REMOTE", "WFH", "Office", "" })
    @DisplayName("Mô hình làm việc ngoài danh mục -> Báo lỗi")
    void validate_shouldReject_whenWorkModelIsNotInList(String model) {
        var d = valid();
        d.setWorkModel(model);
        assertTrue(expectFail(d).getErrors().containsKey("workModel"));
    }

    @Test
    @DisplayName("Thiếu mô hình làm việc -> Báo lỗi")
    void validate_shouldReject_whenWorkModelIsNull() {
        var d = valid();
        d.setWorkModel(null);
        assertTrue(expectFail(d).getErrors().containsKey("workModel"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Any", "Male", "Female" })
    @DisplayName("Giới tính trong danh mục -> Hợp lệ")
    void validate_shouldPass_whenGenderIsInList(String gender) {
        var d = valid();
        d.setGender(gender);
        expectPass(d);
    }

    @Test
    @DisplayName("Giới tính ngoài danh mục -> Báo lỗi")
    void validate_shouldReject_whenGenderIsNotInList() {
        var d = valid();
        d.setGender("Non-binary");
        assertTrue(expectFail(d).getErrors().containsKey("gender"));
    }

    @Test
    @DisplayName("Giới tính null -> Hợp lệ (gán Any)")
    void validate_shouldPass_whenGenderIsNull() {
        var d = valid();
        d.setGender(null);
        expectPass(d);
    }

    // VAL-009 — Các field text bắt buộc

    @Test
    @DisplayName("Gửi duyệt thiếu địa điểm -> Báo lỗi")
    void validate_shouldReject_whenSubmitWorkLocationIsNull() {
        var d = valid();
        d.setWorkLocation(null);
        assertTrue(expectFail(d).getErrors().containsKey("workLocation"));
    }

    @Test
    @DisplayName("Gửi duyệt thiếu mô tả công việc -> Báo lỗi")
    void validate_shouldReject_whenSubmitJobDescriptionIsNull() {
        var d = valid();
        d.setJobDescription(null);
        assertTrue(expectFail(d).getErrors().containsKey("jobDescription"));
    }

    @Test
    @DisplayName("Gửi duyệt thiếu yêu cầu ứng viên -> Báo lỗi")
    void validate_shouldReject_whenSubmitRequirementDetailsIsNull() {
        var d = valid();
        d.setRequirementDetails(null);
        assertTrue(expectFail(d).getErrors().containsKey("requirementDetails"));
    }

    @Test
    @DisplayName("Gửi duyệt thiếu lý do tuyển -> Báo lỗi")
    void validate_shouldReject_whenSubmitReasonForHiringIsNull() {
        var d = valid();
        d.setReasonForHiring(null);
        assertTrue(expectFail(d).getErrors().containsKey("reasonForHiring"));
    }

    @Test
    @DisplayName("Nháp thiếu các ô văn bản -> Hợp lệ")
    void validate_shouldPass_whenDraftTextFieldsAreMissing() {
        expectPass(minimalDraft());
    }

    // VAL-010 — Giới hạn độ dài

    @Test
    @DisplayName("Tên vị trí đúng giới hạn ký tự -> Hợp lệ")
    void validate_shouldPass_whenTitleLengthEqualsLimit() {
        var d = valid();
        d.setTitle("A".repeat(RequisitionValidator.TITLE_CHAR_LIMIT));
        expectPass(d);
    }

    @Test
    @DisplayName("Tên vị trí vượt giới hạn 1 ký tự -> Báo lỗi")
    void validate_shouldReject_whenTitleExceedsLimitByOne() {
        var d = valid();
        d.setTitle("A".repeat(RequisitionValidator.TITLE_CHAR_LIMIT + 1));
        assertTrue(expectFail(d).getErrors().containsKey("title"));
    }

    @Test
    @DisplayName("Lý do tuyển đúng giới hạn -> Hợp lệ")
    void validate_shouldPass_whenReasonLengthEqualsLimit() {
        var d = valid();
        d.setReasonForHiring("x".repeat(RequisitionValidator.REASON_CHAR_LIMIT));
        expectPass(d);
    }

    @Test
    @DisplayName("Lý do tuyển vượt giới hạn 1 ký tự -> Báo lỗi")
    void validate_shouldReject_whenReasonExceedsLimitByOne() {
        var d = valid();
        d.setReasonForHiring("x".repeat(RequisitionValidator.REASON_CHAR_LIMIT + 1));
        assertTrue(expectFail(d).getErrors().containsKey("reasonForHiring"));
    }

    @Test
    @DisplayName("Mô tả công việc đúng giới hạn -> Hợp lệ")
    void validate_shouldPass_whenJobDescriptionLengthEqualsLimit() {
        var d = valid();
        d.setJobDescription("x".repeat(RequisitionValidator.TEXT_FIELD_CHAR_LIMIT));
        expectPass(d);
    }

    @Test
    @DisplayName("Mô tả công việc vượt giới hạn 1 ký tự -> Báo lỗi")
    void validate_shouldReject_whenJobDescriptionExceedsLimitByOne() {
        var d = valid();
        d.setJobDescription("x".repeat(RequisitionValidator.TEXT_FIELD_CHAR_LIMIT + 1));
        assertTrue(expectFail(d).getErrors().containsKey("jobDescription"));
    }

    @Test
    @DisplayName("Yêu cầu ứng viên đúng giới hạn -> Hợp lệ")
    void validate_shouldPass_whenRequirementDetailsLengthEqualsLimit() {
        var d = valid();
        d.setRequirementDetails("x".repeat(RequisitionValidator.TEXT_FIELD_CHAR_LIMIT));
        expectPass(d);
    }

    @Test
    @DisplayName("Yêu cầu ứng viên vượt giới hạn 1 ký tự -> Báo lỗi")
    void validate_shouldReject_whenRequirementDetailsExceedsLimitByOne() {
        var d = valid();
        d.setRequirementDetails("x".repeat(RequisitionValidator.TEXT_FIELD_CHAR_LIMIT + 1));
        assertTrue(expectFail(d).getErrors().containsKey("requirementDetails"));
    }

    @Test
    @DisplayName("Địa điểm đúng giới hạn -> Hợp lệ")
    void validate_shouldPass_whenWorkLocationLengthEqualsLimit() {
        var d = valid();
        d.setWorkLocation("x".repeat(RequisitionValidator.SHORT_FIELD_CHAR_LIMIT));
        expectPass(d);
    }

    @Test
    @DisplayName("Địa điểm vượt giới hạn 1 ký tự -> Báo lỗi")
    void validate_shouldReject_whenWorkLocationExceedsLimitByOne() {
        var d = valid();
        d.setWorkLocation("x".repeat(RequisitionValidator.SHORT_FIELD_CHAR_LIMIT + 1));
        assertTrue(expectFail(d).getErrors().containsKey("workLocation"));
    }

    @Test
    @DisplayName("Thử việc đúng số ngày tối đa -> Hợp lệ")
    void validate_shouldPass_whenProbationDaysEqualsMaximum() {
        var d = valid();
        d.setProbationDays(RequisitionValidator.PROBATION_MAX_DAYS);
        expectPass(d);
    }

    @Test
    @DisplayName("Thử việc tối đa + 1 ngày -> Báo lỗi")
    void validate_shouldReject_whenProbationDaysExceedsMaximumByOne() {
        var d = valid();
        d.setProbationDays(RequisitionValidator.PROBATION_MAX_DAYS + 1);
        assertTrue(expectFail(d).getErrors().containsKey("probationDays"));
    }

    @Test
    @DisplayName("Gửi duyệt không nhập thử việc -> Hợp lệ")
    void validate_shouldPass_whenSubmitProbationDaysIsNull() {
        var d = valid();
        d.setProbationDays(null);
        expectPass(d);
    }

    @Test
    @DisplayName("Tên tiêu chí đúng giới hạn -> Hợp lệ")
    void validate_shouldPass_whenCriteriaNameLengthEqualsLimit() {
        var d = valid();
        d.getScreeningCriteria().get(0).setCriteriaName("x".repeat(RequisitionValidator.CRITERIA_NAME_CHAR_LIMIT));
        expectPass(d);
    }

    @Test
    @DisplayName("Tên tiêu chí vượt giới hạn 1 ký tự -> Báo lỗi")
    void validate_shouldReject_whenCriteriaNameExceedsLimitByOne() {
        var d = valid();
        d.getScreeningCriteria().get(0).setCriteriaName("x".repeat(RequisitionValidator.CRITERIA_NAME_CHAR_LIMIT + 1));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].criteriaName"));
    }

    @Test
    @DisplayName("Giá trị yêu cầu của tiêu chí đúng giới hạn ký tự -> Hợp lệ")
    void validate_shouldPass_whenCriteriaRequiredValueLengthEqualsLimit() {
        var d = valid();
        d.getScreeningCriteria().get(0).setRequiredValue("x".repeat(RequisitionValidator.SHORT_FIELD_CHAR_LIMIT));
        expectPass(d);
    }

    @Test
    @DisplayName("Giá trị yêu cầu của tiêu chí vượt giới hạn 1 ký tự -> Báo lỗi đúng ô")
    void validate_shouldReject_whenCriteriaRequiredValueExceedsLimitByOne() {
        var d = valid();
        d.getScreeningCriteria().get(0).setRequiredValue("x".repeat(RequisitionValidator.SHORT_FIELD_CHAR_LIMIT + 1));
        assertEquals("Nội dung quá " + RequisitionValidator.SHORT_FIELD_CHAR_LIMIT + " ký tự",
                expectFail(d).getErrors().get("screeningCriteria[0].requiredValue"));
    }

    @Test
    @DisplayName("Gửi duyệt ngày bắt đầu đúng 1 năm + 1 ngày (biên) -> Báo lỗi")
    void validate_shouldReject_whenStartDateIsOneYearAndOneDayAhead() {
        var d = valid();
        d.setExpectedStartDate(TODAY.plusYears(1).plusDays(1));
        assertTrue(expectFail(d).getErrors().containsKey("expectedStartDate"));
    }

    @Test
    @DisplayName("Ngày bắt đầu sớm / muộn nhất được chọn -> Hôm nay và hôm nay + 1 năm (form dùng làm min / max)")
    void earliestAndLatestStartDate_shouldFollowClock_whenAskedByForm() {
        assertEquals(TODAY, validator.earliestStartDate());
        assertEquals(TODAY.plusYears(RequisitionValidator.START_DATE_MAX_YEARS), validator.latestStartDate());
    }

    // VAL-011 — Salary và độ chính xác thập phân

    @Test
    @DisplayName("Không nhập lương (Thỏa thuận) -> Hợp lệ")
    void validate_shouldPass_whenBothSalariesAreNull() {
        var d = valid();
        d.setMinSalary(null);
        d.setMaxSalary(null);
        expectPass(d);
    }

    @Test
    @DisplayName("Chỉ nhập lương tối thiểu -> Hợp lệ")
    void validate_shouldPass_whenOnlyMinSalaryIsSet() {
        var d = valid();
        d.setMinSalary(new BigDecimal("10000000"));
        d.setMaxSalary(null);
        expectPass(d);
    }

    @Test
    @DisplayName("Chỉ nhập lương tối đa -> Hợp lệ")
    void validate_shouldPass_whenOnlyMaxSalaryIsSet() {
        var d = valid();
        d.setMinSalary(null);
        d.setMaxSalary(new BigDecimal("20000000"));
        expectPass(d);
    }

    @Test
    @DisplayName("Lương 0 -> Báo lỗi")
    void validate_shouldReject_whenSalaryIsZero() {
        var d = valid();
        d.setMinSalary(BigDecimal.ZERO);
        assertTrue(expectFail(d).getErrors().containsKey("minSalary"));
    }

    @Test
    @DisplayName("Lương âm -> Báo lỗi")
    void validate_shouldReject_whenSalaryIsNegative() {
        var d = valid();
        d.setMinSalary(new BigDecimal("-1"));
        assertTrue(expectFail(d).getErrors().containsKey("minSalary"));
    }

    @Test
    @DisplayName("Lương tối thiểu > tối đa -> Báo lỗi")
    void validate_shouldReject_whenMinSalaryIsGreaterThanMax() {
        var d = valid();
        d.setMinSalary(BigDecimal.TEN);
        d.setMaxSalary(BigDecimal.ONE);
        assertTrue(expectFail(d).getErrors().containsKey("maxSalary"));
    }

    @Test
    @DisplayName("Lương đúng mức tối đa -> Hợp lệ")
    void validate_shouldPass_whenSalaryEqualsMaximum() {
        var d = valid();
        d.setMinSalary(RequisitionValidator.SALARY_MAX);
        d.setMaxSalary(RequisitionValidator.SALARY_MAX);
        expectPass(d);
    }

    @Test
    @DisplayName("Lương vượt mức tối đa -> Báo lỗi")
    void validate_shouldReject_whenSalaryExceedsMaximum() {
        var d = valid();
        d.setMaxSalary(new BigDecimal("1000000001"));
        assertEquals("Mức lương không được vượt quá 1.000.000.000 VND/tháng.",
                expectFail(d).getErrors().get("maxSalary"));
    }

    @Test
    @DisplayName("Lương 3 chữ số thập phân -> Báo lỗi")
    void validate_shouldReject_whenSalaryHasThreeDecimals() {
        var d = valid();
        d.setMinSalary(new BigDecimal("100.001"));
        assertTrue(expectFail(d).getErrors().containsKey("minSalary"));
    }

    @Test
    @DisplayName("Lương 2 chữ số thập phân -> Hợp lệ")
    void validate_shouldPass_whenSalaryHasTwoDecimals() {
        var d = valid();
        d.setMinSalary(new BigDecimal("100.50"));
        d.setMaxSalary(new BigDecimal("200.75"));
        expectPass(d);
    }

    @Test
    @DisplayName("Lương tối thiểu = tối đa -> Hợp lệ")
    void validate_shouldPass_whenMinSalaryEqualsMax() {
        var d = valid();
        d.setMinSalary(new BigDecimal("5000000"));
        d.setMaxSalary(new BigDecimal("5000000"));
        expectPass(d);
    }

    // VAL-012 — Ngày bắt đầu dự kiến

    @Test
    @DisplayName("Gửi duyệt với ngày bắt đầu hôm qua -> Báo lỗi")
    void validate_shouldReject_whenSubmitStartDateIsYesterday() {
        var d = valid();
        d.setExpectedStartDate(TODAY.minusDays(1));
        assertTrue(expectFail(d).getErrors().containsKey("expectedStartDate"));
    }

    @Test
    @DisplayName("Gửi duyệt với ngày bắt đầu hôm nay (biên) -> Hợp lệ")
    void validate_shouldPass_whenSubmitStartDateIsToday() {
        var d = valid();
        d.setExpectedStartDate(TODAY);
        expectPass(d);
    }

    @Test
    @DisplayName("Ngày bắt đầu 3 tháng tới -> Hợp lệ")
    void validate_shouldPass_whenSubmitStartDateIsInFuture() {
        var d = valid();
        d.setExpectedStartDate(TODAY.plusMonths(3));
        expectPass(d);
    }

    @Test
    @DisplayName("Ngày bắt đầu đúng 1 năm sau (biên) -> Hợp lệ")
    void validate_shouldPass_whenStartDateIsExactlyOneYearAhead() {
        var d = valid();
        d.setExpectedStartDate(TODAY.plusYears(1));
        expectPass(d);
    }

    @Test
    @DisplayName("Ngày bắt đầu quá 1 năm (kể cả nháp, năm 5 chữ số) -> Báo lỗi")
    void validate_shouldReject_whenStartDateIsMoreThanOneYearAhead() {
        var submit = valid();
        submit.setExpectedStartDate(TODAY.plusYears(1).plusDays(1));
        assertEquals("Ngày bắt đầu dự kiến không được quá 1 năm kể từ hôm nay.",
                expectFail(submit).getErrors().get("expectedStartDate"));

        // Năm 5 chữ số (gõ nhầm trên ô ngày) cũng bị chặn khi lưu nháp, không để DB báo
        // lỗi
        var draft = minimalDraft();
        draft.setExpectedStartDate(LocalDate.of(20266, 1, 1));
        assertTrue(expectFail(draft).getErrors().containsKey("expectedStartDate"));
    }

    // Xuống dòng "\r\n" (trình duyệt gửi lên) được đổi thành "\n" trước khi đếm ký
    // tự

    @Test
    @DisplayName("Xuống dòng kiểu Windows -> Đổi thành xuống dòng chuẩn và cắt khoảng trắng")
    void validate_shouldNormalizeLineBreaks_whenTextHasWindowsLineBreaks() {
        var d = valid();
        d.setJobDescription("  Dòng 1\r\nDòng 2\r\n  ");
        expectPass(d);
        assertEquals("Dòng 1\nDòng 2", d.getJobDescription());
    }

    @Test
    @DisplayName("Văn bản nhiều dòng -> Đếm độ dài sau khi chuẩn hóa")
    void validate_shouldCountNormalizedLength_whenTextHasLineBreaks() {
        // 1000 dòng "a" = 1000 chữ + 999 lần xuống dòng = 1999 ký tự sau khi chuẩn hóa
        // (gửi lên là 2998 ký tự)
        String text = String.join("\r\n", java.util.Collections.nCopies(1000, "a"));
        var d = valid();
        d.setReasonForHiring(text);
        expectPass(d);
        assertEquals(1999, d.getReasonForHiring().length());
    }

    @Test
    @DisplayName("Gửi duyệt thiếu ngày bắt đầu -> Báo lỗi")
    void validate_shouldReject_whenSubmitStartDateIsNull() {
        var d = valid();
        d.setExpectedStartDate(null);
        assertTrue(expectFail(d).getErrors().containsKey("expectedStartDate"));
    }

    @Test
    @DisplayName("Nháp thiếu ngày bắt đầu -> Hợp lệ")
    void validate_shouldPass_whenDraftStartDateIsNull() {
        var d = minimalDraft();
        d.setExpectedStartDate(null);
        expectPass(d);
    }

    @Test
    @DisplayName("Nháp với ngày đã qua -> Hợp lệ (chỉ chặn khi gửi duyệt)")
    void validate_shouldPass_whenDraftStartDateIsInPast() {
        var d = minimalDraft();
        d.setExpectedStartDate(TODAY.minusDays(1));
        expectPass(d);
    }

    // VAL-013 — Danh sách tiêu chí và dòng trắng

    @Test
    @DisplayName("Danh sách tiêu chí null -> Thành danh sách rỗng, nháp hợp lệ")
    void validate_shouldUseEmptyList_whenCriteriaListIsNull() {
        var d = minimalDraft();
        d.setScreeningCriteria(null);
        expectPass(d);
        assertNotNull(d.getScreeningCriteria());
    }

    @Test
    @DisplayName("Gửi duyệt không có tiêu chí -> Báo lỗi")
    void validate_shouldReject_whenSubmitHasNoCriteria() {
        var d = valid();
        d.setScreeningCriteria(new ArrayList<>());
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria"));
    }

    @Test
    @DisplayName("Đúng 50 tiêu chí (biên) -> Hợp lệ")
    void validate_shouldPass_whenCriteriaCountEqualsLimit() {
        var d = minimalDraft();
        var list = new ArrayList<ScreeningCriteriaRequest>();
        for (int i = 0; i < 50; i++)
            list.add(criterion("C" + i, "1"));
        d.setScreeningCriteria(list);
        expectPass(d);
    }

    @Test
    @DisplayName("51 tiêu chí -> Báo lỗi")
    void validate_shouldReject_whenCriteriaCountExceedsLimit() {
        var d = minimalDraft();
        var list = new ArrayList<ScreeningCriteriaRequest>();
        for (int i = 0; i <= 50; i++)
            list.add(ScreeningCriteriaRequest.builder().criteriaName("C" + i).build());
        d.setScreeningCriteria(list);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria"));
    }

    @Test
    @DisplayName("Dòng tiêu chí null (gửi giả) -> Báo lỗi")
    void validate_shouldReject_whenCriteriaRowIsNull() {
        var d = minimalDraft();
        var list = new ArrayList<ScreeningCriteriaRequest>();
        list.add(null);
        d.setScreeningCriteria(list);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria"));
    }

    @Test
    @DisplayName("Nháp có dòng tiêu chí trống -> Bỏ qua dòng đó")
    void validate_shouldSkipBlankRow_whenDraft() {
        var d = minimalDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("  ").criteriaType(null)
                        .requiredValue("").isMandatory(false).build())));
        expectPass(d);
    }

    // VAL-014 — Các field của một tiêu chí

    @Test
    @DisplayName("Tiêu chí thiếu tên -> Báo lỗi")
    void validate_shouldReject_whenCriteriaNameIsMissing() {
        var d = valid();
        d.getScreeningCriteria().get(0).setCriteriaName(null);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].criteriaName"));
    }

    @Test
    @DisplayName("Tiêu chí thiếu loại -> Báo lỗi")
    void validate_shouldReject_whenCriteriaTypeIsMissing() {
        var d = valid();
        d.getScreeningCriteria().get(0).setCriteriaType(null);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].criteriaType"));
    }

    @Test
    @DisplayName("Tiêu chí thiếu giá trị yêu cầu -> Báo lỗi")
    void validate_shouldReject_whenCriteriaRequiredValueIsMissing() {
        var d = valid();
        d.getScreeningCriteria().get(0).setRequiredValue(null);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].requiredValue"));
    }

    @Test
    @DisplayName("Tiêu chí thiếu trọng số -> Báo lỗi")
    void validate_shouldReject_whenCriteriaWeightIsMissing() {
        var d = valid();
        d.getScreeningCriteria().get(0).setWeight(null);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].weight"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Education", "Experience", "Skill", "Knockout" })
    @DisplayName("Loại tiêu chí trong danh mục -> Hợp lệ")
    void validate_shouldPass_whenCriteriaTypeIsInList(String type) {
        var d = valid();
        d.getScreeningCriteria().get(0).setCriteriaType(type);
        expectPass(d);
    }

    @Test
    @DisplayName("Loại tiêu chí ngoài danh mục -> Báo lỗi")
    void validate_shouldReject_whenCriteriaTypeIsNotInList() {
        var d = valid();
        d.getScreeningCriteria().get(0).setCriteriaType("Language");
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].criteriaType"));
    }

    // VAL-015 — Tên tiêu chí trùng

    @Test
    @DisplayName("Hai tiêu chí trùng tên -> Báo lỗi")
    void validate_shouldReject_whenCriteriaNamesAreDuplicated() {
        var d = valid();
        d.getScreeningCriteria().add(ScreeningCriteriaRequest.builder()
                .criteriaName("Java").criteriaType("Skill").requiredValue("Three years")
                .weight(new BigDecimal("0")).build());
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[1].criteriaName"));
    }

    @Test
    @DisplayName("Hai tiêu chí trùng tên khác hoa / thường -> Báo lỗi")
    void validate_shouldReject_whenCriteriaNamesDifferOnlyInCase() {
        var d = valid();
        d.getScreeningCriteria().add(ScreeningCriteriaRequest.builder()
                .criteriaName(" java ").criteriaType("Skill").requiredValue("Three years")
                .weight(new BigDecimal("50")).build());
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[1].criteriaName"));
    }

    @Test
    @DisplayName("Hai tiêu chí khác tên -> Hợp lệ")
    void validate_shouldPass_whenCriteriaNamesAreDistinct() {
        var d = valid();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Java").criteriaType("Skill")
                        .requiredValue("Two years").weight(new BigDecimal("60")).build(),
                ScreeningCriteriaRequest.builder().criteriaName("Spring Boot").criteriaType("Skill")
                        .requiredValue("One year").weight(new BigDecimal("40")).build())));
        expectPass(d);
    }

    // VAL-016 — ID tiêu chí trùng

    @Test
    @DisplayName("Một criteriaId gửi hai lần (ID giả) -> Báo lỗi")
    void validate_shouldReject_whenCriteriaIdIsSentTwice() {
        var d = minimalDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaId(5).criteriaName("Java")
                        .criteriaType("Skill").requiredValue("Two years").weight(new BigDecimal("100")).build(),
                ScreeningCriteriaRequest.builder().criteriaId(5).criteriaName("Spring")
                        .criteriaType("Skill").requiredValue("One year").weight(new BigDecimal("50")).build())));
        var e = expectFail(d);
        assertTrue(e.getErrors().keySet().stream().anyMatch(k -> k.startsWith("screeningCriteria[1]")));
    }

    @Test
    @DisplayName("Các criteriaId khác nhau -> Hợp lệ")
    void validate_shouldPass_whenCriteriaIdsAreDistinct() {
        var d = minimalDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                withId(criterion("Java", "50"), 1),
                withId(criterion("Python", "50"), 2))));
        expectPass(d);
    }

    // VAL-017 — Giới hạn weight

    @Test
    @DisplayName("Gửi duyệt với trọng số 0 -> Báo lỗi")
    void validate_shouldReject_whenSubmitWeightIsZero() {
        var d = valid();
        d.getScreeningCriteria().get(0).setWeight(BigDecimal.ZERO);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].weight"));
    }

    @Test
    @DisplayName("Gửi duyệt với trọng số âm -> Báo lỗi")
    void validate_shouldReject_whenSubmitWeightIsNegative() {
        var d = valid();
        d.getScreeningCriteria().get(0).setWeight(new BigDecimal("-1"));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].weight"));
    }

    @Test
    @DisplayName("Gửi duyệt với trọng số 100 (biên) -> Hợp lệ")
    void validate_shouldPass_whenSubmitWeightIs100() {
        var d = valid();
        d.getScreeningCriteria().get(0).setWeight(new BigDecimal("100"));
        expectPass(d);
    }

    @Test
    @DisplayName("Gửi duyệt với trọng số > 100 -> Báo lỗi")
    void validate_shouldReject_whenSubmitWeightExceeds100() {
        var d = valid();
        d.getScreeningCriteria().get(0).setWeight(new BigDecimal("100.01"));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].weight"));
    }

    @Test
    @DisplayName("Trọng số 3 chữ số thập phân -> Báo lỗi")
    void validate_shouldReject_whenWeightHasThreeDecimals() {
        var d = valid();
        d.getScreeningCriteria().get(0).setWeight(new BigDecimal("33.333"));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].weight"));
    }

    @Test
    @DisplayName("Trọng số 2 chữ số thập phân -> Hợp lệ")
    void validate_shouldPass_whenWeightHasTwoDecimals() {
        var d = valid();
        d.getScreeningCriteria().get(0).setWeight(new BigDecimal("100.00"));
        expectPass(d);
    }

    @Test
    @DisplayName("Nháp với trọng số 0 -> Báo lỗi")
    void validate_shouldReject_whenDraftWeightIsZero() {
        var d = minimalDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Java").weight(BigDecimal.ZERO).build())));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].weight"));
    }

    @Test
    @DisplayName("Nháp với trọng số 100 -> Hợp lệ")
    void validate_shouldPass_whenDraftWeightIs100() {
        var d = minimalDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                criterion("Java", "100"))));
        expectPass(d);
    }

    @Test
    @DisplayName("Nháp với trọng số > 100 -> Báo lỗi")
    void validate_shouldReject_whenDraftWeightExceeds100() {
        var d = minimalDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                criterion("Java", "100.01"))));
        assertEquals("Trọng số phải từ 0.01 đến 100 với tối đa hai chữ số thập phân.",
                expectFail(d).getErrors().get("screeningCriteria[0].weight"));
    }

    // VAL-018 — Tổng weight khi Submit

    @Test
    @DisplayName("Gửi duyệt tổng trọng số < 100 -> Báo lỗi")
    void validate_shouldReject_whenSubmitTotalWeightIsBelow100() {
        var d = valid();
        d.getScreeningCriteria().get(0).setWeight(new BigDecimal("99.99"));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria"));
    }

    @Test
    @DisplayName("Gửi duyệt tổng trọng số > 100 -> Báo lỗi")
    void validate_shouldReject_whenSubmitTotalWeightIsAbove100() {
        var d = valid();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Java").criteriaType("Skill")
                        .requiredValue("Two years").weight(new BigDecimal("60")).build(),
                ScreeningCriteriaRequest.builder().criteriaName("Spring").criteriaType("Skill")
                        .requiredValue("One year").weight(new BigDecimal("60")).build())));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria"));
    }

    @Test
    @DisplayName("Gửi duyệt tổng trọng số đúng 100 -> Hợp lệ")
    void validate_shouldPass_whenSubmitTotalWeightIsExactly100() {
        var d = valid();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Java").criteriaType("Skill")
                        .requiredValue("Two years").weight(new BigDecimal("60.00")).build(),
                ScreeningCriteriaRequest.builder().criteriaName("Spring").criteriaType("Skill")
                        .requiredValue("One year").weight(new BigDecimal("40.00")).build())));
        expectPass(d);
    }

    // VAL-019 — Weight khi Draft

    @Test
    @DisplayName("Nháp tổng trọng số khác 100 -> Hợp lệ")
    void validate_shouldPass_whenDraftTotalWeightIsNot100() {
        var d = minimalDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                criterion("Java", "30"))));
        expectPass(d);
    }

    @Test
    @DisplayName("Nháp với trọng số 0.01 (biên) -> Hợp lệ")
    void validate_shouldPass_whenDraftWeightIsMinimumPositive() {
        var d = minimalDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                criterion("Java", "0.01"))));
        expectPass(d);
    }

    @Test
    @DisplayName("Trọng số > 100 -> Báo lỗi cả nháp lẫn gửi duyệt")
    void validate_shouldReject_whenWeightExceeds100InDraftOrSubmit() {
        var submit = valid();
        submit.getScreeningCriteria().get(0).setWeight(new BigDecimal("101"));
        assertTrue(expectFail(submit).getErrors().containsKey("screeningCriteria[0].weight"));

        var draft = minimalDraft();
        draft.setScreeningCriteria(new ArrayList<>(List.of(
                criterion("Java", "101"))));
        assertTrue(expectFail(draft).getErrors().containsKey("screeningCriteria[0].weight"));
    }

    // VAL-020 — Mandatory và nhận diện dòng trắng

    @Test
    @DisplayName("Dòng chỉ tick Bắt buộc -> Không phải dòng trống, phải điền đủ")
    void validate_shouldRequireFields_whenRowIsOnlyMarkedMandatory() {
        var d = minimalDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder()
                        .criteriaName(null).criteriaType(null).requiredValue(null)
                        .isMandatory(true).build())));
        // Bật "Bắt buộc" nghĩa là dòng đã được nhập → phải điền đủ (các cột NOT NULL),
        // kể cả khi lưu nháp
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].criteriaName"));
    }

    @Test
    @DisplayName("Dòng trống, không tick Bắt buộc -> Bỏ qua khi lưu nháp")
    void validate_shouldSkipRow_whenRowIsBlankAndNotMandatory() {
        var d = minimalDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder()
                        .criteriaName("  ").criteriaType(null).requiredValue("  ")
                        .isMandatory(false).build())));
        expectPass(d);
    }

    @Test
    @DisplayName("Gửi duyệt có dòng Bắt buộc -> Phải điền đủ")
    void validate_shouldRequireAllFields_whenSubmitHasMandatoryRow() {
        var d = valid();
        d.getScreeningCriteria().get(0).setIsMandatory(true);
        d.getScreeningCriteria().get(0).setRequiredValue(null);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].requiredValue"));
    }

    // Multi-error and utility tests ----------------------------------------

    @Test
    @DisplayName("Nhiều ô sai cùng lúc -> Báo đủ tất cả lỗi một lần")
    void validate_shouldReportAllErrors_whenManyFieldsAreInvalid() {
        var d = valid();
        d.setTitle("  ");
        d.setEmploymentType("Bad");
        d.setWorkModel("Bad");
        d.setExpectedStartDate(TODAY.minusDays(1));
        d.setNumberOfPositions(null);
        var keys = expectFail(d).getErrors().keySet();
        assertTrue(keys.contains("title"));
        assertTrue(keys.contains("employmentType"));
        assertTrue(keys.contains("workModel"));
        assertTrue(keys.contains("expectedStartDate"));
        assertTrue(keys.contains("numberOfPositions"));
    }

    @Test
    @DisplayName("cleanString(null) -> null")
    void cleanString_shouldReturnNull_whenInputIsNull() {
        assertNull(RequisitionValidator.cleanString(null));
    }

    @Test
    @DisplayName("cleanString chỉ khoảng trắng -> null")
    void cleanString_shouldReturnNull_whenInputIsSpaceOnly() {
        assertNull(RequisitionValidator.cleanString("   "));
    }

    @Test
    @DisplayName("cleanString cắt khoảng trắng đầu / cuối")
    void cleanString_shouldTrim_whenInputHasSurroundingSpaces() {
        assertEquals("hello", RequisitionValidator.cleanString("  hello  "));
    }

    @Test
    @DisplayName("cleanString giữ khoảng trắng ở giữa")
    void cleanString_shouldKeepInnerSpaces_whenInputHasInnerSpaces() {
        assertEquals("hello world", RequisitionValidator.cleanString("  hello world  "));
    }

    @Test
    @DisplayName("Dòng toàn null, không bắt buộc -> Là dòng trống")
    void isBlankCriteria_shouldReturnTrue_whenAllFieldsAreNullAndNotMandatory() {
        assertTrue(RequisitionValidator.isBlankCriteria(ScreeningCriteriaRequest.builder()
                .criteriaName(null).criteriaType(null).requiredValue(null).isMandatory(false).build()));
    }

    @Test
    @DisplayName("Dòng có tick Bắt buộc -> Không phải dòng trống")
    void isBlankCriteria_shouldReturnFalse_whenMarkedMandatory() {
        assertFalse(RequisitionValidator.isBlankCriteria(ScreeningCriteriaRequest.builder()
                .criteriaName(null).criteriaType(null).requiredValue(null).isMandatory(true).build()));
    }

    @Test
    @DisplayName("Dòng có tên -> Không phải dòng trống")
    void isBlankCriteria_shouldReturnFalse_whenNameIsPresent() {
        assertFalse(RequisitionValidator.isBlankCriteria(ScreeningCriteriaRequest.builder()
                .criteriaName("Java").criteriaType(null).requiredValue(null).isMandatory(false).build()));
    }

    @CsvSource({
            "Full-time, On-site, Any",
            "Part-time, Remote, Male",
            "Internship, Hybrid, Female",
            "Contract, On-site,"
    })
    @ParameterizedTest
    @DisplayName("Các tổ hợp hình thức / mô hình / giới tính hợp lệ -> Hợp lệ")
    void validate_shouldPass_whenChoiceFieldsAreValidCombination(String type, String model, String gender) {
        var d = valid();
        d.setEmploymentType(type);
        d.setWorkModel(model);
        d.setGender(gender);
        expectPass(d);
    }
}
