package com.group2.rms.core.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigTests {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mailRunsOnBackgroundThreadWithTheCallersLogin() throws Exception {
        Authentication login = UsernamePasswordAuthenticationToken.authenticated("hr_lan", "n/a", java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(login);

        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            String[] seen = context.getBean(Probe.class).capture().get().split("\\|");

            assertTrue(seen[0].startsWith("recruitment-mail-"));
            assertEquals("hr_lan", seen[1]);
        }
    }

    @Configuration
    @Import(AsyncConfig.class)
    static class TestConfig {
        @Bean
        Probe probe() {
            return new Probe();
        }
    }

    static class Probe {
        @Async(AsyncConfig.RECRUITMENT_MAIL_EXECUTOR)
        public CompletableFuture<String> capture() {
            return CompletableFuture.completedFuture(Thread.currentThread().getName() + "|"
                    + SecurityContextHolder.getContext().getAuthentication().getName());
        }
    }
}
