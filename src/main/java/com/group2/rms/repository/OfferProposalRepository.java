package com.group2.rms.repository;

import com.group2.rms.entity.OfferProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferProposalRepository extends JpaRepository<OfferProposal, Integer> {
}
