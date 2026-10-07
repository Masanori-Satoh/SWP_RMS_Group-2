package com.group2.rms.requisition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPostingCreateRequest {

    @NotNull(message = "Yêu cầu tuyển dụng nguồn không được để trống.")
    private Integer requisitionId;

    // Các thông tin tóm tắt từ Requisition nguồn (chỉ hiển thị)
    private String requisitionCode;
    private String departmentName;
    private Integer recruitmentRound;
    private String employmentType;
    private Integer numberOfPositions;
    private String hiringManagerName;

    @NotBlank(message = "Tiêu đề tin tuyển dụng không được để trống.")
    @Size(max = 200, message = "Tiêu đề tin tuyển dụng không vượt quá 200 ký tự.")
    private String postingTitle;

    @NotBlank(message = "Mô tả công việc không được để trống.")
    private String jobDescription;

    @NotBlank(message = "Yêu cầu ứng viên không được để trống.")
    private String jobRequirements;

    private String benefits;

    @Size(max = 100, message = "Hiển thị mức lương không vượt quá 100 ký tự.")
    private String salaryDisplay;

    @Size(max = 255, message = "Địa điểm làm việc không vượt quá 255 ký tự.")
    private String workLocation;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate applicationDeadline;

    /**
     * "draft" hoặc "publish"
     */
    private String action;
}
