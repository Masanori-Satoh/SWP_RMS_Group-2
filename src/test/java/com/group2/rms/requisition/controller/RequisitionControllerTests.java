package com.group2.rms.requisition.controller;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.dto.FilterOption;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.requisition.dto.RequisitionFilterOptions;
import com.group2.rms.requisition.dto.RequisitionListResponse;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.dto.RequisitionSearchRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaResponse;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.service.RequisitionListService;
import com.group2.rms.requisition.service.RequisitionService;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.Department;
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
import org.springframework.dao.DataIntegrityViolationException;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * CONTROLLER TEST (@WebMvcTest + MockMvc): RequisitionController.
 * Kiểm tra URL → view / redirect / flash message, cách hiện lỗi theo ô, bảo mật (403, CSRF, phiên hết hạn).
 * Service, quyền, validator đều là mock: nghiệp vụ đã được kiểm ở RequisitionServiceTests.
 * SecurityConfig và bộ lọc phiên (AccountSessionGuardFilter) là THẬT, nên cần UserRepository trả tài khoản.
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện, Arrange / Act / Assert.
 */
@WebMvcTest(controllers = RequisitionController.class)
@Import({ SecurityConfig.class, DatabaseUserDetailsService.class })
class RequisitionControllerTests {

        @Autowired
        private MockMvc mvc;

        @MockitoBean
        private RequisitionService service;
        @MockitoBean
        private RequisitionListService listService;
        @MockitoBean
        private RequisitionAccess access;
        @MockitoBean
        private RequisitionValidator validator;
        @MockitoBean
        private DepartmentRepository departments;
        @MockitoBean
        private UserRepository userRepository;

        private User hiringManager;

        @BeforeEach
        void setUp() {
                Department department = Department.builder().departmentId(1).departmentName("IT").build();
                hiringManager = User.builder().userId(10).username("hm").fullName("Nguyen Van A").accountStatus("Active")
                                .role(Role.builder().roleId(1).roleName(RequisitionAccess.ROLE_HIRING_MANAGER).build())
                                .department(department).build();

                lenient().when(access.actor()).thenReturn(hiringManager);
                lenient().when(access.canCreate(any())).thenReturn(true);
                lenient().when(departments.findAll()).thenReturn(List.of(department));
                lenient().when(validator.earliestStartDate()).thenReturn(LocalDate.of(2026, 10, 18));
                lenient().when(validator.latestStartDate()).thenReturn(LocalDate.of(2027, 10, 18));
                // Bộ lọc phiên thật kiểm tra tài khoản còn Active và đúng role ở mỗi request
                when(userRepository.findByUsernameIgnoreCase("hm")).thenReturn(Optional.of(hiringManager));
                when(userRepository.findByUsernameIgnoreCase("director")).thenReturn(Optional.of(User.builder()
                                .userId(20).username("director").accountStatus("Active")
                                .role(Role.builder().roleName(RequisitionAccess.ROLE_DIRECTOR).build()).build()));
                when(userRepository.findByUsernameIgnoreCase("candidate")).thenReturn(Optional.of(User.builder()
                                .userId(30).username("candidate").accountStatus("Active")
                                .role(Role.builder().roleName("Candidate").build()).build()));
        }

        // ----- Hàm hỗ trợ -----

        private RequisitionListResponse listWith(RequisitionResponse row, RequisitionFilterOptions options) {
                RequisitionSearchRequest filter = new RequisitionSearchRequest(
                                "", List.of(), null, null, null, null, null, "newest", 0, 10);
                return new RequisitionListResponse(new PageImpl<>(List.of(row)), filter, options, List.of());
        }

        // =========================================================================
        // GET: danh sách, form, chi tiết
        // =========================================================================

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("GET /requisitions -> View danh sách, có model list + filterLinks, sáng menu Yêu cầu")
        void list_shouldRenderListView_whenManagerOpensPage() throws Exception {
                // Arrange
                RequisitionResponse row = RequisitionResponse.builder().requisitionId(1).title("Backend Engineer")
                                .approvalStatus(RequisitionAccess.STATUS_DRAFT).createdAt(LocalDateTime.now())
                                .editable(true).deletable(true).build();
                RequisitionFilterOptions options = new RequisitionFilterOptions(true, true, false, false, true,
                                List.of(new FilterOption("Draft", "Bản nháp")), List.of(new FilterOption("none", "Chưa có tin")),
                                List.of(), List.of(), List.of(new FilterOption("newest", "Mới nhất trước")));
                when(listService.getListPage(any())).thenReturn(listWith(row, options));

                // Act + Assert
                mvc.perform(get("/requisitions"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("requisitions/list"))
                                .andExpect(model().attributeExists("list", "filterLinks"))
                                .andExpect(model().attribute("activeMenu", "requisitions"));
        }

        @Test
        @WithMockUser(username = "director", roles = "DIRECTOR")
        @DisplayName("GET /requisitions -> Tin không xem được: hiện badge nhưng không có link vào trang tin (tránh 403)")
        void list_shouldNotLinkToPosting_whenPostingIsNotViewable() throws Exception {
                // Arrange
                RequisitionResponse row = RequisitionResponse.builder().requisitionId(5).title("Data Engineer")
                                .approvalStatus(RequisitionAccess.STATUS_APPROVED).statusLabel("Đã duyệt").statusTone("success")
                                .jobPostingId(16).postingStatus("Paused").postingLabel("Tạm dừng").postingTone("warning")
                                .postingViewable(false).createdAt(LocalDateTime.now()).build();
                RequisitionFilterOptions options = new RequisitionFilterOptions(true, false, true, true, false,
                                List.of(), List.of(), List.of(), List.of(), List.of(new FilterOption("newest", "Mới nhất trước")));
                when(listService.getListPage(any())).thenReturn(listWith(row, options));

                // Act + Assert
                mvc.perform(get("/requisitions"))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("Tạm dừng")))
                                .andExpect(content().string(not(containsString("/internal/job-postings/16"))));
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("GET /requisitions/create -> Form tạo mới, khoảng ngày bắt đầu lấy từ validator")
        void createForm_shouldRenderEmptyForm_whenViewerCanCreate() throws Exception {
                mvc.perform(get("/requisitions/create"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("requisitions/form"))
                                .andExpect(model().attribute("isEdit", false))
                                .andExpect(model().attribute("startDateMin", LocalDate.of(2026, 10, 18)))
                                .andExpect(model().attribute("startDateMax", LocalDate.of(2027, 10, 18)))
                                .andExpect(model().attributeExists("requisitionDto"));
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("GET /requisitions/create khi không có quyền tạo (gõ thẳng URL) -> 403")
        void createForm_shouldReturnForbidden_whenViewerCannotCreate() throws Exception {
                // Arrange
                doThrow(new AccessDeniedException("no")).when(access).requireCreate(hiringManager);

                // Act + Assert
                mvc.perform(get("/requisitions/create")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("GET /requisitions/{id} -> View chi tiết với model req, đọc được trường của record (tiêu chí bắt buộc)")
        void detail_shouldRenderDetailView_whenRequisitionExists() throws Exception {
                // Arrange
                ScreeningCriteriaResponse mandatory = ScreeningCriteriaResponse.builder().criteriaId(1)
                                .criteriaName("Java").criteriaTypeLabel("Kỹ năng").requiredValue("2 năm")
                                .weight(new BigDecimal("100")).isMandatory(true).build();
                RequisitionResponse detail = RequisitionResponse.builder().requisitionId(1).requisitionCode("REQ-0001")
                                .recruitmentRound(1).title("Backend Engineer").approvalStatus(RequisitionAccess.STATUS_DRAFT)
                                .screeningCriteria(List.of(mandatory)).criteriaTotalWeight(new BigDecimal("100"))
                                .approvals(List.of()).activityLog(List.of()).build();
                when(service.getById(1)).thenReturn(detail);

                // Act + Assert
                mvc.perform(get("/requisitions/1"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("requisitions/detail"))
                                .andExpect(model().attributeExists("req"))
                                .andExpect(content().string(containsString("Bắt buộc")));
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("GET /requisitions/{id} với id không tồn tại -> 404")
        void detail_shouldReturnNotFound_whenIdDoesNotExist() throws Exception {
                // Arrange
                when(service.getById(404)).thenThrow(new ResourceNotFoundException("Không tìm thấy yêu cầu tuyển dụng."));

                // Act + Assert
                mvc.perform(get("/requisitions/404")).andExpect(status().isNotFound());
        }

        // =========================================================================
        // POST: tạo, sửa, xóa
        // =========================================================================

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST /requisitions/create (lưu nháp) -> Gọi service, chuyển tới chi tiết, flash 'Đã lưu bản nháp'")
        void create_shouldRedirectToDetail_whenDraftIsSaved() throws Exception {
                // Arrange
                when(service.createRequisition(any(RequisitionRequest.class))).thenReturn(10);

                // Act + Assert
                mvc.perform(post("/requisitions/create").with(csrf())
                                .param("action", RequisitionValidator.ACTION_DRAFT).param("title", "New Dev"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/requisitions/10"))
                                .andExpect(flash().attribute("successMessage", "Đã lưu bản nháp thành công."));
                verify(service).createRequisition(any(RequisitionRequest.class));
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST /requisitions/create -> Lương '20.000.000' (dấu chấm hàng nghìn) và '30000000' đều đọc đúng")
        void create_shouldParseSalary_whenSalaryHasDotSeparators() throws Exception {
                // Arrange
                when(service.createRequisition(any(RequisitionRequest.class))).thenReturn(10);

                // Act
                mvc.perform(post("/requisitions/create").with(csrf())
                                .param("action", RequisitionValidator.ACTION_DRAFT).param("title", "New Dev")
                                .param("minSalary", "20.000.000").param("maxSalary", "30000000"))
                                .andExpect(status().is3xxRedirection());

                // Assert
                ArgumentCaptor<RequisitionRequest> captor = ArgumentCaptor.forClass(RequisitionRequest.class);
                verify(service).createRequisition(captor.capture());
                assertEquals(0, new BigDecimal("20000000").compareTo(captor.getValue().getMinSalary()));
                assertEquals(0, new BigDecimal("30000000").compareTo(captor.getValue().getMaxSalary()));
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST /requisitions/create -> Lương gõ chữ (sai định dạng) thì báo lỗi ở ô lương, không gọi service")
        void create_shouldShowFieldError_whenSalaryIsNotANumber() throws Exception {
                mvc.perform(post("/requisitions/create").with(csrf())
                                .param("action", RequisitionValidator.ACTION_DRAFT).param("title", "New Dev")
                                .param("minSalary", "hai mươi triệu"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("requisitions/form"))
                                .andExpect(model().attributeHasFieldErrors("requisitionDto", "minSalary"));
                verify(service, never()).createRequisition(any());
        }

        // ----- Bean Validation (@Valid, AG §2.4): chặn trước khi gọi service -----

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST /requisitions/create -> Tên vị trí dài TITLE_CHAR_LIMIT + 1 thì báo lỗi ở ô tên, không gọi service")
        void create_shouldShowFieldError_whenTitleIsTooLong() throws Exception {
                mvc.perform(post("/requisitions/create").with(csrf())
                                .param("action", RequisitionValidator.ACTION_DRAFT)
                                .param("title", "a".repeat(RequisitionValidator.TITLE_CHAR_LIMIT + 1)))
                                .andExpect(status().isOk())
                                .andExpect(view().name("requisitions/form"))
                                .andExpect(model().attributeHasFieldErrorCode("requisitionDto", "title", "Size"));
                verify(service, never()).createRequisition(any());
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST /requisitions/create -> Số lượng POSITIONS_MAX + 1, lương quá 1 tỷ, trọng số 150 -> Lỗi đúng từng ô, không gọi service")
        void create_shouldShowFieldErrors_whenNumbersAreOutOfRange() throws Exception {
                mvc.perform(post("/requisitions/create").with(csrf())
                                .param("action", RequisitionValidator.ACTION_DRAFT).param("title", "New Dev")
                                .param("numberOfPositions", String.valueOf(RequisitionValidator.POSITIONS_MAX + 1))
                                .param("maxSalary", "1000000001")
                                .param("screeningCriteria[0].criteriaName", "Java")
                                .param("screeningCriteria[0].weight", "150"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("requisitions/form"))
                                .andExpect(model().attributeHasFieldErrors("requisitionDto",
                                                "numberOfPositions", "maxSalary", "screeningCriteria[0].weight"));
                verify(service, never()).createRequisition(any());
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST /requisitions/create -> Mô tả nhiều dòng (\\r\\n) đủ giới hạn sau khi chuẩn hóa -> Không bị chặn oan, vẫn gọi service")
        void create_shouldNotRejectMultilineText_whenLimitIsReachedOnlyBecauseOfCrLf() throws Exception {
                // Arrange: gửi lên 2.998 ký tự, sau khi đổi \r\n -> \n chỉ còn 1.999 (< TEXT_FIELD_CHAR_LIMIT)
                when(service.createRequisition(any(RequisitionRequest.class))).thenReturn(10);
                String description = "a\r\n".repeat(999) + "a";

                // Act + Assert
                mvc.perform(post("/requisitions/create").with(csrf())
                                .param("action", RequisitionValidator.ACTION_DRAFT).param("title", "New Dev")
                                .param("jobDescription", description))
                                .andExpect(status().is3xxRedirection());
                verify(service).createRequisition(any(RequisitionRequest.class));
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST /requisitions/edit/{id} -> Thời gian thử việc PROBATION_MAX_DAYS + 1 thì báo lỗi ở ô thử việc, không gọi service")
        void update_shouldShowFieldError_whenProbationIsOutOfRange() throws Exception {
                mvc.perform(post("/requisitions/edit/10").with(csrf())
                                .param("action", RequisitionValidator.ACTION_DRAFT).param("title", "Updated Title")
                                .param("probationDays", String.valueOf(RequisitionValidator.PROBATION_MAX_DAYS + 1)))
                                .andExpect(status().isOk())
                                .andExpect(view().name("requisitions/form"))
                                .andExpect(model().attributeHasFieldErrors("requisitionDto", "probationDays"));
                verify(service, never()).updateRequisition(any(), any());
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST /requisitions/create -> Gửi thêm trường lạ (hiringManager.userId) thì bị bỏ qua")
        void create_shouldIgnoreUnknownField_whenFormIsTampered() throws Exception {
                // Arrange
                when(service.createRequisition(any(RequisitionRequest.class))).thenReturn(10);

                // Act + Assert: không lỗi bind, service vẫn được gọi bình thường
                mvc.perform(post("/requisitions/create").with(csrf())
                                .param("action", RequisitionValidator.ACTION_DRAFT).param("title", "New Dev")
                                .param("recruitmentRound", "99"))
                                .andExpect(status().is3xxRedirection());
                ArgumentCaptor<RequisitionRequest> captor = ArgumentCaptor.forClass(RequisitionRequest.class);
                verify(service).createRequisition(captor.capture());
                assertEquals(null, captor.getValue().getRecruitmentRound(), "Số đợt không nằm trong whitelist của form");
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST /requisitions/create -> Lỗi nghiệp vụ thì trả lại form và hiện lỗi đúng ô")
        void create_shouldReturnFormWithFieldError_whenServiceRejectsData() throws Exception {
                // Arrange
                doThrow(new RequisitionValidationException(Map.of("title", "Tên vị trí không được để trống.")))
                                .when(service).createRequisition(any());

                // Act + Assert
                mvc.perform(post("/requisitions/create").with(csrf()).param("action", RequisitionValidator.ACTION_SUBMIT))
                                .andExpect(status().isOk())
                                .andExpect(view().name("requisitions/form"))
                                .andExpect(model().attributeHasFieldErrors("requisitionDto", "title"));
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST /requisitions/create -> DB báo trùng (DataIntegrityViolation) thì trả lại form kèm lỗi chung")
        void create_shouldReturnFormWithGlobalError_whenDatabaseRejectsData() throws Exception {
                // Arrange
                doThrow(new DataIntegrityViolationException("UX")).when(service).createRequisition(any());

                // Act + Assert
                mvc.perform(post("/requisitions/create").with(csrf())
                                .param("action", RequisitionValidator.ACTION_DRAFT).param("title", "New Dev"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("requisitions/form"))
                                .andExpect(model().hasErrors());
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST /requisitions/edit/{id} (gửi duyệt) -> Chuyển về chi tiết, flash 'đã gửi lên Giám đốc'")
        void update_shouldRedirectToDetail_whenManagerSubmits() throws Exception {
                mvc.perform(post("/requisitions/edit/10").with(csrf())
                                .param("action", RequisitionValidator.ACTION_SUBMIT).param("title", "Updated Title"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/requisitions/10"))
                                .andExpect(flash().attribute("successMessage", "Yêu cầu đã được gửi lên Giám đốc phê duyệt."));
                verify(service).updateRequisition(eq(10), any(RequisitionRequest.class));
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST /requisitions/delete/{id} -> Xóa xong chuyển về danh sách")
        void delete_shouldRedirectToList_whenDeleted() throws Exception {
                mvc.perform(post("/requisitions/delete/10").with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/requisitions"))
                                .andExpect(flash().attribute("successMessage", "Đã xóa yêu cầu tuyển dụng thành công."));
                verify(service).deleteRequisition(10);
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST /requisitions/delete/{id} -> Đã có tin tuyển dụng thì quay lại chi tiết kèm lỗi")
        void delete_shouldRedirectBackWithError_whenServiceRejects() throws Exception {
                // Arrange
                doThrow(new RequisitionValidationException(Map.of("action", "Không thể xóa.")))
                                .when(service).deleteRequisition(10);

                // Act + Assert
                mvc.perform(post("/requisitions/delete/10").with(csrf()))
                                .andExpect(redirectedUrl("/requisitions/10"))
                                .andExpect(flash().attributeExists("failureMessage"));
        }

        // =========================================================================
        // POST: duyệt, rút lại
        // =========================================================================

        @Test
        @WithMockUser(username = "director", roles = "DIRECTOR")
        @DisplayName("POST /requisitions/{id}/decision (approve) -> Gọi service với approved = true, flash thành công")
        void decide_shouldCallServiceWithApprove_whenDecisionIsApprove() throws Exception {
                mvc.perform(post("/requisitions/10/decision").with(csrf())
                                .param("decision", "approve").param("comment", "Looks good"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/requisitions/10"))
                                .andExpect(flash().attribute("successMessage", "Đã lưu quyết định thành công."));
                verify(service).decide(10, true, "Looks good");
        }

        @Test
        @WithMockUser(username = "director", roles = "DIRECTOR")
        @DisplayName("POST /requisitions/{id}/decision với giá trị lạ (sửa HTML) -> Không gọi service, báo chọn quyết định")
        void decide_shouldNotCallService_whenDecisionValueIsUnknown() throws Exception {
                mvc.perform(post("/requisitions/10/decision").with(csrf()).param("decision", "maybe"))
                                .andExpect(redirectedUrl("/requisitions/10"))
                                .andExpect(flash().attribute("failureMessage", "Vui lòng chọn quyết định phê duyệt."));
                verifyNoInteractions(service);
        }

        @Test
        @WithMockUser(username = "director", roles = "DIRECTOR")
        @DisplayName("POST /requisitions/{id}/decision (reject) thiếu lý do -> Flash lỗi và giữ lại ý kiến đã gõ")
        void decide_shouldKeepTypedComment_whenServiceRejectsDecision() throws Exception {
                // Arrange
                doThrow(new RequisitionValidationException(Map.of("comment", "Vui lòng nhập lý do để Hiring Manager chỉnh sửa.")))
                                .when(service).decide(10, false, "   ");

                // Act + Assert
                mvc.perform(post("/requisitions/10/decision").with(csrf())
                                .param("decision", "reject").param("comment", "   "))
                                .andExpect(redirectedUrl("/requisitions/10"))
                                .andExpect(flash().attributeExists("failureMessage"))
                                .andExpect(flash().attribute("decisionComment", "   "));
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST /requisitions/{id}/withdraw -> Rút lại xong chuyển về chi tiết")
        void withdraw_shouldRedirectToDetail_whenWithdrawn() throws Exception {
                mvc.perform(post("/requisitions/10/withdraw").with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/requisitions/10"))
                                .andExpect(flash().attribute("successMessage",
                                                "Đã rút lại yêu cầu thành công. Bạn có thể chỉnh sửa lại bản nháp."));
                verify(service).withdraw(10);
        }

        // =========================================================================
        // Bảo mật
        // =========================================================================

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("POST thiếu CSRF token -> 403, không gọi service")
        void create_shouldReturnForbidden_whenCsrfTokenIsMissing() throws Exception {
                mvc.perform(post("/requisitions/create").param("title", "No CSRF"))
                                .andExpect(status().isForbidden());
                verifyNoInteractions(service);
        }

        @Test
        @WithMockUser(username = "candidate", roles = "CANDIDATE")
        @DisplayName("Ứng viên gõ thẳng /requisitions -> 403 theo SecurityConfig")
        void list_shouldReturnForbidden_whenViewerIsCandidate() throws Exception {
                mvc.perform(get("/requisitions")).andExpect(status().isForbidden());
                verifyNoInteractions(service, listService);
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("Tài khoản bị khóa giữa phiên -> Phiên hết hạn trước khi vào controller")
        void list_shouldExpireSession_whenAccountBecomesInactive() throws Exception {
                hiringManager.setAccountStatus("Inactive");
                mvc.perform(get("/requisitions")).andExpect(redirectedUrl("/login?session-expired"));
                verifyNoInteractions(listService);
        }

        @Test
        @WithMockUser(username = "hm", roles = "HIRING_MANAGER")
        @DisplayName("Role bị đổi giữa phiên -> Phiên hết hạn trước khi vào controller")
        void list_shouldExpireSession_whenRoleChanges() throws Exception {
                hiringManager.setRole(Role.builder().roleName("Candidate").build());
                mvc.perform(get("/requisitions")).andExpect(redirectedUrl("/login?session-expired"));
                verifyNoInteractions(listService);
        }
}
