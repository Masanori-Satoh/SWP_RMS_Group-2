package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.*;
import com.group2.rms.candidate.entity.*;
import com.group2.rms.candidate.exception.TalentPoolException;
import com.group2.rms.candidate.repository.*;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class TalentPoolRecommendationTests {
    @Mock CandidateRepository candidates;
    @Mock ApplicationRepository applications;
    @Mock ApplicationReviewRepository reviews;
    @Mock TalentPoolNoteRepository notes;
    @Mock TalentPoolAuditRepository audit;
    @Mock JobPostingRepository jobs;
    @Mock CurrentUserService current;
    @Mock RequisitionAccess access;
    @Mock ApplicationAccess applicationAccess;
    @Mock UserRepository users;
    @Mock CvStorage storage;
    @InjectMocks TalentPoolService pool;
    User hr, hm;
    JobPosting target;
    Application draft;

    @BeforeEach void fixture() {
        hr = User.builder().userId(4).fullName("HR Lan").role(Role.builder().roleName("HR").build()).build();
        hm = User.builder().userId(5).fullName("HM Tuấn").role(Role.builder().roleName("Hiring Manager").build()).build();
        target = JobPosting.builder().jobPostingId(8).postingStatus("Published")
                .requisition(JobRequisition.builder().hiringManager(hm).build()).build();
        draft = Application.builder().applicationId(219).jobPosting(target).applicationStatus("HM_Suggested")
                .candidate(Candidate.builder().candidateId(11).isPotential(true).build()).appliedCvUrl("local:cv/old.pdf").build();
    }
    void targetFor(User actor) {
        when(current.requireUser()).thenReturn(actor);
        when(jobs.findForUpdate(8)).thenReturn(Optional.of(target));
    }
    void pendingForHm() {
        targetFor(hm);
        when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        when(access.inManagedDepartment(hm, target.getRequisition())).thenReturn(true);
        when(applications.countPendingHrRecommendation(219)).thenReturn(1L);
    }
    Application source(int id, int cid) {
        var source = Application.builder().applicationId(id).candidate(Candidate.builder().candidateId(cid).isPotential(true).build())
                .jobPosting(JobPosting.builder().jobPostingId(3).build()).applicationStatus("Rejected").appliedCvUrl("local:cv/old.pdf").build();
        when(applications.findDetailById(id)).thenReturn(Optional.of(source));
        lenient().when(applications.findPreviousCvByCandidateId(cid)).thenReturn(List.of(source));
        return source;
    }
    @Test void hrCreatesSeparateDraftWithStructuredHoldAndNoCandidateOrSourceEdits() {
        targetFor(hr); var original = source(101, 11);
        when(applications.saveAndFlush(any())).thenAnswer(call -> { Application a = call.getArgument(0); a.setApplicationId(219); return a; });
        pool.recommend(8, new TalentPoolSuggestRequest(List.of(101)));
        var captured = ArgumentCaptor.forClass(ApplicationReview.class);
        verify(reviews).saveAndFlush(captured.capture());
        assertEquals("HR", captured.getValue().getReviewerRole()); assertEquals("Hold", captured.getValue().getDecision());
        assertSame(hr, captured.getValue().getReviewer()); assertNull(captured.getValue().getComments());
        var saved = captured.getValue().getApplication();
        assertNotSame(original, saved); assertSame(original.getCandidate(), saved.getCandidate());
        assertSame(target, saved.getJobPosting()); assertEquals(original.getAppliedCvUrl(), saved.getAppliedCvUrl());
        assertEquals("HM_Suggested", saved.getApplicationStatus()); assertEquals("Rejected", original.getApplicationStatus());
        verify(audit).record(4,"CREATE","Application",219,"101","TalentPoolHrRecommended");
        verifyNoInteractions(candidates, notes);
    }
    @Test void invalidBatchDoesNotInsertEvenFirstDraft() {
        targetFor(hr); source(101, 11); source(102, 12).getCandidate().setPotential(false);
        assertThrows(TalentPoolException.class, () -> pool.recommend(8, new TalentPoolSuggestRequest(List.of(101,102))));
        verify(applications, never()).saveAndFlush(any()); verifyNoInteractions(reviews, audit);
    }
    @Test void existingHmOrHrDraftBlocksHrRecommendation() {
        targetFor(hr); source(101,11);
        when(applications.findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(11,8,"HM_Suggested")).thenReturn(Optional.of(draft));
        assertThrows(TalentPoolException.class, () -> pool.recommend(8,new TalentPoolSuggestRequest(List.of(101))));
        verifyNoInteractions(reviews,audit);
    }
    @Test void hmCannotSendAsHr() {
        when(current.requireUser()).thenReturn(hm);
        assertThrows(AccessDeniedException.class, () -> pool.recommend(8,new TalentPoolSuggestRequest(List.of(101))));
        verifyNoInteractions(applications,jobs,reviews,audit);
    }
    @Test void acceptKeepsSameDraftAndRecordsHmPassOnlyAfterLockAndPendingCheck() {
        pendingForHm(); pool.decideRecommendation(219,true);
        assertEquals("HM_Suggested", draft.getApplicationStatus());
        verify(applications,never()).saveAndFlush(any()); verify(applications,never()).promoteSuggestionToHmPassed(anyInt());
        var captured = ArgumentCaptor.forClass(ApplicationReview.class); verify(reviews).saveAndFlush(captured.capture());
        assertEquals("Pass",captured.getValue().getDecision()); assertEquals("HiringManager",captured.getValue().getReviewerRole());
        assertSame(draft,captured.getValue().getApplication());
        verify(audit).record(5,"UPDATE","Application",219,"HM_Suggested","TalentPoolSuggested");
        var order = inOrder(jobs, applications, reviews); order.verify(jobs).findForUpdate(8);
        order.verify(applications).countPendingHrRecommendation(219); order.verify(reviews).saveAndFlush(any());
        verifyNoInteractions(candidates,notes);
    }
    @Test void rejectUsesExistingRejectedAndFailAndPreservesProfile() {
        pendingForHm(); when(applications.transition(219,Set.of("HM_Suggested"),"Rejected")).thenReturn(1);
        pool.decideRecommendation(219,false);
        verify(reviews).saveAndFlush(argThat(r -> "Fail".equals(r.getDecision()) && "HiringManager".equals(r.getReviewerRole())));
        verify(audit).record(5,"UPDATE","Application",219,"HM_Suggested","TalentPoolCancelled");
        verifyNoInteractions(candidates,notes,storage);
    }
    @Test void alreadyHandledCannotBeAcceptedOrRejectedAgain() {
        pendingForHm(); when(applications.countPendingHrRecommendation(219)).thenReturn(0L);
        assertThrows(TalentPoolException.class, () -> pool.decideRecommendation(219,true));
        assertThrows(TalentPoolException.class, () -> pool.decideRecommendation(219,false));
        verifyNoInteractions(reviews,audit);
    }
    @Test void anotherDepartmentCannotDecide() {
        targetFor(hm); when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        assertThrows(AccessDeniedException.class, () -> pool.decideRecommendation(219,true));
        verifyNoInteractions(reviews,audit);
        verify(applications,never()).countPendingHrRecommendation(anyInt());
    }
    @Test void hrCannotAcceptForHm() {
        when(current.requireUser()).thenReturn(hr);
        assertThrows(AccessDeniedException.class, () -> pool.decideRecommendation(219,true));
        verifyNoInteractions(applications,reviews,audit);
    }
    @Test void hrCannotActivateOrDeclineBeforeHmDecision() {
        targetFor(hr); when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        when(applications.countPendingHrRecommendation(219)).thenReturn(1L);
        assertThrows(TalentPoolException.class, () -> pool.confirm(219,new TalentPoolConfirmRequest(true)));
        assertThrows(TalentPoolException.class, () -> pool.decline(219));
        verify(applications,never()).promoteSuggestionToHmPassed(anyInt()); verify(applications,never()).transition(anyInt(),anyCollection(),anyString());
        verifyNoInteractions(reviews,audit);
    }
    @Test void pausedJobCannotBeAccepted() {
        targetFor(hm); when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        when(access.inManagedDepartment(hm,target.getRequisition())).thenReturn(true); target.setPostingStatus("Paused");
        assertThrows(TalentPoolException.class, () -> pool.decideRecommendation(219,true)); verifyNoInteractions(reviews,audit);
    }
    @Test void missingReceivingManagerRejectsHrDraftBeforeAnyInsert() {
        targetFor(hr); target.getRequisition().setHiringManager(null);
        assertThrows(TalentPoolException.class, () -> pool.recommend(8,new TalentPoolSuggestRequest(List.of(101))));
        verifyNoInteractions(applications,reviews,audit);
    }
    @Test void laterFormalReviewDoesNotRewriteOriginalHmAcceptance() {
        when(current.requireUser()).thenReturn(hm);
        when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        when(access.inManagedDepartment(hm,target.getRequisition())).thenReturn(true);
        when(audit.has("Application",219,"TalentPoolHrRecommended")).thenReturn(true);
        draft.setApplicationStatus("Rejected"); draft.getCandidate().setAccount(User.builder().fullName("Candidate").build());
        var origin=ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Hold").reviewedAt(java.time.LocalDateTime.now()).build();
        var accepted=ApplicationReview.builder().reviewId(2).reviewer(hm).reviewerRole("HiringManager").decision("Pass").build();
        var laterFailure=ApplicationReview.builder().reviewId(3).reviewer(hm).reviewerRole("HiringManager").decision("Fail").build();
        when(reviews.findWithReviewerByApplicationId(219)).thenReturn(List.of(laterFailure,origin,accepted));
        var response=pool.recommendation(219,11);
        assertEquals("HM đã chấp nhận",response.statusLabel()); assertFalse(response.pending()); assertFalse(response.canDecide());
    }

    void pendingForHr() {
        targetFor(hr);
        when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        when(audit.has("Application",219,"TalentPoolHrRecommended")).thenReturn(true);
    }
    @Test void hrWithdrawalCancelsOnlyUnansweredDraftUnderTheSameJobLock() {
        pendingForHr(); target.setPostingStatus("Paused");
        when(applications.countPendingHrRecommendation(219)).thenReturn(1L);
        when(applications.transition(219,Set.of("HM_Suggested"),"Rejected")).thenReturn(1);
        pool.withdrawRecommendation(8,219);
        var order=inOrder(jobs,applications,audit);
        order.verify(jobs).findForUpdate(8);
        order.verify(applications).countPendingHrRecommendation(219);
        order.verify(applications).transition(219,Set.of("HM_Suggested"),"Rejected");
        order.verify(audit).record(4,"UPDATE","Application",219,"HM_Suggested","TalentPoolCancelled");
        order.verify(audit).record(4,"UPDATE","Application",219,"HM_Suggested","TalentPoolHrWithdrawn");
        verifyNoInteractions(candidates,notes,reviews,storage);
    }
    @Test void processedRecommendationCannotBeWithdrawn() {
        pendingForHr(); when(applications.countPendingHrRecommendation(219)).thenReturn(0L);
        assertThrows(TalentPoolException.class,()->pool.withdrawRecommendation(8,219));
        verify(applications,never()).transition(anyInt(),anyCollection(),anyString());
        verify(audit,never()).record(any(),any(),any(),any(),any(),any());
    }
    @Test void lostTransitionCannotReportWithdrawalOrWriteAudit() {
        pendingForHr(); when(applications.countPendingHrRecommendation(219)).thenReturn(1L);
        assertThrows(TalentPoolException.class,()->pool.withdrawRecommendation(8,219));
        verify(audit,never()).record(any(),any(),any(),any(),any(),any());
    }
    @Test void wrongPostingAndHmOriginDraftCannotBeWithdrawn() {
        targetFor(hr); draft.setJobPosting(JobPosting.builder().jobPostingId(9).build());
        when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        assertThrows(com.group2.rms.core.exception.ResourceNotFoundException.class,()->pool.withdrawRecommendation(8,219));
        draft.setJobPosting(target);
        assertThrows(com.group2.rms.core.exception.ResourceNotFoundException.class,()->pool.withdrawRecommendation(8,219));
        verify(applications,never()).transition(anyInt(),anyCollection(),anyString());
    }
    @Test void hmCannotWithdrawHrRecommendation() {
        when(current.requireUser()).thenReturn(hm);
        assertThrows(AccessDeniedException.class,()->pool.withdrawRecommendation(8,219));
        verifyNoInteractions(applications,jobs,reviews,audit);
    }
    @Test void withdrawnRecommendationCannotBeOpenedFromStaleLink() {
        when(current.requireUser()).thenReturn(hr); when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        when(audit.has("Application",219,"TalentPoolHrRecommended")).thenReturn(true);
        when(audit.has("Application",219,"TalentPoolHrWithdrawn")).thenReturn(true);
        assertThrows(com.group2.rms.core.exception.ResourceNotFoundException.class,()->pool.recommendation(219,11));
        verifyNoInteractions(reviews);
    }
    @Test void recommendLinkRequiresAnOpenPostingWithReceivingHm() {
        when(current.requireUser()).thenReturn(hr); when(jobs.findById(8)).thenReturn(Optional.of(target));
        assertTrue(pool.canRecommend(8));
        target.setPostingStatus("Paused"); assertFalse(pool.canRecommend(8));
        target.setPostingStatus("Published"); target.setApplicationDeadline(java.time.LocalDateTime.now().minusDays(1));
        assertFalse(pool.canRecommend(8)); target.setApplicationDeadline(null);
        target.getRequisition().setHiringManager(null); assertFalse(pool.canRecommend(8));
    }
}
