package com.group2.rms.dashboard.internal.service;

import com.group2.rms.dashboard.internal.dto.DashboardMetricResponse;
import com.group2.rms.dashboard.internal.dto.EvaluationItem;
import com.group2.rms.dashboard.internal.dto.InterviewerDashboardResponse;
import com.group2.rms.dashboard.internal.dto.WeekDayItem;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.EvaluationRow;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.InterviewRow;
import com.group2.rms.dashboard.internal.repository.InternalMetricsRepository;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * SERVICE: dựng dữ liệu dashboard Interviewer (chỉ các buổi có tên mình trong hội đồng): KPI,
 * phiếu đánh giá cần gửi, lịch phỏng vấn theo tuần. Không có biểu đồ.
 */
@Service
public class InterviewerDashboardService {

    /** KPI "Buổi phỏng vấn sắp tới" tính trong 7 ngày tới. */
    static final int UPCOMING_DAYS = 7;
    static final int DAYS_PER_WEEK = 7;

    private final InternalMetricsRepository metrics;
    private final DashboardListRepository lists;
    private final Clock clock;

    public InterviewerDashboardService(InternalMetricsRepository metrics, DashboardListRepository lists, Clock clock) {
        this.metrics = metrics;
        this.lists = lists;
        this.clock = clock;
    }

    /** requestedWeek: tham số ?week= trên URL (ngày bất kỳ trong tuần, định dạng yyyy-MM-dd); lạ / trống = tuần này. */
    @Transactional(readOnly = true)
    public InterviewerDashboardResponse build(User user, String requestedWeek) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        int userId = user.getUserId();

        // Từ đầu hôm nay để đếm cả buổi đã xong trong ngày; hết 7 ngày tới cho "Sắp tới".
        List<InterviewRow> soon = lists.panelInterviews(userId, today.atStartOfDay(), now.plusDays(UPCOMING_DAYS));
        List<EvaluationRow> evaluationRows = lists.pendingEvaluations(userId);

        LocalDate weekStart = DashboardPeriod.weekStart(requestedWeek, today);
        List<InterviewRow> weekRows = lists.panelInterviews(userId, weekStart.atStartOfDay(),
                weekStart.plusDays(DAYS_PER_WEEK).atStartOfDay());

        return new InterviewerDashboardResponse(
                user.getRole().getRoleName(),
                user.getFullName(),
                kpis(userId, soon, evaluationRows, now),
                evaluations(evaluationRows),
                week(weekRows, weekStart, today, now),
                weekStart);
    }

    private List<DashboardMetricResponse> kpis(int userId, List<InterviewRow> soon, List<EvaluationRow> evaluationRows,
            LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        long upcoming = 0;
        for (InterviewRow row : soon) {
            if (!row.startTime().isBefore(now) && isActive(row)) {
                upcoming++;
            }
        }

        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        long passedThisMonth = metrics.countPassedInterviewCandidatesBetween(userId, monthStart, now);

        return List.of(
                DashboardMetricResponse.backlog("calendarDays", "Buổi phỏng vấn sắp tới", upcoming, "Trong 7 ngày tới",
                        DashboardViews.INTERVIEWS_HREF, "neutral"),
                DashboardMetricResponse.backlog("clipboardCheck", "Đánh giá chưa hoàn thành", evaluationRows.size(),
                        "Đang chờ đánh giá", DashboardViews.INTERVIEWS_HREF, "neutral"),
                DashboardMetricResponse.backlog("userCheck", "Ứng viên đạt phỏng vấn", passedThisMonth,
                        "Trong tháng này", DashboardViews.INTERVIEWS_HREF, "neutral"));
    }

    private List<EvaluationItem> evaluations(List<EvaluationRow> rows) {
        List<EvaluationItem> items = new ArrayList<>();
        for (EvaluationRow row : rows) {
            items.add(new EvaluationItem(DashboardViews.candidateName(row.candidateName()), row.positionTitle(),
                    row.endTime(), DashboardViews.INTERVIEWS_HREF));
        }
        return items;
    }

    /** 7 cột từ thứ Hai tới Chủ nhật; mỗi cột là các buổi bắt đầu trong ngày đó, đã xếp theo giờ. */
    private List<WeekDayItem> week(List<InterviewRow> rows, LocalDate weekStart, LocalDate today, LocalDateTime now) {
        List<WeekDayItem> days = new ArrayList<>();
        for (int offset = 0; offset < DAYS_PER_WEEK; offset++) {
            LocalDate day = weekStart.plusDays(offset);
            List<InterviewRow> dayRows = new ArrayList<>();
            for (InterviewRow row : rows) {
                if (row.startTime().toLocalDate().equals(day)) {
                    dayRows.add(row);
                }
            }
            days.add(new WeekDayItem(DashboardLabels.weekday(day), day, day.equals(today),
                    DashboardViews.interviewItems(dayRows, now)));
        }
        return days;
    }

    private boolean isActive(InterviewRow row) {
        return InterviewStatus.Scheduled.name().equals(row.interviewStatus())
                || InterviewStatus.Rescheduled.name().equals(row.interviewStatus());
    }
}
