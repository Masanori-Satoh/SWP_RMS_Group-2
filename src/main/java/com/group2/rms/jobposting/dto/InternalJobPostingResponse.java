package com.group2.rms.jobposting.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Một dòng của màn danh sách tin tuyển dụng.
 * Nhãn, màu, số ngày và cờ thao tác đều do JobPostingMapper#toListRow tính sẵn;
 * template chỉ đọc, không tự so sánh trạng thái hay vai trò.
 *
 * @param postingNo            lần đăng thứ mấy của yêu cầu nguồn (PostingNo)
 * @param viewStatus           trạng thái hiển thị: Draft, Published, Paused, Closed
 * @param statusTone           màu badge: success, warning, neutral
 * @param applicationCount     tổng số hồ sơ đã nộp vào tin
 * @param newApplicationCount  số hồ sơ chưa ai xử lý (ApplicationStatus = Applied)
 * @param daysUntilDeadline    số ngày còn lại tới hạn nộp (âm = đã quá hạn); null nếu không có hạn nộp
 * @param deadlineState        "open", "soon" (≤ 7 ngày) hoặc "past" để tô màu cột hạn nộp; null nếu tin đã đóng / không có hạn
 * @param publishable          đăng tin nháp
 * @param reopenable           mở lại tin đang tạm dừng
 * @param closable             đóng tin: chỉ HR (quyết định D7)
 * @param deletable            xóa: chỉ tin nháp chưa có hồ sơ
 * @param applicationsViewable có link "Xem ứng viên"
 */
@Builder
public record InternalJobPostingResponse(
        Integer jobPostingId,
        String postingTitle,
        String departmentName,
        String employmentType,
        LocalDateTime applicationDeadline,
        LocalDateTime postingDate,
        String createdByName,
        Integer postingNo,
        String viewStatus,
        String statusLabel,
        String statusTone,
        long applicationCount,
        long newApplicationCount,
        Long daysUntilDeadline,
        String deadlineState,
        boolean publishable,
        boolean reopenable,
        boolean editable,
        boolean pausable,
        boolean closable,
        boolean deletable,
        boolean applicationsViewable) {
}
