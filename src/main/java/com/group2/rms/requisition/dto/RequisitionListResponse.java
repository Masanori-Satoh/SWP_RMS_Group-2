package com.group2.rms.requisition.dto;

import com.group2.rms.core.dto.FilterChip;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Toàn bộ dữ liệu màn "Danh sách yêu cầu tuyển dụng" cần, do service chuẩn bị sẵn để controller chỉ việc đưa nguyên khối ra view.
 */
public record RequisitionListResponse(
        Page<RequisitionResponse> page,
        RequisitionSearchRequest filter,
        RequisitionFilterOptions options,
        List<FilterChip> chips) {
}
