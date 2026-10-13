package com.group2.rms.offer.repository;

import com.group2.rms.offer.entity.OfferProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Offer nhìn từ phía ứng viên (trang /portal/offers). Mọi query giới hạn theo chính ứng viên đăng nhập
 * và chỉ các offer đã gửi cho ứng viên; offer nội bộ (Draft, Pending_Director...) không bao giờ trả ra.
 */
@Repository
public interface CandidateOfferRepository extends JpaRepository<OfferProposal, Integer> {

    @Query("""
            select o from OfferProposal o
                join fetch o.application a
                join fetch a.jobPosting
            where a.candidate.account.userId = :userId and o.offerStatus in :statuses
            order by o.offerId desc
            """)
    List<OfferProposal> findVisibleByUserId(@Param("userId") Integer userId,
                                            @Param("statuses") Collection<String> statuses);

    @Query("""
            select o from OfferProposal o
                join fetch o.application a
                join fetch a.candidate c
                join fetch c.account u
                join fetch a.jobPosting jp
                left join fetch jp.requisition req
                left join fetch req.department dept
                left join fetch o.proposedBy p
            where o.offerId = :id and u.userId = :userId and o.offerStatus in :statuses
            """)
    Optional<OfferProposal> findVisibleById(@Param("id") Integer offerId, @Param("userId") Integer userId,
                                            @Param("statuses") Collection<String> statuses);

    /**
     * Chỉ chuyển {@code Sent_Candidate → to}. Trả 0 nghĩa là offer đã được trả lời (hoặc đổi trạng thái)
     * trước đó, để hai lần bấm gần nhau không ghi đè nhau.
     */
    @Modifying
    @Query("UPDATE OfferProposal o SET o.offerStatus = :to, o.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE o.offerId = :id AND o.offerStatus = 'Sent_Candidate'")
    int answerIfSent(@Param("id") Integer offerId, @Param("to") String to);
}
