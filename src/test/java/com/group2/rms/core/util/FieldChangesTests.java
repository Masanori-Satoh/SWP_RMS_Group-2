package com.group2.rms.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/*
 * UNIT TEST: FieldChanges (so sánh snapshot trước / sau để ghi audit log) và FilterLinks.
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện.
 */
class FieldChangesTests {

    private Map<String, String> snapshot(String positions, String location) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Số lượng cần tuyển", positions);
        map.put("Địa điểm làm việc", location);
        return map;
    }

    @Test
    @DisplayName("Không đổi gì -> Danh sách trường thay đổi rỗng")
    void changedKeys_shouldBeEmpty_whenSnapshotsAreEqual() {
        assertTrue(FieldChanges.changedKeys(snapshot("2", "Hà Nội"), snapshot("2", "Hà Nội")).isEmpty());
    }

    @Test
    @DisplayName("Đổi số lượng, xóa địa điểm -> Ghi 'cũ → mới', giá trị trống hiện 'Chưa xác định'")
    void describeChange_shouldListOldAndNewValues_whenFieldsChanged() {
        // Arrange
        Map<String, String> before = snapshot("2", "Hà Nội");
        Map<String, String> after = snapshot("3", null);

        // Act
        List<String> changed = FieldChanges.changedKeys(before, after);

        // Assert
        assertEquals(List.of("Số lượng cần tuyển", "Địa điểm làm việc"), changed);
        assertEquals("Số lượng cần tuyển: 2\nĐịa điểm làm việc: Hà Nội", FieldChanges.describeBefore(changed, before));
        assertEquals("Số lượng cần tuyển: 2 → 3\nĐịa điểm làm việc: Hà Nội → Chưa xác định",
                FieldChanges.describeChange(changed, before, after));
    }

    @Test
    @DisplayName("Thêm tiêu chí mới (key chỉ có ở 'sau') -> Vẫn tính là thay đổi")
    void changedKeys_shouldIncludeNewKey_whenKeyOnlyExistsAfter() {
        Map<String, String> after = snapshot("2", "Hà Nội");
        after.put("Tiêu chí: Java", "Skill");

        assertEquals(List.of("Tiêu chí: Java"), FieldChanges.changedKeys(snapshot("2", "Hà Nội"), after));
    }

    @Test
    @DisplayName("FilterLinks bỏ một bộ lọc -> Giữ tham số khác, xóa tham số page")
    void without_shouldKeepOtherParamsAndResetPage_whenRemovingOneFilter() {
        FilterLinks links = new FilterLinks("/requisitions", "q=java&status=Draft&status=Approved&page=3&sort=oldest");

        assertEquals("/requisitions?q=java&sort=oldest", links.without(List.of("status")));
    }
}
