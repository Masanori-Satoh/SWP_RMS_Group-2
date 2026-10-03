# Admin xóa theo nghĩa vô hiệu hóa tài khoản

> **Cập nhật 02/10/2026:** UI gọi **Deactivate**, không Delete; Inactive không có action. Internal/Candidate có route và guard riêng. Chi tiết hiện tại: [account-lifecycle-flow.md](account-lifecycle-flow.md). Nội dung dưới giữ làm lịch sử tài liệu; không hard delete.

**Nguồn:** Account List modal/JS/controller/service/schema. Không hard delete. Browser/DB thực tế chưa chạy trong lượt này.

## End-to-End Execution Flow

```text
GET /admin/accounts → list.html có nút Xóa từng User
 → [PROJECT CODE] account-list.js mở <dialog>, gán action URL /admin/accounts/{id}/deactivate
 → Admin bấm Vô hiệu hóa → POST + CSRF
 → [SPRING FRAMEWORK] SecurityFilterChain: ROLE_SYSTEM_ADMIN + CSRF
 → [PROJECT CODE] AccountController.deactivate(userId)
 → AccountListService.deactivate(userId) [transaction]
 → UserRepository.findById → set AccountStatus=Inactive → UserRepository.save
 → commit → flash success → 302 /admin/accounts
 → session của User đó bị AccountSessionGuardFilter thu hồi ở request sau
```

## Detailed Execution Trace

| Step | Loại; class.method; package/file | Input → output → next |
|---|---|---|
| 1 | [PROJECT CODE] `AccountController.list(...)` + `admin/accounts/list.html` | Admin GET list; row có nút `data-deactivate-account` với id/name/URL. |
| 2 | [PROJECT CODE] event listener trong `src/main/resources/static/js/account-list.js` | Click Xóa → tên và action gán vào `#deactivate-form`, `dialog.showModal()`; Hủy → `dialog.close()` không POST. |
| 3 | [SPRING FRAMEWORK] role/CSRF từ [PROJECT CODE] `SecurityConfig.filterChain(...)`; `com.group2.rms.config` | POST `/admin/accounts/{userId}/deactivate` + token → Admin cho qua; guest/login, role khác/403, token thiếu/403. |
| 4 | [PROJECT CODE] `AccountController.deactivate(int,RedirectAttributes)`; `com.group2.rms.controller` | Path `userId` → `AccountListService.deactivate(userId)` → flash “Vô hiệu hóa tài khoản thành công.”/redirect. |
| 5 | [PROJECT CODE] `AccountListService.deactivate(int)`; `com.group2.rms.service` | `@Transactional`; `findById`; nếu chưa Inactive thì set Inactive và `users.save`; nếu đã Inactive thì idempotent, không save. |
| 6 | [PROJECT CODE] `AccountSessionGuardFilter.doFilterInternal(...)`; `com.group2.rms.security` | User bị vô hiệu hóa gửi request tiếp theo → tra DB thấy không Active → logout + 302 `/login?session-expired`. |

## Failure / Alternative Flows

- Hủy modal: không gửi POST, status giữ nguyên. UserId không tồn tại: 404 từ `ResponseStatusException`.
- Account đã Inactive: Service không ghi thêm, controller vẫn flash success.
- Admin vô hiệu hóa chính mình: POST có thể hoàn tất/redirect, request kế tiếp guard logout; chưa xác minh browser thực tế.
- Thiếu CSRF/không Admin: 403. Không có `deleteById()` ở flow này.

## Database Interaction

| Order | Repository Method | Entity | Table | Operation | Purpose |
|---|---|---|---|---|---|
| 1 | `UserRepository.findById` | User | `User` | SELECT | Tìm account. |
| 2 | `UserRepository.save` nếu status khác Inactive | User | `User` | UPDATE | Đặt `AccountStatus=Inactive`. |
| 3 | `UserRepository.findByUsernameIgnoreCase` ở guard | User | `User` | SELECT | Thu hồi session sau đó. |

Không DELETE User/Candidate/Application/history. SQL UPDATE do JPA sinh động, chưa capture.

## Data Transformation

Row `id/name` → JS `form.action` + dialog → path variable `userId` → User JPA → status `Inactive` → DB; flash message → list. CSRF lấy từ hidden input modal.

## External Test Cases

| ID | Scenario | Preconditions | Action | Input | Expected HTTP | Expected Result | DB Change |
|---|---|---|---|---|---|---|---|
| DEACT-01 | Hủy | Admin + User test Active | Xóa rồi Hủy | Click UI | Không POST | Status vẫn Active | Không |
| DEACT-02 | Xác nhận | Admin + User test Active | Xóa rồi Vô hiệu hóa | POST + CSRF | 302 rồi 200 | Flash success; row Inactive | UPDATE status |
| DEACT-03 | Login/session cũ | DEACT-02; target đã login | Thử login/refresh | Credentials/session | 302 | Không vào protected route | Không thêm |
| DEACT-04 | Sai quyền/token | HR/Admin | POST thủ công | Có/không CSRF | 403 | Status không đổi | Không |

## Test Procedure

Chỉ dùng account test Active, tuyệt đối không chọn Admin chính. DEACT-01: bấm Xóa, đọc nội dung modal, bấm Hủy, refresh list, status Active. DEACT-02: bấm Xóa và Vô hiệu hóa, xem thông báo, SELECT read-only `UserId,AccountStatus` và kiểm tra Candidate/Application liên quan còn. DEACT-03: dùng tab đã login target refresh Dashboard rồi thử login lại; phải bị từ chối. DEACT-04: login HR gửi POST đúng token (403), Admin gửi không token (403). Kiểm tra HTTP bằng DevTools Network.

## Test Data

Admin Active và User test Active khác; nếu kiểm tra lịch sử thì target có Candidate/Application test. SQL chỉ đọc, không dump hash. `dbo.[User].AccountStatus` có CHECK Active/Inactive/Blocked trong schema.

## Debugging Points

| Order | Class | Method | Why |
|---|---|---|---|
| 1 | `account-list.js` [PROJECT CODE] | Click listener | Modal/action URL/Hủy. |
| 2 | `AccountController` [PROJECT CODE] | `deactivate` | Path/flash. |
| 3 | `AccountListService` [PROJECT CODE] | `deactivate` | Idempotence/update. |
| 4 | `AccountSessionGuardFilter` [PROJECT CODE] | `doFilterInternal` | Session target sau update. |

## Code References

`src/main/java/com/group2/rms/config/SecurityConfig.java`; `src/main/java/com/group2/rms/controller/AccountController.java`; `src/main/java/com/group2/rms/service/AccountListService.java`; `src/main/java/com/group2/rms/security/AccountSessionGuardFilter.java`; `src/main/java/com/group2/rms/repository/UserRepository.java`; `src/main/java/com/group2/rms/entity/User.java`; `src/main/resources/templates/admin/accounts/list.html`; `src/main/resources/static/js/account-list.js`; `database/schema/db.sql`.

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
