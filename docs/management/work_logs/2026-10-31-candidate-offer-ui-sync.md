# Đồng Bộ Giao Diện Cổng Đề Xuất Nhận Việc Của Ứng Viên (Candidate Offer Portal) Theo Chuẩn UI_RULES & ARCHITECTURE_GUIDE

- **Ngày:** 31/10/2026.
- **Người thực hiện:** HuyenPT.
- **Yêu cầu đã làm rõ:** Rà soát và chuẩn hóa toàn bộ luồng Offer của Ứng viên (từ Thư mời nhận việc, Danh sách offer, Chi tiết offer, tới Màn hình từ chối) theo `docs/architecture/UI/UI_RULES.md` và `docs/architecture/ARCHITECTURE_GUIDE.md`. Loại bỏ popup tự chế, chuẩn hóa hệ thống thông báo Flash Toast, tuân thủ Buttons DNA, loại bỏ hoàn toàn mã màu hex hardcoded và ký tự entity thô, bổ sung đường link trực tiếp truy cập offer trong email gửi ứng viên.
- **Phạm vi:** 
  - Templates Thymeleaf: `candidate-list.html`, `candidate-detail.html`, `candidate-decline.html`.
  - Static CSS/JS: `candidate-offers.css`, `candidate-offers.js`.
  - Backend Service: `OfferMailSender.java`.
  - Kiểm thử: `OfferControllerBugTests`, `OfferIntegrationTests`, `OfferServiceTests` (148/148 passed).

---

## 1. Danh Sách File Thay Đổi

| Loại | File | Nội dung thay đổi |
| --- | --- | --- |
| **Sửa** | `src/main/resources/templates/offers/candidate-list.html` | Nhúng `flash-toast.css` & `flash-toast.js`; tích hợp fragment `flash-toast :: flashToast`; loại bỏ inline alerts `.alert-success` / `.alert-danger` phân mảnh. |
| **Sửa** | `src/main/resources/templates/offers/candidate-detail.html` | Nhúng Flash Toast fragment; loại bỏ hoàn toàn popup blocking tự chế `#cof-accept-success-modal` và inline alerts; chuẩn hóa Buttons DNA (`.btn.btn--primary`, `.btn.btn--outline`, `.btn.btn--secondary.btn--sm`); thay hex `#2d6a4f` trong SVG watermark bằng `var(--brand)`. |
| **Sửa** | `src/main/resources/templates/offers/candidate-decline.html` | Nhúng Flash Toast fragment; thay thế HTML entity thô `&larr;` bằng inline SVG arrow chuẩn hóa. |
| **Tối ưu** | `src/main/resources/static/css/pages/candidate-offers.css` | Loại bỏ toàn bộ CSS mồ côi và animation keyframes của modal cũ (`.cof-accept-success-*`, `@keyframes cofFadeIn`, `@keyframes cofScaleUp`); làm sạch các quy tắc `@media print`. |
| **Tối ưu** | `src/main/resources/static/js/pages/candidate-offers.js` | Dọn dẹp hoàn toàn đoạn script lắng nghe và hiển thị `#cof-accept-success-modal`, đồng bộ với luồng Flash Toast toàn hệ thống. |
| **Mở rộng** | `src/main/java/com/group2/rms/offer/service/OfferMailSender.java` | Inject configurable `@Value("${app.base-url:${APP_BASE_URL:http://localhost:8080}}") baseUrl`; chèn trực tiếp đường dẫn xem chi tiết Thư mời nhận việc và phản hồi (`baseUrl + "/portal/offers/" + offer.getOfferId()`) vào nội dung email. |
| **Thêm dòng** | `docs/management/WORK_LOG.md` | Ghi nhận thay đổi vào lịch sử dự án. |
| **Tạo mới** | `docs/members/huyenpt/summary-update-offer-candidate-ui.md` | Báo cáo chi tiết công việc của thành viên HuyenPT cho nhánh `feat/huyenpt/update-offer-candidate-ui`. |

---

## 2. Các Phase Thực Hiện Và Kết Quả Kiểm Thử

### Phase 1: Chuẩn Hóa Flash Toast & Loại Bỏ Modal Tự Chế (Commit `936e7d4`)
- Nhúng `flash-toast.css` và `flash-toast.js` vào cả 3 template: `candidate-list.html`, `candidate-detail.html`, `candidate-decline.html`.
- Nhúng Thymeleaf fragment `fragments/ui/flash-toast :: flashToast(${successMessage}, ${errorMessage})`.
- Xóa bỏ hoàn toàn modal tự chế che kín màn hình `#cof-accept-success-modal` và các alert tĩnh `.alert-success`, `.alert-danger`.
- Làm sạch code xử lý modal trong `candidate-offers.js`.
- **Kiểm thử:** 34/34 test cases passed (BUILD SUCCESS).

### Phase 2: Chuẩn Hóa Buttons DNA, Loại Bỏ Hex Colors & Ký Tự Thô (Commit `0c3adab`)
- Chuẩn hóa các nút thao tác trong `candidate-detail.html` sang Buttons DNA (`.btn.btn--primary`, `.btn.btn--outline`, `.btn.btn--secondary.btn--sm`).
- Thay thế mã màu cứng `#2d6a4f` trong SVG watermark con dấu thành biến token `var(--brand)`.
- Thay thế ký tự HTML thô `&larr;` trong `candidate-decline.html` bằng inline SVG arrow chuẩn hóa.
- **Kiểm thử:** 34/34 test cases passed (BUILD SUCCESS).

### Phase 3: Làm Sạch CSS Mồ Côi & Tối Ưu candidate-offers.css (Commit `205c413`)
- Xóa bỏ hoàn toàn các class `.cof-accept-success-modal`, `.cof-accept-success-backdrop`, `.cof-accept-success-card`, `.cof-accept-success-icon`, `.cof-accept-success-title`, `.cof-accept-success-desc`, `.cof-accept-success-btn`.
- Xóa bỏ các animation `@keyframes cofFadeIn`, `@keyframes cofScaleUp`.
- Cập nhật quy tắc `@media print`.
- **Kiểm thử:** 34/34 test cases passed (BUILD SUCCESS).

### Phase 4: Bổ Sung Link Offer Trong Email & Kiểm Thử Toàn Diện (Commit `02eede8`)
- Mở rộng `OfferMailSender.java` để chèn URL truy cập trực tiếp cổng ứng viên (`/portal/offers/{id}`) giúp ứng viên nhận email có thể nhấp trực tiếp vào xem và phản hồi offer.
- Chạy toàn bộ bộ test phân hệ Offer (`OfferControllerBugTests`, `OfferIntegrationTests`, `OfferServiceTests`).
- **Kết quả:** 148/148 test cases passed (0 failures, 0 errors, BUILD SUCCESS).
