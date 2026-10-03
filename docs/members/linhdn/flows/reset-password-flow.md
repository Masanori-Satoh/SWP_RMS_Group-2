# Xác minh liên kết và đặt lại mật khẩu

**Nguồn:** `PasswordRecoveryController`, `PasswordResetService`; không JWT/token table. Link 15 phút, bị vô hiệu sau thay password/email. Browser/SMTP thật chưa chạy trong lượt này. Hướng dẫn thao tác: [password-reset-test-setup.md](../testing/password-reset-test-setup.md).

## End-to-End Execution Flow

```text
Browser mở link GET /reset-password/{token}
 → [PROJECT CODE] PasswordRecoveryController.resetForm()
 → PasswordResetService.isValid() → UserRepository.findById()
 → auth/reset-password.html: form chỉ hiện nếu chữ ký/hạn/status hợp lệ
 → POST /reset-password/{token} + CSRF + password/confirm
 → PasswordRecoveryController.reset() → PasswordResetService.reset()
 → UserRepository.findByIdForUpdate() → kiểm tra HMAC/hạn/Active
 → BCryptPasswordEncoder.encode() → UserRepository.saveAndFlush()
 → transaction commit → 302 /login?reset
```

## Detailed Execution Trace

| Step | Loại; class.method; package/file | Input → output → next |
|---|---|---|
| 1 | [PROJECT CODE] `PasswordRecoveryController.resetForm(String,Model,HttpServletResponse)`; `com.group2.rms.controller` | GET public `/reset-password/{token}` → `Cache-Control:no-store`, `Referrer-Policy:no-referrer`; gọi `isValid`, render view. |
| 2 | [PROJECT CODE] `PasswordResetService.isValid(String)`; `com.group2.rms.service` | Parse regex `v1.userId.expiry.nonce.signature`; `UserRepository.findById`; `validForUser` kiểm tra Active, thời gian, HMAC constant-time. Không hợp lệ → form không hiện. |
| 3 | [SPRING FRAMEWORK] CSRF + MVC/Jakarta validation; [PROJECT CODE] `ResetPasswordForm`; `com.group2.rms.controller.form` | POST token path, password 8–32, confirm + CSRF → `BindingResult`; internal method **Not verified at source-code level**. |
| 4 | [PROJECT CODE] `PasswordRecoveryController.reset(...)` | Confirm phải khớp; lỗi render lại, xóa giá trị password; hợp lệ gọi `PasswordResetService.reset(token,newPassword)`. |
| 5 | [PROJECT CODE] `PasswordResetService.reset(...)`; `com.group2.rms.service` | Parse/hạn, `@Transactional`, `findByIdForUpdate` dùng `PESSIMISTIC_WRITE`; kiểm tra lại chữ ký với hash hiện tại rồi encode BCrypt, `saveAndFlush`; sau commit link cũ không hợp lệ. |
| 6 | [PROJECT CODE] `PasswordRecoveryController.reset(...)` | `true` → 302 `/login?reset`; `false` → 200 form báo link không hợp lệ. |

## Failure / Alternative Flows

- Token sai, hết hạn, account không Active hoặc email/hash đã đổi: `isValid/reset=false`; form không mở hoặc báo lỗi. Mọi link ký bằng hash cũ bị vô hiệu sau reset đầu tiên.
- Password sai độ dài hoặc confirm sai: render lỗi, không UPDATE.
- Thiếu CSRF: 403. Config signing key thiếu: token không hợp lệ.
- Reset không thu hồi ngay mọi session cũ trong code hiện tại; đây là giới hạn đã ghi ở `docs/WORK_LOG.md`.

## Database Interaction

| Order | Repository Method | Entity | Table | Operation | Purpose |
|---|---|---|---|---|---|
| 1 | `UserRepository.findById` | User | `User` | SELECT | Xác minh link ở GET/POST lỗi. |
| 2 | `UserRepository.findByIdForUpdate` | User | `User` | SELECT + pessimistic lock | Tránh hai reset đồng thời. |
| 3 | `UserRepository.saveAndFlush` | User | `User` | UPDATE | Ghi PasswordHash BCrypt mới. |

SQL do Hibernate/JPA sinh động; chưa capture. Không có INSERT token.

## Data Transformation

URL token → `ParsedToken` private (userId, expiry, payload, signature) → User hiện tại → HMAC verify → form password/confirm → BCrypt hash mới → `User.PasswordHash`; password và token không được log/render lại dạng plaintext sau submit.

## External Test Cases

| ID | Scenario | Preconditions | Action | Input | Expected HTTP | Expected Result | DB Change |
|---|---|---|---|---|---|---|---|
| RESET-01 | Link hợp lệ | Có email test mới gửi | Mở, đặt password mới | Mật khẩu 8–32 khớp | 200, POST 302 | `/login?reset`; login password mới được | UPDATE hash |
| RESET-02 | Dùng lại link | RESET-01 xong | Mở lại link | Token cũ | 200 | Báo không hợp lệ, không form | Không |
| RESET-03 | Link hết hạn | Link quá 15 phút | Mở link | Token cũ | 200 | Báo hết hạn | Không |
| RESET-04 | Confirm sai/thiếu CSRF | Link hợp lệ | Submit sai | Hai password khác hoặc không token | 200/403 | Không đổi password | Không |
| RESET-05 | Login bằng password cũ | RESET-01 xong | Đăng nhập | Password cũ | 302 `/login?error` | Không vào Dashboard | Không |
| RESET-06 | Login bằng password mới | RESET-01 xong | Đăng nhập | Password mới | 302 `/dashboard`, GET 200 | Đăng nhập thành công | Không |

## Test Procedure

Chuẩn bị theo [hướng dẫn setup](../testing/password-reset-test-setup.md). RESET-01: gửi email, mở link trong 15 phút, nhập password mới và confirm, thấy `/login?reset`; dùng cùng phiên SSMS để so sánh hash trước/sau mà không in giá trị. RESET-02: mở lại chính link cũ, không thấy form. RESET-03: gửi link khác, chờ >15 phút rồi mở, thấy lỗi; không dùng clock của máy production để làm giả. RESET-04: với link mới, nhập confirm khác, kiểm tra lỗi; thử POST không `_csrf`, xác nhận 403. RESET-05: ở trang Login, nhập mật khẩu cũ và xác nhận `/login?error`. RESET-06: nhập mật khẩu mới và xác nhận vào Dashboard. Không ghi token, password, hash vào chứng cứ.

## Test Data

Một User Active với mailbox test. Ghi `UserId` và kiểm tra hash thay đổi bằng phép so sánh nội bộ/test, không in giá trị hash. Không dùng account thật.

## Debugging Points

| Order | Class | Method | Why |
|---|---|---|---|
| 1 | `PasswordRecoveryController` [PROJECT CODE] | `resetForm`, `reset` | Render, BindingResult, redirect. |
| 2 | `PasswordResetService` [PROJECT CODE] | `parse`, `validForUser`, `reset` | Hạn/chữ ký/transaction; không log token. |
| 3 | `UserRepository` [PROJECT CODE] | `findByIdForUpdate`, `saveAndFlush` | Lock và UPDATE. |
| 4 | `BCryptPasswordEncoder` [SPRING FRAMEWORK] | `encode` | Hash mới; bean từ `SecurityConfig`. |

## Code References

`src/main/java/com/group2/rms/config/SecurityConfig.java`; `src/main/java/com/group2/rms/controller/PasswordRecoveryController.java`; `src/main/java/com/group2/rms/controller/form/ResetPasswordForm.java`; `src/main/java/com/group2/rms/service/PasswordResetService.java`; `src/main/java/com/group2/rms/repository/UserRepository.java`; `src/main/java/com/group2/rms/entity/User.java`; `src/main/resources/templates/auth/reset-password.html`; `database/schema/db.sql`.

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
