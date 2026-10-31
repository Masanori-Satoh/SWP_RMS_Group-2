package com.group2.rms.jobposting.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/*
 * DTO: điều kiện tìm kiếm của màn danh sách tin, lấy từ URL; service làm sạch lại theo quyền.
 */
public record JobPostingSearchRequest(
        String q,
        List<String> status,
        Integer departmentId,
        Integer creatorId,
        String deadline,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate postedFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate postedTo,
        String sort,
        Integer page,
        Integer size) {

    /** Số bộ lọc đang bật, hiện trên nút "Bộ lọc" (không tính từ khóa và sắp xếp). */
    public int activeFilterCount() {
        int count = 0;
        if (status != null && !status.isEmpty()) {
            count++;
        }
        if (departmentId != null) {
            count++;
        }
        if (creatorId != null) {
            count++;
        }
        if (deadline != null && !deadline.isBlank()) {
            count++;
        }
        if (postedFrom != null || postedTo != null) {
            count++;
        }
        return count;
    }

    /** Trạng thái này có đang được chọn không (dùng trong template). */
    public boolean hasStatus(String value) {
        return status != null && status.contains(value);
    }
}
