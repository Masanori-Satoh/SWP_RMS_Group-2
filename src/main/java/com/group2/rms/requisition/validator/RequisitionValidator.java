package com.group2.rms.requisition.validator;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.exception.RequisitionValidationException;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validator cho Job Requisition.
 * Rule: Bản nháp (draft) bỏ qua các quy tắc bắt buộc; chỉ kiểm tra giới hạn độ
 * dài và định dạng cơ bản.
 * Khi gửi duyệt (submit), tất cả các trường bắt buộc và tổng trọng số tiêu chí
 * (100%) phải thỏa mãn.
 * Mọi thông báo lỗi hiển thị frontend bằng tiếng Việt theo quy chuẩn dự án.
 */
@Component
public class RequisitionValidator {

    public static final int REASON_LIMIT = 2000;
    public static final int TITLE_LIMIT = 200;
    public static final int TEXT_AREA_LIMIT = 2000;
    public static final int LOCATION_LIMIT = 255;
    public static final int PROBATION_LIMIT = 255;
    public static final int CRITERIA_NAME_LIMIT = 150;
    public static final int CRITERIA_VALUE_LIMIT = 255;
    public static final int MAX_CRITERIA_COUNT = 50;
    public static final int REASON_CHAR_LIMIT = 255;

    public static final String ACTION_DRAFT = "draft";
    public static final String ACTION_SUBMIT = "submit";

    public static final List<String> EMPLOYMENT_TYPES = List.of("Full-time", "Part-time", "Internship", "Contract");
    public static final List<String> CRITERIA_TYPES = List.of("Education", "Experience", "Skill", "Knockout");
    public static final List<String> GENDERS = List.of("Any", "Male", "Female");
    public static final List<String> WORK_MODELS = List.of("On-site", "Remote", "Hybrid");

    public void validate(RequisitionRequest requisitionRequest) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        boolean isSubmitAction = ACTION_SUBMIT.equals(requisitionRequest.getAction());

        // Rule: Thao tác phải là lưu bản nháp hoặc gửi duyệt
        if (!Set.of(ACTION_DRAFT, ACTION_SUBMIT).contains(Objects.toString(requisitionRequest.getAction(), ""))) {
            validationErrors.put("action", "Vui lòng chọn Lưu bản nháp hoặc Gửi Giám đốc duyệt.");
        }

        // Làm sạch dữ liệu đầu vào dạng văn bản
        requisitionRequest.setTitle(clean(requisitionRequest.getTitle()));
        requisitionRequest.setEmploymentType(clean(requisitionRequest.getEmploymentType()));
        requisitionRequest.setReasonForHiring(clean(requisitionRequest.getReasonForHiring()));
        requisitionRequest.setJobDescription(clean(requisitionRequest.getJobDescription()));
        requisitionRequest.setRequirementDetails(clean(requisitionRequest.getRequirementDetails()));
        requisitionRequest.setWorkLocation(clean(requisitionRequest.getWorkLocation()));
        requisitionRequest.setWorkModel(clean(requisitionRequest.getWorkModel()));
        requisitionRequest.setProbationDuration(clean(requisitionRequest.getProbationDuration()));
        requisitionRequest.setGender(clean(requisitionRequest.getGender()));

        // Rule: Kiểm tra các trường văn bản cơ bản
        validateTextField(validationErrors, "title", requisitionRequest.getTitle(), TITLE_LIMIT, isSubmitAction);
        validateTextField(validationErrors, "reasonForHiring", requisitionRequest.getReasonForHiring(), REASON_LIMIT,
                isSubmitAction);

        if (isSubmitAction && "Untitled requisition".equalsIgnoreCase(requisitionRequest.getTitle())) {
            validationErrors.put("title", "Vui lòng nhập vị trí tuyển dụng cụ thể trước khi gửi duyệt.");
        }

        validateTextField(validationErrors, "jobDescription", requisitionRequest.getJobDescription(), TEXT_AREA_LIMIT,
                isSubmitAction);
        validateTextField(validationErrors, "requirementDetails", requisitionRequest.getRequirementDetails(),
                TEXT_AREA_LIMIT, isSubmitAction);
        validateTextField(validationErrors, "workLocation", requisitionRequest.getWorkLocation(), LOCATION_LIMIT,
                isSubmitAction);

        // Rule: Thời gian thử việc (số ngày) - bắt buộc khi gửi duyệt, phải là số
        // nguyên > 0. Lưu nháp cho phép để trống.
        if (isSubmitAction) {
            if (requisitionRequest.getProbationDuration() == null) {
                validationErrors.put("probationDuration", "Vui lòng nhập thời gian thử việc.");
            } else {
                validateProbationDays(validationErrors, requisitionRequest.getProbationDuration());
            }
        }

        // Rule: Kiểm tra giá trị lựa chọn (combobox/select)
        validateChoiceField(validationErrors, "employmentType", requisitionRequest.getEmploymentType(),
                EMPLOYMENT_TYPES, isSubmitAction);
        validateChoiceField(validationErrors, "workModel", requisitionRequest.getWorkModel(), WORK_MODELS,
                isSubmitAction);
        validateChoiceField(validationErrors, "gender", requisitionRequest.getGender(), GENDERS, false);

        // Rule: Phòng ban bắt buộc khi gửi duyệt
        if (isSubmitAction && requisitionRequest.getDepartmentId() == null) {
            validationErrors.put("departmentId", "Vui lòng chọn phòng ban.");
        }

        // Rule: Số lượng tuyển dụng bắt buộc khi gửi duyệt và phải là số nguyên dương
        // (> 0)
        if (isSubmitAction && requisitionRequest.getNumberOfPositions() == null) {
            validationErrors.put("numberOfPositions", "Vui lòng nhập số lượng tuyển dụng.");
        }
        if (requisitionRequest.getNumberOfPositions() != null && requisitionRequest.getNumberOfPositions() < 1) {
            validationErrors.put("numberOfPositions", "Số lượng tuyển dụng phải là số nguyên lớn hơn 0.");
        }

        // Rule: Ngày bắt đầu dự kiến không được trong quá khứ khi gửi duyệt
        if (isSubmitAction && (requisitionRequest.getExpectedStartDate() == null
                || requisitionRequest.getExpectedStartDate().isBefore(LocalDate.now()))) {
            validationErrors.put("expectedStartDate", "Ngày bắt đầu dự kiến phải là hôm nay hoặc trong tương lai.");
        }

        // Rule: Kiểm tra khoảng lương
        validateMonetaryAmount(validationErrors, "minSalary", requisitionRequest.getMinSalary());
        validateMonetaryAmount(validationErrors, "maxSalary", requisitionRequest.getMaxSalary());
        if (requisitionRequest.getMinSalary() != null && requisitionRequest.getMaxSalary() != null
                && requisitionRequest.getMinSalary().compareTo(requisitionRequest.getMaxSalary()) > 0) {
            validationErrors.put("maxSalary", "Mức lương tối đa phải lớn hơn hoặc bằng mức lương tối thiểu.");
        }

        // Rule: Tiêu chí sàng lọc (screening criteria)
        if (requisitionRequest.getScreeningCriteria() == null) {
            requisitionRequest.setScreeningCriteria(new ArrayList<>());
        }
        if (requisitionRequest.getScreeningCriteria().size() > MAX_CRITERIA_COUNT) {
            validationErrors.put("screeningCriteria", "Tối đa 50 tiêu chí sàng lọc.");
        }

        Set<String> uniqueCriteriaNames = new HashSet<>();
        Set<Integer> uniqueCriteriaIds = new HashSet<>();
        BigDecimal totalCriteriaWeight = BigDecimal.ZERO;
        int validCriteriaCount = 0;

        for (int criteriaIndex = 0; criteriaIndex < requisitionRequest.getScreeningCriteria().size(); criteriaIndex++) {
            ScreeningCriteriaRequest criteriaItem = requisitionRequest.getScreeningCriteria().get(criteriaIndex);
            String fieldPrefixKey = "screeningCriteria[" + criteriaIndex + "].";

            if (criteriaItem == null) {
                validationErrors.put("screeningCriteria", "Tiêu chí sàng lọc không hợp lệ.");
                continue;
            }

            criteriaItem.setCriteriaName(clean(criteriaItem.getCriteriaName()));
            criteriaItem.setCriteriaType(clean(criteriaItem.getCriteriaType()));
            criteriaItem.setRequiredValue(clean(criteriaItem.getRequiredValue()));

            if (blank(criteriaItem) && !isSubmitAction) {
                continue;
            }

            validCriteriaCount++;
            validateTextField(validationErrors, fieldPrefixKey + "criteriaName", criteriaItem.getCriteriaName(),
                    CRITERIA_NAME_LIMIT, isSubmitAction);
            validateTextField(validationErrors, fieldPrefixKey + "requiredValue", criteriaItem.getRequiredValue(),
                    CRITERIA_VALUE_LIMIT, isSubmitAction);
            validateChoiceField(validationErrors, fieldPrefixKey + "criteriaType", criteriaItem.getCriteriaType(),
                    CRITERIA_TYPES, isSubmitAction);

            // Rule: Tên các tiêu chí sàng lọc không được trùng lặp
            if (criteriaItem.getCriteriaName() != null
                    && !uniqueCriteriaNames.add(criteriaItem.getCriteriaName().toLowerCase(Locale.ROOT))) {
                validationErrors.put(fieldPrefixKey + "criteriaName",
                        "Tên các tiêu chí sàng lọc không được trùng lặp.");
            }
            if (criteriaItem.getCriteriaId() != null && !uniqueCriteriaIds.add(criteriaItem.getCriteriaId())) {
                validationErrors.put(fieldPrefixKey + "criteriaName", "Tiêu chí sàng lọc bị gửi trùng lặp.");
            }

            // Rule: Trọng số tiêu chí
            BigDecimal weightValue = criteriaItem.getWeight();
            if (isSubmitAction && weightValue == null) {
                validationErrors.put(fieldPrefixKey + "weight", "Vui lòng nhập trọng số.");
            }
            if (weightValue != null) {
                BigDecimal maximumAllowedWeight = new BigDecimal(isSubmitAction ? "100" : "999.99");
                if (weightValue.signum() <= 0 || weightValue.compareTo(maximumAllowedWeight) > 0
                        || weightValue.stripTrailingZeros().scale() > 2) {
                    validationErrors.put(fieldPrefixKey + "weight", isSubmitAction
                            ? "Trọng số phải từ 0.01 đến 100 và tối đa 2 chữ số thập phân."
                            : "Trọng số phải là số dương và tối đa 2 chữ số thập phân.");
                }
                totalCriteriaWeight = totalCriteriaWeight.add(weightValue);
            }
        }

        // Rule: Khi gửi duyệt, phải có ít nhất 1 tiêu chí và tổng trọng số phải đúng
        // 100%
        if (isSubmitAction && (validCriteriaCount == 0 || totalCriteriaWeight.compareTo(new BigDecimal("100")) != 0)) {
            validationErrors.put("screeningCriteria", "Vui lòng nhập đầy đủ tiêu chí với tổng trọng số đúng 100%.");
        }

        if (!validationErrors.isEmpty()) {
            throw new RequisitionValidationException(validationErrors);
        }
    }

    public static String clean(String textValue) {
        return (textValue == null || textValue.isBlank()) ? null : textValue.trim();
    }

    public static boolean blank(ScreeningCriteriaRequest criteriaRequest) {
        return clean(criteriaRequest.getCriteriaName()) == null
                && clean(criteriaRequest.getCriteriaType()) == null
                && clean(criteriaRequest.getRequiredValue()) == null
                && !Boolean.TRUE.equals(criteriaRequest.getIsMandatory());
    }

    private void validateTextField(Map<String, String> errors, String fieldKey, String fieldValue, int maximumLength,
            boolean isRequired) {
        if (isRequired && fieldValue == null) {
            errors.put(fieldKey, "Bắt buộc nhập trước khi gửi Giám đốc phê duyệt.");
        } else if (fieldValue != null && fieldValue.length() > maximumLength) {
            errors.put(fieldKey, "Tối đa " + maximumLength + " ký tự.");
        }
    }

    private void validateChoiceField(Map<String, String> errors, String fieldKey, String fieldValue,
            List<String> allowedChoices, boolean isRequired) {
        if ((isRequired || fieldValue != null) && (fieldValue == null || !allowedChoices.contains(fieldValue))) {
            errors.put(fieldKey, "Vui lòng chọn giá trị hợp lệ.");
        }
    }

    private void validateMonetaryAmount(Map<String, String> errors, String fieldKey, BigDecimal monetaryAmount) {
        if (monetaryAmount != null && (monetaryAmount.signum() < 0
                || monetaryAmount.compareTo(new BigDecimal("9999999999999999.99")) > 0
                || monetaryAmount.stripTrailingZeros().scale() > 2)) {
            errors.put(fieldKey, "Số tiền phải không âm và tối đa 2 chữ số thập phân.");
        }
    }

    private void validateProbationDays(Map<String, String> errors, String value) {
        if (value == null)
            return;
        try {
            long days = Long.parseLong(value);
            if (days <= 0 || days > 181) {
                errors.put("probationDuration", "Thời gian thử việc phải là số ngày nguyên trong khoảng 1-180.");
            }
        } catch (NumberFormatException e) {
            errors.put("probationDuration", "Thời gian thử việc phải là số ngày nguyên trong khoảng 1-180.");
        }
    }
}
