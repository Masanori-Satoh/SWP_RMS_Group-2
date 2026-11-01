package com.group2.rms.jobposting.controller;

import com.group2.rms.candidate.talentpool.exception.RecruitmentEmailException;
import com.group2.rms.candidate.talentpool.exception.TalentPoolException;
import com.group2.rms.candidate.talentpool.service.RecruitmentEmailService;
import com.group2.rms.candidate.talentpool.service.TalentPoolService;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
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
    private static final String PUBLISHED_NOW_MESSAGE = "Đã đăng tin. Ứng viên có thể nộp hồ sơ ngay.";
    /** Các ô có trên form (lỗi của ô khác, ví dụ "action", hiện ở đầu form). */
    private static final Set<String> FORM_FIELDS = Set.of("postingTitle", "jobDescription", "jobRequirements",
            "benefits", "workLocation", "salaryMode", "salaryCustom", "numberOfPositionsDisplay",
            "applicationDeadline");
    /** Gợi ý quyền lợi trên form (nút thêm nhanh), theo mockup. */
    private static final List<String> BENEFIT_SUGGESTIONS = List.of("Lương tháng 13", "Thưởng theo hiệu quả công việc",
            "Đóng BHXH, BHYT, BHTN đầy đủ", "Bảo hiểm sức khỏe", "Review lương 2 lần/năm", "12 ngày phép năm",
            "Cấp laptop làm việc", "Du lịch công ty hằng năm");

    private final JobPostingService jobPostingService;
    private final JobPostingListService listService;
    private final JobPostingAccess access;
    private final JobPostingValidator validator;
    private final RecruitmentEmailService recruitmentEmailService;
    private final TalentPoolService talentPoolService;

    /** Thông tin người xem cho trang dashboard (khung cũ tự dựng sidebar) và menu đang sáng. */
    @ModelAttribute
    void populateViewerAttributes(Model model) {
        User currentUser = access.actor();
        model.addAttribute("viewerRole", access.role(currentUser));
        model.addAttribute("viewerName", currentUser.getFullName());
        model.addAttribute("activeMenu", "job-postings");
    }

    /** Chỉ nhận các ô HR được nhập (CLAUDE.md §22); thông tin nguồn, trạng thái do service lấy từ DB. */
    @InitBinder("form")
    void formBinder(WebDataBinder binder) {
        binder.setAllowedFields("jobPostingId", "requisitionId", "postingTitle", "jobDescription", "jobRequirements",
                "benefits", "workLocation", "salaryMode", "salaryCustom", "showPositions", "numberOfPositionsDisplay",
                "applicationDeadline", "action");
    }

    /** Màn danh sách; tham số sai định dạng trên URL (?page=abc) chỉ bị bỏ qua, không lỗi trang. */
    @GetMapping
    public String listJobPostings(@ModelAttribute("searchRequest") JobPostingSearchRequest searchRequest,
            BindingResult invalidParams, HttpServletRequest request, Model model) {
        model.addAttribute("list", listService.getListPage(searchRequest, access.actor()));
        // Link "bỏ một bộ lọc" cho các chip: giữ nguyên các tham số khác trên URL (dùng chung với màn yêu cầu)
        model.addAttribute("filterLinks", new FilterLinks(request.getRequestURI(), request.getQueryString()));
        model.addAttribute("bodyClass", "jp-list-page");
        return "job-postings/list";
    }

    /** Form tạo tin từ yêu cầu đã duyệt; fromPostingId = "Đăng lại" từ một tin đã đóng. */
    @GetMapping("/create")
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
        }
    }

    /** id nhận dạng chuỗi để URL gõ tay như "{1689}" vẫn hiểu là tin 1689 (không ra lỗi 500). */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") String idParam, Model model) {
        User currentUser = access.actor();
        return renderForm(model, jobPostingService.prepareEditForm(parseId(idParam), currentUser), currentUser);
    }

    /** Lưu nháp (action=draft) hoặc đăng ngay (action=publish) tin mới. */
    @PostMapping("/create")
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

    /** Lưu thay đổi (action=draft) hoặc đăng / mở lại (action=publish) tin đã có. */
    @PostMapping("/{id}/edit")
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
    public String publishJobPosting(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            jobPostingService.publishJobPosting(id, access.actor());
            flash.addFlashAttribute("successMessage", "Đã đăng tin tuyển dụng.");
        } catch (JobPostingValidationException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return redirectToDetail(id);
    }

    @PostMapping("/{id}/pause")
    public String pauseJobPosting(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            jobPostingService.pauseJobPosting(id, access.actor());
            flash.addFlashAttribute("successMessage", "Đã tạm dừng tin tuyển dụng.");
        } catch (JobPostingValidationException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return redirectToDetail(id);
    }

    /** Đóng tin (chỉ HR), rồi gửi email báo kết quả cho ứng viên; gửi mail lỗi thì vẫn đóng, chỉ báo lỗi. */
    @PostMapping("/{id}/close")
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
    public String showJobPostingDetail(@PathVariable("id") String idParam, Model model) {
        Integer id = parseId(idParam);
        InternalJobPostingDetailResponse detail = jobPostingService.getInternalJobPostingDetail(id, access.actor());
        model.addAttribute("posting", detail);
        model.addAttribute("bodyClass", "jp-detail-page");
        if (detail.talentPoolVisible()) {
            model.addAttribute("hrRecommendationCount", talentPoolService.pendingRecommendationCount(id));
        }
        return "job-postings/detail";
    }

    @PostMapping("/delete/{id}")
    public String deleteJobPosting(@PathVariable("id") Integer id, RedirectAttributes flash) {
        try {
            jobPostingService.deleteJobPosting(id, access.actor());
            flash.addFlashAttribute("successMessage", "Đã xóa tin tuyển dụng thành công.");
        } catch (JobPostingValidationException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + LIST_URL;
    }

    // ----- Các hàm nhỏ -----

    /** Mọi đường hiển thị form (tạo, sửa, đăng lại, hiện lại khi lỗi) đều đi qua đây. */
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
        model.addAttribute("benefitSuggestions", BENEFIT_SUGGESTIONS);
        // Rule ngày nằm ở validator: hạn nộp sớm nhất là hôm nay
        model.addAttribute("today", validator.today());
        // Giới hạn độ dài khớp 3 nơi: maxlength trên form = validator = cột DB
        model.addAttribute("titleLimit", JobPostingValidator.TITLE_LIMIT);
        model.addAttribute("textLimit", JobPostingValidator.TEXT_LIMIT);
        model.addAttribute("salaryTextLimit", JobPostingValidator.SALARY_TEXT_LIMIT);
        model.addAttribute("positionsTextLimit", JobPostingValidator.POSITIONS_TEXT_LIMIT);
        model.addAttribute("locationLimit", JobPostingValidator.LOCATION_LIMIT);
        model.addAttribute("bodyClass", "jp-form-page");
        return FORM_VIEW;
    }

    /** Đưa lỗi theo ô về cạnh từng ô; lỗi chung hiện ở đầu form. */
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

    private String savedMessage(JobPostingRequest request, boolean isPublish, boolean isCreated) {
        if (!isPublish && isCreated) {
            return "Đã lưu bản nháp tin tuyển dụng.";
        }
        if (!isPublish) {
            return "Đã lưu thay đổi.";
        }
        return PUBLISHED_NOW_MESSAGE;
    }

    // Lấy phần số của mã gõ trên URL (vd "{1753}" -> 1753); không có số thì trả null để service báo lỗi
    private static Integer parseId(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.isEmpty() || digits.length() > 9) {
            return null;
        }
        return Integer.valueOf(digits);
    }

    private static String redirectToDetail(Integer id) {
        return "redirect:" + LIST_URL + "/" + id;
    }
}
