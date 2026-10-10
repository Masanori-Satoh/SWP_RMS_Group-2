package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.ApplicationReviewRequest;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.ApplicationReview;
import com.group2.rms.candidate.exception.ApplicationReviewException;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.repository.ApplicationReviewRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * HR và trưởng bộ phận duyệt hồ sơ (SRS UC 42, 46), và HR chấm lại AI.
 * Mỗi lượt duyệt = 1 dòng {@code ApplicationReview} + (nếu Đạt / Không đạt) đổi trạng thái, trong cùng một transaction.
 * Trạng thái chỉ đi tiến (SRS GBR-01):
 * <pre>
 *   Applied, AI_Screened --HR Đạt--> HR_Passed --HM Đạt--> HM_Passed
 *   bất kỳ bước nào ở trên --Không đạt--> Rejected;  Tạm giữ: giữ nguyên
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class ApplicationReviewService {

    static final String REJECTED = "Rejected";

    private final ApplicationRepository applications;
    private final ApplicationReviewRepository reviews;
    private final ApplicationAccess access;
    private final AiScreeningService aiScreeningService;

    /** @return thông báo thành công để hiện trên trang chi tiết */
    @Transactional
    public String review(Integer applicationId, ApplicationReviewRequest request) {
        User actor = access.actor();
        Application application = load(applicationId);
        access.requireView(actor, application);
        if (!access.isReviewer(actor)) {
            throw new AccessDeniedException("Chỉ HR và trưởng bộ phận được duyệt hồ sơ.");
        }
        if (!access.canReview(actor, application)) {
            throw new ApplicationReviewException(null, "Hồ sơ đang ở bước \""
                    + ApplicationStatusLabels.label(application.getApplicationStatus())
                    + "\", không phải bước bạn duyệt. Vui lòng tải lại trang.");
        }

        boolean hrStep = ApplicationAccess.HR_REVIEW_STATUSES.contains(application.getApplicationStatus());
        String decision = request.decision();
        reviews.save(ApplicationReview.builder()
                .application(application)
                .reviewer(actor)
                .reviewerRole(hrStep ? "HR" : "HiringManager")
                .decision(decision)
                .comments(request.comments() == null || request.comments().isBlank() ? null : request.comments().strip())
                .build());

        String target = switch (decision) {
            case "Pass" -> hrStep ? "HR_Passed" : "HM_Passed";
            case "Fail" -> REJECTED;
            default -> null;
        };
        if (target != null) {
            Set<String> from = hrStep ? ApplicationAccess.HR_REVIEW_STATUSES : Set.of(ApplicationAccess.HM_REVIEW_STATUS);
            if (applications.transition(applicationId, from, target) == 0) {
                throw new ApplicationReviewException(null,
                        "Hồ sơ vừa được người khác xử lý. Vui lòng tải lại trang.");
            }
        }
        return switch (decision) {
            case "Pass" -> hrStep ? "Đã chuyển hồ sơ cho trưởng bộ phận." : "Đã duyệt hồ sơ. HR có thể lên lịch phỏng vấn.";
            case "Fail" -> "Đã đánh dấu hồ sơ không đạt.";
            default -> "Đã tạm giữ hồ sơ.";
        };
    }

    /** HR chấm lại AI trước khi duyệt (đơn chưa có điểm, hoặc muốn chấm lần nữa). Chạy ngay, không chạy nền. */
    @Transactional
    public AiScreeningOutcome rescreen(Integer applicationId) {
        User actor = access.actor();
        Application application = load(applicationId);
        access.requireView(actor, application);
        if (!access.isHr(actor)) {
            throw new AccessDeniedException("Chỉ HR được chấm lại AI.");
        }
        if (!access.canRescreen(actor, application)) {
            throw new ApplicationReviewException(null, "Chỉ chấm lại AI khi hồ sơ chưa qua vòng nhân sự.");
        }
        return aiScreeningService.screen(applicationId);
    }

    private Application load(Integer applicationId) {
        return applications.findDetailById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));
    }
}
