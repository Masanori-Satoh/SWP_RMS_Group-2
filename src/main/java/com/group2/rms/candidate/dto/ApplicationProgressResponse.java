package com.group2.rms.candidate.dto;

import com.group2.rms.candidate.service.ApplicationProgressStep;
import com.group2.rms.candidate.service.ApplicationStage;

/**
 * Hồ sơ đang ở giai đoạn nào, chờ ai, hiển thị ra sao.
 *
 * @param detailLabel nhãn trạng thái chi tiết đã điền ngày/số phiếu
 * @param ownerLabel  ai cầm bóng; {@code null} khi hồ sơ đã kết thúc
 * @param tone        {@code neutral}, {@code brand}, {@code success}, {@code warning}, {@code danger}
 */
public record ApplicationProgressResponse(
        ApplicationStage stage,
        ApplicationProgressStep step,
        String detailLabel,
        String ownerLabel,
        String tone) {

    /** Class badge theo tone; bộ badge không có biến thể brand nên dùng success (cùng nền {@code --brand-soft}). */
    public String badge() {
        return "badge--" + ("brand".equals(tone) ? "success" : tone);
    }
}
