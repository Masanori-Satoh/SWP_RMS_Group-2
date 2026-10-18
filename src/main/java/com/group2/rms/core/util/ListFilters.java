package com.group2.rms.core.util;

import com.group2.rms.core.dto.FilterOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Các hàm nhỏ dùng chung khi "làm sạch" bộ lọc lấy từ URL ở màn danh sách.
 *
 * Nguyên tắc: không tin dữ liệu trên URL. Giá trị nào không nằm trong danh sách lựa chọn
 * thì bỏ qua (coi như không lọc) thay vì báo lỗi.
 */
public final class ListFilters {

    /** Từ khóa tìm kiếm dài hơn thế này bị cắt bớt, tránh câu LIKE quá dài. */
    public static final int MAX_KEYWORD_LENGTH = 120;

    private ListFilters() {
        // Chỉ chứa hàm static, không tạo đối tượng
    }

    /** Từ khóa trên URL: bỏ khoảng trắng thừa, null thành "", cắt bớt nếu dài quá MAX_KEYWORD_LENGTH. */
    public static String cleanKeyword(String raw) {
        if (raw == null) {
            return "";
        }
        String keyword = raw.trim();
        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
        }
        return keyword;
    }

    /** Giữ lại các giá trị nằm trong danh sách lựa chọn, bỏ giá trị lạ và giá trị trùng. */
    public static List<String> keepAllowedValues(List<String> values, List<FilterOption> allowed) {
        List<String> kept = new ArrayList<>();
        if (values == null) {
            return kept;
        }
        for (String value : values) {
            if (isAllowedValue(value, allowed) && !kept.contains(value)) {
                kept.add(value);
            }
        }
        return kept;
    }

    /** Giá trị (chữ hoặc số) có nằm trong danh sách lựa chọn không. */
    public static boolean isAllowedValue(Object value, List<FilterOption> allowed) {
        if (value == null) {
            return false;
        }
        for (FilterOption option : allowed) {
            if (option.value().equals(String.valueOf(value))) {
                return true;
            }
        }
        return false;
    }

    /** Chữ hiển thị của một giá trị; không tìm thấy thì trả về chính giá trị đó. */
    public static String labelOf(Object value, List<FilterOption> options) {
        for (FilterOption option : options) {
            if (option.value().equals(String.valueOf(value))) {
                return option.label();
            }
        }
        return String.valueOf(value);
    }

    /**
     * Mẫu LIKE "có chứa từ khóa", không phân biệt hoa thường: "Java" → "%java%".
     * Dùng kèm ký tự escape '\\': {@code cb.like(cb.lower(field), containsPattern(keyword), '\\')}.
     */
    public static String containsPattern(String keyword) {
        return "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%";
    }

    /**
     * Trong lệnh LIKE của SQL Server, các ký tự % _ [ có nghĩa đặc biệt.
     * Thêm dấu \ phía trước để chúng được hiểu đúng là chữ người dùng gõ (ví dụ tìm "50%").
     */
    public static String escapeLike(String text) {
        return text.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .replace("[", "\\[");
    }
}
