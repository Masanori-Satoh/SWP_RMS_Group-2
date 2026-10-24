package com.group2.rms.dashboard;

import com.group2.rms.admin.service.ApiMonitoringService;
import com.group2.rms.dashboard.DashboardMetricsRepository.StatusCount;

import com.group2.rms.dashboard.dto.AdminDashboardResponse;
import com.group2.rms.dashboard.dto.AdminDashboardResponse.AccountSummary;
import com.group2.rms.dashboard.dto.AdminDashboardResponse.DepartmentSummary;
import com.group2.rms.dashboard.dto.AdminDashboardResponse.Shortcut;

import com.group2.rms.dashboard.dto.CandidateDashboardResponse;
import com.group2.rms.dashboard.dto.CandidateDashboardResponse.ApplicationItem;
import com.group2.rms.dashboard.dto.CandidateDashboardResponse.CandidateProfile;
import com.group2.rms.dashboard.dto.CandidateDashboardResponse.InterviewItem;
import com.group2.rms.dashboard.dto.CandidateDashboardResponse.OfferItem;

import com.group2.rms.dashboard.dto.DashboardBreakdownResponse;
import com.group2.rms.dashboard.dto.DashboardMetricResponse;
import com.group2.rms.dashboard.dto.DashboardResponse;
import com.group2.rms.dashboard.dto.RecruitmentDashboardResponse;

import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                // check if user is inactive
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
                        case "Director" -> director(user, now);
                        case "Interviewer" -> interviewer(user, now);
                        case "Candidate" -> candidate(user, now);
                        default -> throw new IllegalStateException("Unsupported dashboard role");
                };
        }

        @SuppressWarnings("null")
        private AdminDashboardResponse admin(User user) {
               
                LocalDateTime to = LocalDateTime.now();
                LocalDateTime from = to.minusDays(30);
                var monitorRows = monitoring.rows();
                Map<String, Long> healthCounts = new LinkedHashMap<>();
                monitorRows.forEach(row -> healthCounts.merge(row.status(), 1L, Long::sum));
                List<StatusCount> health = healthCounts.entrySet().stream()
                                .map(entry -> new StatusCount(entry.getKey(), entry.getValue()))
                                .toList();
                return new AdminDashboardResponse(
                                user.getRole().getRoleName(),
                                user.getFullName(),
                                "Quản lý tài khoản, phòng ban và tình trạng tích hợp của hệ thống.",
                                List.of(accountSummary("Tài khoản nội bộ", "/admin/accounts",
                                                metrics.internalAccountStatuses()),
                                                accountSummary("Tài khoản ứng viên", "/admin/candidate-accounts",
                                                                metrics.candidateAccountStatuses())),
                                departmentSummary(),
                                List.of(new DashboardBreakdownResponse("API và dịch vụ tích hợp", health)),
                                List.of(new Shortcut("Quản lý phòng ban", "/admin/departments"),
                                                new Shortcut("Giám sát API", "/admin/api-monitoring"),
                                                new Shortcut("Cấu hình AI (chưa khả dụng)", null)),
                                metrics.internalAccountsByRole(),
                                monitorRows,
                                new AdminDashboardResponse.CandidateSummary(metrics.candidates(),
                                                metrics.candidatesCreatedBetween(from, to), from, to,
                                                ZoneId.systemDefault().getId()));
        }

        @SuppressWarnings("null")
        private DepartmentSummary departmentSummary() {
                var statuses = metrics.departmentStatuses();
                long active = statuses.stream().filter(row -> "Active".equals(row.status()))
                                .mapToLong(StatusCount::count).sum();
                long inactive = statuses.stream().filter(row -> "Inactive".equals(row.status()))
                                .mapToLong(StatusCount::count).sum();
                return new DepartmentSummary(statuses.stream().mapToLong(StatusCount::count).sum(), active, inactive);
        }

        @SuppressWarnings("null")
        private AccountSummary accountSummary(String title, String url, List<StatusCount> statuses) {
                Map<String, Long> counts = new java.util.HashMap<>();
                statuses.forEach(row -> counts.put(row.status(), row.count()));
                return new AccountSummary(title, url, statuses.stream().mapToLong(StatusCount::count).sum(),
                                counts.getOrDefault("Active", 0L), counts.getOrDefault("Inactive", 0L),
                                counts.getOrDefault("Blocked", 0L));
        }

        @SuppressWarnings("null")
        private RecruitmentDashboardResponse hr(User user, LocalDateTime now) {
                return view(user,
                                "The entire recruitment process",
                                List.of( // get active job posting
                                                new DashboardMetricResponse("Open Job Postings",
                                                                metrics.activeJobPostings(now),
                                                                "Published and within the application deadline"),
                                                // get total application
                                                new DashboardMetricResponse("Total Applications",
                                                                metrics.applications(),
                                                                "All Applications"),
                                                // get upcoming interview
                                                new DashboardMetricResponse("Upcoming Interviews",
                                                                metrics.upcomingInterviews(now),
                                                                "Scheduled or Rescheduled"),
                                                // get cv not filtered by HR
                                                new DashboardMetricResponse("New Applications Awaiting HR Review",
                                                                metrics.newApplicationsAwaitingHrReview(),
                                                                "Applied and not yet reviewed")),
                                // get number of candidate
                                List.of(new DashboardBreakdownResponse("Applications by Status",
                                                metrics.formalApplicationStatuses())),
                                List.of());
        }

        @SuppressWarnings("null")
        private RecruitmentDashboardResponse hiringManager(User user, LocalDateTime now) {
                int userId = user.getUserId();
                Integer departmentId = user.getDepartment() == null ? null : user.getDepartment().getDepartmentId();
                return view(user,
                                "Requisitions I Manage" + (departmentId == null ? "" : " and my department"),
                                List.of(
                                                // get requisitions managed by hiring manager
                                                new DashboardMetricResponse("My Requisitions",
                                                                metrics.ownRequisitions(userId),
                                                                "Linked to this account’s HiringManagerId"),
                                                // get department requisitions
                                                new DashboardMetricResponse("Department Requisitions",
                                                                departmentId == null ? 0
                                                                                : metrics.departmentRequisitions(
                                                                                                departmentId),
                                                                departmentId == null ? "This account has no department"
                                                                                : "Same DepartmentId"),
                                                // get requis pending
                                                new DashboardMetricResponse("Requisitions Awaiting Director Approval",
                                                                metrics.ownPendingRequisitions(userId),
                                                                "My requisitions with Pending_Director status"),
                                                // get candidate for req
                                                new DashboardMetricResponse("Candidates Forwarded to My Department",
                                                                metrics.visibleCandidatesForHiringManager(userId, departmentId),
                                                                "Distinct candidates in the department pipeline"), // dunglt bổ sung: Q1.4, không còn lọc "HR đã chuyển"
                                                // get upcoming inter
                                                new DashboardMetricResponse("Upcoming Interviews",
                                                                metrics.upcomingInterviewsForDepartment(departmentId, now),
                                                                "Within my department")),
                                List.of(),
                                List.of());
        }

        private RecruitmentDashboardResponse director(User user, LocalDateTime now) {
                int userId = user.getUserId();
                return view(user,
                                "System-wide approval queue; my own activity",
                                List.of(
                                                // get requis not approve
                                                new DashboardMetricResponse("Requisitions Awaiting Approval",
                                                                metrics.requisitionsAwaitingDirector(),
                                                                "System-wide Pending_Director records"),
                                                // get offer not approve
                                                new DashboardMetricResponse("Offers Awaiting Approval",
                                                                metrics.offersAwaitingDirector(),
                                                                "System-wide Pending_Director records"),
                                                // get interview is assigned
                                                new DashboardMetricResponse(
                                                                "Upcoming Assigned Interviews",
                                                                metrics.upcomingAssignedInterviews(userId, now),
                                                                "Interviews assigned to me through InterviewPanel"),
                                                // get candidate is assigned
                                                new DashboardMetricResponse("Assigned Candidates",
                                                                metrics.assignedCandidatesForInterviewer(userId),
                                                                "Distinct candidates linked through InterviewPanel")),
                                List.of(),
                                metrics.recentDirectorActivity(userId));
        }

        private RecruitmentDashboardResponse interviewer(User user, LocalDateTime now) {
                int userId = user.getUserId();
                return view(user,
                                "Only interviews assigned to me",
                                List.of(
                                                new DashboardMetricResponse("Upcoming Interviews",
                                                                metrics.upcomingAssignedInterviews(userId, now),
                                                                "Assigned through InterviewPanel"),
                                                new DashboardMetricResponse("Assigned Candidates",
                                                                metrics.assignedCandidatesForInterviewer(userId),
                                                                "Distinct candidates linked through InterviewPanel"),
                                                new DashboardMetricResponse("Pending Interview Evaluations",
                                                                metrics.pendingInterviewEvaluations(userId),
                                                                "Completed interviews without my evaluation")),
                                List.of(), List.of());
        }

        @SuppressWarnings("null")
        private CandidateDashboardResponse candidate(User user, LocalDateTime now) {
                int userId = user.getUserId();

                var applicationStatuses = metrics.candidateApplicationStages(userId)
                                .stream()
                                .map(row -> new StatusCount(
                                                applicationLabel(row.status()),
                                                row.count()))
                                .toList();

                var offerStatuses = metrics.candidateOfferStatuses(userId)
                                .stream()
                                .map(row -> new StatusCount(
                                                offerLabel(row.status()),
                                                row.count()))
                                .toList();

                var applications = metrics.candidateRecentApplications(userId, 20)
                                .stream()
                                .map(row -> new ApplicationItem(
                                                row.jobTitle(),
                                                row.submittedAt().toLocalDate(),
                                                null,
                                                applicationLabel(row.rawStatus())))
                                .toList();

                var interviews = metrics.candidateNextInterviews(userId, now, 10)
                                .stream()
                                .map(row -> {
                                        boolean online = "Online_GoogleMeet".equals(row.format());

                                        return new InterviewItem(
                                                        row.jobTitle(),
                                                        row.startTime(),
                                                        row.endTime(),
                                                        online ? "Trực tuyến" : "Tại văn phòng",
                                                        online ? safeMeetingUrl(row.locationOrLink()) : null,
                                                        online ? null : row.locationOrLink());
                                })
                                .toList();

                var offers = metrics.candidateOffers(userId, 20)
                                .stream()
                                .map(row -> new OfferItem(
                                                row.jobTitle(),
                                                row.salary(),
                                                row.startDate(),
                                                offerLabel(row.rawStatus()),
                                                row.probationSalary(),
                                                row.workLocation(),
                                                row.benefits()))
                                .toList();

                return new CandidateDashboardResponse(
                                user.getRole().getRoleName(),
                                user.getFullName(),
                                "Theo dõi hồ sơ, lịch phỏng vấn và thư mời dành cho bạn.",
                                List.of(
                                                new DashboardMetricResponse("Đơn ứng tuyển",
                                                                metrics.candidateApplications(userId),
                                                                "Tổng số hồ sơ bạn đã nộp"),
                                                new DashboardMetricResponse("Phỏng vấn sắp tới",
                                                                metrics.candidateUpcomingInterviews(userId, now),
                                                                "Lịch đã được hẹn hoặc đổi lịch"),
                                                new DashboardMetricResponse("Thư mời chờ phản hồi",
                                                                metrics.candidateOffersAwaitingResponse(userId),
                                                                "Thư mời chính thức đã gửi cho bạn")),
                                applicationStatuses,
                                offerStatuses,
                                applications,
                                interviews,
                                offers,
                                new CandidateProfile(
                                                user.getFullName(),
                                                user.getEmail(),
                                                user.getDepartment() == null ? null
                                                                : user.getDepartment().getDepartmentName()));
        }

        // Presentation labels only. These never change persisted statuses or infer a
        // separate stage.
        private String applicationLabel(String status) {
                return switch (status) {
                        case "Applied" -> "Đã nộp hồ sơ";
                        case "AI_Screened" -> "Đã sàng lọc";
                        case "HR_Passed" -> "Qua vòng nhân sự";
                        case "HM_Passed" -> "Qua vòng chuyên môn";
                        case "Interviewing" -> "Đang phỏng vấn";
                        case "Offered" -> "Đã có thư mời";
                        case "Hired" -> "Đã nhận việc"; // dunglt bổ sung: khớp nhãn ApplicationStatusLabels
                        case "Rejected" -> "Không tiếp tục";
                        default -> status;
                };
        }

        private String offerLabel(String status) {
                return switch (status) {
                        case "Sent_Candidate" -> "Chờ phản hồi";
                        case "Accepted" -> "Đã chấp nhận";
                        case "Declined" -> "Đã từ chối";
                        default -> status;
                };
        }

        private String safeMeetingUrl(String value) {
                if (value == null || value.isBlank())
                        return null;
                try {
                        URI uri = URI.create(value.trim());
                        return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                                        && uri.getHost() != null && uri.getUserInfo() == null ? uri.toString() : null;
                } catch (IllegalArgumentException exception) {
                        return null;
                }
        }

        private RecruitmentDashboardResponse view(User user, String scope, List<DashboardMetricResponse> numeric,
                        List<DashboardBreakdownResponse> breakdowns,
                        List<DashboardMetricsRepository.ApprovalActivity> activity) {
                return new RecruitmentDashboardResponse(
                                user.getRole().getRoleName(),
                                user.getFullName(),
                                scope,
                                numeric,
                                breakdowns,
                                activity);
        }

}
