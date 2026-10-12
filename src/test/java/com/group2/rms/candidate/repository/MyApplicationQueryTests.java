package com.group2.rms.candidate.repository;

import com.group2.rms.candidate.entity.Application;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Các query My Applications và dấu rút đơn chạy trên DB local. Mỗi test rollback nên DB không đổi.
 */
@SpringBootTest
@Transactional
class MyApplicationQueryTests {

    @Autowired ApplicationRepository applications;
    @Autowired EntityManager entityManager;

    private Application early;
    private Integer ownerUserId;

    @BeforeEach
    void pickEarlyApplication() {
        List<Application> found = entityManager.createQuery("""
                        select a from Application a join fetch a.candidate c join fetch c.account
                        where a.applicationStatus in ('Applied', 'AI_Screened')
                        order by a.applicationId""", Application.class)
                .setMaxResults(1)
                .getResultList();
        assumeTrue(!found.isEmpty(), "DB local cần ít nhất một đơn Applied/AI_Screened");
        early = found.getFirst();
        ownerUserId = early.getCandidate().getAccount().getUserId();
    }

    @Test
    void ownedQueriesReturnOnlyTheCandidatesApplications() {
        List<Application> owned = applications.findOwnedByUserId(ownerUserId);

        assertTrue(owned.stream().anyMatch(a -> a.getApplicationId().equals(early.getApplicationId())));
        assertTrue(owned.stream().allMatch(a -> a.getCandidate().getAccount().getUserId().equals(ownerUserId)));
        assertTrue(applications.findOwnedById(early.getApplicationId(), ownerUserId).isPresent());
        assertTrue(applications.findOwnedById(early.getApplicationId(), -1).isEmpty());
    }

    @Test
    void withdrawalIsRecordedAndReadBack() {
        Integer id = early.getApplicationId();
        assertFalse(applications.findWithdrawnApplicationIds(ownerUserId).contains(id));

        assertEquals(1, applications.transition(id, Set.of("Applied", "AI_Screened"), "Rejected"));
        applications.recordWithdrawal(ownerUserId, id, early.getApplicationStatus());

        assertTrue(applications.findWithdrawnApplicationIds(ownerUserId).contains(id));
        assertFalse(applications.findWithdrawnApplicationIds(-1).contains(id));
    }
}
