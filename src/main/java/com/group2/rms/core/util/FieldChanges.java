package com.group2.rms.core.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * So sánh hai "ảnh chụp" (snapshot) của một bản ghi trước và sau khi sửa, để ghi audit log.
 *
 * Snapshot là Map "tên trường hiển thị" → "giá trị dạng chữ", ví dụ {"Số lượng cần tuyển": "2"}.
 * Cách dùng:
 * <pre>
 *   Map&lt;String, String&gt; before = snapshot(entity);
 *   ... sửa entity ...
 *   Map&lt;String, String&gt; after = snapshot(entity);
 *   List&lt;String&gt; changed = FieldChanges.changedKeys(before, after);
 *   if (!changed.isEmpty()) {
 *       oldValue = FieldChanges.describeBefore(changed, before);         // "Số lượng cần tuyển: 2"
 *       newValue = FieldChanges.describeChange(changed, before, after);  // "Số lượng cần tuyển: 2 → 3"
 *   }
 * </pre>
 */
public final class FieldChanges {

    /** Chữ hiển thị khi trường không có giá trị. */
    public static final String EMPTY_VALUE = "Chưa xác định";

    private FieldChanges() {
        // Chỉ chứa hàm static, không tạo đối tượng
    }

    /** Tên các trường có giá trị khác nhau giữa trước và sau (giữ thứ tự của snapshot). */
    public static List<String> changedKeys(Map<String, String> before, Map<String, String> after) {
        Set<String> allKeys = new LinkedHashSet<>(before.keySet());
        allKeys.addAll(after.keySet());

        List<String> changed = new ArrayList<>();
        for (String key : allKeys) {
            if (!Objects.equals(before.get(key), after.get(key))) {
                changed.add(key);
            }
        }
        return changed;
    }

    /** Mỗi dòng "tên: giá trị cũ", dùng cho cột OldValue của audit log. */
    public static String describeBefore(List<String> keys, Map<String, String> before) {
        List<String> lines = new ArrayList<>();
        for (String key : keys) {
            lines.add(key + ": " + display(before.get(key)));
        }
        return String.join("\n", lines);
    }

    /** Mỗi dòng "tên: giá trị cũ → giá trị mới", dùng cho cột NewValue của audit log. */
    public static String describeChange(List<String> keys, Map<String, String> before, Map<String, String> after) {
        List<String> lines = new ArrayList<>();
        for (String key : keys) {
            lines.add(key + ": " + display(before.get(key)) + " → " + display(after.get(key)));
        }
        return String.join("\n", lines);
    }

    /** null thành "Chưa xác định", còn lại đổi sang chữ. */
    public static String display(Object value) {
        if (value == null) {
            return EMPTY_VALUE;
        }
        return value.toString();
    }
}
