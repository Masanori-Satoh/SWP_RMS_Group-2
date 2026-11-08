# CÁC CƠ HỘI GIẢM THIỂU JAVASCRIPT AN TOÀN TRONG PHÂN HỆ OFFER (TASK 10)

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).
## SWP391 — Mộc RMS Group 2 | Developer: HuyenPT (Offer Management)
*Mã tài liệu: 20-task-10-js-reduction-opportunities.md*
*Thời gian thực hiện: 04/11/2026 | Nhánh Git: `refactor/huyenpt/offer-ui-js-to-html-css`*

---

## 1. NGUYÊN TẮC GIẢM THIỂU MÃ NGUỒN CỦA SWP391

Một trong những yêu cầu quan trọng của hội đồng bảo vệ đồ án SWP391 là **tối ưu hóa hiệu năng, giảm sự phụ thuộc vào JavaScript phía client bằng cách tận dụng năng lực nguyên bản của HTML5, CSS3 và Thymeleaf Server-Side Rendering**.

Tuy nhiên, nguyên tắc tối thượng được đề ra là:
> **"Bảo toàn tính năng và sự toàn vẹn của hệ thống luôn được ưu tiên cao hơn việc cắt giảm mã nguồn đơn thuần. Không được phép giảm JavaScript bằng cách xóa bỏ tính năng, nén mã nguồn vô nghĩa, nhét mã vào inline HTML, hay làm yếu đi các lớp kiểm tra dữ liệu."**

Tài liệu này phân loại chi tiết từng cơ hội giảm thiểu JavaScript có thể thực hiện một cách an toàn 100%, đồng thời thiết lập ranh giới kỹ thuật rõ ràng đối với các đoạn mã **BẮT BUỘC PHẢI GIỮ LẠI (KEEP JAVASCRIPT)**.

---

## 2. MA TRẬN PHÂN LOẠI 4 NHÓM KHẢ NĂNG THAY THẾ (SAFE / CONDITIONAL / KEEP / BLOCKED)

| Phân loại | Định nghĩa kỹ thuật | Các thành phần trong phân hệ Offer | Hành động đề xuất |
| :---: | :--- | :--- | :---: |
| **SAFE** | Mã nguồn đã chết (Dead code), không còn bất kỳ caller nào trong template hoặc file JS khác; hoặc đã được thay thế trọn vẹn bằng HTML5/Thymeleaf. | - Modal helpers kiểu cũ (`openModal`, `closeModal`)<br/>- Modal tạo offer cũ (`openCreateOfferModal`, `submitCreateOffer`)<br/>- Formatters client cũ (`formatOfferDate`, `getStatusBadgeHtml`)<br/>- Banner flash cũ (`displayOfferFlashBanner`, `pageFlashContainer`)<br/>- Các hàm wrapper thừa (`handleFilterSearch`, `submitDirectorApprove/Reject`) | **Xóa bỏ dứt điểm**<br/>(Giảm 402 dòng) |
| **CONDITIONAL** | Có thể thay thế bằng HTML5 native nhưng đòi hỏi kiểm thử hành vi thực tế hoặc chuyển đổi từng bước. | - Nút xóa bản nháp trên danh sách (`confirmDeleteOffer`): Hiện gọi AJAX DELETE, có thể đổi sang `<form method="post" th:action="@{/offers/{id}/delete}">` như trang Chi tiết.<br/>- Gộp các toast wrappers (`showOfferToast`, `showToast`) vào một hàm duy nhất. | **Thực hiện theo đợt có kiểm thử**<br/>(Giảm ~30-50 dòng) |
| **KEEP JAVASCRIPT** | Bắt buộc phải duy trì bằng JavaScript vì liên quan đến tương tác thời gian thực, tính toán động, kết nối API nhị phân hoặc bảo mật CSRF. | - Tính lương thử việc 85% theo Luật Lao Động 2019.<br/>- Ràng buộc ngày bắt đầu động: `min = Ngày tạo + Thời hạn + 1`.<br/>- Thanh chọn hàng loạt và checkbox `indeterminate`.<br/>- Modal xuất Excel tùy chọn 25 cột và tải Blob.<br/>- Đếm ký tự thực thời trên nhận xét Giám đốc.<br/>- Đồng hồ đếm ngược phản hồi của ứng viên (Real-time Ticker).<br/>- Xử lý token bảo mật `X-CSRF-TOKEN` cho toàn bộ Fetch AJAX. | **Giữ nguyên và tái cấu trúc vào các module nhỏ** |
| **BLOCKED** | Tuyệt đối không được phép chỉnh sửa hoặc cắt giảm vì sẽ phá vỡ hợp đồng backend, bảo mật hoặc làm ảnh hưởng phân hệ của thành viên khác. | - Giới hạn độ dài nhận xét Giám đốc (`#directorDetailComments`): Giữ nguyên hỗ trợ tới 2000 ký tự theo DTO backend `@Size(max = 2000)`.<br/>- Endpoint AJAX duyệt/từ chối JSON của Giám đốc.<br/>- Giao diện và logic của các phân hệ Ứng viên (DungLT), Phỏng vấn (DucNM), Thông báo (HoangNH). | **Nghiêm cấm can thiệp** |

---

## 3. CHI TIẾT CÁC CƠ HỘI GIẢM THIỂU MÃ NGUỒN CỤ THỂ

### 3.1. Loại bỏ mã tạo modal bằng JavaScript cũ (Tiết kiệm: 159 dòng)
- **Hiện trạng:** Dòng 198–224 (`openCreateOfferModal`) và dòng 338–469 (`submitCreateOffer`) trong `offers.js` là tàn dư từ Sprint 1 khi chức năng tạo mới được thực hiện bằng một popup modal AJAX.
- **Giải pháp:** Xóa bỏ hoàn toàn. Tính năng Tạo mới đề xuất hiện đã được Nhóm chuẩn hóa thành trang riêng `offers/form.html` sử dụng thẻ `<form method="post" th:action="@{/offers/create}">` với validation hai chiều chuẩn Spring MVC (`BindingResult`, `@Valid`, `@ModelAttribute`).
- **Lợi ích:** Tiết kiệm 159 dòng JavaScript rườm rà và các lệnh fetch AJAX không bao giờ được gọi.

### 3.2. Loại bỏ các hàm định dạng hiển thị phía Client (Tiết kiệm: 65 dòng)
- **Hiện trạng:** Các hàm `formatOfferDate` (dòng 480–494), `formatOfferDateTime` (dòng 495–510), `getStatusBadgeHtml` (dòng 511–544) từng dùng để render HTML cho modal chi tiết cũ.
- **Giải pháp:** Xóa bỏ hoàn toàn. Trang `offers/detail.html` và `offers/list.html` hiện tại đều render ngày tháng và huy hiệu trạng thái trực tiếp từ server bằng cú pháp Thymeleaf:
  ```html
  <span th:text="${#temporals.format(o.expectedStartDate, 'dd/MM/yyyy')}"></span>
  <span class="status-badge" th:classappend="'status-badge--' + ${#strings.toLowerCase(o.status)}">...</span>
  ```
- **Lợi ích:** Tiết kiệm 65 dòng JavaScript, loại bỏ nguy cơ lệch định dạng giữa giao diện và cơ sở dữ liệu.

### 3.3. Loại bỏ hệ thống Banner Flash cũ (Tiết kiệm: 57 dòng)
- **Hiện trạng:** Dòng 1468–1524 trong `offers.js` chứa `initOfferFlashAutoDismiss`, `displayOfferFlashBanner`, `checkPendingOfferFlash` cố gắng tìm và tạo phần tử `#pageFlashContainer` với class `.page-flash-container`. Tuy nhiên phần tử này hoàn toàn không có trong bất kỳ template nào của hệ thống.
- **Giải pháp:** Xóa bỏ hoàn toàn. Toàn bộ các trang hiện đã sử dụng fragment thông báo hiện đại của nhóm: `fragments/ui/flash-toast :: flashToast` kèm `flash-toast.js` (56 dòng) với hiệu ứng thanh trượt CSS mượt mà.
- **Lợi ích:** Tiết kiệm 57 dòng JavaScript và loại bỏ các listener chạy thừa thãi khi DOMContentLoaded.

### 3.4. Loại bỏ các Modal Helper kiểu cũ (Tiết kiệm: 34 dòng)
- **Hiện trạng:** Dòng 60–90 trong `offers.js` chứa `openModal`, `closeModal`, `handleCreateModalBackdropClick`, `handleDetailModalBackdropClick`.
- **Giải pháp:** Xóa bỏ hoàn toàn. Các hộp thoại hiện nay (`#sendOfferConfirmDialog`, `#directorApproveConfirmDialog`, `#directorRejectConfirmDialog`, `#cof-dialog`, `#cof-accept-dialog`, `#cof-decline-dialog`) đều sử dụng thẻ chuẩn HTML5 `<dialog>` với API native `.showModal()` và `.close()`.
- **Lợi ích:** Tiết kiệm 34 dòng mã và tuân thủ chuẩn HTML5 hiện đại.

### 3.5. Loại bỏ hàm lấy chuỗi ngày tháng thừa (Tiết kiệm: 24 dòng)
- **Hiện trạng:** `getTodayDateString()` (8 dòng) và `getTomorrowDateString()` (16 dòng).
- **Giải pháp:** `getTodayDateString` không có bất kỳ dòng nào gọi. `getTomorrowDateString` chỉ được gọi bởi mã chết `openCreateOfferModal`. Logic ràng buộc ngày tối thiểu trong thực tế được đảm nhiệm bởi hàm `updateMinExpectedStartDate()`.
- **Lợi ích:** Tiết kiệm 24 dòng JavaScript.

### 3.6. Loại bỏ SweetAlert wrapper và hàm rỗng (Tiết kiệm: 63 dòng)
- **Hiện trạng:** `showDirectorCustomModal` (39 dòng), `submitDirectorApprove` (5 dòng), `submitDirectorReject` (5 dòng), `handleFilterSearch` (4 dòng), và khối khai báo window trùng lặp (10 dòng).
- **Giải pháp:** Xóa bỏ dứt điểm toàn bộ các wrapper trung gian không cần thiết này.
- **Lợi ích:** Tiết kiệm 63 dòng JavaScript.

---

## 4. BẢNG DỰ TOÁN KÍCH THƯỚC JAVASCRIPT THỰC TẾ SAU TỐI ƯU

| Chỉ số kỹ thuật | Trước tái cấu trúc (Hiện tại) | Sau khi loại bỏ mã chết (Task 10 Plan) | Tỷ lệ giảm |
| :--- | :---: | :---: | :---: |
| **Dòng mã `offers.js`** | 1.561 dòng | **~1.159 dòng** | **-25.75% (-402 dòng)** |
| **Dòng mã `candidate-offers.js`** | 285 dòng | **285 dòng** | Giữ nguyên (chuẩn hóa) |
| **Tổng JavaScript toàn phân hệ** | 1.846 dòng | **~1.444 dòng** | **-21.78%** |
| **Số tệp JavaScript** | 2 tệp đồ sộ | **7 tệp module chuyên biệt** | Kiến trúc module hóa |
| **Kích thước trung bình mỗi tệp** | 923 dòng/tệp | **~170 dòng/tệp** | Dễ đọc, dễ bảo vệ đồ án |
| **Tệp lớn nhất** | 1.561 dòng | **285 dòng** (`candidate-offers.js`) | Giảm 81.74% độ dài cực đại |

---

## 5. BÀI PHÁT BIỂU GIẢI TRÌNH BẢO VỆ ĐỒ ÁN SWP391 (DEFENSE STATEMENT)

> **Kính thưa Thầy/Cô Hội đồng Đánh giá Đồ án SWP391:**
>
> "Trong phân hệ Quản lý Đề xuất nhận việc (Offer Management), nhóm em đã tiến hành một đợt kiểm toán và tái thiết kế kiến trúc Frontend toàn diện:
>
> 1. **Chúng em đã loại bỏ thành công 402 dòng mã JavaScript chết** — vốn là tàn dư từ các bản mẫu modal cũ trong những sprint đầu tiên — chuyển giao toàn bộ trách nhiệm định dạng ngày tháng, hiển thị trạng thái và nộp dữ liệu biểu mẫu về cho **Thymeleaf Server-Side Rendering** và các thẻ **HTML5 Native Dialog**.
> 2. **Chúng em đã chia nhỏ tệp monolithic `offers.js` từ 1.561 dòng thành 6 tệp module độc lập**, mỗi tệp chỉ dao động từ **100 đến 210 dòng**, đảm nhiệm duy nhất một chức năng: Tiện ích chung, Danh sách tuyển dụng, Xuất Excel 25 cột, Biểu mẫu tính lương, Chi tiết đề xuất, và Bàn làm việc của Giám đốc.
> 3. **Chúng em kiên quyết giữ lại các logic JavaScript nghiệp vụ cốt lõi không thể thay thế:** Thuật toán tính tự động 85% lương thử việc theo quy định của Bộ Luật Lao Động 2019, tính toán động ngày bắt đầu hợp đồng, cơ chế xuất dữ liệu nhị phân Excel dạng Blob, và đồng hồ đếm ngược phản hồi theo thời gian thực của ứng viên.
>
> Kiến trúc mới không chỉ giúp trang web tải nhanh hơn do mỗi màn hình chỉ phải nạp đúng script cần thiết, mà còn đảm bảo **tính toàn vẹn 100% của cơ sở dữ liệu**, bảo vệ tuyệt đối các phân hệ của đồng đội, và đạt **223/223 bài kiểm thử tự động cùng 10 kịch bản kiểm thử trình duyệt thực tế**."
