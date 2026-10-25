package com.group2.rms.candidate.talentpool.service;

import com.group2.rms.candidate.domain.rule.ApplicationAccess;
import com.group2.rms.candidate.infra.cv.CvStorage;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.candidate.domain.repository.CandidateRepository;
import com.group2.rms.candidate.talentpool.repository.TalentPoolApplicationRepository;
import com.group2.rms.candidate.talentpool.dto.TalentPoolSuggestRequest;
import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.candidate.talentpool.exception.TalentPoolException;
import com.group2.rms.candidate.talentpool.repository.TalentPoolAuditRepository;
import com.group2.rms.candidate.talentpool.repository.TalentPoolNoteRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TalentPoolSuggestionTests {
    @Mock CandidateRepository candidates;
    @Mock ApplicationRepository applications;
    @Mock TalentPoolApplicationRepository poolApplications;
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
        lenient().when(poolApplications.findPreviousCvByCandidateId(cid)).thenReturn(List.of(a));
        return a;
    }

    @Test
    @DisplayName("HM tạo đúng draft candidate/CV/tin đích, audit đúng ID và giữ nguyên hồ sơ nguồn")
    void createsOnlyDraftApplicationsWithTheExactCandidateAndCvAndPreservesSources() {
        // Given: Hai CV chính thức thuộc hai ứng viên tiềm năng và tin nguồn 3; HM quản lý tin 8.
        allowTarget();
        Application first = source(101, 11);
        Application second = source(102, 12);
        Application savedFirst = Application.builder().applicationId(211).build();
        Application savedSecond = Application.builder().applicationId(212).build();
        Application expectedFirst = Application.builder().candidate(first.getCandidate()).jobPosting(target)
            .applicationStatus("HM_Suggested").appliedCvUrl("local:cv/11/old.pdf").build();
        Application expectedSecond = Application.builder().candidate(second.getCandidate()).jobPosting(target)
            .applicationStatus("HM_Suggested").appliedCvUrl("local:cv/12/old.pdf").build();
        when(applications.saveAndFlush(refEq(expectedFirst))).thenReturn(savedFirst);
        when(applications.saveAndFlush(refEq(expectedSecond))).thenReturn(savedSecond);

        // When: HM tạo hai đề xuất từ CV cũ.
        pool.suggest(8, new TalentPoolSuggestRequest(List.of(101, 102)));

        // Then: Tạo đúng hai draft, đúng candidate/CV/tin đích; audit dùng ID đã lưu, hồ sơ nguồn không đổi.
        verify(applications, times(1)).saveAndFlush(refEq(expectedFirst));
        verify(applications, times(1)).saveAndFlush(refEq(expectedSecond));
        assertEquals("Rejected", first.getApplicationStatus());
        assertEquals("Rejected", second.getApplicationStatus());
        assertEquals(3, first.getJobPosting().getJobPostingId());
        verify(audit, times(1)).record(5, "CREATE", "Application", 211, "101", "TalentPoolSuggested");
        verify(audit, times(1)).record(5, "CREATE", "Application", 212, "102", "TalentPoolSuggested");
        verify(jobs, times(1)).findForUpdate(8);
        verify(candidates, times(0)).save(any());
        verify(notes, times(0)).save(any());
        verify(users, times(0)).save(any());
        verify(applications, times(0)).transition(anyInt(), anyCollection(), anyString());
    }

    @Test
    @DisplayName("Danh sách có Candidate không còn potential: chặn toàn bộ trước lần insert đầu")
    void validatesEntireBatchBeforeAnyInsert() {
        // Given: Danh sách có Candidate không còn potential.
        allowTarget();
        source(101, 11);
        var second = source(102, 12);
        second.getCandidate().setPotential(false);
        // When: Gọi pool.suggest với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101, 102))));
        // Then: chặn toàn bộ trước lần insert đầu.
        verify(jobs, times(1)).findForUpdate(8);
        verify(applications, times(0)).saveAndFlush(any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Đã có application chính thức cùng tin: không tạo đề xuất trùng")
    void existingFormalApplicationBlocksSuggestion() {
        // Given: Đã có application chính thức cùng tin.
        allowTarget();
        source(101, 11);
        when(applications.existsActiveByCandidateAndJobPosting(11, 8)).thenReturn(true);
        // When: Gọi pool.suggest với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101))));
        // Then: không tạo đề xuất trùng.
        verify(applications, times(0)).saveAndFlush(any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Đã có HM_Suggested cùng tin: không tạo draft lặp")
    void existingPendingSuggestionBlocksRepeatSubmission() {
        // Given: Đã có HM_Suggested cùng tin.
        allowTarget();
        var first = source(101, 11);
        when(applications.findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(11, 8, "HM_Suggested")).thenReturn(Optional.of(first));
        // When: Gọi pool.suggest với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101))));
        // Then: không tạo draft lặp.
        verify(applications, times(0)).saveAndFlush(any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Chọn cùng Candidate hai lần trong batch: từ chối và không insert")
    void duplicateCandidateWithinBatchIsRejected() {
        // Given: Chọn cùng Candidate hai lần trong batch.
        allowTarget();
        source(101, 11);
        // When: Gọi pool.suggest với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101, 101))));
        // Then: từ chối và không insert.
        verify(applications, times(0)).saveAndFlush(any());
    }

    @Test
    @DisplayName("File CV nguồn không tồn tại: từ chối toàn batch và không tạo draft")
    void missingCvRejectsEntireBatch() {
        // Given: File CV nguồn không tồn tại.
        allowTarget();
        source(101, 11);
        doThrow(new com.group2.rms.core.exception.ResourceNotFoundException("CV missing")).when(storage).load("cv/11/old.pdf");
        // When: Gọi pool.suggest với dữ liệu đã chuẩn bị.
        assertThrows(RuntimeException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101))));
        // Then: từ chối toàn batch và không tạo draft.
        verify(applications, times(0)).saveAndFlush(any());
    }

    @Test
    @DisplayName("HR không được gọi nghiệp vụ tạo đề xuất dành cho HM")
    void hrCannotCreateHmSuggestion() {
        // Given: HR không được gọi nghiệp vụ tạo đề xuất dành cho HM.
        hm.setRole(Role.builder().roleName("HR").build());
        // When: Gọi pool.suggest với dữ liệu đã chuẩn bị.
        assertThrows(AccessDeniedException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101))));
        // Then: HR không được gọi nghiệp vụ tạo đề xuất dành cho HM.
        verify(jobs, times(0)).findById(any());
        verify(jobs, times(0)).findForUpdate(any());
        verify(jobs, times(0)).findAll();
        verify(applications, times(0)).findDetailById(any());
        verify(applications, times(0)).saveAndFlush(any());
        verify(applications, times(0)).transition(anyInt(), anyCollection(), anyString());
        verify(applications, times(0)).promoteSuggestionToHmPassed(anyInt());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("HM khác phòng ban không được tạo đề xuất, không đọc nguồn hoặc ghi dữ liệu")
    void wrongDepartmentIsDenied() {
        // Given: HM khác phòng ban không được tạo đề xuất, không đọc nguồn hoặc ghi dữ liệu.
        when(jobs.findForUpdate(8)).thenReturn(Optional.of(target));
        // When: Gọi pool.suggest với dữ liệu đã chuẩn bị.
        assertThrows(AccessDeniedException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101))));
        // Then: HM khác phòng ban không được tạo đề xuất, không đọc nguồn hoặc ghi dữ liệu.
        verify(applications, times(0)).findDetailById(any());
        verify(applications, times(0)).saveAndFlush(any());
        verify(applications, times(0)).transition(anyInt(), anyCollection(), anyString());
        verify(applications, times(0)).promoteSuggestionToHmPassed(anyInt());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Tin quá hạn nhận hồ sơ: chặn đề xuất trước khi đọc nguồn")
    void expiredPostingCannotReceiveSuggestions() {
        // Given: Tin quá hạn nhận hồ sơ.
        allowTarget();
        target.setApplicationDeadline(LocalDateTime.now().minusMinutes(1));
        // When: Gọi pool.suggest với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> pool.suggest(8, new TalentPoolSuggestRequest(List.of(101))));
        // Then: chặn đề xuất trước khi đọc nguồn.
        verify(applications, times(0)).findDetailById(any());
        verify(applications, times(0)).saveAndFlush(any());
        verify(applications, times(0)).transition(anyInt(), anyCollection(), anyString());
        verify(applications, times(0)).promoteSuggestionToHmPassed(anyInt());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }
}
