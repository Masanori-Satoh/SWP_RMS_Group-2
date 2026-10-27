package com.group2.rms.candidate.application.portal.dto;

/**
 * Một bước trên thanh 4 bước của "Đơn của tôi" (Nộp CV → Phỏng vấn → Thư mời → Nhận việc).
 *
 * @param state {@code done} (đã qua), {@code current} (đơn đang ở bước này), {@code closed} (đơn dừng ở bước này),
 *              {@code upcoming} (chưa tới)
 */
public record MyApplicationStep(String label, String state) {
}
