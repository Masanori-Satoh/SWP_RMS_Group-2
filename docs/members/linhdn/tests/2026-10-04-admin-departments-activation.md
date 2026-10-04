# Test Department CRUD / Account activation / Admin Dashboard — 04/10/2026

## 1. Kết quả đã chạy

| Nhóm | Kết quả | Phạm vi bằng chứng |
|---|---|---|
| Compile / test compilation | PASS | Maven compile và test gates |
| DepartmentServiceTests | 9/9 PASS | Create/unique/manager/status/delete-soft, lỗi DB duplicate vs FK dùng mock |
| AccountActivationDepartmentTests | 5/5 PASS | Restore, preserve credentials/lifecycle, Blocked/wrong-group, Inactive assignment |
| AdminDepartmentDashboardTests | 1/1 PASS | Summary thật từ repository contract, phân nhóm internal/Candidate |
| AdminDepartmentWebTests | 10/10 PASS | MVC record binding, template, CSRF, Admin-only, central field-error handler, original Inactive dropdown |
| DepartmentReadOnlyTests | 1/1 PASS | Spring Context, Hibernate validate, SELECT/query/pagination/manager/count trên DB hiện tại |
| AccountManagementServiceTests + AccountListServiceTests | 9/9 PASS | Account regression, preserve username/hash/lifecycle |
| CandidateDashboardWebTests + ServiceTests + ReadOnlyTests | 10/10 PASS | Regression Candidate panel/privacy/login/logout, JPA SELECT |
| Tổng Java chọn lọc | **45/45 PASS** | Không phải toàn bộ project test suite |
| Node account-ui.test.cjs | **2/2 PASS** | Cancel không POST, confirm một lần, giữ route/CSRF |
| Chrome round1 / round2 | **115/115 PASS mỗi lượt** | HTML render thật từ MockMvc; 1440/1024/768/375, focus, modal, ARIA/labels/tap size, assets/CSRF/overflow |
| Live HTTP + CRUD ghi DB + browser E2E | **NOT TESTED** | Các case manual bên dưới chưa được thực hiện trong đợt này |
| SQL/schema/seed thay đổi | **NO** | Không chỉnh file DB, không test ghi dữ liệu thật |

Lần đầu có 1 ERROR ở DepartmentServiceTests do restub Mockito kích hoạt mock đã thenThrow. Đã dùng doThrow và rerun PASS; không che lỗi ứng dụng. Báo cáo ở target/surefire-reports; logs target/admin-departments-*.log.

## 2. Chạy lại automated tests

Từ root `SWP_RMS_Group-2`, Java21 + Maven hiện có. Các ReadOnlyTests khai báo validate/never và readOnly transaction; không chạy script tạo/xóa DB.

```powershell
mvn.cmd -q '-Dtest=DepartmentServiceTests,AccountActivationDepartmentTests,AdminDepartmentDashboardTests,AdminDepartmentWebTests,DepartmentReadOnlyTests,AccountManagementServiceTests,AccountListServiceTests,CandidateDashboardWebTests,CandidateDashboardServiceTests,CandidateDashboardReadOnlyTests' '-Dspring.jpa.show-sql=false' test
node --test src/test/js/account-ui.test.cjs
```

Nếu Maven không có trong PATH, dùng đường dẫn Maven đã cài. Không đổi datasource sang DB production để chạy manual CRUD.

### Preview / Chrome

1. Chạy AdminDepartmentWebTests để tạo HTML sanitized trong target/admin-department-preview.
2. Terminal A: `py docs/members/linhdn/scripts/admin-department-preview-server.py --port 8773`.
3. Terminal B: `node docs/members/linhdn/scripts/check-admin-department-ui.mjs http://127.0.0.1:8773 round1`.
4. Xem JSON/png ở [assets](assets/2026-10-04-admin-departments/round2.json). Dừng server bằng Ctrl+C.

Checker chỉ dùng fixture; POST xác nhận được intercept để kiểm tra route/CSRF. Không giả lập lưu DB. Script Chrome chỉ đóng/xóa profile tạm do chính nó tạo.

## 3. Điều kiện test ứng dụng thật

- Dùng DB phát triển riêng, có schema và tài khoản Admin/internal/Candidate sẵn; không chạy script DROP/CREATE của schema để test.
- Khi run, override `spring.jpa.hibernate.ddl-auto=validate` và `spring.sql.init.mode=never`; config working tree hiện là update nhưng đợt này giữ nguyên file theo yêu cầu.
- Đăng nhập Admin, dùng các tài khoản thử nghiệm, giữ một Admin khác hoạt động nếu test deactivation Admin.
- Chuẩn bị tên phòng ban unique như `TEST-DEPT-<thời điểm>`, một internal Active và một Candidate để thử sai manager; không ghi password/token thật trong bằng chứng.
- Mọi case dưới đây đang **MANUAL / NOT RUN**. Ghi kết quả thực tế/PASS/FAIL và ảnh Network/DB SELECT sau khi chạy.

## 4. Case manual từng bước

### D01 — Entry và Dashboard

1. Đăng nhập System Admin → Tổng quan hệ thống.
2. Kiểm tra tổng/Active/Inactive của phòng ban với SELECT bảng Department (chỉ đọc).
3. Bấm link **Quản lý phòng ban** hoặc menu **Phòng ban**.
4. Kỳ vọng: mở danh sách đúng route; menu đánh dấu hiện tại; Admin không có Job Requisitions. Đăng nhập HR ở session khác: vẫn có link requisitions.

### D02 — Tạo phòng ban

1. Bấm **Tạo phòng ban**.
2. Nhập tên TEST unique <=100 ký tự, bấm tạo.
3. Kỳ vọng: thông báo thành công, row Active; ManagerId NULL; không tạo User/Requisition tự động.
4. Kiểm tra DB SELECT tên/trạng thái; ghi ID cho case sau.

### D03 — Required / length

1. Submit tên rỗng hoặc chỉ khoảng trắng. Nếu browser chặn rỗng, thử khoảng trắng để kiểm tra server.
2. Gửi POST >100 ký tự bằng DevTools trong session test, giữ CSRF hợp lệ.
3. Kỳ vọng: lỗi field trên form, không insert; form/summary có focus và aria-invalid.

### D04 — Unique tên

1. Tạo lại tên đã tồn tại, kể cả tên Department Inactive; thử khác chữ hoa/thường theo collation DB.
2. Kỳ vọng: lỗi “Tên phòng ban đã được sử dụng”, giữ tên nhập; không success/500.
3. Sửa phòng ban, lưu cùng tên hiện tại: thành công, không tự trùng với chính ID.

### D05 — Chọn trưởng phòng đúng

1. Tạo Department → mở tài khoản nội bộ thử nghiệm → sửa Department sang phòng vừa tạo.
2. Quay lại sửa Department; nhân viên Active đó phải xuất hiện trong dropdown.
3. Chọn và lưu. Kỳ vọng ManagerId đúng; DepartmentStatus không đổi.

### D06 — Trưởng phòng sai / bỏ chọn

1. Dropdown không có Candidate, Inactive/Blocked hoặc nhân viên phòng khác.
2. Trong POST test có CSRF, sửa managerId thành Candidate/nhân viên phòng khác/ID không tồn tại.
3. Kỳ vọng: lỗi inline manager, không thay tên/ManagerId trong DB.
4. Chọn **Chưa phân công**, lưu: ManagerId=NULL.

### D07 — Manager cũ mất điều kiện

1. Chọn manager hợp lệ bằng D05, rồi vô hiệu hóa hoặc chuyển manager đó sang phòng khác.
2. Mở lại edit Department. Kỳ vọng manager cũ được đánh dấu không còn đủ điều kiện, không âm thầm bỏ ManagerId.
3. Chọn manager mới hợp lệ hoặc bỏ chọn, lưu thành công.

### D08 — Hủy vô hiệu hóa

1. Từ list, bấm **Vô hiệu hóa**.
2. Kỳ vọng native modal đúng tên, giải thích giữ lịch sử; focus Hủy.
3. Bấm Hủy hoặc Escape. Kỳ vọng không POST, status giữ Active, focus về button gốc.

### D09 — Xóa mềm / giữ lịch sử

1. Chọn Department thử nghiệm đang có User/Requisition tham chiếu; lưu ID và trạng thái các record trước test.
2. Bấm **Vô hiệu hóa** → xác nhận.
3. Kỳ vọng DepartmentStatus=Inactive; row còn; User accountStatus, User.DepartmentId, Requisition.DepartmentId/history không đổi.
4. Dashboard số Active giảm1, Inactive tăng1, Total không đổi.

### D10 — Gán vào Inactive

1. Mở Create Internal Account: phòng Inactive không được chọn.
2. Edit tài khoản ở phòng khác: phòng Inactive đó không được chọn.
3. Gửi POST departmentId Inactive thủ công với CSRF. Kỳ vọng bị chặn, User không thay đổi.
4. Edit tài khoản đang thuộc chính phòng Inactive đó, sửa full name/email nhưng giữ Department: được lưu. Không đổi username/hash.

### D11 — Kích hoạt lại phòng ban

1. Row Inactive → **Kích hoạt lại** → Hủy: không đổi status.
2. Làm lại, xác nhận: Active; ID/manager/record liên quan giữ nguyên.
3. Create/transfer Internal Account có thể chọn phòng ban trở lại; Dashboard count cập nhật.

### A01 — Internal Account deactivate → activate → login

1. Chọn internal test Active; ghi username/role/DepartmentId (không ghi hash ra tài liệu).
2. Vô hiệu hóa có xác nhận → Inactive.
3. Đăng nhập test account ở browser riêng: thất bại. Session cũ khi gọi protected route bị thu hồi.
4. Admin bấm **Kích hoạt lại** → confirm. Kỳ vọng Active, cùng role/department/username.
5. Đăng nhập lại bằng mật khẩu cũ: thành công; session cũ đã hết hiệu lực không tự sống lại.

### A02 — Candidate restore, lịch sử giữ nguyên

1. Mở Tài khoản ứng viên; Candidate Inactive có nút kích hoạt lại.
2. Hủy → không đổi; xác nhận → Active.
3. Đăng nhập Candidate bằng mật khẩu cũ; role vẫn Candidate; hồ sơ/đơn/phỏng vấn/offer cũ còn.
4. Không có nút đổi Candidate sang nội bộ; ID Candidate gọi internal activate route phải404, ngược lại cũng404.

### A03 — Blocked và idempotence

1. Row Blocked không có Activate trực tiếp. POST /activate thủ công với Admin/CSRF →409, status còn Blocked.
2. Gửi activate lần hai cho account Active hoặc Department Active → trạng thái giữ nguyên, không tạo record mới.

### S01 — Guest/role access

1. Browser chưa đăng nhập mở Department route → Login.
2. HR/HM/Director/Interviewer/Candidate mở Department route hoặc POST activate admin →403, không mutation.
3. System Admin truy cập và thao tác được.

### S02 — CSRF và request method

1. POST create/update/deactivate/activate không CSRF hoặc token sai →403; DB không đổi.
2. GET URL activate/deactivate không gây mutation (không có GET handler).
3. POST token hợp lệ từ form mới trong session test → tới đúng Service.

### U01 — Responsive / keyboard / empty state

1. Kiểm tra các trang list/new/edit/dashboard/internal/Candidate ở1440/1024/768/375.
2. Kỳ vọng không cuộn ngang toàn trang; bảng rộng cuộn trong vùng bảng, button/form đủ đọc và chạm.
3. Tab qua menu/link/filter/action; Enter mở modal, Escape/Hủy trả focus; menu mobile mở/đóng đúng aria-expanded.
4. Search không có kết quả → empty copy; resetfilter → danh sách trở lại. Search/status/pagination không mất filter.
5. Tên có ký tự `<`/`>` hiển thị text, không thực thi HTML/script.

## 5. Điểm tiếp tục

Hoàn thành manual D01–U01 để có bằng chứng live CRUD/login E2E. Không sửa rule requisition assignment hay tự đồng bộ manager khi User đổi phòng/status trước khi BA/user chốt. Luồng package/class/hàm xem [technical flow](../flows/admin-departments-account-activation-flow.md); danh sách file và lý do xem [process log](<../update architecture/2026-10-04-admin-departments-activation.md>).
