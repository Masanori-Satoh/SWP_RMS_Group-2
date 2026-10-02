package com.group2.rms.interview.dto;

import com.group2.rms.interview.InterviewPanel;
import com.group2.rms.interview.RoleInPanel;
import com.group2.rms.user.entity.User;

/**
 * DTO đại diện cho thông tin thành viên Hội đồng phỏng vấn trong kết quả trả về.
 */
public record PanelMemberResponse(
        Integer interviewerId,
        String username,
        String fullName,
        String email,
        String phoneNumber,
        RoleInPanel roleInPanel,
        String roleInPanelDisplay
) {
    public static PanelMemberResponse fromEntity(InterviewPanel panel) {
        if (panel == null || panel.getInterviewer() == null) {
            return null;
        }
        User interviewer = panel.getInterviewer();
        RoleInPanel role = panel.getRoleInPanel();
        return new PanelMemberResponse(
                interviewer.getUserId(),
                interviewer.getUsername(),
                interviewer.getFullName(),
                interviewer.getEmail(),
                interviewer.getPhoneNumber(),
                role,
                role != null ? role.getDisplayName() : null
        );
    }
}
