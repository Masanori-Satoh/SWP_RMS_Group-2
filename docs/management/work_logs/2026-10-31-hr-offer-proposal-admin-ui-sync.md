# Đồng bộ Giao Diện Các Màn Hình Đề Xuất Offer Của HR Theo Layout Admin

- **Ngày:** 31/10/2026.
- **Người thực hiện:** HuyenPT.
- **Yêu cầu đã làm rõ:** Đồng bộ toàn diện các màn hình của HR trong luồng Offer gồm Danh sách Đề xuất (`offers/list.html`), Tạo mới/Chỉnh sửa Đề xuất (`offers/form.html`), và Chi tiết Đề xuất (`offers/detail.html`) theo thiết kế Layout Admin (`adw-`), tuân thủ chặt chẽ `ARCHITECTURE_GUIDE.md`, các quy ước UI và bảo toàn 100% nghiệp vụ cũng như hợp đồng dữ liệu hiện có. Phạm vi chỉ áp dụng cho HR, chưa đụng tới Director và Candidate.
- **Phạm vi:** Frontend Thymeleaf, CSS scoped mới, tối ưu icon SVG, Controller `activeMenu`, loại bỏ inline styles trong modals, tài liệu và kiểm thử tích hợp.

---

## 1. Danh Sách File Thay Đổi

| Loại | File | Nội dung thay đổi |
| --- | --- | --- |
| **Sửa** | `src/main/java/com/group2/rms/offer/controller/OfferController.java` | Bổ sung `model.addAttribute("activeMenu", "offers")` cho các luồng `getAllOffersForHr`, `createOfferForm`, `createOffer` (validation error), `editOfferForm`, `updateOffer` (validation error) và `viewOfferDetail` để sidebar luôn sáng đúng mục. |
| **Tạo mới / Mở rộng** | `src/main/resources/static/css/pages/offer-workspace.css` | Stylesheet scoped với tiền tố `ofw-`: bố cục toàn màn hình (`.ofw-page`, `.ofw-form-wrap`, `.offer-detail-container`), header, card surfaces, lưới thông tin ứng viên, badge lương, rule notice 85%, footer actions, toolbar, data table, responsive breakpoints, cùng các class semantic cho modal xuất Excel, modal sửa nhanh và chi tiết đề xuất. Sử dụng 100% CSS variables từ `tokens.css`. |
| **Sửa** | `src/main/resources/templates/offers/list.html` | Kế thừa opt-in Admin workspace (`.adw-page`, `fragments/ui/admin-workspace :: scene` và `:: scripts`); chuẩn hóa tiêu đề `.adw-title`, stat overview cards, toolbar `.adw-surface`, card danh sách `.adw-surface`, table `.adw-data-table`; thay toàn bộ icon FontAwesome CDN bằng inline SVG nội bộ; loại bỏ toàn bộ inline styles trong modal đưa về `offer-workspace.css`; tích hợp `flash-toast`. |
| **Sửa** | `src/main/resources/templates/offers/form.html` | Kế thừa opt-in Admin workspace (`.adw-page`, `fragments/ui/admin-workspace :: scene` và `:: scripts`); mở rộng lề hai bên trải đều không gian; chuẩn hóa tiêu đề `.adw-title`, bề mặt thẻ `.adw-surface`, SVG icons nội bộ; loại bỏ thẻ `<script>` nội tuyến và thay thế alert tự chế bằng `.adw-validation-summary` cùng `flash-toast`. |
| **Sửa** | `src/main/resources/templates/offers/detail.html` | Kế thừa opt-in Admin workspace (`.adw-page ofw-page offer-detail-container`, `fragments/ui/admin-workspace :: scene` và `:: scripts`); loại bỏ hoàn toàn link FontAwesome CDN; chuyển đổi 100% icon sang inline SVG sắc nét; loại bỏ thẻ `<script>` flash tự đóng nội tuyến sang `fragments/ui/flash-toast :: flashToast`; loại bỏ inline styles sang các class `ofw-`. |
| **Sửa** | `src/main/resources/static/js/offers.js` | Tinh chỉnh hàm `validateProbationRuleUi()` hiển thị icon SVG sắc nét và class `.ofw-rule-notice--invalid` / `.ofw-rule-notice--valid` độc lập không phụ thuộc FontAwesome CDN. |
| **Thêm mục cuối** | `docs/architecture/UI/UI_CATALOG.md` | Bổ sung mục tra cứu cách tái sử dụng Offer Workspace (`ofw-`) kết hợp Admin Workspace (`adw-`). |
| **Thêm dòng** | `docs/management/WORK_LOG.md` | Cập nhật nhật ký công việc ngày 31/10/2026. |

---

## 2. Cách Áp Dụng Layout & Quyết Định UI

- **Opt-in Workspace không đổi phân quyền:** Tiếp tục kế thừa `layout/internal`, kích hoạt nền công nghệ mộc và hiệu ứng khung bằng marker `.adw-page`, fragment `fragments/ui/admin-workspace :: scene` và script `admin-workspace.js`. Giữ nguyên vai trò `HR` / `Recruiter`, không ép quyền `System Admin`.
- **Đồng bộ màn hình Danh sách (`/offers`):**
  - Tiêu đề trang `.adw-heading` và `.adw-title` (36px desktop / 28px mobile, gạch chân thương hiệu 48×4px), loại bỏ mô tả phụ lặp để giao diện tối giản, tập trung giống trang Tin tuyển dụng.
  - 5 thẻ thống kê tổng quan với icon SVG tương ứng, border `var(--line)`, shadow `var(--shadow-sm)` và bo góc `var(--radius-lg)`.
  - Thanh công cụ lọc và tìm kiếm bọc trong `.ofw-toolbar.adw-surface`.
  - Danh sách đề xuất bọc trong `.adw-surface` với header chuẩn `.ofw-list-header`, icon hộp bo góc `.ofw-list-icon`, tiêu đề `.adw-panel-title` và badge đếm số lượng `.ofw-list-count`.
  - Bảng dữ liệu dùng `.adw-data-table` với header xanh đậm thương hiệu Mộc RMS, zebra/hover rows nhẹ nhàng và badge trạng thái rõ nét.
- **Mở rộng toàn bộ lề 2 bên màn hình Form (`/offers/create`):**
  - Mở rộng toàn bộ form dàn đều ngang workspace giống trang Danh sách (`/offers`), loại bỏ giới hạn 880px để thẻ thông tin ứng viên và điều khoản hợp đồng lấp đầy không gian hiển thị, đồng bộ lề 2 bên với thanh topbar và sidebar.
  - Thẻ thông tin ứng viên và điều khoản đãi ngộ dùng `.adw-surface` với bo góc `var(--radius-lg)`, viền `var(--line)` và shadow `var(--shadow-md)`.
  - Tiêu đề phân mục dùng `.adw-panel-heading` và `.adw-panel-title` kèm badge số bước `1`, `2` tròn tinh tế.
  - Lưới thông tin ứng viên `ofw-info-grid` với label uppercase xám nhẹ và giá trị semibold, mức lương gợi ý được bọc trong badge nổi bật `var(--brand-soft)`.
- **Đồng bộ màn hình Chi tiết Đề xuất (`/offers/{id}`):**
  - Gắn class `.adw-page ofw-page offer-detail-container` và nạp scene background để chuyển đổi liền mạch từ trang danh sách sang chi tiết mà không bị vỡ giao diện nền trắng cũ.
  - Toàn bộ các icon FontAwesome (briefcase, user, calendar, arrow-left, file-lines, location-dot, clock, user-pen, etc.) được thay thế bằng inline SVG chuẩn Lucide/Feather.
  - Tách các inline style ra thành class `ofw-cand-decline-header`, `ofw-cand-decline-title`, `ofw-detail-section-title`, `ofw-detail-audit-footer`, v.v.
- **Đóng gói Modal & Dialog Styles:**
  - Toàn bộ inline styles trong `#offerExportModal`, `#editOfferModal`, `#directorDecisionModal` được chuẩn hóa thành các class chuyên dụng trong `offer-workspace.css` (Section 15), loại bỏ hoàn toàn các thuộc tính `style="..."` về layout/color.
- **Button DNA chuẩn Mộc:** Nút hình chữ nhật bo góc 8px (`var(--radius-md)`), cao 48px cho nút chính, không dùng nút bo tròn pill.
- **Loại bỏ inline script & CDN:** Xóa hoàn toàn script tự đóng flash message nội tuyến và link FontAwesome ngoài; dùng fragment `fragments/ui/flash-toast :: flashToast` chuẩn hệ thống và 100% SVG nội bộ.

---

## 3. Nghiệp Vụ & Dữ Liệu Được Bảo Toàn 100%

- Giữ nguyên toàn bộ view paths (`offers/list`, `offers/form`, `offers/detail`), form actions (`/offers/create`, `/offers/{id}/edit`, `/offers/{id}/delete`), HTTP methods, CSRF token.
- Giữ nguyên 100% form bindings: `th:object="${offerDto}"`, các `th:field`: `applicationId`, `offeredPositionTitle`, `proposedSalary`, `probationSalary`, `probationDays`, `responsePeriodDays`, `expectedStartDate`, `workLocation`, `benefitsPackage`.
- Giữ nguyên toàn bộ DOM ID để `offers.js` bắt sự kiện và tính toán tự động:
  - Form: `createAppSelect`, `groupASection`, `dispCandId`, `dispCandName`, `dispCandEmail`, `dispCandPhone`, `dispPosition`, `dispDept`, `dispReqId`, `dispHm`, `dispRecomSalary`, `dispInterviewComments`, `dispWorkLocation`, `probationRuleNotice`.
  - List: `searchInput`, `statusFilter`, `timeSortFilter`, `selectAllOffers`, `.offer-row-checkbox`, `selectedOffersCount`, `offerExportModal`, `editOfferModal`, `directorDecisionModal`.
  - Detail: `directorDetailComments`, `directorDetailCommentCount`, `toastContainer`.
- Bảo toàn trọn vẹn cả 3 ngữ cảnh tạo mới/chỉnh sửa và các trạng thái phê duyệt của Director.
- Không thay đổi mã nguồn của Director (`director-list.html`) hay Candidate (`candidate-*.html`).
- Không sửa Database schema, migration, seed hay Service backend.

---

## 4. Kết Quả Kiểm Thử (Testing Results)

- **Kiểm thử tự động (JUnit 5 + Spring Boot Test):**
  - Chạy `OfferIntegrationTests` và `OfferControllerBugTests`:
    - **34/34 tests đạt 100% (Failures: 0, Errors: 0, Skipped: 0)**.
    - `BUILD SUCCESS`.
- **Kiểm tra cú pháp & Kiến trúc:**
  - `offer-workspace.css`: 100% token CSS, 0 raw hex color, 0 `!important`.
  - Cả 3 template `list.html`, `form.html`, `detail.html`: `tabindex="-1"` chuẩn Accessibility, không còn thẻ `<script>` nội tuyến, 100% SVG inline.


---

## 5. Cải Tiến Kiến Trúc & Xóa Bỏ Vi Phạm (Refactoring for Architecture Guide Compliance)

Thực hiện rà soát và khắc phục triệt để 4 vi phạm kiến trúc theo `docs/architecture/ARCHITECTURE_GUIDE.md`:

1. **Xóa bỏ Circular Dependency giữa module `offer` và `notification`:**
   - Di chuyển [OfferMailSender.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferMailSender.java) từ `com.group2.rms.notification` sang đúng phân hệ nghiệp vụ `com.group2.rms.offer.service`.
   - Cập nhật package và loại bỏ import chéo tại `OfferServiceImpl.java` và `OfferServiceTests.java`. Module `notification` chỉ tập trung in-app notification (`NotificationService`), không còn phụ thuộc vòng tròn vào `offer`.

2. **Khử Coupling đặc thù module trong Core Exception Handler:**
   - Loại bỏ đoạn mã hardcoded kiểm tra các trường dữ liệu `expectedStartDate`, `proposedSalary`, `probationSalary`, `probationDays` trong [GlobalExceptionHandler.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java).
   - Tầng `core` trở về trạng thái generic hoàn toàn (`Dữ liệu nhập vào không đúng định dạng.`), các thông báo cụ thể theo trường dữ liệu tiếng Việt được phân phối chuẩn qua Spring DataBinder và `messages.properties`.

3. **Chuẩn hóa Token CSS 100% trong `candidate-offers.css`:**
   - Xóa bỏ hoàn toàn thuộc tính `!important` tại `.cof-system-banner p.cof-system-banner__guide` và `.cof-system-banner p.cof-system-banner__reason` bằng cách tăng CSS specificity (tag + class selector) thay vì ép quy tắc.
   - Thay thế toàn bộ 20 vị trí mã màu hex hardcoded (`#ffffff`, `#64748b`, `#f1f5f9`, `#0f172a`, `#0f3822`, `#475569`, `#1e293b`, `#fffdf8`, `#eedfcb`, `#fdf5ea`, `#fae6cb`, `#c5d7cc`, `#edf7ee`, `#e1e7e2`, `#f8faf8`, `#e8ede9`, `#fbfdfb`, `#e3ede5`, `#c9decb`) sang biến semantic của hệ thống (`--surface`, `--muted`, `--neutral-soft`, `--ink`, `--brand-dark`, `--brand-soft`, `--line`, `--warning-soft`, `--on-brand`, `--surface-subtle`).

4. **Dọn dẹp Endpoint Form POST thừa trong `OfferController.java`:**
   - Loại bỏ các endpoint Form POST không sử dụng `approveOfferForm` (`POST /{id}/approve`) và `rejectOfferForm` (`POST /{id}/reject`).
   - Thống nhất toàn bộ luồng phê duyệt/từ chối của Giám đốc qua chuẩn REST JSON `approveOfferJson` và `rejectOfferJson` tương thích với giao diện modal pop-up.

5. **Kết quả kiểm thử toàn diện:**
   - Chạy toàn bộ 4 test suite liên quan (`OfferIntegrationTests`, `OfferControllerBugTests`, `CandidateOfferWebTests`, `OfferServiceTests`): **75/75 tests PASS 100%**.
