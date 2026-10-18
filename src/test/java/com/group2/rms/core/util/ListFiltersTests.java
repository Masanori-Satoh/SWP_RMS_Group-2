package com.group2.rms.core.util;

import com.group2.rms.core.dto.FilterOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * UNIT TEST: ListFilters (helper dùng chung khi làm sạch bộ lọc trên URL ở các màn danh sách).
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện.
 */
class ListFiltersTests {

    private static final List<FilterOption> STATUSES = List.of(
            new FilterOption("Draft", "Bản nháp"),
            new FilterOption("Approved", "Đã duyệt"));

    private static final List<FilterOption> DEPARTMENTS = List.of(new FilterOption("3", "Kế toán"));

    // ----- keepAllowedValues -----

    @Test
    @DisplayName("Giá trị lạ và giá trị trùng -> Bị bỏ, giữ đúng thứ tự giá trị hợp lệ")
    void keepAllowedValues_shouldDropUnknownAndDuplicates_whenUrlHasMixedValues() {
        List<String> kept = ListFilters.keepAllowedValues(
                Arrays.asList("Approved", "Deleted", "Approved", null, "Draft"), STATUSES);

        assertEquals(List.of("Approved", "Draft"), kept);
    }

    @Test
    @DisplayName("URL không có tham số (null) -> Danh sách rỗng, không lỗi")
    void keepAllowedValues_shouldReturnEmptyList_whenValuesAreNull() {
        assertTrue(ListFilters.keepAllowedValues(null, STATUSES).isEmpty());
    }

    @Test
    @DisplayName("Sai hoa / thường ('approved') -> Không khớp, bị bỏ")
    void keepAllowedValues_shouldDropValue_whenCaseDiffers() {
        assertTrue(ListFilters.keepAllowedValues(List.of("approved"), STATUSES).isEmpty());
    }

    // ----- isAllowedValue -----

    @Test
    @DisplayName("Số nguyên khớp value dạng chữ ('3') -> Hợp lệ")
    void isAllowedValue_shouldMatchNumber_whenOptionValueIsText() {
        assertTrue(ListFilters.isAllowedValue(3, DEPARTMENTS));
        assertFalse(ListFilters.isAllowedValue(99, DEPARTMENTS));
    }

    @Test
    @DisplayName("Giá trị null -> Không hợp lệ")
    void isAllowedValue_shouldReturnFalse_whenValueIsNull() {
        assertFalse(ListFilters.isAllowedValue(null, DEPARTMENTS));
    }

    // ----- labelOf -----

    @Test
    @DisplayName("Có lựa chọn -> Trả về chữ hiển thị; không có -> Trả về chính giá trị")
    void labelOf_shouldFallBackToValue_whenOptionIsMissing() {
        assertEquals("Kế toán", ListFilters.labelOf(3, DEPARTMENTS));
        assertEquals("7", ListFilters.labelOf(7, DEPARTMENTS));
    }

    // ----- cleanKeyword -----

    @ParameterizedTest(name = "từ khóa = \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Từ khóa null / rỗng / chỉ khoảng trắng -> Chuỗi rỗng")
    void cleanKeyword_shouldReturnEmpty_whenKeywordIsBlank(String raw) {
        assertEquals("", ListFilters.cleanKeyword(raw));
    }

    @Test
    @DisplayName("Từ khóa đúng giới hạn -> Giữ nguyên; vượt 1 ký tự -> Cắt về đúng giới hạn")
    void cleanKeyword_shouldCutToLimit_whenKeywordIsTooLong() {
        String atLimit = "a".repeat(ListFilters.MAX_KEYWORD_LENGTH);

        assertEquals(atLimit, ListFilters.cleanKeyword("  " + atLimit + "  "));
        assertEquals(atLimit, ListFilters.cleanKeyword(atLimit + "b"));
    }

    // ----- containsPattern / escapeLike -----

    @Test
    @DisplayName("Từ khóa có % _ [ \\ -> Được escape để LIKE hiểu là chữ thường")
    void escapeLike_shouldEscapeWildcards_whenKeywordHasSpecialCharacters() {
        assertEquals("50\\% off\\_\\[x\\\\", ListFilters.escapeLike("50% off_[x\\"));
    }

    @Test
    @DisplayName("containsPattern -> Chữ thường, bọc %...%")
    void containsPattern_shouldLowercaseAndWrap_whenKeywordIsGiven() {
        assertEquals("%java\\_%", ListFilters.containsPattern("JAVA_"));
    }
}
