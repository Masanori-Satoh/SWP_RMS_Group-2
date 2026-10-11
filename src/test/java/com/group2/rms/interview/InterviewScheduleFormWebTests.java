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
    @MockitoBean ApplicationRepository applications;
    @MockitoBean UserRepository users;

    @BeforeEach
    void setUp() {
        User hr = User.builder().userId(1).username("hr_lan").fullName("Lan HR").accountStatus("Active")
                .role(Role.builder().roleName("HR").build()).build();
        when(users.findByUsernameIgnoreCase("hr_lan")).thenReturn(Optional.of(hr));
        when(users.findAllWithRoleAndDepartment()).thenReturn(List.of());
        when(applications.findAllWithCandidateAndJobPosting()).thenReturn(List.of(application(120), application(298)));
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
