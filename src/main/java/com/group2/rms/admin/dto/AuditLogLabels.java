package com.group2.rms.admin.dto;

import java.util.List;
import java.util.Map;

/** Presentation only: keep stored audit values intact, and preserve unfamiliar/free-form text. */
public final class AuditLogLabels {
    private AuditLogLabels() {}

    private static final Map<String, String> ENTITIES = Map.ofEntries(
            Map.entry("User", "Tài khoản"), Map.entry("Department", "Phòng ban"),
            Map.entry("Candidate", "Ứng viên"), Map.entry("Application", "Hồ sơ ứng tuyển"),
            Map.entry("TalentPoolNote", "Ghi chú ứng viên"), Map.entry("JobPosting", "Tin tuyển dụng"),
            Map.entry("JobRequisition", "Yêu cầu tuyển dụng"), Map.entry("InterviewSchedule", "Lịch phỏng vấn"),
            Map.entry("SystemConfig", "Cấu hình hệ thống"), Map.entry("System", "Hệ thống"));
    private static final Map<String, String> APPLICATION_VALUES = Map.ofEntries(
            Map.entry("TalentPoolHrRecommended", "Trạng thái: HR đã gửi gợi ý"),
            Map.entry("TalentPoolSuggested", "Trạng thái: HM đã đề xuất"),
            Map.entry("TalentPoolHrWithdrawn", "Trạng thái: HR đã rút lại gợi ý"),
            Map.entry("TalentPoolCancelled", "Trạng thái: Đề xuất đã bị hủy"),
            Map.entry("TalentPoolActivated", "Trạng thái: Đã xác nhận dùng CV cũ"),
            Map.entry("TalentPoolCandidateSubmitted", "Trạng thái: Ứng viên đã nộp CV mới"),
            Map.entry("HM_Suggested", "Trạng thái: Chờ xử lý đề xuất"),
            Map.entry("Applied", "Trạng thái: Đã ứng tuyển"),
            Map.entry("AI_Screened", "Trạng thái: Đã sàng lọc bằng AI"),
            Map.entry("HR_Passed", "Trạng thái: Đã qua vòng nhân sự"),
            Map.entry("HM_Passed", "Trạng thái: Đã qua vòng chuyên môn"),
            Map.entry("Interviewing", "Trạng thái: Đang phỏng vấn"),
            Map.entry("Offered", "Trạng thái: Đã gửi đề nghị tuyển dụng"),
            Map.entry("Hired", "Trạng thái: Đã tuyển dụng"),
            Map.entry("Rejected", "Trạng thái: Đã bị loại"),
            Map.entry("Withdrawn", "Trạng thái: Ứng viên đã rút hồ sơ"),
            Map.entry("PostingClosed", "Trạng thái: Dừng hồ sơ do đóng đợt tuyển"),
            Map.entry("InterviewCancelledOnClose", "Đã hủy lịch phỏng vấn do đóng đợt tuyển"));
    private static final Map<String, String> MAIL_VALUES = Map.of(
            "MailSent:PostingClosed", "Đã gửi thông báo đóng đợt tuyển",
            "MailFailed:PostingClosed", "Gửi thông báo đóng đợt tuyển thất bại",
            "MailSent:InterviewInvitation", "Đã gửi thư mời phỏng vấn",
            "MailFailed:InterviewInvitation", "Gửi thư mời phỏng vấn thất bại");
    private static final Map<String, String> ROLES = Map.of("System Admin", "Quản trị hệ thống", "HR", "Nhân sự",
            "Hiring Manager", "Trưởng phòng tuyển dụng", "Director", "Giám đốc", "Interviewer", "Người phỏng vấn", "Candidate", "Ứng viên");

    public static String entity(String value) { return ENTITIES.getOrDefault(value, value); }
    public static String action(String value) {
        return switch (value) {
            case "CREATE" -> "Tạo mới"; case "UPDATE" -> "Cập nhật";
            case "DELETE" -> "Xóa"; case "LOGIN" -> "Đăng nhập"; default -> value;
        };
    }
    public static List<Option> actions() {
        return List.of("CREATE", "UPDATE", "DELETE", "LOGIN").stream().map(value -> new Option(value, action(value))).toList();
    }
    public record Option(String value, String label) {}

    public static String content(String entity, String action, String value, boolean before, String newValue) {
        if (value == null || value.isBlank()) return null;
        String key = value.strip();
        if ("Application".equals(entity)) {
            if (before && "CREATE".equals(action) && key.matches("[0-9]+")
                    && ("TalentPoolHrRecommended".equals(newValue) || "TalentPoolSuggested".equals(newValue)))
                return "Hồ sơ nguồn: #" + key;
            if (APPLICATION_VALUES.containsKey(key)) return APPLICATION_VALUES.get(key);
        }
        if ("Candidate".equals(entity)) {
            if ("TalentPoolMarked".equals(key)) return "Tiềm năng: Đã đánh dấu";
            if ("TalentPoolUnmarked".equals(key)) return "Tiềm năng: Đã bỏ đánh dấu";
            if (before && ("true".equals(key) || "false".equals(key))
                    && ("TalentPoolMarked".equals(newValue) || "TalentPoolUnmarked".equals(newValue)))
                return "Tiềm năng: " + ("true".equals(key) ? "Đã đánh dấu" : "Chưa đánh dấu");
        }
        if (("Application".equals(entity) || "InterviewSchedule".equals(entity)) && MAIL_VALUES.containsKey(key))
            return MAIL_VALUES.get(key);
        if ("JobPosting".equals(entity) && "Closed".equals(key)) return "Trạng thái: Đã đóng tin tuyển dụng";
        if ("User".equals(entity)) {
            if ("LOGIN".equals(action) && ("User logged in successfully".equals(key) || "Director initial login".equals(key)))
                return "Đăng nhập vào hệ thống";
            return value.lines().map(line -> {
                if (line.startsWith("Vai trò: ")) {
                    String role = line.substring("Vai trò: ".length());
                    return "Vai trò: " + ROLES.getOrDefault(role, role);
                }
                if (line.equals("Trạng thái: Active")) return "Trạng thái: Hoạt động";
                if (line.equals("Trạng thái: Inactive")) return "Trạng thái: Vô hiệu hóa";
                return line;
            }).collect(java.util.stream.Collectors.joining("\n"));
        }
        return value;
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] words = name.strip().split("\\s+");
        String result = words.length > 1 ? words[words.length - 2].substring(0, 1) : "";
        return (result + words[words.length - 1].substring(0, 1)).toUpperCase(java.util.Locale.ROOT);
    }

    public static String entityIcon(String entity) {
        return switch (entity) {
            case "Department" -> "M4 21V3h16v18M9 3v18M13 7h3M13 11h3M13 15h3M2 21h20";
            case "JobPosting", "JobRequisition" -> "M9 7V4h6v3M3 7h18v14H3zM3 12h18M10 12v3h4v-3";
            case "System", "SystemConfig" -> "M4 4h16v12H4zM8 21h8M12 16v5M7 8h10M7 12h6";
            case "TalentPoolNote" -> "M5 3h14v18H5zM8 7h8M8 11h8M8 15h5";
            case "InterviewSchedule" -> "M4 5h16v16H4zM4 10h16M8 3v4M16 3v4M8 14h3";
            default -> "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75";
        };
    }
}
