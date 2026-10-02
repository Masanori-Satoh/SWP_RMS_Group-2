# Auth + Admin — shared UI flow, 01/10/2026

Phạm vi là giao diện Thymeleaf hiện có. UI English; text từ DB giữ nguyên. Không sửa
Java production, Spring Security, database, seed hoặc JWT trong đợt này.
Static preview chỉ phục vụ HTML fixture; **không thực hiện đăng nhập/ghi DB**.

## 1. Tải một màn hình

| Step | Package / file | Class / hàm hoặc fragment | Kết quả |
|---|---|---|---|
| 1 | `com.group2.rms.config` | `SecurityConfig.filterChain(...)` | Filter chain hiện có kiểm tra session, quyền và CSRF; xem flow authentication gốc cho từng Spring filter. |
| 2 | `com.group2.rms.controller` | Controller GET tương ứng ở bảng dưới | Tạo model thật từ service, trả tên template. Không dùng dữ liệu UI fixture trong runtime. |
| 3 | `resources/templates/fragments/head.html` | `interfaceHead(title, pageStyles)` | Title, viewport, no-referrer; local tokens/interface/workspace/page CSS; defer interface.js. |
| 4 Auth | `fragments/auth-layout.html`, `brand.html` | `brandPanel`, `wordmark(subtitle)` | Brand panel + link Careers; compact brand header trên mobile. |
| 4 Admin | `fragments/workspace-header.html`, `sidebar.html` | `header(role, fullName)`, `workspace(activeMenu, role)` | Wordmark, role, optional real name, native POST logout; active navigation `aria-current`. |
| 5 | `static/js/interface.js` | IIFE; menu `sync()` và native event callbacks | Mobile menu progressive enhancement, password toggle, liên kết/focus error summary. Không gọi API, lưu password hay tạo session. |
| 6 | `static/css/design-tokens.css` | CSS font faces / `:root` | Palette/type/spacing chia sẻ với careers.css. Font local có Vietnamese subsets. |

Dashboard có `fullName` thật trong model. Account/API không có tên user trong model,
nên header chỉ hiển thị System Admin theo route đã bảo vệ; không bịa tên và không
thay controller. Legacy `head`/`sidebar` dùng bởi Hello/Requisition giữ nguyên.

## 2. Controller entry points giữ nguyên

Tất cả controller trong package **`com.group2.rms.controller`**; form trong
**`com.group2.rms.controller.form`**; service trong **`com.group2.rms.service`**.

| Màn hình / hành động | URL / method | Class.hàm | Service / bước tiếp theo |
|---|---|---|---|
| Login GET | `GET /login` | `AuthController.loginPage()` | `auth/login` → username/password native form. |
| Login POST | `POST /login` | `org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.attemptAuthentication(...)` | `DatabaseUserDetailsService.loadUserByUsername(identifier)` trong `com.group2.rms.security`; encoder/Active/session/request cache giữ nguyên. Không vào AuthController POST. |
| Register GET | `GET /register` | `RegistrationController.form(Model)` | `RegisterAccountForm` → `auth/register`. |
| Register POST | `POST /register` | `RegistrationController.register(...)` | Bean Validation/BindingResult; valid → `CandidateRegistrationService.register(form.toCommand())`; invalid render lại và xóa password input. |
| Forgot GET | `GET /forgot-password` | `PasswordRecoveryController.forgotForm(Model)` | `ForgotPasswordForm`. |
| Forgot POST | `POST /forgot-password` | `PasswordRecoveryController.requestReset(...)` | `PasswordResetService.request(...)` → `PasswordResetEmailSender.send(...)` khi có link; generic confirmation giữ nguyên. |
| Reset GET | `GET /reset-password/{token}` | `PasswordRecoveryController.resetForm(...)` | `PasswordResetService.isValid(token)`; no-store/no-referrer; invalid không có form. |
| Reset POST | `POST /reset-password/{token}` | `PasswordRecoveryController.reset(...)` | Binding/confirmation → `PasswordResetService.reset(...)`; success redirect login. Không thêm bảng/JWT. |
| Dashboard | `GET /dashboard` | `DashboardAccessController.dashboard(...)` | `DashboardService.forUsername(authentication.getName())` → DashboardView → loops thật theo role. |
| Accounts GET | `GET /admin/accounts` | `AccountController.list(...)` | `AccountListService.findAccounts(...)`, `findRoles()`, `findDepartments()` → Page + filters + options. |
| Create GET | `GET /admin/accounts/new` | `AccountController.newAccount(...)`, `prepareCreate(...)` | `CreateAccountForm`; role/dept options thật. |
| Create POST | `POST /admin/accounts` | `AccountController.create(...)` | Validate → `AccountManagementService.create(form.toCommand())`; Active mặc định và encoder trong service. |
| Edit GET | `GET /admin/accounts/{userId}/edit` | `AccountController.editAccount(...)`, `prepareEdit(...)` | `AccountManagementService.findForEdit(userId)` → UpdateAccountForm; username readonly, không password input. |
| Edit POST | `POST /admin/accounts/{userId}` | `AccountController.update(...)` | Validate → `AccountManagementService.update(userId, form.toCommand())`; password hash không thay. |
| Delete/Deactivate | `POST /admin/accounts/{userId}/deactivate` | `AccountController.deactivate(...)` | `AccountListService.deactivate(userId)` → Inactive; không repository.deleteById. |
| API Monitoring GET | `GET /admin/api-monitoring` | `ApiMonitoringController.index(Model)` | `ApiMonitoringService.rows()`; real data/Unavailable states. |
| Probe internal | `POST /admin/api-monitoring/probe/internal` | `ApiMonitoringController.probeInternal(...)` | `ApiMonitoringService.probeInternal(request)`; giữ target/rule và feedback thật. |
| Logout | `POST /logout` | `org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(...)` | CSRF + session invalidation hiện có; shared header chỉ đổi markup/style. |

## 3. Password Show/Hide — không gọi backend

1. `interface.js` IIFE tìm `input[type=password]` và label thật.
2. Tạo `.password-field` + button **type=button**, `aria-controls`, `aria-pressed=false`.
3. Callback `click` đổi `input.type` password↔text, Show↔Hide và accessible name.
4. Giữ `name`, `value`, `autocomplete`, min/max/required; không log/storage.
5. Native form `submit` callback trả type=password; browser gửi field như trước.
6. Không JS → password input/form vẫn hoạt động; không có toggle.

## 4. Server validation → focus lỗi

1. Controller POST bind DTO và Bean Validation như bảng trên.
2. Invalid: render template với BindingResult, không gọi write service.
3. Fragment `messages.validationSummary` đọc `#fields.errors('*')`; inline `th:errors`
   giữ nguyên; field có `aria-invalid=true` và `aria-describedby` tới error ID.
4. `interface.js` tìm error message/label phù hợp, biến summary item thành link field.
5. Chỉ khi có error summary mới focus summary; không autofocus màn GET bình thường.
6. Callback link `click` focus input lỗi; không focus về homepage/hero.

## 5. Mobile navigation + table

1. `workspace-header.header` render Menu hidden và `aria-controls=workspace-navigation`.
2. `interface.js` `matchMedia('(max-width:768px)')`, local `sync()` điều chỉnh hidden/expanded.
3. Menu click mở/đóng; Escape đóng và focus Menu. Desktop hiện sidebar.
4. Không JS: sidebar luôn hiện, logout vẫn POST; links không phụ thuộc JS.
5. Bảng accounts/API giữ native table, th scope/caption, named region tabindex=0;
   scroll chỉ trong region. Focus rồi Arrow Left/Right xem cột còn lại. Account identity
   first column sticky; `interface.js` ResizeObserver/`syncHint()` chỉ hiện cue khi overflow.
6. `account-list.js` callback `DOMContentLoaded` tạo matchMedia600px và `syncFilters()`.
   Native More Filters đóng trên phone khi Department/Sort default, mở khi server
   `data-active=true`; desktop/no-JS giữ fields visible. GET names/values không đổi.

## 6. Delete confirmation

1. `account-list.js` callback `DOMContentLoaded` lấy native dialog/form/Cancel.
2. Button `[data-deactivate-account]` click lưu trigger, đọc name/username và URL từ row server-rendered.
3. Dùng `textContent` cho name/username; gán đúng `/admin/accounts/{id}/deactivate` vào form.action.
4. `dialog.showModal()`; focus Cancel; chưa POST, chưa thay record.
5. Cancel hoặc Escape → dialog `close` callback focus trigger. Confirm Deactivate →
   native POST + CSRF → controller/service ở bảng trên.

## 7. Tài liệu nghiệp vụ chi tiết và verification

- [Login/filter trace](authentication-login-flow.md), [Logout](authentication-logout-flow.md)
- [Register](candidate-registration-flow.md), [Forgot](forgot-password-flow.md), [Reset](reset-password-flow.md)
- [Dashboard](role-dashboard-flow.md), [Account List](account-list-flow.md), [Create](create-account-flow.md), [Update](update-account-flow.md), [Deactivate](deactivate-account-flow.md)
- [API Monitoring](api-monitoring-flow.md), [career saved target](career-homepage-apply-flow.md)
- [Step-by-step UI regression](../tests/2026-10-01-auth-admin-ui.md)

MockMvc chứng minh controller/Thymeleaf/security contracts; Chrome trên static fixtures
chứng minh layout/control behavior. Hai loại này không thay thế E2E trên Tomcat/SMTP thật.
