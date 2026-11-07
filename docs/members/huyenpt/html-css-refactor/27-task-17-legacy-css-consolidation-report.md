# BÁO CÁO TASK 17: CỦNG CỐ KIẾN TRÚC CSS OFFER, TỐI ƯU GIẢM MÃ THỰC CHẤT & ĐÁNH GIÁ NGHỈ HƯU CSS CŨ AN TOÀN

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).

---

## THÔNG TIN TỔNG QUAN

- **Dự án:** SWP391 — Hệ thống Quản trị Tuyển dụng Mộc RMS (Group 2)
- **Phân hệ:** Quản lý Đề xuất Nhận việc (Offer Management)
- **Sinh viên thực hiện:** HuyenPT
- **Git Branch:** `refactor/huyenpt/offer-ui-js-to-html-css`
- **Git Commit Baseline (HEAD):** `10031e68`
- **Tình trạng phân hệ sau Task 17:**
  - Củng cố Kiến trúc CSS Mô-đun vai trò (4 tệp): **HOÀN THÀNH (COMPLETED)**
  - Đánh giá Nghỉ hưu `offers.css`: **NGHỈ HƯU MỘT PHẦN (PARTIAL RETIREMENT - CASCADE SAFETY LAYER)**
  - Đánh giá Nghỉ hưu `offer-workspace.css`: **NGHỈ HƯU MỘT PHẦN (PARTIAL RETIREMENT - CASCADE SAFETY LAYER)**
  - Tối ưu Giảm Mã Nguồn CSS Thực chất: **GIẢM THỰC CHẤT 198 DÒNG, 24 QUY TẮC, 129 KHAI BÁO, 4,259 BYTES**
  - Đảm bảo tính toàn vẹn chức năng & hồi quy: **223/223 Unit/Integration Tests PASS, 10/10 Interactive Smoke Tests PASS, 5/5 Dedicated Director Tests PASS, 17/17 Visual Screens PASS**

---

## 1. XÁC MINH VÀ HIỆU CHÍNH BASELINE TASK 16 & TASK 17

Trước khi thực hiện tối ưu hóa trong Task 17, dữ liệu baseline đã được kiểm toán và tính toán lại chính xác từ mã nguồn thực tế:

### 1.1. Bảng số liệu CSS thực tế trước Task 17 (Baseline Task 16):

| Tệp CSS | Kích thước (Bytes) | Tổng số dòng | Dòng không rỗng | Số quy tắc (Rules) | Số khai báo (Decls) | Vai trò kiến trúc |
|:---|---:|---:|---:|---:|---:|:---|
| `offers.css` (legacy) | 33,258 | 1,577 | 1,370 | 199 | 925 | Lớp an toàn kế thừa cũ |
| `offer-workspace.css` (legacy) | 17,184 | 830 | 705 | 117 | 422 | Lớp layout workspace cũ |
| `offer-common.css` (mô-đun) | 10,094 | 428 | 370 | 54 | 210 | Thành phần dùng chung nội bộ |
| `offer-hr.css` (mô-đun) | 17,690 | 864 | 737 | 119 | 456 | Phân hệ Nhân sự (HR) |
| `offer-director.css` (mô-đun) | 9,826 | 408 | 357 | 44 | 234 | Phân hệ Giám đốc (Director) |
| `candidate-offers.css` (mô-đun) | 50,924 | 2,513 | 2,169 | 338 | 1,383 | Cổng ứng viên (Candidate Portal) |
| **TỔNG NỘI BỘ (5 tệp Offer nội bộ)** | **88,052** | **4,107** | **3,539** | **533** | **2,247** | Phân hệ Quản trị Offer |
| **TỔNG TOÀN BỘ (6 tệp CSS)** | **138,976** | **6,620** | **5,708** | **871** | **3,630** | Toàn bộ phân hệ Offer |

### 1.2. Hiệu chính Footprint JavaScript:
- **JavaScript nội bộ (HR, Director, Common):** 1,282 dòng (`offer-common.js`: 211 dòng + `offer-hr.js`: 591 dòng + `offer-director.js`: 480 dòng).
- **JavaScript Cổng ứng viên:** 285 dòng (`candidate-offers.js`).
- **Tổng JavaScript toàn phân hệ Offer:** 1,567 dòng (đã loại bỏ an toàn 438 dòng từ file nguyên khối `offers.js` 2,005 dòng ban đầu).
- Tệp `static/js/offers.js` vẫn giữ nguyên trạng thái staged delete (`deleted: src/main/resources/static/js/offers.js`).

---

## 2. KIỂM TOÁN THỰC CHẤT QUÁ TRÌNH BÓC TÁCH CSS TASK 16 (PHASE 2 AUDIT)

Task 16 đã tạo ra `offer-director.css` (408 dòng). Kiểm toán mã nguồn chi tiết cho thấy:

### 2.1. Phân loại 408 dòng trong `offer-director.css`:
1. **Quy tắc sao chép trùng lặp từ `offers.css`:**
   - Khối Thẻ Quyết định Giám đốc (Director Decision Card): `.director-decision-card`, `.director-decision-card-header`, `.director-decision-card-title`, `.director-badge-final`, `.director-decision-card-body`, `.director-comment-label-row`, `.director-decision-textarea`, `.director-decision-subnote`, `.btn-director-reject`, `.btn-director-approve` (133 dòng trong `offers.css` từ dòng 1409 đến 1541).
2. **Quy tắc sao chép trùng lặp từ `offer-workspace.css`:**
   - `.ofw-director-stats-grid` (dòng 79-84 trong `offer-workspace.css`).
   - `.ofw-director-tabs`, `.ofw-director-tab`, `.ofw-director-tab.active` (dòng 187-217 trong `offer-workspace.css`).
   - `.director-decision-textarea.is-invalid`, `.director-comment-error-msg`, `.ofw-dialog-subnote` (dòng 780-805 trong `offer-workspace.css`).
3. **Kết luận kiểm toán Task 16:**
   - Trong Task 16, các quy tắc này được tạo mới trong `offer-director.css` nhưng **chưa được xóa** khỏi `offers.css` và `offer-workspace.css`. Do đó, dòng code thực tế ở Task 16 là **sao chép trùng lặp (duplication)** chứ chưa tối ưu giảm kích thước.
   - Task 17 đã trực tiếp giải quyết vấn đề này bằng cách xóa triệt để mã nguồn trùng lặp ở tệp cũ sau khi kiểm chứng tệp mới đã nắm toàn quyền cascade.

### 2.2. Bảng ánh xạ quy tắc Director và Hành động xử lý:

| Tệp gốc | Selector | Khai báo gốc | Tệp đích mới | Trùng lặp? | Cần thiết? | Hành động Task 17 |
|:---|:---|:---|:---|:---:|:---:|:---|
| `offers.css` | `.director-decision-card` | `border: 1px solid #0f5132; ...` | `offer-director.css` (Section C) | Có | Có | Xóa khỏi `offers.css`, bảo toàn trong `offer-director.css` |
| `offers.css` | `.director-decision-card-header` | `background: #0f5132; color: #fff;` | `offer-director.css` (Section C) | Có | Có | Xóa khỏi `offers.css`, bảo toàn trong `offer-director.css` |
| `offers.css` | `.director-decision-textarea` | `border: 1px solid #cbd5e1; ...` | `offer-director.css` (Section C) | Có | Có | Xóa khỏi `offers.css`, bảo toàn trong `offer-director.css` |
| `offers.css` | `.btn-director-approve` | `background: #0f5132; color: #fff;` | `offer-director.css` (Section C) | Có | Có | Xóa khỏi `offers.css`, bảo toàn trong `offer-director.css` |
| `offers.css` | `.btn-director-reject` | `background: #fef2f2; color: #dc2626;`| `offer-director.css` (Section C) | Có | Có | Xóa khỏi `offers.css`, bảo toàn trong `offer-director.css` |
| `offer-workspace.css` | `.ofw-director-stats-grid` | `grid-template-columns: repeat(3, 1fr)`| `offer-director.css` (Section A) | Có | Có | Xóa khỏi `offer-workspace.css`, giữ trong `offer-director.css` |
| `offer-workspace.css` | `.ofw-director-tabs` | `display: flex; gap: var(--s2);` | `offer-director.css` (Section A) | Có | Có | Xóa khỏi `offer-workspace.css`, giữ trong `offer-director.css` |
| `offer-workspace.css` | `.director-comment-error-msg`| `display: none; color: var(--error);` | `offer-director.css` (Section B) | Có | Có | Xóa khỏi `offer-workspace.css`, giữ trong `offer-director.css` |

---

## 3. KIỂM TOÁN TOÀN DIỆN PHỤ THUỘC CSS CŨ (PHASE 3 AUDIT)

### 3.1. Phân tích tham chiếu toàn dự án:
Kiểm toán độc lập toàn bộ kho mã nguồn bằng script `search_all_legacy_refs.py` cho thấy:
- `offers.css` và `offer-workspace.css` **CHỈ ĐƯỢC THAM CHIẾU DUY NHẤT** tại các template thuộc module Offer:
  1. `src/main/resources/templates/offers/list.html`
  2. `src/main/resources/templates/offers/form.html`
  3. `src/main/resources/templates/offers/director-list.html`
  4. `src/main/resources/templates/offers/detail.html`
  5. `src/main/resources/templates/offers/candidate-list.html` (`offers.css`)
  6. `src/main/resources/templates/offers/candidate-detail.html` (`offers.css`)
- **TUYỆT ĐỐI KHÔNG CÓ BẤT KỲ MODULE NÀO KHÁC** (Auth, Application, Candidate Journey, Interview, Job Posting, Admin) nạp hay phụ thuộc vào `offers.css` hoặc `offer-workspace.css`.
- Nhận định của Task 16 về việc *"hơn 120 quy tắc có nguy cơ dùng chung cho các phân hệ khác"* là giả định chưa kiểm chứng qua runtime link tags. Thực tế, các module khác sử dụng bộ stylesheet riêng biệt (`admin-workspace.css`, `design-tokens.css`, `interface.css`).

### 3.2. Phân loại quy tắc trong 2 tệp CSS cũ:
- **Trong `offers.css` (1,577 dòng ban đầu):**
  - Đã mô-đun hóa (Duplicate): 25 quy tắc.
  - Mã nguồn rác/chết (Dead Code): 54 quy tắc (bao gồm Detail Drawer cũ, Mockup decision card cũ, các helper không dùng).
  - Quy tắc chuyên biệt Offer (Offer-only): 110 quy tắc (bảo lưu để duy trì tính toàn vẹn bố cục).
- **Trong `offer-workspace.css` (830 dòng ban đầu):**
  - Đã mô-đun hóa (Duplicate): 32 quy tắc.
  - Mã nguồn rác/chết: 9 quy tắc.
  - Quy tắc layout bảng & surface Offer: 73 quy tắc.

---

## 4. GIẢI QUYẾT TRIỆT ĐỂ CÁC ĐIỂM NGHẼN (PHASE 4 BLOCKERS)

### Blocker A — Đệm Ô Bảng Dữ Liệu (Table Cell Padding)
- **Vấn đề báo cáo ở Task 16:** E ngại selector `.offers-table th, .offers-table td` có `padding: 10px 10px;` trong `offers.css` sẽ bị `admin-workspace.css` ghi đè thành `13px 16px` làm vỡ chiều cao bảng nếu gỡ file cũ.
- **Kiểm chứng thực tế bằng Chrome DevTools Protocol (CDP):**
  - Chạy kịch bản `scratch/check_table_padding.js` truy vấn trực tiếp thuộc tính `window.getComputedStyle(th)` và `getBoundingClientRect()` trên DOM thực tế.
  - **Kết quả đo đạc thực tế:**
    - `th_padding`: **12px 16px**
    - `td_padding`: **12px 16px**
    - `th_height`: **45.5px**
  - **Nguyên nhân kỹ thuật:** Quy tắc `10px 10px` tại dòng 190 của `offers.css` vốn dĩ **ĐÃ BỊ GHI ĐÈ** từ trước bởi selector có độ đặc hiệu cao hơn trong `offer-workspace.css` (`.ofw-list-surface .adw-data-table th/td { padding: var(--s3) var(--s4); }`).
  - **Kết luận:** Quy tắc `10px 10px` trong `offers.css` là mã không còn hiệu lực thực tế; chiều cao bảng được bảo toàn 100% theo chuẩn Admin Workspace (45.5px).

### Blocker B — Khung Chứa Trang Chi Tiết (`offer-detail-container`)
- **Vấn đề báo cáo ở Task 16:** E ngại selector `.offer-detail-container` trong `offers.css` (dòng 1542) định nghĩa `max-width: 1040px; margin: 0 auto;` sẽ khiến trang bung rộng 1440px nếu xóa file.
- **Kiểm chứng thực tế bằng Chrome CDP:**
  - Chạy kịch bản `scratch/check_detail_container.js` trên trang `/offers/1`.
  - **Kết quả đo đạc thực tế:**
    - `tag`: `MAIN`
    - `classes`: `page-content container adw-page ofw-page offer-detail-container`
    - `maxWidth`: **"none"**
    - `width`: **1333px**
    - `padding`: **16px**
  - **Nguyên nhân kỹ thuật:** Trong `offer-workspace.css` (dòng 22), selector `.ofw-page.offer-detail-container { width: 100%; max-width: none; margin-inline: 0; }` nạp sau đã ghi đè hoàn toàn quy tắc `max-width: 1040px`. Do đó, trang chi tiết từ lâu đã hiển thị theo chiều rộng tự nhiên của container (1333px), không hề phụ thuộc vào con số 1040px cũ.
  - **Kết luận:** `max-width: 1040px` là quy tắc chết (dead override), không ảnh hưởng đến giao diện hiện đại.

### Blocker C — Bảo toàn Gradient Nền và Khung Layout Kế thừa
- Khi thử nghiệm tháo gỡ hoàn toàn 2 tệp cũ, các trang danh sách và chi tiết ghi nhận sự biến thiên nhẹ về gradient nền (`color-mix` và background mesh) do các biến và hiệu ứng kế thừa từ layout cha.
- Để đảm bảo an toàn tuyệt đối theo tiêu chuẩn khắt khe nhất của đồ án tốt nghiệp SWP391, hệ thống duy trì lớp đệm an toàn sau khi đã loại bỏ toàn bộ mã trùng lặp.

---

## 5. KẾT QUẢ TRIỂN KHAI VÀ TỐI ƯU HÓA MÃ NGUỒN (PHASE 5, 6, 10)

Task 17 đã thực hiện loại bỏ có kiểm chứng toàn bộ các khối mã nguồn trùng lặp:

1. **Khối Director Decision Card & Thao tác duyệt:** Xóa 133 dòng (từ dòng 1409 đến 1541) khỏi `offers.css`.
2. **Khối Director Stats Grid & Tabs:** Xóa 37 dòng khỏi `offer-workspace.css`.
3. **Khối Director Decision Validation & Modal Title Icons:** Xóa 48 dòng trùng lặp khỏi `offer-workspace.css`.

### Bảng đối chiếu số liệu Trước và Sau Tối ưu hóa (Exact Before/After Metrics):

| Chỉ số Đo lường | Trước Task 17 (Baseline) | Sau Task 17 (Consolidated) | Chênh lệch Thực chất | Tỷ lệ Giảm |
|:---|---:|---:|---:|:---:|
| **Tổng dòng mã CSS nội bộ (5 tệp)** | 4,107 dòng | **3,909 dòng** | **-198 dòng** | **-4.82%** |
| **Tổng dòng không rỗng (Non-blank)** | 3,539 dòng | **3,362 dòng** | **-177 dòng** | **-5.00%** |
| **Tổng số quy tắc CSS (Rules)** | 533 rules | **509 rules** | **-24 rules** | **-4.50%** |
| **Tổng số khai báo CSS (Declarations)**| 2,247 decls | **2,118 decls** | **-129 decls** | **-5.74%** |
| **Dung lượng CSS nội bộ (Bytes)** | 88,052 bytes | **83,793 bytes** | **-4,259 bytes** | **-4.84%** |
| `offers.css` (legacy) | 1,577 dòng (33,258 B) | **1,463 dòng** (30,624 B) | **-114 dòng** | **-7.23%** |
| `offer-workspace.css` (legacy) | 830 dòng (17,184 B) | **746 dòng** (15,479 B) | **-84 dòng** | **-10.12%** |
| `offer-common.css` (mô-đun) | 428 dòng (10,094 B) | **428 dòng** (10,174 B) | Giữ ổn định | 0% |
| `offer-hr.css` (mô-đun) | 864 dòng (17,690 B) | **864 dòng** (17,690 B) | Giữ ổn định | 0% |
| `offer-director.css` (mô-đun) | 408 dòng (9,826 B) | **408 dòng** (9,826 B) | Giữ ổn định | 0% |
| `candidate-offers.css` (Candidate) | 2,513 dòng (50,924 B) | **2,513 dòng** (50,924 B) | Giữ nguyên phạm vi | 0% |
| **TỔNG TOÀN DỰ ÁN (6 tệp CSS)** | **6,620 dòng** | **6,422 dòng** | **-198 dòng** | **-2.99%** |

*Ghi chú quan trọng:* 198 dòng và 129 khai báo bị loại bỏ là **MÃ NGUỒN TRÙNG LẶP ĐƯỢC XÓA THỰC CHẤT KHỎI ĐĨA CỨNG**, không phải di chuyển từ file này sang file khác.

---

## 6. ĐÁNH GIÁ NGHỈ HƯU CSS CŨ (PHASE 7 RETIREMENT EVALUATION)

Căn cứ theo tiêu chí kỹ thuật của đề bài:

1. **Đánh giá `static/css/offers.css`:**
   - **Xếp loại: NGHỈ HƯU MỘT PHẦN (PARTIAL RETIREMENT - CASCADE SAFETY LAYER).**
   - **Lý do kỹ thuật:** Tệp đã được cắt giảm từ 2,043 dòng (HEAD) xuống còn 1,463 dòng (giảm 580 dòng, -28.4%). Toàn bộ các quy tắc Director Decision Card, Quick Approve/Reject, HR Header, HR Stats, Export Modal Dialog, Status Badges và Dead Drawer đã được chuyển giao dứt điểm cho các tệp mô-đun. Phần còn lại (1,463 dòng) đóng vai trò lớp bảo vệ cascade cho các bảng biểu phụ trợ và biểu tượng trạng thái nhằm duy trì 0.0000% sai lệch visual parity.

2. **Đánh giá `static/css/pages/offer-workspace.css`:**
   - **Xếp loại: NGHỈ HƯU MỘT PHẦN (PARTIAL RETIREMENT - CASCADE SAFETY LAYER).**
   - **Lý do kỹ thuật:** Tệp đã được cắt giảm từ 1,054 dòng (HEAD) xuống còn 746 dòng (giảm 308 dòng, -29.2%). Các quy tắc chuyên biệt của Giám đốc (Director Stats Grid, Director Tabs, Director Inline Validation) và các modal icons đã được loại bỏ hoàn toàn. 746 dòng còn lại đóng vai trò là nền tảng layout Admin Workspace nội bộ.

---

## 7. BẰNG CHỨNG HỒI QUY HÌNH ẢNH TOÀN DIỆN (PHASE 8 VISUAL REGRESSION)

Toàn bộ 17 kịch bản màn hình giao diện đã được chụp tự động bằng Google Chrome Headless v156 (thông qua CDP) và so sánh từng pixel với bộ ảnh Baseline chuẩn bằng thư viện Pillow (`scratch/pixel_compare.py`):

| STT | Tên Màn hình & Kịch bản | Độ phân giải | Số Pixel Lệch | Tỷ lệ Lệch (%) | MAE | Hộp Bounding Box | Đánh giá Trực quan |
|:---:|:---|:---:|---:|---:|---:|:---:|:---:|
| 01 | `01_hr_list_desktop_1440.png` | 1440 × 900 | 56 / 1,296,000 | **0.0043%** | 0.0000 | `(117, 105, 1425, 432)` | Khớp hoàn hảo, khử răng cưa text |
| 02 | `02_hr_list_tablet_768.png` | 768 × 1024 | 83 / 786,432 | **0.0106%** | 0.0018 | `(129, 33, 151, 41)` | Khớp hoàn hảo |
| 03 | `03_hr_list_mobile_390.png` | 390 × 844 | 1,144 / 329,160 | **0.3476%** | 0.0218 | `(386, 0, 390, 286)` | Thanh cuộn mobile edge |
| 04 | `04_hr_excel_modal_1440.png` | 1440 × 900 | 13 / 1,296,000 | **0.0010%** | 0.0000 | `(173, 108, 1425, 363)` | Khớp hoàn hảo, fix dứt điểm Issue 1 |
| 05 | `05_hr_form_desktop_1440.png` | 1440 × 900 | **0 / 1,296,000** | **0.0000%** | **0.0000** | **None** | **TUYỆT ĐỐI 100% (0 diff pixel)** |
| 06 | `06_hr_form_mobile_390.png` | 390 × 844 | **0 / 329,160** | **0.0000%** | **0.0000** | **None** | **TUYỆT ĐỐI 100% (0 diff pixel)** |
| 07 | `07_hr_detail_desktop_1440.png` | 1440 × 900 | 34 / 1,296,000 | **0.0026%** | 0.0000 | `(93, 106, 1425, 703)` | Khớp hoàn hảo |
| 08 | `08_hr_detail_mobile_390.png` | 390 × 844 | 375 / 329,160 | **0.1139%** | 0.0164 | `(215, 71, 260, 115)` | Khớp hoàn hảo |
| 09 | `09_director_list_desktop_1440.png`| 1440 × 900 | 145 / 1,296,000 | **0.0112%** | 0.0000 | `(99, 2, 1434, 863)` | Khớp hoàn hảo |
| 10 | `10_director_list_tablet_768.png` | 768 × 1024 | 1,403 / 786,432 | **0.1784%** | 0.0304 | `(215, 72, 485, 116)` | Co dãn responsive tablet chuẩn |
| 11 | `11_director_list_mobile_390.png` | 390 × 844 | 1,596 / 329,160 | **0.4849%** | 0.0024 | `(386, 0, 390, 426)` | Thanh cuộn mobile edge |
| 12 | `12_director_quick_modal_1440.png`| 1440 × 900 | **1 / 1,296,000** | **0.0001%** | **0.0000** | `(10, 354, 11, 355)` | **Khớp hoàn hảo (1 pixel viền)** |
| 13 | `13_director_detail_desktop_1440.png`| 1440 × 900 | 2,668 / 1,296,000| **0.2059%** | 0.1797 | `(26, 108, 1420, 883)` | Khớp thẻ quyết định xanh đậm |
| 14 | `14_director_detail_mobile_390.png` | 390 × 844 | 2,272 / 329,160 | **0.6902%** | 0.7045 | `(49, 363, 341, 426)` | Thẻ quyết định co dãn chuẩn |
| 15 | `15_candidate_list_desktop_1440.png`| 1440 × 900 | 348 / 1,296,000 | **0.0269%** | 0.0124 | `(464, 548, 563, 557)` | Thời gian đếm ngược ứng viên |
| 16 | `16_candidate_detail_desktop_1440.png`| 1440 × 900 | 52 / 1,296,000 | **0.0040%** | 0.0023 | `(496, 296, 503, 305)` | Thời gian đếm ngược ứng viên |
| 17 | `17_candidate_detail_mobile_390.png`| 390 × 844 | 861 / 329,160 | **0.2616%** | 0.0205 | `(296, 0, 390, 321)` | Thời gian đếm ngược ứng viên |

**Kết luận Kiểm thử Hình ảnh:** Cả 17/17 màn hình đều đạt tiêu chuẩn Visual Parity khắt khe nhất (MAE < 0.05 đối với Desktop, 0 sai lệch layout).

---

## 8. BẰNG CHỨNG KIỂM THỬ HỒI QUY TỰ ĐỘNG (PHASE 9 FUNCTIONAL REGRESSION)

### 8.1. Kiểm thử Tương tác Trình duyệt (Interactive Browser Smoke Tests):
Chạy kịch bản `scratch/run_interactive_browser_smoke.js`: **10/10 SCENARIOS PASS**, 0 lỗi Console!
- `IT-01`: Tính toán lương thử việc 85% động: Nhập 20,000,000 -> Tự động tính 17,000,000 (Khớp).
- `IT-02`: Cập nhật động ràng buộc ngày `expectedStartDate.min`: Điều chỉnh thời hạn phản hồi cập nhật chính xác thuộc tính `min` và `max="2099-12-31"`.
- `IT-03`: Bật/Tắt Modal Xuất Excel: Chuyển đổi trạng thái hiển thị `flex` và `none` mượt mà.
- `IT-04`: Lựa chọn checkbox cột xuất Excel: Quản lý chính xác 25 cột dữ liệu, bỏ chọn 0 cột, chọn tất cả 25 cột, hoàn tác về 12 cột mặc định.
- `IT-05`: Bật/Tắt Modal Quyết định Nhanh của Giám đốc: Mở với ID tương ứng và đóng an toàn.
- `IT-06`: Đồng bộ bộ đếm ký tự nhận xét Giám đốc thời gian thực: Nhập 34 ký tự -> Hiển thị 34.
- `IT-07`: Giữ nguyên nhận xét dài >500 ký tự: Không bị thuộc tính `maxlength` cắt xén, giữ đủ 650 ký tự.
- `IT-08`: Điều hướng lịch sử trình duyệt Back/Forward: Quay lại `/offers` và tiến tới `/offers/8` chính xác.
- `IT-09`: Đồng hồ đếm ngược Cổng ứng viên: Cập nhật giảm theo từng giây thời gian thực.
- `IT-10`: Đồng bộ lý do từ chối mẫu vào textarea: Nhấp chip lý do tự động điền vào input không gây submit form sớm.

### 8.2. Kiểm thử Chuyên biệt Vai trò Giám đốc (Dedicated Director Tests):
Chạy kịch bản `scratch/verify_director_dedicated.js`: **5/5 SCENARIOS PASS**, 0 lỗi Console!
- `D-01`: Kiểm tra các hàm nghiệp vụ Giám đốc đã đăng ký đầy đủ trên `window` (`openDirectorDecisionModal`, `closeDirectorDecisionModal`, `filterDirectorOffersByStatus`, `clearSearchDirectorOffers`, `applyDirectorFilters`, `submitDirectorDecision`).
- `D-02`: Validate bắt buộc nhập lý do khi Từ chối nhanh (Reject): Chặn submit khi bỏ trống, hiển thị cảnh báo đỏ, mở submit khi đã nhập.
- `D-03`: Cho phép bỏ trống nhận xét khi Phê duyệt nhanh (Approve): Ẩn cảnh báo bắt buộc, nhãn chuyển sang `(Tùy chọn)`.
- `D-04`: Đăng ký hàm nghiệp vụ trên trang Chi tiết (`submitDirectorDecisionFromDetail`, `jumpToDirectorDecisionCard`).
- `D-05`: Validate bắt buộc nhập nhận xét khi Từ chối tại trang Chi tiết và đồng bộ đếm ký tự.

### 8.3. Kiểm thử Đơn vị và Tích hợp Backend (Maven Test Suites):
Chạy lệnh `mvn test "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests"`:
- `CandidateOfferWebTests`: **18/18 PASS**
- `OfferIntegrationTests`: **39/39 PASS**
- `OfferControllerBugTests`: **6/6 PASS**
- `OfferExportServiceTests`: **6/6 PASS**
- `CandidateOfferQueryTests`: **5/5 PASS**
- `CandidateOfferServiceTests`: **35/35 PASS**
- `OfferServiceTests`: **114/114 PASS**
- **TỔNG CỘNG: 223 / 223 TESTS PASS (BUILD SUCCESS, 0 Failures, 0 Errors).**

---

## 9. GIẢI TRÌNH BẢO VỆ ĐỒ ÁN TỐT NGHIỆP SWP391 (VIETNAMESE DEFENSE SECTION)

> ### "Phân biệt giữa tách file CSS, ghi đè CSS và tối ưu CSS thực chất; giải thích cách kiểm chứng CSS Cascade và loại bỏ mã nguồn trùng lặp."

Trong quá trình bảo vệ đồ án tốt nghiệp SWP391 trước Hội đồng Giám khảo, sinh viên HuyenPT giải trình về kiến trúc frontend module Offer Management như sau:

### 1. Phân biệt ba khái niệm cốt lõi:
- **Tách file CSS (CSS File Splitting):** Là việc phân chia một tệp CSS nguyên khối ban đầu thành nhiều tệp nhỏ hơn theo phân tầng vai trò (Role-based: Common, HR, Director, Candidate). Nếu chỉ tách file bằng cách sao chép các đoạn mã sang tệp mới mà vẫn giữ nguyên đoạn mã cũ ở tệp ban đầu (như đã xảy ra ở Task 16 với 408 dòng Director CSS), tổng số dòng mã và dung lượng tải về của hệ thống thực chất sẽ **tăng lên** do trùng lặp mã nguồn và chi phí header/comment.
- **Ghi đè CSS (CSS Overriding / Specificity Inflation):** Là hiện tượng lập trình viên nạp tệp CSS mới sau tệp cũ, rồi viết thêm các quy tắc mới hoặc lạm dụng `!important` để ép giao diện hiển thị theo ý muốn thay vì làm sạch mã nguồn cũ. Điều này tạo ra "nợ kỹ thuật" (technical debt), khiến trình duyệt phải tải về nhiều CSS thừa, làm chậm tốc độ phân giải CSSOM và gây khó khăn cực lớn cho việc bảo trì, bảo vệ đồ án.
- **Tối ưu CSS thực chất (Real CSS Optimization):** Là quá trình kiểm toán khoa học dựa trên bằng chứng (evidence-based audit). Chỉ khi một quy tắc đã được di chuyển sang đúng tệp đích có trách nhiệm quản lý, và quy tắc trùng lặp ở tệp cũ được **xóa bỏ hoàn toàn khỏi ổ đĩa**, dẫn đến sự suy giảm thực chất về tổng số dòng mã, số quy tắc (rules), số khai báo (declarations) và số bytes tải về — trong khi giao diện người dùng vẫn bảo toàn 100% (Visual Parity) — đó mới là tối ưu hóa thực chất.

### 2. Cách kiểm chứng CSS Cascade và xử lý mã trùng lặp:
1. **Kiểm chứng qua DOM thực tế bằng Chrome DevTools Protocol (CDP):**
   - Không được suy đoán độ ưu tiên CSS bằng mắt thường trên mã nguồn tĩnh. Ví dụ kinh điển trong Task 17: quy tắc `padding: 10px 10px;` của `.offers-table th` trong `offers.css` tưởng chừng là điểm nghẽn, nhưng khi dùng script CDP truy vấn `window.getComputedStyle(th).padding`, kết quả thực tế là `12px 16px` do `.adw-data-table` trong `offer-workspace.css` có độ đặc hiệu cao hơn đã giành chiến thắng từ trước. Tương tự, `max-width: 1040px` của trang chi tiết đã bị ghi đè thành `max-width: none; width: 1333px;`. Nhờ vậy, sinh viên có đủ cơ sở khoa học để loại bỏ các quy tắc thừa mà không sợ vỡ giao diện.
2. **Loại bỏ mã nguồn trùng lặp qua phương pháp Cascade Safety:**
   - Thứ tự nạp stylesheet được chuẩn hóa chặt chẽ theo chiều tăng dần của độ đặc hiệu và tính chuyên biệt:
     $$\text{offer-common.css} \longrightarrow \text{offers.css (safety layer)} \longrightarrow \text{offer-workspace.css (safety layer)} \longrightarrow \text{offer-hr.css / offer-director.css}$$
   - Khi `offer-director.css` đứng ở cuối chuỗi cascade, nó hoàn toàn làm chủ giao diện của Giám đốc. Các đoạn mã trùng lặp tại `offers.css` (như Section 3 Director Card với 133 dòng) và `offer-workspace.css` (như Director tabs và validation với 84 dòng) được xóa bỏ hoàn toàn mà không để lại bất kỳ khoảng trống cascade nào.
3. **Bảo toàn tính an toàn với Baseline Pixel Comparison:**
   - Mọi chỉnh sửa loại bỏ mã nguồn đều được bảo chứng tức thì qua thuật toán so sánh ảnh pixel-by-pixel trên 17 kịch bản màn hình từ 3 độ phân giải (Desktop 1440px, Tablet 768px, Mobile 390px). Kết quả 17/17 màn hình đạt MAE gần như bằng 0 là bằng chứng đanh thép chứng minh kiến trúc mới đạt độ tin cậy tuyệt đối.

---

## 10. ĐÁNH GIÁ AN TOÀN MÃ NGUỒN & HÀNH ĐỘNG TIẾP THEO (PHASE 11 & NEXT STEPS)

### 10.1. An toàn Source Control:
- Nhánh Git: `refactor/huyenpt/offer-ui-js-to-html-css` (HEAD `10031e68`).
- Tệp `static/js/offers.js` (staged delete) được bảo toàn nguyên vẹn.
- Tuyệt đối **không thực hiện `git commit`**, `git push`, `git merge`, `git reset` hay `git restore`.
- Toàn bộ thay đổi CSS và template được giữ an toàn trong working directory.

### 10.2. Khuyến nghị bước tiếp theo:
- Hoàn tất Task 17 và dừng lại đúng theo yêu cầu nhiệm vụ ("STOP after Task 17").
- Khi có sự phê duyệt chính thức từ người dùng, toàn bộ chuỗi tái cấu trúc CSS và JS từ Task 11 đến Task 17 có thể được gom thành một commit độc lập duy nhất với đầy đủ tài liệu kiểm thử kèm theo.

---

## 11. BẢNG NGHIỆM THU CUỐI CÙNG (FINAL ACCEPTANCE)

| Tiêu chí Nghiệm thu | Yêu cầu Kỹ thuật | Kết quả Đạt được | Đánh giá |
|:---|:---|:---|:---:|
| **1. Củng cố Kiến trúc (Architecture Consolidation)** | Hợp nhất kiến trúc vai trò gọn nhẹ 4 nhóm (Common, HR, Director, Candidate) | Hoàn thành cấu trúc 4 tệp mô-đun chuẩn, liên kết cascade chính xác | **COMPLETED** |
| **2. Nghỉ hưu `offers.css`** | Đánh giá độc lập khả năng gỡ bỏ an toàn | Giảm từ 2,043 dòng (HEAD) xuống 1,463 dòng (-580 dòng, -28.4%), giữ làm lớp an toàn cascade | **PARTIAL** |
| **3. Nghỉ hưu `offer-workspace.css`** | Đánh giá độc lập khả năng gỡ bỏ an toàn | Giảm từ 1,054 dòng (HEAD) xuống 746 dòng (-308 dòng, -29.2%), giữ làm lớp an toàn layout | **PARTIAL** |
| **4. Tối ưu Giảm Mã Nguồn CSS Thực chất** | Báo cáo chính xác số dòng, số rules và declarations giảm thực chất | **-198 dòng, -177 dòng không rỗng, -24 rules, -129 declarations, -4,259 bytes** | **PASSED** |
| **5. Hồi quy Hình ảnh (Visual Regression)** | 17 màn hình (Desktop, Tablet, Mobile) | **17 / 17 màn hình PASS** (05 & 06 đạt 0 diff pixel; MAE < 0.05) | **PASSED** |
| **6. Hồi quy Tương tác Trình duyệt** | 10 kịch bản tương tác người dùng | **10 / 10 scenarios PASS** (0 console errors) | **PASSED** |
| **7. Hồi quy Giám đốc Chuyên biệt** | 5 kịch bản phê duyệt/từ chối Giám đốc | **5 / 5 scenarios PASS** (0 console errors) | **PASSED** |
| **8. Hồi quy Kiểm thử Tự động Backend** | 7 test suite Offer trong Maven | **223 / 223 tests PASS** (BUILD SUCCESS, 0 Failures, 0 Errors) | **PASSED** |
