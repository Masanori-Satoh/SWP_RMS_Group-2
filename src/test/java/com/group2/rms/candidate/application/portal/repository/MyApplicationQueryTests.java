package com.group2.rms.candidate.application.portal.repository;

import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.candidate.domain.entity.Application;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
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
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional
class MyApplicationQueryTests {

    @Autowired ApplicationRepository applications;
    @Autowired MyApplicationRepository myApplications;
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
        List<Application> owned = myApplications.findOwnedByUserId(ownerUserId);

        assertTrue(owned.stream().anyMatch(a -> a.getApplicationId().equals(early.getApplicationId())));
        assertTrue(owned.stream().allMatch(a -> a.getCandidate().getAccount().getUserId().equals(ownerUserId)));
        assertTrue(myApplications.findOwnedById(early.getApplicationId(), ownerUserId).isPresent());
        assertTrue(myApplications.findOwnedById(early.getApplicationId(), -1).isEmpty());
    }

    /** Thẻ đơn hiện tên phòng ban; open-in-view=false nên phòng ban phải về cùng câu query, không nạp lười. */
    @Test
    void ownedQueriesFetchPostingRequisitionAndDepartment() {
        entityManager.clear();

        Application owned = myApplications.findOwnedById(early.getApplicationId(), ownerUserId).orElseThrow();

        assertTrue(Hibernate.isInitialized(owned.getJobPosting()));
        assertTrue(Hibernate.isInitialized(owned.getJobPosting().getRequisition()));
        assertTrue(Hibernate.isInitialized(owned.getJobPosting().getRequisition().getDepartment()));
        assertNotNull(owned.getJobPosting().getRequisition().getDepartment().getDepartmentName());
    }

    @Test
    void withdrawalIsRecordedAndReadBack() {
        Integer id = early.getApplicationId();
        assertFalse(myApplications.findWithdrawnApplicationIds(ownerUserId).contains(id));

        assertEquals(1, applications.transition(id, Set.of("Applied", "AI_Screened"), "Rejected"));
        applications.recordWithdrawal(ownerUserId, id, early.getApplicationStatus());

        assertTrue(myApplications.findWithdrawnApplicationIds(ownerUserId).contains(id));
        assertFalse(myApplications.findWithdrawnApplicationIds(-1).contains(id));
    }

    @Test
    void withdrawalMarkComesBackWithStatusBeforeAndTime() {
        Integer id = early.getApplicationId();
        applications.transition(id, Set.of("Applied", "AI_Screened"), "Rejected");
        applications.recordWithdrawal(ownerUserId, id, early.getApplicationStatus());

        MyApplicationRepository.MyApplicationMarkRow mark = myApplications.findMyApplicationMarks(ownerUserId).stream()
                .filter(row -> row.getApplicationId().equals(id))
                .findFirst().orElseThrow();
        assertEquals("Withdrawn", mark.getKind());
        assertEquals(early.getApplicationStatus(), mark.getOldStatus());
        assertNotNull(mark.getAt());
        assertTrue(myApplications.findMyApplicationMarks(-1).isEmpty());
    }

    @Test
    void postingClosedMarkWrittenByStaffIsFoundByTheOwner() {
        Integer id = early.getApplicationId();
        Integer staffUserId = ((Number) entityManager.createNativeQuery(
                        "SELECT TOP 1 UserId FROM [User] WHERE UserId <> :owner ORDER BY UserId")
                .setParameter("owner", ownerUserId).getSingleResult()).intValue();
        entityManager.createNativeQuery("""
                        INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue)
                        VALUES (:staff, 'UPDATE', 'Application', CAST(:id AS NVARCHAR(50)), :old, 'PostingClosed')""")
                .setParameter("staff", staffUserId).setParameter("id", id)
                .setParameter("old", early.getApplicationStatus()).executeUpdate();

        assertTrue(myApplications.findMyApplicationMarks(ownerUserId).stream().anyMatch(row ->
                row.getApplicationId().equals(id) && "PostingClosed".equals(row.getKind())
                        && early.getApplicationStatus().equals(row.getOldStatus())));
    }

    @Test
    void internalOrDeletedOffersAreHiddenFromTheCandidate() {
        List<?> found = entityManager.createNativeQuery("""
                        SELECT TOP 1 o.OfferId, c.UserId FROM OfferProposal o
                            JOIN Application a ON a.ApplicationId = o.ApplicationId
                            JOIN Candidate c ON c.CandidateId = a.CandidateId
                        WHERE o.IsDeleted = 0 AND o.OfferStatus IN ('Sent_Candidate', 'Accepted', 'Declined')
                        ORDER BY o.OfferId""").getResultList();
        assumeTrue(!found.isEmpty(), "DB local cần ít nhất một offer đã gửi ứng viên");
        Object[] row = (Object[]) found.getFirst();
        Integer offerId = ((Number) row[0]).intValue();
        Integer userId = ((Number) row[1]).intValue();
        assertTrue(visibleOfferIds(userId).contains(offerId));

        setOffer(offerId, "OfferStatus = 'Pending_Director'");
        assertFalse(visibleOfferIds(userId).contains(offerId));

        setOffer(offerId, "OfferStatus = 'Sent_Candidate', IsDeleted = 1");
        assertFalse(visibleOfferIds(userId).contains(offerId));
    }

    @Test
    void interviewsAreOnlyThoseOfTheCandidatesApplications() {
        List<?> found = entityManager.createNativeQuery("""
                        SELECT TOP 1 c.UserId FROM InterviewSchedule s
                            JOIN Application a ON a.ApplicationId = s.ApplicationId
                            JOIN Candidate c ON c.CandidateId = a.CandidateId
                        ORDER BY s.InterviewId""").getResultList();
        assumeTrue(!found.isEmpty(), "DB local cần ít nhất một lịch phỏng vấn");
        Integer userId = ((Number) found.getFirst()).intValue();
        List<?> ownIds = entityManager.createNativeQuery("""
                        SELECT a.ApplicationId FROM Application a JOIN Candidate c ON c.CandidateId = a.CandidateId
                        WHERE c.UserId = :userId""").setParameter("userId", userId).getResultList();

        List<MyApplicationRepository.MyInterviewRow> rows = myApplications.findMyInterviews(userId);
        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().allMatch(r -> ownIds.contains(r.getApplicationId())
                && r.getStartTime() != null && r.getFormat() != null && r.getStatus() != null
                && r.getScheduledAt() != null));
    }

    private List<Integer> visibleOfferIds(Integer userId) {
        return myApplications.findMyVisibleOffers(userId).stream()
                .map(MyApplicationRepository.MyOfferRow::getOfferId).toList();
    }

    private void setOffer(Integer offerId, String assignments) {
        entityManager.createNativeQuery("UPDATE OfferProposal SET " + assignments + " WHERE OfferId = :id")
                .setParameter("id", offerId).executeUpdate();
    }
}
