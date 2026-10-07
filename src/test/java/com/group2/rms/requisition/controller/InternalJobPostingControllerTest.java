package com.group2.rms.requisition.controller;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.requisition.dto.InternalJobPostingResponse;
import com.group2.rms.requisition.dto.JobPostingCreateRequest;
import com.group2.rms.requisition.service.JobPostingService;
import com.group2.rms.requisition.service.RequisitionAccess;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InternalJobPostingController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class InternalJobPostingControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JobPostingService jobPostingService;

    @MockitoBean
    private RequisitionAccess requisitionAccess;

    @MockitoBean
    private DepartmentRepository departmentRepository;

    @MockitoBean
    private UserRepository userRepository;

    private User hrUser;

    @BeforeEach
    void setUp() {
        Role hrRole = Role.builder().roleId(2).roleName("HR").build();
        hrUser = User.builder()
                .userId(5)
                .username("hr_officer")
                .fullName("Nguyễn Thị HR")
                .accountStatus("Active")
                .role(hrRole)
                .build();

        when(userRepository.findByUsernameIgnoreCase("hr_officer")).thenReturn(Optional.of(hrUser));
        when(userRepository.findByUsernameIgnoreCase("candidate_user")).thenReturn(Optional.of(User.builder()
                .userId(99).username("candidate_user").accountStatus("Active")
                .role(Role.builder().roleName("Candidate").build()).build()));
        when(requisitionAccess.actor()).thenReturn(hrUser);
        when(requisitionAccess.role(hrUser)).thenReturn("HR");
        when(departmentRepository.findAll()).thenReturn(Collections.emptyList());
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = {"HR"})
    @DisplayName("GET /internal/job-postings: HR truy cập danh sách tin tuyển dụng thành công")
    void listJobPostings_asHr_returnsListView() throws Exception {
        InternalJobPostingResponse item = InternalJobPostingResponse.builder()
                .jobPostingId(1)
                .postingTitle("Java Developer")
                .requisitionCode("REQ-2026-001")
                .departmentName("Phòng Công Nghệ")
                .recruitmentRound(1)
                .postingStatus("Published")
                .createdAt(LocalDateTime.now())
                .build();

        when(jobPostingService.searchInternalJobPostings(anyInt(), anyInt(), anyString(), any(), anyString(), anyString(), any(User.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        mvc.perform(get("/internal/job-postings"))
                .andExpect(status().isOk())
                .andExpect(view().name("job-postings/list"))
                .andExpect(model().attributeExists("postings"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"));
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = {"HR"})
    @DisplayName("GET /internal/job-postings/create: Mở form tạo tin với requisitionId thành công")
    void showCreateForm_asHr_returnsCreateView() throws Exception {
        JobPostingCreateRequest dto = JobPostingCreateRequest.builder()
                .requisitionId(101)
                .postingTitle("Java Developer")
                .build();

        when(jobPostingService.prepareCreateForm(eq(101), any(User.class))).thenReturn(dto);

        mvc.perform(get("/internal/job-postings/create").param("requisitionId", "101"))
                .andExpect(status().isOk())
                .andExpect(view().name("job-postings/create"))
                .andExpect(model().attributeExists("postingDto"));
    }

    @Test
    @DisplayName("GET /internal/job-postings: Chưa đăng nhập bị redirect về /login")
    void listJobPostings_unauthenticated_redirectsToLogin() throws Exception {
        mvc.perform(get("/internal/job-postings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "candidate_user", authorities = {"ROLE_CANDIDATE"})
    @DisplayName("GET /internal/job-postings: Role Candidate không có quyền truy cập (403)")
    void listJobPostings_asCandidate_forbidden() throws Exception {
        mvc.perform(get("/internal/job-postings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = {"HR"})
    @DisplayName("GET /internal/job-postings/{id}/edit: Mở form chỉnh sửa tin thành công")
    void showEditForm_asHr_returnsCreateView() throws Exception {
        JobPostingCreateRequest dto = JobPostingCreateRequest.builder()
                .jobPostingId(99)
                .requisitionId(101)
                .postingTitle("Java Developer")
                .build();

        when(jobPostingService.prepareEditForm(eq(99), any(User.class))).thenReturn(dto);

        mvc.perform(get("/internal/job-postings/99/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("job-postings/create"))
                .andExpect(model().attributeExists("postingDto"));
    }

    @Test
    @WithMockUser(username = "hr_officer", roles = {"HR"})
    @DisplayName("POST /internal/job-postings/delete/{id}: Xóa tin tuyển dụng thành công và redirect")
    void deleteJobPosting_asHr_redirectsWithFlashMessage() throws Exception {
        org.mockito.Mockito.doNothing().when(jobPostingService).deleteJobPosting(eq(99), any(User.class));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/internal/job-postings/delete/99")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/internal/job-postings"))
                .andExpect(flash().attributeExists("message"));
    }
}
