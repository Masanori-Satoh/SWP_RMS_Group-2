package com.group2.rms.candidate.talentpool.service;

import com.group2.rms.candidate.talentpool.dto.RecruitmentEmailMessage;
import com.group2.rms.candidate.talentpool.repository.TalentPoolAuditRepository;
import com.group2.rms.candidate.talentpool.exception.RecruitmentEmailException;
import com.group2.rms.candidate.talentpool.exception.TalentPoolException;
import com.group2.rms.core.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Controller gọi sau khi Service ghi DB đã trả về (transaction đã commit). */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecruitmentEmailService {
    private final ObjectProvider<JavaMailSender> sender;
    private final CurrentUserService currentUser;
    private final JdbcTemplate jdbc;
    private final TalentPoolAuditRepository audit;
    @Value("${app.mail.from:${APP_MAIL_FROM:}}")
    private String from;

    public void sendJobClosure(Integer jobId) {
        Integer actorId = actor();
        boolean failed = false;
        for (Integer id : audit.closureRecipients(jobId)) {
            if (audit.has("Application", id, "MailSent:PostingClosed")) continue;
            try { deliver(closureMessage(id), actorId); }
            catch (RecruitmentEmailException e) { failed = true; }
        }
        if (failed) throw new RecruitmentEmailException();
    }
    public void resendClosure(Integer id) { deliver(closureMessage(id), actor()); }
    // ducnm bổ sung: tạo lịch do HR hoặc Director; gửi thư mời cho cả hai vai trò
    public void sendInterview(Long id) {
        Integer actorId = actor("ROLE_HR", "ROLE_DIRECTOR");
        if (!audit.has("InterviewSchedule", id, "MailSent:InterviewInvitation"))
            deliver(interviewMessage(id), actorId);
    }
    public void resendInterview(Long id) { deliver(interviewMessage(id), actor("ROLE_HR", "ROLE_DIRECTOR")); }
    // ducnm bổ sung: báo ứng viên qua email khi lịch được đổi giờ/địa điểm/hội đồng hoặc bị hủy.
    // Không dùng audit.has(...) để chặn trùng: đổi lịch có thể xảy ra nhiều lần, hủy lịch chỉ xảy ra một lần (trạng thái cuối).
    public void sendInterviewRescheduled(Long id) { deliver(rescheduledMessage(id), actor("ROLE_HR", "ROLE_DIRECTOR")); }
    public void sendInterviewCancelled(Long id, String reason) { deliver(cancelledMessage(id, reason), actor("ROLE_HR", "ROLE_DIRECTOR")); }

    private RecruitmentEmailMessage closureMessage(Integer id) {
        if (!audit.has("Application", id, "PostingClosed"))
            throw new TalentPoolException("Đơn này không bị dừng do đóng đợt tuyển.");
        return jdbc.query("SELECT u.Email,u.FullName,j.PostingTitle FROM Application a " +
                "JOIN Candidate c ON c.CandidateId=a.CandidateId JOIN [User] u ON u.UserId=c.UserId " +
                "JOIN JobPosting j ON j.JobPostingId=a.JobPostingId WHERE a.ApplicationId=?", (rs, n) -> {
            String cancelled = audit.has("Application", id, "InterviewCancelledOnClose")
                    ? "\nCác lịch phỏng vấn chưa diễn ra của hồ sơ này đã được hủy.\n" : "\n";
            return new RecruitmentEmailMessage("Application", id, "PostingClosed", rs.getString(1),
                    "Thông báo dừng đợt tuyển — " + rs.getString(3),
                    "Xin chào " + rs.getString(2) + ",\n\nCảm ơn bạn đã ứng tuyển vị trí " + rs.getString(3)
                    + ". Chúng tôi rất tiếc đợt tuyển dụng này đã dừng và hồ sơ chưa thể tiếp tục." + cancelled
                    + "\nCảm ơn thời gian và sự quan tâm của bạn.\nTrân trọng,\nPhòng Nhân sự Mộc");
        }, id).stream().findFirst().orElseThrow(() -> new TalentPoolException("Không tìm thấy đơn để gửi email."));
    }
    private RecruitmentEmailMessage interviewMessage(Long id) {
        // dunglt bổ sung: không mời phỏng vấn đơn đã kết thúc (ApplicationStatusService.TERMINAL) hay bản nháp đề xuất
        java.util.List<String> excluded = new java.util.ArrayList<>(
                com.group2.rms.candidate.domain.rule.ApplicationStatusService.TERMINAL);
        excluded.add("HM_Suggested");
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(id);
        params.addAll(excluded);
        return jdbc.query("SELECT u.Email,u.FullName,j.PostingTitle,s.StartTime,s.EndTime,s.InterviewFormat,s.LocationOrLink " +
                "FROM InterviewSchedule s JOIN Application a ON a.ApplicationId=s.ApplicationId " +
                "JOIN Candidate c ON c.CandidateId=a.CandidateId JOIN [User] u ON u.UserId=c.UserId " +
                "JOIN JobPosting j ON j.JobPostingId=a.JobPostingId " +
                "WHERE s.InterviewId=? AND s.InterviewStatus IN ('Scheduled','Rescheduled') " +
                "AND a.ApplicationStatus NOT IN (" + String.join(",", java.util.Collections.nCopies(excluded.size(), "?")) + ")", (rs, n) -> {
            var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String name = rs.getString(2), job = rs.getString(3);
            String start = rs.getTimestamp(4).toLocalDateTime().format(fmt);
            String end = rs.getTimestamp(5).toLocalDateTime().format(fmt);
            String format = "Online_GoogleMeet".equals(rs.getString(6)) ? "Trực tuyến" : "Tại văn phòng";
            String location = java.util.Objects.toString(rs.getString(7), "Nhân sự sẽ liên hệ bổ sung");
            String text = "Xin chào " + name + ",\n\nMộc mời bạn phỏng vấn vị trí " + job
                    + ".\nBắt đầu: " + start + "\nKết thúc: " + end + "\nHình thức: " + format
                    + "\nĐịa điểm / đường dẫn: " + location
                    + "\n\nVui lòng liên hệ Nhân sự nếu bạn cần thay đổi lịch.\nTrân trọng,\nPhòng Nhân sự Mộc";
            return new RecruitmentEmailMessage("InterviewSchedule", id, "InterviewInvitation", rs.getString(1),
                    "Thư mời phỏng vấn — " + job, text, interviewHtml(name, job, start, end, format, location));
        }, params.toArray()).stream().findFirst().orElseThrow(() -> new TalentPoolException("Lịch này không còn đủ điều kiện gửi thư mời."));
    }
    /** Lịch vừa được HR/Director đổi giờ, địa điểm, hình thức hoặc hội đồng (đã ở trạng thái Rescheduled). */
    private RecruitmentEmailMessage rescheduledMessage(Long id) {
        return jdbc.query("SELECT u.Email,u.FullName,j.PostingTitle,s.StartTime,s.EndTime,s.InterviewFormat,s.LocationOrLink " +
                "FROM InterviewSchedule s JOIN Application a ON a.ApplicationId=s.ApplicationId " +
                "JOIN Candidate c ON c.CandidateId=a.CandidateId JOIN [User] u ON u.UserId=c.UserId " +
                "JOIN JobPosting j ON j.JobPostingId=a.JobPostingId WHERE s.InterviewId=?", (rs, n) -> {
            var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String name = rs.getString(2), job = rs.getString(3);
            String start = rs.getTimestamp(4).toLocalDateTime().format(fmt);
            String end = rs.getTimestamp(5).toLocalDateTime().format(fmt);
            String format = "Online_GoogleMeet".equals(rs.getString(6)) ? "Trực tuyến" : "Tại văn phòng";
            String location = java.util.Objects.toString(rs.getString(7), "Nhân sự sẽ liên hệ bổ sung");
            String text = "Xin chào " + name + ",\n\nLịch phỏng vấn vị trí " + job + " của bạn đã được cập nhật."
                    + "\nBắt đầu: " + start + "\nKết thúc: " + end + "\nHình thức: " + format
                    + "\nĐịa điểm / đường dẫn: " + location
                    + "\n\nVui lòng liên hệ Nhân sự nếu bạn cần hỗ trợ thêm.\nTrân trọng,\nPhòng Nhân sự Mộc";
            return new RecruitmentEmailMessage("InterviewSchedule", id, "InterviewRescheduled", rs.getString(1),
                    "Lịch phỏng vấn đã được cập nhật — " + job, text);
        }, id).stream().findFirst().orElseThrow(() -> new TalentPoolException("Không tìm thấy lịch phỏng vấn để gửi email."));
    }
    /** Lịch vừa bị HR/Director hủy (đã ở trạng thái Cancelled); reason là lý do HR nhập khi hủy. */
    private RecruitmentEmailMessage cancelledMessage(Long id, String reason) {
        return jdbc.query("SELECT u.Email,u.FullName,j.PostingTitle,s.StartTime FROM InterviewSchedule s " +
                "JOIN Application a ON a.ApplicationId=s.ApplicationId " +
                "JOIN Candidate c ON c.CandidateId=a.CandidateId JOIN [User] u ON u.UserId=c.UserId " +
                "JOIN JobPosting j ON j.JobPostingId=a.JobPostingId WHERE s.InterviewId=?", (rs, n) -> {
            var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String name = rs.getString(2), job = rs.getString(3);
            String start = rs.getTimestamp(4).toLocalDateTime().format(fmt);
            String reasonText = (reason == null || reason.isBlank()) ? "" : "\nLý do: " + reason.trim();
            String text = "Xin chào " + name + ",\n\nLịch phỏng vấn vị trí " + job + " lúc " + start
                    + " của bạn đã bị hủy." + reasonText
                    + "\n\nMộc sẽ liên hệ lại nếu có lịch phỏng vấn mới. Vui lòng liên hệ Nhân sự nếu bạn cần hỗ trợ thêm."
                    + "\nTrân trọng,\nPhòng Nhân sự Mộc";
            return new RecruitmentEmailMessage("InterviewSchedule", id, "InterviewCancelled", rs.getString(1),
                    "Lịch phỏng vấn đã bị hủy — " + job, text);
        }, id).stream().findFirst().orElseThrow(() -> new TalentPoolException("Không tìm thấy lịch phỏng vấn để gửi email."));
    }
    /** Bản HTML của thư mời phỏng vấn: nạp khuôn templates/interview/email/interview-invitation.html, mọi giá trị đều được escape. */
    private static String interviewHtml(String name, String job, String start, String end, String format, String location) {
        String locationHtml = escape(location);
        // Đường dẫn họp trực tuyến (http/https) hiển thị dạng liên kết bấm được; giá trị khác chỉ là chữ
        if (location.startsWith("https://") || location.startsWith("http://")) {
            locationHtml = "<a href=\"" + locationHtml + "\" style=\"color:#1b4332; text-decoration:underline;\">" + locationHtml + "</a>";
        }
        try (var input = new org.springframework.core.io.ClassPathResource("templates/interview/email/interview-invitation.html").getInputStream()) {
            String html = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return html.replace("{{candidateName}}", escape(name)).replace("{{jobTitle}}", escape(job))
                    .replace("{{start}}", escape(start)).replace("{{end}}", escape(end))
                    .replace("{{format}}", escape(format)).replace("{{location}}", locationHtml);
        } catch (java.io.IOException e) {
            return null; // không nạp được khuôn HTML thì vẫn gửi bản chữ thuần
        }
    }
    private static String escape(String value) {
        return org.springframework.web.util.HtmlUtils.htmlEscape(java.util.Objects.toString(value, ""));
    }
    private void deliver(RecruitmentEmailMessage mail, Integer actorId) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Recruitment email must be sent after the business transaction.");
        JavaMailSender smtp = sender.getIfAvailable();
        try {
            if (smtp == null || from == null || from.isBlank()) throw new MailSendException("SMTP chưa cấu hình.");
            if (mail.html() == null) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from); message.setTo(mail.to()); message.setSubject(mail.subject()); message.setText(mail.body());
                smtp.send(message);
            } else {
                // Thư nhiều phần: bản chữ thuần dự phòng + bản HTML (cùng cách làm với email OTP đăng ký)
                var mime = smtp.createMimeMessage();
                var helper = new org.springframework.mail.javamail.MimeMessageHelper(mime, true, java.nio.charset.StandardCharsets.UTF_8.name());
                helper.setFrom(from); helper.setTo(mail.to()); helper.setSubject(mail.subject()); helper.setText(mail.body(), mail.html());
                smtp.send(mime);
            }
        } catch (MailException | jakarta.mail.MessagingException e) {
            audit.recordMail(actorId, mail.entity(), mail.id(), mail.kind(), false);
            log.warn("Recruitment email failed: entity={}, id={}, type={}", mail.entity(), mail.id(), e.getClass().getSimpleName());
            throw new RecruitmentEmailException();
        }
        audit.recordMail(actorId, mail.entity(), mail.id(), mail.kind(), true);
    }
    /** Đóng đợt tuyển: chỉ HR (không có Director ở luồng này). */
    private Integer actor() { return actor("ROLE_HR"); }
    /** Email tuyển dụng do một trong các vai trò được liệt kê gửi (so khớp authority, ví dụ "ROLE_HR"). */
    private Integer actor(String... allowedAuthorities) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        java.util.Set<String> allowed = java.util.Set.of(allowedAuthorities);
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> allowed.contains(a.getAuthority())))
            throw new AccessDeniedException("Bạn không có quyền gửi email tuyển dụng này.");
        return currentUser.requireUser().getUserId();
    }
}

