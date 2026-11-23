package com.group2.rms.candidate.controller;

import com.group2.rms.candidate.dto.MyApplicationListResponse;
import com.group2.rms.candidate.exception.ApplicationWithdrawalException;
import com.group2.rms.candidate.service.MyApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller quản lý đơn ứng tuyển cá nhân từ phía ứng viên.
 */
@Controller
@RequestMapping("/portal/applications")
@RequiredArgsConstructor
public class MyApplicationController {

    private final MyApplicationService myApplicationService;

    /**
     * Hiển thị danh sách các đơn ứng tuyển của ứng viên theo tab.
     */
    @GetMapping
    public String list(@RequestParam(required = false) String tab, Model model) {
        MyApplicationListResponse page = myApplicationService.list(tab);
        model.addAttribute("applications", page.applications());
        model.addAttribute("tab", page.tab());
        model.addAttribute("allCount", page.allCount());
        model.addAttribute("activeCount", page.activeCount());
        model.addAttribute("endedCount", page.endedCount());
        model.addAttribute("urgent", page.urgent());
        model.addAttribute("activeMenu", "applications");
        return "candidate/my-applications";
    }

    /**
     * Hiển thị tiến trình chi tiết của một đơn ứng tuyển.
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("myApplication", myApplicationService.detail(id));
        model.addAttribute("activeMenu", "applications");
        return "candidate/my-application-detail";
    }

    /**
     * Tải lại file CV đã nộp của đơn ứng tuyển.
     */
    @GetMapping("/{id}/cv")
    public ResponseEntity<Resource> cv(@PathVariable Integer id) {
        return myApplicationService.cv(id).toResponse("CV-" + id + ".pdf");
    }

    /**
     * Xử lý rút đơn ứng tuyển của ứng viên.
     */
    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            String postingTitle = myApplicationService.withdraw(id);
            redirect.addFlashAttribute("successMessage", "Đã rút hồ sơ ứng tuyển vị trí " + postingTitle + ".");
            return "redirect:/portal/applications";
        } catch (ApplicationWithdrawalException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/portal/applications/" + id;
        }
    }
}
