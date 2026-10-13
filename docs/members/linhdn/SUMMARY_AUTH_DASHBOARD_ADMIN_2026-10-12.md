# Tổng hợp thay đổi — Login, Register, Dashboard và Admin

**Cập nhật đến:** 12/10/2026 (Asia/Saigon).

**Repository đối chiếu:** `SWP_RMS_Group-2`, HEAD `83f5fca` — `push code for merge main`. Working tree sạch tại lần đối chiếu trước khi tạo tài liệu này.

Tài liệu tổng hợp công việc trong chuỗi sửa của LinhDN và các đợt hỗ trợ, ưu tiên tên file/implementation hiện tại. Nguồn gồm Git, source và nhật ký 01–12/10. Không coi toàn bộ nội dung của commit merge hoặc một file nhiều người chỉnh là đóng góp riêng của LinhDN. Các kết quả test bên dưới là kết quả của từng đợt đã ghi nhận, không phải chứng nhận cho mọi thay đổi mới nhất.

## 1. Tổng quan chức năng

| Nhóm | Những thay đổi đã thực hiện |
|---|---|
| Login | UI tiếng Việt theo homepage Mộc, hiện/ẩn password, thông báo lỗi/thành công, login username hoặc email bằng Spring Security form/session |
| Register | Validate thông tin trước, gửi OTP sau; OTP đúng mới tạo User Active và Candidate profile trong transaction |
| Forgot / Reset Password | Thay link reset trong email bằng luồng Email → OTP → mật khẩu mới; tách từng màn hình và DTO |
| OTP / email | Sáu ô số, paste/autofill/keyboard, countdown, resend sau 5 phút, mã hết hạn 15 phút, giới hạn sai ở Service; email HTML và plain text |
| Validation / lỗi | PasswordMatches dùng chung; lỗi flow đăng ký/reset được chuyển về GlobalExceptionHandler đúng bước |
| Simplification | Tách AccountSnapshot/PreparedRegistration khỏi Service; gộp CandidateRegistrationService; rút gọn cooldown và bỏ exception token cũ |
| Dashboard | Tách model Admin/Candidate/Recruitment; UI sáu role dùng layout chung và số liệu backend thực |
| Account Management | Tách internal/Candidate lifecycle; create/update nội bộ; deactivate/activate; giữ username/hash khi sửa |
| Department | CRUD theo schema có sẵn, delete bằng Inactive, reactivate, giới hạn chọn manager và gán phòng ban |
| API Monitoring / Health | Probe GET nội bộ + SQL SELECT 1, dữ liệu status/time; sửa template gọi fragment không tồn tại |
| Tích hợp main | Giữ code các thành viên khác, dùng lại layout/sidebar/topbar và các module Candidate đã có |

## 2. Login và Logout

- Trang Login tiếng Việt, đồng bộ màu/font/brand Mộc; có link Register và Forgot Password, nút hiện/ẩn mật khẩu, lỗi đăng nhập và trạng thái sau đăng ký/reset/logout.
- Login tiếp tục dùng **Spring Security + session/cookie**, **BCrypt**, **CSRF**. Không JWT.
- `DatabaseUserDetailsService.loadUserByUsername(...)` tìm username hoặc email. Active mới được xác thực; session guard kiểm tra lifecycle tài khoản khi truy cập.
- Logout dùng POST `/logout` và dialog HTML5 của topbar chung. Spring Security xử lý logout, không tạo Controller logout riêng.

| File | Thay đổi / mục đích |
|---|---|
| [src/main/java/com/group2/rms/auth/controller/AuthController.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/controller/AuthController.java>) | GET `/login` trả view Authentication; không xử lý password POST. |
| [src/main/java/com/group2/rms/core/config/SecurityConfig.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java>) | Form login, logout, public/protected routes, quyền Admin và session guard; nền xác thực dùng chung. |
| [src/main/java/com/group2/rms/core/security/DatabaseUserDetailsService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/security/DatabaseUserDetailsService.java>) | Nạp UserDetails từ UserRepository, authority và trạng thái tài khoản. |
| [src/main/java/com/group2/rms/core/security/AccountSessionGuardFilter.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/security/AccountSessionGuardFilter.java>) | Kiểm tra tài khoản còn Active đối với session đã đăng nhập. |
| [src/main/java/com/group2/rms/core/security/RoleAuthorities.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/security/RoleAuthorities.java>) | Nguồn role/authority dùng cho phân quyền và phân loại tài khoản nội bộ. |
| [src/main/resources/templates/auth/login.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/login.html>) | Form POST `/login`, nhãn tiếng Việt, lỗi và liên kết các bước Authentication. |
| [src/main/resources/templates/auth/fragments.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/fragments.html>) | Head/header/story/footer, lỗi và tiến trình dùng chung cho Auth. |
| [src/main/resources/static/css/auth.css](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/css/auth.css>) | Bố cục desktop/mobile, typography, focus, mật khẩu và OTP; scope Authentication. |
| [src/main/resources/static/js/auth.js](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/js/auth.js>) | Password toggle, submit state, focus lỗi, xử lý OTP và countdown. |
| [src/main/resources/templates/fragments/layout/topbar-user.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/fragments/layout/topbar-user.html>) | Dialog/logout/profile dùng chung được kế thừa từ code team; không nhận toàn bộ file này là code LinhDN. |

**Luồng:** GET `/login` → `AuthController` → form POST `/login` → Spring Security → `DatabaseUserDetailsService.loadUserByUsername()` → BCrypt → session → saved URL hoặc `/dashboard`. POST `/logout` → Spring Security → `/login?logout`.

## 3. Register — xác minh email trước khi tạo account

### 3.1 Thay đổi nghiệp vụ và UI

1. Người dùng nhập full name, username bắt buộc, email, password và confirm password.
2. Bean Validation và `PasswordMatches` kiểm tra form. `AccountManagementService.assertLoginIdentifiersAvailable(...)` kiểm tra trùng username/email và định danh chéo.
3. `RegistrationVerificationService.requestRegistration(...)` chuẩn hóa thông tin, BCrypt password, gửi OTP và lưu pending trong session. Bước này **chưa ghi User/Candidate**.
4. Redirect `/register/otp`; sáu ô OTP, email pending, countdown và POST resend có CSRF.
5. `verifyAndRegister(...)` kiểm tra pending/hạn/hash OTP/số lần sai; OTP đúng mới gọi writer transaction `createCandidateWithPasswordHash(...)`.
6. Sau khi writer hoàn tất, xóa pending rồi redirect `/login?registered`. Candidate role và Active do server chọn.

| File | Thay đổi / mục đích |
|---|---|
| [src/main/java/com/group2/rms/auth/controller/RegistrationController.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/controller/RegistrationController.java>) | Điều phối form, POST Register, GET/POST OTP, resend và dữ liệu countdown; không catch exception nghiệp vụ tại Controller. |
| [src/main/java/com/group2/rms/auth/service/RegistrationVerificationService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/RegistrationVerificationService.java>) | Hiện đã nhận trực tiếp RegisterAccountRequest, gộp chuẩn bị đăng ký, gửi mã, kiểm tra OTP và gọi writer. |
| [src/main/java/com/group2/rms/auth/dto/RegisterAccountRequest.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/dto/RegisterAccountRequest.java>) | Full name/username/email/password/confirm và Bean Validation; `@PasswordMatches`. |
| [src/main/java/com/group2/rms/auth/dto/VerifyRegistrationOtpRequest.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/dto/VerifyRegistrationOtpRequest.java>) | OTP bắt buộc và đúng sáu chữ số. |
| [src/main/java/com/group2/rms/auth/dto/PreparedRegistrationResponse.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/dto/PreparedRegistrationResponse.java>) | Record đã tách ra package auth/dto; chứa thông tin đã chuẩn hóa và password hash, không gửi DTO này ra form. |
| [src/main/java/com/group2/rms/auth/exception/RegistrationFlowException.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/exception/RegistrationFlowException.java>) | Step Register/OTP, field/code/retry và thông tin form an toàn cho handler. |
| [src/main/java/com/group2/rms/auth/service/RegistrationEmailSender.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/RegistrationEmailSender.java>) | Gửi email OTP đăng ký bằng JavaMailSender, MIME UTF-8 HTML/text; lỗi mail được chuyển thành lỗi flow. |
| [src/main/java/com/group2/rms/user/service/AccountManagementService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountManagementService.java>) | Writer User + Candidate trong transaction; nhận hash đã encode và không encode lại. |
| [src/main/resources/templates/auth/register.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/register.html>) | Form gửi mã xác minh, thông báo/lỗi, không render lại password. |
| [src/main/resources/templates/auth/register-otp.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/register-otp.html>) | Sáu ô OTP, verify/resend, countdown và form CSRF. |
| [src/main/resources/templates/auth/email/registration-otp.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/email/registration-otp.html>) | Email HTML tiếng Việt riêng cho đăng ký. |
| [src/main/resources/templates/auth/email/registration-otp.txt](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/email/registration-otp.txt>) | Email plain text tương ứng. |

### 3.2 Khác với nhật ký ngày 08/10

- `CandidateRegistrationService` đã được xóa; chuẩn bị đăng ký hiện nằm trong `RegistrationVerificationService`, ghi dữ liệu vẫn ở `AccountManagementService`.
- Không còn bước `RegisterAccountRequest.toCommand()` trong luồng hiện tại; Controller truyền form DTO trực tiếp vào Service.
- Cooldown hiện là `Map<String, Instant>` theo email, lưu RAM; không còn SendWindow/compute/synchronized như phiên bản cũ.
- `PreparedRegistrationResponse` nằm ngoài Service nhưng vẫn là **Java record**, không phải mutable class DTO.
- `PendingRegistration` vẫn là private class trong Service. Không khẳng định đã tách mọi class/record trong toàn project.

## 4. Forgot Password / OTP / Reset Password

### 4.1 Luồng hiện tại

1. GET `/forgot-password` → `PasswordRecoveryController.forgotForm()`.
2. POST email → `requestReset()` → `PasswordRecoveryService.requestReset()` → `PasswordResetService.findActiveAccount()`.
3. Email không tồn tại: lỗi tại Forgot, không đẩy sang OTP. Account không Active cũng bị từ chối.
4. Gửi OTP qua `PasswordResetEmailSender.sendOtp()`; session giữ AccountSnapshot + hash OTP + expiry.
5. GET `/reset-password/otp` → `otpForm()` + `addOtpContext()` cấp email pending và thời gian còn phải chờ.
6. POST OTP → `verifyOtp()` → `PasswordRecoveryService.verifyOtp()`; đúng mới đặt verified trong state phía server.
7. GET `/reset-password` chỉ mở khi `canResetPassword(session)` đúng; không nhận cờ verified từ trình duyệt.
8. POST password/confirm → validation → `completeReset()` → `PasswordResetService.changePassword()` trong transaction, kiểm tra snapshot/hạn/Active, encode BCrypt và ghi mật khẩu.
9. Xóa reset state sau thành công → `/login?reset`.

| File | Thay đổi / mục đích |
|---|---|
| [src/main/java/com/group2/rms/auth/controller/PasswordRecoveryController.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/controller/PasswordRecoveryController.java>) | Tách ba bước Forgot/OTP/Password; guard truy cập, validation, context countdown và redirect. |
| [src/main/java/com/group2/rms/auth/service/PasswordRecoveryService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordRecoveryService.java>) | OTP, expiry, five attempts, verified state, resend cooldown RAM theo UserId; code đã được rút gọn. |
| [src/main/java/com/group2/rms/auth/service/PasswordResetService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordResetService.java>) | Tìm Active account, kiểm tra snapshot còn đúng, khóa bản ghi khi ghi password và dùng BCrypt trong transaction. |
| [src/main/java/com/group2/rms/auth/dto/AccountSnapshotResponse.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/dto/AccountSnapshotResponse.java>) | Record UserId/email/passwordHash được tách khỏi Service sang auth/dto, dùng nội bộ cho reset. |
| [src/main/java/com/group2/rms/auth/dto/ForgotPasswordRequest.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/dto/ForgotPasswordRequest.java>) | Validation email đầu vào. |
| [src/main/java/com/group2/rms/auth/dto/VerifyPasswordResetOtpRequest.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/dto/VerifyPasswordResetOtpRequest.java>) | DTO OTP riêng, không gộp password mới. |
| [src/main/java/com/group2/rms/auth/dto/ResetPasswordRequest.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/dto/ResetPasswordRequest.java>) | Password/confirm 8–32 ký tự và PasswordMatches; không còn OTP trong form này. |
| [src/main/java/com/group2/rms/auth/exception/PasswordRecoveryFlowException.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/exception/PasswordRecoveryFlowException.java>) | Step FORGOT/OTP/PASSWORD, field/code/retry interval. |
| [src/main/java/com/group2/rms/auth/exception/PasswordRecoveryUnavailableException.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/exception/PasswordRecoveryUnavailableException.java>) | Phân biệt chưa đủ cấu hình gửi email. |
| [src/main/java/com/group2/rms/auth/exception/PasswordResetDeliveryException.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/exception/PasswordResetDeliveryException.java>) | Phân biệt lỗi vận chuyển email đã dự kiến. |
| [src/main/java/com/group2/rms/auth/service/PasswordResetEmailSender.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordResetEmailSender.java>) | Gửi OTP HTML/plain text tiếng Việt, UTF-8 và escape giá trị động. |
| [src/main/resources/templates/auth/forgot-password.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/forgot-password.html>) | Email + lỗi/cooldown, hướng dẫn flow OTP. |
| [src/main/resources/templates/auth/reset-password-otp.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/reset-password-otp.html>) | Sáu ô OTP, resend POST `/forgot-password`, countdown từ server. |
| [src/main/resources/templates/auth/reset-password.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/reset-password.html>) | Chỉ nhập password mới/confirm sau OTP đúng. |
| [src/main/resources/templates/auth/email/password-reset-otp.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/email/password-reset-otp.html>) | Email HTML OTP reset, hạn và hướng dẫn. |
| [src/main/resources/templates/auth/email/password-reset-otp.txt](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/email/password-reset-otp.txt>) | Bản plain text email reset. |

### 4.2 Ràng buộc được giữ

- OTP sáu số; lưu hash bằng PasswordEncoder; hạn **15 phút**.
- Gửi lại sau **5 phút**; reload đọc thời gian server, không khởi tạo lại cooldown từ đầu.
- Tối đa **5 lần OTP sai được Service xử lý**: hủy yêu cầu, không khóa User.
- Cooldown và state lưu RAM/session; restart mất state. Phiên bản đơn giản hiện tại không được coi là đã kiểm chứng chống mọi request đồng thời.
- Password hash/email/Active của tài khoản bị thay đổi sẽ làm snapshot reset không còn hợp lệ.
- Không thêm bảng OTP/reset token. Không ghi password plaintext.

## 5. Validation và xử lý lỗi

| File | Thay đổi / mục đích |
|---|---|
| [src/main/java/com/group2/rms/core/validation/PasswordMatches.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/validation/PasswordMatches.java>) | Annotation validation cấp DTO cho password/confirm, có tên field cấu hình. |
| [src/main/java/com/group2/rms/core/validation/PasswordMatchesValidator.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/validation/PasswordMatchesValidator.java>) | So sánh hai trường, gắn violation vào confirmPassword; field trống để NotBlank xử lý. |
| [src/main/java/com/group2/rms/user/dto/CreateAccountRequest.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/dto/CreateAccountRequest.java>) | Đã gắn PasswordMatches cùng RegisterAccountRequest và ResetPasswordRequest; quy tắc password Create. |
| [src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java>) | Handler riêng RegistrationFlowException, PasswordRecoveryFlowException, lỗi mail, Department và DatabaseHealth; đưa lỗi flow về đúng bước thay vì trang 500 chung. |

Controller Auth chỉ xử lý `BindingResult` từ Bean Validation; exception nghiệp vụ đi tới handler chung. Đây là thống kê các luồng Auth đã đối chiếu, không khẳng định mọi Controller trong các module khác đã được dọn try-catch.

## 6. Dashboard — model riêng và giao diện theo role

### 6.1 Backend / DTO

| File | Thay đổi / mục đích |
|---|---|
| [src/main/java/com/group2/rms/dashboard/DashboardController.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/DashboardController.java>) | Switch view: Admin → admin; Candidate → candidate; HR/HM/Director/Interviewer → index; thêm activeMenu/dashboard và pageLang/vi. |
| [src/main/java/com/group2/rms/dashboard/DashboardService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/DashboardService.java>) | Giữ switch sáu role; trả DTO riêng, dữ liệu theo scope. Director gọi `director(user, now)` và có số liệu phỏng vấn cá nhân. |
| [src/main/java/com/group2/rms/dashboard/DashboardMetricsRepository.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/DashboardMetricsRepository.java>) | Query số liệu Account/Department/recruitment và dữ liệu Candidate qua UserId; không metrics fake. |
| [src/main/java/com/group2/rms/dashboard/dto/DashboardResponse.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/dto/DashboardResponse.java>) | Interface chung nhỏ: roleName, fullName, scope; thay DTO gộp cũ. |
| [src/main/java/com/group2/rms/dashboard/dto/AdminDashboardResponse.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/dto/AdminDashboardResponse.java>) | Account summaries, Department summary, API breakdown và shortcut. |
| [src/main/java/com/group2/rms/dashboard/dto/CandidateDashboardResponse.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/dto/CandidateDashboardResponse.java>) | Metrics, application/offer statuses, recent applications, interviews, offers và profile. |
| [src/main/java/com/group2/rms/dashboard/dto/RecruitmentDashboardResponse.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/dto/RecruitmentDashboardResponse.java>) | Model chung phù hợp HR/HM/Director/Interviewer; metrics, breakdowns, activities. |
| [src/main/java/com/group2/rms/dashboard/dto/DashboardMetricResponse.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/dto/DashboardMetricResponse.java>) | DTO metric title/value/detail. |
| [src/main/java/com/group2/rms/dashboard/dto/DashboardBreakdownResponse.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/dto/DashboardBreakdownResponse.java>) | DTO nhóm status/count. |

### 6.2 Frontend

| File | Thay đổi / mục đích |
|---|---|
| [src/main/resources/templates/dashboard/admin.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/dashboard/admin.html>) | Trang Admin hoàn chỉnh: nội bộ/ứng viên, phòng ban, API và tools; đọc đúng totalCount/activeCount/inactiveCount của Department. |
| [src/main/resources/templates/dashboard/candidate.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/dashboard/candidate.html>) | Trang độc lập đọc DTO phẳng; hồ sơ gần đây, lịch, offer details, link thông báo/profile/portal. |
| [src/main/resources/templates/dashboard/index.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/dashboard/index.html>) | Trang Recruitment theo role, CTA đúng quyền, breakdown HR và hoạt động Director; nhãn tiếng Việt. |
| [src/main/resources/templates/dashboard/fragments.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/dashboard/fragments.html>) | Fragment feature metrics/statuses/statusRows; statCard tái sử dụng catalog; dịch nhãn tĩnh và giữ DB text. |
| [src/main/resources/static/css/dashboard.css](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/css/dashboard.css>) | Thay CSS trùng/lỗi thời bằng layout có tiền tố dash-, dùng token và catalog; không override shell. |
| [src/main/resources/static/js/candidate-dashboard.js](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/js/candidate-dashboard.js>) | Giữ lọc không dấu/status/aria-live/clear contextual; bỏ handler logout riêng, dùng dialog shared. |

| Role | Nội dung hiện tại |
|---|---|
| Admin | Hai nhóm Account, Active/Inactive/Blocked, Department lifecycle, trạng thái API đã ghi nhận và shortcuts |
| HR | Tin đang tuyển, hồ sơ, tiến trình tuyển dụng, lịch và hồ sơ chờ duyệt |
| Hiring Manager | Requisition riêng/phòng ban, pending, ứng viên, lịch, offer cần xử lý; không link `/offers` khi role bị SecurityConfig chặn |
| Director | Hàng đợi requisition/offer toàn hệ thống, quyết định cá nhân; lịch và ứng viên được phân công |
| Interviewer | Lịch, ứng viên được phân công và đánh giá còn chờ |
| Candidate | Chỉ hồ sơ/lịch/offer/profile của mình; không AI score, nhận xét/phỏng vấn/đàm phán nội bộ |

**Luồng:** `/dashboard` → Security/session guard → `DashboardController.dashboard()` → `DashboardService.forUsername()` → hàm theo role → repository/monitoring → DTO → view → `layout/internal`.

Candidate hiển thị tối đa 20 applications, 10 lịch sắp tới và 20 offer. Bộ lọc là cục bộ trên dữ liệu đã nạp. Link tới `/portal/applications`, `/portal/interviews`, `/notifications`, `/profile` dùng module đã có của team. Offer trong Dashboard chỉ xem chi tiết, chưa có Accept/Decline/Negotiate.

**Lỗi còn giữ:** toàn trang ở 375px có overflow topbar và sidebar vẫn hiện khi hidden. Người dùng chọn giữ nguyên khung chung; không sửa workspace.css/topbar/layout. Các file trong `docs/architecture/UI/` không được cập nhật bởi đợt Dashboard; phần từng sửa UI_MIGRATION đã hoàn lại.

## 7. Account Management và Department

### 7.1 Account

- Internal và Candidate là hai lifecycle; không đổi Candidate thành nhân viên. Nhân viên được tuyển có internal account mới.
- List/filter/page từng nhóm theo role hiện tại, không phân nhóm bằng lịch sử Candidate profile.
- Create nội bộ: role nội bộ, default Active, username/email unique, BCrypt, confirm password.
- Edit: username read-only, không ghi lại password hash; email unique loại trừ chính account.
- Delete = deactivation Inactive, giữ record/history; có Activate lại Inactive. Blocked không được bật lại bằng action Activate đơn giản.
- Không gán mới/chuyển sang Department Inactive; cho phép giữ Department cũ khi edit.

| File | Thay đổi / mục đích |
|---|---|
| [src/main/java/com/group2/rms/user/controller/AccountController.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/controller/AccountController.java>) | List/create/edit/deactivate/activate tài khoản nội bộ. |
| [src/main/java/com/group2/rms/user/controller/CandidateAccountController.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/controller/CandidateAccountController.java>) | List/deactivate/activate Candidate riêng, không đổi role sang nội bộ. |
| [src/main/java/com/group2/rms/user/service/AccountListService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountListService.java>) | Search/filter/page theo lifecycle; activate/deactivate kiểm tra đúng nhóm. |
| [src/main/java/com/group2/rms/user/service/AccountManagementService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountManagementService.java>) | Create/update nội bộ, uniqueness, Candidate writer và policy Department. |
| [src/main/java/com/group2/rms/user/dto/UpdateAccountRequest.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/dto/UpdateAccountRequest.java>) | Form edit không có username/password replacement. |
| [src/main/java/com/group2/rms/user/repository/UserRepository.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/repository/UserRepository.java>) | Lookup/unique identifiers, query manager và bản ghi reset cần khóa. |
| [src/main/resources/templates/admin/accounts/list.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/admin/accounts/list.html>) | Danh sách internal account và action status. |
| [src/main/resources/templates/admin/accounts/candidates.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/admin/accounts/candidates.html>) | Danh sách Candidate account riêng. |
| [src/main/resources/templates/admin/accounts/form.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/admin/accounts/form.html>) | Create/Edit, trường/role/Department theo lifecycle. |
| [src/main/resources/static/css/account-list.css](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/css/account-list.css>) | Table/filter/action của account và Department theo ghi nhận lịch sử. |
| [src/main/resources/static/css/account-form.css](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/css/account-form.css>) | Style form Account. |
| [src/main/resources/static/js/account-list.js](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/js/account-list.js>) | Tương tác bộ lọc; confirmation chuyển sang dùng chung. |
| [src/main/resources/static/js/account-form.js](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/js/account-form.js>) | Tương tác form Account. |

### 7.2 Department

- Có list/search/status/page, Create, Update, deactivate và activate.
- DepartmentName unique, tối đa 100 ký tự; mapping DepartmentStatus theo cột có sẵn.
- Manager optional; nếu chọn phải Active/internal/thuộc đúng Department.
- Vô hiệu hóa giữ User/requisition liên quan; không hard delete.

| File | Thay đổi / mục đích |
|---|---|
| [src/main/java/com/group2/rms/user/entity/Department.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/entity/Department.java>) | Mapping DepartmentStatus/default Active theo schema. |
| [src/main/java/com/group2/rms/user/repository/DepartmentRepository.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/repository/DepartmentRepository.java>) | Search/page, uniqueness và fetch manager. |
| [src/main/java/com/group2/rms/user/controller/DepartmentController.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/controller/DepartmentController.java>) | MVC list/form/create/update/activate/deactivate. |
| [src/main/java/com/group2/rms/user/service/DepartmentService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/DepartmentService.java>) | Validation tên/manager, transaction và chuyển trạng thái. |
| [src/main/java/com/group2/rms/user/dto/DepartmentRequest.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/dto/DepartmentRequest.java>) | Form name/managerId, validation. |
| [src/main/java/com/group2/rms/user/dto/DepartmentResponse.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/dto/DepartmentResponse.java>) | Model list/edit an toàn. |
| [src/main/java/com/group2/rms/user/dto/DepartmentManagerResponse.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/dto/DepartmentManagerResponse.java>) | Danh sách manager hợp lệ. |
| [src/main/java/com/group2/rms/user/exception/DepartmentFieldException.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/exception/DepartmentFieldException.java>) | Lỗi field + dữ liệu để handler trả đúng form. |
| [src/main/resources/templates/admin/departments/list.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/admin/departments/list.html>) | List/filter/pagination/status actions. |
| [src/main/resources/templates/admin/departments/form.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/admin/departments/form.html>) | Create/Edit và chọn manager. |
| [src/main/resources/templates/fragments/status-actions.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/fragments/status-actions.html>) | Confirmation + POST form dùng chung có CSRF. |
| [src/main/resources/static/js/status-confirmation.js](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/js/status-confirmation.js>) | Dialog xác nhận status; hủy/Escape/focus và submit form gốc. |

## 8. API Monitoring và Health

| File | Thay đổi / mục đích |
|---|---|
| [src/main/java/com/group2/rms/admin/dto/MonitorRowResponse.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/admin/dto/MonitorRowResponse.java>) | Rút model về sáu trường: id/name/category/status/responseTimeMs/lastCheckedAt; presentation ở view. |
| [src/main/java/com/group2/rms/admin/dto/ProbeOutcomeResponse.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/admin/dto/ProbeOutcomeResponse.java>) | Kết quả gửi request: success/detail/elapsedMs. |
| [src/main/java/com/group2/rms/admin/service/ApiMonitoringService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/admin/service/ApiMonitoringService.java>) | Một GET đến endpoint nội bộ cố định, đo thời gian, lưu kết quả trong RAM; rows gồm nội bộ/AI/email. |
| [src/main/java/com/group2/rms/admin/service/HttpProbeTransport.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/admin/service/HttpProbeTransport.java>) | Hợp đồng transport cho probe nội bộ. |
| [src/main/java/com/group2/rms/admin/service/LoopbackHttpProbeTransport.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/admin/service/LoopbackHttpProbeTransport.java>) | Giới hạn target loopback/path cố định; gửi session hiện có. |
| [src/main/java/com/group2/rms/admin/controller/ApiMonitoringController.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/admin/controller/ApiMonitoringController.java>) | GET bảng và POST probe, flash kết quả; CSRF/role áp ở Security. |
| [src/main/java/com/group2/rms/admin/controller/HealthController.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/admin/controller/HealthController.java>) | Gọi HealthService và trả JSON UP, no-store. |
| [src/main/java/com/group2/rms/admin/service/HealthService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/admin/service/HealthService.java>) | Bean Service, SELECT 1; lỗi DB chuyển thành DatabaseHealthException. |
| [src/main/java/com/group2/rms/admin/exception/DatabaseHealthException.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/admin/exception/DatabaseHealthException.java>) | Global Handler trả HTTP 503 và JSON DOWN. |
| [src/main/resources/templates/admin/api-monitoring/index.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/admin/api-monitoring/index.html>) | Đã sửa sau hướng dẫn: bỏ fragment monitoring-status không tồn tại, dùng th:switch trực tiếp cho Status. |
| [src/main/resources/static/css/api-monitoring.css](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/css/api-monitoring.css>) | Style table/metadata của trang Monitoring. |

**Fix mới nhất đã thấy trong source ngày 12/10:** ô Status dùng `OPERATIONAL → Hoạt động`, `FAILED → Có lỗi`, `Not check → Chưa kiểm tra`, `Unconfigured → Chưa cấu hình`. Không còn lời gọi `fragments/monitoring-status :: label(...)`. Chưa chạy render/browser/probe lại sau sửa này; không ghi PASS chỉ vì đọc thấy code.

Internal probe có kết quả HTTP 2xx là thành công; lỗi/redirect/mất response là thất bại. Health DB giữ hợp đồng 200 UP hoặc 503 DOWN, Cache-Control no-store. AI/email rows vẫn chưa có probe được cấu hình trong Service hiện tại; không coi Unconfigured là kết quả kiểm tra thực tế.

## 9. Những file đã loại bỏ / thay vị trí

| File cũ | Trạng thái và thay thế đã đối chiếu Git/source |
|---|---|
| `auth/service/CandidateRegistrationService.java` | Đã xóa trong e26c0c9; chuẩn bị đăng ký ở RegistrationVerificationService, writer ở AccountManagementService |
| `auth/exception/InvalidResetTokenException.java` | Đã xóa trong e26c0c9; flow reset dùng PasswordRecoveryFlowException, không link token cũ |
| `dashboard/DashboardResponse.java` | DTO gộp cũ đã xóa trong e26c0c9; thay bởi interface và các DTO trong dashboard/dto |
| Nested AccountSnapshot | Tách thành auth/dto/AccountSnapshotResponse.java; vẫn record |
| Nested PreparedRegistration | Tách thành auth/dto/PreparedRegistrationResponse.java; vẫn record |

Không diễn giải việc bỏ một block CSS/JS/hàm là xóa toàn bộ file. Những source chưa xác minh dependency không được thống kê là đã xóa.

## 10. Code từ main và giao diện Mộc

- 11/10: nhật ký ghi nhận lấy 30 commit main theo fast-forward, giữ PasswordRecoveryService/PasswordResetService local bằng hash.
- 12/10: commit e584624 là merge với nhiều file của các thành viên khác; không lấy toàn bộ diff 135 file để tính đóng góp LinhDN.
- Sidebar/topbar/layout/catalog mới được tái sử dụng; dialog logout, menu Interview/Offer/Profile và code team được bảo toàn theo hướng tích hợp.
- Homepage hiện tại của một công ty dùng `candidate/landing.html` và CareerPortalController. Đây là tham chiếu thiết kế cho Auth/Dashboard, không sửa homepage trong đợt Dashboard.
- My Applications/withdraw, Candidate Interview portal, notifications, Interview evaluation và Director offer approval là các phần tích hợp từ team. Dashboard chỉ nối vào route đã có; không nhận phần implementation này là mới viết trong đợt Dashboard.

## 11. Kết quả kiểm tra — ghi theo từng phiên bản

| Đợt | Kết quả có trong nhật ký | Giới hạn |
|---|---|---|
| Auth UI/email 08/10 | Compile PASS; Node syntax PASS; 171 MVC/template/MIME checks, 74 Recovery runtime checks, 226 browser checks PASS | Phiên bản lúc 08/10, trước một số thay đổi Service/DTO mới; SMTP/DB thực chưa xác minh |
| Register OTP 08/10 | 9 Maven test, 91 runtime checks, 125 MVC/Thymeleaf/MIME checks, 88 browser checks PASS | Các adapter/repository kiểm thử; không chứng minh full SQL Server/SMTP E2E |
| Dashboard UI 12/10 | Compile PASS, 84 render checks PASS, 21 tổ hợp role/width tại 1440/1024/768 PASS; lọc/keyboard/dialog PASS | Service fixture; 375px FAIL ở cả 7 trạng thái do shared shell; chưa full app + DB |
| API Status sau sửa 12/10 | Đã đối chiếu source loại bỏ reference fragment thiếu | NOT TESTED render/browser/probe sau sửa |
| Toàn bộ suite hiện tại | Chưa chạy lại trong đợt tổng hợp tài liệu | Không ghi full test PASS; một số test cũ hiện bị comment |

Trong nhật ký Register 08/10 có lượt 22 test rộng với lỗi ngoài scope Admin/API. Kết quả 9 test xác nhận sau đó không được dùng để tuyên bố toàn suite xanh. Các bộ test nhiều module thay đổi khi tích hợp main cũng chưa được chạy lại trong đợt tạo tài liệu này.

## 12. Checklist test nhanh để nối tiếp

| Luồng | Các bước | Mong đợi |
|---|---|---|
| Login | Guest mở Dashboard → login Active bằng username và email; thử mật khẩu sai | Guest về Login, Active vào đúng Dashboard; sai hiện lỗi, không lộ password |
| Register validation | Thiếu field, email sai, password/confirm lệch, username/email trùng | Không tạo account, không gửi OTP khi form không hợp lệ; lỗi đúng field |
| Register OTP | Gửi form hợp lệ → kiểm tra chưa có User/Candidate → nhập OTP đúng | Tạo Active Candidate cùng profile trong transaction; về Login |
| OTP policy | Mã sai, hết 15 phút, resend trước/sau 5 phút, reload; dùng hai session | Server kiểm soát; lỗi đúng bước; code mới thay code cũ trong state; không bỏ qua OTP |
| Forgot / Reset | Email không tồn tại → email Active → OTP → password/confirm mới | Unknown email ở lại Forgot; chưa verify không vào password; đổi hash thành công và login bằng mật khẩu mới |
| Dashboard | Login từng sáu role; đối chiếu số liệu DB; Candidate A/B | Đúng DTO/view/scope; không lộ dữ liệu Candidate khác hoặc nội bộ |
| Candidate UI | Search không dấu, status, zero results, clear; mở offer details | Count/empty/focus đúng, giữ metadata, không fake action |
| Account | Create/edit internal, uniqueness cùng account, deactivate/reactivate | Username/hash giữ khi edit; inactive không login; không đổi Candidate sang internal |
| Department | Create/duplicate/edit/manager/deactivate/reactivate; gán mới vào Inactive | Policy đúng; giữ history và liên kết cũ, chặn gán mới |
| Monitoring | Reload bảng → đủ 3 dòng → Send GET | Status không lỗi fragment; 2xx/failed, response time, last check và flash cập nhật |
| Logout | Mở dialog → hủy/Escape → xác nhận | Hủy giữ session/focus; xác nhận POST CSRF, session hết, về Login |
| Responsive | 1440/1024/768/375px, tên/email dài, keyboard | Ghi riêng lỗi shell 375px còn giữ; không đánh dấu mobile toàn trang PASS |

Checklist này dùng để test sau các chỉnh sửa hiện tại, chưa được chạy lại trong phiên tổng hợp. Các bước chi tiết từng màn hình nằm trong tài liệu nguồn bên dưới.

## 13. Tài liệu nguồn

- [docs/members/linhdn/DAILY_CHANGE_SUMMARY_2026-10-08.md](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/DAILY_CHANGE_SUMMARY_2026-10-08.md>) — inventory Auth/UI/email và phần người dùng sửa trước/sau đợt trợ giúp.
- [docs/members/linhdn/AUTH_UI_EMAIL_UPDATE_2026-10-08.md](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/AUTH_UI_EMAIL_UPDATE_2026-10-08.md>) — luồng và test Auth/OTP; một số tên hàm/cơ chế cũ cần đọc cùng mục 3.2 ở tài liệu này.
- [docs/members/linhdn/DAILY_CHANGE_SUMMARY_2026-10-11.md](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/DAILY_CHANGE_SUMMARY_2026-10-11.md>) — cập nhật main và bảo toàn local.
- [docs/members/linhdn/DASHBOARD_UI_UPDATE_2026-10-12.md](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/DASHBOARD_UI_UPDATE_2026-10-12.md>) — tám trạng thái render, sáu role, test và lỗi mobile giữ nguyên.
- [docs/members/linhdn/tmp/update architecture/2026-10-04-admin-departments-activation.md](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/tmp/update architecture/2026-10-04-admin-departments-activation.md>) — quyết định/triển khai Account và Department.
- [docs/members/linhdn/tmp/update architecture/2026-10-02-den-2026-10-04-tong-ket-fix.md](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/tmp/update architecture/2026-10-02-den-2026-10-04-tong-ket-fix.md>) — lịch sử architecture/Candidate/Admin; tên package/view cũ không dùng thay cho source hiện tại.

**Phiên tạo tài liệu này:** chỉ tạo file MD tại docs/members/linhdn; không sửa source, test, DB, cấu hình, UI docs, workbook LOC hoặc Git history. Không đưa password/OTP/SMTP secret vào tài liệu. Không tính lại LOC vì phạm vi yêu cầu là tổng hợp thay đổi.

