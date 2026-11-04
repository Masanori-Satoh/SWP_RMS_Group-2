# LỘ TRÌNH THỰC THI CHUYỂN ĐỔI KIẾN TRÚC FRONTEND OFFER (TASK 10)
## SWP391 — Mộc RMS Group 2 | Developer: HuyenPT (Offer Management)
*Mã tài liệu: 19-task-10-migration-roadmap.md*
*Thời gian thực hiện: 04/11/2026 | Nhánh Git: `refactor/huyenpt/offer-ui-js-to-html-css`*

---

## 1. NGUYÊN TẮC THI CÔNG KIỂM SOÁT RỦI RO (SAFETY GATES)

Để đảm bảo quá trình tái cấu trúc không gây bất kỳ gián đoạn nào tới các phân hệ khác (Ứng viên, Phỏng vấn, Thông báo) và bảo toàn tính toàn vẹn 100% của cơ sở dữ liệu dùng chung, toàn bộ lộ trình được chia thành **10 đợt triển khai nhỏ (Batches)** độc lập và có thể rollback tức thì.

Mỗi đợt triển khai phải vượt qua 3 cổng kiểm soát bắt buộc trước khi chuyển sang đợt tiếp theo:
1. **Cổng 1 (Syntax Check):** Trình biên dịch JavaScript Node.js và không có lỗi Console trên trình duyệt.
2. **Cổng 2 (Backend Regression):** Chạy toàn bộ 7 test suite Offer với **223/223 tests PASS**.
3. **Cổng 3 (Browser Interactive Check):** Chạy bộ kiểm thử tự động Chrome CDP tương ứng với màn hình vừa sửa đổi.

---

## 2. BẢNG TỔNG QUAN 10 ĐỢT TRIỂN KHAI (MIGRATION BATCHES OVERVIEW)

| Đợt (Batch) | Nội dung nghiệp vụ | Tệp nguồn | Tệp đích | Dòng di chuyển | Dòng xóa bỏ | Cổng kiểm thử chính |
| :---: | :--- | :--- | :--- | :---: | :---: | :--- |
| **Batch 01** | Trích xuất Shared Helpers & Send Offer Dialog | `offers.js` | `offer-common.js` | ~110 | 0 | Test send offer dialog, CSRF check |
| **Batch 02** | Trích xuất Create/Edit Form Logic & Xóa Dead Create Modal | `offers.js` | `offer-form.js` | ~155 | 159 (dead create modal) | 85% salary calculation, start date min |
| **Batch 03** | Trích xuất Excel Export Controller | `offers.js` | `offer-export.js` | ~210 | 8 (dead toast) | 25-col checkboxes, Blob file download |
| **Batch 04** | Trích xuất HR List Filters & Table Row Selection | `offers.js` | `offer-list.js` | ~175 | 4 (dead filter search) | Search enter, tab filter, select all |
| **Batch 05** | Trích xuất Director Decision Modal & Director List Filters | `offers.js` | `offer-director.js` | ~185 | 10 (dead submit wrappers) | Director quick modal, counter, reject req |
| **Batch 06** | Trích xuất Offer Detail Decision Logic | `offers.js` | `offer-detail.js` | ~160 | 39 (dead swal modal) | Detail scroll, HTML5 dialog approve/reject |
| **Batch 07** | Dọn dẹp dứt điểm mã chết còn lại trong `offers.js` | `offers.js` | *(Elimination)* | 0 | 182 (dead formatters, dead flash) | Tệp `offers.js` giảm về 0 hoặc chuyển thành bridge |
| **Batch 08** | Chuẩn hóa Candidate Portal JavaScript | `candidate-offers.js` | `candidate-offers.js` | 285 | 0 | Reason chips, countdown ticker, double-submit |
| **Batch 09** | Tách cấu trúc CSS theo trang & dọn dẹp selector chết | `offers.css`, `workspace.css` | `pages/offers/*.css` | ~1.400 | ~350 (dead drawer/modal css) | Visual layout parity, responsive checks |
| **Batch 10** | Tích hợp toàn diện & Kiểm thử hồi quy chéo module | Toàn bộ 6 templates | Templates updated | — | — | 223 Maven tests + 10 Chrome CDP tests PASS |

---

## 3. THIẾT KẾ CHI TIẾT TỪNG ĐỢT TRIỂN KHAI

---

### BATCH 01 — TRÍCH XUẤT TỆP TIỆN ÍCH DÙNG CHUNG (`offer-common.js`)
- **Tệp nguồn:** `src/main/resources/static/js/offers.js`
- **Tệp đích mới:** `src/main/resources/static/js/pages/offers/offer-common.js`
- **Các hàm chuyển dịch:**
  - `getCsrfInfo()`
  - `escapeOfferHtml()`
  - `showFlashToast()`
  - `confirmSendOffer(offerId)`
  - `executeSendOfferAction()`
- **Hàm xóa bỏ:** 0.
- **Cập nhật Template:** Thêm `<script th:src="@{/js/pages/offers/offer-common.js}" defer></script>` vào `offers/list.html` và `offers/detail.html`.
- **Thứ tự nạp:** Nạp ngay sau `flash-toast.js` và trước các script nghiệp vụ khác.
- **Kế hoạch Rollback:** Xóa tệp `offer-common.js`, hoàn nguyên `<script>` trong 2 template bằng lệnh `git checkout`.

---

### BATCH 02 — TRÍCH XUẤT FORM LOGIC & XÓA BỎ DEAD CREATE MODAL (`offer-form.js`)
- **Tệp nguồn:** `src/main/resources/static/js/offers.js`
- **Tệp đích mới:** `src/main/resources/static/js/pages/offers/offer-form.js`
- **Các hàm chuyển dịch:**
  - `handleSelectPassedCandidate(appId)`
  - `autoCalculateProbationSalary(forceSet)`
  - `validateProbationRuleUi()`
  - `updateMinExpectedStartDate()`
  - `initOfferFormExpectedStartDateMin()`
  - Chuyển toàn bộ khối `DOMContentLoaded` form-specific từ `offers.js` (dòng 674-689) sang `offer-form.js`.
- **Hàm xóa bỏ (Mã chết):**
  - `openCreateOfferModal` (27 dòng)
  - `submitCreateOffer` (132 dòng)
- **Giảm thiểu JavaScript:** **159 dòng mã chết được loại bỏ**.
- **Cập nhật Template:**
  - `offers/form.html`: Thay `<script th:src="@{/js/offers.js}" defer>` bằng:
    ```html
    <script th:src="@{/js/pages/offers/offer-common.js}" defer></script>
    <script th:src="@{/js/pages/offers/offer-form.js}" defer></script>
    ```
- **Kiểm thử nghiệm thu:**
  - Kiểm tra chọn ứng viên dropdown -> Group A tự động hiển thị dữ liệu.
  - Nhập lương chính thức 10.000.000 -> lương thử việc tự nhảy 8.500.000 (85%).
  - Nhập lương thử việc 8.000.000 -> hiện cảnh báo vi phạm Luật Lao Động.
  - Kiểm tra `expectedStartDate.min` tự động tính bằng Ngày mai + thời hạn phản hồi.

---

### BATCH 03 — TRÍCH XUẤT ĐIỀU KHIỂN XUẤT EXCEL (`offer-export.js`)
- **Tệp nguồn:** `src/main/resources/static/js/offers.js`
- **Tệp đích mới:** `src/main/resources/static/js/pages/offers/offer-export.js`
- **Các hàm chuyển dịch:**
  - `openExportModal()`, `openExportModalWithScope(scope)`, `closeExportModal()`, `handleExportModalBackdropClick(e)`
  - `handleScopeChange()`, `selectAllColumns()`, `deselectAllColumns()`, `resetDefaultColumns()`
  - `executeOfferExport()`
- **Hàm xóa bỏ:**
  - `showOfferToast`: gộp trực tiếp vào `showFlashToast` của `offer-common.js` (giảm 8 dòng).
- **Cập nhật Template:** Thêm `<script th:src="@{/js/pages/offers/offer-export.js}" defer></script>` vào `offers/list.html`.
- **Kiểm thử nghiệm thu:**
  - Mở modal xuất Excel từ nút đầu trang -> Mặc định chọn 10 cột.
  - Bấm "Chọn tất cả" -> 25 checkbox được tick.
  - Bấm "Mặc định" -> khôi phục lại đúng 10 cột.
  - Bấm "Tải xuống" -> kích hoạt tải Blob Excel không bị lỗi script.

---

### BATCH 04 — TRÍCH XUẤT BỘ LỌC VÀ CHỌN BẢNG DANH SÁCH HR (`offer-list.js`)
- **Tệp nguồn:** `src/main/resources/static/js/offers.js`
- **Tệp đích mới:** `src/main/resources/static/js/pages/offers/offer-list.js`
- **Các hàm chuyển dịch:**
  - `applyOfferFilters()`, `filterOffersByStatus(status)`, `clearSearchInput()`
  - `getSelectedOfferIds()`, `toggleSelectAllOffers(isChecked)`, `handleOfferRowSelectionChange()`, `deselectAllOffers()`, `updateSelectionToolbar()`
  - `confirmDeleteOffer(offerId)`
- **Hàm xóa bỏ:**
  - `handleFilterSearch`: wrapper thừa (giảm 4 dòng).
- **Cập nhật Template:** Thêm `<script th:src="@{/js/pages/offers/offer-list.js}" defer></script>` vào `offers/list.html`.
- **Kiểm thử nghiệm thu:**
  - Nhập tìm kiếm và Enter -> URL thay đổi kèm tham số `search=...&page=0`.
  - Chọn checkbox dòng -> thanh công cụ hàng loạt hiện ra, nút Xuất Excel hàng loạt cập nhật số lượng `(N)`.

---

### BATCH 05 — TRÍCH XUẤT LOGIC GIÁM ĐỐC (`offer-director.js`)
- **Tệp nguồn:** `src/main/resources/static/js/offers.js`
- **Tệp đích mới:** `src/main/resources/static/js/pages/offers/offer-director.js`
- **Các hàm chuyển dịch:**
  - `applyDirectorFilters()`, `filterDirectorOffersByStatus(status)`, `clearDirectorSearch()`
  - `openDirectorDecisionModal(offerId, actionType)`, `handleDirectorDecisionCommentInput(textarea)`
  - `closeDirectorDecisionModal()`, `submitDirectorDecision()`
- **Hàm xóa bỏ:**
  - `submitDirectorApprove`, `submitDirectorReject`: wrappers thừa không dùng (giảm 10 dòng).
- **Cập nhật Template:**
  - Thêm `<script th:src="@{/js/pages/offers/offer-director.js}" defer></script>` vào `offers/director-list.html` và `offers/list.html`.
- **Kiểm thử nghiệm thu:**
  - Bấm nút Duyệt trên danh sách Giám đốc -> modal mở ra với tiêu đề xanh, không bắt buộc ghi chú.
  - Bấm nút Từ chối -> modal đổi tiêu đề đỏ, bắt buộc nhập lý do (viền đỏ nếu để trống).

---

### BATCH 06 — TRÍCH XUẤT CHI TIẾT OFFER & DUYỆT TRÊN MÀN HÌNH (`offer-detail.js`)
- **Tệp nguồn:** `src/main/resources/static/js/offers.js`
- **Tệp đích mới:** `src/main/resources/static/js/pages/offers/offer-detail.js`
- **Các hàm chuyển dịch:**
  - `updateDirectorDetailCommentCounter(el)`
  - `scrollToDirectorDecision()`
  - `submitDirectorDecisionFromScreen(offerId, action)`
  - `showDirectorRejectReasonRequiredAlert()`, `showDirectorRejectConfirmModal()`
  - `confirmDirectorApproveAction()`, `confirmDirectorRejectAction()`
  - `executeDirectorDecisionApi(offerId, url, comments)`
- **Hàm xóa bỏ:**
  - `showDirectorCustomModal`: SweetAlert wrapper không ai gọi (giảm 39 dòng).
- **Cập nhật Template:**
  - `offers/detail.html`: Thay thế `<script th:src="@{/js/offers.js}" defer>` bằng:
    ```html
    <script th:src="@{/js/pages/offers/offer-common.js}" defer></script>
    <script th:src="@{/js/pages/offers/offer-detail.js}" defer></script>
    ```
- **Kiểm thử nghiệm thu:**
  - Gõ vào `#directorDetailComments` -> bộ đếm ký tự cập nhật chính xác (hỗ trợ nhập hơn 500 ký tự an toàn theo DTO backend).
  - Bấm nút Duyệt / Từ chối -> hộp thoại HTML5 `<dialog>` xác nhận mở đúng chuẩn.

---

### BATCH 07 — DỌN DẸP TOÀN BỘ MÃ CHẾT CÒN LẠI TRONG `offers.js`
- **Nội dung:** Loại bỏ toàn bộ các hàm mồ côi (không ai gọi từ template hay từ JS khác):
  - `openModal`, `closeModal`, `handleCreateModalBackdropClick`, `handleDetailModalBackdropClick` (34 dòng).
  - `getTodayDateString`, `getTomorrowDateString` (24 dòng).
  - `formatOfferDate`, `formatOfferDateTime`, `getStatusBadgeHtml` (65 dòng).
  - `initOfferFlashAutoDismiss`, `displayOfferFlashBanner`, `checkPendingOfferFlash` (57 dòng).
  - Khối gán `window.*` trùng lặp (10 dòng).
- **Kết quả:** Đã dọn sạch thêm **190 dòng mã chết**. Toàn bộ 1.561 dòng ban đầu của `offers.js` đã được tinh gọn và di chuyển trọn vẹn sang các tệp module mới.

---

### BATCH 08 — CHUẨN HÓA JAVASCRIPT PHÂN HỆ ỨNG VIÊN (`candidate-offers.js`)
- **Tệp nguồn:** `src/main/resources/static/js/pages/candidate-offers.js` (285 dòng).
- **Nội dung:** Giữ nguyên mô hình IIFE đóng gói an toàn; tối ưu hóa tài nguyên nạp trang, kiểm tra tương thích ngược giữa trang từ chối riêng biệt (`/portal/offers/{id}/decline`) và modal xác nhận.
- **Kiểm thử nghiệm thu:**
  - Chip lý do từ chối đồng bộ 2 chiều với textarea.
  - Đồng hồ đếm ngược nhảy từng giây chính xác.
  - Chặn bấm gửi 2 lần khi đồng ý/từ chối.

---

### BATCH 09 — PHÂN TÁCH VÀ DỌN DẸP KIẾN TRÚC CSS
- **Tệp nguồn:** `offers.css` (2.348 dòng) và `offer-workspace.css` (1.223 dòng).
- **Tệp đích mới:**
  - `src/main/resources/static/css/pages/offers/offer-base.css` (~220 dòng)
  - `src/main/resources/static/css/pages/offers/offer-list.css` (~280 dòng)
  - `src/main/resources/static/css/pages/offers/offer-export.css` (~150 dòng)
  - `src/main/resources/static/css/pages/offers/offer-form.css` (~250 dòng)
  - `src/main/resources/static/css/pages/offers/offer-detail.css` (~280 dòng)
  - `src/main/resources/static/css/pages/offers/offer-director.css` (~220 dòng)
- **Mã CSS bị loại bỏ:**
  - Xóa bỏ các selector của drawer cũ `#directorDetailModal` (~160 dòng).
  - Xóa bỏ các selector modal cũ `#createOfferModal` và `#detailOfferModal` (~150 dòng).
  - Xóa bỏ selector `#pageFlashContainer` (~40 dòng).
- **Cập nhật Template:** Cập nhật các thẻ `<link rel="stylesheet">` trên 4 template nội bộ theo ma trận ánh xạ tại Tài liệu 18.

---

### BATCH 10 — TÍCH HỢP TOÀN DIỆN VÀ KIỂM THỬ HỒI QUY TOÀN HỆ THỐNG
- **Nội dung:**
  - Kiểm tra đường dẫn HTTP 200 cho toàn bộ các asset mới.
  - Chạy toàn bộ 223 automated tests của phân hệ Offer.
  - Chạy toàn bộ 10 kịch bản tương tác thực tế bằng Chrome DevTools Protocol.
  - Kiểm tra chéo phân hệ: Đảm bảo không ảnh hưởng đến Candidate Application (DungLT), Interview Management (DucNM), và Notifications (HoangNH).
  - Lập báo cáo nghiệm thu tổng thể.

---

## 4. PHẠM VI CHI TIẾT CỦA ĐỢT ĐẦU TIÊN (PRECISE SCOPE OF BATCH 01)

Khi được phê duyệt bắt đầu thi công, **Batch 01** sẽ được thực hiện với phạm vi cô lập tuyệt đối như sau:

1. **Điều kiện tiên quyết (Pre-conditions):**
   - Đang ở nhánh Git `refactor/huyenpt/offer-ui-js-to-html-css`.
   - Working tree sạch hoàn toàn đối với các tệp theo dõi (`git status` clean).
   - Ứng dụng Spring Boot đang chạy ổn định tại cổng 8080.
2. **Hành động cụ thể:**
   - Tạo thư mục `src/main/resources/static/js/pages/offers/`.
   - Tạo tệp `offer-common.js` chứa đúng 5 hàm dùng chung: `getCsrfInfo`, `escapeOfferHtml`, `showFlashToast`, `confirmSendOffer`, `executeSendOfferAction`.
   - Cập nhật thẻ `<script defer>` trên `offers/list.html` và `offers/detail.html`.
3. **Cổng kiểm tra (Test Gates):**
   - Mở trang `http://localhost:8080/offers` bằng Chrome CDP: Kiểm tra modal gửi thư mời (`#sendOfferConfirmDialog`) mở/đóng bình thường.
   - Chạy 223 Maven tests: Phải đạt **223/223 PASS**.
4. **Chiến lược Rollback khẩn cấp nếu gặp sự cố:**
   - Chạy: `git checkout -- src/main/resources/templates/offers/`
   - Xóa tệp vừa tạo: `Remove-Item src/main/resources/static/js/pages/offers/offer-common.js -Force`
   - Hệ thống lập tức trở về trạng thái ổn định 100% của commit `10031e68`.
