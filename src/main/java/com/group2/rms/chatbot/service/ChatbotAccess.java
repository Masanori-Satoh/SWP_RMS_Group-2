package com.group2.rms.chatbot.service;

import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.user.entity.User;
import org.springframework.stereotype.Component;

/*
 * Luật "ai được hỏi gì" của chatbot. Hàm thuần, chỉ đọc role, test không cần DB.
 * Khách (user = null) chỉ được hỏi phần công khai: FAQ, tìm việc, chi tiết tin, liên hệ HR.
 */
@Component
public class ChatbotAccess {

    public static final String ROLE_CANDIDATE = "Candidate";

    /** Xem hồ sơ của mình và nhận gợi ý việc: chỉ tài khoản ứng viên đã đăng nhập. */
    public boolean canUsePersonalFeatures(User user) {
        return user != null && CurrentUserService.hasRole(user, ROLE_CANDIDATE);
    }
}
