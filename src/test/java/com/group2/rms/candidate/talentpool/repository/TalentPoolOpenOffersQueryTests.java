package com.group2.rms.candidate.talentpool.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Execute the real SQL Server query; all fixture changes roll back after each case. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional
class TalentPoolOpenOffersQueryTests {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 5, 12, 0);
    @Autowired JdbcTemplate jdbc;
    private TalentPoolAuditRepository repository;
    private int offerId;
    private int jobId;

    @BeforeEach
    void fixture() {
        repository = new TalentPoolAuditRepository(jdbc,
                Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));
        List<int[]> rows = jdbc.query("""
                SELECT TOP 1 o.OfferId, a.JobPostingId FROM OfferProposal o
                JOIN Application a ON a.ApplicationId=o.ApplicationId ORDER BY o.OfferId
                """, (rs, n) -> new int[]{rs.getInt(1), rs.getInt(2)});
        assertFalse(rows.isEmpty(), "SQL Server test fixture needs at least one offer");
        offerId = rows.getFirst()[0];
        jobId = rows.getFirst()[1];
        jdbc.update("""
                UPDATE OfferProposal SET OfferStatus='Declined' WHERE ApplicationId IN
                (SELECT ApplicationId FROM Application WHERE JobPostingId=?)
                """, jobId);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "Sent_Candidate,2026-11-05T11:59:59,2026-11-06,false",
            "Sent_Candidate,2026-11-05T12:00:00,2026-11-06,true",
            "Sent_Candidate,2026-11-05T12:00:01,2026-11-06,true",
            "Sent_Candidate,NULL,2026-11-04,false",
            "Sent_Candidate,NULL,2026-11-05,true",
            "Sent_Candidate,NULL,2026-11-06,true",
            "Sent_Candidate,2026-11-06T12:00:00,2026-11-04,false",
            "Sent_Candidate,NULL,NULL,true",
            "Sent_Candidate,2026-11-05T11:59:59,NULL,false",
            "Sent_Candidate,2026-11-06T12:00:00,NULL,true",
            "Draft,2026-11-04T12:00:00,2026-11-04,true",
            "Pending_Director,NULL,NULL,true",
            "Director_Approved,NULL,NULL,true",
            "Accepted,NULL,NULL,false",
            "Declined,NULL,NULL,false",
            "Director_Rejected,NULL,NULL,false"
    }, nullValues = "NULL")
    void closureGuardMatchesOfferExpiry(String status, String deadline, String start, boolean blocks) {
        jdbc.update("UPDATE OfferProposal SET OfferStatus=?, ResponseDeadline=?, ExpectedStartDate=? WHERE OfferId=?",
                status, deadline == null ? null : LocalDateTime.parse(deadline),
                start == null ? null : LocalDate.parse(start), offerId);

        assertEquals(blocks, repository.hasOpenOffers(jobId));
        assertFalse(repository.hasOpenOffers(-1), "An offer must only block its own posting");
        assertEquals(status, jdbc.queryForObject("SELECT OfferStatus FROM OfferProposal WHERE OfferId=?",
                String.class, offerId), "The closure guard must not mutate offer status");
    }

    @Test
    void anotherOpenOfferStillBlocksAfterOneExpires() {
        List<Integer> otherOffers = jdbc.queryForList("SELECT OfferId FROM OfferProposal WHERE OfferId<>?",
                Integer.class, offerId);
        assertFalse(otherOffers.isEmpty(), "Mixed-offer fixture needs two offers");
        int otherId = otherOffers.getFirst();
        // Move the second offer's application into the same posting inside this rollback-only fixture.
        jdbc.update("""
                UPDATE Application SET JobPostingId=? WHERE ApplicationId=
                (SELECT ApplicationId FROM OfferProposal WHERE OfferId=?)
                """, jobId, otherId);
        jdbc.update("UPDATE OfferProposal SET OfferStatus='Sent_Candidate', ResponseDeadline=?, ExpectedStartDate=? WHERE OfferId=?",
                NOW.minusSeconds(1), NOW.toLocalDate().plusDays(1), offerId);
        jdbc.update("UPDATE OfferProposal SET OfferStatus='Sent_Candidate', ResponseDeadline=?, ExpectedStartDate=? WHERE OfferId=?",
                NOW.plusDays(1), NOW.toLocalDate().plusDays(2), otherId);
        assertTrue(repository.hasOpenOffers(jobId));
    }
}
