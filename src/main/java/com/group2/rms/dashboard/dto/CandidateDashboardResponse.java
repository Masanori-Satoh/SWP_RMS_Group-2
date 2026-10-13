package com.group2.rms.dashboard.dto;

import com.group2.rms.dashboard.DashboardMetricsRepository.StatusCount;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CandidateDashboardResponse(
        String roleName,
        String fullName,
        String scope,
        List<DashboardMetricResponse> metrics,
        List<StatusCount> applicationStatuses,
        List<StatusCount> offerStatuses,
        List<ApplicationItem> recentApplications,
        List<InterviewItem> upcomingInterviews,
        List<OfferItem> offers,
        CandidateProfile profile) implements DashboardResponse {
    public record ApplicationItem(
            String jobTitle,
            LocalDate appliedDate,
            String stage,
            String status) {
    }

    public record InterviewItem(
            String jobTitle,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String formatLabel,
            String meetingUrl,
            String location) {
    }

    public record OfferItem(
            String jobTitle,
            BigDecimal salary,
            LocalDate startDate,
            String statusLabel,
            BigDecimal probationSalary,
            String workLocation,
            String benefits) {
    }

    public record CandidateProfile(
            String fullName,
            String email,
            String departmentName) {
    }
}
