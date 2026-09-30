package com.rms.feature.offer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller điều hướng giao diện Thymeleaf cho phân hệ Offer.
 * 
 * Luồng màn hình:
 * - Màn hình 30, 31, 32: /offers/internal (Dành cho Hiring Manager & Director)
 * - Màn hình 40, 41, 42: /offers/candidate/{offerId} (Dành cho Candidate)
 */
@Controller
@RequestMapping("/offers")
public class OfferViewController {

    /**
     * Màn hình 30, 31, 32: Quản lý danh sách Offer, Đề xuất mức lương (HM) và Phê duyệt (Director).
     */
    @GetMapping("/internal")
    public String internalOffersPage() {
        return "offer/internal-offers";
    }

    /**
     * Màn hình 40, 41, 42: Xem thư mời làm việc và phản hồi dành cho Ứng viên.
     */
    @GetMapping("/candidate/{offerId}")
    public String candidateOfferPageByPath(@PathVariable("offerId") Long offerId, Model model) {
        model.addAttribute("offerId", offerId);
        return "offer/candidate-offer";
    }

    /**
     * Màn hình 40, 41, 42: Hỗ trợ truy cập dạng query parameter (/offers/candidate?offerId=1).
     */
    @GetMapping("/candidate")
    public String candidateOfferPageByParam(@RequestParam(value = "offerId", required = false) Long offerId, Model model) {
        model.addAttribute("offerId", offerId);
        return "offer/candidate-offer";
    }
}
