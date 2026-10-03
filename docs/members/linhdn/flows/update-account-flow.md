# Admin cập nhật tài khoản

> **Cập nhật 02/10/2026:** phần mô tả bên dưới là lịch sử, không dùng case role conversion. Admin Edit gọi `updateInternal()` và chỉ internal role; `update()` chặn đổi loại account hai chiều. Không tạo Candidate profile trong Update. Rule/steps/test hiện tại: [account-lifecycle-flow.md](account-lifecycle-flow.md). account-form.js Candidate-special-case đã bỏ.

**Nguồn:** Account edit form/controller/service/schema. Username read-only; password hash giữ nguyên. Browser/DB thực tế chưa chạy trong lượt này.

## End-to-End Execution Flow

```text
GET /admin/accounts/{userId}/edit → Security ROLE_SYSTEM_ADMIN
 → AccountController.editAccount() → AccountManagementService.findForEdit()
 → UserRepository.findById() → UpdateAccountForm → admin/accounts/form.html
POST /admin/accounts/{userId} + CSRF → @Valid UpdateAccountForm
 → AccountController.update() → form.toCommand()
 → AccountManagementService.update() [transaction]
 → User, Role, Department, email uniqueness, Candidate profile lookup
 → mutate allowed User fields → UserRepository.saveAndFlush()
 → nếu vừa chuyển sang Candidate và chưa có profile: CandidateRepository.save()
 → commit → 302 /admin/accounts + flash success
```

## Detailed Execution Trace

| Step | Loại; class.method; package/file | Input → output → next |
|---|---|---|
| 1 | [PROJECT CODE] `SecurityConfig.filterChain(...)`/`AccountSessionGuardFilter.doFilterInternal(...)`; `com.group2.rms.config/security`; [SPRING FRAMEWORK] role/CSRF | GET/POST edit chỉ Admin Active. |
| 2 | [PROJECT CODE] `AccountController.editAccount(int,Model)`; `com.group2.rms.controller` | GET `{userId}` → `AccountManagementService.findForEdit(userId)` → `UpdateAccountForm`; model có `accountUsername` read-only, roles/departments → form. |
| 3 | [PROJECT CODE] `AccountManagementService.findForEdit(int)`; `com.group2.rms.service` | `UserRepository.findById`; User/Role/Department → `AccountForEdit`; không gửi password hash. |
| 4 | [SPRING FRAMEWORK] MVC binding/validation và [PROJECT CODE] `UpdateAccountForm.toCommand()`; `com.group2.rms.controller.form` | POST fullName,email,phone?,roleId,departmentId?,accountStatus,CSRF → `UpdateCommand`. Form không có username/password input được bind vào command. Internal binder method **Not verified at source-code level**. |
| 5 | [PROJECT CODE] `AccountManagementService.update(int,UpdateCommand)`; `com.group2.rms.service` | `@Transactional`; load User; validate fields, role/dept/status Active/Inactive/Blocked, email uniqueness so với account khác (cùng UserId được); tra Candidate profile → mutate allowed fields. |
| 6 | [PROJECT CODE] `UserRepository.saveAndFlush`; optional `CandidateRepository.save`; `com.group2.rms.repository` | UPDATE User; nếu role mới Candidate và chưa profile thì INSERT Candidate. Không assign username/passwordHash. Sau commit, flash success và redirect list. |

## Failure / Alternative Flows

- UserId không tồn tại: `ResponseStatusException(404)` từ Service. Validation/role/dept/status/email trùng: render form với lỗi, không commit.
- Đổi status sang Inactive/Blocked hoặc đổi role của chính session: request sau bị `AccountSessionGuardFilter` thu hồi; response POST hiện tại có thể redirect trước khi guard thấy thay đổi DB.
- Nếu đổi User có Candidate profile sang role nội bộ, code **giữ profile**; quyết định nghiệp vụ vẫn chờ xác nhận trong `docs/WORK_LOG.md`.
- Guest/login; role khác 403; thiếu CSRF 403.

## Database Interaction

| Order | Repository Method | Entity | Table | Operation | Purpose |
|---|---|---|---|---|---|
| 1 | `UserRepository.findById` | User | `User` | SELECT | Load edit/update. |
| 2 | `RoleRepository.findById`, `DepartmentRepository.findById` | Role/Department | Cùng tên | SELECT | Validate selection. |
| 3 | `UserRepository.findByEmailIgnoreCase/findByUsernameIgnoreCase` | User | `User` | SELECT | Email không trùng User khác. |
| 4 | `CandidateRepository.findByAccountUserId` | Candidate | `Candidate` | SELECT | Biết đã có profile hay chưa. |
| 5 | `UserRepository.saveAndFlush` | User | `User` | UPDATE | Chỉ field được phép. |
| 6 | `CandidateRepository.save` nếu cần | Candidate | `Candidate` | INSERT | Tạo profile khi chuyển sang Candidate. |

## Data Transformation

User JPA → `AccountForEdit` → `UpdateAccountForm` → HTML; POST HTML → `UpdateAccountForm.toCommand()` → `UpdateCommand` → mutate User hiện có. Username/passwordHash không nằm trong `UpdateCommand`; email Candidate nằm ở `User.Email`, schema không có `Candidate.Email` để đồng bộ riêng.

## External Test Cases

| ID | Scenario | Preconditions | Action | Input | Expected HTTP | Expected Result | DB Change |
|---|---|---|---|---|---|---|---|
| UPDATE-01 | Sửa thông tin | Admin + User test | Submit | Tên/email mới, role/dept/status hợp lệ | 302 rồi 200 | Flash success; login username/password cũ được | UPDATE User |
| UPDATE-02 | Email trùng | Hai User test | Submit | Email của User khác | 200 | Báo lỗi, dữ liệu cũ giữ | Không |
| UPDATE-03 | Candidate email/role | User test Candidate hoặc đổi role | Submit | Email mới/role Candidate | 302 rồi 200 | Email ở User; profile liên kết | UPDATE User, optional INSERT Candidate |
| UPDATE-04 | Tamper username/password | Admin | POST thủ công | Thêm params username/password | 302 rồi 200 | Hai giá trị không thay đổi | Chỉ UPDATE field hợp lệ |

## Test Procedure

UPDATE-01: login Admin, bấm **Sửa** trên User test, xem username read-only và không có password; đổi tên/email, lưu; logout/login bằng username + password cũ; kiểm tra DB chỉ đọc. UPDATE-02: nhập email của User khác, xem lỗi và DB không đổi. UPDATE-03: đổi email Candidate, kiểm tra chỉ `User.Email` mới và `Candidate.UserId` vẫn giữ; nếu đổi internal→Candidate kiểm tra profile mới. UPDATE-04: qua DevTools/HTTP client thêm `username`/`password` trong POST hợp lệ, kiểm tra DB không đổi hai trường; không in hash.

## Test Data

Admin Active; hai User test với email khác nhau; một Candidate có `Candidate.UserId`. Không dùng User production cho đổi status/role. Đọc metadata UserId/Username/Email/Status, dùng test/encoder để so hash mà không xuất giá trị.

## Debugging Points

| Order | Class | Method | Why |
|---|---|---|---|
| 1 | `AccountController` [PROJECT CODE] | `editAccount`, `update` | DTO/model/lỗi. |
| 2 | `UpdateAccountForm` [PROJECT CODE] | `toCommand` | Chặn tamper field ngoài DTO. |
| 3 | `AccountManagementService` [PROJECT CODE] | `findForEdit`, `update` | Field mutation, uniqueness, profile. |
| 4 | `UserRepository`/`CandidateRepository` [PROJECT CODE] | `saveAndFlush`, `findByAccountUserId`, `save` | UPDATE/INSERT. |

## Code References

`src/main/java/com/group2/rms/config/SecurityConfig.java`; `src/main/java/com/group2/rms/security/AccountSessionGuardFilter.java`; `src/main/java/com/group2/rms/controller/AccountController.java`; `src/main/java/com/group2/rms/controller/form/UpdateAccountForm.java`; `src/main/java/com/group2/rms/service/AccountManagementService.java`; `src/main/java/com/group2/rms/repository/UserRepository.java`; `src/main/java/com/group2/rms/repository/CandidateRepository.java`; `src/main/java/com/group2/rms/repository/RoleRepository.java`; `src/main/java/com/group2/rms/repository/DepartmentRepository.java`; `src/main/java/com/group2/rms/entity/User.java`; `src/main/resources/templates/admin/accounts/form.html`; `src/main/resources/static/js/account-form.js`; `database/schema/db.sql`.

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
