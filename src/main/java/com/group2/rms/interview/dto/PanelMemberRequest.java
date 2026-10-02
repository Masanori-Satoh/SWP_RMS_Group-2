package com.group2.rms.interview.dto;

import com.group2.rms.interview.RoleInPanel;
import jakarta.validation.constraints.NotNull;

/**
 * DTO đại diện cho một thành viên được gán vào Hội đồng phỏng vấn.
 * RoleInPanel có thể để trống (Service sẽ mặc định là HM - Hiring Manager / Technical Interviewer).
 */
public record PanelMemberRequest(
        @NotNull(message = "Interviewer ID không được để trống.")
        Integer interviewerId,

        RoleInPanel roleInPanel
) {}
