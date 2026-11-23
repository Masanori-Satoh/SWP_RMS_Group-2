package com.group2.rms.career.controller;

import com.group2.rms.candidate.service.ApplicationSubmissionService;
import com.group2.rms.career.service.CareerPortalService;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * CONTROLLER TEST (@WebMvcTest): CareerPortalController, chỉ kiểm tra tin không còn công khai -> về /jobs kèm thông báo.
 */
@WebMvcTest(controllers = CareerPortalController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class CareerPortalControllerTests {

    private static final String UNAVAILABLE = "Tin tuyển dụng này không còn khả dụng.";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CareerPortalService careerPortalService;
    @MockitoBean
    private ApplicationSubmissionService applicationSubmissionService;
    // Bộ lọc phiên và các advice dùng chung cần các repository này
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private DepartmentRepository departmentRepository;
    @MockitoBean
    private JobPostingRepository jobPostingRepository;
    @MockitoBean
    private JobRequisitionRepository jobRequisitionRepository;

    @Test
    @DisplayName("Khách mở /jobs/{id} của tin đã tạm dừng / đóng -> Về /jobs kèm thông báo, không ra trang 404")
    void viewJobDetail_shouldRedirectToJobBoardWithMessage_whenJobIsNotPublic() throws Exception {
        // Arrange
        when(careerPortalService.getPublishedJobDetail(eq(7), isNull()))
                .thenThrow(new ResourceNotFoundException(UNAVAILABLE));

        // Act + Assert
        mvc.perform(get("/jobs/7"))
                .andExpect(redirectedUrl("/jobs"))
                .andExpect(flash().attribute("errorMessage", UNAVAILABLE));
    }

    @Test
    @DisplayName("Truy cập /jobs kèm đủ 4 nhóm filter -> Binds model attributes và gọi Service với đúng các tham số")
    void viewPublicJobList_shouldBindAllFilterParamsAndReturnJobBoardView() throws Exception {
        when(careerPortalService.getPublishedJobs(eq("Java"), eq(2), eq("Full-time"), eq("Remote"), eq("25-40"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(careerPortalService.getSmartSortedDepartments()).thenReturn(List.of());

        mvc.perform(get("/jobs")
                        .param("keyword", "  Java  ")
                        .param("departmentId", "2")
                        .param("employmentType", "Full-time")
                        .param("workModel", "Remote")
                        .param("salaryRange", "25-40"))
                .andExpect(status().isOk())
                .andExpect(view().name("candidate/job-board"))
                .andExpect(model().attribute("selectedKeyword", "Java"))
                .andExpect(model().attribute("selectedDept", 2))
                .andExpect(model().attribute("selectedType", "Full-time"))
                .andExpect(model().attribute("selectedWorkModel", "Remote"))
                .andExpect(model().attribute("selectedSalary", "25-40"))
                .andExpect(model().attributeExists("jobsPage"))
                .andExpect(model().attributeExists("departments"));

        verify(careerPortalService).getPublishedJobs(eq("Java"), eq(2), eq("Full-time"), eq("Remote"), eq("25-40"), any(Pageable.class));
    }
}
