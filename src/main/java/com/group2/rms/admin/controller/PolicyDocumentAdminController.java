package com.group2.rms.admin.controller;

import com.group2.rms.admin.exception.PolicyDocumentException;
import com.group2.rms.admin.service.PolicyDocument;
import com.group2.rms.admin.service.PolicyDocumentService;
import com.group2.rms.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
 * Màn System Admin "Tài liệu chính sách": xem và thay 2 file PDF ứng viên đọc.
 * Quyền: /admin/** chỉ System Admin (SecurityConfig). Nghiệp vụ: PolicyDocumentService.
 */
@Controller
@RequestMapping("/admin/policy-documents")
@RequiredArgsConstructor
public class PolicyDocumentAdminController {

    private static final String LIST_URL = "redirect:/admin/policy-documents";

    private final PolicyDocumentService policyDocuments;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("documents", policyDocuments.listDocuments());
        model.addAttribute("activeMenu", "policy-documents");
        return "admin/policy-documents";
    }

    @PostMapping("/{code}")
    public String replace(@PathVariable String code, @RequestParam(value = "file", required = false) MultipartFile file,
                          RedirectAttributes flash) {
        PolicyDocument document = PolicyDocument.fromCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu."));
        try {
            policyDocuments.replace(document, file);
            flash.addFlashAttribute("successMessage", "Đã thay " + document.title() + ". Ứng viên thấy bản mới ngay.");
        } catch (PolicyDocumentException e) {
            // Mở lại đúng hộp thoại tải lên, lỗi hiện cạnh ô chọn file
            flash.addFlashAttribute("uploadError", e.getMessage());
            flash.addFlashAttribute("uploadErrorCode", document.code());
        }
        return LIST_URL;
    }
}
