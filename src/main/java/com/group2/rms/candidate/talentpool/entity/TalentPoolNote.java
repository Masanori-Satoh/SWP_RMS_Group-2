package com.group2.rms.candidate.talentpool.entity;

import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TalentPoolNote")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor 
@Builder
public class TalentPoolNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NoteId")
    private Integer noteId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CandidateId", nullable = false)
    private Candidate candidate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserId", nullable = false)
    private User author;
    @Column(name = "NoteContent", nullable = false, length = 1000)
    private String noteContent;
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @PrePersist
    void created() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}

