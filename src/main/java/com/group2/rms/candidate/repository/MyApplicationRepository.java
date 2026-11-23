package com.group2.rms.candidate.repository;

import com.group2.rms.candidate.entity.Application;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.group2.rms.candidate.repository.ApplicationRepository.FORMAL_FILTER;

/**
 * Repository truy vấn dữ liệu hồ sơ ứng tuyển từ góc nhìn ứng viên.
 */
@org.springframework.stereotype.Repository
public interface MyApplicationRepository extends Repository<Application, Integer> {

    @Query("""
            SELECT a FROM Application a
                JOIN FETCH a.jobPosting j
                JOIN FETCH j.requisition r
                JOIN FETCH r.department
            WHERE a.candidate.account.userId = :userId
              AND """ + " " + FORMAL_FILTER + """
            ORDER BY a.submissionDate DESC, a.applicationId DESC
            """)
    List<Application> findOwnedByUserId(@Param("userId") Integer userId);

    @Query("""
            SELECT a FROM Application a
                JOIN FETCH a.jobPosting j
                JOIN FETCH j.requisition r
                JOIN FETCH r.department
            WHERE a.applicationId = :id
              AND a.candidate.account.userId = :userId
              AND """ + " " + FORMAL_FILTER)
    Optional<Application> findOwnedById(@Param("id") Integer applicationId, @Param("userId") Integer userId);

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

    @Query(value = """
            SELECT o.ApplicationId AS applicationId, o.OfferId AS offerId, o.OfferStatus AS status,
                   o.ResponseDeadline AS responseDeadline, o.ExpectedStartDate AS expectedStartDate,
                   COALESCE(o.UpdatedAt, o.CreatedAt) AS at
            FROM OfferProposal o
                JOIN Application a ON a.ApplicationId = o.ApplicationId
                JOIN Candidate c ON c.CandidateId = a.CandidateId
            WHERE c.UserId = :userId
              AND o.IsDeleted = 0
              AND o.OfferStatus IN ('Sent_Candidate', 'Accepted', 'Declined', 'Expired')
            """, nativeQuery = true)
    List<MyOfferRow> findMyVisibleOffers(@Param("userId") Integer userId);

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

    interface MyInterviewRow {
        Integer getApplicationId();

        LocalDateTime getStartTime();

        String getFormat();

        String getStatus();

        LocalDateTime getScheduledAt();
    }

    interface MyOfferRow {
        Integer getApplicationId();

        Integer getOfferId();

        String getStatus();

        LocalDateTime getResponseDeadline();

        LocalDate getExpectedStartDate();

        LocalDateTime getAt();
    }

    interface MyApplicationMarkRow {
        Integer getApplicationId();

        String getKind();

        String getOldStatus();

        LocalDateTime getAt();
    }
}
