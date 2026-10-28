package com.group2.rms.dashboard.internal.service;

import com.group2.rms.dashboard.internal.dto.DashboardInterviewItem;
import com.group2.rms.dashboard.internal.dto.DashboardMetricResponse;
import com.group2.rms.dashboard.internal.dto.DashboardWorkItem;
import com.group2.rms.dashboard.internal.dto.HiringManagerDashboardResponse;
import com.group2.rms.dashboard.internal.dto.HiringProgressItem;
import com.group2.rms.dashboard.internal.dto.RequisitionItem;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.AttentionRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.ProgressRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.RequisitionRow;
import com.group2.rms.dashboard.internal.repository.InternalMetricsRepository;
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
 * SERVICE: dựng dữ liệu dashboard Hiring Manager (KPI, việc cần xử lý, lịch phỏng vấn, tiến độ tuyển, yêu cầu
 * cần theo dõi). Phạm vi: yêu cầu của HM và phòng ban HM thuộc / quản lý.
 */
@Service
public class HiringManagerDashboardService {

    /** Số dòng tối đa của mỗi danh sách trên dashboard. */
    static final int LIST_LIMIT = 5;
    /** KPI và danh sách phỏng vấn chỉ tính 7 ngày tới. */
    static final int INTERVIEW_WINDOW_DAYS = 7;

    private final InternalMetricsRepository metrics;
    private final DashboardListRepository lists;
    private final Clock clock;

    public HiringManagerDashboardService(InternalMetricsRepository metrics, DashboardListRepository lists,
            Clock clock) {
        this.metrics = metrics;
        this.lists = lists;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public HiringManagerDashboardResponse build(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        int userId = user.getUserId();
        Integer departmentId = null;
        String scopeLabel = "Chưa được gán phòng ban";
        if (user.getDepartment() != null) {
            departmentId = user.getDepartment().getDepartmentId();
            scopeLabel = "Phòng " + user.getDepartment().getDepartmentName();
        }

        List<ProgressRow> progressRows = lists.hiringProgress(userId, departmentId);
        List<DashboardWorkItem> allWork = needsAttention(userId);
        List<DashboardWorkItem> shownWork = DashboardViews.firstItems(allWork, LIST_LIMIT);

        return new HiringManagerDashboardResponse(
                user.getRole().getRoleName(),
                user.getFullName(),
                scopeLabel + " · 30 ngày gần nhất",
                kpis(userId, departmentId, now, progressRows),
                shownWork,
                allWork.size() - shownWork.size(),
                upcomingInterviews(departmentId, now),
                progress(DashboardViews.firstItems(progressRows, LIST_LIMIT)),
                recentRequisitions(userId));
    }

    private List<DashboardMetricResponse> kpis(int userId, Integer departmentId, LocalDateTime now,
            List<ProgressRow> progressRows) {
        LocalDateTime currentFrom = DashboardPeriod.currentFrom(now);
        LocalDateTime previousFrom = DashboardPeriod.previousFrom(now);

        long newApplications = metrics.departmentApplicationsBetween(userId, departmentId, currentFrom, now);
        long previousApplications = metrics.departmentApplicationsBetween(userId, departmentId, previousFrom,
                currentFrom);

        long hired = 0;
        long needed = 0;
        for (ProgressRow row : progressRows) {
            hired += row.hired();
            needed += row.numberOfPositions();
        }

        long completedInterviews = metrics.completedDepartmentInterviewsBetween(departmentId, currentFrom, now);
        long previousInterviews = metrics.completedDepartmentInterviewsBetween(departmentId, previousFrom,
                currentFrom);

        return List.of(
                DashboardViews.flowMetric("file", "Hồ sơ mới", newApplications, previousApplications, "/applications"),
                new DashboardMetricResponse("target", "Đã tuyển / cần tuyển", hired + "/" + needed, null, null,
                        "Các yêu cầu đang tuyển", "/requisitions?status=" + RequisitionAccess.STATUS_APPROVED,
                        "neutral"),
                pendingMetric(userId, now),
                DashboardViews.flowMetric("calendarCheck", "Phỏng vấn đã diễn ra", completedInterviews, previousInterviews,
                        "/interviews"));
    }

    /** "Chờ duyệt": số yêu cầu của HM đang chờ Giám đốc, dòng phụ là thời gian chờ lâu nhất. */
    private DashboardMetricResponse pendingMetric(int userId, LocalDateTime now) {
        long pending = metrics.ownPendingRequisitions(userId);
        String detail = "Không có yêu cầu chờ duyệt";
        String tone = "neutral";
        if (pending > 0) {
            // Rule: HM không tự xử lý được (đang chờ Giám đốc) nên chỉ báo ở thẻ, không đưa vào "Cần bạn xử lý".
            long waitDays = DashboardLabels.waitDays(metrics.oldestOwnPendingSubmittedAt(userId), now);
            detail = "Lâu nhất: " + DashboardLabels.waitText(waitDays);
            tone = DashboardLabels.waitTone(waitDays);
        }
        return DashboardMetricResponse.backlog("clock", "Chờ Giám đốc duyệt", pending, detail,
                "/requisitions?status=" + RequisitionAccess.STATUS_PENDING_DIRECTOR, tone);
    }

    /** Việc HM tự làm được, đã xếp sẵn: bị từ chối trước, bản nháp sau. */
    private List<DashboardWorkItem> needsAttention(int userId) {
        List<DashboardWorkItem> items = new ArrayList<>();
        for (AttentionRow row : lists.requisitionsNeedingAttention(userId)) {
            items.add(workItem(row));
        }
        return items;
    }

    private DashboardWorkItem workItem(AttentionRow row) {
        String status = row.approvalStatus();
        String title = codeOf(row.requisitionId(), row.requisitionCode()) + " · " + row.title();
        String tag = RequisitionLabels.approvalStatus(status);
        String tagTone = RequisitionLabels.approvalTone(status);

        if (RequisitionAccess.STATUS_REJECTED.equals(status)) {
            String detail = "Giám đốc không ghi lý do từ chối";
            if (row.rejectComment() != null && !row.rejectComment().isBlank()) {
                detail = row.rejectComment();
            }
            return new DashboardWorkItem(tag, tagTone, title, detail, null, null,
                    "Chỉnh sửa", editHref(row.requisitionId()), false);
        }
        // Còn lại là bản nháp: badge trạng thái đã đủ nghĩa, không cần dòng giải thích.
        return new DashboardWorkItem(tag, tagTone, title, null, null, null,
                "Gửi duyệt", editHref(row.requisitionId()), false);
    }

    private List<DashboardInterviewItem> upcomingInterviews(Integer departmentId, LocalDateTime now) {
        return DashboardViews.interviewItems(lists.departmentInterviews(departmentId, now,
                now.plusDays(INTERVIEW_WINDOW_DAYS), LIST_LIMIT), now);
    }

    private List<HiringProgressItem> progress(List<ProgressRow> rows) {
        List<HiringProgressItem> items = new ArrayList<>();
        for (ProgressRow row : rows) {
            items.add(progressItem(row));
        }
        return items;
    }

    /**
     * Rule (D11): còn thiếu = số cần tuyển − số đã nhận việc. "Thiếu ứng viên" khi số người đang phỏng vấn
     * cộng đã có offer ít hơn số còn thiếu.
     */
    private HiringProgressItem progressItem(ProgressRow row) {
        long needed = row.numberOfPositions();
        long missing = Math.max(needed - row.hired(), 0);
        long inPipeline = row.interviewing() + row.offered();

        String stateLabel = "Đang tiến triển";
        String stateTone = "neutral";
        String pipelineText = row.interviewing() + " đang phỏng vấn · " + row.offered() + " có offer · còn thiếu "
                + missing;
        if (missing == 0) {
            stateLabel = "Đã đủ người";
            stateTone = "success";
            pipelineText = "Đã tuyển đủ, chờ đóng yêu cầu";
        } else if (inPipeline < missing) {
            stateLabel = "Thiếu ứng viên";
            stateTone = "warning";
        }
        return new HiringProgressItem(codeOf(row.requisitionId(), row.requisitionCode()), row.title(),
                row.hired(), needed, stateLabel, stateTone, pipelineText, detailHref(row.requisitionId()));
    }

    private List<RequisitionItem> recentRequisitions(int userId) {
        List<RequisitionItem> items = new ArrayList<>();
        for (RequisitionRow row : lists.recentRequisitions(userId, LIST_LIMIT)) {
            items.add(new RequisitionItem(
                    codeOf(row.requisitionId(), row.requisitionCode()),
                    row.title(),
                    row.numberOfPositions(),
                    RequisitionLabels.approvalStatus(row.approvalStatus()),
                    RequisitionLabels.approvalTone(row.approvalStatus()),
                    row.updatedAt(),
                    detailHref(row.requisitionId())));
        }
        return items;
    }

    /** Yêu cầu cũ có thể chưa lưu mã thì sinh lại từ id (cùng định dạng REQ-0001). */
    private String codeOf(int requisitionId, String requisitionCode) {
        if (requisitionCode == null || requisitionCode.isBlank()) {
            return RequisitionLabels.requisitionCode(requisitionId);
        }
        return requisitionCode;
    }

    private String detailHref(int requisitionId) {
        return "/requisitions/" + requisitionId;
    }

    private String editHref(int requisitionId) {
        return "/requisitions/edit/" + requisitionId;
    }
}
