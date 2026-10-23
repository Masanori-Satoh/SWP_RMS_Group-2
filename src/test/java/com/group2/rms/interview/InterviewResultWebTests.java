package com.group2.rms.interview;

import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.interview.controller.CandidateInterviewController;
import com.group2.rms.interview.controller.InterviewEvaluationController;
import com.group2.rms.interview.controller.InterviewFinalResultController;
import com.group2.rms.interview.controller.InterviewResultController;
import com.group2.rms.interview.dto.InterviewEvaluationResponse;
import com.group2.rms.interview.dto.InterviewResultDetailResponse;
import com.group2.rms.interview.dto.InterviewResultDetailResponse.Averages;
import com.group2.rms.interview.dto.InterviewResultDetailResponse.FinalResult;
import com.group2.rms.interview.dto.InterviewResultDetailResponse.PanelMemberStatus;
import com.group2.rms.interview.dto.InterviewResultDetailResponse.RecommendationCount;
import com.group2.rms.interview.dto.InterviewScheduleResponse;
import com.group2.rms.interview.dto.SubmitFinalResultRequest;
import com.group2.rms.interview.entity.InterviewFormat;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.interview.dto.SubmitEvaluationRequest;
import com.group2.rms.interview.exception.InterviewEvaluationException;
import com.group2.rms.interview.exception.InterviewFinalResultException;
import com.group2.rms.interview.service.InterviewEvaluationService;
import com.group2.rms.interview.service.InterviewFinalResultService;
import com.group2.rms.interview.service.InterviewSchedulingService;
import com.group2.rms.interview.service.InterviewResultService;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Trang kết quả phỏng vấn + pop-up đánh giá qua Spring MVC + Spring Security thật; service là mock.
 */
@WebMvcTest({InterviewResultController.class, InterviewEvaluationController.class,
        InterviewFinalResultController.class, CandidateInterviewController.class})
@Import({SecurityConfig.class, DatabaseUserDetailsService.class, CurrentUserService.class})
class InterviewResultWebTests {

    private static final String VALID_BODY = """
            {"technicalScore":4,"softSkillScore":3,"culturalFitScore":5,"recommendation":"Hire",
             "strengths":"Nắm vững Spring","weaknesses":"","comments":""}
            """;

    @Autowired MockMvc mvc;
    @MockitoBean InterviewResultService resultService;
    @MockitoBean InterviewEvaluationService evaluationService;
    @MockitoBean InterviewFinalResultService finalResultService;
    @MockitoBean InterviewSchedulingService schedulingService;
    @MockitoBean UserRepository users;

    // ------------------------------------------------------------------ trang kết quả

    @Test
    void guestIsSentToLogin() throws Exception {
        mvc.perform(get("/interviews/7/result"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        verifyNoInteractions(resultService);
    }

    @Test
    void candidateIsForbidden() throws Exception {
        mvc.perform(get("/interviews/7/result").with(as(30, "phong.nguyen", "Candidate")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(resultService);
    }

    @Test
    void hrSeesAllEvaluationsWithoutEvaluateButton() throws Exception {
        when(resultService.getResult(eq(7L), argThat(u -> u.getUserId() == 1))).thenReturn(result(true, false, null, true, false));

        mvc.perform(get("/interviews/7/result").with(as(1, "hr_lan", "HR")))
                .andExpect(status().isOk())
                .andExpect(view().name("interview/result"))
                .andExpect(model().attribute("activeMenu", "interviews"))
                .andExpect(content().string(containsString("Chi Candidate")))
                .andExpect(content().string(containsString("Tổng hợp điểm")))
                .andExpect(content().string(containsString("Giỏi &lt;b&gt;Java&lt;/b&gt;<br/>Chủ động")))
                .andExpect(content().string(containsString("25.000.000 ₫")))
                .andExpect(content().string(containsString("Kết quả cuối")))
                .andExpect(content().string(not(containsString("id=\"iev-dialog\""))))
                .andExpect(content().string(not(containsString("interview-result.js"))));
    }

    @Test
    void panelMemberSeesEvaluateDialog() throws Exception {
        when(resultService.getResult(eq(7L), argThat(u -> u.getUserId() == 11))).thenReturn(result(false, true, null, false, false));

        mvc.perform(get("/interviews/7/result").with(as(11, "iv_minh", "Interviewer")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"iev-dialog\"")))
                .andExpect(content().string(containsString("data-action=\"/interviews/7/evaluations\"")))
                .andExpect(content().string(containsString("Bỏ qua")))
                .andExpect(content().string(containsString("data-require-any=\"technicalScore softSkillScore culturalFitScore\"")))
                .andExpect(content().string(containsString("/js/pages/interview-result.js")))
                .andExpect(content().string(containsString("Bạn chỉ xem được phiếu của mình")))
                .andExpect(content().string(not(containsString("Tổng hợp điểm"))));
    }

    @Test
    void blockedReasonIsShown() throws Exception {
        when(resultService.getResult(eq(7L), argThat(u -> u.getUserId() == 11)))
                .thenReturn(result(false, false, "Chưa đến giờ phỏng vấn.", false, false));

        mvc.perform(get("/interviews/7/result").with(as(11, "iv_minh", "Interviewer")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chưa đến giờ phỏng vấn.")))
                .andExpect(content().string(not(containsString("id=\"iev-dialog\""))));
    }

    @Test
    void interviewerOutsidePanelGets403() throws Exception {
        when(resultService.getResult(anyLong(), any()))
                .thenThrow(new AccessDeniedException("Bạn không được phân công vào buổi phỏng vấn này."));

        mvc.perform(get("/interviews/7/result").with(as(12, "iv_khac", "Interviewer")))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ nộp phiếu (AJAX)

    @Test
    void submitWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/interviews/7/evaluations").with(as(11, "iv_minh", "Interviewer"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(evaluationService);
    }

    @Test
    void candidateCannotSubmit() throws Exception {
        mvc.perform(post("/interviews/7/evaluations").with(as(30, "phong.nguyen", "Candidate")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(evaluationService);
    }

    @Test
    void panelMemberSubmitsEvaluation() throws Exception {
        mvc.perform(post("/interviews/7/evaluations").with(as(11, "iv_minh", "Interviewer")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã gửi phiếu đánh giá."));

        verify(evaluationService).submit(eq(7L),
                eq(new SubmitEvaluationRequest(4, 3, 5, "Hire", "Nắm vững Spring", "", "")), eq(11));
    }

    @Test
    void hrHiringManagerAndDirectorMaySubmit() throws Exception {
        // HR được HR khác xếp vào hội đồng; quyền thành viên thật do Service kiểm tra
        mvc.perform(post("/interviews/7/evaluations").with(as(2, "hr_quang", "HR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
        mvc.perform(post("/interviews/7/evaluations").with(as(12, "hm_khanh", "Hiring Manager")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
        mvc.perform(post("/interviews/7/evaluations").with(as(13, "director", "Director")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void invalidScoreReturnsFieldErrorsAsJson() throws Exception {
        String body = """
                {"technicalScore":6,"softSkillScore":3,"culturalFitScore":5,"recommendation":"Hire"}
                """;

        mvc.perform(post("/interviews/7/evaluations").with(as(11, "iv_minh", "Interviewer")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.technicalScore").value("Điểm kỹ thuật phải từ 1 đến 5."));
        verifyNoInteractions(evaluationService);
    }

    @Test
    void partialScoresAreAccepted() throws Exception {
        // HR trong hội đồng chỉ chấm kỹ năng mềm + văn hóa, bỏ qua kỹ thuật
        String body = """
                {"technicalScore":null,"softSkillScore":4,"culturalFitScore":5,"recommendation":"Consider"}
                """;

        mvc.perform(post("/interviews/7/evaluations").with(as(2, "hr_quang", "HR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(evaluationService).submit(eq(7L),
                eq(new SubmitEvaluationRequest(null, 4, 5, "Consider", null, null, null)), eq(2));
    }

    @Test
    void evaluationWithoutAnyScoreIsRejected() throws Exception {
        String body = """
                {"technicalScore":null,"softSkillScore":null,"culturalFitScore":null,"recommendation":"Hire"}
                """;

        mvc.perform(post("/interviews/7/evaluations").with(as(11, "iv_minh", "Interviewer")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.anyScoreGiven").value("Vui lòng chấm ít nhất một mục điểm."));
        verifyNoInteractions(evaluationService);
    }

    @Test
    void businessErrorReturnsMessageAsJson() throws Exception {
        doThrow(new InterviewEvaluationException("Bạn đã nộp phiếu đánh giá cho buổi phỏng vấn này."))
                .when(evaluationService).submit(eq(7L), any(), eq(11));

        mvc.perform(post("/interviews/7/evaluations").with(as(11, "iv_minh", "Interviewer")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn đã nộp phiếu đánh giá cho buổi phỏng vấn này."));
    }

    // ------------------------------------------------------------------ chốt kết quả (Hiring Manager)

    @Test
    void decisionMakerSeesFinalizeDialog() throws Exception {
        when(resultService.getResult(eq(7L), argThat(u -> u.getUserId() == 40)))
                .thenReturn(result(true, false, null, false, true));

        mvc.perform(get("/interviews/7/result").with(as(40, "hm_khanh", "Hiring Manager")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-dialog-open=\"ifr-dialog\"")))
                .andExpect(content().string(containsString("id=\"ifr-dialog\"")))
                .andExpect(content().string(containsString("data-action=\"/interviews/7/final-result\"")))
                .andExpect(content().string(containsString("Hội đồng mới nộp 1/2 phiếu")))
                .andExpect(content().string(containsString("/js/pages/interview-result.js")))
                .andExpect(content().string(not(containsString("id=\"iev-dialog\""))));
    }

    @Test
    void hiringManagerFinalizesResult() throws Exception {
        mvc.perform(post("/interviews/7/final-result").with(as(40, "hm_khanh", "Hiring Manager")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"finalDecision\":\"Passed\",\"recommendedSalary\":25000000,\"finalSummaryComments\":\"Tốt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(finalResultService).decide(eq(7L),
                eq(new SubmitFinalResultRequest("Passed", new BigDecimal("25000000"), "Tốt")),
                argThat(u -> u.getUserId() == 40));
    }

    @Test
    void onlyHiringManagerMayFinalize() throws Exception {
        String body = "{\"finalDecision\":\"Passed\"}";
        mvc.perform(post("/interviews/7/final-result").with(as(11, "iv_minh", "Interviewer")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/interviews/7/final-result").with(as(1, "hr_lan", "HR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/interviews/7/final-result").with(as(13, "director", "Director")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        verifyNoInteractions(finalResultService);
    }

    @Test
    void invalidDecisionReturnsFieldErrors() throws Exception {
        mvc.perform(post("/interviews/7/final-result").with(as(40, "hm_khanh", "Hiring Manager")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"finalDecision\":\"Maybe\",\"recommendedSalary\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.finalDecision").value("Kết quả không hợp lệ."))
                .andExpect(jsonPath("$.data.recommendedSalary").value("Mức lương đề xuất không được âm."));
        verifyNoInteractions(finalResultService);
    }

    @Test
    void alreadyFinalizedReturnsMessage() throws Exception {
        doThrow(new InterviewFinalResultException("Buổi phỏng vấn này đã được chốt kết quả, không thể chốt lại."))
                .when(finalResultService).decide(eq(7L), any(), any());

        mvc.perform(post("/interviews/7/final-result").with(as(40, "hm_khanh", "Hiring Manager")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"finalDecision\":\"Failed\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Buổi phỏng vấn này đã được chốt kết quả, không thể chốt lại."));
    }

    // ------------------------------------------------------------------ lịch phỏng vấn của ứng viên

    @Test
    void candidateSeesOwnInterviews() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 10, 20, 9, 0);
        when(schedulingService.getSchedulesForCandidate(30)).thenReturn(List.of(new InterviewScheduleResponse(
                5L, 100, "Chi Candidate", "chi@example.com", null, "Java Developer",
                InterviewFormat.Online_GoogleMeet, "Online Google Meet", InterviewStatus.Scheduled, "Đã lên lịch",
                start, start.plusHours(1), "https://meet.google.com/abc", 1, "HR Lan", start.minusDays(3),
                List.of())));

        mvc.perform(get("/portal/interviews").with(as(30, "phong.nguyen", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(view().name("interview/candidate-list"))
                .andExpect(model().attribute("activeMenu", "interviews"))
                .andExpect(content().string(containsString("Lịch phỏng vấn của tôi")))
                .andExpect(content().string(containsString("Java Developer")))
                .andExpect(content().string(containsString("href=\"https://meet.google.com/abc\"")))
                .andExpect(content().string(not(containsString("HR Lan"))));
    }

    @Test
    void candidateWithoutInterviewsSeesEmptyState() throws Exception {
        when(schedulingService.getSchedulesForCandidate(30)).thenReturn(List.of());

        mvc.perform(get("/portal/interviews").with(as(30, "phong.nguyen", "Candidate")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bạn chưa có lịch phỏng vấn nào")))
                .andExpect(content().string(not(containsString("<table"))));
    }

    @Test
    void staffCannotOpenCandidatePortal() throws Exception {
        mvc.perform(get("/portal/interviews").with(as(1, "hr_lan", "HR"))).andExpect(status().isForbidden());
        verifyNoInteractions(schedulingService);
    }

    // ------------------------------------------------------------------ dữ liệu mẫu

    private RequestPostProcessor as(int userId, String username, String roleName) {
        User account = User.builder().userId(userId).username(username).fullName(username).accountStatus("Active")
                .role(Role.builder().roleName(roleName).build()).build();
        when(users.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(account));
        String authority = switch (roleName) {
            case "System Admin" -> "ROLE_SYSTEM_ADMIN";
            case "Hiring Manager" -> "ROLE_HIRING_MANAGER";
            default -> "ROLE_" + roleName.toUpperCase();
        };
        return user(username).authorities(() -> authority);
    }

    private static InterviewResultDetailResponse result(boolean canSeeAll, boolean canEvaluate,
                                                        String blockedReason, boolean withFinalResult,
                                                        boolean canFinalize) {
        LocalDateTime start = LocalDateTime.of(2026, 10, 9, 9, 0);
        InterviewEvaluationResponse evaluation = new InterviewEvaluationResponse(
                1, 21, "An Interviewer", "Interviewer", 4, 4, 5, 4.3, "Hire", "Nên tuyển", "badge--success",
                "Giỏi &lt;b&gt;Java&lt;/b&gt;<br/>Chủ động", null, null, start.plusHours(2), false, false);
        return new InterviewResultDetailResponse(
                7L, 100, "Chi Candidate", "chi@example.com", "Java Developer",
                start, start.plusHours(1), "Trực tiếp tại văn phòng", "Phòng họp 3",
                "Đã hoàn thành", "badge--success",
                List.of(new PanelMemberStatus(21, "An Interviewer", "Interviewer", true, false, "Đã nộp", "badge--success"),
                        new PanelMemberStatus(11, "iv_minh", "Interviewer", false, true, "Chờ đánh giá", "badge--warning")),
                1,
                canSeeAll ? List.of(evaluation) : List.of(),
                canSeeAll ? new Averages(4.0, 4.0, 5.0, 4.3) : null,
                canSeeAll ? List.of(new RecommendationCount("Hire", "Nên tuyển", "badge--success", 1)) : List.of(),
                withFinalResult ? new FinalResult("Passed", "Đạt", "badge--success", "Hoa HM",
                        new BigDecimal("25000000"), null, start.plusDays(1), false, null, null, false, null) : null,
                canSeeAll, canEvaluate, blockedReason, canFinalize, null,
                start.plusHours(25), start.plusHours(73), false);
    }
}
