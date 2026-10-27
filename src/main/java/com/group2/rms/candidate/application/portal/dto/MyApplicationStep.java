package com.group2.rms.candidate.application.portal.dto;

/**
 * Một bước trên thanh 4 bước của "Đơn của tôi" (Nộp CV → Phỏng vấn → Thư mời → Nhận việc).
 *
 * @param state {@code done} (đã qua; đơn dừng thì gồm cả bước dừng), {@code current} (đơn đang ở bước này),
 *              {@code upcoming} (chưa tới). Đơn dừng không có trạng thái riêng ở bước: cả thanh xám theo
 *              {@code MyApplicationResponse.stopped}
 */
public record MyApplicationStep(String label, String state) {
}
