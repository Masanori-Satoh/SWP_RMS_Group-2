package com.group2.rms.candidate.domain.rule;

import java.util.List;

/**
 * Nhãn tiếng Việt và class badge cho trạng thái đơn và quyết định duyệt.
 * Nhãn trạng thái giữ giống {@code DashboardService.applicationLabel} để hai nơi hiển thị cùng một chữ.
 */
public final class ApplicationStatusLabels {

    /** Các trạng thái hợp lệ, theo thứ tự luồng tuyển dụng; tham số lọc {@code status} ngoài danh sách này bị bỏ qua. */
    public static final List<String> STATUSES = List.of(
            "Applied", "HR_Passed", "HM_Suggested", "HM_Passed", "Interviewing", "Offered", "Hired", "Rejected",
            "Withdrawn", "Cancelled");

    private ApplicationStatusLabels() {
    }

    public static String label(String status) {
        return switch (status) {
            case "Applied" -> "Chờ sàng lọc";
            case "HR_Passed" -> "Qua vòng nhân sự";
            case "HM_Passed" -> "Qua vòng chuyên môn";
            case "HM_Suggested" -> "HM đề xuất — chờ HR liên hệ";
            case "Interviewing" -> "Đang phỏng vấn";
            case "Offered" -> "Đã có thư mời";
            case "Hired" -> "Đã nhận việc";
            case "Rejected" -> "Không tiếp tục";
            case "Withdrawn" -> "Đã rút đơn";
            case "Cancelled" -> "Dừng do đóng tin";
            default -> status;
        };
    }

    public static String badge(String status) {
        return switch (status) {
            case "HR_Passed", "HM_Passed", "HM_Suggested", "Interviewing" -> "badge--warning";
            case "Offered", "Hired" -> "badge--success";
            case "Rejected" -> "badge--danger";
            default -> "badge--neutral";
        };
    }

    /**
     * Nhãn trạng thái thư mời nhận việc ({@code OfferProposal.OfferStatus}) cho phía nhân viên; trạng thái lạ giữ nguyên.
     */
    // dunglt bổ sung: dòng hành trình "Thư mời: ..." trước đây hiện thẳng mã trạng thái (vd "Offer: Sent_Candidate").
    public static String offerLabel(String offerStatus) {
        return switch (offerStatus) {
            case "Draft" -> "Đang soạn";
            case "Pending_Director" -> "Chờ giám đốc duyệt";
            case "Director_Approved" -> "Giám đốc đã duyệt";
            case "Director_Rejected" -> "Giám đốc trả lại";
            case "Sent_Candidate" -> "Đã gửi, chờ ứng viên trả lời";
            case "Accepted" -> "Ứng viên đã nhận lời";
            case "Declined" -> "Ứng viên đã từ chối";
            case "Expired" -> "Đã quá hạn";
            default -> offerStatus;
        };
    }

    public static String decisionLabel(String decision) {
        return switch (decision) {
            case "Pass" -> "Đạt";
            case "Hold" -> "Tạm giữ";
            case "Fail" -> "Không đạt";
            default -> decision;
        };
    }

    /** Màu cho timeline (cùng bộ tên với hậu tố badge). */
    public static String decisionTone(String decision) {
        return switch (decision) {
            case "Pass" -> "success";
            case "Hold" -> "warning";
            case "Fail" -> "danger";
            default -> "neutral";
        };
    }

    /** Kết quả chốt phỏng vấn ({@code InterviewFinalResult.FinalDecision}). */
    public static String interviewResultLabel(String finalDecision) {
        return switch (finalDecision) {
            case "Passed" -> "Đạt";
            case "Failed" -> "Không đạt";
            case "Considering" -> "Đang cân nhắc";
            default -> finalDecision;
        };
    }

    public static String interviewResultTone(String finalDecision) {
        return switch (finalDecision) {
            case "Passed" -> "success";
            case "Failed" -> "danger";
            case "Considering" -> "warning";
            default -> "neutral";
        };
    }

    public static String reviewerRoleLabel(String reviewerRole) {
        return switch (reviewerRole) {
            case "HR" -> "HR";
            case "HiringManager" -> "Trưởng bộ phận";
            case "Director" -> "Giám đốc";
            default -> reviewerRole;
        };
    }
}
