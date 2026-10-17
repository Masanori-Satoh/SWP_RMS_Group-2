package com.group2.rms.jobposting.dto;

import java.time.LocalDateTime;
import java.util.List;

public record JobPostingDashboardResponse(
        long activeCount,
        long draftCount,
        long closedCount,
        List<AwaitingRequisition> requisitionsAwaitingPosting,
        List<DeptStat> departmentStats,
        List<RecentPosting> recentPostings) {
    public record DeptStat(String deptName, long count, int percentage) {}
    public record AwaitingRequisition(Integer requisitionId, String title, String requisitionCode,
            String departmentName, Integer recruitmentRound) {}
    public record RecentPosting(Integer jobPostingId, String postingTitle, String departmentName,
            String postingStatus, LocalDateTime applicationDeadline, LocalDateTime postingDate) {}
}
