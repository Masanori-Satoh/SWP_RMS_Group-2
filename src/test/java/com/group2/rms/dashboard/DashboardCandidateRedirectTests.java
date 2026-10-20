package com.group2.rms.dashboard;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.dashboard.dto.DashboardResponse;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ứng viên không có dashboard riêng: /dashboard chuyển sang /portal/applications, không chạy truy vấn dashboard. */
@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class DashboardCandidateRedirectTests {

    @Autowired MockMvc mvc;
    @MockitoBean DashboardService dashboardService;
    @MockitoBean UserRepository users;

    @Test
    void candidateIsSentToMyApplicationsWithoutLoadingTheDashboard() throws Exception {
        User account = User.builder().userId(1).username("cuong.vu").fullName("cuong.vu").accountStatus("Active")
                .role(Role.builder().roleName("Candidate").build()).build();
        when(users.findByUsernameIgnoreCase("cuong.vu")).thenReturn(Optional.of(account));

        mvc.perform(get("/dashboard").with(user("cuong.vu").authorities(() -> "ROLE_CANDIDATE")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/applications"));
        verifyNoInteractions(dashboardService);
    }

    @Test
    void staffStillGetsTheirRoleDashboard() {
        DashboardResponse hr = mock(DashboardResponse.class);
        when(hr.roleName()).thenReturn("HR");
        when(dashboardService.forUsername("hr_lan")).thenReturn(hr);
        var auth = new UsernamePasswordAuthenticationToken("hr_lan", null, List.of(new SimpleGrantedAuthority("ROLE_HR")));

        assertEquals("dashboard/index", new DashboardController(dashboardService).dashboard(auth, new ExtendedModelMap()));
    }
}
