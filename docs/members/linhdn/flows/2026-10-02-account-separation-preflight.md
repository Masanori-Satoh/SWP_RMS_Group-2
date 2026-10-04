# Account separation preflight — luồng Deactivate hiện tại

> Đây là preflight lịch sử trước business confirmation. Implementation/luồng mới dùng [account-lifecycle-flow.md](account-lifecycle-flow.md); đã tách query và scoped actions. Giữ nội dung dưới làm dấu mốc, không dùng như mô tả current backend.

Ngày 02/10/2026. Tách query/list/Dashboard chưa triển khai: chờ xác nhận rule ở [impact](../design/2026-10-02-account-separation-impact.md). Tài liệu này ghi đúng phần đã sửa, không coi flow Candidate management mới đã tồn tại.

## Từng step: mở confirmation → submit

| Step | Package / class / hàm hoặc native callback | Thao tác |
|---|---|---|
| 1 | `com.group2.rms.config.SecurityConfig.filterChain()` | GET `/admin/accounts` chỉ System Admin; Guest login redirect. Authentication/session/CSRF không đổi. |
| 2 | `com.group2.rms.controller.AccountController.list(...)` | Nhận query parameters, gọi service, đưa `accounts` và filter options vào Model. |
| 3 | `com.group2.rms.service.AccountListService.findAccounts(...)` | Specification + `com.group2.rms.repository.UserRepository.findAll(...)` trả Page<AccountRow>. Hiện chưa predicate phân nhóm; role Candidate vẫn có trong list. |
| 4 | `templates/admin/accounts/list.html` | Thymeleaf render Deactivate với `th:if account.status != 'Inactive'`, name/username/URL data attributes. Inactive giữ Edit, không render Deactivate. |
| 5 | `static/js/account-list.js`, DOMContentLoaded → button click callback | Gán textContent name/username và form.action; native `dialog.showModal()`, Cancel.focus(). Không request/ghi dữ liệu. |
| 6 | Cùng file, Cancel click / native Escape → dialog close callback | Đóng, `trigger?.focus()`; không thay account. |
| 7 | Native POST form + Spring Security CSRF | Submit `/admin/accounts/{id}/deactivate`, token từ Thymeleaf; thiếu token/non-Admin bị chặn trước controller. |
| 8 | `com.group2.rms.controller.AccountController.deactivate(int, RedirectAttributes)` | Gọi service, success flash, redirect `/admin/accounts`. |
| 9 | `com.group2.rms.service.AccountListService.deactivate(int)` | `UserRepository.findById` → nếu chưa Inactive: setAccountStatus("Inactive") → save; không delete, không đổi password/role/profile. Missing User trả404. |
| 10 | `AccountController.list(...)` → render sau redirect | Hiện success/status thực; nút Deactivate không còn cho row Inactive. |

Các callback/native APIs đã đối chiếu source JS. Không tuyên bố đã đối chiếu hàm nội bộ framework hay chạy browser POST/live DB trong lượt này.

## Test tương ứng

- `com.group2.rms.SecurityFlowTests.accountListOffersDeactivationOnlyForActiveOrBlockedAccounts()` — render ba status/action/label: PASS.
- `SecurityFlowTests.deactivateRequiresAdminAndCsrf()` — MockMvc auth/CSRF/controller service call: PASS.
- `com.group2.rms.service.AccountListServiceTests.deactivationPreservesAccountAndPasswordHash()` — mocked repository, statusInactive/save, không delete/hash change: PASS.
- [Manual D01–D05 và pending S01–S10](../tests/2026-10-02-account-separation.md): browser/live chưa chạy, scope tách nhóm chưa triển khai.
