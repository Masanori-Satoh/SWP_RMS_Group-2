package com.group2.rms.offer.service.impl;

import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.offer.service.NotificationService;
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

    @Override
    public void notifyDirectorNewPendingOffer(OfferProposal offer) {
        // Trong thực tế, có thể dùng UserRepository để lấy danh sách email của tất cả Director.
        // Ở đây giả lập gửi tới danh sách Ban Giám đốc.
        String candidateName = (offer.getApplication() != null && offer.getApplication().getCandidate() != null)
                ? offer.getApplication().getCandidate().getAccount().getFullName() : "N/A";
        
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo("director@rms-tech.vn"); // Địa chỉ email chung hoặc danh sách các Giám đốc
                message.setSubject("Yêu cầu phê duyệt Đề xuất Offer - " + candidateName);
                message.setText(
                        "Kính gửi Ban Giám đốc,\n\n"
                        + "Hệ thống Mộc RMS vừa nhận được một Đề xuất Offer mới cần Ban Giám đốc xem xét và phê duyệt.\n"
                        + "- Ứng viên: " + candidateName + "\n"
                        + "- Vị trí đề xuất: " + offer.getOfferedPositionTitle() + "\n"
                        + "- Mức lương chính thức: " + offer.getProposedSalary() + " VND/tháng\n\n"
                        + "Vui lòng đăng nhập vào hệ thống để xem chi tiết và thực hiện phê duyệt/từ chối.\n\n"
                        + "Trân trọng,\nHệ thống Mộc RMS"
                );
                sender.send(message);
                log.info("Đã gửi email yêu cầu phê duyệt Offer (ID: {}) tới Ban Giám đốc.", offer.getOfferId());
            } catch (Exception ex) {
                log.error("Lỗi khi gửi email yêu cầu duyệt Offer tới Director: {}", ex.getMessage());
            }
        } else {
            log.info("[MOCK EMAIL] Gửi yêu cầu duyệt Offer (ID: {}) tới Ban Giám đốc. Ứng viên: {}", 
                     offer.getOfferId(), candidateName);
        }
    }

    @Override
    public void notifyHrOfDirectorDecision(OfferProposal offer, String decision, String comments) {
        String hrEmail = null;
        String hrName = "Chuyên viên HR";
        
        if (offer.getProposedBy() != null) {
            hrEmail = offer.getProposedBy().getEmail();
            hrName = offer.getProposedBy().getFullName();
        }

        if (hrEmail == null || hrEmail.isBlank()) {
            log.warn("Không thể gửi thông báo quyết định Offer: HR tạo đề xuất (Offer ID {}) không có email hợp lệ.", offer.getOfferId());
            return;
        }

        String statusVn = "Approved".equalsIgnoreCase(decision) ? "PHÊ DUYỆT" : "TỪ CHỐI";
        String candidateName = (offer.getApplication() != null && offer.getApplication().getCandidate() != null)
                ? offer.getApplication().getCandidate().getAccount().getFullName() : "N/A";

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(hrEmail);
                message.setSubject("Kết quả phê duyệt Đề xuất Offer - " + candidateName);
                message.setText(
                        "Chào " + hrName + ",\n\n"
                        + "Đề xuất Offer của ứng viên " + candidateName + " cho vị trí " + offer.getOfferedPositionTitle() + " đã có kết quả từ Ban Giám đốc.\n\n"
                        + "Quyết định: " + statusVn + "\n"
                        + "Ghi chú của Giám đốc: " + (comments != null && !comments.isBlank() ? comments : "Không có") + "\n\n"
                        + "Vui lòng đăng nhập vào hệ thống Mộc RMS để thực hiện các bước tiếp theo.\n\n"
                        + "Trân trọng,\nHệ thống Mộc RMS"
                );
                sender.send(message);
                log.info("Đã gửi email kết quả duyệt Offer (ID: {}) tới HR {}.", offer.getOfferId(), hrEmail);
            } catch (Exception ex) {
                log.error("Lỗi khi gửi email kết quả duyệt Offer tới {}: {}", hrEmail, ex.getMessage());
            }
        } else {
            log.info("[MOCK EMAIL] Gửi kết quả duyệt Offer ({}) tới HR {} ({}): Quyết định = {}", 
                     offer.getOfferId(), hrName, hrEmail, statusVn);
        }
    }
}
