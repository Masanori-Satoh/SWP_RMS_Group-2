package com.group2.rms.user.controller;

import com.group2.rms.admin.controller.AuditLogController;
import com.group2.rms.admin.service.AuditLogService;
import com.group2.rms.auth.controller.AuthController;
import com.group2.rms.auth.controller.RegistrationController;
import com.group2.rms.auth.service.RegistrationVerificationService;
import com.group2.rms.candidate.talentpool.controller.TalentPoolController;
import com.group2.rms.candidate.talentpool.service.RecruitmentEmailService;
import com.group2.rms.candidate.talentpool.service.TalentPoolService;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.exception.ModuleRequestExceptionResolver;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.exception.AccountOperationException;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.AccountListService;
import com.group2.rms.user.service.AccountManagementService;
import com.group2.rms.user.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real MVC/security/Thymeleaf; mocked business dependencies, no database writes. */
@WebMvcTest({AccountController.class, CandidateAccountController.class, DepartmentController.class,
        TalentPoolController.class, AuditLogController.class, RegistrationController.class, AuthController.class,
        ModuleErrorMessageWebTests.OtherModuleController.class})
@Import({SecurityConfig.class, DatabaseUserDetailsService.class, ModuleRequestExceptionResolver.class,
        ModuleErrorMessageWebTests.OtherModuleController.class})
class ModuleErrorMessageWebTests {
    @Autowired MockMvc mvc;
    @MockitoBean AccountListService accounts;
    @MockitoBean AccountManagementService management;
    @MockitoBean DepartmentService departments;
    @MockitoBean TalentPoolService pool;
    @MockitoBean RecruitmentEmailService email;
    @MockitoBean AuditLogService audit;
    @MockitoBean RegistrationVerificationService registration;
    @MockitoBean UserRepository users;

    private RequestPostProcessor as(String role) {
        when(users.findByUsernameIgnoreCase("viewer")).thenReturn(Optional.of(User.builder()
                .userId(7).username("viewer").fullName("Người dùng thử").accountStatus("Active")
                .role(Role.builder().roleName(role).build()).build()));
        return user("viewer").authorities(() -> RoleAuthorities.fromRoleName(role));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/accounts/77/activate", "/admin/accounts/77/deactivate",
            "/admin/candidate-accounts/77/activate", "/admin/candidate-accounts/77/deactivate"})
    void staleOrWrongGroupAccountReturnsListMessage(String url) throws Exception {
        var failure = new AccountOperationException("Tài khoản không thuộc nhóm này.");
        doThrow(failure).when(accounts).activate(77);
        doThrow(failure).when(accounts).deactivate(77);
        doThrow(failure).when(accounts).activateCandidate(77);
        doThrow(failure).when(accounts).deactivateCandidate(77);
        String destination = url.contains("candidate-accounts") ? "/admin/candidate-accounts" : "/admin/accounts";
        mvc.perform(post(url).with(as("System Admin")).with(csrf()))
                .andExpect(status().isSeeOther()).andExpect(redirectedUrl(destination))
                .andExpect(flash().attribute("errorMessage", failure.getMessage()));
    }

    @Test
    void invalidAccountStateJsonKeepsConflictStatus() throws Exception {
        doThrow(new AccountOperationException("Trạng thái tài khoản không hợp lệ.", "ACCOUNT_STATUS_INVALID"))
                .when(accounts).activate(77);
        mvc.perform(post("/admin/accounts/77/activate").with(as("System Admin")).with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Trạng thái tài khoản không hợp lệ."));
    }

    @Test
    void missingAccountEditReturnsListAndMessageIsRendered() throws Exception {
        when(management.findForEdit(77)).thenThrow(new AccountOperationException("Không tìm thấy tài khoản."));
        var result = mvc.perform(get("/admin/accounts/77/edit").with(as("System Admin")))
                .andExpect(status().isSeeOther()).andExpect(redirectedUrl("/admin/accounts")).andReturn();
        when(accounts.findAccounts(any(), any(), any(), any(), any(), anyInt())).thenReturn(Page.empty());
        when(accounts.findDepartments()).thenReturn(List.of());
        when(accounts.findRoles()).thenReturn(List.of());
        mvc.perform(get("/admin/accounts").with(as("System Admin")).flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Không tìm thấy tài khoản.")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/departments/77/activate", "/admin/departments/77/deactivate", "/admin/departments/77"})
    void staleDepartmentPostReturnsList(String url) throws Exception {
        var missing = new ResourceNotFoundException("Không tìm thấy phòng ban.");
        doThrow(missing).when(departments).activate(77);
        doThrow(missing).when(departments).deactivate(77);
        doThrow(missing).when(departments).update(eq(77), any());
        mvc.perform(post(url).with(as("System Admin")).with(csrf()).param("departmentName", "Kỹ thuật"))
                .andExpect(status().isSeeOther()).andExpect(redirectedUrl("/admin/departments"))
                .andExpect(flash().attribute("errorMessage", missing.getMessage()));
    }

    @Test
    void missingDepartmentEditReturnsList() throws Exception {
        when(departments.findForEdit(77)).thenThrow(new ResourceNotFoundException("Không tìm thấy phòng ban."));
        mvc.perform(get("/admin/departments/77/edit").with(as("System Admin")))
                .andExpect(status().isSeeOther()).andExpect(redirectedUrl("/admin/departments"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"from=abc", "actorId=x", "page=abc"})
    void malformedAuditFiltersReturnCleanListWithoutQueryLoop(String query) throws Exception {
        mvc.perform(get("/admin/audit-logs?" + query).with(as("System Admin")))
                .andExpect(status().isSeeOther()).andExpect(redirectedUrl("/admin/audit-logs"))
                .andExpect(flash().attributeExists("errorMessage"));
        verifyNoInteractions(audit);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/accounts/abc/edit", "/admin/departments/abc/edit", "/admin/accounts?roleId=abc"})
    void malformedIdsReturnOwnList(String url) throws Exception {
        mvc.perform(get(url).with(as("System Admin"))).andExpect(status().isSeeOther())
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void markingForbiddenApplicationReturnsMessageWithoutRedirectingToForbiddenDetail() throws Exception {
        doThrow(new AccessDeniedException("Bạn không có quyền xem hồ sơ này.")).when(pool).markPotential(77);
        mvc.perform(post("/talent-pool/applications/77/mark").with(as("Hiring Manager")).with(csrf()))
                .andExpect(status().isSeeOther()).andExpect(redirectedUrl("/talent-pool"))
                .andExpect(flash().attribute("errorMessage", "Bạn không có quyền xem hồ sơ này."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"suggest", "recommend", "selection-review"})
    void talentPoolAjaxForbiddenUsesJsonWithSpecificMessage(String action) throws Exception {
        var denied = new AccessDeniedException("Tin tuyển dụng không thuộc phạm vi phòng ban của bạn.");
        doThrow(denied).when(pool).suggest(eq(77), any());
        doThrow(denied).when(pool).recommend(eq(77), any());
        when(pool.reviewSelection(eq(77), any())).thenThrow(denied);
        mvc.perform(post("/talent-pool/jobs/77/" + action).with(as("suggest".equals(action) ? "Hiring Manager" : "HR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sourceApplicationIds\":[1]}"))
                .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.message").value(denied.getMessage()));
    }

    @Test
    void missingBookmarkReturnsJson404EvenWithoutJsonHeaders() throws Exception {
        when(pool.bookmark(77)).thenThrow(new ResourceNotFoundException("Không tìm thấy hồ sơ."));
        mvc.perform(get("/talent-pool/applications/77/bookmark").with(as("HR")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy hồ sơ."));
    }

    @Test
    void staleRecommendationReturnsSafeList() throws Exception {
        when(pool.recommendationForDecision(77)).thenThrow(new ResourceNotFoundException("Không tìm thấy gợi ý."));
        mvc.perform(post("/talent-pool/recommendations/77/decision").with(as("Hiring Manager")).with(csrf())
                        .param("decision", "Pass"))
                .andExpect(status().isSeeOther()).andExpect(redirectedUrl("/talent-pool"))
                .andExpect(flash().attribute("errorMessage", "Không tìm thấy gợi ý."));
    }

    @Test
    void editingAnotherUsersNoteShowsSpecificReason() throws Exception {
        doThrow(new AccessDeniedException("Bạn chỉ có thể sửa hoặc xóa ghi chú của mình."))
                .when(pool).updateNote(eq(77), eq(9), any());
        mvc.perform(post("/talent-pool/77/notes/9/edit").with(as("HR")).with(csrf()).param("content", "Ghi chú"))
                .andExpect(status().isSeeOther()).andExpect(flash().attribute("errorMessage",
                        "Bạn chỉ có thể sửa hoặc xóa ghi chú của mình."));
    }

    @Test
    void wrongContentTypeOnJsonRouteReturnsJson415BeforeHandlerSelection() throws Exception {
        mvc.perform(post("/talent-pool/jobs/77/suggest").with(as("Hiring Manager")).with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("sourceApplicationIds", "1"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(pool);
    }

    @Test
    void malformedJsonAndPathIdReturnJson400() throws Exception {
        mvc.perform(post("/talent-pool/jobs/77/suggest").with(as("Hiring Manager")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(post("/talent-pool/jobs/abc/suggest").with(as("Hiring Manager")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sourceApplicationIds\":[1]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void wrongMethodRedirectsWithoutExecutingMutation() throws Exception {
        mvc.perform(get("/admin/accounts/77/deactivate").with(as("System Admin")))
                .andExpect(status().isSeeOther()).andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attributeExists("errorMessage"));
        verifyNoInteractions(accounts);
    }

    @Test
    void uploadTooLargeShowsMessageOnModuleScreen() throws Exception {
        doThrow(new MaxUploadSizeExceededException(10)).when(pool).markPotential(77);
        mvc.perform(post("/talent-pool/applications/77/mark").with(as("HR")).with(csrf()))
                .andExpect(status().isSeeOther()).andExpect(flash().attribute("errorMessage",
                        "Tệp tải lên vượt quá dung lượng cho phép. Vui lòng chọn tệp nhỏ hơn."));
    }

    @Test
    void systemFailureStillGoesThroughGlobalErrorPage() throws Exception {
        doThrow(new IllegalStateException("internal failure must not leak")).when(pool).markPotential(77);
        mvc.perform(post("/talent-pool/applications/77/mark").with(as("HR")).with(csrf()))
                .andExpect(status().isInternalServerError()).andExpect(view().name("error/500"));
    }

    @Test
    void otherModuleStillUsesGlobal404And400Pages() throws Exception {
        mvc.perform(get("/offers/error-boundary").with(as("HR")))
                .andExpect(status().isNotFound()).andExpect(view().name("error/404"));
        mvc.perform(get("/offers/error-boundary?number=x").with(as("HR")))
                .andExpect(status().isBadRequest()).andExpect(view().name("error/400"));
    }

    @Test
    void securityFiltersStillDenyWrongRoleAndMissingCsrf() throws Exception {
        mvc.perform(post("/admin/accounts/77/deactivate").with(as("HR")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/accounts/77/deactivate").with(as("System Admin")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(accounts);
    }

    @Test
    void sharedNotesAndBookmarkRemovalReturnJsonForMissingData() throws Exception {
        when(pool.detail(77)).thenThrow(new ResourceNotFoundException("Không tìm thấy ứng viên."));
        when(pool.removeBookmark(77)).thenThrow(new ResourceNotFoundException("Không tìm thấy hồ sơ."));
        mvc.perform(get("/talent-pool/77/notes").with(as("HR")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(post("/talent-pool/applications/77/bookmark/remove").with(as("HR")).with(csrf()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void contextPathAndUntrustedRefererDoNotChangeSafeDestination() throws Exception {
        doThrow(new AccountOperationException("Không tìm thấy tài khoản.")).when(accounts).deactivate(77);
        mvc.perform(post("/rms/admin/accounts/77/deactivate").contextPath("/rms")
                        .header("Referer", "https://untrusted.example/return").with(as("System Admin")).with(csrf()))
                .andExpect(status().isSeeOther()).andExpect(redirectedUrl("/rms/admin/accounts"))
                .andExpect(flash().attribute("errorMessage", "Không tìm thấy tài khoản."));
    }

    @Controller
    static class OtherModuleController {
        @GetMapping("/offers/error-boundary")
        String missing(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int number) {
            throw new ResourceNotFoundException("Không tìm thấy dữ liệu module khác.");
        }
    }
}
