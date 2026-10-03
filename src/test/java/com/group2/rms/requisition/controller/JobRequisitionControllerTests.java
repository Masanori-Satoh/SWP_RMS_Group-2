package com.group2.rms.requisition.controller;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.service.RequisitionService;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * STAGE 3 — CONTROLLER / MVC TEST.
 * Controller: RequisitionController
 * Kiểm tra các luồng: List, Form, Detail, Create, Edit, Delete, Decision, Withdraw, CSRF và Security.
 */
@WebMvcTest(controllers = RequisitionController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class JobRequisitionControllerTests {

    @Autowired private MockMvc mvc;

    @MockitoBean private RequisitionService service;
    @MockitoBean private RequisitionAccess access;
    @MockitoBean private DepartmentRepository departments;
    @MockitoBean private UserRepository userRepository;

    private User hiringManager;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().roleId(1).roleName("Hiring Manager").build();
        Department dept = Department.builder().departmentId(1).departmentName("IT").build();
        hiringManager = User.builder()
                .userId(10)
                .username("hm")
                .fullName("Nguyen Van A")
                .role(role)
                .department(dept)
                .build();

        lenient().when(access.actor()).thenReturn(hiringManager);
        lenient().when(access.canCreate(any())).thenReturn(true);
        lenient().when(departments.findAll()).thenReturn(List.of(dept));
    }

    // =========================================================================
    // 1. GET Requests: List, Create Form, Detail
    // =========================================================================

    @Test
    @WithMockUser(roles = "HIRING_MANAGER")
    @DisplayName("GET /requisitions -> Trả về view list và model phân trang")
    void list_returnsListViewAndModel() throws Exception {
        when(service.search(anyInt(), anyInt(), anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(service.countVisible()).thenReturn(0L);

        mvc.perform(get("/requisitions"))
                .andExpect(status().isOk())
                .andExpect(view().name("requisitions/list"))
                .andExpect(model().attributeExists("requisitions", "currentPage", "totalPages", "search"));
    }

    @Test
    @WithMockUser(roles = "HIRING_MANAGER")
    @DisplayName("GET /requisitions/create -> Trả về view form tạo mới với DTO mặc định")
    void createForm_returnsFormView() throws Exception {
        mvc.perform(get("/requisitions/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("requisitions/form"))
                .andExpect(model().attribute("isEdit", false))
                .andExpect(model().attributeExists("requisitionDto"));
    }

    @Test
    @WithMockUser(roles = "HIRING_MANAGER")
    @DisplayName("GET /requisitions/{id} -> Trả về view chi tiết với model req")
    void detail_returnsDetailView() throws Exception {
        RequisitionResponse detail = RequisitionResponse.builder()
                .requisitionId(1)
                .title("Backend Engineer")
                .approvalStatus("Draft")
                .build();
        when(service.getById(1)).thenReturn(detail);

        mvc.perform(get("/requisitions/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("requisitions/detail"))
                .andExpect(model().attributeExists("req"));
    }

    // =========================================================================
    // 2. POST Requests: Create (Draft & Submit), Edit, Delete, Decision, Withdraw
    // =========================================================================

    @Test
    @WithMockUser(roles = "HIRING_MANAGER")
    @DisplayName("POST /requisitions/create (Draft) -> Gọi service, redirect tới detail và có flash message")
    void create_saveDraft_success() throws Exception {
        when(service.createRequisition(any(RequisitionRequest.class))).thenReturn(10);

        mvc.perform(post("/requisitions/create")
                        .with(csrf())
                        .param("action", "draft")
                        .param("title", "New Dev"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/requisitions/10"))
                .andExpect(flash().attribute("successMessage", "Draft saved."));

        verify(service).createRequisition(any(RequisitionRequest.class));
    }

    @Test
    @WithMockUser(roles = "HIRING_MANAGER")
    @DisplayName("POST /requisitions/create -> Lỗi validation trả lại form và hiển thị field error")
    void create_validationError_returnsForm() throws Exception {
        doThrow(new RequisitionValidationException(Map.of("title", "Title is required")))
                .when(service).createRequisition(any());

        mvc.perform(post("/requisitions/create")
                        .with(csrf())
                        .param("action", "submit"))
                .andExpect(status().isOk())
                .andExpect(view().name("requisitions/form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("requisitionDto", "title"));
    }

    @Test
    @WithMockUser(roles = "HIRING_MANAGER")
    @DisplayName("POST /requisitions/edit/{id} -> Update thành công redirect về detail")
    void update_success_redirectsToDetail() throws Exception {
        mvc.perform(post("/requisitions/edit/10")
                        .with(csrf())
                        .param("version", "1")
                        .param("action", "submit")
                        .param("title", "Updated Title"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/requisitions/10"))
                .andExpect(flash().attribute("successMessage", "Request submitted to Director."));

        verify(service).updateRequisition(eq(10), any(RequisitionRequest.class));
    }

    @Test
    @WithMockUser(roles = "HIRING_MANAGER")
    @DisplayName("POST /requisitions/delete/{id} -> Xóa thành công redirect về list")
    void delete_success_redirectsToList() throws Exception {
        mvc.perform(post("/requisitions/delete/10")
                        .with(csrf())
                        .param("version", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/requisitions"))
                .andExpect(flash().attribute("successMessage", "Requisition deleted."));

        verify(service).deleteRequisition(10, 2L);
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    @DisplayName("POST /requisitions/{id}/decision -> Director quyết định redirect về detail")
    void decision_approve_success() throws Exception {
        mvc.perform(post("/requisitions/10/decision")
                        .with(csrf())
                        .param("version", "1")
                        .param("decision", "approve")
                        .param("comment", "Looks good"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/requisitions/10"))
                .andExpect(flash().attribute("successMessage", "Decision saved."));

        verify(service).decide(10, 1L, true, "Looks good");
    }

    @Test
    @WithMockUser(roles = "HIRING_MANAGER")
    @DisplayName("POST /requisitions/{id}/withdraw -> Rút request thành công redirect về detail")
    void withdraw_success_redirectsToDetail() throws Exception {
        mvc.perform(post("/requisitions/10/withdraw")
                        .with(csrf())
                        .param("version", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/requisitions/10"))
                .andExpect(flash().attribute("successMessage", "Request withdrawn. You can edit the draft."));

        verify(service).withdraw(10, 1L);
    }

    // =========================================================================
    // 3. Security & CSRF Checks
    // =========================================================================

    @Test
    @WithMockUser(roles = "HIRING_MANAGER")
    @DisplayName("POST thiếu CSRF token -> Bị chặn với mã 403 Forbidden")
    void postWithoutCsrf_isForbidden() throws Exception {
        mvc.perform(post("/requisitions/create")
                        .param("title", "No CSRF"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    @DisplayName("Role CANDIDATE truy cập /requisitions -> Bị chặn 403 theo SecurityConfig")
    void candidateRole_cannotAccessRequisitions() throws Exception {
        mvc.perform(get("/requisitions"))
                .andExpect(status().isForbidden());
    }
}
