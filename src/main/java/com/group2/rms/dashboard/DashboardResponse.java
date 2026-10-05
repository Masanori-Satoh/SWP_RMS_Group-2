package com.group2.rms.dashboard;
import com.group2.rms.dashboard.DashboardMetricsRepository.ApprovalActivity;
import com.group2.rms.dashboard.DashboardMetricsRepository.StatusCount;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Values rendered by the dashboard; all numeric values come from database queries. */
public record DashboardResponse(
        String roleName,
        String fullName,
        String scope,
        List<Metric> metrics,
        List<Breakdown> breakdowns,
        List<ApprovalActivity> activities,
        List<Unavailable> unavailable,
        List<Shortcut> shortcuts,
        List<AccountSummary> accountSummaries,
        CandidatePanel candidate,
        DepartmentSummary departments) {

    public DashboardResponse(String roleName, String fullName, String scope, List<Metric> metrics,
                             List<Breakdown> breakdowns, List<ApprovalActivity> activities,
                             List<Unavailable> unavailable, List<Shortcut> shortcuts,
                             List<AccountSummary> accountSummaries, CandidatePanel candidate) {
        this(roleName, fullName, scope, metrics, breakdowns, activities, unavailable, shortcuts,
                accountSummaries, candidate, null);
    }

    public DashboardResponse(String roleName, String fullName, String scope, List<Metric> metrics,
                         List<Breakdown> breakdowns, List<ApprovalActivity> activities,
                         List<Unavailable> unavailable, List<Shortcut> shortcuts,
                         List<AccountSummary> accountSummaries) {
        this(roleName, fullName, scope, metrics, breakdowns, activities, unavailable, shortcuts,
                accountSummaries, null);
    }

    public DashboardResponse(String roleName, String fullName, String scope, List<Metric> metrics,
                         List<Breakdown> breakdowns, List<ApprovalActivity> activities,
                         List<Unavailable> unavailable, List<Shortcut> shortcuts) {
        this(roleName, fullName, scope, metrics, breakdowns, activities, unavailable, shortcuts, List.of(), null);
    }

    public record AccountSummary(String title, String url, long total, long active, long inactive, long blocked) { }

    public record DepartmentSummary(long total, long active, long inactive) { }

    public record Metric(String title, long value, String detail) { }

    public record Breakdown(String title, List<StatusCount> statuses) { }

    public record Unavailable(String title, String reason) { }

    public record Shortcut(String title, String url) { }

    /** Candidate-only widgets. Contains only data a candidate may see (GBR-02). */
    public record CandidatePanel(List<ApplicationItem> recentApplications,
                                 List<InterviewItem> upcomingInterviews,
                                 List<OfferItem> offers,
                                 CandidateProfile profile) { }

    /** Stage is not inferred: the schema only stores ApplicationStatus. Status is a UI label. */
    public record ApplicationItem(String jobTitle, LocalDate appliedDate, String stage, String status) { }

    /** {@code meetingUrl} is set only for online interviews with an http(s) link. */
    public record InterviewItem(String jobTitle, LocalDateTime startTime, LocalDateTime endTime,
                                String formatLabel, String meetingUrl, String location) { }

    public record OfferItem(String jobTitle, BigDecimal salary, LocalDate startDate, String statusLabel,
                            BigDecimal probationSalary, String workLocation, String benefits) { }

    /** Only the authenticated account's public profile fields, never credentials or internal notes. */
    public record CandidateProfile(String fullName, String email, String departmentName) { }
}

