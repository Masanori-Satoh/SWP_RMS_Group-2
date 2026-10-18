package com.group2.rms.jobposting.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.InternalJobPostingResponse;
import com.group2.rms.jobposting.dto.JobPostingDashboardResponse;
import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.dto.JobPostingFilterOptions;
import com.group2.rms.jobposting.dto.JobPostingListResponse;
import com.group2.rms.jobposting.dto.JobPostingSearchRequest;
import com.group2.rms.core.dto.FilterOption;
import com.group2.rms.core.dto.FilterChip;
import com.group2.rms.core.util.ListFilters;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.jobposting.validator.JobPostingValidator;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.service.RequisitionLabels;
import com.group2.rms.candidate.service.TalentPoolService;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.service.ApplicationStatusLabels;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.user.entity.User;
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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
    private final DepartmentRepository departmentRepository;

    /**
     * Tin "đang mở" của một yêu cầu: mỗi yêu cầu chỉ được có 1 tin ở các trạng thái
     * này.
     */
    private static final List<String> OPEN_STATUSES = List.of(
            JobPostingLabels.STATUS_DRAFT, JobPostingLabels.STATUS_PUBLISHED, JobPostingLabels.STATUS_PAUSED);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Hạn nộp điền sẵn khi tạo tin: hôm nay + 30 ngày (đúng mockup). */
    private static final int DEFAULT_DEADLINE_DAYS = 30;
    /**
     * Chuỗi lương khi chọn "Thỏa thuận" (giống RequisitionLabels.salary khi yêu cầu
     * không có lương).
     */
    private static final String SALARY_NEGOTIABLE_TEXT = "Thỏa thuận";
    /** Số hồ sơ gần đây hiện ở tab "Ứng viên" trang chi tiết tin. */
    private static final int RECENT_APPLICATIONS = 7;
    /** Ngưỡng điểm AI mặc định khi chưa cấu hình AI_SCORE_THRESHOLD. */
    private static final BigDecimal DEFAULT_AI_THRESHOLD = new BigDecimal("70");

    private void requireHrOrAdmin(User currentUser) {
        String role = requisitionAccess.role(currentUser);
        if (!RequisitionAccess.ROLE_HR.equals(role) && !RequisitionAccess.ROLE_SYSTEM_ADMIN.equals(role)) {
            throw new AccessDeniedException("Chỉ HR hoặc Quản trị viên hệ thống mới có quyền quản lý tin tuyển dụng.");
        }
    }

    /**
     * Người được xem tin nội bộ: HR, System Admin, Hiring Manager, Director
     * (Director chỉ xem, không thao tác).
     */
    private void requirePostingReader(User user) {
        String role = requisitionAccess.role(user);
        if (!RequisitionAccess.ROLE_HIRING_MANAGER.equals(role) && !RequisitionAccess.ROLE_DIRECTOR.equals(role)) {
            requireHrOrAdmin(user);
        }
    }

    /** HR và System Admin được tạo / sửa / đăng tin. */
    private boolean canManage(User user) {
        String role = requisitionAccess.role(user);
        return RequisitionAccess.ROLE_HR.equals(role) || RequisitionAccess.ROLE_SYSTEM_ADMIN.equals(role);
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
                        && !p.getApplicationDeadline().isAfter(now)))
                .count();

        List<JobPostingDashboardResponse.AwaitingRequisition> awaiting = requisitionsAwaitingPosting();

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
                                ? null
                                : p.getRequisition().getDepartment().getDepartmentName(),
                        p.getPostingStatus(), p.getApplicationDeadline(), p.getPostingDate()))
                .toList();
        return new JobPostingDashboardResponse(active, drafts, closed, awaiting, departments, recent);
    }

    /**
     * Yêu cầu đã duyệt nhưng chưa có tin nháp / đang đăng / tạm dừng: HR cần tạo
     * tin cho chúng.
     */
    private List<JobPostingDashboardResponse.AwaitingRequisition> requisitionsAwaitingPosting() {
        Set<Integer> occupied = jobPostingRepository.findRequisitionIdsWithPostingStatusIn(OPEN_STATUSES);
        return jobRequisitionRepository.findByApprovalStatusOrderByCreatedAtDesc(RequisitionAccess.STATUS_APPROVED)
                .stream()
                .filter(r -> !occupied.contains(r.getRequisitionId()))
                .map(r -> new JobPostingDashboardResponse.AwaitingRequisition(
                        r.getRequisitionId(), r.getTitle(), r.getRequisitionCode(),
                        r.getDepartment() == null ? null : r.getDepartment().getDepartmentName(),
                        r.getRecruitmentRound()))
                .toList();
    }

    /**
     * Phạm vi dữ liệu theo vai trò (lớp bảo vệ theo dữ liệu, mọi bộ lọc khác được
     * nối thêm AND):
     * - HR, System Admin: mọi tin
     * - Hiring Manager: tin thuộc phòng ban của mình, trừ tin nháp
     * - Director: mọi tin, trừ tin nháp
     */
    private Specification<JobPosting> scope(User viewer) {
        String role = requisitionAccess.role(viewer);
        if (RequisitionAccess.ROLE_HIRING_MANAGER.equals(role)) {
            return JobPostingSpecifications.inDepartmentOf(viewer).and(JobPostingSpecifications.notDraft());
        }
        if (RequisitionAccess.ROLE_DIRECTOR.equals(role)) {
            return JobPostingSpecifications.notDraft();
        }
        return Specification.unrestricted();
    }

    /**
     * Chuẩn bị toàn bộ dữ liệu màn danh sách, theo 4 bước (giống màn yêu cầu tuyển
     * dụng):
     * 1. Vai trò được thấy ô lọc nào
     * 2. Làm sạch bộ lọc từ URL theo quyền
     * 3. Truy vấn DB + đếm hồ sơ
     * 4. Mô tả các bộ lọc đang bật thành chip
     */
    @Override
    @Transactional(readOnly = true)
    public JobPostingListResponse getListPage(JobPostingSearchRequest rawFilter, User viewer) {
        requirePostingReader(viewer);
        LocalDateTime now = LocalDateTime.now();
        JobPostingFilterOptions options = buildFilterOptions(viewer);
        JobPostingSearchRequest filter = normalizeFilter(rawFilter, options);
        Page<InternalJobPostingResponse> page = search(filter, viewer, now);
        List<FilterChip> chips = describeActiveFilters(filter, options);
        return new JobPostingListResponse(page, filter, options, chips, canManage(viewer));
    }

    /**
     * Bước 1: ô lọc theo vai trò (bảng trong plan_posting.md, Bước 2). Mọi vai trò
     * có ô Trạng thái và Sắp xếp.
     */
    private JobPostingFilterOptions buildFilterOptions(User viewer) {
        String role = requisitionAccess.role(viewer);
        List<FilterOption> sorts = sortOptions();
        List<FilterOption> deadlines = deadlineOptions();

        if (RequisitionAccess.ROLE_HIRING_MANAGER.equals(role)) {
            // HM chỉ thấy tin phòng mình nên không cần lọc phòng ban
            return new JobPostingFilterOptions(false, true, false, false,
                    statusOptions(JobPostingLabels.VIEW_STATUSES_WITHOUT_DRAFT), List.of(), List.of(), deadlines,
                    sorts);
        }
        if (RequisitionAccess.ROLE_DIRECTOR.equals(role)) {
            return new JobPostingFilterOptions(true, true, false, false,
                    statusOptions(JobPostingLabels.VIEW_STATUSES_WITHOUT_DRAFT), departmentOptions(), List.of(),
                    deadlines, sorts);
        }
        if (RequisitionAccess.ROLE_HR.equals(role)) {
            return new JobPostingFilterOptions(true, true, false, false,
                    statusOptions(JobPostingLabels.VIEW_STATUSES), departmentOptions(), List.of(), deadlines, sorts);
        }
        // System Admin: thêm lọc người tạo và ngày đăng, không có ô hạn nộp (đúng
        // mockup)
        return new JobPostingFilterOptions(true, false, true, true,
                statusOptions(JobPostingLabels.VIEW_STATUSES), departmentOptions(), creatorOptions(), List.of(), sorts);
    }

    /**
     * Bước 2: giá trị nào vai trò không được dùng, hoặc không có trong danh sách
     * lựa chọn, thì bỏ qua.
     */
    private JobPostingSearchRequest normalizeFilter(JobPostingSearchRequest raw, JobPostingFilterOptions options) {
        String keyword = ListFilters.cleanKeyword(raw.q());
        List<String> statuses = ListFilters.keepAllowedValues(raw.status(), options.statuses());
        Integer departmentId = options.showDepartment() && ListFilters.isAllowedValue(raw.departmentId(), options.departments())
                ? raw.departmentId()
                : null;
        Integer creatorId = options.showCreator() && ListFilters.isAllowedValue(raw.creatorId(), options.creators())
                ? raw.creatorId()
                : null;
        String deadline = options.showDeadline() && ListFilters.isAllowedValue(raw.deadline(), options.deadlines())
                ? raw.deadline()
                : null;
        LocalDate postedFrom = options.showPostedDate() ? raw.postedFrom() : null;
        LocalDate postedTo = options.showPostedDate() ? raw.postedTo() : null;

        String sort = JobPostingSort.fromParam(raw.sort()).param();
        int page = (raw.page() == null || raw.page() < 0) ? 0 : raw.page();
        int size = raw.size() == null ? DEFAULT_PAGE_SIZE : Math.max(1, Math.min(raw.size(), MAX_PAGE_SIZE));
        return new JobPostingSearchRequest(keyword, statuses, departmentId, creatorId, deadline,
                postedFrom, postedTo, sort, page, size);
    }

    /**
     * Bước 3: ghép điều kiện WHERE, truy vấn một trang, rồi đếm hồ sơ cho cả trang
     * bằng 1 câu.
     */
    private Page<InternalJobPostingResponse> search(JobPostingSearchRequest filter, User viewer, LocalDateTime now) {
        Specification<JobPosting> where = scope(viewer);
        if (!filter.q().isEmpty()) {
            where = where.and(JobPostingSpecifications.titleOrDepartmentContains(filter.q()));
        }
        if (!filter.status().isEmpty()) {
            where = where.and(JobPostingSpecifications.viewStatusIn(filter.status(), now));
        }
        if (filter.departmentId() != null) {
            where = where.and(JobPostingSpecifications.departmentIs(filter.departmentId()));
        }
        if (filter.creatorId() != null) {
            where = where.and(JobPostingSpecifications.creatorIs(filter.creatorId()));
        }
        if (filter.deadline() != null) {
            where = where.and(JobPostingSpecifications.deadlineIn(filter.deadline(), now));
        }
        if (filter.postedFrom() != null) {
            where = where.and(JobPostingSpecifications.postedOnOrAfter(filter.postedFrom()));
        }
        if (filter.postedTo() != null) {
            where = where.and(JobPostingSpecifications.postedOnOrBefore(filter.postedTo()));
        }
        JobPostingSort sortOption = JobPostingSort.fromParam(filter.sort());
        if (sortOption == JobPostingSort.MOST_APPLICATIONS) {
            where = where.and(JobPostingSpecifications.orderByApplicationCount());
        }

        Page<JobPosting> result = jobPostingRepository.findAll(where,
                PageRequest.of(filter.page(), filter.size(), sortOption.toSort()));
        // Rule: số trang vượt quá tổng số trang thì lùi về trang cuối
        if (result.getTotalPages() > 0 && result.getNumber() >= result.getTotalPages()) {
            result = jobPostingRepository.findAll(where,
                    PageRequest.of(result.getTotalPages() - 1, filter.size(), sortOption.toSort()));
        }

        Map<Integer, long[]> counts = countApplications(result.getContent());
        boolean manage = canManage(viewer);
        boolean hr = RequisitionAccess.ROLE_HR.equals(requisitionAccess.role(viewer));
        return result.map(posting -> toListRow(posting,
                counts.getOrDefault(posting.getJobPostingId(), new long[2]), manage, hr, now));
    }

    /** Đếm hồ sơ của các tin trên trang: id tin → [tổng, mới]. */
    private Map<Integer, long[]> countApplications(List<JobPosting> postings) {
        Map<Integer, long[]> counts = new HashMap<>();
        if (postings.isEmpty()) {
            return counts;
        }
        List<Integer> ids = postings.stream().map(JobPosting::getJobPostingId).toList();
        for (Object[] row : jobPostingRepository.countApplicationsByPosting(ids)) {
            long total = ((Number) row[1]).longValue();
            long fresh = row[2] == null ? 0 : ((Number) row[2]).longValue();
            counts.put((Integer) row[0], new long[] { total, fresh });
        }
        return counts;
    }

    /**
     * Một dòng của bảng: nhãn, số ngày, cờ thao tác đều tính ở đây để template chỉ
     * việc đọc.
     */
    private InternalJobPostingResponse toListRow(JobPosting posting, long[] applicationCounts,
            boolean manage, boolean hr, LocalDateTime now) {
        JobRequisition req = posting.getRequisition();
        String viewStatus = JobPostingLabels.viewStatus(posting.getPostingStatus(), posting.getPostingDate(), now);
        long total = applicationCounts[0];
        LocalDate today = now.toLocalDate();

        Long daysUntilDeadline = posting.getApplicationDeadline() == null
                ? null
                : JobPostingLabels.daysFromToday(posting.getApplicationDeadline(), today);
        // Tin đã đóng: không tô màu / nhắc "còn N ngày" nữa (deadlineState = null)
        String deadlineState = null;
        if (posting.getApplicationDeadline() != null && !JobPostingLabels.STATUS_CLOSED.equals(viewStatus)) {
            if (posting.getApplicationDeadline().isBefore(now)) {
                deadlineState = JobPostingLabels.DEADLINE_PAST;
            } else if (daysUntilDeadline <= JobPostingLabels.DEADLINE_SOON_DAYS) {
                deadlineState = JobPostingLabels.DEADLINE_SOON;
            } else {
                deadlineState = JobPostingLabels.DEADLINE_OPEN;
            }
        }
        boolean scheduled = JobPostingLabels.STATUS_SCHEDULED.equals(viewStatus);

        return InternalJobPostingResponse.builder()
                .jobPostingId(posting.getJobPostingId())
                .postingTitle(posting.getPostingTitle())
                .requisitionId(req != null ? req.getRequisitionId() : null)
                .requisitionCode(req != null ? req.getRequisitionCode() : "—")
                .departmentId(req != null && req.getDepartment() != null ? req.getDepartment().getDepartmentId() : null)
                .departmentName(req != null && req.getDepartment() != null ? req.getDepartment().getDepartmentName()
                        : "Chưa xác định")
                .recruitmentRound(req != null && req.getRecruitmentRound() != null ? req.getRecruitmentRound() : 1)
                .employmentType(req != null ? formatEmploymentType(req.getEmploymentType()) : "—")
                .postingStatus(posting.getPostingStatus())
                .applicationDeadline(posting.getApplicationDeadline())
                .postingDate(posting.getPostingDate())
                .createdAt(posting.getCreatedAt())
                .createdByName(posting.getCreatedBy() != null ? posting.getCreatedBy().getFullName() : "—")
                .postingNo(posting.getPostingNo())
                .viewStatus(viewStatus)
                .statusLabel(JobPostingLabels.statusLabel(viewStatus))
                .statusTone(JobPostingLabels.statusTone(viewStatus))
                .applicationCount(total)
                .newApplicationCount(applicationCounts[1])
                .daysUntilPosting(scheduled ? JobPostingLabels.daysFromToday(posting.getPostingDate(), today) : null)
                .daysUntilDeadline(daysUntilDeadline)
                .deadlineState(deadlineState)
                .publishable(manage && JobPostingLabels.STATUS_DRAFT.equals(viewStatus))
                .reopenable(manage && JobPostingLabels.STATUS_PAUSED.equals(viewStatus))
                .editable(manage && !JobPostingLabels.STATUS_CLOSED.equals(viewStatus))
                .pausable(manage && JobPostingLabels.STATUS_PUBLISHED.equals(viewStatus))
                // Rule (D7): chỉ HR đóng tin, vì đóng tin còn xử lý Offer / đề xuất ứng viên
                // (TalentPoolService#closeJob)
                .closable(hr && (JobPostingLabels.STATUS_PUBLISHED.equals(viewStatus)
                        || JobPostingLabels.STATUS_PAUSED.equals(viewStatus)))
                .deletable(manage && JobPostingLabels.STATUS_DRAFT.equals(viewStatus) && total == 0)
                .applicationsViewable(total > 0)
                .build();
    }

    /** Bước 4: mô tả các bộ lọc đang bật thành chip. */
    private List<FilterChip> describeActiveFilters(JobPostingSearchRequest filter,
            JobPostingFilterOptions options) {
        List<FilterChip> chips = new ArrayList<>();
        if (!filter.status().isEmpty()) {
            List<String> names = filter.status().stream().map(JobPostingLabels::statusLabel).toList();
            chips.add(new FilterChip("Trạng thái: " + String.join(", ", names), List.of("status")));
        }
        if (filter.departmentId() != null) {
            chips.add(new FilterChip("Phòng ban: " + ListFilters.labelOf(filter.departmentId(), options.departments()),
                    List.of("departmentId")));
        }
        if (filter.creatorId() != null) {
            chips.add(new FilterChip("Người tạo: " + ListFilters.labelOf(filter.creatorId(), options.creators()),
                    List.of("creatorId")));
        }
        if (filter.deadline() != null) {
            chips.add(new FilterChip("Hạn nộp: " + JobPostingLabels.deadlineFilterLabel(filter.deadline()),
                    List.of("deadline")));
        }
        if (filter.postedFrom() != null || filter.postedTo() != null) {
            String from = filter.postedFrom() == null ? "…" : filter.postedFrom().format(DISPLAY_DATE);
            String to = filter.postedTo() == null ? "…" : filter.postedTo().format(DISPLAY_DATE);
            chips.add(new FilterChip("Ngày đăng: " + from + " – " + to, List.of("postedFrom", "postedTo")));
        }
        JobPostingSort sort = JobPostingSort.fromParam(filter.sort());
        if (sort != JobPostingSort.NEWEST) {
            chips.add(new FilterChip("Sắp xếp: " + sort.label(), List.of("sort")));
        }
        return chips;
    }

    // ----- Các hàm nhỏ tạo lựa chọn cho ô lọc -----

    private List<FilterOption> statusOptions(List<String> statuses) {
        return statuses.stream().map(s -> new FilterOption(s, JobPostingLabels.statusLabel(s))).toList();
    }

    private List<FilterOption> deadlineOptions() {
        return JobPostingLabels.DEADLINE_FILTERS.stream()
                .map(d -> new FilterOption(d, JobPostingLabels.deadlineFilterLabel(d))).toList();
    }

    private List<FilterOption> sortOptions() {
        return java.util.Arrays.stream(JobPostingSort.values())
                .map(s -> new FilterOption(s.param(), s.label())).toList();
    }

    private List<FilterOption> departmentOptions() {
        return departmentRepository.findAll(Sort.by("departmentName")).stream()
                .map(d -> new FilterOption(String.valueOf(d.getDepartmentId()), d.getDepartmentName())).toList();
    }

    private List<FilterOption> creatorOptions() {
        return jobPostingRepository.findPostingCreators().stream()
                .map(u -> new FilterOption(String.valueOf(u.getUserId()), u.getFullName())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobPostingCreateRequest prepareCreateForm(Integer requisitionId, User currentUser) {
        // Tin chỉ được tạo từ một yêu cầu cụ thể đã duyệt: thiếu requisitionId thì
        // prepareCreateFormInternal báo lỗi
        return prepareCreateFormInternal(requisitionId, currentUser, true);
    }

    private JobPostingCreateRequest prepareCreateFormInternal(Integer requisitionId, User currentUser,
            boolean checkExistingActive) {
        requireHrOrAdmin(currentUser);
        if (requisitionId == null) {
            throw new JobPostingValidationException("Mã yêu cầu tuyển dụng không hợp lệ.");
        }
        JobRequisition requisition = jobRequisitionRepository.findById(requisitionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy yêu cầu tuyển dụng với mã: " + requisitionId));
        validator.requireApproved(requisition);
        if (checkExistingActive) {
            requireNoOtherOpenPosting(requisitionId, null);
        }

        JobPostingCreateRequest form = new JobPostingCreateRequest();
        form.setRequisitionId(requisition.getRequisitionId());
        fillSourceInfo(form, requisition);
        form.setPostingNo(jobPostingRepository.findMaxPostingNo(requisition.getRequisitionId()) + 1);
        form.setLive(false);

        // Giá trị điền sẵn từ yêu cầu (HR sửa lại cho hấp dẫn ứng viên)
        form.setPostingTitle(requisition.getTitle());
        form.setJobDescription(form.getSourceJobDescription());
        form.setJobRequirements(form.getSourceRequirements());
        form.setWorkLocation(form.getSourceWorkLocation());
        form.setBenefits("");
        boolean hasSalary = requisition.getMinSalary() != null || requisition.getMaxSalary() != null;
        form.setSalaryMode(
                hasSalary ? JobPostingCreateRequest.SALARY_RANGE : JobPostingCreateRequest.SALARY_NEGOTIABLE);
        form.setShowPositions(true);
        form.setNumberOfPositionsDisplay(requisition.getNumberOfPositions() == null
                ? ""
                : requisition.getNumberOfPositions() + " người");
        form.setApplicationDeadline(LocalDate.now().plusDays(DEFAULT_DEADLINE_DAYS));
        form.setPublishMode(JobPostingCreateRequest.PUBLISH_NOW);
        form.setAction("draft");
        return form;
    }

    /**
     * Điền thông tin yêu cầu nguồn (chỉ hiển thị) và các giá trị gốc cho nút "Khôi
     * phục bản gốc".
     */
    private void fillSourceInfo(JobPostingCreateRequest form, JobRequisition requisition) {
        form.setRequisitionCode(requisition.getRequisitionCode());
        form.setRequisitionTitle(requisition.getTitle());
        form.setDepartmentName(requisition.getDepartment() != null ? requisition.getDepartment().getDepartmentName()
                : "Chưa xác định");
        form.setRecruitmentRound(requisition.getRecruitmentRound() != null ? requisition.getRecruitmentRound() : 1);
        form.setEmploymentType(RequisitionLabels.employmentType(requisition.getEmploymentType()));
        form.setWorkModel(RequisitionLabels.workModel(requisition.getWorkModel()));
        form.setNumberOfPositions(requisition.getNumberOfPositions());
        form.setHiringManagerName(
                requisition.getHiringManager() != null ? requisition.getHiringManager().getFullName() : "—");
        form.setReasonForHiring(requisition.getReasonForHiring());
        form.setMinSalary(requisition.getMinSalary());
        form.setMaxSalary(requisition.getMaxSalary());
        form.setProbationDays(requisition.getProbationDays());
        form.setGender(RequisitionLabels.gender(requisition.getGender()));
        form.setExpectedStartDate(requisition.getExpectedStartDate());
        form.setSalaryRangeLabel(RequisitionLabels.salary(requisition.getMinSalary(), requisition.getMaxSalary()));
        form.setSourceWorkLocation(Objects.toString(requisition.getWorkLocation(), ""));
        form.setSourceJobDescription(Objects.toString(requisition.getJobDescription(), ""));
        form.setSourceRequirements(requirementsFromRequisition(requisition));
    }

    /**
     * Yêu cầu ứng viên điền sẵn: chi tiết yêu cầu + danh sách tiêu chí bắt buộc.
     */
    private String requirementsFromRequisition(JobRequisition requisition) {
        StringBuilder requirements = new StringBuilder();
        if (requisition.getRequirementDetails() != null && !requisition.getRequirementDetails().isBlank()) {
            requirements.append(requisition.getRequirementDetails().trim());
        }
        List<ScreeningCriteria> mandatory = requisition.getScreeningCriteria() == null ? List.of()
                : requisition.getScreeningCriteria().stream().filter(c -> Boolean.TRUE.equals(c.getIsMandatory()))
                        .toList();
        if (!mandatory.isEmpty()) {
            requirements.append(requirements.isEmpty() ? "Tiêu chí bắt buộc:\n" : "\n\nTiêu chí bắt buộc:\n");
            for (ScreeningCriteria criteria : mandatory) {
                requirements.append("- ").append(criteria.getCriteriaName());
                if (criteria.getRequiredValue() != null && !criteria.getRequiredValue().isBlank()) {
                    requirements.append(": ").append(criteria.getRequiredValue());
                }
                requirements.append("\n");
            }
        }
        return requirements.toString().trim();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FilterOption> requisitionChoices(Integer currentRequisitionId, User currentUser) {
        requireHrOrAdmin(currentUser);
        List<FilterOption> choices = new ArrayList<>();
        for (var req : requisitionsAwaitingPosting()) {
            int posted = jobPostingRepository.findMaxPostingNo(req.requisitionId());
            String label = req.title() + " · " + req.requisitionCode()
                    + (posted > 0 ? " (đã đăng " + posted + " lần)" : "");
            choices.add(new FilterOption(String.valueOf(req.requisitionId()), label));
        }
        return choices;
    }

    @Override
    @Transactional(readOnly = true)
    public JobPostingCreateRequest restoreSourceInfo(JobPostingCreateRequest edited, User currentUser) {
        requireHrOrAdmin(currentUser);
        JobRequisition requisition;
        if (edited.getJobPostingId() != null) {
            JobPosting posting = findById(edited.getJobPostingId());
            requisition = posting.getRequisition();
            edited.setPostingStatus(posting.getPostingStatus());
            edited.setPostingNo(posting.getPostingNo());
            edited.setLive(isLive(posting, LocalDateTime.now()));
        } else {
            if (edited.getRequisitionId() == null) {
                throw new ResourceNotFoundException("Không tìm thấy yêu cầu tuyển dụng nguồn.");
            }
            requisition = jobRequisitionRepository.findById(edited.getRequisitionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tuyển dụng nguồn."));
            edited.setPostingNo(jobPostingRepository.findMaxPostingNo(requisition.getRequisitionId()) + 1);
            edited.setLive(false);
        }
        edited.setRequisitionId(requisition.getRequisitionId());
        fillSourceInfo(edited, requisition);
        return edited;
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
        validator.validateForm(request, publish, true, now);

        JobPosting posting = new JobPosting();
        posting.setRequisition(requisition);
        // Requisition đã bị khóa ở trên nên số lần đăng kế tiếp không bị trùng
        // (UQ_JobPosting_Requisition_PostingNo)
        posting.setPostingNo(jobPostingRepository.findMaxPostingNo(requisition.getRequisitionId()) + 1);
        posting.setCreatedBy(actor);
        applyEditableFields(posting, request, requisition);
        if (publish) {
            posting.setPostingStatus(JobPostingLabels.STATUS_PUBLISHED);
            // Rule (D1): hẹn đăng = Published + PostingDate ở tương lai; ứng viên chỉ thấy
            // tin từ thời điểm đó
            posting.setPostingDate(chosenPostingTime(request, now));
        } else {
            posting.setPostingStatus(JobPostingLabels.STATUS_DRAFT);
        }

        JobPosting saved = jobPostingRepository.save(posting);
        audit(saved, actor, "CREATE", "Tạo tin tuyển dụng: " + saved.getPostingStatus());
        if (publish)
            notificationService.notifyJobPostingPublished(saved, actor);
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

    /**
     * Sửa tin. Trạng thái sau khi lưu:
     * - Nháp: "Lưu nháp" giữ nháp; "Đăng tin" → đăng ngay hoặc hẹn đăng
     * - Hẹn đăng: lưu lại thời điểm đăng mới (chọn "Đăng ngay" thì đăng luôn)
     * - Đang đăng: giữ nguyên ngày đăng, chỉ đổi nội dung
     * - Tạm dừng: "Lưu thay đổi" giữ tạm dừng; "Đăng tin" → mở lại (ngày đăng = bây
     * giờ)
     */
    private void update(Integer id, JobPostingCreateRequest request, User actor, boolean publish) {
        requireHrOrAdmin(actor);
        JobPosting posting = findForUpdate(id);
        validator.requireEditable(posting.getPostingStatus());
        if (request.getJobPostingId() == null || !id.equals(request.getJobPostingId())
                || !posting.getRequisition().getRequisitionId().equals(request.getRequisitionId())) {
            throw new JobPostingValidationException("Không thể đổi tin hoặc yêu cầu tuyển dụng nguồn.");
        }
        validator.requireApproved(posting.getRequisition());

        LocalDateTime now = LocalDateTime.now();
        String status = posting.getPostingStatus();
        boolean draft = JobPostingLabels.STATUS_DRAFT.equals(status);
        boolean paused = JobPostingLabels.STATUS_PAUSED.equals(status);
        boolean scheduled = JobPostingLabels.STATUS_SCHEDULED.equals(
                JobPostingLabels.viewStatus(status, posting.getPostingDate(), now));
        if (publish && paused) {
            requireNoOtherOpenPosting(posting.getRequisition().getRequisitionId(), id);
        }
        // Tin đã / sẽ hiển thị cho ứng viên thì bắt buộc hạn nộp; chỉ nháp và hẹn đăng
        // mới đổi được ngày đăng
        validator.validateForm(request, publish || !draft, draft || scheduled, now);
        applyEditableFields(posting, request, posting.getRequisition());

        LocalDateTime oldPostingDate = posting.getPostingDate();
        if ((draft && publish) || scheduled) {
            posting.setPostingStatus(JobPostingLabels.STATUS_PUBLISHED);
            posting.setPostingDate(chosenPostingTime(request, now));
        } else if (paused && publish) {
            posting.setPostingStatus(JobPostingLabels.STATUS_PUBLISHED);
            posting.setPostingDate(now);
        }
        boolean newPublication = posting.getPostingDate() != null && !posting.getPostingDate().equals(oldPostingDate);
        boolean laterPublication = newPublication && posting.getPostingDate().isAfter(now);

        jobPostingRepository.save(posting);
        audit(posting, actor, "UPDATE", !newPublication ? "Cập nhật tin tuyển dụng"
                : laterPublication ? "Hẹn đăng tin tuyển dụng" : "Đăng tin tuyển dụng");
        if (newPublication)
            notificationService.notifyJobPostingPublished(posting, actor);
    }

    /**
     * Thời điểm đăng HR chọn: "Hẹn ngày đăng" → thời điểm hẹn; còn lại → ngay bây
     * giờ.
     */
    private LocalDateTime chosenPostingTime(JobPostingCreateRequest request, LocalDateTime now) {
        boolean later = JobPostingCreateRequest.PUBLISH_LATER.equals(request.getPublishMode());
        return later && request.getScheduledAt() != null ? request.getScheduledAt() : now;
    }

    /**
     * Tin đã lên trang tuyển dụng (đang đăng, hoặc tạm dừng sau khi đã đăng): không
     * hẹn lại ngày đăng được.
     */
    private boolean isLive(JobPosting posting, LocalDateTime now) {
        String viewStatus = JobPostingLabels.viewStatus(posting.getPostingStatus(), posting.getPostingDate(), now);
        return JobPostingLabels.STATUS_PUBLISHED.equals(viewStatus)
                || JobPostingLabels.STATUS_PAUSED.equals(viewStatus);
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
        if (posting.getApplicationDeadline() != null
                && !posting.getApplicationDeadline().isAfter(LocalDateTime.now())) {
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
        if (id == null)
            throw new JobPostingValidationException("Mã tin tuyển dụng không hợp lệ.");
        return jobPostingRepository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng."));
    }

    private JobPosting findById(Integer id) {
        if (id == null)
            throw new JobPostingValidationException("Mã tin tuyển dụng không hợp lệ.");
        return jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng với mã: " + id));
    }

    private void requireNoOtherOpenPosting(Integer requisitionId, Integer currentId) {
        boolean exists = currentId == null
                ? jobPostingRepository.existsByRequisition_RequisitionIdAndPostingStatusIn(
                        requisitionId, OPEN_STATUSES)
                : jobPostingRepository.existsByRequisition_RequisitionIdAndJobPostingIdNotAndPostingStatusIn(
                        requisitionId, currentId, OPEN_STATUSES);
        if (exists)
            throw new JobPostingValidationException("Yêu cầu này đã có tin nháp hoặc tin đang hoạt động.");
    }

    /**
     * Ghi các trường HR được sửa (rule §24.3). Chuỗi đã được validator trim / làm
     * sạch.
     */
    private void applyEditableFields(JobPosting posting, JobPostingCreateRequest request, JobRequisition requisition) {
        posting.setPostingTitle(request.getPostingTitle());
        posting.setJobDescription(request.getJobDescription());
        posting.setJobRequirements(request.getJobRequirements());
        posting.setBenefits(request.getBenefits());
        posting.setWorkLocation(request.getWorkLocation());
        posting.setSalaryDisplay(salaryDisplayOf(request, requisition));
        // Tắt "Hiển thị số lượng tuyển" thì lưu NULL (trang công khai không hiện số
        // lượng)
        posting.setNumberOfPositionsDisplay(Boolean.TRUE.equals(request.getShowPositions())
                ? request.getNumberOfPositionsDisplay()
                : null);
        posting.setApplicationDeadline(validator.deadlineOf(request));
    }

    /**
     * Chuỗi lương lưu vào SalaryDisplay: theo yêu cầu (lấy từ DB, không tin số gửi
     * lên), thỏa thuận, hoặc tự nhập.
     */
    private String salaryDisplayOf(JobPostingCreateRequest request, JobRequisition requisition) {
        return switch (request.getSalaryMode()) {
            case JobPostingCreateRequest.SALARY_RANGE ->
                RequisitionLabels.salary(requisition.getMinSalary(), requisition.getMaxSalary());
            case JobPostingCreateRequest.SALARY_CUSTOM -> request.getSalaryCustom();
            default -> SALARY_NEGOTIABLE_TEXT;
        };
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

        JobPostingCreateRequest form = prepareCreateFormInternal(posting.getRequisition().getRequisitionId(),
                currentUser, false);
        LocalDateTime now = LocalDateTime.now();
        form.setJobPostingId(posting.getJobPostingId());
        form.setPostingStatus(posting.getPostingStatus());
        form.setPostingNo(posting.getPostingNo());
        form.setLive(isLive(posting, now));
        copyPostingContent(form, posting);
        form.setApplicationDeadline(
                posting.getApplicationDeadline() != null ? posting.getApplicationDeadline().toLocalDate() : null);

        boolean scheduled = JobPostingLabels.STATUS_SCHEDULED.equals(
                JobPostingLabels.viewStatus(posting.getPostingStatus(), posting.getPostingDate(), now));
        form.setPublishMode(scheduled ? JobPostingCreateRequest.PUBLISH_LATER : JobPostingCreateRequest.PUBLISH_NOW);
        form.setScheduledAt(scheduled ? posting.getPostingDate() : null);
        form.setAction("draft");
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
        // Rule (D5): HM và Director không xem tin nháp, kể cả khi gõ thẳng URL
        if (!canManage(currentUser) && JobPostingLabels.STATUS_DRAFT.equals(posting.getPostingStatus()))
            throw new AccessDeniedException("Tin tuyển dụng này chưa được đăng.");

        LocalDateTime now = LocalDateTime.now();
        String viewStatus = JobPostingLabels.viewStatus(posting.getPostingStatus(), posting.getPostingDate(), now);
        boolean manage = canManage(currentUser);
        boolean hr = RequisitionAccess.ROLE_HR.equals(requisitionAccess.role(currentUser));
        boolean draft = JobPostingLabels.STATUS_DRAFT.equals(viewStatus);
        boolean scheduled = JobPostingLabels.STATUS_SCHEDULED.equals(viewStatus);
        boolean published = JobPostingLabels.STATUS_PUBLISHED.equals(viewStatus);
        boolean paused = JobPostingLabels.STATUS_PAUSED.equals(viewStatus);
        boolean closed = JobPostingLabels.STATUS_CLOSED.equals(viewStatus);
        // Tin đã lên trang tuyển dụng thì mới có hồ sơ để thống kê
        boolean live = published || paused || closed;

        // ----- Số liệu hồ sơ (chỉ khi tin đã lên trang tuyển dụng) -----
        Map<String, Long> byStatus = new HashMap<>();
        if (live) {
            for (Object[] row : jobPostingRepository.countApplicationsByStatus(id)) {
                byStatus.put((String) row[0], ((Number) row[1]).longValue());
            }
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        BigDecimal threshold = aiScoreThreshold();

        return InternalJobPostingDetailResponse.builder()
                .jobPostingId(posting.getJobPostingId())
                .postingTitle(posting.getPostingTitle())
                .requisitionId(req != null ? req.getRequisitionId() : null)
                .requisitionCode(req != null ? req.getRequisitionCode() : "—")
                .requisitionTitle(req != null ? req.getTitle() : "—")
                .requisitionApprovalStatus(req != null ? req.getApprovalStatus() : "—")
                .departmentId(req != null && req.getDepartment() != null ? req.getDepartment().getDepartmentId() : null)
                .departmentName(req != null && req.getDepartment() != null ? req.getDepartment().getDepartmentName()
                        : "Chưa xác định")
                .recruitmentRound(req != null && req.getRecruitmentRound() != null ? req.getRecruitmentRound() : 1)
                .numberOfPositions(req != null ? req.getNumberOfPositions() : null)
                .employmentType(req != null ? formatEmploymentType(req.getEmploymentType()) : "—")
                .workModel(req != null ? req.getWorkModel() : null)
                .workModelLabel(req != null ? RequisitionLabels.workModel(req.getWorkModel()) : "—")
                .expectedStartDate(req != null ? req.getExpectedStartDate() : null)
                .workLocation(posting.getWorkLocation())
                .salaryDisplay(posting.getSalaryDisplay() != null ? posting.getSalaryDisplay() : SALARY_NEGOTIABLE_TEXT)
                .numberOfPositionsDisplay(posting.getNumberOfPositionsDisplay())
                .postingNo(posting.getPostingNo())
                .closedAt(posting.getClosedAt())
                .jobDescription(posting.getJobDescription())
                .jobRequirements(posting.getJobRequirements())
                .benefits(posting.getBenefits())
                .postingStatus(posting.getPostingStatus())
                .viewStatus(viewStatus)
                .postingStatusLabel(JobPostingLabels.statusLabel(viewStatus))
                .statusTone(JobPostingLabels.statusTone(viewStatus))
                .postingDate(posting.getPostingDate())
                .applicationDeadline(posting.getApplicationDeadline())
                .daysUntilDeadline(posting.getApplicationDeadline() == null
                        ? null
                        : JobPostingLabels.daysFromToday(posting.getApplicationDeadline(), now.toLocalDate()))
                .createdAt(posting.getCreatedAt())
                .updatedAt(posting.getUpdatedAt())
                .createdByName(posting.getCreatedBy() != null ? posting.getCreatedBy().getFullName() : "—")
                .createdByEmail(posting.getCreatedBy() != null ? posting.getCreatedBy().getEmail() : null)
                .hiringManagerName(
                        req != null && req.getHiringManager() != null ? req.getHiringManager().getFullName() : "—")
                .hiringManagerEmail(
                        req != null && req.getHiringManager() != null ? req.getHiringManager().getEmail() : null)
                .screeningCriteria(List.of())
                .activityHistory(buildActivityHistory(posting))
                .live(live)
                .canManage(manage)
                // Cờ thao tác theo trạng thái (plan_posting.md, Bước 4)
                .publishable(manage && draft)
                .publishNowable(manage && scheduled)
                .unschedulable(manage && scheduled)
                .editable(manage && !closed)
                .pausable(manage && published)
                .reopenable(manage && paused)
                // Rule (D7): chỉ HR đóng tin (đóng tin còn xử lý Offer / đề xuất ứng viên)
                .closable(hr && (published || paused))
                .deletable(manage && draft && !applicationRepository.existsByJobPosting_JobPostingId(id))
                .repostable(manage && closed && req != null
                        && RequisitionAccess.STATUS_APPROVED.equals(req.getApprovalStatus())
                        && !jobPostingRepository.existsByRequisition_RequisitionIdAndPostingStatusIn(
                                req.getRequisitionId(), OPEN_STATUSES))
                .publicViewable(posting.isVisibleToCandidates(now))
                // Số liệu
                .totalApplications(total)
                .applicationsLast7Days(live ? jobPostingRepository.countApplicationsSince(id, now.minusDays(7)) : 0)
                .pendingApplications(byStatus.getOrDefault("Applied", 0L) + byStatus.getOrDefault("AI_Screened", 0L))
                .aiPassedApplications(
                        live ? jobPostingRepository.countApplicationsWithAiScoreAtLeast(id, threshold) : 0)
                .aiThreshold(threshold)
                .interviewingApplications(byStatus.getOrDefault("Interviewing", 0L))
                .hiredApplications(byStatus.getOrDefault("Hired", 0L))
                .recentApplications(live ? recentApplications(id, threshold) : List.of())
                .rounds(req != null ? postingRounds(req.getRequisitionId(), id, now) : List.of())
                .build();
    }

    /** 7 hồ sơ mới nhất kèm điểm AI lần chấm gần nhất (null = chưa chấm). */
    private List<InternalJobPostingDetailResponse.RecentApplication> recentApplications(Integer postingId,
            BigDecimal threshold) {
        List<Object[]> rows = jobPostingRepository.findRecentApplications(postingId,
                PageRequest.of(0, RECENT_APPLICATIONS));
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Integer, BigDecimal> scores = new HashMap<>();
        List<Integer> ids = rows.stream().map(row -> (Integer) row[0]).toList();
        for (Object[] row : jobPostingRepository.findLatestAiScores(ids)) {
            scores.put((Integer) row[0], (BigDecimal) row[1]);
        }
        List<InternalJobPostingDetailResponse.RecentApplication> result = new ArrayList<>();
        for (Object[] row : rows) {
            Integer applicationId = (Integer) row[0];
            String status = (String) row[2];
            BigDecimal score = scores.get(applicationId);
            result.add(new InternalJobPostingDetailResponse.RecentApplication(applicationId, (String) row[1], score,
                    score != null && score.compareTo(threshold) >= 0,
                    ApplicationStatusLabels.label(status), ApplicationStatusLabels.badge(status),
                    (LocalDateTime) row[3]));
        }
        return result;
    }

    /**
     * Các lần đăng của cùng yêu cầu, kèm số hồ sơ và số đã tuyển (2 câu truy vấn
     * gộp).
     */
    private List<InternalJobPostingDetailResponse.PostingRound> postingRounds(Integer requisitionId, Integer currentId,
            LocalDateTime now) {
        List<JobPosting> all = jobPostingRepository.findByRequisition_RequisitionIdOrderByPostingNoAsc(requisitionId);
        List<Integer> ids = all.stream().map(JobPosting::getJobPostingId).toList();
        Map<Integer, long[]> counts = countApplications(all);
        Map<Integer, Long> hired = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : jobPostingRepository.countHiredByPosting(ids)) {
                hired.put((Integer) row[0], ((Number) row[1]).longValue());
            }
        }
        List<InternalJobPostingDetailResponse.PostingRound> rounds = new ArrayList<>();
        for (JobPosting round : all) {
            String viewStatus = JobPostingLabels.viewStatus(round.getPostingStatus(), round.getPostingDate(), now);
            rounds.add(new InternalJobPostingDetailResponse.PostingRound(round.getJobPostingId(), round.getPostingNo(),
                    round.getPostingDate(), round.getApplicationDeadline(),
                    JobPostingLabels.statusLabel(viewStatus), JobPostingLabels.statusTone(viewStatus),
                    counts.getOrDefault(round.getJobPostingId(), new long[2])[0],
                    hired.getOrDefault(round.getJobPostingId(), 0L),
                    round.getJobPostingId().equals(currentId)));
        }
        return rounds;
    }

    /**
     * Ngưỡng điểm AI "đạt" (SystemConfig.AI_SCORE_THRESHOLD); thiếu hoặc sai định
     * dạng thì dùng 70.
     */
    private BigDecimal aiScoreThreshold() {
        try {
            BigDecimal threshold = jobPostingRepository.findAiScoreThreshold().map(BigDecimal::new)
                    .orElse(DEFAULT_AI_THRESHOLD);
            // Cấu hình lưu "70.00" thì hiển thị "70" (setScale(0) để 70 không thành 7E+1)
            BigDecimal trimmed = threshold.stripTrailingZeros();
            return trimmed.scale() < 0 ? trimmed.setScale(0) : trimmed;
        } catch (NumberFormatException e) {
            return DEFAULT_AI_THRESHOLD;
        }
    }

    /** "Đăng ngay" tin đang hẹn đăng: ngày đăng = bây giờ. */
    @Override
    @Transactional
    public void publishNow(Integer id, User actor) {
        requireHrOrAdmin(actor);
        JobPosting posting = findForUpdate(id);
        LocalDateTime now = LocalDateTime.now();
        if (!JobPostingLabels.STATUS_SCHEDULED
                .equals(JobPostingLabels.viewStatus(posting.getPostingStatus(), posting.getPostingDate(), now))) {
            throw new JobPostingValidationException("Chỉ tin đang hẹn đăng mới đăng ngay được.");
        }
        validator.requireApproved(posting.getRequisition());
        posting.setPostingDate(now);
        jobPostingRepository.save(posting);
        audit(posting, actor, "UPDATE", "Đăng tin tuyển dụng");
        notificationService.notifyJobPostingPublished(posting, actor);
    }

    /**
     * "Hủy lịch đăng": tin hẹn đăng trở về bản nháp (PostingDate = NULL, rule
     * §24.4).
     */
    @Override
    @Transactional
    public void unschedule(Integer id, User actor) {
        requireHrOrAdmin(actor);
        JobPosting posting = findForUpdate(id);
        if (!JobPostingLabels.STATUS_SCHEDULED.equals(
                JobPostingLabels.viewStatus(posting.getPostingStatus(), posting.getPostingDate(),
                        LocalDateTime.now()))) {
            throw new JobPostingValidationException("Chỉ tin đang hẹn đăng mới hủy lịch được.");
        }
        posting.setPostingStatus(JobPostingLabels.STATUS_DRAFT);
        posting.setPostingDate(null);
        jobPostingRepository.save(posting);
        audit(posting, actor, "UPDATE", "Hủy lịch đăng tin tuyển dụng");
    }

    /**
     * "Đăng lại" tin đã đóng (D10): mở form tạo tin lần mới cho cùng yêu cầu,
     * nội dung lấy từ tin vừa đóng (HR đã chỉnh sửa), hạn nộp và ngày đăng chọn
     * lại.
     */
    @Override
    @Transactional(readOnly = true)
    public JobPostingCreateRequest prepareRepostForm(Integer closedPostingId, User currentUser) {
        requireHrOrAdmin(currentUser);
        JobPosting closed = findById(closedPostingId);
        if (!JobPostingLabels.STATUS_CLOSED.equals(closed.getPostingStatus())) {
            throw new JobPostingValidationException("Chỉ tin đã đóng mới đăng lại được.");
        }
        JobPostingCreateRequest form = prepareCreateFormInternal(closed.getRequisition().getRequisitionId(),
                currentUser, true);
        copyPostingContent(form, closed);
        return form;
    }

    /**
     * Chép nội dung một tin có sẵn vào form (dùng cho "Chỉnh sửa" và "Đăng lại").
     */
    private void copyPostingContent(JobPostingCreateRequest form, JobPosting posting) {
        form.setPostingTitle(posting.getPostingTitle());
        form.setJobDescription(posting.getJobDescription());
        form.setJobRequirements(posting.getJobRequirements());
        form.setBenefits(posting.getBenefits());
        form.setWorkLocation(posting.getWorkLocation());

        // Đoán lại lựa chọn lương từ chuỗi đã lưu
        String salary = posting.getSalaryDisplay();
        if (salary != null && salary.equals(form.getSalaryRangeLabel())) {
            form.setSalaryMode(JobPostingCreateRequest.SALARY_RANGE);
        } else if (salary == null || SALARY_NEGOTIABLE_TEXT.equalsIgnoreCase(salary)
                || "Thoả thuận".equalsIgnoreCase(salary)) {
            form.setSalaryMode(JobPostingCreateRequest.SALARY_NEGOTIABLE);
        } else {
            form.setSalaryMode(JobPostingCreateRequest.SALARY_CUSTOM);
            form.setSalaryCustom(salary);
        }

        boolean showPositions = posting.getNumberOfPositionsDisplay() != null;
        form.setShowPositions(showPositions);
        if (showPositions) {
            form.setNumberOfPositionsDisplay(posting.getNumberOfPositionsDisplay());
        }
    }

    // Rule: build structured audit history for job posting detail view
    private List<InternalJobPostingDetailResponse.ActivityHistoryItem> buildActivityHistory(JobPosting posting) {
        List<AuditLog> logs = auditLogRepository.findByEntityNameAndEntityIdOrderByTimestampDesc(
                "JobPosting", String.valueOf(posting.getJobPostingId()));

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
            if (detail.startsWith("Đăng tin tuyển dụng") || detail.contains("Published"))
                return "Đăng tin";
            if (detail.startsWith("Tạm dừng tin tuyển dụng"))
                return "Tạm dừng";
            if (detail.startsWith("Hẹn đăng tin tuyển dụng"))
                return "Hẹn đăng tin";
            if (detail.startsWith("Hủy lịch đăng"))
                return "Hủy lịch đăng";
            if (detail.equals("Closed"))
                return "Đóng tin";
            return "Cập nhật";
        }
        if ("CREATE".equalsIgnoreCase(log.getAction())) {
            return detail.startsWith("Tạo tin tuyển dụng: Published") ? "Tạo và đăng tin" : "Tạo mới";
        }
        if ("DELETE".equalsIgnoreCase(log.getAction()))
            return "Xóa";
        return "Tạo mới";
    }

    private String formatEmploymentType(String type) {
        if (type == null)
            return "—";
        return switch (type) {
            case "Full-time" -> "Toàn thời gian";
            case "Part-time" -> "Bán thời gian";
            case "Internship" -> "Thực tập";
            case "Contract" -> "Hợp đồng";
            default -> type;
        };
    }
}
