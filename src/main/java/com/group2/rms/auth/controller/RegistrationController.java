package com.group2.rms.auth.controller;

import com.group2.rms.auth.service.RegistrationVerificationService;
import com.group2.rms.auth.dto.RegisterAccountRequest;
import com.group2.rms.auth.dto.VerifyRegistrationOtpRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/register")
public class RegistrationController {

    private final RegistrationVerificationService verification;

    public RegistrationController(RegistrationVerificationService verification) {
        this.verification = verification;
    }

    @GetMapping
    public String form(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterAccountRequest());
        }
        return "auth/register";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("form") RegisterAccountRequest form,
                           BindingResult errors, HttpSession session) {
        if (errors.hasErrors()) {
            form.setPassword(null);
            form.setConfirmPassword(null);
            return "auth/register";
        }
        verification.requestRegistration(form.toCommand(), session);
        return "redirect:/register/otp";
    }

    @GetMapping("/otp")
    public String otpForm(Model model, HttpSession session) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new VerifyRegistrationOtpRequest());
        }
        addOtpContext(model, session);
        return "auth/register-otp";
    }

    @PostMapping("/otp")
    public String verifyOtp(@Valid @ModelAttribute("form") VerifyRegistrationOtpRequest form,
            BindingResult errors, Model model, HttpSession session) {
        if (errors.hasErrors()) {
            addOtpContext(model, session);
            return "auth/register-otp";
        }
        verification.verifyAndRegister(form.getOtp(), session);
        return "redirect:/login?registered";
    }

    @PostMapping("/otp/resend")
    public String resendOtp(HttpSession session) {
        verification.resendOtp(session);
        return "redirect:/register/otp";
    }

    private void addOtpContext(Model model, HttpSession session) {
        model.addAttribute("pendingRegistrationEmail", verification.getPendingRegistrationEmail(session));
        model.addAttribute("retryAfterSeconds", verification.getResendRemainingSeconds(session));
    }
}
