package com.group2.rms.notification.service;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.rule.ApplicationStatusLabels;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.interview.entity.InterviewPanel;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.notification.dto.NotificationResponse;
import com.group2.rms.notification.entity.Notification;
import com.group2.rms.notification.repository.NotificationRepository;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * SERVICE: chọn người nhận và lưu thông báo in-app, chạy chung transaction với nghiệp vụ; chữ do NotificationMessages soạn.
 */
@Service("systemNotificationService")
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   ApplicationEventPublisher events,
                                   Clock clock) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.events = events;
        this.clock = clock;
    }

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

        // Không truyền actor thì coi HM chủ yêu cầu là người gửi
        User sender = actor;
        if (sender == null) {
            sender = requisition.getHiringManager();
        }
        String hmName = NotificationMessages.nameOr(sender, "Hiring Manager");
        LocalDateTime submittedAt = requisition.getSubmittedAt();
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now(clock);
        }
        String timeKey = NotificationMessages.eventTime(submittedAt);
        String title = "Yêu cầu phê duyệt tuyển dụng: " + requisition.getTitle();
        String content = NotificationMessages.requisitionPendingContent(requisition, hmName);

        for (User director : directors) {
            if (actor != null && director.getUserId().equals(actor.getUserId())) {
                continue;
            }
            saveIfNotDuplicate(Notification.builder()
                    .recipient(director)
                    .actor(sender)
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
        User hiringManager = requisition.getHiringManager();
        boolean isSelfApproved = director != null && director.getUserId().equals(hiringManager.getUserId());
        LocalDateTime decidedAt = decidedAt(approval);
        String content = NotificationMessages.requisitionApprovedContent(requisition,
                NotificationMessages.nameOr(director, "Giám đốc"), decidedAt, approval.getComments());

        String eventId = "REQ_APPROVE_" + approval.getApprovalId();
        if (approval.getApprovalId() == null) {
            eventId = "REQ_APPROVE_" + requisition.getRequisitionId() + "_" + NotificationMessages.eventTime(decidedAt);
        }
        String title = "Yêu cầu tuyển dụng đã được phê duyệt: " + requisition.getTitle();
        String referenceId = String.valueOf(requisition.getRequisitionId());

        // Giám đốc tự duyệt yêu cầu của mình thì không báo cho chính mình
        if (!isSelfApproved) {
            saveIfNotDuplicate(Notification.builder()
                    .recipient(hiringManager)
                    .actor(director)
                    .title(title)
                    .content(content)
                    .eventType("REQUISITION_APPROVED")
                    .referenceId(referenceId)
                    .linkUrl("/requisitions/" + requisition.getRequisitionId())
                    .eventId(eventId)
                    .build());
        }
        // HR là người tạo tin nên cũng nhận thông báo, link mở thẳng form tạo tin
        for (User hr : userRepository.findAllActiveHrs()) {
            saveIfNotDuplicate(Notification.builder()
                    .recipient(hr)
                    .actor(director)
                    .title(title)
                    .content(content)
                    .eventType("REQUISITION_APPROVED")
                    .referenceId(referenceId)
                    .linkUrl("/internal/job-postings/create?requisitionId=" + requisition.getRequisitionId())
                    .eventId(eventId)
                    .build());
        }
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
        String content = NotificationMessages.requisitionRejectedContent(requisition,
                NotificationMessages.nameOr(director, "Giám đốc"), decidedAt(rejection), rejection.getComments());

        // Mỗi lần từ chối có một bản ghi RequisitionApproval riêng, nên ID của nó là khóa sự kiện ổn định.
        saveIfNotDuplicate(Notification.builder()
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
    public void notifyRequisitionWithdrawn(JobRequisition requisition, User actor) {
        if (requisition == null) {
            log.warn("Cannot send requisition withdrawn notification: Requisition is null");
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String content = NotificationMessages.requisitionWithdrawnContent(requisition,
                NotificationMessages.nameOr(actor, "Hiring Manager"), now);
        notifyEach(userRepository.findAllActiveDirectors(), actor, Notification.builder()
                .title("Yêu cầu tuyển dụng đã được rút lại: " + requisition.getTitle())
                .content(content)
                .eventType("REQUISITION_WITHDRAWN")
                .referenceId(String.valueOf(requisition.getRequisitionId()))
                .linkUrl("/requisitions/" + requisition.getRequisitionId())
                .eventId("REQ_WITHDRAW_" + requisition.getRequisitionId() + "_" + NotificationMessages.eventTime(now)));
    }

    @Override
    @Transactional
    public void notifyJobPostingPublished(JobPosting posting, User hr) {
        if (!hasPostingOwner(posting)) {
            log.warn("Cannot send job posting published notification: Posting or Requisition owner is null");
            return;
        }

        LocalDateTime postingDate = posting.getPostingDate();
        if (postingDate == null) {
            postingDate = LocalDateTime.now(clock);
        }
        String content = NotificationMessages.jobPostingPublishedContent(posting,
                NotificationMessages.nameOr(hr, "Nhân sự (HR)"), postingDate);

        // PostingDate chỉ đổi khi tin được đăng hoặc mở lại, không đổi khi sửa nội dung.
        saveIfNotDuplicate(Notification.builder()
                .recipient(posting.getRequisition().getHiringManager())
                .actor(hr)
                .title("Tin tuyển dụng đã phát hành: " + posting.getPostingTitle())
                .content(content)
                .eventType("JOB_POSTING_PUBLISHED")
                .referenceId(String.valueOf(posting.getJobPostingId()))
                .linkUrl("/internal/job-postings/" + posting.getJobPostingId())
                .eventId("JOB_PUB_" + posting.getJobPostingId() + "_" + NotificationMessages.eventTime(postingDate))
                .build());
    }

    @Override
    @Transactional
    public void notifyJobPostingPaused(JobPosting posting, User actor) {
        if (!hasPostingOwner(posting)) {
            log.warn("Cannot send JOB_POSTING_PAUSED notification: Posting or Requisition owner is null");
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String content = NotificationMessages.jobPostingPausedContent(posting,
                NotificationMessages.nameOr(actor, "Nhân sự (HR)"), now);
        notifyPostingOwner(posting, actor, "JOB_POSTING_PAUSED", "Tin tuyển dụng đã tạm dừng: ", content,
                "JOB_PAUSE_" + posting.getJobPostingId() + "_" + NotificationMessages.eventTime(now));
    }

    @Override
    @Transactional
    public void notifyJobPostingClosed(JobPosting posting, User actor) {
        if (!hasPostingOwner(posting)) {
            log.warn("Cannot send JOB_POSTING_CLOSED notification: Posting or Requisition owner is null");
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String content = NotificationMessages.jobPostingClosedContent(posting,
                NotificationMessages.nameOr(actor, "Nhân sự (HR)"), now);
        notifyPostingOwner(posting, actor, "JOB_POSTING_CLOSED", "Tin tuyển dụng đã đóng: ", content,
                "JOB_CLOSE_" + posting.getJobPostingId() + "_" + NotificationMessages.eventTime(now));
    }

    @Override
    @Transactional
    public void notifyApplicationAwaitingHiringManager(Application application, User hr) {
        JobRequisition requisition = NotificationMessages.requisitionOf(application);
        if (requisition == null) {
            log.warn("Cannot send application review notification: Application or Requisition is null");
            return;
        }
        // HM chủ yêu cầu và trưởng phòng đều duyệt được bước này (ApplicationAccess.canReview)
        List<User> reviewers = new ArrayList<>();
        reviewers.add(requisition.getHiringManager());
        if (requisition.getDepartment() != null) {
            reviewers.add(requisition.getDepartment().getManager());
        }
        String content = NotificationMessages.applicationHrPassedContent(application,
                NotificationMessages.nameOr(hr, "Nhân sự (HR)"));
        notifyEach(reviewers, hr, Notification.builder()
                .title("Hồ sơ chờ bạn duyệt: " + NotificationMessages.candidateName(application))
                .content(content)
                .eventType("APPLICATION_HR_PASSED")
                .referenceId(String.valueOf(application.getApplicationId()))
                .linkUrl("/applications/" + application.getApplicationId())
                .eventId("APP_HR_PASS_" + application.getApplicationId()));
    }

    @Override
    @Transactional
    public void notifyApplicationReadyForInterview(Application application, User reviewer) {
        if (NotificationMessages.requisitionOf(application) == null) {
            log.warn("Cannot send ready-for-interview notification: Application or Requisition is null");
            return;
        }
        String content = NotificationMessages.applicationHmPassedContent(application,
                NotificationMessages.nameOr(reviewer, "Trưởng bộ phận"));
        notifyEach(userRepository.findAllActiveHrs(), reviewer, Notification.builder()
                .title("Hồ sơ sẵn sàng phỏng vấn: " + NotificationMessages.candidateName(application))
                .content(content)
                .eventType("APPLICATION_HM_PASSED")
                .referenceId(String.valueOf(application.getApplicationId()))
                .linkUrl("/applications/" + application.getApplicationId())
                .eventId("APP_HM_PASS_" + application.getApplicationId()));
    }

    @Override
    @Transactional
    public void notifyInterviewScheduled(InterviewSchedule schedule, User actor) {
        notifyInterviewChange(schedule, actor, "INTERVIEW_SCHEDULED", "Lịch phỏng vấn mới",
                "INT_SCHED_" + interviewId(schedule), "");
    }

    @Override
    @Transactional
    public void notifyInterviewRescheduled(InterviewSchedule schedule, User actor) {
        String timeKey = NotificationMessages.eventTime(LocalDateTime.now(clock));
        notifyInterviewChange(schedule, actor, "INTERVIEW_RESCHEDULED", "Lịch phỏng vấn đã thay đổi",
                "INT_RESCHED_" + interviewId(schedule) + "_" + timeKey, "");
    }

    @Override
    @Transactional
    public void notifyInterviewCancelled(InterviewSchedule schedule, User actor, String reason) {
        String reasonText = "";
        if (reason != null && !reason.isBlank()) {
            reasonText = " Lý do: " + reason.trim() + ".";
        }
        notifyInterviewChange(schedule, actor, "INTERVIEW_CANCELLED", "Lịch phỏng vấn đã bị hủy",
                "INT_CANCEL_" + interviewId(schedule), reasonText);
    }

    @Override
    @Transactional
    public void notifyInterviewResultDecided(InterviewSchedule schedule, User hiringManager, String finalDecision) {
        Application application = schedule == null ? null : schedule.getApplication();
        if (NotificationMessages.requisitionOf(application) == null || finalDecision == null) {
            log.warn("Cannot send interview result notification: Schedule, Application or decision is null");
            return;
        }
        String result = ApplicationStatusLabels.interviewResultLabel(finalDecision);
        String content = NotificationMessages.interviewResultContent(application,
                NotificationMessages.nameOr(hiringManager, "Hiring Manager"), result);
        // Considering rồi chốt lại Passed/Failed là hai sự kiện khác nhau
        notifyEach(userRepository.findAllActiveHrs(), hiringManager, Notification.builder()
                .title("Kết quả phỏng vấn (" + result + "): " + NotificationMessages.candidateName(application))
                .content(content)
                .eventType("INTERVIEW_RESULT_DECIDED")
                .referenceId(String.valueOf(schedule.getInterviewId()))
                .linkUrl("/interviews/" + schedule.getInterviewId() + "/result")
                .eventId("INT_RESULT_" + schedule.getInterviewId() + "_" + finalDecision));
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

        // Không truyền hrActor thì coi người lập đề xuất là người gửi
        User sender = hrActor;
        if (sender == null) {
            sender = offer.getProposedBy();
        }
        String title = "Yêu cầu phê duyệt Đề xuất nhận việc: " + NotificationMessages.positionTitle(offer);
        String content = NotificationMessages.offerPendingContent(offer,
                NotificationMessages.nameOr(sender, "Chuyên viên tuyển dụng"));
        String timeKey = offerTimeKey(offer);

        for (User director : directors) {
            saveIfNotDuplicate(Notification.builder()
                    .recipient(director)
                    .actor(sender)
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
        if (offer == null) {
            log.warn("Cannot send director decision notification: Offer is null");
            return;
        }

        // Người lập đề xuất trước, sau đó mọi HR đang hoạt động (bỏ trùng)
        Map<Integer, User> recipientMap = new LinkedHashMap<>();
        if (offer.getProposedBy() != null && offer.getProposedBy().getUserId() != null) {
            recipientMap.put(offer.getProposedBy().getUserId(), offer.getProposedBy());
        }
        List<User> activeHrs = userRepository.findAllActiveHrs();
        if (activeHrs != null) {
            for (User hr : activeHrs) {
                if (hr != null && hr.getUserId() != null) {
                    recipientMap.putIfAbsent(hr.getUserId(), hr);
                }
            }
        }
        if (recipientMap.isEmpty()) {
            log.warn("Cannot send director decision notification: No HR recipients found for Offer ID: {}",
                    offer.getOfferId());
            return;
        }

        String positionTitle = NotificationMessages.positionTitle(offer);
        String directorName = NotificationMessages.nameOr(director, "Giám đốc");
        String timeKey = NotificationMessages.eventTime(LocalDateTime.now(clock));

        String title;
        String content;
        String eventType;
        String eventPrefix;
        if (approved) {
            title = "Đề xuất nhận việc đã được Giám đốc phê duyệt: " + positionTitle;
            content = NotificationMessages.offerApprovedContent(offer, directorName, comments);
            eventType = "OFFER_DIRECTOR_APPROVED";
            eventPrefix = "OFFER_DEC_APPV_";
        } else {
            title = "Đề xuất nhận việc bị Giám đốc từ chối: " + positionTitle;
            content = NotificationMessages.offerRejectedContent(offer, directorName, comments);
            eventType = "OFFER_DIRECTOR_REJECTED";
            eventPrefix = "OFFER_DEC_REJ_";
        }

        for (User recipient : recipientMap.values()) {
            saveIfNotDuplicate(Notification.builder()
                    .recipient(recipient)
                    .actor(director)
                    .referenceId(String.valueOf(offer.getOfferId()))
                    .linkUrl("/offers/" + offer.getOfferId())
                    .title(title)
                    .content(content)
                    .eventType(eventType)
                    .eventId(eventPrefix + offer.getOfferId() + "_" + recipient.getUserId() + "_" + timeKey)
                    .build());
        }
    }

    @Override
    @Transactional
    public void notifyOfferLetterSent(OfferProposal offer) {
        User candidateAccount = candidateAccountOf(offer);
        if (candidateAccount == null) {
            log.warn("Cannot send offer notification: Candidate or User account is null for Offer ID: {}",
                    offer != null ? offer.getOfferId() : null);
            return;
        }

        saveIfNotDuplicate(Notification.builder()
                .recipient(candidateAccount)
                .actor(offer.getProposedBy())
                .title("Bạn có thư mời nhận việc mới: " + NotificationMessages.positionTitle(offer))
                .content(NotificationMessages.offerLetterSentContent(offer))
                .eventType("OFFER_LETTER_SENT")
                .referenceId(String.valueOf(offer.getOfferId()))
                .linkUrl("/portal/offers/" + offer.getOfferId())
                .eventId("OFFER_SENT_" + offer.getOfferId() + "_" + offerTimeKey(offer))
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

        String positionTitle = NotificationMessages.positionTitle(offer);
        User candidateAccount = candidateAccountOf(offer);
        String candidateName = "Ứng viên";
        if (candidateAccount != null && candidateAccount.getFullName() != null) {
            candidateName = candidateAccount.getFullName();
        }
        String reasonText = NotificationMessages.declineReasonText(declineReason);

        Notification.NotificationBuilder hrNotification = Notification.builder()
                .recipient(offer.getProposedBy())
                .actor(candidateAccount)
                .referenceId(String.valueOf(offer.getOfferId()))
                .linkUrl("/offers");
        if (accepted) {
            hrNotification
                    .title(String.format("🎉 Ứng viên đã đồng ý nhận việc: %s (%s)", candidateName, positionTitle))
                    .content(String.format(
                            "Ứng viên %s đã đồng ý nhận lời mời làm việc cho vị trí \"%s\". Trạng thái hồ sơ đã chuyển sang \"Đã nhận việc\". Vui lòng chuẩn bị thủ tục tiếp nhận nhân sự.",
                            candidateName, positionTitle))
                    .eventType("OFFER_ACCEPTED")
                    .eventId("OFFER_RESP_ACC_" + offer.getOfferId());
        } else {
            hrNotification
                    .title(String.format("Ứng viên từ chối thư mời nhận việc: %s (%s)", candidateName, positionTitle))
                    .content(String.format(
                            "Ứng viên %s đã từ chối thư mời làm việc cho vị trí \"%s\".%s Trạng thái hồ sơ đã chuyển sang \"Không tiếp tục\".",
                            candidateName, positionTitle, reasonText))
                    .eventType("OFFER_DECLINED")
                    .eventId("OFFER_RESP_DEC_" + offer.getOfferId());
        }
        saveIfNotDuplicate(hrNotification.build());

        // HM chủ yêu cầu cũng nhận tin, trừ khi HM chính là HR lập đề xuất
        JobRequisition requisition = NotificationMessages.requisitionOf(offer.getApplication());
        User hiringManager = requisition == null ? null : requisition.getHiringManager();
        if (hiringManager == null || hiringManager.getUserId().equals(offer.getProposedBy().getUserId())) {
            return;
        }
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
                    .eventId("OFFER_RESP_HM_ACC_" + offer.getOfferId());
        } else {
            hmNotification
                    .title(String.format("Ứng viên từ chối offer: %s (%s)", candidateName, positionTitle))
                    .content(String.format("Ứng viên %s đã từ chối thư mời làm việc cho vị trí \"%s\".%s",
                            candidateName, positionTitle, reasonText))
                    .eventType("OFFER_DECLINED")
                    .eventId("OFFER_RESP_HM_DEC_" + offer.getOfferId());
        }
        saveIfNotDuplicate(hmNotification.build());
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
        notification.markRead(LocalDateTime.now(clock));
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(User user) {
        if (user == null) {
            return;
        }
        notificationRepository.markAllAsReadByUserId(user.getUserId(), LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public void markOfferNotificationRead(Integer userId, Integer offerId) {
        if (userId != null && offerId != null) {
            notificationRepository.markOfferNotificationRead(userId, String.valueOf(offerId), LocalDateTime.now(clock));
        }
    }

    @Override
    @Transactional
    public void markRequisitionPendingRead(Integer requisitionId) {
        if (requisitionId != null) {
            notificationRepository.markReadByEvent("REQUISITION_PENDING_DIRECTOR", String.valueOf(requisitionId),
                    LocalDateTime.now(clock));
        }
    }

    private void saveIfNotDuplicate(Notification notification) {
        Integer recipientId = notification.getRecipient().getUserId();
        if (notificationRepository.existsByRecipient_UserIdAndEventId(recipientId, notification.getEventId())) {
            log.info("Notification for eventId {} already exists. Skipping duplicate creation.", notification.getEventId());
            return;
        }
        notificationRepository.save(notification);
        // NotificationPushListener đẩy qua WebSocket sau khi commit
        events.publishEvent(new NotificationPushListener.Created(recipientId, notification.getRecipient().getUsername()));
        log.info("Sent {} notification to User ID {} (reference {})",
                notification.getEventType(), recipientId, notification.getReferenceId());
    }

    // Gửi cùng một thông báo cho từng người nhận; bỏ người null, người trùng và chính người làm
    private void notifyEach(Collection<User> recipients, User actor, Notification.NotificationBuilder template) {
        if (recipients == null) {
            return;
        }
        Set<Integer> sent = new HashSet<>();
        for (User recipient : recipients) {
            if (recipient == null || recipient.getUserId() == null || !sent.add(recipient.getUserId())) {
                continue;
            }
            if (actor != null && recipient.getUserId().equals(actor.getUserId())) {
                continue;
            }
            saveIfNotDuplicate(template.recipient(recipient).actor(actor).build());
        }
    }

    // Tạm dừng / đóng tin: chỉ báo HM chủ yêu cầu nguồn
    private void notifyPostingOwner(JobPosting posting, User actor, String eventType, String titlePrefix,
                                    String content, String eventId) {
        notifyEach(List.of(posting.getRequisition().getHiringManager()), actor, Notification.builder()
                .title(titlePrefix + posting.getPostingTitle())
                .content(content)
                .eventType(eventType)
                .referenceId(String.valueOf(posting.getJobPostingId()))
                .linkUrl("/internal/job-postings/" + posting.getJobPostingId())
                .eventId(eventId));
    }

    // Tạo / đổi / hủy lịch: hội đồng mở danh sách lịch nội bộ, ứng viên mở cổng ứng viên; lý do chỉ báo nội bộ
    private void notifyInterviewChange(InterviewSchedule schedule, User actor, String eventType, String title,
                                       String eventId, String internalNote) {
        Application application = schedule == null ? null : schedule.getApplication();
        if (NotificationMessages.requisitionOf(application) == null || schedule.getStartTime() == null) {
            log.warn("Cannot send {} notification: Schedule, Application or start time is null", eventType);
            return;
        }
        String referenceId = String.valueOf(schedule.getInterviewId());

        List<User> panel = schedule.getInterviewPanels().stream().map(InterviewPanel::getInterviewer).toList();
        notifyEach(panel, actor, Notification.builder()
                .title(title + ": " + NotificationMessages.candidateName(application))
                .content(NotificationMessages.interviewPanelContent(schedule, internalNote))
                .eventType(eventType)
                .referenceId(referenceId)
                .linkUrl("/interviews")
                .eventId(eventId));

        User candidateAccount = null;
        if (application.getCandidate() != null) {
            candidateAccount = application.getCandidate().getAccount();
        }
        notifyEach(Collections.singletonList(candidateAccount), actor, Notification.builder()
                .title(title + ": " + application.getJobPosting().getPostingTitle())
                .content(NotificationMessages.interviewCandidateContent(schedule))
                .eventType(eventType)
                .referenceId(referenceId)
                .linkUrl("/portal/interviews")
                .eventId(eventId));
    }

    private LocalDateTime decidedAt(RequisitionApproval approval) {
        if (approval.getApprovalDate() != null) {
            return approval.getApprovalDate();
        }
        return LocalDateTime.now(clock);
    }

    // Offer chưa có UpdatedAt (chưa lưu lần nào) thì lấy giờ hiện tại làm khóa sự kiện
    private String offerTimeKey(OfferProposal offer) {
        if (offer.getUpdatedAt() != null) {
            return NotificationMessages.eventTime(offer.getUpdatedAt());
        }
        return NotificationMessages.eventTime(LocalDateTime.now(clock));
    }

    private static boolean hasPostingOwner(JobPosting posting) {
        return posting != null && posting.getRequisition() != null && posting.getRequisition().getHiringManager() != null;
    }

    private static User candidateAccountOf(OfferProposal offer) {
        if (offer == null || offer.getApplication() == null || offer.getApplication().getCandidate() == null) {
            return null;
        }
        return offer.getApplication().getCandidate().getAccount();
    }

    private static Long interviewId(InterviewSchedule schedule) {
        return schedule == null ? null : schedule.getInterviewId();
    }
}
