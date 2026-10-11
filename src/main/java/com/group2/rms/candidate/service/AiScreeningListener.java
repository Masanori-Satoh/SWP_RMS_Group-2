package com.group2.rms.candidate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Chấm đơn mới nộp ở luồng nền, chỉ sau khi đơn đã lưu thật (transaction nộp đơn đã commit).
 * Lỗi chỉ ghi log: đơn giữ {@code Applied}, HR chấm lại sau.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiScreeningListener {

    private final AiScreeningService aiScreeningService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        try {
            aiScreeningService.screen(event.applicationId());
        } catch (RuntimeException e) {
            log.warn("AI screening failed for application {}; it stays 'Applied'", event.applicationId(), e);
        }
    }
}
