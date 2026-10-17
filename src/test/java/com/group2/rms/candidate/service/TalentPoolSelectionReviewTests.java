package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.TalentPoolSuggestRequest;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.repository.*;
import com.group2.rms.core.exception.ResourceNotFoundException;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TalentPoolSelectionReviewTests {
    @Mock CandidateRepository candidates;
    @Mock ApplicationRepository applications;
    @Mock TalentPoolNoteRepository notes;
    @Mock TalentPoolAuditRepository audit;
    @Mock JobPostingRepository jobs;
    @Mock CurrentUserService current;
    @Mock RequisitionAccess jobAccess;
    @Mock ApplicationAccess applicationAccess;
    @Mock UserRepository users;
    @Mock CvStorage storage;
    @InjectMocks TalentPoolService pool;
    User hm;
    JobPosting target;

    @BeforeEach
    void arrange() {
        hm = User.builder().userId(5).role(Role.builder().roleName("Hiring Manager").build()).build();
        target = JobPosting.builder().jobPostingId(8).postingStatus("Published").build();
    }

    private void allowTarget() {
        when(current.requireUser()).thenReturn(hm);
        when(jobs.findById(8)).thenReturn(Optional.of(target));
        when(jobAccess.inManagedDepartment(hm, target.getRequisition())).thenReturn(true);
    }

    private Application source(int id, int candidateId) {
        var candidate = Candidate.builder().candidateId(candidateId).isPotential(true)
                .account(User.builder().fullName("Ứng viên " + candidateId).build()).build();
        var source = Application.builder().applicationId(id).candidate(candidate)
                .submissionDate(java.time.LocalDateTime.of(2026, 10, 16, 10, 30))
                .applicationStatus("Rejected").appliedCvUrl("local:cv/" + candidateId + "/existing.pdf")
                .jobPosting(JobPosting.builder().jobPostingId(3).postingTitle("Vị trí nguồn").build()).build();
        when(applications.findDetailById(id)).thenReturn(Optional.of(source));
        return source;
    }

    @AfterEach
    void neverWritesRecruitmentOrPoolData() {
        verifyNoInteractions(audit, notes, candidates, users);
        mockingDetails(applications).getInvocations().forEach(invocation ->
                assertTrue(invocation.getMethod().getName().startsWith("find")
                        || invocation.getMethod().getName().startsWith("exists"), "Read-only selection review"));
        mockingDetails(jobs).getInvocations().forEach(invocation ->
                assertEquals("findById", invocation.getMethod().getName()));
    }

    @Test
    void validSelectionIsCheckedWithoutCreatingApplicationsAndReportsMissingSubmissionSupport() {
        allowTarget();
        var source = source(101, 11);
        when(applications.findPreviousCvByCandidateId(11)).thenReturn(List.of(source));
        var result = pool.reviewSelection(8, new TalentPoolSuggestRequest(List.of(101)));
        assertFalse(result.submissionSupported());
        assertEquals(source.getSubmissionDate(), result.items().getFirst().submittedAt());
        assertNull(result.items().getFirst().error());
        assertEquals(11, result.items().getFirst().candidateId());
        assertEquals(101, result.items().getFirst().sourceApplicationId());
        assertEquals("Rejected", source.getApplicationStatus());
        assertTrue(source.getCandidate().isPotential());
    }

    @Test
    void duplicateCandidateAndMissingSourceAreReportedPerRow() {
        allowTarget();
        var first = source(101, 11);
        var duplicate = source(102, 11);
        when(applications.findPreviousCvByCandidateId(11)).thenReturn(List.of(first, duplicate));
        var result = pool.reviewSelection(8, new TalentPoolSuggestRequest(List.of(101, 102, 999)));
        assertNull(result.items().get(0).error());
        assertEquals("Ứng viên được chọn nhiều lần.", result.items().get(1).error());
        assertNotNull(result.items().get(2).error());
        assertEquals("Hồ sơ #999", result.items().get(2).candidateName());
    }

    @Test
    void existingApplicationOrSuggestionBlocksThatCandidate() {
        allowTarget();
        var first = source(101, 11);
        var second = source(102, 12);
        when(applications.findPreviousCvByCandidateId(11)).thenReturn(List.of(first));
        when(applications.findPreviousCvByCandidateId(12)).thenReturn(List.of(second));
        when(applications.existsActiveByCandidateAndJobPosting(11, 8)).thenReturn(true);
        when(applications.findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(12, 8, "HM_Suggested"))
                .thenReturn(Optional.of(second));
        var result = pool.reviewSelection(8, new TalentPoolSuggestRequest(List.of(101, 102)));
        assertTrue(result.items().stream().allMatch(item -> item.error().contains("đã có hồ sơ hoặc đề xuất")));
        verifyNoInteractions(storage);
    }

    @Test
    void sameJobAndRemovedPotentialCandidateAreRejected() {
        allowTarget();
        var first = source(101, 11);
        var second = source(102, 12);
        first.setJobPosting(target);
        second.getCandidate().setPotential(false);
        var result = pool.reviewSelection(8, new TalentPoolSuggestRequest(List.of(101, 102)));
        assertTrue(result.items().get(0).error().contains("tin tuyển dụng khác"));
        assertTrue(result.items().get(1).error().contains("không còn trong kho"));
        verifyNoInteractions(storage);
    }

    @Test
    void missingCvFileAndNonFormalSourceAreReportedWithoutPartialWrites() {
        allowTarget();
        var first = source(101, 11);
        source(102, 12);
        when(applications.findPreviousCvByCandidateId(11)).thenReturn(List.of(first));
        when(storage.load("cv/11/existing.pdf")).thenThrow(new ResourceNotFoundException("Không tìm thấy file CV."));
        var result = pool.reviewSelection(8, new TalentPoolSuggestRequest(List.of(101, 102)));
        assertEquals("Không tìm thấy file CV.", result.items().get(0).error());
        assertTrue(result.items().get(1).error().contains("không còn đủ điều kiện"));
    }

    @Test
    void wrongDepartmentIsDeniedBeforeReadingCandidates() {
        when(current.requireUser()).thenReturn(hm);
        when(jobs.findById(8)).thenReturn(Optional.of(target));
        assertThrows(AccessDeniedException.class, () -> pool.reviewSelection(8, new TalentPoolSuggestRequest(List.of(101))));
        verifyNoInteractions(applications, storage);
    }

    @Test
    void hrCannotReviewASelectionForHm() {
        hm.setRole(Role.builder().roleName("HR").build());
        when(current.requireUser()).thenReturn(hm);
        assertThrows(AccessDeniedException.class, () -> pool.reviewSelection(8, new TalentPoolSuggestRequest(List.of(101))));
        verifyNoInteractions(applications, jobs, storage);
    }
}
