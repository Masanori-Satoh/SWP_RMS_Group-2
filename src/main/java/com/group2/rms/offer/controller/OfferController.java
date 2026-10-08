package com.group2.rms.offer.controller;

import com.group2.rms.core.dto.ApiResponse;
import com.group2.rms.core.exception.BaseBusinessException;
import com.group2.rms.offer.dto.CreateOfferRequest;
import com.group2.rms.offer.dto.DirectorDecisionRequest;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Controller MVC cho phân hệ Offer Proposal tuân thủ ARCHITECTURE_GUIDE.md ({Feature}Controller).
 * Điều hướng và xử lý toàn bộ các màn hình Screen 30 (List & Create) và Screen 32 (Detail & Update).
 */
@Controller
@RequestMapping("/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;
    private final com.group2.rms.offer.service.OfferExportService offerExportService;

    @ModelAttribute
    public void populateCommonAttributes(Model model, Principal principal) {
        String userRole = "HR";
        if (principal instanceof org.springframework.security.core.Authentication auth) {
            if (auth.getAuthorities().stream().anyMatch(
                    a -> com.group2.rms.core.security.RoleAuthorities.SYSTEM_ADMIN.equals(a.getAuthority()))) {
                userRole = "System Admin";
            } else if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_DIRECTOR".equals(a.getAuthority()))) {
                userRole = "Director";
            } else {
                userRole = "HR";
            }
        }
        model.addAttribute("currentUser", principal != null ? principal.getName() : "HR");
        model.addAttribute("userRole", userRole);
    }

    /**
     * 1. VIEW LIST: Danh sách Offer có phân trang và lọc (Screen 30)
     */
    @GetMapping
    public String listOffers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String timeSort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        int validPage = Math.max(0, page);
        Page<OfferResponse> pagedData = offerService.getAllOffersForHr(search, status, timeSort, PageRequest.of(validPage, size));
        List<PassedCandidateResponse> passedCandidates = offerService.getPassedCandidatesForOffer();

        Map<String, Long> stats = offerService.getOfferStats();
        model.addAttribute("statPending", stats.getOrDefault("statPending", 0L));
        model.addAttribute("statApproved", stats.getOrDefault("statApproved", 0L));
        model.addAttribute("statSent", stats.getOrDefault("statSent", 0L));
        model.addAttribute("statAccepted", stats.getOrDefault("statAccepted", 0L));

        model.addAttribute("offers", pagedData.getContent());
        model.addAttribute("offersPage", pagedData);
        model.addAttribute("currentPage", validPage);
        model.addAttribute("totalPages", Math.max(1, pagedData.getTotalPages()));
        model.addAttribute("totalElements", pagedData.getTotalElements());
        model.addAttribute("search", search != null ? search.trim() : "");
        model.addAttribute("currentStatus", status != null && !status.isBlank() ? status.trim() : "ALL");
        model.addAttribute("currentTimeSort", timeSort != null && !timeSort.isBlank() ? timeSort.trim() : "DEFAULT");
        model.addAttribute("passedCandidates", passedCandidates);

        return "offers/list";
    }

    /**
     * 2. CREATE FORM: Màn hình tạo mới Offer Proposal (Screen 30 Create)
     */
    @GetMapping("/create")
    public String createOfferForm(@RequestParam(required = false) Integer applicationId, Model model) {
        List<PassedCandidateResponse> passedCandidates = offerService.getPassedCandidatesForOffer();
        model.addAttribute("passedCandidates", passedCandidates);

        CreateOfferRequest createRequest = new CreateOfferRequest();
        createRequest.setProbationDays(60);
        if (applicationId != null) {
            createRequest.setApplicationId(applicationId);
            passedCandidates.stream()
                    .filter(c -> applicationId.equals(c.getApplicationId()))
                    .findFirst()
                    .ifPresent(c -> {
                        createRequest.setOfferedPositionTitle(c.getAppliedPosition());
                        createRequest.setWorkLocation(c.getWorkLocation() != null ? c.getWorkLocation() : "Trụ sở chính Mộc RMS");
                        if (c.getRecommendedSalary() != null) {
                            createRequest.setProposedSalary(c.getRecommendedSalary());
                            createRequest.setProbationSalary(c.getRecommendedSalary().multiply(new BigDecimal("0.85")).setScale(0, RoundingMode.HALF_UP));
                        }
                    });
        }

        model.addAttribute("offerDto", createRequest);
        model.addAttribute("isEdit", false);
        return "offers/form";
    }

    /**
     * 3a. SUBMIT CREATE VIA AJAX (JSON): Tiếp nhận request từ pop-up Modal trên trang danh sách
     */
    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<OfferResponse>> createOfferJson(
            @Valid @RequestBody CreateOfferRequest createRequest) {
        try {
            OfferResponse created = offerService.createOfferByHr(createRequest);
            boolean isDraft = Boolean.TRUE.equals(createRequest.getIsDraft());
            String msg = isDraft
                    ? "Lưu bản thảo Offer (Draft) thành công!"
                    : "Đã nộp trình đề xuất Offer lên Giám đốc (Pending_Director) thành công!";
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, msg, created));
        } catch (BaseBusinessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * 3b. SUBMIT CREATE: Tiếp nhận form tạo mới Offer (Save Draft hoặc Submit Director)
     */
    @PostMapping("/create")
    public String createOffer(
            @Valid @ModelAttribute("offerDto") CreateOfferRequest createRequest,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {

        if (errors.hasErrors()) {
            model.addAttribute("passedCandidates", offerService.getPassedCandidatesForOffer());
            model.addAttribute("isEdit", false);
            return "offers/form";
        }

        try {
            OfferResponse created = offerService.createOfferByHr(createRequest);
            boolean isDraft = Boolean.TRUE.equals(createRequest.getIsDraft());
            flash.addFlashAttribute("successMessage", isDraft
                    ? "Lưu bản thảo Offer (Draft) thành công!"
                    : "Đã nộp trình đề xuất Offer lên Giám đốc (Pending_Director) thành công!");
            return "redirect:/offers/" + created.getOfferId();
        } catch (BaseBusinessException e) {
            errors.reject("businessError", e.getMessage());
            model.addAttribute("passedCandidates", offerService.getPassedCandidatesForOffer());
            model.addAttribute("isEdit", false);
            return "offers/form";
        }
    }

    /**
     * 4a. VIEW DETAIL VIA AJAX (JSON): Trả về chi tiết Offer cho pop-up Modal trên trang danh sách
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<OfferDetailResponse>> getOfferDetailJson(@PathVariable Integer id) {
        OfferDetailResponse detail = offerService.getOfferDetailForHr(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy chi tiết Offer thành công.", detail));
    }

    /**
     * 4b. VIEW DETAIL: Xem chi tiết 1 Offer + Thông tin ứng viên (Screen 32)
     */
    @GetMapping("/{id}")
    public String viewOfferDetail(@PathVariable Integer id, Model model) {
        OfferDetailResponse detail = offerService.getOfferDetailForHr(id);
        model.addAttribute("detail", detail);
        return "offers/detail";
    }

    /**
     * 5. EDIT FORM: Màn hình chỉnh sửa Offer (Chỉ cho phép khi Draft hoặc Rejected)
     */
    @GetMapping("/{id}/edit")
    public String editOfferForm(@PathVariable Integer id, Model model, RedirectAttributes flash) {
        try {
            UpdateOfferRequest updateRequest = offerService.getUpdateOfferRequestById(id);
            OfferDetailResponse detail = offerService.getOfferDetailForHr(id);
            model.addAttribute("offerDto", updateRequest);
            model.addAttribute("offerId", id);
            model.addAttribute("detail", detail);
            model.addAttribute("isEdit", true);
            return "offers/form";
        } catch (BaseBusinessException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/offers/" + id;
        }
    }

    /**
     * 6a. SUBMIT EDIT VIA AJAX (JSON): Cập nhật Offer Proposal từ pop-up Modal
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<OfferResponse>> updateOfferJson(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateOfferRequest updateRequest) {
        try {
            OfferResponse updated = offerService.updateOfferByHr(id, updateRequest);
            boolean isDraft = Boolean.TRUE.equals(updateRequest.getIsDraft());
            String msg = isDraft
                    ? "Cập nhật bản thảo Offer thành công!"
                    : "Đã nộp trình đề xuất Offer lên Giám đốc thành công!";
            return ResponseEntity.ok(new ApiResponse<>(true, msg, updated));
        } catch (BaseBusinessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @PostMapping(value = "/{id}/edit", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<OfferResponse>> updateOfferJsonPost(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateOfferRequest updateRequest) {
        return updateOfferJson(id, updateRequest);
    }

    /**
     * 6b. SUBMIT EDIT: Cập nhật Offer Proposal (Form Submit Fallback)
     */
    @PostMapping("/{id}/edit")
    public String updateOffer(
            @PathVariable Integer id,
            @Valid @ModelAttribute("offerDto") UpdateOfferRequest updateRequest,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {

        if (errors.hasErrors()) {
            OfferDetailResponse detail = offerService.getOfferDetailForHr(id);
            model.addAttribute("offerId", id);
            model.addAttribute("detail", detail);
            model.addAttribute("isEdit", true);
            return "offers/form";
        }

        try {
            offerService.updateOfferByHr(id, updateRequest);
            boolean isDraft = Boolean.TRUE.equals(updateRequest.getIsDraft());
            flash.addFlashAttribute("successMessage", isDraft
                    ? "Cập nhật bản thảo Offer thành công!"
                    : "Đã nộp trình đề xuất Offer lên Giám đốc thành công!");
            return "redirect:/offers/" + id;
        } catch (BaseBusinessException e) {
            errors.reject("businessError", e.getMessage());
            OfferDetailResponse detail = offerService.getOfferDetailForHr(id);
            model.addAttribute("offerId", id);
            model.addAttribute("detail", detail);
            model.addAttribute("isEdit", true);
            return "offers/form";
        }
    }

    /**
     * 7a. DELETE VIA AJAX (JSON): Xóa bản thảo Offer từ pop-up Modal
     */
    @DeleteMapping(value = "/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteOfferJson(@PathVariable Integer id) {
        try {
            offerService.deleteDraftOfferByHr(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Đã xóa bản thảo Offer thành công."));
        } catch (BaseBusinessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * 7b. DELETE: Xóa bản thảo Offer (Chỉ cho phép khi Draft)
     */
    @PostMapping("/{id}/delete")
    public String deleteOffer(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            offerService.deleteDraftOfferByHr(id);
            flash.addFlashAttribute("successMessage", "Đã xóa bản thảo Offer thành công.");
        } catch (BaseBusinessException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/offers";
    }

    /**
     * 8a. SEND OFFER VIA AJAX (JSON): Phát hành Offer Letter tới ứng viên từ pop-up Modal
     */
    @PostMapping(value = "/{id}/send", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<OfferResponse>> sendOfferToCandidateJson(@PathVariable Integer id) {
        try {
            OfferResponse sentOffer = offerService.sendOfferToCandidate(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Đã phát hành và gửi thư mời nhận việc (Offer Letter) tới ứng viên thành công!", sentOffer));
        } catch (BaseBusinessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * 8b. SEND OFFER: HR phát hành thư mời làm việc cho Candidate (Khi Status == Director_Approved)
     */
    @PostMapping("/{id}/send")
    public String sendOfferToCandidate(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            offerService.sendOfferToCandidate(id);
            flash.addFlashAttribute("successMessage", "Đã phát hành và gửi thư mời nhận việc (Offer Letter) tới ứng viên thành công!");
        } catch (BaseBusinessException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/offers/" + id;
    }

    /**
     * 9a. DIRECTOR APPROVE VIA AJAX (JSON): Director duyệt Offer Proposal
     */
    @PostMapping(value = "/{id}/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<OfferResponse>> approveOfferJson(
            @PathVariable Integer id,
            @Valid @RequestBody DirectorDecisionRequest request) {
        try {
            OfferResponse approved = offerService.approveOfferByDirector(id, request.getComments());
            return ResponseEntity.ok(new ApiResponse<>(true, "Đã phê duyệt đề xuất Offer thành công!", approved));
        } catch (BaseBusinessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * 9b. DIRECTOR REJECT VIA AJAX (JSON): Director từ chối Offer Proposal
     */
    @PostMapping(value = "/{id}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<OfferResponse>> rejectOfferJson(
            @PathVariable Integer id,
            @Valid @RequestBody DirectorDecisionRequest request) {
        try {
            OfferResponse rejected = offerService.rejectOfferByDirector(id, request.getComments());
            return ResponseEntity.ok(new ApiResponse<>(true, "Đã từ chối đề xuất Offer thành công!", rejected));
        } catch (BaseBusinessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * 10. EXPORT EXCEL: Xuất dữ liệu Offer theo phạm vi và bộ cột được chọn (Screen 30)
     */
    @PostMapping("/export")
    public org.springframework.http.ResponseEntity<byte[]> exportOffers(
            @Valid @RequestBody com.group2.rms.offer.dto.OfferExportRequest request,
            Principal principal) {
        if (principal instanceof org.springframework.security.core.Authentication auth) {
            boolean isAuthorized = auth.getAuthorities().stream().anyMatch(a ->
                    "ROLE_HR".equals(a.getAuthority())
                            || "ROLE_DIRECTOR".equals(a.getAuthority())
                            || com.group2.rms.core.security.RoleAuthorities.SYSTEM_ADMIN.equals(a.getAuthority()));
            if (!isAuthorized) {
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }
        }
        String exportedBy = principal != null ? principal.getName() : "HR";
        byte[] excelBytes = offerExportService.exportOffersToExcel(request, exportedBy);
        String filename = offerExportService.generateExportFilename(request);

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excelBytes);
    }
}
