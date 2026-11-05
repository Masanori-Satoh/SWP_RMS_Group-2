package com.group2.rms.candidate.talentpool.repository;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Đóng tin chỉ xử lý đơn còn dở: đơn đã ở trạng thái kết thúc (kể cả {@code Withdrawn}, {@code Cancelled}) không được
 * lôi ra để đổi lại. Chạy trên DB local, rollback sau mỗi test.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional
class TalentPoolClosureQueryTests {

    @Autowired TalentPoolApplicationRepository pool;
    @Autowired ApplicationRepository applications;
    @Autowired EntityManager entityManager;

    @Test
    void pendingApplicationsForClosedJobSkipsEveryTerminalStatus() {
        List<Integer> postings = entityManager.createQuery("""
                select a.jobPosting.jobPostingId from Application a where a.applicationStatus = 'Applied'
                group by a.jobPosting.jobPostingId having count(a) >= 4""", Integer.class).getResultList();
        assumeTrue(!postings.isEmpty(), "DB local cần một tin có ít nhất 4 đơn Applied");
        Integer posting = postings.getFirst();
        List<Integer> ids = entityManager.createQuery("""
                select a.applicationId from Application a
                where a.jobPosting.jobPostingId = :posting and a.applicationStatus = 'Applied' order by a.applicationId""",
                Integer.class).setParameter("posting", posting).setMaxResults(4).getResultList();
        assertEquals(Set.copyOf(ids), idsOf(pool.findPendingApplicationsForClosedJob(posting), ids), "trước: cả 4 đơn còn dở");

        applications.transition(ids.get(0), List.of("Applied"), "Withdrawn");
        applications.transition(ids.get(1), List.of("Applied"), "Cancelled");
        applications.transition(ids.get(2), List.of("Applied"), "Rejected");
        entityManager.clear();

        assertEquals(Set.of(ids.get(3)), idsOf(pool.findPendingApplicationsForClosedJob(posting), ids),
                "chỉ còn đơn Applied; Withdrawn, Cancelled, Rejected đứng yên");
    }

    private static Set<Integer> idsOf(List<Application> found, List<Integer> among) {
        return found.stream().map(Application::getApplicationId).filter(among::contains)
                .collect(java.util.stream.Collectors.toSet());
    }
}
