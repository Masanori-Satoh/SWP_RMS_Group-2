package com.group2.rms.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.rms.admin.entity.SystemConfig;
import com.group2.rms.admin.repository.SystemConfigRepository;
import com.group2.rms.chatbot.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/*
 * Cấu hình chatbot trong bảng SystemConfig: bật/tắt, danh sách FAQ (JSON), thông tin liên hệ HR.
 * HR đổi nội dung trong DB là có hiệu lực ngay, không phải sửa code. Cấu hình lỗi thì bỏ qua, không làm hỏng chatbot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotConfigService {

    public static final String KEY_ENABLED = "CHATBOT_ENABLED";
    public static final String KEY_FAQ = "CHATBOT_FAQ";
    public static final String KEY_HR_EMAIL = "HR_CONTACT_EMAIL";
    public static final String KEY_HR_PHONE = "HR_CONTACT_PHONE";
    public static final String KEY_HR_HOURS = "HR_CONTACT_HOURS";
    /** Giá trị duy nhất của CHATBOT_ENABLED làm tắt chatbot. */
    public static final String VALUE_DISABLED = "false";

    private final SystemConfigRepository configs;
    private final ObjectMapper objectMapper;

    /** Một dòng của JSON CHATBOT_FAQ, đúng như HR nhập (từ khóa còn dấu). */
    record FaqRow(List<String> keywords, String question, String answer) {
    }

    /** Chatbot đang bật. Chưa cấu hình thì coi như bật; chỉ tắt khi giá trị là "false". */
    @Transactional(readOnly = true)
    public boolean isEnabled() {
        String value = valueOf(KEY_ENABLED);
        return value == null || !VALUE_DISABLED.equalsIgnoreCase(value);
    }

    /** FAQ có đủ câu hỏi, câu trả lời và ít nhất một từ khóa; từ khóa đã normalize. JSON lỗi → list rỗng. */
    @Transactional(readOnly = true)
    public List<ChatbotFaq> loadFaqs() {
        String json = valueOf(KEY_FAQ);
        List<ChatbotFaq> faqs = new ArrayList<>();
        if (json == null) {
            return faqs;
        }
        FaqRow[] rows;
        try {
            rows = objectMapper.readValue(json, FaqRow[].class);
        } catch (JsonProcessingException e) {
            log.warn("SystemConfig {} không phải JSON hợp lệ, chatbot bỏ qua FAQ", KEY_FAQ);
            return faqs;
        }
        for (FaqRow row : rows) {
            ChatbotFaq faq = toFaq(row);
            if (faq != null) {
                faqs.add(faq);
            }
        }
        return faqs;
    }

    /** Liên hệ HR; chưa cấu hình mục nào → null. */
    @Transactional(readOnly = true)
    public ChatResponse.Contact loadHrContact() {
        String email = valueOf(KEY_HR_EMAIL);
        String phone = valueOf(KEY_HR_PHONE);
        String hours = valueOf(KEY_HR_HOURS);
        if (email == null && phone == null) {
            return null;
        }
        return new ChatResponse.Contact(email, phone, hours);
    }

    /** Kiểm tra một dòng FAQ từ JSON và chuẩn hóa từ khóa để so khớp câu hỏi. */
    private static ChatbotFaq toFaq(FaqRow row) {
        if (row == null || isBlank(row.question()) || isBlank(row.answer()) || row.keywords() == null) {
            return null;
        }
        List<String> keywords = new ArrayList<>();
        for (String keyword : row.keywords()) {
            String normalized = ChatbotText.normalize(keyword);
            if (!normalized.isEmpty()) {
                keywords.add(normalized);
            }
        }
        if (keywords.isEmpty()) {
            return null;
        }
        return new ChatbotFaq(keywords, row.question().trim(), row.answer().trim());
    }

    /** Đọc giá trị SystemConfig theo key; trả null nếu thiếu hoặc chỉ có khoảng trắng. */
    private String valueOf(String key) {
        String value = configs.findById(key).map(SystemConfig::getConfigValue).orElse(null);
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    /** Dùng chung điều kiện kiểm tra chuỗi null hoặc rỗng cho cấu hình. */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
