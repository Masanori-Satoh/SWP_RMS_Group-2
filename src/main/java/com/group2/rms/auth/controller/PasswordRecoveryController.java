package com.group2.rms.auth.controller;

import com.group2.rms.auth.dto.ForgotPasswordRequest;
import com.group2.rms.auth.service.PasswordRecoveryService;
import com.group2.rms.auth.service.PasswordResetEmailSender;
import com.group2.rms.auth.service.PasswordResetService;
import com.group2.rms.auth.dto.ResetPasswordRequest;
import com.group2.rms.user.exception.AccountFieldException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PasswordRecoveryController {
    private final PasswordRecoveryService recoveryService;
    private final PasswordResetService resetService;

    public PasswordRecoveryController(PasswordRecoveryService recoveryService,
                                      PasswordResetService resetService) {
        this.recoveryService = recoveryService;
        this.resetService = resetService;
    }

    @GetMapping("/forgot-password")
    public String forgotForm(Model model) {
        model.addAttribute("form", new ForgotPasswordRequest());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestReset(@Valid @ModelAttribute("form") ForgotPasswordRequest form) {
        recoveryService.requestReset(form.getEmail());
        return "redirect:/forgot-password?sent";
    }

    @GetMapping("/reset-password/{token}")
    public String resetForm(@PathVariable String token, Model model, HttpServletResponse response) {
        noStore(response);
        model.addAttribute("form", new ResetPasswordRequest());
        model.addAttribute("token", token);
        model.addAttribute("validToken", resetService.isValid(token));
        return "auth/reset-password";
    }

    @PostMapping("/reset-password/{token}")
    public String reset(@PathVariable String token,
                        @Valid @ModelAttribute("form") ResetPasswordRequest form,
                        HttpServletResponse response) {
        noStore(response);
        resetService.reset(token,form.getOtp(), form.getPassword());
        return "redirect:/login?reset";
    }

    private static void noStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
    }
}
