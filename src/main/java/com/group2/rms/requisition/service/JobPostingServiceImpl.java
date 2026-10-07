package com.group2.rms.requisition.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.requisition.dto.InternalJobPostingResponse;
import com.group2.rms.requisition.dto.JobPostingCreateRequest;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.requisition.repository.JobPostingRepository;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.user.entity.User;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobPostingServiceImpl implements JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final JobRequisitionRepository jobRequisitionRepository;
    private final AuditLogRepository auditLogRepository;
    private final RequisitionAccess requisitionAccess;

    private void requireHrOrAdmin(User currentUser) {
        String role = requisitionAccess.role(currentUser);
        if (!RequisitionAccess.ROLE_HR.equals(role) && !RequisitionAccess.ROLE_SYSTEM_ADMIN.equals(role)) {
            throw new AccessDeniedException("Chỉ HR hoặc Quản trị viên hệ thống mới có quyền quản lý tin tuyển dụng.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InternalJobPostingResponse> searchInternalJobPostings(
            int page,
            int size,
            String q,
            Integer departmentId,
            String status,
            String sortOrder,
            User currentUser) {

        requireHrOrAdmin(currentUser);

        Specification<JobPosting> spec = Specification.where(null);

        if (q != null && !q.isBlank()) {
            String trimmed = q.trim();
            spec = spec.and((root, query, cb) -> {
                String pattern = "%" + trimmed.toLowerCase() + "%";
                return cb.or(
                        cb.like(cb.lower(root.get("postingTitle")), pattern),
                        cb.like(cb.lower(root.join("requisition", JoinType.LEFT).get("requisitionCode")), pattern)
                );
            });
        }

        if (departmentId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.join("requisition", JoinType.LEFT).join("department", JoinType.LEFT).get("departmentId"), departmentId));
        }

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("postingStatus"), status.trim()));
        }

        int pageSize = Math.max(1, Math.min(size, 50));
        Sort sort = switch (Objects.toString(sortOrder, "newest")) {
            case "oldest" -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("jobPostingId"));
            case "title_asc" -> Sort.by(Sort.Order.asc("postingTitle"), Sort.Order.desc("jobPostingId"));
            case "title_desc" -> Sort.by(Sort.Order.desc("postingTitle"), Sort.Order.desc("jobPostingId"));
            case "deadline_asc" -> Sort.by(Sort.Order.asc("applicationDeadline"), Sort.Order.desc("jobPostingId"));
            default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("jobPostingId"));
        };

        Page<JobPosting> pageResult = jobPostingRepository.findAll(
                spec,
                PageRequest.of(Math.max(0, page - 1), pageSize, sort)
        );

        if (pageResult.getTotalPages() > 0 && pageResult.getNumber() >= pageResult.getTotalPages()) {
            pageResult = jobPostingRepository.findAll(
                    spec,
                    PageRequest.of(pageResult.getTotalPages() - 1, pageSize, sort)
            );
        }

        return pageResult.map(this::toInternalResponse);
    }

    private InternalJobPostingResponse toInternalResponse(JobPosting posting) {
        JobRequisition req = posting.getRequisition();
        return InternalJobPostingResponse.builder()
                .jobPostingId(posting.getJobPostingId())
                .postingTitle(posting.getPostingTitle())
                .requisitionId(req != null ? req.getRequisitionId() : null)
                .requisitionCode(req != null ? req.getRequisitionCode() : "—")
                .departmentId(req != null && req.getDepartment() != null ? req.getDepartment().getDepartmentId() : null)
                .departmentName(req != null && req.getDepartment() != null ? req.getDepartment().getDepartmentName() : "Chưa xác định")
                .recruitmentRound(req != null && req.getRecruitmentRound() != null ? req.getRecruitmentRound() : 1)
                .employmentType(req != null ? formatEmploymentType(req.getEmploymentType()) : "—")
                .postingStatus(posting.getPostingStatus())
                .applicationDeadline(posting.getApplicationDeadline())
                .postingDate(posting.getPostingDate())
                .createdAt(posting.getCreatedAt())
                .createdByName(posting.getCreatedBy() != null ? posting.getCreatedBy().getFullName() : "—")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public JobPostingCreateRequest prepareCreateForm(Integer requisitionId, User currentUser) {
        requireHrOrAdmin(currentUser);

        if (requisitionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã yêu cầu tuyển dụng không hợp lệ.");
        }

        JobRequisition requisition = jobRequisitionRepository.findById(requisitionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu tuyển dụng với mã: " + requisitionId));

        if (!RequisitionAccess.STATUS_APPROVED.equals(requisition.getApprovalStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể tạo tin tuyển dụng từ yêu cầu đã được phê duyệt.");
        }

        boolean hasActivePosting = jobPostingRepository.existsByRequisition_RequisitionIdAndPostingStatusIn(
                requisitionId, List.of("Draft", "Published")
        );
        if (hasActivePosting) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu tuyển dụng này đã có tin tuyển dụng đang hoạt động hoặc đang ở bản nháp.");
        }

        // Tạo chuỗi mức lương gợi ý
        String salaryDisplay = formatSalary(requisition.getMinSalary(), requisition.getMaxSalary());

        // Tổng hợp yêu cầu ứng viên từ requirementDetails và tiêu chí sàng lọc bắt buộc
        StringBuilder requirements = new StringBuilder();
        if (requisition.getRequirementDetails() != null && !requisition.getRequirementDetails().isBlank()) {
            requirements.append(requisition.getRequirementDetails().trim());
        }
        if (requisition.getScreeningCriteria() != null && !requisition.getScreeningCriteria().isEmpty()) {
            List<ScreeningCriteria> mandatory = requisition.getScreeningCriteria().stream()
                    .filter(c -> Boolean.TRUE.equals(c.getIsMandatory()))
                    .toList();
            if (!mandatory.isEmpty()) {
                if (requirements.length() > 0) {
                    requirements.append("\n\nTiêu chí bắt buộc:\n");
                } else {
                    requirements.append("Tiêu chí bắt buộc:\n");
                }
                for (ScreeningCriteria c : mandatory) {
                    requirements.append("- ").append(c.getCriteriaName());
                    if (c.getRequiredValue() != null && !c.getRequiredValue().isBlank()) {
                        requirements.append(": ").append(c.getRequiredValue());
                    }
                    requirements.append("\n");
                }
            }
        }

        return JobPostingCreateRequest.builder()
                .requisitionId(requisition.getRequisitionId())
                .requisitionCode(requisition.getRequisitionCode())
                .postingTitle(requisition.getTitle())
                .departmentName(requisition.getDepartment() != null ? requisition.getDepartment().getDepartmentName() : "Chưa xác định")
                .recruitmentRound(requisition.getRecruitmentRound() != null ? requisition.getRecruitmentRound() : 1)
                .employmentType(formatEmploymentType(requisition.getEmploymentType()))
                .numberOfPositions(requisition.getNumberOfPositions())
                .hiringManagerName(requisition.getHiringManager() != null ? requisition.getHiringManager().getFullName() : "—")
                .jobDescription(requisition.getJobDescription())
                .jobRequirements(requirements.toString())
                .workLocation(requisition.getWorkLocation() != null ? requisition.getWorkLocation() : "")
                .salaryDisplay(salaryDisplay)
                .benefits("")
                .applicationDeadline(LocalDate.now().plusDays(30))
                .action("draft")
                .build();
    }

    @Override
    @Transactional
    public Integer createJobPosting(JobPostingCreateRequest request, User currentUser) {
        requireHrOrAdmin(currentUser);

        if (request.getRequisitionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu tuyển dụng nguồn không được để trống.");
        }

        JobRequisition requisition = jobRequisitionRepository.findById(request.getRequisitionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu tuyển dụng nguồn."));

        if (!RequisitionAccess.STATUS_APPROVED.equals(requisition.getApprovalStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể tạo tin tuyển dụng từ yêu cầu đã được phê duyệt.");
        }

        boolean hasActivePosting = jobPostingRepository.existsByRequisition_RequisitionIdAndPostingStatusIn(
                request.getRequisitionId(), List.of("Draft", "Published")
        );
        if (hasActivePosting) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu tuyển dụng này đã có tin tuyển dụng đang hoạt động hoặc đang ở bản nháp.");
        }

        boolean isPublish = "publish".equalsIgnoreCase(request.getAction());
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime deadline = null;
        if (request.getApplicationDeadline() != null) {
            deadline = request.getApplicationDeadline().atTime(23, 59, 59);
            if (deadline.isBefore(now)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn nộp hồ sơ phải sau ngày hiện tại.");
            }
        }

        JobPosting jobPosting = JobPosting.builder()
                .requisition(requisition)
                .postingTitle(request.getPostingTitle().trim())
                .jobDescription(request.getJobDescription().trim())
                .jobRequirements(request.getJobRequirements().trim())
                .benefits(request.getBenefits() != null && !request.getBenefits().isBlank() ? request.getBenefits().trim() : null)
                .salaryDisplay(request.getSalaryDisplay() != null && !request.getSalaryDisplay().isBlank() ? request.getSalaryDisplay().trim() : null)
                .workLocation(request.getWorkLocation() != null && !request.getWorkLocation().isBlank() ? request.getWorkLocation().trim() : null)
                .postingDate(isPublish ? now : null)
                .applicationDeadline(deadline)
                .postingStatus(isPublish ? "Published" : "Draft")
                .createdBy(currentUser)
                .build();

        JobPosting saved = jobPostingRepository.save(jobPosting);

        auditLogRepository.save(AuditLog.builder()
                .user(currentUser)
                .action("CREATE")
                .entityName("JobPosting")
                .entityId(String.valueOf(saved.getJobPostingId()))
                .newValue(String.format("Tạo tin tuyển dụng: %s · %s", saved.getPostingTitle(), saved.getPostingStatus()))
                .timestamp(now)
                .build());

        return saved.getJobPostingId();
    }

    private String formatSalary(BigDecimal min, BigDecimal max) {
        if (min != null && max != null) {
            return String.format("%,d - %,d VNĐ", min.longValue(), max.longValue()).replace(',', '.');
        } else if (min != null) {
            return String.format("Từ %,d VNĐ", min.longValue()).replace(',', '.');
        } else if (max != null) {
            return String.format("Lên đến %,d VNĐ", max.longValue()).replace(',', '.');
        }
        return "Thoả thuận";
    }

    private String formatEmploymentType(String type) {
        if (type == null) return "—";
        return switch (type) {
            case "Full-time" -> "Toàn thời gian";
            case "Part-time" -> "Bán thời gian";
            case "Internship" -> "Thực tập";
            case "Contract" -> "Hợp đồng";
            default -> type;
        };
    }
}
