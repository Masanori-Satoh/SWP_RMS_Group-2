package com.group2.rms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Tạm thời cho phép truy cập tất cả các đường dẫn để test giao diện
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            // Tắt CSRF tạm thời để dễ test form (sẽ bật lại sau)
            .csrf(csrf -> csrf.disable());
            
        return http.build();
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}
