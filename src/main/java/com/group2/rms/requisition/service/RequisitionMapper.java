package com.group2.rms.requisition.service;

import com.group2.rms.admin.dto.ActivityLogResponse;
import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.requisition.dto.ApprovalResponse;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;


@Component
@RequiredArgsConstructor
public class RequisitionMapper {

    private final RequisitionAccess access;
    private final RequisitionValidator validator;
    private final Clock clock;

    // Maps a requisition to its edit form.
    public RequisitionRequest toEditForm(JobRequisition jobRequisition) {
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
                .title(jobRequisition.getTitle())
                .departmentId(departmentId)
                .numberOfPositions(jobRequisition.getNumberOfPositions())
                .employmentType(jobRequisition.getEmploymentType())
                .minSalary(jobRequisition.getMinSalary())
                .maxSalary(jobRequisition.getMaxSalary())
                .gender(jobRequisition.getGender())
                .workLocation(jobRequisition.getWorkLocation())
                .workModel(jobRequisition.getWorkModel())
                .probationPeriod(jobRequisition.getProbationPeriod())
                .reasonForHiring(jobRequisition.getReasonForHiring())
                .jobDescription(jobRequisition.getJobDescription())
                .requirementDetails(jobRequisition.getRequirementDetails())
                .build();
    }


    // Maps a requisition to a list row.
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
                .statusLabel(RequisitionLabels.approvalStatus(approvalStatus))
                .statusTone(RequisitionLabels.approvalTone(approvalStatus))
                .postingLabel(RequisitionLabels.postingStatus(postingStatus))
                .postingTone(RequisitionLabels.postingTone(postingStatus))
                .employmentTypeLabel(RequisitionLabels.employmentType(jobRequisition.getEmploymentType()))
                .editable(access.canEdit(viewer, jobRequisition))
                .deletable(access.canDelete(viewer, jobRequisition, hasPosting, hasLaterRound))
                .decidable(access.canDecide(viewer, jobRequisition))
                .copyable(access.canCopy(viewer, jobRequisition))
                .postable(access.canCreatePostingFrom(viewer, jobRequisition, hasPosting))
                .postingViewable(access.canOpenPosting(viewer, postingStatus))
                .build();
    }


    // Maps a requisition to its detail response.
    public RequisitionResponse toDetail(JobRequisition jobRequisition, User viewer, JobPosting latestPosting,
            boolean hasLaterRound, List<RequisitionApproval> approvalHistory, List<AuditLog> activityLogs) {
        String approvalStatus = jobRequisition.getApprovalStatus();
        boolean incomplete = false;
        if (RequisitionAccess.STATUS_DRAFT.equals(approvalStatus)) {
            incomplete = !isReadyToSubmit(jobRequisition);
        }
        LocalDate today = LocalDate.now(clock);
        Long waitingDays = null;
        if (RequisitionAccess.STATUS_PENDING_DIRECTOR.equals(approvalStatus) && jobRequisition.getSubmittedAt() != null) {
            waitingDays = ChronoUnit.DAYS.between(jobRequisition.getSubmittedAt().toLocalDate(), today);
        }
        List<ApprovalResponse> approvals = toApprovalResponses(approvalHistory);
        String rejectionReason = null;
        if (RequisitionAccess.STATUS_REJECTED.equals(approvalStatus) && !approvals.isEmpty()) {
            rejectionReason = approvals.getFirst().comments();
        }

        return toListRow(jobRequisition, viewer, latestPosting, hasLaterRound).toBuilder()
                .incomplete(incomplete)
                .salaryLabel(RequisitionLabels.salary(jobRequisition.getMinSalary(), jobRequisition.getMaxSalary()))
                .probationLabel(RequisitionLabels.probation(jobRequisition.getProbationPeriod()))
                .genderLabel(RequisitionLabels.gender(jobRequisition.getGender()))
                .workModelLabel(RequisitionLabels.workModel(jobRequisition.getWorkModel()))
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


    // Checks whether the requisition is ready for submission.
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

    // Maps approval history to response objects.
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

    // Maps audit history to activity responses.
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
