package com.rms.feature.offer.job;

import com.group2.rms.entity.OfferProposal;
import com.group2.rms.repository.OfferProposalRepository;
import com.rms.feature.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tác vụ ngầm tự động hết hạn Offer (JOB-03: Auto-expire pending offers).
 * Quét các OfferProposal ở trạng thái chờ duyệt hoặc đã gửi ứng viên quá 7 ngày,
 * tự động cập nhật trạng thái thành "Expired" và lưu vết kiểm toán.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoExpireOffersJob {

    private final OfferProposalRepository offerProposalRepository;
    private final AuditLogService auditLogService;

    /**
     * Chạy định kỳ vào 01:00 AM hàng ngày.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void autoExpirePendingOffers() {
        log.info("[JOB-03] Bắt đầu tiến trình kiểm tra OfferProposal hết hạn...");

        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        List<String> pendingStatuses = List.of(
                "Sent_Candidate",
                "Sent_To_Candidate",
                "Sent",
                "Pending_Director",
                "Pending_Director_Approval"
        );

        List<OfferProposal> expiredOffers = offerProposalRepository.findExpiredOffers(pendingStatuses, threshold);

        if (expiredOffers.isEmpty()) {
            log.info("[JOB-03] Không có Offer nào quá hạn.");
            return;
        }

        int count = 0;
        for (OfferProposal offer : expiredOffers) {
            String oldStatus = offer.getOfferStatus();
            offer.setOfferStatus("Expired");
            offerProposalRepository.save(offer);

            log.warn("[JOB-03] Offer ID {} đã hết hạn (Trạng thái cũ: '{}', Tạo lúc: {}). Cập nhật sang 'Expired'.",
                    offer.getOfferId(), oldStatus, offer.getCreatedAt());

            Integer userId = (offer.getProposedBy() != null) ? offer.getProposedBy().getUserId() : 1;
            auditLogService.log(
                    userId,
                    "AUTO_EXPIRE",
                    "OfferProposal",
                    offer.getOfferId().toString(),
                    "Status: " + oldStatus + ", ProposedSalary: " + offer.getProposedSalary(),
                    "Status: Expired (JOB-03 auto-expired after 7 days)"
            );
            count++;
        }

        log.info("[JOB-03] Đã xử lý hết hạn thành công cho {} OfferProposal.", count);
    }
}
