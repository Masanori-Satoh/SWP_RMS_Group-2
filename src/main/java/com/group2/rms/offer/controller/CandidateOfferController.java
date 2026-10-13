package com.group2.rms.offer.controller;

import com.group2.rms.offer.exception.OfferResponseException;
import com.group2.rms.offer.service.CandidateOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    /** Nhận offer; kết quả hoặc lỗi hiện bằng thông báo trên trang danh sách. */
    @PostMapping("/{id}/accept")
    public String accept(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            String postingTitle = candidateOffers.accept(id);
            redirect.addFlashAttribute("successMessage", "Bạn đã nhận offer vị trí " + postingTitle + ".");
        } catch (OfferResponseException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/portal/offers";
    }

    /** Từ chối offer; kết quả hoặc lỗi hiện bằng thông báo trên trang danh sách. */
    @PostMapping("/{id}/decline")
    public String decline(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            String postingTitle = candidateOffers.decline(id);
            redirect.addFlashAttribute("successMessage", "Bạn đã từ chối offer vị trí " + postingTitle + ".");
        } catch (OfferResponseException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/portal/offers";
    }
}
