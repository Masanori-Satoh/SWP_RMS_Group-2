package com.group2.rms.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.dto.ChangePasswordRequest;
import com.group2.rms.user.dto.UpdateProfileRequest;
import com.group2.rms.user.dto.UserProfileResponse;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        // Setup mock user for SecurityConfig's AccountSessionGuardFilter
        User mockUser = User.builder()
                .userId(1)
                .username("testuser")
                .accountStatus("Active")
                .role(Role.builder().roleName("Candidate").build())
                .build();
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(mockUser));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_CANDIDATE")
    void viewProfile_ReturnsProfilePage_WithCorrectAttributes() throws Exception {
        UserProfileResponse mockProfile = new UserProfileResponse(
                1, "testuser", "test@example.com", "Test User", "0123456789", null, "Candidate", "Engineering"
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
    @WithMockUser(username = "testuser", authorities = "ROLE_CANDIDATE")
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
    @WithMockUser(username = "testuser", authorities = "ROLE_CANDIDATE")
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
    @WithMockUser(username = "testuser", authorities = "ROLE_CANDIDATE")
    void changePasswordJson_ValidRequest_ReturnsOk() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass123", "newPass123");
        when(profileService.changePassword(eq("testuser"), any(ChangePasswordRequest.class))).thenReturn(true);

        mvc.perform(post("/profile/change-password")
                .with(csrf())
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("successfully")));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_CANDIDATE")
    void changePasswordJson_PasswordMismatch_ReturnsBadRequest() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass123", "differentPass");

        mvc.perform(post("/profile/change-password")
                .with(csrf())
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors.confirmPassword", containsString("do not match")));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_CANDIDATE")
    void changePasswordJson_IncorrectOldPassword_ReturnsBadRequest() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongOldPass", "newPass123", "newPass123");
        when(profileService.changePassword(eq("testuser"), any(ChangePasswordRequest.class))).thenReturn(false);

        mvc.perform(post("/profile/change-password")
                .with(csrf())
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors.currentPassword", containsString("Incorrect current password")));
    }
}
