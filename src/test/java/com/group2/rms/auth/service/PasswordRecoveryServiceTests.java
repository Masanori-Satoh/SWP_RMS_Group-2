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
import java.time.ZoneOffset;

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
    private static final String RESET_STATE = PasswordRecoveryService.class.getName() + ".RESET_STATE";

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
        // Given: Email Active và gửi mail thành công.
        prepareRequest();
        when(resetService.isCurrent(account)).thenReturn(true);

        // When: Gọi service.requestReset với dữ liệu đã chuẩn bị.
        service.requestReset(EMAIL, session);

        // Then: tạo trạng thái chờ OTP, chưa đổi mật khẩu.
        assertEquals(true, service.hasPendingReset(session));
        assertEquals(false, service.canResetPassword(session));
        assertEquals(EMAIL, service.getPendingResetEmail(session));
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-02 | SMTP chưa cấu hình: báo unavailable trước khi tìm tài khoản hoặc gửi OTP")
    void requestReset_emailNotConfigured_rejectsBeforeLookup() {
        // Given: SMTP chưa cấu hình.
        when(emailSender.isConfigured()).thenReturn(false);

        // When: Gọi service.requestReset với dữ liệu đã chuẩn bị.
        PasswordRecoveryUnavailableException error = assertThrows(PasswordRecoveryUnavailableException.class,
            () -> service.requestReset(EMAIL, session));

        // Then: báo unavailable trước khi tìm tài khoản hoặc gửi OTP.
        assertEquals("MAIL_UNAVAILABLE", error.getErrorCode());
        verify(resetService, times(0)).findActiveAccount(anyString());
        verify(emailSender, times(0)).sendOtp(anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-03 | Email không có account Active: giữ lỗi tìm tài khoản, không gửi OTP")
    void requestReset_accountLookupFails_doesNotSendEmail() {
        // Given: Email không có account Active.
        when(emailSender.isConfigured()).thenReturn(true);
        when(resetService.findActiveAccount(EMAIL)).thenThrow(new PasswordRecoveryFlowException(
            Step.FORGOT, "email", "EMAIL_NOT_FOUND", "Account missing."));

        // When: Gọi service.requestReset với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.requestReset(EMAIL, session));

        // Then: giữ lỗi tìm tài khoản, không gửi OTP.
        assertEquals("EMAIL_NOT_FOUND", error.getErrorCode());
        verify(emailSender, times(0)).sendOtp(anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-04 | Gửi lại yêu cầu ngay: báo cooldown, không gửi email thứ hai")
    void requestReset_beforeCooldownEnds_doesNotSendAgain() {
        // Given: Gửi lại yêu cầu ngay.
        prepareRequest();
        service.requestReset(EMAIL, session);

        // When: Gọi service.requestReset với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.requestReset(EMAIL, session));

        // Then: báo cooldown, không gửi email thứ hai.
        assertEquals("RESET_EMAIL_COOLDOWN", error.getErrorCode());
        assertEquals("email", error.getField());
        assertEquals(1200L, error.getRetryAfterSeconds());
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
        PasswordResetDeliveryException error = assertThrows(PasswordResetDeliveryException.class,
            () -> service.requestReset(EMAIL, session));
        assertEquals("MAIL_DELIVERY_FAILED", error.getErrorCode());
        assertEquals(false, service.hasPendingReset(session));

        // When: Thử lại ngay sau lần gửi thất bại.
        service.requestReset(EMAIL, session);

        // Then: không tạo trạng thái OTP và cho phép thử gửi lại.
        assertEquals(true, service.hasPendingReset(session));
        verify(emailSender, times(2)).sendOtp(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-06 | Chưa có yêu cầu OTP: không cho xác minh mã")
    void verifyOtp_withoutPendingRequest_rejectsRequest() {
        // Given: Session mới chưa gửi yêu cầu quên mật khẩu.

        // When: Gọi service.verifyOtp với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.verifyOtp("123456", session));

        // Then: không cho xác minh mã.
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        verify(encoder, times(0)).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-07 | OTP đúng và còn hạn: cho phép tới bước đặt mật khẩu mới")
    void verifyOtp_correctCode_allowsPasswordReset() {
        // Given: OTP đúng và còn hạn.
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);
        when(encoder.matches("123456", OTP_HASH)).thenReturn(true);

        // When: Gọi service.verifyOtp với dữ liệu đã chuẩn bị.
        service.verifyOtp("123456", session);

        // Then: cho phép tới bước đặt mật khẩu mới.
        assertEquals(true, service.canResetPassword(session));
        assertEquals(false, service.hasPendingReset(session));
        verify(encoder, times(1)).matches("123456", OTP_HASH);
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-08 | OTP đủ 6 số nhưng sai: báo OTP_INVALID và giữ bước chờ mã")
    void verifyOtp_incorrectCode_keepsPendingRequest() {
        // Given: OTP đủ 6 số nhưng sai.
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);
        when(encoder.matches("123456", OTP_HASH)).thenReturn(false);

        // When: Gọi service.verifyOtp với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.verifyOtp("123456", session));

        // Then: báo OTP_INVALID và giữ bước chờ mã.
        assertEquals("OTP_INVALID", error.getErrorCode());
        assertEquals(true, service.hasPendingReset(session));
        assertEquals(false, service.canResetPassword(session));
    }

    @Test
    @DisplayName("AUTH-RECOVERY-09 | OTP sai định dạng: không gọi so khớp hash")
    void verifyOtp_malformedCode_rejectsBeforeHashComparison() {
        // Given: OTP sai định dạng.
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);

        // When: Gọi service.verifyOtp với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.verifyOtp("abc", session));

        // Then: không gọi so khớp hash.
        assertEquals("OTP_INVALID", error.getErrorCode());
        verify(encoder, times(0)).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-10 | Sai OTP lần thứ 5: xóa trạng thái recovery và chưa đổi mật khẩu")
    void verifyOtp_fifthWrongAttempt_clearsResetState() {
        // Given: Sai OTP lần thứ 5.
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

        // Then: xóa trạng thái recovery và chưa đổi mật khẩu.
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

        // When: Gọi service.verifyOtp với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.verifyOtp("123456", session));

        // Then: bị từ chối và xóa trạng thái, không cần sleep.
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        assertEquals(false, service.hasPendingReset(session));
        verify(encoder, times(0)).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-12 | Account đã đổi sau khi gửi OTP: vô hiệu hóa yêu cầu recovery cũ")
    void verifyOtp_accountSnapshotChanged_rejectsRequest() {
        // Given: Account đã đổi sau khi gửi OTP.
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(false);

        // When: Gọi service.verifyOtp với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.verifyOtp("123456", session));

        // Then: vô hiệu hóa yêu cầu recovery cũ.
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

        // When: Gọi service.completeReset với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.completeReset(NEW_PASSWORD, session));

        // Then: không được lưu mật khẩu mới.
        assertEquals("OTP_REQUIRED", error.getErrorCode());
        assertEquals(Step.OTP, error.getStep());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-14 | OTP đã xác minh: gọi đổi mật khẩu đúng snapshot/thời hạn và xóa trạng thái sau thành công")
    void completeReset_verifiedOtp_changesPasswordAndClearsState() {
        // Given: OTP đã xác minh.
        verifyCode();

        // When: Gọi service.completeReset với dữ liệu đã chuẩn bị.
        service.completeReset(NEW_PASSWORD, session);

        // Then: gọi đổi mật khẩu đúng snapshot/thời hạn và xóa trạng thái sau thành công.
        verify(resetService, times(1)).changePassword(account, NEW_PASSWORD, NOW.plusSeconds(900));
        assertEquals(false, service.canResetPassword(session));
        assertEquals(false, service.hasPendingReset(session));
    }

    @Test
    @DisplayName("AUTH-RECOVERY-15 | Gửi lại thao tác lưu sau reset thành công: không đổi mật khẩu lần thứ hai")
    void completeReset_alreadyCompleted_rejectsReplay() {
        // Given: Gửi lại thao tác lưu sau reset thành công.
        verifyCode();
        service.completeReset(NEW_PASSWORD, session);

        // When: Gọi service.completeReset với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.completeReset(NEW_PASSWORD, session));

        // Then: không đổi mật khẩu lần thứ hai.
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        verify(resetService, times(1)).changePassword(account, NEW_PASSWORD, NOW.plusSeconds(900));
    }

    @Test
    @DisplayName("AUTH-RECOVERY-16 | Lưu mật khẩu thất bại: giữ trạng thái OTP đã xác minh để người dùng sửa và thử lại")
    void completeReset_passwordSaveFails_keepsVerifiedState() {
        // Given: Lưu mật khẩu thất bại.
        verifyCode();
        doThrow(new PasswordRecoveryFlowException(Step.PASSWORD, "password", "PASSWORD_INVALID", "Invalid password."))
            .when(resetService).changePassword(account, NEW_PASSWORD, NOW.plusSeconds(900));

        // When: Gọi service.completeReset với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.completeReset(NEW_PASSWORD, session));

        // Then: giữ trạng thái OTP đã xác minh để người dùng sửa và thử lại.
        assertEquals("PASSWORD_INVALID", error.getErrorCode());
        assertEquals(true, service.canResetPassword(session));
    }

    @Test
    @DisplayName("AUTH-RECOVERY-17 | Session chưa có yêu cầu recovery: thời gian chờ gửi lại bằng 0")
    void getResendRemainingSeconds_withoutRequest_returnsZero() {
        // Given: Session chưa có yêu cầu recovery.
        // When: Gọi service.getResendRemainingSeconds với dữ liệu đã chuẩn bị.
        long result = service.getResendRemainingSeconds(session);
        // Then: thời gian chờ gửi lại bằng 0.
        assertEquals(0L, result);
    }

    @Test
    @DisplayName("AUTH-RECOVERY-18 | OTP đã xác minh: xác minh lại không so khớp mã lần nữa")
    void verifyOtp_alreadyVerified_doesNotCompareCodeAgain() {
        // Given: OTP đã xác minh.
        verifyCode();

        // When: Gọi service.verifyOtp với dữ liệu đã chuẩn bị.
        service.verifyOtp("123456", session);

        // Then: xác minh lại không so khớp mã lần nữa.
        assertEquals(true, service.canResetPassword(session));
        verify(encoder, times(1)).matches("123456", OTP_HASH);
    }

    @Test
    @DisplayName("AUTH-RECOVERY-19 | OTP null: báo OTP_INVALID, không so hash hoặc đổi mật khẩu")
    void verifyOtp_nullCode_rejectsBeforeHashComparison() {
        // Given: OTP null.
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);

        // When: Gọi service.verifyOtp với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.verifyOtp(null, session));

        // Then: báo OTP_INVALID, không so hash hoặc đổi mật khẩu.
        assertEquals("OTP_INVALID", error.getErrorCode());
        assertEquals(Step.OTP, error.getStep());
        assertEquals(false, service.canResetPassword(session));
        verify(encoder, times(0)).matches(any(), anyString());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-20 | OTP blank: giữ trạng thái chờ mã, không so hash")
    void verifyOtp_blankCode_keepsPendingRequest() {
        // Given: OTP blank.
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);

        // When: Gọi service.verifyOtp với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.verifyOtp("   ", session));

        // Then: giữ trạng thái chờ mã, không so hash.
        assertEquals("OTP_INVALID", error.getErrorCode());
        assertEquals(true, service.hasPendingReset(session));
        assertEquals(false, service.canResetPassword(session));
        verify(encoder, times(0)).matches(anyString(), anyString());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-21 | Còn 1199,5 giây cooldown: làm tròn lên 1200 giây để UI chưa cho gửi sớm")
    void getResendRemainingSeconds_fractionalSecond_roundsUp() {
        // Given: Còn 1199,5 giây cooldown.
        requestCode();
        when(clock.instant()).thenReturn(NOW.plusMillis(500));

        // When: Gọi service.getResendRemainingSeconds với dữ liệu đã chuẩn bị.
        long result = service.getResendRemainingSeconds(session);

        // Then: làm tròn lên 1200 giây để UI chưa cho gửi sớm.
        assertEquals(1200L, result);
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-22 | Đúng hạn cooldown 20 phút: thời gian chờ bằng 0")
    void getResendRemainingSeconds_atCooldownBoundary_returnsZero() {
        // Given: Đúng hạn cooldown 20 phút.
        requestCode();
        when(clock.instant()).thenReturn(NOW.plusSeconds(1200));

        // When: Gọi service.getResendRemainingSeconds với dữ liệu đã chuẩn bị.
        long result = service.getResendRemainingSeconds(session);

        // Then: thời gian chờ bằng 0.
        assertEquals(0L, result);
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-23 | Gửi lại đúng phút 20: gửi email thứ hai và tạo OTP mới còn hạn")
    void requestReset_atCooldownBoundary_sendsNewCode() {
        // Given: Gửi lại đúng phút 20.
        requestCode();
        when(clock.instant()).thenReturn(NOW.plusSeconds(1200));
        when(resetService.isCurrent(account)).thenReturn(true);

        // When: Gọi service.requestReset với dữ liệu đã chuẩn bị.
        service.requestReset(EMAIL, session);

        // Then: gửi email thứ hai và tạo OTP mới còn hạn.
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

        // When: Gọi service.requestReset với dữ liệu đã chuẩn bị.
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
        // Given: Trước hạn OTP 1 nano giây.
        requestCode();
        when(clock.instant()).thenReturn(NOW.plusSeconds(900).minusNanos(1));
        when(resetService.isCurrent(account)).thenReturn(true);
        when(encoder.matches("123456", OTP_HASH)).thenReturn(true);

        // When: Gọi service.verifyOtp với dữ liệu đã chuẩn bị.
        service.verifyOtp("123456", session);

        // Then: vẫn cho xác minh mã đúng.
        assertEquals(true, service.canResetPassword(session));
        verify(encoder, times(1)).matches("123456", OTP_HASH);
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-26 | Session sai kiểu: countdown bằng 0, không cho xác minh hoặc đổi mật khẩu")
    void verifyOtp_invalidSessionState_rejectsBeforeSideEffects() {
        // Given: Session sai kiểu.
        session.setAttribute(RESET_STATE, "invalid-state");
        assertEquals(0L, service.getResendRemainingSeconds(session));

        // When: Gọi service.verifyOtp với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.verifyOtp("123456", session));

        // Then: countdown bằng 0, không cho xác minh hoặc đổi mật khẩu.
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        assertEquals(Step.FORGOT, error.getStep());
        assertEquals(false, service.canResetPassword(session));
        verify(encoder, times(0)).matches(anyString(), anyString());
        verify(emailSender, times(0)).sendOtp(anyString(), anyString());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-27 | Instance service mới đọc session còn hạn: countdown bằng 0 khi chưa có cooldown trong instance")
    void getResendRemainingSeconds_newServiceWithExistingSession_returnsZero() {
        // Given: Session tồn tại nhưng map cooldown của instance mới chưa có dữ liệu.
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);
        PasswordRecoveryService newInstance = new PasswordRecoveryService(
            resetService, emailSender, encoder, Clock.fixed(NOW, ZoneOffset.UTC));

        // When: Gọi getResendRemainingSeconds_newServiceWithExistingSession_returnsZero với dữ liệu đã chuẩn bị.
        long result = newInstance.getResendRemainingSeconds(session);

        // Then: countdown bằng 0 khi chưa có cooldown trong instance.
        assertEquals(0L, result);
        assertEquals(true, newInstance.hasPendingReset(session));
        assertEquals(EMAIL, newInstance.getPendingResetEmail(session));
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-28 | Xin mã trước hạn cooldown 1 giây: retryAfter=1 và không gửi thêm email")
    void requestReset_oneSecondBeforeCooldown_rejectsDuplicateSend() {
        // Given: Xin mã trước hạn cooldown 1 giây.
        requestCode();
        when(clock.instant()).thenReturn(NOW.plusSeconds(1199));

        // When: Gọi service.requestReset với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.requestReset(EMAIL, session));

        // Then: retryAfter=1 và không gửi thêm email.
        assertEquals("RESET_EMAIL_COOLDOWN", error.getErrorCode());
        assertEquals(Step.FORGOT, error.getStep());
        assertEquals("email", error.getField());
        assertEquals(1L, error.getRetryAfterSeconds());
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-29 | Xin mã sau cooldown 1 giây: tạo pending mới và bắt đầu lại 1200 giây chờ")
    void requestReset_oneSecondAfterCooldown_createsNewPendingRequest() {
        // Given: Xin mã sau cooldown 1 giây.
        requestCode();
        when(clock.instant()).thenReturn(NOW.plusSeconds(1201));
        when(resetService.isCurrent(account)).thenReturn(true);

        // When: Gọi service.requestReset với dữ liệu đã chuẩn bị.
        service.requestReset(EMAIL, session);

        // Then: tạo pending mới và bắt đầu lại 1200 giây chờ.
        assertEquals(true, service.hasPendingReset(session));
        assertEquals(false, service.canResetPassword(session));
        assertEquals(1200L, service.getResendRemainingSeconds(session));
        verify(emailSender, times(2)).sendOtp(eq(EMAIL), anyString());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-30 | Session khác xin mã cùng account: dùng chung cooldown, không tạo pending ở session thứ hai")
    void requestReset_sameAccountInAnotherSession_rejectsDuplicateSend() {
        // Given: Session khác xin mã cùng account.
        requestCode();
        when(resetService.isCurrent(account)).thenReturn(true);
        MockHttpSession anotherSession = new MockHttpSession();

        // When: Gọi service.requestReset với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.requestReset(EMAIL, anotherSession));

        // Then: dùng chung cooldown, không tạo pending ở session thứ hai.
        assertEquals("RESET_EMAIL_COOLDOWN", error.getErrorCode());
        assertEquals(1200L, error.getRetryAfterSeconds());
        assertEquals(true, service.hasPendingReset(session));
        assertEquals(false, service.hasPendingReset(anotherSession));
        assertEquals(null, anotherSession.getAttribute(RESET_STATE));
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-31 | Đổi email yêu cầu nhưng gửi mail lỗi: giữ snapshot và OTP của yêu cầu trước còn hạn")
    void requestReset_otherAccountDeliveryFails_preservesPreviousOtp() {
        // Given: Yêu cầu A còn hạn; yêu cầu B chưa có cooldown nên được thử gửi.
        requestCode();
        AccountSnapshotResponse anotherAccount = new AccountSnapshotResponse(8, "candidate8@example.test", "hash-8");
        when(resetService.findActiveAccount(anotherAccount.email())).thenReturn(anotherAccount);
        when(clock.instant()).thenReturn(NOW.plusSeconds(1));
        when(resetService.isCurrent(account)).thenReturn(true);
        when(encoder.encode(anyString())).thenReturn("failed-new-otp-hash");
        doThrow(new PasswordResetDeliveryException()).when(emailSender)
            .sendOtp(eq(anotherAccount.email()), anyString());
        Object previousState = session.getAttribute(RESET_STATE);

        // When: Gọi service.requestReset với dữ liệu đã chuẩn bị.
        PasswordResetDeliveryException error = assertThrows(PasswordResetDeliveryException.class,
            () -> service.requestReset(anotherAccount.email(), session));

        // Then: Mã/snapshot mới chưa gửi thành công không được ghi đè yêu cầu A.
        assertEquals("MAIL_DELIVERY_FAILED", error.getErrorCode());
        assertEquals(previousState, session.getAttribute(RESET_STATE));
        assertEquals(EMAIL, service.getPendingResetEmail(session));
        when(encoder.matches("123456", OTP_HASH)).thenReturn(true);
        service.verifyOtp("123456", session);
        assertEquals(true, service.canResetPassword(session));
        verify(encoder, times(1)).matches("123456", OTP_HASH);
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
        verify(emailSender, times(1)).sendOtp(eq(anotherAccount.email()), anyString());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-32 | Xác minh sau hạn OTP 1 giây: xóa state và không so hash")
    void verifyOtp_oneSecondAfterExpiry_clearsState() {
        // Given: Xác minh sau hạn OTP 1 giây.
        requestCode();
        when(clock.instant()).thenReturn(NOW.plusSeconds(901));

        // When: Gọi service.verifyOtp với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.verifyOtp("123456", session));

        // Then: xóa state và không so hash.
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        assertEquals(Step.FORGOT, error.getStep());
        assertEquals(null, session.getAttribute(RESET_STATE));
        verify(encoder, times(0)).matches(anyString(), anyString());
        verify(resetService, times(0)).isCurrent(any());
        verify(resetService, times(0)).changePassword(any(), anyString(), any());
    }

    @Test
    @DisplayName("AUTH-RECOVERY-33 | Snapshot đổi sau khi đã xác minh OTP: chặn lưu mật khẩu và xóa state")
    void completeReset_snapshotChangedAfterVerification_doesNotChangePassword() {
        // Given: Snapshot đổi sau khi đã xác minh OTP.
        verifyCode();
        when(resetService.isCurrent(account)).thenReturn(false);

        // When: Gọi service.completeReset với dữ liệu đã chuẩn bị.
        PasswordRecoveryFlowException error = assertThrows(PasswordRecoveryFlowException.class,
            () -> service.completeReset(NEW_PASSWORD, session));

        // Then: chặn lưu mật khẩu và xóa state.
        assertEquals("RESET_REQUEST_INVALID", error.getErrorCode());
        assertEquals(Step.FORGOT, error.getStep());
        assertEquals(null, session.getAttribute(RESET_STATE));
        assertEquals(false, service.canResetPassword(session));
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
