package com.group2.rms.candidate.domain.rule;

import static com.group2.rms.candidate.domain.rule.ApplicationStage.*;

/**
 * Trạng thái chi tiết của hồ sơ: hồ sơ đang nằm trên bàn ai. Mỗi hằng là một
 * dòng của bảng trạng thái
 * (spec luồng trạng thái hồ sơ, mục 2.3); {@link ApplicationProgressResolver}
 * chọn hằng từ dữ kiện.
 * Nhãn có {@code %s}/{@code %d} được điền ở
 * {@link ApplicationProgressResolver}.
 *
 * @see ApplicationStage
 */
public enum ApplicationProgressStep {
    AWAITING_AI(APPLIED, "Chờ AI chấm CV", "Hệ thống", "neutral"),
    AWAITING_HR_REVIEW(SCREENING, "Chờ HR duyệt CV", "HR", "warning"),
    AWAITING_HM_REVIEW(SCREENING, "Chờ trưởng bộ phận duyệt CV", "Trưởng bộ phận", "warning"),
    HM_SUGGESTED(SCREENING, "HM đề xuất — chờ HR liên hệ", "HR", "warning"),
    AWAITING_SCHEDULE(INTERVIEW, "Chờ xếp lịch phỏng vấn", "HR", "warning"),
    INTERVIEW_SCHEDULED(INTERVIEW, "Đã xếp lịch, phỏng vấn ngày %s", "Hội đồng", "brand"),
    AWAITING_EVALUATIONS(INTERVIEW, "Chờ chấm điểm (%d/%d)", "Người phỏng vấn", "warning"),
    AWAITING_DECISION(INTERVIEW, "Chờ chốt kết quả phỏng vấn", "Trưởng bộ phận", "warning"),
    PASSED_AWAITING_OFFER(OFFER, "Đạt phỏng vấn, chờ lập offer", "HR", "warning"),
    OFFER_DRAFT(OFFER, "Offer đang soạn", "HR", "warning"),
    OFFER_AWAITING_DIRECTOR(OFFER, "Chờ giám đốc duyệt offer", "Giám đốc", "warning"),
    OFFER_RETURNED(OFFER, "Giám đốc trả offer, HR sửa lại", "HR", "danger"),
    OFFER_AWAITING_SEND(OFFER, "Chờ HR gửi offer", "HR", "warning"),
    OFFER_SENT(OFFER, "Đã gửi offer, chờ ứng viên trả lời", "Ứng viên", "brand"),
    HIRED_DONE(HIRED, "Đã nhận việc", null, "success"),
    WITHDRAWN(CLOSED, "Ứng viên đã rút hồ sơ", null, "neutral"),
    OFFER_DECLINED(CLOSED, "Ứng viên từ chối offer", null, "neutral"),
    INTERVIEW_FAILED(CLOSED, "Không đạt phỏng vấn", null, "danger"),
    CV_REJECTED(CLOSED, "Không qua vòng CV", null, "danger"),
    POSTING_CLOSED(CLOSED, "Dừng do đợt tuyển đóng", null, "neutral");

    private final ApplicationStage stage;
    private final String label;
    private final String owner;
    private final String tone;

    ApplicationProgressStep(ApplicationStage stage, String label, String owner, String tone) {
        this.stage = stage;
        this.label = label;
        this.owner = owner;
        this.tone = tone;
    }

    public ApplicationStage stage() {
        return stage;
    }

    /** Nhãn hiển thị; có thể chứa chỗ trống {@code %s}/{@code %d}. */
    public String label() {
        return label;
    }

    /** Ai cầm bóng; {@code null} khi hồ sơ đã kết thúc. */
    public String owner() {
        return owner;
    }

    /**
     * Màu: {@code neutral}, {@code brand}, {@code success}, {@code warning},
     * {@code danger} (cùng bộ với timeline).
     */
    public String tone() {
        return tone;
    }
}
