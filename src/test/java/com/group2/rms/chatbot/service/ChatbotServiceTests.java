package com.group2.rms.chatbot.service;

import com.group2.rms.candidate.dto.MyApplicationListResponse;
import com.group2.rms.candidate.dto.MyApplicationResponse;
import com.group2.rms.candidate.service.MyApplicationService;
import com.group2.rms.chatbot.dto.ChatRequest;
import com.group2.rms.chatbot.dto.ChatResponse;
import com.group2.rms.chatbot.dto.ChatResponse.Contact;
import com.group2.rms.chatbot.dto.ChatResponse.JobCard;
import com.group2.rms.chatbot.dto.ChatResponse.JobDetail;
import com.group2.rms.chatbot.dto.ChatResponse.Link;
import com.group2.rms.chatbot.validator.ChatbotValidator;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST (Mockito): ChatbotService (điều phối một lượt hỏi - đáp).
 * Mock: ChatbotConfigService, ChatbotJobService, MyApplicationService, CurrentUserService.
 * Bản thật: ChatbotIntentResolver, ChatbotAccess, ChatbotValidator (hàm thuần) để chạy luật thật.
 * Trọng tâm: khách / tài khoản nội bộ KHÔNG đọc được hồ sơ (verifyNoInteractions(myApplications)).
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện, Arrange / Act / Assert.
 */
@ExtendWith(MockitoExtension.class)
class ChatbotServiceTests {

    private static final String FAQ_ANSWER = "Bấm Ứng tuyển ngay rồi tải CV lên.";
    private static final Contact HR_CONTACT = new Contact("hr@moc.com", null, null);

    @Mock
    private ChatbotConfigService config;
    @Mock
    private ChatbotJobService jobs;
    @Mock
    private MyApplicationService myApplications;
    @Mock
    private CurrentUserService currentUserService;

    private ChatbotService service;

    @BeforeEach
    void setUp() {
        service = new ChatbotService(new ChatbotIntentResolver(), config, jobs, new ChatbotAccess(),
                new ChatbotValidator(), myApplications, currentUserService);
        lenient().when(config.isEnabled()).thenReturn(true);
        lenient().when(config.loadFaqs()).thenReturn(List.of(
                new ChatbotFaq(List.of("cach nop"), "Cách nộp hồ sơ", FAQ_ANSWER)));
        lenient().when(config.loadHrContact()).thenReturn(HR_CONTACT);
    }

    // ----- Hàm hỗ trợ -----

    private static User user(String roleName) {
        return User.builder().userId(7).username("u_" + roleName).fullName("Nguyễn Văn A")
                .role(Role.builder().roleName(roleName).build()).build();
    }

    private void signedInAs(String roleName) {
        when(currentUserService.requireUser()).thenReturn(user(roleName));
    }

    private ChatResponse guestAsks(String message) {
        return service.reply(new ChatRequest(message, null), false);
    }

    private ChatResponse signedInAsks(String message) {
        return service.reply(new ChatRequest(message, null), true);
    }

    private static MyApplicationResponse application(int id, int jobPostingId) {
        return new MyApplicationResponse(id, jobPostingId, "Vị trí " + id, null, null, 1, "Nộp CV", "current",
                "Đã nộp hồ sơ", null, "neutral", null, null, true, List.of());
    }

    private void candidateHasApplications(int count) {
        List<MyApplicationResponse> owned = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            owned.add(application(i, 100 + i));
        }
        when(myApplications.list(isNull())).thenReturn(new MyApplicationListResponse(owned, Map.of(), null, count));
    }

    private static JobCard card(int id) {
        return new JobCard(id, "Java " + id, "Phòng Kỹ thuật", "Hà Nội", null, "30/10/2026", "/jobs/" + id, null);
    }

    @Test
    @DisplayName("Chatbot bị tắt -> Báo tạm ngưng, không đọc phiên, không đọc tin / hồ sơ")
    void reply_shouldAnswerDisabled_whenChatbotIsOff() {
        // Arrange
        when(config.isEnabled()).thenReturn(false);

        // Act
        ChatResponse response = signedInAsks("Hồ sơ của tôi");

        // Assert
        assertEquals(ChatbotIntent.DISABLED.name(), response.intent());
        assertEquals(ChatbotLabels.DISABLED, response.reply());
        verifyNoInteractions(currentUserService, jobs, myApplications);
    }

    @Nested
    @DisplayName("Lời chào")
    class Welcome {

        @Test
        @DisplayName("Khách gửi tin trống / chỉ khoảng trắng -> Lời chào chung + menu khách, không đọc phiên")
        void reply_shouldGreetGuest_whenMessageIsBlank() {
            // Act
            ChatResponse response = guestAsks("   ");

            // Assert
            assertEquals(ChatbotLabels.WELCOME, response.reply());
            assertEquals(ChatbotLabels.MAIN_MENU, response.quickReplies());
            verifyNoInteractions(currentUserService);
        }

        @Test
        @DisplayName("Ứng viên mở chat -> Chào theo tên + menu ứng viên (có Gợi ý việc làm)")
        void reply_shouldGreetCandidateByName_whenSignedIn() {
            // Arrange
            signedInAs(ChatbotAccess.ROLE_CANDIDATE);

            // Act
            ChatResponse response = signedInAsks(null);

            // Assert
            assertEquals(String.format(ChatbotLabels.WELCOME_CANDIDATE, "Nguyễn Văn A"), response.reply());
            assertEquals(ChatbotLabels.CANDIDATE_MENU, response.quickReplies());
        }
    }

    @Nested
    @DisplayName("Trạng thái hồ sơ")
    class ApplicationStatus {

        @Test
        @DisplayName("Khách hỏi hồ sơ -> Mời đăng nhập (link Đăng nhập, Tạo tài khoản), không đọc hồ sơ")
        void reply_shouldAskGuestToLogin_whenGuestAsksStatus() {
            // Act
            ChatResponse response = guestAsks("Hồ sơ của tôi tới bước nào rồi?");

            // Assert
            assertEquals(ChatbotLabels.LOGIN_REQUIRED, response.reply());
            assertEquals(List.of(ChatbotLabels.LINK_LOGIN, ChatbotLabels.LINK_REGISTER), response.links());
            assertTrue(response.applications().isEmpty());
            verifyNoInteractions(myApplications);
        }

        @Test
        @DisplayName("HR (tài khoản nội bộ) hỏi hồ sơ -> Chỉ dành cho ứng viên, không đọc hồ sơ")
        void reply_shouldRefuse_whenInternalUserAsksStatus() {
            // Arrange
            signedInAs("HR");

            // Act
            ChatResponse response = signedInAsks("trạng thái hồ sơ");

            // Assert
            assertEquals(ChatbotLabels.CANDIDATE_ONLY, response.reply());
            verifyNoInteractions(myApplications);
        }

        @Test
        @DisplayName("Ứng viên có nhiều hơn MAX_APPLICATION_CARDS hồ sơ -> Hiện MAX thẻ + câu \"mới cập nhật nhất\"")
        void reply_shouldListOwnApplications_whenCandidateAsksStatus() {
            // Arrange
            signedInAs(ChatbotAccess.ROLE_CANDIDATE);
            candidateHasApplications(ChatbotService.MAX_APPLICATION_CARDS + 1);

            // Act
            ChatResponse response = signedInAsks("Hồ sơ của tôi");

            // Assert
            assertEquals(ChatbotService.MAX_APPLICATION_CARDS, response.applications().size());
            assertEquals(String.format(ChatbotLabels.STATUS_FOUND_MORE, ChatbotService.MAX_APPLICATION_CARDS + 1,
                    ChatbotService.MAX_APPLICATION_CARDS), response.reply());
            assertEquals("Đã nộp hồ sơ", response.applications().get(0).statusLabel());
            assertEquals(List.of(ChatbotLabels.LINK_MY_APPLICATIONS), response.links());
        }

        @Test
        @DisplayName("Ứng viên chưa nộp hồ sơ nào -> Báo chưa có hồ sơ + nút Tìm việc")
        void reply_shouldSuggestSearch_whenCandidateHasNoApplication() {
            // Arrange
            signedInAs(ChatbotAccess.ROLE_CANDIDATE);
            candidateHasApplications(0);

            // Act
            ChatResponse response = signedInAsks("Hồ sơ của tôi");

            // Assert
            assertEquals(ChatbotLabels.STATUS_NONE, response.reply());
            assertEquals(List.of(ChatbotLabels.ASK_SEARCH), response.quickReplies());
        }
    }

    @Nested
    @DisplayName("Gợi ý việc")
    class Recommendation {

        @Test
        @DisplayName("Ứng viên hỏi gợi ý -> Truyền đúng id các tin đã nộp cho ChatbotJobService")
        void reply_shouldRecommendFromOwnApplications_whenCandidateAsks() {
            // Arrange
            signedInAs(ChatbotAccess.ROLE_CANDIDATE);
            candidateHasApplications(2);
            when(jobs.recommend(List.of(101, 102)))
                    .thenReturn(new ChatbotJobService.RecommendResult(true, List.of(card(5))));

            // Act
            ChatResponse response = signedInAsks("Có việc nào hợp với tôi không?");

            // Assert
            assertEquals(ChatbotLabels.RECOMMEND_FROM_HISTORY + " " + ChatbotLabels.RECOMMEND_ASK_SKILLS,
                    response.reply());
            assertEquals(1, response.jobs().size());
        }

        @Test
        @DisplayName("Khách hỏi gợi ý -> Mời đăng nhập, không gọi gợi ý")
        void reply_shouldAskGuestToLogin_whenGuestAsksRecommendation() {
            // Act
            ChatResponse response = guestAsks("gợi ý việc cho tôi");

            // Assert
            assertEquals(ChatbotLabels.LOGIN_REQUIRED, response.reply());
            verify(jobs, never()).recommend(any());
            verifyNoInteractions(myApplications);
        }
    }

    @Nested
    @DisplayName("Chi tiết tin")
    class JobDetailFlow {

        @Test
        @DisplayName("Khách bấm Xem chi tiết tin còn nhận -> Nút Ứng tuyển ngay (#apply) đứng trước, username = null")
        void reply_shouldOfferApplyLink_whenJobAcceptsApplications() {
            // Arrange
            JobDetail detail = new JobDetail(5, "Java", "Kỹ thuật", "Hà Nội", null, "30/10/2026", List.of(), List.of());
            when(jobs.detail(5, null)).thenReturn(Optional.of(new ChatbotJobService.DetailResult(detail, true)));

            // Act
            ChatResponse response = service.reply(new ChatRequest("bị bỏ qua", 5), false);

            // Assert
            assertEquals(detail, response.jobDetail());
            assertEquals(List.of(ChatbotLabels.applyLink(5), ChatbotLabels.jobDetailLink(5)), response.links());
            assertEquals("/jobs/5#apply", response.links().get(0).href());
        }

        @Test
        @DisplayName("Ứng viên đã nộp tin -> Chỉ còn link xem trang chi tiết")
        void reply_shouldHideApplyLink_whenCandidateCannotApply() {
            // Arrange
            signedInAs(ChatbotAccess.ROLE_CANDIDATE);
            JobDetail detail = new JobDetail(5, "Java", "Kỹ thuật", "Hà Nội", null, "30/10/2026", List.of(), List.of());
            when(jobs.detail(5, "u_Candidate")).thenReturn(Optional.of(new ChatbotJobService.DetailResult(detail, false)));

            // Act
            ChatResponse response = service.reply(new ChatRequest(null, 5), true);

            // Assert
            List<Link> links = response.links();
            assertEquals(List.of(ChatbotLabels.jobDetailLink(5)), links);
        }

        @Test
        @DisplayName("Tin không còn khả dụng -> Báo không còn, link xem tất cả vị trí, không có chi tiết")
        void reply_shouldSayNotAvailable_whenJobIsGone() {
            // Arrange
            when(jobs.detail(99, null)).thenReturn(Optional.empty());

            // Act
            ChatResponse response = service.reply(new ChatRequest(null, 99), false);

            // Assert
            assertEquals(ChatbotLabels.JOB_NOT_AVAILABLE, response.reply());
            assertNull(response.jobDetail());
            assertEquals(List.of(ChatbotLabels.LINK_ALL_JOBS), response.links());
        }
    }

    @Nested
    @DisplayName("FAQ, tìm việc, liên hệ, không hiểu")
    class PublicQuestions {

        @Test
        @DisplayName("Câu khớp FAQ -> Trả đúng câu trả lời FAQ, không tìm tin")
        void reply_shouldAnswerFaq_whenMessageMatchesFaq() {
            // Act
            ChatResponse response = guestAsks("Cách nộp đơn như thế nào?");

            // Assert
            assertEquals(ChatbotIntent.FAQ.name(), response.intent());
            assertEquals(FAQ_ANSWER, response.reply());
            verifyNoInteractions(jobs);
        }

        @Test
        @DisplayName("Tìm việc có từ khóa, ra 5 tin, hiện 3 -> Câu \"đây là 3 tin mới nhất\" + link xem tất cả")
        void reply_shouldSummarizeSearch_whenMoreJobsThanCards() {
            // Arrange
            when(jobs.search(anyString())).thenReturn(new ChatbotJobService.SearchResult(5,
                    List.of(card(1), card(2), card(3))));
            when(jobs.hasSearchTerms(anyString())).thenReturn(true);

            // Act
            ChatResponse response = guestAsks("tìm việc java");

            // Assert
            assertEquals(String.format(ChatbotLabels.SEARCH_FOUND_MORE, 5, 3), response.reply());
            assertEquals(3, response.jobs().size());
            assertEquals(List.of(ChatbotLabels.LINK_ALL_JOBS), response.links());
        }

        @Test
        @DisplayName("Tìm việc có từ khóa nhưng không tin nào khớp -> SEARCH_NONE, không có thẻ")
        void reply_shouldSayNoMatch_whenSearchFindsNothing() {
            // Arrange
            when(jobs.search(anyString())).thenReturn(new ChatbotJobService.SearchResult(0, List.of()));
            when(jobs.hasSearchTerms(anyString())).thenReturn(true);

            // Act
            ChatResponse response = guestAsks("tìm việc golang");

            // Assert
            assertEquals(ChatbotLabels.SEARCH_NONE, response.reply());
            assertTrue(response.jobs().isEmpty());
        }

        @Test
        @DisplayName("Hỏi liên hệ HR mà chưa cấu hình -> CONTACT_MISSING, không có khối liên hệ")
        void reply_shouldSayContactMissing_whenHrContactIsNotConfigured() {
            // Arrange
            when(config.loadHrContact()).thenReturn(null);

            // Act
            ChatResponse response = guestAsks("Liên hệ HR");

            // Assert
            assertEquals(ChatbotLabels.CONTACT_MISSING, response.reply());
            assertNull(response.contact());
        }

        @Test
        @DisplayName("Câu không hiểu và không khớp tin nào -> UNKNOWN + thông tin liên hệ HR")
        void reply_shouldOfferHrContact_whenQuestionIsUnknown() {
            // Arrange
            when(jobs.hasSearchTerms(anyString())).thenReturn(true);
            when(jobs.search(anyString())).thenReturn(new ChatbotJobService.SearchResult(0, List.of()));

            // Act
            ChatResponse response = guestAsks("Công ty có hỗ trợ chỗ ở không?");

            // Assert
            assertEquals(ChatbotIntent.UNKNOWN.name(), response.intent());
            assertEquals(ChatbotLabels.UNKNOWN, response.reply());
            assertEquals(HR_CONTACT, response.contact());
        }

        @Test
        @DisplayName("Câu không có từ khóa nhưng khớp tên tin (\"java\") -> Coi như tìm việc")
        void reply_shouldSearchJobs_whenUnknownMessageMatchesJobTitle() {
            // Arrange
            when(jobs.hasSearchTerms("java")).thenReturn(true);
            when(jobs.search("java")).thenReturn(new ChatbotJobService.SearchResult(1, List.of(card(1))));

            // Act
            ChatResponse response = guestAsks("java");

            // Assert
            assertEquals(ChatbotIntent.SEARCH_JOB.name(), response.intent());
            assertEquals(1, response.jobs().size());
        }
    }
}
