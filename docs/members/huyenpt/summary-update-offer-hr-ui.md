# BÁO CÁO ĐỒNG BỘ GIAO DIỆN PHÂN HỆ OFFER (HR) THEO LAYOUT ADMIN WORKSPACE

- **Dự án:** Hệ Thống Quản Lý Tuyển Dụng — SWP_RMS_Group-2 (Mộc RMS)
- **Nhánh thực hiện (Branch):** `feat/huyenpt/update-offer-hr-ui`
- **Nhánh gốc (Base Branch):** `main`
- **Thành viên phụ trách (Assignee):** Phạm Thị Huyền (`HuyenPT`)
- **Phân hệ (Module):** Quản lý Đề xuất Offer (HR Screens: Danh sách `/offers` & Tạo mới/Chỉnh sửa `/offers/create`)
- **Tài liệu tham chiếu:**
  - Quy chuẩn kiến trúc hệ thống: [`docs/architecture/ARCHITECTURE_GUIDE.md`](../../architecture/ARCHITECTURE_GUIDE.md)
  - Hướng dẫn thành phần UI: [`docs/architecture/UI/UI_CATALOG.md`](../../architecture/UI/UI_CATALOG.md)
  - Nhật ký công việc dự án: [`docs/management/WORK_LOG.md`](../../management/WORK_LOG.md)
  - Nhật ký kỹ thuật: [`docs/management/work_logs/2026-10-31-hr-offer-proposal-admin-ui-sync.md`](../../management/work_logs/2026-10-31-hr-offer-proposal-admin-ui-sync.md)

---

## 📌 I. MỤC TIÊU & BỐI CẢNH TRIỂN KHAI

### 1. Bối cảnh
Trước đợt cập nhật này, giao diện các màn hình của HR trong luồng Quản lý Đề xuất nhận việc (Offer Management) vẫn giữ giao diện cũ:
- Nền trang màu trắng phẳng, chưa có hiệu ứng nền văn phòng mờ nhận diện (`admin-tech-office.png`).
- Thanh điều hướng Sidebar và Topbar chưa kích hoạt hiệu ứng kính mờ (glassmorphism) và khung nổi đồng bộ với các màn hình chính như **Tin tuyển dụng** (`/internal/job-postings`) và **Yêu cầu tuyển dụng** (`/requisitions`).
- Tiêu đề chưa có đường gạch chân thương hiệu 48×4px chuẩn `adw-title`.
- Còn phụ thuộc vào thư viện FontAwesome qua CDN ngoài (`cdnjs.cloudflare.com`), chưa chuyển đổi sang hệ thống inline SVG đồng nhất của dự án.
- Màn hình tạo đề xuất trước đó bị thu hẹp trong khung 880px, tạo khoảng trống lề hai bên quá lớn so với trang danh sách.

### 2. Mục tiêu cốt lõi
1. **Đồng bộ toàn diện layout Admin Workspace (`adw-`):** Đưa toàn bộ các màn hình phía HR của phân hệ Offer về chung một hệ thống trải nghiệm thị giác hiện đại, chuyên nghiệp theo phong cách Mộc RMS.
2. **Mở rộng đồng bộ lề hai bên:** Màn hình tạo đề xuất (`offers/create`) mở rộng dàn đều ngang workspace tương tự trang danh sách (`offers`), tối ưu hóa không gian nhập liệu và khả năng quét dữ liệu của HR.
3. **Tinh giản tiêu đề:** Loại bỏ các dòng mô tả phụ rườm rà dưới tiêu đề chính ở cả 2 màn hình, mang lại diện mạo tối giản, trang nhã.
4. **Loại bỏ 100% FontAwesome CDN & Inline Code:** Chuyển đổi toàn bộ icon sang SVG nội bộ sắc nét; không còn bất kỳ thẻ `<script>` nội tuyến nào.
5. **Tuân thủ nghiêm ngặt 100% Token CSS:** Toàn bộ CSS mới dùng biến ngữ nghĩa từ `tokens.css`, 0 mã màu hex hardcoded, 0 `!important`.
6. **Bảo toàn 100% nghiệp vụ và ranh giới phân quyền:** Tuyệt đối không thay đổi logic backend, giữ nguyên các ID/name/data-attributes phục vụ binding dữ liệu; **chỉ áp dụng cho màn hình HR, chưa đụng tới Director (`director-list.html`) và Candidate**.

---

## 🛠️ II. CHI TIẾT CÁC HẠNG MỤC THAY ĐỔI KỸ THUẬT

### 1. Kế thừa cơ chế Admin Workspace Opt-in (`adw-`)

- **Thẻ `<main>` kích hoạt:** Thêm class `.adw-page ofw-page` trên `<main id="main">` tại cả [list.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/list.html) và [form.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/form.html).
- **Phông nền công nghệ:** Nhúng fragment:
  ```html
  <th:block th:replace="~{fragments/ui/admin-workspace :: scene}"></th:block>
  ```
- **Kích hoạt Chrome Script:** Bổ sung fragment scripts ở cuối trang:
  ```html
  <th:block th:replace="~{fragments/ui/admin-workspace :: scripts}"></th:block>
  ```
  Script này tự động gắn class `adw-frame`, `adw-sidebar`, `adw-topbar`, `adw-actions` để tạo hiệu ứng floating rail cho sidebar và glassmorphism cho thanh công cụ phía trên.
- **Làm sáng menu điều hướng:** Bổ sung `model.addAttribute("activeMenu", "offers");` tại tất cả các phương thức liên quan trong [OfferController.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java), đảm bảo icon "Đề nghị tuyển dụng" trên sidebar luôn được active chính xác.

---

### 2. Xây dựng Stylesheet Chuyên biệt `offer-workspace.css`

Tạo mới tệp [offer-workspace.css](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/pages/offer-workspace.css) (744 dòng mã nguồn chất lượng cao):
- **Phạm vi scoped:** Sử dụng tiền tố `ofw-` cho toàn bộ các class nghiệp vụ để không xung đột với các module khác.
- **Tiêu chuẩn Token 100%:** Sử dụng toàn diện biến từ `tokens.css`:
  - Màu sắc: `var(--brand)`, `var(--brand-dark)`, `var(--brand-soft)`, `var(--surface)`, `var(--surface-subtle)`, `var(--line)`, `var(--ink)`, `var(--muted)`, `var(--error)`, `var(--warning)`, `var(--success)`.
  - Khoảng cách: `var(--s1)` đến `var(--s6)`.
  - Bo góc: `var(--radius-sm)`, `var(--radius-md)`, `var(--radius-lg)`, `var(--radius-full)`.
  - Đổ bóng: `var(--shadow-sm)`, `var(--shadow-md)`.
- **Button DNA chuẩn Mộc RMS:** Toàn bộ nút hành động có chiều cao tiêu chuẩn `var(--btn-h)` (48px) và bo góc chữ nhật `var(--radius-md)` (8px), không dùng nút bo tròn viên thuốc (pill) không đúng chuẩn thương hiệu.
- **Hỗ trợ Responsive:** Tối ưu hóa hiển thị mượt mà trên mọi kích thước màn hình từ Desktop (1920px, 1440px), Laptop (1366px), Tablet (768px) đến Mobile (390px).

---

### 3. Màn hình Danh sách Đề xuất (`offers/list.html` — Screen 30)

- **Tiêu đề trang tinh gọn:** Sử dụng `.adw-heading` và `.adw-title` với đường kẻ thương hiệu xanh lá 48×4px. Đã loại bỏ dòng mô tả phụ lặp theo yêu cầu người dùng để tiêu đề ngang bằng với các nút hành động bên phải.
- **5 Thẻ thống kê tổng quan (`.ofw-stats-grid`):**
  - Hiển thị 5 chỉ số: Tổng đề xuất, Chờ Giám đốc duyệt, Giám đốc đã duyệt, Đã gửi thư mời, Ứng viên chấp nhận.
  - Từng thẻ bọc trong `.ofw-stat-card.adw-surface`, có icon SVG trong hộp bo góc màu tương ứng (`stat-icon-brand`, `stat-icon-amber`, `stat-icon-green`, `stat-icon-blue`).
- **Thanh công cụ lọc và tìm kiếm (`.ofw-toolbar.adw-surface`):**
  - Ô tìm kiếm có icon kính lúp SVG và nút xóa nhanh (`.ofw-search-clear`).
  - Dropdown lọc trạng thái và sắp xếp thời gian có chiều cao 42px đồng nhất.
- **Card bề mặt bảng dữ liệu (`.ofw-list-surface.adw-surface`):**
  - Header chuẩn mực `.ofw-list-header`: Icon hộp bo góc `.ofw-list-icon`, tiêu đề bảng `.adw-panel-title` và badge đếm tổng số lượng đề xuất `.ofw-list-count`.
  - Bảng dữ liệu `.adw-data-table`: Hàng tiêu đề nền xám nhạt chữ xanh đậm `var(--brand-dark)`, hàng dữ liệu có hiệu ứng hover êm ái, căn chỉnh cột số tiền tệ rõ ràng kèm badge phần trăm lương thử việc.
- **Thay thế 100% FontAwesome sang Inline SVG:**
  - Thay toàn bộ icon trạng thái (Bản nháp, Chờ GĐ duyệt, GĐ đã duyệt, GĐ từ chối, Đã gửi thư mời, Ứng viên chấp nhận, Ứng viên từ chối).
  - Thay toàn bộ icon nút hành động (Xem chi tiết, Phát hành thư mời, Chỉnh sửa, Xóa bản nháp).
  - Xóa bỏ link nạp `font-awesome/6.4.0` CDN khỏi thẻ `<head>`.

---

### 4. Màn hình Tạo mới & Chỉnh sửa Đề xuất (`offers/form.html` — Screen 31)

- **Mở rộng toàn bộ lề hai bên:**
  - Loại bỏ hoàn toàn khối bao bọc `.adw-form-wrap` (giới hạn 880px trước đây).
  - Cập nhật `.ofw-form-wrap` thành `max-width: none; margin-inline: 0;`.
  - Header và các thẻ Form giờ đây trải đều ngang toàn bộ không gian làm việc của `.page-content.container.adw-page`, căn lề 16px (`var(--s4)`) chuẩn xác với topbar và sidebar.
- **Tiêu đề phân cấp rõ ràng:**
  - Tiêu đề chính `.adw-title` độc lập, loại bỏ dòng mô tả phụ dài dòng.
  - Phân vùng nhập liệu thành **2 bước (Cards)** trực quan với số thứ tự bước tròn `.ofw-step-badge` mang phong cách hiện đại:
    - **Bước 1 — Thông tin Ứng viên Đỗ Phỏng vấn:** Dropdown chọn ứng viên đỗ, bảng thông tin trích xuất 3 cột `.ofw-info-grid` với badge lương khuyến nghị nổi bật `.ofw-salary-badge`.
    - **Bước 2 — Thông tin Đãi ngộ & Điều khoản Đề xuất:** Lưới 2 cột `.grid-2-col` cho mức lương, thời gian thử việc, ngày bắt đầu và địa điểm làm việc.
- **Hỗ trợ tính toán 85% lương thử việc:**
  - Nút "Gợi ý 85%" tự động tính toán nhanh mức sàn theo Luật Lao động.
  - Thông báo phản hồi trực quan `.ofw-rule-notice` đổi màu và icon SVG thời gian thực khi tỷ lệ đạt hoặc không đạt điều kiện.
- **Xử lý lỗi và thông báo:**
  - Thay thế khối alert thủ công bằng `.adw-validation-summary` đồng bộ với hệ thống.
  - Thông báo flash qua `fragments/ui/flash-toast :: flashToast` tự động biến mất sau 3 giây, loại bỏ hoàn toàn các đoạn code `<script>` nội tuyến.

---

### 5. Tinh chỉnh Tầng JavaScript `offers.js`

- Trong hàm `validateProbationRuleUi()`:
  - Thay thế các thẻ `<i class="fa-solid ..."></i>` bằng chuỗi SVG nội bộ inline.
  - Tự động thêm/bỏ class `.ofw-rule-notice--invalid` và dùng màu CSS token thay vì màu cứng.

---

## 🔒 III. BẢO TOÀN 100% NGHIỆP VỤ & HỢP ĐỒNG DỮ LIỆU

Mọi chỉnh sửa trong đợt này đều tuân thủ chặt chẽ nguyên tắc bảo toàn nghiệp vụ (Zero Regression):

| Hạng mục | Hiện trạng sau đồng bộ |
|---|---|
| **View Paths & Routes** | Giữ nguyên: `GET /offers`, `GET /offers/create`, `POST /offers/create`, `GET /offers/{id}`, `GET /offers/{id}/edit`, `POST /offers/{id}/edit`, `POST /offers/export`. |
| **Form Data Bindings** | Giữ nguyên 100% `th:object="${offerDto}"` và các trường: `applicationId`, `offeredPositionTitle`, `proposedSalary`, `probationSalary`, `probationDays`, `responsePeriodDays`, `expectedStartDate`, `workLocation`, `benefitsPackage`. |
| **DOM IDs cho JavaScript** | Giữ nguyên toàn bộ: `createAppSelect`, `groupASection`, `dispCandId`, `dispCandName`, `dispCandEmail`, `dispPosition`, `dispDept`, `dispRecomSalary`, `dispInterviewComments`, `probationRuleNotice`, `searchInput`, `statusFilter`, `timeSortFilter`, `selectAllOffers`, `.offer-row-checkbox`. |
| **Các Pop-up Modal** | Modal Xuất Excel (`#offerExportModal`), Modal Sửa nhanh (`#editOfferModal`), Modal Xóa bản nháp đều hoạt động bình thường với đầy đủ CSRF token. |
| **Phân quyền người dùng** | Giữ nguyên vai trò `HR`/`Recruiter`, không ép quyền `System Admin`. **Không thay đổi mã nguồn của Director (`director-list.html`) hay Candidate**. |

---

## 📊 IV. ĐÁNH GIÁ MỨC ĐỘ TUÂN THỦ ARCHITECTURE_GUIDE.MD

| Tiêu chuẩn trong Guide | Mức độ | Minh chứng kỹ thuật |
|---|:---:|---|
| **Layout Level 3 (Kế thừa layout chung)** | ✅ Đạt 100% | Sử dụng `layout:decorate="~{layout/internal}"`, kích hoạt `.adw-page`, `scene` và `scripts`. |
| **Quy chuẩn Token CSS (No Raw Hex)** | ✅ Đạt 100% | 0 mã hex hardcoded trong `offer-workspace.css`, sử dụng 100% biến từ `tokens.css`. |
| **Button DNA Mộc RMS** | ✅ Đạt 100% | Nút chữ nhật bo góc 8px (`var(--radius-md)`), cao 48px, không dùng nút bo tròn pill. |
| **Zero Inline Code** | ✅ Đạt 100% | Đã loại bỏ toàn bộ thẻ `<script>` nội tuyến khỏi `form.html` và `list.html`. |
| **Chuẩn Icon Nội bộ (No Font CDN)** | ✅ Đạt 100% | Gỡ bỏ hoàn toàn FontAwesome CDN; 100% icon chuyển thành inline SVG có ngữ nghĩa. |
| **Accessibility (a11y)** | ✅ Đạt 100% | Đầy đủ `tabindex="-1"` trên `<main>`, `aria-label`, `aria-hidden="true"` cho icon trang trí. |

---

## 🧪 V. KẾT QUẢ KIỂM THỬ TỰ ĐỘNG (TEST RESULTS)

Đã chạy toàn bộ bộ kiểm thử tích hợp và kiểm thử hồi quy của phân hệ Offer thông qua Maven wrapper:

```powershell
./mvnw.cmd test "-Dtest=OfferIntegrationTests,OfferControllerBugTests"
```

### Kết quả chi tiết:
- **`OfferIntegrationTests`**: **31/31 PASSED** (Bao gồm kiểm thử tải trang danh sách, tạo mới, chỉnh sửa, validation dữ liệu, kiểm tra quyền hạn, xuất Excel).
- **`OfferControllerBugTests`**: **3/3 PASSED** (Kiểm tra các trường hợp biên và xử lý ngoại lệ nghiệp vụ).
- **Tổng kết:**
  ```text
  [INFO] -------------------------------------------------------
  [INFO]  T E S T S
  [INFO] -------------------------------------------------------
  [INFO] Running com.group2.rms.offer.controller.OfferControllerBugTests
  [INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 19.47 s
  [INFO] Running com.group2.rms.offer.OfferIntegrationTests
  [INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.102 s
  [INFO] 
  [INFO] Results:
  [INFO] 
  [INFO] Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
  [INFO] 
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  ```

---

## 📜 VI. DANH SÁCH FILE THAY ĐỔI & LỊCH SỬ COMMIT GIT

### 1. Danh sách file thay đổi
- `src/main/java/com/group2/rms/offer/controller/OfferController.java` (Sửa: Bổ sung `activeMenu`)
- `src/main/resources/static/css/pages/offer-workspace.css` (Tạo mới: 744 dòng CSS chuẩn token)
- `src/main/resources/templates/offers/list.html` (Sửa: Đồng bộ layout Admin, inline SVG, bỏ subtitle)
- `src/main/resources/templates/offers/form.html` (Sửa: Đồng bộ layout Admin, mở rộng lề, bỏ subtitle)
- `src/main/resources/static/js/offers.js` (Sửa: Hàm validate hiển thị inline SVG)
- `docs/architecture/UI/UI_CATALOG.md` (Cập nhật hướng dẫn UI)
- `docs/management/WORK_LOG.md` (Cập nhật nhật ký công việc)
- `docs/management/work_logs/2026-10-31-hr-offer-proposal-admin-ui-sync.md` (Tạo mới: Log kỹ thuật)
- `docs/members/huyenpt/summary-update-offer-hr-ui.md` (Tạo mới: Báo cáo tổng kết)

### 2. Lịch sử commit trên branch `feat/huyenpt/update-offer-hr-ui`
```text
086f7b1 style(offer): remove redundant subtitle descriptions from HR offer headers
caa3abf style(offer): expand create offer proposal form width to match offer workspace
26eeced feat(offer): sync HR offer screens with admin workspace layout
```

