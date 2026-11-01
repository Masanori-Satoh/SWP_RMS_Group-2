package com.group2.rms.jobposting.controller;

import com.group2.rms.candidate.talentpool.exception.TalentPoolException;
import com.group2.rms.candidate.talentpool.service.RecruitmentEmailService;
import com.group2.rms.candidate.talentpool.service.TalentPoolService;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.dto.FilterOption;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse.PostingRound;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse.RecentApplication;
import com.group2.rms.jobposting.dto.InternalJobPostingResponse;
import com.group2.rms.jobposting.dto.JobPostingRequest;
import com.group2.rms.jobposting.dto.JobPostingFilterOptions;
import com.group2.rms.jobposting.dto.JobPostingListResponse;
import com.group2.rms.jobposting.dto.JobPostingSearchRequest;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.jobposting.service.JobPostingListService;
import com.group2.rms.jobposting.service.JobPostingService;
import com.group2.rms.jobposting.validator.JobPostingValidator;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * CONTROLLER TEST (@WebMvcTest): JobPostingController với cấu hình bảo mật THẬT (SecurityConfig).
 * Service / list service / access / validator là mock; template được render thật (Thymeleaf).
 * Kiểm tra: URL → view / redirect / flash, lỗi theo ô, Bean Validation (@Valid), 403 / 404, CSRF, whitelist form.
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện; POST luôn .with(csrf()) trừ test thiếu CSRF.
 */
@WebMvcTest(controllers = JobPostingController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class JobPostingControllerTests {

    private static final String BASE = "/internal/job-postings";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JobPostingService service;
    @MockitoBean
    private JobPostingListService listService;
    @MockitoBean
    private JobPostingAccess access;
    @MockitoBean
    private JobPostingValidator validator;
    @MockitoBean
    private TalentPoolService talentPoolService;
    @MockitoBean
    private RecruitmentEmailService recruitmentEmailService;
    // Bộ lọc phiên (AccountSessionGuardFilter) đọc tài khoản ở mỗi request
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private DepartmentRepository departmentRepository;
    @MockitoBean
    private JobPostingRepository jobPostingRepository;
    @MockitoBean
    private JobRequisitionRepository jobRequisitionRepository;

    private User hrUser;

    @BeforeEach
    void setUp() {
        hrUser = account("hr_officer", RequisitionAccess.ROLE_HR);
        account("candidate_user", "Candidate");
        account("hm_user", RequisitionAccess.ROLE_HIRING_MANAGER);
        when(access.actor()).thenReturn(hrUser);
        when(access.role(hrUser)).thenReturn(RequisitionAccess.ROLE_HR);
        when(validator.today()).thenReturn(LocalDate.of(2026, 10, 18));
    }

    // ----- Hàm hỗ trợ -----

    private User account(String username, String roleName) {
        User user = User.builder().userId(username.hashCode()).username(username).fullName("Tên " + username)
                .accountStatus("Active").role(Role.builder().roleName(roleName).build()).build();
        when(userRepository.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(user));
        return user;
    }

    /** Dữ liệu màn danh sách giả: 1 dòng, bộ lọc rỗng, ô lọc như của HR. */
    private static JobPostingListResponse listOf(InternalJobPostingResponse row, boolean canManage) {
        JobPostingSearchRequest filter = new JobPostingSearchRequest("", List.of(), null, null, null, null, null,
                "newest", 0, 10);
        JobPostingFilterOptions options = new JobPostingFilterOptions(true, true, false, false,
                List.of(new FilterOption("Published", "Đang đăng")), List.of(), List.of(),
                List.of(new FilterOption("soon", "Sắp hết hạn")), List.of(new FilterOption("newest", "Mới đăng nhất")));
        return new JobPostingListResponse(new PageImpl<>(List.of(row)), filter, options, List.of(), canManage);
    }

    private static InternalJobPostingResponse publishedRow() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 18, 10, 0);
        return InternalJobPostingResponse.builder().jobPostingId(1).postingTitle("Java Developer")
                .departmentName("Phòng Công Nghệ").employmentType("Toàn thời gian").postingNo(1)
                .viewStatus("Published").statusLabel("Đang đăng").statusTone("success")
                .applicationCount(3).newApplicationCount(1).daysUntilDeadline(10L)
                .applicationDeadline(now.plusDays(10)).postingDate(now.minusDays(2)).deadlineState("open")
                .pausable(true).editable(true).closable(true).applicationsViewable(true).build();
    }

    /** Trang chi tiết tin đang đăng còn 5 ngày, có số liệu, 2 lần đăng. */
    private static InternalJobPostingDetailResponse.InternalJobPostingDetailResponseBuilder publishedDetail() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 18, 10, 0);
        return InternalJobPostingDetailResponse.builder()
                .jobPostingId(30).postingTitle("Product Owner (SaaS)").postingNo(2).requisitionId(16)
                .requisitionCode("REQ-0016").requisitionTitle("Product Owner").departmentName("Product")
                .employmentType("Toàn thời gian").workModelLabel("Kết hợp").numberOfPositions(1)
                .workLocation("Tầng 8, RMS Tower").salaryDisplay("35 – 45 triệu VND/tháng").numberOfPositionsDisplay("1 người")
                .jobDescription("Mô tả").jobRequirements("Yêu cầu").benefits("- Lương tháng 13")
                .postingStatus("Published").viewStatus("Published").postingStatusLabel("Đang đăng").statusTone("success")
                .postingDate(now.minusDays(10)).applicationDeadline(now.plusDays(5)).daysUntilDeadline(5L)
                .createdAt(now.minusDays(11)).createdByName("Lê Thị Mai Lan").hiringManagerName("Lê Thu Trang")
                .deadlineSoonBanner(true).deadlineCountdown(true)
                .live(true).canManage(true).editable(true).pausable(true).closable(true).publicViewable(true)
                .totalApplications(14).pendingApplications(9)
                .interviewingApplications(2).hiredApplications(0)
                .recentApplications(List.of(new RecentApplication(501, "Trần Quốc Bảo",
                        "Đang phỏng vấn", "badge--warning", now.minusDays(2))))
                .rounds(List.of(
                        new PostingRound(29, 1, now.minusDays(60), now.minusDays(20), "Đã đóng", "neutral", 23, 0, false),
                        new PostingRound(30, 2, now.minusDays(10), now.plusDays(5), "Đang đăng", "success", 14, 0, true)))
                .activityHistory(List.of());
    }

    // =========================================================================
    // Danh sách + quyền theo URL
    // =========================================================================

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("GET danh sách (HR) -> View list, có model list + filterLinks, sáng menu Tin tuyển dụng")
    void listJobPostings_shouldRenderListView_whenViewerIsHr() throws Exception {
        when(listService.getListPage(any(JobPostingSearchRequest.class), eq(hrUser))).thenReturn(listOf(publishedRow(), true));

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(view().name("job-postings/list"))
                .andExpect(model().attributeExists("list", "filterLinks"))
                .andExpect(model().attribute("activeMenu", "job-postings"));
    }

    @Test
    @WithMockUser(username = "director", roles = "DIRECTOR")
    @DisplayName("GET danh sách (Director, D4) -> Xem được, không có nút tạo tin")
    void listJobPostings_shouldHideCreateButton_whenViewerIsDirector() throws Exception {
        User director = account("director", RequisitionAccess.ROLE_DIRECTOR);
        when(access.actor()).thenReturn(director);
        when(listService.getListPage(any(JobPostingSearchRequest.class), eq(director))).thenReturn(listOf(publishedRow(), false));

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("+ Tạo tin tuyển dụng"))));
    }

    @Test
    @DisplayName("Chưa đăng nhập -> Chuyển về /login")
    void listJobPostings_shouldRedirectToLogin_whenNotAuthenticated() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "candidate_user", authorities = "ROLE_CANDIDATE")
    @DisplayName("Ứng viên gõ thẳng URL nội bộ -> 403, không gọi service")
    void listJobPostings_shouldReturnForbidden_whenViewerIsCandidate() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isForbidden());

        verifyNoInteractions(listService);
    }

    @Test
    @WithMockUser(username = "hm_user", roles = "HIRING_MANAGER")
    @DisplayName("HM gửi POST tạm dừng (gõ URL / sửa HTML) -> 403 theo SecurityConfig, không gọi service")
    void pauseJobPosting_shouldReturnForbidden_whenViewerIsHiringManager() throws Exception {
        mvc.perform(post(BASE + "/1/pause").with(csrf())).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    // =========================================================================
    // Form tạo / sửa
    // =========================================================================

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("GET form tạo với requisitionId -> View form, có form, giới hạn độ dài và 'hôm nay' từ validator")
    void showCreateForm_shouldRenderForm_whenRequisitionIsValid() throws Exception {
        JobPostingRequest dto = JobPostingRequest.builder().requisitionId(101).postingTitle("Java Developer")
                .draftOrNew(true).build();
        when(service.prepareCreateForm(eq(101), any(User.class))).thenReturn(dto);

        mvc.perform(get(BASE + "/create").param("requisitionId", "101"))
                .andExpect(status().isOk())
                .andExpect(view().name("job-postings/create"))
                .andExpect(model().attributeExists("form", "requisitionChoices"))
                .andExpect(model().attribute("titleLimit", JobPostingValidator.TITLE_LIMIT))
                .andExpect(model().attribute("today", LocalDate.of(2026, 10, 18)));
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("GET form tạo thiếu requisitionId -> Về danh sách, flash lỗi")
    void showCreateForm_shouldRedirectWithError_whenRequisitionIsMissing() throws Exception {
        when(service.prepareCreateForm(isNull(), any(User.class)))
                .thenThrow(new JobPostingValidationException("Mã yêu cầu tuyển dụng không hợp lệ."));

        mvc.perform(get(BASE + "/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("errorMessage", "Mã yêu cầu tuyển dụng không hợp lệ."));
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("Đăng lại tin chưa đóng -> Quay về trang chi tiết tin đó, flash lỗi")
    void showCreateForm_shouldRedirectToPosting_whenRepostIsRejected() throws Exception {
        when(service.prepareRepostForm(eq(38), any(User.class)))
                .thenThrow(new JobPostingValidationException("Chỉ tin đã đóng mới đăng lại được."));

        mvc.perform(get(BASE + "/create").param("fromPostingId", "38"))
                .andExpect(redirectedUrl(BASE + "/38"))
                .andExpect(flash().attribute("errorMessage", "Chỉ tin đã đóng mới đăng lại được."));
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("GET form sửa -> Dùng chung template form, không có ô 'Đổi yêu cầu'")
    void showEditForm_shouldRenderSharedForm() throws Exception {
        JobPostingRequest dto = JobPostingRequest.builder().jobPostingId(99).requisitionId(101)
                .postingTitle("Java Developer").live(true).build();
        when(service.prepareEditForm(eq(99), any(User.class))).thenReturn(dto);

        mvc.perform(get(BASE + "/99/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("job-postings/create"))
                .andExpect(model().attribute("requisitionChoices", List.of()));
        verify(service, never()).requisitionChoices(any());
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("POST tạo: lỗi theo ô từ service -> Hiện lại form, lỗi đúng ô; trường lạ (postingStatus, draftOrNew) bị bỏ qua")
    void createJobPosting_shouldRerenderFormWithFieldError_whenServiceRejects() throws Exception {
        // Arrange
        when(service.createAndPublishJobPosting(any(JobPostingRequest.class), any(User.class)))
                .thenThrow(new JobPostingValidationException(Map.of("applicationDeadline", "Vui lòng chọn hạn nộp hồ sơ.")));
        when(service.restoreSourceInfo(any(JobPostingRequest.class), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act + Assert
        mvc.perform(post(BASE + "/create").with(csrf())
                        .param("requisitionId", "101").param("postingTitle", "Java Developer")
                        .param("salaryMode", JobPostingRequest.SALARY_NEGOTIABLE)
                        .param("action", JobPostingValidator.ACTION_PUBLISH)
                        // Không có trong whitelist của binder: không được nhận
                        .param("draftOrNew", "false").param("live", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("job-postings/create"))
                .andExpect(model().attributeHasFieldErrors("form", "applicationDeadline"));

        ArgumentCaptor<JobPostingRequest> captor = ArgumentCaptor.forClass(JobPostingRequest.class);
        verify(service).createAndPublishJobPosting(captor.capture(), any(User.class));
        assertFalse(captor.getValue().isLive(), "Trường trạng thái form do server tính, không nhận từ form");
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("POST tạo: tiêu đề TITLE_LIMIT + 1 ký tự (Bean Validation) -> Lỗi ở ô tiêu đề, không gọi service tạo")
    void createJobPosting_shouldRejectBeforeService_whenTitleIsTooLong() throws Exception {
        when(service.restoreSourceInfo(any(JobPostingRequest.class), any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(post(BASE + "/create").with(csrf())
                        .param("requisitionId", "101").param("action", JobPostingValidator.ACTION_DRAFT)
                        .param("postingTitle", "a".repeat(JobPostingValidator.TITLE_LIMIT + 1)))
                .andExpect(status().isOk())
                .andExpect(view().name("job-postings/create"))
                .andExpect(model().attributeHasFieldErrorCode("form", "postingTitle", "Size"));
        verify(service, never()).createJobPosting(any(), any());
        verify(service, never()).createAndPublishJobPosting(any(), any());
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("POST tạo (lưu nháp) -> Sang trang chi tiết tin, flash 'Đã lưu bản nháp'")
    void createJobPosting_shouldRedirectToDetail_whenDraftIsSaved() throws Exception {
        when(service.createJobPosting(any(JobPostingRequest.class), any(User.class))).thenReturn(55);

        mvc.perform(post(BASE + "/create").with(csrf())
                        .param("requisitionId", "101").param("action", JobPostingValidator.ACTION_DRAFT))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(BASE + "/55"))
                .andExpect(flash().attribute("successMessage", "Đã lưu bản nháp tin tuyển dụng."));
        verify(service, never()).createAndPublishJobPosting(any(), any());
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("POST tạo và đăng -> Flash thông báo ứng viên có thể nộp ngay")
    void createJobPosting_shouldShowPublishedMessage() throws Exception {
        when(service.createAndPublishJobPosting(any(JobPostingRequest.class), any(User.class))).thenReturn(56);

        mvc.perform(post(BASE + "/create").with(csrf())
                        .param("requisitionId", "101").param("action", JobPostingValidator.ACTION_PUBLISH))
                .andExpect(redirectedUrl(BASE + "/56"))
                .andExpect(flash().attribute("successMessage", "Đã đăng tin. Ứng viên có thể nộp hồ sơ ngay."));
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("POST sửa (action=publish) -> Gọi updateAndPublishJobPosting, về chi tiết")
    void updateJobPosting_shouldCallPublish_whenActionIsPublish() throws Exception {
        mvc.perform(post(BASE + "/9/edit").with(csrf())
                        .param("jobPostingId", "9").param("requisitionId", "101")
                        .param("action", JobPostingValidator.ACTION_PUBLISH))
                .andExpect(redirectedUrl(BASE + "/9"));

        verify(service).updateAndPublishJobPosting(eq(9), any(JobPostingRequest.class), eq(hrUser));
        verify(service, never()).updateJobPosting(anyInt(), any(), any());
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("POST thiếu CSRF token -> 403, không gọi service")
    void createJobPosting_shouldReturnForbidden_whenCsrfTokenIsMissing() throws Exception {
        mvc.perform(post(BASE + "/create").param("requisitionId", "101")).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    // =========================================================================
    // Trang chi tiết
    // =========================================================================

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("GET chi tiết -> Dựng đủ phần (dải hạn nộp, nút, hồ sơ, lịch sử); có talent pool thì đếm đề xuất")
    void showJobPostingDetail_shouldRenderAllSections() throws Exception {
        when(service.getInternalJobPostingDetail(eq(30), any(User.class)))
                .thenReturn(publishedDetail().talentPoolVisible(true).suggestionsLinkLabel("Ứng viên được HM đề xuất")
                        .recommendationsLinkLabel("Gợi ý đã gửi cho HM").build());
        when(talentPoolService.pendingRecommendationCount(30)).thenReturn(2L);

        mvc.perform(get(BASE + "/30"))
                .andExpect(status().isOk())
                .andExpect(view().name("job-postings/detail"))
                .andExpect(model().attribute("hrRecommendationCount", 2L))
                .andExpect(content().string(allOf(
                        containsString("Còn 5 ngày là hết hạn nộp hồ sơ."),
                        containsString("Tạm dừng nhận hồ sơ"),
                        containsString("Trần Quốc Bảo"),
                        containsString("Xem tất cả 14 hồ sơ"),
                        containsString("Lịch sử"),
                        containsString("Gợi ý đã gửi cho HM"))));
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("GET chi tiết khi không thấy talent pool (Admin / Director) -> Không đếm đề xuất")
    void showJobPostingDetail_shouldSkipTalentPool_whenNotVisible() throws Exception {
        when(service.getInternalJobPostingDetail(eq(30), any(User.class))).thenReturn(publishedDetail().build());

        mvc.perform(get(BASE + "/30"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("hrRecommendationCount"));
        verifyNoInteractions(talentPoolService);
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("GET chi tiết id không tồn tại -> 404")
    void showJobPostingDetail_shouldReturnNotFound_whenIdDoesNotExist() throws Exception {
        when(service.getInternalJobPostingDetail(eq(404), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy tin tuyển dụng."));

        mvc.perform(get(BASE + "/404")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("GET chi tiết khi service chặn quyền (Director mở tin nháp) -> 403")
    void showJobPostingDetail_shouldReturnForbidden_whenServiceDenies() throws Exception {
        when(service.getInternalJobPostingDetail(eq(33), any(User.class)))
                .thenThrow(new AccessDeniedException("Bạn không có quyền xem tin tuyển dụng này."));

        mvc.perform(get(BASE + "/33")).andExpect(status().isForbidden());
    }

    // =========================================================================
    // Nút chuyển trạng thái
    // =========================================================================

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("POST xóa -> Về danh sách, flash 'successMessage'")
    void deleteJobPosting_shouldRedirectToList_whenDeleted() throws Exception {
        mvc.perform(post(BASE + "/delete/99").with(csrf()))
                .andExpect(redirectedUrl(BASE))
                .andExpect(flash().attribute("successMessage", "Đã xóa tin tuyển dụng thành công."));
        verify(service).deleteJobPosting(99, hrUser);
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("POST đăng tin bị lỗi nghiệp vụ -> Về chi tiết, flash 'errorMessage'")
    void publishJobPosting_shouldFlashError_whenServiceRejects() throws Exception {
        doThrow(new JobPostingValidationException("Hạn nộp đã qua. Hãy cập nhật tin trước khi đăng."))
                .when(service).publishJobPosting(20, hrUser);

        mvc.perform(post(BASE + "/20/publish").with(csrf()))
                .andExpect(redirectedUrl(BASE + "/20"))
                .andExpect(flash().attribute("errorMessage", "Hạn nộp đã qua. Hãy cập nhật tin trước khi đăng."));
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("POST đóng tin thành công -> Gửi email đóng đợt; đóng thất bại -> Không gửi email")
    void closeJobPosting_shouldSendEmailOnlyAfterSuccessfulClose() throws Exception {
        mvc.perform(post(BASE + "/28/close").with(csrf())).andExpect(redirectedUrl(BASE + "/28"));
        verify(recruitmentEmailService).sendJobClosure(28);

        doThrow(new TalentPoolException("Còn Offer chưa kết thúc.")).when(service).closeJobPosting(29, hrUser);
        mvc.perform(post(BASE + "/29/close").with(csrf()))
                .andExpect(flash().attribute("errorMessage", "Còn Offer chưa kết thúc."));
        verify(recruitmentEmailService, never()).sendJobClosure(29);
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = "HR")
    @DisplayName("Controller không tự quyết định quyền: luôn truyền người dùng từ phiên (access.actor) cho service")
    void pauseJobPosting_shouldPassSessionUserToService() throws Exception {
        mvc.perform(post(BASE + "/26/pause").with(csrf())).andExpect(redirectedUrl(BASE + "/26"));

        verify(service).pauseJobPosting(26, hrUser);
    }
}
