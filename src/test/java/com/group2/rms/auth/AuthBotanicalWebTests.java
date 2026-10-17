package com.group2.rms.auth;

import com.group2.rms.auth.controller.*;
import com.group2.rms.auth.service.*;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Optional;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real templates and security filters; email/account writes are mocked, no database needed. */
@WebMvcTest({AuthController.class, RegistrationController.class, PasswordRecoveryController.class})
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class AuthBotanicalWebTests {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean UserRepository users;
    @MockitoBean RegistrationVerificationService registration;
    @MockitoBean PasswordRecoveryService recovery;

    @Test void loginAndRegistrationKeepTheirPostContractsAndFiveRegistrationFields() throws Exception {
        var login=mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/login\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("autocomplete=\"current-password\""))).andReturn();
        snapshot("login",login);
        var register=mvc.perform(get("/register")).andExpect(status().isOk())
                .andExpect(content().string(containsString("auth-register-grid")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("action=\"/register\""))).andReturn();
        String html=register.getResponse().getContentAsString(StandardCharsets.UTF_8);
        for(String name:new String[]{"fullName","username","email","password","confirmPassword"})
            org.junit.jupiter.api.Assertions.assertTrue(html.contains("name=\""+name+"\""),name);
        snapshot("register",register);
    }
    @Test void registrationErrorsRemainVietnameseLinkedToFieldsAndDoNotSendEmail() throws Exception {
        var result=mvc.perform(post("/register").with(csrf()).param("fullName","").param("username","")
                        .param("email","wrong").param("password","short").param("confirmPassword","different"))
                .andExpect(status().isOk()).andExpect(view().name("auth/register"))
                .andExpect(content().string(containsString("Vui lòng nhập thông tin này.")))
                .andExpect(content().string(containsString("Vui lòng nhập email hợp lệ.")))
                .andExpect(content().string(containsString("Mật khẩu xác nhận không khớp.")))
                .andExpect(content().string(containsString("href=\"#full-name\"")))
                .andExpect(content().string(not(containsString("Passwords do not match.")))).andReturn();
        verifyNoInteractions(registration); snapshot("register-errors",result);
    }
    @Test void registrationAndOtpSubmitToTheExistingWorkflowWithCsrf() throws Exception {
        mvc.perform(post("/register")).andExpect(status().isForbidden());
        verifyNoInteractions(registration);
        mvc.perform(post("/register").with(csrf()).param("fullName","Ứng viên kiểm thử").param("username","auth-ui-test")
                        .param("email","auth-ui@example.test").param("password","TestPassword12").param("confirmPassword","TestPassword12"))
                .andExpect(redirectedUrl("/register/otp"));
        verify(registration).requestRegistration(argThat(r->"auth-ui-test".equals(r.getUsername())),any());
        mvc.perform(post("/register/otp").with(csrf()).param("otp","123456"))
                .andExpect(redirectedUrl("/login?registered"));
        verify(registration).verifyAndRegister(eq("123456"),any());
        mvc.perform(post("/register/otp/resend").with(csrf())).andExpect(redirectedUrl("/register/otp"));
        verify(registration).resendOtp(any());
    }
    @Test void otpKeepsSixDigitsExpiryResendAndVietnameseErrors() throws Exception {
        when(registration.getPendingRegistrationEmail(any())).thenReturn("ungvien@example.test");
        when(registration.getResendRemainingSeconds(any())).thenReturn(0L);
        snapshot("register-otp",mvc.perform(get("/register/otp")).andExpect(status().isOk())
                .andExpect(content().string(containsString("auth-registration-otp")))
                .andExpect(content().string(containsString("name=\"otp\"")))
                .andExpect(content().string(containsString("15 phút")))
                .andExpect(content().string(containsString("action=\"/register/otp/resend\""))).andReturn());
        mvc.perform(post("/register/otp").with(csrf()).param("otp","abc"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Mã OTP phải gồm đúng 6 chữ số.")));
        verify(registration,never()).verifyAndRegister(anyString(),any());
    }
    @Test void recoveryScreensRetainTheirOriginalLayoutAndPasswordControls() throws Exception {
        snapshot("forgot-password",mvc.perform(get("/forgot-password")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("auth-botanical-art"))))
                .andExpect(content().string(containsString("action=\"/forgot-password\""))).andReturn());
        when(recovery.canResetPassword(any())).thenReturn(true);
        snapshot("reset-password",mvc.perform(get("/reset-password")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("auth-botanical-art"))))
                .andExpect(content().string(containsString("data-password-toggle")))
                .andExpect(content().string(containsString("name=\"_csrf\""))).andReturn());
    }
    @Test void candidateAndInternalAccountsStillUseSpringSecurityLogin() throws Exception {
        for(String role:new String[]{"Candidate","HR"}) {
            var account=User.builder().userId(123).username("auth-ui-test").email("auth-ui@example.test")
                    .fullName("Tài khoản kiểm thử").role(Role.builder().roleName(role).build())
                    .accountStatus("Active").passwordHash(encoder.encode("TestPassword12")).build();
            when(users.findByUsernameIgnoreCase("auth-ui-test")).thenReturn(Optional.of(account));
            mvc.perform(post("/login").with(csrf()).param("username","auth-ui-test").param("password","TestPassword12"))
                    .andExpect(redirectedUrl("/dashboard"));
        }
    }
    private void snapshot(String name,MvcResult result) throws Exception {
        if(!Boolean.getBoolean("auth.preview")) return;
        Path folder=Path.of("target/auth-botanical-preview"); Files.createDirectories(folder);
        Files.writeString(folder.resolve(name+".html"),result.getResponse().getContentAsString(StandardCharsets.UTF_8),StandardCharsets.UTF_8);
    }
}
