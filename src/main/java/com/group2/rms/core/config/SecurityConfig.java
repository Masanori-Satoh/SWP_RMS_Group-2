package com.group2.rms.core.config;

import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.core.security.AccountSessionGuardFilter;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.core.security.RoleAuthorities;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter.XFrameOptionsMode;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Shared authentication foundation and the route rules confirmed for LinhDN.
 */
@Configuration
@EnableWebSecurity
// @EnableMethodSecurity: Tạm tắt để cho phép test các luồng API trong giai đoạn
// phát triển (permitAll)
public class SecurityConfig {

        /**
         * Paths the CV viewer frame loads: the protected CV endpoint and the seed sample CVs it redirects to.
         * Only these may be framed by the same origin.
         */
        private static final RequestMatcher CV_PATHS = new OrRequestMatcher(
                        PathPatternRequestMatcher.withDefaults().matcher("/applications/*/cv"),
                        PathPatternRequestMatcher.withDefaults().matcher("/samples/cv/**"));

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http,
                        DatabaseUserDetailsService userDetailsService,
                        PasswordEncoder passwordEncoder,
                        UserRepository userRepository) throws Exception {
                // set authentication provider
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder);
                // set request cache
                HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
                requestCache.setMatchingRequestParameterName(null);

                http
                                .authenticationProvider(provider)
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/api/**"))
                                // CV viewer: only CV paths may be framed (same origin); every other page keeps DENY
                                .headers(headers -> headers
                                                .frameOptions(frame -> frame.disable())
                                                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(CV_PATHS,
                                                                new XFrameOptionsHeaderWriter(XFrameOptionsMode.SAMEORIGIN)))
                                                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                                                new NegatedRequestMatcher(CV_PATHS),
                                                                new XFrameOptionsHeaderWriter(XFrameOptionsMode.DENY))))
                                .authorizeHttpRequests(auth -> auth
                                                // permit all for static resources
                                                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                                                .permitAll()
                                                // permit all for icon and error
                                                .requestMatchers("/favicon.ico", "/error").permitAll()
                                                // permit all for defaultpage and font
                                                .requestMatchers(HttpMethod.GET, "/", "/fonts/**").permitAll()
                                                // permit all for login, register, forgot-password, reset-password
                                                .requestMatchers("/login", "/register", "/register/**", "/forgot-password",
                                                                "/reset-password/**")
                                                .permitAll()
                                                // required role canididate for job apply
                                                .requestMatchers("/jobs/*/apply", "/jobs/*/apply/**")
                                                .hasAuthority("ROLE_CANDIDATE")
                                                // permit all for job and public job
                                                .requestMatchers(HttpMethod.GET, "/jobs", "/jobs/**", "/public/jobs",
                                                                "/public/jobs/**")
                                                .permitAll()
                                                // requrired role system admin for admin pages
                                                // account
                                                .requestMatchers("/admin/accounts", "/admin/accounts/**",
                                                                // department
                                                                "/admin/departments", "/admin/departments/**",
                                                                // candidate account
                                                                "/admin/candidate-accounts",
                                                                "/admin/candidate-accounts/**",
                                                                // api
                                                                "/admin/api-monitoring", "/admin/api-monitoring/**",
                                                                // api config
                                                                "/admin/ai-configuration", "/admin/ai-configuration/**")
                                                .hasAuthority(RoleAuthorities.SYSTEM_ADMIN)
                                                .requestMatchers("/admin", "/admin/**")
                                                .hasAuthority(RoleAuthorities.SYSTEM_ADMIN)
                                                // Candidate portal is read-only and isolated from internal schedules.
                                                .requestMatchers(HttpMethod.GET, "/portal/interviews")
                                                .hasAuthority("ROLE_CANDIDATE")
                                                .requestMatchers("/portal/interviews", "/portal/interviews/**").denyAll()
                                                // Candidate's own applications and withdrawal (ownership checked in MyApplicationService)
                                                .requestMatchers("/portal/applications", "/portal/applications/**")
                                                .hasAuthority("ROLE_CANDIDATE")
                                                // Candidate's own profile page (Q7.1); internal roles use /profile
                                                .requestMatchers("/portal/profile", "/portal/profile/**")
                                                .hasAuthority("ROLE_CANDIDATE")
                                                // dunglt bổ sung: ứng viên xem / nhận / từ chối offer đã gửi cho mình (phải đứng trước /offers/**)
                                                .requestMatchers("/portal/offers", "/portal/offers/**")
                                                .hasAuthority("ROLE_CANDIDATE")
                                                // Forms and all writes: HR, Director (UC 62-64, SRS footnote 13).
                                                .requestMatchers("/interviews/new", "/interviews/*/edit")
                                                .hasAnyAuthority("ROLE_HR", "ROLE_DIRECTOR")
                                                .requestMatchers(HttpMethod.GET, "/interviews", "/interviews/**")
                                                .hasAnyAuthority("ROLE_HR",
                                                                "ROLE_DIRECTOR", "ROLE_HIRING_MANAGER", "ROLE_INTERVIEWER")
                                                .requestMatchers(HttpMethod.POST, "/interviews/*/evaluations")
                                                .hasAnyAuthority("ROLE_INTERVIEWER", "ROLE_HIRING_MANAGER", "ROLE_DIRECTOR", "ROLE_HR")
                                                .requestMatchers(HttpMethod.POST, "/interviews/*/final-result")
                                                .hasAuthority("ROLE_HIRING_MANAGER")
                                                .requestMatchers("/interviews", "/interviews/**")
                                                .hasAnyAuthority("ROLE_HR", "ROLE_DIRECTOR")
                                                .requestMatchers(HttpMethod.POST, "/offers/*/approve", "/offers/*/reject")
                                                .hasAuthority("ROLE_DIRECTOR")
                                                .requestMatchers("/offers", "/offers/**")
                                                .hasAnyAuthority("ROLE_HR", "ROLE_DIRECTOR")
                                                // authenticaed require for dashboard notification
                                                .requestMatchers("/dashboard", "/dashboard/**", "/notifications",
                                                                "/notifications/**", "/api/notifications", "/api/notifications/**")
                                                .authenticated()
                                                .requestMatchers("/requisitions", "/requisitions/**")
                                                .hasAnyAuthority("ROLE_HIRING_MANAGER", "ROLE_DIRECTOR", "ROLE_HR")
                                                .requestMatchers("/talent-pool/applications/*/confirm", "/talent-pool/applications/*/decline",
                                                        "/talent-pool/applications/*/resend-email", "/talent-pool/interviews/*/resend-email",
                                                        "/talent-pool/jobs/*/close", "/internal/job-postings/*/close")
                                                .hasAuthority("ROLE_HR")
                                                .requestMatchers("/talent-pool/jobs/*/recommend", "/talent-pool/jobs/*/recommendations/*/withdraw")
                                                .hasAuthority("ROLE_HR")
                                                .requestMatchers("/talent-pool/recommendations/*/decision", "/talent-pool/jobs/*/suggest")
                                                .hasAuthority("ROLE_HIRING_MANAGER")
                                                .requestMatchers("/talent-pool", "/talent-pool/**")
                                                .hasAnyAuthority("ROLE_HR", "ROLE_HIRING_MANAGER")
                                                .requestMatchers("/internal/job-postings/create", "/internal/job-postings/dashboard")
                                                .hasAuthority("ROLE_HR")
                                                // Xem danh sách / chi tiết tin: thêm Director (chỉ xem, không thao tác); service ẩn tin nháp với HM, Director
                                                .requestMatchers(HttpMethod.GET, "/internal/job-postings", "/internal/job-postings/*")
                                                .hasAnyAuthority("ROLE_HR", "ROLE_HIRING_MANAGER", "ROLE_DIRECTOR")
                                                .requestMatchers("/internal/job-postings", "/internal/job-postings/**")
                                                .hasAuthority("ROLE_HR")
                                                // application pipeline: HM scope (own department) is checked in ApplicationAccess
                                                .requestMatchers("/applications", "/applications/**")
                                                .hasAnyAuthority("ROLE_HR", "ROLE_HIRING_MANAGER", "ROLE_DIRECTOR")
                                                .anyRequest().authenticated())
                                // store target url before login
                                .requestCache(cache -> cache.requestCache(requestCache))
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .defaultSuccessUrl("/dashboard", false)
                                                .failureUrl("/login?error")
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .permitAll())
                                .httpBasic(basic -> basic.disable())
                                // add filter before authorization filter
                                .addFilterBefore(new AccountSessionGuardFilter(userRepository),
                                                AuthorizationFilter.class);

                return http.build();
        }
}
