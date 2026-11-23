package com.group2.rms.candidate.controller;

import com.group2.rms.candidate.dto.CandidateProfileResponse;
import com.group2.rms.candidate.dto.UpdateCandidateProfileRequest;
import com.group2.rms.candidate.exception.CandidateProfileException;
import com.group2.rms.candidate.service.CandidateProfileService;
import com.group2.rms.user.dto.ChangePasswordRequest;
import com.group2.rms.user.dto.UpdateProfileRequest;
import com.group2.rms.user.exception.AccountFieldException;
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
 * Controller xử lý hồ sơ cá nhân của ứng viên trên career portal.
 */
@Controller
@RequestMapping("/portal/profile")
@RequiredArgsConstructor
public class CandidateProfileController {

    private static final String BINDING_PREFIX = "org.springframework.validation.BindingResult.";

    private final CandidateProfileService candidateProfileService;
    private final UserProfileService userProfileService;

    @GetMapping
    public String view(Authentication authentication, Model model) {
        CandidateProfileResponse profile = candidateProfileService.get(authentication.getName());
        model.addAttribute("profile", profile);
        if (!model.containsAttribute("updateRequest")) {
            model.addAttribute("updateRequest",
                    new UpdateProfileRequest(profile.account().fullName(), profile.account().phoneNumber()));
        }
        if (!model.containsAttribute("detailsRequest")) {
            model.addAttribute("detailsRequest",
                    new UpdateCandidateProfileRequest(profile.dateOfBirth(), profile.gender(),
                            profile.address(), profile.linkedInUrl(), profile.portfolioUrl(),
                            profile.account().email()));
        }
        if (!model.containsAttribute("passwordRequest")) {
            model.addAttribute("passwordRequest", new ChangePasswordRequest("", "", ""));
        }
        model.addAttribute("genders", UpdateCandidateProfileRequest.genderOptions(profile.gender()));
        model.addAttribute("maxBirthDate", LocalDate.now().minusDays(1));
        model.addAttribute("activeMenu", "profile");
        model.addAttribute("vietnameseUi", true);
        return "candidate/my-profile";
    }

    @PostMapping("/update")
    public String update(Authentication authentication,
                         @Valid @ModelAttribute("updateRequest") UpdateProfileRequest account,
                         BindingResult accountErrors,
                         @Valid @ModelAttribute("detailsRequest") UpdateCandidateProfileRequest details,
                         BindingResult detailErrors,
                         RedirectAttributes redirectAttributes) {
        if (!accountErrors.hasErrors() && !detailErrors.hasErrors()) {
            try {
                candidateProfileService.update(authentication.getName(), account, details);
                redirectAttributes.addFlashAttribute("successMessage", "Đã lưu hồ sơ.");
                return "redirect:/portal/profile";
            } catch (CandidateProfileException e) {
                detailErrors.rejectValue(e.getField(), "invalid", e.getMessage());
            } catch (AccountFieldException e) {
                if ("fullName".equals(e.getField()) || "phoneNumber".equals(e.getField())) {
                    accountErrors.rejectValue(e.getField(), "account.invalid", e.getMessage());
                } else {
                    detailErrors.rejectValue(e.getField(), "account.invalid", e.getMessage());
                }
            }
        }
        redirectAttributes.addFlashAttribute(BINDING_PREFIX + "updateRequest", accountErrors);
        redirectAttributes.addFlashAttribute("updateRequest", account);
        redirectAttributes.addFlashAttribute(BINDING_PREFIX + "detailsRequest", detailErrors);
        redirectAttributes.addFlashAttribute("detailsRequest", details);
        return "redirect:/portal/profile";
    }

    @PostMapping("/avatar")
    public String changeAvatar(Authentication authentication,
                               @RequestParam(name = "avatarFile", required = false) MultipartFile avatarFile,
                               RedirectAttributes redirectAttributes) {
        try {
            userProfileService.changeAvatar(authentication.getName(), avatarFile);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật ảnh đại diện.");
        } catch (AvatarUploadException e) {
            redirectAttributes.addFlashAttribute("avatarError", e.getMessage());
        }
        return "redirect:/portal/profile";
    }
}
