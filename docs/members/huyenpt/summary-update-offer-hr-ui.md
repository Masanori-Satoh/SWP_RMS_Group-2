# BÁO CÁO ĐỒNG BỘ GIAO DIỆN PHÂN HỆ OFFER (HR) THEO LAYOUT ADMIN WORKSPACE

- **Dự án:** Hệ Thống Quản Lý Tuyển Dụng — SWP_RMS_Group-2 (Mộc RMS)
- **Nhánh thực hiện (Branch):** `feat/huyenpt/update-offer-hr-ui`
- **Nhánh gốc (Base Branch):** `main`
- **Thành viên phụ trách (Assignee):** Phạm Thị Huyền (`HuyenPT`)
- **Phân hệ (Module):** Quản lý Đề xuất Offer (HR Screens: Danh sách `/offers`, Tạo mới/Chỉnh sửa `/offers/create`, Chi tiết `/offers/{id}`)
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
- Màn hình xem chi tiết đề xuất (`offers/detail.html`) còn dùng layout cũ với nhiều icon FontAwesome và mã script nội tuyến.

### 2. Mục tiêu cốt lõi
1. **Đồng bộ toàn diện layout Admin Workspace (`adw-`):** Đưa toàn bộ các màn hình phía HR của phân hệ Offer (`list.html`, `form.html`, `detail.html`) về chung một hệ thống trải nghiệm thị giác hiện đại, chuyên nghiệp theo phong cách Mộc RMS.
2. **Mở rộng đồng bộ lề hai bên:** Màn hình tạo đề xuất (`offers/create`) và chi tiết đề xuất (`offers/{id}`) mở rộng dàn đều ngang workspace tương tự trang danh sách (`offers`), tối ưu hóa không gian hiển thị và khả năng thao tác của HR.
3. **Tinh giản tiêu đề:** Loại bỏ các dòng mô tả phụ rườm rà dưới tiêu đề chính ở cả 2 màn hình, mang lại diện mạo tối giản, trang nhã.
4. **Loại bỏ 100% FontAwesome CDN & Inline Code:** Chuyển đổi toàn bộ icon sang SVG nội bộ sắc nét; không còn bất kỳ thẻ `<script>` nội tuyến nào.
5. **Đóng gói Modal & Inline Styles vào CSS:** Di dời toàn bộ inline styles trong modal xuất Excel, modal chỉnh sửa nhanh và trang chi tiết vào `offer-workspace.css`.
6. **Tuân thủ nghiêm ngặt 100% Token CSS:** Toàn bộ CSS mới dùng biến ngữ nghĩa từ `tokens.css`, 0 mã màu hex hardcoded, 0 `!important`.
7. **Bảo toàn 100% nghiệp vụ và ranh giới phân quyền:** Tuyệt đối không thay đổi logic backend, giữ nguyên các ID/name/data-attributes phục vụ binding dữ liệu; **chỉ áp dụng cho màn hình HR, chưa đụng tới Director (`director-list.html`) và Candidate**.

---

## 🛠️ II. CHI TIẾT CÁC HẠNG MỤC THAY ĐỔI KỸ THUẬT

### 1. Kế thừa cơ chế Admin Workspace Opt-in (`adw-`)

- **Thẻ `<main>` kích hoạt:** Thêm class `.adw-page ofw-page` trên `<main id="main">` tại cả [list.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/list.html), [form.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/form.html) và [detail.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/detail.html).
- **Phông nền công nghệ:** Nhúng fragment:
  ```html
  <th:block th:replace="~{fragments/ui/admin-workspace :: scene}"></th:block>
  ```
- **Kích hoạt Chrome Script:** Bổ sung fragment scripts ở cuối trang:
  ```html
  <th:block th:replace="~{fragments/ui/admin-workspace :: scripts}"></th:block>
  ```
- **Làm sáng menu điều hướng:** Bổ sung `model.addAttribute("activeMenu", "offers");` tại tất cả các phương thức liên quan trong [OfferController.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java).

---

### 2. Xây dựng Stylesheet Chuyên biệt `offer-workspace.css`

Tệp [offer-workspace.css](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/pages/offer-workspace.css) được mở rộng với 16 phân đoạn rõ ràng:
- **Phạm vi scoped:** Sử dụng tiền tố `ofw-` cho toàn bộ các class nghiệp vụ để không xung đột với các module khác.
- **Tiêu chuẩn Token 100%:** Sử dụng toàn diện biến từ `tokens.css` (màu sắc, khoảng cách, bo góc, bóng đổ).
- **Phân đoạn 15 & 16:** Đóng gói toàn bộ các class cho Modal (Xuất Excel, Sửa nhanh, Giám đốc duyệt) và màn hình Chi tiết (Decline card, Audit footer, Section headers), xóa bỏ sạch sẽ mọi inline `style="..."`.
- **Button DNA chuẩn Mộc RMS:** Toàn bộ nút hành động có chiều cao tiêu chuẩn `var(--btn-h)` (48px) và bo góc chữ nhật `var(--radius-md)` (8px).
- **Hỗ trợ Responsive:** Tối ưu hóa hiển thị mượt mà trên Desktop, Laptop, Tablet đến Mobile.

---

### 3. Màn hình Danh sách Đề xuất (`offers/list.html` — Screen 30)

- **Tiêu đề trang tinh gọn:** Sử dụng `.adw-heading` và `.adw-title` với đường kẻ thương hiệu xanh lá 48×4px. Đã loại bỏ dòng mô tả phụ lặp theo yêu cầu người dùng.
- **5 Thẻ thống kê tổng quan (`.ofw-stats-grid`):** Bọc trong `.ofw-stat-card.adw-surface` với icon SVG trong hộp bo góc màu tương ứng.
- **Thanh công cụ lọc và tìm kiếm (`.ofw-toolbar.adw-surface`):** Ô tìm kiếm kính lúp SVG, nút xóa nhanh, dropdown lọc 42px.
- **Card bề mặt bảng dữ liệu (`.ofw-list-surface.adw-surface`):** Header chuẩn `.ofw-list-header`, icon hộp bo góc `.ofw-list-icon`, tiêu đề `.adw-panel-title`, bảng dữ liệu `.adw-data-table`.
- **Đóng gói Modal & Thay thế 100% FontAwesome sang Inline SVG:**
  - Modal Xuất Excel, Modal Sửa nhanh, Modal Quyết định Giám đốc hoàn toàn dùng class `ofw-`.
  - Thay toàn bộ icon trạng thái và nút hành động sang inline SVG.
  - Xóa bỏ link nạp `font-awesome/6.4.0` CDN.

---

### 4. Màn hình Tạo mới & Chỉnh sửa Đề xuất (`offers/form.html` — Screen 31)

- **Mở rộng toàn bộ lề hai bên:** Cập nhật `.ofw-form-wrap` thành `max-width: none; margin-inline: 0;`, dàn đều ngang toàn bộ không gian làm việc.
- **Tiêu đề phân cấp rõ ràng:** Tiêu đề `.adw-title` độc lập, phân vùng nhập liệu thành **2 bước (Cards)** với số bước tròn `.ofw-step-badge`.
- **Hỗ trợ tính toán 85% lương thử việc:** Nút "Gợi ý 85%" tự động tính toán, thông báo phản hồi `.ofw-rule-notice` đổi màu và icon SVG thời gian thực.
- **Xử lý lỗi và thông báo:** Thay thế alert thủ công bằng `.adw-validation-summary` và `fragments/ui/flash-toast :: flashToast`.

---

### 5. Màn hình Chi tiết Đề xuất (`offers/detail.html` — Screen 32)

- **Đồng bộ Admin Workspace Layout:** Gắn `.adw-page ofw-page offer-detail-container`, nhúng `scene` và `scripts` admin.
- **Thay thế 100% FontAwesome sang Inline SVG:** Toàn bộ ~30 vị trí icon FontAwesome (briefcase, user, calendar, arrow-left, file-lines, location-dot, clock, user-pen, etc.) chuyển sang SVG chuẩn.
- **Xóa bỏ script nội tuyến:** Chuyển flash alert cũ sang `fragments/ui/flash-toast :: flashToast` tự động đóng không cần script nhúng.
- **Xóa bỏ toàn bộ inline styles:** Đóng gói thành các class ngữ nghĩa `ofw-cand-decline-header`, `ofw-cand-decline-title`, `ofw-detail-section-title`, `ofw-detail-audit-footer`, v.v.

---

## 🔒 III. BẢO TOÀN 100% NGHIỆP VỤ & HỢP ĐỒNG DỮ LIỆU

| Hạng mục | Hiện trạng sau đồng bộ |
|---|---|
| **View Paths & Routes** | Giữ nguyên: `GET /offers`, `GET /offers/create`, `POST /offers/create`, `GET /offers/{id}`, `GET /offers/{id}/edit`, `POST /offers/{id}/edit`, `POST /offers/{id}/delete`, `POST /offers/export`. |
| **Form Data Bindings** | Giữ nguyên 100% `th:object="${offerDto}"` và các trường dữ liệu. |
| **DOM IDs cho JavaScript** | Giữ nguyên toàn bộ: `createAppSelect`, `groupASection`, `dispCandId`, `dispCandName`, `dispCandEmail`, `dispPosition`, `dispDept`, `dispRecomSalary`, `dispInterviewComments`, `probationRuleNotice`, `searchInput`, `statusFilter`, `timeSortFilter`, `selectAllOffers`, `.offer-row-checkbox`, `directorDetailComments`. |
| **Các Pop-up Modal** | Modal Xuất Excel (`#offerExportModal`), Modal Sửa nhanh (`#editOfferModal`), Modal Duyệt (`#directorDecisionModal`) đều hoạt động bình thường với đầy đủ CSRF token. |
| **Phân quyền người dùng** | Giữ nguyên vai trò `HR`/`Recruiter`, không ép quyền `System Admin`. **Không thay đổi mã nguồn của Director (`director-list.html`) hay Candidate**. |

---

## 📊 IV. ĐÁNH GIÁ MỨC ĐỘ TUÂN THỦ ARCHITECTURE_GUIDE.MD

| Tiêu chuẩn trong Guide | Mức độ | Minh chứng kỹ thuật |
|---|:---:|---|
| **Layout Level 3 (Kế thừa layout chung)** | ✅ Đạt 100% | Sử dụng `layout:decorate="~{layout/internal}"`, kích hoạt `.adw-page`, `scene` và `scripts` trên cả 3 màn hình. |
| **Quy chuẩn Token CSS (No Raw Hex)** | ✅ Đạt 100% | 0 mã hex hardcoded trong `offer-workspace.css`, sử dụng 100% biến từ `tokens.css`. |
| **Button DNA Mộc RMS** | ✅ Đạt 100% | Nút chữ nhật bo góc 8px (`var(--radius-md)`), cao 48px, không dùng nút bo tròn pill. |
| **Zero Inline Code** | ✅ Đạt 100% | Đã loại bỏ toàn bộ thẻ `<script>` nội tuyến khỏi cả 3 template `list.html`, `form.html`, `detail.html`. |
| **Zero Inline Styles** | ✅ Đạt 100% | Không còn các thuộc tính `style="..."` về layout, margin, padding, color trong template. |
| **Chuẩn Icon Nội bộ (No Font CDN)** | ✅ Đạt 100% | Gỡ bỏ hoàn toàn FontAwesome CDN; 100% icon chuyển thành inline SVG có ngữ nghĩa. |
| **Accessibility (a11y)** | ✅ Đạt 100% | Đầy đủ `tabindex="-1"` trên `<main>`, `aria-label`, `aria-hidden="true"` cho icon trang trí. |

---

## 🧪 V. KẾT QUẢ KIỂM THỬ TỰ ĐỘNG (TEST RESULTS)

```powershell
./mvnw.cmd test "-Dtest=OfferIntegrationTests,OfferControllerBugTests"
```

- **`OfferIntegrationTests`**: **31/31 PASSED**
- **`OfferControllerBugTests`**: **3/3 PASSED**
- **Tổng cộng**: **34/34 PASSED (100%)**

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.group2.rms.offer.controller.OfferControllerBugTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 20.63 s
[INFO] Running com.group2.rms.offer.OfferIntegrationTests
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.046 s
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
- `src/main/resources/static/css/pages/offer-workspace.css` (Mở rộng: Thêm Section 15 & 16 đóng gói toàn bộ modals & detail page)
- `src/main/resources/templates/offers/list.html` (Sửa: Đồng bộ layout Admin, inline SVG, bỏ subtitle, đóng gói inline modal styles)
- `src/main/resources/templates/offers/form.html` (Sửa: Đồng bộ layout Admin, mở rộng lề, bỏ subtitle)
- `src/main/resources/templates/offers/detail.html` (Sửa: Đồng bộ layout Admin, inline SVG, bỏ FA CDN, bỏ flash script, đóng gói inline styles)
- `src/main/resources/static/js/offers.js` (Sửa: Hàm validate hiển thị inline SVG)
- `docs/architecture/UI/UI_CATALOG.md` (Cập nhật hướng dẫn UI)
- `docs/management/WORK_LOG.md` (Cập nhật nhật ký công việc)
- `docs/management/work_logs/2026-10-31-hr-offer-proposal-admin-ui-sync.md` (Cập nhật: Chi tiết kỹ thuật)
- `docs/members/huyenpt/summary-update-offer-hr-ui.md` (Cập nhật: Báo cáo tổng kết)

### 2. Lịch sử commit trên branch `feat/huyenpt/update-offer-hr-ui`
```text
a5ecf60 refactor(offer): extract inline modal and card styles into offer-workspace.css
4ca3b65 style(offer): sync HR offer detail screen with admin workspace layout and inline SVGs
4c6c328 merge: sync with origin/main and resolve conflict in WORK_LOG.md
bc9995f docs(offer): add summary report for HR offer UI synchronization
086f7b1 style(offer): remove redundant subtitle descriptions from HR offer headers
caa3abf style(offer): expand create offer proposal form width to match offer workspace
26eeced feat(offer): sync HR offer screens with admin workspace layout
```
