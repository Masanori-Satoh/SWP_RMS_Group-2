package com.group2.rms.user.controller;

import com.group2.rms.user.dto.ChangePasswordRequest;
import com.group2.rms.user.dto.UpdateProfileRequest;
import com.group2.rms.user.dto.UserProfileResponse;
import com.group2.rms.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Self-service "My Profile" screen and password management.
 * Always securely operates on the authenticated user from the SecurityContext.
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
        UserProfileResponse profile = profileService.getProfile(authentication.getName());
        model.addAttribute("userProfile", profile);
        if (!model.containsAttribute("updateRequest")) {
            model.addAttribute("updateRequest",
                    new UpdateProfileRequest(profile.fullName(), profile.phoneNumber(), profile.avatarUrl()));
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
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công.");
        return "redirect:/profile";
    }

    /**
     * AJAX endpoint đổi mật khẩu trực tiếp không cần tải lại trang.
     * Nhận payload JSON hỗ trợ UTF-8 và trả về trạng thái cùng lỗi từng trường.
     */
    @PostMapping(value = "/change-password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePasswordJson(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request,
            BindingResult bindingResult) {

        Map<String, Object> response = new HashMap<>();

        if (request.newPassword() != null && request.confirmPassword() != null
                && !request.newPassword().equals(request.confirmPassword())) {
            bindingResult.addError(new FieldError("changePasswordRequest", "confirmPassword", "Mật khẩu xác nhận không khớp."));
        }

        if (!bindingResult.hasErrors()
                && !profileService.changePassword(authentication.getName(), request)) {
            bindingResult.addError(new FieldError("changePasswordRequest", "currentPassword",
                    "Mật khẩu hiện tại không chính xác. Vui lòng kiểm tra lại."));
        }

        if (bindingResult.hasErrors()) {
            response.put("success", false);
            response.put("message", "Không thể cập nhật mật khẩu. Vui lòng kiểm tra các lỗi bên dưới.");
            Map<String, String> fieldErrors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
            }
            response.put("errors", fieldErrors);
            return ResponseEntity.badRequest().body(response);
        }

        response.put("success", true);
        response.put("message", "Đổi mật khẩu thành công.");
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint dự phòng submit form chuẩn (trường hợp trình duyệt tắt JavaScript).
     */
    @PostMapping(value = "/change-password", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String changePasswordForm(
            Authentication authentication,
            @Valid @ModelAttribute("passwordRequest") ChangePasswordRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (request.newPassword() != null && request.confirmPassword() != null
                && !request.newPassword().equals(request.confirmPassword())) {
            bindingResult.addError(new FieldError("passwordRequest", "confirmPassword", "Mật khẩu xác nhận không khớp."));
        }
        if (!bindingResult.hasErrors()
                && !profileService.changePassword(authentication.getName(), request)) {
            bindingResult.addError(new FieldError("passwordRequest", "currentPassword",
                    "Mật khẩu hiện tại không chính xác. Vui lòng kiểm tra lại."));
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("openPasswordModal", true);
            redirectAttributes.addFlashAttribute(BINDING_PREFIX + "passwordRequest", bindingResult);
            redirectAttributes.addFlashAttribute("passwordRequest", new ChangePasswordRequest("", "", ""));
            return "redirect:/profile";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công.");
        return "redirect:/profile";
    }
}
