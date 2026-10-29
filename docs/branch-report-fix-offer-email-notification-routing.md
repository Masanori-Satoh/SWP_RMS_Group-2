# BÁO CÁO TỔNG KẾT NHÁNH: `fix/offer-email-notification-routing`

---

## 1. Tình Trạng Nhánh & Kiểm Tra Xung Đột (Health Check)

* **Tên nhánh hiện tại:** `fix/offer-email-notification-routing`
* **Nhánh cơ sở (Base branch):** `main` (`origin/main`)
* **Tình trạng xung đột (Merge Conflict Status):** 
  * ✅ **HOÀN TOÀN KHÔNG CÓ XUNG ĐỘT (0 Conflict)**.
  * Nhánh đã hợp nhất (merged) commit mới nhất từ `origin/main` (`a20cc3c - Feature/update UI authen`).
  * Trạng thái Git: `Working tree clean` (Không có thay đổi tồn đọng chưa commit).
* **Kết quả kiểm thử tự động (Automated Tests):**
  * ✅ **19/19 Test Cases ĐẠT (BUILD SUCCESS)**:
    * `com.group2.rms.offer.CandidateOfferWebTests` (16 test cases).
    * `com.group2.rms.offer.controller.OfferControllerBugTests` (3 test cases).

---

## 2. Tổng Hợp Các Thay Đổi & Tính Năng Đã Thực Hiện Trên Nhánh

Nhánh `fix/offer-email-notification-routing` bao gồm 5 nhóm công việc chính:

### Nhóm 1: Cố Định Ứng Viên Khi Tạo Offer Từ Ngữ Cảnh Hồ Sơ (Contextual Offer Creation)
* **Vấn đề giải quyết:** Trước đây, khi nhân viên HR bấm nút "Tạo đề xuất nhận việc" từ danh sách hồ sơ ứng viên (`/applications?stage=OFFER`) hoặc trang chi tiết hồ sơ (`/applications/{id}`), màn hình tạo offer vẫn hiển thị thanh dropdown cho phép chọn người khác. Điều này dẫn tới nguy cơ nhân viên chọn nhầm hoặc vô tình lăn chuột đổi ứng viên khác.
* **Giải pháp thực hiện:**
  * **Tại Controller ([OfferController.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java)):** Tiếp nhận tham số `applicationId`. Nếu có, kích hoạt cờ `isLockedCandidate = true`, tìm ứng viên đỗ phỏng vấn và khóa cứng vào Model (`fixedCandidate`). Bảo lưu trạng thái này kể cả khi form submit gặp lỗi validation.
  * **Tại View ([form.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/form.html)):**
    * Khi có `applicationId`: Thay thế thanh dropdown bằng một ô hiển thị cố định tĩnh (`.offer-candidate-select-fixed`) hiển thị đúng dòng họ tên và vị trí của ứng viên, không có mũi tên chọn, không cho phép đổi sang người khác, gửi ID qua `<input type="hidden">`.
    * Khi tạo từ trang Quản lý Offer (`/offers`): Vẫn giữ nguyên dropdown để HR tự do tìm và chọn ứng viên trong danh sách.
  * **Tại CSS ([offers.css](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/offers.css)):** Định kiểu `.offer-candidate-select-fixed` tiệp màu và kích thước với form-select gốc, không làm vỡ bố cục.

---

### Nhóm 2: Khắc Phục Lỗi Hiển Thị Dấu Tiếng Việt & Chuẩn Hóa Phông Chữ (Typography Fixes)
* **Vấn đề giải quyết:** Tiêu đề modal xác nhận chấp nhận thư mời bị lỗi đứt khoảng cách thành `Chấ  p nhận thư mời thành công`.
* **Nguyên nhân:** Lớp `.cof-accept-success-title` gọi font `Playfair Display, Georgia, serif` kèm `letter-spacing: -0.01em;`. Trình duyệt Windows fallback sang `Georgia` thiếu ký tự ghép dấu tiếng Việt `ấ` (`U+1EA5`), dẫn đến font-linking bị lệch advance width.
* **Giải pháp thực hiện:**
  * Đồng bộ tiêu đề modal về font chuẩn hệ thống `var(--serif)` ([candidate-offers.css](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/pages/candidate-offers.css)) — liên kết trực tiếp tới font `Lora` offline trong [fonts.css](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/fonts.css) đã hỗ trợ trọn vẹn dải unicode tiếng Việt `U+1EA0-1EF9`.
  * Thêm alias `--font-serif` và `--font-sans` vào [tokens.css](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/tokens.css) để triệt tiêu mọi nguy cơ fallback lỗi font trên toàn ứng dụng.
  * Rà soát 100% các màn hình và tệp style trong luồng Offer, đảm bảo tuân thủ nghiêm ngặt chuẩn font hệ thống: `Lora` cho tiêu đề và `Plus Jakarta Sans / Inter` cho nội dung, bảng biểu và nút bấm.

---

### Nhóm 3: Việt Hóa Toàn Diện Phân Hệ Đề Xuất & Thư Mời Nhận Việc (Localization)
* **Giao diện Ứng viên:**
  * [candidate-list.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-list.html): Danh sách thư mời, nhãn trạng thái, hạn phản hồi, nút xem chi tiết.
  * [candidate-detail.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-detail.html): Thư mời trang trọng, điều khoản hợp đồng, đãi ngộ, modal đồng ý nhận việc, hướng dẫn bước tiếp theo.
  * [candidate-decline.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-decline.html): Khảo sát lý do từ chối (chips lý do), cảnh báo quyết định.
* **Giao diện Quản trị Nội bộ:**
  * [list.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/list.html): Thẻ KPI thống kê, bộ lọc trạng thái, bảng danh sách đề xuất lương, modal xuất Excel.
  * [form.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/form.html): Nhóm thông tin ứng viên (Group A), điều khoản đãi ngộ (Group B), hướng dẫn tỷ lệ 85% lương thử việc.
  * [detail.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/detail.html): Chi tiết đề xuất nội bộ, drawer lịch sử phê duyệt của Giám đốc.
  * [director-list.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/director-list.html): Màn hình Giám đốc phê duyệt, modal từ chối/đồng ý đề xuất.
* **Dịch vụ Backend:**
  * [OfferExportServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferExportServiceImpl.java): Xuất báo cáo Excel với tiêu đề cột và trạng thái thuần tiếng Việt.
  * [NotificationServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/notification/NotificationServiceImpl.java): Thông báo In-App tiếng Việt chuẩn mực.

---

### Nhóm 4: Bố Cục Thư Mời Nhận Việc Trang Trọng (Letterhead Layout Alignment)
* Thiết kế thư mời chính thức (`/portal/offers/{id}`) theo phong cách thư tuyển dụng doanh nghiệp:
  * Khối Letterhead: Logo Mộc RMS, thông tin công ty, ngày phát hành, lời chào trang trọng.
  * Thẻ tóm tắt thông số quan trọng (Chức danh, Mức lương, Ngày bắt đầu, Hạn phản hồi).
  * Chi tiết 6 mục quyền lợi và điều khoản (Lương thưởng, Thử việc, Chế độ BHXH, Thời gian & Địa điểm làm việc, Bảo mật thông tin).
  * Điều chỉnh khối tải thư mời (Download PDF) đặt dưới bảng hướng dẫn bước tiếp theo, loại bỏ nút liên hệ HR thừa ở thanh tải về để tránh lặp giao diện.
  * Căn chỉnh thanh tiến trình stepper ngang chuẩn xác, không bị lệch lề.

---

### Nhóm 5: Tối Ưu Định Tuyến Thông Báo & Gửi Email Thư Mời (Notification Routing)
* **Chính sách phân tách email & in-app:**
  * Email qua giao thức SMTP ([OfferMailSender.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/notification/OfferMailSender.java)) chỉ phát hành tới **Ứng viên** khi HR chính thức gửi Offer Letter.
  * Toàn bộ quy trình nội bộ giữa HR và Giám đốc (Trình duyệt, Phê duyệt, Từ chối) được chuyển sang **Thông báo In-App**, phát sóng tức thì tới toàn bộ chuyên viên HR.
* **Hỗ trợ môi trường phát triển (Dev Environment Routing):**
  * Tự động điều hướng email thư mời về địa chỉ email cấu hình trong `app.mail.from` khi test cục bộ, giúp lập trình viên/kiểm thử viên nhận thư trực tiếp mà không cần dùng email thật của ứng viên mẫu.

---

## 3. Thống Kê Thay Đổi Tệp (Git Diff Summary)

* **Tổng số tệp thay đổi:** 31 tệp.
* **Thống kê dòng:** `+2364` dòng thêm mới, `-644` dòng lược bỏ/cập nhật.

| Phân tầng | Tệp nguồn thay đổi | Mô tả ngắn |
| :--- | :--- | :--- |
| **Backend Controller** | `OfferController.java`<br>`CandidateOfferController.java` | Xử lý khóa ứng viên theo ngữ cảnh, API trả về ApiResponse chuẩn. |
| **Backend Service** | `OfferServiceImpl.java`<br>`CandidateOfferServiceImpl.java`<br>`OfferExportServiceImpl.java`<br>`OfferMailSender.java`<br>`NotificationServiceImpl.java` | Logic nghiệp vụ offer, lọc ứng viên đỗ phỏng vấn, xuất Excel tiếng Việt, định tuyến email. |
| **Model & Entity** | `OfferApproval.java`<br>`CandidateOfferDetailResponse.java`<br>`User.java` | Quản lý lịch sử duyệt của Giám đốc, mở rộng DTO mã thư mời, chức danh người báo cáo. |
| **View Template** | `offers/form.html`<br>`offers/detail.html`<br>`offers/list.html`<br>`offers/director-list.html`<br>`offers/candidate-detail.html`<br>`offers/candidate-list.html`<br>`offers/candidate-decline.html`<br>`candidate/applications.html` | Cố định ứng viên tạo offer, Việt hóa giao diện, thiết kế thư mời trang trọng. |
| **Styling & Assets** | `css/offers.css`<br>`css/tokens.css`<br>`css/pages/candidate-offers.css` | Style cố định thanh chọn, sửa lỗi font `var(--serif)` tiếng Việt, CSS in ấn và letterhead. |
| **JavaScript** | `js/offers.js`<br>`js/pages/candidate-offers.js` | Tự động hóa tính lương 85%, xử lý an toàn khi không có dropdown. |
| **Automated Tests** | `OfferControllerBugTests.java`<br>`CandidateOfferWebTests.java`<br>`NotificationServiceImplTest.java` | Bổ sung unit test xác minh cờ khóa ứng viên, đảm bảo toàn bộ 19 test case xanh. |

---

## 4. Kết Luận & Khuyến Nghị

1. **Độ an toàn nhánh:** Nhánh đã được kiểm tra xung đột với `origin/main` mới nhất, toàn bộ mã nguồn biên dịch sạch sẽ, không có lỗi runtime hay hồi quy (regression).
2. **Khuyến nghị bước tiếp theo:** Nhánh đã hoàn toàn sẵn sàng để tạo **Pull Request (PR)** hợp nhất vào nhánh `main`.
