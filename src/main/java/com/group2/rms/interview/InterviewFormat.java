package com.group2.rms.interview;

import lombok.Getter;

/**
 * Hình thức tổ chức phỏng vấn.
 * Mapping tương ứng với CHECK constraint trong MS SQL Server:
 * CK_InterviewSchedule_Format: CHECK (InterviewFormat IN (N'Online_GoogleMeet', N'Offline_Office'))
 */
@Getter
public enum InterviewFormat {
    Online_GoogleMeet("Online (Google Meet)"),
    Offline_Office("Offline (Văn phòng)");

    private final String displayName;

    InterviewFormat(String displayName) {
        this.displayName = displayName;
    }
}
