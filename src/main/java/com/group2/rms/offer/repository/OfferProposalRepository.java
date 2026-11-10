package com.group2.rms.offer.repository;

import com.group2.rms.offer.entity.OfferProposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import com.group2.rms.core.util.ListFilters;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.JoinType;
import java.util.ArrayList;
import java.util.Locale;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfferProposalRepository extends JpaRepository<OfferProposal, Integer>, JpaSpecificationExecutor<OfferProposal> {

    // Chỉ tải quan hệ tới một bản ghi; không fetch collection gây sai kết quả phân trang.
    @Override
    @EntityGraph(attributePaths = {"application.candidate.account", "application.jobPosting.requisition.department", "proposedBy"})
    Page<OfferProposal> findAll(Specification<OfferProposal> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"application.candidate.account", "application.jobPosting.requisition.department", "proposedBy"})
    List<OfferProposal> findAll(Specification<OfferProposal> spec, Sort sort);

    @Query("SELECT o.application.applicationId FROM OfferProposal o WHERE o.application.applicationId IN :ids "
            + "AND (o.isDeleted IS NULL OR o.isDeleted = false)")
    List<Integer> findActiveApplicationIds(@Param("ids") List<Integer> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE OfferProposal o SET o.offerStatus = 'Expired', o.updatedAt = CURRENT_TIMESTAMP
            WHERE o.offerStatus = 'Sent_Candidate' AND (o.isDeleted IS NULL OR o.isDeleted = false)
                AND ((o.responseDeadline IS NOT NULL AND o.responseDeadline < :now)
                    OR (o.expectedStartDate IS NOT NULL AND o.expectedStartDate < :today))
            """)
    int expireDue(@Param("now") java.time.LocalDateTime now, @Param("today") java.time.LocalDate today);

    @Query("SELECT COUNT(o) FROM OfferProposal o WHERE (o.isDeleted IS NULL OR o.isDeleted = false) AND o.offerStatus = 'Expired'")
    long countExpired();

    /**
     * Bộ lọc HR/Excel: loại bản ghi xóa mềm, chuẩn hóa trạng thái và dùng cùng tìm kiếm với Giám đốc.
     * ListFilters escape các ký tự %, _ và [ để tìm đúng ký tự người dùng nhập, không coi chúng là mẫu LIKE.
     */
    static Specification<OfferProposal> matchingFilters(String search, String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(builder.or(builder.isNull(root.get("isDeleted")), builder.isFalse(root.get("isDeleted"))));
            predicates.add(matchingKeyword(search).toPredicate(root, query, builder));
            if (!normalized.isEmpty() && !"all".equals(normalized)) {
                var statuses = switch (normalized) {
                    case "approved", "director_approved" -> List.of("approved", "director_approved");
                    case "rejected", "director_rejected" -> List.of("rejected", "director_rejected");
                    default -> List.of(normalized);
                };
                predicates.add(builder.lower(root.get("offerStatus")).in(statuses));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Dùng cùng quy tắc keyword cho HR, Giám đốc và Excel; không quyết định phạm vi quyền/trạng thái. */
    static Specification<OfferProposal> matchingKeyword(String search) {
        String keyword = ListFilters.cleanKeyword(search == null ? null : search.replaceAll("(?U)\\s+", " "));
        return (root, query, builder) -> {
            if (keyword.isEmpty()) return builder.conjunction();
            // #ID là tra mã chính xác; số không có # vẫn là keyword thông thường.
            if (keyword.matches("#[0-9]+")) {
                try { return builder.equal(root.get("offerId"), Integer.parseInt(keyword.substring(1))); }
                catch (NumberFormatException overflow) { return builder.disjunction(); }
            }
            var application = root.join("application", JoinType.LEFT);
            var account = application.join("candidate", JoinType.LEFT).join("account", JoinType.LEFT);
            var department = application.join("jobPosting", JoinType.LEFT).join("requisition", JoinType.LEFT)
                    .join("department", JoinType.LEFT);
            String pattern = ListFilters.containsPattern(keyword);
            var hibernate = (HibernateCriteriaBuilder) builder;
            // Collation chỉ áp dụng cho query, không sửa schema; theo cách dùng chung của dự án.
            return builder.or(List.of(root.<String>get("offeredPositionTitle"), account.<String>get("fullName"),
                            account.<String>get("email"), department.<String>get("departmentName")).stream()
                    .map(field -> builder.like(hibernate.collate(field, ListFilters.ACCENT_INSENSITIVE), pattern, '\\'))
                    .toArray(Predicate[]::new));
        };
    }

    // Tìm các OfferProposal của Application mà chưa bị đóng / không còn active (GBR-14)
    List<OfferProposal> findByApplication_ApplicationIdAndOfferStatusNotIn(
            Integer applicationId,
            List<String> inactiveStatuses
    );

    // Chỉ ghi khi vẫn Pending_Director; 0 dòng là xung đột. Xóa context để lần đọc sau không dùng entity cũ.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE OfferProposal o SET o.offerStatus = :status, o.updatedAt = CURRENT_TIMESTAMP
            WHERE o.offerId = :id AND LOWER(o.offerStatus) = 'pending_director'
                AND (o.isDeleted IS NULL OR o.isDeleted = false)
            """)
    int decideIfPending(@Param("id") Integer id, @Param("status") String status);

    Optional<OfferProposal> findByApplication_ApplicationId(Integer applicationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OfferProposal o WHERE o.offerId = :id AND (o.isDeleted IS NULL OR o.isDeleted = false)")
    Optional<OfferProposal> findActiveForUpdate(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OfferProposal o WHERE o.application.applicationId = :applicationId")
    Optional<OfferProposal> findByApplicationForUpdate(@Param("applicationId") Integer applicationId);

    Page<OfferProposal> findByOfferStatusIgnoreCaseOrderByCreatedAtDesc(String offerStatus, Pageable pageable);

    Page<OfferProposal> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<OfferProposal> findByOfferStatusIgnoreCaseOrderByOfferIdAsc(String offerStatus, Pageable pageable);

    Page<OfferProposal> findAllByOrderByOfferIdAsc(Pageable pageable);

    @Query("SELECT o FROM OfferProposal o WHERE (o.isDeleted IS NULL OR o.isDeleted = false) ORDER BY o.offerId ASC")
    @EntityGraph(attributePaths = {"application.candidate.account", "application.jobPosting.requisition.department", "proposedBy"})
    Page<OfferProposal> findAllActiveByOrderByOfferIdAsc(Pageable pageable);

    @EntityGraph(attributePaths = {"application.candidate.account", "application.jobPosting.requisition.department", "proposedBy"})
    @Query("SELECT o FROM OfferProposal o WHERE (o.isDeleted IS NULL OR o.isDeleted = false) " +
           "AND (LOWER(o.offerStatus) = LOWER(:status) " +
           "     OR (LOWER(:status) = 'director_approved' AND LOWER(o.offerStatus) = 'approved') " +
           "     OR (LOWER(:status) = 'director_rejected' AND LOWER(o.offerStatus) = 'rejected')) " +
           "ORDER BY o.offerId ASC")
    Page<OfferProposal> findActiveByStatusOrderByOfferIdAsc(@Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(o) FROM OfferProposal o WHERE (o.isDeleted IS NULL OR o.isDeleted = false)")
    long countActiveOffers();

    @Query("SELECT COUNT(o) FROM OfferProposal o WHERE (o.isDeleted IS NULL OR o.isDeleted = false) AND LOWER(o.offerStatus) = 'pending_director'")
    long countPendingDirector();

    @Query("SELECT COUNT(o) FROM OfferProposal o WHERE (o.isDeleted IS NULL OR o.isDeleted = false) AND LOWER(o.offerStatus) IN ('director_approved', 'approved')")
    long countDirectorApproved();

    @Query("SELECT COUNT(o) FROM OfferProposal o WHERE (o.isDeleted IS NULL OR o.isDeleted = false) AND LOWER(o.offerStatus) = 'sent_candidate'")
    long countSentCandidate();

    @Query("SELECT COUNT(o) FROM OfferProposal o WHERE (o.isDeleted IS NULL OR o.isDeleted = false) AND LOWER(o.offerStatus) = 'accepted'")
    long countAccepted();

    @Query("SELECT COUNT(o) FROM OfferProposal o WHERE (o.isDeleted IS NULL OR o.isDeleted = false) AND LOWER(o.offerStatus) = 'declined'")
    long countDeclined();

    @Query("SELECT COUNT(o) FROM OfferProposal o WHERE (o.isDeleted IS NULL OR o.isDeleted = false) AND LOWER(o.offerStatus) IN ('director_rejected', 'rejected')")
    long countDirectorRejected();

    @Query("SELECT COUNT(o) FROM OfferProposal o WHERE (o.isDeleted IS NULL OR o.isDeleted = false) AND LOWER(o.offerStatus) != 'draft'")
    long countAllNonDraft();
}
