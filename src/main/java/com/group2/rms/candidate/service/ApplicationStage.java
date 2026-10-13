package com.group2.rms.candidate.service;

/**
 * Giai đoạn lớn của hồ sơ, dùng chung cho danh sách, chi tiết và (sau này) thanh giai đoạn.
 * Thứ tự khai báo là thứ tự luồng tuyển dụng; {@link #CLOSED} nằm ngoài luồng, hiển thị tách riêng.
 */
public enum ApplicationStage {
    APPLIED("Ứng tuyển"),
    SCREENING("Sàng lọc CV"),
    INTERVIEW("Phỏng vấn"),
    OFFER("Offer"),
    HIRED("Nhận việc"),
    CLOSED("Đã dừng");

    private final String label;

    ApplicationStage(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
