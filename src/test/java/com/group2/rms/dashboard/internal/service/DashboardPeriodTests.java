package com.group2.rms.dashboard.internal.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/*
 * UNIT TEST: DashboardPeriod (hàm thuần, không Spring, không DB).
 */
class DashboardPeriodTests {

    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 28);
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);

    @ParameterizedTest(name = "{0} so với {1} -> {2}")
    @CsvSource({"12,10,↑ 20%", "8,10,↓ 20%", "10,10,0%", "5,0,Mới"})
    @DisplayName("Mũi tên % so với kỳ trước; kỳ trước 0 mà nay có -> 'Mới'")
    void changeText_shouldShowArrowAndPercent(long current, long previous, String expected) {
        // Act + Assert
        assertEquals(expected, DashboardPeriod.changeText(current, previous));
    }

    @Test
    @DisplayName("Cả hai kỳ đều 0 -> không có mũi tên (null)")
    void changeText_shouldReturnNull_whenBothPeriodsEmpty() {
        // Act + Assert
        assertNull(DashboardPeriod.changeText(0, 0));
    }

    @ParameterizedTest(name = "{0} so với {1} -> {2}")
    @CsvSource({"12,10,success", "8,10,danger", "10,10,neutral"})
    @DisplayName("Tăng xanh, giảm đỏ, bằng xám")
    void changeTone_shouldFollowDirection(long current, long previous, String expected) {
        // Act + Assert
        assertEquals(expected, DashboardPeriod.changeTone(current, previous));
    }

    @Test
    @DisplayName("Tỷ lệ làm tròn; mẫu số 0 -> null")
    void percent_shouldRoundAndReturnNull_whenWholeIsZero() {
        // Act + Assert
        assertEquals(67, DashboardPeriod.percent(2, 3));
        assertNull(DashboardPeriod.percent(0, 0));
    }

    @ParameterizedTest(name = "\"{0}\" -> tuần hiện tại")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "28/10/2026", "2026-13-01", "abc"})
    @DisplayName("Tham số tuần trống / sai định dạng -> thứ Hai tuần hiện tại (D13)")
    void weekStart_shouldFallBackToCurrentWeek_whenInvalid(String requested) {
        // Act + Assert
        assertEquals(MONDAY, DashboardPeriod.weekStart(requested, WEDNESDAY));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"2026-11-02,2026-11-02", "2026-11-08,2026-11-02", "2026-11-04,2026-11-02"})
    @DisplayName("Ngày bất kỳ trong tuần -> thứ Hai của tuần đó (Chủ nhật thuộc tuần trước nó)")
    void weekStart_shouldReturnMonday_whenDateValid(String requested, String expectedMonday) {
        // Act + Assert
        assertEquals(LocalDate.parse(expectedMonday), DashboardPeriod.weekStart(requested, WEDNESDAY));
    }
}
