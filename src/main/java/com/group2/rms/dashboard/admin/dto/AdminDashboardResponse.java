package com.group2.rms.dashboard.admin.dto;

import java.util.List;
import java.time.LocalDateTime;
import com.group2.rms.admin.dto.MonitorRowResponse;
import com.group2.rms.dashboard.shared.DashboardBreakdownResponse;
import com.group2.rms.dashboard.shared.DashboardMetricsRepository.RoleCount;

/*
 * DTO: toàn bộ dữ liệu dashboard System Admin (dashboard/admin.html).
 */
public record AdminDashboardResponse(
        String roleName,
        String fullName,
        String scope,
        List<AccountSummary> accountSummaries,
        DepartmentSummary departments,
        List<DashboardBreakdownResponse> breakdowns,
        List<Shortcut> shortcuts,
        List<RoleCount> roleCounts,
        List<MonitorRowResponse> monitorRows,
        CandidateSummary candidates) {

    public record CandidateSummary(long total, long newLast30Days,
            LocalDateTime from, LocalDateTime to, String timeZone) {
    }

    public long internalTotal() {
        return accountSummaries.stream()
                .filter(summary -> "/admin/accounts".equals(summary.url()))
                .mapToLong(AccountSummary::total).sum();
    }

    public long blockedTotal() {
        return accountSummaries.stream().mapToLong(AccountSummary::blocked).sum();
    }

    public record AccountSummary(
            String title,
            String url,
            long total,
            long active,
            long inactive,
            long blocked) {
    }

    public record DepartmentSummary(
            long totalCount,
            long activeCount,
            long inactiveCount) {
    }

    public record Shortcut(
            String title,
            String url) {
    }
}
