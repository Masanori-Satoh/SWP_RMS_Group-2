package com.group2.rms.offer.repository;

import com.group2.rms.offer.entity.OfferNegotiation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferNegotiationRepository extends JpaRepository<OfferNegotiation, Integer> {

    List<OfferNegotiation> findByOfferProposal_OfferIdOrderByNegotiationDateDesc(Integer offerId);
}
