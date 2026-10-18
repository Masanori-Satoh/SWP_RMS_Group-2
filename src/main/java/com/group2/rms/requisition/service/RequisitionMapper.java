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

/**
 * Chuyển entity Yêu cầu tuyển dụng sang DTO cho form, màn danh sách và màn chi tiết.
 *
 * Mapper KHÔNG truy vấn DB: dữ liệu liên quan (tin mới nhất, lịch sử duyệt, audit log...)
 * do service tra trước rồi truyền vào. Nhờ vậy:
 * - màn danh sách tra 1 lần cho cả trang (không N+1),
 * - test mapper chỉ cần tạo entity bằng builder, không cần mock repository.
 * Cờ quyền (editable, deletable...) đều hỏi RequisitionAccess, mapper không tự so sánh role.
 */
@Component
@RequiredArgsConstructor
public class RequisitionMapper {

    private final RequisitionAccess access;
    private final RequisitionValidator validator;
    private final Clock clock;

    // ===================================================================================
    // FORM: entity → DTO của form tạo / sửa / sao chép
    // ===================================================================================

    public RequisitionRequest toRequestDto(JobRequisition jobRequisition) {
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
                .expectedStartDate(jobRequisition.getExpectedStartDate())
                .reasonForHiring(jobRequisition.getReasonForHiring())
                .jobDescription(jobRequisition.getJobDescription())
                .requirementDetails(jobRequisition.getRequirementDetails())
                .screeningCriteria(criteriaList)
                .build();
    }

    // ===================================================================================
    // MÀN DANH SÁCH: một dòng của bảng
    // ===================================================================================

    /**
     * Một dòng ở màn danh sách: thông tin chính, nhãn tiếng Việt và cờ quyền cho menu ⋯.
     *
     * @param latestPosting tin tuyển dụng mới nhất của yêu cầu, null = chưa có tin
     * @param hasLaterRound đã có đợt tuyển sau sao chép từ yêu cầu này chưa (chặn xóa)
     */
    public RequisitionResponse toListRow(JobRequisition jobRequisition, User viewer,
            JobPosting latestPosting, boolean hasLaterRound) {
        String approvalStatus = jobRequisition.getApprovalStatus();
        boolean hasPosting = latestPosting != null;

        String departmentName = "Chưa xác định";
        if (jobRequisition.getDepartment() != null) {
            departmentName = jobRequisition.getDepartment().getDepartmentName();
        }
        String requisitionCode = jobRequisition.getRequisitionCode();
        if (requisitionCode == null) {
            requisitionCode = RequisitionLabels.requisitionCode(jobRequisition.getRequisitionId());
        }

        RequisitionResponse response = RequisitionResponse.builder()
                .requisitionId(jobRequisition.getRequisitionId())
                .title(jobRequisition.getTitle())
                .departmentName(departmentName)
                .hiringManagerName(jobRequisition.getHiringManager().getFullName())
                .numberOfPositions(jobRequisition.getNumberOfPositions())
                .approvalStatus(approvalStatus)
                .recruitmentRound(jobRequisition.getRecruitmentRound())
                .requisitionCode(requisitionCode)
                .createdAt(jobRequisition.getCreatedAt())
                .workLocation(jobRequisition.getWorkLocation())
                .expectedStartDate(jobRequisition.getExpectedStartDate())
                .build();

        // Tin tuyển dụng mới nhất (nếu có)
        if (hasPosting) {
            response.setPostingStatus(latestPosting.getPostingStatus());
            response.setJobPostingId(latestPosting.getJobPostingId());
            response.setPostingNo(latestPosting.getPostingNo());
            response.setPostingDate(latestPosting.getPostingDate());
            response.setPostingDeadline(latestPosting.getApplicationDeadline());
        }

        // Nhãn tiếng Việt + tông màu badge (dịch ở một chỗ duy nhất: RequisitionLabels)
        response.setStatusLabel(RequisitionLabels.approvalStatus(approvalStatus));
        response.setStatusTone(RequisitionLabels.approvalTone(approvalStatus));
        response.setPostingLabel(RequisitionLabels.postingStatus(response.getPostingStatus()));
        response.setPostingTone(RequisitionLabels.postingTone(response.getPostingStatus()));
        response.setEmploymentTypeLabel(RequisitionLabels.employmentType(jobRequisition.getEmploymentType()));

        // Cờ quyền cho nút / menu ⋯ (luật nằm ở RequisitionAccess, template chỉ đọc cờ)
        response.setEditable(access.canEdit(viewer, jobRequisition));
        response.setDeletable(access.canDelete(viewer, jobRequisition, hasPosting, hasLaterRound));
        response.setDecidable(access.canDecide(viewer, jobRequisition));
        response.setWithdrawable(access.canWithdraw(viewer, jobRequisition));
        response.setCopyable(access.canCopy(viewer, jobRequisition));
        response.setPostable(access.canCreatePostingFrom(viewer, jobRequisition, hasPosting));
        response.setPostingViewable(access.canOpenPosting(viewer, response.getPostingStatus()));
        return response;
    }

    // ===================================================================================
    // MÀN CHI TIẾT: dòng danh sách + nội dung đầy đủ, tiêu chí, lịch sử duyệt, hoạt động
    // ===================================================================================

    /**
     * @param approvalHistory các lần duyệt, mới nhất trước
     * @param activityLogs    audit log của yêu cầu, mới nhất trước
     */
    public RequisitionResponse toDetail(JobRequisition jobRequisition, User viewer, JobPosting latestPosting,
            boolean hasLaterRound, List<RequisitionApproval> approvalHistory, List<AuditLog> activityLogs) {
        RequisitionResponse response = toListRow(jobRequisition, viewer, latestPosting, hasLaterRound);
        String approvalStatus = jobRequisition.getApprovalStatus();

        // Bản nháp: đã điền đủ thông tin để gửi duyệt chưa
        if (RequisitionAccess.STATUS_DRAFT.equals(approvalStatus)) {
            response.setIncomplete(!isReadyToSubmit(jobRequisition));
        }

        response.setScreeningCriteria(toCriteriaResponses(jobRequisition.getScreeningCriteria()));
        response.setCriteriaTotalWeight(totalWeight(jobRequisition.getScreeningCriteria()));

        // Nhãn tiếng Việt cho các trường thông tin cơ bản
        response.setSalaryLabel(RequisitionLabels.salary(jobRequisition.getMinSalary(), jobRequisition.getMaxSalary()));
        response.setProbationLabel(RequisitionLabels.probation(jobRequisition.getProbationDays()));
        response.setGenderLabel(RequisitionLabels.gender(jobRequisition.getGender()));
        response.setWorkModelLabel(RequisitionLabels.workModel(jobRequisition.getWorkModel()));
        response.setHiringTypeLabel(RequisitionLabels.hiringType(jobRequisition.getHiringType()));

        // Văn bản nhiều dòng: escape HTML rồi đổi xuống dòng thành <br/> (AG §6.3)
        response.setReasonForHiringHtml(RequisitionLabels.multilineHtml(jobRequisition.getReasonForHiring()));
        response.setJobDescriptionHtml(RequisitionLabels.multilineHtml(jobRequisition.getJobDescription()));
        response.setRequirementDetailsHtml(RequisitionLabels.multilineHtml(jobRequisition.getRequirementDetails()));

        // Mốc thời gian: lúc gửi duyệt, số ngày đã chờ duyệt, còn bao nhiêu ngày tới ngày bắt đầu
        LocalDate today = LocalDate.now(clock);
        response.setSubmittedAt(jobRequisition.getSubmittedAt());
        if (RequisitionAccess.STATUS_PENDING_DIRECTOR.equals(approvalStatus) && jobRequisition.getSubmittedAt() != null) {
            response.setWaitingDays(ChronoUnit.DAYS.between(jobRequisition.getSubmittedAt().toLocalDate(), today));
        }
        if (jobRequisition.getExpectedStartDate() != null) {
            response.setDaysUntilStart(ChronoUnit.DAYS.between(today, jobRequisition.getExpectedStartDate()));
        }

        response.setApprovals(toApprovalResponses(approvalHistory));
        response.setActivityLog(toActivityResponses(activityLogs));

        // Bị trả lại: lý do là ý kiến của lần duyệt mới nhất
        if (RequisitionAccess.STATUS_REJECTED.equals(approvalStatus) && !response.getApprovals().isEmpty()) {
            response.setRejectionReason(response.getApprovals().getFirst().getComments());
        }
        return response;
    }

    // ----- Các hàm nhỏ cho màn chi tiết -----

    /** Chạy thử validator như lúc bấm "Gửi duyệt": không có lỗi nghĩa là bản nháp đã đủ thông tin. */
    private boolean isReadyToSubmit(JobRequisition jobRequisition) {
        RequisitionRequest submissionCheck = toRequestDto(jobRequisition);
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
            ScreeningCriteriaResponse criterionResponse = new ScreeningCriteriaResponse();
            criterionResponse.setCriteriaId(criterion.getCriteriaId());
            criterionResponse.setCriteriaName(criterion.getCriteriaName());
            criterionResponse.setCriteriaType(criterion.getCriteriaType());
            criterionResponse.setCriteriaTypeLabel(RequisitionLabels.criteriaType(criterion.getCriteriaType()));
            criterionResponse.setRequiredValue(criterion.getRequiredValue());
            criterionResponse.setWeight(criterion.getWeight());
            criterionResponse.setIsMandatory(criterion.getIsMandatory());
            responses.add(criterionResponse);
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
