package com.group2.rms.candidate.service;

import com.group2.rms.candidate.entity.AIScreeningResult;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.repository.AIScreeningResultRepository;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.requisition.repository.ScreeningCriteriaRepository;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.notification.constants.NotificationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Chấm một đơn bằng {@link AiScreeningClient} và lưu điểm. AI chỉ gợi ý: không bao giờ loại đơn,
 * chỉ chuyển {@code Applied → AI_Screened}. Mỗi lần chấm thêm một dòng {@code AIScreeningResult} (giữ lịch sử).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiScreeningService {

    private final ApplicationRepository applications;
    private final AIScreeningResultRepository screeningResults;
    private final ScreeningCriteriaRepository screeningCriteria;
    private final AiScreeningClient aiScreeningClient;
    private final NotificationService notifications;

    @Transactional
    public AiScreeningOutcome screen(Integer applicationId) {
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển."));
        JobPosting job = application.getJobPosting();

        AiScreeningOutcome outcome = aiScreeningClient.score(new AiScreeningInput(
                applicationId, application.getAppliedCvUrl(), job.getPostingTitle(),
                criteriaOf(job.getRequisition().getRequisitionId())));

        screeningResults.save(AIScreeningResult.builder()
                .application(application)
                .aiMatchScore(outcome.matchScore())
                .build());
        if (applications.markScreened(applicationId) == 0) {
            log.info("Application {} is no longer 'Applied'; score saved, status unchanged", applicationId);
        } else if (application.getCandidate() != null) {
            notifications.publish(application.getCandidate().getAccount(), NotificationEventType.APPLICATION_STATUS_UPDATED,
                    "Application", applicationId, "/portal/applications",
                    "APP_STATUS_" + applicationId + "_AI_Screened", false);
        }
        return outcome;
    }

    private List<AiScreeningInput.Criterion> criteriaOf(Integer requisitionId) {
        return screeningCriteria.findByRequisition_RequisitionId(requisitionId).stream()
                .map(AiScreeningService::toCriterion)
                .toList();
    }

    private static AiScreeningInput.Criterion toCriterion(ScreeningCriteria c) {
        return new AiScreeningInput.Criterion(c.getCriteriaName(), c.getCriteriaType(), c.getRequiredValue(),
                c.getWeight(), Boolean.TRUE.equals(c.getIsMandatory()));
    }
}
