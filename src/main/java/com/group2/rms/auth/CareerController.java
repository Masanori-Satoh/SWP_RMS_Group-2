package com.group2.rms.controller;

import com.group2.rms.service.CareerService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.stream.Collectors;

@Controller
public class CareerController {
    private final CareerService careers;

    public CareerController(CareerService careers) {
        this.careers = careers;
    }

    @ModelAttribute("viewer")
    public CareerService.Viewer viewer(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) return null;
        return careers.viewer(authentication.getName()).orElse(null);
    }

    @GetMapping({"/", "/jobs"})
    public String homepage(Model model) {
        var jobs = careers.openJobs();
        model.addAttribute("jobs", jobs);
        var activeDepartments = jobs.stream().map(CareerService.PublicJob::departmentId).collect(Collectors.toSet());
        model.addAttribute("departments", careers.departments().stream()
                .sorted(Comparator.comparing((CareerService.DepartmentOption department) ->
                                !activeDepartments.contains(department.departmentId()))
                        .thenComparing(CareerService.DepartmentOption::departmentName))
                .toList());
        model.addAttribute("locations", jobs.stream().map(CareerService.PublicJob::workLocation)
                .filter(location -> location != null && !location.isBlank()).distinct().sorted().toList());
        return "careers/index";
    }

    @GetMapping("/jobs/{id}")
    public String detail(@PathVariable int id, Model model, HttpServletResponse response) {
        var job = careers.openJob(id);
        if (job.isEmpty()) return unavailable(response, model, 404,
                "This role is no longer available", "Browse our current open positions for another opportunity.");
        model.addAttribute("job", job.get());
        return "careers/detail";
    }

    @GetMapping("/jobs/{id}/apply")
    public String apply(@PathVariable int id, Authentication authentication,
                        Model model, HttpServletResponse response) {
        var job = careers.openJob(id);
        if (job.isEmpty()) return unavailable(response, model, 404,
                "This role is no longer available", "Browse our current open positions for another opportunity.");
        var candidate = careers.candidateDetails(authentication.getName());
        if (candidate.isEmpty()) return unavailable(response, model, 409,
                "Your candidate profile is missing", "Ask your system administrator to check the profile linked to your account. No application has been created.");
        model.addAttribute("job", job.get());
        model.addAttribute("candidate", candidate.get());
        return "careers/apply";
    }

    private String unavailable(HttpServletResponse response, Model model, int status,
                               String title, String message) {
        response.setStatus(status);
        model.addAttribute("title", title);
        model.addAttribute("message", message);
        return "careers/unavailable";
    }
}
