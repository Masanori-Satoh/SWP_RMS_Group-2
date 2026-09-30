package com.rms.feature.offer.service;

import com.group2.rms.entity.Application;
import com.group2.rms.entity.OfferApproval;
import com.group2.rms.entity.OfferNegotiation;
import com.group2.rms.entity.OfferProposal;
import com.group2.rms.entity.User;
import com.group2.rms.repository.ApplicationRepository;
import com.group2.rms.repository.OfferApprovalRepository;
import com.group2.rms.repository.OfferNegotiationRepository;
import com.group2.rms.repository.OfferProposalRepository;
import com.group2.rms.repository.UserRepository;
import com.rms.feature.audit.AuditLogService;
import com.rms.feature.notification.NotificationService;
import com.rms.feature.offer.dto.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Implementation cho OfferService xử lý quy trình phê duyệt Offer và phản hồi từ ứng viên.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OfferServiceImpl implements OfferService {

    private final OfferProposalRepository offerProposalRepository;
    private final OfferApprovalRepository offerApprovalRepository;
    private final OfferNegotiationRepository offerNegotiationRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    /**
     * 1. Quy tắc Lương thử việc (BR-OFF-01 / GBR-11):
     * Mức lương thử việc (probationSalary) phải đạt TỐI THIỂU 85% lương chính thức (proposedSalary).
     * 
     * 2. Quy tắc Offer Duy nhất Active (GBR-14):
     * Một Application chỉ được có duy nhất 1 OfferProposal active ở trạng thái Pending/Approved/Sent.
     * Khi tạo Offer mới, nếu ứng viên đã có OfferProposal cũ ở trạng thái "Draft" hoặc "Pending_Director",
     * tự động cập nhật trạng thái Offer cũ thành "Voided" trước khi lưu Offer mới.
     */
    @Override
    public OfferResponseDTO createOfferProposal(CreateOfferRequestDTO dto) {
        // Validation BR-OFF-01: Kiểm tra lương thử việc: 85% <= probationSalary <= proposedSalary
        BigDecimal proposedSalary = dto.getProposedSalary();
        BigDecimal probationSalary = dto.getProbationSalary();

        if (probationSalary == null || proposedSalary == null) {
            throw new IllegalArgumentException("Mức lương chính thức và lương thử việc không được để trống.");
        }
        if (probationSalary.compareTo(proposedSalary) > 0) {
            throw new IllegalArgumentException("Lương thử việc không được cao hơn mức lương chính thức (phải thấp hơn hoặc bằng lương chính thức).");
        }
        if (probationSalary.compareTo(proposedSalary.multiply(new BigDecimal("0.85"))) < 0) {
            throw new IllegalArgumentException("Lương thử việc phải đạt tối thiểu 85% lương chính thức theo Luật Lao động (BR-OFF-01).");
        }

        // Tìm Application
        Application application = applicationRepository.findById(dto.getApplicationId().intValue())
                .orElseThrow(() -> new EntityNotFoundException("Hồ sơ ứng tuyển không tồn tại với ID: " + dto.getApplicationId()));

        // Tìm User đề xuất
        User proposedBy = userRepository.findById(dto.getProposedById().intValue())
                .orElseThrow(() -> new EntityNotFoundException("Người dùng không tồn tại với ID: " + dto.getProposedById()));

        // Kiểm tra xem Hồ sơ ứng tuyển này đã tồn tại Offer Proposal hay chưa
        List<OfferProposal> existingOffers = offerProposalRepository.findByApplicationApplicationId(application.getApplicationId());
        if (existingOffers != null && !existingOffers.isEmpty()) {
            OfferProposal existing = existingOffers.get(0);
            throw new IllegalStateException(String.format(
                    "Hồ sơ ứng tuyển #%d đã có đề xuất Offer trên hệ thống (Offer ID #%d - Trạng thái: %s). Vui lòng đổi sang mã hồ sơ ứng tuyển khác hoặc sử dụng chức năng Chỉnh sửa.",
                    application.getApplicationId(), existing.getOfferId(), existing.getOfferStatus()));
        }

        boolean isDraft = Boolean.TRUE.equals(dto.getIsDraft());
        String initialStatus = isDraft ? "Draft" : "Pending_Director_Approval";

        // Tạo mới OfferProposal
        OfferProposal offerProposal = OfferProposal.builder()
                .application(application)
                .proposedSalary(proposedSalary)
                .probationSalary(probationSalary)
                .probationDays(dto.getProbationDays() != null ? dto.getProbationDays() : 60)
                .proposedPosition(dto.getProposedPosition())
                .workLocation(dto.getWorkLocation())
                .proposedBy(proposedBy)
                .offerStatus(initialStatus)
                .build();

        OfferProposal savedOffer = offerProposalRepository.save(offerProposal);

        // Ghi vết kiểm toán BR-SEC-02 / GBR-12
        auditLogService.log(
                proposedBy.getUserId(),
                "CREATE",
                "OfferProposal",
                savedOffer.getOfferId().toString(),
                null,
                String.format("ProposedSalary: %s, ProbationSalary: %s, Position: %s, Status: %s",
                        savedOffer.getProposedSalary(), savedOffer.getProbationSalary(), savedOffer.getProposedPosition(), savedOffer.getOfferStatus())
        );

        // Chỉ thông báo tới Director nếu không phải là bản thảo (Draft)
        if (!isDraft) {
            try {
                notificationService.sendOfferSubmittedNotificationToDirector(savedOffer);
            } catch (Exception ex) {
                log.error("[OR-10] Gửi thông báo tới Director thất bại cho Offer ID {}: {}", savedOffer.getOfferId(), ex.getMessage());
            }
        }

        return getOfferById(savedOffer.getOfferId().longValue());
    }

    /**
     * 3. Luồng Phê duyệt của Director:
     * Khi Director duyệt "Approved": cập nhật offerStatus = "Approved", lưu bản ghi vào OfferApproval.
     * Khi Director từ chối "Rejected": cập nhật offerStatus = "Rejected", lưu bản ghi vào OfferApproval kèm directorComments.
     */
    @Override
    public OfferResponseDTO processDirectorApproval(Long offerId, DirectorApprovalDTO dto) {
        OfferProposal offerProposal = offerProposalRepository.findById(offerId.intValue())
                .orElseThrow(() -> new EntityNotFoundException("Offer Proposal không tồn tại với ID: " + offerId));

        User director = userRepository.findById(dto.getDirectorId().intValue())
                .orElseThrow(() -> new EntityNotFoundException("Director không tồn tại với ID: " + dto.getDirectorId()));

        String statusInput = dto.getStatus();
        if (!"Approved".equalsIgnoreCase(statusInput) && !"Rejected".equalsIgnoreCase(statusInput)) {
            throw new IllegalArgumentException("Trạng thái phê duyệt không hợp lệ. Chỉ chấp nhận 'Approved' hoặc 'Rejected'.");
        }

        String oldStatus = offerProposal.getOfferStatus();
        String targetStatus = "Approved".equalsIgnoreCase(statusInput) ? "Approved" : "Rejected";
        offerProposal.setOfferStatus(targetStatus);
        offerProposalRepository.save(offerProposal);

        // Lưu lịch sử phê duyệt
        OfferApproval approval = OfferApproval.builder()
                .offerProposal(offerProposal)
                .director(director)
                .status(targetStatus)
                .directorComments(dto.getDirectorComments())
                .approvedAt(LocalDateTime.now())
                .build();
        offerApprovalRepository.save(approval);

        // Ghi vết kiểm toán BR-SEC-02 / GBR-12
        auditLogService.log(
                director.getUserId(),
                "UPDATE",
                "OfferProposal",
                offerId.toString(),
                "Status: " + oldStatus + ", ProposedSalary: " + offerProposal.getProposedSalary(),
                "Status: " + targetStatus + ", ProposedSalary: " + offerProposal.getProposedSalary() + ", Comments: " + dto.getDirectorComments()
        );

        // Khi Director duyệt "Approved": Chuyển tiếp thông báo cho HR để HR kiểm tra và xác nhận phát hành Offer tới ứng viên
        // Ứng viên CHƯA nhận được email trúng tuyển ở bước này; chỉ nhận được sau khi HR xác nhận phát hành Offer.
        if ("Approved".equalsIgnoreCase(targetStatus)) {
            try {
                notificationService.sendOfferApprovedNotificationToHR(offerProposal);
            } catch (Exception ex) {
                log.error("[DIRECTOR_APPROVAL] Gửi thông báo phê duyệt tới HR thất bại cho Offer ID {}: {}", offerId, ex.getMessage());
            }
        }

        return getOfferById(offerId);
    }

    /**
     * 4. Luồng Phản hồi của Candidate (Chỉ được phép khi HR đã phát hành Offer):
     * - Nếu Candidate chọn "Accept": Cập nhật offerStatus = "Accepted"
     * - Nếu Candidate chọn "Decline": Cập nhật offerStatus = "Declined"
     * - Nếu Candidate chọn "Negotiate": Cập nhật offerStatus = "Negotiating"
     */
    @Override
    public OfferResponseDTO processCandidateResponse(Long offerId, CandidateResponseDTO dto) {
        OfferProposal offerProposal = offerProposalRepository.findById(offerId.intValue())
                .orElseThrow(() -> new EntityNotFoundException("Offer Proposal không tồn tại với ID: " + offerId));

        String oldStatus = offerProposal.getOfferStatus();
        String responseInput = dto.getResponse();

        // Kiểm tra điều kiện: Ứng viên CHỈ được phép xem xét và phản hồi khi Offer đã được HR phát hành chính thức
        String currentStatus = offerProposal.getOfferStatus();
        if (!"Sent_Candidate".equalsIgnoreCase(currentStatus) && !"Sent_To_Candidate".equalsIgnoreCase(currentStatus) && !"Sent".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Thư mời làm việc chưa được Bộ phận Nhân sự (HR) phát hành chính thức tới bạn (Trạng thái hiện tại: " + currentStatus + ").");
        }

        if ("Accept".equalsIgnoreCase(responseInput)) {
            offerProposal.setOfferStatus("Accepted");
            offerProposalRepository.save(offerProposal);
            // Ứng viên đã chấp nhận Thư mời làm việc (Accepted).
            // Quá trình tuyển dụng sẽ chính thức kết thúc khi Hiring Manager bấm nút Tuyển dụng (Hired).

        } else if ("Decline".equalsIgnoreCase(responseInput)) {
            offerProposal.setOfferStatus("Declined");
            offerProposalRepository.save(offerProposal);

        } else if ("Negotiate".equalsIgnoreCase(responseInput)) {
            offerProposal.setOfferStatus("Negotiating");
            offerProposalRepository.save(offerProposal);

            // Lưu bản ghi thương lượng mới
            OfferNegotiation negotiation = OfferNegotiation.builder()
                    .offerProposal(offerProposal)
                    .candidateCounterSalary(dto.getCounterSalary())
                    .candidateNotes(dto.getCandidateNotes())
                    .negotiationDate(LocalDateTime.now())
                    .build();
            offerNegotiationRepository.save(negotiation);

        } else {
            throw new IllegalArgumentException("Phản hồi không hợp lệ. Chỉ chấp nhận 'Accept', 'Decline', hoặc 'Negotiate'.");
        }

        // Ghi vết kiểm toán BR-SEC-02 / GBR-12
        Integer actorUserId = (offerProposal.getProposedBy() != null) ? offerProposal.getProposedBy().getUserId() : 1;
        auditLogService.log(
                actorUserId,
                "UPDATE",
                "OfferProposal",
                offerId.toString(),
                "Status: " + oldStatus + ", ProposedSalary: " + offerProposal.getProposedSalary(),
                "Status: " + offerProposal.getOfferStatus() + ", CandidateResponse: " + responseInput + (dto.getCounterSalary() != null ? ", CounterSalary: " + dto.getCounterSalary() : "")
        );

        // Báo cho HR/Hiring Manager khi Candidate phản hồi (OR-10: bọc try-catch không làm rollback giao dịch)
        try {
            notificationService.sendCandidateResponseNotificationToHR(offerProposal, responseInput);
        } catch (Exception ex) {
            log.error("[OR-10] Gửi thông báo phản hồi của ứng viên tới HR thất bại cho Offer ID {}: {}", offerId, ex.getMessage());
        }

        return getOfferById(offerId);
    }

    /**
     * Lấy thông tin chi tiết OfferProposal kèm lịch sử phê duyệt và thương lượng.
     */
    @Override
    @Transactional(readOnly = true)
    public OfferResponseDTO getOfferById(Long offerId) {
        OfferProposal offer = offerProposalRepository.findDetailedByOfferId(offerId.intValue())
                .orElseThrow(() -> new EntityNotFoundException("Offer Proposal không tồn tại với ID: " + offerId));

        List<OfferApproval> approvals = offerApprovalRepository.findByOfferProposalOfferIdOrderByApprovedAtDesc(offer.getOfferId());
        List<OfferNegotiation> negotiations = offerNegotiationRepository.findByOfferProposalOfferIdOrderByNegotiationDateDesc(offer.getOfferId());

        List<OfferApprovalResponseDTO> approvalDTOs = approvals.stream()
                .map(a -> OfferApprovalResponseDTO.builder()
                        .offerApprovalId(a.getOfferApprovalId() != null ? a.getOfferApprovalId().longValue() : null)
                        .directorId(a.getDirector() != null ? a.getDirector().getUserId().longValue() : null)
                        .directorName(a.getDirector() != null ? a.getDirector().getFullName() : null)
                        .status(a.getStatus())
                        .directorComments(a.getDirectorComments())
                        .approvedAt(a.getApprovedAt())
                        .build())
                .toList();

        List<OfferNegotiationResponseDTO> negotiationDTOs = negotiations.stream()
                .map(n -> OfferNegotiationResponseDTO.builder()
                        .negotiationId(n.getNegotiationId() != null ? n.getNegotiationId().longValue() : null)
                        .candidateCounterSalary(n.getCandidateCounterSalary())
                        .candidateNotes(n.getCandidateNotes())
                        .hrResponseNotes(n.getHrResponseNotes())
                        .negotiationDate(n.getNegotiationDate())
                        .build())
                .toList();

        Application application = offer.getApplication();
        Long applicationId = application != null ? application.getApplicationId().longValue() : null;
        Long candidateId = (application != null && application.getCandidate() != null) ? application.getCandidate().getCandidateId().longValue() : null;
        String candidateName = (application != null && application.getCandidate() != null) ? application.getCandidate().getFullName() : null;
        Long jobPostingId = (application != null && application.getJobPosting() != null) ? application.getJobPosting().getJobPostingId().longValue() : null;
        String jobTitle = (application != null && application.getJobPosting() != null) ? application.getJobPosting().getPostingTitle() : null;

        User proposedBy = offer.getProposedBy();
        Long proposedById = proposedBy != null ? proposedBy.getUserId().longValue() : null;
        String proposedByName = proposedBy != null ? proposedBy.getFullName() : null;

        return OfferResponseDTO.builder()
                .offerId(offer.getOfferId().longValue())
                .applicationId(applicationId)
                .candidateId(candidateId)
                .candidateName(candidateName)
                .jobPostingId(jobPostingId)
                .jobTitle(jobTitle)
                .proposedSalary(offer.getProposedSalary())
                .probationSalary(offer.getProbationSalary())
                .probationDays(offer.getProbationDays())
                .proposedPosition(offer.getProposedPosition())
                .workLocation(offer.getWorkLocation())
                .proposedById(proposedById)
                .proposedByName(proposedByName)
                .offerStatus(offer.getOfferStatus())
                .createdAt(offer.getCreatedAt())
                .updatedAt(offer.getUpdatedAt())
                .approvalHistory(approvalDTOs)
                .negotiationHistory(negotiationDTOs)
                .build();
    }

    /**
     * Tìm kiếm và phân trang danh sách OfferProposal cho nội bộ (HM, Director, HR).
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OfferResponseDTO> getOffers(String status, String candidateName, Pageable pageable) {
        String cleanStatus = (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim()))
                ? status.trim() : null;
        String cleanCandidate = (candidateName != null && !candidateName.trim().isEmpty())
                ? candidateName.trim() : null;

        Page<OfferProposal> pageResult;
        if (cleanStatus == null && cleanCandidate == null) {
            pageResult = offerProposalRepository.findAll(pageable);
        } else {
            pageResult = offerProposalRepository.search(cleanStatus, cleanCandidate, pageable);
        }
        return pageResult.map(this::mapToSummaryDTO);
    }

    private OfferResponseDTO mapToSummaryDTO(OfferProposal offer) {
        Application application = offer.getApplication();
        Long applicationId = application != null ? application.getApplicationId().longValue() : null;
        Long candidateId = (application != null && application.getCandidate() != null) ? application.getCandidate().getCandidateId().longValue() : null;
        String candidateName = (application != null && application.getCandidate() != null) ? application.getCandidate().getFullName() : null;
        Long jobPostingId = (application != null && application.getJobPosting() != null) ? application.getJobPosting().getJobPostingId().longValue() : null;
        String jobTitle = (application != null && application.getJobPosting() != null) ? application.getJobPosting().getPostingTitle() : null;

        User proposedBy = offer.getProposedBy();
        Long proposedById = proposedBy != null ? proposedBy.getUserId().longValue() : null;
        String proposedByName = proposedBy != null ? proposedBy.getFullName() : null;

        return OfferResponseDTO.builder()
                .offerId(offer.getOfferId().longValue())
                .applicationId(applicationId)
                .candidateId(candidateId)
                .candidateName(candidateName)
                .jobPostingId(jobPostingId)
                .jobTitle(jobTitle)
                .proposedSalary(offer.getProposedSalary())
                .probationSalary(offer.getProbationSalary())
                .probationDays(offer.getProbationDays())
                .proposedPosition(offer.getProposedPosition())
                .workLocation(offer.getWorkLocation())
                .proposedById(proposedById)
                .proposedByName(proposedByName)
                .offerStatus(offer.getOfferStatus())
                .createdAt(offer.getCreatedAt())
                .updatedAt(offer.getUpdatedAt())
                .build();
    }

    /**
     * Cập nhật đề xuất Offer (Update Offer Proposal) - Dành cho HM khi Offer ở trạng thái Draft hoặc Rejected.
     */
    @Override
    public OfferResponseDTO updateOfferProposal(Long offerId, UpdateOfferRequestDTO dto) {
        OfferProposal offer = offerProposalRepository.findById(offerId.intValue())
                .orElseThrow(() -> new EntityNotFoundException("Offer Proposal không tồn tại với ID: " + offerId));

        String status = offer.getOfferStatus();
        if (!"Draft".equalsIgnoreCase(status) && !"Rejected".equalsIgnoreCase(status)
                && !"Pending_Director_Approval".equalsIgnoreCase(status) && !"Negotiating".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Chỉ được chỉnh sửa Offer khi đang ở trạng thái Bản thảo ('Draft'), Bị từ chối ('Rejected') hoặc Đang đàm phán ('Negotiating').");
        }

        // Validation BR-OFF-01: Kiểm tra lương thử việc: 85% <= probationSalary <= proposedSalary
        BigDecimal proposedSalary = dto.getProposedSalary();
        BigDecimal probationSalary = dto.getProbationSalary();
        if (probationSalary == null || proposedSalary == null) {
            throw new IllegalArgumentException("Mức lương chính thức và lương thử việc không được để trống.");
        }
        if (probationSalary.compareTo(proposedSalary) > 0) {
            throw new IllegalArgumentException("Lương thử việc không được cao hơn mức lương chính thức (phải thấp hơn hoặc bằng lương chính thức).");
        }
        if (probationSalary.compareTo(proposedSalary.multiply(new BigDecimal("0.85"))) < 0) {
            throw new IllegalArgumentException("Lương thử việc phải đạt tối thiểu 85% lương chính thức theo Luật Lao động (BR-OFF-01).");
        }

        offer.setProposedPosition(dto.getProposedPosition());
        offer.setWorkLocation(dto.getWorkLocation());
        offer.setProposedSalary(proposedSalary);
        offer.setProbationSalary(probationSalary);
        if (dto.getProbationDays() != null) {
            offer.setProbationDays(dto.getProbationDays());
        }

        // Nếu trước đó đang Negotiating: HM cập nhật đề xuất đãi ngộ mới và tự động chuyển về Pending_Director_Approval để Director duyệt lại từ đầu
        if ("Negotiating".equalsIgnoreCase(status)) {
            // Kiểm tra xem HR đã duyệt yêu cầu thương lượng chưa
            List<OfferNegotiation> negotiations = offerNegotiationRepository.findByOfferProposalOfferIdOrderByNegotiationDateDesc(offer.getOfferId());
            boolean hrApproved = false;
            if (negotiations != null && !negotiations.isEmpty()) {
                OfferNegotiation latest = negotiations.get(0);
                if (latest.getHrResponseNotes() != null && !latest.getHrResponseNotes().trim().isEmpty()
                        && !latest.getHrResponseNotes().contains("[Từ chối")) {
                    hrApproved = true;
                }
            }
            if (!hrApproved) {
                throw new IllegalStateException("Yêu cầu thương lượng của ứng viên chưa được Bộ phận Nhân sự (HR) xét duyệt. Vui lòng chờ HR duyệt chấp thuận trước khi điều chỉnh lại đề xuất lương.");
            }

            offer.setOfferStatus("Pending_Director_Approval");
            try {
                notificationService.sendOfferSubmittedNotificationToDirector(offer);
            } catch (Exception ex) {
                log.error("[OR-10] Gửi thông báo tới Director thất bại cho Offer ID {}: {}", offer.getOfferId(), ex.getMessage());
            }
        } else if ("Rejected".equalsIgnoreCase(status)) {
            // Nếu trước đó bị Rejected thì sau khi sửa chuyển lại thành Draft để HM chủ động gửi duyệt
            offer.setOfferStatus("Draft");
        }

        OfferProposal saved = offerProposalRepository.save(offer);

        auditLogService.log(
                (saved.getProposedBy() != null) ? saved.getProposedBy().getUserId() : 1,
                "UPDATE",
                "OfferProposal",
                offerId.toString(),
                "Status: " + status,
                String.format("Cập nhật OfferProposal: ProposedSalary=%s, ProbationSalary=%s, Position=%s",
                        saved.getProposedSalary(), saved.getProbationSalary(), saved.getProposedPosition())
        );

        return getOfferById(offerId);
    }

    /**
     * Xóa bản thảo Offer (Delete Offer Proposal) - Dành cho HM khi Offer ở trạng thái Draft hoặc Voided.
     */
    @Override
    public void deleteOfferProposal(Long offerId) {
        OfferProposal offer = offerProposalRepository.findById(offerId.intValue())
                .orElseThrow(() -> new EntityNotFoundException("Offer Proposal không tồn tại với ID: " + offerId));

        String status = offer.getOfferStatus();
        if (!"Draft".equalsIgnoreCase(status) && !"Voided".equalsIgnoreCase(status) && !"Rejected".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Chỉ được phép xóa bản thảo Offer ('Draft') hoặc đã hủy ('Voided'). Trạng thái hiện tại: " + status);
        }

        offerProposalRepository.delete(offer);

        auditLogService.log(
                (offer.getProposedBy() != null) ? offer.getProposedBy().getUserId() : 1,
                "DELETE",
                "OfferProposal",
                offerId.toString(),
                "Status: " + status,
                "Xóa bản thảo Offer Proposal"
        );
    }

    /**
     * Trình duyệt Director (Submit to Director) - Chuyển từ Draft sang Pending_Director_Approval.
     */
    @Override
    public OfferResponseDTO submitToDirector(Long offerId) {
        OfferProposal offer = offerProposalRepository.findById(offerId.intValue())
                .orElseThrow(() -> new EntityNotFoundException("Offer Proposal không tồn tại với ID: " + offerId));

        String status = offer.getOfferStatus();
        if (!"Draft".equalsIgnoreCase(status) && !"Rejected".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Chỉ có thể trình duyệt Director khi Offer đang ở trạng thái 'Draft' hoặc 'Rejected'. Trạng thái hiện tại: " + status);
        }

        offer.setOfferStatus("Pending_Director_Approval");
        OfferProposal saved = offerProposalRepository.save(offer);

        auditLogService.log(
                (saved.getProposedBy() != null) ? saved.getProposedBy().getUserId() : 1,
                "UPDATE",
                "OfferProposal",
                offerId.toString(),
                "Status: " + status,
                "Trình duyệt Director phê duyệt ngân sách"
        );

        try {
            notificationService.sendOfferSubmittedNotificationToDirector(saved);
        } catch (Exception ex) {
            log.error("[OR-10] Gửi thông báo tới Director thất bại cho Offer ID {}: {}", offerId, ex.getMessage());
        }

        return getOfferById(offerId);
    }

    /**
     * Xác nhận Tuyển dụng chính thức (Confirm Hiring) - Hiring Manager bấm Tuyển dụng:
     * 1. Kết thúc quá trình của candidate: Chuyển applicationStatus sang "Hired"
     * 2. Cập nhật OfferProposal: offerStatus = "Hired"
     * 3. Đẩy thông báo cho HR để chuẩn bị tiếp nhận Onboarding và hợp đồng lao động
     * 4. Báo về cho Ứng viên chúc mừng hoàn tất quá trình tuyển dụng
     * 5. Ghi log kiểm toán hệ thống
     */
    @Override
    public OfferResponseDTO confirmHiring(Long offerId) {
        OfferProposal offer = offerProposalRepository.findById(offerId.intValue())
                .orElseThrow(() -> new EntityNotFoundException("Offer Proposal không tồn tại với ID: " + offerId));

        String status = offer.getOfferStatus();
        if (!"Accepted".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Chỉ có thể xác nhận tuyển dụng khi Ứng viên đã chấp nhận Offer ('Accepted'). Trạng thái hiện tại: " + status);
        }

        // 1. Kết thúc quá trình của candidate: Chuyển ApplicationStatus thành Hired
        Application application = offer.getApplication();
        if (application != null) {
            application.setApplicationStatus("Hired");
            applicationRepository.save(application);
        }

        // 2. Cập nhật OfferStatus sang Hired
        offer.setOfferStatus("Hired");
        offerProposalRepository.save(offer);

        // 3. Đẩy thông báo cho HR để chuẩn bị tiếp nhận Onboarding (bọc try-catch không làm rollback)
        try {
            notificationService.sendHiringConfirmedNotificationToHR(offer);
        } catch (Exception ex) {
            log.error("[CONFIRM_HIRING] Gửi thông báo tới HR thất bại cho Offer ID {}: {}", offerId, ex.getMessage());
        }

        // 4. Báo về cho Ứng viên chúc mừng trúng tuyển chính thức
        try {
            notificationService.sendHiringConfirmedNotificationToCandidate(offer);
        } catch (Exception ex) {
            log.error("[CONFIRM_HIRING] Gửi thông báo tới Ứng viên thất bại cho Offer ID {}: {}", offerId, ex.getMessage());
        }

        // 5. Ghi log kiểm toán
        auditLogService.log(
                (offer.getProposedBy() != null) ? offer.getProposedBy().getUserId() : 1,
                "UPDATE",
                "Application",
                (application != null) ? application.getApplicationId().toString() : offerId.toString(),
                "OfferStatus: Accepted -> Hired",
                "Hiring Manager xác nhận tuyển dụng chính thức (Confirm Hiring). Kết thúc quá trình ứng tuyển của ứng viên -> ApplicationStatus: Hired, OfferStatus: Hired"
        );

        return getOfferById(offerId);
    }

    /**
     * Lấy danh sách các trạng thái thực tế đang có trong bảng OfferProposal của Database.
     */
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<String> getOfferStatuses() {
        return offerProposalRepository.findDistinctOfferStatuses();
    }

    /**
     * 5. Phát hành và gửi Offer tới ứng viên (dành cho HR sau khi Director duyệt).
     * Yêu cầu Offer đang ở trạng thái "Approved". Cập nhật thành "Sent_Candidate".
     */
    @Override
    public OfferResponseDTO sendOfferToCandidate(Long offerId, Long hrUserId) {
        OfferProposal offerProposal = offerProposalRepository.findById(offerId.intValue())
                .orElseThrow(() -> new EntityNotFoundException("Offer Proposal không tồn tại với ID: " + offerId));

        String currentStatus = offerProposal.getOfferStatus();
        if (!"Approved".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Chỉ có thể phát hành Offer đã được Director phê duyệt! (Trạng thái hiện tại: " + currentStatus + ")");
        }

        offerProposal.setOfferStatus("Sent_Candidate");
        OfferProposal saved = offerProposalRepository.save(offerProposal);

        // Gửi email thư mời làm việc cho ứng viên
        try {
            notificationService.sendOfferApprovedNotificationToCandidate(saved);
        } catch (Exception ex) {
            log.error("[OR-10] Gửi email thư mời làm việc thất bại cho Offer ID {}: {}", offerId, ex.getMessage());
        }

        // Ghi nhật ký kiểm toán (Thao tác: "SEND_OFFER")
        Integer actorId = (hrUserId != null) ? hrUserId.intValue() : 1;
        auditLogService.log(
                actorId,
                "SEND_OFFER",
                "OfferProposal",
                offerId.toString(),
                "Status: Approved",
                "Status: Sent_Candidate, HR phát hành thư mời nhận việc tới ứng viên"
        );

        return getOfferById(offerId);
    }

    /**
     * 6. HR xử lý yêu cầu thương lượng của ứng viên:
     * - Nếu HR chọn "APPROVE" (Duyệt thương lượng):
     *   Lưu ý kiến HR, giữ trạng thái 'Negotiating', và gửi thông báo tới Hiring Management để sửa lại đề xuất lương mới.
     * - Nếu HR chọn "REJECT" (Từ chối thương lượng):
     *   Lưu lý do từ chối, chuyển trạng thái Offer về 'Sent_Candidate' để ứng viên tiếp tục xem xét theo mức lương ban đầu,
     *   đồng thời gửi email/thông báo giải thích tới ứng viên.
     */
    @Override
    public OfferResponseDTO respondToCandidateNegotiation(Long offerId, Long negotiationId, HrNegotiationResponseDTO dto) {
        OfferProposal offerProposal = offerProposalRepository.findById(offerId.intValue())
                .orElseThrow(() -> new EntityNotFoundException("Offer Proposal không tồn tại với ID: " + offerId));

        OfferNegotiation negotiation = offerNegotiationRepository.findById(negotiationId.intValue())
                .orElseThrow(() -> new EntityNotFoundException("Bản ghi thương lượng không tồn tại với ID: " + negotiationId));

        if (negotiation.getOfferProposal() == null || !negotiation.getOfferProposal().getOfferId().equals(offerProposal.getOfferId())) {
            throw new IllegalArgumentException("Bản ghi thương lượng ID " + negotiationId + " không thuộc về Offer ID " + offerId);
        }

        String action = (dto.getAction() != null && !dto.getAction().trim().isEmpty())
                ? dto.getAction().trim().toUpperCase() : "APPROVE";
        String notes = (dto.getHrResponseNotes() != null) ? dto.getHrResponseNotes().trim() : "";
        Integer actorId = (dto.getHrUserId() != null) ? dto.getHrUserId().intValue() : 1;

        if ("REJECT".equalsIgnoreCase(action) || "DECLINE".equalsIgnoreCase(action)) {
            // HR Từ chối yêu cầu thương lượng của ứng viên
            String formattedNotes = "[Từ chối thương lượng] " + notes;
            negotiation.setHrResponseNotes(formattedNotes);
            negotiation.setNegotiationDate(LocalDateTime.now());
            offerNegotiationRepository.save(negotiation);

            // Chuyển lại trạng thái Sent_Candidate để ứng viên tiếp tục xem xét Thư mời theo mức đãi ngộ ban đầu
            offerProposal.setOfferStatus("Sent_Candidate");
            offerProposalRepository.save(offerProposal);

            // Gửi thông báo / email từ chối tới ứng viên
            try {
                notificationService.sendHrNegotiationRejectedNotificationToCandidate(offerProposal, notes);
            } catch (Exception ex) {
                log.error("[HR_REJECT_NEGOTIATION] Gửi thông báo từ chối thương lượng tới ứng viên thất bại cho Offer ID {}: {}", offerId, ex.getMessage());
            }

            // Ghi nhật ký kiểm toán
            try {
                auditLogService.log(
                        actorId,
                        "UPDATE",
                        "OfferNegotiation",
                        negotiationId.toString(),
                        "Status: Negotiating",
                        "Status: Sent_Candidate, HR từ chối thương lượng: " + notes
                );
            } catch (Exception ex) {
                log.error("[AUDIT-LOG] Ghi nhật ký từ chối thương lượng thất bại: {}", ex.getMessage());
            }

        } else {
            // HR Duyệt yêu cầu thương lượng (APPROVE) -> Gửi về cho Hiring Management sửa lại đề xuất lương mới
            String formattedNotes = "[Chấp thuận thương lượng] " + notes;
            negotiation.setHrResponseNotes(formattedNotes);
            negotiation.setNegotiationDate(LocalDateTime.now());
            offerNegotiationRepository.save(negotiation);

            // Offer giữ nguyên trạng thái 'Negotiating' để Hiring Manager cập nhật lại đề xuất
            offerProposal.setOfferStatus("Negotiating");
            offerProposalRepository.save(offerProposal);

            // Gửi thông báo tới Hiring Management để cập nhật lại đề xuất với yêu cầu lương mới
            try {
                notificationService.sendHrNegotiationApprovedNotificationToHM(offerProposal, negotiation, notes);
            } catch (Exception ex) {
                log.error("[HR_APPROVE_NEGOTIATION] Gửi thông báo duyệt thương lượng tới HM thất bại cho Offer ID {}: {}", offerId, ex.getMessage());
            }

            // Ghi nhật ký kiểm toán
            try {
                auditLogService.log(
                        actorId,
                        "UPDATE",
                        "OfferNegotiation",
                        negotiationId.toString(),
                        "Status: Negotiating",
                        "HR duyệt thương lượng (đề xuất: " + negotiation.getCandidateCounterSalary() + "), chuyển về cho Hiring Management điều chỉnh: " + notes
                );
            } catch (Exception ex) {
                log.error("[AUDIT-LOG] Ghi nhật ký duyệt thương lượng thất bại: {}", ex.getMessage());
            }
        }

        return getOfferById(offerId);
    }
}
