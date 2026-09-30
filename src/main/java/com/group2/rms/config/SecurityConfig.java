package com.group2.rms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình Spring Security.
 * - Khai báo bean PasswordEncoder (BCrypt) để mã hoá mật khẩu.
 * - Tạm thời cho phép TẤT CẢ request truy cập (permitAll) để dễ phát triển.
 *   Khi triển khai authentication/authorization thật, sẽ siết lại ở đây.
 */
@Configuration
@EnableWebSecurity
// @EnableMethodSecurity: Tạm tắt để cho phép test các luồng API trong giai đoạn phát triển (permitAll)
public class SecurityConfig {

    /**
     * Bean PasswordEncoder sử dụng thuật toán BCrypt.
     * Được inject vào DatabaseSeeder và các Service cần mã hoá password.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cấu hình SecurityFilterChain.
     * Giai đoạn phát triển: cho phép tất cả request, tắt CSRF.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
