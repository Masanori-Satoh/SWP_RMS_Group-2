package com.group2.rms.dashboard.internal.service;

import com.group2.rms.dashboard.internal.dto.DashboardInterviewItem;
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

    /** Trang lịch phỏng vấn; tham số URL do js/interview-list.js đọc để đặt sẵn bộ lọc. */
    public static final String INTERVIEWS_HREF = "/interviews";
    public static final String INTERVIEWS_UPCOMING_HREF = INTERVIEWS_HREF + "?period=upcoming";
    /** Khớp KPI "Đánh giá chưa hoàn thành" (chỉ đếm buổi Completed): lọc thêm status để không lẫn buổi quá giờ chưa đóng. */
    public static final String INTERVIEWS_PENDING_EVALUATION_HREF = INTERVIEWS_HREF + "?status=Completed&evaluation=pending";
    public static final String INTERVIEWS_PASSED_HREF = INTERVIEWS_HREF + "?period=all&result=PASSED";

    /** Mở lịch đúng ngày của buổi và cuộn tới thẻ của buổi đó (chưa có trang chi tiết lịch, D7). */
    public static String interviewHref(InterviewRow row) {
        return INTERVIEWS_HREF + "?date=" + row.startTime().toLocalDate() + "&focus=" + row.interviewId();
    }

    /** Trang kết quả & phiếu đánh giá của một buổi. */
    public static String interviewResultHref(long interviewId) {
        return INTERVIEWS_HREF + "/" + interviewId + "/result";
    }

    private DashboardViews() {
        // Chỉ chứa hàm static, không tạo đối tượng
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
                interviewHref(row),
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
