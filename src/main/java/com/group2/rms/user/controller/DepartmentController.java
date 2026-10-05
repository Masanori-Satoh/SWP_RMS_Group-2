package com.group2.rms.user.controller;

import com.group2.rms.user.dto.DepartmentRequest;
import com.group2.rms.user.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/departments")
public class DepartmentController {
    private final DepartmentService departments;
    public DepartmentController(DepartmentService departments) { this.departments = departments; }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "") String status,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("departments", departments.findDepartments(search, status, page));
        model.addAttribute("search", search);
        model.addAttribute("selectedStatus", status);
        return "admin/departments/list";
    }

    @GetMapping("/new")
    public String newDepartment(Model model) {
        model.addAttribute("vietnameseUi", true);
        if (!model.containsAttribute("form")) model.addAttribute("form", new DepartmentRequest("", null));
        model.addAttribute("departmentId", null);
        return "admin/departments/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") DepartmentRequest form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) return newDepartment(model);
        departments.create(form);
        redirect.addFlashAttribute("successMessage", "Đã tạo phòng ban.");
        return "redirect:/admin/departments";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable int id, Model model) {
        model.addAttribute("vietnameseUi", true);
        var current = departments.findForEdit(id);
        if (!model.containsAttribute("form")) model.addAttribute("form", new DepartmentRequest(current.name(), current.managerId()));
        var choices = departments.managerChoices(id);
        model.addAttribute("departmentId", id);
        model.addAttribute("department", current);
        model.addAttribute("managers", choices);
        model.addAttribute("currentManagerEligible", choices.stream().anyMatch(choice -> choice.id().equals(current.managerId())));
        return "admin/departments/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable int id, @Valid @ModelAttribute("form") DepartmentRequest form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) return edit(id, model);
        departments.update(id, form);
        redirect.addFlashAttribute("successMessage", "Đã cập nhật phòng ban.");
        return "redirect:/admin/departments";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable int id, RedirectAttributes redirect) {
        departments.deactivate(id);
        redirect.addFlashAttribute("successMessage", "Đã vô hiệu hóa phòng ban. Dữ liệu liên quan được giữ nguyên.");
        return "redirect:/admin/departments";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable int id, RedirectAttributes redirect) {
        departments.activate(id);
        redirect.addFlashAttribute("successMessage", "Đã kích hoạt lại phòng ban.");
        return "redirect:/admin/departments";
    }
}
