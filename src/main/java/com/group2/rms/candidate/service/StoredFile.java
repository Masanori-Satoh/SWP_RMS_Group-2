package com.group2.rms.candidate.service;

/**
 * Kết quả lưu một file CV.
 *
 * @param key khóa nội bộ để xóa/đọc lại file qua {@link CvStorage}
 * @param url giá trị lưu vào {@code Application.AppliedCvUrl}
 */
public record StoredFile(String key, String url) {
}
