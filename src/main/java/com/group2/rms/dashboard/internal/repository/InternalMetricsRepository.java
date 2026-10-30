package com.group2.rms.dashboard.internal.repository;

import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.requisition.service.RequisitionAccess;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.group2.rms.candidate.domain.repository.ApplicationRepository.FORMAL_FILTER;

/*
 * REPOSITORY: số đếm cho dashboard nội bộ (HR, Hiring Manager, Director, Interviewer). Chỉ đọc.
 * Danh sách ngắn (việc cần làm, lịch, bảng) nằm ở DashboardListRepository.
 */
@Repository
public class InternalMetricsRepository {

    /** Lịch phỏng vấn chưa diễn ra và chưa bị hủy (InterviewSchedule.InterviewStatus). */
    public static final String ACTIVE_INTERVIEW_STATUSES = "('Scheduled', 'Rescheduled')";
    /** Giá trị Application.ApplicationStatus và OfferProposal.OfferStatus dùng trong truy vấn (khớp CHECK của db.sql). */
    public static final String APPLICATION_HIRED = "Hired";
    public static final String APPLICATION_REJECTED = "Rejected";
    public static final String OFFER_PENDING_DIRECTOR = "Pending_Director";
    public static final String OFFER_ACCEPTED = "Accepted";
    public static final String OFFER_DECLINED = "Declined";

    private final EntityManager entityManager;

    public InternalMetricsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // ===== Hiring Manager =====

    /** Yêu cầu của phòng ban HM thuộc hoặc quản lý đã có ít nhất một tin đang đăng (Published). */
    public long postedDepartmentRequisitions(int userId, Integer departmentId) {
        var query = entityManager.createQuery("select count(r) from JobRequisition r "
                + "left join r.department d left join d.manager m "
                + "where (m.userId = :userId or d.departmentId = :departmentId) "
                + "and exists (select j from JobPosting j where j.requisition = r and j.postingStatus = :published)",
                Long.class);
        query.setParameter("userId", userId);
        query.setParameter("departmentId", departmentId);
        query.setParameter("published", JobPostingAccess.STATUS_PUBLISHED);
        return query.getSingleResult();
    }

    /** Buổi phỏng vấn còn hiệu lực của phòng ban, bắt đầu trong [from, to). Không có phòng ban thì 0. */
    public long upcomingDepartmentInterviews(Integer departmentId, LocalDateTime from, LocalDateTime to) {
        if (departmentId == null) {
            return 0;
        }
        return count("select count(i) from InterviewSchedule i "
                + "where i.application.jobPosting.requisition.department.departmentId = :departmentId "
                + "and i.startTime >= :from and i.startTime < :to "
                + "and i.interviewStatus in " + ACTIVE_INTERVIEW_STATUSES,
                Map.of("departmentId", departmentId, "from", from, "to", to));
    }

    public long ownPendingRequisitions(int userId) {
        return count("select count(r) from JobRequisition r where r.hiringManager.userId = :userId "
                + "and r.approvalStatus = :pending",
                Map.of("userId", userId, "pending", RequisitionAccess.STATUS_PENDING_DIRECTOR));
    }

    /** Ngày gửi duyệt sớm nhất trong các yêu cầu của HM đang chờ Giám đốc; null khi không có yêu cầu nào chờ. */
    public LocalDateTime oldestOwnPendingSubmittedAt(int userId) {
        return entityManager.createQuery("select min(r.submittedAt) from JobRequisition r "
                + "where r.hiringManager.userId = :userId and r.approvalStatus = :pending", LocalDateTime.class)
                .setParameter("userId", userId)
                .setParameter("pending", RequisitionAccess.STATUS_PENDING_DIRECTOR)
                .getSingleResult();
    }

    // ===== HR =====

    /** Tin đang đăng có ngày đăng trong [from, to), tính trên toàn công ty. */
    public long publishedPostingsBetween(LocalDateTime from, LocalDateTime to) {
        return count("select count(j) from JobPosting j where j.postingStatus = :published "
                + "and j.postingDate >= :from and j.postingDate < :to",
                Map.of("published", JobPostingAccess.STATUS_PUBLISHED, "from", from, "to", to));
    }

    /** Hồ sơ chính thức nộp trong [from, to), toàn công ty. */
    public long applicationsBetween(LocalDateTime from, LocalDateTime to) {
        return count("select count(a) from Application a where a.submissionDate >= :from "
                + "and a.submissionDate < :to and " + FORMAL_FILTER, Map.of("from", from, "to", to));
    }

    /** Ngày nộp của các hồ sơ chính thức trong [from, to), để chia theo tuần. */
    public List<LocalDateTime> applicationDates(LocalDateTime from, LocalDateTime to) {
        return entityManager.createQuery("select a.submissionDate from Application a where " + FORMAL_FILTER
                + " and a.submissionDate >= :from and a.submissionDate < :to", LocalDateTime.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    /**
     * Mốc bắt đầu chờ của mọi hồ sơ chính thức chưa kết thúc: applicationId → lần cập nhật gần nhất.
     * Rule (D2): DB không lưu lịch sử trạng thái, nên dùng UpdatedAt (chưa cập nhật lần nào thì CreatedAt).
     */
    public Map<Integer, LocalDateTime> applicationWaitingSince() {
        List<Object[]> rows = entityManager.createQuery("select a.applicationId, coalesce(a.updatedAt, a.createdAt) "
                + "from Application a where a.applicationStatus not in (:hired, :rejected) and " + FORMAL_FILTER,
                Object[].class)
                .setParameter("hired", APPLICATION_HIRED)
                .setParameter("rejected", APPLICATION_REJECTED)
                .getResultList();
        Map<Integer, LocalDateTime> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Integer) row[0], (LocalDateTime) row[1]);
        }
        return result;
    }

    /** Đề nghị chuyển sang trạng thái offerStatus (lần cập nhật cuối) trong [from, to). */
    public long offersWithStatusBetween(String offerStatus, LocalDateTime from, LocalDateTime to) {
        return count("select count(o) from OfferProposal o where o.offerStatus = :status "
                + "and o.updatedAt >= :from and o.updatedAt < :to",
                Map.of("status", offerStatus, "from", from, "to", to));
    }

    // ===== Director =====

    /** Hồ sơ chuyển sang Hired (lần cập nhật cuối) trong [from, to). */
    public long hiredBetween(LocalDateTime from, LocalDateTime to) {
        return count("select count(a) from Application a where a.applicationStatus = :hired "
                + "and a.updatedAt >= :from and a.updatedAt < :to and " + FORMAL_FILTER,
                Map.of("hired", APPLICATION_HIRED, "from", from, "to", to));
    }

    /** 5 quyết định gần nhất của Giám đốc, gộp yêu cầu tuyển dụng và đề nghị tuyển dụng. */
    public List<ApprovalActivity> recentDirectorActivity(int userId, int limit) {
        List<ApprovalActivity> activity = new ArrayList<>();
        List<Object[]> requisitionRows = entityManager.createQuery("select r.title, a.status, a.approvalDate "
                + "from RequisitionApproval a join a.requisition r where a.director.userId = :userId "
                + "order by a.approvalDate desc", Object[].class)
                .setParameter("userId", userId)
                .setMaxResults(limit)
                .getResultList();
        for (Object[] row : requisitionRows) {
            activity.add(new ApprovalActivity(ApprovalActivity.REQUISITION, (String) row[0],
                    (String) row[1], (LocalDateTime) row[2]));
        }
        List<Object[]> offerRows = entityManager.createQuery("select o.offeredPositionTitle, a.status, a.approvedAt "
                + "from OfferApproval a join a.offerProposal o where a.director.userId = :userId "
                + "order by a.approvedAt desc", Object[].class)
                .setParameter("userId", userId)
                .setMaxResults(limit)
                .getResultList();
        for (Object[] row : offerRows) {
            activity.add(new ApprovalActivity(ApprovalActivity.OFFER, (String) row[0],
                    (String) row[1], (LocalDateTime) row[2]));
        }
        // Rule: mới nhất lên đầu; quyết định thiếu ngày (dữ liệu cũ) xếp cuối.
        activity.sort(Comparator.comparing(ApprovalActivity::at, Comparator.nullsFirst(Comparator.naturalOrder()))
                .reversed());
        if (activity.size() > limit) {
            return List.copyOf(activity.subList(0, limit));
        }
        return activity;
    }

    // ===== Interviewer =====

    /** Ứng viên được HM chốt Passed trong [from, to) ở buổi có người này trong hội đồng. */
    public long passedInterviewCandidatesBetween(int userId, LocalDateTime from, LocalDateTime to) {
        return count("select count(distinct r.interviewSchedule.application.candidate.candidateId) "
                + "from InterviewFinalResult r where r.finalDecision = :passed "
                + "and r.approvedAt >= :from and r.approvedAt < :to "
                + "and exists (select p from InterviewPanel p where p.interviewSchedule = r.interviewSchedule "
                + "and p.interviewer.userId = :userId)",
                Map.of("passed", "Passed", "from", from, "to", to, "userId", userId));
    }

    /** Số phiếu đánh giá người này đã gửi từ mốc since (null = từ trước tới nay). */
    public long evaluationsSubmittedSince(int userId, LocalDateTime since) {
        if (since == null) {
            return count("select count(e) from InterviewEvaluation e where e.interviewer.userId = :userId",
                    Map.of("userId", userId));
        }
        return count("select count(e) from InterviewEvaluation e where e.interviewer.userId = :userId "
                + "and e.evaluatedAt >= :since", Map.of("userId", userId, "since", since));
    }

    private long count(String jpql, Map<String, ?> parameters) {
        var query = entityManager.createQuery(jpql, Long.class);
        parameters.forEach(query::setParameter);
        return query.getSingleResult();
    }

    /** Một quyết định của Giám đốc; type = REQUISITION hoặc OFFER. */
    public record ApprovalActivity(String type, String title, String status, LocalDateTime at) {
        public static final String REQUISITION = "Job Requisition";
        public static final String OFFER = "Offer";
    }
}
