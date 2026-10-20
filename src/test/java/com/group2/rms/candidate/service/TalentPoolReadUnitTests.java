package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.*;
import com.group2.rms.candidate.entity.*;
import com.group2.rms.candidate.exception.TalentPoolException;
import com.group2.rms.candidate.repository.*;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class TalentPoolReadUnitTests {
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
    @DisplayName("POOL-TalentPoolReadUnitTests-01 | HR đọc vai trò người xem")
    void viewerRole_hr_returnsRole() {
        // Given: HR đọc vai trò người xem.
        when(currentUser.requireUser()).thenReturn(hr);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        String result = service.viewerRole();
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals("HR", result);
        verify(currentUser, times(1)).requireUser();
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-02 | Phạm vi lựa chọn dùng đúng ID HR")
    void selectionScope_hr_returnsActorId() {
        // Given: Phạm vi lựa chọn dùng đúng ID HR.
        when(currentUser.requireUser()).thenReturn(hr);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        Integer result = service.selectionScope();
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(5, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-03 | Tìm kiếm keyword null và giới hạn 50 dòng")
    void search_null_normalizesKeywordAndLimitsPage() {
        // Given: Tìm kiếm keyword null và giới hạn 50 dòng.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.searchPotential(null, 5, 20, PageRequest.of(2, 50))).thenReturn(Page.empty());
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.search(new TalentPoolSearch(null, 5, 20), PageRequest.of(2, 100));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(0, result.getTotalElements());
        verify(candidates, times(1)).searchPotential(null, 5, 20, PageRequest.of(2, 50));
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-04 | Tìm kiếm keyword blank và giới hạn 50 dòng")
    void search_blank_normalizesKeywordAndLimitsPage() {
        // Given: Tìm kiếm keyword blank và giới hạn 50 dòng.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.searchPotential(null, 5, 20, PageRequest.of(2, 50))).thenReturn(Page.empty());
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.search(new TalentPoolSearch("  ", 5, 20), PageRequest.of(2, 100));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(0, result.getTotalElements());
        verify(candidates, times(1)).searchPotential(null, 5, 20, PageRequest.of(2, 50));
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-05 | Tìm kiếm keyword trimmed và giới hạn 50 dòng")
    void search_trimmed_normalizesKeywordAndLimitsPage() {
        // Given: Tìm kiếm keyword trimmed và giới hạn 50 dòng.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.searchPotential("Java", 5, 20, PageRequest.of(2, 50))).thenReturn(Page.empty());
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.search(new TalentPoolSearch("  Java  ", 5, 20), PageRequest.of(2, 100));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(0, result.getTotalElements());
        verify(candidates, times(1)).searchPotential("Java", 5, 20, PageRequest.of(2, 50));
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-06 | Tìm kiếm keyword long và giới hạn 50 dòng")
    void search_long_normalizesKeywordAndLimitsPage() {
        // Given: Tìm kiếm keyword long và giới hạn 50 dòng.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.searchPotential("x".repeat(100), 5, 20, PageRequest.of(2, 50))).thenReturn(Page.empty());
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.search(new TalentPoolSearch("x".repeat(101), 5, 20), PageRequest.of(2, 100));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(0, result.getTotalElements());
        verify(candidates, times(1)).searchPotential("x".repeat(100), 5, 20, PageRequest.of(2, 50));
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-07 | Kết quả tìm kiếm có thông tin liên hệ và CV nguồn")
    void search_candidateRow_mapsContactAndPreviousCv() {
        // Given: Kết quả tìm kiếm có thông tin liên hệ và CV nguồn.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.searchPotential(null, null, null, PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(candidate)));
        when(applications.findPreviousCvByCandidateId(8)).thenReturn(List.of(application));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var row = service.search(new TalentPoolSearch(null, null, null), PageRequest.of(0, 10)).getContent().getFirst();
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(8, row.candidateId());
        assertEquals("Candidate A", row.fullName());
        assertEquals("a@example.test", row.email());
        assertEquals("0900000000", row.phoneNumber());
        assertEquals(new TalentPoolCvResponse(100, "Java Developer", at), row.cvs().getFirst());
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-08 | Danh sách người đánh dấu loại Candidate và Director")
    void authors_onlyHrAndHm_returnsEligibleAuthors() {
        // Given: Danh sách người đánh dấu loại Candidate và Director.
        when(currentUser.requireUser()).thenReturn(hr);
        User other = User.builder().userId(9).role(Role.builder().roleName("Director").build()).build();
        when(users.findAllWithRoleAndDepartment()).thenReturn(List.of(hr, hm, other, candidate.getAccount()));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.authors();
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(Map.of(5, "HR Linh", 6, "HM An"), result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-09 | Danh sách tin nguồn trả đúng ID và tiêu đề")
    void sourceJobs_existingJob_returnsIdAndTitle() {
        // Given: Danh sách tin nguồn trả đúng ID và tiêu đề.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findAll()).thenReturn(List.of(job));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.sourceJobs();
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(Map.of(20, "Java Developer"), result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-10 | Chi tiết ứng viên tiềm năng có ghi chú và CV")
    void detail_potentialCandidate_mapsNotesAndCvs() {
        // Given: Chi tiết ứng viên tiềm năng có ghi chú và CV.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        when(applications.findPreviousCvByCandidateId(8)).thenReturn(List.of(application));
        TalentPoolNote note = TalentPoolNote.builder().noteId(2).author(hr).noteContent("Good").createdAt(at).build();
        when(notes.findByCandidate_CandidateIdOrderByCreatedAtDescNoteIdDesc(8)).thenReturn(List.of(note));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.detail(8);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(true, result.potential());
        assertEquals(8, result.candidate().candidateId());
        assertEquals(new TalentPoolNoteResponse(2, "HR Linh", "Good", at), result.notes().getFirst());
        assertEquals(new TalentPoolCvResponse(100, "Java Developer", at), result.cvs().getFirst());
        verify(applications, times(2)).findPreviousCvByCandidateId(8);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-11 | Ứng viên chưa đánh dấu không được mở chi tiết")
    void detail_notPotential_rejectsBeforeReadingHistory() {
        // Given: Ứng viên chưa đánh dấu không được mở chi tiết.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        candidate.setPotential(false);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(ResourceNotFoundException.class, () -> service.detail(8));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(applications, times(0)).findPreviousCvByCandidateId(any());
        verify(notes, times(0)).findByCandidate_CandidateIdOrderByCreatedAtDescNoteIdDesc(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-12 | ID ứng viên không tồn tại")
    void detail_missingCandidate_returnsNotFound() {
        // Given: ID ứng viên không tồn tại.
        when(currentUser.requireUser()).thenReturn(hr);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(ResourceNotFoundException.class, () -> service.detail(404));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(candidates, times(1)).findById(404);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-13 | Không được ghi chú ứng viên chưa đánh dấu")
    void addNote_unmarkedCandidate_rejectsBeforeSave() {
        // Given: Không được ghi chú ứng viên chưa đánh dấu.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        candidate.setPotential(false);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.addNote(8, new TalentPoolNoteRequest("Note")));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(notes, times(0)).save(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-14 | Ghi chú đúng 1000 ký tự được chấp nhận")
    void saveBookmark_noteAt1000Characters_isAccepted() {
        // Given: Ghi chú đúng 1000 ký tự được chấp nhận.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.saveBookmark(100, new TalentPoolBookmarkRequest("x".repeat(1000)));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(true, result.saved());
        verify(notes, times(1)).save(any(TalentPoolNote.class));
        assertEquals("HM_Suggested", application.getApplicationStatus());
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-15 | CV https hợp lệ trả đường dẫn chuyển hướng")
    void cv_https_returnsRedirect() {
        // Given: CV https hợp lệ trả đường dẫn chuyển hướng.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        application.setAppliedCvUrl("https://example.test/cv.pdf");
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.cv(8, 100);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertNotNull(result);
        assertEquals("https://example.test/cv.pdf", result.redirectUrl());
        assertEquals(null, result.file());
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-16 | CV http hợp lệ trả đường dẫn chuyển hướng")
    void cv_http_returnsRedirect() {
        // Given: CV http hợp lệ trả đường dẫn chuyển hướng.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        application.setAppliedCvUrl("http://example.test/cv.pdf");
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.cv(8, 100);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertNotNull(result);
        assertEquals("http://example.test/cv.pdf", result.redirectUrl());
        assertEquals(null, result.file());
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-17 | CV sample hợp lệ trả đường dẫn chuyển hướng")
    void cv_sample_returnsRedirect() {
        // Given: CV sample hợp lệ trả đường dẫn chuyển hướng.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        application.setAppliedCvUrl("/samples/cv/sample_cv_java_backend.pdf");
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.cv(8, 100);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertNotNull(result);
        assertEquals("/samples/cv/sample_cv_java_backend.pdf", result.redirectUrl());
        assertEquals(null, result.file());
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-18 | CV local được đọc bằng đúng storage key")
    void cv_localFile_returnsStoredResource() {
        // Given: CV local được đọc bằng đúng storage key.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        application.setAppliedCvUrl("local:cv/8/file.pdf");
        var resource = new ByteArrayResource(new byte[] {1, 2});
        when(storage.load("cv/8/file.pdf")).thenReturn(resource);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.cv(8, 100);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(resource, result.file());
        assertEquals(null, result.redirectUrl());
        verify(storage, times(2)).load("cv/8/file.pdf");
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-19 | CV null bị từ chối")
    void cv_null_rejectsInvalidCv() {
        // Given: CV null bị từ chối.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        application.setAppliedCvUrl(null);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.cv(8, 100));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-20 | CV blank bị từ chối")
    void cv_blank_rejectsInvalidCv() {
        // Given: CV blank bị từ chối.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        application.setAppliedCvUrl("  ");
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.cv(8, 100));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-21 | CV ftp bị từ chối")
    void cv_ftp_rejectsInvalidCv() {
        // Given: CV ftp bị từ chối.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        application.setAppliedCvUrl("ftp://example.test/cv.pdf");
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.cv(8, 100));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-22 | CV doubleSlash bị từ chối")
    void cv_doubleSlash_rejectsInvalidCv() {
        // Given: CV doubleSlash bị từ chối.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        application.setAppliedCvUrl("//example.test/cv.pdf");
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.cv(8, 100));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-23 | CV missingSample bị từ chối")
    void cv_missingSample_rejectsInvalidCv() {
        // Given: CV missingSample bị từ chối.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        application.setAppliedCvUrl("/samples/cv/does-not-exist-linhdn.pdf");
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(TalentPoolException.class, () -> service.cv(8, 100));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-24 | Ứng viên không còn tiềm năng không được xem CV trực tiếp")
    void cv_unmarkedCandidate_deniesAccess() {
        // Given: Ứng viên không còn tiềm năng không được xem CV trực tiếp.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        candidate.setPotential(false);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(AccessDeniedException.class, () -> service.cv(8, 100));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-25 | ID hồ sơ thuộc ứng viên khác bị từ chối")
    void cv_applicationBelongsToOtherCandidate_deniesAccess() {
        // Given: ID hồ sơ thuộc ứng viên khác bị từ chối.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        application.setCandidate(Candidate.builder().candidateId(9).build());
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(AccessDeniedException.class, () -> service.cv(8, 100));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(storage, times(0)).load(any());
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-26 | ID hồ sơ không tồn tại khi xem CV")
    void cv_missingApplication_returnsNotFound() {
        // Given: ID hồ sơ không tồn tại khi xem CV.
        when(currentUser.requireUser()).thenReturn(hr);
        when(candidates.findById(8)).thenReturn(Optional.of(candidate));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(ResourceNotFoundException.class, () -> service.cv(8, 404));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(applications, times(1)).findDetailById(404);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-27 | Không có gợi ý chờ xử lý thì không tạo link")
    void pendingRecommendationUrl_noPending_returnsNull() {
        // Given: Không có gợi ý chờ xử lý thì không tạo link.

        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        String result = service.pendingRecommendationUrl(100);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(null, result);
        verify(currentUser, times(0)).requireUser();
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-28 | Gợi ý đang chờ tạo link đúng candidate và recommendation")
    void pendingRecommendationUrl_pending_returnsCandidateLink() {
        // Given: Gợi ý đang chờ tạo link đúng candidate và recommendation.
        when(currentUser.requireUser()).thenReturn(hr);
        when(applications.countPendingHrRecommendation(100)).thenReturn(1L);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        String result = service.pendingRecommendationUrl(100);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals("/talent-pool/8?recommendationId=100", result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-29 | Tin tuyển dụng không tồn tại")
    void targetJobTitle_missingJob_returnsNotFound() {
        // Given: Tin tuyển dụng không tồn tại.
        when(currentUser.requireUser()).thenReturn(hr);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        assertThrows(ResourceNotFoundException.class, () -> service.targetJobTitle(404));
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        verify(jobs, times(1)).findById(404);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-30 | HR được đọc tiêu đề tin")
    void targetJobTitle_hr_returnsTitle() {
        // Given: HR được đọc tiêu đề tin.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        String result = service.targetJobTitle(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals("Java Developer", result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-31 | Số gợi ý chờ xử lý theo đúng tin")
    void pendingRecommendationCount_returnsRepositoryCount() {
        // Given: Số gợi ý chờ xử lý theo đúng tin.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        when(applications.countPendingHrRecommendationsForJob(20)).thenReturn(3L);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        long result = service.pendingRecommendationCount(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(3L, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-32 | Danh sách đề xuất trả đúng liên hệ và người đề xuất")
    void suggestions_mapsCandidateAndAuthor() {
        // Given: Danh sách đề xuất trả đúng liên hệ và người đề xuất.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        when(applications.findSuggestionsByJobPostingId(20)).thenReturn(List.of(application));
        when(audit.suggestedBy(100)).thenReturn("HM An");
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        var result = service.suggestions(20).getFirst();
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(new TalentPoolSuggestionResponse(100, "Candidate A", "a@example.test", "0900000000", "HM An", application.getCreatedAt()), result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-33 | Người nhận gợi ý: noRequisition")
    void receivingManager_noRequisition_returnsExpectedName() {
        // Given: Người nhận gợi ý: noRequisition.
        when(currentUser.requireUser()).thenReturn(hr);
        job.setRequisition(null);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        String result = service.receivingManager(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(null, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-34 | Người nhận gợi ý: noManager")
    void receivingManager_noManager_returnsExpectedName() {
        // Given: Người nhận gợi ý: noManager.
        when(currentUser.requireUser()).thenReturn(hr);
        job.setRequisition(JobRequisition.builder().build());
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        String result = service.receivingManager(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(null, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-35 | Người nhận gợi ý: hasManager")
    void receivingManager_hasManager_returnsExpectedName() {
        // Given: Người nhận gợi ý: hasManager.
        when(currentUser.requireUser()).thenReturn(hr);
        job.setRequisition(JobRequisition.builder().hiringManager(hm).build());
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        String result = service.receivingManager(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals("HM An", result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-36 | canSuggest với trạng thái hmOpen")
    void canSuggest_hmOpen_returnsEligibility() {
        // Given: canSuggest với trạng thái hmOpen.
        when(currentUser.requireUser()).thenReturn(hm);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Published");
        job.setApplicationDeadline(null);
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        boolean result = service.canSuggest(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(true, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-37 | canSuggest với trạng thái hrCannotSuggest")
    void canSuggest_hrCannotSuggest_returnsEligibility() {
        // Given: canSuggest với trạng thái hrCannotSuggest.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Published");
        job.setApplicationDeadline(null);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        boolean result = service.canSuggest(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(false, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-38 | canSuggest với trạng thái hmPaused")
    void canSuggest_hmPaused_returnsEligibility() {
        // Given: canSuggest với trạng thái hmPaused.
        when(currentUser.requireUser()).thenReturn(hm);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Paused");
        job.setApplicationDeadline(null);
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        boolean result = service.canSuggest(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(false, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-39 | canSuggest với trạng thái hmExpired")
    void canSuggest_hmExpired_returnsEligibility() {
        // Given: canSuggest với trạng thái hmExpired.
        when(currentUser.requireUser()).thenReturn(hm);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Published");
        job.setApplicationDeadline(LocalDateTime.now().minusDays(1));
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        boolean result = service.canSuggest(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(false, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-40 | canSuggest với trạng thái hmFuture")
    void canSuggest_hmFuture_returnsEligibility() {
        // Given: canSuggest với trạng thái hmFuture.
        when(currentUser.requireUser()).thenReturn(hm);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Published");
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        boolean result = service.canSuggest(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(true, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-41 | canRecommend với trạng thái hrOpen")
    void canRecommend_hrOpen_returnsEligibility() {
        // Given: canRecommend với trạng thái hrOpen.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Published");
        job.setApplicationDeadline(null);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        boolean result = service.canRecommend(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(true, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-42 | canRecommend với trạng thái hmCannotRecommend")
    void canRecommend_hmCannotRecommend_returnsEligibility() {
        // Given: canRecommend với trạng thái hmCannotRecommend.
        when(currentUser.requireUser()).thenReturn(hm);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Published");
        job.setApplicationDeadline(null);
        when(jobAccess.inManagedDepartment(hm, job.getRequisition())).thenReturn(true);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        boolean result = service.canRecommend(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(false, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-43 | canRecommend với trạng thái hrPaused")
    void canRecommend_hrPaused_returnsEligibility() {
        // Given: canRecommend với trạng thái hrPaused.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Paused");
        job.setApplicationDeadline(null);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        boolean result = service.canRecommend(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(false, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-44 | canRecommend với trạng thái hrExpired")
    void canRecommend_hrExpired_returnsEligibility() {
        // Given: canRecommend với trạng thái hrExpired.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Published");
        job.setApplicationDeadline(LocalDateTime.now().minusDays(1));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        boolean result = service.canRecommend(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(false, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-45 | canRecommend với trạng thái hrFuture")
    void canRecommend_hrFuture_returnsEligibility() {
        // Given: canRecommend với trạng thái hrFuture.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Published");
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        boolean result = service.canRecommend(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(true, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-46 | canRecommend với trạng thái hrNoRequisition")
    void canRecommend_hrNoRequisition_returnsEligibility() {
        // Given: canRecommend với trạng thái hrNoRequisition.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Published");
        job.setApplicationDeadline(null);
        job.setRequisition(null);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        boolean result = service.canRecommend(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(false, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-47 | canRecommend với trạng thái hrNoManager")
    void canRecommend_hrNoManager_returnsEligibility() {
        // Given: canRecommend với trạng thái hrNoManager.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Published");
        job.setApplicationDeadline(null);
        job.getRequisition().setHiringManager(null);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        boolean result = service.canRecommend(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(false, result);
    }

    @Test
    @DisplayName("POOL-TalentPoolReadUnitTests-48 | canRecommend với trạng thái hrWrongManagerRole")
    void canRecommend_hrWrongManagerRole_returnsEligibility() {
        // Given: canRecommend với trạng thái hrWrongManagerRole.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        job.setPostingStatus("Published");
        job.setApplicationDeadline(null);
        job.getRequisition().setHiringManager(hr);
        // When: Gọi nghiệp vụ cần kiểm tra với dữ liệu đã chuẩn bị.
        boolean result = service.canRecommend(20);
        // Then: Kiểm tra kết quả và số lần gọi repository theo đúng ca này.
        assertEquals(false, result);
    }

    @Test
    @DisplayName("POOL-SCOPE-01 | HM ngoài phòng ban bị chặn khi gọi targetJobTitle")
    void targetJobTitle_hmOutsideDepartment_deniesBeforeReadingDetails() {
        // Given: HM đăng nhập nhưng không quản lý requisition của tin 20.
        when(currentUser.requireUser()).thenReturn(hm);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        // When: HM gọi targetJobTitle với tin ngoài phòng ban.
        assertThrows(AccessDeniedException.class, () -> service.targetJobTitle(20));
        // Then: Kiểm tra phòng ban được gọi đúng một lần trước khi trả dữ liệu.
        verify(jobAccess, times(1)).inManagedDepartment(hm, job.getRequisition());
    }

    @Test
    @DisplayName("POOL-SCOPE-02 | HM ngoài phòng ban bị chặn khi gọi suggestions")
    void suggestions_hmOutsideDepartment_deniesBeforeReadingDetails() {
        // Given: HM đăng nhập nhưng không quản lý requisition của tin 20.
        when(currentUser.requireUser()).thenReturn(hm);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        // When: HM gọi suggestions với tin ngoài phòng ban.
        assertThrows(AccessDeniedException.class, () -> service.suggestions(20));
        // Then: Kiểm tra phòng ban được gọi đúng một lần trước khi trả dữ liệu.
        verify(jobAccess, times(1)).inManagedDepartment(hm, job.getRequisition());
    }

    @Test
    @DisplayName("POOL-SCOPE-03 | HM ngoài phòng ban bị chặn khi gọi receivingManager")
    void receivingManager_hmOutsideDepartment_deniesBeforeReadingDetails() {
        // Given: HM đăng nhập nhưng không quản lý requisition của tin 20.
        when(currentUser.requireUser()).thenReturn(hm);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        // When: HM gọi receivingManager với tin ngoài phòng ban.
        assertThrows(AccessDeniedException.class, () -> service.receivingManager(20));
        // Then: Kiểm tra phòng ban được gọi đúng một lần trước khi trả dữ liệu.
        verify(jobAccess, times(1)).inManagedDepartment(hm, job.getRequisition());
    }

    @Test
    @DisplayName("POOL-SCOPE-04 | HM ngoài phòng ban bị chặn khi gọi pendingRecommendationCount")
    void pendingRecommendationCount_hmOutsideDepartment_deniesBeforeReadingDetails() {
        // Given: HM đăng nhập nhưng không quản lý requisition của tin 20.
        when(currentUser.requireUser()).thenReturn(hm);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        // When: HM gọi pendingRecommendationCount với tin ngoài phòng ban.
        assertThrows(AccessDeniedException.class, () -> service.pendingRecommendationCount(20));
        // Then: Kiểm tra phòng ban được gọi đúng một lần trước khi trả dữ liệu.
        verify(jobAccess, times(1)).inManagedDepartment(hm, job.getRequisition());
    }

    @Test
    @DisplayName("POOL-SCOPE-05 | HM ngoài phòng ban bị chặn khi gọi recommendations")
    void recommendations_hmOutsideDepartment_deniesBeforeReadingDetails() {
        // Given: HM đăng nhập nhưng không quản lý requisition của tin 20.
        when(currentUser.requireUser()).thenReturn(hm);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        // When: HM gọi recommendations với tin ngoài phòng ban.
        assertThrows(AccessDeniedException.class, () -> service.recommendations(20));
        // Then: Kiểm tra phòng ban được gọi đúng một lần trước khi trả dữ liệu.
        verify(jobAccess, times(1)).inManagedDepartment(hm, job.getRequisition());
    }

    @Test
    @DisplayName("POOL-SCOPE-06 | HM ngoài phòng ban bị chặn khi gọi canSuggest")
    void canSuggest_hmOutsideDepartment_deniesBeforeReadingDetails() {
        // Given: HM đăng nhập nhưng không quản lý requisition của tin 20.
        when(currentUser.requireUser()).thenReturn(hm);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        // When: HM gọi canSuggest với tin ngoài phòng ban.
        assertThrows(AccessDeniedException.class, () -> service.canSuggest(20));
        // Then: Kiểm tra phòng ban được gọi đúng một lần trước khi trả dữ liệu.
        verify(jobAccess, times(1)).inManagedDepartment(hm, job.getRequisition());
    }

    @Test
    @DisplayName("POOL-SCOPE-07 | HM không được lấy link gợi ý của phòng ban khác")
    void pendingRecommendationUrl_hmOutsideDepartment_deniesAccess() {
        // Given: Draft đang chờ thuộc tin ngoài phạm vi HM.
        when(currentUser.requireUser()).thenReturn(hm);
        when(applications.countPendingHrRecommendation(100)).thenReturn(1L);
        when(applications.findDetailById(100)).thenReturn(Optional.of(application));
        // When: HM lấy link điều hướng đến gợi ý.
        assertThrows(AccessDeniedException.class, () -> service.pendingRecommendationUrl(100));
        // Then: Không trả link vượt quyền; kiểm tra đúng requisition của tin.
        verify(jobAccess, times(1)).inManagedDepartment(hm, job.getRequisition());
    }

    @Test
    @DisplayName("POOL-SCOPE-08 | Preflight phân biệt ID CV nguồn trong danh sách không rỗng")
    void reviewSelection_sourceNotInPreviousCv_returnsRowError() {
        // Given: Nguồn ID 100 thuộc tin khác nhưng danh sách CV chính thức chỉ có ID 101.
        when(currentUser.requireUser()).thenReturn(hr);
        when(jobs.findById(20)).thenReturn(Optional.of(job));
        Application source = Application.builder().applicationId(100).candidate(candidate)
            .jobPosting(JobPosting.builder().jobPostingId(10).postingTitle("Old Job").build())
            .appliedCvUrl("https://example.test/cv.pdf").build();
        when(applications.findDetailById(100)).thenReturn(Optional.of(source));
        when(applications.findPreviousCvByCandidateId(8)).thenReturn(List.of(Application.builder().applicationId(101).build()));
        // When: Kiểm tra điều kiện của CV nguồn ID 100.
        var result = service.reviewSelection(20, new TalentPoolSuggestRequest(List.of(100)));
        // Then: Báo lỗi đúng dòng, không tạo draft từ ID sai.
        assertEquals("CV này không còn đủ điều kiện làm hồ sơ nguồn.", result.items().getFirst().error());
        verify(applications, times(0)).saveAndFlush(any());
    }
}
