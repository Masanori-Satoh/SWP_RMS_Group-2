# Test Sidebar/Header khôi phục — 2026-10-04

## 1. Test tự động

PowerShell tại project `SWP_RMS_Group-2`:

```powershell
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
$env:SPRING_SQL_INIT_MODE = 'never'
.\mvnw.cmd '-Dtest=CandidateDashboardWebTests,AdminDepartmentWebTests' test
```

Kết quả thực tế: **21 test PASS**, không failure/error/skipped. CandidateDashboardWebTests có 11 lần chạy, AdminDepartmentWebTests có 10.

| Test | Kiểm tra |
|---|---|
| `CandidateDashboardWebTests.restoredWorkspaceKeepsMainMenusAndSharedLogoutForEveryRole` | Sáu role đều giữ Interview/Profile; Offer hiện theo main; Departments chỉ Admin; anchor Candidate chỉ Candidate; logout form/dialog/CSRF tồn tại |
| `CandidateDashboardWebTests.candidateLogoutKeepsCsrfAndRevokesSession` | Thiếu CSRF bị 403; token hợp lệ logout và redirect login |
| `AdminDepartmentWebTests.adminDashboardAddsDepartmentsAndPreservesMainNavigation` | Giữ menu gốc và bổ sung Departments; HR vẫn có Requisitions |
| Các test còn lại trong hai class | Candidate data/escaping/empty state và Department permission/form/activation/validation vẫn chạy |

Package của hai test: `com.group2.rms`. Kiểm tra bằng MockMvc/Thymeleaf thật; Service/Repository được mock trong MVC slice, không gửi mail hoặc thực hiện CRUD vào DB thật.

## 2. Đối chiếu lịch sử Git

1. `git show e8e02f7:src/main/resources/templates/fragments/sidebar.html` — bản sửa lần trước có menu bổ sung và giữ menu main.
2. `git diff origin/main -- src/main/resources/templates/fragments/sidebar.html` — chỉ 12 dòng bổ sung.
3. `git diff --exit-code origin/main -- src/main/resources/templates/fragments/workspace-header.html` — exit 0.
4. `git diff --exit-code e8e02f7 -- src/main/resources/templates/fragments/sidebar.html src/main/resources/templates/fragments/workspace-header.html` — exit 0.
5. `git stash list` — stash nguồn vẫn còn, không bị pop/drop.
6. So sánh SHA256 với `../.codex-tmp/sidebar-header-recovery-2026-10-04/user-file-hashes-before.json` — bốn file đang sửa từ trước giữ nguyên.

## 3. Test tay — hướng dẫn, chưa chạy

### UI-01: HR

1. Đăng nhập HR Active bằng tài khoản hiện có.
2. Mở Dashboard từ giao diện.
3. Sidebar phải có Job Requisitions, Interview Schedule, Offer Management, My Profile.
4. Bấm từng link; trang module tương ứng phải mở theo quyền backend hiện có.
5. Không có Admin Departments.

### UI-02: Interviewer / Hiring Manager / Director

1. Đăng nhập lần lượt ba role Active.
2. Mỗi role phải có Interview Schedule và My Profile.
3. Offer Management có với Director; không có với Interviewer/Hiring Manager, đúng điều kiện main.
4. Bấm Interview Schedule và My Profile; kiểm tra link thực sự hoạt động.

### UI-03: System Admin

1. Đăng nhập System Admin Active.
2. Menu gốc main vẫn còn: Internal Accounts, Candidate Accounts, API Monitoring, Interview Schedule, Offer Management, My Profile và Job Requisitions.
3. Có thêm Admin Departments.
4. Bấm Departments và My Profile. Không suy ra quyền route chỉ từ việc menu xuất hiện; backend vẫn giữ cấu hình hiện tại.

### UI-04: Candidate

1. Đăng nhập Candidate Active.
2. Có Candidate Dashboard và My Applications, Upcoming Interviews, My Offers, My Notifications, Open Positions.
3. Bấm từng anchor; cuộn đến section đúng trên Dashboard.
4. My Profile vẫn trỏ `/profile` của main.

### UI-05: Logout

1. Với mỗi role, bấm logout ở header.
2. Dialog HTML5 `workspace-logout-dialog` mở; chưa đăng xuất ở bước này.
3. Bấm Hủy hoặc Escape: dialog đóng, phiên vẫn còn.
4. Mở lại dialog, xác nhận logout.
5. Network có POST `/logout` với `_csrf`, redirect `/login?logout`.
6. Truy cập lại trang protected: được yêu cầu đăng nhập.

## 4. Kết quả và giới hạn

- Build/test hai class: **PASS**, 2026-10-04 21:20:20 +07:00.
- Header/sidebar đối chiếu main và stash: **PASS**.
- Bốn file đang sửa sẵn giữ nguyên: **PASS**.
- UI-01..05 trong browser thật: **NOT TESTED**.
- Full suite, SQL Server/JPA mapping: **NOT RERUN** ở đợt này; không dùng kết quả 310 test cũ cho checkout hiện tại.
- Log: `../.codex-tmp/sidebar-header-recovery-2026-10-04/tests-ui-recovery.log`.
