package com.group2.rms.notification;

import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * INTEGRATION TEST: NotificationRepository + NotificationService + SQL Server THẬT (cần SQL Server đang chạy).
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional
class NotificationIntegrationTests {

    @Autowired
    private NotificationRepository notifications;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserRepository users;
    @Autowired
    private RoleRepository roles;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private Clock clock;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now(clock);
    }

    // ----- Hàm hỗ trợ -----

    private User savedUser(String username) {
        Role role = roles.findByRoleName(RequisitionAccess.ROLE_HIRING_MANAGER)
                .orElseGet(() -> roles.save(Role.builder().roleName(RequisitionAccess.ROLE_HIRING_MANAGER).build()));
        return users.findByUsernameIgnoreCase(username).orElseGet(() -> users.save(User.builder()
                .username(username).passwordHash("$2a$10$dummy").fullName("Tester " + username)
                .email(username + "@rms.local").accountStatus("Active").role(role).build()));
    }

    /** Thông báo đủ các cột NOT NULL; {@code eventId} riêng cho mỗi lần gọi. */
    private Notification savedNotification(User recipient, String eventType, String referenceId, boolean read,
            String eventId) {
        return notifications.saveAndFlush(Notification.builder().recipient(recipient).title("IT-NOTI " + eventId)
                .content("Nội dung " + eventId).eventType(eventType).referenceId(referenceId)
                .linkUrl("/notifications").isRead(read).readAt(read ? now.minusDays(2) : null)
                .eventId(eventId).createdAt(now).build());
    }

    private Notification reloaded(Notification notification) {
        return notifications.findById(notification.getNotificationId()).orElseThrow();
    }

    // =========================================================================
    // Câu UPDATE tự viết
    // =========================================================================

    @Test
    @DisplayName("Đánh dấu tất cả -> Chỉ đổi thông báo chưa đọc của đúng người; thông báo đã đọc giữ thời điểm cũ; người khác không đổi")
    void markAllAsReadByUserId_shouldOnlyTouchOwnUnreadRows_onSqlServer() {
        // Arrange
        User owner = savedUser("it_noti_owner");
        User other = savedUser("it_noti_other");
        Notification unread = savedNotification(owner, "REQUISITION_REJECTED", "1", false, "IT_A1");
        Notification alreadyRead = savedNotification(owner, "REQUISITION_REJECTED", "2", true, "IT_A2");
        LocalDateTime firstReadAt = alreadyRead.getReadAt();
        Notification othersUnread = savedNotification(other, "REQUISITION_REJECTED", "1", false, "IT_A3");
        entityManager.clear();

        // Act
        int updated = notifications.markAllAsReadByUserId(owner.getUserId(), now);
        entityManager.clear();

        // Assert
        assertEquals(1, updated);
        assertTrue(reloaded(unread).getIsRead());
        assertEquals(now, reloaded(unread).getReadAt());
        assertEquals(firstReadAt, reloaded(alreadyRead).getReadAt());
        assertFalse(reloaded(othersUnread).getIsRead());
    }

    @Test
    @DisplayName("Ứng viên mở offer -> Chỉ đánh dấu thông báo OFFER_LETTER_SENT của đúng offer, không đụng loại khác / offer khác")
    void markOfferNotificationRead_shouldOnlyTouchMatchingOfferLetter_onSqlServer() {
        // Arrange
        User candidate = savedUser("it_noti_candidate");
        Notification target = savedNotification(candidate, "OFFER_LETTER_SENT", "77", false, "IT_O1");
        Notification otherOffer = savedNotification(candidate, "OFFER_LETTER_SENT", "78", false, "IT_O2");
        Notification otherType = savedNotification(candidate, "OFFER_ACCEPTED", "77", false, "IT_O3");
        entityManager.clear();

        // Act
        int updated = notifications.markOfferNotificationRead(candidate.getUserId(), "77", now);
        entityManager.clear();

        // Assert
        assertEquals(1, updated);
        assertTrue(reloaded(target).getIsRead());
        assertFalse(reloaded(otherOffer).getIsRead());
        assertFalse(reloaded(otherType).getIsRead());
    }

    // =========================================================================
    // Truy vấn theo người nhận
    // =========================================================================

    @Test
    @DisplayName("Tìm theo id + người nhận -> Không trả thông báo của người khác")
    void findByNotificationIdAndRecipient_shouldNotReturnOthersNotification_onSqlServer() {
        // Arrange
        User owner = savedUser("it_noti_owner");
        User other = savedUser("it_noti_other");
        Notification notification = savedNotification(owner, "REQUISITION_REJECTED", "1", false, "IT_F1");
        entityManager.clear();

        // Act + Assert
        assertTrue(notifications.findByNotificationIdAndRecipient_UserId(notification.getNotificationId(),
                owner.getUserId()).isPresent());
        assertTrue(notifications.findByNotificationIdAndRecipient_UserId(notification.getNotificationId(),
                other.getUserId()).isEmpty());
    }

    @Test
    @DisplayName("Danh sách + đếm chưa đọc -> Chỉ của người xem, mới nhất lên đầu")
    void listAndCount_shouldBeScopedToRecipientAndNewestFirst_onSqlServer() {
        // Arrange
        User owner = savedUser("it_noti_owner");
        User other = savedUser("it_noti_other");
        Notification older = savedNotification(owner, "REQUISITION_REJECTED", "1", false, "IT_L1");
        older.setCreatedAt(now.minusHours(3));
        Notification newer = savedNotification(owner, "JOB_POSTING_PUBLISHED", "2", true, "IT_L2");
        savedNotification(other, "REQUISITION_REJECTED", "1", false, "IT_L3");
        notifications.saveAndFlush(older);
        entityManager.clear();

        // Act
        List<Notification> rows = notifications
                .findByRecipient_UserIdOrderByCreatedAtDesc(owner.getUserId(), PageRequest.of(0, 10)).getContent();
        long unread = notifications.countByRecipient_UserIdAndIsReadFalse(owner.getUserId());

        // Assert
        assertEquals(List.of(newer.getNotificationId(), older.getNotificationId()),
                rows.stream().map(Notification::getNotificationId).toList());
        assertEquals(1, unread);
    }

    // =========================================================================
    // Ràng buộc DB và NVARCHAR
    // =========================================================================

    @Test
    @DisplayName("Lưu 2 thông báo cùng người nhận + cùng EventId -> DB chặn (unique index UX_Notification_Recipient_EventId)")
    void save_shouldBeRejectedByUniqueIndex_whenRecipientAndEventIdRepeat() {
        // Arrange
        User owner = savedUser("it_noti_owner");
        savedNotification(owner, "REQUISITION_REJECTED", "1", false, "IT_DUP");

        // Act + Assert
        assertThrows(DataIntegrityViolationException.class,
                () -> savedNotification(owner, "REQUISITION_REJECTED", "1", false, "IT_DUP"));
    }

    @Test
    @DisplayName("Cùng EventId nhưng khác người nhận -> Lưu được (unique theo cặp người nhận + EventId)")
    void save_shouldAllowSameEventId_whenRecipientsDiffer() {
        // Arrange
        User owner = savedUser("it_noti_owner");
        User other = savedUser("it_noti_other");
        savedNotification(owner, "REQUISITION_REJECTED", "1", false, "IT_SHARED");

        // Act
        savedNotification(other, "REQUISITION_REJECTED", "1", false, "IT_SHARED");

        // Assert
        assertTrue(notifications.existsByRecipient_UserIdAndEventId(other.getUserId(), "IT_SHARED"));
    }

    @Test
    @DisplayName("Nội dung tiếng Việt có xuống dòng -> Đọc lại từ DB giữ nguyên dấu và ký tự xuống dòng")
    void save_shouldKeepVietnameseTextAndNewlines_onSqlServer() {
        // Arrange
        User owner = savedUser("it_noti_owner");
        String content = "Yêu cầu \"Kỹ sư phần mềm\" đã bị Giám đốc từ chối.\nLý do: Ngân sách vượt mức đề xuất.";
        Notification notification = notifications.saveAndFlush(Notification.builder().recipient(owner)
                .title("Yêu cầu tuyển dụng đã bị từ chối: Kỹ sư phần mềm").content(content)
                .eventType("REQUISITION_REJECTED").eventId("IT_VN").createdAt(now).build());
        entityManager.clear();

        // Act
        Notification reloaded = reloaded(notification);

        // Assert
        assertEquals(content, reloaded.getContent());
        assertEquals("Yêu cầu tuyển dụng đã bị từ chối: Kỹ sư phần mềm", reloaded.getTitle());
    }

    // =========================================================================
    // Transaction
    // =========================================================================

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Nghiệp vụ gọi notify rồi lỗi (rollback) -> Thông báo cũng không được ghi (notify dùng chung transaction)")
    void notify_shouldNotPersist_whenCallerTransactionRollsBack() {
        // Arrange: tạo người nhận trong transaction riêng và commit (test này không chạy trong transaction chung)
        TransactionTemplate inNewTransaction = new TransactionTemplate(transactionManager);
        inNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        User owner = inNewTransaction.execute(status -> savedUser("it_noti_rollback"));
        // Bảng Notification chỉ có khóa ngoại tới người nhận, nên yêu cầu / bản ghi từ chối chỉ cần nằm trong bộ nhớ
        JobRequisition requisition = JobRequisition.builder().requisitionId(-1).title("IT-NOTI rollback")
                .hiringManager(owner).build();
        RequisitionApproval rejection = RequisitionApproval.builder().approvalId(-1).requisition(requisition)
                .comments("Lý do").build();
        try {
            // Act: mô phỏng nghiệp vụ từ chối đã gọi notify rồi gặp lỗi phía sau
            assertThrows(IllegalStateException.class, () -> inNewTransaction.executeWithoutResult(status -> {
                notificationService.notifyRequisitionRejected(rejection);
                notifications.flush();
                throw new IllegalStateException("Lỗi sau khi đã gọi notify");
            }));

            // Assert
            assertFalse(notifications.existsByRecipient_UserIdAndEventId(owner.getUserId(), "REQ_REJECT_-1"));
        } finally {
            inNewTransaction.executeWithoutResult(status -> users.deleteById(owner.getUserId()));
        }
    }
}
