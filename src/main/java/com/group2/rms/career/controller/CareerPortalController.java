package com.group2.rms.career.controller;

import com.group2.rms.career.dto.CareerJobDetailResponse;
import com.group2.rms.career.dto.CareerJobListResponse;
import com.group2.rms.career.service.CareerPortalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/jobs")
public class CareerPortalController {

    @Autowired
    private CareerPortalService jobPostingService;

    @GetMapping
    public String viewPublicJobList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String employmentType,
            @PageableDefault(size = 9, sort = "postingDate", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {

        Page<CareerJobListResponse> jobsPage = jobPostingService.getPublishedJobs(keyword, departmentId,
                employmentType, pageable);
        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("departments", jobPostingService.getAllActiveDepartments());
        model.addAttribute("selectedKeyword", keyword);
        model.addAttribute("selectedDept", departmentId);
        model.addAttribute("selectedType", employmentType);

        return "candidate/job-board";
    }

    @GetMapping("/jobs/{id}")
    public String viewJobDetail(@PathVariable("id") Integer id, Model model, Authentication authentication) {
        CareerJobDetailResponse jobDetail = jobPostingService.getPublishedJobDetail(id, authentication);
        model.addAttribute("job", jobDetail);
        return "candidate/job-detail";
    }
}
