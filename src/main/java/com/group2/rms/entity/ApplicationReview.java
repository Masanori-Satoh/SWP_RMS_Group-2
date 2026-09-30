package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 3.12 ApplicationReview - Đánh giá hồ sơ bởi HR hoặc Hiring Manager.
 */
@Entity
@Table(name = "ApplicationReview")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReviewId")
    private Integer reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApplicationId", nullable = false, referencedColumnName = "ApplicationId")
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReviewerId", nullable = false, referencedColumnName = "UserId")
    private User reviewer;

    /** HR or HiringManager. */
    @Column(name = "ReviewerRole", nullable = false, length = 30)
    private String reviewerRole;

    /** Pass, Fail or Hold. */
    @Column(name = "Decision", nullable = false, length = 20)
    private String decision;

    @Column(name = "Comments", length = 1000)
    private String comments;

    @Column(name = "ReviewedAt", nullable = false)
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onPersist() {
        if (this.reviewedAt == null) {
            this.reviewedAt = LocalDateTime.now();
        }
    }
}
