# BẢNG PHÂN LOẠI & ĐÁNH GIÁ MÃ NGUỒN JAVASCRIPT (JS CLASSIFICATION)
**Nhiệm vụ:** Task 01 — Safe HTML/CSS Refactoring Audit  
**Tệp mục tiêu phân tích:** `src/main/resources/static/js/offers.js` (2,005 dòng, 77 hàm) và `src/main/resources/static/js/pages/candidate-offers.js` (285 dòng, 5 hàm)  
**Phạm vi:** Phân hệ Quản lý Đề xuất & Thư mời nhận việc (HuyenPT)  
**Tiêu chí phân loại:** `CONVERT` | `PARTIAL` | `KEEP` | `BLOCKED`  

---

## 1. Ma trận Tổng hợp Phân loại Toàn bộ 82 Hàm JavaScript

| Nhóm chức năng | Tệp tin | Danh sách hàm đại diện | Số lượng hàm | Phân loại đề xuất | Đánh giá khả năng chuyển đổi |
| :--- | :--- | :--- | :---: | :---: | :--- |
| **1. Date Limits & Min-Date** | `offers.js` | `getTodayDateString`, `getTomorrowDateString`, `updateMinExpectedStartDate`, `initOfferFormExpectedStartDateMin` | 4 | **CONVERT** | 100% thay bằng thuộc tính HTML5 `min` kết hợp Thymeleaf format |
| **2. Export Modal Controls** | `offers.js` | `selectAllColumns`, `deselectAllColumns`, `resetDefaultColumns`, `handleExportModalBackdropClick` | 4 | **CONVERT** | Thay bằng checkbox native checked, form `<input type="reset">` |
| **3. Modal Close & Backdrop** | `offers.js` | `handleCreateModalBackdropClick`, `handleDetailModalBackdropClick`, `handleDirectorModalBackdropClick` | 3 | **CONVERT** | Thay bằng thẻ `<form method="dialog">` và CSS backdrop native |
| **4. Character Counters** | `offers.js` | `updateRejectReasonCounter`, `updateDirectorDetailCommentCounter`, `handleDirectorDecisionCommentInput` | 3 | **CONVERT** | Thay bằng thuộc tính HTML5 `maxlength` và CSS `:user-invalid` |
| **5. Flash Auto-dismiss** | `offers.js` | `initOfferFlashAutoDismiss`, `displayOfferFlashBanner`, `checkPendingOfferFlash` | 3 | **CONVERT** | Thay bằng CSS Pure Keyframe Animation `@keyframes fadeOut` |
| **6. Reason Chips Selector** | `candidate-offers.js` | `initReasonChips` | 1 | **CONVERT** | Thay bằng `<input type="radio" class="chip-radio">` styled CSS `:checked` |
| **7. KPI Filter & Status Tabs**| `offers.js` | `filterOffersByStatus`, `filterDirectorOffersByStatus`, `applyOfferFilters`, `clearSearchInput` | 4 | **PARTIAL** | Chuyển các tab lọc tĩnh thành thẻ liên kết `<a>` server-side query |
| **8. 85% Salary Auto-calc** | `offers.js` | `autoCalculateProbationSalary`, `validateProbationRuleUi` | 2 | **PARTIAL** | Giữ listener JS tối giản (3 dòng) + thuộc tính HTML5 `min` / `step` |
| **9. Director Quick Submit** | `offers.js` | `submitDirectorDecision`, `openDirectorDecisionModal`, `closeDirectorDecisionModal` | 3 | **PARTIAL** | Chuyển từ Ajax JSON sang Form HTML POST native (PRG pattern) |
| **10. Detail View Navigation**| `offers.js` | `viewOfferDetail`, `renderDetailModalHtml`, `renderDetailFooterHtml` | 3 | **PARTIAL** | Thay bằng trang xem chi tiết trực tiếp `/offers/{id}` (đã hoàn thành 1 phần ở commit `5ef6761c`) |
| **11. Countdown Timer** | `candidate-offers.js` | `initCountdowns`, `update`, `pad` | 3 | **KEEP** | **BẮT BUỘC GIỮ:** Cần `setInterval()` tính khoảng cách mili-giây real-time |
| **12. Bulk Export Selection** | `offers.js` | `getSelectedOfferIds`, `toggleSelectAllOffers`, `executeOfferExport`, `updateSelectionToolbar` | 5 | **KEEP** | **BẮT BUỘC GIỮ:** Cần JS gom mảng ID qua các trang và gọi POST stream |
| **13. Send / Delete Actions** | `offers.js` | `confirmSendOffer`, `executeSendOfferAction`, `confirmDeleteOffer` | 3 | **KEEP** | Giữ modal native `<dialog>` xác nhận trước khi submit form POST |
| **14. Shared Toast Engine** | `flash-toast.js` | `showFlashToast`, `dismissToast` | 2 | **BLOCKED** | **BẤT KHẢ XÂM PHẠM:** Dùng chung 19 template của các module khác |
| **15. Topbar Polling** | `topbar-notifications.js`| `fetchUnreadCount`, `updateBadge` | 2 | **BLOCKED** | **BẤT KHẢ XÂM PHẠM:** Script toàn cục của Core/Admin module |
| **16. Security Utilities** | `offers.js` | `getCsrfInfo` | 1 | **BLOCKED** | Không được sửa đổi cơ chế đọc token bảo mật |
| **Các hàm phụ trợ khác** | `offers.js` | `escapeOfferHtml`, `formatOfferDate`, `formatCurrency`, v.v. | 32 | **PARTIAL/KEEP**| Sẽ được tối giản dần khi chuyển đổi sang Thymeleaf Expression |

---

## 2. Phân tích Chi tiết Từng Ứng viên Chuyển đổi (Candidate Deep Dive)

---

### Ứng viên 1: Thiết lập Ngày bắt đầu Tối thiểu (Min Date Enforcement)
* **Đường dẫn tệp & Tên hàm:**
  * File: [`src/main/resources/static/js/offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js)
  * Hàm: `getTodayDateString()` (L174–180), `getTomorrowDateString()` (L182–196), `updateMinExpectedStartDate()` (L1968–1983), `initOfferFormExpectedStartDateMin()` (L1985–2004).
* **Hành vi hiện tại & Phụ thuộc:**
  * Hiện tại JS bắt sự kiện `DOMContentLoaded` và `change` trên ô nhập `responsePeriodDays`, tính toán `today + responsePeriodDays + 1` và gán thuộc tính `min` cho ô `expectedStartDate`.
* **Giải pháp HTML/CSS đề xuất thay thế:**
  * Sử dụng biểu thức Thymeleaf Temporal để kết xuất thẳng giá trị `min` từ máy chủ vào thẻ HTML:
    ```html
    <input type="date" id="expectedStartDate" name="expectedStartDate"
           th:min="${#temporals.format(#temporals.createNow().plusDays(createRequest.responsePeriodDays != null ? createRequest.responsePeriodDays + 1 : 6), 'yyyy-MM-dd')}"
           class="form-control" required />
    ```
* **Tác động phụ tiềm ẩn (Potential Side Effects):**
  * Không có. Trình duyệt tôn trọng thuộc tính `min` của HTML5 và khóa các ngày trước mốc đó trên DatePicker native.
* **Mức độ rủi ro:** **Thấp (Low Risk)**.
* **Kiểm thử hồi quy yêu cầu:**
  * Mở form tạo mới `/offers/create`, kiểm tra DatePicker không cho phép chọn ngày quá khứ.
  * Chạy test case: `OfferServiceTests.testCreateOfferByHr_startDateNotAfterResponsePeriod_throwsException`.

---

### Ứng viên 2: Nút Chọn/Bỏ chọn Cột trong Modal Xuất Excel (Export Modal Controls)
* **Đường dẫn tệp & Tên hàm:**
  * File: [`src/main/resources/static/js/offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js)
  * Hàm: `selectAllColumns()` (L1061–1063), `deselectAllColumns()` (L1065–1067), `resetDefaultColumns()` (L1069–1081).
* **Hành vi hiện tại & Phụ thuộc:**
  * Dùng JS duyệt qua mảng checkbox `.export-col-cb`, set thuộc tính `checked = true/false` bằng vòng lặp.
* **Giải pháp HTML/CSS đề xuất thay thế:**
  * Chuyển nút "Đặt lại mặc định" thành thẻ HTML native `<input type="reset" class="btn btn-secondary" value="Mặc định">`. Các checkbox được đánh dấu sẵn thuộc tính `checked` trong HTML sẽ tự động khôi phục trạng thái chuẩn khi bấm reset mà không cần 1 dòng JS nào!
* **Tác động phụ tiềm ẩn:**
  * Nút reset của HTML chỉ khôi phục về trạng thái lúc ban đầu tải trang (Default DOM state).
* **Mức độ rủi ro:** **Thấp (Low Risk)**.
* **Kiểm thử hồi quy yêu cầu:**
  * Mở modal xuất Excel, bỏ chọn một số cột, bấm nút Reset $\rightarrow$ Các checkbox mặc định sáng lại.
  * Chạy test case: `OfferExportServiceTests.testExport_DefaultColumns_Success`.

---

### Ứng viên 3: Bộ đếm Ký tự Lý do Từ chối (Character Counters)
* **Đường dẫn tệp & Tên hàm:**
  * File: [`src/main/resources/static/js/offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js)
  * Hàm: `updateRejectReasonCounter(el)` (L1678–1683), `updateDirectorDetailCommentCounter(el)` (L1685–1695), `handleDirectorDecisionCommentInput(textarea)` (L1283–1297).
* **Hành vi hiện tại & Phụ thuộc:**
  * Bắt sự kiện `input`, đếm `el.value.length`, tìm phần tử `#charCount` và gán text `len / 2000`.
* **Giải pháp HTML/CSS đề xuất thay thế:**
  * Sử dụng thuộc tính native `maxlength="2000"` trên thẻ `<textarea>`.
  * Kết hợp CSS pseudo-class `:user-invalid` để hiển thị cảnh báo đỏ khi chuỗi rỗng lúc submit.
  * Nếu cần hiển thị số ký tự trực quan, có thể sử dụng giải pháp CSS Counters hoặc thu gọn thành 1 helper chung 3 dòng thay vì 3 hàm riêng biệt dài 50 dòng.
* **Tác động phụ tiềm ẩn:**
  * Trình duyệt tự động chặn không cho gõ quá 2000 ký tự khi có `maxlength`.
* **Mức độ rủi ro:** **Thấp (Low Risk)**.
* **Kiểm thử hồi quy yêu cầu:**
  * Gõ hoặc paste đoạn văn bản 2050 ký tự vào ô nhận xét $\rightarrow$ Trình duyệt tự ngắt ở ký tự 2000.
  * Chạy test case: `OfferControllerBugTests` và `CandidateOfferWebTests`.

---

### Ứng viên 4: Bộ chọn Chip Lý do Từ chối Cổng Ứng viên (Reason Chips Selector)
* **Đường dẫn tệp & Tên hàm:**
  * File: [`src/main/resources/static/js/pages/candidate-offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/candidate-offers.js)
  * Hàm: `initReasonChips(container)` (L12–56).
* **Hành vi hiện tại & Phụ thuộc:**
  * Dùng JavaScript lắng nghe sự kiện `click` trên các thẻ `div.reason-chip`, đọc thuộc tính `data-reason`, cập nhật text vào thẻ `<textarea id="declineReason">` và chuyển đổi class CSS `.active`.
* **Giải pháp HTML/CSS đề xuất thay thế:**
  * Chuyển các chip gợi ý thành cặp thẻ HTML native: `<input type="radio" id="chip1" name="reasonPreset" value="...">` và `<label for="chip1" class="chip-label">`.
  * Định kiểu thuần CSS3:
    ```css
    .chip-radio { display: none; }
    .chip-radio:checked + .chip-label {
        background-color: var(--primary-color);
        color: #fff;
        border-color: var(--primary-color);
    }
    ```
* **Tác động phụ tiềm ẩn:**
  * Nếu ứng viên muốn chọn gợi ý rồi sửa thêm vài từ trong textarea, radio button chỉ gửi giá trị cố định trừ khi có 1 dòng JS nhỏ đồng bộ.
* **Mức độ rủi ro:** **Trung bình (Medium Risk - Cần giữ 2 dòng JS đồng bộ value vào textarea)**.
* **Kiểm thử hồi quy yêu cầu:**
  * Bấm thử từng chip lý do trên modal từ chối $\rightarrow$ Lý do hiển thị đúng trong form gửi đi.
  * Chạy test case: `CandidateOfferServiceTests.declineSetsOfferDeclinedAndRejectsApplication`.

---

### Ứng viên 5: Chuyển đổi Thao tác Phê duyệt Nhanh của Giám đốc (Director Quick Decision)
* **Đường dẫn tệp & Tên hàm:**
  * File: [`src/main/resources/static/js/offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js)
  * Hàm: `submitDirectorDecision()` (L1316–1384), `openDirectorDecisionModal()` (L1212–1281).
* **Hành vi hiện tại & Phụ thuộc:**
  * Hiện tại JS bắt submit modal, đọc `comments`, kiểm tra rỗng, đọc CSRF meta tag, gọi `fetch('/offers/' + id + '/' + action, {method: 'POST', body: JSON})`, nhận JSON trả về và reload trang.
* **Giải pháp HTML/CSS đề xuất thay thế:**
  * Chuyển modal thành Form HTML chuẩn: `<form method="POST" id="directorDecisionForm">` với nút bấm submit trực tiếp.
  * Khi mở modal, chỉ cần cập nhật thuộc tính `form.action = '/offers/' + id + '/approve'`.
  * Spring Boot nhận form POST, validate, xử lý trong transaction và redirect kèm Flash Message (mô hình PRG tiêu chuẩn).
  * **Loại bỏ hơn 120 dòng code xử lý Fetch API, JSON serialization, và bắt lỗi mạng phức tạp!**
* **Tác động phụ tiềm ẩn:**
  * Trang sẽ chuyển hướng (Redirect HTTP 302) thay vì cập nhật DOM tại chỗ. Đây chính là hành vi mong muốn để đồng bộ thẻ KPI và danh sách.
* **Mức độ rủi ro:** **Trung bình (Medium Risk - Cần kiểm thử luồng controller)**.
* **Kiểm thử hồi quy yêu cầu:**
  * Giám đốc bấm Phê duyệt $\rightarrow$ Trang redirect về danh sách, thẻ Đã duyệt tăng 1.
  * Giám đốc bấm Từ chối không nhập lý do $\rightarrow$ Bị chặn lại và báo lỗi rõ ràng.
  * Chạy test case: `OfferIntegrationTests.it16_approveOfferJson_success` và `it17_rejectOfferJson_success`.

---

## 3. Danh mục Mã nguồn Bắt buộc phải Giữ lại (KEEP & BLOCKED)

1. **Bộ đếm ngược thời gian thực (`initCountdowns()` trong `candidate-offers.js`):**
   * **Lý do giữ:** HTML/CSS không có khả năng tự động tính toán thời gian thực chênh lệch từng giây giữa máy khách và deadline mà không có JavaScript `setInterval()`.
2. **Cơ chế thu thập mảng ID xuất Excel (`getSelectedOfferIds()` trong `offers.js`):**
   * **Lý do giữ:** Người dùng có thể tích chọn nhiều checkbox trên nhiều dòng dữ liệu; việc đóng gói danh sách ID thành mảng JSON truyền vào endpoint `/offers/export` đòi hỏi JavaScript DOM query.
3. **Thư viện Toast dùng chung (`flash-toast.js`):**
   * **Lý do khóa (BLOCKED):** Đang được 19 template khác trong toàn bộ dự án sử dụng. Bất kỳ sự thay đổi nào tại file này đều có thể gây lỗi vỡ giao diện của các thành viên khác.

