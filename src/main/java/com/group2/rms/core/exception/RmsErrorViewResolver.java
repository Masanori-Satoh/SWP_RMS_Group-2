package com.group2.rms.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

/**
 * Trang lỗi cho các lỗi không đi qua {@link GlobalExceptionHandler}: Spring Security chặn theo URL
 * (ví dụ ứng viên mở {@code /interviews}), thiếu CSRF, đường dẫn không khớp Controller nào.
 * Spring Boot chuyển các lỗi này sang {@code /error}; bean này thay {@code DefaultErrorViewResolver}
 * để trang có câu thông báo tiếng Việt (Boot mặc định không đưa {@code message} ra ngoài).
 * Request JSON/AJAX vẫn nhận JSON của Boot như cũ, vì Boot chỉ gọi resolver cho request HTML.
 */
@Component
public class RmsErrorViewResolver implements ErrorViewResolver {

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        return ErrorPages.view(status, request);
    }
}
