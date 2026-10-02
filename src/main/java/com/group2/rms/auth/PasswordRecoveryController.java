package com.group2.rms.auth;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordRecoveryController.class);

    private final PasswordResetService resetService;
    private final PasswordResetEmailSender emailSender;

    public PasswordRecoveryController(PasswordResetService resetService,
            PasswordResetEmailSender emailSender) {
        this.resetService = resetService;
        this.emailSender = emailSender;
    }

    @GetMapping("/forgot-password")
    public String forgotForm(Model model) {
        model.addAttribute("form", new ForgotPasswordRequest());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestReset(@Valid @ModelAttribute("form") ForgotPasswordRequest form,
            BindingResult errors) {
        if (!emailSender.isConfigured() || !resetService.isConfigured()) {
            errors.reject("mail.unavailable", "Password recovery email is not configured yet.");
        }
        if (errors.hasErrors()) {
            return "auth/forgot-password";
        }
        resetService.request(form.getEmail()).ifPresent(link -> {
            try {
                emailSender.send(link.email(), link.token());
            } catch (RuntimeException exception) {
                // Never log the address, token, SMTP details, or exception message.
                LOGGER.warn("Password recovery email delivery failed");
            }
        });
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
            BindingResult errors, Model model, HttpServletResponse response) {
        noStore(response);
        if (form.getPassword() != null && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }
        if (!errors.hasErrors()) {
            try {
                if (resetService.reset(token, form.getPassword())) {
                    return "redirect:/login?reset";
                }
                errors.reject("token.invalid", "This link is invalid, used, or expired.");
            } catch (AccountFieldException exception) {
                errors.rejectValue(exception.getField(), "password.invalid", exception.getMessage());
            }
        }
        form.setPassword(null);
        form.setConfirmPassword(null);
        model.addAttribute("token", token);
        model.addAttribute("validToken", resetService.isValid(token));
        return "auth/reset-password";
    }

    private static void noStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
    }
}
