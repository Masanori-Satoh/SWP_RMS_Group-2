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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Màn "Danh sách yêu cầu tuyển dụng": phạm vi dữ liệu theo role, bộ lọc, tìm kiếm, phân trang.
 *
 * Tách riêng khỏi RequisitionServiceImpl (thêm / sửa / duyệt) để mỗi file chỉ lo một màn.
 * Màn danh sách khác (ví dụ tin tuyển dụng) có thể làm theo đúng 4 bước của getListPage.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RequisitionListService {

    /** Tất cả trạng thái duyệt có thể lọc ở màn danh sách. */
    private static final List<String> ALL_STATUSES = List.of(
            RequisitionAccess.STATUS_DRAFT,
            RequisitionAccess.STATUS_PENDING_DIRECTOR,
            RequisitionAccess.STATUS_APPROVED,
            RequisitionAccess.STATUS_REJECTED);

    /** Director không xem bản nháp nên không có lựa chọn "Bản nháp". */
    private static final List<String> STATUSES_WITHOUT_DRAFT = List.of(
            RequisitionAccess.STATUS_PENDING_DIRECTOR,
            RequisitionAccess.STATUS_APPROVED,
            RequisitionAccess.STATUS_REJECTED);

    /** Các lựa chọn của ô lọc "Tin tuyển dụng" (theo trạng thái tin mới nhất). */
    private static final List<String> POSTING_FILTER_VALUES = List.of(
            RequisitionLabels.POSTING_NONE,
            RequisitionLabels.POSTING_PUBLISHED,
            RequisitionLabels.POSTING_PAUSED,
            RequisitionLabels.POSTING_CLOSED);

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JobRequisitionRepository requisitions;
    private final DepartmentRepository departments;
    private final JobPostingRepository postings;
    private final RequisitionAccess access;
    private final RequisitionMapper mapper;

    /**
     * Chuẩn bị toàn bộ dữ liệu cho màn danh sách, theo 4 bước:
     * 1. Xác định role được thấy ô lọc nào
     * 2. Làm sạch bộ lọc từ URL theo quyền
     * 3. Truy vấn DB
     * 4. Mô tả các bộ lọc đang bật thành chip
     */
    public RequisitionListResponse getListPage(RequisitionSearchRequest rawFilter) {
        User currentUser = access.actor();
        RequisitionFilterOptions options = buildFilterOptions(currentUser);
        RequisitionSearchRequest filter = normalizeFilter(rawFilter, options);
        Page<RequisitionResponse> page = search(filter, currentUser);
        List<FilterChip> chips = describeActiveFilters(filter, options);
        return new RequisitionListResponse(page, filter, options, chips);
    }

    /**
     * Rule: Phân quyền phạm vi dữ liệu theo Role người dùng (lớp bảo vệ theo dữ liệu):
     * - Hiring Manager: yêu cầu do mình tạo + mọi yêu cầu của phòng ban mình (phòng mình thuộc hoặc mình làm trưởng phòng),
     *   khớp với quyền xem chi tiết RequisitionAccess#requireView
     * - Director: chỉ xem các yêu cầu đã nộp (khác Draft)
     * - HR: chỉ xem các yêu cầu đã được phê duyệt (Approved)
     * - System Admin: xem tất cả
     * Mọi bộ lọc khác đều được nối thêm (AND) vào điều kiện này, nên sửa URL cũng không thấy dữ liệu ngoài phạm vi.
     */
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
                    myDepartment = cb.equal(department.get("departmentId"), currentUser.getDepartment().getDepartmentId());
                }
                return cb.or(createdByMe, iManageDepartment, myDepartment);
            };
        }
        if (RequisitionAccess.ROLE_DIRECTOR.equals(role)) {
            return (root, query, cb) -> cb.notEqual(root.get("approvalStatus"), RequisitionAccess.STATUS_DRAFT);
        }
        if (RequisitionAccess.ROLE_HR.equals(role)) {
            return (root, query, cb) -> cb.equal(root.get("approvalStatus"), RequisitionAccess.STATUS_APPROVED);
        }
        // System Admin: không giới hạn
        return (root, query, cb) -> cb.conjunction();
    }

    /** Bước 1: mỗi role được thấy những ô lọc nào (đúng bảng đã chốt trong wireframe). */
    private RequisitionFilterOptions buildFilterOptions(User currentUser) {
        String role = access.role(currentUser);
        List<FilterOption> sorts = sortOptions();

        if (RequisitionAccess.ROLE_HIRING_MANAGER.equals(role)) {
            // HM chỉ thấy yêu cầu của phòng ban mình nên không cần lọc phòng ban.
            // Chưa có ô "Người tạo": creatorOptions lấy người tạo của mọi phòng ban, sẽ lộ tên người ngoài phạm vi
            return new RequisitionFilterOptions(true, true, false, false, true,
                    statusOptions(ALL_STATUSES), postingOptions(), List.of(), List.of(), sorts);
        }
        if (RequisitionAccess.ROLE_DIRECTOR.equals(role)) {
            // Director không xem bản nháp nên ô trạng thái không có "Bản nháp"
            return new RequisitionFilterOptions(true, false, true, true, false,
                    statusOptions(STATUSES_WITHOUT_DRAFT), List.of(), departmentOptions(),
                    creatorOptions(STATUSES_WITHOUT_DRAFT), sorts);
        }
        if (RequisitionAccess.ROLE_HR.equals(role)) {
            // HR chỉ thấy yêu cầu Đã duyệt nên lọc trạng thái là thừa; HR cần lọc theo tin tuyển dụng
            List<String> approvedOnly = List.of(RequisitionAccess.STATUS_APPROVED);
            return new RequisitionFilterOptions(false, true, true, true, false,
                    List.of(), postingOptions(), departmentOptions(), creatorOptions(approvedOnly), sorts);
        }
        // System Admin: nhiều ô lọc nhất
        return new RequisitionFilterOptions(true, false, true, true, true,
                statusOptions(ALL_STATUSES), List.of(), departmentOptions(), creatorOptions(ALL_STATUSES), sorts);
    }

    /**
     * Bước 2: không tin dữ liệu từ URL. Giá trị nào role không được dùng, hoặc không nằm trong danh sách lựa chọn,
     * thì bỏ qua (coi như không lọc) thay vì báo lỗi.
     */
    private RequisitionSearchRequest normalizeFilter(RequisitionSearchRequest raw, RequisitionFilterOptions options) {
        String keyword = ListFilters.cleanKeyword(raw.q());

        // Trạng thái duyệt: không lọc sẵn cho role nào (Director vào trang cũng thấy tất cả)
        List<String> statuses = null;
        if (options.showStatus()) {
            statuses = ListFilters.keepAllowedValues(raw.status(), options.statuses());
        }

        // Các ô lọc một giá trị: chỉ giữ khi role có ô đó và giá trị nằm trong danh sách lựa chọn
        String posting = null;
        if (options.showPosting() && ListFilters.isAllowedValue(raw.posting(), options.postings())) {
            posting = raw.posting();
        }
        Integer departmentId = null;
        if (options.showDepartment() && ListFilters.isAllowedValue(raw.departmentId(), options.departments())) {
            departmentId = raw.departmentId();
        }
        Integer creatorId = null;
        if (options.showCreator() && ListFilters.isAllowedValue(raw.creatorId(), options.creators())) {
            creatorId = raw.creatorId();
        }
        LocalDate createdFrom = null;
        LocalDate createdTo = null;
        if (options.showCreatedDate()) {
            createdFrom = raw.createdFrom();
            createdTo = raw.createdTo();
        }

        // Sắp xếp và phân trang
        String sort = RequisitionSort.fromParam(raw.sort()).param();
        int page = 0;
        if (raw.page() != null && raw.page() > 0) {
            page = raw.page();
        }
        int size = DEFAULT_PAGE_SIZE;
        if (raw.size() != null) {
            size = Math.max(1, Math.min(raw.size(), MAX_PAGE_SIZE));
        }

        return new RequisitionSearchRequest(keyword, statuses, posting, departmentId, creatorId,
                createdFrom, createdTo, sort, page, size);
    }

    /** Bước 3: ghép điều kiện WHERE rồi truy vấn một trang. */
    private Page<RequisitionResponse> search(RequisitionSearchRequest filter, User currentUser) {
        // Luôn bắt đầu từ phạm vi theo role, sau đó nối thêm từng bộ lọc
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
        if (filter.createdFrom() != null) {
            where = where.and(RequisitionSpecifications.createdOnOrAfter(filter.createdFrom()));
        }
        if (filter.createdTo() != null) {
            where = where.and(RequisitionSpecifications.createdOnOrBefore(filter.createdTo()));
        }

        Sort sort = RequisitionSort.fromParam(filter.sort()).toSort();
        Page<JobRequisition> result = requisitions.findAll(where, PageRequest.of(filter.page(), filter.size(), sort));

        // Rule: gõ số trang vượt quá tổng số trang thì tự lùi về trang cuối thay vì hiện bảng trống
        if (result.getTotalPages() > 0 && result.getNumber() >= result.getTotalPages()) {
            result = requisitions.findAll(where, PageRequest.of(result.getTotalPages() - 1, filter.size(), sort));
        }

        // Tránh N+1: tra tin mới nhất và "đã có đợt sau" cho CẢ TRANG bằng 2 câu, thay vì vài câu cho mỗi dòng
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

    /** RequisitionId → tin tuyển dụng mới nhất của yêu cầu đó (yêu cầu chưa có tin thì không có trong map). */
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

    /** Các id (trong trang hiện tại) đã có đợt tuyển sau sao chép từ nó. */
    private Set<Integer> idsWithLaterRounds(List<Integer> requisitionIds) {
        if (requisitionIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(requisitions.findIdsWithLaterRounds(requisitionIds));
    }

    /** Bước 4: mô tả các bộ lọc đang bật bằng chữ, để hiện thành chip có nút ✕. */
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

    // ----- Các hàm nhỏ tạo danh sách lựa chọn cho ô lọc -----

    private List<FilterOption> statusOptions(List<String> statuses) {
        List<FilterOption> options = new ArrayList<>();
        for (String status : statuses) {
            options.add(new FilterOption(status, RequisitionLabels.approvalStatus(status)));
        }
        return options;
    }

    private List<FilterOption> postingOptions() {
        List<FilterOption> options = new ArrayList<>();
        for (String posting : POSTING_FILTER_VALUES) {
            options.add(new FilterOption(posting, RequisitionLabels.postingStatus(posting)));
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

    /** Chỉ liệt kê HM có yêu cầu ở các trạng thái người xem được thấy, không lộ tên người ngoài phạm vi. */
    private List<FilterOption> creatorOptions(List<String> visibleStatuses) {
        List<FilterOption> options = new ArrayList<>();
        for (User creator : requisitions.findCreatorsWithStatusIn(visibleStatuses)) {
            options.add(new FilterOption(String.valueOf(creator.getUserId()), creator.getFullName()));
        }
        return options;
    }

    private List<FilterOption> sortOptions() {
        List<FilterOption> options = new ArrayList<>();
        for (RequisitionSort sort : RequisitionSort.values()) {
            options.add(new FilterOption(sort.param(), sort.label()));
        }
        return options;
    }
}
