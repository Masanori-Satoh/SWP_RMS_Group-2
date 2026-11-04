package com.group2.rms.career.controller;

import com.group2.rms.career.service.PolicyConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/*
 * Nút "Tôi đồng ý" trong popup Điều khoản & Chính sách bảo mật (chỉ Candidate, SecurityConfig).
 * Đủ 2 ô → lưu rồi quay về đúng trang đang xem; thiếu ô → quay về, popup hiện lại kèm lỗi.
 */
@Controller
@RequiredArgsConstructor
public class PolicyConsentController {

    public static final String MISSING_AGREEMENT = "Vui lòng tick đồng ý cả Điều khoản sử dụng và Chính sách bảo mật.";
    private static final String FALLBACK_URL = "/jobs";

    private final PolicyConsentService policyConsent;

    @PostMapping("/policy-consents")
    public String agree(@RequestParam(defaultValue = "false") boolean agreeTerms,
                        @RequestParam(defaultValue = "false") boolean agreePrivacy,
                        @RequestParam(required = false) String returnUrl,
                        Authentication authentication, RedirectAttributes flash) {
        // Rule: phải tick đủ cả 2 tài liệu, kể cả khi form bị gửi tay bỏ qua nút đã khóa
        if (!agreeTerms || !agreePrivacy) {
            flash.addFlashAttribute("policyConsentError", MISSING_AGREEMENT);
        } else {
            policyConsent.recordConsent(authentication.getName());
        }
        return "redirect:" + safeReturnUrl(returnUrl);
    }

    /** Security: chỉ quay về đường dẫn nội bộ ("/jobs/5#apply"), chặn "//evil.com", "https://..." (open redirect). */
    static String safeReturnUrl(String returnUrl) {
        if (returnUrl == null || !returnUrl.startsWith("/")) {
            return FALLBACK_URL;
        }
        if (returnUrl.startsWith("//") || returnUrl.startsWith("/\\")) {
            return FALLBACK_URL;
        }
        if (returnUrl.contains("\r") || returnUrl.contains("\n")) {
            return FALLBACK_URL;
        }
        return returnUrl;
    }
}
