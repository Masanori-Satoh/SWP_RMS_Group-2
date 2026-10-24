package com.group2.rms.candidate.service;

import com.group2.rms.candidate.cv.CvStorage;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @DisplayName("Lưu bookmark không ghi chú: đánh dấu potential, ghi audit và giữ trạng thái application")
    void savingWithoutNoteChangesOnlyCandidateFlagAndAudit() {
        // Given: Lưu bookmark không ghi chú.
        allowView();
        // When: Gọi pool.saveBookmark với dữ liệu đã chuẩn bị.
        var result = pool.saveBookmark(218, new TalentPoolBookmarkRequest("  "));
        // Then: đánh dấu potential, ghi audit và giữ trạng thái application.
        assertEquals(true, result.saved());
        assertEquals(true, candidate.isPotential());
        assertEquals("HR_Passed", application.getApplicationStatus());
        verify(applications, times(3)).findDetailById(218);
        verify(audit, times(1)).record(5, "UPDATE", "Candidate", 8, "false", "TalentPoolMarked");
        verify(notes, times(0)).save(any());
        verify(jobs, times(0)).findById(any());
        verify(jobs, times(0)).findForUpdate(any());
        verify(jobs, times(0)).findAll();
        verify(storage, times(0)).load(any());
        verify(candidates, times(0)).save(any());
        verify(candidates, times(0)).findById(any());
        verify(candidates, times(0)).findByAccountUserId(any());
        verify(candidates, times(0)).findAllByAccountUserIdIn(any());
    }

    @Test
    @DisplayName("Thêm ghi chú đã trim cho Candidate tiềm năng; giữ ghi chú cũ và trạng thái Rejected")
    void savingExistingCandidateAppendsNoteAndPreservesRejectedStatus() {
        // Given: Thêm ghi chú đã trim cho Candidate tiềm năng; giữ ghi chú cũ và trạng thái Rejected.
        allowView();
        candidate.setPotential(true);
        application.setApplicationStatus("Rejected");
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        var previous = TalentPoolNote.builder().noteId(1).candidate(candidate).author(hm)
            .noteContent("Ghi chú trước").createdAt(LocalDateTime.of(2026, 10, 16, 17, 0)).build();
        when(notes.findByCandidate_CandidateIdOrderByCreatedAtDescNoteIdDesc(8)).thenReturn(List.of(previous));
        TalentPoolNote expectedNote = TalentPoolNote.builder().candidate(candidate).author(hm).noteContent("Ghi chú mới").build();

        // When: Gọi pool.saveBookmark với dữ liệu đã chuẩn bị.
        var result = pool.saveBookmark(218, new TalentPoolBookmarkRequest("  Ghi chú mới  "));
        // Then: Thêm ghi chú đã trim cho Candidate tiềm năng; giữ ghi chú cũ và trạng thái Rejected.
        verify(notes, times(1)).save(refEq(expectedNote));
        assertEquals("Ghi chú trước", previous.getNoteContent());
        assertEquals("Ghi chú trước", result.notes().getFirst().content());
        assertEquals("Rejected", application.getApplicationStatus());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
        verify(jobs, times(0)).findById(any());
        verify(jobs, times(0)).findForUpdate(any());
        verify(jobs, times(0)).findAll();
        verify(storage, times(0)).load(any());
        verify(applications, times(3)).findDetailById(218);
    }

    @Test
    @DisplayName("Bỏ bookmark: tắt potential, giữ application và lịch sử ghi chú")
    void removingBookmarkPreservesApplicationAndNoteHistory() {
        // Given: Bỏ bookmark.
        allowView();
        candidate.setPotential(true);
        var previous = TalentPoolNote.builder().noteId(1).candidate(candidate).author(hm)
            .noteContent("Ghi chú trước").createdAt(LocalDateTime.of(2026, 10, 16, 17, 0)).build();
        when(notes.findByCandidate_CandidateIdOrderByCreatedAtDescNoteIdDesc(8)).thenReturn(List.of(previous));
        // When: Gọi pool.removeBookmark với dữ liệu đã chuẩn bị.
        var result = pool.removeBookmark(218);
        // Then: tắt potential, giữ application và lịch sử ghi chú.
        assertEquals(false, result.saved());
        assertEquals(false, candidate.isPotential());
        assertEquals("HR_Passed", application.getApplicationStatus());
        assertEquals("Ghi chú trước", result.notes().getFirst().content());
        assertEquals("Ghi chú trước", pool.bookmark(218).notes().getFirst().content());
        verify(audit, times(1)).record(5, "UPDATE", "Candidate", 8, "true", "TalentPoolUnmarked");
        verify(applications, times(2)).findDetailById(218);
        verify(notes, times(2)).findByCandidate_CandidateIdOrderByCreatedAtDescNoteIdDesc(8);
        verify(candidates, times(0)).save(any());
        verify(candidates, times(0)).findById(any());
        verify(candidates, times(0)).findByAccountUserId(any());
        verify(candidates, times(0)).findAllByAccountUserIdIn(any());
        verify(jobs, times(0)).findById(any());
        verify(jobs, times(0)).findForUpdate(any());
        verify(jobs, times(0)).findAll();
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("Bỏ bookmark lặp không ghi audit lần hai; vẫn có thể đánh dấu lại")
    void repeatedRemovalDoesNotWriteAnotherAuditAndCandidateCanBeSavedAgain() {
        // Given: Bỏ bookmark lặp không ghi audit lần hai; vẫn có thể đánh dấu lại.
        allowView();
        candidate.setPotential(true);
        // When: Gọi pool.removeBookmark với dữ liệu đã chuẩn bị.
        pool.removeBookmark(218);
        pool.removeBookmark(218);
        // Then: Bỏ bookmark lặp không ghi audit lần hai; vẫn có thể đánh dấu lại.
        assertEquals(false, candidate.isPotential());
        verify(audit, times(1)).record(5, "UPDATE", "Candidate", 8, "true", "TalentPoolUnmarked");
        assertEquals(true, pool.saveBookmark(218, new TalentPoolBookmarkRequest(null)).saved());
        assertEquals("HR_Passed", application.getApplicationStatus());
        verify(notes, times(0)).save(any());
        verify(applications, times(5)).findDetailById(218);
    }

    @Test
    @DisplayName("Không có quyền xem application: chặn đọc/thêm/bỏ bookmark, không đổi dữ liệu")
    void deniedApplicationCannotReadNotesOrSave() {
        // Given: Không có quyền xem application.
        allowView();
        doThrow(new AccessDeniedException("Forbidden")).when(access).requireView(hm, application);
        // When: Gọi pool.bookmark với dữ liệu đã chuẩn bị.
        assertThrows(AccessDeniedException.class, () -> pool.bookmark(218));
        // Then: chặn đọc/thêm/bỏ bookmark, không đổi dữ liệu.
        assertThrows(AccessDeniedException.class, () -> pool.saveBookmark(218, new TalentPoolBookmarkRequest("x")));
        assertThrows(AccessDeniedException.class, () -> pool.removeBookmark(218));
        assertEquals(false, candidate.isPotential());
        verify(notes, times(0)).save(any());
        verify(notes, times(0)).findByCandidate_CandidateIdOrderByCreatedAtDescNoteIdDesc(any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
        verify(candidates, times(0)).save(any());
        verify(candidates, times(0)).findById(any());
        verify(candidates, times(0)).findByAccountUserId(any());
        verify(candidates, times(0)).findAllByAccountUserIdIn(any());
        verify(jobs, times(0)).findById(any());
        verify(jobs, times(0)).findForUpdate(any());
        verify(jobs, times(0)).findAll();
    }

    @Test
    @DisplayName("Role Candidate không được xem hoặc bỏ bookmark nội bộ")
    void candidateRoleCannotReadSharedNotes() {
        // Given: Role Candidate không được xem hoặc bỏ bookmark nội bộ.
        hm.setRole(Role.builder().roleName("Candidate").build());
        when(currentUser.requireUser()).thenReturn(hm);
        // When: Gọi pool.bookmark với dữ liệu đã chuẩn bị.
        assertThrows(AccessDeniedException.class, () -> pool.bookmark(218));
        // Then: Role Candidate không được xem hoặc bỏ bookmark nội bộ.
        assertThrows(AccessDeniedException.class, () -> pool.removeBookmark(218));
        verify(applications, times(0)).findDetailById(any());
        verify(applications, times(0)).saveAndFlush(any());
        verify(applications, times(0)).transition(anyInt(), anyCollection(), anyString());
        verify(applications, times(0)).promoteSuggestionToHmPassed(anyInt());
        verify(notes, times(0)).save(any());
        verify(notes, times(0)).findByCandidate_CandidateIdOrderByCreatedAtDescNoteIdDesc(any());
    }

    @Test
    @DisplayName("Ghi chú 1001 ký tự: từ chối trước mọi thay đổi")
    void tooLongNoteIsRejectedBeforeAnyMutation() {
        // Given: Ghi chú 1001 ký tự.
        assertThrows(TalentPoolException.class,
            // When: Gọi pool.saveBookmark với dữ liệu đã chuẩn bị.
            () -> pool.saveBookmark(218, new TalentPoolBookmarkRequest("x".repeat(1001))));
        // Then: từ chối trước mọi thay đổi.
        assertEquals(false, candidate.isPotential());
        verify(applications, times(0)).findDetailById(any());
        verify(applications, times(0)).saveAndFlush(any());
        verify(applications, times(0)).transition(anyInt(), anyCollection(), anyString());
        verify(applications, times(0)).promoteSuggestionToHmPassed(anyInt());
        verify(notes, times(0)).save(any());
        verify(notes, times(0)).findByCandidate_CandidateIdOrderByCreatedAtDescNoteIdDesc(any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }
}
