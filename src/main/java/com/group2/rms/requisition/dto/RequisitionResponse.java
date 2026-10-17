package com.group2.rms.requisition.dto;

import com.group2.rms.admin.dto.ActivityLogResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequisitionResponse {
    private Integer requisitionId;
    private String title;
    private String gender;
    private String workLocation;
    private String workModel;
    private Integer probationDays;
    private LocalDate expectedStartDate;
    private boolean editable;
    private boolean deletable;
    private boolean decidable;
    private boolean withdrawable;
    private boolean incomplete;
    private String rejectionReason;
    private String departmentName;
    private String hiringType;
    private String hiringManagerName;
    private Integer numberOfPositions;
    private String employmentType;
    private String approvalStatus;
    private LocalDateTime createdAt;
    private Integer recruitmentRound;
    private String requisitionCode;
    private String postingStatus;
    private Integer jobPostingId;

    // Nhãn tiếng Việt và tông màu badge, service tính sẵn từ RequisitionLabels (template không tự dịch)
    private String statusLabel;
    private String statusTone;
    private String postingLabel;
    private String postingTone;
    private String employmentTypeLabel;

    // Cờ quyền cho menu ⋯ của từng dòng, service tính từ RequisitionAccess (template không tự so sánh role)
    private boolean copyable;
    private boolean postable;
    private boolean postingViewable;

    // ----- Chỉ có ở màn Chi tiết (service tính sẵn, template chỉ hiển thị) -----
    private LocalDateTime submittedAt;          // JobRequisition.SubmittedAt: lúc gửi duyệt gần nhất
    private Long waitingDays;                   // số ngày đã chờ Giám đốc duyệt (chỉ khi Pending_Director)
    private Long daysUntilStart;                // còn bao nhiêu ngày tới ExpectedStartDate (âm = đã qua)
    private String salaryLabel;                 // "20 – 30 triệu VND/tháng" hoặc "Thỏa thuận"
    private String probationLabel;              // "60 ngày" hoặc "Không thử việc"
    private String genderLabel;
    private String workModelLabel;
    private String hiringTypeLabel;
    private String reasonForHiringHtml;         // nội dung nhiều dòng đã escape + <br/> (hiển thị bằng th:utext)
    private String jobDescriptionHtml;
    private String requirementDetailsHtml;
    private BigDecimal criteriaTotalWeight;     // tổng trọng số tiêu chí
    private Integer postingNo;                  // JobPosting.PostingNo của tin mới nhất (lần đăng thứ mấy)
    private LocalDateTime postingDate;          // JobPosting.PostingDate
    private LocalDateTime postingDeadline;      // JobPosting.ApplicationDeadline

    // Các trường phục vụ màn Chi tiết
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String reasonForHiring;
    private String jobDescription;
    private String requirementDetails;

    private java.util.List<ScreeningCriteriaResponse> screeningCriteria;
    private java.util.List<ApprovalResponse> approvals;
    private java.util.List<ActivityLogResponse> activityLog;
}
