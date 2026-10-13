package com.group2.rms.offer;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.offer.controller.CandidateOfferController;
import com.group2.rms.offer.dto.CandidateOfferResponse;
import com.group2.rms.offer.exception.OfferResponseException;
import com.group2.rms.offer.service.CandidateOfferService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Trang "Thư mời nhận việc" của ứng viên qua Spring MVC + Spring Security thật; service là mock.
 */
@WebMvcTest(CandidateOfferController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class CandidateOfferWebTests {

    @Autowired MockMvc mvc;
    @MockitoBean CandidateOfferService candidateOffers;
    @MockitoBean UserRepository users;

    @Test
    void guestIsSentToLogin() throws Exception {
        mvc.perform(get("/portal/offers"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        verifyNoInteractions(candidateOffers);
    }

    @Test
    void internalStaffIsForbiddenFromListAndAnswers() throws Exception {
        for (String role : List.of("HR", "Director", "Hiring Manager", "System Admin")) {
            mvc.perform(get("/portal/offers").with(as("staff", role))).andExpect(status().isForbidden());
            mvc.perform(post("/portal/offers/1/accept").with(as("staff", role)).with(csrf()))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/portal/offers/1/decline").with(as("staff", role)).with(csrf()))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(candidateOffers);
    }

    @Test
    void candidateSeesAnswerButtonsOnlyOnOffersWaitingForThem() throws Exception {
        when(candidateOffers.list()).thenReturn(List.of(
                offer(12, "Junior Java Developer", "Chờ bạn trả lời", "badge--warning", true),
                offer(8, "UI/UX Designer", "Đã nhận offer", "badge--success", false)));

        mvc.perform(get("/portal/offers").with(as("an.vo", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(view().name("offers/candidate-list"))
                .andExpect(model().attribute("activeMenu", "offers"))
                .andExpect(content().string(containsString("Thư mời nhận việc")))
                .andExpect(content().string(containsString("Junior Java Developer")))
                .andExpect(content().string(containsString("20.000.000")))
                .andExpect(content().string(containsString("01/11/2026")))
                .andExpect(content().string(containsString("Chờ bạn trả lời")))
                .andExpect(content().string(containsString("action=\"/portal/offers/12/accept\"")))
                .andExpect(content().string(containsString("action=\"/portal/offers/12/decline\"")))
                .andExpect(content().string(not(containsString("/portal/offers/8/accept"))))
                .andExpect(content().string(not(containsString("/portal/offers/8/decline"))))
                .andExpect(content().string(containsString("id=\"cof-dialog\"")))
                .andExpect(content().string(not(containsString("style=\""))));
    }

    @Test
    void candidateWithoutOffersSeesEmptyState() throws Exception {
        when(candidateOffers.list()).thenReturn(List.of());

        mvc.perform(get("/portal/offers").with(as("new.candidate", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bạn chưa nhận được thư mời nào")))
                .andExpect(content().string(not(containsString("<table"))));
    }

    @Test
    void acceptRedirectsWithSuccessMessage() throws Exception {
        when(candidateOffers.accept(12)).thenReturn("Junior Java Developer");

        mvc.perform(post("/portal/offers/12/accept").with(as("an.vo", "Candidate")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/offers"))
                .andExpect(flash().attribute("successMessage", "Bạn đã nhận offer vị trí Junior Java Developer."));
    }

    @Test
    void declineRedirectsWithSuccessMessage() throws Exception {
        when(candidateOffers.decline(12)).thenReturn("Junior Java Developer");

        mvc.perform(post("/portal/offers/12/decline").with(as("an.vo", "Candidate")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/offers"))
                .andExpect(flash().attribute("successMessage", "Bạn đã từ chối offer vị trí Junior Java Developer."));
    }

    @Test
    void refusedAnswerRedirectsWithErrorMessage() throws Exception {
        when(candidateOffers.accept(8)).thenThrow(new OfferResponseException("Thư mời này đã được trả lời."));
        when(candidateOffers.decline(8)).thenThrow(new OfferResponseException("Thư mời này đã được trả lời."));

        mvc.perform(post("/portal/offers/8/accept").with(as("an.vo", "Candidate")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portal/offers"))
                .andExpect(flash().attribute("errorMessage", "Thư mời này đã được trả lời."));
        mvc.perform(post("/portal/offers/8/decline").with(as("an.vo", "Candidate")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "Thư mời này đã được trả lời."));
    }

    @Test
    void answeringSomeoneElsesOfferIsNotFound() throws Exception {
        when(candidateOffers.accept(99)).thenThrow(new ResourceNotFoundException("Không tìm thấy thư mời nhận việc."));

        mvc.perform(post("/portal/offers/99/accept").with(as("an.vo", "Candidate")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void answerWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/portal/offers/12/accept").with(as("an.vo", "Candidate")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(candidateOffers);
    }

    private static CandidateOfferResponse offer(int id, String postingTitle, String statusLabel, String badge,
                                                boolean canRespond) {
        return new CandidateOfferResponse(id, postingTitle, postingTitle, new BigDecimal("20000000"),
                new BigDecimal("18000000"), LocalDate.of(2026, 11, 1), "Hà Nội", "Bảo hiểm đầy đủ",
                statusLabel, badge, canRespond);
    }

    private RequestPostProcessor as(String username, String roleName) {
        User account = User.builder().userId(1).username(username).fullName(username).accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
        when(users.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(account));
        String authority = "ROLE_" + roleName.toUpperCase().replace(' ', '_');
        return user(username).authorities(() -> authority);
    }
}
