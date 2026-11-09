package com.group2.rms.offer.entity;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.core.base.BaseEntity;
import com.group2.rms.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 3.19 OfferProposal - Đề xuất thư mời do HR tạo; Giám đốc chỉ duyệt hoặc từ chối.
 * Quan hệ 1-1 với Application (UNIQUE constraint trên ApplicationId).
 * OfferStatus: Draft, Pending_Director, Director_Approved, Director_Rejected, Sent_Candidate,
 * Accepted, Declined
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
     * Lương thử việc (>= 85% ProposedSalary theo constraint trong model BR-OFF-01)
     */
    @Column(name = "ProbationSalary", nullable = false, precision = 18, scale = 2)
    private BigDecimal probationSalary;

    @Column(name = "ProbationDays", nullable = false)
    @Builder.Default
    private Integer probationDays = 60;

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
     * Draft, Pending_Director, Director_Approved, Director_Rejected, Sent_Candidate, Expired, Accepted, Declined
     */
    @Column(name = "OfferStatus", nullable = false, length = 40)
    private String offerStatus;

    @Column(name = "DeclineReason", length = 1000)
    private String declineReason;

    @Column(name = "ResponsePeriodDays")
    private Integer responsePeriodDays;

    @Column(name = "ResponseDeadline")
    private LocalDateTime responseDeadline;

    @Column(name = "IsDeleted")
    @Builder.Default
    private Boolean isDeleted = false;
    /**
     * Expired là hết hạn đã lưu; Sent_Candidate cũng coi là hết hạn nếu đã qua mốc thời gian, dù job chưa chạy.
     * Dùng chung cho nhãn quá hạn của HR/ứng viên; chỉ tính toán, không thay đổi dữ liệu database.
     */
    public boolean isResponseExpiredAt(LocalDateTime now) {
        return "Expired".equals(offerStatus) || "Sent_Candidate".equals(offerStatus)
                && (responseDeadline != null && now.isAfter(responseDeadline)
                    || expectedStartDate != null && expectedStartDate.isBefore(now.toLocalDate()));
    }
}
