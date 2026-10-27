package com.group2.rms.candidate.application.portal.controller;

import com.group2.rms.candidate.application.portal.dto.MyApplicationListResponse;
import com.group2.rms.candidate.application.portal.exception.ApplicationWithdrawalException;
import com.group2.rms.candidate.application.portal.service.MyApplicationService;
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
 * "Đơn của tôi" của ứng viên: danh sách (3 tab), chi tiết một đơn, xem lại CV đã nộp (L8) và rút đơn (UC 39).
 * {@code SecurityConfig} giới hạn {@code /portal/applications/**} cho ROLE_CANDIDATE; ứng viên chỉ thấy đơn của mình
 * (đơn người khác: 404 từ Service).
 */
@Controller
@RequestMapping("/portal/applications")
@RequiredArgsConstructor
public class MyApplicationController {

    private final MyApplicationService myApplications;

    /** Danh sách đơn; {@code tab} = {@code active} | {@code ended}, giá trị khác (gõ tay trên URL) thì xem tất cả. */
    @GetMapping
    public String list(@RequestParam(required = false) String tab, Model model) {
        MyApplicationListResponse page = myApplications.list(tab);
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
     * Chi tiết một đơn: chỗ duy nhất để rút đơn. Tên biến không phải {@code application}: Thymeleaf dành tên đó cho
     * thuộc tính của ServletContext ({@code ${application.x}}), nên đặt vậy thì trang đọc nhầm ra {@code null}.
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("myApplication", myApplications.detail(id));
        model.addAttribute("activeMenu", "applications");
        return "candidate/my-application-detail";
    }

    /** CV đã nộp, hiện trong popup của danh sách / trang chi tiết (khung cùng nguồn: {@code SecurityConfig.CV_PATHS}). */
    @GetMapping("/{id}/cv")
    public ResponseEntity<Resource> cv(@PathVariable Integer id) {
        return myApplications.cv(id).toResponse("CV-" + id + ".pdf");
    }

    /** Rút đơn; thành công về danh sách, bị từ chối thì về trang chi tiết kèm lý do. */
    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            String postingTitle = myApplications.withdraw(id);
            redirect.addFlashAttribute("successMessage", "Đã rút hồ sơ ứng tuyển vị trí " + postingTitle + ".");
            return "redirect:/portal/applications";
        } catch (ApplicationWithdrawalException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/portal/applications/" + id;
        }
    }
}
