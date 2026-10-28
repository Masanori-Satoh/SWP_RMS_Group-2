package com.group2.rms.dashboard.internal.dto;

/*
 * DTO: một thanh ngang của biểu đồ; max = số lớn nhất trong cùng biểu đồ để vẽ độ dài thanh bằng thẻ <progress>.
 * note: chữ phụ cạnh con số (ví dụ "3 quá hạn"), null = không có; noteTone tô màu chữ phụ.
 * hint: câu giải thích hiện khi rê chuột (thuộc tính title), gồm số lượng và phạm vi dữ liệu.
 */
public record DashboardBarResponse(
        String label,
        long count,
        long max,
        String href,
        String note,
        String noteTone,
        String hint) {
}
