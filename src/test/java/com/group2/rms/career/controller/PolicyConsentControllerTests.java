package com.group2.rms.career.controller;

import com.group2.rms.career.service.PolicyConsentService;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * CONTROLLER TEST (@WebMvcTest + SecurityConfig thật): POST /policy-consents (nút "Tôi đồng ý").
 * Kiểm tra: đủ 2 ô mới lưu, quay về đúng trang, chặn open redirect, chỉ ứng viên, bắt buộc CSRF.
 */
@WebMvcTest(controllers = PolicyConsentController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class PolicyConsentControllerTests {

    private static final String USERNAME = "an.nguyen";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PolicyConsentService policyConsent;
    @MockitoBean
    private UserRepository users;

    @Test
    @DisplayName("Tick đủ 2 ô -> Lưu đồng ý, quay về đúng trang đang xem (giữ #apply)")
    void agree_shouldRecordAndReturn_whenBothBoxesChecked() throws Exception {
        mvc.perform(post("/policy-consents").with(candidate()).with(csrf())
                        .param("agreeTerms", "true").param("agreePrivacy", "true")
                        .param("returnUrl", "/jobs/5#apply"))
                .andExpect(redirectedUrl("/jobs/5#apply"));

        verify(policyConsent).recordConsent(USERNAME);
    }

    @Test
    @DisplayName("Gửi tay thiếu ô Chính sách bảo mật -> Không lưu, báo lỗi để popup hiện lại")
    void agree_shouldNotRecord_whenPrivacyBoxMissing() throws Exception {
        mvc.perform(post("/policy-consents").with(candidate()).with(csrf())
                        .param("agreeTerms", "true").param("returnUrl", "/jobs"))
                .andExpect(redirectedUrl("/jobs"))
                .andExpect(flash().attribute("policyConsentError", PolicyConsentController.MISSING_AGREEMENT));

        verify(policyConsent, never()).recordConsent(any());
    }

    @Test
    @DisplayName("returnUrl trỏ ra site khác -> Về /jobs, không chuyển hướng ra ngoài")
    void agree_shouldFallBackToJobs_whenReturnUrlIsExternal() throws Exception {
        mvc.perform(post("/policy-consents").with(candidate()).with(csrf())
                        .param("agreeTerms", "true").param("agreePrivacy", "true")
                        .param("returnUrl", "//evil.example"))
                .andExpect(redirectedUrl("/jobs"));
    }

    @Test
    @DisplayName("HR gọi POST /policy-consents -> 403, không lưu")
    void agree_shouldBeForbidden_whenNotCandidate() throws Exception {
        User hr = User.builder().userId(2).username("hr").accountStatus("Active")
                .role(Role.builder().roleName("HR").build()).build();
        when(users.findByUsernameIgnoreCase("hr")).thenReturn(Optional.of(hr));

        mvc.perform(post("/policy-consents").with(user("hr").authorities(() -> "ROLE_HR")).with(csrf())
                        .param("agreeTerms", "true").param("agreePrivacy", "true"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(policyConsent);
    }

    @Test
    @DisplayName("Thiếu CSRF token -> 403, không lưu")
    void agree_shouldBeForbidden_whenCsrfMissing() throws Exception {
        mvc.perform(post("/policy-consents").with(candidate())
                        .param("agreeTerms", "true").param("agreePrivacy", "true"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(policyConsent);
    }

    @ParameterizedTest(name = "returnUrl = \"{0}\" -> /jobs")
    @NullAndEmptySource
    @ValueSource(strings = {"https://evil.example", "//evil.example", "/\\evil.example", "jobs", "/jobs\r\nSet-Cookie: x=1"})
    @DisplayName("safeReturnUrl: đường dẫn không phải nội bộ -> /jobs")
    void safeReturnUrl_shouldFallBack_whenNotInternalPath(String returnUrl) {
        assertEquals("/jobs", PolicyConsentController.safeReturnUrl(returnUrl));
    }

    @ParameterizedTest(name = "returnUrl = \"{0}\" giữ nguyên")
    @ValueSource(strings = {"/", "/jobs?keyword=java&page=2", "/jobs/5#apply", "/portal/applications"})
    @DisplayName("safeReturnUrl: đường dẫn nội bộ -> Giữ nguyên")
    void safeReturnUrl_shouldKeep_whenInternalPath(String returnUrl) {
        assertEquals(returnUrl, PolicyConsentController.safeReturnUrl(returnUrl));
    }

    private RequestPostProcessor candidate() {
        User account = User.builder().userId(9).username(USERNAME).accountStatus("Active")
                .role(Role.builder().roleName("Candidate").build()).build();
        when(users.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(account));
        return user(USERNAME).authorities(() -> "ROLE_CANDIDATE");
    }
}
