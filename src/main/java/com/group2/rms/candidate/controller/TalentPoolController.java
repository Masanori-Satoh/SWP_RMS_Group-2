package com.group2.rms.candidate.controller;

import com.group2.rms.candidate.dto.*;
import com.group2.rms.candidate.exception.*;
import com.group2.rms.candidate.service.*;
import com.group2.rms.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/talent-pool")
@RequiredArgsConstructor
public class TalentPoolController {
    private final TalentPoolService pool;
    private final RecruitmentEmailService email;

    @GetMapping("/jobs/{id}/suggestions")
    // get list potential for a job posting
    public String suggestions(@PathVariable Integer id, Model model) {
        model.addAttribute("jobPostingId", id);
        model.addAttribute("jobTitle", pool.targetJobTitle(id));
        model.addAttribute("suggestions", pool.suggestions(id));
        model.addAttribute("canSuggest", pool.canSuggest(id));
        model.addAttribute("viewerRole", pool.viewerRole());
        model.addAttribute("activeMenu", "talent-pool");
        return "talent-pool/suggestions";
    }

    @GetMapping
    // list all potential candidate
    public String list(@ModelAttribute("filter") TalentPoolSearch filter,
            @RequestParam(defaultValue = "0") int page, @RequestParam(required = false) Integer jobPostingId,
            Model model) {
        model.addAttribute("page", pool.search(filter, PageRequest.of(Math.max(0, page), 20)));
        model.addAttribute("authors", pool.authors());
        model.addAttribute("sourceJobs", pool.sourceJobs());
        model.addAttribute("jobPostingId", jobPostingId);
        model.addAttribute("jobTitle", jobPostingId == null ? null : pool.targetJobTitle(jobPostingId));
        model.addAttribute("viewerRole", pool.viewerRole());
        model.addAttribute("activeMenu", "talent-pool");
        model.addAttribute("selectionScope", pool.selectionScope());
        model.addAttribute("receivingManager", jobPostingId == null ? null : pool.receivingManager(jobPostingId));
        return "talent-pool/list";
    }

    @GetMapping("/{id}")
    // detail for a potential candidate
    public String detail(@PathVariable Integer id, @RequestParam(required = false) Integer jobPostingId,
            @RequestParam(required = false) Integer recommendationId,
            @ModelAttribute("filter") TalentPoolSearch filter,
            @RequestParam(defaultValue = "0") int page, Model model) {
        TalentPoolRecommendationResponse recommendation = recommendationId == null ? null : pool.recommendation(recommendationId, id);
        if (recommendation != null) jobPostingId = recommendation.jobPostingId();
        model.addAttribute("recommendation", recommendation);
        model.addAttribute("detail", recommendation == null ? pool.detail(id) : pool.recommendationDetail(id, recommendationId));
        model.addAttribute("jobPostingId", jobPostingId);
        model.addAttribute("jobTitle", jobPostingId == null ? null : pool.targetJobTitle(jobPostingId));
        model.addAttribute("viewerRole", pool.viewerRole());
        model.addAttribute("activeMenu", "talent-pool");
        model.addAttribute("sourcePage", Math.max(0, page));
        model.addAttribute("backUrl", recommendation == null ? poolUrl(null, jobPostingId, filter, page)
                : recommendationListUrl(jobPostingId, filter.keyword(), page));
        return "talent-pool/detail";
    }

    @PostMapping("/applications/{id}/mark")
    // mark cadidate potential
    public String mark(@PathVariable Integer id, RedirectAttributes flash) {
        pool.markPotential(id);
        flash.addFlashAttribute("successMessage", "Đã đánh dấu ứng viên tiềm năng.");
        return "redirect:/applications/" + id;
    }

    @GetMapping(value = "/{id}/notes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ApiResponse<java.util.List<TalentPoolNoteResponse>> sharedNotes(@PathVariable Integer id) {
        return new ApiResponse<>(true, "Đã tải ghi chú chung.", pool.detail(id).notes());
    }

    @PostMapping(value = "/jobs/{id}/selection-review", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<TalentPoolSelectionReviewResponse>> reviewSelection(@PathVariable Integer id,
            @Valid @RequestBody TalentPoolSuggestRequest request, BindingResult errors) {
        if (errors.hasErrors()) return ResponseEntity.badRequest().body(new ApiResponse<>(false,
                errors.getAllErrors().getFirst().getDefaultMessage()));
        try {
            return ResponseEntity.ok(new ApiResponse<>(true, "Đã kiểm tra danh sách.", pool.reviewSelection(id, request)));
        } catch (TalentPoolException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @PostMapping("/{id}/notes")
    // add note for a potential candidate
    public String note(@PathVariable Integer id, @Valid @ModelAttribute TalentPoolNoteRequest request,
            BindingResult errors, @RequestParam(required = false) Integer jobPostingId,
            @RequestParam(required = false) Integer recommendationId,
            @ModelAttribute("filter") TalentPoolSearch filter,
            @RequestParam(defaultValue = "0") int page, RedirectAttributes flash) {
        if (recommendationId != null) pool.recommendation(recommendationId, id);
        if (errors.hasErrors()) {
            flash.addFlashAttribute("errorMessage", errors.getAllErrors().getFirst().getDefaultMessage());
            flash.addFlashAttribute("noteDraft", request.content());
        } else {
            try {
                pool.addNote(id, request);
                flash.addFlashAttribute("successMessage", "Đã thêm ghi chú.");
            } catch (TalentPoolException e) {
                flash.addFlashAttribute("errorMessage", e.getMessage());
                flash.addFlashAttribute("noteDraft", request.content());
            }
        }
        String destination = poolUrl(id, jobPostingId, filter, page);
        if (recommendationId != null) destination += (destination.contains("?") ? "&" : "?") + "recommendationId=" + recommendationId;
        return "redirect:" + destination;
    }

    @GetMapping("/jobs/{id}/recommendations")
    public String recommendations(@PathVariable Integer id, @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page, Model model) {
        var all = pool.recommendations(id);
        String query = keyword.strip().toLowerCase(java.util.Locale.ROOT);
        if (query.length() > 100) query = query.substring(0, 100);
        final String search = query;
        var filtered = all.stream().filter(r -> r.fullName().toLowerCase(java.util.Locale.ROOT).contains(search)).toList();
        int current = Math.min(Math.max(0, page), Math.max(0, (filtered.size() - 1) / 20));
        model.addAttribute("page", new PageImpl<>(filtered.subList(current * 20, Math.min(filtered.size(), (current + 1) * 20)), PageRequest.of(current, 20), filtered.size()));
        model.addAttribute("keyword", keyword.length() > 100 ? keyword.substring(0, 100) : keyword);
        model.addAttribute("pendingCount", all.stream().filter(TalentPoolRecommendationResponse::pending).count());
        model.addAttribute("canRecommend", pool.canRecommend(id));
        model.addAttribute("jobPostingId", id);
        model.addAttribute("jobTitle", pool.targetJobTitle(id));
        model.addAttribute("viewerRole", pool.viewerRole());
        model.addAttribute("activeMenu", "talent-pool");
        return "talent-pool/recommendations";
    }

    @PostMapping("/jobs/{jobId}/recommendations/{id}/withdraw")
    public String withdrawRecommendation(@PathVariable Integer jobId, @PathVariable Integer id,
            @RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "0") int page,
            RedirectAttributes flash) {
        try {
            pool.withdrawRecommendation(jobId, id);
            flash.addFlashAttribute("successMessage", "Đã xóa gợi ý gửi cho HM. CV và ghi chú của ứng viên được giữ nguyên.");
        } catch (TalentPoolException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + recommendationListUrl(jobId, keyword, page);
    }

    private static String recommendationListUrl(Integer jobId, String keyword, int page) {
        return UriComponentsBuilder.fromPath("/talent-pool/jobs/" + jobId + "/recommendations")
                .queryParamIfPresent("keyword", Optional.ofNullable(keyword).filter(k -> !k.isBlank()))
                .queryParam("page", Math.max(0, page)).build().encode().toUriString();
    }

    @PostMapping("/recommendations/{id}/decision")
    public String decideRecommendation(@PathVariable Integer id,
            @Valid @ModelAttribute TalentPoolRecommendationDecisionRequest request, BindingResult errors,
            @RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "0") int page,
            RedirectAttributes flash) {
        var recommendation = pool.recommendationForDecision(id);
        if (errors.hasErrors()) flash.addFlashAttribute("errorMessage", "Quyết định không hợp lệ.");
        else try {
            pool.decideRecommendation(id, "Pass".equals(request.decision()));
            flash.addFlashAttribute("successMessage", "Pass".equals(request.decision())
                    ? "Đã chấp nhận gợi ý. Chờ HR liên hệ và xác nhận ứng viên đồng ý."
                    : "Đã từ chối gợi ý. Hồ sơ, CV và ghi chú của ứng viên được giữ nguyên.");
        } catch (TalentPoolException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + recommendationListUrl(recommendation.jobPostingId(), keyword, page);
    }

    @PostMapping(value = "/jobs/{id}/recommend", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<TalentPoolSuggestResponse>> recommend(@PathVariable Integer id,
            @Valid @RequestBody TalentPoolSuggestRequest request, BindingResult errors) {
        if (errors.hasErrors()) return ResponseEntity.badRequest().body(new ApiResponse<>(false, errors.getAllErrors().getFirst().getDefaultMessage()));
        try {
            pool.recommend(id, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Đã gửi gợi ý cho HM.", new TalentPoolSuggestResponse(id, request.sourceApplicationIds().size())));
        } catch (TalentPoolException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /** Only typed search context is accepted; the destination path cannot be supplied by the client. */
    private static String poolUrl(Integer candidateId, Integer jobPostingId, TalentPoolSearch filter, int page) {
        var url = UriComponentsBuilder.fromPath(candidateId == null ? "/talent-pool" : "/talent-pool/" + candidateId)
                .queryParamIfPresent("jobPostingId", Optional.ofNullable(jobPostingId))
                .queryParamIfPresent("markedBy", Optional.ofNullable(filter.markedBy()))
                .queryParamIfPresent("sourceJobId", Optional.ofNullable(filter.sourceJobId()));
        if (filter.keyword() != null && !filter.keyword().isBlank()) url.queryParam("keyword", filter.keyword());
        if (page > 0) url.queryParam("page", page);
        return url.build().encode().toUriString();
    }

    @GetMapping(value = "/applications/{id}/bookmark", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TalentPoolBookmarkResponse bookmark(@PathVariable Integer id) {
        return pool.bookmark(id);
    }

    @PostMapping(value = "/applications/{id}/bookmark", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> saveBookmark(@PathVariable Integer id,
            @Valid @ModelAttribute TalentPoolBookmarkRequest request, BindingResult errors) {
        if (errors.hasErrors()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message",
                    errors.getAllErrors().getFirst().getDefaultMessage()));
        }
        try {
            return ResponseEntity.ok(pool.saveBookmark(id, request));
        } catch (TalentPoolException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PostMapping(value = "/applications/{id}/bookmark/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TalentPoolBookmarkResponse removeBookmark(@PathVariable Integer id) {
        return pool.removeBookmark(id);
    }

    @PostMapping(value = "/jobs/{id}/suggest", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<TalentPoolSuggestResponse>> suggest(@PathVariable Integer id,
            @Valid @RequestBody TalentPoolSuggestRequest request, BindingResult errors) {
        if (errors.hasErrors()) return ResponseEntity.badRequest().body(new ApiResponse<>(false,
                errors.getAllErrors().getFirst().getDefaultMessage()));
        try {
            pool.suggest(id, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Đã thêm " + request.sourceApplicationIds().size()
                    + " ứng viên vào danh sách HM đề xuất. HR sẽ liên hệ xác nhận.",
                    new TalentPoolSuggestResponse(id, request.sourceApplicationIds().size())));
        } catch (TalentPoolException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @PostMapping("/applications/{id}/confirm")
    // confirm a candidate
    public String confirm(@PathVariable Integer id, @Valid @ModelAttribute TalentPoolConfirmRequest request,
            BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("errorMessage", errors.getAllErrors().getFirst().getDefaultMessage());
        } else {
            try {
                pool.confirm(id, request);
                flash.addFlashAttribute("successMessage",
                        "Đã xác nhận ứng tuyển bằng CV cũ. Có thể lên lịch phỏng vấn.");
                return "redirect:/interviews/new?applicationId=" + id;
            } catch (TalentPoolException e) {
                flash.addFlashAttribute("errorMessage", e.getMessage());
            }
        }
        return "redirect:/applications/" + id;
    }

    @PostMapping("/applications/{id}/decline")
    // decline a candidate
    public String decline(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            Integer jobId = pool.decline(id);
            flash.addFlashAttribute("successMessage", "Đã kết thúc đề xuất; lịch sử được giữ lại.");
            return "redirect:/internal/job-postings/" + jobId;
        } catch (TalentPoolException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/applications/" + id;
    }

    @PostMapping("/jobs/{id}/close")
    // close a job posting
    public String close(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            pool.closeJob(id);
            flash.addFlashAttribute("successMessage", "Đã đóng đợt tuyển và xử lý các hồ sơ còn lại.");
            try {
                email.sendJobClosure(id);
            } catch (RecruitmentEmailException e) {
                flash.addFlashAttribute("errorMessage", e.getMessage());
            }
        } catch (TalentPoolException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/internal/job-postings/" + id;
    }

    @PostMapping("/applications/{id}/resend-email")
    // resend email to a candidate close job posting
    public String resendApplication(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            email.resendClosure(id);
            flash.addFlashAttribute("successMessage", "Đã gửi lại email.");
        } catch (RecruitmentEmailException | TalentPoolException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/applications/" + id;
    }

    @PostMapping("/interviews/{id}/resend-email")
    // resend email to a candidate interview
    public String resendInterview(@PathVariable Long id, RedirectAttributes flash) {
        try {
            email.resendInterview(id);
            flash.addFlashAttribute("successMessage", "Đã gửi lại thư mời phỏng vấn.");
        } catch (RecruitmentEmailException | TalentPoolException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/interviews/" + id + "/result";
    }

    @GetMapping("/{id}/cv/{appId}")
    // view preview CV
    public ResponseEntity<Resource> cv(@PathVariable Integer id,
            @PathVariable Integer appId, @RequestParam(required = false) Integer recommendationId) {
        ApplicationCv cv = recommendationId == null ? pool.cv(id, appId) : pool.cv(id, appId, recommendationId);
        if (cv.redirectUrl() != null)
            return ResponseEntity.status(302).location(URI.create(cv.redirectUrl()))
                    .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline").body(cv.file());
    }
}
