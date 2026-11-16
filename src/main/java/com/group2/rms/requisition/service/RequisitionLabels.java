package com.group2.rms.requisition.service;

import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;


public final class RequisitionLabels {


    public static final String POSTING_NONE = "none";


    public static final String POSTING_DRAFT = "Draft";
    public static final String POSTING_PUBLISHED = "Published";
    public static final String POSTING_PAUSED = "Paused";
    public static final String POSTING_CLOSED = "Closed";


    // Ngăn tạo đối tượng chứa hàm tiện ích.
    private RequisitionLabels() {
    }


    // Lấy nhãn trạng thái phê duyệt.
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
        if (RequisitionAccess.STATUS_CLOSED.equals(status)) {
            return "Đã đóng";
        }
        return status;
    }


    // Lấy màu trạng thái phê duyệt.
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


    // Lấy màu trạng thái tin tuyển dụng.
    public static String postingTone(String status) {
        if (POSTING_PUBLISHED.equals(status)) {
            return "success";
        }
        if (POSTING_PAUSED.equals(status)) {
            return "warning";
        }
        return "neutral";
    }


    // Lấy nhãn hình thức làm việc.
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
        if ("Freelance".equals(type)) {
            return "Freelance";
        }
        return type == null ? "—" : type;
    }


    // Lấy nhãn mô hình làm việc.
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

    // Định dạng mức lương theo triệu đồng.
    public static String salary(BigDecimal min, BigDecimal max) {
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

    // Định dạng mức lương cho danh sách.
    public static String salaryForList(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return "Chưa cập nhật";
        }
        if (min != null && max != null) {
            return vnd(min) + " – " + vnd(max);
        }
        return min != null ? "Từ " + vnd(min) : "Đến " + vnd(max);
    }

    // Định dạng mức lương cho chi tiết.
    public static String salaryForDetail(BigDecimal min, BigDecimal max) {
        String salary = salaryForList(min, max);
        return min == null && max == null ? salary : salary + " VND/tháng";
    }

    // Định dạng số tiền theo đồng Việt Nam.
    private static String vnd(BigDecimal amount) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.##", symbols).format(amount);
    }

    // Quy đổi số tiền sang triệu đồng.
    private static String millions(BigDecimal amount) {
        BigDecimal value = amount.divide(BigDecimal.valueOf(1_000_000), 2, RoundingMode.HALF_UP);
        return value.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    // Chuyển văn bản nhiều dòng thành HTML an toàn.
    public static String multilineHtml(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return HtmlUtils.htmlEscape(text).replace("\r\n", "\n").replace("\n", "<br/>");
    }


    // Lấy nhãn trạng thái tin tuyển dụng.
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
