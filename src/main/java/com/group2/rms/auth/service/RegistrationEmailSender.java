package com.group2.rms.auth.service;

import com.group2.rms.auth.exception.RegistrationFlowException;
import com.group2.rms.auth.exception.RegistrationFlowException.Step;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class RegistrationEmailSender {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public RegistrationEmailSender(ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public boolean isConfigured() {
        return mailSender.getIfAvailable() != null && from != null && !from.isBlank();
    }

    public void sendOtp(String to, String otp) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from == null || from.isBlank()) {
            throw deliveryFailure("REGISTER_EMAIL_UNAVAILABLE");
        }
        try {
            var helper = new MimeMessageHelper(sender.createMimeMessage(), true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Mã xác minh đăng ký tài khoản Mộc");
            helper.setText(render("txt", otp), render("html",
                    HtmlUtils.htmlEscape(otp, StandardCharsets.UTF_8.name())));
            sender.send(helper.getMimeMessage());
        } catch (MailException | MessagingException | IOException exception) {
            throw deliveryFailure("REGISTER_EMAIL_DELIVERY_FAILED");
        }
    }

    private String render(String extension, String otp) throws IOException {
        var resource = new ClassPathResource("templates/auth/email/registration-otp." + extension);
        try (var input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("{{otp}}", otp);
        }
    }

    private static RegistrationFlowException deliveryFailure(String code) {
        return new RegistrationFlowException(Step.REGISTER, null, code,
                "Unable to send the verification email. Please try again later.");
    }
}
