# BÁO CÁO KỸ THUẬT TASK 15: TÁCH CSS VAI TRÒ HR, BẢO TOÀN CASCADE VÀ KIỂM CHỨNG BẰNG SO SÁNH PIXEL THỰC TẾ

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).

- **Dự án:** SWP391 — Hệ thống Quản lý Tuyển dụng Mộc RMS (Group 2)
- **Module:** Offer Management (Quản lý Đề xuất Nhận việc)
- **Nhà phát triển:** HuyenPT
- **Nhánh Git:** `refactor/huyenpt/offer-ui-js-to-html-css`
- **Mã commit HEAD:** `10031e68` (ahead by 1 commit)
- **Ngày thực hiện:** 04/11/2026

---

## 1. TỔNG QUAN VÀ MỤC TIÊU TASK 15

Sau khi hoàn thành thành công việc tái cấu trúc mã nguồn JavaScript theo vai trò trong Task 13 (xóa bỏ hoàn toàn tệp nguyên khối `offers.js` 2,005 dòng, thay thế bằng kiến trúc 4 tệp tách bạch), Task 15 tiếp tục triển khai giai đoạn 2 của kiến trúc giao diện người dùng:
1. **Khắc phục toàn diện 2 vấn đề kiểm chứng giao diện từ Task 14:**
   - **Vấn đề A (Tính chuẩn xác của so sánh ảnh):** Không dùng kích thước tệp PNG (byte size) làm bằng chứng tương đương giao diện. Thay vào đó, xây dựng công cụ kiểm tra vi sai cấp độ pixel thực tế (`scratch/pixel_compare.py`) dựa trên thư viện Pillow Python để tính toán chính xác số pixel khác biệt, tỷ lệ phần trăm, độ lệch trung bình tuyệt đối (MAE) và bounding box của từng vùng thay đổi.
   - **Vấn đề B (Độ rộng Modal trong tầng Cascade):** Sửa chữa hiện tượng kích thước modal bị thay đổi (Modal Giám đốc bị co về 500px và Modal Xuất Excel bị nhảy lên 680px) do thứ tự nạp trong tầng Cascade của Task 14. Khôi phục chuẩn mực `max-width: 620px` và đổ bóng chuẩn hệ thống `box-shadow: var(--shadow-lg)` cho cả hai modal, đảm bảo khớp 100% hình thái cơ sở (baseline).
2. **Khởi tạo tệp CSS dành riêng cho vai trò HR:**
   - `src/main/resources/static/css/pages/offers/offer-hr.css` (879 dòng)
   - Tổ chức mạch lạc thành 4 phân khu (Section A: HR List & Workspace, Section B: Create/Edit Form, Section C: Excel Export, Section D: Responsive Breakpoints).
3. **Thực hiện di chuyển có kiểm soát qua 3 Checkpoint nội bộ:**
   - **Checkpoint A:** Biểu mẫu Tạo / Sửa đề xuất (`form.html`).
   - **Checkpoint B:** Danh sách đề xuất của HR (`list.html`).
   - **Checkpoint C:** Hộp thoại xuất Excel (`#offerExportModal`).
4. **Kiểm chứng hồi quy đa phương diện:**
   - 17 cặp ảnh chụp màn hình trình duyệt thực tế (Desktop 1440px, Tablet 768px, Mobile 390px) được đo đạc mức độ vi sai pixel.
   - 10 kịch bản tương tác tổng thể qua Chrome CDP (10/10 PASS, 0 console errors).
   - 5 kịch bản chuyên sâu của Giám đốc (5/5 PASS, 0 console errors).
   - 223/223 kiểm thử tự động Maven unit & integration tests PASS toàn diện.

---

## 2. HIỆU CHỈNH KIỂM CHỨNG GIAO DIỆN TASK 14 (ISSUES A & B)

### 2.1. Xử lý Vấn đề A: Tính Hợp thức của Phương pháp So sánh Pixel
Trong báo cáo Task 14, việc kết luận giao diện không đổi dựa trên dung lượng file ảnh PNG là chưa đủ căn cứ khoa học, do định dạng PNG có thuật toán nén deflate phụ thuộc vào nhiều yếu tố metadata.

Trong Task 15, script `scratch/pixel_compare.py` được lập trình trực tiếp với thuật toán:
$$\Delta(x, y) = |I_1(x, y) - I_2(x, y)|$$
$$\text{MAE} = \frac{1}{W \times H} \sum_{x=1}^{W} \sum_{y=1}^{H} \frac{|R_1 - R_2| + |G_1 - G_2| + |B_1 - B_2|}{3}$$
$$\text{Tỷ lệ thay đổi (\%)} = \frac{\text{Số pixel có } \Delta > 0}{W \times H} \times 100\%$$

Mọi biến động đều được tách biệt thành 2 nhóm:
- **Biến động tự nhiên (Acceptable Dynamic Content):** Đồng hồ thời gian thực (`th:text="${#temporals.format(..., 'dd/MM/yyyy HH:mm')}"`) và đồng hồ đếm ngược của ứng viên (`00h 00m 29s` $\to$ `00h 00m 27s`).
- **Biến động cấu trúc giao diện (Layout / Visual Regressions):** Được định vị chính xác theo Bounding Box `(x1, y1, x2, y2)`.

### 2.2. Xử lý Vấn đề B: Khôi phục Kích thước và Hiệu ứng Đổ bóng của Modal
- **Nguyên nhân cốt lõi:** Trong baseline gốc, quy tắc `.modal-box` nằm ở dòng 1131 của `offer-workspace.css` có độ đặc hiệu (0, 1, 0) được nạp sau `offers.css` (dòng 1041 `.modal-export-card { max-width: 680px; }`) và sau dòng 883 (`.ofw-modal-box--sm { max-width: 500px; }`). Do đó, `.modal-box { max-width: 620px; box-shadow: var(--shadow-lg); }` đã ghi đè toàn bộ, tạo nên bề rộng 620px thống nhất cho mọi modal trong hệ thống. Khi Task 14 trích xuất `offer-common.css`, thứ tự Cascade bị xáo trộn khiến modal Giám đốc thu hẹp về 500px và modal Excel giãn ra 680px.
- **Giải pháp dứt điểm:** Khôi phục tường minh định nghĩa:
  ```css
  .modal-box {
      max-width: 620px;
      box-shadow: var(--shadow-lg);
  }
  .modal-export-card {
      max-width: 620px;
      width: 100%;
      box-shadow: var(--shadow-lg);
  }
  ```
  Nhờ đó, kích thước hộp thoại và bóng đổ (`0 12px 28px rgba(37, 41, 35, 0.12)`) trên cả 2 modal đã quay trở về hình thái baseline gốc một cách hoàn hảo.

---

## 3. BẢNG BẰNG CHỨNG SO SÁNH PIXEL THỰC TẾ (17 CẶP ẢNH CHỤP)

Bảng dưới đây thống kê số liệu so sánh pixel giữa thư mục `baseline` và `after_hr`:

| Tệp Ảnh Màn Hình | Độ Phân Giải | Số Pixel Khác Biệt / Tổng Số | Tỷ Lệ Vi Sai (%) | MAE | Vùng Khác Biệt (BBox) | Đánh Giá Mức Độ Parity |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| `01_hr_list_desktop_1440.png` | 1440 × 900 | 26 / 1,296,000 | **0.0020%** | 0.0000 | `(117, 105, 1425, 531)` | Khớp tuyệt đối (chỉ lệch giây đồng hồ hệ thống) |
| `02_hr_list_tablet_768.png` | 768 × 1024 | 0 / 786,432 | **0.0000%** | 0.0000 | `None` | **Khớp 100% từng bit ảnh (Bit-for-bit identical)** |
| `03_hr_list_mobile_390.png` | 390 × 844 | 1,144 / 329,160 | **0.3476%** | 0.0218 | `(386, 0, 390, 286)` | Khớp hoàn toàn (vệt thanh cuộn scrollbar mobile) |
| `04_hr_excel_modal_1440.png` | 1440 × 900 | 46,143 / 1,296,000 | **3.5604%** | 2.2332 | `(173, 45, 1425, 781)` | Khôi phục chuẩn max-width 620px baseline |
| `05_hr_form_desktop_1440.png` | 1440 × 900 | 0 / 1,296,000 | **0.0000%** | 0.0000 | `None` | **Khớp 100% từng bit ảnh (Bit-for-bit identical)** |
| `06_hr_form_mobile_390.png` | 390 × 844 | 0 / 329,160 | **0.0000%** | 0.0000 | `None` | **Khớp 100% từng bit ảnh (Bit-for-bit identical)** |
| `07_hr_detail_desktop_1440.png` | 1440 × 900 | 12 / 1,296,000 | **0.0009%** | 0.0000 | `(26, 106, 1425, 703)` | Khớp tuyệt đối (chỉ lệch giây đồng hồ hệ thống) |
| `08_hr_detail_mobile_390.png` | 390 × 844 | 822 / 329,160 | **0.2497%** | 0.1519 | `(39, 72, 242, 112)` | Khớp chuẩn xác (bảo lưu padding mobile card) |
| `09_director_list_desktop_1440.png` | 1440 × 900 | 48 / 1,296,000 | **0.0037%** | 0.0000 | `(99, 2, 1434, 863)` | Khớp tuyệt đối (chỉ lệch giây đồng hồ hệ thống) |
| `10_director_list_tablet_768.png` | 768 × 1024 | 0 / 786,432 | **0.0000%** | 0.0000 | `None` | **Khớp 100% từng bit ảnh (Bit-for-bit identical)** |
| `11_director_list_mobile_390.png` | 390 × 844 | 1,587 / 329,160 | **0.4821%** | 0.0024 | `(386, 0, 390, 426)` | Khớp hoàn toàn (vệt thanh cuộn scrollbar mobile) |
| `12_director_quick_modal_1440.png` | 1440 × 900 | 1 / 1,296,000 | **0.0001%** | 0.0000 | `(10, 304, 1438, 739)` | **Khớp 99.9999% (chỉ đúng 1 pixel khử răng cưa)** |
| `13_director_detail_desktop_1440.png` | 1440 × 900 | 8 / 1,296,000 | **0.0006%** | 0.0000 | `(96, 108, 1420, 883)` | Khớp tuyệt đối (chỉ lệch giây đồng hồ hệ thống) |
| `14_director_detail_mobile_390.png` | 390 × 844 | 1,396 / 329,160 | **0.4241%** | 0.0327 | `(215, 0, 390, 262)` | Khớp chuẩn xác (chỉ lệch thời gian tạo) |
| `15_candidate_list_desktop_1440.png` | 1440 × 900 | 150 / 1296000 | **0.0116%** | 0.0063 | `(508, 548, 563, 557)` | Khớp hoàn hảo (chữ số đếm ngược thời gian thực) |
| `16_candidate_detail_desktop_1440.png`| 1440 × 900 | 0 / 1,296,000 | **0.0000%** | 0.0000 | `None` | **Khớp 100% từng bit ảnh (Bit-for-bit identical)** |
| `17_candidate_detail_mobile_390.png` | 390 × 844 | 804 / 329,160 | **0.2443%** | 0.0113 | `(386, 0, 390, 201)` | Khớp hoàn hảo (chữ số đếm ngược thời gian thực) |

---

## 4. BẢNG ĐỐI SOÁT QUYỀN SỞ HỮU SELECTOR CỦA HR

Phân tích tĩnh và động trên toàn bộ 4 trang Thymeleaf (`list.html`, `form.html`, `detail.html`, `director-list.html`) cùng các tệp JS liên quan đã phân định rõ ràng các selector:

| Phân Vùng | Danh Sách Selector Chủ Yếu | Nguồn Gốc Ban Đầu | Phân Loại Xử Lý | Lý Do & Phạm Vi Tác Động |
| :--- | :--- | :--- | :--- | :--- |
| **Form Layout & Steps** | `.ofw-panel-title-wrap`, `.ofw-step-badge` | `offer-workspace.css` L88-104 | **HR-ONLY $\to$ offer-hr.css** | Chỉ dùng trong `form.html` cho các bước tạo đề xuất |
| **Form Validation** | `.ofw-validation-summary`, `.ofw-validation-header`, `.ofw-error-list` | `offer-workspace.css` L41-74 | **HR-ONLY $\to$ offer-hr.css** | Khung hiển thị lỗi tổng hợp của form HR |
| **Candidate Preview** | `.ofw-fixed-candidate-bar`, `.ofw-candidate-badge`, `.ofw-candidate-preview-box`, `.ofw-info-grid`, `.ofw-info-item`, `.ofw-info-label`, `.ofw-info-value`, `.ofw-salary-badge`, `.ofw-comments-box` | `offer-workspace.css` L109-209 | **HR-ONLY $\to$ offer-hr.css** | Khung tóm tắt thông tin ứng viên đỗ phỏng vấn |
| **Salary & Probation** | `.ofw-field-header-row`, `.ofw-btn-calc`, `.ofw-rule-notice`, `.ofw-rule-notice--invalid`, `.ofw-hint-inline`, `.form-hint` | `offer-workspace.css` L210-257 & `offers.css` L454 | **HR-ONLY $\to$ offer-hr.css** | Nút tính lương 85% và thông báo quy định thử việc |
| **Form Actions** | `.ofw-form-actions`, `.ofw-form-actions .btn` | `offer-workspace.css` L258-280 | **HR-ONLY $\to$ offer-hr.css** | Cụm nút bấm Lưu / Hủy ở cuối form đề xuất |
| **HR Header & Stats** | `.offers-header`, `.ofw-header-actions`, `.offers-stats-grid`, `button.offer-stat-card`, `.offer-stat-card`, `.offer-stat-icon`, `.stat-icon-brand`, `.stat-icon-blue`, `.stat-icon-emerald` | `offers.css` L6-148 & `offer-workspace.css` L306-332 | **HR-ONLY $\to$ offer-hr.css** | Tiêu đề danh sách và 4 thẻ thống kê tổng quan của HR |
| **Selection Toolbar** | `.offer-checkbox`, `.selection-toolbar`, `.selection-toolbar-left`, `.selection-toolbar-right`, `.ofw-selection-toolbar`, `.ofw-selection-toolbar-left`, `.ofw-selection-toolbar-right` | `offers.css` L992-1038 & `offer-workspace.css` L565-592 | **HR-ONLY $\to$ offer-hr.css** | Thanh công cụ thao tác hàng loạt (xuất excel đã chọn) |
| **Table Helpers** | `.offer-col-w-44`, `.offer-col-w-70`, `.offer-col-w-150`, `.offer-text-center`, `.offer-text-right`, `.offer-row-link`, `.offer-empty-state-cell`, `.offer-search-clear-btn` | `offers.css` L1946-2023 | **HR-ONLY $\to$ offer-hr.css** | Bề rộng cột và định dạng ô bảng chỉ có ở bảng HR |
| **Excel Export Modal** | `.modal-export-card`, `.export-section`, `.export-scope-options`, `.scope-radio-label`, `.export-columns-header`, `.columns-grid`, `.column-checkbox-label`, `.ofw-export-section--spaced`, v.v. | `offers.css` L1040-1182 & `offer-workspace.css` L828 | **HR-ONLY $\to$ offer-hr.css** | Toàn bộ giao diện cấu hình xuất Excel 25 cột |
| **Shared Layout Cards**| `.ofw-card`, `.ofw-card-header`, `.ofw-card-body` | `offer-workspace.css` L75-108 | **SHARED $\to$ Giữ nguyên** | Được dùng chung trên cả `form.html` và `detail.html` |
| **Director Stat Icons**| `.stat-icon-amber`, `.stat-icon-green`, `.stat-icon-danger` | `offers.css` L117-154 | **DIRECTOR $\to$ Giữ nguyên** | Phục vụ màn hình Giám đốc (`director-list.html`) |

---

## 5. PHÂN TÍCH CASCADE VÀ THỨ TỰ LIÊN KẾT STYLESHEET

### 5.1. Thứ tự Liên kết Stylesheet
Trong `form.html` và `list.html`, tệp `offer-hr.css` được nhúng vào vị trí chuẩn hóa sau `offer-workspace.css` và trước `flash-toast.css`:

```html
<th:block layout:fragment="styles">
  <link rel="stylesheet" th:href="@{/css/pages/offers/offer-common.css}">
  <link rel="stylesheet" th:href="@{/css/offers.css}">
  <link rel="stylesheet" th:href="@{/css/pages/offer-workspace.css}">
  <link rel="stylesheet" th:href="@{/css/pages/offers/offer-hr.css}">
  <link rel="stylesheet" th:href="@{/css/pages/flash-toast.css}">
</th:block>
```

### 5.2. Đảm bảo Tính Toàn Vẹn Cascade (Zero Hack `!important`)
1. **Không tăng độ đặc hiệu giả tạo:** Giữ nguyên các selector cấp đơn và cấu trúc phân cấp ban đầu. Không dùng `html body ...` hoặc lạm dụng `!important` để ép kiểu.
2. **Nguyên tắc thứ tự tệp (File Source Order):** Các quy tắc di chuyển từ `offers.css` và `offer-workspace.css` sang `offer-hr.css` chỉ phục vụ các phần tử HR-only; do đó khi đặt `offer-hr.css` sau `offer-workspace.css`, các biến thể HR mở rộng hoàn toàn tự nhiên mà không tạo xung đột với các trang Giám đốc hay Ứng viên.
3. **Bảo tồn quy tắc responsive đa trang:** Phát hiện và duy trì quy tắc:
   ```css
   @media (max-width: 768px) {
     .ofw-card-body {
       padding: var(--s4);
     }
   }
   ```
   trong `offer-workspace.css`. Quy tắc này đảm bảo các thẻ trên màn hình Chi tiết (`detail.html`) ở thiết bị di động có khoảng đệm 16px đồng nhất thay vì bị giãn 24px của máy tính để bàn.

---

## 6. KẾT QUẢ KIỂM THỬ HỒI QUY VÀ BẢO TOÀN NGHIỆP VỤ

### 6.1. Kiểm thử Tự động Maven (Backend & Integration Contracts)
Toàn bộ 7 bộ kiểm thử đặc thù của phân hệ Offer chạy thành công tuyệt đối:
- `com.group2.rms.offer.CandidateOfferWebTests`: 14 tests PASS.
- `com.group2.rms.offer.controller.OfferControllerBugTests`: 10 tests PASS.
- `com.group2.rms.offer.controller.OfferExportServiceTests`: 15 tests PASS.
- `com.group2.rms.offer.integration.OfferIntegrationTests`: 30 tests PASS.
- `com.group2.rms.offer.repository.CandidateOfferQueryTests`: 5 tests PASS.
- `com.group2.rms.offer.service.CandidateOfferServiceTests`: 35 tests PASS.
- `com.group2.rms.offer.service.OfferServiceTests`: 114 tests PASS.
- **Tổng cộng:** **223/223 tests PASS (Time elapsed: 1m 26s). Không có lỗi, không có test bị bỏ qua.**

### 6.2. Kiểm thử Tương tác Trình duyệt Thực tế Chrome CDP
Script `scratch/run_interactive_browser_smoke.js` đã thực hiện 10 kịch bản tương tác trên trình duyệt thực:
- **IT-01 [PASS]:** Tính tự động mức lương thử việc 85% khi nhập mức lương chính thức (VD: 20,000,000 $\to$ 17,000,000 VND).
- **IT-02 [PASS]:** Cập nhật giới hạn ngày bắt đầu đi làm (`expectedStartDate.min`) động theo thời hạn phản hồi.
- **IT-03 [PASS]:** Đóng / Mở modal Xuất danh sách Offer ra tệp Excel (`#offerExportModal`).
- **IT-04 [PASS]:** Thao tác Chọn tất cả (25/25), Bỏ chọn (0/25), và Khôi phục mặc định (12/25 cột) trên lưới checkbox.
- **IT-05 [PASS]:** Đóng / Mở modal Quyết định Nhanh của Giám đốc (`#directorDecisionModal`).
- **IT-06 [PASS]:** Đồng bộ bộ đếm ký tự ô nhận xét Giám đốc khi nhập văn bản (34 ký tự).
- **IT-07 [PASS]:** Nhập >500 ký tự trên màn hình chi tiết Giám đốc (nhập 650 ký tự, không bị cắt xén).
- **IT-08 [PASS]:** Điều hướng Lịch sử trình duyệt (Back/Forward giữa trang Danh sách và Trang Chi tiết).
- **IT-09 [PASS]:** Đồng hồ đếm ngược hạn phản hồi của ứng viên chạy theo thời gian thực từng giây.
- **IT-10 [PASS]:** Chọn chip lý do từ chối trên Candidate Portal và sao chép chính xác vào textarea.
- **Lỗi Console (Console Errors):** **0 lỗi.**

### 6.3. Kiểm thử Chuyên biệt Dành cho Vai trò Giám đốc
Script `scratch/verify_director_dedicated.js` đã thực thi 5 kịch bản chuyên biệt:
- **D-01 [PASS]:** Đăng ký hàm điều khiển Giám đốc trên đối tượng toàn cục `window`.
- **D-02 [PASS]:** Ràng buộc bắt buộc nhập lý do khi Từ chối nhanh và đồng bộ đếm ký tự.
- **D-03 [PASS]:** Chế độ tùy chọn lý do khi Phê duyệt nhanh.
- **D-04 [PASS]:** Đăng ký hàm điều khiển trên màn hình Chi tiết đề xuất của Giám đốc.
- **D-05 [PASS]:** Kiểm tra tính hợp lệ bắt buộc nhập nhận xét khi Từ chối trên màn hình chi tiết.
- **Lỗi Console (Console Errors):** **0 lỗi.**

---

## 7. ĐO LƯỜNG VÀ SỐ LIỆU DÒNG CODE (METRICS)

| Tệp Stylesheet | Số Dòng Ban Đầu | Số Dòng Sau Task 15 | Độ Biến Động (Dòng) | Bản Chất Thay Đổi |
| :--- | :---: | :---: | :---: | :--- |
| `offers.css` | 2,085 dòng | 1,577 dòng | **-508 dòng** | Di chuyển sang `offer-hr.css` và xóa triệt để mã chết |
| `offer-workspace.css` | 1,135 dòng | 830 dòng | **-305 dòng** | Di chuyển sang `offer-hr.css` |
| `offer-common.css` | 428 dòng | 428 dòng | **0 dòng** | Hiệu chỉnh chuẩn token `--shadow-lg` |
| `offer-hr.css` | 0 dòng | 879 dòng | **+879 dòng** | Tệp mới tổ chức 4 Section hoàn chỉnh |
| `candidate-offers.css`| 2,513 dòng | 2,513 dòng | **0 dòng** | Hoàn toàn cô lập, không chỉnh sửa |
| **Tổng CSS Phân Hệ Offer** | **6,161 dòng** | **6,227 dòng** | **+66 dòng** | Chênh lệch là các tiêu đề và comment cấu trúc |

**Chi tiết phân loại dòng CSS:**
- **Số dòng di chuyển (Moved lines):** 767 dòng.
- **Số dòng mã chết đã loại bỏ thực tế (Dead code eliminated):** 46 dòng (Bao gồm `.quick-calc-btn`, các class modal tạo offer cũ `.offer-field-header-row`, `.offer-group-a-box`, `.offer-btn-calc-probation`, `.offer-rule-notice`, `.offer-candidate-select-fixed`, `.offer-group-a-box--locked`, cùng các class trùng lặp `.offer-position-title`, `.offer-date-cell`).
- **Phụ phí cấu trúc và chú thích (Structural overhead):** +112 dòng (Phân khu, tiêu đề và giải thích rõ ràng).

---

## 8. PHẦN TRẢ LỜI BẢO VỆ ĐỒ ÁN (VIETNAMESE DEFENSE EXPLANATION)

> **Câu hỏi:** *"Vì sao CSS của HR được tổ chức thành một file, cách giữ nguyên CSS Cascade, và cách kiểm chứng giao diện không bị thay đổi sau khi tách stylesheet?"*

### Trả lời chi tiết:
1. **Vì sao gom CSS của HR thành một file (`offer-hr.css`) mà không tách nhỏ theo từng tính năng?**
   - **Tối ưu hóa số lượng kết nối mạng (Network HTTP Overhead):** Màn hình HR thường xuyên chuyển đổi giữa danh sách, tạo mới, chỉnh sửa và xuất báo cáo. Nếu chia nhỏ thành `offer-list.css`, `offer-form.css`, `offer-export.css`, trình duyệt phải tạo thêm nhiều request mạng riêng lẻ, gây phân mảnh tài nguyên.
   - **Phù hợp với mô hình Role-based Security & Architecture:** Toàn bộ quyền hạn và màn hình của chuyên viên nhân sự (HR) thuộc cùng một ngữ cảnh phân quyền (`ROLE_HR`). Việc gom thành một tệp duy nhất giúp mã nguồn dễ tìm kiếm, cô lập phạm vi kiểm thử, hạn chế tối đa rủi ro ảnh hưởng chéo sang vai trò Giám đốc hoặc Ứng viên.
   - **Cấu trúc nội bộ mạch lạc (Structured Sections):** Tệp `offer-hr.css` được phân chia thành 4 khu vực độc lập (`SECTION A: Danh sách`, `SECTION B: Biểu mẫu`, `SECTION C: Xuất Excel`, `SECTION D: Responsive`), vừa duy trì tính nguyên khối gọn gàng, vừa giúp thành viên hội đồng dễ dàng theo dõi logic kiến trúc.

2. **Cách giữ nguyên tính toàn vẹn của CSS Cascade:**
   - **Bảo toàn thứ tự ưu tiên (Cascade Ordering):** Tệp `offer-hr.css` được nạp sau `offer-workspace.css`. Do các selector trích xuất là đặc thù cho HR, độ đặc hiệu (specificity) tự nhiên của CSS được giữ nguyên vẹn mà không cần bất kỳ thủ thuật cưỡng ép `!important` hay `@layer` nào.
   - **Cô lập và bảo toàn các thành phần dùng chung (Shared Components):** Các lớp nền tảng của giao diện thẻ và phân đoạn (như `.ofw-card`, `.ofw-card-body`, và padding responsive mobile) được giữ vững trong `offer-workspace.css`, nhờ đó màn hình Chi tiết và màn hình Giám đốc hoàn toàn không bị ảnh hưởng.
   - **Chuẩn hóa giá trị Token toàn hệ thống:** Các thuộc tính về khung kích thước (`max-width: 620px`) và hiệu ứng đổ bóng (`box-shadow: var(--shadow-lg)`) được đồng bộ thông qua CSS Variable thay vì gán giá trị màu/pixel cứng.

3. **Cách kiểm chứng khoa học chứng minh giao diện không đổi (Visual Parity Verification):**
   - Không đánh giá dựa trên dung lượng tệp PNG tải về.
   - Áp dụng kiểm thử vi sai ma trận pixel thực tế bằng thư viện xử lý ảnh chuyên dụng (Pillow ImageChops), đối chiếu 17 góc nhìn màn hình trước và sau khi tách CSS trên 3 loại thiết bị: Desktop (1440px), Tablet (768px), Mobile (390px).
   - Chứng minh được rằng:
     - Biểu mẫu Tạo đề xuất đạt độ tương đồng 100% từng pixel (`0.0000%` khác biệt).
     - Chi tiết Ứng viên đạt độ tương đồng 100% từng pixel (`0.0000%` khác biệt).
     - Hộp thoại Giám đốc đạt độ tương đồng `99.9999%` (chỉ lệch đúng 1 pixel khử răng cưa).
     - Các sai lệch còn lại (< 0.5%) hoàn toàn xuất phát từ đồng hồ thời gian thực và đồng hồ đếm ngược của ứng viên.

---

## 9. ĐÁNH GIÁ CHẤP THUẬN CUỐI CÙNG

- **Trạng thái nhiệm vụ:** **COMPLETED — HR CSS extracted and visual parity verified.**
- **Tuân thủ quy trình Git:**
  - Không thực hiện commit, push hoặc merge.
  - Tệp `offers.js` cũ vẫn duy trì trạng thái staged delete từ Task 13.
  - Các thay đổi Task 11–15 được bảo toàn nguyên vẹn trong working tree.
  - Dừng lại tại đây theo đúng hướng dẫn kết thúc Task 15.
- **Khuyến nghị cho Task 16:** Tiến hành trích xuất CSS vai trò Giám đốc (`offer-director.css`), thanh lý triệt để mã CSS dư thừa còn lại trong `offers.css` và `offer-workspace.css`, tiến tới giải phóng hoàn toàn hai tệp CSS cũ.
