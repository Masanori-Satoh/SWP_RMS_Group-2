package com.group2.rms.candidate.talentpool.service;

import com.group2.rms.candidate.domain.rule.ApplicationAccess;
import com.group2.rms.candidate.infra.cv.CvStorage;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.candidate.domain.repository.CandidateRepository;
import com.group2.rms.candidate.talentpool.repository.TalentPoolApplicationRepository;
import com.group2.rms.candidate.talentpool.dto.TalentPoolSuggestRequest;
import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.candidate.talentpool.repository.TalentPoolAuditRepository;
import com.group2.rms.candidate.talentpool.repository.TalentPoolNoteRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TalentPoolSelectionReviewTests {
    @Mock CandidateRepository candidates;
    @Mock ApplicationRepository applications;
    @Mock TalentPoolApplicationRepository poolApplications;
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
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
        verify(notes, times(0)).save(any());
        verify(candidates, times(0)).save(any());
        verify(users, times(0)).save(any());
        verify(users, times(0)).saveAndFlush(any());
        verify(applications, times(0)).saveAndFlush(any());
        verify(applications, times(0)).transition(anyInt(), anyCollection(), anyString());
        verify(applications, times(0)).promoteSuggestionToHmPassed(anyInt());
        verify(applications, times(0)).delete(any(Application.class));
        verify(applications, times(0)).deleteById(any());
        verify(jobs, times(0)).save(any());
        verify(jobs, times(0)).saveAndFlush(any());
        verify(jobs, times(0)).findForUpdate(any());
        verify(jobs, times(0)).delete(any(JobPosting.class));
        verify(jobs, times(0)).deleteById(any());
    }

    @Test
    @DisplayName("Preflight CV nguồn hợp lệ: trả candidate, CV, ngày nộp, không lỗi và không ghi dữ liệu")
    void validSelectionIsCheckedReadOnlyAndEnablesSubmission() {
        // Given: Preflight CV nguồn hợp lệ.
        allowTarget();
        var source = source(101, 11);
        when(poolApplications.findPreviousCvByCandidateId(11)).thenReturn(List.of(source));
        // When: Gọi pool.reviewSelection với dữ liệu đã chuẩn bị.
        var result = pool.reviewSelection(8, new TalentPoolSuggestRequest(List.of(101)));
        // Then: trả candidate, CV, ngày nộp, không lỗi và không ghi dữ liệu.
        assertEquals(true, result.submissionSupported());
        assertEquals(source.getSubmissionDate(), result.items().getFirst().submittedAt());
        assertEquals(null, result.items().getFirst().error());
        assertEquals(11, result.items().getFirst().candidateId());
        assertEquals(101, result.items().getFirst().sourceApplicationId());
        assertEquals("Rejected", source.getApplicationStatus());
        assertEquals(true, source.getCandidate().isPotential());
    }

    @Test
    @DisplayName("Preflight trả lỗi riêng cho Candidate chọn trùng và application không tồn tại")
    void duplicateCandidateAndMissingSourceAreReportedPerRow() {
        // Given: Preflight trả lỗi riêng cho Candidate chọn trùng và application không tồn tại.
        allowTarget();
        var first = source(101, 11);
        var duplicate = source(102, 11);
        when(poolApplications.findPreviousCvByCandidateId(11)).thenReturn(List.of(first, duplicate));
        // When: Gọi pool.reviewSelection với dữ liệu đã chuẩn bị.
        var result = pool.reviewSelection(8, new TalentPoolSuggestRequest(List.of(101, 102, 999)));
        // Then: Preflight trả lỗi riêng cho Candidate chọn trùng và application không tồn tại.
        assertEquals(null, result.items().get(0).error());
        assertEquals("Ứng viên được chọn nhiều lần.", result.items().get(1).error());
        assertNotNull(result.items().get(2).error());
        assertEquals("Hồ sơ #999", result.items().get(2).candidateName());
    }

    @Test
    @DisplayName("Preflight chặn Candidate đã có application chính thức hoặc draft cùng tin")
    void existingApplicationOrSuggestionBlocksThatCandidate() {
        // Given: Preflight chặn Candidate đã có application chính thức hoặc draft cùng tin.
        allowTarget();
        var first = source(101, 11);
        var second = source(102, 12);
        when(poolApplications.findPreviousCvByCandidateId(11)).thenReturn(List.of(first));
        when(poolApplications.findPreviousCvByCandidateId(12)).thenReturn(List.of(second));
        when(applications.existsActiveByCandidateAndJobPosting(11, 8)).thenReturn(true);
        when(applications.findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(12, 8, "HM_Suggested"))
            .thenReturn(Optional.of(second));
        // When: Gọi pool.reviewSelection với dữ liệu đã chuẩn bị.
        var result = pool.reviewSelection(8, new TalentPoolSuggestRequest(List.of(101, 102)));
        // Then: Preflight chặn Candidate đã có application chính thức hoặc draft cùng tin.
        assertEquals(true, result.items().stream().allMatch(item -> item.error().contains("đã có hồ sơ hoặc đề xuất")));
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("Preflight chặn CV cùng tin đích và Candidate đã bỏ potential")
    void sameJobAndRemovedPotentialCandidateAreRejected() {
        // Given: Preflight chặn CV cùng tin đích và Candidate đã bỏ potential.
        allowTarget();
        var first = source(101, 11);
        var second = source(102, 12);
        first.setJobPosting(target);
        second.getCandidate().setPotential(false);
        // When: Gọi pool.reviewSelection với dữ liệu đã chuẩn bị.
        var result = pool.reviewSelection(8, new TalentPoolSuggestRequest(List.of(101, 102)));
        // Then: Preflight chặn CV cùng tin đích và Candidate đã bỏ potential.
        assertEquals(true, result.items().get(0).error().contains("tin tuyển dụng khác"));
        assertEquals(true, result.items().get(1).error().contains("không còn trong kho"));
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("Preflight trả lỗi CV mất file/không chính thức, không ghi dở danh sách")
    void missingCvFileAndNonFormalSourceAreReportedWithoutPartialWrites() {
        // Given: Preflight trả lỗi CV mất file/không chính thức, không ghi dở danh sách.
        allowTarget();
        var first = source(101, 11);
        source(102, 12);
        when(poolApplications.findPreviousCvByCandidateId(11)).thenReturn(List.of(first));
        when(storage.load("cv/11/existing.pdf")).thenThrow(new ResourceNotFoundException("Không tìm thấy file CV."));
        // When: Gọi pool.reviewSelection với dữ liệu đã chuẩn bị.
        var result = pool.reviewSelection(8, new TalentPoolSuggestRequest(List.of(101, 102)));
        // Then: Preflight trả lỗi CV mất file/không chính thức, không ghi dở danh sách.
        assertEquals("Không tìm thấy file CV.", result.items().get(0).error());
        assertEquals(true, result.items().get(1).error().contains("không còn đủ điều kiện"));
    }

    @Test
    @DisplayName("HM ngoài phòng ban bị chặn trước khi đọc CV nguồn hoặc ghi dữ liệu")
    void wrongDepartmentIsDeniedBeforeReadingCandidates() {
        // Given: HM ngoài phòng ban bị chặn trước khi đọc CV nguồn hoặc ghi dữ liệu.
        when(current.requireUser()).thenReturn(hm);
        when(jobs.findById(8)).thenReturn(Optional.of(target));
        // When: Gọi pool.reviewSelection với dữ liệu đã chuẩn bị.
        assertThrows(AccessDeniedException.class, () -> pool.reviewSelection(8, new TalentPoolSuggestRequest(List.of(101))));
        // Then: HM ngoài phòng ban bị chặn trước khi đọc CV nguồn hoặc ghi dữ liệu.
        verify(applications, times(0)).findDetailById(any());
        verify(applications, times(0)).saveAndFlush(any());
        verify(applications, times(0)).transition(anyInt(), anyCollection(), anyString());
        verify(applications, times(0)).promoteSuggestionToHmPassed(anyInt());
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("HR được preflight toàn tin mà không cần check phòng HM và không ghi dữ liệu")
    void hrCanPreflightItsRecommendationWithoutWriting() {
        // Given: HR được preflight toàn tin mà không cần check phòng HM và không ghi dữ liệu.
        hm.setRole(Role.builder().roleName("HR").build());
        when(current.requireUser()).thenReturn(hm);
        when(jobs.findById(8)).thenReturn(Optional.of(target));
        var first = source(101, 11);
        when(poolApplications.findPreviousCvByCandidateId(11)).thenReturn(List.of(first));
        // When: Gọi pool.reviewSelection với dữ liệu đã chuẩn bị.
        // Then: HR được preflight toàn tin mà không cần check phòng HM và không ghi dữ liệu.
        assertEquals(null, pool.reviewSelection(8, new TalentPoolSuggestRequest(List.of(101))).items().getFirst().error());
        verify(jobAccess, times(0)).inManagedDepartment(any(), any());
    }
}
