package com.group2.rms.offer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.offer.controller.OfferController;
import com.group2.rms.offer.dto.*;
import com.group2.rms.offer.service.OfferExportService;
import com.group2.rms.offer.service.OfferService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test cho tầng Web / MVC của phân hệ Offer Proposal:
 * Kiểm tra các endpoint bảo mật, điều hướng View, xuất Excel và quy tắc phân quyền.
 */
@WebMvcTest(OfferController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class OfferIntegrationTests {

    private static void assertOfferStyles(String html, String roleStylesheet, String page) throws Exception {
        var matcher = java.util.regex.Pattern.compile("href=\"(/css/[^\"]+\\.css)\"").matcher(html);
        var links = new java.util.ArrayList<String>();
        while (matcher.find()) links.add(matcher.group(1));
        String common = "/css/pages/offer-common.css";
        String role = "/css/pages/" + roleStylesheet;
        org.junit.jupiter.api.Assertions.assertEquals(1, java.util.Collections.frequency(links, common));
        org.junit.jupiter.api.Assertions.assertEquals(1, java.util.Collections.frequency(links, role));
        org.junit.jupiter.api.Assertions.assertTrue(links.indexOf(common) < links.indexOf(role));
        org.junit.jupiter.api.Assertions.assertEquals(links.size(), new java.util.HashSet<>(links).size());
        org.junit.jupiter.api.Assertions.assertFalse(html.contains("/css/pages/" + "offers/"));
        String directory = System.getProperty("offer.styles.evidence.dir");
        if (directory != null) {
            var path = java.nio.file.Path.of(directory);
            java.nio.file.Files.createDirectories(path);
            java.nio.file.Files.writeString(path.resolve(page + ".html"), html);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"offer-common.css", "offer-hr.css", "offer-director.css"})
    void flatOfferStylesAreServedWithoutChangingContent(String filename) throws Exception {
        byte[] expected = java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("src/main/resources/static/css/pages", filename));
        mvc.perform(get("/css/pages/" + filename))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/css"))
                .andExpect(content().bytes(expected));
        mvc.perform(get("/css/pages/" + "offers/" + filename)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/interviews", "/offers", "/requisitions", "/internal/job-postings", "/applications"})
    void systemAdminCannotOpenRecruitmentModules(String path) throws Exception {
        mvc.perform(get(path).with(user("admin").authorities(() -> "ROLE_SYSTEM_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    void hrCannotDecideOffer(String action) throws Exception {
        mvc.perform(post("/offers/1/" + action).with(user("hr_specialist").authorities(() -> "ROLE_HR"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"comments\":\"Denied\"}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /offers/create",
            "GET, /offers/1/edit",
            "POST, /offers/create",
            "POST, /offers/1/edit",
            "PUT, /offers/1",
            "DELETE, /offers/1",
            "POST, /offers/1/delete",
            "POST, /offers/1/send"
    })
    void directorCannotManageHrOffers(String method, String path) throws Exception {
        for (MediaType contentType : List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED)) {
            mvc.perform(request(HttpMethod.valueOf(method), path)
                            .with(user("director_user").roles("DIRECTOR")).with(csrf()).contentType(contentType))
                    .andExpect(status().isForbidden());
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OfferService offerService;

    @MockitoBean
    private OfferExportService offerExportService;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        Role hrRole = Role.builder().roleId(2).roleName("HR").build();
        User hrUser = User.builder()
                .userId(1)
                .username("hr_specialist")
                .accountStatus("Active")
                .role(hrRole)
                .build();
        when(userRepository.findByUsernameIgnoreCase("hr_specialist")).thenReturn(Optional.of(hrUser));

        Role candRole = Role.builder().roleId(4).roleName("Candidate").build();
        User candUser = User.builder()
                .userId(2)
                .username("cand_user")
                .accountStatus("Active")
                .role(candRole)
                .build();
        when(userRepository.findByUsernameIgnoreCase("cand_user")).thenReturn(Optional.of(candUser));

        Role dirRole = Role.builder().roleId(3).roleName("Director").build();
        User dirUser = User.builder()
                .userId(3)
                .username("director_user")
                .accountStatus("Active")
                .role(dirRole)
                .build();
        when(userRepository.findByUsernameIgnoreCase("director_user")).thenReturn(Optional.of(dirUser));

        User adminUser = User.builder().userId(4).username("admin").accountStatus("Active")
                .role(Role.builder().roleName("System Admin").build()).build();
        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(adminUser));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HR", "DIRECTOR"})
    void expiredOffersRenderWithTheirOwnFilterAndLabel(String role) throws Exception {
        var page = new PageImpl<>(List.of(OfferResponse.builder().offerId(42)
                .offerStatus("Expired").responseExpired(true).offeredPositionTitle("Test position").build()));
        when(offerService.getAllOffersForHr(any(), any(), any(), any())).thenReturn(page);
        when(offerService.getDirectorOffers(any(), any(), any(), any())).thenReturn(page);
        when(offerService.getOfferStats()).thenReturn(Map.of());
        when(offerService.getDirectorOfferStats()).thenReturn(Map.of());
        when(offerService.getDirectorStatusCounts()).thenReturn(Map.of("Expired", 1L));
        mvc.perform(get("/offers").param("status", "Expired")
                        .with(user("HR".equals(role) ? "hr_specialist" : "director_user").roles(role)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentStatus", "Expired"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"Expired\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đã quá hạn phản hồi")));
    }

    @Test
    @DisplayName("IT-01 GET /offers: Danh sách Offer trả về View offers/list và HTTP 200")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it01_listOffers_returnsOkView() throws Exception {
        when(offerService.getAllOffersForHr(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(offerService.getPassedCandidatesForOffer()).thenReturn(Collections.emptyList());
        when(offerService.getOfferStats()).thenReturn(Map.of(
                "statTotal", 10L,
                "statPending", 2L,
                "statApproved", 3L,
                "statSent", 4L,
                "statAccepted", 1L
        ));

        mvc.perform(get("/offers"))
                .andExpect(status().isOk())
                .andExpect(view().name("offers/list"))
                .andExpect(model().attributeExists("offers", "statTotal", "statPending"))
                .andExpect(model().attribute("statTotal", 10L))
                .andExpect(model().attribute("currentStatus", "ALL"));
    }

    @Test
    @DisplayName("IT-01b GET /offers?status=Sent_Candidate: Lọc theo status và giữ nguyên statTotal toàn cục")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it01b_listOffers_withStatusFilter_returnsFilteredViewAndPreservesGlobalStats() throws Exception {
        when(offerService.getAllOffersForHr(any(), eq("Sent_Candidate"), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(), org.springframework.data.domain.PageRequest.of(0, 10), 0));
        when(offerService.getPassedCandidatesForOffer()).thenReturn(Collections.emptyList());
        when(offerService.getOfferStats()).thenReturn(Map.of(
                "statTotal", 15L,
                "statPending", 1L,
                "statApproved", 2L,
                "statSent", 5L,
                "statAccepted", 3L
        ));

        mvc.perform(get("/offers").param("status", "Sent_Candidate"))
                .andExpect(status().isOk())
                .andExpect(view().name("offers/list"))
                .andExpect(model().attribute("statTotal", 15L))
                .andExpect(model().attribute("currentStatus", "Sent_Candidate"))
                .andExpect(model().attribute("totalElements", 0L));
    }

    @Test
    @DisplayName("IT-02 GET /offers/create: Màn hình tạo Offer trả về View offers/form")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it02_showCreateForm_returnsFormView() throws Exception {
        when(offerService.getPassedCandidatesForOffer()).thenReturn(Collections.emptyList());

        mvc.perform(get("/offers/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("offers/form"))
                .andExpect(result -> assertOfferStyles(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8), "offer-hr.css", "form"))
                .andExpect(model().attributeExists("offerDto", "passedCandidates"));
    }

    @Test
    @DisplayName("IT-03 GET /offers/{id}: Xem chi tiết Offer trả về View offers/detail")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it03_showOfferDetail_returnsDetailView() throws Exception {
        OfferDetailResponse detail = OfferDetailResponse.builder()
                .offerId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .candidateName("Nguyễn Văn A")
                .offerStatus("Draft")
                .proposedSalary(new BigDecimal("30000000"))
                .probationSalary(new BigDecimal("25500000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(14))
                .build();
        when(offerService.getOfferDetailForHr(1)).thenReturn(detail);

        mvc.perform(get("/offers/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("offers/detail"))
                .andExpect(result -> assertOfferStyles(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8), "offer-director.css", "detail"))
                .andExpect(model().attributeExists("detail"));
    }

    @ParameterizedTest
    @CsvSource({
            "hr_specialist, HR, Draft, true, false",
            "hr_specialist, HR, Director_Approved, false, true",
            "hr_specialist, HR, Sent_Candidate, false, false",
            "director_user, DIRECTOR, Draft, false, false",
            "director_user, DIRECTOR, Director_Approved, false, false"
    })
    void sharedConfirmationsKeepRoleAndStatusVisibility(String username, String role, String offerStatus,
                                                       boolean deleteVisible, boolean sendVisible) throws Exception {
        when(offerService.getOfferDetailForHr(1)).thenReturn(OfferDetailResponse.builder()
                .offerId(1).offerStatus(offerStatus).candidateName("Candidate")
                .proposedSalary(new BigDecimal("30000000")).probationSalary(new BigDecimal("25500000"))
                .expectedStartDate(LocalDate.now().plusDays(14)).build());

        String html = mvc.perform(get("/offers/1").with(user(username).roles(role)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(deleteVisible, html.contains("id=\"deleteOfferDialog\""));
        org.junit.jupiter.api.Assertions.assertEquals(sendVisible, html.contains("id=\"sendOfferConfirmDialog\""));
        if (deleteVisible) {
            org.junit.jupiter.api.Assertions.assertTrue(html.contains("form=\"deleteOfferForm\""));
        }
        if (sendVisible) {
            org.junit.jupiter.api.Assertions.assertTrue(html.contains("aria-labelledby=\"sendOfferTitle\""));
        }
    }

    @Test
    @DisplayName("IT-04 POST /offers/{id}/delete: Xóa bản thảo Offer điều hướng về /offers")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it04_deleteDraftOffer_redirectsToList() throws Exception {
        mvc.perform(post("/offers/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/offers"));
    }

    @Test
    @DisplayName("IT-05 POST /offers/{id}/send: Phát hành Offer Letter điều hướng về /offers/{id}")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it05_sendOffer_redirectsToDetail() throws Exception {
        when(offerService.sendOfferToCandidate(1)).thenReturn(OfferResponse.builder()
                .emailStatus(OfferResponse.EmailStatus.NOT_SENT).build());
        mvc.perform(post("/offers/1/send").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/offers/1"))
                .andExpect(flash().attribute("offerWarningMessage",
                        "Đã phát hành thư mời nhận việc trên cổng ứng viên. Chưa gửi được email thông báo."));
    }

    @Test
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void hrListDoesNotLoadTheCreateFormCandidateDropdown() throws Exception {
        when(offerService.getAllOffersForHr(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(offerService.getOfferStats()).thenReturn(Map.of());
        mvc.perform(get("/offers")).andExpect(status().isOk())
                .andExpect(view().name("offers/list"));
        org.mockito.Mockito.verify(offerService, org.mockito.Mockito.never()).getPassedCandidatesForOffer();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/offers/create", "/offers/1/edit"})
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void createAndEditUseOnlyFormSubmissions(String path) throws Exception {
        mvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        org.mockito.Mockito.verifyNoInteractions(offerService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELECTED", "FILTERED", "ALL"})
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void exportAcceptsPayloadGeneratedByTheActualFrontend(String scope) throws Exception {
        String script = """
                const fs = require('fs'), vm = require('vm');
                const scope = process.argv[1];
                const context = {
                  window: {location: {search: '?search=Java&status=Sent_Candidate&timeSort=LATEST'}}, console, URLSearchParams,
                  document: {
                    readyState: 'loading', addEventListener() {},
                    getElementById(id) {
                      return {searchInput: {value: 'Unsubmitted'}, statusFilter: {value: 'Accepted'},
                              timeSortFilter: {value: 'EARLIEST'}}[id] || null;
                    },
                    querySelector() { return {value: scope}; },
                    querySelectorAll(selector) {
                      if (selector.includes('exportCol')) return [{value: 'offerId'}];
                      if (selector.includes('offer-row-checkbox')) return [{value: '42'}];
                      return [];
                    }
                  },
                  fetch(url, options) {
                    process.stdout.write(options.body);
                    return Promise.reject(new Error('Dry run: no network request'));
                  }
                };
                vm.createContext(context);
                vm.runInContext(fs.readFileSync('src/main/resources/static/js/pages/offers/offer-hr.js', 'utf8'), context);
                context.executeOfferExport();
                """;
        Process process = new ProcessBuilder("node", "-e", script, scope).redirectErrorStream(true).start();
        try {
            org.junit.jupiter.api.Assertions.assertTrue(process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS));
            String payload = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            org.junit.jupiter.api.Assertions.assertEquals(0, process.exitValue(), payload);
            OfferExportRequest expected = new OfferExportRequest(OfferExportScope.valueOf(scope),
                    "SELECTED".equals(scope) ? List.of(42) : List.of(),
                    "FILTERED".equals(scope) ? new OfferExportFilterRequest("Java", "Sent_Candidate", "LATEST") : null,
                    List.of("offerId"));
            org.junit.jupiter.api.Assertions.assertEquals(expected, objectMapper.readValue(payload, OfferExportRequest.class));
            when(offerExportService.exportOffersToExcel(any(), any())).thenReturn(new byte[]{1});
            when(offerExportService.generateExportFilename(any())).thenReturn("offers.xlsx");
            mvc.perform(post("/offers/export").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                    .andExpect(status().isOk());
            org.mockito.Mockito.verify(offerExportService).exportOffersToExcel(eq(expected), eq("hr_specialist"));
        } finally {
            process.destroyForcibly();
        }
    }

    @ParameterizedTest
    @CsvSource({"hr_specialist,HR,Draft", "director_user,DIRECTOR,Pending_Director"})
    void keywordSearchKeepsTheExistingToolbarAndRoleFilters(String username, String role, String offerStatus) throws Exception {
        OfferResponse offer = OfferResponse.builder().offerId(12).candidateName("Ứng viên kiểm thử")
                .candidateEmail("offer.search@example.test").offeredPositionTitle("Java Backend")
                .proposedSalary(new BigDecimal("20000000")).probationSalary(new BigDecimal("17000000"))
                .probationDays(60).offerStatus(offerStatus).createdAt(java.time.LocalDateTime.of(2026, 11, 10, 10, 0)).build();
        var page = new PageImpl<>(List.of(offer), org.springframework.data.domain.PageRequest.of(0, 10), 25);
        when(offerService.getAllOffersForHr(any(), any(), any(), any())).thenReturn(page);
        when(offerService.getDirectorOffers(any(), any(), any(), any())).thenReturn(page);
        when(offerService.getOfferStats()).thenReturn(Map.of());
        when(offerService.getDirectorOfferStats()).thenReturn(Map.of());
        when(offerService.getDirectorStatusCounts()).thenReturn(Map.of());
        String keyword = "offer.search@example.test";
        String html = mvc.perform(get("/offers").with(user(username).roles(role))
                        .queryParam("search", keyword).queryParam("status", offerStatus).queryParam("timeSort", "LATEST"))
                .andExpect(status().isOk()).andExpect(model().attribute("search", keyword))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("maxlength=\"120\""));
        assertOfferStyles(html, "HR".equals(role) ? "offer-hr.css" : "offer-director.css", role.toLowerCase());
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("Tìm #mã thư mời, ứng viên, email, vị trí, phòng ban…"));
        org.junit.jupiter.api.Assertions.assertEquals(1, html.split("id=\"offerFilters\"", -1).length - 1);
        org.junit.jupiter.api.Assertions.assertEquals("HR".equals(role), html.contains("id=\"offerExportModal\""));
        var nextPage = java.util.regex.Pattern.compile("href=\"([^\"]*page=1[^\"]*)\"").matcher(html);
        org.junit.jupiter.api.Assertions.assertTrue(nextPage.find(), "A second page link must be available");
        org.junit.jupiter.api.Assertions.assertTrue(nextPage.group(1).contains("search=offer.search"));
        org.junit.jupiter.api.Assertions.assertTrue(nextPage.group(1).contains("status=" + offerStatus));
        org.junit.jupiter.api.Assertions.assertTrue(nextPage.group(1).contains("timeSort=LATEST"));
        if ("HR".equals(role)) org.mockito.Mockito.verify(offerService).getAllOffersForHr(eq(keyword), eq(offerStatus), eq("LATEST"), any());
        else org.mockito.Mockito.verify(offerService).getDirectorOffers(eq(keyword), eq(offerStatus), eq("LATEST"), any());
        // Tùy chọn xuất HTML do Thymeleaf render để kiểm tra browser; không tạo dữ liệu database.
        String evidenceDirectory = System.getProperty("offer.search.evidence.dir");
        if (evidenceDirectory != null) {
            var directory = java.nio.file.Path.of(evidenceDirectory);
            java.nio.file.Files.createDirectories(directory);
            java.nio.file.Files.writeString(directory.resolve(role.toLowerCase() + ".html"), html);
        }
    }

    @Test
    @DisplayName("IT-06 POST /offers/export: HR xuất Excel thành công (HTTP 200 OK, Content-Type xlsx, attachment header)")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it06_exportOffers_hrSuccess() throws Exception {
        OfferExportRequest request = new OfferExportRequest(
                OfferExportScope.FILTERED,
                null,
                new OfferExportFilterRequest("Java", "ALL", "DEFAULT"),
                List.of("offerId", "candidateName", "proposedSalary")
        );
        byte[] mockBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
        when(offerExportService.exportOffersToExcel(any(), any())).thenReturn(mockBytes);
        when(offerExportService.generateExportFilename(any())).thenReturn("offers_filtered_2026-10-07.xlsx");

        mvc.perform(post("/offers/export")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"offers_filtered_2026-10-07.xlsx\""))
                .andExpect(content().contentTypeCompatibleWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(mockBytes));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALL", "FILTERED", "SELECTED"})
    @WithMockUser(username = "director_user", roles = {"DIRECTOR"})
    void directorCannotExportOffers(String scope) throws Exception {
        OfferExportRequest request = new OfferExportRequest(
                OfferExportScope.valueOf(scope), List.of(1),
                new OfferExportFilterRequest("", "ALL", "DEFAULT"), List.of("offerId"));

        mvc.perform(post("/offers/export").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        org.mockito.Mockito.verifyNoInteractions(offerExportService);
    }

    @Test
    @DisplayName("IT-07 POST /offers/export: Candidate bị chặn truy cập (403 Forbidden)")
    @WithMockUser(username = "cand_user", roles = {"CANDIDATE"})
    void it07_exportOffers_unauthorizedRole_forbidden() throws Exception {
        OfferExportRequest request = new OfferExportRequest(
                OfferExportScope.ALL,
                null,
                null,
                List.of("offerId")
        );

        mvc.perform(post("/offers/export")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IT-08 POST /offers/export: Payload thiếu scope trả về lỗi 400 Bad Request")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it08_exportOffers_invalidPayload_badRequest() throws Exception {
        String invalidJson = "{\"scope\":null,\"columns\":[\"offerId\"]}";

        mvc.perform(post("/offers/export")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("IT-09 Security: Chưa đăng nhập truy cập /offers bị điều hướng về login")
    void it09_unauthenticatedAccess_redirectsToLogin() throws Exception {
        mvc.perform(get("/offers"))
                .andExpect(status().is3xxRedirection());
    }



    @Test
    @DisplayName("IT-12 GET /offers/{id} (JSON): Pop-up chi tiết Offer trả về JSON ApiResponse và HTTP 200")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it12_getOfferDetailJson_success() throws Exception {
        OfferDetailResponse detail = OfferDetailResponse.builder()
                .offerId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .candidateName("Nguyễn Văn A")
                .offerStatus("Draft")
                .proposedSalary(new BigDecimal("30000000"))
                .probationSalary(new BigDecimal("25500000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(14))
                .build();
        when(offerService.getOfferDetailForHr(1)).thenReturn(detail);

        mvc.perform(get("/offers/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offerId").value(1))
                .andExpect(jsonPath("$.data.candidateName").value("Nguyễn Văn A"));
    }


    @Test
    @DisplayName("IT-14 DELETE /offers/{id} (JSON): Pop-up xóa bản thảo Offer thành công trả về HTTP 200")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it14_deleteDraftOfferJson_success() throws Exception {
        mvc.perform(delete("/offers/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("IT-15 POST /offers/{id}/send (JSON): Phát hành Offer Letter trả về JSON khi Accept application/json")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it15_sendOfferJson_success() throws Exception {
        OfferResponse mockSent = OfferResponse.builder()
                .emailStatus(OfferResponse.EmailStatus.SENT)
                .offerId(1)
                .offerStatus("Sent_Candidate")
                .build();
        when(offerService.sendOfferToCandidate(1)).thenReturn(mockSent);

        mvc.perform(post("/offers/1/send")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offerStatus").value("Sent_Candidate"))
                .andExpect(jsonPath("$.data.emailStatus").value("SENT"));
    }
    @Test
    @DisplayName("IT-16 POST /offers/{id}/approve (JSON): Director phê duyệt Offer thành công trả về HTTP 200")
    @WithMockUser(username = "director_user", roles = {"DIRECTOR"})
    void it16_approveOfferJson_success() throws Exception {
        DirectorDecisionRequest req = new DirectorDecisionRequest("Đồng ý với mức lương này.");

        OfferResponse mockApproved = OfferResponse.builder()
                .offerId(1)
                .offerStatus("Director_Approved")
                .build();
        when(offerService.approveOfferByDirector(1, "Đồng ý với mức lương này.")).thenReturn(mockApproved);

        mvc.perform(post("/offers/1/approve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offerStatus").value("Director_Approved"));
    }

    @Test
    @DisplayName("IT-17 POST /offers/{id}/reject (JSON): Director từ chối Offer thành công trả về HTTP 200")
    @WithMockUser(username = "director_user", roles = {"DIRECTOR"})
    void it17_rejectOfferJson_success() throws Exception {
        DirectorDecisionRequest req = new DirectorDecisionRequest("Mức lương quá cao.");

        OfferResponse mockRejected = OfferResponse.builder()
                .offerId(1)
                .offerStatus("Director_Rejected")
                .build();
        when(offerService.rejectOfferByDirector(1, "Mức lương quá cao.")).thenReturn(mockRejected);

        mvc.perform(post("/offers/1/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offerStatus").value("Director_Rejected"));
    }

    @Test
    @DisplayName("IT-17b POST /offers/{id}/reject (JSON): Từ chối không nhập lý do bị từ chối trả về HTTP 400 Bad Request")
    @WithMockUser(username = "director_user", roles = {"DIRECTOR"})
    void it17b_rejectOfferJson_emptyComments_returnsBadRequest() throws Exception {
        DirectorDecisionRequest req = new DirectorDecisionRequest("   ");

        mvc.perform(post("/offers/1/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Vui lòng nhập lý do từ chối vào ô nhận xét trước khi tiếp tục."));
    }

    @Test
    @DisplayName("IT-18 GET /offers: Ứng viên (ROLE_CANDIDATE) bị chặn truy cập danh sách Offer nội bộ trả về HTTP 403 Forbidden")
    @WithMockUser(username = "cand_user", roles = {"CANDIDATE"})
    void it18_candidateAccessOffersList_isForbidden() throws Exception {
        mvc.perform(get("/offers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IT-19 GET /offers/{id}: Ứng viên (ROLE_CANDIDATE) bị chặn truy cập chi tiết Offer nội bộ trả về HTTP 403 Forbidden")
    @WithMockUser(username = "cand_user", roles = {"CANDIDATE"})
    void it19_candidateAccessOfferDetail_isForbidden() throws Exception {
        mvc.perform(get("/offers/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IT-20 POST /offers/create: Ứng viên (ROLE_CANDIDATE) bị chặn tạo Offer trả về HTTP 403 Forbidden")
    @WithMockUser(username = "cand_user", roles = {"CANDIDATE"})
    void it20_candidateCreateOffer_isForbidden() throws Exception {
        mvc.perform(post("/offers/create").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IT-21 POST /offers/create: Nhập sai định dạng ngày hoặc năm ngoài phạm vi (như 20165) trả về thông báo lỗi ngắn gọn tiếng Việt, không chứa ngoại lệ kỹ thuật Java")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it21_createOfferWithInvalidDate_showsShortConciseErrorMessage() throws Exception {
        mvc.perform(post("/offers/create").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf())
                        .param("applicationId", "1")
                        .param("offeredPositionTitle", "Java Dev")
                        .param("proposedSalary", "20000000")
                        .param("probationSalary", "18000000")
                        .param("probationDays", "60")
                        .param("workLocation", "Hà Nội")
                        .param("expectedStartDate", "20165-07-14"))
                .andExpect(status().isOk())
                .andExpect(view().name("offers/form"))
                .andExpect(model().attributeHasFieldErrors("offerDto", "expectedStartDate"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ngày bắt đầu dự kiến không hợp lệ.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Failed to convert property value"))));
    }

    // =========================================================================
    // EXTENDED INTEGRATION TESTS (English)
    // =========================================================================

    @Test
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void unexpectedCreateErrorDoesNotExposeTechnicalDetailsAndPreservesTheForm() throws Exception {
        when(offerService.createOfferByHr(any())).thenThrow(new IllegalStateException("SQL_INTERNAL_DETAILS"));
        when(offerService.getPassedCandidatesForOffer()).thenReturn(List.of());
        mvc.perform(post("/offers/create").with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("applicationId", "1").param("offeredPositionTitle", "Java Dev")
                        .param("proposedSalary", "20000000").param("probationSalary", "18000000")
                        .param("probationDays", "60").param("responsePeriodDays", "5")
                        .param("workLocation", "Hà Nội")
                        .param("expectedStartDate", LocalDate.now().plusDays(14).toString()))
                .andExpect(status().isOk()).andExpect(view().name("offers/form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vui lòng thử lại hoặc liên hệ bộ phận hỗ trợ.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQL_INTERNAL_DETAILS"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Java Dev")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"validation", "business", "technical"})
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void createErrorsKeepTheLockedCandidateAndEnteredData(String errorKind) throws Exception {
        var candidate = com.group2.rms.offer.dto.PassedCandidateResponse.builder()
                .applicationId(1).candidateName("Nguyễn Văn A").appliedPosition("Java")
                .departmentName("IT").finalDecision("Passed").build();
        when(offerService.getPassedCandidatesForOffer()).thenReturn(List.of(candidate));
        if (!"validation".equals(errorKind)) {
            RuntimeException error = "business".equals(errorKind)
                    ? new com.group2.rms.offer.exception.OfferValidationException("Hồ sơ đã được cập nhật.")
                    : new IllegalStateException("INTERNAL_FAILURE");
            when(offerService.createOfferByHr(any())).thenThrow(error);
        }
        mvc.perform(post("/offers/create").with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("isLockedCandidate", "true").param("applicationId", "1")
                        .param("offeredPositionTitle", "Entered title")
                        .param("proposedSalary", "validation".equals(errorKind) ? "0" : "20000000")
                        .param("probationSalary", "18000000").param("probationDays", "60")
                        .param("responsePeriodDays", "5").param("workLocation", "Entered office")
                        .param("expectedStartDate", LocalDate.now().plusDays(14).toString()))
                .andExpect(status().isOk()).andExpect(view().name("offers/form"))
                .andExpect(model().attributeHasErrors("offerDto"))
                .andExpect(model().attribute("isLockedCandidate", true))
                .andExpect(model().attribute("fixedCandidate", candidate))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Entered title")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Entered office")));
    }

    @Test
    @DisplayName("IT-22 POST /offers/{id}/approve: Candidate role calling director approve endpoint is forbidden (HTTP 403)")
    @WithMockUser(username = "cand_user", roles = {"CANDIDATE"})
    void it22_candidateApproveOffer_isForbidden() throws Exception {
        DirectorDecisionRequest req = new DirectorDecisionRequest("Approve");
        mvc.perform(post("/offers/1/approve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IT-23 POST /offers/{id}/reject: Candidate role calling director reject endpoint is forbidden (HTTP 403)")
    @WithMockUser(username = "cand_user", roles = {"CANDIDATE"})
    void it23_candidateRejectOffer_isForbidden() throws Exception {
        DirectorDecisionRequest req = new DirectorDecisionRequest("Reject");
        mvc.perform(post("/offers/1/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IT-24 GET /offers with negative page number: Normalizes page number to 0 and returns OK")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it24_listOffers_negativePageNumber_normalizesToPageZero() throws Exception {
        when(offerService.getAllOffersForHr(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(offerService.getPassedCandidatesForOffer()).thenReturn(Collections.emptyList());

        mvc.perform(get("/offers").param("page", "-5"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentPage", 0));
    }

    @Test
    @DisplayName("IT-25 GET /offers/{id}/edit: HR xem form chỉnh sửa Offer thành công (view offers/form, HTTP 200)")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it25_getOfferEditForm_returnsOkView() throws Exception {
        UpdateOfferRequest req = UpdateOfferRequest.builder()
                .offeredPositionTitle("Frontend ReactJS Developer")
                .proposedSalary(new BigDecimal("25000000"))
                .probationSalary(new BigDecimal("21250000"))
                .probationDays(60)
                .responsePeriodDays(5)
                .expectedStartDate(LocalDate.now().plusDays(10))
                .workLocation("Ha Noi")
                .benefitsPackage("Bonus")
                .build();

        OfferDetailResponse detail = OfferDetailResponse.builder()
                .offerId(11)
                .candidateName("Đinh Văn Hiếu")
                .candidateEmail("hieu.van.dinh@gmail.com")
                .candidatePhone("0931604451")
                .appliedPosition("Frontend ReactJS Developer")
                .departmentName("Engineering")
                .requisitionId(2)
                .offerStatus("Draft")
                .build();

        when(offerService.getUpdateOfferRequestById(eq(11))).thenReturn(req);
        when(offerService.getOfferDetailForHr(eq(11))).thenReturn(detail);

        mvc.perform(get("/offers/11/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("offers/form"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attribute("isLockedCandidate", false))
                .andExpect(model().attributeExists("offerDto", "detail"));
    }

    @ParameterizedTest
    @CsvSource({"hr_specialist,HR,Draft,true,false", "director_user,DIRECTOR,Draft,false,false",
            "hr_specialist,HR,Director_Approved,false,true", "director_user,DIRECTOR,Director_Approved,false,false"})
    void detailOnlyShowsManagementActionsToHr(String username, String role, String offerStatus,
                                               boolean deleteVisible, boolean sendVisible) throws Exception {
        when(offerService.getOfferDetailForHr(1)).thenReturn(OfferDetailResponse.builder()
                .offerId(1).offerStatus(offerStatus).candidateName("Test candidate")
                .proposedSalary(new BigDecimal("20000000")).probationSalary(new BigDecimal("18000000"))
                .expectedStartDate(LocalDate.now().plusDays(14)).build());
        String html = mvc.perform(get("/offers/1").with(user(username).roles(role)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(deleteVisible, html.contains("action=\"/offers/1/delete\""));
        org.junit.jupiter.api.Assertions.assertEquals(sendVisible, html.contains("onclick=\"confirmSendOffer(1)\""));
    }

}
