# Authentication UI, email OTP và xác minh đăng ký — 08/10/2026

## 1. Phạm vi và quyết định cuối

- Tài liệu này nối tiếp đợt giao diện Authentication bằng đợt **xác minh email trước khi tạo tài khoản**. Nhật ký và phạm vi bổ sung ở mục 12; bảng LOC ở mục 13.
- Chỉ dẫn mới nhất của người dùng: **giao diện tiếng Việt**, ghi đè “English only” trong task ban đầu.
- Homepage **đang được route `/` trả về là `candidate/job-board.html`**. `CareerPortalController.viewPublicJobList()` trả template này; không lấy `careers/index.html` cũ làm chuẩn cuối.
- Website của một công ty Mộc; không thêm marketplace/employer CTA.
- Giữ đăng nhập bằng session/form Spring Security, không JWT.
- Giữ OTP 6 chữ số, hiệu lực 15 phút kể từ gửi, gửi lại sau 5 phút, tối đa 5 lần sai. Người dùng đã duyệt bổ sung tối thiểu **PasswordRecoveryController và PasswordRecoveryService** để cấp model countdown/email cho trang OTP. Không đổi quy tắc OTP, schema hay cấu hình.
- Tất cả output build, harness, profile trình duyệt, ảnh và log chỉ nằm trong `docs/members/linhdn/`. Không tạo file source test mới; đợt đăng ký cập nhật hai file test hiện có cho hợp đồng OTP mới.
- Một tài liệu này gom quá trình, luồng kỹ thuật và hướng dẫn test; không tách thêm tài liệu MD cho đợt này.

## 2. Skills đã áp dụng

| Skill | Cách áp dụng |
|---|---|
| UI/UX Pro Max, bản local `.agents/skills/ui-ux-pro-max/` | Đọc SKILL; tra UX về accessible authentication, paste, error summary và typography. Dùng semantic form, label, autocomplete, focus và bố cục responsive. Typography cuối đối chiếu trực tiếp homepage thực tế. |
| Marketing Skills — copy-editing | Đọc SKILL và checklist; rút gọn tiêu đề/hướng dẫn/CTA; Việt hóa thông báo và email, bỏ hứa hẹn chức năng backend chưa có. |
| Impeccable | Đọc SKILL, audit, polish, craft floor; dùng context/detect và review trực quan. Giữ hướng hiện tại, giảm trang trí, chuẩn hóa nhịp và trạng thái. Detector có 2 cảnh báo phụ ở email: preheader ẩn line-height 1px và Arial fallback; giữ vì cần cho email client. |

## 3. Homepage design analysis

Đã render template homepage hiện tại với model kiểm tra rỗng, dùng HTML/CSS/JS gốc và xem ảnh trên Chrome ở 1440/375px. Không kết nối database, không thay nội dung homepage.

| Thành phần | Chuẩn kế thừa |
|---|---|
| Brand | Wordmark `mộc.`, biểu tượng lá sẵn có, subtitle `TUYỂN DỤNG` |
| Font | Lora cho tiêu đề; Plus Jakarta Sans cho nội dung như homepage. Lora dùng font local có sẵn; Plus Jakarta dùng cùng Google Fonts provider/font homepage đã dùng; fallback Source Sans 3/Segoe UI. Không thêm thư viện/CDN mới. |
| Màu | Nền chuyển nhẹ trắng → mint #f1f7f3 → kem #faf9f6 theo ảnh homepage mới nhất; surface trắng; chữ #1b241e; muted #59655d; CTA #1b4332; xanh nhạt #eaf3ee; border #d9ded7 |
| Spacing | 4/8/12/16/24/32/48/64/80px từ token có sẵn |
| Container | Tối đa 1180px; form tối đa 480px |
| Form | Border mảnh, panel trắng radius 20px và shadow nhẹ, input 48px/radius 8px, nút pill xanh đậm; OTP 6 ô đều nhau cao 56px |
| Responsive | Desktop 2 cột; <=768px 1 cột, giữ badge và dòng “Làm việc cùng Mộc.”, rút gọn phần giới thiệu; mobile gutter 16px |
| Accessibility | Skip link, label/id đúng, focus rõ, lỗi liên kết đúng input, aria-current bước, aria-live/status, reduced motion |

Theo phản hồi mới nhất của người dùng, bổ sung badge nghề nghiệp, tiêu đề serif xanh, nền mint và panel nổi nhẹ để Login/Register nhận diện cùng homepage. Không thêm hiệu ứng kính, minh họa người, thống kê hoặc cam kết tuyển dụng chưa có căn cứ.

## 4. Files thay đổi

| File | Loại | Sửa gì và lý do |
|---|---|---|
| [src/main/resources/templates/auth/login.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/login.html>) | MODIFY | Việt hóa nhãn và 5 trạng thái; làm rõ username/email, liên kết khôi phục; giữ POST /login và Spring Security. |
| [src/main/resources/templates/auth/register.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/register.html>) | MODIFY | Form ứng viên gồm họ tên, username, email, mật khẩu/xác nhận; lỗi từng trường tiếng Việt; giữ binding/validation/flow đăng ký hiện có. |
| [src/main/resources/templates/auth/forgot-password.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/forgot-password.html>) | MODIFY | Bước 1 nhập email, giải thích OTP; hiển thị cooldown khi backend cung cấp retryAfterSeconds; giữ POST /forgot-password. |
| [src/main/resources/templates/auth/reset-password.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/reset-password.html>) | MODIFY | Bước 3 chỉ nhập mật khẩu mới/xác nhận; khớp route POST /reset-password hiện tại; bỏ UI token/link cũ không còn phù hợp controller. |
| [src/main/resources/templates/auth/fragments.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/fragments.html>) | CREATE | Layout/head/header/story/footer/progress/error dùng riêng Authentication; tái sử dụng brand/global CSS; dịch thông báo BindingResult ở tầng hiển thị. |
| [src/main/resources/templates/auth/reset-password-otp.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/reset-password-otp.html>) | CREATE | Bước 2 có 6 ô số; một field otp gửi backend; fallback không JS; countdown và nút resend POST /forgot-password kèm email phiên reset + CSRF. |
| [src/main/resources/static/css/auth.css](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/css/auth.css>) | MODIFY | Chuẩn hóa spacing, màu/font theo homepage đang chạy; tất cả selector scope auth-page; 2 cột desktop, 1 cột tablet/mobile, focus/contrast/reduced motion. |
| [src/main/resources/static/js/auth.js](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/js/auth.js>) | CREATE | OTP tự chuyển ô, paste/autofill cả mã, Backspace/mũi tên, ghép một giá trị otp; hiện/ẩn mật khẩu, focus summary, busy submit, countdown từ backend; chỉ chạy trên body.auth-page. |
| [src/main/resources/templates/auth/email/password-reset-otp.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/email/password-reset-otp.html>) | CREATE | Email HTML tiếng Việt, bảng + inline CSS, preheader/OTP/security/footer; không script/link chứa OTP/asset ngoài. |
| [src/main/resources/templates/auth/email/password-reset-otp.txt](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/auth/email/password-reset-otp.txt>) | CREATE | Bản plain text tiếng Việt tương ứng email HTML. |
| [src/main/java/com/group2/rms/auth/service/PasswordResetEmailSender.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordResetEmailSender.java>) | MODIFY | Chỉ đổi phần trình bày: MimeMessageHelper UTF-8, multipart HTML/text, subject và render template; giữ constructor/isConfigured/sendOtp, from/to và exception contract. |
| [src/main/java/com/group2/rms/auth/controller/PasswordRecoveryController.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/controller/PasswordRecoveryController.java>) | MODIFY, đã duyệt | Thêm addOtpContext(Model, HttpSession), gọi từ GET OTP và POST lỗi định dạng; cấp pendingResetEmail/retryAfterSeconds. Tái sử dụng POST /forgot-password cho resend, không thêm route hoặc catch. |
| [src/main/java/com/group2/rms/auth/service/PasswordRecoveryService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordRecoveryService.java>) | MODIFY, đã duyệt | Thêm getPendingResetEmail(HttpSession) đọc challenge còn hiệu lực dưới session mutex. Giữ getResendRemainingSeconds và cơ chế cooldown atomic theo userId đang có. Không đưa hash/snapshot vào model. |

**Không xóa file source.** Toàn đợt: 8 file source đã có được sửa, 5 file source mới được thêm. Lượt tinh chỉnh theo ảnh mới thay đổi 8 file source: Controller, Service, auth.css, auth.js, fragments, Login, Register và OTP.

Các file phụ phục vụ kiểm tra nằm trong `docs/members/linhdn/tmp/auth-ui-2026-10-08/`; không phải feature production hay bộ source test mới.

## 5. Nhật ký thực hiện

1. Đọc task, architecture, skills, git status và các hợp đồng Controller/DTO/Service/Security/email hiện tại.
2. Tạo layout Authentication riêng và form tương ứng flow hiện tại; giữ action/method/name/model/CSRF.
3. Tách OTP UI khỏi trang nhập mật khẩu mới để khớp controller đã được người dùng sửa.
4. Chuyển adapter email từ SimpleMailMessage sang MIME UTF-8 HTML + plain text, giữ public contract và lỗi gửi mail.
5. Dùng Impeccable review/polish, kiểm tra render và thao tác bằng harness trong phạm vi output được phép.
6. Khi nhận chỉ dẫn tiếng Việt, kiểm tra lại route thực tế; mở homepage hiện tại, đối chiếu font/màu/brand; Việt hóa toàn bộ copy Authentication/email.
7. Thông báo backend vẫn giữ nguyên trong DTO/Service/Handler. Fragment Authentication dịch 26 thông báo đã audit; BindingResult và lỗi từng trường vẫn giữ nguyên. Thông báo chưa có trong catalogue dùng fallback tiếng Việt, không đưa chi tiết exception lên UI.
8. Kiểm tra render phát hiện lookup map trong SpEL cần `translations.get(text)`; sửa ở fragment, xác nhận nội dung từng lỗi tiếng Việt.
9. Lượt browser đầu tiếng Việt phát hiện nguồn Lora remote trùng font local. Bỏ import Lora remote; dùng asset Lora local và Plus Jakarta cùng homepage. Lượt cuối 203/203 PASS.
10. Kiểm tra hash/diff và ghi tài liệu này. Không commit/push/reset/stash hay sửa DB/config.
11. Người dùng phản hồi Login/Register còn đơn sơ và gửi ảnh homepage/OTP. Đối chiếu ảnh với code homepage hiện tại; bổ sung badge, nền mint, serif xanh và panel trắng, giữ hướng Mộc và giao diện tiếng Việt.
12. Chuyển OTP sang 6 ô bằng progressive enhancement: HTML gốc vẫn có một input dùng khi tắt JS; khi bật JS, 6 ô hiển thị và ghép vào một field name=otp. Không auto-submit hoặc lưu OTP vào URL/storage/log.
13. Xin và nhận duyệt đúng hai file Controller/Service. Thêm model email + thời gian còn lại; nút gửi lại dùng endpoint hiện có, backend tiếp tục là nơi quyết định 5 phút. Không đổi DB/DTO/handler/config.
14. Biên dịch lại 159 source Java; render Spring MVC/Thymeleaf/CSRF/MIME: 171 checks PASS. Chạy riêng 74 kiểm tra PasswordRecoveryService thật với Clock điều khiển; DB và SMTP được thay bằng adapter trong bộ kiểm tra tạm.
15. Chrome headless lượt tinh chỉnh: 226 checks PASS, 20 layout ở 1440/1024/768/375px; 0 console error/asset lỗi. Xem ảnh Login desktop, Register mobile và OTP mobile, xác nhận panel/mint/font giống hướng homepage; 6 ô không tràn, thao tác paste/backspace/autofill/native POST/no-JS và countdown đạt.

## 6. Luồng kỹ thuật hiện tại

### 6.1 Homepage → Login → Dashboard hoặc URL đã yêu cầu

| Bước | Route / hành động | Package, class, hàm |
|---|---|---|
| 1 | GET /, hoặc /jobs | `com.group2.rms.career.controller.CareerPortalController.viewPublicJobList()` → `candidate/job-board.html` |
| 2 | Nhấn Đăng nhập, GET /login | `com.group2.rms.auth.controller.AuthController.loginPage()` → `auth/login.html` |
| 3 | POST /login, username/password + CSRF | `org.springframework.security.web.csrf.CsrfFilter.doFilterInternal()` kiểm tra CSRF; `org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.attemptAuthentication()` đọc credentials |
| 4 | Xác thực | `org.springframework.security.authentication.ProviderManager.authenticate()` → `org.springframework.security.authentication.dao.DaoAuthenticationProvider.retrieveUser()` |
| 5 | Tìm account | `com.group2.rms.core.security.DatabaseUserDetailsService.loadUserByUsername()` → `com.group2.rms.user.repository.UserRepository.findByUsernameIgnoreCase()` / `findByEmailIgnoreCase()` |
| 6 | Đối chiếu mật khẩu/trạng thái | `DaoAuthenticationProvider.additionalAuthenticationChecks()` dùng `BCryptPasswordEncoder.matches()`; UserDetails disabled nếu account không Active |
| 7 | Thành công/thất bại | Success handler của Spring Security dùng saved request hoặc /dashboard theo `com.group2.rms.core.config.SecurityConfig.filterChain()`; thất bại → /login?error |
| 8 | Render trạng thái | `AuthController.loginPage()`; template hiển thị thông báo tiếng Việt |

POST /login được filter Spring Security xử lý; **không có Controller tự kiểm tra mật khẩu**.

### 6.2 Đăng ký Candidate

| Bước | Route / hành động | Package, class, hàm |
|---|---|---|
| 1 | GET /register | `com.group2.rms.auth.controller.RegistrationController.form()` tạo `RegisterAccountRequest`, trả `auth/register.html` |
| 2 | POST /register + CSRF | `RegistrationController.register()`; Spring bind/validate DTO; `com.group2.rms.core.validation.PasswordMatchesValidator.isValid()` so khớp 2 mật khẩu |
| 3 | Kiểm tra trước gửi mail | `com.group2.rms.auth.dto.RegisterAccountRequest.toCommand()` → `com.group2.rms.auth.service.RegistrationVerificationService.requestRegistration()` → `CandidateRegistrationService.prepare()`; Bean Validation, role Candidate, `AccountManagementService.assertLoginIdentifiersAvailable()` kiểm tra username/email và xung đột định danh đăng nhập |
| 4 | Chuẩn bị an toàn | `CandidateRegistrationService.prepare()` trim thông tin, BCrypt mật khẩu đúng một lần → `PreparedRegistration`; chưa ghi User/Candidate |
| 5 | Gửi OTP | `RegistrationVerificationService.sendCode()` dành send window atomic theo email → `RegistrationEmailSender.sendOtp()` → MIME HTML/text → SMTP adapter hiện có; chỉ sau gửi thành công mới lưu pending registration gồm hash mật khẩu và hash OTP trong HttpSession |
| 6 | Mở trang xác minh | Redirect GET /register/otp → `RegistrationController.otpForm()` → `addOtpContext()` → `getPendingRegistrationEmail()` / `getResendRemainingSeconds()`; render `auth/register-otp.html` với sáu ô và countdown |
| 7 | Xác minh | POST /register/otp + CSRF → `RegistrationController.verifyOtp()` → Bean Validation `VerifyRegistrationOtpRequest` → `RegistrationVerificationService.verifyAndRegister()`; kiểm tra state/hạn/số lần sai và BCrypt.matches OTP |
| 8 | Ghi identity/profile | Chỉ khi OTP đúng: `CandidateRegistrationService.registerVerified()` transaction → `AccountManagementService.createCandidateWithPasswordHash()`; kiểm tra uniqueness lần nữa → `persistAccount()` → UserRepository.saveAndFlush + CandidateRepository.save trong cùng transaction; User Active, role Candidate, Department NULL |
| 9 | Hoàn tất | Transaction trả thành công → xóa pending state → /login?registered. Không tự đăng nhập; submit lại không tạo tài khoản thứ hai |
| 10 | Lỗi nghiệp vụ | Service ném `RegistrationFlowException`; `com.group2.rms.core.exception.GlobalExceptionHandler.handleRegistrationFlowException()` redirect 303 + flash BindingResult về Register/OTP, chỉ giữ metadata, không giữ mật khẩu/OTP |
| 11 | Gửi lại | POST /register/otp/resend + CSRF → `RegistrationController.resendOtp()` → `RegistrationVerificationService.resendOtp()` → `sendCode()`; trước 300 giây bị server từ chối, đủ hạn gửi mã mới và thay challenge của session |

**Cập nhật theo yêu cầu mới:** đăng ký chỉ tạo tài khoản sau xác minh email. Route /register/** được public nhưng tất cả POST vẫn qua CSRF. Không JWT, không thêm/sửa bảng. OTP hiệu lực 15 phút, tối đa 5 lần sai, gửi lại sau 5 phút; email/username bị người khác đăng ký trong lúc chờ vẫn bị từ chối khi ghi cuối.

### 6.3 Quên mật khẩu → OTP → Mật khẩu mới

| Bước | Route / hành động | Package, class, hàm |
|---|---|---|
| 1 | GET /forgot-password | `com.group2.rms.auth.controller.PasswordRecoveryController.forgotForm()` → ForgotPasswordRequest → `auth/forgot-password.html` |
| 2 | POST email + CSRF | `PasswordRecoveryController.requestReset()` validate DTO; gọi `com.group2.rms.auth.service.PasswordRecoveryService.requestReset()` |
| 3 | Kiểm tra email | `com.group2.rms.auth.service.PasswordResetService.findActiveAccount()`; email không tồn tại/không Active ném flow exception, không tạo luồng reset |
| 4 | Tạo/gửi OTP | `PasswordRecoveryService.generateOtp()`, BCrypt hash OTP, check send window/cooldown; `PasswordResetEmailSender.sendOtp()`; lưu challenge ở session sau gửi thành công |
| 5 | Chuyển trang OTP | Redirect /reset-password/otp → `PasswordRecoveryController.otpForm()`; guard `canResetPassword()` / `hasPendingReset()`; `addOtpContext()` gọi Service lấy email và cooldown; render `auth/reset-password-otp.html` |
| 6 | POST OTP + CSRF | `PasswordRecoveryController.verifyOtp()`; validate `VerifyPasswordResetOtpRequest`; `PasswordRecoveryService.verifyOtp()` kiểm tra code/hạn/lần sai |
| 7 | OTP đúng | Service đánh dấu verified phía server; redirect /reset-password → `PasswordRecoveryController.resetForm()`; guard verified; render form mật khẩu mới |
| 8 | OTP sai/hết hạn/vượt số lần | Service ném `PasswordRecoveryFlowException`; handler chung redirect đúng bước, flash BindingResult; UI hiển thị lỗi tiếng Việt |
| 9 | POST password/confirm + CSRF | `PasswordRecoveryController.reset()` guard verified, Bean Validation/`PasswordMatchesValidator.isValid()`; `PasswordRecoveryService.completeReset()` |
| 10 | Ghi mật khẩu | `PasswordResetService.changePassword()` transaction; `UserRepository.findByIdForUpdate()`; kiểm tra lại snapshot/hạn/status; BCrypt encode → saveAndFlush |
| 11 | Hoàn tất | Service xóa state reset; redirect /login?reset → “Đặt lại mật khẩu thành công.” |

Không dùng link token/email URL để mở trang mật khẩu mới. Người dùng nhập OTP nhận trong email tại trang OTP của cùng session.

#### Gửi lại OTP và reload — luồng bổ sung đã duyệt

| Bước | Hành động | Package, class, hàm |
|---|---|---|
| 1 | Render/reload GET /reset-password/otp | `com.group2.rms.auth.controller.PasswordRecoveryController.otpForm()` → `addOtpContext()` |
| 2 | Lấy dữ liệu hiển thị | `com.group2.rms.auth.service.PasswordRecoveryService.getPendingResetEmail()` kiểm tra challenge còn sống; `getResendRemainingSeconds()` đọc sendWindows theo userId và Clock hiện tại, không khởi tạo lại 300 giây |
| 3 | Hiển thị | `auth/reset-password-otp.html` render countdown và disabled nút nếu còn thời gian; `static/js/auth.js` giảm số giây và mở nút khi đồng hồ về 0 |
| 4 | Nhấn Gửi lại mã | Native POST /forgot-password, hidden email của yêu cầu đang pending và CSRF; `CsrfFilter.doFilterInternal()` → `PasswordRecoveryController.requestReset()` |
| 5 | Server quyết định | `PasswordRecoveryService.requestReset()` → kiểm tra sendWindows atomic: trước hạn ném flow exception về GlobalExceptionHandler, không gửi mail; đủ hạn gọi email sender, thay challenge của session và bắt đầu cooldown mới |
| 6 | Thành công | Redirect GET /reset-password/otp; OTP mới thay OTP cũ trong cùng session, countdown mới lấy từ server |
| 7 | POST OTP bị lỗi định dạng | `PasswordRecoveryController.verifyOtp()` giữ BindingResult, gọi addOtpContext() và render form có đủ email/countdown |

Sửa DOM/đồng hồ trình duyệt để mở nút sớm không vượt được kiểm tra Service. Cooldown hiện lưu trong RAM của một instance, chưa có cơ chế dùng chung giữa nhiều instance hoặc qua restart; không tự thay kiến trúc/schema trong đợt này.

### 6.4 Email OTP

1. `com.group2.rms.auth.service.PasswordRecoveryService.requestReset()` gọi `PasswordResetEmailSender.sendOtp(to, otp)`.
2. Adapter kiểm tra sender/from theo cấu hình runtime hiện có; không đổi SMTP config.
3. `PasswordResetEmailSender.render(extension, otp)` đọc `templates/auth/email/password-reset-otp.html` / `.txt`.
4. HTML escape giá trị động, thay `{{otp}}`; MIME helper set From/To/subject/UTF-8/plain text + HTML.
5. `JavaMailSender.send()` gửi; lỗi Mail/Messaging/IO → `PasswordResetDeliveryException`; không log OTP.
6. `com.group2.rms.core.exception.GlobalExceptionHandler.handlePasswordRecoveryMailFailure()` giữ hợp đồng redirect/flash hiện tại; fragment dịch lỗi gửi mail sang tiếng Việt.

### 6.5 JavaScript chỉ trình bày

- `src/main/resources/static/js/auth.js` chỉ chạy khi có `body.auth-page`.
- Hiện/ẩn: đổi type input và aria-pressed/aria-label tiếng Việt; không thay giá trị.
- Submit: sau native validation, disabled nút và aria-busy; không Ajax/đổi endpoint.
- Error summary: focus khi server đã trả lỗi; các liên kết dẫn đúng input.
- Countdown: chỉ dùng `data-retry-after-seconds` do backend cấp; hết hạn thông báo qua role=status. **Server vẫn quyết định có được gửi lại hay không.**
- OTP: sáu ô có label riêng, inputmode numeric; paste mã đủ từ bất kỳ ô nào điền lại toàn bộ; nhập số tự tiến, Backspace xóa/lùi, mũi tên di chuyển. Chỉ field hidden `name=otp` gửi lên; không tự submit khi đủ 6 số. Không JS thì dùng input gốc.
- Quay lại bằng back/forward cache: khôi phục nút submit.
- Brand accessible label/tab title được Việt hóa trong phạm vi Auth; shared fragments giữ nguyên.

## 7. Test thủ công — các bước cụ thể

### Chuẩn bị

- Chạy ứng dụng bằng cấu hình hiện có của dự án; không chạy SQL/migration/seeder bổ sung cho đợt UI.
- Chuẩn bị account Active và email nhận mail có thật; dùng account Inactive/Blocked có sẵn để test âm khi có.
- SMTP phải được cấu hình ngoài Git theo thiết lập hiện có. Không đưa mật khẩu SMTP/OTP vào tài liệu hoặc chat.
- Kiểm tra cùng trình duyệt/session trong toàn bộ luồng reset.
- Đối với các test tạo tài khoản/đổi mật khẩu, dùng account test được phép; đây là hướng dẫn thao tác ứng dụng thật, chưa được harness DB thực hiện.

| ID | Test | Các bước | Kết quả mong đợi |
|---|---|---|---|
| UI-01 | Brand/homepage | Mở / → Đăng nhập → Về trang tuyển dụng | Cùng logo, font, màu; trở về /; không chuyển sang prototype cũ |
| UI-02 | Responsive | Mở cả 5 form ở 1440/1024/768/375px | Desktop 2 cột, <=768 một cột; input/nút không tràn; brand vẫn nhìn thấy |
| UI-03 | Keyboard | Tab từ đầu trang → skip link → Enter; Tab qua form/links | Focus rõ, thứ tự hợp lý, skip tới main; không keyboard trap |
| UI-04 | Hiện/ẩn | Nhập mật khẩu → Tab tới Hiện → Space 2 lần | Hiện rồi ẩn, giữ nguyên giá trị, aria-pressed đúng, focus ở nút |
| UI-05 | Không JavaScript | Tắt JS, reload Login/Register | Form vẫn gửi native, link hoạt động, mật khẩu vẫn ẩn; không hiện nút toggle không hoạt động |
| LOGIN-01 | Active account | Nhập username và password hợp lệ → Đăng nhập; lặp lại bằng email | Vào dashboard hoặc saved URL đúng quyền |
| LOGIN-02 | Sai/inactive | Sai password; dùng account Inactive/Blocked có sẵn | /login?error; thông báo tiếng Việt, không lộ chi tiết mật khẩu/tài khoản |
| LOGIN-03 | Trạng thái | Mở /login?logout, ?registered, ?reset, ?session-expired | Copy/visual success hoặc error phù hợp, liên kết tiếp tục rõ |
| REG-01 | Gửi thông tin | Tạo username/email test chưa dùng, đầy đủ trường, 2 mật khẩu giống nhau 8–32 ký tự → Gửi mã xác minh | Sang /register/otp, nhận email; chưa có User/Candidate trong DB |
| REG-02 | Validation | Bỏ trống trường, email sai, password ngắn, confirm khác; submit trực tiếp server khi muốn test Bean Validation | Native ngăn dữ liệu không hợp lệ; lỗi server ở summary/đúng trường, tiếng Việt; password không render lại |
| REG-03 | Uniqueness | Dùng username/email của account test đã có | Lỗi trùng dữ liệu tiếng Việt, form giữ họ tên/username/email, không giữ password |
| REG-04 | Hoàn tất | Sau REG-01, nhập mã đúng nhận trong email → Xác minh và tạo tài khoản → đăng nhập | /login?registered; đúng một User Active/role Candidate và một Candidate profile liên kết; mật khẩu đã BCrypt, không BCrypt lại hash |
| REG-05 | Mã sai / 5 lần | Nhập mã đủ 6 số nhưng sai; lặp đủ 5 lần trên cùng challenge | Lần 1–4 ở OTP, không tạo tài khoản; lần 5 về Register, chỉ hủy challenge, không khóa tài khoản; gửi mới vẫn phải qua cooldown |
| REG-06 | Cooldown / reload | Gửi mã → chờ 60 giây → reload OTP; thử POST resend trực tiếp trước 5 phút; sau đủ 5 phút gửi lại | Countdown còn khoảng 4:00; request sớm bị từ chối dù sửa DOM; sau hạn gửi mới, countdown bắt đầu lại, mã cũ không còn xác minh session này |
| REG-07 | Session khác / bypass | Session mới truy cập GET/POST /register/otp với CSRF đúng, hoặc resend khi chưa đăng ký | Handler đưa về Register, không tạo tài khoản; email/password/OTP từ request không thể tự dựng pending state |
| REG-08 | Hết hạn / submit lặp | Chờ >15 phút rồi xác minh; hoặc sau REG-04 dùng lại form OTP cũ | Không tạo thêm tài khoản; challenge hết hạn/đã dùng bị từ chối |
| REG-09 | Lỗi SMTP | Môi trường test làm gửi mail thất bại → gửi form; khôi phục SMTP rồi gửi lại | Không báo thành công, không ghi User/Candidate/challenge mới; lỗi tiếng Việt qua Global Handler; không giữ reservation 5 phút cho lượt gửi thất bại |
| REG-10 | Race uniqueness | Gửi OTP; trong môi trường test dùng luồng khác tạo cùng username/email; sau đó xác minh mã đúng | Kiểm tra cuối/unique constraint từ chối, quay về Register với lỗi; không tạo hồ sơ mồ côi |
| REG-11 | FE OTP | 1440/1024/768/375px, keyboard, paste đủ mã vào ô đầu/giữa, Backspace, mũi tên; tắt JS và reload | Không tràn; six boxes cùng một field otp; no-JS input gốc vẫn dùng được; CSRF cả verify/resend; không đưa OTP vào URL |
| FP-01 | Email đúng | /forgot-password → nhập email Active → Gửi mã xác minh | Sang /reset-password/otp; nhận email OTP; không cần click link email |
| FP-02 | Email không tồn tại | Nhập email test chưa tồn tại → gửi | Ở Forgot, thông báo không có tài khoản; không tạo reset challenge |
| FP-03 | Lỗi mail | Trong môi trường test có lỗi gửi mail thực tế → gửi yêu cầu | Handler trả Forgot với thông báo gửi mail thất bại tiếng Việt; không fake thành công |
| FP-04 | Cooldown | Gửi thành công → ở trang OTP thấy 5:00/nút resend disabled → chờ 4 phút 59 giây → kiểm tra → đủ 5 phút bấm gửi lại | Trước hạn nút disabled; đủ hạn POST gửi lại, nhận mã mới và countdown mới. Gửi request trực tiếp/cùng email ở trình duyệt khác trước hạn vẫn bị Service từ chối |
| OTP-01 | Paste/autofill | Dán toàn bộ mã 6 số vào ô đầu rồi ô giữa; nhập lần lượt; Backspace ở ô có/không có số; Left/Right; nhập chữ | Mã chia đúng 6 ô; tự chuyển, sửa/xóa/lùi đúng; chặn chữ; một giá trị otp gửi lên; không auto-submit/đưa mã vào URL |
| OTP-06 | Reload countdown | Sau gửi chờ khoảng 60 giây rồi reload trang OTP | Khoảng 4:00 theo thời gian server còn lại, không quay lại 5:00 |
| OTP-07 | Resend và mã cũ | Sau đủ 5 phút bấm gửi lại; nhập mã cũ, sau đó mã mới trong cùng session | Mã cũ không xác minh challenge mới; mã mới đưa tới form mật khẩu. Không ghi mã vào tài liệu |
| OTP-08 | Tắt JS | Tắt JS, reload OTP; nhập đủ mã; sau 5 phút reload lần nữa | Input fallback vẫn dùng được; resend trước hạn disabled từ server, sau hạn enabled qua reload |
| OTP-02 | Sai mã | Nhập mã 6 số sai 1 lần | Ở OTP, lỗi “Mã xác minh không đúng”; focus summary/links đúng ô, không echo mã |
| OTP-03 | 5 lần sai | Nhập mã sai đủ 5 lần trong một challenge | Trở về Forgot, yêu cầu mã mới; không khóa User, vẫn tuân thủ cooldown |
| OTP-04 | Hết hạn | Chờ >15 phút kể từ gửi; submit/refresh bước OTP/reset | Guard/handler ngăn tiếp tục; về bước thích hợp. GET guard có thể redirect Forgot mà không flash lý do, xem hạn chế bên dưới |
| OTP-05 | Không được bypass | Session mới mở /reset-password/otp hoặc /reset-password; chưa xác minh thử POST reset có CSRF hợp lệ | Chuyển về Forgot hoặc OTP; không cập nhật mật khẩu |
| RESET-01 | Mật khẩu không khớp | Xác minh OTP đúng → nhập password/confirm khác → gửi | Lỗi tại confirm bằng tiếng Việt, input password trống lại |
| RESET-02 | Hoàn tất | OTP đúng → 2 mật khẩu mới giống nhau 8–32 ký tự → Lưu → đăng nhập | /login?reset; password mới đăng nhập được, password cũ không được; không reuse reset session đã hoàn tất |
| SEC-01 | CSRF | Với request thử nghiệm, xóa hidden _csrf hoặc gửi token sai trên từng POST | HTTP 403 theo cấu hình, không gửi mail/ghi tài khoản/mật khẩu |
| EMAIL-01 | Nội dung thực | Gửi reset tới email test, mở email trong Gmail/Outlook | Subject/body tiếng Việt, code đủ 6 số, 15 phút, note không chia sẻ/ignore; plain text đúng |
| EMAIL-02 | Mobile/email | Mở email ở điện thoại/desktop; tắt tải ảnh | Vẫn đọc được brand/OTP/nội dung; không cần asset website hoặc JS |
| A11Y-01 | Zoom/motion | Zoom/text 200%, bật giảm chuyển động | Form vẫn dùng được; không tràn, không chuyển động bắt buộc |
| SCOPE-01 | Module khác | Mở homepage và dashboard/module đang có | Auth CSS/JS không được áp dụng sang trang không có auth-page |

## 8. Verification đã thực hiện

| Hạng mục | Kết quả | Phạm vi thực tế |
|---|---|---|
| Maven compile | PASS | 159 source Java, temporary POM trỏ nguồn thật, build output dưới docs/members/linhdn |
| JavaScript syntax | PASS | Node --check auth.js |
| Thymeleaf/MVC/BindingResult/CSRF/MIME, lượt tinh chỉnh cuối | PASS — 171 checks | Controller/DTO/validator/handler/renderer/adapter thật; service nghiệp vụ giả lập; model cooldown/email trên GET và POST lỗi, hai form CSRF; không DB/SMTP |
| PasswordRecoveryService runtime | PASS — 74 checks | Service thật, Clock điều khiển và BCrypt; DAO/mail adapter giả lập. Cooldown 299/300 giây, cross-session, reload còn 240 giây, OTP mới/cũ, 5 sai, 15 phút, snapshot/guard. Không kiểm chứng transaction DB hoặc vận chuyển SMTP |
| Chrome headless, lượt tinh chỉnh cuối | PASS — 226 checks | 20 layout = 5 trang × 4 width; 6 ô OTP, paste ở đầu/giữa, typing/autofill/Backspace/Left/Right/chặn chữ, native POST một otp + CSRF, countdown/resend/reload/no-JS, show-hide/focus/reduced motion/zoom/email preview |
| Text contrast | PASS | Text ~15.94:1; muted ~6.10:1; primary ~11.08:1 |
| Input border contrast | PASS | ~3.60:1 |
| Console / asset | PASS | 0 console error, 0 broken CSS/JS/font trong lượt Auth cuối |
| CSS scope | PASS | Selector Authentication có scope .auth-page; trang khác không nạp auth.js |
| Source diff whitespace | PASS | File Auth đã sửa; không sửa whitespace test file có sẵn của người dùng |
| Hash scope, lượt tiếng Việt trước tinh chỉnh | PASS | 11 file Authentication UI/email; không có file ngoài phạm vi trong delta |
| Hash scope, lượt tinh chỉnh cuối | ĐÃ ĐỐI CHIẾU | Delta gồm 8 source Authentication thuộc phạm vi + tài liệu này; còn phát hiện PasswordResetService.java thay đổi bên ngoài các edit của lượt này, timestamp 21:03:48 sau baseline 21:01:19. Giữ nguyên; không restore/overwrite hoặc nhận là thay đổi của đợt UI |
| Maven full test suite | NOT RUN | Không tự nhận toàn bộ test project xanh |
| Spring Boot + DB startup | NOT TESTED | Chỉ Spring MVC context riêng của harness đã chạy; không gọi seeder/DB của ứng dụng thật |
| Database transaction/login thật/OTP E2E với DB + SMTP | NOT TESTED | OTP Service đã được kiểm tra runtime độc lập; luồng tích hợp DB/mail thật cần dùng môi trường test hiện có với các bước ở mục 7 |
| SMTP/Gmail/Outlook thật | NOT TESTED | Đã kiểm tra MIME/render local; chưa gửi email cho người khác hay kiểm tra client thật |

Các lỗi công cụ kiểm tra tạm thời trong quá trình làm đã được sửa (fixture CSRF/MIME/POST interception); không dùng chúng để thay đổi backend hay bỏ qua lỗi. Lượt browser tiếng Việt đầu fail kiểm tra font do trùng nguồn Lora; lượt cuối sau sửa đã PASS.

### Bằng chứng

- [Log Maven compile cuối](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/tmp/auth-ui-2026-10-08/compile-refinement.log>)
- [171 kiểm tra render/CSRF/MIME](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/tmp/auth-ui-2026-10-08/java-results.txt>)
- [74 kiểm tra OTP Service thật](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/tmp/auth-ui-2026-10-08/otp-runtime-results.txt>)
- [226 kiểm tra trình duyệt cuối](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/tmp/auth-ui-2026-10-08/refinement-round1/results.json>)
- [Login desktop](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/tmp/auth-ui-2026-10-08/refinement-round1/login-1440.png>)
- [Register mobile](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/tmp/auth-ui-2026-10-08/refinement-round1/register-375.png>)
- [OTP mobile](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/tmp/auth-ui-2026-10-08/refinement-round1/otp-375.png>)
- [Email preview — mã đã che](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/tmp/auth-ui-2026-10-08/vietnamese-round2/email-mobile.png>)
- [Homepage thực tế — model test rỗng](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/tmp/auth-ui-2026-10-08/homepage-current-1440.png>)
- [Delta hash 11 file](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/tmp/auth-ui-2026-10-08/scope-after-vietnamese.json>)
- [Delta lượt tinh chỉnh và thay đổi đồng thời](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/docs/members/linhdn/tmp/auth-ui-2026-10-08/scope-after-refinement.json>)

## 9. Scope compliance và thay đổi có sẵn — đợt UI trước bổ sung đăng ký

Git worktree trước khi làm đã có sửa ở PasswordRecoveryController, ResetPasswordRequest, PasswordRecoveryService, PasswordResetService, GlobalExceptionHandler, SecurityFlowTests, AuthenticationDatabaseTests; DTO VerifyPasswordResetOtpRequest và exception PasswordRecoveryFlowException đã có ở dạng untracked. Những thay đổi này là của người dùng, không phải sản phẩm của đợt UI.

PasswordResetEmailSender cũng đã có sửa trước đó; đợt này chỉ thêm presentation MIME/template/subject, giữ contract/cấu hình/hành vi gửi lỗi. PasswordRecoveryController có một comment được chỉnh đồng thời trong phiên; không ghi đè.

Không sửa homepage, shared fragments/styles/JS/tokens, DTO/handler/SecurityConfig, entity/repository/database/migration/config/pom/source test. Ngoại lệ mới đã được người dùng duyệt: bổ sung model/helper trong đúng PasswordRecoveryController và PasswordRecoveryService; không thay flow/luật OTP hay overwrite code có sẵn. Delta hash lượt tiếng Việt trước tinh chỉnh gồm 11 file Auth. Không reset/revert/stash/commit/push.

Đối chiếu hash lượt tinh chỉnh phát hiện `PasswordResetService.java` có thay đổi đồng thời (LastWriteTime 21:03:48, baseline 21:01:19). Các edit của lượt này không ghi file đó; giữ nguyên nội dung hiện tại. Compile cuối lúc 21:07:39 đã đọc phiên bản hiện tại. Runtime OTP helper thay adapter phía DB, nên chưa kiểm chứng transaction thật của PasswordResetService.

## 10. Hạn chế và việc cần kiểm tra tiếp

1. Countdown trên GET OTP và resend trực tiếp đã được bổ sung sau duyệt hai file backend. Send window hiện nằm trong RAM của instance; restart hoặc nhiều instance không dùng chung cooldown. Không thay lưu trữ/schema trong đợt này.
2. GET guard hết hạn hiện có thể redirect Forgot không flash lý do. Khi handler nhận flow exception hết hạn, thông báo đã được Việt hóa; UI không tự bịa trạng thái backend.
3. Đã bổ sung OTP đăng ký theo yêu cầu mới, xem mục 6.2 và 12. Pending registration lưu trong HttpSession; đóng/mất session hoặc restart thì phải đăng ký lại.
4. Shared head/brand vẫn có suffix/accessible label tiếng Anh trong HTML gốc. Auth.js Việt hóa tab title và brand label trên trang Auth; khi tắt JS phần brand dùng tên shared hiện tại, chức năng form không phụ thuộc JS.
5. Homepage hiện tại có tràn ngang khi render 375px (ảnh full-page rộng hơn viewport). Đã báo, **chưa sửa** vì homepage ngoài phạm vi đợt này.
6. Plus Jakarta Sans dùng provider Google Fonts đã có trên homepage; khi mạng không tải được sẽ dùng fallback font sẵn có. Email dùng Georgia/Arial tương thích client, không phụ thuộc font website.
7. Chưa xác nhận production readiness hoặc full-suite green; các bài DB/SMTP/client thật cần thực hiện theo mục 7.

## 11. Ghi chú để nối tiếp

- Bắt đầu bằng đọc tài liệu này, `git status` và controller/flow hiện tại; không phục hồi token/link cũ.
- Bổ sung hai file backend cho model countdown/email đã được duyệt và hoàn thành. Muốn đổi business/handler/shared UI/homepage hoặc lưu trữ cooldown phải xác định phạm vi đợt tiếp theo.
- Nếu backend thêm/đổi thông báo, cập nhật catalogue `auth/fragments :: message(text)` để tránh fallback chung.
- Profile, checker và target trong `docs/members/linhdn/tmp/auth-ui-2026-10-08/` là vật liệu kiểm tra tạm, không chạy như ứng dụng production.

## 12. Đợt nối tiếp — xác minh email đăng ký, 08/10/2026

### 12.1 Yêu cầu và quyết định triển khai

Người dùng yêu cầu sửa trực tiếp **chỉ luồng Register**, bao gồm FE; trình tự đã xác nhận là điền đầy đủ form, validate hết, gửi email, nhập OTP rồi mới tạo tài khoản. Không tạo User Inactive trước xác minh, không sửa schema, không dùng JWT. Không thay đổi luồng Login/Forgot/Reset/Admin.

Thêm phần mở rộng tối thiểu trong AccountManagementService, GlobalExceptionHandler và SecurityConfig vì Register dùng chung các thành phần này. Trước khi bỏ hàm `CandidateRegistrationService.register(RegisterCommand)`, đã search dependency Java: controller đã chuyển sang service OTP, còn hai file test hiện có được cập nhật sang hợp đồng mới. Không xóa entity/repository/template của thành viên khác.

### 12.2 Quá trình và file đã sửa

Đường dẫn dưới đây tính từ thư mục `SWP_RMS_Group-2/`.

| File | Thay đổi | Lý do |
|---|---|---|
| `src/main/java/com/group2/rms/auth/controller/RegistrationController.java` | GET/POST Register, GET/POST OTP, POST resend; gọi Service, không try-catch; xóa password khi Bean Validation lỗi | Tách gửi thông tin và tạo tài khoản; exception về handler chung |
| `src/main/java/com/group2/rms/auth/service/CandidateRegistrationService.java` | `prepare()` validate, kiểm tra uniqueness, BCrypt; `registerVerified()` ghi transaction; bỏ hàm tạo ngay từ raw password; command/record không in thông tin nhạy cảm | Mật khẩu plaintext không giữ qua bước OTP; tránh BCrypt lại hash |
| `src/main/java/com/group2/rms/user/service/AccountManagementService.java` | Thêm `assertLoginIdentifiersAvailable()`, `createCandidateWithPasswordHash()`; tách phần ghi chung `persistAccount()` | Tái sử dụng kiểm tra định danh và tạo User/Candidate; tạo account của Admin vẫn encode raw password như cũ |
| `src/main/java/com/group2/rms/core/config/SecurityConfig.java` | Chỉ thêm `/register/**` vào public routes hiện có | Guest truy cập bước OTP; không bỏ CSRF hoặc đổi quyền module khác |
| `src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java` | Thêm handler RegistrationFlowException, redirect 303 + flash form/BindingResult an toàn | Lỗi sai OTP/cooldown/mail/conflict về đúng bước; không render lỗi trong Controller |
| `src/main/resources/templates/auth/register.html` | CTA Gửi mã xác minh, trình tự hai bước, copy tiếng Việt, countdown khi bị cooldown | Thể hiện đúng việc chưa tạo tài khoản ở bước đầu |
| `src/main/resources/templates/auth/fragments.html` | Thêm fragment hai bước đăng ký và ba thông báo mới | Tái sử dụng header/story/errors/token của Auth hiện có |
| `src/main/resources/static/css/auth.css` | Một selector có scope `data-registration-steps` để hiển thị hai bước | Không đổi ba bước recovery hoặc UI module khác |
| `src/test/java/com/group2/rms/SecurityFlowTests.java` | Mock service xác minh, thay redirect cũ bằng /register/otp; kiểm tra route Guest, CSRF verify/resend và redirect sau xác minh | Test cũ kỳ vọng tạo tài khoản ngay không còn đúng nghiệp vụ |
| `src/test/java/com/group2/rms/service/AuthenticationDatabaseTests.java` | Test/fixture dùng `prepare()` → `registerVerified()`; đổi tên test đang active | Giữ test writer User/Candidate tương thích; không sửa/comment thêm các test reset đã bị comment trước đợt này |

### 12.3 File mới

| File | Nội dung |
|---|---|
| `src/main/java/com/group2/rms/auth/dto/VerifyRegistrationOtpRequest.java` | Bean Validation OTP đúng sáu chữ số |
| `src/main/java/com/group2/rms/auth/exception/RegistrationFlowException.java` | Step Register/OTP, field lỗi, retry interval và metadata an toàn |
| `src/main/java/com/group2/rms/auth/service/RegistrationVerificationService.java` | Pending session, hash OTP, SecureRandom, Clock, expiry, 5 sai, cooldown atomic theo email, commit rồi mới xóa state |
| `src/main/java/com/group2/rms/auth/service/RegistrationEmailSender.java` | SMTP adapter từ cấu hình hiện có, MIME UTF-8 HTML + text; lỗi gửi dự kiến chuyển thành flow exception |
| `src/main/resources/templates/auth/register-otp.html` | Sáu ô OTP, paste/keyboard/fallback no-JS, countdown từ server, hai form có CSRF |
| `src/main/resources/templates/auth/email/registration-otp.html` | Email xác minh riêng cho đăng ký, tiếng Việt, không gửi link/mật khẩu |
| `src/main/resources/templates/auth/email/registration-otp.txt` | Bản plaintext cùng nội dung |

Không xóa file. Không sửa workbook LOC. Không thêm source test mới hoặc file MD mới. Helper, log, screenshot, classpath và temporary POM ở `docs/members/linhdn/tmp/registration-otp-2026-10-08/`; không đổi pom.xml/config của project để phục vụ kiểm tra.

### 12.4 Ràng buộc và tình huống lỗi

- OTP 6 số, 15 phút; 5 lần sai hủy pending; resend sau 300 giây. Cooldown dùng chung theo email giữa các session trên cùng instance.
- Countdown đọc thời gian còn lại từ server, reload không khởi tạo lại 5 phút. JavaScript chỉ trình bày; sửa DOM không vượt được Service.
- Session lưu PreparedRegistration gồm password hash và OTP hash; không lưu mật khẩu plaintext, không đưa hash/OTP vào form/URL/flash/log. OTP plaintext chỉ tồn tại lúc tạo MIME và kiểm tra input.
- Gửi mail lỗi: không tạo account/challenge mới; reservation được giải phóng. Lỗi hệ thống không thuộc nhóm đã biết tiếp tục về Global Handler, không giả thành công.
- Uniqueness kiểm tra trước gửi và lúc tạo account; unique constraint username/email được chuyển thành lỗi đăng ký đúng bước nếu có race. Lỗi constraint khác không bị coi nhầm là email trùng.
- Tạo User và Candidate trong transaction hiện có; role Candidate, status Active, department NULL. Hoàn tất không tự login, không chuyển Candidate thành nhân viên nội bộ.
- Session pending và cooldown nằm trong RAM; restart/mất session phải bắt đầu lại, nhiều instance chưa chia sẻ cooldown. Không thay database/lưu trữ để giải quyết giới hạn này trong đợt hiện tại.
- Frontend dùng Auth CSS/JS và fragment hiện có, tiếng Việt, mint/forest và font đồng bộ homepage. Không sửa homepage hoặc luồng recovery để phục vụ Register.

### 12.5 Verification đợt đăng ký

Các bước test thủ công nằm chung ở mục 7 (REG-01 đến REG-11). Kết quả cuối:

| Kiểm tra | Kết quả | Phạm vi / giới hạn |
|---|---|---|
| Maven compile | PASS | 163 production source và 29 source test biên dịch; output dưới docs/members/linhdn |
| Maven Register + AccountManagement | PASS — 9 test | Hai test Register/validation trong SecurityFlowTests và bảy AccountManagementServiceTests; 0 failure/error, Spring MVC + Security Context khởi tạo thành công |
| Registration runtime | PASS — 91 checks | Service thật, Clock điều khiển, BCrypt và AccountManagement writer; repository/mail adapter in-memory. Gửi lại 299/300 giây, cross-session/concurrency, 5 sai, 15 phút, lỗi mail, uniqueness, không double-hash, hoàn tất một lần |
| MVC / Thymeleaf / MIME | PASS — 125 checks | Controller/DTO/validator/Global Handler/template/email sender thật; verification business stubbed. BindingResult/flash an toàn, CSRF, lỗi/cooldown và MIME HTML/text |
| Browser | PASS — 88 checks | Register + Register OTP tại 1440/1024/768/375; nhập/dán/autofill/keyboard, no-JS, countdown, native POST/CSRF, error focus; 0 console/runtime error và 0 broken asset |
| Review ảnh | PASS | Đã xem Register desktop và OTP 375px; typography/mint/forest/brand giữ hướng homepage; không tràn ngang ở 8 layout |
| Hash scope | ĐÃ ĐỐI CHIẾU | 353 file theo git inventory: 17 source/test thuộc Register + tài liệu này; workbook giữ nguyên. Phát hiện thêm PasswordRecoveryFlowException.java và PasswordResetService.java được chỉnh đồng thời, không phải edit của đợt Register, đã giữ nguyên; không có file bị xóa |
| Maven toàn bộ project | NOT RUN | Không nhận toàn bộ suite xanh |
| Lượt rộng hai lớp test | FAIL ngoài phạm vi còn cần xử lý | Lượt 22 test: 19 PASS, 3 FAIL. Assertion Register cũ `data-validation-summary` đã đổi đúng `data-auth-errors` và PASS ở lượt 9 test cuối. Hai lỗi Admin/API dưới đây giữ nguyên |
| Spring Boot + database / SMTP thực tế | NOT TESTED | Chưa chạy seeder, schema init, transaction DB rollback, nhận email Gmail/Outlook thật hoặc E2E tạo tài khoản thật |

Hai lỗi ngoài phạm vi Register phát hiện trong lượt rộng:

1. `SecurityFlowTests.adminCanEnterAdminRouteAndHrCannot`: test vẫn mong text **Internal Accounts**, còn sidebar hiện tại hiển thị **Tài khoản nội bộ**. Không đổi sidebar hoặc assertion Admin trong đợt Register.
2. `SecurityFlowTests.monitoringProbeNeedsAdminAndCsrf`: Mockito báo `probeInternal(any())` **Wanted 1 time, was 2 times**. Không sửa controller/service/test API Monitoring trong đợt này.

Sự cố môi trường kiểm tra: mặc định sandbox chặn pipe attach của Mockito; JVM fork còn gặp `InstrumentationImpl.appendToBootstrapClassLoaderSearch` / `IllegalArgumentException`. Mockito dù dùng subclass vẫn có ModuleMemberAccessor gọi instrumentation. Lượt cuối chạy Maven **không fork** ngoài sandbox đã thành công, giữ source cấu hình framework của project nguyên vẹn. Temporary POM cũng đọc src/test/resources của project; không dùng việc comment/bỏ test để làm xanh.

Lệnh xác nhận cuối (chạy từ root project; thư mục TEMP/TMP/java.io.tmpdir cũng trỏ vào scratch được duyệt):

```powershell
.\mvnw.cmd -o '-Dmaven.repo.local=C:/Users/Administrator/.m2/repository' `
  -f docs/members/linhdn/tmp/registration-otp-2026-10-08/pom.xml `
  '-Djacoco.skip=true' '-Dui.preview=false' '-DforkCount=0' `
  '-Dtest=SecurityFlowTests#loginShowsRegisterAndForgotPasswordAndRegistrationRequiresCsrf+invalidFormsAssociateErrorsAndPreserveNonSecretInputWithoutServiceWrites,AccountManagementServiceTests' test
```

Bằng chứng trong `docs/members/linhdn/tmp/registration-otp-2026-10-08/`:

- `maven-registration-final.log`: BUILD SUCCESS, 9 test PASS, thời điểm 22:21:55.
- `runtime-results.txt`: 91 checks.
- `mvc-results.txt`: 125 checks.
- `browser-round1/results.json`: 88 checks và 8 layout.
- `browser-round1/register-1440.png` / `registration-otp-375.png`: ảnh đã xem.
- `maven-tests-no-fork-escalated.log`: lượt rộng 22 test và lỗi thật để nối tiếp.
- `scope-before.json` / `scope-after.json`: hash inventory; artifact tạm không tính là source mới.

`PasswordRecoveryFlowException.java` có thay đổi đồng thời được phát hiện ở các mốc 21:57:23 và 22:15:46; `PasswordResetService.java` có timestamp 22:19:19. Root/agent không ghi hai file này. Compile/test cuối 22:21:55 đã đọc các phiên bản đó. Giữ thay đổi có sẵn và chỉnh sửa đồng thời của luồng Recovery; không restore/overwrite. Diff whitespace còn dòng trống có space trong phần Recovery của GlobalExceptionHandler và các comment test cũ; không dọn phần ngoài Register.

## 13. Ước tính LOC theo `Tính Loc.xlsx`

### 13.1 Cách tính đã đọc

Nguồn: `docs/members/linhdn/Tính Loc.xlsx`, sheet **LOC Estimation guide**, vùng **B2:N10** và hai ảnh ví dụ được nhúng trong workbook. Workbook này quy đổi độ phức tạp chức năng/màn hình thành **LOC tối đa**, không đếm số dòng Java/HTML/CSS thực tế.

| Level | Số field/component hoặc transaction | LOC tối đa |
|---|---|---:|
| 1 | 3–5 field hoặc 2 transaction | 60 |
| 2 | 6–7 field hoặc 3 transaction | 90 |
| 3 | 8–9 field hoặc 4 transaction | 120 |
| 4 | 10–11 field hoặc 5 transaction | 150 |
| 5 | 12–13 field hoặc 6 transaction | 180 |
| 6 | 14–15 field hoặc 7 transaction | 210 |
| 7 | >15 field hoặc >7 transaction | 240 |

- Field là input/selector/nút nghiệp vụ, bảng, pagination hoặc loại row action; không nhân theo số hàng. Activate/Deactivate là một action có trạng thái như ảnh ví dụ.
- Transaction DB/external chưa thể hiện qua component thì quy đổi thêm hai field; không đếm gửi mail thêm lần nữa nếu đã tính nút gửi mã.
- Create/Update chung màn hình: **nhân đôi level**, không nhân đôi LOC. Ví dụ trong workbook: L2 → L4 → 150 LOC.
- Sáu ô OTP là **một input nghiệp vụ**, không phải sáu field.
- Không cộng CSRF hidden input, hiện/ẩn mật khẩu, shared header/sidebar/footer, error copy, email template, tài liệu hoặc test thành chức năng riêng.
- Có mâu thuẫn nhỏ: B2 ghi field **OR** transaction, O3:O9 ghi **AND**. Ước tính dưới đây theo phần hướng dẫn và ví dụ, cần người chấm xác nhận nếu dùng để báo cáo chính thức.

### 13.2 Danh sách ước tính theo chức năng hiện có

Đây là allowance của nhóm chức năng được kiểm tra trong code, **chưa phải LOC được công nhận riêng cho LinhDN**: workbook hiện vẫn có owner mẫu TuanNV/AnhPT, chưa có phân công LinhDN; một số dashboard dùng chung với team. Cần đối chiếu phân công và lịch sử commit trước khi ghi điểm cá nhân.

| Chức năng | Thành phần tính | Level | LOC ước tính |
|---|---|---|---:|
| Login | Username/email, password, submit = 3 | L1 | 60 |
| Register form | Full name, username, email, password, confirm, gửi mã = 6 | L2 | 90 |
| Register OTP | Một mã OTP, xác minh/tạo account, gửi lại = 3 | L1 | 60 |
| Forgot Password | Theo tiêu chí transaction: tìm User và gửi SMTP = 2; không cộng thêm SMTP vào nút Gửi | L1 | 60 |
| Recovery OTP | Một OTP, xác minh, gửi lại = 3 | L1 | 60 |
| Reset Password | Password, confirm, lưu = 3 | L1 | 60 |
| Internal Account List | Keyword/role/status/department/sort, filter/clear/create, table/edit/activate-deactivate/pagination = 12 | L5 | 180 |
| Candidate Account List | Keyword/status/sort, filter/clear, table/activate-deactivate/pagination = 8 | L3 | 120 |
| Create/Update Internal Account | Create 9, edit 7; lấy level cao hơn L3 rồi nhân đôi | L6 | 210 |
| Department List | Keyword/status/filter/clear/create/table/edit/activate-deactivate/pagination = 9 | L3 | 120 |
| Create/Update Department | Name, manager, save = 3; L1 nhân đôi | L2 | 90 |
| Dashboard dùng chung | 15 component chức năng tổng hợp; không nhân theo sáu role | L6 | 210 |
| API Monitoring nội bộ | Hai transaction: loopback HTTP GET và health SELECT 1 | L1 | 60 |
| **Tổng ước tính** | | | **1.380** |

Khoảng thực tế theo cách phân loại Dashboard: **1.350–1.410 LOC**. Riêng Register + Register OTP là **150 LOC**; nếu form Register cũ đã được tính 90 thì phần chức năng OTP thêm khoảng **60 LOC**, không cộng lại toàn bộ 150 vào điểm cũ. Logout có thể thêm 60 nếu được giao riêng, nhưng chưa cộng vì ownership shared chưa rõ.

Không cộng phần chưa triển khai: AI Configuration, probe AI/email, Candidate notifications/reminders, withdraw, accept/decline/negotiate offer, cập nhật profile/change password từ dashboard, CV upload/application creation. Không nhận feature chỉ có nút disabled là đã hoàn thành.

Bản phân tích máy đọc: `docs/members/linhdn/tmp/registration-otp-2026-10-08/loc-estimate.json`. Workbook gốc giữ nguyên.
