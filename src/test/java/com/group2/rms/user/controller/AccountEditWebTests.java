package com.group2.rms.user.controller;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.AccountListService;
import com.group2.rms.user.service.AccountManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class AccountEditWebTests {
    @Autowired MockMvc mvc;
    @MockitoBean AccountListService accounts;
    @MockitoBean AccountManagementService management;
    @MockitoBean UserRepository users;

    @BeforeEach
    void setup() {
        when(management.findForEdit(42)).thenReturn(account("Active", 7, "0908901234"));
        when(accounts.findRoles()).thenReturn(List.of(
                Role.builder().roleId(1).roleName("System Admin").build(),
                Role.builder().roleId(2).roleName("HR").build(),
                Role.builder().roleId(5).roleName("Interviewer").build()));
        when(accounts.findDepartments()).thenReturn(List.of(
                Department.builder().departmentId(7).departmentName("Engineering").departmentStatus("Active").build(),
                Department.builder().departmentId(8).departmentName("Lưu trữ").departmentStatus("Inactive").build(),
                Department.builder().departmentId(9).departmentName("Phòng đã đóng khác").departmentStatus("Inactive").build()));
    }

    @Test
    void editShowsSavedAccountAndKeepsUsernameAndPasswordUneditable() throws Exception {
        String html = mvc.perform(get("/admin/accounts/42/edit").with(as("System Admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Thông tin tài khoản đang lưu\"")))
                .andExpect(content().string(containsString("Bùi Quốc Việt")))
                .andExpect(content().string(containsString("Đang hoạt động")))
                .andExpect(content().string(containsString("action=\"/admin/accounts/42\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("readonly")))
                .andExpect(content().string(not(containsString("name=\"username\""))))
                .andExpect(content().string(not(containsString("type=\"password\""))))
                .andReturn().getResponse().getContentAsString();
        fixture("active", html);
    }

    @Test
    void validationErrorKeepsDraftFieldsButSummaryUsesSavedData() throws Exception {
        String html = mvc.perform(post("/admin/accounts/42").with(as("System Admin")).with(csrf())
                        .param("fullName", "Tên vừa chỉnh sửa").param("email", "invalid-email")
                        .param("phoneNumber", "0908901234").param("roleId", "2")
                        .param("departmentId", "7").param("accountStatus", "Active"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "email"))
                .andExpect(model().attribute("form", hasProperty("fullName", is("Tên vừa chỉnh sửa"))))
                .andExpect(content().string(containsString("class=\"aef-summary-name\">Bùi Quốc Việt</h2>")))
                .andExpect(content().string(containsString("viet.bui@rms-tech.vn")))
                .andReturn().getResponse().getContentAsString();
        verify(management, never()).updateInternal(anyInt(), any());
        fixture("error", html);
    }

    @Test
    void inactiveAccountCanRetainItsInactiveDepartmentAndEmptyPhoneIsHonest() throws Exception {
        when(management.findForEdit(42)).thenReturn(account("Inactive", 8, "  "));
        String html = mvc.perform(get("/admin/accounts/42/edit").with(as("System Admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đã vô hiệu hóa")))
                .andExpect(content().string(containsString("Chưa cập nhật")))
                .andExpect(content().string(containsString("Lưu trữ (đã vô hiệu hóa)")))
                .andExpect(content().string(not(containsString("Phòng đã đóng khác"))))
                .andReturn().getResponse().getContentAsString();
        fixture("inactive", html);
    }

    @Test
    void createKeepsItsOriginalFormAndDoesNotLoadEditOnlyUi() throws Exception {
        String html = mvc.perform(get("/admin/accounts/new").with(as("System Admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("account-form-container adw-form-wrap")))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("name=\"confirmPassword\"")))
                .andExpect(content().string(not(containsString("account-edit.css"))))
                .andExpect(content().string(not(containsString("class=\"aef-summary\""))))
                .andReturn().getResponse().getContentAsString();
        fixture("create", html);
    }

    @Test
    void adminOnlyAndCsrfRequirementsRemainEnforced() throws Exception {
        mvc.perform(get("/admin/accounts/42/edit").with(as("HR"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/accounts/42/edit")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/accounts/42").with(as("System Admin"))).andExpect(status().isForbidden());
        verify(management, never()).updateInternal(anyInt(), any());
    }

    @Test
    void validUpdateKeepsExistingCommandAndRedirect() throws Exception {
        mvc.perform(post("/admin/accounts/42").with(as("System Admin")).with(csrf())
                        .param("fullName", "Bùi Quốc Việt").param("email", "viet.bui@rms-tech.vn")
                        .param("phoneNumber", "0908901234").param("roleId", "5")
                        .param("departmentId", "7").param("accountStatus", "Inactive"))
                .andExpect(redirectedUrl("/admin/accounts"));
        verify(management).updateInternal(42, new AccountManagementService.UpdateCommand(
                "Bùi Quốc Việt", "viet.bui@rms-tech.vn", "0908901234", 5, 7, "Inactive"));
    }

    @Test
    void longSavedValuesAreEscapedAndMissingDepartmentHasAnEmptyState() throws Exception {
        when(management.findForEdit(42)).thenReturn(new AccountManagementService.AccountForEdit(
                "interviewer_viet", "Bùi Quốc Việt Nguyễn Hoàng Minh Anh <script>",
                "bui.quoc.viet.nguyen.hoang.minh.anh@rms-tech.vn", null, 5, null, "Active"));
        String html = mvc.perform(get("/admin/accounts/42/edit").with(as("System Admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Minh Anh &lt;script&gt;")))
                .andExpect(content().string(containsString("Chưa phân công phòng ban")))
                .andReturn().getResponse().getContentAsString();
        fixture("long", html);
    }

    private static AccountManagementService.AccountForEdit account(String status, Integer department, String phone) {
        return new AccountManagementService.AccountForEdit("interviewer_viet", "Bùi Quốc Việt",
                "viet.bui@rms-tech.vn", phone, 5, department, status);
    }

    private RequestPostProcessor as(String roleName) {
        User account = User.builder().userId(1).username("admin").fullName("Trần Minh Đức")
                .email("admin@rms-tech.vn").accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(account));
        return user("admin").authorities(() -> "System Admin".equals(roleName) ? "ROLE_SYSTEM_ADMIN" : "ROLE_HR");
    }

    private static void fixture(String name, String html) throws Exception {
        Path dir = Path.of("target/account-edit-fixtures");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name + ".html"), html);
    }
}
