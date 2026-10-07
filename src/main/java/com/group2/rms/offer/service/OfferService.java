package com.group2.rms.offer.service;

import com.group2.rms.offer.dto.CreateOfferRequest;
import com.group2.rms.offer.dto.OfferDetailResponse;
import com.group2.rms.offer.dto.OfferResponse;
import com.group2.rms.offer.dto.PassedCandidateResponse;
import com.group2.rms.offer.dto.UpdateOfferRequest;
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
    OfferResponse createOfferProposal(CreateOfferRequest request);

    /**
     * Lấy thông tin OfferProposal theo OfferId.
     */
    OfferResponse getOfferById(Integer offerId);

    /**
     * Lấy OfferProposal theo ApplicationId.
     */
    OfferResponse getOfferByApplicationId(Integer applicationId);

    /**
     * Lấy danh sách ứng viên đã vượt qua phỏng vấn (Passed) chờ tạo Offer Proposal.
     */
    List<PassedCandidateResponse> getPassedCandidatesForOffer();

    /**
     * HR tạo mới Offer Proposal (lưu Draft hoặc trình Director).
     */
    OfferResponse createOfferByHr(CreateOfferRequest dto);

    /**
     * Lấy danh sách Offer Proposal cho HR có lọc theo status và phân trang.
     */
    Page<OfferResponse> getAllOffersForHr(String status, Pageable pageable);

    /**
     * Lấy danh sách Offer Proposal cho HR có lọc theo 3 tiêu chí: tìm kiếm từ khóa, trạng thái, sắp xếp thời gian trên toàn bộ dữ liệu.
     */
    Page<OfferResponse> getAllOffersForHr(String search, String status, String timeSort, Pageable pageable);

    /**
     * Lấy thống kê số lượng Offer theo các nhóm trạng thái chính trên toàn bộ hệ thống.
     */
    java.util.Map<String, Long> getOfferStats();

    /**
     * Lấy thông tin chi tiết gói Offer (Group A + B + History).
     */
    OfferDetailResponse getOfferDetailForHr(Integer id);

    /**
     * HR cập nhật Offer Proposal (chỉ khi Draft hoặc Rejected).
     */
    OfferResponse updateOfferByHr(Integer id, UpdateOfferRequest dto);

    /**
     * HR xóa bản thảo Offer (chỉ khi Draft).
     */
    void deleteDraftOfferByHr(Integer id);

    /**
     * Lấy thông tin DTO phục vụ cho form chỉnh sửa Offer Proposal (Screen 32 Update).
     */
    UpdateOfferRequest getUpdateOfferRequestById(Integer id);

    /**
     * HR gửi Offer Letter chính thức cho ứng viên sau khi Director Approved.
     */
    OfferResponse sendOfferToCandidate(Integer id);
}
