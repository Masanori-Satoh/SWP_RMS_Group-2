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

    /**
     * Hai nút trên form: "Lưu bản nháp" gửi action=draft, "Gửi duyệt" gửi
     * action=submit.
     */
    public static final String ACTION_DRAFT = "draft";
    public static final String ACTION_SUBMIT = "submit";

    public static final int REASON_CHAR_LIMIT = 2000;
    public static final int TITLE_CHAR_LIMIT = 200;
    public static final int TEXT_FIELD_CHAR_LIMIT = 2000;
    public static final int SHORT_FIELD_CHAR_LIMIT = 255;
    public static final int COMMENT_CHAR_LIMIT = 1000;
    public static final String PLACEHOLDER_TITLE = "Chưa nhập vị trí";
    public static final int PROBATION_CHAR_LIMIT = 100;
    public static final int POSITIONS_MIN = 1;
    public static final int ROUND_MIN = 1;
    public static final String SALARY_MAX_TEXT = "1000000000";
    public static final BigDecimal SALARY_MAX = new BigDecimal(SALARY_MAX_TEXT);

    // Câu lỗi dùng chung cho annotation trên DTO và validate()
    public static final String TITLE_TOO_LONG = "Nội dung quá " + TITLE_CHAR_LIMIT + " ký tự";
    public static final String SHORT_FIELD_TOO_LONG = "Nội dung quá " + SHORT_FIELD_CHAR_LIMIT + " ký tự";
    public static final String PROBATION_TOO_LONG = "Nội dung quá " + PROBATION_CHAR_LIMIT + " ký tự";
    public static final String ROUND_LABEL = "Đợt tuyển dụng";
    public static final String POSITIONS_LABEL = "Số lượng cần tuyển";
    public static final String ROUND_MIN_MESSAGE = ROUND_LABEL + " phải lớn hơn hoặc bằng " + ROUND_MIN + ".";
    public static final String POSITIONS_MIN_MESSAGE = POSITIONS_LABEL + " phải lớn hơn hoặc bằng " + POSITIONS_MIN + ".";
    public static final String POSITIONS_REQUIRED_MESSAGE = POSITIONS_LABEL + " là bắt buộc.";
    public static final String SALARY_FORMAT_MESSAGE = "Mức lương phải lớn hơn 0 với tối đa 2 chữ số thập phân.";
    public static final String SALARY_MAX_MESSAGE = "Mức lương không được vượt quá 1.000.000.000 VND/tháng.";

    public static final List<String> EMPLOYMENT_TYPES = List.of("Full-time", "Part-time", "Internship", "Contract");
    public static final List<String> GENDERS = List.of("Any", "Male", "Female");
    public static final List<String> WORK_MODELS = List.of("On-site", "Remote", "Hybrid");
    public static final String GENDER_ANY = "Any";

    /**
     * Đồng hồ dùng chung (ClockConfig); test truyền Clock.fixed để cố định "hôm
     * nay".
     */
    private final Clock clock;

    /** Người dùng bấm "Gửi duyệt" */
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
        if ("Remote".equals(requisitionRequest.getWorkModel())) {
            requisitionRequest.setWorkLocation(null);
        }
        requisitionRequest.setGender(cleanString(requisitionRequest.getGender()));
        requisitionRequest.setProbationPeriod(cleanString(requisitionRequest.getProbationPeriod()));
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
                isSubmitAction && !"Remote".equals(requisitionRequest.getWorkModel()));

        // Rule: thử việc không bắt buộc, chữ tự do (VD "2 tháng", "Thỏa thuận"), tối đa
        // PROBATION_CHAR_LIMIT ký tự
        validateTextField(errors, "probationPeriod", requisitionRequest.getProbationPeriod(), PROBATION_CHAR_LIMIT,
                false);

        // Rule: Các trường chọn phải nằm trong danh mục hợp lệ.
        validateChoiceField(errors, "employmentType", requisitionRequest.getEmploymentType(), EMPLOYMENT_TYPES, true);
        validateChoiceField(errors, "workModel", requisitionRequest.getWorkModel(), WORK_MODELS, true);
        validateChoiceField(errors, "gender", requisitionRequest.getGender(), GENDERS, true);
        // Rule: đợt tuyển dụng do HM nhập tay; để trống thì là đợt 1
        Integer round = requisitionRequest.getRecruitmentRound();
        if (round == null) {
            requisitionRequest.setRecruitmentRound(ROUND_MIN);
        } else if (round < ROUND_MIN) {
            errors.put("recruitmentRound", ROUND_MIN_MESSAGE);
        }

        // Rule: Số lượng tuyển dụng bắt buộc
        Integer numberOfPositions = requisitionRequest.getNumberOfPositions();
        if (numberOfPositions == null) {
            errors.put("numberOfPositions", POSITIONS_REQUIRED_MESSAGE);
        } else if (numberOfPositions < POSITIONS_MIN) {
            errors.put("numberOfPositions", POSITIONS_MIN_MESSAGE);
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

    /** Câu lỗi khi ô số nguyên nhận giá trị không parse được: số thập phân hay chữ/ký tự đặc biệt. */
    public static String integerFormatMessage(String label, Object rawValue) {
        String raw = rawValue == null ? "" : String.valueOf(rawValue).trim();
        if (raw.matches("[-+]?\\d+[.,]\\d+")) {
            return label + " phải là số nguyên.";
        }
        // Toàn chữ số mà vẫn không đọc được = vượt giới hạn kiểu INT
        return raw.matches("[-+]?\\d+") ? label + " quá lớn." : label + " chỉ được chứa số nguyên.";
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

    /**
     * Lương không bắt buộc; nếu nhập thì từ 1 đến SALARY_MAX VND/tháng, tối đa 2
     * chữ số thập phân.
     */
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
