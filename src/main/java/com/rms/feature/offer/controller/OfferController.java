package com.rms.feature.offer.controller;

import com.rms.feature.offer.dto.ApiResponseDTO;
import com.rms.feature.offer.dto.CandidateResponseDTO;
import com.rms.feature.offer.dto.CreateOfferRequestDTO;
import com.rms.feature.offer.dto.DirectorApprovalDTO;
import com.rms.feature.offer.dto.OfferResponseDTO;
import com.rms.feature.offer.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller cho module Offer Approval & Candidate Response.
 */
@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
@Tag(name = "Offer Management", description = "Quản lý Offer Proposal, Phê duyệt của Director và Phản hồi từ Ứng viên")
public class OfferController {

    private final OfferService offerService;

    /**
     * Tạo mới Offer Proposal.
     * Chỉ cho phép Hiring Manager hoặc Admin.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('HiringManager', 'Admin')")
    @Operation(summary = "Tạo mới Offer Proposal", description = "Hiring Manager hoặc Admin tạo đề xuất offer cho ứng viên. Hệ thống tự động kiểm tra BR-OFF-01 và GBR-14.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tạo Offer thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc vi phạm quy tắc lương thử việc (BR-OFF-01)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy Application hoặc User đề xuất"),
            @ApiResponse(responseCode = "409", description = "Ứng viên đã có OfferProposal đang active (GBR-14)")
    })
    public ResponseEntity<ApiResponseDTO<OfferResponseDTO>> createOfferProposal(
            @Valid @RequestBody CreateOfferRequestDTO dto) {
        OfferResponseDTO response = offerService.createOfferProposal(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(response, "Tạo Offer Proposal thành công"));
    }

    /**
     * Phê duyệt hoặc từ chối Offer từ phía Director.
     * Chỉ cho phép Director.
     */
    @PutMapping("/{offerId}/approval")
    @PreAuthorize("hasRole('Director')")
    @Operation(summary = "Director phê duyệt / từ chối Offer", description = "Director thực hiện phê duyệt ('Approved') hoặc từ chối ('Rejected') một Offer Proposal.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xử lý phê duyệt thành công"),
            @ApiResponse(responseCode = "400", description = "Trạng thái phê duyệt không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy Offer hoặc Director")
    })
    public ResponseEntity<ApiResponseDTO<OfferResponseDTO>> processDirectorApproval(
            @PathVariable("offerId") Long offerId,
            @Valid @RequestBody DirectorApprovalDTO dto) {
        OfferResponseDTO response = offerService.processDirectorApproval(offerId, dto);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Xử lý phê duyệt Offer thành công"));
    }

    /**
     * Xử lý phản hồi từ phía ứng viên (Accept / Decline / Negotiate).
     * Chỉ cho phép Candidate.
     */
    @PutMapping("/{offerId}/candidate-response")
    @PreAuthorize("hasRole('Candidate')")
    @Operation(summary = "Ứng viên phản hồi Offer", description = "Ứng viên gửi phản hồi (Accept, Decline, Negotiate). Phản hồi 'Accept' kích hoạt BR-OFF-02 chuyển trạng thái ứng viên sang Hired.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xử lý phản hồi từ ứng viên thành công"),
            @ApiResponse(responseCode = "400", description = "Phản hồi không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy Offer"),
            @ApiResponse(responseCode = "409", description = "Trạng thái Offer không hợp lệ để phản hồi")
    })
    public ResponseEntity<ApiResponseDTO<OfferResponseDTO>> processCandidateResponse(
            @PathVariable("offerId") Long offerId,
            @Valid @RequestBody CandidateResponseDTO dto) {
        OfferResponseDTO response = offerService.processCandidateResponse(offerId, dto);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Gửi phản hồi Offer thành công"));
    }

    /**
     * Xem thông tin chi tiết Offer Proposal.
     * Cho phép các roles liên quan truy cập.
     */
    @GetMapping("/{offerId}")
    @PreAuthorize("hasAnyRole('HiringManager', 'Director', 'HR_Specialist', 'Candidate', 'Admin')")
    @Operation(summary = "Xem chi tiết Offer Proposal", description = "Lấy thông tin chi tiết Offer kèm toàn bộ lịch sử phê duyệt và thương lượng.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy dữ liệu thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy Offer")
    })
    public ResponseEntity<ApiResponseDTO<OfferResponseDTO>> getOfferById(
            @PathVariable("offerId") Long offerId) {
        OfferResponseDTO response = offerService.getOfferById(offerId);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Lấy thông tin Offer thành công"));
    }

    /**
     * Lấy danh sách Offer Proposal (tìm kiếm và phân trang).
     * Cho phép các roles nội bộ truy cập.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('HiringManager', 'Director', 'HR_Specialist', 'Admin')")
    @Operation(summary = "Lấy danh sách Offer Proposal", description = "Tìm kiếm và phân trang danh sách Offer cho nội bộ.")
    public ResponseEntity<ApiResponseDTO<Page<OfferResponseDTO>>> getOffers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String candidateName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OfferResponseDTO> response = offerService.getOffers(status, candidateName, pageable);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Lấy danh sách Offer thành công"));
    }

    /**
     * Cập nhật đề xuất Offer (dành cho HM khi Draft hoặc Rejected).
     */
    @PutMapping("/{offerId}")
    @PreAuthorize("hasAnyRole('HiringManager', 'Admin')")
    @Operation(summary = "Cập nhật đề xuất Offer", description = "Hiring Manager cập nhật thông tin gói lương và vị trí cho Offer ở trạng thái Draft hoặc Rejected.")
    public ResponseEntity<ApiResponseDTO<OfferResponseDTO>> updateOfferProposal(
            @PathVariable("offerId") Long offerId,
            @Valid @RequestBody com.rms.feature.offer.dto.UpdateOfferRequestDTO dto) {
        OfferResponseDTO response = offerService.updateOfferProposal(offerId, dto);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Cập nhật đề xuất Offer thành công"));
    }

    /**
     * Xóa bản thảo Offer (chỉ cho phép khi Draft hoặc Voided).
     */
    @DeleteMapping("/{offerId}")
    @PreAuthorize("hasAnyRole('HiringManager', 'Admin')")
    @Operation(summary = "Xóa bản thảo Offer", description = "Hiring Manager xóa bản thảo Offer chưa gửi duyệt.")
    public ResponseEntity<ApiResponseDTO<Void>> deleteOfferProposal(
            @PathVariable("offerId") Long offerId) {
        offerService.deleteOfferProposal(offerId);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Xóa bản thảo Offer thành công"));
    }

    /**
     * Trình duyệt Director (Submit to Director).
     */
    @PutMapping("/{offerId}/submit")
    @PreAuthorize("hasAnyRole('HiringManager', 'Admin')")
    @Operation(summary = "Trình duyệt Director", description = "Hiring Manager gửi gói Offer hoàn chỉnh lên Giám đốc (Director) phê duyệt.")
    public ResponseEntity<ApiResponseDTO<OfferResponseDTO>> submitToDirector(
            @PathVariable("offerId") Long offerId) {
        OfferResponseDTO response = offerService.submitToDirector(offerId);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Đã gửi đề xuất Offer tới Director phê duyệt"));
    }

    /**
     * Xác nhận Tuyển dụng chính thức (Confirm Hiring).
     */
    @PutMapping("/{offerId}/confirm-hiring")
    @PreAuthorize("hasAnyRole('HiringManager', 'HR_Specialist', 'Admin')")
    @Operation(summary = "Xác nhận Tuyển dụng", description = "Xác nhận tuyển dụng chính thức khi ứng viên đã chấp nhận Offer (Accepted), chuyển trạng thái ứng viên thành Hired.")
    public ResponseEntity<ApiResponseDTO<OfferResponseDTO>> confirmHiring(
            @PathVariable("offerId") Long offerId) {
        OfferResponseDTO response = offerService.confirmHiring(offerId);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Đã xác nhận Tuyển dụng thành công! Ứng viên chính thức chuyển sang trạng thái Hired."));
    }

    /**
     * Lấy danh sách các trạng thái Offer thực tế có trong Database (bảng OfferProposal).
     */
    @GetMapping("/statuses")
    @Operation(summary = "Lấy danh sách trạng thái Offer thực tế trong Database", description = "Truy vấn DISTINCT OfferStatus từ bảng OfferProposal trong cơ sở dữ liệu.")
    public ResponseEntity<ApiResponseDTO<java.util.List<String>>> getOfferStatuses() {
        java.util.List<String> statuses = offerService.getOfferStatuses();
        return ResponseEntity.ok(ApiResponseDTO.success(statuses, "Lấy danh sách trạng thái thành công"));
    }

    /**
     * Phát hành và gửi Offer tới ứng viên (CHỈ dành cho HR sau khi Director duyệt).
     */
    @RequestMapping(value = {"/{offerId}/send-to-candidate", "/{offerId}/send"}, method = {RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasAnyRole('HR_Specialist', 'Admin')")
    @Operation(summary = "Phát hành Offer tới ứng viên", description = "Chỉ dành cho HR: Chuyển trạng thái từ Approved sang Sent_Candidate và gửi email thư mời nhận việc tới ứng viên.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Phát hành Offer thành công"),
            @ApiResponse(responseCode = "400", description = "Offer chưa được Director phê duyệt"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy Offer")
    })
    public ResponseEntity<ApiResponseDTO<OfferResponseDTO>> sendOfferToCandidate(
            @PathVariable("offerId") Long offerId,
            @RequestParam(value = "hrUserId", defaultValue = "3") Long hrUserId) {
        OfferResponseDTO response = offerService.sendOfferToCandidate(offerId, hrUserId);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Đã phát hành và gửi Offer thành công tới ứng viên"));
    }

    /**
     * HR phản hồi yêu cầu thương lượng của ứng viên.
     */
    @RequestMapping(value = {"/{offerId}/negotiations/{negotiationId}/response", "/{offerId}/negotiations/{negotiationId}/hr-response"}, method = {RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasAnyRole('HR_Specialist', 'Admin')")
    @Operation(summary = "HR phản hồi thương lượng", description = "HR gửi phản hồi chính thức cho yêu cầu thương lượng mức lương/điều khoản từ ứng viên.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Phản hồi thương lượng thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu phản hồi không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy Offer hoặc bản ghi thương lượng")
    })
    public ResponseEntity<ApiResponseDTO<OfferResponseDTO>> respondToCandidateNegotiation(
            @PathVariable("offerId") Long offerId,
            @PathVariable("negotiationId") Long negotiationId,
            @Valid @RequestBody com.rms.feature.offer.dto.HrNegotiationResponseDTO dto) {
        OfferResponseDTO response = offerService.respondToCandidateNegotiation(offerId, negotiationId, dto);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Đã gửi phản hồi thương lượng thành công"));
    }
}
