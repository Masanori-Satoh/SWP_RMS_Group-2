package com.group2.rms.repository;

import com.group2.rms.entity.OfferProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfferProposalRepository extends JpaRepository<OfferProposal, Integer> {

    // Tìm các OfferProposal của Application mà chưa bị đóng / không còn active (GBR-14)
    List<OfferProposal> findByApplication_ApplicationIdAndOfferStatusNotIn(
            Integer applicationId,
            List<String> inactiveStatuses
    );

    Optional<OfferProposal> findByApplication_ApplicationId(Integer applicationId);
}
