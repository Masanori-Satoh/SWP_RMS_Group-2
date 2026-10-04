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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

/**
 * Shared authentication foundation and the route rules confirmed for LinhDN.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
                .authorizeHttpRequests(auth -> auth
                        // permit all for static resources
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        // permit all for icon and error
                        .requestMatchers("/favicon.ico", "/error").permitAll()
                        // permit all for defaultpage and font
                        .requestMatchers(HttpMethod.GET, "/", "/fonts/**").permitAll()
                        // permit all for login, register, forgot-password, reset-password
                        .requestMatchers("/login", "/register", "/forgot-password", "/reset-password/**").permitAll()
                        // required role canididate for job apply
                        .requestMatchers("/jobs/*/apply", "/jobs/*/apply/**").hasAuthority("ROLE_CANDIDATE")
                        // permit all for job and public job
                        .requestMatchers(HttpMethod.GET, "/jobs", "/jobs/**", "/public/jobs", "/public/jobs/**")
                        .permitAll()
                        // requrired role system admin for admin pages
                        // account
                        .requestMatchers("/admin/accounts", "/admin/accounts/**",
                                // department
                                "/admin/departments", "/admin/departments/**",
                                // candidate account
                                "/admin/candidate-accounts", "/admin/candidate-accounts/**",
                                // api
                                "/admin/api-monitoring", "/admin/api-monitoring/**",
                                // api config
                                "/admin/ai-configuration", "/admin/ai-configuration/**")
                        .hasAuthority(RoleAuthorities.SYSTEM_ADMIN)
                        // authenticaed require for dashboard
                        .requestMatchers("/dashboard", "/dashboard/**").authenticated()
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
                .addFilterBefore(new AccountSessionGuardFilter(userRepository), AuthorizationFilter.class);

        return http.build();
    }
}
