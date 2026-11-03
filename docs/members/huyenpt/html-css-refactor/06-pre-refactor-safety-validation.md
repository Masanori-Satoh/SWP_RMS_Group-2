# BÁO CÁO XÁC MINH AN TOÀN TRƯỚC TÁI CẤU TRÚC (PRE-REFACTOR SAFETY VALIDATION)
**Nhiệm vụ:** Task 01.5 — Pre-Refactor Safety Validation  
**Dự án:** SWP391 — Recruitment Management System (RMS) — Group 2  
**Phân hệ:** Quản lý Đề xuất & Thư mời nhận việc (Offer Management)  
**Nhà phát triển phụ trách:** HuyenPT  
**Thời điểm thực hiện:** 04/11/2026  
**Trạng thái nhánh Git:** `refactor/huyenpt/offer-ui-js-to-html-css`  
**Nguyên tắc cốt lõi:** CHỈ PHÂN TÍCH (ANALYSIS ONLY) — Tuyệt đối không chỉnh sửa source code ứng dụng — Không đổi nhánh Git — Không phá vỡ backend contract.

---

## 1. Xác minh Thực tế Bộ Kiểm thử Cơ sở (Task A — Test Baseline Verification)

### 1.1. Đối chiếu Số lượng Ca Kiểm thử (Reported vs. Actual)
Trong tài liệu `Task 01` (`01-project-audit.md` và `04-regression-test-checklist.md`), báo cáo ban đầu ghi nhận **126 test cases**. Sau khi kiểm tra toàn diện mã nguồn trong `src/test/java/com/group2/rms/offer/` và thực thi kiểm thử thực tế qua Maven Surefire, số lượng test case thực tế là **223 ca kiểm thử tự động**, phân bổ trên 7 bộ kiểm thử chuyên biệt.

*Báo cáo Task 01 đã ghi thiếu 97 ca kiểm thử (sai lệch 43.5%).*

### 1.2. Kết quả Thực thi Kiểm thử Maven Chi tiết
Lệnh thực thi kiểm thử chuẩn:
```powershell
mvn test "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests"
```

**Bảng Tổng hợp Kết quả Kiểm thử Cơ sở (Execution Baseline):**

| STT | Tên Test Suite (Lớp kiểm thử) | Gói package | Số test | Pass | Fail | Error | Skip | Thời gian chạy | Trạng thái |
| :---: | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| 1 | `CandidateOfferWebTests` | `com.group2.rms.offer` | 19 | 19 | 0 | 0 | 0 | 13.53 s | **PASS** |
| 2 | `OfferControllerBugTests` | `com.group2.rms.offer.controller` | 6 | 6 | 0 | 0 | 0 | 18.06 s | **PASS** |
| 3 | `OfferExportServiceTests` | `com.group2.rms.offer` | 10 | 10 | 0 | 0 | 0 | 3.12 s | **PASS** |
| 4 | `OfferIntegrationTests` | `com.group2.rms.offer` | 34 | 34 | 0 | 0 | 0 | 2.41 s | **PASS** |
| 5 | `CandidateOfferQueryTests` | `com.group2.rms.offer.repository` | 5 | 5 | 0 | 0 | 0 | 3.13 s | **PASS** |
| 6 | `CandidateOfferServiceTests` | `com.group2.rms.offer.service` | 35 | 35 | 0 | 0 | 0 | 0.39 s | **PASS** |
| 7 | `OfferServiceTests` | `com.group2.rms.offer.service` | 114 | 114 | 0 | 0 | 0 | 0.54 s | **PASS** |
| **Tổng** | **7 Test Suites chuyên biệt Offer** | **Toàn bộ Phân hệ Offer** | **223** | **223** | **0** | **0** | **0** | **1m 14s** | **100% PASS** |

> [!NOTE]
> Ngoài 223 test trực tiếp thuộc phân hệ Offer, gói Candidate Application Portal còn có `MyApplicationWebTests` (20 test) và `MyApplicationServiceTests` (26 test) liên quan đến hành trình ứng viên xem Offer, nâng tổng số test liên đới lên 269 test. Toàn bộ 223 test cốt lõi của Offer chạy thành công 100% không có bất kỳ cảnh báo lỗi logic nào.

---

## 2. Thẩm định Chuyên sâu 10 Rủi ro Tái cấu trúc (Task B — Review Refactoring Risks)

Kiểm tra đối chiếu thực tế giữa các đề xuất trong `Task 01` với kiến trúc mã nguồn hiện tại phát hiện nhiều điểm bất khả thi hoặc rủi ro nghiêm trọng:

### Rủi ro 1: Dynamic `expectedStartDate` dựa trên `responsePeriodDays`
* **Đề xuất Task 01:** Thay thế hoàn toàn JavaScript bằng biểu thức Thymeleaf `th:min="${...}"` trên thẻ `<input type="date">`.
* **Thực tế mã nguồn:**
  * Thymeleaf là template engine phía server. Thuộc tính `th:min` chỉ được tính toán và kết xuất **duy nhất 1 lần** tại thời điểm server trả về HTML.
  * Khi Recruiter thao tác trên trình duyệt và thay đổi giá trị trong ô `responsePeriodDays` (ví dụ từ 3 ngày thành 7 ngày), HTML thuần không có cơ chế Data-binding phản ứng (reactive binding) ở client-side để tự động cập nhật lại thuộc tính `min` của thẻ `expectedStartDate`.
  * Nếu gỡ bỏ `updateMinExpectedStartDate()` trong `offers.js`, người dùng có thể chọn ngày nhận việc vi phạm quy tắc nghiệp vụ (`expectedStartDate` $\le$ `responseDeadline`) mà không nhận được bất kỳ cảnh báo nào từ trình duyệt cho đến khi submit form lên server và bị Exception.
* **Kết luận an toàn:** Thuộc tính `th:min` chỉ được phép bổ sung như một lớp bảo vệ tĩnh khởi tạo ban đầu (defense-in-depth). **BẮT BUỘC GIỮ JavaScript listener** để cập nhật động khi người dùng thay đổi giá trị.

### Rủi ro 2: Nút Reset Modal Xuất Excel (`<input type="reset">`)
* **Đề xuất Task 01:** Thay thế hàm `resetDefaultColumns()` bằng thẻ `<input type="reset">` trong modal xuất Excel.
* **Thực tế mã nguồn:**
  * Kiểm tra cấu trúc DOM trong [`offers/list.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/list.html) (dòng 330–430): Modal xuất file Excel hiện tại được xây dựng hoàn toàn bằng các khối `<div>` lồng nhau của Bootstrap Modal, **HOÀN TOÀN KHÔNG CÓ THẺ `<form>` BAO QUANH**.
  * Theo đặc tả chuẩn của W3C và WHATWG HTML, thẻ `<input type="reset">` đặt ngoài thẻ `<form>` là một phần tử vô hiệu — trình duyệt sẽ hoàn toàn không phản ứng gì khi người dùng nhấp chuột.
  * Hơn nữa, `<input type="reset">` chỉ khôi phục DOM về trạng thái mặc định ban đầu khi tải trang, không thể giải quyết được nhu cầu chọn tất cả ("Select All"), bỏ chọn tất cả ("Deselect All"), hoặc áp dụng tập cột tùy biến theo phòng ban.
* **Kết luận an toàn:** **KHÔNG AN TOÀN (UNSAFE)**. Giữ nguyên JavaScript quản lý cấu hình cột xuất Excel.

### Rủi ro 3: Bộ đếm Ký tự (`Character Counters`) và Thuộc tính `maxlength`
* **Đề xuất Task 01:** Thay thế bộ đếm ký tự bằng thuộc tính `maxlength="2000"` và CSS `:user-invalid`.
* **Thực tế mã nguồn:**
  * Thuộc tính HTML5 `maxlength="2000"` hoạt động chuẩn xác trên mọi trình duyệt hiện đại, ngăn chặn nhập vượt quá 2000 ký tự và đồng bộ hoàn hảo với annotation `@Size(max = 2000)` trong `DirectorDecisionRequest.java`.
  * Tuy nhiên, nếu gỡ bỏ hoàn toàn JavaScript cập nhật bộ đếm hiển thị (`updateRejectReasonCounter`), người dùng sẽ mất phản hồi trực quan (ví dụ: "Đã nhập 150/2000 ký tự").
* **Kết luận an toàn:** Việc bổ sung thuộc tính `maxlength="2000"` là **CỰC KỲ AN TOÀN (SAFE)**. Tuy nhiên, nên giữ lại dòng hiển thị số lượng ký tự đếm được để đảm bảo trải nghiệm người dùng không bị thụt lùi.

### Rủi ro 4: Chip Chọn Lý do Từ chối của Ứng viên (`Candidate Reason Chips`)
* **Đề xuất Task 01:** Thay thế các chip lý do từ chối bằng các nút chọn `<input type="radio">` thuần HTML/CSS.
* **Thực tế mã nguồn:**
  * Trong [`candidate-detail.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/candidate/offers/candidate-detail.html), khi ứng viên bấm vào một chip (ví dụ: "Mức lương chưa đáp ứng kỳ vọng", "Địa điểm làm việc xa"), hệ thống cần sao chép nội dung đó vào thẻ `<textarea id="declineReason">`, đồng thời cho phép ứng viên nhập bổ sung hoặc chỉnh sửa văn bản trước khi gửi.
  * HTML/CSS thuần không thể sao chép văn bản từ thuộc tính hoặc label của radio button vào một trường `<textarea>` độc lập.
* **Kết luận an toàn:** **BẮT BUỘC GIỮ JAVASCRIPT (KEEP JAVASCRIPT)** để thực hiện việc đồng bộ hóa nội dung chip sang textarea.

### Rủi ro 5: Xác thực Form Phê duyệt / Từ chối của Giám đốc (Director Form Validation)
* **Đề xuất Task 01:** Gán thuộc tính `required` trực tiếp vào textarea `#directorDecisionComments` trong HTML.
* **Thực tế mã nguồn:**
  * Cả hai hành động "Phê duyệt" (Approve) và "Từ chối" (Reject) đều dùng chung một modal xác nhận (`#directorDecisionModal`).
  * Theo Business Rule BR-OFF-03: Khi Phê duyệt, lý do/ghi chú là **tùy chọn (optional)**; khi Từ chối, lý do từ chối là **bắt buộc (mandatory)**.
  * Nếu đặt thuộc tính `required` tĩnh trên thẻ HTML `<textarea>`, khi Giám đốc nhấn nút "Phê duyệt" mà không nhập ghi chú, trình duyệt sẽ chặn submit form và báo lỗi "Please fill out this field", làm sai lệch hoàn toàn nghiệp vụ.
* **Kết luận an toàn:** **CÓ ĐIỀU KIỆN (CONDITIONAL / KEEP JS)**. JavaScript bắt buộc phải điều khiển thuộc tính `required` hoặc can thiệp trước khi gửi dữ liệu dựa trên loại hành động (`approve` hay `reject`).

### Rủi ro 6: Chuyển đổi Fetch JSON sang HTML Form POST của Giám đốc (NGHIÊM TRỌNG NHẤT)
* **Đề xuất Task 01:** Chuyển đổi modal phê duyệt từ Fetch API JSON sang Form POST chuẩn (`application/x-www-form-urlencoded`).
* **Thực tế mã nguồn trong `OfferController.java`:**
  ```java
  @PostMapping(value = "/{id}/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<ApiResponse<OfferResponse>> approveOfferJson(...)

  @PostMapping(value = "/{id}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<ApiResponse<OfferResponse>> rejectOfferJson(...)
  ```
  * Endpoint backend **bắt buộc khai báo `consumes = MediaType.APPLICATION_JSON_VALUE`** và nhận dữ liệu qua `@RequestBody DirectorDecisionRequest`.
  * Hoàn toàn **KHÔNG TỒN TẠI** endpoint nào nhận dữ liệu dạng `application/x-www-form-urlencoded`!
  * Nếu chuyển form sang HTML POST native, trình duyệt sẽ gửi request với `Content-Type: application/x-www-form-urlencoded`. Spring Framework sẽ lập tức chặn lại và ném ra lỗi **HTTP 415 Unsupported Media Type**!
  * Hơn nữa, phương thức backend trả về `@ResponseBody ResponseEntity<ApiResponse<OfferResponse>>` chứ không trả về redirect URL. Nếu submit bằng form HTML thông thường, màn hình trình duyệt của Giám đốc sẽ hiển thị chuỗi JSON thô thay vì giao diện quản trị.
* **Kết luận an toàn:** **TUYỆT ĐỐI NGUY HIỂM / KHÔNG THỰC HIỆN ĐƯỢC (STRICTLY UNSAFE & BLOCKED)** nếu không được phép sửa backend Controller. Bắt buộc giữ nguyên luồng Fetch API gửi JSON.

### Rủi ro 7: Xử lý CSRF Token
* **Đề xuất Task 01:** Bỏ hàm `getCsrfInfo()` trong JavaScript, dựa vào thẻ `<input type="hidden" name="_csrf">` do Thymeleaf tự sinh.
* **Thực tế mã nguồn:**
  * Do các API Phê duyệt, Từ chối, Xuất Excel, và Xóa Offer sử dụng Fetch API và AJAX bất đồng bộ, Spring Security yêu cầu header `X-CSRF-TOKEN` gửi kèm trong HTTP Headers.
  * Nếu loại bỏ hàm đọc token từ `<meta name="_csrf">`, toàn bộ các request AJAX sẽ bị Spring Security chặn đứng với lỗi **HTTP 403 Forbidden** (Invalid CSRF Token).
* **Kết luận an toàn:** **BẮT BUỘC GIỮ JAVASCRIPT (KEEP JAVASCRIPT)** cho tất cả các request bất đồng bộ.

### Rủi ro 8: Hành vi Đóng/Mở Modal, Backdrop và Phím Escape
* **Đề xuất Task 01:** Thay thế Bootstrap Modal bằng thẻ `<dialog>` native hoặc CSS `:target` / checkbox hack.
* **Thực tế mã nguồn:**
  * Hệ thống giao diện của RMS dựa trên Bootstrap 5 modal framework (`bootstrap.Modal.getOrCreateInstance()`).
  * Khi mở modal, JavaScript thực hiện việc đọc dữ liệu động (Offer ID, tên ứng viên, vị trí, mức lương) từ phần tử kích hoạt và điền vào các thẻ trong modal trước khi hiển thị.
  * CSS thuần không thể truyền dữ liệu động vào nội dung modal.
  * Hơn nữa, việc gỡ bỏ Bootstrap modal JS sẽ làm mất tính năng bẫy tiêu điểm (focus trapping), đóng modal bằng phím `Escape`, và cơ chế tự động dọn dẹp class `.modal-open` trên thẻ `<body>`.
* **Kết luận an toàn:** **BẮT BUỘC GIỮ JAVASCRIPT (KEEP JAVASCRIPT)**.

### Rủi ro 9: Hiển thị và Tự động Ẩn Thông báo Flash (Flash Messages)
* **Đề xuất Task 01:** Thay thế JavaScript ẩn thông báo bằng CSS Animation `@keyframes fadeOut`.
* **Thực tế mã nguồn:**
  * Cơ chế Toast/Flash hiện tại được điều khiển bởi tệp [`src/main/resources/static/js/flash-toast.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/flash-toast.js).
  * File này thiết lập bộ đếm thời gian 4 giây, làm mờ dần và sau đó **xóa hẳn phần tử DOM** khỏi cây giao diện (`toast.remove()`).
  * Nếu chỉ dùng CSS animation `opacity: 0`, phần tử thông báo vẫn tồn tại vô hình trên cây DOM và có thể chặn đứng thao tác nhấp chuột của người dùng vào các nút bấm nằm bên dưới (Click blocking overlay bug).
* **Kết luận an toàn:** **BỊ CHẶN (BLOCKED)** do phụ thuộc vào tệp dùng chung của hệ thống.

### Rủi ro 10: Ràng buộc Tệp tin Dùng chung Toàn dự án (Cross-Module Dependencies)
* **Thực tế sở hữu:**
  * `flash-toast.js`: Dùng chung cho 19+ màn hình thuộc Core, Auth, Job, Candidate, Interview.
  * `main.css`: Định kiểu toàn cục cho giao diện toàn hệ thống RMS.
  * `topbar-notifications.js`: Polling thông báo thời gian thực trên thanh header dùng chung.
* **Kết luận an toàn:** **TUYỆT ĐỐI KHÔNG CHỈNH SỬA (STRICTLY BLOCKED)**. Bất kỳ sự thay đổi nào trên các tệp này đều có nguy cơ gây lỗi hồi quy trên phân hệ của các thành viên khác.

---

## 3. Xác minh Quyền Sở hữu Mã nguồn (Task C — Source Code Ownership)

| Đường dẫn Tệp tin | Loại tệp | Người sở hữu thực tế | Mức độ an toàn can thiệp | Ghi chú & Ràng buộc |
| :--- | :---: | :---: | :---: | :--- |
| `src/main/resources/templates/offers/*.html` | HTML Template | HuyenPT | **AN TOÀN CÓ KIỂM SOÁT** | Màn hình danh sách, form tạo, chi tiết đề xuất của HR và Director |
| `src/main/resources/templates/candidate/offers/*.html`| HTML Template | HuyenPT | **AN TOÀN CÓ KIỂM SOÁT** | Màn hình xem và phản hồi offer của Ứng viên |
| `src/main/resources/static/js/offers.js` | JavaScript | HuyenPT | **AN TOÀN CÓ KIỂM SOÁT** | 2,005 dòng code xử lý Offer cho HR và Giám đốc |
| `src/main/resources/static/js/pages/candidate-offers.js`| JavaScript | HuyenPT | **AN TOÀN CÓ KIỂM SOÁT** | 285 dòng code đếm ngược và xử lý phản hồi ứng viên |
| `src/main/resources/static/css/offers.css` | CSS Stylesheet | HuyenPT | **AN TOÀN TUYỆT ĐỐI** | File CSS riêng của phân hệ Offer |
| `src/main/resources/static/js/flash-toast.js` | JavaScript | Chung (Shared) | **BỊ CHẶN (BLOCKED)** | Không được chỉnh sửa dưới mọi hình thức |
| `src/main/resources/static/js/topbar-notifications.js`| JavaScript | Chung (Shared) | **BỊ CHẶN (BLOCKED)** | Không được chỉnh sửa dưới mọi hình thức |
| `src/main/resources/static/css/main.css` | CSS Stylesheet | Chung (Shared) | **BỊ CHẶN (BLOCKED)** | Không được chỉnh sửa dưới mọi hình thức |
| `src/main/resources/templates/layout/*.html` | Fragment Template | Chung (Shared) | **BỊ CHẶN (BLOCKED)** | Layout chung của hệ thống |

---

## 4. Tổng hợp Đánh giá An toàn

1. **Sai lệch nghiêm trọng nhất trong Task 01** là đề xuất chuyển đổi API duyệt/từ chối từ Fetch JSON sang Form POST native. Đề xuất này vi phạm trực tiếp ràng buộc backend, sẽ gây lỗi HTTP 415 trên môi trường production.
2. **Số lượng test kiểm thử cơ sở thực tế là 223 ca kiểm thử**, bảo đảm 100% tỷ lệ thành công (PASS).
3. **Phần lớn các tính năng nghiệp vụ Offer yêu cầu trạng thái động** (dynamic DOM, real-time recalculation, dynamic modal population, CSRF headers), do đó JavaScript là thành phần không thể thay thế hoàn toàn bằng HTML/CSS thuần mà chỉ có thể tối ưu hóa và bổ sung thuộc tính bảo vệ tầng client.

