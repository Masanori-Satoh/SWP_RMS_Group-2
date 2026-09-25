package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 3.9 ScreeningCriteria - Tiêu chí sàng lọc CV tự động bởi AI.
 * CriteriaType: Education, Experience, Skill, Knockout
 */
@Entity
@Table(name = "ScreeningCriteria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreeningCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CriteriaId")
    private Integer criteriaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RequisitionId", nullable = false, referencedColumnName = "RequisitionId")
    private JobRequisition requisition;

    @Column(name = "CriteriaName", nullable = false, length = 200)
    private String criteriaName;

    /**
     * Education, Experience, Skill, Knockout
     */
    @Column(name = "CriteriaType", nullable = false, length = 50)
    private String criteriaType;

    @Column(name = "RequiredValue", nullable = false, length = 255)
    private String requiredValue;

    /**
     * Trọng số AI scoring, mặc định 1.00
     */
    @Column(name = "Weight", nullable = false, precision = 5, scale = 2,
            columnDefinition = "DECIMAL(5,2) DEFAULT 1.00")
    private BigDecimal weight;

    /**
     * 1 = bắt buộc, 0 = tùy chọn
     */
    @Column(name = "IsMandatory", nullable = false, columnDefinition = "BIT DEFAULT 0")
    private Boolean isMandatory;
}
