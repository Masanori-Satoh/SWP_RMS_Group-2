package com.group2.rms.candidate.service;

/**
 * Phát ra khi một đơn ứng tuyển vừa được lưu. Bên nghe (AI chấm điểm, làm sau) nên dùng
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} để chỉ chạy khi đơn đã lưu thật.
 */
public record ApplicationSubmittedEvent(Integer applicationId) {
}
