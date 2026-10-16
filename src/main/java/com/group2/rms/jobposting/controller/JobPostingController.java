package com.group2.rms.jobposting.controller;

import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.InternalJobPostingResponse;
import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.service.JobPostingService;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.candidate.service.RecruitmentEmailService;
import com.group2.rms.candidate.exception.RecruitmentEmailException;
import com.group2.rms.candidate.exception.TalentPoolException;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/internal/job-postings")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;
    private final RequisitionAccess requisitionAccess;
    private final DepartmentRepository departmentRepository;
    private final RecruitmentEmailService recruitmentEmailService;

    @ModelAttribute
    void populateViewerAttributes(Model model) {
        User currentUser = requisitionAccess.actor();
        model.addAttribute("viewerRole", requisitionAccess.role(currentUser));
        model.addAttribute("viewerName", currentUser.getFullName());
        model.addAttribute("activeMenu", "job-postings");
    }

    @GetMapping
    public String listJobPostings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        User currentUser = requisitionAccess.actor();
        Page<InternalJobPostingResponse> postingPage = jobPostingService.searchInternalJobPostings(
                page, size, q, departmentId, status, sort, currentUser
        );

        model.addAttribute("postings", postingPage.getContent());
        model.addAttribute("currentPage", postingPage.getNumber() + 1);
        model.addAttribute("totalPages", Math.max(1, postingPage.getTotalPages()));
        model.addAttribute("pageSize", postingPage.getSize());
        model.addAttribute("startPage", Math.max(1, postingPage.getNumber() - 1));
        model.addAttribute("endPage", Math.min(Math.max(1, postingPage.getTotalPages()), postingPage.getNumber() + 3));
        model.addAttribute("totalElements", postingPage.getTotalElements());

        model.addAttribute("search", q);
        model.addAttribute("selectedDepartment", departmentId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSort", sort);

        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("statuses", List.of("Draft", "Published", "Closed", "Paused"));

        return "job-postings/list";
    }

    @GetMapping("/dashboard")
    public String hrDashboard(Model model) {
        var dashboard = jobPostingService.getDashboard(requisitionAccess.actor());
        model.addAttribute("activePostingsCount", dashboard.activeCount());
        model.addAttribute("draftPostingsCount", dashboard.draftCount());
        model.addAttribute("pendingReqsCount", dashboard.requisitionsAwaitingPosting().size());
        model.addAttribute("closedPostingsCount", dashboard.closedCount());
        model.addAttribute("deptStats", dashboard.departmentStats());
        model.addAttribute("reqsAwaitingPosting", dashboard.requisitionsAwaitingPosting());
        model.addAttribute("recentPostings", dashboard.recentPostings());

        return "job-postings/dashboard";
    }

    @GetMapping("/create")
    public String showCreateForm(@RequestParam("requisitionId") Integer requisitionId, Model model) {
        User currentUser = requisitionAccess.actor();
        JobPostingCreateRequest formDto = jobPostingService.prepareCreateForm(requisitionId, currentUser);
        model.addAttribute("postingDto", formDto);
        return "job-postings/create";
    }

    @PostMapping("/create")
    public String createJobPosting(
            @Valid @ModelAttribute("postingDto") JobPostingCreateRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = requisitionAccess.actor();

        if (bindingResult.hasErrors()) {
            return "job-postings/create";
        }

        try {
            if (request.getJobPostingId() != null) {
                throw new JobPostingValidationException("Không thể cập nhật tin qua đường dẫn tạo mới.");
            }
            boolean publish = "publish".equalsIgnoreCase(request.getAction());
            if (!publish && !"draft".equalsIgnoreCase(request.getAction())) {
                throw new JobPostingValidationException("Thao tác lưu tin không hợp lệ.");
            }
            if (publish) jobPostingService.createAndPublishJobPosting(request, currentUser);
            else jobPostingService.createJobPosting(request, currentUser);
            redirectAttributes.addFlashAttribute("message", publish
                    ? "Đã đăng tin tuyển dụng thành công." : "Đã lưu bản nháp tin tuyển dụng.");
            return "redirect:/internal/job-postings";
        } catch (JobPostingValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "job-postings/create";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateJobPosting(@PathVariable Integer id,
            @Valid @ModelAttribute("postingDto") JobPostingCreateRequest request,
            BindingResult errors, Model model, RedirectAttributes flash) {
        User actor = requisitionAccess.actor();
        if (errors.hasErrors()) return "job-postings/create";
        try {
            boolean publish = "publish".equalsIgnoreCase(request.getAction());
            if (!publish && !"draft".equalsIgnoreCase(request.getAction())) {
                throw new JobPostingValidationException("Thao tác lưu tin không hợp lệ.");
            }
            if (publish) jobPostingService.updateAndPublishJobPosting(id, request, actor);
            else jobPostingService.updateJobPosting(id, request, actor);
            flash.addFlashAttribute("message", publish ? "Đã cập nhật và đăng tin." : "Đã cập nhật tin tuyển dụng.");
            return "redirect:/internal/job-postings";
        } catch (JobPostingValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "job-postings/create";
        }
    }

    @PostMapping("/{id}/publish")
    public String publishJobPosting(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            jobPostingService.publishJobPosting(id, requisitionAccess.actor());
            flash.addFlashAttribute("successMessage", "Đã đăng tin tuyển dụng.");
        } catch (JobPostingValidationException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/internal/job-postings/" + id;
    }

    @PostMapping("/{id}/pause")
    public String pauseJobPosting(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            jobPostingService.pauseJobPosting(id, requisitionAccess.actor());
            flash.addFlashAttribute("successMessage", "Đã tạm dừng tin tuyển dụng.");
        } catch (JobPostingValidationException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/internal/job-postings/" + id;
    }

    @PostMapping("/{id}/close")
    public String closeJobPosting(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            jobPostingService.closeJobPosting(id, requisitionAccess.actor());
            flash.addFlashAttribute("successMessage", "Đã đóng đợt tuyển và xử lý các hồ sơ còn lại.");
            try {
                recruitmentEmailService.sendJobClosure(id);
            } catch (RecruitmentEmailException ex) {
                flash.addFlashAttribute("errorMessage", ex.getMessage());
            }
        } catch (TalentPoolException | JobPostingValidationException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/internal/job-postings/" + id;
    }

    @GetMapping("/{id}")
    public String showJobPostingDetail(@PathVariable("id") Integer id, Model model) {
        User currentUser = requisitionAccess.actor();
        InternalJobPostingDetailResponse detail = jobPostingService.getInternalJobPostingDetail(id, currentUser);
        model.addAttribute("posting", detail);
        return "job-postings/detail";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        User currentUser = requisitionAccess.actor();
        JobPostingCreateRequest formDto = jobPostingService.prepareEditForm(id, currentUser);
        model.addAttribute("postingDto", formDto);
        return "job-postings/create";
    }

    @PostMapping("/delete/{id}")
    public String deleteJobPosting(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        User currentUser = requisitionAccess.actor();
        try {
            jobPostingService.deleteJobPosting(id, currentUser);
            redirectAttributes.addFlashAttribute("message", "Đã xóa tin tuyển dụng thành công.");
        } catch (JobPostingValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/internal/job-postings";
    }
}
