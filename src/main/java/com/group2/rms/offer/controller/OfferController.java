package com.group2.rms.offer.controller;

import com.group2.rms.core.dto.ApiResponse;
import com.group2.rms.core.exception.BaseBusinessException;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.offer.dto.CreateOfferRequest;
import com.group2.rms.offer.dto.DirectorDecisionRequest;
import com.group2.rms.offer.dto.OfferDetailResponse;
import com.group2.rms.offer.dto.OfferResponse;
import com.group2.rms.offer.dto.PassedCandidateResponse;
import com.group2.rms.offer.dto.UpdateOfferRequest;
import com.group2.rms.offer.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Controller MVC cho phân hệ Offer Proposal tuân thủ ARCHITECTURE_GUIDE.md
 * ({Feature}Controller).
 * Điều hướng và xử lý toàn bộ các màn hình Screen 30 (List & Create) và Screen
 * 32 (Detail & Update).
 */
@Slf4j
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
            if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_DIRECTOR".equals(a.getAuthority()))) {
                userRole = "Director";
            } else if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_HR".equals(a.getAuthority()))) {
                userRole = "HR";
            } else {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Bạn không có quyền truy cập vào phân hệ Quản lý thư mời nhận việc.");
            }
        }
        model.addAttribute("currentUser", principal != null ? principal.getName() : "HR");
        model.addAttribute("userRole", userRole);
        model.addAttribute("activeMenu", "offers");
    }

    /**
     * Điểm vào GET /offers: phân nhánh giao diện HR/Giám đốc, chuẩn hóa tab rồi gọi service đọc dữ liệu.
     * Controller chuẩn bị model; quyền theo URL do SecurityConfig quản lý, không dựa vào việc nút có hiện trên UI hay không.
     */
    @GetMapping
    public String listOffers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String timeSort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @ModelAttribute("userRole") String userRole,
            Model model) {

        int validPage = Math.max(0, page);
        if ("Director".equals(userRole)) {
            Map<String, Long> statusCounts = offerService.getDirectorStatusCounts();
            Map<String, Long> directorStats = offerService.getDirectorOfferStats();

            String currentTab;
            String currentStatus;

            if ("HISTORY".equalsIgnoreCase(status)) {
                currentTab = "HISTORY";
                currentStatus = "HISTORY";
            } else if ("PENDING".equalsIgnoreCase(status)) {
                currentTab = "PENDING";
                currentStatus = "Pending_Director";
            } else if (status != null && !status.isBlank()) {
                currentStatus = status.trim();
                currentTab = "Pending_Director".equalsIgnoreCase(currentStatus) ? "PENDING" : "HISTORY";
            } else {
                currentTab = "PENDING";
                currentStatus = "Pending_Director";
            }

            Page<OfferResponse> directorOffers = offerService.getDirectorOffers(search, currentStatus, timeSort,
                    PageRequest.of(validPage, size));

            model.addAttribute("offersPage", directorOffers);
            model.addAttribute("offers", directorOffers.getContent());
            model.addAttribute("totalPages", Math.max(1, directorOffers.getTotalPages()));
            model.addAttribute("totalElements", directorOffers.getTotalElements());

            model.addAttribute("statusCounts", statusCounts);
            model.addAttribute("statPending", directorStats.getOrDefault("statPending", 0L));
            model.addAttribute("statApproved", directorStats.getOrDefault("statApproved", 0L));
            model.addAttribute("statRejected", directorStats.getOrDefault("statRejected", 0L));
            model.addAttribute("currentTab", currentTab);
            model.addAttribute("currentStatus", currentStatus);
            model.addAttribute("currentPage", validPage);
            model.addAttribute("search", search != null ? search.trim() : "");
            model.addAttribute("currentTimeSort",
                    timeSort != null && !timeSort.isBlank() ? timeSort.trim() : "DEFAULT");
            model.addAttribute("activeMenu", "offers");

            return "offers/director-list";
        }

        Page<OfferResponse> pagedData = offerService.getAllOffersForHr(search, status, timeSort,
                PageRequest.of(validPage, size));
        model.addAttribute("activeMenu", "offers");

        Map<String, Long> stats = offerService.getOfferStats();
        model.addAttribute("statTotal", stats.getOrDefault("statTotal", 0L));
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
        model.addAttribute("passedCandidates", List.of());

        return "offers/list";
    }

    /**
     * 2. CREATE FORM: Màn hình tạo mới Offer Proposal (Screen 30 Create)
     */
    @GetMapping("/create")
    public String createOfferForm(@RequestParam(required = false) Integer applicationId, Model model) {
        model.addAttribute("activeMenu", "offers");
        List<PassedCandidateResponse> passedCandidates = offerService.getPassedCandidatesForOffer();
        model.addAttribute("passedCandidates", passedCandidates);

        CreateOfferRequest createRequest = new CreateOfferRequest();
        createRequest.setProbationDays(60);
        PassedCandidateResponse fixedCandidate = null;
        boolean isLockedCandidate = false;

        if (applicationId != null) {
            var found = passedCandidates.stream()
                    .filter(c -> applicationId.equals(c.getApplicationId()))
                    .findFirst();
            if (found.isPresent()) {
                fixedCandidate = found.get();
                isLockedCandidate = true;
                createRequest.setApplicationId(applicationId);
                createRequest.setOfferedPositionTitle(fixedCandidate.getAppliedPosition());
                createRequest.setWorkLocation(
                        fixedCandidate.getWorkLocation() != null ? fixedCandidate.getWorkLocation() : "Trụ sở chính Mộc RMS");
                if (fixedCandidate.getRecommendedSalary() != null) {
                    createRequest.setProposedSalary(fixedCandidate.getRecommendedSalary().setScale(0, RoundingMode.HALF_UP));
                    createRequest.setProbationSalary(fixedCandidate.getRecommendedSalary().multiply(new BigDecimal("0.85"))
                            .setScale(0, RoundingMode.HALF_UP));
                }
            }
        }

        model.addAttribute("offerDto", createRequest);
        model.addAttribute("isEdit", false);
        model.addAttribute("isLockedCandidate", isLockedCandidate);
        model.addAttribute("fixedCandidate", fixedCandidate);
        return "offers/form";
    }


    /**
     * 3b. SUBMIT CREATE: Tiếp nhận form tạo mới Offer (Save Draft hoặc Submit
     * Director)
     */
    @PostMapping(value = {"/create", "/{id}/edit"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> rejectLegacyJsonForms() {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ApiResponse<>(false, "Vui lòng sử dụng biểu mẫu tạo hoặc chỉnh sửa đề xuất thư mời."));
    }

    /**
     * Nhận form tạo đề xuất: Bean Validation trước, sau đó service kiểm tra nghiệp vụ và lưu.
     * Có lỗi thì dựng lại đúng form cùng dữ liệu đã nhập; thành công mới redirect kèm flash message.
     */
    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String createOffer(
            @Valid @ModelAttribute("offerDto") CreateOfferRequest createRequest,
            BindingResult errors,
            @RequestParam(required = false, defaultValue = "false") Boolean isLockedCandidate,
            Model model,
            RedirectAttributes flash) {

        if (errors.hasErrors()) {
            model.addAttribute("activeMenu", "offers");
        List<PassedCandidateResponse> passedCandidates = offerService.getPassedCandidatesForOffer();
            model.addAttribute("passedCandidates", passedCandidates);
            model.addAttribute("isEdit", false);
            boolean locked = Boolean.TRUE.equals(isLockedCandidate);
            model.addAttribute("isLockedCandidate", locked);
            if (locked && createRequest.getApplicationId() != null) {
                passedCandidates.stream()
                        .filter(c -> createRequest.getApplicationId().equals(c.getApplicationId()))
                        .findFirst()
                        .ifPresent(c -> model.addAttribute("fixedCandidate", c));
            }
            return "offers/form";
        }

        try {
            OfferResponse created = offerService.createOfferByHr(createRequest);
            boolean isDraft = Boolean.TRUE.equals(createRequest.getIsDraft());
            flash.addFlashAttribute("successMessage", isDraft
                    ? "Lưu bản nháp đề xuất thư mời thành công!"
                    : "Đã gửi đề xuất thư mời để Giám đốc duyệt.");
            return "redirect:/offers";
        } catch (BaseBusinessException e) {
            errors.reject("businessError", e.getMessage());
            model.addAttribute("activeMenu", "offers");
            List<PassedCandidateResponse> passedCandidates = offerService.getPassedCandidatesForOffer();
            model.addAttribute("passedCandidates", passedCandidates);
            model.addAttribute("isEdit", false);
            boolean locked = Boolean.TRUE.equals(isLockedCandidate);
            model.addAttribute("isLockedCandidate", locked);
            if (locked && createRequest.getApplicationId() != null) {
                passedCandidates.stream()
                        .filter(c -> createRequest.getApplicationId().equals(c.getApplicationId()))
                        .findFirst()
                        .ifPresent(c -> model.addAttribute("fixedCandidate", c));
            }
            return "offers/form";
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi tạo đề xuất thư mời: {}", e.getMessage(), e);
            errors.reject("businessError", "Không thể lưu đề xuất thư mời: " + (e.getMessage() != null ? e.getMessage() : "Xung đột dữ liệu hệ thống."));
            model.addAttribute("activeMenu", "offers");
            List<PassedCandidateResponse> passedCandidates = offerService.getPassedCandidatesForOffer();
            model.addAttribute("passedCandidates", passedCandidates);
            model.addAttribute("isEdit", false);
            boolean locked = Boolean.TRUE.equals(isLockedCandidate);
            model.addAttribute("isLockedCandidate", locked);
            if (locked && createRequest.getApplicationId() != null) {
                passedCandidates.stream()
                        .filter(c -> createRequest.getApplicationId().equals(c.getApplicationId()))
                        .findFirst()
                        .ifPresent(c -> model.addAttribute("fixedCandidate", c));
            }
            return "offers/form";
        }
    }

    /**
     * 4a. VIEW DETAIL VIA AJAX (JSON): Trả về chi tiết Offer cho pop-up Modal trên
     * trang danh sách
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<OfferDetailResponse>> getOfferDetailJson(@PathVariable Integer id) {
        OfferDetailResponse detail = offerService.getOfferDetailForHr(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy chi tiết thư mời nhận việc thành công.", detail));
    }

    /**
     * 4b. VIEW DETAIL: Xem chi tiết 1 Offer + Thông tin ứng viên (Screen 32)
     */
    @GetMapping("/{id}")
    public String viewOfferDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("activeMenu", "offers");
        OfferDetailResponse detail = offerService.getOfferDetailForHr(id);
        model.addAttribute("detail", detail);
        return "offers/detail";
    }

    /**
     * 5. EDIT FORM: Màn hình chỉnh sửa Offer (Chỉ cho phép khi Draft)
     */
    @GetMapping("/{id}/edit")
    public String editOfferForm(@PathVariable Integer id, Model model, RedirectAttributes flash) {
        try {
            UpdateOfferRequest updateRequest = offerService.getUpdateOfferRequestById(id);
            OfferDetailResponse detail = offerService.getOfferDetailForHr(id);
            model.addAttribute("activeMenu", "offers");
            model.addAttribute("offerDto", updateRequest);
            model.addAttribute("offerId", id);
            model.addAttribute("detail", detail);
            model.addAttribute("isEdit", true);
            model.addAttribute("isLockedCandidate", false);
            return "offers/form";
        } catch (BaseBusinessException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/offers/" + id;
        }
    }

    /**
     * Nhận form chỉnh sửa; service kiểm tra Draft và khóa bản ghi, không tin trạng thái do trình duyệt gửi.
     * Validation lỗi trả về form; lưu thành công dùng redirect để tránh gửi lại form khi refresh.
     */
    @PostMapping(value = "/{id}/edit", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String updateOffer(
            @PathVariable Integer id,
            @Valid @ModelAttribute("offerDto") UpdateOfferRequest updateRequest,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {

        if (errors.hasErrors()) {
            OfferDetailResponse detail = offerService.getOfferDetailForHr(id);
            model.addAttribute("activeMenu", "offers");
            model.addAttribute("offerId", id);
            model.addAttribute("detail", detail);
            model.addAttribute("isEdit", true);
            model.addAttribute("isLockedCandidate", false);
            return "offers/form";
        }

        try {
            offerService.updateOfferByHr(id, updateRequest);
            boolean isDraft = Boolean.TRUE.equals(updateRequest.getIsDraft());
            flash.addFlashAttribute("successMessage", isDraft
                    ? "Cập nhật bản nháp đề xuất thư mời thành công!"
                    : "Đã gửi đề xuất thư mời để Giám đốc duyệt.");
            return "redirect:/offers/" + id;
        } catch (BaseBusinessException e) {
            errors.reject("businessError", e.getMessage());
            OfferDetailResponse detail = offerService.getOfferDetailForHr(id);
            model.addAttribute("activeMenu", "offers");
            model.addAttribute("offerId", id);
            model.addAttribute("detail", detail);
            model.addAttribute("isEdit", true);
            model.addAttribute("isLockedCandidate", false);
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
            return ResponseEntity.ok(new ApiResponse<>(true, "Đã xóa bản nháp đề xuất thư mời thành công."));
        } catch (BaseBusinessException e) {
            HttpStatus status = (e instanceof ResourceNotFoundException) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi xóa bản nháp Offer ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Đã xảy ra lỗi hệ thống khi xóa bản nháp đề xuất thư mời."));
        }
    }

    /**
     * 7b. DELETE: Xóa bản thảo Offer (Chỉ cho phép khi Draft)
     */
    @PostMapping("/{id}/delete")
    public String deleteOffer(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            offerService.deleteDraftOfferByHr(id);
            flash.addFlashAttribute("successMessage", "Đã xóa bản nháp đề xuất thư mời thành công.");
        } catch (BaseBusinessException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/offers";
    }

    /**
     * 8a. SEND OFFER VIA AJAX (JSON): Phát hành Offer Letter tới ứng viên từ pop-up
     * Modal
     */
    @PostMapping(value = "/{id}/send", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<OfferResponse>> sendOfferToCandidateJson(@PathVariable Integer id) {
        try {
            OfferResponse sentOffer = offerService.sendOfferToCandidate(id);
            return ResponseEntity.ok(new ApiResponse<>(true,
                    offerPublicationMessage(sentOffer), sentOffer));
        } catch (BaseBusinessException e) {
            HttpStatus status = (e instanceof ResourceNotFoundException) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi phát hành thư mời nhận việc ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Đã xảy ra lỗi hệ thống khi phát hành thư mời nhận việc."));
        }
    }

    /**
     * 8b. SEND OFFER: HR phát hành thư mời làm việc cho Candidate (Khi Status ==
     * Director_Approved)
     */
    @PostMapping("/{id}/send")
    public String sendOfferToCandidate(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            OfferResponse sentOffer = offerService.sendOfferToCandidate(id);
            String flashType = sentOffer.getEmailStatus() == OfferResponse.EmailStatus.SENT
                    ? "successMessage" : "offerWarningMessage";
            flash.addFlashAttribute(flashType, offerPublicationMessage(sentOffer));
        } catch (BaseBusinessException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/offers/" + id;
    }

    private String offerPublicationMessage(OfferResponse offer) {
        String published = "Đã phát hành thư mời nhận việc trên cổng ứng viên.";
        if (offer.getEmailStatus() == OfferResponse.EmailStatus.SENT) {
            return published + " Đã gửi email thông báo.";
        }
        if (offer.getEmailStatus() == OfferResponse.EmailStatus.TEST_RECIPIENT) {
            return published + " Email chỉ được gửi tới địa chỉ kiểm thử.";
        }
        return published + " Chưa gửi được email thông báo.";
    }

    /**
     * Nhận quyết định duyệt của Giám đốc qua JSON; service dùng cập nhật có điều kiện để chặn quyết định cũ.
     * Xung đột OFFER_STATUS_INVALID được trả HTTP 409 để người dùng tải lại dữ liệu.
     */
    @PostMapping(value = "/{id}/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<OfferResponse>> approveOfferJson(
            @PathVariable Integer id,
            @Valid @RequestBody DirectorDecisionRequest request) {
        try {
            OfferResponse approved = offerService.approveOfferByDirector(id, request.getComments());
            return ResponseEntity.ok(new ApiResponse<>(true, "Đã phê duyệt đề xuất thư mời thành công!", approved));
        } catch (BaseBusinessException e) {
            HttpStatus status = e instanceof ResourceNotFoundException ? HttpStatus.NOT_FOUND
                    : "OFFER_STATUS_INVALID".equals(e.getErrorCode()) ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi phê duyệt đề xuất thư mời ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Đã xảy ra lỗi hệ thống khi phê duyệt đề xuất thư mời."));
        }
    }

    /**
     * Từ chối đề xuất nội bộ: controller bắt buộc lý do trước khi gọi service lưu quyết định.
     * Không nhầm với việc ứng viên từ chối thư mời đã phát hành ở /portal/offers.
     */
    @PostMapping(value = "/{id}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<OfferResponse>> rejectOfferJson(
            @PathVariable Integer id,
            @Valid @RequestBody DirectorDecisionRequest request) {
        try {
            if (request == null || request.getComments() == null || request.getComments().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(false, "Vui lòng nhập lý do từ chối vào ô nhận xét trước khi tiếp tục."));
            }
            OfferResponse rejected = offerService.rejectOfferByDirector(id, request.getComments().trim());
            return ResponseEntity.ok(new ApiResponse<>(true, "Đã từ chối đề xuất thư mời thành công!", rejected));
        } catch (BaseBusinessException e) {
            HttpStatus status = e instanceof ResourceNotFoundException ? HttpStatus.NOT_FOUND
                    : "OFFER_STATUS_INVALID".equals(e.getErrorCode()) ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi từ chối đề xuất thư mời ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Đã xảy ra lỗi hệ thống khi từ chối đề xuất thư mời."));
        }
    }

    /**
     * 10. EXPORT EXCEL: Xuất dữ liệu Offer theo phạm vi và bộ cột được chọn (Screen
     * 30)
     */
    @PostMapping("/export")
    public org.springframework.http.ResponseEntity<byte[]> exportOffers(
            @Valid @RequestBody com.group2.rms.offer.dto.OfferExportRequest request,
            Principal principal) {
        if (principal instanceof org.springframework.security.core.Authentication auth) {
            boolean isAuthorized = auth.getAuthorities().stream().anyMatch(a -> "ROLE_HR".equals(a.getAuthority())
                    || "ROLE_DIRECTOR".equals(a.getAuthority()));
            if (!isAuthorized) {
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .build();
            }
        }
        String exportedBy = principal != null ? principal.getName() : "HR";
        byte[] excelBytes = offerExportService.exportOffersToExcel(request, exportedBy);
        String filename = offerExportService.generateExportFilename(request);

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excelBytes);
    }
}
