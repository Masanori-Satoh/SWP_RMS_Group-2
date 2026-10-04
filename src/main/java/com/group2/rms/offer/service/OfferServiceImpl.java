package com.group2.rms.offer.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.core.exception.BaseBusinessException;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.interview.entity.InterviewFinalResult;
import com.group2.rms.offer.dto.CreateOfferRequest;
import com.group2.rms.offer.dto.OfferDetailResponse;
import com.group2.rms.offer.dto.OfferResponse;
import com.group2.rms.offer.dto.PassedCandidateResponse;
import com.group2.rms.offer.dto.UpdateOfferRequest;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.offer.repository.OfferApprovalRepository;
import com.group2.rms.offer.repository.OfferNegotiationRepository;
import com.group2.rms.offer.repository.OfferProposalRepository;
import com.group2.rms.interview.repository.InterviewFinalResultRepository;
import com.group2.rms.offer.exception.OfferValidationException;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OfferServiceImpl implements OfferService {

    private final OfferProposalRepository offerProposalRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final InterviewFinalResultRepository interviewFinalResultRepository;
    private final OfferApprovalRepository offerApprovalRepository;
    private final OfferNegotiationRepository offerNegotiationRepository;
    private final NotificationService notificationService;

    @Override
    public OfferResponse createOfferProposal(CreateOfferRequest request) {
        validateProbationSalaryRule(request.getProposedSalary(), request.getProbationSalary());

        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Application với ID: " + request.getApplicationId()));

        User proposedBy = resolveProposedBy(request.getProposedById());

        deactivateExistingActiveOffers(application.getApplicationId());

        OfferProposal newOffer = OfferProposal.builder()
                .application(application)
                .offeredPositionTitle(request.getOfferedPositionTitle())
                .proposedSalary(request.getProposedSalary())
                .probationSalary(request.getProbationSalary())
                .expectedStartDate(request.getExpectedStartDate())
                .workLocation(request.getWorkLocation())
                .benefitsPackage(request.getBenefitsPackage())
                .proposedBy(proposedBy)
                .offerStatus("Pending_Director")
                .build();

        OfferProposal savedOffer = offerProposalRepository.save(newOffer);
        return mapToResponse(savedOffer);
    }

    @Override
    @Transactional(readOnly = true)
    public OfferResponse getOfferById(Integer offerId) {
        OfferProposal offer = offerProposalRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy OfferProposal với ID: " + offerId));
        return mapToResponse(offer);
    }

    @Override
    @Transactional(readOnly = true)
    public OfferResponse getOfferByApplicationId(Integer applicationId) {
        OfferProposal offer = offerProposalRepository.findByApplication_ApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy OfferProposal cho Application ID: " + applicationId));
        return mapToResponse(offer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassedCandidateResponse> getPassedCandidatesForOffer() {
        List<InterviewFinalResult> passedResults = interviewFinalResultRepository.findAllPassedWithDetails();
        if (passedResults == null) {
            return Collections.emptyList();
        }

        return passedResults.stream().map(r -> {
            var schedule = r.getInterviewSchedule();
            var application = schedule != null ? schedule.getApplication() : null;
            var candidate = application != null ? application.getCandidate() : null;
            var user = candidate != null ? candidate.getAccount() : null;
            var jobPosting = application != null ? application.getJobPosting() : null;
            var requisition = jobPosting != null ? jobPosting.getRequisition() : null;
            var department = requisition != null ? requisition.getDepartment() : null;

            String workLocation = null;
            if (jobPosting != null && jobPosting.getWorkLocation() != null && !jobPosting.getWorkLocation().isBlank()) {
                workLocation = jobPosting.getWorkLocation();
            } else if (candidate != null && candidate.getAddress() != null && !candidate.getAddress().isBlank()) {
                workLocation = candidate.getAddress();
            } else {
                workLocation = "Trụ sở chính Mộc RMS";
            }

            return PassedCandidateResponse.builder()
                    .applicationId(application != null ? application.getApplicationId() : null)
                    .candidateId(candidate != null ? candidate.getCandidateId() : null)
                    .candidateName(user != null ? user.getFullName() : null)
                    .email(user != null ? user.getEmail() : null)
                    .phoneNumber(user != null ? user.getPhoneNumber() : null)
                    .appliedPosition(jobPosting != null ? jobPosting.getPostingTitle() : null)
                    .departmentName(department != null ? department.getDepartmentName() : null)
                    .requisitionId(requisition != null ? requisition.getRequisitionId() : null)
                    .jobPostingId(jobPosting != null ? jobPosting.getJobPostingId() : null)
                    .workLocation(workLocation)
                    .finalResultId(r.getFinalResultId())
                    .finalDecision(r.getFinalDecision())
                    .interviewSummaryComments(r.getFinalSummaryComments())
                    .recommendedSalary(r.getRecommendedSalary())
                    .interviewApprovedAt(r.getApprovedAt())
                    .hiringManagerName(r.getHiringManager() != null ? r.getHiringManager().getFullName() : null)
                    .build();
        }).toList();
    }

    @Override
    public OfferResponse createOfferByHr(CreateOfferRequest dto) {
        validateOfferBusinessRules(
                dto.getProposedSalary(),
                dto.getProbationSalary(),
                dto.getProbationDays(),
                dto.getExpectedStartDate(),
                dto.getWorkLocation(),
                dto.getOfferedPositionTitle());

        Application application = applicationRepository.findById(dto.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn ứng tuyển với ID: " + dto.getApplicationId()));

        User proposedBy = resolveProposedBy(dto.getProposedById());

        String status = Boolean.TRUE.equals(dto.getIsDraft()) ? "Draft" : "Pending_Director";

        // Bảng OfferProposal có ràng buộc UNIQUE trên ApplicationId
        // (UQ_OfferProposal_Application)
        // Nếu Application này đã có OfferProposal, cập nhật bản ghi hiện tại (upsert)
        // để tránh lỗi trùng khóa UQ
        Optional<OfferProposal> existingOfferOpt = offerProposalRepository
                .findByApplication_ApplicationId(application.getApplicationId());

        OfferProposal offerToSave;
        if (existingOfferOpt.isPresent()) {
            offerToSave = existingOfferOpt.get();
            offerToSave.setOfferedPositionTitle(dto.getOfferedPositionTitle());
            offerToSave.setProposedSalary(dto.getProposedSalary());
            offerToSave.setProbationSalary(dto.getProbationSalary());
            offerToSave.setExpectedStartDate(dto.getExpectedStartDate());
            offerToSave.setWorkLocation(dto.getWorkLocation());
            offerToSave.setBenefitsPackage(dto.getBenefitsPackage());
            offerToSave.setProposedBy(proposedBy);
            offerToSave.setOfferStatus(status);
            offerToSave.setIsDeleted(false);
        } else {
            offerToSave = OfferProposal.builder()
                    .application(application)
                    .offeredPositionTitle(dto.getOfferedPositionTitle())
                    .proposedSalary(dto.getProposedSalary())
                    .probationSalary(dto.getProbationSalary())
                    .expectedStartDate(dto.getExpectedStartDate())
                    .workLocation(dto.getWorkLocation())
                    .benefitsPackage(dto.getBenefitsPackage())
                    .proposedBy(proposedBy)
                    .offerStatus(status)
                    .isDeleted(false)
                    .build();
        }

        OfferProposal saved = offerProposalRepository.save(offerToSave);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfferResponse> getAllOffersForHr(String status, Pageable pageable) {
        Page<OfferProposal> pagedEntities;
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            pagedEntities = offerProposalRepository.findAllActiveByOrderByOfferIdAsc(pageable);
        } else {
            pagedEntities = offerProposalRepository.findActiveByStatusOrderByOfferIdAsc(status.trim(), pageable);
        }
        return pagedEntities.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OfferDetailResponse getOfferDetailForHr(Integer id) {
        OfferProposal offer = offerProposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy OfferProposal với ID: " + id));

        var app = offer.getApplication();
        var cand = app != null ? app.getCandidate() : null;
        var user = cand != null ? cand.getAccount() : null;
        var jobPosting = app != null ? app.getJobPosting() : null;
        var requisition = jobPosting != null ? jobPosting.getRequisition() : null;
        var department = requisition != null ? requisition.getDepartment() : null;

        String finalDecision = null;
        String interviewSummaryComments = null;
        BigDecimal recommendedSalary = null;
        String hiringManagerName = null;
        LocalDateTime interviewApprovedAt = null;

        if (app != null) {
            List<InterviewFinalResult> interviewResults = interviewFinalResultRepository
                    .findByApplicationIdOrderByApprovedAtDesc(app.getApplicationId());
            if (!interviewResults.isEmpty()) {
                InterviewFinalResult latestResult = interviewResults.get(0);
                finalDecision = latestResult.getFinalDecision();
                interviewSummaryComments = latestResult.getFinalSummaryComments();
                recommendedSalary = latestResult.getRecommendedSalary();
                interviewApprovedAt = latestResult.getApprovedAt();
                if (latestResult.getHiringManager() != null) {
                    hiringManagerName = latestResult.getHiringManager().getFullName();
                }
            }
        }

        List<OfferDetailResponse.DirectorApprovalLog> approvalHistory = offerApprovalRepository
                .findByOfferProposal_OfferIdOrderByApprovedAtDesc(id)
                .stream()
                .map(a -> OfferDetailResponse.DirectorApprovalLog.builder()
                        .approvalId(a.getOfferApprovalId())
                        .directorId(a.getDirector() != null ? a.getDirector().getUserId() : null)
                        .directorName(a.getDirector() != null ? a.getDirector().getFullName() : null)
                        .status(a.getStatus())
                        .directorComments(a.getDirectorComments())
                        .approvedAt(a.getApprovedAt())
                        .build())
                .toList();

        List<OfferDetailResponse.NegotiationRound> negotiationHistory = offerNegotiationRepository
                .findByOfferProposal_OfferIdOrderByNegotiationDateDesc(id)
                .stream()
                .map(n -> OfferDetailResponse.NegotiationRound.builder()
                        .negotiationId(n.getNegotiationId())
                        .candidateCounterSalary(n.getCandidateCounterSalary())
                        .candidateNotes(n.getCandidateNotes())
                        .hrResponseNotes(n.getHrResponseNotes())
                        .negotiationDate(n.getNegotiationDate())
                        .build())
                .toList();

        String proposedByName = offer.getProposedBy() != null ? offer.getProposedBy().getFullName() : null;
        Integer proposedById = offer.getProposedBy() != null ? offer.getProposedBy().getUserId() : null;

        return OfferDetailResponse.builder()
                .offerId(offer.getOfferId())
                .offerStatus(offer.getOfferStatus())
                .offeredPositionTitle(offer.getOfferedPositionTitle())
                .proposedSalary(offer.getProposedSalary())
                .probationSalary(offer.getProbationSalary())
                .probationDays(60)
                .expectedStartDate(offer.getExpectedStartDate())
                .workLocation(offer.getWorkLocation())
                .benefitsPackage(offer.getBenefitsPackage())
                .proposedById(proposedById)
                .proposedByName(proposedByName)
                .createdAt(offer.getCreatedAt())
                .updatedAt(offer.getUpdatedAt())
                .applicationId(app != null ? app.getApplicationId() : null)
                .candidateId(cand != null ? cand.getCandidateId() : null)
                .candidateName(user != null ? user.getFullName() : null)
                .candidateEmail(user != null ? user.getEmail() : null)
                .candidatePhone(user != null ? user.getPhoneNumber() : null)
                .candidateAddress(cand != null ? cand.getAddress() : null)
                .appliedPosition(jobPosting != null ? jobPosting.getPostingTitle() : null)
                .departmentName(department != null ? department.getDepartmentName() : null)
                .requisitionId(requisition != null ? requisition.getRequisitionId() : null)
                .jobPostingId(jobPosting != null ? jobPosting.getJobPostingId() : null)
                .appliedCvUrl(app != null ? app.getAppliedCvUrl() : null)
                .finalDecision(finalDecision)
                .interviewSummaryComments(interviewSummaryComments)
                .recommendedSalary(recommendedSalary)
                .hiringManagerName(hiringManagerName)
                .interviewApprovedAt(interviewApprovedAt)
                .approvalHistory(approvalHistory)
                .negotiationHistory(negotiationHistory)
                .build();
    }

    @Override
    public OfferResponse updateOfferByHr(Integer id, UpdateOfferRequest dto) {
        OfferProposal offer = offerProposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy OfferProposal với ID: " + id));

        String currentStatus = offer.getOfferStatus();
        boolean canEdit = "Draft".equalsIgnoreCase(currentStatus)
                || "Rejected".equalsIgnoreCase(currentStatus)
                || "Director_Rejected".equalsIgnoreCase(currentStatus);

        if (!canEdit) {
            throw new BaseBusinessException(
                    "Chỉ được phép cập nhật Offer khi ở trạng thái Draft hoặc Bị từ chối. Trạng thái hiện tại: "
                            + currentStatus,
                    "OFFER_STATUS_INVALID");
        }

        validateOfferBusinessRules(
                dto.getProposedSalary(),
                dto.getProbationSalary(),
                dto.getProbationDays(),
                dto.getExpectedStartDate(),
                dto.getWorkLocation(),
                dto.getOfferedPositionTitle());

        if (dto.getOfferedPositionTitle() != null && !dto.getOfferedPositionTitle().isBlank()) {
            offer.setOfferedPositionTitle(dto.getOfferedPositionTitle());
        }
        offer.setProposedSalary(dto.getProposedSalary());
        offer.setProbationSalary(dto.getProbationSalary());
        offer.setExpectedStartDate(dto.getExpectedStartDate());
        offer.setWorkLocation(dto.getWorkLocation());
        offer.setBenefitsPackage(dto.getBenefitsPackage());

        if (Boolean.TRUE.equals(dto.getIsDraft())) {
            offer.setOfferStatus("Draft");
        } else {
            offer.setOfferStatus("Pending_Director");
        }

        OfferProposal saved = offerProposalRepository.save(offer);
        return mapToResponse(saved);
    }

    @Override
    public void deleteDraftOfferByHr(Integer id) {
        OfferProposal offer = offerProposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy OfferProposal với ID: " + id));

        if (!"Draft".equalsIgnoreCase(offer.getOfferStatus())) {
            throw new BaseBusinessException(
                    "Chỉ được phép xóa bản thảo Offer (Draft). Trạng thái hiện tại: " + offer.getOfferStatus(),
                    "OFFER_NOT_DRAFT");
        }

        // Soft delete: Xóa đề xuất ra khỏi bảng danh sách hiển thị, KHÔNG xóa hẳn ra
        // khỏi database
        offer.setIsDeleted(true);
        offerProposalRepository.save(offer);
    }

    @Override
    public OfferResponse sendOfferToCandidate(Integer id) {
        OfferProposal offer = offerProposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy OfferProposal với ID: " + id));

        String currentStatus = offer.getOfferStatus();
        boolean isApproved = "Approved".equalsIgnoreCase(currentStatus)
                || "Director_Approved".equalsIgnoreCase(currentStatus);

        if (!isApproved) {
            throw new BaseBusinessException(
                    "Chỉ được gửi thư mời nhận việc khi Offer đã được Director phê duyệt. Trạng thái hiện tại: "
                            + currentStatus,
                    "OFFER_NOT_APPROVED");
        }

        offer.setOfferStatus("Sent_Candidate");
        OfferProposal saved = offerProposalRepository.save(offer);

        if (notificationService != null) {
            try {
                notificationService.sendOfferLetterToCandidate(saved);
            } catch (Exception ignored) {
                // Tiếp tục ngay cả khi gửi mail lỗi ngầm
            }
        }

        if (saved.getApplication() != null) {
            Application application = saved.getApplication();
            application.setApplicationStatus("Offered");
            applicationRepository.save(application);
        }

        return mapToResponse(saved);
    }

    private void validateProbationSalaryRule(BigDecimal proposedSalary, BigDecimal probationSalary) {
        if (proposedSalary == null || probationSalary == null) {
            throw new IllegalArgumentException("Mức lương đề xuất và lương thử việc không được để trống.");
        }

        if (proposedSalary.compareTo(BigDecimal.ZERO) <= 0 || probationSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Mức lương đề xuất và lương thử việc phải lớn hơn 0.");
        }

        BigDecimal minProbationSalary = proposedSalary.multiply(new BigDecimal("0.85"));

        if (probationSalary.compareTo(minProbationSalary) < 0) {
            throw new IllegalArgumentException(
                    "Lương thử việc (" + probationSalary + " VND) phải đạt tối thiểu 85% lương chính thức ("
                            + minProbationSalary + " VND) theo quy định Luật Lao động .");
        }

        if (probationSalary.compareTo(proposedSalary) > 0) {
            throw new IllegalArgumentException("Lương thử việc không được vượt quá lương chính thức.");
        }
    }

    private void validateOfferBusinessRules(
            BigDecimal proposedSalary,
            BigDecimal probationSalary,
            Integer probationDays,
            LocalDate expectedStartDate,
            String workLocation,
            String offeredPositionTitle) {

        if (offeredPositionTitle == null || offeredPositionTitle.isBlank()) {
            throw new OfferValidationException("Vị trí chức danh đề xuất không được để trống.");
        }
        if (proposedSalary == null || proposedSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OfferValidationException("Mức lương chính thức phải lớn hơn 0.");
        }
        if (probationSalary == null || probationSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OfferValidationException("Mức lương thử việc phải lớn hơn 0.");
        }
        BigDecimal minProbationSalary = proposedSalary.multiply(new BigDecimal("0.85"));
        if (probationSalary.compareTo(minProbationSalary) < 0) {
            throw new OfferValidationException(
                    "Lương thử việc (" + probationSalary + " VND) phải đạt tối thiểu 85% lương chính thức ("
                            + minProbationSalary + " VND) theo quy định Luật Lao động.");
        }
        if (probationSalary.compareTo(proposedSalary) > 0) {
            throw new OfferValidationException("Lương thử việc không được vượt quá lương chính thức.");
        }
        if (probationDays == null || probationDays <= 0) {
            throw new OfferValidationException("Thời gian thử việc phải lớn hơn 0.");
        }
        if (expectedStartDate == null) {
            throw new OfferValidationException("Ngày bắt đầu dự kiến không được để trống.");
        }
        if (!expectedStartDate.isAfter(LocalDate.now())) {
            throw new OfferValidationException("Ngày bắt đầu dự kiến phải lớn hơn ngày hiện tại.");
        }
        if (workLocation == null || workLocation.isBlank()) {
            throw new OfferValidationException("Địa điểm làm việc không được để trống.");
        }
    }

    private void deactivateExistingActiveOffers(Integer applicationId) {
        List<String> inactiveStatuses = List.of("Voided", "Canceled", "Rejected", "Declined", "Director_Rejected",
                "Expired");

        List<OfferProposal> activeOffers = offerProposalRepository
                .findByApplication_ApplicationIdAndOfferStatusNotIn(applicationId, inactiveStatuses);

        for (OfferProposal oldOffer : activeOffers) {
            oldOffer.setOfferStatus("Voided");
        }

        if (!activeOffers.isEmpty()) {
            offerProposalRepository.saveAll(activeOffers);
            offerProposalRepository.flush();
        }
    }

    private User resolveProposedBy(Integer proposedById) {
        if (proposedById != null) {
            return userRepository.findById(proposedById).orElse(null);
        }
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                User user = userRepository.findByUsernameIgnoreCase(auth.getName()).orElse(null);
                if (user != null) {
                    return user;
                }
            }
        } catch (Exception ignored) {
        }
        return userRepository.findAll().stream().findFirst().orElse(null);
    }

    private OfferResponse mapToResponse(OfferProposal entity) {
        String candidateName = null;
        String candidateEmail = null;
        if (entity.getApplication() != null && entity.getApplication().getCandidate() != null) {
            var candUser = entity.getApplication().getCandidate().getAccount();
            if (candUser != null) {
                candidateName = candUser.getFullName();
                candidateEmail = candUser.getEmail();
            }
        }

        String proposedByName = null;
        Integer proposedById = null;
        if (entity.getProposedBy() != null) {
            proposedById = entity.getProposedBy().getUserId();
            proposedByName = entity.getProposedBy().getFullName();
        }

        return OfferResponse.builder()
                .offerId(entity.getOfferId())
                .applicationId(entity.getApplication() != null ? entity.getApplication().getApplicationId() : null)
                .candidateName(candidateName)
                .candidateEmail(candidateEmail)
                .offeredPositionTitle(entity.getOfferedPositionTitle())
                .proposedSalary(entity.getProposedSalary())
                .probationSalary(entity.getProbationSalary())
                .expectedStartDate(entity.getExpectedStartDate())
                .workLocation(entity.getWorkLocation())
                .benefitsPackage(entity.getBenefitsPackage())
                .proposedById(proposedById)
                .proposedByName(proposedByName)
                .offerStatus(entity.getOfferStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
