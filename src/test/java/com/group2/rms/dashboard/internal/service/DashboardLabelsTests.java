package com.group2.rms.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * UNIT TEST: DashboardLabels (số ngày chờ, tông màu, nhãn hình thức / trạng thái phỏng vấn). Không cần Spring.
 */
class DashboardLabelsTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 9, 0);

    @Nested
    @DisplayName("waitDays")
    class WaitDays {

        @Test
        @DisplayName("Chưa có mốc thời gian -> 0 ngày")
        void waitDays_shouldReturnZero_whenSinceIsNull() {
            // Act + Assert
            assertEquals(0, DashboardLabels.waitDays(null, NOW));
        }

        @Test
        @DisplayName("Gửi hôm qua lúc 23:00, xem lúc 09:00 hôm nay -> 1 ngày (đếm theo ngày lịch)")
        void waitDays_shouldCountCalendarDays_whenSentLateYesterday() {
            // Act + Assert
            assertEquals(1, DashboardLabels.waitDays(NOW.minusDays(1).withHour(23), NOW));
        }

        @Test
        @DisplayName("Gửi sáng nay -> 0 ngày")
        void waitDays_shouldReturnZero_whenSentToday() {
            // Act + Assert
            assertEquals(0, DashboardLabels.waitDays(NOW.withHour(1), NOW));
        }

        @Test
        @DisplayName("Mốc thời gian ở tương lai (lệch giờ máy) -> 0 ngày, không ra số âm")
        void waitDays_shouldReturnZero_whenSinceIsInFuture() {
            // Act + Assert
            assertEquals(0, DashboardLabels.waitDays(NOW.plusDays(2), NOW));
        }
    }

    @Test
    @DisplayName("0 ngày -> 'Hôm nay'; 5 ngày -> '5 ngày'")
    void waitText_shouldShowTodayOrDays() {
        // Act + Assert
        assertEquals("Hôm nay", DashboardLabels.waitText(0));
        assertEquals("5 ngày", DashboardLabels.waitText(5));
    }

    @ParameterizedTest(name = "{0} ngày -> {1}")
    @CsvSource({"0,neutral", "2,neutral", "3,warning", "6,warning", "7,danger", "30,danger"})
    @DisplayName("Tông màu theo ngưỡng 3 ngày (cam) và 7 ngày (đỏ)")
    void waitTone_shouldFollowThresholds(long days, String expectedTone) {
        // Act + Assert
        assertEquals(expectedTone, DashboardLabels.waitTone(days));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"Online_GoogleMeet,Trực tuyến", "Offline_Office,Tại văn phòng", "Other,Other"})
    @DisplayName("Hình thức phỏng vấn -> nhãn tiếng Việt, giá trị lạ giữ nguyên")
    void interviewFormat_shouldTranslateKnownValues(String format, String expectedLabel) {
        // Act + Assert
        assertEquals(expectedLabel, DashboardLabels.interviewFormat(format));
    }

    @ParameterizedTest(name = "{0} -> {1} / {2}")
    @CsvSource({"Scheduled,Đã lên lịch,neutral", "Rescheduled,Đã đổi lịch,warning",
            "Completed,Đã hoàn thành,neutral", "Cancelled,Đã hủy,neutral", "Other,Other,neutral"})
    @DisplayName("Trạng thái phỏng vấn -> nhãn và tông màu; đổi lịch tô cam")
    void interviewStatus_shouldTranslateAndTone(String status, String expectedLabel, String expectedTone) {
        // Act + Assert
        assertEquals(expectedLabel, DashboardLabels.interviewStatus(status));
        assertEquals(expectedTone, DashboardLabels.interviewStatusTone(status));
    }
}
