package com.group2.rms.offer.service;

import com.group2.rms.offer.entity.OfferProposal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Gửi email cho luồng Offer: phát hành thư mời nhận việc (Offer Letter) tới ứng viên.
 * Không có cấu hình SMTP thì chỉ ghi log (mock).
 * Thông báo in-app cho nội bộ (Giám đốc, HR) nằm ở {@link com.group2.rms.notification.NotificationService}.
 */
@Slf4j
@Service
public class OfferMailSender {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String baseUrl;

    public OfferMailSender(ObjectProvider<JavaMailSender> mailSender,
                           @Value("${app.mail.from:${APP_MAIL_FROM:noreply@rms.group2.com}}") String from,
                           @Value("${app.base-url:${APP_BASE_URL:http://localhost:8080}}") String baseUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public void sendOfferLetterToCandidate(OfferProposal offer) {
        String candidateEmail = null;
        String candidateName = "Ứng viên";

        if (offer.getApplication() != null && offer.getApplication().getCandidate() != null) {
            var user = offer.getApplication().getCandidate().getAccount();
            if (user != null) {
                candidateEmail = user.getEmail();
                candidateName = user.getFullName();
            }
        }

        // Ưu tiên gửi về email được cấu hình nếu đang dùng hòm thư phát triển cá nhân (from != noreply),
        // đảm bảo người test nhận được email trực tiếp vào hòm thư cá nhân.
        String targetRecipient = (from != null && !from.isBlank() && !from.contains("noreply"))
                ? from
                : candidateEmail;

        if (targetRecipient == null || targetRecipient.isBlank()) {
            log.warn("Không thể gửi thư mời nhận việc: Ứng viên cho Offer ID {} không có email hợp lệ.", offer.getOfferId());
            return;
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(targetRecipient);
                message.setSubject("Thư mời nhận việc - " + offer.getOfferedPositionTitle());
                String deadlineInfo = offer.getResponseDeadline() != null
                        ? "Hạn phản hồi: " + offer.getResponseDeadline().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) + "\n\n"
                        : "\n";
                String officialSalary = offer.getProposedSalary() != null
                        ? String.format("%,d VNĐ/tháng", offer.getProposedSalary().longValue()) : "Thỏa thuận";
                String probationSalary = offer.getProbationSalary() != null
                        ? String.format("%,d VNĐ/tháng", offer.getProbationSalary().longValue()) : "Thỏa thuận";
                message.setText(
                        "Xin chào " + candidateName + ",\n\n"
                        + "Chúng tôi rất vui mừng thông báo bạn đã vượt qua các vòng phỏng vấn và trân trọng gửi tới bạn Thư mời nhận việc cho vị trí: "
                        + offer.getOfferedPositionTitle() + ".\n\n"
                        + "THÔNG TIN ĐÃI NGỘ CHÍNH THỨC:\n"
                        + "• Mức lương chính thức (Trước thuế): " + officialSalary + "\n"
                        + "• Mức lương thử việc (Trước thuế): " + probationSalary + "\n"
                        + "• Địa điểm làm việc: " + (offer.getWorkLocation() != null ? offer.getWorkLocation() : "Văn phòng công ty") + "\n"
                        + deadlineInfo
                        + "Bạn có thể xem chi tiết Thư mời nhận việc và gửi phản hồi trực tiếp tại đường dẫn sau:\n"
                        + baseUrl + "/portal/offers/" + offer.getOfferId() + "\n\n"
                        + "Trân trọng,\nBộ phận Tuyển dụng Mộc RMS"
                );
                sender.send(message);
                log.info("Đã gửi email Offer Letter thành công tới {}", targetRecipient);
            } catch (Exception ex) {
                log.error("Lỗi khi gửi email Offer Letter tới {}: {}", targetRecipient, ex.getMessage());
            }
        } else {
            log.info("[MOCK EMAIL] Gửi Offer Letter tới {} ({}) cho vị trí {} với lương {}. Link: {}/portal/offers/{}",
                    candidateName, targetRecipient, offer.getOfferedPositionTitle(), offer.getProposedSalary(), baseUrl, offer.getOfferId());
        }
    }
}
