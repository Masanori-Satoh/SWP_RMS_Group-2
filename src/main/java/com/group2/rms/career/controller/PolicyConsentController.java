package com.group2.rms.career.controller;

import com.group2.rms.career.service.PolicyConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller xử lý xác nhận đồng ý Điều khoản sử dụng và Chính sách bảo mật của ứng viên.
 */
@Controller
@RequiredArgsConstructor
public class PolicyConsentController {

    public static final String MISSING_AGREEMENT = "Vui lòng tick đồng ý cả Điều khoản sử dụng và Chính sách bảo mật.";
    private static final String FALLBACK_URL = "/jobs";

    private final PolicyConsentService policyConsentService;

    /**
     * Ghi nhận ứng viên đã đọc và đồng ý với điều khoản sử dụng và chính sách bảo mật.
     */
    @PostMapping("/policy-consents")
    public String agree(
            @RequestParam(defaultValue = "false") boolean agreeTerms,
            @RequestParam(defaultValue = "false") boolean agreePrivacy,
            @RequestParam(required = false) String returnUrl,
            Authentication authentication,
            RedirectAttributes flash) {
        if (!agreeTerms || !agreePrivacy) {
            flash.addFlashAttribute("policyConsentError", MISSING_AGREEMENT);
        } else {
            policyConsentService.recordConsent(authentication.getName());
        }
        return "redirect:" + safeReturnUrl(returnUrl);
    }

    /**
     * Kiểm tra và chuẩn hóa đường dẫn chuyển hướng an toàn, ngăn chặn open redirect.
     */
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
