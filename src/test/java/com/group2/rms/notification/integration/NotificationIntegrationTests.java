package com.group2.rms.notification.integration;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.notification.entity.Notification;
import com.group2.rms.notification.repository.NotificationRepository;
import com.group2.rms.notification.service.NotificationService;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.repository.RequisitionApprovalRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * INTEGRATION TEST: NotificationRepository / NotificationService / 2 controller trên SQL Server THẬT (cần SQL Server chạy).
 * Mỗi test rollback ở cuối (@Transactional); user test là tài khoản riêng "it_noti_*" nên đếm số chính xác được.
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện, Arrange / Act / Assert.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional
class NotificationIntegrationTests {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2099, 1, 1, 8, 0);

    @Autowired
    private NotificationService service;
    @Autowired
    private NotificationRepository notifications;
    @Autowired
    private JobRequisitionRepository requisitions;
    @Autowired
    private RequisitionApprovalRepository approvals;
    @Autowired
    private DepartmentRepository departments;
    @Autowired
    private UserRepository users;
    @Autowired
    private RoleRepository roles;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private MockMvc mvc;

    private Department department;
    private User userA;
    private User userB;
    private User director;
    private User hr;

    @BeforeEach
    void setUp() {
        department = departments.findAll().stream().findFirst()
                .orElseGet(() -> departments.save(Department.builder().departmentName("Engineering").build()));
        userA = saveUser("it_noti_a", "Người Nhận A", RequisitionAccess.ROLE_HIRING_MANAGER);
        userB = saveUser("it_noti_b", "Người Nhận B", RequisitionAccess.ROLE_HIRING_MANAGER);
        director = saveUser("it_noti_dir", "Giám Đốc Test", RequisitionAccess.ROLE_DIRECTOR);
        hr = saveUser("it_noti_hr", "HR Test", RequisitionAccess.ROLE_HR);
    }

    // ----- Hàm hỗ trợ -----

    private User saveUser(String username, String fullName, String roleName) {
        Role role = roles.findByRoleName(roleName)
                .orElseGet(() -> roles.save(Role.builder().roleName(roleName).build()));
        return users.findByUsernameIgnoreCase(username).orElseGet(() -> users.save(User.builder()
                .username(username).passwordHash("$2a$10$dummy").fullName(fullName)
                .email(username + "@rms.local").accountStatus("Active").role(role).department(department)
                .build()));
    }

    // Thông báo chưa đọc với đủ cột NOT NULL; từng test tự đổi giờ tạo / loại sự kiện
    private Notification.NotificationBuilder unreadNotification(User recipient, String title) {
        return Notification.builder().recipient(recipient).title(title).content("Nội dung " + title)
                .eventType("REQUISITION_APPROVED").referenceId("1").linkUrl("/requisitions/1")
                .eventId("IT_NOTI_" + title).createdAt(BASE_TIME);
    }

    private Notification saveNotification(Notification.NotificationBuilder builder) {
        return notifications.saveAndFlush(builder.build());
    }

    // Đọc lại từ DB, không lấy bản trong cache của EntityManager
    private Notification reload(Notification notification) {
        entityManager.flush();
        entityManager.clear();
        return notifications.findById(notification.getNotificationId()).orElseThrow();
    }

    private List<Notification> findNotificationsOf(User recipient) {
        return notifications.findByRecipient_UserIdOrderByCreatedAtDesc(recipient.getUserId(), PageRequest.of(0, 50))
                .getContent();
    }

    private RequisitionApproval saveApproval(String status, String comments) {
        JobRequisition requisition = requisitions.saveAndFlush(JobRequisition.builder()
                .title("IT Noti Backend").hiringManager(userA).department(department)
                .approvalStatus(status).recruitmentRound(1).numberOfPositions(1)
                .employmentType("Full-time").gender("Any").workModel("On-site")
                .jobDescription("Integration-test responsibilities").requirementDetails("Integration-test requirements").build());
        return approvals.saveAndFlush(RequisitionApproval.builder().requisition(requisition).director(director)
                .status(status).comments(comments).approvalDate(BASE_TIME).build());
    }

    // ----- A. Câu query trên SQL Server -----

    @Nested
    @DisplayName("Repository")
    class RepositoryQueries {

        @Test
        @DisplayName("Hai người cùng có thông báo -> Mỗi người chỉ thấy của mình, mới nhất trước, đúng kích thước trang")
        void findByRecipient_shouldReturnOwnNewestFirst_whenTwoUsersHaveNotifications() {
            // Arrange
            saveNotification(unreadNotification(userA, "A cũ").createdAt(BASE_TIME));
            saveNotification(unreadNotification(userA, "A giữa").createdAt(BASE_TIME.plusHours(1)));
            saveNotification(unreadNotification(userA, "A mới").createdAt(BASE_TIME.plusHours(2)));
            saveNotification(unreadNotification(userB, "B mới hơn").createdAt(BASE_TIME.plusHours(3)));

            // Act
            Page<Notification> page = notifications.findByRecipient_UserIdOrderByCreatedAtDesc(
                    userA.getUserId(), PageRequest.of(0, 2));

            // Assert
            assertEquals(3, page.getTotalElements());
            assertEquals(List.of("A mới", "A giữa"), page.getContent().stream().map(Notification::getTitle).toList());
        }

        @Test
        @DisplayName("Có cả đã đọc và chưa đọc -> Chỉ đếm thông báo chưa đọc của đúng người")
        void countByRecipientUnread_shouldCountOnlyOwnUnread_whenSomeAreRead() {
            // Arrange
            saveNotification(unreadNotification(userA, "A1"));
            saveNotification(unreadNotification(userA, "A2"));
            saveNotification(unreadNotification(userA, "A3").isRead(true).readAt(BASE_TIME));
            saveNotification(unreadNotification(userB, "B1"));

            // Act
            long count = notifications.countByRecipient_UserIdAndIsReadFalse(userA.getUserId());

            // Assert
            assertEquals(2, count);
        }

        @Test
        @DisplayName("Đánh dấu tất cả đã đọc -> Chỉ đổi của mình; người khác vẫn chưa đọc, bản đã đọc giữ ReadAt cũ")
        void markAllAsRead_shouldTouchOnlyOwnUnread_whenOtherUserHasUnread() {
            // Arrange
            LocalDateTime oldReadAt = BASE_TIME.minusDays(5);
            Notification ownUnread = saveNotification(unreadNotification(userA, "A chưa đọc"));
            Notification ownRead = saveNotification(unreadNotification(userA, "A đã đọc").isRead(true).readAt(oldReadAt));
            Notification otherUnread = saveNotification(unreadNotification(userB, "B chưa đọc"));

            // Act
            int updated = notifications.markAllAsReadByUserId(userA.getUserId(), BASE_TIME);

            // Assert
            assertEquals(1, updated);
            assertTrue(reload(ownUnread).getIsRead());
            assertEquals(BASE_TIME, reload(ownUnread).getReadAt());
            assertEquals(oldReadAt, reload(ownRead).getReadAt());
            assertFalse(reload(otherUnread).getIsRead());
        }

        @Test
        @DisplayName("Ứng viên mở offer -> Chỉ đánh dấu OFFER_LETTER_SENT của đúng offer đó")
        void markOfferNotificationRead_shouldMarkOnlyThatOffer_whenOtherEventsShareReference() {
            // Arrange
            Notification offerSent = saveNotification(unreadNotification(userA, "Offer 77").eventType("OFFER_LETTER_SENT").referenceId("77"));
            Notification otherOffer = saveNotification(unreadNotification(userA, "Offer 78").eventType("OFFER_LETTER_SENT").referenceId("78"));
            Notification otherEvent = saveNotification(unreadNotification(userA, "Khác 77").eventType("INTERVIEW_SCHEDULED").referenceId("77"));

            // Act
            service.markOfferNotificationRead(userA.getUserId(), 77);

            // Assert
            assertTrue(reload(offerSent).getIsRead());
            assertFalse(reload(otherOffer).getIsRead());
            assertFalse(reload(otherEvent).getIsRead());
        }

        @Test
        @DisplayName("Một Giám đốc đã xử lý yêu cầu -> Thông báo chờ duyệt của mọi Giám đốc cho yêu cầu đó thành đã đọc")
        void markRequisitionPendingRead_shouldMarkEveryRecipient_whenRequisitionDecided() {
            // Arrange
            String pendingEventType = "REQUISITION_PENDING_DIRECTOR";
            Notification pendingForA = saveNotification(unreadNotification(userA, "Chờ duyệt A").eventType(pendingEventType).referenceId("987654"));
            Notification pendingForB = saveNotification(unreadNotification(userB, "Chờ duyệt B").eventType(pendingEventType).referenceId("987654"));
            Notification otherRequisition = saveNotification(unreadNotification(userA, "Chờ duyệt khác").eventType(pendingEventType).referenceId("987655"));

            // Act
            service.markRequisitionPendingRead(987654);

            // Assert
            assertTrue(reload(pendingForA).getIsRead());
            assertTrue(reload(pendingForB).getIsRead());
            assertFalse(reload(otherRequisition).getIsRead());
        }

        @Test
        @DisplayName("Tiêu đề / nội dung tiếng Việt -> Đọc lại từ DB giữ nguyên dấu")
        void save_shouldKeepVietnameseText_whenTextHasDiacritics() {
            // Arrange
            String title = "Yêu cầu tuyển dụng đã được phê duyệt: Kỹ sư phần mềm";
            String content = "Giám đốc Nguyễn Thị Hằng đã đồng ý. Ý kiến: “Tuyển gấp trước Tết”.";
            Notification saved = saveNotification(unreadNotification(userA, "VN").title(title).content(content));

            // Act
            Notification reloaded = reload(saved);

            // Assert
            assertEquals(title, reloaded.getTitle());
            assertEquals(content, reloaded.getContent());
        }
    }

    // ----- B. Service + DB: chống trùng, quyền sở hữu -----

    @Nested
    @DisplayName("Service")
    class ServiceWithDatabase {

        @Test
        @DisplayName("Gọi thông báo từ chối hai lần cho cùng một lần duyệt -> DB chỉ có một dòng")
        void notifyRequisitionRejected_shouldSaveOnce_whenCalledTwice() {
            // Arrange
            RequisitionApproval rejection = saveApproval(RequisitionAccess.STATUS_REJECTED, "Thiếu ngân sách");

            // Act
            service.notifyRequisitionRejected(rejection);
            service.notifyRequisitionRejected(rejection);

            // Assert
            List<Notification> rejectedNotifications = findNotificationsOf(userA).stream()
                    .filter(n -> "REQUISITION_REJECTED".equals(n.getEventType())).toList();
            assertEquals(1, rejectedNotifications.size());
            assertEquals("REQ_REJECT_" + rejection.getApprovalId(), rejectedNotifications.get(0).getEventId());
        }

        @Test
        @DisplayName("Giám đốc duyệt -> HM và HR đều nhận một dòng, dùng chung EventId mà DB không báo trùng")
        void notifyRequisitionApproved_shouldNotifyManagerAndHr_whenBothShareEventId() {
            // Arrange
            RequisitionApproval approval = saveApproval(RequisitionAccess.STATUS_APPROVED, "Đồng ý");
            String eventId = "REQ_APPROVE_" + approval.getApprovalId();

            // Act
            service.notifyRequisitionApproved(approval);
            entityManager.flush();

            // Assert
            assertTrue(notifications.existsByRecipient_UserIdAndEventId(userA.getUserId(), eventId));
            assertTrue(notifications.existsByRecipient_UserIdAndEventId(hr.getUserId(), eventId));
            assertFalse(notifications.existsByRecipient_UserIdAndEventId(director.getUserId(), eventId));
        }

        @Test
        @DisplayName("Đánh dấu đã đọc thông báo của người khác -> ResourceNotFoundException, thông báo vẫn chưa đọc")
        void markAsRead_shouldThrowNotFound_whenNotificationBelongsToOtherUser() {
            // Arrange
            Notification otherUsersNotification = saveNotification(unreadNotification(userB, "Của B"));

            // Act + Assert
            assertThrows(ResourceNotFoundException.class, () -> service.markAsRead(otherUsersNotification.getNotificationId(), userA));
            assertFalse(reload(otherUsersNotification).getIsRead());
        }
    }

    // ----- C. HTTP qua bộ lọc bảo mật thật -----

    @Nested
    @DisplayName("HTTP")
    class HttpEndpoints {

        @Test
        @WithMockUser(username = "it_noti_a", roles = "HIRING_MANAGER")
        @DisplayName("Chuông gọi số chưa đọc -> JSON đúng số của người đang đăng nhập")
        void getUnreadCount_shouldReturnOwnCount_whenLoggedIn() throws Exception {
            // Arrange
            saveNotification(unreadNotification(userA, "A1"));
            saveNotification(unreadNotification(userA, "A2"));
            saveNotification(unreadNotification(userB, "B1"));

            // Act + Assert
            mvc.perform(get("/api/notifications/unread-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(2));
        }

        @Test
        @WithMockUser(username = "it_noti_a", roles = "HIRING_MANAGER")
        @DisplayName("Chuông lấy danh sách gần đây -> Tối đa 5 dòng, mới nhất trước, không có của người khác")
        void getRecentNotifications_shouldReturnFiveNewestOwn_whenUserHasMore() throws Exception {
            // Arrange
            for (int i = 1; i <= 6; i++) {
                saveNotification(unreadNotification(userA, "A" + i).createdAt(BASE_TIME.plusMinutes(i)));
            }
            saveNotification(unreadNotification(userB, "B mới nhất").createdAt(BASE_TIME.plusDays(1)));

            // Act + Assert
            mvc.perform(get("/api/notifications/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(6))
                    .andExpect(jsonPath("$.notifications.length()").value(5))
                    .andExpect(jsonPath("$.notifications[0].title").value("A6"))
                    .andExpect(jsonPath("$.notifications[4].title").value("A2"));
        }

        @Test
        @WithMockUser(username = "it_noti_a", roles = "HIRING_MANAGER")
        @DisplayName("API đánh dấu đã đọc id của người khác -> 400 success=false, thông báo vẫn chưa đọc")
        void markAsRead_shouldReturnBadRequest_whenApiCalledWithOtherUsersId() throws Exception {
            // Arrange
            Notification otherUsersNotification = saveNotification(unreadNotification(userB, "Của B"));

            // Act
            mvc.perform(post("/api/notifications/{id}/read", otherUsersNotification.getNotificationId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            // Assert
            assertFalse(reload(otherUsersNotification).getIsRead());
        }

        @Test
        @DisplayName("Chưa đăng nhập mở /notifications -> Chuyển về trang đăng nhập")
        void list_shouldRedirectToLogin_whenAnonymous() throws Exception {
            // Act + Assert
            mvc.perform(get("/notifications"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @WithMockUser(username = "it_noti_a", roles = "HIRING_MANAGER")
        @DisplayName("Đã đăng nhập mở /notifications -> Render trang danh sách kèm số chưa đọc")
        void list_shouldRenderWithUnreadCount_whenLoggedIn() throws Exception {
            // Arrange
            saveNotification(unreadNotification(userA, "A1"));

            // Act + Assert
            mvc.perform(get("/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("notifications/list"))
                    .andExpect(model().attribute("unreadCount", 1L));
        }

        @Test
        @WithMockUser(username = "it_noti_a", roles = "HIRING_MANAGER")
        @DisplayName("Bấm mở thông báo có link nội bộ -> Đánh dấu đã đọc và chuyển tới link đó")
        void markAsRead_shouldRedirectToLink_whenOpenInternalLink() throws Exception {
            // Arrange
            Notification own = saveNotification(unreadNotification(userA, "Link nội bộ").linkUrl("/requisitions/1"));

            // Act
            mvc.perform(post("/notifications/{id}/read", own.getNotificationId()).param("open", "true").with(csrf()))
                    .andExpect(redirectedUrl("/requisitions/1"));

            // Assert
            assertTrue(reload(own).getIsRead());
        }

        @Test
        @WithMockUser(username = "it_noti_a", roles = "HIRING_MANAGER")
        @DisplayName("Bấm mở thông báo có link //trang-ngoài -> Không đi ra ngoài, quay về /notifications")
        void markAsRead_shouldStayOnList_whenOpenExternalLink() throws Exception {
            // Arrange
            Notification own = saveNotification(unreadNotification(userA, "Link ngoài").linkUrl("//evil.example.com"));

            // Act + Assert
            mvc.perform(post("/notifications/{id}/read", own.getNotificationId()).param("open", "true").with(csrf()))
                    .andExpect(redirectedUrl("/notifications"));
        }
    }
}
