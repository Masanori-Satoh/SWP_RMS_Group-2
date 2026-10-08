# Tổng kết thay đổi ngày 08/10/2026

## 1. Phạm vi tổng kết

Tổng kết công việc Authentication trong phiên hôm nay: frontend Login/Register/Forgot Password/OTP/Reset Password, email OTP, bổ sung xác minh email khi đăng ký và các phần backend/test liên quan.

File này nằm cùng cấp với [Tính Loc.xlsx](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/Tính Loc.xlsx>). Nguồn đối chiếu: nhật ký thực hiện, git worktree, thời gian sửa file và log kiểm tra. Git status chỉ phản ánh khác biệt với commit, không tự chứng minh ai viết toàn bộ một file.

| Nhóm | Số file duy nhất | Ghi chú |
|---|---:|---|
| Source/test có chỉnh sửa hoặc tạo mới trong hai đợt assistant thực hiện | **27** | 15 file đã có được sửa, 12 file mới; file sửa nhiều lượt chỉ tính một lần |
| Source/backend bạn sửa hoặc đã có trước đợt assistant | **4** | Liệt kê riêng ở mục 7; không nhận là edit của assistant |
| Tổng source/test được thống kê | **31** | Không bao gồm tài liệu, workbook, log, screenshot, helper hoặc build output |
| Source bị xóa | **0** | Bỏ hàm cũ không có nghĩa xóa cả file |

## 2. Những chức năng đã thay đổi

| Chức năng | Thay đổi chính |
|---|---|
| Login | Giao diện tiếng Việt, typography/màu/brand khớp homepage Mộc, hiện/ẩn mật khẩu, thông báo lỗi và trạng thái đăng nhập |
| Register | Form thông tin tài khoản chuyển sang bước gửi mã; validate đầy đủ và kiểm tra trùng trước khi gửi email |
| Xác minh đăng ký | Trang mới có sáu ô OTP; xác minh thành công mới tạo User Active và Candidate profile, sau đó về Login |
| Forgot Password | Giao diện bước nhập email, giải thích cách nhận OTP, thông báo lỗi/cooldown |
| OTP quên mật khẩu | Sáu ô số, dán/autofill, keyboard, countdown do server cấp, gửi lại sau năm phút |
| Reset Password | Trang riêng chỉ nhập mật khẩu mới và xác nhận, phù hợp flow OTP → mật khẩu mới |
| Email | HTML + plain text tiếng Việt, subject và trình bày theo brand Mộc; email đăng ký và reset riêng |
| UI dùng chung | Fragment Authentication, CSS có scope, JavaScript dùng chung cho thao tác form và OTP |
| Backend đăng ký | Pending registration trong session, mật khẩu/OTP dạng hash, giới hạn sai/hết hạn/cooldown, exception về Global Handler |

Hai luồng hiện tại:

1. **Đăng ký:** `/register` → validate/kiểm tra trùng → gửi OTP → `/register/otp` → OTP đúng → tạo User + Candidate → `/login?registered`.
2. **Quên mật khẩu:** `/forgot-password` → gửi OTP → `/reset-password/otp` → OTP đúng → `/reset-password` → lưu mật khẩu mới → `/login?reset`.

Đăng nhập tiếp tục dùng Spring Security form/session; không JWT. Không thêm/sửa bảng database, không chạy SQL hoặc chuyển dữ liệu trong hai đợt này.

## 3. Frontend — 9 file

### 3.1 Các màn hình

| File | Loại | Đã sửa / thêm gì | Lý do |
|---|---|---|---|
| [templates/auth/login.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/login.html>) | Sửa | Nhãn/hướng dẫn tiếng Việt, username hoặc email, password toggle, liên kết Register/Forgot và các trạng thái success/error | Đồng bộ homepage và làm rõ thao tác đăng nhập; giữ POST `/login` do Spring Security xử lý |
| [templates/auth/register.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/register.html>) | Sửa | Form họ tên/username/email/password/confirm, lỗi từng trường, hai bước đăng ký, CTA **Gửi mã xác minh**, countdown khi server từ chối gửi sớm | Không còn báo tạo tài khoản ngay ở bước gửi thông tin; mật khẩu không render lại khi lỗi |
| [templates/auth/forgot-password.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/forgot-password.html>) | Sửa | Trang nhập email tiếng Việt, hướng dẫn OTP, lỗi và countdown | Khớp luồng reset bằng OTP, không bắt mở link trong email |
| [templates/auth/reset-password.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/reset-password.html>) | Sửa | Chỉ nhập password mới và confirm; trình bày bước cuối, errors và password toggle | Tách khỏi bước nhập OTP và UI token/link cũ |
| [templates/auth/reset-password-otp.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/reset-password-otp.html>) | Tạo mới | Sáu ô OTP, một giá trị `otp` gửi backend, email đang reset, countdown, form resend dùng POST `/forgot-password` và CSRF | Đúng flow ba bước; resend được Service kiểm soát, no-JS vẫn có input gốc |
| [templates/auth/register-otp.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/register-otp.html>) | Tạo mới | Sáu ô OTP đăng ký, CTA **Xác minh và tạo tài khoản**, countdown, POST verify/resend có CSRF | Chỉ tạo tài khoản sau chứng minh quyền sở hữu email |

### 3.2 Thành phần dùng chung

| File | Loại | Đã sửa / thêm gì | Lý do |
|---|---|---|---|
| [templates/auth/fragments.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/fragments.html>) | Tạo mới, tinh chỉnh nhiều lượt | Head/header/story/footer, error summary, dịch thông báo, tiến trình recovery ba bước và đăng ký hai bước | Tái sử dụng layout Authentication, giữ brand/global assets; tránh lặp markup giữa các form |
| [static/css/auth.css](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/css/auth.css>) | Sửa | Bố cục hai cột desktop/một cột nhỏ, mint/forest, serif/font khớp homepage, spacing, focus/contrast/reduced motion, OTP cells và riêng hai bước Register | Đồng bộ frontend; selector scope `.auth-page`, không đổi giao diện module khác |
| [static/js/auth.js](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/js/auth.js>) | Tạo mới | Hiện/ẩn password, busy submit, focus error summary, countdown; nhập/dán/autofill OTP, Backspace/mũi tên, ghép một field OTP | Progressive enhancement; server vẫn xác minh OTP và quyết định cooldown, không auto-submit OTP |

## 4. Email — 6 file

| File | Loại | Đã sửa / thêm gì | Lý do |
|---|---|---|---|
| [auth/email/password-reset-otp.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/email/password-reset-otp.html>) | Tạo mới | Email HTML tiếng Việt, bố cục bảng/inline CSS, brand, mã OTP, hạn 15 phút và hướng dẫn bảo mật | Email đọc được ở client mail, không phụ thuộc JS hoặc asset website |
| [auth/email/password-reset-otp.txt](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/email/password-reset-otp.txt>) | Tạo mới | Bản plain text tương ứng | Hỗ trợ client không render HTML |
| [auth/email/registration-otp.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/email/registration-otp.html>) | Tạo mới | Nội dung xác minh đăng ký riêng, OTP sáu số, tiếng Việt, hạn 15 phút | Phân biệt mục đích đăng ký và khôi phục mật khẩu |
| [auth/email/registration-otp.txt](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/email/registration-otp.txt>) | Tạo mới | Bản plain text email đăng ký | Đồng nhất hai định dạng |
| [auth/service/PasswordResetEmailSender.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordResetEmailSender.java>) | Sửa phần trình bày | Chuyển sang MIME UTF-8 HTML/text, subject tiếng Việt, đọc template và escape dữ liệu động | Giữ constructor, cấu hình SMTP/from/to và hợp đồng gửi/lỗi hiện có; file đã có WIP trước đợt UI |
| [auth/service/RegistrationEmailSender.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/RegistrationEmailSender.java>) | Tạo mới | Dùng JavaMailSender/config hiện có, MIME UTF-8 HTML/text cho OTP đăng ký, chuyển lỗi mail dự kiến thành RegistrationFlowException | Không dùng lại nội dung reset cho đăng ký; không log OTP hoặc thông tin SMTP |

## 5. Backend Authentication và thành phần chung — 10 file

### 5.1 Register — 5 file

| File | Loại | Đã sửa / thêm gì | Lý do |
|---|---|---|---|
| [auth/controller/RegistrationController.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/controller/RegistrationController.java>) | Sửa | Gọi verification service; thêm GET/POST `/register/otp`, POST `/register/otp/resend`; model email/countdown; bỏ catch tự xử lý lỗi nghiệp vụ | Controller điều phối request, không tạo account ngay hoặc nuốt exception |
| [auth/service/CandidateRegistrationService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/CandidateRegistrationService.java>) | Sửa | `prepare()` validate/kiểm tra trùng/BCrypt; `registerVerified()` ghi transaction; bỏ hàm `register(RegisterCommand)` tạo ngay; record không in dữ liệu nhạy cảm | Giữ password hash qua bước OTP, chỉ ghi sau xác minh, không hash lần hai |
| [auth/service/RegistrationVerificationService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/RegistrationVerificationService.java>) | Tạo mới | Pending session, SecureRandom OTP/hash, hạn 15 phút, tối đa năm sai, send window atomic năm phút theo email, resend và verify/create | Kiểm soát toàn bộ flow xác minh phía server; chưa có OTP đúng thì chưa có User/Candidate |
| [auth/dto/VerifyRegistrationOtpRequest.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/dto/VerifyRegistrationOtpRequest.java>) | Tạo mới | `@NotBlank`, `@Pattern` cho OTP sáu số | DTO OTP riêng, không nhận password hash/role từ client |
| [auth/exception/RegistrationFlowException.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/exception/RegistrationFlowException.java>) | Tạo mới | Step Register/OTP, field lỗi, retry interval và metadata đăng ký an toàn | Handler biết trả đúng bước, không mang mật khẩu/OTP vào flash |

### 5.2 Recovery — 2 file bổ sung trong đợt UI

| File | Loại | Phần assistant bổ sung | Phần đã có được giữ |
|---|---|---|---|
| [auth/controller/PasswordRecoveryController.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/controller/PasswordRecoveryController.java>) | Sửa tối thiểu sau duyệt | `addOtpContext()` và gọi ở GET OTP/POST lỗi định dạng để cấp `pendingResetEmail`, `retryAfterSeconds` | Flow Forgot → OTP → Password do bạn sửa; resend dùng endpoint `/forgot-password`, không thêm catch |
| [auth/service/PasswordRecoveryService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordRecoveryService.java>) | Sửa tối thiểu sau duyệt | `getPendingResetEmail(HttpSession)` đọc challenge hợp lệ dưới session mutex | Cơ chế OTP/cooldown/verify/reset đang có; không nhận toàn bộ Service là code mới của assistant |

### 5.3 Thành phần chung — 3 file, phục vụ Register

| File | Loại | Phần sửa | Lý do |
|---|---|---|---|
| [user/service/AccountManagementService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountManagementService.java>) | Sửa | `assertLoginIdentifiersAvailable()`, `createCandidateWithPasswordHash()`, tách `persistAccount()` | Dùng lại kiểm tra định danh và writer User/Candidate; Admin create vẫn encode raw password như cũ |
| [core/config/SecurityConfig.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java>) | Sửa một route matcher | Thêm `/register/**` vào nhóm public routes | Guest vào được bước OTP; CSRF và quyền module khác giữ nguyên |
| [core/exception/GlobalExceptionHandler.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java>) | Sửa phần Register; có WIP Recovery của bạn | Thêm `handleRegistrationFlowException()`, redirect 303 + flash form/BindingResult sạch, chỉ log mã lỗi | Tuân thủ handler tập trung; phần Recovery có sẵn/chỉnh đồng thời được giữ, không nhận là edit Register |

## 6. Test hiện có — 2 file

| File | Loại | Phần sửa |
|---|---|---|
| [SecurityFlowTests.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/test/java/com/group2/rms/SecurityFlowTests.java>) | Sửa phần Register | Mock RegistrationVerificationService; kỳ vọng bước đầu redirect `/register/otp`; kiểm tra route Guest/CSRF verify/resend và redirect sau OTP; assertion error summary Register dùng `data-auth-errors`; invalid input không gọi verification |
| [service/AuthenticationDatabaseTests.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/test/java/com/group2/rms/service/AuthenticationDatabaseTests.java>) | Sửa hợp đồng writer Register | Test/fixture chuyển sang `prepare()` → `registerVerified()`, đổi tên test Active Candidate. Không chạy bài DB này trong lượt xác nhận chín test; không comment thêm các bài reset đã bị comment trước đó |

Không tạo file source test mới. Các helper kiểm tra runtime/render/browser nằm trong thư mục tạm ở `docs/members/linhdn/`.

## 7. Backend bạn sửa / đã có trước đợt assistant — 4 file

Các file sau cũng được thống kê để nối tiếp, nhưng **không phải các file assistant sửa trong hai đợt UI/Register**. Thời gian dưới đây là LastWriteTime đối chiếu ngày 08/10/2026, không phải bằng chứng về tác giả từng dòng.

| File | Thời gian | Nội dung hiện tại / thay đổi liên quan |
|---|---|---|
| [auth/dto/ResetPasswordRequest.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/dto/ResetPasswordRequest.java>) | 16:55:57 | DTO password mới/confirm với PasswordMatches và độ dài 8–32; không còn gộp field OTP vào bước nhập password |
| [auth/dto/VerifyPasswordResetOtpRequest.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/dto/VerifyPasswordResetOtpRequest.java>) | 17:04:00 | DTO riêng cho OTP reset, required và đúng sáu chữ số; hiện untracked trong worktree |
| [auth/exception/PasswordRecoveryFlowException.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/exception/PasswordRecoveryFlowException.java>) | 22:15:46 | Step FORGOT/OTP/PASSWORD, field lỗi, error code và thời gian chờ; phát hiện chỉnh đồng thời và giữ nguyên |
| [auth/service/PasswordResetService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordResetService.java>) | 22:19:19 | Luồng AccountSnapshot, findActiveAccount/isCurrent/changePassword, kiểm tra tài khoản và ghi password trong transaction; chuyển khỏi flow link-signing cũ. Có chỉnh đồng thời, assistant không overwrite |

Các file nhiều bên cùng sửa như RecoveryController, RecoveryService, PasswordResetEmailSender, GlobalExceptionHandler và hai file test đã có WIP trước đợt UI; mục 4–6 chỉ mô tả phần assistant thực sự bổ sung.

