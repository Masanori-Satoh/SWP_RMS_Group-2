package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 3.11 Candidate - Hồ sơ ứng viên.
 * CandidateSource: Website, TopCV, Referral
 * Gender: Male, Female, Other (hoặc theo quy ước của nhóm)
 */
@Entity
@Table(name = "Candidate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CandidateId")
    private Integer candidateId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false, unique = true, referencedColumnName = "UserId")
    private User account;

    @Column(name = "DateOfBirth")
    private LocalDate dateOfBirth;

    @Column(name = "Gender", length = 20)
    private String gender;

    @Column(name = "Address", length = 255)
    private String address;

    @Column(name = "LinkedInUrl", length = 500)
    private String linkedInUrl;

    @Column(name = "PortfolioUrl", length = 500)
    private String portfolioUrl;

    /**
     * Website, TopCV, Referral
     */
    @Column(name = "CandidateSource", length = 50)
    private String candidateSource;
}
