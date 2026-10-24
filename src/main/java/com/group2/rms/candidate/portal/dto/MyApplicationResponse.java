package com.group2.rms.candidate.portal.dto;

import com.group2.rms.core.web.TimelineItem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Một thẻ đơn của My Applications (5.1.38): Stage (tag "Bước n · vòng", cùng số và icon với dải quy trình đầu trang),
 * Status (một dòng + một dòng giải thích), việc tiếp theo và hành trình. Không có điểm AI, nhận xét, người duyệt hay
 * offer nội bộ (GBR-02).
 *
 * @param stepNumber  bước đơn đang đứng hoặc dừng, 1–4 (Nộp CV → Xét CV → Phỏng vấn → Thư mời)
 * @param stepLabel   tên vòng của bước đó
 * @param stepState   {@code current} (đang ở bước này), {@code closed} (dừng ở bước này), {@code done} (xong cả 4)
 * @param statusNote  dòng giải thích dưới tình trạng; {@code null} nếu không có
 * @param statusTone  màu: {@code neutral}, {@code success}, {@code warning}, {@code danger}
 * @param actionLabel nút việc tiếp theo; {@code null} nếu không có
 * @param actionHref  đường dẫn trong ứng dụng của nút việc tiếp theo
 * @param canWithdraw đơn còn ở vòng đầu (HR chưa duyệt) nên hiện nút "Rút đơn"
 * @param journey     các mốc đơn đã đi qua, cũ nhất trước
 * @param lastActivityAt lần cập nhật gần nhất ứng viên thấy được; danh sách xếp theo trường này, mới nhất trước
 */
public record MyApplicationResponse(
        Integer applicationId,
        Integer jobPostingId,
        String postingTitle,
        LocalDateTime submissionDate,
        LocalDateTime lastActivityAt,
        int stepNumber,
        String stepLabel,
        String stepState,
        String statusLabel,
        String statusNote,
        String statusTone,
        String actionLabel,
        String actionHref,
        boolean canWithdraw,
        List<TimelineItem> journey) {
}
