package com.group2.rms.offer.repository;

import com.group2.rms.user.entity.Department;
import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.core.config.ClockConfig;
import com.group2.rms.core.config.SchemaNamingConfig;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.offer.service.OfferMailSender;
import com.group2.rms.offer.service.OfferService;
import com.group2.rms.offer.service.OfferServiceImpl;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Opt-in SQL Server regression tests. Schema creation is restricted to a dedicated temporary database. */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({com.group2.rms.offer.service.OfferExpirationJob.class, com.group2.rms.core.security.CurrentUserService.class, OfferServiceImpl.class, com.group2.rms.offer.service.CandidateOfferServiceImpl.class, com.group2.rms.offer.service.OfferExportServiceImpl.class, ClockConfig.class, SchemaNamingConfig.class})
@EnabledIfSystemProperty(named = "offer.integration.url", matches = ".*databaseName=OfferRegression_[a-f0-9]{32};.*")
class OfferPersistenceRegressionTests {
    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry properties) {
        String url = System.getProperty("offer.integration.url", "");
        if (!url.matches("jdbc:sqlserver://localhost:1433;databaseName=OfferRegression_[a-f0-9]{32};encrypt=true;trustServerCertificate=true")) {
            throw new IllegalStateException("An isolated OfferRegression database is required.");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        properties.add("spring.sql.init.mode", () -> "never");
        properties.add("app.offer.expiration.enabled", () -> "true");
        properties.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
        properties.add("logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener", () -> "OFF");
    }

    @Autowired EntityManager entityManager;
    @Autowired OfferService service;
    @Autowired OfferProposalRepository proposalRepository;
    @Autowired CandidateOfferRepository candidateRepository;
    @Autowired com.group2.rms.offer.service.OfferExpirationJob expirationJob;
    @Autowired com.group2.rms.offer.service.CandidateOfferService candidateService;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.group2.rms.candidate.domain.repository.ApplicationRepository applications;
    @Autowired com.group2.rms.offer.service.OfferExportService exportService;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired com.group2.rms.candidate.application.portal.repository.MyApplicationRepository myApplications;
    @MockitoBean OfferMailSender mailSender;
    @MockitoBean com.group2.rms.notification.service.NotificationService notificationService;
    private OfferProposal offer;

    /** Kiểm tra hợp đồng Offer–Application trên SQL thật, cả trước và sau khi chốt hết hạn. */
    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(value = {
            "-1,1,true", "0,1,false", "1,1,false", "3600,-1,true",
            "NULL,1,false", "NULL,-1,true"
    }, nullValues = "NULL")
    void applicationJourneyMatchesOfferExpiryBeforeAndAfterUpdate(Integer seconds, int startDays, boolean expired) {
        var now = java.time.LocalDateTime.now().withNano(0);
        offer.setOfferStatus("Sent_Candidate");
        offer.setResponseDeadline(seconds == null ? null : now.plusSeconds(seconds));
        offer.setExpectedStartDate(now.toLocalDate().plusDays(startDays));
        offer.getApplication().setApplicationStatus("Offered");
        Integer id = offer.getOfferId();
        Integer applicationId = offer.getApplication().getApplicationId();
        Integer owner = offer.getApplication().getCandidate().getAccount().getUserId();
        entityManager.flush();

        assertTrue(myApplications.findMyVisibleOffers(-1).isEmpty());
        assertApplicationJourney(id, owner, now, expired, "Sent_Candidate");
        if (expired) {
            assertEquals(0, candidateRepository.answerIfEligible(id, owner, "Accepted", null, now, now.toLocalDate()));
        }

        assertEquals(expired ? 1 : 0, proposalRepository.expireDue(now, now.toLocalDate()));
        assertApplicationJourney(id, owner, now, expired, expired ? "Expired" : "Sent_Candidate");
        assertEquals("Offered", entityManager.find(Application.class, applicationId).getApplicationStatus());
        assertEquals(0, proposalRepository.expireDue(now, now.toLocalDate()));

        var progress = com.group2.rms.candidate.domain.rule.ApplicationProgressResolver.resolve(
                new com.group2.rms.candidate.domain.rule.ApplicationProgressFacts(
                        applicationId, "Offered", null, null, null, 0, 0, null,
                        expired ? "Expired" : "Sent_Candidate"), now);
        assertEquals(expired ? com.group2.rms.candidate.domain.rule.ApplicationProgressStep.OFFER_EXPIRED
                : com.group2.rms.candidate.domain.rule.ApplicationProgressStep.OFFER_SENT, progress.step());
    }

    private void assertApplicationJourney(Integer id, Integer owner, java.time.LocalDateTime now,
                                          boolean expired, String storedStatus) {
        var row = myApplications.findMyVisibleOffers(owner).stream()
                .filter(item -> id.equals(item.getOfferId())).findFirst().orElseThrow();
        assertEquals(storedStatus, row.getStatus());
        var visibleOffer = new com.group2.rms.candidate.application.portal.service.MyApplicationJourney.Offer(
                row.getOfferId(), row.getStatus(), row.getResponseDeadline(), row.getAt(), row.getExpectedStartDate());
        var facts = new com.group2.rms.candidate.application.portal.service.MyApplicationJourney.Facts(
                "Offered", now.minusDays(3), java.util.List.of(), visibleOffer, java.util.List.of());
        var view = com.group2.rms.candidate.application.portal.service.MyApplicationJourney.resolve(facts, now);
        assertEquals(expired ? "Xem thư mời" : "Trả lời thư mời", view.actionLabel());
        assertEquals("/portal/offers/" + id, view.actionHref());
        assertTrue(!view.ended() && !view.stopped());
        if (expired) {
            assertEquals(null, view.urgentUntil());
            assertEquals(null, view.upcomingAt());
            var expiry = view.journey().stream()
                    .filter(item -> "Thư mời đã quá hạn phản hồi".equals(item.title())).findFirst().orElseThrow();
            assertTrue(expiry.at() != null && !expiry.at().isAfter(now));
        }
    }

    @Test
    void expiredOfferIsStillQueryableByItsOwnerOnly() {
        offer.setOfferStatus("Expired");
        entityManager.flush();
        var statuses = java.util.Set.of("Sent_Candidate", "Accepted", "Declined", "Expired");
        assertTrue(candidateRepository.findVisibleById(offer.getOfferId(), offer.getProposedBy().getUserId(), statuses).isPresent());
        assertTrue(candidateRepository.findVisibleById(offer.getOfferId(), -1, statuses).isEmpty());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "Sent_Candidate,-1,false,Expired", "Sent_Candidate,10,false,Sent_Candidate",
            "Sent_Candidate,-1,true,Sent_Candidate", "Accepted,-1,false,Accepted",
            "Declined,-1,false,Declined", "Draft,-1,false,Draft",
            "Pending_Director,-1,false,Pending_Director", "Director_Approved,-1,false,Director_Approved"
    })
    void expirationJobOnlyClosesDueSentOffersAndIsIdempotent(String status, int minutes, boolean deleted, String expected) {
        offer.setOfferStatus(status);
        offer.setIsDeleted(deleted);
        offer.setResponseDeadline(java.time.LocalDateTime.now().plusMinutes(minutes));
        Integer id = offer.getOfferId();
        Integer applicationId = offer.getApplication().getApplicationId();
        String originalApplicationStatus = offer.getApplication().getApplicationStatus();
        entityManager.flush();
        expirationJob.expireNow();
        assertEquals(expected, entityManager.find(OfferProposal.class, id).getOfferStatus());
        assertEquals(originalApplicationStatus, entityManager.find(Application.class, applicationId).getApplicationStatus());
        assertEquals(0, expirationJob.expireNow());
    }

    @Test
    void pastStartDateExpiresEvenWhenDeadlineIsInTheFuture() {
        offer.setOfferStatus("Sent_Candidate");
        offer.setExpectedStartDate(LocalDate.now().minusDays(1));
        offer.setResponseDeadline(java.time.LocalDateTime.now().plusDays(2));
        Integer id = offer.getOfferId();
        entityManager.flush();
        assertEquals(1, expirationJob.expireNow());
        assertEquals("Expired", entityManager.find(OfferProposal.class, id).getOfferStatus());
    }

    @RepeatedTest(3)
    void responseAndExpirationCannotOverwriteEachOther() throws Exception {
        var deadline = java.time.LocalDateTime.now().withNano(0);
        offer.setOfferStatus("Sent_Candidate");
        offer.setResponseDeadline(deadline);
        offer.getApplication().setApplicationStatus("Offered");
        Integer id = offer.getOfferId();
        Integer owner = offer.getProposedBy().getUserId();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var answer = executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Start timeout");
                return new TransactionTemplate(transactionManager).execute(tx -> candidateRepository.answerIfEligible(
                        id, owner, "Accepted", null, deadline.minusSeconds(1), deadline.toLocalDate()));
            });
            var expire = executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Start timeout");
                return new TransactionTemplate(transactionManager).execute(tx ->
                        proposalRepository.expireDue(deadline.plusSeconds(1), deadline.toLocalDate()));
            });
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            int answerCount = answer.get(30, TimeUnit.SECONDS);
            int expiryCount = expire.get(30, TimeUnit.SECONDS);
            assertEquals(1, answerCount + expiryCount);
            new TransactionTemplate(transactionManager).executeWithoutResult(tx ->
                    assertEquals(answerCount == 1 ? "Accepted" : "Expired",
                            entityManager.find(OfferProposal.class, id).getOfferStatus()));
        } finally {
            start.countDown(); executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    void responseUpdateChecksTheDeadlineAtTheDatabase(int seconds) {
        var now = java.time.LocalDateTime.now().withNano(0);
        offer.setOfferStatus("Sent_Candidate");
        offer.setResponseDeadline(now.plusSeconds(seconds));
        offer.getApplication().setApplicationStatus("Offered");
        entityManager.flush();
        int updated = candidateRepository.answerIfEligible(offer.getOfferId(), offer.getProposedBy().getUserId(),
                "Accepted", null, now, now.toLocalDate());
        assertEquals(seconds < 0 ? 0 : 1, updated);
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong-owner", "deleted", "ended-application", "past-start"})
    void responseUpdateRejectsIneligibleRecords(String scenario) {
        var now = java.time.LocalDateTime.now();
        offer.setOfferStatus("Sent_Candidate");
        offer.setResponseDeadline(now.plusDays(1));
        offer.getApplication().setApplicationStatus("Offered");
        if ("deleted".equals(scenario)) offer.setIsDeleted(true);
        if ("ended-application".equals(scenario)) offer.getApplication().setApplicationStatus("Withdrawn");
        if ("past-start".equals(scenario)) offer.setExpectedStartDate(now.toLocalDate().minusDays(1));
        entityManager.flush();
        int owner = "wrong-owner".equals(scenario) ? -1 : offer.getProposedBy().getUserId();
        assertEquals(0, candidateRepository.answerIfEligible(offer.getOfferId(), owner, "Declined", null,
                now, now.toLocalDate()));
    }

    @Test
    void failedApplicationTransitionRollsBackTheOfferResponse() {
        offer.setOfferStatus("Sent_Candidate");
        offer.setResponseDeadline(java.time.LocalDateTime.now().plusDays(1));
        offer.getApplication().setApplicationStatus("Offered");
        Integer id = offer.getOfferId();
        String username = offer.getProposedBy().getUsername();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        org.mockito.Mockito.doReturn(0).when(applications).transition(org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyCollection(), org.mockito.ArgumentMatchers.anyString());
        var security = org.springframework.security.core.context.SecurityContextHolder.getContext();
        var previous = security.getAuthentication();
        security.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                username, "test-only", java.util.List.of()));
        try {
            assertThrows(com.group2.rms.offer.exception.OfferResponseException.class, () -> candidateService.accept(id));
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                    assertEquals("Sent_Candidate", entityManager.find(OfferProposal.class, id).getOfferStatus()));
            verifyNoInteractions(notificationService);
        } finally { security.setAuthentication(previous); }
    }

    @BeforeEach
    void createSyntheticOffer() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Role role = Role.builder().roleName("HR_" + suffix).build();
        entityManager.persist(role);
        User hr = User.builder().role(role).username("offer_regression_" + suffix)
                .passwordHash("test-only").email(suffix + "@example.invalid")
                .fullName("Offer Regression").accountStatus("Active").build();
        entityManager.persist(hr);
        Department department = Department.builder().departmentName("Offer Regression " + suffix)
                .departmentStatus("Active").build();
        entityManager.persist(department);
        JobRequisition requisition = JobRequisition.builder().title("Test position").department(department)
                .hiringManager(hr).recruitmentRound(1).numberOfPositions(1).employmentType("Full-time")
                .jobDescription("Test").requirementDetails("Test").gender("Any")
                .workModel("On-site").approvalStatus("Approved").build();
        entityManager.persist(requisition);
        JobPosting posting = JobPosting.builder().requisition(requisition).postingNo(1)
                .postingTitle("Test position").jobDescription("Test").jobRequirements("Test")
                .createdBy(hr).postingStatus("Published").build();
        entityManager.persist(posting);
        Candidate candidate = Candidate.builder().account(hr).build();
        entityManager.persist(candidate);
        Application application = Application.builder().candidate(candidate).jobPosting(posting)
                .appliedCvUrl("/test/cv.pdf").applicationStatus("Interviewing").build();
        entityManager.persist(application);
        offer = OfferProposal.builder().application(application).proposedBy(hr)
                .offeredPositionTitle("Test position").proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("17000000")).probationDays(45).offerStatus("Draft")
                .workLocation("Test office").responsePeriodDays(5)
                .expectedStartDate(LocalDate.now().plusDays(30)).build();
        entityManager.persist(offer);
        entityManager.flush();
    }

    @Test
    void probationDurationSurvivesDatabaseRoundTrip() {
        Integer id = offer.getOfferId();
        entityManager.clear();
        assertEquals(45, service.getOfferById(id).getProbationDays());
        assertEquals(45, service.getUpdateOfferRequestById(id).getProbationDays());
        assertEquals(45, service.getOfferDetailForHr(id).getProbationDays());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Rejected", "Withdrawn", "Cancelled", "Hired"})
    void terminalApplicationCannotBePublished(String status) {
        offer.setOfferStatus("Director_Approved");
        offer.getApplication().setApplicationStatus(status);
        entityManager.flush();
        var error = assertThrows(com.group2.rms.offer.exception.OfferValidationException.class,
                () -> service.sendOfferToCandidate(offer.getOfferId()));
        assertEquals("APPLICATION_TERMINATED", error.getErrorCode());
        assertEquals("Director_Approved", offer.getOfferStatus());
        assertEquals(status, offer.getApplication().getApplicationStatus());
        verifyNoInteractions(mailSender, notificationService);
    }

    private String createQueryFixture() {
        String keyword = "Batch_" + UUID.randomUUID().toString().substring(0, 8);
        Application original = offer.getApplication();
        for (int index = 0; index < 25; index++) {
            OfferProposal current = offer;
            if (index > 0) {
                Application application = Application.builder().candidate(original.getCandidate())
                        .jobPosting(original.getJobPosting()).applicationStatus("Interviewing")
                        .appliedCvUrl("/test/cv.pdf").build();
                entityManager.persist(application);
                current = OfferProposal.builder().application(application).proposedBy(offer.getProposedBy())
                        .offeredPositionTitle(keyword).proposedSalary(offer.getProposedSalary())
                        .probationSalary(offer.getProbationSalary()).offerStatus("Draft").build();
                entityManager.persist(current);
            }
            current.setOfferedPositionTitle(keyword);
            for (int decision = 0; decision < 2; decision++) {
                entityManager.persist(com.group2.rms.offer.entity.OfferApproval.builder().offerProposal(current)
                        .director(offer.getProposedBy()).status("Approved")
                        .directorComments(decision == 1 ? "Latest decision" : "Old decision")
                        .approvedAt(java.time.LocalDateTime.of(2026, 1, decision + 1, 12, 0)).build());
            }
        }
        entityManager.flush();
        entityManager.clear();
        return keyword;
    }

    @Test
    void listingTwentyFiveOffersHasABoundedQueryBudget() {
        String keyword = createQueryFixture();
        var statistics = entityManager.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.clear();
        var result = service.getAllOffersForHr(keyword, "Draft", "LATEST",
                org.springframework.data.domain.PageRequest.of(0, 100));
        assertEquals(25, result.getTotalElements());
        System.out.println("OFFER_QUERY_COUNT list25=" + statistics.getPrepareStatementCount());
        assertTrue(statistics.getPrepareStatementCount() <= 2, "Listing must not query each Offer's relationships separately");
    }

    @Test
    void exportingTwentyFiveOffersUsesLatestDecisionsAndABoundedQueryBudget() throws Exception {
        String keyword = createQueryFixture();
        var statistics = entityManager.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.clear();
        byte[] result = exportService.exportOffersToExcel(new com.group2.rms.offer.dto.OfferExportRequest(
                com.group2.rms.offer.dto.OfferExportScope.FILTERED, null,
                new com.group2.rms.offer.dto.OfferExportFilterRequest(keyword, "Draft", "LATEST"),
                java.util.List.of("offerId", "offeredPositionTitle", "directorName", "directorComments")), "Test HR");
        System.out.println("OFFER_QUERY_COUNT export25=" + statistics.getPrepareStatementCount());
        assertTrue(statistics.getPrepareStatementCount() <= 2, "Export must batch relationships and approval history");
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(result))) {
            int positions = 0, latest = 0;
            for (var row : workbook.getSheetAt(0)) {
                for (var cell : row) {
                    if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                        if (keyword.equals(cell.getStringCellValue())) positions++;
                        if ("Latest decision".equals(cell.getStringCellValue())) latest++;
                    }
                }
            }
            assertEquals(25, positions);
            assertEquals(25, latest);
        }
    }

    @Test
    void sharedFiltersPreserveAliasesSoftDeletionAndLiteralLikeCharacters() {
        offer.setOfferedPositionTitle("Engineer 50%_[x]");
        offer.setOfferStatus("Approved");
        entityManager.flush();
        var criteria = entityManager.getCriteriaBuilder();
        var query = criteria.createQuery(OfferProposal.class);
        var root = query.from(OfferProposal.class);
        query.where(OfferProposalRepository.matchingFilters("%_[", "Director_Approved")
                .toPredicate(root, query, criteria));
        assertEquals(1, entityManager.createQuery(query).getResultList().size());
        offer.setIsDeleted(true);
        entityManager.flush();
        assertTrue(entityManager.createQuery(query).getResultList().isEmpty());
    }

    @Test
    void activeApplicationLookupIgnoresDeletedOffers() {
        Integer id = offer.getApplication().getApplicationId();
        // Exercise the actual repository query, including its soft-delete predicate.
        assertEquals(java.util.List.of(id), proposalRepository.findActiveApplicationIds(java.util.List.of(id)));
        offer.setIsDeleted(true);
        entityManager.flush();
        assertTrue(proposalRepository.findActiveApplicationIds(java.util.List.of(id)).isEmpty());
    }

    @RepeatedTest(3)
    void concurrentPublicationSendsOnlyOneLetter() throws Exception {
        offer.setOfferStatus("Director_Approved");
        Integer id = offer.getOfferId();
        Integer applicationId = offer.getApplication().getApplicationId();
        // Worker transactions need a committed fixture; all data stays in the disposable database.
        TestTransaction.flagForCommit();
        TestTransaction.end();
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> publish = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Concurrent publication did not start");
            }
            try {
                service.sendOfferToCandidate(id);
                return true;
            } catch (com.group2.rms.offer.exception.OfferValidationException error) {
                assertEquals("OFFER_NOT_APPROVED", error.getErrorCode());
                return false;
            }
        };
        try {
            var first = executor.submit(publish);
            var second = executor.submit(publish);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(1, (first.get(30, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(30, TimeUnit.SECONDS) ? 1 : 0));
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                assertEquals("Sent_Candidate", entityManager.find(OfferProposal.class, id).getOfferStatus());
                assertEquals("Offered", entityManager.find(Application.class, applicationId).getApplicationStatus());
            });
            verify(mailSender, times(1)).sendOfferLetterToCandidate(any());
            verify(notificationService, times(1)).notifyOfferLetterSent(any());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
