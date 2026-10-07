package com.group2.rms.requisition.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.requisition.dto.JobPostingCreateRequest;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.repository.JobPostingRepository;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobPostingServiceTest {

    @Mock private JobPostingRepository jobPostingRepository;
    @Mock private JobRequisitionRepository jobRequisitionRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private RequisitionAccess requisitionAccess;

    @InjectMocks private JobPostingServiceImpl jobPostingService;

    private User hrUser;
    private User hmUser;
    private JobRequisition approvedReq;

    @BeforeEach
    void setUp() {
        hrUser = User.builder().userId(1).username("hr_user").fullName("HR Specialist").build();
        hmUser = User.builder().userId(2).username("hm_user").fullName("Hiring Manager").build();

        Department dept = Department.builder().departmentId(10).departmentName("Phòng Công Nghệ").build();

        approvedReq = JobRequisition.builder()
                .requisitionId(101)
                .requisitionCode("REQ-2026-001")
                .title("Java Developer")
                .approvalStatus(RequisitionAccess.STATUS_APPROVED)
                .department(dept)
                .hiringManager(hmUser)
                .recruitmentRound(1)
                .numberOfPositions(2)
                .employmentType("Full-time")
                .minSalary(BigDecimal.valueOf(15000000))
                .maxSalary(BigDecimal.valueOf(25000000))
                .workLocation("Hà Nội")
                .jobDescription("Lập trình hệ thống backend.")
                .requirementDetails("Kinh nghiệm 2 năm Spring Boot.")
                .build();
    }

    @Test
    @DisplayName("prepareCreateForm: HR lấy form từ Requisition đã duyệt thành công")
    void prepareCreateForm_success() {
        when(requisitionAccess.role(hrUser)).thenReturn(RequisitionAccess.ROLE_HR);
        when(jobRequisitionRepository.findById(101)).thenReturn(Optional.of(approvedReq));
        when(jobPostingRepository.existsByRequisition_RequisitionIdAndPostingStatusIn(eq(101), any())).thenReturn(false);

        JobPostingCreateRequest result = jobPostingService.prepareCreateForm(101, hrUser);

        assertNotNull(result);
        assertEquals(101, result.getRequisitionId());
        assertEquals("REQ-2026-001", result.getRequisitionCode());
        assertEquals("Java Developer", result.getPostingTitle());
        assertEquals("Phòng Công Nghệ", result.getDepartmentName());
        assertEquals("Toàn thời gian", result.getEmploymentType());
        assertEquals("15.000.000 - 25.000.000 VNĐ", result.getSalaryDisplay());
        assertEquals("Hà Nội", result.getWorkLocation());
        assertEquals("draft", result.getAction());
    }

    @Test
    @DisplayName("prepareCreateForm: Chặn nếu người dùng không phải HR hay System Admin")
    void prepareCreateForm_forbidden_nonHr() {
        when(requisitionAccess.role(hmUser)).thenReturn(RequisitionAccess.ROLE_HIRING_MANAGER);

        assertThrows(AccessDeniedException.class, () -> jobPostingService.prepareCreateForm(101, hmUser));
        verify(jobRequisitionRepository, never()).findById(any());
    }

    @Test
    @DisplayName("prepareCreateForm: Chặn nếu Requisition chưa Approved")
    void prepareCreateForm_notApproved() {
        approvedReq.setApprovalStatus(RequisitionAccess.STATUS_PENDING_DIRECTOR);
        when(requisitionAccess.role(hrUser)).thenReturn(RequisitionAccess.ROLE_HR);
        when(jobRequisitionRepository.findById(101)).thenReturn(Optional.of(approvedReq));

        assertThrows(ResponseStatusException.class, () -> jobPostingService.prepareCreateForm(101, hrUser));
    }

    @Test
    @DisplayName("prepareCreateForm: Chặn nếu đã có tin đăng hoặc bản nháp đang hoạt động")
    void prepareCreateForm_activePostingExists() {
        when(requisitionAccess.role(hrUser)).thenReturn(RequisitionAccess.ROLE_HR);
        when(jobRequisitionRepository.findById(101)).thenReturn(Optional.of(approvedReq));
        when(jobPostingRepository.existsByRequisition_RequisitionIdAndPostingStatusIn(eq(101), any())).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> jobPostingService.prepareCreateForm(101, hrUser));
    }

    @Test
    @DisplayName("createJobPosting: Lưu bản nháp (Draft) thành công")
    void createJobPosting_draft_success() {
        when(requisitionAccess.role(hrUser)).thenReturn(RequisitionAccess.ROLE_HR);
        when(jobRequisitionRepository.findById(101)).thenReturn(Optional.of(approvedReq));
        when(jobPostingRepository.existsByRequisition_RequisitionIdAndPostingStatusIn(eq(101), any())).thenReturn(false);

        JobPosting savedEntity = JobPosting.builder().jobPostingId(555).postingTitle("Java Developer").postingStatus("Draft").build();
        when(jobPostingRepository.save(any(JobPosting.class))).thenReturn(savedEntity);

        JobPostingCreateRequest request = JobPostingCreateRequest.builder()
                .requisitionId(101)
                .postingTitle("Java Developer")
                .jobDescription("Mô tả công việc")
                .jobRequirements("Yêu cầu công việc")
                .applicationDeadline(LocalDate.now().plusDays(15))
                .action("draft")
                .build();

        Integer postingId = jobPostingService.createJobPosting(request, hrUser);

        assertEquals(555, postingId);
        var captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).save(captor.capture());
        JobPosting captured = captor.getValue();
        assertEquals("Draft", captured.getPostingStatus());
        assertNull(captured.getPostingDate());
        assertEquals(hrUser, captured.getCreatedBy());

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("createJobPosting: Đăng tin tuyển dụng (Published) thành công")
    void createJobPosting_publish_success() {
        when(requisitionAccess.role(hrUser)).thenReturn(RequisitionAccess.ROLE_HR);
        when(jobRequisitionRepository.findById(101)).thenReturn(Optional.of(approvedReq));
        when(jobPostingRepository.existsByRequisition_RequisitionIdAndPostingStatusIn(eq(101), any())).thenReturn(false);

        JobPosting savedEntity = JobPosting.builder().jobPostingId(777).postingTitle("Java Developer").postingStatus("Published").build();
        when(jobPostingRepository.save(any(JobPosting.class))).thenReturn(savedEntity);

        JobPostingCreateRequest request = JobPostingCreateRequest.builder()
                .requisitionId(101)
                .postingTitle("Java Developer")
                .jobDescription("Mô tả công việc")
                .jobRequirements("Yêu cầu công việc")
                .applicationDeadline(LocalDate.now().plusDays(15))
                .action("publish")
                .build();

        Integer postingId = jobPostingService.createJobPosting(request, hrUser);

        assertEquals(777, postingId);
        var captor = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).save(captor.capture());
        JobPosting captured = captor.getValue();
        assertEquals("Published", captured.getPostingStatus());
        assertNotNull(captured.getPostingDate());
        assertEquals(hrUser, captured.getCreatedBy());
    }

    @Test
    @DisplayName("createJobPosting: Báo lỗi nếu hạn nộp hồ sơ ở quá khứ")
    void createJobPosting_pastDeadline_throwsException() {
        when(requisitionAccess.role(hrUser)).thenReturn(RequisitionAccess.ROLE_HR);
        when(jobRequisitionRepository.findById(101)).thenReturn(Optional.of(approvedReq));
        when(jobPostingRepository.existsByRequisition_RequisitionIdAndPostingStatusIn(eq(101), any())).thenReturn(false);

        JobPostingCreateRequest request = JobPostingCreateRequest.builder()
                .requisitionId(101)
                .postingTitle("Java Developer")
                .jobDescription("Mô tả công việc")
                .jobRequirements("Yêu cầu công việc")
                .applicationDeadline(LocalDate.now().minusDays(1))
                .action("publish")
                .build();

        assertThrows(ResponseStatusException.class, () -> jobPostingService.createJobPosting(request, hrUser));
        verify(jobPostingRepository, never()).save(any());
    }

    @Test
    @DisplayName("searchInternalJobPostings: HR tìm kiếm thành công và map đúng DTO")
    void searchInternalJobPostings_asHr_success() {
        when(requisitionAccess.role(hrUser)).thenReturn(RequisitionAccess.ROLE_HR);

        JobPosting posting = JobPosting.builder()
                .jobPostingId(99)
                .postingTitle("Backend Lead")
                .postingStatus("Published")
                .requisition(approvedReq)
                .applicationDeadline(LocalDateTime.now().plusDays(20))
                .postingDate(LocalDateTime.now().minusDays(1))
                .createdBy(hrUser)
                .build();

        Page<JobPosting> page = new PageImpl<>(List.of(posting));
        when(jobPostingRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<?> result = jobPostingService.searchInternalJobPostings(1, 10, "Backend", 10, "Published", "newest", hrUser);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        var first = (com.group2.rms.requisition.dto.InternalJobPostingResponse) result.getContent().get(0);
        assertEquals(99, first.getJobPostingId());
        assertEquals("Backend Lead", first.getPostingTitle());
        assertEquals("REQ-2026-001", first.getRequisitionCode());
        assertEquals("Phòng Công Nghệ", first.getDepartmentName());
    }

    @Test
    @DisplayName("searchInternalJobPostings: Role không phải HR/Admin bị chặn 403")
    void searchInternalJobPostings_nonHr_throwsAccessDenied() {
        when(requisitionAccess.role(hmUser)).thenReturn(RequisitionAccess.ROLE_HIRING_MANAGER);

        assertThrows(AccessDeniedException.class, () ->
                jobPostingService.searchInternalJobPostings(1, 10, "", null, "", "newest", hmUser));
        verify(jobPostingRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}
