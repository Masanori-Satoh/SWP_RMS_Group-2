package com.group2.rms.dashboard.internal.dto;

/*
 * DTO: một cột dọc của biểu đồ xu hướng theo tuần.
 * level = chiều cao cột theo bậc 0..10 (10 = cột cao nhất); CSS vẽ bằng class dash-col-h{level}
 * vì template không được dùng style inline. hint = câu giải thích khi rê chuột (title).
 */
public record DashboardColumnResponse(
        String label,
        long count,
        int level,
        boolean current,
        String hint) {
}
