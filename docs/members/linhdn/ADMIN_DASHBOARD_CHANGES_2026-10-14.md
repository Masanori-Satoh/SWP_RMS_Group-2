# Thay đổi System Admin Dashboard — 14/10/2026

## 1. Mục tiêu và phạm vi

Cải thiện Dashboard của System Admin theo phong cách Mộc: số liệu thực, bố cục gọn, biểu đồ tương tác và đường dẫn mở đúng danh sách đã lọc. Dự án tiếp tục sử dụng Spring Boot, Thymeleaf, SQL Server, Vanilla JavaScript và Chart.js.

Tài liệu tổng hợp đợt triển khai Dashboard tương tác và các lần tinh chỉnh giao diện trong phiên làm việc này. Những thay đổi có sẵn của các thành viên khác không thuộc danh sách bên dưới.

## 2. Bố cục cuối cùng

```text
Page Header                         [Tạo tài khoản nội bộ]

[Internal Accounts] [Total Candidates] [New Candidates — Last 30 Days]

[Account Status Distribution]       [Internal Accounts by Role]

[Compact System Health                            Xem chi tiết]

[Quick Administration Actions]
```

- Desktop 1366px, 1440px và 1920px: ba summary cards chia đều một hàng bằng CSS Grid `repeat(3, minmax(0, 1fr))`.
- Các card có cùng chiều cao, khoảng 179px trong dữ liệu kiểm thử hiện tại; không đặt width cố định.
- Biểu đồ nằm ngay dưới summary, xuất hiện từ khoảng 400px tính từ đầu trang.
- Monitoring được chuyển xuống dưới biểu đồ và thu gọn còn khoảng 186px.
- Màn hình nhỏ hơn tự giảm số cột theo breakpoint hiện có.
- Không triển khai Department Chart hoặc Account Creation Trends.

## 3. Summary Metrics và nguồn dữ liệu

| Metric | Nguồn dữ liệu | Giá trị lúc kiểm tra |
| --- | --- | ---: |
| Internal Accounts | User thuộc System Admin, HR, Hiring Manager, Director, Interviewer; tính mọi trạng thái | 12 |
| Total Candidates | Đếm trực tiếp các hàng trong bảng Candidate | 192 |
| New Candidates — Last 30 Days | Đếm Candidate có CreatedAt trong khoảng thời gian 30 ngày trượt | 52 |

Các giá trị trên là snapshot của SQL Server ngày 14/10/2026, không phải hằng số hoặc dữ liệu minh họa trong ứng dụng.

### Phân biệt Candidate và Candidate Account

Candidate là hồ sơ lưu tại bảng `Candidate`, có `CandidateId` riêng và liên kết một-một với User qua `UserId`. Total Candidates đếm hồ sơ Candidate, không đếm User có role Candidate và không đếm Application.

Sau khi xác thực OTP đăng ký, `AccountManagementService` tạo User và Candidate trong cùng transaction. Khi nộp hồ sơ, `ApplicationSubmissionService` tìm Candidate đã có và tạo Application; ứng tuyển nhiều vị trí không làm tăng số Candidate.

`CandidateRepository` đã có `count()` kế thừa từ JpaRepository. Hai aggregate phục vụ Dashboard được bổ sung vào `DashboardMetricsRepository` hiện có, không cần sửa CandidateRepository hoặc thêm lớp trừu tượng mới.

### Cách tính 30 ngày

Candidate kế thừa `createdAt` từ BaseEntity, ánh xạ tới `Candidate.CreatedAt` kiểu `DATETIME2 NOT NULL`.

```text
to   = LocalDateTime.now()
from = to.minusDays(30)

CreatedAt >= from AND CreatedAt <= to
```

- Hai cận đều được tính; cận trên loại các timestamp tương lai khỏi metric 30 ngày.
- Đây là cửa sổ trượt theo thời điểm mở Dashboard, không phải tháng lịch hoặc thời điểm nộp Application.
- DTO giữ `from`, `to`, `timeZone`; giao diện hiển thị khoảng ngày và title có timestamp đầy đủ.
- BaseEntity ghi giờ local JVM; SQL default dùng SYSDATETIME(). JVM local hiện là Asia/Saigon và SQL Server hiện UTC+7, đã đối chiếu trong integration test.
- DATETIME2 không lưu offset; không thay đổi quy ước lưu thời gian, Entity hoặc timezone toàn hệ thống.

Database hiện có 24 Candidate với nhiều Application và các hồ sơ seed mang CreatedAt tương lai. Aggregate không join Application nên không đếm trùng; Total Candidates vẫn tính mọi hồ sơ đang tồn tại, còn metric 30 ngày loại hồ sơ nằm ngoài cửa sổ. Không sửa seed hoặc dữ liệu database.

### Điều hướng từ summary

| Card | Trang đích | Giới hạn |
| --- | --- | --- |
| Internal Accounts | `/admin/accounts?page=0` | Danh sách tài khoản nội bộ |
| Total Candidates | `/admin/candidate-accounts?page=0` | Trang quản lý tài khoản ứng viên, không phải danh sách toàn bộ hồ sơ Candidate |
| New Candidates — Last 30 Days | `/admin/candidate-accounts?page=0` | Chưa có bộ lọc Candidate.CreatedAt; card ghi rõ “Chưa lọc 30 ngày” |

Hiện chưa có trang Candidate dành cho Admin bao phủ toàn bộ hồ sơ và lọc theo ngày tạo. `/applications` là danh sách Application; `/talent-pool` chỉ là hồ sơ tiềm năng và không cấp quyền cho Admin. Không tạo query parameter giả hoặc khẳng định trang đích đã được lọc 30 ngày.

Card Blocked Accounts được bỏ riêng khỏi summary. `blockedTotal()`, các aggregate trạng thái và chức năng quản lý tài khoản bị khóa vẫn được giữ.

## 4. Biểu đồ và drill-down

### Account Status Distribution

- Hiển thị Active, Inactive, Blocked theo nhóm Internal hoặc Candidate Accounts.
- Đổi nhóm cập nhật chart và các link dự phòng tương ứng.
- Click cột có dữ liệu mở đúng Account List với `status` và `page=0`.
- Trạng thái bằng 0 giữ đúng số lượng; người dùng vẫn mở danh sách bằng anchor bên dưới.
- Giới hạn độ dày cột, giảm grid, tooltip hiển thị số tài khoản và gợi ý điều hướng.

### Internal Accounts by Role

- Horizontal Bar Chart sử dụng aggregate theo năm role nội bộ.
- Role ID lấy từ bảng Role; query LEFT JOIN giữ role có 0 tài khoản.
- Click cột mở `/admin/accounts?roleId=<ID thực>&page=0`.
- Các chips dự phòng giữ anchor, số lượng và đường dẫn cho cả role có giá trị 0.

Chỉ click đúng phần tử dữ liệu mới điều hướng; click khoảng trắng không mở danh sách. Có hover, tooltip và cursor pointer. Nếu Chart.js thiếu hoặc lỗi, các đường dẫn HTML vẫn sử dụng được. Chart.js được lưu local, không phụ thuộc CDN khi chạy ứng dụng.

Pagination tại Internal Account List và Candidate Account List sử dụng adapter PageLinks hiện có để giữ search, status, roleId, departmentId, sort và các query parameter đang có khi chuyển trang. Không sửa routing hoặc nghiệp vụ lọc.

## 5. System Health và giao diện Mộc

Monitoring hiển thị tên dịch vụ, status badge và thông tin kiểm tra gần nhất. Action xem chi tiết mở `/admin/api-monitoring`; mở Dashboard không chạy probe.

Healthy/Failed thể hiện kết quả kiểm tra đã có. Not Checked và Unconfigured giữ đúng ý nghĩa do monitoring backend cung cấp; không tự suy diễn uptime, phần trăm health hoặc cấu hình AI/Email ở module khác.

- Giữ màu, spacing, font và component catalog của Mộc.
- Số liệu và nội dung quản trị dùng font sans-serif; tiêu đề thương hiệu giữ phong cách hiện có.
- Bỏ underline chỉ trong `.adm-page`; thay bằng clickable cards, chips và action có dấu điều hướng.
- Có hover background/border và focus-visible rõ ràng; giữ thẻ anchor thật.
- Không thêm inline CSS, framework frontend hoặc dữ liệu demo.
- CSS mới dùng tiền tố `adm-`; các quy tắc `dash-*` dùng chung không đổi.
- Không sửa layout, topbar, sidebar, design tokens hoặc component chung.

## 6. Danh sách file sửa/tạo trong đợt Dashboard

| File | Nội dung thay đổi |
| --- | --- |
| [DashboardMetricsRepository.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/DashboardMetricsRepository.java>) | Aggregate theo role và hai metric Candidate |
| [DashboardService.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/DashboardService.java>) | Cấp role counts, snapshot monitoring và cửa sổ Candidate 30 ngày cho Admin |
| [AdminDashboardResponse.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/dto/AdminDashboardResponse.java>) | Thêm roleCounts, monitorRows, CandidateSummary và các summary helper |
| [admin.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/dashboard/admin.html>) | Ba summary cards, chart, monitoring, link dự phòng và thứ tự nội dung mới |
| [dashboard.css](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/css/dashboard.css>) | Grid, typography, spacing, hover/focus và monitoring compact trong phạm vi Admin |
| [admin-dashboard.js](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/js/pages/admin-dashboard.js>) | Chart.js, đổi scope, hit detection, drill-down và xử lý lỗi chart |
| [chart.umd.min.js](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/js/vendor/chart.umd.min.js>) | Chart.js 4.5.1 lưu local |
| [chart.LICENSE.md](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/static/js/vendor/chart.LICENSE.md>) | Giấy phép MIT của Chart.js |
| [list.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/admin/accounts/list.html>) | Pagination Internal giữ query parameters |
| [candidates.html](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/resources/templates/admin/accounts/candidates.html>) | Pagination Candidate Accounts giữ query parameters |
| [AdminDashboardAnalyticsTests.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/test/java/com/group2/rms/dashboard/AdminDashboardAnalyticsTests.java>) | Summary scope, zero counts, role ID, cửa sổ thời gian và regression role khác |
| [AdminDashboardIntegrationTests.java](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/test/java/com/group2/rms/dashboard/AdminDashboardIntegrationTests.java>) | Đối chiếu SQL, render URL/filter, pagination, timezone, biên thời gian và fixture zero |

Lần điều chỉnh ba summary cards chỉ sửa Repository/Service/DTO của Dashboard, admin.html, dashboard.css và hai test. JavaScript/chart assets và pagination đã được triển khai ở bước trước, không sửa lại trong lần điều chỉnh summary.

Không thay đổi Controller, Entity, schema, business logic đăng ký/CRUD, Spring Security hoặc CSRF. Không sửa Dashboard của HR/Hiring Manager hay các thay đổi có sẵn của thành viên khác.

## 7. Kiểm thử đã thực hiện

| Hạng mục | Kết quả |
| --- | --- |
| Maven package cuối | BUILD SUCCESS |
| Test liên quan | 13 pass: 7 unit và 6 integration; không failure/error/skipped |
| SQL Server | Đối chiếu count Candidate, window 30 ngày và count distinct khi join Application |
| Biên thời gian | Bao gồm timestamp đúng hai cận; khoảng trước hồ sơ đầu tiên trả 0 |
| Timezone | Offset JVM và SQL Server local khớp UTC+7 |
| Desktop | 1366/1440/1920px, sidebar ghim/thu gọn; ba card cùng hàng, cùng chiều cao, không overflow |
| Điều hướng | Ba summary cards, đủ năm role bars, Active hai scope, Inactive/Blocked links và pagination giữ filter |
| Accessibility/fallback | Hover, focus, Enter, Chart.js không tải và fixture zero; không JavaScript pageerror |
| HR/Hiring Manager | Dashboard render HTTP 200, không chứa Admin summary hoặc Admin chart script |

Các integration test chạy chế độ đọc trên SQL Server với `ddl-auto=none`. Fixture zero chỉ dùng trong kiểm thử, không ghi vào database hoặc đưa vào ứng dụng. Không chạy probe hoặc thao tác CRUD để tạo dữ liệu kiểm thử.

Ảnh và log local:

- Giao diện 1366px, sidebar ghim — tệp QA cục bộ `summary-1366-pinned.png`
- Giao diện 1440px — tệp QA cục bộ `summary-1440-collapsed.png`
- Giao diện 1920px — tệp QA cục bộ `summary-1920-collapsed.png`
- Build và 13 test — tệp QA cục bộ `summary-metrics-build-test.log`
- Build cuối — tệp QA cục bộ `summary-final-build.log`
- Kiểm tra Chrome — tệp QA cục bộ `summary-ui-checks.log`
- Đo layout cuối — tệp QA cục bộ `summary-width-checks.log`

## 8. Giới hạn còn lại

- Chưa có drill-down hồ sơ Candidate theo ngày tạo; link hiện tại được mô tả đúng là trang tài khoản chưa lọc 30 ngày.
- Chưa chạy toàn bộ test suite hoặc E2E CRUD.
- Chrome được kiểm tra bằng launcher NIO2 tạm vì môi trường Java local gặp lỗi loopback với Tomcat NIO mặc định. Launcher nằm ngoài source ứng dụng, không đóng gói và đã được dừng sau kiểm thử.
- Maven test dùng `forkCount=0` do lỗi classpath fork của môi trường; cảnh báo JaCoCo execution data cũ không được dùng để khẳng định coverage.
- Conflict có sẵn tại database/schema/db.sql được giữ nguyên.

Không commit, push hoặc merge code.
