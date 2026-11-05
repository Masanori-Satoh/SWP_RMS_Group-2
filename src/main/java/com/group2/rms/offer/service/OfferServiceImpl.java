package com.group2.rms.offer.service;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.interview.entity.InterviewFinalResult;
import com.group2.rms.offer.dto.CreateOfferRequest;
import com.group2.rms.offer.dto.OfferDetailResponse;
import com.group2.rms.offer.dto.OfferResponse;
import com.group2.rms.offer.dto.OfferResponse.EmailStatus;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.group2.rms.candidate.domain.entity.Candidate;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final Clock clock;
    private final PlatformTransactionManager transactionManager;
    private final com.group2.rms.notification.service.NotificationService systemNotificationService;

    /**
     * Quy tắc GBR-07:
     * - Chỉ chấp nhận tạo mới Offer Proposal đối với ứng viên đã đỗ phỏng vấn (Passed) và KHÔNG CÓ Offer chưa xóa mềm.
     * - Khi hồ sơ đã có Offer chưa xóa mềm (kể cả Draft), không gợi ý trong mục tạo đề xuất mới
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

    // Hai helper trạng thái tương thích cũ chưa có caller trong production hiện tại; luồng ghi kiểm tra trực tiếp bên dưới.
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


    private OfferProposal findActiveOffer(Integer id) {
        return offerProposalRepository.findById(id)
                .filter(offer -> !Boolean.TRUE.equals(offer.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề xuất thư mời với ID: " + id));
    }

    /**
     * Đọc Offer chưa xóa và giữ khóa ghi đến khi giao dịch kết thúc.
     * HR phải kiểm tra trạng thái sau khi có khóa để hai yêu cầu không cùng phát hành hoặc ghi đè bản nháp.
     */
    private OfferProposal findActiveOfferForUpdate(Integer id) {
        return offerProposalRepository.findActiveForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề xuất thư mời với ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public OfferResponse getOfferById(Integer offerId) {
        OfferProposal offer = findActiveOffer(offerId);
        return mapToResponse(offer);
    }


    /**
     * Nguồn cho dropdown tạo đề xuất: hồ sơ có kết quả Passed, chưa kết thúc và chưa có Offer chưa xóa mềm.
     * Loại theo ApplicationId, không theo CandidateId: một ứng viên có thể ứng tuyển nhiều tin.
     * Đọc Offer hiện hữu theo lô 500 ID; kết quả trùng hồ sơ chỉ đưa vào dropdown một lần.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PassedCandidateResponse> getPassedCandidatesForOffer() {
        List<InterviewFinalResult> passedResults = interviewFinalResultRepository.findAllPassedWithDetails();
        if (passedResults == null) {
            return Collections.emptyList();
        }

        List<Integer> applicationIds = passedResults.stream().map(result -> result.getInterviewSchedule())
                .filter(java.util.Objects::nonNull).map(schedule -> schedule.getApplication())
                .filter(application -> application != null && application.getApplicationId() != null && !isApplicationEnded(application))
                .map(Application::getApplicationId).distinct().toList();
        Set<Integer> existingApplications = new HashSet<>();
        // Chia lô 500 ID để không vượt giới hạn 2100 tham số của SQL Server.
        for (int offset = 0; offset < applicationIds.size(); offset += 500) {
            existingApplications.addAll(offerProposalRepository.findActiveApplicationIds(
                    applicationIds.subList(offset, Math.min(offset + 500, applicationIds.size()))));
        }

        List<PassedCandidateResponse> list = new ArrayList<>();
        Set<Integer> processedAppIds = new HashSet<>();

        for (InterviewFinalResult r : passedResults) {
            var schedule = r.getInterviewSchedule();
            var application = schedule != null ? schedule.getApplication() : null;
            if (application == null || application.getApplicationId() == null) {
                continue;
            }
            if (isApplicationEnded(application)) {
                continue;
            }

            Integer appId = application.getApplicationId();
            if (processedAppIds.contains(appId)) {
                continue;
            }

            if (existingApplications.contains(appId)) {
                continue;
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

    /**
     * HR tạo đề xuất: kiểm tra đãi ngộ → hồ sơ còn mở → chưa có Offer chưa xóa → có ít nhất một kết quả Passed.
     * Người đề xuất lấy từ tài khoản đăng nhập, không tin proposedById gửi lên từ giao diện.
     * isDraft=true lưu Draft; trường hợp còn lại trình Pending_Director và thông báo cho Giám đốc.
     * Nếu bản nháp cũ đã xóa mềm thì tái sử dụng bản ghi để giữ ràng buộc một Offer cho mỗi hồ sơ.
     */
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

        requireActiveApplication(application);

        String status = Boolean.TRUE.equals(dto.getIsDraft()) ? "Draft" : "Pending_Director";

        // Bảng OfferProposal có ràng buộc UNIQUE trên ApplicationId
        // (UQ_OfferProposal_Application)
        // Áp dụng quy tắc Single Active Offer Rule (GBR-07):
        // Chỉ chấp nhận tạo mới cho ứng viên chưa có Offer; nếu đã tồn tại Offer (kể cả Draft) thì chặn lại
        Optional<OfferProposal> existingOfferOpt = offerProposalRepository
                .findByApplicationForUpdate(application.getApplicationId());

        if (existingOfferOpt.isPresent() && !Boolean.TRUE.equals(existingOfferOpt.get().getIsDeleted())) {
            String currentStatus = existingOfferOpt.get().getOfferStatus();
            throw new OfferValidationException(
                    "Theo quy tắc GBR-07, không thể tạo mới đề xuất thư mời vì đơn ứng tuyển đang có gói đề xuất ở trạng thái ["
                            + OfferExportServiceImpl.mapOfferStatusLabel(currentStatus) + "]. Vui lòng chỉnh sửa trực tiếp trên danh sách đề xuất thay vì tạo mới.",
                    "OFFER_LOCKED_STATE");
        }

        boolean passedInterview = interviewFinalResultRepository
                .findByApplicationIdOrderByApprovedAtDesc(application.getApplicationId()).stream()
                .anyMatch(result -> InterviewFinalResult.PASSED.equalsIgnoreCase(result.getFinalDecision()));
        if (!passedInterview) {
            throw new OfferValidationException("Chỉ được tạo đề xuất thư mời cho hồ sơ đã đạt phỏng vấn.");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new OfferValidationException("Không xác định được tài khoản HR đang đăng nhập.", "USER_NOT_FOUND");
        }
        User proposedBy = userRepository.findByUsernameIgnoreCase(auth.getName())
                .orElseThrow(() -> new OfferValidationException(
                        "Không xác định được tài khoản HR đang đăng nhập.", "USER_NOT_FOUND"));

        Integer responsePeriodDays = dto.getResponsePeriodDays() != null ? dto.getResponsePeriodDays() : 5;

        OfferProposal offerToSave;
        if (existingOfferOpt.isPresent() && Boolean.TRUE.equals(existingOfferOpt.get().getIsDeleted())) {
            offerToSave = existingOfferOpt.get();
            offerToSave.setOfferedPositionTitle(dto.getOfferedPositionTitle());
            offerToSave.setProposedSalary(dto.getProposedSalary());
            offerToSave.setProbationSalary(dto.getProbationSalary());
            offerToSave.setProbationDays(dto.getProbationDays());
            offerToSave.setExpectedStartDate(dto.getExpectedStartDate());
            offerToSave.setWorkLocation(dto.getWorkLocation());
            offerToSave.setBenefitsPackage(dto.getBenefitsPackage());
            offerToSave.setResponsePeriodDays(responsePeriodDays);
            offerToSave.setResponseDeadline(null);
            offerToSave.setProposedBy(proposedBy);
            offerToSave.setOfferStatus(status);
            offerToSave.setIsDeleted(false);
            offerToSave.setDeclineReason(null);
        } else {
            offerToSave = OfferProposal.builder()
                    .application(application)
                    .offeredPositionTitle(dto.getOfferedPositionTitle())
                    .proposedSalary(dto.getProposedSalary())
                    .probationSalary(dto.getProbationSalary())
                    .probationDays(dto.getProbationDays())
                    .expectedStartDate(dto.getExpectedStartDate())
                    .workLocation(dto.getWorkLocation())
                    .benefitsPackage(dto.getBenefitsPackage())
                    .responsePeriodDays(responsePeriodDays)
                    .responseDeadline(null)
                    .proposedBy(proposedBy)
                    .offerStatus(status)
                    .isDeleted(false)
                    .build();
        }

        OfferProposal saved = offerProposalRepository.save(offerToSave);

        if ("Pending_Director".equals(status)) {
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

    /**
     * Danh sách HR: lọc bản ghi chưa xóa, phân trang và chuyển entity thành DTO trong giao dịch đọc.
     * Khi có tìm kiếm/sắp xếp tùy chọn, dùng cùng bộ lọc với xuất Excel để kết quả nhất quán.
     */
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

        Specification<OfferProposal> spec = OfferProposalRepository.matchingFilters(search, status);

        Page<OfferProposal> pagedEntities = offerProposalRepository.findAll(spec, sortedPageable);
        return pagedEntities.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getOfferStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("statTotal", offerProposalRepository.countActiveOffers());
        stats.put("statPending", offerProposalRepository.countPendingDirector());
        stats.put("statApproved", offerProposalRepository.countDirectorApproved());
        stats.put("statSent", offerProposalRepository.countSentCandidate());
        stats.put("statAccepted", offerProposalRepository.countAccepted());
        return stats;
    }

    /**
     * Ghép trang chi tiết: thông tin hồ sơ/phỏng vấn, nội dung đề xuất và lịch sử quyết định của Giám đốc.
     * Trạng thái hiện tại của Offer và quyết định trong lịch sử là hai dữ liệu khác nhau.
     */
    @Override
    @Transactional(readOnly = true)
    public OfferDetailResponse getOfferDetailForHr(Integer id) {
        OfferProposal offer = findActiveOffer(id);

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
                .probationDays(offer.getProbationDays())
                .expectedStartDate(offer.getExpectedStartDate())
                .workLocation(offer.getWorkLocation())
                .benefitsPackage(offer.getBenefitsPackage())
                .responsePeriodDays(offer.getResponsePeriodDays())
                .responseDeadline(offer.getResponseDeadline())
                .responseExpired(offer.isResponseExpiredAt(LocalDateTime.now(clock)))
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

    /**
     * HR chỉ sửa Draft: lấy khóa ghi → kiểm tra hồ sơ/trạng thái → kiểm tra dữ liệu → lưu.
     * Lưu nháp giữ Draft; trình duyệt chuyển sang Pending_Director. Offer đã bị từ chối không được sửa ở đường này.
     */
    @Override
    public OfferResponse updateOfferByHr(Integer id, UpdateOfferRequest dto) {
        OfferProposal offer = findActiveOfferForUpdate(id);
        requireActiveApplication(offer.getApplication());

        String currentStatus = offer.getOfferStatus();
        boolean canEdit = "Draft".equalsIgnoreCase(currentStatus);

        if (!canEdit) {
            throw new OfferValidationException(
                    "Theo quy tắc GBR-07, chỉ được phép cập nhật đề xuất khi ở trạng thái Bản nháp (Draft). Trạng thái hiện tại: "
                            + OfferExportServiceImpl.mapOfferStatusLabel(currentStatus),
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
        offer.setProbationDays(dto.getProbationDays());
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
        OfferProposal offer = findActiveOffer(id);

        String currentStatus = offer.getOfferStatus();
        if (!"Draft".equalsIgnoreCase(currentStatus)) {
            throw new OfferValidationException(
                    "Theo quy tắc GBR-07, chỉ được phép chỉnh sửa đề xuất khi ở trạng thái Bản nháp (Draft). Trạng thái hiện tại: "
                            + OfferExportServiceImpl.mapOfferStatusLabel(currentStatus),
                    "OFFER_STATUS_INVALID");
        }

        return UpdateOfferRequest.builder()
                .offeredPositionTitle(offer.getOfferedPositionTitle())
                .proposedSalary(offer.getProposedSalary())
                .probationSalary(offer.getProbationSalary())
                .probationDays(offer.getProbationDays())
                .expectedStartDate(offer.getExpectedStartDate())
                .workLocation(offer.getWorkLocation())
                .benefitsPackage(offer.getBenefitsPackage())
                .responsePeriodDays(offer.getResponsePeriodDays() != null ? offer.getResponsePeriodDays() : 5)
                .isDraft(true)
                .build();
    }

    /**
     * Xóa mềm bản nháp: khóa và kiểm tra Draft rồi đặt isDeleted=true.
     * Bản ghi vẫn nằm trong database; thao tác này không phải hủy một thư mời đã phát hành.
     */
    @Override
    public void deleteDraftOfferByHr(Integer id) {
        OfferProposal offer = findActiveOfferForUpdate(id);

        if (!"Draft".equalsIgnoreCase(offer.getOfferStatus())) {
            throw new OfferValidationException(
                    "Chỉ được phép xóa bản nháp đề xuất thư mời. Trạng thái hiện tại: " + OfferExportServiceImpl.mapOfferStatusLabel(offer.getOfferStatus()),
                    "OFFER_NOT_DRAFT");
        }

        // Soft delete: Xóa đề xuất ra khỏi bảng danh sách hiển thị, KHÔNG xóa hẳn ra
        // khỏi database
        offer.setIsDeleted(true);
        offerProposalRepository.save(offer);
    }

    /**
     * Phát hành là bước riêng sau khi Giám đốc duyệt: Approved/Director_Approved → Sent_Candidate.
     * Hạn trả lời tính từ thời điểm phát hành, phải trước ngày bắt đầu dự kiến; hồ sơ chuyển sang Offered.
     * Nếu hồ sơ vừa đổi trạng thái, cập nhật có điều kiện thất bại thì hủy giao dịch để không hồi sinh hồ sơ đã đóng.
     * Email chỉ gửi sau commit; gửi email thất bại không đảo ngược việc thư mời đã xuất hiện trên cổng ứng viên.
     */
    @Override
    public OfferResponse sendOfferToCandidate(Integer id) {
        OfferProposal offer = findActiveOfferForUpdate(id);
        requireActiveApplication(offer.getApplication());

        String currentStatus = offer.getOfferStatus();
        boolean isApproved = "Approved".equalsIgnoreCase(currentStatus)
                || "Director_Approved".equalsIgnoreCase(currentStatus);

        if (!isApproved) {
            throw new OfferValidationException(
                    "Chỉ được gửi thư mời nhận việc khi đề xuất đã được Giám đốc phê duyệt. Trạng thái hiện tại: "
                            + OfferExportServiceImpl.mapOfferStatusLabel(currentStatus),
                    "OFFER_NOT_APPROVED");
        }

        if (offer.getResponsePeriodDays() == null || offer.getResponsePeriodDays() <= 0) {
            throw new OfferValidationException("Thư mời nhận việc chưa có thời gian phản hồi hợp lệ.");
        }

        LocalDateTime deadline = LocalDateTime.now(clock).plusDays(offer.getResponsePeriodDays());

        if (offer.getExpectedStartDate() != null
                && !deadline.toLocalDate().isBefore(offer.getExpectedStartDate())) {
            throw new OfferValidationException("Thời hạn phản hồi phải trước ngày bắt đầu dự kiến.");
        }

        offer.setResponseDeadline(deadline);
        offer.setOfferStatus("Sent_Candidate");
        OfferProposal saved = offerProposalRepository.save(offer);

        if (saved.getApplication() != null) {
            Application application = saved.getApplication();
            // So sánh trạng thái đã đọc với trạng thái còn trong DB; không gọi setApplicationStatus rồi save đè.
            if (application.getApplicationStatus() == null
                    || applicationRepository.transition(application.getApplicationId(),
                            Set.of(application.getApplicationStatus()), "Offered") != 1) {
                throw new OfferValidationException("Hồ sơ đã được cập nhật. Vui lòng tải lại trang.",
                        "APPLICATION_STATE_CONFLICT");
            }
        }

        if (systemNotificationService != null) {
            try {
                systemNotificationService.notifyOfferLetterSent(saved);
            } catch (Exception e) {
                log.warn("Lỗi khi gửi in-app notification thư mời nhận việc cho ứng viên Offer ID {}: {}", saved.getOfferId(), e.getMessage());
            }
        }

        OfferResponse response = mapToResponse(saved);
        response.setEmailStatus(EmailStatus.NOT_SENT);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendOfferEmail(saved, response);
                }
            });
        } else {
            sendOfferEmail(saved, response);
        }
        return response;
    }

    private void sendOfferEmail(OfferProposal offer, OfferResponse response) {
        try {
            response.setEmailStatus(offerMailSender.sendOfferLetterToCandidate(offer));
        } catch (Exception e) {
            log.warn("Lỗi khi gửi email cho Offer ID {}: {}", offer.getOfferId(), e.getClass().getSimpleName());
        }
    }

    private void requireActiveApplication(Application application) {
        if (isApplicationEnded(application)) {
            throw new OfferValidationException("Hồ sơ đã kết thúc nên không thể xử lý đề xuất thư mời.",
                    "APPLICATION_TERMINATED");
        }
    }

    private boolean isApplicationEnded(Application application) {
        return application != null && application.getApplicationStatus() != null
                && Set.of("Hired", "Rejected", "Withdrawn", "Cancelled")
                        .contains(application.getApplicationStatus());
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

    /**
     * Kiểm tra ở service để cả form và lời gọi Java đều chịu cùng luật lương/ngày/thời gian thử việc.
     * Lương thử việc phải trong khoảng 85%–100% lương chính thức; thử việc 1–365 ngày.
     * Ngày bắt đầu phải sau hôm nay cộng thời gian phản hồi. DTO còn có giới hạn/định dạng riêng trước khi vào đây.
     */
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
        if (probationDays == null || probationDays <= 0 || probationDays > 365) {
            throw new OfferValidationException("Thời gian thử việc phải từ 1 đến 365 ngày.");
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
        if (!expectedStartDate.isAfter(LocalDate.now().plusDays(responsePeriodDays))) {
            LocalDate minStartDate = LocalDate.now().plusDays(responsePeriodDays + 1);
            throw new OfferValidationException(
                    "Ngày bắt đầu dự kiến phải sau ngày hiện tại cộng với thời hạn phản hồi ("
                            + responsePeriodDays + " ngày). Vui lòng chọn ngày bắt đầu từ ngày "
                            + minStartDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " trở đi.");
        }
    }


    /**
     * Helper tương thích cũ: ưu tiên ID nếu được truyền, rồi tài khoản đăng nhập; cuối cùng vẫn có nhánh lấy user đầu tiên.
     * Tên hàm dễ gây nhầm vì cả quyết định Giám đốc cũng đang gọi hàm này.
     * Đề xuất thay bằng CurrentUserService.requireUser trong một thay đổi riêng; không coi fallback là quy tắc phân quyền.
     */
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
                .probationDays(entity.getProbationDays())
                .expectedStartDate(entity.getExpectedStartDate())
                .workLocation(entity.getWorkLocation())
                .benefitsPackage(entity.getBenefitsPackage())
                .responsePeriodDays(entity.getResponsePeriodDays())
                .responseDeadline(entity.getResponseDeadline())
                .responseExpired(entity.isResponseExpiredAt(LocalDateTime.now(clock)))
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
        return decideOffer(id, comments, true);
    }

    @Override
    public OfferResponse rejectOfferByDirector(Integer id, String comments) {
        return decideOffer(id, comments, false);
    }

    /**
     * Luồng chung cho Giám đốc duyệt/từ chối: chỉ nhận Offer Pending_Director và hồ sơ chưa kết thúc.
     * decideIfPending cập nhật có điều kiện; 0 dòng nghĩa là yêu cầu đã cũ, không ghi thêm lịch sử/thông báo.
     * OfferApproval lưu quyết định Approved/Rejected; OfferProposal lưu vòng đời Director_Approved/Director_Rejected.
     * Thông báo quyết định chạy sau commit trong giao dịch mới để lỗi thông báo không làm mất quyết định đã lưu.
     * Lý do từ chối bắt buộc đang được kiểm tra tại controller, không phải trong hàm dùng chung này.
     */
    private OfferResponse decideOffer(Integer id, String comments, boolean approved) {
        OfferProposal offer = findActiveOffer(id);
        requireActiveApplication(offer.getApplication());
        if (!"Pending_Director".equalsIgnoreCase(offer.getOfferStatus())) {
            throw decisionConflict();
        }

        User director = resolveProposedBy(null);
        if (director == null) {
            throw new OfferValidationException("Không xác định được danh tính Giám đốc.", "USER_NOT_FOUND");
        }

        String status = approved ? "Director_Approved" : "Director_Rejected";
        if (offerProposalRepository.decideIfPending(id, status) == 0) {
            throw decisionConflict();
        }

        OfferProposal decided = findActiveOffer(id);
        offerApprovalRepository.save(new OfferApproval(decided, director, approved ? "Approved" : "Rejected", comments));

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        // Quyết định đã commit; mở giao dịch mới để ghi thông báo mà không kéo dài giao dịch duyệt.
                        TransactionTemplate notificationTransaction = new TransactionTemplate(transactionManager);
                        notificationTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        notificationTransaction.executeWithoutResult(transaction ->
                                systemNotificationService.notifyDirectorDecisionOnOffer(
                                        findActiveOffer(id), director, approved, comments));
                    } catch (Exception e) {
                        log.warn("Lỗi khi gửi thông báo quyết định cho Offer ID {}", id, e);
                    }
                }
            });
        } else {
            try {
                systemNotificationService.notifyDirectorDecisionOnOffer(decided, director, approved, comments);
            } catch (Exception e) {
                log.warn("Lỗi khi gửi thông báo quyết định cho Offer ID {}", id, e);
            }
        }
        return mapToResponse(decided);
    }

    private OfferValidationException decisionConflict() {
        return new OfferValidationException("Đề xuất thư mời đã được cập nhật. Vui lòng tải lại trang.",
                "OFFER_STATUS_INVALID");
    }

    /**
     * Thẻ thống kê: số chờ duyệt là toàn hệ thống; số đã duyệt/từ chối đếm log quyết định của Giám đốc hiện tại.
     * Không đồng nhất các số này với bộ đếm trạng thái vòng đời ở getDirectorStatusCounts.
     */
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

    /**
     * Danh sách Giám đốc luôn ẩn Draft. Nhóm đã duyệt còn bao gồm các Offer đã gửi/được nhận/bị ứng viên từ chối.
     * Đây là nhóm nghiệp vụ khác bộ lọc Approved của HR/Excel, nên không gộp máy móc hai điều kiện.
     * Tìm kiếm hỗ trợ thêm mã Offer; sắp xếp mặc định mới nhất theo ID, khác HR mặc định tăng dần.
     */
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
                            builder.equal(builder.lower(root.get("offerStatus")), "approved"),
                            builder.equal(builder.lower(root.get("offerStatus")), "sent_candidate"),
                            builder.equal(builder.lower(root.get("offerStatus")), "accepted"),
                            builder.equal(builder.lower(root.get("offerStatus")), "declined")
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


}
