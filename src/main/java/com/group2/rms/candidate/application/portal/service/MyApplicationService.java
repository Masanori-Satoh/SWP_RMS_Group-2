package com.group2.rms.candidate.application.portal.service;

import com.group2.rms.candidate.application.portal.dto.MyApplicationListResponse;
import com.group2.rms.candidate.application.portal.dto.MyApplicationResponse;
import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.application.portal.exception.ApplicationWithdrawalException;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.candidate.application.portal.repository.MyApplicationRepository;
import com.group2.rms.candidate.application.portal.repository.MyApplicationRepository.MyApplicationMarkRow;
import com.group2.rms.candidate.application.portal.repository.MyApplicationRepository.MyInterviewRow;
import com.group2.rms.candidate.application.portal.repository.MyApplicationRepository.MyOfferRow;
import com.group2.rms.candidate.application.portal.service.MyApplicationJourney.Facts;
import com.group2.rms.candidate.application.portal.service.MyApplicationJourney.Interview;
import com.group2.rms.candidate.application.portal.service.MyApplicationJourney.Mark;
import com.group2.rms.candidate.application.portal.service.MyApplicationJourney.Offer;
import com.group2.rms.candidate.domain.rule.ApplicationStatusService;
import com.group2.rms.candidate.infra.cv.ApplicationCv;
import com.group2.rms.candidate.infra.cv.CvStorage;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Đơn của chính ứng viên đang đăng nhập ("Đơn của tôi": danh sách, chi tiết, xem lại CV) và rút đơn (UC 39).
 * Rút đơn: schema không có trạng thái {@code Withdrawn} nên đơn chuyển {@code Rejected}
 * và ghi một dòng {@code AuditLog} để phân biệt "đã rút" với "bị loại".
 */
@Service
@RequiredArgsConstructor
public class MyApplicationService {

    /** Chỉ rút được khi đơn còn ở vòng đầu, HR chưa duyệt (SRS: Applied, Screening). */
    static final Set<String> WITHDRAWABLE_STATUSES = Set.of("Applied", "AI_Screened");

    /** Tab của danh sách; giá trị khác (gõ tay trên URL) là "Tất cả". */
    public static final String TAB_ALL = "all";
    public static final String TAB_ACTIVE = "active";
    public static final String TAB_ENDED = "ended";

    private final ApplicationRepository applications;
    private final MyApplicationRepository myApplications;
    private final CurrentUserService currentUserService;
    private final CvStorage cvStorage;

    /**
     * Thứ tự danh sách (dunglt 27/10): nhóm 1 (có phỏng vấn / thư mời sắp tới, sự kiện gần nhất trước, thư mời không hạn
     * sau cùng nhóm) → nhóm 2 (đang xử lý) → nhóm 3 (đã kết thúc, luôn cuối). Trong nhóm: mới có chuyển biến trước, rồi
     * đơn nộp sau, rồi id lớn hơn (thứ tự ổn định cho phân trang sau này).
     */
    static final Comparator<MyApplicationResponse> DISPLAY_ORDER =
            Comparator.comparingInt(MyApplicationResponse::priority)
                    .thenComparing(MyApplicationResponse::upcomingAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(MyApplicationResponse::lastActivityAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(MyApplicationResponse::submissionDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(MyApplicationResponse::applicationId, Comparator.reverseOrder());

    /**
     * Danh sách đơn của ứng viên đang đăng nhập: thẻ của tab {@code tab} (lọc phía server để sau này thêm phân trang
     * vẫn đúng), số đơn mỗi tab, và mọi đơn gấp cho banner (hạn gần nhất trước).
     *
     * @param tab {@value #TAB_ACTIVE}, {@value #TAB_ENDED}; giá trị khác hoặc {@code null} là tất cả
     */
    @Transactional(readOnly = true)
    public MyApplicationListResponse list(String tab) {
        List<MyApplicationResponse> all = cards(currentUserService.requireUser().getUserId());
        String selected = TAB_ACTIVE.equals(tab) || TAB_ENDED.equals(tab) ? tab : TAB_ALL;
        List<MyApplicationResponse> shown = switch (selected) {
            case TAB_ACTIVE -> all.stream().filter(card -> !card.ended()).toList();
            case TAB_ENDED -> all.stream().filter(MyApplicationResponse::ended).toList();
            default -> all;
        };
        int ended = (int) all.stream().filter(MyApplicationResponse::ended).count();
        List<MyApplicationResponse> urgent = all.stream()
                .filter(MyApplicationResponse::urgent)
                .sorted(Comparator.comparing(MyApplicationResponse::urgentUntil))
                .toList();
        return new MyApplicationListResponse(shown, selected, all.size(), all.size() - ended, ended, urgent);
    }

    /**
     * Trang chi tiết một đơn của chính mình (L8). Đi cùng đường với danh sách để chữ, bước, việc gấp chỉ tính ở một
     * chỗ; ứng viên thường chỉ có vài đơn nên đọc cả bộ không đáng kể.
     */
    @Transactional(readOnly = true)
    public MyApplicationResponse detail(Integer applicationId) {
        return cards(currentUserService.requireUser().getUserId()).stream()
                .filter(card -> card.applicationId().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));
    }

    /** CV đã nộp của một đơn của chính mình; đơn người khác hoặc không có CV thì 404. */
    @Transactional(readOnly = true)
    public ApplicationCv cv(Integer applicationId) {
        Application application = myApplications.findOwnedById(applicationId, currentUserService.requireUser().getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));
        return ApplicationCv.of(application.getAppliedCvUrl(), cvStorage)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV của hồ sơ này."));
    }

    /**
     * Mọi thẻ đơn của ứng viên {@code userId}, theo {@link #DISPLAY_ORDER}. Dữ kiện lịch, offer, dấu dừng đọc theo
     * {@code userId} (mỗi loại một câu cho cả trang, không một câu cho từng đơn), gom theo id đơn rồi quy đổi bằng
     * {@link MyApplicationJourney}.
     */
    private List<MyApplicationResponse> cards(Integer userId) {
        List<Application> owned = myApplications.findOwnedByUserId(userId);
        if (owned.isEmpty()) {
            return List.of();
        }
        Map<Integer, List<Interview>> interviews = myApplications.findMyInterviews(userId).stream()
                .collect(Collectors.groupingBy(MyInterviewRow::getApplicationId, Collectors.mapping(
                        row -> new Interview(row.getStartTime(), row.getFormat(), row.getStatus(), row.getScheduledAt()),
                        Collectors.toList())));
        Map<Integer, Offer> offers = myApplications.findMyVisibleOffers(userId).stream()
                .collect(Collectors.toMap(MyOfferRow::getApplicationId,
                        row -> new Offer(row.getOfferId(), row.getStatus(), row.getResponseDeadline(), row.getAt()),
                        (first, second) -> first));
        Map<Integer, List<Mark>> marks = myApplications.findMyApplicationMarks(userId).stream()
                .collect(Collectors.groupingBy(MyApplicationMarkRow::getApplicationId, Collectors.mapping(
                        row -> new Mark(row.getKind(), row.getOldStatus(), row.getAt()),
                        Collectors.toList())));
        LocalDateTime now = LocalDateTime.now();
        return owned.stream()
                .map(application -> {
                    Integer id = application.getApplicationId();
                    Facts facts = new Facts(application.getApplicationStatus(), application.getSubmissionDate(),
                            interviews.getOrDefault(id, List.of()), offers.get(id), marks.getOrDefault(id, List.of()));
                    return toResponse(application, MyApplicationJourney.resolve(facts, now), now);
                })
                .sorted(DISPLAY_ORDER)
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
        Application application = myApplications.findOwnedById(applicationId, user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ứng tuyển."));

        String status = application.getApplicationStatus();
        if (!WITHDRAWABLE_STATUSES.contains(status)) {
            throw new ApplicationWithdrawalException(
                    "Hồ sơ đã qua vòng sơ tuyển nên không thể rút. Vui lòng liên hệ bộ phận tuyển dụng.");
        }
        if (applications.transition(applicationId, WITHDRAWABLE_STATUSES, ApplicationStatusService.REJECTED) == 0) {
            throw new ApplicationWithdrawalException("Hồ sơ vừa được cập nhật. Vui lòng tải lại trang.");
        }
        applications.recordWithdrawal(user.getUserId(), applicationId, status);
        return application.getJobPosting().getPostingTitle();
    }

    /** Mã đơn hiển thị trên thẻ: {@code #CAN-} + id đệm 4 số. Chỉ để ứng viên nhắc khi liên hệ, không lưu DB. */
    static String applicationCode(Integer applicationId) {
        return String.format("#CAN-%04d", applicationId);
    }

    private static MyApplicationResponse toResponse(Application application, MyApplicationJourney.View view,
                                                    LocalDateTime now) {
        JobPosting job = application.getJobPosting();
        String department = job.getRequisition() == null || job.getRequisition().getDepartment() == null
                ? null : job.getRequisition().getDepartment().getDepartmentName();
        String cvUrl = application.getAppliedCvUrl();
        return new MyApplicationResponse(
                application.getApplicationId(),
                job.getJobPostingId(),
                job.getPostingTitle(),
                department,
                applicationCode(application.getApplicationId()),
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
