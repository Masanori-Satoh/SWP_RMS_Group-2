package com.group2.rms.requisition.service;

import com.group2.rms.admin.dto.ActivityLogResponse;
import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.requisition.dto.ApprovalResponse;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.dto.RequisitionTimelineResponse;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaResponse;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.entity.RequisitionWorkflowEvent;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.repository.JobPostingRepository;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.repository.RequisitionApprovalRepository;
import com.group2.rms.requisition.repository.RequisitionWorkflowEventRepository;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final RequisitionWorkflowEventRepository events;
    private final RequisitionAccess access;
    private final RequisitionValidator validator;
    private final NotificationService notificationService;

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

        RequisitionRequest copiedDto = convertToRequestDto(jobRequisition);
        // Reset criteria IDs để khi tạo mới sẽ sinh bản ghi mới
        copiedDto.getScreeningCriteria().forEach(criterion -> criterion.setCriteriaId(null));
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
                .title(jobRequisition.getTitle())
                .departmentId(jobRequisition.getDepartment() == null ? null : jobRequisition.getDepartment().getDepartmentId())
                .numberOfPositions(jobRequisition.getNumberOfPositions())
                .employmentType(jobRequisition.getEmploymentType())
                .minSalary(jobRequisition.getMinSalary())
                .maxSalary(jobRequisition.getMaxSalary())
                .gender(jobRequisition.getGender())
                .workLocation(jobRequisition.getWorkLocation())
                .workModel(jobRequisition.getWorkModel())
                .probationDuration(jobRequisition.getProbationDuration())
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
        validator.validate(requestDto);

        // Rule: Tiêu chí mới không được chứa ID đã tồn tại
        if (requestDto.getScreeningCriteria().stream().anyMatch(c -> c.getCriteriaId() != null)) {
            throw createValidationException("screeningCriteria", "Tiêu chí mới không được tham chiếu bản ghi có sẵn.");
        }

        JobRequisition jobRequisition = new JobRequisition();
        jobRequisition.setHiringManager(currentUser);
        jobRequisition.setScreeningCriteria(new ArrayList<>());
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

        if (jobRequisition.getSubmittedAt() != null) {
            recordWorkflowEvent(jobRequisition, currentUser, "Submitted", "Đã gửi vào hàng đợi phê duyệt của Giám đốc.");
        }
        createAuditLog(jobRequisition, currentUser, "CREATE", null, "Tạo yêu cầu: " + jobRequisition.getTitle() + " · " + jobRequisition.getApprovalStatus());

        return jobRequisition.getRequisitionId();
    }

    @Override
    public void updateRequisition(Integer id, RequisitionRequest requestDto) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, true);
        // Rule: Chỉ cho phép chỉnh sửa khi còn là bản nháp và đúng người sở hữu
        access.requireEdit(currentUser, jobRequisition);
        validator.validate(requestDto);

        Map<String, String> beforeSnapshot = createSnapshot(jobRequisition);
        synchronizeCriteria(jobRequisition, requestDto.getScreeningCriteria());
        applyDataToEntity(jobRequisition, requestDto);

        // Rule: Cập nhật trạng thái khi nộp lại hoặc tiếp tục lưu nháp
        boolean isSubmitted = "submit".equals(requestDto.getAction());
        jobRequisition.setApprovalStatus(isSubmitted ? "Pending_Director" : "Draft");
        if (isSubmitted) {
            jobRequisition.setSubmittedAt(LocalDateTime.now());
            jobRequisition.setDecidedAt(null);
            recordWorkflowEvent(jobRequisition, currentUser, "Submitted", "Đã gửi vào hàng đợi phê duyệt của Giám đốc.");
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
        jobRequisition.setDepartment(requestDto.getDepartmentId() == null ? null : departments.findById(requestDto.getDepartmentId()).orElseThrow(() -> createValidationException("departmentId", "Phòng ban không tồn tại.")));
        jobRequisition.setNumberOfPositions(requestDto.getNumberOfPositions());
        jobRequisition.setEmploymentType(requestDto.getEmploymentType());
        jobRequisition.setMinSalary(requestDto.getMinSalary());
        jobRequisition.setMaxSalary(requestDto.getMaxSalary());
        jobRequisition.setGender(requestDto.getGender());
        jobRequisition.setWorkLocation(requestDto.getWorkLocation());
        jobRequisition.setWorkModel(requestDto.getWorkModel());
        jobRequisition.setProbationDuration(requestDto.getProbationDuration());
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
        events.deleteByRequisition_RequisitionId(id);
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
        jobRequisition.setDecidedAt(LocalDateTime.now());

        approvals.save(RequisitionApproval.builder()
                .requisition(jobRequisition)
                .director(currentUser)
                .status(nextStatus)
                .comments(cleanedComment)
                .build());

        requisitions.saveAndFlush(jobRequisition);
        recordWorkflowEvent(jobRequisition, currentUser, nextStatus, cleanedComment);

        createAuditLog(jobRequisition, currentUser, "UPDATE", "Status: Pending_Director",
                "Status: Pending_Director → " + nextStatus + (cleanedComment == null ? "" : "\nÝ kiến Giám đốc: " + cleanedComment));

        if (!approved) {
            notificationService.notifyRequisitionRejected(jobRequisition, currentUser, cleanedComment);
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
        recordWorkflowEvent(jobRequisition, currentUser, "Withdrawn", "Rút lại để chỉnh sửa.");
        createAuditLog(jobRequisition, currentUser, "UPDATE", "Status: Pending_Director", "Status: Pending_Director → Draft (rút lại)");
    }

    private JobRequisition findRequisitionById(Integer id, boolean lockForUpdate) {
        return (lockForUpdate ? requisitions.findForUpdate(id) : requisitions.findById(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu tuyển dụng."));
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

    private void recordWorkflowEvent(JobRequisition jobRequisition, User currentUser, String eventType, String comment) {
        events.save(RequisitionWorkflowEvent.builder()
                .requisition(jobRequisition)
                .actor(currentUser)
                .eventType(eventType)
                .occurredAt(LocalDateTime.now())
                .comment(comment)
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
        snapshotMap.put("Job title", jobRequisition.getTitle());
        snapshotMap.put("Department", jobRequisition.getDepartment() == null ? null : jobRequisition.getDepartment().getDepartmentName());
        snapshotMap.put("Openings", formatDisplayValue(jobRequisition.getNumberOfPositions()));
        snapshotMap.put("Employment type", jobRequisition.getEmploymentType());
        snapshotMap.put("Minimum salary", formatNumber(jobRequisition.getMinSalary()));
        snapshotMap.put("Maximum salary", formatNumber(jobRequisition.getMaxSalary()));
        snapshotMap.put("Gender", jobRequisition.getGender());
        snapshotMap.put("Location", jobRequisition.getWorkLocation());
        snapshotMap.put("Work model", jobRequisition.getWorkModel());
        snapshotMap.put("Probation duration", jobRequisition.getProbationDuration());
        snapshotMap.put("Expected start date", formatDisplayValue(jobRequisition.getExpectedStartDate()));
        snapshotMap.put("Reason for hiring", jobRequisition.getReasonForHiring());
        snapshotMap.put("Job description", jobRequisition.getJobDescription());
        snapshotMap.put("Candidate requirements", jobRequisition.getRequirementDetails());
        snapshotMap.put("Status", jobRequisition.getApprovalStatus());

        int criteriaIndex = 0;
        for (ScreeningCriteria criterion : jobRequisition.getScreeningCriteria()) {
            String criterionName = (criterion.getCriteriaName() == null ? "Tiêu chí " + (++criteriaIndex) : criterion.getCriteriaName());
            snapshotMap.put("Criterion: " + criterionName,
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
                .createdAt(jobRequisition.getCreatedAt())
                .minSalary(jobRequisition.getMinSalary())
                .maxSalary(jobRequisition.getMaxSalary())
                .gender(jobRequisition.getGender())
                .workLocation(jobRequisition.getWorkLocation())
                .workModel(jobRequisition.getWorkModel())
                .probationDuration(jobRequisition.getProbationDuration())
                .expectedStartDate(jobRequisition.getExpectedStartDate())
                .reasonForHiring(jobRequisition.getReasonForHiring())
                .jobDescription(jobRequisition.getJobDescription())
                .requirementDetails(jobRequisition.getRequirementDetails())
                .editable(isEditable)
                .deletable(isEditable && !postings.existsByRequisition_RequisitionId(jobRequisition.getRequisitionId()))
                .decidable(access.canDecide(currentUser, jobRequisition))
                .withdrawable(access.owns(currentUser, jobRequisition) && "Pending_Director".equals(jobRequisition.getApprovalStatus()))
                .build();

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

            responseDto.setTimeline(events.findByRequisition_RequisitionIdOrderByOccurredAtDescEventIdDesc(jobRequisition.getRequisitionId()).stream()
                    .map(event -> new RequisitionTimelineResponse(event.getEventType(), event.getActor().getFullName(), event.getOccurredAt(), event.getComment()))
                    .toList());

            if ("Rejected".equals(jobRequisition.getApprovalStatus()) && !responseDto.getApprovals().isEmpty()) {
                responseDto.setRejectionReason(responseDto.getApprovals().getFirst().getComments());
            }
        }
        return responseDto;
    }
}
