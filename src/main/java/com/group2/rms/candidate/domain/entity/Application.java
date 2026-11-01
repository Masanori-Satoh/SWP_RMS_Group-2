package com.group2.rms.candidate.domain.entity;

import com.group2.rms.core.base.BaseEntity;
import com.group2.rms.jobposting.entity.JobPosting;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 3.13 Application - Hồ sơ ứng tuyển của ứng viên cho một vị trí.
 * ApplicationStatus: Applied, HR_Passed, HM_Passed, HM_Suggested,
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
     * Applied, HR_Passed, HM_Passed, HM_Suggested, Interviewing, Offered, Hired, Rejected
     */
    @Column(name = "ApplicationStatus", nullable = false, length = 30)
    private String applicationStatus;

    @PrePersist
    protected void onApplicationPersist() {
        if (this.submissionDate == null) {
            this.submissionDate = LocalDateTime.now();
        }
    }
}
