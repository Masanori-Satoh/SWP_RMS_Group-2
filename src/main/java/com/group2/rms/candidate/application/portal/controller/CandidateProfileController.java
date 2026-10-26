package com.group2.rms.candidate.application.portal.controller;

import com.group2.rms.candidate.application.portal.dto.CandidateDetailsRequest;
import com.group2.rms.candidate.application.portal.dto.CandidateProfileResponse;
import com.group2.rms.candidate.application.portal.exception.CandidateProfileException;
import com.group2.rms.candidate.application.portal.service.CandidateProfileService;
import com.group2.rms.user.dto.UpdateProfileRequest;
import com.group2.rms.user.exception.AvatarUploadException;
import com.group2.rms.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * Hồ sơ cá nhân của ứng viên (Q7.1). {@code SecurityConfig} giới hạn {@code /portal/profile/**} cho ROLE_CANDIDATE;
 * ứng viên gõ {@code /profile} được chuyển sang đây. Một form lưu cả hai phần: {@code updateRequest} (bảng {@code User})
 * và {@code detailsRequest} (bảng {@code Candidate}). Đổi mật khẩu dùng lại {@code POST /profile/change-password}.
 */
@Controller
@RequestMapping("/portal/profile")
@RequiredArgsConstructor
public class CandidateProfileController {

    private static final String BINDING_PREFIX = "org.springframework.validation.BindingResult.";

    private final CandidateProfileService candidateProfiles;
    private final UserProfileService userProfiles;

    @GetMapping
    public String view(Authentication authentication, Model model) {
        CandidateProfileResponse profile = candidateProfiles.get(authentication.getName());
        model.addAttribute("profile", profile);
        if (!model.containsAttribute("updateRequest")) {
            model.addAttribute("updateRequest",
                    new UpdateProfileRequest(profile.account().fullName(), profile.account().phoneNumber()));
        }
        if (!model.containsAttribute("detailsRequest")) {
            model.addAttribute("detailsRequest", new CandidateDetailsRequest(profile.dateOfBirth(), profile.gender(),
                    profile.address(), profile.linkedInUrl(), profile.portfolioUrl()));
        }
        model.addAttribute("genders", CandidateDetailsRequest.genderOptions(profile.gender()));
        model.addAttribute("maxBirthDate", LocalDate.now().minusDays(1));
        model.addAttribute("activeMenu", "profile");
        return "candidate/my-profile";
    }

    @PostMapping("/update")
    public String update(Authentication authentication,
                         @Valid @ModelAttribute("updateRequest") UpdateProfileRequest account,
                         BindingResult accountErrors,
                         @Valid @ModelAttribute("detailsRequest") CandidateDetailsRequest details,
                         BindingResult detailErrors,
                         RedirectAttributes redirectAttributes) {
        if (!accountErrors.hasErrors() && !detailErrors.hasErrors()) {
            try {
                candidateProfiles.update(authentication.getName(), account, details);
                redirectAttributes.addFlashAttribute("successMessage", "Đã lưu hồ sơ.");
                return "redirect:/portal/profile";
            } catch (CandidateProfileException e) {
                detailErrors.rejectValue(e.getField(), "invalid", e.getMessage());
            }
        }
        redirectAttributes.addFlashAttribute(BINDING_PREFIX + "updateRequest", accountErrors);
        redirectAttributes.addFlashAttribute("updateRequest", account);
        redirectAttributes.addFlashAttribute(BINDING_PREFIX + "detailsRequest", detailErrors);
        redirectAttributes.addFlashAttribute("detailsRequest", details);
        return "redirect:/portal/profile";
    }

    /** Tải ảnh đại diện mới; cùng nguồn với {@code /profile/avatar} ({@code UserProfileService.changeAvatar}). */
    @PostMapping("/avatar")
    public String changeAvatar(Authentication authentication,
                               @RequestParam(name = "avatarFile", required = false) MultipartFile avatarFile,
                               RedirectAttributes redirectAttributes) {
        try {
            userProfiles.changeAvatar(authentication.getName(), avatarFile);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật ảnh đại diện.");
        } catch (AvatarUploadException e) {
            redirectAttributes.addFlashAttribute("avatarError", e.getMessage());
        }
        return "redirect:/portal/profile";
    }
}
