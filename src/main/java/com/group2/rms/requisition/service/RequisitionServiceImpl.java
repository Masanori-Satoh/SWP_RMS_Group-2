package com.group2.rms.requisition.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.util.FieldChanges;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.notification.service.NotificationService;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.repository.RequisitionApprovalRepository;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class RequisitionServiceImpl implements RequisitionService {

    private static final String DEPARTMENT_ACTIVE = "Active";

    private final JobRequisitionRepository requisitions;
    private final DepartmentRepository departments;
    private final RequisitionApprovalRepository approvals;
    private final JobPostingRepository postings;
    private final RequisitionAccess access;
    private final RequisitionValidator validator;
    private final RequisitionMapper mapper;
    private final RequisitionAuditLogger auditLogger;
    private final NotificationService notificationService;
    private final Clock clock;


    @Override
    @Transactional(readOnly = true)
    // Returns requisition details by ID.
    public RequisitionResponse getById(Integer id) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, false);
        access.requireView(currentUser, jobRequisition);

        JobPosting latestPosting = postings.findTopByRequisition_RequisitionIdOrderByJobPostingIdDesc(id).orElse(null);
        boolean hasLaterRound = requisitions.existsByOriginalRequisition_RequisitionId(id);
        List<RequisitionApproval> approvalHistory = requisitions.findApprovalHistory(id);
        return mapper.toDetail(jobRequisition, currentUser, latestPosting, hasLaterRound,
                approvalHistory, auditLogger.history(id));
    }


    @Override
    @Transactional(readOnly = true)
    // Prepares the requisition edit form.
    public RequisitionRequest prepareEditForm(Integer id) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, false);
        access.requireEdit(currentUser, jobRequisition);
        return mapper.toEditForm(jobRequisition);
    }

    @Override
    @Transactional(readOnly = true)
    // Prepares a copy of the source requisition.
    public RequisitionRequest copy(Integer id) {
        User currentUser = access.actor();
        access.requireCreate(currentUser);
        JobRequisition jobRequisition = findRequisitionById(id, false);
        access.requireCopy(currentUser, jobRequisition);

        RequisitionRequest copiedRequest = mapper.toEditForm(jobRequisition);

        JobRequisition root = rootOf(jobRequisition);
        copiedRequest.setOriginalRequisitionId(root.getRequisitionId());
        copiedRequest.setOriginalTitle(root.getTitle());
        copiedRequest.setRecruitmentRound(requisitions.findMaxRoundInChain(root.getRequisitionId()) + 1);
        return copiedRequest;
    }


    @Override
    // Creates a requisition.
    public Integer createRequisition(RequisitionRequest request) {
        User currentUser = access.actor();
        access.requireCreate(currentUser);

        if (access.isDirector(currentUser)) {
            request.setDepartmentId(requireActiveDepartment(request.getDepartmentId()));
        } else {
            Department department = currentUser.getDepartment();
            if (department == null || !DEPARTMENT_ACTIVE.equals(department.getDepartmentStatus())) {
                throw createValidationException("departmentId",
                        "Tài khoản của bạn chưa được gán phòng ban đang hoạt động. Vui lòng liên hệ quản trị viên.");
            }
            request.setDepartmentId(department.getDepartmentId());
        }

        validator.validate(request);

        requireNoOtherDraft(request, null,
                "Bạn đang có một bản nháp cho vị trí này trong phòng ban. Vui lòng hoàn thiện bản nháp hiện có.");

        JobRequisition jobRequisition = new JobRequisition();
        jobRequisition.setHiringManager(currentUser);
        jobRequisition.setCreatedBy(currentUser);
        assignRecruitmentRound(jobRequisition, request, currentUser);
        applyDataToEntity(jobRequisition, request);

        applySavedStatus(jobRequisition, request.getAction(), currentUser);

        requisitions.saveAndFlush(jobRequisition);

        jobRequisition.setRequisitionCode(RequisitionLabels.requisitionCode(jobRequisition.getRequisitionId()));

        boolean directlyApproved = recordDirectApprovalIfNeeded(jobRequisition, currentUser);
        String description = "Tạo yêu cầu: " + jobRequisition.getTitle() + " · " + jobRequisition.getApprovalStatus();
        if (directlyApproved) {
            description = description + " (Giám đốc phê duyệt trực tiếp)";
        }
        auditLogger.log(jobRequisition, currentUser, RequisitionAuditLogger.ACTION_CREATE, null, description);

        if (RequisitionAccess.STATUS_PENDING_DIRECTOR.equals(jobRequisition.getApprovalStatus())) {
            notificationService.notifyRequisitionPendingDirector(jobRequisition, currentUser);
        }

        return jobRequisition.getRequisitionId();
    }


    @Override
    // Updates an existing requisition.
    public void updateRequisition(Integer id, RequisitionRequest request) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, true);
        access.requireEdit(currentUser, jobRequisition);

        if (access.isDirector(currentUser)) {
            request.setDepartmentId(requireActiveDepartment(request.getDepartmentId()));
        } else {
            request.setDepartmentId(jobRequisition.getDepartment().getDepartmentId());
        }

        validator.validate(request);
        requireNoOtherDraft(request, id, "Bạn đang có một bản nháp khác cho vị trí này trong phòng ban.");

        Map<String, String> before = auditLogger.snapshot(jobRequisition);
        applyDataToEntity(jobRequisition, request);

        JobRequisition original = jobRequisition.getOriginalRequisition();
        if (original != null && !isSameChain(request, original)) {
            jobRequisition.setOriginalRequisition(null);
        }

        applySavedStatus(jobRequisition, request.getAction(), currentUser);

        Map<String, String> after = auditLogger.snapshot(jobRequisition);
        List<String> changedKeys = FieldChanges.changedKeys(before, after);
        if (!changedKeys.isEmpty()) {
            jobRequisition.setUpdatedAt(LocalDateTime.now(clock));
            requisitions.saveAndFlush(jobRequisition);
            auditLogger.logChanges(jobRequisition, currentUser, changedKeys, before, after);
        }
        recordDirectApprovalIfNeeded(jobRequisition, currentUser);
        if (RequisitionAccess.STATUS_PENDING_DIRECTOR.equals(jobRequisition.getApprovalStatus())) {
            notificationService.notifyRequisitionPendingDirector(jobRequisition, currentUser);
        }
    }

    @Override
    // Deletes an eligible requisition.
    public void deleteRequisition(Integer id) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, true);
        access.requireEdit(currentUser, jobRequisition);

        if (postings.existsByRequisition_RequisitionId(id)) {
            throw createValidationException("action",
                    "Yêu cầu này đã được liên kết với tin đăng tuyển và không thể xóa.");
        }
        if (requisitions.existsByOriginalRequisition_RequisitionId(id)) {
            throw createValidationException("action",
                    "Yêu cầu này đã có đợt tuyển sau được sao chép từ nó và không thể xóa.");
        }

        auditLogger.log(jobRequisition, currentUser, RequisitionAuditLogger.ACTION_DELETE, null,
                "Đã xóa yêu cầu: " + jobRequisition.getTitle());
        approvals.deleteAll(requisitions.findApprovalHistory(id));
        requisitions.delete(jobRequisition);
        requisitions.flush();
    }

    @Override
    // Processes the approval decision.
    public void decide(Integer id, boolean approved, String comment) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, true);
        access.requireDecide(currentUser, jobRequisition);

        String cleanedComment = RequisitionValidator.cleanMultiline(comment);
        if (!approved && cleanedComment == null) {
            throw createValidationException("comment", "Vui lòng nhập lý do để Hiring Manager chỉnh sửa.");
        }
        if (cleanedComment != null && cleanedComment.length() > RequisitionValidator.COMMENT_CHAR_LIMIT) {
            throw createValidationException("comment",
                    "Nội dung quá " + RequisitionValidator.COMMENT_CHAR_LIMIT + " ký tự");
        }

        String nextStatus = RequisitionAccess.STATUS_REJECTED;
        if (approved) {
            nextStatus = RequisitionAccess.STATUS_APPROVED;
        }
        jobRequisition.setApprovalStatus(nextStatus);
        jobRequisition.setUpdatedAt(LocalDateTime.now(clock));

        RequisitionApproval decision = approvals.save(RequisitionApproval.builder()
                .requisition(jobRequisition)
                .director(currentUser)
                .status(nextStatus)
                .comments(cleanedComment)
                .build());

        requisitions.saveAndFlush(jobRequisition);

        String oldStatus = "Status: " + RequisitionAccess.STATUS_PENDING_DIRECTOR;
        String description = oldStatus + " → " + nextStatus;
        if (cleanedComment != null) {
            description = description + "\nÝ kiến Giám đốc: " + cleanedComment;
        }
        auditLogger.log(jobRequisition, currentUser, RequisitionAuditLogger.ACTION_UPDATE, oldStatus, description);

        if (!approved) {
            notificationService.notifyRequisitionRejected(decision);
        } else {
            notificationService.notifyRequisitionApproved(decision);
        }
    }

    // Requires the source requisition to be copyable.
    private void requireCopyableSource(JobRequisition source) {
        if (!RequisitionAccess.STATUS_APPROVED.equals(source.getApprovalStatus())) {
            throw createValidationException("action", "Chỉ có thể sao chép yêu cầu đã được phê duyệt.");
        }
    }

    // Requires that no matching draft exists.
    private void requireNoOtherDraft(RequisitionRequest request, Integer excludeId, String message) {
        if (RequisitionValidator.isSubmit(request.getAction())) {
            return;
        }
        if (request.getDepartmentId() == null || request.getTitle() == null || request.getTitle().isBlank()) {
            return;
        }
        boolean hasDraft = requisitions.existsDraftByTitleAndDepartment(
                request.getTitle().trim(), request.getDepartmentId(), excludeId);
        if (hasDraft) {
            throw createValidationException("title", message);
        }
    }

    // Assigns the requisition to a recruitment round.
    private void assignRecruitmentRound(JobRequisition jobRequisition, RequisitionRequest request, User currentUser) {
        if (request.getOriginalRequisitionId() == null) {
            return;
        }
        JobRequisition source = requisitions.findById(request.getOriginalRequisitionId())
                .orElseThrow(() -> createValidationException("originalRequisitionId",
                        "Yêu cầu nguồn để sao chép không tồn tại."));
        access.requireView(currentUser, source);
        requireCopyableSource(source);
        if (!isSameChain(request, rootOf(source))) {
            return;
        }
        jobRequisition.setOriginalRequisition(rootOf(source));
    }

    // Returns the root requisition in the chain.
    private JobRequisition rootOf(JobRequisition jobRequisition) {
        if (jobRequisition.getOriginalRequisition() != null) {
            return jobRequisition.getOriginalRequisition();
        }
        return jobRequisition;
    }

    // Checks whether the request belongs to the same requisition chain.
    private boolean isSameChain(RequisitionRequest request, JobRequisition root) {
        Integer rootDepartmentId = root.getDepartment() == null ? null : root.getDepartment().getDepartmentId();
        return Objects.equals(request.getDepartmentId(), rootDepartmentId)
                && isSameTitle(request.getTitle(), root.getTitle());
    }

    // Checks whether two titles match.
    private boolean isSameTitle(String first, String second) {
        return first != null && second != null && first.trim().equalsIgnoreCase(second.trim());
    }

    // Requires an active department.
    private Integer requireActiveDepartment(Integer departmentId) {
        Department department = null;
        if (departmentId != null) {
            department = departments.findById(departmentId).orElse(null);
        }
        if (department == null || !DEPARTMENT_ACTIVE.equals(department.getDepartmentStatus())) {
            throw createValidationException("departmentId", "Vui lòng chọn phòng ban đang hoạt động.");
        }
        return department.getDepartmentId();
    }


    // Applies the status selected by the form action.
    private void applySavedStatus(JobRequisition jobRequisition, String action, User currentUser) {
        if (!RequisitionValidator.isSubmit(action)) {
            jobRequisition.setApprovalStatus(RequisitionAccess.STATUS_DRAFT);
            return;
        }
        jobRequisition.setSubmittedAt(LocalDateTime.now(clock));
        if (access.isDirector(currentUser)) {
            jobRequisition.setApprovalStatus(RequisitionAccess.STATUS_APPROVED);
        } else {
            jobRequisition.setApprovalStatus(RequisitionAccess.STATUS_PENDING_DIRECTOR);
        }
    }

    // Records direct approval when required.
    private boolean recordDirectApprovalIfNeeded(JobRequisition jobRequisition, User currentUser) {
        boolean isApproved = RequisitionAccess.STATUS_APPROVED.equals(jobRequisition.getApprovalStatus());
        if (!isApproved || !access.isDirector(currentUser)) {
            return false;
        }
        RequisitionApproval approval = RequisitionApproval.builder()
                .requisition(jobRequisition)
                .director(currentUser)
                .status(RequisitionAccess.STATUS_APPROVED)
                .comments("Giám đốc tạo và phê duyệt trực tiếp.")
                .build();
        approvals.save(approval);
        notificationService.notifyRequisitionApproved(approval);
        return true;
    }

    // Copies request data to the requisition entity.
    private void applyDataToEntity(JobRequisition jobRequisition, RequisitionRequest request) {
        jobRequisition.setTitle(request.getTitle());
        jobRequisition.setRecruitmentRound(request.getRecruitmentRound());
        jobRequisition.setDepartment(findDepartment(request.getDepartmentId()));
        jobRequisition.setNumberOfPositions(request.getNumberOfPositions());
        jobRequisition.setEmploymentType(request.getEmploymentType());
        jobRequisition.setMinSalary(request.getMinSalary());
        jobRequisition.setMaxSalary(request.getMaxSalary());
        jobRequisition.setGender(request.getGender());
        jobRequisition.setWorkLocation(request.getWorkLocation());
        jobRequisition.setWorkModel(request.getWorkModel());
        jobRequisition.setProbationPeriod(request.getProbationPeriod());
        jobRequisition.setReasonForHiring(request.getReasonForHiring());
        jobRequisition.setJobDescription(Objects.requireNonNullElse(request.getJobDescription(), ""));
        jobRequisition.setRequirementDetails(Objects.requireNonNullElse(request.getRequirementDetails(), ""));
    }

    // Finds an active department by ID.
    private Department findDepartment(Integer departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departments.findById(departmentId)
                .orElseThrow(() -> createValidationException("departmentId", "Phòng ban không tồn tại."));
    }

    // Finds a requisition by ID with the requested lock.
    private JobRequisition findRequisitionById(Integer id, boolean lockForUpdate) {
        if (lockForUpdate) {
            return requisitions.findForUpdate(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tuyển dụng."));
        }
        return requisitions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tuyển dụng."));
    }

    // Creates a validation exception for the field.
    private RequisitionValidationException createValidationException(String field, String message) {
        return new RequisitionValidationException(Map.of(field, message));
    }
}
