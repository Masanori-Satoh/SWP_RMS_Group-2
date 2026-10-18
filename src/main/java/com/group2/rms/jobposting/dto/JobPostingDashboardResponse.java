package com.group2.rms.jobposting.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bảng điều khiển HR của màn tin tuyển dụng (job-postings/dashboard.html).
 *
 * @param activeCount                 tin đang đăng còn hạn nộp
 * @param draftCount                  tin nháp
 * @param closedCount                 tin đã đóng hoặc đang đăng nhưng đã quá hạn
 * @param requisitionsAwaitingPosting yêu cầu đã duyệt chưa có tin đang mở
 * @param departmentStats             số tin theo phòng ban, nhiều nhất trước
 * @param recentPostings              5 tin mới đăng / mới tạo gần nhất
 */
public record JobPostingDashboardResponse(
        long activeCount,
        long draftCount,
        long closedCount,
        List<AwaitingRequisition> requisitionsAwaitingPosting,
        List<DeptStat> departmentStats,
        List<RecentPosting> recentPostings) {

    public record DeptStat(String deptName, long count, int percentage) {
    }

    public record AwaitingRequisition(Integer requisitionId, String title, String requisitionCode,
            String departmentName, Integer recruitmentRound) {
    }

    /**
     * @param statusLabel nhãn trạng thái (JobPostingLabels.statusLabel)
     * @param badgeClass  class badge của trang dashboard (JobPostingLabels.dashboardBadge)
     * @param editable    hiện nút "Sửa" (tin nháp)
     */
    public record RecentPosting(Integer jobPostingId, String postingTitle, String departmentName,
            String statusLabel, String badgeClass, boolean editable,
            LocalDateTime applicationDeadline, LocalDateTime postingDate) {
    }
}
