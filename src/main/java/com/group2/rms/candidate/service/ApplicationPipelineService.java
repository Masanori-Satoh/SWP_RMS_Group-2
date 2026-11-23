package com.group2.rms.candidate.service;

import com.group2.rms.candidate.storage.ApplicationCv;
import com.group2.rms.candidate.dto.ApplicationDetailResponse;
import com.group2.rms.candidate.dto.ApplicationListResponse;
import com.group2.rms.candidate.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.dto.ApplicationProgressFacts;
import com.group2.rms.candidate.dto.ApplicationProgressResponse;
import com.group2.rms.candidate.dto.ApplicationScope;
import com.group2.rms.candidate.dto.ApplicationSearch;
import com.group2.rms.candidate.dto.ApplicationStatusLabels;
import com.group2.rms.candidate.dto.OwnerWorkloadResponse;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.ApplicationReview;
import com.group2.rms.candidate.entity.ApplicationStage;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.repository.ApplicationPipelineRepository;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.repository.ApplicationReviewRepository;
import com.group2.rms.candidate.storage.CvStorage;
import com.group2.rms.candidate.validator.ApplicationAccess;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.web.TimelineItem;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Service xử lý nghiệp vụ danh sách và chi tiết hồ sơ ứng tuyển cho người tuyển dụng.
 */
@Service
@RequiredArgsConstructor
public class ApplicationPipelineService {

    private static final int FACTS_BATCH = 2000;

    private final ApplicationRepository applicationRepository;
    private final ApplicationPipelineRepository pipelineRepository;
    private final ApplicationReviewRepository reviewRepository;
    private final ApplicationAccess applicationAccess;
    private final CvStorage cvStorage;
    private final JobPostingRepository jobPostingRepository;

    /**
     * Lấy danh sách hồ sơ ứng tuyển kèm phân trang và số lượng hồ sơ theo từng giai đoạn.
     */
    @Transactional(readOnly = true)
    public ApplicationListResponse list(ApplicationSearch search, Pageable pageable) {
        User actor = applicationAccess.actor();
        ApplicationScope scope = applicationAccess.scopeOf(actor);
        ApplicationSearch filter = search.normalized();

        Map<Integer, ApplicationProgressResponse> progress = progressInScope(scope, filter);
        Map<ApplicationStage, Long> stageCounts = stageCounts(progress);

        ApplicationStage stage = filter.stageFilter();
        List<Integer> matching = progress.entrySet().stream()
                .filter(entry -> stage == null || entry.getValue().stage() == stage)
                .map(Map.Entry::getKey)
                .toList();
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        List<Integer> pageIds = matching.stream()
                .skip(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .toList();
        List<ApplicationPipelineResponse> rows = rowsWithProgress(pageIds, progress, actor);

        String postingTitle = filter.jobPostingId() == null ? null
                : jobPostingRepository.findById(filter.jobPostingId())
                        .map(JobPosting::getPostingTitle)
                        .orElse(null);
        return new ApplicationListResponse(
                new PageImpl<>(rows, pageRequest, matching.size()),
                stageCounts,
                pipelineRepository.findPostingOptions(
                        scope.allDepartments(), scope.managerUserId(), scope.departmentId()),
                filter,
                postingTitle,
                applicationAccess.canOpenJobPosting(actor),
                !scope.allDepartments(),
                actor.getRole() == null ? null : actor.getRole().getRoleName());
    }

    /**
     * Thống kê khối lượng công việc và số lượng hồ sơ theo từng giai đoạn cho dashboard.
     */
    @Transactional(readOnly = true)
    public OwnerWorkloadResponse ownerWorkload(String ownerLabel, Collection<ApplicationStage> stages) {
        User actor = applicationAccess.actor();
        ApplicationScope scope = applicationAccess.scopeOf(actor);
        ApplicationSearch filter = new ApplicationSearch(null, null, null, null).normalized();

        Map<Integer, ApplicationProgressResponse> progress = progressInScope(scope, filter);
        List<Integer> ownedIds = progress.entrySet().stream()
                .filter(entry -> stages.contains(entry.getValue().stage())
                        && Objects.equals(ownerLabel, entry.getValue().ownerLabel()))
                .map(Map.Entry::getKey)
                .toList();

        Map<ApplicationStage, List<ApplicationPipelineResponse>> ownedRows = new EnumMap<>(ApplicationStage.class);
        for (ApplicationStage stage : stages) {
            ownedRows.put(stage, new ArrayList<>());
        }
        for (ApplicationPipelineResponse row : rowsWithProgress(ownedIds, progress, actor)) {
            ownedRows.get(progress.get(row.applicationId()).stage()).add(row);
        }
        return new OwnerWorkloadResponse(stageCounts(progress), ownedRows);
    }

    /**
     * Lấy thông tin chi tiết hồ sơ ứng tuyển, timeline và các quyền thao tác.
     */
    @Transactional(readOnly = true)
    public ApplicationDetailResponse detail(Integer applicationId) {
        User actor = applicationAccess.actor();
        Application application = applicationRepository.findDetailById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));
        applicationAccess.requireView(actor, application);

        Candidate candidate = application.getCandidate();
        User account = candidate.getAccount();
        JobPosting job = application.getJobPosting();
        JobRequisition requisition = job.getRequisition();
        String status = application.getApplicationStatus();
        ApplicationProgressFacts facts = knownFacts(applicationId, status, progressFacts(List.of(applicationId)));
        ApplicationProgressResponse progress = ApplicationProgressResolver.resolve(facts, LocalDateTime.now());
        boolean cancelledProposal = pipelineRepository.countTalentPoolCancellation(applicationId) > 0;

        return new ApplicationDetailResponse(
                application.getApplicationId(),
                candidate.getCandidateId(),
                candidate.isPotential(),
                account.getFullName(),
                account.getEmail(),
                account.getPhoneNumber(),
                candidate.getLinkedInUrl(),
                candidate.getPortfolioUrl(),
                candidate.getDateOfBirth(),
                candidate.getGender(),
                candidate.getAddress(),
                requisition.getRequisitionId(),
                job.getJobPostingId(),
                job.getPostingTitle(),
                requisition.getDepartment() == null ? null : requisition.getDepartment().getDepartmentName(),
                application.getSubmissionDate(),
                status,
                cancelledProposal ? "Đề xuất đã kết thúc" : progress.detailLabel(),
                cancelledProposal ? "badge--neutral" : progress.badge(),
                timeline(application, account),
                ApplicationStepper.items(progress),
                facts.interviewId(),
                new ApplicationDetailResponse.Actions(
                        applicationAccess.canReview(actor, application),
                        applicationAccess.canScheduleInterview(actor, progress),
                        applicationAccess.canCreateOffer(actor, progress),
                        applicationAccess.canOpenJobPosting(actor)),
                actor.getRole() == null ? null : actor.getRole().getRoleName(),
                "HM_Suggested".equals(status) || cancelledProposal);
    }

    /**
     * Kiểm tra đơn ứng tuyển có bị dừng do tin tuyển dụng bị đóng hay không.
     */
    @Transactional(readOnly = true)
    public boolean closedByPosting(Integer id) {
        Application application = applicationRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ."));
        applicationAccess.requireView(applicationAccess.actor(), application);
        return ApplicationStatusService.CANCELLED.equals(application.getApplicationStatus());
    }

    /**
     * Lấy thông tin và tài nguyên CV đính kèm của hồ sơ ứng tuyển.
     */
    @Transactional(readOnly = true)
    public ApplicationCv cv(Integer applicationId) {
        User actor = applicationAccess.actor();
        Application application = applicationRepository.findDetailById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));
        applicationAccess.requireView(actor, application);

        return ApplicationCv.of(application.getAppliedCvUrl(), cvStorage)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV của hồ sơ này."));
    }

    private Map<Integer, ApplicationProgressFacts> progressFacts(List<Integer> applicationIds) {
        Map<Integer, ApplicationProgressFacts> facts = new HashMap<>();
        for (int from = 0; from < applicationIds.size(); from += FACTS_BATCH) {
            List<Integer> batch = applicationIds.subList(from, Math.min(from + FACTS_BATCH, applicationIds.size()));
            applicationRepository.findProgressFacts(batch).stream()
                    .map(ApplicationProgressFacts::from)
                    .forEach(row -> facts.put(row.applicationId(), row));
        }
        return facts;
    }

    private Map<Integer, ApplicationProgressResponse> progressInScope(
            ApplicationScope scope, ApplicationSearch filter) {
        List<Integer> ids = pipelineRepository.findPipelineIds(
                scope.allDepartments(), scope.managerUserId(), scope.departmentId(),
                filter.jobPostingId(), filter.status(), filter.keyword());
        Map<Integer, ApplicationProgressFacts> facts = progressFacts(ids);
        LocalDateTime now = LocalDateTime.now();
        Map<Integer, ApplicationProgressResponse> progress = new LinkedHashMap<>();
        ids.stream()
                .filter(facts::containsKey)
                .forEach(id -> progress.put(id, ApplicationProgressResolver.resolve(facts.get(id), now)));
        return progress;
    }

    private static Map<ApplicationStage, Long> stageCounts(Map<Integer, ApplicationProgressResponse> progress) {
        Map<ApplicationStage, Long> stageCounts = new EnumMap<>(ApplicationStage.class);
        for (ApplicationStage stage : ApplicationStage.values()) {
            stageCounts.put(stage, 0L);
        }
        progress.values().forEach(p -> stageCounts.merge(p.stage(), 1L, Long::sum));
        return stageCounts;
    }

    private List<ApplicationPipelineResponse> rowsWithProgress(
            List<Integer> ids, Map<Integer, ApplicationProgressResponse> progress, User actor) {
        return pageRows(ids).stream()
                .map(row -> {
                    ApplicationProgressResponse rowProgress = progress.get(row.applicationId());
                    return row.withProgress(
                            rowProgress,
                            applicationAccess.canScheduleInterview(actor, rowProgress),
                            applicationAccess.canCreateOffer(actor, rowProgress));
                })
                .toList();
    }

    private List<ApplicationPipelineResponse> pageRows(List<Integer> pageIds) {
        if (pageIds.isEmpty()) {
            return List.of();
        }
        Map<Integer, ApplicationPipelineResponse> byId = new HashMap<>();
        for (int from = 0; from < pageIds.size(); from += FACTS_BATCH) {
            pipelineRepository.findPipelineRows(pageIds.subList(from, Math.min(from + FACTS_BATCH, pageIds.size())))
                    .forEach(row -> byId.put(row.applicationId(), row));
        }
        return pageIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private static ApplicationProgressFacts knownFacts(
            Integer applicationId, String status, Map<Integer, ApplicationProgressFacts> facts) {
        return facts.getOrDefault(applicationId,
                new ApplicationProgressFacts(applicationId, status, null, null, null, 0, 0, null, null));
    }

    private List<TimelineItem> timeline(Application application, User account) {
        Integer id = application.getApplicationId();
        List<TimelineItem> items = new ArrayList<>();
        var poolEvents = pipelineRepository.findTalentPoolEvents(id);
        boolean hrRecommendation = poolEvents.stream()
                .anyMatch(event -> "TalentPoolHrRecommended".equals(event.getKind()));
        for (var event : poolEvents) {
            String title = switch (event.getKind()) {
                case "TalentPoolHrRecommended" -> "HR gửi gợi ý cho HM";
                case "TalentPoolSuggested" -> hrRecommendation
                        ? "HM chấp nhận gợi ý — chờ HR liên hệ"
                        : "HM đề xuất";
                case "TalentPoolActivated" -> "HR xác nhận ứng viên đồng ý dùng CV cũ";
                case "TalentPoolCandidateSubmitted" -> "Ứng viên nộp CV mới";
                default -> "Kết thúc đề xuất";
            };
            items.add(new TimelineItem(event.getAt(), title, event.getActor(), null, "neutral"));
        }
        if (poolEvents.isEmpty() && "HM_Suggested".equals(application.getApplicationStatus())) {
            items.add(new TimelineItem(
                    application.getSubmissionDate(),
                    "HM đề xuất",
                    null,
                    "Chờ HR liên hệ và xác nhận ứng viên đồng ý ứng tuyển.",
                    "neutral"));
        } else if (poolEvents.isEmpty()) {
            items.add(new TimelineItem(
                    application.getSubmissionDate(), "Nộp hồ sơ", account.getFullName(), null, "neutral"));
        }
        var reviewHistory = reviewRepository.findWithReviewerByApplicationId(id);
        Integer originReviewId = hrRecommendation ? reviewHistory.stream()
                .filter(r -> "HR".equals(r.getReviewerRole()) && "Hold".equals(r.getDecision()))
                .map(ApplicationReview::getReviewId)
                .min(Integer::compareTo)
                .orElse(null) : null;
        Integer hmRecommendationDecisionId = hrRecommendation ? reviewHistory.stream()
                .filter(r -> "HiringManager".equals(r.getReviewerRole())
                        && Set.of("Pass", "Fail").contains(r.getDecision()))
                .map(ApplicationReview::getReviewId)
                .min(Integer::compareTo)
                .orElse(null) : null;
        reviewHistory.stream()
                .filter(review -> (originReviewId == null || !originReviewId.equals(review.getReviewId()))
                        && (hmRecommendationDecisionId == null
                                || !hmRecommendationDecisionId.equals(review.getReviewId())))
                .forEach(review -> items.add(new TimelineItem(
                        review.getReviewedAt(),
                        ApplicationStatusLabels.reviewerRoleLabel(review.getReviewerRole()) + ": "
                                + ApplicationStatusLabels.decisionLabel(review.getDecision()),
                        review.getReviewer().getFullName(),
                        review.getComments(),
                        ApplicationStatusLabels.decisionTone(review.getDecision()))));
        pipelineRepository.findInterviewEvents(id).forEach(event -> items.add(new TimelineItem(
                event.getAt(), "Lịch phỏng vấn: " + event.getStatus(), null, null, "neutral")));
        pipelineRepository.findInterviewResultEvents(id).forEach(event -> items.add(new TimelineItem(
                event.getAt(),
                "Kết quả phỏng vấn: " + ApplicationStatusLabels.interviewResultLabel(event.getStatus()),
                null,
                null,
                ApplicationStatusLabels.interviewResultTone(event.getStatus()))));
        pipelineRepository.findOfferEvents(id).forEach(event -> items.add(new TimelineItem(
                event.getAt(),
                "Thư mời: " + ApplicationStatusLabels.offerLabel(event.getStatus()),
                null,
                null,
                "success")));
        if (ApplicationStatusService.CANCELLED.equals(application.getApplicationStatus())) {
            items.add(new TimelineItem(
                    application.getUpdatedAt(),
                    "Đóng đợt tuyển dụng",
                    "Hệ thống",
                    "Đợt tuyển dụng đã kết thúc, hồ sơ dừng xử lý",
                    "neutral"));
        }
        items.sort(Comparator.comparing(TimelineItem::at, Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }
}
