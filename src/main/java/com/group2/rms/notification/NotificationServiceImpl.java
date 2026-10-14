package com.group2.rms.notification;

import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service("systemNotificationService")
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void notifyRequisitionRejected(JobRequisition requisition, User director, String comment) {
        if (requisition == null || requisition.getHiringManager() == null) {
            log.warn("Cannot send rejection notification: Requisition or Hiring Manager is null");
            return;
        }

        User recipient = requisition.getHiringManager();
        String decidedTime = requisition.getDecidedAt() != null
            ? requisition.getDecidedAt().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSSS"))
            : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSSS"));
        String eventId = "REQ_REJECT_" + requisition.getRequisitionId() + "_" + decidedTime;

        // Idempotency: Kiểm tra chống trùng lặp nếu retry cùng sự kiện
        if (notificationRepository.existsByEventId(eventId)) {
            log.info("Notification for eventId {} already exists. Skipping duplicate creation.", eventId);
            return;
        }

        String directorName = director != null ? director.getFullName() : "Giám đốc";
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String cleanComment = (comment != null && !comment.isBlank()) ? comment.trim() : "Không có lý do cụ thể";

        String title = "Yêu cầu tuyển dụng đã bị từ chối: " + requisition.getTitle();
        String content = String.format(
            "Yêu cầu tuyển dụng \"%s\" (Mã: REQ-%d) đã bị Giám đốc %s từ chối vào lúc %s.\nLý do: %s.\nVui lòng chỉnh sửa lại thông tin và nộp duyệt lại.",
            requisition.getTitle(),
            requisition.getRequisitionId(),
            directorName,
            timeStr,
            cleanComment
        );

        Notification notification = Notification.builder()
            .recipient(recipient)
            .title(title)
            .content(content)
            .eventType("REQUISITION_REJECTED")
            .referenceId(String.valueOf(requisition.getRequisitionId()))
            .linkUrl("/requisitions/" + requisition.getRequisitionId())
            .isRead(false)
            .eventId(eventId)
            .createdAt(LocalDateTime.now())
            .build();

        notificationRepository.save(notification);
        log.info("Sent rejection notification to Hiring Manager (User ID: {}) for Requisition ID: {}", recipient.getUserId(), requisition.getRequisitionId());
    }

    @Override
    @Transactional
    public void notifyJobPostingPublished(JobPosting posting, User hr) {
        if (posting == null || posting.getRequisition() == null || posting.getRequisition().getHiringManager() == null) {
            log.warn("Cannot send job posting published notification: Posting or Requisition owner is null");
            return;
        }

        User recipient = posting.getRequisition().getHiringManager();
        String postingTimeStr = posting.getPostingDate() != null
                ? posting.getPostingDate().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSSS"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSSS"));
        String eventId = "JOB_PUB_" + posting.getJobPostingId() + "_" + postingTimeStr;

        if (notificationRepository.existsByEventId(eventId)) {
            log.info("Notification for eventId {} already exists. Skipping duplicate creation.", eventId);
            return;
        }

        String hrName = hr != null ? hr.getFullName() : "Nhân sự (HR)";
        String timeDisplay = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String deadlineDisplay = posting.getApplicationDeadline() != null
                ? posting.getApplicationDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "Không giới hạn";

        String reqCode = posting.getRequisition().getRequisitionCode() != null
                ? posting.getRequisition().getRequisitionCode()
                : ("REQ-" + posting.getRequisition().getRequisitionId());

        String title = "Tin tuyển dụng đã phát hành: " + posting.getPostingTitle();
        String content = String.format(
                "Tin tuyển dụng \"%s\" (thuộc yêu cầu %s) đã được nhân sự %s phát hành lên cổng việc làm công khai vào lúc %s.\nHạn nộp hồ sơ: %s.\nỨng viên hiện đã có thể xem chi tiết và nộp hồ sơ trực tuyến.",
                posting.getPostingTitle(),
                reqCode,
                hrName,
                timeDisplay,
                deadlineDisplay
        );

        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .content(content)
                .eventType("JOB_POSTING_PUBLISHED")
                .referenceId(String.valueOf(posting.getJobPostingId()))
                .linkUrl("/internal/job-postings/" + posting.getJobPostingId())
                .isRead(false)
                .eventId(eventId)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        log.info("Sent job posting published notification to Hiring Manager (User ID: {}) for Job Posting ID: {}", recipient.getUserId(), posting.getJobPostingId());
    }

    @Override
    @Transactional
    public void notifyOfferLetterSent(OfferProposal offer) {
        if (offer == null || offer.getApplication() == null
                || offer.getApplication().getCandidate() == null
                || offer.getApplication().getCandidate().getAccount() == null) {
            log.warn("Cannot send offer notification: Candidate or User account is null for Offer ID: {}",
                    offer != null ? offer.getOfferId() : null);
            return;
        }

        User recipient = offer.getApplication().getCandidate().getAccount();
        String eventId = "OFFER_SENT_" + offer.getOfferId();

        if (notificationRepository.existsByEventId(eventId)) {
            log.info("Notification for eventId {} already exists. Skipping duplicate creation.", eventId);
            return;
        }

        String positionTitle = (offer.getOfferedPositionTitle() != null && !offer.getOfferedPositionTitle().isBlank())
                ? offer.getOfferedPositionTitle()
                : "Vị trí tuyển dụng";

        String title = "Bạn có thư mời nhận việc mới: " + positionTitle;
        String content = String.format(
                "Chúc mừng bạn! Nhà tuyển dụng đã gửi Thư mời nhận việc cho vị trí \"%s\". Vui lòng bấm vào đây để xem chi tiết điều khoản và phản hồi thư mời.",
                positionTitle
        );

        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .content(content)
                .eventType("OFFER_LETTER_SENT")
                .referenceId(String.valueOf(offer.getOfferId()))
                .linkUrl("/portal/offers/" + offer.getOfferId())
                .isRead(false)
                .eventId(eventId)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        log.info("Sent offer notification to Candidate (User ID: {}) for Offer ID: {}", recipient.getUserId(), offer.getOfferId());
    }

    @Override
    @Transactional
    public void notifyOfferResponseFromCandidate(OfferProposal offer, boolean accepted, String declineReason) {
        if (offer == null || offer.getProposedBy() == null) {
            log.warn("Cannot send offer response notification: Offer or ProposedBy (HR) is null for Offer ID: {}",
                    offer != null ? offer.getOfferId() : null);
            return;
        }

        User recipient = offer.getProposedBy();
        String eventId = (accepted ? "OFFER_RESP_ACC_" : "OFFER_RESP_DEC_") + offer.getOfferId();

        if (notificationRepository.existsByEventId(eventId)) {
            log.info("Notification for eventId {} already exists. Skipping duplicate creation.", eventId);
            return;
        }

        String positionTitle = (offer.getOfferedPositionTitle() != null && !offer.getOfferedPositionTitle().isBlank())
                ? offer.getOfferedPositionTitle()
                : "Vị trí tuyển dụng";

        String candidateName = "Ứng viên";
        if (offer.getApplication() != null && offer.getApplication().getCandidate() != null) {
            if (offer.getApplication().getCandidate().getAccount() != null
                    && offer.getApplication().getCandidate().getAccount().getFullName() != null) {
                candidateName = offer.getApplication().getCandidate().getAccount().getFullName();
            }
        }

        String title;
        String content;
        String eventType;

        if (accepted) {
            title = String.format("🎉 Ứng viên đã nhận offer: %s (%s)", candidateName, positionTitle);
            content = String.format(
                    "Ứng viên %s đã đồng ý nhận lời mời làm việc cho vị trí \"%s\". Trạng thái hồ sơ đã chuyển sang \"Đã nhận việc\" (Hired). Vui lòng chuẩn bị thủ tục tiếp nhận nhân sự.",
                    candidateName, positionTitle
            );
            eventType = "OFFER_ACCEPTED";
        } else {
            title = String.format("Ứng viên từ chối offer: %s (%s)", candidateName, positionTitle);
            String reasonText = (declineReason != null && !declineReason.isBlank())
                    ? String.format(" Lý do: \"%s\".", declineReason.trim())
                    : "";
            content = String.format(
                    "Ứng viên %s đã từ chối thư mời làm việc cho vị trí \"%s\".%s Trạng thái hồ sơ đã chuyển sang \"Không tiếp tục\" (Rejected).",
                    candidateName, positionTitle, reasonText
            );
            eventType = "OFFER_DECLINED";
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .content(content)
                .eventType(eventType)
                .referenceId(String.valueOf(offer.getOfferId()))
                .linkUrl("/offers")
                .isRead(false)
                .eventId(eventId)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        log.info("Sent candidate offer response notification ({}) to HR (User ID: {}) for Offer ID: {}",
                eventType, recipient.getUserId(), offer.getOfferId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsForUser(User user, int page, int size) {
        if (user == null) {
            return Page.empty();
        }
        int validPage = Math.max(0, page - 1);
        int validSize = Math.max(1, Math.min(size, 50));
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "";
        boolean isHrOrAdmin = "HR".equalsIgnoreCase(roleName) || "System Admin".equalsIgnoreCase(roleName);

        return notificationRepository.findByRecipient_UserIdOrderByCreatedAtDesc(user.getUserId(), PageRequest.of(validPage, validSize))
            .map(n -> NotificationResponse.from(n, isHrOrAdmin));
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        if (user == null) {
            return 0;
        }
        return notificationRepository.countByRecipient_UserIdAndIsReadFalse(user.getUserId());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, User user) {
        if (notificationId == null || user == null) {
            return;
        }
        Notification notification = notificationRepository.findByNotificationIdAndRecipient_UserId(notificationId, user.getUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thông báo không tồn tại hoặc không thuộc về bạn."));
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(User user) {
        if (user == null) {
            return;
        }
        notificationRepository.markAllAsReadByUserId(user.getUserId());
    }

    @Override
    @Transactional
    public void markOfferNotificationRead(Integer userId, Integer offerId) {
        if (userId != null && offerId != null) {
            notificationRepository.markOfferNotificationRead(userId, String.valueOf(offerId));
        }
    }
}
