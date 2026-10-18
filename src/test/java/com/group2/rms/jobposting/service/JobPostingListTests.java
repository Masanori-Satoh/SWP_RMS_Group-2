package com.group2.rms.jobposting.service;

import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.service.TalentPoolService;
import com.group2.rms.jobposting.dto.InternalJobPostingResponse;
import com.group2.rms.jobposting.dto.JobPostingListResponse;
import com.group2.rms.jobposting.dto.JobPostingSearchRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.jobposting.validator.JobPostingValidator;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
 * Màn danh sách tin tuyển dụng (JobPostingServiceImpl#getListPage).
 * Unit test thuần Mockito: kiểm tra ô lọc theo vai trò, việc làm sạch tham số URL,
 * nhãn / số ngày / cờ thao tác của từng dòng và số hồ sơ đếm gộp.
 * Điều kiện SQL (Specification) được kiểm tra thật ở integration test.
 */
@ExtendWith(MockitoExtension.class)
class JobPostingListTests {

    @Mock private JobPostingRepository postings;
    @Mock private JobRequisitionRepository requisitions;
    @Mock private AuditLogRepository audit;
    @Mock private RequisitionAccess access;
    @Mock private NotificationService notificationService;
    @Mock private TalentPoolService talentPoolService;
    @Mock private ApplicationRepository applications;
    @Mock private DepartmentRepository departments;
    @Spy private JobPostingValidator validator = new JobPostingValidator();

    @InjectMocks private JobPostingServiceImpl service;

    private User viewer;
    private JobRequisition requisition;
    private final List<JobPosting> rows = new ArrayList<>();
    private final List<Object[]> counts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        viewer = User.builder().userId(7).username("viewer").fullName("Người xem").build();
        Department it = Department.builder().departmentId(1).departmentName("Engineering").build();
        requisition = JobRequisition.builder().requisitionId(5).requisitionCode("REQ-0005").title("Java Dev")
                .department(it).employmentType("Full-time").recruitmentRound(1).build();

        lenient().when(postings.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(rows));
        lenient().when(postings.countApplicationsByPosting(any())).thenReturn(counts);
        lenient().when(departments.findAll(any(Sort.class))).thenReturn(List.of(it));
        lenient().when(postings.findRequisitionIdsWithPostingStatusIn(any())).thenReturn(Set.of());
        lenient().when(requisitions.findByApprovalStatusOrderByCreatedAtDesc(RequisitionAccess.STATUS_APPROVED))
                .thenReturn(List.of(requisition));
    }

    // ----- Hàm hỗ trợ -----

    private void loginAs(String role) {
        when(access.role(viewer)).thenReturn(role);
    }

    private static JobPostingSearchRequest emptyFilter() {
        return new JobPostingSearchRequest(null, null, null, null, null, null, null, null, null, null);
    }

    /** Thêm một tin vào kết quả truy vấn giả, kèm số hồ sơ [tổng, mới]. */
    private JobPosting addRow(int id, String status, LocalDateTime postingDate, LocalDateTime deadline,
            long totalApplications, long newApplications) {
        JobPosting posting = JobPosting.builder().jobPostingId(id).postingNo(1).postingTitle("Tin " + id)
                .postingStatus(status).postingDate(postingDate).applicationDeadline(deadline)
                .requisition(requisition).createdBy(viewer).build();
        rows.add(posting);
        if (totalApplications > 0) {
            counts.add(new Object[] {id, totalApplications, newApplications});
        }
        return posting;
    }

    private InternalJobPostingResponse firstRow(JobPostingListResponse list) {
        return list.page().getContent().get(0);
    }

    // ----- Quyền xem -----

    @Test
    @DisplayName("Vai trò không phải HR / Admin / HM / Director (ví dụ Interviewer) -> 403, không truy vấn")
    void otherRole_isDenied() {
        loginAs("Interviewer");

        assertThrows(AccessDeniedException.class, () -> service.getListPage(emptyFilter(), viewer));
        verify(postings, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Director được xem (D4): ô trạng thái không có Bản nháp, có Phòng ban và Hạn nộp, không có nút thao tác")
    void director_canView_withoutDraftOption() {
        loginAs(RequisitionAccess.ROLE_DIRECTOR);

        JobPostingListResponse list = service.getListPage(emptyFilter(), viewer);

        assertFalse(list.canManage());
        assertTrue(list.options().showDepartment());
        assertTrue(list.options().showDeadline());
        assertFalse(list.options().showCreator());
        assertTrue(list.options().statuses().stream().noneMatch(o -> JobPostingLabels.STATUS_DRAFT.equals(o.value())));
    }

    @Test
    @DisplayName("HM: không có ô Phòng ban (chỉ thấy phòng mình), không có Bản nháp")
    void hiringManager_filterOptions() {
        loginAs(RequisitionAccess.ROLE_HIRING_MANAGER);

        JobPostingListResponse list = service.getListPage(emptyFilter(), viewer);

        assertFalse(list.options().showDepartment());
        assertTrue(list.options().showDeadline());
        assertEquals(4, list.options().statuses().size());
    }

    @Test
    @DisplayName("Admin: có Người tạo và Ngày đăng, không có Hạn nộp; HR được quản lý tin")
    void admin_andHr_options() {
        loginAs(RequisitionAccess.ROLE_SYSTEM_ADMIN);
        JobPostingListResponse adminList = service.getListPage(emptyFilter(), viewer);
        assertTrue(adminList.options().showCreator());
        assertTrue(adminList.options().showPostedDate());
        assertFalse(adminList.options().showDeadline());
        assertEquals(5, adminList.options().statuses().size());

        when(access.role(viewer)).thenReturn(RequisitionAccess.ROLE_HR);
        JobPostingListResponse hrList = service.getListPage(emptyFilter(), viewer);
        assertTrue(hrList.canManage());
    }

    // ----- Làm sạch bộ lọc -----

    @Test
    @DisplayName("Director gửi status=Draft, sort lạ, page âm, deadline lạ -> bị bỏ qua / về mặc định")
    void normalizeFilter_dropsValuesOutsideRole() {
        loginAs(RequisitionAccess.ROLE_DIRECTOR);
        var raw = new JobPostingSearchRequest("  java  ", List.of("Draft", "Published"), null, 99, "tomorrow",
                null, null, "hack", -3, 999);

        JobPostingListResponse list = service.getListPage(raw, viewer);

        assertEquals("java", list.filter().q());
        assertEquals(List.of("Published"), list.filter().status());
        assertNull(list.filter().creatorId(), "Director không có ô Người tạo");
        assertNull(list.filter().deadline());
        assertEquals("newest", list.filter().sort());
        assertEquals(0, list.filter().page());
        assertEquals(50, list.filter().size());
    }

    @Test
    @DisplayName("Chip: trạng thái, hạn nộp, sắp xếp khác mặc định; 'Nhiều hồ sơ nhất' sắp bằng Specification")
    void chips_andMostApplicationsSort() {
        loginAs(RequisitionAccess.ROLE_HR);
        var raw = new JobPostingSearchRequest(null, List.of("Scheduled"), null, null, "soon", null, null, "apps", 0, 10);

        JobPostingListResponse list = service.getListPage(raw, viewer);

        List<String> labels = list.chips().stream().map(c -> c.label()).toList();
        assertEquals(List.of("Trạng thái: Hẹn đăng", "Hạn nộp: Sắp hết hạn (≤ 7 ngày)", "Sắp xếp: Nhiều hồ sơ nhất"), labels);
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postings).findAll(any(Specification.class), pageable.capture());
        assertTrue(pageable.getValue().getSort().isUnsorted(), "Sắp theo số hồ sơ nằm trong Specification, không dùng Sort");
    }

    // ----- Một dòng của bảng -----

    @Test
    @DisplayName("Tin đang đăng có 3 hồ sơ (2 mới): HR được tạm dừng / đóng / sửa, không xóa được")
    void publishedRow_forHr() {
        loginAs(RequisitionAccess.ROLE_HR);
        LocalDateTime now = LocalDateTime.now();
        addRow(11, "Published", now.minusDays(3), now.plusDays(20), 3, 2);

        InternalJobPostingResponse row = firstRow(service.getListPage(emptyFilter(), viewer));

        assertEquals("Published", row.getViewStatus());
        assertEquals("Đang đăng", row.getStatusLabel());
        assertEquals("success", row.getStatusTone());
        assertEquals(3, row.getApplicationCount());
        assertEquals(2, row.getNewApplicationCount());
        assertEquals(20, row.getDaysUntilDeadline());
        assertEquals(JobPostingLabels.DEADLINE_OPEN, row.getDeadlineState());
        assertTrue(row.isPausable());
        assertTrue(row.isClosable());
        assertTrue(row.isEditable());
        assertTrue(row.isApplicationsViewable());
        assertFalse(row.isDeletable());
        assertFalse(row.isPublishable());
    }

    @Test
    @DisplayName("Published nhưng ngày đăng ở tương lai -> 'Hẹn đăng', không tạm dừng / đóng được")
    void scheduledRow() {
        loginAs(RequisitionAccess.ROLE_HR);
        LocalDateTime now = LocalDateTime.now();
        addRow(12, "Published", now.plusDays(3), now.plusDays(30), 0, 0);

        InternalJobPostingResponse row = firstRow(service.getListPage(emptyFilter(), viewer));

        assertEquals("Scheduled", row.getViewStatus());
        assertEquals("Hẹn đăng", row.getStatusLabel());
        assertEquals(3, row.getDaysUntilPosting());
        assertFalse(row.isPausable());
        assertFalse(row.isClosable());
        assertTrue(row.isEditable());
    }

    @Test
    @DisplayName("Tin nháp chưa có hồ sơ: đăng / sửa / xóa được; hạn nộp còn 5 ngày là 'soon'")
    void draftRow() {
        loginAs(RequisitionAccess.ROLE_SYSTEM_ADMIN);
        addRow(13, "Draft", null, LocalDateTime.now().plusDays(5), 0, 0);

        InternalJobPostingResponse row = firstRow(service.getListPage(emptyFilter(), viewer));

        assertTrue(row.isPublishable());
        assertTrue(row.isDeletable());
        assertEquals(JobPostingLabels.DEADLINE_SOON, row.getDeadlineState());
        assertFalse(row.isApplicationsViewable());
    }

    @Test
    @DisplayName("Admin không đóng tin được (D7: chỉ HR); tạm dừng thì mở lại được")
    void pausedRow_forAdmin() {
        loginAs(RequisitionAccess.ROLE_SYSTEM_ADMIN);
        LocalDateTime now = LocalDateTime.now();
        addRow(14, "Paused", now.minusDays(10), now.minusDays(1), 1, 0);

        InternalJobPostingResponse row = firstRow(service.getListPage(emptyFilter(), viewer));

        assertTrue(row.isReopenable());
        assertFalse(row.isClosable());
        assertEquals(JobPostingLabels.DEADLINE_PAST, row.getDeadlineState());
    }

    @Test
    @DisplayName("Director: mọi cờ thao tác đều tắt, vẫn xem được ứng viên nếu có hồ sơ")
    void directorRow_hasNoActions() {
        loginAs(RequisitionAccess.ROLE_DIRECTOR);
        LocalDateTime now = LocalDateTime.now();
        addRow(15, "Published", now.minusDays(1), now.plusDays(10), 2, 0);

        InternalJobPostingResponse row = firstRow(service.getListPage(emptyFilter(), viewer));

        assertFalse(row.isEditable() || row.isPausable() || row.isClosable() || row.isDeletable()
                || row.isPublishable() || row.isReopenable());
        assertTrue(row.isApplicationsViewable());
    }
}
