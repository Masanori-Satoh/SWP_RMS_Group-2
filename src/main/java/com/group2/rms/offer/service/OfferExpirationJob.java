package com.group2.rms.offer.service;

import com.group2.rms.offer.repository.OfferProposalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Chốt trạng thái Offer quá hạn mỗi phút, độc lập với cơ chế scheduling của module khác.
 * Tắt mặc định đến khi các module đọc Offer được duyệt tương thích; bật bằng app.offer.expiration.enabled=true.
 * Chỉ khởi động khi có web server thực; MockMvc/DataJpaTest không tự chạy job.
 * Job cập nhật trạng thái hiển thị, không thay thế kiểm tra hạn nguyên tử của thao tác phản hồi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.offer.expiration.enabled", havingValue = "true", matchIfMissing = false)
public class OfferExpirationJob implements DisposableBean {
    private final OfferProposalRepository offers;
    private final PlatformTransactionManager transactions;
    private final Clock clock;
    private ScheduledExecutorService executor;

    @EventListener
    public synchronized void onWebServerStarted(WebServerInitializedEvent event) {
        if (executor != null) return;
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "offer-expiration");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(() -> {
            try { expireNow(); }
            catch (RuntimeException error) { log.warn("Không thể cập nhật Offer quá hạn: {}", error.getClass().getSimpleName()); }
        }, 0, 60, TimeUnit.SECONDS);
    }

    /** Chỉ Sent_Candidate chưa xóa và đã quá hạn mới đổi; chạy lại không làm thay đổi kết quả đã chốt. */
    public int expireNow() {
        LocalDateTime now = LocalDateTime.now(clock);
        Integer updated = new TransactionTemplate(transactions).execute(status -> offers.expireDue(now, now.toLocalDate()));
        return updated == null ? 0 : updated;
    }

    @Override
    public synchronized void destroy() {
        if (executor != null) executor.shutdownNow();
    }
}
