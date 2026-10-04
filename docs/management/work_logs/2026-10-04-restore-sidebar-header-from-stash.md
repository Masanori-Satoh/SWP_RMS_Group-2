# Khôi phục Sidebar/Header từ lịch sử Git — 2026-10-04

## Yêu cầu

Khôi phục menu Interview Schedule, Offer Management, My Profile và dialog logout của main; giữ các menu bổ sung Admin Departments/Candidate Dashboard bằng tiếng Anh.

## Nguồn tìm thấy

- HEAD lúc bắt đầu: `7fb9401`, nhánh `feature/fix-architecture-resolved-conflict-hoang`.
- `origin/main` sau fetch: `1182ffe`, chứa PR #14 từ commit `65129ca`.
- Bản fix lần trước nằm trong stash `e8e02f7` — `temp before checking main conflict`, không nằm trong commit hiện tại.
- Trong stash: header có diff bằng 0 với main; sidebar chỉ thêm 12 dòng, không xóa dòng nào của main.
- Commit `7fb9401` không thay hai fragment so với `4191a8a`. Không quy kết việc mất fix cho commit dọn thư mục; phiên bản sửa được giữ riêng trong stash.

## Quá trình

1. Đọc Git status, log theo hai file, stash list và bản gốc origin/main.
2. Đối chiếu stash e8e02f7; xác nhận đúng bản cần phục hồi.
3. Sao lưu diff của working tree và archive bốn file sẽ khôi phục vào `../.codex-tmp/sidebar-header-recovery-2026-10-04/`.
4. Lưu SHA256 bốn file đang được người dùng chỉnh sửa để kiểm tra sau thao tác.
5. Dùng `git restore --source=e8e02f7 --worktree -- <file>` cho riêng hai fragment và hai test UI cùng bản fix. Không áp dụng toàn bộ stash, không pop/drop stash.
6. Chạy Maven Wrapper cho CandidateDashboardWebTests và AdminDepartmentWebTests.
7. Kiểm tra header khớp main, mọi dòng main còn trong sidebar, bản khôi phục khớp stash và bốn file đang sửa giữ nguyên.

## File thay đổi

| File | Thay đổi / lý do |
|---|---|
| `src/main/resources/templates/fragments/sidebar.html` | Khôi phục menu gốc main, thêm 12 dòng Admin Departments và các anchor Candidate Dashboard; phần bổ sung bằng tiếng Anh |
| `src/main/resources/templates/fragments/workspace-header.html` | Khôi phục nguyên bản main: Profile, Menu, POST logout và HTML5 dialog; không dịch role bằng ternary |
| `src/test/java/com/group2/rms/CandidateDashboardWebTests.java` | Khôi phục regression test menu/header/logout cho sáu role và assertion dialog main |
| `src/test/java/com/group2/rms/AdminDepartmentWebTests.java` | Khôi phục assertion Departments được bổ sung mà menu main vẫn còn |

File tài liệu tạo mới: báo cáo này và [kịch bản test](../../members/linhdn/tests/2026-10-04-sidebar-header-recovery.md).

## Luồng được khôi phục

- Template workspace gọi `fragments/sidebar :: workspace(activeMenu, role)` để hiện link gốc theo điều kiện role của main.
- Candidate có thêm anchor `/dashboard#applications`, `#interviews`, `#offers`, `#notifications` và `/jobs`.
- System Admin có thêm `/admin/departments`; quyền backend hiện có được giữ nguyên.
- Header gọi `workspace-logout-dialog.showModal()`; Hủy đóng dialog, xác nhận submit `workspace-logout-form` tới POST `/logout`.
- Thymeleaf POST form có `_csrf`; Spring Security xử lý logout và redirect `/login?logout`. Không có Controller logout riêng.

## Verification

- Maven Wrapper: **PASS — 21 test, 0 failure, 0 error, 0 skipped**, kết thúc 2026-10-04 21:20:20 +07:00.
- Hai Spring MVC test contexts: **PASS**.
- Header so với origin/main: **PASS**, diff bằng 0.
- Sidebar so với origin/main: **PASS**, chỉ thêm 12 dòng, không xóa nội dung gốc.
- Bốn file đang sửa sẵn (`ApiMonitoringService.java`, `DashboardService.java`, `candidate/job-detail.html`, `CareerFlowTests.java`): **PASS**, SHA256 không thay đổi.
- Full Maven suite / full JPA Context / browser thật: **NOT RERUN** trong đợt khôi phục hai fragment này. Kết quả 310 test của lần recovery trước không đại diện cho HEAD hiện tại.

## Trạng thái bàn giao

- Thay đổi nằm trong working tree, chưa commit/push/rebase.
- Stash gốc vẫn được giữ. Không áp dụng các thay đổi backend khác nằm trong stash.
- Header main vốn có nhãn dialog tiếng Việt; giữ nguyên copy và CSS/JS của tác giả. Sidebar được khôi phục bằng tiếng Anh.
- Log và backup: `../.codex-tmp/sidebar-header-recovery-2026-10-04/`.
