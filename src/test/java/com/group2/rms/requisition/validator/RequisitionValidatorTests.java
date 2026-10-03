package com.group2.rms.requisition.validator;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.exception.RequisitionValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * STAGE 1 — UNIT TEST VALIDATION.
 * Đối tượng: RequisitionValidator.validate(RequisitionRequest).
 * Không Spring context, không database. Naming: <rule-id>_<short description>.
 * Các nhóm VAL-001..020 bên dưới đã có test; comment không khẳng định chúng đã chạy/pass.
 *
 * CÁCH ĐỌC / LÀM TIẾP:
 * 1. valid() tạo Submit hợp lệ; emptyDraft() tạo Draft chưa đủ dữ liệu.
 * 2. Mỗi case đổi dữ liệu ở Arrange, gọi validator ở Act, kiểm tra đúng field lỗi ở Assert.
 * 3. Dùng parameterized test cho các giá trị biên cùng quy tắc; không thêm rule ngoài code.
 * 4. Draft bỏ kiểm tra đủ field, nhưng vẫn kiểm tra tính hợp lệ của giá trị đã nhập.
 *
 * TODO [VAL-REVIEW] Đối chiếu từng assertion với validator; test có tên đúng chưa đủ
 *                   để khẳng định đã kiểm tra đúng hành vi. Không xóa coverage đang có.
 * TODO [VAL-LENGTH] Bổ sung biên requiredValue: đúng 255 và 256 ký tự, kiểm tra đúng key lỗi.
 * TODO [VAL-RESULT] Chạy riêng RequisitionValidatorTests; ghi số executed/pass/fail/skipped
 *                   theo kết quả thực tế (parameterized test có nhiều lượt chạy).
 * TODO [VAL-GAPS] Ghi lại nhánh còn thiếu hoặc rule mâu thuẫn; không tự sửa production.
 *
 * Department không tồn tại, quyền người dùng, criteria ID của request khác: Stage 2/4.
 * Chuỗi ngày/số không parse được khi gửi HTTP: Stage 3, không thuộc validator nhận DTO đã bind.
 * Kết thúc Stage 1 thì dừng; Stage 2 chỉ bắt đầu khi được yêu cầu tiếp tục.
 */
class RequisitionValidatorTests {

    private final RequisitionValidator validator = new RequisitionValidator();

    // helpers ---------------------------------------------------------------

    private RequisitionRequest valid() {
        return RequisitionRequest.builder()
                .action("submit")
                .title("Java Engineer")
                .departmentId(1)
                .numberOfPositions(1)
                .employmentType("Full-time")
                .workModel("On-site")
                .expectedStartDate(LocalDate.now().plusDays(1))
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

    private RequisitionValidationException expectFail(RequisitionRequest req) {
        return assertThrows(RequisitionValidationException.class, () -> validator.validate(req));
    }

    private void expectPass(RequisitionRequest req) {
        assertDoesNotThrow(() -> validator.validate(req));
    }

    // VAL-001 — Submit hợp lệ

    @Test void val001_completeValidSubmitPasses() { expectPass(valid()); }

    @Test void val001_hybridWorkModelIsAccepted() {
        var d = valid(); d.setWorkModel("Hybrid"); expectPass(d);
    }

    @Test void val001_remoteWorkModelIsAccepted() {
        var d = valid(); d.setWorkModel("Remote"); expectPass(d);
    }

    // VAL-002 — Draft được thiếu dữ liệu

    @Test void val002_emptyDraftPassesWithNoFieldsSet() { expectPass(emptyDraft()); }

    @Test void val002_draftWithPartialCriteriaAndLowWeightPasses() {
        var d = emptyDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Partial").weight(new BigDecimal("20")).build())));
        expectPass(d);
    }

    @Test void val002_draftDoesNotRequireDepartmentOrDate() {
        var d = emptyDraft(); d.setTitle("Draft Title"); expectPass(d);
    }

    @Test void val002_submitWithSameDraftDataFails() {
        var d = emptyDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Partial").weight(new BigDecimal("20")).build())));
        d.setAction("submit");
        expectFail(d);
    }

    // VAL-003 — Draft vẫn kiểm tra giá trị đã nhập

    @Test void val003_draftWithInvalidWorkModelIsRejected() {
        var d = emptyDraft(); d.setWorkModel("Flexible");
        assertTrue(expectFail(d).getErrors().containsKey("workModel"));
    }

    @Test void val003_draftWithInvalidEmploymentTypeIsRejected() {
        var d = emptyDraft(); d.setEmploymentType("Freelance");
        assertTrue(expectFail(d).getErrors().containsKey("employmentType"));
    }

    @Test void val003_draftWithInvalidGenderIsRejected() {
        var d = emptyDraft(); d.setGender("Unknown");
        assertTrue(expectFail(d).getErrors().containsKey("gender"));
    }

    @Test void val003_draftWithNegativePositionsIsRejected() {
        var d = emptyDraft(); d.setNumberOfPositions(-1);
        assertTrue(expectFail(d).getErrors().containsKey("numberOfPositions"));
    }

    @Test void val003_draftWithZeroPositionsIsRejected() {
        var d = emptyDraft(); d.setNumberOfPositions(0);
        assertTrue(expectFail(d).getErrors().containsKey("numberOfPositions"));
    }

    // VAL-004 — Action draft/submit

    @Test void val004_nullActionIsRejected() {
        var d = valid(); d.setAction(null);
        assertTrue(expectFail(d).getErrors().containsKey("action"));
    }

    @Test void val004_blankActionIsRejected() {
        var d = valid(); d.setAction("   ");
        assertTrue(expectFail(d).getErrors().containsKey("action"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "Submit", "save", "approve", "publish", ""})
    void val004_unrecognizedActionIsRejected(String action) {
        var d = valid(); d.setAction(action);
        assertTrue(expectFail(d).getErrors().containsKey("action"), "action '" + action + "' must be rejected");
    }

    @ParameterizedTest
    @ValueSource(strings = {"draft", "submit"})
    void val004_validActionsAreAccepted(String action) {
        var d = valid(); d.setAction(action); expectPass(d);
    }

    // VAL-005 — Chuẩn hóa text và khoảng trắng

    @Test void val005_titleIsNullAfterWhitespaceOnly() {
        var d = valid(); d.setTitle("   ");
        assertTrue(expectFail(d).getErrors().containsKey("title"));
    }

    @Test void val005_titleWithLeadingTrailingSpacesIsTrimmed() {
        var d = valid(); d.setTitle("  Java Engineer  ");
        expectPass(d);
        assertEquals("Java Engineer", d.getTitle());
    }

    @Test void val005_workLocationIsNullAfterWhitespaceOnly() {
        var d = valid(); d.setWorkLocation("   ");
        assertTrue(expectFail(d).getErrors().containsKey("workLocation"));
    }

    @Test void val005_nullGenderPassesForSubmit() {
        var d = valid(); d.setGender(null); expectPass(d);
    }

    @Test void val005_probationDurationWhitespaceIsNullified() {
        var d = emptyDraft(); d.setProbationDuration("   ");
        expectPass(d);
        assertNull(d.getProbationDuration());
    }

    // VAL-006 — Tiêu đề placeholder của Draft

    @Test void val006_placeholderTitleRejectedOnSubmit() {
        var d = valid(); d.setTitle("Untitled requisition");
        assertTrue(expectFail(d).getErrors().containsKey("title"));
    }

    @Test void val006_placeholderTitleCaseInsensitive() {
        var d = valid(); d.setTitle("UNTITLED REQUISITION");
        assertTrue(expectFail(d).getErrors().containsKey("title"));
    }

    @Test void val006_placeholderTitleAllowedAsDraft() {
        var d = emptyDraft(); d.setTitle("Untitled requisition"); expectPass(d);
    }

    // VAL-007 — Department và headcount

    @Test void val007_missingDepartmentRejectedOnSubmit() {
        var d = valid(); d.setDepartmentId(null);
        assertTrue(expectFail(d).getErrors().containsKey("departmentId"));
    }

    @Test void val007_missingDepartmentAllowedAsDraft() {
        var d = emptyDraft(); d.setDepartmentId(null); expectPass(d);
    }

    @Test void val007_missingPositionsRejectedOnSubmit() {
        var d = valid(); d.setNumberOfPositions(null);
        assertTrue(expectFail(d).getErrors().containsKey("numberOfPositions"));
    }

    @Test void val007_zeroPositionsAlwaysRejected() {
        var d = valid(); d.setNumberOfPositions(0);
        assertTrue(expectFail(d).getErrors().containsKey("numberOfPositions"));
    }

    @Test void val007_negativePositionsAlwaysRejected() {
        var d = valid(); d.setNumberOfPositions(-5);
        assertTrue(expectFail(d).getErrors().containsKey("numberOfPositions"));
    }

    @Test void val007_positivePositionsAreValid() {
        var d = valid(); d.setNumberOfPositions(10); expectPass(d);
    }

    // VAL-008 — Employment type, work model, gender

    @ParameterizedTest
    @ValueSource(strings = {"Full-time", "Part-time", "Internship", "Contract"})
    void val008_allValidEmploymentTypesAccepted(String type) {
        var d = valid(); d.setEmploymentType(type); expectPass(d);
    }

    @ParameterizedTest
    @ValueSource(strings = {"full-time", "FULL-TIME", "Freelance", "Temp", ""})
    void val008_invalidEmploymentTypeRejected(String type) {
        var d = valid(); d.setEmploymentType(type);
        assertTrue(expectFail(d).getErrors().containsKey("employmentType"));
    }

    @Test void val008_missingEmploymentTypeRejectedOnSubmit() {
        var d = valid(); d.setEmploymentType(null);
        assertTrue(expectFail(d).getErrors().containsKey("employmentType"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"On-site", "Remote", "Hybrid"})
    void val008_allValidWorkModelsAccepted(String model) {
        var d = valid(); d.setWorkModel(model); expectPass(d);
    }

    @ParameterizedTest
    @ValueSource(strings = {"on-site", "REMOTE", "WFH", "Office", ""})
    void val008_invalidWorkModelRejectedOnSubmit(String model) {
        var d = valid(); d.setWorkModel(model);
        assertTrue(expectFail(d).getErrors().containsKey("workModel"));
    }

    @Test void val008_missingWorkModelRejectedOnSubmit() {
        var d = valid(); d.setWorkModel(null);
        assertTrue(expectFail(d).getErrors().containsKey("workModel"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Any", "Male", "Female"})
    void val008_allValidGendersAccepted(String gender) {
        var d = valid(); d.setGender(gender); expectPass(d);
    }

    @Test void val008_invalidSuppliedGenderRejected() {
        var d = valid(); d.setGender("Non-binary");
        assertTrue(expectFail(d).getErrors().containsKey("gender"));
    }

    @Test void val008_nullGenderAccepted() {
        var d = valid(); d.setGender(null); expectPass(d);
    }

    // VAL-009 — Các field text bắt buộc

    @Test void val009_missingWorkLocationRejectedOnSubmit() {
        var d = valid(); d.setWorkLocation(null);
        assertTrue(expectFail(d).getErrors().containsKey("workLocation"));
    }

    @Test void val009_missingJobDescriptionRejectedOnSubmit() {
        var d = valid(); d.setJobDescription(null);
        assertTrue(expectFail(d).getErrors().containsKey("jobDescription"));
    }

    @Test void val009_missingRequirementDetailsRejectedOnSubmit() {
        var d = valid(); d.setRequirementDetails(null);
        assertTrue(expectFail(d).getErrors().containsKey("requirementDetails"));
    }

    @Test void val009_missingReasonForHiringRejectedOnSubmit() {
        var d = valid(); d.setReasonForHiring(null);
        assertTrue(expectFail(d).getErrors().containsKey("reasonForHiring"));
    }

    @Test void val009_allMissingTextFieldsAllowedAsDraft() {
        expectPass(emptyDraft());
    }

    // VAL-010 — Giới hạn độ dài

    @Test void val010_titleAtMaxLengthPasses() {
        var d = valid(); d.setTitle("A".repeat(200)); expectPass(d);
    }

    @Test void val010_titleOverMaxLengthFails() {
        var d = valid(); d.setTitle("A".repeat(201));
        assertTrue(expectFail(d).getErrors().containsKey("title"));
    }

    @Test void val010_reasonForHiringAtMaxLengthPasses() {
        var d = valid(); d.setReasonForHiring("x".repeat(RequisitionValidator.REASON_LIMIT)); expectPass(d);
    }

    @Test void val010_reasonForHiringOverMaxLengthFails() {
        var d = valid(); d.setReasonForHiring("x".repeat(RequisitionValidator.REASON_LIMIT + 1));
        assertTrue(expectFail(d).getErrors().containsKey("reasonForHiring"));
    }

    @Test void val010_jobDescriptionAtMaxLengthPasses() {
        var d = valid(); d.setJobDescription("x".repeat(2000)); expectPass(d);
    }

    @Test void val010_jobDescriptionOverMaxLengthFails() {
        var d = valid(); d.setJobDescription("x".repeat(2001));
        assertTrue(expectFail(d).getErrors().containsKey("jobDescription"));
    }

    @Test void val010_requirementDetailsAtMaxLengthPasses() {
        var d = valid(); d.setRequirementDetails("x".repeat(2000)); expectPass(d);
    }

    @Test void val010_requirementDetailsOverMaxLengthFails() {
        var d = valid(); d.setRequirementDetails("x".repeat(2001));
        assertTrue(expectFail(d).getErrors().containsKey("requirementDetails"));
    }

    @Test void val010_workLocationAtMaxLengthPasses() {
        var d = valid(); d.setWorkLocation("x".repeat(255)); expectPass(d);
    }

    @Test void val010_workLocationOverMaxLengthFails() {
        var d = valid(); d.setWorkLocation("x".repeat(256));
        assertTrue(expectFail(d).getErrors().containsKey("workLocation"));
    }

    @Test void val010_probationDurationAtMaxLengthPasses() {
        var d = valid(); d.setProbationDuration("x".repeat(255)); expectPass(d);
    }

    @Test void val010_probationDurationOverMaxLengthFails() {
        var d = valid(); d.setProbationDuration("x".repeat(256));
        assertTrue(expectFail(d).getErrors().containsKey("probationDuration"));
    }

    @Test void val010_probationDurationNeverRequiredOnSubmit() {
        var d = valid(); d.setProbationDuration(null); expectPass(d);
    }

    @Test void val010_criteriaNameAtMaxLengthPasses() {
        var d = valid(); d.getScreeningCriteria().get(0).setCriteriaName("x".repeat(150)); expectPass(d);
    }

    @Test void val010_criteriaNameOverMaxLengthFails() {
        var d = valid(); d.getScreeningCriteria().get(0).setCriteriaName("x".repeat(151));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].criteriaName"));
    }

    // VAL-011 — Salary và độ chính xác thập phân

    @Test void val011_bothSalariesNullPass() {
        var d = valid(); d.setMinSalary(null); d.setMaxSalary(null); expectPass(d);
    }

    @Test void val011_onlyMinSalarySetPasses() {
        var d = valid(); d.setMinSalary(new BigDecimal("10000000")); d.setMaxSalary(null); expectPass(d);
    }

    @Test void val011_onlyMaxSalarySetPasses() {
        var d = valid(); d.setMinSalary(null); d.setMaxSalary(new BigDecimal("20000000")); expectPass(d);
    }

    @Test void val011_zeroSalaryIsAccepted() {
        var d = valid(); d.setMinSalary(BigDecimal.ZERO); d.setMaxSalary(BigDecimal.ZERO); expectPass(d);
    }

    @Test void val011_negativeSalaryIsRejected() {
        var d = valid(); d.setMinSalary(new BigDecimal("-1"));
        assertTrue(expectFail(d).getErrors().containsKey("minSalary"));
    }

    @Test void val011_minGreaterThanMaxIsRejected() {
        var d = valid(); d.setMinSalary(BigDecimal.TEN); d.setMaxSalary(BigDecimal.ONE);
        assertTrue(expectFail(d).getErrors().containsKey("maxSalary"));
    }

    @Test void val011_salaryAtMaxValuePasses() {
        var d = valid();
        d.setMinSalary(new BigDecimal("9999999999999999.99"));
        d.setMaxSalary(new BigDecimal("9999999999999999.99"));
        expectPass(d);
    }

    @Test void val011_salaryOverMaxValueRejected() {
        var d = valid(); d.setMaxSalary(new BigDecimal("10000000000000000.00"));
        assertTrue(expectFail(d).getErrors().containsKey("maxSalary"));
    }

    @Test void val011_salaryWith3DecimalsRejected() {
        var d = valid(); d.setMinSalary(new BigDecimal("100.001"));
        assertTrue(expectFail(d).getErrors().containsKey("minSalary"));
    }

    @Test void val011_salaryWith2DecimalsPasses() {
        var d = valid(); d.setMinSalary(new BigDecimal("100.50")); d.setMaxSalary(new BigDecimal("200.75")); expectPass(d);
    }

    @Test void val011_equalMinAndMaxPass() {
        var d = valid(); d.setMinSalary(new BigDecimal("5000000")); d.setMaxSalary(new BigDecimal("5000000")); expectPass(d);
    }

    // VAL-012 — Ngày bắt đầu dự kiến

    @Test void val012_pastDateRejectedOnSubmit() {
        var d = valid(); d.setExpectedStartDate(LocalDate.now().minusDays(1));
        assertTrue(expectFail(d).getErrors().containsKey("expectedStartDate"));
    }

    @Test void val012_todayAcceptedOnSubmit() {
        var d = valid(); d.setExpectedStartDate(LocalDate.now()); expectPass(d);
    }

    @Test void val012_futureDateAcceptedOnSubmit() {
        var d = valid(); d.setExpectedStartDate(LocalDate.now().plusMonths(3)); expectPass(d);
    }

    @Test void val012_nullDateRejectedOnSubmit() {
        var d = valid(); d.setExpectedStartDate(null);
        assertTrue(expectFail(d).getErrors().containsKey("expectedStartDate"));
    }

    @Test void val012_nullDateAllowedAsDraft() {
        var d = emptyDraft(); d.setExpectedStartDate(null); expectPass(d);
    }

    @Test void val012_pastDateAllowedAsDraft() {
        var d = emptyDraft(); d.setExpectedStartDate(LocalDate.now().minusDays(1)); expectPass(d);
    }

    // VAL-013 — Danh sách tiêu chí và dòng trắng

    @Test void val013_nullCriteriaListNormalizedToEmptyAndPassesDraft() {
        var d = emptyDraft(); d.setScreeningCriteria(null);
        expectPass(d);
        assertNotNull(d.getScreeningCriteria());
    }

    @Test void val013_emptyCriteriaRejectedOnSubmit() {
        var d = valid(); d.setScreeningCriteria(new ArrayList<>());
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria"));
    }

    @Test void val013_criteriaAt50Passes() {
        var d = emptyDraft();
        var list = new ArrayList<ScreeningCriteriaRequest>();
        for (int i = 0; i < 50; i++) list.add(ScreeningCriteriaRequest.builder().criteriaName("C" + i).build());
        d.setScreeningCriteria(list);
        expectPass(d);
    }

    @Test void val013_criteriaOver50Rejected() {
        var d = emptyDraft();
        var list = new ArrayList<ScreeningCriteriaRequest>();
        for (int i = 0; i <= 50; i++) list.add(ScreeningCriteriaRequest.builder().criteriaName("C" + i).build());
        d.setScreeningCriteria(list);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria"));
    }

    @Test void val013_nullRowInCriteriaRejected() {
        var d = emptyDraft();
        var list = new ArrayList<ScreeningCriteriaRequest>();
        list.add(null);
        d.setScreeningCriteria(list);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria"));
    }

    @Test void val013_blankRowSkippedInDraft() {
        var d = emptyDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("  ").criteriaType(null)
                        .requiredValue("").isMandatory(false).build())));
        expectPass(d);
    }

    // VAL-014 — Các field của một tiêu chí

    @Test void val014_missingCriteriaNameRejectedOnSubmit() {
        var d = valid(); d.getScreeningCriteria().get(0).setCriteriaName(null);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].criteriaName"));
    }

    @Test void val014_missingCriteriaTypeRejectedOnSubmit() {
        var d = valid(); d.getScreeningCriteria().get(0).setCriteriaType(null);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].criteriaType"));
    }

    @Test void val014_missingRequiredValueRejectedOnSubmit() {
        var d = valid(); d.getScreeningCriteria().get(0).setRequiredValue(null);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].requiredValue"));
    }

    @Test void val014_missingWeightRejectedOnSubmit() {
        var d = valid(); d.getScreeningCriteria().get(0).setWeight(null);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].weight"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Education", "Experience", "Skill", "Knockout"})
    void val014_allValidCriteriaTypesAccepted(String type) {
        var d = valid(); d.getScreeningCriteria().get(0).setCriteriaType(type); expectPass(d);
    }

    @Test void val014_invalidCriteriaTypeRejected() {
        var d = valid(); d.getScreeningCriteria().get(0).setCriteriaType("Language");
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].criteriaType"));
    }

    // VAL-015 — Tên tiêu chí trùng

    @Test void val015_duplicateCriterionNameRejected() {
        var d = valid();
        d.getScreeningCriteria().add(ScreeningCriteriaRequest.builder()
                .criteriaName("Java").criteriaType("Skill").requiredValue("Three years")
                .weight(new BigDecimal("0")).build());
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[1].criteriaName"));
    }

    @Test void val015_duplicateCriterionNameCaseInsensitiveRejected() {
        var d = valid();
        d.getScreeningCriteria().add(ScreeningCriteriaRequest.builder()
                .criteriaName(" java ").criteriaType("Skill").requiredValue("Three years")
                .weight(new BigDecimal("50")).build());
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[1].criteriaName"));
    }

    @Test void val015_twoDistinctCriterionNamesPass() {
        var d = valid();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Java").criteriaType("Skill")
                        .requiredValue("Two years").weight(new BigDecimal("60")).build(),
                ScreeningCriteriaRequest.builder().criteriaName("Spring Boot").criteriaType("Skill")
                        .requiredValue("One year").weight(new BigDecimal("40")).build())));
        expectPass(d);
    }

    // VAL-016 — ID tiêu chí trùng

    @Test void val016_duplicateCriterionIdsRejected() {
        var d = emptyDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaId(5).criteriaName("Java")
                        .criteriaType("Skill").requiredValue("Two years").weight(new BigDecimal("100")).build(),
                ScreeningCriteriaRequest.builder().criteriaId(5).criteriaName("Spring")
                        .criteriaType("Skill").requiredValue("One year").weight(new BigDecimal("50")).build())));
        var e = expectFail(d);
        assertTrue(e.getErrors().keySet().stream().anyMatch(k -> k.startsWith("screeningCriteria[1]")));
    }

    @Test void val016_distinctCriterionIdsPass() {
        var d = emptyDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaId(1).criteriaName("Java").build(),
                ScreeningCriteriaRequest.builder().criteriaId(2).criteriaName("Python").build())));
        expectPass(d);
    }

    // VAL-017 — Giới hạn weight

    @Test void val017_zeroWeightRejectedOnSubmit() {
        var d = valid(); d.getScreeningCriteria().get(0).setWeight(BigDecimal.ZERO);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].weight"));
    }

    @Test void val017_negativeWeightRejectedOnSubmit() {
        var d = valid(); d.getScreeningCriteria().get(0).setWeight(new BigDecimal("-1"));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].weight"));
    }

    @Test void val017_weight100ValidOnSubmit() {
        var d = valid(); d.getScreeningCriteria().get(0).setWeight(new BigDecimal("100")); expectPass(d);
    }

    @Test void val017_weightOver100RejectedOnSubmit() {
        var d = valid(); d.getScreeningCriteria().get(0).setWeight(new BigDecimal("100.01"));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].weight"));
    }

    @Test void val017_threeDecimalPlacesRejected() {
        var d = valid(); d.getScreeningCriteria().get(0).setWeight(new BigDecimal("33.333"));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].weight"));
    }

    @Test void val017_twoDecimalPlacesAcceptedOnSubmit() {
        var d = valid(); d.getScreeningCriteria().get(0).setWeight(new BigDecimal("100.00")); expectPass(d);
    }

    @Test void val017_zeroWeightRejectedOnDraft() {
        var d = emptyDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Java").weight(BigDecimal.ZERO).build())));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].weight"));
    }

    @Test void val017_weightUpTo999_99OnDraftPasses() {
        var d = emptyDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Java").weight(new BigDecimal("999.99")).build())));
        expectPass(d);
    }

    @Test void val017_weightOver999_99OnDraftRejected() {
        var d = emptyDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Java").weight(new BigDecimal("1000.00")).build())));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].weight"));
    }

    // VAL-018 — Tổng weight khi Submit

    @Test void val018_totalWeightBelow100RejectedOnSubmit() {
        var d = valid(); d.getScreeningCriteria().get(0).setWeight(new BigDecimal("99.99"));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria"));
    }

    @Test void val018_totalWeightAbove100RejectedOnSubmit() {
        var d = valid();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Java").criteriaType("Skill")
                        .requiredValue("Two years").weight(new BigDecimal("60")).build(),
                ScreeningCriteriaRequest.builder().criteriaName("Spring").criteriaType("Skill")
                        .requiredValue("One year").weight(new BigDecimal("60")).build())));
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria"));
    }

    @Test void val018_totalWeightExactly100PassesOnSubmit() {
        var d = valid();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Java").criteriaType("Skill")
                        .requiredValue("Two years").weight(new BigDecimal("60.00")).build(),
                ScreeningCriteriaRequest.builder().criteriaName("Spring").criteriaType("Skill")
                        .requiredValue("One year").weight(new BigDecimal("40.00")).build())));
        expectPass(d);
    }

    // VAL-019 — Weight khi Draft

    @Test void val019_draftDoesNotRequireTotalWeightOf100() {
        var d = emptyDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Java").weight(new BigDecimal("30")).build())));
        expectPass(d);
    }

    @Test void val019_draftAllowsMinimumPositiveWeight() {
        var d = emptyDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Java").weight(new BigDecimal("0.01")).build())));
        expectPass(d);
    }

    @Test void val019_submitRejectsAbove100WhileDraftAccepts999_99() {
        var submit = valid();
        submit.getScreeningCriteria().get(0).setWeight(new BigDecimal("101"));
        assertThrows(RequisitionValidationException.class, () -> validator.validate(submit));

        var draft = emptyDraft();
        draft.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder().criteriaName("Java").weight(new BigDecimal("999.99")).build())));
        assertDoesNotThrow(() -> validator.validate(draft));
    }

    // VAL-020 — Mandatory và nhận diện dòng trắng

    @Test void val020_isMandatoryTruePreventsBlanknessSkip() {
        var d = emptyDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder()
                        .criteriaName(null).criteriaType(null).requiredValue(null)
                        .isMandatory(true).build())));
        // Not blank (isMandatory=true), but draft → no submit-required rules → passes
        expectPass(d);
    }

    @Test void val020_isMandatoryFalseAllBlankSkippedInDraft() {
        var d = emptyDraft();
        d.setScreeningCriteria(new ArrayList<>(List.of(
                ScreeningCriteriaRequest.builder()
                        .criteriaName("  ").criteriaType(null).requiredValue("  ")
                        .isMandatory(false).build())));
        expectPass(d);
    }

    @Test void val020_submitWithMandatoryRowRequiresAllFields() {
        var d = valid();
        d.getScreeningCriteria().get(0).setIsMandatory(true);
        d.getScreeningCriteria().get(0).setRequiredValue(null);
        assertTrue(expectFail(d).getErrors().containsKey("screeningCriteria[0].requiredValue"));
    }

    // Multi-error and utility tests ----------------------------------------

    @Test void multipleErrorsReportedSimultaneously() {
        var d = valid();
        d.setTitle("  ");
        d.setEmploymentType("Bad");
        d.setWorkModel("Bad");
        d.setExpectedStartDate(LocalDate.now().minusDays(1));
        d.setNumberOfPositions(null);
        var keys = expectFail(d).getErrors().keySet();
        assertTrue(keys.contains("title"));
        assertTrue(keys.contains("employmentType"));
        assertTrue(keys.contains("workModel"));
        assertTrue(keys.contains("expectedStartDate"));
        assertTrue(keys.contains("numberOfPositions"));
    }

    @Test void cleanReturnsNullForNull() { assertNull(RequisitionValidator.clean(null)); }
    @Test void cleanReturnsNullForBlank() { assertNull(RequisitionValidator.clean("   ")); }
    @Test void cleanTrimsSurroundingWhitespace() { assertEquals("hello", RequisitionValidator.clean("  hello  ")); }
    @Test void cleanPreservesInternalWhitespace() { assertEquals("hello world", RequisitionValidator.clean("  hello world  ")); }

    @Test void blankTrueWhenAllNullAndNotMandatory() {
        assertTrue(RequisitionValidator.blank(ScreeningCriteriaRequest.builder()
                .criteriaName(null).criteriaType(null).requiredValue(null).isMandatory(false).build()));
    }

    @Test void blankFalseWhenIsMandatoryTrue() {
        assertFalse(RequisitionValidator.blank(ScreeningCriteriaRequest.builder()
                .criteriaName(null).criteriaType(null).requiredValue(null).isMandatory(true).build()));
    }

    @Test void blankFalseWhenCriteriaNamePresent() {
        assertFalse(RequisitionValidator.blank(ScreeningCriteriaRequest.builder()
                .criteriaName("Java").criteriaType(null).requiredValue(null).isMandatory(false).build()));
    }

    @CsvSource({
        "Full-time, On-site, Any",
        "Part-time, Remote, Male",
        "Internship, Hybrid, Female",
        "Contract, On-site,"
    })
    @ParameterizedTest
    void validCombinationsOfChoiceFieldsPass(String type, String model, String gender) {
        var d = valid();
        d.setEmploymentType(type);
        d.setWorkModel(model);
        d.setGender(gender);
        expectPass(d);
    }
}
