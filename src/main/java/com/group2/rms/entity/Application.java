package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 3.13 Application - Hồ sơ ứng tuyển của ứng viên cho một vị trí.
 * ApplicationStatus: Applied, Screening, Interview_Pending, Interviewing,
 *                    Offered, Hired, Rejected
 */
@Entity
@Table(name = "Application")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ApplicationId")
    private Integer applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CandidateId", nullable = false, referencedColumnName = "CandidateId")
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JobPostingId", nullable = false, referencedColumnName = "JobPostingId")
    private JobPosting jobPosting;

    /**
     * Đường dẫn CV dùng cho ứng tuyển này (TEXT)
     */
    @Column(name = "AppliedCvUrl", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String appliedCvUrl;

    @Column(name = "SubmissionDate", nullable = false)
    private LocalDateTime submissionDate;

    /**
     * Applied, Screening, Interview_Pending, Interviewing, Offered, Hired, Rejected
     */
    @Column(name = "ApplicationStatus", nullable = false, length = 30)
    private String applicationStatus;

    /**
     * Điểm tổng hợp (từ AI + đánh giá phỏng vấn)
     */
    @Column(name = "OverallScore", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "HRReviewNotes", columnDefinition = "NVARCHAR(MAX)")
    private String hrReviewNotes;

    @Column(name = "HMReviewNotes", columnDefinition = "NVARCHAR(MAX)")
    private String hmReviewNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReviewedBy", referencedColumnName = "UserId")
    private User reviewedBy;

    @Column(name = "ReviewedAt")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onApplicationPersist() {
        if (this.submissionDate == null) {
            this.submissionDate = LocalDateTime.now();
        }
    }
}
