# Danh sách tài khoản

**Nguồn:** `AccountController`, `AccountListService`, `UserRepository`, template và schema. Bao gồm User nội bộ và Candidate. Browser thật chưa chạy trong lượt này.

## End-to-End Execution Flow

```text
System Admin GET /admin/accounts?search=&roleId=&departmentId=&status=&sort=&page=
 → [SPRING FRAMEWORK] SecurityFilterChain: ROLE_SYSTEM_ADMIN
 → [PROJECT CODE] AccountSessionGuardFilter
 → AccountController.list()
 → AccountListService.findAccounts() + findRoles() + findDepartments()
 → UserRepository.findAll(Specification,PageRequest) với EntityGraph(role,department)
 → SQL Server User/Role/Department → Page<AccountRow>
 → model → admin/accounts/list.html → HTTP 200
```

## Detailed Execution Trace

| Step | Loại; class.method; package/file | Input → output → next |
|---|---|---|
| 1 | [PROJECT CODE] `SecurityConfig.filterChain(...)`; `com.group2.rms.config`; [SPRING FRAMEWORK] authorization | GET `/admin/accounts` chỉ `ROLE_SYSTEM_ADMIN`; guest → login, role khác → 403. |
| 2 | [PROJECT CODE] `AccountSessionGuardFilter.doFilterInternal(...)`; `com.group2.rms.security` | Reload User theo username để xác nhận Active và role không đổi → controller. |
| 3 | [PROJECT CODE] `AccountController.list(...)`; `com.group2.rms.controller` | Query params `search,roleId,departmentId,status,sort,page` với default; gọi Service, đặt model → view. |
| 4 | [PROJECT CODE] `AccountListService.findAccounts(...)`; `com.group2.rms.service` | Trim/lowercase search, escape LIKE, filter fullName/email/username, role/dept/status hợp lệ; sort whitelist; page size 10 → `UserRepository.findAll`. Negative page → 0. |
| 5 | [PROJECT CODE] `UserRepository.findAll(Specification,Pageable)`; `com.group2.rms.repository` | JPA Specification + `@EntityGraph(role,department)` → Page<User>, count; Service map từng User thành `AccountRow` không có password hash. SQL cụ thể do Hibernate tạo, chưa capture. |
| 6 | [PROJECT CODE] `AccountListService.findRoles/findDepartments`; repositories `RoleRepository.findAll`, `DepartmentRepository.findAll` | SELECT danh mục để render bộ lọc. |
| 7 | [SPRING FRAMEWORK] Thymeleaf render `admin/accounts/list.html` | Rows, filter, pagination, liên kết Tạo/Sửa và nút Xóa → 200 HTML. Internal renderer method **Not verified at source-code level**. |

## Failure / Alternative Flows

- Guest: 302 login; HR/Candidate: 403. Status query không nằm trong Active/Inactive/Blocked thì filter status bỏ qua; sort lạ về tên A–Z; page âm về 0.
- Không tìm thấy: Page rỗng; vẫn 200. DB lỗi: không có dữ liệu giả/fallback.

## Database Interaction

| Order | Repository Method | Entity | Table | Operation | Purpose |
|---|---|---|---|---|---|
| 1 | `UserRepository.findByUsernameIgnoreCase` | User | `User` | SELECT | Session guard. |
| 2 | `UserRepository.findAll(Specification,Pageable)` | User/Role/Department | `User`, `Role`, `Department` | SELECT + count | Filter, sort, page. |
| 3 | `RoleRepository.findAll(Sort)` | Role | `Role` | SELECT | Dropdown role. |
| 4 | `DepartmentRepository.findAll(Sort)` | Department | `Department` | SELECT | Dropdown department. |

## Data Transformation

GET params → normalized filters/`Specification<User>`/`PageRequest` → `Page<User>` → `Page<AccountRow>` → Thymeleaf table. Candidate hiện như User có RoleName Candidate; profile Candidate không trộn vào list.

## External Test Cases

| ID | Scenario | Preconditions | Action | Input | Expected HTTP | Expected Result | DB Change |
|---|---|---|---|---|---|---|---|
| LIST-01 | Admin xem tất cả | Admin + Candidate/Internal có sẵn | Mở list | GET route | 200 | Có cả Candidate và internal | Không |
| LIST-02 | Lọc/tìm/phân trang | >10 User test | Dùng controls | search/role/status/page | 200 | Đúng 10/page và filter | Không |
| LIST-03 | Không đủ quyền | HR/guest | Mở list | GET route | 403/302 | Không lộ dữ liệu | Không |

## Test Procedure

LIST-01: login Admin, bấm **Quản lý tài khoản**, đối chiếu tên/role của một Candidate và HR với DB. LIST-02: nhập keyword, chọn role/status, bấm **Áp dụng**, dùng **Sau/Trước**; so kết quả với SELECT read-only theo cùng điều kiện. LIST-03: logout rồi mở route (về login); login HR rồi mở route (403). Quan sát Network status, không chỉ ảnh giao diện.

## Test Data

Ít nhất 1 Candidate, 1 HR; muốn test phân trang cần >10 User test. Không tạo account thật chỉ để thử. `User.RoleId` liên kết `Role` và `DepartmentId` có thể NULL cho Candidate.

## Debugging Points

| Order | Class | Method | Why |
|---|---|---|---|
| 1 | `AccountSessionGuardFilter` [PROJECT CODE] | `doFilterInternal` | Quyền session. |
| 2 | `AccountController` [PROJECT CODE] | `list` | Query params/model. |
| 3 | `AccountListService` [PROJECT CODE] | `findAccounts` | Specification/sort/page. |
| 4 | `UserRepository` [PROJECT CODE] | `findAll` | Query và EntityGraph. |

## Code References

`src/main/java/com/group2/rms/config/SecurityConfig.java`; `src/main/java/com/group2/rms/security/AccountSessionGuardFilter.java`; `src/main/java/com/group2/rms/controller/AccountController.java`; `src/main/java/com/group2/rms/service/AccountListService.java`; `src/main/java/com/group2/rms/repository/UserRepository.java`; `src/main/java/com/group2/rms/repository/RoleRepository.java`; `src/main/java/com/group2/rms/repository/DepartmentRepository.java`; `src/main/java/com/group2/rms/entity/User.java`; `src/main/resources/templates/admin/accounts/list.html`; `database/schema/db.sql`.

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
