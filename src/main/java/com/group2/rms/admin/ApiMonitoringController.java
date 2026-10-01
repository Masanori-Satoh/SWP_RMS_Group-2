package com.group2.rms.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** System Admin monitoring dashboard and CSRF-protected probe action. */
@Controller
@RequestMapping("/admin/api-monitoring")
public class ApiMonitoringController {

    private final ApiMonitoringService monitoring;

    public ApiMonitoringController(ApiMonitoringService monitoring) {
        this.monitoring = monitoring;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("endpoints", monitoring.rows());
        return "admin/api-monitoring/index";
    }

    @PostMapping("/probe/internal")
    public String probeInternal(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        ApiMonitoringService.ProbeOutcome outcome = monitoring.probeInternal(request);
        String message = outcome.detail() + " • " + outcome.elapsedMs() + " ms";
        redirectAttributes.addFlashAttribute(outcome.success() ? "successMessage" : "failureMessage", message);
        return "redirect:/admin/api-monitoring";
    }
}
