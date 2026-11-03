# KẾ HOẠCH CHUYỂN ĐỔI TỪNG BƯỚC AN TOÀN (SAFE MIGRATION PLAN)
**Nhiệm vụ:** Task 01 — Safe HTML/CSS Refactoring Audit  
**Nguyên tắc chỉ đạo:** "Đi từng bước nhỏ — An toàn tuyệt đối — Có khả năng Rollback ngay lập tức — Không phá vỡ hệ thống."  
**Điều kiện tiên quyết:** KHÔNG BẮT ĐẦU REFACTOR CHO ĐẾN KHI CÓ SỰ PHÊ DUYỆT RÕ RÀNG TỪ NGƯỜI DÙNG / TRƯỞNG NHÓM.  

---

## 1. Nguyên tắc An toàn Bất biến (Safety Guardrails)

Trong suốt quá trình thực thi kế hoạch chuyển đổi, lập trình viên bắt buộc phải tuân thủ nghiêm ngặt 5 nguyên tắc sau:
1. **Nguyên tắc "Chỉ một thành phần tại một thời điểm" (One Component at a Time):** Không bao giờ refactor đồng thời 2 thành phần khác nhau trong cùng một lượt.
2. **Quy trình "Test trước — Sửa — Test sau — So sánh UI" (T-R-T-V Cycle):**
   * *Bước 1 (Pre-test):* Chạy `mvn test -Dtest=*Offer*` đảm bảo toàn bộ test đang PASS màu xanh.
   * *Bước 2 (Refactor):* Chỉ sửa đổi 1 thành phần HTML/CSS hoặc lược bỏ hàm JS tương ứng.
   * *Bước 3 (Post-test):* Chạy lại test suite để chứng minh không làm gãy logic backend.
   * *Bước 4 (Visual Check):* Mở trình duyệt kiểm tra thực tế giao diện và tính năng.
3. **Bảo toàn Giao diện Công khai (Public Interfaces Preservation):** Giữ nguyên toàn bộ các ID, tên trường `name`, và các class CSS cốt lõi mà các file khác có thể đang liên kết tới.
4. **Không "Lách luật" Mã nguồn (No Inline Script Cheat):** Không được chuyển code từ `offers.js` thành các thuộc tính `onclick="..."`, `oninput="..."` trên thẻ HTML. Mục tiêu là dùng tính năng native của HTML5/CSS3.
5. **Cấm sửa Tệp tin Dùng chung:** Tuyệt đối không chỉnh sửa `flash-toast.js`, `main.css`, hay `topbar-notifications.js`.

---

## 2. Lộ trình Chuyển đổi 5 Giai đoạn (5-Phase Phased Migration)

```
[Giai đoạn 1: Low-Risk] ───► [Giai đoạn 2: Filters] ───► [Giai đoạn 3: UX Controls] ───► [Giai đoạn 4: Forms] ───► [Giai đoạn 5: Cleanup]
• Min Date HTML5             • KPI Tab Links               • Character Limits              • Director Modal Form        • Xóa JS thừa
• Export Reset Button        • Search Reset Button         • Chip Radios                   • Action Confirmations       • Final Verification
```

---

### GIAI ĐOẠN 1: Chuyển đổi Rủi ro Cực thấp — Thuộc tính Form & Thiết lập Mặc định
*Thời gian ước tính: 1 – 2 giờ | Mức độ rủi ro: Cực thấp (Zero Business Logic Impact)*

#### Bước 1.1: Chuyển đổi ràng buộc Ngày bắt đầu tối thiểu (`minDate`) sang Thymeleaf native
* **Thành phần tác động:** Ô nhập ngày `expectedStartDate` trong [`offers/form.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/form.html).
* **Thao tác thực hiện:**
  1. Thêm thuộc tính `th:min="${#temporals.format(#temporals.createNow().plusDays(createRequest.responsePeriodDays != null ? createRequest.responsePeriodDays + 1 : 6), 'yyyy-MM-dd')}"` trực tiếp vào thẻ `<input type="date">`.
  2. Vô hiệu hóa hàm `updateMinExpectedStartDate()` và `initOfferFormExpectedStartDateMin()` trong `offers.js`.
* **Xác minh (Verification):** Mở `/offers/create`, DatePicker tự động làm mờ các ngày quá khứ.
* **Quy trình Hoàn nguyên (Rollback):** `git restore src/main/resources/templates/offers/form.html src/main/resources/static/js/offers.js`.

#### Bước 1.2: Chuyển đổi nút "Đặt lại mặc định" trong Modal Xuất Excel sang HTML5 native
* **Thành phần tác động:** Nút `#btnResetColumns` trong [`offers/list.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/list.html).
* **Thao tác thực hiện:**
  1. Chuyển thẻ `<button id="btnResetColumns">` thành thẻ `<input type="reset" value="Mặc định" class="btn btn-secondary">`.
  2. Bổ sung thuộc tính `checked` mặc định cho các cột chuẩn trong file HTML.
  3. Lược bỏ hàm `resetDefaultColumns()` trong `offers.js`.
* **Xác minh:** Mở modal xuất Excel, bỏ chọn một số cột, bấm nút Reset $\rightarrow$ Các checkbox chuẩn tự động tích lại.

---

### GIAI ĐOẠN 2: Tối ưu hóa Điều hướng Bộ lọc & Thẻ KPI (Filters & Status Navigation)
*Thời gian ước tính: 2 – 3 giờ | Mức độ rủi ro: Thấp*

#### Bước 2.1: Chuyển đổi các Thẻ KPI Cards thành Thẻ Liên kết `<a>` Trực tiếp
* **Thành phần tác động:** Thẻ KPI trong [`offers/list.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/list.html) và [`offers/director-list.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/director-list.html).
* **Hành vi hiện tại:** Thẻ là thẻ `<div>`, lắng nghe sự kiện JS click, đọc `data-status`, gán param và submit form ngầm.
* **Thao tác thực hiện:**
  1. Chuyển thẻ bọc ngoài từ `<div class="stat-card">` sang thẻ `<a th:href="@{/offers(status='Pending_Director')}" class="stat-card">`.
  2. Định kiểu CSS `text-decoration: none; color: inherit;` để giữ nguyên 100% hình ảnh giao diện.
  3. Lược bỏ hàm `filterOffersByStatus()` và `filterDirectorOffersByStatus()` trong `offers.js`.
* **Xác minh:** Bấm vào thẻ KPI "Chờ phê duyệt" $\rightarrow$ Trình duyệt chuyển thẳng sang URL `?status=Pending_Director` và lọc đúng dữ liệu.

---

### GIAI ĐOẠN 3: Tinh giản Bộ đếm Ký tự & Bộ chọn Lý do Từ chối
*Thời gian ước tính: 2 – 3 giờ | Mức độ rủi ro: Thấp - Trung bình*

#### Bước 3.1: Sử dụng `maxlength` và CSS `:user-invalid` cho Ô Lý do Từ chối
* **Thành phần tác động:** Thẻ `<textarea id="directorDecisionComments">` trong modal từ chối của Giám đốc.
* **Thao tác thực hiện:**
  1. Thêm thuộc tính `maxlength="2000"` và `required` vào thẻ `<textarea>`.
  2. Thêm luật CSS trong `offers.css`:
     ```css
     textarea:user-invalid {
         border-color: var(--danger-color, #dc3545);
         box-shadow: 0 0 0 2px rgba(220, 53, 69, 0.2);
     }
     ```
  3. Tinh giản hàm `handleDirectorDecisionCommentInput()` chỉ còn 1 dòng đếm đơn giản.
* **Xác minh:** Cố tình paste hơn 2000 ký tự $\rightarrow$ Trình duyệt tự động cắt ngắn ở ký tự 2000. Để trống textarea và bấm submit $\rightarrow$ Giao diện tự động viền đỏ cảnh báo.

#### Bước 3.2: Chuyển đổi Chip Lý do Từ chối của Ứng viên sang Radio Buttons Native
* **Thành phần tác động:** Nhóm chip `.reason-chip` trong [`offers/candidate-detail.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-detail.html).
* **Thao tác thực hiện:**
  1. Chuyển các thẻ `div` thành cấu trúc `<label class="chip-label"><input type="radio" name="reasonPreset" ...> ...</label>`.
  2. Định kiểu trạng thái checked bằng CSS `:has(:checked)` hoặc `:checked + label`.
  3. Giữ 2 dòng JS tối giản để sao chép giá trị radio đã chọn vào textarea.
* **Xác minh:** Click vào từng chip $\rightarrow$ Chip đổi màu xanh nổi bật và textarea tự điền lý do tương ứng.

---

### GIAI ĐOẠN 4: Chuyển đổi Modal Duyệt Nhanh của Giám đốc sang Form POST Native
*Thời gian ước tính: 3 – 4 giờ | Mức độ rủi ro: Trung bình (Cần kiểm tra kỹ luồng CSRF và Controller)*

#### Bước 4.1: Chuyển đổi Modal Duyệt/Từ chối thành Form POST Chuẩn (PRG Pattern)
* **Thành phần tác động:** Modal `#directorDecisionModal` trong [`offers/director-list.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/director-list.html).
* **Hành vi hiện tại:** JS bắt submit, parse JSON, gọi Fetch API bất đồng bộ với header CSRF, parse JSON response, rồi gọi `window.location.reload()`.
* **Thao tác thực hiện:**
  1. Chuyển modal thành form HTML có cấu trúc chuẩn:
     ```html
     <dialog id="directorDecisionModal" class="modal-dialog">
         <form method="POST" id="directorDecisionForm" th:action="@{/offers/0/approve}">
             <!-- Thymeleaf tự động render CSRF hidden input -->
             <textarea name="comments" id="directorDecisionComments" required maxlength="2000"></textarea>
             <button type="submit" class="btn btn-primary">Xác nhận</button>
             <button type="button" onclick="this.closest('dialog').close()" class="btn btn-secondary">Hủy</button>
         </form>
     </dialog>
     ```
  2. JavaScript chỉ còn đúng 2 dòng làm nhiệm vụ mở modal và đổi thuộc tính `action` của form theo Offer ID:
     ```javascript
     function openDirectorDecisionModal(offerId, action) {
         const form = document.getElementById('directorDecisionForm');
         form.action = `/offers/${offerId}/${action}`;
         document.getElementById('directorDecisionModal').showModal();
     }
     ```
  3. **Lược bỏ toàn bộ 150 dòng code xử lý Fetch API, xử lý lỗi mạng, và đọc CSRF token thủ công!**
* **Xác minh:**
  * Giám đốc bấm Phê duyệt $\rightarrow$ Form submit native, trang reload kèm Flash Toast: *"Đã phê duyệt đề xuất nhận việc thành công!"*.
  * Chạy test suite: `mvn test -Dtest=OfferIntegrationTests`.

---

### GIAI ĐOẠN 5: Dọn dẹp Mã nguồn Thừa & Kiểm thử Tổng thể Hệ thống
*Thời gian ước tính: 2 giờ | Mức độ rủi ro: Thấp*

#### Bước 5.1: Xóa bỏ các hàm JavaScript Không còn Sử dụng (Dead Code Removal)
* Lọc bỏ các hàm đã được thay thế hoàn toàn trong `offers.js` (ước tính cắt giảm từ **2,005 dòng xuống còn dưới 700 dòng**).
* Kiểm tra console trình duyệt để đảm bảo không còn bất kỳ lỗi `Uncaught ReferenceError` nào.

#### Bước 5.2: Kiểm thử Hồi quy Toàn diện (Final Regression Execution)
* Kích hoạt toàn bộ 126 ca test tự động của Maven: `mvn test -Dtest=*Offer*`.
* Thực hiện kiểm thử thủ công cả 7 Hành trình Người dùng Cốt lõi (Critical User Journeys) trên Chrome và Edge.
* Báo cáo kết quả nghiệm thu cho Trưởng nhóm.

---

## 3. Quy trình Hoàn nguyên Khẩn cấp (Rollback Procedures)

Nếu tại bất kỳ thời điểm nào sau khi sửa code mà xảy ra lỗi phát sinh (Test Fail, giao diện bị vỡ, hoặc lỗi bảo mật), lập trình viên phải thực hiện hoàn nguyên ngay lập tức theo quy trình 3 bước sau:

1. **Bước 1 (Xác định file bị lỗi):** Chạy `git status` để xem chính xác các file vừa sửa đổi.
2. **Bước 2 (Khôi phục trạng thái commit trước đó):**
   ```powershell
   # Khôi phục file HTML
   git restore src/main/resources/templates/offers/<ten-file>.html
   # Khôi phục file JS
   git restore src/main/resources/static/js/offers.js
   # Khôi phục file CSS
   git restore src/main/resources/static/css/offers.css
   ```
3. **Bước 3 (Kiểm tra lại tính an toàn):** Chạy lại `mvn test -Dtest=*Offer*` để xác nhận hệ thống đã quay về trạng thái màu xanh an toàn 100%.

