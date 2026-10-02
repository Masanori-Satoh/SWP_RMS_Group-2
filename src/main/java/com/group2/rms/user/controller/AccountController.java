package com.group2.rms.user.controller;

import com.group2.rms.user.dto.CreateAccountRequest;
import com.group2.rms.user.dto.UpdateAccountRequest;
import com.group2.rms.user.exception.AccountFieldException;
import com.group2.rms.user.service.AccountListService;
import com.group2.rms.user.service.AccountManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/accounts")
public class AccountController {

    private final AccountListService accounts;
    private final AccountManagementService management;

    public AccountController(AccountListService accounts, AccountManagementService management) {
        this.accounts = accounts;
        this.management = management;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String search,
                       @RequestParam(required = false) Integer roleId,
                       @RequestParam(required = false) Integer departmentId,
                       @RequestParam(defaultValue = "") String status,
                       @RequestParam(defaultValue = "name") String sort,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<AccountListService.AccountRow> accountPage = accounts.findAccounts(
                search, roleId, departmentId, status, sort, page);
        model.addAttribute("accounts", accountPage);
        model.addAttribute("roles", accounts.findRoles());
        model.addAttribute("departments", accounts.findDepartments());
        model.addAttribute("search", search.trim());
        model.addAttribute("selectedRoleId", roleId);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSort", sort);
        return "admin/accounts/list";
    }

    @PostMapping("/{userId}/deactivate")
    public String deactivate(@PathVariable int userId, RedirectAttributes redirectAttributes) {
        accounts.deactivate(userId);
        redirectAttributes.addFlashAttribute("successMessage", "Account deactivated successfully.");
        return "redirect:/admin/accounts";
    }

    @GetMapping("/new")
    public String newAccount(Model model) {
        model.addAttribute("form", new CreateAccountRequest());
        prepareCreate(model);
        return "admin/accounts/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CreateAccountRequest form,
                         BindingResult errors, Model model, RedirectAttributes redirectAttributes) {
        if (form.getPassword() != null && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }
        if (!errors.hasErrors()) {
            try {
                management.createInternal(form.toCommand());
                redirectAttributes.addFlashAttribute("successMessage", "Account created successfully.");
                return "redirect:/admin/accounts";
            } catch (AccountFieldException exception) {
                errors.rejectValue(exception.getField(), "account.invalid", exception.getMessage());
            } catch (DataIntegrityViolationException exception) {
                errors.reject("account.conflict", "The username or email is already in use.");
            }
        }
        form.setPassword(null);
        form.setConfirmPassword(null);
        prepareCreate(model);
        return "admin/accounts/form";
    }

    @GetMapping("/{userId}/edit")
    public String editAccount(@PathVariable int userId, Model model) {
        AccountManagementService.AccountForEdit account = management.findForEdit(userId);
        UpdateAccountRequest form = new UpdateAccountRequest();
        form.setFullName(account.fullName());
        form.setEmail(account.email());
        form.setPhoneNumber(account.phoneNumber());
        form.setRoleId(account.roleId());
        form.setDepartmentId(account.departmentId());
        form.setAccountStatus(account.accountStatus());
        model.addAttribute("form", form);
        prepareEdit(model, userId, account.username());
        return "admin/accounts/form";
    }

    @PostMapping("/{userId}")
    public String update(@PathVariable int userId,
                         @Valid @ModelAttribute("form") UpdateAccountRequest form,
                         BindingResult errors, Model model, RedirectAttributes redirectAttributes) {
        management.findForEdit(userId);
        if (!errors.hasErrors()) {
            try {
                management.updateInternal(userId, form.toCommand());
                redirectAttributes.addFlashAttribute("successMessage", "Account updated successfully.");
                return "redirect:/admin/accounts";
            } catch (AccountFieldException exception) {
                errors.rejectValue(exception.getField(), "account.invalid", exception.getMessage());
            } catch (DataIntegrityViolationException exception) {
                errors.reject("account.conflict", "The email is already in use.");
            }
        }
        prepareEdit(model, userId, management.findForEdit(userId).username());
        return "admin/accounts/form";
    }

    private void prepareCreate(Model model) {
        model.addAttribute("createMode", true);
        model.addAttribute("roles", accounts.findRoles());
        model.addAttribute("departments", accounts.findDepartments());
    }

    private void prepareEdit(Model model, int userId, String username) {
        model.addAttribute("createMode", false);
        model.addAttribute("accountId", userId);
        model.addAttribute("accountUsername", username);
        model.addAttribute("roles", accounts.findRoles());
        model.addAttribute("departments", accounts.findDepartments());
    }
}
