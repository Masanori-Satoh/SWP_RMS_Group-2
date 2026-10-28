package com.group2.rms.dashboard.internal.service;

import com.group2.rms.candidate.application.pipeline.dto.ApplicationListResponse;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.application.pipeline.service.ApplicationPipelineService;
import com.group2.rms.candidate.domain.rule.ApplicationStage;
import com.group2.rms.dashboard.internal.dto.DashboardBarResponse;
import com.group2.rms.dashboard.internal.dto.DashboardColumnResponse;
import com.group2.rms.dashboard.internal.dto.DashboardMetricResponse;
import com.group2.rms.dashboard.internal.dto.DashboardWorkItem;
import com.group2.rms.dashboard.internal.dto.HrDashboardResponse;
import com.group2.rms.dashboard.internal.dto.OpenPostingItem;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.AwaitingPostingRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.OpenPostingRow;
import com.group2.rms.dashboard.internal.repository.InternalMetricsRepository;
import com.group2.rms.jobposting.service.JobPostingAccess;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
    @DisplayName("KPI chờ quá 3 ngày chỉ đếm hồ sơ HR cầm bóng, không cộng yêu cầu tạo tin")
    void build_shouldCountScreeningAndOverdue_whenHrWorkExists() {
        // Arrange
        application(1, ApplicationStage.SCREENING, HrDashboardService.HR_OWNER, 6);
        application(2, ApplicationStage.SCREENING, HrDashboardService.HR_OWNER, 1);
        application(3, ApplicationStage.SCREENING, "Trưởng bộ phận", 10);
        when(lists.requisitionsAwaitingPosting()).thenReturn(List.of(
                new AwaitingPostingRow(40, "REQ-0040", "Kế toán", NOW.minusDays(8))));

        // Act
        List<DashboardMetricResponse> kpis = build().kpis();

        // Assert
        assertEquals("2", kpis.get(1).value());
        assertEquals("Lâu nhất: 6 ngày", kpis.get(1).detail());
        assertEquals("warning", kpis.get(1).tone());
        assertEquals("1", kpis.get(2).value());
        assertEquals("Hồ sơ chờ quá 3 ngày", kpis.get(2).title());
        assertEquals("/applications", kpis.get(2).href());
        assertEquals("danger", kpis.get(2).tone());
    }

    @Test
    @DisplayName("Giai đoạn hơn 10.000 hồ sơ không bị cắt khỏi biểu đồ và yêu cầu pipeline trả đủ")
    void build_shouldRequestAllStageRows_whenStageExceedsOldLimit() {
        List<ApplicationPipelineResponse> many = new ArrayList<>();
        for (int id = 1; id <= 10_001; id++) {
            many.add(new ApplicationPipelineResponse(id, "Ứng viên " + id, "a@b.c", "Java Dev",
                    NOW, "AI_Screened", "Chờ xử lý", "badge--warning", "Hệ thống", null,
                    false, false, false));
        }
        stageRows.put(ApplicationStage.APPLIED, many);

        HrDashboardResponse result = build();

        assertEquals(10_001, result.waitingStages().get(0).count());
        verify(applicationPipeline).list(
                argThat(search -> search != null && ApplicationStage.APPLIED.name().equals(search.stage())),
                argThat(pageable -> pageable != null && pageable.getPageSize() == Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("Hồ sơ đang chờ: 4 thanh theo thứ tự tab, chữ phụ = số hồ sơ chờ từ 3 ngày, giai đoạn 0 không có chữ phụ")
    void build_shouldBuildStageBarsWithOverdueNote_whenApplicationsWaiting() {
        // Arrange
        application(1, ApplicationStage.APPLIED, "Hệ thống", 4);
        application(2, ApplicationStage.APPLIED, "Hệ thống", 0);

        // Act
        List<DashboardBarResponse> bars = build().waitingStages();

        // Assert
        assertEquals(4, bars.size());
        assertEquals(ApplicationStage.APPLIED.label(), bars.get(0).label());
        assertEquals(2, bars.get(0).count());
        assertEquals("1 chờ quá 3 ngày", bars.get(0).note());
        assertEquals("warning", bars.get(0).noteTone());
        assertEquals("2 hồ sơ ở " + ApplicationStage.APPLIED.label() + " · 1 chờ quá 3 ngày · toàn công ty", bars.get(0).hint());
        assertEquals("/applications?stage=APPLIED", bars.get(0).href());
        assertNull(bars.get(1).note());
    }

    @Test
    @DisplayName("Xu hướng: 8 cột theo tuần từ thứ Hai, cột cao nhất bậc 10, cột cuối là tuần này")
    void build_shouldBucketApplicationsByWeek_whenDatesGiven() {
        // Arrange — thứ Hai tuần này là 26/10, tuần đầu là 07/09
        when(metrics.applicationDates(any(), any())).thenReturn(List.of(
                LocalDateTime.of(2026, 9, 7, 8, 0),
                LocalDateTime.of(2026, 10, 26, 8, 0),
                LocalDateTime.of(2026, 10, 27, 8, 0)));

        // Act
        HrDashboardResponse result = build();
        List<DashboardColumnResponse> columns = result.weeklyApplications();

        // Assert
        assertEquals(8, columns.size());
        assertEquals("07/09", columns.get(0).label());
        assertEquals(1, columns.get(0).count());
        assertEquals(5, columns.get(0).level());
        assertEquals(2, columns.get(7).count());
        assertEquals(10, columns.get(7).level());
        assertTrue(columns.get(7).current());
        assertEquals("Tuần 26/10 – 01/11: 2 hồ sơ mới · toàn công ty", columns.get(7).hint());
        assertEquals("4 tuần gần nhất: 2 hồ sơ · ↑ 100%", result.trendSummary());
    }

    @Test
    @DisplayName("Hạn nộp là hôm nay -> badge đỏ 'Hết hạn hôm nay'")
    void build_shouldMarkDeadlineToday_whenPostingClosesToday() {
        // Arrange
        when(lists.openPostings(NOW)).thenReturn(List.of(
                new OpenPostingRow(1, "Java Dev", "Kỹ thuật", 1, NOW.plusHours(8), JobPostingAccess.STATUS_PUBLISHED, 9)));

        // Act
        OpenPostingItem posting = build().openPostings().get(0);

        // Assert
        assertEquals("Hết hạn hôm nay", posting.deadlineText());
        assertEquals("danger", posting.deadlineTone());
    }

    @Test
    @DisplayName("Tin đang mở: ít hồ sơ / vị trí lên đầu; trạng thái lấy từ PostingStatus; còn ≤ 7 ngày -> badge 'Còn N ngày'")
    void build_shouldSortAndLabelOpenPostings_whenPostingsOpen() {
        // Arrange
        when(lists.openPostings(NOW)).thenReturn(List.of(
                new OpenPostingRow(1, "Java Dev", "Kỹ thuật", 2, NOW.plusDays(20), JobPostingAccess.STATUS_PUBLISHED, 40),
                new OpenPostingRow(2, "Kế toán", null, 2, NOW.plusDays(20), JobPostingAccess.STATUS_PUBLISHED, 6),
                new OpenPostingRow(3, "Marketing", "Marketing", 1, NOW.plusDays(3), JobPostingAccess.STATUS_PUBLISHED, 12)));

        // Act
        HrDashboardResponse result = build();
        List<OpenPostingItem> postings = result.openPostings();

        // Assert
        assertEquals(3, result.openPostingCount());
        assertEquals("Kế toán", postings.get(0).title());
        assertEquals("Đang đăng", postings.get(0).statusLabel());
        assertEquals("Chưa xác định", postings.get(0).departmentName());
        assertEquals("Marketing", postings.get(1).title());
        assertEquals("Đang đăng", postings.get(1).statusLabel());
        assertEquals("Còn 3 ngày", postings.get(1).deadlineText());
        assertEquals("warning", postings.get(1).deadlineTone());
        assertEquals("Đang đăng", postings.get(2).statusLabel());
        assertNull(postings.get(2).deadlineText());
        assertEquals(40, postings.get(0).applicationsMax());
    }
}
