package com.group2.rms.offer.entity;

import com.group2.rms.user.entity.User;
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
public class OfferApproval {

    public OfferApproval(OfferProposal offerProposal, User director, String status, String directorComments) {
        this.offerProposal = offerProposal;
        this.director = director;
        this.status = status;
        this.directorComments = directorComments;
        this.approvedAt = LocalDateTime.now();
    }

    public static OfferApprovalBuilder builder() {
        return new OfferApprovalBuilder();
    }

    public static class OfferApprovalBuilder {
        private Integer offerApprovalId;
        private OfferProposal offerProposal;
        private User director;
        private String status;
        private String directorComments;
        private LocalDateTime approvedAt;

        public OfferApprovalBuilder offerApprovalId(Integer offerApprovalId) {
            this.offerApprovalId = offerApprovalId;
            return this;
        }

        public OfferApprovalBuilder offerProposal(OfferProposal offerProposal) {
            this.offerProposal = offerProposal;
            return this;
        }

        public OfferApprovalBuilder director(User director) {
            this.director = director;
            return this;
        }

        public OfferApprovalBuilder status(String status) {
            this.status = status;
            return this;
        }

        public OfferApprovalBuilder directorComments(String directorComments) {
            this.directorComments = directorComments;
            return this;
        }

        public OfferApprovalBuilder approvedAt(LocalDateTime approvedAt) {
            this.approvedAt = approvedAt;
            return this;
        }

        public OfferApproval build() {
            OfferApproval a = new OfferApproval();
            a.setOfferApprovalId(this.offerApprovalId);
            a.setOfferProposal(this.offerProposal);
            a.setDirector(this.director);
            a.setStatus(this.status);
            a.setDirectorComments(this.directorComments);
            a.setApprovedAt(this.approvedAt != null ? this.approvedAt : LocalDateTime.now());
            return a;
        }
    }

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
