# Đăng xuất

**Nguồn:** `SecurityConfig`, các form POST `/logout` trong Dashboard/Admin, `SecurityFlowTests`. Spring Boot 3.5.16. Chưa chạy browser thực tế trong lượt viết tài liệu.

## End-to-End Execution Flow

```text
Browser bấm Đăng xuất → form POST /logout + CSRF
 → [SPRING FRAMEWORK] SecurityFilterChain / CSRF check
 → [SPRING FRAMEWORK] logout handling do SecurityConfig.logout(...) bật
 → xóa Authentication/SecurityContext và session theo framework
 → 302 /login?logout → [PROJECT CODE] AuthController.loginPage()
 → auth/login.html hiển thị “Đăng xuất thành công.”
```

Tên class/method xử lý logout và persistence session cụ thể ở phiên bản runtime: **Not verified at source-code level**. Project không có LogoutController; cấu hình `.logoutUrl("/logout")` và `.logoutSuccessUrl("/login?logout")` ở `SecurityConfig.filterChain`.

## Detailed Execution Trace

| Step | Loại; class.method; package/file | Input → output → next |
|---|---|---|
| 1 | [PROJECT CODE] Thymeleaf form; `resources/templates/dashboard/index.html`, `admin/accounts/list.html`, `admin/accounts/form.html`, `admin/api-monitoring/index.html` | Nút Đăng xuất → `POST /logout`; form Thymeleaf gửi CSRF → 2. |
| 2 | [SPRING FRAMEWORK] CSRF processing trong `SecurityFilterChain`; cấu hình [PROJECT CODE] `SecurityConfig.filterChain(...)`; `com.group2.rms.config` | Token hợp lệ → logout handling; thiếu token → 403. Internal method **Not verified at source-code level**. |
| 3 | [SPRING FRAMEWORK] logout handling cấu hình trong `SecurityConfig.filterChain(...)` | Authentication/session hiện tại → hủy trạng thái đăng nhập và chuyển `/login?logout`; exact internal method **Not verified at source-code level**. |
| 4 | [PROJECT CODE] `AuthController.loginPage()`; `com.group2.rms.controller` | `GET /login?logout` → `auth/login.html` hiển thị thông báo; request bảo vệ sau đó lại yêu cầu đăng nhập. |

## Failure / Alternative Flows

- POST thiếu CSRF: 403, session vẫn còn (`SecurityFlowTests.logoutRequiresCsrf`).
- Request đến URL bảo vệ sau đăng xuất: 302 tới `/login`; chưa xác nhận bằng browser thật.

## Database Interaction

This flow does not directly access the database. Không có `Repository.save/delete`; trạng thái account trong SQL Server không đổi. Nếu `AccountSessionGuardFilter` nhận Authentication trước framework logout xử lý, guard có thể đọc User, nhưng thứ tự chính xác cho POST `/logout` **Not verified at source-code level**.

## Data Transformation

Form POST + CSRF + session cookie → framework lấy Authentication hiện tại → xóa SecurityContext/session → redirect và render thông báo. Không chuyển hay lưu mật khẩu.

## External Test Cases

| ID | Scenario | Preconditions | Action | Input | Expected HTTP | Expected Result | DB Change |
|---|---|---|---|---|---|---|---|
| LOGOUT-01 | Đăng xuất bình thường | User Active đã login | Bấm Đăng xuất | Form CSRF | 302 rồi 200 | `/login?logout`; mở `/dashboard` phải đăng nhập lại | Không |
| LOGOUT-02 | Thiếu CSRF | User Active đã login | POST thủ công | Cookie, không token | 403 | Session vẫn vào Dashboard được | Không |

## Test Procedure

Chuẩn bị User test Active. LOGOUT-01: đăng nhập, bấm Đăng xuất trên Dashboard; xem URL/thông báo, sau đó mở `/dashboard` và xác nhận redirect login. LOGOUT-02: đăng nhập lại, dùng DevTools/HTTP client POST `/logout` với cookie nhưng bỏ `_csrf`; xác nhận 403, refresh Dashboard vẫn hiển thị. Test trên browser/SQL Server thật chưa chạy trong lượt này.

## Test Data

Một `User` Active với role hợp lệ và password hash BCrypt; không dùng account sản xuất. Chỉ đọc `UserId, AccountStatus` trước/sau, không đọc hash.

## Debugging Points

| Order | Class | Method | Why |
|---|---|---|---|
| 1 | `SecurityConfig` [PROJECT CODE] | `filterChain` | Xem logout URL, success URL, CSRF. |
| 2 | Logout/CSRF filters [SPRING FRAMEWORK] | **Not verified at source-code level** | Kiểm tra token và xóa session khi đã gắn framework source. |
| 3 | `AuthController` [PROJECT CODE] | `loginPage` | Kiểm tra view sau redirect. |

## Code References

`src/main/java/com/group2/rms/config/SecurityConfig.java`; `src/main/java/com/group2/rms/controller/AuthController.java`; `src/main/resources/templates/auth/login.html`; `src/main/resources/templates/dashboard/index.html`; `src/main/resources/templates/admin/accounts/list.html`; `src/main/resources/templates/admin/accounts/form.html`; `src/main/resources/templates/admin/api-monitoring/index.html`; `src/test/java/com/group2/rms/SecurityFlowTests.java`.

## Flow Completion Checklist

- [x] Entry point identified
- [x] Request URL identified
- [x] Security behavior documented
- [x] Controller identified (N/A: Spring Security xử lý, không có LogoutController)
- [x] Service identified (N/A: project không có LogoutService)
- [x] Repository identified (N/A: logout không trực tiếp dùng Repository)
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
