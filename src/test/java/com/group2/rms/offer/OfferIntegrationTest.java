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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
class OfferIntegrationTest {

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
    }

    @Test
    @DisplayName("IT-01 GET /offers: Danh sách Offer trả về View offers/list và HTTP 200")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it01_listOffers_returnsOkView() throws Exception {
        when(offerService.getAllOffersForHr(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(offerService.getPassedCandidatesForOffer()).thenReturn(Collections.emptyList());

        mvc.perform(get("/offers"))
                .andExpect(status().isOk())
                .andExpect(view().name("offers/list"))
                .andExpect(model().attributeExists("offers", "statPending"));
    }

    @Test
    @DisplayName("IT-02 GET /offers/create: Màn hình tạo Offer trả về View offers/form")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it02_showCreateForm_returnsFormView() throws Exception {
        when(offerService.getPassedCandidatesForOffer()).thenReturn(Collections.emptyList());

        mvc.perform(get("/offers/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("offers/form"))
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
                .andExpect(model().attributeExists("detail"));
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
        mvc.perform(post("/offers/1/send").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/offers/1"));
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
    @DisplayName("IT-10 POST /offers/create (JSON): Modal tạo Offer thành công trả về HTTP 201 Created")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it10_createOfferJson_success() throws Exception {
        CreateOfferRequest req = new CreateOfferRequest();
        req.setApplicationId(10);
        req.setOfferedPositionTitle("Senior Java Dev");
        req.setProposedSalary(new BigDecimal("25000000"));
        req.setProbationSalary(new BigDecimal("21250000"));
        req.setProbationDays(60);
        req.setExpectedStartDate(LocalDate.now().plusDays(7));
        req.setWorkLocation("Trụ sở Mộc RMS");
        req.setIsDraft(false);

        OfferResponse mockCreated = OfferResponse.builder()
                .offerId(99)
                .candidateName("Trần Văn B")
                .offerStatus("Pending_Director")
                .build();
        when(offerService.createOfferByHr(any())).thenReturn(mockCreated);

        mvc.perform(post("/offers/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offerId").value(99));
    }

    @Test
    @DisplayName("IT-11 POST /offers/create (JSON): Lỗi nghiệp vụ từ chối tạo offer trả về HTTP 400")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it11_createOfferJson_businessError() throws Exception {
        CreateOfferRequest req = new CreateOfferRequest();
        req.setApplicationId(10);
        req.setOfferedPositionTitle("Senior Java Dev");
        req.setProposedSalary(new BigDecimal("25000000"));
        req.setProbationSalary(new BigDecimal("21250000"));
        req.setProbationDays(60);
        req.setExpectedStartDate(LocalDate.now().plusDays(7));
        req.setWorkLocation("Trụ sở Mộc RMS");
        req.setIsDraft(false);

        when(offerService.createOfferByHr(any()))
                .thenThrow(new com.group2.rms.core.exception.BaseBusinessException("Ứng viên đã có offer", "ERR_ACTIVE_OFFER"));

        mvc.perform(post("/offers/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Ứng viên đã có offer"));
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
    @DisplayName("IT-13 PUT /offers/{id} (JSON): Pop-up chỉnh sửa Offer thành công trả về HTTP 200")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it13_updateOfferJson_success() throws Exception {
        UpdateOfferRequest req = new UpdateOfferRequest();
        req.setOfferedPositionTitle("Lead Engineer");
        req.setProposedSalary(new BigDecimal("35000000"));
        req.setProbationSalary(new BigDecimal("29750000"));
        req.setProbationDays(60);
        req.setExpectedStartDate(LocalDate.now().plusDays(10));
        req.setWorkLocation("Trụ sở Mộc RMS");
        req.setIsDraft(true);

        OfferResponse mockUpdated = OfferResponse.builder()
                .offerId(1)
                .offeredPositionTitle("Lead Engineer")
                .offerStatus("Draft")
                .build();
        when(offerService.updateOfferByHr(any(), any())).thenReturn(mockUpdated);

        mvc.perform(put("/offers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offeredPositionTitle").value("Lead Engineer"));
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
                .offerId(1)
                .offerStatus("Sent_Candidate")
                .build();
        when(offerService.sendOfferToCandidate(1)).thenReturn(mockSent);

        mvc.perform(post("/offers/1/send")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offerStatus").value("Sent_Candidate"));
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
}
