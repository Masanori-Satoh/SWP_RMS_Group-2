package com.group2.rms.notification;

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
        String eventId = "REQ_REJECT_" + requisition.getRequisitionId() + "_V" + requisition.getVersion();

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
}
