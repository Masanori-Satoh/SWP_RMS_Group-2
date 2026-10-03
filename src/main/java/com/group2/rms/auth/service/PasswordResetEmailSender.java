package com.group2.rms.auth.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** SMTP adapter; credentials and public URL come only from runtime configuration. */
@Component
public class PasswordResetEmailSender {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String publicBaseUrl;

    public PasswordResetEmailSender(ObjectProvider<JavaMailSender> mailSender,
                                    @Value("${APP_MAIL_FROM:}") String from,
                                    @Value("${APP_PUBLIC_BASE_URL:}") String publicBaseUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.publicBaseUrl = publicBaseUrl;
    }

    public boolean isConfigured() {
        return mailSender.getIfAvailable() != null && !from.isBlank() && !publicBaseUrl.isBlank();
    }

    public void send(String to, String token) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from.isBlank() || publicBaseUrl.isBlank()) {
            throw new IllegalStateException("Password recovery email is not configured");
        }
        String link = publicBaseUrl.replaceAll("/+$", "") + "/reset-password/" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Reset Password RMS");
        message.setText("Open this link to verify your email and reset your password:\n"
                + link + "\n\nThis link expires after 15 minutes and can be used once."
                + " If you did not request this, ignore this email.");
        sender.send(message);
    }
}
