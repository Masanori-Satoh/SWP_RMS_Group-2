package com.group2.rms.career.controller;

import com.group2.rms.career.service.PolicyConsentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST: PolicyConsentAdvice — ai thấy popup đồng ý.
 * Khách / vai trò nội bộ phải trả false mà không hỏi service (không truy vấn DB ở mọi trang).
 */
@ExtendWith(MockitoExtension.class)
class PolicyConsentAdviceTests {

    @Mock
    private ObjectProvider<PolicyConsentService> provider;
    @Mock
    private PolicyConsentService service;

    private PolicyConsentAdvice advice;

    @BeforeEach
    void setUp() {
        lenient().when(provider.getIfAvailable()).thenReturn(service);
        advice = new PolicyConsentAdvice(provider);
    }

    @Test
    @DisplayName("Khách chưa đăng nhập -> Không popup, không hỏi DB")
    void policyConsentRequired_shouldBeFalse_whenAnonymous() {
        var anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        assertFalse(advice.policyConsentRequired(anonymous));
        assertFalse(advice.policyConsentRequired(null));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("HR mở trang công khai -> Không popup, không hỏi DB")
    void policyConsentRequired_shouldBeFalse_whenInternalRole() {
        var hr = UsernamePasswordAuthenticationToken.authenticated("hr", null,
                AuthorityUtils.createAuthorityList("ROLE_HR"));

        assertFalse(advice.policyConsentRequired(hr));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Ứng viên chưa đồng ý -> Hiện popup")
    void policyConsentRequired_shouldBeTrue_whenCandidateHasNotAgreed() {
        var candidate = UsernamePasswordAuthenticationToken.authenticated("an.nguyen", null,
                AuthorityUtils.createAuthorityList("ROLE_CANDIDATE"));
        when(service.needsConsent("an.nguyen")).thenReturn(true);

        assertTrue(advice.policyConsentRequired(candidate));
    }

    @Test
    @DisplayName("Ứng viên đã đồng ý -> Không popup")
    void policyConsentRequired_shouldBeFalse_whenCandidateAgreed() {
        var candidate = UsernamePasswordAuthenticationToken.authenticated("an.nguyen", null,
                AuthorityUtils.createAuthorityList("ROLE_CANDIDATE"));
        when(service.needsConsent("an.nguyen")).thenReturn(false);

        assertFalse(advice.policyConsentRequired(candidate));
    }
}
