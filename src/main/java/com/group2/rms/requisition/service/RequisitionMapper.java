package com.group2.rms.requisition.service;

import com.group2.rms.admin.dto.ActivityLogResponse;
import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.requisition.dto.ApprovalResponse;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaResponse;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/*
 * MAPPER: entity yêu cầu → DTO cho form, danh sách, chi tiết; không truy vấn DB.
 */
@Component
@RequiredArgsConstructor
public class RequisitionMapper {

    private final RequisitionAccess access;
    private final RequisitionValidator validator;
    private final Clock clock;

    // ----- Form tạo / sửa / sao chép -----

    public RequisitionRequest toEditForm(JobRequisition jobRequisition) {
        List<ScreeningCriteriaRequest> criteriaList = new ArrayList<>();
        for (ScreeningCriteria criterion : jobRequisition.getScreeningCriteria()) {
            criteriaList.add(ScreeningCriteriaRequest.builder()
                    .criteriaId(criterion.getCriteriaId())
                    .criteriaName(criterion.getCriteriaName())
                    .criteriaType(criterion.getCriteriaType())
                    .requiredValue(criterion.getRequiredValue())
                    .weight(criterion.getWeight())
                    .isMandatory(criterion.getIsMandatory())
                    .build());
        }

        String originalTitle = null;
        if (jobRequisition.getOriginalRequisition() != null) {
            originalTitle = jobRequisition.getOriginalRequisition().getTitle();
        }
        Integer departmentId = null;
        if (jobRequisition.getDepartment() != null) {
            departmentId = jobRequisition.getDepartment().getDepartmentId();
        }

        return RequisitionRequest.builder()
                .recruitmentRound(jobRequisition.getRecruitmentRound())
                .originalTitle(originalTitle)
                .hiringType(jobRequisition.getHiringType())
                .title(jobRequisition.getTitle())
                .departmentId(departmentId)
                .numberOfPositions(jobRequisition.getNumberOfPositions())
                .employmentType(jobRequisition.getEmploymentType())
                .minSalary(jobRequisition.getMinSalary())
                .maxSalary(jobRequisition.getMaxSalary())
                .gender(jobRequisition.getGender())
                .workLocation(jobRequisition.getWorkLocation())
                .workModel(jobRequisition.getWorkModel())
                .probationDays(jobRequisition.getProbationDays())
                .reasonForHiring(jobRequisition.getReasonForHiring())
                .jobDescription(jobRequisition.getJobDescription())
                .requirementDetails(jobRequisition.getRequirementDetails())
                .screeningCriteria(criteriaList)
                .build();
    }

    // ----- Màn danh sách -----

    /** Một dòng danh sách kèm cờ quyền cho menu ⋯; latestPosting null = chưa có tin. */
    public RequisitionResponse toListRow(JobRequisition jobRequisition, User viewer,
            JobPosting latestPosting, boolean hasLaterRound) {
        String approvalStatus = jobRequisition.getApprovalStatus();
        boolean hasPosting = latestPosting != null;

        String departmentName = "Chưa xác định";
        if (jobRequisition.getDepartment() != null) {
            departmentName = jobRequisition.getDepartment().getDepartmentName();
        }
        String requisitionCode = RequisitionLabels.requisitionCode(jobRequisition.getRequisitionId(),
                jobRequisition.getRequisitionCode());

        RequisitionResponse.RequisitionResponseBuilder row = RequisitionResponse.builder()
                .requisitionId(jobRequisition.getRequisitionId())
                .title(jobRequisition.getTitle())
                .departmentName(departmentName)
                .hiringManagerName(jobRequisition.getHiringManager().getFullName())
                .numberOfPositions(jobRequisition.getNumberOfPositions())
                .approvalStatus(approvalStatus)
                .recruitmentRound(jobRequisition.getRecruitmentRound())
                .requisitionCode(requisitionCode)
                .createdAt(jobRequisition.getCreatedAt())
                .workLocation(jobRequisition.getWorkLocation());

        // Tin tuyển dụng mới nhất (nếu có)
        String postingStatus = null;
        if (hasPosting) {
            postingStatus = latestPosting.getPostingStatus();
            row.postingStatus(postingStatus)
                    .jobPostingId(latestPosting.getJobPostingId())
                    .postingNo(latestPosting.getPostingNo())
                    .postingDate(latestPosting.getPostingDate())
                    .postingDeadline(latestPosting.getApplicationDeadline());
        }

        return row
                // Nhãn tiếng Việt + tông màu badge (dịch ở một chỗ duy nhất: RequisitionLabels)
                .statusLabel(RequisitionLabels.approvalStatus(approvalStatus))
                .statusTone(RequisitionLabels.approvalTone(approvalStatus))
                .postingLabel(RequisitionLabels.postingStatus(postingStatus))
                .postingTone(RequisitionLabels.postingTone(postingStatus))
                .employmentTypeLabel(RequisitionLabels.employmentType(jobRequisition.getEmploymentType()))
                // Cờ quyền cho nút / menu ⋯ (luật nằm ở RequisitionAccess, template chỉ đọc cờ)
                .editable(access.canEdit(viewer, jobRequisition))
                .deletable(access.canDelete(viewer, jobRequisition, hasPosting, hasLaterRound))
                .decidable(access.canDecide(viewer, jobRequisition))
                .withdrawable(access.canWithdraw(viewer, jobRequisition))
                .copyable(access.canCopy(viewer, jobRequisition))
                .postable(access.canCreatePostingFrom(viewer, jobRequisition, hasPosting))
                .postingViewable(access.canOpenPosting(viewer, postingStatus))
                .build();
    }

    // ----- Màn chi tiết -----

    /** Trang chi tiết; lịch sử duyệt và audit đều mới nhất trước. */
    public RequisitionResponse toDetail(JobRequisition jobRequisition, User viewer, JobPosting latestPosting,
            boolean hasLaterRound, List<RequisitionApproval> approvalHistory, List<AuditLog> activityLogs) {
        String approvalStatus = jobRequisition.getApprovalStatus();

        // Bản nháp: đã điền đủ thông tin để gửi duyệt chưa
        boolean incomplete = false;
        if (RequisitionAccess.STATUS_DRAFT.equals(approvalStatus)) {
            incomplete = !isReadyToSubmit(jobRequisition);
        }

        // Mốc thời gian: số ngày đã chờ duyệt, còn bao nhiêu ngày tới ngày bắt đầu
        LocalDate today = LocalDate.now(clock);
        Long waitingDays = null;
        if (RequisitionAccess.STATUS_PENDING_DIRECTOR.equals(approvalStatus) && jobRequisition.getSubmittedAt() != null) {
            waitingDays = ChronoUnit.DAYS.between(jobRequisition.getSubmittedAt().toLocalDate(), today);
        }

        // Bị trả lại: lý do là ý kiến của lần duyệt mới nhất
        List<ApprovalResponse> approvals = toApprovalResponses(approvalHistory);
        String rejectionReason = null;
        if (RequisitionAccess.STATUS_REJECTED.equals(approvalStatus) && !approvals.isEmpty()) {
            rejectionReason = approvals.getFirst().comments();
        }

        return toListRow(jobRequisition, viewer, latestPosting, hasLaterRound).toBuilder()
                .incomplete(incomplete)
                .screeningCriteria(toCriteriaResponses(jobRequisition.getScreeningCriteria()))
                .criteriaTotalWeight(totalWeight(jobRequisition.getScreeningCriteria()))
                // Nhãn tiếng Việt cho các trường thông tin cơ bản
                .salaryLabel(RequisitionLabels.salary(jobRequisition.getMinSalary(), jobRequisition.getMaxSalary()))
                .probationLabel(RequisitionLabels.probation(jobRequisition.getProbationDays()))
                .genderLabel(RequisitionLabels.gender(jobRequisition.getGender()))
                .workModelLabel(RequisitionLabels.workModel(jobRequisition.getWorkModel()))
                .hiringTypeLabel(RequisitionLabels.hiringType(jobRequisition.getHiringType()))
                // Văn bản nhiều dòng: escape HTML rồi đổi xuống dòng thành <br/> (AG §6.3)
                .reasonForHiringHtml(RequisitionLabels.multilineHtml(jobRequisition.getReasonForHiring()))
                .jobDescriptionHtml(RequisitionLabels.multilineHtml(jobRequisition.getJobDescription()))
                .requirementDetailsHtml(RequisitionLabels.multilineHtml(jobRequisition.getRequirementDetails()))
                .submittedAt(jobRequisition.getSubmittedAt())
                .waitingDays(waitingDays)
                .approvals(approvals)
                .activityLog(toActivityResponses(activityLogs))
                .rejectionReason(rejectionReason)
                .build();
    }

    // ----- Các hàm nhỏ cho màn chi tiết -----

    /** Chạy thử validator như lúc bấm "Gửi duyệt": không có lỗi nghĩa là bản nháp đã đủ thông tin. */
    private boolean isReadyToSubmit(JobRequisition jobRequisition) {
        RequisitionRequest submissionCheck = toEditForm(jobRequisition);
        submissionCheck.setAction(RequisitionValidator.ACTION_SUBMIT);
        try {
            validator.validate(submissionCheck);
            return true;
        } catch (RequisitionValidationException incomplete) {
            return false;
        }
    }

    private List<ScreeningCriteriaResponse> toCriteriaResponses(List<ScreeningCriteria> criteria) {
        List<ScreeningCriteriaResponse> responses = new ArrayList<>();
        for (ScreeningCriteria criterion : criteria) {
            responses.add(ScreeningCriteriaResponse.builder()
                    .criteriaId(criterion.getCriteriaId())
                    .criteriaName(criterion.getCriteriaName())
                    .criteriaType(criterion.getCriteriaType())
                    .criteriaTypeLabel(RequisitionLabels.criteriaType(criterion.getCriteriaType()))
                    .requiredValue(criterion.getRequiredValue())
                    .weight(criterion.getWeight())
                    .isMandatory(criterion.getIsMandatory())
                    .build());
        }
        return responses;
    }

    /** Tổng trọng số tiêu chí (hiện dưới bảng tiêu chí), bỏ qua tiêu chí chưa có trọng số. */
    private BigDecimal totalWeight(List<ScreeningCriteria> criteria) {
        BigDecimal total = BigDecimal.ZERO;
        for (ScreeningCriteria criterion : criteria) {
            if (criterion.getWeight() != null) {
                total = total.add(criterion.getWeight());
            }
        }
        return total.stripTrailingZeros();
    }

    private List<ApprovalResponse> toApprovalResponses(List<RequisitionApproval> approvalHistory) {
        List<ApprovalResponse> responses = new ArrayList<>();
        for (RequisitionApproval approval : approvalHistory) {
            responses.add(ApprovalResponse.builder()
                    .approverName(approval.getDirector().getFullName())
                    .status(approval.getStatus())
                    .approvalDate(approval.getApprovalDate())
                    .comments(approval.getComments())
                    .build());
        }
        return responses;
    }

    private List<ActivityLogResponse> toActivityResponses(List<AuditLog> activityLogs) {
        List<ActivityLogResponse> responses = new ArrayList<>();
        for (AuditLog log : activityLogs) {
            responses.add(ActivityLogResponse.builder()
                    .auditLogId(log.getAuditLogId())
                    .action(log.getAction())
                    .performedBy(log.getUser().getFullName())
                    .description(log.getNewValue())
                    .timestamp(log.getTimestamp())
                    .build());
        }
        return responses;
    }
}
