package com.group2.rms.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Request format failures for explicitly scoped routes. Runs before the shared MVC
 * resolver, including mapping failures (405/415) that have no HandlerMethod.
 * Business failures stay in module controllers; security/system failures retain existing handling.
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ModuleRequestExceptionResolver implements HandlerExceptionResolver {
    private final ObjectMapper mapper;
    private static final List<String> ROOTS = List.of("/admin/accounts", "/admin/candidate-accounts",
            "/admin/departments", "/admin/audit-logs", "/talent-pool", "/profile",
            "/register", "/forgot-password", "/reset-password", "/login");
    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                         Object handler, Exception exception) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String root = ROOTS.stream().filter(value -> path.equals(value) || path.startsWith(value + "/"))
                .findFirst().orElse(null);
        if (root == null || response.isCommitted()) return null;
        Failure failure = failure(exception);
        if (failure == null) return null;

        if (exception instanceof HttpRequestMethodNotSupportedException method && method.getSupportedMethods() != null) {
            response.setHeader("Allow", String.join(", ", method.getSupportedMethods()));
        }
        Object result = ModuleErrorResponse.reject(request, response, handler, failure.status(), failure.message(),
                root.equals("/reset-password") ? "/forgot-password" : root);
        if (result instanceof ModelAndView view) return view;
        var json = (ResponseEntity<?>) result;
        response.setStatus(json.getStatusCode().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        try {
            mapper.writeValue(response.getWriter(), json.getBody());
        } catch (IOException writeFailure) {
            throw new IllegalStateException("Cannot write request error response", writeFailure);
        }
        return new ModelAndView();
    }

    private static Failure failure(Exception exception) {
        if (exception instanceof MethodArgumentTypeMismatchException) {
            return new Failure(400, "Tham số không đúng định dạng. Vui lòng kiểm tra lại bộ lọc hoặc đường dẫn.");
        }
        if (exception instanceof ServletRequestBindingException || exception instanceof MissingServletRequestPartException) {
            return new Failure(400, "Yêu cầu thiếu thông tin bắt buộc. Vui lòng kiểm tra và gửi lại.");
        }
        if (exception instanceof BindException) {
            return new Failure(400, "Dữ liệu nhập vào không đúng định dạng. Vui lòng kiểm tra lại.");
        }
        if (exception instanceof HandlerMethodValidationException validation && !validation.isForReturnValue()) {
            return new Failure(400, "Dữ liệu gửi lên không hợp lệ. Vui lòng kiểm tra lại.");
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return new Failure(400, "Dữ liệu gửi lên không đúng định dạng. Vui lòng kiểm tra và gửi lại.");
        }
        if (exception instanceof MaxUploadSizeExceededException) {
            return new Failure(413, "Tệp tải lên vượt quá dung lượng cho phép. Vui lòng chọn tệp nhỏ hơn.");
        }
        if (exception instanceof HttpMediaTypeNotSupportedException) {
            return new Failure(415, "Định dạng dữ liệu gửi lên không được hỗ trợ. Vui lòng tải lại trang và thử lại.");
        }
        if (exception instanceof HttpMediaTypeNotAcceptableException) {
            return new Failure(406, "Định dạng phản hồi được yêu cầu không được hỗ trợ. Vui lòng tải lại trang.");
        }
        if (exception instanceof HttpRequestMethodNotSupportedException) {
            return new Failure(405, "Thao tác không hợp lệ cho đường dẫn này. Vui lòng thực hiện bằng nút trên màn hình.");
        }
        return null;
    }

    private record Failure(int status, String message) { }
}
