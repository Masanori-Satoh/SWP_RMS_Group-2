package com.group2.rms.admin.repository;

import com.group2.rms.admin.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/*
 * Đọc bảng SystemConfig (key-value). Khóa chính là ConfigKey nên findById(key) trả đúng một dòng.
 * Dùng ở: chatbot (FAQ, thông tin liên hệ HR, bật/tắt chatbot).
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
}
