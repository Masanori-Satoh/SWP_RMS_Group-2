package com.group2.rms.dashboard.internal.repository;

import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.requisition.service.RequisitionAccess;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.group2.rms.candidate.domain.repository.ApplicationRepository.FORMAL_FILTER;

/*
 * REPOSITORY: các danh sách ngắn trên dashboard nội bộ (chỉ đọc). Hàm đếm nằm ở InternalMetricsRepository.
 * Mỗi danh sách là 1 câu truy vấn (hoặc 1 câu + 1 câu IN cho cả danh sách), không tra từng dòng (§15.3).
 */
@Repository
public class DashboardListRepository {

    /** Phần SELECT / FROM chung của mọi danh sách lịch phỏng vấn (khớp thứ tự cột của InterviewRow). */
    private static final String INTERVIEW_SELECT = "select i.interviewId, i.startTime, i.endTime, "
            + "u.fullName, j.postingTitle, i.interviewFormat, i.locationOrLink, i.interviewStatus, "
            + "(select count(p) from InterviewPanel p where p.interviewSchedule = i) "
            + "from InterviewSchedule i join i.application a join a.jobPosting j "
            + "join a.candidate c left join c.account u ";

    private final EntityManager entityManager;

    public DashboardListRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // ===== Hiring Manager =====

    /** Yêu cầu bị từ chối (xếp trước) và bản nháp của HM, mới cập nhật lên đầu. */
    public List<AttentionRow> requisitionsNeedingAttention(int userId) {
        List<Object[]> rows = entityManager.createQuery("select r.requisitionId, r.requisitionCode, r.title, "
                        + "r.approvalStatus, r.submittedAt from JobRequisition r "
                        + "where r.hiringManager.userId = :userId and r.approvalStatus in (:draft, :rejected) "
                        + "order by case when r.approvalStatus = :rejected then 0 else 1 end, "
                        + "coalesce(r.updatedAt, r.createdAt) desc, r.requisitionId desc", Object[].class)
                .setParameter("userId", userId)
                .setParameter("draft", RequisitionAccess.STATUS_DRAFT)
                .setParameter("rejected", RequisitionAccess.STATUS_REJECTED)
                .getResultList();

        // Lý do từ chối lấy bằng 1 câu cho mọi yêu cầu bị từ chối, không tra từng yêu cầu (§15.3).
        List<Integer> rejectedIds = new ArrayList<>();
        for (Object[] row : rows) {
            if (RequisitionAccess.STATUS_REJECTED.equals(row[3])) {
                rejectedIds.add((Integer) row[0]);
            }
        }
        Map<Integer, String> rejectComments = latestRejectComments(rejectedIds);

        List<AttentionRow> result = new ArrayList<>();
        for (Object[] row : rows) {
            Integer requisitionId = (Integer) row[0];
            result.add(new AttentionRow(requisitionId, (String) row[1], (String) row[2], (String) row[3],
                    (LocalDateTime) row[4], rejectComments.get(requisitionId)));
        }
        return result;
    }

    /** Yêu cầu đang tuyển trong phạm vi HM, kèm số đã nhận việc / đang phỏng vấn / đã có offer. */
    public List<ProgressRow> hiringProgress(int userId, Integer departmentId) {
        List<Object[]> rows = entityManager.createQuery("select r.requisitionId, r.requisitionCode, r.title, "
                        + "r.numberOfPositions, "
                        + "sum(case when a.applicationStatus = 'Hired' then 1 else 0 end), "
                        + "sum(case when a.applicationStatus = 'Interviewing' then 1 else 0 end), "
                        + "sum(case when a.applicationStatus = 'Offered' then 1 else 0 end) "
                        + "from JobRequisition r left join r.department d left join d.manager m "
                        + "left join JobPosting j on j.requisition = r "
                        + "left join Application a on a.jobPosting = j "
                        + "where r.approvalStatus = :approved and (r.hiringManager.userId = :userId "
                        + "or m.userId = :userId or d.departmentId = :departmentId) "
                        + "group by r.requisitionId, r.requisitionCode, r.title, r.numberOfPositions "
                        + "order by r.requisitionId desc", Object[].class)
                .setParameter("approved", RequisitionAccess.STATUS_APPROVED)
                .setParameter("userId", userId)
                .setParameter("departmentId", departmentId)
                .getResultList();
        List<ProgressRow> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new ProgressRow((Integer) row[0], (String) row[1], (String) row[2], (Integer) row[3],
                    toLong(row[4]), toLong(row[5]), toLong(row[6])));
        }
        return result;
    }

    // ===== Lịch phỏng vấn =====

    /** Lịch còn hiệu lực của phòng HM thuộc hoặc làm trưởng phòng, bắt đầu trong [from, to), sớm nhất lên đầu. */
    public List<InterviewRow> departmentInterviews(int userId, Integer departmentId, LocalDateTime from,
            LocalDateTime to, int limit) {
        TypedQuery<Object[]> query = entityManager.createQuery(INTERVIEW_SELECT
                + "join j.requisition r left join r.department d left join d.manager m "
                + "where (m.userId = :userId or d.departmentId = :departmentId) "
                + "and i.startTime >= :from and i.startTime < :to "
                + "and i.interviewStatus in " + InternalMetricsRepository.ACTIVE_INTERVIEW_STATUSES + " "
                + "order by i.startTime asc, i.interviewId asc", Object[].class);
        query.setParameter("userId", userId);
        query.setParameter("departmentId", departmentId);
        query.setParameter("from", from);
        query.setParameter("to", to);
        query.setMaxResults(limit);
        return toInterviewRows(query.getResultList());
    }

    /** Lịch còn hiệu lực của toàn công ty (HR), bắt đầu trong [from, to), sớm nhất lên đầu. */
    public List<InterviewRow> allInterviews(LocalDateTime from, LocalDateTime to, int limit) {
        TypedQuery<Object[]> query = entityManager.createQuery(INTERVIEW_SELECT
                + "where i.startTime >= :from and i.startTime < :to "
                + "and i.interviewStatus in " + InternalMetricsRepository.ACTIVE_INTERVIEW_STATUSES + " "
                + "order by i.startTime asc, i.interviewId asc", Object[].class);
        query.setParameter("from", from);
        query.setParameter("to", to);
        query.setMaxResults(limit);
        return toInterviewRows(query.getResultList());
    }

    /** Lịch người này có tên trong hội đồng, bắt đầu trong [from, to), trừ lịch đã hủy. Sớm nhất lên đầu. */
    public List<InterviewRow> panelInterviews(int userId, LocalDateTime from, LocalDateTime to) {
        TypedQuery<Object[]> query = entityManager.createQuery(INTERVIEW_SELECT
                + "where exists (select p.interviewer.userId from InterviewPanel p "
                + "where p.interviewSchedule = i and p.interviewer.userId = :userId) "
                + "and i.startTime >= :from and i.startTime < :to and i.interviewStatus <> 'Cancelled' "
                + "order by i.startTime asc, i.interviewId asc", Object[].class);
        query.setParameter("userId", userId);
        query.setParameter("from", from);
        query.setParameter("to", to);
        return toInterviewRows(query.getResultList());
    }

    /** Buổi đã hoàn tất có người này trong hội đồng nhưng chưa có phiếu của họ; kết thúc sớm nhất lên đầu. */
    public List<EvaluationRow> pendingEvaluations(int userId) {
        List<Object[]> rows = entityManager.createQuery("select i.interviewId, u.fullName, j.postingTitle, i.endTime "
                        + "from InterviewPanel p join p.interviewSchedule i join i.application a "
                        + "join a.jobPosting j join a.candidate c left join c.account u "
                        + "where p.interviewer.userId = :userId and i.interviewStatus = 'Completed' "
                        + "and not exists (select e.evaluationId from InterviewEvaluation e "
                        + "where e.interviewSchedule = i and e.interviewer.userId = :userId) "
                        + "order by i.endTime asc, i.interviewId asc", Object[].class)
                .setParameter("userId", userId)
                .getResultList();
        List<EvaluationRow> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new EvaluationRow((Long) row[0], (String) row[1], (String) row[2], (LocalDateTime) row[3]));
        }
        return result;
    }

    // ===== HR =====

    /** Yêu cầu đã duyệt nhưng chưa có tin nháp / đang đăng / tạm dừng: HR cần tạo tin. Chờ lâu nhất lên đầu. */
    public List<AwaitingPostingRow> requisitionsAwaitingPosting() {
        List<Object[]> rows = entityManager.createQuery("select r.requisitionId, r.requisitionCode, r.title, "
                        + "coalesce(r.updatedAt, r.createdAt) from JobRequisition r "
                        + "where r.approvalStatus = :approved and not exists (select j.jobPostingId from JobPosting j "
                        + "where j.requisition = r and j.postingStatus in :openStatuses) "
                        + "order by coalesce(r.updatedAt, r.createdAt) asc, r.requisitionId asc", Object[].class)
                .setParameter("approved", RequisitionAccess.STATUS_APPROVED)
                .setParameter("openStatuses", JobPostingAccess.OPEN_STATUSES)
                .getResultList();
        List<AwaitingPostingRow> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new AwaitingPostingRow((Integer) row[0], (String) row[1], (String) row[2],
                    (LocalDateTime) row[3]));
        }
        return result;
    }

    // ===== Director =====

    /** Yêu cầu Giám đốc đang xem được quyền duyệt, loại yêu cầu do chính người đó tạo. */
    public List<PendingRequisitionRow> pendingRequisitions(int viewerId) {
        List<Object[]> rows = entityManager.createQuery("select r.requisitionId, r.requisitionCode, r.title, "
                        + "d.departmentName, hm.fullName, r.numberOfPositions, r.hiringType, r.recruitmentRound, "
                        + "coalesce(r.submittedAt, r.updatedAt, r.createdAt) "
                        + "from JobRequisition r left join r.department d left join r.hiringManager hm "
                        + "where r.approvalStatus = :pending and r.hiringManager.userId <> :viewerId "
                        + "order by coalesce(r.submittedAt, r.updatedAt, r.createdAt) asc, r.requisitionId asc",
                        Object[].class)
                .setParameter("pending", RequisitionAccess.STATUS_PENDING_DIRECTOR)
                .setParameter("viewerId", viewerId)
                .getResultList();
        List<PendingRequisitionRow> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new PendingRequisitionRow((Integer) row[0], (String) row[1], (String) row[2],
                    (String) row[3], (String) row[4], (Integer) row[5], (String) row[6], (Integer) row[7],
                    (LocalDateTime) row[8]));
        }
        return result;
    }

    /** Mọi đề nghị tuyển dụng đang chờ Giám đốc duyệt, chờ lâu nhất lên đầu. */
    public List<PendingOfferRow> pendingOffers() {
        List<Object[]> rows = entityManager.createQuery("select o.offerId, u.fullName, o.offeredPositionTitle, "
                        + "o.proposedSalary, coalesce(o.updatedAt, o.createdAt) "
                        + "from OfferProposal o join o.application a join a.candidate c left join c.account u "
                        + "where o.offerStatus = :pending "
                        + "order by coalesce(o.updatedAt, o.createdAt) asc, o.offerId asc", Object[].class)
                .setParameter("pending", InternalMetricsRepository.OFFER_PENDING_DIRECTOR)
                .getResultList();
        List<PendingOfferRow> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new PendingOfferRow((Integer) row[0], (String) row[1], (String) row[2],
                    (BigDecimal) row[3], (LocalDateTime) row[4]));
        }
        return result;
    }

    /** Số yêu cầu chờ duyệt và đang tuyển của từng phòng ban; nhiều nhất lên đầu. */
    public List<DepartmentRequisitionRow> openRequisitionsByDepartment() {
        List<Object[]> rows = entityManager.createQuery("select d.departmentName, "
                        + "sum(case when r.approvalStatus = :pending then 1 else 0 end), "
                        + "sum(case when r.approvalStatus = :approved then 1 else 0 end) "
                        + "from JobRequisition r join r.department d "
                        + "where r.approvalStatus in (:pending, :approved) "
                        + "group by d.departmentName order by count(r) desc, d.departmentName asc", Object[].class)
                .setParameter("pending", RequisitionAccess.STATUS_PENDING_DIRECTOR)
                .setParameter("approved", RequisitionAccess.STATUS_APPROVED)
                .getResultList();
        List<DepartmentRequisitionRow> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new DepartmentRequisitionRow((String) row[0], toLong(row[1]), toLong(row[2])));
        }
        return result;
    }

    /** Ý kiến của lần từ chối gần nhất cho từng yêu cầu. */
    private Map<Integer, String> latestRejectComments(List<Integer> requisitionIds) {
        Map<Integer, String> comments = new HashMap<>();
        if (requisitionIds.isEmpty()) {
            return comments;
        }
        List<Object[]> rows = entityManager.createQuery("select a.requisition.requisitionId, a.comments "
                        + "from RequisitionApproval a where a.requisition.requisitionId in :ids "
                        + "and a.status = :rejected order by a.approvalDate desc, a.approvalId desc", Object[].class)
                .setParameter("ids", requisitionIds)
                .setParameter("rejected", RequisitionAccess.STATUS_REJECTED)
                .getResultList();
        for (Object[] row : rows) {
            // Rule: dòng đầu tiên của mỗi yêu cầu là lần từ chối mới nhất, các dòng sau bỏ qua.
            comments.putIfAbsent((Integer) row[0], (String) row[1]);
        }
        return comments;
    }

    private List<InterviewRow> toInterviewRows(List<Object[]> rows) {
        List<InterviewRow> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new InterviewRow((Long) row[0], (LocalDateTime) row[1], (LocalDateTime) row[2],
                    (String) row[3], (String) row[4], (String) row[5], (String) row[6], (String) row[7],
                    toLong(row[8])));
        }
        return result;
    }

    /** SUM / COUNT trả về Long (hoặc Integer tùy Hibernate); không có dòng nào thì null → 0. */
    private static long toLong(Object value) {
        if (value == null) {
            return 0;
        }
        return ((Number) value).longValue();
    }

    public record AttentionRow(int requisitionId, String requisitionCode, String title, String approvalStatus,
            LocalDateTime submittedAt, String rejectComment) {
    }

    public record ProgressRow(int requisitionId, String requisitionCode, String title, int numberOfPositions,
            long hired, long interviewing, long offered) {
    }

    public record InterviewRow(long interviewId, LocalDateTime startTime, LocalDateTime endTime,
            String candidateName, String positionTitle, String interviewFormat, String locationOrLink,
            String interviewStatus, long panelSize) {
    }

    public record EvaluationRow(long interviewId, String candidateName, String positionTitle,
            LocalDateTime endTime) {
    }

    public record AwaitingPostingRow(int requisitionId, String requisitionCode, String title,
            LocalDateTime waitingSince) {
    }

    public record PendingRequisitionRow(int requisitionId, String requisitionCode, String title,
            String departmentName, String requesterName, int numberOfPositions, String hiringType,
            Integer recruitmentRound, LocalDateTime waitingSince) {
    }

    public record PendingOfferRow(int offerId, String candidateName, String positionTitle,
            BigDecimal proposedSalary, LocalDateTime waitingSince) {
    }

    public record DepartmentRequisitionRow(String departmentName, long pending, long approved) {
    }
}
