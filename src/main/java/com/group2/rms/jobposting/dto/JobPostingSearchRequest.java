package com.group2.rms.jobposting.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Mọi điều kiện tìm kiếm của màn "Tin tuyển dụng", lấy từ URL.
 * Ví dụ: /internal/job-postings?q=java&status=Published&status=Paused&deadline=soon&sort=apps&page=1
 *
 * Giá trị chưa được kiểm tra: service "làm sạch" lại theo quyền của người xem
 * (xem JobPostingListService#normalizeFilter).
 *
 * @param q            từ khóa tìm theo tiêu đề tin hoặc tên phòng ban
 * @param status       các trạng thái hiển thị được chọn (Draft, Scheduled, Published, Paused, Closed)
 * @param departmentId lọc theo phòng ban
 * @param creatorId    lọc theo người tạo tin
 * @param deadline     nhóm hạn nộp: open, soon, past
 * @param postedFrom   ngày đăng từ (tính cả ngày này)
 * @param postedTo     ngày đăng đến (tính cả ngày này)
 * @param sort         cách sắp xếp, xem JobPostingSort
 * @param page         số trang, đếm từ 0
 * @param size         số dòng mỗi trang
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

    /** Ô checkbox trạng thái có đang được chọn không (dùng trong template). */
    public boolean hasStatus(String value) {
        return status != null && status.contains(value);
    }
}
