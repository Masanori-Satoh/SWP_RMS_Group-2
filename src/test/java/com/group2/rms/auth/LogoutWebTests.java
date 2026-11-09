package com.group2.rms.auth;

import com.group2.rms.auth.controller.AuthController;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class LogoutWebTests {
    @Autowired MockMvc mvc;
    @MockitoBean UserRepository users;

    @Test
    void validLogoutInvalidatesSessionAndRedirectsWithNotification() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/logout").session(session).with(user("candidate").roles("CANDIDATE")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(unauthenticated());
        assertTrue(session.isInvalid());
    }

    @Test
    void logoutLandingShowsDismissibleToastButNormalLoginDoesNot() throws Exception {
        mvc.perform(get("/login").param("logout", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bạn đã đăng xuất.")))
                .andExpect(content().string(containsString("data-flash-toast")))
                .andExpect(content().string(containsString("data-flash-close")))
                .andExpect(content().string(containsString("/js/pages/flash-toast.js")));
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Bạn đã đăng xuất."))));
    }

    // ducnm sửa lỗi SA6: phiên hết hạn (không còn đăng nhập) mà bấm gửi form -> token CSRF cũ không hợp lệ,
    // phải về trang đăng nhập với thông báo hết phiên thay vì trang 403
    @Test
    void expiredSessionFormSubmitRedirectsToLoginWithSessionExpiredMessage() throws Exception {
        mvc.perform(post("/interviews/new").param("_csrf", "token-cua-phien-cu").with(request -> {
                    request.setRequestedSessionId("phien-da-het-han");
                    request.setRequestedSessionIdValid(false);
                    return request;
                }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?session-expired"));
        // Khách chưa từng có phiên mà thiếu CSRF: vẫn 403 như cũ
        mvc.perform(post("/interviews/new"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/login").param("session-expired", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Phiên")))
                .andExpect(content().string(containsString("làm việc đã hết hạn. Vui lòng đăng nhập lại.")));
    }

    @Test
    void missingCsrfRejectsLogoutBeforeClearingAuthentication() throws Exception {
        mvc.perform(post("/logout").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden())
                .andExpect(authenticated());
    }

    @Test
    void invalidCsrfRejectsLogoutBeforeClearingAuthentication() throws Exception {
        mvc.perform(post("/logout").with(user("candidate").roles("CANDIDATE"))
                        .with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden())
                .andExpect(authenticated());
    }
}
