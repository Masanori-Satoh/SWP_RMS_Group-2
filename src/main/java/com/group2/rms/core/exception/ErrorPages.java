package com.group2.rms.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

/**
 * Nguồn duy nhất dựng trang lỗi {@code error/400|403|404|500}.
 * Dùng chung cho hai đường: lỗi ném từ Controller/Service ({@link GlobalExceptionHandler})
 * và lỗi bị chặn trước Controller, ví dụ Spring Security từ chối theo URL ({@link RmsErrorViewResolver}).
 * Nhờ vậy trang lỗi nào cũng có câu giải thích và nút quay về đúng chỗ của vai trò đang đăng nhập.
 */
public final class ErrorPages {

    static final String CANDIDATE_AUTHORITY = "ROLE_CANDIDATE";

    private ErrorPages() {
    }

    /** Trang lỗi với câu thông báo mặc định theo mã lỗi. */
    public static ModelAndView view(HttpStatus status, HttpServletRequest request) {
        return view(status, null, request);
    }

    /** Trang lỗi với câu thông báo riêng; {@code message} rỗng thì dùng câu mặc định theo mã lỗi. */
    public static ModelAndView view(HttpStatus status, String message, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView(viewName(status));
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("message", message == null || message.isBlank() ? defaultMessage(status) : message);
        mav.addObject("url", request == null ? null : request.getRequestURL());
        addHomeLink(mav, SecurityContextHolder.getContext().getAuthentication());
        return mav;
    }

    static String viewName(HttpStatus status) {
        if (status == HttpStatus.NOT_FOUND) {
            return "error/404";
        }
        if (status == HttpStatus.FORBIDDEN || status == HttpStatus.UNAUTHORIZED) {
            return "error/403";
        }
        return status.is4xxClientError() ? "error/400" : "error/500";
    }

    static String defaultMessage(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> "Bạn cần đăng nhập để tiếp tục.";
            case FORBIDDEN -> "Bạn không có quyền truy cập vào chức năng hoặc tài nguyên này.";
            case NOT_FOUND -> "Trang hoặc dữ liệu bạn tìm không tồn tại, hoặc đã bị xóa.";
            case METHOD_NOT_ALLOWED -> "Thao tác này không được hỗ trợ cho đường dẫn này.";
            default -> status.is4xxClientError()
                    ? "Thao tác không hợp lệ ở trạng thái hiện tại. Vui lòng tải lại trang và thử lại."
                    : "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau.";
        };
    }

    /** Ứng viên về danh sách đơn của mình, nhân sự nội bộ về bảng điều khiển, khách về trang chủ. */
    static void addHomeLink(ModelAndView mav, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            mav.addObject("homeUrl", "/");
            mav.addObject("homeLabel", "Về trang chủ");
            return;
        }
        boolean candidate = authentication.getAuthorities().stream()
                .anyMatch(authority -> CANDIDATE_AUTHORITY.equals(authority.getAuthority()));
        mav.addObject("homeUrl", candidate ? "/portal/applications" : "/dashboard");
        mav.addObject("homeLabel", candidate ? "Về đơn ứng tuyển của tôi" : "Về bảng điều khiển");
        // TC-DETAIL-19: nhân sự nội bộ thấy trang lỗi trong khung nội bộ (sidebar + topbar), không phải khung công khai.
        // Trang lỗi không đi qua TopbarUserAdvice nên tự truyền vai trò (đọc từ authority) và tên đăng nhập.
        String roleName = internalRoleName(authentication);
        if (roleName != null) {
            mav.addObject("errorLayout", "layout/internal");
            mav.addObject("role", roleName);
            mav.addObject("fullName", authentication.getName());
        }
    }

    static String internalRoleName(Authentication authentication) {
        for (var authority : authentication.getAuthorities()) {
            switch (authority.getAuthority()) {
                case "ROLE_SYSTEM_ADMIN": return "System Admin";
                case "ROLE_HR": return "HR";
                case "ROLE_HIRING_MANAGER": return "Hiring Manager";
                case "ROLE_DIRECTOR": return "Director";
                case "ROLE_INTERVIEWER": return "Interviewer";
                default: break;
            }
        }
        return null;
    }
}
