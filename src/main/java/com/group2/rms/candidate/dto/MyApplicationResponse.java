package com.group2.rms.candidate.dto;

import com.group2.rms.core.web.TimelineItem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Một thẻ đơn của My Applications (5.1.38): Stage (thanh 4 chấm), Status (badge + một dòng giải thích), việc tiếp
 * theo và hành trình. Không có điểm AI, nhận xét, người duyệt hay offer nội bộ (GBR-02).
 *
 * @param steps       4 vòng Nộp CV → Xét CV → Phỏng vấn → Thư mời, kèm trạng thái từng vòng
 * @param statusNote  dòng giải thích dưới badge; {@code null} nếu không có
 * @param statusTone  hậu tố class badge: {@code neutral}, {@code success}, {@code warning}, {@code danger}
 * @param actionLabel nút việc tiếp theo; {@code null} nếu không có
 * @param actionHref  đường dẫn trong ứng dụng của nút việc tiếp theo
 * @param canWithdraw đơn còn ở vòng đầu (HR chưa duyệt) nên hiện nút "Rút đơn"
 * @param journey     các mốc đơn đã đi qua, cũ nhất trước
 */
public record MyApplicationResponse(
        Integer applicationId,
        Integer jobPostingId,
        String postingTitle,
        LocalDateTime submissionDate,
        List<MyApplicationStep> steps,
        String statusLabel,
        String statusNote,
        String statusTone,
        String actionLabel,
        String actionHref,
        boolean canWithdraw,
        List<TimelineItem> journey) {
}
