package com.group2.rms.candidate.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import java.util.List;

/** Truy vấn audit/đối chiếu liên module bằng SQL; không import Entity của admin/interview/offer. */
@Repository
@RequiredArgsConstructor
public class TalentPoolAuditRepository {
    private final JdbcTemplate jdbc;
    @Transactional
    public void record(Integer actorId, String action, String entity, Number id, String oldValue, String value) {
        jdbc.update("INSERT INTO AuditLog(UserId,[Action],EntityName,EntityId,OldValue,NewValue) VALUES(?,?,?,?,?,?)",
                actorId, action, entity, id.toString(), oldValue, value);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMail(Integer actorId, String entity, Number id, String kind, boolean sent) {
        record(actorId, "UPDATE", entity, id, null, (sent ? "MailSent:" : "MailFailed:") + kind);
    }
    public boolean has(String entity, Number id, String value) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM AuditLog WHERE EntityName=? AND EntityId=? AND NewValue=?",
                Integer.class, entity, id.toString(), value) > 0;
    }
    //get infor user recommend
    public String suggestedBy(Integer id) {
        return jdbc.query("SELECT TOP 1 u.FullName FROM AuditLog l JOIN [User] u ON u.UserId=l.UserId " +
                "WHERE l.EntityName='Application' AND l.EntityId=? AND l.NewValue='TalentPoolSuggested' ORDER BY l.AuditLogId",
                (rs, n) -> rs.getString(1), id.toString()).stream().findFirst().orElse("—");
    }
    public boolean hasOpenOffers(Integer jobId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM OfferProposal o JOIN Application a ON a.ApplicationId=o.ApplicationId " +
                "WHERE a.JobPostingId=? AND o.OfferStatus NOT IN ('Accepted','Declined','Director_Rejected')",
                Integer.class, jobId) > 0;
    }
    public boolean hasAcceptedOffer(Integer appId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM OfferProposal WHERE ApplicationId=? AND OfferStatus='Accepted'",
                Integer.class, appId) > 0;
    }
    public int cancelInterviews(Integer appId) {
        return jdbc.update("UPDATE InterviewSchedule SET InterviewStatus='Cancelled' WHERE ApplicationId=? " +
                "AND InterviewStatus IN ('Scheduled','Rescheduled')", appId);
    }
    public List<Integer> closureRecipients(Integer jobId) {
        return jdbc.queryForList("SELECT a.ApplicationId FROM Application a WHERE a.JobPostingId=? " +
                "AND EXISTS(SELECT 1 FROM AuditLog l WHERE l.EntityName='Application' " +
                "AND l.EntityId=CAST(a.ApplicationId AS NVARCHAR(50)) AND l.NewValue='PostingClosed')",
                Integer.class, jobId);
    }
}

