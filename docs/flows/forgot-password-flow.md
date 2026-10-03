# Yêu cầu email xác minh để đặt lại mật khẩu

**Nguồn:** code hiện tại; link ký HMAC 15 phút, không JWT/bảng token. SMTP và browser thật chưa chạy trong lượt này. Hướng dẫn cấu hình và thao tác: [password-reset-test-setup.md](../testing/password-reset-test-setup.md).

## End-to-End Execution Flow

```text
GET /forgot-password → [PROJECT CODE] PasswordRecoveryController.forgotForm()
 → auth/forgot-password.html
POST /forgot-password + CSRF → @Valid ForgotPasswordForm
 → PasswordRecoveryController.requestReset()
 → PasswordResetEmailSender.isConfigured() + PasswordResetService.isConfigured()
 → PasswordResetService.request(email) → UserRepository.findByEmailIgnoreCase()
 → nếu Active: nonce + expiry + HMAC(email, PasswordHash, payload)
 → PasswordResetEmailSender.send(email, token) → SMTP
 → 302 /forgot-password?sent → thông báo chung
```

## Detailed Execution Trace

| Step | Loại; class.method; package/file | Input → output → next |
|---|---|---|
| 1 | [PROJECT CODE] `PasswordRecoveryController.forgotForm(Model)`; `com.group2.rms.controller` | GET public nhờ `SecurityConfig.filterChain()` → `ForgotPasswordForm`/template `auth/forgot-password`. |
| 2 | [SPRING FRAMEWORK] CSRF/MVC validation; [PROJECT CODE] `ForgotPasswordForm`; `com.group2.rms.controller.form` | POST email + CSRF → email nonblank/valid/≤150 → `BindingResult`; internal method **Not verified at source-code level**. |
| 3 | [PROJECT CODE] `PasswordRecoveryController.requestReset(...)` | Kiểm tra MailSender bean, địa chỉ From, public base URL và signing secret; thiếu → 200 lỗi, có → `PasswordResetService.request(email)`. Việc kiểm tra này không thử xác thực SMTP. |
| 4 | [PROJECT CODE] `PasswordResetService.request(String)`; `com.group2.rms.service` | Trim email; SELECT User. Nếu Active, tạo nonce 24 byte, expiry `now+900s`, payload `v1.userId.expiry.nonce`; HMAC-SHA256 với secret runtime + email + hash hiện tại; trả `ResetLink(email,token)`. |
| 5 | [PROJECT CODE] `PasswordResetEmailSender.send(String,String)`; `com.group2.rms.service` | Ghép `APP_PUBLIC_BASE_URL` + `/reset-password/{token}`, `SimpleMailMessage`, `JavaMailSender.send`. Mật khẩu SMTP và signing secret đến từ môi trường, không render. |
| 6 | [PROJECT CODE] `PasswordRecoveryController.requestReset(...)` | Có hay không có account đều 302 `/forgot-password?sent` (nếu cấu hình OK); lỗi gửi được bắt, log thông báo chung, vẫn redirect. |

## Failure / Alternative Flows

- Email không hợp lệ hoặc thiếu MailSender host/From/base URL/secret: 200 cùng form và lỗi; không gửi. Username/password SMTP sai có thể vẫn qua kiểm tra cấu hình, nhưng gửi mail sẽ lỗi.
- Email không tồn tại/Inactive/Blocked: `request()` trả empty, vẫn 302 thông báo chung để không lộ account.
- SMTP ném runtime error: controller log câu chung, vẫn 302 `?sent`; thông báo là đã tiếp nhận, **không chứng minh email đến nơi**.
- POST không CSRF: 403. Không có rate limiter bền vững trong schema/code này.

## Database Interaction

| Order | Repository Method | Entity | Table | Operation | Purpose |
|---|---|---|---|---|---|
| 1 | `UserRepository.findByEmailIgnoreCase` | User | `User` | SELECT | Tìm tài khoản Active để ký link. |

Không INSERT/UPDATE token; schema không có bảng token. SQL SELECT do JPA sinh động, chưa capture.

## Data Transformation

HTML email → `ForgotPasswordForm` → `request(email)` → `User` Active → payload ký HMAC + nonce → `ResetLink` → `SimpleMailMessage`/SMTP → redirect. `User.Email` và `PasswordHash` tham gia chữ ký nhưng không nằm trong payload/link ở dạng rõ.

## External Test Cases

| ID | Scenario | Preconditions | Action | Input | Expected HTTP | Expected Result | DB Change |
|---|---|---|---|---|---|---|---|
| FORGOT-01 | Email Active | SMTP/secret configured | Submit form | Email test | 302 rồi 200 | `?sent`, nhận mail có link 15 phút | Không |
| FORGOT-02 | Email không tồn tại | Cấu hình OK | Submit form | Email mới | 302 rồi 200 | Cùng thông báo chung, không mail | Không |
| FORGOT-03 | Thiếu cấu hình | SMTP hoặc secret thiếu | Submit form | Email hợp lệ | 200 | Báo chưa cấu hình | Không |
| FORGOT-04 | Thiếu CSRF | Browser guest | POST thủ công | Email, không token | 403 | Không gửi mail | Không |

## Test Procedure

Làm theo [hướng dẫn setup](../testing/password-reset-test-setup.md) để cấu hình SMTP test, signing secret và account Active. FORGOT-01: bấm **Quên mật khẩu?**, nhập email User Active, submit, xem `?sent`, kiểm tra hộp thư test và thời điểm link. FORGOT-02: dùng email không có trong DB, so sánh cùng thông báo và không có mail. FORGOT-03: ở môi trường test tắt SMTP host hoặc signing secret, submit, xem lỗi. FORGOT-04: gửi POST thiếu CSRF từ HTTP client. Không chụp ảnh hay log chứa token/secret.

## Test Data

Một User Active có mailbox test kiểm soát được; một email chưa tồn tại. Không dùng email thật hoặc secret production. DB chỉ đọc `UserId, Email, AccountStatus` để xác nhận fixture.

## Debugging Points

| Order | Class | Method | Why |
|---|---|---|---|
| 1 | `PasswordRecoveryController` [PROJECT CODE] | `requestReset` | Config, validation, catch SMTP. |
| 2 | `PasswordResetService` [PROJECT CODE] | `request` | Active filter, expiry/signature; không inspect/log token giá trị thật. |
| 3 | `UserRepository` [PROJECT CODE] | `findByEmailIgnoreCase` | Account lookup. |
| 4 | `PasswordResetEmailSender` [PROJECT CODE] | `isConfigured`, `send` | SMTP adapter/URL. |

## Code References

`src/main/java/com/group2/rms/config/SecurityConfig.java`; `src/main/java/com/group2/rms/controller/PasswordRecoveryController.java`; `src/main/java/com/group2/rms/controller/form/ForgotPasswordForm.java`; `src/main/java/com/group2/rms/service/PasswordResetService.java`; `src/main/java/com/group2/rms/service/PasswordResetEmailSender.java`; `src/main/java/com/group2/rms/repository/UserRepository.java`; `src/main/java/com/group2/rms/entity/User.java`; `src/main/resources/templates/auth/forgot-password.html`; `database/schema/db.sql`; `pom.xml`.

## Flow Completion Checklist

- [x] Entry point identified
- [x] Request URL identified
- [x] Security behavior documented
- [x] Controller identified
- [x] Service identified
- [x] Repository identified
- [x] Database interaction identified
- [ ] Spring internal components identified at source-code level (các method nội bộ chưa được xác minh)
- [x] Data transformation documented
- [x] Success path documented
- [x] Failure paths documented
- [x] External test cases created
- [x] Manual test procedure created
- [x] Debugging breakpoints documented
- [x] Code references verified
- [ ] Flow verified against current implementation bằng HTTP/browser/SMTP/DB ngoài test suite

**FLOW VERIFICATION: PARTIAL.**
