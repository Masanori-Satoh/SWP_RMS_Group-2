package com.group2.rms.candidate.repository;

import com.group2.rms.candidate.entity.TalentPoolNote;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface TalentPoolNoteRepository extends JpaRepository<TalentPoolNote, Integer> {
    @EntityGraph(attributePaths = "author")
    List<TalentPoolNote> findByCandidate_CandidateIdOrderByCreatedAtDescNoteIdDesc(Integer candidateId);
}

