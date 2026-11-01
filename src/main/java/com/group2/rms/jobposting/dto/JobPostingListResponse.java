package com.group2.rms.jobposting.dto;

import com.group2.rms.core.dto.FilterChip;
import org.springframework.data.domain.Page;

import java.util.List;

/*
 * DTO: toàn bộ dữ liệu màn danh sách tin (trang kết quả, bộ lọc đã làm sạch, ô lọc, chip).
 */
public record JobPostingListResponse(
                Page<InternalJobPostingResponse> page,
                JobPostingSearchRequest filter,
                JobPostingFilterOptions options,
                List<FilterChip> chips,
                boolean canManage) {
}
