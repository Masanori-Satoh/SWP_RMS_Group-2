package com.group2.rms.dashboard;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import com.group2.rms.core.security.RoleAuthorities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Read-only aggregates for the LinhDN dashboard. Each personal query is scoped
 * in JPQL.
 */
@Repository
public class DashboardMetricsRepository {

    /** Only offers sent to the candidate are visible; director decisions stay internal. */
    private static final String CANDIDATE_VISIBLE_OFFER_STATUSES =
            "('Sent_Candidate', 'Accepted', 'Declined')";

    private final EntityManager entityManager;

    public DashboardMetricsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long accounts() {
        return count("select count(u) from User u", Map.of());
    }

    public List<StatusCount> departmentStatuses() {
        return statuses("select d.departmentStatus, count(d) from Department d group by d.departmentStatus", Map.of());
    }

    public long accountsWithStatus(String status) {
        return count("select count(u) from User u where u.accountStatus = :status", Map.of("status", status));
    }

    public List<StatusCount> internalAccountStatuses() {
        return statuses("select u.accountStatus, count(u) from User u where u.role.roleName in :roles "
                + "group by u.accountStatus", Map.of("roles", RoleAuthorities.INTERNAL_ROLE_NAMES));
    }

    public List<StatusCount> candidateAccountStatuses() {
        return statuses("select u.accountStatus, count(u) from User u where u.role.roleName = 'Candidate' "
                + "group by u.accountStatus", Map.of());
    }

    public long activeJobPostings(LocalDateTime now) {
        return count("select count(j) from JobPosting j where j.postingStatus = 'Published' "
                + "and (j.applicationDeadline is null or j.applicationDeadline >= :now)", Map.of("now", now));
    }

    public long applications() {
        return count("select count(a) from Application a", Map.of());
    }

    public List<StatusCount> applicationStages() {
        return statuses("select a.applicationStatus, count(distinct a.candidate.candidateId) from Application a "
                + "group by a.applicationStatus order by a.applicationStatus", Map.of());
    }

    public long upcomingInterviews(LocalDateTime now) {
        return count("select count(i) from InterviewSchedule i where i.startTime >= :now "
                + "and i.interviewStatus in ('Scheduled', 'Rescheduled')", Map.of("now", now));
    }

    public long newApplicationsAwaitingHrReview() {
        return count("select count(a) from Application a where a.applicationStatus = 'Applied' "
                + "and not exists (select r.reviewId from ApplicationReview r "
                + "where r.application = a and r.reviewerRole = 'HR')", Map.of());
    }

    public long ownRequisitions(int userId) {
        return count("select count(r) from JobRequisition r where r.hiringManager.userId = :userId",
                Map.of("userId", userId));
    }

    public long departmentRequisitions(int departmentId) {
        return count("select count(r) from JobRequisition r where r.department.departmentId = :departmentId",
                Map.of("departmentId", departmentId));
    }

    public long ownPendingRequisitions(int userId) {
        return count("select count(r) from JobRequisition r where r.hiringManager.userId = :userId "
                + "and r.approvalStatus = 'Pending_Director'", Map.of("userId", userId));
    }

    public long candidatesForOwnRequisitions(int userId) {
        return count("select count(distinct a.candidate.candidateId) from Application a "
                + "where a.jobPosting.requisition.hiringManager.userId = :userId", Map.of("userId", userId));
    }

    public long upcomingInterviewsForHiringManager(int userId, LocalDateTime now) {
        return count("select count(i) from InterviewSchedule i "
                + "where i.application.jobPosting.requisition.hiringManager.userId = :userId "
                + "and i.startTime >= :now and i.interviewStatus in ('Scheduled', 'Rescheduled')",
                Map.of("userId", userId, "now", now));
    }

    public long offersNeedingHiringManagerAction(int userId) {
        return count("select count(o) from OfferProposal o where o.proposedBy.userId = :userId "
                + "and o.offerStatus in ('Draft', 'Approved')", Map.of("userId", userId));
    }

    public long requisitionsAwaitingDirector() {
        return count("select count(r) from JobRequisition r where r.approvalStatus = 'Pending_Director'", Map.of());
    }

    public long offersAwaitingDirector() {
        return count("select count(o) from OfferProposal o where o.offerStatus = 'Pending_Director'", Map.of());
    }

    public List<ApprovalActivity> recentDirectorActivity(int userId) {
        List<ApprovalActivity> activity = new ArrayList<>();
        entityManager.createQuery("select r.title, a.status, a.approvalDate from RequisitionApproval a "
                + "join a.requisition r where a.director.userId = :userId order by a.approvalDate desc",
                Object[].class)
                .setParameter("userId", userId)
                .setMaxResults(5)
                .getResultList()
                .forEach(row -> activity.add(new ApprovalActivity("Job Requisition", (String) row[0],
                        (String) row[1], (LocalDateTime) row[2])));
        entityManager.createQuery("select o.offeredPositionTitle, a.status, a.approvedAt from OfferApproval a "
                + "join a.offerProposal o where a.director.userId = :userId order by a.approvedAt desc",
                Object[].class)
                .setParameter("userId", userId)
                .setMaxResults(5)
                .getResultList()
                .forEach(row -> activity.add(new ApprovalActivity("Offer", (String) row[0],
                        (String) row[1], (LocalDateTime) row[2])));
        return activity.stream().sorted(Comparator.comparing(ApprovalActivity::at).reversed()).limit(5).toList();
    }

    public long upcomingAssignedInterviews(int userId, LocalDateTime now) {
        return count("select count(p) from InterviewPanel p where p.interviewer.userId = :userId "
                + "and p.interviewSchedule.startTime >= :now "
                + "and p.interviewSchedule.interviewStatus in ('Scheduled', 'Rescheduled')",
                Map.of("userId", userId, "now", now));
    }

    public long assignedCandidatesForInterviewer(int userId) {
        return count("select count(distinct p.interviewSchedule.application.candidate.candidateId) "
                + "from InterviewPanel p where p.interviewer.userId = :userId", Map.of("userId", userId));
    }

    public long pendingInterviewEvaluations(int userId) {
        return count("select count(p) from InterviewPanel p where p.interviewer.userId = :userId "
                + "and p.interviewSchedule.interviewStatus = 'Completed' and not exists "
                + "(select e.evaluationId from InterviewEvaluation e "
                + "where e.interviewSchedule = p.interviewSchedule and e.interviewer.userId = :userId)",
                Map.of("userId", userId));
    }

    public long candidateApplications(int userId) {
        return count("select count(a) from Application a where a.candidate.account.userId = :userId",
                Map.of("userId", userId));
    }

    public List<StatusCount> candidateApplicationStages(int userId) {
        return statuses("select a.applicationStatus, count(a) from Application a "
                + "where a.candidate.account.userId = :userId "
                + "group by a.applicationStatus order by a.applicationStatus", Map.of("userId", userId));
    }

    public long candidateUpcomingInterviews(int userId, LocalDateTime now) {
        return count("select count(i) from InterviewSchedule i "
                + "where i.application.candidate.account.userId = :userId "
                + "and i.startTime >= :now and i.interviewStatus in ('Scheduled', 'Rescheduled')",
                Map.of("userId", userId, "now", now));
    }

    public List<StatusCount> candidateOfferStatuses(int userId) {
        return statuses("select o.offerStatus, count(o) from OfferProposal o "
                + "where o.application.candidate.account.userId = :userId "
                + "and o.offerStatus in " + CANDIDATE_VISIBLE_OFFER_STATUSES + " "
                + "group by o.offerStatus order by o.offerStatus", Map.of("userId", userId));
    }

    /** Offers that were officially sent to the candidate and still wait for their answer. */
    public long candidateOffersAwaitingResponse(int userId) {
        return count("select count(o) from OfferProposal o "
                + "where o.application.candidate.account.userId = :userId "
                + "and o.offerStatus = 'Sent_Candidate'", Map.of("userId", userId));
    }

    /**
     * Latest applications of the candidate. Only the title, submission date and raw
     * status are selected; scores and internal reviews are never read here (GBR-02).
     */
    public List<CandidateApplicationRow> candidateRecentApplications(int userId, int limit) {
        return entityManager.createQuery("select a.applicationId, j.postingTitle, a.submissionDate, "
                + "a.applicationStatus from Application a join a.jobPosting j "
                + "where a.candidate.account.userId = :userId "
                + "order by a.submissionDate desc, a.applicationId desc", Object[].class)
                .setParameter("userId", userId)
                .setMaxResults(limit)
                .getResultList().stream()
                .map(row -> new CandidateApplicationRow((Integer) row[0], (String) row[1],
                        (LocalDateTime) row[2], (String) row[3]))
                .toList();
    }

    public List<CandidateInterviewRow> candidateNextInterviews(int userId, LocalDateTime now, int limit) {
        return entityManager.createQuery("select j.postingTitle, i.startTime, i.endTime, i.interviewFormat, "
                + "i.locationOrLink from InterviewSchedule i join i.application a join a.jobPosting j "
                + "where a.candidate.account.userId = :userId and i.startTime >= :now "
                + "and i.interviewStatus in ('Scheduled', 'Rescheduled') order by i.startTime asc",
                Object[].class)
                .setParameter("userId", userId)
                .setParameter("now", now)
                .setMaxResults(limit)
                .getResultList().stream()
                .map(row -> new CandidateInterviewRow((String) row[0], (LocalDateTime) row[1],
                        (LocalDateTime) row[2], (String) row[3], (String) row[4]))
                .toList();
    }

    /** Official offer data only; internal negotiation notes and approvals are not selected. */
    public List<CandidateOfferRow> candidateOffers(int userId, int limit) {
        return entityManager.createQuery("select o.offerId, o.offeredPositionTitle, o.proposedSalary, "
                + "o.expectedStartDate, o.offerStatus, o.probationSalary, o.workLocation, o.benefitsPackage "
                + "from OfferProposal o "
                + "where o.application.candidate.account.userId = :userId "
                + "and o.offerStatus in " + CANDIDATE_VISIBLE_OFFER_STATUSES + " "
                + "order by coalesce(o.updatedAt, o.createdAt) desc", Object[].class)
                .setParameter("userId", userId)
                .setMaxResults(limit)
                .getResultList().stream()
                .map(row -> new CandidateOfferRow((Integer) row[0], (String) row[1],
                        (BigDecimal) row[2], (LocalDate) row[3], (String) row[4],
                        (BigDecimal) row[5], (String) row[6], (String) row[7]))
                .toList();
    }

    private long count(String jpql, Map<String, ?> parameters) {
        var query = entityManager.createQuery(jpql, Long.class);
        parameters.forEach(query::setParameter);
        return query.getSingleResult();
    }

    private List<StatusCount> statuses(String jpql, Map<String, ?> parameters) {
        var query = entityManager.createQuery(jpql, Object[].class);
        parameters.forEach(query::setParameter);
        return query.getResultList().stream()
                .map(row -> new StatusCount((String) row[0], (Long) row[1]))
                .toList();
    }

    public record StatusCount(String status, long count) {
    }

    public record ApprovalActivity(String type, String title, String status, LocalDateTime at) {
    }

    public record CandidateApplicationRow(int applicationId, String jobTitle, LocalDateTime submittedAt,
            String rawStatus) {
    }

    public record CandidateInterviewRow(String jobTitle, LocalDateTime startTime, LocalDateTime endTime,
            String format, String locationOrLink) {
    }

    public record CandidateOfferRow(int offerId, String jobTitle, BigDecimal salary, LocalDate startDate,
            String rawStatus, BigDecimal probationSalary, String workLocation, String benefits) {
    }
}
