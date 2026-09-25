package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 3.18 InterviewFinalResult - Quyết định cuối cùng sau phỏng vấn bởi Hiring Manager.
 * Quan hệ 1-1 với InterviewSchedule (UNIQUE constraint trên InterviewId).
 * FinalDecision: Passed / Failed
 */
@Entity
@Table(name = "InterviewFinalResult")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewFinalResult {

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
     * Passed / Failed
     */
    @Column(name = "FinalDecision", nullable = false, length = 20)
    private String finalDecision;

    @Column(name = "FinalSummaryComments", columnDefinition = "NVARCHAR(MAX)")
    private String finalSummaryComments;

    @Column(name = "ApprovedAt", nullable = false)
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onPersist() {
        if (this.approvedAt == null) {
            this.approvedAt = LocalDateTime.now();
        }
    }
}
