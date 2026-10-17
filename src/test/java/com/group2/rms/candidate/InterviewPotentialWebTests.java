package com.group2.rms.candidate;

import com.group2.rms.candidate.controller.TalentPoolController;
import com.group2.rms.candidate.dto.*;
import com.group2.rms.candidate.exception.TalentPoolException;
import com.group2.rms.candidate.service.*;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.*;
import com.group2.rms.interview.controller.InterviewResultController;
import com.group2.rms.interview.dto.InterviewResultDetailResponse;
import com.group2.rms.interview.service.InterviewResultService;
import com.group2.rms.user.entity.*;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.time.LocalDateTime;
import java.util.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Integration adapter tests belong to Talent Pool; no changes to the interview controller/DTO. */
@WebMvcTest({InterviewResultController.class,TalentPoolController.class})
@Import({SecurityConfig.class,DatabaseUserDetailsService.class,CurrentUserService.class})
class InterviewPotentialWebTests {
    @Autowired MockMvc mvc;
    @MockitoBean InterviewResultService resultService;
    @MockitoBean TalentPoolService pool;
    @MockitoBean RecruitmentEmailService email;
    @MockitoBean UserRepository users;
    private RequestPostProcessor as(String role) {
        User account=User.builder().userId(5).username("viewer").fullName("Người xem").accountStatus("Active")
                .role(Role.builder().roleName(role).build()).build();
        when(users.findByUsernameIgnoreCase("viewer")).thenReturn(Optional.of(account));
        return user("viewer").authorities(()->RoleAuthorities.fromRoleName(role));
    }
    private InterviewResultDetailResponse result(String decision,boolean canFinalize) {
        var time=LocalDateTime.of(2026,10,16,10,0);
        var finalResult=decision==null?null:new InterviewResultDetailResponse.FinalResult(decision,
                "Failed".equals(decision)?"Không đạt":"Đạt","badge--neutral","HM Tuấn",null,"Nhận xét chuyên môn",time);
        return new InterviewResultDetailResponse(7L,218,"Ứng viên An","an@example.com","Backend",time,time.plusHours(1),
                "Trực tuyến","Phòng 1","Đã hoàn thành","badge--success",List.of(),0,List.of(),null,List.of(),finalResult,
                true,false,null,canFinalize,null);
    }
    @ParameterizedTest @ValueSource(strings={"Passed","Failed"})
    void hmFinalPassOrFailHasIndependentPotentialActionAndUnchangedFinalResult(String decision) throws Exception {
        when(resultService.getResult(eq(7L),any())).thenReturn(result(decision,false));
        mvc.perform(get("/interviews/7/result").with(as("Hiring Manager")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("data-tp-potential")))
                .andExpect(content().string(containsString("/talent-pool/applications/218/bookmark")))
                .andExpect(content().string(containsString("Kết quả cuối")))
                .andExpect(content().string(containsString("Nhận xét chuyên môn")))
                .andExpect(content().string(containsString("Đang tải trạng thái…")))
                .andExpect(content().string(containsString("aria-controls=\"app-bookmark\"")))
                .andExpect(content().string(containsString("id=\"app-bookmark\"")))
                .andExpect(content().string(containsString("Lịch sử ghi chú")))
                .andExpect(content().string(not(containsString("data-tp-notes"))))
                .andExpect(content().string(not(containsString("id=\"app-preview\""))));
        verifyNoInteractions(pool,email);
    }
    @Test void hrHasSameActionBeforeFinalDecisionAndHmFinalizeDialogIsPreserved() throws Exception {
        when(resultService.getResult(eq(7L),any())).thenReturn(result(null,false));
        mvc.perform(get("/interviews/7/result").with(as("HR")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("data-tp-potential")))
                .andExpect(content().string(containsString("Thông tin buổi phỏng vấn")));
        when(resultService.getResult(eq(7L),any())).thenReturn(result(null,true));
        mvc.perform(get("/interviews/7/result").with(as("Hiring Manager")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("data-action=\"/interviews/7/final-result\"")))
                .andExpect(content().string(containsString("Chốt kết quả")))
                .andExpect(content().string(containsString("/js/pages/interview-result.js")))
                .andExpect(content().string(containsString("/js/pages/applications.js")));
    }
    @ParameterizedTest @ValueSource(strings={"Interviewer","Director","System Admin"})
    void otherResultViewersDoNotReceivePotentialUiOrItsScript(String role) throws Exception {
        when(resultService.getResult(eq(7L),any())).thenReturn(result("Passed",false));
        mvc.perform(get("/interviews/7/result").with(as(role))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("data-tp-potential"))))
                .andExpect(content().string(not(containsString("/js/pages/applications.js"))))
                .andExpect(content().string(not(containsString("id=\"app-bookmark\""))));
        verifyNoInteractions(pool);
    }
    @Test void existingReadApiReturnsSavedStateAndSharedNotes() throws Exception {
        when(pool.bookmark(218)).thenReturn(new TalentPoolBookmarkResponse(8,true,
                List.of(new TalentPoolNoteResponse(1,"HR Lan","Ghi chú chung",LocalDateTime.now()))));
        mvc.perform(get("/talent-pool/applications/218/bookmark").with(as("Hiring Manager")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.candidateId").value(8))
                .andExpect(jsonPath("$.saved").value(true)).andExpect(jsonPath("$.notes[0].author").value("HR Lan"));
        verifyNoInteractions(resultService,email);
    }
    @ParameterizedTest @ValueSource(strings={"HR","Hiring Manager"})
    void existingWriteApiOnlyReceivesBookmarkNoteAndDoesNotCallInterviewLogic(String role) throws Exception {
        when(pool.saveBookmark(eq(218),any())).thenReturn(new TalentPoolBookmarkResponse(8,true,List.of()));
        mvc.perform(post("/talent-pool/applications/218/bookmark").with(as(role)).with(csrf()).param("content","Ghi chú chung")
                        .param("decision","Fail").param("applicationStatus","Rejected").param("isPotential","false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true));
        verify(pool).saveBookmark(218,new TalentPoolBookmarkRequest("Ghi chú chung"));
        verifyNoInteractions(resultService,email);
    }
    @Test void validationAndFailureCannotBeReportedAsSuccessfulSave() throws Exception {
        mvc.perform(post("/talent-pool/applications/218/bookmark").with(as("HR")).with(csrf()).param("content","x".repeat(1001)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(pool);
        when(pool.saveBookmark(eq(218),any())).thenThrow(new TalentPoolException("Không thể lưu ghi chú."));
        mvc.perform(post("/talent-pool/applications/218/bookmark").with(as("HR")).with(csrf()).param("content","Ghi chú"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Không thể lưu ghi chú."));
    }
    @ParameterizedTest @ValueSource(strings={"HR","Hiring Manager"})
    void toggleCanUseExistingRemoveApiWithoutInterviewMutation(String role) throws Exception {
        when(pool.removeBookmark(218)).thenReturn(new TalentPoolBookmarkResponse(8,false,List.of(
                new TalentPoolNoteResponse(1,"HR Lan","Giữ ghi chú",LocalDateTime.now()))));
        mvc.perform(post("/talent-pool/applications/218/bookmark/remove").with(as(role)).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(false))
                .andExpect(jsonPath("$.notes[0].content").value("Giữ ghi chú"));
        verify(pool).removeBookmark(218);
        verifyNoInteractions(resultService,email);
    }
    @Test void csrfRolesAndApplicationScopeStayProtected() throws Exception {
        mvc.perform(post("/talent-pool/applications/218/bookmark").with(as("HR"))).andExpect(status().isForbidden());
        mvc.perform(post("/talent-pool/applications/218/bookmark/remove").with(as("HR"))).andExpect(status().isForbidden());
        for(String role:List.of("Candidate","Interviewer")) {
            mvc.perform(get("/talent-pool/applications/218/bookmark").with(as(role))).andExpect(status().isForbidden());
            mvc.perform(post("/talent-pool/applications/218/bookmark").with(as(role)).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(post("/talent-pool/applications/218/bookmark/remove").with(as(role)).with(csrf())).andExpect(status().isForbidden());
        }
        verifyNoInteractions(pool);
        when(pool.bookmark(218)).thenThrow(new AccessDeniedException("Wrong application scope"));
        mvc.perform(get("/talent-pool/applications/218/bookmark").with(as("Hiring Manager"))).andExpect(status().isForbidden());
        when(pool.removeBookmark(218)).thenThrow(new AccessDeniedException("Wrong application scope"));
        mvc.perform(post("/talent-pool/applications/218/bookmark/remove").with(as("Hiring Manager")).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
