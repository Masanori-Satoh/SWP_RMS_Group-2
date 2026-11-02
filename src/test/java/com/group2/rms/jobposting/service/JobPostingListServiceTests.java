package com.group2.rms.jobposting.service;

import com.group2.rms.core.dto.FilterChip;
import com.group2.rms.core.dto.FilterOption;
import com.group2.rms.jobposting.dto.InternalJobPostingResponse;
import com.group2.rms.jobposting.dto.JobPostingListResponse;
import com.group2.rms.jobposting.dto.JobPostingSearchRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.jobposting.validator.JobPostingValidator;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST (Mockito): JobPostingListService (màn danh sách tin tuyển dụng).
 * Kiểm tra: ô lọc theo vai trò, làm sạch tham số URL, chip, phân trang, đếm hồ sơ gộp (chống N+1).
 * Repository là mock; Access / Mapper / Validator dùng bản thật (hàm thuần).
 * Điều kiện SQL (Specification) được kiểm tra thật ở JobPostingIntegrationTests.
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện, Clock cố định.
 */
@ExtendWith(MockitoExtension.class)
class JobPostingListServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private JobPostingRepository postings;
    @Mock
    private DepartmentRepository departments;

    private JobPostingListService service;
    private JobRequisition requisition;
    private final List<JobPosting> rows = new ArrayList<>();
    private final List<Object[]> counts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        JobPostingAccess access = new JobPostingAccess(new RequisitionAccess(null));
        JobPostingMapper mapper = new JobPostingMapper(access, new JobPostingValidator(CLOCK));
        service = new JobPostingListService(postings, departments, access, mapper, CLOCK);

        Department engineering = Department.builder().departmentId(1).departmentName("Engineering").build();
        requisition = JobRequisition.builder().requisitionId(5).requisitionCode("REQ-0005").title("Java Dev")
                .department(engineering).employmentType("Full-time").recruitmentRound(1).build();

        lenient().when(postings.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(rows, invocation.getArgument(1), rows.size()));
        lenient().when(postings.countApplicationsByPosting(anyCollection(), eq(JobPostingListService.NEW_APPLICATION_STATUS)))
                .thenReturn(counts);
        lenient().when(departments.findAll(any(Sort.class))).thenReturn(List.of(engineering));
    }

    // ----- Hàm hỗ trợ -----

    private static User userWithRole(String roleName) {
        return User.builder().userId(7).fullName("Người xem").role(Role.builder().roleName(roleName).build()).build();
    }

    private static JobPostingSearchRequest emptyFilter() {
        return new JobPostingSearchRequest(null, null, null, null, null, null, null, null, null, null);
    }

    /** Thêm một tin vào kết quả truy vấn giả, kèm số hồ sơ [tổng, mới]. */
    private void addRow(int id, String status, long totalApplications, long newApplications) {
        rows.add(JobPosting.builder().jobPostingId(id).postingNo(1).postingTitle("Tin " + id).postingStatus(status)
                .postingDate(NOW.minusDays(3)).applicationDeadline(NOW.plusDays(20)).requisition(requisition)
                .build());
        if (totalApplications > 0) {
            counts.add(new Object[] {id, totalApplications, newApplications});
        }
    }

    // ----- Quyền xem -----

    @Test
    @DisplayName("Interviewer (không phải HR / HM / Director) -> 403, không truy vấn")
    void getListPage_shouldDeny_whenRoleCannotRead() {
        User interviewer = userWithRole("Interviewer");

        assertThrows(AccessDeniedException.class, () -> service.getListPage(emptyFilter(), interviewer));
        verify(postings, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    // ----- Bước 1: ô lọc theo vai trò -----

    @Test
    @DisplayName("Director (D4) -> Không có 'Bản nháp', có Phòng ban và Hạn nộp, không quản lý tin")
    void getListPage_shouldHideDraftOption_whenViewerIsDirector() {
        JobPostingListResponse list = service.getListPage(emptyFilter(), userWithRole(RequisitionAccess.ROLE_DIRECTOR));

        assertFalse(list.canManage());
        assertTrue(list.options().showDepartment() && list.options().showDeadline());
        assertFalse(list.options().showCreator());
        assertTrue(list.options().statuses().stream()
                .noneMatch(option -> JobPostingAccess.STATUS_DRAFT.equals(option.value())));
    }

    @Test
    @DisplayName("HM -> Không có ô Phòng ban (chỉ thấy phòng mình), 4 trạng thái")
    void getListPage_shouldHideDepartmentFilter_whenViewerIsHiringManager() {
        JobPostingListResponse list = service.getListPage(emptyFilter(), userWithRole(RequisitionAccess.ROLE_HIRING_MANAGER));

        assertFalse(list.options().showDepartment());
        assertEquals(JobPostingAccess.VIEW_STATUSES_WITHOUT_DRAFT.size(), list.options().statuses().size());
    }

    @Test
    @DisplayName("Admin bị chặn; HR được quản lý tin")
    void getListPage_shouldDenyAdminAndAllowHr() {
        assertThrows(AccessDeniedException.class,
                () -> service.getListPage(emptyFilter(), userWithRole(RequisitionAccess.ROLE_SYSTEM_ADMIN)));
        assertTrue(service.getListPage(emptyFilter(), userWithRole(RequisitionAccess.ROLE_HR)).canManage());
    }

    @Test
    @DisplayName("Ô Hạn nộp chỉ còn 'Còn hạn' / 'Đã quá hạn'; link cũ deadline=soon bị bỏ qua")
    void getListPage_shouldOfferOnlyOpenAndPastDeadlines() {
        // Arrange
        JobPostingSearchRequest raw = new JobPostingSearchRequest(null, null, null, null,
                JobPostingLabels.DEADLINE_SOON, null, null, null, 0, 10);

        // Act
        JobPostingListResponse list = service.getListPage(raw, userWithRole(RequisitionAccess.ROLE_HR));

        // Assert
        List<String> labels = list.options().deadlines().stream().map(FilterOption::label).toList();
        assertEquals(List.of("Còn hạn", "Đã quá hạn"), labels);
        assertNull(list.filter().deadline());
    }

    // ----- Bước 2: làm sạch bộ lọc -----

    @Test
    @DisplayName("Director gửi status=Draft, creatorId, deadline lạ, sort lạ, page âm, size quá lớn -> Bỏ qua / về mặc định")
    void getListPage_shouldDropValuesOutsideRole_whenUrlIsTampered() {
        JobPostingSearchRequest raw = new JobPostingSearchRequest("  java  ", List.of("Draft", "Published"), null, 99,
                "tomorrow", null, null, "hack", -3, 999);

        JobPostingListResponse list = service.getListPage(raw, userWithRole(RequisitionAccess.ROLE_DIRECTOR));

        assertEquals("java", list.filter().q());
        assertEquals(List.of(JobPostingAccess.STATUS_PUBLISHED), list.filter().status());
        assertNull(list.filter().creatorId(), "Director không có ô Người tạo");
        assertNull(list.filter().deadline());
        assertEquals(JobPostingSort.NEWEST.param(), list.filter().sort());
        assertEquals(0, list.filter().page());
        assertEquals(JobPostingListService.MAX_PAGE_SIZE, list.filter().size());
    }

    @Test
    @DisplayName("size = 0 -> Kẹp về 1; thiếu size -> DEFAULT_PAGE_SIZE")
    void getListPage_shouldClampPageSize() {
        User hr = userWithRole(RequisitionAccess.ROLE_HR);
        JobPostingSearchRequest zero = new JobPostingSearchRequest(null, null, null, null, null, null, null, null, 0, 0);

        assertEquals(1, service.getListPage(zero, hr).filter().size());
        assertEquals(JobPostingListService.DEFAULT_PAGE_SIZE, service.getListPage(emptyFilter(), hr).filter().size());
    }

    @Test
    @DisplayName("Trang 5 trong khi chỉ có 1 trang -> Tự lùi về trang cuối (truy vấn lại trang 0)")
    void getListPage_shouldFallBackToLastPage_whenPageIsBeyondTotal() {
        // Arrange
        addRow(11, JobPostingAccess.STATUS_PUBLISHED, 0, 0);
        when(postings.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(1), 1))
                .thenAnswer(invocation -> new PageImpl<>(rows, invocation.getArgument(1), 1));
        JobPostingSearchRequest raw = new JobPostingSearchRequest(null, null, null, null, null, null, null, null, 5, 10);

        // Act
        JobPostingListResponse list = service.getListPage(raw, userWithRole(RequisitionAccess.ROLE_HR));

        // Assert
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postings, times(2)).findAll(any(Specification.class), pageable.capture());
        assertEquals(PageRequest.of(0, 10, JobPostingSort.NEWEST.toSort()), pageable.getAllValues().get(1));
        assertEquals(1, list.page().getContent().size());
    }

    // ----- Bước 3 + 4: truy vấn, chip -----

    @Test
    @DisplayName("Chip: trạng thái, hạn nộp, sắp xếp khác mặc định; 'Nhiều hồ sơ nhất' sắp bằng Specification")
    void getListPage_shouldDescribeChips_andSortByApplicationsInSpecification() {
        JobPostingSearchRequest raw = new JobPostingSearchRequest(null, List.of(JobPostingAccess.STATUS_PUBLISHED), null,
                null, JobPostingLabels.DEADLINE_OPEN, null, null, JobPostingSort.MOST_APPLICATIONS.param(), 0, 10);

        JobPostingListResponse list = service.getListPage(raw, userWithRole(RequisitionAccess.ROLE_HR));

        List<String> labels = list.chips().stream().map(FilterChip::label).toList();
        assertEquals(List.of("Trạng thái: Đang đăng", "Hạn nộp: Còn hạn", "Sắp xếp: Nhiều hồ sơ nhất"), labels);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postings).findAll(any(Specification.class), pageable.capture());
        assertTrue(pageable.getValue().getSort().isUnsorted(), "Sắp theo số hồ sơ nằm trong Specification, không dùng Sort");
    }

    @Test
    @DisplayName("Trang 3 tin -> Đếm hồ sơ đúng 1 câu cho cả trang (chống N+1), số hồ sơ gắn đúng dòng")
    void getListPage_shouldCountApplicationsInOneQuery_whenPageHasManyRows() {
        // Arrange
        addRow(11, JobPostingAccess.STATUS_PUBLISHED, 3, 2);
        addRow(12, JobPostingAccess.STATUS_PAUSED, 0, 0);
        addRow(13, JobPostingAccess.STATUS_DRAFT, 0, 0);

        // Act
        List<InternalJobPostingResponse> page =
                service.getListPage(emptyFilter(), userWithRole(RequisitionAccess.ROLE_HR)).page().getContent();

        // Assert
        verify(postings, times(1)).countApplicationsByPosting(anyCollection(), eq(JobPostingListService.NEW_APPLICATION_STATUS));
        assertEquals(3, page.get(0).applicationCount());
        assertEquals(2, page.get(0).newApplicationCount());
        assertEquals(0, page.get(1).applicationCount());
    }

    @Test
    @DisplayName("Trang rỗng -> Không chạy câu đếm hồ sơ (tránh IN () rỗng)")
    void getListPage_shouldSkipCountQuery_whenPageIsEmpty() {
        service.getListPage(emptyFilter(), userWithRole(RequisitionAccess.ROLE_HR));

        verify(postings, never()).countApplicationsByPosting(anyCollection(), any());
    }
}
