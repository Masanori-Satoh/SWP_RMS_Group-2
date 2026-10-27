package com.group2.rms.candidate.application.portal.repository;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.group2.rms.candidate.domain.repository.ApplicationRepository.FORMAL_FILTER;

/**
 * Câu đọc riêng của My Applications (5.1.38). Câu ghi và câu đọc dùng chung nằm ở
 * {@link ApplicationRepository}.
 */
@Repository
public interface MyApplicationRepository extends org.springframework.data.repository.Repository<Application, Integer> {
        /** Đơn của một ứng viên ("Đơn của tôi"), mới nhất trước; kèm tin, yêu cầu tuyển, phòng ban (thẻ hiện tên phòng). */
        @Query("""
                        SELECT a FROM Application a
                            JOIN FETCH a.jobPosting j
                            JOIN FETCH j.requisition r
                            JOIN FETCH r.department
                        WHERE a.candidate.account.userId = :userId
                        """ + " AND " + FORMAL_FILTER + """
                        ORDER BY a.submissionDate DESC, a.applicationId DESC
                        """)
        List<Application> findOwnedByUserId(@Param("userId") Integer userId);

        /** Một đơn, chỉ khi thuộc về ứng viên {@code userId}. */
        @Query("""
                        SELECT a FROM Application a
                            JOIN FETCH a.jobPosting j
                            JOIN FETCH j.requisition r
                            JOIN FETCH r.department
                        WHERE a.applicationId = :id
                          AND a.candidate.account.userId = :userId
                        """ + " AND " + FORMAL_FILTER + """
                        """)
        Optional<Application> findOwnedById(@Param("id") Integer applicationId, @Param("userId") Integer userId);

        /**
         * Các đơn ứng viên {@code userId} đã tự rút (dấu do {@link ApplicationRepository#recordWithdrawal}
         * ghi).
         */
        @Query(value = "SELECT CAST(l.EntityId AS INT) FROM AuditLog l "
                        + "WHERE l.EntityName = 'Application' AND l.NewValue = 'Withdrawn' AND l.UserId = :userId", nativeQuery = true)
        List<Integer> findWithdrawnApplicationIds(@Param("userId") Integer userId);

        // dunglt bổ sung: 3 câu dữ kiện cho My Applications (5.1.38), mỗi câu lấy cho mọi đơn của
        // một ứng viên (theo userId) nên số câu không tăng theo số đơn. SQL thuần, cùng lý do với
        // ApplicationPipelineRepository.findInterviewEvents.

        /**
         * Mọi lịch phỏng vấn (kể cả lịch đã hủy) của các đơn thuộc ứng viên {@code userId}, cũ nhất trước.
         * {@code scheduledAt} (lúc xếp lịch) là lúc ứng viên được báo, dùng để xếp "đơn mới có chuyển biến".
         */
        @Query(value = """
                        SELECT s.ApplicationId AS applicationId, s.StartTime AS startTime,
                               s.InterviewFormat AS format, s.InterviewStatus AS status, s.CreatedAt AS scheduledAt
                        FROM InterviewSchedule s
                            JOIN Application a ON a.ApplicationId = s.ApplicationId
                            JOIN Candidate c ON c.CandidateId = a.CandidateId
                        WHERE c.UserId = :userId
                        ORDER BY s.StartTime, s.InterviewId
                        """, nativeQuery = true)
        List<MyInterviewRow> findMyInterviews(@Param("userId") Integer userId);

        /**
         * Offer ứng viên {@code userId} được thấy: đã gửi cho họ hoặc họ đã trả lời. Offer nội bộ
         * ({@code Draft}, {@code Pending_Director}, {@code Director_*}) và offer đã xóa mềm bị lọc ngay
         * trong câu SQL (GBR-02). {@code UpdatedAt} trống thì lấy {@code CreatedAt}.
         */
        @Query(value = """
                        SELECT o.ApplicationId AS applicationId, o.OfferId AS offerId, o.OfferStatus AS status,
                               o.ResponseDeadline AS responseDeadline, COALESCE(o.UpdatedAt, o.CreatedAt) AS at
                        FROM OfferProposal o
                            JOIN Application a ON a.ApplicationId = o.ApplicationId
                            JOIN Candidate c ON c.CandidateId = a.CandidateId
                        WHERE c.UserId = :userId
                          AND o.IsDeleted = 0
                          AND o.OfferStatus IN ('Sent_Candidate', 'Accepted', 'Declined')
                        """, nativeQuery = true)
        List<MyOfferRow> findMyVisibleOffers(@Param("userId") Integer userId);

        /**
         * Dấu {@code Withdrawn} (ứng viên tự rút) và {@code PostingClosed} (dừng do đóng tin) trên các đơn
         * của ứng viên {@code userId}, cũ nhất trước. Lọc theo chủ đơn, không theo người ghi dấu: dấu
         * {@code PostingClosed} do HR ghi. {@code OldValue} là trạng thái đơn ngay trước khi dừng.
         */
        @Query(value = """
                        SELECT a.ApplicationId AS applicationId, l.NewValue AS kind,
                               l.OldValue AS oldStatus, l.[Timestamp] AS at
                        FROM AuditLog l
                            JOIN Application a ON l.EntityName = 'Application'
                                AND l.EntityId = CAST(a.ApplicationId AS NVARCHAR(50))
                            JOIN Candidate c ON c.CandidateId = a.CandidateId
                        WHERE c.UserId = :userId
                          AND l.NewValue IN ('Withdrawn', 'PostingClosed')
                        ORDER BY l.[Timestamp], l.AuditLogId
                        """, nativeQuery = true)
        List<MyApplicationMarkRow> findMyApplicationMarks(@Param("userId") Integer userId);

        /** Một dòng của {@link #findMyInterviews}. */
        interface MyInterviewRow {
                Integer getApplicationId();

                LocalDateTime getStartTime();

                String getFormat();

                String getStatus();

                LocalDateTime getScheduledAt();
        }

        /** Một dòng của {@link #findMyVisibleOffers}; {@code responseDeadline} có thể {@code null}. */
        interface MyOfferRow {
                Integer getApplicationId();

                Integer getOfferId();

                String getStatus();

                LocalDateTime getResponseDeadline();

                LocalDateTime getAt();
        }

        /** Một dòng của {@link #findMyApplicationMarks}; {@code oldStatus} có thể {@code null}. */
        interface MyApplicationMarkRow {
                Integer getApplicationId();

                String getKind();

                String getOldStatus();

                LocalDateTime getAt();
        }
}
