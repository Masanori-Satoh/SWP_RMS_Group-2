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
        return new DashboardView(user.getRole().getRoleName(), user.getFullName(), "Internal employees and external candidates",
                List.of(),
                List.of(new Breakdown("API and Integration Health", health)), List.of(),
                List.of(
                        new Unavailable("AI Configuration", "No confirmed configuration keys or editing page are available yet.")),
                List.of(new Shortcut("API Monitoring", "/admin/api-monitoring"),
                        new Shortcut("AI Configuration (Unavailable)", null)),
                List.of(accountSummary("Internal Accounts", "/admin/accounts", metrics.internalAccountStatuses()),
                        accountSummary("Candidate Accounts", "/admin/candidate-accounts", metrics.candidateAccountStatuses())));
    }

    private AccountSummary accountSummary(String title, String url, List<StatusCount> statuses) {
        Map<String, Long> counts = new java.util.HashMap<>();
        statuses.forEach(row -> counts.put(row.status(), row.count()));
        return new AccountSummary(title, url, statuses.stream().mapToLong(StatusCount::count).sum(),
                counts.getOrDefault("Active", 0L), counts.getOrDefault("Inactive", 0L), counts.getOrDefault("Blocked", 0L));
    }

    private DashboardView hr(User user, LocalDateTime now) {
        return view(user, "The entire recruitment process",
                List.of(
                        new Metric("Open Job Postings", metrics.activeJobPostings(now),
                                "Published and within the application deadline"),
                        new Metric("Total Applications", metrics.applications(), "All Applications"),
                        new Metric("Upcoming Interviews", metrics.upcomingInterviews(now), "Scheduled or Rescheduled"),
                        new Metric("New Applications Awaiting HR Review", metrics.newApplicationsAwaitingHrReview(),
                                "Applied and not yet reviewed")),
                List.of(new Breakdown("Candidates by Recruitment Stage", metrics.applicationStages())),
                List.of(), List.of(), List.of());
    }

    private DashboardView hiringManager(User user, LocalDateTime now) {
        int userId = user.getUserId();
        Integer departmentId = user.getDepartment() == null ? null : user.getDepartment().getDepartmentId();
        return view(user, "Requisitions I Manage" + (departmentId == null ? "" : " and my department"),
                List.of(
                        new Metric("My Requisitions", metrics.ownRequisitions(userId), "Linked to this account’s HiringManagerId"),
                        new Metric("Department Requisitions",
                                departmentId == null ? 0 : metrics.departmentRequisitions(departmentId),
                                departmentId == null ? "This account has no department" : "Same DepartmentId"),
                        new Metric("Requisitions Awaiting Director Approval", metrics.ownPendingRequisitions(userId),
                                "My requisitions with Pending_Director status"),
                        new Metric("Candidates for My Requisitions", metrics.candidatesForOwnRequisitions(userId),
                                "Distinct candidates who have applied"),
                        new Metric("Upcoming Interviews", metrics.upcomingInterviewsForHiringManager(userId, now),
                                "Within my requisitions"),
                        new Metric("My Offers Requiring Action", metrics.offersNeedingHiringManagerAction(userId),
                                "Draft or Approved offers proposed by me")),
                List.of(), List.of(), List.of(), List.of());
    }

    private DashboardView director(User user) {
        return view(user, "System-wide approval queue; my own activity",
                List.of(
                        new Metric("Requisitions Awaiting Approval", metrics.requisitionsAwaitingDirector(),
                                "System-wide Pending_Director records"),
                        new Metric("Offers Awaiting Approval", metrics.offersAwaitingDirector(),
                                "System-wide Pending_Director records")),
                List.of(), metrics.recentDirectorActivity(user.getUserId()), List.of(), List.of());
    }

    private DashboardView interviewer(User user, LocalDateTime now) {
        int userId = user.getUserId();
        return view(user, "Only interviews assigned to me",
                List.of(
                        new Metric("Upcoming Interviews", metrics.upcomingAssignedInterviews(userId, now),
                                "Assigned through InterviewPanel"),
                        new Metric("Assigned Candidates", metrics.assignedCandidatesForInterviewer(userId),
                                "Distinct candidates linked through InterviewPanel"),
                        new Metric("Pending Interview Evaluations", metrics.pendingInterviewEvaluations(userId),
                                "Completed interviews without my evaluation")),
                List.of(), List.of(), List.of(), List.of());
    }

    private DashboardView candidate(User user, LocalDateTime now) {
        int userId = user.getUserId();
        return view(user, "Only the candidate profile linked to my account",
                List.of(
                        new Metric("My Applications", metrics.candidateApplications(userId),
                                "Linked through this account’s Candidate.UserId"),
                        new Metric("Upcoming Interviews", metrics.candidateUpcomingInterviews(userId, now),
                                "Only my interview schedule")),
                List.of(
                        new Breakdown("Application Status", metrics.candidateApplicationStages(userId)),
                        new Breakdown("Offer Status", metrics.candidateOfferStatuses(userId))),
                List.of(),
                List.of(new Unavailable("Notifications", "No notification table or service is available yet.")),
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
