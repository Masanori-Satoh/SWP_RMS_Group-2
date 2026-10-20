package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.MyApplicationListResponse;
import com.group2.rms.candidate.dto.MyApplicationResponse;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.exception.ApplicationWithdrawalException;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.core.web.TimelineItem;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * My Applications và rút đơn. Repository là mock; câu SQL thật kiểm ở {@code MyApplicationQueryTests}.
 */
@ExtendWith(MockitoExtension.class)
class MyApplicationServiceTests {

    private static final int USER_ID = 41;
    private static final int ID = 300;

    @Mock ApplicationRepository applications;
    @Mock CurrentUserService currentUserService;

    private MyApplicationService service;

    @BeforeEach
    void setUp() {
        service = new MyApplicationService(applications, currentUserService);
        when(currentUserService.requireUser()).thenReturn(User.builder().userId(USER_ID).build());
    }

    @Test
    void listGroupsFactsByApplicationAndShowsCandidateStatus() {
        LocalDateTime nextMonth = LocalDateTime.now().plusMonths(1).withHour(10).withMinute(0);
        when(applications.findOwnedByUserId(USER_ID)).thenReturn(List.of(
                application(1, "Applied"), application(2, "Rejected"), application(3, "Rejected"),
                application(4, "Interviewing"), application(5, "Hired")));
        when(applications.findMyInterviews(USER_ID)).thenReturn(List.of(
                new InterviewRow(4, nextMonth, "Online_GoogleMeet", "Scheduled"),
                new InterviewRow(5, LocalDateTime.of(2026, 10, 19, 9, 0), "Offline_Office", "Completed")));
        when(applications.findMyVisibleOffers(USER_ID)).thenReturn(List.of(
                new OfferRow(5, 8, "Accepted", null, LocalDateTime.of(2026, 10, 20, 9, 0))));
        when(applications.findMyApplicationMarks(USER_ID)).thenReturn(List.of(
                new MarkRow(2, "Withdrawn", "Applied", LocalDateTime.of(2026, 10, 6, 9, 0))));

        MyApplicationListResponse page = service.list(null);
        // Mới cập nhật nhất trước: #5 nhận lời 20/10, #2 rút 06/10, còn lại cùng 05/10 thì id lớn trước
        // (lịch phỏng vấn tháng sau của #4 chưa xảy ra nên chưa tính).
        assertEquals(List.of(5, 2, 4, 3, 1), page.applications().stream().map(MyApplicationResponse::applicationId).toList());
        Map<Integer, MyApplicationResponse> rows = page.applications().stream()
                .collect(Collectors.toMap(MyApplicationResponse::applicationId, row -> row));

        assertEquals("Đã nộp CV", rows.get(1).statusLabel());
        assertEquals("Đã tự rút hồ sơ", rows.get(2).statusLabel());
        assertEquals("Không đạt vòng CV", rows.get(3).statusLabel());
        assertEquals("Phỏng vấn 10:00 " + nextMonth.format(DateTimeFormatter.ofPattern("dd/MM")), rows.get(4).statusLabel());
        assertEquals("Đã nhận việc", rows.get(5).statusLabel());
        assertTrue(rows.get(1).canWithdraw());
        assertFalse(rows.get(2).canWithdraw());
        assertEquals(List.of(2, 2, 2, 3, 4), List.of(1, 2, 3, 4, 5).stream().map(id -> rows.get(id).stepNumber()).toList());
        assertEquals(List.of("current", "closed", "closed", "current", "done"),
                List.of(1, 2, 3, 4, 5).stream().map(id -> rows.get(id).stepState()).toList());
        assertEquals("Phỏng vấn", rows.get(4).stepLabel());
        assertEquals("/portal/interviews", rows.get(4).actionHref());
        assertEquals("/portal/offers/8", rows.get(5).actionHref());
        assertEquals(List.of("Đã nộp hồ sơ", "Đã phỏng vấn", "Bạn đã nhận lời"),
                rows.get(5).journey().stream().map(TimelineItem::title).toList());
        assertEquals(LocalDateTime.of(2026, 10, 20, 9, 0), rows.get(5).lastActivityAt());
        assertEquals("danger", rows.get(3).statusTone());
        assertEquals(Map.of(1, 0L, 2, 3L, 3, 1L, 4, 1L), page.stepCounts());
        assertNull(page.selectedStep());
        assertEquals(5, page.total());
    }

    @Test
    void stepFilterKeepsCountsOfAllApplications() {
        when(applications.findOwnedByUserId(USER_ID)).thenReturn(List.of(
                application(1, "Applied"), application(2, "HR_Passed"), application(3, "HM_Passed")));

        MyApplicationListResponse page = service.list(2);

        assertEquals(List.of(2, 1), page.applications().stream().map(MyApplicationResponse::applicationId).toList());
        assertEquals(2, page.selectedStep());
        assertEquals(Map.of(1, 0L, 2, 2L, 3, 1L, 4, 0L), page.stepCounts());
        assertEquals(3, page.total());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5, -1})
    void stepOutsideTheFourStepsShowsEverything(int step) {
        when(applications.findOwnedByUserId(USER_ID)).thenReturn(List.of(application(1, "Applied")));

        MyApplicationListResponse page = service.list(step);

        assertNull(page.selectedStep());
        assertEquals(1, page.applications().size());
    }

    @Test
    void listWithoutApplicationsSkipsFactQueries() {
        when(applications.findOwnedByUserId(USER_ID)).thenReturn(List.of());

        MyApplicationListResponse page = service.list(null);

        assertTrue(page.applications().isEmpty());
        assertEquals(0, page.total());
        assertEquals(Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L), page.stepCounts());
        verify(applications, never()).findMyInterviews(anyInt());
        verify(applications, never()).findMyVisibleOffers(anyInt());
        verify(applications, never()).findMyApplicationMarks(anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Applied", "AI_Screened"})
    void withdrawRejectsEarlyApplicationAndRecordsWithdrawal(String status) {
        when(applications.findOwnedById(ID, USER_ID)).thenReturn(Optional.of(application(ID, status)));
        when(applications.transition(ID, MyApplicationService.WITHDRAWABLE_STATUSES, "Rejected")).thenReturn(1);

        String title = service.withdraw(ID);

        assertEquals("Junior Java Developer", title);
        verify(applications).recordWithdrawal(USER_ID, ID, status);
    }

    @ParameterizedTest
    @ValueSource(strings = {"HR_Passed", "HM_Passed", "Interviewing", "Offered", "Hired", "Rejected"})
    void withdrawIsRefusedAfterScreening(String status) {
        when(applications.findOwnedById(ID, USER_ID)).thenReturn(Optional.of(application(ID, status)));

        assertThrows(ApplicationWithdrawalException.class, () -> service.withdraw(ID));

        verify(applications, never()).transition(anyInt(), anyCollection(), anyString());
        verify(applications, never()).recordWithdrawal(anyInt(), anyInt(), anyString());
    }

    @Test
    void withdrawFailsWhenHrActedFirst() {
        when(applications.findOwnedById(ID, USER_ID)).thenReturn(Optional.of(application(ID, "Applied")));
        when(applications.transition(ID, MyApplicationService.WITHDRAWABLE_STATUSES, "Rejected")).thenReturn(0);

        ApplicationWithdrawalException error = assertThrows(ApplicationWithdrawalException.class,
                () -> service.withdraw(ID));

        assertEquals("Hồ sơ vừa được cập nhật. Vui lòng tải lại trang.", error.getMessage());
        verify(applications, never()).recordWithdrawal(anyInt(), anyInt(), anyString());
    }

    @Test
    void withdrawOfSomeoneElsesApplicationIsNotFound() {
        when(applications.findOwnedById(ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.withdraw(ID));

        verify(applications, never()).transition(anyInt(), anyCollection(), anyString());
    }

    private static Application application(int id, String status) {
        return Application.builder()
                .applicationId(id)
                .applicationStatus(status)
                .submissionDate(LocalDateTime.of(2026, 10, 5, 9, 0))
                .jobPosting(JobPosting.builder().jobPostingId(7).postingTitle("Junior Java Developer").build())
                .build();
    }

    // Dòng kết quả giả của 3 câu dữ kiện (tên thành phần trùng tên getter của interface).
    private record InterviewRow(Integer getApplicationId, LocalDateTime getStartTime, String getFormat,
                                String getStatus) implements ApplicationRepository.MyInterviewRow {
    }

    private record OfferRow(Integer getApplicationId, Integer getOfferId, String getStatus,
                            LocalDateTime getResponseDeadline, LocalDateTime getAt)
            implements ApplicationRepository.MyOfferRow {
    }

    private record MarkRow(Integer getApplicationId, String getKind, String getOldStatus, LocalDateTime getAt)
            implements ApplicationRepository.MyApplicationMarkRow {
    }
}
