package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 3.21 OfferNegotiation - Vòng thương lượng lương giữa ứng viên và HR.
 * Mỗi OfferProposal có thể có nhiều vòng thương lượng.
 */
@Entity
@Table(name = "OfferNegotiation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferNegotiation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NegotiationId")
    private Integer negotiationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OfferId", nullable = false, referencedColumnName = "OfferId")
    private OfferProposal offerProposal;

    /**
     * Mức lương đề xuất ngược từ ứng viên
     */
    @Column(name = "CandidateCounterSalary", precision = 18, scale = 2)
    private BigDecimal candidateCounterSalary;

    @Column(name = "CandidateNotes", columnDefinition = "NVARCHAR(MAX)")
    private String candidateNotes;

    @Column(name = "HRResponseNotes", columnDefinition = "NVARCHAR(MAX)")
    private String hrResponseNotes;

    @Column(name = "NegotiationDate", nullable = false)
    private LocalDateTime negotiationDate;

    @PrePersist
    protected void onPersist() {
        if (this.negotiationDate == null) {
            this.negotiationDate = LocalDateTime.now();
        }
    }
}
