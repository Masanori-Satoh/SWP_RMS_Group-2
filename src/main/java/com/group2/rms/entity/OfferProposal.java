package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 3.19 OfferProposal - Đề xuất offer lương do Hiring Manager tạo.
 * Quan hệ 1-1 với Application (UNIQUE constraint trên ApplicationId).
 * OfferStatus: Draft, Pending_Director_Approval, Approved, Sent_To_Candidate, Accepted, Rejected
 */
@Entity
@Table(name = "OfferProposal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferProposal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OfferId")
    private Integer offerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApplicationId", nullable = false, unique = true, referencedColumnName = "ApplicationId")
    private Application application;

    @Column(name = "ProposedSalary", nullable = false, precision = 18, scale = 2)
    private BigDecimal proposedSalary;

    /**
     * Lương thử việc (>= 85% ProposedSalary theo constraint trong model)
     */
    @Column(name = "ProbationSalary", nullable = false, precision = 18, scale = 2)
    private BigDecimal probationSalary;

    /**
     * Số ngày thử việc, mặc định 60
     */
    @Column(name = "ProbationDays", nullable = false, columnDefinition = "INT DEFAULT 60")
    private Integer probationDays;

    @Column(name = "ProposedPosition", nullable = false, length = 200)
    private String proposedPosition;

    @Column(name = "WorkLocation", length = 200)
    private String workLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProposedBy", nullable = false, referencedColumnName = "UserId")
    private User proposedBy;

    /**
     * Draft, Pending_Director_Approval, Approved, Sent_To_Candidate, Accepted, Rejected
     */
    @Column(name = "OfferStatus", nullable = false, length = 40)
    private String offerStatus;
}
