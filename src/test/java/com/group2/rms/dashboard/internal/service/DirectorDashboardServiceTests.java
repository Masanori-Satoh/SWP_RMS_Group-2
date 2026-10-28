package com.group2.rms.dashboard.internal.service;

import com.group2.rms.dashboard.internal.dto.DashboardBarResponse;
import com.group2.rms.dashboard.internal.dto.DashboardMetricResponse;
import com.group2.rms.dashboard.internal.dto.DecisionItem;
import com.group2.rms.dashboard.internal.dto.DirectorDashboardResponse;
import com.group2.rms.dashboard.internal.dto.PendingOfferItem;
import com.group2.rms.dashboard.internal.dto.PendingRequisitionItem;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.DepartmentRequisitionRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.PendingOfferRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.PendingRequisitionRow;
import com.group2.rms.dashboard.internal.repository.InternalMetricsRepository;
import com.group2.rms.dashboard.internal.repository.InternalMetricsRepository.ApprovalActivity;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST (Mockito): DirectorDashboardService; repository là mock, giờ cố định bằng Clock.fixed.
 */
class DirectorDashboardServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-28T02:00:00Z"),
            ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final LocalDateTime CURRENT_FROM = NOW.minusDays(30);
    private static final LocalDateTime PREVIOUS_FROM = NOW.minusDays(60);
    private static final int USER_ID = 9;

    private InternalMetricsRepository metrics;
    private DashboardListRepository lists;
    private DirectorDashboardService service;

    @BeforeEach
    void setUp() {
        metrics = mock(InternalMetricsRepository.class);
        lists = mock(DashboardListRepository.class);
        service = new DirectorDashboardService(metrics, lists, CLOCK);
    }

    private User director() {
        return User.builder().userId(USER_ID).fullName("Giám đốc").accountStatus("Active")
                .role(Role.builder().roleName("Director").build()).build();
    }

    private PendingRequisitionRow requisition(int id, String hiringType, Integer round, int waitedDays) {
        return new PendingRequisitionRow(id, null, "Vị trí " + id, "Kỹ thuật", "Hoài Nam", 2, hiringType, round,
                NOW.minusDays(waitedDays));
    }

    private void givenOffers(String status, LocalDateTime from, LocalDateTime to, long count) {
        when(metrics.offersWithStatusBetween(eq(status), eq(from), eq(to))).thenReturn(count);
    }

    @Test
    @DisplayName("Yêu cầu chờ: mã sinh từ id, tuyển thay thế được nhấn, đợt 2 ghi 'Tuyển lại lần 2', chờ 9 ngày tô đỏ")
    void build_shouldMapPendingRequisition_whenReplacementSecondRound() {
        // Arrange
        when(lists.pendingRequisitions(USER_ID)).thenReturn(List.of(
                requisition(12, "Replacement", 2, 9),
                requisition(13, "New", 1, 1)));

        // Act
        List<PendingRequisitionItem> items = service.build(director()).pendingRequisitions();

        // Assert
        assertEquals("REQ-0012", items.get(0).code());
        assertTrue(items.get(0).replacement());
        assertEquals("Tuyển thay thế", items.get(0).hiringTypeLabel());
        assertEquals("Tuyển lại lần 2", items.get(0).roundText());
        assertEquals("danger", items.get(0).waitTone());
        assertFalse(items.get(1).replacement());
        assertNull(items.get(1).roundText());
        assertEquals("/requisitions/13", items.get(1).href());
    }

    @Test
    @DisplayName("KPI yêu cầu chờ: đếm toàn bộ (kể cả ngoài 5 dòng hiện), dòng phụ = số yêu cầu chờ từ 7 ngày")
    void build_shouldCountAllPendingRequisitions_whenMoreThanLimit() {
        // Arrange
        List<PendingRequisitionRow> rows = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            rows.add(requisition(i, "New", 1, i));
        }
        when(lists.pendingRequisitions(USER_ID)).thenReturn(rows);

        // Act
        DirectorDashboardResponse result = service.build(director());
        DashboardMetricResponse kpi = result.kpis().get(0);

        // Assert
        assertEquals("7", kpi.value());
        assertEquals("1 yêu cầu chờ quá 7 ngày", kpi.detail());
        assertEquals("danger", kpi.tone());
        assertEquals(5, result.pendingRequisitions().size());
        assertEquals(2, result.morePendingRequisitions());
    }

    @Test
    @DisplayName("Đề nghị chờ: lương có dấu chấm, link chi tiết; KPI 'Lâu nhất' lấy dòng đầu (chờ lâu nhất)")
    void build_shouldMapPendingOffers_whenOffersWaiting() {
        // Arrange
        when(lists.pendingOffers()).thenReturn(List.of(
                new PendingOfferRow(31, "Lê Thu Hà", "Data Analyst", new BigDecimal("25000000"), NOW.minusDays(4)),
                new PendingOfferRow(32, null, "Kế toán", new BigDecimal("15000000"), NOW.minusDays(1))));

        // Act
        DirectorDashboardResponse result = service.build(director());
        List<PendingOfferItem> offers = result.pendingOffers();

        // Assert
        assertEquals("25.000.000 VND", offers.get(0).salaryText());
        assertEquals("/offers/31", offers.get(0).href());
        assertEquals("Ứng viên chưa có tên", offers.get(1).candidateName());
        assertEquals("Lâu nhất: 4 ngày", result.kpis().get(1).detail());
        assertEquals("warning", result.kpis().get(1).tone());
    }

    @Test
    @DisplayName("Tỷ lệ nhận offer: 17/20 = 85%, kỳ trước 6/8 = 75% -> '↑ 10 điểm'")
    void build_shouldComputeAcceptanceRate_whenOffersDecided() {
        // Arrange
        givenOffers(InternalMetricsRepository.OFFER_ACCEPTED, CURRENT_FROM, NOW, 17);
        givenOffers(InternalMetricsRepository.OFFER_DECLINED, CURRENT_FROM, NOW, 3);
        givenOffers(InternalMetricsRepository.OFFER_ACCEPTED, PREVIOUS_FROM, CURRENT_FROM, 6);
        givenOffers(InternalMetricsRepository.OFFER_DECLINED, PREVIOUS_FROM, CURRENT_FROM, 2);

        // Act
        DashboardMetricResponse rate = service.build(director()).kpis().get(3);

        // Assert
        assertEquals("85%", rate.value());
        assertEquals("↑ 10 điểm", rate.delta());
        assertEquals("success", rate.deltaTone());
    }

    @Test
    @DisplayName("Chưa có offer nào có kết quả -> tỷ lệ '—', không có mũi tên")
    void build_shouldShowDash_whenNoOfferDecided() {
        // Act
        DashboardMetricResponse rate = service.build(director()).kpis().get(3);

        // Assert
        assertEquals("—", rate.value());
        assertNull(rate.delta());
    }

    @Test
    @DisplayName("Theo phòng ban: thanh = chờ duyệt + đang tuyển, chữ phụ chỉ khi có yêu cầu chờ duyệt")
    void build_shouldBuildDepartmentBars_whenRequisitionsOpen() {
        // Arrange
        when(lists.openRequisitionsByDepartment()).thenReturn(List.of(
                new DepartmentRequisitionRow("Kỹ thuật", 3, 15),
                new DepartmentRequisitionRow("Tài chính", 0, 4)));

        // Act
        List<DashboardBarResponse> bars = service.build(director()).openByDepartment();

        // Assert
        assertEquals(18, bars.get(0).count());
        assertEquals(18, bars.get(0).max());
        assertEquals("3 chờ duyệt", bars.get(0).note());
        assertNull(bars.get(1).note());
    }

    @Test
    @DisplayName("Quyết định gần đây: nhãn tiếng Việt cho loại và kết quả")
    void build_shouldTranslateDecisions_whenActivityExists() {
        // Arrange
        when(metrics.recentDirectorActivity(USER_ID, DirectorDashboardService.LIST_LIMIT)).thenReturn(List.of(
                new ApprovalActivity(ApprovalActivity.OFFER, "Sales Executive", "Rejected", NOW)));

        // Act
        DecisionItem decision = service.build(director()).recentDecisions().get(0);

        // Assert
        assertEquals("Đề nghị tuyển dụng", decision.typeLabel());
        assertEquals("Đã từ chối", decision.statusLabel());
        assertEquals("danger", decision.statusTone());
    }
}
