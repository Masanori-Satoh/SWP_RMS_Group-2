# Thống kê thay đổi UI Potential Candidate

- **Ngày tổng hợp:** 28/10/2026.
- **Người tổng hợp:** LinhDN, theo yêu cầu người dùng.
- **Phạm vi báo cáo:** đợt đồng bộ các trang Potential Candidate theo layout Admin hiện tại.
- **Commit đối chiếu:** `ddd73d63a061ec771311a414d720ba56f85f1b14` — `sync potential candidate v1`, ngày 28/10/2026 lúc 20:38:22, giờ Việt Nam.
- **Trạng thái khi bắt đầu xuất báo cáo:** Git working tree và index sạch; các thay đổi dưới đây đã có trong commit, không còn là thay đổi chưa lưu của tác vụ trước.
- **Lần xuất báo cáo này:** chỉ tạo file Markdown này, không sửa source, không chạy lại build/test, không stage/commit/push.

## 1. Tổng số file và dòng thay đổi

| Nhóm | Số file | Dòng thêm | Dòng xóa |
| --- | ---: | ---: | ---: |
| Frontend: 4 template và 1 CSS mới | 5 | 152 | 64 |
| Kiểm thử: điều chỉnh một assertion | 1 | 1 | 1 |
| Catalog, work log và tài liệu chi tiết | 3 | 107 | 0 |
| **Tổng phạm vi đồng bộ UI** | **9** | **260** | **65** |

Trong 9 file: **2 file tạo mới**, **7 file chỉnh sửa**, **0 file bị xóa**. Số dòng lấy từ Git diff của commit đối chiếu; không dùng để suy ra số lượng chức năng.

File thống kê đang đọc được tạo sau commit trên nên không tính vào 9 file của đợt triển khai UI.

## 2. Danh sách file change

Các đường dẫn bên dưới tính từ root repository.

| File | Loại | Thêm | Xóa | Nội dung chính |
| --- | --- | ---: | ---: | --- |
| `src/main/resources/templates/talent-pool/list.html` | Sửa | 42 | 23 | Bật workspace Admin; toolbar tìm kiếm/bộ lọc; card chứa danh sách; header bảng, phân trang và cỡ nút thao tác. |
| `src/main/resources/templates/talent-pool/detail.html` | Sửa | 20 | 19 | Đồng bộ khung trang, profile, panel CV/ghi chú/thông tin ứng viên và vùng phản hồi gợi ý. |
| `src/main/resources/templates/talent-pool/suggestions.html` | Sửa | 16 | 12 | Đồng bộ trang đề xuất HM: title, card bảng và phân cấp nội dung. |
| `src/main/resources/templates/talent-pool/recommendations.html` | Sửa | 13 | 10 | Đồng bộ trang gợi ý HR: title, search toolbar, card gợi ý và màu chữ badge. |
| `src/main/resources/static/css/pages/talent-pool-workspace.css` | **Tạo mới** | 61 | 0 | Alias `tpw-` riêng cho bố cục, spacing, responsive, status và cỡ điều khiển. |
| `src/test/java/com/group2/rms/candidate/application/pipeline/controller/ApplicationPipelineWebTests.java` | Sửa | 1 | 1 | Assertion vẫn kiểm tra nội dung H1 nhưng không phụ thuộc toàn bộ chuỗi class cũ. |
| `docs/architecture/UI/UI_CATALOG.md` | Thêm mục cuối | 22 | 0 | Ghi cách tái sử dụng `adw-`, toolbar `mft-`, alias `tpw-` và phạm vi role. |
| `docs/management/WORK_LOG.md` | Thêm mục | 2 | 0 | Ghi đợt đồng bộ và thêm dòng phân cách cho bảng Markdown. |
| `docs/management/work_logs/2026-10-28-potential-candidate-admin-ui-sync.md` | **Tạo mới** | 83 | 0 | File change, quyết định UI, skill, kiểm thử, giới hạn và việc còn lại. |

## 3. Thay đổi người dùng nhìn thấy

| Khu vực | Trước | Sau |
| --- | --- | --- |
| Khung trang | Workspace nội bộ cũ, vùng dữ liệu hẹp hơn | Cùng workspace opt-in `adw-` với Admin: nền công nghệ, sidebar, topbar và vùng nội dung rộng. |
| Tiêu đề | Kiểu chữ và nhịp khoảng cách khác Admin | Dùng `adw-title`/`adw-heading`, tiêu đề desktop 36px, mobile 28px. |
| Danh sách ứng viên | Bộ lọc trải nhiều trường phía trên, bảng đứng riêng | Toolbar gọn; từ khóa bên ngoài, bộ lọc phụ trong disclosure; bảng nằm trong card có title và số lượng từ model. |
| Card và panel | Phân cấp chưa thống nhất với Admin | Tái sử dụng surface trắng, viền/shadow và panel title 20px của Admin. |
| Bảng dữ liệu | Kiểu hiển thị cũ của feature | Font dữ liệu 14px, header và inset đồng bộ; màn nhỏ cuộn trong wrapper, giữ đủ cột. |
| Điều khiển | Một số nút icon 32px | Alias feature đưa nút icon lên 40px; toolbar input/toggle 44px. |
| Badge | Foreground legacy làm màu trạng thái nhạt/không đúng | Alias `tpw-status` dùng token semantic, không sửa `.badge` dùng chung. |
| Breadcrumb và phân trang | Breadcrumb trùng với đường quay lại, khoảng trống pager lớn hơn | Bỏ breadcrumb lặp; giữ nút quay lại và pager catalog, thu gọn khoảng cách. |

Không tạo KPI, bộ lọc trạng thái, menu hoặc dữ liệu nghiệp vụ mới để lấp bố cục.

## 4. Những phần được giữ nguyên

- Layout vẫn decorate `layout/internal`; HR/Hiring Manager giữ menu, quyền và danh tính của mình. Không đổi role thành System Admin.
- Không đổi route/view name, query params, ID/name của field, binding Thymeleaf cũ, CSRF hoặc handler nghiệp vụ.
- Giữ chọn nhiều ứng viên, chọn CV nguồn, draft, cảnh báo khi rời trang, xem CV, ghi chú, xác nhận gửi và trạng thái lỗi/thử lại.
- Giữ riêng luồng HM đề xuất và HR gợi ý; giữ quyền chấp nhận/từ chối/xóa gợi ý cùng ngữ cảnh quay lại.
- Không sửa `potential-candidate-action.html` hoặc handler JS nghiệp vụ của Talent Pool; không đổi màn Hồ sơ ứng tuyển/Phỏng vấn ngoài phạm vi.
- Không chỉnh layout shell, tokens/fonts, workspace/topbar, sidebar-shell, brand, interface.js, Homepage hoặc Authentication trong đợt đồng bộ.
- Không đổi Controller, Service, Repository, Security, API, schema, seed hoặc database trong phạm vi công việc UI.

## 5. Tách biệt thay đổi khác trong cùng commit

Commit `ddd73d6` có tổng cộng **11 file**; 9 file ở mục 2 thuộc đợt đồng bộ UI. Hai file còn lại đã có thay đổi trước khi bắt đầu tác vụ UI:

| File | Git ghi nhận trong commit | Phân loại |
| --- | --- | --- |
| `src/main/java/com/group2/rms/candidate/talentpool/service/TalentPoolService.java` | Thêm 18, xóa 17 dòng | Thay đổi có sẵn trong workspace; tác vụ đồng bộ UI không sửa file này. |
| `docs/members/linhdn/~$Professional_Bug_Tracker_Template.xlsx` | Thêm file nhị phân | File khóa Excel có sẵn; không phải asset hay dữ liệu mẫu được tạo cho UI. |

Bằng chứng của đợt UI ghi nhận SHA-256 Service trước/sau không đổi: `4ae3ded63ad1ad6c64dd2d97b9cd3b18a797fee23b32b3cd15d136a652eed8ed`. Việc hai file cùng xuất hiện trong commit không đồng nghĩa chúng thuộc phạm vi tác vụ UI. Báo cáo này không xóa hoặc sửa chúng.

## 6. Kiểm tra đã thực hiện trong đợt đồng bộ

| Loại bằng chứng | Kết quả đã ghi nhận | Giới hạn |
| --- | --- | --- |
| Java | **74/74 đạt:** ApplicationPipelineWebTests 60; SidebarRoleWebTests 4; AdminDashboardAnalyticsTests 7; AdminDashboardViewTests 3 | MockMvc/service mock hoặc unit test; không chứng minh SQL/SMTP thực tế. |
| JavaScript | **35/35 đạt:** selection, UI, recommendations, potential-action | Kiểm tra hành vi trong bộ test; không phải toàn bộ end-to-end với dữ liệu thật. |
| Render HTML | Harness tạm **5/5 đạt**, xuất view qua Spring MVC/Thymeleaf thật | Dùng fixture kiểm thử, không đưa dữ liệu giả vào sản phẩm. |
| Chrome | **48 lượt render**, không tràn ngang document, không JS exception | 11 biến thể × 4 viewport, cộng 4 lượt list ở viewport/reflow khác. |
| Binding | Không mất binding/ID/name/data-handler cũ của 5 template đã đối chiếu | Ngoại trừ markup breadcrumb lặp được bỏ có chủ đích. |
| Tương tác | Bộ lọc/Escape/focus, chọn/bỏ chọn, dialog, sidebar pin 250px, menu mobile 9 link đạt trong preview | Preview chặn POST; không gửi đề xuất hoặc lưu ghi chú thật. |
| CSRF | Form POST trong các bản render có CSRF | Không suy ra bảo mật toàn hệ thống từ kiểm tra này. |
| Màu chữ badge | Warning **5.02:1**, success **6.77:1**, neutral **5.70:1** | Đo đúng cặp chữ/nền badge; không phải chứng nhận WCAG toàn trang. |
| CSS/lint | CSS mới không raw color hoặc `!important`; Taste lint 0 FAIL/0 WARN cho CSS mới | Lint tổng CSS và 4 template có 1 WARN dấu gạch ngang trong nội dung có từ trước. |

Viewport đã kiểm tra: 1440×900, 1366×768, 768×1024, 390×844; list thêm 1920×1080 và reflow 1800×1125, 1152×720, 960×600. Các lượt reflow chỉ tương đương vùng nhìn khi zoom, **chưa phải kiểm chứng thao tác zoom gốc của Chrome**.

Các số liệu trên được đối chiếu lại với work log và artifact QA có sẵn khi xuất báo cáo. Không chạy lại test hoặc render trong lần tạo Markdown này. Tại thời điểm QA của đợt UI, `localhost:8081` không chạy; ảnh lấy từ HTML MockMvc cùng CSS/JS thật. Không kết luận trạng thái chạy hiện tại của ứng dụng từ bằng chứng đó.

## 7. Kiến trúc, skill và giới hạn còn lại

- Dùng UI UX Pro Max cho table/filter/reflow; Impeccable theo Operate/Polish; Taste `Accent ladder`, seed `378800654`, giữ palette/type; Awesome tham khảo phân cấp chữ và ô dữ liệu trong bản phân tích SAP. Không thay nhận diện Mộc bằng palette/font do skill đề xuất.
- CSS mới chỉ phục vụ bốn trang Talent Pool; dùng alias `tpw-` và component `adw-`/`mft-` có sẵn, không sao chép markup sidebar/topbar.
- Việc tái sử dụng `adw-` vẫn phụ thuộc nợ kiến trúc hiện trạng đã được báo cáo: shell được áp một phần qua component CSS/JS opt-in và có hiện tượng stylesheet lặp. Đợt UI này không sửa chúng và không khẳng định đã tuân thủ sạch toàn bộ phân tầng.
- Chưa kiểm thử lưu SQL, gửi email, CV thật, native zoom, FPS/Core Web Vitals hoặc tương phản mọi vùng ảnh nền. Muốn xử lý shell dùng chung cần một tác vụ riêng theo quyền sở hữu UI của hệ thống.

## 8. Tài liệu và bằng chứng liên quan

- [Work log chi tiết](../../management/work_logs/2026-10-28-potential-candidate-admin-ui-sync.md).
- [Mục công việc chung](../../management/WORK_LOG.md).
- [UI Catalog](../../architecture/UI/UI_CATALOG.md).
- [Báo cáo kiến trúc Admin trước đó](2026-10-27-admin-ui-architecture-review.md).
- Artifact QA cục bộ: thư mục QA cục bộ `potential-ui-20261028/`, gồm `quality.json`, `contracts.json`, `interactions.json`, `source-verification.json`, ảnh `before/` và `final/`.

### Ghi chú lệnh khởi động

Lỗi `Unknown lifecycle phase "mvn"` đã hỏi sau đợt UI đến từ lệnh gõ lặp `mvn mvn ...`; không có sửa source để xử lý lỗi đó. Lệnh đúng đã hướng dẫn:

```powershell
mvn clean compile spring-boot:run
```
