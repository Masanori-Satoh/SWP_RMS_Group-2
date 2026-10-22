package com.group2.rms.chatbot.dto;

import com.group2.rms.chatbot.validator.ChatbotValidator;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Một lượt hỏi gửi lên chatbot (JSON). Không có trường người dùng: ai đang hỏi luôn lấy từ phiên đăng nhập.
 *
 * @param message      câu hỏi; trống thì chatbot trả lời chào
 * @param jobPostingId có giá trị khi ứng viên bấm "Xem chi tiết" trên một thẻ tin, khi đó bỏ qua {@code message}
 */
public record ChatRequest(
        @Size(max = ChatbotValidator.MESSAGE_CHAR_LIMIT, message = ChatbotValidator.MESSAGE_TOO_LONG)
        String message,
        @Positive
        Integer jobPostingId) {
}
