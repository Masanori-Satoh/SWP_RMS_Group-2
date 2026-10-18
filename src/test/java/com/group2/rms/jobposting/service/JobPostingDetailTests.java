package com.group2.rms.jobposting.service;

import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.service.TalentPoolService;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/*
 * Trang chi tiết tin tuyển dụng (Bước 4 trong plan_posting.md):
 * cờ thao tác theo trạng thái × vai trò, số liệu hồ sơ, Đăng ngay / Hủy lịch đăng / Đăng lại.
 */
@ExtendWith(MockitoExtension.class)
class JobPostingDetailTests {

    @Mock private JobPostingRepository postings;
    @Mock private JobRequisitionRepository requisitions;
    @Mock private AuditLogRepository audit;
    @Mock private RequisitionAccess access;
    @Mock private NotificationService notifications;
    @Mock private TalentPoolService talentPool;
    @Mock private ApplicationRepository applications;
    @Mock private DepartmentRepository departments;
    @Spy private JobPostingValidator validator = new JobPostingValidator();
    @InjectMocks private JobPostingServiceImpl service;

    private User viewer;
    private JobRequisition requisition;

    @BeforeEach
    void setUp() {
        viewer = User.builder().userId(1).fullName("Người xem").build();
        requisition = JobRequisition.builder().requisitionId(16).requisitionCode("REQ-0016").title("Product Owner")
                .approvalStatus(RequisitionAccess.STATUS_APPROVED).numberOfPositions(1).employmentType("Full-time")
                .workModel("Hybrid").department(Department.builder().departmentId(4).departmentName("Product").build())
                .screeningCriteria(new ArrayList<>()).build();
        lenient().when(postings.findAiScoreThreshold()).thenReturn(Optional.of("70"));
        lenient().when(postings.save(any(JobPosting.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private JobPosting posting(int id, String status, LocalDateTime postingDate) {
        JobPosting posting = JobPosting.builder().jobPostingId(id).postingNo(2).postingTitle("Product Owner (SaaS)")
                .postingStatus(status).postingDate(postingDate).requisition(requisition)
                .applicationDeadline(LocalDateTime.now().plusDays(5)).build();
        lenient().when(postings.findById(id)).thenReturn(Optional.of(posting));
        lenient().when(postings.findForUpdate(id)).thenReturn(Optional.of(posting));
        return posting;
    }

    private InternalJobPostingDetailResponse detailAs(String role, int id) {
        when(access.role(viewer)).thenReturn(role);
        return service.getInternalJobPostingDetail(id, viewer);
    }

    @Test
    @DisplayName("Đang đăng, HR: tạm dừng / đóng / sửa; số liệu đếm theo trạng thái; ngưỡng AI từ cấu hình")
    void published_forHr_withKpis() {
        posting(30, "Published", LocalDateTime.now().minusDays(10));
        when(postings.countApplicationsByStatus(30)).thenReturn(List.of(
                new Object[] {"Applied", 4L}, new Object[] {"AI_Screened", 5L}, new Object[] {"Interviewing", 2L},
                new Object[] {"Hired", 1L}, new Object[] {"Rejected", 2L}));
        when(postings.countApplicationsSince(eq(30), any())).thenReturn(2L);
        when(postings.countApplicationsWithAiScoreAtLeast(30, new BigDecimal("70"))).thenReturn(7L);

        InternalJobPostingDetailResponse d = detailAs(RequisitionAccess.ROLE_HR, 30);

        assertEquals("Published", d.getViewStatus());
        assertTrue(d.isLive());
        assertTrue(d.isPausable() && d.isClosable() && d.isEditable() && d.isPublicViewable());
        assertFalse(d.isPublishable() || d.isDeletable() || d.isRepostable());
        assertEquals(14, d.getTotalApplications());
        assertEquals(9, d.getPendingApplications(), "Chờ xử lý = mới nộp + AI đã chấm");
        assertEquals(7, d.getAiPassedApplications());
        assertEquals(2, d.getApplicationsLast7Days());
        assertEquals(1, d.getHiredApplications());
    }

    @Test
    @DisplayName("Hẹn đăng: Đăng ngay / Hủy lịch đăng; chưa có số liệu; ứng viên chưa thấy tin")
    void scheduled_forAdmin() {
        posting(31, "Published", LocalDateTime.now().plusDays(3));

        InternalJobPostingDetailResponse d = detailAs(RequisitionAccess.ROLE_SYSTEM_ADMIN, 31);

        assertEquals("Scheduled", d.getViewStatus());
        assertTrue(d.isPublishNowable() && d.isUnschedulable() && d.isEditable());
        assertFalse(d.isPausable() || d.isClosable() || d.isPublicViewable() || d.isLive());
        verify(postings, never()).countApplicationsByStatus(any());
    }

    @Test
    @DisplayName("Đã đóng, yêu cầu còn duyệt, chưa có tin đang mở: được Đăng lại; Director thì không có nút nào")
    void closed_repostable_onlyForManagers() {
        posting(32, "Closed", LocalDateTime.now().minusDays(40));
        when(postings.existsByRequisition_RequisitionIdAndPostingStatusIn(eq(16), any())).thenReturn(false);

        assertTrue(detailAs(RequisitionAccess.ROLE_HR, 32).isRepostable());

        InternalJobPostingDetailResponse director = detailAs(RequisitionAccess.ROLE_DIRECTOR, 32);
        assertFalse(director.isRepostable() || director.isEditable() || director.isClosable());
    }

    @Test
    @DisplayName("HM / Director mở tin nháp -> 403 (D5)")
    void draft_hiddenFromDirector() {
        posting(33, "Draft", null);
        when(access.role(viewer)).thenReturn(RequisitionAccess.ROLE_DIRECTOR);
        assertThrows(AccessDeniedException.class, () -> service.getInternalJobPostingDetail(33, viewer));
    }

    @Test
    @DisplayName("Đăng ngay: ngày đăng = bây giờ, có thông báo; tin không hẹn đăng thì báo lỗi")
    void publishNow() {
        JobPosting scheduled = posting(34, "Published", LocalDateTime.now().plusDays(2));
        when(access.role(viewer)).thenReturn(RequisitionAccess.ROLE_HR);

        service.publishNow(34, viewer);

        assertFalse(scheduled.getPostingDate().isAfter(LocalDateTime.now()));
        verify(notifications).notifyJobPostingPublished(scheduled, viewer);

        posting(35, "Published", LocalDateTime.now().minusDays(1));
        assertThrows(JobPostingValidationException.class, () -> service.publishNow(35, viewer));
    }

    @Test
    @DisplayName("Hủy lịch đăng: về bản nháp, PostingDate = NULL")
    void unschedule() {
        JobPosting scheduled = posting(36, "Published", LocalDateTime.now().plusDays(2));
        when(access.role(viewer)).thenReturn(RequisitionAccess.ROLE_SYSTEM_ADMIN);

        service.unschedule(36, viewer);

        assertEquals("Draft", scheduled.getPostingStatus());
        assertNull(scheduled.getPostingDate());
        verifyNoInteractions(notifications);
    }

    @Test
    @DisplayName("Đăng lại (D10): form tin lần mới, nội dung từ tin đã đóng; tin chưa đóng thì báo lỗi")
    void prepareRepostForm() {
        JobPosting closed = posting(37, "Closed", LocalDateTime.now().minusDays(40));
        closed.setSalaryDisplay("Lên đến 45 triệu");
        closed.setBenefits("- Lương tháng 13");
        when(access.role(viewer)).thenReturn(RequisitionAccess.ROLE_HR);
        when(requisitions.findById(16)).thenReturn(Optional.of(requisition));
        when(postings.existsByRequisition_RequisitionIdAndPostingStatusIn(eq(16), any())).thenReturn(false);
        when(postings.findMaxPostingNo(16)).thenReturn(2);

        JobPostingCreateRequest form = service.prepareRepostForm(37, viewer);

        assertNull(form.getJobPostingId(), "Là tin mới");
        assertEquals(3, form.getPostingNo());
        assertEquals("Product Owner (SaaS)", form.getPostingTitle());
        assertEquals(JobPostingCreateRequest.SALARY_CUSTOM, form.getSalaryMode());
        assertEquals("- Lương tháng 13", form.getBenefits());

        posting(38, "Paused", LocalDateTime.now().minusDays(3));
        assertThrows(JobPostingValidationException.class, () -> service.prepareRepostForm(38, viewer));
    }
}
