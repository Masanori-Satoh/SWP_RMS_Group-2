# BÁO CÁO CHUẨN HÓA GIAO DIỆN CỔNG OFFER ỨNG VIÊN (CANDIDATE OFFER PORTAL) THEO UI_RULES & ARCHITECTURE_GUIDE

- **Dự án:** Hệ Thống Quản Lý Tuyển Dụng — SWP_RMS_Group-2 (Mực RMS)
- **Nhánh thực hiện (Branch):** `feat/huyenpt/update-offer-candidate-ui`
- **Nhánh gốc (Base Branch):** `main`
- **Thành viên phụ trách (Assignee):** Phạm Thị Huyền (`HuyenPT`)
- **Phân hệ (Module):** Cổng ứng viên phản hồi thư mời nhận việc (`/portal/offers/**`)
- **Tài liệu tham chiếu:**
  - Quy chuẩn kiến trúc hệ thống: [`docs/architecture/ARCHITECTURE_GUIDE.md`](../../architecture/ARCHITECTURE_GUIDE.md)
  - Hướng dẫn thành phần UI: [`docs/architecture/UI/UI_CATALOG.md`](../../architecture/UI/UI_CATALOG.md)
  - Hướng dẫn di chuyển UI: [`docs/architecture/UI/UI_MIGRATION.md`](../../architecture/UI/UI_MIGRATION.md)
  - Quy tắc dọn dẹp mã UI cũ: [`docs/architecture/UI/UI_LEGACY_CLEANUP.md`](../../architecture/UI/UI_LEGACY_CLEANUP.md)
  - Quy tắc UI toàn hệ thống: [`docs/architecture/UI/UI_RULES.md`](../../architecture/UI/UI_RULES.md)
  - Nhật ký công việc dự án: [`docs/management/WORK_LOG.md`](../../management/WORK_LOG.md)
  - Nhật ký kỹ thuật chi tiết: [`docs/management/work_logs/2026-10-31-candidate-offer-ui-sync.md`](../../management/work_logs/2026-10-31-candidate-offer-ui-sync.md)

---

## 🎯 I. MỤC TIÊU & BỐI CẢNH TRIỂN KHAI

### 1. Bối cảnh
Trước đợt rà soát và chuẩn hóa này, phân hệ Cổng thông tin Offer dành cho Ứng viên (`/portal/offers/**`) còn tồn tại một số điểm chưa đồng bộ với tiêu chuẩn thiết kế chung của hệ thống:
1. **Hệ thống thông báo phân mảnh & Popup che màn hình:**
   - Khi ứng viên đồng ý nhận việc, giao diện hiển thị một modal blocking tự chế `#cof-accept-success-modal` với backdrop tối và animation riêng, thay vì sử dụng cơ chế Flash Toast chuẩn toàn hệ thống (`fragments/ui/flash-toast`).
   - Màn hình danh sách và chi tiết sử dụng các thẻ `.alert.alert-success`, `.alert.alert-danger` tĩnh, gây giật giao diện và không đồng bộ với trải nghiệm của HR/Director.
2. **Vi phạm Button DNA:**
   - Nút chấp nhận offer trong `candidate-detail.html` dùng `.btn--success` (màu xanh lá riêng biệt) thay vì `.btn.btn--primary` chuẩn mực.
   - Nút từ chối dùng `.btn--outline-danger` thay vì `.btn.btn--outline`.
   - Nút tải PDF dùng class không chuẩn `.cof-download-bar__btn` thay vì `.btn.btn--secondary.btn--sm`.
3. **Hardcoded Hex Color trong Watermark:**
   - Dấu watermark "THƯ MỜI NHẬN VIỆC CHÍNH THỨC" trong `candidate-detail.html` chứa mã hex cứng `stroke="#2d6a4f"` và `fill="#2d6a4f"` thay vì dùng biến token `var(--brand)`.
4. **Ký tự Entity thô:**
   - Màn hình từ chối `candidate-decline.html` dùng thực thể HTML thô `&larr; Quay lại` thay vì inline SVG icon có aria-hidden.
5. **Thiếu liên kết trực tiếp trong Email Offer:**
   - Khi HR phát hành offer, `OfferMailSender.java` chỉ gửi email thông báo văn bản thuần mà không có đường link trực tiếp dẫn ứng viên tới `/portal/offers/{offerId}`.

### 2. Mục tiêu cốt lõi
1. **Chuẩn hóa thông báo Flash Toast:** Đưa toàn bộ phản hồi (đồng ý nhận việc, từ chối nhận việc, thông báo hết hạn, lỗi phân quyền) về Flash Toast chuẩn đặt giữa màn hình (`fragments/ui/flash-toast`), xóa bỏ hoàn toàn modal tự chế.
2. **Tuân thủ Buttons DNA & Token CSS:** 100% nút bấm tuân thủ cấu trúc `.btn`, sử dụng `var(--brand)`, bo góc tiêu chuẩn 8px, 0 mã hex hardcoded.
3. **Làm sạch mã nguồn CSS & JS:** Xóa sạch CSS mồ côi và keyframes thừa trong `candidate-offers.css`, loại bỏ mã JS mở modal cũ trong `candidate-offers.js`.
4. **Bổ sung đường link trực tiếp trong Email:** Inject `app.base-url` và tự động gắn link chi tiết vào email Offer gửi ứng viên.
5. **Bảo toàn 100% nghiệp vụ và kiểm thử:** Đảm bảo 100% test cases của phân hệ Offer đều vượt qua.

---

## 🛠️ II. TIẾN ĐỘ THỰC HIỆN THEO TỪNG PHASE

### Phase 1: Chuẩn Hóa Flash Toast & Loại Bỏ Modal Tự Chế (Commit `936e7d4`)
- Nhúng `flash-toast.css` và `flash-toast.js` vào:
  - `candidate-list.html`
  - `candidate-detail.html`
  - `candidate-decline.html`
- Thay thế các khối `.alert-success` / `.alert-danger` bằng Thymeleaf fragment chuẩn:
  ```html
  <th:block th:replace="~{fragments/ui/flash-toast :: flashToast(${successMessage}, ${errorMessage})}"></th:block>
  ```
- Loại bỏ hoàn toàn modal tự chế `#cof-accept-success-modal` khỏi `candidate-detail.html`.
- Loại bỏ mã script lắng nghe và hiển thị modal thành công trong `candidate-offers.js`.
- **Kiểm thử:** 34/34 tests passing.

### Phase 2: Chuẩn Hóa Buttons DNA, Loại Bỏ Hex Colors & Ký Tự Thô (Commit `0c3adab`)
- Nút "Đồng ý nhận việc": chuyển từ `class="btn btn--success btn--lg"` sang `class="btn btn--primary btn--lg"`.
- Nút "Từ chối nhận việc": chuyển từ `class="btn btn--outline-danger btn--lg"` sang `class="btn btn--outline btn--lg"`.
- Nút "Tải bản PDF": chuyển từ `class="btn cof-download-bar__btn"` sang `class="btn btn--secondary btn--sm"`.
- Thay thế mã màu cứng `#2d6a4f` trong SVG watermark bằng biến token `var(--brand)`.
- Thay thế ký tự thô `&larr;` trong `candidate-decline.html` bằng inline SVG mũi tên quay lại chuẩn hóa.
- **Kiểm thử:** 34/34 tests passing.

### Phase 3: Làm Sạch CSS Mồ Côi & Tối Ưu candidate-offers.css (Commit `205c413`)
- Xóa bỏ hoàn toàn 7 selector CSS mồ côi của modal chấp nhận thành công:
  - `.cof-accept-success-modal`, `.cof-accept-success-backdrop`, `.cof-accept-success-card`, `.cof-accept-success-icon`, `.cof-accept-success-title`, `.cof-accept-success-desc`, `.cof-accept-success-btn`.
- Xóa bỏ các animation keyframes không còn dùng: `@keyframes cofFadeIn`, `@keyframes cofScaleUp`.
- Cập nhật quy tắc `@media print` sạch sẽ, sẵn sàng cho việc in ấn/xuất PDF thư mời nhận việc.
- **Kiểm thử:** 34/34 tests passing.

### Phase 4: Bổ Sung Link Offer Trong Email & Kiểm Thử Toàn Diện (Commit `02eede8`)
- Mở rộng `OfferMailSender.java`:
  - Tiêm thuộc tính `@Value("${app.base-url:${APP_BASE_URL:http://localhost:8080}}") baseUrl`.
  - Chuẩn hóa URL loại bỏ dấu gạch chéo cuối nếu có.
  - Chèn khối hướng dẫn truy cập kèm đường link trực tiếp `baseUrl + "/portal/offers/" + offer.getOfferId()` vào nội dung email gửi ứng viên.
  - Cập nhật định dạng email chuyên nghiệp, rõ ràng mức đãi ngộ và thời hạn phản hồi.
- Chạy toàn diện bộ test phân hệ Offer:
  - `OfferControllerBugTests`: 3/3 passed.
  - `OfferIntegrationTests`: 31/31 passed.
  - `OfferServiceTests`: 114/114 passed.
  - **Tổng cộng:** 148/148 passed (0 failures, 0 errors, BUILD SUCCESS).

---

## 📊 III. BẢNG TỔNG HỢP KIỂM CHỨNG THEO TIÊU CHUẨN HỆ THỐNG

| Tiêu chuẩn | Trước khi nâng cấp | Sau khi nâng cấp | Đánh giá |
| --- | --- | --- | --- |
| **Hệ thống Flash Toast** | Dùng modal tự chế `#cof-accept-success-modal` và inline alerts | Nhúng `fragments/ui/flash-toast` chuẩn toàn hệ thống | ✅ Đạt 100% |
| **Buttons DNA** | Dùng `.btn--success`, `.btn--outline-danger`, `.cof-download-bar__btn` | Chuẩn hóa `.btn.btn--primary`, `.btn.btn--outline`, `.btn.btn--secondary.btn--sm` | ✅ Đạt 100% |
| **Màu sắc & Token CSS** | Chứa mã màu cứng `#2d6a4f` trong SVG watermark | 100% dùng `var(--brand)` và biến token `tokens.css` | ✅ Đạt 100% |
| **Biểu tượng (Icons)** | Dùng ký tự HTML thô `&larr;` | 100% inline SVG có `aria-hidden="true"` | ✅ Đạt 100% |
| **Mã CSS mồ côi** | Còn tồn tại nhiều class modal và keyframes thừa | Dọn dẹp sạch sẽ 100% | ✅ Đạt 100% |
| **Trải nghiệm Email** | Email thông báo không có đường dẫn đến offer | Tự động sinh link trực tiếp `/portal/offers/{id}` | ✅ Đạt 100% |
| **Kiểm thử tự động** | Chưa chạy toàn bộ test sau sửa đổi UI | 148/148 tests pass (BUILD SUCCESS) | ✅ Đạt 100% |
