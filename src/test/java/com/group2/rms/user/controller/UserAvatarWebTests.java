package com.group2.rms.user.controller;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.dto.UserAvatarResponse;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** {@code /users/{id}/avatar}: người đã đăng nhập xem được ảnh của nhau; khách về trang đăng nhập; không có ảnh thì 404. */
@WebMvcTest(UserAvatarController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class UserAvatarWebTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserProfileService profileService;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User viewer = User.builder().userId(5).username("hm_tuan").accountStatus("Active")
                .role(Role.builder().roleName("Hiring Manager").build()).build();
        when(userRepository.findByUsernameIgnoreCase("hm_tuan")).thenReturn(Optional.of(viewer));
    }

    @Test
    @WithMockUser(username = "hm_tuan", authorities = "ROLE_HIRING_MANAGER")
    void signedInUserSeesAnotherUsersAvatarWithItsType() throws Exception {
        when(profileService.avatarFile(3)).thenReturn(
                new UserAvatarResponse(new ByteArrayResource(new byte[]{1, 2, 3}), "image/webp"));

        mvc.perform(get("/users/3/avatar"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/webp"))
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    @WithMockUser(username = "hm_tuan", authorities = "ROLE_HIRING_MANAGER")
    void userWithoutUploadedAvatarIsNotFound() throws Exception {
        when(profileService.avatarFile(4)).thenThrow(new ResourceNotFoundException("Không tìm thấy ảnh đại diện."));

        mvc.perform(get("/users/4/avatar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void guestIsSentToLogin() throws Exception {
        mvc.perform(get("/users/3/avatar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
