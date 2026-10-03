package com.group2.rms.offer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferProposalRepository extends JpaRepository<OfferProposal, Integer> {
}
