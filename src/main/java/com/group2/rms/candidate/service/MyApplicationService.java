package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.MyApplicationResponse;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.exception.ApplicationWithdrawalException;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Đơn của chính ứng viên đang đăng nhập (5.1.38 My Applications) và rút đơn (UC 39).
 * Rút đơn: schema không có trạng thái {@code Withdrawn} nên đơn chuyển {@code Rejected}
 * và ghi một dòng {@code AuditLog} để phân biệt "đã rút" với "bị loại".
 */
@Service
@RequiredArgsConstructor
public class MyApplicationService {

    /** Chỉ rút được khi đơn còn ở vòng đầu, HR chưa duyệt (SRS: Applied, Screening). */
    static final Set<String> WITHDRAWABLE_STATUSES = Set.of("Applied", "AI_Screened");

    private final ApplicationRepository applications;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<MyApplicationResponse> list() {
        Integer userId = currentUserService.requireUser().getUserId();
        Set<Integer> withdrawnIds = Set.copyOf(applications.findWithdrawnApplicationIds(userId));
        return applications.findOwnedByUserId(userId).stream()
                .map(application -> toResponse(application, withdrawnIds))
                .toList();
    }

    /**
     * Rút một đơn của chính mình.
     *
     * @return tên vị trí, cho thông báo thành công
     */
    @Transactional
    public String withdraw(Integer applicationId) {
        User user = currentUserService.requireUser();
        Application application = applications.findOwnedById(applicationId, user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));

        String status = application.getApplicationStatus();
        if (!WITHDRAWABLE_STATUSES.contains(status)) {
            throw new ApplicationWithdrawalException(
                    "Hồ sơ đã qua vòng sơ tuyển nên không thể rút. Vui lòng liên hệ bộ phận tuyển dụng.");
        }
        if (applications.transition(applicationId, WITHDRAWABLE_STATUSES, ApplicationReviewService.REJECTED) == 0) {
            throw new ApplicationWithdrawalException("Hồ sơ vừa được cập nhật. Vui lòng tải lại trang.");
        }
        applications.recordWithdrawal(user.getUserId(), applicationId, status);
        return application.getJobPosting().getPostingTitle();
    }

    private static MyApplicationResponse toResponse(Application application, Set<Integer> withdrawnIds) {
        String status = application.getApplicationStatus();
        boolean withdrawn = ApplicationReviewService.REJECTED.equals(status)
                && withdrawnIds.contains(application.getApplicationId());
        return new MyApplicationResponse(
                application.getApplicationId(),
                application.getJobPosting().getJobPostingId(),
                application.getJobPosting().getPostingTitle(),
                application.getSubmissionDate(),
                withdrawn ? ApplicationStatusLabels.WITHDRAWN_LABEL : ApplicationStatusLabels.label(status),
                withdrawn ? "badge--neutral" : ApplicationStatusLabels.badge(status),
                WITHDRAWABLE_STATUSES.contains(status));
    }
}
