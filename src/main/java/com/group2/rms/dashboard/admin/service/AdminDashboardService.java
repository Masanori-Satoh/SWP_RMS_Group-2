package com.group2.rms.dashboard.admin.service;

import com.group2.rms.admin.service.ApiMonitoringService;
import com.group2.rms.dashboard.admin.dto.AdminDashboardResponse;
import com.group2.rms.dashboard.admin.dto.AdminDashboardResponse.AccountSummary;
import com.group2.rms.dashboard.admin.dto.AdminDashboardResponse.DepartmentSummary;
import com.group2.rms.dashboard.admin.dto.AdminDashboardResponse.Shortcut;
import com.group2.rms.dashboard.shared.DashboardAccess;
import com.group2.rms.dashboard.shared.DashboardBreakdownResponse;
import com.group2.rms.dashboard.shared.DashboardMetricsRepository;
import com.group2.rms.dashboard.shared.DashboardMetricsRepository.StatusCount;
import com.group2.rms.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * SERVICE: dựng dữ liệu dashboard System Admin (tài khoản, phòng ban, ứng viên, tình trạng API).
 */
@Service
public class AdminDashboardService {

        private final DashboardAccess access;
        private final DashboardMetricsRepository metrics;
        private final ApiMonitoringService monitoring;
        private final Clock clock;

        public AdminDashboardService(DashboardAccess access, DashboardMetricsRepository metrics,
                        ApiMonitoringService monitoring, Clock clock) {
                this.access = access;
                this.metrics = metrics;
                this.monitoring = monitoring;
                this.clock = clock;
        }

        @Transactional(readOnly = true)
        public AdminDashboardResponse forUsername(String username) {
                User user = access.actor(username);
                access.requireAdmin(user);
                return build(user);
        }

        @SuppressWarnings("null")
        private AdminDashboardResponse build(User user) {
                LocalDateTime to = LocalDateTime.now(clock);
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
                                                // dunglt bổ sung: bỏ lối tắt "Cấu hình AI" (gỡ AI, 01/11)
                                                new Shortcut("Giám sát API", "/admin/api-monitoring")),
                                metrics.internalAccountsByRole(),
                                monitorRows,
                                new AdminDashboardResponse.CandidateSummary(metrics.countCandidates(),
                                                metrics.countCandidatesCreatedBetween(from, to), from, to,
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
                                counts.getOrDefault("Active", 0L), counts.getOrDefault("Inactive", 0L));
        }
}
