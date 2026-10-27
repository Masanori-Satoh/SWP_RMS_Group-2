package com.group2.rms.candidate.application.portal.dto;

import com.group2.rms.core.web.TimelineItem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Một đơn của "Đơn của tôi": thẻ trên danh sách và nội dung trang chi tiết. Thanh 4 bước, chữ trạng thái kèm một dòng
 * giải thích, việc tiếp theo, việc gấp và hành trình. Không có điểm AI, nhận xét, người duyệt hay offer nội bộ (GBR-02).
 *
 * @param departmentName    phòng ban của yêu cầu tuyển
 * @param applicationCode   mã hiển thị {@code #CAN-0042} (id đệm 4 số), không lưu DB
 * @param steps             thanh Nộp CV → Phỏng vấn → Thư mời → Nhận việc, mỗi bước một trạng thái
 *                          {@code done}, {@code current}, {@code closed} (đơn dừng ở bước này), {@code upcoming}
 * @param lastActivityAt    lần cập nhật gần nhất ứng viên thấy được; danh sách xếp theo trường này, mới nhất trước
 * @param statusNote        dòng giải thích dưới chữ trạng thái; {@code null} nếu không có
 * @param statusTone        màu: {@code neutral}, {@code success}, {@code warning}, {@code danger}
 * @param actionLabel       nút việc tiếp theo; {@code null} nếu không có
 * @param actionHref        đường dẫn trong ứng dụng của nút việc tiếp theo
 * @param urgentUntil       hạn của việc cần làm ngay; {@code null} nếu đơn không gấp
 * @param urgentLabel       dòng "còn …" của việc gấp; {@code null} nếu đơn không gấp
 * @param ended             đơn đã dừng hoặc đã nhận việc (tab "Đã kết thúc")
 * @param canWithdraw       đơn còn rút được nên trang chi tiết hiện nút "Rút đơn"
 * @param hasCv             đơn có CV để xem lại
 * @param jobPostingVisible tin còn hiện ở trang công khai (không thì ẩn link "Xem tin tuyển dụng")
 * @param journey           các mốc đơn đã đi qua, cũ nhất trước
 */
public record MyApplicationResponse(
        Integer applicationId,
        Integer jobPostingId,
        String postingTitle,
        String departmentName,
        String applicationCode,
        LocalDateTime submissionDate,
        LocalDateTime lastActivityAt,
        List<MyApplicationStep> steps,
        String statusLabel,
        String statusNote,
        String statusTone,
        String actionLabel,
        String actionHref,
        LocalDateTime urgentUntil,
        String urgentLabel,
        boolean ended,
        boolean canWithdraw,
        boolean hasCv,
        boolean jobPostingVisible,
        List<TimelineItem> journey) {

    /** Đơn có việc cần làm ngay (viền nổi, dòng "còn …", banner). */
    public boolean urgent() {
        return urgentUntil != null;
    }
}
