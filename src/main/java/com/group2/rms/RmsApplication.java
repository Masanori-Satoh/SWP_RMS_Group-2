package com.group2.rms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point của ứng dụng Recruitment Management System.

 */
@SpringBootApplication
@EnableAsync
public class RmsApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(RmsApplication.class, args);
    }
}
