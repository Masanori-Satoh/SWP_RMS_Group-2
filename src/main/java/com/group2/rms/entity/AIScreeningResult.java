package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 3.14 AIScreeningResult - Kết quả chấm điểm tự động bởi AI.
 * AIRecommendation: Strongly_Recommend, Consider, Reject
 */
@Entity
@Table(name = "AIScreeningResult")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIScreeningResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AIScreeningId")
    private Integer aiScreeningId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApplicationId", nullable = false, unique = true, referencedColumnName = "ApplicationId")
    private Application application;

    /**
     * Điểm AI match từ 0.00 đến 100.00
     */
    @Column(name = "AIMatchScore", nullable = false, precision = 5, scale = 2)
    private BigDecimal aiMatchScore;

    /**
     * Kỹ năng phù hợp, dạng JSON
     */
    @Column(name = "MatchedSkills", columnDefinition = "NVARCHAR(MAX)")
    private String matchedSkills;

    /**
     * Yêu cầu không đáp ứng, dạng JSON
     */
    @Column(name = "UnmatchedRequirements", columnDefinition = "NVARCHAR(MAX)")
    private String unmatchedRequirements;

    @Column(name = "AISummary", columnDefinition = "NVARCHAR(MAX)")
    private String aiSummary;

    /**
     * Strongly_Recommend, Consider, Reject
     */
    @Column(name = "AIRecommendation", nullable = false, length = 30)
    private String aiRecommendation;

    @Column(name = "ScreenedAt", nullable = false)
    private LocalDateTime screenedAt;

    @PrePersist
    protected void onPersist() {
        if (this.screenedAt == null) {
            this.screenedAt = LocalDateTime.now();
        }
    }
}
