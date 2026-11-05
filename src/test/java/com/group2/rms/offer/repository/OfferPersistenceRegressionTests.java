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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

/** Opt-in SQL Server regression tests. Schema creation is restricted to a dedicated temporary database. */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OfferServiceImpl.class, ClockConfig.class, SchemaNamingConfig.class})
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
    }

    @Autowired EntityManager entityManager;
    @Autowired OfferService service;
    @MockitoBean OfferMailSender mailSender;
    @MockitoBean com.group2.rms.notification.service.NotificationService notificationService;
    private OfferProposal offer;

    @BeforeEach
    void createSyntheticOffer() {
        Role role = Role.builder().roleName("HR").build();
        entityManager.persist(role);
        User hr = User.builder().role(role).username("offer_regression")
                .passwordHash("test-only").email("offer-regression@example.invalid")
                .fullName("Offer Regression").accountStatus("Active").build();
        entityManager.persist(hr);
        Department department = Department.builder().departmentName("Offer Regression")
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
}
