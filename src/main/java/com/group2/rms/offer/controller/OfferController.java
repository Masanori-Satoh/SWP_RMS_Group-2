package com.group2.rms.offer.controller;

import com.group2.rms.core.dto.ApiResponse;
import com.group2.rms.offer.dto.CreateOfferRequest;
import com.group2.rms.offer.dto.OfferDetailResponse;
import com.group2.rms.offer.dto.OfferResponse;
import com.group2.rms.offer.dto.PassedCandidateResponse;
import com.group2.rms.offer.dto.UpdateOfferRequest;
import com.group2.rms.offer.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller cho phân hệ Offer Proposal tuân thủ ARCHITECTURE_GUIDE.md ({Feature}Controller).
 * Phục vụ các màn hình Screen 30 (List & Create) và Screen 32 (Detail & Update).
 */
@RestController
@RequestMapping("/api/v1/hr/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    /**
     * 1. Lấy danh sách ứng viên đỗ phỏng vấn (FinalDecision = 'Passed') chờ tạo Offer
     */
    @GetMapping("/passed-candidates")
    public ResponseEntity<ApiResponse<List<PassedCandidateResponse>>> getPassedCandidates() {
        List<PassedCandidateResponse> passedCandidates = offerService.getPassedCandidatesForOffer();
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách ứng viên đỗ phỏng vấn thành công.", passedCandidates));
    }

    /**
     * 2. CREATE: Tạo Offer Proposal (Save Draft hoặc Submit Director)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OfferResponse>> createOffer(@Valid @RequestBody CreateOfferRequest dto) {
        OfferResponse created = offerService.createOfferByHr(dto);
        String msg = Boolean.TRUE.equals(dto.getIsDraft())
                ? "Lưu bản thảo Offer (Draft) thành công."
                : "Đã nộp trình Offer lên Director (Pending_Director) thành công.";
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, msg, created));
    }

    /**
     * 3. VIEW: Danh sách Offer có phân trang và lọc theo Status (Screen 30)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OfferResponse>>> getAllOffers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String timeSort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<OfferResponse> offers;
        if (search == null && timeSort == null) {
            offers = offerService.getAllOffersForHr(status, PageRequest.of(Math.max(0, page), Math.max(1, size)));
        } else {
            offers = offerService.getAllOffersForHr(search, status, timeSort, PageRequest.of(Math.max(0, page), Math.max(1, size)));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách Offer thành công.", offers));
    }

    /**
     * 4. VIEW DETAIL: Xem chi tiết 1 Offer + Thông tin ứng viên (Screen 32)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OfferDetailResponse>> getOfferDetail(@PathVariable Integer id) {
        OfferDetailResponse detail = offerService.getOfferDetailForHr(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy chi tiết Offer thành công.", detail));
    }

    /**
     * 5. UPDATE: Cập nhật Offer (Chỉ cho phép khi Draft hoặc Rejected / Director_Rejected)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OfferResponse>> updateOffer(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateOfferRequest dto) {
        OfferResponse updated = offerService.updateOfferByHr(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cập nhật thông tin Offer thành công.", updated));
    }

    /**
     * 6. DELETE: Xóa Offer (Chỉ cho phép khi Draft)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOffer(@PathVariable Integer id) {
        offerService.deleteDraftOfferByHr(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Đã xóa bản thảo Offer thành công."));
    }

    /**
     * 7. SEND OFFER: HR phát hành thư mời làm việc cho Candidate (Khi Status == Approved / Director_Approved)
     */
    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse<OfferResponse>> sendOfferToCandidate(@PathVariable Integer id) {
        OfferResponse sentOffer = offerService.sendOfferToCandidate(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Đã phát hành và gửi Offer Letter tới ứng viên thành công.", sentOffer));
    }
}
