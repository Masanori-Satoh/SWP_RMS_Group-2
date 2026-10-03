package com.group2.rms.offer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Page<OfferProposal> findByOfferStatusIgnoreCaseOrderByCreatedAtDesc(String offerStatus, Pageable pageable);

    Page<OfferProposal> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<OfferProposal> findByOfferStatusIgnoreCaseOrderByOfferIdAsc(String offerStatus, Pageable pageable);

    Page<OfferProposal> findAllByOrderByOfferIdAsc(Pageable pageable);

    @Query("SELECT o FROM OfferProposal o WHERE (o.isDeleted IS NULL OR o.isDeleted = false) ORDER BY o.offerId ASC")
    Page<OfferProposal> findAllActiveByOrderByOfferIdAsc(Pageable pageable);

    @Query("SELECT o FROM OfferProposal o WHERE (o.isDeleted IS NULL OR o.isDeleted = false) AND LOWER(o.offerStatus) = LOWER(:status) ORDER BY o.offerId ASC")
    Page<OfferProposal> findActiveByStatusOrderByOfferIdAsc(@Param("status") String status, Pageable pageable);
}
