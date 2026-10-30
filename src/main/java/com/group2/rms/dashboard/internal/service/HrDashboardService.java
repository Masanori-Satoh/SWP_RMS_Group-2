package com.group2.rms.dashboard.internal.service;

import com.group2.rms.candidate.application.pipeline.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationSearch;
import com.group2.rms.candidate.application.pipeline.service.ApplicationPipelineService;
import com.group2.rms.candidate.domain.rule.ApplicationStage;
import com.group2.rms.dashboard.internal.dto.DashboardBarResponse;
import com.group2.rms.dashboard.internal.dto.DashboardColumnResponse;
import com.group2.rms.dashboard.internal.dto.DashboardInterviewItem;
import com.group2.rms.dashboard.internal.dto.DashboardMetricResponse;
import com.group2.rms.dashboard.internal.dto.DashboardWorkItem;
import com.group2.rms.dashboard.internal.dto.HrDashboardResponse;
import com.group2.rms.dashboard.internal.dto.OpenPostingItem;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.AwaitingPostingRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.OpenPostingRow;
import com.group2.rms.dashboard.internal.repository.InternalMetricsRepository;
import com.group2.rms.jobposting.service.JobPostingLabels;
import com.group2.rms.requisition.service.RequisitionLabels;
import com.group2.rms.user.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/*
 * SERVICE: dựng dữ liệu dashboard HR (toàn công ty): KPI, việc cần xử lý, phỏng vấn sắp diễn ra,
 * hồ sơ đang chờ theo giai đoạn, xu hướng hồ sơ theo tuần, tin đang mở.
 */
@Service
public class HrDashboardService {

    /** Số dòng tối đa của mỗi danh sách trên dashboard. */
    static final int LIST_LIMIT = 5;
    /** Phỏng vấn sắp diễn ra: từ bây giờ đến hết ngày mai. */
    static final int INTERVIEW_WINDOW_DAYS = 2;
    /** Biểu đồ xu hướng: 8 tuần gần nhất (kể cả tuần này). */
    static final int TREND_WEEKS = 8;
    /** Biểu đồ cột vẽ theo bậc 0..10 (xem DashboardColumnResponse). */
    static final int COLUMN_LEVELS = 10;
    /** Người đang cầm bóng của bước hồ sơ (ApplicationProgressStep.owner) khi HR phải xử lý. */
    static final String HR_OWNER = "HR";
    /** Các giai đoạn hồ sơ còn đang chờ xử lý (D14), theo đúng thứ tự tab của /applications. */
    static final List<ApplicationStage> WAITING_STAGES = List.of(
            ApplicationStage.APPLIED, ApplicationStage.SCREENING, ApplicationStage.INTERVIEW, ApplicationStage.OFFER);

    private static final DateTimeFormatter WEEK_LABEL = DateTimeFormatter.ofPattern("dd/MM");

    private final InternalMetricsRepository metrics;
    private final DashboardListRepository lists;
    private final ApplicationPipelineService applicationPipeline;
    private final Clock clock;

    public HrDashboardService(InternalMetricsRepository metrics, DashboardListRepository lists,
            ApplicationPipelineService applicationPipeline, Clock clock) {
        this.metrics = metrics;
        this.lists = lists;
        this.applicationPipeline = applicationPipeline;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public HrDashboardResponse build(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        Map<ApplicationStage, List<ApplicationPipelineResponse>> stageRows = applicationsByStage();
        Map<Integer, LocalDateTime> waitingSince = metrics.applicationWaitingSince();

        List<DashboardWorkItem> allWork = workQueue(stageRows, waitingSince, now);
        List<DashboardWorkItem> shownWork = DashboardViews.firstItems(allWork, LIST_LIMIT);
        List<OpenPostingRow> postingRows = lists.openPostings(now);

        List<LocalDateTime> applicationDates = metrics.applicationDates(trendFrom(now), now);

        return new HrDashboardResponse(
                user.getRole().getRoleName(),
                user.getFullName(),
                kpis(now, stageRows),
                shownWork,
                allWork.size() - shownWork.size(),
                upcomingInterviews(now),
                waitingStageBars(stageRows, waitingSince, now),
                weeklyColumns(applicationDates, now),
                trendSummary(applicationDates, now),
                openPostings(postingRows, now),
                postingRows.size());
    }

    /**
     * Hồ sơ của từng giai đoạn, lấy từ đúng màn /applications (cùng ApplicationProgressResolver) để số trên
     * dashboard khớp số trên các tab và mỗi hồ sơ có sẵn nhãn trạng thái chi tiết + người đang cầm bóng.
     */
    private Map<ApplicationStage, List<ApplicationPipelineResponse>> applicationsByStage() {
        Map<ApplicationStage, List<ApplicationPipelineResponse>> result = new EnumMap<>(ApplicationStage.class);
        for (ApplicationStage stage : WAITING_STAGES) {
            ApplicationSearch onlyStage = new ApplicationSearch(null, null, null, null, stage.name());
            // Pipeline đã tính tất cả hồ sơ trong phạm vi trước khi cắt trang; lấy toàn bộ kết quả để KPI không
            // lệch khỏi các tab /applications khi một giai đoạn vượt 10.000 hồ sơ.
            result.put(stage, applicationPipeline.list(onlyStage, PageRequest.of(0, Integer.MAX_VALUE))
                    .applications().getContent());
        }
        return result;
    }

    private List<DashboardMetricResponse> kpis(LocalDateTime now,
            Map<ApplicationStage, List<ApplicationPipelineResponse>> stageRows) {
        LocalDate today = now.toLocalDate();
        LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        long unscreened = stageRows.get(ApplicationStage.APPLIED).size();
        long postedToday = metrics.publishedPostingsBetween(today.atStartOfDay(), now);
        long acceptedThisWeek = metrics.offersWithStatusBetween(InternalMetricsRepository.OFFER_ACCEPTED,
                weekStart, now);

        return List.of(
                DashboardMetricResponse.backlog("fileSearch", "Hồ sơ chờ sàng lọc", unscreened,
                        "Chưa được sàng lọc", "/applications?stage=" + ApplicationStage.APPLIED.name(), "neutral"),
                DashboardMetricResponse.backlog("megaphone", "Tin mới đăng trong ngày", postedToday,
                        "Đã đăng hôm nay", "/internal/job-postings?status=Published&postedFrom=" + today
                                + "&postedTo=" + today, "neutral"),
                DashboardMetricResponse.backlog("circleCheck", "Offer được chấp nhận", acceptedThisWeek,
                        "Trong tuần này", "/offers?status=Accepted", "neutral"));
    }

    /** Việc HR phải làm: hồ sơ HR đang cầm bóng + yêu cầu đã duyệt chưa có tin. Chờ lâu nhất lên đầu. */
    private List<DashboardWorkItem> workQueue(Map<ApplicationStage, List<ApplicationPipelineResponse>> stageRows,
            Map<Integer, LocalDateTime> waitingSince, LocalDateTime now) {
        List<WaitingWork> waiting = new ArrayList<>();
        for (ApplicationStage stage : WAITING_STAGES) {
            for (ApplicationPipelineResponse row : stageRows.get(stage)) {
                if (HR_OWNER.equals(row.ownerLabel())) {
                    long days = DashboardLabels.waitDays(waitingSince.get(row.applicationId()), now);
                    waiting.add(new WaitingWork(days, applicationWork(stage, row, days)));
                }
            }
        }
        for (AwaitingPostingRow row : lists.requisitionsAwaitingPosting()) {
            long days = DashboardLabels.waitDays(row.waitingSince(), now);
            waiting.add(new WaitingWork(days, postingWork(row, days)));
        }
        waiting.sort(Comparator.comparingLong(WaitingWork::days).reversed());

        List<DashboardWorkItem> items = new ArrayList<>();
        for (WaitingWork work : waiting) {
            items.add(work.item());
        }
        return items;
    }

    private DashboardWorkItem applicationWork(ApplicationStage stage, ApplicationPipelineResponse row, long days) {
        String title = DashboardViews.candidateName(row.fullName()) + " · " + row.postingTitle();
        return new DashboardWorkItem(stage.label(), "neutral", title, row.statusLabel(),
                DashboardLabels.waitText(days), DashboardLabels.waitTone(days), "Xử lý",
                "/applications/" + row.applicationId(), false);
    }

    private DashboardWorkItem postingWork(AwaitingPostingRow row, long days) {
        String code = row.requisitionCode();
        if (code == null || code.isBlank()) {
            code = RequisitionLabels.requisitionCode(row.requisitionId());
        }
        return new DashboardWorkItem("Yêu cầu", "success", code + " · " + row.title(),
                "Đã duyệt, chưa có tin tuyển dụng", DashboardLabels.waitText(days), DashboardLabels.waitTone(days),
                "Tạo tin", "/internal/job-postings/create?requisitionId=" + row.requisitionId(), true);
    }

    private List<DashboardInterviewItem> upcomingInterviews(LocalDateTime now) {
        LocalDateTime endOfTomorrow = now.toLocalDate().plusDays(INTERVIEW_WINDOW_DAYS).atStartOfDay();
        return DashboardViews.interviewItems(lists.allInterviews(now, endOfTomorrow, LIST_LIMIT), now);
    }

    /** Mỗi giai đoạn một thanh (kể cả 0); chữ phụ đỏ = số hồ sơ đã chờ từ 3 ngày. */
    private List<DashboardBarResponse> waitingStageBars(Map<ApplicationStage, List<ApplicationPipelineResponse>> stageRows,
            Map<Integer, LocalDateTime> waitingSince, LocalDateTime now) {
        long max = 0;
        for (ApplicationStage stage : WAITING_STAGES) {
            max = Math.max(max, stageRows.get(stage).size());
        }
        List<DashboardBarResponse> bars = new ArrayList<>();
        for (ApplicationStage stage : WAITING_STAGES) {
            List<ApplicationPipelineResponse> rows = stageRows.get(stage);
            long overdue = 0;
            for (ApplicationPipelineResponse row : rows) {
                long days = DashboardLabels.waitDays(waitingSince.get(row.applicationId()), now);
                if (days >= DashboardLabels.WAIT_WARNING_DAYS) {
                    overdue++;
                }
            }
            // Rule: chỉ phần chờ lâu được tô màu cảnh báo, không tô cả giai đoạn chỉ vì số lượng lớn.
            String note = null;
            if (overdue > 0) {
                note = overdue + " chờ quá " + DashboardLabels.WAIT_WARNING_DAYS + " ngày";
            }
            String hint = rows.size() + " hồ sơ ở " + stage.label() + " · " + overdue + " chờ quá "
                    + DashboardLabels.WAIT_WARNING_DAYS + " ngày · toàn công ty";
            bars.add(new DashboardBarResponse(stage.label(), rows.size(), Math.max(max, 1),
                    "/applications?stage=" + stage.name(), note, "warning", hint));
        }
        return bars;
    }

    /** Thứ Hai đầu tiên của 8 tuần trong biểu đồ xu hướng. */
    private LocalDateTime trendFrom(LocalDateTime now) {
        LocalDate thisMonday = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return thisMonday.minusWeeks(TREND_WEEKS - 1L).atStartOfDay();
    }

    /** Số hồ sơ mới mỗi tuần, cột cuối là tuần này (tô đậm). */
    private List<DashboardColumnResponse> weeklyColumns(List<LocalDateTime> applicationDates, LocalDateTime now) {
        LocalDate firstMonday = trendFrom(now).toLocalDate();
        long[] counts = new long[TREND_WEEKS];
        for (LocalDateTime submittedAt : applicationDates) {
            int week = (int) (ChronoUnit.DAYS.between(firstMonday, submittedAt.toLocalDate()) / 7);
            if (week >= 0 && week < TREND_WEEKS) {
                counts[week]++;
            }
        }
        long max = 0;
        for (long count : counts) {
            max = Math.max(max, count);
        }
        List<DashboardColumnResponse> columns = new ArrayList<>();
        for (int week = 0; week < TREND_WEEKS; week++) {
            int level = 0;
            if (max > 0) {
                level = (int) Math.round(counts[week] * (double) COLUMN_LEVELS / max);
            }
            LocalDate weekStart = firstMonday.plusWeeks(week);
            String label = weekStart.format(WEEK_LABEL);
            String hint = "Tuần " + label + " – " + weekStart.plusDays(6).format(WEEK_LABEL) + ": " + counts[week]
                    + " hồ sơ mới · toàn công ty";
            columns.add(new DashboardColumnResponse(label, counts[week], level, week == TREND_WEEKS - 1, hint));
        }
        return columns;
    }

    /** "4 tuần gần nhất: 312 hồ sơ · ↑ 16%" (so với 4 tuần trước đó trong cùng biểu đồ). */
    private String trendSummary(List<LocalDateTime> applicationDates, LocalDateTime now) {
        LocalDateTime half = trendFrom(now).plusWeeks(TREND_WEEKS / 2);
        long recent = 0;
        long earlier = 0;
        for (LocalDateTime submittedAt : applicationDates) {
            if (submittedAt.isBefore(half)) {
                earlier++;
            } else {
                recent++;
            }
        }
        String summary = (TREND_WEEKS / 2) + " tuần gần nhất: " + recent + " hồ sơ";
        String change = DashboardPeriod.changeText(recent, earlier);
        if (change != null) {
            summary = summary + " · " + change;
        }
        return summary;
    }

    /** Tin đang mở, ít hồ sơ trên mỗi vị trí cần tuyển nhất lên đầu (HR cần đẩy tin này). */
    private List<OpenPostingItem> openPostings(List<OpenPostingRow> rows, LocalDateTime now) {
        List<OpenPostingRow> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparingDouble(row -> row.applications() / (double) Math.max(row.numberOfPositions(), 1)));
        List<OpenPostingRow> shown = DashboardViews.firstItems(sorted, LIST_LIMIT);

        long max = 1;
        for (OpenPostingRow row : shown) {
            max = Math.max(max, row.applications());
        }
        List<OpenPostingItem> items = new ArrayList<>();
        for (OpenPostingRow row : shown) {
            items.add(openPostingItem(row, max, now));
        }
        return items;
    }

    private OpenPostingItem openPostingItem(OpenPostingRow row, long max, LocalDateTime now) {
        // Rule: tin còn ≤ 7 ngày (JobPostingLabels.DEADLINE_SOON_DAYS) hiện "Còn N ngày" thay cho ngày tháng.
        String deadlineText = null;
        String deadlineTone = null;
        if (row.applicationDeadline() != null) {
            long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), row.applicationDeadline().toLocalDate());
            if (daysLeft <= 0) {
                deadlineText = "Hết hạn hôm nay";
                deadlineTone = "danger";
            } else if (daysLeft <= JobPostingLabels.DEADLINE_SOON_DAYS) {
                deadlineText = "Còn " + daysLeft + " ngày";
                deadlineTone = "warning";
            }
        }
        String department = row.departmentName();
        if (department == null) {
            department = "Chưa xác định";
        }
        return new OpenPostingItem(row.postingTitle(), department, row.numberOfPositions(), row.applications(), max,
                row.applicationDeadline(), deadlineText, deadlineTone,
                JobPostingLabels.statusLabel(row.postingStatus()), JobPostingLabels.statusTone(row.postingStatus()),
                "/internal/job-postings/" + row.jobPostingId());
    }

    /** Một việc kèm số ngày đã chờ, chỉ dùng để sắp xếp. */
    private record WaitingWork(long days, DashboardWorkItem item) {
    }
}
