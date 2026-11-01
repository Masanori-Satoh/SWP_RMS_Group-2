package com.group2.rms.notification.service;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionLabels;
import com.group2.rms.user.entity.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
 * MESSAGES: soạn tiêu đề, nội dung và khóa thời gian của thông báo; không lưu, không chọn người nhận.
 */
public final class NotificationMessages {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CLOCK_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter EVENT_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSSS");

    private NotificationMessages() {
    }

    // Khóa thời gian ghép vào EventId để mỗi lần xảy ra là một sự kiện riêng
    public static String eventTime(LocalDateTime time) {
        return time.format(EVENT_TIME);
    }

    public static String displayTime(LocalDateTime time) {
        return time.format(DISPLAY_TIME);
    }

    public static String nameOr(User user, String fallback) {
        if (user == null) {
            return fallback;
        }
        return user.getFullName();
    }

    public static String requisitionCode(JobRequisition requisition) {
        return RequisitionLabels.requisitionCode(requisition.getRequisitionId(), requisition.getRequisitionCode());
    }

    public static JobRequisition requisitionOf(Application application) {
        if (application == null || application.getJobPosting() == null) {
            return null;
        }
        return application.getJobPosting().getRequisition();
    }

    public static String candidateName(Application application) {
        if (application == null || application.getCandidate() == null || application.getCandidate().getAccount() == null) {
            return "Ứng viên";
        }
        return application.getCandidate().getAccount().getFullName();
    }

    public static String positionTitle(OfferProposal offer) {
        if (offer.getOfferedPositionTitle() == null || offer.getOfferedPositionTitle().isBlank()) {
            return "Vị trí tuyển dụng";
        }
        return offer.getOfferedPositionTitle();
    }

    public static String requisitionPendingContent(JobRequisition requisition, String hmName) {
        String deptName = "Chưa xác định";
        if (requisition.getDepartment() != null) {
            deptName = requisition.getDepartment().getDepartmentName();
        }
        return String.format(
                "Hiring Manager %s đã gửi yêu cầu tuyển dụng \"%s\" (Mã: %s, Phòng ban: %s) để chờ phê duyệt. Vui lòng xem xét và thực hiện phê duyệt.",
                hmName, requisition.getTitle(), requisitionCode(requisition), deptName);
    }

    public static String requisitionApprovedContent(JobRequisition requisition, String directorName,
                                                    LocalDateTime decidedAt, String comments) {
        String commentText = "";
        if (comments != null && !comments.isBlank()) {
            commentText = " Ý kiến: \"" + comments.trim() + "\".";
        }
        return String.format(
                "Yêu cầu tuyển dụng \"%s\" (Mã: %s) đã được Giám đốc %s phê duyệt vào lúc %s.%s Bộ phận Nhân sự (HR) có thể tiến hành tạo tin tuyển dụng.",
                requisition.getTitle(), requisitionCode(requisition), directorName, displayTime(decidedAt), commentText);
    }

    public static String requisitionRejectedContent(JobRequisition requisition, String directorName,
                                                    LocalDateTime decidedAt, String comments) {
        String reason = "Không có lý do cụ thể";
        if (comments != null && !comments.isBlank()) {
            reason = comments.trim();
        }
        return String.format(
                "Yêu cầu tuyển dụng \"%s\" (Mã: %s) đã bị Giám đốc %s từ chối vào lúc %s.\nLý do: %s.\nVui lòng chỉnh sửa lại thông tin và nộp duyệt lại.",
                requisition.getTitle(), requisitionCode(requisition), directorName, displayTime(decidedAt), reason);
    }

    public static String requisitionWithdrawnContent(JobRequisition requisition, String actorName, LocalDateTime now) {
        return String.format(
                "%s đã rút lại yêu cầu tuyển dụng \"%s\" (Mã: %s) vào lúc %s. Yêu cầu trở về bản nháp, không cần phê duyệt nữa.",
                actorName, requisition.getTitle(), requisitionCode(requisition), displayTime(now));
    }

    public static String jobPostingPublishedContent(JobPosting posting, String hrName, LocalDateTime postingDate) {
        String deadline = "Không giới hạn";
        LocalDateTime applicationDeadline = posting.getApplicationDeadline();
        if (applicationDeadline != null) {
            deadline = applicationDeadline.format(DISPLAY_DATE);
        }
        return String.format(
                "Tin tuyển dụng \"%s\" (cho %s) đã được %s phát hành vào lúc %s (Hạn nộp hồ sơ: %s). Ứng viên hiện đã có thể xem và nộp hồ sơ.",
                posting.getPostingTitle(), requisitionCode(posting.getRequisition()), hrName, displayTime(postingDate), deadline);
    }

    public static String jobPostingReopenedContent(JobPosting posting, String hrName, LocalDateTime postingDate) {
        String deadline = "Không giới hạn";
        LocalDateTime applicationDeadline = posting.getApplicationDeadline();
        if (applicationDeadline != null) {
            deadline = applicationDeadline.format(DISPLAY_DATE);
        }
        return String.format(
                "Tin tuyển dụng \"%s\" (cho %s) đã được %s mở lại vào lúc %s (Hạn nộp hồ sơ: %s). Ứng viên có thể xem và nộp hồ sơ trở lại.",
                posting.getPostingTitle(), requisitionCode(posting.getRequisition()), hrName, displayTime(postingDate), deadline);
    }

    public static String jobPostingPausedContent(JobPosting posting, String actorName, LocalDateTime now) {
        return String.format(
                "%s đã tạm dừng tin tuyển dụng \"%s\" vào lúc %s. Tin bị ẩn khỏi trang tuyển dụng và tạm ngừng nhận hồ sơ.",
                actorName, posting.getPostingTitle(), displayTime(now));
    }

    public static String jobPostingClosedContent(JobPosting posting, String actorName, LocalDateTime now) {
        return String.format(
                "%s đã đóng đợt tuyển của tin \"%s\" vào lúc %s. Tin không còn nhận hồ sơ mới.",
                actorName, posting.getPostingTitle(), displayTime(now));
    }

    public static String applicationHrPassedContent(Application application, String hrName) {
        return String.format(
                "%s đã duyệt Đạt hồ sơ của ứng viên %s (Vị trí: %s). Vui lòng xem xét và duyệt hồ sơ ở bước trưởng bộ phận.",
                hrName, candidateName(application), application.getJobPosting().getPostingTitle());
    }

    public static String applicationHmPassedContent(Application application, String reviewerName) {
        return String.format(
                "%s đã duyệt Đạt hồ sơ của ứng viên %s (Vị trí: %s). Bạn có thể lên lịch phỏng vấn.",
                reviewerName, candidateName(application), application.getJobPosting().getPostingTitle());
    }

    // "dd/MM/yyyy HH:mm – HH:mm"; chưa có giờ kết thúc thì chỉ hiện giờ bắt đầu
    public static String interviewTime(InterviewSchedule schedule) {
        String when = displayTime(schedule.getStartTime());
        if (schedule.getEndTime() != null) {
            when = when + " – " + schedule.getEndTime().format(CLOCK_TIME);
        }
        return when;
    }

    public static String interviewPlace(InterviewSchedule schedule) {
        String where = "Chưa xác định";
        if (schedule.getInterviewFormat() != null) {
            where = schedule.getInterviewFormat().getDisplayName();
        }
        if (schedule.getLocationOrLink() != null && !schedule.getLocationOrLink().isBlank()) {
            where = where + ", địa điểm / link: " + schedule.getLocationOrLink().trim();
        }
        return where;
    }

    public static String interviewPanelContent(InterviewSchedule schedule, String internalNote) {
        Application application = schedule.getApplication();
        return String.format("Phỏng vấn ứng viên %s (Vị trí: %s) lúc %s, hình thức %s.%s",
                candidateName(application), application.getJobPosting().getPostingTitle(),
                interviewTime(schedule), interviewPlace(schedule), internalNote);
    }

    public static String interviewCandidateContent(InterviewSchedule schedule) {
        return String.format("Lịch phỏng vấn vị trí \"%s\" của bạn: lúc %s, hình thức %s.",
                schedule.getApplication().getJobPosting().getPostingTitle(),
                interviewTime(schedule), interviewPlace(schedule));
    }

    public static String interviewResultContent(Application application, String hmName, String result) {
        return String.format("%s đã chốt kết quả phỏng vấn của ứng viên %s (Vị trí: %s): %s.",
                hmName, candidateName(application), application.getJobPosting().getPostingTitle(), result);
    }

    public static String offerPendingContent(OfferProposal offer, String hrName) {
        String salary = "Thỏa thuận";
        if (offer.getProposedSalary() != null) {
            salary = String.format("%,d ₫", offer.getProposedSalary().longValue());
        }
        return String.format(
                "Chuyên viên tuyển dụng %s đã nộp đề xuất nhận việc cho ứng viên %s (Vị trí: %s, Lương chính thức: %s). Vui lòng xem xét và thực hiện phê duyệt.",
                hrName, candidateName(offer.getApplication()), positionTitle(offer), salary);
    }

    public static String offerApprovedContent(OfferProposal offer, String directorName, String comments) {
        String noteText = "";
        if (comments != null && !comments.isBlank()) {
            noteText = " Ghi chú: \"" + comments.trim() + "\".";
        }
        return String.format(
                "Giám đốc %s đã phê duyệt đề xuất nhận việc cho ứng viên %s (Vị trí: %s).%s Bạn có thể tiến hành phát hành Thư mời nhận việc cho ứng viên.",
                directorName, candidateName(offer.getApplication()), positionTitle(offer), noteText);
    }

    public static String offerRejectedContent(OfferProposal offer, String directorName, String comments) {
        String reasonText = "Không có lý do cụ thể";
        if (comments != null && !comments.isBlank()) {
            reasonText = comments.trim();
        }
        return String.format(
                "Giám đốc %s đã từ chối đề xuất nhận việc cho ứng viên %s (Vị trí: %s). Lý do: \"%s\". Vui lòng kiểm tra lại điều khoản và chỉnh sửa bản thảo nếu cần.",
                directorName, candidateName(offer.getApplication()), positionTitle(offer), reasonText);
    }

    public static String offerLetterSentContent(OfferProposal offer) {
        return String.format(
                "Chúc mừng bạn! Nhà tuyển dụng đã gửi Thư mời nhận việc cho vị trí \"%s\". Vui lòng bấm vào đây để xem chi tiết điều khoản và phản hồi thư mời.",
                positionTitle(offer));
    }

    // Lý do từ chối offer của ứng viên, dạng " Lý do: "...".", để trống nếu ứng viên không ghi
    public static String declineReasonText(String declineReason) {
        if (declineReason == null || declineReason.isBlank()) {
            return "";
        }
        return String.format(" Lý do: \"%s\".", declineReason.trim());
    }
}
