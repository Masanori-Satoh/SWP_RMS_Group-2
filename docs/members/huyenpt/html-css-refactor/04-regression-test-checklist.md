> Ghi chú 11/11/2026: đường dẫn và package test đã được cập nhật theo tầng kiểm thử; kết quả Task/PR lịch sử giữ nguyên.

# DANH MỤC KIỂM THỬ HỒI QUY & TIÊU CHÍ AN TOÀN (REGRESSION TEST CHECKLIST)

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).
**Nhiệm vụ:** Task 01 — Safe HTML/CSS Refactoring Audit<br>
**Mục tiêu:** Xác định đường cơ sở an toàn (Safety Baseline) và tiêu chí nghiệm thu khách quan cho mọi thay đổi giao diện.<br>
**Tài liệu tham chiếu:** `01-project-audit.md`, `02-js-classification.md`, `offer-management-defense-practice.md`<br>

---

## 1. Bộ Kiểm thử Tự động Sẵn có & Lệnh Thực thi (Automated Test Baseline)

Trước và sau bất kỳ thay đổi nào liên quan đến HTML/CSS, bộ kiểm thử tự động của Maven bắt buộc phải được kích hoạt để đảm bảo không làm gãy vỡ các luồng backend và giao tiếp MVC.

### 1.1. Danh mục 7 Suite Kiểm thử Tự động Thuộc Phân hệ Offer

| Tên Test Suite | Đường dẫn tệp tin | Số lượng ca test | Phạm vi kiểm tra |
| :--- | :--- | :---: | :--- |
| **OfferIntegrationTests** | `src/test/java/com/group2/rms/offer/controller/OfferIntegrationTests.java` | 27 ca test | Tích hợp MVC MockMvc, bảo mật phân quyền URL, CSRF token, model binding |
| **OfferServiceTests** | `src/test/java/com/group2/rms/offer/service/OfferServiceTests.java` | 41 ca test | Logic tính lương 85%, nộp duyệt, soft delete, deadline calculation |
| **CandidateOfferWebTests**| `src/test/java/com/group2/rms/offer/controller/CandidateOfferWebTests.java` | 19 ca test | Bảo mật Cổng ứng viên, chống IDOR, kiểm tra nút bấm hiển thị theo trạng thái |
| **CandidateOfferServiceTests**| `src/test/java/com/group2/rms/offer/service/CandidateOfferServiceTests.java`| 18 ca test | Nghiệp vụ ứng viên chấp thuận, từ chối, atomic update, đồng bộ Application |
| **OfferExportServiceTests**| `src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java` | 10 ca test | Xuất Excel Apache POI theo 3 phạm vi (FILTERED, SELECTED, ALL) |
| **OfferControllerBugTests**| `src/test/java/com/group2/rms/offer/controller/OfferControllerBugTests.java` | 6 ca test | Kiểm thử hồi quy các bug cũ (`BUG-OFF-01`, `BUG-OFF-11`, filter Giám đốc) |
| **CandidateOfferQueryTests**| `src/test/java/com/group2/rms/offer/repository/CandidateOfferQueryTests.java` | 5 ca test | Kiểm thử câu lệnh JPA Query, JOIN FETCH và khóa nguyên tử CSDL |
| **TỔNG CỘNG** | | **126 ca test** | **BẮT BUỘC 100% PASS TRƯỚC VÀ SAU KHI REFACTOR** |

### 1.2. Lệnh Thực thi Kiểm thử Tự động qua Terminal
* **Chạy toàn bộ 126 ca test của phân hệ Offer:**
  ```powershell
  mvn test -Dtest=*Offer*
  ```
* **Chạy riêng rẽ từng bộ test giao diện MVC quan trọng:**
  ```powershell
  mvn test -Dtest=OfferIntegrationTests
  mvn test -Dtest=CandidateOfferWebTests
  mvn test -Dtest=OfferControllerBugTests
  ```

---

## 2. Ma trận 7 Hành trình Người dùng Cốt lõi (Critical User Journeys)

Mỗi hành trình dưới đây mô tả chính xác trải nghiệm mà người dùng (HR, Director, Candidate) phải trải qua, đóng vai trò làm kịch bản kiểm thử thủ công sau khi chuyển đổi:

### Journey 1: HR Tạo mới Bản nháp & Nộp trình Giám đốc
1. HR đăng nhập `lan.hr@rms-tech.vn` $\rightarrow$ Truy cập `/offers`.
2. Bấm nút **"Tạo mới Thư mời"** $\rightarrow$ Mở trang `/offers/create`.
3. Chọn ứng viên trúng tuyển từ danh sách.
4. Nhập Lương chính thức `20,000,000` $\rightarrow$ Hệ thống tự động điền gợi ý Lương thử việc `17,000,000` (85%).
5. Kiểm tra ô "Ngày bắt đầu dự kiến": Các ngày quá khứ bị khóa, không thể chọn.
6. Bấm nút **"Lưu bản nháp"** $\rightarrow$ Hệ thống chuyển về `/offers`, hiển thị Toast xanh: *"Đã lưu bản nháp đề xuất nhận việc thành công!"*, thẻ KPI "Bản nháp" tăng 1.
7. Mở xem chi tiết bản nháp $\rightarrow$ Bấm nút **"Trình Giám đốc phê duyệt"** $\rightarrow$ Thẻ KPI chuyển sang "Chờ Giám đốc duyệt" (`Pending_Director`).

### Journey 2: HR Chỉnh sửa & Xóa mềm Bản nháp
1. HR mở chi tiết một Offer ở trạng thái `Draft`.
2. Bấm nút **"Chỉnh sửa"** $\rightarrow$ Mở trang `/offers/{id}/edit` với đầy đủ dữ liệu cũ.
3. Thay đổi mức lương hoặc địa điểm làm việc $\rightarrow$ Bấm Lưu $\rightarrow$ Cập nhật thành công.
4. Bấm nút **"Xóa bản nháp"** $\rightarrow$ Modal native `<dialog>` xác nhận mở ra $\rightarrow$ Bấm Xác nhận xóa $\rightarrow$ Bản ghi bị ẩn khỏi bảng (Database cập nhật `IsDeleted = 1`).

### Journey 3: Giám đốc Phê duyệt & Từ chối Đề xuất
1. Giám đốc đăng nhập `hoang.director@rms-tech.vn` $\rightarrow$ Truy cập `/offers`.
2. Giao diện tự động render bàn làm việc Giám đốc (`offers/director-list.html`).
3. Click vào thẻ KPI "Chờ phê duyệt" $\rightarrow$ Bảng tự động lọc các bản ghi `Pending_Director`.
4. Bấm nút **"Phê duyệt"** $\rightarrow$ Modal mở ra $\rightarrow$ Nhập ý kiến tùy chọn $\rightarrow$ Bấm "Xác nhận Phê duyệt" $\rightarrow$ Thành công, Offer chuyển sang `Director_Approved`.
5. Bấm nút **"Từ chối"** trên một bản ghi khác $\rightarrow$ Cố tình để trống ô nhận xét và bấm nút $\rightarrow$ **Hệ thống bắt buộc chặn lại và báo lỗi**. Nhập lý do hợp lệ $\rightarrow$ Offer chuyển sang `Director_Rejected`.

### Journey 4: HR Phát hành Thư mời tới Ứng viên
1. HR mở chi tiết một Offer đã được duyệt (`Director_Approved`).
2. Nút **"Phát hành Thư mời"** (`#btnSendOffer`) hiển thị nổi bật.
3. Bấm nút $\rightarrow$ Modal xác nhận mở ra kèm thông tin hạn phản hồi $\rightarrow$ Bấm Xác nhận.
4. Hệ thống cập nhật:
   * Trạng thái Offer chuyển `Sent_Candidate`.
   * Cột `ResponseDeadline` được chốt mốc thời gian.
   * Trạng thái hồ sơ `Application` chuyển sang `Offered`.
   * Email Offer Letter chính thức được gửi tới hòm thư ứng viên.

### Journey 5: Ứng viên Xem & Chấp thuận Nhận việc
1. Ứng viên đăng nhập tài khoản Candidate $\rightarrow$ Truy cập `/portal/offers`.
2. Bấm vào thư mời vừa nhận $\rightarrow$ Mở trang chi tiết `/portal/offers/{id}` dạng letterhead trang trọng.
3. Đồng hồ đếm ngược hiển thị thời gian còn lại chính xác từng giây.
4. Bấm nút **"Đồng ý nhận việc"** $\rightarrow$ Modal native `<dialog id="acceptModal">` mở ra.
5. Bấm "Xác nhận Đồng ý" $\rightarrow$ Trang tải lại với biểu ngữ chúc mừng: *"Chúc mừng bạn đã chính thức gia nhập Mộc RMS!"*.
6. Hồ sơ ứng tuyển tự động chuyển sang trạng thái **`Hired`**. Các nút thao tác biến mất hoàn toàn.

### Journey 6: Ứng viên Xem & Từ chối Nhận việc
1. Ứng viên mở chi tiết thư mời $\rightarrow$ Bấm nút **"Từ chối Thư mời"**.
2. Modal khảo sát lý do mở ra $\rightarrow$ Click vào chip gợi ý *"Tôi đã nhận được lời mời khác phù hợp hơn"*.
3. Bấm "Xác nhận Từ chối" $\rightarrow$ Offer chuyển `Declined`, lưu lý do vào `DeclineReason`, hồ sơ ứng tuyển chuyển sang **`Rejected`**.

### Journey 7: HR Xuất Dữ liệu Offer ra Excel
1. HR truy cập `/offers` $\rightarrow$ Bấm nút **"Xuất Excel"**.
2. Modal xuất mở ra: Cho phép chọn phạm vi (Toàn bộ, Đang lọc, Đã chọn) và tùy chọn tích/bỏ tích 22 cột dữ liệu.
3. Bấm "Xuất Báo Cáo" $\rightarrow$ Trình duyệt tự động tải xuống file `.xlsx` định dạng chuẩn.

---

## 3. Khoảng trống Kiểm thử Tự động (Test Coverage Gaps) & Kiểm thử Thủ công Bắt buộc

Hiện tại, bộ kiểm thử của dự án chủ yếu tập trung ở tầng Backend và Controller MockMvc. Tầng giao diện trình duyệt thật đang tồn tại các khoảng trống cần được kiểm thử thủ công nghiêm ngặt:

| Khoảng trống kiểm thử (Gap) | Lý do MockMvc không bắt được | Kịch bản kiểm thử thủ công bắt buộc |
| :--- | :--- | :--- |
| **Hành vi mở/đóng Modal `<dialog>`** | MockMvc chỉ kiểm tra chuỗi HTML trả về, không khởi chạy DOM engine của trình duyệt để test phương thức `.showModal()` hay `.close()`. | Thử mở modal, nhấn phím `Escape` trên bàn phím, click chuột ra vùng nền mờ (backdrop) xem modal có đóng mượt mà không. |
| **Hiệu ứng đếm ngược Countdown Timer** | `setInterval()` chỉ chạy trong môi trường JavaScript Runtime của trình duyệt thật. | Quan sát đồng hồ giây giảm đều đặn từ 59 về 00 trên màn hình ứng viên. |
| **Tự động điền 85% lương** | Sự kiện `input` trên thẻ HTML không được MockMvc kích hoạt. | Gõ mức lương chính thức và kiểm tra ô lương thử việc có tự nhảy số tiền tương ứng không. |
| **Giao diện hiển thị khi quá hạn** | MockMvc không mô phỏng được độ trễ thời gian thực tế. | Chỉnh lùi thời hạn phản hồi trong database và xem các nút bấm có bị vô hiệu hóa kèm nhãn màu xám hay không. |

---

## 4. Tiêu chí Nghiệm thu Khách quan cho Từng Thay đổi (Acceptance Criteria)

Bất kỳ thành phần nào sau khi được chuyển đổi từ JavaScript sang HTML/CSS đều phải đáp ứng đầy đủ **5 Tiêu chí Vàng** sau đây trước khi được coi là thành công:

1. **Tiêu chí 1 (Green Test Suite):** Chạy `mvn test -Dtest=*Offer*` đạt **126/126 ca test PASS 100%**, không có bất kỳ lỗi biên dịch (Compilation Error) hay hồi quy logic nào.
2. **Tiêu chí 2 (Visual Parity):** Giao diện trước và sau khi refactor phải giống nhau **99.9%** về kích thước, màu sắc, font chữ, hiệu ứng chuyển động trên cả 3 trình duyệt phổ biến (Google Chrome, Microsoft Edge, Mozilla Firefox).
3. **Tiêu chí 3 (No Inline Script Cheat):** Tuyệt đối không được "lách luật" bằng cách chuyển mã JavaScript từ file `.js` sang các thuộc tính HTML nội tuyến như `onclick="..."`, `onchange="..."` hoặc thẻ `<script>` trong file HTML.
4. **Tiêu chí 4 (CSRF & Security Preservation):** Mọi form submit mới đều phải gửi kèm CSRF Token hợp lệ, không gây ra bất kỳ lỗi HTTP 403 Forbidden hay 415 Unsupported Media Type nào.
5. **Tiêu chí 5 (Zero Cross-Module Pollution):** Các file dùng chung (`flash-toast.js`, `main.css`, `topbar-notifications.js`) hoàn toàn không bị chỉnh sửa một dòng code nào.
