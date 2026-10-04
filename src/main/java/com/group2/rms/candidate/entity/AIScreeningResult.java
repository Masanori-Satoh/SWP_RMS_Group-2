package com.group2.rms.candidate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 3.14 AIScreeningResult - Kết quả chấm điểm tự động bởi AI.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApplicationId", nullable = false, referencedColumnName = "ApplicationId")
    private Application application;

    /**
     * Điểm AI match từ 0.00 đến 100.00
     */
    @Column(name = "AIMatchScore", nullable = false, precision = 5, scale = 2)
    private BigDecimal aiMatchScore;

    @Column(name = "ScreenedAt", nullable = false)
    private LocalDateTime screenedAt;

    @PrePersist
    protected void onPersist() {
        if (this.screenedAt == null) {
            this.screenedAt = LocalDateTime.now();
        }
    }
}
