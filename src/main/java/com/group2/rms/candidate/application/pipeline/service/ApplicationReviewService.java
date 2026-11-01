package com.group2.rms.candidate.application.pipeline.service;

import com.group2.rms.candidate.application.pipeline.dto.ApplicationReviewRequest;
import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.entity.ApplicationReview;
import com.group2.rms.candidate.application.pipeline.exception.ApplicationReviewException;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.candidate.domain.repository.ApplicationReviewRepository;
import com.group2.rms.candidate.domain.rule.ApplicationAccess;
import com.group2.rms.candidate.domain.rule.ApplicationStatusLabels;
import com.group2.rms.candidate.domain.rule.ApplicationStatusService;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.notification.service.NotificationService;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * HR và trưởng bộ phận duyệt hồ sơ (SRS UC 42, 46). HM vắng thì Director duyệt thay ở bước HM
 * (Q2.3); lượt duyệt ghi vai trò {@code Director} để lịch sử hiện "Giám đốc".
 * Mỗi lượt duyệt = 1 dòng {@code ApplicationReview} + (nếu Đạt / Không đạt) đổi trạng thái, trong cùng một transaction.
 * Trạng thái chỉ đi tiến (SRS GBR-01):
 * <pre>
 *   Applied --HR Đạt--> HR_Passed --HM (hoặc Director) Đạt--> HM_Passed
 *   bất kỳ bước nào ở trên --Không đạt--> Rejected;  Tạm giữ: giữ nguyên
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class ApplicationReviewService {

    private final ApplicationRepository applications;
    private final ApplicationReviewRepository reviews;
    private final ApplicationAccess access;
    private final NotificationService notificationService;

    /** @return thông báo thành công để hiện trên trang chi tiết */
    @Transactional
    public String review(Integer applicationId, ApplicationReviewRequest request) {
        User actor = access.actor();
        Application application = load(applicationId);
        access.requireView(actor, application);
        if (!access.isReviewer(actor)) {
            throw new AccessDeniedException("Chỉ HR, trưởng bộ phận hoặc Giám đốc (duyệt thay) được duyệt hồ sơ.");
        }
        if (!access.canReview(actor, application)) {
            throw new ApplicationReviewException(null, "Hồ sơ đang ở bước \""
                    + ApplicationStatusLabels.label(application.getApplicationStatus())
                    + "\", không phải bước bạn duyệt. Vui lòng tải lại trang.");
        }

        boolean hrStep = ApplicationAccess.HR_REVIEW_STATUSES.contains(application.getApplicationStatus());
        boolean onBehalf = !hrStep && access.isDirector(actor);
        String decision = request.decision();
        reviews.save(ApplicationReview.builder()
                .application(application)
                .reviewer(actor)
                .reviewerRole(hrStep ? "HR" : onBehalf ? "Director" : "HiringManager")
                .decision(decision)
                .comments(request.comments() == null || request.comments().isBlank() ? null : request.comments().strip())
                .build());

        String target = switch (decision) {
            case "Pass" -> hrStep ? "HR_Passed" : "HM_Passed";
            case "Fail" -> ApplicationStatusService.REJECTED;
            default -> null;
        };
        if (target != null) {
            Set<String> from = hrStep ? ApplicationAccess.HR_REVIEW_STATUSES : Set.of(ApplicationAccess.HM_REVIEW_STATUS);
            if (applications.transition(applicationId, from, target) == 0) {
                throw new ApplicationReviewException(null,
                        "Hồ sơ vừa được người khác xử lý. Vui lòng tải lại trang.");
            }
        }
        // hoangnh bổ sung: Đạt thì báo người làm bước tiếp theo
        if ("Pass".equals(decision)) {
            if (hrStep) {
                notificationService.notifyApplicationAwaitingHiringManager(application, actor);
            } else {
                notificationService.notifyApplicationReadyForInterview(application, actor);
            }
        }
        return switch (decision) {
            case "Pass" -> hrStep ? "Đã chuyển hồ sơ cho trưởng bộ phận."
                    : onBehalf ? "Đã duyệt thay trưởng bộ phận. HR có thể lên lịch phỏng vấn."
                    : "Đã duyệt hồ sơ. HR có thể lên lịch phỏng vấn.";
            case "Fail" -> "Đã đánh dấu hồ sơ không đạt.";
            default -> "Đã tạm giữ hồ sơ.";
        };
    }

    private Application load(Integer applicationId) {
        return applications.findDetailById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));
    }
}
