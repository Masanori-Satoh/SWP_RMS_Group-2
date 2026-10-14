package com.group2.rms.offer.repository;

import com.group2.rms.offer.entity.OfferProposal;
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
 * Các query offer của ứng viên chạy trên DB local. Mỗi test rollback nên DB không đổi;
 * trạng thái offer được đặt ngay trong test nên không phụ thuộc dữ liệu seed.
 */
@SpringBootTest
@Transactional
class CandidateOfferQueryTests {

    private static final Set<String> VISIBLE = Set.of("Sent_Candidate", "Accepted", "Declined");

    @Autowired CandidateOfferRepository offers;
    @Autowired EntityManager entityManager;

    private Integer offerId;
    private Integer ownerUserId;

    @BeforeEach
    void pickAnOffer() {
        List<OfferProposal> found = entityManager.createQuery("""
                        select o from OfferProposal o join fetch o.application a
                        join fetch a.candidate c join fetch c.account order by o.offerId""", OfferProposal.class)
                .setMaxResults(1)
                .getResultList();
        assumeTrue(!found.isEmpty(), "DB local cần ít nhất một OfferProposal");
        offerId = found.getFirst().getOfferId();
        ownerUserId = found.getFirst().getApplication().getCandidate().getAccount().getUserId();
    }

    private void setStatus(String status) {
        entityManager.createQuery("update OfferProposal o set o.offerStatus = :s where o.offerId = :id")
                .setParameter("s", status).setParameter("id", offerId).executeUpdate();
        entityManager.clear();
    }

    @Test
    void sentOfferIsVisibleOnlyToItsOwner() {
        setStatus("Sent_Candidate");

        assertTrue(offers.findVisibleByUserId(ownerUserId, VISIBLE).stream()
                .anyMatch(o -> o.getOfferId().equals(offerId)));
        assertTrue(offers.findVisibleById(offerId, ownerUserId, VISIBLE).isPresent());
        assertTrue(offers.findVisibleById(offerId, -1, VISIBLE).isEmpty());
        assertTrue(offers.findVisibleByUserId(-1, VISIBLE).isEmpty());
    }

    @Test
    void internalOfferIsNeverVisibleToTheCandidate() {
        for (String status : List.of("Draft", "Pending_Director", "Director_Approved", "Director_Rejected")) {
            setStatus(status);

            assertTrue(offers.findVisibleById(offerId, ownerUserId, VISIBLE).isEmpty(), status);
            assertTrue(offers.findVisibleByUserId(ownerUserId, VISIBLE).stream()
                    .noneMatch(o -> o.getOfferId().equals(offerId)), status);
        }
    }

    @Test
    void answerIfSentChangesOnlyOffersStillWaitingForTheCandidate() {
        setStatus("Sent_Candidate");

        assertEquals(1, offers.answerIfSent(offerId, "Accepted"));
        assertEquals(0, offers.answerIfSent(offerId, "Declined"));

        entityManager.clear();
        assertEquals("Accepted", entityManager.find(OfferProposal.class, offerId).getOfferStatus());
    }

    @Test
    void answerIfSentLeavesInternalOffersAlone() {
        setStatus("Pending_Director");

        assertEquals(0, offers.answerIfSent(offerId, "Accepted"));
    }

    @Autowired com.group2.rms.notification.NotificationService notifService;
    @Autowired com.group2.rms.user.repository.UserRepository userRepo;

    @Test
    void candidateCanQueryNotificationsWithoutJdbcConversionError() {
        userRepo.findByUsernameIgnoreCase("huy.vu").ifPresent(u -> {
            var page = notifService.getNotificationsForUser(u, 1, 5);
            assertNotNull(page);
        });
    }
}
