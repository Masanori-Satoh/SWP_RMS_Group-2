package com.group2.rms.admin.controller;

import com.group2.rms.admin.dto.PolicyDocumentResponse;
import com.group2.rms.admin.exception.PolicyDocumentException;
import com.group2.rms.admin.service.PolicyDocument;
import com.group2.rms.admin.service.PolicyDocumentService;
import com.group2.rms.admin.service.PolicyDocumentStorage;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * CONTROLLER TEST (@WebMvcTest + SecurityConfig thật): màn System Admin "Tài liệu chính sách".
 */
@WebMvcTest(controllers = PolicyDocumentAdminController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class PolicyDocumentAdminControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PolicyDocumentService policyDocuments;
    @MockitoBean
    private UserRepository users;

    private final MockMultipartFile pdf = new MockMultipartFile("file", "dk.pdf", "application/pdf", "%PDF-1.4".getBytes());

    @Test
    @DisplayName("Admin mở màn -> Thấy 2 tài liệu, nhãn trạng thái, người cập nhật, menu sáng đúng mục")
    void list_shouldRenderBothDocuments_whenAdmin() throws Exception {
        when(policyDocuments.listDocuments()).thenReturn(List.of(
                new PolicyDocumentResponse("terms", "Điều khoản sử dụng", "/terms", "terms.pdf", true, "320 KB",
                        LocalDateTime.of(2026, 11, 1, 9, 30), "Trần Minh Đức"),
                new PolicyDocumentResponse("privacy", "Chính sách bảo mật", "/privacy-policy", "privacy.pdf", false, "1 KB",
                        null, null)));

        mvc.perform(get("/admin/policy-documents").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Điều khoản sử dụng")))
                .andExpect(content().string(containsString("Đã tải lên")))
                .andExpect(content().string(containsString("Bản mặc định")))
                .andExpect(content().string(containsString("01/11/2026 09:30")))
                .andExpect(content().string(containsString("bởi Trần Minh Đức")))
                .andExpect(content().string(containsString("Chưa tải bản nào")))
                .andExpect(content().string(containsString("action=\"/admin/policy-documents/privacy\"")))
                .andExpect(content().string(containsString("href=\"/admin/policy-documents\"")));
    }

    @Test
    @DisplayName("Tải PDF hợp lệ -> Về danh sách kèm thông báo thành công")
    void replace_shouldRedirectWithSuccess_whenFileValid() throws Exception {
        mvc.perform(multipart("/admin/policy-documents/terms").file(pdf).with(admin()).with(csrf()))
                .andExpect(redirectedUrl("/admin/policy-documents"))
                .andExpect(flash().attribute("successMessage", "Đã thay Điều khoản sử dụng. Ứng viên thấy bản mới ngay."));

        verify(policyDocuments).replace(eq(PolicyDocument.TERMS), any());
    }

    @Test
    @DisplayName("File không phải PDF -> Về danh sách, mở lại hộp tải lên của đúng tài liệu kèm lỗi")
    void replace_shouldReturnErrorForThatDocument_whenFileInvalid() throws Exception {
        doThrow(new PolicyDocumentException(PolicyDocumentStorage.NOT_PDF))
                .when(policyDocuments).replace(eq(PolicyDocument.PRIVACY), any());

        mvc.perform(multipart("/admin/policy-documents/privacy").file(pdf).with(admin()).with(csrf()))
                .andExpect(redirectedUrl("/admin/policy-documents"))
                .andExpect(flash().attribute("uploadError", PolicyDocumentStorage.NOT_PDF))
                .andExpect(flash().attribute("uploadErrorCode", "privacy"));
    }

    @Test
    @DisplayName("Mã tài liệu lạ trên URL -> 404, không gọi service")
    void replace_shouldBeNotFound_whenCodeUnknown() throws Exception {
        mvc.perform(multipart("/admin/policy-documents/abc").file(pdf).with(admin()).with(csrf()))
                .andExpect(status().isNotFound());

        verifyNoInteractions(policyDocuments);
    }

    @Test
    @DisplayName("HR mở màn hoặc tải file -> 403, không gọi service")
    void adminScreen_shouldBeForbidden_whenNotSystemAdmin() throws Exception {
        User hr = User.builder().userId(2).username("hr").accountStatus("Active")
                .role(Role.builder().roleName("HR").build()).build();
        when(users.findByUsernameIgnoreCase("hr")).thenReturn(Optional.of(hr));
        RequestPostProcessor asHr = user("hr").authorities(() -> "ROLE_HR");

        mvc.perform(get("/admin/policy-documents").with(asHr)).andExpect(status().isForbidden());
        mvc.perform(multipart("/admin/policy-documents/terms").file(pdf).with(asHr).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(policyDocuments);
    }

    private RequestPostProcessor admin() {
        User account = User.builder().userId(1).username("admin").fullName("Trần Minh Đức")
                .email("admin@rms-tech.vn").accountStatus("Active")
                .role(Role.builder().roleName("System Admin").build()).build();
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(account));
        return user("admin").authorities(() -> "ROLE_SYSTEM_ADMIN");
    }
}
