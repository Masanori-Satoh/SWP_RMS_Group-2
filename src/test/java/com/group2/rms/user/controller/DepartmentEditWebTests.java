package com.group2.rms.user.controller;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.dto.DepartmentManagerResponse;
import com.group2.rms.user.dto.DepartmentResponse;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.DepartmentService;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class DepartmentEditWebTests {
    @Autowired MockMvc mvc;
    @MockitoBean DepartmentService departments;
    @MockitoBean UserRepository users;

    @Test
    void editShowsSavedDepartmentSummaryAndKeepsManagerField() throws Exception {
        when(departments.findForEdit(7)).thenReturn(new DepartmentResponse(7, "Engineering", 3, "Vũ Anh Tuấn", "hm_tuan", "Active"));
        when(departments.managerChoices(7)).thenReturn(List.of(new DepartmentManagerResponse(3, "Vũ Anh Tuấn", "hm_tuan")));
        String html = mvc.perform(get("/admin/departments/7/edit").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Thông tin phòng ban đang lưu\"")))
                .andExpect(content().string(containsString("class=\"def-summary-name\">Engineering</h2>")))
                .andExpect(content().string(containsString("Đang hoạt động")))
                .andExpect(content().string(containsString("id=\"department-manager\"")))
                .andExpect(content().string(containsString("action=\"/admin/departments/7\"")))
                .andExpect(content().string(containsString("department-edit.css")))
                .andReturn().getResponse().getContentAsString();
        fixture("active", html);
    }

    @Test
    void unassignedAndInactiveDepartmentHasHonestEmptyState() throws Exception {
        when(departments.findForEdit(8)).thenReturn(new DepartmentResponse(8, "Lưu trữ <script>", null, null, null, "Inactive"));
        when(departments.managerChoices(8)).thenReturn(List.of());
        String html = mvc.perform(get("/admin/departments/8/edit").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chưa phân công")))
                .andExpect(content().string(containsString("Đã vô hiệu hóa")))
                .andExpect(content().string(containsString("Lưu trữ &lt;script&gt;")))
                .andReturn().getResponse().getContentAsString();
        fixture("inactive", html);
    }

    @Test
    void createKeepsItsControlsAndDoesNotLoadEditOnlyUi() throws Exception {
        String html = mvc.perform(get("/admin/departments/new").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin-create-forms.css")))
                .andExpect(content().string(not(containsString("department-edit.css"))))
                .andExpect(content().string(not(containsString("class=\"def-summary\""))))
                .andReturn().getResponse().getContentAsString();
        fixture("create", html);
    }

    private RequestPostProcessor admin() {
        User account = User.builder().userId(1).username("admin").fullName("Trần Minh Đức")
                .email("admin@rms-tech.vn").accountStatus("Active")
                .role(Role.builder().roleName("System Admin").build()).build();
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(account));
        return user("admin").authorities(() -> "ROLE_SYSTEM_ADMIN");
    }

    private static void fixture(String name, String html) throws Exception {
        Path dir = Path.of("target/department-edit-fixtures");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name + ".html"), html);
    }
}
