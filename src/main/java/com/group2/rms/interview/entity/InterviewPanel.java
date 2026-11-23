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

    /* Hai quan hệ @MapsId cùng tạo khóa (InterviewId, InterviewerId), mỗi người chỉ có một chỗ trong hội đồng. */
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
        /* So sánh bằng khóa ghép để Set nhận diện đúng thành viên mà không duyệt các quan hệ LAZY. */
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
        /* Chỉ in khóa và vai trò để log không tải quan hệ LAZY hoặc lặp giữa lịch và hội đồng. */
        return "InterviewPanel{" +
               "id=" + id +
               ", roleInPanel=" + roleInPanel +
               '}';
    }
}
