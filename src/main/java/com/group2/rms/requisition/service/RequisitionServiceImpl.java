package com.group2.rms.requisition.service;

import com.group2.rms.admin.dto.ActivityLogResponse;
import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.requisition.dto.ApprovalResponse;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.dto.RequisitionTimelineResponse;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaResponse;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.repository.JobPostingRepository;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.repository.RequisitionApprovalRepository;
import com.group2.rms.requisition.repository.ScreeningCriteriaRepository;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý nghiệp vụ Job Requisition (Yêu cầu tuyển dụng).
 * Tuân thủ quy định tại docs/members/hoangnh/rule_code_nhh.md
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RequisitionServiceImpl implements RequisitionService {

    private static final String ACTION_SUBMIT = "submit";
    private static final String ACTION_DRAFT = "draft";

    private final JobRequisitionRepository requisitionRepository;
    private final DepartmentRepository departmentRepository;
    private final RequisitionApprovalRepository approvalRepository;
    private final ScreeningCriteriaRepository screeningCriteriaRepository;
    private final JobPostingRepository jobPostingRepository;
    private final AuditLogRepository auditLogRepository;
    private final RequisitionAccess requisitionAccess;
    private final RequisitionValidator requisitionValidator;

    /**
     * Tạo bộ lọc phạm vi dữ liệu dựa trên vai trò của người dùng hiện tại.
     */
    private Specification<JobRequisition> scope(User currentUser) {
        return (root, query, criteriaBuilder) -> {
            String roleName = requisitionAccess.role(currentUser);

            // Rule: HM chỉ xem requisition do chính mình tạo hoặc thuộc các phòng ban mình phụ trách
            if (RequisitionAccess.ROLE_HIRING_MANAGER.equals(roleName)) {
                List<Department> managedDepartments = getAvailableDepartments(currentUser);
                List<Integer> departmentIds = managedDepartments.stream()
                        .map(Department::getDepartmentId)
                        .filter(Objects::nonNull)
                        .toList();

                Join<JobRequisition, User> hiringManagerJoin = root.join("hiringManager", JoinType.LEFT);

                if (!departmentIds.isEmpty()) {
                    Join<JobRequisition, Department> departmentJoin = root.join("department", JoinType.LEFT);
                    CriteriaBuilder.In<Integer> inDepartmentClause = criteriaBuilder.in(departmentJoin.get("departmentId"));
                    for (Integer departmentId : departmentIds) {
                        inDepartmentClause.value(departmentId);
                    }
                    return criteriaBuilder.or(
                            criteriaBuilder.equal(hiringManagerJoin.get("userId"), currentUser.getUserId()),
                            inDepartmentClause
                    );
                } else {
                    return criteriaBuilder.equal(hiringManagerJoin.get("userId"), currentUser.getUserId());
                }
            }

            // Rule: Director xem tất cả các requisition không phải Draft (hoặc xem chính Draft của mình)
            if (RequisitionAccess.ROLE_DIRECTOR.equals(roleName)) {
                Join<JobRequisition, User> hiringManagerJoin = root.join("hiringManager", JoinType.LEFT);
                return criteriaBuilder.or(
                        criteriaBuilder.notEqual(root.get("approvalStatus"), RequisitionAccess.STATUS_DRAFT),
                        criteriaBuilder.equal(hiringManagerJoin.get("userId"), currentUser.getUserId())
                );
            }

            // Rule: HR chỉ xem các requisition đã được Director phê duyệt
            if (RequisitionAccess.ROLE_HR.equals(roleName)) {
                return criteriaBuilder.equal(root.get("approvalStatus"), RequisitionAccess.STATUS_APPROVED);
            }

            return criteriaBuilder.conjunction();
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<Department> getAvailableDepartments(User currentUser) {
        String roleName = requisitionAccess.role(currentUser);

        // Security: Director và System Admin quản lý toàn bộ phòng ban hoạt động
        if (Set.of(RequisitionAccess.ROLE_DIRECTOR, RequisitionAccess.ROLE_SYSTEM_ADMIN).contains(roleName)) {
            return departmentRepository.findByDepartmentStatus("Active");
        }

        // Rule: HM chỉ lấy các phòng ban mình làm trưởng phòng hoặc trực thuộc
        if (RequisitionAccess.ROLE_HIRING_MANAGER.equals(roleName)) {
            List<Department> managedDepartments = departmentRepository.findByManager_UserId(currentUser.getUserId());
            Set<Integer> seenDepartmentIds = managedDepartments.stream()
                    .map(Department::getDepartmentId)
                    .collect(Collectors.toSet());

            List<Department> resultDepartments = new ArrayList<>(managedDepartments);

            if (currentUser.getDepartment() != null
                    && currentUser.getDepartment().getDepartmentId() != null
                    && !seenDepartmentIds.contains(currentUser.getDepartment().getDepartmentId())) {
                departmentRepository.findById(currentUser.getDepartment().getDepartmentId())
                        .ifPresent(resultDepartments::add);
            }
            return resultDepartments;
        }

        return departmentRepository.findByDepartmentStatus("Active");
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAvailablePositions(User currentUser) {
        // Rule: HM chỉ lọc các vị trí thuộc phòng ban mình quản lý
        if (RequisitionAccess.ROLE_HIRING_MANAGER.equals(requisitionAccess.role(currentUser))) {
            List<Integer> departmentIds = getAvailableDepartments(currentUser).stream()
                    .map(Department::getDepartmentId)
                    .filter(Objects::nonNull)
                    .toList();
            if (!departmentIds.isEmpty()) {
                return requisitionRepository.findDistinctTitlesByDepartmentIds(departmentIds);
            }
            return Collections.emptyList();
        }
        return requisitionRepository.findDistinctTitles();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getAvailableRounds() {
        return List.of(1, 2, 3, 4, 5);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequisitionResponse> search(int page, int size, String query, Integer departmentId, String employmentType, String status) {
        return search(page, size, query, departmentId, null, null, employmentType, status, "newest");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequisitionResponse> search(int page, int size, String query, Integer departmentId, String employmentType, String status, String sortOrder) {
        return search(page, size, query, departmentId, null, null, employmentType, status, sortOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequisitionResponse> search(int page, int size, String query, Integer departmentId, String position, Integer recruitmentRound, String employmentType, String status, String sortOrder) {
        User currentUser = requisitionAccess.actor();
        Specification<JobRequisition> filterSpecification = scope(currentUser);

        String searchTerm = Objects.toString(query, "").trim().toLowerCase(Locale.ROOT);
        if (searchTerm.length() > 120) {
            searchTerm = searchTerm.substring(0, 120);
        }

        if (!searchTerm.isEmpty()) {
            String escapedPattern = "%" + searchTerm.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("[", "\\[") + "%";
            filterSpecification = filterSpecification.and((root, queryObj, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), escapedPattern, '\\'),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("requisitionCode")), escapedPattern, '\\'),
                    criteriaBuilder.like(criteriaBuilder.lower(root.join("department", JoinType.LEFT).get("departmentName")), escapedPattern, '\\')
            ));
        }

        if (departmentId != null) {
            filterSpecification = filterSpecification.and((root, queryObj, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("department").get("departmentId"), departmentId));
        }
        if (position != null && !position.isBlank()) {
            filterSpecification = filterSpecification.and((root, queryObj, criteriaBuilder) ->
                    criteriaBuilder.equal(criteriaBuilder.lower(root.get("title")), position.trim().toLowerCase(Locale.ROOT)));
        }
        if (recruitmentRound != null && recruitmentRound > 0) {
            filterSpecification = filterSpecification.and((root, queryObj, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("recruitmentRound"), recruitmentRound));
        }
        if (employmentType != null && !employmentType.isBlank()) {
            filterSpecification = filterSpecification.and((root, queryObj, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("employmentType"), employmentType));
        }
        if (status != null && !status.isBlank()) {
            filterSpecification = filterSpecification.and((root, queryObj, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("approvalStatus"), status));
        }

        int pageSize = Math.max(1, Math.min(size, 50));
        Sort sortCriteria = switch (Objects.toString(sortOrder, "newest")) {
            case "oldest" -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("requisitionId"));
            case "position_asc" -> Sort.by(Sort.Order.asc("title"), Sort.Order.desc("requisitionId"));
            case "position_desc" -> Sort.by(Sort.Order.desc("title"), Sort.Order.desc("requisitionId"));
            default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("requisitionId"));
        };

        Page<JobRequisition> pageResult = requisitionRepository.findAll(
                filterSpecification,
                PageRequest.of(Math.max(0, page - 1), pageSize, sortCriteria)
        );

        if (pageResult.getTotalPages() > 0 && pageResult.getNumber() >= pageResult.getTotalPages()) {
            pageResult = requisitionRepository.findAll(
                    filterSpecification,
                    PageRequest.of(pageResult.getTotalPages() - 1, pageSize, sortCriteria)
            );
        }

        return pageResult.map(requisition -> toResponse(requisition, currentUser, false));
    }

    @Override
    @Transactional(readOnly = true)
    public long countVisible() {
        return requisitionRepository.count(scope(requisitionAccess.actor()));
    }

    @Override
    @Transactional(readOnly = true)
    public RequisitionResponse getById(Integer requisitionId) {
        User currentUser = requisitionAccess.actor();
        JobRequisition requisition = findRequisitionById(requisitionId, false);
        // Security: Kiểm tra quyền xem chi tiết yêu cầu tuyển dụng
        requisitionAccess.requireView(currentUser, requisition);
        return toResponse(requisition, currentUser, true);
    }

    @Override
    @Transactional(readOnly = true)
    public RequisitionRequest getRequestDtoById(Integer requisitionId) {
        JobRequisition requisition = findRequisitionById(requisitionId, false);
        // Security: Kiểm tra quyền chỉnh sửa yêu cầu tuyển dụng
        requisitionAccess.requireEdit(requisitionAccess.actor(), requisition);
        return toRequestDto(requisition);
    }

    @Override
    @Transactional(readOnly = true)
    public RequisitionRequest copy(Integer requisitionId) {
        User currentUser = requisitionAccess.actor();
        // Security: Kiểm tra quyền tạo yêu cầu tuyển dụng khi sao chép
        requisitionAccess.requireCreate(currentUser);
        JobRequisition existingRequisition = findRequisitionById(requisitionId, false);
        requisitionAccess.requireView(currentUser, existingRequisition);

        RequisitionRequest copyRequest = toRequestDto(existingRequisition);
        copyRequest.getScreeningCriteria().forEach(criterion -> criterion.setCriteriaId(null));
        copyRequest.setRequisitionCode(null);

        // Rule: Copy requisition tự động tăng RecruitmentRound lên vòng tiếp theo
        int departmentId = existingRequisition.getDepartment() != null ? existingRequisition.getDepartment().getDepartmentId() : 0;
        Integer maxRound = (departmentId > 0 && existingRequisition.getTitle() != null)
                ? requisitionRepository.findMaxRecruitmentRound(existingRequisition.getTitle(), departmentId)
                : null;
        int nextRound = (maxRound != null && maxRound > 0)
                ? maxRound + 1
                : ((existingRequisition.getRecruitmentRound() != null ? existingRequisition.getRecruitmentRound() : 1) + 1);

        copyRequest.setRecruitmentRound(nextRound);
        return copyRequest;
    }

    @Override
    public Integer createRequisition(RequisitionRequest request) {
        User currentUser = requisitionAccess.actor();
        // Security: Kiểm tra quyền tạo yêu cầu tuyển dụng
        requisitionAccess.requireCreate(currentUser);
        requisitionValidator.validate(request);

        // Rule: HM chỉ được chọn phòng ban mà mình phụ trách
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> createValidationException("departmentId", "Vui lòng chọn phòng ban hợp lệ."));
            if (!requisitionAccess.canManageDepartment(currentUser, department.getDepartmentId(), department.getManager())) {
                throw createValidationException("departmentId", "Bạn chỉ có thể tạo yêu cầu tuyển dụng cho các phòng ban mình quản lý.");
            }
        }

        if (request.getScreeningCriteria().stream().anyMatch(criteria -> criteria.getCriteriaId() != null)) {
            throw createValidationException("screeningCriteria", "Tiêu chí mới không được tham chiếu bản ghi cũ.");
        }

        JobRequisition requisition = new JobRequisition();
        requisition.setHiringManager(currentUser);
        requisition.setScreeningCriteria(new ArrayList<>());
        applyRequestDataToEntity(requisition, request);

        // Rule: Tự động tính recruitmentRound = maxRound + 1 nếu chưa nhập
        if (requisition.getRecruitmentRound() == null || requisition.getRecruitmentRound() < 1) {
            int departmentId = requisition.getDepartment() != null ? requisition.getDepartment().getDepartmentId() : 0;
            Integer maxRound = (departmentId > 0 && requisition.getTitle() != null)
                    ? requisitionRepository.findMaxRecruitmentRound(requisition.getTitle(), departmentId)
                    : null;
            requisition.setRecruitmentRound((maxRound != null && maxRound > 0) ? maxRound + 1 : 1);
        }

        request.getScreeningCriteria().stream()
                .filter(criterion -> !RequisitionValidator.blank(criterion))
                .forEach(criterion -> requisition.getScreeningCriteria().add(buildScreeningCriteriaEntity(requisition, criterion)));

        boolean isDirector = RequisitionAccess.ROLE_DIRECTOR.equals(requisitionAccess.role(currentUser));

        // Rule: Director tạo và gửi yêu cầu sẽ được tự động phê duyệt trực tiếp
        if (ACTION_SUBMIT.equals(request.getAction())) {
            if (isDirector) {
                requisition.setApprovalStatus(RequisitionAccess.STATUS_APPROVED);
                requisition.setSubmittedAt(LocalDateTime.now());
                requisition.setDecidedAt(LocalDateTime.now());
            } else {
                requisition.setApprovalStatus(RequisitionAccess.STATUS_PENDING_DIRECTOR);
                requisition.setSubmittedAt(LocalDateTime.now());
            }
        } else {
            requisition.setApprovalStatus(RequisitionAccess.STATUS_DRAFT);
        }

        if (requisition.getRequisitionCode() == null || requisition.getRequisitionCode().isBlank()) {
            int year = LocalDate.now().getYear();
            long nextSequence = requisitionRepository.count() + 1;
            requisition.setRequisitionCode(String.format("REQ-%d-%03d", year, nextSequence));
        }

        requisitionRepository.saveAndFlush(requisition);

        if (requisition.getSubmittedAt() != null) {
            if (isDirector) {
                approvalRepository.save(RequisitionApproval.builder()
                        .requisition(requisition)
                        .director(currentUser)
                        .status(RequisitionAccess.STATUS_APPROVED)
                        .comments("Được tạo và tự động phê duyệt bởi Giám đốc.")
                        .build());
            }
        }

        createAuditLog(requisition, currentUser, "CREATE", null, "Tạo yêu cầu: " + requisition.getTitle() + " · " + requisition.getApprovalStatus());
        return requisition.getRequisitionId();
    }

    @Override
    public void updateRequisition(Integer requisitionId, RequisitionRequest request) {
        User currentUser = requisitionAccess.actor();
        JobRequisition requisition = findRequisitionById(requisitionId, true);
        // Security: Kiểm tra quyền chỉnh sửa yêu cầu tuyển dụng
        requisitionAccess.requireEdit(currentUser, requisition);
        requisitionValidator.validate(request);

        Map<String, String> snapshotBefore = createSnapshot(requisition);
        synchronizeCriteria(requisition, request.getScreeningCriteria());
        applyRequestDataToEntity(requisition, request);

        // Rule: Cập nhật trạng thái tùy theo hành động lưu nháp hay gửi duyệt
        requisition.setApprovalStatus(ACTION_SUBMIT.equals(request.getAction())
                ? RequisitionAccess.STATUS_PENDING_DIRECTOR
                : RequisitionAccess.STATUS_DRAFT);

        if (ACTION_SUBMIT.equals(request.getAction())) {
            requisition.setSubmittedAt(LocalDateTime.now());
            requisition.setDecidedAt(null);
        }

        Map<String, String> snapshotAfter = createSnapshot(requisition);
        Set<String> allKeys = new LinkedHashSet<>(snapshotBefore.keySet());
        allKeys.addAll(snapshotAfter.keySet());

        List<String> changedFieldKeys = allKeys.stream()
                .filter(key -> !Objects.equals(snapshotBefore.get(key), snapshotAfter.get(key)))
                .toList();

        if (!changedFieldKeys.isEmpty()) {
            requisition.setUpdatedAt(LocalDateTime.now());
            requisitionRepository.saveAndFlush(requisition);

            String oldValues = changedFieldKeys.stream()
                    .map(key -> key + ": " + formatDisplayValue(snapshotBefore.get(key)))
                    .collect(Collectors.joining("\n"));
            String newValues = changedFieldKeys.stream()
                    .map(key -> key + ": " + formatDisplayValue(snapshotBefore.get(key)) + " → " + formatDisplayValue(snapshotAfter.get(key)))
                    .collect(Collectors.joining("\n"));

            createAuditLog(requisition, currentUser, "UPDATE", oldValues, newValues);
        }
    }

    @Override
    public void deleteRequisition(Integer requisitionId) {
        User currentUser = requisitionAccess.actor();
        JobRequisition requisition = findRequisitionById(requisitionId, true);
        // Security: Kiểm tra quyền xóa yêu cầu tuyển dụng
        requisitionAccess.requireEdit(currentUser, requisition);

        // Rule: Requisition đã liên kết tin tuyển dụng thì không được phép xóa
        if (jobPostingRepository.existsByRequisition_RequisitionId(requisitionId)) {
            throw createValidationException("action", "Yêu cầu tuyển dụng này đã có tin đăng tuyển và không thể xóa.");
        }

        createAuditLog(requisition, currentUser, "DELETE", null, "Đã xóa yêu cầu tuyển dụng: " + requisition.getTitle());
        // Rule: Xóa các bảng phụ thuộc (RequisitionApproval, ScreeningCriteria) trước khi xóa JobRequisition để tránh vi phạm khóa ngoại
        approvalRepository.deleteAll(approvalRepository.findByRequisition_RequisitionIdOrderByApprovalDateDesc(requisitionId));
        if (requisition.getScreeningCriteria() != null) {
            requisition.getScreeningCriteria().clear();
        }
        screeningCriteriaRepository.deleteAll(screeningCriteriaRepository.findByRequisition_RequisitionId(requisitionId));
        requisitionRepository.delete(requisition);
        requisitionRepository.flush();
    }

    @Override
    public void decide(Integer requisitionId, boolean isApproved, String comment) {
        User currentUser = requisitionAccess.actor();
        JobRequisition requisition = findRequisitionById(requisitionId, true);
        // Security: Kiểm tra quyền xem chi tiết trước khi quyết định
        requisitionAccess.requireView(currentUser, requisition);

        // Rule: Chỉ Director mới có quyền duyệt hoặc từ chối yêu cầu
        if (!requisitionAccess.canDecide(currentUser, requisition)) {
            throw new AccessDeniedException("Chỉ Giám đốc mới có quyền phê duyệt hoặc từ chối yêu cầu.");
        }

        String cleanedComment = RequisitionValidator.clean(comment);

        // Rule: Khi từ chối bắt buộc Director phải nhập lý do
        if (!isApproved && cleanedComment == null) {
            throw createValidationException("comment", "Vui lòng nhập lý do giải thích nội dung cần sửa đổi.");
        }
        if (cleanedComment != null && cleanedComment.length() > 1000) {
            throw createValidationException("comment", "Nhận xét tối đa 1000 ký tự.");
        }

        String targetStatus = isApproved ? RequisitionAccess.STATUS_APPROVED : RequisitionAccess.STATUS_REJECTED;
        requisition.setApprovalStatus(targetStatus);
        requisition.setDecidedAt(LocalDateTime.now());

        approvalRepository.save(RequisitionApproval.builder()
                .requisition(requisition)
                .director(currentUser)
                .status(targetStatus)
                .comments(cleanedComment)
                .build());

        requisitionRepository.saveAndFlush(requisition);

        createAuditLog(requisition, currentUser, "UPDATE",
                "Status: Pending_Director",
                "Status: Pending_Director → " + targetStatus + (cleanedComment == null ? "" : "\nPhản hồi của Giám đốc: " + cleanedComment));
    }

    @Override
    public void withdraw(Integer requisitionId) {
        User currentUser = requisitionAccess.actor();
        JobRequisition requisition = findRequisitionById(requisitionId, true);
        // Security: Kiểm tra quyền xem chi tiết trước khi rút yêu cầu
        requisitionAccess.requireView(currentUser, requisition);

        // Rule: Chỉ người tạo mới được rút lại yêu cầu đang chờ duyệt
        if (!requisitionAccess.owns(currentUser, requisition) || !RequisitionAccess.STATUS_PENDING_DIRECTOR.equals(requisition.getApprovalStatus())) {
            throw new AccessDeniedException("Chỉ người gửi mới có quyền rút lại yêu cầu đang chờ phê duyệt.");
        }

        requisition.setApprovalStatus(RequisitionAccess.STATUS_DRAFT);
        requisitionRepository.saveAndFlush(requisition);
        createAuditLog(requisition, currentUser, "UPDATE", "Status: Pending_Director", "Status: Pending_Director → Draft (Đã rút)");
    }

    private JobRequisition findRequisitionById(Integer requisitionId, boolean lock) {
        return (lock ? requisitionRepository.findForUpdate(requisitionId) : requisitionRepository.findById(requisitionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu tuyển dụng."));
    }

    private RequisitionValidationException createValidationException(String field, String message) {
        return new RequisitionValidationException(Map.of(field, message));
    }

    private void createAuditLog(JobRequisition requisition, User currentUser, String action, String oldValue, String newValue) {
        auditLogRepository.save(AuditLog.builder()
                .user(currentUser)
                .action(action)
                .entityName("JobRequisition")
                .entityId(requisition.getRequisitionId().toString())
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

    private void synchronizeCriteria(JobRequisition requisition, List<ScreeningCriteriaRequest> criteriaInputs) {
        List<ScreeningCriteriaRequest> validRows = criteriaInputs.stream()
                .filter(criterion -> !RequisitionValidator.blank(criterion))
                .toList();

        Map<Integer, ScreeningCriteria> existingCriteriaMap = requisition.getScreeningCriteria().stream()
                .collect(Collectors.toMap(ScreeningCriteria::getCriteriaId, criterion -> criterion));

        if (validRows.stream().anyMatch(row -> row.getCriteriaId() != null && !existingCriteriaMap.containsKey(row.getCriteriaId()))) {
            throw createValidationException("screeningCriteria", "Tiêu chí lọc không thuộc yêu cầu tuyển dụng này.");
        }

        Set<Integer> keptIds = validRows.stream()
                .map(ScreeningCriteriaRequest::getCriteriaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        boolean hasChanged = requisition.getScreeningCriteria().removeIf(criterion -> !keptIds.contains(criterion.getCriteriaId()));

        for (ScreeningCriteriaRequest row : validRows) {
            ScreeningCriteria existingCriterion = existingCriteriaMap.get(row.getCriteriaId());
            if (existingCriterion != null && !Objects.equals(existingCriterion.getCriteriaName(), row.getCriteriaName())) {
                existingCriterion.setCriteriaName("__rename_" + UUID.randomUUID());
                hasChanged = true;
            }
        }

        if (hasChanged) {
            requisitionRepository.flush();
        }

        for (ScreeningCriteriaRequest row : validRows) {
            ScreeningCriteria existingCriterion = existingCriteriaMap.get(row.getCriteriaId());
            if (existingCriterion == null) {
                requisition.getScreeningCriteria().add(buildScreeningCriteriaEntity(requisition, row));
            } else {
                copyCriteriaProperties(existingCriterion, row);
            }
        }
    }

    private ScreeningCriteria buildScreeningCriteriaEntity(JobRequisition requisition, ScreeningCriteriaRequest request) {
        ScreeningCriteria criterion = new ScreeningCriteria();
        criterion.setRequisition(requisition);
        copyCriteriaProperties(criterion, request);
        return criterion;
    }

    private void copyCriteriaProperties(ScreeningCriteria entity, ScreeningCriteriaRequest request) {
        entity.setCriteriaName(request.getCriteriaName());
        entity.setCriteriaType(request.getCriteriaType());
        entity.setRequiredValue(request.getRequiredValue());
        entity.setWeight(request.getWeight());
        entity.setIsMandatory(Boolean.TRUE.equals(request.getIsMandatory()));
    }

    private void applyRequestDataToEntity(JobRequisition requisition, RequisitionRequest request) {
        requisition.setTitle(request.getTitle() == null ? "Yêu cầu tuyển dụng chưa đặt tên" : request.getTitle());
        requisition.setDepartment(request.getDepartmentId() == null ? null : departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> createValidationException("departmentId", "Vui lòng chọn phòng ban hợp lệ.")));

        if (request.getRecruitmentRound() != null && request.getRecruitmentRound() > 0) {
            requisition.setRecruitmentRound(request.getRecruitmentRound());
        }
        requisition.setNumberOfPositions(request.getNumberOfPositions());
        requisition.setEmploymentType(request.getEmploymentType());
        requisition.setMinSalary(request.getMinSalary());
        requisition.setMaxSalary(request.getMaxSalary());
        requisition.setGender(request.getGender());
        requisition.setWorkLocation(request.getWorkLocation());
        requisition.setWorkModel(request.getWorkModel());
        requisition.setProbationDuration(request.getProbationDuration());
        requisition.setExpectedStartDate(request.getExpectedStartDate());
        requisition.setReasonForHiring(request.getReasonForHiring());
        requisition.setJobDescription(request.getJobDescription());
        requisition.setRequirementDetails(request.getRequirementDetails());
    }

    private Map<String, String> createSnapshot(JobRequisition requisition) {
        Map<String, String> snapshotMap = new LinkedHashMap<>();
        snapshotMap.put("Mã yêu cầu", requisition.getRequisitionCode());
        snapshotMap.put("Vị trí tuyển dụng", requisition.getTitle());
        snapshotMap.put("Đợt tuyển dụng", formatDisplayValue(requisition.getRecruitmentRound()));
        snapshotMap.put("Phòng ban", requisition.getDepartment() == null ? null : requisition.getDepartment().getDepartmentName());
        snapshotMap.put("Số lượng", formatDisplayValue(requisition.getNumberOfPositions()));
        snapshotMap.put("Hình thức làm việc", requisition.getEmploymentType());
        snapshotMap.put("Lương tối thiểu", formatNumber(requisition.getMinSalary()));
        snapshotMap.put("Lương tối đa", formatNumber(requisition.getMaxSalary()));
        snapshotMap.put("Giới tính", requisition.getGender());
        snapshotMap.put("Địa điểm", requisition.getWorkLocation());
        snapshotMap.put("Mô hình làm việc", requisition.getWorkModel());
        snapshotMap.put("Thời gian thử việc", requisition.getProbationDuration());
        snapshotMap.put("Ngày bắt đầu dự kiến", formatDisplayValue(requisition.getExpectedStartDate()));
        snapshotMap.put("Lý do tuyển dụng", requisition.getReasonForHiring());
        snapshotMap.put("Mô tả công việc", requisition.getJobDescription());
        snapshotMap.put("Yêu cầu ứng viên", requisition.getRequirementDetails());
        snapshotMap.put("Trạng thái", requisition.getApprovalStatus());

        int criteriaIndex = 0;
        for (ScreeningCriteria criterion : requisition.getScreeningCriteria()) {
            criteriaIndex++;
            String criteriaName = criterion.getCriteriaName() == null ? ("Tiêu chí " + criteriaIndex) : criterion.getCriteriaName();
            snapshotMap.put("Tiêu chí: " + criteriaName,
                    formatDisplayValue(criterion.getCriteriaType()) + "; " +
                            formatDisplayValue(criterion.getRequiredValue()) + "; trọng số " +
                            formatDisplayValue(formatNumber(criterion.getWeight())) + "%; bắt buộc " +
                            Boolean.TRUE.equals(criterion.getIsMandatory()));
        }
        return snapshotMap;
    }

    private RequisitionRequest toRequestDto(JobRequisition requisition) {
        ArrayList<ScreeningCriteriaRequest> criteriaRows = requisition.getScreeningCriteria().stream()
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
                .requisitionCode(requisition.getRequisitionCode())
                .title(requisition.getTitle())
                .departmentId(requisition.getDepartment() == null ? null : requisition.getDepartment().getDepartmentId())
                .recruitmentRound(requisition.getRecruitmentRound())
                .numberOfPositions(requisition.getNumberOfPositions())
                .employmentType(requisition.getEmploymentType())
                .minSalary(requisition.getMinSalary())
                .maxSalary(requisition.getMaxSalary())
                .gender(requisition.getGender())
                .workLocation(requisition.getWorkLocation())
                .workModel(requisition.getWorkModel())
                .probationDuration(requisition.getProbationDuration())
                .expectedStartDate(requisition.getExpectedStartDate())
                .reasonForHiring(requisition.getReasonForHiring())
                .jobDescription(requisition.getJobDescription())
                .requirementDetails(requisition.getRequirementDetails())
                .screeningCriteria(criteriaRows)
                .build();
    }

    private RequisitionResponse toResponse(JobRequisition requisition, User currentUser, boolean includeDetails) {
        boolean isEditable = requisitionAccess.canEdit(currentUser, requisition);
        int roundNumber = requisition.getRecruitmentRound() != null && requisition.getRecruitmentRound() > 0 ? requisition.getRecruitmentRound() : 1;
        String generatedCode = requisition.getRequisitionCode() != null && !requisition.getRequisitionCode().isBlank()
                ? requisition.getRequisitionCode()
                : String.format("REQ-%d-%03d", requisition.getCreatedAt() != null ? requisition.getCreatedAt().getYear() : 2026, requisition.getRequisitionId() != null ? requisition.getRequisitionId() : 0);

        var activePosting = jobPostingRepository != null && requisition.getRequisitionId() != null
                ? jobPostingRepository.findTopByRequisition_RequisitionIdOrderByJobPostingIdDesc(requisition.getRequisitionId())
                : Optional.<JobPosting>empty();
        String postingStatus = activePosting.map(JobPosting::getPostingStatus).orElse("None");
        Integer jobPostingId = activePosting.map(JobPosting::getJobPostingId).orElse(null);

        RequisitionResponse response = RequisitionResponse.builder()
                .requisitionId(requisition.getRequisitionId())
                .requisitionCode(generatedCode)
                .title(requisition.getTitle())
                .recruitmentRound(roundNumber)
                .departmentId(requisition.getDepartment() != null ? requisition.getDepartment().getDepartmentId() : null)
                .departmentName(requisition.getDepartment() == null ? "Chưa xác định" : requisition.getDepartment().getDepartmentName())
                .hiringManagerName(requisition.getHiringManager() != null ? requisition.getHiringManager().getFullName() : "Chưa phân công")
                .numberOfPositions(requisition.getNumberOfPositions())
                .employmentType(requisition.getEmploymentType())
                .approvalStatus(requisition.getApprovalStatus())
                .createdAt(requisition.getCreatedAt())
                .postingStatus(postingStatus)
                .jobPostingId(jobPostingId)
                .minSalary(requisition.getMinSalary())
                .maxSalary(requisition.getMaxSalary())
                .gender(requisition.getGender())
                .workLocation(requisition.getWorkLocation())
                .workModel(requisition.getWorkModel())
                .probationDuration(requisition.getProbationDuration())
                .expectedStartDate(requisition.getExpectedStartDate())
                .reasonForHiring(requisition.getReasonForHiring())
                .jobDescription(requisition.getJobDescription())
                .requirementDetails(requisition.getRequirementDetails())
                .editable(isEditable)
                .deletable(isEditable && !jobPostingRepository.existsByRequisition_RequisitionId(requisition.getRequisitionId()))
                .decidable(requisitionAccess.canDecide(currentUser, requisition))
                .withdrawable(requisitionAccess.owns(currentUser, requisition) && RequisitionAccess.STATUS_PENDING_DIRECTOR.equals(requisition.getApprovalStatus()))
                .build();

        if (includeDetails) {
            if (RequisitionAccess.STATUS_DRAFT.equals(requisition.getApprovalStatus())) {
                RequisitionRequest submissionRequest = toRequestDto(requisition);
                submissionRequest.setAction(ACTION_SUBMIT);
                try {
                    requisitionValidator.validate(submissionRequest);
                } catch (RequisitionValidationException validationException) {
                    response.setIncomplete(true);
                }
            }

            response.setScreeningCriteria(requisition.getScreeningCriteria().stream().map(criterion -> {
                ScreeningCriteriaResponse criteriaResponse = new ScreeningCriteriaResponse();
                criteriaResponse.setCriteriaId(criterion.getCriteriaId());
                criteriaResponse.setCriteriaName(criterion.getCriteriaName());
                criteriaResponse.setCriteriaType(criterion.getCriteriaType());
                criteriaResponse.setRequiredValue(criterion.getRequiredValue());
                criteriaResponse.setWeight(criterion.getWeight());
                criteriaResponse.setIsMandatory(criterion.getIsMandatory());
                return criteriaResponse;
            }).toList());

            response.setApprovals(approvalRepository.findByRequisition_RequisitionIdOrderByApprovalDateDesc(requisition.getRequisitionId()).stream()
                    .map(approval -> ApprovalResponse.builder()
                            .approverName(approval.getDirector().getFullName())
                            .status(approval.getStatus())
                            .approvalDate(approval.getApprovalDate())
                            .comments(approval.getComments())
                            .build())
                    .toList());

            response.setActivityLog(auditLogRepository.findByEntityNameAndEntityIdOrderByTimestampDesc("JobRequisition", requisition.getRequisitionId().toString()).stream()
                    .map(auditLog -> ActivityLogResponse.builder()
                            .auditLogId(auditLog.getAuditLogId())
                            .action(auditLog.getAction())
                            .performedBy(auditLog.getUser().getFullName())
                            .description(auditLog.getNewValue())
                            .timestamp(auditLog.getTimestamp())
                            .build())
                    .toList());

            response.setTimeline(Collections.emptyList());

            if (RequisitionAccess.STATUS_REJECTED.equals(requisition.getApprovalStatus()) && !response.getApprovals().isEmpty()) {
                response.setRejectionReason(response.getApprovals().getFirst().getComments());
            }
        }

        return response;
    }
}
