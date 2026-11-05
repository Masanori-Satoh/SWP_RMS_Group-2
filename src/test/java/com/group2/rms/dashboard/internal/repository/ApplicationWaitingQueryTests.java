package com.group2.rms.dashboard.internal.repository;

import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** "Đang chờ" của dashboard chỉ tính đơn chưa kết thúc: đơn rút / dừng do đóng tin không được đếm. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional
class ApplicationWaitingQueryTests {

    @Autowired InternalMetricsRepository metrics;
    @Autowired ApplicationRepository applications;
    @Autowired EntityManager entityManager;

    @Test
    void withdrawnAndCancelledApplicationsAreNotWaiting() {
        List<Integer> ids = entityManager.createQuery(
                "select a.applicationId from Application a where a.applicationStatus = 'Applied' order by a.applicationId",
                Integer.class).setMaxResults(3).getResultList();
        assumeTrue(ids.size() == 3, "DB local cần ít nhất 3 đơn Applied");
        assertTrue(metrics.applicationWaitingSince().keySet().containsAll(ids), "trước: cả 3 đơn đang chờ");

        applications.transition(ids.get(0), List.of("Applied"), "Withdrawn");
        applications.transition(ids.get(1), List.of("Applied"), "Cancelled");
        entityManager.clear();

        var waiting = metrics.applicationWaitingSince();
        assertFalse(waiting.containsKey(ids.get(0)), "Withdrawn không còn đang chờ");
        assertFalse(waiting.containsKey(ids.get(1)), "Cancelled không còn đang chờ");
        assertTrue(waiting.containsKey(ids.get(2)), "đơn Applied vẫn đang chờ");
    }
}
