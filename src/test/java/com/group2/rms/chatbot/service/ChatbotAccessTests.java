package com.group2.rms.chatbot.service;

import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/*
 * UNIT TEST: ChatbotAccess (ai được xem hồ sơ / nhận gợi ý qua chatbot). Hàm thuần, không DB.
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): bảng quyền dùng @CsvSource, một dòng một trường hợp.
 */
class ChatbotAccessTests {

    private final ChatbotAccess access = new ChatbotAccess();

    @ParameterizedTest(name = "role {0} -> được dùng = {1}")
    @CsvSource({
            "Candidate, true",
            "HR, false",
            "Hiring Manager, false",
            "Director, false",
            "Interviewer, false",
            "System Admin, false"
    })
    @DisplayName("Tính năng cá nhân (hồ sơ, gợi ý) -> Chỉ tài khoản ứng viên")
    void canUsePersonalFeatures_shouldFollowRoleTable(String roleName, boolean expected) {
        // Arrange
        User user = User.builder().username("u").role(Role.builder().roleName(roleName).build()).build();

        // Act + Assert
        assertEquals(expected, access.canUsePersonalFeatures(user));
    }

    @Test
    @DisplayName("Khách (chưa đăng nhập, user = null) -> Không được dùng")
    void canUsePersonalFeatures_shouldBeFalse_whenGuest() {
        // Act + Assert
        assertFalse(access.canUsePersonalFeatures(null));
    }
}
