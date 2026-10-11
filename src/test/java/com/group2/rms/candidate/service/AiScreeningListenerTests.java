package com.group2.rms.candidate.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Nối dây thật của Spring ({@code @Async} + {@code @TransactionalEventListener}), không cần DB:
 * transaction manager giả chỉ để Spring chạy các bước commit/rollback.
 */
@SpringJUnitConfig(AiScreeningListenerTests.Config.class)
class AiScreeningListenerTests {

    private static final int APPLICATION_ID = 42;
    private static final long WAIT_MS = 2000;

    @Autowired ApplicationEventPublisher events;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean AiScreeningService aiScreeningService;

    @Test
    void screensInBackgroundOnlyAfterCommit() {
        AtomicReference<Thread> screeningThread = new AtomicReference<>();
        when(aiScreeningService.screen(APPLICATION_ID)).thenAnswer(inv -> {
            screeningThread.set(Thread.currentThread());
            return new AiScreeningOutcome(BigDecimal.TEN);
        });

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            events.publishEvent(new ApplicationSubmittedEvent(APPLICATION_ID));
            verifyNoInteractions(aiScreeningService);
        });

        verify(aiScreeningService, timeout(WAIT_MS)).screen(APPLICATION_ID);
        assertNotSame(Thread.currentThread(), screeningThread.get());
    }

    @Test
    void skipsScreeningWhenSubmissionRollsBack() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            events.publishEvent(new ApplicationSubmittedEvent(APPLICATION_ID));
            status.setRollbackOnly();
        });

        verify(aiScreeningService, after(500).never()).screen(any());
    }

    @Test
    void screeningFailureIsLoggedNotThrown() {
        AiScreeningService failing = mock(AiScreeningService.class);
        when(failing.screen(APPLICATION_ID)).thenThrow(new IllegalStateException("AI down"));

        assertDoesNotThrow(() -> new AiScreeningListener(failing)
                .onApplicationSubmitted(new ApplicationSubmittedEvent(APPLICATION_ID)));
    }

    /** Giống app thật: Spring Boot tự bật quản lý transaction; {@code @EnableAsync} bật ở {@code RmsApplication}. */
    @Configuration
    @EnableAsync
    @EnableTransactionManagement
    @Import(AiScreeningListener.class)
    static class Config {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }
    }
}
