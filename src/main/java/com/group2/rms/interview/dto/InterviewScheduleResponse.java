package com.group2.rms.interview.dto;

import com.group2.rms.candidate.Application;
import com.group2.rms.interview.entity.InterviewFormat;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Output DTO đại diện cho kết quả chi tiết lịch phỏng vấn (sử dụng Java 21 Record).
 * Bao gồm đầy đủ thông tin: Lịch phỏng vấn, Hồ sơ ứng tuyển, Ứng viên, Vị trí tuyển dụng và Hội đồng.
 * Hoàn toàn KHÔNG chứa trường interviewRound theo đúng yêu cầu refactor.
 */
public record InterviewScheduleResponse(
        Long interviewId,
        Integer applicationId,
        String candidateFullName,
        String candidateEmail,
        String candidatePhoneNumber,
        String jobPostingTitle,
        InterviewFormat interviewFormat,
        String interviewFormatDisplay,
        InterviewStatus interviewStatus,
        String interviewStatusDisplay,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String locationOrLink,
        Integer createdById,
        String createdByName,
        LocalDateTime createdAt,
        List<PanelMemberResponse> interviewers
) {

    public static InterviewScheduleResponse fromEntity(InterviewSchedule schedule) {
        if (schedule == null) {
            return null;
        }

        Application app = schedule.getApplication();
        String candidateName = null;
        String candidateEmail = null;
        String candidatePhone = null;
        String jobTitle = null;

        if (app != null) {
            if (app.getCandidate() != null && app.getCandidate().getAccount() != null) {
                User candidateUser = app.getCandidate().getAccount();
                candidateName = candidateUser.getFullName();
                candidateEmail = candidateUser.getEmail();
                candidatePhone = candidateUser.getPhoneNumber();
            }
            if (app.getJobPosting() != null) {
                jobTitle = app.getJobPosting().getPostingTitle();
            }
        }

        User createdBy = schedule.getCreatedBy();
        Integer createdById = createdBy != null ? createdBy.getUserId() : null;
        String createdByName = createdBy != null ? createdBy.getFullName() : null;

        List<PanelMemberResponse> panelResponses = schedule.getInterviewPanels() != null
                ? schedule.getInterviewPanels().stream()
                    .map(PanelMemberResponse::fromEntity)
                    .toList()
                : List.of();

        return new InterviewScheduleResponse(
                schedule.getInterviewId(),
                app != null ? app.getApplicationId() : null,
                candidateName,
                candidateEmail,
                candidatePhone,
                jobTitle,
                schedule.getInterviewFormat(),
                schedule.getInterviewFormat() != null ? schedule.getInterviewFormat().getDisplayName() : null,
                schedule.getInterviewStatus(),
                schedule.getInterviewStatus() != null ? schedule.getInterviewStatus().getDisplayName() : null,
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getLocationOrLink(),
                createdById,
                createdByName,
                schedule.getCreatedAt(),
                panelResponses
        );
    }
}
