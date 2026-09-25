package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 3.17 InterviewEvaluation - Phiếu đánh giá ứng viên do Interviewer điền sau phỏng vấn.
 * Recommendation: Hire, No_Hire, Consider
 * Score fields: TechnicalScore, SoftSkillScore, CulturalFitScore (1-5)
 */
@Entity
@Table(name = "InterviewEvaluation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EvaluationId")
    private Integer evaluationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "InterviewId", nullable = false, referencedColumnName = "InterviewId")
    private InterviewSchedule interviewSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "InterviewerId", nullable = false, referencedColumnName = "UserId")
    private User interviewer;

    /**
     * Điểm kỹ năng kỹ thuật (1-5)
     */
    @Column(name = "TechnicalScore")
    private Integer technicalScore;

    /**
     * Điểm kỹ năng mềm (1-5)
     */
    @Column(name = "SoftSkillScore")
    private Integer softSkillScore;

    /**
     * Điểm phù hợp văn hóa (1-5)
     */
    @Column(name = "CulturalFitScore")
    private Integer culturalFitScore;

    @Column(name = "Strengths", columnDefinition = "NVARCHAR(MAX)")
    private String strengths;

    @Column(name = "Weaknesses", columnDefinition = "NVARCHAR(MAX)")
    private String weaknesses;

    /**
     * Hire, No_Hire, Consider
     */
    @Column(name = "Recommendation", nullable = false, length = 20)
    private String recommendation;

    @Column(name = "Comments", columnDefinition = "NVARCHAR(MAX)")
    private String comments;

    @Column(name = "EvaluatedAt", nullable = false)
    private LocalDateTime evaluatedAt;

    @PrePersist
    protected void onPersist() {
        if (this.evaluatedAt == null) {
            this.evaluatedAt = LocalDateTime.now();
        }
    }
}
