package com.group2.rms.candidate.controller;

import com.group2.rms.candidate.dto.ApplicationDetailResponse;
import com.group2.rms.candidate.dto.ApplicationListResponse;
import com.group2.rms.candidate.dto.ApplicationReviewRequest;
import com.group2.rms.candidate.dto.ApplicationSearch;
import com.group2.rms.candidate.exception.ApplicationReviewException;
import com.group2.rms.candidate.service.AiScreeningOutcome;
import com.group2.rms.candidate.service.ApplicationCv;
import com.group2.rms.candidate.service.ApplicationPipelineService;
import com.group2.rms.candidate.service.ApplicationReviewService;
import com.group2.rms.candidate.service.ApplicationStatusLabels;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Locale;

/**
 * Hồ sơ ứng tuyển cho người tuyển dụng: danh sách (5.1.22), chi tiết, CV, duyệt và chấm lại AI (5.1.23). Quyền theo vai trò ở {@code SecurityConfig},
 * phạm vi từng người (HM chỉ thấy phòng ban mình) ở {@code ApplicationAccess}.
 */
@Controller
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationPipelineController {

    static final int PAGE_SIZE = 20;
    private static final Locale VIETNAMESE = Locale.of("vi", "VN");

    private final ApplicationPipelineService pipeline;
    private final ApplicationReviewService reviewService;

    @GetMapping
    public String list(@ModelAttribute ApplicationSearch search,
                       @PageableDefault(size = PAGE_SIZE) Pageable pageable, Model model) {
        ApplicationListResponse result = pipeline.list(search, pageable);
        model.addAttribute("result", result);
        model.addAttribute("statusOptions", ApplicationStatusLabels.options());
        model.addAttribute("activeMenu", "applications");
        model.addAttribute("viewerRole", result.viewerRole());
        return "candidate/applications";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        if (!model.containsAttribute("reviewForm")) {
            model.addAttribute("reviewForm", new ApplicationReviewRequest(null, null));
        }
        return renderDetail(id, model);
    }

    /** Đạt / Tạm giữ / Không đạt (form truyền thống). Sai bước duyệt thì render lại trang kèm lỗi. */
    @PostMapping("/{id}/review")
    public String review(@PathVariable Integer id,
                         @Valid @ModelAttribute("reviewForm") ApplicationReviewRequest form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                redirect.addFlashAttribute("successMessage", reviewService.review(id, form));
                return "redirect:/applications/" + id;
            } catch (ApplicationReviewException e) {
                if (e.getField() != null) {
                    errors.rejectValue(e.getField(), "review.invalid", e.getMessage());
                } else {
                    errors.reject("review.invalid", e.getMessage());
                }
            }
        }
        return renderDetail(id, model);
    }

    /** HR chấm lại AI; kết quả hoặc lỗi hiện bằng thông báo trên trang chi tiết. */
    @PostMapping("/{id}/ai-screening")
    public String rescreen(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            AiScreeningOutcome outcome = reviewService.rescreen(id);
            redirect.addFlashAttribute("successMessage",
                    "Đã chấm lại AI: " + String.format(VIETNAMESE, "%.2f", outcome.matchScore()) + " điểm.");
        } catch (ApplicationReviewException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/applications/" + id;
    }

    private String renderDetail(Integer id, Model model) {
        ApplicationDetailResponse detail = pipeline.detail(id);
        model.addAttribute("detail", detail);
        model.addAttribute("activeMenu", "applications");
        model.addAttribute("viewerRole", detail.viewerRole());
        return "candidate/application-detail";
    }

    /**
     * CV hiển thị trong khung của trang chi tiết (hoặc tab mới). File lưu nội bộ trả thẳng PDF {@code inline};
     * CV mẫu / link ngoài thì chuyển hướng. Header cho phép nhúng khung cùng nguồn đặt ở {@code SecurityConfig}.
     */
    @GetMapping("/{id}/cv")
    public ResponseEntity<Resource> cv(@PathVariable Integer id) {
        ApplicationCv cv = pipeline.cv(id);
        if (cv.redirectUrl() != null) {
            URI location = cv.redirectUrl().startsWith("/")
                    ? ServletUriComponentsBuilder.fromCurrentContextPath().path(cv.redirectUrl()).build().toUri()
                    : URI.create(cv.redirectUrl());
            return ResponseEntity.status(302).location(location).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("CV-" + id + ".pdf").build().toString())
                .body(cv.file());
    }
}
