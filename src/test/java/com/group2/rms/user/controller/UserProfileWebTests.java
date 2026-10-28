package com.group2.rms.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.dto.ChangePasswordRequest;
import com.group2.rms.user.dto.UpdateProfileRequest;
import com.group2.rms.user.dto.UserProfileResponse;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.exception.AvatarUploadException;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StringUtils;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserProfileController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class UserProfileWebTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProfileService profileService;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        when(userRepository.findByUsernameIgnoreCase("an.vo")).thenReturn(Optional.of(User.builder()
                .userId(54).username("an.vo").accountStatus("Active").role(Role.builder().roleName("Candidate").build()).build()));
        // Setup mock user for SecurityConfig's AccountSessionGuardFilter
        User mockUser = User.builder()
                .userId(1)
                .username("testuser")
                .accountStatus("Active")
                .role(Role.builder().roleName("HR").build())
                .build();
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(mockUser));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_HR")
    void viewProfile_ReturnsProfilePage_WithCorrectAttributes() throws Exception {
        UserProfileResponse mockProfile = new UserProfileResponse(
                1, "testuser", "test@example.com", "Test User", "0123456789", null, "HR", "Nhân sự", "Engineering"
        );
        when(profileService.getProfile("testuser")).thenReturn(mockProfile);

        mvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"))
                .andExpect(model().attributeExists("userProfile"))
                .andExpect(model().attributeExists("updateRequest"))
                .andExpect(model().attributeExists("passwordRequest"));
    }

    @Test
    void viewProfile_Unauthenticated_RedirectsToLogin() throws Exception {
        mvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_HR")
    void updateProfile_ValidRequest_RedirectsWithSuccess() throws Exception {
        mvc.perform(post("/profile/update")
                .with(csrf())
                .param("fullName", "Updated Name")
                .param("phoneNumber", "0987654321")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_HR")
    void updateProfile_InvalidRequest_RedirectsWithErrors() throws Exception {
        // Test with blank full name (assuming @NotBlank on fullName)
        mvc.perform(post("/profile/update")
                .with(csrf())
                .param("fullName", "")
                .param("phoneNumber", "0987654321")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.updateRequest"))
                .andExpect(flash().attributeExists("updateRequest"));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_HR")
    void changePasswordJson_ValidRequest_ReturnsOk() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass123", "newPass123");
        when(profileService.changePassword(eq("testuser"), any(ChangePasswordRequest.class))).thenReturn(true);

        mvc.perform(post("/profile/change-password")
                .with(csrf())
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Đã đổi mật khẩu.")));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_HR")
    void changePasswordJson_PasswordMismatch_ReturnsBadRequest() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass123", "differentPass");

        mvc.perform(post("/profile/change-password")
                .with(csrf())
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors.confirmPassword", is("Mật khẩu nhập lại không khớp.")));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_HR")
    void changePasswordJson_IncorrectOldPassword_ReturnsBadRequest() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongOldPass", "newPass123", "newPass123");
        when(profileService.changePassword(eq("testuser"), any(ChangePasswordRequest.class))).thenReturn(false);

        mvc.perform(post("/profile/change-password")
                .with(csrf())
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors.currentPassword", is("Mật khẩu hiện tại không đúng.")));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_HR")
    void viewProfile_HasAvatarUploadFormAndNoAvatarUrlField() throws Exception {
        when(profileService.getProfile("testuser")).thenReturn(new UserProfileResponse(
                1, "testuser", "test@example.com", "Test User", null, null, "HR", "Nhân sự", "Engineering"));

        mvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"avatarFile\"")))
                .andExpect(content().string(containsString("enctype=\"multipart/form-data\"")))
                .andExpect(content().string(not(containsString("avatar-url"))));
    }

    /** Mọi chữ người dùng thấy trên /profile là tiếng Việt (bước 3b). */
    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_HR")
    void viewProfile_ShowsVietnameseLabels() throws Exception {
        when(profileService.getProfile("testuser")).thenReturn(new UserProfileResponse(
                1, "testuser", "test@example.com", "Test User", null, null, "HR", "Nhân sự", "Chưa thuộc phòng ban"));

        mvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"vi\">")))
                .andExpect(content().string(containsString("<h1 class=\"page-title adw-title\">Hồ sơ cá nhân</h1>")))
                .andExpect(content().string(containsString("value=\"Nhân sự\"")))
                .andExpect(content().string(containsString("Chưa thuộc phòng ban")))
                .andExpect(content().string(containsString(">Đổi mật khẩu</button>")))
                .andExpect(content().string(not(containsString("My Profile"))))
                .andExpect(content().string(not(containsString("Save Profile"))))
                .andExpect(content().string(not(containsString("Change Password"))))
                .andExpect(content().string(not(containsString("Cancel"))));
    }

    /** /profile nằm trong layout/internal (đúng một sidebar, menu "profile" sáng) và bật giao diện adw. */
    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_HR")
    void viewProfile_UsesInternalLayoutWithAdwLook() throws Exception {
        when(profileService.getProfile("testuser")).thenReturn(new UserProfileResponse(
                1, "testuser", "test@example.com", "Test User", null, null, "HR", "Nhân sự", "Chưa thuộc phòng ban"));

        String html = mvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeMenu", "profile"))
                .andExpect(content().string(containsString("adw-page")))
                .andExpect(content().string(containsString("adw-scene")))
                .andExpect(content().string(containsString("/js/pages/admin-workspace.js")))
                .andReturn().getResponse().getContentAsString();

        assertEquals(1, StringUtils.countOccurrencesOf(html, "id=\"workspace-navigation\""));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_HR")
    void changePasswordJson_BlankFields_ReturnVietnameseMessages() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("", "short", "");

        mvc.perform(post("/profile/change-password")
                .with(csrf())
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Chưa đổi được mật khẩu. Vui lòng xem lỗi bên dưới.")))
                .andExpect(jsonPath("$.errors.currentPassword", is("Vui lòng nhập mật khẩu hiện tại.")))
                .andExpect(jsonPath("$.errors.newPassword", is("Mật khẩu mới dài 8–32 ký tự.")))
                .andExpect(jsonPath("$.errors.confirmPassword", is("Vui lòng nhập lại mật khẩu mới.")));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_HR")
    void changeAvatar_ValidFile_RedirectsWithSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("avatarFile", "me.png", "image/png", new byte[]{1, 2, 3});

        mvc.perform(multipart("/profile/avatar").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("successMessage"));
        verify(profileService).changeAvatar(eq("testuser"), any());
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_HR")
    void changeAvatar_InvalidFile_RedirectsWithAvatarError() throws Exception {
        doThrow(new AvatarUploadException("Chỉ nhận ảnh JPEG, PNG hoặc WebP."))
                .when(profileService).changeAvatar(eq("testuser"), any());
        MockMultipartFile file = new MockMultipartFile("avatarFile", "x.svg", "image/svg+xml", "<svg/>".getBytes());

        mvc.perform(multipart("/profile/avatar").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("avatarError", "Chỉ nhận ảnh JPEG, PNG hoặc WebP."))
                .andExpect(flash().attributeCount(1));
    }

    @Test
    @WithMockUser(username = "an.vo", authorities = "ROLE_CANDIDATE")
    void viewProfile_Candidate_RedirectsToPortalProfile() throws Exception {
        mvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/profile"));
        org.mockito.Mockito.verify(profileService, org.mockito.Mockito.never()).getProfile(any());
    }
}
