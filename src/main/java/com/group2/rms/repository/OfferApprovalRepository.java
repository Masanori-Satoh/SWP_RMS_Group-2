package com.group2.rms.repository;

import com.group2.rms.entity.OfferApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferApprovalRepository extends JpaRepository<OfferApproval, Integer> {

    List<OfferApproval> findByOfferProposalOfferIdOrderByApprovedAtDesc(Integer offerId);
}