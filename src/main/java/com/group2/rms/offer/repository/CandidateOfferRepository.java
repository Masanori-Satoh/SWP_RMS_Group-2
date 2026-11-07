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
                and (o.isDeleted is null or o.isDeleted = false)
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
                and (o.isDeleted is null or o.isDeleted = false)
            """)
    Optional<OfferProposal> findVisibleById(@Param("id") Integer offerId, @Param("userId") Integer userId,
                                            @Param("statuses") Collection<String> statuses);

    /**
     * Kiểm tra và ghi trong cùng UPDATE, theo một mốc thời gian server truyền vào.
     * Bằng đúng hạn vẫn hợp lệ; 0 dòng là quá hạn, đổi trạng thái, sai chủ sở hữu hoặc hồ sơ không còn Offered.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE OfferProposal o SET o.offerStatus = :to, o.declineReason = :reason, o.updatedAt = CURRENT_TIMESTAMP
            WHERE o.offerId = :id AND o.offerStatus = 'Sent_Candidate'
                AND (o.isDeleted IS NULL OR o.isDeleted = false)
                AND (o.responseDeadline IS NULL OR o.responseDeadline >= :now)
                AND (o.expectedStartDate IS NULL OR o.expectedStartDate >= :today)
                AND EXISTS (SELECT a.applicationId FROM Application a WHERE a.applicationId = o.application.applicationId
                    AND a.candidate.account.userId = :userId AND a.applicationStatus = 'Offered')
            """)
    int answerIfEligible(@Param("id") Integer offerId, @Param("userId") Integer userId,
                         @Param("to") String to, @Param("reason") String reason,
                         @Param("now") java.time.LocalDateTime now, @Param("today") java.time.LocalDate today);
}
