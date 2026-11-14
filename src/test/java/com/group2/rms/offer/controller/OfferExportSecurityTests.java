package com.group2.rms.offer.controller;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.offer.dto.OfferExportRequest;
import com.group2.rms.offer.dto.OfferExportScope;
import com.group2.rms.offer.dto.OfferResponse;
import com.group2.rms.offer.exception.OfferValidationException;
import com.group2.rms.offer.service.OfferExportService;
import com.group2.rms.offer.service.OfferService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real application security chain; workbook rendering is covered by OfferExportServiceTests. */
@WebMvcTest(OfferController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class OfferExportSecurityTests {
    private static final String EXPORT = "/offers/export";
    private static final String REQUEST = "{\"scope\":\"ALL\",\"columns\":[\"offerId\"]}";

    @Autowired private MockMvc mvc;
    @MockitoBean private OfferService offers;
    @MockitoBean private OfferExportService exporter;
    @MockitoBean private UserRepository users;

    private RequestPostProcessor account(String roleName) {
        User account = User.builder().userId(7).username("export.user").fullName("Export Test")
                .accountStatus("Active").role(Role.builder().roleName(roleName).build()).build();
        when(users.findByUsernameIgnoreCase("export.user")).thenReturn(Optional.of(account));
        return user("export.user").authorities(new SimpleGrantedAuthority(RoleAuthorities.fromRoleName(roleName)));
    }

    @Test
    void export_shouldReturnDownloadForHrWithValidCsrf() throws Exception {
        byte[] workbook = {80, 75, 3, 4};
        when(exporter.exportOffersToExcel(any(), eq("export.user"))).thenReturn(workbook);
        when(exporter.generateExportFilename(any())).thenReturn("offers.xlsx");

        mvc.perform(post(EXPORT).with(account("HR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"offers.xlsx\""))
                .andExpect(content().contentTypeCompatibleWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(workbook));

        verify(exporter).exportOffersToExcel(argThat(request -> request.scope() == OfferExportScope.ALL
                && request.columns().equals(List.of("offerId"))), eq("export.user"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EMPTY_EXPORT_DATA", "EMPTY_SELECTED_OFFERS", "INVALID_EXPORT_COLUMN"})
    void export_shouldReturnBusinessFailureAsJsonWithoutDownload(String errorCode) throws Exception {
        String message = switch (errorCode) {
            case "EMPTY_EXPORT_DATA" -> "Không có dữ liệu thư mời nhận việc phù hợp để xuất Excel.";
            case "EMPTY_SELECTED_OFFERS" -> "Vui lòng chọn ít nhất một thư mời để xuất dữ liệu.";
            default -> "Cột dữ liệu yêu cầu không hợp lệ hoặc không được phép xuất: invalidColumn";
        };
        when(exporter.exportOffersToExcel(any(), eq("export.user")))
                .thenThrow(new OfferValidationException(message, errorCode));
        String request = "{\"scope\":\"FILTERED\",\"filters\":{\"keyword\":\"OE13_NO_MATCH_2026\","
                + "\"status\":\"ALL\",\"sort\":\"DEFAULT\"},\"columns\":[\"offerId\"]}";

        mvc.perform(post(EXPORT).with(account("HR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(header().doesNotExist("Content-Disposition"));

        verify(exporter, never()).generateExportFilename(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Director", "Hiring Manager", "Interviewer", "Candidate", "System Admin"})
    void export_shouldRejectEveryNonHrRoleBeforeInvokingExporter(String roleName) throws Exception {
        mvc.perform(post(EXPORT).with(account(roleName)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isForbidden());

        verifyNoInteractions(exporter);
    }

    @Test
    void export_shouldRejectDirectorAtFilterEvenWhenBodyCannotBeBound() throws Exception {
        // A controller-only guard would allow binding to fail with 400 before checking the role.
        mvc.perform(post(EXPORT).with(account("Director")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("not-json"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(exporter);
    }

    @Test
    void export_shouldRequireCsrfEvenForHr() throws Exception {
        mvc.perform(post(EXPORT).with(account("HR")).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isForbidden());

        verifyNoInteractions(exporter);
    }

    @Test
    void export_shouldRequireAuthentication() throws Exception {
        mvc.perform(post(EXPORT).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(exporter);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Director", "Hiring Manager", "Interviewer", "Candidate", "System Admin"})
    void controllerGuard_shouldAlsoRejectNonHrWhenInvokedDirectly(String roleName) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated("export.user", "test",
                List.of(new SimpleGrantedAuthority(RoleAuthorities.fromRoleName(roleName))));
        var request = new OfferExportRequest(OfferExportScope.ALL, null, null, List.of("offerId"));

        assertEquals(HttpStatus.FORBIDDEN,
                new OfferController(offers, exporter).exportOffers(request, authentication).getStatusCode());

        verifyNoInteractions(exporter);
    }

    @Test
    void controllerGuard_shouldRejectMissingUnauthenticatedAndNonAuthenticationPrincipals() {
        var controller = new OfferController(offers, exporter);
        var request = new OfferExportRequest(OfferExportScope.ALL, null, null, List.of("offerId"));

        assertEquals(HttpStatus.FORBIDDEN, controller.exportOffers(request, null).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.exportOffers(request, () -> "export.user").getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.exportOffers(request,
                UsernamePasswordAuthenticationToken.unauthenticated("export.user", "test")).getStatusCode());

        verifyNoInteractions(exporter);
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    void director_shouldStillBeAbleToDecideOffers(String decision) throws Exception {
        OfferResponse response = OfferResponse.builder().offerId(41)
                .offerStatus("approve".equals(decision) ? "Director_Approved" : "Director_Rejected").build();
        if ("approve".equals(decision)) {
            when(offers.approveOfferByDirector(41, "Decision reason")).thenReturn(response);
        } else {
            when(offers.rejectOfferByDirector(41, "Decision reason")).thenReturn(response);
        }

        mvc.perform(post("/offers/41/" + decision).with(account("Director")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content("{\"comments\":\"Decision reason\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void hr_shouldNotGainDirectorApprovalPermission() throws Exception {
        mvc.perform(post("/offers/41/approve").with(account("HR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comments\":\"Decision reason\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(offers);
    }
}
