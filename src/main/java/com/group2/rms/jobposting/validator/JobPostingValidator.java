package com.group2.rms.jobposting.validator;

import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.validator.RequisitionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Quy tắc kiểm tra dữ liệu của tin tuyển dụng (tầng 3, AG §2.4). JS ở form (pages/job-posting-form.js) viết GIỐNG
 * các quy tắc này để người dùng thấy lỗi sớm, nhưng server mới là nơi quyết định.
 * Không kiểm tra quyền (JobPostingAccess). Giới hạn / câu lỗi để public để DTO, controller, template và test dùng chung.
 */
@Component
@RequiredArgsConstructor
public class JobPostingValidator {

    /** Hai giá trị nút trên form: "Lưu nháp" / "Lưu thay đổi" gửi draft, "Đăng tin" / "Lên lịch đăng" / "Lưu và mở lại" gửi publish. */
    public static final String ACTION_DRAFT = "draft";
    public static final String ACTION_PUBLISH = "publish";

    /** Cột PostingTitle NVARCHAR(200). */
    public static final int TITLE_LIMIT = 200;
    /** Mô tả / Yêu cầu / Quyền lợi là NVARCHAR(MAX), giới hạn ở tầng ứng dụng để trang tin không quá dài. */
    public static final int TEXT_LIMIT = 5000;
    /** Cột SalaryDisplay NVARCHAR(100). */
    public static final int SALARY_TEXT_LIMIT = 100;
    /** Cột NumberOfPositionsDisplay NVARCHAR(50). */
    public static final int POSITIONS_TEXT_LIMIT = 50;
    /** Cột WorkLocation NVARCHAR(255). */
    public static final int LOCATION_LIMIT = 255;
    /** Hẹn đăng tối đa bao nhiêu ngày kể từ bây giờ. */
    public static final int MAX_SCHEDULE_DAYS = 90;
    /** Hạn nộp điền sẵn khi tạo tin: hôm nay + 30 ngày (đúng mockup). */
    public static final int DEFAULT_DEADLINE_DAYS = 30;
    /** Hạn nộp được lưu là cuối ngày đã chọn (23:59:59). */
    private static final LocalTime DEADLINE_TIME = LocalTime.of(23, 59, 59);

    // Câu lỗi dùng chung giữa annotation trên DTO (Bean Validation) và normalizeAndValidate() bên dưới
    public static final String TITLE_TOO_LONG = "Nội dung quá " + TITLE_LIMIT + " ký tự";
    public static final String LOCATION_TOO_LONG = "Nội dung quá " + LOCATION_LIMIT + " ký tự";
    public static final String SALARY_TEXT_TOO_LONG = "Nội dung quá " + SALARY_TEXT_LIMIT + " ký tự";
    public static final String POSITIONS_TEXT_TOO_LONG = "Nội dung quá " + POSITIONS_TEXT_LIMIT + " ký tự";

    private static final Set<String> ACTIONS = Set.of(ACTION_DRAFT, ACTION_PUBLISH);
    private static final List<String> SALARY_MODES = List.of(JobPostingCreateRequest.SALARY_RANGE,
            JobPostingCreateRequest.SALARY_NEGOTIABLE, JobPostingCreateRequest.SALARY_CUSTOM);
    private static final List<String> PUBLISH_MODES =
            List.of(JobPostingCreateRequest.PUBLISH_NOW, JobPostingCreateRequest.PUBLISH_LATER);

    /** Đồng hồ dùng chung (core/config/ClockConfig); test truyền Clock.fixed để cố định "bây giờ". */
    private final Clock clock;

    /** Người dùng bấm nút đăng ("Đăng tin", "Lên lịch đăng", "Lưu và mở lại"). */
    public static boolean isPublish(String action) {
        return ACTION_PUBLISH.equals(action);
    }

    /** Hôm nay: ngày sớm nhất được chọn làm hạn nộp (thuộc tính min của ô ngày trên form). */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Hạn nộp điền sẵn khi mở form tạo tin. */
    public LocalDate defaultDeadline() {
        return today().plusDays(DEFAULT_DEADLINE_DAYS);
    }

    public void requireApproved(JobRequisition requisition) {
        if (!RequisitionAccess.STATUS_APPROVED.equals(requisition.getApprovalStatus())) {
            throw new JobPostingValidationException("Chỉ có thể đăng tin từ yêu cầu đã được phê duyệt.");
        }
    }

    public void requireEditable(String postingStatus) {
        if (!JobPostingAccess.isEditableStatus(postingStatus)) {
            throw new JobPostingValidationException("Tin đã đóng không thể chỉnh sửa.");
        }
    }

    /** Đăng tin nháp / mở lại tin tạm dừng từ nút trên trang chi tiết: tin phải đủ nội dung và còn hạn nộp. */
    public void requirePublishableContent(JobPosting posting, LocalDateTime now) {
        if (isBlank(posting.getPostingTitle()) || isBlank(posting.getJobDescription())
                || isBlank(posting.getJobRequirements())) {
            throw new JobPostingValidationException("Tin thiếu tiêu đề, mô tả hoặc yêu cầu ứng viên.");
        }
        if (posting.getApplicationDeadline() == null) {
            throw new JobPostingValidationException("Tin chưa có hạn nộp hồ sơ. Hãy chỉnh sửa tin trước khi đăng.");
        }
        if (!posting.getApplicationDeadline().isAfter(now)) {
            throw new JobPostingValidationException("Hạn nộp đã qua. Hãy cập nhật tin trước khi đăng.");
        }
    }

    /**
     * Làm sạch rồi kiểm tra toàn bộ form. Lỗi nào cũng gom vào một map (tên ô → câu lỗi) rồi ném một lần,
     * để người dùng thấy hết lỗi cùng lúc. Có sửa DTO (trim chuỗi, đổi \r\n thành \n).
     *
     * @param request     dữ liệu form
     * @param publishing  true khi tin sẽ / đang hiển thị cho ứng viên (đăng tin, hoặc sửa tin đã đăng): bắt buộc hạn nộp
     * @param canSchedule true khi tin chưa lên trang tuyển dụng (tạo mới, nháp, hẹn đăng): được chọn "Hẹn ngày đăng"
     */
    public void normalizeAndValidate(JobPostingCreateRequest request, boolean publishing, boolean canSchedule) {
        LocalDateTime now = LocalDateTime.now(clock);
        normalize(request);
        Map<String, String> errors = new LinkedHashMap<>();

        // Rule: action chỉ là "draft" (lưu nháp / lưu thay đổi) hoặc "publish" (đăng tin / lên lịch đăng)
        if (request.getAction() == null || !ACTIONS.contains(request.getAction())) {
            errors.put("action", "Thao tác lưu tin không hợp lệ.");
        }

        // Rule: tiêu đề, mô tả, yêu cầu bắt buộc cả khi lưu nháp (các cột này NOT NULL trong bảng JobPosting)
        requireText(errors, "postingTitle", request.getPostingTitle(), TITLE_LIMIT, "Vui lòng nhập tiêu đề tin.");
        requireText(errors, "jobDescription", request.getJobDescription(), TEXT_LIMIT, "Vui lòng nhập mô tả công việc.");
        requireText(errors, "jobRequirements", request.getJobRequirements(), TEXT_LIMIT, "Vui lòng nhập yêu cầu ứng viên.");
        maxLength(errors, "benefits", request.getBenefits(), TEXT_LIMIT);
        maxLength(errors, "workLocation", request.getWorkLocation(), LOCATION_LIMIT);

        // Rule: mức lương hiển thị là 1 trong 3 lựa chọn; "Tự nhập" thì phải có nội dung
        // (List.of(...).contains(null) ném NullPointerException nên kiểm tra null trước)
        if (request.getSalaryMode() == null || !SALARY_MODES.contains(request.getSalaryMode())) {
            errors.put("salaryMode", "Vui lòng chọn cách hiển thị mức lương.");
        } else if (JobPostingCreateRequest.SALARY_CUSTOM.equals(request.getSalaryMode())) {
            requireText(errors, "salaryCustom", request.getSalaryCustom(), SALARY_TEXT_LIMIT,
                    "Nhập mức lương hiển thị hoặc chọn một lựa chọn khác.");
        }

        // Rule: bật "Hiển thị số lượng tuyển" thì phải có nội dung
        if (Boolean.TRUE.equals(request.getShowPositions())) {
            requireText(errors, "numberOfPositionsDisplay", request.getNumberOfPositionsDisplay(), POSITIONS_TEXT_LIMIT,
                    "Nhập số lượng hiển thị hoặc tắt hiển thị số lượng.");
        }

        LocalDateTime postingTime = validateSchedule(errors, request, publishing, canSchedule, now);
        validateDeadline(errors, request, publishing, postingTime, now);

        if (!errors.isEmpty()) {
            throw new JobPostingValidationException(errors);
        }
    }

    /** Hạn nộp lưu vào DB: cuối ngày đã chọn; null nếu chưa chọn. */
    public LocalDateTime deadlineOf(JobPostingCreateRequest request) {
        if (request.getApplicationDeadline() == null) {
            return null;
        }
        return request.getApplicationDeadline().atTime(DEADLINE_TIME);
    }

    /** Thời điểm đăng HR chọn: "Hẹn ngày đăng" → thời điểm hẹn; còn lại → ngay bây giờ. */
    public LocalDateTime chosenPostingTime(JobPostingCreateRequest request, LocalDateTime now) {
        boolean isLater = JobPostingCreateRequest.PUBLISH_LATER.equals(request.getPublishMode());
        if (isLater && request.getScheduledAt() != null) {
            return request.getScheduledAt();
        }
        return now;
    }

    // ----- Các hàm nhỏ -----

    private void normalize(JobPostingCreateRequest request) {
        request.setPostingTitle(RequisitionValidator.cleanString(request.getPostingTitle()));
        request.setJobDescription(RequisitionValidator.cleanMultiline(request.getJobDescription()));
        request.setJobRequirements(RequisitionValidator.cleanMultiline(request.getJobRequirements()));
        request.setBenefits(RequisitionValidator.cleanMultiline(request.getBenefits()));
        request.setWorkLocation(RequisitionValidator.cleanString(request.getWorkLocation()));
        request.setSalaryCustom(RequisitionValidator.cleanString(request.getSalaryCustom()));
        request.setNumberOfPositionsDisplay(RequisitionValidator.cleanString(request.getNumberOfPositionsDisplay()));
    }

    /**
     * Ngày đăng: "Đăng ngay" hoặc "Hẹn ngày đăng" (chỉ khi tin chưa lên trang tuyển dụng).
     * Lưu nháp không giữ lịch đăng (PostingDate của tin nháp luôn NULL), nên chỉ kiểm tra khi tin sẽ được đăng.
     *
     * @return thời điểm tin sẽ hiện cho ứng viên (để so với hạn nộp)
     */
    private LocalDateTime validateSchedule(Map<String, String> errors, JobPostingCreateRequest request,
            boolean publishing, boolean canSchedule, LocalDateTime now) {
        String publishMode = request.getPublishMode();
        if (publishMode != null && !PUBLISH_MODES.contains(publishMode)) {
            errors.put("publishMode", "Vui lòng chọn ngày đăng.");
            return now;
        }
        boolean isLater = publishing && JobPostingCreateRequest.PUBLISH_LATER.equals(publishMode);
        if (!isLater) {
            return now;
        }
        if (!canSchedule) {
            errors.put("publishMode", "Tin đã đăng nên không hẹn lại ngày đăng được.");
            return now;
        }
        LocalDateTime scheduledAt = request.getScheduledAt();
        if (scheduledAt == null) {
            errors.put("scheduledAt", "Vui lòng chọn thời điểm đăng.");
        } else if (!scheduledAt.isAfter(now)) {
            errors.put("scheduledAt", "Thời điểm hẹn đăng phải sau thời điểm hiện tại.");
        } else if (scheduledAt.isAfter(now.plusDays(MAX_SCHEDULE_DAYS))) {
            errors.put("scheduledAt", "Chỉ hẹn đăng trong vòng " + MAX_SCHEDULE_DAYS + " ngày tới.");
        } else {
            return scheduledAt;
        }
        return now;
    }

    /** Rule (D6): đăng tin bắt buộc có hạn nộp; hạn nộp phải sau ngày đăng (CK_JobPosting_Deadline). */
    private void validateDeadline(Map<String, String> errors, JobPostingCreateRequest request, boolean publishing,
            LocalDateTime postingTime, LocalDateTime now) {
        LocalDateTime deadline = deadlineOf(request);
        if (deadline == null) {
            if (publishing) {
                errors.put("applicationDeadline", "Vui lòng chọn hạn nộp hồ sơ.");
            }
        } else if (!deadline.isAfter(now)) {
            errors.put("applicationDeadline", "Hạn nộp hồ sơ phải từ hôm nay trở đi.");
        } else if (!deadline.isAfter(postingTime)) {
            errors.put("applicationDeadline", "Hạn nộp hồ sơ phải sau ngày đăng.");
        }
    }

    private void requireText(Map<String, String> errors, String field, String value, int maxLength,
            String requiredMessage) {
        if (value == null) {
            errors.put(field, requiredMessage);
        } else {
            maxLength(errors, field, value, maxLength);
        }
    }

    private void maxLength(Map<String, String> errors, String field, String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            errors.put(field, "Nội dung quá " + maxLength + " ký tự");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
