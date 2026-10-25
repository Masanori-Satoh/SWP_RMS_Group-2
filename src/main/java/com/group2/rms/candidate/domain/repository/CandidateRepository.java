package com.group2.rms.candidate.domain.repository;

import com.group2.rms.candidate.domain.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
//new interface cho dynacmic filter nha các bảo bối
public interface CandidateRepository extends JpaRepository<Candidate, Integer>, JpaSpecificationExecutor<Candidate> {
    Optional<Candidate> findByAccountUserId(Integer userId);

    Optional<Candidate> findByAccount_UsernameIgnoreCase(String username);

    List<Candidate> findAllByAccountUserIdIn(Collection<Integer> userIds);
    //filter potential candidate based on  filter
    String POTENTIAL_FILTER = """
        FROM Candidate c JOIN [User] u ON u.UserId = c.UserId
        WHERE c.IsPotential = 1
          AND (:keyword IS NULL OR u.FullName LIKE CONCAT('%', :keyword, '%') OR u.Email LIKE CONCAT('%', :keyword, '%'))
          AND (:markedBy IS NULL OR EXISTS (SELECT 1 FROM AuditLog l
            WHERE l.EntityName = 'Candidate' AND l.EntityId = CAST(c.CandidateId AS NVARCHAR(50))
              AND l.NewValue = 'TalentPoolMarked' AND l.UserId = :markedBy))
          AND (:sourceJobId IS NULL OR EXISTS (SELECT 1 FROM Application a
            WHERE a.CandidateId = c.CandidateId AND a.JobPostingId = :sourceJobId
              AND a.ApplicationStatus <> 'HM_Suggested'))
        """;

    @Query(value = "SELECT c.* " + POTENTIAL_FILTER + " ORDER BY c.CandidateId DESC",
           countQuery = "SELECT COUNT(*) " + POTENTIAL_FILTER, nativeQuery = true)
    Page<Candidate> searchPotential(@Param("keyword") String keyword, @Param("markedBy") Integer markedBy,
                                   @Param("sourceJobId") Integer sourceJobId, Pageable pageable);
}
