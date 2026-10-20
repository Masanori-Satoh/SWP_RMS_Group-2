package com.group2.rms.auth.service;

import com.group2.rms.auth.dto.RegisterAccountRequest;
import com.group2.rms.auth.exception.RegistrationFlowException;
import com.group2.rms.auth.exception.RegistrationFlowException.Step;
import com.group2.rms.user.exception.AccountFieldException;
import com.group2.rms.user.service.AccountManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class RegistrationVerificationServiceTests {

    private static final String EMAIL = "candidate7@example.test";
    private static final String PASSWORD = "TestPass123!";
    private static final String HASH = "mock-encoded-value";
    private static final String NEW_OTP_HASH = "new-otp-hash";
    private static final Instant NOW = Instant.parse("2026-10-19T02:00:00Z");
    private static final String REGISTER_STATE = RegistrationVerificationService.class.getName() + ".REGISTER_STATE";

    @Mock
    private AccountManagementService accounts;

    @Mock
    private RegistrationEmailSender emailSender;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private RegistrationVerificationService service;

    private RegisterAccountRequest form;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        form = new RegisterAccountRequest();
        form.setFullName("  Candidate Seven  ");
        form.setUsername("  candidate7  ");
        form.setEmail("  " + EMAIL + "  ");
        form.setPassword(PASSWORD);
        form.setConfirmPassword(PASSWORD);
    }

    @Test
    @DisplayName("AUTH-REGISTER-01 | Đăng ký hợp lệ: trim thông tin, mã hóa password và gửi OTP, chưa tạo account")
    void requestRegistration_validForm_sendsCodeWithoutCreatingAccount() {
        // Given: Dependency mã hóa được mock; không gọi BCrypt hoặc SMTP thật.
        when(encoder.encode(anyString())).thenReturn(HASH);

        // When: Gọi service.requestRegistration với dữ liệu đã chuẩn bị.
        service.requestRegistration(form, session);

        // Then: trim thông tin, mã hóa password và gửi OTP, chưa tạo account.
        assertEquals(EMAIL, service.getPendingRegistrationEmail(session));
        verify(accounts, times(1)).assertLoginIdentifiersAvailable("candidate7", EMAIL);
        verify(encoder, times(1)).encode(PASSWORD);
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
        verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-02 | Chưa yêu cầu đăng ký: đọc email OTP phải báo REGISTER_REQUEST_INVALID")
    void getPendingRegistrationEmail_missingSessionState_rejectsRequest() {
        // Given: Session mới, chưa gửi form đăng ký.

        // When: Gọi service.getPendingRegistrationEmail với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.getPendingRegistrationEmail(session));

        // Then: đọc email OTP phải báo REGISTER_REQUEST_INVALID.
        assertEquals("REGISTER_REQUEST_INVALID", error.getErrorCode());
        assertEquals(Step.REGISTER, error.getStep());
    }

    @Test
    @DisplayName("AUTH-REGISTER-03 | Username đã dùng: trả lỗi field username, không mã hóa hoặc gửi OTP")
    void requestRegistration_duplicateUsername_doesNotSendCode() {
        // Given: Username đã dùng.
        doThrow(new AccountFieldException("username", "Username already used."))
            .when(accounts).assertLoginIdentifiersAvailable("candidate7", EMAIL);

        // When: Gọi service.requestRegistration với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.requestRegistration(form, session));

        // Then: trả lỗi field username, không mã hóa hoặc gửi OTP.
        assertEquals("REGISTER_FIELD_INVALID", error.getErrorCode());
        assertEquals("username", error.getField());
        assertNotNull(error.getDetails());
        assertEquals("candidate7", error.getDetails().username());
        verify(encoder, times(0)).encode(anyString());
        verify(emailSender, times(0)).sendOtp(anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-04 | OTP đủ 6 số nhưng sai: báo REGISTER_OTP_INVALID và chưa tạo account")
    void verifyAndRegister_incorrectCode_doesNotCreateAccount() {
        // Given: Đã gửi OTP; mock xác nhận mã người dùng nhập là sai.
        requestCode();
        when(encoder.matches("123456", HASH)).thenReturn(false);

        // When: Gọi service.verifyAndRegister với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.verifyAndRegister("123456", session));

        // Then: báo REGISTER_OTP_INVALID và chưa tạo account.
        assertEquals("REGISTER_OTP_INVALID", error.getErrorCode());
        assertEquals(Step.OTP, error.getStep());
        assertEquals(EMAIL, service.getPendingRegistrationEmail(session));
        verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-05 | OTP sai định dạng: từ chối trước khi so khớp hash")
    void verifyAndRegister_malformedCode_doesNotCompareHash() {
        // Given: OTP sai định dạng.
        requestCode();

        // When: Gọi service.verifyAndRegister với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.verifyAndRegister("abc", session));

        // Then: từ chối trước khi so khớp hash.
        assertEquals("REGISTER_OTP_INVALID", error.getErrorCode());
        verify(encoder, times(0)).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-06 | Nhập sai OTP lần thứ 5: xóa yêu cầu đăng ký và không tạo account")
    void verifyAndRegister_fifthWrongAttempt_clearsPendingRegistration() {
        // Given: Có yêu cầu đăng ký; cùng một mã sai được nhập liên tiếp.
        requestCode();
        when(encoder.matches("123456", HASH)).thenReturn(false);
        for (int attempt = 1; attempt <= 4; attempt++) {
            RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.verifyAndRegister("123456", session));
            assertEquals("REGISTER_OTP_INVALID", error.getErrorCode());
        }

        // When: Lần sai thứ 5.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.verifyAndRegister("123456", session));

        // Then: xóa yêu cầu đăng ký và không tạo account.
        assertEquals("REGISTER_OTP_ATTEMPTS_EXCEEDED", error.getErrorCode());
        assertEquals(Step.REGISTER, error.getStep());
        assertThrows(RegistrationFlowException.class, () -> service.getPendingRegistrationEmail(session));
        verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-07 | OTP đúng: tạo Candidate một lần bằng thông tin đã trim và hash đã chuẩn bị")
    void verifyAndRegister_correctCode_createsCandidateOnce() {
        // Given: OTP đúng.
        requestCode();
        when(encoder.matches("123456", HASH)).thenReturn(true);
        when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH)).thenReturn(7);

        // When: Gọi service.verifyAndRegister với dữ liệu đã chuẩn bị.
        int result = service.verifyAndRegister("123456", session);

        // Then: tạo Candidate một lần bằng thông tin đã trim và hash đã chuẩn bị.
        assertEquals(7, result);
        verify(accounts, times(1)).createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH);
        assertThrows(RegistrationFlowException.class, () -> service.getPendingRegistrationEmail(session));
    }

    @Test
    @DisplayName("AUTH-REGISTER-08 | Xác minh lại sau khi đăng ký thành công: không tạo thêm tài khoản")
    void verifyAndRegister_alreadyCompleted_doesNotCreateAccountAgain() {
        // Given: Lần xác minh đầu đã tạo tài khoản thành công.
        requestCode();
        when(encoder.matches("123456", HASH)).thenReturn(true);
        when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH)).thenReturn(7);
        service.verifyAndRegister("123456", session);

        // When: Gửi lại mã bằng session đã hoàn tất đăng ký.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.verifyAndRegister("123456", session));

        // Then: không tạo thêm tài khoản.
        assertEquals("REGISTER_REQUEST_INVALID", error.getErrorCode());
        verify(accounts, times(1)).createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH);
    }

    @Test
    @DisplayName("AUTH-REGISTER-09 | Bấm gửi lại OTP ngay: báo cooldown và chỉ gửi một email")
    void resendOtp_beforeCooldownEnds_doesNotSendSecondCode() {
        // Given: Bấm gửi lại OTP ngay.
        requestCode();

        // When: Gọi service.resendOtp với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.resendOtp(session));

        // Then: báo cooldown và chỉ gửi một email.
        assertEquals("REGISTER_EMAIL_COOLDOWN", error.getErrorCode());
        assertEquals(Step.OTP, error.getStep());
        assertEquals(true, error.getRetryAfterSeconds() > 0);
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-10 | Gửi lại form cùng email ở session khác: vẫn áp dụng cooldown theo email")
    void requestRegistration_sameEmailInAnotherSession_rejectsSecondSend() {
        // Given: Gửi lại form cùng email ở session khác.
        requestCode();
        MockHttpSession anotherSession = new MockHttpSession();

        // When: Gọi service.requestRegistration với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.requestRegistration(form, anotherSession));

        // Then: vẫn áp dụng cooldown theo email.
        assertEquals("REGISTER_EMAIL_COOLDOWN", error.getErrorCode());
        assertEquals("email", error.getField());
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-11 | Username bị lấy trước lúc xác minh: trả lỗi field và xóa pending registration")
    void verifyAndRegister_identifierTakenBeforeCommit_clearsPendingRegistration() {
        // Given: Username bị lấy trước lúc xác minh.
        requestCode();
        when(encoder.matches("123456", HASH)).thenReturn(true);
        when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH))
            .thenThrow(new AccountFieldException("username", "Username already used."));

        // When: Gọi service.verifyAndRegister với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.verifyAndRegister("123456", session));

        // Then: trả lỗi field và xóa pending registration.
        assertEquals("REGISTER_FIELD_INVALID", error.getErrorCode());
        assertEquals("username", error.getField());
        assertThrows(RegistrationFlowException.class, () -> service.getPendingRegistrationEmail(session));
    }

    @Test
    @DisplayName("AUTH-REGISTER-12 | DB báo trùng UQ_User_Email: chuyển thành lỗi đăng ký trùng, không lộ lỗi DB")
    void verifyAndRegister_uniqueEmailConstraint_returnsConflictError() {
        // Given: DB báo trùng UQ_User_Email.
        requestCode();
        when(encoder.matches("123456", HASH)).thenReturn(true);
        when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH))
            .thenThrow(new DataIntegrityViolationException("UQ_User_Email"));

        // When: Gọi service.verifyAndRegister với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.verifyAndRegister("123456", session));

        // Then: chuyển thành lỗi đăng ký trùng, không lộ lỗi DB.
        assertEquals("REGISTER_ACCOUNT_CONFLICT", error.getErrorCode());
        assertThrows(RegistrationFlowException.class, () -> service.getPendingRegistrationEmail(session));
    }

    @Test
    @DisplayName("AUTH-REGISTER-13 | Lỗi DB không phải trùng identifier: không đổi nhầm thành lỗi username/email")
    void verifyAndRegister_unrelatedDatabaseFailure_preservesOriginalError() {
        // Given: Lỗi DB không phải trùng identifier.
        requestCode();
        when(encoder.matches("123456", HASH)).thenReturn(true);
        when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH))
            .thenThrow(new DataIntegrityViolationException("FK_Candidate_UserId"));

        // When: Gọi service.verifyAndRegister với dữ liệu đã chuẩn bị.
        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
            () -> service.verifyAndRegister("123456", session));

        // Then: Người dùng chưa bị mất pending registration chỉ vì lỗi lưu tạm thời.
        assertEquals("FK_Candidate_UserId", error.getMessage());
        assertEquals(EMAIL, service.getPendingRegistrationEmail(session));
    }

    @Test
    @DisplayName("AUTH-REGISTER-14 | Gửi OTP thất bại: không tạo pending registration hoặc tạo account")
    void requestRegistration_emailDeliveryFails_doesNotCreatePendingRegistration() {
        // Given: doThrow dùng cho sendOtp vì đây là method void.
        when(encoder.encode(anyString())).thenReturn(HASH);
        doThrow(new RegistrationFlowException(Step.REGISTER, null, "REGISTER_EMAIL_DELIVERY_FAILED", "Mail failed."))
            .when(emailSender).sendOtp(eq(EMAIL), anyString());

        // When: Gọi service.requestRegistration với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.requestRegistration(form, session));

        // Then: không tạo pending registration hoặc tạo account.
        assertEquals("REGISTER_EMAIL_DELIVERY_FAILED", error.getErrorCode());
        assertThrows(RegistrationFlowException.class, () -> service.getPendingRegistrationEmail(session));
        verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-15 | Chưa có pending registration: không cho gọi gửi lại OTP")
    void resendOtp_withoutRegistration_doesNotSendEmail() {
        // Given: Session mới chưa từng đăng ký.

        // When: Gọi service.resendOtp với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.resendOtp(session));

        // Then: không cho gọi gửi lại OTP.
        assertEquals("REGISTER_REQUEST_INVALID", error.getErrorCode());
        verify(emailSender, times(0)).sendOtp(anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-16 | OTP null: báo REGISTER_OTP_INVALID, không so hash hoặc tạo account")
    void verifyAndRegister_nullCode_rejectsBeforeHashComparison() {
        // Given: OTP null.
        requestCode();

        // When: Gọi service.verifyAndRegister với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.verifyAndRegister(null, session));

        // Then: báo REGISTER_OTP_INVALID, không so hash hoặc tạo account.
        assertEquals("REGISTER_OTP_INVALID", error.getErrorCode());
        assertEquals(Step.OTP, error.getStep());
        assertEquals(EMAIL, service.getPendingRegistrationEmail(session));
        verify(encoder, times(0)).matches(org.mockito.ArgumentMatchers.isNull(), anyString());
        verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-17 | OTP blank: giữ yêu cầu chờ OTP và không tạo account")
    void verifyAndRegister_blankCode_doesNotCreateAccount() {
        // Given: OTP blank.
        requestCode();

        // When: Gọi service.verifyAndRegister với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.verifyAndRegister("   ", session));

        // Then: giữ yêu cầu chờ OTP và không tạo account.
        assertEquals("REGISTER_OTP_INVALID", error.getErrorCode());
        assertEquals(EMAIL, service.getPendingRegistrationEmail(session));
        verify(encoder, times(0)).matches(anyString(), anyString());
        verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-18 | Vừa gửi OTP: UI nhận thời gian cooldown lớn hơn 0 và không quá 300 giây")
    void getResendRemainingSeconds_pendingRegistration_returnsPositiveCooldown() {
        // Given: Service dùng Instant.now trực tiếp; kiểm tra khoảng thay vì giá trị phụ thuộc mili giây chạy test.
        requestCode();

        // When: Gọi service.getResendRemainingSeconds với dữ liệu đã chuẩn bị.
        long result = service.getResendRemainingSeconds(session);

        // Then: UI nhận thời gian cooldown lớn hơn 0 và không quá 300 giây.
        assertEquals(true, result > 0 && result <= 300);
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
        verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-19 | Hibernate báo UQ_User_Username: chuyển thành conflict và xóa yêu cầu")
    void verifyAndRegister_hibernateUsernameConstraint_returnsConflictError() {
        // Given: Tên constraint do Hibernate cung cấp, message không chứa tên username/email.
        requestCode();
        when(encoder.matches("123456", HASH)).thenReturn(true);
        var constraint = new org.hibernate.exception.ConstraintViolationException(
            "constraint violation", new java.sql.SQLException("duplicate"), "UQ_User_Username");
        when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH))
            .thenThrow(new DataIntegrityViolationException("could not execute statement", constraint));

        // When: Gọi service.verifyAndRegister với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.verifyAndRegister("123456", session));

        // Then: chuyển thành conflict và xóa yêu cầu.
        assertEquals("REGISTER_ACCOUNT_CONFLICT", error.getErrorCode());
        assertEquals(Step.REGISTER, error.getStep());
        assertThrows(RegistrationFlowException.class, () -> service.getPendingRegistrationEmail(session));
        verify(accounts, times(1)).createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH);
    }

    @Test
    @DisplayName("AUTH-REGISTER-20 | UQ_User_Email nằm trong cause: vẫn nhận diện conflict và xóa pending")
    void verifyAndRegister_nestedEmailConstraint_returnsConflictError() {
        // Given: UQ_User_Email nằm trong cause.
        requestCode();
        when(encoder.matches("123456", HASH)).thenReturn(true);
        when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH))
            .thenThrow(new DataIntegrityViolationException("could not execute statement",
            new java.sql.SQLException("UQ_User_Email")));

        // When: Gọi service.verifyAndRegister với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.verifyAndRegister("123456", session));

        // Then: vẫn nhận diện conflict và xóa pending.
        assertEquals("REGISTER_ACCOUNT_CONFLICT", error.getErrorCode());
        assertThrows(RegistrationFlowException.class, () -> service.getPendingRegistrationEmail(session));
        verify(accounts, times(1)).createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH);
    }

    @Test
    @DisplayName("AUTH-REGISTER-21 | Hibernate không có tên constraint: giữ lỗi DB, không gán nhầm lỗi trùng account")
    void verifyAndRegister_unnamedConstraint_preservesDatabaseError() {
        // Given: Hibernate không có tên constraint.
        requestCode();
        when(encoder.matches("123456", HASH)).thenReturn(true);
        var constraint = new org.hibernate.exception.ConstraintViolationException(
            "constraint violation", new java.sql.SQLException("foreign key violation"), null);
        DataIntegrityViolationException databaseError = new DataIntegrityViolationException("write failed", constraint);
        when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH))
            .thenThrow(databaseError);

        // When: Gọi service.verifyAndRegister với dữ liệu đã chuẩn bị.
        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
            () -> service.verifyAndRegister("123456", session));

        // Then: giữ lỗi DB, không gán nhầm lỗi trùng account.
        assertEquals(databaseError, error);
        assertEquals(EMAIL, service.getPendingRegistrationEmail(session));
        verify(accounts, times(1)).createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH);
    }

    @Test
    @DisplayName("AUTH-REGISTER-22 | Lần gửi mail đầu lỗi: cho gửi lại ngay, không để cooldown giả chặn đăng ký")
    void requestRegistration_deliveryFailure_allowsImmediateRetry() {
        // Given: Lần gửi mail đầu lỗi.
        when(encoder.encode(anyString())).thenReturn(HASH);
        doThrow(new RegistrationFlowException(Step.REGISTER, null, "REGISTER_EMAIL_DELIVERY_FAILED", "Mail failed."))
            .doNothing().when(emailSender).sendOtp(eq(EMAIL), anyString());
        assertThrows(RegistrationFlowException.class, () -> service.requestRegistration(form, session));

        // When: Gọi service.requestRegistration với dữ liệu đã chuẩn bị.
        service.requestRegistration(form, session);

        // Then: cho gửi lại ngay, không để cooldown giả chặn đăng ký.
        assertEquals(EMAIL, service.getPendingRegistrationEmail(session));
        verify(emailSender, times(2)).sendOtp(eq(EMAIL), anyString());
        verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-23 | Resend đúng giây 300: thay hash OTP, gia hạn OTP và giữ hash password đã chuẩn bị")
    void resendOtp_atCooldownBoundary_replacesOtpAndExtendsExpiry() {
        // Given: Resend đúng giây 300.
        try (MockedStatic<Instant> time = fixedTime()) {
            requestCode();
            setTime(time, NOW.plusSeconds(300));
            when(encoder.encode(anyString())).thenReturn(NEW_OTP_HASH);

            // When: Gọi service.resendOtp với dữ liệu đã chuẩn bị.
            service.resendOtp(session);

            // Then: Mã mới còn hạn tại phút 15, dù mã ban đầu đã hết hạn.
            assertEquals(300L, service.getResendRemainingSeconds(session));
            verify(emailSender, times(2)).sendOtp(eq(EMAIL), anyString());
            verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
            setTime(time, NOW.plusSeconds(900));
            when(encoder.matches("123456", NEW_OTP_HASH)).thenReturn(true);
            when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH)).thenReturn(7);
            assertEquals(7, service.verifyAndRegister("123456", session));
            verify(encoder, times(1)).matches("123456", NEW_OTP_HASH);
            verify(accounts, times(1)).createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH);
            assertEquals(null, session.getAttribute(REGISTER_STATE));
        }
    }

    @Test
    @DisplayName("AUTH-REGISTER-24 | Resend trước hạn 1 giây: retryAfter=1, không sinh hash mới hoặc gửi mail thứ hai")
    void resendOtp_oneSecondBeforeCooldown_rejectsWithoutChangingState() {
        // Given: Resend trước hạn 1 giây.
        try (MockedStatic<Instant> time = fixedTime()) {
            requestCode();
            Object pending = session.getAttribute(REGISTER_STATE);
            setTime(time, NOW.plusSeconds(299));

            // When: Gọi service.resendOtp với dữ liệu đã chuẩn bị.
            RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.resendOtp(session));

            // Then: retryAfter=1, không sinh hash mới hoặc gửi mail thứ hai.
            assertEquals("REGISTER_EMAIL_COOLDOWN", error.getErrorCode());
            assertEquals(Step.OTP, error.getStep());
            assertEquals(null, error.getField());
            assertEquals(1L, error.getRetryAfterSeconds());
            assertEquals(pending, session.getAttribute(REGISTER_STATE));
            verify(encoder, times(2)).encode(anyString());
            verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
            verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
        }
    }

    @Test
    @DisplayName("AUTH-REGISTER-25 | Resend sau cooldown bị lỗi mail: giữ pending và hash OTP cũ để tiếp tục xác minh")
    void resendOtp_deliveryFailure_preservesPreviousOtp() {
        // Given: Resend sau cooldown bị lỗi mail.
        try (MockedStatic<Instant> time = fixedTime()) {
            requestCode();
            Object pending = session.getAttribute(REGISTER_STATE);
            setTime(time, NOW.plusSeconds(301));
            when(encoder.encode(anyString())).thenReturn(NEW_OTP_HASH);
            doThrow(new RegistrationFlowException(Step.REGISTER, null, "REGISTER_EMAIL_DELIVERY_FAILED", "Mail failed."))
                .when(emailSender).sendOtp(eq(EMAIL), anyString());

            // When: Gọi service.resendOtp với dữ liệu đã chuẩn bị.
            RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.resendOtp(session));

            // Then: Lỗi phải giữ context OTP và thông tin form an toàn.
            assertEquals("REGISTER_EMAIL_DELIVERY_FAILED", error.getErrorCode());
            assertEquals(Step.OTP, error.getStep());
            assertEquals(EMAIL, error.getDetails().email());
            assertEquals(pending, session.getAttribute(REGISTER_STATE));
            assertEquals(0L, service.getResendRemainingSeconds(session));
            verify(emailSender, times(2)).sendOtp(eq(EMAIL), anyString());
            verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
            when(encoder.matches("123456", HASH)).thenReturn(true);
            when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH)).thenReturn(7);
            assertEquals(7, service.verifyAndRegister("123456", session));
            verify(encoder, times(1)).matches("123456", HASH);
        }
    }

    @Test
    @DisplayName("AUTH-REGISTER-26 | Resend lỗi mail rồi thử lại ngay: không bị cooldown giả, dùng hash của lần gửi thành công")
    void resendOtp_deliveryFailure_allowsImmediateRetry() {
        // Given: Resend lỗi mail rồi thử lại ngay.
        try (MockedStatic<Instant> time = fixedTime()) {
            requestCode();
            setTime(time, NOW.plusSeconds(301));
            when(encoder.encode(anyString())).thenReturn(NEW_OTP_HASH);
            doThrow(new RegistrationFlowException(Step.REGISTER, null, "REGISTER_EMAIL_DELIVERY_FAILED", "Mail failed."))
                .doNothing().when(emailSender).sendOtp(eq(EMAIL), anyString());
            RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.resendOtp(session));
            assertEquals("REGISTER_EMAIL_DELIVERY_FAILED", error.getErrorCode());

            // When: Gọi service.resendOtp với dữ liệu đã chuẩn bị.
            service.resendOtp(session);

            // Then: không bị cooldown giả, dùng hash của lần gửi thành công.
            assertEquals(300L, service.getResendRemainingSeconds(session));
            assertEquals(EMAIL, service.getPendingRegistrationEmail(session));
            verify(emailSender, times(3)).sendOtp(eq(EMAIL), anyString());
            when(encoder.matches("123456", NEW_OTP_HASH)).thenReturn(true);
            when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH)).thenReturn(7);
            assertEquals(7, service.verifyAndRegister("123456", session));
            verify(accounts, times(1)).createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH);
        }
    }

    @Test
    @DisplayName("AUTH-REGISTER-27 | Session chứa pending sai kiểu: báo request invalid, không mã hóa/gửi mail/tạo account")
    void resendOtp_invalidSessionState_rejectsBeforeSideEffects() {
        // Given: Mô phỏng attribute không phải PendingRegistration qua API session công khai.
        session.setAttribute(REGISTER_STATE, "invalid-state");

        // When: Gọi service.resendOtp với dữ liệu đã chuẩn bị.
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
            () -> service.resendOtp(session));

        // Then: báo request invalid, không mã hóa/gửi mail/tạo account.
        assertEquals("REGISTER_REQUEST_INVALID", error.getErrorCode());
        assertEquals(Step.REGISTER, error.getStep());
        verify(encoder, times(0)).encode(anyString());
        verify(emailSender, times(0)).sendOtp(anyString(), anyString());
        verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-28 | Resend đúng hạn OTP 900 giây: xóa pending và không gửi mã mới")
    void resendOtp_atOtpExpiry_clearsPendingAndRejectsRequest() {
        // Given: Resend đúng hạn OTP 900 giây.
        try (MockedStatic<Instant> time = fixedTime()) {
            requestCode();
            setTime(time, NOW.plusSeconds(900));

            // When: Gọi service.resendOtp với dữ liệu đã chuẩn bị.
            RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.resendOtp(session));

            // Then: xóa pending và không gửi mã mới.
            assertEquals("REGISTER_REQUEST_INVALID", error.getErrorCode());
            assertEquals(Step.REGISTER, error.getStep());
            assertEquals(EMAIL, error.getDetails().email());
            assertEquals(null, session.getAttribute(REGISTER_STATE));
            verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
            verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
        }
    }

    @Test
    @DisplayName("AUTH-REGISTER-29 | Xác minh sau hạn OTP 1 giây: xóa pending, không so hash hoặc tạo account")
    void verifyAndRegister_oneSecondAfterExpiry_clearsPending() {
        // Given: Xác minh sau hạn OTP 1 giây.
        try (MockedStatic<Instant> time = fixedTime()) {
            requestCode();
            setTime(time, NOW.plusSeconds(901));

            // When: Gọi service.verifyAndRegister với dữ liệu đã chuẩn bị.
            RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.verifyAndRegister("123456", session));

            // Then: xóa pending, không so hash hoặc tạo account.
            assertEquals("REGISTER_REQUEST_INVALID", error.getErrorCode());
            assertEquals(Step.REGISTER, error.getStep());
            assertEquals(null, session.getAttribute(REGISTER_STATE));
            verify(encoder, times(0)).matches(anyString(), anyString());
            verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
        }
    }

    @Test
    @DisplayName("AUTH-REGISTER-30 | Xác minh trước hạn OTP 1 giây: chấp nhận mã đúng, tạo account một lần")
    void verifyAndRegister_oneSecondBeforeExpiry_createsAccount() {
        // Given: Xác minh trước hạn OTP 1 giây.
        try (MockedStatic<Instant> time = fixedTime()) {
            requestCode();
            setTime(time, NOW.plusSeconds(899));
            when(encoder.matches("123456", HASH)).thenReturn(true);
            when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH)).thenReturn(7);

            // When: Gọi service.verifyAndRegister với dữ liệu đã chuẩn bị.
            int result = service.verifyAndRegister("123456", session);

            // Then: chấp nhận mã đúng, tạo account một lần.
            assertEquals(7, result);
            assertEquals(null, session.getAttribute(REGISTER_STATE));
            verify(accounts, times(1)).createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH);
        }
    }

    @Test
    @DisplayName("AUTH-REGISTER-31 | Countdown: làm tròn 999ms thành 1 giây, đúng/sau hạn trả 0")
    void getResendRemainingSeconds_nearAndPastDeadline_returnsRoundedCountdown() {
        // Given: Countdown.
        try (MockedStatic<Instant> time = fixedTime()) {
            requestCode();
            // When / Then: Chỉ đọc countdown, không gọi gửi lại để tránh thay deadline.
            setTime(time, NOW.plusMillis(299001));
            // Then: làm tròn 999ms thành 1 giây, đúng/sau hạn trả 0.
            assertEquals(1L, service.getResendRemainingSeconds(session));
            setTime(time, NOW.plusSeconds(300));
            assertEquals(0L, service.getResendRemainingSeconds(session));
            setTime(time, NOW.plusSeconds(301));
            assertEquals(0L, service.getResendRemainingSeconds(session));
            assertEquals(EMAIL, service.getPendingRegistrationEmail(session));
            verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
        }
    }

    @Test
    @DisplayName("AUTH-REGISTER-32 | Request email khác dọn cooldown cũ: countdown session cũ bằng 0, pending vẫn còn hạn")
    void getResendRemainingSeconds_expiredEntryCleanedByAnotherRequest_returnsZero() {
        // Given: Đi qua public flow, không sửa trực tiếp map cooldown.
        try (MockedStatic<Instant> time = fixedTime()) {
            requestCode();
            setTime(time, NOW.plusSeconds(301));
            form.setUsername("candidate8");
            form.setEmail("candidate8@example.test");
            service.requestRegistration(form, new MockHttpSession());

            // When: Gọi service.getResendRemainingSeconds với dữ liệu đã chuẩn bị.
            long result = service.getResendRemainingSeconds(session);

            // Then: countdown session cũ bằng 0, pending vẫn còn hạn.
            assertEquals(0L, result);
            assertEquals(EMAIL, service.getPendingRegistrationEmail(session));
            verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
            verify(emailSender, times(1)).sendOtp(eq("candidate8@example.test"), anyString());
            verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
        }
    }

    @Test
    @DisplayName("AUTH-REGISTER-33 | Resend thành công sau 4 lần sai: mã mới có lại đủ giới hạn nhập sai")
    void resendOtp_afterFourWrongAttempts_resetsAttemptCounter() {
        // Given: Resend thành công sau 4 lần sai.
        try (MockedStatic<Instant> time = fixedTime()) {
            requestCode();
            when(encoder.matches("111111", HASH)).thenReturn(false);
            for (int attempt = 1; attempt <= 4; attempt++) {
                RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                    () -> service.verifyAndRegister("111111", session));
                assertEquals("REGISTER_OTP_INVALID", error.getErrorCode());
            }
            setTime(time, NOW.plusSeconds(300));
            when(encoder.encode(anyString())).thenReturn(NEW_OTP_HASH);
            service.resendOtp(session);
            when(encoder.matches("111111", NEW_OTP_HASH)).thenReturn(false);

            // When: Lần sai đầu tiên của OTP mới, dù tổng hai vòng đã sai 5 lần.
            RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.verifyAndRegister("111111", session));

            // Then: mã mới có lại đủ giới hạn nhập sai.
            assertEquals("REGISTER_OTP_INVALID", error.getErrorCode());
            assertEquals(EMAIL, service.getPendingRegistrationEmail(session));
            verify(emailSender, times(2)).sendOtp(eq(EMAIL), anyString());
            verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
        }
    }

    // Tính Instant xong trước khi bắt đầu stub để không lồng hai lời gọi static Mockito.
    private void setTime(MockedStatic<Instant> time, Instant currentTime) {
        time.when(Instant::now).thenReturn(currentTime);
    }

    // Production không nhận Clock: chỉ cố định Instant.now trong scope test.
    // Chọn inline cho mock thời gian này; các dependency khác vẫn dùng subclass như cấu hình project.
    private MockedStatic<Instant> fixedTime() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        MockedStatic<Instant> time = mockStatic(Instant.class,
                withSettings().mockMaker(MockMakers.INLINE).defaultAnswer(CALLS_REAL_METHODS));
        time.when(Instant::now).thenReturn(clock.instant());
        return time;
    }

    // Chỉ tạo yêu cầu OTP trong bộ nhớ. Hai dependency đã được mock.
    private void requestCode() {
        when(encoder.encode(anyString())).thenReturn(HASH);
        service.requestRegistration(form, session);
    }
}
