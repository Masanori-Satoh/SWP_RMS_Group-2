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
        LocalDateTime currentFrom = DashboardPeriod.currentFrom(now);
        LocalDateTime previousFrom = DashboardPeriod.previousFrom(now);

        long hired = metrics.hiredBetween(currentFrom, now);
        long previousHired = metrics.hiredBetween(previousFrom, currentFrom);

        return List.of(
                requisitionMetric(requisitionRows, now),
                offerMetric(offerRows, now),
                DashboardViews.flowMetric("userCheck", "Người nhận việc", hired, previousHired, "/applications?stage=HIRED"),
                acceptanceMetric(currentFrom, previousFrom, now));
    }

    /** "Yêu cầu chờ duyệt": dòng phụ là số yêu cầu đã chờ từ 7 ngày (đỏ). */
    private DashboardMetricResponse requisitionMetric(List<PendingRequisitionRow> rows, LocalDateTime now) {
        long longWaiting = 0;
        for (PendingRequisitionRow row : rows) {
            if (DashboardLabels.waitDays(row.waitingSince(), now) >= DashboardLabels.WAIT_DANGER_DAYS) {
                longWaiting++;
            }
        }
        String detail = "Không có yêu cầu chờ quá " + DashboardLabels.WAIT_DANGER_DAYS + " ngày";
        String tone = "neutral";
        if (longWaiting > 0) {
            detail = longWaiting + " yêu cầu chờ quá " + DashboardLabels.WAIT_DANGER_DAYS + " ngày";
            tone = "danger";
        } else if (!rows.isEmpty()) {
            tone = "warning";
        }
        return DashboardMetricResponse.backlog("inbox", "Yêu cầu chờ duyệt", rows.size(), detail,
                "/requisitions?status=" + RequisitionAccess.STATUS_PENDING_DIRECTOR, tone);
    }

    /** "Đề nghị chờ duyệt": dòng phụ là thời gian chờ lâu nhất (danh sách đã xếp chờ lâu nhất lên đầu). */
    private DashboardMetricResponse offerMetric(List<PendingOfferRow> rows, LocalDateTime now) {
        String detail = "Không có đề nghị chờ";
        String tone = "neutral";
        if (!rows.isEmpty()) {
            long longestWait = DashboardLabels.waitDays(rows.get(0).waitingSince(), now);
            detail = "Lâu nhất: " + DashboardLabels.waitText(longestWait);
            tone = DashboardLabels.waitTone(longestWait);
        }
        return DashboardMetricResponse.backlog("banknote", "Đề nghị chờ duyệt", rows.size(), detail,
                "/offers?status=" + InternalMetricsRepository.OFFER_PENDING_DIRECTOR, tone);
    }

    /**
     * Rule (D12): tỷ lệ nhận offer = Accepted / (Accepted + Declined) của các offer có kết quả trong 30 ngày.
     * Không có offer nào có kết quả thì hiện "—".
     */
    private DashboardMetricResponse acceptanceMetric(LocalDateTime currentFrom, LocalDateTime previousFrom,
            LocalDateTime now) {
        Integer rate = acceptanceRate(currentFrom, now);
        Integer previousRate = acceptanceRate(previousFrom, currentFrom);
        String value = "—";
        String delta = null;
        String deltaTone = null;
        if (rate != null) {
            value = rate + "%";
            delta = DashboardPeriod.pointChangeText(rate, previousRate);
            if (previousRate != null) {
                deltaTone = DashboardPeriod.changeTone(rate, previousRate);
            }
        }
        return new DashboardMetricResponse("percent", "Tỷ lệ nhận offer", value, delta, deltaTone,
                "so với 30 ngày trước", "/offers", "neutral");
    }

    private Integer acceptanceRate(LocalDateTime from, LocalDateTime to) {
        long accepted = metrics.offersWithStatusBetween(InternalMetricsRepository.OFFER_ACCEPTED, from, to);
        long declined = metrics.offersWithStatusBetween(InternalMetricsRepository.OFFER_DECLINED, from, to);
        return DashboardPeriod.percent(accepted, accepted + declined);
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
