package com.group2.rms.candidate.controller;

import com.group2.rms.candidate.dto.*;
import com.group2.rms.candidate.exception.*;
import com.group2.rms.candidate.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.net.URI;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        return "talent-pool/list";
    }

    @GetMapping("/{id}")
    // detail for a potential candidate
    public String detail(@PathVariable Integer id, @RequestParam(required = false) Integer jobPostingId, Model model) {
        model.addAttribute("detail", pool.detail(id));
        model.addAttribute("jobPostingId", jobPostingId);
        model.addAttribute("jobTitle", jobPostingId == null ? null : pool.targetJobTitle(jobPostingId));
        model.addAttribute("viewerRole", pool.viewerRole());
        model.addAttribute("activeMenu", "talent-pool");
        return "talent-pool/detail";
    }

    @PostMapping("/applications/{id}/mark")
    // mark cadidate potential
    public String mark(@PathVariable Integer id, RedirectAttributes flash) {
        pool.markPotential(id);
        flash.addFlashAttribute("successMessage", "Đã đánh dấu ứng viên tiềm năng.");
        return "redirect:/applications/" + id;
    }

    @PostMapping("/{id}/notes")
    // add note for a potential candidate
    public String note(@PathVariable Integer id, @Valid @ModelAttribute TalentPoolNoteRequest request,
            BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("errorMessage", errors.getAllErrors().getFirst().getDefaultMessage());
            flash.addFlashAttribute("noteDraft", request.content());
        } else {
            try {
                pool.addNote(id, request);
                flash.addFlashAttribute("successMessage", "Đã thêm ghi chú.");
            } catch (TalentPoolException e) {
                flash.addFlashAttribute("errorMessage", e.getMessage());
            }
        }
        return "redirect:/talent-pool/" + id;
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

    @PostMapping("/jobs/{id}/suggest")
    // suggest a candidate for a job posting
    public String suggest(@PathVariable Integer id, @Valid @ModelAttribute TalentPoolSuggestRequest request,
            BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors())
            flash.addFlashAttribute("errorMessage", errors.getAllErrors().getFirst().getDefaultMessage());
        else {
            try {
                pool.suggest(id, request);
                flash.addFlashAttribute("successMessage", "Đã đề xuất ứng viên. HR sẽ liên hệ xác nhận.");
            } catch (TalentPoolException e) {
                flash.addFlashAttribute("errorMessage", e.getMessage());
            }
        }
        return "redirect:/talent-pool?jobPostingId=" + id;
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
            @PathVariable Integer appId) {
        ApplicationCv cv = pool.cv(id, appId);
        if (cv.redirectUrl() != null)
            return ResponseEntity.status(302).location(URI.create(cv.redirectUrl()))
                    .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline").body(cv.file());
    }
}
