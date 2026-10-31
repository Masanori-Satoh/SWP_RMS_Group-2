package com.group2.rms.dashboard.internal.repository;

import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.requisition.service.RequisitionAccess;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    public static final String OFFER_DECLINED = "Declined";

    private final EntityManager entityManager;

    public InternalMetricsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // ===== Hiring Manager =====

    /** Yêu cầu của phòng ban HM thuộc hoặc quản lý đã có ít nhất một tin đang đăng (Published). */
    public long countPostedDepartmentRequisitions(int userId, Integer departmentId) {
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

    /** Buổi phỏng vấn còn hiệu lực của phòng HM thuộc hoặc làm trưởng phòng, bắt đầu trong [from, to). */
    public long countUpcomingDepartmentInterviews(int userId, Integer departmentId, LocalDateTime from,
            LocalDateTime to) {
        var query = entityManager.createQuery("select count(i) from InterviewSchedule i "
                + "join i.application a join a.jobPosting j join j.requisition r "
                + "left join r.department d left join d.manager m "
                + "where (m.userId = :userId or d.departmentId = :departmentId) "
                + "and i.startTime >= :from and i.startTime < :to "
                + "and i.interviewStatus in " + ACTIVE_INTERVIEW_STATUSES, Long.class);
        query.setParameter("userId", userId);
        query.setParameter("departmentId", departmentId);
        query.setParameter("from", from);
        query.setParameter("to", to);
        return query.getSingleResult();
    }

    /** Yêu cầu chờ Giám đốc duyệt trong phạm vi HM, cùng phạm vi màn /requisitions (RequisitionListService#scope). */
    public long countPendingRequisitionsInScope(int userId, Integer departmentId) {
        var query = entityManager.createQuery("select count(r) from JobRequisition r "
                + "left join r.department d left join d.manager m "
                + "where r.approvalStatus = :pending and (r.hiringManager.userId = :userId "
                + "or m.userId = :userId or d.departmentId = :departmentId)", Long.class);
        query.setParameter("pending", RequisitionAccess.STATUS_PENDING_DIRECTOR);
        query.setParameter("userId", userId);
        query.setParameter("departmentId", departmentId);
        return query.getSingleResult();
    }

    // ===== HR =====

    /** Tin đang đăng có ngày đăng trong [from, to), tính trên toàn công ty. */
    public long countPublishedPostingsBetween(LocalDateTime from, LocalDateTime to) {
        return count("select count(j) from JobPosting j where j.postingStatus = :published "
                + "and j.postingDate >= :from and j.postingDate < :to",
                Map.of("published", JobPostingAccess.STATUS_PUBLISHED, "from", from, "to", to));
    }

    /** Ngày nộp của các hồ sơ chính thức trong [from, to), để chia theo tuần. */
    public List<LocalDateTime> applicationDates(LocalDateTime from, LocalDateTime to) {
        return entityManager.createQuery("select a.submissionDate from Application a where " + FORMAL_FILTER
                + " and a.submissionDate >= :from and a.submissionDate < :to", LocalDateTime.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    /** Rule (D2): mốc bắt đầu chờ của hồ sơ chưa kết thúc = UpdatedAt, chưa cập nhật thì CreatedAt. */
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

    // ===== Director =====

    /** Hồ sơ chuyển sang Hired (lần cập nhật cuối) trong [from, to). */
    public long countHiredBetween(LocalDateTime from, LocalDateTime to) {
        return count("select count(a) from Application a where a.applicationStatus = :hired "
                + "and a.updatedAt >= :from and a.updatedAt < :to and " + FORMAL_FILTER,
                Map.of("hired", APPLICATION_HIRED, "from", from, "to", to));
    }

    // ===== Interviewer =====

    /** Ứng viên được HM chốt Passed trong [from, to) ở buổi có người này trong hội đồng. */
    public long countPassedInterviewCandidatesBetween(int userId, LocalDateTime from, LocalDateTime to) {
        return count("select count(distinct r.interviewSchedule.application.candidate.candidateId) "
                + "from InterviewFinalResult r where r.finalDecision = :passed "
                + "and r.approvedAt >= :from and r.approvedAt < :to "
                + "and exists (select p from InterviewPanel p where p.interviewSchedule = r.interviewSchedule "
                + "and p.interviewer.userId = :userId)",
                Map.of("passed", "Passed", "from", from, "to", to, "userId", userId));
    }

    private long count(String jpql, Map<String, ?> parameters) {
        var query = entityManager.createQuery(jpql, Long.class);
        parameters.forEach(query::setParameter);
        return query.getSingleResult();
    }
}
