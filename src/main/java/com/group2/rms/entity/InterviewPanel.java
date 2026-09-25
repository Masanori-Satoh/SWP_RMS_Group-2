package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 3.16 InterviewPanel - Danh sách người tham gia phỏng vấn.
 * Bảng này có Composite Primary Key: (InterviewId, InterviewerId).
 * RoleInPanel: Lead_Interviewer, Member
 */
@Entity
@Table(name = "InterviewPanel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewPanel {

    @EmbeddedId
    private InterviewPanelId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("interviewId")
    @JoinColumn(name = "InterviewId", referencedColumnName = "InterviewId")
    private InterviewSchedule interviewSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("interviewerId")
    @JoinColumn(name = "InterviewerId", referencedColumnName = "UserId")
    private User interviewer;

    /**
     * Lead_Interviewer, Member
     */
    @Column(name = "RoleInPanel", length = 30)
    private String roleInPanel;
}
