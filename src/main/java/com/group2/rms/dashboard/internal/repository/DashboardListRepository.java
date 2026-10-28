package com.group2.rms.dashboard;

import com.group2.rms.requisition.service.RequisitionAccess;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * REPOSITORY: các danh sách ngắn trên dashboard (chỉ đọc). Hàm đếm nằm ở DashboardMetricsRepository.
 */
@Repository
public class DashboardListRepository {

    private final EntityManager entityManager;

    public DashboardListRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Yêu cầu do HM này phụ trách, mới cập nhật lên đầu. */
    public List<RequisitionRow> recentRequisitions(int userId, int limit) {
        List<Object[]> rows = entityManager.createQuery("select r.requisitionId, r.requisitionCode, r.title, "
                        + "r.numberOfPositions, r.approvalStatus, coalesce(r.updatedAt, r.createdAt) "
                        + "from JobRequisition r where r.hiringManager.userId = :userId "
                        + "order by coalesce(r.updatedAt, r.createdAt) desc, r.requisitionId desc", Object[].class)
                .setParameter("userId", userId)
                .setMaxResults(limit)
                .getResultList();
        List<RequisitionRow> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new RequisitionRow((Integer) row[0], (String) row[1], (String) row[2],
                    (Integer) row[3], (String) row[4], (LocalDateTime) row[5]));
        }
        return result;
    }

    /**
     * Yêu cầu HM phải tự làm tiếp: bị từ chối (sửa lại) và bản nháp (gửi duyệt).
     * Bị từ chối xếp trước, trong mỗi nhóm mới cập nhật lên đầu.
     */
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

    /** Lịch phỏng vấn còn hiệu lực của phòng ban, bắt đầu trong [from, to), sớm nhất lên đầu. */
    public List<InterviewRow> departmentInterviews(Integer departmentId, LocalDateTime from, LocalDateTime to,
            int limit) {
        if (departmentId == null) {
            return List.of();
        }
        List<Object[]> rows = entityManager.createQuery("select i.interviewId, i.startTime, i.endTime, "
                        + "u.fullName, j.postingTitle, i.interviewFormat, i.locationOrLink, i.interviewStatus, "
                        + "(select count(p) from InterviewPanel p where p.interviewSchedule = i) "
                        + "from InterviewSchedule i join i.application a join a.jobPosting j "
                        + "join a.candidate c left join c.account u "
                        + "where j.requisition.department.departmentId = :departmentId "
                        + "and i.startTime >= :from and i.startTime < :to "
                        + "and i.interviewStatus in " + DashboardMetricsRepository.ACTIVE_INTERVIEW_STATUSES + " "
                        + "order by i.startTime asc, i.interviewId asc", Object[].class)
                .setParameter("departmentId", departmentId)
                .setParameter("from", from)
                .setParameter("to", to)
                .setMaxResults(limit)
                .getResultList();
        List<InterviewRow> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new InterviewRow((Long) row[0], (LocalDateTime) row[1], (LocalDateTime) row[2],
                    (String) row[3], (String) row[4], (String) row[5], (String) row[6], (String) row[7],
                    (Long) row[8]));
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

    public record RequisitionRow(int requisitionId, String requisitionCode, String title, int numberOfPositions,
            String approvalStatus, LocalDateTime updatedAt) {
    }

    public record AttentionRow(int requisitionId, String requisitionCode, String title, String approvalStatus,
            LocalDateTime submittedAt, String rejectComment) {
    }

    public record InterviewRow(long interviewId, LocalDateTime startTime, LocalDateTime endTime,
            String candidateName, String positionTitle, String interviewFormat, String locationOrLink,
            String interviewStatus, long panelSize) {
    }
}
