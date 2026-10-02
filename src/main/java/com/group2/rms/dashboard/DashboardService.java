package com.group2.rms.dashboard;

import com.group2.rms.user.entity.User;
import com.group2.rms.dashboard.DashboardMetricsRepository.StatusCount;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.admin.ApiMonitoringService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.group2.rms.dashboard.DashboardView.*;

/** Builds a dashboard from the persisted account role and role-scoped aggregates. */
@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final DashboardMetricsRepository metrics;
    private final ApiMonitoringService monitoring;

    public DashboardService(UserRepository userRepository, DashboardMetricsRepository metrics,
                            ApiMonitoringService monitoring) {
        this.userRepository = userRepository;
        this.metrics = metrics;
        this.monitoring = monitoring;
    }

    @Transactional(readOnly = true)
    public DashboardView forUsername(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        if (!"Active".equals(user.getAccountStatus())) {
            throw new IllegalStateException("Inactive account cannot access dashboard");
        }

        LocalDateTime now = LocalDateTime.now();
        String role = user.getRole().getRoleName();
        return switch (role) {
            case "System Admin" -> admin(user);
            case "HR" -> hr(user, now);
            case "Hiring Manager" -> hiringManager(user, now);
            case "Director" -> director(user);
            case "Interviewer" -> interviewer(user, now);
            case "Candidate" -> candidate(user, now);
            default -> throw new IllegalStateException("Unsupported dashboard role");
        };
    }

    private DashboardView admin(User user) {
        Map<String, Long> healthCounts = new LinkedHashMap<>();
        monitoring.rows().forEach(row -> healthCounts.merge(row.statusLabel(), 1L, Long::sum));
        List<StatusCount> health = healthCounts.entrySet().stream()
                .map(entry -> new StatusCount(entry.getKey(), entry.getValue()))
                .toList();
        return view(user, "Tất cả tài khoản trong hệ thống",
                List.of(
                        new Metric("Tổng tài khoản", metrics.accounts(), "Bao gồm tài khoản nội bộ và Candidate"),
                        new Metric("Đang hoạt động", metrics.accountsWithStatus("Active"), "Trạng thái Active"),
                        new Metric("Ngừng hoạt động", metrics.accountsWithStatus("Inactive"), "Trạng thái Inactive"),
                        new Metric("Bị khóa", metrics.accountsWithStatus("Blocked"), "Trạng thái Blocked")),
                List.of(new Breakdown("Sức khỏe API và tích hợp", health)), List.of(),
                List.of(
                        new Unavailable("Cấu hình AI", "Chưa có khóa cấu hình được xác nhận hoặc trang chỉnh sửa.")),
                List.of(new Shortcut("Quản lý tài khoản", "/admin/accounts"),
                        new Shortcut("API Monitoring", "/admin/api-monitoring"),
                        new Shortcut("Cấu hình AI (chưa khả dụng)", null)));
    }

    private DashboardView hr(User user, LocalDateTime now) {
        return view(user, "Toàn bộ quy trình tuyển dụng",
                List.of(
                        new Metric("Tin tuyển dụng đang mở", metrics.activeJobPostings(now),
                                "Published và chưa quá hạn nộp"),
                        new Metric("Tổng hồ sơ ứng tuyển", metrics.applications(), "Tất cả hồ sơ"),
                        new Metric("Phỏng vấn sắp tới", metrics.upcomingInterviews(now), "Scheduled hoặc Rescheduled"),
                        new Metric("Hồ sơ mới cần HR xem", metrics.newApplicationsAwaitingHrReview(),
                                "Applied và chưa có người duyệt")),
                List.of(new Breakdown("Ứng viên theo giai đoạn tuyển dụng", metrics.applicationStages())),
                List.of(), List.of(), List.of());
    }

    private DashboardView hiringManager(User user, LocalDateTime now) {
        int userId = user.getUserId();
        Integer departmentId = user.getDepartment() == null ? null : user.getDepartment().getDepartmentId();
        return view(user, "Yêu cầu do tôi phụ trách" + (departmentId == null ? "" : " và phòng ban của tôi"),
                List.of(
                        new Metric("Yêu cầu của tôi", metrics.ownRequisitions(userId), "HiringManagerId của tài khoản"),
                        new Metric("Yêu cầu của phòng ban",
                                departmentId == null ? 0 : metrics.departmentRequisitions(departmentId),
                                departmentId == null ? "Tài khoản chưa gắn phòng ban" : "Cùng DepartmentId"),
                        new Metric("Yêu cầu chờ Giám đốc", metrics.ownPendingRequisitions(userId),
                                "Pending_Director trong yêu cầu của tôi"),
                        new Metric("Ứng viên trong yêu cầu của tôi", metrics.candidatesForOwnRequisitions(userId),
                                "Số Candidate khác nhau đã ứng tuyển"),
                        new Metric("Phỏng vấn sắp tới", metrics.upcomingInterviewsForHiringManager(userId, now),
                                "Trong yêu cầu của tôi"),
                        new Metric("Offer của tôi cần xử lý", metrics.offersNeedingHiringManagerAction(userId),
                                "Offer Draft hoặc Approved do tôi đề xuất")),
                List.of(), List.of(), List.of(), List.of());
    }

    private DashboardView director(User user) {
        return view(user, "Hàng chờ phê duyệt toàn hệ thống; hoạt động của riêng tôi",
                List.of(
                        new Metric("Yêu cầu chờ duyệt", metrics.requisitionsAwaitingDirector(),
                                "Pending_Director trên toàn hệ thống"),
                        new Metric("Offer chờ duyệt", metrics.offersAwaitingDirector(),
                                "Pending_Director trên toàn hệ thống")),
                List.of(), metrics.recentDirectorActivity(user.getUserId()), List.of(), List.of());
    }

    private DashboardView interviewer(User user, LocalDateTime now) {
        int userId = user.getUserId();
        return view(user, "Chỉ các buổi phỏng vấn được phân công cho tôi",
                List.of(
                        new Metric("Phỏng vấn sắp tới", metrics.upcomingAssignedInterviews(userId, now),
                                "Theo InterviewPanel"),
                        new Metric("Ứng viên được phân công", metrics.assignedCandidatesForInterviewer(userId),
                                "Candidate khác nhau trong InterviewPanel"),
                        new Metric("Đánh giá cần hoàn thành", metrics.pendingInterviewEvaluations(userId),
                                "Buổi Completed chưa có đánh giá của tôi")),
                List.of(), List.of(), List.of(), List.of());
    }

    private DashboardView candidate(User user, LocalDateTime now) {
        int userId = user.getUserId();
        return view(user, "Chỉ hồ sơ Candidate gắn với tài khoản của tôi",
                List.of(
                        new Metric("Đơn ứng tuyển của tôi", metrics.candidateApplications(userId),
                                "Theo Candidate.UserId của tài khoản"),
                        new Metric("Phỏng vấn sắp tới", metrics.candidateUpcomingInterviews(userId, now),
                                "Chỉ lịch phỏng vấn của tôi")),
                List.of(
                        new Breakdown("Trạng thái đơn ứng tuyển", metrics.candidateApplicationStages(userId)),
                        new Breakdown("Trạng thái offer", metrics.candidateOfferStatuses(userId))),
                List.of(),
                List.of(new Unavailable("Thông báo", "Hiện chưa có bảng hoặc dịch vụ thông báo để truy vấn.")),
                List.of());
    }

    private DashboardView view(User user, String scope, List<Metric> numeric,
                               List<Breakdown> breakdowns,
                               List<DashboardMetricsRepository.ApprovalActivity> activity,
                               List<Unavailable> unavailable, List<Shortcut> shortcuts) {
        return new DashboardView(user.getRole().getRoleName(), user.getFullName(), scope,
                numeric, breakdowns, activity, unavailable, shortcuts);
    }

}
