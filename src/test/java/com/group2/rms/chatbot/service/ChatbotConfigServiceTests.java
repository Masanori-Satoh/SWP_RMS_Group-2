package com.group2.rms.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.rms.admin.entity.SystemConfig;
import com.group2.rms.admin.repository.SystemConfigRepository;
import com.group2.rms.chatbot.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST (Mockito): ChatbotConfigService đọc SystemConfig (bật/tắt, FAQ JSON, liên hệ HR).
 * SystemConfigRepository là mock; ObjectMapper là bản thật để kiểm tra đọc JSON như khi chạy app.
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện, Arrange / Act / Assert.
 */
@ExtendWith(MockitoExtension.class)
class ChatbotConfigServiceTests {

    @Mock
    private SystemConfigRepository configs;

    private ChatbotConfigService service;

    @BeforeEach
    void setUp() {
        service = new ChatbotConfigService(configs, new ObjectMapper());
        lenient().when(configs.findById(anyString())).thenReturn(Optional.empty());
    }

    private void givenConfig(String key, String value) {
        when(configs.findById(key)).thenReturn(Optional.of(SystemConfig.builder().configKey(key).configValue(value).build()));
    }

    @ParameterizedTest(name = "CHATBOT_ENABLED = \"{0}\" -> bật = {1}")
    @CsvSource({"true, true", "false, false", "FALSE, false", "' false ', false", "yes, true"})
    @DisplayName("Giá trị CHATBOT_ENABLED -> Chỉ \"false\" (không phân biệt hoa thường) mới tắt chatbot")
    void isEnabled_shouldBeFalseOnlyForFalse_whenConfigured(String value, boolean expected) {
        // Arrange
        givenConfig(ChatbotConfigService.KEY_ENABLED, value);

        // Act + Assert
        assertEquals(expected, service.isEnabled());
    }

    @Test
    @DisplayName("Chưa cấu hình CHATBOT_ENABLED -> Coi như bật")
    void isEnabled_shouldBeTrue_whenKeyIsMissing() {
        // Act + Assert
        assertTrue(service.isEnabled());
    }

    @Test
    @DisplayName("FAQ JSON hợp lệ -> Đọc đủ, từ khóa được bỏ dấu, câu thiếu trả lời / không có từ khóa bị bỏ")
    void loadFaqs_shouldNormalizeKeywordsAndSkipIncompleteRows_whenJsonIsValid() {
        // Arrange
        givenConfig(ChatbotConfigService.KEY_FAQ, """
                [
                  {"keywords": ["Nộp Hồ Sơ", "  "], "question": " Cách nộp hồ sơ ", "answer": "Bấm Ứng tuyển ngay"},
                  {"keywords": ["cv"], "question": "Thiếu trả lời", "answer": "  "},
                  {"keywords": [], "question": "Không từ khóa", "answer": "A"}
                ]
                """);

        // Act
        List<ChatbotFaq> faqs = service.loadFaqs();

        // Assert
        assertEquals(1, faqs.size());
        assertEquals(List.of("nop ho so"), faqs.get(0).keywords());
        assertEquals("Cách nộp hồ sơ", faqs.get(0).question());
    }

    @Test
    @DisplayName("FAQ không phải JSON -> List rỗng, không ném lỗi (chatbot vẫn chạy)")
    void loadFaqs_shouldReturnEmpty_whenJsonIsInvalid() {
        // Arrange
        givenConfig(ChatbotConfigService.KEY_FAQ, "không phải json");

        // Act + Assert
        assertTrue(service.loadFaqs().isEmpty());
    }

    @Test
    @DisplayName("Chưa cấu hình FAQ -> List rỗng")
    void loadFaqs_shouldReturnEmpty_whenKeyIsMissing() {
        // Act + Assert
        assertTrue(service.loadFaqs().isEmpty());
    }

    @Test
    @DisplayName("Có email HR, không có SĐT -> Contact có email, SĐT null")
    void loadHrContact_shouldKeepConfiguredFields_whenOnlyEmailIsSet() {
        // Arrange
        givenConfig(ChatbotConfigService.KEY_HR_EMAIL, " hr@moc.com ");

        // Act
        ChatResponse.Contact contact = service.loadHrContact();

        // Assert
        assertEquals("hr@moc.com", contact.email());
        assertNull(contact.phone());
        assertFalse(contact.email().isBlank());
    }

    @Test
    @DisplayName("Không có email lẫn SĐT HR -> null (không hiện khối liên hệ)")
    void loadHrContact_shouldReturnNull_whenNothingIsConfigured() {
        // Act + Assert
        assertNull(service.loadHrContact());
    }
}
