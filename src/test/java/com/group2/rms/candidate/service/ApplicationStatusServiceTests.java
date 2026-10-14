package com.group2.rms.candidate.service;

import com.group2.rms.candidate.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Hook đổi trạng thái đơn theo phỏng vấn. Câu {@code transition} thật (đổi có điều kiện) được kiểm ở
 * {@code ApplicationPipelineQueryTests}; ở đây chỉ kiểm tham số: từ trạng thái nào sang trạng thái nào.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationStatusServiceTests {
    @org.junit.jupiter.api.Nested
    class TalentPoolFlows {
        com.group2.rms.candidate.repository.TalentPoolAuditRepository audit = mock(com.group2.rms.candidate.repository.TalentPoolAuditRepository.class);
        com.group2.rms.requisition.repository.JobPostingRepository jobs = mock(com.group2.rms.requisition.repository.JobPostingRepository.class);
        com.group2.rms.core.security.CurrentUserService current = mock(com.group2.rms.core.security.CurrentUserService.class);
        TalentPoolService pool;
        com.group2.rms.user.entity.User hr;
        com.group2.rms.candidate.entity.Application suggestion;

        @BeforeEach
        void arrange() {
            hr = com.group2.rms.user.entity.User.builder().userId(9)
                    .role(com.group2.rms.user.entity.Role.builder().roleName("HR").build()).build();
            when(current.requireUser()).thenReturn(hr);
            pool = new TalentPoolService(mock(com.group2.rms.candidate.repository.CandidateRepository.class), applications,
                    mock(com.group2.rms.candidate.repository.TalentPoolNoteRepository.class), audit, jobs, current,
                    mock(com.group2.rms.requisition.service.RequisitionAccess.class), mock(ApplicationAccess.class),
                    mock(com.group2.rms.user.repository.UserRepository.class), mock(CvStorage.class));
            suggestion = com.group2.rms.candidate.entity.Application.builder().applicationId(ID)
                    .applicationStatus("HM_Suggested").appliedCvUrl("local:cv/15/existing.pdf")
                    .jobPosting(com.group2.rms.requisition.entity.JobPosting.builder().jobPostingId(12).postingStatus("Published").build()).build();
        }
        @Test
        void confirmingOldCvRecordsConsentAndPromotesWithoutAi() {
            when(applications.findDetailById(ID)).thenReturn(java.util.Optional.of(suggestion));
            when(jobs.findForUpdate(12)).thenReturn(java.util.Optional.of(suggestion.getJobPosting()));
            when(applications.promoteSuggestionToHmPassed(ID)).thenReturn(1);
            pool.confirm(ID, new com.group2.rms.candidate.dto.TalentPoolConfirmRequest(true));
            verify(audit).record(9, "UPDATE", "Application", ID, "HM_Suggested", "TalentPoolActivated");
            verify(applications, never()).markScreened(anyInt());
        }
        @Test
        void confirmingWithoutConsentIsRejectedBeforeAnyWrite() {
            org.junit.jupiter.api.Assertions.assertThrows(com.group2.rms.candidate.exception.TalentPoolException.class,
                    () -> pool.confirm(ID, new com.group2.rms.candidate.dto.TalentPoolConfirmRequest(false)));
            verifyNoInteractions(applications, jobs, audit);
        }
        @Test
        void decliningSuggestionPreservesHistoryAndMarksItPrivate() {
            when(applications.findDetailById(ID)).thenReturn(java.util.Optional.of(suggestion));
            when(jobs.findForUpdate(12)).thenReturn(java.util.Optional.of(suggestion.getJobPosting()));
            when(applications.transition(ID, Set.of("HM_Suggested"), "Rejected")).thenReturn(1);
            pool.decline(ID);
            verify(audit).record(9, "UPDATE", "Application", ID, "HM_Suggested", "TalentPoolCancelled");
            verify(applications, never()).delete(any());
        }
        @Test
        void openOfferBlocksClosureBeforeChangingPosting() {
            when(jobs.findForUpdate(12)).thenReturn(java.util.Optional.of(suggestion.getJobPosting()));
            when(audit.hasOpenOffers(12)).thenReturn(true);
            org.junit.jupiter.api.Assertions.assertThrows(com.group2.rms.candidate.exception.TalentPoolException.class, () -> pool.closeJob(12));
            org.junit.jupiter.api.Assertions.assertEquals("Published", suggestion.getJobPosting().getPostingStatus());
            verifyNoInteractions(applications);
        }
    }

    private static final int ID = 298;

    @Mock ApplicationRepository applications;

    private ApplicationStatusService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationStatusService(applications, mock(com.group2.rms.requisition.repository.JobPostingRepository.class));
    }

    @Test
    void schedulingMovesOnlyHiringManagerPassedToInterviewing() {
        when(applications.transition(anyInt(), anyCollection(), anyString())).thenReturn(1);

        service.onInterviewScheduled(ID);

        verify(applications).transition(ID, Set.of("HM_Passed"), "Interviewing");
    }

    @Test
    void passedDecisionMovesOnlyHiringManagerPassedToInterviewing() {
        when(applications.transition(anyInt(), anyCollection(), anyString())).thenReturn(1);

        service.onInterviewDecided(ID, true);

        verify(applications).transition(ID, Set.of("HM_Passed"), "Interviewing");
    }

    @Test
    void failedDecisionRejectsApplicationsStillInInterview() {
        when(applications.transition(anyInt(), anyCollection(), anyString())).thenReturn(1);

        service.onInterviewDecided(ID, false);

        verify(applications).transition(ID, Set.of("HM_Passed", "Interviewing"), "Rejected");
    }

    /** Đơn đã Offered / Hired / Rejected: repository trả 0, hook không được chặn thao tác của module interview. */
    @Test
    void applicationAlreadyMovedOnIsLeftUntouchedWithoutError() {
        when(applications.transition(anyInt(), anyCollection(), anyString())).thenReturn(0);

        assertDoesNotThrow(() -> service.onInterviewScheduled(ID));
        assertDoesNotThrow(() -> service.onInterviewDecided(ID, true));
        assertDoesNotThrow(() -> service.onInterviewDecided(ID, false));
        verify(applications, times(3)).transition(eq(ID), anyCollection(), anyString());
    }
}
