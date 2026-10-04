package com.group2.rms;

import com.group2.rms.auth.controller.AuthController;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.dashboard.*;
import com.group2.rms.dashboard.DashboardResponse.*;
import com.group2.rms.user.controller.*;
import com.group2.rms.user.dto.*;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.exception.DepartmentFieldException;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers={AuthController.class,DepartmentController.class,AccountController.class,CandidateAccountController.class,DashboardController.class})
@Import({SecurityConfig.class,DatabaseUserDetailsService.class})
class AdminDepartmentWebTests {
    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder encoder;
    @MockitoBean private UserRepository users;
    @MockitoBean private DepartmentService departments;
    @MockitoBean private AccountListService accounts;
    @MockitoBean private AccountManagementService management;
    @MockitoBean private DashboardService dashboard;

    @Test void listAndEditUseSharedVietnameseShellRealRoutesAndCsrf() throws Exception {
        setupDepartments(); var session=login("System Admin");
        var list=mvc.perform(get("/admin/departments").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Quản lý phòng ban")))
                .andExpect(content().string(containsString("/admin/departments/7/deactivate")))
                .andExpect(content().string(containsString("/admin/departments/8/activate")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("href=\"/requisitions\""))))
                .andExpect(content().string(containsString("&lt;script&gt;"))).andReturn();
        fixture("departments",list);
        fixture("new",mvc.perform(get("/admin/departments/new").session(session)).andExpect(status().isOk()).andReturn());
        fixture("edit",mvc.perform(get("/admin/departments/7/edit").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("department-manager"))).andReturn());
    }
    @Test void guestAndNonAdminCannotReadOrMutateDepartments() throws Exception {
        mvc.perform(get("/admin/departments")).andExpect(status().is3xxRedirection());
        for(String role:List.of("HR","Hiring Manager","Director","Interviewer","Candidate")) {
            var session=login(role); mvc.perform(get("/admin/departments").session(session)).andExpect(status().isForbidden());
            var token=csrf(session); mvc.perform(post("/admin/departments/7/deactivate").session(session)
                    .param(token.getParameterName(),token.getToken())).andExpect(status().isForbidden());
            mvc.perform(post("/admin/accounts/42/activate").session(session).param(token.getParameterName(),token.getToken())).andExpect(status().isForbidden());
            mvc.perform(post("/admin/candidate-accounts/62/activate").session(session).param(token.getParameterName(),token.getToken())).andExpect(status().isForbidden());
        }
        verifyNoInteractions(departments);
    }
    @Test void accountDepartmentPickerAllowsActiveAndOnlyOriginalInactiveMembership() throws Exception {
        var session=login("System Admin");
        when(accounts.findRoles()).thenReturn(List.of(Role.builder().roleId(2).roleName("HR").build()));
        when(accounts.findDepartments()).thenReturn(List.of(
                Department.builder().departmentId(7).departmentName("Phòng ban cũ").departmentStatus("Inactive").build(),
                Department.builder().departmentId(8).departmentName("Phòng ban khác").departmentStatus("Inactive").build(),
                Department.builder().departmentId(9).departmentName("Phòng ban hoạt động").build()));
        mvc.perform(get("/admin/accounts/new").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Phòng ban hoạt động")))
                .andExpect(content().string(not(containsString("Phòng ban cũ"))));
        when(management.findForEdit(42)).thenReturn(new AccountManagementService.AccountForEdit("stable","Name","same@example.test",null,2,7,"Active"));
        mvc.perform(get("/admin/accounts/42/edit").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Phòng ban cũ (đã vô hiệu hóa)")))
                .andExpect(content().string(not(containsString("Phòng ban khác"))));
    }
    @Test void validCreateAndUpdateReachServiceButMissingCsrfDoesNot() throws Exception {
        var session=login("System Admin"); var token=csrf(session);
        mvc.perform(post("/admin/departments").session(session).param("departmentName","Legal")) .andExpect(status().isForbidden());
        mvc.perform(post("/admin/departments").session(session).param(token.getParameterName(),token.getToken()).param("departmentName","Legal"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/departments"));
        mvc.perform(post("/admin/departments/7").session(session).param(token.getParameterName(),token.getToken())
                .param("departmentName","Product").param("managerId","21")) .andExpect(status().is3xxRedirection());
        verify(departments).create(new DepartmentRequest("Legal",null)); verify(departments).update(7,new DepartmentRequest("Product",21));
    }
    @Test void beanValidationRendersFieldErrorsWithoutCallingService() throws Exception {
        var session=login("System Admin"); var token=csrf(session);
        var result=mvc.perform(post("/admin/departments").session(session).param(token.getParameterName(),token.getToken())
                .param("departmentName"," ")).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","departmentName"))
                .andExpect(content().string(containsString("aria-invalid=\"true\""))).andReturn();
        fixture("validation",result); verify(departments,never()).create(any());
    }
    @Test void businessDuplicateUsesGlobalHandlerAndRestoresSubmittedFormOnGet() throws Exception {
        var session=login("System Admin"); var token=csrf(session); var form=new DepartmentRequest("Existing",null);
        when(departments.create(form)).thenThrow(new DepartmentFieldException("departmentName","Tên phòng ban đã được sử dụng.",null,form));
        var failure=mvc.perform(post("/admin/departments").session(session).param(token.getParameterName(),token.getToken()).param("departmentName","Existing"))
                .andExpect(status().isSeeOther()).andExpect(redirectedUrl("/admin/departments/new")).andReturn();
        mvc.perform(get("/admin/departments/new").session(session).flashAttrs(failure.getFlashMap())).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form","departmentName"))
                .andExpect(content().string(containsString("value=\"Existing\""))).andExpect(content().string(containsString("Tên phòng ban đã được sử dụng.")));
    }
    @Test void invalidManagerUsesGlobalHandlerAndEditBinding() throws Exception {
        setupDepartments(); var session=login("System Admin"); var token=csrf(session); var form=new DepartmentRequest("Sản phẩm",99);
        doThrow(new DepartmentFieldException("managerId","Trưởng phòng không hợp lệ.",7,form)).when(departments).update(7,form);
        var failure=mvc.perform(post("/admin/departments/7").session(session).param(token.getParameterName(),token.getToken())
                .param("departmentName","Sản phẩm").param("managerId","99")).andExpect(status().isSeeOther()).andReturn();
        mvc.perform(get("/admin/departments/7/edit").session(session).flashAttrs(failure.getFlashMap()))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","managerId"));
    }
    @Test void lifecyclePostsUseScopedServicesAndCsrf() throws Exception {
        var session=login("System Admin"); var token=csrf(session);
        for(String route:List.of("/admin/departments/7/deactivate","/admin/departments/7/activate","/admin/accounts/42/activate","/admin/candidate-accounts/62/activate")) {
            mvc.perform(post(route).session(session)).andExpect(status().isForbidden());
            mvc.perform(post(route).session(session).param(token.getParameterName(),token.getToken())).andExpect(status().is3xxRedirection());
        }
        verify(departments).deactivate(7); verify(departments).activate(7); verify(accounts).activate(42); verify(accounts).activateCandidate(62);
    }
    @Test void accountPagesOfferRestoreOnlyForInactiveAndPreserveLifecycle() throws Exception {
        var session=login("System Admin");
        when(accounts.findAccounts("",null,null,"","name",0)).thenReturn(new PageImpl<>(List.of(
                new AccountListService.AccountRow(41,"Nguyễn Minh An","an@example.test","an","HR","Sản phẩm","Active"),
                new AccountListService.AccountRow(42,"Trần Thanh Bình","binh@example.test","binh","HR","Sản phẩm","Inactive"),
                new AccountListService.AccountRow(43,"Lê Bảo","bao@example.test","bao","HR","Sản phẩm","Blocked"))));
        fixture("accounts",mvc.perform(get("/admin/accounts").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/accounts/42/activate")))
                .andExpect(content().string(not(containsString("/admin/accounts/43/activate")))).andReturn());
        when(accounts.findCandidateAccounts("","","name",0)).thenReturn(new PageImpl<>(List.of(
                new AccountListService.CandidateRow(61,"Ứng viên An","c61@example.test","c61","Active",LocalDateTime.now(),31),
                new AccountListService.CandidateRow(62,"Ứng viên Bình","c62@example.test","c62","Inactive",LocalDateTime.now(),32))));
        fixture("candidates",mvc.perform(get("/admin/candidate-accounts").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/candidate-accounts/62/activate"))).andReturn());
    }
    @Test void adminDashboardHasDepartmentsAndNoRequisitionWhileHrKeepsIt() throws Exception {
        when(dashboard.forUsername("admin-ui-System Admin")).thenReturn(new DashboardResponse("System Admin","Admin kiểm thử","Quản lý hệ thống",
                List.of(),List.of(),List.of(),List.of(),List.of(),List.of(new AccountSummary("Tài khoản nội bộ","/admin/accounts",3,1,1,1)),null,new DepartmentSummary(2,1,1)));
        fixture("dashboard",mvc.perform(get("/dashboard").session(login("System Admin"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"vi\"")))
                .andExpect(content().string(containsString("href=\"/admin/departments\"")))
                .andExpect(content().string(not(containsString("href=\"/requisitions\"")))).andReturn());
        when(dashboard.forUsername("admin-ui-HR")).thenReturn(new DashboardResponse("HR","HR","Existing scope",List.of(),List.of(),List.of(),List.of(),List.of()));
        mvc.perform(get("/dashboard").session(login("HR"))).andExpect(status().isOk()).andExpect(content().string(containsString("href=\"/requisitions\"")));
    }
    private void setupDepartments() {
        when(departments.findDepartments("","",0)).thenReturn(new PageImpl<>(List.of(
                new DepartmentResponse(7,"Sản phẩm",21,"Nguyễn Minh An","an","Active"),
                new DepartmentResponse(8,"Thiết kế <script>",null,null,null,"Inactive"))));
        when(departments.findForEdit(7)).thenReturn(new DepartmentResponse(7,"Sản phẩm",21,"Nguyễn Minh An","an","Active"));
        when(departments.managerChoices(7)).thenReturn(List.of(new DepartmentManagerResponse(21,"Nguyễn Minh An","an")));
    }
    private MockHttpSession login(String role) throws Exception {
        var username="admin-ui-"+role; var user=User.builder().userId(500).username(username).fullName("Fixture")
                .role(Role.builder().roleName(role).build()).passwordHash(encoder.encode("ui-password123")).accountStatus("Active").build();
        when(users.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(user));
        var page=mvc.perform(get("/login")).andReturn(); var token=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        return (MockHttpSession)mvc.perform(post("/login").session((MockHttpSession)page.getRequest().getSession())
                .param(token.getParameterName(),token.getToken()).param("username",username).param("password","ui-password123"))
                .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession();
    }
    private CsrfToken csrf(MockHttpSession session) throws Exception {
        var page=mvc.perform(get("/login").session(session)).andReturn(); return (CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
    }
    private void fixture(String name,MvcResult result) throws Exception {
        var path=Path.of("target","admin-department-preview",name); Files.createDirectories(path);
        Files.writeString(path.resolve("index.html"),result.getResponse().getContentAsString(StandardCharsets.UTF_8)
                .replaceAll("(name=\"_csrf\" value=\")[^\"]+","$1visual-fixture-only"),StandardCharsets.UTF_8);
    }
}
