package com.group2.rms.admin.service;

import java.util.Optional;

/*
 * Hai tài liệu chính sách ứng viên đọc: tên hiển thị, đường dẫn công khai, tên file cố định.
 * Nguồn duy nhất cho các giá trị này (màn Admin, /terms, /privacy-policy, popup đồng ý).
 */
public enum PolicyDocument {
    TERMS("terms", "Điều khoản sử dụng", "/terms", "terms.pdf"),
    PRIVACY("privacy", "Chính sách bảo mật", "/privacy-policy", "privacy.pdf");

    private final String code;
    private final String title;
    private final String publicPath;
    private final String fileName;

    PolicyDocument(String code, String title, String publicPath, String fileName) {
        this.code = code;
        this.title = title;
        this.publicPath = publicPath;
        this.fileName = fileName;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public String publicPath() {
        return publicPath;
    }

    public String fileName() {
        return fileName;
    }

    /** Tìm theo mã trên URL ("terms", "privacy"); mã lạ → rỗng để controller trả 404. */
    public static Optional<PolicyDocument> fromCode(String code) {
        for (PolicyDocument document : values()) {
            if (document.code.equals(code)) {
                return Optional.of(document);
            }
        }
        return Optional.empty();
    }
}
