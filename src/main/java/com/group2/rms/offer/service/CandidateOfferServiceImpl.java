package com.group2.rms.offer.service;

import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.offer.dto.CandidateOfferDetailResponse;
import com.group2.rms.offer.dto.CandidateOfferResponse;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.offer.exception.OfferResponseException;
import com.group2.rms.offer.repository.CandidateOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class CandidateOfferServiceImpl implements CandidateOfferService {

    static final String SENT = "Sent_Candidate";
    static final String ACCEPTED = "Accepted";
    static final String DECLINED = "Declined";

    /** Offer nội bộ (Draft, Pending_Director, Director_*) không hiện cho ứng viên. */
    static final Set<String> CANDIDATE_VISIBLE_STATUSES = Set.of(SENT, ACCEPTED, DECLINED, "Expired");

    private final CandidateOfferRepository offers;
    private final CurrentUserService currentUserService;
    private final ApplicationRepository applications;
    private final com.group2.rms.notification.service.NotificationService systemNotificationService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public CandidateOfferServiceImpl(CandidateOfferRepository offers,
                                    CurrentUserService currentUserService,
                                    ApplicationRepository applications,
                                    com.group2.rms.notification.service.NotificationService systemNotificationService,
                                    Clock clock) {
        this.offers = offers;
        this.currentUserService = currentUserService;
        this.applications = applications;
        this.systemNotificationService = systemNotificationService;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public CandidateOfferServiceImpl(CandidateOfferRepository offers,
                                    CurrentUserService currentUserService,
                                    ApplicationRepository applications,
                                    com.group2.rms.notification.service.NotificationService systemNotificationService) {
        this(offers, currentUserService, applications, systemNotificationService, Clock.systemDefaultZone());
    }

    public CandidateOfferServiceImpl(CandidateOfferRepository offers,
                                    CurrentUserService currentUserService,
                                    ApplicationRepository applications) {
        this(offers, currentUserService, applications, null, Clock.systemDefaultZone());
    }

    /**
     * Lấy thư mời của chính ứng viên đăng nhập, gồm Sent_Candidate, Accepted, Declined và Expired (chỉ xem, không được trả lời).
     * Query ràng buộc UserId tại database; không lấy mọi Offer rồi mới lọc quyền ở JavaScript.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CandidateOfferResponse> list() {
        Integer userId = currentUserService.requireUser().getUserId();
        List<OfferProposal> visibleOffers = offers.findVisibleByUserId(userId, CANDIDATE_VISIBLE_STATUSES);

        return visibleOffers.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Xem chi tiết đúng chủ sở hữu và đánh dấu thông báo thư mời đã đọc.
     * Có thao tác ghi thông báo nên phương thức này không chỉ là giao dịch đọc.
     */
    @Override
    @Transactional
    public CandidateOfferDetailResponse getDetail(Integer offerId) {
        Integer userId = currentUserService.requireUser().getUserId();
        OfferProposal offer = offers.findVisibleById(offerId, userId, CANDIDATE_VISIBLE_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thư mời nhận việc."));

        if (systemNotificationService != null) {
            try {
                systemNotificationService.markOfferNotificationRead(userId, offerId);
            } catch (Exception ignored) {}
        }

        return toDetailResponse(offer);
    }

    @Override
    @Transactional
    public String accept(Integer offerId) {
        return answer(offerId, true, null);
    }

    @Override
    @Transactional
    public String decline(Integer offerId) {
        return answer(offerId, false, null);
    }

    @Override
    @Transactional
    public String decline(Integer offerId, String declineReason) {
        return answer(offerId, false, declineReason);
    }

    /**
     * Luồng chung nhận/từ chối: xác định chủ sở hữu → kiểm tra hạn/trạng thái → cập nhật nếu còn Sent_Candidate.
     * Chỉ một yêu cầu cập nhật được; yêu cầu đến sau nhận lỗi thay vì ghi đè câu trả lời trước đó.
     * Cập nhật hồ sơ Offered → Hired/Withdrawn bằng query sẵn có; 0 dòng thì rollback cả phản hồi Offer.
     * Lý do từ chối của ứng viên là tùy chọn, khác với lý do từ chối đề xuất của Giám đốc.
     */
    private String answer(Integer offerId, boolean accepted, String declineReason) {
        Integer userId = currentUserService.requireUser().getUserId();
        OfferProposal offer = offers.findVisibleById(offerId, userId, CANDIDATE_VISIBLE_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thư mời nhận việc."));

        requireAnswerable(offer);
        String toStatus = accepted ? ACCEPTED : DECLINED;
        String cleanReason = (!accepted && declineReason != null && !declineReason.isBlank()) ? declineReason.trim() : null;
        if (cleanReason != null && cleanReason.length() > 1000) {
            cleanReason = cleanReason.substring(0, 1000);
        }
        LocalDateTime responseTime = LocalDateTime.now(clock);
        int updated = offers.answerIfEligible(offerId, userId, toStatus, cleanReason,
                responseTime, responseTime.toLocalDate());
        if (updated == 0) {
            throw new OfferResponseException("Thư mời đã cập nhật, đã hết hạn hoặc hồ sơ không còn chờ phản hồi. Vui lòng tải lại trang.");
        }
        if (applications.transition(offer.getApplication().getApplicationId(), Set.of("Offered"),
                accepted ? "Hired" : "Withdrawn") != 1) {
            throw new OfferResponseException("Hồ sơ vừa thay đổi. Vui lòng tải lại trang để kiểm tra kết quả.");
        }
        if (systemNotificationService != null) {
            try {
                systemNotificationService.markOfferNotificationRead(userId, offerId);
            } catch (Exception ignored) {}
            try {
                systemNotificationService.notifyOfferResponseFromCandidate(offer, accepted, cleanReason);
            } catch (Exception ignored) {}
        }
        return offer.getApplication().getJobPosting().getPostingTitle();
    }

    /**
     * Chấp nhận phản hồi khi còn Sent_Candidate, chưa qua hạn trả lời và chưa qua ngày bắt đầu dự kiến.
     * Đúng thời điểm bằng hạn vẫn hợp lệ vì code dùng isAfter; ngày bắt đầu bằng hôm nay chưa bị chặn bởi điều kiện ngày.
     * Đây là lỗi giải thích sớm; câu UPDATE còn kiểm tra lại hạn, chủ sở hữu và trạng thái hồ sơ.
     */
    private void requireAnswerable(OfferProposal offer) {
        if ("Expired".equals(offer.getOfferStatus())) {
            throw new OfferResponseException("Thư mời này đã quá hạn phản hồi. Vui lòng liên hệ bộ phận Tuyển dụng để được hỗ trợ.");
        }
        if (!SENT.equals(offer.getOfferStatus())) {
            throw new OfferResponseException("Thư mời này đã được trả lời nên không thể thay đổi.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (offer.getResponseDeadline() != null && now.isAfter(offer.getResponseDeadline())) {
            String deadlineStr = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                    .format(offer.getResponseDeadline());
            throw new OfferResponseException("Thư mời này đã quá thời hạn phản hồi (" + deadlineStr + "). Vui lòng liên hệ bộ phận Tuyển dụng để được hỗ trợ.");
        }
        if (offer.getExpectedStartDate() != null && offer.getExpectedStartDate().isBefore(now.toLocalDate())) {
            String dateStr = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(offer.getExpectedStartDate());
            throw new OfferResponseException("Thư mời này đã quá ngày bắt đầu nhận việc dự kiến (" + dateStr + "). Vui lòng liên hệ bộ phận Tuyển dụng để được hỗ trợ.");
        }
    }

    private boolean isExpired(OfferProposal offer) {
        return offer.isResponseExpiredAt(LocalDateTime.now(clock));
    }

    /**
     * Tạo nhãn và quyền bấm cho UI; hết hạn chỉ đổi nhãn/ẩn thao tác, không ghi OfferStatus=Expired.
     */
    private CandidateOfferResponse toResponse(OfferProposal offer) {
        String status = offer.getOfferStatus();
        boolean expired = isExpired(offer);
        boolean canRespond = SENT.equals(status) && !expired;
        String label = expired ? "Đã quá hạn phản hồi" : statusLabel(status);
        String badge = expired ? "badge--neutral" : statusBadge(status);

        return new CandidateOfferResponse(
                offer.getOfferId(),
                offer.getApplication().getJobPosting().getPostingTitle(),
                offer.getOfferedPositionTitle(),
                offer.getProposedSalary(),
                offer.getProbationSalary(),
                offer.getExpectedStartDate(),
                offer.getWorkLocation(),
                offer.getBenefitsPackage(),
                label,
                badge,
                canRespond,
                offer.getResponsePeriodDays(),
                offer.getResponseDeadline());
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case ACCEPTED -> "Đã nhận việc";
            case DECLINED -> "Đã từ chối";
            default -> "Chờ bạn trả lời";
        };
    }

    private static String statusBadge(String status) {
        return switch (status) {
            case ACCEPTED -> "badge--success";
            case DECLINED -> "badge--neutral";
            default -> "badge--warning";
        };
    }

    private CandidateOfferDetailResponse toDetailResponse(OfferProposal offer) {
        String status = offer.getOfferStatus();
        boolean expired = isExpired(offer);
        boolean canRespond = SENT.equals(status) && !expired;
        String label = expired ? "Đã quá hạn phản hồi" : statusLabel(status);
        String badge = expired ? "badge--neutral" : statusBadge(status);

        var app = offer.getApplication();
        var posting = app != null ? app.getJobPosting() : null;
        var candidate = app != null ? app.getCandidate() : null;
        var user = candidate != null ? candidate.getAccount() : null;
        var requisition = posting != null ? posting.getRequisition() : null;
        var department = requisition != null ? requisition.getDepartment() : null;
        var proposedBy = offer.getProposedBy();

        BigDecimal proposed = offer.getProposedSalary();
        BigDecimal probation = offer.getProbationSalary();
        Integer ratePct = null;
        if (proposed != null && proposed.compareTo(BigDecimal.ZERO) > 0 && probation != null) {
            ratePct = probation.multiply(BigDecimal.valueOf(100))
                    .divide(proposed, 0, java.math.RoundingMode.HALF_UP)
                    .intValue();
        }

        String probDuration = offer.getProbationDays() + " ngày";

        String deptName = department != null ? department.getDepartmentName() : "Toàn công ty";
        String reportTo = (department != null && department.getDepartmentName() != null)
                ? "Trưởng phòng " + department.getDepartmentName()
                : "Ban Giám đốc";

        String empTypeLabel = (requisition != null && requisition.getEmploymentType() != null)
                ? com.group2.rms.requisition.service.RequisitionLabels.employmentType(requisition.getEmploymentType())
                : "Toàn thời gian";
        String employmentType = "Toàn thời gian".equalsIgnoreCase(empTypeLabel)
                ? "Toàn thời gian (08:30 – 17:30, Thứ Hai đến Thứ Sáu)"
                : empTypeLabel;

        String offerCode = offer.getOfferId() != null
                ? String.format("OFFR-%d-%04d",
                        offer.getCreatedAt() != null ? offer.getCreatedAt().getYear() : LocalDateTime.now(clock).getYear(),
                        offer.getOfferId())
                : "OFFR-2026-0000";

        Integer appId = app != null ? app.getApplicationId() : (offer.getOfferId() != null ? offer.getOfferId() : null);
        String appCode = appId != null ? String.format("#CAN-%04d", appId) : "#CAN-0001";
        LocalDateTime subDate = app != null && app.getSubmissionDate() != null
                ? app.getSubmissionDate()
                : (offer.getCreatedAt() != null ? offer.getCreatedAt().minusDays(11) : null);

        return new CandidateOfferDetailResponse(
                offer.getOfferId(),
                offerCode,
                user != null ? user.getFullName() : "Ứng viên",
                user != null ? user.getEmail() : null,
                user != null ? user.getPhoneNumber() : null,
                candidate != null ? candidate.getAddress() : null,
                posting != null ? posting.getPostingTitle() : offer.getOfferedPositionTitle(),
                offer.getOfferedPositionTitle(),
                deptName,
                reportTo,
                employmentType,
                proposed,
                probation,
                ratePct,
                probDuration,
                offer.getExpectedStartDate(),
                offer.getWorkLocation(),
                offer.getBenefitsPackage(),
                status,
                label,
                badge,
                canRespond,
                proposedBy != null ? proposedBy.getFullName() : "Phòng Tuyển dụng Mộc RMS",
                proposedBy != null ? proposedBy.getEmail() : "tuyendung@mocrms.com.vn",
                offer.getDeclineReason(),
                offer.getResponsePeriodDays(),
                offer.getResponseDeadline(),
                offer.getCreatedAt(),
                offer.getUpdatedAt(),
                appId,
                appCode,
                subDate);
    }
}
