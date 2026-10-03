package com.group2.rms.interview.entity;

import com.group2.rms.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

/**
 * 3.15 InterviewPanel - Bảng trung gian N-N lưu danh sách Hội đồng phỏng vấn.
 * Sử dụng Composite Primary Key: InterviewPanelId (InterviewId + InterviewerId).
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

    @Enumerated(EnumType.STRING)
    @Column(name = "RoleInPanel", nullable = false, length = 30)
    private RoleInPanel roleInPanel;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterviewPanel that = (InterviewPanel) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "InterviewPanel{" +
               "id=" + id +
               ", roleInPanel=" + roleInPanel +
               '}';
    }
}
