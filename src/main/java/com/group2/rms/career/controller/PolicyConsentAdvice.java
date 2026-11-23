package com.group2.rms.career.controller;

import com.group2.rms.career.service.PolicyConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller advice cung cấp thuộc tính kiểm tra yêu cầu đồng ý chính sách cho ứng viên.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class PolicyConsentAdvice {

    private static final String ROLE_CANDIDATE = "ROLE_CANDIDATE";

    private final ObjectProvider<PolicyConsentService> policyConsent;

    /**
     * Xác định ứng viên có cần xác nhận chính sách trước khi tiếp tục hay không.
     */
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
