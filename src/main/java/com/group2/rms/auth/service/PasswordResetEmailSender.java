package com.group2.rms.auth.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.group2.rms.auth.exception.PasswordRecoveryUnavailableException;
import com.group2.rms.auth.exception.PasswordResetDeliveryException;


@Component
public class PasswordResetEmailSender {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public PasswordResetEmailSender(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.mail.from:}") String from) {

        this.mailSender = mailSender;
        this.from = from;
    }

    public boolean isConfigured() {
        // check if mail sender is configured
        if (mailSender.getIfAvailable() != null && from != null && !from.isBlank()) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("null")
    public void sendOtp(String to, String otp) {

        JavaMailSender sender = mailSender.getIfAvailable();
        // check if mail sender not be configured
        if (sender == null || from == null || from.isBlank()) {
            throw new PasswordRecoveryUnavailableException();
        }

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Mã xác minh đặt lại mật khẩu Mộc");
            helper.setText(
                    render("txt", String.valueOf(otp)),
                    render("html", HtmlUtils.htmlEscape(String.valueOf(otp), StandardCharsets.UTF_8.name())));
            sender.send(message);
        } catch (MailException | MessagingException | IOException exception) {
            throw new PasswordResetDeliveryException();
        }
    }

    private String render(String extension, String otp) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/auth/email/password-reset-otp." + extension);
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("{{otp}}", otp);
        }
    }
}
