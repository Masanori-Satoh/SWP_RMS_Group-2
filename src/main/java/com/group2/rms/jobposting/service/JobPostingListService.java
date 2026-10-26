package com.group2.rms.jobposting.service;

import com.group2.rms.core.dto.FilterChip;
import com.group2.rms.core.dto.FilterOption;
import com.group2.rms.core.util.ListFilters;
import com.group2.rms.jobposting.dto.InternalJobPostingResponse;
import com.group2.rms.jobposting.dto.JobPostingFilterOptions;
import com.group2.rms.jobposting.dto.JobPostingListResponse;
import com.group2.rms.jobposting.dto.JobPostingSearchRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn danh sách tin tuyển dụng (/internal/job-postings): phạm vi theo vai trò, làm sạch bộ lọc, tìm kiếm,
 * sắp xếp, phân trang, chip. Chỉ đọc dữ liệu, không ghi.
 *
 * getListPage luôn theo 4 bước (giống RequisitionListService):
 * 1. buildFilterOptions — vai trò này thấy ô lọc nào, mỗi ô có lựa chọn gì
 * 2. normalizeFilter    — không tin URL: giá trị vai trò không được dùng / ngoài danh sách thì bỏ qua
 * 3. search             — phạm vi theo vai trò AND từng bộ lọc → 1 trang; số hồ sơ đếm gộp cho cả trang (chống N+1)
 * 4. describeActiveFilters — mỗi bộ lọc đang bật thành một chip có nút ✕
 */
@Service
@RequiredArgsConstructor
public class JobPostingListService {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 50;
    /** Hồ sơ "mới" trên cột Hồ sơ: chưa ai xử lý. */
    public static final String NEW_APPLICATION_STATUS = "Applied";

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Chip "Ngày đăng" khi chỉ chọn một đầu. */
    private static final String OPEN_END = "…";

    private final JobPostingRepository postings;
    private final DepartmentRepository departments;
    private final JobPostingAccess access;
    private final JobPostingMapper mapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public JobPostingListResponse getListPage(JobPostingSearchRequest rawFilter, User viewer) {
        access.requireRead(viewer);
        LocalDateTime now = LocalDateTime.now(clock);
        JobPostingFilterOptions options = buildFilterOptions(viewer);
        JobPostingSearchRequest filter = normalizeFilter(rawFilter, options);
        Page<InternalJobPostingResponse> page = search(filter, viewer, now);
        List<FilterChip> chips = describeActiveFilters(filter, options);
        return new JobPostingListResponse(page, filter, options, chips, access.canManage(viewer));
    }

    /**
     * Phạm vi dữ liệu theo vai trò (lớp bảo vệ theo dữ liệu, mọi bộ lọc khác được nối thêm AND, khớp
     * JobPostingAccess#canView):
     * - HR: mọi tin
     * - Hiring Manager: tin thuộc phòng ban của mình, trừ tin nháp
     * - Director: mọi tin, trừ tin nháp
     */
    private Specification<JobPosting> scope(User viewer) {
        if (access.isHiringManager(viewer)) {
            return JobPostingSpecifications.inDepartmentOf(viewer).and(JobPostingSpecifications.notDraft());
        }
        if (!access.canManage(viewer)) {
            return JobPostingSpecifications.notDraft();
        }
        return Specification.unrestricted();
    }

    /** Bước 1: ô lọc theo vai trò (bảng trong plan_posting.md, Bước 2). Mọi vai trò có ô Trạng thái và Sắp xếp. */
    private JobPostingFilterOptions buildFilterOptions(User viewer) {
        String role = access.role(viewer);
        List<FilterOption> sorts = sortOptions();
        List<FilterOption> deadlines = deadlineOptions();

        if (RequisitionAccess.ROLE_HIRING_MANAGER.equals(role)) {
            // HM chỉ thấy tin phòng mình nên không cần lọc phòng ban
            return new JobPostingFilterOptions(false, true, false, false,
                    statusOptions(JobPostingAccess.VIEW_STATUSES_WITHOUT_DRAFT), List.of(), List.of(), deadlines,
                    sorts);
        }
        if (RequisitionAccess.ROLE_DIRECTOR.equals(role)) {
            return new JobPostingFilterOptions(true, true, false, false,
                    statusOptions(JobPostingAccess.VIEW_STATUSES_WITHOUT_DRAFT), departmentOptions(), List.of(),
                    deadlines, sorts);
        }
        if (RequisitionAccess.ROLE_HR.equals(role)) {
            return new JobPostingFilterOptions(true, true, false, false,
                    statusOptions(JobPostingAccess.VIEW_STATUSES), departmentOptions(), List.of(), deadlines, sorts);
        }
        throw new org.springframework.security.access.AccessDeniedException("Vai trò không có quyền xem tin tuyển dụng.");
    }

    /** Bước 2: giá trị nào vai trò không được dùng, hoặc không có trong danh sách lựa chọn, thì bỏ qua. */
    private JobPostingSearchRequest normalizeFilter(JobPostingSearchRequest raw, JobPostingFilterOptions options) {
        String keyword = ListFilters.cleanKeyword(raw.q());
        List<String> statuses = ListFilters.keepAllowedValues(raw.status(), options.statuses()).stream().limit(1).toList();

        Integer departmentId = null;
        if (options.showDepartment() && ListFilters.isAllowedValue(raw.departmentId(), options.departments())) {
            departmentId = raw.departmentId();
        }
        Integer creatorId = null;
        if (options.showCreator() && ListFilters.isAllowedValue(raw.creatorId(), options.creators())) {
            creatorId = raw.creatorId();
        }
        String deadline = null;
        if (options.showDeadline() && ListFilters.isAllowedValue(raw.deadline(), options.deadlines())) {
            deadline = raw.deadline();
        }
        LocalDate postedFrom = null;
        LocalDate postedTo = null;
        if (options.showPostedDate()) {
            postedFrom = raw.postedFrom();
            postedTo = raw.postedTo();
        }

        String sort = JobPostingSort.fromParam(raw.sort()).param();
        return new JobPostingSearchRequest(keyword, statuses, departmentId, creatorId, deadline,
                postedFrom, postedTo, sort, pageNumber(raw.page()), pageSize(raw.size()));
    }

    /** Bước 3: ghép điều kiện WHERE, truy vấn một trang, rồi đếm hồ sơ cho cả trang bằng 1 câu. */
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

        Page<JobPosting> result = postings.findAll(where,
                PageRequest.of(filter.page(), filter.size(), sortOption.toSort()));
        // Rule: số trang vượt quá tổng số trang thì lùi về trang cuối
        if (result.getTotalPages() > 0 && result.getNumber() >= result.getTotalPages()) {
            result = postings.findAll(where,
                    PageRequest.of(result.getTotalPages() - 1, filter.size(), sortOption.toSort()));
        }

        Map<Integer, long[]> counts = countApplications(result.getContent());
        return result.map(posting -> {
            long[] postingCounts = counts.getOrDefault(posting.getJobPostingId(), new long[2]);
            return mapper.toListRow(posting, viewer, postingCounts[0], postingCounts[1], now);
        });
    }

    /** Đếm hồ sơ của các tin trên trang: id tin → [tổng, mới]. Trang rỗng thì không chạy câu IN (). */
    private Map<Integer, long[]> countApplications(List<JobPosting> pagePostings) {
        Map<Integer, long[]> counts = new HashMap<>();
        if (pagePostings.isEmpty()) {
            return counts;
        }
        List<Integer> ids = new ArrayList<>();
        for (JobPosting posting : pagePostings) {
            ids.add(posting.getJobPostingId());
        }
        for (Object[] row : postings.countApplicationsByPosting(ids, NEW_APPLICATION_STATUS)) {
            long total = ((Number) row[1]).longValue();
            long fresh = 0;
            if (row[2] != null) {
                fresh = ((Number) row[2]).longValue();
            }
            counts.put((Integer) row[0], new long[] {total, fresh});
        }
        return counts;
    }

    /** Bước 4: mô tả các bộ lọc đang bật thành chip. */
    private List<FilterChip> describeActiveFilters(JobPostingSearchRequest filter, JobPostingFilterOptions options) {
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
            chips.add(new FilterChip("Ngày đăng: " + formatDate(filter.postedFrom()) + " – "
                    + formatDate(filter.postedTo()), List.of("postedFrom", "postedTo")));
        }
        JobPostingSort sort = JobPostingSort.fromParam(filter.sort());
        if (sort != JobPostingSort.NEWEST) {
            chips.add(new FilterChip("Sắp xếp: " + sort.label(), List.of("sort")));
        }
        return chips;
    }

    // ----- Các hàm nhỏ -----

    /** Rule: page âm hoặc thiếu → trang đầu. */
    private static int pageNumber(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    /** Rule: size thiếu → mặc định; kẹp trong [1, MAX_PAGE_SIZE]. */
    private static int pageSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }

    private static String formatDate(LocalDate date) {
        if (date == null) {
            return OPEN_END;
        }
        return date.format(DISPLAY_DATE);
    }

    private List<FilterOption> statusOptions(List<String> statuses) {
        return statuses.stream().map(status -> new FilterOption(status, JobPostingLabels.statusLabel(status))).toList();
    }

    private List<FilterOption> deadlineOptions() {
        return JobPostingLabels.DEADLINE_FILTERS.stream()
                .map(value -> new FilterOption(value, JobPostingLabels.deadlineFilterLabel(value))).toList();
    }

    private List<FilterOption> sortOptions() {
        List<FilterOption> options = new ArrayList<>();
        for (JobPostingSort sort : JobPostingSort.values()) {
            options.add(new FilterOption(sort.param(), sort.label()));
        }
        return options;
    }

    private List<FilterOption> departmentOptions() {
        List<FilterOption> options = new ArrayList<>();
        for (Department department : departments.findAll(Sort.by("departmentName"))) {
            options.add(new FilterOption(String.valueOf(department.getDepartmentId()), department.getDepartmentName()));
        }
        return options;
    }

}
