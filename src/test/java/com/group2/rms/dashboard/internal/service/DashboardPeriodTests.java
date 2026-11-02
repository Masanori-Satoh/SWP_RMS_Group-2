package com.group2.rms.dashboard.internal.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * UNIT TEST: DashboardPeriod (hàm thuần, không Spring, không DB).
 */
class DashboardPeriodTests {

    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 28);
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);

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
