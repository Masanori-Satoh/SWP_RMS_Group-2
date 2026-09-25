package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 3.8 RequisitionApproval - Bản ghi phê duyệt JD bởi Director.
 * Status: Approved / Rejected
 */
@Entity
@Table(name = "RequisitionApproval")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequisitionApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ApprovalId")
    private Integer approvalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RequisitionId", nullable = false, referencedColumnName = "RequisitionId")
    private JobRequisition requisition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DirectorId", nullable = false, referencedColumnName = "UserId")
    private User director;

    /**
     * Approved / Rejected
     */
    @Column(name = "Status", nullable = false, length = 20)
    private String status;

    @Column(name = "Comments", columnDefinition = "NVARCHAR(MAX)")
    private String comments;

    @Column(name = "ApprovalDate", nullable = false)
    private LocalDateTime approvalDate;

    @PrePersist
    protected void onPersist() {
        if (this.approvalDate == null) {
            this.approvalDate = LocalDateTime.now();
        }
    }
}
