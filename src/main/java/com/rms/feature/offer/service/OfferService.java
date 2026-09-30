package com.rms.feature.offer.service;

import com.rms.feature.offer.dto.CandidateResponseDTO;
import com.rms.feature.offer.dto.CreateOfferRequestDTO;
import com.rms.feature.offer.dto.DirectorApprovalDTO;
import com.rms.feature.offer.dto.OfferResponseDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface cho Service quản lý Offer Approval & Candidate Response.
 */
public interface OfferService {

    /**
     * Tạo mới một OfferProposal cho ứng viên.
     * Enforces BR-OFF-01 (Probation Salary >= 85%) & GBR-14 (Single active offer proposal per application).
     */
    OfferResponseDTO createOfferProposal(CreateOfferRequestDTO dto);

    /**
     * Xử lý phê duyệt / từ chối OfferProposal từ Director.
     */
    OfferResponseDTO processDirectorApproval(Long offerId, DirectorApprovalDTO dto);

    /**
     * Xử lý phản hồi từ phía Candidate (Accept / Decline / Negotiate).
     * Trạng thái "Accept" kích hoạt BR-OFF-02 chuyển trạng thái Application thành "Hired".
     */
    OfferResponseDTO processCandidateResponse(Long offerId, CandidateResponseDTO dto);

    /**
     * Lấy chi tiết OfferProposal kèm lịch sử phê duyệt và lịch sử thương lượng.
     */
    OfferResponseDTO getOfferById(Long offerId);

    /**
     * Tìm kiếm và phân trang danh sách OfferProposal cho nội bộ.
     */
    Page<OfferResponseDTO> getOffers(String status, String candidateName, Pageable pageable);

    /**
     * Cập nhật đề xuất Offer (Update Offer Proposal) - Dành cho HM khi Offer ở trạng thái Draft hoặc Rejected.
     */
    OfferResponseDTO updateOfferProposal(Long offerId, com.rms.feature.offer.dto.UpdateOfferRequestDTO dto);

    /**
     * Xóa bản thảo Offer (Delete Offer Proposal) - Dành cho HM khi Offer ở trạng thái Draft hoặc Voided.
     */
    void deleteOfferProposal(Long offerId);

    /**
     * Trình duyệt Director (Submit to Director) - Chuyển từ Draft sang Pending_Director_Approval.
     */
    OfferResponseDTO submitToDirector(Long offerId);

    /**
     * Xác nhận Tuyển dụng chính thức (Confirm Hiring) - Chuyển ứng viên thành Hired khi Offer đã Accepted.
     */
    OfferResponseDTO confirmHiring(Long offerId);

    /**
     * Lấy danh sách các trạng thái thực tế đang có trong bảng OfferProposal của Database.
     */
    java.util.List<String> getOfferStatuses();

    /**
     * Phát hành và gửi Offer tới ứng viên (dành cho HR sau khi Director duyệt).
     * Yêu cầu Offer đang ở trạng thái "Approved". Cập nhật thành "Sent_Candidate".
     */
    OfferResponseDTO sendOfferToCandidate(Long offerId, Long hrUserId);

    /**
     * HR phản hồi yêu cầu thương lượng của ứng viên.
     */
    OfferResponseDTO respondToCandidateNegotiation(Long offerId, Long negotiationId, com.rms.feature.offer.dto.HrNegotiationResponseDTO dto);
}
