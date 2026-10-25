package com.group2.rms.candidate.application.pipeline.dto;

import com.group2.rms.candidate.domain.rule.ApplicationStage;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * Dữ liệu cho trang danh sách hồ sơ.
 *
 * @param stageCounts       số hồ sơ mỗi giai đoạn (đủ mọi {@link ApplicationStage}, kể cả 0), cùng phạm vi và bộ
 *                          lọc với danh sách nhưng chưa lọc theo giai đoạn
 * @param postingTitle      tên tin đang lọc ({@code search.jobPostingId}); {@code null} nếu không lọc theo tin
 *                          hoặc tin không tồn tại
 * @param canOpenJobPosting người xem mở được trang tin nội bộ (hiện link quay lại tin)
 * @param departmentView    {@code true} khi người xem là Hiring Manager: thấy mọi hồ sơ của phòng ban mình,
 *                          chỉ duyệt hồ sơ HR đã chuyển
 * @param viewerRole        tên vai trò người xem, để layout nội bộ chọn đúng sidebar
 */
public record ApplicationListResponse(
        Page<ApplicationPipelineResponse> applications,
        Map<ApplicationStage, Long> stageCounts,
        List<JobPostingOption> jobOptions,
        ApplicationSearch search,
        String postingTitle,
        boolean canOpenJobPosting,
        boolean departmentView,
        String viewerRole) {

    /** Tổng số hồ sơ mọi giai đoạn (ô "Tất cả"). */
    public long totalCount() {
        return stageCounts.values().stream().mapToLong(Long::longValue).sum();
    }
}
