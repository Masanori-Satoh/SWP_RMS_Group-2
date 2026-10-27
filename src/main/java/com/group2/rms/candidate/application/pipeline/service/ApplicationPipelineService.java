package com.group2.rms.candidate.application.pipeline.service;

import com.group2.rms.candidate.domain.entity.ApplicationReview;
import com.group2.rms.candidate.domain.rule.*;
import com.group2.rms.candidate.infra.cv.ApplicationCv;
import com.group2.rms.candidate.infra.cv.CvStorage;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationDetailResponse;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationListResponse;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationSearch;
import com.group2.rms.candidate.domain.entity.AIScreeningResult;
import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.candidate.domain.repository.AIScreeningResultRepository;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.candidate.application.pipeline.repository.ApplicationPipelineRepository;
import com.group2.rms.candidate.domain.repository.ApplicationReviewRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.web.TimelineItem;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Đọc dữ liệu cho danh sách hồ sơ (5.1.22) và chi tiết hồ sơ (5.1.23), trong phạm vi người xem được phép.
 */
@Service
@RequiredArgsConstructor
public class ApplicationPipelineService {

    private static final Locale VIETNAMESE = Locale.of("vi", "VN");
    private static final int FACTS_BATCH = 2000;

    private final ApplicationRepository applications;
    private final ApplicationPipelineRepository pipelineQueries;
    private final AIScreeningResultRepository screeningResults;
    private final ApplicationReviewRepository reviews;
    private final ApplicationAccess access;
    private final CvStorage cvStorage;
    private final JobPostingRepository jobPostings;

    /**
     * Một trang danh sách kèm số hồ sơ mỗi giai đoạn. Thứ tự do {@code search.sort()} quyết định; sort trong
     * {@code pageable} bị bỏ qua.
     * <p>
     * Giai đoạn tính bằng {@link ApplicationProgressResolver} (cùng chỗ tính nhãn), không viết lại bằng SQL, để số
     * đếm và nhãn không bao giờ lệch nhau. Vì vậy mỗi lần mở trang phải đọc dữ kiện của <b>mọi</b> đơn trong phạm vi
     * và bộ lọc rồi mới đếm, lọc giai đoạn và cắt trang trong Java: ổn với vài nghìn đơn; lớn hơn nhiều thì cần
     * xem lại (ví dụ lưu sẵn giai đoạn).
     */
    @Transactional(readOnly = true)
    public ApplicationListResponse list(ApplicationSearch search, Pageable pageable) {
        User actor = access.actor();
        ApplicationScope scope = access.scopeOf(actor);
        ApplicationSearch filter = search.normalized();

        List<Integer> ids = pipelineQueries.findPipelineIds(
                scope.allDepartments(), scope.managerUserId(), scope.departmentId(),
                filter.jobPostingId(), filter.status(), filter.keyword(), filter.sort());
        Map<Integer, ApplicationProgressFacts> facts = progressFacts(ids);
        LocalDateTime now = LocalDateTime.now();
        Map<Integer, ApplicationProgressResponse> progress = new LinkedHashMap<>();
        ids.stream().filter(facts::containsKey)
                .forEach(id -> progress.put(id, progress(facts.get(id), now)));

        Map<ApplicationStage, Long> stageCounts = new EnumMap<>(ApplicationStage.class);
        for (ApplicationStage stage : ApplicationStage.values()) {
            stageCounts.put(stage, 0L);
        }
        progress.values().forEach(p -> stageCounts.merge(p.stage(), 1L, Long::sum));

        ApplicationStage stage = filter.stageFilter();
        List<Integer> matching = progress.entrySet().stream()
                .filter(entry -> stage == null || entry.getValue().stage() == stage)
                .map(Map.Entry::getKey).toList();
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        List<Integer> pageIds = matching.stream()
                .skip(pageRequest.getOffset()).limit(pageRequest.getPageSize()).toList();
        List<ApplicationPipelineResponse> rows = pageRows(pageIds).stream().map(row -> {
            ApplicationProgressResponse rowProgress = progress.get(row.applicationId());
            return row.withProgress(rowProgress,
                    access.canScheduleInterview(actor, rowProgress), access.canCreateOffer(actor, rowProgress));
        }).toList();

        // Lấy thẳng từ bảng tin: tin chưa có hồ sơ nào (mở từ trang tin) không nằm trong danh sách lựa chọn.
        String postingTitle = filter.jobPostingId() == null ? null
                : jobPostings.findById(filter.jobPostingId()).map(JobPosting::getPostingTitle).orElse(null);
        return new ApplicationListResponse(new PageImpl<>(rows, pageRequest, matching.size()), stageCounts,
                pipelineQueries.findPostingOptions(scope.allDepartments(), scope.managerUserId(), scope.departmentId()),
                filter, postingTitle, access.canOpenJobPosting(actor), !scope.allDepartments(),
                actor.getRole() == null ? null : actor.getRole().getRoleName());
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
        ApplicationProgressFacts facts = knownFacts(applicationId, status, progressFacts(List.of(applicationId)));
        ApplicationProgressResponse progress = progress(facts, LocalDateTime.now());
        boolean cancelledProposal = pipelineQueries.countTalentPoolCancellation(applicationId) > 0;

        return new ApplicationDetailResponse(
                application.getApplicationId(),
                candidate.getCandidateId(), candidate.isPotential(),
                account.getFullName(), account.getEmail(), account.getPhoneNumber(),
                candidate.getLinkedInUrl(), candidate.getPortfolioUrl(),
                candidate.getDateOfBirth(), candidate.getGender(), candidate.getAddress(),
                requisition.getRequisitionId(), requisition.getRequisitionCode(),
                job.getJobPostingId(), job.getPostingTitle(),
                requisition.getDepartment() == null ? null : requisition.getDepartment().getDepartmentName(),
                application.getSubmissionDate(),
                status, cancelledProposal ? "Đề xuất đã kết thúc" : progress.detailLabel(), cancelledProposal ? "badge--neutral" : progress.badge(),
                latest == null ? null : latest.getAiMatchScore(),
                latest == null ? null : latest.getScreenedAt(),
                timeline(application, account, scores),
                ApplicationStepper.items(progress),
                facts.interviewId(),
                new ApplicationDetailResponse.Actions(
                        access.canReview(actor, application),
                        access.canRescreen(actor, application),
                        access.canScheduleInterview(actor, progress),
                        access.canCreateOffer(actor, progress),
                        access.canOpenJobPosting(actor)),
                actor.getRole() == null ? null : actor.getRole().getRoleName(),
                "HM_Suggested".equals(status) || cancelledProposal);
    }

    @Transactional(readOnly = true)
    public boolean closedByPosting(Integer id) {
        Application application = applications.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ."));
        access.requireView(access.actor(), application);
        return pipelineQueries.countPostingClosure(id) > 0;
    }

    /** Phân biệt đóng đợt thật (audit) với hồ sơ bị loại riêng lẻ. */
    private ApplicationProgressResponse progress(ApplicationProgressFacts facts, LocalDateTime now) {
        if ("Rejected".equals(facts.applicationStatus()) && pipelineQueries.countPostingClosure(facts.applicationId()) > 0) {
            ApplicationProgressStep step = ApplicationProgressStep.POSTING_CLOSED;
            return new ApplicationProgressResponse(step.stage(), step, step.label(), step.owner(), step.tone());
        }
        return ApplicationProgressResolver.resolve(facts, now);
    }

    /** CV của đơn, cùng quyền với trang chi tiết. Dạng {@code AppliedCvUrl}: {@link ApplicationCv#of}. */
    @Transactional(readOnly = true)
    public ApplicationCv cv(Integer applicationId) {
        User actor = access.actor();
        Application application = applications.findDetailById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));
        access.requireView(actor, application);

        return ApplicationCv.of(application.getAppliedCvUrl(), cvStorage)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV của hồ sơ này."));
    }

    /**
     * Dữ kiện trạng thái chi tiết theo id đơn, mỗi lô {@value #FACTS_BATCH} id một câu query (giới hạn ~2100 tham số
     * của SQL Server). Đơn vừa bị xóa giữa hai câu query thì không có trong kết quả.
     */
    private Map<Integer, ApplicationProgressFacts> progressFacts(List<Integer> applicationIds) {
        Map<Integer, ApplicationProgressFacts> facts = new HashMap<>();
        for (int from = 0; from < applicationIds.size(); from += FACTS_BATCH) {
            applications.findProgressFacts(applicationIds.subList(from,
                            Math.min(from + FACTS_BATCH, applicationIds.size()))).stream()
                    .map(ApplicationProgressFacts::from)
                    .forEach(row -> facts.put(row.applicationId(), row));
        }
        return facts;
    }

    /** Các dòng của một trang, đúng thứ tự của {@code pageIds}. */
    private List<ApplicationPipelineResponse> pageRows(List<Integer> pageIds) {
        if (pageIds.isEmpty()) {
            return List.of();
        }
        Map<Integer, ApplicationPipelineResponse> byId = pipelineQueries.findPipelineRows(pageIds).stream()
                .collect(Collectors.toMap(ApplicationPipelineResponse::applicationId, Function.identity()));
        return pageIds.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    /** Thiếu dữ kiện (không xảy ra với đơn vừa đọc) thì tính theo riêng trạng thái đơn, coi như chưa có lịch, offer. */
    private static ApplicationProgressFacts knownFacts(Integer applicationId, String status,
                                                       Map<Integer, ApplicationProgressFacts> facts) {
        return facts.getOrDefault(applicationId,
                new ApplicationProgressFacts(applicationId, status, false, null, null, null, 0, 0, null, null));
    }

    /** Hành trình của đơn, mới nhất trước: nộp, AI chấm, các lượt duyệt, lịch và kết quả phỏng vấn, offer. */
    private List<TimelineItem> timeline(Application application, User account, List<AIScreeningResult> scores) {
        Integer id = application.getApplicationId();
        List<TimelineItem> items = new ArrayList<>();
        var poolEvents = pipelineQueries.findTalentPoolEvents(id);
        boolean hrRecommendation = poolEvents.stream().anyMatch(event -> "TalentPoolHrRecommended".equals(event.getKind()));
        for (var event : poolEvents) {
            String title = switch (event.getKind()) {
                case "TalentPoolHrRecommended" -> "HR gửi gợi ý cho HM";
                case "TalentPoolSuggested" -> hrRecommendation ? "HM chấp nhận gợi ý — chờ HR liên hệ" : "HM đề xuất";
                case "TalentPoolActivated" -> "HR xác nhận ứng viên đồng ý dùng CV cũ";
                case "TalentPoolCandidateSubmitted" -> "Ứng viên nộp CV mới";
                default -> "Kết thúc đề xuất";
            };
            items.add(new TimelineItem(event.getAt(), title, event.getActor(), null, "neutral"));
        }
        if (poolEvents.isEmpty() && "HM_Suggested".equals(application.getApplicationStatus())) {
            items.add(new TimelineItem(application.getSubmissionDate(), "HM đề xuất", null,
                    "Chờ HR liên hệ và xác nhận ứng viên đồng ý ứng tuyển.", "neutral"));
        } else if (poolEvents.isEmpty()) {
            items.add(new TimelineItem(application.getSubmissionDate(), "Nộp hồ sơ", account.getFullName(), null,
                    "neutral"));
        }
        scores.forEach(score -> items.add(new TimelineItem(score.getScreenedAt(),
                "AI chấm điểm: " + formatScore(score.getAiMatchScore()), "Hệ thống", null, "brand")));
        var reviewHistory = reviews.findWithReviewerByApplicationId(id);
        Integer originReviewId = hrRecommendation ? reviewHistory.stream()
                .filter(r -> "HR".equals(r.getReviewerRole()) && "Hold".equals(r.getDecision()))
                .map(ApplicationReview::getReviewId).min(Integer::compareTo).orElse(null) : null;
        Integer hmRecommendationDecisionId = hrRecommendation ? reviewHistory.stream()
                .filter(r -> "HiringManager".equals(r.getReviewerRole()) && java.util.Set.of("Pass", "Fail").contains(r.getDecision()))
                .map(ApplicationReview::getReviewId).min(Integer::compareTo).orElse(null) : null;
        reviewHistory.stream().filter(review -> (originReviewId == null || !originReviewId.equals(review.getReviewId()))
                && (hmRecommendationDecisionId == null || !hmRecommendationDecisionId.equals(review.getReviewId()))).forEach(review -> items.add(new TimelineItem(
                review.getReviewedAt(),
                ApplicationStatusLabels.reviewerRoleLabel(review.getReviewerRole()) + ": "
                        + ApplicationStatusLabels.decisionLabel(review.getDecision()),
                review.getReviewer().getFullName(), review.getComments(),
                ApplicationStatusLabels.decisionTone(review.getDecision()))));
        pipelineQueries.findInterviewEvents(id).forEach(event -> items.add(new TimelineItem(
                event.getAt(), "Lịch phỏng vấn: " + event.getStatus(), null, null, "neutral")));
        pipelineQueries.findInterviewResultEvents(id).forEach(event -> items.add(new TimelineItem(
                event.getAt(), "Kết quả phỏng vấn: " + ApplicationStatusLabels.interviewResultLabel(event.getStatus()),
                null, null, ApplicationStatusLabels.interviewResultTone(event.getStatus()))));
        pipelineQueries.findOfferEvents(id).forEach(event -> items.add(new TimelineItem(
                event.getAt(), "Offer: " + event.getStatus(), null, null, "success")));
        if ("Rejected".equals(application.getApplicationStatus()) && pipelineQueries.countPostingClosure(id) > 0) {
            items.add(new TimelineItem(application.getUpdatedAt(), "Đóng đợt tuyển dụng", "Hệ thống",
                    "Đợt tuyển dụng đã kết thúc, hồ sơ dừng xử lý", "neutral"));
        }
        items.sort(Comparator.comparing(TimelineItem::at, Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }

    private static String formatScore(BigDecimal score) {
        return String.format(VIETNAMESE, "%.2f", score);
    }
}
