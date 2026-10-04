package com.group2.rms.requisition.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.group2.rms.requisition.dto.*;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.repository.RequisitionApprovalRepository;
import com.group2.rms.requisition.repository.ScreeningCriteriaRepository;
import org.springframework.stereotype.Service;

import com.group2.rms.admin.dto.ActivityLogResponse;
import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
public class RequisitionServiceImpl implements RequisitionService {

    private final JobRequisitionRepository requisitionRepo;
    private final UserRepository userRepo;
    private final DepartmentRepository departmentRepo;
    private final RequisitionApprovalRepository approvalRepo;
    private final ScreeningCriteriaRepository criteriaRepo;
    private final AuditLogRepository auditLogRepo;

    // Hệ thống hiện dùng mock user id=1 (Admin). Sau sẽ lấy từ SecurityContext.
    private static final Integer SYSTEM_USER_ID = 1;

    public RequisitionServiceImpl(
            JobRequisitionRepository requisitionRepo,
            UserRepository userRepo,
            DepartmentRepository departmentRepo,
            RequisitionApprovalRepository approvalRepo,
            ScreeningCriteriaRepository criteriaRepo,
            AuditLogRepository auditLogRepo) {
        this.requisitionRepo = requisitionRepo;
        this.userRepo = userRepo;
        this.departmentRepo = departmentRepo;
        this.approvalRepo = approvalRepo;
        this.criteriaRepo = criteriaRepo;
        this.auditLogRepo = auditLogRepo;
    }

    /** Ghi AuditLog an toàn - không ném exception nếu user không tìm thấy */
    private void writeLog(String action, String entityId, String description) {
        try {
            User actor = userRepo.findById(SYSTEM_USER_ID).orElse(null);
            if (actor == null)
                return;
            AuditLog log = AuditLog.builder()
                    .user(actor)
                    .action(action)
                    .entityName("JobRequisition")
                    .entityId(entityId)
                    .newValue(description)
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepo.save(log);
        } catch (Exception ignored) {
            // Không để lỗi log ảnh hưởng nghiệp vụ chính
        }
    }

    @Override
    public RequisitionResponse getById(Integer id) {
        JobRequisition req = requisitionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Not find Requisition with ID: " + id));
        return convertToDto(req);
    }

    @Override
    public RequisitionRequest getRequestDtoById(Integer id) {
        JobRequisition req = requisitionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Requisition not found with id: " + id));

        java.util.List<ScreeningCriteriaRequest> criteriaDtoList = new java.util.ArrayList<>();
        if (req.getScreeningCriteria() != null) {
            criteriaDtoList = req.getScreeningCriteria().stream().map(c -> {
                ScreeningCriteriaRequest cDto = new ScreeningCriteriaRequest();
                cDto.setCriteriaId(c.getCriteriaId());
                cDto.setCriteriaName(c.getCriteriaName());
                cDto.setCriteriaType(c.getCriteriaType());
                cDto.setRequiredValue(c.getRequiredValue());
                cDto.setWeight(c.getWeight());
                cDto.setIsMandatory(c.getIsMandatory());
                return cDto;
            }).collect(java.util.stream.Collectors.toList());
        }

        if (criteriaDtoList.isEmpty()) {
            ScreeningCriteriaRequest emptyCrit = new ScreeningCriteriaRequest();
            emptyCrit.setWeight(new java.math.BigDecimal(0));
            emptyCrit.setIsMandatory(false);
            criteriaDtoList.add(emptyCrit);
        }

        RequisitionRequest dto = new RequisitionRequest();
        dto.setTitle(req.getTitle());
        dto.setDepartmentId(req.getDepartment() != null ? req.getDepartment().getDepartmentId() : null);
        dto.setHiringManagerId(req.getHiringManager() != null ? req.getHiringManager().getUserId() : null);
        dto.setNumberOfPositions(req.getNumberOfPositions());
        dto.setEmploymentType(req.getEmploymentType());
        dto.setMinSalary(req.getMinSalary());
        dto.setMaxSalary(req.getMaxSalary());
        dto.setReasonForHiring(req.getReasonForHiring());
        dto.setJobDescription(req.getJobDescription());
        dto.setRequirementDetails(req.getRequirementDetails());
        dto.setScreeningCriteria(criteriaDtoList);

        return dto;
    }

    @Override
    public Page<RequisitionResponse> getAllRequisitions(int page, int size) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        Page<JobRequisition> pagedReqs = requisitionRepo.findAllByOrderByCreatedAtDesc(pageable);
        return pagedReqs.map(this::convertToDto);
    }

    @Override
    public List<RequisitionResponse> getByStatus(String status) {
        List<JobRequisition> list = requisitionRepo.findByApprovalStatusOrderByCreatedAtDesc(status);
        return list.stream().map(this::convertToDto).toList();
    }

    @Override
    public List<RequisitionResponse> getByHiringManager(Integer userId) {
        List<JobRequisition> list = requisitionRepo.findByHiringManager_UserIdOrderByCreatedAtDesc(userId);
        return list.stream().map(this::convertToDto).toList();
    }

    @Override
    public void createRequisition(RequisitionRequest dto, Integer hiringManagerId) {
        JobRequisition req = new JobRequisition();

        req.setTitle(dto.getTitle());
        req.setNumberOfPositions(dto.getNumberOfPositions());
        req.setEmploymentType(dto.getEmploymentType());
        req.setMinSalary(dto.getMinSalary());
        req.setMaxSalary(dto.getMaxSalary());
        req.setReasonForHiring(dto.getReasonForHiring());
        req.setJobDescription(dto.getJobDescription());
        req.setRequirementDetails(dto.getRequirementDetails());

        String statusLabel;
        if (dto.getAction() != null && dto.getAction().equals("draft")) {
            req.setApprovalStatus("Draft");
            statusLabel = "saved as Draft";
        } else {
            req.setApprovalStatus("Pending_Director");
            statusLabel = "submitted for Approval";
        }

        Integer deptId = dto.getDepartmentId();
        if (deptId != null) {
            Department dept = departmentRepo.findById(deptId)
                    .orElseThrow(() -> new RuntimeException("Not find any department with id: " + deptId));
            req.setDepartment(dept);
        }

        Integer hmId = dto.getHiringManagerId() != null ? dto.getHiringManagerId() : hiringManagerId;
        User hm = userRepo.findById(hmId)
                .orElseThrow(() -> new RuntimeException("Not found Hiring Manager with id: " + hmId));
        req.setHiringManager(hm);

        requisitionRepo.save(req);

        // Ghi criteria vào collection entity để Hibernate quản lý nhất quán
        if (dto.getScreeningCriteria() != null) {
            List<ScreeningCriteria> criteriaList = dto.getScreeningCriteria().stream()
                    .filter(c -> c.getCriteriaName() != null && !c.getCriteriaName().trim().isEmpty())
                    .map(c -> {
                        ScreeningCriteria entity = new ScreeningCriteria();
                        entity.setRequisition(req);
                        entity.setCriteriaName(c.getCriteriaName());
                        entity.setCriteriaType(c.getCriteriaType());
                        entity.setRequiredValue(c.getRequiredValue());
                        entity.setWeight(c.getWeight() != null ? c.getWeight() : java.math.BigDecimal.ONE);
                        entity.setIsMandatory(c.getIsMandatory() != null ? c.getIsMandatory() : false);
                        return entity;
                    }).toList();
            criteriaRepo.saveAll(criteriaList);
        }

        // Ghi Activity Log
        writeLog("CREATE", String.valueOf(req.getRequisitionId()),
                "Requisition \"" + req.getTitle() + "\" " + statusLabel + " with " + req.getNumberOfPositions()
                        + " position(s).");
    }

    @Override
    public void updateRequisition(Integer id, RequisitionRequest dto) {
        JobRequisition req = requisitionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Requisition not found with id: " + id));

        req.setTitle(dto.getTitle());
        req.setNumberOfPositions(dto.getNumberOfPositions());
        req.setEmploymentType(dto.getEmploymentType());
        req.setMinSalary(dto.getMinSalary());
        req.setMaxSalary(dto.getMaxSalary());
        req.setReasonForHiring(dto.getReasonForHiring());
        req.setJobDescription(dto.getJobDescription());
        req.setRequirementDetails(dto.getRequirementDetails());

        String actionLabel;
        if ("draft".equals(dto.getAction())) {
            req.setApprovalStatus("Draft");
            actionLabel = "saved as Draft";
        } else if ("submit".equals(dto.getAction())) {
            req.setApprovalStatus("Pending_Director");
            actionLabel = "submitted to Director";
        } else {
            // action="save" hoặc null → giữ nguyên status hiện tại
            actionLabel = "updated (status kept: " + req.getApprovalStatus() + ")";
        }

        if (dto.getDepartmentId() != null) {
            Department dept = departmentRepo.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Not find any department"));
            req.setDepartment(dept);
        }

        if (dto.getHiringManagerId() != null) {
            User hm = userRepo.findById(dto.getHiringManagerId())
                    .orElseThrow(() -> new RuntimeException("Not found Hiring Manager"));
            req.setHiringManager(hm);
        }

        // FIX: Thay vì dùng criteriaRepo.deleteByRequisition_RequisitionId (gây
        // conflict với
        // orphanRemoval=true trên entity collection), ta clear collection của entity và
        // add lại. Hibernate sẽ tự quản lý DELETE + INSERT nhất quán.
        if (req.getScreeningCriteria() == null) {
            req.setScreeningCriteria(new ArrayList<>());
        }
        req.getScreeningCriteria().clear();

        if (dto.getScreeningCriteria() != null) {
            dto.getScreeningCriteria().stream()
                    .filter(c -> c.getCriteriaName() != null && !c.getCriteriaName().trim().isEmpty())
                    .forEach(c -> {
                        ScreeningCriteria entity = new ScreeningCriteria();
                        entity.setRequisition(req);
                        entity.setCriteriaName(c.getCriteriaName());
                        entity.setCriteriaType(c.getCriteriaType());
                        entity.setRequiredValue(c.getRequiredValue());
                        entity.setWeight(c.getWeight() != null ? c.getWeight() : java.math.BigDecimal.ONE);
                        entity.setIsMandatory(c.getIsMandatory() != null ? c.getIsMandatory() : false);
                        req.getScreeningCriteria().add(entity);
                    });
        }

        // Hibernate tự flush update + cascade criteria
        requisitionRepo.save(req);

        // Ghi Activity Log
        writeLog("UPDATE", String.valueOf(id),
                "Requisition \"" + req.getTitle() + "\" " + actionLabel + ". Positions: " + req.getNumberOfPositions()
                        + ".");
    }

    @Override
    public void deleteRequisition(Integer id) {
        JobRequisition req = requisitionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Requisition not found with id: " + id));

        String title = req.getTitle();

        // Ghi log TRƯỚC khi xoá (vì sau xoá không còn entity)
        writeLog("DELETE", String.valueOf(id),
                "Requisition \"" + title + "\" (ID: " + id + ") was permanently deleted.");

        var approvals = approvalRepo.findByRequisition_RequisitionIdOrderByApprovalDateDesc(id);
        if (approvals != null && !approvals.isEmpty()) {
            approvalRepo.deleteAll(approvals);
        }

        // Dùng collection entity để orphanRemoval tự dọn criteria
        if (req.getScreeningCriteria() != null) {
            req.getScreeningCriteria().clear();
        }

        requisitionRepo.delete(req);
    }

    @Override
    public void submitForApproval(Integer id) {
    }

    @Override
    public void approveRequisition(Integer id, Integer directorId, String comment) {
    }

    @Override
    public void rejectRequisition(Integer id, Integer directorId, String comment) {
    }

    private RequisitionResponse convertToDto(JobRequisition req) {
        java.util.List<ScreeningCriteriaResponse> criteriaDtoList = null;
        if (req.getScreeningCriteria() != null) {
            criteriaDtoList = req.getScreeningCriteria().stream().map(c -> {
                ScreeningCriteriaResponse cDto = new ScreeningCriteriaResponse();
                cDto.setCriteriaId(c.getCriteriaId());
                cDto.setCriteriaName(c.getCriteriaName());
                cDto.setCriteriaType(c.getCriteriaType());
                cDto.setRequiredValue(c.getRequiredValue());
                cDto.setWeight(c.getWeight());
                cDto.setIsMandatory(c.getIsMandatory());
                return cDto;
            }).toList();
        }

        var approvalsList = approvalRepo.findByRequisition_RequisitionIdOrderByApprovalDateDesc(req.getRequisitionId());
        java.util.List<ApprovalResponse> approvalDtoList = new java.util.ArrayList<>();
        if (approvalsList != null) {
            int step = 1;
            for (var a : approvalsList) {
                approvalDtoList.add(ApprovalResponse.builder()
                        .stepNumber(step++)
                        .approverName(a.getDirector() != null ? a.getDirector().getFullName() : "Approver")
                        .status(a.getStatus())
                        .approvalDate(a.getApprovalDate())
                        .comments(a.getComments())
                        .build());
            }
        }

        // Load activity log
        List<ActivityLogResponse> activityLogList = new ArrayList<>();
        var auditLogs = auditLogRepo.findByEntityNameAndEntityIdOrderByTimestampDesc(
                "JobRequisition", String.valueOf(req.getRequisitionId()));
        if (auditLogs != null) {
            for (var log : auditLogs) {
                activityLogList.add(ActivityLogResponse.builder()
                        .auditLogId(log.getAuditLogId())
                        .action(log.getAction())
                        .performedBy(log.getUser() != null ? log.getUser().getFullName() : "System")
                        .description(log.getNewValue())
                        .timestamp(log.getTimestamp())
                        .build());
            }
        }

        return RequisitionResponse.builder()
                .requisitionId(req.getRequisitionId())
                .title(req.getTitle())
                .departmentName(req.getDepartment() != null ? req.getDepartment().getDepartmentName() : "N/A")
                .hiringManagerName(req.getHiringManager() != null ? req.getHiringManager().getFullName() : "N/A")
                .numberOfPositions(req.getNumberOfPositions())
                .employmentType(req.getEmploymentType())
                .approvalStatus(req.getApprovalStatus())
                .createdAt(req.getCreatedAt())
                .minSalary(req.getMinSalary())
                .maxSalary(req.getMaxSalary())
                .reasonForHiring(req.getReasonForHiring())
                .jobDescription(req.getJobDescription())
                .requirementDetails(req.getRequirementDetails())
                .screeningCriteria(criteriaDtoList)
                .approvals(approvalDtoList)
                .activityLog(activityLogList)
                .build();
    }
}
