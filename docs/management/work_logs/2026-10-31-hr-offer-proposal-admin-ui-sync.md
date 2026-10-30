# Đồng bộ Giao Diện Các Màn Hình Đề Xuất Offer Của HR Theo Layout Admin

- **Ngày:** 31/10/2026.
- **Người thực hiện:** HuyenPT.
- **Yêu cầu đã làm rõ:** Đồng bộ toàn diện các màn hình của HR trong luồng Offer gồm Danh sách Đề xuất (`offers/list.html`) và Tạo mới/Chỉnh sửa Đề xuất (`offers/form.html`) theo thiết kế Layout Admin (`adw-`), tuân thủ chặt chẽ `ARCHITECTURE_GUIDE.md`, các quy ước UI và bảo toàn 100% nghiệp vụ cũng như hợp đồng dữ liệu hiện có. Phạm vi chỉ áp dụng cho HR, chưa đụng tới Director và Candidate.
- **Phạm vi:** Frontend Thymeleaf, CSS scoped mới, tối ưu icon SVG, Controller `activeMenu`, tài liệu và kiểm thử tích hợp.

---

## 1. Danh Sách File Thay Đổi

| Loại | File | Nội dung thay đổi |
| --- | --- | --- |
| **Sửa** | `src/main/java/com/group2/rms/offer/controller/OfferController.java` | Bổ sung `model.addAttribute("activeMenu", "offers")` cho các luồng `getAllOffersForHr`, `createOfferForm`, `createOffer` (validation error), `editOfferForm`, `updateOffer` (validation error) và `viewOfferDetail` để sidebar luôn sáng đúng mục. |
| **Tạo mới** | `src/main/resources/static/css/pages/offer-workspace.css` | Stylesheet scoped với tiền tố `ofw-`: bố cục khung 880px (`.ofw-form-wrap`), header, card surfaces, lưới thông tin ứng viên, badge lương, rule notice 85%, footer actions, toolbar, data table và responsive breakpoints. Sử dụng 100% CSS variables từ `tokens.css`. |
| **Sửa** | `src/main/resources/templates/offers/list.html` | Kế thừa opt-in Admin workspace (`.adw-page`, `fragments/ui/admin-workspace :: scene` và `:: scripts`); chuẩn hóa tiêu đề `.adw-title`, stat overview cards, toolbar `.adw-surface`, card danh sách `.adw-surface`, table `.adw-data-table`; thay toàn bộ icon FontAwesome CDN bằng inline SVG nội bộ; tích hợp `flash-toast`. |
| **Sửa** | `src/main/resources/templates/offers/form.html` | Kế thừa opt-in Admin workspace (`.adw-page`, `fragments/ui/admin-workspace :: scene` và `:: scripts`); chuẩn hóa tiêu đề `.adw-title`, bề mặt thẻ `.adw-surface`, SVG icons nội bộ; loại bỏ thẻ `<script>` nội tuyến và thay thế alert tự chế bằng `.adw-validation-summary` cùng `flash-toast`. |
| **Sửa** | `src/main/resources/static/js/offers.js` | Tinh chỉnh hàm `validateProbationRuleUi()` hiển thị icon SVG sắc nét và class `.ofw-rule-notice--invalid` / `.ofw-rule-notice--valid` độc lập không phụ thuộc FontAwesome CDN. |
| **Thêm mục cuối** | `docs/architecture/UI/UI_CATALOG.md` | Bổ sung mục tra cứu cách tái sử dụng Offer Workspace (`ofw-`) kết hợp Admin Workspace (`adw-`). |
| **Thêm dòng** | `docs/management/WORK_LOG.md` | Cập nhật nhật ký công việc ngày 31/10/2026. |

---

## 2. Cách Áp Dụng Layout & Quyết Định UI

- **Opt-in Workspace không đổi phân quyền:** Tiếp tục kế thừa `layout/internal`, kích hoạt nền công nghệ mộc và hiệu ứng khung bằng marker `.adw-page`, fragment `fragments/ui/admin-workspace :: scene` và script `admin-workspace.js`. Giữ nguyên vai trò `HR` / `Recruiter`, không ép quyền `System Admin`.
- **Đồng bộ màn hình Danh sách (`/offers`):**
  - Tiêu đề trang `.adw-heading` và `.adw-title` (36px desktop / 28px mobile, gạch chân thương hiệu 48×4px).
  - 5 thẻ thống kê tổng quan với icon SVG tương ứng, border `var(--line)`, shadow `var(--shadow-sm)` và bo góc `var(--radius-lg)`.
  - Thanh công cụ lọc và tìm kiếm bọc trong `.ofw-toolbar.adw-surface`.
  - Danh sách đề xuất bọc trong `.adw-surface` với header chuẩn `.ofw-list-header`, icon hộp bo góc `.ofw-list-icon`, tiêu đề `.adw-panel-title` và badge đếm số lượng `.ofw-list-count`.
  - Bảng dữ liệu dùng `.adw-data-table` với header xanh đậm thương hiệu Mộc RMS, zebra/hover rows nhẹ nhàng và badge trạng thái rõ nét.
- **Mở rộng toàn bộ lề 2 bên màn hình Form (`/offers/create`):**
  - Mở rộng toàn bộ form dàn đều ngang workspace giống trang Danh sách (`/offers`), loại bỏ giới hạn 880px để thẻ thông tin ứng viên và điều khoản hợp đồng lấp đầy không gian hiển thị, đồng bộ lề 2 bên với thanh topbar và sidebar.
  - Thẻ thông tin ứng viên và điều khoản đãi ngộ dùng `.adw-surface` với bo góc `var(--radius-lg)`, viền `var(--line)` và shadow `var(--shadow-md)`.
  - Tiêu đề phân mục dùng `.adw-panel-heading` và `.adw-panel-title` kèm badge số bước `1`, `2` tròn tinh tế.
  - Lưới thông tin ứng viên `ofw-info-grid` với label uppercase xám nhẹ và giá trị semibold, mức lương gợi ý được bọc trong badge nổi bật `var(--brand-soft)`.
- **Button DNA chuẩn Mộc:** Nút hình chữ nhật bo góc 8px (`var(--radius-md)`), cao 48px cho nút chính, không dùng nút bo tròn pill.
- **Loại bỏ inline script & CDN:** Xóa hoàn toàn script tự đóng flash message nội tuyến và link FontAwesome ngoài; dùng fragment `fragments/ui/flash-toast :: flashToast` chuẩn hệ thống và 100% SVG nội bộ.

---

## 3. Nghiệp Vụ & Dữ Liệu Được Bảo Toàn 100%

- Giữ nguyên toàn bộ view paths (`offers/list`, `offers/form`), form actions (`/offers/create`, `/offers/{id}/edit`), HTTP methods, CSRF token.
- Giữ nguyên 100% form bindings: `th:object="${offerDto}"`, các `th:field`: `applicationId`, `offeredPositionTitle`, `proposedSalary`, `probationSalary`, `probationDays`, `responsePeriodDays`, `expectedStartDate`, `workLocation`, `benefitsPackage`.
- Giữ nguyên toàn bộ DOM ID để `offers.js` bắt sự kiện và tính toán tự động:
  - Form: `createAppSelect`, `groupASection`, `dispCandId`, `dispCandName`, `dispCandEmail`, `dispCandPhone`, `dispPosition`, `dispDept`, `dispReqId`, `dispHm`, `dispRecomSalary`, `dispInterviewComments`, `dispWorkLocation`, `probationRuleNotice`.
  - List: `searchInput`, `statusFilter`, `timeSortFilter`, `selectAllOffers`, `.offer-row-checkbox`, `selectedOffersCount`, `exportModal`, `deleteOfferModal`, `editOfferModal`.
- Bảo toàn trọn vẹn cả 3 ngữ cảnh:
  1. Tạo mới từ hồ sơ phỏng vấn đạt (Khóa ứng viên cố định `fixedCandidate`).
  2. Tạo mới tự do từ danh sách ứng viên đỗ (`passedCandidates`).
  3. Chỉnh sửa bản nháp đề xuất (`detail`).
- Không sửa Database schema, migration, seed hay Service backend.

---

## 4. Kết Quả Kiểm Thử (Testing Results)

- **Kiểm thử tự động (JUnit 5 + Spring Boot Test):**
  - Chạy `OfferIntegrationTests` và `OfferControllerBugTests`:
    - **34/34 tests đạt 100% (Failures: 0, Errors: 0, Skipped: 0)**.
    - Thời gian chạy: 01:12 min.
    - `BUILD SUCCESS`.
- **Kiểm tra cú pháp & Kiến trúc:**
  - `offer-workspace.css`: 100% token CSS, 0 raw hex color, 0 `!important`.
  - Cả 2 template `list.html` và `form.html`: `tabindex="-1"` chuẩn Accessibility, không còn thẻ `<script>` nội tuyến, 100% SVG inline.
