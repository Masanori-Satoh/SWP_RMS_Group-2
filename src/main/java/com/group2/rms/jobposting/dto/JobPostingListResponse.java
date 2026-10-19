package com.group2.rms.jobposting.dto;

import com.group2.rms.core.dto.FilterChip;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Toàn bộ dữ liệu màn "Tin tuyển dụng" cần, service chuẩn bị sẵn để controller đưa nguyên khối ra view.
 */
public record JobPostingListResponse(
                Page<InternalJobPostingResponse> page,
                JobPostingSearchRequest filter,
                JobPostingFilterOptions options,
                List<FilterChip> chips,
                boolean canManage) {
}
