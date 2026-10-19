package com.group2.rms.jobposting.dto;

import com.group2.rms.jobposting.validator.JobPostingValidator;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dữ liệu form Tạo / Sửa tin tuyển dụng (job-postings/create.html).
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

    // Chỉ các trường trong whitelist JobPostingController#formBinder được nhận từ form; còn lại service điền từ DB
    private Integer jobPostingId;
    private Integer requisitionId;

    // ----- Thông tin yêu cầu nguồn (chỉ hiển thị, service điền từ DB; server không dùng giá trị gửi lên) -----
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
    /** Thông tin nội bộ: chỉ HR xem, không đưa vào nội dung tin công khai. */
    private String reasonForHiring;
    private Integer probationDays;
    /** Nhãn tiếng Việt, ví dụ "Không yêu cầu". */
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

    // ----- Trạng thái form (service tính, template chỉ đọc; server không dùng giá trị gửi lên) -----
    /** Tạo mới hoặc đang sửa bản nháp: có nút "Lưu nháp" + "Đăng tin". */
    private boolean draftOrNew;
    /** Đang sửa tin tạm dừng: có nút "Lưu thay đổi" + "Lưu và mở lại". */
    private boolean paused;
    /** Đang sửa tin hẹn đăng: "Đăng ngay" nghĩa là đăng khi lưu. */
    private boolean scheduled;
    /** Tin đã lên trang tuyển dụng (đang đăng hoặc tạm dừng): không đổi được ngày đăng nữa. */
    private boolean live;

    // ----- Nội dung tin (HR nhập) -----
    // Ô nhiều dòng không đặt @Size: trình duyệt gửi xuống dòng là \r\n (2 ký tự), validator đếm sau khi đổi về \n
    @Size(max = JobPostingValidator.TITLE_LIMIT, message = JobPostingValidator.TITLE_TOO_LONG)
    private String postingTitle;
    private String jobDescription;
    private String jobRequirements;
    private String benefits;
    @Size(max = JobPostingValidator.LOCATION_LIMIT, message = JobPostingValidator.LOCATION_TOO_LONG)
    private String workLocation;

    /** "range" (theo yêu cầu), "nego" (thỏa thuận) hoặc "custom" (tự nhập). */
    private String salaryMode;
    /** Mức lương tự nhập khi salaryMode = custom. */
    @Size(max = JobPostingValidator.SALARY_TEXT_LIMIT, message = JobPostingValidator.SALARY_TEXT_TOO_LONG)
    private String salaryCustom;

    /** Có công khai số lượng tuyển không; tắt thì lưu NumberOfPositionsDisplay = NULL. */
    private Boolean showPositions;
    @Size(max = JobPostingValidator.POSITIONS_TEXT_LIMIT, message = JobPostingValidator.POSITIONS_TEXT_TOO_LONG)
    private String numberOfPositionsDisplay;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate applicationDeadline;

    /** "now" (đăng ngay) hoặc "later" (hẹn ngày đăng). */
    private String publishMode;
    /** Thời điểm hẹn đăng (ô datetime-local gửi dạng 2026-10-20T08:00). */
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledAt;

    /** JobPostingValidator.ACTION_DRAFT (lưu nháp / lưu thay đổi) hoặc ACTION_PUBLISH (đăng tin / lên lịch đăng). */
    private String action;
}
