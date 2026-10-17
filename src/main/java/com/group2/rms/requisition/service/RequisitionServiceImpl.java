package com.group2.rms.requisition.service;

import com.group2.rms.admin.dto.ActivityLogResponse;
import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.requisition.dto.ApprovalResponse;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaResponse;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.repository.RequisitionApprovalRepository;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý nghiệp vụ Yêu cầu tuyển dụng (Job Requisition).
 * Tuân thủ quy chuẩn: Naming rõ nghĩa, không biến viết tắt vô nghĩa, có comment Rule trước logic quan trọng.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RequisitionServiceImpl implements RequisitionService {

    private final JobRequisitionRepository requisitions;
    private final DepartmentRepository departments;
    private final RequisitionApprovalRepository approvals;
    private final JobPostingRepository postings;
    private final AuditLogRepository audit;
    private final RequisitionAccess access;
    private final RequisitionValidator validator;
    private final NotificationService notificationService;

    private static final Set<String> STATUSES_NOT_COPYABLE =
            Set.of(RequisitionAccess.STATUS_DRAFT, RequisitionAccess.STATUS_PENDING_DIRECTOR);

    /**
     * Rule: Phân quyền phạm vi dữ liệu theo Role người dùng:
     * - Hiring Manager: chỉ xem các yêu cầu do chính mình tạo
     * - Director: chỉ xem các yêu cầu đã nộp (khác Draft)
     * - HR: chỉ xem các yêu cầu đã được phê duyệt (Approved)
     */
    private Specification<JobRequisition> scope(User currentUser) {
        return (root, query, criteriaBuilder) -> switch (access.role(currentUser)) {
            case "Hiring Manager" -> criteriaBuilder.equal(root.get("hiringManager").get("userId"), currentUser.getUserId());
            case "Director" -> criteriaBuilder.notEqual(root.get("approvalStatus"), "Draft");
            case "HR" -> criteriaBuilder.equal(root.get("approvalStatus"), "Approved");
            default -> criteriaBuilder.conjunction();
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequisitionResponse> search(int page, int size, String query, Integer departmentId, String type, String status) {
        return search(page, size, query, departmentId, type, status, "newest");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequisitionResponse> search(int page, int size, String query, Integer departmentId, String type, String status, String order) {
        return search(page, size, query, departmentId, null, type, status, order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequisitionResponse> search(int page, int size, String query, Integer departmentId, String position, String type, String status, String order) {
        User currentUser = access.actor();
        Specification<JobRequisition> filter = scope(currentUser);

        String searchTerm = Objects.toString(query, "").trim().toLowerCase(Locale.ROOT);
        if (searchTerm.length() > 120) {
            searchTerm = searchTerm.substring(0, 120);
        }

        String searchPattern = "%" + searchTerm.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("[", "\\[") + "%";
        if (!searchTerm.isEmpty()) {
            filter = filter.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), searchPattern, '\\'),
                    cb.like(cb.lower(root.join("department", jakarta.persistence.criteria.JoinType.LEFT).get("departmentName")), searchPattern, '\\')));
        }
        if (departmentId != null) {
            filter = filter.and((root, cq, cb) -> cb.equal(root.get("department").get("departmentId"), departmentId));
        }
        if (position != null && !position.isBlank()) {
            filter = filter.and((root, cq, cb) -> cb.equal(root.get("title"), position));
        }
        if (type != null && !type.isBlank()) {
            filter = filter.and((root, cq, cb) -> cb.equal(root.get("employmentType"), type));
        }
        if (status != null && !status.isBlank()) {
            filter = filter.and((root, cq, cb) -> cb.equal(root.get("approvalStatus"), status));
        }

        int pageSize = Math.max(1, Math.min(size, 50));
        Sort sort = switch (Objects.toString(order, "newest")) {
            case "oldest" -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("requisitionId"));
            case "position_asc" -> Sort.by(Sort.Order.asc("title"), Sort.Order.desc("requisitionId"));
            case "position_desc" -> Sort.by(Sort.Order.desc("title"), Sort.Order.desc("requisitionId"));
            default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("requisitionId"));
        };

        Page<JobRequisition> pageResult = requisitions.findAll(filter, PageRequest.of(Math.max(0, page - 1), pageSize, sort));
        if (pageResult.getTotalPages() > 0 && pageResult.getNumber() >= pageResult.getTotalPages()) {
            pageResult = requisitions.findAll(filter, PageRequest.of(pageResult.getTotalPages() - 1, pageSize, sort));
        }
        return pageResult.map(jobRequisition -> response(jobRequisition, currentUser, false));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAvailablePositions() {
        return requisitions.findDistinctTitles();
    }

    @Override
    @Transactional(readOnly = true)
    public long countVisible() {
        return requisitions.count(scope(access.actor()));
    }

    @Override
    @Transactional(readOnly = true)
    public RequisitionResponse getById(Integer id) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, false);
        // Rule: Kiểm tra quyền xem chi tiết yêu cầu
        access.requireView(currentUser, jobRequisition);
        return response(jobRequisition, currentUser, true);
    }

    @Override
    @Transactional(readOnly = true)
    public RequisitionRequest getRequestDtoById(Integer id) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, false);
        // Rule: Chỉ người có quyền sửa (HM sở hữu yêu cầu khi còn là Draft) mới lấy được DTO form
        access.requireEdit(currentUser, jobRequisition);
        return convertToRequestDto(jobRequisition);
    }

    @Override
    @Transactional(readOnly = true)
    public RequisitionRequest copy(Integer id) {
        User currentUser = access.actor();
        // Rule: Người sao chép phải có quyền tạo mới yêu cầu
        access.requireCreate(currentUser);
        JobRequisition jobRequisition = findRequisitionById(id, false);
        access.requireView(currentUser, jobRequisition);

        // Hướng 1: Chặn sao chép đối với các đơn Draft và Pending_Director
        if (STATUSES_NOT_COPYABLE.contains(jobRequisition.getApprovalStatus())) {
            throw createValidationException("action", "Chỉ có thể sao chép yêu cầu đã hoàn tất phê duyệt hoặc bị từ chối.");
        }

        RequisitionRequest copiedDto = convertToRequestDto(jobRequisition);
        // Reset criteria IDs để khi tạo mới sẽ sinh bản ghi mới
        copiedDto.getScreeningCriteria().forEach(criterion -> criterion.setCriteriaId(null));
        copiedDto.setRequisitionCode(null);

        // Đợt tiếp theo trong chuỗi của phiếu gốc; chỉ để hiển thị, server tính lại khi lưu
        JobRequisition root = rootOf(jobRequisition);
        copiedDto.setOriginalRequisitionId(root.getRequisitionId());
        copiedDto.setRecruitmentRound(requisitions.findMaxRoundInChain(root.getRequisitionId()) + 1);

        return copiedDto;
    }

    private RequisitionRequest convertToRequestDto(JobRequisition jobRequisition) {
        List<ScreeningCriteriaRequest> criteriaList = jobRequisition.getScreeningCriteria().stream()
                .map(criterion -> ScreeningCriteriaRequest.builder()
                        .criteriaId(criterion.getCriteriaId())
                        .criteriaName(criterion.getCriteriaName())
                        .criteriaType(criterion.getCriteriaType())
                        .requiredValue(criterion.getRequiredValue())
                        .weight(criterion.getWeight())
                        .isMandatory(criterion.getIsMandatory())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        return RequisitionRequest.builder()
                .requisitionCode(jobRequisition.getRequisitionCode())
                .recruitmentRound(jobRequisition.getRecruitmentRound())
                .hiringType(jobRequisition.getHiringType())
                .title(jobRequisition.getTitle())
                .departmentId(jobRequisition.getDepartment() == null ? null : jobRequisition.getDepartment().getDepartmentId())
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

    @Override
    public Integer createRequisition(RequisitionRequest requestDto) {
        User currentUser = access.actor();
        // Rule: Kiểm tra quyền tạo yêu cầu
        access.requireCreate(currentUser);

        // HM chỉ tạo cho phòng ban mà mình quản lý
        if (currentUser.getDepartment() != null) {
            requestDto.setDepartmentId(currentUser.getDepartment().getDepartmentId());
        }

        validator.validate(requestDto);

        // Hướng 2: Bắt lỗi Validation chặn trùng lặp đơn Draft cho cùng vị trí trong phòng ban
        if (requestDto.getDepartmentId() != null && requestDto.getTitle() != null && !requestDto.getTitle().isBlank()) {
            boolean hasDraft = requisitions.existsDraftByTitleAndDepartment(requestDto.getTitle().trim(), requestDto.getDepartmentId(), null);
            if (hasDraft) {
                throw createValidationException("title", "Bạn đang có một bản nháp cho vị trí này trong phòng ban. Vui lòng hoàn thiện bản nháp hiện có.");
            }
        }

        // Rule: Tiêu chí mới không được chứa ID đã tồn tại
        if (requestDto.getScreeningCriteria().stream().anyMatch(c -> c.getCriteriaId() != null)) {
            throw createValidationException("screeningCriteria", "Tiêu chí mới không được tham chiếu bản ghi có sẵn.");
        }

        JobRequisition jobRequisition = new JobRequisition();
        jobRequisition.setHiringManager(currentUser);
        jobRequisition.setCreatedBy(currentUser);
        jobRequisition.setScreeningCriteria(new ArrayList<>());
        assignRecruitmentRound(jobRequisition, requestDto.getOriginalRequisitionId(), currentUser);
        applyDataToEntity(jobRequisition, requestDto);

        requestDto.getScreeningCriteria().stream()
                .filter(c -> !RequisitionValidator.isBlankCriteria(c))
                .forEach(c -> jobRequisition.getScreeningCriteria().add(buildCriterionEntity(jobRequisition, c)));

        // Rule: Xác định trạng thái ban đầu (Draft hoặc Pending_Director)
        boolean isSubmitted = "submit".equals(requestDto.getAction());
        jobRequisition.setApprovalStatus(isSubmitted ? "Pending_Director" : "Draft");
        if (isSubmitted) {
            jobRequisition.setSubmittedAt(LocalDateTime.now());
        }

        requisitions.saveAndFlush(jobRequisition);

        createAuditLog(jobRequisition, currentUser, "CREATE", null, "Tạo yêu cầu: " + jobRequisition.getTitle() + " · " + jobRequisition.getApprovalStatus());

        return jobRequisition.getRequisitionId();
    }

    @Override
    public void updateRequisition(Integer id, RequisitionRequest requestDto) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, true);
        // Rule: Chỉ cho phép chỉnh sửa khi còn là bản nháp và đúng người sở hữu
        access.requireEdit(currentUser, jobRequisition);

        if (currentUser.getDepartment() != null) {
            requestDto.setDepartmentId(currentUser.getDepartment().getDepartmentId());
        }

        validator.validate(requestDto);

        // Kiểm tra không trùng tên bản nháp khác trong cùng phòng ban
        if (requestDto.getDepartmentId() != null && requestDto.getTitle() != null && !requestDto.getTitle().isBlank()) {
            boolean hasDraft = requisitions.existsDraftByTitleAndDepartment(requestDto.getTitle().trim(), requestDto.getDepartmentId(), id);
            if (hasDraft) {
                throw createValidationException("title", "Bạn đang có một bản nháp khác cho vị trí này trong phòng ban.");
            }
        }

        Map<String, String> beforeSnapshot = createSnapshot(jobRequisition);
        synchronizeCriteria(jobRequisition, requestDto.getScreeningCriteria());
        applyDataToEntity(jobRequisition, requestDto);

        // Rule: Cập nhật trạng thái khi nộp lại hoặc tiếp tục lưu nháp
        boolean isSubmitted = "submit".equals(requestDto.getAction());
        jobRequisition.setApprovalStatus(isSubmitted ? "Pending_Director" : "Draft");
        if (isSubmitted) {
            jobRequisition.setSubmittedAt(LocalDateTime.now());
        }

        Map<String, String> afterSnapshot = createSnapshot(jobRequisition);
        Set<String> allKeys = new LinkedHashSet<>(beforeSnapshot.keySet());
        allKeys.addAll(afterSnapshot.keySet());
        List<String> changedKeys = allKeys.stream()
                .filter(k -> !Objects.equals(beforeSnapshot.get(k), afterSnapshot.get(k)))
                .toList();

        if (!changedKeys.isEmpty()) {
            jobRequisition.setUpdatedAt(LocalDateTime.now());
            requisitions.saveAndFlush(jobRequisition);
            createAuditLog(jobRequisition, currentUser, "UPDATE",
                    changedKeys.stream().map(k -> k + ": " + formatDisplayValue(beforeSnapshot.get(k))).collect(Collectors.joining("\n")),
                    changedKeys.stream().map(k -> k + ": " + formatDisplayValue(beforeSnapshot.get(k)) + " → " + formatDisplayValue(afterSnapshot.get(k))).collect(Collectors.joining("\n")));
        }
    }

    private void synchronizeCriteria(JobRequisition jobRequisition, List<ScreeningCriteriaRequest> inputCriteriaList) {
        List<ScreeningCriteriaRequest> validCriteriaList = inputCriteriaList.stream()
                .filter(c -> !RequisitionValidator.isBlankCriteria(c))
                .toList();

        Map<Integer, ScreeningCriteria> existingCriteriaMap = jobRequisition.getScreeningCriteria().stream()
                .collect(Collectors.toMap(ScreeningCriteria::getCriteriaId, c -> c));

        if (validCriteriaList.stream().anyMatch(c -> c.getCriteriaId() != null && !existingCriteriaMap.containsKey(c.getCriteriaId()))) {
            throw createValidationException("screeningCriteria", "Một tiêu chí không thuộc về yêu cầu này.");
        }

        Set<Integer> keptCriteriaIds = validCriteriaList.stream()
                .map(ScreeningCriteriaRequest::getCriteriaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

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
     */
    private void assignRecruitmentRound(JobRequisition jobRequisition, Integer sourceRequisitionId, User currentUser) {
        if (sourceRequisitionId == null) {
            jobRequisition.setRecruitmentRound(1);
            return;
        }
        JobRequisition source = requisitions.findById(sourceRequisitionId)
                .orElseThrow(() -> createValidationException("originalRequisitionId", "Yêu cầu nguồn để sao chép không tồn tại."));
        access.requireView(currentUser, source);
        if (STATUSES_NOT_COPYABLE.contains(source.getApprovalStatus())) {
            throw createValidationException("action", "Chỉ có thể sao chép yêu cầu đã hoàn tất phê duyệt hoặc bị từ chối.");
        }
        // Khóa phiếu gốc để hai lần tạo đợt mới đồng thời không lấy trùng số đợt
        JobRequisition root = findRequisitionById(rootOf(source).getRequisitionId(), true);
        jobRequisition.setOriginalRequisition(root);
        jobRequisition.setRecruitmentRound(requisitions.findMaxRoundInChain(root.getRequisitionId()) + 1);
    }

    private JobRequisition rootOf(JobRequisition jobRequisition) {
        return jobRequisition.getOriginalRequisition() != null ? jobRequisition.getOriginalRequisition() : jobRequisition;
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
        jobRequisition.setTitle(requestDto.getTitle() == null ? "Untitled requisition" : requestDto.getTitle());
        jobRequisition.setHiringType(requestDto.getHiringType());
        jobRequisition.setDepartment(requestDto.getDepartmentId() == null ? null : departments.findById(requestDto.getDepartmentId()).orElseThrow(() -> createValidationException("departmentId", "Phòng ban không tồn tại.")));
        jobRequisition.setNumberOfPositions(requestDto.getNumberOfPositions());
        jobRequisition.setEmploymentType(requestDto.getEmploymentType());
        jobRequisition.setMinSalary(requestDto.getMinSalary());
        jobRequisition.setMaxSalary(requestDto.getMaxSalary());
        jobRequisition.setGender(requestDto.getGender());
        jobRequisition.setWorkLocation(requestDto.getWorkLocation());
        jobRequisition.setWorkModel(requestDto.getWorkModel());
        jobRequisition.setProbationDays(requestDto.getProbationDays());
        jobRequisition.setExpectedStartDate(requestDto.getExpectedStartDate());
        jobRequisition.setReasonForHiring(requestDto.getReasonForHiring());
        jobRequisition.setJobDescription(requestDto.getJobDescription());
        jobRequisition.setRequirementDetails(requestDto.getRequirementDetails());
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

        createAuditLog(jobRequisition, currentUser, "DELETE", null, "Đã xóa yêu cầu: " + jobRequisition.getTitle());
        approvals.deleteAll(approvals.findByRequisition_RequisitionIdOrderByApprovalDateDesc(id));
        requisitions.delete(jobRequisition);
        requisitions.flush();
    }

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
        if (cleanedComment != null && cleanedComment.length() > 1000) {
            throw createValidationException("comment", "Tối đa 1000 ký tự.");
        }

        String nextStatus = approved ? "Approved" : "Rejected";
        jobRequisition.setApprovalStatus(nextStatus);

        RequisitionApproval decision = approvals.save(RequisitionApproval.builder()
                .requisition(jobRequisition)
                .director(currentUser)
                .status(nextStatus)
                .comments(cleanedComment)
                .build());

        requisitions.saveAndFlush(jobRequisition);

        createAuditLog(jobRequisition, currentUser, "UPDATE", "Status: Pending_Director",
                "Status: Pending_Director → " + nextStatus + (cleanedComment == null ? "" : "\nÝ kiến Giám đốc: " + cleanedComment));

        if (!approved) {
            notificationService.notifyRequisitionRejected(decision);
        }
    }

    @Override
    public void withdraw(Integer id) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, true);
        access.requireView(currentUser, jobRequisition);

        // Rule: Chỉ người tạo yêu cầu mới được rút lại yêu cầu đang chờ duyệt
        if (!access.owns(currentUser, jobRequisition) || !"Pending_Director".equals(jobRequisition.getApprovalStatus())) {
            throw new AccessDeniedException("Chỉ người tạo yêu cầu mới có quyền rút lại yêu cầu đang chờ duyệt.");
        }

        // Rule: Chuyển trạng thái từ Pending_Director về Draft
        jobRequisition.setApprovalStatus("Draft");
        requisitions.saveAndFlush(jobRequisition);
        createAuditLog(jobRequisition, currentUser, "UPDATE", "Status: Pending_Director", "Status: Pending_Director → Draft (rút lại)");
    }

    private JobRequisition findRequisitionById(Integer id, boolean lockForUpdate) {
        return (lockForUpdate ? requisitions.findForUpdate(id) : requisitions.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tuyển dụng."));
    }

    private RequisitionValidationException createValidationException(String field, String message) {
        return new RequisitionValidationException(Map.of(field, message));
    }

    private void createAuditLog(JobRequisition jobRequisition, User currentUser, String action, String oldValue, String newValue) {
        audit.save(AuditLog.builder()
                .user(currentUser)
                .action(action)
                .entityName("JobRequisition")
                .entityId(jobRequisition.getRequisitionId().toString())
                .oldValue(oldValue)
                .newValue(newValue)
                .build());
    }

    private String formatDisplayValue(Object value) {
        return value == null ? "Chưa xác định" : value.toString();
    }

    private String formatNumber(BigDecimal number) {
        return number == null ? null : number.stripTrailingZeros().toPlainString();
    }

    private Map<String, String> createSnapshot(JobRequisition jobRequisition) {
        Map<String, String> snapshotMap = new LinkedHashMap<>();
        snapshotMap.put("Chức danh công việc", jobRequisition.getTitle());
        snapshotMap.put("Phòng ban", jobRequisition.getDepartment() == null ? null : jobRequisition.getDepartment().getDepartmentName());
        snapshotMap.put("Số lượng cần tuyển", formatDisplayValue(jobRequisition.getNumberOfPositions()));
        snapshotMap.put("Hình thức làm việc", jobRequisition.getEmploymentType());
        snapshotMap.put("Mức lương tối thiểu", formatNumber(jobRequisition.getMinSalary()));
        snapshotMap.put("Mức lương tối đa", formatNumber(jobRequisition.getMaxSalary()));
        snapshotMap.put("Yêu cầu giới tính", jobRequisition.getGender());
        snapshotMap.put("Địa điểm làm việc", jobRequisition.getWorkLocation());
        snapshotMap.put("Mô hình làm việc", jobRequisition.getWorkModel());
        snapshotMap.put("Thời gian thử việc", jobRequisition.getProbationDays() == null ? null : jobRequisition.getProbationDays() + " ngày");
        snapshotMap.put("Ngày bắt đầu dự kiến", formatDisplayValue(jobRequisition.getExpectedStartDate()));
        snapshotMap.put("Lý do tuyển dụng", jobRequisition.getReasonForHiring());
        snapshotMap.put("Mô tả công việc", jobRequisition.getJobDescription());
        snapshotMap.put("Yêu cầu ứng viên", jobRequisition.getRequirementDetails());
        snapshotMap.put("Trạng thái phê duyệt", jobRequisition.getApprovalStatus());

        int criteriaIndex = 0;
        for (ScreeningCriteria criterion : jobRequisition.getScreeningCriteria()) {
            String criterionName = (criterion.getCriteriaName() == null ? "Tiêu chí " + (++criteriaIndex) : criterion.getCriteriaName());
            snapshotMap.put("Tiêu chí: " + criterionName,
                    formatDisplayValue(criterion.getCriteriaType()) + "; "
                            + formatDisplayValue(criterion.getRequiredValue()) + "; trọng số "
                            + formatDisplayValue(formatNumber(criterion.getWeight())) + "%; bắt buộc "
                            + Boolean.TRUE.equals(criterion.getIsMandatory()));
        }
        return snapshotMap;
    }

    private RequisitionResponse response(JobRequisition jobRequisition, User currentUser, boolean isDetailView) {
        boolean isEditable = access.canEdit(currentUser, jobRequisition);
        RequisitionResponse responseDto = RequisitionResponse.builder()
                .requisitionId(jobRequisition.getRequisitionId())
                .title(jobRequisition.getTitle())
                .departmentName(jobRequisition.getDepartment() == null ? "Chưa xác định" : jobRequisition.getDepartment().getDepartmentName())
                .hiringManagerName(jobRequisition.getHiringManager().getFullName())
                .numberOfPositions(jobRequisition.getNumberOfPositions())
                .employmentType(jobRequisition.getEmploymentType())
                .approvalStatus(jobRequisition.getApprovalStatus())
                .recruitmentRound(jobRequisition.getRecruitmentRound())
                .hiringType(jobRequisition.getHiringType())
                .requisitionCode(jobRequisition.getRequisitionCode() != null ? jobRequisition.getRequisitionCode() : ("REQ-" + jobRequisition.getRequisitionId()))
                .createdAt(jobRequisition.getCreatedAt())
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
                .editable(isEditable)
                .deletable(isEditable && !postings.existsByRequisition_RequisitionId(jobRequisition.getRequisitionId()))
                .decidable(access.canDecide(currentUser, jobRequisition))
                .withdrawable(access.owns(currentUser, jobRequisition) && "Pending_Director".equals(jobRequisition.getApprovalStatus()))
                .build();

        postings.findTopByRequisition_RequisitionIdOrderByJobPostingIdDesc(jobRequisition.getRequisitionId())
                .ifPresent(p -> {
                    responseDto.setPostingStatus(p.getPostingStatus());
                    responseDto.setJobPostingId(p.getJobPostingId());
                });

        if (isDetailView) {

            // Kiểm tra xem bản nháp đã điền đủ thông tin để submit chưa
            if ("Draft".equals(jobRequisition.getApprovalStatus())) {
                RequisitionRequest submissionCheck = convertToRequestDto(jobRequisition);
                submissionCheck.setAction("submit");
                try {
                    validator.validate(submissionCheck);
                } catch (RequisitionValidationException incomplete) {
                    responseDto.setIncomplete(true);
                }
            }

            responseDto.setScreeningCriteria(jobRequisition.getScreeningCriteria().stream().map(c -> {
                ScreeningCriteriaResponse criterionResponse = new ScreeningCriteriaResponse();
                criterionResponse.setCriteriaId(c.getCriteriaId());
                criterionResponse.setCriteriaName(c.getCriteriaName());
                criterionResponse.setCriteriaType(c.getCriteriaType());
                criterionResponse.setRequiredValue(c.getRequiredValue());
                criterionResponse.setWeight(c.getWeight());
                criterionResponse.setIsMandatory(c.getIsMandatory());
                return criterionResponse;
            }).toList());

            responseDto.setApprovals(approvals.findByRequisition_RequisitionIdOrderByApprovalDateDesc(jobRequisition.getRequisitionId()).stream()
                    .map(approval -> ApprovalResponse.builder()
                            .approverName(approval.getDirector().getFullName())
                            .status(approval.getStatus())
                            .approvalDate(approval.getApprovalDate())
                            .comments(approval.getComments())
                            .build())
                    .toList());

            responseDto.setActivityLog(audit.findByEntityNameAndEntityIdOrderByTimestampDesc("JobRequisition", jobRequisition.getRequisitionId().toString()).stream()
                    .map(log -> ActivityLogResponse.builder()
                            .auditLogId(log.getAuditLogId())
                            .action(log.getAction())
                            .performedBy(log.getUser().getFullName())
                            .description(log.getNewValue())
                            .timestamp(log.getTimestamp())
                            .build())
                    .toList());

            if ("Rejected".equals(jobRequisition.getApprovalStatus()) && !responseDto.getApprovals().isEmpty()) {
                responseDto.setRejectionReason(responseDto.getApprovals().getFirst().getComments());
            }
        }
        return responseDto;
    }
}
