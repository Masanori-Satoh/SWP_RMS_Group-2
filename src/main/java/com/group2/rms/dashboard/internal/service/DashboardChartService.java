package com.group2.rms.dashboard;

import com.group2.rms.candidate.domain.rule.ApplicationStatusLabels;
import com.group2.rms.dashboard.DashboardMetricsRepository.DecisionRow;
import com.group2.rms.dashboard.DashboardMetricsRepository.StatusCount;
import com.group2.rms.dashboard.dto.RecruitmentChartResponse;
import com.group2.rms.dashboard.dto.RecruitmentChartResponse.Point;
import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds role-scoped chart series while leaving ordinary links and values in the rendered page. */
// NOTE: chỉ còn HR, Director, Interviewer; Hiring Manager đã chuyển sang HiringManagerDashboardService.
@Service
public class DashboardChartService {
    private static final List<String> APPLICATION_STATUSES = List.of(
            "Applied", "AI_Screened", "HR_Passed", "HM_Passed", "Interviewing", "Offered", "Hired", "Rejected");
    private static final List<String> POSTING_STATUSES = List.of(
            JobPostingAccess.STATUS_DRAFT, JobPostingAccess.STATUS_PUBLISHED,
            JobPostingAccess.STATUS_SCHEDULED, JobPostingAccess.STATUS_PAUSED, JobPostingAccess.STATUS_CLOSED);
    private static final List<String> INTERVIEW_STATUSES = List.of(
            InterviewStatus.Scheduled.name(), InterviewStatus.Rescheduled.name(),
            InterviewStatus.Completed.name(), InterviewStatus.Cancelled.name());
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");

    private final UserRepository users;
    private final DashboardMetricsRepository metrics;
    private final Clock clock;

    public DashboardChartService(UserRepository users, DashboardMetricsRepository metrics, Clock clock) {
        this.users = users;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RecruitmentChartResponse> forUsername(String username, String roleName) {
        User user = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        if (!"Active".equals(user.getAccountStatus()) || user.getRole() == null
                || !roleName.equals(user.getRole().getRoleName())) {
            throw new AccessDeniedException("Dashboard role is no longer active");
        }
        return switch (roleName) {
            case "HR" -> hr();
            case "Director" -> director(user);
            case "Interviewer" -> interviewer(user);
            default -> List.of();
        };
    }

    private List<RecruitmentChartResponse> hr() {
        LocalDate today = LocalDate.now(clock);
        List<Point> applications = statusPoints(metrics.formalApplicationStatuses(), APPLICATION_STATUSES,
                status -> "/applications?status=" + status, ApplicationStatusLabels::label);
        List<Point> postings = statusPoints(metrics.jobPostingViewStatuses(LocalDateTime.now(clock)), POSTING_STATUSES,
                status -> "/internal/job-postings?status=" + status, this::postingLabel);
        List<Point> received = dailyPoints(today.minusDays(13), 14,
                metrics.recentApplicationDates(today.minusDays(13).atStartOfDay(), today.plusDays(1).atStartOfDay()),
                null);
        return List.of(
                new RecruitmentChartResponse("Hồ sơ theo trạng thái", "Số hồ sơ ứng tuyển chính thức trên toàn hệ thống.",
                        "bar", applications),
                new RecruitmentChartResponse("Hồ sơ nhận trong 14 ngày", "Đếm theo ngày nộp hồ sơ, bao gồm hôm nay.",
                        "line", received),
                new RecruitmentChartResponse("Tin tuyển dụng theo trạng thái",
                        "Trạng thái hiển thị của tin, gồm cả tin hẹn đăng; số tin đang nhận hồ sơ ở thẻ tổng quan.",
                        "bar", postings));
    }

    private List<RecruitmentChartResponse> director(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        List<Point> queue = List.of(
                new Point("Yêu cầu", metrics.requisitionsAwaitingDirector(),
                        "/requisitions?status=Pending_Director"),
                new Point("Đề nghị", metrics.offersAwaitingDirector(), "/offers?status=PENDING"));
        List<DecisionRow> decisions = metrics.directorDecisions(user.getUserId(),
                today.minusDays(13).atStartOfDay(), today.plusDays(1).atStartOfDay());
        long approved = decisions.stream().filter(decision -> "Approved".equals(decision.status())).count();
        long rejected = decisions.stream().filter(decision -> "Rejected".equals(decision.status())).count();
        List<Point> outcomes = List.of(new Point("Đã duyệt", approved, null),
                new Point("Đã từ chối", rejected, null));
        List<LocalDateTime> decisionDates = decisions.stream().map(DecisionRow::at).toList();
        return List.of(
                new RecruitmentChartResponse("Hàng đợi phê duyệt", "Toàn hệ thống; chọn loại để mở danh sách chờ duyệt.",
                        "bar", queue),
                new RecruitmentChartResponse("Kết quả quyết định của tôi",
                        "Quyết định của bạn trong 14 ngày qua, gồm yêu cầu và đề nghị tuyển dụng.",
                        "bar", outcomes),
                new RecruitmentChartResponse("Quyết định của tôi trong 14 ngày",
                        "Số lần duyệt hoặc từ chối do chính bạn thực hiện, theo ngày.",
                        "line", dailyPoints(today.minusDays(13), 14, decisionDates, null)));
    }

    private List<RecruitmentChartResponse> interviewer(User user) {
        int userId = user.getUserId();
        LocalDate today = LocalDate.now(clock);
        List<Point> statuses = statusPoints(metrics.assignedInterviewStatuses(userId), INTERVIEW_STATUSES,
                status -> "/interviews", this::interviewLabel);
        List<Point> upcoming = dailyPoints(today, 7,
                metrics.upcomingAssignedInterviewDates(userId, LocalDateTime.now(clock),
                        today.plusDays(7).atStartOfDay()), "/interviews");
        List<Point> evaluations = List.of(
                new Point("Chờ đánh giá", metrics.pendingInterviewEvaluations(userId), "/interviews"),
                new Point("Đã đánh giá", metrics.completedInterviewsEvaluatedBy(userId), "/interviews"));
        return List.of(
                new RecruitmentChartResponse("Lịch được phân công theo trạng thái",
                        "Các lịch bạn tham gia hội đồng; mở danh sách để xem chi tiết.", "bar", statuses),
                new RecruitmentChartResponse("Lịch của tôi trong 7 ngày tới",
                        "Lịch đã hẹn hoặc đổi lịch được phân công cho bạn.", "line", upcoming),
                new RecruitmentChartResponse("Đánh giá sau phỏng vấn",
                        "Các lịch đã hoàn tất được phân công cho bạn, theo tình trạng phiếu đánh giá của bạn.",
                        "bar", evaluations));
    }

    private List<Point> statusPoints(List<StatusCount> rows, List<String> statuses,
            java.util.function.Function<String, String> href,
            java.util.function.Function<String, String> label) {
        Map<String, Long> counts = new LinkedHashMap<>();
        if (rows != null) {
            for (StatusCount row : rows) {
                counts.put(row.status(), row.count());
            }
        }
        List<Point> points = new ArrayList<>();
        for (String status : statuses) {
            points.add(new Point(label.apply(status), counts.getOrDefault(status, 0L), href.apply(status)));
        }
        return points;
    }

    private List<Point> dailyPoints(LocalDate firstDay, int days, List<LocalDateTime> dates, String href) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (int offset = 0; offset < days; offset++) {
            counts.put(firstDay.plusDays(offset), 0L);
        }
        if (dates != null) {
            for (LocalDateTime date : dates) {
                counts.computeIfPresent(date.toLocalDate(), (day, count) -> count + 1);
            }
        }
        List<Point> points = new ArrayList<>();
        counts.forEach((date, count) -> points.add(new Point(date.format(DAY_LABEL), count, href)));
        return points;
    }

    private String postingLabel(String status) {
        return switch (status) {
            case JobPostingAccess.STATUS_DRAFT -> "Bản nháp";
            case JobPostingAccess.STATUS_PUBLISHED -> "Đã đăng";
            case JobPostingAccess.STATUS_SCHEDULED -> "Hẹn đăng";
            case JobPostingAccess.STATUS_PAUSED -> "Tạm dừng";
            case JobPostingAccess.STATUS_CLOSED -> "Đã đóng";
            default -> status;
        };
    }

    private String interviewLabel(String status) {
        return InterviewStatus.valueOf(status).getDisplayName();
    }
}
