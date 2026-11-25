package com.group2.rms.candidate.repository;

import com.group2.rms.candidate.entity.ApplicationReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationReviewRepository extends JpaRepository<ApplicationReview, Integer> {

        boolean existsByApplication_ApplicationIdAndReviewerRoleAndDecision(
                        Integer applicationId, String reviewerRole, String decision);

        /** Lượt duyệt mới nhất của đơn (ReviewId lớn nhất); dùng để biết đơn bị loại ở bước nào khi khôi phục. */
        Optional<ApplicationReview> findFirstByApplication_ApplicationIdOrderByReviewIdDesc(Integer applicationId);

        /**
         * Vai trò đã loại đơn ở lượt duyệt mới nhất; {@code null} nếu lượt mới nhất không phải {@code Fail} (hoặc chưa
         * có lượt nào), tức đơn không bị loại ở vòng CV.
         */
        default String findLastFailRole(Integer applicationId) {
                return findFirstByApplication_ApplicationIdOrderByReviewIdDesc(applicationId)
                                .filter(review -> "Fail".equals(review.getDecision()))
                                .map(ApplicationReview::getReviewerRole)
                                .orElse(null);
        }

        @Query("""
                        SELECT rv FROM ApplicationReview rv
                        JOIN FETCH rv.reviewer
                        WHERE rv.application.applicationId = :id
                        """)
        List<ApplicationReview> findWithReviewerByApplicationId(@Param("id") Integer applicationId);
}
