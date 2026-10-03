package com.group2.rms;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.requisition.controller.RequisitionController;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.service.RequisitionService;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RequisitionController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class RequisitionReviewWebProbeTests {
    @Autowired MockMvc mvc;
    @MockitoBean RequisitionService service;
    @MockitoBean
    UserRepository users;
    @MockitoBean
    DepartmentRepository departments;
    private MockHttpSession candidateSession() {
        var user = User.builder().userId(123).username("review-candidate").accountStatus("Active")
            .role(Role.builder().roleName("Candidate").build()).build();
        when(users.findByUsernameIgnoreCase("review-candidate")).thenReturn(Optional.of(user));
        var auth = UsernamePasswordAuthenticationToken.authenticated("review-candidate", "", List.of(new SimpleGrantedAuthority(RoleAuthorities.fromRoleName("Candidate"))));
        var session = new MockHttpSession(); session.setAttribute("SPRING_SECURITY_CONTEXT", new SecurityContextImpl(auth)); return session;
    }
    @Test void candidateCanListAndDeleteUsingGetWithoutCsrf() throws Exception {
        var session = candidateSession();
        when(service.getAllRequisitions(1,10)).thenReturn(Page.empty());
        mvc.perform(get("/requisitions").session(session)).andExpect(status().isOk());
        mvc.perform(get("/requisitions/delete/999").session(session)).andExpect(status().is3xxRedirection());
        verify(service).deleteRequisition(999);
        System.out.println("WEB PROBE Candidate: list allowed, GET delete calls service without CSRF");
    }
    @Test void invalidSubmitIsForwardedToService() throws Exception {
        var session = candidateSession();
        var page = mvc.perform(get("/requisitions/create").session(session)).andExpect(status().isOk()).andReturn();
        var csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/requisitions/create").session(session).param(csrf.getParameterName(),csrf.getToken())
            .param("title","   ").param("action","submit"))
            .andExpect(status().is3xxRedirection());
        verify(service).createRequisition(argThat((RequisitionRequest d) -> d.getTitle().isBlank() && d.getDepartmentId()==null),eq(1));
        System.out.println("WEB PROBE submit: blank title and missing department forwarded without validation");
    }
}
