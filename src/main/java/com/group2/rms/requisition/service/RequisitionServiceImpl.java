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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/*
 * SERVICE: nghiệp vụ yêu cầu tuyển dụng (tạo, sửa, xóa, duyệt, rút lại, sao chép, xem chi tiết).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RequisitionServiceImpl implements RequisitionService {

    /** Giá trị DepartmentStatus của phòng ban đang hoạt động. */
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

    // ----- Đọc: chi tiết, form sửa, form sao chép -----

    @Override
    @Transactional(readOnly = true)
    public RequisitionResponse getById(Integer id) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, false);
        // Rule: Kiểm tra quyền xem chi tiết yêu cầu
        access.requireView(currentUser, jobRequisition);

        JobPosting latestPosting = postings.findTopByRequisition_RequisitionIdOrderByJobPostingIdDesc(id).orElse(null);
        boolean hasLaterRound = requisitions.existsByOriginalRequisition_RequisitionId(id);
        List<RequisitionApproval> approvalHistory = approvals.findByRequisition_RequisitionIdOrderByApprovalDateDesc(id);
        return mapper.toDetail(jobRequisition, currentUser, latestPosting, hasLaterRound,
                approvalHistory, auditLogger.history(id));
    }

    @Override
    @Transactional(readOnly = true)
    public RequisitionRequest prepareEditForm(Integer id) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, false);
        // Rule: Chỉ người có quyền sửa (HM sở hữu yêu cầu khi còn là Draft) mới lấy được DTO form
        access.requireEdit(currentUser, jobRequisition);
        return mapper.toEditForm(jobRequisition);
    }

    @Override
    @Transactional(readOnly = true)
    public RequisitionRequest copy(Integer id) {
        User currentUser = access.actor();
        // Rule: Người sao chép phải có quyền tạo mới yêu cầu
        access.requireCreate(currentUser);
        JobRequisition jobRequisition = findRequisitionById(id, false);
        access.requireView(currentUser, jobRequisition);
        requireCopyableSource(jobRequisition);

        RequisitionRequest copiedRequest = mapper.toEditForm(jobRequisition);

        // Đợt kế tiếp chỉ để hiển thị; server tính lại khi lưu
        JobRequisition root = rootOf(jobRequisition);
        copiedRequest.setOriginalRequisitionId(root.getRequisitionId());
        copiedRequest.setOriginalTitle(root.getTitle());
        copiedRequest.setRecruitmentRound(requisitions.findMaxRoundInChain(root.getRequisitionId()) + 1);
        return copiedRequest;
    }

    // ----- Ghi: tạo, sửa, xóa -----

    @Override
    public Integer createRequisition(RequisitionRequest request) {
        User currentUser = access.actor();
        // Rule: Kiểm tra quyền tạo yêu cầu
        access.requireCreate(currentUser);

        if (access.isDirector(currentUser)) {
            // Rule: Giám đốc không thuộc phòng ban nào: tự chọn phòng ban cho yêu cầu trên form
            request.setDepartmentId(requireActiveDepartment(request.getDepartmentId()));
        } else {
            // Rule: yêu cầu thuộc phòng ban đang hoạt động của người tạo, không lấy từ form
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

        // Rule: Xác định trạng thái ban đầu (Draft, Pending_Director hoặc Approved nếu Giám đốc gửi)
        applySavedStatus(jobRequisition, request.getAction(), currentUser);

        requisitions.saveAndFlush(jobRequisition);

        // Mã sinh từ id nên gán sau lần lưu đầu; Hibernate tự UPDATE khi kết thúc transaction
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
    public void updateRequisition(Integer id, RequisitionRequest request) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, true);
        // Rule: Chỉ cho phép chỉnh sửa khi còn là bản nháp và đúng người sở hữu
        access.requireEdit(currentUser, jobRequisition);

        if (access.isDirector(currentUser)) {
            // Rule: Giám đốc sửa bản nháp của mình thì được chọn lại phòng ban
            request.setDepartmentId(requireActiveDepartment(request.getDepartmentId()));
        } else {
            // Rule: Sửa yêu cầu không đổi phòng ban: giữ phòng ban đã lưu
            request.setDepartmentId(jobRequisition.getDepartment().getDepartmentId());
        }

        validator.validate(request);
        requireNoOtherDraft(request, id, "Bạn đang có một bản nháp khác cho vị trí này trong phòng ban.");

        Map<String, String> before = auditLogger.snapshot(jobRequisition);
        applyDataToEntity(jobRequisition, request);

        // Rule: bản sao đổi tên vị trí hoặc phòng ban thì thành yêu cầu mới ở đợt 1
        JobRequisition original = jobRequisition.getOriginalRequisition();
        if (original != null && !isSameChain(request, original)) {
            jobRequisition.setOriginalRequisition(null);
            jobRequisition.setRecruitmentRound(1);
        }

        // Rule: Cập nhật trạng thái khi nộp lại hoặc tiếp tục lưu nháp
        applySavedStatus(jobRequisition, request.getAction(), currentUser);

        // Rule: Không đổi gì thì không lưu và không ghi audit
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
    public void deleteRequisition(Integer id) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, true);
        access.requireEdit(currentUser, jobRequisition);

        // Rule: Không cho phép xóa yêu cầu tuyển dụng nếu đã có tin tuyển dụng liên kết
        if (postings.existsByRequisition_RequisitionId(id)) {
            throw createValidationException("action", "Yêu cầu này đã được liên kết với tin đăng tuyển và không thể xóa.");
        }
        // Rule: không xóa phiếu gốc đã có đợt sau (FK_JobRequisition_Original)
        if (requisitions.existsByOriginalRequisition_RequisitionId(id)) {
            throw createValidationException("action", "Yêu cầu này đã có đợt tuyển sau được sao chép từ nó và không thể xóa.");
        }

        auditLogger.log(jobRequisition, currentUser, RequisitionAuditLogger.ACTION_DELETE, null,
                "Đã xóa yêu cầu: " + jobRequisition.getTitle());
        approvals.deleteAll(approvals.findByRequisition_RequisitionIdOrderByApprovalDateDesc(id));
        requisitions.delete(jobRequisition);
        requisitions.flush();
    }

    // ----- Chuyển trạng thái: duyệt / trả lại, rút lại -----

    @Override
    public void decide(Integer id, boolean approved, String comment) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, true);
        access.requireView(currentUser, jobRequisition);

        // Rule: Chỉ Giám đốc (Director) mới có quyền duyệt hoặc từ chối yêu cầu đang chờ
        if (!access.canDecide(currentUser, jobRequisition)) {
            throw new AccessDeniedException("Chỉ Giám đốc mới có quyền quyết định phê duyệt yêu cầu.");
        }

        String cleanedComment = RequisitionValidator.cleanString(comment);
        // Rule: Khi từ chối bắt buộc phải có ý kiến nhận xét / lý do trả về
        if (!approved && cleanedComment == null) {
            throw createValidationException("comment", "Vui lòng nhập lý do để Hiring Manager chỉnh sửa.");
        }
        if (cleanedComment != null && cleanedComment.length() > RequisitionValidator.COMMENT_CHAR_LIMIT) {
            throw createValidationException("comment", "Nội dung quá " + RequisitionValidator.COMMENT_CHAR_LIMIT + " ký tự");
        }

        String nextStatus = RequisitionAccess.STATUS_REJECTED;
        if (approved) {
            nextStatus = RequisitionAccess.STATUS_APPROVED;
        }
        jobRequisition.setApprovalStatus(nextStatus);
        // Dashboard HR tính số ngày chờ tạo tin từ mốc này
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

    @Override
    public void withdraw(Integer id) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, true);
        access.requireView(currentUser, jobRequisition);

        // Rule: Chỉ người tạo yêu cầu mới được rút lại yêu cầu đang chờ duyệt
        if (!access.canWithdraw(currentUser, jobRequisition)) {
            throw new AccessDeniedException("Chỉ người tạo yêu cầu mới có quyền rút lại yêu cầu đang chờ duyệt.");
        }

        // Rule: Chuyển trạng thái từ Pending_Director về Draft
        jobRequisition.setApprovalStatus(RequisitionAccess.STATUS_DRAFT);
        requisitions.saveAndFlush(jobRequisition);
        String oldStatus = "Status: " + RequisitionAccess.STATUS_PENDING_DIRECTOR;
        auditLogger.log(jobRequisition, currentUser, RequisitionAuditLogger.ACTION_UPDATE, oldStatus,
                oldStatus + " → " + RequisitionAccess.STATUS_DRAFT + " (rút lại)");
        notificationService.markRequisitionPendingRead(id);
        notificationService.notifyRequisitionWithdrawn(jobRequisition, currentUser);
    }

    // ----- Hàm nhỏ dùng chung -----

    /** Rule: chỉ sao chép yêu cầu đã duyệt; bị từ chối thì sửa và gửi lại chính nó. */
    private void requireCopyableSource(JobRequisition source) {
        if (!RequisitionAccess.STATUS_APPROVED.equals(source.getApprovalStatus())) {
            throw createValidationException("action", "Chỉ có thể sao chép yêu cầu đã được phê duyệt.");
        }
    }

    /** Rule: khi lưu nháp, một phòng ban không có hai bản nháp cùng tên vị trí (excludeId = yêu cầu đang sửa). */
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

    /** Rule: bản sao cùng tên vị trí và phòng ban thì nối vào phiếu đợt 1, số đợt = max + 1; khác thì đợt 1. */
    private void assignRecruitmentRound(JobRequisition jobRequisition, RequisitionRequest request, User currentUser) {
        if (request.getOriginalRequisitionId() == null) {
            jobRequisition.setRecruitmentRound(1);
            return;
        }
        JobRequisition source = requisitions.findById(request.getOriginalRequisitionId())
                .orElseThrow(() -> createValidationException("originalRequisitionId", "Yêu cầu nguồn để sao chép không tồn tại."));
        access.requireView(currentUser, source);
        requireCopyableSource(source);
        if (!isSameChain(request, rootOf(source))) {
            jobRequisition.setRecruitmentRound(1);
            return;
        }
        // Khóa phiếu gốc để hai lần tạo đợt mới đồng thời không lấy trùng số đợt
        JobRequisition root = findRequisitionById(rootOf(source).getRequisitionId(), true);
        jobRequisition.setOriginalRequisition(root);
        jobRequisition.setRecruitmentRound(requisitions.findMaxRoundInChain(root.getRequisitionId()) + 1);
    }

    private JobRequisition rootOf(JobRequisition jobRequisition) {
        if (jobRequisition.getOriginalRequisition() != null) {
            return jobRequisition.getOriginalRequisition();
        }
        return jobRequisition;
    }

    /** Cùng chuỗi đợt khi cùng phòng ban và cùng tên vị trí (bỏ qua hoa/thường, khoảng trắng đầu/cuối). */
    private boolean isSameChain(RequisitionRequest request, JobRequisition root) {
        Integer rootDepartmentId = root.getDepartment() == null ? null : root.getDepartment().getDepartmentId();
        return Objects.equals(request.getDepartmentId(), rootDepartmentId)
                && isSameTitle(request.getTitle(), root.getTitle());
    }

    /** Hai tên vị trí coi là giống nhau khi khác nhau chỉ ở khoảng trắng đầu/cuối hoặc chữ hoa/thường. */
    private boolean isSameTitle(String first, String second) {
        return first != null && second != null && first.trim().equalsIgnoreCase(second.trim());
    }

    /** Phòng ban Giám đốc chọn trên form: bắt buộc, phải tồn tại và đang hoạt động. Trả về id phòng ban. */
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

    /** Rule: lưu nháp → Draft; gửi → Pending_Director, Giám đốc gửi → Approved. */
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

    /** Giám đốc tự gửi thì lưu bản ghi duyệt của chính mình và báo HR tạo tin; trả về true nếu đã ghi. */
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

    private void applyDataToEntity(JobRequisition jobRequisition, RequisitionRequest request) {
        // Tên vị trí luôn có giá trị (validator bắt buộc cả khi lưu nháp)
        jobRequisition.setTitle(request.getTitle());
        jobRequisition.setHiringType(request.getHiringType());
        jobRequisition.setDepartment(findDepartment(request.getDepartmentId()));
        jobRequisition.setNumberOfPositions(request.getNumberOfPositions());
        jobRequisition.setEmploymentType(request.getEmploymentType());
        jobRequisition.setMinSalary(request.getMinSalary());
        jobRequisition.setMaxSalary(request.getMaxSalary());
        jobRequisition.setGender(request.getGender());
        jobRequisition.setWorkLocation(request.getWorkLocation());
        jobRequisition.setWorkModel(request.getWorkModel());
        jobRequisition.setProbationDays(request.getProbationDays());
        jobRequisition.setReasonForHiring(request.getReasonForHiring());
        // Rule: nháp được để trống mô tả / yêu cầu, lưu chuỗi rỗng vì cột NOT NULL
        jobRequisition.setJobDescription(Objects.requireNonNullElse(request.getJobDescription(), ""));
        jobRequisition.setRequirementDetails(Objects.requireNonNullElse(request.getRequirementDetails(), ""));
    }

    /** Phòng ban theo id; id null thì trả về null, id không tồn tại thì báo lỗi ở ô phòng ban. */
    private Department findDepartment(Integer departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departments.findById(departmentId)
                .orElseThrow(() -> createValidationException("departmentId", "Phòng ban không tồn tại."));
    }

    private JobRequisition findRequisitionById(Integer id, boolean lockForUpdate) {
        if (lockForUpdate) {
            return requisitions.findForUpdate(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tuyển dụng."));
        }
        return requisitions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tuyển dụng."));
    }

    private RequisitionValidationException createValidationException(String field, String message) {
        return new RequisitionValidationException(Map.of(field, message));
    }
}
