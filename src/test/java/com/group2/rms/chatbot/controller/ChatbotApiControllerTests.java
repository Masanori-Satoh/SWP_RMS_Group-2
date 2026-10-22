package com.group2.rms.chatbot.controller;

import com.group2.rms.chatbot.dto.ChatRequest;
import com.group2.rms.chatbot.dto.ChatResponse;
import com.group2.rms.chatbot.service.ChatbotService;
import com.group2.rms.chatbot.validator.ChatbotValidator;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * CONTROLLER TEST (@WebMvcTest): ChatbotApiController với cấu hình bảo mật THẬT (SecurityConfig).
 * ChatbotService là mock. Kiểm tra: khách gọi được (permitAll), cờ đăng nhập truyền đúng cho service,
 * tin nhắn quá dài → 400 và không gọi service, route khác của /api/chatbot vẫn bị chặn.
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện.
 */
@WebMvcTest(controllers = ChatbotApiController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class ChatbotApiControllerTests {

    private static final String URL = "/api/chatbot/message";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChatbotService chatbotService;
    // Bộ lọc phiên (AccountSessionGuardFilter) đọc tài khoản ở mỗi request
    @MockitoBean
    private UserRepository userRepository;

    private static ChatResponse hello() {
        return ChatResponse.builder().intent("WELCOME").reply("Xin chào").build();
    }

    private void account(String username, String roleName) {
        User user = User.builder().userId(1).username(username).fullName("Tên " + username).accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
        when(userRepository.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("Khách (chưa đăng nhập) gửi câu hỏi -> 200, service nhận signedIn = false")
    void message_shouldAnswerGuest_whenNotSignedIn() throws Exception {
        // Arrange
        when(chatbotService.reply(any(ChatRequest.class), eq(false))).thenReturn(hello());

        // Act + Assert
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Xin chào\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reply").value("Xin chào"))
                .andExpect(jsonPath("$.data.jobs").isArray());
        verify(chatbotService).reply(new ChatRequest("Xin chào", null), false);
    }

    @Test
    @WithMockUser(username = "candidate_user", roles = "CANDIDATE")
    @DisplayName("Ứng viên đã đăng nhập -> service nhận signedIn = true")
    void message_shouldPassSignedIn_whenCandidateIsLoggedIn() throws Exception {
        // Arrange
        account("candidate_user", "Candidate");
        when(chatbotService.reply(any(ChatRequest.class), eq(true))).thenReturn(hello());

        // Act + Assert
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Hồ sơ của tôi\"}"))
                .andExpect(status().isOk());
        verify(chatbotService).reply(new ChatRequest("Hồ sơ của tôi", null), true);
    }

    @Test
    @DisplayName("Tin nhắn dài MESSAGE_CHAR_LIMIT + 1 ký tự -> 400 + câu lỗi của validator, không gọi service")
    void message_shouldReject_whenMessageIsTooLong() throws Exception {
        // Arrange
        String tooLong = "a".repeat(ChatbotValidator.MESSAGE_CHAR_LIMIT + 1);

        // Act + Assert
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(ChatbotValidator.MESSAGE_TOO_LONG));
        verifyNoInteractions(chatbotService);
    }

    @Test
    @DisplayName("Tin nhắn đúng MESSAGE_CHAR_LIMIT ký tự -> 200 (biên trên hợp lệ)")
    void message_shouldAccept_whenMessageIsExactlyAtLimit() throws Exception {
        // Arrange
        String atLimit = "a".repeat(ChatbotValidator.MESSAGE_CHAR_LIMIT);
        when(chatbotService.reply(any(ChatRequest.class), anyBoolean())).thenReturn(hello());

        // Act + Assert
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"" + atLimit + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("jobPostingId âm (id giả) -> 400, không gọi service")
    void message_shouldReject_whenJobPostingIdIsNotPositive() throws Exception {
        // Act + Assert
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"jobPostingId\":-1}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(chatbotService);
    }

    @Test
    @DisplayName("Khách gọi GET vào /api/chatbot/message -> Không mở (chỉ POST được permitAll)")
    void message_shouldNotBeOpenForGet_whenGuest() throws Exception {
        // Act + Assert
        mvc.perform(get(URL)).andExpect(status().is3xxRedirection());
        verifyNoInteractions(chatbotService);
    }
}
