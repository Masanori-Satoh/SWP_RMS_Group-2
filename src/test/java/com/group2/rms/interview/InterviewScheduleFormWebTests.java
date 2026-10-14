package com.group2.rms.interview;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.interview.controller.InterviewSchedulingController;
import com.group2.rms.interview.service.InterviewSchedulingService;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Form "Lên lịch phỏng vấn mới" mở từ trang hồ sơ (/applications/{id}) với ?applicationId=.
 */
@WebMvcTest(InterviewSchedulingController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class InterviewScheduleFormWebTests {

    @Autowired MockMvc mvc;
    @MockitoBean InterviewSchedulingService schedulingService;
    @MockitoBean com.group2.rms.candidate.service.RecruitmentEmailService recruitmentEmail;
    @MockitoBean ApplicationRepository applications;
    @MockitoBean UserRepository users;

    @BeforeEach
    void setUp() {
        User hr = User.builder().userId(1).username("hr_lan").fullName("Lan HR").accountStatus("Active")
                .role(Role.builder().roleName("HR").build()).build();
        when(users.findByUsernameIgnoreCase("hr_lan")).thenReturn(Optional.of(hr));
        when(users.findAllWithRoleAndDepartment()).thenReturn(List.of());
        when(applications.findEligibleForInterview()).thenReturn(List.of(application(120), application(298)));
    }

    private static Application application(int id) {
        User account = User.builder().userId(1000 + id).fullName("Ung vien " + id).email(id + "@example.com").build();
        return Application.builder()
                .applicationId(id)
                .candidate(Candidate.builder().candidateId(id).account(account).build())
                .jobPosting(JobPosting.builder().jobPostingId(5).postingTitle("Java Developer").build())
                .build();
    }

    @Test
    void migratedPagesUseInternalLayoutWithoutLegacyFrame() throws Exception {
        when(schedulingService.getAllForHR()).thenReturn(List.of());

        mvc.perform(get("/interviews").with(user("hr_lan").authorities(() -> "ROLE_HR")))
                .andExpect(status().isOk())
                .andExpect(view().name("interview/list"))
                .andExpect(model().attribute("activeMenu", "interviews"))
                .andExpect(content().string(containsString("/css/pages/interview-common.css")))
                .andExpect(content().string(containsString("/js/interview-list.js")))
                .andExpect(content().string(containsString("Chưa có lịch phỏng vấn nào")))
                .andExpect(content().string(not(containsString("interface.css"))))
                .andExpect(content().string(not(containsString("global.js"))))
                .andExpect(content().string(not(containsString("style=\""))));

        mvc.perform(get("/interviews/new").with(user("hr_lan").authorities(() -> "ROLE_HR")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeMenu", "interviews"))
                .andExpect(content().string(containsString("/js/interview-form.js")))
                .andExpect(content().string(containsString("class=\"form-select\"")))
                .andExpect(content().string(not(containsString("interface.css"))))
                .andExpect(content().string(not(containsString("<script>"))));
    }

    @Test
    void scheduleCardsRenderWithCatalogClassesAndCancelDialog() throws Exception {
        java.time.LocalDateTime start = java.time.LocalDateTime.now().plusDays(2).withNano(0);
        when(schedulingService.getAllForHR()).thenReturn(List.of(new com.group2.rms.interview.dto.InterviewScheduleResponse(
                7L, 120, "Ung vien 120", "120@example.com", null, "Senior Java Developer",
                com.group2.rms.interview.entity.InterviewFormat.Online_GoogleMeet, "Online Google Meet",
                com.group2.rms.interview.entity.InterviewStatus.Scheduled, "Đã lên lịch",
                start, start.plusHours(1), "https://meet.google.com/abc", 1, "Lan HR", start.minusDays(1),
                List.of(new com.group2.rms.interview.dto.PanelMemberResponse(2, "hr_quang", "Quang HR", null, null,
                        com.group2.rms.interview.entity.RoleInPanel.HR, "HR Interviewer")))));

        mvc.perform(get("/interviews").with(user("hr_lan").authorities(() -> "ROLE_HR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"card il-card\"")))
                .andExpect(content().string(containsString("badge badge--warning")))
                .andExpect(content().string(containsString("data-member-name=\"Quang HR\"")))
                .andExpect(content().string(containsString("href=\"/interviews/7/result\"")))
                .andExpect(content().string(containsString("href=\"/interviews/7/edit\"")))
                .andExpect(content().string(containsString("data-cancel-url=\"/interviews/7/cancel\"")))
                .andExpect(content().string(containsString("id=\"il-cancel-dialog\"")))
                .andExpect(content().string(matchesPattern("(?s).*<form id=\"il-cancel-form\".*name=\"_csrf\".*")))
                // onclick còn trong topbar-user (khung chung) nên chỉ kiểm tra không còn confirm() nội tuyến của trang cũ
                .andExpect(content().string(not(containsString("return confirm("))));
    }

    @Test
    void applicationIdFromApplicationPageIsPreselected() throws Exception {
        mvc.perform(get("/interviews/new").param("applicationId", "298").with(user("hr_lan").authorities(() -> "ROLE_HR")))
                .andExpect(status().isOk())
                .andExpect(view().name("interview/form"))
                .andExpect(model().attribute("scheduleRequest",
                        org.hamcrest.Matchers.hasProperty("applicationId", org.hamcrest.Matchers.is(298))))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"298\"[^>]*selected=\"selected\".*")))
                .andExpect(content().string(not(matchesPattern("(?s).*<option value=\"120\"[^>]*selected=\"selected\".*"))));
    }

    @Test
    void hrSeesOtherHrButNotSelfInPanelList() throws Exception {
        User lan = User.builder().userId(1).username("hr_lan").fullName("Lan HR").accountStatus("Active")
                .role(Role.builder().roleName("HR").build()).build();
        User quang = User.builder().userId(2).username("hr_quang").fullName("Quang HR").accountStatus("Active")
                .role(Role.builder().roleName("HR").build()).build();
        when(users.findAllWithRoleAndDepartment()).thenReturn(List.of(lan, quang));

        mvc.perform(get("/interviews/new").with(user("hr_lan").authorities(() -> "ROLE_HR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Quang HR")))
                .andExpect(content().string(not(containsString("Lan HR ("))))
                .andExpect(content().string(not(containsString("<option value=\"1\""))));
    }

    @Test
    void formWithoutApplicationIdHasNothingPreselected() throws Exception {
        mvc.perform(get("/interviews/new").with(user("hr_lan").authorities(() -> "ROLE_HR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("-- Chọn hồ sơ ứng viên cần phỏng vấn --")))
                .andExpect(content().string(not(matchesPattern("(?s).*<option value=\"\\d+\"[^>]*selected=\"selected\".*"))));
    }
}
