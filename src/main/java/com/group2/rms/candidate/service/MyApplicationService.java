package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.MyApplicationListResponse;
import com.group2.rms.candidate.dto.MyApplicationResponse;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.exception.ApplicationWithdrawalException;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.repository.MyApplicationRepository;
import com.group2.rms.candidate.repository.MyApplicationRepository.MyApplicationMarkRow;
import com.group2.rms.candidate.repository.MyApplicationRepository.MyInterviewRow;
import com.group2.rms.candidate.repository.MyApplicationRepository.MyOfferRow;
import com.group2.rms.candidate.service.MyApplicationJourney.Facts;
import com.group2.rms.candidate.service.MyApplicationJourney.Interview;
import com.group2.rms.candidate.service.MyApplicationJourney.Mark;
import com.group2.rms.candidate.service.MyApplicationJourney.Offer;
import com.group2.rms.candidate.storage.ApplicationCv;
import com.group2.rms.candidate.storage.CvStorage;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service quản lý danh sách, chi tiết đơn ứng tuyển và thao tác rút đơn của ứng viên.
 */
@Service
@RequiredArgsConstructor
public class MyApplicationService {

    public static final Set<String> WITHDRAWABLE_STATUSES = Set.of("Applied");

    public static final String TAB_ALL = "all";
    public static final String TAB_ACTIVE = "active";
    public static final String TAB_ENDED = "ended";

    private final ApplicationRepository applicationRepository;
    private final MyApplicationRepository myApplicationRepository;
    private final CurrentUserService currentUserService;
    private final CvStorage cvStorage;

    /**
     * Thứ tự hiển thị ưu tiên danh sách đơn ứng tuyển.
     */
    static final Comparator<MyApplicationResponse> DISPLAY_ORDER = Comparator
            .comparingInt(MyApplicationResponse::priority)
            .thenComparing(MyApplicationResponse::upcomingAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(MyApplicationResponse::lastActivityAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(MyApplicationResponse::submissionDate, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(MyApplicationResponse::applicationId, Comparator.reverseOrder());

    /**
     * Lấy danh sách đơn ứng tuyển của ứng viên theo từng tab kèm số lượng và đơn cần chú ý.
     */
    @Transactional(readOnly = true)
    public MyApplicationListResponse list(String tab) {
        List<MyApplicationResponse> all = cards(currentUserService.requireUser().getUserId());

        int allCount = all.size();
        int endedCount = 0;
        List<MyApplicationResponse> active = new ArrayList<>();
        List<MyApplicationResponse> ended = new ArrayList<>();
        List<MyApplicationResponse> urgent = new ArrayList<>();

        for (MyApplicationResponse card : all) {
            if (card.ended()) {
                endedCount++;
                ended.add(card);
            } else {
                active.add(card);
            }
            if (card.urgent()) {
                urgent.add(card);
            }
        }

        urgent.sort(Comparator.comparing(MyApplicationResponse::urgentUntil));

        String selected = TAB_ACTIVE.equals(tab) || TAB_ENDED.equals(tab) ? tab : TAB_ALL;
        List<MyApplicationResponse> shown = switch (selected) {
            case TAB_ACTIVE -> active;
            case TAB_ENDED -> ended;
            default -> all;
        };

        int activeCount = allCount - endedCount;
        return new MyApplicationListResponse(shown, selected, allCount, activeCount, endedCount, urgent);
    }

    /**
     * Lấy thông tin chi tiết một đơn ứng tuyển của ứng viên.
     */
    @Transactional(readOnly = true)
    public MyApplicationResponse detail(Integer applicationId) {
        return cards(currentUserService.requireUser().getUserId()).stream()
                .filter(card -> card.applicationId().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));
    }

    /**
     * Lấy tài nguyên CV đã nộp của đơn ứng tuyển.
     */
    @Transactional(readOnly = true)
    public ApplicationCv cv(Integer applicationId) {
        Application application = myApplicationRepository
                .findOwnedById(applicationId, currentUserService.requireUser().getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));
        return ApplicationCv.of(application.getAppliedCvUrl(), cvStorage)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV của hồ sơ này."));
    }

    private List<MyApplicationResponse> cards(Integer userId) {
        List<Application> owned = myApplicationRepository.findOwnedByUserId(userId);
        if (owned.isEmpty()) {
            return List.of();
        }
        Map<Integer, List<Interview>> interviews = myApplicationRepository.findMyInterviews(userId).stream()
                .collect(Collectors.groupingBy(
                        MyInterviewRow::getApplicationId,
                        Collectors.mapping(
                                row -> new Interview(
                                        row.getStartTime(),
                                        row.getFormat(),
                                        row.getStatus(),
                                        row.getScheduledAt()),
                                Collectors.toList())));
        Map<Integer, Offer> offers = myApplicationRepository.findMyVisibleOffers(userId).stream()
                .collect(Collectors.toMap(
                        MyOfferRow::getApplicationId,
                        row -> new Offer(
                                row.getOfferId(),
                                row.getStatus(),
                                row.getResponseDeadline(),
                                row.getAt(),
                                row.getExpectedStartDate()),
                        (first, second) -> first));
        Map<Integer, List<Mark>> marks = myApplicationRepository.findMyApplicationMarks(userId).stream()
                .collect(Collectors.groupingBy(
                        MyApplicationMarkRow::getApplicationId,
                        Collectors.mapping(
                                row -> new Mark(row.getKind(), row.getOldStatus(), row.getAt()),
                                Collectors.toList())));
        LocalDateTime now = LocalDateTime.now();
        return owned.stream()
                .map(application -> {
                    Integer id = application.getApplicationId();
                    Facts facts = new Facts(
                            application.getApplicationStatus(),
                            application.getSubmissionDate(),
                            interviews.getOrDefault(id, List.of()),
                            offers.get(id),
                            marks.getOrDefault(id, List.of()));
                    return toResponse(application, MyApplicationJourney.resolve(facts, now), now);
                })
                .sorted(DISPLAY_ORDER)
                .toList();
    }

    /**
     * Thực hiện rút đơn ứng tuyển khi hồ sơ còn ở trạng thái cho phép.
     */
    @Transactional
    public String withdraw(Integer applicationId) {
        User user = currentUserService.requireUser();
        Application application = myApplicationRepository.findOwnedById(applicationId, user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));

        String status = application.getApplicationStatus();
        if (!WITHDRAWABLE_STATUSES.contains(status)) {
            throw new ApplicationWithdrawalException(
                    "Hồ sơ đã qua vòng sơ tuyển nên không thể rút. Vui lòng liên hệ bộ phận tuyển dụng.");
        }
        if (applicationRepository.transition(
                applicationId, WITHDRAWABLE_STATUSES, ApplicationStatusService.WITHDRAWN) == 0) {
            throw new ApplicationWithdrawalException("Hồ sơ vừa được cập nhật. Vui lòng tải lại trang.");
        }
        applicationRepository.recordWithdrawal(user.getUserId(), applicationId, status);
        return application.getJobPosting().getPostingTitle();
    }

    private static MyApplicationResponse toResponse(
            Application application, MyApplicationJourney.View view, LocalDateTime now) {
        JobPosting job = application.getJobPosting();
        String department = job.getRequisition() == null || job.getRequisition().getDepartment() == null
                ? null
                : job.getRequisition().getDepartment().getDepartmentName();
        String cvUrl = application.getAppliedCvUrl();
        String applicationCode = String.format("#CAN-%04d", application.getApplicationId());

        return new MyApplicationResponse(
                application.getApplicationId(),
                job.getJobPostingId(),
                job.getPostingTitle(),
                department,
                applicationCode,
                application.getSubmissionDate(),
                view.lastActivityAt(),
                view.steps(),
                view.statusLabel(),
                view.statusNote(),
                view.tone(),
                view.actionLabel(),
                view.actionHref(),
                view.urgentUntil(),
                view.urgentLabel(),
                view.ended(),
                view.stopped(),
                view.priority(),
                view.upcomingAt(),
                WITHDRAWABLE_STATUSES.contains(application.getApplicationStatus()),
                cvUrl != null && !cvUrl.isBlank(),
                job.isVisibleToCandidates(now),
                view.journey());
    }
}
