package com.group2.rms.auth.service;

import com.group2.rms.auth.dto.AccountSnapshotResponse;
import com.group2.rms.auth.exception.PasswordRecoveryFlowException;
import com.group2.rms.auth.exception.PasswordRecoveryFlowException.Step;
import com.group2.rms.auth.exception.PasswordRecoveryUnavailableException;
import com.group2.rms.auth.exception.PasswordResetDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTests {

    private static final Instant NOW = Instant.parse("2026-10-19T02:00:00Z");
    private static final String EMAIL = "candidate7@example.test";
    private static final String OTP_HASH = "mock-otp-hash";
    private static final String NEW_PASSWORD = "NewPass123!";

    @Mock
    private PasswordResetService resetService;

    @Mock
    private PasswordResetEmailSender emailSender;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private Clock clock;

    private PasswordRecoveryService service;
    private AccountSnapshotResponse account;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        service = new PasswordRecoveryService(resetService, emailSender, encoder, clock);
        account = new AccountSnapshotResponse(7, EMAIL, "old-password-hash");
        session = new MockHttpSession();
    }

    @Test
    @DisplayName("AUTH-RECOVERY-01 | Email Active và gửi mail thành công: tạo trạng thái chờ OTP, chưa đổi mật khẩu")
    void requestReset_validAccount_createsPendingOtp() {
        // Given
        prepareRequest();
        when(resetService.isCurrent(account)).thenReturn(true);

        // When
        service.requestReset(EMAIL, session);

        // Then
        assertEquals(true, service.hasPendingReset(session));
        assertEquals(false, service.canResetPassword(session));
        assertEquals(EMAIL, service.getPendingResetEmail(session));
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-02 | SMTP chưa cấu hình: báo unavailable trước khi tìm tài khoản hoặc gửi OTP")
    void requestReset_emailNotConfigured_rejectsBeforeLookup() {
        // Given
        when(emailSender.isConfigured()).thenReturn(false);

        // When
        assertThrows(PasswordRecoveryUnavailableException.class,
                () -> service.requestReset(EMAIL, session));

        // Then
        verify(resetService, times(0)).findActiveAccount(anyString());
        verify(emailSender, times(0)).sendOtp(anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-03 | Email không có account Active: giữ lỗi tìm tài khoản, không gửi OTP")
    void requestReset_accountLookupFails_doesNotSendEmail() {
        // Given
        when(emailSender.isConfigured()).thenReturn(true);
        when(resetService.findActiveAccount(EMAIL)).thenThrow(new PasswordRecoveryFlowException(
                Step.FORGOT, "email", "EMAIL_NOT_FOUND", "Account missing."));

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.requestReset(EMAIL, session));

        // Then
        assertEquals("EMAIL_NOT_FOUND", error.getErrorCode());
        verify(emailSender, times(0)).sendOtp(anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-04 | Gửi lại yêu cầu ngay: báo cooldown, không gửi email thứ hai")
    void requestReset_beforeCooldownEnds_doesNotSendAgain() {
        // Given
        prepareRequest();
        service.requestReset(EMAIL, session);

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.requestReset(EMAIL, session));

        // Then
        assertEquals("RESET_EMAIL_COOLDOWN", error.getErrorCode());
        assertEquals("email", error.getField());
        assertEquals(true, error.getRetryAfterSeconds() > 0);
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-05 | Gửi email lỗi: không tạo trạng thái OTP và cho phép thử gửi lại")
    void requestReset_deliveryFailure_releasesReservationForRetry() {
        // Given: Lần gửi thứ nhất lỗi, lần thứ hai được mock thành công.
        prepareRequest();
        when(resetService.isCurrent(account)).thenReturn(true);
        doThrow(new PasswordResetDeliveryException()).doNothing()
                .when(emailSender).sendOtp(eq(EMAIL), anyString());
        assertThrows(PasswordResetDeliveryException.class, () -> service.requestReset(EMAIL, session));
        assertEquals(false, service.hasPendingReset(session));

        // When: Thử lại ngay sau lần gửi thất bại.
        service.requestReset(EMAIL, session);

        // Then
        assertEquals(true, service.hasPendingReset(session));
        verify(emailSender, times(2)).sendOtp(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-06 | Chưa có yêu cầu OTP: không cho xác minh mã")
    void verifyOtp_withoutPendingRequest_rejectsRequest() {
        // Given: Session mới chưa gửi yêu cầu quên mật khẩu.

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.verifyOtp("123456", session));

        // Then
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        verify(encoder, times(0)).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-07 | OTP đúng và còn hạn: cho phép tới bước đặt mật khẩu mới")
    void verifyOtp_correctCode_allowsPasswordReset() {
        // Given
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);
        when(encoder.matches("123456", OTP_HASH)).thenReturn(true);

        // When
        service.verifyOtp("123456", session);

        // Then
        assertEquals(true, service.canResetPassword(session));
        assertEquals(false, service.hasPendingReset(session));
        verify(encoder, times(1)).matches("123456", OTP_HASH);
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-08 | OTP đủ 6 số nhưng sai: báo OTP_INVALID và giữ bước chờ mã")
    void verifyOtp_incorrectCode_keepsPendingRequest() {
        // Given
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);
        when(encoder.matches("123456", OTP_HASH)).thenReturn(false);

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.verifyOtp("123456", session));

        // Then
        assertEquals("OTP_INVALID", error.getErrorCode());
        assertEquals(true, service.hasPendingReset(session));
        assertEquals(false, service.canResetPassword(session));
    }

    @Test
    @DisplayName("AUTH-RECOVERY-09 | OTP sai định dạng: không gọi so khớp hash")
    void verifyOtp_malformedCode_rejectsBeforeHashComparison() {
        // Given
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.verifyOtp("abc", session));

        // Then
        assertEquals("OTP_INVALID", error.getErrorCode());
        verify(encoder, times(0)).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-10 | Sai OTP lần thứ 5: xóa trạng thái recovery và chưa đổi mật khẩu")
    void verifyOtp_fifthWrongAttempt_clearsResetState() {
        // Given
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);
        when(encoder.matches("123456", OTP_HASH)).thenReturn(false);
        for (int attempt = 1; attempt <= 4; attempt++) {
            PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                    () -> service.verifyOtp("123456", session));
            assertEquals("OTP_INVALID", error.getErrorCode());
        }

        // When: Lần sai thứ 5.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.verifyOtp("123456", session));

        // Then
        assertEquals("OTP_ATTEMPTS_EXCEEDED", error.getErrorCode());
        assertEquals(false, service.hasPendingReset(session));
        assertEquals(false, service.canResetPassword(session));
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-11 | OTP đến đúng hạn 15 phút: bị từ chối và xóa trạng thái, không cần sleep")
    void verifyOtp_atExpiryBoundary_rejectsExpiredCode() {
        // Given: Dùng clock mock để đi thẳng tới hạn, không chờ 15 phút thật.
        requestCode();
        when(clock.instant()).thenReturn(NOW.plusSeconds(900));

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.verifyOtp("123456", session));

        // Then
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        assertEquals(false, service.hasPendingReset(session));
        verify(encoder, times(0)).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-12 | Account đã đổi sau khi gửi OTP: vô hiệu hóa yêu cầu recovery cũ")
    void verifyOtp_accountSnapshotChanged_rejectsRequest() {
        // Given
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(false);

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.verifyOtp("123456", session));

        // Then
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        assertEquals(false, service.canResetPassword(session));
        verify(encoder, times(0)).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-13 | Chưa xác minh OTP: không được lưu mật khẩu mới")
    void completeReset_beforeOtpVerification_doesNotChangePassword() {
        // Given: Có mã đã gửi nhưng người dùng chưa xác minh.
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.completeReset(NEW_PASSWORD, session));

        // Then
        assertEquals("OTP_REQUIRED", error.getErrorCode());
        assertEquals(Step.OTP, error.getStep());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-14 | OTP đã xác minh: gọi đổi mật khẩu đúng snapshot/thời hạn và xóa trạng thái sau thành công")
    void completeReset_verifiedOtp_changesPasswordAndClearsState() {
        // Given
        verifyCode();

        // When
        service.completeReset(NEW_PASSWORD, session);

        // Then
        verify(resetService, times(1)).changePassword(account, NEW_PASSWORD, NOW.plusSeconds(900));
        assertEquals(false, service.canResetPassword(session));
        assertEquals(false, service.hasPendingReset(session));
    }

    @Test
    @DisplayName("AUTH-RECOVERY-15 | Gửi lại thao tác lưu sau reset thành công: không đổi mật khẩu lần thứ hai")
    void completeReset_alreadyCompleted_rejectsReplay() {
        // Given
        verifyCode();
        service.completeReset(NEW_PASSWORD, session);

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.completeReset(NEW_PASSWORD, session));

        // Then
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        verify(resetService, times(1)).changePassword(account, NEW_PASSWORD, NOW.plusSeconds(900));
    }

    @Test
    @DisplayName("AUTH-RECOVERY-16 | Lưu mật khẩu thất bại: giữ trạng thái OTP đã xác minh để người dùng sửa và thử lại")
    void completeReset_passwordSaveFails_keepsVerifiedState() {
        // Given
        verifyCode();
        doThrow(new PasswordRecoveryFlowException(Step.PASSWORD, "password", "PASSWORD_INVALID", "Invalid password."))
                .when(resetService).changePassword(account, NEW_PASSWORD, NOW.plusSeconds(900));

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.completeReset(NEW_PASSWORD, session));

        // Then
        assertEquals("PASSWORD_INVALID", error.getErrorCode());
        assertEquals(true, service.canResetPassword(session));
    }

    @Test
    @DisplayName("AUTH-RECOVERY-17 | Session chưa có yêu cầu recovery: thời gian chờ gửi lại bằng 0")
    void getResendRemainingSeconds_withoutRequest_returnsZero() {
        // When
        long result = service.getResendRemainingSeconds(session);

        // Then
        assertEquals(0L, result);
    }

    @Test
    @DisplayName("AUTH-RECOVERY-18 | OTP đã xác minh: xác minh lại không so khớp mã lần nữa")
    void verifyOtp_alreadyVerified_doesNotCompareCodeAgain() {
        // Given
        verifyCode();

        // When
        service.verifyOtp("123456", session);

        // Then
        assertEquals(true, service.canResetPassword(session));
        verify(encoder, times(1)).matches("123456", OTP_HASH);
    }

    @Test
    @DisplayName("AUTH-RECOVERY-19 | OTP null: báo OTP_INVALID, không so hash hoặc đổi mật khẩu")
    void verifyOtp_nullCode_rejectsBeforeHashComparison() {
        // Given
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.verifyOtp(null, session));

        // Then
        assertEquals("OTP_INVALID", error.getErrorCode());
        assertEquals(Step.OTP, error.getStep());
        assertEquals(false, service.canResetPassword(session));
        verify(encoder, times(0)).matches(any(), anyString());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-20 | OTP blank: giữ trạng thái chờ mã, không so hash")
    void verifyOtp_blankCode_keepsPendingRequest() {
        // Given
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.verifyOtp("   ", session));

        // Then
        assertEquals("OTP_INVALID", error.getErrorCode());
        assertEquals(true, service.hasPendingReset(session));
        assertEquals(false, service.canResetPassword(session));
        verify(encoder, times(0)).matches(anyString(), anyString());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-21 | Còn 1199,5 giây cooldown: làm tròn lên 1200 giây để UI chưa cho gửi sớm")
    void getResendRemainingSeconds_fractionalSecond_roundsUp() {
        // Given
        requestCode();
        when(clock.instant()).thenReturn(NOW.plusMillis(500));

        // When
        long result = service.getResendRemainingSeconds(session);

        // Then
        assertEquals(1200L, result);
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-22 | Đúng hạn cooldown 20 phút: thời gian chờ bằng 0")
    void getResendRemainingSeconds_atCooldownBoundary_returnsZero() {
        // Given
        requestCode();
        when(clock.instant()).thenReturn(NOW.plusSeconds(1200));

        // When
        long result = service.getResendRemainingSeconds(session);

        // Then
        assertEquals(0L, result);
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-23 | Gửi lại đúng phút 20: gửi email thứ hai và tạo OTP mới còn hạn")
    void requestReset_atCooldownBoundary_sendsNewCode() {
        // Given
        requestCode();
        when(clock.instant()).thenReturn(NOW.plusSeconds(1200));
        when(resetService.isCurrent(account)).thenReturn(true);

        // When
        service.requestReset(EMAIL, session);

        // Then
        verify(emailSender, times(2)).sendOtp(eq(EMAIL), anyString());
        assertEquals(true, service.hasPendingReset(session));
        assertEquals(false, service.canResetPassword(session));
        assertEquals(EMAIL, service.getPendingResetEmail(session));
        assertEquals(1200L, service.getResendRemainingSeconds(session));
    }

    @Test
    @DisplayName("AUTH-RECOVERY-24 | Ghi nhận vấn đề: OTP hết hạn phút 15 nhưng vẫn phải chờ 300 giây để yêu cầu mã mới")
    void requestReset_otpExpiredButCooldownActive_documentsFiveMinuteGap() {
        // Given: Đây là test ghi nhận hành vi hiện tại để báo bug, không khẳng định hành vi đúng nghiệp vụ.
        requestCode();
        when(clock.instant()).thenReturn(NOW.plusSeconds(900));
        assertEquals(false, service.hasPendingReset(session));

        // When
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
                () -> service.requestReset(EMAIL, session));

        // Then: Mã cũ không còn dùng được, nhưng yêu cầu mới vẫn bị chặn thêm 5 phút.
        assertEquals("RESET_EMAIL_COOLDOWN", error.getErrorCode());
        assertEquals(300L, error.getRetryAfterSeconds());
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-25 | Trước hạn OTP 1 nano giây: vẫn cho xác minh mã đúng")
    void verifyOtp_justBeforeExpiry_acceptsCorrectCode() {
        // Given
        requestCode();
        when(clock.instant()).thenReturn(NOW.plusSeconds(900).minusNanos(1));
        when(resetService.isCurrent(account)).thenReturn(true);
        when(encoder.matches("123456", OTP_HASH)).thenReturn(true);

        // When
        service.verifyOtp("123456", session);

        // Then
        assertEquals(true, service.canResetPassword(session));
        verify(encoder, times(1)).matches("123456", OTP_HASH);
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    private void prepareRequest() {
        when(emailSender.isConfigured()).thenReturn(true);
        when(resetService.findActiveAccount(EMAIL)).thenReturn(account);
        when(clock.instant()).thenReturn(NOW);
        when(encoder.encode(anyString())).thenReturn(OTP_HASH);
    }

    // Session là đối tượng test trong bộ nhớ; không có HTTP server hoặc SMTP thật.
    private void requestCode() {
        prepareRequest();
        service.requestReset(EMAIL, session);
    }

    private void verifyCode() {
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);
        when(encoder.matches("123456", OTP_HASH)).thenReturn(true);
        service.verifyOtp("123456", session);
    }
}
