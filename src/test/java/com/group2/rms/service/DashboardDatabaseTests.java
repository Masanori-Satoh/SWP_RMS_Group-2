package com.group2.rms.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.ApplicationReview;
import com.group2.rms.candidate.entity.AIScreeningResult;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.dashboard.service.DashboardService;
import com.group2.rms.dashboard.dto.DashboardView;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.offer.OfferProposal;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.repository.ApplicationReviewRepository;
import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.dashboard.repository.DashboardMetricsRepository;
import com.group2.rms.requisition.repository.JobPostingRepository;
import com.group2.rms.offer.OfferProposalRepository;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.AccountManagementService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class DashboardDatabaseTests {

    @Autowired private DashboardService dashboard;
    @Autowired private DashboardMetricsRepository metrics;
    @Autowired private AccountManagementService accounts;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private CandidateRepository candidates;
    @Autowired private ApplicationRepository applications;
    @Autowired private ApplicationReviewRepository reviews;
    @Autowired private JobPostingRepository postings;
    @Autowired private OfferProposalRepository offers;
    @Autowired private EntityManager entityManager;

    @Test
    void allSixRolesCanBuildDashboardAgainstCurrentDatabase() {
        assertTrue(metrics.applicationStages().stream()
                .mapToLong(DashboardMetricsRepository.StatusCount::count).sum() <= metrics.applications());
        assertEquals(metrics.accounts(), metrics.accountsWithStatus("Active")
                + metrics.accountsWithStatus("Inactive") + metrics.accountsWithStatus("Blocked"));

        for (String roleName : List.of("System Admin", "HR", "Hiring Manager", "Director", "Interviewer")) {
            User account = users.findAll().stream()
                    .filter(user -> roleName.equals(user.getRole().getRoleName()))
                    .filter(user -> "Active".equals(user.getAccountStatus()))
                    .findFirst().orElseThrow();
            DashboardView view = dashboard.forUsername(account.getUsername());
            assertEquals(roleName, view.roleName());
            assertFalse(view.metrics().isEmpty());
            assertTrue(view.metrics().stream().allMatch(metric -> metric.value() >= 0));
        }

        User candidateAccount = createCandidateAccount();
        DashboardView candidateView = dashboard.forUsername(candidateAccount.getUsername());
        assertEquals("Candidate", candidateView.roleName());
        assertEquals(0, candidateView.metrics().getFirst().value());
    }

    @Test
    void candidateDashboardOnlySeesApplicationsLinkedToItsOwnAccount() {
        User first = createCandidateAccount();
        User second = createCandidateAccount();
        assertEquals(0, metrics.candidateApplications(first.getUserId()));
        assertEquals(0, metrics.candidateApplications(second.getUserId()));

        Candidate profile = candidates.findByAccountUserId(first.getUserId()).orElseThrow();
        JobPosting posting = postings.findAll().stream().findFirst().orElseThrow();
        Application application = applications.saveAndFlush(Application.builder()
                .candidate(profile)
                .jobPosting(posting)
                .appliedCvUrl("/test/dashboard-cv.pdf")
                .applicationStatus("Offered")
                .build());
        entityManager.clear();

        assertEquals(1, metrics.candidateApplications(first.getUserId()));
        assertEquals(0, metrics.candidateApplications(second.getUserId()));
        assertEquals(1, metrics.candidateApplicationStages(first.getUserId()).stream()
                .mapToLong(DashboardMetricsRepository.StatusCount::count).sum());
        assertTrue(metrics.candidateApplicationStages(second.getUserId()).isEmpty());
        assertEquals(1, dashboard.forUsername(first.getUsername()).metrics().getFirst().value());
        assertEquals(0, dashboard.forUsername(second.getUsername()).metrics().getFirst().value());

        User manager = users.findAll().stream()
                .filter(user -> "Hiring Manager".equals(user.getRole().getRoleName()))
                .findFirst().orElseThrow();
        OfferProposal offer = offers.saveAndFlush(OfferProposal.builder()
                .application(application)
                .proposedSalary(BigDecimal.valueOf(20_000_000))
                .probationSalary(BigDecimal.valueOf(17_000_000))
                .offeredPositionTitle("Dashboard test")
                .proposedBy(manager)
                .offerStatus("Draft")
                .build());
        assertTrue(metrics.candidateOfferStatuses(first.getUserId()).isEmpty());
        offer.setOfferStatus("Sent_Candidate");
        entityManager.flush();
        assertEquals(1, metrics.candidateOfferStatuses(first.getUserId()).stream()
                .mapToLong(DashboardMetricsRepository.StatusCount::count).sum());
        assertTrue(metrics.candidateOfferStatuses(second.getUserId()).isEmpty());
    }

    @Test
    void pendingHrCountUsesApplicationReviewRows() {
        long before = metrics.newApplicationsAwaitingHrReview();
        User candidateAccount = createCandidateAccount();
        Candidate candidate = candidates.findByAccountUserId(candidateAccount.getUserId()).orElseThrow();
        JobPosting posting = postings.findAll().stream().findFirst().orElseThrow();
        Application application = applications.saveAndFlush(Application.builder()
                .candidate(candidate)
                .jobPosting(posting)
                .appliedCvUrl("/test/hr-review-cv.pdf")
                .applicationStatus("Applied")
                .build());
        assertEquals(before + 1, metrics.newApplicationsAwaitingHrReview());

        User manager = users.findAll().stream()
                .filter(user -> "Hiring Manager".equals(user.getRole().getRoleName()))
                .findFirst().orElseThrow();
        reviews.saveAndFlush(ApplicationReview.builder().application(application)
                .reviewer(manager).reviewerRole("HiringManager").decision("Hold").build());
        assertEquals(before + 1, metrics.newApplicationsAwaitingHrReview());

        User hr = users.findAll().stream()
                .filter(user -> "HR".equals(user.getRole().getRoleName()))
                .findFirst().orElseThrow();
        reviews.saveAndFlush(ApplicationReview.builder().application(application)
                .reviewer(hr).reviewerRole("HR").decision("Pass").build());
        assertEquals(before, metrics.newApplicationsAwaitingHrReview());
    }

    @Test
    void applicationCanKeepMultipleAiScreeningResults() {
        User candidateAccount = createCandidateAccount();
        Candidate candidate = candidates.findByAccountUserId(candidateAccount.getUserId()).orElseThrow();
        JobPosting posting = postings.findAll().stream().findFirst().orElseThrow();
        Application application = applications.saveAndFlush(Application.builder()
                .candidate(candidate)
                .jobPosting(posting)
                .appliedCvUrl("/test/rescored-cv.pdf")
                .applicationStatus("Applied")
                .build());

        entityManager.persist(AIScreeningResult.builder()
                .application(application).aiMatchScore(BigDecimal.valueOf(60)).build());
        entityManager.persist(AIScreeningResult.builder()
                .application(application).aiMatchScore(BigDecimal.valueOf(85)).build());
        entityManager.flush();

        Long resultCount = entityManager.createQuery(
                        "select count(result) from AIScreeningResult result "
                                + "where result.application.applicationId = :applicationId", Long.class)
                .setParameter("applicationId", application.getApplicationId())
                .getSingleResult();
        assertEquals(2L, resultCount);
    }

    private User createCandidateAccount() {
        Role role = roles.findAll().stream()
                .filter(item -> "Candidate".equals(item.getRoleName()))
                .findFirst().orElseThrow();
        String username = "dash" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        int userId = accounts.create(new AccountManagementService.CreateCommand(
                "Dashboard Candidate", username, username + "@example.test", "0900000000",
                role.getRoleId(), null, "validPassword12"));
        entityManager.flush();
        return users.findById(userId).orElseThrow();
    }
}
