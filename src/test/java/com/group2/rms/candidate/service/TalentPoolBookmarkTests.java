package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.TalentPoolBookmarkRequest;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.entity.TalentPoolNote;
import com.group2.rms.candidate.exception.TalentPoolException;
import com.group2.rms.candidate.repository.*;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TalentPoolBookmarkTests {
    @Mock CandidateRepository candidates;
    @Mock ApplicationRepository applications;
    @Mock TalentPoolNoteRepository notes;
    @Mock TalentPoolAuditRepository audit;
    @Mock JobPostingRepository jobs;
    @Mock CurrentUserService currentUser;
    @Mock RequisitionAccess jobAccess;
    @Mock ApplicationAccess access;
    @Mock UserRepository users;
    @Mock CvStorage storage;
    @InjectMocks TalentPoolService pool;
    User hm;
    Candidate candidate;
    Application application;

    @BeforeEach
    void setUp() {
        hm = User.builder().userId(5).fullName("HM Tuấn")
                .role(Role.builder().roleName("Hiring Manager").build()).build();
        candidate = Candidate.builder().candidateId(8).build();
        application = Application.builder().applicationId(218).candidate(candidate)
                .applicationStatus("HR_Passed").build();
    }

    private void allowView() {
        when(currentUser.requireUser()).thenReturn(hm);
        when(applications.findDetailById(218)).thenReturn(Optional.of(application));
    }

    @Test
    void savingWithoutNoteChangesOnlyCandidateFlagAndAudit() {
        allowView();
        var result = pool.saveBookmark(218, new TalentPoolBookmarkRequest("  "));
        assertTrue(result.saved());
        assertTrue(candidate.isPotential());
        assertEquals("HR_Passed", application.getApplicationStatus());
        verify(applications, atLeastOnce()).findDetailById(218);
        verifyNoMoreInteractions(applications);
        verify(audit).record(5, "UPDATE", "Candidate", 8, "false", "TalentPoolMarked");
        verify(notes, never()).save(any());
        verifyNoInteractions(jobs, storage, candidates);
    }

    @Test
    void savingExistingCandidateAppendsNoteAndPreservesRejectedStatus() {
        allowView();
        candidate.setPotential(true);
        application.setApplicationStatus("Rejected");
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        var previous = TalentPoolNote.builder().noteId(1).candidate(candidate).author(hm)
                .noteContent("Ghi chú trước").createdAt(LocalDateTime.of(2026, 10, 16, 17, 0)).build();
        when(notes.findByCandidate_CandidateIdOrderByCreatedAtDescNoteIdDesc(8)).thenReturn(List.of(previous));
        var result = pool.saveBookmark(218, new TalentPoolBookmarkRequest("  Ghi chú mới  "));
        var captor = ArgumentCaptor.forClass(TalentPoolNote.class);
        verify(notes).save(captor.capture());
        assertEquals("Ghi chú mới", captor.getValue().getNoteContent());
        assertSame(hm, captor.getValue().getAuthor());
        assertEquals("Ghi chú trước", previous.getNoteContent());
        assertEquals("Ghi chú trước", result.notes().getFirst().content());
        assertEquals("Rejected", application.getApplicationStatus());
        verifyNoInteractions(audit, jobs, storage);
        verify(applications, atLeastOnce()).findDetailById(218);
        verifyNoMoreInteractions(applications);
    }

    @Test
    void removingBookmarkPreservesApplicationAndNoteHistory() {
        allowView();
        candidate.setPotential(true);
        var previous = TalentPoolNote.builder().noteId(1).candidate(candidate).author(hm)
                .noteContent("Ghi chú trước").createdAt(LocalDateTime.of(2026, 10, 16, 17, 0)).build();
        when(notes.findByCandidate_CandidateIdOrderByCreatedAtDescNoteIdDesc(8)).thenReturn(List.of(previous));

        var result = pool.removeBookmark(218);
        assertFalse(result.saved());
        assertFalse(candidate.isPotential());
        assertEquals("HR_Passed", application.getApplicationStatus());
        assertEquals("Ghi chú trước", result.notes().getFirst().content());
        assertEquals("Ghi chú trước", pool.bookmark(218).notes().getFirst().content());
        verify(audit).record(5, "UPDATE", "Candidate", 8, "true", "TalentPoolUnmarked");
        verify(applications, times(2)).findDetailById(218);
        verify(notes, times(2)).findByCandidate_CandidateIdOrderByCreatedAtDescNoteIdDesc(8);
        verifyNoMoreInteractions(applications, notes);
        verifyNoInteractions(candidates, jobs, storage);
    }

    @Test
    void repeatedRemovalDoesNotWriteAnotherAuditAndCandidateCanBeSavedAgain() {
        allowView();
        candidate.setPotential(true);
        pool.removeBookmark(218);
        pool.removeBookmark(218);
        assertFalse(candidate.isPotential());
        verify(audit, times(1)).record(5, "UPDATE", "Candidate", 8, "true", "TalentPoolUnmarked");
        assertTrue(pool.saveBookmark(218, new TalentPoolBookmarkRequest(null)).saved());
        assertEquals("HR_Passed", application.getApplicationStatus());
        verify(notes, never()).save(any());
        verify(applications, atLeastOnce()).findDetailById(218);
        verifyNoMoreInteractions(applications);
    }

    @Test
    void deniedApplicationCannotReadNotesOrSave() {
        allowView();
        doThrow(new AccessDeniedException("Forbidden")).when(access).requireView(hm, application);
        assertThrows(AccessDeniedException.class, () -> pool.bookmark(218));
        assertThrows(AccessDeniedException.class, () -> pool.saveBookmark(218, new TalentPoolBookmarkRequest("x")));
        assertThrows(AccessDeniedException.class, () -> pool.removeBookmark(218));
        assertFalse(candidate.isPotential());
        verifyNoInteractions(notes, audit, candidates, jobs);
    }

    @Test
    void candidateRoleCannotReadSharedNotes() {
        hm.setRole(Role.builder().roleName("Candidate").build());
        when(currentUser.requireUser()).thenReturn(hm);
        assertThrows(AccessDeniedException.class, () -> pool.bookmark(218));
        assertThrows(AccessDeniedException.class, () -> pool.removeBookmark(218));
        verifyNoInteractions(applications, notes);
    }

    @Test
    void tooLongNoteIsRejectedBeforeAnyMutation() {
        assertThrows(TalentPoolException.class,
                () -> pool.saveBookmark(218, new TalentPoolBookmarkRequest("x".repeat(1001))));
        assertFalse(candidate.isPotential());
        verifyNoInteractions(applications, notes, audit);
    }
}
