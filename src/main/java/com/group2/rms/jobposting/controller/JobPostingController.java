package com.group2.rms.jobposting.controller;

import com.group2.rms.talentpool.exception.RecruitmentEmailException;
import com.group2.rms.talentpool.exception.TalentPoolException;
import com.group2.rms.talentpool.service.RecruitmentEmailService;
import com.group2.rms.talentpool.service.TalentPoolService;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.util.FilterLinks;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.JobPostingRequest;
import com.group2.rms.jobposting.dto.JobPostingSearchRequest;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.jobposting.service.JobPostingListService;
import com.group2.rms.jobposting.service.JobPostingService;
import com.group2.rms.jobposting.validator.JobPostingValidator;
import com.group2.rms.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

/*
 * CONTROLLER: màn tin tuyển dụng nội bộ (danh sách, form tạo / sửa, chi tiết, nút chuyển trạng thái).
 */
@Controller
@RequestMapping("/internal/job-postings")
@RequiredArgsConstructor
public class JobPostingController {

    private static final String LIST_URL = "/internal/job-postings";
    private static final String FORM_VIEW = "job-postings/create";
    private static final String PUBLISHED_NOW_MESSAGE = "Đã đăng tin thành công lên trang tuyển dụng";
    private static final Set<String> FORM_FIELDS = Set.of("postingTitle", "jobDescription", "jobRequirements",
            "benefits", "workLocation", "workingHours", "employmentType", "workModel", "salaryMode",
            "salaryCustom", "numberOfPositionsDisplay", "applicationDeadline");

    private final JobPostingService jobPostingService;
    private final JobPostingListService listService;
    private final JobPostingAccess access;
    private final JobPostingValidator validator;
    private final RecruitmentEmailService recruitmentEmailService;
    private final TalentPoolService talentPoolService;

    @ModelAttribute
    // Đưa thông tin người xem vào model.
    void populateViewerAttributes(Model model) {
        User currentUser = access.actor();
        model.addAttribute("viewerRole", access.role(currentUser));
        model.addAttribute("viewerName", currentUser.getFullName());
        model.addAttribute("activeMenu", "job-postings");
    }

    @InitBinder("form")
    // Giới hạn các trường được nhập từ form.
    void formBinder(WebDataBinder binder) {
        binder.setAllowedFields("jobPostingId", "requisitionId", "postingTitle", "jobDescription", "jobRequirements",
                "benefits", "workLocation", "workingHours", "employmentType", "workModel", "salaryMode", "salaryCustom", "showPositions", "numberOfPositionsDisplay",
                "applicationDeadline", "action");
    }

    @GetMapping
    // Hiển thị danh sách tin tuyển dụng.
    public String listJobPostings(@ModelAttribute("searchRequest") JobPostingSearchRequest searchRequest,
            BindingResult invalidParams, HttpServletRequest request, Model model) {
        model.addAttribute("list", listService.getListPage(searchRequest, access.actor(),
                invalidParams.getFieldErrors().stream().map(FieldError::getField).distinct().toList()));
        // Link "bỏ một bộ lọc" cho các chip: giữ nguyên các tham số khác trên URL (dùng chung với màn yêu cầu)
        model.addAttribute("filterLinks", new FilterLinks(request.getRequestURI(), request.getQueryString()));
        model.addAttribute("bodyClass", "jp-list-page");
        return "job-postings/list";
    }

    @GetMapping("/create")
    // Hiển thị form tạo hoặc đăng lại tin.
    public String showCreateForm(@RequestParam(value = "requisitionId", required = false) String requisitionIdParam,
            @RequestParam(value = "fromPostingId", required = false) String fromPostingIdParam,
            Model model, RedirectAttributes flash) {
        User currentUser = access.actor();
        // Có tham số fromPostingId là "Đăng lại"; lấy phần số như "{1873}" -> 1873
        boolean isRepost = fromPostingIdParam != null;
        Integer fromPostingId = parseId(fromPostingIdParam);
        try {
            JobPostingRequest form;
            if (isRepost) {
                form = jobPostingService.prepareRepostForm(fromPostingId, currentUser);
            } else {
                form = jobPostingService.prepareCreateForm(parseId(requisitionIdParam), currentUser);
            }
            return renderForm(model, form, currentUser);
        } catch (JobPostingValidationException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            if (fromPostingId != null) {
                return redirectToDetail(fromPostingId);
            }
            return "redirect:" + LIST_URL;
        } catch (ResourceNotFoundException e) {
            // Mã yêu cầu / tin nguồn gõ tay không tồn tại: về danh sách tin kèm thông báo
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:" + LIST_URL;
        }
    }

    @GetMapping("/{id}/edit")
    // Hiển thị form sửa tin.
    public String showEditForm(@PathVariable("id") String idParam, Model model, RedirectAttributes flash) {
        User currentUser = access.actor();
        Integer id = parseId(idParam);
        try {
            return renderForm(model, jobPostingService.prepareEditForm(id, currentUser), currentUser);
        } catch (JobPostingValidationException e) {
            // Tin đã đóng: về trang chi tiết tin; mã sai: về danh sách
            flash.addFlashAttribute("errorMessage", e.getMessage());
            if (id != null) {
                return redirectToDetail(id);
            }
            return "redirect:" + LIST_URL;
        } catch (ResourceNotFoundException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:" + LIST_URL;
        }
    }

    @PostMapping("/create")
    // Tạo tin nháp hoặc đăng tin mới.
    public String createJobPosting(@Valid @ModelAttribute("form") JobPostingRequest form,
            BindingResult errors, Model model, RedirectAttributes flash) {
        User currentUser = access.actor();
        if (!errors.hasErrors()) {
            try {
                boolean isPublish = JobPostingValidator.isPublish(form.getAction());
                Integer id;
                if (isPublish) {
                    id = jobPostingService.createAndPublishJobPosting(form, currentUser);
                } else {
                    id = jobPostingService.createJobPosting(form, currentUser);
                }
                flash.addFlashAttribute("successMessage", savedMessage(form, isPublish, true));
                return redirectToDetail(id);
            } catch (JobPostingValidationException e) {
                addErrors(errors, e);
            }
        }
        return renderForm(model, jobPostingService.restoreSourceInfo(form, currentUser), currentUser);
    }

    @PostMapping("/{id}/edit")
    // Lưu thay đổi hoặc đăng tin đã có.
    public String updateJobPosting(@PathVariable Integer id,
            @Valid @ModelAttribute("form") JobPostingRequest form,
            BindingResult errors, Model model, RedirectAttributes flash) {
        User currentUser = access.actor();
        if (!errors.hasErrors()) {
            try {
                boolean isPublish = JobPostingValidator.isPublish(form.getAction());
                if (isPublish) {
                    jobPostingService.updateAndPublishJobPosting(id, form, currentUser);
                } else {
                    jobPostingService.updateJobPosting(id, form, currentUser);
                }
                flash.addFlashAttribute("successMessage", savedMessage(form, isPublish, false));
                return redirectToDetail(id);
            } catch (JobPostingValidationException e) {
                addErrors(errors, e);
            }
        }
        return renderForm(model, jobPostingService.restoreSourceInfo(form, currentUser), currentUser);
    }

    @PostMapping("/{id}/publish")
    // Đăng hoặc mở lại tin.
    public String publishJobPosting(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            jobPostingService.publishJobPosting(id, access.actor());
            flash.addFlashAttribute("successMessage", PUBLISHED_NOW_MESSAGE);
        } catch (JobPostingValidationException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return redirectToDetail(id);
    }

    @PostMapping("/{id}/pause")
    // Tạm dừng tin đang đăng.
    public String pauseJobPosting(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            jobPostingService.pauseJobPosting(id, access.actor());
            flash.addFlashAttribute("successMessage", "Đã tạm dừng tin tuyển dụng.");
        } catch (JobPostingValidationException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return redirectToDetail(id);
    }

    @PostMapping("/{id}/close")
    // Đóng tin tuyển dụng.
    public String closeJobPosting(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            jobPostingService.closeJobPosting(id, access.actor());
            flash.addFlashAttribute("successMessage", "Đã đóng đợt tuyển và xử lý các hồ sơ còn lại.");
        } catch (TalentPoolException | JobPostingValidationException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return redirectToDetail(id);
        }
        try {
            recruitmentEmailService.sendJobClosure(id);
        } catch (RecruitmentEmailException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return redirectToDetail(id);
    }

    @GetMapping("/{id}")
    // Hiển thị chi tiết tin.
    public String showJobPostingDetail(@PathVariable("id") String idParam, Model model, RedirectAttributes flash) {
        Integer id = parseId(idParam);
        InternalJobPostingDetailResponse detail;
        try {
            detail = jobPostingService.getInternalJobPostingDetail(id, access.actor());
        } catch (JobPostingValidationException | ResourceNotFoundException e) {
            // Mã sai hoặc tin không tồn tại: về danh sách tin kèm thông báo, không hiện trang lỗi
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:" + LIST_URL;
        }
        model.addAttribute("posting", detail);
        model.addAttribute("bodyClass", "jp-detail-page");
        if (detail.talentPoolVisible()) {
            model.addAttribute("suggestedCandidateCount", talentPoolService.suggestedCandidateCount(id));
            model.addAttribute("hrRecommendationCount", talentPoolService.pendingRecommendationCount(id));
        }
        return "job-postings/detail";
    }

    @PostMapping("/delete/{id}")
    // Xóa tin đủ điều kiện.
    public String deleteJobPosting(@PathVariable("id") Integer id, RedirectAttributes flash) {
        try {
            jobPostingService.deleteJobPosting(id, access.actor());
            flash.addFlashAttribute("successMessage", "Đã xóa tin tuyển dụng thành công.");
        } catch (JobPostingValidationException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + LIST_URL;
    }

    @ExceptionHandler(AccessDeniedException.class)
    // Xử lý lỗi thiếu quyền truy cập.
    public String whenAccessDenied(AccessDeniedException e, RedirectAttributes flash, HttpServletRequest request) {
        flash.addFlashAttribute("errorMessage", e.getMessage());
        if (LIST_URL.equals(request.getRequestURI())) {
            // Chính danh sách bị chặn thì về dashboard, tránh chuyển trang lặp vô hạn
            return "redirect:/dashboard";
        }
        return "redirect:" + LIST_URL;
    }

    // ----- Các hàm nhỏ -----

    // Chuẩn bị và hiển thị form tin.
    private String renderForm(Model model, JobPostingRequest form, User currentUser) {
        boolean isEdit = form.getJobPostingId() != null;
        model.addAttribute("form", form);
        model.addAttribute("isEdit", isEdit);
        // Ô "Đổi yêu cầu" chỉ có khi tạo mới (tin đã tạo không đổi được yêu cầu nguồn, CLAUDE.md §22)
        if (isEdit) {
            model.addAttribute("requisitionChoices", List.of());
        } else {
            model.addAttribute("requisitionChoices", jobPostingService.requisitionChoices(currentUser));
        }
        // Rule ngày nằm ở validator: hạn nộp sớm nhất là hôm nay
        model.addAttribute("today", validator.today());
        // Giới hạn độ dài khớp 3 nơi: maxlength trên form = validator = cột DB
        model.addAttribute("titleLimit", JobPostingValidator.TITLE_LIMIT);
        model.addAttribute("textLimit", JobPostingValidator.TEXT_LIMIT);
        model.addAttribute("salaryTextLimit", JobPostingValidator.SALARY_TEXT_LIMIT);
        model.addAttribute("locationLimit", JobPostingValidator.LOCATION_LIMIT);
        model.addAttribute("workingHoursLimit", JobPostingValidator.WORKING_HOURS_LIMIT);
        model.addAttribute("bodyClass", "jp-form-page");
        return FORM_VIEW;
    }

    // Đưa lỗi kiểm tra vào form.
    private void addErrors(BindingResult errors, JobPostingValidationException e) {
        if (e.getFieldErrors().isEmpty()) {
            errors.reject("business", e.getMessage());
            return;
        }
        e.getFieldErrors().forEach((field, message) -> {
            if (FORM_FIELDS.contains(field)) {
                errors.rejectValue(field, "invalid", message);
            } else {
                errors.reject(field, message);
            }
        });
    }

    // Tạo thông báo sau khi lưu.
    private String savedMessage(JobPostingRequest request, boolean isPublish, boolean isCreated) {
        if (!isPublish && isCreated) {
            return "Đã lưu bản nháp tin tuyển dụng.";
        }
        if (!isPublish) {
            return "Đã lưu thay đổi.";
        }
        return PUBLISHED_NOW_MESSAGE;
    }

    // Chuyển mã tin trên URL thành số.
    private static Integer parseId(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.isEmpty() || digits.length() > 9) {
            return null;
        }
        return Integer.valueOf(digits);
    }

    // Chuyển về trang chi tiết tin.
    private static String redirectToDetail(Integer id) {
        return "redirect:" + LIST_URL + "/" + id;
    }
}
