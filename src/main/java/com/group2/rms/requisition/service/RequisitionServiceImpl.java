package com.group2.rms.requisition.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.util.FieldChanges;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.entity.ScreeningCriteria;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ Yêu cầu tuyển dụng (Job Requisition): tạo, sửa, xóa, duyệt, rút lại, sao chép, xem chi tiết.
 *
 * Các phần phụ đã tách ra file riêng để file này chỉ còn luồng nghiệp vụ:
 * - màn danh sách (lọc, tìm kiếm, phân trang): RequisitionListService
 * - entity → DTO và cờ quyền cho template: RequisitionMapper
 * - ghi / đọc audit log: RequisitionAuditLogger
 * - luật phân quyền: RequisitionAccess; luật dữ liệu form: RequisitionValidator
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

    // ===================================================================================
    // ĐỌC: chi tiết, form sửa, form sao chép
    // ===================================================================================

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
    public RequisitionRequest getRequestDtoById(Integer id) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, false);
        // Rule: Chỉ người có quyền sửa (HM sở hữu yêu cầu khi còn là Draft) mới lấy được DTO form
        access.requireEdit(currentUser, jobRequisition);
        return mapper.toRequestDto(jobRequisition);
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

        RequisitionRequest copiedDto = mapper.toRequestDto(jobRequisition);
        // Reset criteria IDs để khi tạo mới sẽ sinh bản ghi mới
        for (ScreeningCriteriaRequest criterion : copiedDto.getScreeningCriteria()) {
            criterion.setCriteriaId(null);
        }

        // Đợt tiếp theo trong chuỗi của phiếu gốc; chỉ để hiển thị, server tính lại khi lưu
        // (đổi tên vị trí thì thành yêu cầu mới, quay về đợt 1)
        JobRequisition root = rootOf(jobRequisition);
        copiedDto.setOriginalRequisitionId(root.getRequisitionId());
        copiedDto.setOriginalTitle(root.getTitle());
        copiedDto.setRecruitmentRound(requisitions.findMaxRoundInChain(root.getRequisitionId()) + 1);
        return copiedDto;
    }

    // ===================================================================================
    // GHI: tạo, sửa, xóa
    // ===================================================================================

    @Override
    public Integer createRequisition(RequisitionRequest requestDto) {
        User currentUser = access.actor();
        // Rule: Kiểm tra quyền tạo yêu cầu
        access.requireCreate(currentUser);

        if (access.isDirector(currentUser)) {
            // Rule: Giám đốc không thuộc phòng ban nào: tự chọn phòng ban cho yêu cầu trên form
            requestDto.setDepartmentId(requireActiveDepartment(requestDto.getDepartmentId()));
        } else {
            // Rule: Yêu cầu luôn thuộc phòng ban của người tạo (không lấy từ form).
            // Tài khoản chưa được gán phòng ban, hoặc phòng ban đã ngừng hoạt động, thì không tạo được
            // (cột DepartmentId là NOT NULL, báo lỗi ở đây thay vì để DB báo lỗi).
            Department department = currentUser.getDepartment();
            if (department == null || !DEPARTMENT_ACTIVE.equals(department.getDepartmentStatus())) {
                throw createValidationException("departmentId",
                        "Tài khoản của bạn chưa được gán phòng ban đang hoạt động. Vui lòng liên hệ quản trị viên.");
            }
            requestDto.setDepartmentId(department.getDepartmentId());
        }

        validator.validate(requestDto);
        // Hướng 2: chặn trùng lặp đơn Draft cho cùng vị trí trong phòng ban
        requireNoOtherDraft(requestDto, null,
                "Bạn đang có một bản nháp cho vị trí này trong phòng ban. Vui lòng hoàn thiện bản nháp hiện có.");

        // Rule: Tiêu chí mới không được chứa ID đã tồn tại
        for (ScreeningCriteriaRequest criteriaRow : requestDto.getScreeningCriteria()) {
            if (criteriaRow.getCriteriaId() != null) {
                throw createValidationException("screeningCriteria", "Tiêu chí mới không được tham chiếu bản ghi có sẵn.");
            }
        }

        JobRequisition jobRequisition = new JobRequisition();
        jobRequisition.setHiringManager(currentUser);
        jobRequisition.setCreatedBy(currentUser);
        jobRequisition.setScreeningCriteria(new ArrayList<>());
        assignRecruitmentRound(jobRequisition, requestDto.getOriginalRequisitionId(), requestDto.getTitle(), currentUser);
        applyDataToEntity(jobRequisition, requestDto);

        for (ScreeningCriteriaRequest criteriaRow : requestDto.getScreeningCriteria()) {
            if (!RequisitionValidator.isBlankCriteria(criteriaRow)) {
                jobRequisition.getScreeningCriteria().add(buildCriterionEntity(jobRequisition, criteriaRow));
            }
        }

        // Rule: Xác định trạng thái ban đầu (Draft, Pending_Director hoặc Approved nếu Giám đốc gửi)
        applySavedStatus(jobRequisition, requestDto.getAction(), currentUser);

        requisitions.saveAndFlush(jobRequisition);

        // Rule: Mã yêu cầu sinh từ RequisitionId, nên chỉ có sau khi DB cấp id ở lần lưu đầu tiên.
        // Gán vào entity đang được quản lý trong transaction, Hibernate tự UPDATE khi transaction kết thúc.
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
    public void updateRequisition(Integer id, RequisitionRequest requestDto) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, true);
        // Rule: Chỉ cho phép chỉnh sửa khi còn là bản nháp và đúng người sở hữu
        access.requireEdit(currentUser, jobRequisition);

        if (access.isDirector(currentUser)) {
            // Rule: Giám đốc sửa bản nháp của mình thì được chọn lại phòng ban
            requestDto.setDepartmentId(requireActiveDepartment(requestDto.getDepartmentId()));
        } else {
            // Rule: Sửa yêu cầu không đổi phòng ban: giữ phòng ban đã lưu
            // (Admin sửa phiếu của HM thì phiếu vẫn thuộc phòng ban của HM, không chuyển sang phòng ban của Admin)
            requestDto.setDepartmentId(jobRequisition.getDepartment().getDepartmentId());
        }

        validator.validate(requestDto);
        requireNoOtherDraft(requestDto, id, "Bạn đang có một bản nháp khác cho vị trí này trong phòng ban.");

        Map<String, String> before = auditLogger.snapshot(jobRequisition);
        synchronizeCriteria(jobRequisition, requestDto.getScreeningCriteria());
        applyDataToEntity(jobRequisition, requestDto);

        // Rule: Bản sao (đợt > 1) bị đổi tên vị trí thì thành yêu cầu mới: bỏ phiếu gốc, quay về đợt 1
        JobRequisition original = jobRequisition.getOriginalRequisition();
        if (original != null && !isSameTitle(jobRequisition.getTitle(), original.getTitle())) {
            jobRequisition.setOriginalRequisition(null);
            jobRequisition.setRecruitmentRound(1);
        }

        // Rule: Cập nhật trạng thái khi nộp lại hoặc tiếp tục lưu nháp
        applySavedStatus(jobRequisition, requestDto.getAction(), currentUser);

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
        // Rule: Không cho phép xóa phiếu gốc khi đã có đợt tuyển sau sao chép từ nó
        // (nếu xóa, các đợt sau mất phiếu gốc và vi phạm khóa ngoại FK_JobRequisition_Original)
        if (requisitions.existsByOriginalRequisition_RequisitionId(id)) {
            throw createValidationException("action", "Yêu cầu này đã có đợt tuyển sau được sao chép từ nó và không thể xóa.");
        }

        auditLogger.log(jobRequisition, currentUser, RequisitionAuditLogger.ACTION_DELETE, null,
                "Đã xóa yêu cầu: " + jobRequisition.getTitle());
        approvals.deleteAll(approvals.findByRequisition_RequisitionIdOrderByApprovalDateDesc(id));
        requisitions.delete(jobRequisition);
        requisitions.flush();
    }

    // ===================================================================================
    // CHUYỂN TRẠNG THÁI: duyệt / trả lại, rút lại
    // ===================================================================================

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
    }

    // ===================================================================================
    // CÁC HÀM NHỎ DÙNG CHUNG
    // ===================================================================================

    /** Hướng 1: chỉ sao chép được yêu cầu đã xong vòng duyệt (không phải Draft / Pending_Director). */
    private void requireCopyableSource(JobRequisition source) {
        if (!RequisitionAccess.isFinishedStatus(source.getApprovalStatus())) {
            throw createValidationException("action", "Chỉ có thể sao chép yêu cầu đã hoàn tất phê duyệt hoặc bị từ chối.");
        }
    }

    /**
     * Rule: Một phòng ban không có hai bản nháp cùng tên vị trí.
     *
     * @param excludeId id của chính yêu cầu đang sửa (null khi tạo mới)
     */
    private void requireNoOtherDraft(RequisitionRequest requestDto, Integer excludeId, String message) {
        if (requestDto.getDepartmentId() == null || requestDto.getTitle() == null || requestDto.getTitle().isBlank()) {
            return;
        }
        boolean hasDraft = requisitions.existsDraftByTitleAndDepartment(
                requestDto.getTitle().trim(), requestDto.getDepartmentId(), excludeId);
        if (hasDraft) {
            throw createValidationException("title", message);
        }
    }

    private void synchronizeCriteria(JobRequisition jobRequisition, List<ScreeningCriteriaRequest> inputCriteriaList) {
        // Bỏ các dòng tiêu chí để trống hoàn toàn
        List<ScreeningCriteriaRequest> validCriteriaList = new ArrayList<>();
        for (ScreeningCriteriaRequest criteriaRow : inputCriteriaList) {
            if (!RequisitionValidator.isBlankCriteria(criteriaRow)) {
                validCriteriaList.add(criteriaRow);
            }
        }

        Map<Integer, ScreeningCriteria> existingCriteriaMap = new HashMap<>();
        for (ScreeningCriteria criterion : jobRequisition.getScreeningCriteria()) {
            existingCriteriaMap.put(criterion.getCriteriaId(), criterion);
        }

        // Rule: Chỉ sửa được tiêu chí của chính yêu cầu này; gom id các tiêu chí được giữ lại
        Set<Integer> keptCriteriaIds = new HashSet<>();
        for (ScreeningCriteriaRequest criteriaRow : validCriteriaList) {
            Integer criteriaId = criteriaRow.getCriteriaId();
            if (criteriaId == null) {
                continue;
            }
            if (!existingCriteriaMap.containsKey(criteriaId)) {
                throw createValidationException("screeningCriteria", "Một tiêu chí không thuộc về yêu cầu này.");
            }
            keptCriteriaIds.add(criteriaId);
        }

        boolean hasChanged = jobRequisition.getScreeningCriteria().removeIf(c -> !keptCriteriaIds.contains(c.getCriteriaId()));

        // Giải phóng tên cũ tránh xung đột unique name trước khi chèn mới
        for (ScreeningCriteriaRequest criteriaRow : validCriteriaList) {
            ScreeningCriteria existingCriterion = existingCriteriaMap.get(criteriaRow.getCriteriaId());
            if (existingCriterion != null && !Objects.equals(existingCriterion.getCriteriaName(), criteriaRow.getCriteriaName())) {
                existingCriterion.setCriteriaName("__rename_" + UUID.randomUUID());
                hasChanged = true;
            }
        }
        if (hasChanged) {
            requisitions.flush();
        }

        for (ScreeningCriteriaRequest criteriaRow : validCriteriaList) {
            ScreeningCriteria existingCriterion = existingCriteriaMap.get(criteriaRow.getCriteriaId());
            if (existingCriterion == null) {
                jobRequisition.getScreeningCriteria().add(buildCriterionEntity(jobRequisition, criteriaRow));
            } else {
                copyCriterionProperties(existingCriterion, criteriaRow);
            }
        }
    }

    /**
     * Rule: Đợt 1 không có phiếu gốc; đợt sau trỏ về phiếu đợt 1 và lấy số đợt kế tiếp trong chuỗi
     * (CK_JobRequisition_RoundOrigin, UX_JobRequisition_Original_Round). Số đợt client gửi lên bị bỏ qua.
     * Sao chép nhưng đổi tên vị trí thì coi là yêu cầu mới: đợt 1, không có phiếu gốc.
     */
    private void assignRecruitmentRound(JobRequisition jobRequisition, Integer sourceRequisitionId, String title, User currentUser) {
        if (sourceRequisitionId == null) {
            jobRequisition.setRecruitmentRound(1);
            return;
        }
        JobRequisition source = requisitions.findById(sourceRequisitionId)
                .orElseThrow(() -> createValidationException("originalRequisitionId", "Yêu cầu nguồn để sao chép không tồn tại."));
        access.requireView(currentUser, source);
        requireCopyableSource(source);
        if (!isSameTitle(title, rootOf(source).getTitle())) {
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

    /**
     * Rule: Trạng thái khi lưu form tạo / sửa.
     * Lưu nháp → Draft. Gửi → Pending_Director; riêng Giám đốc gửi thì không cần ai duyệt nữa → Approved.
     */
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

    /**
     * Giám đốc gửi yêu cầu của mình thì lưu luôn một bản ghi phê duyệt (người duyệt là chính Giám đốc),
     * để lịch sử duyệt ở trang chi tiết vẫn có người duyệt và ngày duyệt như phiếu thường.
     * Trả về true nếu đã ghi.
     */
    private boolean recordDirectApprovalIfNeeded(JobRequisition jobRequisition, User currentUser) {
        boolean isApproved = RequisitionAccess.STATUS_APPROVED.equals(jobRequisition.getApprovalStatus());
        if (!isApproved || !access.isDirector(currentUser)) {
            return false;
        }
        approvals.save(RequisitionApproval.builder()
                .requisition(jobRequisition)
                .director(currentUser)
                .status(RequisitionAccess.STATUS_APPROVED)
                .comments("Giám đốc tạo và phê duyệt trực tiếp.")
                .build());
        return true;
    }

    private ScreeningCriteria buildCriterionEntity(JobRequisition jobRequisition, ScreeningCriteriaRequest criteriaDto) {
        ScreeningCriteria criterion = new ScreeningCriteria();
        criterion.setRequisition(jobRequisition);
        copyCriterionProperties(criterion, criteriaDto);
        return criterion;
    }

    private void copyCriterionProperties(ScreeningCriteria targetEntity, ScreeningCriteriaRequest sourceDto) {
        targetEntity.setCriteriaName(sourceDto.getCriteriaName());
        targetEntity.setCriteriaType(sourceDto.getCriteriaType());
        targetEntity.setRequiredValue(sourceDto.getRequiredValue());
        targetEntity.setWeight(sourceDto.getWeight());
        targetEntity.setIsMandatory(Boolean.TRUE.equals(sourceDto.getIsMandatory()));
    }

    private void applyDataToEntity(JobRequisition jobRequisition, RequisitionRequest requestDto) {
        // Tên vị trí luôn có giá trị (validator bắt buộc cả khi lưu nháp)
        jobRequisition.setTitle(requestDto.getTitle());
        jobRequisition.setHiringType(requestDto.getHiringType());
        jobRequisition.setDepartment(findDepartment(requestDto.getDepartmentId()));
        jobRequisition.setNumberOfPositions(requestDto.getNumberOfPositions());
        jobRequisition.setEmploymentType(requestDto.getEmploymentType());
        jobRequisition.setMinSalary(requestDto.getMinSalary());
        jobRequisition.setMaxSalary(requestDto.getMaxSalary());
        jobRequisition.setGender(requestDto.getGender());
        jobRequisition.setWorkLocation(requestDto.getWorkLocation());
        jobRequisition.setWorkModel(requestDto.getWorkModel());
        jobRequisition.setProbationDays(requestDto.getProbationDays());
        jobRequisition.setReasonForHiring(requestDto.getReasonForHiring());
        // Rule: Bản nháp được phép để trống mô tả / yêu cầu, nhưng 2 cột này NOT NULL trong DB
        // nên lưu chuỗi rỗng thay cho null (khi gửi duyệt, validator đã bắt buộc nhập)
        jobRequisition.setJobDescription(Objects.requireNonNullElse(requestDto.getJobDescription(), ""));
        jobRequisition.setRequirementDetails(Objects.requireNonNullElse(requestDto.getRequirementDetails(), ""));
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
