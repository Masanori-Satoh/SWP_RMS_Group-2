package com.group2.rms.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Đồng hồ dùng chung cho cả hệ thống.
 *
 * Service / validator tiêm {@link Clock} rồi gọi {@code LocalDate.now(clock)} thay cho {@code LocalDate.now()}.
 * Khi chạy thật thì giống hệt giờ máy chủ; khi test thì truyền {@code Clock.fixed(...)} để
 * kiểm tra đúng một ngày cố định (test không còn lỗi ngẫu nhiên khi chạy lúc qua nửa đêm).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
