# Admin tạo tài khoản

> **Cập nhật 02/10/2026:** phần mô tả bên dưới là lịch sử flow trước khi tách lifecycle. Admin Create chỉ tạo **Internal Account** qua `AccountManagementService.createInternal()`, không Candidate role. Rule/steps/test hiện tại: [account-lifecycle-flow.md](account-lifecycle-flow.md). Candidate profile chỉ tạo trong public registration; account-form.js cũ đã bỏ, Department nội bộ required bằng native HTML.

**Nguồn:** Account form/controller/service/schema mới. Chỉ System Admin; Candidate có User auth và Candidate profile riêng. Browser/DB thực tế chưa chạy trong lượt tài liệu.

## End-to-End Execution Flow

```text
GET /admin/accounts/new → Security ROLE_SYSTEM_ADMIN → AccountController.newAccount()
 → CreateAccountForm + Role/Department dropdown → admin/accounts/form.html
POST /admin/accounts + CSRF → @Valid CreateAccountForm → AccountController.create()
 → confirmPassword check → form.toCommand()
 → AccountManagementService.create() [transaction]
 → Role/Department lookup + username/email uniqueness lookup
 → BCryptPasswordEncoder.encode(raw)
 → UserRepository.saveAndFlush(User Active)
 → nếu Candidate: CandidateRepository.save(Candidate.account=User)
 → commit → 302 /admin/accounts + flash success
```

## Detailed Execution Trace

| Step | Loại; class.method; package/file | Input → output → next |
|---|---|---|
| 1 | [PROJECT CODE] `SecurityConfig.filterChain(...)`; `com.group2.rms.config`; [SPRING FRAMEWORK] authorization/CSRF | GET/POST `/admin/accounts` chỉ `ROLE_SYSTEM_ADMIN`; POST thiếu CSRF → 403. |
| 2 | [PROJECT CODE] `AccountSessionGuardFilter.doFilterInternal(...)`; `com.group2.rms.security` | Session phải còn Active, role không đổi → 3. |
| 3 | [PROJECT CODE] `AccountController.newAccount(Model)` + `prepareCreate(Model)`; `com.group2.rms.controller` | GET → `CreateAccountForm`, `AccountListService.findRoles/findDepartments` → view `admin/accounts/form`. |
| 4 | [SPRING FRAMEWORK] MVC binding/Jakarta validation trên [PROJECT CODE] `CreateAccountForm`; `com.group2.rms.controller.form` | POST fullName, username, email, phone?, roleId, departmentId?, password, confirm → `BindingResult`; giới hạn name100/username50/email150/phone20/password8–32. Internal framework method **Not verified at source-code level**. |
| 5 | [PROJECT CODE] `AccountController.create(...)` | Confirm khớp; `form.toCommand()` → `CreateCommand` (không chứa confirm). Validation/lỗi → xóa password khỏi form và render lại. |
| 6 | [PROJECT CODE] `AccountManagementService.create(CreateCommand)`; `com.group2.rms.service` | `@Transactional`; trim field, password rule, `findRole`, `findDepartment` (internal phải có department), `checkUsernameAvailable`/`checkEmailAvailable` tra cả chéo username-email. |
| 7 | [PROJECT CODE] `SecurityConfig.passwordEncoder()` trả [SPRING FRAMEWORK] `BCryptPasswordEncoder`; `com.group2.rms.config` | `encode(rawPassword)` → hash đưa vào `User.PasswordHash`; mặc định `AccountStatus=Active`, không hỏi status lúc tạo. |
| 8 | [PROJECT CODE] `UserRepository.saveAndFlush`, nếu role Candidate `CandidateRepository.save`; `com.group2.rms.repository` | INSERT User; Candidate profile riêng FK `UserId` unique/NOT NULL; commit khi service trả bình thường. SQL do Hibernate sinh, chưa capture. |
| 9 | [PROJECT CODE] `AccountController.create` | Flash “Tạo tài khoản thành công.” → 302 `/admin/accounts` → list. |

## Failure / Alternative Flows

- `@Valid`/confirm sai: 200 form, password không echo, không INSERT.
- Role hoặc department sai/thiếu internal: `AccountFieldException` gắn field; Candidate được để department trống.
- Username/email trùng, kể cả trùng chéo login identity: field error; DB unique race: global error. Transaction rollback nếu lỗi.
- Guest → login; non-Admin → 403; thiếu CSRF → 403. Unknown role không được tạo.

## Database Interaction

| Order | Repository Method | Entity | Table | Operation | Purpose |
|---|---|---|---|---|---|
| 1 | `RoleRepository.findAll/findById` | Role | `Role` | SELECT | Dropdown và xác thực role. |
| 2 | `DepartmentRepository.findAll/findById` | Department | `Department` | SELECT | Dropdown và kiểm tra department. |
| 3 | `UserRepository.findByUsernameIgnoreCase/findByEmailIgnoreCase` | User | `User` | SELECT | Uniqueness cùng và chéo định danh. |
| 4 | `UserRepository.saveAndFlush` | User | `User` | INSERT | Tạo account Active, hash BCrypt. |
| 5 | `CandidateRepository.save` khi Candidate | Candidate | `Candidate` | INSERT | Tạo hồ sơ FK UserId. |

## Data Transformation

HTML form → `CreateAccountForm` → `CreateCommand` → normalized values + Role/Department entities → `User` (hash BCrypt, Active) → optional `Candidate(account)` → JPA/SQL Server. `ConfirmPassword` chỉ ở controller, không lưu. `PasswordHash` không render ra list.

## External Test Cases

| ID | Scenario | Preconditions | Action | Input | Expected HTTP | Expected Result | DB Change |
|---|---|---|---|---|---|---|---|
| CREATE-01 | Internal hợp lệ | Admin, role HR + department | Submit | Username/email mới, dept, password đúng | 302 rồi 200 | Thông báo thành công, row Active | +1 User |
| CREATE-02 | Candidate hợp lệ | Admin, role Candidate | Submit | Dept trống | 302 rồi 200 | Row Candidate Active | +1 User +1 Candidate |
| CREATE-03 | Dữ liệu trùng/confirm sai | Account cùng identifier | Submit | Trùng hoặc confirm khác | 200 | Field/global error, password xóa | Không |
| CREATE-04 | Sai quyền/CSRF | HR hoặc Admin | POST | Có/không CSRF | 403 | Không tạo | Không |

## Test Procedure

CREATE-01: login Admin, vào list bấm **Tạo tài khoản**, chọn HR + department, nhập giá trị mới và mật khẩu 8–32, submit; xem thông báo/row; SELECT chỉ đọc `UserId,Username,Email,AccountStatus,RoleId,DepartmentId`. CREATE-02: tạo Candidate, bỏ department; kiểm tra `Candidate.UserId` trỏ đúng User. CREATE-03: thử username cũ, email cũ, confirm sai, internal thiếu dept; kiểm tra không tăng count. CREATE-04: login HR mở form/POST (403); với Admin POST không `_csrf` (403). Không dùng mật khẩu account sản xuất.

## Test Data

Role `HR`, `Candidate`, một Department thật trong môi trường test; username/email mới như `flow_hr_01`, `flow_candidate_01@example.test`. Chỉ đọc DB để đối chiếu, không in hash.

## Debugging Points

| Order | Class | Method | Why |
|---|---|---|---|
| 1 | `AccountController` [PROJECT CODE] | `newAccount`, `create` | Form, validation, flash. |
| 2 | `CreateAccountForm` [PROJECT CODE] | `toCommand` | DTO mapping. |
| 3 | `AccountManagementService` [PROJECT CODE] | `create`, `findDepartment`, uniqueness checks | Root cause business validation. |
| 4 | `UserRepository`/`CandidateRepository` [PROJECT CODE] | lookup, `saveAndFlush`, `save` | DB writes/constraint. |

## Code References

`src/main/java/com/group2/rms/config/SecurityConfig.java`; `src/main/java/com/group2/rms/controller/AccountController.java`; `src/main/java/com/group2/rms/controller/form/CreateAccountForm.java`; `src/main/java/com/group2/rms/service/AccountManagementService.java`; `src/main/java/com/group2/rms/service/AccountListService.java`; `src/main/java/com/group2/rms/repository/UserRepository.java`; `src/main/java/com/group2/rms/repository/CandidateRepository.java`; `src/main/java/com/group2/rms/repository/RoleRepository.java`; `src/main/java/com/group2/rms/repository/DepartmentRepository.java`; `src/main/java/com/group2/rms/entity/User.java`; `src/main/java/com/group2/rms/entity/Candidate.java`; `src/main/resources/templates/admin/accounts/form.html`; `src/main/resources/static/js/account-form.js`; `database/schema/db.sql`.

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
