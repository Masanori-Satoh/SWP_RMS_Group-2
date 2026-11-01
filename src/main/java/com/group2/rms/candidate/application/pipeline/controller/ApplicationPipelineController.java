package com.group2.rms.candidate.application.pipeline.controller;

import com.group2.rms.candidate.application.pipeline.dto.ApplicationDetailResponse;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationListResponse;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationReviewRequest;
import com.group2.rms.candidate.application.pipeline.dto.ApplicationSearch;
import com.group2.rms.candidate.application.pipeline.exception.ApplicationReviewException;
import com.group2.rms.candidate.application.pipeline.service.ApplicationPipelineService;
import com.group2.rms.candidate.application.pipeline.service.ApplicationReviewService;
import com.group2.rms.candidate.talentpool.service.TalentPoolService;
import com.group2.rms.candidate.domain.rule.ApplicationStage;
import com.group2.rms.core.web.StageItem;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Hồ sơ ứng tuyển cho người tuyển dụng: danh sách (5.1.22), chi tiết, CV và duyệt (5.1.23). Quyền theo vai trò ở {@code SecurityConfig},
 * phạm vi từng người (HM chỉ thấy phòng ban mình) ở {@code ApplicationAccess}.
 */
@Controller
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationPipelineController {

    static final int PAGE_SIZE = 20;

    private final ApplicationPipelineService pipeline;
    private final ApplicationReviewService reviewService;
    private final TalentPoolService talentPool;

    @GetMapping
    public String list(@ModelAttribute ApplicationSearch search,
                       @PageableDefault(size = PAGE_SIZE) Pageable pageable, Model model) {
        ApplicationListResponse result = pipeline.list(search, pageable);
        model.addAttribute("result", result);
        model.addAttribute("stageItems", stageItems(result));
        model.addAttribute("activeMenu", "applications");
        model.addAttribute("viewerRole", result.viewerRole());
        return "candidate/applications";
    }

    /**
     * Các ô của thanh giai đoạn: "Tất cả", các giai đoạn trong luồng, "Đã dừng". Link giữ bộ lọc đang có, chỉ đổi
     * giai đoạn và quay về trang đầu.
     */
    static List<StageItem> stageItems(ApplicationListResponse result) {
        ApplicationSearch search = result.search();
        ApplicationStage selected = search.stageFilter();
        List<StageItem> items = new ArrayList<>();
        items.add(new StageItem("ALL", "Tất cả", Math.toIntExact(result.totalCount()), stageHref(search, null),
                selected == null ? "current" : "upcoming", null, true));
        for (ApplicationStage stage : ApplicationStage.values()) {
            items.add(new StageItem(stage.name(), stage.label(), Math.toIntExact(result.stageCounts().get(stage)),
                    stageHref(search, stage), stage == selected ? "current" : "upcoming", null,
                    stage == ApplicationStage.CLOSED));
        }
        return items;
    }

    private static String stageHref(ApplicationSearch search, ApplicationStage stage) {
        return UriComponentsBuilder.fromPath("/applications")
                .queryParamIfPresent("jobPostingId", Optional.ofNullable(search.jobPostingId()))
                .queryParamIfPresent("keyword", Optional.ofNullable(search.keyword()))
                .queryParamIfPresent("status", Optional.ofNullable(search.status()))
                .queryParamIfPresent("stage", Optional.ofNullable(stage).map(Enum::name))
                .encode()
                .toUriString();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        String recommendationUrl = talentPool.pendingRecommendationUrl(id);
        if (recommendationUrl != null) return "redirect:" + recommendationUrl;
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

    private String renderDetail(Integer id, Model model) {
        ApplicationDetailResponse detail = pipeline.detail(id);
        model.addAttribute("detail", detail);
        // Keep shared note history available to authorized reviewers after removing a bookmark.
        model.addAttribute("talentNotes", Set.of("HR", "Hiring Manager").contains(detail.viewerRole())
                ? talentPool.notesFor(detail.candidateId()) : List.of());
        model.addAttribute("closedByPosting", pipeline.closedByPosting(id));
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
        return pipeline.cv(id).toResponse("CV-" + id + ".pdf");
    }
}
