package com.group2.rms.offer.repository;

import com.group2.rms.offer.entity.OfferApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferApprovalRepository extends JpaRepository<OfferApproval, Integer> {

    List<OfferApproval> findByOfferProposal_OfferIdOrderByApprovedAtDesc(Integer offerId);
}
