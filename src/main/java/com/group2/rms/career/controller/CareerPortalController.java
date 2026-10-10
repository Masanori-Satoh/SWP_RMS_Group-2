package com.group2.rms.career.controller;

import com.group2.rms.candidate.dto.ApplyJobRequest;
import com.group2.rms.candidate.exception.ApplicationSubmissionException;
import com.group2.rms.candidate.service.ApplicationSubmissionService;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CareerPortalController {

    private final CareerPortalService careerPortalService;

    private final ApplicationSubmissionService applicationSubmissionService;

    private static final int FEATURED_JOB_COUNT = 3;
    private static final int JOBS_PER_PAGE = 6;

    @ModelAttribute("currentUser")
    public ViewerProfileResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return careerPortalService.getViewerProfile(authentication.getName()).orElse(null);
    }

    /** Landing page: giới thiệu Mộc + 3 vị trí mới nhất (theo ngày đăng, lấy tại thời điểm truy cập). */
    @GetMapping("/")
    public String viewLandingPage(Model model) {
        model.addAttribute("featuredJobs", careerPortalService.getLatestJobs(FEATURED_JOB_COUNT));
        return "candidate/landing";
    }

    /** Jobs board: tìm kiếm, lọc và phân trang toàn bộ tin tuyển dụng công khai. */
    @GetMapping("/jobs")
    public String viewPublicJobList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String employmentType,
            @PageableDefault(size = JOBS_PER_PAGE, sort = "postingDate", direction = Sort.Direction.DESC) Pageable pageable,
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
        model.addAttribute("applyForm", new ApplyJobRequest(null));
        return "candidate/job-detail";
    }

    /**
     * Điểm vào sau đăng nhập: khách bấm "Ứng tuyển" → /login → quay lại đây → về trang tin với hộp thoại mở sẵn.
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

    @PostMapping("/jobs/{id}/apply")
    public String submitApplication(@PathVariable("id") Integer id,
            @ModelAttribute("applyForm") ApplyJobRequest form, BindingResult errors,
            Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        try {
            applicationSubmissionService.apply(authentication.getName(), id, form.cvFile());
            redirectAttributes.addFlashAttribute("applySuccess", true);
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
        return "candidate/job-detail";
    }
}
