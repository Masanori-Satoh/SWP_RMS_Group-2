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
    public void sendInterview(Long id) {
        Integer actorId = actor();
        if (!audit.has("InterviewSchedule", id, "MailSent:InterviewInvitation"))
            deliver(interviewMessage(id), actorId);
    }
    public void resendInterview(Long id) { deliver(interviewMessage(id), actor()); }

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
        return jdbc.query("SELECT u.Email,u.FullName,j.PostingTitle,s.StartTime,s.EndTime,s.InterviewFormat,s.LocationOrLink " +
                "FROM InterviewSchedule s JOIN Application a ON a.ApplicationId=s.ApplicationId " +
                "JOIN Candidate c ON c.CandidateId=a.CandidateId JOIN [User] u ON u.UserId=c.UserId " +
                "JOIN JobPosting j ON j.JobPostingId=a.JobPostingId " +
                "WHERE s.InterviewId=? AND s.InterviewStatus IN ('Scheduled','Rescheduled') " +
                "AND a.ApplicationStatus NOT IN ('HM_Suggested','Rejected')", (rs, n) ->
                new RecruitmentEmailMessage("InterviewSchedule", id, "InterviewInvitation", rs.getString(1),
                    "Thư mời phỏng vấn — " + rs.getString(3),
                    "Xin chào " + rs.getString(2) + ",\n\nMộc mời bạn phỏng vấn vị trí " + rs.getString(3)
                    + ".\nBắt đầu: " + rs.getTimestamp(4).toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + "\nKết thúc: " + rs.getTimestamp(5).toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + "\nHình thức: " + ("Online_GoogleMeet".equals(rs.getString(6)) ? "Trực tuyến" : "Tại văn phòng")
                    + "\nĐịa điểm / đường dẫn: " + java.util.Objects.toString(rs.getString(7), "HR sẽ liên hệ bổ sung")
                    + "\n\nVui lòng liên hệ HR nếu bạn cần thay đổi lịch.\nTrân trọng,\nPhòng Nhân sự Mộc"),
                id).stream().findFirst().orElseThrow(() -> new TalentPoolException("Lịch này không còn đủ điều kiện gửi thư mời."));
    }
    private void deliver(RecruitmentEmailMessage mail, Integer actorId) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Recruitment email must be sent after the business transaction.");
        JavaMailSender smtp = sender.getIfAvailable();
        try {
            if (smtp == null || from == null || from.isBlank()) throw new MailSendException("SMTP chưa cấu hình.");
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from); message.setTo(mail.to()); message.setSubject(mail.subject()); message.setText(mail.body());
            smtp.send(message);
        } catch (MailException e) {
            audit.recordMail(actorId, mail.entity(), mail.id(), mail.kind(), false);
            log.warn("Recruitment email failed: entity={}, id={}, type={}", mail.entity(), mail.id(), e.getClass().getSimpleName());
            throw new RecruitmentEmailException();
        }
        audit.recordMail(actorId, mail.entity(), mail.id(), mail.kind(), true);
    }
    private Integer actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_HR")))
            throw new AccessDeniedException("Chỉ HR được gửi email tuyển dụng.");
        return currentUser.requireUser().getUserId();
    }
}

