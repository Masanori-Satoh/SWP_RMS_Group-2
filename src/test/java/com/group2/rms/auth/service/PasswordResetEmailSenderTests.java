package com.group2.rms.auth.service;

import com.group2.rms.auth.exception.PasswordRecoveryUnavailableException;
import com.group2.rms.auth.exception.PasswordResetDeliveryException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailSenderTests {

    private static final String FROM = "rms@example.test";
    private static final String TO = "candidate7@example.test";

    @Mock
    private ObjectProvider<JavaMailSender> provider;

    @Mock
    private JavaMailSender sender;

    @Test
    @DisplayName("AUTH-RESET-MAIL-01 | Không có JavaMailSender: chưa cấu hình và từ chối gửi")
    void sendOtp_missingSender_rejectsUnavailableConfiguration() {
        // Given
        when(provider.getIfAvailable()).thenReturn(null);
        PasswordResetEmailSender service = new PasswordResetEmailSender(provider, FROM);

        // When
        assertEquals(false, service.isConfigured());
        PasswordRecoveryUnavailableException error = assertThrows(PasswordRecoveryUnavailableException.class,
                () -> service.sendOtp(TO, "123456"));

        // Then
        assertNotNull(error);
        verify(sender, times(0)).createMimeMessage();
        verify(sender, times(0)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("AUTH-RESET-MAIL-02 | Địa chỉ from null: chưa cấu hình và không tạo email")
    void sendOtp_nullFrom_rejectsBeforeCreatingMessage() {
        // Given
        when(provider.getIfAvailable()).thenReturn(sender);
        PasswordResetEmailSender service = new PasswordResetEmailSender(provider, null);

        // When
        assertEquals(false, service.isConfigured());
        PasswordRecoveryUnavailableException error = assertThrows(PasswordRecoveryUnavailableException.class,
                () -> service.sendOtp(TO, "123456"));

        // Then
        assertNotNull(error);
        verify(sender, times(0)).createMimeMessage();
        verify(sender, times(0)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("AUTH-RESET-MAIL-03 | Địa chỉ from blank: từ chối gửi trước khi tạo email")
    void sendOtp_blankFrom_rejectsBeforeCreatingMessage() {
        // Given
        when(provider.getIfAvailable()).thenReturn(sender);
        PasswordResetEmailSender service = new PasswordResetEmailSender(provider, "   ");

        // When
        assertEquals(false, service.isConfigured());
        PasswordRecoveryUnavailableException error = assertThrows(PasswordRecoveryUnavailableException.class,
                () -> service.sendOtp(TO, "123456"));

        // Then
        assertNotNull(error);
        verify(sender, times(0)).createMimeMessage();
        verify(sender, times(0)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("AUTH-RESET-MAIL-04 | Cấu hình hợp lệ: đúng người gửi, người nhận, tiêu đề và OTP trong cả text/HTML")
    void sendOtp_validConfiguration_buildsExpectedMessage() throws Exception {
        // Given: MimeMessage chỉ ở bộ nhớ, thao tác gửi được mock.
        when(provider.getIfAvailable()).thenReturn(sender);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        PasswordResetEmailSender service = new PasswordResetEmailSender(provider, FROM);

        // When
        assertEquals(true, service.isConfigured());
        service.sendOtp(TO, "123456");
        message.saveChanges(); // Hoàn tất header MIME trước khi đọc email trong bộ nhớ.

        // Then
        assertEquals(FROM, message.getFrom()[0].toString());
        assertEquals(TO, message.getAllRecipients()[0].toString());
        assertEquals("Mã xác minh đặt lại mật khẩu Mộc", message.getSubject());
        String plainText = findBody(message, "text/plain");
        String html = findBody(message, "text/html");
        assertNotNull(plainText);
        assertNotNull(html);
        assertEquals(true, plainText.contains("123456"));
        assertEquals(true, html.contains("123456"));
        assertEquals(false, plainText.contains("{{otp}}"));
        assertEquals(false, html.contains("{{otp}}"));
        verify(sender, times(1)).send(message);
    }

    @Test
    @DisplayName("AUTH-RESET-MAIL-05 | JavaMailSender báo lỗi gửi: chuyển thành exception của luồng Authentication")
    void sendOtp_mailFailure_returnsDeliveryError() {
        // Given
        when(provider.getIfAvailable()).thenReturn(sender);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(message);
        PasswordResetEmailSender service = new PasswordResetEmailSender(provider, FROM);

        // When
        PasswordResetDeliveryException error = assertThrows(PasswordResetDeliveryException.class,
                () -> service.sendOtp(TO, "123456"));

        // Then
        assertNotNull(error);
        verify(sender, times(1)).send(message);
    }

    @Test
    @DisplayName("AUTH-RESET-MAIL-06 | Địa chỉ người nhận sai cú pháp: báo lỗi delivery và không gọi send")
    void sendOtp_invalidRecipient_doesNotSendMessage() {
        // Given
        when(provider.getIfAvailable()).thenReturn(sender);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        PasswordResetEmailSender service = new PasswordResetEmailSender(provider, FROM);

        // When
        PasswordResetDeliveryException error = assertThrows(PasswordResetDeliveryException.class,
                () -> service.sendOtp("invalid@@example.test", "123456"));

        // Then
        assertNotNull(error);
        verify(sender, times(0)).send(any(MimeMessage.class));
    }

    // Đọc nội dung MIME thật để kiểm tra OTP trong từng phiên bản text/HTML.
    private String findBody(Part part, String mimeType) throws Exception {
        if (part.isMimeType(mimeType)) {
            return (String) part.getContent();
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                String body = findBody(multipart.getBodyPart(index), mimeType);
                if (body != null) {
                    return body;
                }
            }
        }
        return null;
    }
}

