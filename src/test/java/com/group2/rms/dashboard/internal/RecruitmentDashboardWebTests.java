package com.group2.rms.dashboard;

import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Renders each recruitment dashboard against the configured SQL Server without changing data. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional(readOnly = true)
class RecruitmentDashboardWebTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @ParameterizedTest
    @CsvSource({"HR,ROLE_HR", "Director,ROLE_DIRECTOR", "Interviewer,ROLE_INTERVIEWER"})
    void dashboard_shouldRenderChartsAndAccessibleValues_whenRoleIsActive(String roleName, String authority)
            throws Exception {
        User actor = users.findAllWithRoleAndDepartment().stream()
                .filter(account -> roleName.equals(account.getRole().getRoleName())
                        && "Active".equals(account.getAccountStatus()))
                .findFirst().orElse(null);
        assumeTrue(actor != null, "An active account is required for this role");

        String html = mvc.perform(get("/dashboard").with(user(actor.getUsername())
                        .authorities(new SimpleGrantedAuthority(authority))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(html.contains("Phân tích tuyển dụng"));
        assertTrue(html.contains("data-chart-point"));
        assertTrue(html.contains("recruitment-dashboard.js"));
        assertTrue(html.contains("chart.umd.min.js"));
    }
}
