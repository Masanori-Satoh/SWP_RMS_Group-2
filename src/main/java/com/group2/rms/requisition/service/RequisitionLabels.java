package com.group2.rms.requisition.service;

/**
 * Nơi DUY NHẤT dịch các giá trị lưu trong DB (tiếng Anh) sang chữ tiếng Việt cho màn hình Requisition.
 * Muốn đổi cách gọi một trạng thái thì chỉ sửa ở đây, không sửa trong template.
 */
public final class RequisitionLabels {

    /** Giá trị của ô lọc "Tin tuyển dụng" cho yêu cầu chưa có tin nào. */
    public static final String POSTING_NONE = "none";

    /** Các trạng thái của tin tuyển dụng (cột JobPosting.PostingStatus). */
    public static final String POSTING_DRAFT = "Draft";
    public static final String POSTING_PUBLISHED = "Published";
    public static final String POSTING_PAUSED = "Paused";
    public static final String POSTING_CLOSED = "Closed";

    /** Định dạng mã yêu cầu: REQ-0001, REQ-0025... (không kèm năm, theo quyết định của nhóm). */
    private static final String REQUISITION_CODE_FORMAT = "REQ-%04d";

    private RequisitionLabels() {
        // Chỉ chứa hàm static, không tạo đối tượng
    }

    /** Mã yêu cầu dạng REQ-0025: "REQ-" + RequisitionId, đệm cho đủ 4 chữ số. */
    public static String requisitionCode(Integer requisitionId) {
        return String.format(REQUISITION_CODE_FORMAT, requisitionId);
    }

    /** Trạng thái duyệt của yêu cầu (JobRequisition.ApprovalStatus). */
    public static String approvalStatus(String status) {
        if (RequisitionAccess.STATUS_DRAFT.equals(status)) {
            return "Bản nháp";
        }
        if (RequisitionAccess.STATUS_PENDING_DIRECTOR.equals(status)) {
            return "Chờ duyệt";
        }
        if (RequisitionAccess.STATUS_APPROVED.equals(status)) {
            return "Đã duyệt";
        }
        if (RequisitionAccess.STATUS_REJECTED.equals(status)) {
            return "Từ chối";
        }
        return status;
    }

    /**
     * "Tông màu" của badge trạng thái duyệt, khớp với các class trong catalog:
     * badge--success (xanh), badge--warning (vàng), badge--danger (đỏ), badge--neutral (xám).
     * Service chỉ trả về ý nghĩa (success/warning...), template tự ghép thành class "badge--...".
     */
    public static String approvalTone(String status) {
        if (RequisitionAccess.STATUS_APPROVED.equals(status)) {
            return "success";
        }
        if (RequisitionAccess.STATUS_PENDING_DIRECTOR.equals(status)) {
            return "warning";
        }
        if (RequisitionAccess.STATUS_REJECTED.equals(status)) {
            return "danger";
        }
        return "neutral";
    }

    /** Tông màu badge của tin tuyển dụng: đang đăng = xanh, tạm dừng = vàng, còn lại = xám. */
    public static String postingTone(String status) {
        if (POSTING_PUBLISHED.equals(status)) {
            return "success";
        }
        if (POSTING_PAUSED.equals(status)) {
            return "warning";
        }
        return "neutral";
    }

    /** Hình thức làm việc (JobRequisition.EmploymentType). */
    public static String employmentType(String type) {
        if ("Full-time".equals(type)) {
            return "Toàn thời gian";
        }
        if ("Part-time".equals(type)) {
            return "Bán thời gian";
        }
        if ("Internship".equals(type)) {
            return "Thực tập";
        }
        if ("Contract".equals(type)) {
            return "Hợp đồng";
        }
        return type == null ? "—" : type;
    }

    /** Mô hình làm việc (JobRequisition.WorkModel). */
    public static String workModel(String model) {
        if ("On-site".equals(model)) {
            return "Tại văn phòng";
        }
        if ("Hybrid".equals(model)) {
            return "Kết hợp";
        }
        if ("Remote".equals(model)) {
            return "Từ xa";
        }
        return model == null ? "—" : model;
    }

    /** Yêu cầu giới tính (JobRequisition.RequiredGender). */
    public static String gender(String gender) {
        if ("Male".equals(gender)) {
            return "Nam";
        }
        if ("Female".equals(gender)) {
            return "Nữ";
        }
        return "Không yêu cầu";
    }

    /** Loại tuyển dụng (JobRequisition.HiringType), NULL = chưa xác định. */
    public static String hiringType(String type) {
        if ("New".equals(type)) {
            return "Tuyển mới";
        }
        if ("Replacement".equals(type)) {
            return "Tuyển thay thế";
        }
        return "Chưa xác định";
    }

    /** Loại tiêu chí sàng lọc (ScreeningCriteria.CriteriaType). */
    public static String criteriaType(String type) {
        if ("Education".equals(type)) {
            return "Học vấn";
        }
        if ("Experience".equals(type)) {
            return "Kinh nghiệm";
        }
        if ("Skill".equals(type)) {
            return "Kỹ năng";
        }
        if ("Knockout".equals(type)) {
            return "Điều kiện loại trừ";
        }
        return type == null ? "—" : type;
    }

    /**
     * Mức lương từ MinSalary / MaxSalary, đổi sang đơn vị triệu cho dễ đọc.
     * Ví dụ: 20000000 và 30000000 → "20 – 30 triệu VND/tháng"; không nhập gì → "Thỏa thuận".
     */
    public static String salary(java.math.BigDecimal min, java.math.BigDecimal max) {
        if (min == null && max == null) {
            return "Thỏa thuận";
        }
        if (min != null && max != null) {
            return millions(min) + " – " + millions(max) + " triệu VND/tháng";
        }
        if (min != null) {
            return "Từ " + millions(min) + " triệu VND/tháng";
        }
        return "Đến " + millions(max) + " triệu VND/tháng";
    }

    private static String millions(java.math.BigDecimal amount) {
        java.math.BigDecimal value = amount.divide(java.math.BigDecimal.valueOf(1_000_000), 2, java.math.RoundingMode.HALF_UP);
        return value.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    /** Thời gian thử việc (JobRequisition.ProbationDays); NULL = không thử việc. */
    public static String probation(Integer days) {
        return days == null ? "Không thử việc" : days + " ngày";
    }

    /**
     * Văn bản nhiều dòng do người dùng nhập → HTML an toàn để hiển thị bằng th:utext (theo ARCHITECTURE_GUIDE §6.3).
     * BẮT BUỘC escape trước (đổi < > & " thành mã HTML) rồi mới thay xuống dòng bằng <br/>,
     * nếu không người dùng có thể chèn mã độc (XSS) vào nội dung yêu cầu.
     */
    public static String multilineHtml(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return org.springframework.web.util.HtmlUtils.htmlEscape(text).replace("\r\n", "\n").replace("\n", "<br/>");
    }

    /** Trạng thái tin tuyển dụng mới nhất của yêu cầu; null hoặc POSTING_NONE nghĩa là chưa có tin. */
    public static String postingStatus(String status) {
        if (status == null || POSTING_NONE.equals(status)) {
            return "Chưa có tin";
        }
        if (POSTING_DRAFT.equals(status)) {
            return "Tin nháp";
        }
        if (POSTING_PUBLISHED.equals(status)) {
            return "Đang đăng";
        }
        if (POSTING_PAUSED.equals(status)) {
            return "Tạm dừng";
        }
        if (POSTING_CLOSED.equals(status)) {
            return "Đã đóng";
        }
        return status;
    }
}
