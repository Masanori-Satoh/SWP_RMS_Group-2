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

/*
 * Hai tài liệu chính sách công khai (khách cũng mở được): trả PDF để trình duyệt tự hiển thị.
 * Link trong popup đồng ý và ở footer đều trỏ về đây, nên luôn mở cùng một file.
 */
@Controller
@RequiredArgsConstructor
public class PolicyDocumentController {

    private final PolicyDocumentStorage storage;

    @GetMapping("/terms")
    public ResponseEntity<Resource> terms() {
        return pdf(PolicyDocument.TERMS);
    }

    @GetMapping("/privacy-policy")
    public ResponseEntity<Resource> privacyPolicy() {
        return pdf(PolicyDocument.PRIVACY);
    }

    private ResponseEntity<Resource> pdf(PolicyDocument document) {
        Resource file = storage.read(document);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        // inline: mở ngay trong tab, không bắt tải về
        ContentDisposition inline = ContentDisposition.inline().filename(document.fileName()).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, inline.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(file);
    }
}
