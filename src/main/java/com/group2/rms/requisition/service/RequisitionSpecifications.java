package com.group2.rms.requisition.service;

import com.group2.rms.core.util.ListFilters;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;


public final class RequisitionSpecifications {

    private RequisitionSpecifications() {
    }


    // Builds a title or department search condition.
    public static Specification<JobRequisition> titleOrDepartmentContains(String keyword) {
        String pattern = ListFilters.containsPattern(keyword);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.join("department", JoinType.LEFT).get("departmentName")), pattern, '\\'));
    }


    // Builds a status filter condition.
    public static Specification<JobRequisition> statusIn(List<String> statuses) {
        return (root, query, cb) -> root.get("approvalStatus").in(statuses);
    }


    // Builds a department filter condition.
    public static Specification<JobRequisition> departmentIs(Integer departmentId) {
        return (root, query, cb) -> cb.equal(root.get("department").get("departmentId"), departmentId);
    }


    // Builds a creator filter condition.
    public static Specification<JobRequisition> creatorIs(Integer userId) {
        return (root, query, cb) -> cb.equal(root.get("hiringManager").get("userId"), userId);
    }


    // Builds an employment type filter condition.
    public static Specification<JobRequisition> employmentTypeIs(String employmentType) {
        return (root, query, cb) -> cb.equal(root.get("employmentType"), employmentType);
    }


    // Builds a lower creation date condition.
    public static Specification<JobRequisition> createdOnOrAfter(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
    }


    // Builds an upper creation date condition.
    public static Specification<JobRequisition> createdOnOrBefore(LocalDate to) {
        return (root, query, cb) -> cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay());
    }


    // Builds a condition for requisitions without postings.
    public static Specification<JobRequisition> hasNoPosting() {
        return (root, query, cb) -> {
            Subquery<Integer> postingIds = query.subquery(Integer.class);
            Root<JobPosting> posting = postingIds.from(JobPosting.class);
            postingIds.select(posting.get("jobPostingId")).where(cb.equal(posting.get("requisition"), root));
            return cb.not(cb.exists(postingIds));
        };
    }


    // Builds a latest posting status condition.
    public static Specification<JobRequisition> latestPostingStatusIs(String postingStatus) {
        return (root, query, cb) -> {
            Subquery<Integer> latestId = query.subquery(Integer.class);
            Root<JobPosting> latest = latestId.from(JobPosting.class);
            latestId.select(cb.max(latest.get("jobPostingId")))
                    .where(cb.equal(latest.get("requisition"), root));
            Subquery<Integer> match = query.subquery(Integer.class);
            Root<JobPosting> posting = match.from(JobPosting.class);
            match.select(posting.get("jobPostingId"))
                    .where(cb.equal(posting.get("jobPostingId"), latestId),
                            cb.equal(posting.get("postingStatus"), postingStatus));
            return cb.exists(match);
        };
    }

    // Orders pending-approval requisitions first, then newest. Only applies when the Pageable is unsorted.
    public static Specification<JobRequisition> pendingFirstThenNewest() {
        return (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType())) {
                query.orderBy(
                        cb.asc(cb.selectCase()
                                .when(cb.equal(root.get("approvalStatus"),
                                        RequisitionAccess.STATUS_PENDING_DIRECTOR), 0)
                                .otherwise(1)),
                        cb.desc(root.get("createdAt")),
                        cb.desc(root.get("requisitionId")));
            }
            return null;
        };
    }
}
