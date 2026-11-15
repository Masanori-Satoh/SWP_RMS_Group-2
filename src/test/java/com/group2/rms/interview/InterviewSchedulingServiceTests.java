package com.group2.rms.interview;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.candidate.domain.rule.ApplicationStatusService;
import com.group2.rms.interview.dto.InterviewScheduleRequest;
import com.group2.rms.interview.dto.InterviewScheduleResponse;
import com.group2.rms.interview.dto.PanelMemberRequest;
import com.group2.rms.interview.entity.InterviewFormat;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.interview.entity.RoleInPanel;
import com.group2.rms.interview.exception.InterviewStatusException;
import com.group2.rms.interview.repository.InterviewPanelRepository;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import com.group2.rms.interview.service.InterviewSchedulingServiceImpl;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InterviewSchedulingServiceTests {

    private InterviewScheduleRepository interviewScheduleRepository;
    private InterviewPanelRepository interviewPanelRepository;
    private ApplicationRepository applicationRepository;
    private UserRepository userRepository;
    private com.group2.rms.interview.service.InterviewTimeValidationService interviewTimeValidationService;
    private ApplicationStatusService applicationStatusService; // dunglt bổ sung
    private NotificationService notificationService; // hoangnh bổ sung
    private InterviewSchedulingServiceImpl service;

    private User hrUser;
    private User candidateUser;
    private User hmUser;
    private User directorUser;
    private Application application;

    @BeforeEach
    void setUp() {
        interviewScheduleRepository = mock(InterviewScheduleRepository.class);
        interviewPanelRepository = mock(InterviewPanelRepository.class);
        applicationRepository = mock(ApplicationRepository.class);
        userRepository = mock(UserRepository.class);
        interviewTimeValidationService = new com.group2.rms.interview.service.InterviewTimeValidationService(interviewScheduleRepository);
        applicationStatusService = mock(ApplicationStatusService.class); // dunglt bổ sung
        notificationService = mock(NotificationService.class); // hoangnh bổ sung

        service = new InterviewSchedulingServiceImpl(
                interviewScheduleRepository,
                interviewPanelRepository,
                applicationRepository,
                userRepository,
                interviewTimeValidationService,
                applicationStatusService, // dunglt bổ sung
                notificationService // hoangnh bổ sung
        );

        hrUser = User.builder()
                .userId(1)
                .username("hr_user")
                .fullName("HR Manager")
                .accountStatus("Active")
                .role(Role.builder().roleId(2).roleName("HR").build())
                .build();

        candidateUser = User.builder()
                .userId(2)
                .username("candidate_user")
                .fullName("Nguyen Candidate")
                .accountStatus("Active")
                .role(Role.builder().roleId(6).roleName("Candidate").build())
                .build();

        hmUser = User.builder()
                .userId(3)
                .username("hm_user")
                .fullName("Le Hiring Manager")
                .accountStatus("Active")
                .role(Role.builder().roleId(3).roleName("Hiring Manager").build())
                .build();

        application = Application.builder()
                .applicationId(10)
                .applicationStatus("HM_Passed")
                .build();

        directorUser = User.builder()
                .userId(6)
                .username("director_user")
                .fullName("Pham Director")
                .accountStatus("Active")
                .role(Role.builder().roleId(4).roleName("Director").build())
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(hrUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(candidateUser));
        when(userRepository.findById(3)).thenReturn(Optional.of(hmUser));
        when(userRepository.findById(6)).thenReturn(Optional.of(directorUser));
        when(applicationRepository.findById(10)).thenReturn(Optional.of(application));
    }

    @Test
    @DisplayName("Gán người dùng có vai trò Candidate vào hội đồng phỏng vấn phải bị từ chối")
    void cannotAssignCandidateUserToInterviewPanel() {
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .panelMembers(List.of(new PanelMemberRequest(2, RoleInPanel.HM)))
                .build();

        InterviewStatusException ex = assertThrows(InterviewStatusException.class, () ->
                service.createSchedule(request, 1)
        );

        assertTrue(ex.getMessage().contains("không được phép tham gia Hội đồng phỏng vấn"));
    }

    @Test
    @DisplayName("HR được xếp một HR khác vào hội đồng; vai trò trong hội đồng tự suy là HR")
    void hrCanAssignAnotherHrToInterviewPanel() {
        User anotherHr = User.builder()
                .userId(4)
                .username("hr_quang")
                .fullName("Quang HR")
                .accountStatus("Active")
                .role(Role.builder().roleId(2).roleName("HR").build())
                .build();
        when(userRepository.findById(4)).thenReturn(Optional.of(anotherHr));
        when(interviewScheduleRepository.save(any(InterviewSchedule.class))).thenAnswer(invocation -> {
            InterviewSchedule s = invocation.getArgument(0);
            s.setInterviewId(102L);
            return s;
        });

        LocalDateTime now = LocalDateTime.now().plusDays(1);
        // Form gửi interviewerIds (không kèm vai trò) như trên giao diện thật
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .interviewerIds(java.util.Set.of(4, 3))
                .build();

        InterviewScheduleResponse response = service.createSchedule(request, 1);

        assertEquals(2, response.interviewers().size());
        assertEquals(RoleInPanel.HR, response.interviewers().stream()
                .filter(m -> m.interviewerId() == 4).findFirst().orElseThrow().roleInPanel());
        assertEquals(RoleInPanel.HM, response.interviewers().stream()
                .filter(m -> m.interviewerId() == 3).findFirst().orElseThrow().roleInPanel());
    }

    @Test
    @DisplayName("TC-NOTIF-INT-01: đổi lịch gỡ Giám đốc khỏi hội đồng -> Giám đốc nhận thông báo bị gỡ")
    void updateSchedule_shouldNotifyRemovedInterviewers() {
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewSchedule existing = InterviewSchedule.builder()
                .interviewId(104L)
                .application(application)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .interviewStatus(InterviewStatus.Scheduled)
                .startTime(now)
                .endTime(now.plusHours(1))
                .build();
        existing.addPanelMember(hrUser, RoleInPanel.HR);
        existing.addPanelMember(directorUser, RoleInPanel.HM);
        when(interviewScheduleRepository.findByIdWithDetails(104L)).thenReturn(Optional.of(existing));
        when(interviewScheduleRepository.save(any(InterviewSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .interviewerIds(java.util.Set.of(1, 3))
                .build();

        service.updateSchedule(104L, request, 1);

        verify(notificationService).notifyInterviewRescheduled(existing, hrUser);
        verify(notificationService).notifyInterviewersRemoved(existing, List.of(directorUser), hrUser);
    }

    @Test
    @DisplayName("HR sửa lịch mà HR khác đã xếp mình vào hội đồng: vẫn giữ được chỗ của mình")
    void hrAlreadyInPanelCanKeepSelfWhenEditing() {
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewSchedule existing = InterviewSchedule.builder()
                .interviewId(103L)
                .application(application)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .interviewStatus(InterviewStatus.Scheduled)
                .startTime(now)
                .endTime(now.plusHours(1))
                .build();
        existing.addPanelMember(hrUser, RoleInPanel.HR);
        when(interviewScheduleRepository.findByIdWithDetails(103L)).thenReturn(Optional.of(existing));
        when(interviewScheduleRepository.save(any(InterviewSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .interviewerIds(java.util.Set.of(1, 3))
                .build();

        InterviewScheduleResponse response = service.updateSchedule(103L, request, 1);

        assertEquals(2, response.interviewers().size());
        verify(notificationService).notifyInterviewRescheduled(existing, hrUser);
    }

    @Test
    @DisplayName("HR sửa lịch không được tự thêm mình vào hội đồng")
    void hrCannotAddSelfWhenEditing() {
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewSchedule existing = InterviewSchedule.builder()
                .interviewId(104L)
                .application(application)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .interviewStatus(InterviewStatus.Scheduled)
                .startTime(now)
                .endTime(now.plusHours(1))
                .build();
        existing.addPanelMember(hmUser, RoleInPanel.HM);
        when(interviewScheduleRepository.findByIdWithDetails(104L)).thenReturn(Optional.of(existing));

        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .interviewerIds(java.util.Set.of(1, 3))
                .build();

        InterviewStatusException ex = assertThrows(InterviewStatusException.class,
                () -> service.updateSchedule(104L, request, 1));
        assertTrue(ex.getMessage().contains("không được phép tự gán mình"));
    }

    @Test
    @DisplayName("Tài khoản không Active không được tham gia hội đồng phỏng vấn")
    void cannotAssignInactiveUserToInterviewPanel() {
        User inactiveHm = User.builder()
                .userId(5)
                .username("inactive_hm")
                .fullName("Inactive HM")
                .accountStatus("Inactive")
                .role(Role.builder().roleId(3).roleName("Hiring Manager").build())
                .build();
        when(userRepository.findById(5)).thenReturn(Optional.of(inactiveHm));

        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .panelMembers(List.of(new PanelMemberRequest(5, RoleInPanel.HM)))
                .build();

        InterviewStatusException ex = assertThrows(InterviewStatusException.class, () ->
                service.createSchedule(request, 1)
        );

        assertTrue(ex.getMessage().contains("không ở trạng thái Active"));
    }

    @Test
    @DisplayName("Lên lịch thành công với Interviewer hợp lệ (Hiring Manager, Active)")
    void createScheduleSuccessWithValidInternalUser() {
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .panelMembers(List.of(new PanelMemberRequest(3, RoleInPanel.HM)))
                .build();

        when(interviewScheduleRepository.save(any(InterviewSchedule.class))).thenAnswer(invocation -> {
            InterviewSchedule s = invocation.getArgument(0);
            s.setInterviewId(100L);
            return s;
        });

        InterviewScheduleResponse response = service.createSchedule(request, 1);
        assertNotNull(response);
        assertEquals(InterviewStatus.Scheduled, response.interviewStatus());
        verify(interviewScheduleRepository, atLeastOnce()).save(any(InterviewSchedule.class));
    }

    // dunglt bổ sung: xếp lịch xong thì báo module candidate đổi trạng thái hồ sơ; lỗi validate thì không báo
    @Test
    @DisplayName("Lên lịch thành công thì hồ sơ được chuyển sang Interviewing; lên lịch lỗi thì không")
    void createScheduleNotifiesApplicationStatusOnlyOnSuccess() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(start)
                .endTime(start.plusHours(1))
                .panelMembers(List.of(new PanelMemberRequest(3, RoleInPanel.HM)))
                .build();
        when(interviewScheduleRepository.save(any(InterviewSchedule.class))).thenAnswer(invocation -> {
            InterviewSchedule s = invocation.getArgument(0);
            s.setInterviewId(100L);
            return s;
        });

        service.createSchedule(request, 1);

        verify(applicationStatusService).onInterviewScheduled(10);
        verify(notificationService).notifyInterviewScheduled(any(InterviewSchedule.class), eq(hrUser));

        InterviewScheduleRequest selfAssigned = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(start)
                .endTime(start.plusHours(1))
                .panelMembers(List.of(new PanelMemberRequest(1, RoleInPanel.HR)))
                .build();
        assertThrows(InterviewStatusException.class, () -> service.createSchedule(selfAssigned, 1));
        verify(applicationStatusService, times(2)).requireScheduleable(application.getApplicationId());
        verifyNoMoreInteractions(applicationStatusService, notificationService);
    }

    @Test
    @DisplayName("Hủy lịch đang Scheduled -> Cancelled và báo hội đồng + ứng viên; lịch đã Completed -> lỗi, không báo")
    void cancelScheduleNotifiesOnlyOnSuccess() {
        InterviewSchedule scheduled = InterviewSchedule.builder()
                .interviewId(300L)
                .application(application)
                .interviewStatus(InterviewStatus.Scheduled)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .build();
        when(interviewScheduleRepository.findByIdWithDetails(300L)).thenReturn(Optional.of(scheduled));
        when(interviewScheduleRepository.save(any(InterviewSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelSchedule(300L, "Ứng viên bận", 1);

        assertEquals(InterviewStatus.Cancelled, scheduled.getInterviewStatus());
        verify(notificationService).notifyInterviewCancelled(scheduled, hrUser, "Ứng viên bận");

        InterviewSchedule completed = InterviewSchedule.builder()
                .interviewId(301L)
                .application(application)
                .interviewStatus(InterviewStatus.Completed)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().minusDays(1).plusHours(1))
                .build();
        when(interviewScheduleRepository.findByIdWithDetails(301L)).thenReturn(Optional.of(completed));

        assertThrows(InterviewStatusException.class, () -> service.cancelSchedule(301L, "x", 1));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("Director có thể tự đặt lịch và tự gán mình vào Hội đồng phỏng vấn")
    void directorCanAssignSelfToInterviewPanel() {
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .panelMembers(List.of(new PanelMemberRequest(6, RoleInPanel.Director)))
                .build();

        when(interviewScheduleRepository.save(any(InterviewSchedule.class))).thenAnswer(invocation -> {
            InterviewSchedule s = invocation.getArgument(0);
            s.setInterviewId(101L);
            return s;
        });

        InterviewScheduleResponse response = service.createSchedule(request, 6);
        assertNotNull(response);
        assertEquals(InterviewStatus.Scheduled, response.interviewStatus());
    }

    @Test
    @DisplayName("HR tự gán mình vào Hội đồng phỏng vấn bị ném ngoại lệ")
    void hrCannotAssignSelfToInterviewPanel() {
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .panelMembers(List.of(new PanelMemberRequest(1, RoleInPanel.HM)))
                .build();

        InterviewStatusException ex = assertThrows(InterviewStatusException.class, () ->
                service.createSchedule(request, 1)
        );
        assertTrue(ex.getMessage().contains("HR tạo lịch không được phép tự gán mình vào Hội đồng phỏng vấn"));
    }

    // Trạng thái không còn do client chọn: client truyền gì cũng bị bỏ qua, hệ thống tự suy.
    @Test
    @DisplayName("Trạng thái người dùng gửi lên bị bỏ qua: hồ sơ chưa có lịch nào thì lịch mới luôn là Đã lên lịch")
    void clientSuppliedStatusIsIgnoredOnCreate() {
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .interviewStatus(InterviewStatus.Cancelled)
                .panelMembers(List.of(new PanelMemberRequest(3, RoleInPanel.HM)))
                .build();
        when(interviewScheduleRepository.save(any(InterviewSchedule.class))).thenAnswer(invocation -> {
            InterviewSchedule s = invocation.getArgument(0);
            s.setInterviewId(106L);
            return s;
        });

        InterviewScheduleResponse response = service.createSchedule(request, 1);
        assertEquals(InterviewStatus.Scheduled, response.interviewStatus());
    }

    // ducnm bổ sung: không có nút chọn trạng thái trên form nữa; hồ sơ đã có lịch còn hoạt động
    // thì lịch mới tạo cho hồ sơ đó coi như đổi lịch (Rescheduled), không phải lần đầu (Scheduled).
    @Test
    @DisplayName("Tạo lịch mới cho hồ sơ đã có lịch đang hoạt động: tự động là Đã đổi lịch")
    void creatingNewScheduleForApplicationWithActiveScheduleBecomesRescheduled() {
        InterviewSchedule oldSchedule = InterviewSchedule.builder()
                .interviewId(70L).application(application).interviewStatus(InterviewStatus.Scheduled)
                .startTime(LocalDateTime.now().plusHours(2)).endTime(LocalDateTime.now().plusHours(3)).build();
        when(interviewScheduleRepository.findActiveByApplicationId(10)).thenReturn(List.of(oldSchedule));
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(1))
                .panelMembers(List.of(new PanelMemberRequest(3, RoleInPanel.HM)))
                .build();
        when(interviewScheduleRepository.save(any(InterviewSchedule.class))).thenAnswer(invocation -> {
            InterviewSchedule s = invocation.getArgument(0);
            s.setInterviewId(107L);
            return s;
        });

        InterviewScheduleResponse response = service.createSchedule(request, 1);
        assertEquals(InterviewStatus.Rescheduled, response.interviewStatus());
        verify(notificationService).notifyInterviewRescheduled(any(InterviewSchedule.class), eq(hrUser));
        verify(notificationService, never()).notifyInterviewScheduled(any(InterviewSchedule.class), any());
        assertEquals(InterviewStatus.Cancelled, oldSchedule.getInterviewStatus());
    }

    // ducnm bổ sung: hội đồng có thể đang phỏng vấn nên không được thay lịch đã bắt đầu
    @Test
    @DisplayName("Hồ sơ có buổi phỏng vấn đang diễn ra: không tạo được lịch mới, lịch cũ giữ nguyên")
    void cannotReplaceScheduleInProgress() {
        InterviewSchedule running = InterviewSchedule.builder()
                .interviewId(71L).application(application).interviewStatus(InterviewStatus.Scheduled)
                .startTime(LocalDateTime.now().minusMinutes(5)).endTime(LocalDateTime.now().plusMinutes(30)).build();
        when(interviewScheduleRepository.findActiveByApplicationId(10)).thenReturn(List.of(running));
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(10).interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(start).endTime(start.plusHours(1))
                .panelMembers(List.of(new PanelMemberRequest(3, RoleInPanel.HM))).build();

        assertThrows(InterviewStatusException.class, () -> service.createSchedule(request, 1));
        assertEquals(InterviewStatus.Scheduled, running.getInterviewStatus());
    }

    // ducnm bổ sung: mở danh sách lịch thì các lịch đã qua giờ kết thúc được chuyển Completed trước khi đọc
    @Test
    @DisplayName("Xem danh sách lịch: chuyển các lịch đã hết giờ sang Đã hoàn thành trước khi đọc")
    void listingCompletesEndedSchedulesBeforeReading() {
        service.getAllForHR();

        org.mockito.InOrder order = inOrder(interviewScheduleRepository);
        order.verify(interviewScheduleRepository).completeEndedSchedules(any(LocalDateTime.class));
        order.verify(interviewScheduleRepository).findAllWithDetailsForHr();
    }

    // ducnm bổ sung: buổi phỏng vấn phải dài 15 phút đến 4 giờ
    @Test
    @DisplayName("Buổi phỏng vấn quá ngắn hoặc quá dài bị từ chối")
    void interviewDurationMustBeWithinBounds() {
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest tooShort = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusMinutes(5))
                .panelMembers(List.of(new PanelMemberRequest(3, RoleInPanel.HM)))
                .build();
        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(tooShort, 1));

        InterviewScheduleRequest tooLong = InterviewScheduleRequest.builder()
                .applicationId(10)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(now)
                .endTime(now.plusHours(5))
                .panelMembers(List.of(new PanelMemberRequest(3, RoleInPanel.HM)))
                .build();
        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(tooLong, 1));
    }

    // ducnm bổ sung: lịch Hoàn thành / Hủy là trạng thái cuối, không sửa được nữa
    @Test
    @DisplayName("Không thể sửa lịch đã Hoàn thành hoặc Đã hủy")
    void cannotUpdateCompletedOrCancelledSchedule() {
        InterviewSchedule completed = InterviewSchedule.builder()
                .interviewId(500L)
                .application(application)
                .interviewStatus(InterviewStatus.Completed)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().minusDays(1).plusHours(1))
                .build();
        when(interviewScheduleRepository.findByIdWithDetails(500L)).thenReturn(Optional.of(completed));

        LocalDateTime future = LocalDateTime.now().plusDays(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .startTime(future).endTime(future.plusHours(1)).build();

        assertThrows(InterviewStatusException.class, () -> service.updateSchedule(500L, request, 1));
        verifyNoInteractions(notificationService);
    }

    // ducnm bổ sung: sửa lịch đưa về quá khứ (so với hiện tại) là không hợp lệ, dù lịch đang Đã lên lịch
    @Test
    @DisplayName("Sửa lịch đang hoạt động: thời gian bắt đầu mới phải ở tương lai")
    void cannotMoveActiveScheduleStartTimeToThePast() {
        LocalDateTime originalStart = LocalDateTime.now().plusDays(1);
        InterviewSchedule scheduled = InterviewSchedule.builder()
                .interviewId(501L)
                .application(application)
                .interviewStatus(InterviewStatus.Scheduled)
                .startTime(originalStart)
                .endTime(originalStart.plusHours(1))
                .build();
        when(interviewScheduleRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(scheduled));

        LocalDateTime past = LocalDateTime.now().minusHours(1);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .startTime(past).endTime(past.plusHours(1)).build();

        assertThrows(IllegalArgumentException.class, () -> service.updateSchedule(501L, request, 1));
    }

    // ducnm bổ sung: mọi lần sửa thành công lịch Đã lên lịch đều tự chuyển sang Đã đổi lịch
    @Test
    @DisplayName("Sửa lịch đang Đã lên lịch thành công: tự động chuyển sang Đã đổi lịch")
    void updatingScheduledInterviewAutoTransitionsToRescheduled() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        InterviewSchedule scheduled = InterviewSchedule.builder()
                .interviewId(502L)
                .application(application)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .interviewStatus(InterviewStatus.Scheduled)
                .startTime(start)
                .endTime(start.plusHours(1))
                .build();
        scheduled.addPanelMember(hmUser, RoleInPanel.HM);
        when(interviewScheduleRepository.findByIdWithDetails(502L)).thenReturn(Optional.of(scheduled));
        when(interviewScheduleRepository.save(any(InterviewSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime newStart = start.plusHours(2);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .startTime(newStart).endTime(newStart.plusHours(1)).build();

        InterviewScheduleResponse response = service.updateSchedule(502L, request, 1);
        assertEquals(InterviewStatus.Rescheduled, response.interviewStatus());
        verify(notificationService).notifyInterviewRescheduled(scheduled, hrUser);
    }

    @Test
    @DisplayName("Rule GBR-01: Cập nhật đi lùi trạng thái từ Completed về Scheduled bị ném ngoại lệ")
    void forwardOnlyStateTransitionEnforced() {
        InterviewSchedule existingSchedule = InterviewSchedule.builder()
                .interviewId(200L)
                .interviewStatus(InterviewStatus.Completed)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .build();

        when(interviewScheduleRepository.findByIdWithDetails(200L)).thenReturn(Optional.of(existingSchedule));

        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .interviewStatus(InterviewStatus.Scheduled)
                .build();

        assertThrows(InterviewStatusException.class, () ->
                service.updateSchedule(200L, request, 1)
        );
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Candidate có thể lấy lịch phỏng vấn của riêng mình")
    void candidateCanGetTheirOwnSchedules() {
        InterviewSchedule s1 = InterviewSchedule.builder()
                .interviewId(301L)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1))
                .interviewStatus(InterviewStatus.Scheduled)
                .build();

        when(interviewScheduleRepository.findAllByCandidateUserId(2)).thenReturn(List.of(s1));

        List<InterviewScheduleResponse> results = service.getSchedulesForCandidate(2);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(301L, results.get(0).interviewId());
    }

    @Test
    @DisplayName("Hiring Manager có thể lấy lịch phỏng vấn theo phòng ban")
    void hiringManagerCanGetSchedulesForDepartment() {
        InterviewSchedule s1 = InterviewSchedule.builder()
                .interviewId(401L)
                .startTime(LocalDateTime.now().plusDays(3))
                .endTime(LocalDateTime.now().plusDays(3).plusHours(1))
                .interviewStatus(InterviewStatus.Scheduled)
                .build();

        when(interviewScheduleRepository.findAllByDepartmentId(10, 7)).thenReturn(List.of(s1));

        List<InterviewScheduleResponse> results = service.getSchedulesForDepartment(10, 7);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(401L, results.get(0).interviewId());
    }
}
