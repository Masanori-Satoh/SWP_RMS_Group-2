package com.group2.rms.jobposting.controller;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.jobposting.dto.InternalJobPostingResponse;
import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.dto.JobPostingFilterOptions;
import com.group2.rms.jobposting.dto.JobPostingListResponse;
import com.group2.rms.jobposting.dto.JobPostingSearchRequest;
import com.group2.rms.core.dto.FilterOption;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.jobposting.service.JobPostingService;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = JobPostingController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class JobPostingControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JobPostingService jobPostingService;
    @MockitoBean
    private com.group2.rms.candidate.service.TalentPoolService talentPoolService;

    @MockitoBean
    private com.group2.rms.candidate.service.RecruitmentEmailService recruitmentEmailService;

    @MockitoBean
    private RequisitionAccess requisitionAccess;

    @MockitoBean
    private DepartmentRepository departmentRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private com.group2.rms.jobposting.repository.JobPostingRepository jobPostingRepository;

    @MockitoBean
    private com.group2.rms.requisition.repository.JobRequisitionRepository jobRequisitionRepository;

    private User hrUser;

    @BeforeEach
    void setUp() {
        Role hrRole = Role.builder().roleId(2).roleName("HR").build();
        hrUser = User.builder()
                .userId(5)
                .username("hr_officer")
                .fullName("Nguyễn Thị HR")
                .accountStatus("Active")
                .role(hrRole)
                .build();

        when(userRepository.findByUsernameIgnoreCase("hr_officer")).thenReturn(Optional.of(hrUser));
        when(userRepository.findByUsernameIgnoreCase("candidate_user")).thenReturn(Optional.of(User.builder()
                .userId(99).username("candidate_user").accountStatus("Active")
                .role(Role.builder().roleName("Candidate").build()).build()));
        when(requisitionAccess.actor()).thenReturn(hrUser);
        when(requisitionAccess.role(hrUser)).thenReturn("HR");
        when(departmentRepository.findAll()).thenReturn(Collections.emptyList());
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = {"HR"})
    @DisplayName("GET /internal/job-postings: HR truy cập danh sách tin tuyển dụng thành công")
    void listJobPostings_asHr_returnsListView() throws Exception {
        InternalJobPostingResponse item = InternalJobPostingResponse.builder()
                .jobPostingId(1)
                .postingTitle("Java Developer")
                .requisitionCode("REQ-2026-001")
                .departmentName("Phòng Công Nghệ")
                .recruitmentRound(1)
                .postingStatus("Published")
                .viewStatus("Published")
                .statusLabel("Đang đăng")
                .statusTone("success")
                .postingNo(1)
                .employmentType("Toàn thời gian")
                .applicationCount(3)
                .newApplicationCount(1)
                .daysUntilDeadline(10L)
                .applicationDeadline(LocalDateTime.now().plusDays(10))
                .deadlineState("open")
                .pausable(true)
                .editable(true)
                .closable(true)
                .applicationsViewable(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(jobPostingService.getListPage(any(JobPostingSearchRequest.class), any(User.class)))
                .thenReturn(listOf(item, true));

        mvc.perform(get("/internal/job-postings"))
                .andExpect(status().isOk())
                .andExpect(view().name("job-postings/list"))
                .andExpect(model().attributeExists("list", "filterLinks"))
                .andExpect(model().attribute("activeMenu", "job-postings"));
    }

    @Test
    @WithMockUser(username = "director", roles = {"DIRECTOR"})
    @DisplayName("GET /internal/job-postings: Director được xem danh sách (D4), không có nút tạo tin")
    void listJobPostings_asDirector_ok() throws Exception {
        User director = User.builder().userId(20).username("director").fullName("Giám đốc").accountStatus("Active")
                .role(Role.builder().roleName("Director").build()).build();
        when(userRepository.findByUsernameIgnoreCase("director")).thenReturn(Optional.of(director));
        when(requisitionAccess.actor()).thenReturn(director);
        when(requisitionAccess.role(director)).thenReturn("Director");
        InternalJobPostingResponse item = InternalJobPostingResponse.builder()
                .jobPostingId(2).postingTitle("Tester").postingStatus("Published").viewStatus("Published")
                .statusLabel("Đang đăng").statusTone("success").departmentName("QA").postingNo(1)
                .employmentType("Toàn thời gian").createdAt(LocalDateTime.now()).build();
        when(jobPostingService.getListPage(any(JobPostingSearchRequest.class), any(User.class)))
                .thenReturn(listOf(item, false));

        mvc.perform(get("/internal/job-postings"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("+ Tạo tin tuyển dụng"))));
    }

    /** Dữ liệu màn danh sách giả: 1 dòng, bộ lọc rỗng, ô lọc như của HR. */
    private static JobPostingListResponse listOf(InternalJobPostingResponse item, boolean canManage) {
        var filter = new JobPostingSearchRequest("", List.of(), null, null, null, null, null, "newest", 0, 10);
        var options = new JobPostingFilterOptions(true, true, false, false,
                List.of(new FilterOption("Published", "Đang đăng")), List.of(), List.of(),
                List.of(new FilterOption("soon", "Sắp hết hạn")), List.of(new FilterOption("newest", "Mới đăng nhất")));
        return new JobPostingListResponse(new PageImpl<>(List.of(item)), filter, options, List.of(), canManage);
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = {"HR"})
    @DisplayName("GET /internal/job-postings/create: Mở form tạo tin với requisitionId thành công")
    void showCreateForm_asHr_returnsCreateView() throws Exception {
        JobPostingCreateRequest dto = JobPostingCreateRequest.builder()
                .requisitionId(101)
                .postingTitle("Java Developer")
                .build();

        when(jobPostingService.prepareCreateForm(eq(101), any(User.class))).thenReturn(dto);

        mvc.perform(get("/internal/job-postings/create").param("requisitionId", "101"))
                .andExpect(status().isOk())
                .andExpect(view().name("job-postings/create"))
                .andExpect(model().attributeExists("postingDto"));
    }

    @Test
    @DisplayName("GET /internal/job-postings: Chưa đăng nhập bị redirect về /login")
    void listJobPostings_unauthenticated_redirectsToLogin() throws Exception {
        mvc.perform(get("/internal/job-postings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "candidate_user", authorities = {"ROLE_CANDIDATE"})
    @DisplayName("GET /internal/job-postings: Role Candidate không có quyền truy cập (403)")
    void listJobPostings_asCandidate_forbidden() throws Exception {
        mvc.perform(get("/internal/job-postings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = {"HR"})
    @DisplayName("GET /internal/job-postings/{id}/edit: Mở form chỉnh sửa tin thành công")
    void showEditForm_asHr_returnsCreateView() throws Exception {
        JobPostingCreateRequest dto = JobPostingCreateRequest.builder()
                .jobPostingId(99)
                .requisitionId(101)
                .postingTitle("Java Developer")
                .build();

        when(jobPostingService.prepareEditForm(eq(99), any(User.class))).thenReturn(dto);

        mvc.perform(get("/internal/job-postings/99/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("job-postings/create"))
                .andExpect(model().attributeExists("postingDto"));
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = {"HR"})
    @DisplayName("GET /internal/job-postings/create (thiếu requisitionId) -> về danh sách, báo lỗi")
    void showCreateForm_missingRequisition_redirects() throws Exception {
        when(jobPostingService.prepareCreateForm(isNull(), any(User.class)))
                .thenThrow(new JobPostingValidationException("Mã yêu cầu tuyển dụng không hợp lệ."));

        mvc.perform(get("/internal/job-postings/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/internal/job-postings"))
                .andExpect(flash().attribute("errorMessage", "Mã yêu cầu tuyển dụng không hợp lệ."));
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = {"HR"})
    @DisplayName("POST /internal/job-postings/create: lỗi theo ô -> hiện lại form, lỗi nằm ở đúng ô; trường lạ bị bỏ qua")
    void createJobPosting_fieldErrors_rerenderForm() throws Exception {
        when(jobPostingService.createAndPublishJobPosting(any(JobPostingCreateRequest.class), any(User.class)))
                .thenThrow(new JobPostingValidationException(java.util.Map.of(
                        "applicationDeadline", "Vui lòng chọn hạn nộp hồ sơ.")));
        when(jobPostingService.restoreSourceInfo(any(JobPostingCreateRequest.class), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/internal/job-postings/create")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("requisitionId", "101")
                        .param("postingTitle", "Java Developer")
                        .param("salaryMode", "nego")
                        .param("action", "publish")
                        // Không có trong whitelist của binder: không được nhận
                        .param("postingStatus", "Published")
                        .param("minSalary", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("job-postings/create"))
                .andExpect(model().attributeHasFieldErrors("postingDto", "applicationDeadline"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng chọn hạn nộp hồ sơ.")));

        var captor = org.mockito.ArgumentCaptor.forClass(JobPostingCreateRequest.class);
        org.mockito.Mockito.verify(jobPostingService).createAndPublishJobPosting(captor.capture(), any(User.class));
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().getPostingStatus());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().getMinSalary());
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = {"HR"})
    @DisplayName("POST /internal/job-postings/create (lưu nháp) -> sang trang chi tiết tin, có thông báo")
    void createJobPosting_draft_redirectsToDetail() throws Exception {
        when(jobPostingService.createJobPosting(any(JobPostingCreateRequest.class), any(User.class))).thenReturn(55);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/internal/job-postings/create")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("requisitionId", "101")
                        .param("action", "draft"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/internal/job-postings/55"))
                .andExpect(flash().attribute("successMessage", "Đã lưu bản nháp tin tuyển dụng."));
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = {"HR"})
    @DisplayName("GET /internal/job-postings/{id}: dựng trang chi tiết đủ phần (số liệu, tiến trình, hồ sơ, các lần đăng)")
    void showDetail_rendersAllSections() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        var detail = com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse.builder()
                .jobPostingId(30).postingTitle("Product Owner (SaaS)").postingNo(2).requisitionId(16)
                .requisitionCode("REQ-0016").requisitionTitle("Product Owner").departmentName("Product")
                .employmentType("Toàn thời gian").workModelLabel("Kết hợp").numberOfPositions(1)
                .workLocation("Tầng 8, RMS Tower").salaryDisplay("35 – 45 triệu VND/tháng").numberOfPositionsDisplay("1 người")
                .jobDescription("Mô tả").jobRequirements("Yêu cầu").benefits("- Lương tháng 13")
                .postingStatus("Published").viewStatus("Published").postingStatusLabel("Đang đăng").statusTone("success")
                .postingDate(now.minusDays(10)).applicationDeadline(now.plusDays(5)).daysUntilDeadline(5L)
                .createdAt(now.minusDays(11)).createdByName("Lê Thị Mai Lan").hiringManagerName("Lê Thu Trang")
                .live(true).canManage(true).editable(true).pausable(true).closable(true).publicViewable(true)
                .totalApplications(14).pendingApplications(9).aiPassedApplications(7).aiThreshold(new java.math.BigDecimal("70"))
                .interviewingApplications(2).hiredApplications(0)
                .recentApplications(List.of(new com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse.RecentApplication(
                        501, "Trần Quốc Bảo", new java.math.BigDecimal("84.5"), true, "Đang phỏng vấn", "badge--warning", now.minusDays(2))))
                .rounds(List.of(
                        new com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse.PostingRound(29, 1, now.minusDays(60), now.minusDays(20), "Đã đóng", "neutral", 23, 0, false),
                        new com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse.PostingRound(30, 2, now.minusDays(10), now.plusDays(5), "Đang đăng", "success", 14, 0, true)))
                .activityHistory(List.of()).screeningCriteria(List.of())
                .build();
        when(jobPostingService.getInternalJobPostingDetail(eq(30), any(User.class))).thenReturn(detail);

        mvc.perform(get("/internal/job-postings/30"))
                .andExpect(status().isOk())
                .andExpect(view().name("job-postings/detail"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Còn 5 ngày là hết hạn nộp hồ sơ."),
                        org.hamcrest.Matchers.containsString("Tạm dừng nhận hồ sơ"),
                        org.hamcrest.Matchers.containsString("Trần Quốc Bảo"),
                        org.hamcrest.Matchers.containsString("Xem tất cả 14 hồ sơ"),
                        org.hamcrest.Matchers.containsString("Tin đang xem"))));
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = {"HR"})
    @DisplayName("POST /internal/job-postings/delete/{id}: Xóa tin tuyển dụng thành công và redirect")
    void deleteJobPosting_asHr_redirectsWithFlashMessage() throws Exception {
        org.mockito.Mockito.doNothing().when(jobPostingService).deleteJobPosting(eq(99), any(User.class));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/internal/job-postings/delete/99")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/internal/job-postings"))
                .andExpect(flash().attributeExists("message"));
    }
}
