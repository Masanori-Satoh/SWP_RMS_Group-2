package com.group2.rms.candidate.application.portal.dto;

/**
 * Một vòng trên thanh 4 chấm của My Applications (Nộp CV → Xét CV → Phỏng vấn → Thư mời).
 *
 * @param state {@code done} (đã qua), {@code current} (đơn đang ở vòng này), {@code closed} (đơn dừng ở vòng này),
 *              {@code upcoming} (chưa tới)
 */
public record MyApplicationStep(String label, String state) {
}
