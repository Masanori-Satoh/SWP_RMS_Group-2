package com.group2.rms.candidate.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.exception.ApplicationSubmissionException;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.candidate.validator.CvFileValidator;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.repository.JobPostingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Nộp đơn ứng tuyển; repository và nơi lưu file là mock, validator là bản thật.
 * Transaction được giả lập bằng {@link TransactionSynchronizationManager} để kiểm tra việc xóa file khi rollback.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationSubmissionServiceTests {

    private static final String USERNAME = "phong.nguyen";
    private static final int CANDIDATE_ID = 15;
    private static final int JOB_ID = 6;
    private static final MockMultipartFile PDF = new MockMultipartFile("cvFile", "cv.pdf", "application/pdf",
            "%PDF-1.7 sample".getBytes(StandardCharsets.US_ASCII));
    private static final StoredFile STORED = new StoredFile("cv/15/abc.pdf", "local:cv/15/abc.pdf");

    @Mock CandidateRepository candidates;
    @Mock ApplicationRepository applications;
    @Mock JobPostingRepository jobPostings;
    @Mock CvStorage cvStorage;
    @Mock ApplicationEventPublisher events;

    private ApplicationSubmissionService service;
    private final Candidate candidate = Candidate.builder().candidateId(CANDIDATE_ID).build();

    @BeforeEach
    void setUp() {
        service = new ApplicationSubmissionService(candidates, applications, jobPostings,
                new CvFileValidator(), cvStorage, events);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // ------------------------------------------------------------------ thành công

    @Test
    void applySavesAppliedApplicationAndPublishesEvent() {
        givenCandidate();
        givenJob(job("Published", LocalDateTime.now().plusDays(7)));
        when(cvStorage.store(PDF, CANDIDATE_ID)).thenReturn(STORED);
        when(applications.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            a.setApplicationId(301);
            return a;
        });

        Integer id = service.apply(USERNAME, JOB_ID, PDF);

        assertEquals(301, id);
        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(applications).save(saved.capture());
        assertSame(candidate, saved.getValue().getCandidate());
        assertEquals(JOB_ID, saved.getValue().getJobPosting().getJobPostingId());
        assertEquals("local:cv/15/abc.pdf", saved.getValue().getAppliedCvUrl());
        assertEquals("Applied", saved.getValue().getApplicationStatus());
        verify(events).publishEvent(new ApplicationSubmittedEvent(301));
    }

    @Test
    void applyAcceptsJobWithoutDeadline() {
        givenCandidate();
        givenJob(job("Published", null));
        when(cvStorage.store(PDF, CANDIDATE_ID)).thenReturn(STORED);
        when(applications.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.apply(USERNAME, JOB_ID, PDF));
    }

    // ------------------------------------------------------------------ bù trừ file

    @Test
    void storedFileIsDeletedWhenTransactionRollsBack() {
        givenCandidate();
        givenJob(job("Published", null));
        when(cvStorage.store(PDF, CANDIDATE_ID)).thenReturn(STORED);
        when(applications.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        service.apply(USERNAME, JOB_ID, PDF);

        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(cvStorage).delete("cv/15/abc.pdf");
    }

    @Test
    void storedFileIsKeptWhenTransactionCommits() {
        givenCandidate();
        givenJob(job("Published", null));
        when(cvStorage.store(PDF, CANDIDATE_ID)).thenReturn(STORED);
        when(applications.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        service.apply(USERNAME, JOB_ID, PDF);

        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        verify(cvStorage, never()).delete(any());
    }

    @Test
    void storedFileIsDeletedWhenSavingFails() {
        givenCandidate();
        givenJob(job("Published", null));
        when(cvStorage.store(PDF, CANDIDATE_ID)).thenReturn(STORED);
        when(applications.save(any(Application.class))).thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> service.apply(USERNAME, JOB_ID, PDF));
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(cvStorage).delete("cv/15/abc.pdf");
        verify(events, never()).publishEvent(any(Object.class));
    }

    // ------------------------------------------------------------------ bị chặn trước khi lưu file

    @Test
    void missingCandidateProfileIsRejected() {
        when(candidates.findByAccount_UsernameIgnoreCase(USERNAME)).thenReturn(Optional.empty());

        ApplicationSubmissionException e = assertThrows(ApplicationSubmissionException.class,
                () -> service.apply(USERNAME, JOB_ID, PDF));

        assertNull(e.getField());
        assertNothingStored();
    }

    @Test
    void unknownJobIsNotFound() {
        givenCandidate();
        when(jobPostings.findById(JOB_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.apply(USERNAME, JOB_ID, PDF));
        assertNothingStored();
    }

    @ParameterizedTest
    @ValueSource(strings = { "Draft", "Paused", "Closed" })
    void unpublishedJobIsNotFound(String status) {
        givenCandidate();
        givenJob(job(status, LocalDateTime.now().plusDays(7)));

        assertThrows(ResourceNotFoundException.class, () -> service.apply(USERNAME, JOB_ID, PDF));
        assertNothingStored();
    }

    @Test
    void expiredJobIsRejected() {
        givenCandidate();
        givenJob(job("Published", LocalDateTime.now().minusMinutes(1)));

        ApplicationSubmissionException e = assertThrows(ApplicationSubmissionException.class,
                () -> service.apply(USERNAME, JOB_ID, PDF));

        assertNull(e.getField());
        assertEquals("Tin tuyển dụng đã hết hạn nhận hồ sơ.", e.getMessage());
        assertNothingStored();
    }

    @Test
    void duplicateApplicationIsRejected() {
        givenCandidate();
        givenJob(job("Published", null));
        when(applications.existsByCandidate_CandidateIdAndJobPosting_JobPostingId(CANDIDATE_ID, JOB_ID))
                .thenReturn(true);

        ApplicationSubmissionException e = assertThrows(ApplicationSubmissionException.class,
                () -> service.apply(USERNAME, JOB_ID, PDF));

        assertEquals("Bạn đã ứng tuyển vị trí này.", e.getMessage());
        assertNothingStored();
    }

    @Test
    void invalidFileIsRejectedOnCvField() {
        givenCandidate();
        givenJob(job("Published", null));
        MockMultipartFile docx = new MockMultipartFile("cvFile", "cv.docx", "application/msword", new byte[] { 1 });

        ApplicationSubmissionException e = assertThrows(ApplicationSubmissionException.class,
                () -> service.apply(USERNAME, JOB_ID, docx));

        assertEquals(CvFileValidator.FIELD, e.getField());
        assertNothingStored();
    }

    // ------------------------------------------------------------------ hasApplied

    @Test
    void hasAppliedIsFalseForGuest() {
        assertFalse(service.hasApplied(null, JOB_ID));
        verifyNoInteractions(candidates, applications);
    }

    @Test
    void hasAppliedIsFalseForAccountWithoutCandidateProfile() {
        when(candidates.findByAccount_UsernameIgnoreCase("hr_lan")).thenReturn(Optional.empty());

        assertFalse(service.hasApplied("hr_lan", JOB_ID));
    }

    @Test
    void hasAppliedReflectsExistingApplication() {
        givenCandidate();
        when(applications.existsByCandidate_CandidateIdAndJobPosting_JobPostingId(CANDIDATE_ID, JOB_ID))
                .thenReturn(true);

        assertTrue(service.hasApplied(USERNAME, JOB_ID));
    }

    // ------------------------------------------------------------------ helpers

    private void givenCandidate() {
        when(candidates.findByAccount_UsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(candidate));
    }

    private void givenJob(JobPosting job) {
        when(jobPostings.findById(JOB_ID)).thenReturn(Optional.of(job));
    }

    private static JobPosting job(String status, LocalDateTime deadline) {
        return JobPosting.builder().jobPostingId(JOB_ID).postingStatus(status).applicationDeadline(deadline).build();
    }

    private void assertNothingStored() {
        verify(cvStorage, never()).store(any(), any());
        verify(applications, never()).save(any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    private static void completeTransaction(int status) {
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(status));
    }
}
