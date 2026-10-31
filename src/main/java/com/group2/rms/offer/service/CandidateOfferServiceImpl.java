package com.group2.rms.offer.service;

import com.group2.rms.candidate.domain.rule.ApplicationStatusService;
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
public class CandidateOfferServiceImpl implements CandidateOfferService {

    static final String SENT = "Sent_Candidate";
    static final String ACCEPTED = "Accepted";
    static final String DECLINED = "Declined";

    /** Offer nội bộ (Draft, Pending_Director, Director_*) không hiện cho ứng viên. */
    static final Set<String> CANDIDATE_VISIBLE_STATUSES = Set.of(SENT, ACCEPTED, DECLINED);

    private final CandidateOfferRepository offers;
    private final CurrentUserService currentUserService;
    private final ApplicationStatusService applicationStatusService;
    private final com.group2.rms.notification.NotificationService systemNotificationService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public CandidateOfferServiceImpl(CandidateOfferRepository offers,
                                    CurrentUserService currentUserService,
                                    ApplicationStatusService applicationStatusService,
                                    com.group2.rms.notification.NotificationService systemNotificationService,
                                    Clock clock) {
        this.offers = offers;
        this.currentUserService = currentUserService;
        this.applicationStatusService = applicationStatusService;
        this.systemNotificationService = systemNotificationService;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public CandidateOfferServiceImpl(CandidateOfferRepository offers,
                                    CurrentUserService currentUserService,
                                    ApplicationStatusService applicationStatusService,
                                    com.group2.rms.notification.NotificationService systemNotificationService) {
        this(offers, currentUserService, applicationStatusService, systemNotificationService, Clock.systemDefaultZone());
    }

    public CandidateOfferServiceImpl(CandidateOfferRepository offers,
                                    CurrentUserService currentUserService,
                                    ApplicationStatusService applicationStatusService) {
        this(offers, currentUserService, applicationStatusService, null, Clock.systemDefaultZone());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidateOfferResponse> list() {
        Integer userId = currentUserService.requireUser().getUserId();
        List<OfferProposal> visibleOffers = offers.findVisibleByUserId(userId, CANDIDATE_VISIBLE_STATUSES);

        return visibleOffers.stream()
                .map(this::toResponse)
                .toList();
    }

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
    public String decline(Integer offerId, String declineReason) {
        return answer(offerId, false, declineReason);
    }

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
        int updated = (cleanReason != null)
                ? offers.answerIfSent(offerId, toStatus, cleanReason)
                : offers.answerIfSent(offerId, toStatus);
        if (updated == 0) {
            throw new OfferResponseException("Thư mời vừa được cập nhật. Vui lòng tải lại trang.");
        }
        applicationStatusService.onOfferAnswered(offer.getApplication().getApplicationId(), accepted);
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
     * Nơi bổ sung ràng buộc trả lời offer (huyenpt). Accept và Decline cùng đi qua đây, ví dụ hạn trả lời,
     * lý do từ chối, chặn theo ngày bắt đầu. Offer phải đang chờ ứng viên trả lời và chưa quá ngày bắt đầu.
     */
    private void requireAnswerable(OfferProposal offer) {
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
        if (!SENT.equals(offer.getOfferStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        boolean deadlinePassed = offer.getResponseDeadline() != null
                && now.isAfter(offer.getResponseDeadline());
        boolean startDatePassed = offer.getExpectedStartDate() != null
                && offer.getExpectedStartDate().isBefore(now.toLocalDate());
        return deadlinePassed || startDatePassed;
    }

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

        String probDuration = (requisition != null && requisition.getProbationDays() != null)
                ? requisition.getProbationDays() + " ngày"
                : "60 ngày";

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
