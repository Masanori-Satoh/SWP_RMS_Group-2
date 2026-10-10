package com.group2.rms.candidate.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Dữ liệu cho trang danh sách hồ sơ.
 *
 * @param forwardedView {@code true} khi người xem là Hiring Manager: chỉ thấy hồ sơ HR đã chuyển (SRS "View CV List")
 */
public record ApplicationListResponse(
        Page<ApplicationPipelineResponse> applications,
        List<JobPostingOption> jobOptions,
        ApplicationSearch search,
        boolean forwardedView) {
}
