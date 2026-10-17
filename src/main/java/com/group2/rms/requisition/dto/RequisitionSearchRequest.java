package com.group2.rms.requisition.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Mọi điều kiện tìm kiếm của màn "Yêu cầu tuyển dụng", lấy từ URL.
 * Ví dụ: /requisitions?q=java&status=Approved&status=Rejected&sort=oldest&page=1
 *
 * Spring tự tạo record này từ query string (controller dùng @ModelAttribute).
 * Giá trị chưa được kiểm tra: service sẽ "làm sạch" lại theo quyền của người xem
 * (xem RequisitionServiceImpl#normalizeFilter).
 *
 * @param q            từ khóa tìm theo tên vị trí hoặc phòng ban
 * @param status       các trạng thái duyệt được chọn (chọn được nhiều).
 *                     null = URL không có tham số status (lần vào đầu);
 *                     danh sách rỗng = người dùng chủ động bỏ hết trạng thái
 * @param posting      trạng thái tin tuyển dụng mới nhất: "none", "Published", "Paused", "Closed"
 * @param departmentId lọc theo phòng ban
 * @param creatorId    lọc theo người tạo yêu cầu (Hiring Manager)
 * @param createdFrom  ngày tạo từ (tính cả ngày này)
 * @param createdTo    ngày tạo đến (tính cả ngày này)
 * @param sort         cách sắp xếp, xem RequisitionSort
 * @param page         số trang, đếm từ 0 (giống Spring Data)
 * @param size         số dòng mỗi trang
 */
public record RequisitionSearchRequest(
        String q,
        List<String> status,
        String posting,
        Integer departmentId,
        Integer creatorId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
        String sort,
        Integer page,
        Integer size) {

    /** Đếm số bộ lọc đang bật để hiện con số trên nút "Bộ lọc" (không tính từ khóa và sắp xếp). */
    public int activeFilterCount() {
        int count = 0;
        if (status != null && !status.isEmpty()) {
            count++;
        }
        if (posting != null && !posting.isBlank()) {
            count++;
        }
        if (departmentId != null) {
            count++;
        }
        if (creatorId != null) {
            count++;
        }
        if (createdFrom != null || createdTo != null) {
            count++;
        }
        return count;
    }

    /** Ô checkbox trạng thái có đang được chọn không (dùng trong template). */
    public boolean hasStatus(String value) {
        return status != null && status.contains(value);
    }
}
