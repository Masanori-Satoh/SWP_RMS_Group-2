package com.group2.rms.offer;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.core.base.BaseEntity;
import com.group2.rms.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 3.19 OfferProposal - Đề xuất offer lương do Hiring Manager tạo.
 * Quan hệ 1-1 với Application (UNIQUE constraint trên ApplicationId).
 * OfferStatus: Draft, Pending_Director, Approved, Sent_Candidate,
 * Accepted, Rejected, Negotiating
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApplicationId", nullable = false, referencedColumnName = "ApplicationId")
    private Application application;

    @Column(name = "OfferedPositionTitle", nullable = false, length = 200)
    private String offeredPositionTitle;

    @Column(name = "ProposedSalary", nullable = false, precision = 18, scale = 2)
    private BigDecimal proposedSalary;

    /**
     * Lương thử việc (>= 85% ProposedSalary theo constraint trong model)
     */
    @Column(name = "ProbationSalary", nullable = false, precision = 18, scale = 2)
    private BigDecimal probationSalary;

    @Column(name = "ExpectedStartDate")
    private LocalDate expectedStartDate;

    @Column(name = "WorkLocation", length = 255)
    private String workLocation;

    @Column(name = "BenefitsPackage", columnDefinition = "NVARCHAR(MAX)")
    private String benefitsPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProposedBy", nullable = false, referencedColumnName = "UserId")
    private User proposedBy;

    /**
     * Draft, Pending_Director, Approved, Sent_Candidate, Accepted, Rejected, Negotiating
     */
    @Column(name = "OfferStatus", nullable = false, length = 40)
    private String offerStatus;

    @Column(name = "IsDeleted")
    @Builder.Default
    private Boolean isDeleted = false;
}
