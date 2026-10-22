package com.group2.rms.chatbot.dto;

import lombok.Builder;

import java.util.List;

/**
 * Một câu trả lời của chatbot. Giao diện vẽ theo thứ tự: reply → thẻ tin / chi tiết tin / thẻ hồ sơ / liên hệ HR
 * → các link → các nút gợi ý. Danh sách không có dữ liệu luôn là list rỗng (không null).
 *
 * @param intent       ý định chatbot đã nhận ra (tên {@code ChatbotIntent}), giao diện chỉ dùng để gắn class
 * @param reply        câu trả lời chính
 * @param jobs         thẻ tin tuyển dụng (tìm việc, gợi ý việc)
 * @param jobDetail    chi tiết một tin; {@code null} nếu không hỏi chi tiết
 * @param applications thẻ hồ sơ của chính ứng viên đang đăng nhập
 * @param contact      thông tin liên hệ HR; {@code null} nếu không cần hiện
 * @param links        link sang trang có sẵn (đăng nhập, ứng tuyển, xem tất cả)
 * @param quickReplies nút gợi ý câu hỏi tiếp theo
 */
@Builder
public record ChatResponse(
        String intent,
        String reply,
        List<JobCard> jobs,
        JobDetail jobDetail,
        List<ApplicationCard> applications,
        Contact contact,
        List<Link> links,
        List<QuickReply> quickReplies) {

    /** Chuẩn hóa các danh sách để frontend luôn nhận mảng rỗng thay vì null. */
    public ChatResponse {
        jobs = jobs == null ? List.of() : jobs;
        applications = applications == null ? List.of() : applications;
        links = links == null ? List.of() : links;
        quickReplies = quickReplies == null ? List.of() : quickReplies;
    }

    /**
     * Thẻ một tin tuyển dụng.
     *
     * @param deadline hạn nộp đã định dạng dd/MM/yyyy
     * @param reason   lý do gợi ý (chỉ có ở gợi ý việc làm); {@code null} khi tìm việc
     */
    public record JobCard(Integer id, String title, String departmentName, String workLocation, String salaryDisplay,
            String deadline, String detailHref, String reason) {
    }

    /**
     * Chi tiết một tin, rút gọn cho khung chat (tối đa vài dòng yêu cầu / quyền lợi).
     *
     * @param salaryDisplay {@code null} nếu tin không công bố lương
     */
    public record JobDetail(Integer id, String title, String departmentName, String workLocation,
            String salaryDisplay, String deadline, List<String> requirements, List<String> benefits) {
    }

    /**
     * Thẻ một hồ sơ ứng tuyển.
     *
     * @param statusTone {@code neutral}, {@code success}, {@code warning} hoặc {@code danger} (ghép class badge--...)
     */
    public record ApplicationCard(String postingTitle, String statusLabel, String statusNote, String statusTone) {
    }

    /** Thông tin liên hệ HR đọc từ SystemConfig; trường nào chưa cấu hình là {@code null}. */
    public record Contact(String email, String phone, String workingHours) {
    }

    /** @param primary nút chính (tô màu), còn lại là nút phụ */
    public record Link(String label, String href, boolean primary) {
    }

    /**
     * Nút gợi ý: bấm vào thì gửi {@code message} (hoặc {@code jobPostingId}) như một câu hỏi mới.
     */
    public record QuickReply(String label, String message, Integer jobPostingId) {
    }
}
