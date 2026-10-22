package com.group2.rms.chatbot.service;

import java.util.List;

/**
 * Một câu hỏi thường gặp, đọc từ khóa CHATBOT_FAQ của SystemConfig.
 *
 * @param keywords các cụm từ khóa ĐÃ normalize (không dấu, chữ thường); khớp một cụm là khớp câu hỏi
 * @param question câu hỏi hiển thị trên nút gợi ý
 * @param answer   câu trả lời
 */
public record ChatbotFaq(List<String> keywords, String question, String answer) {
}
