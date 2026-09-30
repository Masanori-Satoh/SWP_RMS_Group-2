package com.group2.rms.repository;

import com.group2.rms.entity.OfferProposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfferProposalRepository extends JpaRepository<OfferProposal, Integer> {

		boolean existsByApplicationApplicationId(Integer applicationId);

		java.util.List<OfferProposal> findByApplicationApplicationId(Integer applicationId);

		Page<OfferProposal> findByApplicationApplicationId(Integer applicationId, Pageable pageable);

		Page<OfferProposal> findByOfferStatus(String offerStatus, Pageable pageable);

		@EntityGraph(attributePaths = {"application", "application.candidate", "application.jobPosting", "proposedBy"})
		Optional<OfferProposal> findDetailedByOfferId(Integer offerId);

		@EntityGraph(attributePaths = {"application", "application.candidate", "application.jobPosting", "proposedBy"})
		@Query("""
						select offer from OfferProposal offer
						left join offer.application application
						left join application.candidate candidate
						where (:status is null or :status = '' or offer.offerStatus = :status or upper(offer.offerStatus) = upper(:status))
							and (:candidateName is null or :candidateName = '' or lower(candidate.fullName) like lower(concat('%', :candidateName, '%')))
						""")
		Page<OfferProposal> search(
				@org.springframework.data.repository.query.Param("status") String status,
				@org.springframework.data.repository.query.Param("candidateName") String candidateName,
				Pageable pageable
		);

		@Query("""
						select distinct offer.offerStatus from OfferProposal offer
						where offer.offerStatus is not null and offer.offerStatus <> ''
						order by offer.offerStatus
						""")
		java.util.List<String> findDistinctOfferStatuses();

		@Query("""
						select offer from OfferProposal offer
						where offer.offerStatus in :statuses
						  and offer.createdAt <= :threshold
						""")
		java.util.List<OfferProposal> findExpiredOffers(
				@org.springframework.data.repository.query.Param("statuses") java.util.Collection<String> statuses,
				@org.springframework.data.repository.query.Param("threshold") java.time.LocalDateTime threshold
		);
}
