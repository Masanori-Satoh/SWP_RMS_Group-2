package com.group2.rms.dashboard.internal.dto;

/*
 * DTO: phần chung của dashboard mọi role nội bộ. section() là tên file trong templates/dashboard/sections/
 * để template không phải so sánh tên role khi chọn nội dung.
 */
public interface DashboardResponse {
    String roleName();

    String fullName();

    String section();

    /** true = trang có biểu đồ, cần nạp Chart.js và internal-dashboard.js. */
    default boolean hasCharts() {
        return true;
    }
}
