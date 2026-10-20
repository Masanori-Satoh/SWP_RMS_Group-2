package com.group2.rms.dashboard;

import com.group2.rms.candidate.service.ApplicationStatusLabels;
import com.group2.rms.dashboard.DashboardMetricsRepository.DecisionRow;
import com.group2.rms.dashboard.DashboardMetricsRepository.StatusCount;
import com.group2.rms.dashboard.dto.DashboardChartResponse;
import com.group2.rms.dashboard.dto.DashboardChartResponse.Point;
import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.requisition.service.RequisitionAccess;
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
@Service
public class DashboardChartService {
    private static final List<String> REQUISITION_STATUSES = List.of(
            RequisitionAccess.STATUS_DRAFT, RequisitionAccess.STATUS_PENDING_DIRECTOR,
            RequisitionAccess.STATUS_APPROVED, RequisitionAccess.STATUS_REJECTED);
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
    public List<DashboardChartResponse> forUsername(String username, String roleName) {
        User user = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        if (!"Active".equals(user.getAccountStatus()) || user.getRole() == null
                || !roleName.equals(user.getRole().getRoleName())) {
            throw new AccessDeniedException("Dashboard role is no longer active");
        }
        return switch (roleName) {
            case "Hiring Manager" -> hiringManager(user);
            case "HR" -> hr();
            case "Director" -> director(user);
            case "Interviewer" -> interviewer(user);
            default -> List.of();
        };
    }

    private List<DashboardChartResponse> hiringManager(User user) {
        int userId = user.getUserId();
        Integer departmentId = user.getDepartment() == null ? null : user.getDepartment().getDepartmentId();
        List<Point> requisitions = statusPoints(metrics.hiringManagerRequisitionStatuses(userId, departmentId),
                REQUISITION_STATUSES, status -> "/requisitions?status=" + status, this::requisitionLabel);
        List<Point> applications = statusPoints(metrics.hiringManagerApplicationStatuses(userId, departmentId),
                APPLICATION_STATUSES, status -> "/applications?status=" + status, ApplicationStatusLabels::label);
        LocalDate today = LocalDate.now(clock);
        List<Point> interviews = dailyPoints(today, 7,
                metrics.upcomingDepartmentInterviewDates(departmentId, LocalDateTime.now(clock),
                        today.plusDays(7).atStartOfDay()), "/interviews");
        return List.of(
                new DashboardChartResponse("Yêu cầu trong phạm vi của tôi",
                        "Yêu cầu do bạn tạo hoặc thuộc phòng ban bạn quản lý; chọn trạng thái để mở danh sách.",
                        "bar", requisitions),
                new DashboardChartResponse("Hồ sơ HR đã chuyển",
                        "Hồ sơ chính thức trong phạm vi phòng ban của bạn, theo trạng thái hiện tại.",
                        "bar", applications),
                new DashboardChartResponse("Lịch phỏng vấn 7 ngày tới",
                        "Lịch sắp tới thuộc phòng ban của bạn.", "line", interviews));
    }

    private List<DashboardChartResponse> hr() {
        LocalDate today = LocalDate.now(clock);
        List<Point> applications = statusPoints(metrics.formalApplicationStatuses(), APPLICATION_STATUSES,
                status -> "/applications?status=" + status, ApplicationStatusLabels::label);
        List<Point> postings = statusPoints(metrics.jobPostingViewStatuses(LocalDateTime.now(clock)), POSTING_STATUSES,
                status -> "/internal/job-postings?status=" + status, this::postingLabel);
        List<Point> received = dailyPoints(today.minusDays(13), 14,
                metrics.recentApplicationDates(today.minusDays(13).atStartOfDay(), today.plusDays(1).atStartOfDay()),
                null);
        return List.of(
                new DashboardChartResponse("Hồ sơ theo trạng thái", "Số hồ sơ ứng tuyển chính thức trên toàn hệ thống.",
                        "bar", applications),
                new DashboardChartResponse("Hồ sơ nhận trong 14 ngày", "Đếm theo ngày nộp hồ sơ, bao gồm hôm nay.",
                        "line", received),
                new DashboardChartResponse("Tin tuyển dụng theo trạng thái",
                        "Trạng thái hiển thị của tin, gồm cả tin hẹn đăng; số tin đang nhận hồ sơ ở thẻ tổng quan.",
                        "bar", postings));
    }

    private List<DashboardChartResponse> director(User user) {
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
                new DashboardChartResponse("Hàng đợi phê duyệt", "Toàn hệ thống; chọn loại để mở danh sách chờ duyệt.",
                        "bar", queue),
                new DashboardChartResponse("Kết quả quyết định của tôi",
                        "Quyết định của bạn trong 14 ngày qua, gồm yêu cầu và đề nghị tuyển dụng.",
                        "bar", outcomes),
                new DashboardChartResponse("Quyết định của tôi trong 14 ngày",
                        "Số lần duyệt hoặc từ chối do chính bạn thực hiện, theo ngày.",
                        "line", dailyPoints(today.minusDays(13), 14, decisionDates, null)));
    }

    private List<DashboardChartResponse> interviewer(User user) {
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
                new DashboardChartResponse("Lịch được phân công theo trạng thái",
                        "Các lịch bạn tham gia hội đồng; mở danh sách để xem chi tiết.", "bar", statuses),
                new DashboardChartResponse("Lịch của tôi trong 7 ngày tới",
                        "Lịch đã hẹn hoặc đổi lịch được phân công cho bạn.", "line", upcoming),
                new DashboardChartResponse("Đánh giá sau phỏng vấn",
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

    private String requisitionLabel(String status) {
        return switch (status) {
            case RequisitionAccess.STATUS_DRAFT -> "Bản nháp";
            case RequisitionAccess.STATUS_PENDING_DIRECTOR -> "Chờ Giám đốc duyệt";
            case RequisitionAccess.STATUS_APPROVED -> "Đã duyệt";
            case RequisitionAccess.STATUS_REJECTED -> "Bị từ chối";
            default -> status;
        };
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
