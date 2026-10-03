package com.group2.rms.service.impl;

import com.group2.rms.offer.OfferProposal;
import com.group2.rms.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public NotificationServiceImpl(ObjectProvider<JavaMailSender> mailSender,
                                   @Value("${APP_MAIL_FROM:noreply@rms.group2.com}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
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

        if (candidateEmail == null || candidateEmail.isBlank()) {
            log.warn("Không thể gửi thư mời nhận việc: Ứng viên cho Offer ID {} không có email hợp lệ.", offer.getOfferId());
            return;
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(candidateEmail);
                message.setSubject("Thư mời nhận việc (Job Offer Letter) - " + offer.getOfferedPositionTitle());
                message.setText(
                        "Xin chào " + candidateName + ",\n\n"
                        + "Chúng tôi rất vui mừng thông báo bạn đã vượt qua các vòng phỏng vấn và trân trọng gửi tới bạn Thư mời nhận việc cho vị trí: "
                        + offer.getOfferedPositionTitle() + ".\n"
                        + "Mức lương chính thức: " + offer.getProposedSalary() + " VND/tháng\n"
                        + "Mức lương thử việc: " + offer.getProbationSalary() + " VND/tháng\n"
                        + "Địa điểm làm việc: " + (offer.getWorkLocation() != null ? offer.getWorkLocation() : "Văn phòng công ty") + "\n\n"
                        + "Vui lòng đăng nhập vào hệ thống RMS để xem chi tiết và phản hồi thư mời làm việc.\n\n"
                        + "Trân trọng,\nPhòng Nhân sự RMS"
                );
                sender.send(message);
                log.info("Đã gửi email Offer Letter thành công tới {}", candidateEmail);
            } catch (Exception ex) {
                log.error("Lỗi khi gửi email Offer Letter tới {}: {}", candidateEmail, ex.getMessage());
            }
        } else {
            log.info("[MOCK EMAIL] Gửi Offer Letter tới {} ({}) cho vị trí {} với lương {}",
                    candidateName, candidateEmail, offer.getOfferedPositionTitle(), offer.getProposedSalary());
        }
    }
}
