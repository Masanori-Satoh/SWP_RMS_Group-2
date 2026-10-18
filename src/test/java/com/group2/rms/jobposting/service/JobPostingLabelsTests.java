package com.group2.rms.jobposting.service;

import com.group2.rms.admin.entity.AuditLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/*
 * UNIT TEST: JobPostingLabels (nơi duy nhất đổi giá trị DB → chữ tiếng Việt / màu badge / tên hoạt động).
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện; bảng giá trị dùng @CsvSource.
 */
class JobPostingLabelsTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 18, 10, 0);

    @ParameterizedTest(name = "{0} -> {1} ({2})")
    @CsvSource({
            "Draft,     Bản nháp,  neutral",
            "Scheduled, Hẹn đăng,  warning",
            "Published, Đang đăng, success",
            "Paused,    Tạm dừng,  warning",
            "Closed,    Đã đóng,   neutral"})
    @DisplayName("Trạng thái hiển thị -> Nhãn tiếng Việt và tông badge")
    void statusLabelAndTone_shouldTranslateEveryStatus(String viewStatus, String label, String tone) {
        assertEquals(label, JobPostingLabels.statusLabel(viewStatus));
        assertEquals(tone, JobPostingLabels.statusTone(viewStatus));
    }

    @ParameterizedTest(name = "Hạn nộp sau {0} giờ -> {1}")
    @CsvSource({"-1, past", "1, soon", "168, soon", "192, open"})
    @DisplayName("Nhóm hạn nộp: đã qua / còn ≤ 7 ngày / còn trên 7 ngày")
    void deadlineState_shouldGroupByDays(long hoursFromNow, String expected) {
        assertEquals(expected,
                JobPostingLabels.deadlineState(JobPostingAccess.STATUS_PUBLISHED, NOW.plusHours(hoursFromNow), NOW));
    }

    @Test
    @DisplayName("Tin đã đóng hoặc không có hạn nộp -> Không tô màu (null)")
    void deadlineState_shouldBeNull_whenClosedOrNoDeadline() {
        assertNull(JobPostingLabels.deadlineState(JobPostingAccess.STATUS_CLOSED, NOW.minusDays(1), NOW));
        assertNull(JobPostingLabels.deadlineState(JobPostingAccess.STATUS_PUBLISHED, null, NOW));
    }

    @ParameterizedTest(name = "{0} / [{1}] -> {2}")
    @CsvSource(delimiter = '|', value = {
            "CREATE | Tạo tin tuyển dụng: Published: Java | Tạo và đăng tin",
            "CREATE | Tạo tin tuyển dụng: Draft: Java     | Tạo mới",
            "UPDATE | Đăng tin tuyển dụng: Java           | Đăng tin",
            "UPDATE | Hẹn đăng tin tuyển dụng             | Hẹn đăng tin",
            "UPDATE | Tạm dừng tin tuyển dụng: Java       | Tạm dừng",
            "UPDATE | Hủy lịch đăng tin tuyển dụng: Java  | Hủy lịch đăng",
            "UPDATE | Closed                              | Đóng tin",
            "UPDATE | Cập nhật tin tuyển dụng             | Cập nhật",
            "DELETE | Xóa tin tuyển dụng: Java            | Xóa"})
    @DisplayName("Dòng audit log -> Tên hoạt động ở mục Lịch sử")
    void activityLabel_shouldNameEachAuditEntry(String action, String newValue, String expected) {
        AuditLog log = AuditLog.builder().action(action).newValue(newValue).build();

        assertEquals(expected, JobPostingLabels.activityLabel(log));
    }
}
