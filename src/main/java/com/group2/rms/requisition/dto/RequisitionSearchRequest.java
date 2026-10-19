package com.group2.rms.requisition.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Mọi điều kiện tìm kiếm của màn "Yêu cầu tuyển dụng", lấy từ URL.
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
