package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.ApplicationProgressFacts;
import com.group2.rms.candidate.dto.ApplicationProgressResponse;
import com.group2.rms.candidate.entity.ApplicationProgressStep;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.group2.rms.candidate.entity.ApplicationProgressStep.AWAITING_DECISION;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.AWAITING_EVALUATIONS;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.AWAITING_HM_REVIEW;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.AWAITING_HR_REVIEW;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.AWAITING_SCHEDULE;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.CV_REJECTED;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.HIRED_DONE;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.HM_SUGGESTED;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.INTERVIEW_FAILED;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.INTERVIEW_SCHEDULED;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.OFFER_AWAITING_DIRECTOR;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.OFFER_AWAITING_SEND;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.OFFER_DECLINED;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.OFFER_DRAFT;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.OFFER_EXPIRED;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.OFFER_RETURNED;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.OFFER_SENT;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.PASSED_AWAITING_OFFER;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.POSTING_CLOSED;
import static com.group2.rms.candidate.entity.ApplicationProgressStep.WITHDRAWN;

/**
 * Resolver tính toán trạng thái chi tiết của hồ sơ ứng tuyển từ các dữ kiện tiến trình.
 */
public final class ApplicationProgressResolver {

    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

    private ApplicationProgressResolver() {
    }

    /**
     * Xác định trạng thái tiến trình chi tiết của hồ sơ dựa vào dữ kiện thực tế.
     */
    public static ApplicationProgressResponse resolve(ApplicationProgressFacts facts, LocalDateTime now) {
        return switch (facts.applicationStatus()) {
            case "Applied" -> of(AWAITING_HR_REVIEW);
            case "HR_Passed" -> of(AWAITING_HM_REVIEW);
            case "HM_Suggested" -> of(HM_SUGGESTED);
            case "Hired" -> of(HIRED_DONE);
            case "Rejected" -> of(rejectedStep(facts));
            case "Withdrawn" -> of(withdrawnStep(facts));
            case "Cancelled" -> of(POSTING_CLOSED);
            default -> inProgress(facts, now);
        };
    }

    private static ApplicationProgressStep rejectedStep(ApplicationProgressFacts facts) {
        return "Failed".equals(facts.finalDecision()) ? INTERVIEW_FAILED : CV_REJECTED;
    }

    private static ApplicationProgressStep withdrawnStep(ApplicationProgressFacts facts) {
        return "Declined".equals(facts.offerStatus()) ? OFFER_DECLINED : WITHDRAWN;
    }

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
        if ("Considering".equals(facts.finalDecision())) {
            return of(AWAITING_DECISION);
        }
        boolean upcoming = ("Scheduled".equals(facts.interviewStatus())
                || "Rescheduled".equals(facts.interviewStatus()))
                && facts.interviewStart() != null
                && facts.interviewStart().isAfter(now);
        if (upcoming) {
            return of(INTERVIEW_SCHEDULED, facts.interviewStart().format(DAY_MONTH));
        }
        if (facts.evaluationCount() < facts.panelCount()) {
            return of(AWAITING_EVALUATIONS, facts.evaluationCount(), facts.panelCount());
        }
        return of(AWAITING_DECISION);
    }

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
            case "Expired" -> OFFER_EXPIRED;
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
