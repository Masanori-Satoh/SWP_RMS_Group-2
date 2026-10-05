package com.group2.rms.career.controller;

import com.group2.rms.career.dto.PublicJobDetailResponse;
import com.group2.rms.career.dto.PublicJobListResponse;
import com.group2.rms.career.dto.ViewerProfileResponse;
import com.group2.rms.career.service.CareerPortalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CareerPortalController {

    private final CareerPortalService careerPortalService;

    @ModelAttribute("currentUser")
    public ViewerProfileResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return careerPortalService.getViewerProfile(authentication.getName()).orElse(null);
    }

    @GetMapping({ "/", "/jobs" })
    public String viewPublicJobList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String employmentType,
            @PageableDefault(size = 9, sort = "postingDate", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {

        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }

        if (employmentType != null) {
            employmentType = employmentType.trim();
            if (employmentType.isEmpty()) {
                employmentType = null;
            }
        }

        Page<PublicJobListResponse> jobsPage = careerPortalService.getPublishedJobs(keyword, departmentId,
                employmentType, pageable);

        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("departments", careerPortalService.getSmartSortedDepartments());

        model.addAttribute("selectedKeyword", keyword);
        model.addAttribute("selectedDept", departmentId);
        model.addAttribute("selectedType", employmentType);

        return "candidate/job-board";
    }

    @GetMapping("/jobs/{id}")
    public String viewJobDetail(@PathVariable("id") Integer id, Model model, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        PublicJobDetailResponse jobDetail = careerPortalService.getPublishedJobDetail(id, username);
        model.addAttribute("job", jobDetail);
        return "candidate/job-detail";
    }

    @GetMapping("/jobs/{id}/apply")
    public String applyForJob(@PathVariable("id") Integer id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        careerPortalService.validateJobForApplication(id);
        careerPortalService.validateCandidateApplicationProfile(authentication.getName());

        throw new com.group2.rms.core.exception.BaseBusinessException(
                "Tính năng nộp hồ sơ trực tuyến sẽ được bổ sung ở Iteration 2.", "ITERATION_2_PENDING");
    }
}
