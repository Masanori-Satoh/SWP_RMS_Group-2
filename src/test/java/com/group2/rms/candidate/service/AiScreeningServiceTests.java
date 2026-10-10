package com.group2.rms.candidate.service;

import com.group2.rms.candidate.entity.AIScreeningResult;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.repository.AIScreeningResultRepository;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.requisition.repository.ScreeningCriteriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Chấm đơn: repository và AI client là mock. Câu update có điều kiện của {@code markScreened}
 * chạy trên DB thật nên ở đây chỉ kiểm tra số dòng nó trả về được xử lý đúng.
 */
@ExtendWith(MockitoExtension.class)
class AiScreeningServiceTests {

    private static final int APPLICATION_ID = 300;
    private static final int REQUISITION_ID = 7;
    private static final BigDecimal SCORE = new BigDecimal("81.25");

    @Mock ApplicationRepository applications;
    @Mock AIScreeningResultRepository screeningResults;
    @Mock ScreeningCriteriaRepository screeningCriteria;
    @Mock AiScreeningClient aiScreeningClient;

    private AiScreeningService service;
    private Application application;

    @BeforeEach
    void setUp() {
        service = new AiScreeningService(applications, screeningResults, screeningCriteria, aiScreeningClient);
        JobPosting job = JobPosting.builder()
                .postingTitle("Java Developer")
                .requisition(JobRequisition.builder().requisitionId(REQUISITION_ID).build())
                .build();
        application = Application.builder()
                .applicationId(APPLICATION_ID)
                .jobPosting(job)
                .appliedCvUrl("local:cv/15/abc.pdf")
                .applicationStatus("Applied")
                .build();
    }

    @Test
    void savesScoreAndMarksApplicationScreened() {
        when(applications.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(screeningCriteria.findByRequisition_RequisitionId(REQUISITION_ID)).thenReturn(List.of(
                ScreeningCriteria.builder().criteriaName("Java").criteriaType("Skill").requiredValue("Spring Boot")
                        .weight(new BigDecimal("2.00")).isMandatory(true).build()));
        when(aiScreeningClient.score(any())).thenReturn(new AiScreeningOutcome(SCORE));
        when(applications.markScreened(APPLICATION_ID)).thenReturn(1);

        AiScreeningOutcome outcome = service.screen(APPLICATION_ID);

        assertEquals(SCORE, outcome.matchScore());
        ArgumentCaptor<AiScreeningInput> input = ArgumentCaptor.forClass(AiScreeningInput.class);
        verify(aiScreeningClient).score(input.capture());
        assertEquals("local:cv/15/abc.pdf", input.getValue().cvUrl());
        assertEquals("Java Developer", input.getValue().jobTitle());
        assertEquals(List.of(new AiScreeningInput.Criterion("Java", "Skill", "Spring Boot",
                new BigDecimal("2.00"), true)), input.getValue().criteria());

        ArgumentCaptor<AIScreeningResult> saved = ArgumentCaptor.forClass(AIScreeningResult.class);
        verify(screeningResults).save(saved.capture());
        assertSame(application, saved.getValue().getApplication());
        assertEquals(SCORE, saved.getValue().getAiMatchScore());
        verify(applications).markScreened(APPLICATION_ID);
    }

    @Test
    void keepsScoreWhenHrAlreadyMovedTheApplication() {
        application.setApplicationStatus("HR_Passed");
        when(applications.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(screeningCriteria.findByRequisition_RequisitionId(REQUISITION_ID)).thenReturn(List.of());
        when(aiScreeningClient.score(any())).thenReturn(new AiScreeningOutcome(SCORE));
        when(applications.markScreened(APPLICATION_ID)).thenReturn(0);

        assertEquals(SCORE, service.screen(APPLICATION_ID).matchScore());

        verify(screeningResults).save(any(AIScreeningResult.class));
        assertEquals("HR_Passed", application.getApplicationStatus());
    }

    @Test
    void missingApplicationIsNotFound() {
        when(applications.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.screen(APPLICATION_ID));

        verifyNoInteractions(aiScreeningClient, screeningResults);
    }
}
