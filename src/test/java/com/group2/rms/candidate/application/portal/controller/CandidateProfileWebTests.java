package com.group2.rms.candidate.application.portal.controller;

import com.group2.rms.candidate.application.portal.dto.CandidateDetailsRequest;
import com.group2.rms.candidate.application.portal.dto.CandidateProfileResponse;
import com.group2.rms.candidate.application.portal.exception.CandidateProfileException;
import com.group2.rms.candidate.application.portal.service.CandidateProfileService;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.dto.UpdateProfileRequest;
import com.group2.rms.user.dto.UserProfileResponse;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.exception.AvatarUploadException;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Trang hồ sơ ứng viên {@code /portal/profile} qua Spring MVC + Spring Security thật; service là mock. */
@WebMvcTest(CandidateProfileController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class CandidateProfileWebTests {

    @Autowired MockMvc mvc;
    @MockitoBean CandidateProfileService candidateProfiles;
    @MockitoBean UserProfileService userProfiles;
    @MockitoBean UserRepository users;

    @Test
    void guestIsSentToLogin() throws Exception {
        mvc.perform(get("/portal/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void internalStaffIsForbidden() throws Exception {
        mvc.perform(get("/portal/profile").with(as("hr_lan", "HR"))).andExpect(status().isForbidden());
        mvc.perform(post("/portal/profile/update").with(as("hr_lan", "HR")).with(csrf()).param("fullName", "Lan"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(candidateProfiles, userProfiles);
    }

    @Test
    void candidateSeesOwnFieldsButNotInternalOnes() throws Exception {
        when(candidateProfiles.get("an.vo")).thenReturn(profile());

        mvc.perform(get("/portal/profile").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/my-profile"))
                .andExpect(model().attribute("activeMenu", "profile"))
                .andExpect(content().string(containsString("value=\"1999-04-12\"")))
                .andExpect(content().string(containsString("<option value=\"Nam\" selected=\"selected\">Nam</option>")))
                .andExpect(content().string(containsString("name=\"address\" value=\"Số 1 L")))
                .andExpect(content().string(containsString("https://linkedin.com/in/an")))
                .andExpect(content().string(containsString("name=\"avatarFile\"")))
                .andExpect(content().string(containsString("action=\"/portal/profile/update\"")))
                .andExpect(content().string(not(containsString("CandidateSource"))))
                .andExpect(content().string(not(containsString("Tiềm năng"))))
                .andExpect(content().string(not(containsString("sidebar-shell"))));
    }

    @Test
    void validSaveUpdatesBothPartsAndRedirects() throws Exception {
        mvc.perform(save().param("dateOfBirth", "1999-04-12").param("gender", "Nữ"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/profile"))
                .andExpect(flash().attribute("successMessage", "Đã lưu hồ sơ."));

        ArgumentCaptor<UpdateProfileRequest> account = ArgumentCaptor.forClass(UpdateProfileRequest.class);
        ArgumentCaptor<CandidateDetailsRequest> details = ArgumentCaptor.forClass(CandidateDetailsRequest.class);
        verify(candidateProfiles).update(eq("an.vo"), account.capture(), details.capture());
        assertEquals("Võ Thành An", account.getValue().fullName());
        assertEquals(LocalDate.of(1999, 4, 12), details.getValue().dateOfBirth());
        assertEquals("Nữ", details.getValue().gender());
        assertEquals("https://linkedin.com/in/an", details.getValue().linkedInUrl());
    }

    @Test
    void futureBirthDateBadLinkAndUnknownGenderAreRejected() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();
        mvc.perform(invalidSave(tomorrow))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/profile"))
                .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.detailsRequest"))
                .andExpect(flash().attributeExists("detailsRequest"));
        verifyNoInteractions(candidateProfiles);
    }

    @Test
    void blankFullNameIsRejected() throws Exception {
        mvc.perform(post("/portal/profile/update").with(as("an.vo", "Candidate")).with(csrf())
                        .param("fullName", "").param("phoneNumber", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.updateRequest"));
        verifyNoInteractions(candidateProfiles);
    }

    @Test
    void validationErrorsAreShownNextToFields() throws Exception {
        when(candidateProfiles.get("an.vo")).thenReturn(profile());
        var redirect = mvc.perform(invalidSave(LocalDate.now().plusDays(1).toString())).andReturn();

        mvc.perform(get("/portal/profile").with(as("an.vo", "Candidate")).flashAttrs(redirect.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ngày được chọn phải ở trong quá khứ.")))
                .andExpect(content().string(containsString("Link LinkedIn phải bắt đầu bằng http:// hoặc https://.")));
    }

    @Test
    void legacyGenderIsOfferedAndSelectedAndServiceRejectionShowsAtGenderField() throws Exception {
        CandidateProfileResponse legacy = new CandidateProfileResponse(profile().account(), null, "Male", null, null, null);
        when(candidateProfiles.get("an.vo")).thenReturn(legacy);
        mvc.perform(get("/portal/profile").with(as("an.vo", "Candidate")))
                .andExpect(content().string(containsString("<option value=\"Male\" selected=\"selected\">Male</option>")))
                .andExpect(content().string(containsString("<option value=\"Khác\">Khác</option>")));

        doThrow(new CandidateProfileException("gender", "Giới tính không hợp lệ."))
                .when(candidateProfiles).update(eq("an.vo"), any(), any());
        var redirect = mvc.perform(save().param("gender", "X"))
                .andExpect(redirectedUrl("/portal/profile"))
                .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.detailsRequest"))
                .andReturn();
        mvc.perform(get("/portal/profile").with(as("an.vo", "Candidate")).flashAttrs(redirect.getFlashMap()))
                .andExpect(content().string(containsString("<span id=\"gender-error\" class=\"form-error\">Giới tính không hợp lệ.</span>")));
    }

    @Test
    void avatarUploadGoesThroughSharedServiceAndReportsErrors() throws Exception {
        MockMultipartFile ok = new MockMultipartFile("avatarFile", "me.png", "image/png", new byte[]{1});
        mvc.perform(multipart("/portal/profile/avatar").file(ok).with(as("an.vo", "Candidate")).with(csrf()))
                .andExpect(redirectedUrl("/portal/profile"))
                .andExpect(flash().attribute("successMessage", "Đã cập nhật ảnh đại diện."));
        verify(userProfiles).changeAvatar(eq("an.vo"), any());

        doThrow(new AvatarUploadException("Ảnh tối đa 2 MB.")).when(userProfiles).changeAvatar(eq("an.vo"), any());
        mvc.perform(multipart("/portal/profile/avatar").file(ok).with(as("an.vo", "Candidate")).with(csrf()))
                .andExpect(redirectedUrl("/portal/profile"))
                .andExpect(flash().attribute("avatarError", "Ảnh tối đa 2 MB."));
    }

    /** Ngày sinh tương lai, giới tính lạ (sửa form bằng DevTools), LinkedIn không phải link. */
    private MockHttpServletRequestBuilder invalidSave(String dateOfBirth) {
        return post("/portal/profile/update").with(as("an.vo", "Candidate")).with(csrf())
                .param("fullName", "Võ Thành An").param("dateOfBirth", dateOfBirth)
                .param("gender", "X").param("linkedInUrl", "abc");
    }

    private MockHttpServletRequestBuilder save() {
        return post("/portal/profile/update").with(as("an.vo", "Candidate")).with(csrf())
                .param("fullName", "Võ Thành An").param("phoneNumber", "0901234567")
                .param("address", "Số 1 Lê Lợi").param("linkedInUrl", "https://linkedin.com/in/an")
                .param("portfolioUrl", "");
    }

    private static CandidateProfileResponse profile() {
        return new CandidateProfileResponse(
                new UserProfileResponse(54, "an.vo", "an.vo@example.test", "Võ Thành An", "0901234567", null, "Candidate", "Ứng viên", "Chưa thuộc phòng ban"),
                LocalDate.of(1999, 4, 12), "Nam", "Số 1 Lê Lợi", "https://linkedin.com/in/an", null);
    }

    private RequestPostProcessor as(String username, String roleName) {
        User account = User.builder().userId(1).username(username).fullName(username).accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
        when(users.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(account));
        String authority = "HR".equals(roleName) ? "ROLE_HR" : "ROLE_CANDIDATE";
        return user(username).authorities(() -> authority);
    }
}
