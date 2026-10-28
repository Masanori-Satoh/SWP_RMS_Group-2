package com.group2.rms.notification;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Thông báo in-app. Mọi phương thức notify* chạy trong transaction của nghiệp vụ gọi tới
 * (không REQUIRES_NEW) để nghiệp vụ rollback thì thông báo cũng không được ghi.
 * Chống trùng bằng EventId ổn định theo sự kiện, unique cùng RecipientId (UX_Notification_Recipient_EventId).
 */
@Service("systemNotificationService")
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EVENT_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSSS");

    private final NotificationRepository notificationRepository;
    private final com.group2.rms.user.repository.UserRepository userRepository;

    @Override
    @Transactional
    public void notifyRequisitionPendingDirector(JobRequisition requisition, User actor) {
        if (requisition == null) {
            log.warn("Cannot send requisition pending director notification: Requisition is null");
            return;
        }

        List<User> directors = userRepository.findAllActiveDirectors();
        if (directors == null || directors.isEmpty()) {
            log.warn("No active directors found to notify for pending Requisition ID: {}", requisition.getRequisitionId());
            return;
        }

        String hmName = actor != null ? actor.getFullName()
                : (requisition.getHiringManager() != null ? requisition.getHiringManager().getFullName() : "Hiring Manager");
        String deptName = requisition.getDepartment() != null ? requisition.getDepartment().getDepartmentName() : "Chưa xác định";
        String timeKey = requisition.getSubmittedAt() != null
                ? requisition.getSubmittedAt().format(EVENT_TIME)
                : LocalDateTime.now().format(EVENT_TIME);

        String title = "Yêu cầu phê duyệt tuyển dụng: " + requisition.getTitle();
        String content = String.format(
                "Hiring Manager %s đã gửi yêu cầu tuyển dụng \"%s\" (Mã: %s, Phòng ban: %s) để chờ phê duyệt. Vui lòng xem xét và thực hiện phê duyệt.",
                hmName, requisition.getTitle(), requisitionCode(requisition), deptName);

        for (User director : directors) {
            if (actor != null && director.getUserId().equals(actor.getUserId())) {
                continue;
            }
            send(Notification.builder()
                    .recipient(director)
                    .actor(actor != null ? actor : requisition.getHiringManager())
                    .title(title)
                    .content(content)
                    .eventType("REQUISITION_PENDING_DIRECTOR")
                    .referenceId(String.valueOf(requisition.getRequisitionId()))
                    .linkUrl("/requisitions/" + requisition.getRequisitionId())
                    .eventId("REQ_PENDING_DIR_" + requisition.getRequisitionId() + "_" + director.getUserId() + "_" + timeKey)
                    .build());
        }
    }

    @Override
    @Transactional
    public void notifyRequisitionApproved(RequisitionApproval approval) {
        JobRequisition requisition = approval == null ? null : approval.getRequisition();
        if (requisition == null || requisition.getHiringManager() == null) {
            log.warn("Cannot send requisition approved notification: Requisition or Hiring Manager is null");
            return;
        }

        User director = approval.getDirector();
        if (director != null && director.getUserId().equals(requisition.getHiringManager().getUserId())) {
            return;
        }

        String directorName = director != null ? director.getFullName() : "Giám đốc";
        LocalDateTime decidedAt = approval.getApprovalDate() != null ? approval.getApprovalDate() : LocalDateTime.now();
        String commentText = approval.getComments() != null && !approval.getComments().isBlank()
                ? " Ý kiến: \"" + approval.getComments().trim() + "\"." : "";

        String content = String.format(
                "Yêu cầu tuyển dụng \"%s\" (Mã: %s) đã được Giám đốc %s phê duyệt vào lúc %s.%s Bộ phận Nhân sự (HR) có thể tiến hành tạo tin tuyển dụng.",
                requisition.getTitle(), requisitionCode(requisition), directorName, decidedAt.format(DISPLAY_TIME), commentText);

        String eventId = approval.getApprovalId() != null
                ? "REQ_APPROVE_" + approval.getApprovalId()
                : "REQ_APPROVE_" + requisition.getRequisitionId() + "_" + decidedAt.format(EVENT_TIME);

        send(Notification.builder()
                .recipient(requisition.getHiringManager())
                .actor(director)
                .title("Yêu cầu tuyển dụng đã được phê duyệt: " + requisition.getTitle())
                .content(content)
                .eventType("REQUISITION_APPROVED")
                .referenceId(String.valueOf(requisition.getRequisitionId()))
                .linkUrl("/requisitions/" + requisition.getRequisitionId())
                .eventId(eventId)
                .build());
    }

    @Override
    @Transactional
    public void notifyRequisitionRejected(RequisitionApproval rejection) {
        JobRequisition requisition = rejection == null ? null : rejection.getRequisition();
        if (requisition == null || requisition.getHiringManager() == null) {
            log.warn("Cannot send rejection notification: Requisition or Hiring Manager is null");
            return;
        }

        User director = rejection.getDirector();
        String directorName = director != null ? director.getFullName() : "Giám đốc";
        LocalDateTime decidedAt = rejection.getApprovalDate() != null ? rejection.getApprovalDate() : LocalDateTime.now();
        String reason = rejection.getComments() != null && !rejection.getComments().isBlank()
                ? rejection.getComments().trim() : "Không có lý do cụ thể";

        String content = String.format(
                "Yêu cầu tuyển dụng \"%s\" (Mã: %s) đã bị Giám đốc %s từ chối vào lúc %s.\nLý do: %s.\nVui lòng chỉnh sửa lại thông tin và nộp duyệt lại.",
                requisition.getTitle(), requisitionCode(requisition), directorName, decidedAt.format(DISPLAY_TIME), reason);

        // Mỗi lần từ chối có một bản ghi RequisitionApproval riêng, nên ID của nó là khóa sự kiện ổn định.
        send(Notification.builder()
                .recipient(requisition.getHiringManager())
                .actor(director)
                .title("Yêu cầu tuyển dụng đã bị từ chối: " + requisition.getTitle())
                .content(content)
                .eventType("REQUISITION_REJECTED")
                .referenceId(String.valueOf(requisition.getRequisitionId()))
                .linkUrl("/requisitions/" + requisition.getRequisitionId())
                .eventId("REQ_REJECT_" + rejection.getApprovalId())
                .build());
    }

    @Override
    @Transactional
    public void notifyJobPostingPublished(JobPosting posting, User hr) {
        if (posting == null || posting.getRequisition() == null || posting.getRequisition().getHiringManager() == null) {
            log.warn("Cannot send job posting published notification: Posting or Requisition owner is null");
            return;
        }

        LocalDateTime postingDate = posting.getPostingDate() != null ? posting.getPostingDate() : LocalDateTime.now();
        String hrName = hr != null ? hr.getFullName() : "Nhân sự (HR)";
        String deadline = posting.getApplicationDeadline() != null
                ? posting.getApplicationDeadline().format(DISPLAY_DATE) : "Không giới hạn";

        String content = String.format("Tin tuyển dụng \"%s\" (cho %s) đã được %s phát hành vào lúc %s (Hạn nộp hồ sơ: %s). Ứng viên hiện đã có thể xem và nộp hồ sơ.",
                posting.getPostingTitle(), requisitionCode(posting.getRequisition()), hrName,
                postingDate.format(DISPLAY_TIME), deadline);

        // PostingDate chỉ đổi khi tin được đăng hoặc mở lại, không đổi khi sửa nội dung.
        send(Notification.builder()
                .recipient(posting.getRequisition().getHiringManager())
                .actor(hr)
                .title("Tin tuyển dụng đã phát hành: " + posting.getPostingTitle())
                .content(content)
                .eventType("JOB_POSTING_PUBLISHED")
                .referenceId(String.valueOf(posting.getJobPostingId()))
                .linkUrl("/internal/job-postings/" + posting.getJobPostingId())
                .eventId("JOB_PUB_" + posting.getJobPostingId() + "_" + postingDate.format(EVENT_TIME))
                .build());
    }

    @Override
    @Transactional
    public void notifyOfferPendingDirector(OfferProposal offer, User hrActor) {
        if (offer == null) {
            log.warn("Cannot send offer pending director notification: Offer is null");
            return;
        }

        List<User> directors = userRepository.findAllActiveDirectors();
        if (directors == null || directors.isEmpty()) {
            log.warn("No active directors found to notify for pending Offer ID: {}", offer.getOfferId());
            return;
        }

        String positionTitle = positionTitle(offer);
        String candidateName = (offer.getApplication() != null && offer.getApplication().getCandidate() != null
                && offer.getApplication().getCandidate().getAccount() != null)
                ? offer.getApplication().getCandidate().getAccount().getFullName() : "Ứng viên";
        String hrName = hrActor != null ? hrActor.getFullName()
                : (offer.getProposedBy() != null ? offer.getProposedBy().getFullName() : "Chuyên viên HR");
        String salary = offer.getProposedSalary() != null
                ? String.format("%,d ₫", offer.getProposedSalary().longValue()) : "Thỏa thuận";

        String title = "Yêu cầu phê duyệt Đề xuất Offer: " + positionTitle;
        String content = String.format(
                "Chuyên viên HR %s đã nộp đề xuất Offer cho ứng viên %s (Vị trí: %s, Lương chính thức: %s). Vui lòng xem xét và thực hiện phê duyệt.",
                hrName, candidateName, positionTitle, salary);
        String timeKey = offer.getUpdatedAt() != null ? offer.getUpdatedAt().format(EVENT_TIME) : LocalDateTime.now().format(EVENT_TIME);

        for (User director : directors) {
            send(Notification.builder()
                    .recipient(director)
                    .actor(hrActor != null ? hrActor : offer.getProposedBy())
                    .title(title)
                    .content(content)
                    .eventType("OFFER_PENDING_DIRECTOR")
                    .referenceId(String.valueOf(offer.getOfferId()))
                    .linkUrl("/offers/" + offer.getOfferId())
                    .eventId("OFFER_PENDING_DIR_" + offer.getOfferId() + "_" + director.getUserId() + "_" + timeKey)
                    .build());
        }
    }

    @Override
    @Transactional
    public void notifyDirectorDecisionOnOffer(OfferProposal offer, User director, boolean approved, String comments) {
        if (offer == null || offer.getProposedBy() == null) {
            log.warn("Cannot send director decision notification: Offer or ProposedBy (HR) is null for Offer ID: {}",
                    offer != null ? offer.getOfferId() : null);
            return;
        }

        String positionTitle = positionTitle(offer);
        String candidateName = (offer.getApplication() != null && offer.getApplication().getCandidate() != null
                && offer.getApplication().getCandidate().getAccount() != null)
                ? offer.getApplication().getCandidate().getAccount().getFullName() : "Ứng viên";
        String directorName = director != null ? director.getFullName() : "Giám đốc";
        String timeKey = LocalDateTime.now().format(EVENT_TIME);

        Notification.NotificationBuilder notification = Notification.builder()
                .recipient(offer.getProposedBy())
                .actor(director)
                .referenceId(String.valueOf(offer.getOfferId()))
                .linkUrl("/offers/" + offer.getOfferId());

        if (approved) {
            String noteText = comments != null && !comments.isBlank() ? " Ghi chú: \"" + comments.trim() + "\"." : "";
            notification
                    .title("Đề xuất Offer đã được Giám đốc phê duyệt: " + positionTitle)
                    .content(String.format(
                            "Giám đốc %s đã phê duyệt đề xuất Offer cho ứng viên %s (Vị trí: %s).%s Bạn có thể tiến hành phát hành Thư mời làm việc cho ứng viên.",
                            directorName, candidateName, positionTitle, noteText))
                    .eventType("OFFER_DIRECTOR_APPROVED")
                    .eventId("OFFER_DEC_APPV_" + offer.getOfferId() + "_" + timeKey);
        } else {
            String reasonText = comments != null && !comments.isBlank() ? comments.trim() : "Không có lý do cụ thể";
            notification
                    .title("Đề xuất Offer bị Giám đốc từ chối: " + positionTitle)
                    .content(String.format(
                            "Giám đốc %s đã từ chối đề xuất Offer cho ứng viên %s (Vị trí: %s). Lý do: \"%s\". Vui lòng kiểm tra lại điều khoản và chỉnh sửa bản thảo nếu cần.",
                            directorName, candidateName, positionTitle, reasonText))
                    .eventType("OFFER_DIRECTOR_REJECTED")
                    .eventId("OFFER_DEC_REJ_" + offer.getOfferId() + "_" + timeKey);
        }

        send(notification.build());
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

        String positionTitle = positionTitle(offer);
        String timeKey = offer.getUpdatedAt() != null ? offer.getUpdatedAt().format(EVENT_TIME) : LocalDateTime.now().format(EVENT_TIME);
        send(Notification.builder()
                .recipient(offer.getApplication().getCandidate().getAccount())
                .actor(offer.getProposedBy())
                .title("Bạn có thư mời nhận việc mới: " + positionTitle)
                .content(String.format(
                        "Chúc mừng bạn! Nhà tuyển dụng đã gửi Thư mời nhận việc cho vị trí \"%s\". Vui lòng bấm vào đây để xem chi tiết điều khoản và phản hồi thư mời.",
                        positionTitle))
                .eventType("OFFER_LETTER_SENT")
                .referenceId(String.valueOf(offer.getOfferId()))
                .linkUrl("/portal/offers/" + offer.getOfferId())
                .eventId("OFFER_SENT_" + offer.getOfferId() + "_" + timeKey)
                .build());
    }

    @Override
    @Transactional
    public void notifyOfferResponseFromCandidate(OfferProposal offer, boolean accepted, String declineReason) {
        if (offer == null || offer.getProposedBy() == null) {
            log.warn("Cannot send offer response notification: Offer or ProposedBy (HR) is null for Offer ID: {}",
                    offer != null ? offer.getOfferId() : null);
            return;
        }

        String positionTitle = positionTitle(offer);
        User candidateAccount = offer.getApplication() != null && offer.getApplication().getCandidate() != null
                ? offer.getApplication().getCandidate().getAccount() : null;
        String candidateName = candidateAccount != null && candidateAccount.getFullName() != null
                ? candidateAccount.getFullName() : "Ứng viên";
        String timeKey = LocalDateTime.now().format(EVENT_TIME);

        Notification.NotificationBuilder hrNotification = Notification.builder()
                .recipient(offer.getProposedBy())
                .actor(candidateAccount)
                .referenceId(String.valueOf(offer.getOfferId()))
                .linkUrl("/offers");

        if (accepted) {
            hrNotification
                    .title(String.format("🎉 Ứng viên đã nhận offer: %s (%s)", candidateName, positionTitle))
                    .content(String.format(
                            "Ứng viên %s đã đồng ý nhận lời mời làm việc cho vị trí \"%s\". Trạng thái hồ sơ đã chuyển sang \"Đã nhận việc\" (Hired). Vui lòng chuẩn bị thủ tục tiếp nhận nhân sự.",
                            candidateName, positionTitle))
                    .eventType("OFFER_ACCEPTED")
                    .eventId("OFFER_RESP_ACC_" + offer.getOfferId() + "_" + timeKey);
        } else {
            String reasonText = declineReason != null && !declineReason.isBlank()
                    ? String.format(" Lý do: \"%s\".", declineReason.trim()) : "";
            hrNotification
                    .title(String.format("Ứng viên từ chối offer: %s (%s)", candidateName, positionTitle))
                    .content(String.format(
                            "Ứng viên %s đã từ chối thư mời làm việc cho vị trí \"%s\".%s Trạng thái hồ sơ đã chuyển sang \"Không tiếp tục\" (Rejected).",
                            candidateName, positionTitle, reasonText))
                    .eventType("OFFER_DECLINED")
                    .eventId("OFFER_RESP_DEC_" + offer.getOfferId() + "_" + timeKey);
        }
        send(hrNotification.build());

        // Bổ sung: Gửi thông báo cho Hiring Manager phụ trách Requisition (nếu có và khác HR)
        User hiringManager = (offer.getApplication() != null
                && offer.getApplication().getJobPosting() != null
                && offer.getApplication().getJobPosting().getRequisition() != null)
                ? offer.getApplication().getJobPosting().getRequisition().getHiringManager() : null;

        if (hiringManager != null && !hiringManager.getUserId().equals(offer.getProposedBy().getUserId())) {
            Notification.NotificationBuilder hmNotification = Notification.builder()
                    .recipient(hiringManager)
                    .actor(candidateAccount)
                    .referenceId(String.valueOf(offer.getOfferId()))
                    .linkUrl("/offers");

            if (accepted) {
                hmNotification
                        .title(String.format("🎉 Ứng viên nhận offer: %s (%s)", candidateName, positionTitle))
                        .content(String.format(
                                "Ứng viên %s đã đồng ý nhận việc cho vị trí \"%s\". Bộ phận Nhân sự đang chuẩn bị thủ tục Onboarding tiếp theo.",
                                candidateName, positionTitle))
                        .eventType("OFFER_ACCEPTED")
                        .eventId("OFFER_RESP_HM_ACC_" + offer.getOfferId() + "_" + timeKey);
            } else {
                String reasonText = declineReason != null && !declineReason.isBlank()
                        ? String.format(" Lý do: \"%s\".", declineReason.trim()) : "";
                hmNotification
                        .title(String.format("Ứng viên từ chối offer: %s (%s)", candidateName, positionTitle))
                        .content(String.format(
                                "Ứng viên %s đã từ chối thư mời làm việc cho vị trí \"%s\".%s",
                                candidateName, positionTitle, reasonText))
                        .eventType("OFFER_DECLINED")
                        .eventId("OFFER_RESP_HM_DEC_" + offer.getOfferId() + "_" + timeKey);
            }
            send(hmNotification.build());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsForUser(User user, int page, int size) {
        if (user == null) {
            return Page.empty();
        }
        int validPage = Math.max(0, page - 1);
        int validSize = Math.max(1, Math.min(size, 50));
        return notificationRepository.findByRecipient_UserIdOrderByCreatedAtDesc(user.getUserId(), PageRequest.of(validPage, validSize))
                .map(NotificationResponse::from);
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
    public NotificationResponse markAsRead(Long notificationId, User user) {
        if (notificationId == null || user == null) {
            throw new ResourceNotFoundException("Thông báo không tồn tại hoặc không thuộc về bạn.");
        }
        Notification notification = notificationRepository.findByNotificationIdAndRecipient_UserId(notificationId, user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Thông báo không tồn tại hoặc không thuộc về bạn."));
        notification.markRead(LocalDateTime.now());
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(User user) {
        if (user == null) {
            return;
        }
        notificationRepository.markAllAsReadByUserId(user.getUserId(), LocalDateTime.now());
    }

    @Override
    @Transactional
    public void markOfferNotificationRead(Integer userId, Integer offerId) {
        if (userId != null && offerId != null) {
            notificationRepository.markOfferNotificationRead(userId, String.valueOf(offerId), LocalDateTime.now());
        }
    }

    private void send(Notification notification) {
        Integer recipientId = notification.getRecipient().getUserId();
        if (notificationRepository.existsByRecipient_UserIdAndEventId(recipientId, notification.getEventId())) {
            log.info("Notification for eventId {} already exists. Skipping duplicate creation.", notification.getEventId());
            return;
        }
        notificationRepository.save(notification);
        log.info("Sent {} notification to User ID {} (reference {})",
                notification.getEventType(), recipientId, notification.getReferenceId());
    }

    private static String requisitionCode(JobRequisition requisition) {
        return requisition.getRequisitionCode() != null
                ? requisition.getRequisitionCode() : "REQ-" + requisition.getRequisitionId();
    }

    private static String positionTitle(OfferProposal offer) {
        return offer.getOfferedPositionTitle() != null && !offer.getOfferedPositionTitle().isBlank()
                ? offer.getOfferedPositionTitle() : "Vị trí tuyển dụng";
    }
}
