package com.group2.rms.jobposting.controller;

import com.group2.rms.candidate.exception.RecruitmentEmailException;
import com.group2.rms.candidate.exception.TalentPoolException;
import com.group2.rms.candidate.service.RecruitmentEmailService;
import com.group2.rms.candidate.service.TalentPoolService;
import com.group2.rms.core.util.FilterLinks;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.dto.JobPostingDashboardResponse;
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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * Màn tin tuyển dụng nội bộ (/internal/job-postings): danh sách, bảng điều khiển HR, form tạo / sửa, trang chi tiết và các nút chuyển trạng thái.
 */
@Controller
@RequestMapping("/internal/job-postings")
@RequiredArgsConstructor
public class JobPostingController {

    private static final String LIST_URL = "/internal/job-postings";
    private static final String FORM_VIEW = "job-postings/create";
    private static final String PUBLISHED_NOW_MESSAGE = "Đã đăng tin. Ứng viên có thể nộp hồ sơ ngay.";
    private static final DateTimeFormatter SCHEDULE_FORMAT = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
    /** Các ô có trên form (lỗi của ô khác, ví dụ "action", hiện ở đầu form). */
    private static final Set<String> FORM_FIELDS = Set.of("postingTitle", "jobDescription", "jobRequirements",
            "benefits", "workLocation", "salaryMode", "salaryCustom", "numberOfPositionsDisplay",
            "applicationDeadline", "publishMode", "scheduledAt");
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

    /**
     * Chỉ nhận từ form các trường HR được nhập. Thông tin yêu cầu nguồn, trạng thái, ngày đăng,
     * người tạo... không bao giờ lấy từ form: service điền lại từ DB.
     */
    @InitBinder("postingDto")
    void formBinder(WebDataBinder binder) {
        binder.setAllowedFields("jobPostingId", "requisitionId", "postingTitle", "jobDescription", "jobRequirements",
                "benefits", "workLocation", "salaryMode", "salaryCustom", "showPositions", "numberOfPositionsDisplay",
                "applicationDeadline", "publishMode", "scheduledAt", "action");
    }

    /**
     * Màn danh sách. Mọi tham số trên URL (?q=...&status=...&page=...) được Spring gom vào JobPostingSearchRequest;
     * list service lo phân quyền, làm sạch bộ lọc, truy vấn và đếm hồ sơ.
     * BindingResult để giá trị sai định dạng (ví dụ ?page=abc) chỉ bị bỏ qua thay vì làm lỗi cả trang.
     */
    @GetMapping
    public String listJobPostings(@ModelAttribute("searchRequest") JobPostingSearchRequest searchRequest,
            BindingResult invalidParams, HttpServletRequest request, Model model) {
        model.addAttribute("list", listService.getListPage(searchRequest, access.actor()));
        // Link "bỏ một bộ lọc" cho các chip: giữ nguyên các tham số khác trên URL (dùng chung với màn yêu cầu)
        model.addAttribute("filterLinks", new FilterLinks(request.getRequestURI(), request.getQueryString()));
        model.addAttribute("bodyClass", "jp-list-page");
        return "job-postings/list";
    }

    @GetMapping("/dashboard")
    public String hrDashboard(Model model) {
        JobPostingDashboardResponse dashboard = jobPostingService.getDashboard(access.actor());
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
     * Form tạo tin, luôn mở từ một yêu cầu đã duyệt (requisitionId). Ô "Đổi yêu cầu" gọi lại URL này với
     * requisitionId khác. fromPostingId: "Đăng lại" một tin đã đóng (nội dung lấy từ tin đó).
     */
    @GetMapping("/create")
    public String showCreateForm(@RequestParam(value = "requisitionId", required = false) Integer requisitionId,
            @RequestParam(value = "fromPostingId", required = false) Integer fromPostingId,
            Model model, RedirectAttributes flash) {
        User currentUser = access.actor();
        try {
            JobPostingCreateRequest dto;
            if (fromPostingId != null) {
                dto = jobPostingService.prepareRepostForm(fromPostingId, currentUser);
            } else {
                dto = jobPostingService.prepareCreateForm(requisitionId, currentUser);
            }
            return form(model, dto, currentUser);
        } catch (JobPostingValidationException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            if (fromPostingId != null) {
                return redirectToDetail(fromPostingId);
            }
            return "redirect:" + LIST_URL;
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        User currentUser = access.actor();
        return form(model, jobPostingService.prepareEditForm(id, currentUser), currentUser);
    }

    /** Lưu nháp (action=draft) hoặc đăng / lên lịch đăng (action=publish) tin mới. */
    @PostMapping("/create")
    public String createJobPosting(@Valid @ModelAttribute("postingDto") JobPostingCreateRequest request,
            BindingResult errors, Model model, RedirectAttributes flash) {
        User currentUser = access.actor();
        if (!errors.hasErrors()) {
            try {
                boolean isPublish = JobPostingValidator.isPublish(request.getAction());
                Integer id;
                if (isPublish) {
                    id = jobPostingService.createAndPublishJobPosting(request, currentUser);
                } else {
                    id = jobPostingService.createJobPosting(request, currentUser);
                }
                flash.addFlashAttribute("successMessage", savedMessage(request, isPublish, true));
                return redirectToDetail(id);
            } catch (JobPostingValidationException e) {
                addErrors(errors, e);
            }
        }
        return form(model, jobPostingService.restoreSourceInfo(request, currentUser), currentUser);
    }

    /** Lưu thay đổi (action=draft) hoặc đăng / lên lịch đăng / mở lại (action=publish) tin đã có. */
    @PostMapping("/{id}/edit")
    public String updateJobPosting(@PathVariable Integer id,
            @Valid @ModelAttribute("postingDto") JobPostingCreateRequest request,
            BindingResult errors, Model model, RedirectAttributes flash) {
        User currentUser = access.actor();
        if (!errors.hasErrors()) {
            try {
                boolean isPublish = JobPostingValidator.isPublish(request.getAction());
                if (isPublish) {
                    jobPostingService.updateAndPublishJobPosting(id, request, currentUser);
                } else {
                    jobPostingService.updateJobPosting(id, request, currentUser);
                }
                flash.addFlashAttribute("successMessage", savedMessage(request, isPublish, false));
                return redirectToDetail(id);
            } catch (JobPostingValidationException e) {
                addErrors(errors, e);
            }
        }
        return form(model, jobPostingService.restoreSourceInfo(request, currentUser), currentUser);
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

    /** Tin đang hẹn đăng: đăng ngay bây giờ. */
    @PostMapping("/{id}/publish-now")
    public String publishNow(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            jobPostingService.publishNow(id, access.actor());
            flash.addFlashAttribute("successMessage", PUBLISHED_NOW_MESSAGE);
        } catch (JobPostingValidationException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return redirectToDetail(id);
    }

    /** Tin đang hẹn đăng: hủy lịch, trở về bản nháp. */
    @PostMapping("/{id}/unschedule")
    public String unschedule(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            jobPostingService.unschedule(id, access.actor());
            flash.addFlashAttribute("successMessage", "Đã hủy lịch đăng. Tin trở về bản nháp.");
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
    public String showJobPostingDetail(@PathVariable("id") Integer id, Model model) {
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
    private String form(Model model, JobPostingCreateRequest dto, User currentUser) {
        boolean isEdit = dto.getJobPostingId() != null;
        model.addAttribute("postingDto", dto);
        model.addAttribute("isEdit", isEdit);
        // Ô "Đổi yêu cầu" chỉ có khi tạo mới (tin đã tạo không đổi được yêu cầu nguồn)
        if (isEdit) {
            model.addAttribute("requisitionChoices", List.of());
        } else {
            model.addAttribute("requisitionChoices", jobPostingService.requisitionChoices(currentUser));
        }
        model.addAttribute("benefitSuggestions", BENEFIT_SUGGESTIONS);
        // Rule ngày nằm ở validator: hạn nộp sớm nhất là hôm nay
        model.addAttribute("today", validator.today());
        model.addAttribute("maxScheduleDays", JobPostingValidator.MAX_SCHEDULE_DAYS);
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

    private String savedMessage(JobPostingCreateRequest request, boolean isPublish, boolean isCreated) {
        if (!isPublish && isCreated) {
            return "Đã lưu bản nháp tin tuyển dụng.";
        }
        if (!isPublish) {
            return "Đã lưu thay đổi.";
        }
        boolean isLater = JobPostingCreateRequest.PUBLISH_LATER.equals(request.getPublishMode());
        if (isLater && request.getScheduledAt() != null) {
            return "Đã lên lịch đăng tin lúc " + request.getScheduledAt().format(SCHEDULE_FORMAT) + ".";
        }
        return PUBLISHED_NOW_MESSAGE;
    }

    private static String redirectToDetail(Integer id) {
        return "redirect:" + LIST_URL + "/" + id;
    }
}
