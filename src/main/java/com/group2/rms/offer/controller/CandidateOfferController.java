package com.group2.rms.offer.controller;

import com.group2.rms.offer.dto.CandidateOfferDetailResponse;
import com.group2.rms.offer.exception.OfferResponseException;
import com.group2.rms.offer.service.CandidateOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thư mời nhận việc của ứng viên (menu sidebar candidate trỏ tới /portal/offers): xem, nhận, từ chối.
 * {@code SecurityConfig} giới hạn {@code /portal/offers/**} cho ROLE_CANDIDATE; ứng viên chỉ thấy và trả lời
 * offer của chính mình (offer của người khác là 404). Duyệt / từ chối offer nội bộ là việc của Director, ở {@link OfferController}.
 */
@Controller
@RequestMapping("/portal/offers")
@RequiredArgsConstructor
public class CandidateOfferController {

    private final CandidateOfferService candidateOffers;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("offers", candidateOffers.list());
        model.addAttribute("activeMenu", "offers");
        return "offers/candidate-list";
    }

    /** Màn hình riêng xem chi tiết Thư mời nhận việc (Offer Letter). */
    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("offer", candidateOffers.getDetail(id));
        model.addAttribute("activeMenu", "offers");
        return "offers/candidate-detail";
    }

    /** Màn hình riêng xác nhận từ chối Thư mời nhận việc (Decline Page). */
    @GetMapping("/{id}/decline")
    public String declinePage(@PathVariable Integer id, Model model, RedirectAttributes redirect) {
        CandidateOfferDetailResponse offer = candidateOffers.getDetail(id);
        if (!offer.canRespond()) {
            String msg = "Đã quá hạn phản hồi".equals(offer.statusLabel())
                    ? "Thư mời này đã quá ngày bắt đầu nhận việc dự kiến, không thể phản hồi nữa."
                    : "Thư mời này đã được trả lời nên không thể thay đổi.";
            redirect.addFlashAttribute("errorMessage", msg);
            return "redirect:/portal/offers/" + id;
        }
        model.addAttribute("offer", offer);
        model.addAttribute("activeMenu", "offers");
        return "offers/candidate-decline";
    }

    /** Nhận offer; kết quả hoặc lỗi hiện bằng thông báo trên trang danh sách hoặc chi tiết. */
    @PostMapping("/{id}/accept")
    public String accept(@PathVariable Integer id,
                         @RequestParam(value = "fromDetail", required = false, defaultValue = "false") boolean fromDetail,
                         RedirectAttributes redirect) {
        try {
            String postingTitle = candidateOffers.accept(id);
            redirect.addFlashAttribute("successMessage", "Bạn đã đồng ý nhận việc vị trí " + postingTitle + ".");
            redirect.addFlashAttribute("decisionType", "ACCEPTED");
            redirect.addFlashAttribute("congratsTitle", postingTitle);
        } catch (OfferResponseException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return fromDetail ? "redirect:/portal/offers/" + id : "redirect:/portal/offers";
    }

    /** Từ chối offer kèm lý do; kết quả hoặc lỗi hiện bằng thông báo trên trang danh sách hoặc chi tiết. */
    @PostMapping("/{id}/decline")
    public String decline(@PathVariable Integer id,
                          @RequestParam(value = "declineReason", required = false) String declineReason,
                          @RequestParam(value = "fromDetail", required = false, defaultValue = "false") boolean fromDetail,
                          RedirectAttributes redirect) {
        try {
            String postingTitle = (declineReason != null && !declineReason.isBlank())
                    ? candidateOffers.decline(id, declineReason.trim())
                    : candidateOffers.decline(id);
            redirect.addFlashAttribute("successMessage", "Bạn đã từ chối thư mời nhận việc vị trí " + postingTitle + ".");
            redirect.addFlashAttribute("decisionType", "DECLINED");
            if (declineReason != null && !declineReason.isBlank()) {
                redirect.addFlashAttribute("declineReasonDisplay", declineReason.trim());
            }
        } catch (OfferResponseException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return fromDetail ? "redirect:/portal/offers/" + id : "redirect:/portal/offers";
    }
}
