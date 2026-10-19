package com.group2.rms.requisition.dto;

import com.group2.rms.admin.dto.ActivityLogResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;

/**
 * Một yêu cầu tuyển dụng đã dựng sẵn cho màn danh sách (một dòng) và màn chi tiết.
 */
@Builder(toBuilder = true)
public record RequisitionResponse(
        Integer requisitionId,
        String title,
        String workLocation,
        LocalDate expectedStartDate,
        boolean editable,
        boolean deletable,
        boolean decidable,
        boolean withdrawable,
        boolean incomplete,
        String rejectionReason,
        String departmentName,
        String hiringManagerName,
        Integer numberOfPositions,
        String approvalStatus,
        LocalDateTime createdAt,
        Integer recruitmentRound,
        String requisitionCode,
        String postingStatus,
        Integer jobPostingId,

        // Nhãn tiếng Việt và tông màu badge, tính sẵn từ RequisitionLabels (template không tự dịch)
        String statusLabel,
        String statusTone,
        String postingLabel,
        String postingTone,
        String employmentTypeLabel,

        // Cờ quyền cho menu ⋯ của từng dòng, tính từ RequisitionAccess (template không tự so sánh role)
        boolean copyable,
        boolean postable,
        boolean postingViewable,

        // ----- Chỉ có ở màn Chi tiết -----
        LocalDateTime submittedAt,          // JobRequisition.SubmittedAt: lúc gửi duyệt gần nhất
        Long waitingDays,                   // số ngày đã chờ Giám đốc duyệt (chỉ khi Pending_Director)
        Long daysUntilStart,                // còn bao nhiêu ngày tới ExpectedStartDate (âm = đã qua)
        String salaryLabel,                 // "20 – 30 triệu VND/tháng" hoặc "Thỏa thuận"
        String probationLabel,              // "60 ngày" hoặc "Không thử việc"
        String genderLabel,
        String workModelLabel,
        String hiringTypeLabel,
        String reasonForHiringHtml,         // nội dung nhiều dòng đã escape + <br/> (hiển thị bằng th:utext)
        String jobDescriptionHtml,
        String requirementDetailsHtml,
        BigDecimal criteriaTotalWeight,     // tổng trọng số tiêu chí
        Integer postingNo,                  // JobPosting.PostingNo của tin mới nhất (lần đăng thứ mấy)
        LocalDateTime postingDate,          // JobPosting.PostingDate
        LocalDateTime postingDeadline,      // JobPosting.ApplicationDeadline

        List<ScreeningCriteriaResponse> screeningCriteria,
        List<ApprovalResponse> approvals,
        List<ActivityLogResponse> activityLog) {
}
