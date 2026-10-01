package com.group2.rms.auth;

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
        message.setSubject("Đặt lại mật khẩu RMS");
        message.setText("Mở liên kết sau để xác minh email và đặt lại mật khẩu:\n"
                + link + "\n\nLiên kết hết hạn sau 15 phút và chỉ dùng một lần."
                + " Nếu bạn không yêu cầu, hãy bỏ qua email này.");
        sender.send(message);
    }
}
