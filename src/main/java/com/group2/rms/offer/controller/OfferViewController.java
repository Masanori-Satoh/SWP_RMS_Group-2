package com.group2.rms.offer.controller;

import com.group2.rms.dto.response.OfferResponseDto;
import com.group2.rms.dto.response.PassedCandidateResponseDto;
import com.group2.rms.offer.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

/**
 * Controller điều hướng giao diện quản lý Offer (Screen 30 & Screen 32)
 * Kế thừa layout chung của RMS: app-layout.css & fragments/sidebar.
 */
@Controller
@RequestMapping("/offers")
@RequiredArgsConstructor
public class OfferViewController {

    private final OfferService offerService;

    @GetMapping
    public String listOffers(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            Principal principal) {

        int validPage = Math.max(0, page);
        Page<OfferResponseDto> pagedData = offerService.getAllOffersForHr(status, PageRequest.of(validPage, size));
        List<PassedCandidateResponseDto> passedCandidates = offerService.getPassedCandidatesForOffer();

        model.addAttribute("offers", pagedData.getContent());
        model.addAttribute("offersPage", pagedData);
        model.addAttribute("currentPage", validPage);
        model.addAttribute("totalPages", Math.max(1, pagedData.getTotalPages()));
        model.addAttribute("totalElements", pagedData.getTotalElements());
        model.addAttribute("currentStatus", status != null ? status : "ALL");
        model.addAttribute("passedCandidates", passedCandidates);
        model.addAttribute("currentUser", principal != null ? principal.getName() : "HR Specialist");

        return "offers/list";
    }
}
