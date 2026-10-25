package com.group2.rms.candidate.domain.rule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.group2.rms.candidate.domain.rule.ApplicationProgressStep.*;

/**
 * Tính trạng thái chi tiết của hồ sơ từ dữ kiện (hàm thuần, không đọc DB).
 * <p>
 * Đơn đang trong vòng phỏng vấn/offer ({@code HM_Passed}, {@code Interviewing},
 * {@code Offered}) được xét theo
 * việc thật đã xảy ra (offer, lịch, kết quả), không theo con dấu trạng thái: dữ
 * liệu cũ có thể chưa đổi trạng thái
 * (ví dụ đơn {@code HM_Passed} đã có lịch, đã chốt Không đạt). Offer đứng sau
 * phỏng vấn trong luồng nên xét trước.
 */
public final class ApplicationProgressResolver {

    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

    private ApplicationProgressResolver() {
    }

    public static ApplicationProgressResponse resolve(ApplicationProgressFacts facts, LocalDateTime now) {
        return switch (facts.applicationStatus()) {
            case "Applied" -> of(AWAITING_AI);
            case "AI_Screened" -> of(AWAITING_HR_REVIEW);
            case "HR_Passed" -> of(AWAITING_HM_REVIEW);
            case "HM_Suggested" -> of(HM_SUGGESTED);
            case "Hired" -> of(HIRED_DONE);
            case "Rejected" -> of(closedStep(facts));
            default -> inProgress(facts, now);
        };
    }

    /** Đơn {@code Rejected}: vì sao dừng. */
    private static ApplicationProgressStep closedStep(ApplicationProgressFacts facts) {
        if (facts.withdrawn()) {
            return WITHDRAWN;
        }
        if ("Declined".equals(facts.offerStatus())) {
            return OFFER_DECLINED;
        }
        if ("Failed".equals(facts.finalDecision())) {
            return INTERVIEW_FAILED;
        }
        return CV_REJECTED;
    }

    /** Đơn {@code HM_Passed}, {@code Interviewing}, {@code Offered}. */
    private static ApplicationProgressResponse inProgress(ApplicationProgressFacts facts, LocalDateTime now) {
        ApplicationProgressStep offerStep = offerStep(facts.offerStatus());
        if (offerStep != null) {
            return of(offerStep);
        }
        if ("Offered".equals(facts.applicationStatus())) {
            return of(OFFER_SENT);
        }
        if (facts.interviewStatus() == null) {
            return of(AWAITING_SCHEDULE);
        }
        if ("Failed".equals(facts.finalDecision())) {
            return of(INTERVIEW_FAILED);
        }
        if ("Passed".equals(facts.finalDecision())) {
            return of(PASSED_AWAITING_OFFER);
        }
        // HM đang cân nhắc: ứng viên chỉ thấy "chờ chốt kết quả", không lộ quyết định nội bộ (GBR-02)
        if ("Considering".equals(facts.finalDecision())) {
            return of(AWAITING_DECISION);
        }
        boolean upcoming = ("Scheduled".equals(facts.interviewStatus())
                || "Rescheduled".equals(facts.interviewStatus()))
                && facts.interviewStart() != null && facts.interviewStart().isAfter(now);
        if (upcoming) {
            return of(INTERVIEW_SCHEDULED, facts.interviewStart().format(DAY_MONTH));
        }
        if (facts.evaluationCount() < facts.panelCount()) {
            return of(AWAITING_EVALUATIONS, facts.evaluationCount(), facts.panelCount());
        }
        return of(AWAITING_DECISION);
    }

    /**
     * {@code null} khi chưa có offer (hoặc trạng thái lạ ngoài
     * {@code CK_OfferProposal_Status}).
     */
    private static ApplicationProgressStep offerStep(String offerStatus) {
        if (offerStatus == null) {
            return null;
        }
        return switch (offerStatus) {
            case "Draft" -> OFFER_DRAFT;
            case "Pending_Director" -> OFFER_AWAITING_DIRECTOR;
            case "Director_Rejected" -> OFFER_RETURNED;
            case "Director_Approved", "Approved" -> OFFER_AWAITING_SEND;
            case "Sent_Candidate" -> OFFER_SENT;
            case "Accepted" -> HIRED_DONE;
            case "Declined" -> OFFER_DECLINED;
            default -> null;
        };
    }

    private static ApplicationProgressResponse of(ApplicationProgressStep step, Object... labelArgs) {
        String label = labelArgs.length == 0 ? step.label() : String.format(step.label(), labelArgs);
        return new ApplicationProgressResponse(step.stage(), step, label, step.owner(), step.tone());
    }
}
