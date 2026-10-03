package com.group2.rms.offer.service;

import com.group2.rms.dto.request.CreateOfferRequestDto;
import com.group2.rms.dto.request.UpdateOfferRequestDto;
import com.group2.rms.dto.response.OfferDetailResponseDto;
import com.group2.rms.dto.response.OfferResponseDto;
import com.group2.rms.dto.response.PassedCandidateResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OfferService {

    /**
     * Tạo mới OfferProposal áp dụng quy tắc BR-OFF-01 và GBR-14 (GBR-07).
     *
     * @param request Dữ liệu đề xuất lương
     * @return Thông tin OfferProposal đã được tạo
     */
    OfferResponseDto createOfferProposal(CreateOfferRequestDto request);

    /**
     * Lấy thông tin OfferProposal theo OfferId.
     */
    OfferResponseDto getOfferById(Integer offerId);

    /**
     * Lấy OfferProposal theo ApplicationId.
     */
    OfferResponseDto getOfferByApplicationId(Integer applicationId);

    /**
     * Lấy danh sách ứng viên đã vượt qua phỏng vấn (Passed) chờ tạo Offer Proposal.
     */
    List<PassedCandidateResponseDto> getPassedCandidatesForOffer();

    /**
     * HR tạo mới Offer Proposal (lưu Draft hoặc trình Director).
     */
    OfferResponseDto createOfferByHr(CreateOfferRequestDto dto);

    /**
     * Lấy danh sách Offer Proposal cho HR có lọc theo status và phân trang.
     */
    Page<OfferResponseDto> getAllOffersForHr(String status, Pageable pageable);

    /**
     * Lấy thông tin chi tiết gói Offer (Group A + B + History).
     */
    OfferDetailResponseDto getOfferDetailForHr(Integer id);

    /**
     * HR cập nhật Offer Proposal (chỉ khi Draft hoặc Rejected).
     */
    OfferResponseDto updateOfferByHr(Integer id, UpdateOfferRequestDto dto);

    /**
     * HR xóa bản thảo Offer (chỉ khi Draft).
     */
    void deleteDraftOfferByHr(Integer id);

    /**
     * HR gửi Offer Letter chính thức cho ứng viên sau khi Director Approved.
     */
    OfferResponseDto sendOfferToCandidate(Integer id);
}
