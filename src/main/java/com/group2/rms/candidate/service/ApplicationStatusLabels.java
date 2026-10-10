package com.group2.rms.candidate.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nhãn tiếng Việt và class badge cho trạng thái đơn và quyết định duyệt.
 * Nhãn trạng thái giữ giống {@code DashboardService.applicationLabel} để hai nơi hiển thị cùng một chữ.
 */
public final class ApplicationStatusLabels {

    /** Thứ tự theo luồng tuyển dụng, dùng cho ô lọc. */
    public static final List<String> STATUSES = List.of(
            "Applied", "AI_Screened", "HR_Passed", "HM_Passed", "Interviewing", "Offered", "Hired", "Rejected");

    private ApplicationStatusLabels() {
    }

    /** Trạng thái → nhãn, giữ thứ tự {@link #STATUSES}, cho ô lọc trạng thái. */
    public static Map<String, String> options() {
        Map<String, String> options = new LinkedHashMap<>();
        STATUSES.forEach(status -> options.put(status, label(status)));
        return options;
    }

    public static String label(String status) {
        return switch (status) {
            case "Applied" -> "Đã nộp hồ sơ";
            case "AI_Screened" -> "Đã sàng lọc";
            case "HR_Passed" -> "Qua vòng nhân sự";
            case "HM_Passed" -> "Qua vòng chuyên môn";
            case "Interviewing" -> "Đang phỏng vấn";
            case "Offered" -> "Đã có thư mời";
            case "Hired" -> "Đã tuyển dụng";
            case "Rejected" -> "Không tiếp tục";
            default -> status;
        };
    }

    /**
     * Ai đang phải làm gì với hồ sơ ở trạng thái này: cho ô "Bước tiếp theo" trên trang chi tiết,
     * để người xem biết hồ sơ đang đi về đâu dù chính họ không có thao tác nào.
     */
    public static String currentStep(String status, String departmentName) {
        return switch (status) {
            case "Applied", "AI_Screened" -> "HR sàng lọc hồ sơ và chuyển cho trưởng bộ phận.";
            case "HR_Passed" -> "Trưởng bộ phận" + (departmentName == null ? "" : " " + departmentName) + " duyệt hồ sơ.";
            case "HM_Passed" -> "HR lên lịch phỏng vấn.";
            case "Interviewing" -> "Phỏng vấn và tổng hợp kết quả; đạt thì tạo offer.";
            case "Offered" -> "Chờ ứng viên phản hồi thư mời.";
            case "Hired" -> "Đã tuyển dụng. Quy trình kết thúc.";
            case "Rejected" -> "Hồ sơ đã dừng. Quy trình kết thúc.";
            default -> "";
        };
    }

    public static String badge(String status) {
        return switch (status) {
            case "HR_Passed", "HM_Passed", "Interviewing" -> "badge--warning";
            case "Offered", "Hired" -> "badge--success";
            case "Rejected" -> "badge--danger";
            default -> "badge--neutral";
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

    public static String reviewerRoleLabel(String reviewerRole) {
        return switch (reviewerRole) {
            case "HR" -> "HR";
            case "HiringManager" -> "Trưởng bộ phận";
            case "Director" -> "Giám đốc";
            default -> reviewerRole;
        };
    }
}
