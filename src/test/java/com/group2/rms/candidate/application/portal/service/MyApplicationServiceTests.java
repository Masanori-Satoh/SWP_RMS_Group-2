package com.group2.rms.candidate.application.portal.service;

import com.group2.rms.candidate.application.portal.dto.MyApplicationListResponse;
import com.group2.rms.candidate.application.portal.dto.MyApplicationResponse;
import com.group2.rms.candidate.application.portal.dto.MyApplicationStep;
import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.application.portal.exception.ApplicationWithdrawalException;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.candidate.application.portal.repository.MyApplicationRepository;
import com.group2.rms.candidate.infra.cv.ApplicationCv;
import com.group2.rms.candidate.infra.cv.CvStorage;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.core.web.TimelineItem;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * "Đơn của tôi" (danh sách, chi tiết, CV) và rút đơn. Repository là mock; câu SQL thật kiểm ở {@code MyApplicationQueryTests}.
 */
@ExtendWith(MockitoExtension.class)
class MyApplicationServiceTests {

    private static final int USER_ID = 41;
    private static final int ID = 300;

    @Mock ApplicationRepository applications;
    @Mock MyApplicationRepository myApplications;
    @Mock CurrentUserService currentUserService;
    @Mock CvStorage cvStorage;

    private MyApplicationService service;

    @BeforeEach
    void setUp() {
        service = new MyApplicationService(applications, myApplications, currentUserService, cvStorage);
        when(currentUserService.requireUser()).thenReturn(User.builder().userId(USER_ID).build());
    }

    @Test
    void listGroupsFactsByApplicationAndShowsCandidateStatus() {
        LocalDateTime nextMonth = LocalDateTime.now().plusMonths(1).withHour(10).withMinute(0);
        when(myApplications.findOwnedByUserId(USER_ID)).thenReturn(List.of(
                application(1, "Applied"), application(2, "Rejected"), application(3, "Rejected"),
                application(4, "Interviewing"), application(5, "Hired")));
        when(myApplications.findMyInterviews(USER_ID)).thenReturn(List.of(
                new InterviewRow(4, nextMonth, "Online_GoogleMeet", "Scheduled", LocalDateTime.of(2026, 10, 4, 9, 0)),
                new InterviewRow(5, LocalDateTime.of(2026, 10, 19, 9, 0), "Offline_Office", "Completed",
                        LocalDateTime.of(2026, 10, 15, 9, 0))));
        when(myApplications.findMyVisibleOffers(USER_ID)).thenReturn(List.of(
                new OfferRow(5, 8, "Accepted", null, LocalDateTime.of(2026, 10, 20, 9, 0))));
        when(myApplications.findMyApplicationMarks(USER_ID)).thenReturn(List.of(
                new MarkRow(2, "Withdrawn", "Applied", LocalDateTime.of(2026, 10, 6, 9, 0))));

        MyApplicationListResponse page = service.list(null);
        // Nhóm 1: #4 có lịch phỏng vấn tháng sau. Nhóm 2: #1 đang xét. Nhóm 3 (đã kết thúc), mới có chuyển biến trước:
        // #5 nhận lời 20/10, #2 rút 06/10, #3 bị loại (chỉ có mốc nộp 05/10).
        assertEquals(List.of(4, 1, 5, 2, 3), ids(page.applications()));
        Map<Integer, MyApplicationResponse> rows = page.applications().stream()
                .collect(Collectors.toMap(MyApplicationResponse::applicationId, row -> row));

        assertEquals("Đã nộp CV", rows.get(1).statusLabel());
        assertEquals("Đã tự rút hồ sơ", rows.get(2).statusLabel());
        assertEquals("Không đạt vòng CV", rows.get(3).statusLabel());
        assertEquals("Phỏng vấn 10:00 " + nextMonth.format(DateTimeFormatter.ofPattern("dd/MM")), rows.get(4).statusLabel());
        assertEquals("Đã nhận việc", rows.get(5).statusLabel());
        assertTrue(rows.get(1).canWithdraw());
        assertFalse(rows.get(2).canWithdraw());
        assertEquals(List.of(new MyApplicationStep("Nộp CV", "done"), new MyApplicationStep("Phỏng vấn", "current"),
                new MyApplicationStep("Thư mời", "upcoming"), new MyApplicationStep("Nhận việc", "upcoming")),
                rows.get(4).steps());
        assertEquals("/portal/interviews", rows.get(4).actionHref());
        assertEquals("/portal/offers/8", rows.get(5).actionHref());
        assertEquals(List.of("Đã nộp hồ sơ", "Đã phỏng vấn", "Bạn đã nhận lời"),
                rows.get(5).journey().stream().map(TimelineItem::title).toList());
        assertEquals(LocalDateTime.of(2026, 10, 20, 9, 0), rows.get(5).lastActivityAt());
        assertEquals("danger", rows.get(3).statusTone());
        assertEquals("all", page.tab());
        assertEquals(5, page.allCount());
        assertEquals(2, page.activeCount());
        assertEquals(3, page.endedCount());
    }

    /** Lỗi dunglt thấy ở an.vo (27/10): đơn vừa có lịch phỏng vấn nằm dưới đơn đã rút. */
    @Test
    void applicationWithNewInterviewComesBeforeRecentlyWithdrawnOne() {
        LocalDateTime now = LocalDateTime.now();
        when(myApplications.findOwnedByUserId(USER_ID)).thenReturn(List.of(application(44, "Rejected"), application(210, "Interviewing")));
        when(myApplications.findMyInterviews(USER_ID)).thenReturn(List.of(
                new InterviewRow(210, now.plusHours(22), "Offline_Office", "Scheduled", now.minusMinutes(30))));
        when(myApplications.findMyApplicationMarks(USER_ID)).thenReturn(List.of(
                new MarkRow(44, "Withdrawn", "Applied", now.minusDays(2))));

        List<MyApplicationResponse> cards = service.list(null).applications();

        assertEquals(List.of(210, 44), ids(cards));
        assertEquals(now.minusMinutes(30), cards.getFirst().lastActivityAt());
        assertTrue(cards.get(1).stopped());
    }

    @Test
    void upcomingGroupIsOrderedByTheNearestEventAndOffersWithoutDeadlineComeLast() {
        LocalDateTime now = LocalDateTime.now();
        when(myApplications.findOwnedByUserId(USER_ID)).thenReturn(List.of(
                application(1, "Offered"), application(2, "Interviewing"), application(3, "Interviewing"),
                application(4, "Applied")));
        when(myApplications.findMyInterviews(USER_ID)).thenReturn(List.of(
                new InterviewRow(2, now.plusDays(10), "Offline_Office", "Scheduled", now.minusDays(1)),
                new InterviewRow(3, now.plusDays(3), "Offline_Office", "Scheduled", now.minusDays(5))));
        when(myApplications.findMyVisibleOffers(USER_ID)).thenReturn(List.of(
                new OfferRow(1, 9, "Sent_Candidate", null, now.minusHours(1))));

        assertEquals(List.of(3, 2, 1, 4), ids(service.list(null).applications()));
    }

    @Test
    void cardCarriesDepartmentCodeCvAndWhetherThePostingIsStillPublic() {
        Application closedPosting = application(42, "Rejected");
        closedPosting.getJobPosting().setPostingStatus("Closed");
        Application noCv = application(7, "Applied");
        noCv.setAppliedCvUrl(" ");
        when(myApplications.findOwnedByUserId(USER_ID)).thenReturn(List.of(closedPosting, noCv));

        Map<Integer, MyApplicationResponse> rows = service.list(null).applications().stream()
                .collect(Collectors.toMap(MyApplicationResponse::applicationId, row -> row));

        assertEquals("Engineering", rows.get(42).departmentName());
        assertEquals("#CAN-0042", rows.get(42).applicationCode());
        assertEquals("#CAN-0007", rows.get(7).applicationCode());
        assertTrue(rows.get(42).hasCv());
        assertFalse(rows.get(7).hasCv());
        assertFalse(rows.get(42).jobPostingVisible());
        assertTrue(rows.get(7).jobPostingVisible());
    }

    @Test
    void tabsSplitInFlowFromEndedAndKeepCountsOfAll() {
        when(myApplications.findOwnedByUserId(USER_ID)).thenReturn(List.of(
                application(1, "Applied"), application(2, "HR_Passed"), application(3, "Rejected"),
                application(4, "Hired")));

        MyApplicationListResponse active = service.list("active");
        MyApplicationListResponse ended = service.list("ended");

        assertEquals("active", active.tab());
        assertEquals(List.of(2, 1), ids(active.applications()));
        assertEquals("ended", ended.tab());
        assertEquals(List.of(4, 3), ids(ended.applications()));
        for (MyApplicationListResponse page : List.of(active, ended)) {
            assertEquals(4, page.allCount());
            assertEquals(2, page.activeCount());
            assertEquals(2, page.endedCount());
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "all", "ACTIVE", "x"})
    void unknownTabShowsEverything(String tab) {
        when(myApplications.findOwnedByUserId(USER_ID)).thenReturn(List.of(application(1, "Applied"), application(2, "Hired")));

        MyApplicationListResponse page = service.list(tab);

        assertEquals("all", page.tab());
        assertEquals(2, page.applications().size());
    }

    @Test
    void urgentApplicationsAreListedForTheBannerNearestFirstWhateverTheTab() {
        LocalDateTime now = LocalDateTime.now();
        when(myApplications.findOwnedByUserId(USER_ID)).thenReturn(List.of(
                application(1, "Offered"), application(2, "Interviewing"), application(3, "Applied")));
        when(myApplications.findMyInterviews(USER_ID)).thenReturn(List.of(
                new InterviewRow(2, now.plusHours(5).plusMinutes(30), "Offline_Office", "Scheduled", now.minusDays(1))));
        when(myApplications.findMyVisibleOffers(USER_ID)).thenReturn(List.of(
                new OfferRow(1, 9, "Sent_Candidate", now.plusDays(2), now.minusDays(1))));

        MyApplicationListResponse page = service.list("ended");

        assertTrue(page.applications().isEmpty());
        assertEquals(List.of(2, 1), ids(page.urgent()));
        assertEquals("Phỏng vấn sau 5 giờ", page.urgent().getFirst().urgentLabel());
    }

    @Test
    void listWithoutApplicationsSkipsFactQueries() {
        when(myApplications.findOwnedByUserId(USER_ID)).thenReturn(List.of());

        MyApplicationListResponse page = service.list(null);

        assertTrue(page.applications().isEmpty());
        assertTrue(page.urgent().isEmpty());
        assertEquals(0, page.allCount());
        verify(myApplications, never()).findMyInterviews(anyInt());
        verify(myApplications, never()).findMyVisibleOffers(anyInt());
        verify(myApplications, never()).findMyApplicationMarks(anyInt());
    }

    @Test
    void detailIsOneOfTheCandidatesOwnCards() {
        when(myApplications.findOwnedByUserId(USER_ID)).thenReturn(List.of(application(1, "Applied"), application(2, "Hired")));

        MyApplicationResponse detail = service.detail(2);

        assertEquals(2, detail.applicationId());
        assertEquals("Đã nhận việc", detail.statusLabel());
    }

    @Test
    void detailOfSomeoneElsesApplicationIsNotFound() {
        when(myApplications.findOwnedByUserId(USER_ID)).thenReturn(List.of(application(1, "Applied")));

        assertThrows(ResourceNotFoundException.class, () -> service.detail(999));
    }

    @Test
    void cvOfOwnApplicationIsLoadedFromStorageOrRedirectsToTheSample() {
        Application uploaded = application(ID, "Applied");
        uploaded.setAppliedCvUrl("local:cv/41/abc.pdf");
        ByteArrayResource pdf = new ByteArrayResource("%PDF-1.7".getBytes());
        when(myApplications.findOwnedById(ID, USER_ID)).thenReturn(Optional.of(uploaded));
        when(cvStorage.load("cv/41/abc.pdf")).thenReturn(pdf);

        assertEquals(ApplicationCv.file(pdf), service.cv(ID));

        when(myApplications.findOwnedById(ID, USER_ID)).thenReturn(Optional.of(application(ID, "Applied")));
        assertEquals(ApplicationCv.redirect("/samples/cv/sample_cv_java.pdf"), service.cv(ID));
    }

    @Test
    void cvOfSomeoneElsesApplicationOrWithoutCvIsNotFound() {
        when(myApplications.findOwnedById(ID, USER_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.cv(ID));

        Application noCv = application(ID, "Applied");
        noCv.setAppliedCvUrl("//evil.example/cv.pdf");
        when(myApplications.findOwnedById(ID, USER_ID)).thenReturn(Optional.of(noCv));
        assertThrows(ResourceNotFoundException.class, () -> service.cv(ID));
        verifyNoInteractions(cvStorage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Applied"})
    void withdrawRejectsEarlyApplicationAndRecordsWithdrawal(String status) {
        when(myApplications.findOwnedById(ID, USER_ID)).thenReturn(Optional.of(application(ID, status)));
        when(applications.transition(ID, MyApplicationService.WITHDRAWABLE_STATUSES, "Rejected")).thenReturn(1);

        String title = service.withdraw(ID);

        assertEquals("Junior Java Developer", title);
        verify(applications).recordWithdrawal(USER_ID, ID, status);
    }

    @ParameterizedTest
    @ValueSource(strings = {"HR_Passed", "HM_Passed", "Interviewing", "Offered", "Hired", "Rejected"})
    void withdrawIsRefusedAfterScreening(String status) {
        when(myApplications.findOwnedById(ID, USER_ID)).thenReturn(Optional.of(application(ID, status)));

        assertThrows(ApplicationWithdrawalException.class, () -> service.withdraw(ID));

        verify(applications, never()).transition(anyInt(), anyCollection(), anyString());
        verify(applications, never()).recordWithdrawal(anyInt(), anyInt(), anyString());
    }

    @Test
    void withdrawFailsWhenHrActedFirst() {
        when(myApplications.findOwnedById(ID, USER_ID)).thenReturn(Optional.of(application(ID, "Applied")));
        when(applications.transition(ID, MyApplicationService.WITHDRAWABLE_STATUSES, "Rejected")).thenReturn(0);

        ApplicationWithdrawalException error = assertThrows(ApplicationWithdrawalException.class,
                () -> service.withdraw(ID));

        assertEquals("Hồ sơ vừa được cập nhật. Vui lòng tải lại trang.", error.getMessage());
        verify(applications, never()).recordWithdrawal(anyInt(), anyInt(), anyString());
    }

    @Test
    void withdrawOfSomeoneElsesApplicationIsNotFound() {
        when(myApplications.findOwnedById(ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.withdraw(ID));

        verify(applications, never()).transition(anyInt(), anyCollection(), anyString());
    }

    private static Application application(int id, String status) {
        JobRequisition requisition = JobRequisition.builder()
                .department(Department.builder().departmentName("Engineering").build())
                .build();
        return Application.builder()
                .applicationId(id)
                .applicationStatus(status)
                .submissionDate(LocalDateTime.of(2026, 10, 5, 9, 0))
                .appliedCvUrl("/samples/cv/sample_cv_java.pdf")
                .jobPosting(JobPosting.builder().jobPostingId(7).postingTitle("Junior Java Developer")
                        .postingStatus("Published").requisition(requisition).build())
                .build();
    }

    private static List<Integer> ids(List<MyApplicationResponse> cards) {
        return cards.stream().map(MyApplicationResponse::applicationId).toList();
    }

    // Dòng kết quả giả của 3 câu dữ kiện (tên thành phần trùng tên getter của interface).
    private record InterviewRow(Integer getApplicationId, LocalDateTime getStartTime, String getFormat,
                                String getStatus, LocalDateTime getScheduledAt)
            implements MyApplicationRepository.MyInterviewRow {
    }

    private record OfferRow(Integer getApplicationId, Integer getOfferId, String getStatus,
                            LocalDateTime getResponseDeadline, LocalDateTime getAt)
            implements MyApplicationRepository.MyOfferRow {
    }

    private record MarkRow(Integer getApplicationId, String getKind, String getOldStatus, LocalDateTime getAt)
            implements MyApplicationRepository.MyApplicationMarkRow {
    }
}
