package com.group2.rms.requisition.dto;

import com.group2.rms.core.dto.FilterChip;
import org.springframework.data.domain.Page;

import java.util.List;

/*
 * DTO: toàn bộ dữ liệu màn danh sách yêu cầu (trang kết quả, bộ lọc đã làm sạch, ô lọc, chip).
 */
public record RequisitionListResponse(
        Page<RequisitionResponse> page,
        RequisitionSearchRequest filter,
        RequisitionFilterOptions options,
        List<FilterChip> chips) {
}
