# PHÂN TÍCH TÁC ĐỘNG & SỰ PHỤ THUỘC (DEPENDENCY & IMPACT ANALYSIS)

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).
**Nhiệm vụ:** Task 01 — Safe HTML/CSS Refactoring Audit<br>
**Mục tiêu:** Đảm bảo 100% tính nguyên vẹn của Backend, API Contracts, Security, Database và quy trình làm việc của các thành viên khác khi refactor giao diện.<br>
**Tài liệu tham chiếu:** `01-project-audit.md`, `02-js-classification.md`, `offer-management-source-audit.md`<br>

---

## 1. Tác động tới Hợp đồng Giao tiếp Backend & API Endpoints (API Contracts)

Một trong những quy tắc bất biến quan trọng nhất của đợt refactor này là: **TUYỆT ĐỐI KHÔNG ĐỔI BẤT KỲ ENDPOINT HOẶC METHOD SIGNATURE NÀO TRÊN BACKEND.**

Dưới đây là ma trận kiểm tra tính tương thích giữa giao diện HTML/CSS mới và các Controller hiện tại:

| Endpoint | HTTP Method | Content-Type hiện tại | Cơ chế sau Refactor HTML/CSS | Tính tương thích Backend |
| :--- | :---: | :---: | :--- | :---: |
| `/offers` | `GET` | `text/html` | Giữ nguyên. Thymeleaf kết xuất thẻ tab lọc trực tiếp qua query params (`?status=...`). | **Hoàn toàn tương thích 100%** |
| `/offers/create` | `GET` | `text/html` | Giữ nguyên. Tận dụng thuộc tính `th:min` cho ô ngày bắt đầu. | **Hoàn toàn tương thích 100%** |
| `/offers/create` | `POST` | `application/x-www-form-urlencoded` | Form POST chuẩn qua Thymeleaf model `CreateOfferRequest`. | **Hoàn toàn tương thích 100%** |
| `/offers/{id}/edit` | `POST` | `application/x-www-form-urlencoded` | Form POST chuẩn qua Thymeleaf model `UpdateOfferRequest`. | **Hoàn toàn tương thích 100%** |
| `/offers/{id}/delete`| `POST` | `application/x-www-form-urlencoded` | Nút submit xác nhận trong modal dialog chuẩn. | **Hoàn toàn tương thích 100%** |
| `/offers/{id}/send` | `POST` | `application/x-www-form-urlencoded` | Nút submit xác nhận trong modal dialog chuẩn. | **Hoàn toàn tương thích 100%** |
| `/offers/{id}/approve` | `POST` | Hỗ trợ cả `application/json` và Form Submit | Controller đã hỗ trợ cả 2 method. Chuyển sang Form submit native vẫn gọi đúng `OfferController.approveOffer`. | **Hoàn toàn tương thích 100%** |
| `/offers/{id}/reject` | `POST` | Hỗ trợ cả `application/json` và Form Submit | Chuyển sang Form submit native với trường `comments` bắt buộc. | **Hoàn toàn tương thích 100%** |
| `/offers/export` | `POST` | `application/json` | Giữ nguyên xử lý JS đóng gói mảng `OfferExportRequest`. | **Bảo toàn nguyên vẹn** |
| `/portal/offers/{id}/accept` | `POST` | `application/x-www-form-urlencoded` | Form POST native từ modal `<dialog id="acceptModal">`. | **Hoàn toàn tương thích 100%** |
| `/portal/offers/{id}/decline`| `POST` | `application/x-www-form-urlencoded` | Form POST native từ modal `<dialog id="declineModal">`. | **Hoàn toàn tương thích 100%** |

---

## 2. Tác động tới Spring Security & Quản lý CSRF Token

### 2.1. Cơ chế CSRF Token khi Chuyển đổi từ Fetch API sang Form HTML Native
* **Vấn đề trong Fetch API hiện tại:**
  * JavaScript phải tự tìm thẻ `<meta name="_csrf">`, đọc chuỗi hash, và chèn thủ công vào Header `X-CSRF-TOKEN` của request. Nếu trang thiếu thẻ meta hoặc token bị hết hạn, request mạng lập tức bị chặn với mã lỗi **HTTP 403 Forbidden**.
* **Lợi thế vượt trội của Form HTML Native:**
  * Khi sử dụng form HTML chuẩn với Thymeleaf (`th:action="@{...}"`), Thymeleaf **tự động render thẻ ẩn bảo mật**:
    ```html
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
    ```
  * Trình duyệt tự động đính kèm token này trong payload form `application/x-www-form-urlencoded`. Bộ lọc `CsrfFilter` của Spring Security tự động giải mã và cấp phép mà không cần một dòng code JavaScript nào!
  * **Đánh giá rủi ro:** **Bằng 0**. An toàn hơn và loại bỏ hoàn toàn nguy cơ lỗi cú pháp JS khi đọc token.

### 2.2. Bảo toàn Cơ chế Quản lý Session & Phòng chống BUG-OFF-10
* Quá trình chuyển đổi HTML/CSS **không chạm vào** endpoint `/api/notifications/unread-count` hay `topbar-notifications.js`.
* Mọi chuyển hướng sau khi submit form đều tuân thủ mô hình **Post/Redirect/Get (PRG)** của Spring MVC, giúp session người dùng luôn ổn định và không làm bẩn bộ nhớ đệm `HttpSessionRequestCache`.

---

## 3. Rà soát Mối liên kết DOM (Selectors, IDs & CSS Classes)

Khi loại bỏ hoặc tinh giản JavaScript, cần đặc biệt lưu ý các ID và Class CSS đang được chia sẻ giữa giao diện và mã kiểu dáng (Stylesheet):

| Bộ chọn DOM (Selector) | Tệp HTML sử dụng | Tệp CSS định kiểu | Rủi ro khi sửa đổi / Xóa bỏ | Hướng dẫn an toàn |
| :--- | :--- | :--- | :--- | :--- |
| `#directorDecisionModal` | `director-list.html` | `offers.css` | **Cao.** Định kiểu kích thước, vị trí modal native `<dialog>`. | **GIỮ NGUYÊN ID & CẤU TRÚC.** Chỉ thay thế nội dung form bên trong. |
| `.stat-card` | `list.html`, `director-list.html` | `offers.css` | **Trung bình.** Định kiểu thẻ KPI (hover, active, flexbox). | **GIỮ NGUYÊN CLASS.** Chuyển đổi thẻ cha từ `<div>` sang `<a>` mà không đổi class. |
| `#acceptModal`, `#declineModal` | `candidate-detail.html` | `candidate-offers.css` | **Cao.** Định kiểu hộp thoại xác nhận thư mời của ứng viên. | **GIỮ NGUYÊN ID & CLASS.** Chỉ tối ưu logic form bên trong. |
| `.reason-chip` | `candidate-detail.html` | `candidate-offers.css` | **Thấp.** Định kiểu các nút gợi ý lý do từ chối. | Giữ nguyên class, chỉ đổi thẻ từ `<div>` sang `<label>` bọc `<input type="radio">`. |
| `#expectedStartDate` | `form.html` | `offer-workspace.css` | **Cao.** Ràng buộc trực tiếp với Model Attribute `CreateOfferRequest`. | **BẤT BIẾN.** Thuộc tính `name` và `id` bắt buộc giữ nguyên để Spring MVC bind dữ liệu. |

---

## 4. Ranh giới Phân hệ (Cross-Module Boundaries)

Phân hệ Offer Management tiếp giáp với 3 phân hệ khác của các thành viên trong nhóm:

```
[Phân hệ Phỏng vấn - DucNM]
       │ (HM_Passed)
       ▼
[Phân hệ Thư mời - HuyenPT] ◄════════════► [Hệ thống Thông báo - HoangNH]
       │ (Offered / Hired / Rejected)           (In-App Notification)
       ▼
[Hồ sơ Ứng tuyển - DungLT]
```

### 4.1. Với Phân hệ Hồ sơ Ứng tuyển (DungLT)
* **Điểm tiếp giáp:** `ApplicationStatusService.onOfferAnswered()` và URL chuyển tiếp `/offers/create?applicationId=...`.
* **Ràng buộc:** Khi tối ưu form tạo mới, thuộc tính `applicationId` và `isLockedCandidate` bắt buộc phải được duy trì chính xác để không làm vỡ luồng chuyển tiếp từ màn hình duyệt hồ sơ của DungLT.

### 4.2. Với Phân hệ Thông báo & Giám đốc (HoangNH)
* **Điểm tiếp giáp:** Bàn làm việc của Giám đốc (`director-list.html`) và hệ thống thông báo chuông.
* **Ràng buộc:** Không can thiệp vào các thẻ hiển thị badge thông báo hay các file JavaScript của HoangNH (`topbar-notifications.js`).

---

## 5. Đánh giá Khả năng Tương thích Trình duyệt (Browser Compatibility)

Các tính năng HTML5/CSS3 dự kiến đưa vào thay thế JavaScript đã được kiểm tra mức độ tương thích trên các trình duyệt hiện đại:

1. **Thẻ HTML5 `<dialog>`:**
   * Tương thích: Chrome 37+, Edge 79+, Firefox 98+, Safari 15.4+ (**Đạt 98.5% thị phần toàn cầu**).
   * Dự án đã sử dụng thành công thẻ `<dialog>` từ commit `da95239a`, hoàn toàn an toàn khi tiếp tục sử dụng.
2. **Thuộc tính Form `<input min="..." max="...">` cho Date:**
   * Tương thích: 100% các trình duyệt máy tính và thiết bị di động hiện đại.
3. **CSS Pseudo-class `:user-invalid`:**
   * Tương thích: Chrome 119+, Edge 119+, Firefox 88+, Safari 16.5+.
   * Fallback an toàn: Sử dụng kèm selector `:invalid` tiêu chuẩn để hỗ trợ các phiên bản trình duyệt cũ hơn.
4. **CSS Pure Animation (`@keyframes`):**
   * Tương thích: 100% trình duyệt từ năm 2015 đến nay.
