package com.group2.rms.service;

import com.group2.rms.dashboard.DashboardMetricsRepository;
import com.group2.rms.dashboard.DashboardService;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Runs SELECTs only; never creates test accounts, applications, offers, or schema. */
@SpringBootTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.sql.init.mode=never", "spring.jpa.show-sql=false"})
@Transactional(readOnly = true)
class CandidateDashboardReadOnlyTests {
    @Autowired private DashboardMetricsRepository metrics;
    @Autowired private DashboardService dashboard;
    @Autowired private UserRepository users;

    @Test
    void allCandidateQueriesExecuteAgainstCurrentSchemaWithoutWrites() {
        int nonexistentId = -1;
        var now = LocalDateTime.now();
        assertEquals(0, metrics.candidateApplications(nonexistentId));
        assertEquals(0, metrics.candidateUpcomingInterviews(nonexistentId, now));
        assertEquals(0, metrics.candidateOffersAwaitingResponse(nonexistentId));
        assertTrue(metrics.candidateApplicationStages(nonexistentId).isEmpty());
        assertTrue(metrics.candidateOfferStatuses(nonexistentId).isEmpty());
        assertTrue(metrics.candidateRecentApplications(nonexistentId, 20).isEmpty());
        assertTrue(metrics.candidateNextInterviews(nonexistentId, now, 10).isEmpty());
        assertTrue(metrics.candidateOffers(nonexistentId, 20).isEmpty());
        // Exercise the mapping with any existing active Candidate; no assumption that it has data.
        users.findAll().stream().filter(user -> "Candidate".equals(user.getRole().getRoleName()))
                .filter(user -> "Active".equals(user.getAccountStatus())).forEach(user -> {
                    var result = dashboard.forUsername(user.getUsername());
                    assertNotNull(result.candidate());
                    assertEquals(user.getEmail(), result.candidate().profile().email());
                    assertEquals(metrics.candidateApplications(user.getUserId()), result.metrics().getFirst().value());
                });
    }
}
