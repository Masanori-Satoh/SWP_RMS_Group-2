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
import org.springframework.security.access.AccessDeniedException;

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
        return new RequisitionSearchRequest(null, status, null, null, null, null, null, null, null, null, null);
    }

    private RequisitionSearchRequest onlyKeyword(String keyword) {
        return new RequisitionSearchRequest(keyword, null, null, null, null, null, null, null, null, null, null);
    }

    private JobRequisition row(int id) {
        return JobRequisition.builder().requisitionId(id).title("Vị trí " + id).approvalStatus(RequisitionAccess.STATUS_APPROVED)
                .hiringManager(User.builder().userId(1).fullName("HM").build()).build();
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
    @DisplayName("HR gửi ?status=Draft -> Báo lỗi, bảng trống, vì HR không có ô lọc trạng thái")
    void getListPage_shouldRejectStatusFilter_whenViewerIsHr() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HR);

        // Act
        RequisitionListResponse list = service.getListPage(onlyStatus(List.of(RequisitionAccess.STATUS_DRAFT)));

        // Assert
        assertFalse(list.options().showStatus());
        assertNull(list.filter().status());
        assertTrue(list.chips().isEmpty());
        assertTrue(list.page().isEmpty());
        assertEquals(List.of("Bộ lọc trạng thái không hợp lệ. Vui lòng chọn bộ lọc hợp lệ."), list.errors());
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
    @DisplayName("Director chọn Bản nháp -> Được giữ (lọc được bản nháp của chính mình)")
    void getListPage_shouldKeepDraftStatus_whenViewerIsDirector() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_DIRECTOR);

        // Act
        RequisitionListResponse list = service.getListPage(onlyStatus(List.of(RequisitionAccess.STATUS_DRAFT)));

        // Assert
        assertEquals(List.of(RequisitionAccess.STATUS_DRAFT), list.filter().status());
        assertEquals("Trạng thái: Bản nháp", list.chips().get(0).label());
        assertTrue(list.options().statuses().stream()
                .anyMatch(option -> option.value().equals(RequisitionAccess.STATUS_DRAFT)));
    }

    @Test
    @DisplayName("Director mở danh sách -> WHERE: (không phải Draft) HOẶC (người tạo là chính mình)")
    void getListPage_shouldQueryNonDraftOrOwnDraft_whenViewerIsDirector() {
        // Arrange
        User director = loginAs(RequisitionAccess.ROLE_DIRECTOR);

        // Act
        service.getListPage(onlyStatus(null));

        // Assert: chạy thử Specification với Root / CriteriaBuilder giả
        ArgumentCaptor<Specification> where = ArgumentCaptor.forClass(Specification.class);
        verify(requisitions).findAll(where.capture(), any(Pageable.class));
        Root root = mock(Root.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path status = mock(Path.class);
        Path creator = mock(Path.class);
        Path creatorId = mock(Path.class);
        when(root.get("approvalStatus")).thenReturn(status);
        when(root.get("hiringManager")).thenReturn(creator);
        when(creator.get("userId")).thenReturn(creatorId);
        where.getValue().toPredicate(root, null, builder);
        verify(builder).notEqual(status, RequisitionAccess.STATUS_DRAFT);
        verify(builder).equal(creatorId, director.getUserId());
        verify(builder).or(any(), any());
    }

    @Test
    @DisplayName("Gửi trùng một trạng thái hai lần -> Chỉ giữ một lần, chip hiện một lần")
    void getListPage_shouldRemoveDuplicateStatus_whenSameStatusIsSentTwice() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_DIRECTOR);

        // Act
        RequisitionListResponse list = service.getListPage(onlyStatus(
                List.of(RequisitionAccess.STATUS_APPROVED, RequisitionAccess.STATUS_APPROVED)));

        // Assert
        assertEquals(List.of(RequisitionAccess.STATUS_APPROVED), list.filter().status());
        assertEquals("Trạng thái: Đã duyệt", list.chips().get(0).label());
    }

    // ----- Ô lọc theo role -----

    @Test
    void getListPage_shouldDenySystemAdmin() {
        loginAs(RequisitionAccess.ROLE_SYSTEM_ADMIN);
        assertThrows(AccessDeniedException.class, () -> service.getListPage(onlyStatus(List.of())));
    }

    @Test
    @DisplayName("Hiring Manager -> Không có ô Phòng ban / Người tạo; departmentId, creatorId trên URL báo lỗi")
    void getListPage_shouldHideDepartmentAndCreator_whenViewerIsHiringManager() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);
        RequisitionSearchRequest raw = new RequisitionSearchRequest(null, null, null, 1, 5, null, null, null, null, null, null);

        // Act
        RequisitionListResponse list = service.getListPage(raw);

        // Assert
        assertFalse(list.options().showDepartment());
        assertFalse(list.options().showCreator());
        assertTrue(list.options().showPosting());
        assertFalse(list.options().showCreatedDate());
        assertTrue(list.options().showEmploymentType());
        assertNull(list.filter().departmentId());
        assertNull(list.filter().creatorId());
        assertTrue(list.page().isEmpty());
        assertEquals(List.of("Bộ lọc phòng ban không hợp lệ. Vui lòng chọn bộ lọc hợp lệ.",
                "Bộ lọc người tạo yêu cầu không hợp lệ. Vui lòng chọn bộ lọc hợp lệ."), list.errors());
    }

    @Test
    @DisplayName("Phòng ban không có trong danh sách lựa chọn -> Báo lỗi, bảng trống; phòng ban hợp lệ được giữ")
    void getListPage_shouldRejectUnknownDepartment_whenViewerIsDirector() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_DIRECTOR);
        Department engineering = Department.builder().departmentId(1).departmentName("Engineering").build();
        when(departments.findAll(any(Sort.class))).thenReturn(List.of(engineering));

        // Act
        RequisitionListResponse unknown = service.getListPage(
                new RequisitionSearchRequest(null, null, null, 99, null, null, null, null, null, null, null));
        RequisitionListResponse known = service.getListPage(
                new RequisitionSearchRequest(null, null, null, 1, null, null, null, null, null, null, null));

        // Assert
        assertNull(unknown.filter().departmentId());
        assertTrue(unknown.page().isEmpty());
        assertEquals(List.of("Bộ lọc phòng ban không hợp lệ. Vui lòng chọn bộ lọc hợp lệ."), unknown.errors());
        assertTrue(known.errors().isEmpty());
        assertEquals(1, known.filter().departmentId());
        assertEquals("Phòng ban: Engineering", known.chips().get(0).label());
    }

    @Test
    @DisplayName("Ô lọc Tin tuyển dụng -> Chỉ nhận none / Published / Paused / Closed, giá trị lạ báo lỗi, bảng trống")
    void getListPage_shouldIgnoreUnknownPostingValue_whenViewerIsHr() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HR);

        // Act
        RequisitionListResponse unknown = service.getListPage(
                new RequisitionSearchRequest(null, null, "Deleted", null, null, null, null, null, null, null, null));
        RequisitionListResponse none = service.getListPage(
                new RequisitionSearchRequest(null, null, "none", null, null, null, null, null, null, null, null));

        // Assert
        assertNull(unknown.filter().posting());
        assertTrue(unknown.page().isEmpty());
        assertEquals(List.of("Bộ lọc tin tuyển dụng không hợp lệ. Vui lòng chọn bộ lọc hợp lệ."), unknown.errors());
        assertTrue(none.errors().isEmpty());
        assertEquals(RequisitionLabels.POSTING_NONE, none.filter().posting());
        assertEquals("Tin tuyển dụng: Chưa có tin", none.chips().get(0).label());
    }

    @Test
    @DisplayName("Ô lọc Hình thức -> HM chọn Part-time được giữ + có chip; giá trị lạ báo lỗi; Director không có ô này")
    void getListPage_shouldFilterByEmploymentType_onlyForHiringManager() {
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);
        RequisitionListResponse partTime = service.getListPage(
                new RequisitionSearchRequest(null, null, null, null, null, null, null, null, null, null, "Part-time"));
        RequisitionListResponse unknown = service.getListPage(
                new RequisitionSearchRequest(null, null, null, null, null, null, null, null, null, null, "Freelance"));

        assertTrue(partTime.errors().isEmpty());
        assertEquals("Part-time", partTime.filter().employmentType());
        assertEquals("Hình thức làm việc: Bán thời gian", partTime.chips().get(0).label());
        assertNull(unknown.filter().employmentType());
        assertEquals(List.of("Bộ lọc hình thức làm việc không hợp lệ. Vui lòng chọn bộ lọc hợp lệ."), unknown.errors());

        loginAs(RequisitionAccess.ROLE_DIRECTOR);
        RequisitionListResponse director = service.getListPage(
                new RequisitionSearchRequest(null, null, null, null, null, null, null, null, null, null, "Part-time"));
        assertFalse(director.options().showEmploymentType());
        assertNull(director.filter().employmentType());
    }

    @Test
    @DisplayName("departmentId=ooooooom sai kiểu -> Báo lỗi phòng ban, bảng trống")
    void getListPage_shouldReportError_whenDepartmentIdIsNotANumber() {
        // Arrange
        loginAs(RequisitionAccess.ROLE_HR);

        // Act
        RequisitionListResponse list = service.getListPage(
                new RequisitionSearchRequest(null, null, null, null, null, null, null, null, null, null, null),
                List.of("departmentId"));

        // Assert
        assertTrue(list.page().isEmpty());
        assertEquals(List.of("Bộ lọc phòng ban không hợp lệ. Vui lòng chọn bộ lọc hợp lệ."), list.errors());
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
    @DisplayName("Sort lạ -> Bảng trống, báo lỗi sắp xếp, không truy vấn")
    void getListPage_shouldReportError_whenSortIsUnknown() {
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);

        RequisitionListResponse list = service.getListPage(
                new RequisitionSearchRequest(null, null, null, null, null, null, null, "abc' OR 1=1", null, null, null));

        assertTrue(list.page().isEmpty());
        assertEquals(List.of("Giá trị sắp xếp không hợp lệ. Vui lòng chọn tùy chọn sắp xếp hợp lệ."), list.errors());
        verify(requisitions, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Page âm + size quá lớn -> Hiện cả hai lỗi, bảng trống")
    void getListPage_shouldReportBothErrors_whenPageNegativeAndSizeTooLarge() {
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);

        RequisitionListResponse list = service.getListPage(
                new RequisitionSearchRequest(null, null, null, null, null, null, null, null, -3, 999, null));

        assertTrue(list.page().isEmpty());
        assertEquals(List.of("Số trang phải lớn hơn hoặc bằng 1.", "Số dòng mỗi trang không được vượt quá 50."),
                list.errors());
    }

    @Test
    @DisplayName("Size = 0 -> Báo lỗi tối thiểu 1 dòng, bảng trống")
    void getListPage_shouldReportError_whenSizeIsZero() {
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);

        RequisitionListResponse list = service.getListPage(
                new RequisitionSearchRequest(null, null, null, null, null, null, null, null, null, 0, null));

        assertTrue(list.page().isEmpty());
        assertEquals(List.of("Số dòng mỗi trang phải lớn hơn hoặc bằng 1."), list.errors());
    }

    @Test
    @DisplayName("Size = 50 (biên trên) -> Hợp lệ, không lỗi")
    void getListPage_shouldAcceptMaxSize() {
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);

        RequisitionListResponse list = service.getListPage(
                new RequisitionSearchRequest(null, null, null, null, null, null, null, null, null, 50, null));

        assertTrue(list.errors().isEmpty());
        assertEquals(50, list.filter().size());
    }

    @Test
    @DisplayName("Số trang vượt quá tổng số trang -> Bảng trống kèm lỗi, không lùi về trang cuối")
    void getListPage_shouldReportError_whenPageIsBeyondTotal() {
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);
        Page<JobRequisition> beyond = new PageImpl<>(List.of(), PageRequest.of(998, 10), 15);
        when(requisitions.findAll(any(Specification.class), any(Pageable.class))).thenReturn(beyond);

        RequisitionListResponse list = service.getListPage(
                new RequisitionSearchRequest(null, null, null, null, null, null, null, null, 998, null, null));

        assertTrue(list.page().isEmpty());
        assertEquals(0, list.page().getTotalElements());
        assertEquals(List.of("Trang bạn yêu cầu không tồn tại."), list.errors());
        verify(requisitions, times(1)).findAll(any(Specification.class), any(Pageable.class));
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

        assertNull(rows.get(0).jobPostingId());
        assertEquals(77, rows.get(1).jobPostingId());
        assertEquals("Đang đăng", rows.get(1).postingLabel());
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
