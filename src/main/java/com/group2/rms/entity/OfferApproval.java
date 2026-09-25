package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 3.20 OfferApproval - Phê duyệt offer bởi Director.
 * Status: Approved / Rejected
 */
@Entity
@Table(name = "OfferApproval")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OfferApprovalId")
    private Integer offerApprovalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OfferId", nullable = false, referencedColumnName = "OfferId")
    private OfferProposal offerProposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DirectorId", nullable = false, referencedColumnName = "UserId")
    private User director;

    /**
     * Approved / Rejected
     */
    @Column(name = "Status", nullable = false, length = 20)
    private String status;

    @Column(name = "DirectorComments", columnDefinition = "NVARCHAR(MAX)")
    private String directorComments;

    @Column(name = "ApprovedAt", nullable = false)
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onPersist() {
        if (this.approvedAt == null) {
            this.approvedAt = LocalDateTime.now();
        }
    }
}
