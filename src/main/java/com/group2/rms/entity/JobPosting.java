package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 3.10 JobPosting - Tin tuyển dụng công khai (từ JobRequisition đã được duyệt).
 * PostingStatus: Draft, Published, Paused, Closed
 */
@Entity
@Table(name = "JobPosting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPosting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "JobPostingId")
    private Integer jobPostingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RequisitionId", nullable = false, referencedColumnName = "RequisitionId")
    private JobRequisition requisition;

    @Column(name = "PostingTitle", nullable = false, length = 200)
    private String postingTitle;

    @Column(name = "JobDescription", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String jobDescription;

    @Column(name = "JobRequirements", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String jobRequirements;

    @Column(name = "Benefits", columnDefinition = "NVARCHAR(MAX)")
    private String benefits;

    @Column(name = "SalaryDisplay", length = 100)
    private String salaryDisplay;

    @Column(name = "WorkLocation", length = 200)
    private String workLocation;

    @Column(name = "PostingDate")
    private LocalDate postingDate;

    @Column(name = "ApplicationDeadline")
    private LocalDate applicationDeadline;

    /**
     * Draft, Published, Paused, Closed
     */
    @Column(name = "PostingStatus", nullable = false, length = 20)
    private String postingStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false, referencedColumnName = "UserId")
    private User createdBy;
}
