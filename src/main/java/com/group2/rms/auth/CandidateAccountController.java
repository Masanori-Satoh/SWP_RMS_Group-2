package com.group2.rms.controller;

import com.group2.rms.service.AccountListService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Authentication-account oversight; candidate profile editing belongs to the candidate domain. */
@Controller
@RequestMapping("/admin/candidate-accounts")
public class CandidateAccountController {
    private final AccountListService accounts;

    public CandidateAccountController(AccountListService accounts) { this.accounts = accounts; }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String search,
                       @RequestParam(defaultValue = "") String status,
                       @RequestParam(defaultValue = "name") String sort,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("accounts", accounts.findCandidateAccounts(search, status, sort, page));
        model.addAttribute("search", search.trim());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSort", sort);
        return "admin/accounts/candidates";
    }

    @PostMapping("/{userId}/deactivate")
    public String deactivate(@PathVariable int userId, RedirectAttributes redirectAttributes) {
        accounts.deactivateCandidate(userId);
        redirectAttributes.addFlashAttribute("successMessage", "Candidate account deactivated successfully.");
        return "redirect:/admin/candidate-accounts";
    }
}
