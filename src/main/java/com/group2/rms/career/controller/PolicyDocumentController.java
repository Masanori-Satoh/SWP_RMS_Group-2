package com.group2.rms.career.controller;

import com.group2.rms.admin.service.PolicyDocument;
import com.group2.rms.admin.service.PolicyDocumentStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller hiển thị và tải các tài liệu điều khoản và chính sách bảo mật công khai.
 */
@Controller
@RequiredArgsConstructor
public class PolicyDocumentController {

    private final PolicyDocumentStorage policyDocumentStorage;

    /**
     * Hiển thị nội dung văn bản Điều khoản sử dụng hệ thống.
     */
    @GetMapping("/terms")
    public ResponseEntity<Resource> terms() {
        return pdf(PolicyDocument.TERMS);
    }

    /**
     * Hiển thị nội dung văn bản Chính sách bảo mật thông tin.
     */
    @GetMapping("/privacy-policy")
    public ResponseEntity<Resource> privacyPolicy() {
        return pdf(PolicyDocument.PRIVACY);
    }

    private ResponseEntity<Resource> pdf(PolicyDocument document) {
        Resource file = policyDocumentStorage.read(document);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        ContentDisposition inline = ContentDisposition.inline().filename(document.fileName()).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, inline.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(file);
    }
}
