package com.group2.rms.interview.entity;

import com.group2.rms.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 3.18 InterviewFinalResult - Quyết định cuối cùng sau phỏng vấn bởi Hiring Manager.
 * Quan hệ 1-1 với InterviewSchedule (UNIQUE constraint trên InterviewId).
 * FinalDecision: Passed / Failed / Considering.
 * Considering là quyết định tạm: HM chốt lại thành Passed/Failed sau; Passed/Failed là cuối cùng.
 */
@Entity
@Table(name = "InterviewFinalResult")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewFinalResult {

    public static final String PASSED = "Passed";
    public static final String FAILED = "Failed";
    public static final String CONSIDERING = "Considering";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FinalResultId")
    private Integer finalResultId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "InterviewId", nullable = false, unique = true, referencedColumnName = "InterviewId")
    private InterviewSchedule interviewSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HiringManagerId", nullable = false, referencedColumnName = "UserId")
    private User hiringManager;

    /**
     * Passed / Failed / Considering
     */
    @Column(name = "FinalDecision", nullable = false, length = 20)
    private String finalDecision;

    @Column(name = "RecommendedSalary", precision = 18, scale = 2)
    private BigDecimal recommendedSalary;

    @Column(name = "FinalSummaryComments", columnDefinition = "NVARCHAR(MAX)")
    private String finalSummaryComments;

    /** Thời điểm quyết định hiện tại (cập nhật lại khi chốt từ Considering sang Passed/Failed). */
    @Column(name = "ApprovedAt", nullable = false)
    private LocalDateTime approvedAt;

    /** Lúc HM chọn Considering lần đầu; giữ nguyên khi chốt lại, NULL nếu chốt thẳng. */
    @Column(name = "ConsideredAt")
    private LocalDateTime consideredAt;

    public boolean isConsidering() {
        return CONSIDERING.equals(finalDecision);
    }

    @PrePersist
    protected void onPersist() {
        if (this.approvedAt == null) {
            this.approvedAt = LocalDateTime.now();
        }
    }
}
