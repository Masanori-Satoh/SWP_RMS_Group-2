package com.group2.rms.admin.service;

import com.group2.rms.admin.dto.MonitorRowResponse;
import com.group2.rms.admin.dto.ProbeOutcomeResponse;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Manual SMTP connectivity checks only: no message creation, recipients or delivery. */
@Service
public class EmailMonitoringService {
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String from;
    private final AtomicReference<MonitorRowResponse> lastProbe = new AtomicReference<>();

    public EmailMonitoringService(ObjectProvider<JavaMailSender> mailSenders,
            @Value("${app.mail.from:}") String from) {
        this.mailSenders = mailSenders;
        this.from = from;
    }

    public MonitorRowResponse row() {
        if (configuredSender() == null) return emailRow("Unconfigured", null, null);
        MonitorRowResponse latest = lastProbe.get();
        return latest != null ? latest : emailRow("Not check", null, null);
    }

    public ProbeOutcomeResponse probe() {
        JavaMailSenderImpl sender = configuredSender();
        if (sender == null) {
            return new ProbeOutcomeResponse(false, "Dịch vụ email chưa có cấu hình SMTP và địa chỉ gửi hợp lệ.", 0);
        }
        long started = System.nanoTime();
        boolean success = false;
        String detail;
        try {
            connectionProbe(sender).testConnection();
            success = true;
            detail = "Kết nối SMTP thành công. Không gửi email thử.";
        } catch (AuthenticationFailedException exception) {
            detail = "Xác thực SMTP thất bại. Kiểm tra thông tin đăng nhập và cấu hình bảo mật.";
        } catch (MessagingException | RuntimeException exception) {
            // Provider exception messages can contain account/server details; never expose them.
            detail = "Không thể kết nối SMTP. Kiểm tra cấu hình, mạng và TLS.";
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        lastProbe.set(emailRow(success ? "OPERATIONAL" : "FAILED", elapsedMs, LocalDateTime.now()));
        return new ProbeOutcomeResponse(success, detail, elapsedMs);
    }

    private JavaMailSenderImpl configuredSender() {
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (!(sender instanceof JavaMailSenderImpl smtp) || !StringUtils.hasText(smtp.getHost())
                || !StringUtils.hasText(from)
                || !("smtp".equals(protocol(smtp)) || "smtps".equals(protocol(smtp)))) return null;
        try {
            new InternetAddress(from, true).validate();
        } catch (MessagingException exception) {
            return null;
        }
        String prefix = "mail." + protocol(smtp) + ".";
        if (Boolean.parseBoolean(smtp.getSession().getProperty(prefix + "auth"))
                && (!StringUtils.hasText(smtp.getUsername()) || !StringUtils.hasText(smtp.getPassword()))) return null;
        return smtp;
    }

    JavaMailSenderImpl connectionProbe(JavaMailSenderImpl sender) {
        // Copy delivery settings so monitoring timeouts/debug settings never alter real mail delivery.
        Properties properties = new Properties();
        properties.putAll(sender.getSession().getProperties());
        String prefix = "mail." + protocol(sender) + ".";
        for (String timeout : new String[]{"connectiontimeout", "timeout", "writetimeout"}) {
            properties.setProperty(prefix + timeout, "5000");
        }
        properties.setProperty("mail.debug", "false");
        properties.setProperty("mail.debug.auth", "false");
        JavaMailSenderImpl probe = new JavaMailSenderImpl();
        probe.setJavaMailProperties(properties);
        probe.setHost(sender.getHost());
        probe.setPort(sender.getPort());
        probe.setProtocol(protocol(sender));
        probe.setUsername(sender.getUsername());
        probe.setPassword(sender.getPassword());
        return probe;
    }

    private String protocol(JavaMailSenderImpl sender) {
        return sender.getProtocol() != null ? sender.getProtocol() : JavaMailSenderImpl.DEFAULT_PROTOCOL;
    }

    private MonitorRowResponse emailRow(String status, Long elapsedMs, LocalDateTime checkedAt) {
        return new MonitorRowResponse("email", "Email Service", "External Integration", status, elapsedMs, checkedAt);
    }
}
