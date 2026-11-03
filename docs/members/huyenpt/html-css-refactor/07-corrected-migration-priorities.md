# BẢNG ĐIỀU CHỈNH THỨ TỰ ƯU TIÊN CHUYỂN ĐỔI (CORRECTED MIGRATION PRIORITIES)
**Nhiệm vụ:** Task 01.5 — Pre-Refactor Safety Validation  
**Dự án:** SWP391 — Recruitment Management System (RMS) — Group 2  
**Phân hệ:** Quản lý Đề xuất & Thư mời nhận việc (Offer Management)  
**Nhà phát triển phụ trách:** HuyenPT  
**Trạng thái nhánh Git:** `refactor/huyenpt/offer-ui-js-to-html-css`  
**Quy tắc:** Chỉ đề xuất kế hoạch khả thi sau khi đã kiểm tra thực tế mã nguồn, tôn trọng 100% ràng buộc hệ thống.

---

## 1. Ma trận Tái phân loại Ứng viên Chuyển đổi (Task D — Candidate Reclassification Matrix)

Sau khi rà soát toàn diện mã nguồn backend (`OfferController.java`), giao diện Thymeleaf (`*.html`), và kịch bản frontend (`offers.js`, `candidate-offers.js`), toàn bộ các thành phần đề xuất ban đầu trong Task 01 được tái phân loại thành 4 nhóm chính xác:

| Nhóm phân loại | Ý nghĩa & Điều kiện | Nguyên tắc ứng xử |
| :--- | :--- | :--- |
| **SAFE** | 100% an toàn, thuần HTML/CSS, không ảnh hưởng API/Backend, không làm gãy JS | Có thể triển khai từng bước |
| **CONDITIONAL** | Có thể bổ sung HTML/CSS làm tầng phòng thủ tĩnh, nhưng **bắt buộc giữ JS** cho hành vi động | Triển khai mô hình "phòng thủ kép" (dual-layer), không xóa JS |
| **UNSAFE** | Phá vỡ định dạng dữ liệu (HTTP 415), lỗi form không hợp lệ, hoặc hỏng nghiệp vụ | **Loại bỏ hoàn toàn khỏi kế hoạch chuyển đổi** |
| **KEEP JAVASCRIPT** | Các tính năng phụ thuộc xử lý thời gian thực, truyền nhận bất đồng bộ hoặc chia sẻ toàn cục | **Bảo tồn nguyên vẹn 100% mã nguồn JavaScript** |

---

### Bảng Chi tiết Đánh giá & Điều chỉnh Toàn bộ 10 Ứng viên

| STT | Thành phần / Tính năng | Hiện trạng mã nguồn | Đề xuất cũ (Task 01) | Phân loại ĐÃ ĐIỀU CHỈNH | Bằng chứng kỹ thuật & Rủi ro thực tế | Khuyến nghị chuyển đổi an toàn | Chiến lược Hoàn nguyên (Rollback) |
| :---: | :--- | :--- | :--- | :---: | :--- | :--- | :--- |
| **1** | **Giới hạn độ dài lý do từ chối của Giám đốc** | JS bắt sự kiện `input`, cảnh báo nếu vượt 2000 ký tự | Dùng `maxlength="2000"` và CSS `:user-invalid`, xóa JS | **SAFE** | Backend có `@Size(max = 2000)` trong `DirectorDecisionRequest`. Thuộc tính HTML5 `maxlength` chặn ngay từ bàn phím mà không ảnh hưởng gì tới DOM hay event listener. | **Thêm thuộc tính `maxlength="2000"` vào thẻ `<textarea id="directorDecisionComments">`**. Vẫn giữ nguyên JS đếm ký tự để giữ trải nghiệm người dùng trực quan. | `git checkout src/main/resources/templates/offers/director-list.html` |
| **2** | **Giới hạn độ dài lý do từ chối của Ứng viên** | JS bắt sự kiện `input` trên `#declineReason` | Dùng `maxlength="2000"` | **SAFE** | Backend có `@Size(max = 2000)` trong `CandidateOfferDecisionRequest`. Thuộc tính native chuẩn xác 100%. | **Thêm thuộc tính `maxlength="2000"` vào `<textarea id="declineReason">`**. Giữ nguyên logic JS hiện có. | `git checkout src/main/resources/templates/candidate/offers/candidate-detail.html` |
| **3** | **Khởi tạo mốc Ngày bắt đầu tối thiểu khi tải trang** | JS gọi `initOfferFormExpectedStartDateMin` khi load DOM | Dùng `th:min` thay thế hoàn toàn JS | **CONDITIONAL** | `th:min` chỉ chạy 1 lần trên server. Khi người dùng thay đổi `responsePeriodDays` từ 3 lên 7 ngày, HTML không thể tự cập nhật `minDate` nếu thiếu JS. | **Thêm `th:min` làm mốc khởi tạo ban đầu an toàn (dual-layer), nhưng BẮT BUỘC GIỮ hàm `updateMinExpectedStartDate()` trong JS**. | `git checkout src/main/resources/templates/offers/form.html` |
| **4** | **Điều hướng Bộ lọc Trạng thái qua Thẻ KPI** | JS bắt sự kiện `click` trên `.stat-card`, đọc `data-status`, gán form | Đổi thẻ `<div>` thành thẻ `<a>` kèm `th:href` | **CONDITIONAL** | Chuyển sang thẻ `<a>` thuần túy có thể gây mất các tham số tìm kiếm khác (như `keyword`, `departmentId`) nếu không bảo tồn URL query params. | Giữ nguyên JS xử lý submit bộ lọc form để giữ trọn vẹn trạng thái lọc đa tiêu chí của HR. | `git checkout src/main/resources/templates/offers/list.html` |
| **5** | **Xác thực Lý do Từ chối bắt buộc của Giám đốc** | JS kiểm tra nếu action là `reject` thì `comments` không được trống | Đặt thuộc tính `required` trực tiếp trên thẻ `<textarea>` | **CONDITIONAL** | Modal dùng chung cho cả Phê duyệt và Từ chối. Khi Phê duyệt, comment là không bắt buộc. Đặt `required` tĩnh sẽ chặn Giám đốc Phê duyệt! | **BẮT BUỘC GIỮ JavaScript** để kiểm tra tính bắt buộc của comment dựa trên ngữ cảnh nút bấm. | Không sửa đổi code |
| **6** | **Chuyển đổi Modal Duyệt của Giám đốc sang Form POST** | Fetch API gửi payload JSON tới endpoint `consumes = MediaType.APPLICATION_JSON_VALUE` | Chuyển thành `<form method="POST">` native, xóa 150 dòng JS | **UNSAFE / BLOCKED** | `OfferController.java` dòng 462 & 483 chỉ nhận `application/json`. Gửi Form POST HTML sẽ bị lỗi **HTTP 415 Unsupported Media Type**. Không có redirect PRG trên backend. | **TUYỆT ĐỐI KHÔNG CHUYỂN ĐỔI**. Giữ nguyên Fetch API JSON và CSRF header. | Không sửa đổi code |
| **7** | **Nút Đặt lại Mặc định Modal Xuất Excel** | JS duyệt mảng checkbox, gán lại giá trị theo cấu hình chuẩn | Thay bằng `<input type="reset">`, xóa hàm `resetDefaultColumns()` | **UNSAFE** | Modal xuất Excel trong `list.html` không có thẻ `<form>`. Thẻ `<input type="reset">` đặt ngoài `<form>` là vô hiệu hoàn toàn theo chuẩn HTML. | **TUYỆT ĐỐI KHÔNG DÙNG `<input type="reset">`**. Giữ nguyên hàm JS `resetDefaultColumns()`. | Không sửa đổi code |
| **8** | **Đồng bộ Chip Lý do Từ chối của Ứng viên** | JS bắt sự kiện click chip, gán text vào textarea | Thay bằng thẻ `<input type="radio">` thuần | **KEEP JAVASCRIPT** | HTML/CSS thuần không thể copy nội dung từ chip radio vào ô textarea cho ứng viên chỉnh sửa bổ sung. | **BẮT BUỘC GIỮ JAVASCRIPT** cho logic đồng bộ chip vào textarea. | Không sửa đổi code |
| **9** | **Đóng/Mở Modal, Backdrop và Phím Escape** | Bootstrap 5 Modal API quản lý focus, backdrop và dọn dẹp body class | Thay bằng thẻ `<dialog>` native hoặc CSS `:target` | **KEEP JAVASCRIPT** | Cần JS để nạp động Offer ID, tên ứng viên, vị trí tuyển dụng vào modal trước khi mở. CSS thuần không truyền được tham số. | **BẮT BUỘC GIỮ JAVASCRIPT** và Bootstrap Modal. | Không sửa đổi code |
| **10**| **Quản lý Thông báo Flash Toast** | `flash-toast.js` hẹn giờ 4s, tạo hiệu ứng mờ dần và xóa phần tử DOM | Thay bằng CSS Pure Animation `@keyframes` | **BLOCKED** | Tệp `flash-toast.js` thuộc sở hữu chung của toàn dự án (19+ màn hình). Sửa đổi sẽ gây ảnh hưởng tới toàn bộ các thành viên khác. | **BẤT KHẢ XÂM PHẠM (BLOCKED)**. | Không sửa đổi code |

---

## 2. Lựa chọn Ứng viên Tái cấu trúc Đầu tiên An toàn Nhất (Task E — First Safe Candidate Recommendation)

Nhằm tuân thủ nguyên tắc cốt lõi: *"Một thành phần duy nhất — Rủi ro bằng không — Bảo toàn 100% hợp đồng API và mã nguồn đồng nghiệp"*, ứng viên được lựa chọn triển khai đầu tiên cho Task 02 là:

### Ứng viên Số 1: Bổ sung Thuộc tính Native `maxlength="2000"` cho Textarea Lý do/Ghi chú của Giám đốc

* **Tên thành phần:** Trường nhập ghi chú/lý do quyết định của Giám đốc (`directorDecisionComments`).
* **Tệp tin bị tác động DUY NHẤT:**
  [`src/main/resources/templates/offers/director-list.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/director-list.html)
* **Số dòng tác động:** Đúng 01 dòng duy nhất tại thẻ `<textarea>` bên trong `#directorDecisionModal`:
  ```html
  <!-- Hiện tại -->
  <textarea class="form-control" id="directorDecisionComments" rows="4" 
            placeholder="Nhập lý do từ chối hoặc ghi chú phê duyệt..."></textarea>

  <!-- Đề xuất bổ sung -->
  <textarea class="form-control" id="directorDecisionComments" rows="4" maxlength="2000"
            placeholder="Nhập lý do từ chối hoặc ghi chú phê duyệt..."></textarea>
  ```
* **Lý do khẳng định An toàn Tuyệt đối 100% (Safety Justification):**
  1. **Không sửa đổi Backend:** Khớp hoàn toàn với giới hạn độ dài `@Size(max = 2000)` đã định nghĩa sẵn trong `DirectorDecisionRequest.java`.
  2. **Không làm gãy JavaScript:** Thao tác này chỉ bổ sung thuộc tính HTML5 native. Mọi sự kiện lắng nghe bàn phím `input`, hàm đếm ký tự `handleDirectorDecisionCommentInput()`, và luồng gửi Fetch JSON trong `offers.js` vẫn hoạt động nguyên vẹn 100%.
  3. **Không ảnh hưởng tới thành viên khác:** Tệp `director-list.html` thuộc quyền sở hữu riêng của phân hệ Offer (HuyenPT).
  4. **Bảo toàn kết quả kiểm thử:** Toàn bộ 223 test case tự động hiện tại sẽ tiếp tục PASS 100%.
  5. **Khả năng Hoàn nguyên tức thì:** Nếu cần hủy bỏ, chỉ mất 1 giây thực hiện lệnh `git checkout src/main/resources/templates/offers/director-list.html`.

---

## 3. Rủi ro Còn lại & Yêu cầu Kiểm thử sau Tái cấu trúc

Trước khi tiến hành sửa đổi dòng mã trên trong Task 02, nhóm phát triển cần xác lập quy trình kiểm thử nghiệm thu như sau:

1. **Kiểm thử Tự động (Automated Verification):**
   * Chạy lại toàn bộ bộ test:
     ```powershell
     mvn test "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests"
     ```
   * Tiêu chuẩn đạt: **223/223 tests PASS (0 failures, 0 errors)**.
2. **Kiểm thử Trực quan Giao diện (Manual UI Inspection):**
   * Đăng nhập với quyền Giám đốc (`role: DIRECTOR`).
   * Truy cập `/director/offers`.
   * Mở modal phê duyệt/từ chối của một đề xuất chờ duyệt.
   * Thử dán một đoạn văn bản dài hơn 2000 ký tự $\rightarrow$ Trình duyệt phải tự động cắt đúng tại ký tự thứ 2000 mà không gây tràn khung hoặc treo giao diện.
   * Bấm "Phê duyệt" với nội dung ngắn $\rightarrow$ Request JSON được gửi thành công, toast thông báo hiển thị đúng.

---

> [!IMPORTANT]
> **DỪNG LẠI TẠI ĐÂY (STOP RULE APPLIED)**:  
> Báo cáo thẩm định an toàn Task 01.5 đã hoàn tất. Chưa có bất kỳ dòng mã nguồn ứng dụng nào bị thay đổi. Vui lòng xem xét các phát hiện và cung cấp phê duyệt rõ ràng trước khi chuyển sang bước thực hiện chỉnh sửa trong Task 02.

