package com.group2.rms.candidate.portal.service;

import com.group2.rms.candidate.portal.dto.MyApplicationListResponse;
import com.group2.rms.candidate.portal.dto.MyApplicationResponse;
import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.portal.exception.ApplicationWithdrawalException;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.candidate.domain.repository.ApplicationRepository.MyApplicationMarkRow;
import com.group2.rms.candidate.domain.repository.ApplicationRepository.MyInterviewRow;
import com.group2.rms.candidate.domain.repository.ApplicationRepository.MyOfferRow;
import com.group2.rms.candidate.portal.service.MyApplicationJourney.Facts;
import com.group2.rms.candidate.portal.service.MyApplicationJourney.Interview;
import com.group2.rms.candidate.portal.service.MyApplicationJourney.Mark;
import com.group2.rms.candidate.portal.service.MyApplicationJourney.Offer;
import com.group2.rms.candidate.pipeline.service.ApplicationReviewService;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Đơn của chính ứng viên đang đăng nhập (5.1.38 My Applications) và rút đơn (UC 39).
 * Rút đơn: schema không có trạng thái {@code Withdrawn} nên đơn chuyển {@code Rejected}
 * và ghi một dòng {@code AuditLog} để phân biệt "đã rút" với "bị loại".
 */
@Service
@RequiredArgsConstructor
public class MyApplicationService {

    /** Chỉ rút được khi đơn còn ở vòng đầu, HR chưa duyệt (SRS: Applied, Screening). */
    static final Set<String> WITHDRAWABLE_STATUSES = Set.of("Applied", "AI_Screened");

    private final ApplicationRepository applications;
    private final CurrentUserService currentUserService;

    /** Số bước trên dải hướng dẫn (Nộp CV → Xét CV → Phỏng vấn → Thư mời). */
    static final int STEP_COUNT = 4;

    /** Mới cập nhật nhất trước; cùng lúc thì đơn nộp sau, rồi id lớn hơn trước (thứ tự ổn định cho phân trang sau này). */
    static final Comparator<MyApplicationResponse> NEWEST_ACTIVITY_FIRST =
            Comparator.comparing(MyApplicationResponse::lastActivityAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(MyApplicationResponse::submissionDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(MyApplicationResponse::applicationId, Comparator.reverseOrder());

    /**
     * Trang My Applications của ứng viên đang đăng nhập: thẻ đơn mới cập nhật nhất trước, lọc theo bước nếu có, kèm
     * số đơn ở từng bước cho dải hướng dẫn.
     *
     * @param step bước cần lọc (1–4); {@code null} hoặc ngoài khoảng thì xem tất cả
     */
    @Transactional(readOnly = true)
    public MyApplicationListResponse list(Integer step) {
        List<MyApplicationResponse> all = cards(currentUserService.requireUser().getUserId());
        Map<Integer, Long> counts = new TreeMap<>();
        for (int i = 1; i <= STEP_COUNT; i++) {
            counts.put(i, 0L);
        }
        all.forEach(card -> counts.merge(card.stepNumber(), 1L, Long::sum));
        Integer selected = step != null && step >= 1 && step <= STEP_COUNT ? step : null;
        List<MyApplicationResponse> shown = selected == null ? all
                : all.stream().filter(card -> card.stepNumber() == selected).toList();
        return new MyApplicationListResponse(shown, counts, selected, all.size());
    }

    /**
     * Mọi thẻ đơn của ứng viên {@code userId}, mới cập nhật nhất trước. Dữ kiện lịch, offer, dấu dừng đọc theo
     * {@code userId} (mỗi loại một câu cho cả trang, không một câu cho từng đơn), gom theo id đơn rồi quy đổi bằng
     * {@link MyApplicationJourney}.
     */
    private List<MyApplicationResponse> cards(Integer userId) {
        List<Application> owned = applications.findOwnedByUserId(userId);
        if (owned.isEmpty()) {
            return List.of();
        }
        Map<Integer, List<Interview>> interviews = applications.findMyInterviews(userId).stream()
                .collect(Collectors.groupingBy(MyInterviewRow::getApplicationId, Collectors.mapping(
                        row -> new Interview(row.getStartTime(), row.getFormat(), row.getStatus()),
                        Collectors.toList())));
        Map<Integer, Offer> offers = applications.findMyVisibleOffers(userId).stream()
                .collect(Collectors.toMap(MyOfferRow::getApplicationId,
                        row -> new Offer(row.getOfferId(), row.getStatus(), row.getResponseDeadline(), row.getAt()),
                        (first, second) -> first));
        Map<Integer, List<Mark>> marks = applications.findMyApplicationMarks(userId).stream()
                .collect(Collectors.groupingBy(MyApplicationMarkRow::getApplicationId, Collectors.mapping(
                        row -> new Mark(row.getKind(), row.getOldStatus(), row.getAt()),
                        Collectors.toList())));
        LocalDateTime now = LocalDateTime.now();
        return owned.stream()
                .map(application -> {
                    Integer id = application.getApplicationId();
                    Facts facts = new Facts(application.getApplicationStatus(), application.getSubmissionDate(),
                            interviews.getOrDefault(id, List.of()), offers.get(id), marks.getOrDefault(id, List.of()));
                    return toResponse(application, MyApplicationJourney.resolve(facts, now));
                })
                .sorted(NEWEST_ACTIVITY_FIRST)
                .toList();
    }

    /**
     * Rút một đơn của chính mình.
     *
     * @return tên vị trí, cho thông báo thành công
     */
    @Transactional
    public String withdraw(Integer applicationId) {
        User user = currentUserService.requireUser();
        Application application = applications.findOwnedById(applicationId, user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));

        String status = application.getApplicationStatus();
        if (!WITHDRAWABLE_STATUSES.contains(status)) {
            throw new ApplicationWithdrawalException(
                    "Hồ sơ đã qua vòng sơ tuyển nên không thể rút. Vui lòng liên hệ bộ phận tuyển dụng.");
        }
        if (applications.transition(applicationId, WITHDRAWABLE_STATUSES, ApplicationReviewService.REJECTED) == 0) {
            throw new ApplicationWithdrawalException("Hồ sơ vừa được cập nhật. Vui lòng tải lại trang.");
        }
        applications.recordWithdrawal(user.getUserId(), applicationId, status);
        return application.getJobPosting().getPostingTitle();
    }

    private static MyApplicationResponse toResponse(Application application, MyApplicationJourney.View view) {
        return new MyApplicationResponse(
                application.getApplicationId(),
                application.getJobPosting().getJobPostingId(),
                application.getJobPosting().getPostingTitle(),
                application.getSubmissionDate(),
                view.lastActivityAt(),
                view.stepNumber(),
                view.step().label(),
                view.step().state(),
                view.statusLabel(),
                view.statusNote(),
                view.tone(),
                view.actionLabel(),
                view.actionHref(),
                WITHDRAWABLE_STATUSES.contains(application.getApplicationStatus()),
                view.journey());
    }
}
