package com.group2.rms.dashboard.internal.service;

import com.group2.rms.dashboard.internal.dto.DashboardInterviewItem;
import com.group2.rms.dashboard.internal.dto.DashboardMetricResponse;
import com.group2.rms.dashboard.internal.repository.DashboardListRepository.InterviewRow;
import com.group2.rms.interview.entity.InterviewFormat;
import com.group2.rms.interview.entity.InterviewStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * VIEWS: hàm dựng DTO dùng chung cho dashboard 4 role (thẻ KPI so sánh kỳ, dòng lịch phỏng vấn, cắt danh sách).
 * Hàm thuần, không truy vấn DB.
 */
public final class DashboardViews {

    /** Link của mọi dòng lịch phỏng vấn: hệ thống chưa có trang chi tiết lịch (D7). */
    public static final String INTERVIEWS_HREF = "/interviews";

    private DashboardViews() {
        // Chỉ chứa hàm static, không tạo đối tượng
    }

    /** Thẻ KPI "dòng chảy": số trong 30 ngày gần nhất kèm % so với 30 ngày trước đó. */
    public static DashboardMetricResponse flowMetric(String icon, String title, long current, long previous,
            String href) {
        String delta = DashboardPeriod.changeText(current, previous);
        String deltaTone = DashboardPeriod.changeTone(current, previous);
        return new DashboardMetricResponse(icon, title, String.valueOf(current), delta, deltaTone,
                "so với 30 ngày trước", href, "neutral");
    }

    /** Dòng lịch phỏng vấn. Buổi đã kết thúc hiện "Đã xong" để người xem phân biệt với buổi sắp tới. */
    public static List<DashboardInterviewItem> interviewItems(List<InterviewRow> rows, LocalDateTime now) {
        List<DashboardInterviewItem> items = new ArrayList<>();
        for (InterviewRow row : rows) {
            items.add(interviewItem(row, now));
        }
        return items;
    }

    public static DashboardInterviewItem interviewItem(InterviewRow row, LocalDateTime now) {
        // Security: link họp trực tuyến không hiện trên dashboard, chỉ hiện địa điểm của buổi tại văn phòng.
        String location = null;
        if (InterviewFormat.Offline_Office.name().equals(row.interviewFormat())) {
            location = row.locationOrLink();
        }
        String stateLabel = DashboardLabels.interviewStatus(row.interviewStatus());
        String stateTone = DashboardLabels.interviewStatusTone(row.interviewStatus());
        boolean finished = InterviewStatus.Completed.name().equals(row.interviewStatus())
                || (row.endTime() != null && row.endTime().isBefore(now));
        if (finished) {
            stateLabel = "Đã xong";
            stateTone = "neutral";
        }
        return new DashboardInterviewItem(
                row.startTime(),
                row.endTime(),
                candidateName(row.candidateName()),
                row.positionTitle(),
                DashboardLabels.interviewFormat(row.interviewFormat()),
                location,
                (int) row.panelSize(),
                stateLabel,
                stateTone,
                INTERVIEWS_HREF,
                finished);
    }

    /** Ứng viên cũ có thể chưa có tài khoản (không có họ tên). */
    public static String candidateName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Ứng viên chưa có tên";
        }
        return fullName;
    }

    /** Tối đa limit phần tử đầu; danh sách ngắn hơn thì giữ nguyên. */
    public static <T> List<T> firstItems(List<T> items, int limit) {
        if (items.size() <= limit) {
            return items;
        }
        return List.copyOf(items.subList(0, limit));
    }
}
