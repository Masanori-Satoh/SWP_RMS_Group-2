package com.group2.rms.core.security;

import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountSessionGuardFilterTests {
    private final UserRepository users = mock(UserRepository.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final AccountSessionGuardFilter filter = new AccountSessionGuardFilter(users);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("manager", "unused",
                        List.of(new SimpleGrantedAuthority("ROLE_HIRING_MANAGER"))));
    }

    @Test
    @DisplayName("SEC-SESSION-01 | Cho đi tiếp đúng một lần, không lookup và không redirect")
    void doFilter_noAuthentication_continuesWithoutLookup() throws Exception {
        // Given: Request chưa có Authentication.
        SecurityContextHolder.clearContext();
        // When: Chạy filter với request/response trong bộ nhớ.
        filter.doFilter(request, response, chain);
        // Then: Cho đi tiếp đúng một lần, không lookup và không redirect.
        verify(chain, times(1)).doFilter(request, response);
        verify(users, times(0)).findByUsernameIgnoreCase(any());
        assertEquals(null, response.getRedirectedUrl());
    }

    @Test
    @DisplayName("SEC-SESSION-02 | Cho đi tiếp đúng một lần và không lookup")
    void doFilter_unauthenticatedToken_continuesWithoutLookup() throws Exception {
        // Given: Request có token chưa xác thực.
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("manager", "unused"));
        // When: Chạy filter.
        filter.doFilter(request, response, chain);
        // Then: Cho đi tiếp đúng một lần và không lookup.
        verify(chain, times(1)).doFilter(request, response);
        verify(users, times(0)).findByUsernameIgnoreCase(any());
    }

    @Test
    @DisplayName("SEC-SESSION-03 | Cho đi tiếp và không lookup tài khoản anonymous")
    void doFilter_anonymousToken_continuesWithoutLookup() throws Exception {
        // Given: Request dùng AnonymousAuthenticationToken.
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        // When: Chạy filter.
        filter.doFilter(request, response, chain);
        // Then: Cho đi tiếp và không lookup tài khoản anonymous.
        verify(chain, times(1)).doFilter(request, response);
        verify(users, times(0)).findByUsernameIgnoreCase(any());
    }

    @Test
    @DisplayName("SEC-SESSION-04 | Session còn hiệu lực, không redirect và chain được gọi một lần")
    void doFilter_activeAccountWithSameRole_preservesSessionAndContinues() throws Exception {
        // Given: Account Active và role DB khớp authority trong session.
        authenticate();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        User account = User.builder().accountStatus("Active")
                .role(Role.builder().roleName("Hiring Manager").build()).build();
        when(users.findByUsernameIgnoreCase("manager")).thenReturn(Optional.of(account));
        // When: Chạy filter.
        filter.doFilter(request, response, chain);
        // Then: Session còn hiệu lực, không redirect và chain được gọi một lần.
        assertEquals(false, session.isInvalid());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(null, response.getRedirectedUrl());
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("SEC-SESSION-05 | Session HR bị logout và chặn request")
    void doFilter_staleOrInvalidAccount_logsOutAndStopsChain_HR() throws Exception {
        // Given: Session đang có authority HM, nhưng account DB ở tình huống HR.
        authenticate();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        request.setContextPath("/rms");
        User account = User.builder().accountStatus("Active")
                .role(Role.builder().roleName("Hiring Manager").build()).build();
        account.getRole().setRoleName("HR");
        when(users.findByUsernameIgnoreCase("manager")).thenReturn(Optional.of(account));
        // When: Request đi qua filter.
        filter.doFilter(request, response, chain);
        // Then: Hủy session, xóa Authentication và dừng chain trước khi redirect.
        assertEquals(true, session.isInvalid());
        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
        assertEquals("/rms/login?session-expired", response.getRedirectedUrl());
        verify(chain, times(0)).doFilter(request, response);
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("SEC-SESSION-06 | Session Director bị logout và chặn request")
    void doFilter_staleOrInvalidAccount_logsOutAndStopsChain_Director() throws Exception {
        // Given: Session đang có authority HM, nhưng account DB ở tình huống Director.
        authenticate();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        request.setContextPath("/rms");
        User account = User.builder().accountStatus("Active")
                .role(Role.builder().roleName("Hiring Manager").build()).build();
        account.getRole().setRoleName("Director");
        when(users.findByUsernameIgnoreCase("manager")).thenReturn(Optional.of(account));
        // When: Request đi qua filter.
        filter.doFilter(request, response, chain);
        // Then: Hủy session, xóa Authentication và dừng chain trước khi redirect.
        assertEquals(true, session.isInvalid());
        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
        assertEquals("/rms/login?session-expired", response.getRedirectedUrl());
        verify(chain, times(0)).doFilter(request, response);
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("SEC-SESSION-07 | Session Unknown bị logout và chặn request")
    void doFilter_staleOrInvalidAccount_logsOutAndStopsChain_Unknown() throws Exception {
        // Given: Session đang có authority HM, nhưng account DB ở tình huống Unknown.
        authenticate();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        request.setContextPath("/rms");
        User account = User.builder().accountStatus("Active")
                .role(Role.builder().roleName("Hiring Manager").build()).build();
        account.getRole().setRoleName("Unknown");
        when(users.findByUsernameIgnoreCase("manager")).thenReturn(Optional.of(account));
        // When: Request đi qua filter.
        filter.doFilter(request, response, chain);
        // Then: Hủy session, xóa Authentication và dừng chain trước khi redirect.
        assertEquals(true, session.isInvalid());
        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
        assertEquals("/rms/login?session-expired", response.getRedirectedUrl());
        verify(chain, times(0)).doFilter(request, response);
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("SEC-SESSION-08 | Session NO_ROLE bị logout và chặn request")
    void doFilter_staleOrInvalidAccount_logsOutAndStopsChain_NO_ROLE() throws Exception {
        // Given: Session đang có authority HM, nhưng account DB ở tình huống NO_ROLE.
        authenticate();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        request.setContextPath("/rms");
        User account = User.builder().accountStatus("Active")
                .role(Role.builder().roleName("Hiring Manager").build()).build();
        account.setRole(null);
        when(users.findByUsernameIgnoreCase("manager")).thenReturn(Optional.of(account));
        // When: Request đi qua filter.
        filter.doFilter(request, response, chain);
        // Then: Hủy session, xóa Authentication và dừng chain trước khi redirect.
        assertEquals(true, session.isInvalid());
        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
        assertEquals("/rms/login?session-expired", response.getRedirectedUrl());
        verify(chain, times(0)).doFilter(request, response);
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("SEC-SESSION-09 | Session NULL_ROLE_NAME bị logout và chặn request")
    void doFilter_staleOrInvalidAccount_logsOutAndStopsChain_NULL_ROLE_NAME() throws Exception {
        // Given: Session đang có authority HM, nhưng account DB ở tình huống NULL_ROLE_NAME.
        authenticate();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        request.setContextPath("/rms");
        User account = User.builder().accountStatus("Active")
                .role(Role.builder().roleName("Hiring Manager").build()).build();
        account.getRole().setRoleName(null);
        when(users.findByUsernameIgnoreCase("manager")).thenReturn(Optional.of(account));
        // When: Request đi qua filter.
        filter.doFilter(request, response, chain);
        // Then: Hủy session, xóa Authentication và dừng chain trước khi redirect.
        assertEquals(true, session.isInvalid());
        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
        assertEquals("/rms/login?session-expired", response.getRedirectedUrl());
        verify(chain, times(0)).doFilter(request, response);
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("SEC-SESSION-10 | Session Inactive bị logout và chặn request")
    void doFilter_staleOrInvalidAccount_logsOutAndStopsChain_Inactive() throws Exception {
        // Given: Session đang có authority HM, nhưng account DB ở tình huống Inactive.
        authenticate();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        request.setContextPath("/rms");
        User account = User.builder().accountStatus("Active")
                .role(Role.builder().roleName("Hiring Manager").build()).build();
        account.setAccountStatus("Inactive");
        when(users.findByUsernameIgnoreCase("manager")).thenReturn(Optional.of(account));
        // When: Request đi qua filter.
        filter.doFilter(request, response, chain);
        // Then: Hủy session, xóa Authentication và dừng chain trước khi redirect.
        assertEquals(true, session.isInvalid());
        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
        assertEquals("/rms/login?session-expired", response.getRedirectedUrl());
        verify(chain, times(0)).doFilter(request, response);
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("SEC-SESSION-11 | Session Blocked bị logout và chặn request")
    void doFilter_staleOrInvalidAccount_logsOutAndStopsChain_Blocked() throws Exception {
        // Given: Session đang có authority HM, nhưng account DB ở tình huống Blocked.
        authenticate();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        request.setContextPath("/rms");
        User account = User.builder().accountStatus("Active")
                .role(Role.builder().roleName("Hiring Manager").build()).build();
        account.setAccountStatus("Blocked");
        when(users.findByUsernameIgnoreCase("manager")).thenReturn(Optional.of(account));
        // When: Request đi qua filter.
        filter.doFilter(request, response, chain);
        // Then: Hủy session, xóa Authentication và dừng chain trước khi redirect.
        assertEquals(true, session.isInvalid());
        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
        assertEquals("/rms/login?session-expired", response.getRedirectedUrl());
        verify(chain, times(0)).doFilter(request, response);
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("SEC-SESSION-12 | Session REMOVED bị logout và chặn request")
    void doFilter_staleOrInvalidAccount_logsOutAndStopsChain_REMOVED() throws Exception {
        // Given: Session đang có authority HM, nhưng account DB ở tình huống REMOVED.
        authenticate();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        request.setContextPath("/rms");
        User account = User.builder().accountStatus("Active")
                .role(Role.builder().roleName("Hiring Manager").build()).build();
        account.getRole().setRoleName("REMOVED");
        when(users.findByUsernameIgnoreCase("manager")).thenReturn(Optional.empty());
        // When: Request đi qua filter.
        filter.doFilter(request, response, chain);
        // Then: Hủy session, xóa Authentication và dừng chain trước khi redirect.
        assertEquals(true, session.isInvalid());
        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
        assertEquals("/rms/login?session-expired", response.getRedirectedUrl());
        verify(chain, times(0)).doFilter(request, response);
        verify(users, times(0)).save(any());
    }

    @Test
    @DisplayName("SEC-SESSION-13 | Giữ nguyên exception và không cho request đi tiếp")
    void doFilter_repositoryFailure_propagatesWithoutContinuing() throws Exception {
        // Given: Repository ném lỗi khi tìm account.
        authenticate();
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(users.findByUsernameIgnoreCase("manager")).thenThrow(failure);
        // When: Chạy filter.
        Exception error = assertThrows(IllegalStateException.class, () -> filter.doFilter(request, response, chain));
        // Then: Giữ nguyên exception và không cho request đi tiếp.
        assertEquals(failure, error);
        verify(chain, times(0)).doFilter(request, response);
    }

}

