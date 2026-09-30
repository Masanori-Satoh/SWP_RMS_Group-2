package com.group2.rms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point của ứng dụng Recruitment Management System.
 * Nhấn nút ▶️ (Run) ngay cạnh class này trong IntelliJ để khởi động.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = {"com.group2.rms", "com.rms"})
public class RmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RmsApplication.class, args);
    }
}
