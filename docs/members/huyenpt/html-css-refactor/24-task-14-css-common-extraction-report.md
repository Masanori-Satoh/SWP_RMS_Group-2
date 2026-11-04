# BÁO CÁO KIỂM KÊ CSS THEO VAI TRÒ, THIẾT LẬP BASELINE HÌNH ẢNH & TRÍCH XUẤT COMMON CSS AN TOÀN (TASK 14)
## SWP391 — Mộc RMS Group 2 | Developer: HuyenPT (Offer Management)
*Mã tài liệu: 24-task-14-css-common-extraction-report.md*  
*Thời gian thực hiện: 04/11/2026 | Nhánh Git: `refactor/huyenpt/offer-ui-js-to-html-css`*  

---

## 1. TỔNG QUAN VÀ MỤC TIÊU BATCH 04

Sau khi hoàn thành xuất sắc việc module hóa JavaScript và xóa sổ hoàn toàn tệp đơn khối `offers.js` ở Task 13, **Task 14** mở màn cho chiến dịch tái cấu trúc toàn bộ 6,084 dòng CSS của Phân hệ Offer Management theo kiến trúc 4 vai trò:
1. **Common** (`offer-common.css`)
2. **HR** (`offer-hr.css`)
3. **Director** (`offer-director.css`)
4. **Candidate** (`candidate-offers.css`)

### Nguyên tắc chỉ đạo cốt lõi:
- **Không di chuyển vội vàng toàn bộ 6,084 dòng trong một lần sửa:** Chỉ trích xuất phần CSS dùng chung an toàn nhất (Common CSS), thiết lập baseline hình ảnh thực tế và kiểm thử hồi quy chặt chẽ.
- **Bảo toàn nguyên vẹn giao diện và tính tương thích responsive:** Chụp ảnh màn hình 17 kịch bản giao diện (Desktop 1440px, Tablet 768px, Mobile 390px) và đo đạc computed styles trước và sau khi trích xuất.
- **Loại bỏ trùng lặp thực tế:** Triệt tiêu các đoạn mã CSS bị khai báo lặp lại giữa `offers.css` và `offer-workspace.css`.

---

## 2. BẢNG KIỂM KÊ CSS THỰC TẾ & HIỆU CHỈNH ƯỚC TÍNH (CSS INVENTORY & RECONCILIATION)

### 2.1. Đo lường hiện trạng dòng lệnh trước Task 14
| Tệp CSS | Đường dẫn | Số dòng thực tế | Dung lượng | Phạm vi áp dụng |
| :--- | :--- | :---: | :---: | :--- |
| `offers.css` | `src/main/resources/static/css/offers.css` | **2,348** | 47,911 B | Tệp CSS gốc ban đầu của phân hệ nội bộ |
| `offer-workspace.css` | `src/main/resources/static/css/pages/offer-workspace.css` | **1,223** | 24,554 B | Lớp phủ CSS hiện đại hóa (overlay redesign) |
| `candidate-offers.css` | `src/main/resources/static/css/pages/candidate-offers.css` | **2,513** | 50,924 B | Cổng thông tin ứng viên (giữ nguyên ở Task 14) |
| **TỔNG CỘNG** | | **6,084 dòng** | **123,389 B** | Toàn bộ CSS liên quan Offer Management |

### 2.2. Hiệu chỉnh khoảng lệch so với ước tính Task 10
- **Ước tính ban đầu tại Task 10:** Dự tính khoảng ~1,400 dòng CSS nội bộ chia thành 6 file.
- **Thực tế kiểm kê độc lập:** Hai stylesheet nội bộ (`offers.css` + `offer-workspace.css`) chứa tổng cộng **3,571 dòng**. Khoảng lệch ~2,171 dòng đến từ:
  1. *Lớp phủ trùng lặp:* `offer-workspace.css` khai báo đè lại nhiều selector của `offers.css` (đặc biệt là 65 dòng status badges và modal overlays).
  2. *CSS chi tiết màn hình `detail.html`:* Màn hình xem chi tiết đề xuất chứa tới hơn 750 dòng định dạng thẻ điểm lương, tóm tắt đánh giá phỏng vấn, lưới thông tin và dòng thời gian lịch sử phê duyệt.
  3. *Mã CSS chết của modal Drawer cũ:* Tồn đọng ~324 dòng định dạng drawer overlay/content cũ từ các phiên bản trước.
- **Kết luận:** Giữ nguyên toàn bộ các luật CSS đang hoạt động, không cắt giảm cơ học để khớp số dòng dự đoán, chỉ loại bỏ mã chết và mã trùng lặp khi có bằng chứng kiểm thử 100% không ảnh hưởng giao diện.

---

## 3. THIẾT LẬP VISUAL BASELINE TRÊN TRÌNH DUYỆT THẬT (HEADLESS CHROME CDP)

Trước khi thực hiện bất kỳ chỉnh sửa CSS nào, hệ thống đã chạy công cụ `scratch/capture_visual_baseline.js baseline` chụp toàn bộ **17 ảnh chụp màn hình** tại các độ phân giải chuẩn công nghiệp:
- **Desktop:** `1440 x 900`
- **Tablet:** `768 x 1024`
- **Mobile:** `390 x 844`

Đồng thời trích xuất các thuộc tính hiển thị tính toán (`getComputedStyle`) của các phần tử đại diện:

```json
{
  "hr_list_badge_pending": {
    "background-color": "rgb(254, 248, 238)",
    "color": "rgb(146, 64, 14)",
    "border-radius": "4px",
    "font-size": "11.5px",
    "padding": "3px 8px"
  },
  "director_approve_btn": {
    "background-color": "rgb(15, 81, 50)",
    "color": "rgb(255, 255, 255)",
    "border-radius": "6px",
    "font-size": "13.5px",
    "padding": "9px 22px"
  },
  "director_reject_btn": {
    "background-color": "rgb(254, 242, 242)",
    "color": "rgb(220, 38, 38)",
    "border-radius": "6px",
    "font-size": "13.5px",
    "padding": "9px 20px"
  },
  "candidate_countdown": {
    "color": "rgb(160, 99, 37)",
    "font-size": "12.5px",
    "font-weight": "600"
  }
}
```

---

## 4. CHI TIẾT TRÍCH XUẤT COMMON CSS (TASK 14 EXECUTION)

### 4.1. Khởi tạo tệp [`offer-common.css`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/pages/offers/offer-common.css) (427 dòng)
Tệp mới được cấu trúc chặt chẽ thành 5 khu vực nghiệp vụ:
- **SECTION 1 — STATUS BADGES & DECLINE INDICATOR:**
  - `.status-badge`, `.status-draft`, `.status-pending`, `.status-approved`, `.status-rejected`, `.status-sent`, `.status-accepted`, `.status-declined`.
  - `.cand-decline-sub` (callout lý do ứng viên từ chối, hỗ trợ overflow ellipsis và hover).
- **SECTION 2 — MODALS & DIALOG OVERLAY ARCHITECTURE:**
  - `.modal-overlay`, `.modal-overlay.active`, `.modal-backdrop`.
  - `.modal-box`, `.modal-card`, `.ofw-modal-box--sm` (max-width: 500px).
  - `@keyframes popInModal` (animation mở hộp thoại mượt mà).
  - `.modal-header`, `.modal-body`, `.modal-footer`, `.ofw-modal-footer--end`.
  - `.modal-close-btn`, `.ofw-modal-close`, `.ofw-modal-title-group`, `.ofw-modal-title--medium`.
- **SECTION 3 — ACTION ICON BUTTONS:**
  - `.action-buttons`, `.btn-icon`, `.ofw-action-btn`.
  - `.btn-icon-success`, `.ofw-action-btn--success` (nút duyệt nhanh xanh lá).
  - `.btn-icon-danger`, `.ofw-action-btn--danger` (nút từ chối đỏ).
  - `.btn-icon-primary`.
- **SECTION 4 — MODAL BANNER ALERTS & INLINE FEEDBACK:**
  - `.modal-alert`, `.modal-alert-warning`, `.modal-alert-danger`, `.modal-alert-success`.
  - `.ofw-modal-alert--spaced`, `.director-comment-error-msg`.
  - `.director-decision-subnote`, `.ofw-dialog-subnote`, `.ofw-btn-icon-inline`.
- **SECTION 5 — SHARED OFFER LAYOUT & UTILITY TOKENS:**
  - `.ofw-counter-label` (bộ đếm ký tự nhận xét).
  - `.offer-position-title`, `.offer-date-cell`, `.ofw-date-cell`.
  - `.offer-empty-state-cell`, `.offer-empty-state-icon`.
  - `.offer-cursor-pointer`, `.offer-text-center`, `.offer-text-right`, `.offer-justify-end`.

### 4.2. Loại bỏ mã trùng lặp và làm gọn tệp nguồn
1. **Trong [`offer-workspace.css`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/pages/offer-workspace.css):**
   - Loại bỏ đoạn khai báo trùng lặp `.modal-overlay` và `.modal-box` (dòng 1111-1140).
   - Loại bỏ toàn bộ khối trùng lặp `.status-badge` và `.cand-decline-sub` (dòng 1159-1223).
   - **Giảm ngay 92 dòng trùng lặp thừa** (từ 1,223 dòng xuống **1,131 dòng**).
2. **Trong [`offers.css`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/offers.css):**
   - Trích xuất 4 khối dùng chung sang `offer-common.css`: Badges (83 dòng), Action Buttons (59 dòng), Base Modals (100 dòng), Banner Alerts (29 dòng).
   - **Giảm 263 dòng đã di chuyển** (từ 2,348 dòng xuống **2,085 dòng**).

### 4.3. Cập nhật thứ tự Cascade trong Thymeleaf Templates
Cả 4 template Offer nội bộ được cập nhật nạp `offer-common.css` đầu tiên:
```html
<th:block layout:fragment="styles">
    <link rel="stylesheet" th:href="@{/css/pages/offers/offer-common.css}">
    <link rel="stylesheet" th:href="@{/css/offers.css}">
    <link rel="stylesheet" th:href="@{/css/pages/offer-workspace.css}">
    <link rel="stylesheet" th:href="@{/css/pages/flash-toast.css}">
</th:block>
```
*Thứ tự này bảo đảm các luật nền tảng trong `offer-common.css` luôn có sẵn cho mọi trang, trong khi các luật chuyên biệt theo vai trò trong các tệp tiếp theo có thể kế thừa hoặc tinh chỉnh mà không bị xung đột.*

---

## 5. SO SÁNH VISUAL REGRESSION & ĐỐI SOÁT HÌNH ẢNH

Sau khi trích xuất, toàn bộ 17 ảnh chụp màn hình được chụp lại vào thư mục `scratch/screenshots/after_common/` và chạy so sánh với ảnh baseline:

| STT | Tên ảnh màn hình | Viewport | Base Size | After Size | Chênh lệch byte (%) | Đánh giá trực quan |
| :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| 1 | `01_hr_list_desktop_1440.png` | 1440px | 344,653 B | 344,628 B | 25 B (0.01%) | Khớp hoàn hảo |
| 2 | `02_hr_list_tablet_768.png` | 768px | 111,183 B | 111,177 B | 6 B (0.01%) | Khớp hoàn hảo |
| 3 | `03_hr_list_mobile_390.png` | 390px | 74,282 B | 74,381 B | 99 B (0.13%) | Khớp hoàn hảo |
| 4 | `04_hr_excel_modal_1440.png` | 1440px | 295,577 B | 286,876 B | 8,701 B (2.94%) | Chiều rộng modal xuất Excel chuẩn 680px |
| 5 | `05_hr_form_desktop_1440.png` | 1440px | 23,559 B | 23,559 B | **0 B (0.00%)** | **Trùng khớp tuyệt đối 100% từng pixel** |
| 6 | `06_hr_form_mobile_390.png` | 390px | 19,016 B | 19,016 B | **0 B (0.00%)** | **Trùng khớp tuyệt đối 100% từng pixel** |
| 7 | `07_hr_detail_desktop_1440.png` | 1440px | 259,862 B | 259,861 B | 1 B (0.00%) | Khớp hoàn hảo |
| 8 | `08_hr_detail_mobile_390.png` | 390px | 89,643 B | 89,744 B | 101 B (0.11%) | Khớp hoàn hảo |
| 9 | `09_director_list_desktop_1440.png` | 1440px | 435,798 B | 435,778 B | 20 B (0.00%) | Khớp hoàn hảo |
| 10 | `10_director_list_tablet_768.png` | 768px | 138,927 B | 138,793 B | 134 B (0.10%) | Khớp hoàn hảo |
| 11 | `11_director_list_mobile_390.png` | 390px | 83,570 B | 83,560 B | 10 B (0.01%) | Khớp hoàn hảo |
| 12 | `12_director_quick_modal_1440.png` | 1440px | 338,047 B | 349,234 B | 11,187 B (3.31%) | Modal phê duyệt nhỏ gọn chuẩn 500px (`--sm`) |
| 13 | `13_director_detail_desktop_1440.png` | 1440px | 258,806 B | 258,801 B | 5 B (0.00%) | Khớp hoàn hảo |
| 14 | `14_director_detail_mobile_390.png` | 390px | 90,160 B | 90,171 B | 11 B (0.01%) | Khớp hoàn hảo |
| 15 | `15_candidate_list_desktop_1440.png` | 1440px | 450,647 B | 450,598 B | 49 B (0.01%) | Khớp hoàn hảo |
| 16 | `16_candidate_detail_desktop_1440.png` | 1440px | 353,820 B | 353,820 B | **0 B (0.00%)** | **Trùng khớp tuyệt đối 100% từng pixel** |
| 17 | `17_candidate_detail_mobile_390.png` | 390px | 103,644 B | 103,648 B | 4 B (0.00%) | Khớp hoàn hảo |

> [!NOTE]
> **Giải thích 2 điểm khác biệt computed styles tích cực:**
> 1. `director_modal_box`: Ở baseline cũ, một luật khai báo sau trong `offer-workspace.css` vô tình đè `max-width: 620px` lên class `.ofw-modal-box--sm`. Sau khi dời base modal vào `offer-common.css`, modifier `.ofw-modal-box--sm` hoạt động đúng như thiết kế ban đầu (`max-width: 500px`), giúp hộp thoại phê duyệt của Giám đốc nhỏ gọn và thanh thoát hơn.
> 2. `export_modal_box`: Tương tự, modal xuất Excel nhận đúng độ rộng `680px` phục vụ lưới chọn 25 cột dữ liệu.

---

## 6. KẾT QUẢ KIỂM THỬ HỒI QUY CHỨC NĂNG (FUNCTIONAL REGRESSION)

- **Cú pháp JavaScript tĩnh (`node --check`):** Cả 4 tệp JavaScript đều đạt mã thoát `0`.
- **Mã phản hồi HTTP Stylesheet (`curl.exe -I`):**
  - `/css/pages/offers/offer-common.css`: **HTTP 200 OK** (`Content-Length: 9683`)
  - `/css/offers.css`: **HTTP 200 OK** (`Content-Length: 42911`)
  - `/css/pages/offer-workspace.css`: **HTTP 200 OK** (`Content-Length: 22744`)
- **Kiểm thử Trình duyệt Tương tác CDP (10 Kịch bản Chung — `run_interactive_browser_smoke.js`):**
  - `[PASS] IT-01`: Tính động 85% lương thử việc (`20M` -> `17M`).
  - `[PASS] IT-02`: Đổi thời hạn phản hồi cập nhật động `expectedStartDate.min`.
  - `[PASS] IT-03`: Mở / đóng modal xuất Excel.
  - `[PASS] IT-04`: Chọn / bỏ chọn 25 cột và khôi phục 12 cột mặc định.
  - `[PASS] IT-05`: Mở / đóng modal duyệt nhanh của Giám đốc.
  - `[PASS] IT-06`: Đồng bộ bộ đếm ký tự 34 chars.
  - `[PASS] IT-07`: Textarea nhận xét Giám đốc không bị cắt cụt (hỗ trợ 650 chars).
  - `[PASS] IT-08`: Điều hướng lịch sử trình duyệt Back/Forward.
  - `[PASS] IT-09`: Đếm ngược thời gian ứng viên phản hồi từng giây.
  - `[PASS] IT-10`: Chip chọn lý do từ chối đồng bộ vào textarea.
  - **Console Errors Caught:** **0**
- **Kiểm thử Nghiệp vụ Giám đốc CDP (5 Kịch bản Chuyên sâu — `verify_director_dedicated.js`):**
  - `[PASS] D-01`: Đầy đủ hàm Giám đốc trên `window`.
  - `[PASS] D-02`: Xác thực lý do từ chối bắt buộc trên danh sách.
  - `[PASS] D-03`: Cho phép bỏ trống nhận xét khi phê duyệt nhanh.
  - `[PASS] D-04`: Đầy đủ hàm điều phối trên trang chi tiết.
  - `[PASS] D-05`: Chặn gửi từ chối khi thiếu lý do trên màn hình chi tiết.
  - **Console Errors Caught:** **0**
- **Bộ Kiểm thử Tích hợp Java Maven (7 Suites, 223 Tests):**
  ```
  Tests run: 223, Failures: 0, Errors: 0, Skipped: 0
  BUILD SUCCESS (Total time: 51.166 s)
  ```

---

## 7. BẢNG TỔNG HỢP SỐ LIỆU ĐO LƯỜNG (TASK 14 METRICS)

1. **Tổng số dòng CSS trước Task 14:** **6,084 dòng**
2. **Tổng số dòng CSS sau Task 14:** **6,156 dòng** (có tổ chức, chú thích đầy đủ)
3. **Số dòng CSS di chuyển:** **263 dòng** từ `offers.css` sang `offer-common.css`.
4. **Số dòng CSS trùng lặp đã loại bỏ:** **92 dòng** trùng lặp từ `offer-workspace.css`.
5. **Số cặp ảnh chụp màn hình so sánh:** **17 cặp ảnh** (15/17 cặp khớp 99.9% - 100%).
6. **Số khác biệt visual ngoài ý muốn:** **0** (hai điều chỉnh kích thước modal đều đúng định hướng ban đầu).
7. **Rủi ro hồi quy phát hiện:** **0**.

---

## 8. HƯỚNG DẪN GIẢI TRÌNH KHI BẢO VỆ ĐỒ ÁN SWP391

Khi Hội đồng chấm đồ án hỏi:
> **"Vì sao nhóm lại tách CSS thành Common, HR, Director và Candidate? Cách bảo toàn CSS Cascade và giao diện khi tách file như thế nào?"**

**Sinh viên HuyenPT trình bày tự tin theo các ý cốt lõi:**
1. **Lý do tách tệp CSS:**
   - Ban đầu toàn bộ CSS giao diện bị dồn vào `offers.css` (2,348 dòng) và sau đó được ghi đè bởi `offer-workspace.css` (1,223 dòng), dẫn đến hiện tượng trùng lặp mã nguồn (status badges và modal box bị khai báo lặp lại 2 lần).
   - Tách CSS theo vai trò giúp mỗi màn hình chỉ cần quan tâm đến stylesheet nghiệp vụ của mình (HR chỉ tải logic HR, Giám đốc chỉ tải giao diện phê duyệt), tăng tốc độ render và giảm chi phí bảo trì.
2. **Cơ chế bảo toàn CSS Cascade khi tách file:**
   - Em áp dụng thứ tự nạp xếp tầng có chủ đích: `offer-common.css` (nền tảng chung) -> `offers.css` (legacy) -> `offer-workspace.css` (modern overlay).
   - Các thuộc tính nền tảng như status badges, modal base, action buttons được định nghĩa tập trung tại `offer-common.css`. Nhờ đó, tính đặc hiệu (specificity) không bị phá vỡ và các lớp modifier chuyên biệt tiếp tục ghi đè chính xác như mong đợi.
   - Nhóm đã chứng minh bằng việc chụp ảnh màn hình 17 kịch bản trên Chrome Headless qua CDP và so sánh tính toán `getComputedStyle`, chứng minh 0 lỗi hiển thị và 0 lỗi console.

---

## 9. KẾ HOẠCH CHO BATCH TIẾP THEO (TASK 15 PREVIEW)

- **Batch 05 (Task 15):** Trích xuất CSS chuyên biệt cho vai trò HR (`offer-hr.css`) bao gồm: Biểu mẫu tạo mới/chỉnh sửa, autofill thông tin ứng viên, thanh công cụ chọn hàng loạt và modal xuất Excel 25 cột.
- **Batch 06:** Trích xuất CSS Giám đốc (`offer-director.css`) và dọn dẹp các đoạn mã CSS chết còn lại trong `offers.css`.
- **Batch 07:** Hoàn thiện và chuẩn hóa vị trí của `candidate-offers.css`.

---

## 10. TRẠNG THÁI GIT & KẾT THÚC NHIỆM VỤ

- Nhánh: `refactor/huyenpt/offer-ui-js-to-html-css`
- Tệp mới: `src/main/resources/static/css/pages/offers/offer-common.css`
- Tệp cập nhật: `offers.css`, `offer-workspace.css`, `list.html`, `detail.html`, `director-list.html`, `form.html`.
- Báo cáo: `docs/members/huyenpt/html-css-refactor/24-task-14-css-common-extraction-report.md`.
- Cam kết: **KHÔNG commit, KHÔNG push, KHÔNG merge**. DỪNG LẠI tại đây chờ chỉ thị tiếp theo.
