package com.rms.feature.notification;

import com.group2.rms.entity.OfferNegotiation;
import com.group2.rms.entity.OfferProposal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Triển khai NotificationService thực hiện gửi thông báo bất đồng bộ (@Async).
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Async
    @Override
    public void sendOfferSubmittedNotificationToDirector(OfferProposal offer) {
        try {
            log.info("[NOTIFICATION -> DIRECTOR] Đề xuất Offer ID {} cho vị trí '{}' (Lương đề xuất: {}, Lương thử việc: {}) đã được tạo và đang chờ Director phê duyệt.",
                    offer.getOfferId(), offer.getProposedPosition(), offer.getProposedSalary(), offer.getProbationSalary());
        } catch (Exception ex) {
            log.error("[NOTIFICATION -> DIRECTOR] Lỗi gửi thông báo cho Offer ID {}: {}", offer.getOfferId(), ex.getMessage());
        }
    }

    @Async
    @Override
    public void sendOfferApprovedNotificationToHR(OfferProposal offer) {
        try {
            String candidateName = (offer.getApplication() != null && offer.getApplication().getCandidate() != null)
                    ? offer.getApplication().getCandidate().getFullName() : "Ứng viên";
            String position = offer.getProposedPosition() != null ? offer.getProposedPosition() : "Vị trí tuyển dụng";

            log.info("[NOTIFICATION -> HR] Giám đốc đã phê duyệt đề xuất Offer ID #{} cho ứng viên {} (Vị trí: '{}'). "
                            + "Đề nghị Bộ phận Nhân sự (HR) kiểm tra và bấm 'Phát hành Offer' để gửi thư mời làm việc chính thức tới ứng viên.",
                    offer.getOfferId(), candidateName, position);
        } catch (Exception ex) {
            log.error("[NOTIFICATION -> HR] Lỗi gửi thông báo phê duyệt tới HR cho Offer ID {}: {}", offer.getOfferId(), ex.getMessage());
        }
    }

    @Async
    @Override
    public void sendOfferApprovedNotificationToCandidate(OfferProposal offer) {
        try {
            String candidateName = (offer.getApplication() != null && offer.getApplication().getCandidate() != null)
                    ? offer.getApplication().getCandidate().getFullName() : "Ứng viên";
            String candidateEmail = (offer.getApplication() != null && offer.getApplication().getCandidate() != null)
                    ? offer.getApplication().getCandidate().getEmail() : "N/A";

            log.info("[EMAIL -> CANDIDATE] Đã gửi Offer Letter tới ứng viên {} ({}) cho vị trí '{}'. Mức lương chính thức: {}, Lương thử việc: {}.",
                    candidateName, candidateEmail, offer.getProposedPosition(), offer.getProposedSalary(), offer.getProbationSalary());
        } catch (Exception ex) {
            log.error("[EMAIL -> CANDIDATE] Lỗi gửi email thư mời cho Offer ID {}: {}", offer.getOfferId(), ex.getMessage());
        }
    }

    @Async
    @Override
    public void sendCandidateResponseNotificationToHR(OfferProposal offer, String action) {
        try {
            String hrName = (offer.getProposedBy() != null) ? offer.getProposedBy().getFullName() : "HR / Hiring Manager";
            String candidateName = (offer.getApplication() != null && offer.getApplication().getCandidate() != null)
                    ? offer.getApplication().getCandidate().getFullName() : "Ứng viên";

            log.info("[NOTIFICATION -> HR] Thông báo tới {}: Ứng viên {} đã gửi phản hồi '{}' cho Offer ID {}.",
                    hrName, candidateName, action, offer.getOfferId());
        } catch (Exception ex) {
            log.error("[NOTIFICATION -> HR] Lỗi gửi thông báo cho Offer ID {}: {}", offer.getOfferId(), ex.getMessage());
        }
    }

    @Async
    @Override
    public void sendHrNegotiationResponseNotification(OfferProposal offer, String hrResponseNotes) {
        try {
            String candidateName = (offer.getApplication() != null && offer.getApplication().getCandidate() != null)
                    ? offer.getApplication().getCandidate().getFullName() : "Ứng viên";
            String hmName = (offer.getProposedBy() != null) ? offer.getProposedBy().getFullName() : "Hiring Manager";

            log.info("[NOTIFICATION] HR đã phản hồi yêu cầu thương lượng cho ứng viên {} thuộc Offer ID {}. HM liên quan: {}. Ghi chú: {}",
                    candidateName, offer.getOfferId(), hmName, hrResponseNotes);
        } catch (Exception ex) {
            log.error("[NOTIFICATION] Lỗi gửi thông báo phản hồi thương lượng cho Offer ID {}: {}", offer.getOfferId(), ex.getMessage());
        }
    }

    @Async
    @Override
    public void sendHrNegotiationApprovedNotificationToHM(OfferProposal offer, OfferNegotiation negotiation, String hrNotes) {
        try {
            String candidateName = (offer.getApplication() != null && offer.getApplication().getCandidate() != null)
                    ? offer.getApplication().getCandidate().getFullName() : "Ứng viên";
            String hmName = (offer.getProposedBy() != null) ? offer.getProposedBy().getFullName() : "Hiring Manager";
            String counterSalaryStr = negotiation.getCandidateCounterSalary() != null
                    ? String.format("%,.0f VNĐ", negotiation.getCandidateCounterSalary()) : "Chưa xác định";

            log.info("[NOTIFICATION -> HM] HR đã DUYỆT yêu cầu deal lương của ứng viên {} (Offer ID #{}). "
                            + "Mức lương ứng viên đề xuất: {}. Ý kiến từ HR: '{}'. "
                            + "Thông báo chuyển tới Quản lý tuyển dụng ({}) để cập nhật lại đề xuất Offer với mức lương mới và trình Giám đốc phê duyệt lần cuối.",
                    candidateName, offer.getOfferId(), counterSalaryStr, hrNotes, hmName);
        } catch (Exception ex) {
            log.error("[NOTIFICATION -> HM] Lỗi gửi thông báo duyệt thương lượng tới HM cho Offer ID {}: {}", offer.getOfferId(), ex.getMessage());
        }
    }

    @Async
    @Override
    public void sendHrNegotiationRejectedNotificationToCandidate(OfferProposal offer, String hrNotes) {
        try {
            String candidateName = (offer.getApplication() != null && offer.getApplication().getCandidate() != null)
                    ? offer.getApplication().getCandidate().getFullName() : "Ứng viên";
            String candidateEmail = (offer.getApplication() != null && offer.getApplication().getCandidate() != null)
                    ? offer.getApplication().getCandidate().getEmail() : "N/A";

            log.info("[EMAIL -> CANDIDATE] Thông báo phản hồi đàm phán gửi tới ứng viên {} ({}): "
                            + "Phòng Nhân sự đã TỪ CHỐI yêu cầu thương lượng mức lương cho Offer ID #{} với lý do: '{}'. "
                            + "Thư mời làm việc được giữ nguyên theo các điều khoản ban đầu. Ứng viên có thể tiếp tục xem xét và đưa ra quyết định trên hệ thống.",
                    candidateName, candidateEmail, offer.getOfferId(), hrNotes);
        } catch (Exception ex) {
            log.error("[EMAIL -> CANDIDATE] Lỗi gửi thông báo từ chối thương lượng tới ứng viên cho Offer ID {}: {}", offer.getOfferId(), ex.getMessage());
        }
    }

    @Async
    @Override
    public void sendHiringConfirmedNotificationToHR(OfferProposal offer) {
        try {
            String candidateName = (offer.getApplication() != null && offer.getApplication().getCandidate() != null)
                    ? offer.getApplication().getCandidate().getFullName() : "Ứng viên";
            String candidateEmail = (offer.getApplication() != null && offer.getApplication().getCandidate() != null)
                    ? offer.getApplication().getCandidate().getEmail() : "N/A";
            String hmName = (offer.getProposedBy() != null) ? offer.getProposedBy().getFullName() : "Hiring Manager";
            String position = offer.getProposedPosition() != null ? offer.getProposedPosition() : "Vị trí tuyển dụng";

            log.info("[NOTIFICATION -> HR] Hiring Manager ({}) đã xác nhận Tuyển dụng ứng viên {} ({}) cho vị trí '{}' (Offer ID #{}). "
                            + "Quá trình tuyển dụng của ứng viên đã chính thức kết thúc! Đề nghị Bộ phận Nhân sự (HR) bắt đầu quy trình tiếp nhận Onboarding và chuẩn bị hợp đồng lao động.",
                    hmName, candidateName, candidateEmail, position, offer.getOfferId());
        } catch (Exception ex) {
            log.error("[NOTIFICATION -> HR] Lỗi gửi thông báo tuyển dụng thành công tới HR cho Offer ID {}: {}", offer.getOfferId(), ex.getMessage());
        }
    }

    @Async
    @Override
    public void sendHiringConfirmedNotificationToCandidate(OfferProposal offer) {
        try {
            String candidateName = (offer.getApplication() != null && offer.getApplication().getCandidate() != null)
                    ? offer.getApplication().getCandidate().getFullName() : "Ứng viên";
            String candidateEmail = (offer.getApplication() != null && offer.getApplication().getCandidate() != null)
                    ? offer.getApplication().getCandidate().getEmail() : "N/A";
            String position = offer.getProposedPosition() != null ? offer.getProposedPosition() : "Vị trí tuyển dụng";

            log.info("[EMAIL -> CANDIDATE] Đã gửi thông báo chúc mừng trúng tuyển chính thức tới ứng viên {} ({}) cho vị trí '{}' (Offer ID #{}). "
                            + "Chúc mừng bạn đã hoàn tất xuất sắc quá trình tuyển dụng và chính thức trở thành nhân viên! Bộ phận Nhân sự sẽ sớm liên hệ để hoàn tất thủ tục Onboarding.",
                    candidateName, candidateEmail, position, offer.getOfferId());
        } catch (Exception ex) {
            log.error("[EMAIL -> CANDIDATE] Lỗi gửi email thông báo trúng tuyển tới ứng viên cho Offer ID {}: {}", offer.getOfferId(), ex.getMessage());
        }
    }
}

