package com.group2.rms.core.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Tiện ích dùng chung cho template (có sẵn ở mọi view). */
@ControllerAdvice
public class ViewHelpersAdvice {

    /** Link phân trang giữ nguyên bộ lọc: dùng bởi fragments/ui/pagination :: paged(page). */
    @ModelAttribute("pageLinks")
    public PageLinks pageLinks(HttpServletRequest request) {
        return new PageLinks(request.getRequestURI(), request.getQueryString());
    }
}
