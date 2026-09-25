package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 3.12 ApplicationReview (Resume) - File CV của ứng viên.
 * Lưu metadata file CV: tên, đường dẫn, loại, kích thước.
 * FileType: PDF, DOCX
 */
@Entity
@Table(name = "ApplicationReview")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ResumeId")
    private Integer resumeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CandidateId", nullable = false, referencedColumnName = "CandidateId")
    private Candidate candidate;

    @Column(name = "FileName", nullable = false, length = 255)
    private String fileName;

    @Column(name = "FilePath", nullable = false, length = 1000)
    private String filePath;

    /**
     * PDF, DOCX
     */
    @Column(name = "FileType", length = 10)
    private String fileType;

    @Column(name = "FileSize")
    private Long fileSize;

    @Column(name = "UploadedAt", nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onPersist() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }
}
