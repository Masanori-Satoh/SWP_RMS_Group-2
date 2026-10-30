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
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.service.RequisitionLabels;
import com.group2.rms.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * SERVICE: dựng dữ liệu dashboard Giám đốc (toàn công ty): KPI, yêu cầu và đề nghị chờ duyệt,
 * yêu cầu đang mở theo phòng ban, quyết định gần đây của chính Giám đốc.
 */
@Service
public class DirectorDashboardService {

    /** Số dòng tối đa của mỗi danh sách trên dashboard. */
    static final int LIST_LIMIT = 5;
    /** Giá trị JobRequisition.HiringType của tuyển thay thế (có người đã nghỉ). */
    static final String HIRING_TYPE_REPLACEMENT = "Replacement";

    private final InternalMetricsRepository metrics;
    private final DashboardListRepository lists;
    private final Clock clock;

    public DirectorDashboardService(InternalMetricsRepository metrics, DashboardListRepository lists, Clock clock) {
        this.metrics = metrics;
        this.lists = lists;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DirectorDashboardResponse build(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<PendingRequisitionRow> requisitionRows = lists.pendingRequisitions(user.getUserId());
        List<PendingOfferRow> offerRows = lists.pendingOffers();

        List<PendingRequisitionRow> shownRequisitions = DashboardViews.firstItems(requisitionRows, LIST_LIMIT);
        List<PendingOfferRow> shownOffers = DashboardViews.firstItems(offerRows, LIST_LIMIT);

        return new DirectorDashboardResponse(
                user.getRole().getRoleName(),
                user.getFullName(),
                kpis(requisitionRows, offerRows, now),
                pendingRequisitions(shownRequisitions, now),
                requisitionRows.size() - shownRequisitions.size(),
                pendingOffers(shownOffers, now),
                offerRows.size() - shownOffers.size(),
                departmentBars(lists.openRequisitionsByDepartment()),
                recentDecisions(user.getUserId()));
    }

    private List<DashboardMetricResponse> kpis(List<PendingRequisitionRow> requisitionRows,
            List<PendingOfferRow> offerRows, LocalDateTime now) {
        LocalDateTime monthStart = LocalDate.of(now.getYear(), now.getMonth(), 1).atStartOfDay();
        long hiredThisMonth = metrics.hiredBetween(monthStart, now);

        return List.of(
                requisitionMetric(requisitionRows),
                offerMetric(offerRows),
                DashboardMetricResponse.backlog("userCheck", "Ứng viên trúng tuyển trong tháng", hiredThisMonth,
                        "Trong tháng này", "/applications?stage=HIRED", "neutral"));
    }

    private DashboardMetricResponse requisitionMetric(List<PendingRequisitionRow> rows) {
        return DashboardMetricResponse.backlog("clock", "Yêu cầu tuyển dụng chờ duyệt", rows.size(),
                "Đang chờ phê duyệt", "/requisitions?status=" + RequisitionAccess.STATUS_PENDING_DIRECTOR,
                rows.isEmpty() ? "neutral" : "warning");
    }

    private DashboardMetricResponse offerMetric(List<PendingOfferRow> rows) {
        return DashboardMetricResponse.backlog("fileCheck", "Thư mời nhận việc chờ duyệt", rows.size(),
                "Đang chờ phê duyệt", "/offers?status=" + InternalMetricsRepository.OFFER_PENDING_DIRECTOR,
                "neutral");
    }

    private List<PendingRequisitionItem> pendingRequisitions(List<PendingRequisitionRow> rows, LocalDateTime now) {
        List<PendingRequisitionItem> items = new ArrayList<>();
        for (PendingRequisitionRow row : rows) {
            String code = row.requisitionCode();
            if (code == null || code.isBlank()) {
                code = RequisitionLabels.requisitionCode(row.requisitionId());
            }
            String department = row.departmentName();
            if (department == null) {
                department = "Chưa xác định";
            }
            // Rule: đợt 2 trở đi là tuyển lại cùng vị trí, ghi rõ để Giám đốc biết đợt trước chưa đủ người.
            String roundText = null;
            if (row.recruitmentRound() != null && row.recruitmentRound() > 1) {
                roundText = "Tuyển lại lần " + row.recruitmentRound();
            }
            long days = DashboardLabels.waitDays(row.waitingSince(), now);
            items.add(new PendingRequisitionItem(code, row.title(), department, row.requesterName(),
                    row.numberOfPositions(), RequisitionLabels.hiringType(row.hiringType()),
                    HIRING_TYPE_REPLACEMENT.equals(row.hiringType()), roundText,
                    DashboardLabels.waitText(days), DashboardLabels.waitTone(days),
                    "/requisitions/" + row.requisitionId()));
        }
        return items;
    }

    private List<PendingOfferItem> pendingOffers(List<PendingOfferRow> rows, LocalDateTime now) {
        List<PendingOfferItem> items = new ArrayList<>();
        for (PendingOfferRow row : rows) {
            long days = DashboardLabels.waitDays(row.waitingSince(), now);
            // Rule (D8): Giám đốc là người duyệt lương nên thấy mức lương đề xuất.
            items.add(new PendingOfferItem(DashboardViews.candidateName(row.candidateName()), row.positionTitle(),
                    DashboardLabels.money(row.proposedSalary()), DashboardLabels.waitText(days),
                    DashboardLabels.waitTone(days), "/offers/" + row.offerId()));
        }
        return items;
    }

    /** Mỗi phòng ban một thanh = chờ duyệt + đang tuyển; chữ phụ ghi số đang chờ duyệt. */
    private List<DashboardBarResponse> departmentBars(List<DepartmentRequisitionRow> rows) {
        long max = 1;
        for (DepartmentRequisitionRow row : rows) {
            max = Math.max(max, row.pending() + row.approved());
        }
        List<DashboardBarResponse> bars = new ArrayList<>();
        for (DepartmentRequisitionRow row : rows) {
            String note = null;
            if (row.pending() > 0) {
                note = row.pending() + " chờ duyệt";
            }
            String hint = (row.pending() + row.approved()) + " yêu cầu của " + row.departmentName() + " · "
                    + row.pending() + " chờ duyệt, " + row.approved() + " đang tuyển";
            bars.add(new DashboardBarResponse(row.departmentName(), row.pending() + row.approved(), max,
                    "/requisitions", note, "warning", hint));
        }
        return bars;
    }

    private List<DecisionItem> recentDecisions(int userId) {
        List<DecisionItem> items = new ArrayList<>();
        for (ApprovalActivity activity : metrics.recentDirectorActivity(userId, LIST_LIMIT)) {
            items.add(new DecisionItem(DashboardLabels.decisionType(activity.type()), activity.title(),
                    DashboardLabels.decisionStatus(activity.status()), DashboardLabels.decisionTone(activity.status()),
                    activity.at()));
        }
        return items;
    }
}
