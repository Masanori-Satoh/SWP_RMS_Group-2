package com.group2.rms.interview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key cho Entity InterviewPanel.
 * Ánh xạ tới khóa chính tổ hợp (InterviewId, InterviewerId) trong bảng InterviewPanel (MS SQL Server).
 *
 * Bắt buộc tuân thủ JPA Specification cho Composite ID:
 * 1. Phải đánh dấu @Embeddable.
 * 2. Phải implement java.io.Serializable.
 * 3. Phải cài đặt equals() và hashCode() dựa trên giá trị các thuộc tính thành phần để đảm bảo
 *    hoạt động chính xác trong Hibernate Session / First-Level Cache và các cấu trúc Set (HashSet).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewPanelId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "InterviewId", nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    private Long interviewId;

    @Column(name = "InterviewerId", nullable = false)
    private Integer interviewerId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterviewPanelId that = (InterviewPanelId) o;
        return Objects.equals(interviewId, that.interviewId) &&
               Objects.equals(interviewerId, that.interviewerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interviewId, interviewerId);
    }

    @Override
    public String toString() {
        return "InterviewPanelId{" +
               "interviewId=" + interviewId +
               ", interviewerId=" + interviewerId +
               '}';
    }
}
