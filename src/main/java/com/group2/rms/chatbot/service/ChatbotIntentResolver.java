package com.group2.rms.chatbot.service;

import org.springframework.stereotype.Component;

import java.util.List;

/*
 * Đoán ý định từ câu hỏi bằng từ khóa (GĐ 1, chưa dùng LLM). Hàm thuần, không truy cập DB.
 * Thứ tự ưu tiên: hồ sơ của tôi → gợi ý việc → liên hệ HR → FAQ → tìm việc → không hiểu.
 * FAQ đứng trước tìm việc vì "quy trình tuyển dụng" có chữ "tuyển" nhưng là câu hỏi quy trình.
 */
@Component
public class ChatbotIntentResolver {

    // Từ khóa đã bỏ dấu (xem ChatbotText.normalize)
    static final List<String> STATUS_PHRASES = List.of(
            "ho so cua toi", "ho so cua minh", "don cua toi", "don cua minh", "trang thai", "toi buoc nao",
            "ket qua ung tuyen", "ho so toi");
    static final List<String> RECOMMEND_PHRASES = List.of(
            "goi y", "phu hop voi toi", "hop voi toi", "hop voi minh", "danh cho toi");
    static final List<String> CONTACT_PHRASES = List.of(
            "lien he", "hr", "nhan su", "hotline", "so dien thoai", "email");
    static final List<String> SEARCH_PHRASES = List.of(
            "tim", "viec", "vi tri", "job", "jobs", "tuyen", "cong viec");

    /** Kết quả đoán: ý định và câu FAQ khớp nhất (chỉ có khi ý định là FAQ). */
    public record Resolution(ChatbotIntent intent, ChatbotFaq faq) {
    }

    /**
     * Phân loại câu hỏi theo thứ tự ưu tiên của chatbot và chọn FAQ phù hợp nếu có.
     *
     * @param message câu hỏi đã làm sạch (không null)
     * @param faqs    danh sách FAQ hiện có, từ khóa đã normalize
     */
    public Resolution resolve(String message, List<ChatbotFaq> faqs) {
        String text = ChatbotText.normalize(message);
        if (containsAny(text, STATUS_PHRASES)) {
            return new Resolution(ChatbotIntent.CHECK_STATUS, null);
        }
        if (containsAny(text, RECOMMEND_PHRASES)) {
            return new Resolution(ChatbotIntent.RECOMMEND, null);
        }
        if (containsAny(text, CONTACT_PHRASES)) {
            return new Resolution(ChatbotIntent.CONTACT_HR, null);
        }
        ChatbotFaq faq = bestFaq(text, faqs);
        if (faq != null) {
            return new Resolution(ChatbotIntent.FAQ, faq);
        }
        if (containsAny(text, SEARCH_PHRASES)) {
            return new Resolution(ChatbotIntent.SEARCH_JOB, null);
        }
        return new Resolution(ChatbotIntent.UNKNOWN, null);
    }

    /** FAQ khớp nhiều cụm từ khóa nhất; bằng nhau thì lấy câu đứng trước; không khớp câu nào → null. */
    private static ChatbotFaq bestFaq(String text, List<ChatbotFaq> faqs) {
        ChatbotFaq best = null;
        int bestHits = 0;
        for (ChatbotFaq faq : faqs) {
            int hits = 0;
            for (String keyword : faq.keywords()) {
                if (ChatbotText.containsPhrase(text, keyword)) {
                    hits++;
                }
            }
            if (hits > bestHits) {
                best = faq;
                bestHits = hits;
            }
        }
        return best;
    }

    /** Kiểm tra câu đã chuẩn hóa có chứa ít nhất một cụm từ của nhóm ý định. */
    private static boolean containsAny(String text, List<String> phrases) {
        for (String phrase : phrases) {
            if (ChatbotText.containsPhrase(text, phrase)) {
                return true;
            }
        }
        return false;
    }
}
