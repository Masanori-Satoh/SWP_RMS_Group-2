package com.group2.rms.offer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.exception.BaseBusinessException;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.offer.controller.OfferController;
import com.group2.rms.offer.dto.CreateOfferRequest;
import com.group2.rms.offer.dto.OfferDetailResponse;
import com.group2.rms.offer.dto.OfferResponse;
import com.group2.rms.offer.dto.PassedCandidateResponse;
import com.group2.rms.offer.dto.UpdateOfferRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test cho tầng Web / REST API của phân hệ Offer Proposal:
 * Kiểm tra các endpoint bảo mật, CSRF, HTTP response codes, Bean validation và quy tắc GBR-07.
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
    }

    @Test
    @DisplayName("IT-01 GET /api/v1/hr/offers/passed-candidates: Trả về danh sách ứng viên đỗ hợp lệ theo GBR-07")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it01_getPassedCandidates_returnsOk() throws Exception {
        PassedCandidateResponse cand = PassedCandidateResponse.builder()
                .applicationId(10)
                .candidateName("Nguyễn Văn A")
                .appliedPosition("Senior Java Backend Engineer")
                .departmentName("Engineering")
                .existingOfferStatus(null)
                .build();

        when(offerService.getPassedCandidatesForOffer()).thenReturn(List.of(cand));

        mvc.perform(get("/api/v1/hr/offers/passed-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].candidateName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data[0].existingOfferStatus").doesNotExist());
    }

    @Test
    @DisplayName("IT-02 POST /api/v1/hr/offers: Tạo Offer hợp lệ trả về 201 Created")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it02_createOffer_validPayload_returnsCreated() throws Exception {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(10)
                .offeredPositionTitle("Senior Java Backend Engineer")
                .proposedSalary(new BigDecimal("30000000"))
                .probationSalary(new BigDecimal("26000000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(7))
                .workLocation("Tầng 8, Tòa nhà RMS Tower, Hà Nội")
                .isDraft(false)
                .build();

        OfferResponse response = OfferResponse.builder()
                .offerId(101)
                .applicationId(10)
                .offeredPositionTitle("Senior Java Backend Engineer")
                .proposedSalary(new BigDecimal("30000000"))
                .probationSalary(new BigDecimal("26000000"))
                .offerStatus("Pending_Director")
                .build();

        when(offerService.createOfferByHr(any(CreateOfferRequest.class))).thenReturn(response);

        mvc.perform(post("/api/v1/hr/offers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offerId").value(101))
                .andExpect(jsonPath("$.data.offerStatus").value("Pending_Director"));
    }

    @Test
    @DisplayName("IT-03 POST /api/v1/hr/offers: Lương thử việc lớn hơn lương chính thức vi phạm Bean Validation -> 400 Bad Request")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it03_createOffer_probationSalaryGreaterThanProposed_returnsBadRequest() throws Exception {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(10)
                .offeredPositionTitle("Senior Java Backend Engineer")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("25000000")) // Vi phạm: 25M > 20M
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(7))
                .workLocation("Trụ sở chính")
                .isDraft(false)
                .build();

        mvc.perform(post("/api/v1/hr/offers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("IT-04 POST /api/v1/hr/offers: Tạo đè khi đơn có Offer thuộc Nhóm B bị chặn trả về lỗi")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it04_createOffer_groupBLockedState_returnsError() throws Exception {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(10)
                .offeredPositionTitle("Senior Java Backend Engineer")
                .proposedSalary(new BigDecimal("30000000"))
                .probationSalary(new BigDecimal("26000000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(7))
                .workLocation("Trụ sở chính")
                .isDraft(false)
                .build();

        when(offerService.createOfferByHr(any(CreateOfferRequest.class)))
                .thenThrow(new BaseBusinessException("Offer đang ở trạng thái Nhóm B", "OFFER_LOCKED_STATE"));

        mvc.perform(post("/api/v1/hr/offers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT-05 PUT /api/v1/hr/offers/{id}: Cập nhật Offer hợp lệ trả về 200 OK")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it05_updateOffer_valid_returnsOk() throws Exception {
        UpdateOfferRequest request = UpdateOfferRequest.builder()
                .offeredPositionTitle("Tech Lead")
                .proposedSalary(new BigDecimal("45000000"))
                .probationSalary(new BigDecimal("39000000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(14))
                .workLocation("Trụ sở chính")
                .isDraft(true)
                .build();

        OfferResponse response = OfferResponse.builder()
                .offerId(1)
                .offeredPositionTitle("Tech Lead")
                .proposedSalary(new BigDecimal("45000000"))
                .probationSalary(new BigDecimal("39000000"))
                .offerStatus("Draft")
                .build();

        when(offerService.updateOfferByHr(eq(1), any(UpdateOfferRequest.class))).thenReturn(response);

        mvc.perform(put("/api/v1/hr/offers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offerStatus").value("Draft"))
                .andExpect(jsonPath("$.data.offeredPositionTitle").value("Tech Lead"));
    }

    @Test
    @DisplayName("IT-06 POST /api/v1/hr/offers/{id}/send: HR phát hành Offer Letter thành công trả về 200 OK")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it06_sendOfferToCandidate_returnsOk() throws Exception {
        OfferResponse sentOffer = OfferResponse.builder()
                .offerId(1)
                .offerStatus("Sent_Candidate")
                .build();

        when(offerService.sendOfferToCandidate(1)).thenReturn(sentOffer);

        mvc.perform(post("/api/v1/hr/offers/1/send").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offerStatus").value("Sent_Candidate"));
    }

    @Test
    @DisplayName("IT-07 Security: Truy cập API khi chưa đăng nhập bị chuyển hướng hoặc chặn 401/403")
    void it07_unauthenticatedAccess_isForbiddenOrRedirected() throws Exception {
        mvc.perform(get("/api/v1/hr/offers/passed-candidates"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("IT-08 GET /api/v1/hr/offers: Lấy danh sách Offer có phân trang và lọc theo trạng thái trả về 200 OK")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it08_getAllOffers_withStatusAndPaging_returnsOk() throws Exception {
        OfferResponse offer = OfferResponse.builder()
                .offerId(1)
                .offerStatus("Draft")
                .offeredPositionTitle("Senior Developer")
                .build();
        PageImpl<OfferResponse> pagedResult = new PageImpl<>(List.of(offer), PageRequest.of(0, 10), 1);

        when(offerService.getAllOffersForHr(eq("Draft"), any())).thenReturn(pagedResult);

        mvc.perform(get("/api/v1/hr/offers").param("status", "Draft").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].offerStatus").value("Draft"));
    }

    @Test
    @DisplayName("IT-09 GET /api/v1/hr/offers/{id}: Lấy chi tiết Offer đầy đủ trả về 200 OK")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it09_getOfferDetail_returnsOk() throws Exception {
        OfferDetailResponse detail = OfferDetailResponse.builder()
                .offerId(1)
                .offeredPositionTitle("Senior Developer")
                .candidateName("Nguyễn Văn A")
                .offerStatus("Pending_Director")
                .proposedSalary(new BigDecimal("30000000"))
                .build();

        when(offerService.getOfferDetailForHr(1)).thenReturn(detail);

        mvc.perform(get("/api/v1/hr/offers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.offerId").value(1))
                .andExpect(jsonPath("$.data.candidateName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.offeredPositionTitle").value("Senior Developer"));
    }

    @Test
    @DisplayName("IT-10 DELETE /api/v1/hr/offers/{id}: Xóa bản thảo Offer (Draft) thành công trả về 200 OK")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it10_deleteDraftOffer_returnsOk() throws Exception {
        mvc.perform(delete("/api/v1/hr/offers/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Đã xóa bản thảo Offer thành công")));
    }

    @Test
    @DisplayName("IT-11 DELETE /api/v1/hr/offers/{id}: Cố tình xóa Offer không phải Draft trả về lỗi 400 Bad Request")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it11_deleteNonDraftOffer_returnsBadRequest() throws Exception {
        doThrow(new BaseBusinessException("Chỉ được phép xóa bản thảo Offer (Draft)", "OFFER_NOT_DRAFT"))
                .when(offerService).deleteDraftOfferByHr(1);

        mvc.perform(delete("/api/v1/hr/offers/1").with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("IT-12 POST /api/v1/hr/offers/{id}/send: Gửi Offer chưa được duyệt trả về lỗi 400 Bad Request")
    @WithMockUser(username = "hr_specialist", roles = {"HR"})
    void it12_sendOffer_notApproved_returnsBadRequest() throws Exception {
        when(offerService.sendOfferToCandidate(1))
                .thenThrow(new BaseBusinessException("Chỉ được gửi thư mời nhận việc khi Offer đã được Director phê duyệt", "OFFER_NOT_APPROVED"));

        mvc.perform(post("/api/v1/hr/offers/1/send").with(csrf()))
                .andExpect(status().is4xxClientError());
    }
}
