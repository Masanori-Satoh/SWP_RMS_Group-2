package com.group2.rms.requisition.service;

import com.group2.rms.core.dto.FilterChip;
import com.group2.rms.core.dto.FilterOption;
import com.group2.rms.core.util.ListFilters;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.dto.RequisitionFilterOptions;
import com.group2.rms.requisition.dto.RequisitionListResponse;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.dto.RequisitionSearchRequest;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RequisitionListService {


    private static final List<String> ALL_STATUSES = List.of(
            RequisitionAccess.STATUS_DRAFT,
            RequisitionAccess.STATUS_PENDING_DIRECTOR,
            RequisitionAccess.STATUS_APPROVED,
            RequisitionAccess.STATUS_REJECTED);


    private static final List<String> STATUSES_WITHOUT_DRAFT = List.of(
            RequisitionAccess.STATUS_PENDING_DIRECTOR,
            RequisitionAccess.STATUS_APPROVED,
            RequisitionAccess.STATUS_REJECTED);


    private static final List<String> POSTING_FILTER_VALUES = List.of(
            RequisitionLabels.POSTING_NONE,
            RequisitionLabels.POSTING_PUBLISHED,
            RequisitionLabels.POSTING_PAUSED,
            RequisitionLabels.POSTING_CLOSED);

    private static final Map<String, String> PARAM_LABELS = Map.of(
            "departmentId", "phòng ban",
            "creatorId", "người tạo yêu cầu",
            "createdFrom", "ngày tạo từ",
            "createdTo", "ngày tạo đến",
            "page", "số trang",
            "size", "số dòng mỗi trang");

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JobRequisitionRepository requisitions;
    private final DepartmentRepository departments;
    private final JobPostingRepository postings;
    private final RequisitionAccess access;
    private final RequisitionMapper mapper;


    // Returns the filtered requisition list page.
    public RequisitionListResponse getListPage(RequisitionSearchRequest rawFilter) {
        return getListPage(rawFilter, List.of());
    }

    // unparsableParams: tham số URL sai kiểu (departmentId=abc, createdFrom=xyz) do controller gom từ BindingResult.
    public RequisitionListResponse getListPage(RequisitionSearchRequest rawFilter, Collection<String> unparsableParams) {
        User currentUser = access.actor();
        RequisitionFilterOptions options = buildFilterOptions(currentUser);
        List<String> errors = new ArrayList<>();
        for (String param : unparsableParams) {
            errors.add(ListFilters.invalidFilterMessage(PARAM_LABELS.getOrDefault(param, param)));
        }
        RequisitionSearchRequest filter = normalizeFilter(rawFilter, options, errors);
        Page<RequisitionResponse> page = errors.isEmpty()
                ? search(filter, currentUser, errors)
                : Page.empty(PageRequest.of(filter.page(), filter.size()));
        List<FilterChip> chips = describeActiveFilters(filter, options);
        return new RequisitionListResponse(page, filter, options, chips, errors);
    }


    // Builds the visibility scope for the current user.
    private Specification<JobRequisition> scope(User currentUser) {
        String role = access.role(currentUser);
        if (RequisitionAccess.ROLE_HIRING_MANAGER.equals(role)) {
            return (root, query, cb) -> {
                Join<Object, Object> department = root.join("department", JoinType.LEFT);
                Predicate createdByMe = cb.equal(root.get("hiringManager").get("userId"), currentUser.getUserId());
                Predicate iManageDepartment = cb.equal(
                        department.join("manager", JoinType.LEFT).get("userId"), currentUser.getUserId());
                Predicate myDepartment = cb.disjunction();
                if (currentUser.getDepartment() != null) {
                    myDepartment = cb.equal(department.get("departmentId"),
                            currentUser.getDepartment().getDepartmentId());
                }
                return cb.or(createdByMe, iManageDepartment, myDepartment);
            };
        }
        if (RequisitionAccess.ROLE_DIRECTOR.equals(role)) {
            return (root, query, cb) -> cb.or(
                    cb.notEqual(root.get("approvalStatus"), RequisitionAccess.STATUS_DRAFT),
                    cb.equal(root.get("hiringManager").get("userId"), currentUser.getUserId()));
        }
        if (RequisitionAccess.ROLE_HR.equals(role)) {
            return (root, query, cb) -> cb.equal(root.get("approvalStatus"), RequisitionAccess.STATUS_APPROVED);
        }
        throw new org.springframework.security.access.AccessDeniedException(
                "Vai trò không có quyền xem yêu cầu tuyển dụng.");
    }


    // Builds the filter options available to the current user.
    private RequisitionFilterOptions buildFilterOptions(User currentUser) {
        String role = access.role(currentUser);

        // Mỗi role chỉ 2 ô lọc trong popover (vừa 1 hàng): HM = tin + hình thức, Director = phòng ban + người tạo,
        // HR = tin + phòng ban.
        if (RequisitionAccess.ROLE_HIRING_MANAGER.equals(role)) {
            return new RequisitionFilterOptions(true, true, false, false, false, true,
                    statusOptions(ALL_STATUSES), postingOptions(), List.of(), List.of(), employmentTypeOptions());
        }
        if (RequisitionAccess.ROLE_DIRECTOR.equals(role)) {
            return new RequisitionFilterOptions(true, false, true, true, false, false,
                    statusOptions(ALL_STATUSES), List.of(), departmentOptions(),
                    creatorOptions(STATUSES_WITHOUT_DRAFT), List.of());
        }
        if (RequisitionAccess.ROLE_HR.equals(role)) {
            return new RequisitionFilterOptions(false, true, true, false, false, false,
                    List.of(), postingOptions(), departmentOptions(), List.of(), List.of());
        }
        throw new org.springframework.security.access.AccessDeniedException(
                "Vai trò không có quyền xem yêu cầu tuyển dụng.");
    }


    // Validates and normalizes the requisition list filters.
    private RequisitionSearchRequest normalizeFilter(RequisitionSearchRequest raw, RequisitionFilterOptions options,
            List<String> errors) {
        String keyword = ListFilters.cleanKeyword(raw.q());
        if (raw.q() != null && raw.q().length() > 120) {
            errors.add("Từ khóa tìm kiếm không được vượt quá 120 ký tự.");
        }
        List<String> statuses = null;
        if (ListFilters.hasInvalidChoice(raw.status(), options.showStatus(), options.statuses())) {
            errors.add(ListFilters.invalidFilterMessage("trạng thái"));
        } else if (options.showStatus()) {
            statuses = ListFilters.keepAllowedValues(raw.status(), options.statuses()).stream().limit(1).toList();
        }
        String posting = null;
        if (ListFilters.isInvalidChoice(raw.posting(), options.showPosting(), options.postings())) {
            errors.add(ListFilters.invalidFilterMessage("tin tuyển dụng"));
        } else if (raw.posting() != null && !raw.posting().isBlank()) {
            posting = raw.posting();
        }
        Integer departmentId = null;
        if (ListFilters.isInvalidChoice(raw.departmentId(), options.showDepartment(), options.departments())) {
            errors.add(ListFilters.invalidFilterMessage("phòng ban"));
        } else {
            departmentId = raw.departmentId();
        }
        Integer creatorId = null;
        if (ListFilters.isInvalidChoice(raw.creatorId(), options.showCreator(), options.creators())) {
            errors.add(ListFilters.invalidFilterMessage("người tạo yêu cầu"));
        } else {
            creatorId = raw.creatorId();
        }
        String employmentType = null;
        if (ListFilters.isInvalidChoice(raw.employmentType(), options.showEmploymentType(), options.employmentTypes())) {
            errors.add(ListFilters.invalidFilterMessage("hình thức làm việc"));
        } else if (raw.employmentType() != null && !raw.employmentType().isBlank()) {
            employmentType = raw.employmentType();
        }
        LocalDate createdFrom = raw.createdFrom();
        LocalDate createdTo = raw.createdTo();
        if (!options.showCreatedDate() && (createdFrom != null || createdTo != null)) {
            errors.add(ListFilters.invalidFilterMessage("ngày tạo"));
            createdFrom = null;
            createdTo = null;
        } else if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            errors.add("Ngày tạo từ không được sau ngày tạo đến.");
        }
        String sort = RequisitionSort.fromParam(raw.sort()).param();
        if (raw.sort() != null && !raw.sort().isBlank() && !sort.equalsIgnoreCase(raw.sort())) {
            errors.add("Giá trị sắp xếp không hợp lệ. Vui lòng chọn tùy chọn sắp xếp hợp lệ.");
        }
        int page = 0;
        if (raw.page() != null && raw.page() < 0) {
            errors.add("Số trang phải lớn hơn hoặc bằng 1.");
        } else if (raw.page() != null) {
            page = raw.page();
        }
        int size = DEFAULT_PAGE_SIZE;
        if (raw.size() != null && raw.size() < 1) {
            errors.add("Số dòng mỗi trang phải lớn hơn hoặc bằng 1.");
        } else if (raw.size() != null && raw.size() > MAX_PAGE_SIZE) {
            errors.add("Số dòng mỗi trang không được vượt quá " + MAX_PAGE_SIZE + ".");
        } else if (raw.size() != null) {
            size = raw.size();
        }

        return new RequisitionSearchRequest(keyword, statuses, posting, departmentId, creatorId,
                createdFrom, createdTo, sort, page, size, employmentType);
    }


    // Searches requisitions using the normalized filters.
    private Page<RequisitionResponse> search(RequisitionSearchRequest filter, User currentUser, List<String> errors) {
        Specification<JobRequisition> where = scope(currentUser);

        if (!filter.q().isEmpty()) {
            where = where.and(RequisitionSpecifications.titleOrDepartmentContains(filter.q()));
        }
        if (filter.status() != null && !filter.status().isEmpty()) {
            where = where.and(RequisitionSpecifications.statusIn(filter.status()));
        }
        if (RequisitionLabels.POSTING_NONE.equals(filter.posting())) {
            where = where.and(RequisitionSpecifications.hasNoPosting());
        } else if (filter.posting() != null) {
            where = where.and(RequisitionSpecifications.latestPostingStatusIs(filter.posting()));
        }
        if (filter.departmentId() != null) {
            where = where.and(RequisitionSpecifications.departmentIs(filter.departmentId()));
        }
        if (filter.creatorId() != null) {
            where = where.and(RequisitionSpecifications.creatorIs(filter.creatorId()));
        }
        if (filter.employmentType() != null) {
            where = where.and(RequisitionSpecifications.employmentTypeIs(filter.employmentType()));
        }
        if (filter.createdFrom() != null) {
            where = where.and(RequisitionSpecifications.createdOnOrAfter(filter.createdFrom()));
        }
        if (filter.createdTo() != null) {
            where = where.and(RequisitionSpecifications.createdOnOrBefore(filter.createdTo()));
        }

        RequisitionSort sortOption = RequisitionSort.fromParam(filter.sort());
        Sort sort = sortOption.toSort();
        if (sortOption == RequisitionSort.NEWEST) {
            // CASE ordering can't be expressed with Sort; the spec sets ORDER BY, so pass unsorted.
            where = where.and(RequisitionSpecifications.pendingFirstThenNewest());
            sort = Sort.unsorted();
        }
        Page<JobRequisition> result = requisitions.findAll(where, PageRequest.of(filter.page(), filter.size(), sort));
        if (filter.page() > 0 && filter.page() >= result.getTotalPages()) {
            errors.add("Trang bạn yêu cầu không tồn tại.");
            return Page.empty(result.getPageable());
        }
        List<Integer> ids = new ArrayList<>();
        for (JobRequisition jobRequisition : result.getContent()) {
            ids.add(jobRequisition.getRequisitionId());
        }
        Map<Integer, JobPosting> latestPostings = latestPostingsOf(ids);
        Set<Integer> idsWithLaterRounds = idsWithLaterRounds(ids);

        return result.map(jobRequisition -> mapper.toListRow(jobRequisition, currentUser,
                latestPostings.get(jobRequisition.getRequisitionId()),
                idsWithLaterRounds.contains(jobRequisition.getRequisitionId())));
    }


    // Returns the latest posting for each requisition.
    private Map<Integer, JobPosting> latestPostingsOf(List<Integer> requisitionIds) {
        Map<Integer, JobPosting> latestByRequisition = new HashMap<>();
        if (requisitionIds.isEmpty()) {
            return latestByRequisition;
        }
        for (JobPosting posting : postings.findLatestByRequisitionIds(requisitionIds)) {
            latestByRequisition.put(posting.getRequisition().getRequisitionId(), posting);
        }
        return latestByRequisition;
    }


    // Returns requisition IDs that have later rounds.
    private Set<Integer> idsWithLaterRounds(List<Integer> requisitionIds) {
        if (requisitionIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(requisitions.findIdsWithLaterRounds(requisitionIds));
    }


    // Builds labels for the active filters.
    private List<FilterChip> describeActiveFilters(RequisitionSearchRequest filter, RequisitionFilterOptions options) {
        List<FilterChip> chips = new ArrayList<>();

        if (filter.status() != null && !filter.status().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (String status : filter.status()) {
                names.add(RequisitionLabels.approvalStatus(status));
            }
            chips.add(new FilterChip("Trạng thái: " + String.join(", ", names), List.of("status")));
        }
        if (filter.posting() != null) {
            chips.add(new FilterChip(
                    "Tin tuyển dụng: " + RequisitionLabels.postingStatus(filter.posting()), List.of("posting")));
        }
        if (filter.departmentId() != null) {
            chips.add(new FilterChip(
                    "Phòng ban: " + ListFilters.labelOf(filter.departmentId(), options.departments()),
                    List.of("departmentId")));
        }
        if (filter.creatorId() != null) {
            chips.add(new FilterChip(
                    "Người tạo yêu cầu: " + ListFilters.labelOf(filter.creatorId(), options.creators()),
                    List.of("creatorId")));
        }
        if (filter.employmentType() != null) {
            chips.add(new FilterChip(
                    "Hình thức làm việc: " + RequisitionLabels.employmentType(filter.employmentType()),
                    List.of("employmentType")));
        }
        if (filter.createdFrom() != null || filter.createdTo() != null) {
            String from = "…";
            if (filter.createdFrom() != null) {
                from = filter.createdFrom().format(DISPLAY_DATE);
            }
            String to = "…";
            if (filter.createdTo() != null) {
                to = filter.createdTo().format(DISPLAY_DATE);
            }
            chips.add(new FilterChip("Ngày tạo: " + from + " – " + to, List.of("createdFrom", "createdTo")));
        }
        RequisitionSort sort = RequisitionSort.fromParam(filter.sort());
        if (sort != RequisitionSort.NEWEST) {
            chips.add(new FilterChip("Sắp xếp: " + sort.label(), List.of("sort")));
        }
        return chips;
    }

    // Builds the available status options.
    private List<FilterOption> statusOptions(List<String> statuses) {
        List<FilterOption> options = new ArrayList<>();
        for (String status : statuses) {
            options.add(new FilterOption(status, RequisitionLabels.approvalStatus(status)));
        }
        return options;
    }

    // Builds the available posting options.
    private List<FilterOption> postingOptions() {
        List<FilterOption> options = new ArrayList<>();
        for (String posting : POSTING_FILTER_VALUES) {
            options.add(new FilterOption(posting, RequisitionLabels.postingStatus(posting)));
        }
        return options;
    }

    // Builds the available department options.
    private List<FilterOption> departmentOptions() {
        List<FilterOption> options = new ArrayList<>();
        for (Department department : departments.findAll(Sort.by("departmentName"))) {
            options.add(new FilterOption(String.valueOf(department.getDepartmentId()), department.getDepartmentName()));
        }
        return options;
    }


    // Builds the available creator options.
    private List<FilterOption> creatorOptions(List<String> visibleStatuses) {
        List<FilterOption> options = new ArrayList<>();
        for (User creator : requisitions.findCreatorsWithStatusIn(visibleStatuses)) {
            options.add(new FilterOption(String.valueOf(creator.getUserId()), creator.getFullName()));
        }
        return options;
    }

    // Builds the available employment type options.
    private List<FilterOption> employmentTypeOptions() {
        List<FilterOption> options = new ArrayList<>();
        for (String type : RequisitionValidator.EMPLOYMENT_TYPES) {
            options.add(new FilterOption(type, RequisitionLabels.employmentType(type)));
        }
        return options;
    }
}
