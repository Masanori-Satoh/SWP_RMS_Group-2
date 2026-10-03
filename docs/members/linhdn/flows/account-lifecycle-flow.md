# Internal / Candidate account lifecycle — flow hiện tại

Nguồn 02/10/2026: schema hiện tại, working tree và business rule được user xác nhận. Không JWT, không bảng/cột mới, không migration dữ liệu. Package ghi dưới đây là **project code**; framework dispatch/render ghi cơ chế, không tuyên bố source nội bộ library đã đối chiếu.

## 1. Rule và route

| Account group | Query rule | Entry |
|---|---|---|
| Internal | User.role.roleName nằm trong RoleAuthorities.INTERNAL_ROLE_NAMES: System Admin/HR/Hiring Manager/Director/Interviewer | `/admin/accounts` |
| Candidate | User.role.roleName = Candidate | `/admin/candidate-accounts` |

Candidate.account = User qua NOT NULL UNIQUE FK; chỉ dùng đọc profile, không phân nhóm. Missing profile vẫn là Candidate Account và hiện Profile missing; không tự sửa DB. Không dùng Candidate.Email độc lập: name/email/username từ User. Khi hired, tạo User nội bộ mới; username/email vẫn unique toàn hệ thống. Không reuse Candidate role/account.

## 2. Internal list/filter

1. Spring Security dispatch → `com.group2.rms.config.SecurityConfig.filterChain()`: Admin matcher; `com.group2.rms.security.AccountSessionGuardFilter` kiểm tra session persisted account. Auth/CSRF behavior cũ giữ nguyên.
2. `com.group2.rms.controller.AccountController.list(search,roleId,departmentId,status,sort,page,model)` nhận GET params.
3. `com.group2.rms.service.AccountListService.findAccounts(...)` → private `findUsers(...,candidate=false)` xây JPA Specification với internal role predicate **và** search/role/department/status. Sort stable với UserId, page size10.
4. `com.group2.rms.repository.UserRepository.findAll(Specification,Pageable)` EntityGraph role/department → Page<User>.
5. Service map `AccountRow`, Controller thêm Model accounts/roles/departments/current filters; `findRoles()` lọc persisted roles theo shared supported set.
6. Thymeleaf `admin/accounts/list.html` render HTML thật; GET form/pagination giữ params. JS chỉ disclosure/dialog, không dữ liệu account arrays.

## 3. Create/Edit Internal

| Step | Package/class/method | Kết quả |
|---|---|---|
| C1 | `com.group2.rms.controller.AccountController.newAccount()/prepareCreate()` | CreateAccountForm, internal roles + Department, render form; create password/confirm, không status picker. |
| C2 | `AccountController.create()` + `com.group2.rms.controller.form.CreateAccountForm.toCommand()` | Bean validation/confirm match; gọi createInternal. |
| C3 | `com.group2.rms.service.AccountManagementService.createInternal()` → requireInternalRole → create() | Reject Candidate/unsupportedrole trước User save; normalize/unique identity/internalDepartment required/password8–32; BCrypt hash/Active. |
| C4 | `com.group2.rms.repository.UserRepository.saveAndFlush()` | INSERT User mới. Không Candidate insert trong internal create; redirect flash khi thành công. |
| E1 | `AccountController.editAccount()` → `AccountManagementService.findForEdit()` → requireInternal | Candidate UserId trả404; DTO internal; username readonly/no password fields. |
| E2 | `AccountController.update()` → findForEdit gate ngay trước validation handling → UpdateAccountForm.toCommand() → updateInternal() | URL Candidate ngay cả invalid body cũng404; roleCandidate bị AccountFieldException gắn role. |
| E3 | `AccountManagementService.updateInternal()` → requireInternal + requireInternalRole → update()` | `update` guard loại account trước field gán; email unique excluding current; status3allowed; Department rule. Username/hash không assigned; không create Candidate profile. |
| E4 | `UserRepository.saveAndFlush()` | UPDATE User; flash+redirect; validation/conflict render fields/errors, không fake success. |

`AccountManagementService.update()` vẫn là shared service entry, chặn cross-lifecycle cho cả caller ngoài controller. Candidate same-role update không làm role conversion; không có Candidate edit route/UI mới. Public `com.group2.rms.service.CandidateRegistrationService.register()` vẫn gọi create() với Candidate role/Departmentnull, tạo User + Candidate cùng transaction; controller đăng ký/login/logout không thay đổi.

## 4. Candidate Accounts

1. GET `/admin/candidate-accounts` qua Admin-only matcher → `com.group2.rms.controller.CandidateAccountController.list(search,status,sort,page,model)`.
2. `com.group2.rms.service.AccountListService.findCandidateAccounts(...)` → findUsers(candidate=true) chỉ Candidate role; search/status/sort/page server-side.
3. `UserRepository.findAll(...)` lấy page User. `com.group2.rms.repository.CandidateRepository.findAllByAccountUserIdIn(ids)` batch profiles cho page, không query từng row.
4. Map CandidateRow: UserId/name/email/username/status/CreatedAt và profileId từ FK. Không Department, internal role hierarchy, application counts guessed.
5. `admin/accounts/candidates.html` render linkage/missing/date/filter/pagination. Candidate profile management riêng; không link edit/detail/upload/activate chưa hỗ trợ. No Create Candidate Account tại Admin.

## 5. Scoped deactivation

1. `static/js/account-list.js` DOMContentLoaded → click [data-deactivate-account] → set name/username textContent, form.action → native dialog.showModal() → Cancel.focus(). Không network.
2. Cancel/ESC → close event → trigger.focus(). Inactive không có trigger.
3. Submit form POST với CSRF → Internal `AccountController.deactivate()` hoặc Candidate `CandidateAccountController.deactivate()`.
4. `AccountListService.deactivate(id)` / `deactivateCandidate(id)` → private deactivateScoped(id,isCandidate): UserRepository.findById, current-role scope guard404 nếu sai group, statusInactive idempotent, save nếu cần.
5. Không repository.deleteById hoặc profile/history delete. Controller success flash+redirect đúng list. Session/auth guard hiện có từ chối Inactive/Blocked.

## 6. Admin Dashboard

1. `com.group2.rms.controller.DashboardAccessController.dashboard()` → `com.group2.rms.service.DashboardService.forUsername(username)` tìm persisted UserActive và dispatch role.
2. Private `DashboardService.admin()` → `com.group2.rms.repository.DashboardMetricsRepository.internalAccountStatuses()/candidateAccountStatuses()`: JPQL GROUP BY status với đúng role predicate.
3. `DashboardService.accountSummary()` map Total=sum status, Active/Inactive/Blocked từ rows (vắng status=0), không hardcoded metric. `DashboardView.AccountSummary` chứa hai groups/route links; các role khác dùng constructor View cũ với summaries empty.
4. `ApiMonitoringService.rows()` vẫn lấy persisted integration state riêng. DashboardView.breakdowns health không trộn counts account.
5. Thymeleaf dashboard/index.html: Accounts→2summaryrows, System Health riêng, contextual View Internal/Candidate Accounts thật. Sidebar Accounts/System groups, active nav aria-current; mobile JS behavior cũ giữ nguyên.

## 7. Verification và external test

Chín suites initial41PASS/final42PASS, actual SELECT-only SQLServer internal10/Candidate265, category/status totals khớp; historicalinternalprofiles0/CandidateDepartments0. Root browser fixture112/112 initial và119/119 finalconfirmationPASS. Final gate/review ở [test doc](../tests/2026-10-02-account-separation.md) và [report](../design/2026-10-02-account-separation-report.md). HTTP startup gặp Tomcat loopback socket environment failure; không claim live login/deactivation/E2E từ MockMvc/static preview.
