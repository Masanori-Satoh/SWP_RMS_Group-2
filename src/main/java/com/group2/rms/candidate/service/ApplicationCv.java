package com.group2.rms.candidate.service;

import org.springframework.core.io.Resource;

/**
 * CV của một đơn: hoặc file đọc từ {@link CvStorage} (đơn nộp qua web), hoặc đường dẫn để chuyển hướng
 * (CV mẫu {@code /samples/cv/...} của dữ liệu seed, dịch vụ lưu file ngoài sau này). Đúng một trong hai khác null.
 */
public record ApplicationCv(Resource file, String redirectUrl) {

    public static ApplicationCv file(Resource file) {
        return new ApplicationCv(file, null);
    }

    public static ApplicationCv redirect(String url) {
        return new ApplicationCv(null, url);
    }
}
