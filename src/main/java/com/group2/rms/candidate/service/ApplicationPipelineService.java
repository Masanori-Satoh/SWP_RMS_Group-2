package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.ApplicationDetailResponse;
import com.group2.rms.candidate.dto.ApplicationListResponse;
import com.group2.rms.candidate.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.dto.ApplicationSearch;
import com.group2.rms.candidate.entity.AIScreeningResult;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.repository.AIScreeningResultRepository;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.repository.ApplicationReviewRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.web.TimelineItem;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Đọc dữ liệu cho danh sách hồ sơ (5.1.22) và chi tiết hồ sơ (5.1.23), trong phạm vi người xem được phép.
 */
@Service
@RequiredArgsConstructor
public class ApplicationPipelineService {

    private static final Locale VIETNAMESE = Locale.of("vi", "VN");

    private final ApplicationRepository applications;
    private final AIScreeningResultRepository screeningResults;
    private final ApplicationReviewRepository reviews;
    private final ApplicationAccess access;

    /** Thứ tự do {@code search.sort()} quyết định; sort trong {@code pageable} bị bỏ qua. */
    @Transactional(readOnly = true)
    public ApplicationListResponse list(ApplicationSearch search, Pageable pageable) {
        User actor = access.actor();
        ApplicationScope scope = access.scopeOf(actor);
        ApplicationSearch filter = search.normalized();

        Page<ApplicationPipelineResponse> page = applications.searchPipeline(
                        scope.allDepartments(), scope.managerUserId(), scope.departmentId(), scope.forwardedOnly(),
                        filter.jobPostingId(), filter.status(), filter.keyword(), filter.sort(),
                        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()))
                .map(row -> row.withActions(
                        access.canScheduleInterview(actor, row.applicationStatus()),
                        access.canCreateOffer(actor, row.applicationStatus())));

        return new ApplicationListResponse(page,
                applications.findPostingOptions(scope.allDepartments(), scope.managerUserId(), scope.departmentId(),
                        scope.forwardedOnly()),
                filter, scope.forwardedOnly(), actor.getRole() == null ? null : actor.getRole().getRoleName());
    }

    @Transactional(readOnly = true)
    public ApplicationDetailResponse detail(Integer applicationId) {
        User actor = access.actor();
        Application application = applications.findDetailById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));
        access.requireView(actor, application);

        Candidate candidate = application.getCandidate();
        User account = candidate.getAccount();
        JobPosting job = application.getJobPosting();
        JobRequisition requisition = job.getRequisition();
        String status = application.getApplicationStatus();
        List<AIScreeningResult> scores = screeningResults
                .findByApplication_ApplicationIdOrderByAiScreeningIdDesc(applicationId);
        AIScreeningResult latest = scores.isEmpty() ? null : scores.getFirst();

        return new ApplicationDetailResponse(
                application.getApplicationId(),
                account.getFullName(), account.getEmail(), account.getPhoneNumber(),
                candidate.getLinkedInUrl(), candidate.getPortfolioUrl(),
                requisition.getRequisitionId(), requisition.getRequisitionCode(),
                job.getJobPostingId(), job.getPostingTitle(),
                requisition.getDepartment() == null ? null : requisition.getDepartment().getDepartmentName(),
                application.getSubmissionDate(),
                status, ApplicationStatusLabels.label(status), ApplicationStatusLabels.badge(status),
                latest == null ? null : latest.getAiMatchScore(),
                latest == null ? null : latest.getScreenedAt(),
                timeline(application, account, scores),
                new ApplicationDetailResponse.Actions(
                        access.canReview(actor, application),
                        access.canRescreen(actor, application),
                        access.canScheduleInterview(actor, status),
                        access.canCreateOffer(actor, status),
                        access.canOpenJobPosting(actor)));
    }

    /** Hành trình của đơn, mới nhất trước: nộp, AI chấm, các lượt duyệt, lịch phỏng vấn, offer. */
    private List<TimelineItem> timeline(Application application, User account, List<AIScreeningResult> scores) {
        Integer id = application.getApplicationId();
        List<TimelineItem> items = new ArrayList<>();
        items.add(new TimelineItem(application.getSubmissionDate(), "Nộp hồ sơ", account.getFullName(), null,
                "neutral"));
        scores.forEach(score -> items.add(new TimelineItem(score.getScreenedAt(),
                "AI chấm điểm: " + formatScore(score.getAiMatchScore()), "Hệ thống", null, "brand")));
        reviews.findWithReviewerByApplicationId(id).forEach(review -> items.add(new TimelineItem(
                review.getReviewedAt(),
                ApplicationStatusLabels.reviewerRoleLabel(review.getReviewerRole()) + ": "
                        + ApplicationStatusLabels.decisionLabel(review.getDecision()),
                review.getReviewer().getFullName(), review.getComments(),
                ApplicationStatusLabels.decisionTone(review.getDecision()))));
        applications.findInterviewEvents(id).forEach(event -> items.add(new TimelineItem(
                event.getAt(), "Lịch phỏng vấn: " + event.getStatus(), null, null, "neutral")));
        applications.findOfferEvents(id).forEach(event -> items.add(new TimelineItem(
                event.getAt(), "Offer: " + event.getStatus(), null, null, "success")));
        items.sort(Comparator.comparing(TimelineItem::at, Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }

    private static String formatScore(BigDecimal score) {
        return String.format(VIETNAMESE, "%.2f", score);
    }
}
