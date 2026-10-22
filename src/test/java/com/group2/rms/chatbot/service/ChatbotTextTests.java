package com.group2.rms.chatbot.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * UNIT TEST: ChatbotText (hàm thuần bỏ dấu, tách từ, khớp theo nguyên từ). Không Spring, không DB.
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện.
 */
class ChatbotTextTests {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
            "'Hồ sơ của TÔI', 'ho so cua toi'",
            "'Đà Nẵng', 'da nang'",
            "'  Java,   Hà Nội?! ', 'java ha noi'",
            "'C++ / C#', 'c++ c#'"
    })
    @DisplayName("Chữ có dấu, hoa thường, dấu câu -> Chữ thường không dấu, một khoảng trắng")
    void normalize_shouldRemoveAccentsAndPunctuation_whenTextIsVietnamese(String input, String expected) {
        // Act + Assert
        assertEquals(expected, ChatbotText.normalize(input));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullAndEmptySource
    @DisplayName("null / chuỗi rỗng -> Chuỗi rỗng")
    void normalize_shouldReturnEmpty_whenTextIsNullOrEmpty(String input) {
        // Act + Assert
        assertEquals("", ChatbotText.normalize(input));
    }

    @Test
    @DisplayName("Viết tắt địa điểm (hcm, hn) -> Đổi thành tên đầy đủ để khớp WorkLocation")
    void words_shouldExpandLocationAliases_whenShortNameIsUsed() {
        // Act
        List<String> words = ChatbotText.words("java hcm hn");

        // Assert
        assertEquals(List.of("java", "ho", "chi", "minh", "ha", "noi"), words);
    }

    @Test
    @DisplayName("Khớp theo nguyên từ -> \"hr\" khớp \"lien he hr\" nhưng không khớp \"chrome\"")
    void containsPhrase_shouldMatchWholeWordsOnly_whenPhraseIsPartOfAnotherWord() {
        // Act + Assert
        assertTrue(ChatbotText.containsPhrase("lien he hr", "hr"));
        assertTrue(ChatbotText.containsPhrase("cach nop ho so nhanh", "nop ho so"));
        assertFalse(ChatbotText.containsPhrase("cai chrome", "hr"));
        assertFalse(ChatbotText.containsPhrase("bat ky", ""));
    }
}
