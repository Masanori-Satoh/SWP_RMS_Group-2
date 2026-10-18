package com.group2.rms.jobposting.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Một dòng của màn danh sách tin tuyển dụng.
 * Nhãn, màu, số ngày và cờ thao tác đều do service tính sẵn (JobPostingServiceImpl#toListRow);
 * template chỉ đọc, không tự so sánh trạng thái hay vai trò.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalJobPostingResponse {

    private Integer jobPostingId;
    private String postingTitle;
    private Integer requisitionId;
    private String requisitionCode;
    private Integer departmentId;
    private String departmentName;
    private Integer recruitmentRound;
    private String employmentType;
    private String postingStatus;
    private LocalDateTime applicationDeadline;
    private LocalDateTime postingDate;
    private LocalDateTime createdAt;
    private String createdByName;

    // ----- Hiển thị (tính ở service) -----
    /** Lần đăng thứ mấy của yêu cầu nguồn (PostingNo). */
    private Integer postingNo;
    /** Trạng thái hiển thị: Draft, Scheduled (hẹn đăng), Published, Paused, Closed. */
    private String viewStatus;
    private String statusLabel;
    /** Màu badge: success, warning, neutral. */
    private String statusTone;
    /** Tổng số hồ sơ đã nộp vào tin. */
    private long applicationCount;
    /** Số hồ sơ chưa ai xử lý (ApplicationStatus = Applied). */
    private long newApplicationCount;
    /** Tin hẹn đăng: còn bao nhiêu ngày nữa thì đăng. */
    private Long daysUntilPosting;
    /** Số ngày còn lại tới hạn nộp (âm = đã quá hạn); null nếu không có hạn nộp. */
    private Long daysUntilDeadline;
    /** "open", "soon" (≤ 7 ngày) hoặc "past" để tô màu cột hạn nộp; null nếu tin đã đóng hoặc không có hạn nộp. */
    private String deadlineState;

    // ----- Cờ thao tác (menu ⋯) -----
    /** Đăng tin nháp. */
    private boolean publishable;
    /** Mở lại tin đang tạm dừng. */
    private boolean reopenable;
    private boolean editable;
    private boolean pausable;
    /** Đóng tin: chỉ HR (quyết định D7). */
    private boolean closable;
    /** Xóa: chỉ tin nháp chưa có hồ sơ. */
    private boolean deletable;
    /** Có link "Xem ứng viên". */
    private boolean applicationsViewable;

    public boolean isExpired() {
        return applicationDeadline != null && applicationDeadline.isBefore(LocalDateTime.now());
    }
}
