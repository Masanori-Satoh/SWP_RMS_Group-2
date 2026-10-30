# Đồng Bộ Giao Diện Danh Sách Offer Của Giám Đốc (Director List) Theo Layout Admin Workspace

- **Ngày:** 31/10/2026.
- **Người thực hiện:** HuyenPT.
- **Yêu cầu đã làm rõ:** Đồng bộ toàn diện màn hình Danh sách Đề xuất Nhận việc của Giám đốc (`src/main/resources/templates/offers/director-list.html`) theo ngôn ngữ Admin Workspace (`adw-`), tuân thủ chặt chẽ `ARCHITECTURE_GUIDE.md`, các quy ước UI (`UI_CATALOG.md`, `UI_MIGRATION.md`, `UI_LEGACY_CLEANUP.md`, `UI_RULES.md`), bảo toàn 100% nghiệp vụ phê duyệt/từ chối của Giám đốc và hợp đồng dữ liệu hiện có.
- **Phạm vi:** Frontend Thymeleaf, CSS scoped `offer-workspace.css`, tối ưu icon SVG, Controller `activeMenu`, loại bỏ inline styles, bổ sung nút phê duyệt/từ chối nhanh kèm modal ra quyết định, tài liệu và kiểm thử tích hợp.

---

## 1. Danh Sách File Thay Đổi

| Loại | File | Nội dung thay đổi |
| --- | --- | --- |
| **Sửa** | `src/main/java/com/group2/rms/offer/controller/OfferController.java` | Bổ sung `model.addAttribute("activeMenu", "offers")` cho luồng `getAllOffersForDirector` để sidebar hiển thị sáng mục chính xác khi Giám đốc đăng nhập. |
| **Mở rộng** | `src/main/resources/static/css/pages/offer-workspace.css` | Bổ sung Section 17 token utilities cho cột bảng: `.ofw-col-id`, `.ofw-col-action`, `.ofw-col-action-wide`, `.ofw-cell-link`, `.ofw-date-cell`, `.ofw-empty-cell`, `.ofw-empty-state`. Sử dụng 100% CSS variables từ `tokens.css`. |
| **Sửa** | `src/main/resources/templates/offers/director-list.html` | Kế thừa opt-in Admin workspace (`.adw-page ofw-page`, `fragments/ui/admin-workspace :: scene` và `:: scripts`); chuẩn hóa tiêu đề `.adw-heading` + `.adw-title`; chuyển 3 stat cards sang `.adw-surface.ofw-stat-card`; chuyển tabs sang `.ofw-director-tabs`; bọc toolbar trong `.ofw-toolbar.adw-surface`; bọc bảng trong `.ofw-list-surface.adw-surface`; áp dụng `.adw-data-table`; thay 100% icon FontAwesome CDN bằng inline SVG; loại bỏ 9 thuộc tính inline styles; bổ sung các nút phê duyệt/từ chối nhanh kèm modal `#directorDecisionModal` và `#toastContainer`. |
| **Thêm mục** | `docs/architecture/UI/UI_CATALOG.md` | Bổ sung mục tra cứu cập nhật 31/10/2026 cho màn hình Director Offer list. |
| **Thêm dòng** | `docs/management/WORK_LOG.md` | Cập nhật nhật ký công việc ngày 31/10/2026. |
| **Tạo mới** | `docs/members/huyenpt/summary-update-offer-director-ui.md` | Báo cáo chi tiết công việc của thành viên HuyenPT cho nhánh `feat/huyenpt/update-offer-director-ui`. |

---

## 2. Các Phase Thực Hiện Và Kết Quả Kiểm Thử

### Phase 1: Nền tảng Admin Workspace & Header Chuẩn (Commit `aa16b04`)
- **Stylesheet:** Chuyển từ file css cũ sang `offer-workspace.css`. Loại bỏ link FontAwesome CDN (`cdnjs.cloudflare.com`).
- **Layout Opt-in:** Bổ sung class `.adw-page ofw-page` vào thẻ `<main>`. Nhúng `fragments/ui/admin-workspace :: scene` và `fragments/ui/admin-workspace :: scripts`.
- **Heading:** Chuẩn hóa tiêu đề chính thành `.adw-heading ofw-heading` với `.adw-title ofw-title` (gạch chân thương hiệu 48×4px). Loại bỏ dòng mô tả phụ rườm rà `.offers-subtitle`.
- **Sidebar Menu:** Thêm `model.addAttribute("activeMenu", "offers")` trong `OfferController.java` cho role Director.
- **Kiểm thử:** 34/34 test cases passed (0 failure, 0 error).

### Phase 2: Thẻ Thống Kê & Tab Bộ Lọc Trạng Thái (Commit `a40ce93`)
- **Stat Cards:** Tái cấu trúc 3 thẻ thống kê ngang sang `.adw-surface.ofw-stat-card` bên trong grid `.ofw-director-stats-grid`. Thay thế toàn bộ icon FontAwesome bằng inline SVG (`stat-icon-amber`, `stat-icon-green`, `stat-icon-danger`).
- **Director Tabs:** Chuyển đổi container tabs sang `.director-tabs.ofw-director-tabs`. Mỗi tab sử dụng class `.ofw-director-tab` kèm inline SVG icon tương ứng (Đồng hồ cát cho Chờ duyệt, Tick tròn cho Đã duyệt, Danh sách cho Tất cả).
- **Kiểm thử:** 34/34 test cases passed (0 failure, 0 error).

### Phase 3: Bảng Dữ Liệu Hiện Đại & Toolbar Lọc (Commit `65be0af`)
- **CSS Section 17 Token Utilities:** Bổ sung các class kích thước cột và căn chỉnh văn bản vào `offer-workspace.css` (`.ofw-col-id`, `.ofw-col-action`, `.ofw-col-action-wide`, `.ofw-cell-link`, `.ofw-date-cell`, `.ofw-empty-cell`, `.ofw-empty-state`).
- **Toolbar Surface:** Bọc toolbar bộ lọc trong `.card.offers-toolbar.ofw-toolbar.adw-surface`. Thay thế icon tìm kiếm và đặt lại bộ lọc bằng inline SVG.
- **Table Card Surface:** Bọc toàn bộ bảng trong thẻ `.card.ofw-list-surface.adw-surface` với header chuẩn `.ofw-list-header`, icon hộp `.ofw-list-icon`, tiêu đề bảng `.adw-panel-title` và badge đếm số lượng `.ofw-list-count`.
- **Bảng dữ liệu:** Áp dụng class chuẩn `.adw-data-table ofw-data-table` với thương hiệu Mực RMS, hỗ trợ hover và bo góc.
- **Loại bỏ Inline Styles:** Xóa bỏ hoàn toàn 9 vị trí `style="..."` trong `director-list.html` và chuyển sang các class Section 17.
- **Inline SVG Icons:** Thay thế toàn bộ 7 badge trạng thái (`Draft`, `Pending_Director`, `Director_Approved`, `Director_Rejected`, `Sent_Candidate`, `Accepted`, `Declined`) và trạng thái rỗng bằng inline SVG nội bộ.
- **Kiểm thử:** 34/34 test cases passed (0 failure, 0 error).

### Phase 4: Thao Tác Nhanh & Modal Ra Quyết Định Của Giám Đốc (Commit `b0aaae2`)
- **Nút hành động nhanh:** Bổ sung 2 nút Phê duyệt nhanh (`btn-icon-success ofw-action-btn--success`) và Từ chối nhanh (`btn-icon-danger ofw-action-btn--danger`) cho các offer ở trạng thái `Pending_Director` ngay trên cột Hành động của bảng.
- **Mở rộng cột hành động:** Thêm class `.ofw-col-action-wide` (min-width: 140px) để các nút thao tác hiển thị thoải mái, không bị gãy dòng.
- **Modal Quyết định:** Nhúng modal `#directorDecisionModal` vào cuối trang kèm form nhập ghi chú/lý do từ chối, kết nối trực tiếp với các hàm `openDirectorDecisionModal()`, `closeDirectorDecisionModal()`, và `submitDirectorDecision()` đã có sẵn trong `offers.js`.
- **Toast Container:** Bổ sung container `#toastContainer` cho các thông báo phản hồi thao tác.
- **Kiểm thử:** 34/34 test cases passed (0 failure, 0 error).

---

## 3. Xác Minh Chất Lượng & Tuân Thủ

- **Kiến trúc hệ thống:** 100% tuân thủ `ARCHITECTURE_GUIDE.md` (giữ nguyên controller endpoints, request body, DTOs, CSRF protection, phân quyền role-based).
- **Ngôn ngữ thiết kế:** 100% tuân thủ Admin Workspace DNA (`.adw-page`, `.adw-surface`, `.adw-data-table`, `fragments/ui/admin-workspace`).
- **Bộ icon:** 100% inline SVG nội bộ, 0 link FontAwesome CDN bên ngoài.
- **Kiểu dáng CSS:** 100% semantic CSS variables từ `tokens.css`, 0 inline `style="..."`, 0 mã màu hex hardcoded, 0 `!important`.
- **Bộ kiểm thử tự động:**
  - `OfferControllerBugTests`: 3/3 passed.
  - `OfferIntegrationTests`: 31/31 passed.
  - Tổng số: 34/34 passed, 0 failures, 0 errors, build thành công (`BUILD SUCCESS`).
