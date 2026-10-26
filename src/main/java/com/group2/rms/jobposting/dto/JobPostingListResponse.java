package com.group2.rms.jobposting.dto;

import com.group2.rms.core.dto.FilterChip;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Toàn bộ dữ liệu màn "Tin tuyển dụng" cần, service chuẩn bị sẵn để controller
 * đưa nguyên khối ra view.
 *
 * @param page      trang kết quả (dùng với fragment fragments/ui/pagination ::
 *                  paged)
 * @param filter    bộ lọc ĐÃ làm sạch theo quyền (giữ lựa chọn trên form)
 * @param options   ô lọc nào được hiện và các lựa chọn của từng ô
 * @param chips     các bộ lọc đang bật, hiển thị thành chip có nút ✕ (dùng
 *                  chung kiểu chip với màn yêu cầu)
 * @param canManage người xem được tạo / sửa / đăng tin không (HR)
 */
public record JobPostingListResponse(
                Page<InternalJobPostingResponse> page,
                JobPostingSearchRequest filter,
                JobPostingFilterOptions options,
                List<FilterChip> chips,
                boolean canManage) {
}
