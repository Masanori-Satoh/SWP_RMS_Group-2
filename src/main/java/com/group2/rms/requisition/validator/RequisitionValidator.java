package com.group2.rms.requisition.validator;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.exception.RequisitionValidationException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validator nghiệp vụ cho Yêu cầu tuyển dụng (Job Requisition).
 */
@Component
@RequiredArgsConstructor
public class RequisitionValidator {

    /** Hai nút trên form: "Lưu bản nháp" gửi action=draft, "Gửi duyệt" gửi action=submit. */
    public static final String ACTION_DRAFT = "draft";
    public static final String ACTION_SUBMIT = "submit";

    public static final int REASON_CHAR_LIMIT = 2000;
    public static final int TITLE_CHAR_LIMIT = 200;
    public static final int TEXT_FIELD_CHAR_LIMIT = 2000;
    public static final int SHORT_FIELD_CHAR_LIMIT = 255;
    public static final int COMMENT_CHAR_LIMIT = 1000;
    public static final String PLACEHOLDER_TITLE = "Untitled requisition";
    public static final int PROBATION_MIN_DAYS = 1;
    public static final int PROBATION_MAX_DAYS = 180;
    public static final int POSITIONS_MIN = 1;
    public static final int POSITIONS_MAX = 30;
    public static final String SALARY_MAX_TEXT = "1000000000";
    public static final BigDecimal SALARY_MAX = new BigDecimal(SALARY_MAX_TEXT);

    // Câu lỗi dùng chung cho annotation trên DTO và validate()
    public static final String TITLE_TOO_LONG = "Nội dung quá " + TITLE_CHAR_LIMIT + " ký tự";
    public static final String SHORT_FIELD_TOO_LONG = "Nội dung quá " + SHORT_FIELD_CHAR_LIMIT + " ký tự";
    public static final String PROBATION_RANGE_MESSAGE = "Thời gian thử việc phải từ " + PROBATION_MIN_DAYS + " đến "
            + PROBATION_MAX_DAYS + " ngày.";
    public static final String POSITIONS_RANGE_MESSAGE = "Số lượng cần tuyển phải từ " + POSITIONS_MIN + " đến "
            + POSITIONS_MAX + " người.";
    public static final String SALARY_FORMAT_MESSAGE = "Mức lương phải lớn hơn 0 với tối đa 2 chữ số thập phân.";
    public static final String SALARY_MAX_MESSAGE = "Mức lương không được vượt quá 1.000.000.000 VND/tháng.";

    public static final List<String> EMPLOYMENT_TYPES = List.of("Full-time", "Part-time", "Internship", "Contract");
    public static final List<String> GENDERS = List.of("Any", "Male", "Female");
    public static final List<String> WORK_MODELS = List.of("On-site", "Remote", "Hybrid");
    public static final String GENDER_ANY = "Any";
    public static final List<String> HIRING_TYPES = List.of("New", "Replacement");

    /** Đồng hồ dùng chung (ClockConfig); test truyền Clock.fixed để cố định "hôm nay". */
    private final Clock clock;

    /** Người dùng bấm "Gửi duyệt" (không phải "Lưu bản nháp"). */
    public static boolean isSubmit(String action) {
        return ACTION_SUBMIT.equals(action);
    }

    public void validate(RequisitionRequest requisitionRequest) {
        Map<String, String> errors = new LinkedHashMap<>();
        String action = requisitionRequest.getAction();
        boolean isSubmitAction = isSubmit(action);

        // Rule: Hành động bắt buộc phải là 'draft' (Lưu nháp) hoặc 'submit' (Gửi duyệt)
        if (!ACTION_DRAFT.equals(action) && !ACTION_SUBMIT.equals(action)) {
            errors.put("action", "Vui lòng chọn Lưu bản nháp hoặc Gửi Giám đốc duyệt.");
        }

        // Làm sạch dữ liệu chuỗi đầu vào
        requisitionRequest.setTitle(cleanString(requisitionRequest.getTitle()));
        requisitionRequest.setEmploymentType(cleanString(requisitionRequest.getEmploymentType()));
        requisitionRequest.setReasonForHiring(cleanMultiline(requisitionRequest.getReasonForHiring()));
        requisitionRequest.setJobDescription(cleanMultiline(requisitionRequest.getJobDescription()));
        requisitionRequest.setRequirementDetails(cleanMultiline(requisitionRequest.getRequirementDetails()));
        requisitionRequest.setWorkLocation(cleanString(requisitionRequest.getWorkLocation()));
        requisitionRequest.setWorkModel(cleanString(requisitionRequest.getWorkModel()));
        requisitionRequest.setGender(cleanString(requisitionRequest.getGender()));
        requisitionRequest.setHiringType(cleanString(requisitionRequest.getHiringType()));

        if (requisitionRequest.getGender() == null) {
            requisitionRequest.setGender(GENDER_ANY);
        }

        // Rule: tên vị trí bắt buộc cả khi lưu nháp
        validateTextField(errors, "title", requisitionRequest.getTitle(), TITLE_CHAR_LIMIT, true);
        validateTextField(errors, "reasonForHiring", requisitionRequest.getReasonForHiring(), REASON_CHAR_LIMIT,
                isSubmitAction);

        // Rule: Không cho phép gửi duyệt với tiêu đề placeholder mặc định
        if (isSubmitAction && PLACEHOLDER_TITLE.equalsIgnoreCase(requisitionRequest.getTitle())) {
            errors.put("title", "Vui lòng nhập chức danh công việc cụ thể trước khi gửi duyệt.");
        }

        validateTextField(errors, "jobDescription", requisitionRequest.getJobDescription(), TEXT_FIELD_CHAR_LIMIT,
                isSubmitAction);
        validateTextField(errors, "requirementDetails", requisitionRequest.getRequirementDetails(),
                TEXT_FIELD_CHAR_LIMIT, isSubmitAction);
        validateTextField(errors, "workLocation", requisitionRequest.getWorkLocation(), SHORT_FIELD_CHAR_LIMIT,
                isSubmitAction);

        // Rule: thử việc không bắt buộc; nếu nhập thì từ PROBATION_MIN_DAYS đến PROBATION_MAX_DAYS ngày
        Integer probationDays = requisitionRequest.getProbationDays();
        if (probationDays != null && (probationDays < PROBATION_MIN_DAYS || probationDays > PROBATION_MAX_DAYS)) {
            errors.put("probationDays", PROBATION_RANGE_MESSAGE);
        }

        // Rule: Các trường chọn phải nằm trong danh mục hợp lệ.
        validateChoiceField(errors, "employmentType", requisitionRequest.getEmploymentType(), EMPLOYMENT_TYPES, true);
        validateChoiceField(errors, "workModel", requisitionRequest.getWorkModel(), WORK_MODELS, true);
        validateChoiceField(errors, "gender", requisitionRequest.getGender(), GENDERS, true);
        validateChoiceField(errors, "hiringType", requisitionRequest.getHiringType(), HIRING_TYPES, false);

        // Rule: Số lượng tuyển dụng bắt buộc
        Integer numberOfPositions = requisitionRequest.getNumberOfPositions();
        if (numberOfPositions == null) {
            errors.put("numberOfPositions", "Vui lòng nhập số lượng cần tuyển.");
        } else if (numberOfPositions < POSITIONS_MIN || numberOfPositions > POSITIONS_MAX) {
            errors.put("numberOfPositions", POSITIONS_RANGE_MESSAGE);
        }

        // Rule: Mức lương phải lớn hơn 0 và lương tối đa >= lương tối thiểu
        validateMoneyField(errors, "minSalary", requisitionRequest.getMinSalary());
        validateMoneyField(errors, "maxSalary", requisitionRequest.getMaxSalary());
        if (requisitionRequest.getMinSalary() != null && requisitionRequest.getMaxSalary() != null
                && requisitionRequest.getMinSalary().compareTo(requisitionRequest.getMaxSalary()) > 0) {
            errors.put("maxSalary", "Mức lương tối đa phải lớn hơn hoặc bằng mức lương tối thiểu.");
        }

        if (!errors.isEmpty()) {
            throw new RequisitionValidationException(errors);
        }
    }

    public static String cleanString(String textValue) {
        return (textValue == null || textValue.isBlank()) ? null : textValue.trim();
    }

    public static String cleanMultiline(String textValue) {
        String cleaned = cleanString(textValue);
        return cleaned == null ? null : cleaned.replace("\r\n", "\n");
    }

    private void validateTextField(Map<String, String> errorsMap, String fieldKey, String textValue, int maxLength,
            boolean isRequired) {
        if (isRequired && textValue == null) {
            if ("title".equals(fieldKey)) {
                errorsMap.put(fieldKey, "Tên vị trí không được để trống.");
            } else if ("workLocation".equals(fieldKey)) {
                errorsMap.put(fieldKey, "Địa điểm làm việc không được để trống.");
            } else {
                errorsMap.put(fieldKey, "Vui lòng nhập trường này.");
            }
        } else if (textValue != null && textValue.length() > maxLength) {
            errorsMap.put(fieldKey, "Nội dung quá " + maxLength + " ký tự");
        }
    }

    private void validateChoiceField(Map<String, String> errorsMap, String fieldKey, String selectedValue,
            List<String> allowedChoices, boolean isRequired) {
        if ((isRequired || selectedValue != null)
                && (selectedValue == null || !allowedChoices.contains(selectedValue))) {
            errorsMap.put(fieldKey, "Vui lòng chọn một tùy chọn hợp lệ.");
        }
    }

    /** Lương không bắt buộc; nếu nhập thì từ 1 đến SALARY_MAX VND/tháng, tối đa 2 chữ số thập phân. */
    private void validateMoneyField(Map<String, String> errorsMap, String fieldKey, BigDecimal amount) {
        if (amount == null) {
            return;
        }
        if (amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 2) {
            errorsMap.put(fieldKey, SALARY_FORMAT_MESSAGE);
        } else if (amount.compareTo(SALARY_MAX) > 0) {
            errorsMap.put(fieldKey, SALARY_MAX_MESSAGE);
        }
    }
}
