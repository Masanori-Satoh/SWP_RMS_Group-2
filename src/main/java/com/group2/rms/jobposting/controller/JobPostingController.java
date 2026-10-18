package com.group2.rms.jobposting.controller;

import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.dto.JobPostingSearchRequest;
import com.group2.rms.core.util.FilterLinks;
import jakarta.servlet.http.HttpServletRequest;
import com.group2.rms.jobposting.service.JobPostingService;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.jobposting.validator.JobPostingValidator;
import com.group2.rms.candidate.service.RecruitmentEmailService;
import com.group2.rms.candidate.exception.RecruitmentEmailException;
import com.group2.rms.candidate.exception.TalentPoolException;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/internal/job-postings")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;
    private final RequisitionAccess requisitionAccess;
    private final RecruitmentEmailService recruitmentEmailService;
    private final com.group2.rms.candidate.service.TalentPoolService talentPoolService;

    @ModelAttribute
    void populateViewerAttributes(Model model) {
        User currentUser = requisitionAccess.actor();
        model.addAttribute("viewerRole", requisitionAccess.role(currentUser));
        model.addAttribute("viewerName", currentUser.getFullName());
        model.addAttribute("activeMenu", "job-postings");
    }

    /**
     * Màn danh sách. Mọi tham số trên URL (?q=...&status=...&page=...) được Spring gom vào JobPostingSearchRequest;
     * service lo phân quyền, làm sạch bộ lọc, truy vấn và đếm hồ sơ.
     * BindingResult để giá trị sai định dạng (ví dụ ?page=abc) chỉ bị bỏ qua thay vì làm lỗi cả trang.
     */
    @GetMapping
    public String listJobPostings(@ModelAttribute("searchRequest") JobPostingSearchRequest searchRequest,
            BindingResult invalidParams, HttpServletRequest request, Model model) {
        model.addAttribute("list", jobPostingService.getListPage(searchRequest, requisitionAccess.actor()));
        // Link "bỏ một bộ lọc" cho các chip: giữ nguyên các tham số khác trên URL (dùng chung với màn yêu cầu)
        model.addAttribute("filterLinks", new FilterLinks(request.getRequestURI(), request.getQueryString()));
        model.addAttribute("bodyClass", "jp-list-page");
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

    /**
     * Chỉ nhận từ form các trường HR được nhập (rule §24.3). Thông tin yêu cầu nguồn, trạng thái, ngày đăng,
     * người tạo... không bao giờ lấy từ form: service điền lại từ DB.
     */
    @InitBinder("postingDto")
    void formBinder(WebDataBinder binder) {
        binder.setAllowedFields("jobPostingId", "requisitionId", "postingTitle", "jobDescription", "jobRequirements",
                "benefits", "workLocation", "salaryMode", "salaryCustom", "showPositions", "numberOfPositionsDisplay",
                "applicationDeadline", "publishMode", "scheduledAt", "action");
    }

    /**
     * Form tạo tin, luôn mở từ một yêu cầu đã duyệt (requisitionId). Thiếu requisitionId thì service báo lỗi.
     * Ô "Đổi yêu cầu" trên form gọi lại đúng URL này với requisitionId khác.
     */
    @GetMapping("/create")
    public String showCreateForm(@RequestParam(value = "requisitionId", required = false) Integer requisitionId,
            @RequestParam(value = "fromPostingId", required = false) Integer fromPostingId,
            Model model, RedirectAttributes flash) {
        User currentUser = requisitionAccess.actor();
        try {
            // fromPostingId: "Đăng lại" một tin đã đóng (nội dung lấy từ tin đó)
            JobPostingCreateRequest dto = fromPostingId != null
                    ? jobPostingService.prepareRepostForm(fromPostingId, currentUser)
                    : jobPostingService.prepareCreateForm(requisitionId, currentUser);
            return form(model, dto, currentUser);
        } catch (JobPostingValidationException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
            return fromPostingId != null ? "redirect:/internal/job-postings/" + fromPostingId : "redirect:/internal/job-postings";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        User currentUser = requisitionAccess.actor();
        return form(model, jobPostingService.prepareEditForm(id, currentUser), currentUser);
    }

    /** Lưu nháp (action=draft) hoặc đăng / lên lịch đăng (action=publish) tin mới. */
    @PostMapping("/create")
    public String createJobPosting(@ModelAttribute("postingDto") JobPostingCreateRequest request,
            BindingResult errors, Model model, RedirectAttributes flash) {
        User currentUser = requisitionAccess.actor();
        if (!errors.hasErrors()) {
            try {
                boolean publish = "publish".equals(request.getAction());
                Integer id = publish
                        ? jobPostingService.createAndPublishJobPosting(request, currentUser)
                        : jobPostingService.createJobPosting(request, currentUser);
                flash.addFlashAttribute("successMessage", savedMessage(request, publish, true));
                return "redirect:/internal/job-postings/" + id;
            } catch (JobPostingValidationException ex) {
                addErrors(errors, ex);
            }
        }
        return form(model, jobPostingService.restoreSourceInfo(request, currentUser), currentUser);
    }

    /** Lưu thay đổi (action=draft) hoặc đăng / lên lịch đăng (action=publish) tin đã có. */
    @PostMapping("/{id}/edit")
    public String updateJobPosting(@PathVariable Integer id, @ModelAttribute("postingDto") JobPostingCreateRequest request,
            BindingResult errors, Model model, RedirectAttributes flash) {
        User currentUser = requisitionAccess.actor();
        if (!errors.hasErrors()) {
            try {
                boolean publish = "publish".equals(request.getAction());
                if (publish) jobPostingService.updateAndPublishJobPosting(id, request, currentUser);
                else jobPostingService.updateJobPosting(id, request, currentUser);
                flash.addFlashAttribute("successMessage", savedMessage(request, publish, false));
                return "redirect:/internal/job-postings/" + id;
            } catch (JobPostingValidationException ex) {
                addErrors(errors, ex);
            }
        }
        return form(model, jobPostingService.restoreSourceInfo(request, currentUser), currentUser);
    }

    /** Mọi đường hiển thị form (tạo, sửa, hiện lại khi lỗi) đều đi qua đây. */
    private String form(Model model, JobPostingCreateRequest dto, User currentUser) {
        model.addAttribute("postingDto", dto);
        model.addAttribute("isEdit", dto.getJobPostingId() != null);
        // Ô "Đổi yêu cầu" chỉ có khi tạo mới (tin đã tạo không đổi được yêu cầu nguồn, rule §24.3)
        model.addAttribute("requisitionChoices", dto.getJobPostingId() == null
                ? jobPostingService.requisitionChoices(dto.getRequisitionId(), currentUser) : List.of());
        model.addAttribute("benefitSuggestions", BENEFIT_SUGGESTIONS);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("maxScheduleDays", JobPostingValidator.MAX_SCHEDULE_DAYS);
        model.addAttribute("bodyClass", "jp-form-page");
        return "job-postings/create";
    }

    /** Đưa lỗi theo ô về cạnh từng ô; lỗi chung hiện ở đầu form. */
    private void addErrors(BindingResult errors, JobPostingValidationException ex) {
        if (ex.getFieldErrors().isEmpty()) {
            errors.reject("business", ex.getMessage());
            return;
        }
        ex.getFieldErrors().forEach((field, message) -> {
            if (FORM_FIELDS.contains(field)) {
                errors.rejectValue(field, "invalid", message);
            } else {
                errors.reject(field, message);
            }
        });
    }

    private String savedMessage(JobPostingCreateRequest request, boolean publish, boolean created) {
        if (!publish) {
            return created ? "Đã lưu bản nháp tin tuyển dụng." : "Đã lưu thay đổi.";
        }
        if (JobPostingCreateRequest.PUBLISH_LATER.equals(request.getPublishMode()) && request.getScheduledAt() != null) {
            return "Đã lên lịch đăng tin lúc " + request.getScheduledAt().format(SCHEDULE_FORMAT) + ".";
        }
        return "Đã đăng tin. Ứng viên có thể nộp hồ sơ ngay.";
    }

    /** Các ô có trên form (lỗi của ô khác, ví dụ "action", hiện ở đầu form). */
    private static final java.util.Set<String> FORM_FIELDS = java.util.Set.of("postingTitle", "jobDescription",
            "jobRequirements", "benefits", "workLocation", "salaryMode", "salaryCustom", "numberOfPositionsDisplay",
            "applicationDeadline", "publishMode", "scheduledAt");
    private static final java.time.format.DateTimeFormatter SCHEDULE_FORMAT =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
    /** Gợi ý quyền lợi trên form (nút thêm nhanh), theo mockup. */
    private static final List<String> BENEFIT_SUGGESTIONS = List.of("Lương tháng 13", "Thưởng theo hiệu quả công việc",
            "Đóng BHXH, BHYT, BHTN đầy đủ", "Bảo hiểm sức khỏe", "Review lương 2 lần/năm", "12 ngày phép năm",
            "Cấp laptop làm việc", "Du lịch công ty hằng năm");

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

    /** Tin đang hẹn đăng: đăng ngay bây giờ. */
    @PostMapping("/{id}/publish-now")
    public String publishNow(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            jobPostingService.publishNow(id, requisitionAccess.actor());
            flash.addFlashAttribute("successMessage", "Đã đăng tin. Ứng viên có thể nộp hồ sơ ngay.");
        } catch (JobPostingValidationException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/internal/job-postings/" + id;
    }

    /** Tin đang hẹn đăng: hủy lịch, trở về bản nháp. */
    @PostMapping("/{id}/unschedule")
    public String unschedule(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            jobPostingService.unschedule(id, requisitionAccess.actor());
            flash.addFlashAttribute("successMessage", "Đã hủy lịch đăng. Tin trở về bản nháp.");
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
        model.addAttribute("bodyClass", "jp-detail-page");
        if (List.of("HR", "Hiring Manager").contains(requisitionAccess.role(currentUser)))
            model.addAttribute("hrRecommendationCount", talentPoolService.pendingRecommendationCount(id));
        return "job-postings/detail";
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
