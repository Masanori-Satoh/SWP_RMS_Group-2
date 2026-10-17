package com.group2.rms.requisition.service;

import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.requisition.dto.RequisitionListResponse;
import com.group2.rms.requisition.dto.RequisitionSearchRequest;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.repository.RequisitionApprovalRepository;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
 * Màn danh sách yêu cầu tuyển dụng: bộ lọc theo role và việc "làm sạch" tham số từ URL.
 * Unit test thuần (Mockito), không cần Spring context hay SQL Server.
 * Repository luôn trả trang rỗng: test chỉ kiểm tra bộ lọc thật sự được áp dụng (list.filter())
 * và các ô lọc mỗi role được thấy (list.options()), không kiểm tra từng dòng dữ liệu.
 */
@ExtendWith(MockitoExtension.class)
class RequisitionListFilterTests {

    @Mock private JobRequisitionRepository requisitions;
    @Mock private DepartmentRepository departments;
    @Mock private RequisitionApprovalRepository approvals;
    @Mock private JobPostingRepository postings;
    @Mock private AuditLogRepository audit;
    @Mock private RequisitionAccess access;
    @Mock private RequisitionValidator validator;
    @Mock private NotificationService notificationService;

    @InjectMocks private RequisitionServiceImpl service;

    // ----- Hàm hỗ trợ cho các test bên dưới -----

    /** Giả lập người đang đăng nhập có role cho trước. */
    private void loginAs(String roleName) {
        User user = User.builder().userId(7).username("tester").fullName("Người kiểm thử").build();
        when(access.actor()).thenReturn(user);
        when(access.role(user)).thenReturn(roleName);
        when(requisitions.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
    }

    /** Tạo request chỉ có tham số status, các tham số khác để trống. */
    private RequisitionSearchRequest onlyStatus(List<String> status) {
        return new RequisitionSearchRequest(null, status, null, null, null, null, null, null, null, null);
    }

    // ----- Trạng thái duyệt -----

    @Test
    @DisplayName("HR gửi ?status=Draft thì bị bỏ qua, vì HR không có ô lọc trạng thái")
    void getListPage_shouldIgnoreStatusFilter_whenViewerIsHr() {
        loginAs(RequisitionAccess.ROLE_HR);

        RequisitionListResponse list = service.getListPage(onlyStatus(List.of(RequisitionAccess.STATUS_DRAFT)));

        assertFalse(list.options().showStatus());
        assertNull(list.filter().status());
        assertTrue(list.chips().isEmpty());
    }

    @Test
    @DisplayName("Director vào trang lần đầu (không có tham số status) thì tự lọc Chờ duyệt")
    void getListPage_shouldDefaultToPending_whenDirectorOpensPageFirstTime() {
        loginAs(RequisitionAccess.ROLE_DIRECTOR);

        RequisitionListResponse list = service.getListPage(onlyStatus(null));

        assertEquals(List.of(RequisitionAccess.STATUS_PENDING_DIRECTOR), list.filter().status());
        assertEquals(1, list.chips().size());
        assertEquals("Trạng thái: Chờ duyệt", list.chips().get(0).label());
    }

    @Test
    @DisplayName("Director bỏ chip trạng thái (?status=) thì xem tất cả, không bị tự lọc lại")
    void getListPage_shouldShowAllStatuses_whenDirectorClearsStatus() {
        loginAs(RequisitionAccess.ROLE_DIRECTOR);

        RequisitionListResponse list = service.getListPage(onlyStatus(List.of("")));

        assertTrue(list.filter().status().isEmpty());
        assertTrue(list.chips().isEmpty());
    }

    @Test
    @DisplayName("Director không lọc được Bản nháp: giá trị Draft bị bỏ, giá trị hợp lệ được giữ")
    void getListPage_shouldDropDraftStatus_whenViewerIsDirector() {
        loginAs(RequisitionAccess.ROLE_DIRECTOR);

        RequisitionListResponse list = service.getListPage(onlyStatus(
                List.of(RequisitionAccess.STATUS_DRAFT, RequisitionAccess.STATUS_APPROVED)));

        assertEquals(List.of(RequisitionAccess.STATUS_APPROVED), list.filter().status());
        assertTrue(list.options().statuses().stream()
                .noneMatch(option -> option.value().equals(RequisitionAccess.STATUS_DRAFT)));
    }

    // ----- Ô lọc theo role -----

    @Test
    @DisplayName("Hiring Manager không có ô Phòng ban / Người tạo; departmentId trên URL bị bỏ qua")
    void getListPage_shouldHideDepartmentAndCreator_whenViewerIsHiringManager() {
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);
        RequisitionSearchRequest raw = new RequisitionSearchRequest(
                null, null, null, 1, 5, null, null, null, null, null);

        RequisitionListResponse list = service.getListPage(raw);

        assertFalse(list.options().showDepartment());
        assertFalse(list.options().showCreator());
        assertTrue(list.options().showPosting());
        assertTrue(list.options().showCreatedDate());
        assertNull(list.filter().departmentId());
        assertNull(list.filter().creatorId());
    }

    @Test
    @DisplayName("Phòng ban không có trong danh sách lựa chọn thì bị bỏ qua, phòng ban hợp lệ được giữ")
    void getListPage_shouldIgnoreUnknownDepartment_whenViewerIsAdmin() {
        loginAs(RequisitionAccess.ROLE_SYSTEM_ADMIN);
        Department engineering = Department.builder().departmentId(1).departmentName("Engineering").build();
        when(departments.findAll(any(Sort.class))).thenReturn(List.of(engineering));

        RequisitionListResponse unknown = service.getListPage(
                new RequisitionSearchRequest(null, null, null, 99, null, null, null, null, null, null));
        RequisitionListResponse known = service.getListPage(
                new RequisitionSearchRequest(null, null, null, 1, null, null, null, null, null, null));

        assertNull(unknown.filter().departmentId());
        assertEquals(1, known.filter().departmentId());
        assertEquals("Phòng ban: Engineering", known.chips().get(0).label());
    }

    @Test
    @DisplayName("Ô lọc Tin tuyển dụng chỉ nhận các giá trị đã định (none, Published, Paused, Closed)")
    void getListPage_shouldIgnoreUnknownPostingValue_whenViewerIsHr() {
        loginAs(RequisitionAccess.ROLE_HR);

        RequisitionListResponse unknown = service.getListPage(
                new RequisitionSearchRequest(null, null, "Deleted", null, null, null, null, null, null, null));
        RequisitionListResponse none = service.getListPage(
                new RequisitionSearchRequest(null, null, "none", null, null, null, null, null, null, null));

        assertNull(unknown.filter().posting());
        assertEquals(RequisitionLabels.POSTING_NONE, none.filter().posting());
        assertEquals("Tin tuyển dụng: Chưa có tin", none.chips().get(0).label());
    }

    // ----- Sắp xếp và phân trang -----

    @Test
    @DisplayName("Sort lạ, page âm, size quá lớn thì về mặc định: mới nhất, trang đầu, tối đa 50 dòng")
    void getListPage_shouldFallBackToSafeDefaults_whenParamsAreInvalid() {
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);
        RequisitionSearchRequest raw = new RequisitionSearchRequest(
                "  java  ", null, null, null, null, null, null, "password", -3, 999);

        RequisitionListResponse list = service.getListPage(raw);

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
    @DisplayName("Sắp xếp theo ngày bắt đầu gần nhất dùng đúng cột expectedStartDate tăng dần")
    void getListPage_shouldSortByExpectedStartDate_whenSortIsStartDate() {
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);

        service.getListPage(new RequisitionSearchRequest(
                null, null, null, null, null, null, null, "start_date", null, null));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(requisitions).findAll(any(Specification.class), pageable.capture());
        Sort.Order firstOrder = pageable.getValue().getSort().iterator().next();
        assertEquals("expectedStartDate", firstOrder.getProperty());
        assertTrue(firstOrder.isAscending());
    }
}
