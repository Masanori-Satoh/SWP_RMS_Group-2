package com.group2.rms.dashboard.internal.service;

import com.group2.rms.dashboard.internal.dto.DashboardMetricResponse;
import com.group2.rms.dashboard.internal.dto.DashboardWorkItem;
import com.group2.rms.dashboard.internal.dto.HiringManagerDashboardResponse;
import com.group2.rms.dashboard.internal.dto.HiringProgressItem;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.AttentionRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.ProgressRow;
import com.group2.rms.dashboard.internal.repository.InternalMetricsRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST (Mockito): HiringManagerDashboardService; repository là mock, giờ cố định bằng Clock.fixed.
 */
class HiringManagerDashboardServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-28T02:00:00Z"),
            ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final int USER_ID = 7;
    private static final int DEPARTMENT_ID = 3;

    private InternalMetricsRepository metrics;
    private DashboardListRepository lists;
    private HiringManagerDashboardService service;

    @BeforeEach
    void setUp() {
        metrics = mock(InternalMetricsRepository.class);
        lists = mock(DashboardListRepository.class);
        service = new HiringManagerDashboardService(metrics, lists, CLOCK);
    }

    private User hiringManager(Integer departmentId) {
        Department department = null;
        if (departmentId != null) {
            department = Department.builder().departmentId(departmentId).departmentName("Kỹ thuật").build();
        }
        return User.builder().userId(USER_ID).fullName("Nguyễn Văn HM").accountStatus("Active")
                .role(Role.builder().roleName(RequisitionAccess.ROLE_HIRING_MANAGER).build())
                .department(department).build();
    }

    private ProgressRow progressRow(int id, int needed, long hired, long interviewing, long offered) {
        return new ProgressRow(id, "REQ-" + id, "Vị trí " + id, needed, hired, interviewing, offered);
    }

    @Nested
    @DisplayName("KPI")
    class Kpis {

        @Test
        @DisplayName("3 thẻ: chờ Giám đốc duyệt, đã đăng tuyển, phỏng vấn 7 ngày tới; icon và link đúng thứ tự")
        void build_shouldReturnThreeKpis_whenDepartmentAssigned() {
            // Arrange
            when(metrics.countPendingRequisitionsInScope(USER_ID, DEPARTMENT_ID)).thenReturn(5L);
            when(metrics.countPostedDepartmentRequisitions(USER_ID, DEPARTMENT_ID)).thenReturn(7L);
            when(metrics.countUpcomingDepartmentInterviews(USER_ID, DEPARTMENT_ID, NOW, NOW.plusDays(7))).thenReturn(8L);

            // Act
            List<DashboardMetricResponse> kpis = service.build(hiringManager(DEPARTMENT_ID)).kpis();

            // Assert
            assertEquals(List.of("clock", "megaphone", "calendarDays"),
                    kpis.stream().map(DashboardMetricResponse::icon).toList());
            assertEquals(List.of("5", "7", "8"), kpis.stream().map(DashboardMetricResponse::value).toList());
            assertEquals("Đang chờ phê duyệt", kpis.get(0).detail());
            assertEquals("warning", kpis.get(0).tone());
            assertEquals("/requisitions?status=" + RequisitionAccess.STATUS_PENDING_DIRECTOR, kpis.get(0).href());
            assertEquals("/internal/job-postings?status=Published", kpis.get(1).href());
            assertEquals("Trong 7 ngày tới", kpis.get(2).detail());
            assertEquals("/interviews", kpis.get(2).href());
        }

        @Test
        @DisplayName("Không có yêu cầu chờ -> thẻ chờ duyệt xám")
        void build_shouldMarkPendingNeutral_whenNothingPending() {
            // Act
            DashboardMetricResponse pending = service.build(hiringManager(DEPARTMENT_ID)).kpis().get(0);

            // Assert
            assertEquals("0", pending.value());
            assertEquals("neutral", pending.tone());
        }

        @Test
        @DisplayName("Tài khoản chưa có phòng ban -> truy vấn với departmentId null, phạm vi ghi rõ")
        void build_shouldUseNullDepartment_whenAccountHasNoDepartment() {
            // Act
            HiringManagerDashboardResponse result = service.build(hiringManager(null));

            // Assert
            assertEquals("Chưa được gán phòng ban", result.scopeLabel());
            verify(metrics).countPostedDepartmentRequisitions(eq(USER_ID), isNull());
            verify(metrics).countUpcomingDepartmentInterviews(eq(USER_ID), isNull(), any(), any());
            verify(lists).hiringProgress(eq(USER_ID), isNull());
        }
    }

    @Nested
    @DisplayName("Tiến độ theo vị trí")
    class Progress {

        @Test
        @DisplayName("Thanh chồng: đã tuyển / còn thiếu; dòng phụ = đang phỏng vấn + có offer, đủ người thì báo chờ đóng")
        void build_shouldSplitHiredAndMissing_whenRequisitionsInProgress() {
            // Arrange
            when(lists.hiringProgress(USER_ID, DEPARTMENT_ID)).thenReturn(List.of(
                    progressRow(1, 3, 3, 0, 0),
                    progressRow(2, 3, 1, 1, 0),
                    progressRow(3, 4, 2, 1, 1)));

            // Act
            List<HiringProgressItem> items = service.build(hiringManager(DEPARTMENT_ID)).progress();

            // Assert
            assertEquals(0, items.get(0).missing());
            assertEquals("Đã tuyển đủ, chờ đóng yêu cầu", items.get(0).pipelineText());
            assertEquals(2, items.get(1).missing());
            assertEquals("1 đang phỏng vấn · 0 có offer", items.get(1).pipelineText());
            assertEquals(2, items.get(2).hired());
            assertEquals("/requisitions/3", items.get(2).href());
        }

        @Test
        @DisplayName("Hơn 5 yêu cầu đang tuyển -> chỉ hiện 5")
        void build_shouldShowFiveRows_whenMoreRequisitionsInProgress() {
            // Arrange
            List<ProgressRow> rows = new ArrayList<>();
            for (int i = 1; i <= 7; i++) {
                rows.add(progressRow(i, 1, 0, 0, 0));
            }
            when(lists.hiringProgress(USER_ID, DEPARTMENT_ID)).thenReturn(rows);

            // Act
            HiringManagerDashboardResponse result = service.build(hiringManager(DEPARTMENT_ID));

            // Assert
            assertEquals(HiringManagerDashboardService.LIST_LIMIT, result.progress().size());
        }
    }

    @Nested
    @DisplayName("Việc cần xử lý")
    class NeedsAttention {

        @Test
        @DisplayName("Bị từ chối có lý do -> nút Chỉnh sửa; bản nháp -> nút Gửi duyệt, không có dòng giải thích")
        void build_shouldMapRejectedAndDraft_whenAttentionRowsExist() {
            // Arrange
            when(lists.requisitionsNeedingAttention(USER_ID)).thenReturn(List.of(
                    new AttentionRow(5, "REQ-0005", "DevOps", RequisitionAccess.STATUS_REJECTED, NOW, "Thiếu ngân sách"),
                    new AttentionRow(6, null, "QA", RequisitionAccess.STATUS_DRAFT, null, null)));

            // Act
            List<DashboardWorkItem> work = service.build(hiringManager(DEPARTMENT_ID)).needsAttention();

            // Assert
            assertEquals("REQ-0005 · DevOps", work.get(0).title());
            assertEquals("Thiếu ngân sách", work.get(0).detail());
            assertEquals("Chỉnh sửa", work.get(0).actionLabel());
            assertEquals("REQ-0006 · QA", work.get(1).title());
            assertNull(work.get(1).detail());
            assertEquals("Gửi duyệt", work.get(1).actionLabel());
        }

        @Test
        @DisplayName("7 việc -> hiện 5, còn 2 việc khác")
        void build_shouldCountRemainingWork_whenMoreThanLimit() {
            // Arrange
            List<AttentionRow> rows = new ArrayList<>();
            for (int i = 1; i <= 7; i++) {
                rows.add(new AttentionRow(i, "REQ-" + i, "Nháp " + i, RequisitionAccess.STATUS_DRAFT, null, null));
            }
            when(lists.requisitionsNeedingAttention(USER_ID)).thenReturn(rows);

            // Act
            HiringManagerDashboardResponse result = service.build(hiringManager(DEPARTMENT_ID));

            // Assert
            assertEquals(5, result.needsAttention().size());
            assertEquals(2, result.moreNeedsAttention());
        }
    }
}
