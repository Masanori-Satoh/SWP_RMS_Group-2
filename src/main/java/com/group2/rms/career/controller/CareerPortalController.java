package com.group2.rms.career.controller;

import com.group2.rms.candidate.dto.ApplicationSubmissionRequest;
import com.group2.rms.candidate.exception.ApplicationSubmissionException;
import com.group2.rms.candidate.service.ApplicationSubmissionService;
import com.group2.rms.career.dto.PublicJobDetailResponse;
import com.group2.rms.career.dto.PublicJobListResponse;
import com.group2.rms.career.dto.ViewerProfileResponse;
import com.group2.rms.career.service.CareerPortalService;
import com.group2.rms.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller phục vụ cổng tuyển dụng công khai cho ứng viên: tra cứu tin tuyển dụng và nộp hồ sơ.
 */
@Controller
@RequiredArgsConstructor
public class CareerPortalController {

    private static final int FEATURED_JOB_COUNT = 3;
    private static final int JOBS_PER_PAGE = 6;
    private static final String PUBLIC_NAV = "publicNav";
    private static final String NAV_JOB_DETAIL = "job-detail";

    private final CareerPortalService careerPortalService;
    private final ApplicationSubmissionService applicationSubmissionService;

    /**
     * Cung cấp thông tin tài khoản người xem đang đăng nhập vào model.
     */
    @ModelAttribute("currentUser")
    public ViewerProfileResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return careerPortalService.getViewerProfile(authentication.getName()).orElse(null);
    }

    /**
     * Hiển thị landing page giới thiệu công ty và danh sách việc làm nổi bật mới nhất.
     */
    @GetMapping("/")
    public String viewLandingPage(Model model) {
        model.addAttribute("featuredJobs", careerPortalService.getLatestJobs(FEATURED_JOB_COUNT));
        return "candidate/landing";
    }

    /**
     * Hiển thị bảng tin tuyển dụng công khai, hỗ trợ tìm kiếm và lọc phân trang.
     */
    @GetMapping("/jobs")
    public String viewPublicJobList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String workModel,
            @RequestParam(required = false) String salaryRange,
            @PageableDefault(size = JOBS_PER_PAGE, sort = "postingDate", direction = Sort.Direction.DESC)
            Pageable pageable,
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

        if (workModel != null) {
            workModel = workModel.trim();
            if (workModel.isEmpty()) {
                workModel = null;
            }
        }

        if (salaryRange != null) {
            salaryRange = salaryRange.trim();
            if (salaryRange.isEmpty()) {
                salaryRange = null;
            }
        }

        Page<PublicJobListResponse> jobsPage = careerPortalService.getPublishedJobs(
                keyword, departmentId, employmentType, workModel, salaryRange, pageable);

        model.addAttribute("jobsPage", jobsPage);
        model.addAttribute("departments", careerPortalService.getSmartSortedDepartments());

        model.addAttribute("selectedKeyword", keyword);
        model.addAttribute("selectedDept", departmentId);
        model.addAttribute("selectedType", employmentType);
        model.addAttribute("selectedWorkModel", workModel);
        model.addAttribute("selectedSalary", salaryRange);

        return "candidate/job-board";
    }

    /**
     * Hiển thị chi tiết một vị trí tuyển dụng đang mở cho ứng viên.
     */
    @GetMapping("/jobs/{id}")
    public String viewJobDetail(@PathVariable("id") Integer id, Model model, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        PublicJobDetailResponse jobDetail = careerPortalService.getPublishedJobDetail(id, username);
        model.addAttribute("job", jobDetail);
        model.addAttribute("applyForm", new ApplicationSubmissionRequest(null));
        model.addAttribute(PUBLIC_NAV, NAV_JOB_DETAIL);
        return "candidate/job-detail";
    }

    /**
     * Điều hướng ứng viên sau khi đăng nhập về form ứng tuyển công việc.
     */
    @GetMapping("/jobs/{id}/apply")
    public String applyForJob(@PathVariable("id") Integer id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        careerPortalService.validateJobForApplication(id);
        careerPortalService.validateCandidateApplicationProfile(authentication.getName());

        return "redirect:/jobs/" + id + "#apply";
    }

    /**
     * Tiếp nhận hồ sơ ứng tuyển, upload CV và thông báo kết quả.
     */
    @PostMapping("/jobs/{id}/apply")
    public String submitApplication(
            @PathVariable("id") Integer id,
            @ModelAttribute("applyForm") ApplicationSubmissionRequest form,
            BindingResult errors,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Integer applicationId = applicationSubmissionService.apply(authentication.getName(), id, form.cvFile());
            redirectAttributes.addFlashAttribute("applySuccess", true);
            redirectAttributes.addFlashAttribute("appliedApplicationId", applicationId);
            return "redirect:/jobs/" + id;
        } catch (ApplicationSubmissionException e) {
            if (e.getField() != null) {
                errors.rejectValue(e.getField(), "apply.invalid", e.getMessage());
            } else {
                errors.reject("apply.invalid", e.getMessage());
            }
        }

        model.addAttribute("job", careerPortalService.getPublishedJobDetail(id, authentication.getName()));
        model.addAttribute("applyDialogOpen", true);
        model.addAttribute(PUBLIC_NAV, NAV_JOB_DETAIL);
        return "candidate/job-detail";
    }

    /**
     * Xử lý ngoại lệ khi tin tuyển dụng không tìm thấy hoặc đã đóng.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public String whenJobUnavailable(ResourceNotFoundException e, RedirectAttributes flash) {
        flash.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/jobs";
    }
}
