package com.group2.rms.auth;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@ControllerAdvice
public class NavigationAdvice {
    @ModelAttribute("sidebarAdmin")
    public boolean administrator(Authentication auth) {
        return auth!=null&&auth.getAuthorities().stream().anyMatch(a->"ROLE_SYSTEM_ADMIN".equals(a.getAuthority()));
    }
}
