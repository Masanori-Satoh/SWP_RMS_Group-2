package com.group2.rms.dashboard;

import com.group2.rms.candidate.application.pipeline.dto.ApplicationListResponse;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationSearch;
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
import com.group2.rms.interview.entity.InterviewFormat;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.service.RequisitionLabels;
import com.group2.rms.user.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/*
 * SERVICE: dựng dữ liệu dashboard Hiring Manager (KPI, yêu cầu gần đây, lịch phỏng vấn, việc cần xử lý, biểu đồ).
 */
@Service
public class HiringManagerDashboardService {

    /** Số dòng tối đa của "Yêu cầu gần đây" và "Phỏng vấn sắp tới". */
    static final int RECENT_LIMIT = 5;
    /** KPI và danh sách phỏng vấn chỉ tính 7 ngày tới. */
    static final int INTERVIEW_WINDOW_DAYS = 7;
    /** Khối "Công việc cần xử lý" chỉ hiện vài việc ưu tiên nhất để không đẩy các khối khác xuống. */
    static final int ATTENTION_LIMIT = 4;

    private static final List<String> REQUISITION_STATUSES = List.of(
            RequisitionAccess.STATUS_DRAFT, RequisitionAccess.STATUS_PENDING_DIRECTOR,
            RequisitionAccess.STATUS_APPROVED, RequisitionAccess.STATUS_REJECTED);

    private final DashboardMetricsRepository metrics;
    private final DashboardListRepository lists;
    private final ApplicationPipelineService applicationPipeline;
    private final Clock clock;

    public HiringManagerDashboardService(DashboardMetricsRepository metrics, DashboardListRepository lists,
            ApplicationPipelineService applicationPipeline, Clock clock) {
        this.metrics = metrics;
        this.lists = lists;
        this.applicationPipeline = applicationPipeline;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public HiringManagerDashboardResponse build(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        int userId = user.getUserId();
        Integer departmentId = null;
        if (user.getDepartment() != null) {
            departmentId = user.getDepartment().getDepartmentId();
        }
        Map<ApplicationStage, Long> stageCounts = applicationStageCounts();
        List<DashboardWorkItem> allWork = needsAttention(userId);
        List<DashboardWorkItem> shownWork = allWork;
        int moreWork = 0;
        if (allWork.size() > ATTENTION_LIMIT) {
            shownWork = List.copyOf(allWork.subList(0, ATTENTION_LIMIT));
            moreWork = allWork.size() - ATTENTION_LIMIT;
        }
        return new HiringManagerDashboardResponse(
                user.getRole().getRoleName(),
                user.getFullName(),
                kpis(userId, departmentId, now, stageCounts),
                recentRequisitions(userId),
                upcomingInterviews(departmentId, now),
                shownWork,
                moreWork,
                charts(userId, departmentId, stageCounts));
    }

    /**
     * Số hồ sơ mỗi giai đoạn, lấy từ đúng màn /applications (cùng phạm vi HM, cùng ApplicationProgressResolver)
     * để số trên dashboard khớp số trên các tab của màn danh sách. Chỉ xin trang đầu 1 dòng vì chỉ cần số đếm.
     */
    private Map<ApplicationStage, Long> applicationStageCounts() {
        ApplicationSearch noFilter = new ApplicationSearch(null, null, null, null, null);
        ApplicationListResponse result = applicationPipeline.list(noFilter, PageRequest.of(0, 1));
        return result.stageCounts();
    }

    private List<DashboardMetricResponse> kpis(int userId, Integer departmentId, LocalDateTime now,
            Map<ApplicationStage, Long> stageCounts) {
        long pending = metrics.ownPendingRequisitions(userId);
        // Rule: chờ lâu = đã chờ từ WAIT_DANGER_DAYS ngày lịch trở lên, khớp cách DashboardLabels.waitDays đếm.
        // HM không tự xử lý được (đang chờ Giám đốc) nên chỉ báo trên KPI, không đưa vào "Công việc cần xử lý".
        LocalDateTime staleBefore = now.toLocalDate()
                .minusDays(DashboardLabels.WAIT_DANGER_DAYS - 1L).atStartOfDay();
        long stalePending = metrics.ownStalePendingRequisitions(userId, staleBefore);
        String pendingDetail = "Yêu cầu của bạn đang chờ quyết định";
        String pendingTone = "neutral";
        if (stalePending > 0) {
            pendingDetail = stalePending + " yêu cầu chờ quá " + DashboardLabels.WAIT_DANGER_DAYS + " ngày";
            pendingTone = "danger";
        } else if (pending > 0) {
            pendingTone = "warning";
        }
        // Rule: chỉ đếm lịch trong 7 ngày tới (trước đây đếm mọi lịch tương lai).
        long interviews = metrics.interviewsForDepartmentBetween(departmentId, now,
                now.plusDays(INTERVIEW_WINDOW_DAYS));
        String interviewDetail = "Lịch của phòng ban trong 7 ngày tới";
        if (departmentId == null) {
            interviewDetail = "Tài khoản chưa được gán phòng ban";
        }
        return List.of(
                new DashboardMetricResponse("Yêu cầu của tôi", metrics.ownRequisitions(userId),
                        "Yêu cầu tuyển dụng do bạn phụ trách", "/requisitions", "neutral"),
                new DashboardMetricResponse("Chờ duyệt", pending, pendingDetail,
                        "/requisitions?status=" + RequisitionAccess.STATUS_PENDING_DIRECTOR, pendingTone),
                // Rule: đếm số hồ sơ (không đếm số người) để khớp số dòng của màn /applications.
                new DashboardMetricResponse("Hồ sơ ứng tuyển", total(stageCounts),
                        "Tất cả hồ sơ trong phạm vi phòng ban", "/applications", "neutral"),
                new DashboardMetricResponse("Phỏng vấn sắp tới", interviews, interviewDetail,
                        "/interviews", "neutral"));
    }

    private List<RequisitionItem> recentRequisitions(int userId) {
        List<RequisitionItem> items = new ArrayList<>();
        for (RequisitionRow row : lists.recentRequisitions(userId, RECENT_LIMIT)) {
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

    private List<DashboardInterviewItem> upcomingInterviews(Integer departmentId, LocalDateTime now) {
        List<InterviewRow> rows = lists.departmentInterviews(departmentId, now,
                now.plusDays(INTERVIEW_WINDOW_DAYS), RECENT_LIMIT);
        List<DashboardInterviewItem> items = new ArrayList<>();
        for (InterviewRow row : rows) {
            // Security: link họp trực tuyến không hiện trên dashboard, chỉ hiện địa điểm của buổi tại văn phòng.
            String location = null;
            if (InterviewFormat.Offline_Office.name().equals(row.interviewFormat())) {
                location = row.locationOrLink();
            }
            items.add(new DashboardInterviewItem(
                    row.startTime(),
                    row.endTime(),
                    row.candidateName(),
                    row.positionTitle(),
                    DashboardLabels.interviewFormat(row.interviewFormat()),
                    location,
                    (int) row.panelSize(),
                    DashboardLabels.interviewStatus(row.interviewStatus()),
                    DashboardLabels.interviewStatusTone(row.interviewStatus()),
                    "/interviews"));
        }
        return items;
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
                    "Chỉnh sửa", editHref(row.requisitionId()));
        }
        // Còn lại là bản nháp: badge trạng thái đã đủ nghĩa, không cần dòng giải thích.
        return new DashboardWorkItem(tag, tagTone, title, null, null, null,
                "Gửi duyệt", editHref(row.requisitionId()));
    }

    private List<DashboardChartResponse> charts(int userId, Integer departmentId,
            Map<ApplicationStage, Long> stageCounts) {
        // Rule (Q8): biểu đồ tính cả yêu cầu của phòng ban HM quản lý, khớp phạm vi màn danh sách yêu cầu.
        List<DashboardBarResponse> requisitionBars = bars(
                metrics.hiringManagerRequisitionStatuses(userId, departmentId), REQUISITION_STATUSES,
                RequisitionLabels::approvalStatus, "/requisitions?status=");
        long stopped = stageCounts.getOrDefault(ApplicationStage.CLOSED, 0L);
        return List.of(
                new DashboardChartResponse("Yêu cầu theo trạng thái",
                        "Yêu cầu do bạn tạo hoặc thuộc phòng ban bạn quản lý", requisitionBars),
                new DashboardChartResponse("Pipeline ứng viên",
                        "Hồ sơ theo giai đoạn hiện tại · Đã dừng: " + stopped + " hồ sơ", pipelineBars(stageCounts)));
    }

    /** Mỗi trạng thái trong danh sách là một thanh (kể cả số 0), theo đúng thứ tự của luồng. */
    private List<DashboardBarResponse> bars(List<StatusCount> rows, List<String> statuses,
            Function<String, String> label, String hrefPrefix) {
        Map<String, Long> counts = new HashMap<>();
        for (StatusCount row : rows) {
            counts.put(row.status(), row.count());
        }
        long max = 0;
        for (String status : statuses) {
            max = Math.max(max, counts.getOrDefault(status, 0L));
        }
        List<DashboardBarResponse> bars = new ArrayList<>();
        for (String status : statuses) {
            bars.add(new DashboardBarResponse(label.apply(status), counts.getOrDefault(status, 0L), max,
                    hrefPrefix + status));
        }
        return bars;
    }

    /** 5 giai đoạn theo luồng (Ứng tuyển → Nhận việc); "Đã dừng" không vẽ thanh mà ghi ở dòng phạm vi. */
    private List<DashboardBarResponse> pipelineBars(Map<ApplicationStage, Long> stageCounts) {
        long max = 0;
        for (ApplicationStage stage : ApplicationStage.values()) {
            if (stage != ApplicationStage.CLOSED) {
                max = Math.max(max, stageCounts.getOrDefault(stage, 0L));
            }
        }
        List<DashboardBarResponse> bars = new ArrayList<>();
        for (ApplicationStage stage : ApplicationStage.values()) {
            if (stage != ApplicationStage.CLOSED) {
                bars.add(new DashboardBarResponse(stage.label(), stageCounts.getOrDefault(stage, 0L), max,
                        "/applications?stage=" + stage.name()));
            }
        }
        return bars;
    }

    /** Tổng mọi giai đoạn (kể cả Đã dừng) = tab "Tất cả" của màn /applications. */
    private long total(Map<ApplicationStage, Long> stageCounts) {
        long total = 0;
        for (Long count : stageCounts.values()) {
            total += count;
        }
        return total;
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
