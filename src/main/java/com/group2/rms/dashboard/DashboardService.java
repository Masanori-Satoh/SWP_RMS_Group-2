package com.group2.rms.dashboard;

import com.group2.rms.dashboard.DashboardMetricsRepository;
import com.group2.rms.dashboard.DashboardResponse;
import com.group2.rms.dashboard.DashboardResponse.AccountSummary;
import com.group2.rms.dashboard.DashboardResponse.Breakdown;
import com.group2.rms.dashboard.DashboardResponse.Metric;
import com.group2.rms.dashboard.DashboardResponse.Shortcut;
import com.group2.rms.dashboard.DashboardResponse.Unavailable;
import com.group2.rms.user.entity.User;
import com.group2.rms.dashboard.DashboardMetricsRepository.StatusCount;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.admin.service.ApiMonitoringService;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.net.URI;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.group2.rms.dashboard.DashboardResponse.*;

/**
 * Builds a dashboard from the persisted account role and role-scoped
 * aggregates.
 */
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
        public DashboardResponse forUsername(String username) {
                User user = userRepository.findByUsernameIgnoreCase(username)
                                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
                //check if  user is inactive
                if (!"Active".equals(user.getAccountStatus())) {
                        throw new AccessDeniedException("Inactive account cannot access dashboard");
                }

                LocalDateTime now = LocalDateTime.now();
                String role = user.getRole().getRoleName();
                // performed navigation based role
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

        private DashboardResponse admin(User user) {
                Map<String, Long> healthCounts = new LinkedHashMap<>();
                monitoring.rows().forEach(row -> healthCounts.merge(row.statusLabel(), 1L, Long::sum));
                List<StatusCount> health = healthCounts.entrySet().stream()
                                .map(entry -> new StatusCount(entry.getKey(), entry.getValue()))
                                .toList();
                return new DashboardResponse(user.getRole().getRoleName(),
                                        user.getFullName(),
                                        "Quản lý tài khoản, phòng ban và tình trạng tích hợp của hệ thống.",
                                List.of(),
                                List.of(new Breakdown("API và dịch vụ tích hợp", health)), List.of(),
                                List.of(new Unavailable("Cấu hình AI",                                                               "Chưa có danh mục cấu hình được xác nhận để triển khai trang chỉnh sửa.")),
                                List.of(new Shortcut("Quản lý phòng ban", "/admin/departments"),
                                        new Shortcut("Giám sát API", "/admin/api-monitoring"),
                                        new Shortcut("Cấu hình AI (chưa khả dụng)", null)),
                                List.of(accountSummary("Tài khoản nội bộ", "/admin/accounts",
                                                metrics.internalAccountStatuses()),
                                                accountSummary("Tài khoản ứng viên", "/admin/candidate-accounts",
                                                                metrics.candidateAccountStatuses())), null, departmentSummary());
        }

        private DepartmentSummary departmentSummary() {
                var statuses = metrics.departmentStatuses();
                long active = statuses.stream().filter(row -> "Active".equals(row.status())).mapToLong(StatusCount::count).sum();
                long inactive = statuses.stream().filter(row -> "Inactive".equals(row.status())).mapToLong(StatusCount::count).sum();
                return new DepartmentSummary(statuses.stream().mapToLong(StatusCount::count).sum(), active, inactive);
        }

        private AccountSummary accountSummary(String title, String url, List<StatusCount> statuses) {
                Map<String, Long> counts = new java.util.HashMap<>();
                statuses.forEach(row -> counts.put(row.status(), row.count()));
                return new AccountSummary(title, url, statuses.stream().mapToLong(StatusCount::count).sum(),
                                counts.getOrDefault("Active", 0L), counts.getOrDefault("Inactive", 0L),
                                counts.getOrDefault("Blocked", 0L));
        }

        private DashboardResponse hr(User user, LocalDateTime now) {
                return view(user, "The entire recruitment process",
                                List.of(
                                                new Metric("Open Job Postings", metrics.activeJobPostings(now),
                                                                "Published and within the application deadline"),
                                                new Metric("Total Applications", metrics.applications(),
                                                                "All Applications"),
                                                new Metric("Upcoming Interviews", metrics.upcomingInterviews(now),
                                                                "Scheduled or Rescheduled"),
                                                new Metric("New Applications Awaiting HR Review",
                                                                metrics.newApplicationsAwaitingHrReview(),
                                                                "Applied and not yet reviewed")),
                                List.of(new Breakdown("Candidates by Recruitment Stage", metrics.applicationStages())),
                                List.of(), List.of(), List.of());
        }

        private DashboardResponse hiringManager(User user, LocalDateTime now) {
                int userId = user.getUserId();
                Integer departmentId = user.getDepartment() == null ? null : user.getDepartment().getDepartmentId();
                return view(user, "Requisitions I Manage" + (departmentId == null ? "" : " and my department"),
                                List.of(
                                                new Metric("My Requisitions", metrics.ownRequisitions(userId),
                                                                "Linked to this account’s HiringManagerId"),
                                                new Metric("Department Requisitions",
                                                                departmentId == null ? 0
                                                                                : metrics.departmentRequisitions(
                                                                                                departmentId),
                                                                departmentId == null ? "This account has no department"
                                                                                : "Same DepartmentId"),
                                                new Metric("Requisitions Awaiting Director Approval",
                                                                metrics.ownPendingRequisitions(userId),
                                                                "My requisitions with Pending_Director status"),
                                                new Metric("Candidates for My Requisitions",
                                                                metrics.candidatesForOwnRequisitions(userId),
                                                                "Distinct candidates who have applied"),
                                                new Metric("Upcoming Interviews",
                                                                metrics.upcomingInterviewsForHiringManager(userId, now),
                                                                "Within my requisitions"),
                                                new Metric("My Offers Requiring Action",
                                                                metrics.offersNeedingHiringManagerAction(userId),
                                                                "Draft or Approved offers proposed by me")),
                                List.of(), List.of(), List.of(), List.of());
        }

        private DashboardResponse director(User user) {
                return view(user, "System-wide approval queue; my own activity",
                                List.of(
                                                new Metric("Requisitions Awaiting Approval",
                                                                metrics.requisitionsAwaitingDirector(),
                                                                "System-wide Pending_Director records"),
                                                new Metric("Offers Awaiting Approval", metrics.offersAwaitingDirector(),
                                                                "System-wide Pending_Director records")),
                                List.of(), metrics.recentDirectorActivity(user.getUserId()), List.of(), List.of());
        }

        private DashboardResponse interviewer(User user, LocalDateTime now) {
                int userId = user.getUserId();
                return view(user, "Only interviews assigned to me",
                                List.of(
                                                new Metric("Upcoming Interviews",
                                                                metrics.upcomingAssignedInterviews(userId, now),
                                                                "Assigned through InterviewPanel"),
                                                new Metric("Assigned Candidates",
                                                                metrics.assignedCandidatesForInterviewer(userId),
                                                                "Distinct candidates linked through InterviewPanel"),
                                                new Metric("Pending Interview Evaluations",
                                                                metrics.pendingInterviewEvaluations(userId),
                                                                "Completed interviews without my evaluation")),
                                List.of(), List.of(), List.of(), List.of());
        }

        private DashboardResponse candidate(User user, LocalDateTime now) {
                int userId = user.getUserId();
                var applications = metrics.candidateRecentApplications(userId, 20).stream()
                                .map(row -> new ApplicationItem(row.jobTitle(),
                                                row.submittedAt().toLocalDate(), null,
                                                applicationLabel(row.rawStatus())))
                                .toList();
                var interviews = metrics.candidateNextInterviews(userId, now, 10).stream()
                                .map(row -> {
                                        boolean online = "Online_GoogleMeet".equals(row.format());
                                        return new InterviewItem(row.jobTitle(), row.startTime(), row.endTime(),
                                                        online ? "Trực tuyến" : "Tại văn phòng",
                                                        online ? safeMeetingUrl(row.locationOrLink()) : null,
                                                        online ? null : row.locationOrLink());
                                }).toList();
                var offers = metrics.candidateOffers(userId, 20).stream()
                                .map(row -> new OfferItem(row.jobTitle(), row.salary(), row.startDate(),
                                                offerLabel(row.rawStatus()), row.probationSalary(),
                                                row.workLocation(), row.benefits()))
                                .toList();
                return new DashboardResponse(user.getRole().getRoleName(), user.getFullName(),
                                "Theo dõi hồ sơ, lịch phỏng vấn và thư mời dành cho bạn.",
                                List.of(
                                                new Metric("Đơn ứng tuyển", metrics.candidateApplications(userId),
                                                                "Tổng số hồ sơ bạn đã nộp"),
                                                new Metric("Phỏng vấn sắp tới",
                                                                metrics.candidateUpcomingInterviews(userId, now),
                                                                "Lịch đã được hẹn hoặc đổi lịch"),
                                                new Metric("Thư mời chờ phản hồi",
                                                                metrics.candidateOffersAwaitingResponse(userId),
                                                                "Thư mời chính thức đã gửi cho bạn")),
                                List.of(
                                                new Breakdown("Trạng thái đơn ứng tuyển",
                                                                metrics.candidateApplicationStages(userId).stream()
                                                                                .map(row -> new StatusCount(applicationLabel(row.status()), row.count()))
                                                                                .toList()),
                                                new Breakdown("Trạng thái thư mời",
                                                                metrics.candidateOfferStatuses(userId).stream()
                                                                                .map(row -> new StatusCount(offerLabel(row.status()), row.count()))
                                                                                .toList())),
                                List.of(), List.of(), List.of(), List.of(),
                                new CandidatePanel(applications, interviews, offers,
                                                new CandidateProfile(user.getFullName(), user.getEmail(),
                                                                user.getDepartment() == null ? null
                                                                                : user.getDepartment().getDepartmentName())));
        }

        // Presentation labels only. These never change persisted statuses or infer a separate stage.
        private String applicationLabel(String status) {
                return switch (status) {
                        case "Applied" -> "Đã nộp hồ sơ";
                        case "AI_Screened" -> "Đã sàng lọc";
                        case "HR_Passed" -> "Qua vòng nhân sự";
                        case "HM_Passed" -> "Qua vòng chuyên môn";
                        case "Interviewing" -> "Đang phỏng vấn";
                        case "Offered" -> "Đã có thư mời";
                        case "Hired" -> "Đã tuyển dụng";
                        case "Rejected" -> "Không tiếp tục";
                        default -> status;
                };
        }

        private String offerLabel(String status) {
                return switch (status) {
                        case "Sent_Candidate" -> "Chờ phản hồi";
                        case "Negotiating" -> "Đang thương lượng";
                        case "Accepted" -> "Đã chấp nhận";
                        case "Declined" -> "Đã từ chối";
                        default -> status;
                };
        }

        private String safeMeetingUrl(String value) {
                if (value == null || value.isBlank()) return null;
                try {
                        URI uri = URI.create(value.trim());
                        return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                                        && uri.getHost() != null && uri.getUserInfo() == null ? uri.toString() : null;
                } catch (IllegalArgumentException exception) {
                        return null;
                }
        }

        private DashboardResponse view(User user, String scope, List<Metric> numeric,
                        List<Breakdown> breakdowns,
                        List<DashboardMetricsRepository.ApprovalActivity> activity,
                        List<Unavailable> unavailable, List<Shortcut> shortcuts) {
                return new DashboardResponse(user.getRole().getRoleName(), user.getFullName(), scope,
                                numeric, breakdowns, activity, unavailable, shortcuts);
        }

}
