package com.group2.rms.jobposting.validator;

import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionAccess;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class JobPostingValidator {
    public void requireApproved(JobRequisition requisition) {
        if (!RequisitionAccess.STATUS_APPROVED.equals(requisition.getApprovalStatus())) {
            throw new JobPostingValidationException("Chỉ có thể đăng tin từ yêu cầu đã được phê duyệt.");
        }
    }

    public LocalDateTime deadline(JobPostingCreateRequest request, LocalDateTime now, boolean publishing) {
        if (Boolean.TRUE.equals(request.getIsContinuousRecruitment())) return null;
        var date = request.getApplicationDeadline();
        if (date == null && publishing) {
            throw new JobPostingValidationException("Vui lòng chọn hạn nộp hoặc tuyển liên tục.");
        }
        if (date == null) return null;
        LocalDateTime deadline = date.atTime(23, 59, 59);
        if (!deadline.isAfter(now)) {
            throw new JobPostingValidationException("Hạn nộp hồ sơ phải sau thời điểm hiện tại.");
        }
        return deadline;
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
    }
}
