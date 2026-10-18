package com.group2.rms.requisition.service;

import com.group2.rms.core.util.ListFilters;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.dto.RequisitionListResponse;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.dto.RequisitionSearchRequest;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/*
 * UNIT TEST SERVICE: RequisitionListService (màn danh sách yêu cầu tuyển dụng).
 * Kiểm tra bộ lọc theo role, việc "làm sạch" tham số từ URL, phân trang và số câu truy vấn (không N+1).
 * Không cần Spring context hay SQL Server; câu SQL thật được kiểm ở RequisitionIntegrationTests.
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): getListPage_shouldKếtQuả_whenĐiềuKiện, Arrange / Act / Assert.
 */
@ExtendWith(MockitoExtension.class)
class RequisitionListServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock
    private JobRequisitionRepository requisitions;
    @Mock
    private DepartmentRepository departments;
    @Mock
    private JobPostingRepository postings;
    @Mock
    private RequisitionAccess access;
    @Mock
    private RequisitionValidator validator;

    private RequisitionListService service;

    @BeforeEach
    void setUp() {
        RequisitionMapper mapper = new RequisitionMapper(access, validator, CLOCK);
        service = new RequisitionListService(requisitions, departments, postings, access, mapper);
    }

    // ----- Hàm hỗ trợ -----

    /** Giả lập người đang đăng nhập có role cho trước; repository trả về trang rỗng. */
    private User loginAs(String roleName) {
        User user = User.builder().userId(7).username("tester").fullName("Người kiểm thử").build();
        when(access.actor()).thenReturn(user);
        when(access.role(user)).thenReturn(roleName);
        lenient().when(requisitions.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        return user;
    }

    /** Request chỉ có tham số status, các tham số khác để trống. */
    private RequisitionSearchRequest onlyStatus(List<String> status) {
        return new RequisitionSearchRequest(null, status, null, null, null, null, null, null, null, null);
    }

    private RequisitionSearchRequest onlyKeyword(String keyword) {
        return new RequisitionSearchRequest(keyword, null, null, null, null, null, null, null, null, null);
    }

    private JobRequisition row(int id) {
        return JobRequisition.builder().requisitionId(id).title("Vị trí " + id).approvalStatus(RequisitionAccess.STATUS_APPROVED)
                .hiringManager(User.builder().userId(1).fullName("HM").build()).screeningCriteria(new ArrayList<>()).build();
    }

    // ----- Phạm vi dữ liệu theo role -----

    @Test
    @DisplayName("HR mở danh sách -> Câu WHERE luôn có điều kiện chỉ lấy yêu cầu Approved")
    void getListPage_shouldOnlyQueryApproved_whenViewerIsHr() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HR);

        // Act
        service.getListPage(onlyStatus(null));

        // Assert: chạy thử Specification đã gửi xuống repository với Root / CriteriaBuilder giả
        ArgumentCaptor<Specification> where = ArgumentCaptor.forClass(Specification.class);
        verify(requisitions).findAll(where.capture(), any(Pageable.class));
        Root root = mock(Root.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path status = mock(Path.class);
        when(root.get("approvalStatus")).thenReturn(status);
        where.getValue().toPredicate(root, null, builder);
        verify(builder).equal(status, RequisitionAccess.STATUS_APPROVED);
    }

    // ----- Trạng thái duyệt -----

    @Test
    @DisplayName("HR gửi ?status=Draft -> Bị bỏ qua, vì HR không có ô lọc trạng thái")
    void getListPage_shouldIgnoreStatusFilter_whenViewerIsHr() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HR);

        // Act
        RequisitionListResponse list = service.getListPage(onlyStatus(List.of(RequisitionAccess.STATUS_DRAFT)));

        // Assert
        assertFalse(list.options().showStatus());
        assertNull(list.filter().status());
        assertTrue(list.chips().isEmpty());
    }

    @Test
    @DisplayName("Director vào trang (không có tham số status) -> Xem tất cả, không bị lọc sẵn Chờ duyệt")
    void getListPage_shouldShowAllStatuses_whenDirectorOpensPage() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_DIRECTOR);

        // Act
        RequisitionListResponse list = service.getListPage(onlyStatus(null));

        // Assert
        assertTrue(list.filter().status().isEmpty());
        assertTrue(list.chips().isEmpty());
    }

    @Test
    @DisplayName("Director bỏ chip trạng thái (?status= rỗng) -> Xem tất cả, không bị tự lọc lại")
    void getListPage_shouldShowAllStatuses_whenDirectorClearsStatus() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_DIRECTOR);

        // Act
        RequisitionListResponse list = service.getListPage(onlyStatus(List.of("")));

        // Assert
        assertTrue(list.filter().status().isEmpty());
        assertTrue(list.chips().isEmpty());
    }

    @Test
    @DisplayName("Director gửi Draft + Approved -> Bỏ Draft (không được xem), giữ Approved")
    void getListPage_shouldDropDraftStatus_whenViewerIsDirector() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_DIRECTOR);

        // Act
        RequisitionListResponse list = service.getListPage(onlyStatus(
                List.of(RequisitionAccess.STATUS_DRAFT, RequisitionAccess.STATUS_APPROVED)));

        // Assert
        assertEquals(List.of(RequisitionAccess.STATUS_APPROVED), list.filter().status());
        assertTrue(list.options().statuses().stream()
                .noneMatch(option -> option.value().equals(RequisitionAccess.STATUS_DRAFT)));
    }

    @Test
    @DisplayName("Gửi trùng một trạng thái hai lần -> Chỉ giữ một lần, chip hiện một lần")
    void getListPage_shouldRemoveDuplicateStatus_whenSameStatusIsSentTwice() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_SYSTEM_ADMIN);

        // Act
        RequisitionListResponse list = service.getListPage(onlyStatus(
                List.of(RequisitionAccess.STATUS_APPROVED, RequisitionAccess.STATUS_APPROVED)));

        // Assert
        assertEquals(List.of(RequisitionAccess.STATUS_APPROVED), list.filter().status());
        assertEquals("Trạng thái: Đã duyệt", list.chips().get(0).label());
    }

    // ----- Ô lọc theo role -----

    @Test
    @DisplayName("Hiring Manager -> Không có ô Phòng ban / Người tạo; departmentId, creatorId trên URL bị bỏ qua")
    void getListPage_shouldHideDepartmentAndCreator_whenViewerIsHiringManager() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);
        RequisitionSearchRequest raw = new RequisitionSearchRequest(null, null, null, 1, 5, null, null, null, null, null);

        // Act
        RequisitionListResponse list = service.getListPage(raw);

        // Assert
        assertFalse(list.options().showDepartment());
        assertFalse(list.options().showCreator());
        assertTrue(list.options().showPosting());
        assertTrue(list.options().showCreatedDate());
        assertNull(list.filter().departmentId());
        assertNull(list.filter().creatorId());
    }

    @Test
    @DisplayName("Phòng ban không có trong danh sách lựa chọn (id giả) -> Bỏ qua; phòng ban hợp lệ được giữ")
    void getListPage_shouldIgnoreUnknownDepartment_whenViewerIsAdmin() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_SYSTEM_ADMIN);
        Department engineering = Department.builder().departmentId(1).departmentName("Engineering").build();
        when(departments.findAll(any(Sort.class))).thenReturn(List.of(engineering));

        // Act
        RequisitionListResponse unknown = service.getListPage(
                new RequisitionSearchRequest(null, null, null, 99, null, null, null, null, null, null));
        RequisitionListResponse known = service.getListPage(
                new RequisitionSearchRequest(null, null, null, 1, null, null, null, null, null, null));

        // Assert
        assertNull(unknown.filter().departmentId());
        assertEquals(1, known.filter().departmentId());
        assertEquals("Phòng ban: Engineering", known.chips().get(0).label());
    }

    @Test
    @DisplayName("Ô lọc Tin tuyển dụng -> Chỉ nhận none / Published / Paused / Closed, giá trị lạ bị bỏ")
    void getListPage_shouldIgnoreUnknownPostingValue_whenViewerIsHr() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HR);

        // Act
        RequisitionListResponse unknown = service.getListPage(
                new RequisitionSearchRequest(null, null, "Deleted", null, null, null, null, null, null, null));
        RequisitionListResponse none = service.getListPage(
                new RequisitionSearchRequest(null, null, "none", null, null, null, null, null, null, null));

        // Assert
        assertNull(unknown.filter().posting());
        assertEquals(RequisitionLabels.POSTING_NONE, none.filter().posting());
        assertEquals("Tin tuyển dụng: Chưa có tin", none.chips().get(0).label());
    }

    // ----- Từ khóa -----

    @Test
    @DisplayName("Từ khóa chỉ có khoảng trắng -> Coi như không tìm (chuỗi rỗng)")
    void getListPage_shouldTreatKeywordAsEmpty_whenKeywordIsSpaceOnly() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);

        // Act
        RequisitionListResponse list = service.getListPage(onlyKeyword("     "));

        // Assert
        assertEquals("", list.filter().q());
    }

    @Test
    @DisplayName("Từ khóa đúng giới hạn ký tự -> Giữ nguyên; dài hơn 1 ký tự -> Cắt về đúng giới hạn")
    void getListPage_shouldCutKeyword_whenKeywordExceedsMaxLength() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);
        String atLimit = "a".repeat(ListFilters.MAX_KEYWORD_LENGTH);

        // Act
        RequisitionListResponse kept = service.getListPage(onlyKeyword(atLimit));
        RequisitionListResponse cut = service.getListPage(onlyKeyword(atLimit + "b"));

        // Assert
        assertEquals(atLimit, kept.filter().q());
        assertEquals(atLimit, cut.filter().q());
    }

    // ----- Sắp xếp và phân trang -----

    @Test
    @DisplayName("Sort lạ, page âm, size quá lớn -> Về mặc định an toàn: mới nhất, trang đầu, tối đa 50 dòng")
    void getListPage_shouldFallBackToSafeDefaults_whenParamsAreInvalid() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);
        RequisitionSearchRequest raw = new RequisitionSearchRequest(
                "  java  ", null, null, null, null, null, null, "password", -3, 999);

        // Act
        RequisitionListResponse list = service.getListPage(raw);

        // Assert
        assertEquals("java", list.filter().q());
        assertEquals(RequisitionSort.NEWEST.param(), list.filter().sort());
        assertEquals(0, list.filter().page());
        assertEquals(50, list.filter().size());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(requisitions).findAll(any(Specification.class), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(50, pageable.getValue().getPageSize());
        assertEquals(RequisitionSort.NEWEST.toSort(), pageable.getValue().getSort());
    }

    @Test
    @DisplayName("Size = 0 (biên dưới) -> Tối thiểu 1 dòng mỗi trang")
    void getListPage_shouldUseOneRowPerPage_whenSizeIsZero() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);

        // Act
        RequisitionListResponse list = service.getListPage(
                new RequisitionSearchRequest(null, null, null, null, null, null, null, null, null, 0));

        // Assert
        assertEquals(1, list.filter().size());
    }

    @Test
    @DisplayName("Sắp xếp 'start_date' -> Dùng cột expectedStartDate tăng dần")
    void getListPage_shouldSortByExpectedStartDate_whenSortIsStartDate() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);

        // Act
        service.getListPage(new RequisitionSearchRequest(null, null, null, null, null, null, null, "start_date", null, null));

        // Assert
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(requisitions).findAll(any(Specification.class), pageable.capture());
        Sort.Order firstOrder = pageable.getValue().getSort().iterator().next();
        assertEquals("expectedStartDate", firstOrder.getProperty());
        assertTrue(firstOrder.isAscending());
    }

    @Test
    @DisplayName("Gõ số trang vượt quá tổng số trang -> Tự lùi về trang cuối thay vì bảng trống")
    void getListPage_shouldFallBackToLastPage_whenPageIsBeyondTotal() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);
        Page<JobRequisition> beyond = new PageImpl<>(List.of(), PageRequest.of(9, 10), 15);
        Page<JobRequisition> lastPage = new PageImpl<>(List.of(), PageRequest.of(1, 10), 15);
        when(requisitions.findAll(any(Specification.class), any(Pageable.class))).thenReturn(beyond, lastPage);

        // Act
        service.getListPage(new RequisitionSearchRequest(null, null, null, null, null, null, null, null, 9, null));

        // Assert
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(requisitions, times(2)).findAll(any(Specification.class), pageable.capture());
        assertEquals(1, pageable.getAllValues().get(1).getPageNumber());
    }

    // ----- Số câu truy vấn (không N+1) -----

    @Test
    @DisplayName("Trang có 3 dòng -> Tin mới nhất và 'đã có đợt sau' tra 1 câu cho cả trang, không tra từng dòng")
    void getListPage_shouldLoadPostingsInOneQuery_whenPageHasRows() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HR);
        JobRequisition first = row(1);
        JobRequisition second = row(2);
        JobRequisition third = row(3);
        when(requisitions.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second, third)));
        JobPosting postingOfSecond = JobPosting.builder().jobPostingId(77).requisition(second)
                .postingStatus(RequisitionLabels.POSTING_PUBLISHED).build();
        when(postings.findLatestByRequisitionIds(List.of(1, 2, 3))).thenReturn(List.of(postingOfSecond));
        when(requisitions.findIdsWithLaterRounds(List.of(1, 2, 3))).thenReturn(List.of(3));

        // Act
        List<RequisitionResponse> rows = service.getListPage(onlyStatus(null)).page().getContent();

        // Assert: mỗi loại dữ liệu đúng 1 câu, không dùng các hàm tra theo từng id
        verify(postings, times(1)).findLatestByRequisitionIds(anyCollection());
        verify(requisitions, times(1)).findIdsWithLaterRounds(anyCollection());
        verify(postings, never()).findTopByRequisition_RequisitionIdOrderByJobPostingIdDesc(any());
        verify(postings, never()).existsByRequisition_RequisitionId(any());
        verify(requisitions, never()).existsByOriginalRequisition_RequisitionId(any());

        assertNull(rows.get(0).getJobPostingId());
        assertEquals(77, rows.get(1).getJobPostingId());
        assertEquals("Đang đăng", rows.get(1).getPostingLabel());
    }

    @Test
    @DisplayName("Trang rỗng -> Không chạy câu tra tin tuyển dụng (tránh IN () rỗng)")
    void getListPage_shouldSkipBatchQueries_whenPageIsEmpty() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HR);

        // Act
        service.getListPage(onlyStatus(null));

        // Assert
        verifyNoInteractions(postings);
        verify(requisitions, never()).findIdsWithLaterRounds(anyCollection());
    }
}
