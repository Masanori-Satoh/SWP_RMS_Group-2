# Dashboard System Admin — giao diện, luồng và kiểm thử

Ngày thực hiện: 13/10/2026. Phạm vi: giao diện Dashboard Admin và bổ sung STT cho ba danh sách quản trị liên quan.

## 1. Yêu cầu và giới hạn đã tuân thủ

- Giao diện Dashboard bằng tiếng Việt, thống nhất nhận diện Mộc đang dùng ở homepage: xanh rừng, nền trắng ngà, tiêu đề Lora, đường viền nhẹ.
- Đọc quy định `docs/architecture/UI/UI_RULES.md`, `UI_CATALOG.md`, `UI_CSS_GUIDE.md`; đối chiếu homepage thật trong source và bản render trên trình duyệt.
- Dùng UI UX Pro Max để đối chiếu cách biểu diễn số liệu; áp dụng Impeccable theo chế độ giao diện nghiệp vụ và lượt polish có giới hạn. Dùng copy-editing để làm rõ nhãn hành động và trạng thái.
- Không sửa backend, DTO, Security, database, dữ liệu seed hoặc cấu hình. Không đưa số liệu mẫu vào template/source chạy thật.
- Không sửa khung chung, topbar, sidebar, token, component dùng chung hoặc bất kỳ tài liệu nào trong `docs/architecture/UI/`.
- Không thêm thư viện biểu đồ/JavaScript mới; dùng HTML, Thymeleaf, CSS và các component hiện có.
- Tài liệu thay đổi, luồng và test nằm chung trong file này. Không thêm source test vào `src/test`.

## 2. Những vấn đề được xử lý

| Trước | Sau | Lý do |
|---|---|---|
| Hai thẻ tài khoản có nhiều con số cùng mức nhấn | Một bảng so sánh nhóm nội bộ/ứng viên, tổng số nổi bật, các trạng thái nằm đúng cột | Đọc và so sánh nhanh hơn |
| Muốn xem tài khoản Inactive/Blocked phải mở danh sách rồi tự chọn bộ lọc | Bấm trực tiếp con số để mở danh sách có `status` tương ứng | Tận dụng bộ lọc backend đã có |
| CTA đầu trang chỉ mở danh sách tài khoản | CTA tạo tài khoản nội bộ; quản lý từng nhóm ngay trong bảng | Phân biệt rõ tạo mới và quản lý |
| Phòng ban và API là các danh sách chữ ít phân cấp | Hai khối nội dung có tiêu đề, số liệu, chú thích và hành động riêng | Cùng nhận diện, rõ nhiệm vụ |
| Trạng thái API dễ bị hiểu là kết quả kiểm tra mới | Ghi rõ kết quả đã ghi nhận, có link mở giám sát để kiểm tra mới | Không tuyên bố dịch vụ đang hoạt động khi chưa kiểm tra |
| Các danh sách quản trị chưa có STT | Thêm cột STT đầu bảng, nối tiếp theo trang | Theo yêu cầu bổ sung của người dùng |
| Empty state tài khoản bị trộn Anh–Việt | Viết lại hai câu thông báo không tìm thấy bằng tiếng Việt | Nội dung dễ hiểu |

Các thành phần được giữ: nguồn dữ liệu thật từ DTO; hai vòng đời tài khoản riêng; các trạng thái Active/Inactive/Blocked; thao tác kích hoạt/vô hiệu hóa; CSRF; liên kết quản lý; khung đăng xuất HTML5 dialog; Dashboard các role khác.

## 3. Danh sách file đã sửa

### 3.1. `src/main/resources/templates/dashboard/admin.html`

- Giữ `layout:decorate="~{layout/internal}"`, `main#main`, route/view hiện tại và các trường của `AdminDashboardResponse`.
- Tổ chức `dashboard.accountSummaries` thành bảng so sánh; mỗi hàng vẫn đọc `summary.title`, `url`, `total`, `active`, `inactive`, `blocked`.
- Link số lượng dùng các URL sẵn có và query `status=Active/Inactive/Blocked`.
- Thêm CTA `/admin/accounts/new` và `/admin/departments/new`, đều đã có Controller xử lý.
- Phòng ban vẫn dùng `dashboard.departments`; bổ sung link lọc Active/Inactive.
- API vẫn dùng `dashboard.breakdowns` và fragment `dashboard/fragments :: statusRows`; không tạo dữ liệu về endpoint, thời điểm, tỷ lệ uptime hoặc lịch sử mà DTO chưa có.
- Giữ `dashboard.shortcuts` và điều kiện `shortcut.url != null`; AI chỉ hiển thị chưa khả dụng, không có nút giả.
- Thêm trường hợp danh sách thống kê rỗng, chưa có phòng ban hoặc chưa ghi nhận trạng thái tích hợp.
- Dùng caption, `scope` cho header bảng, vùng cuộn có tên và focus được, link có nhãn chỉ rõ nhóm/trạng thái.

### 3.2. `src/main/resources/static/css/dashboard.css`

- Bổ sung nhóm class `adm-` chỉ dùng cho Dashboard Admin, trong file CSS hiện có.
- Bỏ các selector `dash-account` / `dash-account__counts` của bố cục Admin cũ sau khi tìm dependency trong source và xác nhận không còn template/JS sử dụng.
- Dùng token có sẵn cho màu, spacing, cỡ chữ, bo góc.
- Phân cấp số tổng và số theo trạng thái; tài khoản bị khóa có cả nhãn và màu, không chỉ dựa vào màu.
- Số lượng có vùng bấm tối thiểu 44 × 44px, kế thừa focus-visible chung.
- Lưới hai khối chuyển một cột ở 992px; mobile giảm padding và xếp các mục theo chiều dọc.
- Bảng so sánh cuộn trong wrapper; không đẩy chiều rộng phần nội dung trang.
- Không đổi selector, token hoặc hành vi của các role khác.

### 3.3. `src/main/resources/templates/admin/accounts/list.html`

- Thêm cột STT đầu tiên.
- Vòng lặp thêm biến `row`; STT = `accounts.number * accounts.size + row.count`.
- Empty state đổi `colspan` từ 7 thành 8 và sửa câu trộn ngôn ngữ.
- Giữ nguyên bộ lọc, trường dữ liệu, form, phân trang và các action.

### 3.4. `src/main/resources/templates/admin/accounts/candidates.html`

- Thêm STT theo cùng công thức từ `accounts`.
- Empty state đổi `colspan` từ 7 thành 8, dùng câu tiếng Việt.
- Không thay đổi lifecycle Candidate, hồ sơ liên kết hoặc action kích hoạt/vô hiệu hóa.

### 3.5. `src/main/resources/templates/admin/departments/list.html`

- Thêm cột STT đầu tiên; STT = `departments.number * departments.size + row.count`.
- Empty state đổi `colspan` từ 4 thành 5.
- Giữ các action sửa, vô hiệu hóa, kích hoạt lại và bộ lọc hiện có.

File mới được giữ lại: chính tài liệu `docs/members/linhdn/ADMIN_DASHBOARD_UI_2026-10-13.md`.

Không xóa file source. Công cụ kiểm tra tạm dưới thư mục thành viên được dọn sau khi kiểm tra.

## 4. Quy tắc STT

- STT là vị trí trong kết quả danh sách hiện tại, không phải ID database.
- `Page.number` bắt đầu từ 0; `row.count` bắt đầu từ 1.
- Với size = 10: trang đầu là 1–10, trang thứ hai bắt đầu 11. Size lấy từ `Page`, không hardcode 10 trong template.
- Sau khi lọc/sắp xếp, STT phản ánh thứ tự mới của kết quả.
- Không đánh số hàng empty state.
- Bảng Dashboard là thống kê theo nhóm tài khoản, không phải danh sách từng User. STT được đặt ở ba trang danh sách quản lý bản ghi.
- Đã đọc `account-list.js`, `status-confirmation.js`, `account-list.css`: không thấy truy cập cột theo `cells[index]` hoặc `nth-child` bị lệch do thêm STT trong những file này.

## 5. Luồng kỹ thuật hiện tại

Đợt này không thay đổi các hàm Java bên dưới; tài liệu ghi lại cách UI gọi luồng đã tồn tại.

### A. Mở Dashboard Admin

1. Trình duyệt GET `/dashboard`, đi qua cơ chế session/authorization hiện tại.
2. `com.group2.rms.dashboard.DashboardController.dashboard(Authentication, Model)` lấy tên tài khoản từ Authentication.
3. Gọi `com.group2.rms.dashboard.DashboardService.forUsername(String)`; role System Admin chọn `admin(User)`.
4. `admin(User)` lấy dữ liệu từ `DashboardMetricsRepository.internalAccountStatuses()`, `candidateAccountStatuses()` và `departmentSummary()` → `DashboardMetricsRepository.departmentStatuses()`.
5. Tình trạng dịch vụ lấy từ `com.group2.rms.admin.service.ApiMonitoringService.rows()` rồi nhóm theo status; mở Dashboard không tự chạy một probe mới.
6. Service trả `com.group2.rms.dashboard.dto.AdminDashboardResponse`.
7. Controller đặt `dashboard`, `activeMenu`, `pageLang` vào model và trả view `dashboard/admin`.
8. Thymeleaf render `dashboard/admin.html` trong `layout/internal` → `layout/base`, dùng fragments chung và `dashboard.css`.

### B. Từ số lượng tài khoản đến danh sách đã lọc

1. Người dùng bấm con số dưới cột Hoạt động/Vô hiệu hóa/Bị khóa.
2. Nhóm nội bộ: GET `/admin/accounts?status=...` → `com.group2.rms.user.controller.AccountController.list(...)`.
3. Controller gọi `com.group2.rms.user.service.AccountListService.findAccounts(...)` → `findUsers(..., false)` → `UserRepository.findAll(Specification, Pageable)`.
4. Nhóm ứng viên: GET `/admin/candidate-accounts?status=...` → `CandidateAccountController.list(...)` → `AccountListService.findCandidateAccounts(...)` → `findUsers(..., true)`.
5. Service trả `Page<AccountRow>` hoặc `Page<CandidateRow>`; Controller đưa vào `accounts`.
6. View `admin/accounts/list` hoặc `admin/accounts/candidates` render các dòng, STT và phân trang từ Page đã nhận.
7. Link này chỉ đọc danh sách. Việc đổi trạng thái vẫn qua form POST và dialog xác nhận hiện có.

### C. Từ Dashboard đến phòng ban

1. Bấm Quản lý phòng ban hoặc số lượng theo trạng thái → GET `/admin/departments`, tùy link có `status=Active/Inactive`.
2. `com.group2.rms.user.controller.DepartmentController.list(...)` gọi `com.group2.rms.user.service.DepartmentService.findDepartments(...)`.
3. Service dùng Specification và Pageable với `DepartmentRepository.findAll(...)`, map sang `DepartmentResponse`.
4. View `admin/departments/list` render STT theo `departments.number`, `size` và `row.count`.
5. Bấm Tạo phòng ban → `DepartmentController.newDepartment(Model)` → `admin/departments/form`.

## 6. Kiểm tra đã thực hiện

| Kiểm tra | Kết quả | Phạm vi bằng chứng |
|---|---|---|
| Maven compile offline | PASS | Source Java hiện tại; không có Java bị sửa trong đợt này |
| Thymeleaf/Spring MVC render | PASS — 98 kiểm tra | 14 trang render từ template thật và DTO/Page kiểm thử; có LayoutDialect và CSRF filter |
| Nhóm tài khoản, số tổng/trạng thái, href query | PASS | Giá trị từ fixture được render đúng, link trỏ route/query hiện có |
| Dữ liệu rỗng, thiếu nhóm thống kê, số lượng lớn | PASS | Có hướng dẫn khi rỗng; giá trị lớn không bị cắt thành số khác |
| STT ba danh sách, trang đầu và trang 2 | PASS | Trang đầu 1, 2; trang 2 size 10 là 11, 12; colspan đúng khi rỗng |
| Desktop 1440/1024, tablet 768 | PASS phần nội dung | Không tràn ngang toàn trang tại các kích thước này; bảng 768 cuộn riêng |
| Mobile 375 | PASS phần nội dung, FAIL khung chung có sẵn | Chiều rộng scroll của main bằng clientWidth; topbar tràn, sidebar vẫn hiện dù hidden |
| Bàn phím vùng bảng/link số lượng | PASS | Cuộn ngang, Tab chuyển link, focus có viền nhìn thấy |
| Dialog logout dùng chung | PASS mở/đóng | Click mở, Escape đóng và trả focus về nút Đăng xuất; không gửi POST logout thật |
| Console preview Dashboard | Không ghi nhận error | Không chứng minh các API thật hoạt động |
| Contrast từ token đang render | PASS các cặp đã đo | ink/page 14.05:1; muted/surface 6.25:1; brand-dark/surface 7.88:1; error/error-soft 6.92:1; on-brand/brand 5.39:1 |
| `git diff --check` | PASS | Không có lỗi whitespace trong diff |
| Full Maven test, SQL Server startup/JPA, CRUD thật | NOT RUN | Chưa kiểm tra end-to-end với database trong đợt giao diện này |

Lệnh compile đã chạy:

```powershell
.\mvnw.cmd '-Dmaven.repo.local=C:\Users\Administrator\.m2\repository' '-Dstyle.color=never' '-o' compile
```

Bộ kiểm tra tạm dùng Spring MVC/Thymeleaf thật và dữ liệu giả lập riêng cho kiểm thử; không chạy application seeder, không truy vấn/sửa database, không thêm dữ liệu giả vào source. Những con số trong ảnh preview không phải số liệu production/database của dự án.

## 7. Kịch bản test lại trên ứng dụng thật

### T01 — Dashboard và số liệu

1. Chạy ứng dụng với cấu hình local hiện có, đăng nhập System Admin.
2. Mở `/dashboard`; kiểm tra heading Tổng quan hệ thống và CTA Tạo tài khoản nội bộ.
3. Đối chiếu từng nhóm: tổng, Active, Inactive, Blocked với dữ liệu thật đang có.
4. Bấm tên nhóm hoặc Quản lý: đi đúng tài khoản nội bộ/ứng viên, không lẫn hai nhóm.
5. Bấm Tạo tài khoản nội bộ: mở form hiện có, không tự tạo bản ghi khi bấm link.

### T02 — Link lọc trạng thái

1. Bấm số dưới Vô hiệu hóa của nhóm nội bộ.
2. URL phải có `/admin/accounts?status=Inactive`; dropdown trạng thái chọn Vô hiệu hóa.
3. Tất cả hàng phải thuộc nội bộ và Inactive; tổng kết quả khớp Dashboard tại thời điểm kiểm tra.
4. Lặp lại Active/Blocked và nhóm ứng viên; con số 0 phải dẫn tới empty state hợp lệ.
5. Chọn Xóa bộ lọc; kiểm tra kết quả quay về danh sách của đúng nhóm.

### T03 — STT và phân trang

1. Kiểm tra lần lượt `/admin/accounts`, `/admin/candidate-accounts`, `/admin/departments`.
2. Cột đầu tiên là STT; bản ghi đầu trang đầu là 1.
3. Nếu có trang tiếp, chuyển trang; STT đầu phải bằng `page.number * page.size + 1`.
4. Thay đổi bộ lọc/sắp xếp; STT liên tục theo kết quả hiển thị, không dùng UserId/DepartmentId.
5. Tìm từ khóa không có kết quả: chỉ có thông báo rỗng, không có STT giả; hàng rỗng phủ đủ các cột.
6. Kiểm tra các nút Sửa/Kích hoạt/Vô hiệu hóa vẫn gắn đúng bản ghi sau khi thêm cột.

### T04 — Phòng ban

1. Đối chiếu tổng phòng ban và hai trạng thái.
2. Bấm từng số lượng; URL có status đúng, danh sách tương ứng đúng.
3. Bấm Tạo phòng ban; form hiện có mở bình thường.
4. Với dữ liệu kiểm thử được phép thay đổi, vô hiệu hóa rồi kích hoạt lại một phòng ban; reload Dashboard để đối chiếu số liệu. Không xóa vật lý.

### T05 — API và AI

1. Khi chưa chạy probe, Dashboard phải hiện Chưa kiểm tra/Chưa cấu hình theo dữ liệu hiện tại.
2. Bấm Mở giám sát API, thực hiện kiểm tra bằng chức năng sẵn có.
3. Quay lại/reload Dashboard; số dịch vụ theo trạng thái cập nhật từ kết quả backend.
4. Không có nhãn uptime, thời gian kiểm tra hoặc biểu đồ lịch sử được bịa thêm.
5. Cấu hình AI chỉ ghi Chưa khả dụng, không có form/action giả.

### T06 — Responsive và bàn phím

1. Mở Dashboard ở 1440, 1024, 768, 375px.
2. Kiểm tra bảng cuộn trong vùng riêng; title, CTA, ghi chú không đè nhau.
3. Tab đến vùng bảng và dùng phím mũi tên để cuộn; Tab qua các con số, focus phải nhìn thấy và tên link phải chỉ rõ nhóm/trạng thái.
4. Mở Đăng xuất rồi Escape; dialog đóng, focus trả về nút, session vẫn còn.
5. Tại mobile ghi nhận lỗi shared shell ở mục 8, không đánh dấu toàn trang đạt khi khung vẫn lỗi.

### T07 — Hồi quy phạm vi

1. Mở Dashboard Candidate/HR/Director/Interviewer/Hiring Manager bằng tài khoản tương ứng.
2. Xác nhận bố cục cũ của các role đó không nhận class `adm-` và không đổi chức năng.
3. Guest vẫn phải đăng nhập để vào Dashboard; role khác không được truy cập quản trị tài khoản/phòng ban. Đây là kiểm tra quyền hiện có, không phải quyền mới của đợt này.

## 8. Giới hạn và việc tiếp theo

- Lỗi shared shell tại 375px đã có trước: sidebar mang `hidden` nhưng computed display vẫn là flex; khối tên/email của topbar vượt viewport. Người dùng đã yêu cầu giữ nguyên khung chung, nên đợt này chỉ ghi nhận.
- Trong lượt xác nhận cuối, nội dung main không có overflow riêng tại bốn kích thước; chiều rộng toàn trang mobile vẫn vượt viewport do shared shell. Cần người phụ trách khung chung xử lý riêng khi được duyệt.
- Các trang danh sách cũ vẫn còn một số breadcrumb/nội dung tiếng Anh và CSS legacy. Đợt này chỉ bổ sung STT, colspan và hai câu empty state, không migrate toàn bộ các trang đó.
- Dashboard chưa có danh sách endpoint chi tiết, lần kiểm tra cụ thể hoặc lịch sử biến động trong DTO. Nếu muốn thêm các dữ liệu này phải báo trước khi sửa backend.
- Không chạy CRUD thật hoặc xác nhận quyền/JPA/SMTP/database bằng preview. Các bước ở mục 7 dành cho lần kiểm tra trên app thật.
- Không commit, merge, push hoặc sửa tài liệu kiến trúc UI trong đợt này.

## 9. Nhật ký thực hiện

1. Đọc UI rules/catalog/CSS guide và ba skill; kiểm tra source Dashboard, DTO, Service, route và homepage.
2. Chọn bố cục bảng so sánh tài khoản + hai khối phòng ban/API; giữ nguyên nguồn số liệu và design tokens.
3. Sửa Admin template/CSS, thêm STT ba danh sách theo yêu cầu mới.
4. Chạy Maven compile và bộ render tạm; bổ sung CSRF filter cho môi trường kiểm tra để kiểm tra đúng form thực tế.
5. Kiểm tra trình duyệt desktop/tablet/mobile, dữ liệu rỗng/số lớn, focus và dialog. Lượt polish cuối bỏ header ẩn gây overflow trong bảng và giảm khoảng trống cuối bảng.
6. Xác nhận lại 98 kiểm tra render; đối chiếu diff, ghi tài liệu này và dọn công cụ preview tạm.
