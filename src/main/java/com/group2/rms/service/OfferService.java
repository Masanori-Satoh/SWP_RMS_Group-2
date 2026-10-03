package com.group2.rms.service;

import com.group2.rms.dto.request.CreateOfferRequestDto;
import com.group2.rms.dto.response.OfferResponseDto;

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
}
