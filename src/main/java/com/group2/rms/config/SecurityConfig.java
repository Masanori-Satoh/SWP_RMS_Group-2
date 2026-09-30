package com.group2.rms.config;

import com.group2.rms.repository.UserRepository;
import com.group2.rms.security.AccountSessionGuardFilter;
import com.group2.rms.security.DatabaseUserDetailsService;
import com.group2.rms.security.RoleAuthorities;
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

/** Shared authentication foundation and the route rules confirmed for LinhDN. */
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
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        http
            .authenticationProvider(provider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers("/favicon.ico", "/error").permitAll()
                .requestMatchers("/login", "/register", "/forgot-password", "/reset-password/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/jobs", "/jobs/**", "/public/jobs", "/public/jobs/**").permitAll()
                .requestMatchers("/admin/accounts", "/admin/accounts/**",
                        "/admin/api-monitoring", "/admin/api-monitoring/**",
                        "/admin/ai-configuration", "/admin/ai-configuration/**")
                    .hasAuthority(RoleAuthorities.SYSTEM_ADMIN)
                .requestMatchers("/dashboard", "/dashboard/**").authenticated()
                .requestMatchers("/requisitions", "/requisitions/**")
                    .hasAnyAuthority("ROLE_HIRING_MANAGER", "ROLE_DIRECTOR", "ROLE_HR", RoleAuthorities.SYSTEM_ADMIN)
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .httpBasic(basic -> basic.disable())
            .addFilterBefore(new AccountSessionGuardFilter(userRepository), AuthorizationFilter.class);

        // Spring Security's default CSRF protection stays enabled for web POST forms.
        return http.build();
    }
}
