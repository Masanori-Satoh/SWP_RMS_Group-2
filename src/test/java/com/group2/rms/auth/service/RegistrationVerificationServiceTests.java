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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationVerificationServiceTests {

    private static final String EMAIL = "candidate7@example.test";
    private static final String PASSWORD = "TestPass123!";
    private static final String HASH = "mock-encoded-value";

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

        // When
        service.requestRegistration(form, session);

        // Then
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

        // When
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.getPendingRegistrationEmail(session));

        // Then
        assertEquals("REGISTER_REQUEST_INVALID", error.getErrorCode());
        assertEquals(Step.REGISTER, error.getStep());
    }

    @Test
    @DisplayName("AUTH-REGISTER-03 | Username đã dùng: trả lỗi field username, không mã hóa hoặc gửi OTP")
    void requestRegistration_duplicateUsername_doesNotSendCode() {
        // Given
        doThrow(new AccountFieldException("username", "Username already used."))
                .when(accounts).assertLoginIdentifiersAvailable("candidate7", EMAIL);

        // When
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.requestRegistration(form, session));

        // Then
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

        // When
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.verifyAndRegister("123456", session));

        // Then
        assertEquals("REGISTER_OTP_INVALID", error.getErrorCode());
        assertEquals(Step.OTP, error.getStep());
        assertEquals(EMAIL, service.getPendingRegistrationEmail(session));
        verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-05 | OTP sai định dạng: từ chối trước khi so khớp hash")
    void verifyAndRegister_malformedCode_doesNotCompareHash() {
        // Given
        requestCode();

        // When
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.verifyAndRegister("abc", session));

        // Then
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

        // Then
        assertEquals("REGISTER_OTP_ATTEMPTS_EXCEEDED", error.getErrorCode());
        assertEquals(Step.REGISTER, error.getStep());
        assertThrows(RegistrationFlowException.class, () -> service.getPendingRegistrationEmail(session));
        verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-07 | OTP đúng: tạo Candidate một lần bằng thông tin đã trim và hash đã chuẩn bị")
    void verifyAndRegister_correctCode_createsCandidateOnce() {
        // Given
        requestCode();
        when(encoder.matches("123456", HASH)).thenReturn(true);
        when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH)).thenReturn(7);

        // When
        int result = service.verifyAndRegister("123456", session);

        // Then
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

        // Then
        assertEquals("REGISTER_REQUEST_INVALID", error.getErrorCode());
        verify(accounts, times(1)).createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH);
    }

    @Test
    @DisplayName("AUTH-REGISTER-09 | Bấm gửi lại OTP ngay: báo cooldown và chỉ gửi một email")
    void resendOtp_beforeCooldownEnds_doesNotSendSecondCode() {
        // Given
        requestCode();

        // When
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.resendOtp(session));

        // Then
        assertEquals("REGISTER_EMAIL_COOLDOWN", error.getErrorCode());
        assertEquals(Step.OTP, error.getStep());
        assertEquals(true, error.getRetryAfterSeconds() > 0);
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-10 | Gửi lại form cùng email ở session khác: vẫn áp dụng cooldown theo email")
    void requestRegistration_sameEmailInAnotherSession_rejectsSecondSend() {
        // Given
        requestCode();
        MockHttpSession anotherSession = new MockHttpSession();

        // When
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.requestRegistration(form, anotherSession));

        // Then
        assertEquals("REGISTER_EMAIL_COOLDOWN", error.getErrorCode());
        assertEquals("email", error.getField());
        verify(emailSender, times(1)).sendOtp(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-11 | Username bị lấy trước lúc xác minh: trả lỗi field và xóa pending registration")
    void verifyAndRegister_identifierTakenBeforeCommit_clearsPendingRegistration() {
        // Given
        requestCode();
        when(encoder.matches("123456", HASH)).thenReturn(true);
        when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH))
                .thenThrow(new AccountFieldException("username", "Username already used."));

        // When
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.verifyAndRegister("123456", session));

        // Then
        assertEquals("REGISTER_FIELD_INVALID", error.getErrorCode());
        assertEquals("username", error.getField());
        assertThrows(RegistrationFlowException.class, () -> service.getPendingRegistrationEmail(session));
    }

    @Test
    @DisplayName("AUTH-REGISTER-12 | DB báo trùng UQ_User_Email: chuyển thành lỗi đăng ký trùng, không lộ lỗi DB")
    void verifyAndRegister_uniqueEmailConstraint_returnsConflictError() {
        // Given
        requestCode();
        when(encoder.matches("123456", HASH)).thenReturn(true);
        when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH))
                .thenThrow(new DataIntegrityViolationException("UQ_User_Email"));

        // When
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.verifyAndRegister("123456", session));

        // Then
        assertEquals("REGISTER_ACCOUNT_CONFLICT", error.getErrorCode());
        assertThrows(RegistrationFlowException.class, () -> service.getPendingRegistrationEmail(session));
    }

    @Test
    @DisplayName("AUTH-REGISTER-13 | Lỗi DB không phải trùng identifier: không đổi nhầm thành lỗi username/email")
    void verifyAndRegister_unrelatedDatabaseFailure_preservesOriginalError() {
        // Given
        requestCode();
        when(encoder.matches("123456", HASH)).thenReturn(true);
        when(accounts.createCandidateWithPasswordHash("Candidate Seven", "candidate7", EMAIL, HASH))
                .thenThrow(new DataIntegrityViolationException("FK_Candidate_UserId"));

        // When
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

        // When
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.requestRegistration(form, session));

        // Then
        assertEquals("REGISTER_EMAIL_DELIVERY_FAILED", error.getErrorCode());
        assertThrows(RegistrationFlowException.class, () -> service.getPendingRegistrationEmail(session));
        verify(accounts, times(0)).createCandidateWithPasswordHash(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AUTH-REGISTER-15 | Chưa có pending registration: không cho gọi gửi lại OTP")
    void resendOtp_withoutRegistration_doesNotSendEmail() {
        // Given: Session mới chưa từng đăng ký.

        // When
        RegistrationFlowException error = assertThrows(RegistrationFlowException.class,
                () -> service.resendOtp(session));

        // Then
        assertEquals("REGISTER_REQUEST_INVALID", error.getErrorCode());
        verify(emailSender, times(0)).sendOtp(anyString(), anyString());
    }

    // Chỉ tạo yêu cầu OTP trong bộ nhớ. Hai dependency đã được mock.
    private void requestCode() {
        when(encoder.encode(anyString())).thenReturn(HASH);
        service.requestRegistration(form, session);
    }
}
