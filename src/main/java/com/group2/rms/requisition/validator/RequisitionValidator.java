package com.group2.rms.requisition.validator;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.exception.RequisitionValidationException;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Validator nghiệp vụ cho Yêu cầu tuyển dụng (Job Requisition).
 * Tuân thủ quy chuẩn: không dùng tên biến vô nghĩa, có comment giải thích business rule.
 */
@Component
public class RequisitionValidator {

    public static final int REASON_CHAR_LIMIT = 2000;
    public static final int REASON_LIMIT = REASON_CHAR_LIMIT;
    public static final int TITLE_CHAR_LIMIT = 200;
    public static final int TEXT_FIELD_CHAR_LIMIT = 2000;
    public static final int SHORT_FIELD_CHAR_LIMIT = 255;
    public static final int MAX_CRITERIA_LIMIT = 50;
    public static final BigDecimal REQUIRED_TOTAL_WEIGHT = new BigDecimal("100");

    public static final List<String> EMPLOYMENT_TYPES = List.of("Full-time", "Part-time", "Internship", "Contract");
    public static final List<String> CRITERIA_TYPES = List.of("Education", "Experience", "Skill", "Knockout");
    public static final List<String> GENDERS = List.of("Any", "Male", "Female");
    public static final List<String> WORK_MODELS = List.of("On-site", "Remote", "Hybrid");

    public void validate(RequisitionRequest requisitionRequest) {
        Map<String, String> errors = new LinkedHashMap<>();
        boolean isSubmitAction = "submit".equals(requisitionRequest.getAction());

        // Rule: Hành động bắt buộc phải là 'draft' (Lưu nháp) hoặc 'submit' (Gửi duyệt)
        if (!Set.of("draft", "submit").contains(Objects.toString(requisitionRequest.getAction(), ""))) {
            errors.put("action", "Vui lòng chọn Lưu bản nháp hoặc Gửi Giám đốc duyệt.");
        }

        // Làm sạch dữ liệu chuỗi đầu vào
        requisitionRequest.setTitle(cleanString(requisitionRequest.getTitle()));
        requisitionRequest.setEmploymentType(cleanString(requisitionRequest.getEmploymentType()));
        requisitionRequest.setReasonForHiring(cleanString(requisitionRequest.getReasonForHiring()));
        requisitionRequest.setJobDescription(cleanString(requisitionRequest.getJobDescription()));
        requisitionRequest.setRequirementDetails(cleanString(requisitionRequest.getRequirementDetails()));
        requisitionRequest.setWorkLocation(cleanString(requisitionRequest.getWorkLocation()));
        requisitionRequest.setWorkModel(cleanString(requisitionRequest.getWorkModel()));
        requisitionRequest.setProbationDuration(cleanString(requisitionRequest.getProbationDuration()));
        requisitionRequest.setGender(cleanString(requisitionRequest.getGender()));

        // Rule: Kiểm tra độ dài và trường bắt buộc cho các thông tin văn bản
        validateTextField(errors, "title", requisitionRequest.getTitle(), TITLE_CHAR_LIMIT, isSubmitAction);
        validateTextField(errors, "reasonForHiring", requisitionRequest.getReasonForHiring(), REASON_CHAR_LIMIT, isSubmitAction);

        // Rule: Không cho phép gửi duyệt với tiêu đề placeholder mặc định
        if (isSubmitAction && "Untitled requisition".equalsIgnoreCase(requisitionRequest.getTitle())) {
            errors.put("title", "Vui lòng nhập chức danh công việc cụ thể trước khi gửi duyệt.");
        }

        validateTextField(errors, "jobDescription", requisitionRequest.getJobDescription(), TEXT_FIELD_CHAR_LIMIT, isSubmitAction);
        validateTextField(errors, "requirementDetails", requisitionRequest.getRequirementDetails(), TEXT_FIELD_CHAR_LIMIT, isSubmitAction);
        validateTextField(errors, "workLocation", requisitionRequest.getWorkLocation(), SHORT_FIELD_CHAR_LIMIT, isSubmitAction);
        validateTextField(errors, "probationDuration", requisitionRequest.getProbationDuration(), SHORT_FIELD_CHAR_LIMIT, false);

        // Rule: Các trường chọn phải nằm trong danh mục hợp lệ
        validateChoiceField(errors, "employmentType", requisitionRequest.getEmploymentType(), EMPLOYMENT_TYPES, isSubmitAction);
        validateChoiceField(errors, "workModel", requisitionRequest.getWorkModel(), WORK_MODELS, isSubmitAction);
        validateChoiceField(errors, "gender", requisitionRequest.getGender(), GENDERS, false);

        // Rule: Khi nộp duyệt bắt buộc phải chọn phòng ban
        if (isSubmitAction && requisitionRequest.getDepartmentId() == null) {
            errors.put("departmentId", "Vui lòng chọn phòng ban.");
        }

        // Rule: Số lượng tuyển dụng bắt buộc và phải là số nguyên lớn hơn 0
        if (isSubmitAction && requisitionRequest.getNumberOfPositions() == null) {
            errors.put("numberOfPositions", "Vui lòng nhập số lượng cần tuyển.");
        }
        if (requisitionRequest.getNumberOfPositions() != null && requisitionRequest.getNumberOfPositions() < 1) {
            errors.put("numberOfPositions", "Số lượng cần tuyển phải là số nguyên lớn hơn 0.");
        }

        // Rule: Ngày dự kiến bắt đầu làm việc phải từ ngày hôm nay trở đi
        if (isSubmitAction && (requisitionRequest.getExpectedStartDate() == null || requisitionRequest.getExpectedStartDate().isBefore(LocalDate.now()))) {
            errors.put("expectedStartDate", "Vui lòng chọn ngày hôm nay hoặc ngày trong tương lai.");
        }

        // Rule: Mức lương phải là số không âm và lương tối đa >= lương tối thiểu
        validateMoneyField(errors, "minSalary", requisitionRequest.getMinSalary());
        validateMoneyField(errors, "maxSalary", requisitionRequest.getMaxSalary());
        if (requisitionRequest.getMinSalary() != null && requisitionRequest.getMaxSalary() != null
                && requisitionRequest.getMinSalary().compareTo(requisitionRequest.getMaxSalary()) > 0) {
            errors.put("maxSalary", "Mức lương tối đa phải lớn hơn hoặc bằng mức lương tối thiểu.");
        }

        // Rule: Kiểm tra danh sách tiêu chí sàng lọc AI
        if (requisitionRequest.getScreeningCriteria() == null) {
            requisitionRequest.setScreeningCriteria(new ArrayList<>());
        }
        if (requisitionRequest.getScreeningCriteria().size() > MAX_CRITERIA_LIMIT) {
            errors.put("screeningCriteria", "Chỉ được thêm tối đa " + MAX_CRITERIA_LIMIT + " tiêu chí.");
        }

        Set<String> criteriaNames = new HashSet<>();
        Set<Integer> criteriaIds = new HashSet<>();
        BigDecimal totalWeight = BigDecimal.ZERO;
        int validCriteriaCount = 0;

        for (int index = 0; index < requisitionRequest.getScreeningCriteria().size(); index++) {
            ScreeningCriteriaRequest criteriaRow = requisitionRequest.getScreeningCriteria().get(index);
            String fieldPrefix = "screeningCriteria[" + index + "].";

            if (criteriaRow == null) {
                errors.put("screeningCriteria", "Tiêu chí không hợp lệ.");
                continue;
            }

            criteriaRow.setCriteriaName(cleanString(criteriaRow.getCriteriaName()));
            criteriaRow.setCriteriaType(cleanString(criteriaRow.getCriteriaType()));
            criteriaRow.setRequiredValue(cleanString(criteriaRow.getRequiredValue()));

            // Nếu là bản nháp và dòng tiêu chí trống thì bỏ qua
            if (isBlankCriteria(criteriaRow) && !isSubmitAction) {
                continue;
            }

            validCriteriaCount++;
            validateTextField(errors, fieldPrefix + "criteriaName", criteriaRow.getCriteriaName(), 150, isSubmitAction);
            validateTextField(errors, fieldPrefix + "requiredValue", criteriaRow.getRequiredValue(), SHORT_FIELD_CHAR_LIMIT, isSubmitAction);
            validateChoiceField(errors, fieldPrefix + "criteriaType", criteriaRow.getCriteriaType(), CRITERIA_TYPES, isSubmitAction);

            // Rule: Tên các tiêu chí sàng lọc không được trùng lặp
            if (criteriaRow.getCriteriaName() != null && !criteriaNames.add(criteriaRow.getCriteriaName().toLowerCase(Locale.ROOT))) {
                errors.put(fieldPrefix + "criteriaName", "Tên các tiêu chí không được trùng nhau.");
            }
            if (criteriaRow.getCriteriaId() != null && !criteriaIds.add(criteriaRow.getCriteriaId())) {
                errors.put(fieldPrefix + "criteriaName", "Tiêu chí đã được gửi hai lần.");
            }

            // Rule: Kiểm tra trọng số của từng tiêu chí
            BigDecimal weight = criteriaRow.getWeight();
            if (isSubmitAction && weight == null) {
                errors.put(fieldPrefix + "weight", "Vui lòng nhập trọng số.");
            }
            if (weight != null) {
                BigDecimal maxWeight = isSubmitAction ? new BigDecimal("100") : new BigDecimal("999.99");
                if (weight.signum() <= 0 || weight.compareTo(maxWeight) > 0 || weight.stripTrailingZeros().scale() > 2) {
                    errors.put(fieldPrefix + "weight", isSubmitAction
                            ? "Trọng số phải từ 0.01 đến 100 với tối đa hai chữ số thập phân."
                            : "Trọng số phải là số dương với tối đa hai chữ số thập phân.");
                }
                totalWeight = totalWeight.add(weight);
            }
        }

        // Rule: Khi nộp duyệt, phải có ít nhất 1 tiêu chí và tổng trọng số đúng bằng 100%
        if (isSubmitAction && (validCriteriaCount == 0 || totalWeight.compareTo(REQUIRED_TOTAL_WEIGHT) != 0)) {
            errors.put("screeningCriteria", "Vui lòng thêm đầy đủ tiêu chí với tổng trọng số đúng bằng 100%.");
        }

        if (!errors.isEmpty()) {
            throw new RequisitionValidationException(errors);
        }
    }

    public static String cleanString(String textValue) {
        return (textValue == null || textValue.isBlank()) ? null : textValue.trim();
    }

    public static boolean isBlankCriteria(ScreeningCriteriaRequest criteria) {
        return cleanString(criteria.getCriteriaName()) == null
                && cleanString(criteria.getCriteriaType()) == null
                && cleanString(criteria.getRequiredValue()) == null
                && !Boolean.TRUE.equals(criteria.getIsMandatory());
    }

    /**
     * Tương thích ngược với phương thức cũ blank() nếu có service gọi tới
     */
    public static boolean blank(ScreeningCriteriaRequest criteria) {
        return isBlankCriteria(criteria);
    }

    public static String clean(String textValue) {
        return cleanString(textValue);
    }

    private void validateTextField(Map<String, String> errorsMap, String fieldKey, String textValue, int maxLength, boolean isRequired) {
        if (isRequired && textValue == null) {
            errorsMap.put(fieldKey, "Trường này bắt buộc nhập trước khi gửi phê duyệt.");
        } else if (textValue != null && textValue.length() > maxLength) {
            errorsMap.put(fieldKey, "Tối đa " + maxLength + " ký tự.");
        }
    }

    private void validateChoiceField(Map<String, String> errorsMap, String fieldKey, String selectedValue, List<String> allowedChoices, boolean isRequired) {
        if ((isRequired || selectedValue != null) && (selectedValue == null || !allowedChoices.contains(selectedValue))) {
            errorsMap.put(fieldKey, "Vui lòng chọn một tùy chọn hợp lệ.");
        }
    }

    private void validateMoneyField(Map<String, String> errorsMap, String fieldKey, BigDecimal amount) {
        BigDecimal maxAllowedAmount = new BigDecimal("9999999999999999.99");
        if (amount != null && (amount.signum() < 0 || amount.compareTo(maxAllowedAmount) > 0 || amount.stripTrailingZeros().scale() > 2)) {
            errorsMap.put(fieldKey, "Vui lòng nhập số tiền không âm với tối đa 2 chữ số thập phân.");
        }
    }
}
