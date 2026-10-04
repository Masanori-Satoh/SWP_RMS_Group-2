package com.group2.rms.auth.controller;

import com.group2.rms.auth.service.CandidateRegistrationService;
import com.group2.rms.auth.dto.RegisterAccountRequest;
import com.group2.rms.user.exception.AccountFieldException;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
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

    private final CandidateRegistrationService registration;

    public RegistrationController(CandidateRegistrationService registration) {
        this.registration = registration;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("form", new RegisterAccountRequest());
        return "auth/register";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("form") RegisterAccountRequest form,
                           BindingResult errors) {
      
        if (!errors.hasErrors()) {
            try {
                registration.register(form.toCommand());
                return "redirect:/login?registered";
            } catch (AccountFieldException exception) {
                errors.rejectValue(exception.getField(), "account.invalid", exception.getMessage());
            } catch (DataIntegrityViolationException exception) {
                errors.reject("account.conflict", "The username or email is already in use.");
            }
        }
        form.setPassword(null);
        form.setConfirmPassword(null);
        return "auth/register";
    }
}
