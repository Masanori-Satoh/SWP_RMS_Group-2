package com.group2.rms.career.controller;

import com.group2.rms.career.service.PolicyConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/*
 * Cấp cờ policyConsentRequired cho mọi view; public-footer dựa vào cờ này để chèn popup đồng ý.
 * Khách và vai trò nội bộ: false ngay, không truy vấn DB.
 * PolicyConsentService lấy qua ObjectProvider: advice được nạp vào mọi @WebMvcTest, phần lớn không có bean đó.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class PolicyConsentAdvice {

    private static final String ROLE_CANDIDATE = "ROLE_CANDIDATE";

    private final ObjectProvider<PolicyConsentService> policyConsent;

    @ModelAttribute("policyConsentRequired")
    public boolean policyConsentRequired(Authentication authentication) {
        if (!isCandidate(authentication)) {
            return false;
        }
        PolicyConsentService service = policyConsent.getIfAvailable();
        if (service == null) {
            return false;
        }
        return service.needsConsent(authentication.getName());
    }

    private static boolean isCandidate(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (ROLE_CANDIDATE.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
