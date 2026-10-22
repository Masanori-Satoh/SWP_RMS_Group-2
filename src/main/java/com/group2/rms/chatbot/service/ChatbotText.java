package com.group2.rms.chatbot.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 * Hàm thuần xử lý chữ cho chatbot: bỏ dấu tiếng Việt, tách từ, so khớp theo nguyên từ.
 * Bỏ dấu để ứng viên gõ "ho so cua toi" hay "hồ sơ của tôi" đều hiểu như nhau.
 */
public final class ChatbotText {

    /** Viết tắt địa điểm hay gặp → tên đầy đủ (đã bỏ dấu) để khớp với cột WorkLocation. */
    private static final Map<String, String> LOCATION_ALIASES = Map.of(
            "hcm", "ho chi minh",
            "tphcm", "ho chi minh",
            "sg", "ho chi minh",
            "saigon", "ho chi minh",
            "hn", "ha noi",
            "dn", "da nang");

    /** Lớp chỉ chứa hàm xử lý chữ, không tạo đối tượng. */
    private ChatbotText() {
    }

    /** Chữ thường, bỏ dấu, đ → d, ký tự không phải chữ / số thành khoảng trắng, gộp khoảng trắng. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT).replace('đ', 'd');
        String noAccent = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccent.replaceAll("[^a-z0-9+#]+", " ").trim();
    }

    /** Các từ của chuỗi đã normalize, viết tắt địa điểm được thay bằng tên đầy đủ. */
    public static List<String> words(String normalizedText) {
        List<String> words = new ArrayList<>();
        if (normalizedText == null || normalizedText.isBlank()) {
            return words;
        }
        for (String word : normalizedText.split(" ")) {
            String expanded = LOCATION_ALIASES.getOrDefault(word, word);
            for (String part : expanded.split(" ")) {
                words.add(part);
            }
        }
        return words;
    }

    /**
     * Chuỗi đã normalize có chứa cụm {@code phrase} (cũng đã normalize) theo nguyên từ không.
     * Ví dụ "hr" khớp "lien he hr" nhưng không khớp "chrome".
     */
    public static boolean containsPhrase(String normalizedText, String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return false;
        }
        return (" " + normalizedText + " ").contains(" " + phrase + " ");
    }
}
