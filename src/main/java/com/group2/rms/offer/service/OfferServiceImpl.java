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
import com.group2.rms.offer.entity.OfferApproval;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.offer.repository.OfferApprovalRepository;
import com.group2.rms.offer.repository.OfferProposalRepository;
import com.group2.rms.interview.repository.InterviewFinalResultRepository;
import com.group2.rms.offer.exception.OfferValidationException;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.notification.OfferMailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.group2.rms.candidate.entity.Candidate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OfferServiceImpl implements OfferService {

    private final OfferProposalRepository offerProposalRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final InterviewFinalResultRepository interviewFinalResultRepository;
    private final OfferApprovalRepository offerApprovalRepository;
    private final OfferMailSender offerMailSender;
    private final com.group2.rms.notification.NotificationService systemNotificationService;

    /**
     * Quy tắc GBR-07:
     * - Chỉ chấp nhận tạo mới Offer Proposal đối với ứng viên đã đỗ phỏng vấn (Passed) và CHƯA CÓ bất kỳ lịch sử/gói Offer nào.
     * - Khi đơn ứng tuyển đã có Offer trong hệ thống (kể cả trạng thái Draft), không gợi ý trong mục tạo đề xuất mới
     *   và chặn thao tác tạo mới trực tiếp (bắt buộc HR chỉnh sửa trực tiếp trên danh sách Offer).
     */
    public static final Set<String> OVERRIDABLE_STATUSES = Collections.emptySet();

    /**
     * Quy tắc GBR-07:
     * Tất cả các trạng thái có Offer đang hoạt động (kể cả Draft) đều bị khóa khỏi luồng tạo mới trực tiếp.
     */
    public static final Set<String> LOCKED_STATUSES = Set.of(
            "Draft", "Pending_Director", "Approved", "Director_Approved", "Sent_Candidate", "Accepted",
            "Rejected", "Director_Rejected", "Declined", "Canceled", "Voided");

    public static boolean isOverridableStatus(String status) {
        if (status == null) return false;
        for (String s : OVERRIDABLE_STATUSES) {
            if (s.equalsIgnoreCase(status)) return true;
        }
        return false;
    }

    public static boolean isLockedStatus(String status) {
        if (status == null) return false;
        for (String s : LOCKED_STATUSES) {
            if (s.equalsIgnoreCase(status)) return true;
        }
        return false;
    }

    @Override
    public OfferResponse createOfferProposal(CreateOfferRequest request) {
        validateProbationSalaryRule(request.getProposedSalary(), request.getProbationSalary());

        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Application với ID: " + request.getApplicationId()));

        User proposedBy = resolveProposedBy(request.getProposedById());

        deactivateExistingActiveOffers(application.getApplicationId());

        Integer responsePeriodDays = request.getResponsePeriodDays() != null ? request.getResponsePeriodDays() : 5;

        OfferProposal newOffer = OfferProposal.builder()
                .application(application)
                .offeredPositionTitle(request.getOfferedPositionTitle())
                .proposedSalary(request.getProposedSalary())
                .probationSalary(request.getProbationSalary())
                .expectedStartDate(request.getExpectedStartDate())
                .workLocation(request.getWorkLocation())
                .benefitsPackage(request.getBenefitsPackage())
                .responsePeriodDays(responsePeriodDays)
                .responseDeadline(null)
                .proposedBy(proposedBy)
                .offerStatus("Pending_Director")
                .build();

        OfferProposal savedOffer = offerProposalRepository.save(newOffer);

        if ("Pending_Director".equals(savedOffer.getOfferStatus())) {
            if (offerMailSender != null) {
                try {
                    offerMailSender.notifyDirectorNewPendingOffer(savedOffer);
                } catch (Exception e) {
                    log.warn("Lỗi khi gửi email thông báo Director về Offer mới ID {}: {}", savedOffer.getOfferId(), e.getMessage());
                }
            }
            if (systemNotificationService != null) {
                try {
                    systemNotificationService.notifyOfferPendingDirector(savedOffer, proposedBy);
                } catch (Exception e) {
                    log.warn("Lỗi khi gửi in-app notification Director về Offer mới ID {}: {}", savedOffer.getOfferId(), e.getMessage());
                }
            }
        }

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

        List<PassedCandidateResponse> list = new ArrayList<>();
        Set<Integer> processedAppIds = new HashSet<>();

        for (InterviewFinalResult r : passedResults) {
            var schedule = r.getInterviewSchedule();
            var application = schedule != null ? schedule.getApplication() : null;
            if (application == null || application.getApplicationId() == null) {
                continue;
            }

            Integer appId = application.getApplicationId();
            if (processedAppIds.contains(appId)) {
                continue;
            }

            // Quy tắc GBR-07: Chỉ chấp nhận tạo mới đối với ứng viên chưa từng có bất kỳ Offer nào (kể cả bản Draft)
            Optional<OfferProposal> offerOpt = offerProposalRepository.findByApplication_ApplicationId(appId);
            if (offerOpt.isPresent() && !Boolean.TRUE.equals(offerOpt.get().getIsDeleted())) {
                continue; // Ứng viên đã có Offer (kể cả Draft) -> Không gợi ý trong mục tạo đề xuất mới
            }

            processedAppIds.add(appId);

            var candidate = application.getCandidate();
            var user = candidate != null ? candidate.getAccount() : null;
            var jobPosting = application.getJobPosting();
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

            list.add(PassedCandidateResponse.builder()
                    .applicationId(appId)
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
                    .existingOfferStatus(null)
                    .existingOfferId(null)
                    .build());
        }

        return list;
    }

    @Override
    public OfferResponse createOfferByHr(CreateOfferRequest dto) {
        validateOfferBusinessRules(
                dto.getProposedSalary(),
                dto.getProbationSalary(),
                dto.getProbationDays(),
                dto.getExpectedStartDate(),
                dto.getWorkLocation(),
                dto.getOfferedPositionTitle(),
                dto.getResponsePeriodDays());

        Application application = applicationRepository.findById(dto.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn ứng tuyển với ID: " + dto.getApplicationId()));

        User proposedBy = resolveProposedBy(dto.getProposedById());

        String status = Boolean.TRUE.equals(dto.getIsDraft()) ? "Draft" : "Pending_Director";

        // Bảng OfferProposal có ràng buộc UNIQUE trên ApplicationId
        // (UQ_OfferProposal_Application)
        // Áp dụng quy tắc Single Active Offer Rule (GBR-07):
        // Chỉ chấp nhận tạo mới cho ứng viên chưa có Offer; nếu đã tồn tại Offer (kể cả Draft) thì chặn lại
        Optional<OfferProposal> existingOfferOpt = offerProposalRepository
                .findByApplication_ApplicationId(application.getApplicationId());

        if (existingOfferOpt.isPresent() && !Boolean.TRUE.equals(existingOfferOpt.get().getIsDeleted())) {
            String currentStatus = existingOfferOpt.get().getOfferStatus();
            throw new OfferValidationException(
                    "Theo quy tắc GBR-07, không thể tạo mới Offer Proposal vì đơn ứng tuyển đang có gói Offer ở trạng thái ["
                            + currentStatus + "]. Vui lòng chỉnh sửa trực tiếp trên danh sách Offer thay vì tạo mới.",
                    "OFFER_LOCKED_STATE");
        }

        Integer responsePeriodDays = dto.getResponsePeriodDays() != null ? dto.getResponsePeriodDays() : 5;

        OfferProposal offerToSave = OfferProposal.builder()
                .application(application)
                .offeredPositionTitle(dto.getOfferedPositionTitle())
                .proposedSalary(dto.getProposedSalary())
                .probationSalary(dto.getProbationSalary())
                .expectedStartDate(dto.getExpectedStartDate())
                .workLocation(dto.getWorkLocation())
                .benefitsPackage(dto.getBenefitsPackage())
                .responsePeriodDays(responsePeriodDays)
                .responseDeadline(null)
                .proposedBy(proposedBy)
                .offerStatus(status)
                .isDeleted(false)
                .build();

        OfferProposal saved = offerProposalRepository.save(offerToSave);

        if ("Pending_Director".equals(status)) {
            if (offerMailSender != null) {
                try {
                    offerMailSender.notifyDirectorNewPendingOffer(saved);
                } catch (Exception e) {
                    log.warn("Lỗi khi gửi email thông báo Director về Offer mới ID {}: {}", saved.getOfferId(), e.getMessage());
                }
            }
            if (systemNotificationService != null) {
                try {
                    systemNotificationService.notifyOfferPendingDirector(saved, proposedBy);
                } catch (Exception e) {
                    log.warn("Lỗi khi gửi in-app notification Director về Offer mới ID {}: {}", saved.getOfferId(), e.getMessage());
                }
            }
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfferResponse> getAllOffersForHr(String status, Pageable pageable) {
        return getAllOffersForHr(null, status, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfferResponse> getAllOffersForHr(String search, String status, String timeSort, Pageable pageable) {
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasStatus = status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim());
        boolean hasCustomSort = timeSort != null && !timeSort.trim().isEmpty() && !"DEFAULT".equalsIgnoreCase(timeSort.trim());

        // Fast-path: Nếu không tìm kiếm theo từ khóa và không chọn sắp xếp tùy biến
        if (!hasSearch && !hasCustomSort) {
            Page<OfferProposal> pagedEntities;
            if (!hasStatus) {
                pagedEntities = offerProposalRepository.findAllActiveByOrderByOfferIdAsc(pageable);
            } else {
                pagedEntities = offerProposalRepository.findActiveByStatusOrderByOfferIdAsc(status.trim(), pageable);
            }
            return pagedEntities.map(this::mapToResponse);
        }

        // Cấu hình sắp xếp theo thời gian (EARLIEST / LATEST / DEFAULT)
        Sort sort;
        if ("EARLIEST".equalsIgnoreCase(timeSort)) {
            sort = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("offerId"));
        } else if ("LATEST".equalsIgnoreCase(timeSort)) {
            sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("offerId"));
        } else {
            sort = Sort.by(Sort.Order.asc("offerId"));
        }

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Specification<OfferProposal> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Chỉ lấy các bản ghi chưa bị xóa mềm
            predicates.add(builder.or(
                    builder.isNull(root.get("isDeleted")),
                    builder.isFalse(root.get("isDeleted"))
            ));

            // 2. Lọc theo từ khóa (tên ứng viên, vị trí đề xuất)
            if (hasSearch) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Join<OfferProposal, Application> appJoin = root.join("application", JoinType.LEFT);
                Join<Application, Candidate> candJoin = appJoin.join("candidate", JoinType.LEFT);
                Join<Candidate, User> userJoin = candJoin.join("account", JoinType.LEFT);

                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("offeredPositionTitle")), pattern),
                        builder.like(builder.lower(userJoin.get("fullName")), pattern)
                ));
            }

            // 3. Lọc theo trạng thái
            if (hasStatus) {
                String s = status.trim();
                if ("Director_Approved".equalsIgnoreCase(s) || "Approved".equalsIgnoreCase(s)) {
                    predicates.add(builder.or(
                            builder.equal(builder.lower(root.get("offerStatus")), "director_approved"),
                            builder.equal(builder.lower(root.get("offerStatus")), "approved")
                    ));
                } else if ("Director_Rejected".equalsIgnoreCase(s) || "Rejected".equalsIgnoreCase(s)) {
                    predicates.add(builder.or(
                            builder.equal(builder.lower(root.get("offerStatus")), "director_rejected"),
                            builder.equal(builder.lower(root.get("offerStatus")), "rejected")
                    ));
                } else {
                    predicates.add(builder.equal(builder.lower(root.get("offerStatus")), s.toLowerCase(Locale.ROOT)));
                }
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };

        Page<OfferProposal> pagedEntities = offerProposalRepository.findAll(spec, sortedPageable);
        return pagedEntities.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getOfferStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("statPending", offerProposalRepository.countPendingDirector());
        stats.put("statApproved", offerProposalRepository.countDirectorApproved());
        stats.put("statSent", offerProposalRepository.countSentCandidate());
        stats.put("statAccepted", offerProposalRepository.countAccepted());
        return stats;
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

        String proposedByName = offer.getProposedBy() != null ? offer.getProposedBy().getFullName() : null;
        Integer proposedById = offer.getProposedBy() != null ? offer.getProposedBy().getUserId() : null;

        return OfferDetailResponse.builder()
                .offerId(offer.getOfferId())
                .offerStatus(offer.getOfferStatus())
                .declineReason(offer.getDeclineReason())
                .offeredPositionTitle(offer.getOfferedPositionTitle())
                .proposedSalary(offer.getProposedSalary())
                .probationSalary(offer.getProbationSalary())
                .probationDays(60)
                .expectedStartDate(offer.getExpectedStartDate())
                .workLocation(offer.getWorkLocation())
                .benefitsPackage(offer.getBenefitsPackage())
                .responsePeriodDays(offer.getResponsePeriodDays())
                .responseDeadline(offer.getResponseDeadline())
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
                .build();
    }

    @Override
    public OfferResponse updateOfferByHr(Integer id, UpdateOfferRequest dto) {
        OfferProposal offer = offerProposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy OfferProposal với ID: " + id));

        String currentStatus = offer.getOfferStatus();
        boolean canEdit = "Draft".equalsIgnoreCase(currentStatus);

        if (!canEdit) {
            throw new OfferValidationException(
                    "Theo quy tắc GBR-07, chỉ được phép cập nhật Offer khi ở trạng thái thuộc Nhóm A (Draft). Trạng thái hiện tại: "
                            + currentStatus,
                    "OFFER_STATUS_INVALID");
        }

        validateOfferBusinessRules(
                dto.getProposedSalary(),
                dto.getProbationSalary(),
                dto.getProbationDays(),
                dto.getExpectedStartDate(),
                dto.getWorkLocation(),
                dto.getOfferedPositionTitle(),
                dto.getResponsePeriodDays());

        if (dto.getOfferedPositionTitle() != null && !dto.getOfferedPositionTitle().isBlank()) {
            offer.setOfferedPositionTitle(dto.getOfferedPositionTitle());
        }
        offer.setProposedSalary(dto.getProposedSalary());
        offer.setProbationSalary(dto.getProbationSalary());
        offer.setExpectedStartDate(dto.getExpectedStartDate());
        offer.setWorkLocation(dto.getWorkLocation());
        offer.setBenefitsPackage(dto.getBenefitsPackage());
        if (dto.getResponsePeriodDays() != null) {
            offer.setResponsePeriodDays(dto.getResponsePeriodDays());
        }

        if (Boolean.TRUE.equals(dto.getIsDraft())) {
            offer.setOfferStatus("Draft");
        } else {
            offer.setOfferStatus("Pending_Director");
        }

        OfferProposal saved = offerProposalRepository.save(offer);

        if ("Pending_Director".equals(offer.getOfferStatus())) {
            if (offerMailSender != null) {
                try {
                    offerMailSender.notifyDirectorNewPendingOffer(saved);
                } catch (Exception e) {
                    log.warn("Lỗi khi gửi email thông báo Director về Offer cập nhật ID {}: {}", saved.getOfferId(), e.getMessage());
                }
            }
            if (systemNotificationService != null) {
                try {
                    User currentHr = resolveProposedBy(null);
                    systemNotificationService.notifyOfferPendingDirector(saved, currentHr != null ? currentHr : saved.getProposedBy());
                } catch (Exception e) {
                    log.warn("Lỗi khi gửi in-app notification Director về Offer cập nhật ID {}: {}", saved.getOfferId(), e.getMessage());
                }
            }
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UpdateOfferRequest getUpdateOfferRequestById(Integer id) {
        OfferProposal offer = offerProposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy OfferProposal với ID: " + id));

        String currentStatus = offer.getOfferStatus();
        if (!"Draft".equalsIgnoreCase(currentStatus)) {
            throw new OfferValidationException(
                    "Theo quy tắc GBR-07, chỉ được phép chỉnh sửa Offer khi ở trạng thái Bản thảo (Draft). Trạng thái hiện tại: "
                            + currentStatus,
                    "OFFER_STATUS_INVALID");
        }

        return UpdateOfferRequest.builder()
                .offeredPositionTitle(offer.getOfferedPositionTitle())
                .proposedSalary(offer.getProposedSalary())
                .probationSalary(offer.getProbationSalary())
                .probationDays(60)
                .expectedStartDate(offer.getExpectedStartDate())
                .workLocation(offer.getWorkLocation())
                .benefitsPackage(offer.getBenefitsPackage())
                .responsePeriodDays(offer.getResponsePeriodDays() != null ? offer.getResponsePeriodDays() : 5)
                .isDraft(true)
                .build();
    }

    @Override
    public void deleteDraftOfferByHr(Integer id) {
        OfferProposal offer = offerProposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy OfferProposal với ID: " + id));

        if (!"Draft".equalsIgnoreCase(offer.getOfferStatus())) {
            throw new OfferValidationException(
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
            throw new OfferValidationException(
                    "Chỉ được gửi thư mời nhận việc khi Offer đã được Director phê duyệt. Trạng thái hiện tại: "
                            + currentStatus,
                    "OFFER_NOT_APPROVED");
        }

        if (offer.getResponsePeriodDays() == null || offer.getResponsePeriodDays() <= 0) {
            throw new OfferValidationException("Offer chưa có thời gian phản hồi hợp lệ.");
        }

        LocalDateTime deadline = LocalDateTime.now().plusDays(offer.getResponsePeriodDays());

        if (offer.getExpectedStartDate() != null
                && !deadline.toLocalDate().isBefore(offer.getExpectedStartDate())) {
            throw new OfferValidationException("Thời hạn phản hồi phải trước ngày bắt đầu dự kiến.");
        }

        offer.setResponseDeadline(deadline);
        offer.setOfferStatus("Sent_Candidate");
        OfferProposal saved = offerProposalRepository.save(offer);

        if (offerMailSender != null) {
            try {
                offerMailSender.sendOfferLetterToCandidate(saved);
            } catch (Exception e) {
                log.warn("Lỗi khi gửi email thư mời nhận việc cho ứng viên Offer ID {}: {}", saved.getOfferId(), e.getMessage());
            }
        }

        if (systemNotificationService != null) {
            try {
                systemNotificationService.notifyOfferLetterSent(saved);
            } catch (Exception e) {
                log.warn("Lỗi khi gửi in-app notification thư mời nhận việc cho ứng viên Offer ID {}: {}", saved.getOfferId(), e.getMessage());
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
            throw new OfferValidationException("Mức lương đề xuất và lương thử việc không được để trống.");
        }

        if (proposedSalary.compareTo(BigDecimal.ZERO) <= 0 || probationSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OfferValidationException("Mức lương đề xuất và lương thử việc phải lớn hơn 0.");
        }

        BigDecimal minProbationSalary = proposedSalary.multiply(new BigDecimal("0.85"));

        if (probationSalary.compareTo(minProbationSalary) < 0) {
            throw new OfferValidationException(
                    "Lương thử việc (" + probationSalary + " VND) phải đạt tối thiểu 85% lương chính thức ("
                            + minProbationSalary + " VND) theo quy định Luật Lao động .");
        }

        if (probationSalary.compareTo(proposedSalary) > 0) {
            throw new OfferValidationException("Lương thử việc không được vượt quá lương chính thức.");
        }
    }

    private void validateOfferBusinessRules(
            BigDecimal proposedSalary,
            BigDecimal probationSalary,
            Integer probationDays,
            LocalDate expectedStartDate,
            String workLocation,
            String offeredPositionTitle,
            Integer responsePeriodDays) {

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
        if (responsePeriodDays == null || responsePeriodDays <= 0) {
            throw new OfferValidationException("Thời gian phản hồi phải lớn hơn 0 ngày.");
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
                .responsePeriodDays(entity.getResponsePeriodDays())
                .responseDeadline(entity.getResponseDeadline())
                .proposedById(proposedById)
                .proposedByName(proposedByName)
                .offerStatus(entity.getOfferStatus())
                .declineReason(entity.getDeclineReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public OfferResponse approveOfferByDirector(Integer id, String comments) {
        OfferProposal offer = offerProposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy OfferProposal với ID: " + id));

        if (!"Pending_Director".equalsIgnoreCase(offer.getOfferStatus())) {
            throw new OfferValidationException(
                    "Chỉ được phê duyệt đề xuất Offer khi ở trạng thái chờ duyệt (Pending_Director). Trạng thái hiện tại: "
                            + offer.getOfferStatus(),
                    "OFFER_STATUS_INVALID");
        }

        User director = resolveProposedBy(null); // Resolve current logged in user (Director)
        if (director == null) {
            throw new OfferValidationException("Không xác định được danh tính Director để thực hiện phê duyệt.", "USER_NOT_FOUND");
        }

        offer.setOfferStatus("Director_Approved");
        OfferProposal saved = offerProposalRepository.save(offer);

        OfferApproval approval = OfferApproval.builder()
                .offerProposal(saved)
                .director(director)
                .status("Approved")
                .directorComments(comments)
                .approvedAt(LocalDateTime.now())
                .build();
        offerApprovalRepository.save(approval);

        if (offerMailSender != null) {
            try {
                offerMailSender.notifyHrOfDirectorDecision(saved, "Approved", comments);
            } catch (Exception e) {
                log.warn("Lỗi khi gửi email thông báo HR duyệt Offer ID {}: {}", saved.getOfferId(), e.getMessage());
            }
        }

        if (systemNotificationService != null) {
            try {
                systemNotificationService.notifyDirectorDecisionOnOffer(saved, director, true, comments);
            } catch (Exception e) {
                log.warn("Lỗi khi gửi in-app notification duyệt Offer ID {}: {}", saved.getOfferId(), e.getMessage());
            }
        }

        return mapToResponse(saved);
    }

    @Override
    public OfferResponse rejectOfferByDirector(Integer id, String comments) {
        OfferProposal offer = offerProposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy OfferProposal với ID: " + id));

        if (!"Pending_Director".equalsIgnoreCase(offer.getOfferStatus())) {
            throw new OfferValidationException(
                    "Chỉ được từ chối đề xuất Offer khi ở trạng thái chờ duyệt (Pending_Director). Trạng thái hiện tại: "
                            + offer.getOfferStatus(),
                    "OFFER_STATUS_INVALID");
        }

        User director = resolveProposedBy(null); // Resolve current logged in user (Director)
        if (director == null) {
            throw new OfferValidationException("Không xác định được danh tính Director để thực hiện từ chối.", "USER_NOT_FOUND");
        }

        offer.setOfferStatus("Director_Rejected");
        OfferProposal saved = offerProposalRepository.save(offer);

        OfferApproval approval = OfferApproval.builder()
                .offerProposal(saved)
                .director(director)
                .status("Rejected")
                .directorComments(comments)
                .approvedAt(LocalDateTime.now())
                .build();
        offerApprovalRepository.save(approval);

        if (offerMailSender != null) {
            try {
                offerMailSender.notifyHrOfDirectorDecision(saved, "Rejected", comments);
            } catch (Exception e) {
                log.warn("Lỗi khi gửi email thông báo HR từ chối Offer ID {}: {}", saved.getOfferId(), e.getMessage());
            }
        }

        if (systemNotificationService != null) {
            try {
                systemNotificationService.notifyDirectorDecisionOnOffer(saved, director, false, comments);
            } catch (Exception e) {
                log.warn("Lỗi khi gửi in-app notification từ chối Offer ID {}: {}", saved.getOfferId(), e.getMessage());
            }
        }

        return mapToResponse(saved);
    }
    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getDirectorOfferStats() {
        User director = resolveProposedBy(null);
        Integer directorId = director != null ? director.getUserId() : 0;
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("statPending", offerProposalRepository.countPendingDirector());
        stats.put("statApproved", offerApprovalRepository.countByDirector_UserIdAndStatusIgnoreCase(directorId, "Approved"));
        stats.put("statRejected", offerApprovalRepository.countByDirector_UserIdAndStatusIgnoreCase(directorId, "Rejected"));
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getDirectorStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("ALL", offerProposalRepository.countAllNonDraft());
        counts.put("Pending_Director", offerProposalRepository.countPendingDirector());
        counts.put("Director_Approved", offerProposalRepository.countDirectorApproved());
        counts.put("Sent_Candidate", offerProposalRepository.countSentCandidate());
        counts.put("Accepted", offerProposalRepository.countAccepted());
        counts.put("Declined", offerProposalRepository.countDeclined());
        counts.put("Director_Rejected", offerProposalRepository.countDirectorRejected());
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfferResponse> getDirectorOffers(String search, String status, String timeSort, Pageable pageable) {
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasStatus = status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim());

        Sort sort;
        if ("EARLIEST".equalsIgnoreCase(timeSort)) {
            sort = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("offerId"));
        } else if ("LATEST".equalsIgnoreCase(timeSort)) {
            sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("offerId"));
        } else {
            sort = Sort.by(Sort.Order.desc("offerId"));
        }

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Specification<OfferProposal> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Chỉ lấy các bản ghi chưa bị xóa mềm
            predicates.add(builder.or(
                    builder.isNull(root.get("isDeleted")),
                    builder.isFalse(root.get("isDeleted"))
            ));

            // 2. Không hiển thị bản nháp cho Director (chỉ các đề xuất đã được HR trình lên)
            predicates.add(builder.notEqual(builder.lower(root.get("offerStatus")), "draft"));

            // 3. Tìm kiếm theo tên ứng viên, vị trí tuyển dụng, hoặc mã Offer ID
            if (hasSearch) {
                String term = search.trim();
                String pattern = "%" + term.toLowerCase(Locale.ROOT) + "%";
                Join<OfferProposal, Application> appJoin = root.join("application", JoinType.LEFT);
                Join<Application, Candidate> candJoin = appJoin.join("candidate", JoinType.LEFT);
                Join<Candidate, User> userJoin = candJoin.join("account", JoinType.LEFT);

                List<Predicate> searchPredicates = new ArrayList<>();
                searchPredicates.add(builder.like(builder.lower(root.get("offeredPositionTitle")), pattern));
                searchPredicates.add(builder.like(builder.lower(userJoin.get("fullName")), pattern));

                try {
                    Integer searchId = Integer.parseInt(term);
                    searchPredicates.add(builder.equal(root.get("offerId"), searchId));
                } catch (NumberFormatException ignored) {
                    // Không phải số thì bỏ qua tìm kiếm theo ID
                }

                predicates.add(builder.or(searchPredicates.toArray(new Predicate[0])));
            }

            // 4. Lọc theo trạng thái
            if (hasStatus) {
                String s = status.trim();
                if ("HISTORY".equalsIgnoreCase(s) || "HISTORY_ALL".equalsIgnoreCase(s)) {
                    predicates.add(builder.notEqual(builder.lower(root.get("offerStatus")), "pending_director"));
                } else if ("Director_Approved".equalsIgnoreCase(s) || "Approved".equalsIgnoreCase(s)) {
                    predicates.add(builder.or(
                            builder.equal(builder.lower(root.get("offerStatus")), "director_approved"),
                            builder.equal(builder.lower(root.get("offerStatus")), "approved")
                    ));
                } else if ("Director_Rejected".equalsIgnoreCase(s) || "Rejected".equalsIgnoreCase(s)) {
                    predicates.add(builder.or(
                            builder.equal(builder.lower(root.get("offerStatus")), "director_rejected"),
                            builder.equal(builder.lower(root.get("offerStatus")), "rejected")
                    ));
                } else {
                    predicates.add(builder.equal(builder.lower(root.get("offerStatus")), s.toLowerCase(Locale.ROOT)));
                }
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };

        Page<OfferProposal> pagedEntities = offerProposalRepository.findAll(spec, sortedPageable);
        return pagedEntities.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfferResponse> getDirectorOfferHistory(Pageable pageable) {
        User director = resolveProposedBy(null);
        Integer directorId = director != null ? director.getUserId() : 0;
        
        Page<OfferApproval> approvals = offerApprovalRepository.findByDirector_UserIdOrderByApprovedAtDesc(directorId, pageable);
        return approvals.map(approval -> {
            OfferProposal offer = approval.getOfferProposal();
            return mapToResponse(offer);
        });
    }
}
