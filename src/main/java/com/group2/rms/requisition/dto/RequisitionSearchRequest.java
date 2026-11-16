package com.group2.rms.requisition.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/*
 * DTO: điều kiện tìm kiếm của màn danh sách yêu cầu, lấy từ URL; service làm sạch lại theo quyền.
 */
public record RequisitionSearchRequest(
        String q,
        // null = lần vào đầu (chưa có tham số status); rỗng = người dùng bỏ hết trạng thái
        List<String> status,
        String posting,
        Integer departmentId,
        Integer creatorId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
        String sort,
        Integer page,
        Integer size,
        String employmentType) {

    // Đếm số bộ lọc đang bật.
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
        if (employmentType != null && !employmentType.isBlank()) {
            count++;
        }
        if (createdFrom != null || createdTo != null) {
            count++;
        }
        return count;
    }

    // Kiểm tra trạng thái đang được chọn.
    public boolean hasStatus(String value) {
        return status != null && status.contains(value);
    }
}
