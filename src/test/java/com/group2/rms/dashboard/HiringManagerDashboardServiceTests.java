package com.group2.rms.dashboard;

import com.group2.rms.candidate.application.pipeline.dto.ApplicationListResponse;
import com.group2.rms.candidate.application.pipeline.service.ApplicationPipelineService;
import com.group2.rms.candidate.domain.rule.ApplicationStage;
import com.group2.rms.dashboard.DashboardListRepository.AttentionRow;
import com.group2.rms.dashboard.DashboardListRepository.InterviewRow;
import com.group2.rms.dashboard.DashboardListRepository.RequisitionRow;
import com.group2.rms.dashboard.DashboardMetricsRepository.StatusCount;
import com.group2.rms.dashboard.dto.DashboardBarResponse;
import com.group2.rms.dashboard.dto.DashboardChartResponse;
import com.group2.rms.dashboard.dto.DashboardInterviewItem;
import com.group2.rms.dashboard.dto.DashboardMetricResponse;
import com.group2.rms.dashboard.dto.DashboardWorkItem;
import com.group2.rms.dashboard.dto.HiringManagerDashboardResponse;
import com.group2.rms.dashboard.dto.RequisitionItem;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Page;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST (Mockito): HiringManagerDashboardService; repository là mock, giờ cố định bằng Clock.fixed.
 */
class HiringManagerDashboardServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-26T02:00:00Z"),
            ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final int USER_ID = 7;
    private static final int DEPARTMENT_ID = 3;

    private DashboardMetricsRepository metrics;
    private DashboardListRepository lists;
    private ApplicationPipelineService applicationPipeline;
    private HiringManagerDashboardService service;

    @BeforeEach
    void setUp() {
        metrics = mock(DashboardMetricsRepository.class);
        lists = mock(DashboardListRepository.class);
        applicationPipeline = mock(ApplicationPipelineService.class);
        givenStageCounts(Map.of());
        service = new HiringManagerDashboardService(metrics, lists, applicationPipeline, CLOCK);
    }

    /** Màn /applications trả đủ 6 giai đoạn; giai đoạn không truyền vào = 0. */
    private void givenStageCounts(Map<ApplicationStage, Long> values) {
        Map<ApplicationStage, Long> counts = new EnumMap<>(ApplicationStage.class);
        for (ApplicationStage stage : ApplicationStage.values()) {
            counts.put(stage, values.getOrDefault(stage, 0L));
        }
        ApplicationListResponse listPage = new ApplicationListResponse(Page.empty(), counts, List.of(), null,
                null, false, true, RequisitionAccess.ROLE_HIRING_MANAGER);
        when(applicationPipeline.list(any(), any())).thenReturn(listPage);
    }

    private User hiringManager(Integer departmentId) {
        Department department = null;
        if (departmentId != null) {
            department = Department.builder().departmentId(departmentId).build();
        }
        return User.builder().userId(USER_ID).fullName("Nguyễn Văn HM").accountStatus("Active")
                .role(Role.builder().roleName(RequisitionAccess.ROLE_HIRING_MANAGER).build())
                .department(department).build();
    }

    @Nested
    @DisplayName("KPI")
    class Kpis {

        @Test
        @DisplayName("Có phòng ban -> 4 thẻ đúng số và link; hồ sơ = tổng mọi giai đoạn kể cả Đã dừng; phỏng vấn 7 ngày")
        void build_shouldReturnFourLinkedKpis_whenDepartmentAssigned() {
            // Arrange
            when(metrics.ownRequisitions(USER_ID)).thenReturn(5L);
            when(metrics.ownPendingRequisitions(USER_ID)).thenReturn(2L);
            givenStageCounts(Map.of(
                    ApplicationStage.SCREENING, 6L, ApplicationStage.INTERVIEW, 2L, ApplicationStage.CLOSED, 1L));
            when(metrics.interviewsForDepartmentBetween(DEPARTMENT_ID, NOW, NOW.plusDays(7))).thenReturn(4L);

            // Act
            List<DashboardMetricResponse> kpis = service.build(hiringManager(DEPARTMENT_ID)).kpis();

            // Assert
            assertEquals(4, kpis.size());
            assertEquals(5L, kpis.get(0).value());
            assertEquals("/requisitions", kpis.get(0).href());
            assertEquals(2L, kpis.get(1).value());
            assertEquals("/requisitions?status=" + RequisitionAccess.STATUS_PENDING_DIRECTOR, kpis.get(1).href());
            assertEquals("warning", kpis.get(1).tone());
            assertEquals("Hồ sơ ứng tuyển", kpis.get(2).title());
            assertEquals(9L, kpis.get(2).value());
            assertEquals("/applications", kpis.get(2).href());
            assertEquals("Yêu cầu của bạn đang chờ quyết định", kpis.get(1).detail());
            assertEquals(4L, kpis.get(3).value());
            assertEquals("/interviews", kpis.get(3).href());
            verify(metrics, times(1)).interviewsForDepartmentBetween(DEPARTMENT_ID, NOW, NOW.plusDays(7));
            // KPI và biểu đồ dùng chung một lần gọi màn danh sách hồ sơ
            verify(applicationPipeline, times(1)).list(any(), any());
        }

        @Test
        @DisplayName("Không có yêu cầu chờ duyệt -> thẻ 'Chờ duyệt' tông xám")
        void build_shouldUseNeutralTone_whenNothingPending() {
            // Arrange
            when(metrics.ownPendingRequisitions(USER_ID)).thenReturn(0L);

            // Act
            List<DashboardMetricResponse> kpis = service.build(hiringManager(DEPARTMENT_ID)).kpis();

            // Assert
            assertEquals("neutral", kpis.get(1).tone());
        }

        @Test
        @DisplayName("Có yêu cầu chờ Giám đốc ≥ 7 ngày -> thẻ 'Chờ duyệt' tô đỏ, dòng phụ nêu số yêu cầu chờ lâu")
        void build_shouldWarnOnPendingKpi_whenRequisitionsWaitTooLong() {
            // Arrange
            LocalDateTime staleBefore = NOW.toLocalDate().minusDays(6).atStartOfDay();
            when(metrics.ownPendingRequisitions(USER_ID)).thenReturn(3L);
            when(metrics.ownStalePendingRequisitions(USER_ID, staleBefore)).thenReturn(2L);

            // Act
            DashboardMetricResponse pendingKpi = service.build(hiringManager(DEPARTMENT_ID)).kpis().get(1);

            // Assert
            assertEquals(3L, pendingKpi.value());
            assertEquals("2 yêu cầu chờ quá 7 ngày", pendingKpi.detail());
            assertEquals("danger", pendingKpi.tone());
            // Mốc "chờ lâu" = đầu ngày (hôm nay − 6): gửi từ 7 ngày lịch trở lên mới tính
            verify(metrics, times(1)).ownStalePendingRequisitions(USER_ID, staleBefore);
        }

        @Test
        @DisplayName("Tài khoản chưa có phòng ban -> truy vấn với departmentId null và ghi rõ lý do trên thẻ")
        void build_shouldPassNullDepartment_whenUserHasNoDepartment() {
            // Act
            List<DashboardMetricResponse> kpis = service.build(hiringManager(null)).kpis();

            // Assert
            assertEquals("Tài khoản chưa được gán phòng ban", kpis.get(3).detail());
            verify(lists).departmentInterviews(isNull(), eq(NOW), eq(NOW.plusDays(7)),
                    eq(HiringManagerDashboardService.RECENT_LIMIT));
        }
    }

    @Nested
    @DisplayName("needsAttention")
    class NeedsAttention {

        @Test
        @DisplayName("6 việc -> chỉ hiện 4 việc đầu (giữ thứ tự repository trả về), còn lại báo '+2 việc khác'")
        void build_shouldShowOnlyFirstItems_whenMoreThanLimit() {
            // Arrange
            List<AttentionRow> rows = new ArrayList<>();
            for (int id = 1; id <= 6; id++) {
                rows.add(new AttentionRow(id, null, "Yêu cầu " + id, RequisitionAccess.STATUS_DRAFT, null, null));
            }
            when(lists.requisitionsNeedingAttention(USER_ID)).thenReturn(rows);

            // Act
            HiringManagerDashboardResponse result = service.build(hiringManager(DEPARTMENT_ID));

            // Assert
            assertEquals(HiringManagerDashboardService.ATTENTION_LIMIT, result.needsAttention().size());
            assertEquals("REQ-0001 · Yêu cầu 1", result.needsAttention().getFirst().title());
            assertEquals(2, result.moreNeedsAttention());
        }

        @Test
        @DisplayName("Đúng 4 việc -> hiện đủ 4, không có dòng '+N việc khác'")
        void build_shouldNotReportMore_whenExactlyAtLimit() {
            // Arrange
            List<AttentionRow> rows = new ArrayList<>();
            for (int id = 1; id <= HiringManagerDashboardService.ATTENTION_LIMIT; id++) {
                rows.add(new AttentionRow(id, null, "Yêu cầu " + id, RequisitionAccess.STATUS_DRAFT, null, null));
            }
            when(lists.requisitionsNeedingAttention(USER_ID)).thenReturn(rows);

            // Act
            HiringManagerDashboardResponse result = service.build(hiringManager(DEPARTMENT_ID));

            // Assert
            assertEquals(HiringManagerDashboardService.ATTENTION_LIMIT, result.needsAttention().size());
            assertEquals(0, result.moreNeedsAttention());
        }

        @Test
        @DisplayName("Bị từ chối có lý do -> hiện lý do, nút 'Chỉnh sửa' mở form sửa, không có thời gian chờ")
        void build_shouldShowRejectComment_whenRequisitionRejected() {
            // Arrange
            when(lists.requisitionsNeedingAttention(USER_ID)).thenReturn(List.of(
                    new AttentionRow(11, "REQ-0011", "Kỹ sư Java", RequisitionAccess.STATUS_REJECTED,
                            NOW.minusDays(3), "Ngân sách chưa duyệt")));

            // Act
            DashboardWorkItem item = service.build(hiringManager(DEPARTMENT_ID)).needsAttention().getFirst();

            // Assert
            assertEquals("Từ chối", item.tag());
            assertEquals("danger", item.tagTone());
            assertEquals("REQ-0011 · Kỹ sư Java", item.title());
            assertEquals("Ngân sách chưa duyệt", item.detail());
            assertEquals("Chỉnh sửa", item.actionLabel());
            assertEquals("/requisitions/edit/11", item.href());
            assertNull(item.waitText());
        }

        @Test
        @DisplayName("Bị từ chối nhưng Giám đốc để trống lý do -> câu mặc định")
        void build_shouldUseDefaultDetail_whenRejectCommentBlank() {
            // Arrange
            when(lists.requisitionsNeedingAttention(USER_ID)).thenReturn(List.of(
                    new AttentionRow(11, "REQ-0011", "Kỹ sư Java", RequisitionAccess.STATUS_REJECTED, null, "  ")));

            // Act
            DashboardWorkItem item = service.build(hiringManager(DEPARTMENT_ID)).needsAttention().getFirst();

            // Assert
            assertEquals("Giám đốc không ghi lý do từ chối", item.detail());
        }

        @Test
        @DisplayName("Bản nháp -> nút 'Gửi duyệt' mở form sửa, không có dòng giải thích")
        void build_shouldOfferSubmit_whenRequisitionIsDraft() {
            // Arrange
            when(lists.requisitionsNeedingAttention(USER_ID)).thenReturn(List.of(
                    new AttentionRow(12, "REQ-0012", "Kế toán", RequisitionAccess.STATUS_DRAFT, null, null)));

            // Act
            DashboardWorkItem item = service.build(hiringManager(DEPARTMENT_ID)).needsAttention().getFirst();

            // Assert
            assertEquals("Bản nháp", item.tag());
            assertEquals("Gửi duyệt", item.actionLabel());
            assertNull(item.detail());
            assertEquals("/requisitions/edit/12", item.href());
        }
    }

    @Test
    @DisplayName("Yêu cầu gần đây: chưa lưu mã -> sinh REQ-xxxx từ id; nhãn và link chi tiết đúng")
    void build_shouldMapRecentRequisitions() {
        // Arrange
        when(lists.recentRequisitions(USER_ID, HiringManagerDashboardService.RECENT_LIMIT)).thenReturn(List.of(
                new RequisitionRow(25, null, "Kỹ sư Java", 2, RequisitionAccess.STATUS_APPROVED, NOW)));

        // Act
        RequisitionItem item = service.build(hiringManager(DEPARTMENT_ID)).recentRequisitions().getFirst();

        // Assert
        assertEquals("REQ-0025", item.code());
        assertEquals("Đã duyệt", item.statusLabel());
        assertEquals("success", item.statusTone());
        assertEquals(2, item.positions());
        assertEquals("/requisitions/25", item.href());
    }

    @Test
    @DisplayName("Phỏng vấn: buổi trực tuyến không lộ link họp; buổi tại văn phòng hiện địa điểm")
    void build_shouldHideMeetingLink_whenInterviewIsOnline() {
        // Arrange
        when(lists.departmentInterviews(DEPARTMENT_ID, NOW, NOW.plusDays(7),
                HiringManagerDashboardService.RECENT_LIMIT)).thenReturn(List.of(
                new InterviewRow(1L, NOW.plusHours(2), NOW.plusHours(3), "Trần A", "Kỹ sư Java",
                        "Online_GoogleMeet", "https://meet.google.com/abc", "Scheduled", 2L),
                new InterviewRow(2L, NOW.plusDays(1), NOW.plusDays(1).plusHours(1), "Lê B", "Kế toán",
                        "Offline_Office", "Phòng 301", "Rescheduled", 3L)));

        // Act
        List<DashboardInterviewItem> items = service.build(hiringManager(DEPARTMENT_ID)).upcomingInterviews();

        // Assert
        assertEquals("Trực tuyến", items.get(0).formatLabel());
        assertNull(items.get(0).location());
        assertEquals(2, items.get(0).panelSize());
        assertEquals("Tại văn phòng", items.get(1).formatLabel());
        assertEquals("Phòng 301", items.get(1).location());
        assertEquals("Đã đổi lịch", items.get(1).stateLabel());
        assertEquals("warning", items.get(1).stateTone());
    }

    @Test
    @DisplayName("Biểu đồ: yêu cầu đủ 4 trạng thái; pipeline 5 giai đoạn (Đã dừng ghi ở dòng phạm vi), link lọc đúng")
    void build_shouldBuildRequisitionAndPipelineCharts() {
        // Arrange
        when(metrics.hiringManagerRequisitionStatuses(USER_ID, DEPARTMENT_ID)).thenReturn(List.of(
                new StatusCount(RequisitionAccess.STATUS_APPROVED, 6), new StatusCount(RequisitionAccess.STATUS_DRAFT, 2)));
        givenStageCounts(Map.of(
                ApplicationStage.SCREENING, 4L, ApplicationStage.HIRED, 1L, ApplicationStage.CLOSED, 12L));

        // Act
        List<DashboardChartResponse> charts = service.build(hiringManager(DEPARTMENT_ID)).charts();

        // Assert
        List<DashboardBarResponse> requisitionBars = charts.get(0).bars();
        assertEquals(2, charts.size());
        assertEquals(4, requisitionBars.size());
        assertEquals(new DashboardBarResponse("Bản nháp", 2, 6, "/requisitions?status=Draft"), requisitionBars.get(0));
        assertEquals(new DashboardBarResponse("Chờ duyệt", 0, 6, "/requisitions?status=Pending_Director"),
                requisitionBars.get(1));
        assertEquals(6, requisitionBars.get(2).count());
        List<DashboardBarResponse> pipelineBars = charts.get(1).bars();
        assertEquals(5, pipelineBars.size());
        assertEquals(new DashboardBarResponse(ApplicationStage.APPLIED.label(), 0, 4, "/applications?stage=APPLIED"),
                pipelineBars.get(0));
        assertEquals(new DashboardBarResponse(ApplicationStage.SCREENING.label(), 4, 4,
                "/applications?stage=SCREENING"), pipelineBars.get(1));
        assertEquals(1, pipelineBars.get(4).count());
        assertTrue(charts.get(1).scope().contains("Đã dừng: 12"));
    }

    @Test
    @DisplayName("Dashboard HM không đọc số liệu của role khác (toàn hệ thống / Director / Interviewer)")
    void build_shouldNotQueryOtherRoleMetrics() {
        // Act
        HiringManagerDashboardResponse result = service.build(hiringManager(DEPARTMENT_ID));

        // Assert
        assertEquals("Nguyễn Văn HM", result.fullName());
        verify(metrics, never()).requisitionsAwaitingDirector();
        verify(metrics, never()).upcomingInterviews(any());
        verify(metrics, never()).pendingInterviewEvaluations(anyInt());
        verify(metrics, never()).formalApplicationStatuses();
    }
}
