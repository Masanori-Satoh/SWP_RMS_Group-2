package com.group2.rms.candidate.dto;

import com.group2.rms.candidate.service.ApplicationStage;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * Dữ liệu cho trang danh sách hồ sơ.
 *
 * @param stageCounts   số hồ sơ mỗi giai đoạn (đủ mọi {@link ApplicationStage}, kể cả 0), cùng phạm vi và bộ lọc
 *                      với danh sách nhưng chưa lọc theo giai đoạn
 * @param forwardedView {@code true} khi người xem là Hiring Manager: chỉ thấy hồ sơ HR đã chuyển (SRS "View CV List")
 * @param viewerRole    tên vai trò người xem, để layout nội bộ chọn đúng sidebar
 */
public record ApplicationListResponse(
        Page<ApplicationPipelineResponse> applications,
        Map<ApplicationStage, Long> stageCounts,
        List<JobPostingOption> jobOptions,
        ApplicationSearch search,
        boolean forwardedView,
        String viewerRole) {

    /** Tổng số hồ sơ mọi giai đoạn (ô "Tất cả"). */
    public long totalCount() {
        return stageCounts.values().stream().mapToLong(Long::longValue).sum();
    }
}
