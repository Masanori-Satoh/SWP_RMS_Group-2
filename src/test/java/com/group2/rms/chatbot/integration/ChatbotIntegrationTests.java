package com.group2.rms.chatbot.integration;

import com.group2.rms.chatbot.service.ChatbotJobService;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * INTEGRATION TEST: chatbot + SQL Server THẬT (cần SQL Server đang chạy).
 * Mỗi test chạy trong một transaction và rollback ở cuối (@Transactional), nên không để lại dữ liệu.
 * Kiểm tra: 2 câu JPQL mới của JobPostingRepository (findOpenWithDepartment, findByJobPostingIdIn) lọc đúng và
 * nạp sẵn phòng ban; tìm tiếng Việt có dấu qua NVARCHAR; API /api/chatbot/message (khách gọi được).
 * Dữ liệu tạo trong một phòng ban riêng, tiêu đề riêng để không đụng dữ liệu seed.
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện, "bây giờ" lấy từ bean Clock.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional
class ChatbotIntegrationTests {

    private static final String TITLE_PREFIX = "ITCB ";

    @Autowired
    private ChatbotJobService chatbotJobService;
    @Autowired
    private JobPostingRepository postings;
    @Autowired
    private JobRequisitionRepository requisitions;
    @Autowired
    private DepartmentRepository departments;
    @Autowired
    private UserRepository users;
    @Autowired
    private RoleRepository roles;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private Clock clock;
    @Autowired
    private MockMvc mvc;

    private LocalDateTime now;
    private Department department;
    private User hr;
    private User hiringManager;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now(clock);
        department = departments.save(Department.builder().departmentName("ITCB Phòng Chatbot")
                .departmentStatus("Active").build());
        hr = savedUser("it_cb_hr", RequisitionAccess.ROLE_HR);
        hiringManager = savedUser("it_cb_hm", RequisitionAccess.ROLE_HIRING_MANAGER);
    }

    // ----- Hàm hỗ trợ -----

    private User savedUser(String username, String roleName) {
        Role role = roles.findByRoleName(roleName).orElseGet(() -> roles.save(Role.builder().roleName(roleName).build()));
        return users.findByUsernameIgnoreCase(username).orElseGet(() -> users.save(User.builder()
                .username(username).passwordHash("$2a$10$dummy").fullName("Tester " + username)
                .email(username + "@rms.local").accountStatus("Active").role(role).department(department).build()));
    }

    private JobPosting savedPosting(String title, String location, String status, LocalDateTime postingDate,
            LocalDateTime deadline) {
        JobRequisition requisition = requisitions.saveAndFlush(JobRequisition.builder().title(title)
                .recruitmentRound(1).numberOfPositions(1).employmentType("Full-time").gender("Any").workModel("On-site")
                .jobDescription("Mô tả").requirementDetails("Yêu cầu")
                .approvalStatus(RequisitionAccess.STATUS_APPROVED).hiringManager(hiringManager)
                .department(department).screeningCriteria(new ArrayList<>()).build());
        return postings.saveAndFlush(JobPosting.builder().requisition(requisition).postingNo(1).postingTitle(title)
                .jobDescription("Mô tả").jobRequirements("Có kinh nghiệm\nLàm việc nhóm").workLocation(location)
                .postingStatus(status).postingDate(postingDate).applicationDeadline(deadline).createdBy(hr).build());
    }

    private List<String> openTitles() {
        return postings.findOpenWithDepartment(JobPostingAccess.STATUS_PUBLISHED, now).stream()
                .map(JobPosting::getPostingTitle)
                .filter(title -> title.startsWith(TITLE_PREFIX))
                .toList();
    }

    @Test
    @DisplayName("findOpenWithDepartment -> Chỉ tin Published đã tới giờ đăng, chưa hết hạn; bỏ nháp / hẹn đăng / hết hạn")
    void findOpenWithDepartment_shouldReturnOnlyOpenPostings_onSqlServer() {
        // Arrange
        savedPosting(TITLE_PREFIX + "Đang mở", "Hà Nội", JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(5));
        savedPosting(TITLE_PREFIX + "Nháp", "Hà Nội", JobPostingAccess.STATUS_DRAFT, null, now.plusDays(5));
        savedPosting(TITLE_PREFIX + "Hẹn đăng", "Hà Nội", JobPostingAccess.STATUS_PUBLISHED, now.plusDays(2), now.plusDays(9));
        savedPosting(TITLE_PREFIX + "Hết hạn", "Hà Nội", JobPostingAccess.STATUS_PUBLISHED, now.minusDays(9), now.minusDays(1));
        entityManager.clear();

        // Act + Assert
        assertEquals(List.of(TITLE_PREFIX + "Đang mở"), openTitles());
    }

    @Test
    @DisplayName("findOpenWithDepartment / findByJobPostingIdIn -> Phòng ban đã nạp sẵn (không chạy thêm câu SELECT)")
    void newQueries_shouldFetchDepartment_onSqlServer() {
        // Arrange
        JobPosting closed = savedPosting(TITLE_PREFIX + "Đã đóng", "Đà Nẵng", JobPostingAccess.STATUS_CLOSED,
                now.minusDays(9), now.plusDays(5));
        savedPosting(TITLE_PREFIX + "Đang mở", "Hà Nội", JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(5));
        entityManager.clear();

        // Act
        List<JobPosting> open = postings.findOpenWithDepartment(JobPostingAccess.STATUS_PUBLISHED, now);
        List<JobPosting> byIds = postings.findByJobPostingIdIn(List.of(closed.getJobPostingId()));

        // Assert
        assertTrue(Hibernate.isInitialized(open.get(0).getRequisition().getDepartment()));
        assertEquals(1, byIds.size());
        assertTrue(Hibernate.isInitialized(byIds.get(0).getRequisition().getDepartment()));
        assertEquals("ITCB Phòng Chatbot", byIds.get(0).getRequisition().getDepartment().getDepartmentName());
    }

    @Test
    @DisplayName("Tìm \"itcb ha noi\" không dấu -> Khớp tin \"Hà Nội\" lưu NVARCHAR có dấu")
    void search_shouldMatchVietnameseLocation_onSqlServer() {
        // Arrange
        savedPosting(TITLE_PREFIX + "Kỹ sư", "Hà Nội", JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(5));
        savedPosting(TITLE_PREFIX + "Kế toán", "Đà Nẵng", JobPostingAccess.STATUS_PUBLISHED, now.minusDays(1), now.plusDays(5));
        entityManager.clear();

        // Act
        ChatbotJobService.SearchResult result = chatbotJobService.search("tìm việc itcb ha noi");

        // Assert
        assertEquals(1, result.total());
        assertEquals(TITLE_PREFIX + "Kỹ sư", result.jobs().get(0).title());
    }

    @Test
    @DisplayName("Khách gọi API hỏi chi tiết một tin đang mở -> 200, có tiêu đề và dòng yêu cầu")
    void message_shouldReturnJobDetail_forGuest_onSqlServer() throws Exception {
        // Arrange
        JobPosting open = savedPosting(TITLE_PREFIX + "Chi tiết", "Hà Nội", JobPostingAccess.STATUS_PUBLISHED,
                now.minusDays(1), now.plusDays(5));
        entityManager.clear();

        // Act + Assert
        mvc.perform(post("/api/chatbot/message").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobPostingId\":" + open.getJobPostingId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("JOB_DETAIL"))
                .andExpect(jsonPath("$.data.jobDetail.title").value(TITLE_PREFIX + "Chi tiết"))
                .andExpect(jsonPath("$.data.jobDetail.requirements[0]").value("Có kinh nghiệm"));
    }
}
