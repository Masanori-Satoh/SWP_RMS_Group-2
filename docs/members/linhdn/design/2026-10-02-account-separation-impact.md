# System Admin — tách Internal và Candidate: impact trước khi sửa backend

Ngày: 02/10/2026. Nguồn: working tree, `database/schema/db.sql`, `database/seeds/seed_data.sql`, yêu cầu đính kèm của người dùng. Không sửa/chạy schema hoặc seed. Quy tắc mới thay yêu cầu UI cũ về một danh sách trộn và tên Delete.

## Audit 7 điểm

1. `Candidate.UserId` là FK tới `User.UserId`, NOT NULL, UNIQUE. Candidate sở hữu mapping `Candidate.account` OneToOne. Không tồn tại hồ sơ Candidate không có User theo schema; schema vẫn cho phép User chưa có hồ sơ.
2. `DashboardService.admin()` gọi `DashboardMetricsRepository.accounts()` và `accountsWithStatus(status)`: JPQL đếm mọi User, trộn hai nhóm.
3. `AccountListService.findAccounts(...)`: Specification chỉ tìm tên/email/username, roleId, departmentId, status; không có predicate nhóm tài khoản. `findRoles()` trả mọi role.
4. Seed định nghĩa sáu role: System Admin, HR, Hiring Manager, Director, Interviewer, Candidate. RoleName UNIQUE nhưng schema không CHECK danh sách role; không suy ra role nội bộ mới từ tên chưa được phê duyệt.
5. **NEED CONFIRMATION:** `AccountManagementService.update()` giữ Candidate profile khi đổi sang role nội bộ. Role hiện tại dùng cho auth/Dashboard, còn liên kết Candidate dùng để giữ profile/history. Hai tiêu chí có thể khác nhau; không tự chọn tiêu chí truy vấn.
6. Có thể tái sử dụng GET/POST `/admin/accounts`, `/new`, `/{id}/edit`, `/{id}/deactivate` cho nội bộ, sau khi bổ sung kiểm tra scope ở backend. `/register` tiếp tục riêng và dùng `CandidateRegistrationService.register()` → `AccountManagementService.create()`.
7. Cần GET `/admin/candidate-accounts` cùng controller/list query scoped; POST deactivation scoped nếu tái sử dụng hành vi hiện có. Thêm đúng route mới vào matcher System Admin. Không đổi login/session/CSRF/auth foundation hoặc schema.

## Quyết định đang chờ

**Vấn đề:** User role HR vẫn có thể có Candidate profile do lịch sử đổi role.

**Schema hiện tại:** không có CHECK ràng buộc Candidate.UserId với User.RoleId.

**Code hiện tại:** update đổi role và giữ hồ sơ; quyền truy cập theo role.

**A — Đề xuất:** phân nhóm theo role hiện tại. Candidate.UserId để đọc hồ sơ; User role Candidate thiếu profile hiện cảnh báo ở Candidate Accounts. Người đã trở thành nhân viên thuộc nhóm nội bộ; lịch sử hồ sơ vẫn giữ.

**B:** phân nhóm theo sự tồn tại Candidate.UserId. Người có role HR nhưng còn hồ sơ sẽ bị xếp nhóm bên ngoài; cần làm rõ User role Candidate thiếu profile. Không thay auth theo tiêu chí này nếu chưa được yêu cầu.

Không sửa query/service/form scope/Dashboard counts trước câu trả lời. Không tự đổi role, xóa hồ sơ, ghép email hoặc tạo profile để ép dữ liệu phù hợp.

## Impact

| Loại | Code bị ảnh hưởng | Vấn đề / thay đổi cần làm |
|---|---|---|
| KEEP | entities, schema, seeds, registration, login/logout, password/reset | Giữ dữ liệu, mapping và auth; không bảng/cột mới, JWT hay chuyển dữ liệu. |
| MODIFY, chờ xác nhận | AccountListService, AccountManagementService, AccountController | Predicate nhóm thật; roles nội bộ; chặn URL/form giả mạo Candidate trong luồng nội bộ; preserve username/hash/unique/status. |
| CREATE, chờ xác nhận | Candidate account list/controller/query/template | Dữ liệu User + profile FK, không Department/role hierarchy; search/status/page; chỉ hành động backend thực sự hỗ trợ. |
| MODIFY, chờ xác nhận | DashboardMetricsRepository, DashboardService/View/template | Total/Active/Inactive/Blocked theo hai nhóm, cùng tiêu chí với list; System Health tách riêng. |
| MODIFY, chờ xác nhận | SecurityConfig, workspace sidebar | Matcher Admin cho route mới; nhóm Accounts/System và link thật. Không đổi legacy Requisition fragment hoặc merge index. |
| MODIFY, đã thực hiện | admin/accounts/list.html | Delete → Deactivate; ẩn nút khi Inactive; modal/CSRF/action/JS hiện có giữ nguyên. |
| MODIFY, đã thực hiện | SecurityFlowTests | Test render ba status, kiểm tra action/accessible name và Inactive vẫn Edit được. |
| CREATE/MODIFY | docs/test/flow/log | Ghi audit, tình trạng chờ và test từng bước; không gọi việc tách nhóm là hoàn tất. |

## Thiết kế giữ lại

Mộc: Lora/Source Sans 3, warm paper/forest, hairline, compact table, native controls, keyboard focus, contained horizontal table scroll. Hai nhóm Dashboard dạng summary rows, không tám KPI cards. Candidate oversight không có Create Candidate/Department selector. UI English; dữ liệu DB giữ nguyên ngôn ngữ.

UI UX Pro Max đã tra guidance table/action/status: giữ local horizontal scroll và feedback hiện có. Bulk edit không áp dụng vì ngoài scope/backend hiện tại. Impeccable craft floor đã đọc trước edit thuật ngữ; critique/audit/polish toàn bộ surface sẽ thực hiện sau khi có quy tắc phân nhóm và implementation hoàn chỉnh. Không chạy vòng polish cho cấu trúc chưa được quyết định.

## Điểm tiếp tục

Nhận quyết định A/B → shared rule cho queries/list/dashboard → scoped guards/forms → compile → UI/sidebar → tests DB SELECT-only và MockMvc → review hai agent và browser batched 1440/1024/768/375 → một fix batch/confirmation → report. Không claim production/E2E bằng test mock/static.

## Quyết định cuối đã nhận và review mismatch (02/10)

Người dùng xác nhận hai lifecycle riêng: Candidate không chuyển thành nhân viên; khi tuyển chính thức tạo User nội bộ **mới**, giữ hoặc deactivation Candidate theo hành động hiện có. Phân nhóm **theo role hiện tại**, không theo lịch sử Candidate profile. Chặn cả internal→Candidate và Candidate→internal trong account update. Nội bộ chỉ nhận năm role đã được RoleAuthorities và seed hỗ trợ; không tự coi một role lạ là internal.

Đã review root cause `AccountManagementService.update()` trước sửa: setRole cho mọi role và tự tạo Candidate profile khi đổi internal→Candidate. Đã chặn đổi loại trước gán field; bỏ auto-create profile trong update. Candidate profile chỉ tạo trong create/registration transaction. Giữ `create()` để public registration; thêm `createInternal()/updateInternal()` guards cho Admin routes. Không sửa các record cũ hoặc FK để ép phù hợp.

DB vẫn UNIQUE Username/Email toàn hệ thống: User nhân viên mới phải dùng username/email khác account Candidate; deactivation không giải phóng email. Không tự tái dùng email hoặc bỏ unique constraint. Nếu dữ liệu lịch sử đã được chuyển role, giữ nguyên và báo audit count; không tự reverse migration.
