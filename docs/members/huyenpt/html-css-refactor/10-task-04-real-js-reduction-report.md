# BÁO CÁO THỰC THI TASK 04 — CẮT GIẢM JAVASCRIPT THỰC TẾ & BẢO TOÀN NGHIỆP VỤ
**Nhiệm vụ:** Task 04 — Real JavaScript Reduction with Zero Functional Regression  
**Dự án:** SWP391 — Recruitment Management System (RMS) — Group 2  
**Phân hệ:** Quản lý Đề xuất & Thư mời nhận việc (Offer Management)  
**Nhà phát triển phụ trách:** HuyenPT  
**Thời điểm thực hiện:** 04/11/2026  
**Nhánh Git:** `refactor/huyenpt/offer-ui-js-to-html-css`  
**Commit cơ sở:** `5e4e1fd8cc72deabb38e53f009bfbe76f1a1a53a`  
**Trạng thái nghiệm thu:** **HOÀN THÀNH THÀNH CÔNG (COMPLETED & VERIFIED)**  

---

## 1. Thống kê Số dòng Mã nguồn JavaScript Ban đầu (Baseline Line Counts)

| Tệp tin JavaScript | Đường dẫn lưu trữ | Số dòng ban đầu | Số hàm | Vai trò & Phạm vi |
| :--- | :--- | :---: | :---: | :--- |
| `offers.js` | `src/main/resources/static/js/offers.js` | **2,005** | 77 | Logic giao diện Quản lý Offer cho HR và Giám đốc |
| `candidate-offers.js` | `src/main/resources/static/js/pages/candidate-offers.js` | **285** | 5 | Đếm ngược thời gian phản hồi và chip lý do ứng viên |
| **Tổng cộng** | **Phân hệ Offer Management** | **2,290** | **82** |  |

---

## 2. Khảo sát Toàn diện Các Ứng viên Chuyển đổi (Phase 1 & 2 Candidates)

Qua quá trình rà soát toàn bộ 82 hàm JavaScript và đối chiếu với các template HTML của phân hệ Offer, 6 ứng viên có tiềm năng chuyển đổi/cắt giảm đã được thẩm định:

### Ứng viên 1: Hàm Điều hướng Dư thừa `viewOfferDetail(offerId)` (CHỌN THỰC THI)
* **Tệp tin & Dòng mã:** `src/main/resources/static/js/offers.js`, dòng 735–738.
* **Hành vi hiện tại:** Nhận `offerId` và thực thi chuyển hướng client-side: `window.location.href = '/offers/' + offerId`.
* **Phụ thuộc hiện có:** **0 phụ thuộc**. Kiểm tra toàn bộ template HTML và script JS, không có bất kỳ nút bấm hay phần tử nào gọi hàm này.
* **Giải pháp HTML/Thymeleaf thay thế:** Các liên kết chuyển trang xem chi tiết đã được chuyển đổi hoàn toàn sang thẻ liên kết ngữ nghĩa native:
  ```html
  <a th:href="@{/offers/{id}(id=${o.offerId})}" class="offer-id-link">...</a>
  <a th:href="@{/offers/{id}(id=${o.offerId})}" class="btn-icon ofw-action-btn" title="Xem chi tiết">...</a>
  ```
* **Số dòng JS cắt giảm được:** **4 dòng mã + 1 dòng trống (5 dòng)**.
* **Sai khác hành vi:** **Không có (Zero)**. Hành vi điều hướng bằng thẻ `<a>` chuẩn SEO và trợ năng hơn.
* **Phân loại:** **SAFE (An toàn tuyệt đối)**.

### Ứng viên 2: Gán Thuộc tính Ngày tối đa Dư thừa `startDateInput.max = '2099-12-31'`
* **Tệp tin & Dòng mã:** `src/main/resources/static/js/offers.js`, dòng 1982 trong hàm `updateMinExpectedStartDate()`.
* **Hành vi hiện tại:** Mỗi lần người dùng thay đổi thời hạn phản hồi, JS lại gán thuộc tính `max` cho ô ngày bắt đầu.
* **Giải pháp HTML native:** Thẻ `<input type="date" id="expectedStartDate">` trong `offers/form.html` (dòng 246) đã có sẵn thuộc tính native `max="2099-12-31"`.
* **Số dòng JS cắt giảm được:** 1 dòng.
* **Phân loại:** **SAFE**.

### Ứng viên 3: Khối Dựng DOM HTML Thừa của Modal Chi tiết Cũ (`renderDetailModalHtml`, `renderDetailFooterHtml`)
* **Tệp tin & Dòng mã:** `src/main/resources/static/js/offers.js`, dòng 534–734 (201 dòng).
* **Hành vi hiện tại:** Lắp ráp chuỗi HTML khổng lồ bằng template literal để hiển thị chi tiết Offer trong popup modal cũ.
* **Giải pháp thay thế hiện hữu:** Màn hình chi tiết đã chuyển sang trang server-side Thymeleaf độc lập `src/main/resources/templates/offers/detail.html`. Toàn bộ modal cũ và container `#offerDetailModalContent` đã bị xóa khỏi HTML.
* **Số dòng JS cắt giảm được:** 201 dòng.
* **Phân loại:** **SAFE (Dead Code Elimination)**.

### Ứng viên 4: Khối Xử lý Modal Drawer Giám đốc Cũ (`openDirectorDetailModal`, `closeDirectorDetailModal`)
* **Tệp tin & Dòng mã:** `src/main/resources/static/js/offers.js`, dòng 1446–1675 (230 dòng).
* **Hành vi hiện tại:** Gọi AJAX nạp dữ liệu vào modal `#directorDetailModal`.
* **Giải pháp thay thế hiện hữu:** Trang `offers/detail.html` độc lập của Giám đốc. Modal này không còn tồn tại trong template.
* **Số dòng JS cắt giảm được:** 230 dòng.
* **Phân loại:** **SAFE (Dead Code Elimination)**.

### Ứng viên 5: Nút Bấm Thẻ KPI Điều hướng Lọc (`filterOffersByStatus`, `filterDirectorOffersByStatus`)
* **Tệp tin & Dòng mã:** `offers/list.html` và `offers/director-list.html`.
* **Hành vi hiện tại:** Thẻ `<button onclick="filterOffersByStatus(...)">` đọc trạng thái form, cập nhật URL param và reload.
* **Giải pháp HTML đề xuất:** Đổi thành thẻ `<a th:href="@{/offers(status='...')}">`.
* **Rủi ro hành vi:** Nếu người dùng đã gõ từ khóa `search` hoặc chọn `timeSort`, bấm link tĩnh sẽ làm mất các tham số này trừ khi Thymeleaf nhúng đầy đủ tham số động.
* **Phân loại:** **CONDITIONAL**.

### Ứng viên 6: Nút Cuộn trang Nhanh `scrollToDirectorDecision()`
* **Tệp tin & Dòng mã:** `offers/detail.html` dòng 90 và `offers.js` dòng 1697–1704.
* **Hành vi hiện tại:** Cuộn trang tới phần phê duyệt và tự động focus vào textarea `#directorDetailComments`.
* **Giải pháp HTML đề xuất:** Thẻ `<a href="#directorDecisionSection">`.
* **Sai khác hành vi:** Neo HTML chỉ cuộn trang chứ không tự động focus con trỏ vào ô nhập liệu textarea.
* **Phân loại:** **CONDITIONAL**.

---

## 3. Thực thi Tái cấu trúc Có Kiểm soát (Phase 3 — Controlled Refactoring)

Theo nguyên tắc: *"Chọn chính xác 01 ứng viên SAFE nhỏ nhất, có bằng chứng tương đương hành vi rõ ràng nhất"*, **Ứng viên 1 (`viewOfferDetail`)** đã được lựa chọn thực thi.

### 3.1. Tệp tin Tác động Duy nhất:
[`src/main/resources/static/js/offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js)

### 3.2. So sánh Trước và Sau khi Sửa đổi (Before / After Git Diff):
```diff
diff --git a/src/main/resources/static/js/offers.js b/src/main/resources/static/js/offers.js
index 01339724..7cfb1dff 100644
--- a/src/main/resources/static/js/offers.js
+++ b/src/main/resources/static/js/offers.js
@@ -732,11 +732,6 @@ function renderDetailFooterHtml(d) {
     return btns;
 }
 
-function viewOfferDetail(offerId) {
-    if (!offerId) return;
-    window.location.href = `/offers/${offerId}`;
-}
-
 // =========================================================================
 // 3.2. POP-UP MODAL CHỈNH SỬA OFFER (Screen 30/32 Edit Modal) - Đã xóa, chuyển sang trang chỉnh sửa trực tiếp
 // =========================================================================
```

### 3.3. Số dòng Mã nguồn JavaScript Đã Cắt giảm:
* Số dòng trước khi sửa: **2,005 dòng**.
* Số dòng sau khi sửa: **2,000 dòng**.
* **Thực giảm:** **5 dòng (4 dòng mã nguồn + 1 dòng khoảng trắng phân tách)**.

---

## 4. Kết quả Kiểm thử Hồi quy Toàn diện (Phase 4 — Regression Verification)

Lệnh thực thi kiểm thử hồi quy:
```powershell
mvn test "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests"
```

**Bảng Tổng hợp Kết quả Kiểm thử:**

| STT | Bộ kiểm thử (Test Suite) | Số test | Pass | Fail | Error | Skip | Thời gian chạy | Trạng thái |
| :---: | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| 1 | `CandidateOfferWebTests` | 19 | 19 | 0 | 0 | 0 | 12.18 s | **PASS** |
| 2 | `OfferControllerBugTests` | 6 | 6 | 0 | 0 | 0 | 17.15 s | **PASS** |
| 3 | `OfferExportServiceTests` | 10 | 10 | 0 | 0 | 0 | 2.15 s | **PASS** |
| 4 | `OfferIntegrationTests` | 34 | 34 | 0 | 0 | 0 | 2.45 s | **PASS** |
| 5 | `CandidateOfferQueryTests` | 5 | 5 | 0 | 0 | 0 | 2.11 s | **PASS** |
| 6 | `CandidateOfferServiceTests` | 35 | 35 | 0 | 0 | 0 | 0.18 s | **PASS** |
| 7 | `OfferServiceTests` | 114 | 114 | 0 | 0 | 0 | 0.57 s | **PASS** |
| **Tổng** | **7 Bộ kiểm thử Offer** | **223** | **223** | **0** | **0** | **0** | **50.81 s** | **100% PASS** |

### Đánh giá Tính tương đương Hành vi Giao diện:
* **Hiển thị trực quan (Visual Appearance):** 100% giữ nguyên. Các liên kết bảng danh sách vẫn giữ đúng kiểu dáng CSS `.offer-id-link`, `.offer-row-link`.
* **Tương tác người dùng (User Interaction):** Bấm vào ID hoặc nút Xem chi tiết trên dòng Offer $\rightarrow$ Trình duyệt điều hướng thẳng tới `/offers/{id}` qua thẻ `<a>` chuẩn W3C.
* **Tham số URL & Truy vấn:** Hoàn toàn chính xác, không thay đổi router hay controller.
* **Lỗi Console:** Không phát sinh bất kỳ lỗi `Uncaught ReferenceError` nào vì hàm không còn được gọi từ bất kỳ đâu.
* **Kiểm thử Trình duyệt Tự động (Headless Browser Testing):** **NOT VERIFIED** (Đánh dấu trung thực do môi trường thực thi chưa cấu hình Selenium/Playwright tự động).

---

## 5. Xác minh Cách ly Mã nguồn & Bảo vệ Liên phân hệ (Phase 5 — Isolation)

* **Lệnh kiểm tra:** `git status` và `git diff --check`
* **Kết quả:**
  - Chỉ có duy nhất tệp `src/main/resources/static/js/offers.js` bị thay đổi.
  - Không có bất kỳ tệp Java backend, Controller, Service, DTO, Entity nào bị tác động.
  - Không thay đổi schema database hay file migration.
  - Không can thiệp vào các tệp dùng chung (`flash-toast.js`, `topbar-notifications.js`, `main.css`).
  - Toàn bộ các thay đổi chưa commit trước đó được bảo toàn nguyên vẹn.

---

## 6. Đề xuất Bước tiếp theo (Next Candidate for Task 05)

Sau khi kiểm chứng thành công quy trình cắt giảm với Candidate 1, bước chuyển đổi tiếp theo được khuyến nghị:
* **Ứng viên:** **Dọn dẹp các khối dựng DOM HTML thừa đã được thay thế bởi Thymeleaf** (Ứng viên 3: `renderDetailModalHtml` và `renderDetailFooterHtml`).
* **Lợi ích:** Cắt giảm thêm **201 dòng JavaScript thừa**, giúp tệp `offers.js` gọn nhẹ hơn mà vẫn đảm bảo 100% không ảnh hưởng nghiệp vụ.

---

> [!IMPORTANT]
> **DỪNG LẠI SAU TASK 04 (STOP RULE APPLIED):**  
> Tôi đã dừng lại sau khi hoàn thành đúng 01 thay đổi duy nhất đã được kiểm chứng an toàn. Vui lòng cho biết phản hồi của bạn!

