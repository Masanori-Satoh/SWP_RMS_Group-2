package com.group2.rms.candidate.domain.rule;

import com.group2.rms.candidate.domain.repository.ApplicationRepository;

import java.time.LocalDateTime;

/**
 * Dữ kiện thô của một hồ sơ để tính trạng thái chi tiết, đọc từ bảng của candidate, interview, offer, AuditLog.
 * "Lịch mới nhất" = lịch có {@code StartTime} lớn nhất, bỏ qua lịch đã hủy; các trường lịch {@code null} nếu chưa có.
 *
 * @param withdrawn       ứng viên tự rút (dấu {@code Withdrawn} trong AuditLog)
 * @param interviewId     id lịch mới nhất, để dẫn sang trang kết quả phỏng vấn
 * @param interviewStatus trạng thái lịch mới nhất
 * @param panelCount      số người trong hội đồng của lịch mới nhất
 * @param evaluationCount số người đã nộp phiếu chấm cho lịch mới nhất
 * @param finalDecision   kết quả chốt của lịch mới nhất ({@code Passed}/{@code Failed}), {@code null} nếu chưa chốt
 * @param offerStatus     trạng thái offer của hồ sơ, {@code null} nếu chưa có
 */
public record ApplicationProgressFacts(
        Integer applicationId,
        String applicationStatus,
        boolean withdrawn,
        Long interviewId,
        String interviewStatus,
        LocalDateTime interviewStart,
        int panelCount,
        int evaluationCount,
        String finalDecision,
        String offerStatus) {

    public static ApplicationProgressFacts from(ApplicationRepository.ProgressFactsRow row) {
        return new ApplicationProgressFacts(row.getApplicationId(), row.getApplicationStatus(),
                Boolean.TRUE.equals(row.getWithdrawn()), row.getInterviewId(), row.getInterviewStatus(),
                row.getInterviewStart(),
                row.getPanelCount() == null ? 0 : row.getPanelCount(),
                row.getEvaluationCount() == null ? 0 : row.getEvaluationCount(),
                row.getFinalDecision(), row.getOfferStatus());
    }
}
