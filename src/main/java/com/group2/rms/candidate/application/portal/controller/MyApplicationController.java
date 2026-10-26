package com.group2.rms.candidate.application.portal.controller;

import com.group2.rms.candidate.application.portal.dto.MyApplicationListResponse;
import com.group2.rms.candidate.application.portal.exception.ApplicationWithdrawalException;
import com.group2.rms.candidate.application.portal.service.MyApplicationService;
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
 * Hồ sơ đã ứng tuyển của ứng viên (5.1.38 My Applications) và rút đơn (UC 39).
 * {@code SecurityConfig} giới hạn {@code /portal/applications/**} cho ROLE_CANDIDATE; ứng viên chỉ thấy đơn của mình.
 */
@Controller
@RequestMapping("/portal/applications")
@RequiredArgsConstructor
public class MyApplicationController {

    private final MyApplicationService myApplications;

    /**
     * Danh sách đơn; {@code step} (1–4) lọc theo bước khi ứng viên bấm một thẻ trên dải hướng dẫn. Giá trị khác
     * (gõ tay trên URL) thì xem tất cả, không báo lỗi.
     */
    @GetMapping
    public String list(@RequestParam(required = false) String step, Model model) {
        Integer selected = step != null && step.matches("[1-4]") ? Integer.valueOf(step) : null;
        MyApplicationListResponse page = myApplications.list(selected);
        model.addAttribute("applications", page.applications());
        model.addAttribute("stepCounts", page.stepCounts());
        model.addAttribute("selectedStep", page.selectedStep());
        model.addAttribute("totalApplications", page.total());
        model.addAttribute("activeMenu", "applications");
        return "candidate/my-applications";
    }

    /** Rút đơn; kết quả hoặc lỗi hiện bằng thông báo trên trang danh sách. */
    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            String postingTitle = myApplications.withdraw(id);
            redirect.addFlashAttribute("successMessage", "Đã rút hồ sơ ứng tuyển vị trí " + postingTitle + ".");
        } catch (ApplicationWithdrawalException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/portal/applications";
    }
}
