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
    /** Thời gian thử việc tối thiểu (ngày) khi có nhập. */
    public static final int PROBATION_MIN_DAYS = 1;
    /** Thời gian thử việc tối đa (ngày), theo Điều 25 Bộ luật Lao động 2019. */
    public static final int PROBATION_MAX_DAYS = 180;
    /** Số lượng cần tuyển tối thiểu của một yêu cầu. */
    public static final int POSITIONS_MIN = 1;
    /** Số lượng cần tuyển tối đa của một yêu cầu: công ty khoảng 200 nhân sự, một đợt tuyển không quá 30 người. */
    public static final int POSITIONS_MAX = 30;
    /** Ngày bắt đầu dự kiến tối đa bao nhiêu năm kể từ hôm nay. */
    public static final int START_DATE_MAX_YEARS = 1;
    /** Mức lương tối đa (VND/tháng) được nhập, chặn gõ thừa số 0. */
    public static final BigDecimal SALARY_MAX = new BigDecimal("1000000000");
    public static final int MAX_CRITERIA_LIMIT = 50;
    public static final BigDecimal REQUIRED_TOTAL_WEIGHT = new BigDecimal("100");

    public static final List<String> EMPLOYMENT_TYPES = List.of("Full-time", "Part-time", "Internship", "Contract");
    public static final List<String> CRITERIA_TYPES = List.of("Education", "Experience", "Skill", "Knockout");
    public static final List<String> GENDERS = List.of("Any", "Male", "Female");
    public static final List<String> WORK_MODELS = List.of("On-site", "Remote", "Hybrid");
    /** Giá trị "Không yêu cầu giới tính", cũng là DEFAULT của cột RequiredGender. */
    public static final String GENDER_ANY = "Any";
    public static final List<String> HIRING_TYPES = List.of("New", "Replacement");

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
        requisitionRequest.setReasonForHiring(cleanMultiline(requisitionRequest.getReasonForHiring()));
        requisitionRequest.setJobDescription(cleanMultiline(requisitionRequest.getJobDescription()));
        requisitionRequest.setRequirementDetails(cleanMultiline(requisitionRequest.getRequirementDetails()));
        requisitionRequest.setWorkLocation(cleanString(requisitionRequest.getWorkLocation()));
        requisitionRequest.setWorkModel(cleanString(requisitionRequest.getWorkModel()));
        requisitionRequest.setGender(cleanString(requisitionRequest.getGender()));
        requisitionRequest.setHiringType(cleanString(requisitionRequest.getHiringType()));

        // Rule: Không chọn giới tính nghĩa là "Không yêu cầu" (giống DEFAULT N'Any' của cột RequiredGender)
        if (requisitionRequest.getGender() == null) {
            requisitionRequest.setGender(GENDER_ANY);
        }

        // Rule: Kiểm tra độ dài và trường bắt buộc cho các thông tin văn bản.
        // Tên vị trí bắt buộc cả khi lưu nháp: cột Title là NOT NULL và là thứ duy nhất để nhận ra bản nháp trong danh sách.
        validateTextField(errors, "title", requisitionRequest.getTitle(), TITLE_CHAR_LIMIT, true);
        validateTextField(errors, "reasonForHiring", requisitionRequest.getReasonForHiring(), REASON_CHAR_LIMIT, isSubmitAction);

        // Rule: Không cho phép gửi duyệt với tiêu đề placeholder mặc định
        if (isSubmitAction && "Untitled requisition".equalsIgnoreCase(requisitionRequest.getTitle())) {
            errors.put("title", "Vui lòng nhập chức danh công việc cụ thể trước khi gửi duyệt.");
        }

        validateTextField(errors, "jobDescription", requisitionRequest.getJobDescription(), TEXT_FIELD_CHAR_LIMIT, isSubmitAction);
        validateTextField(errors, "requirementDetails", requisitionRequest.getRequirementDetails(), TEXT_FIELD_CHAR_LIMIT, isSubmitAction);
        validateTextField(errors, "workLocation", requisitionRequest.getWorkLocation(), SHORT_FIELD_CHAR_LIMIT, isSubmitAction);

        // Rule: Thời gian thử việc không bắt buộc; nếu nhập thì là số ngày từ PROBATION_MIN_DAYS đến PROBATION_MAX_DAYS
        Integer probationDays = requisitionRequest.getProbationDays();
        if (probationDays != null && (probationDays < PROBATION_MIN_DAYS || probationDays > PROBATION_MAX_DAYS)) {
            errors.put("probationDays", "Thời gian thử việc phải từ " + PROBATION_MIN_DAYS + " đến " + PROBATION_MAX_DAYS + " ngày.");
        }

        // Rule: Các trường chọn phải nằm trong danh mục hợp lệ.
        // Hình thức, mô hình làm việc, giới tính bắt buộc cả khi lưu nháp vì các cột này là NOT NULL trong DB
        // (form đã chọn sẵn giá trị mặc định nên người dùng gần như không gặp lỗi này).
        validateChoiceField(errors, "employmentType", requisitionRequest.getEmploymentType(), EMPLOYMENT_TYPES, true);
        validateChoiceField(errors, "workModel", requisitionRequest.getWorkModel(), WORK_MODELS, true);
        validateChoiceField(errors, "gender", requisitionRequest.getGender(), GENDERS, true);
        validateChoiceField(errors, "hiringType", requisitionRequest.getHiringType(), HIRING_TYPES, false);

        // Rule: Khi nộp duyệt bắt buộc phải chọn phòng ban
        if (isSubmitAction && requisitionRequest.getDepartmentId() == null) {
            errors.put("departmentId", "Vui lòng chọn phòng ban.");
        }

        // Rule: Số lượng tuyển dụng bắt buộc (cả khi lưu nháp, cột NumberOfPositions là NOT NULL)
        // và nằm trong khoảng POSITIONS_MIN..POSITIONS_MAX người
        Integer numberOfPositions = requisitionRequest.getNumberOfPositions();
        if (numberOfPositions == null) {
            errors.put("numberOfPositions", "Vui lòng nhập số lượng cần tuyển.");
        } else if (numberOfPositions < POSITIONS_MIN || numberOfPositions > POSITIONS_MAX) {
            errors.put("numberOfPositions", "Số lượng cần tuyển phải từ " + POSITIONS_MIN + " đến " + POSITIONS_MAX + " người.");
        }

        // Rule: Ngày dự kiến bắt đầu làm việc phải từ ngày hôm nay trở đi (chỉ khi gửi duyệt)
        // và không quá START_DATE_MAX_YEARS năm kể từ hôm nay (luôn kiểm tra, kể cả bản nháp:
        // ô ngày cho gõ năm 5-6 chữ số, vượt giới hạn kiểu DATE của SQL Server)
        LocalDate startDate = requisitionRequest.getExpectedStartDate();
        LocalDate today = LocalDate.now();
        if (isSubmitAction && (startDate == null || startDate.isBefore(today))) {
            errors.put("expectedStartDate", "Ngày bắt đầu dự kiến phải từ hôm nay trở đi.");
        } else if (startDate != null && startDate.isAfter(today.plusYears(START_DATE_MAX_YEARS))) {
            errors.put("expectedStartDate", "Ngày bắt đầu dự kiến không được quá " + START_DATE_MAX_YEARS + " năm kể từ hôm nay.");
        }

        // Rule: Mức lương phải lớn hơn 0 và lương tối đa >= lương tối thiểu
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
            // Rule: Dòng tiêu chí đã bắt đầu nhập thì phải điền đủ, kể cả khi lưu nháp
            // (CriteriaName, CriteriaType, RequiredValue, Weight đều NOT NULL trong bảng ScreeningCriteria).
            // Dòng để trống hoàn toàn thì đã được bỏ qua ở trên.
            validateTextField(errors, fieldPrefix + "criteriaName", criteriaRow.getCriteriaName(), 150, true);
            validateTextField(errors, fieldPrefix + "requiredValue", criteriaRow.getRequiredValue(), SHORT_FIELD_CHAR_LIMIT, true);
            validateChoiceField(errors, fieldPrefix + "criteriaType", criteriaRow.getCriteriaType(), CRITERIA_TYPES, true);

            // Rule: Tên các tiêu chí sàng lọc không được trùng lặp
            if (criteriaRow.getCriteriaName() != null && !criteriaNames.add(criteriaRow.getCriteriaName().toLowerCase(Locale.ROOT))) {
                errors.put(fieldPrefix + "criteriaName", "Tên các tiêu chí không được trùng nhau.");
            }
            if (criteriaRow.getCriteriaId() != null && !criteriaIds.add(criteriaRow.getCriteriaId())) {
                errors.put(fieldPrefix + "criteriaName", "Tiêu chí đã được gửi hai lần.");
            }

            // Rule: Trọng số mỗi tiêu chí từ 0.01 đến 100%, tối đa 2 chữ số thập phân (cột DECIMAL(5,2)).
            // Áp dụng cả khi lưu nháp: một tiêu chí không thể chiếm quá 100% tổng điểm.
            BigDecimal weight = criteriaRow.getWeight();
            if (weight == null) {
                errors.put(fieldPrefix + "weight", "Vui lòng nhập trọng số.");
            } else {
                if (weight.signum() <= 0 || weight.compareTo(REQUIRED_TOTAL_WEIGHT) > 0 || weight.stripTrailingZeros().scale() > 2) {
                    errors.put(fieldPrefix + "weight", "Trọng số phải từ 0.01 đến 100 với tối đa hai chữ số thập phân.");
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

    /**
     * Làm sạch nội dung ô nhiều dòng (textarea): trim và đổi xuống dòng "\r\n" thành "\n".
     * Trình duyệt đếm mỗi lần xuống dòng là 1 ký tự (maxlength), nhưng khi gửi form lại gửi "\r\n" (2 ký tự).
     * Không đổi thì văn bản nhiều dòng vừa đủ 2000 ký tự trên form sẽ bị server báo vượt giới hạn.
     */
    public static String cleanMultiline(String textValue) {
        String cleaned = cleanString(textValue);
        return cleaned == null ? null : cleaned.replace("\r\n", "\n");
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
            if ("title".equals(fieldKey)) {
                errorsMap.put(fieldKey, "Tên vị trí không được để trống.");
            } else if ("workLocation".equals(fieldKey)) {
                errorsMap.put(fieldKey, "Địa điểm làm việc không được để trống.");
            } else {
                errorsMap.put(fieldKey, "Vui lòng nhập trường này.");
            }
        } else if (textValue != null && textValue.length() > maxLength) {
            errorsMap.put(fieldKey, "Tối đa " + maxLength + " ký tự.");
        }
    }

    private void validateChoiceField(Map<String, String> errorsMap, String fieldKey, String selectedValue, List<String> allowedChoices, boolean isRequired) {
        if ((isRequired || selectedValue != null) && (selectedValue == null || !allowedChoices.contains(selectedValue))) {
            errorsMap.put(fieldKey, "Vui lòng chọn một tùy chọn hợp lệ.");
        }
    }

    /** Lương (không bắt buộc): nếu nhập thì từ 1 đến SALARY_MAX VND/tháng, tối đa 2 chữ số thập phân. */
    private void validateMoneyField(Map<String, String> errorsMap, String fieldKey, BigDecimal amount) {
        if (amount == null) {
            return;
        }
        if (amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 2) {
            errorsMap.put(fieldKey, "Mức lương phải lớn hơn 0 với tối đa 2 chữ số thập phân.");
        } else if (amount.compareTo(SALARY_MAX) > 0) {
            errorsMap.put(fieldKey, "Mức lương không được vượt quá 1.000.000.000 VND/tháng.");
        }
    }
}
