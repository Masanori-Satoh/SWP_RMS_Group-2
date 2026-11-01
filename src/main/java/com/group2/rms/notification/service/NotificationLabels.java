package com.group2.rms.notification.service;

/*
 * LABELS: dịch EventType lưu DB sang nhãn tiếng Việt và tông màu badge cho trang /notifications.
 */
public final class NotificationLabels {

    private NotificationLabels() {
    }

    public static String eventLabel(String eventType) {
        if (eventType == null) {
            return null;
        }
        return switch (eventType) {
            case "REQUISITION_PENDING_DIRECTOR" -> "Chờ phê duyệt";
            case "REQUISITION_APPROVED" -> "Yêu cầu đã được duyệt";
            case "REQUISITION_REJECTED" -> "Yêu cầu bị từ chối";
            case "REQUISITION_WITHDRAWN" -> "Yêu cầu đã rút lại";
            case "JOB_POSTING_PUBLISHED" -> "Tin tuyển dụng đã phát hành";
            case "JOB_POSTING_REOPENED" -> "Tin tuyển dụng đã mở lại";
            case "JOB_POSTING_PAUSED" -> "Tin tạm dừng";
            case "JOB_POSTING_CLOSED" -> "Tin đã đóng";
            case "APPLICATION_HR_PASSED" -> "Hồ sơ chờ duyệt";
            case "APPLICATION_HM_PASSED" -> "Hồ sơ chờ xếp lịch";
            case "INTERVIEW_SCHEDULED" -> "Lịch phỏng vấn mới";
            case "INTERVIEW_RESCHEDULED" -> "Đổi lịch phỏng vấn";
            case "INTERVIEW_CANCELLED" -> "Hủy lịch phỏng vấn";
            case "INTERVIEW_RESULT_DECIDED" -> "Kết quả phỏng vấn";
            case "OFFER_PENDING_DIRECTOR" -> "Offer chờ phê duyệt";
            case "OFFER_DIRECTOR_APPROVED" -> "Offer đã được duyệt";
            case "OFFER_DIRECTOR_REJECTED" -> "Offer bị từ chối";
            case "OFFER_LETTER_SENT" -> "Thư mời nhận việc";
            case "OFFER_ACCEPTED" -> "Ứng viên nhận việc";
            case "OFFER_DECLINED" -> "Ứng viên từ chối offer";
            default -> null;
        };
    }

    public static String eventTone(String eventType) {
        if (eventType == null) {
            return "neutral";
        }
        return switch (eventType) {
            case "REQUISITION_APPROVED", "JOB_POSTING_PUBLISHED", "JOB_POSTING_REOPENED", "APPLICATION_HM_PASSED",
                 "INTERVIEW_SCHEDULED", "OFFER_DIRECTOR_APPROVED", "OFFER_LETTER_SENT", "OFFER_ACCEPTED" -> "success";
            case "REQUISITION_PENDING_DIRECTOR", "APPLICATION_HR_PASSED", "INTERVIEW_RESCHEDULED",
                 "OFFER_PENDING_DIRECTOR", "JOB_POSTING_PAUSED" -> "warning";
            case "REQUISITION_REJECTED", "INTERVIEW_CANCELLED", "OFFER_DIRECTOR_REJECTED", "OFFER_DECLINED" -> "danger";
            default -> "neutral";
        };
    }
}
