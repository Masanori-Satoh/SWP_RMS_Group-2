package com.group2.rms.dashboard.internal.service;

import com.group2.rms.candidate.application.pipeline.dto.ApplicationListResponse;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.application.pipeline.service.ApplicationPipelineService;
import com.group2.rms.candidate.domain.rule.ApplicationStage;
import com.group2.rms.dashboard.internal.dto.WeeklyApplicationItem;
import com.group2.rms.dashboard.internal.dto.DashboardMetricResponse;
import com.group2.rms.dashboard.internal.dto.DashboardWorkItem;
import com.group2.rms.dashboard.internal.dto.HrDashboardResponse;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.AwaitingPostingRow;
import com.group2.rms.dashboard.internal.repository.InternalMetricsRepository;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST (Mockito): HrDashboardService. Màn /applications (ApplicationPipelineService) và repository là mock;
 * giờ cố định thứ Tư 28/10/2026 09:00 bằng Clock.fixed.
 */
class HrDashboardServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-28T02:00:00Z"),
            ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private InternalMetricsRepository metrics;
    private DashboardListRepository lists;
    private ApplicationPipelineService applicationPipeline;
    private HrDashboardService service;
    private final Map<ApplicationStage, List<ApplicationPipelineResponse>> stageRows = new EnumMap<>(ApplicationStage.class);
    private final Map<Integer, LocalDateTime> waitingSince = new HashMap<>();

    @BeforeEach
    void setUp() {
        metrics = mock(InternalMetricsRepository.class);
        lists = mock(DashboardListRepository.class);
        applicationPipeline = mock(ApplicationPipelineService.class);
        service = new HrDashboardService(metrics, lists, applicationPipeline, CLOCK);
        for (ApplicationStage stage : HrDashboardService.WAITING_STAGES) {
            stageRows.put(stage, List.of());
        }
        when(metrics.applicationWaitingSince()).thenReturn(waitingSince);
    }

    private User hr() {
        return User.builder().userId(3).fullName("Nhân sự").accountStatus("Active")
                .role(Role.builder().roleName("HR").build()).build();
    }

    /** Một hồ sơ ở giai đoạn stage, người cầm bóng owner, đã chờ days ngày. */
    private ApplicationPipelineResponse application(int id, ApplicationStage stage, String owner, int days) {
        waitingSince.put(id, NOW.minusDays(days));
        ApplicationPipelineResponse row = new ApplicationPipelineResponse(id, "Ứng viên " + id, "a@b.c", "Java Dev",
                NOW, "AI_Screened", "Chờ HR duyệt CV", "badge--warning", owner, null, false, false, false);
        List<ApplicationPipelineResponse> rows = new ArrayList<>(stageRows.get(stage));
        rows.add(row);
        stageRows.put(stage, rows);
        return row;
    }

    private HrDashboardResponse build() {
        for (ApplicationStage stage : HrDashboardService.WAITING_STAGES) {
            ApplicationListResponse response = new ApplicationListResponse(new PageImpl<>(stageRows.get(stage)),
                    Map.of(), List.of(), null, null, false, false, "HR");
            when(applicationPipeline.list(argThat(search -> search != null && stage.name().equals(search.stage())), any()))
                    .thenReturn(response);
        }
        return service.build(hr());
    }

    @Test
    @DisplayName("Việc cần xử lý: chỉ hồ sơ HR cầm bóng + yêu cầu chưa có tin, chờ lâu nhất lên đầu; chỉ 'Tạo tin' là nút chính")
    void build_shouldSortHrWorkByWaitDays_whenApplicationsAndRequisitionsWaiting() {
        // Arrange
        application(1, ApplicationStage.SCREENING, HrDashboardService.HR_OWNER, 2);
        application(2, ApplicationStage.SCREENING, "Trưởng bộ phận", 9);
        application(3, ApplicationStage.INTERVIEW, HrDashboardService.HR_OWNER, 5);
        when(lists.requisitionsAwaitingPosting()).thenReturn(List.of(
                new AwaitingPostingRow(40, "REQ-0040", "Kế toán", NOW.minusDays(4))));

        // Act
        List<DashboardWorkItem> work = build().work();

        // Assert
        assertEquals(3, work.size());
        assertEquals("/applications/3", work.get(0).href());
        assertEquals("5 ngày", work.get(0).waitText());
        assertEquals("/internal/job-postings/create?requisitionId=40", work.get(1).href());
        assertEquals("Tạo tin", work.get(1).actionLabel());
        assertTrue(work.get(1).primary());
        assertEquals("/applications/1", work.get(2).href());
        assertFalse(work.get(2).primary());
    }

    @Test
    @DisplayName("HR có đúng 3 KPI: hồ sơ chưa sàng lọc, tin đăng hôm nay, hồ sơ chờ lên lịch phỏng vấn (chỉ HR cầm bóng)")
    void build_shouldReturnThreeKpis_whenApplicationsAndPostingsExist() {
        // Arrange
        application(1, ApplicationStage.APPLIED, "Hệ thống", 2);
        application(2, ApplicationStage.APPLIED, "Hệ thống", 1);
        application(3, ApplicationStage.SCREENING, HrDashboardService.HR_OWNER, 6);
        application(4, ApplicationStage.INTERVIEW, HrDashboardService.HR_OWNER, 3);
        application(5, ApplicationStage.INTERVIEW, "Hội đồng", 1);
        when(metrics.countPublishedPostingsBetween(NOW.toLocalDate().atStartOfDay(), NOW)).thenReturn(5L);

        // Act
        List<DashboardMetricResponse> kpis = build().kpis();

        // Assert
        assertEquals(List.of("Hồ sơ chờ sàng lọc", "Tin mới đăng trong ngày", "Hồ sơ chờ lên lịch phỏng vấn"),
                kpis.stream().map(DashboardMetricResponse::title).toList());
        assertEquals(List.of("2", "5", "1"), kpis.stream().map(DashboardMetricResponse::value).toList());
        assertEquals(List.of("Chưa được sàng lọc", "Đã đăng hôm nay", "Chưa xếp lịch"),
                kpis.stream().map(DashboardMetricResponse::detail).toList());
        assertEquals("/applications?stage=APPLIED", kpis.get(0).href());
        assertEquals("/internal/job-postings?status=Published&postedFrom=2026-10-28&postedTo=2026-10-28",
                kpis.get(1).href());
        assertEquals("/applications?stage=INTERVIEW", kpis.get(2).href());
    }

    @Test
    @DisplayName("Giai đoạn hơn 10.000 hồ sơ không bị cắt khỏi KPI và yêu cầu pipeline trả đủ")
    void build_shouldRequestAllStageRows_whenStageExceedsOldLimit() {
        List<ApplicationPipelineResponse> many = new ArrayList<>();
        for (int id = 1; id <= 10_001; id++) {
            many.add(new ApplicationPipelineResponse(id, "Ứng viên " + id, "a@b.c", "Java Dev",
                    NOW, "AI_Screened", "Chờ xử lý", "badge--warning", "Hệ thống", null,
                    false, false, false));
        }
        stageRows.put(ApplicationStage.APPLIED, many);

        HrDashboardResponse result = build();

        assertEquals("10001", result.kpis().get(0).value());
        verify(applicationPipeline).list(
                argThat(search -> search != null && ApplicationStage.APPLIED.name().equals(search.stage())),
                argThat(pageable -> pageable != null && pageable.getPageSize() == Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("Xu hướng: 8 điểm theo tuần từ thứ Hai, điểm cuối là tuần này")
    void build_shouldBucketApplicationsByWeek_whenDatesGiven() {
        // Arrange — thứ Hai tuần này là 26/10, tuần đầu là 07/09
        when(metrics.applicationDates(any(), any())).thenReturn(List.of(
                LocalDateTime.of(2026, 9, 7, 8, 0),
                LocalDateTime.of(2026, 10, 26, 8, 0),
                LocalDateTime.of(2026, 10, 27, 8, 0)));

        // Act
        HrDashboardResponse result = build();
        List<WeeklyApplicationItem> weeks = result.weeklyApplications();

        // Assert
        assertEquals(8, weeks.size());
        assertEquals("07/09", weeks.get(0).weekLabel());
        assertEquals(1, weeks.get(0).count());
        assertEquals(0, weeks.get(1).count());
        assertEquals(2, weeks.get(7).count());
        assertEquals("Tuần 26/10 – 01/11", weeks.get(7).rangeLabel());
    }

}
