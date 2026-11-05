package com.group2.rms.offer.repository;

import com.group2.rms.offer.entity.OfferApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface OfferApprovalRepository extends JpaRepository<OfferApproval, Integer> {

    List<OfferApproval> findByOfferProposal_OfferIdOrderByApprovedAtDesc(Integer offerId);

    /**
     * Lấy lịch sử nhiều Offer trong một lần; sắp ApprovedAt giảm dần rồi ID giảm dần để xử lý cả hai log cùng thời điểm.
     * Service dùng phần tử đầu tiên của từng Offer; fetch director để đọc tên mà không phát sinh truy vấn cho từng dòng.
     */
    @EntityGraph(attributePaths = "director")
    List<OfferApproval> findByOfferProposal_OfferIdInOrderByApprovedAtDescOfferApprovalIdDesc(List<Integer> offerIds);

    Page<OfferApproval> findByDirector_UserIdOrderByApprovedAtDesc(Integer directorId, Pageable pageable);

    long countByDirector_UserIdAndStatusIgnoreCase(Integer directorId, String status);
}
