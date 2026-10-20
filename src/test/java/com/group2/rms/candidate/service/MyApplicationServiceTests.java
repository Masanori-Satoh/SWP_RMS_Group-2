package com.group2.rms.candidate.service;

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
import java.util.Optional;

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

        List<MyApplicationResponse> rows = service.list();

        assertEquals(List.of("Đã nộp CV", "Đã tự rút hồ sơ", "Không đạt vòng CV", "Phỏng vấn 10:00 "
                        + nextMonth.format(DateTimeFormatter.ofPattern("dd/MM")), "Đã nhận việc"),
                rows.stream().map(MyApplicationResponse::statusLabel).toList());
        assertEquals(List.of(true, false, false, false, false),
                rows.stream().map(MyApplicationResponse::canWithdraw).toList());
        assertEquals("/portal/interviews", rows.get(3).actionHref());
        assertEquals("/portal/offers/8", rows.get(4).actionHref());
        assertEquals(List.of("Đã nộp hồ sơ", "Đã phỏng vấn", "Bạn đã nhận lời"),
                rows.get(4).journey().stream().map(TimelineItem::title).toList());
        assertEquals("danger", rows.get(2).statusTone());
    }

    @Test
    void listWithoutApplicationsSkipsFactQueries() {
        when(applications.findOwnedByUserId(USER_ID)).thenReturn(List.of());

        assertTrue(service.list().isEmpty());

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
