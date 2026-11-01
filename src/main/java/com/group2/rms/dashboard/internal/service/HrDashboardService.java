package com.group2.rms.dashboard.internal.service;

import com.group2.rms.candidate.application.pipeline.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationSearch;
import com.group2.rms.candidate.application.pipeline.service.ApplicationPipelineService;
import com.group2.rms.candidate.domain.rule.ApplicationStage;
import com.group2.rms.dashboard.internal.dto.DashboardInterviewItem;
import com.group2.rms.dashboard.internal.dto.DashboardMetricResponse;
import com.group2.rms.dashboard.internal.dto.DashboardWorkItem;
import com.group2.rms.dashboard.internal.dto.HrDashboardResponse;
import com.group2.rms.dashboard.internal.dto.WeeklyApplicationItem;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.AwaitingPostingRow;
import com.group2.rms.dashboard.internal.repository.InternalMetricsRepository;
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
 * xu hướng hồ sơ ứng tuyển theo tuần (biểu đồ đường).
 */
@Service
public class HrDashboardService {

    /** Số dòng tối đa của mỗi danh sách trên dashboard. */
    static final int LIST_LIMIT = 5;
    /** Phỏng vấn sắp diễn ra: từ bây giờ đến hết ngày mai. */
    static final int INTERVIEW_WINDOW_DAYS = 2;
    /** Biểu đồ xu hướng: 8 tuần gần nhất (kể cả tuần này). */
    static final int TREND_WEEKS = 8;
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

        List<LocalDateTime> applicationDates = metrics.applicationDates(trendFrom(now), now);

        return new HrDashboardResponse(
                user.getRole().getRoleName(),
                user.getFullName(),
                kpis(now, stageRows),
                shownWork,
                allWork.size() - shownWork.size(),
                upcomingInterviews(now),
                weeklyApplications(applicationDates, now));
    }

    /** Hồ sơ từng giai đoạn lấy từ màn /applications để số trên dashboard khớp các tab. */
    private Map<ApplicationStage, List<ApplicationPipelineResponse>> applicationsByStage() {
        Map<ApplicationStage, List<ApplicationPipelineResponse>> result = new EnumMap<>(ApplicationStage.class);
        for (ApplicationStage stage : WAITING_STAGES) {
            // dunglt bổ sung: bỏ tham số sort (danh sách hồ sơ không còn xếp theo điểm AI)
            ApplicationSearch onlyStage = new ApplicationSearch(null, null, null, stage.name());
            // Lấy toàn bộ kết quả để KPI không lệch khỏi tab /applications khi giai đoạn rất đông
            result.put(stage, applicationPipeline.list(onlyStage, PageRequest.of(0, Integer.MAX_VALUE))
                    .applications().getContent());
        }
        return result;
    }

    private List<DashboardMetricResponse> kpis(LocalDateTime now,
            Map<ApplicationStage, List<ApplicationPipelineResponse>> stageRows) {
        LocalDate today = now.toLocalDate();
        long unscreened = stageRows.get(ApplicationStage.APPLIED).size();
        long postedToday = metrics.countPublishedPostingsBetween(today.atStartOfDay(), now);
        long awaitingSchedule = countHrOwned(stageRows.get(ApplicationStage.INTERVIEW));

        return List.of(
                DashboardMetricResponse.backlog("fileSearch", "Hồ sơ chờ sàng lọc", unscreened,
                        "Chưa được sàng lọc", "/applications?stage=" + ApplicationStage.APPLIED.name(), "neutral"),
                DashboardMetricResponse.backlog("megaphone", "Tin mới đăng trong ngày", postedToday,
                        "Đã đăng hôm nay", "/internal/job-postings?status=Published&postedFrom=" + today
                                + "&postedTo=" + today, "neutral"),
                DashboardMetricResponse.backlog("calendar", "Hồ sơ chờ lên lịch phỏng vấn", awaitingSchedule,
                        "Chưa xếp lịch", "/applications?stage=" + ApplicationStage.INTERVIEW.name(), "neutral"));
    }

    /** Ở giai đoạn phỏng vấn, HR chỉ cầm bóng khi hồ sơ đang chờ xếp lịch (AWAITING_SCHEDULE). */
    private long countHrOwned(List<ApplicationPipelineResponse> rows) {
        return rows.stream().filter(row -> HR_OWNER.equals(row.ownerLabel())).count();
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
        String code = RequisitionLabels.requisitionCode(row.requisitionId(), row.requisitionCode());
        return new DashboardWorkItem("Yêu cầu", "success", code + " · " + row.title(),
                "Đã duyệt, chưa có tin tuyển dụng", DashboardLabels.waitText(days), DashboardLabels.waitTone(days),
                "Tạo tin", "/internal/job-postings/create?requisitionId=" + row.requisitionId(), true);
    }

    private List<DashboardInterviewItem> upcomingInterviews(LocalDateTime now) {
        LocalDateTime endOfTomorrow = now.toLocalDate().plusDays(INTERVIEW_WINDOW_DAYS).atStartOfDay();
        return DashboardViews.interviewItems(lists.allInterviews(now, endOfTomorrow, LIST_LIMIT), now);
    }

    /** Thứ Hai đầu tiên của 8 tuần trong biểu đồ xu hướng. */
    private LocalDateTime trendFrom(LocalDateTime now) {
        LocalDate thisMonday = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return thisMonday.minusWeeks(TREND_WEEKS - 1L).atStartOfDay();
    }

    /** Số hồ sơ mới mỗi tuần (điểm cuối là tuần này) cho biểu đồ đường. */
    private List<WeeklyApplicationItem> weeklyApplications(List<LocalDateTime> applicationDates, LocalDateTime now) {
        LocalDate firstMonday = trendFrom(now).toLocalDate();
        long[] counts = new long[TREND_WEEKS];
        for (LocalDateTime submittedAt : applicationDates) {
            int week = (int) (ChronoUnit.DAYS.between(firstMonday, submittedAt.toLocalDate()) / 7);
            if (week >= 0 && week < TREND_WEEKS) {
                counts[week]++;
            }
        }
        List<WeeklyApplicationItem> items = new ArrayList<>();
        for (int week = 0; week < TREND_WEEKS; week++) {
            LocalDate weekStart = firstMonday.plusWeeks(week);
            String label = weekStart.format(WEEK_LABEL);
            String range = "Tuần " + label + " – " + weekStart.plusDays(6).format(WEEK_LABEL);
            items.add(new WeeklyApplicationItem(label, range, counts[week]));
        }
        return items;
    }

    /** Một việc kèm số ngày đã chờ, chỉ dùng để sắp xếp. */
    private record WaitingWork(long days, DashboardWorkItem item) {
    }
}
