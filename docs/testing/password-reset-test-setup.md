# Chạy thử Forgot Password → Email → Reset Password trên Windows

Tài liệu này áp dụng cho checkout RMS hiện tại (Spring Boot 3.5.16). Project dùng form login + HTTP session, BCrypt, CSRF và link HMAC hết hạn 15 phút; **không dùng JWT hoặc bảng reset token**. Hai flow kỹ thuật ở [Forgot Password](../flows/forgot-password-flow.md) và [Reset Password](../flows/reset-password-flow.md). Chỉ ghi PASS E2E sau khi thấy email trong mailbox, đổi hash trong DB và đăng nhập bằng mật khẩu mới.

## Requirements

1. SQL Server chạy database local `RitirementManagement2` theo `database/schema/db.sql`. File schema có lệnh **xóa/tạo lại DB**; không chạy lại trên DB đang có dữ liệu. `src/main/resources/application.properties` trên máy này là file Git ignore chứa kết nối DB local và `spring.jpa.hibernate.ddl-auto=validate`.
2. Java 21, Maven (`mvn.cmd`) hoặc Maven Wrapper, Spring Boot và browser. `pom.xml` đã có `spring-boot-starter-mail`, cung cấp Spring Boot `JavaMailSender` khi `spring.mail.host` được cấu hình. [Spring Boot 3.5 mail documentation](https://docs.spring.io/spring-boot/3.5/reference/io/email.html).
3. Một SMTP test sender và mailbox bạn thực sự mở được. Script dưới đây mặc định Gmail SMTP `smtp.gmail.com:587` với SMTP AUTH + STARTTLS, theo [cấu hình SMTP Gmail](https://support.google.com/mail/answer/7104828). Có thể truyền SMTP host/port khác nếu nhà cung cấp hỗ trợ AUTH + STARTTLS.
4. Một User `Active` có Email đúng mailbox nhận test, PasswordHash BCrypt và Role hợp lệ. Cách tạo qua Register ở phần **Test Account Setup**. Không sử dụng dữ liệu production.

## Environment Variables

Project **không tự đọc `.env`**. `.env.example` chỉ liệt kê tên biến; `application.yml` không tồn tại. Spring Boot đọc biến môi trường của tiến trình. Tên thực tế là `SPRING_MAIL_*`, **không phải** `MAIL_HOST`/`MAIL_PORT`/`MAIL_USERNAME`/`MAIL_PASSWORD`. `PasswordResetService` đọc `APP_PASSWORD_RESET_SECRET`; `PasswordResetEmailSender` đọc `APP_MAIL_FROM` và `APP_PUBLIC_BASE_URL`. Script `scripts/run-password-reset-local.ps1` thiết lập chúng chỉ cho phiên chạy rồi khôi phục môi trường cũ.

| Variable | Purpose | Example placeholder | Required/Optional |
|---|---|---|---|
| `SPRING_MAIL_HOST` | SMTP host, kích hoạt MailSender bean | `smtp.gmail.com` | Required |
| `SPRING_MAIL_PORT` | SMTP STARTTLS port | `587` | Required cho Gmail |
| `SPRING_MAIL_USERNAME` | Tài khoản gửi | `<your-test-mailbox>` | Required cho SMTP AUTH |
| `SPRING_MAIL_PASSWORD` | App Password/SMTP credential; script nhập ẩn | `<enter-in-local-prompt>` | Required cho SMTP AUTH |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | JavaMail `mail.smtp.auth` | `true` | Required cho Gmail |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | Bật STARTTLS | `true` | Required cho Gmail |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED` | Không gửi nếu không có TLS | `true` | Recommended |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_CONNECTIONTIMEOUT` / `...TIMEOUT` / `...WRITETIMEOUT` | Giới hạn thời gian chờ; script đặt 10000 ms | `10000` | Recommended |
| `APP_MAIL_FROM` | From address trong `SimpleMailMessage` | `<same-test-mailbox>` | Required |
| `APP_PUBLIC_BASE_URL` | Gốc URL trong email, phải trùng cổng app | `http://localhost:8082` | Required |
| `APP_PASSWORD_RESET_SECRET` | HMAC signing key ≥32 byte UTF-8 | `<random-secret-outside-Git>` | Required; script tự tạo nếu chưa có |
| `SERVER_PORT` | Ghi đè `server.port` local để khớp base URL | `8082` | Script đặt tự động |

`SPRING_MAIL_PASSWORD` và signing secret không được lưu vào `.env`, `application.properties`, command line, Git hoặc báo cáo test. Script sinh 48 byte ngẫu nhiên rồi mã hóa Base64 cho signing secret nếu tiến trình chưa có khóa. Giữ **cùng một tiến trình app** từ lúc gửi email đến lúc bấm link; chạy lại script với khóa mới sẽ vô hiệu link cũ. Nếu đã tự đặt `APP_PASSWORD_RESET_SECRET` trong PowerShell thì script dùng lại sau khi kiểm tra độ dài ≥32 byte. Spring Boot cho phép biến môi trường ghi đè `application.properties`; xem [Externalized Configuration](https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html).

## Test Account Setup

Schema `dbo.[User]` yêu cầu `RoleId`, `Username` duy nhất, `PasswordHash`, `Email` duy nhất, `FullName`, `AccountStatus` và `CreatedAt`; `DepartmentId` có thể NULL. `Candidate.UserId` là FK UNIQUE/NOT NULL. Project đã có Register tạo `User` Active + `Candidate` cùng transaction và hash BCrypt, nên dùng **giao diện** thay vì tự tạo hash/INSERT User.

1. Trong SSMS kết nối **DB local test**, kiểm tra Role Candidate bằng truy vấn chỉ đọc:

   ```sql
   USE RitirementManagement2;
   SELECT RoleId, RoleName FROM dbo.[Role] WHERE RoleName = N'Candidate';
   ```

2. Nếu Role Candidate chưa có, dừng và chuẩn bị dữ liệu role theo quy trình seed được nhóm duyệt trên DB test; không chạy `database/schema/db.sql` để sửa thiếu seed. Test hiện có `AuthenticationDatabaseTests` cũng yêu cầu role này.
3. Khi app đã chạy, mở `/register` từ liên kết trên Login. Nhập họ tên, Username chưa dùng, Email đúng mailbox bạn kiểm soát, mật khẩu test 8–32 ký tự và xác nhận. Submit; xem `/login?registered`. Giữ mật khẩu test cũ để kiểm tra sau reset; không ghi vào tài liệu/log.
4. Trong SSMS xác nhận account Active, role Candidate và profile liên kết, không SELECT `PasswordHash`:

   ```sql
   DECLARE @TestEmail NVARCHAR(150) = N'<your-test-mailbox>';
   SELECT u.UserId, u.Username, u.Email, u.AccountStatus,
          r.RoleName, c.CandidateId
   FROM dbo.[User] AS u
   JOIN dbo.[Role] AS r ON r.RoleId = u.RoleId
   LEFT JOIN dbo.Candidate AS c ON c.UserId = u.UserId
   WHERE u.Email = @TestEmail;
   ```

   Kỳ vọng đúng một User `Active`, `RoleName=Candidate` và một `CandidateId`. Có thể dùng account Active sẵn có với mailbox này nếu phù hợp, không cần tạo trùng email.

## Run Application

Trong PowerShell hoặc terminal PowerShell của VS Code, tại thư mục `SWP_RMS_Group-2`:

```powershell
mvn.cmd -q -DskipTests compile
mvn.cmd -q test
& .\scripts\run-password-reset-local.ps1
```

Script hỏi Email SMTP và **App Password bằng prompt ẩn**, tự sinh signing secret (trừ khi đã có), cấu hình Gmail và base URL `http://localhost:8082`, rồi chạy Spring Boot. Nếu cổng 8082 bận, dùng:

```powershell
& .\scripts\run-password-reset-local.ps1 -Port 18082
```

Khi đổi cổng, script đồng bộ `SERVER_PORT` và `APP_PUBLIC_BASE_URL`. Chỉ bắt đầu test sau khi log có `Started RmsApplication` và trình duyệt mở được trang. **Không coi exit code Maven là bằng chứng startup**: trong môi trường thực thi này Maven trả exit 0 dù Tomcat log `APPLICATION FAILED TO START`. Đã gặp 8082 bận và `Unable to establish loopback connection` ở cả 18082 lẫn 18083; nếu máy bạn lặp lại lỗi này, ghi BLOCKED với lỗi thực tế, không đánh dấu E2E PASS.

Nếu PowerShell chặn script theo execution policy, chạy nó trong một PowerShell mới cho riêng lượt test theo chính sách máy của bạn; có thể đặt các biến ở **Process** scope thủ công qua prompt ẩn rồi chạy `mvn.cmd spring-boot:run`. Đừng dán App Password vào lệnh một dòng hay chat. Không dùng `setx` cho secret vì nó lưu lâu dài vào user environment.

### Gmail App Password: ACTION REQUIRED FROM USER

Nếu chọn Gmail: trên tài khoản **test** của bạn, bật 2-Step Verification và tạo App Password trong Google Account theo [hướng dẫn chính thức](https://support.google.com/accounts/answer/185833). Một số tài khoản tổ chức/Advanced Protection không có tùy chọn này; dùng SMTP test khác nếu vậy. Nhập App Password vào **prompt trên máy**, không dùng mật khẩu Gmail chính và không gửi cho Codex. Mailbox nhận có thể là cùng địa chỉ test; Gmail SMTP dùng `smtp.gmail.com:587` với AUTH/TLS theo [Google](https://support.google.com/mail/answer/7104828).

## Forgot Password Test

**FORGOT-01:** Mở `http://localhost:8082/forgot-password` (hoặc cổng đã chọn), nhập Email của User Active, bấm **Gửi liên kết xác minh**. Browser POST form có CSRF, dự kiến 302 tới `/forgot-password?sent` rồi GET 200. `PasswordRecoveryController.requestReset()` gọi `PasswordResetService.request()` → `UserRepository.findByEmailIgnoreCase()` → `PasswordResetEmailSender.send()` → `JavaMailSender`/SMTP. Kiểm tra mailbox có email mới và link mang đúng host/port; không chép link/token vào log. Generic `?sent` một mình **không chứng minh email gửi thành công**, vì code cũng dùng thông báo này khi SMTP ném lỗi.

**FORGOT-02:** Dùng Email chưa có trong DB nhưng là alias/mailbox phụ bạn kiểm soát nếu muốn xác minh không có thư; form vẫn trả cùng `?sent`, không tiết lộ tồn tại tài khoản. Nếu không kiểm soát inbox đó, chỉ xác nhận phản hồi generic và ghi phần “không nhận email” là chưa kiểm chứng.

## Reset Password Test

**RESET-01:** Trong 15 phút và cùng lần chạy app, mở link từ mailbox. GET `/reset-password/{token}` phải 200, hiển thị form nếu `PasswordResetService.isValid()` đúng. Nhập mật khẩu mới 8–32 ký tự khác mật khẩu cũ, confirm khớp, submit bằng form có CSRF. `PasswordResetService.reset()` gọi `UserRepository.findByIdForUpdate()` (pessimistic lock), BCrypt `encode()`, `saveAndFlush()` và commit. Kỳ vọng 302 `/login?reset` rồi GET 200.

**RESET-05/06:** Đăng nhập bằng mật khẩu cũ → 302 `/login?error`; bằng mật khẩu mới → 302 `/dashboard` và Dashboard 200. Có thể dùng Username hoặc Email.

## Database Verification

Không SELECT, in, chụp hoặc log `PasswordHash`. Trong **cùng một phiên SSMS của DB test**, trước khi submit Reset Password, chạy batch 1 để lưu hash vào temp table của phiên, chỉ in số dòng:

```sql
USE RitirementManagement2;
DECLARE @TestEmail NVARCHAR(150) = N'<your-test-mailbox>';
IF OBJECT_ID('tempdb..#ResetBefore') IS NOT NULL DROP TABLE #ResetBefore;
SELECT UserId, PasswordHash INTO #ResetBefore
FROM dbo.[User]
WHERE Email = @TestEmail AND AccountStatus = N'Active';
SELECT COUNT(*) AS SnapshotRows FROM #ResetBefore;
```

Kỳ vọng `SnapshotRows=1`. Sau khi reset trong browser, **giữ nguyên kết nối SSMS** và chạy batch 2 (đổi placeholder Email):

```sql
DECLARE @TestEmail NVARCHAR(150) = N'<your-test-mailbox>';
SELECT u.UserId, u.AccountStatus,
       CASE WHEN CONVERT(VARBINARY(MAX), u.PasswordHash) <>
                 CONVERT(VARBINARY(MAX), b.PasswordHash)
            THEN N'CHANGED' ELSE N'UNCHANGED' END AS PasswordHashState
FROM dbo.[User] AS u
JOIN #ResetBefore AS b ON b.UserId = u.UserId
WHERE u.Email = @TestEmail;
DROP TABLE #ResetBefore;
```

Kỳ vọng `PasswordHashState=CHANGED`, status vẫn `Active`. Temp table chỉ nằm trong phiên SQL test và bị xóa sau đối chiếu. Đăng nhập mật khẩu cũ/mới như trên là kiểm tra xác thực bổ sung.

## Failure Tests

| ID | Các bước ngoài hệ thống | Kết quả cần quan sát |
|---|---|---|
| FORGOT-03 | Khởi động app trong terminal test mới **không** đặt SMTP host hoặc signing secret; mở form và submit email hợp lệ. | HTTP 200 form báo dịch vụ chưa cấu hình; không gửi email. Đừng xóa cấu hình của app đang dùng chung. |
| FORGOT-04 | Gửi `curl.exe -i -X POST http://localhost:8082/forgot-password -d "email=unknown@example.test"` không CSRF (đổi cổng nếu cần). | HTTP 403; không gửi email. Không disable CSRF. |
| RESET-02 | Sau RESET-01, mở lại **chính link đã dùng**. | GET 200 với “Liên kết không hợp lệ…”; không có form reset, vì chữ ký gắn hash cũ. |
| RESET-03 | Xin link mới khi app còn chạy, chờ **hơn 15 phút**, mở link. | GET 200 với thông báo không hợp lệ/hết hạn; không thay clock production hoặc expiry code. |
| RESET-04 | Với link còn hợp lệ, nhập mật khẩu mới và confirm khác nhau, submit form. | HTTP 200 báo không khớp; DB unchanged. Có thể kiểm tra `PasswordHashState=UNCHANGED` bằng cách chụp snapshot mới như trên. |
| RESET-05 | Sau reset hợp lệ, đăng nhập mật khẩu cũ. | 302 `/login?error`, không vào Dashboard. |
| RESET-06 | Đăng nhập mật khẩu mới. | 302 `/dashboard`, GET 200. |

Trường hợp account Inactive/Blocked: `PasswordResetService.request()` không tạo link; link đã gửi trước khi đổi status cũng bị `isValid()` từ chối. Cần account test khác nếu muốn thử, không vô hiệu hóa account chính/production. POST Reset thiếu CSRF cũng phải 403; dùng form browser để thử luồng chuẩn.

## Expected Results

| Bước | HTTP/UI | DB |
|---|---|---|
| GET Forgot | 200, form có CSRF khi render | Không đổi |
| POST Forgot Active | 302 `?sent`, email thật xuất hiện | Không đổi |
| POST Forgot unknown | 302 `?sent`, không tiết lộ account | Không đổi |
| GET Reset valid | 200, form mật khẩu mới | Không đổi |
| POST Reset valid | 302 `/login?reset` | Chỉ `User.PasswordHash` đổi (và metadata update nếu JPA quản lý) |
| Reuse/expired link | 200 báo invalid, không form | Không đổi |
| Old/new login | Cũ lỗi; mới vào Dashboard | Không đổi |

## Troubleshooting

| Triệu chứng | Kiểm tra an toàn |
|---|---|
| `MailAuthenticationException` | Gmail 2-Step Verification/App Password đúng account, `SPRING_MAIL_USERNAME` khớp `APP_MAIL_FROM`; không dùng mật khẩu Gmail chính. App Password có thể không khả dụng cho tài khoản tổ chức/Advanced Protection. Không in credential/log exception chứa chi tiết nhạy cảm. |
| SMTP connection refused/timeout | Kiểm tra host/port, Internet/firewall bằng `Test-NetConnection smtp.gmail.com -Port 587`; script đặt timeout hữu hạn. Nếu provider khác, dùng host/port do provider cấp. |
| Form báo `?sent` nhưng không thấy email | `?sent` là thông báo chung; kiểm tra Spam, sender mailbox, SMTP connectivity và log generic `Password recovery email delivery failed`. Không log reset URL/token hoặc SMTP error message. |
| Invalid reset token | Link quá 15 phút, đã dùng, account/email/password hash/status đổi, hoặc app restart với signing secret mới. Kiểm tra `APP_PUBLIC_BASE_URL` trùng cổng, không chia sẻ token để debug. |
| HTTP 403 CSRF | Dùng form Thymeleaf cùng session/cookie; POST thủ công phải kèm token mới. Không tắt CSRF. |
| User not found / không nhận mail | Query chỉ đọc `UserId,Email,AccountStatus` cho test mailbox. Email không tồn tại hoặc account không Active đều được phản hồi generic. |
| Inactive account | `AccountStatus` phải Active trước khi xin link và khi dùng link; dùng User test Active. |
| Database connection error | SQL Server/DB name, kết nối trong file local Git ignore, `ddl-auto=validate`, Role Candidate; không in DB password. `mvn test` có thể kiểm tra Context/JPA. |
| MailSender bean không có | Phải có `spring-boot-starter-mail` và `SPRING_MAIL_HOST`; test `MailEnvironmentBindingTests` xác minh binding/auto-config mà không gửi email. |
| Port 8082 bận hoặc Tomcat loopback lỗi | Dùng `-Port` khác để đồng bộ URL; nếu vẫn `Unable to establish loopback connection`, ghi BLOCKED và kiểm tra Java/network stack trên máy, không đánh dấu test E2E PASS. |

## Nguồn kiểm chứng cấu hình

- [Spring Boot 3.5: Sending Email](https://docs.spring.io/spring-boot/3.5/reference/io/email.html) và [Externalized Configuration](https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html).
- [Google: Gmail SMTP host/port](https://support.google.com/mail/answer/7104828) và [Google: App Password](https://support.google.com/accounts/answer/185833).
- Mã dự án: `SecurityConfig`, `PasswordRecoveryController`, `PasswordResetService`, `PasswordResetEmailSender`, `UserRepository`, `User`, `RegisterAccountForm`, `CandidateRegistrationService`; cấu hình mẫu `.env.example` và script `scripts/run-password-reset-local.ps1`.
