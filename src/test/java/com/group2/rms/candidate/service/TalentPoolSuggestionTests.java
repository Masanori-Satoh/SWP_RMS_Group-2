package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.TalentPoolSuggestRequest;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.exception.TalentPoolException;
import com.group2.rms.candidate.repository.*;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class TalentPoolSuggestionTests {
    @Mock CandidateRepository candidates;
    @Mock ApplicationRepository applications;
    @Mock TalentPoolNoteRepository notes;
    @Mock TalentPoolAuditRepository audit;
    @Mock JobPostingRepository jobs;
    @Mock CurrentUserService currentUser;
    @Mock RequisitionAccess jobAccess;
    @Mock ApplicationAccess applicationAccess;
    @Mock UserRepository users;
    @Mock CvStorage storage;
    @InjectMocks TalentPoolService pool;
    User hm;
    JobPosting target;

    @BeforeEach void arrange() {
        hm = User.builder().userId(5).role(Role.builder().roleName("Hiring Manager").build()).build();
        target = JobPosting.builder().jobPostingId(8).postingStatus("Published").build();
        when(currentUser.requireUser()).thenReturn(hm);
    }
    private void allowTarget() {
        when(jobs.findForUpdate(8)).thenReturn(Optional.of(target));
        when(jobAccess.inManagedDepartment(hm, target.getRequisition())).thenReturn(true);
    }
    private Application source(int id, int cid) {
        Application a = Application.builder().applicationId(id)
                .candidate(Candidate.builder().candidateId(cid).isPotential(true).build())
                .jobPosting(JobPosting.builder().jobPostingId(3).build())
                .applicationStatus("Rejected").appliedCvUrl("local:cv/" + cid + "/old.pdf").build();
        when(applications.findDetailById(id)).thenReturn(Optional.of(a));
        lenient().when(applications.findPreviousCvByCandidateId(cid)).thenReturn(List.of(a));
        return a;
    }
    @Test void createsOnlyDraftApplicationsWithTheExactCandidateAndCvAndPreservesSources() {
        allowTarget();
        var first = source(101, 11); var second = source(102, 12);
        when(applications.saveAndFlush(any())).thenAnswer(call -> {
            Application a = call.getArgument(0); a.setApplicationId(200 + a.getCandidate().getCandidateId()); return a;
        });
        pool.suggest(8, new TalentPoolSuggestRequest(List.of(101, 102)));
        var saved = ArgumentCaptor.forClass(Application.class);
        verify(applications, times(2)).saveAndFlush(saved.capture());
        assertEquals(List.of(first.getCandidate(), second.getCandidate()), saved.getAllValues().stream().map(Application::getCandidate).toList());
        for (Application a : saved.getAllValues()) {
            assertSame(target, a.getJobPosting()); assertEquals("HM_Suggested", a.getApplicationStatus());
            assertEquals("local:cv/" + a.getCandidate().getCandidateId() + "/old.pdf", a.getAppliedCvUrl());
        }
        assertEquals("Rejected", first.getApplicationStatus()); assertEquals("Rejected", second.getApplicationStatus());
        assertEquals(3, first.getJobPosting().getJobPostingId());
        verify(audit).record(5, "CREATE", "Application", 211, "101", "TalentPoolSuggested");
        verify(audit).record(5, "CREATE", "Application", 212, "102", "TalentPoolSuggested");
        var order = inOrder(jobs, applications);
        order.verify(jobs).findForUpdate(8); order.verify(applications).findDetailById(101);
        verifyNoInteractions(candidates, notes, users);
        verify(applications, never()).transition(anyInt(), anyCollection(), anyString());
    }
    @Test void validatesEntireBatchBeforeAnyInsert() {
        allowTarget(); source(101, 11); var second = source(102, 12); second.getCandidate().setPotential(false);
        assertThrows(TalentPoolException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101, 102))));
        verify(applications, never()).saveAndFlush(any()); verifyNoInteractions(audit);
    }
    @Test void existingFormalApplicationBlocksSuggestion() {
        allowTarget(); source(101, 11);
        when(applications.existsActiveByCandidateAndJobPosting(11, 8)).thenReturn(true);
        assertThrows(TalentPoolException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101))));
        verify(applications, never()).saveAndFlush(any()); verifyNoInteractions(audit);
    }
    @Test void existingPendingSuggestionBlocksRepeatSubmission() {
        allowTarget(); var first = source(101, 11);
        when(applications.findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(11, 8, "HM_Suggested")).thenReturn(Optional.of(first));
        assertThrows(TalentPoolException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101))));
        verify(applications, never()).saveAndFlush(any()); verifyNoInteractions(audit);
    }
    @Test void duplicateCandidateWithinBatchIsRejected() {
        allowTarget(); source(101, 11);
        assertThrows(TalentPoolException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101, 101))));
        verify(applications, never()).saveAndFlush(any());
    }
    @Test void missingCvRejectsEntireBatch() {
        allowTarget(); source(101, 11);
        doThrow(new com.group2.rms.core.exception.ResourceNotFoundException("CV missing")).when(storage).load("cv/11/old.pdf");
        assertThrows(RuntimeException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101))));
        verify(applications, never()).saveAndFlush(any());
    }
    @Test void hrCannotCreateHmSuggestion() {
        hm.setRole(Role.builder().roleName("HR").build());
        assertThrows(AccessDeniedException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101))));
        verifyNoInteractions(jobs, applications, audit);
    }
    @Test void wrongDepartmentIsDenied() {
        when(jobs.findForUpdate(8)).thenReturn(Optional.of(target));
        assertThrows(AccessDeniedException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101))));
        verifyNoInteractions(applications, audit);
    }
    @Test void expiredPostingCannotReceiveSuggestions() {
        allowTarget(); target.setApplicationDeadline(LocalDateTime.now().minusMinutes(1));
        assertThrows(TalentPoolException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101))));
        verifyNoInteractions(applications, audit);
    }
}
