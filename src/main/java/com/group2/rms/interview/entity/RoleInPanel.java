package com.group2.rms.interview.entity;

import lombok.Getter;

/**
 * Vai trò của thành viên trong Hội đồng phỏng vấn (InterviewPanel).
 * Mapping tương ứng với CHECK constraint trong MS SQL Server:
 * CK_InterviewPanel_Role: CHECK (RoleInPanel IN (N'HR', N'HM', N'Interviewer', N'Director'))
 *
 * HR: Đại diện bộ phận nhân sự (kiểm tra độ phù hợp văn hóa, giao tiếp, chế độ đãi ngộ cơ bản).
 * HM: Hiring Manager - Trưởng bộ phận chuyên môn (đánh giá năng lực chuyên môn, giải quyết case study).
 * Interviewer/Director: vai trò tham gia hội đồng theo tài khoản; không tự quyết định quyền chốt kết quả.
 */
@Getter
public enum RoleInPanel {
    HR("HR Interviewer"),
    HM("Hiring Manager"),
    Interviewer("Interviewer"),
    Director("Director");

    private final String displayName;

    RoleInPanel(String displayName) {
        this.displayName = displayName;
    }
}
