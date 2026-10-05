package com.group2.rms.auth.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.group2.rms.auth.exception.PasswordRecoveryUnavailableException;
import com.group2.rms.auth.exception.PasswordResetDeliveryException;

/**
 * SMTP adapter; credentials and public URL come only from runtime
 * configuration.
 */
@Component
public class PasswordResetEmailSender {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String publicBaseUrl;

    public PasswordResetEmailSender(ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.mail.from:}") String from,
            @Value("${app.public.base.url:}") String publicBaseUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.publicBaseUrl = publicBaseUrl;
    }

    public boolean isConfigured() {
        return mailSender.getIfAvailable() != null && !from.isBlank() && !publicBaseUrl.isBlank();
    }

    public void send(String to, String token, String otp) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from.isBlank() || publicBaseUrl.isBlank()) {
            throw new PasswordRecoveryUnavailableException();
        }
        String link = publicBaseUrl.replaceAll("/+$", "") + "/reset-password/" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Reset Password RMS");
        message.setText("Your verification OTP code is: " + otp + "\n\n"
                + "Open this link and enter the OTP code above to reset your password:\n"
                + link + "\n\nThis code and link expire after 15 minutes and can be used once."
                + " If you did not request this, ignore this email.");
        try {
            sender.send(message);
        } catch (MailSendException exception) {
            throw new PasswordResetDeliveryException();
        }

    }

    public void send(String to, String token) {
        send(to, token, "");
    }
}
