package com.group2.rms.dashboard.dto;

public interface DashboardResponse {
    String roleName();
    String fullName();

    /**
     * Tên file trong templates/dashboard/sections/ của dashboard theo role mới; null = trang dùng khung cũ.
     * Nhờ vậy template không phải so sánh tên role để chọn nội dung.
     */
    default String section() {
        return null;
    }
}
