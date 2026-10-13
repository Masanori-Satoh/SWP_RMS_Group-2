package com.group2.rms.core.web;

/**
 * Một ô trên thanh giai đoạn, hiển thị bằng fragment {@code fragments/ui/stages :: stageBar(items, mode)}.
 * Chỉ là dữ liệu hiển thị: mỗi module tự dựng danh sách ô (nhãn, số đếm, link) từ nghiệp vụ của mình.
 *
 * @param key     mã ô (ví dụ {@code INTERVIEW}), không hiển thị
 * @param count   số đếm cạnh nhãn; {@code null} thì không hiện
 * @param href    đường dẫn trong ứng dụng (bắt đầu bằng {@code /}, fragment tự thêm context path);
 *                {@code null} thì ô không bấm được
 * @param state   {@code current} (ô đang lọc / bước hiện tại), {@code done} (bước đã qua), {@code upcoming} (còn lại)
 * @param detail  nhãn chi tiết dưới bước hiện tại; {@code null} nếu không có
 * @param offFlow ô ngoài luồng (ví dụ "Tất cả", "Đã dừng"): tách khỏi các ô trong luồng bằng vạch ngăn
 */
public record StageItem(String key, String label, Integer count, String href, String state, String detail,
                        boolean offFlow) {
}
