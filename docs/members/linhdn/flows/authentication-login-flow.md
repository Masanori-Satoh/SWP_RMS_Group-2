# Truy cập URL bảo vệ và đăng nhập

**Cơ sở:** `SecurityConfig` của project, Spring Boot 3.5.16 (`pom.xml`), `SecurityFlowTests`. Trạng thái: trace project đã đối chiếu; chuỗi method nội bộ Spring và HTTP/browser thực tế **chưa xác minh ở source-code level / chưa chạy trong lượt này**. Không dùng JWT; form login và HTTP session.

## End-to-End Execution Flow

```text
Browser GET /admin/accounts (guest)
 → [SPRING FRAMEWORK] SecurityFilterChain: matcher System Admin
 → [SPRING FRAMEWORK] entry point của formLogin → 302 /login
 → [PROJECT CODE] AuthController.loginPage() → auth/login.html
 → Browser POST /login + username/password + CSRF
 → [SPRING FRAMEWORK] form-login authentication filter / AuthenticationManager
 → [SPRING FRAMEWORK] DaoAuthenticationProvider
 → [PROJECT CODE] DatabaseUserDetailsService.loadUserByUsername(identifier)
 → [PROJECT CODE] UserRepository.findByUsernameIgnoreCase + findByEmailIgnoreCase
 → [SPRING FRAMEWORK] JPA → dbo.[User], dbo.[Role]
 → [SPRING FRAMEWORK] BCryptPasswordEncoder.matches(raw, PasswordHash)
 → [SPRING FRAMEWORK] authenticated Authentication, SecurityContext, session → 302 saved URL (fallback /dashboard)
 → GET saved URL → [PROJECT CODE] AccountSessionGuardFilter → authorization → controller
```

**Cập nhật 01/10:** `SecurityConfig.filterChain()` dùng HttpSessionRequestCache, matchingRequestParameterName=null và `defaultSuccessUrl("/dashboard", false)`. Login ưu tiên saved URL, chỉ fallback Dashboard khi login trực tiếp. Guest `/jobs/{id}/apply` → login → đúng apply URL được xác minh bởi CareerFlowTests, chưa bởi browser HTTP thật. `/` và GET jobs public; apply matcher Candidate đứng trước public jobs. `POST /login` do framework xử lý, không vào AuthController. Method nội bộ framework không được tuyên bố đã đối chiếu source library.

## Detailed Execution Trace

| Step | Loại; class.method; package/file | Input → output → bước tiếp |
|---|---|---|
| 1 | [SPRING FRAMEWORK] `SecurityFilterChain` bean từ [PROJECT CODE] `SecurityConfig.filterChain(...)`; `com.group2.rms.config/SecurityConfig.java` | `GET /admin/accounts` guest → `.hasAuthority(ROLE_SYSTEM_ADMIN)` cần auth → form-login entry point. `/dashboard` dùng `.authenticated()`, còn `anyRequest` cũng yêu cầu auth. |
| 2 | [SPRING FRAMEWORK] form-login entry point | Guest chưa có authenticated principal → 302 `/login` → bước 3. Tên class/method entry point cụ thể **Not verified at source-code level**. |
| 3 | [PROJECT CODE] `AuthController.loginPage()`; `com.group2.rms.controller/AuthController.java` | `GET /login` → view `auth/login` → template `auth/login.html`, form `POST /login` có `username`, `password`; Thymeleaf/Spring chèn CSRF. |
| 4 | [SPRING FRAMEWORK] CSRF filter và form-login authentication filter; khai báo trong `SecurityConfig` | `POST /login` + CSRF → kiểm tra token, tạo yêu cầu authentication chưa xác thực từ credentials → manager/provider. Tên method nội bộ chính xác **Not verified at source-code level**. Thiếu CSRF → 403 trước xác thực. |
| 5 | [SPRING FRAMEWORK] `DaoAuthenticationProvider` được tạo tại `SecurityConfig.filterChain(...)`; `org.springframework.security.authentication.dao` | Identifier → gọi project `UserDetailsService`; raw password → `PasswordEncoder.matches`. Implementation cụ thể của `AuthenticationManager`/`ProviderManager` tại runtime **Not verified at source-code level**. |
| 6 | [PROJECT CODE] `DatabaseUserDetailsService.loadUserByUsername(String)`; `com.group2.rms.security/DatabaseUserDetailsService.java` | Trim identifier; tra username và email không phân biệt hoa thường; nếu hai account khác nhau cùng khớp thì từ chối. Chuyển role qua `RoleAuthorities.fromRoleName(String)`; trả `UserDetails` có username chuẩn, `PasswordHash`, authority và `disabled` khi status khác Active. |
| 7 | [PROJECT CODE] `UserRepository.findByUsernameIgnoreCase(String)` và `findByEmailIgnoreCase(String)`; `com.group2.rms.repository/UserRepository.java` | SELECT do JPA sinh động trên `User` (quan hệ `Role` EAGER); không ghi DB → bước 8. SQL cụ thể chưa capture. |
| 8 | [SPRING FRAMEWORK] `BCryptPasswordEncoder.matches(...)`, bean từ [PROJECT CODE] `SecurityConfig.passwordEncoder()` | So raw password của POST với hash DB; không so chuỗi hash trực tiếp. Thành công → authenticated principal; sai → failure URL. |
| 9 | [SPRING FRAMEWORK] SavedRequestAwareAuthenticationSuccessHandler / HttpSessionRequestCache; `org.springframework.security.web.authentication` / `org.springframework.security.web.savedrequest` | Authentication thành công mang principal UserDetails/ROLE_*. Redirect saved GET URL khi có, fallback `/dashboard`; không ép Dashboard. CareerFlowTests giữ MockHttpSession từ guest apply qua POST login và assert URL chính xác. Method nội bộ/session persistence source library chưa đối chiếu. |
| 10 | [PROJECT CODE] `AccountSessionGuardFilter.doFilterInternal(...)`; `com.group2.rms.security/AccountSessionGuardFilter.java` | Request tiếp theo lấy Authentication từ `SecurityContextHolder`; tra User theo username, kiểm tra Active và role chưa đổi. Sai → `SecurityContextLogoutHandler.logout(...)`, 302 `/login?session-expired`; đúng → chain/authorization/controller. |
| 11 | [PROJECT CODE] `DashboardAccessController.dashboard(...)`; `com.group2.rms.controller/DashboardAccessController.java` | `Authentication.getName()` → `DashboardService.forUsername(...)` → view `dashboard/index`; chi tiết trong `role-dashboard-flow.md`. |

## Failure / Alternative Flows

- Identifier trống, không tồn tại, khớp hai User hoặc role lạ: `loadUserByUsername()` ném `UsernameNotFoundException`; form login chuyển `/login?error` theo cấu hình.
- Sai mật khẩu: `matches` thất bại, 302 `/login?error`. Inactive/Blocked: `UserDetails.disabled(true)`, 302 cùng URL; giao diện không tiết lộ trường hợp nào.
- Thiếu/sai CSRF: HTTP 403. HR đăng nhập được nhưng GET `/admin/accounts` bị 403 theo matcher.
- Account bị vô hiệu hóa/đổi role sau login: `AccountSessionGuardFilter` thu hồi session khi request sau tới, chuyển `/login?session-expired`.

## Database Interaction

| Order | Repository Method | Entity | Table | Operation | Purpose |
|---|---|---|---|---|---|
| 1 | `findByUsernameIgnoreCase` | User/Role | `User`, `Role` | SELECT | Tìm tên đăng nhập và quyền. |
| 2 | `findByEmailIgnoreCase` | User/Role | `User`, `Role` | SELECT | Cho phép đăng nhập bằng email, phát hiện trùng định danh. |
| 3 | `findByUsernameIgnoreCase` trong guard | User/Role | `User`, `Role` | SELECT | Xác nhận session hiện còn hợp lệ ở request sau. |

## Data Transformation

Form `username/password` → yêu cầu Authentication framework (chưa xác thực) → `User` JPA → `UserDetails` (`username`, hash, role authority, disabled) → BCrypt check → `Authentication` đã xác thực → `SecurityContext`/session → `Authentication.getName()` cho Dashboard. Mật khẩu thô không được đưa vào `User`/DB ở flow này.

## External Test Cases

| ID | Scenario | Preconditions | Action | Input | Expected HTTP | Expected Result | DB Change |
|---|---|---|---|---|---|---|---|
| LOGIN-01 | Guest mở URL bảo vệ | Chưa login | Mở Account List | GET `/admin/accounts` | 302 rồi 200 | Tới `/login`, không lộ list | Không |
| LOGIN-02 | Đăng nhập đúng | User Active có role hợp lệ | Submit form | Username/email + mật khẩu đúng | 302 rồi 200 | Saved URL nếu có; login trực tiếp tới Dashboard | Không |
| LOGIN-03 | Mật khẩu sai/Inactive | User test đã chuẩn bị | Submit form | Sai mật khẩu hoặc account Inactive | 302 rồi 200 | `/login?error` | Không |
| LOGIN-04 | Sai quyền | HR Active đã login | Mở Account List | GET `/admin/accounts` | 403 | Không có list | Không |
| LOGIN-05 | Thiếu CSRF | Có session nhận form | POST thủ công không token | username/password | 403 | Không đăng nhập | Không |

## Test Procedure

Chuẩn bị hai tài khoản test `Active`: một System Admin, một HR; thêm một User `Inactive`. Không ghi mật khẩu thật vào tài liệu. Chạy app bằng cấu hình DB mới; base URL lấy từ `server.port` và context path (checkout hiện là `http://localhost:8082`).

1. LOGIN-01: dùng cửa sổ ẩn danh mở `/admin/accounts`; xem URL cuối là `/login` và không có danh sách.
2. LOGIN-02: đăng nhập Admin qua form; xem URL `/dashboard`, nội dung System Admin; đăng xuất, lặp lại bằng email của cùng account.
3. LOGIN-03: dùng sai mật khẩu, rồi thử User Inactive; mỗi lần thấy `?error`, không vào được Dashboard.
4. LOGIN-04: đăng nhập HR, mở Account List từ thanh địa chỉ; trong DevTools Network xác nhận 403.
5. LOGIN-05: dùng DevTools/HTTP client giữ cookie nhưng bỏ `_csrf` khi POST `/login`; xác nhận 403. Không dùng mật khẩu thật trong lệnh được chia sẻ.

## Test Data

`User` test có `Username`, `Email` duy nhất, `PasswordHash` BCrypt, `AccountStatus=Active/Inactive`, `RoleId` tới `RoleName='System Admin'/'HR'`. Ghi fixture trong môi trường test riêng; không thêm vào production seeder. Kiểm tra SELECT chỉ đọc trên `dbo.[User]`/`dbo.[Role]`; không đọc `PasswordHash` ra màn hình.

## Debugging Points

| Order | Class / loại | Method | Why |
|---|---|---|---|
| 1 | `SecurityConfig` [PROJECT CODE] | `filterChain` | Kiểm tra matcher, provider, login URL. |
| 2 | `DatabaseUserDetailsService` [PROJECT CODE] | `loadUserByUsername` | Xem identifier, lookup, disabled/authority. |
| 3 | `UserRepository` [PROJECT CODE] | `findByUsernameIgnoreCase`/`findByEmailIgnoreCase` | Xem kết quả tìm account. |
| 4 | `DaoAuthenticationProvider` [SPRING FRAMEWORK] | Internal password check: **Not verified at source-code level** | Theo dõi nhánh đúng/sai BCrypt khi có framework source. |
| 5 | `AccountSessionGuardFilter` [PROJECT CODE] | `doFilterInternal` | Xem session sau login và đổi status/role. |
| 6 | `DashboardAccessController` [PROJECT CODE] | `dashboard` | Xác nhận request đã qua security. |

## Code References

`src/main/java/com/group2/rms/config/SecurityConfig.java`; `src/main/java/com/group2/rms/controller/AuthController.java`; `src/main/java/com/group2/rms/security/DatabaseUserDetailsService.java`; `src/main/java/com/group2/rms/security/RoleAuthorities.java`; `src/main/java/com/group2/rms/security/AccountSessionGuardFilter.java`; `src/main/java/com/group2/rms/repository/UserRepository.java`; `src/main/java/com/group2/rms/entity/User.java`; `src/main/java/com/group2/rms/entity/Role.java`; `src/main/java/com/group2/rms/controller/DashboardAccessController.java`; `src/main/resources/templates/auth/login.html`; `database/schema/db.sql`; `src/test/java/com/group2/rms/SecurityFlowTests.java`.

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
