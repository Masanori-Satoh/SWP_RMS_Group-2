package com.group2.rms.offer.service;

import com.group2.rms.offer.repository.OfferProposalRepository;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Chốt trạng thái Offer quá hạn mỗi phút, độc lập với cơ chế scheduling của module khác.
 * Tắt mặc định đến khi các module đọc Offer được duyệt tương thích; bật bằng app.offer.expiration.enabled=true.
 * Chỉ khởi động khi có web server thực; MockMvc/DataJpaTest không tự chạy job.
 * Job cập nhật trạng thái hiển thị, không thay thế kiểm tra hạn nguyên tử của thao tác phản hồi.
 * Thông báo HR được tạo trong transaction mới sau commit; một sự kiện hết hạn không tạo thông báo trùng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.offer.expiration.enabled", havingValue = "true", matchIfMissing = false)
public class OfferExpirationJob implements DisposableBean {
    private static final int BATCH_SIZE = 100;
    private final OfferProposalRepository offers;
    private final PlatformTransactionManager transactions;
    private final Clock clock;
    private final NotificationService systemNotificationService;
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
        int total = 0;
        int updated;
        do {
            Integer result = new TransactionTemplate(transactions).execute(status -> expireBatch(now));
            updated = result == null ? 0 : result;
            total += updated;
        } while (updated == BATCH_SIZE);
        return total;
    }

    private int expireBatch(LocalDateTime now) {
        List<Integer> expiredIds = new ArrayList<>();
        for (OfferProposal offer : offers.findDueForUpdate(now, now.toLocalDate(), PageRequest.of(0, BATCH_SIZE))) {
            if (!"Sent_Candidate".equals(offer.getOfferStatus()) || Boolean.TRUE.equals(offer.getIsDeleted())
                    || !offer.isResponseExpiredAt(now)) {
                continue;
            }
            offer.setOfferStatus("Expired");
            offers.save(offer);
            expiredIds.add(offer.getOfferId());
        }
        if (!expiredIds.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    expiredIds.forEach(OfferExpirationJob.this::notifyExpired);
                }
            });
        }
        return expiredIds.size();
    }

    /** Đọc lại trong transaction mới; lỗi thông báo không đảo ngược trạng thái Expired đã commit. */
    private void notifyExpired(Integer offerId) {
        try {
            TransactionTemplate notificationTransaction = new TransactionTemplate(transactions);
            notificationTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            notificationTransaction.executeWithoutResult(status -> offers.findActiveForUpdate(offerId)
                    .filter(offer -> "Expired".equals(offer.getOfferStatus()))
                    .ifPresent(systemNotificationService::notifyOfferExpired));
        } catch (RuntimeException error) {
            log.warn("Không thể gửi thông báo hết hạn cho Offer {}", offerId, error);
        }
    }

    @Override
    public synchronized void destroy() {
        if (executor != null) executor.shutdownNow();
    }
}
