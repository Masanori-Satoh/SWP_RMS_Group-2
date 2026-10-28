package com.group2.rms.dashboard.internal.service;

import com.group2.rms.dashboard.internal.dto.DashboardInterviewItem;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * SERVICE: dựng dữ liệu dashboard Interviewer (chỉ các buổi có tên mình trong hội đồng): KPI, buổi tiếp theo,
 * phiếu đánh giá cần gửi, lịch theo tuần. Không có biểu đồ.
 */
@Service
public class InterviewerDashboardService {

    /** "Sắp tới" và "buổi tiếp theo" tính trong 7 ngày tới. */
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

        InterviewRow next = nextInterview(soon, now);
        DashboardInterviewItem nextItem = null;
        long minutesUntilNext = 0;
        if (next != null) {
            nextItem = DashboardViews.interviewItem(next, now);
            minutesUntilNext = Math.max(Duration.between(now, next.startTime()).toMinutes(), 0);
        }

        return new InterviewerDashboardResponse(
                user.getRole().getRoleName(),
                user.getFullName(),
                kpis(userId, soon, evaluationRows, now),
                nextItem,
                minutesUntilNext,
                evaluations(evaluationRows, now),
                week(weekRows, weekStart, today, now),
                weekStart,
                weekRows.size());
    }

    private List<DashboardMetricResponse> kpis(int userId, List<InterviewRow> soon, List<EvaluationRow> evaluationRows,
            LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        long todayTotal = 0;
        long todayDone = 0;
        long upcoming = 0;
        for (InterviewRow row : soon) {
            if (row.startTime().toLocalDate().equals(today)) {
                todayTotal++;
                if (isFinished(row, now)) {
                    todayDone++;
                }
            }
            if (!row.startTime().isBefore(now) && isActive(row)) {
                upcoming++;
            }
        }

        long overdueEvaluations = 0;
        for (EvaluationRow row : evaluationRows) {
            if (DashboardLabels.waitDays(row.endTime(), now) >= DashboardLabels.WAIT_WARNING_DAYS) {
                overdueEvaluations++;
            }
        }
        String evaluationDetail = "Không có phiếu quá hạn";
        String evaluationTone = "neutral";
        if (overdueEvaluations > 0) {
            evaluationDetail = overdueEvaluations + " phiếu quá " + DashboardLabels.WAIT_WARNING_DAYS + " ngày";
            evaluationTone = "danger";
        } else if (!evaluationRows.isEmpty()) {
            evaluationTone = "warning";
        }

        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        long sentThisMonth = metrics.evaluationsSubmittedSince(userId, monthStart);
        long sentTotal = metrics.evaluationsSubmittedSince(userId, null);

        return List.of(
                DashboardMetricResponse.backlog("calendarCheck", "Phỏng vấn hôm nay", todayTotal,
                        todayDone + " đã xong, " + (todayTotal - todayDone) + " còn lại", DashboardViews.INTERVIEWS_HREF,
                        "neutral"),
                DashboardMetricResponse.backlog("clipboardCheck", "Chờ đánh giá", evaluationRows.size(), evaluationDetail,
                        DashboardViews.INTERVIEWS_HREF, evaluationTone),
                DashboardMetricResponse.backlog("calendar", "Sắp tới", upcoming, "Trong 7 ngày tới",
                        DashboardViews.INTERVIEWS_HREF, "neutral"),
                DashboardMetricResponse.backlog("send", "Đã gửi tháng này", sentThisMonth,
                        "Tổng: " + sentTotal + " phiếu", DashboardViews.INTERVIEWS_HREF, "neutral"));
    }

    /** Buổi còn hiệu lực sớm nhất chưa kết thúc (đang diễn ra cũng tính); không có thì null. */
    private InterviewRow nextInterview(List<InterviewRow> rows, LocalDateTime now) {
        for (InterviewRow row : rows) {
            if (isActive(row) && !isFinished(row, now)) {
                return row;
            }
        }
        return null;
    }

    private List<EvaluationItem> evaluations(List<EvaluationRow> rows, LocalDateTime now) {
        List<EvaluationItem> items = new ArrayList<>();
        for (EvaluationRow row : rows) {
            long days = DashboardLabels.waitDays(row.endTime(), now);
            items.add(new EvaluationItem(DashboardViews.candidateName(row.candidateName()), row.positionTitle(),
                    row.endTime(), DashboardLabels.waitText(days), DashboardLabels.waitTone(days),
                    DashboardViews.INTERVIEWS_HREF));
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

    private boolean isFinished(InterviewRow row, LocalDateTime now) {
        return InterviewStatus.Completed.name().equals(row.interviewStatus())
                || (row.endTime() != null && row.endTime().isBefore(now));
    }
}
