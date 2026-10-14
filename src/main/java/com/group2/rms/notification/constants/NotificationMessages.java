package com.group2.rms.notification.constants;

import java.util.Map;

/** Tiêu đề và nội dung tiếng Việt của toàn bộ sự kiện, dùng chung ở mọi luồng. */
public final class NotificationMessages {
    private NotificationMessages() {}

    public record Message(String title, String content) {}

    private static final Message HR_REQUISITION_APPROVED = new Message(
            "Có yêu cầu tuyển dụng mới được phê duyệt",
            "Một yêu cầu tuyển dụng đã được phê duyệt và sẵn sàng để tạo tin tuyển dụng.");

    private static final Map<NotificationEventType, Message> MESSAGES = Map.ofEntries(
            Map.entry(NotificationEventType.REQUISITION_SUBMITTED, new Message("Yêu cầu tuyển dụng chờ phê duyệt", "Có một yêu cầu tuyển dụng mới đang chờ bạn xem xét và phê duyệt.")),
            Map.entry(NotificationEventType.REQUISITION_APPROVED, new Message("Yêu cầu tuyển dụng đã được phê duyệt", "Yêu cầu tuyển dụng của bạn đã được phê duyệt. Bạn có thể theo dõi các bước tuyển dụng tiếp theo.")),
            Map.entry(NotificationEventType.REQUISITION_REJECTED, new Message("Yêu cầu tuyển dụng bị từ chối", "Yêu cầu tuyển dụng của bạn đã bị từ chối. Vui lòng xem lý do từ chối và điều chỉnh nếu cần.")),
            Map.entry(NotificationEventType.REQUISITION_CANCELLED, new Message("Yêu cầu tuyển dụng đã bị hủy", "Yêu cầu tuyển dụng đã bị hủy. Vui lòng kiểm tra thông tin chi tiết.")),
            Map.entry(NotificationEventType.REQUISITION_CLOSED, new Message("Yêu cầu tuyển dụng đã đóng", "Yêu cầu tuyển dụng đã được đóng và không còn tiếp tục xử lý.")),
            Map.entry(NotificationEventType.JOB_POSTING_PUBLISHED, new Message("Tin tuyển dụng đã được đăng", "Tin tuyển dụng thuộc yêu cầu của bạn đã được đăng thành công.")),
            Map.entry(NotificationEventType.JOB_POSTING_PAUSED, new Message("Tin tuyển dụng đã tạm dừng", "Tin tuyển dụng đã được tạm dừng. Vui lòng kiểm tra trạng thái và thông tin liên quan.")),
            Map.entry(NotificationEventType.JOB_POSTING_CLOSED, new Message("Tin tuyển dụng đã đóng", "Tin tuyển dụng đã được đóng và không tiếp tục nhận hồ sơ.")),
            Map.entry(NotificationEventType.APPLICATION_RECEIVED, new Message("Có hồ sơ ứng tuyển mới", "Một ứng viên vừa nộp hồ sơ cho vị trí tuyển dụng. Vui lòng kiểm tra và xem xét hồ sơ.")),
            Map.entry(NotificationEventType.APPLICATION_SUBMITTED, new Message("Nộp hồ sơ thành công", "Hồ sơ ứng tuyển của bạn đã được tiếp nhận thành công.")),
            Map.entry(NotificationEventType.APPLICATION_STATUS_UPDATED, new Message("Trạng thái hồ sơ đã cập nhật", "Trạng thái hồ sơ ứng tuyển của bạn đã thay đổi. Vui lòng xem chi tiết để biết thêm thông tin.")),
            Map.entry(NotificationEventType.APPLICATION_WITHDRAWN, new Message("Ứng viên đã rút hồ sơ", "Một ứng viên đã rút hồ sơ ứng tuyển khỏi vị trí tuyển dụng.")),
            Map.entry(NotificationEventType.INTERVIEW_SCHEDULED, new Message("Lịch phỏng vấn mới", "Lịch phỏng vấn đã được tạo. Vui lòng kiểm tra thời gian và thông tin chi tiết.")),
            Map.entry(NotificationEventType.INTERVIEW_RESCHEDULED, new Message("Lịch phỏng vấn đã thay đổi", "Lịch phỏng vấn đã được thay đổi. Vui lòng kiểm tra thời gian mới.")),
            Map.entry(NotificationEventType.INTERVIEW_CANCELLED, new Message("Lịch phỏng vấn đã bị hủy", "Lịch phỏng vấn đã bị hủy. Vui lòng xem thông tin chi tiết.")),
            Map.entry(NotificationEventType.INTERVIEW_FEEDBACK_SUBMITTED, new Message("Đã có đánh giá phỏng vấn", "Người phỏng vấn đã gửi đánh giá. Vui lòng xem kết quả để tiếp tục xử lý hồ sơ.")),
            Map.entry(NotificationEventType.INTERVIEW_RESULT_UPDATED, new Message("Kết quả phỏng vấn đã cập nhật", "Kết quả phỏng vấn đã được cập nhật. Vui lòng xem thông tin chi tiết.")),
            Map.entry(NotificationEventType.OFFER_SENT, new Message("Bạn đã nhận được thư mời làm việc", "Công ty đã gửi thư mời làm việc cho bạn. Vui lòng xem chi tiết và phản hồi theo hướng dẫn.")),
            Map.entry(NotificationEventType.OFFER_ACCEPTED, new Message("Ứng viên đã chấp nhận thư mời", "Ứng viên đã chấp nhận thư mời làm việc. Vui lòng cập nhật các bước tuyển dụng tiếp theo.")),
            Map.entry(NotificationEventType.OFFER_DECLINED, new Message("Ứng viên đã từ chối thư mời", "Ứng viên đã từ chối thư mời làm việc. Vui lòng xem xét phương án tuyển dụng tiếp theo.")),
            Map.entry(NotificationEventType.OFFER_WITHDRAWN, new Message("Thư mời làm việc đã bị thu hồi", "Thư mời làm việc đã bị thu hồi. Vui lòng xem thông tin chi tiết."))
    );

    public static Message forEvent(NotificationEventType event, boolean hrRecipient) {
        return event == NotificationEventType.REQUISITION_APPROVED && hrRecipient
                ? HR_REQUISITION_APPROVED : MESSAGES.get(event);
    }
}
