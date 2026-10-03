package com.group2.rms.interview.dto;

import com.group2.rms.interview.entity.InterviewFormat;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.interview.entity.RoleInPanel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO nhận dữ liệu yêu cầu tạo mới hoặc cập nhật lịch phỏng vấn (sử dụng Java Record).
 * Tuân thủ đầy đủ quy chuẩn DTO bất biến, validation và tương thích ngược với Spring MVC Binding / Thymeleaf.
 */
@Builder
@ValidInterviewTime
public record InterviewScheduleRequest(
        @NotNull(message = "Application ID không được để trống.")
        Integer applicationId,

        @NotNull(message = "Hình thức phỏng vấn không được để trống (Online_GoogleMeet hoặc Offline_Office).")
        InterviewFormat interviewFormat,

        @NotNull(message = "Thời gian bắt đầu phỏng vấn không được để trống.")
        @Future(message = "Thời gian bắt đầu phỏng vấn phải ở trong tương lai.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startTime,

        @NotNull(message = "Thời gian kết thúc phỏng vấn không được để trống.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endTime,

        @Size(max = 500, message = "Địa điểm hoặc liên kết phòng họp tối đa 500 ký tự.")
        String locationOrLink,

        InterviewStatus interviewStatus,

        Set<Integer> interviewerIds,

        @NotEmpty(message = "Hội đồng phỏng vấn phải có ít nhất 1 người phỏng vấn.")
        @Valid
        List<PanelMemberRequest> panelMembers
) {

    /**
     * Compact constructor: Đảm bảo tính toán đồng bộ giữa interviewerIds và panelMembers,
     * đồng thời gán giá trị mặc định cho interviewStatus.
     */
    public InterviewScheduleRequest {
        if (panelMembers == null || panelMembers.isEmpty()) {
            if (interviewerIds != null && !interviewerIds.isEmpty()) {
                panelMembers = interviewerIds.stream()
                        .filter(Objects::nonNull)
                        .map(id -> new PanelMemberRequest(id, RoleInPanel.HM))
                        .toList();
            } else {
                panelMembers = Collections.emptyList();
            }
        } else {
            if (interviewerIds == null || interviewerIds.isEmpty()) {
                interviewerIds = panelMembers.stream()
                        .map(PanelMemberRequest::interviewerId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }
        }
        if (interviewStatus == null) {
            interviewStatus = InterviewStatus.Scheduled;
        }
    }

    /**
     * No-args constructor hỗ trợ form binding khởi tạo ban đầu.
     */
    public InterviewScheduleRequest() {
        this(null, null, null, null, null, InterviewStatus.Scheduled, Collections.emptySet(), Collections.emptyList());
    }

    /**
     * Class-level validation helper (Rule MSG26: endTime > startTime).
     *
     * NULL-SAFETY GUARANTEE:
     * Nếu startTime hoặc endTime bị null, method này lập tức trả về true để nhường quyền
     * báo lỗi cho các annotation @NotNull tương ứng, tuyệt đối KHÔNG văng NullPointerException.
     */
    @AssertTrue(message = "Thời gian kết thúc (endTime) phải lớn hơn thời gian bắt đầu (startTime).")
    public boolean isEndTimeAfterStartTime() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return endTime.isAfter(startTime);
    }

    // ==========================================
    // BACKWARD-COMPATIBLE GETTERS (Thymeleaf / Service)
    // ==========================================
    public Integer getApplicationId() {
        return applicationId;
    }

    public InterviewFormat getInterviewFormat() {
        return interviewFormat;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getLocationOrLink() {
        return locationOrLink;
    }

    public InterviewStatus getInterviewStatus() {
        return interviewStatus;
    }

    public Set<Integer> getInterviewerIds() {
        return interviewerIds;
    }

    public List<PanelMemberRequest> getPanelMembers() {
        return panelMembers;
    }
}
