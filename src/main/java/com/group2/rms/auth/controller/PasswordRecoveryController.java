package com.group2.rms.auth.controller;

import com.group2.rms.auth.dto.ForgotPasswordRequest;
import com.group2.rms.auth.service.PasswordRecoveryService;
import com.group2.rms.auth.dto.ResetPasswordRequest;
import com.group2.rms.auth.dto.VerifyPasswordResetOtpRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PasswordRecoveryController {
    private final PasswordRecoveryService recoveryService;

    public PasswordRecoveryController(PasswordRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    //1. show form enter email
    @GetMapping("/forgot-password")
    public String forgotForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ForgotPasswordRequest());
        }
        return "auth/forgot-password";
    }

    // 2. send otp and handle request reset
    @PostMapping("/forgot-password")
    public String requestReset(@Valid @ModelAttribute("form") ForgotPasswordRequest form, BindingResult errors,
            HttpSession session) {
        // check if email has error
        if (errors.hasErrors()) {
            return "auth/forgot-password";
        }
        recoveryService.requestReset(form.getEmail(), session);
        return "redirect:/reset-password/otp";
    }

    // 3. otp form
    @GetMapping("/reset-password/otp")
    public String otpForm(Model model, HttpSession session) {
        // check if valid for change new password
        if (recoveryService.canResetPassword(session)) {
            return "redirect:/reset-password";
        }
        // check if otp has not send or expired
        if (!recoveryService.hasPendingReset(session)) {
            return "redirect:/forgot-password";
        }
        // check if model don't have form
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new VerifyPasswordResetOtpRequest());
        }
        addOtpContext(model, session);
        return "auth/reset-password-otp";
    }

    // 4. verify otp
    @PostMapping("/reset-password/otp")
    public String verifyOtp(@Valid @ModelAttribute("form") VerifyPasswordResetOtpRequest form, BindingResult errors,
            Model model, HttpSession session) {
        // check if can ResetPassword
        if (recoveryService.canResetPassword(session)) {
            return "redirect:/reset-password";
        }
        // check if otp is not valid or expired
        if (!recoveryService.hasPendingReset(session)) {
            return "redirect:/forgot-password";
        }
        // check if otp has error
        if (errors.hasErrors()) {
            addOtpContext(model, session);
            return "auth/reset-password-otp";
        }
        recoveryService.verifyOtp(form.getOtp(), session);
        return "redirect:/reset-password";
    }

    // 5. show form new password after verify OTP
    @GetMapping("/reset-password")
    public String resetForm(Model model, HttpSession session) {
        // check if can reset password
        if (!recoveryService.canResetPassword(session)) {
            // check if has pending before
            return recoveryService.hasPendingReset(session)
                    // can reset
                    ? "redirect:/reset-password/otp"
                    // not can reset
                    : "redirect:/forgot-password";
        }
        // check if form not exist in model
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ResetPasswordRequest());
        }
        return "auth/reset-password";

    }

    // 6 reset password
    @PostMapping("/reset-password")
    public String reset(@Valid @ModelAttribute("form") ResetPasswordRequest form,
            BindingResult errors,
            HttpSession session) {
        // check if can not resetpassword
        if (!recoveryService.canResetPassword(session)) {
            return recoveryService.hasPendingReset(session)
                    ? "redirect:/reset-password/otp"
                    : "redirect:/forgot-password";
        }
        // check if has error
        if (errors.hasErrors()) {
            form.setPassword(null);
            form.setConfirmPassword(null);

            return "auth/reset-password";
        }
        // reset password
        recoveryService.completeReset(form.getPassword(), session);
        return "redirect:/login?reset";
    }

    private void addOtpContext(Model model, HttpSession session) {
        model.addAttribute("pendingResetEmail", recoveryService.getPendingResetEmail(session));
        model.addAttribute("retryAfterSeconds", recoveryService.getResendRemainingSeconds(session));
    }

}
