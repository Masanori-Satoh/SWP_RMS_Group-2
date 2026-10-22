package com.group2.rms.chatbot.validator;

import org.springframework.stereotype.Component;

/*
 * Giới hạn và làm sạch tin nhắn gửi cho chatbot.
 * MESSAGE_CHAR_LIMIT dùng chung cho @Size của ChatRequest và test.
 */
@Component
public class ChatbotValidator {

    public static final int MESSAGE_CHAR_LIMIT = 500;
    public static final String MESSAGE_TOO_LONG = "Tin nhắn tối đa " + MESSAGE_CHAR_LIMIT + " ký tự.";

    /** Cắt khoảng trắng hai đầu, gộp khoảng trắng liên tiếp; chuỗi rỗng / chỉ khoảng trắng → null. */
    public String cleanMessage(String message) {
        if (message == null) {
            return null;
        }
        String cleaned = message.trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned;
    }
}
