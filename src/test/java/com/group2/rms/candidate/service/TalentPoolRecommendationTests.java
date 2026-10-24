package com.group2.rms.candidate.service;

import com.group2.rms.candidate.cv.CvStorage;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

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

    @Test
    @DisplayName("HR gợi ý CV cũ: tạo draft riêng, HR Hold, audit nguồn; giữ Candidate/CV cũ")
    void hrCreatesSeparateDraftWithStructuredHoldAndNoCandidateOrSourceEdits() {
        // Given: HR chọn CV cũ của ứng viên 11; tin đích có HM hợp lệ và repository trả draft ID 219.
        targetFor(hr);
        Application original = source(101, 11);
        Application expectedDraft = Application.builder().candidate(original.getCandidate()).jobPosting(target)
            .applicationStatus("HM_Suggested").appliedCvUrl(original.getAppliedCvUrl()).build();
        when(applications.saveAndFlush(any(Application.class))).thenReturn(draft);
        ApplicationReview expectedReview = ApplicationReview.builder().application(draft).reviewer(hr)
            .reviewerRole("HR").decision("Hold").comments(null).build();

        // When: HR gửi gợi ý cho HM.
        pool.recommend(8, new TalentPoolSuggestRequest(List.of(101)));

        // Then: Lưu draft riêng và HR Hold chính xác, không sửa trạng thái CV nguồn hoặc candidate.
        verify(applications, times(1)).saveAndFlush(refEq(expectedDraft));
        verify(reviews, times(1)).saveAndFlush(refEq(expectedReview));
        assertEquals(false, original == draft);
        assertEquals("Rejected", original.getApplicationStatus());
        verify(audit, times(1)).record(4, "CREATE", "Application", 219, "101", "TalentPoolHrRecommended");
        verify(candidates, times(0)).save(any());
        verify(notes, times(0)).save(any());
    }

    @Test
    @DisplayName("HR gửi batch có Candidate sai điều kiện: không insert draft đầu tiên")
    void invalidBatchDoesNotInsertEvenFirstDraft() {
        // Given: HR gửi batch có Candidate sai điều kiện.
        targetFor(hr);
        source(101, 11);
        source(102, 12).getCandidate().setPotential(false);
        // When: Gọi pool.recommend với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> pool.recommend(8, new TalentPoolSuggestRequest(List.of(101,102))));
        // Then: không insert draft đầu tiên.
        verify(applications, times(0)).saveAndFlush(any());
        verify(reviews, times(0)).saveAndFlush(any());
        verify(reviews, times(0)).findWithReviewerByApplicationId(any());
        verify(reviews, times(0)).existsByApplication_ApplicationIdAndReviewerRoleAndDecision(any(), any(), any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("HR không gửi gợi ý trùng khi đã có draft HM/HR cho cùng tin")
    void existingHmOrHrDraftBlocksHrRecommendation() {
        // Given: HR không gửi gợi ý trùng khi đã có draft HM/HR cho cùng tin.
        targetFor(hr);
        source(101,11);
        when(applications.findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(11,8,"HM_Suggested")).thenReturn(Optional.of(draft));
        // When: Gọi pool.recommend với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> pool.recommend(8,new TalentPoolSuggestRequest(List.of(101))));
        // Then: HR không gửi gợi ý trùng khi đã có draft HM/HR cho cùng tin.
        verify(reviews, times(0)).saveAndFlush(any());
        verify(reviews, times(0)).findWithReviewerByApplicationId(any());
        verify(reviews, times(0)).existsByApplication_ApplicationIdAndReviewerRoleAndDecision(any(), any(), any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("HM không được gửi nghiệp vụ gợi ý dành riêng HR")
    void hmCannotSendAsHr() {
        // Given: HM không được gửi nghiệp vụ gợi ý dành riêng HR.
        when(current.requireUser()).thenReturn(hm);
        // When: Gọi pool.recommend với dữ liệu đã chuẩn bị.
        assertThrows(AccessDeniedException.class, () -> pool.recommend(8,new TalentPoolSuggestRequest(List.of(101))));
        // Then: HM không được gửi nghiệp vụ gợi ý dành riêng HR.
        verify(applications, times(0)).findDetailById(any());
        verify(applications, times(0)).saveAndFlush(any());
        verify(applications, times(0)).transition(anyInt(), anyCollection(), anyString());
        verify(applications, times(0)).promoteSuggestionToHmPassed(anyInt());
        verify(jobs, times(0)).findById(any());
        verify(jobs, times(0)).findForUpdate(any());
        verify(jobs, times(0)).findAll();
        verify(reviews, times(0)).saveAndFlush(any());
        verify(reviews, times(0)).findWithReviewerByApplicationId(any());
        verify(reviews, times(0)).existsByApplication_ApplicationIdAndReviewerRoleAndDecision(any(), any(), any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("HM chấp nhận: khóa tin/kiểm tra pending, lưu HM Pass, giữ draft và trả ID tin")
    void acceptKeepsSameDraftAndRecordsHmPassOnlyAfterLockAndPendingCheck() {
        // Given: HM đúng phòng ban và draft đang chờ phản hồi.
        pendingForHm();
        ApplicationReview expectedReview = ApplicationReview.builder().application(draft).reviewer(hm)
            .reviewerRole("HiringManager").decision("Pass").build();

        // When: HM chấp nhận gợi ý.
        Integer result = pool.decideRecommendation(219, true);

        // Then: Giữ draft, lưu HM Pass và audit; chưa kích hoạt hồ sơ chính thức.
        assertEquals(8, result);
        assertEquals("HM_Suggested", draft.getApplicationStatus());
        verify(jobs, times(1)).findForUpdate(8);
        verify(applications, times(1)).countPendingHrRecommendation(219);
        verify(reviews, times(1)).saveAndFlush(refEq(expectedReview));
        verify(applications, times(0)).saveAndFlush(any());
        verify(applications, times(0)).promoteSuggestionToHmPassed(anyInt());
        verify(audit, times(1)).record(5, "UPDATE", "Application", 219, "HM_Suggested", "TalentPoolSuggested");
        verify(candidates, times(0)).save(any());
        verify(notes, times(0)).save(any());
    }

    @Test
    @DisplayName("HM từ chối: transition Rejected, lưu HM Fail và audit, giữ profile")
    void rejectUsesExistingRejectedAndFailAndPreservesProfile() {
        // Given: HM đúng phòng ban và repository chuyển draft sang Rejected thành công.
        pendingForHm();
        when(applications.transition(219, Set.of("HM_Suggested"), "Rejected")).thenReturn(1);
        ApplicationReview expectedReview = ApplicationReview.builder().application(draft).reviewer(hm)
            .reviewerRole("HiringManager").decision("Fail").build();

        // When: HM từ chối gợi ý.
        Integer result = pool.decideRecommendation(219, false);

        // Then: Lưu đúng HM Fail và audit hủy, không sửa candidate hoặc đọc storage.
        assertEquals(8, result);
        verify(reviews, times(1)).saveAndFlush(refEq(expectedReview));
        verify(audit, times(1)).record(5, "UPDATE", "Application", 219, "HM_Suggested", "TalentPoolCancelled");
        verify(candidates, times(0)).save(any());
        verify(notes, times(0)).save(any());
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("Gợi ý đã xử lý: không accept/reject lần nữa và không ghi review/audit")
    void alreadyHandledCannotBeAcceptedOrRejectedAgain() {
        // Given: Gợi ý đã xử lý.
        pendingForHm();
        when(applications.countPendingHrRecommendation(219)).thenReturn(0L);
        // When: Gọi pool.decideRecommendation với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> pool.decideRecommendation(219,true));
        // Then: không accept/reject lần nữa và không ghi review/audit.
        assertThrows(TalentPoolException.class, () -> pool.decideRecommendation(219,false));
        verify(jobs, times(2)).findForUpdate(8);
        verify(reviews, times(0)).saveAndFlush(any());
        verify(reviews, times(0)).findWithReviewerByApplicationId(any());
        verify(reviews, times(0)).existsByApplication_ApplicationIdAndReviewerRoleAndDecision(any(), any(), any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("HM phòng khác không được quyết định gợi ý hoặc đọc pending")
    void anotherDepartmentCannotDecide() {
        // Given: HM phòng khác không được quyết định gợi ý hoặc đọc pending.
        targetFor(hm);
        when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        // When: Gọi pool.decideRecommendation với dữ liệu đã chuẩn bị.
        assertThrows(AccessDeniedException.class, () -> pool.decideRecommendation(219,true));
        // Then: HM phòng khác không được quyết định gợi ý hoặc đọc pending.
        verify(reviews, times(0)).saveAndFlush(any());
        verify(reviews, times(0)).findWithReviewerByApplicationId(any());
        verify(reviews, times(0)).existsByApplication_ApplicationIdAndReviewerRoleAndDecision(any(), any(), any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
        verify(applications,times(0)).countPendingHrRecommendation(anyInt());
    }

    @Test
    @DisplayName("HR không được chấp nhận gợi ý thay HM")
    void hrCannotAcceptForHm() {
        // Given: HR không được chấp nhận gợi ý thay HM.
        when(current.requireUser()).thenReturn(hr);
        // When: Gọi pool.decideRecommendation với dữ liệu đã chuẩn bị.
        assertThrows(AccessDeniedException.class, () -> pool.decideRecommendation(219,true));
        // Then: HR không được chấp nhận gợi ý thay HM.
        verify(applications, times(0)).findDetailById(any());
        verify(applications, times(0)).saveAndFlush(any());
        verify(applications, times(0)).transition(anyInt(), anyCollection(), anyString());
        verify(applications, times(0)).promoteSuggestionToHmPassed(anyInt());
        verify(reviews, times(0)).saveAndFlush(any());
        verify(reviews, times(0)).findWithReviewerByApplicationId(any());
        verify(reviews, times(0)).existsByApplication_ApplicationIdAndReviewerRoleAndDecision(any(), any(), any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("HR không được confirm/decline khi gợi ý còn chờ HM phản hồi")
    void hrCannotActivateOrDeclineBeforeHmDecision() {
        // Given: HR không được confirm/decline khi gợi ý còn chờ HM phản hồi.
        targetFor(hr);
        when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        when(applications.countPendingHrRecommendation(219)).thenReturn(1L);
        // When: Gọi pool.confirm với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> pool.confirm(219,new TalentPoolConfirmRequest(true)));
        // Then: HR không được confirm/decline khi gợi ý còn chờ HM phản hồi.
        assertThrows(TalentPoolException.class, () -> pool.decline(219));
        verify(applications,times(0)).promoteSuggestionToHmPassed(anyInt());
        verify(applications,times(0)).transition(anyInt(),anyCollection(),anyString());
        verify(reviews, times(0)).saveAndFlush(any());
        verify(reviews, times(0)).findWithReviewerByApplicationId(any());
        verify(reviews, times(0)).existsByApplication_ApplicationIdAndReviewerRoleAndDecision(any(), any(), any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Tin Paused: HM không được chấp nhận gợi ý")
    void pausedJobCannotBeAccepted() {
        // Given: Tin Paused.
        targetFor(hm);
        when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        when(access.inManagedDepartment(hm,target.getRequisition())).thenReturn(true);
        target.setPostingStatus("Paused");
        // When: Gọi pool.decideRecommendation với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> pool.decideRecommendation(219,true));
        // Then: HM không được chấp nhận gợi ý.
        verify(reviews, times(0)).saveAndFlush(any());
        verify(reviews, times(0)).findWithReviewerByApplicationId(any());
        verify(reviews, times(0)).existsByApplication_ApplicationIdAndReviewerRoleAndDecision(any(), any(), any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Tin chưa có HM nhận gợi ý: HR bị chặn trước insert")
    void missingReceivingManagerRejectsHrDraftBeforeAnyInsert() {
        // Given: Tin chưa có HM nhận gợi ý.
        targetFor(hr);
        target.getRequisition().setHiringManager(null);
        // When: Gọi pool.recommend với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> pool.recommend(8,new TalentPoolSuggestRequest(List.of(101))));
        // Then: HR bị chặn trước insert.
        verify(applications, times(0)).findDetailById(any());
        verify(applications, times(0)).saveAndFlush(any());
        verify(applications, times(0)).transition(anyInt(), anyCollection(), anyString());
        verify(applications, times(0)).promoteSuggestionToHmPassed(anyInt());
        verify(reviews, times(0)).saveAndFlush(any());
        verify(reviews, times(0)).findWithReviewerByApplicationId(any());
        verify(reviews, times(0)).existsByApplication_ApplicationIdAndReviewerRoleAndDecision(any(), any(), any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Review Fail về sau không ghi đè quyết định HM Pass đầu tiên của gợi ý")
    void laterFormalReviewDoesNotRewriteOriginalHmAcceptance() {
        // Given: Review Fail về sau không ghi đè quyết định HM Pass đầu tiên của gợi ý.
        when(current.requireUser()).thenReturn(hm);
        when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        when(access.inManagedDepartment(hm,target.getRequisition())).thenReturn(true);
        when(audit.has("Application",219,"TalentPoolHrRecommended")).thenReturn(true);
        draft.setApplicationStatus("Rejected");
        draft.getCandidate().setAccount(User.builder().fullName("Candidate").build());
        var origin=ApplicationReview.builder().reviewId(1).reviewer(hr).reviewerRole("HR").decision("Hold").reviewedAt(java.time.LocalDateTime.now()).build();
        var accepted=ApplicationReview.builder().reviewId(2).reviewer(hm).reviewerRole("HiringManager").decision("Pass").build();
        var laterFailure=ApplicationReview.builder().reviewId(3).reviewer(hm).reviewerRole("HiringManager").decision("Fail").build();
        when(reviews.findWithReviewerByApplicationId(219)).thenReturn(List.of(laterFailure,origin,accepted));
        // When: Gọi pool.recommendation với dữ liệu đã chuẩn bị.
        var response=pool.recommendation(219,11);
        // Then: Review Fail về sau không ghi đè quyết định HM Pass đầu tiên của gợi ý.
        assertEquals("HM đã chấp nhận",response.statusLabel());
        assertEquals(false, response.pending());
        assertEquals(false, response.canDecide());
    }

    void pendingForHr() {
        targetFor(hr);
        when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        when(audit.has("Application",219,"TalentPoolHrRecommended")).thenReturn(true);
    }

    @Test
    @DisplayName("HR rút gợi ý chưa xử lý: khóa tin, transition một lần và ghi hai audit")
    void hrWithdrawalCancelsOnlyUnansweredDraftUnderTheSameJobLock() {
        // Given: HR rút draft của đúng tin, tin đang Paused nhưng gợi ý chưa được HM xử lý.
        pendingForHr();
        target.setPostingStatus("Paused");
        when(applications.countPendingHrRecommendation(219)).thenReturn(1L);
        when(applications.transition(219, Set.of("HM_Suggested"), "Rejected")).thenReturn(1);

        // When: HR rút gợi ý.
        pool.withdrawRecommendation(8, 219);

        // Then: Khóa tin, kiểm tra pending và chuyển draft một lần; ghi cả audit hủy và audit rút.
        verify(jobs, times(1)).findForUpdate(8);
        verify(applications, times(1)).countPendingHrRecommendation(219);
        verify(applications, times(1)).transition(219, Set.of("HM_Suggested"), "Rejected");
        verify(audit, times(1)).record(4, "UPDATE", "Application", 219, "HM_Suggested", "TalentPoolCancelled");
        verify(audit, times(1)).record(4, "UPDATE", "Application", 219, "HM_Suggested", "TalentPoolHrWithdrawn");
        verify(candidates, times(0)).save(any());
        verify(notes, times(0)).save(any());
        verify(reviews, times(0)).saveAndFlush(any());
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("Gợi ý đã được xử lý: không rút, không transition hoặc ghi audit")
    void processedRecommendationCannotBeWithdrawn() {
        // Given: Gợi ý đã được xử lý.
        pendingForHr();
        when(applications.countPendingHrRecommendation(219)).thenReturn(0L);
        // When: Gọi pool.withdrawRecommendation với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class,()->pool.withdrawRecommendation(8,219));
        // Then: không rút, không transition hoặc ghi audit.
        verify(jobs, times(1)).findForUpdate(8);
        verify(applications,times(0)).transition(anyInt(),anyCollection(),anyString());
        verify(audit,times(0)).record(any(),any(),any(),any(),any(),any());
    }

    @Test
    @DisplayName("Rút gợi ý gặp transition=0: báo lỗi và không ghi audit thành công")
    void lostTransitionCannotReportWithdrawalOrWriteAudit() {
        // Given: Rút gợi ý gặp transition=0.
        pendingForHr();
        when(applications.countPendingHrRecommendation(219)).thenReturn(1L);
        // When: Gọi pool.withdrawRecommendation với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class,()->pool.withdrawRecommendation(8,219));
        // Then: báo lỗi và không ghi audit thành công.
        verify(jobs, times(1)).findForUpdate(8);
        verify(audit,times(0)).record(any(),any(),any(),any(),any(),any());
    }

    @Test
    @DisplayName("Chặn rút gợi ý sai tin hoặc draft không xuất phát từ HR")
    void wrongPostingAndHmOriginDraftCannotBeWithdrawn() {
        // Given: Chặn rút gợi ý sai tin hoặc draft không xuất phát từ HR.
        targetFor(hr);
        draft.setJobPosting(JobPosting.builder().jobPostingId(9).build());
        when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        // When: Gọi pool.withdrawRecommendation với dữ liệu đã chuẩn bị.
        assertThrows(com.group2.rms.core.exception.ResourceNotFoundException.class,()->pool.withdrawRecommendation(8,219));
        // Then: Chặn rút gợi ý sai tin hoặc draft không xuất phát từ HR.
        draft.setJobPosting(target);
        assertThrows(com.group2.rms.core.exception.ResourceNotFoundException.class,()->pool.withdrawRecommendation(8,219));
        verify(applications,times(0)).transition(anyInt(),anyCollection(),anyString());
    }

    @Test
    @DisplayName("HM không được rút gợi ý của HR")
    void hmCannotWithdrawHrRecommendation() {
        // Given: HM không được rút gợi ý của HR.
        when(current.requireUser()).thenReturn(hm);
        // When: Gọi pool.withdrawRecommendation với dữ liệu đã chuẩn bị.
        assertThrows(AccessDeniedException.class,()->pool.withdrawRecommendation(8,219));
        // Then: HM không được rút gợi ý của HR.
        verify(applications, times(0)).findDetailById(any());
        verify(applications, times(0)).saveAndFlush(any());
        verify(applications, times(0)).transition(anyInt(), anyCollection(), anyString());
        verify(applications, times(0)).promoteSuggestionToHmPassed(anyInt());
        verify(jobs, times(0)).findById(any());
        verify(jobs, times(0)).findForUpdate(any());
        verify(jobs, times(0)).findAll();
        verify(reviews, times(0)).saveAndFlush(any());
        verify(reviews, times(0)).findWithReviewerByApplicationId(any());
        verify(reviews, times(0)).existsByApplication_ApplicationIdAndReviewerRoleAndDecision(any(), any(), any());
        verify(audit, times(0)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Link gợi ý đã rút: trả not found và không đọc review")
    void withdrawnRecommendationCannotBeOpenedFromStaleLink() {
        // Given: Link gợi ý đã rút.
        when(current.requireUser()).thenReturn(hr);
        when(applications.findDetailById(219)).thenReturn(Optional.of(draft));
        when(audit.has("Application",219,"TalentPoolHrRecommended")).thenReturn(true);
        when(audit.has("Application",219,"TalentPoolHrWithdrawn")).thenReturn(true);
        // When: Gọi pool.recommendation với dữ liệu đã chuẩn bị.
        assertThrows(com.group2.rms.core.exception.ResourceNotFoundException.class,()->pool.recommendation(219,11));
        // Then: trả not found và không đọc review.
        verify(reviews, times(0)).saveAndFlush(any());
        verify(reviews, times(0)).findWithReviewerByApplicationId(any());
        verify(reviews, times(0)).existsByApplication_ApplicationIdAndReviewerRoleAndDecision(any(), any(), any());
    }

    @Test
    @DisplayName("Link recommend chỉ hợp lệ với tin Published còn hạn và có HM nhận")
    void recommendLinkRequiresAnOpenPostingWithReceivingHm() {
        // Given: Link recommend chỉ hợp lệ với tin Published còn hạn và có HM nhận.
        when(current.requireUser()).thenReturn(hr);
        when(jobs.findById(8)).thenReturn(Optional.of(target));
        // When: Gọi pool.canRecommend với dữ liệu đã chuẩn bị.
        // Then: Link recommend chỉ hợp lệ với tin Published còn hạn và có HM nhận.
        assertEquals(true, pool.canRecommend(8));
        target.setPostingStatus("Paused");
        assertEquals(false, pool.canRecommend(8));
        target.setPostingStatus("Published");
        target.setApplicationDeadline(java.time.LocalDateTime.now().minusDays(1));
        assertEquals(false, pool.canRecommend(8));
        target.setApplicationDeadline(null);
        target.getRequisition().setHiringManager(null);
        assertEquals(false, pool.canRecommend(8));
    }
}
