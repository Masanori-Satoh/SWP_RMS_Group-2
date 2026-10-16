package com.group2.rms.jobposting.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.InternalJobPostingResponse;
import com.group2.rms.jobposting.dto.JobPostingDashboardResponse;
import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.jobposting.validator.JobPostingValidator;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.candidate.service.TalentPoolService;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.user.entity.User;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobPostingServiceImpl implements JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final JobRequisitionRepository jobRequisitionRepository;
    private final AuditLogRepository auditLogRepository;
    private final RequisitionAccess requisitionAccess;
    private final NotificationService notificationService;
    private final JobPostingValidator validator;
    private final TalentPoolService talentPoolService;
    private final ApplicationRepository applicationRepository;

    private void requireHrOrAdmin(User currentUser) {
        String role = requisitionAccess.role(currentUser);
        if (!RequisitionAccess.ROLE_HR.equals(role) && !RequisitionAccess.ROLE_SYSTEM_ADMIN.equals(role)) {
            throw new AccessDeniedException("Chỉ HR hoặc Quản trị viên hệ thống mới có quyền quản lý tin tuyển dụng.");
        }
    }

    private void requirePostingReader(User user) {
        if (!RequisitionAccess.ROLE_HIRING_MANAGER.equals(requisitionAccess.role(user))) requireHrOrAdmin(user);
    }

    @Override
    @Transactional(readOnly = true)
    public JobPostingDashboardResponse getDashboard(User currentUser) {
        requireHrOrAdmin(currentUser);
        List<JobPosting> postings = jobPostingRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        long active = postings.stream().filter(p -> "Published".equals(p.getPostingStatus())
                && (p.getApplicationDeadline() == null || p.getApplicationDeadline().isAfter(now))).count();
        long drafts = postings.stream().filter(p -> "Draft".equals(p.getPostingStatus())).count();
        long closed = postings.stream().filter(p -> "Closed".equals(p.getPostingStatus())
                || ("Published".equals(p.getPostingStatus()) && p.getApplicationDeadline() != null
                    && !p.getApplicationDeadline().isAfter(now))).count();

        Set<Integer> occupiedRequisitions = postings.stream()
                .filter(p -> List.of("Draft", "Published", "Paused").contains(p.getPostingStatus()))
                .map(p -> p.getRequisition().getRequisitionId())
                .collect(Collectors.toSet());
        List<JobPostingDashboardResponse.AwaitingRequisition> awaiting = jobRequisitionRepository
                .findByApprovalStatusOrderByCreatedAtDesc("Approved").stream()
                .filter(r -> !occupiedRequisitions.contains(r.getRequisitionId()))
                .map(r -> new JobPostingDashboardResponse.AwaitingRequisition(
                        r.getRequisitionId(), r.getTitle(), r.getRequisitionCode(),
                        r.getDepartment() == null ? null : r.getDepartment().getDepartmentName(),
                        r.getRecruitmentRound()))
                .toList();

        Map<String, Long> byDepartment = postings.stream()
                .filter(p -> p.getRequisition() != null && p.getRequisition().getDepartment() != null)
                .collect(Collectors.groupingBy(p -> p.getRequisition().getDepartment().getDepartmentName(),
                        Collectors.counting()));
        List<JobPostingDashboardResponse.DeptStat> departments = byDepartment.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new JobPostingDashboardResponse.DeptStat(e.getKey(), e.getValue(),
                        postings.isEmpty() ? 0 : (int) Math.round(e.getValue() * 100.0 / postings.size())))
                .toList();
        List<JobPostingDashboardResponse.RecentPosting> recent = postings.stream().sorted(Comparator.comparing(
                (JobPosting p) -> p.getPostingDate() != null ? p.getPostingDate() : p.getCreatedAt(),
                Comparator.nullsFirst(Comparator.naturalOrder())).reversed()).limit(5)
                .map(p -> new JobPostingDashboardResponse.RecentPosting(
                        p.getJobPostingId(), p.getPostingTitle(),
                        p.getRequisition() == null || p.getRequisition().getDepartment() == null
                                ? null : p.getRequisition().getDepartment().getDepartmentName(),
                        p.getPostingStatus(), p.getApplicationDeadline(), p.getPostingDate()))
                .toList();
        return new JobPostingDashboardResponse(active, drafts, closed, awaiting, departments, recent);
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

        requirePostingReader(currentUser);

        Specification<JobPosting> spec = Specification.unrestricted();
        if (RequisitionAccess.ROLE_HIRING_MANAGER.equals(requisitionAccess.role(currentUser))) {
            Integer ownDepartment = currentUser.getDepartment() == null ? -1 : currentUser.getDepartment().getDepartmentId();
            spec = spec.and((root, query, cb) -> {
                var department = root.join("requisition").join("department");
                var manager = department.join("manager", JoinType.LEFT);
                return cb.or(cb.equal(department.get("departmentId"), ownDepartment),
                        cb.equal(manager.get("userId"), currentUser.getUserId()));
            });
        }

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

        int pageSize = Math.clamp(size, 1, 50);
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
        return prepareCreateFormInternal(requisitionId, currentUser, true);
    }

    private JobPostingCreateRequest prepareCreateFormInternal(Integer requisitionId, User currentUser, boolean checkExistingActive) {
        requireHrOrAdmin(currentUser);

        if (requisitionId == null) {
            throw new JobPostingValidationException("Mã yêu cầu tuyển dụng không hợp lệ.");
        }

        JobRequisition requisition = jobRequisitionRepository.findById(requisitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tuyển dụng với mã: " + requisitionId));

        validator.requireApproved(requisition);

        if (checkExistingActive) {
            requireNoOtherOpenPosting(requisitionId, null);
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
                if (!requirements.isEmpty()) {
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
                .workModel(requisition.getWorkModel() != null ? requisition.getWorkModel() : "Tại văn phòng")
                .numberOfPositions(requisition.getNumberOfPositions())
                .hiringManagerName(requisition.getHiringManager() != null ? requisition.getHiringManager().getFullName() : "—")
                .reasonForHiring(requisition.getReasonForHiring() != null ? requisition.getReasonForHiring() : "—")
                .minSalary(requisition.getMinSalary())
                .maxSalary(requisition.getMaxSalary())
                .probationDuration(requisition.getProbationDuration())
                .gender(requisition.getGender() != null ? requisition.getGender() : "Any")
                .expectedStartDate(requisition.getExpectedStartDate())
                .recruitmentDeadline(requisition.getExpectedStartDate())
                .isContinuousRecruitment(false)
                .jobDescription(requisition.getJobDescription())
                .jobRequirements(requirements.toString())
                .workLocation(requisition.getWorkLocation() != null ? requisition.getWorkLocation() : "")
                .salaryDisplay(salaryDisplay)
                .numberOfPositionsDisplay(requisition.getNumberOfPositions() == null ? null : String.valueOf(requisition.getNumberOfPositions()))
                .benefits("")
                .applicationDeadline(requisition.getExpectedStartDate() != null ? requisition.getExpectedStartDate() : LocalDate.now().plusDays(30))
                .action("draft")
                .build();
    }

    @Override
    @Transactional
    public Integer createJobPosting(JobPostingCreateRequest request, User currentUser) {
        return create(request, currentUser, false);
    }

    @Override
    @Transactional
    public Integer createAndPublishJobPosting(JobPostingCreateRequest request, User currentUser) {
        return create(request, currentUser, true);
    }

    private Integer create(JobPostingCreateRequest request, User actor, boolean publish) {
        requireHrOrAdmin(actor);
        if (request.getJobPostingId() != null || request.getRequisitionId() == null) {
            throw new JobPostingValidationException("Yêu cầu tạo tin không hợp lệ.");
        }
        JobRequisition requisition = jobRequisitionRepository.findForUpdate(request.getRequisitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tuyển dụng nguồn."));
        validator.requireApproved(requisition);
        requireNoOtherOpenPosting(requisition.getRequisitionId(), null);

        LocalDateTime now = LocalDateTime.now();
        JobPosting posting = new JobPosting();
        posting.setRequisition(requisition);
        // Requisition đã bị khóa ở trên nên số lần đăng kế tiếp không bị trùng (UQ_JobPosting_Requisition_PostingNo)
        posting.setPostingNo(jobPostingRepository.findMaxPostingNo(requisition.getRequisitionId()) + 1);
        posting.setCreatedBy(actor);
        applyEditableFields(posting, request, now, publish);
        posting.setPostingStatus(publish ? "Published" : "Draft");
        if (publish) posting.setPostingDate(now);

        JobPosting saved = jobPostingRepository.save(posting);
        audit(saved, actor, "CREATE", "Tạo tin tuyển dụng: " + saved.getPostingStatus());
        if (publish) notificationService.notifyJobPostingPublished(saved, actor);
        return saved.getJobPostingId();
    }

    @Override
    @Transactional
    public void updateJobPosting(Integer id, JobPostingCreateRequest request, User actor) {
        update(id, request, actor, false);
    }

    @Override
    @Transactional
    public void updateAndPublishJobPosting(Integer id, JobPostingCreateRequest request, User actor) {
        update(id, request, actor, true);
    }

    private void update(Integer id, JobPostingCreateRequest request, User actor, boolean publish) {
        requireHrOrAdmin(actor);
        JobPosting posting = findForUpdate(id);
        validator.requireEditable(posting.getPostingStatus());
        if (request.getJobPostingId() == null || !id.equals(request.getJobPostingId())
                || !posting.getRequisition().getRequisitionId().equals(request.getRequisitionId())) {
            throw new JobPostingValidationException("Không thể đổi tin hoặc yêu cầu tuyển dụng nguồn.");
        }
        validator.requireApproved(posting.getRequisition());
        if (publish && "Paused".equals(posting.getPostingStatus())) {
            requireNoOtherOpenPosting(posting.getRequisition().getRequisitionId(), id);
        }
        boolean newPublication = publish && !"Published".equals(posting.getPostingStatus());
        LocalDateTime now = LocalDateTime.now();
        applyEditableFields(posting, request, now, publish || "Published".equals(posting.getPostingStatus()));
        if (publish) {
            posting.setPostingStatus("Published");
            if (newPublication) posting.setPostingDate(now);
        }
        jobPostingRepository.save(posting);
        audit(posting, actor, "UPDATE", newPublication ? "Đăng tin tuyển dụng" : "Cập nhật tin tuyển dụng");
        if (newPublication) notificationService.notifyJobPostingPublished(posting, actor);
    }

    @Override
    @Transactional
    public void publishJobPosting(Integer id, User actor) {
        requireHrOrAdmin(actor);
        JobPosting posting = findForUpdate(id);
        if (!"Draft".equals(posting.getPostingStatus()) && !"Paused".equals(posting.getPostingStatus())) {
            throw new JobPostingValidationException("Chỉ tin nháp hoặc đang tạm dừng mới được đăng.");
        }
        validator.requireApproved(posting.getRequisition());
        validator.requirePublishableContent(posting);
        requireNoOtherOpenPosting(posting.getRequisition().getRequisitionId(), id);
        if (posting.getApplicationDeadline() != null && !posting.getApplicationDeadline().isAfter(LocalDateTime.now())) {
            throw new JobPostingValidationException("Hạn nộp đã qua. Hãy cập nhật tin trước khi đăng.");
        }
        posting.setPostingStatus("Published");
        posting.setPostingDate(LocalDateTime.now());
        jobPostingRepository.save(posting);
        audit(posting, actor, "UPDATE", "Đăng tin tuyển dụng");
        notificationService.notifyJobPostingPublished(posting, actor);
    }

    @Override
    @Transactional
    public void pauseJobPosting(Integer id, User actor) {
        requireHrOrAdmin(actor);
        JobPosting posting = findForUpdate(id);
        if (!"Published".equals(posting.getPostingStatus())) {
            throw new JobPostingValidationException("Chỉ tin đang đăng mới được tạm dừng.");
        }
        posting.setPostingStatus("Paused");
        jobPostingRepository.save(posting);
        audit(posting, actor, "UPDATE", "Tạm dừng tin tuyển dụng");
    }

    @Override
    public void closeJobPosting(Integer id, User actor) {
        if (!RequisitionAccess.ROLE_HR.equals(requisitionAccess.role(actor))) {
            throw new AccessDeniedException("Chỉ HR được đóng đợt tuyển dụng.");
        }
        talentPoolService.closeJob(id);
    }

    private JobPosting findForUpdate(Integer id) {
        if (id == null) throw new JobPostingValidationException("Mã tin tuyển dụng không hợp lệ.");
        return jobPostingRepository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng."));
    }

    private JobPosting findById(Integer id) {
        if (id == null) throw new JobPostingValidationException("Mã tin tuyển dụng không hợp lệ.");
        return jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng với mã: " + id));
    }

    private void requireNoOtherOpenPosting(Integer requisitionId, Integer currentId) {
        boolean exists = currentId == null
                ? jobPostingRepository.existsByRequisition_RequisitionIdAndPostingStatusIn(
                        requisitionId, List.of("Draft", "Published", "Paused"))
                : jobPostingRepository.existsByRequisition_RequisitionIdAndJobPostingIdNotAndPostingStatusIn(
                        requisitionId, currentId, List.of("Draft", "Published", "Paused"));
        if (exists) throw new JobPostingValidationException("Yêu cầu này đã có tin nháp hoặc tin đang hoạt động.");
    }

    private void applyEditableFields(JobPosting posting, JobPostingCreateRequest request,
            LocalDateTime now, boolean publishing) {
        if (request.getPostingTitle() == null || request.getPostingTitle().isBlank()
                || request.getJobDescription() == null || request.getJobDescription().isBlank()
                || request.getJobRequirements() == null || request.getJobRequirements().isBlank()) {
            throw new JobPostingValidationException("Tiêu đề, mô tả và yêu cầu ứng viên không được để trống.");
        }
        posting.setPostingTitle(request.getPostingTitle().trim());
        posting.setJobDescription(request.getJobDescription().trim());
        posting.setJobRequirements(request.getJobRequirements().trim());
        posting.setBenefits(blankToNull(request.getBenefits()));
        posting.setSalaryDisplay(blankToNull(request.getSalaryDisplay()));
        posting.setWorkLocation(blankToNull(request.getWorkLocation()));
        posting.setNumberOfPositionsDisplay(blankToNull(request.getNumberOfPositionsDisplay()));
        posting.setApplicationDeadline(validator.deadline(request, now, publishing));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void audit(JobPosting posting, User actor, String action, String detail) {
        auditLogRepository.save(AuditLog.builder()
                .user(actor).action(action).entityName("JobPosting")
                .entityId(String.valueOf(posting.getJobPostingId()))
                .newValue(detail + ": " + posting.getPostingTitle())
                .timestamp(LocalDateTime.now()).build());
    }

    @Override
    @Transactional(readOnly = true)
    public JobPostingCreateRequest prepareEditForm(Integer jobPostingId, User currentUser) {
        requireHrOrAdmin(currentUser);

        JobPosting posting = findById(jobPostingId);
        validator.requireEditable(posting.getPostingStatus());

        JobRequisition req = posting.getRequisition();
        JobPostingCreateRequest form = prepareCreateFormInternal(req.getRequisitionId(), currentUser, false);
        form.setJobPostingId(posting.getJobPostingId());
        form.setPostingStatus(posting.getPostingStatus());
        form.setPostingTitle(posting.getPostingTitle());
        form.setJobDescription(posting.getJobDescription());
        form.setJobRequirements(posting.getJobRequirements());
        form.setBenefits(posting.getBenefits());
        form.setSalaryDisplay(posting.getSalaryDisplay());
        form.setWorkLocation(posting.getWorkLocation());
        form.setNumberOfPositionsDisplay(posting.getNumberOfPositionsDisplay());
        form.setApplicationDeadline(posting.getApplicationDeadline() != null ? posting.getApplicationDeadline().toLocalDate() : null);
        form.setIsContinuousRecruitment(posting.getApplicationDeadline() == null);
        form.setAction("draft".equalsIgnoreCase(posting.getPostingStatus()) ? "draft" : "publish");

        return form;
    }

    @Override
    @Transactional
    public void deleteJobPosting(Integer id, User currentUser) {
        requireHrOrAdmin(currentUser);

        JobPosting posting = findById(id);

        if (!"Draft".equals(posting.getPostingStatus()) || applicationRepository.existsByJobPosting_JobPostingId(id)) {
            throw new JobPostingValidationException("Chỉ có thể xóa bản nháp chưa có hồ sơ ứng tuyển.");
        }

        jobPostingRepository.delete(posting);

        auditLogRepository.save(AuditLog.builder()
                .user(currentUser)
                .action("DELETE")
                .entityName("JobPosting")
                .entityId(String.valueOf(id))
                .newValue(String.format("Xóa tin tuyển dụng: %s", posting.getPostingTitle()))
                .timestamp(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public InternalJobPostingDetailResponse getInternalJobPostingDetail(Integer id, User currentUser) {
        requirePostingReader(currentUser);

        JobPosting posting = findById(id);

        JobRequisition req = posting.getRequisition();

        if (RequisitionAccess.ROLE_HIRING_MANAGER.equals(requisitionAccess.role(currentUser))
                && !requisitionAccess.inManagedDepartment(currentUser, req))
            throw new AccessDeniedException("Tin tuyển dụng không thuộc phòng ban của bạn.");

        String statusLabel = switch (posting.getPostingStatus()) {
            case "Published" -> "Đang đăng tuyển";
            case "Draft" -> "Bản nháp";
            case "Paused" -> "Tạm dừng";
            case "Closed" -> "Đã đóng";
            default -> posting.getPostingStatus();
        };

        List<String> criteriaList = (req != null && req.getScreeningCriteria() != null)
                ? req.getScreeningCriteria().stream()
                        .map(c -> c.getCriteriaName() + " (Trọng số: " + c.getWeight() + "%)")
                        .collect(Collectors.toList())
                : List.of();

        // Rule: gather audit history logs or synthesize for job posting
        List<InternalJobPostingDetailResponse.ActivityHistoryItem> history = buildActivityHistory(posting);

        return InternalJobPostingDetailResponse.builder()
                .jobPostingId(posting.getJobPostingId())
                .postingTitle(posting.getPostingTitle())
                .requisitionId(req != null ? req.getRequisitionId() : null)
                .requisitionCode(req != null ? req.getRequisitionCode() : "—")
                .requisitionTitle(req != null ? req.getTitle() : "—")
                .requisitionApprovalStatus(req != null ? req.getApprovalStatus() : "—")
                .departmentId(req != null && req.getDepartment() != null ? req.getDepartment().getDepartmentId() : null)
                .departmentName(req != null && req.getDepartment() != null ? req.getDepartment().getDepartmentName() : "Chưa xác định")
                .recruitmentRound(req != null && req.getRecruitmentRound() != null ? req.getRecruitmentRound() : 1)
                .numberOfPositions(req != null ? req.getNumberOfPositions() : 1)
                .employmentType(req != null ? formatEmploymentType(req.getEmploymentType()) : "—")
                .workModel(req != null && req.getWorkModel() != null ? req.getWorkModel() : "Tại văn phòng")
                .workLocation(posting.getWorkLocation() != null ? posting.getWorkLocation() : (req != null ? req.getWorkLocation() : "—"))
                .salaryDisplay(posting.getSalaryDisplay() != null ? posting.getSalaryDisplay() : "Thoả thuận")
                .numberOfPositionsDisplay(posting.getNumberOfPositionsDisplay())
                .postingNo(posting.getPostingNo())
                .closedAt(posting.getClosedAt())
                .jobDescription(posting.getJobDescription())
                .jobRequirements(posting.getJobRequirements())
                .benefits(posting.getBenefits())
                .postingStatus(posting.getPostingStatus())
                .postingStatusLabel(statusLabel)
                .postingDate(posting.getPostingDate())
                .applicationDeadline(posting.getApplicationDeadline())
                .createdAt(posting.getCreatedAt())
                .updatedAt(posting.getUpdatedAt())
                .createdByName(posting.getCreatedBy() != null ? posting.getCreatedBy().getFullName() : "—")
                .createdByEmail(posting.getCreatedBy() != null ? posting.getCreatedBy().getEmail() : null)
                .hiringManagerName(req != null && req.getHiringManager() != null ? req.getHiringManager().getFullName() : "—")
                .hiringManagerEmail(req != null && req.getHiringManager() != null ? req.getHiringManager().getEmail() : null)
                .screeningCriteria(criteriaList)
                .activityHistory(history)
                .build();
    }

    // Rule: build structured audit history for job posting detail view
    private List<InternalJobPostingDetailResponse.ActivityHistoryItem> buildActivityHistory(JobPosting posting) {
        List<AuditLog> logs = auditLogRepository.findByEntityNameAndEntityIdOrderByTimestampDesc(
                "JobPosting", String.valueOf(posting.getJobPostingId())
        );

        List<InternalJobPostingDetailResponse.ActivityHistoryItem> history = new ArrayList<>();
        if (logs != null && !logs.isEmpty()) {
            List<AuditLog> chronological = new ArrayList<>(logs);
            chronological.sort(Comparator.comparing(AuditLog::getTimestamp));
            int index = 1;
            for (AuditLog log : chronological) {
                history.add(InternalJobPostingDetailResponse.ActivityHistoryItem.builder()
                        .no(index++)
                        .action(activityAction(log))
                        .performedBy(log.getUser() != null ? log.getUser().getFullName() : "Hệ thống")
                        .timestamp(log.getTimestamp())
                        .build());
            }
        } else {
            // NOTE: Fallback synthesis when older records do not yet have audit log entries
            String author = posting.getCreatedBy() != null ? posting.getCreatedBy().getFullName() : "Hệ thống";
            int index = 1;
            if (posting.getCreatedAt() != null) {
                history.add(InternalJobPostingDetailResponse.ActivityHistoryItem.builder()
                        .no(index++)
                        .action("Tạo mới")
                        .performedBy(author)
                        .timestamp(posting.getCreatedAt())
                        .build());
            }
            if (posting.getUpdatedAt() != null && !posting.getUpdatedAt().equals(posting.getCreatedAt())) {
                history.add(InternalJobPostingDetailResponse.ActivityHistoryItem.builder()
                        .no(index++)
                        .action("Cập nhật")
                        .performedBy(author)
                        .timestamp(posting.getUpdatedAt())
                        .build());
            }
            if ("Published".equalsIgnoreCase(posting.getPostingStatus()) && posting.getPostingDate() != null) {
                history.add(InternalJobPostingDetailResponse.ActivityHistoryItem.builder()
                        .no(index)
                        .action("Đăng tin")
                        .performedBy(author)
                        .timestamp(posting.getPostingDate())
                        .build());
            }
        }
        return history;
    }

    private String activityAction(AuditLog log) {
        String detail = Objects.toString(log.getNewValue(), "");
        if ("UPDATE".equalsIgnoreCase(log.getAction())) {
            if (detail.startsWith("Đăng tin tuyển dụng") || detail.contains("Published")) return "Đăng tin";
            if (detail.startsWith("Tạm dừng tin tuyển dụng")) return "Tạm dừng";
            if (detail.equals("Closed")) return "Đóng tin";
            return "Cập nhật";
        }
        if ("CREATE".equalsIgnoreCase(log.getAction())) {
            return detail.startsWith("Tạo tin tuyển dụng: Published") ? "Tạo và đăng tin" : "Tạo mới";
        }
        if ("DELETE".equalsIgnoreCase(log.getAction())) return "Xóa";
        return "Tạo mới";
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
