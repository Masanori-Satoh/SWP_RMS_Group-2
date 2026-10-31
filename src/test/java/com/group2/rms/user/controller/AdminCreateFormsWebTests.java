package com.group2.rms.user.controller;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.dto.DepartmentRequest;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.AccountListService;
import com.group2.rms.user.service.AccountManagementService;
import com.group2.rms.user.service.DepartmentService;
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

@WebMvcTest({AccountController.class, DepartmentController.class})
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class AdminCreateFormsWebTests {
    @Autowired MockMvc mvc;
    @MockitoBean AccountListService accounts;
    @MockitoBean AccountManagementService management;
    @MockitoBean DepartmentService departments;
    @MockitoBean UserRepository users;

    @BeforeEach
    void setup() {
        when(accounts.findRoles()).thenReturn(List.of(
                Role.builder().roleId(1).roleName("System Admin").build(),
                Role.builder().roleId(2).roleName("HR").build()));
        when(accounts.findDepartments()).thenReturn(List.of(
                Department.builder().departmentId(7).departmentName("Engineering").departmentStatus("Active").build(),
                Department.builder().departmentId(8).departmentName("Phòng đã vô hiệu hóa").departmentStatus("Inactive").build()));
    }

    @Test
    void accountCreateRendersGuidanceAndOriginalSecureFormControls() throws Exception {
        String html = mvc.perform(get("/admin/accounts/new").with(as("System Admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/admin/accounts\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("id=\"acf-notes-title\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("name=\"confirmPassword\"")))
                .andExpect(content().string(not(containsString("name=\"accountStatus\""))))
                .andExpect(content().string(not(containsString("Phòng đã vô hiệu hóa"))))
                .andExpect(content().string(not(containsString("account-edit.css"))))
                .andReturn().getResponse().getContentAsString();
        fixture("account", html);
    }

    @Test
    void accountValidationKeepsInputAndErrorsButClearsBothPasswords() throws Exception {
        String html = mvc.perform(post("/admin/accounts").with(as("System Admin")).with(csrf())
                        .param("fullName", "Nguyễn Minh Anh").param("username", "hr_minh_anh")
                        .param("email", "invalid-email").param("roleId", "2").param("departmentId", "7")
                        .param("password", "FixturePass2026!").param("confirmPassword", "Different2026!"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "email"))
                .andExpect(model().attribute("form", hasProperty("username", is("hr_minh_anh"))))
                .andExpect(model().attribute("form", hasProperty("password", nullValue())))
                .andExpect(model().attribute("form", hasProperty("confirmPassword", nullValue())))
                .andExpect(content().string(not(containsString("FixturePass2026!"))))
                .andExpect(content().string(not(containsString("Different2026!"))))
                .andReturn().getResponse().getContentAsString();
        verify(management, never()).createInternal(any());
        fixture("account-error", html);
    }

    @Test
    void validAccountStillUsesExistingCommandAndRedirect() throws Exception {
        mvc.perform(post("/admin/accounts").with(as("System Admin")).with(csrf())
                        .param("fullName", "Nguyễn Minh Anh").param("username", "hr_minh_anh")
                        .param("email", "minh.anh@rms-tech.vn").param("roleId", "2").param("departmentId", "7")
                        .param("password", "FixturePass2026!").param("confirmPassword", "FixturePass2026!"))
                .andExpect(redirectedUrl("/admin/accounts"));
        verify(management).createInternal(argThat(command -> "hr_minh_anh".equals(command.username())
                && command.roleId() == 2 && command.departmentId() == 7));
    }

    @Test
    void departmentCreateKeepsOneNameFieldAndNoPrematureManagerAssignment() throws Exception {
        String html = mvc.perform(get("/admin/departments/new").with(as("System Admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/admin/departments\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"departmentName\"")))
                .andExpect(content().string(containsString("maxlength=\"100\"")))
                .andExpect(content().string(not(containsString("name=\"managerId\""))))
                .andExpect(content().string(not(containsString("department-edit.css"))))
                .andReturn().getResponse().getContentAsString();
        fixture("department", html);
    }

    @Test
    void invalidDepartmentRendersFieldErrorInTheCreationForm() throws Exception {
        String html = mvc.perform(post("/admin/departments").with(as("System Admin")).with(csrf())
                        .param("departmentName", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "departmentName"))
                .andExpect(content().string(matchesPattern("(?s).*id=\"department-name-error\">[^<]+</span>.*")))
                .andReturn().getResponse().getContentAsString();
        verify(departments, never()).create(any());
        fixture("department-error", html);
    }

    @Test
    void validDepartmentKeepsExistingCreateCommandAndRedirect() throws Exception {
        mvc.perform(post("/admin/departments").with(as("System Admin")).with(csrf())
                        .param("departmentName", "Nghiên cứu và phát triển"))
                .andExpect(redirectedUrl("/admin/departments"));
        verify(departments).create(new DepartmentRequest("Nghiên cứu và phát triển", null));
    }

    @Test
    void creationRemainsAdminOnlyAndRequiresCsrf() throws Exception {
        for (String path : List.of("/admin/accounts/new", "/admin/departments/new")) {
            mvc.perform(get(path).with(as("HR"))).andExpect(status().isForbidden());
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
        }
        mvc.perform(post("/admin/accounts").with(as("System Admin"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/departments").with(as("System Admin"))).andExpect(status().isForbidden());
    }

    private RequestPostProcessor as(String roleName) {
        User account = User.builder().userId(1).username("admin").fullName("Trần Minh Đức")
                .email("admin@rms-tech.vn").accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(account));
        return user("admin").authorities(() -> "System Admin".equals(roleName) ? "ROLE_SYSTEM_ADMIN" : "ROLE_HR");
    }

    private static void fixture(String name, String html) throws Exception {
        Path dir = Path.of("target/admin-create-fixtures");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name + ".html"), html);
    }
}
