package com.group2.rms.candidate.talentpool.service;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.entity.ApplicationReview;
import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.candidate.domain.rule.ApplicationAccess;
import com.group2.rms.candidate.infra.cv.CvStorage;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.candidate.domain.repository.ApplicationReviewRepository;
import com.group2.rms.candidate.domain.repository.CandidateRepository;
import com.group2.rms.candidate.talentpool.exception.TalentPoolException;
import com.group2.rms.candidate.talentpool.dto.TalentPoolConfirmRequest;
import com.group2.rms.candidate.talentpool.dto.TalentPoolSuggestRequest;
import com.group2.rms.candidate.talentpool.repository.TalentPoolAuditRepository;
import com.group2.rms.candidate.talentpool.repository.TalentPoolNoteRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class TalentPoolDecisionUnitTests {
    private final CandidateRepository candidates = mock(CandidateRepository.class);
    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final TalentPoolNoteRepository notes = mock(TalentPoolNoteRepository.class);
    private final TalentPoolAuditRepository audit = mock(TalentPoolAuditRepository.class);
    private final JobPostingRepository jobs = mock(JobPostingRepository.class);
    private final CurrentUserService currentUser = mock(CurrentUserService.class);
    private final RequisitionAccess jobAccess = mock(RequisitionAccess.class);
    private final ApplicationAccess applicationAccess = mock(ApplicationAccess.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CvStorage storage = mock(CvStorage.class);
    private final ApplicationReviewRepository reviews = mock(ApplicationReviewRepository.class);
    private final TalentPoolService service = new TalentPoolService(candidates, applications, notes, audit,
            jobs, currentUser, jobAccess, applicationAccess, users, storage, reviews);
    private final User hr = User.builder().userId(5).fullName("HR Linh")
            .role(Role.builder().roleName("HR").build()).build();
    private final User hm = User.builder().userId(6).fullName("HM An")
            .role(Role.builder().roleName("Hiring Manager").build()).build();
    private final Candidate candidate = Candidate.builder().candidateId(8).isPotential(true)
            .account(User.builder().fullName("Candidate A").email("a@example.test").phoneNumber("0900000000").build()).build();
    private final JobPosting job = JobPosting.builder().jobPostingId(20).postingTitle("Java Developer")
            .postingStatus("Published").requisition(JobRequisition.builder().hiringManager(hm).build()).build();
    private final LocalDateTime at = LocalDateTime.of(2026, 10, 19, 9, 0);
    private final Application application = Application.builder().applicationId(100).candidate(candidate)
            .jobPosting(job).applicationStatus("HM_Suggested").appliedCvUrl("https://example.test/cv.pdf")
            .submissionDate(at).build();

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-01 | Chưa có đồng ý sử dụng CV cũ")
    void confirm_noConsent_rejectsBeforeReadingApplication() {
        // Given: Chưa có đồng ý sử dụng CV cũ.
        when(currentUser.requireUser()).thenReturn(hr);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.confirm(100, new TalentPoolConfirmRequest(false)));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(applications, times(0)).findDetailById(any());
        verify(applications, times(0)).promoteSuggestionToHmPassed(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-02 | HR xác nhận draft hợp lệ sau HM phản hồi")
    void confirm_validDraft_promotesAndWritesAudit() {
        // Given: HR xác nhận draft hợp lệ sau HM phản hồi.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        when(applications.promoteSuggestionToHmPassed(100)).thenReturn(1);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        Integer result = service.confirm(100, new TalentPoolConfirmRequest(true));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(100, result);
        verify(applications, times(1)).promoteSuggestionToHmPassed(100);
        verify(audit, times(1)).record(5, "UPDATE", "Application", 100, "HM_Suggested", "TalentPoolActivated");
        assertEquals(true, candidate.isPotential());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-03 | Hồ sơ đã Rejected không được kích hoạt")
    void confirm_wrongState_rejectsWithoutPromotion() {
        // Given: Hồ sơ đã Rejected không được kích hoạt.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        application.setApplicationStatus("Rejected");
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.confirm(100, new TalentPoolConfirmRequest(true)));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(applications, times(0)).promoteSuggestionToHmPassed(any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-04 | Ứng viên đã có hồ sơ chính thức cho cùng tin")
    void confirm_existingFormalApplication_rejectsDuplicate() {
        // Given: Ứng viên đã có hồ sơ chính thức cho cùng tin.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        when(applications.existsActiveByCandidateAndJobPosting(8, 20)).thenReturn(true);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.confirm(100, new TalentPoolConfirmRequest(true)));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(applications, times(0)).promoteSuggestionToHmPassed(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-05 | Promotion trả 0 do hồ sơ đã đổi trạng thái")
    void confirm_lostPromotion_rejectsWithoutAudit() {
        // Given: Promotion trả 0 do hồ sơ đã đổi trạng thái.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.confirm(100, new TalentPoolConfirmRequest(true)));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(applications, times(1)).promoteSuggestionToHmPassed(100);
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-06 | Draft không có CV để ứng tuyển")
    void confirm_missingCv_rejectsBeforePromotion() {
        // Given: Draft không có CV để ứng tuyển.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        application.setAppliedCvUrl(null);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.confirm(100, new TalentPoolConfirmRequest(true)));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(applications, times(0)).promoteSuggestionToHmPassed(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-07 | HM không được xác nhận ứng tuyển thay HR")
    void confirm_hmRole_rejectsBeforeReadingApplication() {
        // Given: HM không được xác nhận ứng tuyển thay HR.
        when(currentUser.requireUser()).thenReturn(hm);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(AccessDeniedException.class, () -> service.confirm(100, new TalentPoolConfirmRequest(true)));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(applications, times(0)).findDetailById(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-08 | Tin đã bị xóa trước lúc xác nhận")
    void confirm_missingLockedJob_returnsNotFound() {
        // Given: Tin đã bị xóa trước lúc xác nhận.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(ResourceNotFoundException.class, () -> service.confirm(100, new TalentPoolConfirmRequest(true)));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(jobs, times(1)).findForUpdate(20);
        verify(applications, times(0)).promoteSuggestionToHmPassed(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-09 | HR kết thúc liên hệ sau HM phản hồi")
    void decline_validDraft_transitionsAndWritesAudit() {
        // Given: HR kết thúc liên hệ sau HM phản hồi.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        when(applications.transition(100, Set.of("HM_Suggested"), "Rejected")).thenReturn(1);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        Integer result = service.decline(100);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(20, result);
        verify(audit, times(1)).record(5, "UPDATE", "Application", 100, "HM_Suggested", "TalentPoolCancelled");
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-10 | Kết thúc liên hệ nhưng transition trả 0")
    void decline_lostTransition_rejectsWithoutAudit() {
        // Given: Kết thúc liên hệ nhưng transition trả 0.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.decline(100));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(applications, times(1)).transition(100, Set.of("HM_Suggested"), "Rejected");
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-11 | HM từ chối nhưng draft đã được xử lý")
    void decide_rejectLostTransition_doesNotSaveReview() {
        // Given: HM từ chối nhưng draft đã được xử lý.
        when(currentUser.requireUser()).thenReturn(hm);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        when(applications.countPendingHrRecommendation(100)).thenReturn(1L);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.decideRecommendation(100, false));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(reviews, times(0)).saveAndFlush(any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-12 | HM chấp nhận nhưng ứng viên đã có hồ sơ chính thức")
    void decide_acceptExistingApplication_doesNotSaveReview() {
        // Given: HM chấp nhận nhưng ứng viên đã có hồ sơ chính thức.
        when(currentUser.requireUser()).thenReturn(hm);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        when(applications.countPendingHrRecommendation(100)).thenReturn(1L);
        when(applications.existsActiveByCandidateAndJobPosting(8, 20)).thenReturn(true);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.decideRecommendation(100, true));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(reviews, times(0)).saveAndFlush(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-13 | Đóng tin có trạng thái Closed")
    void closeJob_closed_handlesPostingState() {
        // Given: Đóng tin có trạng thái Closed.
        when(currentUser.requireUser()).thenReturn(hr);
        job.setPostingStatus("Closed");
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        service.closeJob(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals("Closed", job.getPostingStatus());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
        verify(applications, times(0)).findAllDraftsByJobPostingId(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-14 | Đóng tin có trạng thái Draft")
    void closeJob_draft_handlesPostingState() {
        // Given: Đóng tin có trạng thái Draft.
        when(currentUser.requireUser()).thenReturn(hr);
        job.setPostingStatus("Draft");
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.closeJob(20));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals("Draft", job.getPostingStatus());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-15 | Đóng tin có trạng thái Published")
    void closeJob_published_handlesPostingState() {
        // Given: Đóng tin có trạng thái Published.
        when(currentUser.requireUser()).thenReturn(hr);
        job.setPostingStatus("Published");
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        service.closeJob(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals("Closed", job.getPostingStatus());
        assertNotNull(job.getClosedAt());
        verify(audit, times(1)).record(5, "UPDATE", "JobPosting", 20, null, "Closed");
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-16 | Đóng tin có trạng thái Paused")
    void closeJob_paused_handlesPostingState() {
        // Given: Đóng tin có trạng thái Paused.
        when(currentUser.requireUser()).thenReturn(hr);
        job.setPostingStatus("Paused");
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        service.closeJob(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals("Closed", job.getPostingStatus());
        assertNotNull(job.getClosedAt());
        verify(audit, times(1)).record(5, "UPDATE", "JobPosting", 20, null, "Closed");
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-17 | Còn Offer chưa kết thúc phải chặn đóng tin")
    void closeJob_openOffer_keepsPostingOpen() {
        // Given: Còn Offer chưa kết thúc phải chặn đóng tin.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        when(audit.hasOpenOffers(20)).thenReturn(true);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.closeJob(20));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals("Published", job.getPostingStatus());
        assertEquals(null, job.getClosedAt());
        verify(applications, times(0)).findAllDraftsByJobPostingId(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-18 | Chỉ draft transition thành công mới ghi hủy")
    void closeJob_draftTransitionsOnlySuccessfulRowsWriteAudit() {
        // Given: Chỉ draft transition thành công mới ghi hủy.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        Application moved = Application.builder().applicationId(101).build();
        when(applications.findAllDraftsByJobPostingId(20)).thenReturn(List.of(application, moved));
        when(applications.transition(100, Set.of("HM_Suggested"), "Rejected")).thenReturn(1);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        service.closeJob(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(audit, times(1)).record(5, "UPDATE", "Application", 100, "HM_Suggested", "TalentPoolCancelled");
        verify(audit, times(0)).record(5, "UPDATE", "Application", 101, "HM_Suggested", "TalentPoolCancelled");
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-19 | Đóng tin xử lý hồ sơ acceptedOffer")
    void closeJob_acceptedOffer_handlesPendingApplication() {
        // Given: Đóng tin xử lý hồ sơ acceptedOffer.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        application.setApplicationStatus("Interviewing");
        when(applications.findPendingApplicationsForClosedJob(20)).thenReturn(List.of(application));
        when(audit.hasAcceptedOffer(100)).thenReturn(true);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        service.closeJob(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(applications, times(0)).transition(100, Set.of("Interviewing"), "Rejected");
        verify(audit, times(0)).cancelInterviews(100);
        verify(audit, times(0)).record(5, "UPDATE", "Application", 100, null, "InterviewCancelledOnClose");
        verify(audit, times(0)).record(5, "UPDATE", "Application", 100, "Interviewing", "PostingClosed");
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-20 | Đóng tin xử lý hồ sơ lostTransition")
    void closeJob_lostTransition_handlesPendingApplication() {
        // Given: Đóng tin xử lý hồ sơ lostTransition.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        application.setApplicationStatus("Interviewing");
        when(applications.findPendingApplicationsForClosedJob(20)).thenReturn(List.of(application));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        service.closeJob(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(applications, times(1)).transition(100, Set.of("Interviewing"), "Rejected");
        verify(audit, times(0)).cancelInterviews(100);
        verify(audit, times(0)).record(5, "UPDATE", "Application", 100, null, "InterviewCancelledOnClose");
        verify(audit, times(0)).record(5, "UPDATE", "Application", 100, "Interviewing", "PostingClosed");
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-21 | Đóng tin xử lý hồ sơ withInterviews")
    void closeJob_withInterviews_handlesPendingApplication() {
        // Given: Đóng tin xử lý hồ sơ withInterviews.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        application.setApplicationStatus("Interviewing");
        when(applications.findPendingApplicationsForClosedJob(20)).thenReturn(List.of(application));
        when(applications.transition(100, Set.of("Interviewing"), "Rejected")).thenReturn(1);
        when(audit.cancelInterviews(100)).thenReturn(2);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        service.closeJob(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(applications, times(1)).transition(100, Set.of("Interviewing"), "Rejected");
        verify(audit, times(1)).cancelInterviews(100);
        verify(audit, times(1)).record(5, "UPDATE", "Application", 100, null, "InterviewCancelledOnClose");
        verify(audit, times(1)).record(5, "UPDATE", "Application", 100, "Interviewing", "PostingClosed");
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-22 | Đóng tin xử lý hồ sơ withoutInterviews")
    void closeJob_withoutInterviews_handlesPendingApplication() {
        // Given: Đóng tin xử lý hồ sơ withoutInterviews.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        application.setApplicationStatus("Interviewing");
        when(applications.findPendingApplicationsForClosedJob(20)).thenReturn(List.of(application));
        when(applications.transition(100, Set.of("Interviewing"), "Rejected")).thenReturn(1);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        service.closeJob(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(applications, times(1)).transition(100, Set.of("Interviewing"), "Rejected");
        verify(audit, times(1)).cancelInterviews(100);
        verify(audit, times(0)).record(5, "UPDATE", "Application", 100, null, "InterviewCancelledOnClose");
        verify(audit, times(1)).record(5, "UPDATE", "Application", 100, "Interviewing", "PostingClosed");
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-23 | Chi tiết gợi ý trạng thái pending")
    void recommendation_pending_mapsStateAndOrigin() {
        // Given: Chi tiết gợi ý trạng thái pending.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(audit.has("Application", 100, "TalentPoolHrRecommended")).thenReturn(true);
        ApplicationReview origin = ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Hold").reviewedAt(at).build();
        application.setApplicationStatus("HM_Suggested");
        when(reviews.findWithReviewerByApplicationId(100)).thenReturn(List.of(origin));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.recommendation(100, 8);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals("Chờ phản hồi", result.statusLabel());
        assertEquals(true, result.pending());
        assertEquals(false, result.canDecide());
        assertEquals("HR Linh", result.recommendedBy());
        assertEquals(at, result.recommendedAt());
        assertEquals("Candidate A", result.fullName());
        assertEquals(20, result.jobPostingId());
        assertEquals("Java Developer", result.jobTitle());
        assertEquals(100, result.applicationId());
        assertEquals(8, result.candidateId());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-24 | Chi tiết gợi ý trạng thái ended")
    void recommendation_ended_mapsStateAndOrigin() {
        // Given: Chi tiết gợi ý trạng thái ended.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(audit.has("Application", 100, "TalentPoolHrRecommended")).thenReturn(true);
        ApplicationReview origin = ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Hold").reviewedAt(at).build();
        application.setApplicationStatus("Rejected");
        when(reviews.findWithReviewerByApplicationId(100)).thenReturn(List.of(origin));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.recommendation(100, 8);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals("Đã kết thúc chờ phản hồi", result.statusLabel());
        assertEquals(false, result.pending());
        assertEquals(false, result.canDecide());
        assertEquals("HR Linh", result.recommendedBy());
        assertEquals(at, result.recommendedAt());
        assertEquals("Candidate A", result.fullName());
        assertEquals(20, result.jobPostingId());
        assertEquals("Java Developer", result.jobTitle());
        assertEquals(100, result.applicationId());
        assertEquals(8, result.candidateId());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-25 | Chi tiết gợi ý trạng thái hmRejected")
    void recommendation_hmRejected_mapsStateAndOrigin() {
        // Given: Chi tiết gợi ý trạng thái hmRejected.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(audit.has("Application", 100, "TalentPoolHrRecommended")).thenReturn(true);
        ApplicationReview origin = ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Hold").reviewedAt(at).build();
        application.setApplicationStatus("Rejected");
        ApplicationReview response = ApplicationReview.builder().reviewId(2).reviewer(hm).reviewerRole("HiringManager").decision("Fail").build();
        when(reviews.findWithReviewerByApplicationId(100)).thenReturn(List.of(origin, response));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.recommendation(100, 8);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals("HM đã từ chối", result.statusLabel());
        assertEquals(false, result.pending());
        assertEquals(false, result.canDecide());
        assertEquals("HR Linh", result.recommendedBy());
        assertEquals(at, result.recommendedAt());
        assertEquals("Candidate A", result.fullName());
        assertEquals(20, result.jobPostingId());
        assertEquals("Java Developer", result.jobTitle());
        assertEquals(100, result.applicationId());
        assertEquals(8, result.candidateId());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-26 | Chi tiết gợi ý trạng thái hmAccepted")
    void recommendation_hmAccepted_mapsStateAndOrigin() {
        // Given: Chi tiết gợi ý trạng thái hmAccepted.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(audit.has("Application", 100, "TalentPoolHrRecommended")).thenReturn(true);
        ApplicationReview origin = ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Hold").reviewedAt(at).build();
        application.setApplicationStatus("HM_Suggested");
        ApplicationReview response = ApplicationReview.builder().reviewId(2).reviewer(hm).reviewerRole("HiringManager").decision("Pass").build();
        when(reviews.findWithReviewerByApplicationId(100)).thenReturn(List.of(origin, response));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.recommendation(100, 8);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals("HM đã chấp nhận", result.statusLabel());
        assertEquals(false, result.pending());
        assertEquals(false, result.canDecide());
        assertEquals("HR Linh", result.recommendedBy());
        assertEquals(at, result.recommendedAt());
        assertEquals("Candidate A", result.fullName());
        assertEquals(20, result.jobPostingId());
        assertEquals("Java Developer", result.jobTitle());
        assertEquals(100, result.applicationId());
        assertEquals(8, result.candidateId());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-27 | Gợi ý không có HR Hold trong lịch sử")
    void recommendation_missingOrigin_rejectsIncompleteHistory() {
        // Given: Gợi ý không có HR Hold trong lịch sử.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(audit.has("Application", 100, "TalentPoolHrRecommended")).thenReturn(true);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.recommendation(100, 8));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(reviews, times(1)).findWithReviewerByApplicationId(100);
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-28 | Không đọc được gợi ý của candidate khác")
    void recommendation_wrongCandidate_returnsNotFound() {
        // Given: Không đọc được gợi ý của candidate khác.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(ResourceNotFoundException.class, () -> service.recommendation(100, 9));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(reviews, times(0)).findWithReviewerByApplicationId(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-29 | Draft do HM tạo không phải gợi ý HR")
    void recommendation_notHrOrigin_returnsNotFound() {
        // Given: Draft do HM tạo không phải gợi ý HR.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(ResourceNotFoundException.class, () -> service.recommendation(100, 8));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(reviews, times(0)).findWithReviewerByApplicationId(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-30 | HR không mở form quyết định dành cho HM")
    void recommendationForDecision_hrRole_deniesBeforeReading() {
        // Given: HR không mở form quyết định dành cho HM.
        when(currentUser.requireUser()).thenReturn(hr);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(AccessDeniedException.class, () -> service.recommendationForDecision(100));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(applications, times(0)).findDetailById(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-31 | HM mở gợi ý đang chờ với tin open")
    void recommendationForDecision_hmopen_checksEligibility() {
        // Given: HM mở gợi ý đang chờ với tin open.
        when(currentUser.requireUser()).thenReturn(hm);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        when(audit.has("Application", 100, "TalentPoolHrRecommended")).thenReturn(true);
        ApplicationReview origin = ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Hold").reviewedAt(at).build();
        when(reviews.findWithReviewerByApplicationId(100)).thenReturn(List.of(origin));
        job.setPostingStatus("Published");
        job.setApplicationDeadline(null);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.recommendationForDecision(100);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(true, result.pending());
        assertEquals(true, result.canDecide());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-32 | HM mở gợi ý đang chờ với tin paused")
    void recommendationForDecision_hmpaused_checksEligibility() {
        // Given: HM mở gợi ý đang chờ với tin paused.
        when(currentUser.requireUser()).thenReturn(hm);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        when(audit.has("Application", 100, "TalentPoolHrRecommended")).thenReturn(true);
        ApplicationReview origin = ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Hold").reviewedAt(at).build();
        when(reviews.findWithReviewerByApplicationId(100)).thenReturn(List.of(origin));
        job.setPostingStatus("Paused");
        job.setApplicationDeadline(null);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.recommendationForDecision(100);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(true, result.pending());
        assertEquals(false, result.canDecide());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-33 | HM mở gợi ý đang chờ với tin expired")
    void recommendationForDecision_hmexpired_checksEligibility() {
        // Given: HM mở gợi ý đang chờ với tin expired.
        when(currentUser.requireUser()).thenReturn(hm);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        when(audit.has("Application", 100, "TalentPoolHrRecommended")).thenReturn(true);
        ApplicationReview origin = ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Hold").reviewedAt(at).build();
        when(reviews.findWithReviewerByApplicationId(100)).thenReturn(List.of(origin));
        job.setPostingStatus("Published");
        job.setApplicationDeadline(LocalDateTime.now().minusDays(1));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.recommendationForDecision(100);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(true, result.pending());
        assertEquals(false, result.canDecide());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-34 | HM mở gợi ý đang chờ với tin future")
    void recommendationForDecision_hmfuture_checksEligibility() {
        // Given: HM mở gợi ý đang chờ với tin future.
        when(currentUser.requireUser()).thenReturn(hm);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        when(audit.has("Application", 100, "TalentPoolHrRecommended")).thenReturn(true);
        ApplicationReview origin = ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Hold").reviewedAt(at).build();
        when(reviews.findWithReviewerByApplicationId(100)).thenReturn(List.of(origin));
        job.setPostingStatus("Published");
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.recommendationForDecision(100);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(true, result.pending());
        assertEquals(true, result.canDecide());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-35 | Danh sách gợi ý ánh xạ draft từ repository")
    void recommendations_existingDraft_mapsList() {
        // Given: Danh sách gợi ý ánh xạ draft từ repository.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        when(applications.findHrRecommendations(20)).thenReturn(List.of(application));
        ApplicationReview origin = ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Hold").reviewedAt(at).build();
        when(reviews.findWithReviewerByApplicationId(100)).thenReturn(List.of(origin));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.recommendations(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(1, result.size());
        assertEquals(100, result.getFirst().applicationId());
        assertEquals(true, result.getFirst().pending());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-36 | Ứng viên đã bỏ đánh dấu vẫn đọc qua gợi ý HR hợp lệ")
    void recommendationDetail_unmarkedCandidate_keepsReadableHistory() {
        // Given: Ứng viên đã bỏ đánh dấu vẫn đọc qua gợi ý HR hợp lệ.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(audit.has("Application", 100, "TalentPoolHrRecommended")).thenReturn(true);
        ApplicationReview origin = ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Hold").reviewedAt(at).build();
        when(reviews.findWithReviewerByApplicationId(100)).thenReturn(List.of(origin));
        candidate.setPotential(false);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        when(applications.findPreviousCvByCandidateId(8)).thenReturn(List.of(application));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.recommendationDetail(8, 100);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(false, result.potential());
        assertEquals(8, result.candidate().candidateId());
        assertEquals(100, result.cvs().getFirst().applicationId());
    }

    @Test
    @DisplayName("POOL-TalentPoolDecisionUnitTests-37 | Gợi ý HR hợp lệ cho phép xem CV sau khi bỏ đánh dấu")
    void cv_validRecommendation_allowsUnmarkedCandidate() {
        // Given: Gợi ý HR hợp lệ cho phép xem CV sau khi bỏ đánh dấu.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(audit.has("Application", 100, "TalentPoolHrRecommended")).thenReturn(true);
        ApplicationReview origin = ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Hold").reviewedAt(at).build();
        when(reviews.findWithReviewerByApplicationId(100)).thenReturn(List.of(origin));
        candidate.setPotential(false);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.cv(8, 100, 100);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals("https://example.test/cv.pdf", result.redirectUrl());
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("POOL-EXTRA-01 | HR không gửi gợi ý đến manager có role không phải HM")
    void recommend_wrongManagerRole_rejectsBeforeReadingSources() {
        // Given: Tin có người phụ trách role HR thay vì Hiring Manager.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        job.getRequisition().setHiringManager(hr);
        // When: HR gửi gợi ý ứng viên.
        assertThrows(TalentPoolException.class, () -> service.recommend(20, new TalentPoolSuggestRequest(List.of(100))));
        // Then: Chặn trước khi đọc hoặc ghi hồ sơ nguồn.
        verify(applications, times(0)).findDetailById(any());
        verify(applications, times(0)).saveAndFlush(any());
    }

    @Test
    @DisplayName("POOL-EXTRA-02 | Không dùng CV cùng tin làm nguồn đề xuất")
    void suggest_sameSourceJob_rejectsBeforeWriting() {
        // Given: HM quản lý tin 20 và CV nguồn cũng thuộc tin 20.
        when(currentUser.requireUser()).thenReturn(hm);
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        // When: Tạo đề xuất từ chính tin đích.
        assertThrows(TalentPoolException.class, () -> service.suggest(20, new TalentPoolSuggestRequest(List.of(100))));
        // Then: Không tạo draft hoặc ghi audit.
        verify(applications, times(0)).saveAndFlush(any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POOL-EXTRA-03 | CV không còn trong danh sách hồ sơ nguồn chính thức")
    void recommend_nonFormalSource_rejectsBeforeWriting() {
        // Given: CV nguồn thuộc tin khác nhưng repository chỉ trả CV có ID 101.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        Application source = Application.builder().applicationId(100).candidate(candidate)
            .jobPosting(JobPosting.builder().jobPostingId(10).build()).appliedCvUrl("https://example.test/cv.pdf").build();
        when(applications.findDetailById(100)).thenReturn(Optional.of(source));
        when(applications.findPreviousCvByCandidateId(8)).thenReturn(List.of(Application.builder().applicationId(101).build()));
        // When: HR chọn CV đã mất điều kiện làm nguồn.
        assertThrows(TalentPoolException.class, () -> service.recommend(20, new TalentPoolSuggestRequest(List.of(100))));
        // Then: Không ghi draft từ CV sai điều kiện.
        verify(applications, times(0)).saveAndFlush(any());
    }

    @Test
    @DisplayName("POOL-EXTRA-04 | HR Pass không thay thế được nguồn HR Hold")
    void recommendation_hrPassWithoutHold_rejectsMissingOrigin() {
        // Given: Lịch sử chỉ có HR Pass, không có HR Hold tạo gợi ý.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(audit.has("Application", 100, "TalentPoolHrRecommended")).thenReturn(true);
        ApplicationReview pass = ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Pass").build();
        when(reviews.findWithReviewerByApplicationId(100)).thenReturn(List.of(pass));
        // When: Mở chi tiết gợi ý.
        assertThrows(TalentPoolException.class, () -> service.recommendation(100, 8));
        // Then: Báo rõ dữ liệu nguồn thiếu, không ghi review bổ sung giả.
        verify(reviews, times(0)).saveAndFlush(any());
    }

    @Test
    @DisplayName("POOL-EXTRA-05 | HM Hold không được coi là đã chấp nhận hoặc từ chối")
    void recommendation_hmHold_keepsPending() {
        // Given: Gợi ý có nguồn HR Hold và HM Hold, chưa có Pass hoặc Fail.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(audit.has("Application", 100, "TalentPoolHrRecommended")).thenReturn(true);
        ApplicationReview origin = ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Hold").build();
        ApplicationReview hold = ApplicationReview.builder().reviewId(2).reviewer(hm).reviewerRole("HiringManager").decision("Hold").build();
        when(reviews.findWithReviewerByApplicationId(100)).thenReturn(List.of(origin, hold));
        // When: Đọc trạng thái gợi ý.
        var result = service.recommendation(100, 8);
        // Then: Vẫn chờ phản hồi và HR không được quyết định thay HM.
        assertEquals(true, result.pending());
        assertEquals("Chờ phản hồi", result.statusLabel());
        assertEquals(false, result.canDecide());
    }

    @Test
    @DisplayName("POOL-EXTRA-06 | HM chấp nhận draft hợp lệ trả đúng ID tin")
    void decideRecommendation_acceptValidDraft_returnsPostingId() {
        // Given: HM đúng phòng ban và draft vẫn chờ xử lý, CV hợp lệ.
        when(currentUser.requireUser()).thenReturn(hm);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        when(applications.countPendingHrRecommendation(100)).thenReturn(1L);
        // When: HM chấp nhận gợi ý.
        Integer result = service.decideRecommendation(100, true);
        // Then: Trả ID tin, lưu một review và audit chấp nhận; không đổi draft thành hồ sơ chính thức.
        assertEquals(20, result);
        assertEquals("HM_Suggested", application.getApplicationStatus());
        verify(reviews, times(1)).saveAndFlush(any(ApplicationReview.class));
        verify(audit, times(1)).record(6, "UPDATE", "Application", 100, "HM_Suggested", "TalentPoolSuggested");
    }

    @Test
    @DisplayName("POOL-EXTRA-07 | HM không chấp nhận draft mất CV")
    void decideRecommendation_acceptMissingCv_rejectsBeforeReview() {
        // Given: HM đúng phạm vi nhưng CV nguồn bị trống.
        when(currentUser.requireUser()).thenReturn(hm);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        when(applications.countPendingHrRecommendation(100)).thenReturn(1L);
        application.setAppliedCvUrl(null);
        // When: HM chấp nhận gợi ý.
        assertThrows(TalentPoolException.class, () -> service.decideRecommendation(100, true));
        // Then: Không lưu quyết định khi CV không hợp lệ.
        verify(reviews, times(0)).saveAndFlush(any());
    }

    @Test
    @DisplayName("POOL-EXTRA-08 | Preflight chặn tin tạm dừng trước khi đọc CV nguồn")
    void reviewSelection_pausedPosting_rejectsBeforeSources() {
        // Given: HR chọn tin đã tạm dừng.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Paused");
        // When: Kiểm tra danh sách CV trước khi gửi.
        assertThrows(TalentPoolException.class, () -> service.reviewSelection(20, new TalentPoolSuggestRequest(List.of(100))));
        // Then: Chặn trước khi đọc nguồn hoặc lưu draft.
        verify(applications, times(0)).findDetailById(any());
        verify(applications, times(0)).saveAndFlush(any());
    }

    @Test
    @DisplayName("POOL-EXTRA-09 | HM ngoài phòng ban bị chặn cả khi kiểm tra quyền recommend")
    void canRecommend_hmOutsideDepartment_deniesAccess() {
        // Given: HM không quản lý tin đích.
        when(currentUser.requireUser()).thenReturn(hm);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        // When: Kiểm tra quyền gửi gợi ý HR đối với tin ngoài phạm vi.
        assertThrows(AccessDeniedException.class, () -> service.canRecommend(20));
        // Then: Guard phòng ban được kiểm tra một lần.
        verify(jobAccess, times(1)).inManagedDepartment(hm, job.getRequisition());
    }

    @Test
    @DisplayName("POOL-EXTRA-10 | Tin còn hạn được đi tiếp tới bước tìm hồ sơ nguồn")
    void recommend_futureDeadline_continuesToSourceLookup() {
        // Given: Tin Published có deadline ở ngày mai và manager hợp lệ, nguồn không tồn tại.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));
        // When: HR gửi gợi ý với ID nguồn không tồn tại.
        assertThrows(ResourceNotFoundException.class, () -> service.recommend(20, new TalentPoolSuggestRequest(List.of(404))));
        // Then: Tin còn hạn vượt guard và được tìm nguồn, không ghi draft khi nguồn thiếu.
        verify(applications, times(1)).findDetailById(404);
        verify(applications, times(0)).saveAndFlush(any());
    }

    @Test
    @DisplayName("POOL-EXTRA-11 | Gửi gợi ý cần tin liên kết requisition")
    void recommend_missingRequisition_rejectsBeforeSourceLookup() {
        // Given: HR chọn tin không có requisition để xác định HM nhận gợi ý.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        job.setRequisition(null);
        // When: HR gửi gợi ý.
        assertThrows(TalentPoolException.class, () -> service.recommend(20, new TalentPoolSuggestRequest(List.of(100))));
        // Then: Chặn trước khi tìm nguồn hoặc ghi draft.
        verify(applications, times(0)).findDetailById(any());
        verify(applications, times(0)).saveAndFlush(any());
    }

    @Test
    @DisplayName("POOL-EXTRA-12 | HR không xác nhận ứng tuyển vào tin tạm dừng")
    void confirm_pausedPosting_rejectsBeforePromotion() {
        // Given: Draft hợp lệ nhưng tin đích đã tạm dừng.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Paused");
        // When: HR xác nhận dùng CV cũ.
        assertThrows(TalentPoolException.class, () -> service.confirm(100, new TalentPoolConfirmRequest(true)));
        // Then: Không chuyển draft thành hồ sơ chính thức hoặc ghi audit.
        verify(applications, times(0)).promoteSuggestionToHmPassed(any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POOL-EXTRA-13 | Tin tạm dừng chặn HM trước khi kiểm tra pending và lưu quyết định")
    void decideRecommendation_pausedPosting_rejectsAtOpenGuard() {
        // Given: HM đúng phòng ban nhưng tin đích đã tạm dừng.
        when(currentUser.requireUser()).thenReturn(hm);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        when(jobs.findForUpdate(20)).thenReturn(Optional.of(job));
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        job.setPostingStatus("Paused");
        // When: HM chấp nhận gợi ý ở tin tạm dừng.
        TalentPoolException error = assertThrows(TalentPoolException.class, () -> service.decideRecommendation(100, true));
        // Then: Đúng lỗi tin ngừng nhận hồ sơ, không rơi xuống guard pending hoặc lưu review.
        assertEquals("Tin tuyển dụng không còn nhận hồ sơ.", error.getMessage());
        verify(applications, times(0)).countPendingHrRecommendation(any());
        verify(reviews, times(0)).saveAndFlush(any());
    }
}
