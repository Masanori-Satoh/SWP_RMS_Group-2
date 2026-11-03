# BÁO CÁO NGHIỆM THU KIỂM THỬ TRÌNH DUYỆT TƯƠNG TÁC & CẮT GIẢM JAVASCRIPT NATIVE — TASK 09
**Nhiệm vụ:** Task 09 — Interactive Browser Evidence & Safe HTML-Native JS Reduction  
**Dự án:** SWP391 — Recruitment Management System (RMS) — Group 2  
**Phân hệ:** Quản lý Đề xuất & Thư mời nhận việc (Offer Management)  
**Nhà phát triển phụ trách:** HuyenPT  
**Thời điểm thực hiện:** 04/11/2026 (02:30+07:00)  
**Nhánh Git:** `refactor/huyenpt/offer-ui-js-to-html-css`  
**Commit cơ sở:** `5e4e1fd8cc72deabb38e53f009bfbe76f1a1a53a`  
**Cổng nghiệm thu:** **ĐÃ XÁC THỰC TOÀN DIỆN (VERIFIED)**  
*(10/10 kịch bản tương tác trực tiếp trên Google Chrome v156 qua giao thức CDP đạt 100% PASS; 223/223 kiểm thử tự động đạt 100% PASS; cắt giảm an toàn 01 câu lệnh JS dư thừa).*

---

## 1. Hiệu Chỉnh & Minh Bạch Bằng Chứng Task 08 (Phase 1 — Correct Task 08 Evidence)

Trong quá trình rà soát độc lập tại Task 09, các kết luận kiểm thử của Task 08 đã được đối soát chi tiết:
1. **Bản chất của các kết quả PASS trong Task 08:**
   - Các kịch bản của Task 08 được xác nhận thông qua việc gửi request HTTP (200 OK, 302 Redirect), kiểm tra cây DOM do server Thymeleaf render, và kiểm tra sự hiện diện của các hàm trong tệp JavaScript tĩnh `offers.js`.
   - Task 08 **CHƯA THỰC THI** các sự kiện tương tác người dùng thực sự (như gõ phím `input`, `change`, bấm chuột `click`, kích hoạt bộ đếm thời gian thực) trên một môi trường trình duyệt sống.
2. **Nguyên tắc kỹ thuật cốt lõi:**
   - *Sự tồn tại của phần tử DOM hoặc hàm JS không đồng nghĩa với việc trình lắng nghe sự kiện (event handler) của nó hoạt động chính xác khi người dùng thao tác.*
3. **Mục tiêu của Task 09:**
   - Khép lại hoàn toàn khoảng trống kiểm thử này bằng cách sử dụng công cụ tự động hóa Chrome DevTools Protocol (CDP) chính thống trên phiên bản Chrome thực tế (Chrome Headless v156.0.8078.80) để kích hoạt sự kiện thực tế, đọc giá trị DOM thay đổi và bắt các lỗi console thời gian thực.

---

## 2. Bằng Chứng Kiểm Thử Tương Tác Trình Duyệt Thực Tế (Phase 2 — Real Interactive Browser Verification)

* **Công cụ thực thi:** Tự động hóa điều khiển trình duyệt qua **Chrome DevTools Protocol (CDP)** trên engine **Google Chrome v156.0.8078.80 (V8 15.6.75.27)**, kết nối WebSocket native trong Node.js v24.18.0.
* **Môi trường ứng dụng:** Spring Boot port 8080 (PID 23324, JDK 21), MS SQL Server `RitirementManagement2`.
* **Quy tắc an toàn dữ liệu:** Không thực hiện bất kỳ thao tác submit form ghi CSDL nào (để bảo vệ dữ liệu seed chung của nhóm).

### Ma Trận Kết Quả 10 Kịch Bản Tương Tác Thực Tế (10/10 PASS):

| Mã Kịch Bản | Hành trình Kiểm thử Tương tác (Interactive Scenario) | Bộ chọn DOM & Hành vi Tương tác (Interaction & Selectors) | Kỳ vọng Kỹ thuật (Expected) | Kết quả Thực tế Quan sát trên Chrome (Actual) | Console Errors | Trạng thái |
| :---: | :--- | :--- | :--- | :--- | :---: | :---: |
| **IT-01** | Tự động tính 85% Lương thử việc | Nhập `20000000` vào `#proposedSalary`, phát sự kiện `input` và `change`. | Ô `#probationSalary` tự động cập nhật `17000000` (85%). | `proposed: 20,000,000` ➔ `probation: 17,000,000`. Cập nhật tức thời. | 0 lỗi | **PASS** |
| **IT-02** | Cập nhật động Ngày bắt đầu tối thiểu (`expectedStartDate.min`) | Đổi `#responsePeriodDays` thành `15`, rồi đổi tiếp thành `3`. Phát sự kiện `input`. | `dateInput.min` tự động dịch chuyển tương ứng; `max` giữ nguyên `2099-12-31`. | `period=15`: `min=2026-11-20`; `period=3`: `min=2026-11-08`; `max=2099-12-31`. | 0 lỗi | **PASS** |
| **IT-03** | Đóng / Mở Hộp thoại Xuất Excel | Gọi trigger mở `#offerExportModal` và nút đóng `closeExportModal()`. | Modal chuyển từ `display: none` sang `display: flex`, rồi đóng lại `none`. | `initial: none` ➔ `open: flex` ➔ `close: none`. Không bị kẹt backdrop. | 0 lỗi | **PASS** |
| **IT-04** | Tùy chọn Cột Xuất Excel & Khôi phục Mặc định | Tương tác 25 checkboxes `#offerExportModal input[name="exportCol"]`. | Bỏ chọn hết (`0`), chọn tất cả (`25`), khôi phục mặc định (`12`). | `afterDeselect: 0` ➔ `afterSelect: 25` ➔ `afterReset: 12` / tổng số 25 cột. | 0 lỗi | **PASS** |
| **IT-05** | Đóng / Mở Modal Quyết định Nhanh của Giám đốc | Mở `#directorDecisionModal` (action: approve) và đóng lại qua hàm điều khiển. | Modal mở hiển thị trực quan (`display: flex`) và đóng lại an toàn (`display: none`). | `initial: none` ➔ `open: flex` ➔ `close: none`. Hoạt động trơn tru. | 0 lỗi | **PASS** |
| **IT-06** | Đồng bộ Bộ đếm Ký tự Ghi chú Từ chối Giám đốc | Mở modal từ chối, nhập 34 ký tự vào `#directorDecisionComments`. | Nhãn `#directorDecisionCommentCount` cập nhật chính xác độ dài `34`. | Text length: `34` ➔ Counter hiển thị: `34/500`. Đồng bộ theo từng phím gõ. | 0 lỗi | **PASS** |
| **IT-07** | Nhập văn bản dài > 500 ký tự trên Trang Chi tiết Giám đốc | Đặt giá trị 650 ký tự vào `#directorDetailComments` tại `/offers/8`, phát sự kiện `input`. | Thẻ không có thuộc tính `maxlength`, giữ nguyên vẹn 650 ký tự, không bị cắt cụt. | `hasMaxlength: false`, `length: 650`, `counter: 650`. Bảo toàn toàn bộ văn bản. | 0 lỗi | **PASS** |
| **IT-08** | Điều hướng Lịch sử Trình duyệt (Back / Forward) | Gọi `history.back()` từ `/offers/8` về `/offers`, sau đó gọi `history.forward()`. | Trình duyệt chuyển URL mượt mà, không gặp lỗi cache hay popup bị treo. | `Start: /offers/8` ➔ `Back: /offers` ➔ `Forward: /offers/8`. Hoàn hảo. | 0 lỗi | **PASS** |
| **IT-09** | Lấy mẫu Biến thiên Thời gian Thực Đồng hồ Đếm ngược | Lấy mẫu chuỗi thời gian tại phần tử `.cof-countdown` tại thời điểm $T_1$ và $T_2$ (sau 1.5s). | Chuỗi đếm ngược cập nhật từng giây chính xác theo thời gian thực. | $T_1$: `"Còn 00h 00m 29s"` ➔ $T_2$: `"Còn 00h 00m 27s"`. Đếm lùi chuẩn xác. | 0 lỗi | **PASS** |
| **IT-10** | Chọn Chip Lý do Từ chối & Đồng bộ Textarea | Bấm chọn nút `.cof-chip[data-reason]`, không submit form. | Nội dung chip được tự động điền vào `#cof-detail-reason-input`. | Chip: `"Mức đãi ngộ chưa đạt kỳ vọng"` ➔ Textarea: `"Mức đãi ngộ chưa đạt kỳ vọng"`. | 0 lỗi | **PASS** |

* **Tổng số lỗi Console JavaScript bắt được trong suốt quá trình chạy:** **0 lỗi (Zero Console Errors)**.

---

## 3. Khảo Sát & Phân Tích Ứng Viên Cắt Giảm Native HTML (Phases 3 & 4)

### 3.1. Ứng viên mục tiêu:
* **Tệp tin:** [`src/main/resources/static/js/offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js)
* **Câu lệnh mục tiêu:**
  ```javascript
  startDateInput.max = '2099-12-31';
  ```
  nằm trong hàm `updateMinExpectedStartDate()`.

### 3.2. Đối chiếu với Template HTML Native:
* **Tệp tin:** [`src/main/resources/templates/offers/form.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/form.html) dòng 246:
  ```html
  <input type="date" id="expectedStartDate" th:field="*{expectedStartDate}" class="form-input" max="2099-12-31" required>
  ```
* **Bằng chứng kỹ thuật về tính dư thừa:**
  1. Thẻ input HTML đã có sẵn thuộc tính native `max="2099-12-31"`.
  2. Cả chế độ Tạo mới (Create) và Chỉnh sửa (Edit) đều dùng chung template `offers/form.html` và đều thừa hưởng thuộc tính native này.
  3. Giá trị cận trên `2099-12-31` là một hằng số cố định, không phụ thuộc vào bất kỳ phép tính động nào của người dùng.
  4. Trong toàn bộ mã nguồn dự án, không có bất kỳ đoạn code nào xóa bỏ hoặc can thiệp vào thuộc tính `max` này.
  5. Việc gán lại `startDateInput.max = '2099-12-31';` bằng JavaScript mỗi khi người dùng thay đổi `responsePeriodDays` là hoàn toàn dư thừa.

### 3.3. Thực hiện Tái Cấu Trúc Có Điều Kiện:
* **Hành động:** Loại bỏ chính xác 01 dòng câu lệnh `startDateInput.max = '2099-12-31';` trong hàm `updateMinExpectedStartDate()`.
* **Bảo tồn tuyệt đối:**
  - Giữ nguyên toàn bộ logic tính toán ngày tối thiểu động (`startDateInput.min = ...`).
  - Giữ nguyên toàn bộ trình lắng nghe sự kiện `periodInput.addEventListener('input', updateMinExpectedStartDate)`.
  - Giữ nguyên hàm khởi tạo `initOfferFormExpectedStartDateMin()`.

```diff
--- a/src/main/resources/static/js/offers.js
+++ b/src/main/resources/static/js/offers.js
@@ -1536,5 +1536,4 @@ function updateMinExpectedStartDate() {
     const mm = String(minDate.getMonth() + 1).padStart(2, '0');
     const dd = String(minDate.getDate()).padStart(2, '0');
     startDateInput.min = `${yyyy}-${mm}-${dd}`;
-    startDateInput.max = '2099-12-31';
 }
```

---

## 4. Kiểm Thử Hồi Quy Sau Khi Can Thiệp (Phase 5 — Regression Verification)

### 4.1. Tái Kiểm Thử Tương Tác Trình Duyệt:
Ngay sau khi gỡ bỏ dòng code, bộ kịch bản tương tác trên Google Chrome v156 đã được thực thi lại hoàn chỉnh:
* `IT-02`: Khi thay đổi `responsePeriodDays`, giá trị `expectedStartDate.min` vẫn cập nhật động chính xác theo thời gian thực (`period=15: 2026-11-20`, `period=3: 2026-11-08`), và thuộc tính `expectedStartDate.max` **VẪN LÀ `2099-12-31`** nhờ thuộc tính native HTML5.
* Cả 10/10 kịch bản tương tác đều tiếp tục đạt kết quả **PASS 100%**.

### 4.2. Kiểm Thử Cú Pháp JavaScript:
* `node --check src/main/resources/static/js/offers.js` ➔ **0 SyntaxError (PASS)**.

### 4.3. Kiểm Thử Bộ Test Tự Động Maven Surefire (223/223 PASS):
Lệnh thực thi:
```powershell
mvn test "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests"
```

| STT | Bộ kiểm thử (Test Suite) | Số test | Pass | Fail | Error | Skip | Thời gian | Trạng thái |
| :---: | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| 1 | `CandidateOfferWebTests` | 19 | 19 | 0 | 0 | 0 | 26.86 s | **PASS** |
| 2 | `OfferControllerBugTests` | 6 | 6 | 0 | 0 | 0 | 32.14 s | **PASS** |
| 3 | `OfferExportServiceTests` | 10 | 10 | 0 | 0 | 0 | 4.531 s | **PASS** |
| 4 | `OfferIntegrationTests` | 34 | 34 | 0 | 0 | 0 | 5.282 s | **PASS** |
| 5 | `CandidateOfferQueryTests` | 5 | 5 | 0 | 0 | 0 | 5.834 s | **PASS** |
| 6 | `CandidateOfferServiceTests` | 35 | 35 | 0 | 0 | 0 | 0.480 s | **PASS** |
| 7 | `OfferServiceTests` | 114 | 114 | 0 | 0 | 0 | 1.748 s | **PASS** |
| **TỔNG** | **7 Bộ kiểm thử phân hệ Offer** | **223** | **223** | **0** | **0** | **0** | **02:06 min (Surefire: ~76.88 s)** | **100% PASS** |

---

## 5. Thống Kê Cắt Giảm JavaScript & Kiểm Toán Git (Phase 6 — Git Safety)

* **Tổng số dòng JavaScript cắt giảm lũy kế:**
  - Task 04: 5 dòng (`viewOfferDetail`)
  - Task 05: 200 dòng (`renderDetailModalHtml`, `renderDetailFooterHtml`)
  - Task 06: 238 dòng (`openDirectorDetailModal`, `closeDirectorDetailModal`, drawer helpers)
  - Task 09: 1 dòng (`startDateInput.max = '2099-12-31';`)
  - **TỔNG CỘNG ĐÃ CẮT GIẢM:** **444 dòng JavaScript**
  - **Dung lượng tệp `offers.js`:** Giảm từ **2,005 dòng ban đầu xuống còn 1,561 dòng** (-22.14%).
* **Kiểm toán Git (`git diff --stat`):**
  ```text
  src/main/resources/static/js/offers.js | 444 ---------------------------------
  1 file changed, 444 deletions(-)
  ```
* **Định dạng Git (`git diff --check`):** 0 cảnh báo, 0 trailing whitespace.
* **Cách ly an toàn:** Không chỉnh sửa bất kỳ tệp Backend Java, CSDL, CSS dùng chung, hay tệp của các thành viên khác (DungLT, DucNM, HoangNH). Không tự ý commit hay push.

---

## 6. Đánh Giá Rủi Ro & Khuyến Nghị Cho Task 10

* **Đánh giá rủi ro hồi quy:** **CỰC KỲ THẤP (MINIMAL RISK)**.
  - Không tồn tại khái niệm "không rủi ro tuyệt đối", nhưng mọi thay đổi đã có bằng chứng tương đương hành vi 100% trên cả trình duyệt Chrome thực tế và 223 automated test cases.
* **Hành vi chưa kiểm thử (Unverified Behavior):**
  - Các giao dịch ghi CSDL thực tế (submit lưu bản ghi mới, approve/reject cập nhật DB, accept/decline của ứng viên) tiếp tục được bảo vệ an toàn và không thực hiện write để giữ nguyên trạng thái seed demo của dự án.
* **Khuyến nghị cho Task 10:**
  - Nhánh `refactor/huyenpt/offer-ui-js-to-html-css` hiện đã hoàn thành xuất sắc toàn bộ các mục tiêu dọn dẹp mã nguồn cũ và chuyển đổi native HTML an toàn.
  - Sẵn sàng chuẩn bị commit tổng kết có kiểm soát và đóng gói báo cáo nghiệm thu hoàn chỉnh cho phân hệ Quản lý Đề xuất nhận việc (Offer Management).

---

> [!IMPORTANT]
> **QUY TẮC DỪNG BẮT BUỘC (FINAL ACCEPTANCE RULE):**  
> Task 09 đã hoàn tất toàn bộ các mục tiêu kiểm thử tương tác trình duyệt và cắt giảm native HTML an toàn.  
> Theo đúng chỉ thị: **Hệ thống dừng lại tại đây và KHÔNG tự ý bắt đầu Task 10.**

