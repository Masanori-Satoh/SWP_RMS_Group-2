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
