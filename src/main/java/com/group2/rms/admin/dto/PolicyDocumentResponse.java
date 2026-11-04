package com.group2.rms.admin.dto;

import java.time.LocalDateTime;

/**
 * Một dòng trên màn Tài liệu chính sách.
 *
 * @param code       mã trên URL tải lên ("terms", "privacy")
 * @param title      tên tài liệu tiếng Việt
 * @param publicPath đường dẫn ứng viên mở ("/terms")
 * @param fileName   tên file cố định
 * @param uploaded   true = Admin đã tải bản riêng; false = đang dùng bản mặc định
 * @param sizeLabel  kích thước dễ đọc ("312 KB")
 * @param updatedAt  lần thay gần nhất; null = chưa tải bản nào
 * @param updatedBy  tên người thay gần nhất; null = chưa tải bản nào
 */
public record PolicyDocumentResponse(String code, String title, String publicPath, String fileName,
                                     boolean uploaded, String sizeLabel,
                                     LocalDateTime updatedAt, String updatedBy) {
}
