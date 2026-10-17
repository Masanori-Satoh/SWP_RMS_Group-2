package com.group2.rms.requisition.service;

import com.group2.rms.admin.dto.ActivityLogResponse;
import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.requisition.dto.ApprovalResponse;
import com.group2.rms.requisition.dto.FilterOption;
import com.group2.rms.requisition.dto.RequisitionFilterChip;
import com.group2.rms.requisition.dto.RequisitionFilterOptions;
import com.group2.rms.requisition.dto.RequisitionListResponse;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.dto.RequisitionSearchRequest;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    // ===================================================================================
    // MÀN DANH SÁCH: phạm vi dữ liệu, bộ lọc theo role, tìm kiếm, phân trang
    // ===================================================================================

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

    /** Định dạng mã yêu cầu: REQ-0001, REQ-0025... (không kèm năm, theo quyết định của nhóm). */
    private static final String REQUISITION_CODE_FORMAT = "REQ-%04d";
    /** Giá trị DepartmentStatus của phòng ban đang hoạt động. */
    private static final String DEPARTMENT_ACTIVE = "Active";

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    /**
     * Rule: Phân quyền phạm vi dữ liệu theo Role người dùng (lớp bảo vệ theo dữ liệu):
     * - Hiring Manager: chỉ xem các yêu cầu do chính mình tạo
     * - Director: chỉ xem các yêu cầu đã nộp (khác Draft)
     * - HR: chỉ xem các yêu cầu đã được phê duyệt (Approved)
     * - System Admin: xem tất cả
     * Mọi bộ lọc khác đều được nối thêm (AND) vào điều kiện này, nên sửa URL cũng không thấy dữ liệu ngoài phạm vi.
     */
    private Specification<JobRequisition> scope(User currentUser) {
        return (root, query, criteriaBuilder) -> switch (access.role(currentUser)) {
            case RequisitionAccess.ROLE_HIRING_MANAGER ->
                    criteriaBuilder.equal(root.get("hiringManager").get("userId"), currentUser.getUserId());
            case RequisitionAccess.ROLE_DIRECTOR ->
                    criteriaBuilder.notEqual(root.get("approvalStatus"), RequisitionAccess.STATUS_DRAFT);
            case RequisitionAccess.ROLE_HR ->
                    criteriaBuilder.equal(root.get("approvalStatus"), RequisitionAccess.STATUS_APPROVED);
            default -> criteriaBuilder.conjunction();
        };
    }

    /**
     * Chuẩn bị toàn bộ dữ liệu cho màn danh sách, theo 4 bước:
     * 1. Xác định role được thấy ô lọc nào
     * 2. Làm sạch bộ lọc từ URL theo quyền
     * 3. Truy vấn DB
     * 4. Mô tả các bộ lọc đang bật thành chip
     */
    @Override
    @Transactional(readOnly = true)
    public RequisitionListResponse getListPage(RequisitionSearchRequest rawFilter) {
        User currentUser = access.actor();
        RequisitionFilterOptions options = buildFilterOptions(currentUser);
        RequisitionSearchRequest filter = normalizeFilter(rawFilter, options, currentUser);
        Page<RequisitionResponse> page = search(filter, currentUser);
        List<RequisitionFilterChip> chips = describeActiveFilters(filter, options);
        return new RequisitionListResponse(page, filter, options, chips);
    }

    /** Bước 1: mỗi role được thấy những ô lọc nào (đúng bảng đã chốt trong wireframe). */
    private RequisitionFilterOptions buildFilterOptions(User currentUser) {
        String role = access.role(currentUser);
        List<FilterOption> sorts = sortOptions();

        if (RequisitionAccess.ROLE_HIRING_MANAGER.equals(role)) {
            // HM chỉ thấy yêu cầu của mình nên không cần lọc phòng ban / người tạo
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
    private RequisitionSearchRequest normalizeFilter(RequisitionSearchRequest raw, RequisitionFilterOptions options,
            User currentUser) {
        // Từ khóa: bỏ khoảng trắng thừa, cắt bớt nếu quá dài
        String keyword = raw.q() == null ? "" : raw.q().trim();
        if (keyword.length() > RequisitionSpecifications.MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, RequisitionSpecifications.MAX_KEYWORD_LENGTH);
        }

        // Trạng thái duyệt
        List<String> statuses = null;
        if (options.showStatus()) {
            boolean firstVisit = raw.status() == null;
            if (firstVisit && RequisitionAccess.ROLE_DIRECTOR.equals(access.role(currentUser))) {
                // Rule: Director vào trang lần đầu (URL chưa có tham số status) thì tự lọc "Chờ duyệt"
                statuses = List.of(RequisitionAccess.STATUS_PENDING_DIRECTOR);
            } else {
                statuses = keepAllowedValues(raw.status(), options.statuses());
            }
        }

        // Các ô lọc một giá trị: chỉ giữ khi role có ô đó và giá trị nằm trong danh sách lựa chọn
        String posting = options.showPosting() && isAllowedValue(raw.posting(), options.postings())
                ? raw.posting() : null;
        Integer departmentId = options.showDepartment() && isAllowedValue(raw.departmentId(), options.departments())
                ? raw.departmentId() : null;
        Integer creatorId = options.showCreator() && isAllowedValue(raw.creatorId(), options.creators())
                ? raw.creatorId() : null;
        LocalDate createdFrom = options.showCreatedDate() ? raw.createdFrom() : null;
        LocalDate createdTo = options.showCreatedDate() ? raw.createdTo() : null;

        // Sắp xếp và phân trang
        String sort = RequisitionSort.fromParam(raw.sort()).param();
        int page = (raw.page() == null || raw.page() < 0) ? 0 : raw.page();
        int size = raw.size() == null ? DEFAULT_PAGE_SIZE : Math.max(1, Math.min(raw.size(), MAX_PAGE_SIZE));

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
        return result.map(jobRequisition -> response(jobRequisition, currentUser, false));
    }

    /** Bước 4: mô tả các bộ lọc đang bật bằng chữ, để hiện thành chip có nút ✕. */
    private List<RequisitionFilterChip> describeActiveFilters(RequisitionSearchRequest filter,
            RequisitionFilterOptions options) {
        List<RequisitionFilterChip> chips = new ArrayList<>();

        if (filter.status() != null && !filter.status().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (String status : filter.status()) {
                names.add(RequisitionLabels.approvalStatus(status));
            }
            chips.add(new RequisitionFilterChip("Trạng thái: " + String.join(", ", names), List.of("status")));
        }
        if (filter.posting() != null) {
            chips.add(new RequisitionFilterChip(
                    "Tin tuyển dụng: " + RequisitionLabels.postingStatus(filter.posting()), List.of("posting")));
        }
        if (filter.departmentId() != null) {
            chips.add(new RequisitionFilterChip(
                    "Phòng ban: " + labelOf(filter.departmentId(), options.departments()), List.of("departmentId")));
        }
        if (filter.creatorId() != null) {
            chips.add(new RequisitionFilterChip(
                    "Người tạo yêu cầu: " + labelOf(filter.creatorId(), options.creators()), List.of("creatorId")));
        }
        if (filter.createdFrom() != null || filter.createdTo() != null) {
            String from = filter.createdFrom() == null ? "…" : filter.createdFrom().format(DISPLAY_DATE);
            String to = filter.createdTo() == null ? "…" : filter.createdTo().format(DISPLAY_DATE);
            chips.add(new RequisitionFilterChip("Ngày tạo: " + from + " – " + to,
                    List.of("createdFrom", "createdTo")));
        }
        if (RequisitionSort.fromParam(filter.sort()) != RequisitionSort.NEWEST) {
            chips.add(new RequisitionFilterChip(
                    "Sắp xếp: " + RequisitionSort.fromParam(filter.sort()).label(), List.of("sort")));
        }
        return chips;
    }

    private static final java.time.format.DateTimeFormatter DISPLAY_DATE =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
        for (var department : departments.findAll(Sort.by("departmentName"))) {
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

    /** Giữ lại các giá trị nằm trong danh sách lựa chọn, bỏ giá trị lạ và giá trị trùng. */
    private List<String> keepAllowedValues(List<String> values, List<FilterOption> allowed) {
        List<String> kept = new ArrayList<>();
        if (values == null) {
            return kept;
        }
        for (String value : values) {
            if (isAllowedValue(value, allowed) && !kept.contains(value)) {
                kept.add(value);
            }
        }
        return kept;
    }

    private boolean isAllowedValue(Object value, List<FilterOption> allowed) {
        if (value == null) {
            return false;
        }
        for (FilterOption option : allowed) {
            if (option.value().equals(String.valueOf(value))) {
                return true;
            }
        }
        return false;
    }

    private String labelOf(Integer value, List<FilterOption> options) {
        for (FilterOption option : options) {
            if (option.value().equals(String.valueOf(value))) {
                return option.label();
            }
        }
        return String.valueOf(value);
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

        // Rule: Yêu cầu luôn thuộc phòng ban của người tạo (không lấy từ form).
        // Tài khoản chưa được gán phòng ban, hoặc phòng ban đã ngừng hoạt động, thì không tạo được
        // (cột DepartmentId là NOT NULL, báo lỗi ở đây thay vì để DB báo lỗi).
        var department = currentUser.getDepartment();
        if (department == null || !DEPARTMENT_ACTIVE.equals(department.getDepartmentStatus())) {
            throw createValidationException("departmentId",
                    "Tài khoản của bạn chưa được gán phòng ban đang hoạt động. Vui lòng liên hệ quản trị viên.");
        }
        requestDto.setDepartmentId(department.getDepartmentId());

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

        // Rule: Mã yêu cầu sinh từ RequisitionId, nên chỉ có sau khi DB cấp id ở lần lưu đầu tiên.
        // Gán vào entity đang được quản lý trong transaction, Hibernate tự UPDATE khi transaction kết thúc.
        jobRequisition.setRequisitionCode(formatRequisitionCode(jobRequisition.getRequisitionId()));

        createAuditLog(jobRequisition, currentUser, "CREATE", null, "Tạo yêu cầu: " + jobRequisition.getTitle() + " · " + jobRequisition.getApprovalStatus());

        return jobRequisition.getRequisitionId();
    }

    @Override
    public void updateRequisition(Integer id, RequisitionRequest requestDto) {
        User currentUser = access.actor();
        JobRequisition jobRequisition = findRequisitionById(id, true);
        // Rule: Chỉ cho phép chỉnh sửa khi còn là bản nháp và đúng người sở hữu
        access.requireEdit(currentUser, jobRequisition);

        // Rule: Sửa yêu cầu không đổi phòng ban: giữ phòng ban đã lưu
        // (Admin sửa phiếu của HM thì phiếu vẫn thuộc phòng ban của HM, không chuyển sang phòng ban của Admin)
        requestDto.setDepartmentId(jobRequisition.getDepartment().getDepartmentId());

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
        // Tên vị trí luôn có giá trị (validator bắt buộc cả khi lưu nháp)
        jobRequisition.setTitle(requestDto.getTitle());
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
        // Rule: Bản nháp được phép để trống mô tả / yêu cầu, nhưng 2 cột này NOT NULL trong DB
        // nên lưu chuỗi rỗng thay cho null (khi gửi duyệt, validator đã bắt buộc nhập)
        jobRequisition.setJobDescription(Objects.requireNonNullElse(requestDto.getJobDescription(), ""));
        jobRequisition.setRequirementDetails(Objects.requireNonNullElse(requestDto.getRequirementDetails(), ""));
    }

    /** Mã yêu cầu dạng REQ-0025: "REQ-" + RequisitionId, đệm cho đủ 4 chữ số. */
    private static String formatRequisitionCode(Integer requisitionId) {
        return String.format(REQUISITION_CODE_FORMAT, requisitionId);
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
        // Rule: Không cho phép xóa phiếu gốc khi đã có đợt tuyển sau sao chép từ nó
        // (nếu xóa, các đợt sau mất phiếu gốc và vi phạm khóa ngoại FK_JobRequisition_Original)
        if (requisitions.existsByOriginalRequisition_RequisitionId(id)) {
            throw createValidationException("action", "Yêu cầu này đã có đợt tuyển sau được sao chép từ nó và không thể xóa.");
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
                .requisitionCode(jobRequisition.getRequisitionCode() != null ? jobRequisition.getRequisitionCode() : formatRequisitionCode(jobRequisition.getRequisitionId()))
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
                .deletable(isEditable
                        && !postings.existsByRequisition_RequisitionId(jobRequisition.getRequisitionId())
                        && !requisitions.existsByOriginalRequisition_RequisitionId(jobRequisition.getRequisitionId()))
                .decidable(access.canDecide(currentUser, jobRequisition))
                .withdrawable(access.owns(currentUser, jobRequisition)
                        && RequisitionAccess.STATUS_PENDING_DIRECTOR.equals(jobRequisition.getApprovalStatus()))
                .build();

        postings.findTopByRequisition_RequisitionIdOrderByJobPostingIdDesc(jobRequisition.getRequisitionId())
                .ifPresent(p -> {
                    responseDto.setPostingStatus(p.getPostingStatus());
                    responseDto.setJobPostingId(p.getJobPostingId());
                    responseDto.setPostingNo(p.getPostingNo());
                    responseDto.setPostingDate(p.getPostingDate());
                    responseDto.setPostingDeadline(p.getApplicationDeadline());
                });

        // Nhãn tiếng Việt + tông màu badge (dịch ở một chỗ duy nhất: RequisitionLabels)
        String approvalStatus = jobRequisition.getApprovalStatus();
        responseDto.setStatusLabel(RequisitionLabels.approvalStatus(approvalStatus));
        responseDto.setStatusTone(RequisitionLabels.approvalTone(approvalStatus));
        responseDto.setPostingLabel(RequisitionLabels.postingStatus(responseDto.getPostingStatus()));
        responseDto.setPostingTone(RequisitionLabels.postingTone(responseDto.getPostingStatus()));
        responseDto.setEmploymentTypeLabel(RequisitionLabels.employmentType(jobRequisition.getEmploymentType()));

        // Cờ quyền cho menu ⋯ (kiểm quyền ở server, template chỉ đọc cờ)
        boolean hasPosting = responseDto.getJobPostingId() != null;
        boolean isApproved = RequisitionAccess.STATUS_APPROVED.equals(approvalStatus);
        // Rule: chỉ sao chép được yêu cầu đã gửi đi (không phải Draft / Pending_Director)
        responseDto.setCopyable(access.canCreate(currentUser) && !STATUSES_NOT_COPYABLE.contains(approvalStatus));
        // Rule: tin tuyển dụng chỉ tạo từ yêu cầu Đã duyệt và chưa có tin
        responseDto.setPostable(isApproved && !hasPosting && access.canCreatePosting(currentUser));
        // Rule: chỉ hiện link tin tuyển dụng cho role vào được trang tin (Director sẽ bị 403)
        responseDto.setPostingViewable(hasPosting && access.canViewPosting(currentUser));

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
                criterionResponse.setCriteriaTypeLabel(RequisitionLabels.criteriaType(c.getCriteriaType()));
                criterionResponse.setRequiredValue(c.getRequiredValue());
                criterionResponse.setWeight(c.getWeight());
                criterionResponse.setIsMandatory(c.getIsMandatory());
                return criterionResponse;
            }).toList());

            // Tổng trọng số tiêu chí (hiện dưới bảng tiêu chí)
            BigDecimal totalWeight = BigDecimal.ZERO;
            for (ScreeningCriteria criterion : jobRequisition.getScreeningCriteria()) {
                if (criterion.getWeight() != null) {
                    totalWeight = totalWeight.add(criterion.getWeight());
                }
            }
            responseDto.setCriteriaTotalWeight(totalWeight.stripTrailingZeros());

            // Nhãn tiếng Việt cho các trường thông tin cơ bản
            responseDto.setSalaryLabel(RequisitionLabels.salary(jobRequisition.getMinSalary(), jobRequisition.getMaxSalary()));
            responseDto.setProbationLabel(RequisitionLabels.probation(jobRequisition.getProbationDays()));
            responseDto.setGenderLabel(RequisitionLabels.gender(jobRequisition.getGender()));
            responseDto.setWorkModelLabel(RequisitionLabels.workModel(jobRequisition.getWorkModel()));
            responseDto.setHiringTypeLabel(RequisitionLabels.hiringType(jobRequisition.getHiringType()));

            // Văn bản nhiều dòng: escape HTML rồi đổi xuống dòng thành <br/> (AG §6.3)
            responseDto.setReasonForHiringHtml(RequisitionLabels.multilineHtml(jobRequisition.getReasonForHiring()));
            responseDto.setJobDescriptionHtml(RequisitionLabels.multilineHtml(jobRequisition.getJobDescription()));
            responseDto.setRequirementDetailsHtml(RequisitionLabels.multilineHtml(jobRequisition.getRequirementDetails()));

            // Mốc thời gian: lúc gửi duyệt, số ngày đã chờ duyệt, còn bao nhiêu ngày tới ngày bắt đầu
            responseDto.setSubmittedAt(jobRequisition.getSubmittedAt());
            if (RequisitionAccess.STATUS_PENDING_DIRECTOR.equals(jobRequisition.getApprovalStatus())
                    && jobRequisition.getSubmittedAt() != null) {
                responseDto.setWaitingDays(ChronoUnit.DAYS.between(jobRequisition.getSubmittedAt().toLocalDate(), LocalDate.now()));
            }
            if (jobRequisition.getExpectedStartDate() != null) {
                responseDto.setDaysUntilStart(ChronoUnit.DAYS.between(LocalDate.now(), jobRequisition.getExpectedStartDate()));
            }

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
