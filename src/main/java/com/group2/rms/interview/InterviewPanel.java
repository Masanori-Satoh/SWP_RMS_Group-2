package com.group2.rms.interview;

import com.group2.rms.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 3.16 InterviewPanel - Danh sách người tham gia phỏng vấn.
 * Bảng này có Composite Primary Key: (InterviewId, InterviewerId).
 * RoleInPanel: HR, HM
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
    @JoinColumn(name = "InterviewId", nullable = false, referencedColumnName = "InterviewId")
    private InterviewSchedule interviewSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("interviewerId")
    @JoinColumn(name = "InterviewerId", nullable = false, referencedColumnName = "UserId")
    private User interviewer;

    /**
     * HR, HM
     */
    @Column(name = "RoleInPanel", nullable = false, length = 30)
    private String roleInPanel;
}
