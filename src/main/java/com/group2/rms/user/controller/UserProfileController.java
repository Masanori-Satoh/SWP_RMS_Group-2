package com.group2.rms.user.controller;

import com.group2.rms.core.security.AccountSessionGuardFilter;
import com.group2.rms.user.dto.ChangePasswordRequest;
import com.group2.rms.user.dto.UpdateProfileRequest;
import com.group2.rms.user.dto.UserProfileResponse;
import com.group2.rms.user.exception.AvatarUploadException;
import com.group2.rms.user.service.UserProfileService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller quản lý hồ sơ cá nhân và đổi mật khẩu cho người dùng.
 */
@Controller
@RequestMapping("/profile")
public class UserProfileController {

    private static final String BINDING_PREFIX = "org.springframework.validation.BindingResult.";

    private final UserProfileService profileService;

    public UserProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String viewProfile(Authentication authentication, Model model) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CANDIDATE".equals(a.getAuthority()))) {
            return "redirect:/portal/profile";
        }
        UserProfileResponse profile = profileService.getProfile(authentication.getName());
        model.addAttribute("userProfile", profile);
        model.addAttribute("activeMenu", "profile");
        model.addAttribute("vietnameseUi", true);
        if (!model.containsAttribute("updateRequest")) {
            model.addAttribute("updateRequest",
                    new UpdateProfileRequest(profile.fullName(), profile.phoneNumber()));
        }
        if (!model.containsAttribute("passwordRequest")) {
            model.addAttribute("passwordRequest", new ChangePasswordRequest("", "", ""));
        }
        return "user/profile";
    }

    @PostMapping("/update")
    public String updateProfile(Authentication authentication,
                                @Valid @ModelAttribute("updateRequest") UpdateProfileRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(BINDING_PREFIX + "updateRequest", bindingResult);
            redirectAttributes.addFlashAttribute("updateRequest", request);
            return "redirect:/profile";
        }
        profileService.updateProfile(authentication.getName(), request);
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu hồ sơ.");
        return "redirect:/profile";
    }

    @PostMapping("/avatar")
    public String changeAvatar(Authentication authentication,
                               @RequestParam(name = "avatarFile", required = false) MultipartFile avatarFile,
                               RedirectAttributes redirectAttributes) {
        try {
            profileService.changeAvatar(authentication.getName(), avatarFile);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật ảnh đại diện.");
        } catch (AvatarUploadException e) {
            redirectAttributes.addFlashAttribute("avatarError", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(
            Authentication authentication,
            @Valid @ModelAttribute("passwordRequest") ChangePasswordRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        boolean isCandidate = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CANDIDATE".equals(a.getAuthority()));
        String redirectTarget = isCandidate ? "redirect:/portal/profile" : "redirect:/profile";

        if (request.newPassword() != null && request.confirmPassword() != null
                && !request.newPassword().equals(request.confirmPassword())) {
            bindingResult.addError(
                    new FieldError("passwordRequest", "confirmPassword", "Mật khẩu nhập lại không khớp."));
        }
        if (!bindingResult.hasErrors()
                && !profileService.changePassword(authentication.getName(), request)) {
            bindingResult.addError(new FieldError("passwordRequest", "currentPassword",
                    "Mật khẩu hiện tại không đúng."));
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("openPasswordModal", true);
            redirectAttributes.addFlashAttribute(BINDING_PREFIX + "passwordRequest", bindingResult);
            redirectAttributes.addFlashAttribute("passwordRequest", request);
            return redirectTarget;
        }
        session.removeAttribute(AccountSessionGuardFilter.PASSWORD_HASH);
        redirectAttributes.addFlashAttribute("successMessage", "Đã đổi mật khẩu.");
        return redirectTarget;
    }
}
