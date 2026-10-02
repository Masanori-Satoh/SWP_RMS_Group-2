# Candidate tự đăng ký

**Nguồn:** code hiện tại và `database/schema/db.sql`; không dùng JWT, không thêm bảng. Chưa chạy browser thật trong lượt tài liệu.

## End-to-End Execution Flow

```text
GET /register → [PROJECT CODE] RegistrationController.form() → auth/register.html
POST /register + CSRF → [SPRING FRAMEWORK] validation/binding
 → [PROJECT CODE] RegistrationController.register()
 → RegisterAccountForm.toCommand() → CandidateRegistrationService.register()
 → RoleRepository.findByRoleName("Candidate")
 → AccountManagementService.create() → UserRepository uniqueness lookup
 → BCryptPasswordEncoder.encode() → UserRepository.saveAndFlush(User Active)
 → CandidateRepository.save(Candidate.account=User) → transaction commit
 → 302 /login?registered → auth/login.html
```

## Detailed Execution Trace

| Step | Loại; class.method; package/file | Input → output → next |
|---|---|---|
| 1 | [PROJECT CODE] `RegistrationController.form(Model)`; `com.group2.rms.controller` | `GET /register` public theo `SecurityConfig.filterChain` → model `RegisterAccountForm` → `auth/register.html`. |
| 2 | [SPRING FRAMEWORK] CSRF, MVC binding, Jakarta validation | POST gồm `fullName, username, email, password, confirmPassword`, CSRF → `@Valid RegisterAccountForm`, `BindingResult`; method nội bộ **Not verified at source-code level**. |
| 3 | [PROJECT CODE] `RegistrationController.register(...)` | So confirm; lỗi render lại form và xóa cả hai password; hợp lệ gọi `form.toCommand()` → 4. |
| 4 | [PROJECT CODE] `RegisterAccountForm.toCommand()`; `com.group2.rms.controller.form` | Chuyển 4 trường kinh doanh thành `CandidateRegistrationService.RegisterCommand`; confirm không xuống Service. |
| 5 | [PROJECT CODE] `CandidateRegistrationService.register(RegisterCommand)`; `com.group2.rms.service` | Trong `@Transactional`, `RoleRepository.findByRoleName("Candidate")`; tạo `AccountManagementService.CreateCommand` với phone/department null → 6. |
| 6 | [PROJECT CODE] `AccountManagementService.create(CreateCommand)`; `com.group2.rms.service` | Chuẩn hóa chuỗi, mật khẩu 8–32, Role/Department, tra username/email cả hai namespace; tạo `User` Active với BCrypt hash và `Candidate.account=User` → 7. |
| 7 | [PROJECT CODE] `UserRepository.saveAndFlush`, `CandidateRepository.save`; `com.group2.rms.repository` | JPA INSERT vào `dbo.[User]`, `dbo.Candidate`; transaction của `register` commit khi return bình thường. SQL chính xác Hibernate sinh động chưa capture. |
| 8 | [PROJECT CODE] `RegistrationController.register` | Thành công → 302 `/login?registered`; `AuthController.loginPage()` render thông báo. |

## Failure / Alternative Flows

- `@Valid` lỗi, confirm sai: 200 form có lỗi, không gọi Service, password trống.
- Username/email trùng hoặc trùng chéo định danh: `AccountFieldException`; DB unique race gây `DataIntegrityViolationException`; controller render lỗi, không tạo User/Candidate thành công.
- Thiếu Candidate role: `IllegalStateException` chưa được controller xử lý riêng; flow sẽ lỗi server. Không giả định HTTP cụ thể.
- Thiếu CSRF: 403; người chưa login vẫn được đăng ký qua route permitAll.

## Database Interaction

| Order | Repository Method | Entity | Table | Operation | Purpose |
|---|---|---|---|---|---|
| 1 | `RoleRepository.findByRoleName` rồi `findById` | Role | `Role` | SELECT | Tìm Candidate và xác nhận role. |
| 2 | `UserRepository.findByUsernameIgnoreCase`, `findByEmailIgnoreCase` | User | `User` | SELECT | Chặn trùng username/email, cả nhập chéo. |
| 3 | `UserRepository.saveAndFlush` | User | `User` | INSERT | Tạo identity/login Active. |
| 4 | `CandidateRepository.save` | Candidate | `Candidate` | INSERT | Tạo profile riêng với FK `UserId` unique, NOT NULL. |

## Data Transformation

HTML form → `RegisterAccountForm` → `RegisterCommand` → `CreateCommand` → `User` (`PasswordHash=encode(raw)`) + `Candidate(account)` → JPA. `ConfirmPassword` chỉ dùng kiểm tra tại Controller, không lưu DB.

## External Test Cases

| ID | Scenario | Preconditions | Action | Input | Expected HTTP | Expected Result | DB Change |
|---|---|---|---|---|---|---|---|
| REG-01 | Candidate mới | DB có role Candidate | Submit form | Username/email mới, password 8–32 khớp | 302 rồi 200 | `/login?registered`; đăng nhập được | +1 User, +1 Candidate |
| REG-02 | Username trùng | Có User cùng username | Submit form | Username cũ | 200 | Lỗi username | Không |
| REG-03 | Confirm sai | Có role Candidate | Submit form | Hai mật khẩu khác | 200 | Lỗi confirm, password không echo | Không |
| REG-04 | Thiếu CSRF | Không cần login | POST thủ công | Form không token | 403 | Không tạo account | Không |

## Test Procedure

Chuẩn bị role Candidate trong `dbo.[Role]`. REG-01: mở link **Đăng ký tài khoản Candidate** ở login, nhập dữ liệu mới, submit, thấy thông báo; trong SSMS SELECT chỉ đọc User join Candidate bằng `UserId`, xác nhận một cặp; đăng nhập bằng username. REG-02: lặp lại với username đã tồn tại, xác nhận lỗi và count không tăng. REG-03: dùng confirm khác và xem form không chứa password đã gõ. REG-04: POST từ HTTP client giữ cookie nhưng bỏ token, xác nhận 403.

## Test Data

Username/email thử nghiệm duy nhất như `candidate_flow_01` / `candidate_flow_01@example.test`; RoleName `Candidate`. Không thêm fixture vào production seeder. Password chỉ nhập ở form test, không ghi trong tài liệu.

## Debugging Points

| Order | Class | Method | Why |
|---|---|---|---|
| 1 | `RegistrationController` [PROJECT CODE] | `register` | BindingResult/confirm/redirect. |
| 2 | `CandidateRegistrationService` [PROJECT CODE] | `register` | Role Candidate và transaction. |
| 3 | `AccountManagementService` [PROJECT CODE] | `create` | Validation, encode, User/Candidate. |
| 4 | `UserRepository`, `CandidateRepository` [PROJECT CODE] | lookup / `saveAndFlush` / `save` | Truy vấn và constraint. |

## Code References

`src/main/java/com/group2/rms/config/SecurityConfig.java`; `src/main/java/com/group2/rms/controller/RegistrationController.java`; `src/main/java/com/group2/rms/controller/form/RegisterAccountForm.java`; `src/main/java/com/group2/rms/service/CandidateRegistrationService.java`; `src/main/java/com/group2/rms/service/AccountManagementService.java`; `src/main/java/com/group2/rms/repository/RoleRepository.java`; `src/main/java/com/group2/rms/repository/UserRepository.java`; `src/main/java/com/group2/rms/repository/CandidateRepository.java`; `src/main/java/com/group2/rms/entity/User.java`; `src/main/java/com/group2/rms/entity/Candidate.java`; `src/main/resources/templates/auth/register.html`; `database/schema/db.sql`; `src/test/java/com/group2/rms/SecurityFlowTests.java`.

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
