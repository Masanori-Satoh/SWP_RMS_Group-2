package com.group2.rms.candidate.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Nơi lưu file CV của đơn ứng tuyển. Bản hiện tại: {@link LocalCvStorage}; dịch vụ ngoài (Cloudinary) thêm bản mới sau.
 * Lưu file nằm ngoài transaction của DB: nơi gọi phải tự xóa file nếu lưu đơn thất bại.
 */
public interface CvStorage {

    /** Lưu file đã được kiểm tra hợp lệ; tên file do hệ thống sinh, không dùng tên gốc. */
    StoredFile store(MultipartFile file, Integer candidateId);

    /** Xóa file theo khóa; không ném lỗi nếu file không còn. */
    void delete(String key);

    /** Đọc file theo khóa; không có file thì ném {@code ResourceNotFoundException}. */
    Resource load(String key);
}
