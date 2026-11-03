package com.group2.rms.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

class ErrorPagesTests {

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/interviews");

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Ứng viên bị chặn ở cổng (403 qua /error) -> trang error/403 có câu thông báo, nút về đơn của tôi")
    void resolver_candidateForbidden_rendersMessageAndCandidateHome() {
        loginAs("an.vo", "ROLE_CANDIDATE");

        ModelAndView mav = new RmsErrorViewResolver().resolveErrorView(request, HttpStatus.FORBIDDEN, Map.of());

        assertThat(mav.getViewName()).isEqualTo("error/403");
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(mav.getModel().get("message")).asString().contains("không có quyền");
        assertThat(mav.getModel()).containsEntry("homeUrl", "/portal/applications");
    }

    @Test
    @DisplayName("Nhân sự nội bộ -> nút về /dashboard")
    void view_internalUser_homeIsDashboard() {
        loginAs("hr_lan", "ROLE_HR");

        ModelAndView mav = ErrorPages.view(HttpStatus.FORBIDDEN, request);

        assertThat(mav.getModel()).containsEntry("homeUrl", "/dashboard");
    }

    @Test
    @DisplayName("Chưa đăng nhập -> nút về trang chủ")
    void view_anonymous_homeIsRoot() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        ModelAndView mav = ErrorPages.view(HttpStatus.NOT_FOUND, request);

        assertThat(mav.getViewName()).isEqualTo("error/404");
        assertThat(mav.getModel()).containsEntry("homeUrl", "/");
    }

    @Test
    @DisplayName("Câu thông báo riêng được giữ; câu rỗng thì dùng câu mặc định theo mã")
    void view_customMessageKept_blankFallsBackToDefault() {
        assertThat(ErrorPages.view(HttpStatus.NOT_FOUND, "Không tìm thấy lịch phỏng vấn #9.", request).getModel())
                .containsEntry("message", "Không tìm thấy lịch phỏng vấn #9.");
        assertThat(ErrorPages.view(HttpStatus.NOT_FOUND, " ", request).getModel())
                .containsEntry("message", ErrorPages.defaultMessage(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("Mỗi mã lỗi về đúng một trong 4 trang error/*")
    void viewName_mapsEveryStatusToOneOfFourPages() {
        assertThat(ErrorPages.viewName(HttpStatus.UNAUTHORIZED)).isEqualTo("error/403");
        assertThat(ErrorPages.viewName(HttpStatus.METHOD_NOT_ALLOWED)).isEqualTo("error/400");
        assertThat(ErrorPages.viewName(HttpStatus.CONFLICT)).isEqualTo("error/400");
        assertThat(ErrorPages.viewName(HttpStatus.SERVICE_UNAVAILABLE)).isEqualTo("error/500");
    }

    private void loginAs(String username, String authority) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                username, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }
}
