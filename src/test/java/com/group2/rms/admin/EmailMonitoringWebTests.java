package com.group2.rms.admin;

import com.group2.rms.admin.dto.MonitorRowResponse;
import com.group2.rms.admin.dto.ProbeOutcomeResponse;
import com.group2.rms.admin.service.EmailMonitoringService;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** SELECT-only database setup; the SMTP probe itself is mocked and never connects. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc
@Transactional(readOnly = true)
class EmailMonitoringWebTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @MockitoBean EmailMonitoringService email;
    private String admin;

    @BeforeEach
    void currentAdmin() {
        User account = users.findAllWithRoleAndDepartment().stream()
                .filter(u -> "System Admin".equals(u.getRole().getRoleName()) && "Active".equals(u.getAccountStatus()))
                .findFirst().orElse(null);
        assumeTrue(account != null, "An active System Admin is required for the rendered Admin page");
        admin = account.getUsername();
        when(email.row()).thenReturn(new MonitorRowResponse("email", "Email Service", "External Integration", "Not check", null, null));
    }

    @Test
    void pageOffersManualSmtpCheckWithoutRunningIt() throws Exception {
        mvc.perform(get("/admin/api-monitoring").with(user(admin).authorities(new SimpleGrantedAuthority(RoleAuthorities.SYSTEM_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/api-monitoring/probe/email")))
                .andExpect(content().string(containsString("Kiểm tra SMTP")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        verify(email, never()).probe();
    }

    @Test
    void unconfiguredEmailHasNoProbeForm() throws Exception {
        when(email.row()).thenReturn(new MonitorRowResponse("email", "Email Service", "External Integration", "Unconfigured", null, null));
        mvc.perform(get("/admin/api-monitoring").with(user(admin).authorities(new SimpleGrantedAuthority(RoleAuthorities.SYSTEM_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/admin/api-monitoring/probe/email"))));
        verify(email, never()).probe();
    }

    @Test
    void emailProbeRequiresCsrf() throws Exception {
        mvc.perform(post("/admin/api-monitoring/probe/email").session(new MockHttpSession())
                .with(user(admin).authorities(new SimpleGrantedAuthority(RoleAuthorities.SYSTEM_ADMIN))))
                .andExpect(status().isForbidden());
        verify(email, never()).probe();
    }

    @Test
    void hrCannotRunEmailProbeEvenWithCsrf() throws Exception {
        String hr = users.findAllWithRoleAndDepartment().stream()
                .filter(u -> "HR".equals(u.getRole().getRoleName()) && "Active".equals(u.getAccountStatus()))
                .map(User::getUsername).findFirst().orElse(null);
        assumeTrue(hr != null, "An active HR account is required for the authorization check");
        mvc.perform(post("/admin/api-monitoring/probe/email").session(new MockHttpSession()).with(csrf())
                .with(user(hr).authorities(new SimpleGrantedAuthority(RoleAuthorities.fromRoleName("HR")))))
                .andExpect(status().isForbidden());
        verify(email, never()).probe();
    }

    @Test
    void signedOutUserCannotOpenMonitoring() throws Exception {
        mvc.perform(get("/admin/api-monitoring")).andExpect(status().is3xxRedirection());
        verify(email, never()).probe();
    }

    @Test
    void adminCanExplicitlyProbeAndReceivesSuccessMessage() throws Exception {
        when(email.probe()).thenReturn(new ProbeOutcomeResponse(true, "Kết nối SMTP thành công.", 12));
        mvc.perform(post("/admin/api-monitoring/probe/email").session(new MockHttpSession()).with(csrf())
                .with(user(admin).authorities(new SimpleGrantedAuthority(RoleAuthorities.SYSTEM_ADMIN))))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/api-monitoring"))
                .andExpect(flash().attribute("successMessage", "Kết nối SMTP thành công. • 12 ms"));
        verify(email).probe();
    }

    @Test
    void failedProbeReturnsFailureMessageWithoutChangingRouting() throws Exception {
        when(email.probe()).thenReturn(new ProbeOutcomeResponse(false, "Không thể kết nối SMTP.", 5000));
        mvc.perform(post("/admin/api-monitoring/probe/email").session(new MockHttpSession()).with(csrf())
                .with(user(admin).authorities(new SimpleGrantedAuthority(RoleAuthorities.SYSTEM_ADMIN))))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/api-monitoring"))
                .andExpect(flash().attribute("failureMessage", "Không thể kết nối SMTP. • 5000 ms"));
        verify(email).probe();
    }
}
