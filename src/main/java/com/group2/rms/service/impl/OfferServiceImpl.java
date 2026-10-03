package com.group2.rms.service.impl;

import com.group2.rms.dto.request.CreateOfferRequestDto;
import com.group2.rms.dto.response.OfferResponseDto;
import com.group2.rms.entity.Application;
import com.group2.rms.entity.OfferProposal;
import com.group2.rms.entity.User;
import com.group2.rms.repository.ApplicationRepository;
import com.group2.rms.repository.OfferProposalRepository;
import com.group2.rms.repository.UserRepository;
import com.group2.rms.service.OfferService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OfferServiceImpl implements OfferService {

    private final OfferProposalRepository offerProposalRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Override
    public OfferResponseDto createOfferProposal(CreateOfferRequestDto request) {
        // 1. KIỂM TRA QUY TẮC BR-OFF-01: Lương thử việc >= 85% lương chính thức
        validateProbationSalaryRule(request.getProposedSalary(), request.getProbationSalary());

        // 2. TÌM ĐƠN ỨNG TUYỂN (APPLICATION)
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Application với ID: " + request.getApplicationId()));

        // 3. TÌM NGƯỜI ĐỀ XUẤT (USER)
        User proposedBy = null;
        if (request.getProposedById() != null) {
            proposedBy = userRepository.findById(request.getProposedById())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy User đề xuất với ID: " + request.getProposedById()));
        }

        // 4. XỬ LÝ QUY TẮC GBR-14 / GBR-07: Vô hiệu hóa các Offer cũ đang active
        deactivateExistingActiveOffers(application.getApplicationId());

        // 5. KHỞI TẠO VÀ LƯU OFFER PROPOSAL MỚI
        OfferProposal newOffer = OfferProposal.builder()
                .application(application)
                .offeredPositionTitle(request.getOfferedPositionTitle())
                .proposedSalary(request.getProposedSalary())
                .probationSalary(request.getProbationSalary())
                .expectedStartDate(request.getExpectedStartDate())
                .workLocation(request.getWorkLocation())
                .benefitsPackage(request.getBenefitsPackage())
                .proposedBy(proposedBy)
                .offerStatus("Pending_Director") // Trạng thái chờ Director duyệt
                .build();

        OfferProposal savedOffer = offerProposalRepository.save(newOffer);
        return mapToResponseDto(savedOffer);
    }

    @Override
    @Transactional(readOnly = true)
    public OfferResponseDto getOfferById(Integer offerId) {
        OfferProposal offer = offerProposalRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy OfferProposal với ID: " + offerId));
        return mapToResponseDto(offer);
    }

    @Override
    @Transactional(readOnly = true)
    public OfferResponseDto getOfferByApplicationId(Integer applicationId) {
        OfferProposal offer = offerProposalRepository.findByApplication_ApplicationId(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy OfferProposal cho Application ID: " + applicationId));
        return mapToResponseDto(offer);
    }

    /**
     * Hàm Validate quy tắc BR-OFF-01:
     * - Lương đề xuất và lương thử việc không được để trống.
     * - Cả 2 mức lương phải lớn hơn 0.
     * - Lương thử việc phải đạt tối thiểu 85% lương chính thức.
     */
    private void validateProbationSalaryRule(BigDecimal proposedSalary, BigDecimal probationSalary) {
        if (proposedSalary == null || probationSalary == null) {
            throw new IllegalArgumentException("Mức lương đề xuất và lương thử việc không được để trống.");
        }

        if (proposedSalary.compareTo(BigDecimal.ZERO) <= 0 || probationSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Mức lương đề xuất và lương thử việc phải lớn hơn 0.");
        }

        // Tính 85% mức lương chính thức: proposedSalary * 0.85
        BigDecimal minProbationSalary = proposedSalary.multiply(new BigDecimal("0.85"));

        // So sánh: probationSalary < minProbationSalary
        if (probationSalary.compareTo(minProbationSalary) < 0) {
            throw new IllegalArgumentException(
                    "Lương thử việc (" + probationSalary + " VND) phải đạt tối thiểu 85% lương chính thức ("
                            + minProbationSalary + " VND) theo quy định Luật Lao động (BR-OFF-01)."
            );
        }
    }

    /**
     * Hàm xử lý quy tắc GBR-14 / GBR-07:
     * Chuyển tất cả các Offer cũ đang active (Draft, Pending_Director...) sang trạng thái "Voided".
     */
    private void deactivateExistingActiveOffers(Integer applicationId) {
        // Các trạng thái được coi là đã kết thúc / không còn active
        List<String> inactiveStatuses = List.of("Voided", "Canceled", "Rejected", "Declined", "Director_Rejected", "Expired");

        // Tìm tất cả các OfferProposal cũ còn active của Application này
        List<OfferProposal> activeOffers = offerProposalRepository
                .findByApplication_ApplicationIdAndOfferStatusNotIn(applicationId, inactiveStatuses);

        // Chuyển trạng thái các Offer cũ sang "Voided"
        for (OfferProposal oldOffer : activeOffers) {
            oldOffer.setOfferStatus("Voided");
        }

        if (!activeOffers.isEmpty()) {
            offerProposalRepository.saveAll(activeOffers);
            offerProposalRepository.flush(); // Đẩy update ngay lập tức xuống DB trước khi insert record mới
        }
    }

    /**
     * Ánh xạ từ OfferProposal Entity sang OfferResponseDto
     */
    private OfferResponseDto mapToResponseDto(OfferProposal entity) {
        String candidateName = null;
        String candidateEmail = null;
        if (entity.getApplication() != null && entity.getApplication().getCandidate() != null) {
            var candUser = entity.getApplication().getCandidate().getUser();
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

        return OfferResponseDto.builder()
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
