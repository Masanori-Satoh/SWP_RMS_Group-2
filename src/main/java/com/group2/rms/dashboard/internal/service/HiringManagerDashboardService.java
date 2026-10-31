package com.group2.rms.dashboard.internal.service;

import com.group2.rms.dashboard.internal.dto.DashboardInterviewItem;
import com.group2.rms.dashboard.internal.dto.DashboardMetricResponse;
import com.group2.rms.dashboard.internal.dto.DashboardWorkItem;
import com.group2.rms.dashboard.internal.dto.HiringManagerDashboardResponse;
import com.group2.rms.dashboard.internal.dto.HiringProgressItem;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.AttentionRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.ProgressRow;
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
 * SERVICE: dựng dữ liệu dashboard Hiring Manager (KPI, việc cần xử lý, lịch phỏng vấn, tiến độ tuyển).
 * Phạm vi: yêu cầu của HM và phòng ban HM thuộc / quản lý.
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
                scopeLabel,
                kpis(userId, departmentId, now),
                shownWork,
                allWork.size() - shownWork.size(),
                upcomingInterviews(userId, departmentId, now),
                progress(DashboardViews.firstItems(progressRows, LIST_LIMIT)));
    }

    private List<DashboardMetricResponse> kpis(int userId, Integer departmentId, LocalDateTime now) {
        long pending = metrics.countPendingRequisitionsInScope(userId, departmentId);
        long posted = metrics.countPostedDepartmentRequisitions(userId, departmentId);
        long interviews = metrics.countUpcomingDepartmentInterviews(userId, departmentId, now,
                now.plusDays(INTERVIEW_WINDOW_DAYS));

        return List.of(
                // HM không tự xử lý được yêu cầu đang chờ Giám đốc nên chỉ báo ở thẻ, không đưa vào "Cần bạn xử lý".
                DashboardMetricResponse.backlog("clock", "Yêu cầu chờ Giám đốc duyệt", pending,
                        "Đang chờ phê duyệt",
                        "/requisitions?status=" + RequisitionAccess.STATUS_PENDING_DIRECTOR,
                        pending > 0 ? "warning" : "neutral"),
                DashboardMetricResponse.backlog("megaphone", "Yêu cầu đã đăng tuyển", posted,
                        "Đã được HR đăng tin", "/internal/job-postings?status=Published", "neutral"),
                DashboardMetricResponse.backlog("calendarDays", "Buổi phỏng vấn sắp tới", interviews,
                        "Trong " + INTERVIEW_WINDOW_DAYS + " ngày tới", "/interviews", "neutral"));
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
        String title = RequisitionLabels.requisitionCode(row.requisitionId(), row.requisitionCode()) + " · " + row.title();
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

    private List<DashboardInterviewItem> upcomingInterviews(int userId, Integer departmentId, LocalDateTime now) {
        return DashboardViews.interviewItems(lists.departmentInterviews(userId, departmentId, now,
                now.plusDays(INTERVIEW_WINDOW_DAYS), LIST_LIMIT), now);
    }

    private List<HiringProgressItem> progress(List<ProgressRow> rows) {
        List<HiringProgressItem> items = new ArrayList<>();
        for (ProgressRow row : rows) {
            items.add(progressItem(row));
        }
        return items;
    }

    /** Dòng phụ trong tooltip của thanh chồng: số đang phỏng vấn / có offer, hoặc đã đủ người. */
    private HiringProgressItem progressItem(ProgressRow row) {
        long needed = row.numberOfPositions();
        String pipelineText = row.interviewing() + " đang phỏng vấn · " + row.offered() + " có offer";
        if (row.hired() >= needed) {
            pipelineText = "Đã tuyển đủ, chờ đóng yêu cầu";
        }
        return new HiringProgressItem(RequisitionLabels.requisitionCode(row.requisitionId(), row.requisitionCode()),
                row.title(), row.hired(), needed, pipelineText, detailHref(row.requisitionId()));
    }

    private String detailHref(int requisitionId) {
        return "/requisitions/" + requisitionId;
    }

    private String editHref(int requisitionId) {
        return "/requisitions/edit/" + requisitionId;
    }
}
