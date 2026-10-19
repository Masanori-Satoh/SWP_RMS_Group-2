# Unit test Authentication

Phạm vi: chỉ Authentication, gồm đăng nhập, đăng ký OTP, yêu cầu quên mật khẩu, xác minh OTP và lưu mật khẩu mới. Chưa làm Authorization, Account Manager, Department Manager hoặc Potential Candidate trong đợt này.

Có 67 case trong 4 lớp. Mỗi @Test có @DisplayName tiếng Việt, mã case riêng và Given–When–Then. Các test dùng JUnit 5, Mockito, assertEquals(expected, actual), assertThrows, assertNotNull, when(...).thenReturn(...) và verify(mock, times(n)). doThrow chỉ dùng để giả lập lỗi của method void.

Repository, dịch vụ gửi mail và PasswordEncoder đều được mock. MockHttpSession chỉ giữ trạng thái trong bộ nhớ; Clock fixed/mock giúp kiểm tra hết hạn mà không sleep. Không chạy Spring Boot, SQL, SMTP hay browser thật. Các test không chứng minh BCrypt thật, transaction DB, thu hồi session đăng nhập, CSRF hoặc concurrency. Các bug đã rà soát vẫn cần xử lý/kiểm chứng riêng.

## Kết quả thực thi

Đã chạy ngày 19/10/2026, hoàn tất lúc 09:52:07 (giờ Việt Nam), bằng lệnh nạp Mockito agent bên dưới.

| Lớp | Tests run | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| DatabaseUserDetailsServiceTests | 12 | 0 | 0 | 0 |
| RegistrationVerificationServiceTests | 15 | 0 | 0 | 0 |
| PasswordResetServiceTests | 22 | 0 | 0 | 0 |
| PasswordRecoveryServiceTests | 18 | 0 | 0 | 0 |
| Tổng | 67 | 0 | 0 | 0 |

Maven BUILD SUCCESS. Báo cáo từng lớp nằm tại target/surefire-reports/. JaCoCo được bỏ qua trong lần chạy này nên không công bố phần trăm coverage. Các lần thử trước bị kẹt ở bước tự attach agent trên Windows, không được tính là kết quả pass/fail của case.

Đã dừng sau Authentication để người dùng kiểm tra trước khi làm module tiếp theo.

## Cách chạy riêng module này

Chạy tại thư mục SWP_RMS_Group-2. Không chạy mvn test không có -Dtest vì sẽ chọn cả test DB/module khác.

```powershell
mvn.cmd -o "-Dtest=DatabaseUserDetailsServiceTests,RegistrationVerificationServiceTests,PasswordResetServiceTests,PasswordRecoveryServiceTests" test
```

Maven vẫn biên dịch các file test trong repo để kiểm tra cú pháp; tham số -Dtest chỉ chọn bốn lớp Authentication để thực thi.

Nếu Windows báo lỗi attach pipe của Mockito và test bị kẹt, nạp agent từ lúc khởi động JVM:

```powershell
$mockitoAgent = Join-Path $env:USERPROFILE '.m2\repository\org\mockito\mockito-core\5.17.0\mockito-core-5.17.0.jar'
mvn.cmd -o "-Djacoco.skip=true" "-DargLine=-javaagent:$mockitoAgent" "-Dtest=DatabaseUserDetailsServiceTests,RegistrationVerificationServiceTests,PasswordResetServiceTests,PasswordRecoveryServiceTests" test
```

Lệnh thay thế chỉ bỏ thu thập coverage JaCoCo và nạp Mockito agent; vẫn chạy đầy đủ assertions của cùng 67 case. Không đổi pom.xml hoặc cấu hình chung.

## Hai case thiếu dữ liệu mà người dùng yêu cầu giải thích

- AUTH-RESET-21: truyền snapshot=null, mật khẩu và thời hạn hợp lệ. Kỳ vọng RESET_REQUEST_INVALID; không tìm/khóa User, không encode và không save. Snapshot là thông tin tài khoản được phép reset, không phải OTP.
- AUTH-RESET-22: truyền snapshot hợp lệ và expiresAt=null. Kỳ vọng RESET_REQUEST_INVALID; không tìm/khóa User, không encode và không save vì không thể xác định yêu cầu còn hạn.

## Đăng nhập — DatabaseUserDetailsServiceTests (12 case)

[Mở file test](java/com/group2/rms/core/security/DatabaseUserDetailsServiceTests.java)

| Case | Điều kiện và kết quả mong đợi | Method @Test |
|---|---|---|
| AUTH-LOGIN-01 | Username tồn tại: trả đúng username, hash, quyền Candidate và trạng thái được đăng nhập | `loadUserByUsername_existingUsername_returnsLoginDetails` |
| AUTH-LOGIN-02 | Đăng nhập bằng email: trả username gốc của đúng tài khoản | `loadUserByUsername_existingEmail_returnsCanonicalUsername` |
| AUTH-LOGIN-03 | Identifier có khoảng trắng đầu/cuối: bỏ khoảng trắng trước khi tìm tài khoản | `loadUserByUsername_identifierHasSpaces_trimsBeforeLookup` |
| AUTH-LOGIN-04 | Không tìm thấy username hoặc email: báo thông tin đăng nhập không hợp lệ | `loadUserByUsername_missingAccount_throwsException` |
| AUTH-LOGIN-05 | Identifier trùng username và email của hai tài khoản khác nhau: từ chối đăng nhập | `loadUserByUsername_identifierBelongsToTwoAccounts_rejectsAmbiguousLogin` |
| AUTH-LOGIN-06 | Username và email cùng trỏ một tài khoản: không từ chối nhầm vì trùng identifier | `loadUserByUsername_bothLookupsReturnSameAccount_allowsLogin` |
| AUTH-LOGIN-07 | Tài khoản Inactive: trả UserDetails bị vô hiệu hóa để Security chặn đăng nhập | `loadUserByUsername_inactiveAccount_returnsDisabledDetails` |
| AUTH-LOGIN-08 | Tài khoản Blocked: trả UserDetails bị vô hiệu hóa để Security chặn đăng nhập | `loadUserByUsername_blockedAccount_returnsDisabledDetails` |
| AUTH-LOGIN-09 | Role không được hỗ trợ: từ chối tạo thông tin đăng nhập | `loadUserByUsername_unknownRole_throwsException` |
| AUTH-LOGIN-10 | Tài khoản thiếu role: từ chối tạo thông tin đăng nhập | `loadUserByUsername_missingRole_throwsException` |
| AUTH-LOGIN-11 | Identifier null: ném lỗi, không gọi repository tìm tài khoản | `loadUserByUsername_nullIdentifier_doesNotQueryRepository` |
| AUTH-LOGIN-12 | Identifier chỉ chứa khoảng trắng: ném lỗi, không gọi repository | `loadUserByUsername_blankIdentifier_doesNotQueryRepository` |

## Đăng ký OTP — RegistrationVerificationServiceTests (15 case)

[Mở file test](java/com/group2/rms/auth/service/RegistrationVerificationServiceTests.java)

| Case | Điều kiện và kết quả mong đợi | Method @Test |
|---|---|---|
| AUTH-REGISTER-01 | Đăng ký hợp lệ: trim thông tin, mã hóa password và gửi OTP, chưa tạo account | `requestRegistration_validForm_sendsCodeWithoutCreatingAccount` |
| AUTH-REGISTER-02 | Chưa yêu cầu đăng ký: đọc email OTP phải báo REGISTER_REQUEST_INVALID | `getPendingRegistrationEmail_missingSessionState_rejectsRequest` |
| AUTH-REGISTER-03 | Username đã dùng: trả lỗi field username, không mã hóa hoặc gửi OTP | `requestRegistration_duplicateUsername_doesNotSendCode` |
| AUTH-REGISTER-04 | OTP đủ 6 số nhưng sai: báo REGISTER_OTP_INVALID và chưa tạo account | `verifyAndRegister_incorrectCode_doesNotCreateAccount` |
| AUTH-REGISTER-05 | OTP sai định dạng: từ chối trước khi so khớp hash | `verifyAndRegister_malformedCode_doesNotCompareHash` |
| AUTH-REGISTER-06 | Nhập sai OTP lần thứ 5: xóa yêu cầu đăng ký và không tạo account | `verifyAndRegister_fifthWrongAttempt_clearsPendingRegistration` |
| AUTH-REGISTER-07 | OTP đúng: tạo Candidate một lần bằng thông tin đã trim và hash đã chuẩn bị | `verifyAndRegister_correctCode_createsCandidateOnce` |
| AUTH-REGISTER-08 | Xác minh lại sau khi đăng ký thành công: không tạo thêm tài khoản | `verifyAndRegister_alreadyCompleted_doesNotCreateAccountAgain` |
| AUTH-REGISTER-09 | Bấm gửi lại OTP ngay: báo cooldown và chỉ gửi một email | `resendOtp_beforeCooldownEnds_doesNotSendSecondCode` |
| AUTH-REGISTER-10 | Gửi lại form cùng email ở session khác: vẫn áp dụng cooldown theo email | `requestRegistration_sameEmailInAnotherSession_rejectsSecondSend` |
| AUTH-REGISTER-11 | Username bị lấy trước lúc xác minh: trả lỗi field và xóa pending registration | `verifyAndRegister_identifierTakenBeforeCommit_clearsPendingRegistration` |
| AUTH-REGISTER-12 | DB báo trùng UQ_User_Email: chuyển thành lỗi đăng ký trùng, không lộ lỗi DB | `verifyAndRegister_uniqueEmailConstraint_returnsConflictError` |
| AUTH-REGISTER-13 | Lỗi DB không phải trùng identifier: không đổi nhầm thành lỗi username/email | `verifyAndRegister_unrelatedDatabaseFailure_preservesOriginalError` |
| AUTH-REGISTER-14 | Gửi OTP thất bại: không tạo pending registration hoặc tạo account | `requestRegistration_emailDeliveryFails_doesNotCreatePendingRegistration` |
| AUTH-REGISTER-15 | Chưa có pending registration: không cho gọi gửi lại OTP | `resendOtp_withoutRegistration_doesNotSendEmail` |

## Tài khoản và lưu mật khẩu reset — PasswordResetServiceTests (22 case)

[Mở file test](java/com/group2/rms/auth/service/PasswordResetServiceTests.java)

| Case | Điều kiện và kết quả mong đợi | Method @Test |
|---|---|---|
| AUTH-RESET-01 | Email Active tồn tại: tạo snapshot chứa đúng ID, email và hash hiện tại | `findActiveAccount_existingEmail_returnsSnapshot` |
| AUTH-RESET-02 | Email có khoảng trắng đầu/cuối: tìm bằng email đã trim | `findActiveAccount_emailHasSpaces_trimsBeforeLookup` |
| AUTH-RESET-03 | Email null: báo EMAIL_REQUIRED, không truy vấn tài khoản | `findActiveAccount_nullEmail_throwsRequiredError` |
| AUTH-RESET-04 | Email chỉ chứa khoảng trắng: báo EMAIL_REQUIRED | `findActiveAccount_blankEmail_throwsRequiredError` |
| AUTH-RESET-05 | Email không tồn tại: báo EMAIL_NOT_FOUND tại field email | `findActiveAccount_missingEmail_throwsNotFoundError` |
| AUTH-RESET-06 | Email thuộc tài khoản Inactive: không cho khởi tạo reset | `findActiveAccount_inactiveAccount_throwsInactiveError` |
| AUTH-RESET-07 | Email thuộc tài khoản Blocked: không cho khởi tạo reset | `findActiveAccount_blockedAccount_throwsInactiveError` |
| AUTH-RESET-08 | Snapshot khớp tài khoản Active: xác nhận yêu cầu reset còn hợp lệ | `isCurrent_unchangedAccount_returnsTrue` |
| AUTH-RESET-09 | Kiểm tra snapshot null: trả false và không truy vấn tài khoản | `isCurrent_nullSnapshot_returnsFalse` |
| AUTH-RESET-10 | Tài khoản trong snapshot đã bị xóa: yêu cầu reset không còn hợp lệ | `isCurrent_removedAccount_returnsFalse` |
| AUTH-RESET-11 | Email tài khoản đã đổi sau khi tạo snapshot: trả false | `isCurrent_emailChanged_returnsFalse` |
| AUTH-RESET-12 | Hash mật khẩu đã đổi sau khi tạo snapshot: trả false | `isCurrent_passwordChanged_returnsFalse` |
| AUTH-RESET-13 | Tài khoản bị vô hiệu hóa sau khi tạo snapshot: trả false | `isCurrent_accountDeactivated_returnsFalse` |
| AUTH-RESET-14 | Snapshot hợp lệ và còn hạn: mã hóa mật khẩu mới, lưu đúng tài khoản một lần | `changePassword_validSnapshot_encodesAndSavesPassword` |
| AUTH-RESET-15 | Yêu cầu đã quá hạn: ném lỗi, giữ hash cũ và không lưu | `changePassword_expiredRequest_doesNotSave` |
| AUTH-RESET-16 | Thời gian hiện tại đúng bằng hạn reset: coi là hết hạn và không lưu | `changePassword_atExpiryBoundary_doesNotSave` |
| AUTH-RESET-17 | Mật khẩu đã được đổi bởi yêu cầu khác: không ghi đè bằng snapshot cũ | `changePassword_stalePasswordSnapshot_doesNotOverwriteNewerPassword` |
| AUTH-RESET-18 | Tài khoản không còn tồn tại khi lưu: ném lỗi và không ghi mật khẩu | `changePassword_missingAccount_doesNotSave` |
| AUTH-RESET-19 | Mật khẩu ngắn hơn 8 ký tự: báo PASSWORD_INVALID trước khi truy vấn DB | `changePassword_shortPassword_returnsPasswordFieldError` |
| AUTH-RESET-20 | Mật khẩu dài hơn 32 ký tự: từ chối và không lưu | `changePassword_passwordOver32Characters_doesNotSave` |
| AUTH-RESET-21 | Thiếu snapshot tài khoản: báo RESET_REQUEST_INVALID trước khi truy vấn hoặc lưu DB | `changePassword_nullSnapshot_rejectsRequest` |
| AUTH-RESET-22 | Thiếu thời hạn reset: báo RESET_REQUEST_INVALID trước khi truy vấn hoặc lưu DB | `changePassword_missingExpiry_rejectsRequest` |

## Quy trình quên mật khẩu OTP — PasswordRecoveryServiceTests (18 case)

[Mở file test](java/com/group2/rms/auth/service/PasswordRecoveryServiceTests.java)

| Case | Điều kiện và kết quả mong đợi | Method @Test |
|---|---|---|
| AUTH-RECOVERY-01 | Email Active và gửi mail thành công: tạo trạng thái chờ OTP, chưa đổi mật khẩu | `requestReset_validAccount_createsPendingOtp` |
| AUTH-RECOVERY-02 | SMTP chưa cấu hình: báo unavailable trước khi tìm tài khoản hoặc gửi OTP | `requestReset_emailNotConfigured_rejectsBeforeLookup` |
| AUTH-RECOVERY-03 | Email không có account Active: giữ lỗi tìm tài khoản, không gửi OTP | `requestReset_accountLookupFails_doesNotSendEmail` |
| AUTH-RECOVERY-04 | Gửi lại yêu cầu ngay: báo cooldown, không gửi email thứ hai | `requestReset_beforeCooldownEnds_doesNotSendAgain` |
| AUTH-RECOVERY-05 | Gửi email lỗi: không tạo trạng thái OTP và cho phép thử gửi lại | `requestReset_deliveryFailure_releasesReservationForRetry` |
| AUTH-RECOVERY-06 | Chưa có yêu cầu OTP: không cho xác minh mã | `verifyOtp_withoutPendingRequest_rejectsRequest` |
| AUTH-RECOVERY-07 | OTP đúng và còn hạn: cho phép tới bước đặt mật khẩu mới | `verifyOtp_correctCode_allowsPasswordReset` |
| AUTH-RECOVERY-08 | OTP đủ 6 số nhưng sai: báo OTP_INVALID và giữ bước chờ mã | `verifyOtp_incorrectCode_keepsPendingRequest` |
| AUTH-RECOVERY-09 | OTP sai định dạng: không gọi so khớp hash | `verifyOtp_malformedCode_rejectsBeforeHashComparison` |
| AUTH-RECOVERY-10 | Sai OTP lần thứ 5: xóa trạng thái recovery và chưa đổi mật khẩu | `verifyOtp_fifthWrongAttempt_clearsResetState` |
| AUTH-RECOVERY-11 | OTP đến đúng hạn 15 phút: bị từ chối và xóa trạng thái, không cần sleep | `verifyOtp_atExpiryBoundary_rejectsExpiredCode` |
| AUTH-RECOVERY-12 | Account đã đổi sau khi gửi OTP: vô hiệu hóa yêu cầu recovery cũ | `verifyOtp_accountSnapshotChanged_rejectsRequest` |
| AUTH-RECOVERY-13 | Chưa xác minh OTP: không được lưu mật khẩu mới | `completeReset_beforeOtpVerification_doesNotChangePassword` |
| AUTH-RECOVERY-14 | OTP đã xác minh: gọi đổi mật khẩu đúng snapshot/thời hạn và xóa trạng thái sau thành công | `completeReset_verifiedOtp_changesPasswordAndClearsState` |
| AUTH-RECOVERY-15 | Gửi lại thao tác lưu sau reset thành công: không đổi mật khẩu lần thứ hai | `completeReset_alreadyCompleted_rejectsReplay` |
| AUTH-RECOVERY-16 | Lưu mật khẩu thất bại: giữ trạng thái OTP đã xác minh để người dùng sửa và thử lại | `completeReset_passwordSaveFails_keepsVerifiedState` |
| AUTH-RECOVERY-17 | Session chưa có yêu cầu recovery: thời gian chờ gửi lại bằng 0 | `getResendRemainingSeconds_withoutRequest_returnsZero` |
| AUTH-RECOVERY-18 | OTP đã xác minh: xác minh lại không so khớp mã lần nữa | `verifyOtp_alreadyVerified_doesNotCompareCodeAgain` |

## Nguồn tham khảo

- [Video người dùng cung cấp](https://youtu.be/29rGLS-aOB8).
- [Bài viết của tác giả nhúng video đó](https://holydev.uk/microservices/bai-11-mockito-junit-5-huong-dan-viet-unit-test-cho-service-layer-step-by-step/).
