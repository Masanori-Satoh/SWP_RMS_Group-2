package com.group2.rms.interview.dto;

import com.group2.rms.interview.InterviewFormat;
import com.group2.rms.interview.InterviewStatus;
import com.group2.rms.interview.RoleInPanel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * DTO nhận dữ liệu yêu cầu tạo mới hoặc cập nhật lịch phỏng vấn.
 * Áp dụng đầy đủ validation chuẩn: @NotNull, @Future, @Size, @NotEmpty và class-level @ValidInterviewTime.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidInterviewTime
public class InterviewScheduleRequest {

    @NotNull(message = "Application ID không được để trống.")
    private Integer applicationId;

    @NotNull(message = "Hình thức phỏng vấn không được để trống (Online_GoogleMeet hoặc Offline_Office).")
    private InterviewFormat interviewFormat;

    @NotNull(message = "Thời gian bắt đầu phỏng vấn không được để trống.")
    @Future(message = "Thời gian bắt đầu phỏng vấn phải ở trong tương lai.")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc phỏng vấn không được để trống.")
    private LocalDateTime endTime;

    @Size(max = 500, message = "Địa điểm hoặc liên kết phòng họp tối đa 500 ký tự.")
    private String locationOrLink;

    /**
     * Trạng thái phỏng vấn (tùy chọn khi tạo mới - mặc định Scheduled; dùng khi cập nhật trạng thái).
     */
    private InterviewStatus interviewStatus;

    /**
     * Danh sách thành viên Hội đồng phỏng vấn (Interviewer).
     * Bắt buộc có ít nhất 1 thành viên.
     */
    @NotEmpty(message = "Hội đồng phỏng vấn phải có ít nhất 1 người phỏng vấn.")
    @Valid
    @Builder.Default
    private List<PanelMemberRequest> panelMembers = new ArrayList<>();

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

    /**
     * Tiện ích hỗ trợ client nếu chỉ truyền danh sách ID người phỏng vấn.
     */
    public void setInterviewerIds(Set<Integer> interviewerIds) {
        if (interviewerIds != null) {
            this.panelMembers = interviewerIds.stream()
                    .map(id -> new PanelMemberRequest(id, RoleInPanel.HM))
                    .toList();
        }
    }

    public Set<Integer> getInterviewerIds() {
        if (panelMembers == null) {
            return java.util.Collections.emptySet();
        }
        return panelMembers.stream()
                .map(PanelMemberRequest::interviewerId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }
}
