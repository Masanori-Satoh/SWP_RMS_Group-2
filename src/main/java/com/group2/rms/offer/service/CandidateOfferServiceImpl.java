package com.group2.rms.offer.service;

import com.group2.rms.candidate.service.ApplicationStatusService;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.offer.dto.CandidateOfferResponse;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.offer.exception.OfferResponseException;
import com.group2.rms.offer.repository.CandidateOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CandidateOfferServiceImpl implements CandidateOfferService {

    static final String SENT = "Sent_Candidate";
    static final String ACCEPTED = "Accepted";
    static final String DECLINED = "Declined";

    /** Offer nội bộ (Draft, Pending_Director, Director_*) không hiện cho ứng viên. */
    static final Set<String> CANDIDATE_VISIBLE_STATUSES = Set.of(SENT, ACCEPTED, DECLINED);

    private final CandidateOfferRepository offers;
    private final CurrentUserService currentUserService;
    private final ApplicationStatusService applicationStatusService;

    @Override
    @Transactional(readOnly = true)
    public List<CandidateOfferResponse> list() {
        Integer userId = currentUserService.requireUser().getUserId();
        return offers.findVisibleByUserId(userId, CANDIDATE_VISIBLE_STATUSES).stream()
                .map(CandidateOfferServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public String accept(Integer offerId) {
        return answer(offerId, true);
    }

    @Override
    @Transactional
    public String decline(Integer offerId) {
        return answer(offerId, false);
    }

    private String answer(Integer offerId, boolean accepted) {
        Integer userId = currentUserService.requireUser().getUserId();
        OfferProposal offer = offers.findVisibleById(offerId, userId, CANDIDATE_VISIBLE_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thư mời nhận việc."));

        requireAnswerable(offer);
        if (offers.answerIfSent(offerId, accepted ? ACCEPTED : DECLINED) == 0) {
            throw new OfferResponseException("Thư mời vừa được cập nhật. Vui lòng tải lại trang.");
        }
        applicationStatusService.onOfferAnswered(offer.getApplication().getApplicationId(), accepted);
        return offer.getApplication().getJobPosting().getPostingTitle();
    }

    /**
     * Nơi bổ sung ràng buộc trả lời offer (huyenpt). Accept và Decline cùng đi qua đây, ví dụ hạn trả lời,
     * lý do từ chối, chặn theo ngày bắt đầu. Hiện chỉ có một luật: offer phải đang chờ ứng viên trả lời.
     * Offer có thuộc ứng viên hay không đã được kiểm ở bước tải offer (không thuộc thì 404).
     */
    private void requireAnswerable(OfferProposal offer) {
        if (!SENT.equals(offer.getOfferStatus())) {
            throw new OfferResponseException("Thư mời này đã được trả lời nên không thể thay đổi.");
        }
    }

    private static CandidateOfferResponse toResponse(OfferProposal offer) {
        String status = offer.getOfferStatus();
        return new CandidateOfferResponse(
                offer.getOfferId(),
                offer.getApplication().getJobPosting().getPostingTitle(),
                offer.getOfferedPositionTitle(),
                offer.getProposedSalary(),
                offer.getProbationSalary(),
                offer.getExpectedStartDate(),
                offer.getWorkLocation(),
                offer.getBenefitsPackage(),
                statusLabel(status),
                statusBadge(status),
                SENT.equals(status));
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case ACCEPTED -> "Đã nhận offer";
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
}
