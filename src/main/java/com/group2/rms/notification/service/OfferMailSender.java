package com.group2.rms.notification.service;

import com.group2.rms.offer.entity.OfferProposal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/*
 * MAIL: gửi thư mời nhận việc tới ứng viên; không cấu hình SMTP thì chỉ ghi log.
 */
@Slf4j
@Service
public class OfferMailSender {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public OfferMailSender(ObjectProvider<JavaMailSender> mailSender,
                           @Value("${app.mail.from:${APP_MAIL_FROM:noreply@rms.group2.com}}") String from) {
        this.mailSender = mailSender;
        this.from = from;
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

        // Hòm thư phát triển cá nhân (from khác noreply) thì gửi về địa chỉ cấu hình để người test nhận được
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
                        ? "Hạn phản hồi: " + offer.getResponseDeadline().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) + "\n\n"
                        : "\n";
                String officialSalary = offer.getProposedSalary() != null
                        ? String.format("%,d VNĐ/tháng", offer.getProposedSalary().longValue()) : "Thỏa thuận";
                String probationSalary = offer.getProbationSalary() != null
                        ? String.format("%,d VNĐ/tháng", offer.getProbationSalary().longValue()) : "Thỏa thuận";
                message.setText(
                        "Xin chào " + candidateName + ",\n\n"
                        + "Chúng tôi rất vui mừng thông báo bạn đã vượt qua các vòng phỏng vấn và trân trọng gửi tới bạn Thư mời nhận việc cho vị trí: "
                        + offer.getOfferedPositionTitle() + ".\n"
                        + "Mức lương chính thức (Trước thuế): " + officialSalary + "\n"
                        + "Mức lương thử việc (Trước thuế): " + probationSalary + "\n"
                        + "Địa điểm làm việc: " + (offer.getWorkLocation() != null ? offer.getWorkLocation() : "Văn phòng công ty") + "\n"
                        + deadlineInfo
                        + "Vui lòng đăng nhập vào hệ thống để xem chi tiết và phản hồi thư mời nhận việc.\n\n"
                        + "Trân trọng,\nBộ phận Tuyển dụng Mộc RMS"
                );
                sender.send(message);
                log.info("Đã gửi email Offer Letter thành công tới {}", targetRecipient);
            } catch (Exception ex) {
                log.error("Lỗi khi gửi email Offer Letter tới {}: {}", targetRecipient, ex.getMessage());
            }
        } else {
            log.info("[MOCK EMAIL] Gửi Offer Letter tới {} ({}) cho vị trí {} với lương {}",
                    candidateName, targetRecipient, offer.getOfferedPositionTitle(), offer.getProposedSalary());
        }
    }
}
