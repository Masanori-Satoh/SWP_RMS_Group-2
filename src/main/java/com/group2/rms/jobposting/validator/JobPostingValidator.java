package com.group2.rms.jobposting.validator;

import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.validator.RequisitionValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Quy tắc kiểm tra của tin tuyển dụng. JS ở form (pages/job-posting-form.js) viết GIỐNG các quy tắc này
 * để người dùng thấy lỗi sớm, nhưng server mới là nơi quyết định.
 */
@Component
public class JobPostingValidator {

    public static final int TITLE_LIMIT = 200;
    /** Mô tả / Yêu cầu / Quyền lợi là NVARCHAR(MAX), giới hạn ở tầng ứng dụng để trang tin không quá dài. */
    public static final int TEXT_LIMIT = 5000;
    public static final int SALARY_TEXT_LIMIT = 100;
    public static final int POSITIONS_TEXT_LIMIT = 50;
    public static final int LOCATION_LIMIT = 255;
    /** Hẹn đăng tối đa bao nhiêu ngày kể từ hôm nay. */
    public static final int MAX_SCHEDULE_DAYS = 90;
    /** Hạn nộp được lưu là cuối ngày đã chọn (23:59:59). */
    private static final int DEADLINE_HOUR = 23;
    private static final int DEADLINE_MINUTE = 59;
    private static final int DEADLINE_SECOND = 59;

    private static final Set<String> SALARY_MODES = Set.of(JobPostingCreateRequest.SALARY_RANGE,
            JobPostingCreateRequest.SALARY_NEGOTIABLE, JobPostingCreateRequest.SALARY_CUSTOM);
    private static final Set<String> PUBLISH_MODES = Set.of(JobPostingCreateRequest.PUBLISH_NOW,
            JobPostingCreateRequest.PUBLISH_LATER);

    public void requireApproved(JobRequisition requisition) {
        if (!RequisitionAccess.STATUS_APPROVED.equals(requisition.getApprovalStatus())) {
            throw new JobPostingValidationException("Chỉ có thể đăng tin từ yêu cầu đã được phê duyệt.");
        }
    }

    /**
     * Kiểm tra toàn bộ form. Lỗi nào cũng gom vào một map (tên ô → câu lỗi) rồi ném một lần,
     * để người dùng thấy hết lỗi cùng lúc.
     *
     * @param request     dữ liệu form (chuỗi được trim ngay tại đây)
     * @param publishing  true khi tin sẽ / đang hiển thị cho ứng viên (đăng tin, hoặc sửa tin đã đăng): bắt buộc hạn nộp
     * @param canSchedule true khi tin chưa lên trang tuyển dụng (tạo mới, nháp, hẹn đăng): được chọn "Hẹn ngày đăng"
     * @param now         thời điểm hiện tại (truyền vào để test được)
     */
    public void validateForm(JobPostingCreateRequest request, boolean publishing, boolean canSchedule, LocalDateTime now) {
        Map<String, String> errors = new LinkedHashMap<>();

        request.setPostingTitle(RequisitionValidator.cleanString(request.getPostingTitle()));
        request.setJobDescription(RequisitionValidator.cleanMultiline(request.getJobDescription()));
        request.setJobRequirements(RequisitionValidator.cleanMultiline(request.getJobRequirements()));
        request.setBenefits(RequisitionValidator.cleanMultiline(request.getBenefits()));
        request.setWorkLocation(RequisitionValidator.cleanString(request.getWorkLocation()));
        request.setSalaryCustom(RequisitionValidator.cleanString(request.getSalaryCustom()));
        request.setNumberOfPositionsDisplay(RequisitionValidator.cleanString(request.getNumberOfPositionsDisplay()));

        // Rule: action chỉ là "draft" (lưu nháp / lưu thay đổi) hoặc "publish" (đăng tin / lên lịch đăng)
        if (!"draft".equals(request.getAction()) && !"publish".equals(request.getAction())) {
            errors.put("action", "Thao tác lưu tin không hợp lệ.");
        }

        // Rule: tiêu đề, mô tả, yêu cầu bắt buộc cả khi lưu nháp (các cột này NOT NULL trong bảng JobPosting)
        requireText(errors, "postingTitle", request.getPostingTitle(), TITLE_LIMIT, "Vui lòng nhập tiêu đề tin.");
        requireText(errors, "jobDescription", request.getJobDescription(), TEXT_LIMIT, "Vui lòng nhập mô tả công việc.");
        requireText(errors, "jobRequirements", request.getJobRequirements(), TEXT_LIMIT, "Vui lòng nhập yêu cầu ứng viên.");
        maxLength(errors, "benefits", request.getBenefits(), TEXT_LIMIT);
        maxLength(errors, "workLocation", request.getWorkLocation(), LOCATION_LIMIT);

        // Rule: mức lương hiển thị là 1 trong 3 lựa chọn; "Tự nhập" thì phải có nội dung
        // (Set.of(...).contains(null) ném lỗi nên kiểm tra null trước)
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

        // Ngày đăng: "Đăng ngay" hoặc "Hẹn ngày đăng" (chỉ khi tin chưa lên trang tuyển dụng).
        // Lưu nháp không giữ lịch đăng (PostingDate của tin nháp luôn NULL), nên chỉ kiểm tra khi tin sẽ được đăng.
        LocalDateTime postingTime = now;
        boolean later = publishing && JobPostingCreateRequest.PUBLISH_LATER.equals(request.getPublishMode());
        if (request.getPublishMode() != null && !PUBLISH_MODES.contains(request.getPublishMode())) {
            errors.put("publishMode", "Vui lòng chọn ngày đăng.");
        } else if (later && !canSchedule) {
            errors.put("publishMode", "Tin đã đăng nên không hẹn lại ngày đăng được.");
        } else if (later) {
            LocalDateTime scheduledAt = request.getScheduledAt();
            if (scheduledAt == null) {
                errors.put("scheduledAt", "Vui lòng chọn thời điểm đăng.");
            } else if (!scheduledAt.isAfter(now)) {
                errors.put("scheduledAt", "Thời điểm hẹn đăng phải sau thời điểm hiện tại.");
            } else if (scheduledAt.isAfter(now.plusDays(MAX_SCHEDULE_DAYS))) {
                errors.put("scheduledAt", "Chỉ hẹn đăng trong vòng " + MAX_SCHEDULE_DAYS + " ngày tới.");
            } else {
                postingTime = scheduledAt;
            }
        }

        // Rule (D6): đăng tin bắt buộc có hạn nộp; hạn nộp phải sau ngày đăng (CK_JobPosting_Deadline)
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

        if (!errors.isEmpty()) {
            throw new JobPostingValidationException(errors);
        }
    }

    /** Hạn nộp lưu vào DB: cuối ngày đã chọn; null nếu chưa chọn. */
    public LocalDateTime deadlineOf(JobPostingCreateRequest request) {
        if (request.getApplicationDeadline() == null) {
            return null;
        }
        return request.getApplicationDeadline().atTime(DEADLINE_HOUR, DEADLINE_MINUTE, DEADLINE_SECOND);
    }

    public void requireEditable(String status) {
        if (!"Draft".equals(status) && !"Published".equals(status) && !"Paused".equals(status)) {
            throw new JobPostingValidationException("Tin đã đóng không thể chỉnh sửa.");
        }
    }

    public void requirePublishableContent(JobPosting posting) {
        if (posting.getPostingTitle() == null || posting.getPostingTitle().isBlank()
                || posting.getJobDescription() == null || posting.getJobDescription().isBlank()
                || posting.getJobRequirements() == null || posting.getJobRequirements().isBlank()) {
            throw new JobPostingValidationException("Tin thiếu tiêu đề, mô tả hoặc yêu cầu ứng viên.");
        }
        if (posting.getApplicationDeadline() == null) {
            throw new JobPostingValidationException("Tin chưa có hạn nộp hồ sơ. Hãy chỉnh sửa tin trước khi đăng.");
        }
    }

    private void requireText(Map<String, String> errors, String field, String value, int maxLength, String requiredMessage) {
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
}
