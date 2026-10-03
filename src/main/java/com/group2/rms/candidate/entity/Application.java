package com.group2.rms.candidate.entity;

import com.group2.rms.core.base.BaseEntity;
import com.group2.rms.requisition.entity.JobPosting;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 3.13 Application - Hồ sơ ứng tuyển của ứng viên cho một vị trí.
 * ApplicationStatus: Applied, AI_Screened, HR_Passed, HM_Passed,
 *                    Interviewing, Offered, Hired, Rejected
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
     * Đường dẫn CV dùng cho ứng tuyển này.
     */
    @Column(name = "AppliedCvUrl", nullable = false, length = 500)
    private String appliedCvUrl;

    @Column(name = "SubmissionDate", nullable = false)
    private LocalDateTime submissionDate;

    /**
     * Applied, AI_Screened, HR_Passed, HM_Passed, Interviewing, Offered, Hired, Rejected
     */
    @Column(name = "ApplicationStatus", nullable = false, length = 30)
    private String applicationStatus;

    /**
     * Điểm tổng hợp (từ AI + đánh giá phỏng vấn)
     */
    @Column(name = "OverallScore", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @PrePersist
    protected void onApplicationPersist() {
        if (this.submissionDate == null) {
            this.submissionDate = LocalDateTime.now();
        }
    }
}
