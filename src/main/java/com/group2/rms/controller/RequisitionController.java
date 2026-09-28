package com.group2.rms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group2.rms.dto.request.RequisitionRequestDto;
import com.group2.rms.repository.DepartmentRepository;
import com.group2.rms.repository.UserRepository;
import com.group2.rms.service.RequisitionService;

import lombok.RequiredArgsConstructor;

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

        // 1. Goi service lay data, truyen page - 1 (vi Spring JPA page bat dau tu 0)
        var pagedData = requisitionService.getAllRequisitions(page - 1, size);

        // goi data de mang ra giao dien
        model.addAttribute("requisitions", pagedData.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pagedData.getTotalPages());

        return "requisitions/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model){
        //Tao 1 dto rong de form hung du lieu
        model.addAttribute("requisitionDto", new com.group2.rms.dto.request.RequisitionRequestDto());
    
        //Lay danh sach department day ra dropdown
        model.addAttribute("departments", departmentRepo.findAll());
        //Lay danh sach user lam hiring manager
        model.addAttribute("hiringManagers", userRepository.findAll());

        return "requisitions/form";
    }

    @org.springframework.web.bind.annotation.PostMapping("/create")
    public String createRequisition(
            @org.springframework.web.bind.annotation.ModelAttribute("requisitionDto") com.group2.rms.dto.request.RequisitionRequestDto dto) {
        
        // Tạm thời hardcode hiringManagerId = 1 (vì chưa hoàn thiện tính năng Đăng nhập - Login)
        // Sau này khi có Spring Security, ta sẽ lấy ID từ User đang đăng nhập: 
        // Integer hiringManagerId = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
        Integer mockHiringManagerId = 1; 

        // Gọi Service lưu vào DB
        requisitionService.createRequisition(dto, mockHiringManagerId);

        // Lưu xong thì chuyển hướng (redirect) về trang danh sách
        return "redirect:/requisitions";
    }

    @GetMapping("/{id}")
    public String showRequisitionDetail(@org.springframework.web.bind.annotation.PathVariable("id") Integer id, Model model) {
        var requisitionDetail = requisitionService.getById(id);
        model.addAttribute("req", requisitionDetail);
        return "requisitions/detail";
    }
}
