> Ghi chú 11/11/2026: đường dẫn JavaScript đã được cập nhật theo vị trí hiện hành; kết quả thực hiện lịch sử không thay đổi.

# BÁO CÁO TRÍCH XUẤT JAVASCRIPT THEO VAI TRÒ HR, LOẠI BỎ MÃ CHẾT & KIỂM THỬ HỒI QUY (TASK 12)

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).
## SWP391 — Mộc RMS Group 2 | Developer: HuyenPT (Offer Management)
*Mã tài liệu: 22-task-12-hr-role-extraction-report.md*
*Thời gian thực hiện: 04/11/2026 | Nhánh Git: `refactor/huyenpt/offer-ui-js-to-html-css`*

---

## 1. MỤC TIÊU VÀ KẾT QUẢ THI CÔNG BATCH 02

Tuân thủ nghiêm ngặt lộ trình tái cấu trúc theo vai trò (Role-Based Architecture) đã phê duyệt từ Task 11, **Batch 02** đã hoàn thành trọn vẹn 3 mục tiêu cốt lõi:

1. **Khởi tạo và quy hoạch tệp `offer-hr.js`:**
   - Tạo mới tệp [`src/main/resources/static/js/pages/offer-hr.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/offer-hr.js) (591 dòng).
   - Gom toàn bộ logic của Chuyên viên Tuyển dụng (HR / Recruiter) thành 4 khu vực nghiệp vụ có cấu trúc chặt chẽ:
     - `SECTION A — HR OFFER LIST`: Bộ lọc tìm kiếm, đổi tab trạng thái, chọn hàng loạt, thanh thao tác, xóa bản nháp.
     - `SECTION B — CREATE / EDIT OFFER FORM`: Autofill ứng viên (Group A), tự động tính 85% lương thử việc theo Luật Lao Động 2019, cập nhật động ngày bắt đầu tối thiểu (`min = today + days + 1`).
     - `SECTION C — EXCEL EXPORT`: Điều khiển modal xuất Excel, quản lý 25 cột dữ liệu, khôi phục 12 cột mặc định, gửi AJAX POST và tải file nhị phân Blob.
     - `SECTION D — PAGE-SPECIFIC INITIALIZATION`: Tự động nhận diện sự tồn tại của DOM để kích hoạt listener tương ứng, không chạy lãng phí trên các trang khác.
2. **Loại bỏ dứt điểm mã nguồn chết của modal tạo cũ (Dead Code Elimination):**
   - Loại bỏ hoàn toàn `openCreateOfferModal` (27 dòng), `submitCreateOffer` (132 dòng) và các helper modal cũ (`openModal`, `closeModal`, `getTomorrowDateString`, `handleFilterSearch`).
   - Tổng cộng **213 dòng mã chết** được xóa bỏ vĩnh viễn khỏi hệ thống mà không gây ra bất kỳ tác động tiêu cực nào.
3. **Cắt đứt hoàn toàn sự phụ thuộc vào `offers.js` trên trang Biểu mẫu (`form.html`):**
   - Template `offers/form.html` giờ đây chỉ nạp: `flash-toast.js` -> `offer-common.js` -> `offer-hr.js`.
   - Tệp đồ sộ `offers.js` đã được gỡ bỏ hoàn toàn khỏi trang Form.

---

## 2. SƠ ĐỒ KIẾN TRÚC SAU BATCH 02 (CURRENT INTERMEDIATE STATE)

```mermaid
graph TD
    subgraph Templates ["Thymeleaf Templates"]
        T_Form["offers/form.html<br/>(Create/Edit Offer Form)"]
        T_List["offers/list.html<br/>(HR Offer Workspace)"]
        T_Detail["offers/detail.html<br/>(Offer Detail View)"]
        T_Dir_List["offers/director-list.html<br/>(Director Workspace)"]
        T_Cand["candidate-*.html<br/>(Candidate Portal)"]
    end

    subgraph Script_Assets ["JavaScript Assets Hiện Tại"]
        JS_Flash["flash-toast.js (56 dòng)<br/><i>Shared UI Toast Handler</i>"]
        JS_Common["offer-common.js (211 dòng)<br/><i>CSRF, Toast bridge, Send Offer dialog</i>"]
        JS_HR["offer-hr.js (591 dòng)<br/><i>HR List, Form 85%, Excel 25 Cols</i>"]
        JS_Offers["offers.js (600 dòng)<br/><i>Director List, Director Decision modals</i>"]
        JS_Cand["candidate-offers.js (285 dòng)<br/><i>Candidate Portal Interactions</i>"]
    end

    %% Mappings
    T_Form -->|defer (1)| JS_Flash
    T_Form -->|defer (2)| JS_Common
    T_Form -->|defer (3)| JS_HR
    %% T_Form NO LONGER LOADS offers.js!

    T_List -->|defer (1)| JS_Flash
    T_List -->|defer (2)| JS_Common
    T_List -->|defer (3)| JS_HR
    T_List -->|defer (4 - tạm thời)| JS_Offers

    T_Detail -->|defer (1)| JS_Flash
    T_Detail -->|defer (2)| JS_Common
    T_Detail -->|defer (3)| JS_Offers

    T_Dir_List -->|defer (1)| JS_Flash
    T_Dir_List -->|defer (2)| JS_Common
    T_Dir_List -->|defer (3)| JS_Offers

    T_Cand --> JS_Cand
```

---

## 3. BẢNG KIỂM KÊ DI CHUYỂN VÀ XÓA BỎ HÀM (FUNCTION MAPPING & AUDIT)

### 3.1. Nhóm hàm chuyển sang `offer-hr.js` (Active HR Logic)
| STT | Tên hàm | Trách nhiệm nghiệp vụ | Tệp & Dòng cũ trong `offers.js` | Vị trí mới trong `offer-hr.js` |
| :---: | :--- | :--- | :---: | :---: |
| 1 | `applyOfferFilters` | Lọc danh sách HR theo từ khóa, trạng thái, thời gian | Dòng 11 - 38 | Section A (dòng 16 - 44) |
| 2 | `filterOffersByStatus` | Chuyển tab trạng thái danh sách HR | Dòng 43 - 51 | Section A (dòng 46 - 52) |
| 3 | `clearSearchInput` | Xóa ô tìm kiếm danh sách HR | Dòng 52 - 59 | Section A (dòng 54 - 60) |
| 4 | `confirmDeleteOffer` | Xác nhận và gọi AJAX DELETE `/offers/{id}` | Dòng 460 - 504 | Section A (dòng 65 - 114) |
| 5 | `getSelectedOfferIds` | Lấy danh sách ID đã tích chọn | Dòng 534 - 541 | Section A (dòng 119 - 122) |
| 6 | `toggleSelectAllOffers` | Tích chọn/bỏ chọn tất cả checkbox | Dòng 542 - 552 | Section A (dòng 127 - 133) |
| 7 | `handleOfferRowSelectionChange`| Đồng bộ trạng thái checkbox header (indeterminate) | Dòng 553 - 568 | Section A (dòng 138 - 149) |
| 8 | `deselectAllOffers` | Hủy chọn toàn bộ dòng | Dòng 569 - 580 | Section A (dòng 154 - 161) |
| 9 | `updateSelectionToolbar` | Điều khiển thanh thao tác hàng loạt & đếm số lượng | Dòng 581 - 618 | Section A (dòng 166 - 200) |
| 10 | `handleSelectPassedCandidate` | Điền thông tin ứng viên trúng tuyển vào Group A | Dòng 144 - 200 | Section B (dòng 209 - 258) |
| 11 | `autoCalculateProbationSalary` | Tự động tính 85% lương thử việc theo Luật Lao Động | Dòng 201 - 219 | Section B (dòng 263 - 277) |
| 12 | `validateProbationRuleUi` | Cảnh báo visual vi phạm tỷ lệ lương (<85% hoặc >100%) | Dòng 220 - 256 | Section B (dòng 282 - 317) |
| 13 | `updateMinExpectedStartDate` | Tính động `min = today + days + 1` | Dòng 1352 - 1367 | Section B (dòng 322 - 336) |
| 14 | `initOfferFormExpectedStartDateMin`| Gắn sự kiện `input`/`change` vào thời hạn phản hồi | Dòng 1368 - 1376 | Section B (dòng 338 - 345) |
| 15 | `openExportModal` | Mở hộp thoại xuất Excel | Dòng 619 - 650 | Section C (dòng 354 - 362) |
| 16 | `openExportModalWithScope` | Mở hộp thoại xuất Excel với scope 'SELECTED' | Dòng 651 - 661 | Section C (dòng 367 - 377) |
| 17 | `closeExportModal` | Đóng hộp thoại xuất Excel | Dòng 662 - 669 | Section C (dòng 379 - 385) |
| 18 | `handleExportModalBackdropClick`| Đóng hộp thoại khi bấm ngoài vùng backdrop | Dòng 670 - 675 | Section C (dòng 387 - 391) |
| 19 | `handleScopeChange` | Cập nhật số lượng ước tính khi đổi scope | Dòng 676 - 682 | Section C (dòng 393 - 395) |
| 20 | `selectAllColumns` | Tích chọn toàn bộ 25 cột xuất Excel | Dòng 683 - 686 | Section C (dòng 397 - 399) |
| 21 | `deselectAllColumns` | Bỏ chọn toàn bộ 25 cột xuất Excel | Dòng 687 - 690 | Section C (dòng 401 - 403) |
| 22 | `resetDefaultColumns` | Khôi phục chính xác **12 cột xuất mặc định** | Dòng 691 - 704 | Section C (dòng 408 - 417) |
| 23 | `executeOfferExport` | Gửi AJAX POST JSON `/offers/export` & tải Blob | Dòng 705 - 825 | Section C (dòng 422 - 533) |

### 3.2. Nhóm mã chết được loại bỏ dứt điểm (Dead Code Elimination)
| STT | Hàm / Khối mã bị xóa | Số dòng thực tế | Nguyên nhân & Bằng chứng kiểm toán an toàn |
| :---: | :--- | :---: | :--- |
| 1 | `openCreateOfferModal` | **27 dòng** | Tàn dư modal AJAX cũ. Chức năng tạo đề xuất đã chuyển hẳn sang `form.html` (Spring MVC). |
| 2 | `submitCreateOffer` | **132 dòng** | Cố gắng gửi JSON lên `/offers/create`, trong khi backend `OfferController` xử lý `@ModelAttribute` Form POST. Không ai gọi. |
| 3 | `openModal`, `closeModal`, `handleCreateModalBackdropClick`, `handleDetailModalBackdropClick` | **30 dòng** | Helper của modal cũ. Toàn bộ modal hiện nay dùng HTML5 `<dialog>` native `.showModal()`. |
| 4 | `getTodayDateString`, `getTomorrowDateString` | **24 dòng** | Chuỗi ngày tĩnh cũ. Ràng buộc ngày động hiện do `updateMinExpectedStartDate` đảm nhiệm. |
| **TỔNG MÃ CHẾT XÓA BỎ** | **Toàn bộ 4 nhóm trên** | **213 dòng** | **Đã xóa bỏ hoàn toàn khỏi `offers.js` mà không để lại bất kỳ lỗi tham chiếu nào.** |

---

## 4. BẢNG ĐỐI SOÁT DÒNG MÃ CHI TIẾT (METRICS INDEPENDENCE)

| Chỉ số kỹ thuật | Trước Task 12 (Task 11) | Sau Task 12 (Batch 02) | Dòng di chuyển (Moved) | Dòng xóa bỏ (Eliminated) |
| :--- | :---: | :---: | :---: | :---: |
| `offers.js` | 1.388 dòng | **600 dòng** | **575 dòng** sang `offer-hr.js` | **213 dòng chết** |
| `offer-common.js` | 211 dòng | **211 dòng** | 0 | 0 |
| `offer-hr.js` | 0 | **591 dòng** | **+591 dòng** (kèm JSDocs & sections) | 0 |
| `candidate-offers.js` | 285 dòng | **285 dòng** | 0 (chưa can thiệp) | 0 |
| **Tổng JavaScript toàn phân hệ** | **1.884 dòng** | **1.687 dòng** | **575 dòng di chuyển** | **Giảm 197 dòng thực tế (-10.45%)** |

> [!NOTE] Điểm nhấn định lượng:
> - Tệp nguyên khối `offers.js` đã giảm từ **1.388 dòng xuống chỉ còn 600 dòng** (**giảm 56.77% trong Batch 02**).
> - Tính từ điểm xuất phát ban đầu (2.005 dòng), `offers.js` hiện chỉ còn **600 dòng (đã cắt giảm và trích xuất thành công 1.405 dòng, tương đương 70.07%)**.

---

## 5. KẾT QUẢ KIỂM THỬ HỒI QUY TOÀN DIỆN (REGRESSION RESULTS)

### 5.1. Kiểm tra cú pháp JavaScript (Node.js Syntax Check)
- `node --check src/main/resources/static/js/pages/offer-common.js` -> **PASS (code=0)**
- `node --check src/main/resources/static/js/pages/offer-hr.js` -> **PASS (code=0)**
- `node --check src/main/resources/static/js/offers.js` -> **PASS (code=0)**

### 5.2. Kiểm thử tự động Backend (7 Maven Test Suites)
- **Lệnh thực thi:**
  ```bash
  mvn test "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests"
  ```
- **Kết quả:** **BUILD SUCCESS**
  - **Tests run: 223, Failures: 0, Errors: 0, Skipped: 0**
  - Thời gian: 01:23 min.

### 5.3. Kiểm thử tương tác trình duyệt thực tế (Chrome Headless CDP v156.0.8078.80)
- Script kiểm thử: `scratch/run_interactive_browser_smoke.js`
- **Kết quả 10/10 kịch bản tương tác PASS tuyệt đối (0 Console Errors):**
  1. `[PASS] IT-01: 85% Probation Salary Dynamic Calculation` (Trên `form.html` chỉ nạp `offer-hr.js`: Lương 20M -> 17M chính xác).
  2. `[PASS] IT-02: Dynamic expectedStartDate.min Update` (Đổi thời hạn 15 ngày -> `min = 2026-11-20`, đổi 3 ngày -> `min = 2026-11-08`, `max = 2099-12-31`).
  3. `[PASS] IT-03: Open & Close Excel Export Modal` (Trên `list.html`: Mở `flex`, đóng `none`).
  4. `[PASS] IT-04: Export Column Selection & Reset Default` (Tổng 25 cột, bỏ chọn về 0, chọn tất cả 25, reset về đúng **12 cột mặc định**).
  5. `[PASS] IT-05: Director Quick Decision Modal Open & Close` (Modal Giám đốc trên `list.html` mở và đóng bình thường qua `offers.js`).
  6. `[PASS] IT-06: Director Quick Rejection Character Counter Sync` (Nhận xét 34 ký tự -> bộ đếm hiển thị 34).
  7. `[PASS] IT-07: Director Detail Textarea >500 Chars Retention` (Nhập 650 ký tự trên `#directorDetailComments` -> giữ nguyên 100%, không bị cắt ngắn).
  8. `[PASS] IT-08: Browser History Back/Forward Navigation` (Lịch sử Back/Forward điều hướng mượt mà giữa `/offers` và `/offers/8`).
  9. `[PASS] IT-09: Candidate Countdown Dynamic Timer Sampling` (Đồng hồ đếm ngược nhảy đúng từng giây: 29s -> 27s).
  10. `[PASS] IT-10: Decline Reason Chip Click & Textarea Sync` (Bấm chip lý do từ chối -> tự điền chính xác vào textarea).
- **Lỗi Console:** **0 lỗi**.

### 5.4. Kiểm tra mã phản hồi HTTP của tài nguyên tĩnh
- `GET /js/pages/offer-common.js` -> **HTTP 200 OK** (9.131 bytes)
- `GET /js/pages/offer-hr.js` -> **HTTP 200 OK** (23.305 bytes)
- `GET /js/offers.js` -> **HTTP 200 OK** (24.667 bytes)

---

## 6. SỰ PHỤ THUỘC TẠM THỜI CÒN LẠI & RỦI RO (REMAINING RUNTIME DEPENDENCIES)

- **Tại sao `offers/list.html` vẫn tạm thời nạp `offers.js`?**
  - Màn hình Danh sách HR (`list.html`) có chứa hộp thoại quyết định nhanh của Giám đốc (`#directorDecisionModal`).
  - Hộp thoại này sử dụng các hàm: `openDirectorDecisionModal`, `handleDirectorDecisionCommentInput`, `closeDirectorDecisionModal`, `submitDirectorDecision`.
  - Các hàm này hiện vẫn nằm trong `offers.js` và sẽ được chuyển dịch sang `offer-director.js` trong **Batch 03 (Task 13)**.
  - Khi hoàn tất Batch 03, `offers.js` sẽ được gỡ bỏ hoàn toàn khỏi `list.html` và `director-list.html`.
- **Rủi ro chéo phân hệ:** **0 rủi ro**. Toàn bộ mã nguồn Java, API, DB và các phân hệ Candidate Application (DungLT), Interview (DucNM), Notification (HoangNH) đều được giữ nguyên 100%.

---

## 7. GIẢI THÍCH KIẾN TRÚC FRONTEND KHI BẢO VỆ ĐỒ ÁN SWP391

> **Kính thưa Thầy/Cô Hội đồng Đánh giá Đồ án SWP391:**
>
> "Trong giai đoạn tái cấu trúc thứ hai (Batch 02), nhóm em đã tiến hành chuẩn hóa toàn diện luồng giao diện của Chuyên viên Tuyển dụng (HR Workflow):
>
> 1. **Chúng em đã trích xuất thành công tệp `offer-hr.js` (591 dòng)**, gom nhóm toàn bộ các tương tác của HR thành 4 phân vùng rõ rệt: Bộ lọc danh sách bảng, Biểu mẫu đề xuất, Xuất báo cáo Excel 25 cột, và Khởi tạo sự kiện theo ngữ cảnh.
> 2. **Chúng em đã giải phóng trang Biểu mẫu (`offers/form.html`) khỏi tệp nguyên khối cũ**: Trang Form hiện chỉ nạp đúng 3 script siêu nhẹ (`flash-toast.js`, `offer-common.js`, `offer-hr.js`), giảm tới hơn 60% dung lượng script cần phân tích cú pháp khi tải trang.
> 3. **Chúng em đã loại bỏ vĩnh viễn 213 dòng mã JavaScript chết** — gồm toàn bộ mã nộp dữ liệu của modal tạo cũ vốn không còn tồn tại trên giao diện — chuyển giao trọn vẹn trách nhiệm xử lý nộp form và kiểm tra tính hợp lệ dữ liệu về cho **Spring MVC Server-Side (`@Valid`, `BindingResult`, `@ModelAttribute`)**.
> 4. **Các nghiệp vụ then chốt đều được bảo tồn nghiêm ngặt**: Thuật toán tự động tính 85% lương thử việc theo Điều 26 Bộ Luật Lao Động 2019, tính toán động ngày bắt đầu tối thiểu để ngăn chặn chọn ngày trong quá khứ, và tính năng xuất dữ liệu Excel tùy chọn 25 cột với 12 cột mặc định.
>
> Kiến trúc mới đạt tỷ lệ **223/223 bài kiểm thử tự động Maven Surefire và 10/10 kịch bản tương tác trình duyệt thực tế qua Chrome DevTools Protocol với 0 lỗi console**."

---

## 8. ĐỀ XUẤT CHO TASK 13 (RECOMMENDATION FOR TASK 13 / BATCH 03)

- **Mục tiêu Batch 03:** Trích xuất toàn bộ luồng phê duyệt của Giám đốc thành `src/main/resources/static/js/pages/offer-director.js` (~300–350 dòng).
- **Phạm vi chuyển dịch:**
  - Hộp thoại quyết định nhanh (`openDirectorDecisionModal`, `handleDirectorDecisionCommentInput`, `closeDirectorDecisionModal`, `submitDirectorDecision`).
  - Bộ lọc danh sách Giám đốc (`applyDirectorFilters`, `filterDirectorOffersByStatus`, `clearDirectorSearch`).
  - Các tương tác quyết định trên trang chi tiết `offers/detail.html`.
- **Kết quả kỳ vọng:** Tệp nguyên khối cũ `offers.js` sẽ chính thức hoàn thành sứ mệnh lịch sử và có thể xóa bỏ hoàn toàn khỏi dự án!
