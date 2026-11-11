package com.group2.rms.requisition.service;

import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;


public final class RequisitionLabels {


    public static final String POSTING_NONE = "none";


    public static final String POSTING_DRAFT = "Draft";
    public static final String POSTING_PUBLISHED = "Published";
    public static final String POSTING_PAUSED = "Paused";
    public static final String POSTING_CLOSED = "Closed";


    private static final String REQUISITION_CODE_FORMAT = "REQ-%04d";

    private RequisitionLabels() {
    }


    // Returns the display code for the requisition.
    public static String requisitionCode(Integer requisitionId) {
        return String.format(REQUISITION_CODE_FORMAT, requisitionId);
    }


    // Returns the display code for the requisition.
    public static String requisitionCode(Integer requisitionId, String savedCode) {
        if (savedCode == null || savedCode.isBlank()) {
            return requisitionCode(requisitionId);
        }
        return savedCode;
    }


    // Returns the display label for an approval status.
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


    // Returns the visual tone for an approval status.
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


    // Returns the visual tone for a posting status.
    public static String postingTone(String status) {
        if (POSTING_PUBLISHED.equals(status)) {
            return "success";
        }
        if (POSTING_PAUSED.equals(status)) {
            return "warning";
        }
        return "neutral";
    }


    // Returns the display label for an employment type.
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
            return "Tự do";
        }
        return type == null ? "—" : type;
    }


    // Returns the display label for a work model.
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

    /** Lương theo đơn vị triệu: "20 – 30 triệu VND/tháng"; không nhập thì "Thỏa thuận". */
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

    // Formats an amount in millions.
    private static String millions(BigDecimal amount) {
        BigDecimal value = amount.divide(BigDecimal.valueOf(1_000_000), 2, RoundingMode.HALF_UP);
        return value.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    /** Thời gian thử việc (JobRequisition.ProbationPeriod); trống = không thử việc. */
    public static String probation(String period) {
        return period == null || period.isBlank() ? "Không thử việc" : period;
    }


    // Returns the display label for a gender value.
    public static String gender(String gender) {
        if ("Male".equals(gender)) {
            return "Nam";
        }
        if ("Female".equals(gender)) {
            return "Nữ";
        }
        return "Không yêu cầu";
    }


    // Converts plain text to safe multiline HTML.
    public static String multilineHtml(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return HtmlUtils.htmlEscape(text).replace("\r\n", "\n").replace("\n", "<br/>");
    }


    // Returns the display label for a posting status.
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
