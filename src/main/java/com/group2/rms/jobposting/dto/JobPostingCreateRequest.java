package com.group2.rms.jobposting.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dữ liệu form Tạo / Sửa tin tuyển dụng (job-postings/create.html).
 *
 * Chỉ các trường trong whitelist của JobPostingController#binder được nhận từ form
 * (requisitionId, nội dung tin, cách hiển thị lương / số lượng, hạn nộp, cách đăng, action).
 * Các trường còn lại là thông tin yêu cầu nguồn để HIỂN THỊ, luôn do service điền lại từ DB
 * (người dùng sửa trên trình duyệt cũng không có tác dụng).
 * Mọi quy tắc kiểm tra nằm ở JobPostingValidator#validateForm.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPostingCreateRequest {

    // Lựa chọn "Mức lương hiển thị"
    public static final String SALARY_RANGE = "range";
    public static final String SALARY_NEGOTIABLE = "nego";
    public static final String SALARY_CUSTOM = "custom";
    // Lựa chọn "Ngày đăng"
    public static final String PUBLISH_NOW = "now";
    public static final String PUBLISH_LATER = "later";

    private Integer jobPostingId;
    private String postingStatus;
    private Integer requisitionId;

    // ----- Thông tin yêu cầu nguồn (chỉ hiển thị, service điền từ DB) -----
    private String requisitionCode;
    private String requisitionTitle;
    private String departmentName;
    private Integer recruitmentRound;
    /** Nhãn tiếng Việt, ví dụ "Toàn thời gian". */
    private String employmentType;
    /** Nhãn tiếng Việt, ví dụ "Kết hợp". */
    private String workModel;
    private Integer numberOfPositions;
    private String hiringManagerName;
    /** Thông tin nội bộ: chỉ HR xem, không đưa vào nội dung tin công khai (rule §24.2). */
    private String reasonForHiring;
    private java.math.BigDecimal minSalary;
    private java.math.BigDecimal maxSalary;
    private Integer probationDays;
    private String gender;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expectedStartDate;
    /** Tin này là lần đăng thứ mấy của yêu cầu. */
    private Integer postingNo;
    /** Mức lương theo yêu cầu, ví dụ "20 – 30 triệu VND/tháng" (lựa chọn "Theo yêu cầu"). */
    private String salaryRangeLabel;
    // Giá trị gốc từ yêu cầu, cho nút "Khôi phục bản gốc"
    private String sourceWorkLocation;
    private String sourceJobDescription;
    private String sourceRequirements;
    /** Tin đã lên trang tuyển dụng (đang đăng hoặc tạm dừng): không đổi được ngày đăng nữa. */
    private boolean live;

    // ----- Nội dung tin (HR nhập) -----
    private String postingTitle;
    private String jobDescription;
    private String jobRequirements;
    private String benefits;
    private String workLocation;

    /** "range" (theo yêu cầu), "nego" (thỏa thuận) hoặc "custom" (tự nhập). */
    private String salaryMode;
    /** Mức lương tự nhập khi salaryMode = custom, tối đa 100 ký tự. */
    private String salaryCustom;
    /** Chuỗi lương được lưu vào cột SalaryDisplay (service tính từ salaryMode). */
    private String salaryDisplay;

    /** Có công khai số lượng tuyển không; tắt thì lưu NumberOfPositionsDisplay = NULL. */
    private Boolean showPositions;
    private String numberOfPositionsDisplay;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate applicationDeadline;

    /** "now" (đăng ngay) hoặc "later" (hẹn ngày đăng). */
    private String publishMode;
    /** Thời điểm hẹn đăng (ô datetime-local gửi dạng 2026-10-20T08:00). */
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledAt;

    /** "draft" (lưu nháp / lưu thay đổi) hoặc "publish" (đăng tin / lên lịch đăng). */
    private String action;
}
