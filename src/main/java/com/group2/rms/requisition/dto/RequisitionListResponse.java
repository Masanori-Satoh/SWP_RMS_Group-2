package com.group2.rms.requisition.dto;

import com.group2.rms.core.dto.FilterChip;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Toàn bộ dữ liệu màn "Danh sách yêu cầu tuyển dụng" cần, do service chuẩn bị sẵn
 * để controller chỉ việc đưa nguyên khối ra view.
 *
 * @param page    trang kết quả hiện tại (dùng với fragment fragments/ui/pagination :: paged)
 * @param filter  bộ lọc ĐÃ làm sạch theo quyền (giá trị thật đang áp dụng, dùng để giữ lựa chọn trên form)
 * @param options ô lọc nào được hiện và các lựa chọn của từng ô
 * @param chips   các bộ lọc đang bật, hiển thị thành chip có nút ✕
 */
public record RequisitionListResponse(
        Page<RequisitionResponse> page,
        RequisitionSearchRequest filter,
        RequisitionFilterOptions options,
        List<FilterChip> chips) {
}
