package com.group2.rms.requisition.controller;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.service.RequisitionService;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;

@Controller
@RequestMapping("/requisitions")
@RequiredArgsConstructor
public class RequisitionController {
    private final RequisitionService requisitionService;
    private final DepartmentRepository departmentRepo;
    private final UserRepository userRepository;

    @GetMapping
    public String listRequisitions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        int validPage = Math.max(1, page);
        var pagedData = requisitionService.getAllRequisitions(validPage, size);

        model.addAttribute("requisitions", pagedData.getContent());
        model.addAttribute("currentPage", validPage);
        model.addAttribute("totalPages", Math.max(1, pagedData.getTotalPages()));

        return "requisitions/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        RequisitionRequest dto = new RequisitionRequest();
        dto.setNumberOfPositions(1);
        var initialList = new ArrayList<ScreeningCriteriaRequest>();
        var initialCriteria = new ScreeningCriteriaRequest();
        initialCriteria.setWeight(new BigDecimal(100));
        initialCriteria.setIsMandatory(false);
        initialList.add(initialCriteria);
        dto.setScreeningCriteria(initialList);

        model.addAttribute("requisitionDto", dto);
        model.addAttribute("isEdit", false);
        model.addAttribute("departments", departmentRepo.findAll());
        model.addAttribute("hiringManagers", userRepository.findAll());

        return "requisitions/form";
    }
    // @Valid
    @PostMapping("/create")
    public String createRequisition(@ModelAttribute("requisitionDto") RequisitionRequest dto) {
        Integer mockHiringManagerId = 1;
        requisitionService.createRequisition(dto, mockHiringManagerId);
        return "redirect:/requisitions";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        var dto = requisitionService.getRequestDtoById(id);
        model.addAttribute("requisitionDto", dto);
        model.addAttribute("requisitionId", id);
        model.addAttribute("isEdit", true);
        model.addAttribute("departments", departmentRepo.findAll());
        model.addAttribute("hiringManagers", userRepository.findAll());
        return "requisitions/form";
    }

    @PostMapping("/edit/{id}")
    public String updateRequisition(
            @PathVariable("id") Integer id,
            @ModelAttribute("requisitionDto") RequisitionRequest dto) {
        requisitionService.updateRequisition(id, dto);
        return "redirect:/requisitions";
    }

    @PostMapping("/delete/{id}")
    public String deleteRequisition(@PathVariable("id") Integer id) {
        requisitionService.deleteRequisition(id);
        return "redirect:/requisitions";
    }

    @GetMapping("/delete/{id}")
    public String deleteRequisitionGet(@PathVariable("id") Integer id) {
        requisitionService.deleteRequisition(id);
        return "redirect:/requisitions";
    }

    @GetMapping("/{id}")
    public String showRequisitionDetail(@PathVariable("id") Integer id, Model model) {
        var requisitionDetail = requisitionService.getById(id);
        model.addAttribute("req", requisitionDetail);
        return "requisitions/detail";
    }
}
