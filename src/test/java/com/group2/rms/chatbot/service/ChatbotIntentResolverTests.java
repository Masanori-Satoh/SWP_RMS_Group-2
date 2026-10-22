package com.group2.rms.chatbot.service;

import com.group2.rms.chatbot.service.ChatbotIntentResolver.Resolution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/*
 * UNIT TEST: ChatbotIntentResolver (đoán ý định bằng từ khóa). Bản thật, không mock, không DB.
 * FAQ mẫu dựng bằng tay, từ khóa đã normalize như ChatbotConfigService trả về.
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện, Arrange / Act / Assert.
 */
class ChatbotIntentResolverTests {

    private static final ChatbotFaq HOW_TO_APPLY =
            new ChatbotFaq(List.of("nop ho so", "cach nop"), "Cách nộp hồ sơ", "Trả lời cách nộp");
    private static final ChatbotFaq PROCESS =
            new ChatbotFaq(List.of("quy trinh", "cac buoc"), "Quy trình tuyển dụng", "Trả lời quy trình");
    private static final List<ChatbotFaq> FAQS = List.of(HOW_TO_APPLY, PROCESS);

    private final ChatbotIntentResolver resolver = new ChatbotIntentResolver();

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({
            "'Hồ sơ của tôi tới bước nào rồi?', CHECK_STATUS",
            "'ho so cua toi', CHECK_STATUS",
            "'Xem trạng thái đơn', CHECK_STATUS",
            "'Có việc nào hợp với tôi không?', RECOMMEND",
            "'Gợi ý việc làm', RECOMMEND",
            "'Liên hệ HR', CONTACT_HR",
            "'Cho mình số điện thoại nhân sự', CONTACT_HR",
            "'Có việc Java nào ở Hà Nội không?', SEARCH_JOB",
            "'tim job', SEARCH_JOB",
            "'Công ty có hỗ trợ chỗ ở không?', UNKNOWN",
            "'java', UNKNOWN"
    })
    @DisplayName("Câu hỏi thường gặp của từng ý định -> Đoán đúng ý định")
    void resolve_shouldDetectIntent_whenMessageHasKeyword(String message, ChatbotIntent expected) {
        // Act
        Resolution resolution = resolver.resolve(message, FAQS);

        // Assert
        assertEquals(expected, resolution.intent());
        assertNull(resolution.faq());
    }

    @Test
    @DisplayName("\"Quy trình tuyển dụng\" có chữ \"tuyển\" -> Vẫn là FAQ (FAQ ưu tiên hơn tìm việc)")
    void resolve_shouldPreferFaq_whenMessageAlsoLooksLikeJobSearch() {
        // Act
        Resolution resolution = resolver.resolve("Quy trình tuyển dụng thế nào?", FAQS);

        // Assert
        assertEquals(ChatbotIntent.FAQ, resolution.intent());
        assertEquals(PROCESS, resolution.faq());
    }

    @Test
    @DisplayName("Câu khớp nhiều từ khóa của một FAQ hơn -> Chọn FAQ khớp nhiều nhất")
    void resolve_shouldPickFaqWithMostHits_whenSeveralFaqsMatch() {
        // Arrange
        ChatbotFaq oneHit = new ChatbotFaq(List.of("cach nop"), "Một từ", "A");
        ChatbotFaq twoHits = new ChatbotFaq(List.of("cach nop", "nop ho so"), "Hai từ", "B");

        // Act
        Resolution resolution = resolver.resolve("Cách nộp hồ sơ", List.of(oneHit, twoHits));

        // Assert
        assertEquals(twoHits, resolution.faq());
    }

    @Test
    @DisplayName("\"Hồ sơ của tôi\" khớp cả FAQ \"nộp hồ sơ\"? -> Trạng thái hồ sơ được ưu tiên trước FAQ")
    void resolve_shouldPreferStatus_whenMessageAsksAboutOwnApplication() {
        // Act
        Resolution resolution = resolver.resolve("Hồ sơ của tôi đã nộp hồ sơ chưa", FAQS);

        // Assert
        assertEquals(ChatbotIntent.CHECK_STATUS, resolution.intent());
    }

    @Test
    @DisplayName("Không có FAQ nào và câu không có từ khóa -> UNKNOWN")
    void resolve_shouldReturnUnknown_whenNoFaqAndNoKeyword() {
        // Act
        Resolution resolution = resolver.resolve("xin chào", List.of());

        // Assert
        assertEquals(ChatbotIntent.UNKNOWN, resolution.intent());
    }
}
